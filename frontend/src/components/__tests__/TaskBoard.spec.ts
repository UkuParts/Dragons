import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import TaskBoard from '@/components/TaskBoard.vue'
import type { Task } from '@/types/game'

const tasks: Task[] = [
  {
    adId: 'risky',
    message: 'Risky one',
    reward: 100,
    expiresIn: 5,
    encrypted: null,
    probability: 'Suicide mission',
  },
  {
    adId: 'easy',
    message: 'Easy one',
    reward: 10,
    expiresIn: 5,
    encrypted: null,
    probability: 'Piece of cake',
  },
  {
    adId: 'rich',
    message: 'Safe but rich',
    reward: 90,
    expiresIn: 5,
    encrypted: null,
    probability: 'Piece of cake',
  },
  {
    adId: 'safest',
    message: 'Sure and steady',
    reward: 5,
    expiresIn: 5,
    encrypted: null,
    probability: 'Sure thing',
  },
]

describe('TaskBoard', () => {
  it('sorts the safest tasks first and breaks ties by reward', () => {
    const wrapper = mount(TaskBoard, { props: { tasks, busy: false } })

    const messages = wrapper.findAll('.task__message').map((node) => node.text())

    expect(messages).toEqual(['Sure and steady', 'Safe but rich', 'Easy one', 'Risky one'])
  })

  it('emits the ad id of the task that should be solved', async () => {
    const wrapper = mount(TaskBoard, { props: { tasks, busy: false } })

    await wrapper.findAll('button')[0]?.trigger('click')

    expect(wrapper.emitted('solve')?.[0]).toEqual(['safest'])
  })

  it('disables every action while the game is busy', () => {
    const wrapper = mount(TaskBoard, { props: { tasks, busy: true } })

    const allDisabled = wrapper
      .findAll('button')
      .every((button) => button.attributes('disabled') !== undefined)

    expect(allDisabled).toBe(true)
  })

  it('hides the actions in read-only mode', () => {
    const wrapper = mount(TaskBoard, { props: { tasks, busy: false, readOnly: true } })

    expect(wrapper.findAll('button')).toHaveLength(0)
  })
})
