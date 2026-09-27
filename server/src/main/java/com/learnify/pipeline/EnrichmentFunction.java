package com.learnify.pipeline;

import com.learnify.integration.youtube.YouTubeClient;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class EnrichmentFunction implements Function<EnrichmentFunction.Input, EnrichmentFunction.Output> {

    private final YouTubeClient youTubeClient;

    public EnrichmentFunction(YouTubeClient youTubeClient) {
        this.youTubeClient = youTubeClient;
    }

    @Override
    public Output apply(Input input) {
        if (input.videoQuery() == null || input.videoQuery().isBlank()) {
            return new Output(null);
        }
        return new Output(youTubeClient.searchVideoId(input.videoQuery()).orElse(null));
    }

    public record Input(String videoQuery) {
    }

    public record Output(String videoId) {
    }
}
