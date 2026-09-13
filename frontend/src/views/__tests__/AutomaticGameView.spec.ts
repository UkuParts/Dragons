import { mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'

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

import { useAutoGame } from '@/composables/useAutoGame'
import { gameState, healingPotion, task } from '@/__tests__/fixtures'
import { routes } from '@/router'
import type { GameState } from '@/types/game'
import AutomaticGameView from '@/views/AutomaticGameView.vue'

function runState(overrides: Partial<GameState> = {}): GameState {
  return gameState({
    gold: 450,
    turn: 3,
    tasks: [task],
    shopItems: [healingPotion],
    lastMessage: 'You successfully solved the mission!',
    ...overrides,
  })
}

function mountView() {
  const router = createRouter({ history: createMemoryHistory(), routes })
  return mount(AutomaticGameView, { global: { plugins: [router] } })
}

beforeEach(() => {
  vi.useFakeTimers()
  vi.clearAllMocks()
  gameApiMock.startGame.mockResolvedValue(runState())
  autoApiMock.nextMove.mockResolvedValue({
    finished: true,
    reason: 'GAME_OVER',
    state: runState({ lives: 0, score: 420, lastMessage: 'You lost the game!' }),
  })
})

afterEach(() => {
  useAutoGame().stop()
  vi.useRealTimers()
})

describe('AutomaticGameView', () => {
  it('watches the run without offering game controls', async () => {
    const wrapper = mountView()
    await vi.advanceTimersByTimeAsync(0)

    expect(autoApiMock.nextMove).toHaveBeenCalledWith('game-1')
    expect(wrapper.text()).toContain('Fix a wagon')
    expect(wrapper.text()).toContain('Healing potion')
    expect(wrapper.find('.task__action').exists()).toBe(false)
    expect(wrapper.find('.shop__item button').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('Investigate')
    expect(wrapper.get('.game-over').text()).toContain('Game over. Final score 420')

    wrapper.unmount()
  })

  it('restarts the run on demand', async () => {
    const wrapper = mountView()
    await vi.advanceTimersByTimeAsync(0)

    const restart = wrapper.findAll('button').find((button) => button.text() === 'Restart')
    await restart?.trigger('click')
    await vi.advanceTimersByTimeAsync(0)

    expect(gameApiMock.startGame).toHaveBeenCalledTimes(2)

    wrapper.unmount()
  })

  it('shows the error when a move fails', async () => {
    autoApiMock.nextMove.mockRejectedValue(new Error('The game API is down.'))

    const wrapper = mountView()
    await vi.advanceTimersByTimeAsync(0)

    expect(wrapper.get('.alert').text()).toBe('The game API is down.')

    wrapper.unmount()
  })

  it('explains why the run ended', async () => {
    autoApiMock.nextMove.mockResolvedValue({
      finished: true,
      reason: 'BOARD_DEAD',
      state: runState({ score: 420 }),
    })

    const wrapper = mountView()
    await vi.advanceTimersByTimeAsync(0)

    expect(wrapper.get('.game-over').text()).toContain('The board ran out of solvable missions.')
    expect(wrapper.get('.game-over').text()).toContain('Final score 420')

    wrapper.unmount()
  })
})
