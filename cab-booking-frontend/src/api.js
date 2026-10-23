export const BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

export function getAuth() {
  try {
    return JSON.parse(localStorage.getItem('auth'))
  } catch {
    return null
  }
}

export async function api(path, { method = 'GET', body, blob = false } = {}) {
  const auth = getAuth()
  const headers = {}
  if (body) headers['Content-Type'] = 'application/json'
  if (auth?.token) headers['Authorization'] = 'Bearer ' + auth.token

  const res = await fetch(BASE_URL + path, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  })

  if (res.status === 401 && auth?.token) {
    localStorage.removeItem('auth')
    window.location.href = '/login'
    throw new Error('Session expired')
  }

  if (blob && res.ok) return res.blob()

  const text = await res.text()
  let data = null
  try {
    data = text ? JSON.parse(text) : null
  } catch {
    data = null
  }

  if (!res.ok) {
    throw new Error(data?.error || data?.message || 'Request failed (' + res.status + ')')
  }
  return data
}