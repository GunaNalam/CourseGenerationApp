package com.learnify.repository;

import com.learnify.entity.PipelineRun;
import com.learnify.pipeline.JobStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PipelineRunRepository extends JpaRepository<PipelineRun, UUID> {

    Optional<PipelineRun> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<PipelineRun> findAllByOrderByCreatedAtDesc();

    List<PipelineRun> findAllByStatusIn(List<JobStatus> statuses);

    long countByStatus(JobStatus status);
}
