import { apiRequest } from './client'

// All admin calls go straight to the live backend (see docs/api-reference.md).

export const getOverview = (o) => apiRequest('/admin/reports/overview', o)
export const getResolutionTime = (o) => apiRequest('/admin/reports/resolution-time', o)
export const runMonitor = () => apiRequest('/admin/monitor/run', { method: 'POST' })

export const getUsers = (o) => apiRequest('/admin/users', o)
export const createUser = (body) => apiRequest('/admin/users', { method: 'POST', body })
export const updateUser = (id, body) =>
  apiRequest(`/admin/users/${encodeURIComponent(id)}`, { method: 'PUT', body })

export const getTechnicians = (o) => apiRequest('/admin/technicians', o)
export const setTechnicianAvailability = (id, available) =>
  apiRequest(`/admin/technicians/${encodeURIComponent(id)}/availability`, {
    method: 'PUT',
    body: { available },
  })

export const getServices = (o) => apiRequest('/admin/services', o)
export const createService = (body) => apiRequest('/admin/services', { method: 'POST', body })
export const updateService = (id, body) =>
  apiRequest(`/admin/services/${id}`, { method: 'PUT', body })

export const getSlaPolicies = (o) => apiRequest('/sla-policies', o)
export const updateSlaPolicy = (priority, body) =>
  apiRequest(`/admin/sla-policies/${priority}`, { method: 'PUT', body })

export const getAssignmentStrategy = (o) => apiRequest('/admin/assignment-strategy', o)
export const setAssignmentStrategy = (name) =>
  apiRequest('/admin/assignment-strategy', { method: 'PUT', body: { name } })
export const getWorkloadLimit = (o) => apiRequest('/admin/workload-limit', o)
export const setWorkloadLimit = (maxActiveIncidents) =>
  apiRequest('/admin/workload-limit', { method: 'PUT', body: { maxActiveIncidents } })

export const getSlaRisks = (minLevel, o) =>
  apiRequest(`/admin/sla-risks${minLevel ? `?minLevel=${minLevel}` : ''}`, o)
export const getCorrelations = (o) => apiRequest('/admin/correlations', o)

// ---- one incident, seen by an admin ----
export const assignIncident = (id, technicianId) =>
  apiRequest(`/incidents/${encodeURIComponent(id)}/assign`, { method: 'POST', body: { technicianId } })
export const autoAssignIncident = (id) =>
  apiRequest(`/incidents/${encodeURIComponent(id)}/auto-assign`, { method: 'POST' })
export const closeIncident = (id, note) =>
  apiRequest(`/incidents/${encodeURIComponent(id)}/close`, { method: 'POST', body: { note } })
