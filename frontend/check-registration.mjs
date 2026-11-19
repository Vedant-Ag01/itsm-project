import assert from 'node:assert/strict'
import { createServer } from 'vite'
process.env.VITE_USE_MOCKS = 'false'
process.env.VITE_REGISTRATION_ENABLED = 'true'
const server = await createServer({ server: { middlewareMode: true } })
const values = { name: ' Naman ', email: 'naman@bits.edu', department: 'CSE', password: 'password123', confirmPassword: 'password123', role: 'ADMIN' }
const calls = []
let status = 201
let networkFailure = false
globalThis.fetch = async (url, options) => {
  if (networkFailure) throw new TypeError('offline')
  calls.push({ url, ...options })
  return { status, ok: status < 400, text: async () => status === 201 ? '' : JSON.stringify({ error: 'Server validation failed' }) }
}
try {
  const { registerEmployee } = await server.ssrLoadModule('/src/api/registration.js')
  await registerEmployee(values)
  assert.ok(calls[0].url.endsWith('/api/auth/register'))
  assert.equal(calls[0].method, 'POST')
  assert.deepEqual(JSON.parse(calls[0].body), { name: 'Naman', email: 'naman@bits.edu', department: 'CSE', password: 'password123' })
  assert.equal(calls[0].headers.Authorization, undefined)
  await assert.rejects(registerEmployee({ ...values, name: '' }), /highlighted/)
  assert.equal(calls.length, 1)
  for (status of [400, 401, 403, 409, 500]) {
    await assert.rejects(registerEmployee(values), (error) => error.status === status && error.message === 'Server validation failed')
  }
  networkFailure = true
  await assert.rejects(registerEmployee(values), (error) => error.status === 0 && /Unable to reach/.test(error.message))
  console.log('PASS: registration request whitelist, validation, empty success, 400/401/403/409/500 and network errors (intercepted API)')
} finally { await server.close() }
