import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { ErrorMessage } from '../components/ErrorMessage'
import { LoadingSpinner } from '../components/LoadingSpinner'
import { useApi } from '../hooks/useApi'
import { API_ROUTES } from '../utils/api-routes'
import type { ApiKeyStatusResponse } from '../utils/api-types'

export function SettingsPage() {
  const api = useApi()
  const [status, setStatus] = useState<ApiKeyStatusResponse | null>(null)
  const [keyInput, setKeyInput] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [savedMessage, setSavedMessage] = useState<string | null>(null)

  const load = () => {
    setError(null)
    api
      .get<ApiKeyStatusResponse>(API_ROUTES.users.apiKey())
      .then(setStatus)
      .catch((e) => setError(e instanceof Error ? e.message : 'Failed to load API key status'))
  }

  useEffect(load, []) // eslint-disable-line react-hooks/exhaustive-deps

  async function handleSave(event: FormEvent) {
    event.preventDefault()
    if (!keyInput.trim()) return
    setSaving(true)
    setError(null)
    setSavedMessage(null)
    try {
      const result = await api.put<ApiKeyStatusResponse>(API_ROUTES.users.apiKey(), { apiKey: keyInput.trim() })
      setStatus(result)
      setKeyInput('')
      setSavedMessage('Your API key has been saved and encrypted.')
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to save API key')
    } finally {
      setSaving(false)
    }
  }

  async function handleClear() {
    setSaving(true)
    setError(null)
    setSavedMessage(null)
    try {
      await api.del(API_ROUTES.users.apiKey())
      setStatus({ configured: false })
      setSavedMessage('Your API key has been removed — course generation will use the default key.')
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to remove API key')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="mx-auto max-w-xl px-4 py-10">
      <h1 className="text-3xl font-semibold tracking-tight">Settings</h1>
      <p className="mt-2 text-[var(--color-text-muted)]">Bring your own Gemini API key for course generation.</p>

      <div className="mt-6 rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-5 shadow-[var(--shadow-md)]">
        {status === null && !error ? <LoadingSpinner label="Loading…" /> : null}

        {status ? (
          <>
            <p className="mb-4 flex items-center gap-2 text-sm">
              <span
                className={`h-2 w-2 rounded-full ${status.configured ? 'bg-[var(--color-success)]' : 'bg-[var(--color-text-muted)]'}`}
              />
              {status.configured ? (
                <span className="font-medium text-[var(--color-success)]">Using your own API key</span>
              ) : (
                <span className="font-medium text-[var(--color-text-muted)]">Using the default project key</span>
              )}
            </p>

            <form onSubmit={handleSave} className="flex flex-col gap-3 sm:flex-row">
              <input
                type="password"
                value={keyInput}
                onChange={(e) => setKeyInput(e.target.value)}
                placeholder="Paste your Gemini API key"
                className="flex-1 rounded-xl border border-[var(--color-border)] bg-[var(--color-bg)] px-3 py-2 text-sm outline-none focus:border-[var(--color-primary)]"
              />
              <button
                type="submit"
                disabled={saving || !keyInput.trim()}
                className="rounded-xl px-4 py-2 text-sm font-medium text-[var(--color-primary-text)] shadow-[var(--shadow-glow)] transition hover:brightness-110 disabled:cursor-not-allowed disabled:opacity-40 disabled:shadow-none"
                style={{ backgroundImage: 'var(--gradient-primary)' }}
              >
                Save
              </button>
            </form>

            {status.configured ? (
              <button
                type="button"
                onClick={handleClear}
                disabled={saving}
                className="mt-3 text-sm text-[var(--color-danger)] hover:underline disabled:opacity-50"
              >
                Remove my key
              </button>
            ) : null}
          </>
        ) : null}

        {savedMessage ? <p className="mt-4 text-sm text-[var(--color-success)]">{savedMessage}</p> : null}
        {error ? (
          <div className="mt-4">
            <ErrorMessage message={error} onRetry={load} />
          </div>
        ) : null}
      </div>

      <p className="mt-4 text-xs text-[var(--color-text-muted)]">
        Your key is encrypted at rest. If it ever fails during generation, we automatically fall back to the
        default key and let you know.
      </p>
    </div>
  )
}
