import { ref } from 'vue'

import { autoApi } from '@/services/autoApi'
import { gameApi } from '@/services/gameApi'
import { ApiError } from '@/services/http'
import type { AutoMoveResult, AutoStopReason, GameState } from '@/types/game'
import { errorMessage } from '@/utils/errors'

export const ACTION_PAUSE_MS = 1000
export const MAX_CONSECUTIVE_ERRORS = 5

const snapshot = ref<GameState | null>(null)
const error = ref<string | null>(null)
const running = ref(false)
const starting = ref(false)
const stopReason = ref<AutoStopReason | null>(null)
let generation = 0

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

function retryDelay(cause: unknown): number {
  return cause instanceof ApiError && cause.retryAfterMs !== null
    ? cause.retryAfterMs
    : ACTION_PAUSE_MS
}

async function play(generationAtRun: number): Promise<void> {
  let consecutiveErrors = 0

  while (generationAtRun === generation && running.value) {
    const current = snapshot.value
    if (current === null) break

    let result: AutoMoveResult
    try {
      result = await autoApi.nextMove(current.gameId)
    } catch (cause) {
      if (generationAtRun !== generation) return
      error.value = errorMessage(cause)
      consecutiveErrors += 1
      if (consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) break
      await delay(retryDelay(cause))
      continue
    }

    if (generationAtRun !== generation) return
    snapshot.value = result.state
    error.value = null
    consecutiveErrors = 0

    if (result.finished) {
      stopReason.value = result.reason
      break
    }
    await delay(ACTION_PAUSE_MS)
  }

  if (generationAtRun === generation) {
    running.value = false
  }
}

export function useAutoGame() {
  async function start(): Promise<void> {
    const generationAtStart = ++generation
    running.value = false
    starting.value = true
    snapshot.value = null
    error.value = null
    stopReason.value = null

    try {
      const state = await gameApi.startGame()
      if (generationAtStart !== generation) return
      snapshot.value = state
      starting.value = false
      running.value = true
      void play(generationAtStart)
    } catch (cause) {
      if (generationAtStart === generation) {
        starting.value = false
        error.value = errorMessage(cause)
      }
    }
  }

  function stop(): void {
    generation += 1
    running.value = false
    starting.value = false
  }

  return { snapshot, error, running, starting, stopReason, start, stop }
}
