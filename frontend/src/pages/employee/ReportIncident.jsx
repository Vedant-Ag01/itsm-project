import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router'
import Card from '../../components/Card'
import {
  createIncident,
  getReportServices,
  USING_MOCKS,
} from '../../api/incidents'

const initialForm = {
  title: '',
  description: '',
  serviceId: '',
  impact: '',
  urgency: '',
  affectedUsers: '1',
}

function validate(form) {
  const errors = {}

  if (!form.title.trim()) {
    errors.title = 'Enter a short title for the incident.'
  }

  if (!form.description.trim()) {
    errors.description = 'Describe what happened.'
  }

  if (!form.serviceId) {
    errors.serviceId = 'Choose the affected service.'
  }

  if (!form.impact) {
    errors.impact = 'Choose the impact.'
  }

  if (!form.urgency) {
    errors.urgency = 'Choose the urgency.'
  }

  const affectedUsers = Number(form.affectedUsers)

  if (
    !form.affectedUsers.trim() ||
    !Number.isSafeInteger(affectedUsers) ||
    affectedUsers < 1
  ) {
    errors.affectedUsers = 'Enter a whole number of at least 1.'
  }

  return errors
}

export default function ReportIncident() {
  const [form, setForm] = useState(initialForm)
  const [errors, setErrors] = useState({})
  const [services, setServices] = useState({
    phase: 'loading',
    items: [],
    error: '',
  })
  const [attempt, setAttempt] = useState(0)
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState('')
  const [result, setResult] = useState(null)

  const submissionLock = useRef(false)
  const formRef = useRef(null)
  const successRef = useRef(null)

  useEffect(() => {
    let active = true

    getReportServices()
      .then((items) => {
        if (active) {
          setServices({ phase: 'ready', items, error: '' })
        }
      })
      .catch((error) => {
        if (active) {
          setServices({
            phase: 'error',
            items: [],
            error: error.message,
          })
        }
      })

    return () => {
      active = false
    }
  }, [attempt])

  useEffect(() => {
    if (result) {
      successRef.current?.focus()
    }
  }, [result])

  function updateField(event) {
    const { name, value } = event.target

    setForm((current) => ({ ...current, [name]: value }))
    setErrors((current) => ({ ...current, [name]: undefined }))
    setSubmitError('')
  }

  function fieldProps(name) {
    return {
      id: name,
      name,
      value: form[name],
      onChange: updateField,
      'aria-invalid': Boolean(errors[name]),
      'aria-describedby': errors[name] ? `${name}-error` : undefined,
    }
  }

  function fieldError(name) {
    return errors[name] ? (
      <span className="report-field-error" id={`${name}-error`}>
        {errors[name]}
      </span>
    ) : null
  }

  async function handleSubmit(event) {
    event.preventDefault()

    if (submissionLock.current || services.phase !== 'ready') {
      return
    }

    const nextErrors = validate(form)
    setErrors(nextErrors)
    setSubmitError('')

    if (Object.keys(nextErrors).length > 0) {
      const firstField = Object.keys(nextErrors)[0]
      formRef.current?.elements.namedItem(firstField)?.focus()
      return
    }

    submissionLock.current = true
    setSubmitting(true)

    try {
      const response = await createIncident({
        title: form.title.trim(),
        description: form.description.trim(),
        serviceId: Number(form.serviceId),
        impact: form.impact,
        urgency: form.urgency,
        affectedUsers: Number(form.affectedUsers),
      })

      setResult(response)
    } catch (error) {
      setSubmitError(error.message || 'Could not report the incident.')
    } finally {
      submissionLock.current = false
      setSubmitting(false)
    }
  }

  function reportAnother() {
    setForm({ ...initialForm })
    setErrors({})
    setSubmitError('')
    setResult(null)
  }

  if (result) {
    const duplicates = result.possibleDuplicates ?? []

    return (
      <>
        <div className="page-heading">
          <div>
            <p className="eyebrow">Report incident</p>
            <h1 ref={successRef} tabIndex={-1}>
              Incident reported
            </h1>
            <p className="subtitle">
              Your incident reference is {result.incident.id}.
            </p>
          </div>
        </div>

        <Card title={result.incident.title}>
          <p>
            Your incident has been submitted. Open its details to follow
            progress and add notes.
          </p>

          {USING_MOCKS && (
            <p className="subtitle">
              Demo mode: this incident is stored in memory and resets
              when you refresh.
            </p>
          )}

          <div className="report-actions">
            <Link
              className="primary-button"
              to={`/employee/incidents/${result.incident.id}`}
            >
              View incident
            </Link>

            <button
              type="button"
              className="theme-toggle"
              onClick={reportAnother}
            >
              Report another
            </button>

            <Link className="text-link" to="/employee/incidents">
              My incidents
            </Link>
          </div>
        </Card>

        {duplicates.length > 0 && (
          <div className="report-duplicates">
            <Card title="Possible duplicates">
              <p className="subtitle">
                Your incident was created. These existing incidents may
                describe the same issue.
              </p>

              {USING_MOCKS && (
                <p className="subtitle">
                  This demo matches identical titles only. Backend
                  duplicate detection will replace it.
                </p>
              )}

              <ul className="report-duplicate-list">
                {duplicates.map((duplicate) => (
                  <li key={duplicate.id}>
                    <Link
                      className="incident-link"
                      to={`/employee/incidents/${duplicate.id}`}
                    >
                      {duplicate.id} · {duplicate.title}
                    </Link>
                  </li>
                ))}
              </ul>
            </Card>
          </div>
        )}
      </>
    )
  }

  return (
    <>
      <div className="page-heading">
        <div>
          <p className="eyebrow">Employee workspace</p>
          <h1>Report incident</h1>
          <p className="subtitle">
            Tell us what happened and which service is affected.
          </p>
        </div>
      </div>

      {USING_MOCKS && (
        <p className="report-demo-note">
          Demo mode · Services and classification are sample data.
          Created incidents reset on refresh.
        </p>
      )}

      <Card title="Incident information">
        {services.phase === 'loading' && (
          <p role="status">Loading available services…</p>
        )}

        {services.phase === 'error' && (
          <div>
            <p role="alert">{services.error}</p>
            <button
              type="button"
              className="theme-toggle"
              onClick={() => {
                setServices({
                  phase: 'loading',
                  items: [],
                  error: '',
                })
                setAttempt((current) => current + 1)
              }}
            >
              Retry
            </button>
          </div>
        )}

        {services.phase === 'ready' && services.items.length === 0 && (
          <p role="status">
            No services are available. Contact your administrator.
          </p>
        )}

        {services.phase === 'ready' && services.items.length > 0 && (
          <form ref={formRef} onSubmit={handleSubmit} noValidate>
            <p className="subtitle">All fields are required.</p>

            <fieldset className="report-fields" disabled={submitting}>
              <div className="report-field">
                <label htmlFor="title">Incident title</label>
                <input
                  {...fieldProps('title')}
                  type="text"
                  placeholder="For example: Campus Wi-Fi unavailable"
                  required
                />
                {fieldError('title')}
              </div>

              <div className="report-field">
                <label htmlFor="description">Description</label>
                <textarea
                  {...fieldProps('description')}
                  rows={5}
                  placeholder="What happened? When did it start? Include any error messages."
                  required
                />
                {fieldError('description')}
              </div>

              <div className="report-form-grid">
                <div className="report-field">
                  <label htmlFor="serviceId">Affected service</label>
                  <select {...fieldProps('serviceId')} required>
                    <option value="">Select a service</option>
                    {services.items.map((service) => (
                      <option key={service.id} value={service.id}>
                        {service.name}
                      </option>
                    ))}
                  </select>
                  {fieldError('serviceId')}
                </div>

                <div className="report-field">
                  <label htmlFor="affectedUsers">Affected users</label>
                  <input
                    {...fieldProps('affectedUsers')}
                    type="number"
                    min="1"
                    step="1"
                    required
                  />
                  {fieldError('affectedUsers')}
                </div>

                <div className="report-field">
                  <label htmlFor="impact">Impact</label>
                  <select {...fieldProps('impact')} required>
                    <option value="">Select impact</option>
                    <option value="LOW">Low</option>
                    <option value="MEDIUM">Medium</option>
                    <option value="HIGH">High</option>
                  </select>
                  <span className="report-help">
                    How much is the issue disrupting work?
                  </span>
                  {fieldError('impact')}
                </div>

                <div className="report-field">
                  <label htmlFor="urgency">Urgency</label>
                  <select {...fieldProps('urgency')} required>
                    <option value="">Select urgency</option>
                    <option value="LOW">Low</option>
                    <option value="MEDIUM">Medium</option>
                    <option value="HIGH">High</option>
                  </select>
                  <span className="report-help">
                    How quickly does work need to resume?
                  </span>
                  {fieldError('urgency')}
                </div>
              </div>

              <p className="report-help">
                Priority is assigned automatically after submission.
              </p>

              <div className="report-actions">
                <button className="primary-button" type="submit">
                  {submitting ? 'Submitting…' : 'Submit incident'}
                </button>
              </div>
            </fieldset>

            {submitError && (
              <p className="report-field-error" role="alert">
                {submitError} Your entered details have been preserved.
              </p>
            )}

            {!submitting && (
              <Link className="report-back-link text-link" to="/employee/incidents">
                Back to my incidents
              </Link>
            )}
          </form>
        )}
      </Card>
    </>
  )
}