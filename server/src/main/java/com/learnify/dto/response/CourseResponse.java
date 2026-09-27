package com.learnify.dto.response;

import com.learnify.entity.Course;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CourseResponse(
    UUID id,
    String title,
    String description,
    List<String> tags,
    Instant createdAt
) {

    public static CourseResponse from(Course course) {
        return new CourseResponse(
            course.getId(),
            course.getTitle(),
            course.getDescription(),
            course.getTags(),
            course.getCreatedAt()
        );
    }
}
