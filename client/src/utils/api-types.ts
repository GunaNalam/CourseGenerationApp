export interface CourseResponse {
  id: string
  title: string
  description: string | null
  tags: string[]
  createdAt: string
}

export interface ModuleResponse {
  id: string
  title: string
  orderIndex: number
}

export type ContentBlock =
  | { type: 'heading'; text: string }
  | { type: 'paragraph'; text: string }
  | { type: 'code'; language: string; text: string }
  | { type: 'video'; query: string; videoId?: string; embedUrl?: string }
  | { type: 'mcq'; question: string; options: string[]; answer: number; explanation: string }
  | { type: string; [key: string]: unknown }

export interface LessonSummaryResponse {
  id: string
  title: string
  orderIndex: number
  isEnriched: boolean
  completed: boolean
  bookmarked: boolean
}

export interface LessonResponse extends LessonSummaryResponse {
  objectives: string[]
  content: ContentBlock[]
}

export interface ModuleWithLessonsResponse extends ModuleResponse {
  lessons: LessonSummaryResponse[]
}

export interface CourseTreeResponse extends CourseResponse {
  modules: ModuleWithLessonsResponse[]
}

export interface UpdateLessonStateRequest {
  completed?: boolean
  bookmarked?: boolean
}

export interface GenerateCourseRequest {
  topic: string
}

export interface GenerateCourseResponse {
  pipelineRunId: string
}

export type PipelineStatus = 'PENDING' | 'RUNNING' | 'DONE' | 'FAILED' | 'CANCELLED'

export interface JobStatusResponse {
  pipelineRunId: string
  status: PipelineStatus
  currentStep: string | null
  position: number
  courseId: string | null
  error: string | null
  usingDefaultKey: boolean
  keyFallbackReason: string | null
}

export interface ApiKeyStatusResponse {
  configured: boolean
}

export interface CourseExportModule {
  title: string
  orderIndex: number
  lessons: (LessonResponse & { orderIndex: number })[]
}

export interface CourseExportResponse {
  title: string
  description: string | null
  tags: string[]
  modules: CourseExportModule[]
}

export interface PipelineRunSummary {
  id: string
  ownerId: string
  status: PipelineStatus
  courseId: string | null
  createdAt: string
}

export interface JobErrorSummary {
  stepId: string
  pipelineRunId: string
  type: string
  error: string | null
  attempt: number
  createdAt: string
}

export interface AdminStatsResponse {
  totalRuns: number
  doneRuns: number
  failedRuns: number
}
