# 9. Admin & Observability

## Purpose
A basic, honest window into system health for whoever's running this — not a metrics stack, just queries against tables that already exist, behind an admin-only route.

## Key Classes / Packages
- `AdminController` — `GET /api/v1/admin/jobs` (recent `PipelineRun`s + their step statuses), `GET /api/v1/admin/errors` (recent `FAILED` `JobStep`s with their `error` field), `GET /api/v1/admin/stats` (counts: jobs today, success/fail rate — simple `GROUP BY status` queries).
- `security/AdminOnly` — a role check (e.g. a hardcoded admin `ownerId`/email allowlist for now, since this is single-admin-scale) guarding the above.
- Standard Spring Boot logging (Logback) to stdout — Render captures this natively, no separate log shipping needed at this scale.

## Data Flow
Admin hits `/admin` (frontend route, Auth0-gated + admin check) → calls the `AdminController` endpoints → reads directly from `PipelineRun`/`JobStep`/`GenerationRequestLog` tables via existing repositories. No write path, no side effects — purely observational.

## Depends On
Persistence (§2) for all data; Auth (§3) for the admin gate.

## Extensibility Notes
- Deliberately no Sentry/external APM now — the tables already capture what's needed (status, error, timestamps) for this project's scale. If real production usage ever justifies it, this component is where a client for an external tool would get wired in, without changing how errors are captured upstream (they already land in `JobStep.error`).
