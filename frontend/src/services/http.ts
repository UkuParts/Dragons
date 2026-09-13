const API_BASE = import.meta.env.VITE_API_BASE || '/api'
const REQUEST_TIMEOUT_MS = Number(import.meta.env.VITE_API_TIMEOUT_MS) || 120_000

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

async function extractErrorMessage(response: Response): Promise<string> {
  try {
    const body = await response.json()
    if (typeof body?.error === 'string') return body.error
  } catch {
    // response body was not JSON, fall through to the generic message
  }
  return `Request failed with status ${response.status}`
}

export async function http<T>(path: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers)
  if (init?.body != null && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS)

  try {
    const response = await fetch(`${API_BASE}${path}`, {
      ...init,
      headers,
      signal: controller.signal,
    })

    if (!response.ok) {
      throw new ApiError(await extractErrorMessage(response), response.status)
    }

    try {
      return (await response.json()) as T
    } catch {
      throw new ApiError('The server returned an invalid response.', response.status)
    }
  } catch (cause) {
    if (cause instanceof ApiError) throw cause
    if (controller.signal.aborted) throw new ApiError('The request timed out.', 0)
    throw new ApiError('The server could not be reached.', 0)
  } finally {
    clearTimeout(timeout)
  }
}
