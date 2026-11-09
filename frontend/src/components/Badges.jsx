const statuses = {
  NEW: ['New', 'neutral'],
  CLASSIFIED: ['Classified', 'neutral'],
  ASSIGNED: ['Assigned', 'info'],
  ACKNOWLEDGED: ['Acknowledged', 'info'],
  IN_PROGRESS: ['In progress', 'info'],
  RESOLVED: ['Resolved', 'success'],
  VERIFIED: ['Verified', 'success'],
  CLOSED: ['Closed', 'neutral'],
}

const priorities = {
  P1: ['P1 · Critical', 'danger'],
  P2: ['P2 · High', 'warning'],
  P3: ['P3 · Medium', 'medium'],
  P4: ['P4 · Low', 'neutral'],
}

const slaStatuses = {
  ON_TRACK: ['On track', 'success'],
  AT_RISK: ['At risk', 'warning'],
  BREACHED: ['Breached', 'danger'],
}

function Badge({ value, options }) {
  const [label, tone] = options[value] ?? [
    value || 'Not available',
    'neutral',
  ]

  return <span className={`badge badge--${tone}`}>{label}</span>
}

export function StatusBadge({ value }) {
  return <Badge value={value} options={statuses} />
}

export function PriorityBadge({ value }) {
  return <Badge value={value} options={priorities} />
}

export function SlaBadge({ value }) {
  return <Badge value={value} options={slaStatuses} />
}