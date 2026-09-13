<script setup lang="ts">
import { computed, onMounted, onUnmounted } from 'vue'
import { RouterLink } from 'vue-router'

import PlayerStats from '@/components/PlayerStats.vue'
import ShopPanel from '@/components/ShopPanel.vue'
import TaskBoard from '@/components/TaskBoard.vue'
import { useAutoGame } from '@/composables/useAutoGame'

const { snapshot, error, running, starting, stopReason, start, stop } = useAutoGame()

const busy = computed(() => starting.value || running.value)
const finished = computed(() => snapshot.value !== null && !busy.value && error.value === null)
const stopMessage = computed(() => {
  switch (stopReason.value) {
    case 'GAME_OVER':
      return 'Game over.'
    case 'TURN_LIMIT':
      return 'Turn limit reached.'
    case 'BOARD_DEAD':
      return 'The board ran out of solvable missions.'
    case 'SKIP_LIMIT':
      return 'The board stayed unsolvable for too long.'
    default:
      return 'Run finished.'
  }
})

onMounted(() => {
  void start()
})

onUnmounted(() => {
  stop()
})
</script>

<template>
  <div class="game">
    <header class="game__header">
      <h1 class="game__title">Automatic run</h1>
      <div class="game__actions">
        <RouterLink class="button button--ghost" :to="{ name: 'home' }">Back home</RouterLink>
        <button class="button button--ghost" type="button" :disabled="starting" @click="start">
          Restart
        </button>
      </div>
    </header>

    <p v-if="snapshot === null && busy" class="muted">Starting a new game…</p>

    <template v-if="snapshot">
      <div v-if="finished" class="game-over" role="alert">
        <p>
          {{ stopMessage }} Final score <strong>{{ snapshot.score }}</strong>
        </p>
      </div>

      <PlayerStats :game="snapshot" />

      <div class="game__layout">
        <main class="game__main">
          <TaskBoard :tasks="snapshot.tasks" :busy="busy" read-only />
        </main>
        <aside class="game__side">
          <ShopPanel :items="snapshot.shopItems" :gold="snapshot.gold" :busy="busy" read-only />
        </aside>
      </div>
    </template>

    <p v-if="error" class="alert" role="alert">{{ error }}</p>
    <p
      v-else-if="snapshot?.lastMessage"
      class="notice"
      :class="{ 'notice--failure': snapshot.lastMessageFailed }"
      role="status"
    >
      {{ snapshot.lastMessage }}
    </p>
  </div>
</template>

<style scoped src="@/styles/game.css"></style>
