import { useCallback, useState } from 'react'
import Card from '../../components/Card'
import DataTable from '../../components/DataTable'
import { PriorityBadge, StatusBadge } from '../../components/Badges'
import { getCorrelations, getSlaRisks, runMonitor } from '../../api/admin'
import { LoadError, Notice, useAction, useLoad } from './adminUi'

const TONE = { HIGH: 'danger', MEDIUM: 'warning', LOW: 'success' }

export default function AdminSlaViolations() {
  const [minLevel, setMinLevel] = useState('')
  const loadRisks = useCallback((o) => getSlaRisks(minLevel, o), [minLevel])
  const risks = useLoad(loadRisks)
  const groups = useLoad(getCorrelations)
  const { notice, busy, run } = useAction()

  async function checkNow() {
    const done = await run(runMonitor, 'SLA check finished. Escalations and notifications were sent where needed.')
    if (done) {
      risks.reload()
      groups.reload()
    }
  }

  const riskColumns = [
    { key: 'incidentId', label: 'Incident' },
    { key: 'title', label: 'Title' },
    { key: 'priority', label: 'Priority', render: (r) => <PriorityBadge value={r.priority} /> },
    { key: 'status', label: 'Status', render: (r) => <StatusBadge value={r.status} /> },
    { key: 'technician', label: 'Technician', render: (r) => r.technician || 'Unassigned' },
    {
      key: 'level',
      label: 'Risk',
      render: (r) => <span className={`badge badge--${TONE[r.level] || 'neutral'}`}>{r.level} · {r.score}</span>,
    },
    { key: 'reason', label: 'Why' },
  ]

  return (
    <div className="workspace-stack">
      <Card
        title="SLA risk"
        action={
          <button className="secondary-button" disabled={busy} onClick={checkNow}>
            Run SLA check now
          </button>
        }
      >
        <Notice notice={notice} />
        <label className="report-field">Show
          <select value={minLevel} onChange={(e) => setMinLevel(e.target.value)}>
            <option value="">All open incidents</option>
            <option value="MEDIUM">Medium risk and above</option>
            <option value="HIGH">High risk only</option>
          </select>
        </label>
        <LoadError error={risks.error} />
        {risks.data && (
          <DataTable columns={riskColumns} rows={risks.data} rowKey="incidentId"
            caption="Open incidents ranked by risk of missing their SLA"
            emptyMessage="No incidents at this risk level." />
        )}
      </Card>

      <Card title="Possible outages">
        <LoadError error={groups.error} />
        {groups.data && groups.data.length === 0 && (
          <p>No related incidents right now. A group appears when 2 or more open incidents hit the same service within an hour.</p>
        )}
        {groups.data && groups.data.map((g) => (
          <div key={g.serviceName} style={{ marginBottom: '16px' }}>
            <p><strong>{g.serviceName}</strong> — {g.summary}
              {g.category ? ` (mostly ${g.category})` : ''}</p>
            <DataTable
              columns={[
                { key: 'id', label: 'Incident' },
                { key: 'title', label: 'Title' },
                { key: 'priority', label: 'Priority', render: (i) => <PriorityBadge value={i.priority} /> },
                { key: 'status', label: 'Status', render: (i) => <StatusBadge value={i.status} /> },
              ]}
              rows={g.incidents}
              caption={`Incidents on ${g.serviceName}`}
            />
          </div>
        ))}
      </Card>
    </div>
  )
}
