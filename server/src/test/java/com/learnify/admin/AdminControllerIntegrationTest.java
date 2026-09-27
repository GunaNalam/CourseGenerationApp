package com.learnify.admin;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
class AdminControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("pipeline.poll-interval-ms", () -> "3600000");
        registry.add("admin.allowed-subs", () -> "auth0|the-admin");
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void nonAdminUserIsForbidden() throws Exception {
        JwtRequestPostProcessor user = jwt().jwt(builder -> builder.subject("auth0|regular-user"));

        mockMvc.perform(get("/api/v1/admin/jobs").with(user))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminUserCanReadStats() throws Exception {
        JwtRequestPostProcessor admin = jwt().jwt(builder -> builder.subject("auth0|the-admin"));

        mockMvc.perform(get("/api/v1/admin/stats").with(admin))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/admin/jobs").with(admin))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/admin/errors").with(admin))
            .andExpect(status().isOk());
    }
}
