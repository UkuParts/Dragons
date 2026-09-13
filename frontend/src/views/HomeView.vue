<script setup lang="ts">
import { RouterLink } from 'vue-router'

import type { GameMode } from '@/types/game'

const modes: {
  mode: GameMode
  title: string
  description: string
  cta: string
}[] = [
  {
    mode: 'manual',
    title: 'Manual',
    description: 'Pick missions from the message board, buy equipment and try to get a high score.',
    cta: 'Play manually',
  },
  {
    mode: 'automatic',
    title: 'Automatic',
    description: 'Watch an automated player run the best-known strategy from start to game over.',
    cta: 'Watch a run',
  },
]
</script>

<template>
  <main class="home">
    <header class="home__hero">
      <h1 class="home__title">Dragons of Mugloar</h1>
      <p class="home__tagline">
        Take on odd jobs, spend your gold wisely and keep an eye on your reputation. Play every turn
        yourself, or hand the reins to an automatic player.
      </p>
    </header>

    <div class="home__modes">
      <RouterLink v-for="mode in modes" :key="mode.mode" class="mode" :to="{ name: mode.mode }">
        <span class="mode__title">{{ mode.title }}</span>
        <span class="mode__description">{{ mode.description }}</span>
        <span class="mode__cta">{{ mode.cta }}</span>
      </RouterLink>
    </div>
  </main>
</template>

<style scoped>
.home {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2.25rem;
  padding: 3.5rem 1rem 2rem;
  text-align: center;
}

.home__hero {
  max-width: 34rem;
}

.home__title {
  margin-bottom: 0.6rem;
  font-size: clamp(1.9rem, 4.5vw, 2.5rem);
  letter-spacing: -0.025em;
}

.home__tagline {
  color: var(--color-text-muted);
}

.home__modes {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(16rem, 1fr));
  gap: 1rem;
  width: min(42rem, 100%);
}

.mode {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.5rem;
  padding: 1.35rem 1.25rem;
  text-align: left;
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  box-shadow: var(--shadow);
  color: inherit;
  text-decoration: none;
  cursor: pointer;
  transition:
    border-color 120ms ease,
    box-shadow 120ms ease;
}

.mode:hover {
  border-color: var(--color-border-strong);
  box-shadow: 0 2px 6px rgb(20 20 15 / 7%);
}

.mode:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

.mode__title {
  font-size: 1.1rem;
}

.mode__description {
  color: var(--color-text-muted);
  font-size: 0.9rem;
}

.mode__cta {
  margin-top: auto;
  padding-top: 0.6rem;
  font-weight: 600;
  font-size: 0.9rem;
  color: var(--color-accent);
}
</style>
