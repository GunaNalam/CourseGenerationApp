package com.learnify.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record CreateCourseRequest(
    @NotBlank String title,
    String description,
    List<String> tags
) {
}
