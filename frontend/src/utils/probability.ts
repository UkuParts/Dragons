const SAFETY_ORDER = [
  'Sure thing',
  'Piece of cake',
  'Walk in the park',
  'Quite likely',
  'Hmmm....',
  'Gamble',
  'Risky',
  'Rather detrimental',
  'Playing with fire',
  'Suicide mission',
  'Impossible',
] as const

export type RiskLevel = 'safe' | 'medium' | 'risky' | 'unknown'

export function probabilityRank(probability: string): number {
  const index = SAFETY_ORDER.indexOf(probability as (typeof SAFETY_ORDER)[number])
  return index === -1 ? SAFETY_ORDER.length : index
}

export function probabilityRisk(probability: string): RiskLevel {
  const rank = probabilityRank(probability)
  if (rank === SAFETY_ORDER.length) return 'unknown'
  if (rank <= 3) return 'safe'
  if (rank <= 5) return 'medium'
  return 'risky'
}
