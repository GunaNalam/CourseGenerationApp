package com.learnify.pipeline;

import com.learnify.entity.JobStep;
import com.learnify.entity.PipelineRun;
import com.learnify.ratelimit.SlidingWindowRateLimiter;
import com.learnify.repository.JobStepRepository;
import com.learnify.repository.PipelineRunRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PipelineOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(PipelineOrchestrator.class);

    private final PipelineRunRepository pipelineRunRepository;
    private final JobStepRepository jobStepRepository;
    private final SlidingWindowRateLimiter rateLimiter;

    public PipelineOrchestrator(
        PipelineRunRepository pipelineRunRepository,
        JobStepRepository jobStepRepository,
        SlidingWindowRateLimiter rateLimiter
    ) {
        this.pipelineRunRepository = pipelineRunRepository;
        this.jobStepRepository = jobStepRepository;
        this.rateLimiter = rateLimiter;
    }

    public UUID startGeneration(UUID ownerId, String topic) {
        rateLimiter.checkAndRecord(ownerId);

        PipelineRun run = new PipelineRun();
        run.setOwnerId(ownerId);
        run.setStatus(JobStatus.PENDING);
        run = pipelineRunRepository.save(run);

        Map<String, Object> input = new HashMap<>();
        input.put("topic", topic);

        JobStep outline = new JobStep();
        outline.setPipelineRunId(run.getId());
        outline.setType(JobStepType.OUTLINE);
        outline.setStatus(JobStatus.PENDING);
        outline.setInput(input);
        jobStepRepository.save(outline);

        log.info("Started pipeline run {} for owner {} with topic \"{}\"", run.getId(), ownerId, topic);
        return run.getId();
    }
}
