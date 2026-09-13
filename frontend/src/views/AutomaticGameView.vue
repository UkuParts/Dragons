<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>

<template>
  <div class="game">
    <header class="game__header">
      <h1 class="game__title">Automatic run</h1>
      <RouterLink class="button button--ghost" :to="{ name: 'home' }">Back home</RouterLink>
    </header>

    <section class="panel placeholder">
      <h2 class="placeholder__title">Coming soon</h2>
      <p class="muted">The automatic player is not implemented yet.</p>
    </section>
  </div>
</template>

<style scoped>
.game {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1.5rem 0;
}

.game__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.game__title {
  font-size: 1.25rem;
}

.placeholder {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.75rem;
  max-width: 34rem;
}

.placeholder__title {
  font-size: 1.05rem;
}
</style>
