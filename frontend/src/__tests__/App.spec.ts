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

import App from '@/App.vue'
import { routes } from '@/router'

function createTestRouter() {
  return createRouter({ history: createMemoryHistory(), routes })
}

beforeEach(() => {
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
  gameApiMock.getTasks.mockResolvedValue([])
  gameApiMock.getShop.mockResolvedValue([])
  gameApiMock.investigateReputation.mockResolvedValue({ people: 0, state: 0, underworld: 0 })
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
    expect(gameApiMock.getTasks).toHaveBeenCalledWith('game-1')
  })

  it('opens the automatic placeholder and returns home', async () => {
    const router = createTestRouter()
    const wrapper = mount(App, { global: { plugins: [router] } })
    await router.isReady()

    await wrapper.findAll('.mode')[1]?.trigger('click')
    await vi.dynamicImportSettled()
    await flushPromises()

    expect(router.currentRoute.value.name).toBe('automatic')
    expect(wrapper.text()).toContain('Automatic run')

    const backHome = wrapper.findAll('a').find((link) => link.text() === 'Back home')
    await backHome?.trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.name).toBe('home')
    expect(wrapper.text()).toContain('Dragons of Mugloar')
  })
})
