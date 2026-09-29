package com.learnify.pipeline;

import com.learnify.ai.GeminiClient;
import com.learnify.ai.GeneratedCourse;
import com.learnify.ai.JsonResponseValidator;
import com.learnify.ai.prompt.CoursePromptBuilder;
import java.util.function.Function;
import org.springframework.stereotype.Component;

/**
 * Plain Function<I,O> with no framework glue, per the Spring Cloud Function shape
 * (BACKEND_PLAN.md §8) — invoked in-process by PipelinePoller today, independently
 * deployable as a standalone function later without touching this class. Which API
 * key to use is resolved by the caller (JobStepProcessor), not this class — key
 * resolution/fallback is an orchestration concern, not generation logic.
 */
@Component
public class OutlineFunction implements Function<OutlineFunction.Input, OutlineFunction.Output> {

    private final CoursePromptBuilder promptBuilder;
    private final GeminiClient geminiClient;
    private final JsonResponseValidator validator;

    public OutlineFunction(CoursePromptBuilder promptBuilder, GeminiClient geminiClient, JsonResponseValidator validator) {
        this.promptBuilder = promptBuilder;
        this.geminiClient = geminiClient;
        this.validator = validator;
    }

    @Override
    public Output apply(Input input) {
        String prompt = promptBuilder.build(input.topic(), input.retryHint());
        String raw = geminiClient.generate(prompt, input.apiKey());
        return new Output(validator.parseCourse(raw));
    }

    public record Input(String topic, String retryHint, String apiKey) {
    }

    public record Output(GeneratedCourse course) {
    }
}
