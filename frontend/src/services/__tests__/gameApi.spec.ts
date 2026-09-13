import { beforeEach, describe, expect, it, vi } from 'vitest'

const httpMock = vi.hoisted(() => vi.fn())

vi.mock('@/services/http', () => ({ http: httpMock }))

import { gameApi } from '@/services/gameApi'

beforeEach(() => {
  httpMock.mockReset()
  httpMock.mockResolvedValue({})
})

describe('gameApi', () => {
  it('starts a game', async () => {
    await gameApi.startGame()

    expect(httpMock).toHaveBeenCalledWith('/games', { method: 'POST' })
  })

  it('loads the message board', async () => {
    await gameApi.getTasks('game-1')

    expect(httpMock).toHaveBeenCalledWith('/games/game-1/messages')
  })

  it('solves a task', async () => {
    await gameApi.solveTask('game-1', 'ad-1')

    expect(httpMock).toHaveBeenCalledWith('/games/game-1/solve/ad-1', { method: 'POST' })
  })

  it('loads the shop', async () => {
    await gameApi.getShop('game-1')

    expect(httpMock).toHaveBeenCalledWith('/games/game-1/shop')
  })

  it('buys an item', async () => {
    await gameApi.buyItem('game-1', 'hpot')

    expect(httpMock).toHaveBeenCalledWith('/games/game-1/shop/buy/hpot', { method: 'POST' })
  })

  it('investigates the reputation', async () => {
    await gameApi.investigateReputation('game-1')

    expect(httpMock).toHaveBeenCalledWith('/games/game-1/investigate/reputation', {
      method: 'POST',
    })
  })
})
