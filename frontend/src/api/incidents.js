import {
  createMockIncident,
  listMockServices,
} from '../mocks/incidentStore'
import { ApiError, apiRequest } from './client'
import {
  addMockNote,
  getMockAudit,
  getMockIncident,
  listMockIncidents,
  submitMockFeedback,
  verifyMockIncident,
} from '../mocks/incidentStore'

export const USING_MOCKS =
  import.meta.env.VITE_USE_MOCKS !== 'false'

function incidentPath(id) {
  return `/incidents/${encodeURIComponent(id)}`
}

function matchesSearchFilters(item, filters) {
  if (filters.q) {
    const q = String(filters.q).trim().toLowerCase()
    const haystack = `${item.id ?? ''} ${item.title ?? ''} ${item.description ?? ''}`.toLowerCase()
    if (!haystack.includes(q)) return false
  }
  if (filters.status && item.status !== filters.status) return false
  if (filters.priority && item.priority !== filters.priority) return false
  if (filters.category && item.category !== filters.category) return false

  if (filters.service) {
    const svc = String(item.serviceName || item.service || '').toLowerCase()
    if (!svc.includes(String(filters.service).trim().toLowerCase())) return false
  }

  const techFilter = filters.technician || filters.technicianId
  if (techFilter) {
    const tech = String(item.assignedTechnician || '').toLowerCase()
    if (!tech.includes(String(techFilter).trim().toLowerCase())) return false
  }

  if (filters.slaStatus && item.slaStatus !== filters.slaStatus) return false

  if (filters.from && item.createdAt && String(item.createdAt).slice(0, 10) < filters.from) {
    return false
  }
  if (filters.to && item.createdAt && String(item.createdAt).slice(0, 10) > filters.to) {
    return false
  }

  return true
}

export async function getIncidents(filters = {}, { signal } = {}) {
  if (USING_MOCKS) {
    const { items } = await listMockIncidents(filters)
    const filtered = items.filter((item) => matchesSearchFilters(item, filters))
    return { total: filtered.length, items: filtered }
  }

  const params = new URLSearchParams()
  const serverKeys = ['q', 'status', 'priority', 'category', 'technicianId', 'from', 'to']

  for (const key of serverKeys) {
    const value = filters[key]
    if (value !== undefined && value !== null && value !== '') {
      params.set(key, String(value))
    }
  }

  const query = params.toString()
  const rows = await apiRequest(`/incidents${query ? `?${query}` : ''}`, { signal })
  if (!Array.isArray(rows)) throw new Error('Unexpected incident list response.')

  const normalized = rows
    .map((row) => ({
      ...row,
      service: row.serviceName,
      slaStatus: row.slaStatus ?? null,
    }))
    .filter((item) => matchesSearchFilters(item, filters))

  return { total: normalized.length, items: normalized }
}

export async function getIncident(id, { signal } = {}) {
  if (USING_MOCKS) {
    return getMockIncident(id)
  }

  return normalizeIncident(await apiRequest(incidentPath(id), { signal }))
}

export async function getIncidentAudit(id, { signal } = {}) {
  if (USING_MOCKS) {
    return getMockAudit(id)
  }

  void signal
  throw new ApiError('Incident history is not available yet.', 501)
}

export async function addIncidentNote(id, text) {
  if (USING_MOCKS) {
    return addMockNote(id, text)
  }

  throw new ApiError('Adding notes is not available yet.', 501)
}

export async function verifyIncident(id) {
  if (USING_MOCKS) {
    return verifyMockIncident(id)
  }

  return normalizeIncident(await apiRequest(`${incidentPath(id)}/verify`, { method: 'POST' }))
}

export async function submitIncidentFeedback(id, rating, comment) {
  if (USING_MOCKS) {
    return submitMockFeedback(id, rating, comment)
  }

  throw new ApiError('Feedback is not available yet.', 501)
}

export async function getReportServices() {
  if (USING_MOCKS) {
    return listMockServices()
  }

  throw new Error(
    'The employee service-list endpoint has not been configured yet.',
  )
}

export async function createIncident(input) {
  if (USING_MOCKS) {
    return createMockIncident(input)
  }

  return apiRequest('/incidents', {
    method: 'POST',
    body: input,
  })
}

function normalizeIncident(row) {
  return {
    ...row,
    service: { name: row.serviceName },
    reporter: { name: row.reporterName },
    assignedTechnician: row.assignedTechnician ? { name: row.assignedTechnician } : null,
    impact: 'Not available', urgency: 'Not available', slaStatus: null,
    notes: [], feedback: null,
  }
}
