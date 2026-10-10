<script setup>
defineProps({
  variant: { type: String, default: 'neutral' }, // neutral | primary | success | warning | error | info | outline | dark
  size: { type: String, default: 'md' }, // sm | md
  dot: Boolean,
  pill: { type: Boolean, default: true }
})
</script>

<template>
  <span
    class="ui-badge"
    :class="[`ui-badge--${variant}`, `ui-badge--${size}`, { 'is-pill': pill }]"
  >
    <span v-if="dot" class="ui-badge__dot"></span>
    <slot />
  </span>
</template>

<style scoped>
.ui-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-family: var(--font-body);
  font-weight: 500;
  line-height: 1.2;
  border: 1px solid transparent;
  white-space: nowrap;
}
.ui-badge.is-pill {
  border-radius: var(--r-pill);
}
.ui-badge:not(.is-pill) {
  border-radius: var(--r-sm);
}
.ui-badge--sm {
  font-size: 0.6875rem;
  padding: 2px 8px;
}
.ui-badge--md {
  font-size: 0.75rem;
  padding: 3px 10px;
}
.ui-badge__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.ui-badge--neutral {
  background: var(--surface-2);
  color: var(--text-body);
  border-color: var(--border);
}
.ui-badge--outline {
  background: transparent;
  color: var(--text-muted);
  border-color: var(--border-strong);
}
.ui-badge--primary {
  background: var(--primary);
  color: var(--on-primary);
}
.ui-badge--info {
  background: var(--primary-soft);
  color: var(--primary);
}
.ui-badge--success {
  background: color-mix(in srgb, var(--success) 16%, transparent);
  color: var(--success);
}
.ui-badge--warning {
  background: color-mix(in srgb, var(--warning) 18%, transparent);
  color: var(--warning);
}
.ui-badge--error {
  background: color-mix(in srgb, var(--error) 16%, transparent);
  color: var(--error);
}
.ui-badge--dark {
  background: var(--surface-inverse);
  color: var(--text-on-inverse);
}
</style>