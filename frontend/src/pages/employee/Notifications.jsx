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

export default function Notifications() {
  const { user } = useSession()
  const key = `itsm-seen-resolutions:${user.userId}`
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
    .filter(({ resolvedAt }) => resolvedAt)
    .sort((a, b) => new Date(b.resolvedAt) - new Date(a.resolvedAt))

  return (
    <>
      <div className="page-heading"><h1>Notifications</h1></div>
      <Card title="Resolution updates">
        <p className="subtitle">Read status is saved in this browser.</p>
        {error ? (
          <>
            <p role="alert">{error}</p>
            <button className="secondary-button" onClick={() => {
              setError('')
              setAttempt((value) => value + 1)
            }}>Try again</button>
          </>
        ) : !result ? (
          <p role="status">Loading resolution updates…</p>
        ) : resolutions.length === 0 ? (
          <p>No resolutions to show yet.</p>
        ) : (
          <ul className="attention-list">
            {resolutions.map((incident) => {
              const id = `${incident.id}:${incident.resolvedAt}`
              return (
                <li key={id}>
                  <div>
                    <span className="incident-id">{incident.id}</span>
                    <h3>{incident.title}</h3>
                    <p>Resolved <time dateTime={incident.resolvedAt}>
                      {new Date(incident.resolvedAt).toLocaleString('en-IN')}
                    </time></p>
                    <Link className="incident-link" to={`/employee/incidents/${encodeURIComponent(incident.id)}`}>
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
