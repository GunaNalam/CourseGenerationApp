package com.learnify.pipeline;

import com.learnify.entity.JobStep;
import com.learnify.entity.PipelineRun;
import com.learnify.repository.JobStepRepository;
import com.learnify.repository.PipelineRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RetryPolicy {

    private static final Logger log = LoggerFactory.getLogger(RetryPolicy.class);
    private static final int MAX_ATTEMPTS = 3;

    private final JobStepRepository jobStepRepository;
    private final PipelineRunRepository pipelineRunRepository;

    public RetryPolicy(JobStepRepository jobStepRepository, PipelineRunRepository pipelineRunRepository) {
        this.jobStepRepository = jobStepRepository;
        this.pipelineRunRepository = pipelineRunRepository;
    }

    public void handleFailure(JobStep step, Exception error) {
        step.setAttempt(step.getAttempt() + 1);
        step.setError(error.getMessage());

        boolean quotaExhausted = error.getMessage() != null && error.getMessage().contains("HTTP 429");

        if (quotaExhausted || step.getAttempt() >= MAX_ATTEMPTS) {
            step.setStatus(JobStatus.FAILED);
            jobStepRepository.save(step);
            if (quotaExhausted) {
                log.warn("JobStep {} ({}) hit a Gemini quota limit (HTTP 429), failing fast without retrying: {}",
                    step.getId(), step.getType(), error.getMessage());
            } else {
                log.warn("JobStep {} ({}) exhausted {} attempts, failing pipeline run {}",
                    step.getId(), step.getType(), MAX_ATTEMPTS, step.getPipelineRunId());
            }
            failRun(step.getPipelineRunId(), error.getMessage());
        } else {
            step.getInput().put("previousError", error.getMessage());
            step.setStatus(JobStatus.PENDING);
            jobStepRepository.save(step);
            log.info("JobStep {} ({}) re-enqueued for attempt {}/{}", step.getId(), step.getType(), step.getAttempt() + 1, MAX_ATTEMPTS);
        }
    }

    private void failRun(java.util.UUID pipelineRunId, String message) {
        PipelineRun run = pipelineRunRepository.findById(pipelineRunId).orElseThrow();
        run.setStatus(JobStatus.FAILED);
        pipelineRunRepository.save(run);

        for (JobStep sibling : jobStepRepository.findByPipelineRunIdOrderByCreatedAt(pipelineRunId)) {
            if (sibling.getStatus() == JobStatus.PENDING || sibling.getStatus() == JobStatus.RUNNING) {
                sibling.setStatus(JobStatus.CANCELLED);
                sibling.setError("Cancelled: sibling step in this pipeline run failed");
                jobStepRepository.save(sibling);
            }
        }
    }
}
