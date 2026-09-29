package com.learnify.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnify.ai.prompt.CoursePromptBuilder;
import com.learnify.ai.prompt.LessonPromptBuilder;
import com.learnify.ai.prompt.PromptTemplateLoader;
import org.junit.jupiter.api.Test;

class PromptBuilderTest {

    private final PromptTemplateLoader loader = new PromptTemplateLoader(new ObjectMapper());

    @Test
    void coursePromptFillsTopicPlaceholder() {
        CoursePromptBuilder builder = new CoursePromptBuilder(loader);

        String prompt = builder.build("Introduction to Machine Learning");

        assertThat(prompt).contains("Introduction to Machine Learning");
        assertThat(prompt).contains("raw JSON only");
        assertThat(prompt).doesNotContain("{topic}");
    }

    @Test
    void lessonPromptFillsAllPlaceholders() {
        LessonPromptBuilder builder = new LessonPromptBuilder(loader);

        String prompt = builder.build("Intro to ML", "Supervised Learning", "What is Regression?");

        assertThat(prompt).contains("Intro to ML");
        assertThat(prompt).contains("Supervised Learning");
        assertThat(prompt).contains("What is Regression?");
        assertThat(prompt).doesNotContain("{courseTitle}", "{moduleTitle}", "{lessonTitle}");
    }
}
