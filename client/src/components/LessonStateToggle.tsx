import { useState } from 'react'
import { useApi } from '../hooks/useApi'
import { API_ROUTES } from '../utils/api-routes'
import type { LessonResponse, UpdateLessonStateRequest } from '../utils/api-types'
import { BookmarkIcon, CheckCircleIcon } from './icons'

interface LessonStateToggleProps {
  courseId: string
  moduleId: string
  lessonId: string
  initialCompleted: boolean
  initialBookmarked: boolean
}

export function LessonStateToggle({
  courseId,
  moduleId,
  lessonId,
  initialCompleted,
  initialBookmarked,
}: LessonStateToggleProps) {
  const api = useApi()
  const [completed, setCompleted] = useState(initialCompleted)
  const [bookmarked, setBookmarked] = useState(initialBookmarked)
  const [saving, setSaving] = useState(false)

  async function update(change: UpdateLessonStateRequest) {
    setSaving(true)
    // Optimistic update — reverted if the request fails.
    const previous = { completed, bookmarked }
    if (change.completed !== undefined) setCompleted(change.completed)
    if (change.bookmarked !== undefined) setBookmarked(change.bookmarked)

    try {
      await api.patch<LessonResponse>(API_ROUTES.lessons.update(courseId, moduleId, lessonId), change)
    } catch {
      setCompleted(previous.completed)
      setBookmarked(previous.bookmarked)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="flex flex-wrap gap-2">
      <button
        type="button"
        onClick={() => update({ completed: !completed })}
        disabled={saving}
        className={`flex items-center gap-1.5 rounded-lg border px-3 py-1.5 text-sm shadow-[var(--shadow-sm)] transition disabled:opacity-50 ${
          completed
            ? 'border-[var(--color-success)] bg-[var(--color-success-soft)] text-[var(--color-success)]'
            : 'border-[var(--color-border)] bg-[var(--color-surface)] text-[var(--color-text-muted)] hover:border-[var(--color-primary)] hover:text-[var(--color-primary)]'
        }`}
      >
        <CheckCircleIcon filled={completed} className="h-4 w-4" />
        {completed ? 'Completed' : 'Mark as completed'}
      </button>

      <button
        type="button"
        onClick={() => update({ bookmarked: !bookmarked })}
        disabled={saving}
        className={`flex items-center gap-1.5 rounded-lg border px-3 py-1.5 text-sm shadow-[var(--shadow-sm)] transition disabled:opacity-50 ${
          bookmarked
            ? 'border-[var(--color-warning)] bg-[var(--color-warning-soft)] text-[var(--color-warning)]'
            : 'border-[var(--color-border)] bg-[var(--color-surface)] text-[var(--color-text-muted)] hover:border-[var(--color-primary)] hover:text-[var(--color-primary)]'
        }`}
      >
        <BookmarkIcon filled={bookmarked} className="h-4 w-4" />
        {bookmarked ? 'Bookmarked' : 'Bookmark'}
      </button>
    </div>
  )
}
