import { useRef, useState } from 'react'
import { Link } from 'react-router'
import { registerEmployee } from '../auth/session'
import ThemeToggle from '../components/ThemeToggle'

export default function Register({ theme, onToggle }) {
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [department, setDepartment] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const lock = useRef(false)

  async function submit(event) {
    event.preventDefault()
    if (lock.current) return
    if (password.length < 8) {
      setError('Password must be at least 8 characters long.')
      return
    }
    lock.current = true
    setBusy(true)
    setError('')
    try {
      await registerEmployee({ name, email, department, password })
    } catch (failure) {
      setError(failure.message)
    } finally {
      lock.current = false
      setBusy(false)
    }
  }

  return (
    <main className="login-shell">
      <section className="panel login-panel">
        <div className="panel-heading">
          <strong>I-ITSM</strong>
          <ThemeToggle theme={theme} onToggle={onToggle} />
        </div>
        <h1>Create employee account</h1>
        <p className="subtitle">Register to report and track IT incidents.</p>

        <form className="detail-form" onSubmit={submit}>
          <label className="report-field">
            Full name
            <input type="text" required value={name} disabled={busy} onChange={(e) => setName(e.target.value)} />
          </label>
          <label className="report-field">
            Email
            <input type="email" autoComplete="username" required value={email} disabled={busy} onChange={(e) => setEmail(e.target.value)} />
          </label>
          <label className="report-field">
            Department
            <input type="text" required placeholder="e.g. Finance, Engineering, HR" value={department} disabled={busy} onChange={(e) => setDepartment(e.target.value)} />
          </label>
          <label className="report-field">
            Password (min 8 characters)
            <input type="password" autoComplete="new-password" minLength={8} required value={password} disabled={busy} onChange={(e) => setPassword(e.target.value)} />
          </label>

          {error && <p role="alert">{error}</p>}
          <button className="primary-button" disabled={busy}>
            {busy ? 'Creating account…' : 'Register'}
          </button>
        </form>

        <p className="subtitle" style={{ marginTop: '1rem' }}>
          Already have an account? <Link to="/login">Sign in</Link>
        </p>
      </section>
    </main>
  )
}
