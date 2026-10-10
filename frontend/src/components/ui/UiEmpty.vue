<script setup>
import UiIcon from './UiIcon.vue'

defineProps({
  icon: { type: String, default: 'inbox' },
  title: { type: String, default: '暂无内容' },
  description: { type: String, default: '' }
})
</script>

<template>
  <div class="ui-empty">
    <div class="ui-empty__icon">
      <UiIcon :name="icon === 'inbox' ? 'feather' : icon" :size="26" />
    </div>
    <p class="ui-empty__title">{{ title }}</p>
    <p v-if="description" class="ui-empty__desc">{{ description }}</p>
    <div v-if="$slots.default" class="ui-empty__action">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.ui-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 56px 24px;
  color: var(--text-muted);
}
.ui-empty__icon {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: var(--surface-3);
  border: 1px solid var(--border);
  color: var(--text-subtle);
  margin-bottom: 16px;
}
.ui-empty__title {
  font-family: var(--font-display);
  font-size: 1.125rem;
  font-weight: var(--display-weight);
  color: var(--text);
  letter-spacing: -0.2px;
}
.ui-empty__desc {
  margin-top: 6px;
  font-size: 0.875rem;
  max-width: 42ch;
}
.ui-empty__action {
  margin-top: 18px;
}
</style>