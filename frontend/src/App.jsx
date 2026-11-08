import { useEffect, useState } from 'react'
import { Navigate, Route, Routes } from 'react-router'
import WorkspaceLayout from './layouts/WorkspaceLayout'
import { pageUrl, workspaces } from './layouts/workspaces'
import EmployeeOverview from './pages/employee/EmployeeOverview'
import PlaceholderPage from './pages/PlaceholderPage'
import NotFound from './pages/NotFound'
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