package com.learnify.pipeline;

import com.learnify.entity.JobStep;
import com.learnify.repository.JobStepRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * The control-plane half of the pipeline (BACKEND_PLAN.md §8): periodically picks up
 * runnable JobSteps and hands each to JobStepProcessor, which invokes the matching
 * Function<I,O> in-process.
 */
@Component
public class PipelinePoller {

    private final JobStepRepository jobStepRepository;
    private final JobStepProcessor jobStepProcessor;

    public PipelinePoller(JobStepRepository jobStepRepository, JobStepProcessor jobStepProcessor) {
        this.jobStepRepository = jobStepRepository;
        this.jobStepProcessor = jobStepProcessor;
    }

    @Scheduled(fixedDelayString = "${pipeline.poll-interval-ms:2000}")
    public void pollAndProcess() {
        for (JobStep step : jobStepRepository.findRunnableSteps()) {
            jobStepProcessor.process(step.getId());
        }
    }
}
