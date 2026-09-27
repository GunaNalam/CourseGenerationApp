import { useEffect, useState } from 'react'
import { useApi } from '../hooks/useApi'
import { API_ROUTES } from '../utils/api-routes'
import { ErrorMessage } from './ErrorMessage'
import { Volume2Icon } from './icons'
import { LoadingSpinner } from './LoadingSpinner'

interface HinglishAudioButtonProps {
  courseId: string
  moduleId: string
  lessonId: string
}

export function HinglishAudioButton({ courseId, moduleId, lessonId }: HinglishAudioButtonProps) {
  const api = useApi()
  const [audioUrl, setAudioUrl] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Revoke the object URL when the lesson changes or the component unmounts.
  useEffect(() => () => { if (audioUrl) URL.revokeObjectURL(audioUrl) }, [audioUrl])

  async function handleGenerate() {
    setLoading(true)
    setError(null)
    try {
      const blob = await api.getBlob(API_ROUTES.lessons.audio(courseId, moduleId, lessonId))
      setAudioUrl(URL.createObjectURL(blob))
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not generate Hinglish audio')
    } finally {
      setLoading(false)
    }
  }

  if (audioUrl) {
    // eslint-disable-next-line jsx-a11y/media-has-caption
    return <audio controls autoPlay src={audioUrl} className="w-full" />
  }

  return (
    <div className="flex flex-col gap-2">
      <button
        type="button"
        onClick={handleGenerate}
        disabled={loading}
        className="flex w-fit items-center gap-2 rounded-lg border border-[var(--color-border)] bg-[var(--color-surface)] px-3 py-1.5 text-sm text-[var(--color-text-muted)] shadow-[var(--shadow-sm)] transition hover:border-[var(--color-primary)] hover:text-[var(--color-primary)] disabled:opacity-50"
      >
        {loading ? <LoadingSpinner size="sm" /> : <Volume2Icon className="h-4 w-4" />}
        {loading ? 'Generating Hinglish audio…' : 'Listen in Hinglish'}
      </button>
      {error ? <ErrorMessage message={error} onRetry={handleGenerate} /> : null}
    </div>
  )
}
