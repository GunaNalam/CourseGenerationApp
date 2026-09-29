import { useEffect, useRef, useState } from 'react'
import { useApi } from './useApi'
import { API_ROUTES } from '../utils/api-routes'
import type { LessonResponse } from '../utils/api-types'

/**
 * Fetches full lesson content (body + objectives) on demand, one lesson at a
 * time, instead of the course tree loading every lesson's content upfront.
 * The cache is a ref so it survives re-renders for as long as the calling
 * component stays mounted — which covers navigating between lessons via
 * prev/next or the sidebar, since that's a param change on the same route,
 * not a remount.
 */
export function useLessonContent(courseId: string | undefined, moduleId: string | undefined, lessonId: string | undefined) {
  const api = useApi()
  const cacheRef = useRef<Map<string, LessonResponse>>(new Map())
  const [lesson, setLesson] = useState<LessonResponse | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!courseId || !moduleId || !lessonId) return

    const cached = cacheRef.current.get(lessonId)
    if (cached) {
      setLesson(cached)
      setError(null)
      return
    }

    let cancelled = false
    setLesson(null)
    setError(null)

    api
      .get<LessonResponse>(API_ROUTES.lessons.get(courseId, moduleId, lessonId))
      .then((result) => {
        if (cancelled) return
        cacheRef.current.set(lessonId, result)
        setLesson(result)
      })
      .catch((e) => {
        if (!cancelled) setError(e instanceof Error ? e.message : 'Failed to load lesson content')
      })

    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [courseId, moduleId, lessonId])

  return { lesson, error }
}
