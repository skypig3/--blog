<script setup>
import { ref, onMounted } from 'vue'
import { articleApi } from '@/api/articles'
import { taxonomyApi } from '@/api/site'
import { useUserStore } from '@/stores/user'
import ArticleCard from '@/components/portal/ArticleCard.vue'
import UiSkeleton from '@/components/ui/UiSkeleton.vue'

const store = useUserStore()

const latest = ref([])
const latestTotal = ref(0)
const current = ref(1)
const size = 6
const hot = ref([])
const categories = ref([])
const tags = ref([])
const loading = ref(true)

async function loadLatest() {
  loading.value = true
  try {
    const page = await articleApi.list({ current: current.value, size })
    latest.value = page.records
    latestTotal.value = page.total
  } finally {
    loading.value = false
  }
}

function onPage(p) {
  current.value = p
  loadLatest()
  document.getElementById('latest')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

onMounted(async () => {
  const [cats, tgs, hotList] = await Promise.all([
    taxonomyApi.categories().catch(() => []),
    taxonomyApi.tags().catch(() => []),
    articleApi.hot().catch(() => [])
  ])
  categories.value = cats || []
  tags.value = tgs || []
  hot.value = hotList || []
  await loadLatest()
})
</script>

<template>
  <div class="home">
    <!-- Hero -->
    <section class="hero">
      <div class="container hero__grid">
        <div class="hero__text">
          <span class="eyebrow">个人博客 · 写作与分享</span>
          <h1 class="display-1 hero__h1">把想法写下来，<br />才算真正想清楚。</h1>
          <p class="hero__sub">
            这里收集技术笔记、项目复盘与生活随笔。注册后即可创作投稿，
            所有内容经管理员审核后公开，游客可以直接浏览已发布文章。
          </p>
          <div class="hero__actions">
            <UiButton variant="primary" size="lg" :to="{ name: 'articles' }" icon-right="arrow-right">
              开始阅读
            </UiButton>
            <UiButton
              variant="secondary"
              size="lg"
              :to="store.isLoggedIn ? { name: 'editor' } : { name: 'login' }"
              icon="edit"
            >
              写一篇文章
            </UiButton>
          </div>
          <p class="hero__note">
            <UiIcon name="user" :size="15" /> 已注册 {{ categories.length }} 个分类 ·
            {{ tags.length }} 个标签 · 内容真实存于 SQLite
          </p>
        </div>

        <div class="hero__aside">
          <div class="codewin">
            <div class="codewin__bar">
              <span class="codewin__dot" style="background: #e06c5a"></span>
              <span class="codewin__dot" style="background: #e8a55a"></span>
              <span class="codewin__dot" style="background: #5db872"></span>
              <span class="codewin__file">post.md</span>
            </div>
            <pre class="codewin__code"><code><span class="c-h">## 关于这个博客</span>

- Vue 3 负责界面，试着重写的第二版
- Spring Boot 提供接口，Session 认证
- SQLite 保存文章、评论与审核记录

<span class="c-c">```js</span>
const idea = <span class="c-s">'写下来才算想清楚'</span>
<span class="c-k">await</span> publish(idea) <span class="c-m">// 待审核 → 公开</span>
<span class="c-c">```</span></code></pre>
          </div>
        </div>
      </div>
    </section>

    <!-- 最新 + 侧栏 -->
    <section id="latest" class="container home__body">
      <div class="home__main">
        <div class="home__head">
          <h2 class="display-3">最新文章</h2>
          <RouterLink class="home__more" :to="{ name: 'articles' }">
            查看全部<UiIcon name="arrow-right" :size="15" />
          </RouterLink>
        </div>

        <template v-if="loading">
          <div class="home__grid">
            <UiSkeleton v-for="i in 4" :key="i" height="320px" radius="12px" />
          </div>
        </template>
        <template v-else-if="latest.length">
          <div class="home__grid">
            <ArticleCard v-for="a in latest" :key="a.id" :article="a" />
          </div>
          <div class="home__pager">
            <UiPagination :current="current" :total="latestTotal" :size="size" @change="onPage" />
          </div>
        </template>
        <UiEmpty v-else title="还没有文章" description="等第一篇文章通过审核后，就会出现在这里。">
          <UiButton v-if="store.isLoggedIn" variant="primary" :to="{ name: 'editor' }">
            去写第一篇
          </UiButton>
        </UiEmpty>
      </div>

      <aside class="home__aside">
        <div class="panel">
          <h3 class="panel__title">热门文章</h3>
          <ol v-if="hot.length" class="hotlist">
            <li v-for="(a, i) in hot" :key="a.id">
              <RouterLink class="hotlist__item" :to="{ name: 'article-detail', params: { id: a.id } }">
                <span class="hotlist__rank" :class="{ 'is-top': i < 3 }">{{ i + 1 }}</span>
                <span class="hotlist__text">
                  <span class="hotlist__title clamp-2">{{ a.title }}</span>
                  <span class="hotlist__meta">
                    <UiIcon name="eye" :size="13" />{{ a.viewCount }}
                    <UiIcon name="heart" :size="13" />{{ a.likeCount }}
                  </span>
                </span>
              </RouterLink>
            </li>
          </ol>
          <UiEmpty v-else title="暂无热门" description="最近 30 天内还没有已发布文章。" />
          <p class="panel__note">
            热门规则：阅读×1 + 点赞×2 + 收藏×3 + 评论×1，取最近 30 天已发布文章前 10 篇。
          </p>
        </div>

        <div class="panel">
          <h3 class="panel__title">分类</h3>
          <div class="cats">
            <RouterLink
              v-for="c in categories"
              :key="c.id"
              class="cats__item"
              :to="{ name: 'category', params: { id: c.id } }"
            >
              <span>{{ c.name }}</span>
              <span class="cats__count">{{ c.articleCount }}</span>
            </RouterLink>
          </div>
        </div>

        <div class="panel">
          <h3 class="panel__title">标签</h3>
          <div class="tags">
            <RouterLink
              v-for="t in tags"
              :key="t.id"
              class="tags__item"
              :to="{ name: 'tag', params: { id: t.id } }"
            >
              {{ t.name }}<span class="tags__count">{{ t.articleCount }}</span>
            </RouterLink>
          </div>
        </div>
      </aside>
    </section>

    <!-- CTA -->
    <section class="container">
      <div class="cta">
        <div>
          <h2 class="cta__h">有想写的东西了吗？</h2>
          <p class="cta__p">注册一个账号，把草稿存下来，提交后经管理员审核即可公开。</p>
        </div>
        <UiButton
          variant="inverse"
          size="lg"
          :to="store.isLoggedIn ? { name: 'editor' } : { name: 'register' }"
        >
          {{ store.isLoggedIn ? '开始写作' : '注册账号' }}
        </UiButton>
      </div>
    </section>
  </div>
</template>

<style scoped>
.home {
  background: var(--bg);
}

/* Hero */
.hero {
  padding: var(--sp-section) 0 calc(var(--sp-section) * 0.7);
}
.hero__grid {
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  gap: var(--sp-xxl);
  align-items: center;
}
.hero__h1 {
  margin: 18px 0;
}
.hero__sub {
  font-size: 1.0625rem;
  line-height: 1.7;
  color: var(--text-muted);
  max-width: 46ch;
}
.hero__actions {
  display: flex;
  gap: 12px;
  margin-top: 28px;
  flex-wrap: wrap;
}
.hero__note {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  margin-top: 22px;
  font-size: 0.8125rem;
  color: var(--text-subtle);
}

.codewin {
  background: var(--surface-inverse);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: var(--r-xl);
  overflow: hidden;
  box-shadow: var(--shadow-pop);
}
.codewin__bar {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 12px 16px;
  background: var(--surface-inverse-2);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}
.codewin__dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}
.codewin__file {
  margin-left: 10px;
  font-family: var(--font-mono);
  font-size: 0.75rem;
  color: var(--text-on-inverse-soft);
}
.codewin__code {
  margin: 0;
  padding: 22px;
  font-family: var(--font-mono);
  font-size: 0.8125rem;
  line-height: 1.75;
  color: var(--text-on-inverse);
  overflow-x: auto;
}
.c-h {
  color: #e8a55a;
}
.c-c {
  color: var(--text-on-inverse-soft);
}
.c-s {
  color: #8fd6a0;
}
.c-k {
  color: #9aa8ff;
}
.c-m {
  color: var(--text-on-inverse-soft);
}

