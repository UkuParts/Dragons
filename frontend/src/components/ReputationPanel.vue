<script setup lang="ts">
import type { Reputation } from '@/types/game'

defineProps<{
  reputation: Reputation | null
  investigatedTurn: number | null
  busy: boolean
}>()

const emit = defineEmits<{ investigate: [] }>()

function format(value: number): string {
  const rounded = Math.round(value * 10) / 10
  return rounded > 0 ? `+${rounded}` : `${rounded}`
}

function mood(value: number): string {
  if (value > 0) return 'reputation__value--positive'
  if (value < 0) return 'reputation__value--negative'
  return ''
}
</script>

<template>
  <section class="panel">
    <h2 class="panel__title">Reputation</h2>
    <p v-if="reputation === null" class="muted">Not investigated yet.</p>
    <ul v-else class="reputation">
      <li class="reputation__row">
        <span>People</span>
        <span class="reputation__value" :class="mood(reputation.people)">
          {{ format(reputation.people) }}
        </span>
      </li>
      <li class="reputation__row">
        <span>State</span>
        <span class="reputation__value" :class="mood(reputation.state)">
          {{ format(reputation.state) }}
        </span>
      </li>
      <li class="reputation__row">
        <span>Underworld</span>
        <span class="reputation__value" :class="mood(reputation.underworld)">
          {{ format(reputation.underworld) }}
        </span>
      </li>
    </ul>

    <p v-if="investigatedTurn !== null" class="reputation__stamp">
      Last investigated turn {{ investigatedTurn }}
    </p>

    <div class="reputation__actions">
      <button class="button" type="button" :disabled="busy" @click="emit('investigate')">
        Investigate
      </button>
    </div>
  </section>
</template>

<style scoped>
.reputation {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.reputation__stamp {
  margin-top: 0.6rem;
  font-size: 0.78rem;
  color: var(--color-text-muted);
}

.reputation__actions {
  margin-top: 0.85rem;
}

.reputation__row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 1rem;
  font-size: 0.9rem;
}

.reputation__value {
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.reputation__value--positive {
  color: var(--color-success);
}

.reputation__value--negative {
  color: var(--color-danger);
}
</style>
