<script setup>
defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '' },
  rows: { type: [String, Number], default: 5 },
  disabled: Boolean,
  invalid: Boolean,
  maxlength: { type: [String, Number], default: undefined }
})
defineEmits(['update:modelValue'])
</script>

<template>
  <div class="ui-textarea" :class="{ 'is-invalid': invalid, 'is-disabled': disabled }">
    <textarea
      :value="modelValue"
      :placeholder="placeholder"
      :rows="rows"
      :disabled="disabled"
      :maxlength="maxlength"
      @input="$emit('update:modelValue', $event.target.value)"
    ></textarea>
  </div>
</template>

<style scoped>
.ui-textarea {
  background: var(--input-bg);
  border: 1px solid var(--border-strong);
  border-radius: var(--r-md);
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}
.ui-textarea:focus-within {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--focus-ring);
}
.ui-textarea.is-invalid {
  border-color: var(--error);
}
.ui-textarea.is-disabled {
  opacity: 0.6;
}
.ui-textarea textarea {
  display: block;
  width: 100%;
  border: 0;
  outline: none;
  resize: vertical;
  background: transparent;
  color: var(--text);
  font-family: inherit;
  font-size: 0.9375rem;
  line-height: 1.6;
  padding: 10px 14px;
}
.ui-textarea textarea::placeholder {
  color: var(--text-subtle);
}
</style>