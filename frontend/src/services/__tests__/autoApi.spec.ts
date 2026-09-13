import { beforeEach, describe, expect, it, vi } from 'vitest'

const httpMock = vi.hoisted(() => vi.fn())

vi.mock('@/services/http', () => ({ http: httpMock }))

import { autoApi } from '@/services/autoApi'

beforeEach(() => {
  httpMock.mockReset()
  httpMock.mockResolvedValue({})
})

describe('autoApi', () => {
  it('requests the next move for a game', async () => {
    await autoApi.nextMove('game-1')

    expect(httpMock).toHaveBeenCalledWith('/auto/games/game-1/next-move', { method: 'POST' })
  })
})
