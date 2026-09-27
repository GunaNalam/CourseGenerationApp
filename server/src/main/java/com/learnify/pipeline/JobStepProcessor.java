package com.learnify.pipeline;

import com.learnify.ai.AiGenerationException;
import com.learnify.ai.GeneratedCourse;
import com.learnify.ai.GeneratedLesson;
import com.learnify.ai.GeneratedModule;
import com.learnify.apikey.ApiKeyResolver;
import com.learnify.entity.Course;
import com.learnify.entity.JobStep;
import com.learnify.entity.Lesson;
import com.learnify.entity.Module;
import com.learnify.entity.PipelineRun;
import com.learnify.repository.CourseRepository;
import com.learnify.repository.JobStepRepository;
import com.learnify.repository.LessonRepository;
import com.learnify.repository.ModuleRepository;
import com.learnify.repository.PipelineRunRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs one JobStep to completion (or failure). Kept as its own bean, separate from
 * PipelinePoller's @Scheduled loop, so @Transactional actually applies — Spring's
 * self-invocation rule means a scheduled method calling a @Transactional method on
 * the *same* bean would silently skip the proxy.
 */
@Component
public class JobStepProcessor {

    private static final Logger log = LoggerFactory.getLogger(JobStepProcessor.class);

    private final JobStepRepository jobStepRepository;
    private final PipelineRunRepository pipelineRunRepository;
    private final CourseRepository courseRepository;
    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final OutlineFunction outlineFunction;
    private final LessonContentFunction lessonContentFunction;
    private final EnrichmentFunction enrichmentFunction;
    private final RetryPolicy retryPolicy;
    private final ApiKeyResolver apiKeyResolver;

    public JobStepProcessor(
        JobStepRepository jobStepRepository,
        PipelineRunRepository pipelineRunRepository,
        CourseRepository courseRepository,
        ModuleRepository moduleRepository,
        LessonRepository lessonRepository,
        OutlineFunction outlineFunction,
        LessonContentFunction lessonContentFunction,
        EnrichmentFunction enrichmentFunction,
        RetryPolicy retryPolicy,
        ApiKeyResolver apiKeyResolver
    ) {
        this.jobStepRepository = jobStepRepository;
        this.pipelineRunRepository = pipelineRunRepository;
        this.courseRepository = courseRepository;
        this.moduleRepository = moduleRepository;
        this.lessonRepository = lessonRepository;
        this.outlineFunction = outlineFunction;
        this.lessonContentFunction = lessonContentFunction;
        this.enrichmentFunction = enrichmentFunction;
        this.retryPolicy = retryPolicy;
        this.apiKeyResolver = apiKeyResolver;
    }

    @Transactional
    public void process(UUID stepId) {
        JobStep step = jobStepRepository.findById(stepId).orElse(null);
        if (step == null || step.getStatus() != JobStatus.PENDING) {
            return;
        }

        step.setStatus(JobStatus.RUNNING);
        jobStepRepository.save(step);
        log.info("JobStep {} ({}) started for pipeline run {}", step.getId(), step.getType(), step.getPipelineRunId());

        try {
            switch (step.getType()) {
                case OUTLINE -> handleOutline(step);
                case LESSON_CONTENT -> handleLessonContent(step);
                case ENRICHMENT -> handleEnrichment(step);
            }
            step.setStatus(JobStatus.DONE);
            jobStepRepository.save(step);
            log.info("JobStep {} ({}) completed", step.getId(), step.getType());
            completeRunIfFinished(step.getPipelineRunId());
        } catch (RuntimeException e) {
            log.error("JobStep {} ({}) failed on attempt {}: {}", step.getId(), step.getType(), step.getAttempt() + 1, e.getMessage(), e);
            retryPolicy.handleFailure(step, e);
        }
    }

    private void handleOutline(JobStep step) {
        String topic = (String) step.getInput().get("topic");
        String retryHint = (String) step.getInput().get("previousError");

        PipelineRun run = pipelineRunRepository.findById(step.getPipelineRunId()).orElseThrow();
        Map<String, Object> output = new HashMap<>();
        ApiKeyResolver.ResolvedKey resolvedKey = apiKeyResolver.resolve(run.getOwnerId());

        GeneratedCourse generated;
        try {
            generated = outlineFunction.apply(new OutlineFunction.Input(topic, retryHint, resolvedKey.value())).course();
        } catch (AiGenerationException e) {
            generated = retryWithDefaultKeyOnUserKeyFailure(resolvedKey, output, e,
                key -> outlineFunction.apply(new OutlineFunction.Input(topic, retryHint, key)).course());
        }

        Course course = new Course();
        course.setTitle(generated.title());
        course.setDescription(generated.description());
        course.setOwnerId(run.getOwnerId());
        if (generated.tags() != null) {
            course.setTags(new ArrayList<>(generated.tags()));
        }
        course = courseRepository.save(course);

        run.setCourseId(course.getId());
        pipelineRunRepository.save(run);

        int moduleIndex = 0;
        for (GeneratedModule generatedModule : generated.modules()) {
            Module module = new Module();
            module.setTitle(generatedModule.title());
            module.setOrderIndex(moduleIndex++);
            module.setCourse(course);
            module = moduleRepository.save(module);

            int lessonIndex = 0;
            for (String lessonTitle : generatedModule.lessons()) {
                Lesson lesson = new Lesson();
                lesson.setTitle(lessonTitle);
                lesson.setOrderIndex(lessonIndex++);
                lesson.setModule(module);
                lesson = lessonRepository.save(lesson);

                enqueueLessonContentStep(step, lesson, course.getTitle(), module.getTitle());
            }
        }

        output.put("courseId", course.getId().toString());
        step.setOutput(output);
    }

