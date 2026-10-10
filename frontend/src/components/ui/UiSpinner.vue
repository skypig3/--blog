<script setup>
defineProps({
  size: { type: [Number, String], default: 24 },
  label: { type: String, default: '' }
})
</script>

<template>
  <div class="ui-spinner" role="status">
    <span class="ui-spinner__ring" :style="{ width: `${size}px`, height: `${size}px` }"></span>
    <span v-if="label" class="ui-spinner__label">{{ label }}</span>
  </div>
</template>

<style scoped>
.ui-spinner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 40px 0;
  color: var(--text-subtle);
}
.ui-spinner__ring {
  border-radius: 50%;
  border: 2.5px solid var(--border-strong);
  border-top-color: var(--primary);
  animation: ui-spin 0.8s linear infinite;
}
.ui-spinner__label {
  font-size: 0.8125rem;
}
@keyframes ui-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>