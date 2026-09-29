package com.learnify.integration.gemini;

import com.learnify.ai.AiGenerationException;
import com.learnify.ai.GeminiRateLimiter;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Calls a Gemini TTS-capable model (responseModalities: AUDIO) and wraps the raw PCM
 * it returns into a playable WAV. This is the one integration in the project not
 * exercised against the real API during development (no live Gemini key available) —
 * the request/response shape follows Google's documented TTS contract as of when this
 * was written, but should be the first thing verified once a real key is configured.
 */
@Component
public class GeminiTtsClient {

    private static final int SAMPLE_RATE_HZ = 24_000;
    private static final int CHANNELS = 1;
    private static final int BITS_PER_SAMPLE = 16;

    private final RestClient restClient;
    private final String model;
    private final GeminiRateLimiter rateLimiter;

    public GeminiTtsClient(RestClient.Builder restClientBuilder, @Value("${gemini.tts-model}") String model, GeminiRateLimiter rateLimiter) {
        this.restClient = restClientBuilder.baseUrl("https://generativelanguage.googleapis.com").build();
        this.model = model;
        this.rateLimiter = rateLimiter;
    }

    public byte[] synthesizeSpeech(String text, String voiceName, String apiKey) {
        Map<String, Object> requestBody = Map.of(
            "contents", List.of(Map.of("parts", List.of(Map.of("text", text)))),
            "generationConfig", Map.of(
                "responseModalities", List.of("AUDIO"),
                "speechConfig", Map.of(
                    "voiceConfig", Map.of("prebuiltVoiceConfig", Map.of("voiceName", voiceName))
                )
            )
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
            throw new AiGenerationException(
                "Gemini TTS call failed: HTTP " + e.getStatusCode().value() + " - " + e.getResponseBodyAsString(), e
            );
        } catch (RestClientException e) {
            throw new AiGenerationException("Gemini TTS call failed: " + e.getMessage(), e);
        }

        byte[] pcm = Base64.getDecoder().decode(extractAudioBase64(response));
        return WavEncoder.wrapPcmAsWav(pcm, SAMPLE_RATE_HZ, CHANNELS, BITS_PER_SAMPLE);
    }

    private String extractAudioBase64(Map<?, ?> response) {
        try {
            List<?> candidates = (List<?>) response.get("candidates");
            Map<?, ?> firstCandidate = (Map<?, ?>) candidates.get(0);
            Map<?, ?> content = (Map<?, ?>) firstCandidate.get("content");
            List<?> parts = (List<?>) content.get("parts");
            Map<?, ?> firstPart = (Map<?, ?>) parts.get(0);
            Map<?, ?> inlineData = (Map<?, ?>) firstPart.get("inlineData");
            return (String) inlineData.get("data");
        } catch (RuntimeException e) {
            throw new AiGenerationException("Unexpected Gemini TTS response shape", e);
        }
    }
}
