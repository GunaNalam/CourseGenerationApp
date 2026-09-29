# 11. Course Export/Import

## Purpose
Let a stored course be pulled out as portable JSON — for sharing, backup, or seeding test data independent of the live DB.

## Key Classes / Packages
- `CourseController.exportCourse(id)` → `GET /api/v1/courses/{id}/export` — reassembles the course + its modules + lessons (including content JSONB) into one nested JSON document via existing repositories; this is a read of already-persisted data, not a new generation.
- `dto/response/CourseExportDto` — the full nested shape returned.

## Data Flow
Request → `CourseRepository.findById` (+ eager-fetch modules/lessons, or a few sequential repository calls assembled in the service) → map to `CourseExportDto` → serialize as JSON response.

## Depends On
Persistence (§2) only; gated by ownership check in Auth (§3) (a user can only export their own courses, unless made public later).

## Extensibility Notes
- Import (accepting this same JSON shape back in via `POST /api/v1/courses/import`) is the natural next step and reuses the same DTO — deliberately not built yet since only export was asked for, but the shape is already import-compatible.
