<script setup>
import UiIcon from './UiIcon.vue'

defineProps({
  modelValue: { type: [String, Number], default: '' },
  type: { type: String, default: 'text' },
  placeholder: { type: String, default: '' },
  icon: { type: String, default: '' },
  disabled: Boolean,
  invalid: Boolean,
  autocomplete: { type: String, default: 'off' },
  maxlength: { type: [String, Number], default: undefined },
  size: { type: String, default: 'md' }
})
defineEmits(['update:modelValue', 'enter'])
</script>

<template>
  <div class="ui-input" :class="[`ui-input--${size}`, { 'is-invalid': invalid, 'is-disabled': disabled }]">
    <UiIcon v-if="icon" :name="icon" :size="18" class="ui-input__icon" />
    <input
      :value="modelValue"
      :type="type"
      :placeholder="placeholder"
      :disabled="disabled"
      :autocomplete="autocomplete"
      :maxlength="maxlength"
      @input="$emit('update:modelValue', $event.target.value)"
      @keyup.enter="$emit('enter')"
    />
    <slot name="suffix" />
  </div>
</template>

<style scoped>
.ui-input {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--input-bg);
  border: 1px solid var(--border-strong);
  border-radius: var(--r-md);
  padding: 0 14px;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}
.ui-input--sm {
  min-height: 34px;
}
.ui-input--md {
  min-height: 40px;
}
.ui-input--lg {
  min-height: 48px;
}
.ui-input:focus-within {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--focus-ring);
}
.ui-input.is-invalid {
  border-color: var(--error);
}
.ui-input.is-disabled {
  opacity: 0.6;
}
.ui-input__icon {
  color: var(--text-subtle);
}
.ui-input input {
  flex: 1;
  min-width: 0;
  border: 0;
  outline: none;
  background: transparent;
  color: var(--text);
  font-family: inherit;
  font-size: 0.9375rem;
  padding: 0;
}
.ui-input input::placeholder {
  color: var(--text-subtle);
}
</style>