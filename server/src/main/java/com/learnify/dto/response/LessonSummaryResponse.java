package com.learnify.dto.response;

import com.learnify.entity.Lesson;
import java.util.UUID;

public record LessonSummaryResponse(
    UUID id,
    String title,
    int orderIndex,
    boolean isEnriched,
    boolean completed,
    boolean bookmarked
) {

    public static LessonSummaryResponse from(Lesson lesson) {
        return new LessonSummaryResponse(
            lesson.getId(),
            lesson.getTitle(),
            lesson.getOrderIndex(),
            lesson.isEnriched(),
            lesson.isCompleted(),
            lesson.isBookmarked()
        );
    }
}
