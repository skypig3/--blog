<script setup>
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import UiIcon from './UiIcon.vue'

const props = defineProps({
  variant: { type: String, default: 'primary' }, // primary | secondary | ghost | inverse | danger | link
  size: { type: String, default: 'md' }, // sm | md | lg
  block: Boolean,
  disabled: Boolean,
  loading: Boolean,
  type: { type: String, default: 'button' },
  to: { type: [String, Object], default: null },
  href: { type: String, default: '' },
  icon: { type: String, default: '' },
  iconRight: { type: String, default: '' },
  square: Boolean
})

const tag = computed(() => (props.to ? RouterLink : props.href ? 'a' : 'button'))
const bindings = computed(() => {
  if (props.to) return { to: props.to }
  if (props.href) return { href: props.href, target: '_blank', rel: 'noopener' }
  return { type: props.type, disabled: props.disabled || props.loading }
})
</script>

<template>
  <component
    :is="tag"
    v-bind="bindings"
    class="ui-btn"
    :class="[
      `ui-btn--${variant}`,
      `ui-btn--${size}`,
      { 'is-block': block, 'is-disabled': disabled, 'is-loading': loading, 'is-square': square }
    ]"
  >
    <span v-if="loading" class="ui-btn__spin" aria-hidden="true"></span>
    <UiIcon v-else-if="icon" :name="icon" :size="size === 'lg' ? 20 : 18" />
    <span v-if="$slots.default" class="ui-btn__label"><slot /></span>
    <UiIcon v-if="iconRight && !loading" :name="iconRight" :size="size === 'lg' ? 20 : 18" />
  </component>
</template>

<style scoped>
.ui-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 1px solid transparent;
  border-radius: var(--r-md);
  font-family: var(--font-body);
  font-weight: 500;
  line-height: 1;
  white-space: nowrap;
  transition: background-color 0.15s ease, border-color 0.15s ease, color 0.15s ease,
    opacity 0.15s ease;
  text-decoration: none;
}
.ui-btn.is-block {
  width: 100%;
}
.ui-btn.is-disabled,
.ui-btn:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}
.ui-btn.is-square {
  padding: 0;
  aspect-ratio: 1;
}

/* 尺寸 */
.ui-btn--sm {
  padding: 6px 12px;
  font-size: 0.8125rem;
  min-height: 32px;
}
.ui-btn--md {
  padding: 0 18px;
  font-size: 0.875rem;
  min-height: 40px;
}
.ui-btn--lg {
  padding: 0 24px;
  font-size: 0.9375rem;
  min-height: 48px;
}

/* 变体 */
.ui-btn--primary {
  background: var(--primary);
  color: var(--on-primary);
}
.ui-btn--primary:not(:disabled):hover {
  background: var(--primary-hover);
}

.ui-btn--secondary {
  background: var(--input-bg);
  border-color: var(--border-strong);
  color: var(--text);
}
.ui-btn--secondary:not(:disabled):hover {
  background: var(--surface-3);
  border-color: var(--text-subtle);
}

.ui-btn--ghost {
  background: transparent;
  color: var(--text);
}
.ui-btn--ghost:not(:disabled):hover {
  background: var(--surface-3);
}

.ui-btn--inverse {
  background: var(--surface-inverse);
  color: var(--text-on-inverse);
}
.ui-btn--inverse:not(:disabled):hover {
  opacity: 0.88;
}

.ui-btn--danger {
  background: var(--error);
  color: #fff;
}
.ui-btn--danger:not(:disabled):hover {
  filter: brightness(0.92);
}

.ui-btn--link {
  background: transparent;
  color: var(--primary);
  padding-left: 0;
  padding-right: 0;
  min-height: 0;
}
.ui-btn--link:not(:disabled):hover {
  text-decoration: underline;
  text-underline-offset: 3px;
}

.ui-btn__spin {
  width: 15px;
  height: 15px;
  border-radius: 50%;
  border: 2px solid currentColor;
  border-top-color: transparent;
  animation: ui-btn-spin 0.7s linear infinite;
}
@keyframes ui-btn-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>