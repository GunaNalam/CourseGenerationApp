# 8. Rate Limiting

## Purpose
Guard AI generation cost/quota per user: max 2 generation requests per rolling 60-second window (sliding-window-log approach).

## Key Classes / Packages
- `entity/GenerationRequestLog(userId, createdAt)` — one row per generation attempt (successful or not — logging the attempt, not the result).
- `ratelimit/SlidingWindowRateLimiter` — `allow(userId) -> boolean`, backed by:
  ```sql
  SELECT COUNT(*) FROM generation_request_log
  WHERE user_id = :userId AND created_at > NOW() - INTERVAL '60 seconds';
  ```
  Allows if count `< 2`; on allow, inserts a new log row in the same transaction (avoids a race between check and insert).
- Wired in as a check in `PipelineOrchestrator` (§5) before a new `PipelineRun`/`OUTLINE` step is created — `POST /courses/generate` returns `429 Too Many Requests` if denied.

## Data Flow
Request hits `PipelineOrchestrator.startGeneration(userId, topic)` → `SlidingWindowRateLimiter.allow(userId)` (SQL check + insert, transactional) → proceed or reject.

## Depends On
Persistence (§2) only. Deliberately plain SQL, no Redis — consistent with "no caching layer yet" (`BACKEND_PLAN.md` §9); this table also doubles as an audit trail of generation attempts.

## Extensibility Notes
- Window size and request cap are just two constants — trivially config-driven if they need to change per user tier later.
- If Redis is introduced later for caching, this is a natural candidate to move to a Redis sorted-set sliding window for lower DB load — not required now.
