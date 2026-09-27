package com.learnify.controller;

import com.learnify.dto.request.GenerateCourseRequest;
import com.learnify.dto.response.GenerateCourseResponse;
import com.learnify.dto.response.JobStatusResponse;
import com.learnify.entity.JobStep;
import com.learnify.entity.PipelineRun;
import com.learnify.exception.NotFoundException;
import com.learnify.pipeline.JobStatus;
import com.learnify.pipeline.PipelineOrchestrator;
import com.learnify.repository.JobStepRepository;
import com.learnify.repository.PipelineRunRepository;
import com.learnify.security.CurrentUserProvider;
import jakarta.validation.Valid;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class JobController {

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

    @PostMapping("/courses/generate")
    public ResponseEntity<GenerateCourseResponse> generate(@Valid @RequestBody GenerateCourseRequest request) {
        UUID pipelineRunId = pipelineOrchestrator.startGeneration(currentUserProvider.currentUserId(), request.topic());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new GenerateCourseResponse(pipelineRunId));
    }

    @GetMapping("/jobs/{pipelineRunId}/status")
    public JobStatusResponse status(@PathVariable UUID pipelineRunId) {
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

        String currentStep = oldestActive == null ? null : oldestActive.getType().name();

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

        return new JobStatusResponse(
            run.getId(), run.getStatus().name(), currentStep, position, run.getCourseId(), error,
            usingDefaultKey, keyFallbackReason
        );
    }
}
