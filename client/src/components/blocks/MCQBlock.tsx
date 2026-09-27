import { useState } from 'react'

interface MCQBlockProps {
  question: string
  options: string[]
  answer: number
  explanation: string
}

export function MCQBlock({ question, options, answer, explanation }: MCQBlockProps) {
  const [selected, setSelected] = useState<number | null>(null)

  const answered = selected !== null
  const isCorrect = selected === answer

  return (
    <div className="mb-4 rounded-xl border border-[var(--color-border)] bg-[var(--color-surface)] p-4">
      <p className="mb-3 font-medium text-[var(--color-text)]">{question}</p>

      <div className="flex flex-col gap-2">
        {options.map((option, index) => {
          const isSelected = selected === index
          const isAnswer = index === answer

          let stateClasses = 'border-[var(--color-border)] text-[var(--color-text)] hover:border-[var(--color-primary)]'
          if (answered && isAnswer) {
            stateClasses = 'border-[var(--color-success)] bg-[var(--color-success-soft)] text-[var(--color-success)]'
          } else if (answered && isSelected && !isCorrect) {
            stateClasses = 'border-[var(--color-danger)] bg-[var(--color-danger-soft)] text-[var(--color-danger)]'
          }

          return (
            <button
              key={option}
              type="button"
              disabled={answered}
              onClick={() => setSelected(index)}
              className={`rounded-lg border px-3 py-2 text-left text-sm transition disabled:cursor-default ${stateClasses}`}
            >
              {option}
            </button>
          )
        })}
      </div>

      {answered ? (
        <p
          className={`mt-3 rounded-lg px-3 py-2 text-sm ${
            isCorrect
              ? 'bg-[var(--color-success-soft)] text-[var(--color-success)]'
              : 'bg-[var(--color-warning-soft)] text-[var(--color-warning)]'
          }`}
        >
          {isCorrect ? '✓ Correct — ' : '✗ Not quite — '}
          {explanation}
        </p>
      ) : null}
    </div>
  )
}
