import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { getIncidents } from '../../api/incidents'
import Card from '../../components/Card'
import DataTable from '../../components/DataTable'
import { PriorityBadge, SlaBadge, StatusBadge } from '../../components/Badges'

const STATUS_OPTIONS = [
  'NEW',
  'CLASSIFIED',
  'ASSIGNED',
  'ACKNOWLEDGED',
  'IN_PROGRESS',
  'RESOLVED',
  'VERIFIED',
  'CLOSED',
]

const PRIORITY_OPTIONS = ['P1', 'P2', 'P3', 'P4']

const CATEGORY_OPTIONS = [
  'HARDWARE',
  'SOFTWARE',
  'NETWORK',
  'DATABASE',
  'CLOUD',
  'SECURITY',
  'AUTHENTICATION',
  'APPLICATION',
  'STORAGE',
]

const SLA_OPTIONS = ['ON_TRACK', 'AT_RISK', 'BREACHED']

const INITIAL_FILTERS = {
  q: '',
  status: '',
  priority: '',
  category: '',
  service: '',
  technician: '',
  slaStatus: '',
  from: '',
  to: '',
}

const columns = [
  {
    key: 'id',
    label: 'ID',
    render: (row) => <Link className="incident-link" to={`/admin/incidents/${encodeURIComponent(row.id)}`}>{row.id}</Link>,
  },
  { key: 'title', label: 'Title' },
  {
    key: 'service',
    label: 'Service',
    render: (row) => row.serviceName || row.service || '—',
  },
  { key: 'category', label: 'Category' },
  {
    key: 'assignedTechnician',
    label: 'Technician',
    render: (row) => row.assignedTechnician || 'Unassigned',
  },
  {
    key: 'priority',
    label: 'Priority',
    render: (row) => <PriorityBadge value={row.priority} />,
  },
  {
    key: 'status',
    label: 'Status',
    render: (row) => <StatusBadge value={row.status} />,
  },
  {
    key: 'slaStatus',
    label: 'SLA',
    render: (row) => (row.slaStatus ? <SlaBadge value={row.slaStatus} /> : 'Not available'),
  },
]

export default function AdminIncidents() {
  const [filters, setFilters] = useState(INITIAL_FILTERS)
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    const controller = new AbortController()
    setLoading(true)
    setError('')

    getIncidents(filters, { signal: controller.signal })
      .then(({ items }) => {
        if (!controller.signal.aborted) setRows(items)
      })
      .catch((err) => {
        if (!controller.signal.aborted) setError(err.message)
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false)
      })

    return () => controller.abort()
  }, [filters])

  function updateFilter(key, value) {
    setFilters((prev) => ({ ...prev, [key]: value }))
  }

  function resetFilters() {
    setFilters(INITIAL_FILTERS)
  }

  return (
    <div className="workspace-stack">
      <Card
        title="Incident Search & Filtering"
        subtitle="Filter incidents by keyword, status, priority, category, service, technician, SLA status, or date range."
      >
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
            gap: '16px',
            marginBottom: '24px',
          }}
        >
          <label className="report-field">
            Keyword
            <input
              type="search"
              placeholder="ID, title, description…"
              value={filters.q}
              onChange={(e) => updateFilter('q', e.target.value)}
            />
          </label>

          <label className="report-field">
            Status
            <select value={filters.status} onChange={(e) => updateFilter('status', e.target.value)}>
              <option value="">All statuses</option>
              {STATUS_OPTIONS.map((s) => (
                <option key={s} value={s}>{s}</option>
              ))}
            </select>
          </label>

          <label className="report-field">
            Priority
            <select value={filters.priority} onChange={(e) => updateFilter('priority', e.target.value)}>
              <option value="">All priorities</option>
              {PRIORITY_OPTIONS.map((p) => (
                <option key={p} value={p}>{p}</option>
              ))}
            </select>
          </label>

          <label className="report-field">
            Category
            <select value={filters.category} onChange={(e) => updateFilter('category', e.target.value)}>
              <option value="">All categories</option>
              {CATEGORY_OPTIONS.map((c) => (
                <option key={c} value={c}>{c}</option>
              ))}
            </select>
          </label>

          <label className="report-field">
            Service
            <input
              type="text"
              placeholder="e.g. Printing, WiFi"
              value={filters.service}
              onChange={(e) => updateFilter('service', e.target.value)}
            />
          </label>

          <label className="report-field">
            Technician
            <input
              type="text"
              placeholder="e.g. Meera, Ravi"
              value={filters.technician}
              onChange={(e) => updateFilter('technician', e.target.value)}
            />
          </label>

          <label className="report-field">
            SLA status
            <select value={filters.slaStatus} onChange={(e) => updateFilter('slaStatus', e.target.value)}>
              <option value="">All SLA states</option>
              {SLA_OPTIONS.map((s) => (
                <option key={s} value={s}>{s}</option>
              ))}
            </select>
          </label>

          <label className="report-field">
            From date
            <input
              type="date"
              value={filters.from}
              onChange={(e) => updateFilter('from', e.target.value)}
            />
          </label>

          <label className="report-field">
            To date
            <input
              type="date"
              value={filters.to}
              onChange={(e) => updateFilter('to', e.target.value)}
            />
          </label>
        </div>

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <span style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
            {loading ? 'Searching…' : `Showing ${rows.length} matching incident(s)`}
          </span>
          <button type="button" className="secondary-button" onClick={resetFilters}>
            Reset filters
          </button>
        </div>

        {error && <p role="alert">{error}</p>}
        {!loading && (
          <DataTable
            columns={columns}
            rows={rows}
            caption="Filtered incidents"
            emptyMessage="No incidents match your current filter combination."
          />
        )}
      </Card>
    </div>
  )
}
