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

const autoApiMock = vi.hoisted(() => ({
  nextMove: vi.fn(),
}))

vi.mock('@/services/gameApi', () => ({ gameApi: gameApiMock }))
vi.mock('@/services/autoApi', () => ({ autoApi: autoApiMock }))

import App from '@/App.vue'
import { gameState } from '@/__tests__/fixtures'
import { routes } from '@/router'

function createTestRouter() {
  return createRouter({ history: createMemoryHistory(), routes })
}

beforeEach(() => {
  vi.clearAllMocks()
  gameApiMock.startGame.mockResolvedValue(gameState())
  gameApiMock.getState.mockResolvedValue(gameState())
  gameApiMock.solveTask.mockResolvedValue(gameState())
  gameApiMock.buyItem.mockResolvedValue(gameState())
  gameApiMock.investigateReputation.mockResolvedValue(gameState())
  autoApiMock.nextMove.mockResolvedValue({
    finished: true,
    reason: 'GAME_OVER',
    state: gameState({ lives: 0 }),
  })
})

describe('App', () => {
  it('starts on the home view and opens manual mode', async () => {
    const router = createTestRouter()
    const wrapper = mount(App, { global: { plugins: [router] } })
    await router.isReady()

    expect(router.currentRoute.value.name).toBe('home')
    expect(wrapper.text()).toContain('Dragons of Mugloar')

    await wrapper.findAll('.mode')[0]?.trigger('click')
    await vi.dynamicImportSettled()
    await flushPromises()

    expect(router.currentRoute.value.name).toBe('manual')
    expect(wrapper.text()).toContain('Manual run')
    expect(gameApiMock.startGame).toHaveBeenCalled()
  })

  it('opens the automatic run and returns home', async () => {
    const router = createTestRouter()
    const wrapper = mount(App, { global: { plugins: [router] } })
    await router.isReady()

    await wrapper.findAll('.mode')[1]?.trigger('click')
    await vi.dynamicImportSettled()
    await flushPromises()

    expect(router.currentRoute.value.name).toBe('automatic')
    expect(wrapper.text()).toContain('Automatic run')
    expect(autoApiMock.nextMove).toHaveBeenCalledWith('game-1')

    const backHome = wrapper.findAll('a').find((link) => link.text() === 'Back home')
    await backHome?.trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.name).toBe('home')
    expect(wrapper.text()).toContain('Dragons of Mugloar')
  })
})
