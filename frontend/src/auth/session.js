import { useSyncExternalStore } from 'react'
import { apiRequest, setAuthToken } from '../api/client'
import { USING_MOCKS } from '../api/incidents'

const key = USING_MOCKS ? 'itsm-demo-session' : 'itsm-session'
const listeners = new Set()
let state = { phase: 'loading', user: null, error: '' }
let pending
function publish(next) { state = next; listeners.forEach((fn) => fn()) }
function remember(value) {
  try { if (value) sessionStorage.setItem(key, value); else sessionStorage.removeItem(key) } catch { /* Memory session still works. */ }
}
function saved() { try { return sessionStorage.getItem(key) } catch { return null } }
function valid(user) { return user && ['EMPLOYEE', 'TECHNICIAN', 'ADMIN'].includes(user.role) }
export function useSession() {
  return useSyncExternalStore((fn) => { listeners.add(fn); return () => listeners.delete(fn) }, () => state)
}
export function clearSession() {
  setAuthToken(null); remember(null); publish({ phase: 'anonymous', user: null, error: '' })
}
export function restoreSession() {
  if (pending) return pending
  publish({ phase: 'loading', user: null, error: '' })
  pending = (async () => {
    const token = saved()
    if (!token) { clearSession(); return }
    try {
      let user
      if (USING_MOCKS) user = JSON.parse(token)
      else { setAuthToken(token); user = await apiRequest('/auth/me') }
      if (!valid(user)) throw new Error('Invalid session response. Please sign in again.')
      publish({ phase: 'ready', user, error: '' })
    } catch (error) {
      if (USING_MOCKS || error.status === 401) clearSession()
      else publish({ phase: 'error', user: null, error: error.message })
    }
  })().finally(() => { pending = null })
  return pending
}
export async function signIn(email, password) {
  let user
  if (USING_MOCKS) {
    if (email.trim().toLowerCase() !== 'asha@bits.edu' || password !== 'password123') {
      throw new Error('Use asha@bits.edu and password123 for the employee demo.')
    }
    user = { userId: 'E1', name: 'Asha', email: 'asha@bits.edu', role: 'EMPLOYEE', department: 'Finance' }
  } else {
    user = await apiRequest('/auth/login', { method: 'POST', body: { email: email.trim(), password } })
    if (!valid(user) || !user.token) throw new Error('The server returned an invalid login response.')
    setAuthToken(user.token)
  }
  remember(USING_MOCKS ? JSON.stringify(user) : user.token)
  publish({ phase: 'ready', user, error: '' })
}
export async function signOut() {
  if (!USING_MOCKS) {
    try { await apiRequest('/auth/logout', { method: 'POST' }) }
    catch (error) { if (error.status !== 401) throw error }
  }
  clearSession()
  // Refresh also clears the in-memory demo incident store.
  window.location.assign('/login')
}
window.addEventListener('itsm-session-expired', clearSession)
