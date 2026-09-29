package com.learnify.controller;

import com.learnify.admin.AdminAccessDeniedException;
import com.learnify.admin.AdminAuthorizer;
import com.learnify.api.AdminApi;
import com.learnify.api.model.AdminStatsResponse;
import com.learnify.api.model.JobErrorSummary;
import com.learnify.api.model.PipelineRunSummary;
import com.learnify.entity.JobStep;
import com.learnify.entity.PipelineRun;
import com.learnify.pipeline.JobStatus;
import com.learnify.repository.JobStepRepository;
import com.learnify.repository.PipelineRunRepository;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AdminController implements AdminApi {

    private final PipelineRunRepository pipelineRunRepository;
    private final JobStepRepository jobStepRepository;
    private final AdminAuthorizer adminAuthorizer;

    public AdminController(
        PipelineRunRepository pipelineRunRepository,
        JobStepRepository jobStepRepository,
        AdminAuthorizer adminAuthorizer
    ) {
        this.pipelineRunRepository = pipelineRunRepository;
        this.jobStepRepository = jobStepRepository;
        this.adminAuthorizer = adminAuthorizer;
    }

    @Override
    public ResponseEntity<List<PipelineRunSummary>> listAdminJobs() {
        requireAdmin();
        var responses = pipelineRunRepository.findAllByOrderByCreatedAtDesc().stream()
            .limit(50)
            .map(this::toSummary)
            .toList();
        return ResponseEntity.ok(responses);
    }

    @Override
    public ResponseEntity<List<JobErrorSummary>> listAdminErrors() {
        requireAdmin();
        var responses = jobStepRepository.findByStatusOrderByCreatedAtDesc(JobStatus.FAILED).stream()
            .limit(50)
            .map(this::toErrorSummary)
            .toList();
        return ResponseEntity.ok(responses);
    }

    @Override
    public ResponseEntity<AdminStatsResponse> getAdminStats() {
        requireAdmin();
        var response = new AdminStatsResponse(
            pipelineRunRepository.count(),
            pipelineRunRepository.countByStatus(JobStatus.DONE),
            pipelineRunRepository.countByStatus(JobStatus.FAILED)
        );
        return ResponseEntity.ok(response);
    }

    @Override
    @Transactional
    public ResponseEntity<Void> cancelAdminJob(UUID pipelineRunId) {
        requireAdmin();
        cancelRun(pipelineRunId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @Transactional
    public ResponseEntity<Integer> cancelAllAdminJobs() {
        requireAdmin();
        List<PipelineRun> active = pipelineRunRepository.findAllByStatusIn(List.of(JobStatus.PENDING, JobStatus.RUNNING));
        active.forEach(run -> cancelRun(run.getId()));
        return ResponseEntity.ok(active.size());
    }

    private void cancelRun(UUID pipelineRunId) {
        PipelineRun run = pipelineRunRepository.findById(pipelineRunId).orElseThrow();
        run.setStatus(JobStatus.CANCELLED);
        pipelineRunRepository.save(run);

        for (JobStep step : jobStepRepository.findByPipelineRunIdOrderByCreatedAt(pipelineRunId)) {
            if (step.getStatus() == JobStatus.PENDING || step.getStatus() == JobStatus.RUNNING) {
                step.setStatus(JobStatus.CANCELLED);
                step.setError("Cancelled by admin");
                jobStepRepository.save(step);
            }
        }
    }

    private PipelineRunSummary toSummary(PipelineRun run) {
        return new PipelineRunSummary(
            run.getId(), run.getOwnerId(), com.learnify.api.model.JobStatus.valueOf(run.getStatus().name()),
            run.getCourseId(), run.getCreatedAt().atOffset(ZoneOffset.UTC)
        );
    }

    private JobErrorSummary toErrorSummary(JobStep step) {
        return new JobErrorSummary(
            step.getId(), step.getPipelineRunId(), com.learnify.api.model.JobStepType.valueOf(step.getType().name()),
            step.getError(), step.getAttempt(), step.getCreatedAt().atOffset(ZoneOffset.UTC)
        );
    }

    private void requireAdmin() {
        Jwt jwt = currentJwt();
        if (jwt == null || !adminAuthorizer.isAdmin(jwt.getSubject())) {
            throw new AdminAccessDeniedException("Admin access required");
        }
    }

    /**
     * The generated AdminApi interface methods take no parameters (an OpenAPI spec has no way to
     * express "inject the resolved JWT principal"), so this replaces the old @AuthenticationPrincipal
     * Jwt parameter - same JWT, same claims, just read from the security context instead of the
     * method signature.
     */
    private Jwt currentJwt() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            return jwtAuthenticationToken.getToken();
        }
        return null;
    }
}
