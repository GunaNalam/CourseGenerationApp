package com.learnify.pipeline;

import com.learnify.ai.GeminiClient;
import com.learnify.ai.GeneratedLesson;
import com.learnify.ai.JsonResponseValidator;
import com.learnify.ai.prompt.LessonPromptBuilder;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class LessonContentFunction implements Function<LessonContentFunction.Input, LessonContentFunction.Output> {

    private final LessonPromptBuilder promptBuilder;
    private final GeminiClient geminiClient;
    private final JsonResponseValidator validator;

    public LessonContentFunction(LessonPromptBuilder promptBuilder, GeminiClient geminiClient, JsonResponseValidator validator) {
        this.promptBuilder = promptBuilder;
        this.geminiClient = geminiClient;
        this.validator = validator;
    }

    @Override
    public Output apply(Input input) {
        String prompt = promptBuilder.build(input.courseTitle(), input.moduleTitle(), input.lessonTitle(), input.retryHint());
        String raw = geminiClient.generate(prompt, input.apiKey());
        return new Output(validator.parseLesson(raw));
    }

    public record Input(String courseTitle, String moduleTitle, String lessonTitle, String retryHint, String apiKey) {
    }

    public record Output(GeneratedLesson lesson) {
    }
}
