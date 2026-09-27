package com.learnify.dto.response;

import com.learnify.entity.Module;
import java.util.List;
import java.util.UUID;

public record ModuleWithLessonsResponse(
    UUID id,
    String title,
    int orderIndex,
    List<LessonSummaryResponse> lessons
) {

    public static ModuleWithLessonsResponse from(Module module, List<LessonSummaryResponse> lessons) {
        return new ModuleWithLessonsResponse(module.getId(), module.getTitle(), module.getOrderIndex(), lessons);
    }
}
