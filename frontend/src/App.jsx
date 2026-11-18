import { useEffect, useState } from 'react'
import { Navigate, Route, Routes } from 'react-router'
import WorkspaceLayout from './layouts/WorkspaceLayout'
import { pageUrl, workspaces } from './layouts/workspaces'
import EmployeeOverview from './pages/employee/EmployeeOverview'
import Workload from './pages/technician/Workload'
import Notifications from './pages/employee/Notifications'
import MyIncidents from './pages/employee/MyIncidents'
import IncidentDetails from './pages/employee/IncidentDetails'
import PlaceholderPage from './pages/PlaceholderPage'
import NotFound from './pages/NotFound'
import './App.css'
import './design-polish.css'
import ReportIncident from './pages/employee/ReportIncident'
import SessionGate from './auth/SessionGate'
import Login from './pages/Login'
import LiveReportIncident from './pages/employee/LiveReportIncident'
import { USING_MOCKS } from './api/incidents'

function getInitialTheme() {
  try {
    const saved = localStorage.getItem('itsm-theme')
    if (['light', 'soft', 'dim', 'dark', 'midnight', 'system'].includes(saved)) return saved
  } catch {
    // Fall back to the system preference.
  }

  return 'system'
}

export default function App() {
  const [theme, setTheme] = useState(getInitialTheme)

  useEffect(() => {
    const media = window.matchMedia('(prefers-color-scheme: dark)')
    function apply() {
      const selected = theme === 'system' ? (media.matches ? 'dark' : 'light') : theme
      document.documentElement.dataset.theme = ['dim', 'midnight'].includes(selected) ? 'dark' : selected === 'soft' ? 'light' : selected
      document.documentElement.dataset.palette = selected
    }
    apply()
    media.addEventListener('change', apply)

    try {
      localStorage.setItem('itsm-theme', theme)
    } catch {
      // Theme switching still works without storage.
    }
    return () => media.removeEventListener('change', apply)
  }, [theme])

  function toggleTheme(value) {
    setTheme(value)
  }

  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="/login" element={<SessionGate><Login theme={theme} onToggle={toggleTheme} /></SessionGate>} />

      {Object.entries(workspaces).map(([role, workspace]) => (
        <Route
          key={role}
          path={`/${role}`}
          element={
            <SessionGate role={role}><WorkspaceLayout
              role={role}
              theme={theme}
              onToggle={toggleTheme}
            /></SessionGate>
          }
        >
          <Route
            index
            element={
              role === 'employee'
                ? <EmployeeOverview />
                : role === 'technician' ? <MyIncidents role="technician" /> : (
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
    USING_MOCKS ? <ReportIncident /> : <LiveReportIncident />
  ) : ['employee', 'technician'].includes(role) && page.path === 'notifications' ? (
    <Notifications role={role} />
  ) : role === 'technician' && page.path === 'workload' ? (
    <Workload />
  ) : (
    <PlaceholderPage title={page.label} role={role} />
  )
}
              />
            ))}

          {['employee', 'technician'].includes(role) && (
            <Route path="incidents/:id" element={<IncidentDetails role={role} />} />
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
