import { useRef, useState } from 'react'
import { Link } from 'react-router'
import { signIn } from '../auth/session'
import { USING_MOCKS } from '../api/incidents'
import ThemeToggle from '../components/ThemeToggle'

export default function Login({ theme, onToggle }) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const lock = useRef(false)
  async function submit(event) {
    event.preventDefault()
    if (lock.current) return
    lock.current = true; setBusy(true); setError('')
    try { await signIn(email, password) }
    catch (failure) { setError(failure.message) }
    finally { lock.current = false; setBusy(false) }
  }
  return <main className="login-shell"><section className="panel login-panel">
    <div className="panel-heading"><strong>I-ITSM</strong><ThemeToggle theme={theme} onToggle={onToggle} /></div>
    <h1>Sign in</h1><p className="subtitle">Access your service workspace.</p>
    {USING_MOCKS && <p className="data-notice">Employee demo: asha@bits.edu · password123. Sample incident changes reset on refresh.</p>}
    <form className="detail-form" onSubmit={submit}>
      <label className="report-field">Email<input type="email" autoComplete="username" required value={email} disabled={busy} onChange={(e) => setEmail(e.target.value)} /></label>
      <label className="report-field">Password<input type="password" autoComplete="current-password" required value={password} disabled={busy} onChange={(e) => setPassword(e.target.value)} /></label>
      {error && <p role="alert">{error}</p>}
      <button className="primary-button" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</button>
    </form>
    <p className="subtitle" style={{ marginTop: '1rem' }}>
      New employee? <Link to="/register">Create an account</Link>
    </p>
  </section></main>
}
