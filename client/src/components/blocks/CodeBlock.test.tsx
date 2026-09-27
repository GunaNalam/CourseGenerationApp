import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { CodeBlock } from './CodeBlock'

describe('CodeBlock', () => {
  beforeEach(() => {
    Object.assign(navigator, {
      clipboard: { writeText: vi.fn().mockResolvedValue(undefined) },
    })
  })

  it('copies the code text to the clipboard and shows confirmation', async () => {
    render(<CodeBlock language="javascript" text="console.log('hi')" />)

    fireEvent.click(screen.getByText('Copy'))

    expect(navigator.clipboard.writeText).toHaveBeenCalledWith("console.log('hi')")
    await waitFor(() => expect(screen.getByText('Copied!')).toBeInTheDocument())
  })

  it('renders the language label', () => {
    render(<CodeBlock language="python" text="print('hi')" />)

    expect(screen.getByText('python')).toBeInTheDocument()
  })
})
