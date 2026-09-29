import { Link } from 'react-router-dom'
import type { ModuleWithLessons } from '../hooks/useCourseTree'

interface SidebarNavigationProps {
  courseId: string
  modules: ModuleWithLessons[]
  activeLessonId: string
}

export function SidebarNavigation({ courseId, modules, activeLessonId }: SidebarNavigationProps) {
  return (
    <nav className="flex flex-col gap-4">
      {modules.map((module, moduleIndex) => (
        <div key={module.id}>
          <p className="mb-1 px-2 text-xs font-medium tracking-wide text-[var(--color-text-muted)] uppercase">
            Module {moduleIndex + 1}
          </p>
          <div className="flex flex-col">
            {module.lessons.map((lesson, lessonIndex) => {
              const isActive = lesson.id === activeLessonId
              return (
                <Link
                  key={lesson.id}
                  to={`/courses/${courseId}/modules/${module.id}/lessons/${lesson.id}`}
                  className={`relative rounded-lg px-2 py-1.5 text-sm transition ${
                    isActive
                      ? 'bg-[var(--color-primary-soft)] font-medium text-[var(--color-primary)]'
                      : 'text-[var(--color-text-muted)] hover:bg-[var(--color-surface-hover)] hover:text-[var(--color-text)]'
                  }`}
                >
                  {isActive ? (
                    <span
                      className="absolute top-1/2 -left-2 h-4 w-0.5 -translate-y-1/2 rounded-full"
                      style={{ backgroundImage: 'var(--gradient-primary)' }}
                      aria-hidden="true"
                    />
                  ) : null}
                  {lessonIndex + 1}. {lesson.title}
                </Link>
              )
            })}
          </div>
        </div>
      ))}
    </nav>
  )
}
