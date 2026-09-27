package com.learnify.dto.response;

import java.util.UUID;

public record JobStatusResponse(
    UUID pipelineRunId,
    String status,
    String currentStep,
    long position,
    UUID courseId,
    String error,
    boolean usingDefaultKey,
    String keyFallbackReason
) {
}