/* 主体 */
.home__body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 330px;
  gap: var(--sp-xl);
  padding-top: var(--sp-xl);
  padding-bottom: var(--sp-xl);
}
.home__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: var(--sp-lg);
}
.home__more {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 0.875rem;
  color: var(--primary);
}
.home__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--sp-lg);
}
.home__pager {
  margin-top: var(--sp-xl);
}
.home__aside {
  display: flex;
  flex-direction: column;
  gap: var(--sp-lg);
  position: sticky;
  top: calc(var(--nav-h) + 24px);
  align-self: start;
}
.panel {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--r-lg);
  padding: 22px;
}
.panel__title {
  font-family: var(--font-display);
  font-size: 1.125rem;
  font-weight: var(--display-weight);
  color: var(--text);
  letter-spacing: -0.2px;
  margin-bottom: 14px;
}
.panel__note {
  margin-top: 14px;
  font-size: 0.75rem;
  line-height: 1.6;
  color: var(--text-subtle);
}
.hotlist {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.hotlist__item {
  display: flex;
  gap: 12px;
  padding: 8px 6px;
  border-radius: var(--r-sm);
}
.hotlist__item:hover {
  background: var(--surface-3);
}
.hotlist__rank {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  border-radius: var(--r-xs);
  background: var(--surface-2);
  color: var(--text-muted);
  font-size: 0.75rem;
  font-weight: 500;
  margin-top: 2px;
}
.hotlist__rank.is-top {
  background: var(--primary);
  color: var(--on-primary);
}
.hotlist__text {
  min-width: 0;
}
.hotlist__title {
  font-size: 0.875rem;
  color: var(--text);
  line-height: 1.45;
}
.hotlist__meta {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin-top: 4px;
  font-size: 0.75rem;
  color: var(--text-subtle);
}
.hotlist__meta :deep(.icon) {
  margin-right: 3px;
}
.cats {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.cats__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  border-radius: var(--r-sm);
  font-size: 0.875rem;
  color: var(--text-body);
}
.cats__item:hover {
  background: var(--surface-3);
  color: var(--text);
}
.cats__count {
  font-size: 0.75rem;
  color: var(--text-subtle);
}
.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.tags__item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 11px;
  border-radius: var(--r-pill);
  background: var(--surface-2);
  border: 1px solid var(--border);
  font-size: 0.8125rem;
  color: var(--text-body);
}
.tags__item:hover {
  border-color: var(--primary);
  color: var(--text);
}
.tags__count {
  font-size: 0.6875rem;
  color: var(--text-subtle);
}

/* CTA */
.cta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--sp-xl);
  padding: var(--sp-xxl) var(--sp-xl);
  background: var(--primary);
  color: var(--on-primary);
  border-radius: var(--r-lg);
  margin-bottom: var(--sp-section);
  flex-wrap: wrap;
}
.cta__h {
  font-family: var(--font-display);
  font-size: clamp(1.5rem, 3vw, 1.875rem);
  font-weight: var(--display-weight);
  letter-spacing: -0.4px;
}
.cta__p {
  margin-top: 8px;
  opacity: 0.9;
  font-size: 0.9375rem;
}

@media (max-width: 1000px) {
  .hero__grid {
    grid-template-columns: 1fr;
  }
  .hero__aside {
    order: 2;
  }
  .home__body {
    grid-template-columns: 1fr;
  }
  .home__aside {
    position: static;
  }
}
@media (max-width: 640px) {
  .home__grid {
    grid-template-columns: 1fr;
  }
}
</style>