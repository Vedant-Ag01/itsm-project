import { useEffect } from 'react'
import { Navigate } from 'react-router'
import { clearSession, restoreSession, useSession } from './session'

export default function SessionGate({ role, children }) {
  const session = useSession()
  useEffect(() => { void restoreSession() }, [])
  if (session.phase === 'loading') return <main className="login-shell" role="status">Checking session…</main>
  if (session.phase === 'error') return <main className="login-shell"><section className="panel"><h1>Unable to check session</h1><p role="alert">{session.error}</p><button className="primary-button" onClick={() => void restoreSession()}>Retry</button><button className="theme-toggle" onClick={clearSession}>Return to sign in</button></section></main>
  if (session.phase !== 'ready') return role ? <Navigate to="/login" replace /> : children
  const home = `/${session.user.role.toLowerCase()}`
  if (!role || role.toUpperCase() !== session.user.role) return <Navigate to={home} replace />
  return children
}
