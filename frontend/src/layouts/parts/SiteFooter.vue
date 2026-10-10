<script setup>
import { useUserStore } from '@/stores/user'
import { useRoute } from 'vue-router'

const store = useUserStore()
const route = useRoute()
const year = new Date().getFullYear()
</script>

<template>
  <footer class="site-foot">
    <div class="container site-foot__grid">
      <div class="site-foot__brand">
        <span class="brand">
          <UiIcon name="spike" :size="22" class="brand__mark" />
          <span class="brand__name">个人博客</span>
        </span>
        <p class="site-foot__desc">
          写下来，才算真正想清楚。一个记录技术笔记、项目实战与生活随笔的地方，
          所有内容经管理员审核后公开。
        </p>
      </div>

      <div class="site-foot__col">
        <h4 class="site-foot__title">浏览</h4>
        <RouterLink to="/" class="site-foot__link">首页</RouterLink>
        <RouterLink to="/articles" class="site-foot__link">全部文章</RouterLink>
        <RouterLink :to="{ name: 'search' }" class="site-foot__link">搜索</RouterLink>
      </div>

      <div class="site-foot__col">
        <h4 class="site-foot__title">我的</h4>
        <template v-if="store.isLoggedIn">
          <RouterLink :to="{ name: 'editor' }" class="site-foot__link">写文章</RouterLink>
          <RouterLink :to="{ name: 'my-articles' }" class="site-foot__link">我的文章</RouterLink>
          <RouterLink :to="{ name: 'my-collects' }" class="site-foot__link">我的收藏</RouterLink>
        </template>
        <template v-else>
          <RouterLink :to="{ name: 'login' }" class="site-foot__link">登录</RouterLink>
          <RouterLink :to="{ name: 'register' }" class="site-foot__link">注册</RouterLink>
        </template>
      </div>

      <div class="site-foot__col">
        <h4 class="site-foot__title">热门排序规则</h4>
        <p class="site-foot__note">
          score = 阅读×1 + 点赞×2 + 收藏×3 + 评论×1，取最近 30 天的已发布文章前 10 篇。
        </p>
      </div>
    </div>

    <div class="container site-foot__bottom">
      <span>© {{ year }} 个人博客系统 · 课程项目</span>
      <span>Spring Boot + SQLite + Vue 3</span>
    </div>
  </footer>
</template>

<style scoped>
.site-foot {
  background: var(--surface-inverse);
  color: var(--text-on-inverse-soft);
  padding: var(--sp-xxl) 0 var(--sp-lg);
  margin-top: var(--sp-section);
}
.site-foot__grid {
  display: grid;
  grid-template-columns: 1.6fr 1fr 1fr 1.4fr;
  gap: var(--sp-xl);
  padding-bottom: var(--sp-xl);
}
.brand {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  color: var(--text-on-inverse);
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
.site-foot__desc {
  margin-top: 14px;
  font-size: 0.875rem;
  line-height: 1.7;
  max-width: 34ch;
}
.site-foot__col {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.site-foot__title {
  font-size: 0.75rem;
  font-weight: 500;
  letter-spacing: 1.2px;
  text-transform: uppercase;
  color: var(--text-on-inverse);
  margin-bottom: 2px;
}
.site-foot__link {
  font-size: 0.875rem;
  color: var(--text-on-inverse-soft);
}
.site-foot__link:hover {
  color: var(--text-on-inverse);
}
.site-foot__note {
  font-size: 0.8125rem;
  line-height: 1.7;
  margin: 0;
}
.site-foot__bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: var(--sp-lg);
  border-top: 1px solid rgba(255, 255, 255, 0.09);
  font-size: 0.8125rem;
}

@media (max-width: 900px) {
  .site-foot__grid {
    grid-template-columns: 1fr 1fr;
  }
}
@media (max-width: 560px) {
  .site-foot__grid {
    grid-template-columns: 1fr;
  }
  .site-foot__bottom {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>