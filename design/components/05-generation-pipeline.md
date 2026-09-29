# 5. Generation Pipeline & Job Queue

## Purpose
Orchestrate the multi-step course generation (outline → per-lesson content → per-lesson enrichment) as visible, retryable, DB-backed state, with each step's actual logic written as a **portable, independently-deployable unit** (see `BACKEND_PLAN.md` §8) — the queue table is the control plane, the step logic is the execution unit, and those two are deliberately decoupled.

## Key Classes / Packages
- `pipeline/PipelineRun`, `pipeline/JobStep` — entities (see §2 for schema).
- `pipeline/JobStepType` — enum: `OUTLINE`, `LESSON_CONTENT`, `ENRICHMENT`.
- `pipeline/StepFunction<I, O>` — each step's logic is a **Spring Cloud Function** (`java.util.function.Function<I, O>`), one per `JobStepType` (`OutlineFunction`, `LessonContentFunction`, `EnrichmentFunction`). Written as plain functions with no dependency on how they're invoked — that's what makes them independently unit-testable *and* independently deployable.
- `pipeline/PipelinePoller` — `@Scheduled` job (e.g. every 2-3s), picks up `PENDING` steps whose `dependsOnStepId` is `DONE` (or null), and **invokes the matching `StepFunction` in-process** via a `Map<JobStepType, Function<?,?>>` — this is the "run everything inside the one free-tier Spring Boot service" mode we're using now.
- `pipeline/PipelineOrchestrator` — creates a `PipelineRun` + first `JobStep` on `POST /courses/generate`; each `StepFunction`'s result, on success, is used by the orchestrator/poller to enqueue the downstream step(s) with `input` built from that output.
- `pipeline/RetryPolicy` — on a function throwing, increments `JobStep.attempt`, appends the error to the next attempt's input (so the AI-generation retry in §4 can self-correct), re-enqueues up to `maxAttempts`, else marks `FAILED`.

**Deployment flexibility (the actual point of using Spring Cloud Function here):** because `OutlineFunction`/`LessonContentFunction`/`EnrichmentFunction` are plain `Function<I,O>` implementations with no framework glue baked in, the exact same classes can later be deployed as standalone AWS Lambda functions via Spring Cloud Function's adapter — true serverless-per-step — without touching their internals. Today they're called directly by `PipelinePoller` to stay free-tier-simple; that invocation path is the only thing that would change.

## Data Flow (dependency chaining, concretely)
1. `OutlineFunction` finishes → poller writes its output (course + modules + lesson titles) to `JobStep.output` → for each lesson title, inserts a new `JobStep(type=LESSON_CONTENT, dependsOnStepId=<outline step id>, input={lessonTitle, moduleTitle, courseTitle})`.
2. Poller only ever selects steps where the dependency step (if any) has `status = DONE` — this is the entire "how do we handle dependencies between requests" mechanism, no separate orchestration engine needed.
3. `LessonContentFunction` finishes → poller writes lesson content to `JobStep.output` *and* persists it via §2 → enqueues one `ENRICHMENT` step depending on itself.
4. `EnrichmentFunction` calls §6, poller updates the lesson row, marks the step `DONE`.
5. When no `PENDING`/`RUNNING` steps remain for a `PipelineRun`, it's marked `DONE`.

## Queue Position Query
```sql
SELECT COUNT(*) FROM job_step
WHERE status IN ('PENDING','RUNNING') AND created_at < :thisStepCreatedAt;
```
Exposed via `GET /api/v1/jobs/{pipelineRunId}/status`.

## Depends On
Persistence (§2) for all state; AI Content Generation (§4) and External Enrichment (§6) as the actual step function implementations; Rate Limiting (§8) gates *creating* a new `PipelineRun` in the first place.

## Extensibility Notes
- New step type = new `JobStepType` enum value + one new `Function<I,O>` — the poller and dependency mechanism don't change.
- Because state is just rows in a table, this survives a service restart (in-flight jobs just get picked back up by the poller) without needing an external queue broker.
- Because each step is a Spring Cloud Function, moving any individual step (e.g. just `EnrichmentFunction`, which calls slow external APIs) to run as a real deployed serverless function later is a per-step decision, not an all-or-nothing platform migration.
