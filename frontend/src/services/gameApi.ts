import { http } from './http'
import type { GameState } from '@/types/game'

export const gameApi = {
  startGame: () => http<GameState>('/games', { method: 'POST' }),

  getState: (gameId: string) => http<GameState>(`/games/${encodeURIComponent(gameId)}`),

  solveTask: (gameId: string, adId: string) =>
    http<GameState>(`/games/${encodeURIComponent(gameId)}/solve/${encodeURIComponent(adId)}`, {
      method: 'POST',
    }),

  buyItem: (gameId: string, itemId: string) =>
    http<GameState>(`/games/${encodeURIComponent(gameId)}/shop/buy/${encodeURIComponent(itemId)}`, {
      method: 'POST',
    }),

  investigateReputation: (gameId: string) =>
    http<GameState>(`/games/${encodeURIComponent(gameId)}/investigate/reputation`, {
      method: 'POST',
    }),
}
