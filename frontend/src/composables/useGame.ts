import { computed, ref } from 'vue'

import { gameApi } from '@/services/gameApi'
import { ApiError } from '@/services/http'
import type { Game, Reputation, ShopItem, Task } from '@/types/game'
import { decodeTask } from '@/utils/decrypt'

const game = ref<Game | null>(null)
const tasks = ref<Task[]>([])
const shopItems = ref<ShopItem[]>([])
const reputation = ref<Reputation | null>(null)
const reputationTurn = ref<number | null>(null)
const loading = ref(false)
const acting = ref(false)
const error = ref<string | null>(null)
const lastMessage = ref<string | null>(null)
const lastMessageFailed = ref(false)

const hasGame = computed(() => game.value !== null)
const busy = computed(() => loading.value || acting.value)
const isGameOver = computed(() => game.value !== null && game.value.lives <= 0)

function clearFeedback(): void {
  error.value = null
  lastMessage.value = null
  lastMessageFailed.value = false
}

function setError(message: string): void {
  error.value = message
  lastMessage.value = null
  lastMessageFailed.value = false
}

function reportError(cause: unknown): void {
  setError(cause instanceof Error ? cause.message : 'Something went wrong.')
}

function reportMessage(message: string, failed = false): void {
  lastMessage.value = message
  lastMessageFailed.value = failed
  error.value = null
}

function isGameOverError(cause: unknown): boolean {
  return cause instanceof ApiError && cause.status === 410
}

export function useGame() {
  async function startGame(): Promise<void> {
    if (busy.value) return

    loading.value = true
    clearFeedback()
    try {
      game.value = await gameApi.startGame()
      tasks.value = []
      shopItems.value = []
      reputation.value = null
      reputationTurn.value = null
      await refreshBoard()
    } catch (cause) {
      reportError(cause)
    } finally {
      loading.value = false
    }
  }

  async function refreshBoard(): Promise<void> {
    const current = game.value
    if (current === null) return

    loading.value = true
    error.value = null
    try {
      const [messages, items] = await Promise.all([
        gameApi.getTasks(current.gameId),
        gameApi.getShop(current.gameId),
      ])
      tasks.value = messages.map(decodeTask)
      shopItems.value = items
    } catch (cause) {
      if (isGameOverError(cause)) {
        game.value = { ...current, lives: 0 }
      } else {
        reportError(cause)
      }
    } finally {
      loading.value = false
    }
  }

  async function recoverFromActionFailure(cause: unknown, previous: Game): Promise<void> {
    if (isGameOverError(cause)) {
      game.value = { ...previous, lives: 0 }
      return
    }
    await refreshBoard()
    if (!isGameOver.value) {
      reportError(cause)
    }
  }

  async function investigateReputation(): Promise<void> {
    const current = game.value
    if (current === null || busy.value || current.lives <= 0) return

    acting.value = true
    clearFeedback()
    try {
      reputation.value = await gameApi.investigateReputation(current.gameId)
      const nextTurn = current.turn + 1
      reputationTurn.value = nextTurn
      game.value = { ...current, turn: nextTurn }
      await refreshBoard()
    } catch (cause) {
      await recoverFromActionFailure(cause, current)
    } finally {
      acting.value = false
    }
  }

  async function solveTask(adId: string): Promise<void> {
    const current = game.value
    if (current === null || busy.value || current.lives <= 0) return

    acting.value = true
    clearFeedback()
    try {
      const result = await gameApi.solveTask(current.gameId, adId)
      game.value = {
        ...current,
        lives: result.lives,
        gold: result.gold,
        score: result.score,
        highScore: result.highScore,
        turn: result.turn,
      }
      if (result.lives > 0) {
        await refreshBoard()
      }
      if (error.value === null) {
        reportMessage(result.message, !result.success)
      }
    } catch (cause) {
      await recoverFromActionFailure(cause, current)
    } finally {
      acting.value = false
    }
  }

  async function buyItem(itemId: string): Promise<void> {
    const current = game.value
    if (current === null || busy.value || current.lives <= 0) return

    acting.value = true
    clearFeedback()
    try {
      const result = await gameApi.buyItem(current.gameId, itemId)
      game.value = {
        ...current,
        gold: result.gold,
        lives: result.lives,
        level: result.level,
        turn: result.turn,
      }
      if (result.lives > 0) {
        await refreshBoard()
      }
      const item = shopItems.value.find((candidate) => candidate.id === itemId)
      if (!result.shoppingSuccess) {
        setError(item ? `Could not buy ${item.name}.` : 'The purchase failed.')
      } else if (error.value === null) {
        reportMessage(item ? `Bought ${item.name}.` : 'The purchase succeeded.')
      }
    } catch (cause) {
      await recoverFromActionFailure(cause, current)
    } finally {
      acting.value = false
    }
  }

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
    refreshBoard,
    solveTask,
    buyItem,
    investigateReputation,
  }
}
