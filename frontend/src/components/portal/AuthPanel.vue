<script setup>
defineProps({
  eyebrow: { type: String, default: '' },
  title: { type: String, default: '' },
  subtitle: { type: String, default: '' }
})
</script>

<template>
  <div class="auth">
    <div class="auth__inner">
      <div class="auth__brand">
        <UiIcon name="spike" :size="26" class="auth__mark" />
        <span class="auth__brand-name">个人博客</span>
      </div>
      <div class="auth__card">
        <span v-if="eyebrow" class="eyebrow">{{ eyebrow }}</span>
        <h1 class="display-3 auth__title">{{ title }}</h1>
        <p v-if="subtitle" class="auth__sub">{{ subtitle }}</p>
        <slot />
      </div>
      <div class="auth__foot">
        <slot name="footer" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth {
  padding: var(--sp-section) 0;
  min-height: calc(100vh - var(--nav-h));
  display: flex;
  align-items: flex-start;
  justify-content: center;
}
.auth__inner {
  width: 100%;
  max-width: 460px;
  padding: 0 var(--sp-lg);
}
.auth__brand {
  display: flex;
  align-items: center;
  gap: 9px;
  justify-content: center;
  margin-bottom: var(--sp-lg);
}
.auth__mark {
  color: var(--primary);
}
.auth__brand-name {
  font-family: var(--font-display);
  font-size: 1.375rem;
  font-weight: 500;
  letter-spacing: -0.4px;
  color: var(--text);
}
.auth__card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--r-xl);
  padding: var(--sp-xl);
}
.auth__title {
  margin: 10px 0 6px;
}
.auth__sub {
  font-size: 0.9375rem;
  color: var(--text-muted);
  margin-bottom: var(--sp-lg);
  line-height: 1.6;
}
.auth__foot {
  margin-top: var(--sp-lg);
  text-align: center;
  font-size: 0.875rem;
  color: var(--text-muted);
}
</style>