import { useEffect, useState } from 'react'
import { getIncidents } from '../../api/incidents'
import Card from '../../components/Card'
import DataTable from '../../components/DataTable'
import { PriorityBadge, SlaBadge, StatusBadge } from '../../components/Badges'

const columns = [
  { key: 'id', label: 'ID' },
  { key: 'title', label: 'Title' },
  { key: 'service', label: 'Service' },
  { key: 'category', label: 'Category' },
  { key: 'priority', label: 'Priority', render: (row) => <PriorityBadge value={row.priority} /> },
  { key: 'status', label: 'Status', render: (row) => <StatusBadge value={row.status} /> },
  { key: 'slaStatus', label: 'SLA', render: (row) => (row.slaStatus ? <SlaBadge value={row.slaStatus} /> : '—') },
]

export default function AdminIncidents() {
  const [filters, setFilters] = useState({ q: '', status: '', priority: '', category: '', slaStatus: '', from: '', to: '' })
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    const controller = new AbortController()
    setLoading(true)
    setError('')
    getIncidents(filters, { signal: controller.signal })
      .then(({ items }) => { if (!controller.signal.aborted) setRows(items) })
      .catch((err) => { if (!controller.signal.aborted) setError(err.message) })
      .finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [filters])

  function updateFilter(key, value) {
    setFilters((prev) => ({ ...prev, [key]: value }))
  }

  function resetFilters() {
    setFilters({ q: '', status: '', priority: '', category: '', slaStatus: '', from: '', to: '' })
  }

  return (
    <div className="workspace-stack">
      <Card title="Incident Search & Filtering" subtitle="Search across all incidents by keyword, status, category, priority, SLA, or date range.">
        <div className="filter-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(170px, 1fr))', gap: '0.75rem', marginBottom: '1rem' }}>
          <input type="search" placeholder="Keyword (title, description, ID)…" value={filters.q} onChange={(e) => updateFilter('q', e.target.value)} />
          <select value={filters.status} onChange={(e) => updateFilter('status', e.target.value)}>
            <option value="">All statuses</option>
            {['NEW', 'CLASSIFIED', 'ASSIGNED', 'ACKNOWLEDGED', 'IN_PROGRESS', 'RESOLVED', 'VERIFIED', 'CLOSED'].map((s) => (
              <option key={s} value={s}>{s}</option>
            ))}
          </select>
          <select value={filters.priority} onChange={(e) => updateFilter('priority', e.target.value)}>
            <option value="">All priorities</option>
            {['P1', 'P2', 'P3', 'P4'].map((p) => (
              <option key={p} value={p}>{p}</option>
            ))}
          </select>
          <select value={filters.category} onChange={(e) => updateFilter('category', e.target.value)}>
            <option value="">All categories</option>
            {['HARDWARE', 'SOFTWARE', 'NETWORK', 'DATABASE', 'CLOUD', 'SECURITY', 'AUTHENTICATION', 'APPLICATION', 'STORAGE'].map((c) => (
              <option key={c} value={c}>{c}</option>
            ))}
          </select>
          <select value={filters.slaStatus} onChange={(e) => updateFilter('slaStatus', e.target.value)}>
            <option value="">All SLA states</option>
            {['ON_TRACK', 'AT_RISK', 'BREACHED'].map((s) => (
              <option key={s} value={s}>{s}</option>
            ))}
          </select>
          <input type="date" aria-label="From date" value={filters.from} onChange={(e) => updateFilter('from', e.target.value)} />
          <input type="date" aria-label="To date" value={filters.to} onChange={(e) => updateFilter('to', e.target.value)} />
          <button type="button" className="secondary-button" onClick={resetFilters}>Clear filters</button>
        </div>

        {error && <p role="alert">{error}</p>}
        {loading ? <p>Searching incidents…</p> : <DataTable columns={columns} rows={rows} caption="Filtered incidents" emptyMessage="No incidents match your search filters." />}
      </Card>
    </div>
  )
}
