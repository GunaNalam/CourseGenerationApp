package com.learnify.ratelimit;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.learnify.ai.GeminiClient;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class RateLimitIntegrationTest {

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

    @MockBean
    private GeminiClient geminiClient;

    @Test
    void thirdGenerationRequestWithinWindowIsRejected() throws Exception {
        JwtRequestPostProcessor user = jwt().jwt(builder -> builder.subject("auth0|ratelimit-test"));

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/v1/courses/generate")
                    .with(user)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"topic\": \"Topic " + i + "\"}"))
                .andExpect(status().isAccepted());
        }

        mockMvc.perform(post("/api/v1/courses/generate")
                .with(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"topic\": \"Topic 3\"}"))
            .andExpect(status().isTooManyRequests());
    }
}
