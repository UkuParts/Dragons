import { describe, expect, it } from 'vitest'

import type { Task } from '@/types/game'
import { decodeField, decodeTask } from '@/utils/decrypt'

describe('decodeField', () => {
  it('decodes padded base64', () => {
    expect(decodeField('UGllY2Ugb2YgY2FrZQ==')).toBe('Piece of cake')
  })

  it('decodes base64 with stripped padding', () => {
    expect(decodeField('UGllY2Ugb2YgY2FrZQ')).toBe('Piece of cake')
  })

  it('decodes URL-safe base64', () => {
    expect(decodeField('Pz8_')).toBe('???')
  })

  it('falls back to rot13 for values that are not base64', () => {
    expect(decodeField('Cvrpr bs pnxr')).toBe('Piece of cake')
  })

  it('falls back to rot13 when base64 decoding yields control characters', () => {
    const encodedControlCharacter = btoa(String.fromCharCode(0x61, 0xc2, 0x85, 0x62))

    expect(decodeField(encodedControlCharacter)).toBe('LpXSLt==')
  })

  it('keeps decoded values that contain line breaks or joiner characters', () => {
    expect(decodeField(btoa('First line\nSecond line'))).toBe('First line\nSecond line')
    expect(decodeField(btoa(String.fromCharCode(0x61, 0xe2, 0x80, 0x8d, 0x62)))).toBe('a\u200db')
  })
})

describe('decodeTask', () => {
  const encrypted: Task = {
    adId: btoa('abc123'),
    message: btoa('Infiltrate The Ivory Pygmy Posse and recover their secrets.'),
    reward: 120,
    expiresIn: 3,
    encrypted: true,
    probability: btoa('Quite likely'),
  }

  it('decodes the id, message and probability of an encrypted task', () => {
    expect(decodeTask(encrypted)).toEqual({
      adId: 'abc123',
      message: 'Infiltrate The Ivory Pygmy Posse and recover their secrets.',
      reward: 120,
      expiresIn: 3,
      encrypted: true,
      probability: 'Quite likely',
    })
  })

  it('leaves plain tasks untouched', () => {
    const plain: Task = {
      adId: 'ad-1',
      message: 'Fix a wagon',
      reward: 10,
      expiresIn: 7,
      encrypted: null,
      probability: 'Piece of cake',
    }

    expect(decodeTask(plain)).toEqual(plain)
  })
})
