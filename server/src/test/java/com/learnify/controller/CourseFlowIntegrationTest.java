package com.learnify.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
class CourseFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("pipeline.poll-interval-ms", () -> "3600000");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static JwtRequestPostProcessor asUser(String sub) {
        return jwt().jwt(builder -> builder.subject(sub));
    }

    @Test
    void createsAndReadsCourseModuleAndLesson() throws Exception {
        JwtRequestPostProcessor user = asUser("auth0|user-a");

        MvcResult courseResult = mockMvc.perform(post("/api/v1/courses")
                .with(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title": "Intro to Testing", "description": "desc", "tags": ["testing"]}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Intro to Testing"))
            .andReturn();
        UUID courseId = extractId(courseResult);

        mockMvc.perform(get("/api/v1/courses/{id}", courseId).with(user))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Intro to Testing"));

        mockMvc.perform(get("/api/v1/courses").with(user))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(courseId.toString()));

        MvcResult moduleResult = mockMvc.perform(post("/api/v1/courses/{courseId}/modules", courseId)
                .with(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title": "Module 1", "orderIndex": 0}
                    """))
            .andExpect(status().isCreated())
            .andReturn();
        UUID moduleId = extractId(moduleResult);

        mockMvc.perform(get("/api/v1/courses/{courseId}/modules", courseId).with(user))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(moduleId.toString()));

        mockMvc.perform(post("/api/v1/courses/{courseId}/modules/{moduleId}/lessons", courseId, moduleId)
                .with(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "What is a unit test?",
                      "orderIndex": 0,
                      "objectives": ["Understand unit tests"],
                      "content": [{"type": "heading", "text": "Intro"}]
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.content[0].type").value("heading"));

        mockMvc.perform(get("/api/v1/courses/{courseId}/modules/{moduleId}/lessons", courseId, moduleId).with(user))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("What is a unit test?"));

        mockMvc.perform(get("/api/v1/courses/{id}/export", courseId).with(user))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Intro to Testing"))
            .andExpect(jsonPath("$.modules[0].title").value("Module 1"))
            .andExpect(jsonPath("$.modules[0].lessons[0].title").value("What is a unit test?"))
            .andExpect(jsonPath("$.modules[0].lessons[0].content[0].type").value("heading"));
    }

    @Test
    void togglesLessonCompletedAndBookmarkedState() throws Exception {
        JwtRequestPostProcessor user = asUser("auth0|progress-user");

        MvcResult courseResult = mockMvc.perform(post("/api/v1/courses")
                .with(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title": "Progress Test Course"}
                    """))
            .andExpect(status().isCreated())
            .andReturn();
        UUID courseId = extractId(courseResult);

        MvcResult moduleResult = mockMvc.perform(post("/api/v1/courses/{courseId}/modules", courseId)
                .with(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title": "Module 1", "orderIndex": 0}
                    """))
            .andExpect(status().isCreated())
            .andReturn();
        UUID moduleId = extractId(moduleResult);

        MvcResult lessonResult = mockMvc.perform(post("/api/v1/courses/{courseId}/modules/{moduleId}/lessons", courseId, moduleId)
                .with(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title": "Lesson 1", "orderIndex": 0}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.completed").value(false))
            .andExpect(jsonPath("$.bookmarked").value(false))
            .andReturn();
        UUID lessonId = extractId(lessonResult);

        String lessonPath = "/api/v1/courses/{courseId}/modules/{moduleId}/lessons/{lessonId}";

        mockMvc.perform(patch(lessonPath, courseId, moduleId, lessonId)
                .with(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"completed": true}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.completed").value(true))
            .andExpect(jsonPath("$.bookmarked").value(false));

        mockMvc.perform(patch(lessonPath, courseId, moduleId, lessonId)
                .with(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"bookmarked": true}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.completed").value(true))
            .andExpect(jsonPath("$.bookmarked").value(true));

        // toggling back to not-completed
        mockMvc.perform(patch(lessonPath, courseId, moduleId, lessonId)
                .with(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"completed": false}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.completed").value(false))
            .andExpect(jsonPath("$.bookmarked").value(true));

        // another user can't toggle state on someone else's lesson
        mockMvc.perform(patch(lessonPath, courseId, moduleId, lessonId)
                .with(asUser("auth0|different-user"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"completed": true}
                    """))
            .andExpect(status().isNotFound());
    }

    @Test
    void anotherUsersCourseIsNotFoundNotForbidden() throws Exception {
        MvcResult courseResult = mockMvc.perform(post("/api/v1/courses")
                .with(asUser("auth0|owner"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title": "Someone else's course"}
                    """))
            .andExpect(status().isCreated())
            .andReturn();
        UUID courseId = extractId(courseResult);

        mockMvc.perform(get("/api/v1/courses/{id}", courseId).with(asUser("auth0|different-user")))
            .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/courses"))
            .andExpect(status().isUnauthorized());
    }

    private UUID extractId(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(body).get("id").asText());
    }
}
