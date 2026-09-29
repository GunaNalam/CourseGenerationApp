import type { JobStatusResponse } from '../utils/api-types'
import { LoadingSpinner } from './LoadingSpinner'

const STEP_LABELS: Record<string, string> = {
  OUTLINE: 'Designing course outline',
  LESSON_CONTENT: 'Writing lesson content',
  ENRICHMENT: 'Adding videos & finishing touches',
}

interface GenerationProgressProps {
  status: JobStatusResponse
}

const STEP_ORDER = ['OUTLINE', 'LESSON_CONTENT', 'ENRICHMENT']

export function GenerationProgress({ status }: GenerationProgressProps) {
  const stepLabel = status.currentStep ? STEP_LABELS[status.currentStep] ?? status.currentStep : 'Getting started'
  const activeIndex = status.currentStep ? STEP_ORDER.indexOf(status.currentStep) : -1

  return (
    <div className="relative overflow-hidden rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-5 shadow-[var(--shadow-md)]">
      <div
        className="absolute inset-x-0 top-0 h-0.5 animate-pulse opacity-70"
        style={{ backgroundImage: 'var(--gradient-primary)' }}
        aria-hidden="true"
      />

      <div className="flex items-center gap-3">
        <LoadingSpinner size="md" />
        <div>
          <p className="font-medium">{stepLabel}…</p>
          {status.position > 0 ? (
            <p className="text-sm text-[var(--color-text-muted)]">{status.position} job(s) ahead of yours in the queue</p>
          ) : (
            <p className="text-sm text-[var(--color-text-muted)]">This can take a minute — hang tight.</p>
          )}
        </div>
      </div>

      <div className="mt-4 flex items-center gap-1.5">
        {STEP_ORDER.map((step, index) => (
          <span
            key={step}
            className="h-1 flex-1 rounded-full transition-colors"
            style={{
              backgroundColor:
                index < activeIndex
                  ? 'var(--color-primary)'
                  : index === activeIndex
                    ? 'var(--color-primary)'
                    : 'var(--color-border)',
              opacity: index === activeIndex ? 1 : index < activeIndex ? 0.6 : 1,
            }}
          />
        ))}
      </div>

      {status.usingDefaultKey ? (
        <p className="mt-3 rounded-lg bg-[var(--color-warning-soft)] px-3 py-2 text-xs text-[var(--color-warning)]">
          Your API key didn't work, so this course is being generated with the default key instead.
          {status.keyFallbackReason ? ` (${status.keyFallbackReason})` : ''}
        </p>
      ) : null}
    </div>
  )
}
