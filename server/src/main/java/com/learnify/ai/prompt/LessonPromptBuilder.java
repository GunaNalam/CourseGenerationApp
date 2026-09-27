package com.learnify.ai.prompt;

import org.springframework.stereotype.Component;

@Component
public class LessonPromptBuilder {

    private final PromptTemplate template;

    public LessonPromptBuilder(PromptTemplateLoader loader) {
        this.template = loader.load("prompts/lesson-prompt.json");
    }

    public String build(String courseTitle, String moduleTitle, String lessonTitle) {
        return build(courseTitle, moduleTitle, lessonTitle, null);
    }

    public String build(String courseTitle, String moduleTitle, String lessonTitle, String retryHint) {
        String filled = template.template()
            .replace("{courseTitle}", courseTitle)
            .replace("{moduleTitle}", moduleTitle)
            .replace("{lessonTitle}", lessonTitle);
        String prompt = template.system() + "\n\n" + filled;
        if (retryHint != null && !retryHint.isBlank()) {
            prompt += "\n\nNote: a previous attempt failed validation with this error — fix it this time: " + retryHint;
        }
        return prompt;
    }
}
