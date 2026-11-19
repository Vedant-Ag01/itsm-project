import { apiRequest } from './client'
import { getIncidents, USING_MOCKS } from './incidents'

export async function getNotifications({ signal } = {}) {
  if (USING_MOCKS) {
    const { items } = await getIncidents({}, { signal })
    const notifications = items
      .filter((i) => ['ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'VERIFIED'].includes(i.status))
      .map((i) => ({
        id: `notif-${i.id}`,
        recipient: 'asha@bits.edu',
        subject: `Incident ${i.id} is ${i.status}`,
        message: `${i.title} (${i.service}) moved to ${i.status}.`,
        createdAt: i.updatedAt || i.createdAt || new Date().toISOString(),
        read: false,
      }))
    return { unread: notifications.length, notifications }
  }
  return apiRequest('/notifications', { signal })
}

export async function markNotificationRead(id) {
  if (USING_MOCKS) return { unread: 0 }
  return apiRequest(`/notifications/${encodeURIComponent(id)}/read`, { method: 'POST' })
}

export async function markAllNotificationsRead() {
  if (USING_MOCKS) return { unread: 0 }
  return apiRequest('/notifications/read-all', { method: 'POST' })
}

export async function getAdminReports({ signal } = {}) {
  if (USING_MOCKS) {
    const { items } = await getIncidents({}, { signal })
    const openIncidents = items.filter((i) => !['RESOLVED', 'VERIFIED', 'CLOSED'].includes(i.status)).length
    const resolvedIncidents = items.length - openIncidents
    const p1Count = items.filter((i) => i.priority === 'P1').length
    const slaViolations = items.filter((i) => i.slaStatus === 'BREACHED').length

    const catMap = {}
    items.forEach((i) => {
      const cat = i.category || 'UNCATEGORIZED'
      catMap[cat] = (catMap[cat] || 0) + 1
    })

    return {
      overview: {
        totalIncidents: items.length,
        openIncidents,
        criticalP1Incidents: p1Count,
        resolvedIncidents,
        slaViolations,
      },
      categories: Object.entries(catMap).map(([category, count]) => ({ category, count })),
      resolutionTime: { averageResolutionMinutes: 42 },
      services: [
        {
          serviceName: 'Campus WiFi',
          criticality: 'HIGH',
          totalIncidents: items.length,
          openIncidents,
          resolvedIncidents,
          p1Incidents: p1Count,
          slaBreaches: slaViolations,
          averageResolutionMinutes: 42,
          mostCommonCategory: 'NETWORK',
        },
      ],
      workload: [
        { technicianId: 'Ravi', activeIncidentCount: 2 },
        { technicianId: 'Meera', activeIncidentCount: 1 },
      ],
    }
  }

  const [overview, categories, resolutionTime, services, workload] = await Promise.all([
    apiRequest('/admin/reports/overview', { signal }),
    apiRequest('/admin/reports/categories', { signal }),
    apiRequest('/admin/reports/resolution-time', { signal }),
    apiRequest('/admin/reports/services', { signal }),
    apiRequest('/admin/reports/workload', { signal }),
  ])

  return { overview, categories, resolutionTime, services, workload }
}

export async function getAdminAuditLogs({ signal } = {}) {
  if (USING_MOCKS) {
    return [
      {
        incidentId: 'INC1001',
        eventType: 'CREATED',
        actor: 'E1',
        previousStatus: null,
        newStatus: 'NEW',
        description: 'Incident submitted via portal',
        timestamp: new Date().toISOString(),
        formattedLog: '[CREATED] INC1001 by E1 -> NEW',
      },
    ]
  }
  return apiRequest('/admin/audit', { signal })
}
