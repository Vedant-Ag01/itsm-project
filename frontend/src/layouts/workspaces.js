export const workspaces = {
  employee: {
    label: 'Employee',
    pages: [
      { path: '', label: 'Overview' },
      { path: 'incidents', label: 'My incidents' },
      { path: 'report', label: 'Report incident' },
      { path: 'notifications', label: 'Notifications' },
    ],
  },
  technician: {
    label: 'Technician',
    pages: [
      { path: '', label: 'Assigned incidents' },
      { path: 'workload', label: 'Workload' },
      { path: 'notifications', label: 'Notifications' },
    ],
  },
  admin: {
    label: 'Administrator',
    pages: [
      { path: '', label: 'Overview' },
      { path: 'incidents', label: 'Incidents' },
      { path: 'users', label: 'Users' },
      { path: 'technicians', label: 'Technicians' },
      { path: 'services', label: 'Services' },
      { path: 'sla-policies', label: 'SLA policies' },
      { path: 'assignment', label: 'Assignment strategy' },
      { path: 'reports', label: 'Reports' },
      { path: 'audit', label: 'Audit logs' },
      { path: 'sla-violations', label: 'SLA violations' },
    ],
  },
}

export function pageUrl(role, path = '') {
  return path ? `/${role}/${path}` : `/${role}`
}