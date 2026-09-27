# 6. External Enrichment (YouTube + Multilingual TTS)

## Purpose
Take a validated lesson's `video` query and language preference and attach real media — the `ENRICHMENT` step's implementation (§5).

## Key Classes / Packages
- `integration/youtube/YouTubeClient` — wraps YouTube Data API v3 `search.list` (`q`, `maxResults=1-3`, `type=video`, `videoEmbeddable=true`); returns a video ID/embed URL.
- `integration/gemini/TranslationClient` — Gemini text translation, English → Hinglish, only invoked when `autoTranslateToHindi` is set on the request/user preference.
- `integration/gemini/TtsClient` — Gemini TTS on the translated text, returns a `.wav` buffer.
- `EnrichmentStepHandler` (implements `StepHandler` from §5) — reads the lesson's `video` block query, calls `YouTubeClient`; if translation requested, calls `TranslationClient` then `TtsClient`; writes results back onto the `Lesson` entity (video embed URL, audio reference) and sets `isEnriched = true`.

## Data Flow
`EnrichmentStepHandler` → `YouTubeClient.search(query)` → embed URL persisted on the lesson's video block. In parallel/sequence → `TranslationClient.translate(lessonText)` → `TtsClient.synthesize(translatedText, voiceName)` → audio stored (as a blob column or object storage reference, free-tier dependent) → referenced from the lesson response DTO.

## Depends On
API Key Management (§7) for the Gemini key; Persistence (§2) to write results back; invoked only from the Pipeline (§5), never called directly from a controller.

## Extensibility Notes
- Each client (`YouTubeClient`, `TranslationClient`, `TtsClient`) is a separate class behind no shared interface today — deliberately simple since there's one provider each; if a second video/TTS provider is ever needed, that's the seam to introduce an interface at.
- Language toggle (pure Hindi, Tamil, etc.) is additive: `TranslationClient` takes a target-language param already; extending it is a new enum value + prompt variant, not a new component.
- Caching YouTube results by query (to save quota) is a known future add — see `BACKEND_PLAN.md` §9, deliberately deferred, but this is the component it would slot into (a cache check before `YouTubeClient.search`).
