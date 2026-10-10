<script setup>
defineProps({
  width: { type: String, default: '100%' },
  height: { type: String, default: '16px' },
  radius: { type: String, default: '' },
  circle: Boolean
})
</script>

<template>
  <span
    class="ui-skeleton"
    :class="{ 'is-circle': circle }"
    :style="{ width, height, borderRadius: radius || undefined }"
  ></span>
</template>

<style scoped>
.ui-skeleton {
  display: block;
  background: linear-gradient(
    90deg,
    var(--surface-3) 25%,
    var(--surface-2) 37%,
    var(--surface-3) 63%
  );
  background-size: 400% 100%;
  border-radius: var(--r-sm);
  animation: ui-skeleton 1.4s ease infinite;
}
.ui-skeleton.is-circle {
  border-radius: 50%;
}
@keyframes ui-skeleton {
  0% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0 50%;
  }
}
</style>