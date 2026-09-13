import { http } from './http'
import type { Game, PurchaseResult, Reputation, ShopItem, SolveResult, Task } from '@/types/game'

export const gameApi = {
  startGame: () => http<Game>('/games', { method: 'POST' }),

  getTasks: (gameId: string) => http<Task[]>(`/games/${encodeURIComponent(gameId)}/messages`),

  solveTask: (gameId: string, adId: string) =>
    http<SolveResult>(`/games/${encodeURIComponent(gameId)}/solve/${encodeURIComponent(adId)}`, {
      method: 'POST',
    }),

  getShop: (gameId: string) => http<ShopItem[]>(`/games/${encodeURIComponent(gameId)}/shop`),

  buyItem: (gameId: string, itemId: string) =>
    http<PurchaseResult>(
      `/games/${encodeURIComponent(gameId)}/shop/buy/${encodeURIComponent(itemId)}`,
      { method: 'POST' },
    ),

  investigateReputation: (gameId: string) =>
    http<Reputation>(`/games/${encodeURIComponent(gameId)}/investigate/reputation`, {
      method: 'POST',
    }),
}
