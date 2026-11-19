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

  const incident = normalizeIncident(await apiRequest(incidentPath(id), { signal }))
  try {
    const sla = await apiRequest(`${incidentPath(id)}/sla`, { signal })
    return {
      ...incident,
      responseDeadline: sla.responseDeadline,
      resolutionDeadline: sla.resolutionDeadline,
      slaStatus: incident.slaStatus ?? sla.status ?? null,
    }
  } catch {
    return incident
  }
}

export async function getIncidentAudit(id, { signal } = {}) {
  if (USING_MOCKS) {
    return getMockAudit(id)
  }

  const rows = await apiRequest(`${incidentPath(id)}/audit`, { signal })
  return rows
    .map((row) => ({ ...row, user: row.actor }))
    .sort((a, b) => String(a.timestamp).localeCompare(String(b.timestamp)))
}

export async function addIncidentNote(id, text) {
  if (USING_MOCKS) {
    return addMockNote(id, text)
  }

  const notes = await apiRequest(`${incidentPath(id)}/notes`, { method: 'POST', body: { text } })
  return { notes: notes.map((entry) => ({ ...entry, author: entry.authorName })) }
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

  return normalizeIncident(await apiRequest(`${incidentPath(id)}/feedback`, {
    method: 'POST', body: { rating, comment },
  }))
}

export async function getReportServices() {
  if (USING_MOCKS) {
    return listMockServices()
  }

  // the catalogue, active services only (names are what the report form sends back)
  return apiRequest('/services')
}

// What category would the system pick for this text? (null when it cannot tell)
export async function suggestCategory(title, description, { signal } = {}) {
  if (USING_MOCKS) return null
  const result = await apiRequest('/classification/suggest', {
    method: 'POST', body: { title, description }, signal,
  })
  return result?.category ? result : null
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
    slaStatus: row.slaStatus ?? null,
    feedback: row.feedbackRating != null
      ? { rating: row.feedbackRating, comment: row.feedbackComment }
      : null,
  }
}

export async function getIncidentNotes(id, { signal } = {}) {
  if (USING_MOCKS) return (await getMockIncident(id)).notes
  const notes = await apiRequest(`${incidentPath(id)}/notes`, { signal })
  return notes.map((entry) => ({ ...entry, author: entry.authorName }))
}

export async function technicianAction(id, action, note) {
  if (USING_MOCKS) throw new ApiError('Technician actions require live mode.', 501)
  if (!['acknowledge', 'start-work', 'resolve'].includes(action)) throw new Error('Unknown action.')
  return normalizeIncident(await apiRequest(`${incidentPath(id)}/${action}`, {
    method: 'POST', ...(action === 'resolve' ? { body: { note } } : {}),
  }))
}

// Technician asks the reporter a question (status does not change)
export async function requestInformation(id, message) {
  if (USING_MOCKS) throw new ApiError('Information requests require live mode.', 501)
  return normalizeIncident(await apiRequest(`${incidentPath(id)}/request-info`, {
    method: 'POST', body: { message },
  }))
}

// Reporter answers the question
export async function provideInformation(id, message) {
  if (USING_MOCKS) throw new ApiError('Information replies require live mode.', 501)
  return normalizeIncident(await apiRequest(`${incidentPath(id)}/provide-info`, {
    method: 'POST', body: { message },
  }))
}
