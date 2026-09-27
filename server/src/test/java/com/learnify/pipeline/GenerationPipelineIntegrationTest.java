package com.learnify.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnify.ai.AiGenerationException;
import com.learnify.ai.GeminiClient;
import com.learnify.apikey.ApiKeyEncryptor;
import com.learnify.entity.PipelineRun;
import com.learnify.entity.User;
import com.learnify.entity.UserApiKey;
import com.learnify.integration.youtube.YouTubeClient;
import com.learnify.repository.CourseRepository;
import com.learnify.repository.LessonRepository;
import com.learnify.repository.PipelineRunRepository;
import com.learnify.repository.UserApiKeyRepository;
import com.learnify.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
class GenerationPipelineIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // Prevent the real @Scheduled poller from racing our manual pollAndProcess() calls below.
        registry.add("pipeline.poll-interval-ms", () -> "3600000");
        // Force a known default key regardless of any local config/application.properties on
        // the machine running these tests — tests must not depend on developer machine state.
        registry.add("gemini.api-key", () -> "");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PipelinePoller pipelinePoller;

    @Autowired
    private PipelineRunRepository pipelineRunRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserApiKeyRepository userApiKeyRepository;

    @Autowired
    private ApiKeyEncryptor apiKeyEncryptor;

    @MockBean
    private GeminiClient geminiClient;

    @MockBean
    private YouTubeClient youTubeClient;

    @BeforeEach
    void resetMock() {
        reset(geminiClient);
        reset(youTubeClient);
    }

    @Test
    void generatesCourseEndToEnd() throws Exception {
        String courseJson = """
            {
              "title": "Intro to Testing",
              "description": "A course about testing",
              "tags": ["testing"],
              "modules": [
                { "title": "Basics", "lessons": ["What is a test?", "Writing your first test"] }
              ]
            }
            """;
        String lessonJson = """
            {
              "title": "What is a test?",
              "objectives": ["Understand testing"],
              "content": [
                {"type": "heading", "text": "Intro"},
                {"type": "video", "query": "what is software testing"}
              ]
            }
            """;
        when(geminiClient.generate(anyString(), anyString())).thenReturn(courseJson, lessonJson, lessonJson);
        when(youTubeClient.searchVideoId(anyString())).thenReturn(Optional.of("abc123"));

        UUID pipelineRunId = startGeneration("Software Testing");

        runPollerUntilFinished(pipelineRunId);

        PipelineRun run = pipelineRunRepository.findById(pipelineRunId).orElseThrow();
        assertThat(run.getStatus()).isEqualTo(JobStatus.DONE);
        assertThat(run.getCourseId()).isNotNull();

        var course = courseRepository.findById(run.getCourseId()).orElseThrow();
        assertThat(course.getTitle()).isEqualTo("Intro to Testing");

        var lessons = lessonRepository.findAll();
        assertThat(lessons).hasSize(2);
        assertThat(lessons.get(0).isEnriched()).isTrue();
        assertThat(lessons.get(0).getContent()).isNotEmpty();

        var videoBlock = lessons.get(0).getContent().stream()
            .filter(block -> "video".equals(block.get("type")))
            .findFirst()
            .orElseThrow();
        assertThat(videoBlock.get("embedUrl")).isEqualTo("https://www.youtube.com/embed/abc123");
    }

    @Test
    void malformedAiResponseRetriesThenFailsCleanly() throws Exception {
        when(geminiClient.generate(anyString(), anyString())).thenReturn("this is not valid json");

        UUID pipelineRunId = startGeneration("Broken Topic");

        runPollerUntilFinished(pipelineRunId);

        PipelineRun run = pipelineRunRepository.findById(pipelineRunId).orElseThrow();
        assertThat(run.getStatus()).isEqualTo(JobStatus.FAILED);
    }

    @Test
    void invalidUserApiKeyFallsBackToDefaultAndFlagsIt() throws Exception {
        String badKey = "bad-user-key";
        User user = new User();
        user.setAuth0Sub("auth0|fallback-test");
        user = userRepository.save(user);

        UserApiKey userApiKey = new UserApiKey();
        userApiKey.setUserId(user.getId());
        userApiKey.setProvider("gemini");
        userApiKey.setEncryptedKey(apiKeyEncryptor.encrypt(badKey));
        userApiKeyRepository.save(userApiKey);

        String courseJson = """
            {"title": "Recovered Course", "modules": [{"title": "M1", "lessons": ["L1"]}]}
            """;
        String lessonJson = """
            {"title": "L1", "objectives": ["obj"], "content": [{"type": "heading", "text": "h"}]}
            """;
        when(geminiClient.generate(anyString(), eq(badKey))).thenThrow(new AiGenerationException("invalid API key"));
        when(geminiClient.generate(anyString(), eq(""))).thenReturn(courseJson, lessonJson);

        UUID pipelineRunId = startGenerationAs("auth0|fallback-test", "Fallback Topic");

        runPollerUntilFinished(pipelineRunId);

        PipelineRun run = pipelineRunRepository.findById(pipelineRunId).orElseThrow();
        assertThat(run.getStatus()).isEqualTo(JobStatus.DONE);

        var course = courseRepository.findById(run.getCourseId()).orElseThrow();
        assertThat(course.getTitle()).isEqualTo("Recovered Course");
    }

    private UUID startGeneration(String topic) throws Exception {
        return startGenerationAs("auth0|pipeline-test", topic);
    }

    private UUID startGenerationAs(String subject, String topic) throws Exception {
        JwtRequestPostProcessor user = jwt().jwt(builder -> builder.subject(subject));

        MvcResult result = mockMvc.perform(post("/api/v1/courses/generate")
                .with(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"topic\": \"" + topic + "\"}"))
            .andExpect(status().isAccepted())
            .andReturn();

        return UUID.fromString(
            objectMapper.readTree(result.getResponse().getContentAsString()).get("pipelineRunId").asText()
        );
    }

    private void runPollerUntilFinished(UUID pipelineRunId) {
        for (int i = 0; i < 10; i++) {
            PipelineRun run = pipelineRunRepository.findById(pipelineRunId).orElseThrow();
            if (run.getStatus() == JobStatus.DONE || run.getStatus() == JobStatus.FAILED) {
                return;
            }
            pipelinePoller.pollAndProcess();
        }
    }
}
