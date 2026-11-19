import { useEffect, useState } from 'react'
import { getAdminReports } from '../../api/c7'
import Card from '../../components/Card'
import DataTable from '../../components/DataTable'

function HorizontalBarChart({ items, labelKey, valueKey, unit = '', ariaLabel }) {
  const max = Math.max(1, ...items.map((item) => Number(item[valueKey] || 0)))

  if (items.length === 0) {
    return (
      <div className="empty-state">
        <h3>No chart data available</h3>
        <p>Metrics will appear once incidents are recorded.</p>
      </div>
    )
  }

  return (
    <div role="img" aria-label={ariaLabel} style={{ display: 'grid', gap: '16px' }}>
      {items.map((item) => {
        const value = Number(item[valueKey] || 0)
        const percentage = Math.round((value / max) * 100)

        return (
          <div key={item[labelKey]}>
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'baseline',
                fontSize: '14px',
                marginBottom: '4px',
              }}
            >
              <span>{item[labelKey]}</span>
              <strong style={{ fontVariantNumeric: 'tabular-nums' }}>
                {value}
                {unit}
              </strong>
            </div>

            <div
              style={{
                height: '8px',
                borderRadius: '4px',
                background: 'var(--border)',
                overflow: 'hidden',
              }}
            >
              <div
                style={{
                  width: `${percentage}%`,
                  height: '100%',
                  background: 'var(--action)',
                  borderRadius: '4px',
                  transition: 'width 180ms ease',
                }}
              />
            </div>
          </div>
        )
      })}
    </div>
  )
}

export default function Reports() {
  const [data, setData] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    const controller = new AbortController()

    getAdminReports({ signal: controller.signal })
      .then((res) => {
        if (!controller.signal.aborted) setData(res)
      })
      .catch((err) => {
        if (!controller.signal.aborted) setError(err.message)
      })

    return () => controller.abort()
  }, [])

  if (error) {
    return (
      <Card title="Operational Reports">
        <p role="alert">Failed to load reports: {error}</p>
      </Card>
    )
  }

  if (!data) {
    return (
      <Card title="Operational Reports">
        <p>Loading operational analytics…</p>
      </Card>
    )
  }

  const { overview, resolutionTime, services = [], workload = [], categories = [] } = data

  const summaryCards = [
    { label: 'Total Incidents', value: overview.totalIncidents },
    { label: 'Open Incidents', value: overview.openIncidents },
    { label: 'Critical (P1)', value: overview.criticalP1Incidents ?? 0 },
    { label: 'Resolved', value: overview.resolvedIncidents },
    { label: 'SLA Violations', value: overview.slaViolations },
    {
      label: 'Avg Resolution',
      value: `${Math.round(resolutionTime?.averageResolutionMinutes ?? 0)}m`,
    },
  ]

  const topService = services[0]
  const busiestTech = [...workload].sort(
    (a, b) => b.activeIncidentCount - a.activeIncidentCount,
  )[0]

  return (
    <div className="workspace-stack">
      <Card
        title="Operational Overview"
        subtitle="System-wide incident volume, SLA compliance, and mean resolution performance."
      >
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))',
            gap: '16px',
          }}
        >
          {summaryCards.map((stat) => (
            <div
              key={stat.label}
              style={{
                background: 'var(--surface-muted)',
                border: '1px solid var(--border)',
                borderRadius: '12px',
                padding: '16px',
              }}
            >
              <div style={{ fontSize: '13px', color: 'var(--text-secondary)', marginBottom: '8px' }}>
                {stat.label}
              </div>
              <div
                style={{
                  fontSize: '32px',
                  fontWeight: 500,
                  lineHeight: 1.2,
                  fontVariantNumeric: 'tabular-nums',
                }}
              >
                {stat.value}
              </div>
            </div>
          ))}
        </div>
      </Card>

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))',
          gap: '24px',
        }}
      >
        <Card
          title="Service Incident Analysis"
          subtitle={
            topService
              ? `Most affected service: ${topService.serviceName} (${topService.totalIncidents} total).`
              : 'Incident distribution across IT services.'
          }
        >
          <HorizontalBarChart
            items={services}
            labelKey="serviceName"
            valueKey="totalIncidents"
            unit=" incidents"
            ariaLabel="Bar chart of total incidents per IT service"
          />
        </Card>

        <Card
          title="Technician Active Workload"
          subtitle={
            busiestTech
              ? `Highest active queue: ${busiestTech.technicianId} (${busiestTech.activeIncidentCount} open).`
              : 'Current open incident load per technician.'
          }
        >
          <HorizontalBarChart
            items={workload}
            labelKey="technicianId"
            valueKey="activeIncidentCount"
            unit=" active"
            ariaLabel="Bar chart of active incidents per technician"
          />
        </Card>
      </div>

      <Card
        title="Service Health Breakdown"
        subtitle="Detailed breakdown of open incidents, P1 severity, and SLA breaches by service."
      >
        <DataTable
          rowKey="serviceName"
          caption="Service health breakdown"
          rows={services}
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

      <Card title="Incidents by Category">
        <DataTable
          rowKey="category"
          caption="Category breakdown"
          rows={categories}
          columns={[
            { key: 'category', label: 'Category' },
            { key: 'count', label: 'Incidents' },
          ]}
        />
      </Card>
    </div>
  )
}
