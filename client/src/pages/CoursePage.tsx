import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ErrorMessage } from '../components/ErrorMessage'
import { ExportCourseButton } from '../components/ExportCourseButton'
import { BookmarkIcon, CheckCircleIcon, ChevronRightIcon } from '../components/icons'
import { LoadingSpinner } from '../components/LoadingSpinner'
import { useCourseTree } from '../hooks/useCourseTree'
import type { LessonSummaryResponse } from '../utils/api-types'

type LessonFilter = 'all' | 'completed' | 'bookmarked'

const FILTERS: { value: LessonFilter; label: string }[] = [
  { value: 'all', label: 'All' },
  { value: 'completed', label: 'Completed' },
  { value: 'bookmarked', label: 'Bookmarked' },
]

function matchesFilter(lesson: LessonSummaryResponse, filter: LessonFilter): boolean {
  if (filter === 'completed') return lesson.completed
  if (filter === 'bookmarked') return lesson.bookmarked
  return true
}

export function CoursePage() {
  const { courseId } = useParams<{ courseId: string }>()
  const { course, modules, error, reload } = useCourseTree(courseId)
  const [filter, setFilter] = useState<LessonFilter>('all')

  if (error) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-10">
        <ErrorMessage message={error} onRetry={reload} />
      </div>
    )
  }

  if (!course || !modules) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-10">
        <LoadingSpinner label="Loading course…" />
      </div>
    )
  }

  const visibleModules = modules
    .map((module) => ({ ...module, lessons: module.lessons.filter((lesson) => matchesFilter(lesson, filter)) }))
    .filter((module) => filter === 'all' || module.lessons.length > 0)

  return (
    <div className="mx-auto max-w-3xl px-4 py-10">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <h1 className="text-2xl font-semibold tracking-tight">{course.title}</h1>
        <ExportCourseButton courseId={courseId!} fileName={course.title} />
      </div>
      {course.description ? <p className="mt-2 text-[var(--color-text-muted)]">{course.description}</p> : null}

      {course.tags.length > 0 ? (
        <div className="mt-3 flex flex-wrap gap-1.5">
          {course.tags.map((tag) => (
            <span
              key={tag}
              className="rounded-full bg-[var(--color-primary-soft)] px-2 py-0.5 text-xs text-[var(--color-primary)]"
            >
              {tag}
            </span>
          ))}
        </div>
      ) : null}

      <div className="mt-6 flex gap-2">
        {FILTERS.map((option) => (
          <button
            key={option.value}
            type="button"
            onClick={() => setFilter(option.value)}
            className={`flex items-center gap-1.5 rounded-full border px-3 py-1 text-xs font-medium transition ${
              filter === option.value
                ? 'border-[var(--color-primary)] bg-[var(--color-primary-soft)] text-[var(--color-primary)] shadow-[var(--shadow-sm)]'
                : 'border-[var(--color-border)] bg-[var(--color-surface)] text-[var(--color-text-muted)] hover:border-[var(--color-primary)] hover:text-[var(--color-primary)]'
            }`}
          >
            {option.value === 'completed' ? <CheckCircleIcon className="h-3.5 w-3.5" /> : null}
            {option.value === 'bookmarked' ? <BookmarkIcon className="h-3.5 w-3.5" filled={filter === 'bookmarked'} /> : null}
            {option.label}
          </button>
        ))}
      </div>

      <div className="mt-6 flex flex-col gap-6">
        {visibleModules.length === 0 ? (
          <p className="text-sm text-[var(--color-text-muted)]">No lessons match this filter yet.</p>
        ) : null}

        {visibleModules.map((module) => {
          const moduleIndex = modules.findIndex((m) => m.id === module.id)
          return (
            <div key={module.id}>
              <h2 className="mb-2 flex items-center gap-2 text-sm font-medium text-[var(--color-text-muted)]">
                <span
                  className="flex h-5 w-5 items-center justify-center rounded-md text-[10px] font-semibold text-[var(--color-primary-text)]"
                  style={{ backgroundImage: 'var(--gradient-primary)' }}
                >
                  {moduleIndex + 1}
                </span>
                {module.title}
              </h2>
              <div className="flex flex-col divide-y divide-[var(--color-border)] rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] shadow-[var(--shadow-sm)]">
                {module.lessons.map((lesson) => {
                  const lessonIndex = modules[moduleIndex].lessons.findIndex((l) => l.id === lesson.id)
                  return (
                    <Link
                      key={lesson.id}
                      to={`/courses/${courseId}/modules/${module.id}/lessons/${lesson.id}`}
                      className="group flex items-center justify-between gap-3 px-4 py-3 text-sm transition first:rounded-t-2xl last:rounded-b-2xl hover:bg-[var(--color-surface-hover)]"
                    >
                      <span className="flex min-w-0 items-center gap-2.5">
                        <span className="shrink-0 text-xs tabular-nums text-[var(--color-text-muted)]">
                          {lessonIndex + 1}
                        </span>
                        <span className="truncate">{lesson.title}</span>
                      </span>
                      <span className="flex shrink-0 items-center gap-2 text-xs">
                        {lesson.bookmarked ? (
                          <BookmarkIcon filled className="h-3.5 w-3.5 text-[var(--color-warning)]" />
                        ) : null}
                        {lesson.completed ? (
                          <span className="flex items-center gap-1 text-[var(--color-success)]">
                            <CheckCircleIcon filled className="h-3.5 w-3.5" />
                            Completed
                          </span>
                        ) : lesson.isEnriched ? (
                          <span className="text-[var(--color-text-muted)]">Ready</span>
                        ) : (
                          <span className="text-[var(--color-warning)]">Preparing…</span>
                        )}
                        <ChevronRightIcon className="h-3.5 w-3.5 text-[var(--color-text-muted)] opacity-0 transition group-hover:translate-x-0.5 group-hover:opacity-100" />
                      </span>
                    </Link>
                  )
                })}
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}
