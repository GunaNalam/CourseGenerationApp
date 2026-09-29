package com.learnify.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
class ApiKeyControllerIntegrationTest {

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

    @Test
    void setCheckAndDeleteApiKey() throws Exception {
        JwtRequestPostProcessor user = jwt().jwt(builder -> builder.subject("auth0|apikey-test"));

        mockMvc.perform(get("/api/v1/users/me/api-key").with(user))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.configured").value(false));

        mockMvc.perform(put("/api/v1/users/me/api-key")
                .with(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"apiKey": "sk-my-real-key"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.configured").value(true));

        mockMvc.perform(get("/api/v1/users/me/api-key").with(user))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.configured").value(true));

        mockMvc.perform(delete("/api/v1/users/me/api-key").with(user))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/me/api-key").with(user))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.configured").value(false));
    }
}
