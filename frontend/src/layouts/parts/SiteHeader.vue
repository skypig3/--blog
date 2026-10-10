<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { taxonomyApi } from '@/api/site'
import { toast } from '@/composables/feedback'

const store = useUserStore()
const router = useRouter()
const route = useRoute()

const categories = ref([])
const tags = ref([])
const keyword = ref('')
const mobileOpen = ref(false)

onMounted(async () => {
  try {
    const [cats, tgs] = await Promise.all([taxonomyApi.categories(), taxonomyApi.tags()])
    categories.value = cats || []
    tags.value = tgs || []
  } catch {
    /* 分类标签加载失败不影响主流程 */
  }
})

// 路由变化时收起移动端抽屉
watch(
  () => route.fullPath,
  () => {
    mobileOpen.value = false
  }
)

function doSearch() {
  const q = keyword.value.trim()
  router.push({ name: 'search', query: q ? { q } : {} })
  mobileOpen.value = false
}

async function onLogout() {
  await store.logout()
  toast.success('已退出登录')
  if (route.meta.requiresAuth) router.push({ name: 'home' })
}
</script>

<template>
  <header class="site-nav">
    <div class="container site-nav__inner">
      <RouterLink to="/" class="brand" aria-label="回到首页">
        <UiIcon name="spike" :size="22" class="brand__mark" />
        <span class="brand__name">个人博客</span>
      </RouterLink>

      <nav class="site-nav__links" aria-label="主导航">
        <RouterLink to="/" exact-active-class="is-active" class="nav-link">首页</RouterLink>
        <RouterLink to="/articles" active-class="is-active" class="nav-link">全部文章</RouterLink>

        <UiDropdown v-if="categories.length" align="start" width="240px">
          <template #trigger>
            <span class="nav-link nav-link--drop">
              分类<UiIcon name="chevron-down" :size="14" />
            </span>
          </template>
          <RouterLink
            v-for="c in categories"
            :key="c.id"
            class="nav-drop__item"
            :to="{ name: 'category', params: { id: c.id } }"
          >
            <span>{{ c.name }}</span>
            <span class="nav-drop__count">{{ c.articleCount }}</span>
          </RouterLink>
        </UiDropdown>

        <UiDropdown v-if="tags.length" align="start" width="280px">
          <template #trigger>
            <span class="nav-link nav-link--drop">
              标签<UiIcon name="chevron-down" :size="14" />
            </span>
          </template>
          <div class="nav-drop__tags">
            <RouterLink
              v-for="t in tags"
              :key="t.id"
              class="nav-drop__tag"
              :to="{ name: 'tag', params: { id: t.id } }"
            >
              {{ t.name }}<span class="nav-drop__count">{{ t.articleCount }}</span>
            </RouterLink>
          </div>
        </UiDropdown>
      </nav>

      <div class="site-nav__right">
        <div class="site-search">
          <UiInput
            v-model="keyword"
            icon="search"
            size="sm"
            placeholder="搜索标题…"
            @enter="doSearch"
          />
        </div>

        <template v-if="store.isLoggedIn">
          <UiButton size="sm" variant="primary" :to="{ name: 'editor' }" icon="edit">
            写文章
          </UiButton>
          <UiDropdown width="220px">
            <template #trigger>
              <button class="user-chip" type="button">
                <UiAvatar :src="store.user?.avatar" :name="store.displayName" :size="32" />
                <span class="user-chip__name">{{ store.displayName }}</span>
                <UiIcon name="chevron-down" :size="14" />
              </button>
            </template>
            <UiMenuItem :to="{ name: 'me' }">
              <UiIcon name="user" :size="16" />个人资料
            </UiMenuItem>
            <UiMenuItem :to="{ name: 'my-articles' }">
              <UiIcon name="file-text" :size="16" />我的文章
            </UiMenuItem>
            <UiMenuItem :to="{ name: 'my-comments' }">
              <UiIcon name="message-circle" :size="16" />我的评论
            </UiMenuItem>
            <UiMenuItem :to="{ name: 'my-collects' }">
              <UiIcon name="bookmark" :size="16" />我的收藏
            </UiMenuItem>
            <UiMenuItem v-if="store.isAdmin" :to="{ name: 'admin-dashboard' }">
              <UiIcon name="shield" :size="16" />后台管理
            </UiMenuItem>
            <div class="nav-drop__sep"></div>
            <UiMenuItem danger @click="onLogout">
              <UiIcon name="log-out" :size="16" />退出登录
            </UiMenuItem>
          </UiDropdown>
        </template>
        <template v-else>
          <RouterLink :to="{ name: 'login' }" class="nav-link">登录</RouterLink>
          <UiButton size="sm" variant="primary" :to="{ name: 'register' }">注册</UiButton>
        </template>

        <button class="hamburger" type="button" aria-label="菜单" @click="mobileOpen = true">
          <UiIcon name="menu" :size="22" />
        </button>
      </div>
    </div>

    <!-- 移动端全屏抽屉 -->
    <Transition name="fade">
      <div v-if="mobileOpen" class="mobile-sheet">
        <div class="mobile-sheet__head">
          <span class="brand">
            <UiIcon name="spike" :size="22" class="brand__mark" />
            <span class="brand__name">个人博客</span>
          </span>
          <button class="hamburger" type="button" aria-label="关闭" @click="mobileOpen = false">
            <UiIcon name="x" :size="22" />
          </button>
        </div>
        <UiInput v-model="keyword" icon="search" placeholder="搜索文章标题…" @enter="doSearch" />
        <nav class="mobile-sheet__links">
          <RouterLink to="/" class="mobile-link">首页</RouterLink>
          <RouterLink to="/articles" class="mobile-link">全部文章</RouterLink>
          <RouterLink v-for="c in categories" :key="c.id" class="mobile-link mobile-link--sub"
            :to="{ name: 'category', params: { id: c.id } }">
            分类 · {{ c.name }}
          </RouterLink>
        </nav>
        <div class="mobile-sheet__auth">
          <template v-if="store.isLoggedIn">
            <UiButton block variant="primary" :to="{ name: 'editor' }" icon="edit">写文章</UiButton>
            <UiButton block variant="secondary" :to="{ name: 'me' }">个人中心</UiButton>
            <UiButton v-if="store.isAdmin" block variant="ghost" :to="{ name: 'admin-dashboard' }">
              后台管理
            </UiButton>
            <UiButton block variant="ghost" @click="onLogout">退出登录</UiButton>
          </template>
          <template v-else>
            <UiButton block variant="primary" :to="{ name: 'login' }">登录</UiButton>
            <UiButton block variant="secondary" :to="{ name: 'register' }">注册</UiButton>
          </template>
        </div>
      </div>
    </Transition>
  </header>
