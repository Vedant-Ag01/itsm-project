import { mockIncidents } from './incidents'
import { ApiError } from '../api/client'

const stages = [
  'NEW',
  'CLASSIFIED',
  'ASSIGNED',
  'ACKNOWLEDGED',
  'IN_PROGRESS',
  'RESOLVED',
  'VERIFIED',
  'CLOSED',
]

const stageActions = {
  NEW: 'CREATED',
  CLASSIFIED: 'CLASSIFIED',
  ASSIGNED: 'ASSIGNED',
  ACKNOWLEDGED: 'ACKNOWLEDGED',
  IN_PROGRESS: 'WORK_STARTED',
  RESOLVED: 'RESOLVED',
  VERIFIED: 'VERIFIED',
  CLOSED: 'CLOSED',
}

const priorityLabels = {
  P1: 'Critical',
  P2: 'High',
  P3: 'Medium',
  P4: 'Low',
}

const targets = {
  P1: [5, 30],
  P2: [15, 120],
  P3: [60, 480],
  P4: [240, 1440],
}

// Match the API's local date-time format without a timezone suffix.
function localTimestamp(date) {
  const pad = (value) => String(value).padStart(2, '0')

  return (
    `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}` +
    `T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
  )
}

function addMinutes(timestamp, minutes) {
  const date = new Date(timestamp)
  date.setMinutes(date.getMinutes() + minutes)
  return localTimestamp(date)
}

const records = new Map()
const histories = new Map()

const serviceNames = [...new Set(mockIncidents.map((item) => item.service))]

for (const item of mockIncidents) {
  const stageIndex = stages.indexOf(item.status)
  const [responseMinutes, resolutionMinutes] = targets[item.priority]

  const detail = {
    id: item.id,
    title: item.title,
    description:
      `I am experiencing this issue with ${item.service}. ` +
      'Please investigate and provide an update.',
    category: item.category,
    service: {
      id: serviceNames.indexOf(item.service) + 1,
      name: item.service,
      criticality: item.priority === 'P1' ? 'HIGH' : 'MEDIUM',
    },
    impact: item.priority === 'P1' ? 'HIGH' : 'MEDIUM',
    urgency: item.priority === 'P1' ? 'HIGH' : 'MEDIUM',
    affectedUsers: 1,
    priority: item.priority,
    priorityLabel: priorityLabels[item.priority],
    status: item.status,
    reporter: { id: 'E1', name: 'Naman' },
    assignedTechnician: item.assignedTechnician
      ? {
          id: item.assignedTechnician === 'Ravi' ? 'T1' : 'T2',
          name: item.assignedTechnician,
        }
      : null,
    createdAt: item.createdAt,
    acknowledgedAt:
      stageIndex >= 3 ? addMinutes(item.createdAt, 3) : null,
    resolvedAt:
      stageIndex >= 5 ? addMinutes(item.createdAt, 20) : null,
    closedAt:
      stageIndex >= 7 ? addMinutes(item.createdAt, 30) : null,
    responseDeadline: addMinutes(item.createdAt, responseMinutes),
    resolutionDeadline: addMinutes(item.createdAt, resolutionMinutes),
    slaStatus: item.slaStatus,
    resolutionNote:
      stageIndex >= 5
        ? 'The affected service was restored and checked by the support team.'
        : null,
    closureNote:
      stageIndex >= 7 ? 'The reporter confirmed that the issue is fixed.' : null,
    notes: [],
    feedback: null,
  }

  // Give the historic breached example a genuinely late resolution.
  if (item.slaStatus === 'BREACHED' && stageIndex >= 5) {
    detail.resolvedAt = addMinutes(
      item.createdAt,
      resolutionMinutes + 30,
    )

    if (stageIndex >= 7) {
      detail.closedAt = addMinutes(detail.resolvedAt, 10)
    }
  }

  const events = stages.slice(0, stageIndex + 1).map((stage, index) => {
    let timestamp = addMinutes(item.createdAt, index)

    if (index >= 5) {
      timestamp = addMinutes(detail.resolvedAt, (index - 5) * 5)
    }

    return {
      timestamp,
      user:
        index === 0 || stage === 'VERIFIED'
          ? 'Naman (E1)'
          : 'System',
      action: stageActions[stage],
      incidentId: item.id,
      previousStatus: index === 0 ? null : stages[index - 1],
      newStatus: stage,
      description: `Incident ${stage.toLowerCase().replaceAll('_', ' ')}.`,
    }
  })

  records.set(item.id, detail)
  histories.set(item.id, events)
}

function findIncident(id) {
  const incident = records.get(id)

  if (!incident) {
    throw new ApiError('Incident not found.', 404)
  }

  return incident
}

function copy(value) {
  return structuredClone(value)
}

function recordEvent(incident, action, description, previousStatus) {
  histories.get(incident.id).push({
    timestamp: localTimestamp(new Date()),
    user: 'Naman (E1)',
    action,
    incidentId: incident.id,
    previousStatus,
    newStatus: incident.status,
    description,
  })
}

export function listMockIncidents(filters = {}) {
  const supported = ['id', 'status', 'priority', 'category', 'slaStatus']

  const items = [...records.values()]
    .filter((incident) =>
      supported.every(
        (key) => !filters[key] || incident[key] === filters[key],
      ),
    )
    .map((incident) => ({
      id: incident.id,
      title: incident.title,
      service: incident.service.name,
      category: incident.category,
      priority: incident.priority,
      status: incident.status,
      assignedTechnician: incident.assignedTechnician?.name ?? null,
      slaStatus: incident.slaStatus,
      createdAt: incident.createdAt,
    }))

  return copy({ total: items.length, items })
}

export function getMockIncident(id) {
  return copy(findIncident(id))
}

export function getMockAudit(id) {
  findIncident(id)

  return copy(
    [...histories.get(id)].sort((a, b) =>
      a.timestamp.localeCompare(b.timestamp),
    ),
  )
}

export function addMockNote(id, text) {
  const incident = findIncident(id)
  const cleaned = text.trim()

  if (!cleaned) {
    throw new ApiError('Enter a note before submitting.', 400)
  }

  if (incident.status === 'CLOSED') {
    throw new ApiError('This incident is closed.', 409)
  }

  incident.notes.push({
    author: 'Naman',
    text: cleaned,
    createdAt: localTimestamp(new Date()),
  })

  // Demo audit label; confirm the final notes-event name with the backend.
  recordEvent(incident, 'NOTE_ADDED', 'Reporter added information.', incident.status)

  return copy(incident)
}

export function verifyMockIncident(id) {
  const incident = findIncident(id)

  if (incident.status !== 'RESOLVED') {
    throw new ApiError('Only a resolved incident can be verified.', 409)
  }

  const previousStatus = incident.status
  incident.status = 'VERIFIED'

  recordEvent(
    incident,
    'VERIFIED',
    'Reporter confirmed that the resolution works.',
    previousStatus,
  )

  return copy(incident)
}

export function submitMockFeedback(id, rating, comment) {
  const incident = findIncident(id)

  if (!['RESOLVED', 'VERIFIED', 'CLOSED'].includes(incident.status)) {
    throw new ApiError('Feedback is available after resolution.', 409)
  }

  if (!Number.isInteger(rating) || rating < 1 || rating > 5) {
    throw new ApiError('Choose a rating from 1 to 5.', 400)
  }

  incident.feedback = { rating, comment: comment.trim() }

  recordEvent(
    incident,
    'FEEDBACK_ADDED',
    `Reporter submitted a rating of ${rating}/5.`,
    incident.status,
  )

  return copy(incident)
}