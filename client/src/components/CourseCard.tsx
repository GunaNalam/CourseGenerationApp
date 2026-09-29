import { Link } from 'react-router-dom'
import type { CourseResponse } from '../utils/api-types'
import { ChevronRightIcon } from './icons'

export function CourseCard({ course }: { course: CourseResponse }) {
  return (
    <Link
      to={`/courses/${course.id}`}
      className="group relative block overflow-hidden rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-4 shadow-[var(--shadow-sm)] transition hover:-translate-y-0.5 hover:border-[var(--color-primary)] hover:shadow-[var(--shadow-md)]"
    >
      <div
        className="absolute inset-x-0 top-0 h-1 opacity-0 transition-opacity group-hover:opacity-100"
        style={{ backgroundImage: 'var(--gradient-primary)' }}
        aria-hidden="true"
      />
      <div className="flex items-start justify-between gap-2">
        <h3 className="font-medium group-hover:text-[var(--color-primary)]">{course.title}</h3>
        <ChevronRightIcon className="mt-0.5 h-4 w-4 shrink-0 text-[var(--color-text-muted)] transition group-hover:translate-x-0.5 group-hover:text-[var(--color-primary)]" />
      </div>
      {course.description ? (
        <p className="mt-1 line-clamp-2 text-sm text-[var(--color-text-muted)]">{course.description}</p>
      ) : null}
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
    </Link>
  )
}
