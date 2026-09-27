import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { CourseCard } from '../components/CourseCard'
import { ErrorMessage } from '../components/ErrorMessage'
import { GenerationProgress } from '../components/GenerationProgress'
import { LoadingSpinner } from '../components/LoadingSpinner'
import { PromptForm } from '../components/PromptForm'
import { useApi } from '../hooks/useApi'
import { useJobPolling } from '../hooks/useJobPolling'
import { API_ROUTES } from '../utils/api-routes'
import type { CourseResponse, GenerateCourseResponse } from '../utils/api-types'

const PIPELINE_RUN_ID_STORAGE_KEY = 'texttolearn.activePipelineRunId'

export function Home() {
  const api = useApi()
  const navigate = useNavigate()

  const [courses, setCourses] = useState<CourseResponse[] | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [pipelineRunId, setPipelineRunId] = useState<string | null>(() =>
    localStorage.getItem(PIPELINE_RUN_ID_STORAGE_KEY),
  )
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const { status: jobStatus, error: pollError } = useJobPolling(pipelineRunId)

  useEffect(() => {
    if (pipelineRunId) {
      localStorage.setItem(PIPELINE_RUN_ID_STORAGE_KEY, pipelineRunId)
    } else {
      localStorage.removeItem(PIPELINE_RUN_ID_STORAGE_KEY)
    }
  }, [pipelineRunId])

  const loadCourses = () => {
    setLoadError(null)
    api
      .get<CourseResponse[]>(API_ROUTES.courses.list())
      .then(setCourses)
      .catch((e) => setLoadError(e instanceof Error ? e.message : 'Failed to load your courses'))
  }

  useEffect(loadCourses, []) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (jobStatus?.status === 'DONE' && jobStatus.courseId) {
      setPipelineRunId(null)
      navigate(`/courses/${jobStatus.courseId}`)
    } else if (jobStatus?.status === 'FAILED' || jobStatus?.status === 'CANCELLED') {
      localStorage.removeItem(PIPELINE_RUN_ID_STORAGE_KEY)
    }
  }, [jobStatus, navigate])

  async function handleGenerate(topic: string) {
    if (submitting) return
    setSubmitting(true)
    setSubmitError(null)
    try {
      const result = await api.post<GenerateCourseResponse>(API_ROUTES.courses.generate(), { topic })
      setPipelineRunId(result.pipelineRunId)
    } catch (e) {
      setSubmitError(e instanceof Error ? e.message : 'Could not start generation')
    } finally {
      setSubmitting(false)
    }
  }

  const isGenerating =
    submitting ||
    (pipelineRunId !== null && jobStatus?.status !== 'FAILED' && jobStatus?.status !== 'CANCELLED')

  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-8 px-4 py-10">
      <div>
        <h1 className="text-3xl font-semibold tracking-tight">What do you want to learn today?</h1>
        <p className="mt-2 text-[var(--color-text-muted)]">
          Describe a topic and we'll build a structured course for you.
        </p>
      </div>

      <PromptForm onSubmit={handleGenerate} disabled={isGenerating} />

      {submitError ? <ErrorMessage message={submitError} /> : null}
      {pollError ? <ErrorMessage message={pollError} /> : null}

      {jobStatus && jobStatus.status !== 'FAILED' && jobStatus.status !== 'DONE' && jobStatus.status !== 'CANCELLED' ? (
        <GenerationProgress status={jobStatus} />
      ) : null}

      {jobStatus?.status === 'FAILED' ? (
        <ErrorMessage
          message="Something went wrong generating this course. Please try again — if it keeps happening, an admin can check /admin for the exact cause."
          onRetry={() => setPipelineRunId(null)}
        />
      ) : null}

      {jobStatus?.status === 'CANCELLED' ? (
        <ErrorMessage
          message="This generation was cancelled by an admin."
          onRetry={() => setPipelineRunId(null)}
        />
      ) : null}

      <div>
        <h2 className="mb-3 text-xs font-semibold tracking-widest text-[var(--color-text-muted)] uppercase">
          Your courses
        </h2>

        {courses === null && !loadError ? <LoadingSpinner label="Loading your courses…" /> : null}
        {loadError ? <ErrorMessage message={loadError} onRetry={loadCourses} /> : null}

        {courses && courses.length === 0 ? (
          <p className="text-sm text-[var(--color-text-muted)]">
            You haven't generated any courses yet — try the form above.
          </p>
        ) : null}

        {courses && courses.length > 0 ? (
          <div className="grid gap-4 sm:grid-cols-2">
            {courses.map((course) => (
              <CourseCard key={course.id} course={course} />
            ))}
          </div>
        ) : null}
      </div>
    </div>
  )
}
