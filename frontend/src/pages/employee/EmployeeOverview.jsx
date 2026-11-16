import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import Card from '../../components/Card'
import DataTable from '../../components/DataTable'
import {
  PriorityBadge,
  SlaBadge,
  StatusBadge,
} from '../../components/Badges'
import { getIncidents, USING_MOCKS } from '../../api/incidents'

const completedStatuses = new Set([
  'RESOLVED',
  'VERIFIED',
  'CLOSED',
])

const statusOptions = [
  ['NEW', 'New'],
  ['CLASSIFIED', 'Classified'],
  ['ASSIGNED', 'Assigned'],
  ['ACKNOWLEDGED', 'Acknowledged'],
  ['IN_PROGRESS', 'In progress'],
  ['RESOLVED', 'Resolved'],
  ['VERIFIED', 'Verified'],
  ['CLOSED', 'Closed'],
]

const columns = [
  {
    key: 'incident',
    label: 'Incident',
    render: (incident) => (
      <div className="incident-cell">
        <span className="incident-id">{incident.id}</span>
        <Link
          className="incident-link"
          to={`/employee/incidents/${incident.id}`}
        >
  {incident.title}
</Link>
      </div>
    ),
  },
  { key: 'service', label: 'Service' },
  {
    key: 'priority',
    label: 'Priority',
    render: (incident) => (
      <PriorityBadge value={incident.priority} />
    ),
  },
  {
    key: 'status',
    label: 'Status',
    render: (incident) => (
      <StatusBadge value={incident.status} />
    ),
  },
  {
    key: 'slaStatus',
    label: 'SLA',
    render: (incident) => (
      <span title="SLA status">{incident.slaStatus ? <SlaBadge value={incident.slaStatus} /> : "Not available"}</span>
    ),
  },
]


function RecentReports({ items, isReady }) {
  const recent = [...items]
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
    .slice(0, 4)

  return (
    <Card title="Recent reports">
      {!isReady ? (
        <p className="subtitle">Incident data is not available yet.</p>
      ) : recent.length === 0 ? (
        <p className="subtitle">You haven’t reported any incidents yet.</p>
      ) : (
        <ol className="audit-timeline">
          {recent.map((incident) => (
            <li key={incident.id}>
              <Link
                className="incident-link"
                to={`/employee/incidents/${encodeURIComponent(incident.id)}`}
              >
                {incident.title}
              </Link>
              <span>{incident.id}</span>
              <time dateTime={incident.createdAt}>
                {new Date(incident.createdAt).toLocaleString('en-IN', {
                  dateStyle: 'medium',
                  timeStyle: 'short',
                })}
              </time>
            </li>
          ))}
        </ol>
      )}
    </Card>
  )
}

function ResolutionTrend({ items, isReady }) {
  const today = new Date()
  const days = Array.from({ length: 7 }, (_, index) => {
    const date = new Date(today)
    date.setDate(today.getDate() - (6 - index))
    date.setHours(0, 0, 0, 0)

    const nextDay = new Date(date)
    nextDay.setDate(date.getDate() + 1)

    return {
      key: date.getTime(),
      label: date.toLocaleDateString('en-IN', { weekday: 'short' }),
      count: items.filter(({ resolvedAt }) => {
        if (!resolvedAt) return false
        const resolved = new Date(resolvedAt)
        return resolved >= date && resolved < nextDay
      }).length,
    }
  })
  const max = Math.max(1, ...days.map(({ count }) => count))

  return (
    <Card title="Resolutions over the last 7 days">
      {!isReady ? (
        <p className="subtitle">Incident data is not available yet.</p>
      ) : (
        <ul className="report-trend">
          {days.map(({ key, label, count }) => (
            <li key={key}>
              <div className="report-trend-column">
                {count > 0 && (
                  <span
                    className="report-trend-bar"
                    style={{ height: `${(count / max) * 75}%` }}
                    aria-hidden="true"
                  />
                )}
                <span>{count}</span>
              </div>
              <span>{label}</span>
            </li>
          ))}
        </ul>
      )}
    </Card>
  )
}

