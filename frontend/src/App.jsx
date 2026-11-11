import { useEffect, useState } from 'react'
import { Navigate, Route, Routes } from 'react-router'
import WorkspaceLayout from './layouts/WorkspaceLayout'
import { pageUrl, workspaces } from './layouts/workspaces'
import EmployeeOverview from './pages/employee/EmployeeOverview'
import MyIncidents from './pages/employee/MyIncidents'
import IncidentDetails from './pages/employee/IncidentDetails'
import PlaceholderPage from './pages/PlaceholderPage'
import NotFound from './pages/NotFound'
import './App.css'
import ReportIncident from './pages/employee/ReportIncident'

function getInitialTheme() {
  try {
    const saved = localStorage.getItem('itsm-theme')
    if (saved === 'light' || saved === 'dark') return saved
  } catch {
    // Fall back to the system preference.
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
      // Theme switching still works without storage.
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
              role === 'employee'
                ? <EmployeeOverview />
                : (
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
  role === 'employee' && page.path === 'incidents' ? (
    <MyIncidents />
  ) : role === 'employee' && page.path === 'report' ? (
    <ReportIncident />
  ) : (
    <PlaceholderPage title={page.label} role={role} />
  )
}
              />
            ))}

          {role === 'employee' && (
            <Route path="incidents/:id" element={<IncidentDetails />} />
          )}

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