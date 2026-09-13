import { describe, expect, it } from 'vitest'

import { errorMessage } from '@/utils/errors'

describe('errorMessage', () => {
  it('uses the message of an error', () => {
    expect(errorMessage(new Error('The game API is down.'))).toBe('The game API is down.')
  })

  it('falls back for values that are not errors', () => {
    expect(errorMessage('nope')).toBe('Something went wrong.')
  })
})
