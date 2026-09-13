import { beforeEach, describe, expect, it, vi } from 'vitest'

const gameApiMock = vi.hoisted(() => ({
  startGame: vi.fn(),
  getTasks: vi.fn(),
  solveTask: vi.fn(),
  getShop: vi.fn(),
  buyItem: vi.fn(),
  investigateReputation: vi.fn(),
}))

vi.mock('@/services/gameApi', () => ({ gameApi: gameApiMock }))

import { useGame } from '@/composables/useGame'
import { ApiError } from '@/services/http'

const game = useGame()

const defaultGame = {
  gameId: 'game-1',
  lives: 3,
  gold: 0,
  level: 0,
  score: 0,
  highScore: 0,
  turn: 0,
}

async function beginGame(): Promise<void> {
  gameApiMock.startGame.mockResolvedValue(defaultGame)
  await game.startGame()
}

beforeEach(async () => {
  vi.clearAllMocks()
  gameApiMock.startGame.mockResolvedValue(defaultGame)
  gameApiMock.getTasks.mockResolvedValue([])
  gameApiMock.getShop.mockResolvedValue([])
  gameApiMock.investigateReputation.mockResolvedValue({ people: 0, state: 0, underworld: 0 })
  await game.startGame()
})

describe('useGame', () => {
  it('starts a game and loads the board', async () => {
    gameApiMock.getTasks.mockResolvedValue([
      {
        adId: 'ad-1',
        message: 'Fix a wagon',
        reward: 10,
        expiresIn: 7,
        encrypted: null,
        probability: 'Piece of cake',
      },
    ])

    await beginGame()

    expect(game.game.value?.gameId).toBe('game-1')
    expect(game.tasks.value).toHaveLength(1)
    expect(gameApiMock.investigateReputation).not.toHaveBeenCalled()
  })

  it('decrypts encrypted tasks before they reach the board', async () => {
    gameApiMock.getTasks.mockResolvedValue([
      {
        adId: btoa('abc123'),
        message: btoa('Infiltrate The Ivory Pygmy Posse and recover their secrets.'),
        reward: 120,
        expiresIn: 3,
        encrypted: true,
        probability: btoa('Quite likely'),
      },
    ])

    await beginGame()

    const task = game.tasks.value[0]
    expect(task?.adId).toBe('abc123')
    expect(task?.message).toContain('Ivory Pygmy Posse')
    expect(task?.probability).toBe('Quite likely')

    gameApiMock.solveTask.mockResolvedValue({
      success: true,
      lives: 2,
      gold: 120,
      score: 120,
      highScore: 0,
      turn: 1,
      message: 'You successfully solved the mission!',
    })
    await game.solveTask(task?.adId ?? '')

    expect(gameApiMock.solveTask).toHaveBeenCalledWith('game-1', 'abc123')
  })

  it('solves a task and merges the result', async () => {
    await beginGame()
    gameApiMock.solveTask.mockResolvedValue({
      success: true,
      lives: 3,
      gold: 12,
      score: 12,
      highScore: 0,
      turn: 1,
      message: 'You successfully solved the mission!',
    })

    await game.solveTask('ad-1')

    expect(gameApiMock.solveTask).toHaveBeenCalledWith('game-1', 'ad-1')
    expect(game.game.value?.gold).toBe(12)
    expect(game.game.value?.turn).toBe(1)
    expect(game.lastMessage.value).toBe('You successfully solved the mission!')
    expect(game.lastMessageFailed.value).toBe(false)
  })

  it('marks a failed task outcome', async () => {
    await beginGame()
    gameApiMock.solveTask.mockResolvedValue({
      success: false,
      lives: 2,
      gold: 0,
      score: 0,
      highScore: 0,
      turn: 1,
      message: 'You failed the mission.',
    })

    await game.solveTask('ad-1')

    expect(game.lastMessage.value).toBe('You failed the mission.')
    expect(game.lastMessageFailed.value).toBe(true)
  })

  it('buys an item and merges the purchase result', async () => {
    gameApiMock.getShop.mockResolvedValue([{ id: 'hpot', name: 'Healing potion', cost: 50 }])
    await beginGame()
    gameApiMock.buyItem.mockResolvedValue({
      shoppingSuccess: true,
      gold: 3,
      lives: 4,
      level: 0,
      turn: 5,
    })

    await game.buyItem('hpot')

    expect(game.game.value?.gold).toBe(3)
    expect(game.game.value?.lives).toBe(4)
    expect(game.game.value?.turn).toBe(5)
    expect(game.lastMessage.value).toBe('Bought Healing potion.')
  })

  it('merges a rejected purchase and reports it', async () => {
    gameApiMock.getShop.mockResolvedValue([{ id: 'hpot', name: 'Healing potion', cost: 50 }])
    await beginGame()
    gameApiMock.buyItem.mockResolvedValue({
      shoppingSuccess: false,
      gold: 0,
      lives: 3,
      level: 0,
      turn: 1,
    })

    await game.buyItem('hpot')

    expect(game.error.value).toBe('Could not buy Healing potion.')
    expect(game.game.value?.turn).toBe(1)
  })

  it('keeps a board refresh error over the outcome message', async () => {
    await beginGame()
    gameApiMock.solveTask.mockResolvedValue({
      success: true,
      lives: 3,
      gold: 12,
      score: 12,
      highScore: 0,
      turn: 1,
      message: 'You successfully solved the mission!',
    })
    gameApiMock.getTasks.mockRejectedValue(new Error('The game API is down.'))

    await game.solveTask('ad-1')

    expect(game.error.value).toBe('The game API is down.')
    expect(game.lastMessage.value).toBeNull()
  })

  it('keeps a failed board refresh over the purchase outcome', async () => {
    gameApiMock.getShop.mockResolvedValue([{ id: 'hpot', name: 'Healing potion', cost: 50 }])
    await beginGame()
    gameApiMock.buyItem.mockResolvedValue({
      shoppingSuccess: true,
      gold: 3,
      lives: 4,
      level: 0,
      turn: 5,
    })
    gameApiMock.getTasks.mockRejectedValue(new Error('The game API is down.'))

    await game.buyItem('hpot')

    expect(game.error.value).toBe('The game API is down.')
    expect(game.lastMessage.value).toBeNull()
  })

  it('investigates reputation and advances the turn', async () => {
    await beginGame()
    gameApiMock.investigateReputation.mockResolvedValue({ people: 0.5, state: -1, underworld: 0 })
    gameApiMock.getTasks.mockClear()

    await game.investigateReputation()

    expect(gameApiMock.investigateReputation).toHaveBeenCalledWith('game-1')
    expect(game.reputation.value).toEqual({ people: 0.5, state: -1, underworld: 0 })
    expect(game.reputationTurn.value).toBe(1)
    expect(game.game.value?.turn).toBe(1)
    expect(gameApiMock.getTasks).toHaveBeenCalledWith('game-1')
  })

  it('clears the reputation turn when a new game starts', async () => {
    await beginGame()
    gameApiMock.investigateReputation.mockResolvedValue({ people: 0.5, state: 0, underworld: 0 })
    await game.investigateReputation()
    expect(game.reputationTurn.value).toBe(1)

    await beginGame()

    expect(game.reputation.value).toBeNull()
    expect(game.reputationTurn.value).toBeNull()
  })

  it('reports errors raised by the API', async () => {
    gameApiMock.startGame.mockRejectedValue(new Error('The game API is down.'))

    await game.startGame()

    expect(game.error.value).toBe('The game API is down.')
  })

  it('ignores actions once the game is over', async () => {
    await beginGame()
    if (game.game.value) {
      game.game.value = { ...game.game.value, lives: 0 }
    }

    await game.solveTask('ad-1')
    await game.buyItem('hpot')

    expect(gameApiMock.solveTask).not.toHaveBeenCalled()
    expect(gameApiMock.buyItem).not.toHaveBeenCalled()
  })

  it('reloads the board after a failed task', async () => {
    await beginGame()
    gameApiMock.getTasks.mockClear()
    gameApiMock.solveTask.mockRejectedValue(new Error('The ad has expired.'))

    await game.solveTask('ad-1')

    expect(gameApiMock.getTasks).toHaveBeenCalledWith('game-1')
    expect(game.error.value).toBe('The ad has expired.')
  })

  it('reloads the board after a failed investigation', async () => {
    await beginGame()
    gameApiMock.getTasks.mockClear()
    gameApiMock.investigateReputation.mockRejectedValue(new Error('The game API is down.'))

    await game.investigateReputation()

    expect(gameApiMock.getTasks).toHaveBeenCalledWith('game-1')
    expect(game.error.value).toBe('The game API is down.')
  })

  it('clears the previous board when the new one fails to load', async () => {
    gameApiMock.getTasks.mockResolvedValue([
      {
        adId: 'ad-1',
        message: 'Fix a wagon',
        reward: 10,
        expiresIn: 7,
        encrypted: null,
        probability: 'Piece of cake',
      },
    ])
    await beginGame()
    expect(game.tasks.value).toHaveLength(1)

    gameApiMock.startGame.mockResolvedValue({
      gameId: 'game-2',
      lives: 3,
      gold: 0,
      level: 0,
      score: 0,
      highScore: 0,
      turn: 0,
    })
    gameApiMock.getTasks.mockRejectedValue(new Error('The game API is down.'))

    await game.startGame()

    expect(game.game.value?.gameId).toBe('game-2')
    expect(game.tasks.value).toEqual([])
  })

  it('skips the board refresh when the last life is gone', async () => {
    await beginGame()
    gameApiMock.getTasks.mockClear()
    gameApiMock.solveTask.mockResolvedValue({
      success: false,
      lives: 0,
      gold: 0,
      score: 0,
      highScore: 0,
      turn: 3,
      message: 'You lost the game!',
    })

    await game.solveTask('ad-1')

    expect(gameApiMock.getTasks).not.toHaveBeenCalled()
  })

  it('treats 410 as game over without refetching', async () => {
    await beginGame()
    gameApiMock.getTasks.mockClear()
    gameApiMock.solveTask.mockRejectedValue(new ApiError('Game over.', 410))

    await game.solveTask('ad-1')

    expect(game.game.value?.lives).toBe(0)
    expect(game.error.value).toBeNull()
    expect(gameApiMock.getTasks).not.toHaveBeenCalled()
  })

  it('marks the game over when a board refresh returns 410', async () => {
    await beginGame()
    gameApiMock.getTasks.mockRejectedValue(new ApiError('Game over.', 410))

    await game.refreshBoard()

    expect(game.game.value?.lives).toBe(0)
    expect(game.error.value).toBeNull()
  })

  it('keeps the game over state over the action error', async () => {
    await beginGame()
    gameApiMock.solveTask.mockRejectedValue(new Error('The ad has expired.'))
    gameApiMock.getTasks.mockRejectedValue(new ApiError('Game over.', 410))

    await game.solveTask('ad-1')

    expect(game.game.value?.lives).toBe(0)
    expect(game.error.value).toBeNull()
  })

  it('ignores a second start while one is already running', async () => {
    let resolveStart: (value: unknown) => void = () => {}
    gameApiMock.startGame.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveStart = resolve
        }),
    )
    gameApiMock.startGame.mockClear()

    const first = game.startGame()
    await game.startGame()

    expect(gameApiMock.startGame).toHaveBeenCalledTimes(1)

    resolveStart(defaultGame)
    await first
  })
})
