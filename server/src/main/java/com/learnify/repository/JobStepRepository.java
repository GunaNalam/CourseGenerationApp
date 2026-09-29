package com.learnify.repository;

import com.learnify.entity.JobStep;
import com.learnify.pipeline.JobStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface JobStepRepository extends JpaRepository<JobStep, UUID> {

    /**
     * Steps that are PENDING and either have no dependency, or whose dependency step
     * has already reached DONE — the entire "handle dependencies between steps"
     * mechanism (design/components/05-generation-pipeline.md).
     */
    @Query("""
        SELECT j FROM JobStep j
        WHERE j.status = com.learnify.pipeline.JobStatus.PENDING
        AND (j.dependsOnStepId IS NULL OR EXISTS (
            SELECT 1 FROM JobStep d WHERE d.id = j.dependsOnStepId AND d.status = com.learnify.pipeline.JobStatus.DONE
        ))
        ORDER BY j.createdAt ASC
        """)
    List<JobStep> findRunnableSteps();

    List<JobStep> findByPipelineRunIdOrderByCreatedAt(UUID pipelineRunId);

    List<JobStep> findByStatusOrderByCreatedAtDesc(JobStatus status);

    boolean existsByPipelineRunIdAndStatusIn(UUID pipelineRunId, List<JobStatus> statuses);

    long countByStatusInAndCreatedAtLessThan(List<JobStatus> statuses, Instant createdAt);
}
