import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import PlayerStats from '@/components/PlayerStats.vue'
import type { Game } from '@/types/game'

const game: Game = {
  gameId: 'game-1',
  lives: 1,
  gold: 50,
  level: 3,
  score: 120,
  highScore: 500,
  turn: 12,
}

describe('PlayerStats', () => {
  it('renders the current lives, gold, level, score and turn', () => {
    const wrapper = mount(PlayerStats, { props: { game } })

    const labels = wrapper.findAll('.stats__label').map((label) => label.text())
    const values = wrapper.findAll('.stats__value').map((value) => value.text())

    expect(labels).toEqual(['Lives', 'Gold', 'Level', 'Score', 'Turn'])
    expect(values).toEqual(['1', '50', '3', '120', '12'])
  })

  it('flags the last life as dangerous', () => {
    const wrapper = mount(PlayerStats, { props: { game } })

    expect(wrapper.get('.stats__value--danger').text()).toBe('1')
  })
})
