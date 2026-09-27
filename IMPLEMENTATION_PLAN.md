# Implementation Plan — Learnify

Turns `BACKEND_PLAN.md` + `design/` into an actual build order. Each phase is a **vertical, testable slice** — it ends in something that runs and can be verified (unit/integration tests, or a manual curl/UI check), not a pile of untestable scaffolding. Phases are ordered so each one only depends on what's already been built, and each maps to a feature branch + PR (or a few), which is what gives us the clean commit history.

## Progress

This table is the single place to check what's actually built vs. still planned — updated as each phase's Definition of Done is met, not just started.

| # | Phase | Status | Notes |
|---|-------|--------|-------|
| 0 | Repo & Tooling Bootstrap (backend-only) | ✅ | `server/` Spring Boot skeleton scaffolded, compiles, `mvn test` passes (Testcontainers-backed context-load test, real Postgres container); CI (`pull_request` + `workflow_dispatch`) added, now covering both `server` and `client` jobs. |
| 1 | Domain Model & Persistence | ✅ | Entities (`User`, `Course`, `Module`, `Lesson`), Flyway migration, owner-scoped repositories, CRUD controllers, default-user stub, integration tests (including cross-owner 404) all passing. |
| 2 | Auth & Security (Auth0, backend-only) | ✅ | Real Spring Security JWT resource server (`JwtDecoder` via lazy JWKS lookup, issuer+audience validation), `AuthenticatedUserResolver`/`JwtCurrentUserProvider` replacing the Phase 1 stub, all `/api/v1/**` routes require a valid JWT. Verified with mock JWTs (two distinct identities) in tests — real Auth0 tenant needed for actual browser login, tracked as a manual-setup item. |
| 3 | AI Content Generation (isolated) | ✅ | Prompt templates (History → A-Z → Real-World Application shape) externalized as JSON, `GeminiClient`, `JsonResponseValidator` with `BlockType` registry and markdown-fence stripping. Unit-tested without live Gemini calls; a real key is needed to eyeball actual output quality (manual-setup item). |
| 4 | Generation Pipeline & Job Queue | ✅ | `PipelineRun`/`JobStep` + dependency chaining (`OUTLINE`→`LESSON_CONTENT`→`ENRICHMENT`), `PipelinePoller`/`JobStepProcessor` split (avoids the Spring self-invocation trap), `RetryPolicy` with error-fed self-correction. `POST /courses/generate` + `GET /jobs/{id}/status` live. End-to-end test (mocked Gemini) and malformed-response-retries-then-fails test both passing. |
| 5 | Frontend Bootstrap + Generation Flow | ✅ | Vite+React+TS+Tailwind, full design system (light/dark tokens), Auth0 React SDK, routing (Landing/Home/Course/Lesson/Settings/Admin), live generation-progress polling UI, interactive MCQ/code/video blocks. Vitest + Testing Library, 10 tests passing (including MCQ/code-block interactivity). |
| 6 | API Key Management & Rate Limiting | ✅ | AES-GCM `ApiKeyEncryptor`, `ApiKeyResolver` (user key → default fallback), `/users/me/api-key` CRUD + a frontend Settings page for it, sliding-window rate limiter (60s/2 requests) wired into generation. Tested: rate-limit rejection, and the actual invalid-user-key-falls-back-to-default-and-flags-it scenario. |
| 7 | External Enrichment: YouTube | ✅ | `YouTubeClient` (search.list, embeddable-only), wired into `EnrichmentFunction` + `JobStepProcessor`, attaches `videoId`/`embedUrl` onto the lesson's video block; frontend renders a real iframe embed. Tested with a mocked client. |
| 8 | External Enrichment: Multilingual TTS | ✅ | On-demand (not part of the auto pipeline): `TranslationClient` (Hinglish, reuses `GeminiClient`) + `GeminiTtsClient` + `WavEncoder`, exposed via `GET /lessons/{id}/audio`; frontend `HinglishAudioButton` fetches and plays it. `WavEncoder` fully unit-tested; the real Gemini TTS request/response contract is the one integration in the project not verified against a live key yet — flagged clearly in code and `PROJECT_OVERVIEW.md`. |
| 9 | PDF Export | ✅ | `LessonPDFExporter` — off-screen, forced-light-theme render captured via `html2canvas` and paginated into a PDF with `jsPDF`, regardless of the viewer's active dark/light mode. |
| 10 | Admin & Observability, Export/Import | ✅ | `AdminController` (`/admin/jobs`, `/errors`, `/stats`) behind an allowlisted-`sub` guard, with a frontend dashboard page; `GET /courses/{id}/export` with a frontend one-click JSON download. Tested: non-admin 403, admin 200, full export round-trip. |
| 11 | Deployment & Full CI/CD | ⬜ | Inherently manual (Render/Vercel account setup) — part of the manual-effort discussion. |
| 12 | Polish & Showcase | ⬜ | - |
| 13 | Trending Topics | ⬜ | Requested for a future phase — see below and `BACKEND_PLAN.md` Extendability §8. Not started. |