</template>

<style scoped>
.site-nav {
  position: sticky;
  top: 0;
  z-index: 50;
  background: color-mix(in srgb, var(--bg) 88%, transparent);
  backdrop-filter: saturate(1.2) blur(10px);
  border-bottom: 1px solid var(--border);
}
.site-nav__inner {
  height: var(--nav-h);
  display: flex;
  align-items: center;
  gap: var(--sp-lg);
}
.brand {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  color: var(--text);
}
.brand__mark {
  color: var(--primary);
}
.brand__name {
  font-family: var(--font-display);
  font-size: 1.375rem;
  font-weight: 500;
  letter-spacing: -0.4px;
}
.site-nav__links {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
}
.nav-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 8px 12px;
  border-radius: var(--r-md);
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--text-muted);
  white-space: nowrap;
  transition: color 0.15s ease, background-color 0.15s ease;
}
.nav-link:hover,
.nav-link.is-active {
  color: var(--text);
  background: var(--surface-3);
}
.nav-link--drop {
  cursor: pointer;
}
.nav-drop__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 9px 12px;
  border-radius: var(--r-sm);
  color: var(--text-body);
  font-size: 0.875rem;
}
.nav-drop__item:hover {
  background: var(--surface-3);
  color: var(--text);
}
.nav-drop__count {
  font-size: 0.75rem;
  color: var(--text-subtle);
}
.nav-drop__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 8px;
}
.nav-drop__tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 10px;
  border-radius: var(--r-pill);
  background: var(--surface-2);
  border: 1px solid var(--border);
  font-size: 0.8125rem;
  color: var(--text-body);
}
.nav-drop__tag:hover {
  border-color: var(--primary);
  color: var(--text);
}
.nav-drop__sep {
  height: 1px;
  background: var(--border);
  margin: 6px 8px;
}
.site-nav__right {
  display: flex;
  align-items: center;
  gap: var(--sp-sm);
}
.site-search {
  width: 200px;
}
.user-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px 4px 4px;
  border: 1px solid var(--border);
  border-radius: var(--r-pill);
  background: transparent;
  color: var(--text);
  font-size: 0.875rem;
}
.user-chip:hover {
  border-color: var(--border-strong);
}
.user-chip__name {
  max-width: 96px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.hamburger {
  display: none;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border: 1px solid var(--border);
  border-radius: var(--r-md);
  background: transparent;
  color: var(--text);
}

/* 移动端抽屉 */
.mobile-sheet {
  position: fixed;
  inset: 0;
  z-index: 200;
  background: var(--bg);
  padding: 16px 20px 32px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.mobile-sheet__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.mobile-sheet__links {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.mobile-link {
  padding: 12px 8px;
  border-bottom: 1px solid var(--border);
  color: var(--text);
  font-size: 1.0625rem;
  font-family: var(--font-display);
  letter-spacing: -0.2px;
}
.mobile-link--sub {
  font-size: 0.9375rem;
  font-family: var(--font-body);
  color: var(--text-muted);
  padding-left: 18px;
}
.mobile-sheet__auth {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: auto;
}

@media (max-width: 1024px) {
  .site-search {
    width: 150px;
  }
}
@media (max-width: 960px) {
  .site-nav__links,
  .site-search,
  .user-chip__name {
    display: none;
  }
  .hamburger {
    display: inline-flex;
  }
  .site-nav__right {
    margin-left: auto;
  }
}
</style>