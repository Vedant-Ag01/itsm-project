const API_BASE_URL = (
  import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
).replace(/\/+$/, '')

let authToken = null

export function setAuthToken(token) {
  authToken = token || null
}

export class ApiError extends Error {
  constructor(message, status) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

export async function apiRequest(
  path,
  { method = 'GET', body, signal } = {},
) {
  const headers = {
    Accept: 'application/json',
  }

  if (authToken) {
    headers.Authorization = `Bearer ${authToken}`
  }

  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }

  let response

  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method,
      headers,
      signal,
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch (error) {
    if (error.name === 'AbortError') {
      throw error
    }

    throw new ApiError(
      'Unable to reach the server. Check your connection and try again.',
      0,
    )
  }

  const text = await response.text()
  let data = null

  if (text) {
    try {
      data = JSON.parse(text)
    } catch {
      throw new ApiError(
        response.ok
          ? 'The server returned an unexpected response.'
          : `Request failed (${response.status}). Please try again.`,
        response.status,
      )
    }
  }

  if (!response.ok) {
    throw new ApiError(
      data?.error || `Request failed (${response.status}).`,
      response.status,
    )
  }

  return data
}