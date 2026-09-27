package com.learnify.ai.prompt;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class PromptTemplateLoader {

    private final ObjectMapper objectMapper;

    public PromptTemplateLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public PromptTemplate load(String classpathLocation) {
        try (InputStream in = new ClassPathResource(classpathLocation).getInputStream()) {
            return objectMapper.readValue(in, PromptTemplate.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load prompt template: " + classpathLocation, e);
        }
    }
}
