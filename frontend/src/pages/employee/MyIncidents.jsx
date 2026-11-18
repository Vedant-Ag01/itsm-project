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

const statuses = [
  'NEW',
  'CLASSIFIED',
  'ASSIGNED',
  'ACKNOWLEDGED',
  'IN_PROGRESS',
  'RESOLVED',
  'VERIFIED',
  'CLOSED',
]

const makeColumns = (role) => [
  {
    key: 'incident',
    label: 'Incident',
    render: (incident) => (
      <div className="incident-cell">
        <span className="incident-id">{incident.id}</span>
        <Link
          className="incident-link"
          to={`/${role}/incidents/${encodeURIComponent(incident.id)}`}
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
    render: (incident) => <PriorityBadge value={incident.priority} />,
  },
  {
    key: 'status',
    label: 'Status',
    render: (incident) => <StatusBadge value={incident.status} />,
  },
  {
    key: 'slaStatus',
    label: 'SLA',
    render: (incident) => <span title="SLA status">{incident.slaStatus ? <SlaBadge value={incident.slaStatus} /> : "Not available"}</span>,
  },
]

export default function MyIncidents({ role = 'employee' }) {
  const technician = role === 'technician'
  const columns = makeColumns(role)
  const [state, setState] = useState({
    phase: 'loading',
    items: [],
    error: '',
  })
  const [attempt, setAttempt] = useState(0)
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState('')
  const [priority, setPriority] = useState('')

  useEffect(() => {
    const controller = new AbortController()
    let active = true

    getIncidents({}, { signal: controller.signal })
      .then((result) => {
        if (active) {
          setState({ phase: 'ready', items: result.items, error: '' })
        }
      })
      .catch((error) => {
        if (active && error.name !== 'AbortError') {
          setState({ phase: 'error', items: [], error: error.message })
        }
      })

    return () => {
      active = false
      controller.abort()
    }
  }, [attempt])

  const query = search.trim().toLowerCase()

  const visible = state.items.filter((incident) => {
    const text =
      `${incident.id} ${incident.title} ${incident.service}`.toLowerCase()

    return (
      (!query || text.includes(query)) &&
      (!status || incident.status === status) &&
      (!priority || incident.priority === priority)
    )
  })

  function retry() {
    setState({ phase: 'loading', items: [], error: '' })
    setAttempt((value) => value + 1)
  }

  return (
    <>
      <div className="page-heading">
        <div>
          <p className="eyebrow">{technician ? 'Technician workspace' : 'Employee workspace'}</p>
          <h1>{technician ? 'Assigned incidents' : 'My incidents'}</h1>
          <p className="subtitle">Open an incident to view updates and respond.</p>
        </div>
        {!technician && <Link className="primary-button" to="/employee/report">
          + Report incident
        </Link>}
      </div>

      {USING_MOCKS && (
        <div className="data-notice">
          Demo data · Changes reset when the page is refreshed.
        </div>
      )}

      <Card title={technician ? "Your assigned incidents" : "Your incidents"} action={<button className="secondary-button" onClick={retry}>Refresh</button>}>
        {state.phase === 'loading' && (
          <div className="empty-state" role="status">Loading incidents…</div>
        )}

        {state.phase === 'error' && (
          <div className="empty-state">
            <p role="alert">{state.error}</p>
            <button className="secondary-button" onClick={retry}>
              Try again
            </button>
          </div>
        )}

        {state.phase === 'ready' && (
          <>
            <div className="table-toolbar">
              <label className="filter-field search-field">
                <span>Search</span>
                <input
                  type="search"
                  value={search}
                  placeholder="ID, title or service"
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
                  {statuses.map((value) => (
                    <option key={value} value={value}>
                      {value.replaceAll('_', ' ')}
                    </option>
                  ))}
                </select>
              </label>

              <label className="filter-field">
                <span>Priority</span>
                <select
                  value={priority}
                  onChange={(event) => setPriority(event.target.value)}
                >
                  <option value="">All priorities</option>
                  <option value="P1">P1 · Critical</option>
                  <option value="P2">P2 · High</option>
                  <option value="P3">P3 · Medium</option>
                  <option value="P4">P4 · Low</option>
                </select>
              </label>

              <button
                className="secondary-button"
                disabled={!search && !status && !priority}
                onClick={() => {
                  setSearch('')
                  setStatus('')
                  setPriority('')
                }}
              >
                Clear
              </button>
            </div>

            <p className="results-count" role="status">
              Showing {visible.length} of {state.items.length} incidents
            </p>

            <DataTable
              columns={columns}
              rows={visible}
              caption={technician ? 'Your assigned incidents' : 'Your reported incidents'}
              emptyMessage={
                state.items.length
                  ? 'No incidents match your filters.'
                  : technician ? 'No incidents are assigned to you yet.' : 'You have not reported any incidents yet.'
              }
            />
          </>
        )}
      </Card>
    </>
  )
}
