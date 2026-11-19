import { Link } from 'react-router'
import Card from '../../components/Card'
import { getCorrelations, getOverview, getResolutionTime, getSlaRisks } from '../../api/admin'
import { pageUrl } from '../../layouts/workspaces'
import { gridStyle, LoadError, statStyle, useLoad } from './adminUi'

const loadHigh = (o) => getSlaRisks('HIGH', o)

export default function AdminOverview() {
  const overview = useLoad(getOverview)
  const time = useLoad(getResolutionTime)
  const risks = useLoad(loadHigh)
  const groups = useLoad(getCorrelations)

  const o = overview.data
  const cards = o
    ? [
        ['Total incidents', o.totalIncidents],
        ['Open', o.openIncidents],
        ['Critical (P1)', o.criticalP1Incidents ?? 0],
        ['Resolved', o.resolvedIncidents],
        ['SLA violations', o.slaViolations],
        ['Avg resolution', `${Math.round(time.data?.averageResolutionMinutes ?? 0)} min`],
      ]
    : []

  return (
    <div className="workspace-stack">
      <Card title="System overview">
        <LoadError error={overview.error} />
        <div style={gridStyle}>
          {cards.map(([label, value]) => (
            <div key={label} style={statStyle}>
              <div style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>{label}</div>
              <div style={{ fontSize: '28px', fontWeight: 600 }}>{value}</div>
            </div>
          ))}
        </div>
      </Card>

      <Card title="Needs attention"
        action={<Link className="text-link" to={pageUrl('admin', 'sla-violations')}>Open SLA risk</Link>}>
        <LoadError error={risks.error} />
        {risks.data && (
          <p>
            <strong>{risks.data.length}</strong> open incident{risks.data.length === 1 ? ' is' : 's are'} at
            high risk of missing the SLA
            {risks.data[0] ? ` — worst: ${risks.data[0].incidentId} (${risks.data[0].reason})` : ''}.
          </p>
        )}
        {groups.data && (
          <p>
            <strong>{groups.data.length}</strong> possible outage group{groups.data.length === 1 ? '' : 's'}
            {groups.data[0] ? ` — largest: ${groups.data[0].summary}` : ''}.
          </p>
        )}
      </Card>
    </div>
  )
}
