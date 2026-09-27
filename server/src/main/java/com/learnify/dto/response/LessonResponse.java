package com.learnify.dto.response;

import com.learnify.entity.Lesson;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record LessonResponse(
    UUID id,
    String title,
    int orderIndex,
    List<String> objectives,
    List<Map<String, Object>> content,
    boolean isEnriched,
    boolean completed,
    boolean bookmarked
) {

    public static LessonResponse from(Lesson lesson) {
        return new LessonResponse(
            lesson.getId(),
            lesson.getTitle(),
            lesson.getOrderIndex(),
            lesson.getObjectives(),
            lesson.getContent(),
            lesson.isEnriched(),
            lesson.isCompleted(),
            lesson.isBookmarked()
        );
    }
}
