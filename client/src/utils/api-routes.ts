const V1 = '/api/v1'

export const API_ROUTES = {
  courses: {
    list: () => `${V1}/courses`,
    generate: () => `${V1}/courses/generate`,
    get: (courseId: string) => `${V1}/courses/${courseId}`,
    export: (courseId: string) => `${V1}/courses/${courseId}/export`,
    tree: (courseId: string) => `${V1}/courses/${courseId}/tree`,
    modules: (courseId: string) => `${V1}/courses/${courseId}/modules`,
  },
  modules: {
    lessons: (courseId: string, moduleId: string) => `${V1}/courses/${courseId}/modules/${moduleId}/lessons`,
  },
  lessons: {
    get: (courseId: string, moduleId: string, lessonId: string) =>
      `${V1}/courses/${courseId}/modules/${moduleId}/lessons/${lessonId}`,
    update: (courseId: string, moduleId: string, lessonId: string) =>
      `${V1}/courses/${courseId}/modules/${moduleId}/lessons/${lessonId}`,
    audio: (courseId: string, moduleId: string, lessonId: string) =>
      `${V1}/courses/${courseId}/modules/${moduleId}/lessons/${lessonId}/audio`,
  },
  jobs: {
    status: (pipelineRunId: string) => `${V1}/jobs/${pipelineRunId}/status`,
  },
  users: {
    apiKey: () => `${V1}/users/me/api-key`,
  },
  admin: {
    stats: () => `${V1}/admin/stats`,
    jobs: () => `${V1}/admin/jobs`,
    errors: () => `${V1}/admin/errors`,
    cancelJob: (jobId: string) => `${V1}/admin/jobs/${jobId}/cancel`,
    cancelAll: () => `${V1}/admin/jobs/cancel-all`,
  },
} as const
