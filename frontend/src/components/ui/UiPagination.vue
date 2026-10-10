<script setup>
import { computed } from 'vue'
import UiIcon from './UiIcon.vue'

const props = defineProps({
  current: { type: Number, default: 1 },
  total: { type: Number, default: 0 },
  size: { type: Number, default: 10 },
  disabled: Boolean
})
const emit = defineEmits(['change'])

const pages = computed(() => Math.max(1, Math.ceil(props.total / props.size)))

// 生成带省略号的页码序列
const items = computed(() => {
  const p = pages.value
  const c = props.current
  if (p <= 7) return Array.from({ length: p }, (_, i) => i + 1)
  const out = [1]
  const start = Math.max(2, c - 1)
  const end = Math.min(p - 1, c + 1)
  if (start > 2) out.push('...')
  for (let i = start; i <= end; i++) out.push(i)
  if (end < p - 1) out.push('...')
  out.push(p)
  return out
})

function go(p) {
  if (props.disabled || p < 1 || p > pages.value || p === props.current) return
  emit('change', p)
}
</script>

<template>
  <nav v-if="pages > 1" class="ui-pager" aria-label="分页">
    <button class="ui-pager__btn" :disabled="current <= 1 || disabled" @click="go(current - 1)">
      <UiIcon name="chevron-left" :size="16" />
    </button>
    <template v-for="(it, i) in items" :key="`${it}-${i}`">
      <span v-if="it === '...'" class="ui-pager__gap">…</span>
      <button
        v-else
        class="ui-pager__btn"
        :class="{ 'is-active': it === current }"
        :disabled="disabled"
        @click="go(it)"
      >
        {{ it }}
      </button>
    </template>
    <button class="ui-pager__btn" :disabled="current >= pages || disabled" @click="go(current + 1)">
      <UiIcon name="chevron-right" :size="16" />
    </button>
  </nav>
</template>

<style scoped>
.ui-pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  flex-wrap: wrap;
}
.ui-pager__btn {
  min-width: 36px;
  height: 36px;
  padding: 0 8px;
  border: 1px solid var(--border);
  border-radius: var(--r-md);
  background: transparent;
  color: var(--text-body);
  font-size: 0.875rem;
  font-family: inherit;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.15s ease, border-color 0.15s ease, color 0.15s ease;
}
.ui-pager__btn:hover:not(:disabled) {
  background: var(--surface-3);
  color: var(--text);
}
.ui-pager__btn.is-active {
  background: var(--primary);
  border-color: var(--primary);
  color: var(--on-primary);
}
.ui-pager__btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.ui-pager__gap {
  color: var(--text-subtle);
  padding: 0 2px;
}
</style>