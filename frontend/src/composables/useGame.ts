import { computed, ref } from 'vue'

import { gameApi } from '@/services/gameApi'
import { ApiError } from '@/services/http'
import type { GameState } from '@/types/game'
import { errorMessage } from '@/utils/errors'

const state = ref<GameState | null>(null)
const loading = ref(false)
const acting = ref(false)
const error = ref<string | null>(null)

const game = computed(() => state.value)
const tasks = computed(() => state.value?.tasks ?? [])
const shopItems = computed(() => state.value?.shopItems ?? [])
const reputation = computed(() => state.value?.reputation ?? null)
const reputationTurn = computed(() => state.value?.reputationTurn ?? null)
const lastMessage = computed(() => state.value?.lastMessage ?? null)
const lastMessageFailed = computed(() => state.value?.lastMessageFailed ?? false)
const hasGame = computed(() => state.value !== null)
const busy = computed(() => loading.value || acting.value)
const isGameOver = computed(() => state.value !== null && state.value.lives <= 0)

function isGameOverError(cause: unknown): boolean {
  return cause instanceof ApiError && cause.status === 410
}

async function startGame(): Promise<void> {
  if (busy.value) return
  await startNewGame()
}

async function startNewGame(): Promise<void> {
  loading.value = true
  error.value = null
  try {
    state.value = await gameApi.startGame()
  } catch (cause) {
    error.value = errorMessage(cause)
  } finally {
    loading.value = false
  }
}

async function refreshState(): Promise<void> {
  const current = state.value
  if (current === null || busy.value) return

  loading.value = true
  error.value = null
  try {
    state.value = await gameApi.getState(current.gameId)
  } catch (cause) {
    if (cause instanceof ApiError && cause.status === 404) {
      state.value = null
      await startNewGame()
    } else if (isGameOverError(cause)) {
      state.value = { ...current, lives: 0 }
    } else {
      error.value = errorMessage(cause)
    }
  } finally {
    loading.value = false
  }
}

async function ensureGame(): Promise<void> {
  if (state.value === null) {
    await startGame()
    return
  }
  await refreshState()
}

async function recoverFromActionFailure(cause: unknown, previous: GameState): Promise<void> {
  try {
    state.value = await gameApi.getState(previous.gameId)
  } catch {
    state.value = isGameOverError(cause) ? { ...previous, lives: 0 } : previous
  }
  if (!isGameOverError(cause) && !isGameOver.value) {
    error.value = errorMessage(cause)
  }
}

async function act(request: (gameId: string) => Promise<GameState>): Promise<void> {
  const current = state.value
  if (current === null || busy.value || current.lives <= 0) return

  acting.value = true
  error.value = null
  try {
    state.value = await request(current.gameId)
  } catch (cause) {
    await recoverFromActionFailure(cause, current)
  } finally {
    acting.value = false
  }
}

export function useGame() {
  return {
    game,
    tasks,
    shopItems,
    reputation,
    reputationTurn,
    loading,
    busy,
    error,
    lastMessage,
    lastMessageFailed,
    hasGame,
    isGameOver,
    startGame,
    ensureGame,
    refreshState,
    solveTask: (adId: string) => act((gameId) => gameApi.solveTask(gameId, adId)),
    buyItem: (itemId: string) => act((gameId) => gameApi.buyItem(gameId, itemId)),
    investigateReputation: () => act((gameId) => gameApi.investigateReputation(gameId)),
  }
}
