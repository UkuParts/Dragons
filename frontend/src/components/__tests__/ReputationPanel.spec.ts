import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import ReputationPanel from '@/components/ReputationPanel.vue'
import type { Reputation } from '@/types/game'

const reputation: Reputation = { people: 1.5, state: 0, underworld: -2 }

function render(
  overrides: {
    reputation?: Reputation | null
    investigatedTurn?: number | null
    busy?: boolean
  } = {},
) {
  return mount(ReputationPanel, {
    props: { reputation: null, investigatedTurn: null, busy: false, ...overrides },
  })
}

describe('ReputationPanel', () => {
  it('shows a placeholder until the reputation is investigated', () => {
    expect(render().text()).toContain('Not investigated yet.')
  })

  it('formats and colours each faction value', () => {
    const wrapper = render({ reputation })

    expect(wrapper.get('.reputation__value--positive').text()).toBe('+1.5')
    expect(wrapper.get('.reputation__value--negative').text()).toBe('-2')
  })

  it('shows the turn the reputation was last investigated', () => {
    const wrapper = render({ reputation, investigatedTurn: 4 })

    expect(wrapper.get('.reputation__stamp').text()).toBe('Last investigated turn 4')
  })

  it('rounds floating point noise before formatting', () => {
    const wrapper = render({
      reputation: { people: 0.30000000000000004, state: 0, underworld: 0 },
    })

    expect(wrapper.text()).toContain('+0.3')
  })

  it('emits investigate when the button is clicked', async () => {
    const wrapper = render()

    await wrapper.get('button').trigger('click')

    expect(wrapper.emitted('investigate')).toHaveLength(1)
  })

  it('disables the investigate button while busy', () => {
    expect(render({ busy: true }).get('button').attributes('disabled')).toBeDefined()
  })
})
