package com.learnify.ai;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class GeminiClient {

    private final RestClient restClient;
    private final String model;
    private final GeminiRateLimiter rateLimiter;

    public GeminiClient(RestClient.Builder restClientBuilder, @Value("${gemini.model}") String model, GeminiRateLimiter rateLimiter) {
        this.restClient = restClientBuilder.baseUrl("https://generativelanguage.googleapis.com").build();
        this.model = model;
        this.rateLimiter = rateLimiter;
    }

    public String generate(String prompt, String apiKey) {
        Map<String, Object> requestBody = Map.of(
            "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
        );

        rateLimiter.acquire();
        Map<?, ?> response;
        try {
            response = restClient.post()
                .uri("/v1beta/models/{model}:generateContent?key={key}", model, apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(Map.class);
        } catch (RestClientResponseException e) {
            // Carries the real reason Gemini rejected the call (bad key, wrong model
            // name, quota, etc.) — surfaced all the way to JobStep.error / /admin/errors,
            // instead of a generic message that hides the actual cause.
            throw new AiGenerationException(
                "Gemini API call failed: HTTP " + e.getStatusCode().value() + " - " + e.getResponseBodyAsString(), e
            );
        } catch (RestClientException e) {
            throw new AiGenerationException("Gemini API call failed: " + e.getMessage(), e);
        }

        return extractText(response);
    }

    private String extractText(Map<?, ?> response) {
        try {
            List<?> candidates = (List<?>) response.get("candidates");
            Map<?, ?> firstCandidate = (Map<?, ?>) candidates.get(0);
            Map<?, ?> content = (Map<?, ?>) firstCandidate.get("content");
            List<?> parts = (List<?>) content.get("parts");
            Map<?, ?> firstPart = (Map<?, ?>) parts.get(0);
            return (String) firstPart.get("text");
        } catch (RuntimeException e) {
            throw new AiGenerationException("Unexpected Gemini response shape: " + response, e);
        }
    }
}