    private void enqueueLessonContentStep(JobStep outlineStep, Lesson lesson, String courseTitle, String moduleTitle) {
        Map<String, Object> input = new HashMap<>();
        input.put("lessonId", lesson.getId().toString());
        input.put("courseTitle", courseTitle);
        input.put("moduleTitle", moduleTitle);
        input.put("lessonTitle", lesson.getTitle());

        JobStep lessonStep = new JobStep();
        lessonStep.setPipelineRunId(outlineStep.getPipelineRunId());
        lessonStep.setType(JobStepType.LESSON_CONTENT);
        lessonStep.setStatus(JobStatus.PENDING);
        lessonStep.setDependsOnStepId(outlineStep.getId());
        lessonStep.setInput(input);
        jobStepRepository.save(lessonStep);
    }

    private void handleLessonContent(JobStep step) {
        Map<String, Object> input = step.getInput();
        UUID lessonId = UUID.fromString((String) input.get("lessonId"));
        String courseTitle = (String) input.get("courseTitle");
        String moduleTitle = (String) input.get("moduleTitle");
        String lessonTitle = (String) input.get("lessonTitle");
        String retryHint = (String) input.get("previousError");

        PipelineRun run = pipelineRunRepository.findById(step.getPipelineRunId()).orElseThrow();
        Map<String, Object> output = new HashMap<>();
        ApiKeyResolver.ResolvedKey resolvedKey = apiKeyResolver.resolve(run.getOwnerId());

        GeneratedLesson generated;
        try {
            generated = lessonContentFunction
                .apply(new LessonContentFunction.Input(courseTitle, moduleTitle, lessonTitle, retryHint, resolvedKey.value()))
                .lesson();
        } catch (AiGenerationException e) {
            generated = retryWithDefaultKeyOnUserKeyFailure(resolvedKey, output, e,
                key -> lessonContentFunction
                    .apply(new LessonContentFunction.Input(courseTitle, moduleTitle, lessonTitle, retryHint, key))
                    .lesson());
        }

        Lesson lesson = lessonRepository.findById(lessonId).orElseThrow();
        lesson.setObjectives(new ArrayList<>(generated.objectives()));
        lesson.setContent(new ArrayList<>(generated.content()));
        lessonRepository.save(lesson);

        Map<String, Object> enrichmentInput = new HashMap<>();
        enrichmentInput.put("lessonId", lessonId.toString());

        JobStep enrichmentStep = new JobStep();
        enrichmentStep.setPipelineRunId(step.getPipelineRunId());
        enrichmentStep.setType(JobStepType.ENRICHMENT);
        enrichmentStep.setStatus(JobStatus.PENDING);
        enrichmentStep.setDependsOnStepId(step.getId());
        enrichmentStep.setInput(enrichmentInput);
        jobStepRepository.save(enrichmentStep);

        output.put("lessonId", lessonId.toString());
        step.setOutput(output);
    }

    private void handleEnrichment(JobStep step) {
        UUID lessonId = UUID.fromString((String) step.getInput().get("lessonId"));
        Lesson lesson = lessonRepository.findById(lessonId).orElseThrow();

        String videoQuery = findVideoBlockQuery(lesson.getContent());
        if (videoQuery != null) {
            String videoId = enrichmentFunction.apply(new EnrichmentFunction.Input(videoQuery)).videoId();
            if (videoId != null) {
                attachVideoId(lesson.getContent(), videoId);
            }
        }

        lesson.setEnriched(true);
        lessonRepository.save(lesson);

        step.setOutput(Map.of("lessonId", lessonId.toString()));
    }

    private String findVideoBlockQuery(List<Map<String, Object>> content) {
        for (Map<String, Object> block : content) {
            if ("video".equalsIgnoreCase(String.valueOf(block.get("type")))) {
                Object query = block.get("query");
                return query == null ? null : query.toString();
            }
        }
        return null;
    }

    private void attachVideoId(List<Map<String, Object>> content, String videoId) {
        for (Map<String, Object> block : content) {
            if ("video".equalsIgnoreCase(String.valueOf(block.get("type")))) {
                block.put("videoId", videoId);
                block.put("embedUrl", "https://www.youtube.com/embed/" + videoId);
            }
        }
    }

    /**
     * If the resolved key was the user's own and the AI call failed, retry once with
     * the default key and flag it — rather than silently substituting (BACKEND_PLAN.md
     * Extendability #1). A non-USER-key failure (or a second failure) is rethrown so
     * RetryPolicy's normal attempt-counting handles it.
     */
    private <T> T retryWithDefaultKeyOnUserKeyFailure(
        ApiKeyResolver.ResolvedKey resolvedKey,
        Map<String, Object> output,
        AiGenerationException original,
        java.util.function.Function<String, T> attempt
    ) {
        if (resolvedKey.source() != ApiKeyResolver.Source.USER) {
            throw original;
        }
        output.put("usingDefaultKey", true);
        output.put("keyFallbackReason", "your API key failed: " + original.getMessage());
        return attempt.apply(apiKeyResolver.defaultKey());
    }

    private void completeRunIfFinished(UUID pipelineRunId) {
        boolean stillActive = jobStepRepository.existsByPipelineRunIdAndStatusIn(
            pipelineRunId, List.of(JobStatus.PENDING, JobStatus.RUNNING)
        );
        if (!stillActive) {
            PipelineRun run = pipelineRunRepository.findById(pipelineRunId).orElseThrow();
            if (run.getStatus() != JobStatus.FAILED && run.getStatus() != JobStatus.CANCELLED) {
                run.setStatus(JobStatus.DONE);
                pipelineRunRepository.save(run);
                log.info("Pipeline run {} completed (courseId={})", run.getId(), run.getCourseId());
            }
        }
    }
}
