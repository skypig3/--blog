<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { adminApi } from '@/api/admin'
import { applyTheme } from '@/utils/theme'
import { toast } from '@/composables/feedback'
import UiIcon from '@/components/ui/UiIcon.vue'
import UiAvatar from '@/components/ui/UiAvatar.vue'
import UiBadge from '@/components/ui/UiBadge.vue'
import UiButton from '@/components/ui/UiButton.vue'
import UiDropdown from '@/components/ui/UiDropdown.vue'
import UiMenuItem from '@/components/ui/UiMenuItem.vue'

const store = useUserStore()
const route = useRoute()
const router = useRouter()

const pendingArticles = ref(0)
const pendingComments = ref(0)

const nav = computed(() => [
  { name: 'admin-dashboard', label: '数据总览', icon: 'grid' },
  { name: 'admin-articles', label: '文章审核', icon: 'file-text', badge: pendingArticles.value },
  { name: 'admin-comments', label: '评论审核', icon: 'message-circle', badge: pendingComments.value },
  { name: 'admin-users', label: '用户管理', icon: 'users' },
  { name: 'admin-taxonomy', label: '分类与标签', icon: 'folder' },
  { name: 'admin-logs', label: '操作记录', icon: 'clipboard' }
])

const pageTitle = computed(() => route.meta.title || '后台管理')

async function loadBadges() {
  try {
    const s = await adminApi.stats()
    pendingArticles.value = s.pendingArticleCount
    pendingComments.value = s.pendingCommentCount
  } catch {
    /* 忽略：侧栏角标不是关键路径 */
  }
}

onMounted(() => {
  // 双保险：路由守卫已经切换过，这里在后台布局真正挂载时再确认一次
  applyTheme(true)
  loadBadges()
})
onBeforeUnmount(() => {
  applyTheme(false)
})
// 切换页面时刷新角标，保证审核后数字跟着变
watch(() => route.fullPath, loadBadges)

async function onLogout() {
  await store.logout()
  toast.success('已退出登录')
  router.push({ name: 'login' })
}
</script>

<template>
  <div class="admin-shell">
    <aside class="admin-side">
      <RouterLink :to="{ name: 'admin-dashboard' }" class="admin-brand">
        <span class="admin-brand__mark"><UiIcon name="spike" :size="18" /></span>
        <span class="admin-brand__name">个人博客 · 后台</span>
      </RouterLink>

      <nav class="admin-nav" aria-label="后台导航">
        <RouterLink
          v-for="item in nav"
          :key="item.name"
          :to="{ name: item.name }"
          class="admin-nav__item"
          active-class="is-active"
        >
          <UiIcon :name="item.icon" :size="17" />
          <span class="admin-nav__label">{{ item.label }}</span>
          <UiBadge v-if="item.badge" variant="warning" size="sm">{{ item.badge }}</UiBadge>
        </RouterLink>
      </nav>

      <div class="admin-side__foot">
        <RouterLink :to="{ name: 'home' }" class="admin-nav__item">
          <UiIcon name="arrow-left" :size="17" />
          <span class="admin-nav__label">返回前台</span>
        </RouterLink>
      </div>
    </aside>

    <div class="admin-body">
      <header class="admin-top">
        <div class="admin-top__title">
          <h1 class="admin-top__h">{{ pageTitle }}</h1>
          <span class="admin-top__crumb">管理后台</span>
        </div>
        <UiDropdown width="210px">
          <template #trigger>
            <button class="admin-user" type="button">
              <UiAvatar :src="store.user?.avatar" :name="store.displayName" :size="28" />
              <span class="admin-user__name">{{ store.displayName }}</span>
              <UiIcon name="chevron-down" :size="14" />
            </button>
          </template>
          <UiMenuItem :to="{ name: 'me' }"><UiIcon name="user" :size="16" />个人资料</UiMenuItem>
          <UiMenuItem :to="{ name: 'home' }"><UiIcon name="arrow-left" :size="16" />返回前台</UiMenuItem>
          <div class="admin-sep"></div>
          <UiMenuItem danger @click="onLogout"><UiIcon name="log-out" :size="16" />退出登录</UiMenuItem>
        </UiDropdown>
      </header>

      <main class="admin-content">
        <RouterView v-slot="{ Component }">
          <component :is="Component" @refresh-badges="loadBadges" />
        </RouterView>
      </main>
    </div>
  </div>
</template>

<style scoped>
.admin-shell {
  min-height: 100%;
  display: flex;
  background: var(--bg);
  color: var(--text-body);
}
.admin-side {
  width: 240px;
  flex-shrink: 0;
  background: var(--surface);
  border-right: 1px solid var(--border);
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  gap: 18px;
  position: sticky;
  top: 0;
  height: 100vh;
}
.admin-brand {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 8px 8px 14px;
  color: var(--text);
  border-bottom: 1px solid var(--border);
}
.admin-brand__mark {
  display: inline-grid;
  place-items: center;
  width: 26px;
  height: 26px;
  border-radius: var(--r-md);
  background: var(--primary);
  color: var(--on-primary);
}
.admin-brand__name {
  font-size: 0.875rem;
  font-weight: 600;
  letter-spacing: -0.2px;
}
.admin-nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
}
.admin-nav__item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 10px;
  border-radius: var(--r-md);
  color: var(--text-muted);
  font-size: 0.875rem;
  font-weight: 500;
  transition: background-color 0.15s ease, color 0.15s ease;
}
.admin-nav__item:hover {
  background: var(--surface-2);
  color: var(--text);
}
.admin-nav__item.is-active {
  background: var(--surface-2);
  color: var(--text);
  box-shadow: inset 0 0 0 1px var(--border-strong);
}
.admin-nav__label {
  flex: 1;
}
.admin-side__foot {
  border-top: 1px solid var(--border);
  padding-top: 10px;
}
.admin-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.admin-top {
  height: var(--nav-h);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--sp-xl);
  border-bottom: 1px solid var(--border);
  background: color-mix(in srgb, var(--bg) 86%, transparent);
  backdrop-filter: blur(8px);
  position: sticky;
  top: 0;
  z-index: 30;
}
.admin-top__title {
  display: flex;
  align-items: baseline;
  gap: 10px;
}
.admin-top__h {
  font-size: 1.125rem;
  font-weight: 600;
  letter-spacing: -0.3px;
  color: var(--text);
}
.admin-top__crumb {
  font-size: 0.75rem;
  color: var(--text-subtle);
}
.admin-user {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px 4px 4px;
  border: 1px solid var(--border);
  border-radius: var(--r-pill);
  background: transparent;
  color: var(--text);
  font-size: 0.8125rem;
}
.admin-user:hover {
  border-color: var(--border-strong);
}
.admin-sep {
  height: 1px;
  background: var(--border);
  margin: 6px 8px;
}
.admin-content {
  padding: var(--sp-xl);
  flex: 1;
}

.hamburger-admin {
  display: none;
}

@media (max-width: 860px) {
  .admin-side {
    width: 64px;
    padding: 12px 8px;
  }
  .admin-brand__name,
  .admin-nav__label,
  .admin-nav__item :deep(.ui-badge) {
    display: none;
  }
  .admin-nav__item {
    justify-content: center;
  }
  .admin-top {
    padding: 0 var(--sp-md);
  }
  .admin-content {
    padding: var(--sp-md);
  }
}
</style>