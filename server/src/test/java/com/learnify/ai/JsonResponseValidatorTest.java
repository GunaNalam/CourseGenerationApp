package com.learnify.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class JsonResponseValidatorTest {

    private final JsonResponseValidator validator = new JsonResponseValidator(new ObjectMapper());

    @Test
    void parsesValidCourseJson() {
        String raw = """
            {
              "title": "Intro to ML",
              "description": "A beginner course",
              "tags": ["ml", "ai"],
              "modules": [
                { "title": "Basics", "lessons": ["What is ML?", "Types of ML"] }
              ]
            }
            """;

        GeneratedCourse course = validator.parseCourse(raw);

        assertThat(course.title()).isEqualTo("Intro to ML");
        assertThat(course.modules()).hasSize(1);
        assertThat(course.modules().get(0).lessons()).containsExactly("What is ML?", "Types of ML");
    }

    @Test
    void parsesCourseJsonWrappedInMarkdownFence() {
        String raw = """
            ```json
            {"title": "Intro", "modules": [{"title": "M1", "lessons": ["L1"]}]}
            ```
            """;

        GeneratedCourse course = validator.parseCourse(raw);

        assertThat(course.title()).isEqualTo("Intro");
    }

    @Test
    void rejectsCourseJsonWithNoModules() {
        String raw = """
            {"title": "Intro to ML"}
            """;

        assertThatThrownBy(() -> validator.parseCourse(raw))
            .isInstanceOf(AiGenerationException.class)
            .hasMessageContaining("modules");
    }

    @Test
    void rejectsMalformedJson() {
        assertThatThrownBy(() -> validator.parseCourse("not json at all"))
            .isInstanceOf(AiGenerationException.class);
    }

    @Test
    void parsesValidLessonJson() {
        String raw = """
            {
              "title": "What is Regression?",
              "objectives": ["Understand regression"],
              "content": [
                {"type": "heading", "text": "Intro"},
                {"type": "mcq", "question": "Q?", "options": ["a", "b"], "answer": 0, "explanation": "because"}
              ]
            }
            """;

        GeneratedLesson lesson = validator.parseLesson(raw);

        assertThat(lesson.title()).isEqualTo("What is Regression?");
        assertThat(lesson.content()).hasSize(2);
        assertThat(lesson.content().get(0)).containsEntry("type", "heading");
    }

    @Test
    void rejectsLessonContentBlockWithUnknownType() {
        String raw = """
            {"title": "L", "content": [{"type": "diagram", "text": "x"}]}
            """;

        assertThatThrownBy(() -> validator.parseLesson(raw))
            .isInstanceOf(AiGenerationException.class)
            .hasMessageContaining("type");
    }
}
