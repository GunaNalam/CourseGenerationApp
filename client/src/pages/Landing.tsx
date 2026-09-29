import { useAuth0 } from '@auth0/auth0-react'
import { SparklesIcon } from '../components/icons'

const FEATURES = [
  { title: 'Structured, multi-module courses', description: 'Full outlines with modules and lessons, not a wall of text.' },
  { title: 'Videos & real examples', description: 'Enriched automatically with relevant YouTube videos per lesson.' },
  { title: 'Track your progress', description: 'Bookmark lessons, mark them complete, export as PDF or JSON.' },
]

export function Landing() {
  const { loginWithRedirect } = useAuth0()

  return (
    <div className="relative flex min-h-screen flex-col items-center overflow-hidden px-4 py-24">
      <div
        className="pointer-events-none absolute -top-40 left-1/2 h-[32rem] w-[32rem] -translate-x-1/2 rounded-full opacity-30 blur-3xl"
        style={{ backgroundImage: 'var(--gradient-primary)' }}
        aria-hidden="true"
      />

      <div className="relative mx-auto flex max-w-2xl flex-col items-center text-center">
        <span
          className="mb-6 flex h-16 w-16 items-center justify-center rounded-2xl text-2xl font-semibold text-[var(--color-primary-text)] shadow-[var(--shadow-glow)]"
          style={{ backgroundImage: 'var(--gradient-primary)' }}
        >
          L
        </span>

        <span className="mb-3 inline-flex items-center gap-1.5 rounded-full border border-[var(--color-border)] bg-[var(--color-surface)] px-3 py-1 text-xs font-medium text-[var(--color-text-muted)] shadow-[var(--shadow-sm)]">
          <SparklesIcon className="h-3.5 w-3.5 text-[var(--color-primary)]" />
          AI-powered course generator
        </span>

        <h1 className="text-4xl font-semibold tracking-tight sm:text-5xl">
          Turn any topic into a{' '}
          <span
            className="bg-clip-text text-transparent"
            style={{ backgroundImage: 'var(--gradient-primary)' }}
          >
            full course
          </span>
        </h1>
        <p className="mt-4 max-w-lg text-[var(--color-text-muted)]">
          Type a topic, get a structured, multi-module course with lessons, examples, videos, and quizzes — generated
          for you in minutes.
        </p>
        <button
          type="button"
          onClick={() => loginWithRedirect()}
          className="mt-8 rounded-xl px-6 py-3 font-medium text-[var(--color-primary-text)] shadow-[var(--shadow-glow)] transition hover:scale-[1.02] hover:brightness-110 active:scale-[0.99]"
          style={{ backgroundImage: 'var(--gradient-primary)' }}
        >
          Log in to get started
        </button>

        <div className="mt-16 grid w-full gap-3 sm:grid-cols-3">
          {FEATURES.map((feature) => (
            <div
              key={feature.title}
              className="rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)]/60 p-4 text-left shadow-[var(--shadow-sm)] backdrop-blur-sm"
            >
              <p className="text-sm font-medium">{feature.title}</p>
              <p className="mt-1 text-xs text-[var(--color-text-muted)]">{feature.description}</p>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
