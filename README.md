# Learnify

AI-powered course generator — type a topic, get a structured, multi-module course.

**Status:** Backend + frontend both implemented (see `IMPLEMENTATION_PLAN.md`'s progress table for the current phase-by-phase state). Final polish/deployment (Phase 11–12) still pending.

## Design & Planning Docs

- [`ProblemReq.txt`](./ProblemReq.txt) — the original hackathon brief this project is scoped from.
- [`BACKEND_PLAN.md`](./BACKEND_PLAN.md) — architecture decisions, option-by-option.
- [`design/`](./design) — HLD, one LLD per component, and the final folder structure.
- [`IMPLEMENTATION_PLAN.md`](./IMPLEMENTATION_PLAN.md) — phased build plan with a live progress table.
- [`PROJECT_OVERVIEW.md`](./PROJECT_OVERVIEW.md) — what was built + a file-by-file reference.

A full setup guide and architecture summary land here in the final phase (M13), once deployed.

## Repo Layout

```
server/   Spring Boot backend (Maven)
client/   React + Vite + TypeScript frontend
design/   Architecture docs (HLD/LLD)
```

## Local Development

**Backend** (needs a local Postgres — `cd server && docker compose up -d`):
```
cd server
cp config/application.properties.example config/application.properties   # fill in values
mvn spring-boot:run
```
Spring Boot auto-loads `config/application.properties` from the working directory (native behavior, no extra library) — this is where local secrets/overrides go. `server/.env.example` is a separate reference: the actual OS environment variable names to set in a real deployment (Render, etc.), not for local dev.

**Frontend:**
```
cd client
cp .env.example .env.local   # fill in values
npm install
npm run dev
```
