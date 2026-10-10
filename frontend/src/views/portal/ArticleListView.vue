<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { articleApi } from '@/api/articles'
import { taxonomyApi } from '@/api/site'
import ArticleCard from '@/components/portal/ArticleCard.vue'

const route = useRoute()
const router = useRouter()

const keyword = ref('')
const categoryId = ref('')
const tagId = ref('')
const sort = ref('latest')
const current = ref(1)
const size = 9

const records = ref([])
const total = ref(0)
const loading = ref(true)

const categories = ref([])
const tags = ref([])

const mode = computed(() => route.name) // articles | search | category | tag
const isCategoryMode = computed(() => mode.value === 'category')
const isTagMode = computed(() => mode.value === 'tag')

const heading = computed(() => {
  if (isCategoryMode.value) {
    const c = categories.value.find((x) => String(x.id) === String(route.params.id))
    return c ? `分类 · ${c.name}` : '分类'
  }
  if (isTagMode.value) {
    const t = tags.value.find((x) => String(x.id) === String(route.params.id))
    return t ? `标签 · ${t.name}` : '标签'
  }
  if (mode.value === 'search') return keyword.value ? `搜索「${keyword.value}」` : '搜索文章'
  return '全部文章'
})

const activeCategoryName = computed(
  () => categories.value.find((x) => String(x.id) === String(categoryId.value))?.name || ''
)
const activeTagName = computed(
  () => tags.value.find((x) => String(x.id) === String(tagId.value))?.name || ''
)

function buildParams() {
  const params = { current: current.value, size, sort: sort.value }
  if (keyword.value.trim()) params.keyword = keyword.value.trim()
  const cid = isCategoryMode.value ? route.params.id : categoryId.value
  const tid = isTagMode.value ? route.params.id : tagId.value
  if (cid) params.categoryId = cid
  if (tid) params.tagId = tid
  return params
}

async function load() {
  loading.value = true
  try {
    const page = await articleApi.list(buildParams())
    records.value = page.records
    total.value = page.total
  } catch {
    records.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

/** 从路由同步筛选条件（支持直接访问带 query 的链接 / 前进后退） */
function syncFromRoute() {
  keyword.value = route.query.q || ''
  categoryId.value = route.query.categoryId || ''
  tagId.value = route.query.tagId || ''
  sort.value = route.query.sort || 'latest'
  current.value = Number(route.query.current || 1)
}

/** 把筛选条件写回 query（replace 避免污染历史） */
function pushQuery(resetPage = false) {
  if (resetPage) current.value = 1
  const q = keyword.value.trim()
  const query = {}
  if (q) query.q = q
  if (categoryId.value && !isCategoryMode.value) query.categoryId = categoryId.value
  if (tagId.value && !isTagMode.value) query.tagId = tagId.value
  if (sort.value !== 'latest') query.sort = sort.value
  if (current.value > 1) query.current = String(current.value)

  // 分类 / 标签页保持自身路由；其余情况带关键词时切到 /search，与站内搜索入口保持一致
  const name = isCategoryMode.value || isTagMode.value ? route.name : q ? 'search' : 'articles'
  router.replace({ name, params: route.params, query })
}

function onSearch() {
  pushQuery(true)
}
function onFilterChange() {
  pushQuery(true)
}
function onPage(p) {
  current.value = p
  pushQuery(false)
}
function clearAll() {
  keyword.value = ''
  categoryId.value = ''
  tagId.value = ''
  sort.value = 'latest'
  router.replace({ name: 'articles' })
}

watch(
  () => route.fullPath,
  () => {
    syncFromRoute()
    load()
  }
)

onMounted(async () => {
  syncFromRoute()
  const [cats, tgs] = await Promise.all([
    taxonomyApi.categories().catch(() => []),
    taxonomyApi.tags().catch(() => [])
  ])
  categories.value = cats || []
  tags.value = tgs || []
  await load()
})
</script>

<template>
  <div class="container list">
    <header class="list__head">
      <span class="eyebrow">浏览</span>
      <h1 class="display-2 list__h">{{ heading }}</h1>
      <p class="list__count">
        <template v-if="!loading">共 {{ total }} 篇文章</template>
        <template v-else>加载中…</template>
      </p>
    </header>

    <div class="list__toolbar">
      <div class="list__search">
        <UiInput
          v-model="keyword"
          icon="search"
          placeholder="按标题搜索…"
          @enter="onSearch"
        />
      </div>

      <UiSelect
        v-if="!isCategoryMode"
        v-model="categoryId"
        :options="categories.map((c) => ({ label: `${c.name}（${c.articleCount}）`, value: c.id }))"
        placeholder="全部分类"
        @update:model-value="onFilterChange"
      />
      <UiSelect
        v-if="!isTagMode"
        v-model="tagId"
        :options="tags.map((t) => ({ label: `${t.name}（${t.articleCount}）`, value: t.id }))"
        placeholder="全部标签"
        @update:model-value="onFilterChange"
      />
      <UiSelect
        v-model="sort"
        :options="[
          { label: '最新发布', value: 'latest' },
          { label: '按热度', value: 'hot' }
        ]"
        :placeholder="''"
        @update:model-value="onFilterChange"
      />

      <UiButton variant="secondary" icon="search" @click="onSearch">搜索</UiButton>
      <UiButton
        v-if="keyword || categoryId || tagId || sort !== 'latest' || isCategoryMode || isTagMode"
        variant="ghost"
        @click="clearAll"
      >
        清空筛选
      </UiButton>
    </div>

    <div v-if="activeCategoryName || activeTagName" class="list__chips">
      <UiBadge v-if="activeCategoryName" variant="neutral">分类 · {{ activeCategoryName }}</UiBadge>
      <UiBadge v-if="activeTagName" variant="neutral">标签 · {{ activeTagName }}</UiBadge>
    </div>

    <template v-if="loading">
      <div class="list__grid">
        <UiSkeleton v-for="i in 6" :key="i" height="330px" radius="12px" />
      </div>
    </template>

    <template v-else-if="records.length">
      <div class="list__grid">
        <ArticleCard v-for="a in records" :key="a.id" :article="a" />
      </div>
      <div class="list__pager">
        <UiPagination :current="current" :total="total" :size="size" @change="onPage" />
      </div>
    </template>

    <UiEmpty
      v-else
      icon="search"
      title="没有找到匹配的文章"
      :description="keyword ? `没有标题包含「${keyword}」的已发布文章，换个关键词试试。` : '这个筛选条件下还没有文章。'"
    >
      <UiButton variant="secondary" @click="clearAll">重置筛选</UiButton>
    </UiEmpty>
  </div>
</template>

<style scoped>
.list {
  padding-top: var(--sp-xxl);
  padding-bottom: var(--sp-xxl);
}
.list__head {
  margin-bottom: var(--sp-lg);
}
.list__h {
  margin: 10px 0 8px;
}
.list__count {
  font-size: 0.875rem;
  color: var(--text-subtle);
}
.list__toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  padding: 14px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--r-lg);
  margin-bottom: var(--sp-md);
}
.list__search {
  flex: 1;
  min-width: 200px;
}
.list__chips {
  display: flex;
  gap: 8px;
  margin-bottom: var(--sp-lg);
}
.list__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--sp-lg);
}
.list__pager {
  margin-top: var(--sp-xl);
}
@media (max-width: 1000px) {
  .list__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 640px) {
  .list__grid {
    grid-template-columns: 1fr;
  }
}
</style>