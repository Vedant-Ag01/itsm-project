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

export async function getIncidents(filters = {}, { signal } = {}) {
  if (USING_MOCKS) {
    return listMockIncidents(filters)
  }

  const params = new URLSearchParams()

  for (const [key, value] of Object.entries(filters)) {
    if (value !== undefined && value !== null && value !== '') {
      params.set(key, String(value))
    }
  }

  const query = params.toString()

  const rows = await apiRequest(`/incidents${query ? `?${query}` : ''}`, { signal })
  if (!Array.isArray(rows)) throw new Error('Unexpected incident list response.')
  return { total: rows.length, items: rows.map((row) => ({ ...row, service: row.serviceName, slaStatus: null })) }
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
