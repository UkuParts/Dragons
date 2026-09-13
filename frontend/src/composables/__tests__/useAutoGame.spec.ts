import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const autoApiMock = vi.hoisted(() => ({
  nextMove: vi.fn(),
}))

const gameApiMock = vi.hoisted(() => ({
  startGame: vi.fn(),
  getState: vi.fn(),
  solveTask: vi.fn(),
  buyItem: vi.fn(),
  investigateReputation: vi.fn(),
}))

vi.mock('@/services/autoApi', () => ({ autoApi: autoApiMock }))
vi.mock('@/services/gameApi', () => ({ gameApi: gameApiMock }))

import { ACTION_PAUSE_MS, MAX_CONSECUTIVE_ERRORS, useAutoGame } from '@/composables/useAutoGame'
import { gameState } from '@/__tests__/fixtures'
import { ApiError } from '@/services/http'

beforeEach(() => {
  vi.useFakeTimers()
  vi.clearAllMocks()
  gameApiMock.startGame.mockResolvedValue(gameState())
  autoApiMock.nextMove.mockResolvedValue({
    finished: true,
    reason: 'GAME_OVER',
    state: gameState(),
  })
})

afterEach(() => {
  useAutoGame().stop()
  vi.useRealTimers()
})

describe('useAutoGame', () => {
  it('requests one move per second until the run finishes', async () => {
    autoApiMock.nextMove
      .mockResolvedValueOnce({ finished: false, reason: null, state: gameState({ turn: 1 }) })
      .mockResolvedValueOnce({
        finished: true,
        reason: 'GAME_OVER',
        state: gameState({ turn: 2, lives: 0 }),
      })

    const game = useAutoGame()
    await game.start()
    await vi.advanceTimersByTimeAsync(0)

    expect(gameApiMock.startGame).toHaveBeenCalledTimes(1)
    expect(autoApiMock.nextMove).toHaveBeenCalledTimes(1)
    expect(game.snapshot.value?.turn).toBe(1)
    expect(game.running.value).toBe(true)

    await vi.advanceTimersByTimeAsync(ACTION_PAUSE_MS - 1)
    expect(autoApiMock.nextMove).toHaveBeenCalledTimes(1)

    await vi.advanceTimersByTimeAsync(1)
    expect(autoApiMock.nextMove).toHaveBeenCalledTimes(2)
    expect(game.snapshot.value?.turn).toBe(2)
    expect(game.running.value).toBe(false)
    expect(game.stopReason.value).toBe('GAME_OVER')
  })

  it('restarts the run and drops the previous loop', async () => {
    gameApiMock.startGame
      .mockResolvedValueOnce(gameState({ gameId: 'game-1' }))
      .mockResolvedValueOnce(gameState({ gameId: 'game-2' }))
    autoApiMock.nextMove.mockImplementation((gameId: string) =>
      Promise.resolve({ finished: false, reason: null, state: gameState({ gameId }) }),
    )

    const game = useAutoGame()
    await game.start()
    await vi.advanceTimersByTimeAsync(0)
    expect(autoApiMock.nextMove).toHaveBeenLastCalledWith('game-1')

    await game.start()
    await vi.advanceTimersByTimeAsync(0)
    expect(gameApiMock.startGame).toHaveBeenCalledTimes(2)
    expect(autoApiMock.nextMove).toHaveBeenLastCalledWith('game-2')

    const calls = autoApiMock.nextMove.mock.calls.length
    await vi.advanceTimersByTimeAsync(ACTION_PAUSE_MS)

    expect(autoApiMock.nextMove).toHaveBeenCalledTimes(calls + 1)
    expect(autoApiMock.nextMove).toHaveBeenLastCalledWith('game-2')
  })

  it('stops on demand', async () => {
    autoApiMock.nextMove.mockResolvedValue({
      finished: false,
      reason: null,
      state: gameState({ turn: 1 }),
    })

    const game = useAutoGame()
    await game.start()
    await vi.advanceTimersByTimeAsync(0)
    const calls = autoApiMock.nextMove.mock.calls.length

    game.stop()
    await vi.advanceTimersByTimeAsync(2 * ACTION_PAUSE_MS)

    expect(autoApiMock.nextMove).toHaveBeenCalledTimes(calls)
    expect(game.running.value).toBe(false)
  })

  it('reports a failed start', async () => {
    gameApiMock.startGame.mockRejectedValue(new Error('The server could not be reached.'))

    const game = useAutoGame()
    await game.start()

    expect(game.error.value).toBe('The server could not be reached.')
    expect(game.starting.value).toBe(false)
    expect(game.running.value).toBe(false)
  })

  it('retries a transient failed move', async () => {
    autoApiMock.nextMove
      .mockRejectedValueOnce(new Error('The ad has expired.'))
      .mockResolvedValueOnce({
        finished: true,
        reason: 'GAME_OVER',
        state: gameState({ turn: 1, lives: 0 }),
      })

    const game = useAutoGame()
    await game.start()
    await vi.advanceTimersByTimeAsync(0)

    expect(game.error.value).toBe('The ad has expired.')
    expect(game.running.value).toBe(true)

    await vi.advanceTimersByTimeAsync(ACTION_PAUSE_MS)

    expect(autoApiMock.nextMove).toHaveBeenCalledTimes(2)
    expect(game.error.value).toBeNull()
    expect(game.running.value).toBe(false)
  })

  it('honours Retry-After before retrying a rate-limited move', async () => {
    autoApiMock.nextMove
      .mockRejectedValueOnce(new ApiError('Slow down.', 429, 5000))
      .mockResolvedValueOnce({
        finished: true,
        reason: 'GAME_OVER',
        state: gameState({ turn: 1, lives: 0 }),
      })

    const game = useAutoGame()
    await game.start()
    await vi.advanceTimersByTimeAsync(0)

    expect(autoApiMock.nextMove).toHaveBeenCalledTimes(1)
    expect(game.running.value).toBe(true)

    await vi.advanceTimersByTimeAsync(4999)
    expect(autoApiMock.nextMove).toHaveBeenCalledTimes(1)

    await vi.advanceTimersByTimeAsync(1)
    expect(autoApiMock.nextMove).toHaveBeenCalledTimes(2)
    expect(game.error.value).toBeNull()
    expect(game.running.value).toBe(false)
  })

  it('stops after too many consecutive failed moves', async () => {
    autoApiMock.nextMove.mockRejectedValue(new Error('The game API is down.'))

    const game = useAutoGame()
    await game.start()
    await vi.advanceTimersByTimeAsync(MAX_CONSECUTIVE_ERRORS * ACTION_PAUSE_MS)

    expect(autoApiMock.nextMove).toHaveBeenCalledTimes(MAX_CONSECUTIVE_ERRORS)
    expect(game.error.value).toBe('The game API is down.')
    expect(game.running.value).toBe(false)
  })
})
