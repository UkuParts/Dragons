import { http } from './http'
import type { AutoMoveResult } from '@/types/game'

export const autoApi = {
  nextMove: (gameId: string) =>
    http<AutoMoveResult>(`/auto/games/${encodeURIComponent(gameId)}/next-move`, {
      method: 'POST',
    }),
}
