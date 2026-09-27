package com.learnify.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SetApiKeyRequest(@NotBlank String apiKey) {
}
