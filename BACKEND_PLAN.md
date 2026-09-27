# Backend Plan — Learnify

Source doc analyzed: `ProblemReq.txt` (hackathon brief + 13-milestone roadmap: MERN backend, Auth0, Gemini/YouTube, PDF export, Render/Vercel). We're using it as a scope reference, not a spec — all milestones (course/lesson generation, rich lesson rendering, YouTube videos, Hinglish TTS, PDF export, auth, deployment) are still in scope and **none are being dropped**. This doc is the actual architecture we're building, decided incrementally. Ticked = decided. Notes explain *why*, since several of your calls changed defaults or resolved conflicts between two of your own answers.

Goals locked in: **free-tier only**, **hackathon-first but built to actually learn from and extend afterward**, **single user for now, architected so multi-user is a config flip, not a rewrite**.

## Alignment Check vs. Source Doc

Verified against `ProblemReq.txt`'s "Scope & Technology Stack" section and the 13-milestone roadmap:

| Doc says | We picked | Aligned? |
|---|---|---|
| Frontend: React, Vue, Angular, or vanilla | React + Vite | ✅ exact match |
| Backend: MERN recommended, **Spring Boot or Django permitted** | Spring Boot | ✅ explicitly permitted by the doc |
| AI: OpenAI, Hugging Face, or rule-based | Gemini | ✅ not in the doc's one-line scope summary, but Milestone 10 and the Milestone 13 resume bullets both explicitly use Gemini — the detailed roadmap already commits to it, so this follows the doc's own later intent |
| DB: Mongoose/MongoDB (Milestone 5's literal schema) | PostgreSQL + JSONB | ⚠️ deliberate adaptation — that schema is specific to the MERN path; having taken the Spring Boot path instead (permitted above), the same `Course/Module/Lesson` relationships and flexible `content` array carry over 1:1, just on a relational engine. See §3. |
| Version Control: GitHub, feature branches, PRs | Same | ✅ |
| Deployment: Heroku/Vercel/AWS "or similar" | Render (backend) + Vercel (frontend) | ✅ Render is literally what the doc's own Milestone 12 uses |
| Auth: Auth0 (Milestone 4) | Auth0 | ✅ |

All 13 milestones remain in scope — none dropped; see `design/FINAL_STRUCTURE.md` for the milestone → file mapping. Everything past this table (BYO API key, rate limiting, admin view, export/import) is additive, from your Extendability answers — not required by the doc, but doesn't conflict with anything in it either.

---

## 1. Runtime & Framework

- [x] **Spring Boot (Java)**

**Decision:** Spring Boot, Java. (See §2 — this superseded an earlier TypeScript pick for the *backend*; TypeScript still shows up, just on the frontend — see below.)

**Common API spec file:** we'll use **springdoc-openapi** to auto-generate an OpenAPI 3.0 spec (`openapi.yaml`) directly from annotated controllers, and commit that generated file to the repo as the single source of truth for the API contract. This also closes the loop on your TypeScript point: the frontend (React) generates its TypeScript types straight from `openapi.yaml` (via `openapi-typescript`), so the backend contract stays typed end-to-end without needing TypeScript on the backend itself. That's the actual reconciliation of points 1 and 2.

**Feedback:**

---

## 2. Language

- [x] **Java** (backend, via Spring Boot)
- [x] **TypeScript** (frontend only — types generated from the OpenAPI spec in §1, for the extendability reason you gave)

**Decision:** These aren't competing anymore once split by layer — Java gives static typing on the backend for free, TypeScript gives the same on the frontend, and the OpenAPI spec keeps them in sync instead of drifting.

**Feedback:**

---

## 3. Database

- [x] **Hybrid, behind a repository abstraction**

**Decision:** Single **PostgreSQL** instance (free tier: Neon, Supabase, or Railway — all have a free Postgres), using:
- Regular relational tables/columns for structured entities: `User`, `Course`, `Module`, `Lesson` metadata (title, tags, ownerId, timestamps, refs).
- A **JSONB column** for the flexible, AI-generated lesson content array (headings/paragraphs/code/video/mcq blocks) — this gives you schema-less flexibility *inside* a relational engine, so you're not running two separate database engines on free tiers.

To satisfy "easy to swap to another SQL/NoSQL later": all DB access goes through **Spring Data repository interfaces** (`CourseRepository`, `LessonRepository`, etc.) — business logic (services/controllers) never touches JPA/SQL specifics directly. Swapping Postgres → MySQL is a config/driver change. Swapping to MongoDB later means writing a new repository implementation behind the same interface; nothing above the repository layer changes.

**Feedback:**

---

## 4. API Style

- [x] **REST + versioning** (`/api/v1/...`)

**Why not GraphQL, since you asked:** GraphQL's real advantage is letting the client ask for exactly the nested fields it wants in one round trip — e.g. "give me this course, its modules, but only lesson titles, not full content" — which avoids over/under-fetching when the shape of what you need varies a lot per screen. It shines when you have deeply nested, client-driven, variable queries and many different consumers of the same API.

Here the hierarchy is fixed and shallow (Course → Module → Lesson, 3 levels, known in advance), and you control both frontend and backend. A couple of well-designed REST endpoints (e.g. `GET /courses/{id}?include=modules,lessons`) get you the same practical result with far less infrastructure — no schema/resolver layer, no N+1 query problem to manage, simpler caching (HTTP caching just works on REST, not naturally on GraphQL), and it matches the OpenAPI-spec approach in §1. GraphQL would be solving a problem we don't have yet. Sticking with REST.

**Feedback:**

---

## 5. Authentication

- [x] **Auth0**

**Decision:** Auth0's free tier covers up to 7,500 active users/month — well beyond what this project needs, so it stays free. To de-risk vendor lock-in anyway, auth will sit behind a small interface (`AuthProvider`/Spring Security integration point) so swapping to self-hosted JWT + Spring Security later is a contained change, not a rewrite. No need to self-host now.

**Ownership/authorization — locked in during design review:** courses are **private by default**, matching the source doc (`Course.creator`, `/api/user-courses`, "user-specific access to courses" — it never describes shared/public access). Identity is resolved *only* from the verified JWT (never a client-supplied `email`/`userId` param — that would be a textbook IDOR hole), and every course/module/lesson query is owner-scoped at the repository level. Full detail in `design/components/01-api-layer.md` and `03-auth-security.md`.

**Feedback:**

---

## 6. AI / Content Generation

- [x] **Single provider, hardcoded** — **Gemini** (source doc already uses it for TTS/translation, has a workable free tier, and using one provider for text + TTS avoids juggling two API keys/quotas)
- [x] **Prompts live in a dedicated prompt-template file** (not inline strings in code), e.g. `resources/prompts/course-prompt.json` and `resources/prompts/lesson-prompt.json`

**Lesson content prompt shape** (per your style reference at `go.md` — concise progressive-depth notes, not fluff):
1. **History / Context** — short, basic grounding (why this topic exists, where it fits).
2. **Core Content, A → Z** — the actual teaching content, foundational to advanced, with concrete examples wherever a concept needs one (code samples, worked scenarios) — this is the bulk of the lesson.
3. **Real-World Application** — only if genuinely applicable to the topic; how this is actually used in practice.

Goal stated explicitly in the prompt itself: generate something a person can actually learn from, not filler — depth and correctness over length.

- [x] **Generation strategy: async job queue** (not synchronous request/response — generation is multi-step and can take too long for one HTTP call)

**Feedback:**

---

## 7. Data Model Shape

- [x] **Fully open JSON content** — lesson `content` is an unrestricted JSON array/object, not locked to a fixed set of block types.

**Take:** this is the right call for extendability (matches Extendability §2 below — new block types are just new `type` values, no migration). The tradeoff is you lose DB-level validation on shape, so that responsibility moves entirely to the **application-level validation + retry logic** in §11 — that's where "open schema" stays safe rather than becoming a garbage-in problem.

**Feedback:**

---

## 8. Background Jobs / Pipeline Execution

- [x] **DB-backed job queue (`generation_job` table) for state/visibility** + **step handlers written as Spring Cloud Function units** (`Function<Input, Output>`) for the actual "serverless functions per step" you asked for.

**Reconciling your two answers (queue, and serverless-per-step) instead of picking one:** they're not actually competing — the queue table is the *control plane* (state, retries, queue position via SQL), and each step's business logic being a Spring Cloud Function is the *execution unit*. Writing handlers this way means the exact same code can run two different ways without a rewrite:
- **Now, on the free tier:** the poller invokes each `Function<Input,Output>` in-process, inside the one Render-hosted Spring Boot service. No Lambda, no JVM cold-start tax, no extra infra to pay for.
- **Later, if/when true serverless-per-step is wanted:** the same `Function` classes deploy as standalone AWS Lambda functions via Spring Cloud Function's adapter — Spring Cloud Function is built specifically to make a function portable between "called directly" and "deployed as a cloud function" with no code change.

So the plan.md answer stands as the target shape (serverless functions per step) — we're just choosing *when* each step actually runs as a deployed function vs. an in-process call, and that choice is now free to change later without touching the handler logic itself.

**How the pipeline + dependency chaining works** (answering your question directly): one `PipelineRun` row represents a full course generation; it owns an ordered set of `JobStep` rows — `OUTLINE` → `LESSON_CONTENT` (one per lesson) → `ENRICHMENT` (video/translation/TTS, per lesson) → `DONE`. Each step has `status` (`PENDING`/`RUNNING`/`DONE`/`FAILED`), an `input` reference, and an `output` reference. A step only gets enqueued (inserted as `PENDING`) once its dependency's row is `DONE`, and its `input` is built from that prior step's stored `output` — so "one step's response is the next step's input" is handled by literally reading the previous row's output column when building the next row. The poller just repeatedly picks up `PENDING` steps whose dependencies are satisfied and runs them; a strategy pattern (one handler class per `JobStep` type) keeps each step's logic isolated and independently testable/retryable.

**Queue position visibility — yes, worth showing:** since state lives in a normal table, position is just a query: count of other `PENDING`/`RUNNING` steps for the same user ahead of this one, ordered by `createdAt`. Roughly:
```sql
SELECT COUNT(*) FROM generation_job
WHERE status IN ('PENDING','RUNNING') AND created_at < :thisJobCreatedAt;
```
Frontend polls a `GET /api/v1/jobs/{id}/status` endpoint that returns `{status, position, currentStep}`.

**Feedback:**

---

## 9. Caching

- [x] **None initially**

**Feedback:**

---

## 10. External Integrations

- [x] **All of them, to fully meet the milestones — nothing skipped:** YouTube Data API (lesson videos), Gemini translation + TTS (Hinglish audio), PDF export.

**"Add these to the AI prompt too" — done at the prompt-design level:** the lesson-generation prompt (§6) explicitly asks the model to emit a `video` block as a **search query string**, not a link (feeds YouTube search directly); keep prose in plain, cleanly-segmented text/paragraph blocks (translation- and TTS-friendly, no embedded formatting that would break translation); and keep code blocks clearly typed with a `language` field and MCQs in a fixed shape (both needed for consistent PDF rendering). So the downstream integrations' needs are baked into what the AI is asked to produce, not bolted on after.

**Feedback:**

---

## 11. Validation & Error Handling

- [x] Schema validation on all AI-generated JSON before persisting (structure/required-fields check, since content itself is open per §7)
- [x] Centralized exception handling (`@ControllerAdvice` in Spring)
- [x] **Retry logic**: if the AI returns malformed/non-JSON output, or a step fails, that `JobStep` moves to `FAILED` with an attempt count and gets automatically re-enqueued (with the original error appended to the retry prompt, so the model gets a chance to self-correct) up to a small max-attempts cap, then surfaces as a real failure to the user rather than retrying forever.

**Feedback:**

---

## 12. Testing

- [x] Unit tests (JUnit + Mockito) for services/controllers
- [x] Integration tests (Spring Boot Test + Testcontainers or an embedded Postgres) for API routes and the job pipeline

**Feedback:**

---

## 13. Observability

- [x] **Basic only — no external tooling (no Sentry etc. for now)**
- [x] A simple **admin-only view** (protected route, e.g. `/admin`) showing: current job queue (status/position per job), recent failed jobs with their error, and basic counts (jobs today, success/fail rate). No fancy metrics stack — just a page backed by queries against the tables we already have.

**Feedback:**

---

## 14. Deployment & CI/CD

- **Frontend:** Vercel, auto-deploys on push to `main` via its GitHub integration.
- **Backend:** Render (free Web Service, builds via Maven/Gradle or a Dockerfile). This isn't a substitution caused by Spring Boot — the source doc's own Milestone 12 already puts the backend on Render and the frontend on Vercel, independent of MERN vs. Spring Boot. Render just needs to build a JAR instead of running `node server.js`.
- [x] **Single monorepo**, `project-root/{server,client}` — Render watches `/server`, Vercel watches `/client`, exactly like the original doc's split.
- [x] `.env`-based config on both sides (Render env vars for backend secrets, Vercel env vars for `VITE_*` frontend config) — matches what you already assumed.
- [x] **GitHub Actions** workflow, two triggers:
  - `pull_request` — run backend + frontend tests automatically to gate merges (we're already writing tests in §12).
  - `workflow_dispatch` — manual, on-demand run against **any branch**, not just `main`/PRs — lets you re-run the test suite on a custom branch on demand from the Actions tab (or CLI/API) without opening a PR first.
  Nothing beyond that — Render/Vercel already handle build+deploy on merge, so we're not duplicating a deploy pipeline in Actions, just a test gate you can also trigger manually.

**Feedback:**

---

## Extendability Ideas

- [x] **1. BYO AI key, per user, with safe fallback.** If a user configures their own Gemini API key, we use it; otherwise we fall back to our default project key. If a user's stored key is invalid/expired, the response/UI clearly flags "using default key — your key failed" rather than silently substituting. Stored keys get basic symmetric encryption at rest (e.g. AES via a key in env config) for now — good enough for a hackathon-scale project; note explicitly in code/docs that production-grade secret storage (KMS/vault) is a deliberate future scope cut, not an oversight.
- [x] **2. Pluggable content block types** — a registry (backend validates against a small `BlockType` registry; frontend's `LessonRenderer` reads the same set) so adding e.g. `diagram` or `flashcard` later is additive.
- [x] **3. Pipeline steps as first-class, inspectable state** — covered in full in §8 (job table, dependency chaining, position query).
- [x] **4. Multi-tenancy readiness** — every entity (`Course`, `Module`, `Lesson`, `GenerationJob`) carries an `ownerId` from day one, even in single-user mode (where it just points at one default/system user). No architectural limitation to going multi-user later — it's a matter of turning real auth on and letting `ownerId` vary, not a schema change.
- [x] **5. Rate limiting — sliding window, 1 min / max 2 requests.** Confirming the approach: yes, this is the sliding-window-log pattern — store a timestamp per generation request for a user, and on each new request run:
  ```sql
  SELECT COUNT(*) FROM generation_request
  WHERE user_id = :userId AND created_at > NOW() - INTERVAL '60 seconds';
  ```
  Reject (or queue-delay) if the count is already ≥ 2. Deliberately reusing plain SQL here rather than Redis, consistent with §9 (no caching layer yet) — this table also gives us the audit trail for free.
- [x] **6. Webhook/event system — future scope**, explicitly deferred, not built now.
- [x] **7. Export/import course as JSON** — `GET /api/v1/courses/{id}/export` returns the full stored course tree as JSON (this doubles as "return the stored one" — it's just reading what's already persisted, not regenerating).
- [ ] **8. Trending topics — future scope, not built yet.** A `topic_trend(topic_normalized, display_topic, generation_count, last_generated_at)` table, upserted (increment count) on every `POST /courses/generate`. This is a deliberate, narrow exception to "everything is private by default" (§5) — it tracks only the topic *string* and a count, never who generated it or their course content, so it stays cross-user without leaking any user's actual data. `GET /api/v1/topics/trending` returns the top N; the frontend's hardcoded example-topic chips (`PromptForm.tsx`) get replaced by this live list, falling back to the static examples when the table's empty (e.g. a fresh install with no generations yet). Tracked as its own phase in `IMPLEMENTATION_PLAN.md`.

**Feedback:**

---

## Open Questions

Genuinely not many left — most of what would've been open got resolved above:

- Which free Postgres provider for §3 (Neon vs Supabase vs Railway) — functionally near-identical for this project; can decide when we actually provision it, not architecturally important now.
- Confirm you're fine with **Render** as the backend host (only real alternative on free tier for a Java process would be Railway or Fly.io — Render is the closest like-for-like swap for what the source doc used).

**Feedback:**
