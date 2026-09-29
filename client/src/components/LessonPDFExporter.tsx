import html2canvas from 'html2canvas'
import jsPDF from 'jspdf'
import { useRef, useState } from 'react'
import type { LessonResponse } from '../utils/api-types'
import { DownloadIcon } from './icons'
import { LessonRenderer } from './LessonRenderer'

interface LessonPDFExporterProps {
  lesson: LessonResponse
}

// Forces a print-friendly light theme on the hidden export target, regardless of the
// viewer's active dark/light mode — these are the same CSS custom properties consumed
// by every block component, just overridden at this subtree's root.
const PDF_THEME_COLORS: Record<string, string> = {
  '--color-bg': '#ffffff',
  '--color-surface': '#ffffff',
  '--color-surface-hover': '#f1f5f9',
  '--color-border': '#e2e8f0',
  '--color-text': '#0f172a',
  '--color-text-muted': '#475569',
  '--color-primary': '#4f46e5',
  '--color-primary-soft': '#eef2ff',
  '--color-success': '#059669',
  '--color-success-soft': '#ecfdf5',
  '--color-danger': '#e11d48',
  '--color-danger-soft': '#fff1f2',
  '--color-warning': '#b45309',
  '--color-warning-soft': '#fffbeb',
}

const PDF_THEME_VARS = PDF_THEME_COLORS as React.CSSProperties

export function LessonPDFExporter({ lesson }: LessonPDFExporterProps) {
  const exportRef = useRef<HTMLDivElement>(null)
  const [exporting, setExporting] = useState(false)

  async function handleExport() {
    if (!exportRef.current) return
    setExporting(true)
    try {
      const canvas = await html2canvas(exportRef.current, { scale: 2, backgroundColor: '#ffffff' })
      const imgData = canvas.toDataURL('image/png')

      const pdf = new jsPDF('p', 'pt', 'a4')
      const pageWidth = pdf.internal.pageSize.getWidth()
      const pageHeight = pdf.internal.pageSize.getHeight()
      const imgWidth = pageWidth
      const imgHeight = (canvas.height * imgWidth) / canvas.width

      let heightLeft = imgHeight
      let position = 0

      pdf.addImage(imgData, 'PNG', 0, position, imgWidth, imgHeight)
      heightLeft -= pageHeight

      while (heightLeft > 0) {
        position -= pageHeight
        pdf.addPage()
        pdf.addImage(imgData, 'PNG', 0, position, imgWidth, imgHeight)
        heightLeft -= pageHeight
      }

      pdf.save(`${lesson.title}.pdf`)
    } finally {
      setExporting(false)
    }
  }

  return (
    <>
      <button
        type="button"
        onClick={handleExport}
        disabled={exporting}
        className="flex items-center gap-1.5 rounded-lg border border-[var(--color-border)] bg-[var(--color-surface)] px-3 py-1.5 text-sm text-[var(--color-text-muted)] shadow-[var(--shadow-sm)] transition hover:border-[var(--color-primary)] hover:text-[var(--color-primary)] disabled:opacity-50"
      >
        <DownloadIcon className="h-3.5 w-3.5" />
        {exporting ? 'Preparing PDF…' : 'Download as PDF'}
      </button>

      <div style={{ position: 'fixed', top: 0, left: '-10000px', width: '700px' }} aria-hidden="true">
        <div
          ref={exportRef}
          style={{ ...PDF_THEME_VARS, backgroundColor: '#ffffff', color: PDF_THEME_COLORS['--color-text'] }}
          className="p-8"
        >
          <h1 className="mb-4 text-2xl font-bold text-[var(--color-text)]">{lesson.title}</h1>
          {lesson.objectives.length > 0 ? (
            <ul className="mb-6 list-inside list-disc text-sm text-[var(--color-text)]">
              {lesson.objectives.map((objective) => (
                <li key={objective}>{objective}</li>
              ))}
            </ul>
          ) : null}
          <LessonRenderer content={lesson.content} />
        </div>
      </div>
    </>
  )
}
