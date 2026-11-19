import { useCallback, useState } from 'react'
import { Link, useParams } from 'react-router'
import Card from '../../components/Card'
import { PriorityBadge, SlaBadge, StatusBadge } from '../../components/Badges'
import { getIncident, getIncidentAudit, getIncidentNotes } from '../../api/incidents'
import {
  assignIncident,
  autoAssignIncident,
  closeIncident,
  getTechnicians,
} from '../../api/admin'
import { LoadError, Notice, useAction, useLoad } from './adminUi'

function when(value) {
  if (!value) return '—'
  const d = new Date(value)
  return Number.isNaN(d.getTime())
    ? value
    : d.toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })
}

export default function AdminIncidentDetail() {
  const { id } = useParams()
  const loadIncident = useCallback((o) => getIncident(id, o), [id])
  const loadAudit = useCallback((o) => getIncidentAudit(id, o), [id])
  const loadNotes = useCallback((o) => getIncidentNotes(id, o), [id])
  const incidentLoad = useLoad(loadIncident)
  const audit = useLoad(loadAudit)
  const notes = useLoad(loadNotes)
  const techs = useLoad(getTechnicians)
  const { notice, busy, run } = useAction()
  const [chosen, setChosen] = useState('')
  const [closing, setClosing] = useState('')

  function refreshAll() {
    incidentLoad.reload()
    audit.reload()
    notes.reload()
    techs.reload()
  }

  async function assign() {
    const name = techs.data?.find((t) => t.id === chosen)?.name
    const done = await run(() => assignIncident(id, chosen), `Assigned to ${name}.`)
    if (done) refreshAll()
  }

  async function autoAssign() {
    const done = await run(() => autoAssignIncident(id), 'Assigned automatically using the current strategy.')
    if (done) refreshAll()
  }

  async function close() {
    const done = await run(() => closeIncident(id, closing.trim()), 'Incident closed.')
    if (done) {
      setClosing('')
      refreshAll()
    }
  }

  const incident = incidentLoad.data

  if (incidentLoad.error) {
    return (
      <Card title="Unable to open incident">
        <p role="alert">{incidentLoad.error}</p>
        <Link className="incident-link" to="/admin/incidents">Back to incidents</Link>
      </Card>
    )
  }
  if (!incident) return <Card><p role="status">Loading incident…</p></Card>

  const open = !['RESOLVED', 'VERIFIED', 'CLOSED'].includes(incident.status)
  const canAssign = ['CLASSIFIED', 'ASSIGNED'].includes(incident.status)

  return (
    <div className="workspace-stack">
      <Link className="incident-link" to="/admin/incidents">← Back to incidents</Link>

      <div className="page-heading detail-heading">
        <div>
          <p className="eyebrow">{incident.id}</p>
          <h1>{incident.title}</h1>
          <p className="subtitle">Reported by {incident.reporter?.name} on {when(incident.createdAt)}</p>
        </div>
        <div className="detail-badges">
          <PriorityBadge value={incident.priority} />
          <StatusBadge value={incident.status} />
          {incident.slaStatus && <SlaBadge value={incident.slaStatus} />}
        </div>
      </div>

      <Notice notice={notice} />

      <Card title="Incident information">
        <p className="detail-description">{incident.description}</p>
        <dl className="facts-grid">
          <div><dt>Service</dt><dd>{incident.service?.name}</dd></div>
          <div><dt>Category</dt><dd>{incident.category}</dd></div>
          <div><dt>Technician</dt><dd>{incident.assignedTechnician?.name ?? 'Not assigned'}</dd></div>
          <div><dt>Affected users</dt><dd>{incident.affectedUsers}</dd></div>
          <div><dt>Impact</dt><dd>{incident.impact}</dd></div>
          <div><dt>Urgency</dt><dd>{incident.urgency}</dd></div>
          <div><dt>Response deadline</dt><dd>{when(incident.responseDeadline)}</dd></div>
          <div><dt>Resolution deadline</dt><dd>{when(incident.resolutionDeadline)}</dd></div>
        </dl>
        {incident.resolutionNote && <p className="detail-description">Resolution: {incident.resolutionNote}</p>}
        {incident.closureNote && <p className="detail-description">Closure: {incident.closureNote}</p>}
        {incident.feedback && (
          <p className="subtitle">Reporter feedback: {incident.feedback.rating}/5
            {incident.feedback.comment ? ` — ${incident.feedback.comment}` : ''}</p>
        )}
      </Card>

      {open && (
        <Card title="Assignment">
          <LoadError error={techs.error} />
          {canAssign ? (
            <>
              <label className="report-field">Assign to a technician
                <select value={chosen} onChange={(e) => setChosen(e.target.value)}>
                  <option value="">Choose a technician…</option>
                  {(techs.data ?? []).map((t) => (
                    <option key={t.id} value={t.id} disabled={!t.available}>
                      {t.name} — {t.activeIncidents} active{t.available ? '' : ' (unavailable)'} · {t.skills.join(', ')}
                    </option>
                  ))}
                </select>
              </label>
              <div className="detail-actions">
                <button className="primary-button" disabled={busy || !chosen} onClick={assign}>Assign</button>
                <button className="secondary-button" disabled={busy} onClick={autoAssign}>
                  Auto-assign using current strategy
                </button>
              </div>
              <p className="report-help">Re-assigning is possible until the technician acknowledges the incident.</p>
            </>
          ) : (
            <p className="subtitle">The technician has acknowledged this incident, so it can no longer be re-assigned.</p>
          )}
        </Card>
      )}

      {incident.status === 'VERIFIED' && (
        <Card title="Close this incident">
          <p className="subtitle">The reporter has verified the fix. Add a closing note and close it.</p>
          <label className="report-field">Closing note
            <textarea rows={3} value={closing} onChange={(e) => setClosing(e.target.value)} />
          </label>
          <button className="primary-button" disabled={busy || !closing.trim()} onClick={close}>Close incident</button>
        </Card>
      )}

      <Card title="Notes and information requests">
        <LoadError error={notes.error} />
        {notes.data && notes.data.length === 0 && <p className="subtitle">No notes yet.</p>}
        {notes.data && notes.data.length > 0 && (
          <ul className="notes-list">
            {notes.data.map((n, i) => (
              <li key={`${n.createdAt}-${i}`}>
                <div className="note-meta">
                  <strong>{n.author}</strong>
                  {n.type === 'INFO_REQUEST' && <span className="badge badge--warning">Question from support</span>}
                  {n.type === 'INFO_RESPONSE' && <span className="badge badge--info">Reply</span>}
                  <time dateTime={n.createdAt}>{when(n.createdAt)}</time>
                </div>
                <p>{n.text}</p>
              </li>
            ))}
          </ul>
        )}
      </Card>

      <Card title="Incident history">
        <LoadError error={audit.error} />
        {audit.data && (
          <ol className="audit-timeline">
            {audit.data.map((e, i) => (
              <li key={`${e.timestamp}-${i}`}>
                <strong>{e.description}</strong>
                <span>{e.user}</span>
                <time dateTime={e.timestamp}>{when(e.timestamp)}</time>
              </li>
            ))}
          </ol>
        )}
      </Card>
    </div>
  )
}
