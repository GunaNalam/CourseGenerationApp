package com.learnify.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateModuleRequest(
    @NotBlank String title,
    int orderIndex
) {
}
