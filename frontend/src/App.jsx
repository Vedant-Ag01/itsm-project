import Sidebar from './components/Sidebar'
import { pageUrl, workspaces } from './layouts/workspaces'
import { useEffect, useState } from 'react'
import {
  Link,
  Navigate,
  Outlet,
  Route,
  Routes,
  useLocation,
} from 'react-router'
import './App.css'



function getInitialTheme() {
  try {
    const saved = localStorage.getItem('itsm-theme')

    if (saved === 'light' || saved === 'dark') {
      return saved
    }
  } catch {
    // Storage may be unavailable; use the system preference instead.
  }

  return window.matchMedia('(prefers-color-scheme: dark)').matches
    ? 'dark'
    : 'light'
}



function ThemeToggle({ theme, onToggle }) {
  const nextTheme = theme === 'dark' ? 'light' : 'dark'

  return (
    <button
      className="theme-toggle"
      type="button"
      onClick={onToggle}
      aria-label={`Switch to ${nextTheme} mode`}
    >
      <span aria-hidden="true">
        {theme === 'dark' ? '☀' : '☾'}
      </span>
      {nextTheme === 'light' ? 'Light mode' : 'Dark mode'}
    </button>
  )
}



function WorkspaceLayout({ role, theme, onToggle }) {
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

function EmployeeOverview() {
  const metrics = [
    'Open incidents',
    'In progress',
    'Awaiting verification',
    'Resolved this month',
  ]

  return (
    <>
      <div className="page-heading">
        <div>
          <p className="eyebrow">Overview</p>
          <h1>Your workspace</h1>
          <p className="subtitle">Report issues. Track progress.</p>
        </div>

        <Link className="primary-button" to="/employee/report">
          + Report incident
        </Link>
      </div>

      <div className="stats-grid">
        {metrics.map((label) => (
          <section className="stat-card" key={label}>
            <h2>{label}</h2>
            <div className="stat-value" aria-label="Not connected">
              —
            </div>
            <p>Data not connected yet</p>
          </section>
        ))}
      </div>

      <div className="dashboard-grid">
        <section className="panel">
          <div className="panel-heading">
            <h2>My incidents</h2>
            <span className="preview-badge">Layout preview</span>
          </div>

          <div className="empty-state">
            <span className="empty-symbol" aria-hidden="true">＋</span>
            <h3>Your incidents will appear here</h3>
            <p>
              We’ll connect this panel to mock incident data in a later step.
            </p>
          </div>
        </section>

        <section className="panel">
          <div className="panel-heading">
            <h2>Incident activity</h2>
          </div>

          <div className="empty-state">
            <h3>No activity loaded</h3>
            <p>
              Incident updates and their timestamps will appear here.
            </p>
          </div>
        </section>
      </div>

      <section className="panel attention-panel">
        <h2>Needs your attention</h2>
        <p className="subtitle">
          Requests for information and resolutions awaiting verification
          will appear here once connected.
        </p>
      </section>
    </>
  )
}

function PlaceholderPage({ title, role }) {
  return (
    <>
      <div className="page-heading">
        <div>
          <p className="eyebrow">
            {workspaces[role].label} workspace
          </p>
          <h1>{title}</h1>
        </div>
      </div>

      <section className="panel empty-state">
        <h2>This page is next</h2>
        <p>
          This route is ready. Its page content and API connection
          will be added separately.
        </p>
      </section>
    </>
  )
}

function NotFound({ home = '/employee' }) {
  return (
    <section className="panel empty-state">
      <h1>Page not found</h1>
      <p>Check the address or return to your workspace.</p>
      <Link className="text-link" to={home}>
        Return to workspace
      </Link>
    </section>
  )
}

export default function App() {
  const [theme, setTheme] = useState(getInitialTheme)

  useEffect(() => {
    document.documentElement.dataset.theme = theme

    try {
      localStorage.setItem('itsm-theme', theme)
    } catch {
      // Theme switching still works without persistence.
    }
  }, [theme])

  function toggleTheme() {
    setTheme((current) => current === 'dark' ? 'light' : 'dark')
  }

  return (
    <Routes>
      <Route path="/" element={<Navigate to="/employee" replace />} />

      {Object.entries(workspaces).map(([role, workspace]) => (
        <Route
          key={role}
          path={`/${role}`}
          element={
            <WorkspaceLayout
              role={role}
              theme={theme}
              onToggle={toggleTheme}
            />
          }
        >
          <Route
            index
            element={
              role === 'employee' ? (
                <EmployeeOverview />
              ) : (
                <PlaceholderPage
                  title={workspace.pages[0].label}
                  role={role}
                />
              )
            }
          />

          {workspace.pages
            .filter((page) => page.path !== '')
            .map((page) => (
              <Route
                key={page.path}
                path={page.path}
                element={
                  <PlaceholderPage title={page.label} role={role} />
                }
              />
            ))}

          <Route
            path="*"
            element={<NotFound home={pageUrl(role)} />}
          />
        </Route>
      ))}

      <Route path="*" element={<NotFound />} />
    </Routes>
  )
}