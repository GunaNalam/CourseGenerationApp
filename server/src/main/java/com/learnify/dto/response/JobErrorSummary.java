package com.learnify.dto.response;

import com.learnify.entity.JobStep;
import java.time.Instant;
import java.util.UUID;

public record JobErrorSummary(
    UUID stepId,
    UUID pipelineRunId,
    String type,
    String error,
    int attempt,
    Instant createdAt
) {

    public static JobErrorSummary from(JobStep step) {
        return new JobErrorSummary(
            step.getId(), step.getPipelineRunId(), step.getType().name(), step.getError(), step.getAttempt(), step.getCreatedAt()
        );
    }
}
