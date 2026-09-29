import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import App from './App'

vi.mock('@auth0/auth0-react', () => ({
  useAuth0: () => ({ isLoading: false, isAuthenticated: false, loginWithRedirect: vi.fn() }),
}))

describe('App', () => {
  it('shows the landing page with a login call-to-action when logged out', () => {
    render(
      <MemoryRouter>
        <App />
      </MemoryRouter>
    )

    expect(screen.getByText(/log in to get started/i)).toBeInTheDocument()
  })
})
