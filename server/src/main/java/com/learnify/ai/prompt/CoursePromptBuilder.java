package com.learnify.ai.prompt;

import org.springframework.stereotype.Component;

@Component
public class CoursePromptBuilder {

    private final PromptTemplate template;

    public CoursePromptBuilder(PromptTemplateLoader loader) {
        this.template = loader.load("prompts/course-prompt.json");
    }

    public String build(String topic) {
        return build(topic, null);
    }

    public String build(String topic, String retryHint) {
        String filled = template.template().replace("{topic}", topic);
        String prompt = template.system() + "\n\n" + filled;
        if (retryHint != null && !retryHint.isBlank()) {
            prompt += "\n\nNote: a previous attempt failed validation with this error — fix it this time: " + retryHint;
        }
        return prompt;
    }
}
