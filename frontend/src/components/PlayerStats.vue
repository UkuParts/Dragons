<script setup lang="ts">
import type { Game } from '@/types/game'

defineProps<{ game: Game }>()
</script>

<template>
  <section class="panel stats" aria-label="Player stats">
    <div class="stats__item">
      <span class="stats__label">Lives</span>
      <span class="stats__value" :class="{ 'stats__value--danger': game.lives <= 1 }">
        {{ game.lives }}
      </span>
    </div>
    <div class="stats__item">
      <span class="stats__label">Gold</span>
      <span class="stats__value">{{ game.gold }}</span>
    </div>
    <div class="stats__item">
      <span class="stats__label">Level</span>
      <span class="stats__value">{{ game.level }}</span>
    </div>
    <div class="stats__item">
      <span class="stats__label">Score</span>
      <span class="stats__value">{{ game.score }}</span>
    </div>
    <div class="stats__item">
      <span class="stats__label">Turn</span>
      <span class="stats__value">{{ game.turn }}</span>
    </div>
  </section>
</template>

<style scoped>
.stats {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 0.5rem 1rem;
}

.stats__item {
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
}

.stats__label {
  font-size: 0.72rem;
  font-weight: 500;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--color-text-muted);
}

.stats__value {
  font-size: 1.2rem;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.stats__value--danger {
  color: var(--color-danger);
}
</style>
