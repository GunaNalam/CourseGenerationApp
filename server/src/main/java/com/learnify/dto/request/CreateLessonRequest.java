package com.learnify.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

public record CreateLessonRequest(
    @NotBlank String title,
    int orderIndex,
    List<String> objectives,
    List<Map<String, Object>> content
) {
}
