<script setup lang="ts">
import { computed } from 'vue'

import type { Task } from '@/types/game'
import { probabilityRank, probabilityRisk } from '@/utils/probability'

const props = defineProps<{
  tasks: Task[]
  busy: boolean
}>()

const emit = defineEmits<{ solve: [adId: string] }>()

const sortedTasks = computed(() =>
  [...props.tasks].sort(
    (a, b) =>
      probabilityRank(a.probability) - probabilityRank(b.probability) || b.reward - a.reward,
  ),
)
</script>

<template>
  <section class="panel board">
    <h2 class="panel__title">Message board</h2>

    <p v-if="sortedTasks.length === 0" class="muted">No missions are available right now.</p>

    <ul v-else class="board__list">
      <li v-for="task in sortedTasks" :key="task.adId" class="task">
        <div class="task__body">
          <p class="task__message">{{ task.message }}</p>
          <div class="task__meta">
            <span class="badge" :class="`badge--${probabilityRisk(task.probability)}`">
              {{ task.probability }}
            </span>
            <span v-if="task.encrypted" class="badge badge--encrypted">Encrypted</span>
            <span class="task__reward">{{ task.reward }} gold</span>
            <span class="task__expires">expires in {{ task.expiresIn }} turns</span>
          </div>
        </div>
        <button
          class="button task__action"
          type="button"
          :disabled="busy"
          @click="emit('solve', task.adId)"
        >
          Solve
        </button>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.board__list {
  list-style: none;
  display: flex;
  flex-direction: column;
}

.task {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1.25rem;
  padding: 0.7rem 0;
  border-bottom: 1px solid var(--color-border);
}

.task:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.task__body {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  min-width: 0;
}

.task__message {
  font-size: 0.92rem;
}

.task__meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem;
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

.task__reward {
  color: var(--color-accent);
  font-weight: 600;
}

.task__action {
  flex-shrink: 0;
  min-width: 4.5rem;
}

@media (max-width: 640px) {
  .task {
    align-items: stretch;
    flex-direction: column;
    gap: 0.75rem;
  }
}
</style>
