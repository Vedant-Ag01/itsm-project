import { apiRequest } from './client'
import { mockIncidents } from '../mocks/incidents'

export const USING_MOCKS =
  import.meta.env.VITE_USE_MOCKS !== 'false'

const supportedFilters = [
  'id',
  'status',
  'priority',
  'category',
  'slaStatus',
]

export async function getIncidents(filters = {}, { signal } = {}) {
  if (USING_MOCKS) {
    const items = mockIncidents.filter((incident) =>
      supportedFilters.every(
        (key) => !filters[key] || incident[key] === filters[key],
      ),
    )

    return {
      total: items.length,
      items: items.map((incident) => ({ ...incident })),
    }
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