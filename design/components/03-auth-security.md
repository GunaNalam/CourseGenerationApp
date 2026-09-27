# 3. Auth & Security

## Purpose
Verify who's calling, resolve them to an internal `User`/`ownerId`, and protect routes — without hard-wiring the rest of the app to Auth0 specifically.

## Key Classes / Packages
- `security/SecurityConfig` — Spring Security filter chain; validates the Auth0-issued JWT (issuer/audience from config) on every request under `/api/v1/**` except public ones.
- `security/AuthenticatedUserResolver` — pulls `sub`/email/name off the validated JWT and resolves (or lazily creates) the matching internal `User` row.
- `security/AuthProvider` (interface) — thin seam: `resolveUser(request) -> User`. Auth0 is the only implementation today.

## Data Flow
Request with `Authorization: Bearer <token>` → Spring Security JWT filter validates signature/issuer/audience against Auth0 → `AuthenticatedUserResolver` attaches the internal `User` to the request context (e.g. via a `@RequestScope` bean or Spring Security's `Authentication` principal) → controllers/services read the current user from there, never from raw claims.

**Identity source of truth:** the resolved `User`/`ownerId` comes *only* from the verified token's `sub` claim. No controller, DTO, or query param is ever allowed to accept a client-supplied user identifier (email, userId, etc.) and treat it as "who's asking" — that would let any caller impersonate any other user by just changing a value in the request. This is what §1's ownership enforcement is actually built on top of; if this resolver were ever bypassable, every "private by default" check downstream would be too.

## Single-user mode
When auth is effectively "off" (local/demo), `AuthProvider` can be swapped for a `DefaultUserAuthProvider` that always resolves to one system default `User` row — same interface, zero changes elsewhere. This is the mechanism behind "single user now, multi-user later is a config flip."

## Depends On
Persistence (to resolve/create the `User` row).

## Extensibility Notes
- Because nothing outside this package imports Auth0's SDK types directly, replacing Auth0 with self-hosted JWT + Spring Security later means writing one new `AuthProvider` implementation, not touching controllers.
- `ownerId` propagated from here is what every other component's multi-tenancy relies on.
