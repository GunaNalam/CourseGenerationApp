package com.learnify.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnify.integration.gemini.GeminiTtsClient;
import com.learnify.integration.gemini.TranslationClient;
import com.learnify.integration.gemini.WavEncoder;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class LessonAudioIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("pipeline.poll-interval-ms", () -> "3600000");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TranslationClient translationClient;

    @MockBean
    private GeminiTtsClient ttsClient;

    private static JwtRequestPostProcessor asUser(String sub) {
        return jwt().jwt(builder -> builder.subject(sub));
    }

    @Test
    void generatesHinglishAudioForOwnedLesson() throws Exception {
        JwtRequestPostProcessor owner = asUser("auth0|audio-owner");
        byte[] fakeWav = WavEncoder.wrapPcmAsWav(new byte[]{1, 2, 3, 4}, 24_000, 1, 16);

        when(translationClient.translateToHinglish(anyString(), anyString())).thenReturn("Yeh ek Hinglish text hai.");
        when(ttsClient.synthesizeSpeech(anyString(), anyString(), anyString())).thenReturn(fakeWav);

        UUID lessonId = createCourseModuleLesson(owner);

        MvcResult result = mockMvc.perform(
                get("/api/v1/courses/{courseId}/modules/{moduleId}/lessons/{lessonId}/audio", courseId, moduleId, lessonId)
                    .with(owner)
            )
            .andExpect(status().isOk())
            .andReturn();

        assertThat(result.getResponse().getContentType()).isEqualTo("audio/wav");
        assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(fakeWav);
    }

    @Test
    void secondRequestForSameLessonIsServedFromCacheWithoutCallingGeminiAgain() throws Exception {
        JwtRequestPostProcessor owner = asUser("auth0|audio-cache-owner");
        byte[] fakeWav = WavEncoder.wrapPcmAsWav(new byte[]{5, 6, 7, 8}, 24_000, 1, 16);

        when(translationClient.translateToHinglish(anyString(), anyString())).thenReturn("Yeh cached Hinglish hai.");
        when(ttsClient.synthesizeSpeech(anyString(), anyString(), anyString())).thenReturn(fakeWav);

        UUID lessonId = createCourseModuleLesson(owner);

        for (int i = 0; i < 2; i++) {
            MvcResult result = mockMvc.perform(
                    get("/api/v1/courses/{courseId}/modules/{moduleId}/lessons/{lessonId}/audio", courseId, moduleId, lessonId)
                        .with(owner)
                )
                .andExpect(status().isOk())
                .andReturn();
            assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(fakeWav);
        }

        verify(translationClient, times(1)).translateToHinglish(anyString(), anyString());
        verify(ttsClient, times(1)).synthesizeSpeech(anyString(), anyString(), anyString());
    }

    @Test
    void otherUsersLessonAudioIsNotFound() throws Exception {
        when(translationClient.translateToHinglish(anyString(), anyString())).thenReturn("translated");
        when(ttsClient.synthesizeSpeech(anyString(), anyString(), anyString()))
            .thenReturn(WavEncoder.wrapPcmAsWav(new byte[]{1}, 24_000, 1, 16));

        UUID lessonId = createCourseModuleLesson(asUser("auth0|audio-owner-2"));

        mockMvc.perform(
                get("/api/v1/courses/{courseId}/modules/{moduleId}/lessons/{lessonId}/audio", courseId, moduleId, lessonId)
                    .with(asUser("auth0|different-listener"))
            )
            .andExpect(status().isNotFound());
    }

    private UUID courseId;
    private UUID moduleId;

    private UUID createCourseModuleLesson(JwtRequestPostProcessor user) throws Exception {
        MvcResult courseResult = mockMvc.perform(post("/api/v1/courses")
                .with(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title": "Audio Test Course"}
                    """))
            .andExpect(status().isCreated())
            .andReturn();
        courseId = UUID.fromString(objectMapper.readTree(courseResult.getResponse().getContentAsString()).get("id").asText());

        MvcResult moduleResult = mockMvc.perform(post("/api/v1/courses/{courseId}/modules", courseId)
                .with(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title": "Module 1", "orderIndex": 0}
                    """))
            .andExpect(status().isCreated())
            .andReturn();
        moduleId = UUID.fromString(objectMapper.readTree(moduleResult.getResponse().getContentAsString()).get("id").asText());

        MvcResult lessonResult = mockMvc.perform(post("/api/v1/courses/{courseId}/modules/{moduleId}/lessons", courseId, moduleId)
                .with(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title": "Lesson 1", "orderIndex": 0, "content": [{"type": "paragraph", "text": "Hello world."}]}
                    """))
            .andExpect(status().isCreated())
            .andReturn();
        return UUID.fromString(objectMapper.readTree(lessonResult.getResponse().getContentAsString()).get("id").asText());
    }
}
