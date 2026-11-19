import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router'
import ThemeToggle from '../components/ThemeToggle'
import { REGISTRATION_ENABLED, registerEmployee } from '../api/registration'
import { validateRegistration } from '../validation/registration'

const initial = { name: '', email: '', department: '', password: '', confirmPassword: '' }
const fields = [
  ['name', 'Full name', 'text', 'name', 100],
  ['email', 'Email address', 'email', 'email', 254],
  ['department', 'Department (optional)', 'text', 'organization-title', 100],
  ['password', 'Password', 'password', 'new-password', 128],
  ['confirmPassword', 'Confirm password', 'password', 'new-password', 128],
]

export default function Register({ theme, onToggle }) {
  const [values, setValues] = useState(initial)
  const [errors, setErrors] = useState({})
  const [failure, setFailure] = useState('')
  const [busy, setBusy] = useState(false)
  const [complete, setComplete] = useState(false)
  const lock = useRef(false)
  const form = useRef(null)
  const mounted = useRef(false)
  const successHeading = useRef(null)
  useEffect(() => {
    mounted.current = true
    document.title = 'Create account · I-ITSM'
    return () => { mounted.current = false }
  }, [])
  useEffect(() => { if (complete) successHeading.current?.focus() }, [complete])

  async function submit(event) {
    event.preventDefault()
    if (lock.current) return
    const nextErrors = validateRegistration(values)
    setErrors(nextErrors); setFailure('')
    if (Object.keys(nextErrors).length) {
      form.current.elements.namedItem(Object.keys(nextErrors)[0])?.focus()
      return
    }
    lock.current = true; setBusy(true)
    try {
      await registerEmployee(values)
      if (mounted.current) { setValues(initial); setComplete(true) }
    } catch (error) {
      if (mounted.current) setFailure(error.status === 409
        ? 'An account with this email already exists. Sign in or use another email.'
        : error.message)
    } finally {
      lock.current = false
      if (mounted.current) setBusy(false)
    }
  }

  return <main className="login-shell"><section className="panel login-panel registration-panel">
    <div className="panel-heading"><strong>I-ITSM</strong><ThemeToggle theme={theme} onToggle={onToggle} /></div>
    {complete ? <>
      <h1 ref={successHeading} tabIndex={-1}>Account created</h1>
      <p role="status">Your employee account is ready. Sign in to report and track incidents.</p>
      <Link className="primary-button" to="/login">Continue to sign in</Link>
    </> : <>
      <h1>Create an account</h1>
      <p className="subtitle">Report issues and follow their progress with an employee account.</p>
      {!REGISTRATION_ENABLED && <p className="data-notice" role="status">Registration is not open yet. Contact your administrator for an account, or sign in with your existing account.</p>}
      <form ref={form} className="detail-form" onSubmit={submit} noValidate aria-busy={busy}>
        {fields.map(([name, label, type, autoComplete, maxLength]) => <label className="report-field" key={name} htmlFor={`register-${name}`}>
          {label}
          <input id={`register-${name}`} name={name} type={type} autoComplete={autoComplete}
            required={name !== 'department'} maxLength={maxLength} disabled={busy}
            value={values[name]} aria-invalid={Boolean(errors[name])}
            aria-describedby={errors[name] ? `${name}-error` : name === 'password' ? 'password-help' : undefined}
            onChange={(event) => {
              setValues((current) => ({ ...current, [name]: event.target.value }))
              setErrors((current) => ({ ...current, [name]: undefined }))
              setFailure('')
            }} />
          {name === 'password' && <span id="password-help" className="report-help">Use 8–128 characters. Spaces are preserved.</span>}
          {errors[name] && <span id={`${name}-error`} className="field-error">{errors[name]}</span>}
        </label>)}
        {Object.values(errors).some(Boolean) && <p role="alert">Please correct the marked fields.</p>}
        {failure && <p role="alert">{failure} Your entered values have been preserved.</p>}
        <button type="submit" className="primary-button" disabled={busy || !REGISTRATION_ENABLED}>{busy ? 'Creating account…' : 'Create employee account'}</button>
      </form>
      <p>Already registered? <Link className="incident-link" to="/login">Sign in</Link></p>
    </>}
  </section></main>
}
