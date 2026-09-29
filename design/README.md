# Learnify — Design

This folder breaks `../BACKEND_PLAN.md`'s decisions into an actual system design: one High-Level Design (HLD) covering how components fit together, then one Low-Level Design (LLD) doc per component covering its internals, and finally a concrete folder/package structure derived from all of it.

Read in this order:

1. **[HLD.md](./HLD.md)** — the component map and how a request flows through the system end to end.
2. **[components/](./components)** — one LLD doc per component (classes/interfaces, data flow, extensibility notes). Read whichever ones you're touching.
3. **[FINAL_STRUCTURE.md](./FINAL_STRUCTURE.md)** — the actual repo/package/folder layout that falls out of 1 and 2. This is what you'd scaffold from.

## Components

| # | Component | Covers milestones |
|---|-----------|--------------------|
| 1 | [API Layer & Contract](./components/01-api-layer.md) | REST/versioning, OpenAPI spec |
| 2 | [Persistence](./components/02-persistence.md) | DB schema design (M5) |
| 3 | [Auth & Security](./components/03-auth-security.md) | Auth0 integration (M4) |
| 4 | [AI Content Generation](./components/04-ai-content-generation.md) | Prompt design (M8) |
| 5 | [Generation Pipeline & Job Queue](./components/05-generation-pipeline.md) | Async orchestration, queue visibility |
| 6 | [External Enrichment (YouTube, TTS)](./components/06-external-enrichment.md) | Video generation (M9), Multilingual (M10) |
| 7 | [API Key Management](./components/07-api-key-management.md) | Extendability — BYO key |
| 8 | [Rate Limiting](./components/08-rate-limiting.md) | Extendability — usage quotas |
| 9 | [Admin & Observability](./components/09-admin-observability.md) | Internal visibility, no external tooling |
| 10 | [PDF Export](./components/10-pdf-export.md) | PDF export (M11) — mostly frontend, backend's contract |
| 11 | [Course Export/Import](./components/11-course-export-import.md) | Extendability — JSON export |

Lesson rendering (M6) and routing/sidebar (M7) are frontend-only and covered in `FINAL_STRUCTURE.md`'s client layout, not as separate backend LLDs. Deployment (M12) and documentation (M13) stay in `BACKEND_PLAN.md` §14 — they're not components, they're process.
