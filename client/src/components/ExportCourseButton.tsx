import { useState } from 'react'
import { useApi } from '../hooks/useApi'
import { API_ROUTES } from '../utils/api-routes'
import type { CourseExportResponse } from '../utils/api-types'
import { DownloadIcon } from './icons'

interface ExportCourseButtonProps {
  courseId: string
  fileName: string
}

export function ExportCourseButton({ courseId, fileName }: ExportCourseButtonProps) {
  const api = useApi()
  const [exporting, setExporting] = useState(false)

  async function handleExport() {
    setExporting(true)
    try {
      const data = await api.get<CourseExportResponse>(API_ROUTES.courses.export(courseId))
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `${fileName}.json`
      link.click()
      URL.revokeObjectURL(url)
    } finally {
      setExporting(false)
    }
  }

  return (
    <button
      type="button"
      onClick={handleExport}
      disabled={exporting}
      className="flex items-center gap-1.5 rounded-lg border border-[var(--color-border)] bg-[var(--color-surface)] px-3 py-1.5 text-sm text-[var(--color-text-muted)] shadow-[var(--shadow-sm)] transition hover:border-[var(--color-primary)] hover:text-[var(--color-primary)] disabled:opacity-50"
    >
      <DownloadIcon className="h-3.5 w-3.5" />
      {exporting ? 'Exporting…' : 'Export as JSON'}
    </button>
  )
}
