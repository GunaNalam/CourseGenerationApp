package com.learnify.integration.youtube;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class YouTubeClient {

    private final RestClient restClient;
    private final String apiKey;

    public YouTubeClient(RestClient.Builder restClientBuilder, @Value("${youtube.api-key}") String apiKey) {
        this.restClient = restClientBuilder.baseUrl("https://www.googleapis.com/youtube/v3").build();
        this.apiKey = apiKey;
    }

    /**
     * Best-effort video lookup — a failed or empty search should not fail the whole
     * lesson's enrichment, so this returns empty rather than throwing.
     */
    public Optional<String> searchVideoId(String query) {
        try {
            Map<?, ?> response = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/search")
                    .queryParam("part", "snippet")
                    .queryParam("q", query)
                    .queryParam("type", "video")
                    .queryParam("videoEmbeddable", "true")
                    .queryParam("maxResults", 1)
                    .queryParam("key", apiKey)
                    .build())
                .retrieve()
                .body(Map.class);
            return extractVideoId(response);
        } catch (RestClientException e) {
            return Optional.empty();
        }
    }

    private Optional<String> extractVideoId(Map<?, ?> response) {
        try {
            List<?> items = (List<?>) response.get("items");
            if (items == null || items.isEmpty()) {
                return Optional.empty();
            }
            Map<?, ?> first = (Map<?, ?>) items.get(0);
            Map<?, ?> id = (Map<?, ?>) first.get("id");
            return Optional.ofNullable((String) id.get("videoId"));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }
}
