<script setup>
defineProps({
  label: { type: String, default: '' },
  hint: { type: String, default: '' },
  error: { type: String, default: '' },
  required: Boolean
})
</script>

<template>
  <label class="ui-field">
    <span v-if="label" class="ui-field__label">
      {{ label }}<span v-if="required" class="ui-field__req">*</span>
    </span>
    <slot />
    <span v-if="error" class="ui-field__msg ui-field__msg--error">{{ error }}</span>
    <span v-else-if="hint" class="ui-field__msg">{{ hint }}</span>
  </label>
</template>

<style scoped>
.ui-field {
  display: block;
}
.ui-field__label {
  display: block;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--text);
  margin-bottom: 6px;
}
.ui-field__req {
  color: var(--error);
  margin-left: 3px;
}
.ui-field__msg {
  display: block;
  font-size: 0.8125rem;
  color: var(--text-subtle);
  margin-top: 6px;
}
.ui-field__msg--error {
  color: var(--error);
}
</style>