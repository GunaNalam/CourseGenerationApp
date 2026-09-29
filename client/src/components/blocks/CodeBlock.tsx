import { useState } from 'react'

interface CodeBlockProps {
  language: string
  text: string
}

export function CodeBlock({ language, text }: CodeBlockProps) {
  const [copied, setCopied] = useState(false)

  async function handleCopy() {
    try {
      await navigator.clipboard.writeText(text)
      setCopied(true)
      window.setTimeout(() => setCopied(false), 1500)
    } catch {
      // clipboard API unavailable — silently ignore, the button just won't confirm
    }
  }

  return (
    <div className="mb-4 overflow-hidden rounded-xl border border-[var(--color-border)]">
      <div className="flex items-center justify-between bg-[var(--color-surface-hover)] px-3 py-1.5">
        <span className="text-xs font-medium text-[var(--color-text-muted)]">{language || 'code'}</span>
        <button
          type="button"
          onClick={handleCopy}
          className="text-xs font-medium text-[var(--color-text-muted)] hover:text-[var(--color-primary)]"
        >
          {copied ? 'Copied!' : 'Copy'}
        </button>
      </div>
      <pre className="overflow-x-auto bg-[var(--color-bg)] px-4 py-3 text-sm text-[var(--color-text)]">
        <code className="font-[var(--font-mono)]">{text}</code>
      </pre>
    </div>
  )
}
