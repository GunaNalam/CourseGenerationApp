import { useState } from 'react'
import type { FormEvent } from 'react'
import { SparklesIcon } from './icons'

interface PromptFormProps {
  onSubmit: (topic: string) => void
  disabled?: boolean
}

const EXAMPLE_TOPICS = ['Intro to React Hooks', 'Basics of Copyright Law', 'How Neural Networks Work']

export function PromptForm({ onSubmit, disabled }: PromptFormProps) {
  const [topic, setTopic] = useState('')

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const trimmed = topic.trim()
    if (!trimmed || disabled) return
    onSubmit(trimmed)
  }

  return (
    <form onSubmit={handleSubmit} className="w-full">
      <div className="flex flex-col gap-3 rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-4 shadow-[var(--shadow-md)] transition focus-within:border-[var(--color-primary)] sm:flex-row sm:items-center">
        <SparklesIcon className="hidden h-5 w-5 shrink-0 text-[var(--color-primary)] sm:block" />
        <input
          value={topic}
          onChange={(e) => setTopic(e.target.value)}
          disabled={disabled}
          placeholder="What do you want to learn? e.g. Intro to React Hooks"
          className="flex-1 rounded-xl border border-transparent bg-transparent px-2 py-2 text-base outline-none placeholder:text-[var(--color-text-muted)]"
        />
        <button
          type="submit"
          disabled={disabled || !topic.trim()}
          className="rounded-xl px-5 py-2.5 font-medium text-[var(--color-primary-text)] shadow-[var(--shadow-glow)] transition hover:brightness-110 disabled:cursor-not-allowed disabled:opacity-40 disabled:shadow-none"
          style={{ backgroundImage: 'var(--gradient-primary)' }}
        >
          Generate course
        </button>
      </div>

      <div className="mt-3 flex flex-wrap gap-2">
        {EXAMPLE_TOPICS.map((example) => (
          <button
            key={example}
            type="button"
            disabled={disabled}
            onClick={() => setTopic(example)}
            className="rounded-full border border-[var(--color-border)] bg-[var(--color-surface)] px-3 py-1 text-xs text-[var(--color-text-muted)] shadow-[var(--shadow-sm)] transition hover:border-[var(--color-primary)] hover:text-[var(--color-primary)] disabled:opacity-50"
          >
            {example}
          </button>
        ))}
      </div>
    </form>
  )
}
