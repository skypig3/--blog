<script setup>
import { computed } from 'vue'
import UiIcon from './UiIcon.vue'

const props = defineProps({
  modelValue: { type: [String, Number, null], default: '' },
  options: { type: Array, default: () => [] }, // [{ label, value }]
  placeholder: { type: String, default: '全部' },
  disabled: Boolean,
  size: { type: String, default: 'md' }
})
defineEmits(['update:modelValue'])

const normalized = computed(() =>
  props.options.map((o) => (typeof o === 'object' ? o : { label: o, value: o }))
)
</script>

<template>
  <div class="ui-select" :class="[`ui-select--${size}`, { 'is-disabled': disabled }]">
    <select
      :value="modelValue === null || modelValue === undefined ? '' : modelValue"
      :disabled="disabled"
      @change="$emit('update:modelValue', $event.target.value)"
    >
      <option v-if="placeholder" value="">{{ placeholder }}</option>
      <option v-for="o in normalized" :key="o.value" :value="o.value">{{ o.label }}</option>
    </select>
    <UiIcon name="chevron-down" :size="16" class="ui-select__arrow" />
  </div>
</template>

<style scoped>
.ui-select {
  position: relative;
  display: inline-flex;
  align-items: center;
  background: var(--input-bg);
  border: 1px solid var(--border-strong);
  border-radius: var(--r-md);
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}
.ui-select--sm {
  min-height: 34px;
}
.ui-select--md {
  min-height: 40px;
}
.ui-select:focus-within {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--focus-ring);
}
.ui-select.is-disabled {
  opacity: 0.6;
}
.ui-select select {
  appearance: none;
  border: 0;
  outline: none;
  background: transparent;
  color: var(--text);
  font-family: inherit;
  font-size: 0.875rem;
  padding: 0 34px 0 12px;
  width: 100%;
  min-height: inherit;
  cursor: pointer;
}
.ui-select__arrow {
  position: absolute;
  right: 10px;
  color: var(--text-subtle);
  pointer-events: none;
}
/* 深色主题下原生下拉选项的背景跟随系统，这里保证文字可读 */
.ui-select select option {
  background: var(--surface-2);
  color: var(--text);
}
</style>