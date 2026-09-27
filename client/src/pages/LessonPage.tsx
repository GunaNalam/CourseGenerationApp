import { Link, useParams } from 'react-router-dom'
import { ErrorMessage } from '../components/ErrorMessage'
import { HinglishAudioButton } from '../components/HinglishAudioButton'
import { ArrowLeftIcon, ArrowRightIcon } from '../components/icons'
import { LessonPDFExporter } from '../components/LessonPDFExporter'
import { LessonRenderer } from '../components/LessonRenderer'
import { LessonStateToggle } from '../components/LessonStateToggle'
import { LoadingSpinner } from '../components/LoadingSpinner'
import { SidebarNavigation } from '../components/SidebarNavigation'
import { useCourseTree } from '../hooks/useCourseTree'
import { useLessonContent } from '../hooks/useLessonContent'

export function LessonPage() {
  const { courseId, lessonId } = useParams<{ courseId: string; moduleId: string; lessonId: string }>()
  const { course, modules, error, reload } = useCourseTree(courseId)

  const flatLessons = modules?.flatMap((module) => module.lessons) ?? []
  const currentIndex = flatLessons.findIndex((lesson) => lesson.id === lessonId)
  const lessonSummary = flatLessons[currentIndex]
  const currentModuleId = modules?.find((m) => m.lessons.some((l) => l.id === lessonId))?.id

  const { lesson: lessonContent, error: contentError } = useLessonContent(courseId, currentModuleId, lessonId)

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
        <LoadingSpinner label="Loading lesson…" />
      </div>
    )
  }

  if (!lessonSummary) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-10">
        <ErrorMessage message="Lesson not found." />
      </div>
    )
  }

  const previousLesson = currentIndex > 0 ? flatLessons[currentIndex - 1] : null
  const nextLesson = currentIndex < flatLessons.length - 1 ? flatLessons[currentIndex + 1] : null
  const moduleForLesson = (targetLessonId: string) =>
    modules.find((m) => m.lessons.some((l) => l.id === targetLessonId))?.id

  return (
    <div className="mx-auto flex max-w-5xl gap-8 px-4 py-10">
      <aside className="hidden w-56 shrink-0 md:block">
        <div className="sticky top-20">
          <Link
            to={`/courses/${courseId}`}
            className="mb-4 flex items-center gap-1.5 text-sm text-[var(--color-primary)] hover:underline"
          >
            <ArrowLeftIcon className="h-3.5 w-3.5" />
            {course.title}
          </Link>
          <SidebarNavigation courseId={courseId!} modules={modules} activeLessonId={lessonSummary.id} />
        </div>
      </aside>

      <main className="min-w-0 flex-1">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <h1 className="text-2xl font-semibold tracking-tight">{lessonSummary.title}</h1>
          <div className="flex flex-wrap gap-2">
            {lessonContent ? <LessonPDFExporter lesson={lessonContent} /> : null}
          </div>
        </div>

        <div className="mt-3">
          <LessonStateToggle
            key={lessonSummary.id}
            courseId={courseId!}
            moduleId={currentModuleId!}
            lessonId={lessonSummary.id}
            initialCompleted={lessonSummary.completed}
            initialBookmarked={lessonSummary.bookmarked}
          />
        </div>

        <div className="mt-3">
          <HinglishAudioButton courseId={courseId!} moduleId={currentModuleId!} lessonId={lessonSummary.id} />
        </div>

        {contentError ? (
          <div className="mt-6">
            <ErrorMessage message={contentError} />
          </div>
        ) : null}

        {!lessonContent && !contentError ? (
          <div className="mt-6">
            <LoadingSpinner label="Loading lesson content…" />
          </div>
        ) : null}

        {lessonContent && lessonContent.objectives.length > 0 ? (
          <div
            className="mt-4 rounded-xl border border-[var(--color-border)] p-4 shadow-[var(--shadow-sm)]"
            style={{ backgroundImage: 'var(--gradient-soft)' }}
          >
            <p className="mb-2 text-xs font-semibold tracking-widest text-[var(--color-primary)] uppercase">
              What you'll learn
            </p>
            <ul className="list-inside list-disc space-y-1 text-sm">
              {lessonContent.objectives.map((objective) => (
                <li key={objective}>{objective}</li>
              ))}
            </ul>
          </div>
        ) : null}

        {lessonContent ? (
          <div className="mt-6">
            <LessonRenderer content={lessonContent.content} />
          </div>
        ) : null}

        <div className="mt-10 flex items-center justify-between gap-3 border-t border-[var(--color-border)] pt-4 text-sm">
          {previousLesson ? (
            <Link
              to={`/courses/${courseId}/modules/${moduleForLesson(previousLesson.id)}/lessons/${previousLesson.id}`}
              className="group flex min-w-0 items-center gap-1.5 rounded-lg px-2 py-1.5 text-[var(--color-text-muted)] transition hover:bg-[var(--color-surface-hover)] hover:text-[var(--color-primary)]"
            >
              <ArrowLeftIcon className="h-4 w-4 shrink-0 transition group-hover:-translate-x-0.5" />
              <span className="truncate">{previousLesson.title}</span>
            </Link>
          ) : (
            <span />
          )}
          {nextLesson ? (
            <Link
              to={`/courses/${courseId}/modules/${moduleForLesson(nextLesson.id)}/lessons/${nextLesson.id}`}
              className="group flex min-w-0 items-center gap-1.5 rounded-lg px-2 py-1.5 text-right text-[var(--color-text-muted)] transition hover:bg-[var(--color-surface-hover)] hover:text-[var(--color-primary)]"
            >
              <span className="truncate">{nextLesson.title}</span>
              <ArrowRightIcon className="h-4 w-4 shrink-0 transition group-hover:translate-x-0.5" />
            </Link>
          ) : null}
        </div>
      </main>
    </div>
  )
}
