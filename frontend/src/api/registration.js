import { ApiError, apiRequest } from './client'
import { validateRegistration, registrationPayload } from '../validation/registration'

// Proposed contract, isolated here until docs/api-reference.md is published.
// Do not enable before the backend owner confirms the request and validation rules.
export const REGISTRATION_ENABLED = import.meta.env.VITE_REGISTRATION_ENABLED === 'true'
  && import.meta.env.VITE_USE_MOCKS === 'false'

export async function registerEmployee(values) {
  if (Object.keys(validateRegistration(values)).length) {
    throw new ApiError('Check the highlighted registration fields.', 400)
  }
  if (!REGISTRATION_ENABLED) {
    throw new ApiError('Account registration is not available yet. Please contact your administrator.', 503)
  }
  // No role, user ID, token or confirmation password is supplied by the browser.
  // The server must always create an EMPLOYEE and reject duplicate email addresses.
  await apiRequest('/auth/register', { method: 'POST', body: registrationPayload(values) })
}
