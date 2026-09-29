package com.learnify.controller;

import com.learnify.api.JobsApi;
import com.learnify.api.model.GenerateCourseRequest;
import com.learnify.api.model.GenerateCourseResponse;
import com.learnify.api.model.JobStatusResponse;
import com.learnify.entity.JobStep;
import com.learnify.entity.PipelineRun;
import com.learnify.exception.NotFoundException;
import com.learnify.pipeline.JobStatus;
import com.learnify.pipeline.PipelineOrchestrator;
import com.learnify.repository.JobStepRepository;
import com.learnify.repository.PipelineRunRepository;
import com.learnify.security.CurrentUserProvider;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class JobController implements JobsApi {

    private static final List<JobStatus> ACTIVE_STATUSES = List.of(JobStatus.PENDING, JobStatus.RUNNING);

    private final PipelineOrchestrator pipelineOrchestrator;
    private final PipelineRunRepository pipelineRunRepository;
    private final JobStepRepository jobStepRepository;
    private final CurrentUserProvider currentUserProvider;

    public JobController(
        PipelineOrchestrator pipelineOrchestrator,
        PipelineRunRepository pipelineRunRepository,
        JobStepRepository jobStepRepository,
        CurrentUserProvider currentUserProvider
    ) {
        this.pipelineOrchestrator = pipelineOrchestrator;
        this.pipelineRunRepository = pipelineRunRepository;
        this.jobStepRepository = jobStepRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public ResponseEntity<GenerateCourseResponse> generateCourse(GenerateCourseRequest generateCourseRequest) {
        UUID pipelineRunId = pipelineOrchestrator.startGeneration(currentUserProvider.currentUserId(), generateCourseRequest.getTopic());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new GenerateCourseResponse(pipelineRunId));
    }

    @Override
    public ResponseEntity<JobStatusResponse> getJobStatus(UUID pipelineRunId) {
        PipelineRun run = pipelineRunRepository.findByIdAndOwnerId(pipelineRunId, currentUserProvider.currentUserId())
            .orElseThrow(() -> new NotFoundException("Pipeline run not found: " + pipelineRunId));

        List<JobStep> steps = jobStepRepository.findByPipelineRunIdOrderByCreatedAt(pipelineRunId);

        JobStep oldestActive = steps.stream()
            .filter(s -> ACTIVE_STATUSES.contains(s.getStatus()))
            .min(Comparator.comparing(JobStep::getCreatedAt))
            .orElse(null);

        long position = oldestActive == null
            ? 0
            : jobStepRepository.countByStatusInAndCreatedAtLessThan(ACTIVE_STATUSES, oldestActive.getCreatedAt());

        var currentStep = oldestActive == null
            ? null
            : com.learnify.api.model.JobStepType.valueOf(oldestActive.getType().name());

        String error = steps.stream()
            .filter(s -> s.getStatus() == JobStatus.FAILED || s.getStatus() == JobStatus.CANCELLED)
            .map(JobStep::getError)
            .findFirst()
            .orElse(null);

        boolean usingDefaultKey = steps.stream()
            .anyMatch(s -> Boolean.TRUE.equals(s.getOutput().get("usingDefaultKey")));
        String keyFallbackReason = steps.stream()
            .map(s -> (String) s.getOutput().get("keyFallbackReason"))
            .filter(java.util.Objects::nonNull)
            .findFirst()
            .orElse(null);

        var response = new JobStatusResponse(
            run.getId(),
            com.learnify.api.model.JobStatus.valueOf(run.getStatus().name()),
            currentStep,
            position,
            run.getCourseId(),
            error,
            usingDefaultKey,
            keyFallbackReason
        );

        return ResponseEntity.ok(response);
    }
}
