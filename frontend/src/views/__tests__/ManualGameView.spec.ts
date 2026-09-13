import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'

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
import { routes } from '@/router'
import { ApiError } from '@/services/http'
import type { GameState } from '@/types/game'
import ManualGameView from '@/views/ManualGameView.vue'

function boardState(overrides: Partial<GameState> = {}): GameState {
  return gameState({ tasks: [task], ...overrides })
}

function mountView() {
  const router = createRouter({ history: createMemoryHistory(), routes })
  return mount(ManualGameView, { global: { plugins: [router] } })
}

beforeEach(async () => {
  vi.clearAllMocks()
  gameApiMock.startGame.mockResolvedValue(boardState())
  gameApiMock.getState.mockResolvedValue(boardState())
  await useGame().startGame()
})

describe('ManualGameView', () => {
  it('shows game over and disables Refresh once the last life is lost', async () => {
    const wrapper = mountView()
    await flushPromises()

    const refreshButton = wrapper.findAll('button').find((button) => button.text() === 'Refresh')
    expect(refreshButton?.attributes('disabled')).toBeUndefined()

    gameApiMock.solveTask.mockRejectedValue(new ApiError('Game over.', 410))
    gameApiMock.getState.mockResolvedValue(boardState({ lives: 0 }))

    await wrapper.get('.task__action').trigger('click')
    await flushPromises()

    expect(wrapper.get('.game-over').text()).toContain('Game over. Final score 0')
    expect(refreshButton?.attributes('disabled')).toBeDefined()
  })

  it('shows a failed outcome below the board', async () => {
    const wrapper = mountView()
    await flushPromises()

    gameApiMock.solveTask.mockResolvedValue(
      boardState({ lives: 2, lastMessage: 'You failed the mission.', lastMessageFailed: true }),
    )

    await wrapper.get('.task__action').trigger('click')
    await flushPromises()

    const notice = wrapper.get('.notice--failure')
    expect(notice.text()).toBe('You failed the mission.')
    expect(wrapper.element.lastElementChild).toBe(notice.element)
  })

  it('reloads the stored state through Refresh', async () => {
    const wrapper = mountView()
    await flushPromises()
    gameApiMock.getState.mockClear()

    const refreshButton = wrapper.findAll('button').find((button) => button.text() === 'Refresh')
    await refreshButton?.trigger('click')
    await flushPromises()

    expect(gameApiMock.getState).toHaveBeenCalledWith('game-1')
  })
})
