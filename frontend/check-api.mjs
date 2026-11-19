import assert from 'node:assert/strict'
import { createServer } from 'vite'
process.env.VITE_USE_MOCKS = 'false'
const server = await createServer({ server: { middlewareMode: true } })
const calls = []
const row = { id: 'INC1', serviceName: 'VPN', reporterName: 'Asha', assignedTechnician: 'Ravi', impact: 'HIGH', urgency: 'MEDIUM', status: 'RESOLVED', feedbackRating: 4, feedbackComment: 'Thanks', resolvedAt: '2026-11-18T10:00:00' }
globalThis.fetch = async (url, options) => {
  calls.push({ url, ...options })
  const result = url.endsWith('/notes') ? [{ authorName: 'Ravi', text: 'Checked', createdAt: '2026-11-18T09:00:00' }] : url.endsWith('/incidents') ? [row] : row
  return { ok: true, status: 200, text: async () => JSON.stringify(result) }
}
try {
  const api = await server.ssrLoadModule('/src/api/incidents.js')
  assert.equal(api.USING_MOCKS, false)
  const detail = await api.getIncident('INC1')
  assert.equal(detail.impact, 'HIGH')
  assert.equal(detail.feedback.rating, 4)
  assert.equal(detail.service.name, 'VPN')
  assert.equal((await api.getIncidents()).items[0].resolvedAt, row.resolvedAt)
  assert.equal((await api.getIncidentNotes('INC1'))[0].author, 'Ravi')
  assert.equal((await api.addIncidentNote('INC1', 'Checked')).notes[0].author, 'Ravi')
  assert.deepEqual(JSON.parse(calls.at(-1).body), { text: 'Checked' })
  await api.submitIncidentFeedback('INC1', 4, 'Thanks')
  assert.deepEqual(JSON.parse(calls.at(-1).body), { rating: 4, comment: 'Thanks' })
  for (const action of ['acknowledge', 'start-work', 'resolve']) {
    await api.technicianAction('INC1', action, 'Fixed')
    assert.ok(calls.at(-1).url.endsWith('/' + action))
    assert.equal(calls.at(-1).method, 'POST')
  }
  assert.deepEqual(JSON.parse(calls.at(-1).body), { note: 'Fixed' })
  await assert.rejects(api.technicianAction('INC1', 'close'))
  console.log('PASS: live adapters, note/feedback shapes and technician action routes')
} finally { await server.close() }
