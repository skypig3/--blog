<script setup>
import { toasts, dismiss, dialog } from '@/composables/feedback'
import UiIcon from './UiIcon.vue'
import UiModal from './UiModal.vue'
import UiButton from './UiButton.vue'
import UiTextarea from './UiTextarea.vue'
import { ref, watch } from 'vue'

const ICON = { success: 'check-circle', error: 'alert-circle', warning: 'alert-triangle', info: 'info' }

const inputRef = ref('')
watch(
  () => dialog.open,
  (v) => {
    if (v && dialog.mode === 'prompt') inputRef.value = dialog.inputValue || ''
  }
)

function cancel() {
  dialog._close(dialog.mode === 'prompt' ? null : false)
}
function accept() {
  if (dialog.mode === 'prompt') {
    const val = (inputRef.value || '').trim()
    if (dialog.required && !val) return
    dialog._close(val)
  } else {
    dialog._close(true)
  }
}
</script>

<template>
  <Teleport to="body">
    <!-- 消息提示 -->
    <div class="fb-toasts" aria-live="polite">
      <TransitionGroup name="toast">
        <div v-for="t in toasts" :key="t.id" class="fb-toast" :class="`fb-toast--${t.type}`">
          <UiIcon :name="ICON[t.type] || 'info'" :size="18" />
          <span class="fb-toast__msg">{{ t.message }}</span>
          <button class="fb-toast__x" type="button" aria-label="关闭" @click="dismiss(t.id)">
            <UiIcon name="x" :size="14" />
          </button>
        </div>
      </TransitionGroup>
    </div>

    <!-- 确认框 / 输入框 -->
    <UiModal
      :model-value="dialog.open"
      :title="dialog.title"
      :size="dialog.mode === 'prompt' ? 'md' : 'sm'"
      @update:model-value="!$event && cancel()"
    >
      <p v-if="dialog.content" class="fb-dialog__text">{{ dialog.content }}</p>
      <template v-if="dialog.mode === 'prompt'">
        <label v-if="dialog.inputLabel" class="fb-dialog__label">
          {{ dialog.inputLabel }}<span v-if="dialog.required" class="fb-dialog__req">*</span>
        </label>
        <UiTextarea v-model="inputRef" :rows="dialog.inputRows" :placeholder="dialog.inputPlaceholder" />
      </template>
      <template #footer>
        <UiButton variant="ghost" @click="cancel">{{ dialog.cancelText }}</UiButton>
        <UiButton :variant="dialog.danger ? 'danger' : 'primary'" @click="accept">
          {{ dialog.confirmText }}
        </UiButton>
      </template>
    </UiModal>
  </Teleport>
</template>

<style scoped>
.fb-toasts {
  position: fixed;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 2000;
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: center;
  pointer-events: none;
}
.fb-toast {
  display: flex;
  align-items: center;
  gap: 10px;
  pointer-events: auto;
  padding: 11px 14px;
  min-width: 220px;
  max-width: 90vw;
  background: var(--bg);
  border: 1px solid var(--border-strong);
  border-radius: var(--r-md);
  box-shadow: var(--shadow-pop);
  color: var(--text);
  font-size: 0.875rem;
}
.fb-toast--success {
  color: var(--success);
  border-color: color-mix(in srgb, var(--success) 45%, var(--border-strong));
}
.fb-toast--error {
  color: var(--error);
  border-color: color-mix(in srgb, var(--error) 45%, var(--border-strong));
}
.fb-toast--warning {
  color: var(--warning);
  border-color: color-mix(in srgb, var(--warning) 45%, var(--border-strong));
}
.fb-toast__msg {
  flex: 1;
  color: var(--text);
}
.fb-toast__x {
  border: 0;
  background: transparent;
  color: var(--text-subtle);
  display: inline-flex;
  padding: 2px;
}
.fb-toast__x:hover {
  color: var(--text);
}

.fb-dialog__text {
  color: var(--text-body);
  font-size: 0.9375rem;
  line-height: 1.6;
  white-space: pre-wrap;
}
.fb-dialog__label {
  display: block;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--text);
  margin: 16px 0 6px;
}
.fb-dialog__req {
  color: var(--error);
  margin-left: 3px;
}

.toast-enter-active,
.toast-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>