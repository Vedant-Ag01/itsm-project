import { useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router'
import Card from '../../components/Card'
import {
  PriorityBadge,
  SlaBadge,
  StatusBadge,
} from '../../components/Badges'
import {
  addIncidentNote,
  getIncidentNotes,
  technicianAction,
  getIncident,
  getIncidentAudit,
  submitIncidentFeedback,
  USING_MOCKS,
  verifyIncident,
} from '../../api/incidents'

function formatDate(value) {
  if (!value) return '—'

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) return value

  return new Intl.DateTimeFormat('en-IN', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date)
}

export default function IncidentDetails({ role = 'employee' }) {
  const { id } = useParams()

  // Reset forms and loading state when navigating to another incident.
  return <IncidentDetailsContent key={id} id={id} role={role} />
}

function IncidentDetailsContent({ id, role }) {
  const technician = role === 'technician'
  const back = technician ? '/technician' : '/employee/incidents'
  const [resolution, setResolution] = useState('')
  const [notesState, setNotesState] = useState({ items: [], error: '', loading: true })
  const [notesAttempt, setNotesAttempt] = useState(0)
  useEffect(() => {
    const controller = new AbortController()
    getIncidentNotes(id, { signal: controller.signal }).then((items) => {
      if (!controller.signal.aborted) setNotesState({ items, error: '', loading: false })
    }).catch((error) => {
      if (!controller.signal.aborted) setNotesState({ items: [], error: error.message, loading: false })
    })
    return () => controller.abort()
  }, [id, notesAttempt])

  const [state, setState] = useState({
    phase: 'loading',
    incident: null,
    error: '',
  })
  const [attempt, setAttempt] = useState(0)
  const [audit, setAudit] = useState({
    phase: 'loading',
    items: [],
    error: '',
  })
  const [busy, setBusy] = useState(false)
  const requestInFlight = useRef(false)
  const mounted = useRef(false)
  const [notice, setNotice] = useState(null)
  const [note, setNote] = useState('')
  const [rating, setRating] = useState('5')
  const [comment, setComment] = useState('')

  useEffect(() => {
    mounted.current = true
    return () => {
      mounted.current = false
    }
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    let active = true

    getIncident(id, { signal: controller.signal })
      .then((incident) => {
        if (active) {
          setState({ phase: 'ready', incident, error: '' })
          setRating(String(incident.feedback?.rating ?? 5))
          setComment(incident.feedback?.comment ?? '')
        }
      })
      .catch((error) => {
        if (active && error.name !== 'AbortError') {
          setState({
            phase: 'error',
            incident: null,
            error: error.message,
          })
        }
      })

    if (USING_MOCKS) getIncidentAudit(id, { signal: controller.signal })
      .then((items) => {
        if (active) {
          setAudit({ phase: 'ready', items, error: '' })
        }
      })
      .catch((error) => {
        if (active && error.name !== 'AbortError') {
          setAudit({ phase: 'error', items: [], error: error.message })
        }
      })

    return () => {
      active = false
      controller.abort()
    }
  }, [id, attempt])

  async function refreshAudit() {
    setAudit((current) => ({ ...current, phase: 'loading', error: '' }))

    try {
      const items = await getIncidentAudit(id)

      if (mounted.current) {
        setAudit({ phase: 'ready', items, error: '' })
      }
    } catch (error) {
      if (mounted.current) {
        setAudit({
          phase: 'error',
          items: [],
          error: `Unable to load history: ${error.message}`,
        })
      }
    }
  }

  async function runAction(action, successMessage, afterSuccess) {
    if (requestInFlight.current) return

    requestInFlight.current = true
    setBusy(true)
    setNotice(null)

    try {
      const incident = await action()

      if (!mounted.current) return

      setState((current) => ({ phase: 'ready', incident: { ...current.incident, ...incident }, error: '' }))
      if (incident.notes) setNotesState({ items: incident.notes, loading: false, error: '' })
      setNotice({ type: 'success', message: successMessage })
      afterSuccess?.()

      // Failure here must not make a successful update look unsuccessful.
      if (USING_MOCKS) await refreshAudit()
    } catch (error) {
      if (mounted.current) {
        setNotice({ type: 'error', message: error.message })
      }
    } finally {
      requestInFlight.current = false

      if (mounted.current) {
        setBusy(false)
      }
    }
  }

  function retryIncident() {
    setState({ phase: 'loading', incident: null, error: '' })
    setAudit({ phase: 'loading', items: [], error: '' })
    setAttempt((value) => value + 1)
  }

  if (state.phase === 'loading') {
    return (
      <Card>
        <div className="empty-state" role="status">
          Loading incident…
        </div>
      </Card>
    )
  }

  if (state.phase === 'error') {
    return (
      <Card title="Unable to open incident">
        <p className="subtitle" role="alert">{state.error}</p>
        <div className="detail-actions">
          <button className="secondary-button" onClick={retryIncident}>
            Try again
          </button>
          <Link className="incident-link" to={back}>
            Back to my incidents
          </Link>
        </div>
      </Card>
    )
  }

  const incident = state.incident
  const canVerify = !technician && incident.status === 'RESOLVED'
  const canGiveFeedback = !technician && ['RESOLVED', 'VERIFIED', 'CLOSED'].includes(
    incident.status,
  )
  const canAddNote = incident.status !== 'CLOSED' && !notesState.loading && !notesState.error

  return (
    <>
      <Link className="incident-link" to={back}>
        ← Back to incidents
      </Link>

      <div className="page-heading detail-heading">
        <div>
          <p className="eyebrow">{incident.id}</p>
          <h1>{incident.title}</h1>
          <p className="subtitle">
            Reported {formatDate(incident.createdAt)}
          </p>
        </div>

        <div className="detail-badges">
          <PriorityBadge value={incident.priority} />
          <StatusBadge value={incident.status} />
          <span title="SLA status">{incident.slaStatus ? <SlaBadge value={incident.slaStatus} /> : "Not available"}</span>
        </div>
      </div>

      {USING_MOCKS && (
        <div className="data-notice">
          Demo incident · Updates stay during this session and reset on refresh.
        </div>
      )}

      {notice && (
        <div
          className={`action-notice action-notice--${notice.type}`}
          role={notice.type === 'error' ? 'alert' : 'status'}
        >
          {notice.message}
        </div>
      )}

      {technician && ['ASSIGNED', 'ACKNOWLEDGED', 'IN_PROGRESS'].includes(incident.status) && (
        <Card title="Work on this incident">
          {incident.status === 'IN_PROGRESS' ? (
            <form className="detail-form" onSubmit={(event) => {
              event.preventDefault()
              if (!resolution.trim()) return
              void runAction(() => technicianAction(id, 'resolve', resolution.trim()), 'Incident resolved. The reporter can now verify the fix.', () => setResolution(''))
            }}>
              <label className="filter-field">Resolution note<textarea required rows={4} value={resolution} disabled={busy} onChange={(e) => setResolution(e.target.value)} /></label>
              <button className="primary-button" disabled={busy || !resolution.trim()}>{busy ? 'Saving…' : 'Resolve incident'}</button>
            </form>
          ) : (
            <button className="primary-button" disabled={busy} onClick={() => void runAction(
              () => technicianAction(id, incident.status === 'ASSIGNED' ? 'acknowledge' : 'start-work'),
              incident.status === 'ASSIGNED' ? 'Incident acknowledged.' : 'Work started.',
            )}>{busy ? 'Saving…' : incident.status === 'ASSIGNED' ? 'Acknowledge incident' : 'Start work'}</button>
          )}
        </Card>
      )}
      <div className="incident-detail-grid">
        <div className="detail-stack">
          <Card title="Incident information">
            <p className="detail-description">{incident.description}</p>

            <dl className="facts-grid">
              <div>
                <dt>Service</dt>
                <dd>{incident.service.name}</dd>
              </div>
              <div>
                <dt>Category</dt>
                <dd>{incident.category}</dd>
              </div>
              <div>
                <dt>Technician</dt>
                <dd>{incident.assignedTechnician?.name ?? 'Not assigned'}</dd>
              </div>
              <div>
                <dt>Affected users</dt>
                <dd>{incident.affectedUsers}</dd>
              </div>
              <div>
                <dt>Impact</dt>
                <dd>{incident.impact}</dd>
              </div>
              <div>
                <dt>Urgency</dt>
                <dd>{incident.urgency}</dd>
              </div>
              <div>
                <dt>Response deadline</dt>
                <dd>{formatDate(incident.responseDeadline)}</dd>
              </div>
              <div>
                <dt>Resolution deadline</dt>
                <dd>{formatDate(incident.resolutionDeadline)}</dd>
              </div>
            </dl>
          </Card>

          <Card title="Notes and additional information">
            {notesState.loading && <p role="status">Loading notes…</p>}
            {notesState.error && <div><p role="alert">{notesState.error}</p><button className="secondary-button" onClick={() => setNotesAttempt((n) => n + 1)}>Retry notes</button></div>}
            {notesState.items.length === 0 ? (
              <p className="subtitle">No notes have been added yet.</p>
            ) : (
              <ul className="notes-list">
                {notesState.items.map((entry, index) => (
                  <li key={`${entry.createdAt}-${index}`}>
                    <div className="note-meta">
                      <strong>{entry.author}</strong>
                      <time dateTime={entry.createdAt}>
                        {formatDate(entry.createdAt)}
                      </time>
                    </div>
                    <p>{entry.text}</p>
                  </li>
                ))}
              </ul>
            )}

            {canAddNote ? (
              <form
                className="detail-form"
                onSubmit={(event) => {
                  event.preventDefault()
                  void runAction(
                    () => addIncidentNote(id, note.trim()),
                    'Your information was added.',
                    () => setNote(''),
                  )
                }}
              >
                <label className="filter-field" htmlFor="incident-note">
                  <span>Add information or respond to support</span>
                  <textarea
                    id="incident-note"
                    rows={4}
                    required
                    value={note}
                    disabled={busy}
                    onChange={(event) => setNote(event.target.value)}
                    placeholder="Describe what you tried or provide more details."
                  />
                </label>
                <button
                  className="primary-button"
                  disabled={busy || !note.trim()}
                >
                  {busy ? 'Please wait…' : 'Add information'}
                </button>
              </form>
            ) : (
              <p className="subtitle">
                Notes are unavailable while loading, after a loading error, or once the incident is closed.
              </p>
            )}
          </Card>

          { (incident.resolutionNote || canVerify) && (
            <Card title="Resolution">
              <p className="detail-description">{incident.resolutionNote ?? "Support marked this incident resolved. Check that your issue is fixed before verifying."}</p>
              <p className="subtitle">
                Resolved {formatDate(incident.resolvedAt)}
              </p>

              {canVerify ? (
                <div className="detail-actions">
                  <p className="subtitle">
                    Confirm only after checking that the issue is fixed.
                  </p>
                  <button
                    className="primary-button"
                    disabled={busy}
                    onClick={() =>
                      void runAction(
                        () => verifyIncident(id),
                        'Resolution verified. An administrator can now close the incident.',
                      )
                    }
                  >
                    {busy ? 'Please wait…' : 'Confirm the fix works'}
                  </button>
                </div>
              ) : (
                <p className="subtitle">
                  {incident.status === 'CLOSED'
                    ? 'This incident has been closed.'
                    : incident.status === 'RESOLVED' ? 'Awaiting verification by the reporter.' : 'The resolution has been verified.'}
                </p>
              )}

              {incident.closureNote && (
                <p className="detail-description">
                  Closure note: {incident.closureNote}
                </p>
              )}
            </Card>
          )}

          {canGiveFeedback && (
            <Card title="Resolution feedback">
              {incident.feedback && (
                <p className="subtitle">
                  Saved rating: {incident.feedback.rating}/5
                  {incident.feedback.comment
                    ? ` — ${incident.feedback.comment}`
                    : ''}
                </p>
              )}

              <form
                className="detail-form"
                onSubmit={(event) => {
                  event.preventDefault()
                  void runAction(
                    () =>
                      submitIncidentFeedback(id, Number(rating), comment),
                    'Your feedback was saved.',
                  )
                }}
              >
                <label className="filter-field" htmlFor="feedback-rating">
                  <span>Rating</span>
                  <select
                    id="feedback-rating"
                    value={rating}
                    disabled={busy}
                    onChange={(event) => setRating(event.target.value)}
                  >
                    <option value="5">5 — Excellent</option>
                    <option value="4">4 — Good</option>
                    <option value="3">3 — Okay</option>
                    <option value="2">2 — Poor</option>
                    <option value="1">1 — Very poor</option>
                  </select>
                </label>

                <label className="filter-field" htmlFor="feedback-comment">
                  <span>Comment (optional)</span>
                  <textarea
                    id="feedback-comment"
                    rows={3}
                    value={comment}
                    disabled={busy}
                    onChange={(event) => setComment(event.target.value)}
                  />
                </label>

                <button className="primary-button" disabled={busy}>
                  {busy ? 'Please wait…' : 'Save feedback'}
                </button>
              </form>
            </Card>
          )}
        </div>

        {USING_MOCKS ? <Card title="Incident history">
          {audit.phase === 'loading' && (
            <p className="subtitle" role="status">Loading history…</p>
          )}

          {audit.phase === 'error' && (
            <div className="detail-form">
              <p role="alert">{audit.error}</p>
              <button
                className="secondary-button"
                disabled={busy}
                onClick={() => void refreshAudit()}
              >
                Reload history
              </button>
            </div>
          )}

          {audit.phase === 'ready' && (
            audit.items.length === 0 ? (
              <p className="subtitle">No history is available yet.</p>
            ) : (
              <ol className="audit-timeline">
                {audit.items.map((event, index) => (
                  <li key={`${event.timestamp}-${index}`}>
                    <strong>{event.description}</strong>
                    <span>{event.user}</span>
                    <time dateTime={event.timestamp}>
                      {formatDate(event.timestamp)}
                    </time>
                  </li>
                ))}
              </ol>
            )
          )}
        </Card> : <Card title="Recorded milestones">
          <p className="subtitle">Available incident timestamps. A full audit trail is not provided by the server.</p>
          <ol className="audit-timeline">
            {[
              ['Reported', incident.createdAt],
              ['Acknowledged', incident.acknowledgedAt],
              ['Resolved', incident.resolvedAt],
              ['Closed', incident.closedAt],
            ].filter(([, timestamp]) => timestamp).map(([label, timestamp]) => (
              <li key={label}><strong>{label}</strong><time dateTime={timestamp}>{formatDate(timestamp)}</time></li>
            ))}
          </ol>
        </Card>}
      </div>
    </>
  )
}
