// All HTTP calls go through this file.
// The login token is kept only in memory (not localStorage), so a page refresh logs you out
// but a script injected into the page cannot read it from storage.

const BASE = import.meta.env.VITE_API_BASE || ''

let token = null

export function setToken(value) {
  token = value
}

export async function api(path, { method = 'GET', body, idempotencyKey } = {}) {
  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (token) headers['Authorization'] = `Bearer ${token}`
  if (idempotencyKey) headers['Idempotency-Key'] = idempotencyKey

  let res
  try {
    res = await fetch(BASE + path, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    })
  } catch {
    throw new Error('Cannot reach the server. Is the backend running?')
  }

  const text = await res.text()
  let data = null
  if (text) {
    try {
      data = JSON.parse(text)
    } catch {
      data = null
    }
  }

  if (!res.ok) {
    const err = new Error(data?.message || `Request failed (${res.status})`)
    err.status = res.status
    err.code = data?.error
    err.fieldErrors = data?.fieldErrors
    throw err
  }
  return data
}

export function money(value) {
  if (value === null || value === undefined) return '—'
  return Number(value).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

export function describeError(err) {
  if (err.fieldErrors) {
    return Object.values(err.fieldErrors).join(' ')
  }
  return err.message
}