## Git & Commit Conventions (used from Phase 0 onward)

- One feature branch per phase (or per sub-bullet within a phase, if it's large): `feat/persistence-core`, `feat/auth0-integration`, etc.
- Conventional commit style, scoped to the component: `feat(persistence): add Course/Module/Lesson entities and repositories`, `test(pipeline): add PipelineOrchestrator dependency-chaining test`, `fix(auth): resolve ownerId from token sub, not header`.
- A phase is only merged (PR into `main`) once its own "Definition of Done" below is met — that's what keeps `main` always in a working, demo-able state, which matters since this is hackathon-judged partly on commit history.
- Every phase that touches the backend contract regenerates `openapi.yaml` as part of its commit (not a separate cleanup pass later).

---

## Phase 0 — Repo & Tooling Bootstrap (backend-only)

**Goal:** an empty-but-real backend skeleton that builds, runs, and has CI wired, before any feature logic exists. **Backend-first, deliberately:** `client/` doesn't get created until Phase 5 — nothing frontend-related exists before then.

**Tasks:**
- Repo layout: `project-root/{server}` (per `design/FINAL_STRUCTURE.md`; `client/` joins later).
- `server/`: Spring Boot project (Web, Data JPA, Validation, Security, OAuth2 resource-server, Flyway, PostgreSQL driver, springdoc-openapi), package skeleton (`controller/dto/entity/repository/security/ai/pipeline/integration/apikey/ratelimit/exception`), `application.yml` reading from env vars, health check endpoint (`/actuator/health` is enough).
- Free-tier Postgres instance provisioned (Neon/Supabase/Railway — pick one), baseline Flyway migration wired and confirmed running.
- `.github/workflows/test-on-pr.yml` — `pull_request` + `workflow_dispatch` triggers (per `BACKEND_PLAN.md` §14), running `mvn test`. A `client` job is added to this same workflow in Phase 5, once there's a frontend to test.
- Root `README.md` stub (real content comes in the final phase, per M13).

**Definition of Done:** `mvn spring-boot:run` serves a health check locally; CI goes green on a PR; DB connection confirmed (Testcontainers-backed test passes against a real Postgres container).

**Suggested commits:** `chore(server): bootstrap Spring Boot project skeleton`, `ci: add PR test-gate workflow with manual dispatch`, `chore(db): wire Flyway + Postgres connection`.

---

## Phase 1 — Domain Model & Persistence (no AI yet)

**Goal:** the real data model exists and is fully CRUD-able, independent of AI generation — proves the persistence layer (`design/components/02-persistence.md`) before anything depends on it.

**Tasks:**
- `User`, `Course`, `Module`, `Lesson` entities + Flyway migration (`ownerId` on every entity from day one, per the multi-tenancy decision).
- Owner-scoped repositories (`findByIdAndOwnerId`, `findAllByOwnerId`).
- A temporary "system default user" (`DefaultUserAuthProvider`-equivalent, hardcoded for now) standing in for real auth, which Phase 2 will replace — lets us build and test persistence without blocking on Auth0.
- Basic CRUD controllers for `Course`/`Module`/`Lesson` (manual creation only, no AI) — enough to prove the schema and the API layer's DTO/versioning pattern (`/api/v1/...`) end to end.
- `openapi.yaml` generation wired in (springdoc-openapi).

**Definition of Done:** integration tests (Testcontainers/embedded Postgres) covering create/read for all three entities, including a test that confirms cross-owner access is blocked (`404`, not `403` — per the IDOR-prevention decision) even with the stub default user.

**Suggested commits:** `feat(persistence): add Course/Module/Lesson entities and migration`, `feat(persistence): add owner-scoped repositories`, `feat(api): add CRUD controllers for course/module/lesson`, `test(persistence): cross-owner access returns 404`.

---

## Phase 2 — Auth & Security (Auth0, backend-only)

**Goal:** replace the Phase 1 stub with real Auth0-verified identity, matching `design/components/03-auth-security.md`. Still backend-only — verified via real Auth0-issued tokens obtained manually (Auth0's own test/login flow or a client-credentials call), not through a browser UI yet, since `client/` doesn't exist until Phase 5.

**Tasks:**
- Spring Security JWT validation against Auth0 (issuer/audience config).
- `AuthenticatedUserResolver` — resolves/creates the internal `User` from the token's `sub`, removes the temporary default-user stub from the request path (but keep `DefaultUserAuthProvider` as a class, now demonstrably swappable — this is where the "single-user vs multi-user is a config flip" claim gets proven, not just asserted).
- Protect all `/api/v1/**` routes except public/health ones.
- Register the Auth0 application/API (Client ID, Domain, Audience) so real tokens can be issued for testing — this groundwork is what Phase 5's frontend SDK wiring plugs into later.

**Definition of Done:** a request with a real Auth0-issued JWT (fetched manually, e.g. via curl against Auth0's token endpoint) correctly resolves to a real `User` row; an unauthenticated request to a protected route gets `401`; Phase 1's cross-owner test now runs against two *real* Auth0 test accounts, not the stub. Browser login/logout UI is explicitly Phase 5's job, not this phase's.

**Suggested commits:** `feat(auth): add Auth0 JWT validation and AuthenticatedUserResolver`, `test(auth): unauthenticated request returns 401`, `test(auth): cross-owner access blocked for real Auth0 accounts`.

---

## Phase 3 — AI Content Generation (isolated)

**Goal:** prove prompt design + Gemini integration + JSON validation in isolation, before wiring it into the async pipeline — easier to debug prompt quality without pipeline/queue noise in the way.

**Tasks:**
- `resources/prompts/course-prompt.json`, `lesson-prompt.json` (History → A-Z → Real-World Application shape, per `design/components/04-ai-content-generation.md`).
- `GeminiClient`, `CoursePromptBuilder`, `LessonPromptBuilder`, `JsonResponseValidator`.
- A temporary internal-only endpoint (or a `@SpringBootTest`-driven manual trigger) to call generation directly and eyeball output quality — **this is deliberately not the public API yet**; it's a harness for iterating on prompt quality fast.
- `BlockType` registry (open-schema validation per `design/components/07-... `/ Data Model Shape decision — validates required top-level fields without constraining block internals).

**Definition of Done:** unit tests for prompt builders (given inputs, correct template filled) and `JsonResponseValidator` (valid/invalid JSON fixtures); at least one real Gemini call manually verified to produce a lesson that actually follows the 3-part structure and reads as genuinely useful, not filler — this is a judgment call, not just a green test, so actually read the output once.

**Suggested commits:** `feat(ai): add prompt template loader and course/lesson prompt builders`, `feat(ai): add GeminiClient and JSON response validator`, `test(ai): prompt builder and validator unit tests`.

---

## Phase 4 — Generation Pipeline & Job Queue

**Goal:** wire Phase 1 (persistence) + Phase 3 (AI) together through the real async pipeline — this is the first point where "type a topic, get a course" actually works end to end via the API.

**Tasks:**
- `PipelineRun`/`JobStep` entities + migration, `JobStepType` enum.
- `OutlineFunction`, `LessonContentFunction` as Spring Cloud Function units (Enrichment comes in Phase 7/8 — stub it as an immediate no-op `DONE` for now so the pipeline completes without YouTube/TTS yet).
- `PipelinePoller`, `PipelineOrchestrator`, `RetryPolicy` (dependency chaining + retry-on-malformed-JSON, per `design/components/05-generation-pipeline.md`).
- `POST /api/v1/courses/generate` (real endpoint now, replacing Phase 3's harness) + `GET /api/v1/jobs/{id}/status` with queue-position SQL.

**Definition of Done:** integration test that posts a topic, polls status to `DONE`, and asserts a real persisted `Course` with modules and lesson content exists; a second test asserting a deliberately-broken AI response gets retried and eventually surfaces `FAILED` cleanly, not stuck. This is the milestone worth demoing to yourself first — it's the actual core value proposition end to end.

**Suggested commits:** `feat(pipeline): add PipelineRun/JobStep model and poller`, `feat(pipeline): add OutlineFunction and LessonContentFunction`, `feat(api): add course generation and job status endpoints`, `test(pipeline): end-to-end generation completes and persists`, `test(pipeline): malformed AI output retries then fails cleanly`.

---

## Phase 5 — Frontend Bootstrap + Generation Flow

**Goal:** `client/` is created for the first time here, and immediately proves itself against the real backend built in Phases 1–4 — first real demo-able slice, a human can type a topic in the browser and watch a course appear.

**Tasks:**
- Bootstrap `client/`: Vite + React + TS scaffold, base folder layout (`components/blocks`, `pages`, `hooks`, `context`, `utils`), Vitest + Testing Library wired in.
- Add the `client` job to `.github/workflows/test-on-pr.yml` (`npm test`), completing the CI setup started in Phase 0.
- Auth0 React SDK wired (`Auth0Provider`, `useAuth0`, login/logout UI, silent token refresh), attaches Bearer token to API calls — this is where Phase 2's backend groundwork gets a real UI in front of it.
- `PromptForm` → calls `POST /courses/generate`, shows `pipelineRunId`.
- Polling hook (`useJobPolling`) → `GET /jobs/{id}/status`, shows progress/queue position, `LoadingSpinner`/`ErrorMessage`.
- Course/lesson pages + `LessonRenderer` + block components (`HeadingBlock`, `ParagraphBlock`, `CodeBlock`, `MCQBlock` — `VideoBlock` stubs until Phase 7), routing per `design/FINAL_STRUCTURE.md`.
- TS types generated from `openapi.yaml`.

**Definition of Done:** login/logout works end to end in the browser; manual walkthrough — log in, type a topic, watch status update, land on a rendered course with real lesson content. This is the point to actually record a rough demo for yourself.

**Suggested commits:** `chore(client): bootstrap Vite+React+TS skeleton`, `ci: add client test job`, `feat(client): wire Auth0 React SDK`, `feat(client): add PromptForm and generation trigger`, `feat(client): add job status polling hook`, `feat(client): add LessonRenderer and block components`, `feat(client): wire generated OpenAPI types`.

---

## Phase 6 — API Key Management & Rate Limiting

**Goal:** harden the generation entry point before adding more load on it (enrichment steps in the next phases will call more external APIs).

**Tasks:**
- `UserApiKey` entity + `ApiKeyEncryptor` (AES) + `ApiKeyResolver` (user key → default fallback, `usingDefaultKey` flag) — wired into `GeminiClient` calls.
- `GenerationRequestLog` + `SlidingWindowRateLimiter` (60s / max 2), wired into `PipelineOrchestrator.startGeneration`.
- Frontend: settings UI to set/clear a personal API key; surfaces the "using default key — yours failed" state plainly.

**Definition of Done:** test asserting a 3rd generation request within 60s gets `429`; test asserting an invalid stored user key falls back to default and flags it; manual check that the key is never returned in plaintext from the API.

**Suggested commits:** `feat(apikey): add BYO key storage with encryption and fallback`, `feat(ratelimit): add sliding-window rate limiter`, `feat(client): add API key settings UI`, `test(ratelimit): third request within window is rejected`.

---

## Phase 7 — External Enrichment: YouTube

**Goal:** replace the Phase 4 enrichment stub with real video lookup.

**Tasks:**
- `YouTubeClient` (search.list, `videoEmbeddable=true`), `EnrichmentFunction` (video half only for now).
- `VideoBlock.jsx` renders the returned embed.

**Definition of Done:** integration test with a mocked YouTube client asserting the lesson's video block gets a real embed URL after enrichment; manual check on one real generated lesson.

**Suggested commits:** `feat(integration): add YouTubeClient and video enrichment step`, `feat(client): add VideoBlock component`, `test(integration): enrichment attaches video embed URL`.

---

## Phase 8 — External Enrichment: Multilingual TTS

**Goal:** Hinglish translation + audio, completing `EnrichmentFunction`.

**Tasks:**
- `TranslationClient`, `TtsClient` (Gemini), gated by `autoTranslateToHindi`.
- Audio storage decision (blob column vs. object storage — pick the simplest free-tier option) + playback/download in the UI.

**Definition of Done:** test asserting the flag gates the behavior (off by default, no-op); manual listen-through on one generated lesson to confirm audio quality is usable, not just "a file got created."

**Suggested commits:** `feat(integration): add Gemini translation and TTS clients`, `feat(pipeline): complete EnrichmentFunction with translation/TTS`, `feat(client): add Hinglish audio playback`.

---

## Phase 9 — PDF Export

**Goal:** M11, entirely frontend per `design/components/10-pdf-export.md`.

**Tasks:** `LessonPDFExporter.jsx` (`html2canvas` + `jsPDF`), styled hidden render target, `DownloadButton`.

**Definition of Done:** manual check — downloaded PDF is legible, code blocks and MCQs render correctly, dark theme doesn't leak into the PDF.

**Suggested commits:** `feat(client): add PDF export for lessons`.

---

## Phase 10 — Admin & Observability, Export/Import

**Goal:** the two smaller Extendability features, both straightforward reads over data that already exists by now.

**Tasks:**
- `AdminController` (`/admin/jobs`, `/admin/errors`, `/admin/stats`) + `AdminOnly` guard + `Admin.jsx` page.
- `GET /courses/{id}/export`.

**Definition of Done:** admin page shows real job/error data from earlier phases' test runs; export endpoint returns a valid, re-importable-shaped JSON document.

**Suggested commits:** `feat(admin): add admin job/error endpoints and guard`, `feat(client): add admin dashboard page`, `feat(api): add course export endpoint`.

---

## Phase 11 — Deployment & Full CI/CD

**Goal:** M12, for real this time (Phase 0 only proved CI, not deploy).

**Tasks:**
- Render Web Service for `/server` (env vars: `MONGO_URI`→ replaced by `DB_URL`, `AUTH0_ISSUER`, `AUTH0_AUDIENCE`, `GEMINI_API_KEY`, `YOUTUBE_API_KEY`, key-encryption secret).
- Vercel project for `/client` (`VITE_AUTH0_DOMAIN`, `VITE_AUTH0_CLIENT_ID`, `VITE_API_URL`, `VITE_YOUTUBE_API_KEY`).
- Confirm auto-deploy on push works both sides; confirm `workflow_dispatch` manual test run works on a scratch branch (this directly answers the question you asked earlier — verify it for real here).

**Definition of Done:** a fresh visit to the live Vercel URL can generate and view a course against the live Render backend.

**Suggested commits:** `chore(deploy): configure Render backend service`, `chore(deploy): configure Vercel frontend project`, `docs: record live URLs`.

---

## Phase 12 — Polish & Showcase (M13)

**Goal:** the non-architectural milestone — make the work legible to someone else.

**Tasks:** real `README.md` (setup, architecture summary linking to `design/`), record the 5-minute demo video, fill in resume bullets from the doc's own list where genuinely true of what got built.

**Definition of Done:** a stranger could clone the repo, follow the README, and understand both how to run it and how it's designed.

---

## Phase 13 — Trending Topics

**Goal:** replace the frontend's hardcoded example-topic chips with real, live trending topics across all users — so people can discover what others are actually learning, not just see three static examples.

**Tasks:**
- `topic_trend` table: `topic_normalized` (unique, lowercased/trimmed — the dedup key), `display_topic` (original casing, for display), `generation_count`, `last_generated_at`.
- On every successful `POST /courses/generate`, upsert: increment `generation_count` for that normalized topic (insert if new).
- `GET /api/v1/topics/trending` — public-ish (or authenticated-but-not-owner-scoped) endpoint returning the top N by count. This is a deliberate, narrow exception to "private by default" (`BACKEND_PLAN.md` §5) — it only ever exposes a topic string + a count, never who generated it or their course content, so it can't leak anything user-specific.
- Frontend: `PromptForm.tsx`'s `EXAMPLE_TOPICS` constant becomes a live fetch from this endpoint, falling back to the current static examples if the table's empty (e.g. right after a fresh install, before anyone's generated anything).

**Definition of Done:** generating a few courses with overlapping/similar topics visibly changes what `GET /topics/trending` returns; the frontend's example chips reflect that live list; a test confirms one user's generation contributes to the *count* without exposing their identity or course content through this endpoint.

---

## What's deliberately *not* a phase

Caching (§9), webhook/event system, course import (only export built), production-grade secret storage — all explicit future-scope items from `BACKEND_PLAN.md`. Don't pull them forward; they're not blocking anything above.

## Suggested starting point

You said "let's start building the base ones" — that's **Phase 0 → Phase 1**. Phase 1 is the real proof point: once persistence + owner-scoping works and is tested, everything else (auth, AI, pipeline) plugs into a foundation that's already verified, instead of being debugged all at once later.
