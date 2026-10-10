<script setup>
defineProps({
  as: { type: String, default: 'div' },
  padded: { type: Boolean, default: true },
  hover: Boolean,
  flat: Boolean,
  to: { type: [String, Object], default: null }
})
</script>

<template>
  <component
    :is="to ? 'router-link' : as"
    :to="to || undefined"
    class="ui-card"
    :class="{ 'is-padded': padded, 'is-hover': hover || !!to, 'is-flat': flat }"
  >
    <slot />
  </component>
</template>

<style scoped>
.ui-card {
  display: block;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--r-lg);
  color: var(--text-body);
  transition: border-color 0.15s ease, background-color 0.15s ease, transform 0.15s ease;
}
.ui-card.is-padded {
  padding: var(--sp-xl);
}
.ui-card.is-hover:hover {
  border-color: var(--border-strong);
}
.ui-card.is-flat {
  background: var(--input-bg);
}
</style>