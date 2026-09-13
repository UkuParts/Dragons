import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { ApiError, http } from '@/services/http'

const fetchMock = vi.fn()

function jsonResponse(body: unknown, status = 200): Response {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: () => Promise.resolve(body),
  } as Response
}

function requestInit(): RequestInit {
  return fetchMock.mock.calls[0]?.[1] as RequestInit
}

beforeEach(() => {
  fetchMock.mockReset()
  vi.stubGlobal('fetch', fetchMock)
})

afterEach(() => {
  vi.useRealTimers()
  vi.unstubAllGlobals()
})

describe('http', () => {
  it('requests the API base path and returns the parsed body', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ gameId: 'game-1' }))

    const result = await http<{ gameId: string }>('/games', { method: 'POST' })

    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(fetchMock.mock.calls[0]?.[0]).toBe('/api/games')
    expect(requestInit().method).toBe('POST')
    expect(result).toEqual({ gameId: 'game-1' })
  })

  it('sends a JSON content type when the request has a body', async () => {
    fetchMock.mockResolvedValue(jsonResponse({}))

    await http('/games', { method: 'POST', body: '{}' })

    const headers = requestInit().headers as Headers
    expect(headers.get('Content-Type')).toBe('application/json')
  })

  it('does not send a content type without a body', async () => {
    fetchMock.mockResolvedValue(jsonResponse({}))

    await http('/games')

    const headers = requestInit().headers as Headers
    expect(headers.has('Content-Type')).toBe(false)
  })

  it('throws an ApiError carrying the backend message', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ error: 'The game API is down.' }, 502))

    await expect(http('/games')).rejects.toMatchObject({
      name: 'ApiError',
      message: 'The game API is down.',
      status: 502,
    })
  })

  it('falls back to a generic message when the error body is not JSON', async () => {
    fetchMock.mockResolvedValue({
      ok: false,
      status: 500,
      json: () => Promise.reject(new SyntaxError('Unexpected token')),
    } as unknown as Response)

    const error = await http('/games').catch((cause: unknown) => cause)

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).message).toBe('Request failed with status 500')
  })

  it('throws an ApiError when a successful response is not JSON', async () => {
    fetchMock.mockResolvedValue({
      ok: true,
      status: 200,
      json: () => Promise.reject(new SyntaxError('Unexpected token')),
    } as unknown as Response)

    await expect(http('/games')).rejects.toMatchObject({
      name: 'ApiError',
      message: 'The server returned an invalid response.',
      status: 200,
    })
  })

  it('throws an ApiError when the network request fails', async () => {
    fetchMock.mockRejectedValue(new TypeError('Failed to fetch'))

    await expect(http('/games')).rejects.toMatchObject({
      name: 'ApiError',
      message: 'The server could not be reached.',
      status: 0,
    })
  })

  it('times out requests that take too long', async () => {
    vi.useFakeTimers()
    fetchMock.mockImplementation(
      (_url: string, init: RequestInit) =>
        new Promise((_resolve, reject) => {
          init.signal?.addEventListener('abort', () =>
            reject(new DOMException('The operation was aborted.', 'AbortError')),
          )
        }),
    )

    const request = http('/games')
    const rejection = expect(request).rejects.toMatchObject({
      name: 'ApiError',
      message: 'The request timed out.',
      status: 0,
    })

    await vi.advanceTimersByTimeAsync(120_000)
    await rejection
  })
})
