import type { GameState, ShopItem, Task } from '@/types/game'

export const task: Task = {
  adId: 'ad-1',
  message: 'Fix a wagon',
  reward: 10,
  expiresIn: 7,
  encrypted: null,
  probability: 'Piece of cake',
}

export const healingPotion: ShopItem = { id: 'hpot', name: 'Healing potion', cost: 50 }

export function gameState(overrides: Partial<GameState> = {}): GameState {
  return {
    gameId: 'game-1',
    lives: 3,
    gold: 0,
    level: 0,
    score: 0,
    highScore: 0,
    turn: 0,
    tasks: [],
    shopItems: [],
    reputation: null,
    reputationTurn: null,
    lastMessage: null,
    lastMessageFailed: false,
    ...overrides,
  }
}
