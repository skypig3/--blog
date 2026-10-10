<script setup>
import { computed } from 'vue'
import { RouterLink } from 'vue-router'

const props = defineProps({
  danger: Boolean,
  disabled: Boolean,
  to: { type: [String, Object], default: null }
})

const tag = computed(() => (props.to ? RouterLink : 'button'))
const bindings = computed(() =>
  props.to ? { to: props.to } : { type: 'button', disabled: props.disabled }
)
</script>

<template>
  <component :is="tag" v-bind="bindings" class="ui-menu-item" :class="{ 'is-danger': danger }">
    <slot />
  </component>
</template>

<style scoped>
.ui-menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 9px 12px;
  border: 0;
  background: transparent;
  border-radius: var(--r-sm);
  color: var(--text-body);
  font-family: inherit;
  font-size: 0.875rem;
  text-align: left;
  text-decoration: none;
  cursor: pointer;
}
.ui-menu-item:hover:not(:disabled) {
  background: var(--surface-3);
  color: var(--text);
}
.ui-menu-item.is-danger {
  color: var(--error);
}
.ui-menu-item.is-danger:hover:not(:disabled) {
  background: color-mix(in srgb, var(--error) 12%, transparent);
}
.ui-menu-item:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>