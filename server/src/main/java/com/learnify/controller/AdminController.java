package com.learnify.controller;

import com.learnify.admin.AdminAccessDeniedException;
import com.learnify.admin.AdminAuthorizer;
import com.learnify.dto.response.AdminStatsResponse;
import com.learnify.dto.response.JobErrorSummary;
import com.learnify.dto.response.PipelineRunSummary;
import com.learnify.entity.JobStep;
import com.learnify.entity.PipelineRun;
import com.learnify.pipeline.JobStatus;
import com.learnify.repository.JobStepRepository;
import com.learnify.repository.PipelineRunRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

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

    @GetMapping("/jobs")
    public List<PipelineRunSummary> jobs(@AuthenticationPrincipal Jwt jwt) {
        requireAdmin(jwt);
        return pipelineRunRepository.findAllByOrderByCreatedAtDesc().stream()
            .limit(50)
            .map(PipelineRunSummary::from)
            .toList();
    }

    @GetMapping("/errors")
    public List<JobErrorSummary> errors(@AuthenticationPrincipal Jwt jwt) {
        requireAdmin(jwt);
        return jobStepRepository.findByStatusOrderByCreatedAtDesc(JobStatus.FAILED).stream()
            .limit(50)
            .map(JobErrorSummary::from)
            .toList();
    }

    @GetMapping("/stats")
    public AdminStatsResponse stats(@AuthenticationPrincipal Jwt jwt) {
        requireAdmin(jwt);
        return new AdminStatsResponse(
            pipelineRunRepository.count(),
            pipelineRunRepository.countByStatus(JobStatus.DONE),
            pipelineRunRepository.countByStatus(JobStatus.FAILED)
        );
    }

    @PostMapping("/jobs/{pipelineRunId}/cancel")
    @Transactional
    public ResponseEntity<Void> cancelJob(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID pipelineRunId) {
        requireAdmin(jwt);
        cancelRun(pipelineRunId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/jobs/cancel-all")
    @Transactional
    public int cancelAllActiveJobs(@AuthenticationPrincipal Jwt jwt) {
        requireAdmin(jwt);
        List<PipelineRun> active = pipelineRunRepository.findAllByStatusIn(List.of(JobStatus.PENDING, JobStatus.RUNNING));
        active.forEach(run -> cancelRun(run.getId()));
        return active.size();
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

    private void requireAdmin(Jwt jwt) {
        if (jwt == null || !adminAuthorizer.isAdmin(jwt.getSubject())) {
            throw new AdminAccessDeniedException("Admin access required");
        }
    }
}
