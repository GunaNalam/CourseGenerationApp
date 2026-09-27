# High-Level Design

## Components

```
                                   ┌─────────────────────┐
                                   │   Frontend (React)   │
                                   │  Vercel, /client      │
                                   └──────────┬───────────┘
                                              │ REST (/api/v1), JWT
                                              ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                     Backend — Spring Boot, single service (Render)         │
│                                                                              │
│  ┌────────────────┐   ┌──────────────────┐   ┌──────────────────────┐     │
│  │  API Layer      │──▶│  Auth & Security  │   │  API Key Management   │     │
│  │  (controllers,  │   │  (Auth0 / JWT,    │◀──│  (BYO key, fallback,  │     │
│  │  DTOs, OpenAPI) │   │  ownerId context) │   │  encryption)           │     │
│  └────────┬────────┘   └──────────────────┘   └───────────┬────────────┘     │
│           │                                                 │ used by         │
│           ▼                                                 ▼                 │
│  ┌────────────────────┐      enqueues       ┌──────────────────────────┐     │
│  │  Rate Limiting       │────────────────────▶│ Generation Pipeline &     │     │
│  │  (sliding window)    │                     │ Job Queue (poller +       │     │
│  └──────────────────────┘                     │ per-step functions)       │     │
│                                                └──────┬───────────┬───────┘     │
│                                                        │           │             │
│                                     step: OUTLINE/     │           │ step:       │
│                                     LESSON_CONTENT      ▼           │ ENRICHMENT  │
│                                     ┌──────────────────────────┐    ▼             │
│                                     │  AI Content Generation    │  ┌────────────┐ │
│                                     │  (prompt templates,       │  │ External    │ │
│                                     │  Gemini client, JSON       │  │ Enrichment  │ │
│                                     │  validation + retry)       │  │ (YouTube,   │ │
│                                     └──────────────┬─────────────┘  │ TTS/xlate)  │ │
│                                                     │                └─────┬──────┘ │
│                                                     ▼                      │        │
│                                     ┌───────────────────────────────────────┐      │
│                                     │            Persistence Layer            │      │
│                                     │  (JPA repositories → PostgreSQL,        │      │
│                                     │   JSONB for lesson content)             │      │
│                                     └───────────────────────────────────────┘      │
│                                                                                       │
│  ┌──────────────────────┐   ┌──────────────────────┐                              │
│  │ Admin & Observability │   │ Course Export/Import  │                              │
│  │ (reads job + error     │   │ (reads persistence)    │                              │
│  │  state, admin-only)    │   └──────────────────────┘                              │
│  └──────────────────────┘                                                          │
└───────────────────────────────────────────────────────────────────────────┘
                        │
                        ▼  free-tier managed Postgres (Neon/Supabase/Railway)
```

`PDF Export` isn't in this diagram — it's primarily a frontend concern (see [10-pdf-export.md](./components/10-pdf-export.md)); the backend's only job is making sure content coming out of Persistence is clean enough to render into a PDF client-side.

## Request Flow — "Generate a course from a topic prompt"

1. **Frontend** sends `POST /api/v1/courses/generate { topic }` with a Bearer JWT.
2. **API Layer** validates the DTO, delegates to Auth & Security to resolve `ownerId` from the token.
3. **Rate Limiting** checks the sliding window for this `ownerId`; rejects with `429` if over quota, otherwise proceeds.
4. **API Key Management** resolves which Gemini key to use for this user (their own, or the default) and attaches it to the request context.
5. **Generation Pipeline** creates a `PipelineRun` + first `JobStep` (`OUTLINE`, status `PENDING`) and returns `{ pipelineRunId }` to the frontend immediately (HTTP 202).
6. Frontend polls `GET /api/v1/jobs/{pipelineRunId}/status` for `{status, position, currentStep}`.
7. The pipeline's poller picks up `PENDING` steps whose dependencies are satisfied, in order:
   - `OUTLINE` → **AI Content Generation** builds the course prompt, calls Gemini, validates/repairs the JSON, persists the course + module + lesson-title skeleton via **Persistence**, marks step `DONE`, enqueues one `LESSON_CONTENT` step per lesson.
   - `LESSON_CONTENT` (×N, can run concurrently) → builds the lesson prompt (History → A-Z → Real-World Application shape), calls Gemini, validates, persists lesson content, enqueues its `ENRICHMENT` step.
   - `ENRICHMENT` → **External Enrichment** takes the lesson's `video` query block and calls YouTube search; if Hinglish requested, calls Gemini translation + TTS; persists results back onto the lesson.
8. Once all steps for a `PipelineRun` are `DONE`, status flips to `DONE`; frontend's poll picks that up and navigates to the course view.
9. Any step failure → **§11 retry logic** (from `BACKEND_PLAN.md`) re-enqueues with the error appended to the prompt, up to a cap, then surfaces as `FAILED` with a message.
10. **Admin & Observability** reads the same `generation_job`/`pipeline_run` tables (plus an error log) to render the admin view — it doesn't sit in the request path at all, it's a read-only reporting surface.

## Cross-cutting concerns (not components, but touch everything)

- **Multi-tenancy readiness:** every entity carries `ownerId`; single-user mode just pins it to one system default user.
- **Private by default:** courses/modules/lessons/jobs are only ever visible to their `ownerId`. Identity for this is resolved solely from the verified Auth0 JWT (§3) — never from a client-supplied param — and enforced via owner-scoped repository queries (§2), not post-fetch checks. See [01-api-layer.md](./components/01-api-layer.md) for the full rationale (this is IDOR prevention, not just a style choice).
- **Extensibility seam:** Persistence is accessed only through repository interfaces (never raw JPA queries from services), and AI content blocks are validated against an open, registry-driven `BlockType` set — both documented in their respective component docs.
- **Execution-model flexibility for pipeline steps:** each `JobStep`'s logic is a plain Spring Cloud Function, invoked in-process by the poller today (single free-tier service) but individually deployable as a real serverless function later with no rewrite — see [05-generation-pipeline.md](./components/05-generation-pipeline.md).
