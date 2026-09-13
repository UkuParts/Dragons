import { describe, expect, it } from 'vitest'

import { probabilityRank, probabilityRisk } from '@/utils/probability'

describe('probability', () => {
  it('ranks safer probabilities lower than riskier ones', () => {
    expect(probabilityRank('Sure thing')).toBeLessThan(probabilityRank('Piece of cake'))
    expect(probabilityRank('Piece of cake')).toBeLessThan(probabilityRank('Gamble'))
    expect(probabilityRank('Gamble')).toBeLessThan(probabilityRank('Suicide mission'))
    expect(probabilityRank('Suicide mission')).toBeLessThan(probabilityRank('Impossible'))
  })

  it('classifies known probabilities into risk levels', () => {
    expect(probabilityRisk('Sure thing')).toBe('safe')
    expect(probabilityRisk('Piece of cake')).toBe('safe')
    expect(probabilityRisk('Gamble')).toBe('medium')
    expect(probabilityRisk('Suicide mission')).toBe('risky')
    expect(probabilityRisk('Impossible')).toBe('risky')
  })

  it('falls back to unknown for unexpected probabilities', () => {
    expect(probabilityRisk('definitely not a probability')).toBe('unknown')
  })
})
