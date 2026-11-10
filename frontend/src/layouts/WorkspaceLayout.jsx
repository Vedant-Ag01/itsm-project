import { useEffect } from 'react'
import { Outlet, useLocation } from 'react-router'
import Sidebar from '../components/Sidebar'
import ThemeToggle from '../components/ThemeToggle'
import { pageUrl, workspaces } from './workspaces'

export default function WorkspaceLayout({ role, theme, onToggle }) {
  const { pathname } = useLocation()
  const workspace = workspaces[role]
  const normalizedPath = pathname.replace(/\/+$/, '')

  const currentPage = workspace.pages.find(
    (page) => pageUrl(role, page.path) === normalizedPath,
  )

  const title = currentPage?.label ?? 'Page not found'

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

          <ThemeToggle theme={theme} onToggle={onToggle} />
        </header>

        <main className="main-content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}