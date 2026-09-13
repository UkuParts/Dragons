<script setup lang="ts">
import { onMounted } from 'vue'
import { RouterLink } from 'vue-router'

import PlayerStats from '@/components/PlayerStats.vue'
import ReputationPanel from '@/components/ReputationPanel.vue'
import ShopPanel from '@/components/ShopPanel.vue'
import TaskBoard from '@/components/TaskBoard.vue'
import { useGame } from '@/composables/useGame'

const {
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
  solveTask,
  buyItem,
  investigateReputation,
} = useGame()

onMounted(() => {
  void ensureGame()
})
</script>

<template>
  <div class="game">
    <header class="game__header">
      <h1 class="game__title">Manual run</h1>
      <div class="game__actions">
        <RouterLink class="button button--ghost" :to="{ name: 'home' }">Back home</RouterLink>
        <button
          class="button button--ghost"
          type="button"
          :disabled="busy || !hasGame"
          @click="startGame"
        >
          New game
        </button>
        <button
          class="button button--ghost"
          type="button"
          :disabled="busy || !hasGame || isGameOver"
          @click="refreshState"
        >
          Refresh
        </button>
      </div>
    </header>

    <template v-if="game === null">
      <p v-if="loading" class="muted">Starting a new game…</p>
      <button v-else-if="error" class="button button--primary" type="button" @click="startGame">
        Try again
      </button>
    </template>

    <template v-if="game">
      <div v-if="isGameOver" class="game-over" role="alert">
        <p>
          Game over. Final score <strong>{{ game.score }}</strong>
        </p>
        <button class="button button--primary" type="button" :disabled="busy" @click="startGame">
          Start a new game
        </button>
      </div>

      <PlayerStats :game="game" />

      <div class="game__layout">
        <main class="game__main">
          <TaskBoard :tasks="tasks" :busy="busy || isGameOver" @solve="solveTask" />
        </main>
        <aside class="game__side">
          <ReputationPanel
            :reputation="reputation"
            :investigated-turn="reputationTurn"
            :busy="busy || isGameOver"
            @investigate="investigateReputation"
          />
          <ShopPanel
            :items="shopItems"
            :gold="game.gold"
            :busy="busy || isGameOver"
            @buy="buyItem"
          />
        </aside>
      </div>
    </template>

    <p v-if="error" class="alert" role="alert">{{ error }}</p>
    <p
      v-else-if="lastMessage"
      class="notice"
      :class="{ 'notice--failure': lastMessageFailed }"
      role="status"
    >
      {{ lastMessage }}
    </p>
  </div>
</template>

<style scoped src="@/styles/game.css"></style>
