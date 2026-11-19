import { useEffect, useState } from 'react'
import { getAdminReports } from '../../api/c7'
import Card from '../../components/Card'
import DataTable from '../../components/DataTable'

export default function Reports() {
  const [data, setData] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    const controller = new AbortController()
    getAdminReports({ signal: controller.signal })
      .then((res) => { if (!controller.signal.aborted) setData(res) })
      .catch((err) => { if (!controller.signal.aborted) setError(err.message) })
    return () => controller.abort()
  }, [])

  if (error) return <Card title="Reports"><p role="alert">{error}</p></Card>
  if (!data) return <Card title="Reports"><p>Loading analytics…</p></Card>

  const { overview, categories, resolutionTime, services, workload } = data

  return (
    <div className="workspace-stack">
      <Card title="Operational Overview" subtitle="Live incident volume, SLA compliance, and resolution performance.">
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))', gap: '1rem' }}>
          <div className="panel"><strong>Total Incidents</strong><div style={{ fontSize: '1.6rem', fontWeight: 700 }}>{overview.totalIncidents}</div></div>
          <div className="panel"><strong>Open Incidents</strong><div style={{ fontSize: '1.6rem', fontWeight: 700 }}>{overview.openIncidents}</div></div>
          <div className="panel"><strong>Critical (P1)</strong><div style={{ fontSize: '1.6rem', fontWeight: 700 }}>{overview.criticalP1Incidents ?? 0}</div></div>
          <div className="panel"><strong>Resolved</strong><div style={{ fontSize: '1.6rem', fontWeight: 700 }}>{overview.resolvedIncidents}</div></div>
          <div className="panel"><strong>SLA Violations</strong><div style={{ fontSize: '1.6rem', fontWeight: 700 }}>{overview.slaViolations}</div></div>
          <div className="panel"><strong>Avg Resolution</strong><div style={{ fontSize: '1.6rem', fontWeight: 700 }}>{Math.round(resolutionTime?.averageResolutionMinutes ?? 0)} min</div></div>
        </div>
      </Card>

      <Card title="Service Health Breakdown" subtitle="Incident volume, P1s, and SLA breaches by IT service (busiest first).">
        <DataTable
          rowKey="serviceName"
          caption="Service breakdown"
          rows={services || []}
          columns={[
            { key: 'serviceName', label: 'Service' },
            { key: 'criticality', label: 'Criticality' },
            { key: 'totalIncidents', label: 'Total' },
            { key: 'openIncidents', label: 'Open' },
            { key: 'p1Incidents', label: 'P1' },
            { key: 'slaBreaches', label: 'SLA Breaches' },
            { key: 'mostCommonCategory', label: 'Top Category' },
          ]}
        />
      </Card>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '1rem' }}>
        <Card title="Incidents by Category">
          <DataTable
            rowKey="category"
            caption="Category breakdown"
            rows={categories || []}
            columns={[
              { key: 'category', label: 'Category' },
              { key: 'count', label: 'Incidents' },
            ]}
          />
        </Card>

        <Card title="Technician Active Workload">
          <DataTable
            rowKey="technicianId"
            caption="Technician workload"
            rows={workload || []}
            columns={[
              { key: 'technicianId', label: 'Technician' },
              { key: 'activeIncidentCount', label: 'Active Incidents' },
            ]}
          />
        </Card>
      </div>
    </div>
  )
}
