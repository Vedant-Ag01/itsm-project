import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { getIncidents } from '../../api/incidents'
import { useSession } from '../../auth/session'
import Card from '../../components/Card'

function readSeen(key) {
  try {
    const value = JSON.parse(localStorage.getItem(key))
    return Array.isArray(value) ? value : []
  } catch {
    return []
  }
}

export default function Notifications({ role = 'employee' }) {
  const { user } = useSession()
  const key = `itsm-seen-updates:${role}:${user.userId}`
  const [seen, setSeen] = useState(() => readSeen(key))
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    getIncidents({}, { signal: controller.signal })
      .then(({ items }) => {
        if (!controller.signal.aborted) setResult(items)
      })
      .catch((failure) => {
        if (!controller.signal.aborted) setError(failure.message)
      })
    return () => controller.abort()
  }, [attempt])

  function markRead(id) {
    const updated = [...seen, id]
    setSeen(updated)
    try {
      localStorage.setItem(key, JSON.stringify(updated))
    } catch {
      // Keep the read status for the current visit.
    }
  }

  const resolutions = (result ?? [])
    .filter((item) => role === 'technician' || item.resolvedAt)
    .map((item) => ({ ...item, updateTime: item.closedAt || item.resolvedAt || item.acknowledgedAt || item.createdAt }))
    .sort((a, b) => new Date(b.updateTime) - new Date(a.updateTime))

  return (
    <>
      <div className="page-heading"><h1>Notifications</h1></div>
      <Card title={role === 'technician' ? 'Assignment updates' : 'Resolution updates'}>
        <p className="subtitle">Updates are derived from your current incidents, not a notification inbox. Read status is saved in this browser.</p>
        {error ? (
          <>
            <p role="alert">{error}</p>
            <button className="secondary-button" onClick={() => {
              setError('')
              setAttempt((value) => value + 1)
            }}>Try again</button>
          </>
        ) : !result ? (
          <p role="status">Loading incident updates…</p>
        ) : resolutions.length === 0 ? (
          <p>No incident updates to show yet.</p>
        ) : (
          <ul className="attention-list">
            {resolutions.map((incident) => {
              const id = `${incident.id}:${incident.status}:${incident.updateTime}`
              return (
                <li key={id}>
                  <div>
                    <span className="incident-id">{incident.id}</span>
                    <h3>{incident.title}</h3>
                    <p>{incident.status.replaceAll('_', ' ')} · <time dateTime={incident.updateTime}>
                      {new Date(incident.updateTime).toLocaleString('en-IN')}
                    </time></p>
                    <Link className="incident-link" to={`/${role}/incidents/${encodeURIComponent(incident.id)}`}>
                      View incident
                    </Link>
                  </div>
                  {seen.includes(id) ? <span>Read</span> : (
                    <button className="secondary-button" onClick={() => markRead(id)}>Mark as read</button>
                  )}
                </li>
              )
            })}
          </ul>
        )}
      </Card>
    </>
  )
}
