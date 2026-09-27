import { useAuth0 } from '@auth0/auth0-react'
import { Link } from 'react-router-dom'
import { useDarkMode } from '../hooks/useDarkMode'
import { GearIcon, MoonIcon, ShieldIcon, SunIcon } from './icons'

export function Navbar() {
  const { isAuthenticated, user, logout } = useAuth0()
  const { isDark, toggle } = useDarkMode()

  return (
    <header className="sticky top-0 z-10 border-b border-[var(--color-border)] bg-[var(--color-surface)]/75 shadow-[var(--shadow-sm)] backdrop-blur-md">
      <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-3">
        <Link to="/" className="group flex items-center gap-2 font-semibold tracking-tight">
          <span
            className="flex h-8 w-8 items-center justify-center rounded-xl text-sm text-[var(--color-primary-text)] shadow-[var(--shadow-glow)] transition-transform group-hover:scale-105"
            style={{ backgroundImage: 'var(--gradient-primary)' }}
          >
            L
          </span>
          <span>Learnify</span>
        </Link>

        <div className="flex items-center gap-1.5">
          {isAuthenticated ? (
            <>
              <Link
                to="/settings"
                className="hidden items-center gap-1.5 rounded-full px-3 py-1.5 text-sm text-[var(--color-text-muted)] transition hover:bg-[var(--color-surface-hover)] hover:text-[var(--color-text)] sm:flex"
              >
                <GearIcon className="h-4 w-4" />
                Settings
              </Link>
              <Link
                to="/admin"
                className="hidden items-center gap-1.5 rounded-full px-3 py-1.5 text-sm text-[var(--color-text-muted)] transition hover:bg-[var(--color-surface-hover)] hover:text-[var(--color-text)] sm:flex"
              >
                <ShieldIcon className="h-4 w-4" />
                Admin
              </Link>
            </>
          ) : null}

          <button
            type="button"
            onClick={toggle}
            aria-label="Toggle dark mode"
            className="flex h-9 w-9 items-center justify-center rounded-full border border-[var(--color-border)] text-[var(--color-text-muted)] transition hover:border-[var(--color-primary)] hover:text-[var(--color-primary)]"
          >
            {isDark ? <SunIcon className="h-4 w-4" /> : <MoonIcon className="h-4 w-4" />}
          </button>

          {isAuthenticated ? (
            <div className="ml-1 flex items-center gap-2 border-l border-[var(--color-border)] pl-3">
              {user?.picture ? (
                <img
                  src={user.picture}
                  alt=""
                  className="h-8 w-8 rounded-full ring-2 ring-[var(--color-border)]"
                  referrerPolicy="no-referrer"
                />
              ) : null}
              <button
                type="button"
                onClick={() => logout({ logoutParams: { returnTo: window.location.origin } })}
                className="text-sm text-[var(--color-text-muted)] hover:text-[var(--color-text)]"
              >
                Log out
              </button>
            </div>
          ) : null}
        </div>
      </div>
    </header>
  )
}
