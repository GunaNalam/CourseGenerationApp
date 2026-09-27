package com.learnify.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GenerateCourseRequest(@NotBlank String topic) {
}
