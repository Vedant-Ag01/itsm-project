import { useEffect, useState } from 'react'
import { getAdminAuditLogs } from '../../api/c7'
import Card from '../../components/Card'
import DataTable from '../../components/DataTable'

export default function AuditLogs() {
  const [logs, setLogs] = useState([])
  const [query, setQuery] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const controller = new AbortController()
    getAdminAuditLogs({ signal: controller.signal })
      .then((rows) => { if (!controller.signal.aborted) setLogs(Array.isArray(rows) ? rows : []) })
      .catch((err) => { if (!controller.signal.aborted) setError(err.message) })
      .finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [])

  const filtered = logs.filter((log) => {
    if (!query.trim()) return true
    const q = query.toLowerCase()
    return (
      String(log.incidentId || '').toLowerCase().includes(q) ||
      String(log.eventType || '').toLowerCase().includes(q) ||
      String(log.actor || '').toLowerCase().includes(q) ||
      String(log.description || '').toLowerCase().includes(q)
    )
  })

  return (
    <Card title="System Audit Trail" subtitle="Immutable chronological log of all incident lifecycle, assignment, and SLA events.">
      <div style={{ marginBottom: '1rem' }}>
        <input
          type="search"
          placeholder="Filter by incident ID, event type, actor, or description…"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          style={{ width: '100%', maxWidth: '420px' }}
        />
      </div>
      {error && <p role="alert">{error}</p>}
      {loading ? (
        <p>Loading audit records…</p>
      ) : (
        <DataTable
          rowKey="timestamp"
          caption="Audit records"
          rows={filtered}
          columns={[
            { key: 'timestamp', label: 'Timestamp' },
            { key: 'incidentId', label: 'Incident' },
            { key: 'eventType', label: 'Event' },
            { key: 'actor', label: 'Actor' },
            {
              key: 'transition',
              label: 'Transition',
              render: (r) => `${r.previousStatus || '—'} → ${r.newStatus || '—'}`,
            },
            { key: 'description', label: 'Details' },
          ]}
        />
      )}
    </Card>
  )
}
