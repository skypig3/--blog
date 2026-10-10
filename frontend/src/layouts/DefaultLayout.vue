<script setup>
import SiteHeader from './parts/SiteHeader.vue'
import SiteFooter from './parts/SiteFooter.vue'
</script>

<template>
  <div class="portal-shell">
    <SiteHeader />
    <main class="portal-main">
      <RouterView />
    </main>
    <SiteFooter />
  </div>
</template>

<style scoped>
.portal-shell {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--bg);
}
.portal-main {
  flex: 1;
}
</style>