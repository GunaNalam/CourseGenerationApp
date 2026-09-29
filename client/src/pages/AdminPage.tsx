import { useEffect, useState } from 'react'
import { ErrorMessage } from '../components/ErrorMessage'
import { ShieldIcon } from '../components/icons'
import { LoadingSpinner } from '../components/LoadingSpinner'
import { useApi } from '../hooks/useApi'
import { ApiError } from '../utils/api'
import { API_ROUTES } from '../utils/api-routes'
import type { AdminStatsResponse, JobErrorSummary, PipelineRunSummary } from '../utils/api-types'

const ACTIVE_STATUSES = new Set(['PENDING', 'RUNNING'])

export function AdminPage() {
  const api = useApi()
  const [stats, setStats] = useState<AdminStatsResponse | null>(null)
  const [jobs, setJobs] = useState<PipelineRunSummary[] | null>(null)
  const [errors, setErrors] = useState<JobErrorSummary[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [forbidden, setForbidden] = useState(false)
  const [cancellingId, setCancellingId] = useState<string | null>(null)
  const [cancellingAll, setCancellingAll] = useState(false)

  const loadAll = () => {
    Promise.all([
      api.get<AdminStatsResponse>(API_ROUTES.admin.stats()),
      api.get<PipelineRunSummary[]>(API_ROUTES.admin.jobs()),
      api.get<JobErrorSummary[]>(API_ROUTES.admin.errors()),
    ])
      .then(([s, j, e]) => {
        setStats(s)
        setJobs(j)
        setErrors(e)
      })
      .catch((e) => {
        if (e instanceof ApiError && e.status === 403) {
          setForbidden(true)
        } else {
          setError(e instanceof Error ? e.message : 'Failed to load admin data')
        }
      })
  }

  useEffect(loadAll, []) // eslint-disable-line react-hooks/exhaustive-deps

  async function cancelJob(jobId: string) {
    setCancellingId(jobId)
    try {
      await api.post<void>(API_ROUTES.admin.cancelJob(jobId))
      loadAll()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to cancel job')
    } finally {
      setCancellingId(null)
    }
  }

  async function cancelAll() {
    setCancellingAll(true)
    try {
      await api.post<number>(API_ROUTES.admin.cancelAll())
      loadAll()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to cancel active jobs')
    } finally {
      setCancellingAll(false)
    }
  }

  if (forbidden) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-10">
        <ErrorMessage message="You don't have admin access." />
      </div>
    )
  }

  if (error) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-10">
        <ErrorMessage message={error} />
      </div>
    )
  }

  if (!stats || !jobs || !errors) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-10">
        <LoadingSpinner label="Loading admin dashboard…" />
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-10">
      <div className="mb-6 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <ShieldIcon className="h-6 w-6 text-[var(--color-primary)]" />
          <h1 className="text-3xl font-semibold tracking-tight">Admin</h1>
        </div>
        <button
          type="button"
          onClick={cancelAll}
          disabled={cancellingAll || !jobs.some((j) => ACTIVE_STATUSES.has(j.status))}
          className="rounded-full border border-[var(--color-danger)]/30 bg-[var(--color-danger-soft)] px-3 py-1.5 text-xs font-medium text-[var(--color-danger)] shadow-[var(--shadow-sm)] transition hover:opacity-80 disabled:cursor-not-allowed disabled:opacity-40"
        >
          {cancellingAll ? 'Cancelling…' : 'Cancel all active jobs'}
        </button>
      </div>

      <div className="mb-8 grid grid-cols-3 gap-3">
        <StatCard label="Total runs" value={stats.totalRuns} />
        <StatCard label="Completed" value={stats.doneRuns} accent="success" />
        <StatCard label="Failed" value={stats.failedRuns} accent="danger" />
      </div>

      <section className="mb-8">
        <h2 className="mb-3 text-xs font-semibold tracking-widest text-[var(--color-text-muted)] uppercase">
          Recent jobs
        </h2>
        <div className="flex flex-col divide-y divide-[var(--color-border)] rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] shadow-[var(--shadow-sm)]">
          {jobs.length === 0 ? (
            <p className="px-4 py-3 text-sm text-[var(--color-text-muted)]">No generation jobs yet.</p>
          ) : (
            jobs.map((job) => (
              <div key={job.id} className="flex items-center justify-between px-4 py-2.5 text-sm">
                <span className="font-mono text-xs text-[var(--color-text-muted)]">{job.id.slice(0, 8)}</span>
                <div className="flex items-center gap-2">
                  <StatusBadge status={job.status} />
                  {ACTIVE_STATUSES.has(job.status) ? (
                    <button
                      type="button"
                      onClick={() => cancelJob(job.id)}
                      disabled={cancellingId === job.id}
                      className="text-xs font-medium text-[var(--color-danger)] underline decoration-dotted underline-offset-2 hover:opacity-80 disabled:cursor-not-allowed disabled:opacity-40"
                    >
                      {cancellingId === job.id ? 'Cancelling…' : 'Cancel'}
                    </button>
                  ) : null}
                </div>
              </div>
            ))
          )}
        </div>
      </section>

      <section>
        <h2 className="mb-3 text-xs font-semibold tracking-widest text-[var(--color-text-muted)] uppercase">
          Recent errors
        </h2>
        <div className="flex flex-col gap-2">
          {errors.length === 0 ? (
            <p className="text-sm text-[var(--color-text-muted)]">No errors — nice.</p>
          ) : (
            errors.map((jobError) => (
              <div
                key={jobError.stepId}
                className="rounded-xl border border-[var(--color-danger)]/30 bg-[var(--color-danger-soft)] px-4 py-3 text-sm shadow-[var(--shadow-sm)]"
              >
                <p className="font-medium text-[var(--color-danger)]">
                  {jobError.type} (attempt {jobError.attempt})
                </p>
                <p className="mt-1 text-[var(--color-text-muted)]">{jobError.error}</p>
              </div>
            ))
          )}
        </div>
      </section>
    </div>
  )
}

function StatCard({ label, value, accent }: { label: string; value: number; accent?: 'success' | 'danger' }) {
  const color = accent ? `var(--color-${accent})` : 'var(--color-text)'
  return (
    <div className="rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-4 text-center shadow-[var(--shadow-sm)]">
      <p className="text-2xl font-semibold" style={{ color }}>
        {value}
      </p>
      <p className="text-xs text-[var(--color-text-muted)]">{label}</p>
    </div>
  )
}

function StatusBadge({ status }: { status: string }) {
  const styles: Record<string, string> = {
    DONE: 'text-[var(--color-success)] bg-[var(--color-success-soft)]',
    FAILED: 'text-[var(--color-danger)] bg-[var(--color-danger-soft)]',
    RUNNING: 'text-[var(--color-primary)] bg-[var(--color-primary-soft)]',
    PENDING: 'text-[var(--color-warning)] bg-[var(--color-warning-soft)]',
    CANCELLED: 'text-[var(--color-text-muted)] bg-[var(--color-border)]',
  }
  return (
    <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${styles[status] ?? ''}`}>{status}</span>
  )
}
