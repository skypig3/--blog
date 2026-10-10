<script setup>
import SiteHeader from './parts/SiteHeader.vue'
import SiteFooter from './parts/SiteFooter.vue'
import { useUserStore } from '@/stores/user'

const store = useUserStore()

const nav = [
  { name: 'me', label: '个人资料', icon: 'user' },
  { name: 'my-articles', label: '我的文章', icon: 'file-text' },
  { name: 'my-comments', label: '我的评论', icon: 'message-circle' },
  { name: 'my-collects', label: '我的收藏', icon: 'bookmark' }
]
</script>

<template>
  <div class="user-shell">
    <SiteHeader />
    <main class="portal-main">
      <div class="container user-layout">
        <aside class="user-side">
          <div class="user-side__card">
            <UiAvatar :src="store.user?.avatar" :name="store.displayName" :size="52" />
            <div class="user-side__meta">
              <p class="user-side__name">{{ store.displayName }}</p>
              <p class="caption">@{{ store.user?.username }}</p>
            </div>
          </div>
          <nav class="user-nav" aria-label="个人中心">
            <RouterLink
              v-for="item in nav"
              :key="item.name"
              :to="{ name: item.name }"
              class="user-nav__item"
              active-class="is-active"
            >
              <UiIcon :name="item.icon" :size="17" />
              <span>{{ item.label }}</span>
            </RouterLink>
          </nav>
        </aside>
        <section class="user-content">
          <RouterView />
        </section>
      </div>
    </main>
    <SiteFooter />
  </div>
</template>

<style scoped>
.user-shell {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--bg);
}
.portal-main {
  flex: 1;
}
.user-layout {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: var(--sp-xl);
  padding-top: var(--sp-xl);
  padding-bottom: var(--sp-xl);
}
.user-side {
  display: flex;
  flex-direction: column;
  gap: var(--sp-md);
  position: sticky;
  top: calc(var(--nav-h) + 24px);
  align-self: start;
}
.user-side__card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 18px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--r-lg);
}
.user-side__name {
  font-family: var(--font-display);
  font-size: 1.125rem;
  font-weight: var(--display-weight);
  color: var(--text);
  letter-spacing: -0.2px;
}
.user-nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 8px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--r-lg);
}
.user-nav__item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: var(--r-md);
  color: var(--text-muted);
  font-size: 0.9375rem;
}
.user-nav__item:hover {
  background: var(--surface-3);
  color: var(--text);
}
.user-nav__item.is-active {
  background: var(--primary-soft);
  color: var(--primary);
  font-weight: 500;
}
.user-content {
  min-width: 0;
}

@media (max-width: 860px) {
  .user-layout {
    grid-template-columns: 1fr;
  }
  .user-side {
    position: static;
  }
  .user-nav {
    flex-direction: row;
    overflow-x: auto;
  }
  .user-nav__item {
    white-space: nowrap;
  }
}
</style>