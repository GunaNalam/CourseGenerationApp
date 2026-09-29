import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { PromptForm } from './PromptForm'

describe('PromptForm', () => {
  it('calls onSubmit with the trimmed topic when submitted', () => {
    const onSubmit = vi.fn()
    render(<PromptForm onSubmit={onSubmit} />)

    fireEvent.change(screen.getByPlaceholderText(/what do you want to learn/i), {
      target: { value: '  Intro to Testing  ' },
    })
    fireEvent.click(screen.getByText('Generate course'))

    expect(onSubmit).toHaveBeenCalledWith('Intro to Testing')
  })

  it('does not submit an empty topic', () => {
    const onSubmit = vi.fn()
    render(<PromptForm onSubmit={onSubmit} />)

    fireEvent.click(screen.getByText('Generate course'))

    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('fills the input when an example topic is clicked', () => {
    const onSubmit = vi.fn()
    render(<PromptForm onSubmit={onSubmit} />)

    fireEvent.click(screen.getByText('Intro to React Hooks'))
    fireEvent.click(screen.getByText('Generate course'))

    expect(onSubmit).toHaveBeenCalledWith('Intro to React Hooks')
  })

  it('disables the submit button while disabled prop is set', () => {
    render(<PromptForm onSubmit={vi.fn()} disabled />)

    expect(screen.getByText('Generate course')).toBeDisabled()
  })
})
