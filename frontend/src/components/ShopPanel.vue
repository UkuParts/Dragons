<script setup lang="ts">
import type { ShopItem } from '@/types/game'

defineProps<{
  items: ShopItem[]
  gold: number
  busy: boolean
  readOnly?: boolean
}>()

const emit = defineEmits<{ buy: [itemId: string] }>()
</script>

<template>
  <section class="panel">
    <h2 class="panel__title">Shop</h2>

    <p v-if="items.length === 0" class="muted">Nothing in stock.</p>

    <ul v-else class="shop">
      <li v-for="item in items" :key="item.id" class="shop__item">
        <div>
          <p class="shop__name">{{ item.name }}</p>
          <p class="shop__cost">{{ item.cost }} gold</p>
        </div>
        <button
          v-if="!readOnly"
          class="button"
          type="button"
          :disabled="busy || item.cost > gold"
          :title="item.cost > gold ? 'Not enough gold' : undefined"
          @click="emit('buy', item.id)"
        >
          Buy
        </button>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.shop {
  list-style: none;
  display: flex;
  flex-direction: column;
}

.shop__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.55rem 0;
  border-bottom: 1px solid var(--color-border);
}

.shop__item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.shop__name {
  font-size: 0.9rem;
  font-weight: 500;
}

.shop__cost {
  font-size: 0.8rem;
  color: var(--color-text-muted);
  font-variant-numeric: tabular-nums;
}
</style>
