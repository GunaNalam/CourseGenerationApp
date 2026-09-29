import { useEffect, useState } from 'react'
import { useApi } from './useApi'
import { API_ROUTES } from '../utils/api-routes'
import type { CourseResponse, CourseTreeResponse, ModuleWithLessonsResponse } from '../utils/api-types'

export type ModuleWithLessons = ModuleWithLessonsResponse

export function useCourseTree(courseId: string | undefined) {
  const api = useApi()
  const [course, setCourse] = useState<CourseResponse | null>(null)
  const [modules, setModules] = useState<ModuleWithLessons[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [reloadToken, setReloadToken] = useState(0)

  useEffect(() => {
    if (!courseId) return
    let cancelled = false
    setError(null)
    setCourse(null)
    setModules(null)

    api
      .get<CourseTreeResponse>(API_ROUTES.courses.tree(courseId))
      .then(({ modules: treeModules, ...courseFields }) => {
        if (cancelled) return
        setCourse(courseFields)
        setModules(treeModules)
      })
      .catch((e) => {
        if (!cancelled) setError(e instanceof Error ? e.message : 'Failed to load course')
      })

    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [courseId, reloadToken])

  return { course, modules, error, reload: () => setReloadToken((t) => t + 1) }
}
