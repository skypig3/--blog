<script setup>
import { computed } from 'vue'

const props = defineProps({
  src: { type: String, default: '' },
  name: { type: String, default: '' },
  size: { type: [Number, String], default: 40 },
  square: Boolean
})

const initials = computed(() => {
  const n = (props.name || '').trim()
  if (!n) return '?'
  // 中文取最后一字，英文取首字母
  if (/[\u4e00-\u9fa5]/.test(n)) return n.slice(-1)
  return n.slice(0, 1).toUpperCase()
})
const px = computed(() => `${props.size}px`)
</script>

<template>
  <span
    class="ui-avatar"
    :class="{ 'is-square': square }"
    :style="{ width: px, height: px, fontSize: `calc(${px} * 0.42)` }"
  >
    <img v-if="src" :src="src" :alt="name" />
    <span v-else class="ui-avatar__text">{{ initials }}</span>
  </span>
</template>

<style scoped>
.ui-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  overflow: hidden;
  background: var(--surface-2);
  color: var(--text-body);
  border: 1px solid var(--border);
  font-weight: 500;
  flex-shrink: 0;
  user-select: none;
}
.ui-avatar.is-square {
  border-radius: var(--r-md);
}
.ui-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>