export default function EmployeeOverview() {
  const [loadState, setLoadState] = useState({
    phase: 'loading',
    items: [],
    error: '',
  })
  const [attempt, setAttempt] = useState(0)
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState('')

  useEffect(() => {
    const controller = new AbortController()
    let active = true

    getIncidents({}, { signal: controller.signal })
      .then((result) => {
        if (active) {
          setLoadState({
            phase: 'ready',
            items: result.items,
            error: '',
          })
        }
      })
      .catch((error) => {
        if (active && error.name !== 'AbortError') {
          setLoadState({
            phase: 'error',
            items: [],
            error: error.message,
          })
        }
      })

    return () => {
      active = false
      controller.abort()
    }
  }, [attempt])

  const { phase, items, error } = loadState
  const isReady = phase === 'ready'
  const query = search.trim().toLowerCase()

  const filteredItems = items.filter((incident) => {
    const searchableText =
      `${incident.id} ${incident.title} ${incident.service}`.toLowerCase()

    return (
      (!status || incident.status === status) &&
      (!query || searchableText.includes(query))
    )
  })

  const metrics = [
    {
      label: 'Open incidents',
      count: items.filter(
        (incident) => !completedStatuses.has(incident.status),
      ).length,
    },
    {
      label: 'In progress',
      count: items.filter(
        (incident) => incident.status === 'IN_PROGRESS',
      ).length,
    },
    {
      label: 'Awaiting verification',
      count: items.filter(
        (incident) => incident.status === 'RESOLVED',
      ).length,
    },
    {
      label: 'Resolved this month',
      count: items.filter(({ resolvedAt }) => {
        if (!resolvedAt) return false
        const resolved = new Date(resolvedAt)
        const now = new Date()
        return resolved.getFullYear() === now.getFullYear() && resolved.getMonth() === now.getMonth()
      }).length,
    },
  ]

  const awaitingVerification = items.filter(
    (incident) => incident.status === 'RESOLVED',
  )

  function retry() {
    setLoadState({ phase: 'loading', items: [], error: '' })
    setAttempt((current) => current + 1)
  }

  function clearFilters() {
    setSearch('')
    setStatus('')
  }

  return (
    <>
      <div className="page-heading">
        <div>
          <p className="eyebrow">Overview</p>
          <h1>Your workspace</h1>
          <p className="subtitle">Report issues. Track progress.</p>
        </div>

        <Link className="primary-button" to="/employee/report">
          + Report incident
        </Link>
      </div>

      <div className="data-notice">
        {USING_MOCKS
          ? 'Demo data · These incidents are examples, not live records.'
          : 'Connected to the incident API.'}
      </div>

      <div className="stats-grid">
        {metrics.map((metric) => (
          <section className="stat-card" key={metric.label}>
            <h2>{metric.label}</h2>
            <div className="stat-value">
              {isReady ? metric.count : '—'}
            </div>
            <p>
              {isReady
                ? 'Across your loaded incidents'
                : 'Data not available yet'}
            </p>
          </section>
        ))}
      </div>

           <div className="overview-primary">
        <Card
          title="My incidents"
          action={
            <span className="preview-badge">
              {USING_MOCKS ? 'Demo records' : 'Your records'}
            </span>
          }
        >
          {phase === 'loading' && (
            <div className="empty-state" role="status">
              <p>Loading incidents…</p>
            </div>
          )}

          {phase === 'error' && (
            <div className="empty-state">
              <p role="alert">{error}</p>
              <button
                className="secondary-button"
                type="button"
                onClick={retry}
              >
                Try again
              </button>
            </div>
          )}

          {isReady && (
            <>
              <div className="table-toolbar">
                <label className="filter-field search-field">
                  <span>Search incidents</span>
                  <input
                    type="search"
                    placeholder="ID, title or service"
                    value={search}
                    onChange={(event) => setSearch(event.target.value)}
                  />
                </label>

                <label className="filter-field">
                  <span>Status</span>
                  <select
                    value={status}
                    onChange={(event) => setStatus(event.target.value)}
                  >
                    <option value="">All statuses</option>
                    {statusOptions.map(([value, label]) => (
                      <option key={value} value={value}>
                        {label}
                      </option>
                    ))}
                  </select>
                </label>

                <button
                  className="secondary-button"
                  type="button"
                  onClick={clearFilters}
                  disabled={!search && !status}
                >
                  Clear
                </button>
              </div>

              <p className="results-count" role="status">
                Showing {filteredItems.length} of {items.length} incidents
              </p>

              <DataTable
                columns={columns}
                rows={filteredItems}
                caption="Your incidents with service, priority, status and SLA"
                emptyMessage={
                  items.length === 0
                    ? 'You have no incidents yet.'
                    : 'No incidents match your filters.'
                }
              />
            </>
          )}
        </Card>

        <RecentReports items={items} isReady={isReady} />
      </div>

      <div className="dashboard-grid overview-bottom">
        <ResolutionTrend items={items} isReady={isReady} />

        <Card title="Needs your attention">
          {!isReady ? (
            <p className="subtitle">Incident data is not available yet.</p>
          ) : awaitingVerification.length === 0 ? (
            <p className="subtitle">
              No resolutions are awaiting your verification.
            </p>
          ) : (
            <ul className="attention-list">
              {awaitingVerification.map((incident) => (
                <li key={incident.id}>
                  <div>
                    <span className="incident-id">{incident.id}</span>
                    <h3>{incident.title}</h3>
                    <p>Check that the issue is fixed before verifying.</p>
                  </div>

                  <Link
                    className="incident-link"
                    to={`/employee/incidents/${encodeURIComponent(incident.id)}`}
                  >
                    Review resolution
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>
    </>
  )
}