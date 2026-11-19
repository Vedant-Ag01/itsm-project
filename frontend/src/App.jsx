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
import Register from './pages/Register'
import LiveReportIncident from './pages/employee/LiveReportIncident'
import AdminIncidents from './pages/admin/AdminIncidents'
import Reports from './pages/admin/Reports'
import AuditLogs from './pages/admin/AuditLogs'
import AdminOverview from './pages/admin/AdminOverview'
import AdminUsers from './pages/admin/AdminUsers'
import AdminTechnicians from './pages/admin/AdminTechnicians'
import AdminServices from './pages/admin/AdminServices'
import AdminSlaPolicies from './pages/admin/AdminSlaPolicies'
import AdminAssignment from './pages/admin/AdminAssignment'
import AdminSlaViolations from './pages/admin/AdminSlaViolations'
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

  function renderWorkspacePage(role, page) {
    if (role === 'employee' && page.path === 'incidents') return <MyIncidents />
    if (role === 'employee' && page.path === 'report') {
      return USING_MOCKS ? <ReportIncident /> : <LiveReportIncident />
    }
    if ((role === 'employee' || role === 'technician') && page.path === 'notifications') {
      return <Notifications />
    }
    if (role === 'technician' && page.path === 'workload') return <Workload />
    if (role === 'admin' && page.path === 'incidents') return <AdminIncidents />
    if (role === 'admin' && page.path === 'reports') return <Reports />
    if (role === 'admin' && page.path === 'audit') return <AuditLogs />
    if (role === 'admin' && page.path === 'users') return <AdminUsers />
    if (role === 'admin' && page.path === 'technicians') return <AdminTechnicians />
    if (role === 'admin' && page.path === 'services') return <AdminServices />
    if (role === 'admin' && page.path === 'sla-policies') return <AdminSlaPolicies />
    if (role === 'admin' && page.path === 'assignment') return <AdminAssignment />
    if (role === 'admin' && page.path === 'sla-violations') return <AdminSlaViolations />

    return <PlaceholderPage title={page.label} role={role} />
  }

  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="/login" element={<SessionGate><Login theme={theme} onToggle={toggleTheme} /></SessionGate>} />
      <Route path="/register" element={<SessionGate><Register theme={theme} onToggle={toggleTheme} /></SessionGate>} />

      <Route path="/register" element={<SessionGate><Register theme={theme} onToggle={toggleTheme} /></SessionGate>} />

      {Object.entries(workspaces).map(([role, workspace]) => (
        <Route
          key={role}
          path={`/${role}`}
          element={
            <SessionGate role={role}>
              <WorkspaceLayout role={role} theme={theme} onToggle={toggleTheme} />
            </SessionGate>
          }
        >
          <Route
            index
            element={
              role === 'employee'
                ? <EmployeeOverview />
                : role === 'admin'
                  ? <AdminOverview />
                  : role === 'technician'
                    ? <MyIncidents role="technician" />
                    : <PlaceholderPage title={workspace.pages[0].label} role={role} />
            }
          />

          {workspace.pages
            .filter((page) => page.path !== '')
            .map((page) => (
              <Route
                key={page.path}
                path={page.path}
                element={renderWorkspacePage(role, page)}
              />
            ))}

          {['employee', 'technician'].includes(role) && (
            <Route path="incidents/:id" element={<IncidentDetails role={role} />} />
          )}

          <Route path="*" element={<NotFound home={pageUrl(role)} />} />
        </Route>
      ))}

      <Route path="*" element={<NotFound />} />
    </Routes>
  )
}
