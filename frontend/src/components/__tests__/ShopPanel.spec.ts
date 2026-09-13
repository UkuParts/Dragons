import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import ShopPanel from '@/components/ShopPanel.vue'
import type { ShopItem } from '@/types/game'

const items: ShopItem[] = [
  { id: 'hpot', name: 'Healing potion', cost: 50 },
  { id: 'sword', name: 'Sword', cost: 120 },
]

describe('ShopPanel', () => {
  it('disables only the items the player cannot afford', () => {
    const wrapper = mount(ShopPanel, { props: { items, gold: 100, busy: false } })

    const buttons = wrapper.findAll('button')

    expect(buttons[0]?.attributes('disabled')).toBeUndefined()
    expect(buttons[1]?.attributes('disabled')).toBeDefined()
  })

  it('emits the item id when buying', async () => {
    const wrapper = mount(ShopPanel, { props: { items, gold: 100, busy: false } })

    await wrapper.findAll('button')[0]?.trigger('click')

    expect(wrapper.emitted('buy')?.[0]).toEqual(['hpot'])
  })

  it('disables every purchase while the game is busy', () => {
    const wrapper = mount(ShopPanel, { props: { items, gold: 100, busy: true } })

    const allDisabled = wrapper
      .findAll('button')
      .every((button) => button.attributes('disabled') !== undefined)

    expect(allDisabled).toBe(true)
  })
})
