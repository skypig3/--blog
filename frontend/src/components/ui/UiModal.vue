<script setup>
import { watch, onBeforeUnmount } from 'vue'
import UiIcon from './UiIcon.vue'

const props = defineProps({
  modelValue: Boolean,
  title: { type: String, default: '' },
  size: { type: String, default: 'md' }, // sm | md | lg
  closeOnOverlay: { type: Boolean, default: true }
})
const emit = defineEmits(['update:modelValue'])

function close() {
  emit('update:modelValue', false)
}

function onKey(e) {
  if (e.key === 'Escape') close()
}

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      document.addEventListener('keydown', onKey)
      document.body.style.overflow = 'hidden'
    } else {
      document.removeEventListener('keydown', onKey)
      document.body.style.overflow = ''
    }
  }
)

onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKey)
  document.body.style.overflow = ''
})
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="modelValue" class="ui-modal" @click.self="closeOnOverlay && close()">
        <div class="ui-modal__panel" :class="`ui-modal__panel--${size}`" role="dialog" aria-modal="true">
          <header v-if="title || $slots.header" class="ui-modal__head">
            <slot name="header">
              <h3 class="ui-modal__title">{{ title }}</h3>
            </slot>
            <button class="ui-modal__close" type="button" aria-label="关闭" @click="close">
              <UiIcon name="x" :size="18" />
            </button>
          </header>
          <div class="ui-modal__body">
            <slot />
          </div>
          <footer v-if="$slots.footer" class="ui-modal__foot">
            <slot name="footer" />
          </footer>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.ui-modal {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 8vh 16px 16px;
  background: var(--overlay);
  backdrop-filter: blur(2px);
}
.ui-modal__panel {
  width: 100%;
  background: var(--bg);
  border: 1px solid var(--border-strong);
  border-radius: var(--r-lg);
  box-shadow: var(--shadow-pop);
  max-height: 84vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.ui-modal__panel--sm {
  max-width: 420px;
}
.ui-modal__panel--md {
  max-width: 560px;
}
.ui-modal__panel--lg {
  max-width: 820px;
}
.ui-modal__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 22px;
  border-bottom: 1px solid var(--border);
}
.ui-modal__title {
  font-family: var(--font-display);
  font-weight: var(--display-weight);
  font-size: 1.25rem;
  letter-spacing: -0.3px;
  color: var(--text);
}
.ui-modal__close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 0;
  border-radius: var(--r-md);
  background: transparent;
  color: var(--text-muted);
}
.ui-modal__close:hover {
  background: var(--surface-3);
  color: var(--text);
}
.ui-modal__body {
  padding: 22px;
  overflow-y: auto;
}
.ui-modal__foot {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 22px;
  border-top: 1px solid var(--border);
}

.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}
.modal-enter-active .ui-modal__panel,
.modal-leave-active .ui-modal__panel {
  transition: transform 0.2s ease;
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
.modal-enter-from .ui-modal__panel,
.modal-leave-to .ui-modal__panel {
  transform: translateY(-12px);
}
</style>