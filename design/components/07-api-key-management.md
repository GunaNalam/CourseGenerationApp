# 7. API Key Management (BYO Key)

## Purpose
Let a user optionally supply their own Gemini key; otherwise fall back to the project's default key — with a clear, honest signal about which one is actually being used.

## Key Classes / Packages
- `entity/UserApiKey(userId FK, encryptedKey, provider, createdAt)`.
- `apikey/ApiKeyEncryptor` — AES (symmetric) encrypt/decrypt using a key from environment config. Explicitly documented as a hackathon-scale stopgap, not production secret storage (KMS/vault is future scope).
- `apikey/ApiKeyResolver` — `resolve(userId) -> ResolvedKey{value, source: USER | DEFAULT, userKeyFailed: boolean}`. Tries the user's decrypted key first; if absent, or if a prior call flagged it as failing, falls back to the default key and sets `userKeyFailed = true` when relevant so the caller can surface it.
- `ApiKeyController` — `GET/PUT/DELETE /api/v1/users/me/api-key` (store/replace/remove the user's own key; never returns the raw key back, only whether one is set).

## Data Flow
Before any Gemini call (in §4 or §6), the calling `StepHandler` asks `ApiKeyResolver.resolve(ownerId)`. If the user's key was tried and Gemini rejected it (auth error), that failure is caught, `ApiKeyResolver` marks it, retries once with the default key, and the pipeline's job-status response includes a `usingDefaultKey: true, reason: "your key failed"` flag so the frontend can show it plainly — never silently substitutes without telling the user.

## Depends On
Persistence (§2) for the encrypted key row; consumed by §4 and §6, gated behind Auth (§3) for the management endpoints.

## Extensibility Notes
- `provider` column already anticipates more than one AI provider having its own BYO key later.
- Swapping AES-at-rest for a real secrets manager later is contained to `ApiKeyEncryptor` — nothing else references encryption directly.
