import { beforeEach, describe, expect, it, vi } from 'vitest'

const gameApiMock = vi.hoisted(() => ({
  startGame: vi.fn(),
  getState: vi.fn(),
  solveTask: vi.fn(),
  buyItem: vi.fn(),
  investigateReputation: vi.fn(),
}))

vi.mock('@/services/gameApi', () => ({ gameApi: gameApiMock }))

import { useGame } from '@/composables/useGame'
import { gameState, task } from '@/__tests__/fixtures'
import { ApiError } from '@/services/http'

const game = useGame()

beforeEach(async () => {
  vi.clearAllMocks()
  gameApiMock.startGame.mockResolvedValue(gameState())
  gameApiMock.getState.mockResolvedValue(gameState())
  await game.startGame()
})

describe('useGame', () => {
  it('starts a game and stores the returned state', async () => {
    gameApiMock.startGame.mockResolvedValue(
      gameState({
        tasks: [task],
        shopItems: [{ id: 'hpot', name: 'Healing potion', cost: 50 }],
        reputation: { people: 1.5, state: 0, underworld: -2 },
        reputationTurn: 1,
      }),
    )

    await game.startGame()

    expect(game.game.value?.gameId).toBe('game-1')
    expect(game.tasks.value).toHaveLength(1)
    expect(game.shopItems.value).toHaveLength(1)
    expect(game.reputation.value).toEqual({ people: 1.5, state: 0, underworld: -2 })
    expect(game.reputationTurn.value).toBe(1)
    expect(game.hasGame.value).toBe(true)
  })

  it('solves a task and merges the result', async () => {
    gameApiMock.solveTask.mockResolvedValue(
      gameState({
        gold: 12,
        score: 12,
        turn: 1,
        lastMessage: 'You successfully solved the mission!',
      }),
    )

    await game.solveTask('ad-1')

    expect(gameApiMock.solveTask).toHaveBeenCalledWith('game-1', 'ad-1')
    expect(game.game.value?.gold).toBe(12)
    expect(game.game.value?.turn).toBe(1)
    expect(game.lastMessage.value).toBe('You successfully solved the mission!')
    expect(game.lastMessageFailed.value).toBe(false)
  })

  it('keeps a failed task outcome', async () => {
    gameApiMock.solveTask.mockResolvedValue(
      gameState({ lives: 2, lastMessage: 'You failed the mission.', lastMessageFailed: true }),
    )

    await game.solveTask('ad-1')

    expect(game.lastMessage.value).toBe('You failed the mission.')
    expect(game.lastMessageFailed.value).toBe(true)
  })

  it('buys an item and merges the purchase result', async () => {
    gameApiMock.buyItem.mockResolvedValue(
      gameState({ gold: 3, lives: 4, lastMessage: 'Bought Healing potion.' }),
    )

    await game.buyItem('hpot')

    expect(gameApiMock.buyItem).toHaveBeenCalledWith('game-1', 'hpot')
    expect(game.game.value?.gold).toBe(3)
    expect(game.game.value?.lives).toBe(4)
    expect(game.lastMessage.value).toBe('Bought Healing potion.')
  })

  it('keeps a rejected purchase as a failed outcome', async () => {
    gameApiMock.buyItem.mockResolvedValue(
      gameState({ lastMessage: 'Could not buy Healing potion.', lastMessageFailed: true }),
    )

    await game.buyItem('hpot')

    expect(game.lastMessage.value).toBe('Could not buy Healing potion.')
    expect(game.lastMessageFailed.value).toBe(true)
  })

  it('reloads the state after a failed action and reports the error', async () => {
    gameApiMock.solveTask.mockRejectedValue(new Error('The ad has expired.'))
    gameApiMock.getState.mockResolvedValue(gameState({ tasks: [task], turn: 1 }))

    await game.solveTask('ad-1')

    expect(gameApiMock.getState).toHaveBeenCalledWith('game-1')
    expect(game.game.value?.turn).toBe(1)
    expect(game.error.value).toBe('The ad has expired.')
  })

  it('treats 410 as game over', async () => {
    gameApiMock.solveTask.mockRejectedValue(new ApiError('Game over.', 410))
    gameApiMock.getState.mockResolvedValue(gameState({ lives: 0 }))

    await game.solveTask('ad-1')

    expect(game.game.value?.lives).toBe(0)
    expect(game.error.value).toBeNull()
  })

  it('marks the game over on 410 even when the state cannot be reloaded', async () => {
    gameApiMock.solveTask.mockRejectedValue(new ApiError('Game over.', 410))
    gameApiMock.getState.mockRejectedValue(new Error('The server could not be reached.'))

    await game.solveTask('ad-1')

    expect(game.game.value?.lives).toBe(0)
    expect(game.error.value).toBeNull()
  })

  it('ignores actions once the game is over', async () => {
    gameApiMock.solveTask.mockResolvedValue(gameState({ lives: 0 }))
    await game.solveTask('ad-1')
    gameApiMock.solveTask.mockClear()
    gameApiMock.buyItem.mockClear()

    await game.solveTask('ad-1')
    await game.buyItem('hpot')

    expect(gameApiMock.solveTask).not.toHaveBeenCalled()
    expect(gameApiMock.buyItem).not.toHaveBeenCalled()
  })

  it('investigates the reputation', async () => {
    gameApiMock.investigateReputation.mockResolvedValue(
      gameState({
        turn: 1,
        reputation: { people: 0.5, state: -1, underworld: 0 },
        reputationTurn: 1,
      }),
    )

    await game.investigateReputation()

    expect(gameApiMock.investigateReputation).toHaveBeenCalledWith('game-1')
    expect(game.reputation.value).toEqual({ people: 0.5, state: -1, underworld: 0 })
    expect(game.reputationTurn.value).toBe(1)
    expect(game.game.value?.turn).toBe(1)
  })

  it('starts a new game when the stored game is unknown', async () => {
    gameApiMock.getState.mockRejectedValue(new ApiError('Not Found', 404))
    gameApiMock.startGame.mockResolvedValue(gameState({ gameId: 'game-2' }))

    await game.refreshState()

    expect(game.game.value?.gameId).toBe('game-2')
    expect(game.error.value).toBeNull()
  })

  it('refreshes the state through the backend', async () => {
    gameApiMock.getState.mockResolvedValue(gameState({ turn: 5, tasks: [task] }))

    await game.ensureGame()

    expect(gameApiMock.getState).toHaveBeenCalledWith('game-1')
    expect(game.game.value?.turn).toBe(5)
    expect(game.tasks.value).toEqual([task])
  })

  it('reports errors raised by the API', async () => {
    gameApiMock.startGame.mockRejectedValue(new Error('The game API is down.'))

    await game.startGame()

    expect(game.error.value).toBe('The game API is down.')
  })
})
