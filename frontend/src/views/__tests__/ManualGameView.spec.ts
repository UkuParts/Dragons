import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'

const gameApiMock = vi.hoisted(() => ({
  startGame: vi.fn(),
  getTasks: vi.fn(),
  solveTask: vi.fn(),
  getShop: vi.fn(),
  buyItem: vi.fn(),
  investigateReputation: vi.fn(),
}))

vi.mock('@/services/gameApi', () => ({ gameApi: gameApiMock }))

import { routes } from '@/router'
import { useGame } from '@/composables/useGame'
import ManualGameView from '@/views/ManualGameView.vue'

function mountView() {
  const router = createRouter({ history: createMemoryHistory(), routes })
  return mount(ManualGameView, { global: { plugins: [router] } })
}

beforeEach(async () => {
  vi.clearAllMocks()
  gameApiMock.startGame.mockResolvedValue({
    gameId: 'game-1',
    lives: 3,
    gold: 0,
    level: 0,
    score: 0,
    highScore: 0,
    turn: 0,
  })
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
  gameApiMock.getShop.mockResolvedValue([])
  await useGame().startGame()
})

describe('ManualGameView', () => {
  it('shows game over and disables Refresh once the last life is lost', async () => {
    gameApiMock.solveTask.mockResolvedValue({
      success: false,
      lives: 0,
      gold: 0,
      score: 0,
      highScore: 0,
      turn: 1,
      message: 'You lost the game!',
    })
    const wrapper = mountView()
    await flushPromises()

    const refreshButton = wrapper.findAll('button').find((button) => button.text() === 'Refresh')
    expect(refreshButton?.attributes('disabled')).toBeUndefined()

    await wrapper.get('.task__action').trigger('click')
    await flushPromises()

    expect(wrapper.get('.game-over').text()).toContain('Game over. Final score 0')
    expect(refreshButton?.attributes('disabled')).toBeDefined()
  })

  it('shows a failed outcome below the board', async () => {
    gameApiMock.solveTask.mockResolvedValue({
      success: false,
      lives: 2,
      gold: 0,
      score: 0,
      highScore: 0,
      turn: 1,
      message: 'You failed the mission.',
    })
    const wrapper = mountView()
    await flushPromises()

    await wrapper.get('.task__action').trigger('click')
    await flushPromises()

    const notice = wrapper.get('.notice--failure')
    expect(notice.text()).toBe('You failed the mission.')
    expect(wrapper.element.lastElementChild).toBe(notice.element)
  })
})
