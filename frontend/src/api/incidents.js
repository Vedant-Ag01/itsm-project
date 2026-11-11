import {
  createMockIncident,
  listMockServices,
} from '../mocks/incidentStore'
import { apiRequest } from './client'
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

  return apiRequest(
    `/incidents${query ? `?${query}` : ''}`,
    { signal },
  )
}

export async function getIncident(id, { signal } = {}) {
  if (USING_MOCKS) {
    return getMockIncident(id)
  }

  return apiRequest(incidentPath(id), { signal })
}

export async function getIncidentAudit(id, { signal } = {}) {
  if (USING_MOCKS) {
    return getMockAudit(id)
  }

  return apiRequest(`${incidentPath(id)}/audit`, { signal })
}

export async function addIncidentNote(id, text) {
  if (USING_MOCKS) {
    return addMockNote(id, text)
  }

  return apiRequest(`${incidentPath(id)}/notes`, {
    method: 'POST',
    body: { text },
  })
}

export async function verifyIncident(id) {
  if (USING_MOCKS) {
    return verifyMockIncident(id)
  }

  return apiRequest(`${incidentPath(id)}/verify`, {
    method: 'POST',
  })
}

export async function submitIncidentFeedback(id, rating, comment) {
  if (USING_MOCKS) {
    return submitMockFeedback(id, rating, comment)
  }

  return apiRequest(`${incidentPath(id)}/feedback`, {
    method: 'POST',
    body: { rating, comment },
  })
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