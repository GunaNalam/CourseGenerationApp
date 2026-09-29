# 1. API Layer & Contract

## Purpose
Single entry point for the frontend. Owns request/response shape, versioning, and the OpenAPI contract that the frontend's TypeScript types are generated from.

## Key Classes / Packages
- `controller/` — one `@RestController` per resource: `CourseController`, `LessonController`, `JobController`, `AdminController`, `ApiKeyController`.
- `dto/request/`, `dto/response/` — explicit DTOs, never expose JPA entities directly over the wire.
- `GlobalExceptionHandler` (`@ControllerAdvice`) — maps domain exceptions (from every other component) to consistent HTTP error responses.
- `config/OpenApiConfig` — springdoc-openapi setup; generates `/v3/api-docs` → exported to `openapi.yaml` at build time.

## Endpoints (representative, not exhaustive)
| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/courses/generate` | Kick off a course generation pipeline run |
| GET | `/api/v1/courses/{id}` | Fetch a full course (modules + lessons) |
| GET | `/api/v1/courses/{id}/export` | Full course as JSON (§11) |
| GET | `/api/v1/jobs/{pipelineRunId}/status` | Poll pipeline status + queue position |
| GET/PUT | `/api/v1/users/me/api-key` | Manage BYO AI key (§7) |
| GET | `/api/v1/admin/jobs`, `/api/v1/admin/errors` | Admin-only views (§9) |

## Data Flow
Frontend → Controller → DTO validation (`@Valid`) → delegate to the relevant service (Pipeline, Persistence, etc.) → map domain result to response DTO.

## Authorization — Private by Default

Decision (per `ProblemReq.txt` — `/api/user-courses`, `Course.creator`, "user-specific access to courses" — the source doc never describes cross-user/public access): **every course-scoped endpoint is owned-by-user, enforced server-side.**

- The current user is resolved **exclusively** from the verified Auth0 JWT via §3 (`AuthenticatedUserResolver`) — never from a query param, header, or request body field the client controls. No `?email=...` or `?userId=...` anywhere in this API. If a client could pass their own identity, they could pass anyone's — that's a direct-object-reference hole, not an edge case.
- Every repository call that fetches a `Course`/`Module`/`Lesson` is **owner-scoped in the query itself** (e.g. `findByIdAndOwnerId(id, currentUser.id)`, see §2), not "fetch by ID, then check `.ownerId` in the controller." Scoping at the query makes it structurally hard for a future endpoint to forget the check — there's no unscoped finder to accidentally call.
- A course that exists but belongs to someone else returns **`404 Not Found`**, not `403 Forbidden` — this avoids leaking *whether* a given course ID exists to a user who isn't its owner.
- Applies to reads and writes alike: `GET /courses/{id}`, `GET /courses/{id}/export`, `PUT/DELETE`, job status lookups scoped to the pipeline's owning user, etc.

## Depends On
Auth & Security (to resolve the authenticated user for every request), and every other service it fronts.

## Extensibility Notes
- Versioned from day one (`/api/v1`) so a breaking change later ships as `/api/v2` alongside the old one instead of a hard cutover.
- Because everything routes through DTOs, changing the internal entity/JSONB shape never has to change the wire contract unless we choose to.
- If public/shareable courses are ever wanted (e.g. "share this course via link"), that's an explicit future opt-in — a `Course.isPublic` flag plus one additional unauthenticated read path — not a relaxation of the default. Private-by-default doesn't get weakened to add it.
