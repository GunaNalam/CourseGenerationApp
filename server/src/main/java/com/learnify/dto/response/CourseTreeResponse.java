package com.learnify.dto.response;

import com.learnify.entity.Course;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CourseTreeResponse(
    UUID id,
    String title,
    String description,
    List<String> tags,
    Instant createdAt,
    List<ModuleWithLessonsResponse> modules
) {

    public static CourseTreeResponse from(Course course, List<ModuleWithLessonsResponse> modules) {
        return new CourseTreeResponse(
            course.getId(),
            course.getTitle(),
            course.getDescription(),
            course.getTags(),
            course.getCreatedAt(),
            modules
        );
    }
}
