import assert from 'node:assert/strict'
import { createServer } from 'vite'
process.env.VITE_USE_MOCKS = 'false'
process.env.VITE_REGISTRATION_ENABLED = 'false'
const server = await createServer({ server: { middlewareMode: true } })
let calls = 0
globalThis.fetch = async () => { calls++; throw new Error('Must not call fetch') }
try {
  const { REGISTRATION_ENABLED, registerEmployee } = await server.ssrLoadModule('/src/api/registration.js')
  assert.equal(REGISTRATION_ENABLED, false)
  await assert.rejects(registerEmployee({ name: 'Naman', email: 'naman@bits.edu', password: 'password123', confirmPassword: 'password123', department: '' }), (error) => error.status === 503)
  assert.equal(calls, 0)
  console.log('PASS: unconfirmed registration is disabled and makes no HTTP request')
} finally { await server.close() }
