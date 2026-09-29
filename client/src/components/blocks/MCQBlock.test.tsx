import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { MCQBlock } from './MCQBlock'

describe('MCQBlock', () => {
  const props = {
    question: 'What is 2 + 2?',
    options: ['3', '4', '5'],
    answer: 1,
    explanation: 'Basic arithmetic.',
  }

  it('reveals the explanation and marks the correct answer when selected', () => {
    render(<MCQBlock {...props} />)

    fireEvent.click(screen.getByText('4'))

    expect(screen.getByText(/Basic arithmetic\./)).toBeInTheDocument()
    expect(screen.getByText(/Correct —/)).toBeInTheDocument()
  })

  it('marks a wrong answer as incorrect and still shows the explanation', () => {
    render(<MCQBlock {...props} />)

    fireEvent.click(screen.getByText('3'))

    expect(screen.getByText(/Not quite —/)).toBeInTheDocument()
  })

  it('disables options after answering so the choice cannot be changed', () => {
    render(<MCQBlock {...props} />)

    fireEvent.click(screen.getByText('4'))

    expect(screen.getByText('3')).toBeDisabled()
    expect(screen.getByText('4')).toBeDisabled()
  })
})
