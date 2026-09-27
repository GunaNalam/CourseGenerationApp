import { useEffect, useRef, useState } from 'react'
import { useApi } from './useApi'
import { API_ROUTES } from '../utils/api-routes'
import type { JobStatusResponse } from '../utils/api-types'

const POLL_INTERVAL_MS = 2000

export function useJobPolling(pipelineRunId: string | null) {
  const api = useApi()
  const [status, setStatus] = useState<JobStatusResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const intervalRef = useRef<number | undefined>(undefined)

  useEffect(() => {
    setStatus(null)
    setError(null)
    if (!pipelineRunId) {
      return
    }

    let cancelled = false

    const poll = async () => {
      try {
        const result = await api.get<JobStatusResponse>(API_ROUTES.jobs.status(pipelineRunId))
        if (cancelled) return
        setStatus(result)
        if (result.status === 'DONE' || result.status === 'FAILED' || result.status === 'CANCELLED') {
          window.clearInterval(intervalRef.current)
        }
      } catch (e) {
        if (!cancelled) {
          setError(e instanceof Error ? e.message : 'Failed to fetch generation status')
          window.clearInterval(intervalRef.current)
        }
      }
    }

    poll()
    intervalRef.current = window.setInterval(poll, POLL_INTERVAL_MS)

    return () => {
      cancelled = true
      window.clearInterval(intervalRef.current)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pipelineRunId])

  return { status, error }
}
