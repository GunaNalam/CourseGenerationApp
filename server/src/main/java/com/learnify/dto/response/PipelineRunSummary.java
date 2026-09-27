package com.learnify.dto.response;

import com.learnify.entity.PipelineRun;
import java.time.Instant;
import java.util.UUID;

public record PipelineRunSummary(
    UUID id,
    UUID ownerId,
    String status,
    UUID courseId,
    Instant createdAt
) {

    public static PipelineRunSummary from(PipelineRun run) {
        return new PipelineRunSummary(run.getId(), run.getOwnerId(), run.getStatus().name(), run.getCourseId(), run.getCreatedAt());
    }
}
