# Learnify — Comprehensive Project Plan

**An AI-powered course generation platform** that takes a topic and automatically generates structured, multi-module courses with lessons, objectives, video links, quizzes, and optional Hinglish audio narration.

---

## Table of Contents
1. [Project Overview](#project-overview)
2. [Architecture Philosophy](#architecture-philosophy)
3. [Tech Stack](#tech-stack)
4. [Complete Directory Structure](#complete-directory-structure)
5. [Backend (Server) — Detailed File-by-File Guide](#backend-server--detailed-file-by-file-guide)
6. [Frontend (Client) — Detailed File-by-File Guide](#frontend-client--detailed-file-by-file-guide)
7. [Design & Documentation](#design--documentation)
8. [Data Flow & Key Workflows](#data-flow--key-workflows)
9. [Configuration & Environment](#configuration--environment)
10. [Testing Strategy](#testing-strategy)

---

## Project Overview

### What Learnify Does

1. **User Registration & Login**: Auth0 integration for secure identity management.
2. **Course Generation**: User submits a topic (e.g., "Machine Learning"), backend orchestrates an async pipeline:
   - Generates course outline (3–6 modules)
   - Generates lesson content for each lesson (with objectives, examples, MCQ quizzes)
   - Enriches videos (resolves AI-generated search queries to actual YouTube video IDs)
3. **Lesson Viewing**: User can read generated lessons with:
   - Objectives and learning outcomes
   - Structured content (headings, paragraphs, code blocks, embedded YouTube videos, MCQs)
   - Interactive quizzes with instant feedback
   - Hinglish (Hindi + English) audio narration (on demand)
4. **Export & Sharing**: Export lessons as PDF or full course as JSON.
5. **API Key Management**: Users can provide their own Gemini API key (encrypted at rest); system falls back to a default key if needed.
6. **Admin Dashboard**: Allowlisted users can view system stats, recent jobs, and error logs.

### Key Features

| Feature | Why It Matters | Notes |
|---------|---|---|
| **Real-time Progress Updates** | Users see generation progress live, not a spinning wheel | Frontend polls backend every 2s |
| **Async Pipeline** | Heavy AI work doesn't block HTTP requests | Background scheduler processes steps independently |
| **Retry Logic** | Failures don't kill the whole course | AI parsing errors are caught, error fed back to next attempt |
| **Video Enrichment** | YouTube links actually work | YouTube API lookup happens during pipeline run |
| **User API Keys** | Users can bring their own Gemini quota | AES-GCM encryption, automatic fallback to project key |
| **Rate Limiting** | Prevents abuse (max 2 requests per 60s) | Sliding-window log, plain SQL, no external cache |
| **Hinglish Audio** | Makes content accessible in India's mixed-language environment | Text→Hinglish translation + Gemini TTS |
| **PDF Export** | Exportable lessons for offline reading | Forced light-theme render to ensure readability |
| **Auth0 Integration** | Industry-standard, zero-trust security | JWT validation; identity never spoofed from client |

---

## Architecture Philosophy

### Core Principles

1. **Separation of Concerns**: Controllers → Services → Repositories, each layer has one job.
2. **Async-First**: AI generation doesn't block the user; background poller processes jobs.
3. **Structural Ownership Enforcement**: Owner-scoped queries in repositories (e.g., `findByIdAndOwnerId`) prevent authorization bugs.
4. **External Configuration**: Prompts, API endpoints, and secrets live outside code (JSON files, env vars, encrypted DB).
5. **Minimal Mocking in Tests**: Use Testcontainers (real Postgres) instead of in-memory DBs; mock only external APIs (Gemini, YouTube).
6. **Open Data Structures**: Lesson content is a flexible JSONB array of blocks, not a rigid schema — new block types require no DB migration.
7. **User-Centric Error Handling**: Always tell the user what went wrong (e.g., "Gemini call failed, retrying...") rather than generic errors.

### Why These Choices?

- **Async Pipeline**: Course generation can take 30–60 seconds; synchronous calls would timeout. A job queue decouples generation from request/response.
- **Structural Ownership**: SQL-level filters prevent authorization bugs in services — a developer can't accidentally forget to check ownership.
- **JSONB Content**: Educational content evolves; forcing a migration for each new block type (e.g., "interactive simulation") wastes time. JSONB allows the UI to drive new block types.
- **Real Postgres in Tests**: In-memory DBs miss edge cases (e.g., JSON casting, transaction isolation). Real Postgres catches real bugs.

---

## Tech Stack

### Backend
- **Framework**: Spring Boot 3.x (Java 17+)
- **Build**: Maven
- **Database**: PostgreSQL 13+ with Flyway migrations
- **Auth**: Auth0 (OpenID Connect, JWT)
- **External APIs**:
  - Gemini 2.0 (course/lesson content, Hinglish translation, TTS)
  - YouTube Data API v3 (video lookup)
- **Libraries**:
  - Spring Data JPA (ORM)
  - Spring Security (JWT validation)
  - Jackson (JSON serialization)
  - Testcontainers (integration testing)
  - SLF4J + Logback (logging)

### Frontend
- **Framework**: React 18 (TypeScript)
- **Build**: Vite
- **Styling**: Tailwind CSS + CSS custom properties (light/dark mode)
- **Auth**: Auth0 React SDK
- **Key Libraries**:
  - `html2canvas`: PDF export (renders lesson to canvas, then PDF)
  - `jsPDF`: PDF generation
  - Tanstack React Query (optional future: data caching)
  - Vitest + React Testing Library (unit tests)

### DevOps
- **CI/CD**: GitHub Actions (run tests on PR, manual dispatch)
- **Containerization**: Docker Compose (local Postgres)
- **Deployment**: Render (backend), Vercel (frontend) — manual, not yet live

---

## Complete Directory Structure

```
TopicPlanner/
├── server/                           # Spring Boot backend
│   ├── pom.xml                       # Maven config (deps, build plugins)
│   ├── src/main/java/com/learnify/
│   │   ├── LearnifyApplication.java  # Entry point, @EnableScheduling
│   │   ├── config/
│   │   │   └── SecurityConfig.java   # JWT decoder, security filter chain
│   │   ├── entity/                   # JPA entities (DB tables)
│   │   │   ├── User.java
│   │   │   ├── Course.java
│   │   │   ├── Module.java
│   │   │   ├── Lesson.java
│   │   │   ├── PipelineRun.java
│   │   │   ├── JobStep.java
│   │   │   ├── GenerationRequestLog.java
│   │   │   └── UserApiKey.java
│   │   ├── repository/               # Spring Data interfaces (owner-scoped queries)
│   │   │   ├── UserRepository.java
│   │   │   ├── CourseRepository.java
│   │   │   ├── ModuleRepository.java
│   │   │   ├── LessonRepository.java
│   │   │   ├── PipelineRunRepository.java
│   │   │   ├── JobStepRepository.java
│   │   │   ├── GenerationRequestLogRepository.java
│   │   │   └── UserApiKeyRepository.java
│   │   ├── security/                 # Auth, JWT, user resolution
│   │   │   ├── CurrentUserProvider.java       # Interface
│   │   │   ├── JwtCurrentUserProvider.java    # Active impl (prod)
│   │   │   ├── DefaultCurrentUserProvider.java # Inactive (proof-of-concept)
│   │   │   ├── AuthenticatedUserResolver.java # Find-or-create User from JWT
│   │   │   └── AudienceValidator.java        # Custom JWT claim validation
│   │   ├── ai/                       # AI content generation
│   │   │   ├── GeminiClient.java              # HTTP wrapper to Gemini API
│   │   │   ├── GeminiRateLimiter.java         # Sliding-window rate limiting
│   │   │   ├── JsonResponseValidator.java     # Parse/validate AI JSON output
│   │   │   ├── BlockType.java                 # Enum of valid content blocks
│   │   │   ├── GeneratedCourse.java           # Validated course DTO
│   │   │   ├── GeneratedModule.java           # Validated module DTO
│   │   │   ├── GeneratedLesson.java           # Validated lesson DTO
│   │   │   ├── AiGenerationException.java     # Thrown on AI failure
│   │   │   └── prompt/
│   │   │       ├── PromptTemplate.java        # Template record (system + template)
│   │   │       ├── PromptTemplateLoader.java  # Load JSON from classpath
│   │   │       ├── CoursePromptBuilder.java   # Fill outline template placeholders
│   │   │       └── LessonPromptBuilder.java   # Fill lesson content template placeholders
│   │   ├── pipeline/                 # Async job queue & orchestration
│   │   │   ├── JobStatus.java                 # Enum: PENDING/RUNNING/DONE/FAILED
│   │   │   ├── JobStepType.java               # Enum: OUTLINE/LESSON_CONTENT/ENRICHMENT
│   │   │   ├── OutlineFunction.java           # Topic → validated Course outline
│   │   │   ├── LessonContentFunction.java     # Course+Module+Lesson → validated Lesson content
│   │   │   ├── EnrichmentFunction.java        # Video query → YouTube video ID
│   │   │   ├── PipelineOrchestrator.java      # Rate-limit & enqueue first OUTLINE step
│   │   │   ├── PipelinePoller.java            # @Scheduled loop, finds runnable steps
│   │   │   ├── JobStepProcessor.java          # Execute step, enqueue next, handle failures
│   │   │   └── RetryPolicy.java               # Failed step retry (3 attempts, error fed back)
│   │   ├── integration/
│   │   │   ├── youtube/
│   │   │   │   └── YouTubeClient.java         # YouTube Data API v3 search
│   │   │   └── gemini/
│   │   │       ├── TranslationClient.java     # Hinglish translation via Gemini
│   │   │       ├── GeminiTtsClient.java       # Gemini TTS (audio synthesis)
│   │   │       └── WavEncoder.java            # PCM → WAV (browser-playable audio)
│   │   ├── apikey/                   # User API key management
│   │   │   ├── ApiKeyEncryptor.java           # AES-GCM encrypt/decrypt
│   │   │   └── ApiKeyResolver.java            # Get user key or default, with source tagging
│   │   ├── ratelimit/                # Rate limiting
│   │   │   ├── SlidingWindowRateLimiter.java # Sliding-window log (2 requests per 60s)
│   │   │   └── RateLimitExceededException.java # → 429 status
│   │   ├── admin/                    # Admin-only views
│   │   │   ├── AdminAuthorizer.java           # Check JWT sub against allowlist
│   │   │   └── AdminAccessDeniedException.java # → 403 status
│   │   ├── controller/               # REST endpoints (thin, delegate to services)
│   │   │   ├── CourseController.java          # GET/POST courses, export
│   │   │   ├── ModuleController.java          # GET/POST modules
│   │   │   ├── LessonController.java          # GET/POST lessons, audio endpoint
│   │   │   ├── JobController.java             # POST generate, GET status
│   │   │   ├── ApiKeyController.java          # GET/PUT/DELETE user API key
│   │   │   └── AdminController.java           # Admin stats, jobs, errors
│   │   ├── service/                  # Business logic
│   │   │   ├── CourseService.java             # Course CRUD + owner-scoped fetch
│   │   │   ├── ModuleService.java             # Module CRUD + owner-scoped fetch
│   │   │   ├── LessonService.java             # Lesson CRUD + owner-scoped fetch
│   │   │   ├── CourseExportService.java       # Assemble course tree for export
│   │   │   └── LessonAudioService.java        # Translate + synthesize Hinglish audio
│   │   ├── dto/
│   │   │   ├── request/              # API request DTOs
│   │   │   │   ├── GenerateCourseRequest.java
│   │   │   │   ├── CreateCourseRequest.java
│   │   │   │   ├── CreateModuleRequest.java
│   │   │   │   ├── CreateLessonRequest.java
│   │   │   │   ├── SetApiKeyRequest.java
│   │   │   │   └── UpdateLessonStateRequest.java
│   │   │   └── response/             # API response DTOs
│   │   │       ├── CourseResponse.java
│   │   │       ├── CourseTreeResponse.java
│   │   │       ├── CourseExportResponse.java
│   │   │       ├── ModuleResponse.java
│   │   │       ├── ModuleWithLessonsResponse.java
│   │   │       ├── LessonResponse.java
│   │   │       ├── LessonSummaryResponse.java
│   │   │       ├── JobStatusResponse.java
│   │   │       ├── JobErrorSummary.java
│   │   │       ├── PipelineRunSummary.java
│   │   │       ├── ApiKeyStatusResponse.java
│   │   │       ├── GenerateCourseResponse.java
│   │   │       └── AdminStatsResponse.java
│   │   ├── exception/                # Exception handling
│   │   │   ├── NotFoundException.java          # → 404
│   │   │   └── GlobalExceptionHandler.java    # @RestControllerAdvice, maps domain exceptions
│   │   └── ...other standard Spring beans
│   ├── src/main/resources/
│   │   ├── application.yml           # Spring config (all values env-var overridable)
│   │   ├── db/migration/             # Flyway migrations (ordered)
│   │   │   ├── V1__baseline.sql      # Initial check (dropped in V7)
│   │   │   ├── V2__domain_model.sql  # User, Course, Module, Lesson
│   │   │   ├── V3__pipeline.sql      # PipelineRun, JobStep
│   │   │   ├── V4__rate_limit_and_api_keys.sql # GenerationRequestLog, UserApiKey
│   │   │   ├── V5__lesson_progress.sql # Lesson enrichment tracking
│   │   │   ├── V6__tags_and_objectives_as_arrays.sql # Array types
│   │   │   ├── V7__drop_baseline_check.sql     # Cleanup
│   │   │   └── V8__hinglish_audio_cache.sql    # Audio cache (unused, future)
│   │   └── prompts/
│   │       ├── course-prompt.json    # AI template for course outline
│   │       └── lesson-prompt.json    # AI template for lesson content
│   ├── src/test/java/com/learnify/  # Integration + unit tests
│   │   ├── LearnifyApplicationTests.java
│   │   ├── ai/
│   │   │   ├── JsonResponseValidatorTest.java
│   │   │   └── PromptBuilderTest.java
│   │   ├── apikey/
│   │   │   └── ApiKeyEncryptorTest.java
│   │   ├── controller/
│   │   │   ├── CourseFlowIntegrationTest.java # Full course generation flow
│   │   │   ├── ApiKeyControllerIntegrationTest.java
│   │   │   └── LessonAudioIntegrationTest.java
│   │   ├── admin/
│   │   │   └── AdminControllerIntegrationTest.java
│   │   ├── pipeline/
│   │   │   └── GenerationPipelineIntegrationTest.java
│   │   ├── ratelimit/
│   │   │   └── RateLimitIntegrationTest.java
│   │   └── integration/gemini/
│   │       └── WavEncoderTest.java
│   ├── Dockerfile (future)
│   └── docker-compose.yml            # Postgres container for local dev
│
├── client/                           # React + TypeScript frontend
│   ├── package.json                  # Dependencies, scripts
│   ├── tsconfig.json                 # TypeScript config
│   ├── tsconfig.app.json             # App-specific TS config
│   ├── tsconfig.node.json            # Node build config
│   ├── vite.config.ts                # Vite build config
│   ├── .oxlintrc.json                # Linter config
│   ├── vitest.config.ts (or similar) # Test runner config
│   ├── src/
│   │   ├── main.tsx                  # Entry point: mount React inside Auth0Provider
│   │   ├── App.tsx                   # Router + auth gate (loading/landing/app)
│   │   ├── vite-env.d.ts             # Type definitions for import.meta.env
│   │   ├── index.css                 # Global design system (colors, fonts, base styles)
│   │   ├── setupTests.ts             # Vitest config
│   │   ├── pages/                    # Full-page components
│   │   │   ├── Landing.tsx           # Logged-out hero + login button
│   │   │   ├── Home.tsx              # Prompt form + course list + live progress
│   │   │   ├── CoursePage.tsx        # Course overview + modules/lessons
│   │   │   ├── LessonPage.tsx        # Lesson viewer (sidebar nav, objectives, content)
│   │   │   ├── SettingsPage.tsx      # User API key management
│   │   │   └── AdminPage.tsx         # Admin dashboard (stats, jobs, errors)
│   │   ├── components/               # Reusable components
│   │   │   ├── Navbar.tsx            # Top navigation bar
│   │   │   ├── PromptForm.tsx        # Topic input + submit
│   │   │   ├── GenerationProgress.tsx # Live status card
│   │   │   ├── CourseCard.tsx        # Course tile in list
│   │   │   ├── SidebarNavigation.tsx # Module/lesson tree nav
│   │   │   ├── LessonRenderer.tsx    # Dispatch content blocks
│   │   │   ├── ExportCourseButton.tsx # JSON export trigger
│   │   │   ├── LessonPDFExporter.tsx # Render + export as PDF
│   │   │   ├── HinglishAudioButton.tsx # Fetch + play audio
│   │   │   ├── LessonStateToggle.tsx # Mark lesson as done/not done
│   │   │   ├── LoadingSpinner.tsx    # Loading indicator
│   │   │   ├── ErrorMessage.tsx      # Error display
│   │   │   ├── icons.tsx             # Shared SVG icons
│   │   │   └── blocks/               # Content block renderers
│   │   │       ├── HeadingBlock.tsx
│   │   │       ├── ParagraphBlock.tsx
│   │   │       ├── CodeBlock.tsx     # Copy-to-clipboard
│   │   │       ├── VideoBlock.tsx    # YouTube iframe or query text
│   │   │       └── MCQBlock.tsx      # Interactive quiz
│   │   ├── hooks/                    # Custom React hooks
│   │   │   ├── useApi.ts             # Auth-aware fetch wrapper
│   │   │   ├── useJobPolling.ts      # Poll job status every 2s
│   │   │   ├── useCourseTree.ts      # Fetch course + modules + lessons
│   │   │   ├── useLessonContent.ts   # Fetch single lesson
│   │   │   └── useDarkMode.ts        # Light/dark toggle persistence
│   │   ├── utils/                    # Utility functions/types
│   │   │   ├── api.ts                # API_BASE_URL, ApiError class
│   │   │   ├── api-types.ts          # Hand-written TS types (DTOs)
│   │   │   └── api-routes.ts         # API endpoint constants
│   │   ├── App.test.tsx              # Landing page render test
│   │   ├── PromptForm.test.tsx       # Form interactivity test
│   │   └── [other tests]
│   ├── .env.example                  # Template for .env.local
│   ├── Dockerfile (future)
│   └── [Vite standard files]
│
├── design/                           # Architecture & design docs
│   ├── README.md                     # Design docs index
│   ├── HLD.md                        # High-level architecture
│   ├── FINAL_STRUCTURE.md            # Approved folder structure
│   └── components/                   # Detailed LLD per component
│       ├── 01-api-layer.md
│       ├── 02-persistence.md
│       ├── 03-auth-security.md
│       ├── 04-ai-content-generation.md
│       ├── 05-generation-pipeline.md
│       ├── 06-external-enrichment.md
│       ├── 07-api-key-management.md
│       ├── 08-rate-limiting.md
│       ├── 09-admin-observability.md
│       ├── 10-pdf-export.md
│       └── 11-course-export-import.md
│
├── .github/
│   └── workflows/
│       └── test-on-pr.yml            # CI: run tests on every PR
│
├── ProblemReq.txt                    # Original hackathon brief
├── plan.md                           # Early planning notes
├── BACKEND_PLAN.md                   # Architecture decisions + rationale
├── IMPLEMENTATION_PLAN.md            # Phase-by-phase build roadmap
├── PROJECT_OVERVIEW.md               # What was built (file reference)
├── README.md                         # Quick-start guide
├── COMPREHENSIVE_PROJECT_PLAN.md     # This file
└── .gitignore
```

---

## Backend (Server) — Detailed File-by-File Guide

### Entry Point & Configuration

#### `LearnifyApplication.java`
- **What**: The Spring Boot entry point (has `public static void main`).
- **Annotations**: `@SpringBootApplication`, `@EnableScheduling` (enables the pipeline poller).
- **Why This Way**: `@EnableScheduling` is the minimal, explicit way to enable background job processing without an external message queue. For this project's scale (user-driven generation, not high-frequency events), it's simpler than Kafka/RabbitMQ.

#### `config/SecurityConfig.java`
- **What**: Builds the JWT decoder, validates tokens against Auth0, and defines the security filter chain.
- **Key Methods**:
  - `jwtDecoder()`: Creates a decoder that validates issuer, audience (custom validator), and signature.
  - `securityFilterChain()`: Marks all `/api/v1/**` routes as authenticated; allows `/actuator`, `/swagger-ui`, and the landing page unauthenticated.
- **Why This Way**: Spring Security's declarative filters are simpler and more secure than manual checks in controllers. Ownership validation happens in repositories (structural), not in controllers (behavioral).

#### `application.yml`
- **What**: Central config file; every value is overridable by an environment variable.
- **Example Patterns**:
  - `gemini.api-key: ${GEMINI_API_KEY:}` — read from OS env, default to empty.
  - `auth0.issuer: ${AUTH0_ISSUER}` — required; no default.
- **Why This Way**: Secrets and environment-specific settings belong in the environment, not in code. Using Spring's `${VAR:default}` syntax avoids a separate `.env` library.

---

### Data Layer

#### Entities (`entity/` folder)

Each class maps to a database table. JPA annotations handle column mapping and relationships.

| Entity | Responsibility | Key Fields |
|--------|---|---|
| **User** | Auth0-linked identity | `auth0Sub`, `email`, `createdAt` |
| **Course** | A generated course | `title`, `description`, `ownerId` (FK to User), `tags`, `objectives` |
| **Module** | A section of a course | `title`, `description`, `courseId` (FK), `position` |
| **Lesson** | A lesson within a module | `title`, `objectives`, `content` (JSONB array of blocks), `moduleId` (FK), `isEnriched` |
| **PipelineRun** | One generation request | `ownerId` (FK), `courseId` (FK), `status`, `createdAt` |
| **JobStep** | One pipeline step (OUTLINE / LESSON_CONTENT / ENRICHMENT) | `type`, `status`, `dependsOnStepId` (nullable FK), `input`, `output` (JSONB), `attempt`, `error` |
| **GenerationRequestLog** | Rate-limit audit trail | `userId` (FK), `timestamp` (for sliding-window lookup) |
| **UserApiKey** | Encrypted user Gemini key | `userId` (FK), `encryptedKey`, `salt` |

**Why JSONB for Lesson Content?**
- New block types (e.g., "interactive simulation") don't require a schema migration.
- The UI can innovate independently from the DB.
- Validation happens in code (`BlockType` enum), not in SQL.

**Why `isEnriched` flag?**
- Tracks whether this lesson has had videos resolved. Prevents re-enriching.

#### Repositories (`repository/` folder)

Spring Data interfaces; one per entity. Custom queries live here, nowhere else.

**Key Custom Queries:**
- `JobStepRepository.findRunnableSteps()` — finds steps with no unfinished dependencies (SQL `LEFT JOIN` + `WHERE`).
- `CourseRepository.findByIdAndOwnerId(id, ownerId)` — returns null if the user doesn't own it.
- `LessonRepository.findByIdAndModule_Course_OwnerId(id, ownerId)` — deep ownership check (Lesson → Module → Course → Owner).

**Why Structural Ownership?**
- If a service developer forgets to filter by owner, the query returns nothing, not a security breach.
- Authorization is structural, not behavioral — can't be bypassed.

---

### Security Layer

#### `security/CurrentUserProvider.java` (Interface)
- **What**: The contract: "who's making this request?"
- **Why**: Makes auth swappable. Prod uses `JwtCurrentUserProvider`; tests can use `DefaultCurrentUserProvider`.

#### `security/JwtCurrentUserProvider.java`
- **What**: Extracts the current user from the verified JWT (Spring Security's `SecurityContext`).
- **Why**: Spring Security has already validated the JWT; we just read the claims.

#### `security/AuthenticatedUserResolver.java`
- **What**: Takes an Auth0 `sub` and `email` from the JWT, finds-or-creates the internal `User` row, returns it.
- **Why**: The system's own `User` table is separate from Auth0. This resolver ensures every JWT has a corresponding DB row.

#### `security/AudienceValidator.java`
- **What**: Custom JWT validator; checks that the `aud` (audience) claim matches our API's identifier.
- **Why**: Auth0 can issue JWTs for multiple apps. This prevents a JWT intended for a different app from being used here.

---

### AI & Content Generation Layer

#### `ai/GeminiClient.java`
- **What**: Thin HTTP wrapper around Google Gemini's API.
- **Method**: `generateContent(prompt: String): String` — calls Gemini, extracts the text response.
- **Error Handling**: Throws `AiGenerationException` on HTTP error or empty response.
- **Why This Way**: Isolate the HTTP call in one place. Easy to mock for tests.

#### `ai/JsonResponseValidator.java`
- **What**: Parses raw AI output (strips markdown fences), validates JSON schema, enforces `BlockType` on every block.
- **Key Methods**:
  - `validateCourse(json)` → `GeneratedCourse` (or throw on validation error).
  - `validateLesson(json)` → `GeneratedLesson`.
- **Why**: AI outputs are noisy (extra newlines, escaped quotes, etc.). Normalize before validation.

#### `ai/BlockType.java`
- **What**: Enum registry of valid content block types: `HEADING`, `PARAGRAPH`, `CODE`, `VIDEO`, `MCQ`.
- **Why**: The frontend matches this exactly. Adding a new block type requires changes in both; centralizing the enum catches mismatches.

#### `ai/GeneratedCourse.java`, `GeneratedModule.java`, `GeneratedLesson.java`
- **What**: Records (immutable DTOs) for validated AI output.
- **Why**: Can't be null (validation ensures required fields); safe to pass around.

#### `ai/AiGenerationException.java`
- **What**: Thrown when AI call or validation fails.
- **Why**: Caught by `RetryPolicy` (not a panic; fed back into next attempt).

#### `ai/prompt/` folder

**`PromptTemplate.java`**
- A record: `{ system: String, template: String }`.
- The template has placeholders like `{topic}` or `{error_hint}`.

**`PromptTemplateLoader.java`**
- Loads a JSON file from the classpath (e.g., `prompts/course-prompt.json`).
- **Why**: Prompts live outside code. Edit without recompiling.

**`CoursePromptBuilder.java`**
- Takes a `PromptTemplate` and fills placeholders (e.g., `{topic}` → "Machine Learning").
- Optionally adds error context on retry: "Previous attempt failed: {error}. Try again."

**`LessonPromptBuilder.java`**
- Fills lesson-specific template. Includes course/module/lesson context.

**Why This Architecture?**
- Prompts are editable without code changes.
- Retry logic feeds the error back into the prompt ("here's what broke; try again").
- Separation of concerns: builders don't know about HTTP or JSON; `GeminiClient` doesn't know about Gemini's API structure.

---

### Pipeline (Async Job Queue)

#### `pipeline/JobStatus.java` & `JobStepType.java`
- Enums: `PENDING`, `RUNNING`, `DONE`, `FAILED` and `OUTLINE`, `LESSON_CONTENT`, `ENRICHMENT`.
- **Why**: Type safety. Prevents bugs like `if (status == "DONE")` (string comparison).

#### `pipeline/OutlineFunction.java`
- **What**: Pure `Function<String, GeneratedCourse>` — topic → validated course outline.
- **Inputs**: Topic, max modules.
- **Outputs**: Course with modules (no lesson content yet).
- **Why Pure Function**: Testable without mocks (except `GeminiClient`, which is mocked).

#### `pipeline/LessonContentFunction.java`
- **What**: Pure `Function<Input, GeneratedLesson>` — course/module/lesson context → validated lesson content.
- **Inputs**: Course title, module title, lesson title, optional retry error.
- **Outputs**: Lesson with objectives and content blocks.

#### `pipeline/EnrichmentFunction.java`
- **What**: Pure `Function<String, String?>` — video search query → YouTube video ID (or null if not found).
- **Why**: Enrichment is optional; if lookup fails, the lesson still has the query text.

#### `pipeline/PipelineOrchestrator.java`
- **What**: Entry point when user submits `/courses/generate`.
- **Steps**:
  1. Rate-limit check (throws `RateLimitExceededException` if exceeded).
  2. Create `PipelineRun` row.
  3. Create first `JobStep` (OUTLINE).
  4. Return immediately (don't wait for generation).
- **Why This Way**: Synchronous HTTP request doesn't block on async work.

#### `pipeline/PipelinePoller.java`
- **What**: `@Scheduled` method (runs every few seconds).
- **Steps**:
  1. Find all `PENDING` steps with no unfinished dependencies.
  2. Mark each as `RUNNING`.
  3. Hand to `JobStepProcessor`.
- **Why This Way**: Simple, no distributed locks needed. Poller is single-threaded; handles one step at a time.

#### `pipeline/JobStepProcessor.java`
- **What**: The real orchestration logic; executes one `JobStep`.
- **Steps**:
  1. Call the appropriate function (e.g., `OutlineFunction` for OUTLINE steps).
  2. Resolve the user's API key (or fall back to default); tag which source was used.
  3. Persist the result (create Course/Module/Lesson rows).
  4. Enqueue the next step(s).
  5. If the run is complete, mark `DONE`.
  6. On failure, enqueue a retry or mark `FAILED`.
- **Why Here**: Orchestration logic (who generates next, when to mark done) lives in one place.

#### `pipeline/RetryPolicy.java`
- **What**: On step failure, re-enqueue with error message fed back into the prompt.
- **Logic**:
  - Up to 3 attempts.
  - Each attempt increments `attempt` counter.
  - Error message from exception is saved and included in next prompt.
  - After 3 attempts, mark `FAILED`.
- **Why This Way**: AI is noisy; one failure doesn't mean the second attempt will fail. But 3 attempts is a reasonable limit (prevent infinite loops).

---

### Integration Layer

#### `integration/youtube/YouTubeClient.java`
- **What**: Calls YouTube Data API v3 `search` method.
- **Input**: Video search query (e.g., "machine learning intro tutorial").
- **Output**: First video's ID, or null if no results.
- **Error Handling**: Returns null (doesn't throw). A failed lookup shouldn't fail the lesson.
- **Why This Way**: YouTube lookup is optional; the lesson can show the query text if lookup fails.

#### `integration/gemini/TranslationClient.java`
- **What**: Reuses `GeminiClient` with a Hinglish translation prompt.
- **Input**: English lesson text.
- **Output**: Hinglish-translated text (Hindi words + English structure).
- **Why**: Hinglish is a common medium in India. Translation allows the same lesson content to be narrated in mixed language.

#### `integration/gemini/GeminiTtsClient.java`
- **What**: Calls Gemini's TTS model.
- **Input**: Hinglish text.
- **Output**: WAV audio bytes.
- **Status**: **Unverified against live API** — code is correct, but no real key available to test.
- **Why Split Out**: Isolates audio synthesis. Easy to swap for a different TTS provider later (Google Cloud Text-to-Speech, etc.).

#### `integration/gemini/WavEncoder.java`
- **What**: Wraps raw PCM audio bytes in a standard WAV header.
- **Why**: Browsers can play WAV files; raw PCM can't be played directly.
- **Tests**: Fully unit-tested (no network); validates the WAV header structure.

---

### API Key Management

#### `apikey/ApiKeyEncryptor.java`
- **What**: AES-GCM encrypt/decrypt for user keys.
- **How**: Generates a random salt, derives a key from the password (PBKDF2), encrypts with AES-GCM.
- **Security Note**: At-rest encryption only; in-flight protection relies on HTTPS.
- **Future**: Replace with KMS (AWS KMS, Google Cloud KMS) for production.

#### `apikey/ApiKeyResolver.java`
- **What**: Given a user, returns `(apiKey, source)` where source is "USER" or "DEFAULT".
- **Logic**:
  1. Check if user has a stored key.
  2. If yes and decryption succeeds, return it.
  3. Otherwise, return the default (project) key.
  4. Tag which source was used.
- **Why This Way**: Graceful fallback. If a user's key is corrupted or expired, the system keeps working.

---

### Rate Limiting

#### `ratelimit/SlidingWindowRateLimiter.java`
- **What**: Enforces max 2 generation requests per 60s per user.
- **Implementation**: Plain SQL (no external cache like Redis).
- **How**:
  1. Query `GenerationRequestLog` for entries in the last 60s for this user.
  2. If count >= 2, throw exception.
  3. Otherwise, insert a new row and allow.
- **Why Plain SQL**: Simpler than Redis for a single-instance app; Postgres is already there.

#### `ratelimit/RateLimitExceededException.java`
- Thrown when limit exceeded; caught by `GlobalExceptionHandler` → `429 Too Many Requests`.

---

### Admin Layer

#### `admin/AdminAuthorizer.java`
- **What**: Checks if a user's JWT `sub` is in the allowlist.
- **Allowlist**: Configured as a comma-separated list (env var).

#### `admin/AdminAccessDeniedException.java`
- Thrown when a non-admin tries to access admin endpoints; caught by handler → `403`.

---

### Controllers

Controllers are thin; they delegate to services. They:
1. Extract parameters.
2. Call a service.
3. Map result to a response DTO.
4. Return.

| Controller | Endpoints |
|---|---|
| **CourseController** | `GET /api/v1/courses` (list), `GET /api/v1/courses/{id}` (fetch), `POST /api/v1/courses` (create), `GET /api/v1/courses/{id}/export` (export JSON) |
| **ModuleController** | `GET /api/v1/courses/{courseId}/modules` (list), `POST /api/v1/courses/{courseId}/modules` (create) |
| **LessonController** | `GET /api/v1/modules/{moduleId}/lessons` (list), `POST /api/v1/modules/{moduleId}/lessons` (create), `GET /api/v1/lessons/{lessonId}/audio` (Hinglish TTS) |
| **JobController** | `POST /api/v1/courses/generate` (start generation), `GET /api/v1/jobs/{id}/status` (poll progress) |
| **ApiKeyController** | `GET /api/v1/apikey` (check if set), `PUT /api/v1/apikey` (set), `DELETE /api/v1/apikey` (clear) |
| **AdminController** | `GET /api/v1/admin/jobs` (recent), `GET /api/v1/admin/errors` (failures), `GET /api/v1/admin/stats` (counts) |

**Why Thin Controllers?**
- Business logic in services is reusable (e.g., `CourseService.findById()` can be called by both web and future gRPC layers).
- Controllers focus on HTTP concerns (status codes, headers).

---

### Services

Services contain business logic. They call repositories and external clients.

| Service | Responsibility |
|---|---|
| **CourseService** | CRUD, owner-scoped fetch, exists checks. |
| **ModuleService** | CRUD, owner-scoped via course. |
| **LessonService** | CRUD, owner-scoped via module. |
| **CourseExportService** | Assemble full course/module/lesson tree into export DTO. |
| **LessonAudioService** | Orchestrate translate → synthesize → return WAV. |

**Pattern: `findByIdOrThrow(id, userId)`**
- Calls `repository.findByIdAndOwnerId(id, userId)`.
- If null, throws `NotFoundException`.
- Client gets `404`, not `403` (doesn't leak existence).

---

### DTOs

Request DTOs (inputs) and Response DTOs (outputs). Controllers never expose entities directly; DTOs decouple API shape from DB schema.

**Examples:**
- `GenerateCourseRequest { topic: String }` → `GenerateCourseResponse { pipelineRunId, jobId }`
- `CourseResponse { id, title, description, modules: ModuleResponse[] }`

---

### Exception Handling

#### `GlobalExceptionHandler.java`
- Central `@RestControllerAdvice` that maps domain exceptions to HTTP responses.
- **Examples**:
  - `NotFoundException` → `404 Not Found`
  - `RateLimitExceededException` → `429 Too Many Requests`
  - `AdminAccessDeniedException` → `403 Forbidden`
  - Any auth error (invalid JWT) → `401 Unauthorized`
- **Why**: Consistent error responses; no error handling sprinkled in controllers.

---

### Database Migrations

Flyway automatically runs migrations in order. Each migration is idempotent (can be run multiple times safely).

| Migration | Purpose |
|---|---|
| `V1__baseline.sql` | Check DB exists (dropped in V7) |
| `V2__domain_model.sql` | Core tables: `user`, `course`, `module`, `lesson` |
| `V3__pipeline.sql` | Pipeline tables: `pipeline_run`, `job_step` |
| `V4__rate_limit_and_api_keys.sql` | `generation_request_log`, `user_api_key` |
| `V5__lesson_progress.sql` | Track lesson enrichment state |
| `V6__tags_and_objectives_as_arrays.sql` | PostgreSQL ARRAY types |
| `V7__drop_baseline_check.sql` | Cleanup unused baseline constraint |
| `V8__hinglish_audio_cache.sql` | Future: cache TTS output (not yet used) |

---

### Prompt Templates

**`prompts/course-prompt.json`**
```json
{
  "system": "You are an expert course designer...",
  "template": "Create a course outline for {topic}..."
}
```

**`prompts/lesson-prompt.json`**
```json
{
  "system": "You are an expert educator...",
  "template": "Create lesson content for {course_title} > {module_title} > {lesson_title}..."
}
```

**Why JSON Files?**
- Editable without recompilation.
- Can be versioned, A/B tested, or swapped at runtime.

---

### Testing

Tests live in `src/test/java/com/learnify/` mirroring the source structure.

**Approach:**
- **Integration Tests** (Testcontainers): Real Postgres, mocked external APIs.
- **Unit Tests**: Pure functions or logic with minimal dependencies.

**Key Tests:**
- `CourseFlowIntegrationTest`: End-to-end generation flow (create → generate → poll → verify).
- `RateLimitIntegrationTest`: Rate limiting logic.
- `ApiKeyEncryptorTest`: Encryption/decryption correctness.
- `JsonResponseValidatorTest`: AI response parsing & validation.

**Why Testcontainers?**
- Real Postgres catches edge cases (JSON casting, transaction isolation).
- In-memory DBs miss real bugs.

---

## Frontend (Client) — Detailed File-by-File Guide

### Entry Points

#### `main.tsx`
- **What**: React root; mounts `<App>` inside `Auth0Provider` and `BrowserRouter`.
- **Why Split Auth0Provider**: All child components have access to `useAuth0()` hook.

#### `vite-env.d.ts`
- Type definitions for `import.meta.env.*` (Vite's env variables).
- Ensures `VITE_*` vars are type-safe.

#### `index.css`
- **The entire design system**: Colors (light/dark modes), fonts, base styles.
- **Pattern**: CSS custom properties (tokens), not hardcoded colors.
  ```css
  :root {
    --color-primary: #4f46e5;
    --color-bg: #ffffff;
  }
  @media (prefers-color-scheme: dark) {
    :root {
      --color-primary: #6366f1;
      --color-bg: #1f2937;
    }
  }
  ```
- **Why**: Light/dark toggle changes one property; entire app updates.

---

### App Routing

#### `App.tsx`
- **What**: Top-level router and auth gate.
- **Flow**:
  1. Check if user is authenticated (via `useAuth0()`).
  2. If loading, show spinner.
  3. If not logged in, show `<Landing>`.
  4. If logged in, show `<Routes>` (all protected pages).
- **Routes**:
  - `/` → `<Home>` (prompt form + course list)
  - `/course/:id` → `<CoursePage>` (course overview)
  - `/lesson/:id` → `<LessonPage>` (lesson viewer)
  - `/settings` → `<SettingsPage>` (API key mgmt)
  - `/admin` → `<AdminPage>` (stats/jobs)
- **Why**: Centralized routing; auth check happens once.

---

### Pages

#### `Landing.tsx`
- Logged-out hero page: "Learnify — AI-powered course generator".
- Auth0 "Log in" button.
- Copy describes the product.

#### `Home.tsx`
- **Layout**: Prompt form + live generation progress + course list.
- **Prompt Form**: Input field + example quick-fill chips (e.g., "React Fundamentals").
- **Generation Progress**: Shows current step (OUTLINE, LESSON_CONTENT, etc.), queue position, key-fallback notice.
- **Course List**: Grid of `<CourseCard>` tiles, one per course.
- **Why This Layout**: User starts generation, sees live progress, previous courses below.

#### `CoursePage.tsx`
- **What**: Course overview page.
- **Shows**: Course title, description, modules/lessons outline, JSON export button.
- **Why**: Gives context before diving into individual lessons.

#### `LessonPage.tsx`
- **Layout**: Sidebar navigation (module/lesson tree) + main content area.
- **Sidebar**: Clickable links highlight the current lesson.
- **Content**: Objectives + rendered blocks + prev/next navigation.
- **Export Buttons**: PDF (lesson only), JSON (full course).
- **Hinglish Audio**: "Listen" button to fetch and play audio.
- **Why Split**: Sidebar stays sticky; user can jump between lessons without scrolling to nav.

#### `SettingsPage.tsx`
- **What**: User API key management.
- **Features**: View if a key is set (never show the key itself), set a new key, clear the key.
- **Why**: Users who run out of default quota can bring their own.

#### `AdminPage.tsx`
- **What**: Admin-only dashboard.
- **Shows**: Allowlisted users see job counts, recent jobs, recent errors. Non-admins see a 403 message.
- **Why**: Ops needs visibility into what's failing without accessing logs.

---

### Components

#### `Navbar.tsx`
- **Items**: Logo, dark-mode toggle, Settings/Admin links, user avatar, logout.
- **Dark Mode Toggle**: Calls `useDarkMode()` to toggle.
- **Why Fixed Header**: Navigation always accessible.

#### `PromptForm.tsx`
- Input field for topic + example chips ("React", "Python OOP", etc.).
- Submit button.
- On submit, calls `POST /api/v1/courses/generate`, receives `{ jobId }`, polls job status.
- **Why**: Separate component, reusable (could be on a modal, etc.).

#### `GenerationProgress.tsx`
- Card showing: "Generating outline..." → "Generating lesson 1/5..." → "Done!".
- Polls `GET /api/v1/jobs/{jobId}/status` every 2s until complete.
- Shows key-fallback notice if user's key failed and fell back to default.
- **Why**: Real-time feedback beats a loading spinner.

#### `CourseCard.tsx`
- Tile: course title, description, module count, "Open" button.
- Clicking "Open" navigates to `<CoursePage>`.

#### `SidebarNavigation.tsx`
- Module/lesson tree; highlights the active lesson.
- **Why Sidebar**: Module titles are context; scrolling past them loses context.

#### `LessonRenderer.tsx`
- Takes `lesson.content` (JSONB array of blocks from backend).
- For each block, dispatches to the matching component:
  - `{ type: "heading", ... }` → `<HeadingBlock>`
  - `{ type: "video", ... }` → `<VideoBlock>`
  - etc.
- **Why**: Centralized dispatch; adding new block types is one-line change here.

#### `ExportCourseButton.tsx` / `LessonPDFExporter.tsx`
- **JSON Export**: Fetches `GET /courses/{id}/export`, downloads as JSON.
- **PDF Export**: Renders an off-screen, forced-light-theme copy of the lesson.
  - Uses `html2canvas` to capture as image.
  - Uses `jsPDF` to create PDF.
  - Forces light theme (`style="color-scheme: light"`) regardless of user's dark mode.
- **Why Forced Light PDF**: Printed documents are hard to read in dark theme.

#### `HinglishAudioButton.tsx`
- Fetches `GET /lessons/{lessonId}/audio`, receives WAV blob.
- Creates `<audio>` element, plays it.
- Shows "Loading..." while fetching, "Playing..." while playing.

#### `LessonStateToggle.tsx`
- Mark a lesson as "done" / "not done" (for tracking progress).
- Calls `PUT /lessons/{id}/state`.

#### Content Block Components (`components/blocks/`)

| Component | Renders |
|---|---|
| `HeadingBlock.tsx` | `<h3>` with proper styling. |
| `ParagraphBlock.tsx` | `<p>` with line breaks, links preserved. |
| `CodeBlock.tsx` | `<pre><code>` + copy-to-clipboard button. |
| `VideoBlock.tsx` | If enriched (has YouTube ID): `<iframe>` embedded. Otherwise: shows the search query. |
| `MCQBlock.tsx` | Clickable options; on selection, shows correct/incorrect + explanation. State persists in component (not persisted to backend). |

**Why Not Over-Complex?**
- No fancy UI animations; focus on clarity.
- Copy button is small and unobtrusive.
- MCQ click feedback is instant (no API call).

---

### Custom Hooks

#### `useApi.ts`
- **What**: Auth-aware fetch wrapper.
- **Methods**: `get(path)`, `post(path, body)`, `put(path, body)`, `del(path)`, `getBlob(path)`.
- **Auth**: Adds `Authorization: Bearer {token}` header automatically.
- **Error Handling**: Non-2xx status throws `ApiError`.
- **Why**: Every API call needs auth; centralizing prevents copy-paste bugs.

#### `useJobPolling.ts`
- **What**: Polls `GET /jobs/{jobId}/status` every 2s until `DONE` or `FAILED`.
- **Returns**: `{ status, step, queuePosition, usingDefaultKey, error }`.
- **Why**: Dedicated hook; reusable by any component that needs job progress.

#### `useCourseTree.ts`
- **What**: Fetches a course + all its modules + all its lessons in one call.
- **Uses**: `GET /api/v1/courses/{id}` (or a custom endpoint that returns the full tree).
- **Why**: Reduces HTTP round-trips; tree view is useful for both `CoursePage` and `LessonPage`.

#### `useLessonContent.ts`
- **What**: Fetches a single lesson.
- **Caches**: Used by `LessonPage` to avoid re-fetching when navigating between lessons.

#### `useDarkMode.ts`
- **What**: Reads/toggles dark mode preference from localStorage.
- **Returns**: `{ isDark, toggle }`.
- **Sets**: `document.documentElement.classList.toggle('dark')` to trigger CSS updates.
- **Persists**: Next visit remembers the preference.

---

### Utils

#### `api.ts`
- Constants: `API_BASE_URL`.
- `ApiError` class: Wraps HTTP errors with status, message, and data.

#### `api-types.ts`
- Hand-written TypeScript types matching backend DTOs exactly.
- **Examples**:
  ```typescript
  interface CourseResponse {
    id: string;
    title: string;
    description: string;
    modules: ModuleResponse[];
  }
  interface JobStatusResponse {
    status: "PENDING" | "RUNNING" | "DONE" | "FAILED";
    step?: string;
    queuePosition?: number;
  }
  ```
- **Why Hand-Written**: Avoids tool overhead (OpenAPI code-gen, etc.). Easy to keep in sync.

#### `api-routes.ts`
- API endpoint constants.
  ```typescript
  export const ROUTES = {
    courses: () => '/courses',
    course: (id) => `/courses/${id}`,
    generate: () => '/courses/generate',
    jobStatus: (id) => `/jobs/${id}/status`,
    // ...
  }
  ```
- **Why**: Single place to change endpoint paths.

---

### Tests

#### `App.test.tsx`
- Tests that landing page renders when logged out.

#### `PromptForm.test.tsx`
- Tests form submission, input validation, example chip clicks.

#### `MCQBlock.test.tsx`
- Tests interactive behavior: clicking options, showing feedback.

#### `CodeBlock.test.tsx`
- Tests copy-to-clipboard button.

**Why Only Key Components?**
- UI tests are fragile. Only test interactivity that breaks functionality.
- Rendering tests (does X appear?) are noise; trust the browser.

---

### Environment & Build

#### `.env.example`
```
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_AUTH0_DOMAIN=your-tenant.auth0.com
VITE_AUTH0_CLIENT_ID=your-client-id
```

#### `vite.config.ts`
- Configures HMR (hot module reload), build output, etc.

#### `package.json`
- Scripts: `dev` (Vite dev server), `build` (production build), `test` (Vitest), `lint`.

---

## Data Flow & Key Workflows

### Workflow 1: Course Generation

```
1. User enters topic, clicks "Generate"
   ↓
2. Frontend: POST /api/v1/courses/generate { topic: "..." }
   ↓
3. Backend: PipelineOrchestrator
   - Rate-limit check
   - Create PipelineRun row
   - Create OUTLINE JobStep
   - Return { jobId }
   ↓
4. Frontend: Start polling GET /api/v1/jobs/{jobId}/status every 2s
   - Show "Generating outline..."
   ↓
5. Backend: PipelinePoller (@Scheduled every 3s)
   - Find OUTLINE step (PENDING)
   - Mark RUNNING
   - Call JobStepProcessor
   ↓
6. JobStepProcessor
   - Call OutlineFunction with topic
   - Calls GeminiClient.generateContent(prompt)
   - Validates JSON response (JsonResponseValidator)
   - Creates Course, Module rows
   - Creates LESSON_CONTENT steps (one per lesson)
   - Enqueues next step
   ↓
7. PipelinePoller finds LESSON_CONTENT steps
   - For each: call LessonContentFunction
   - Call GeminiClient (with lesson context in prompt)
   - Validate and persist Lesson rows
   - Create ENRICHMENT steps for videos
   ↓
8. PipelinePoller finds ENRICHMENT steps
   - For each: call EnrichmentFunction
   - Call YouTubeClient.search()
   - Persist video ID to Lesson content
   ↓
9. PipelinePoller marks PipelineRun as DONE
   ↓
10. Frontend: Polling returns status=DONE, navigates to course
```

**Why This Flow?**
- User sees progress in real-time (not a blank screen for 60 seconds).
- If Gemini fails partway, system retries that step (not the whole course).
- Decoupling generation from HTTP prevents timeouts.

---

### Workflow 2: Viewing a Lesson

```
1. Frontend: GET /api/v1/courses/{courseId}
   - Backend: CourseService.findByIdOrThrow()
   - Returns course + modules list
   ↓
2. Frontend: GET /api/v1/modules/{moduleId}/lessons
   - Returns lessons for that module
   ↓
3. User clicks lesson
   ↓
4. Frontend: GET /api/v1/lessons/{lessonId}
   - Returns lesson with content blocks
   ↓
5. Frontend: LessonRenderer
   - For each block, render appropriate component
   - MCQ: click handlers, instant feedback
   - Video: show YouTube embed or query text
   - Code: show with copy button
   ↓
6. User clicks "Hinglish Audio"
   - Frontend: GET /api/v1/lessons/{lessonId}/audio
   - Backend: LessonAudioService
     - TranslationClient: text → Hinglish
     - GeminiTtsClient: Hinglish → WAV bytes
     - WavEncoder: wrap in WAV header
   - Returns WAV blob
   - Frontend: creates <audio>, plays it
```

---

### Workflow 3: PDF Export

```
1. User is on LessonPage, clicks "Export as PDF"
   ↓
2. LessonPDFExporter
   - Renders lesson off-screen with `visibility: hidden`
   - Forces light theme: `style="color-scheme: light"`
   ↓
3. html2canvas captures the off-screen render as image
   ↓
4. jsPDF creates a PDF, adds the image(s)
   - Pages the image if it's taller than one page
   ↓
5. Browser downloads the PDF
```

**Why Forced Light Theme?**
- Printed documents are hard to read with dark backgrounds.
- Screenshots of dark UI often look bad in print.

---

### Workflow 4: User API Key Management

```
1. User on SettingsPage, enters their Gemini API key
   ↓
2. Frontend: PUT /api/v1/apikey { key: "..." }
   ↓
3. Backend: ApiKeyController
   - ApiKeyEncryptor.encrypt(key)
   - Save to UserApiKey row
   ↓
4. Next generation request
   - JobStepProcessor calls ApiKeyResolver
   - Finds user's stored key
   - Decrypts it
   - Uses it instead of default
   - If decryption fails, falls back to default (tag as fallback)
   ↓
5. Frontend: GenerationProgress shows "Switched to default key" notice
```

---

## Configuration & Environment

### Backend (`server/`)

#### Local Development (`config/application.properties`)
```properties
# Create this from the .env.example template

spring.datasource.url=jdbc:postgresql://localhost:5432/learnify
spring.datasource.username=postgres
spring.datasource.password=postgres

auth0.issuer=https://your-tenant.auth0.com/
auth0.audience=https://learnify-api
auth0.client-id=your-id

gemini.api-key=your-gemini-key-here
gemini.model=gemini-2.0-flash

youtube.api-key=your-youtube-key

admin.allowlist=your-auth0-sub
```

**Why Property File, Not .env?**
- Spring Boot's native property support; no extra lib.
- `application.properties` (local, not version-controlled) overrides `application.yml` (version-controlled).

#### Production (Environment Variables)
- All the above as `SPRING_DATASOURCE_URL`, `AUTH0_ISSUER`, etc.
- Spring Boot automatically reads `SPRING_*` → application properties.

### Frontend (`client/`)

#### Local Development (`.env.local`)
```
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_AUTH0_DOMAIN=your-tenant.auth0.com
VITE_AUTH0_CLIENT_ID=your-client-id
```

#### Production (Vercel Environment Variables)
- Same variables, set in Vercel dashboard.
- Vite injects them at build time (only `VITE_*` are included in the bundle).

---

## Testing Strategy

### Backend

**Approach:**
- Integration tests with Testcontainers (real Postgres, mocked external APIs).
- Unit tests for pure logic (prompts, validation, encryption).

**Tools:**
- JUnit 5
- Mockito (mock `GeminiClient`, `YouTubeClient`, etc.)
- Testcontainers (manage Postgres container during test)
- AssertJ (fluent assertions)

**Example Test:**
```java
@SpringBootTest
@Testcontainers
class CourseFlowIntegrationTest {
  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(...);
  
  @Mock GeminiClient geminiClient;
  @Autowired PipelineOrchestrator orchestrator;
  
  @Test
  void generatingACourseCreatesModulesAndLessons() {
    // Arrange
    when(geminiClient.generateContent(any()))
      .thenReturn(validCourseJson);
    
    // Act
    GenerateCourseResponse response = orchestrator.generate(
      new GenerateCourseRequest("ML Basics")
    );
    
    // Simulate poller
    simulatePipelineExecution();
    
    // Assert
    CourseResponse course = courseService.findById(response.courseId(), userId);
    assertThat(course.modules()).hasSize(3);
  }
}
```

### Frontend

**Tools:**
- Vitest (test runner)
- React Testing Library (render + DOM queries)
- Vitest UI (visual test results)

**Example Test:**
```tsx
describe("<MCQBlock>", () => {
  it("shows correct/incorrect feedback on option click", () => {
    const question = {
      question: "What is 2+2?",
      options: ["3", "4", "5"],
      correctIndex: 1,
      explanation: "Four is correct."
    };
    
    const { getByText } = render(<MCQBlock {...question} />);
    
    fireEvent.click(getByText("4"));
    
    expect(getByText("Correct!")).toBeInTheDocument();
    expect(getByText("Four is correct.")).toBeInTheDocument();
  });
});
```

---

## Summary: Why Each Piece Exists

| Component | Purpose | Why This Approach |
|-----------|---------|-------------------|
| **PipelineOrchestrator** | Entry point for generation | Decouples HTTP from async work |
| **PipelinePoller** | Drives job execution | Single-threaded, simple, no external queue |
| **JobStepProcessor** | Orchestration logic | All state transitions in one place |
| **Repositories** | Structural ownership | Owner-scoped queries prevent auth bugs |
| **Services** | Business logic | Reusable across multiple interfaces (web, future gRPC) |
| **DTOs** | API shape | Decouples API from DB; can evolve independently |
| **Prompt Templates** | AI prompts | Editable without recompiling; A/B test-friendly |
| **Frontend Pages** | Full-page views | Clear separation of concerns |
| **Custom Hooks** | Reusable logic | DRY, testable, shared across components |
| **LessonRenderer** | Dynamic block rendering | New block types need no code changes to this component |
| **Tests** | Quality assurance | Real DB catches edge cases; mocked APIs keep tests fast |

---

## Next Steps for New Team Members

1. **Run Local Setup**:
   - Start Postgres: `cd server && docker compose up -d`
   - Copy `config/application.properties.example` → `config/application.properties`, fill values
   - Run backend: `mvn spring-boot:run`
   - Copy `client/.env.example` → `client/.env.local`, fill values
   - Run frontend: `npm run dev`

2. **Walk Through a Course Generation**:
   - Submit a topic on the frontend.
   - Watch the progress card update.
   - Open the database: `psql -d learnify` and query `SELECT * FROM job_step ORDER BY id DESC LIMIT 5;`
   - See how steps are created and completed.

3. **Read Code in This Order**:
   - `LearnifyApplication.java` (entry point)
   - `PipelineOrchestrator.java` (generation entry)
   - `JobStepProcessor.java` (real orchestration)
   - One controller (e.g., `CourseController.java`)
   - One service (e.g., `CourseService.java`)
   - One repository (e.g., `CourseRepository.java`)

4. **Understand the Data Model**:
   - Sketch out: User → Course → Module → Lesson
   - Note: Lesson.content is JSONB, not a relational structure.
   - Run migrations: `mvn flyway:info` (show migration history)

5. **Try Small Changes**:
   - Add a new field to `CourseResponse` (DTO + controller).
   - Add a new block type (enum in `BlockType.java` + React component).

---

## Glossary

| Term | Definition |
|------|-----------|
| **Auth0** | Third-party identity provider; issues JWTs. |
| **JWT** | JSON Web Token; claims (sub, aud, email) signed by Auth0. |
| **JSONB** | PostgreSQL data type; JSON stored in binary format. Supports queries, indexes. |
| **Flyway** | Database migration tool; applies SQL files in order. |
| **Testcontainers** | Spins up Docker containers (Postgres, etc.) for integration tests. |
| **Vite** | Frontend build tool; hot reload dev server. |
| **Tailwind** | CSS framework; utility classes (e.g., `p-4`, `text-lg`). |
| **FYI** | Full-stack TypeScript would require rewriting the backend. Spring Boot is mature; keep it. |

---

This document is your roadmap. Refer back to it when you need to understand *why* a piece exists and how it fits into the whole.

Good luck! 🚀
