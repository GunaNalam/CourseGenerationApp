interface VideoBlockProps {
  query: string
  embedUrl?: string
}

export function VideoBlock({ query, embedUrl }: VideoBlockProps) {
  if (!embedUrl) {
    return (
      <div className="mb-4 rounded-xl border border-dashed border-[var(--color-border)] px-4 py-3 text-sm text-[var(--color-text-muted)]">
        📺 Suggested video search: <span className="italic">"{query}"</span>
      </div>
    )
  }

  return (
    <div className="mb-4 aspect-video w-full overflow-hidden rounded-xl border border-[var(--color-border)]">
      <iframe
        src={embedUrl}
        title={query}
        className="h-full w-full"
        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
        allowFullScreen
      />
    </div>
  )
}
