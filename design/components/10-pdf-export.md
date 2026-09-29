# 10. PDF Export

## Purpose
Let a user download a lesson (text, code, MCQs) as a styled PDF. This is primarily a **frontend** concern (per the source doc: `html2canvas` + `jsPDF`, client-side DOM capture) — this doc exists to be explicit about the backend's (small) role, not to design a backend PDF service.

## Backend's Actual Responsibility
None functionally beyond what §2/§4 already guarantee: lesson `content` blocks are consistently shaped enough (typed `code` blocks with a `language`, plain `paragraph`/`heading` text, structured `mcq` with options/answer/explanation) that the frontend's `LessonPDFExporter.jsx` can render them predictably. No dedicated backend endpoint is needed for single-lesson export.

## If Extended Later (explicitly out of scope now)
A future "export whole course as one PDF" could warrant a server-side endpoint (e.g. using a headless-browser render or a Java PDF lib) instead of stitching many client-side captures together — noted here as a deliberate non-goal for now, not an oversight, consistent with keeping this hackathon-scoped.

## Depends On
Nothing new — relies on AI Content Generation (§4) producing well-typed blocks and Persistence (§2) serving them as-is via the normal course/lesson GET endpoints.
