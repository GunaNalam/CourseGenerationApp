// Derived from server/openapi.yaml via `npm run generate:types` -> api-schema.generated.ts.
// Don't hand-edit the generated file - change openapi.yaml and re-run the script instead.
import type { components } from './api-schema.generated'

export type CourseResponse = components['schemas']['CourseResponse']
export type CourseTreeResponse = components['schemas']['CourseTreeResponse']
export type CourseExportResponse = components['schemas']['CourseExportResponse']
export type ModuleWithLessonsResponse = components['schemas']['ModuleWithLessonsResponse']
export type LessonSummaryResponse = components['schemas']['LessonSummaryResponse']
export type UpdateLessonStateRequest = components['schemas']['UpdateLessonStateRequest']
export type GenerateCourseResponse = components['schemas']['GenerateCourseResponse']
export type JobStatusResponse = components['schemas']['JobStatusResponse']
export type ApiKeyStatusResponse = components['schemas']['ApiKeyStatusResponse']
export type PipelineRunSummary = components['schemas']['PipelineRunSummary']
export type JobErrorSummary = components['schemas']['JobErrorSummary']
export type AdminStatsResponse = components['schemas']['AdminStatsResponse']

// The spec loosens each content-block variant's `type` field to a plain string -
// an openapi-generator limitation, not a real backend shape: a discriminated oneOf's
// parent Java interface can't have variants returning a narrower enum type than its
// own abstract getter. Kept hand-written here so the frontend still gets real
// discriminated-union narrowing in switch(block.type). Matches what the backend
// actually persists (server/src/main/resources/prompts/lesson-prompt.json), minus
// `videoId`/`embedUrl` - fields the backend never sends (see VideoBlock docs below).
export type ContentBlock =
  | { type: 'heading'; text: string }
  | { type: 'paragraph'; text: string }
  | { type: 'code'; language: string; text: string }
  | { type: 'video'; query: string }
  | { type: 'mcq'; question: string; options: string[]; answer: number; explanation: string }
  | { type: string; [key: string]: unknown }

type GeneratedLessonResponse = components['schemas']['LessonResponse']
export type LessonResponse = Omit<GeneratedLessonResponse, 'content'> & { content: ContentBlock[] }
