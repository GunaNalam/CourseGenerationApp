# 4. AI Content Generation

## Purpose
Turn a topic (or a course/module/lesson title triple) into validated, structured JSON — this is the actual "intelligence" of the product, and the component most responsible for quality ("a genuinely useful resource, not filler").

## Key Classes / Packages
- `ai/GeminiClient` — thin wrapper over the Gemini API (text + TTS), takes an API key per call (from §7, not a static config) so BYO keys work per-request.
- `ai/prompt/PromptTemplateLoader` — loads `resources/prompts/course-prompt.json` and `resources/prompts/lesson-prompt.json` at startup.
- `ai/prompt/CoursePromptBuilder` — fills the course template with `{topic}`.
- `ai/prompt/LessonPromptBuilder` — fills the lesson template with `{courseTitle, moduleTitle, lessonTitle}`, enforcing the 3-part shape (History → A-Z content → Real-World Application) and the integration requirements from §10 (video as a search query, translation-safe plain text, typed code blocks).
- `ai/JsonResponseValidator` — parses the model's raw output, validates required top-level fields (`title`, `content[]`, etc.) exist and each content block has a recognized `type` (against the registry from Extendability §2), without constraining the rest of the shape.
- `ai/AiGenerationException` — thrown on malformed/unparseable output; caught by the pipeline (§5) to drive retry.

## Prompt File Shape (illustrative)
```json
{
  "system": "You are an expert course author. Return raw JSON only — no markdown, no prose.",
  "template": "Topic: {topic}\n\nGenerate a course with 3-6 modules, 3-5 lessons each...",
  "outputSchemaHint": { "title": "string", "description": "string", "tags": ["string"], "modules": [...] }
}
```
Lesson template additionally encodes the three-part structure and the explicit instruction: *"Prioritize genuine understanding and correct, concrete examples over length or filler."*

## Data Flow
Pipeline step (§5) calls `CoursePromptBuilder`/`LessonPromptBuilder` → `GeminiClient.generate(prompt, apiKey)` → `JsonResponseValidator.parse(raw)` → returns a validated domain object, or throws for the pipeline to retry.

## Depends On
API Key Management (§7) for which key to call Gemini with; nothing else — this component is intentionally isolated so it's the one place "the actual prompt engineering" lives.

## Extensibility Notes
- Provider is hardcoded to Gemini today, but because everything goes through `GeminiClient` behind no wider interface, a future provider-agnostic swap only touches this one class plus the two prompt files — not the pipeline or persistence.
- Prompt files are data, not code — iterating on prompt quality doesn't require a redeploy if externalized further (e.g. loaded from DB instead of classpath) later.
