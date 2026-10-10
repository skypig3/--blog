<script setup>
import { useUserStore } from '@/stores/user'
const store = useUserStore()
</script>

<template>
  <div class="container err">
    <span class="err__code display-1">403</span>
    <h1 class="display-3">没有权限访问这个页面</h1>
    <p class="err__text">
      这个区域只对管理员开放。如果你认为这是误判，请联系站点管理员。
    </p>
    <div class="err__actions">
      <UiButton variant="primary" :to="{ name: 'home' }">回到首页</UiButton>
      <UiButton v-if="store.isLoggedIn" variant="secondary" :to="{ name: 'me' }">个人中心</UiButton>
      <UiButton v-else variant="secondary" :to="{ name: 'login' }">切换账号登录</UiButton>
    </div>
  </div>
</template>

<style scoped>
.err {
  padding: var(--sp-section) var(--sp-lg);
  text-align: center;
  max-width: 620px;
}
.err__code {
  color: var(--primary);
  display: block;
  line-height: 1;
}
.err__text {
  margin: 14px auto 0;
  color: var(--text-muted);
  max-width: 46ch;
  line-height: 1.7;
}
.err__actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-top: var(--sp-xl);
  flex-wrap: wrap;
}
</style>