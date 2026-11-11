import { signOut } from '../auth/session'
import { useEffect, useState } from 'react'
import { Outlet, useLocation } from 'react-router'
import Sidebar from '../components/Sidebar'
import ThemeToggle from '../components/ThemeToggle'
import { pageUrl, workspaces } from './workspaces'

export default function WorkspaceLayout({ role, theme, onToggle }) {
  const { pathname } = useLocation()
  const [logoutError, setLogoutError] = useState('')
  const [loggingOut, setLoggingOut] = useState(false)
  async function logout() {
    if (loggingOut) return
    setLoggingOut(true); setLogoutError('')
    try { await signOut() } catch (error) { setLogoutError(error.message); setLoggingOut(false) }
  }
  const workspace = workspaces[role]
  const normalizedPath = pathname.replace(/\/+$/, '')

  const currentPage = workspace.pages.find(
    (page) => pageUrl(role, page.path) === normalizedPath,
  )

  const title = /^\/employee\/incidents\/[^/]+$/.test(normalizedPath)
    ? 'Incident details' : currentPage?.label ?? 'Page not found'

  useEffect(() => {
    document.title = `${title} · I-ITSM`
  }, [title])

  return (
    <div className="app-shell">
      <Sidebar role={role} />

      <div className="workspace">
        <header className="topbar">
          <div className="breadcrumb">
            <span>{workspace.label}</span>
            <span aria-hidden="true">/</span>
            <strong>{title}</strong>
          </div>

          <div className="account-actions"><ThemeToggle theme={theme} onToggle={onToggle} /><button className="theme-toggle" disabled={loggingOut} onClick={() => void logout()}>{loggingOut ? "Signing out…" : "Sign out"}</button></div>
        </header>

        <main className="main-content">
          {logoutError && <p role="alert">Sign out failed: {logoutError}. Please retry.</p>}
          <Outlet />
        </main>
      </div>
    </div>
  )
}
