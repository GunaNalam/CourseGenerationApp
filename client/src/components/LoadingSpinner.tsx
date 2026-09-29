interface LoadingSpinnerProps {
  label?: string
  size?: 'sm' | 'md' | 'lg'
}

const SIZE_CLASSES: Record<NonNullable<LoadingSpinnerProps['size']>, string> = {
  sm: 'h-4 w-4 border-2',
  md: 'h-6 w-6 border-2',
  lg: 'h-10 w-10 border-[3px]',
}

export function LoadingSpinner({ label, size = 'md' }: LoadingSpinnerProps) {
  return (
    <div className="flex items-center gap-3 text-[var(--color-text-muted)]">
      <span
        className={`inline-block animate-spin rounded-full border-[var(--color-border)] border-t-[var(--color-primary)] border-r-[var(--color-accent)] ${SIZE_CLASSES[size]}`}
        role="status"
        aria-label={label ?? 'Loading'}
      />
      {label ? <span className="text-sm">{label}</span> : null}
    </div>
  )
}
