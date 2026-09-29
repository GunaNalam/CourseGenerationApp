# 2. Persistence Layer

## Purpose
Single point of contact with the database. Everything above this layer talks to interfaces, never to SQL/JPA directly — this is what makes "swap the database later" realistic (`BACKEND_PLAN.md` §3).

## Key Classes / Packages
- `entity/` — `User`, `Course`, `Module`, `Lesson`, `PipelineRun`, `JobStep`, `GenerationRequestLog`, `UserApiKey`. Every entity has `ownerId`, `createdAt`, `updatedAt`.
- `repository/` — one Spring Data interface per entity (`CourseRepository extends JpaRepository<Course, UUID>`, etc.), plus custom query methods (e.g. `JobStepRepository.countPendingBefore(Instant)` for queue position).
- **Owner-scoped finders as the default access pattern** — `CourseRepository.findByIdAndOwnerId(id, ownerId)`, `CourseRepository.findAllByOwnerId(ownerId)`, same pattern on `Module`/`Lesson`/`PipelineRun`. Services call these, not a bare `findById`, so ownership enforcement (§1, §3) is baked into the query itself rather than relying on every caller remembering a separate check.
- `Lesson.content` — mapped as `@JdbcTypeCode(SqlTypes.JSON)` to a `JSONB` column, deserialized to a generic `List<Map<String,Object>>` (matches the open-schema decision in §7).

## Schema (core entities)
```
User(id, auth0Sub, email, createdAt)
Course(id, ownerId, title, description, tags[], createdAt)
Module(id, courseId FK, title, orderIndex)
Lesson(id, moduleId FK, title, objectives[], content JSONB, isEnriched, orderIndex)
PipelineRun(id, ownerId, courseId FK nullable, status, createdAt)
JobStep(id, pipelineRunId FK, type, status, input JSONB, output JSONB, attempt, error, dependsOnStepId, createdAt, updatedAt)
```

## Data Flow
Service calls repository interface → Spring Data JPA → PostgreSQL (Neon/Supabase/Railway free tier). Migrations managed by **Flyway** (`db/migration/V1__init.sql`, versioned, checked into the repo) — this is also what makes provider swaps traceable.

## Depends On
Nothing internal — this is the bottom layer. Everything else depends on it.

## Extensibility Notes
- Swapping Postgres → MySQL: driver + dialect config change only, repositories unchanged.
- Swapping to MongoDB later: implement the same repository interfaces against Spring Data MongoDB (or a hand-rolled adapter); services above never notice, since they only ever called the interface.
- `JobStep.dependsOnStepId` is what lets the pipeline poller in §5 answer "is this step's dependency satisfied" with one query.
