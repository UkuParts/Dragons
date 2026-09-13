export type GameMode = 'manual' | 'automatic'

export interface Game {
  gameId: string
  lives: number
  gold: number
  level: number
  score: number
  highScore: number
  turn: number
}

export interface Reputation {
  people: number
  state: number
  underworld: number
}

export interface Task {
  adId: string
  message: string
  reward: number
  expiresIn: number
  encrypted: boolean | null
  probability: string
}

export interface ShopItem {
  id: string
  name: string
  cost: number
}

export interface GameState extends Game {
  tasks: Task[]
  shopItems: ShopItem[]
  reputation: Reputation | null
  reputationTurn: number | null
  lastMessage: string | null
  lastMessageFailed: boolean
}

export type AutoStopReason = 'GAME_OVER' | 'TURN_LIMIT' | 'BOARD_DEAD' | 'SKIP_LIMIT'

export interface AutoMoveResult {
  finished: boolean
  reason: AutoStopReason | null
  state: GameState
}
