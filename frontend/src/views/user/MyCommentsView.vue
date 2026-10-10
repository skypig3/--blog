<script setup>
import { ref, onMounted } from 'vue'
import { myApi } from '@/api/articles'
import { commentStatus, formatDateTime } from '@/utils/format'

const status = ref('')
const current = ref(1)
const size = 10
const records = ref([])
const total = ref(0)
const loading = ref(true)

const TABS = [
  { label: '全部', value: '' },
  { label: '待审核', value: 'PENDING' },
  { label: '已通过', value: 'PUBLISHED' },
  { label: '已驳回', value: 'REJECTED' }
]

async function load() {
  loading.value = true
  try {
    const page = await myApi.comments({ status: status.value || undefined, current: current.value, size })
    records.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}

function switchTab(v) {
  status.value = v
  current.value = 1
  load()
}
function onPage(p) {
  current.value = p
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <header class="uc-head">
      <h1 class="display-3">我的评论</h1>
      <p class="caption">共 {{ total }} 条 · 可以在这里查看每条评论的审核结果</p>
    </header>

    <div class="tabs">
      <button
        v-for="t in TABS"
        :key="t.value"
        type="button"
        class="tabs__item"
        :class="{ 'is-active': status === t.value }"
        @click="switchTab(t.value)"
      >
        {{ t.label }}
      </button>
    </div>

    <div v-if="loading" class="uc-loading"><UiSpinner label="加载中…" /></div>

    <div v-else-if="records.length" class="rows">
      <div v-for="c in records" :key="c.id" class="crow">
        <div class="crow__head">
          <UiBadge :variant="commentStatus(c.status).variant" size="sm">
            {{ commentStatus(c.status).label }}
          </UiBadge>
          <RouterLink
            v-if="c.articleId"
            class="crow__article"
            :to="{ name: 'article-detail', params: { id: c.articleId } }"
          >
            {{ c.articleTitle || `文章 #${c.articleId}` }}
          </RouterLink>
          <span class="crow__time">{{ formatDateTime(c.createdAt) }}</span>
        </div>
        <p class="crow__text">{{ c.content }}</p>
        <p v-if="c.status === 'REJECTED' && c.rejectReason" class="crow__reason">
          <UiIcon name="alert-circle" :size="14" />驳回原因：{{ c.rejectReason }}
        </p>
      </div>
      <UiPagination :current="current" :total="total" :size="size" @change="onPage" />
    </div>

    <UiEmpty v-else icon="message-circle" title="还没有评论" description="在文章详情页发表评论，审核结果会显示在这里。" />
  </div>
</template>

<style scoped>
.uc-head {
  margin-bottom: var(--sp-lg);
}
.uc-head .caption {
  margin-top: 6px;
}
.tabs {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
  margin-bottom: var(--sp-lg);
}
.tabs__item {
  border: 1px solid transparent;
  background: transparent;
  padding: 6px 14px;
  border-radius: var(--r-md);
  font-size: 0.875rem;
  color: var(--text-muted);
  font-family: inherit;
}
.tabs__item:hover {
  background: var(--surface-3);
  color: var(--text);
}
.tabs__item.is-active {
  background: var(--surface);
  border-color: var(--border);
  color: var(--text);
}
.uc-loading {
  padding: var(--sp-xxl) 0;
}
.rows {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.crow {
  padding: 18px 20px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--r-lg);
}
.crow__head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.crow__article {
  font-size: 0.875rem;
  color: var(--text);
  font-weight: 500;
}
.crow__article:hover {
  color: var(--primary);
}
.crow__time {
  margin-left: auto;
  font-size: 0.8125rem;
  color: var(--text-subtle);
}
.crow__text {
  margin-top: 10px;
  font-size: 0.9375rem;
  line-height: 1.7;
  color: var(--text-body);
  white-space: pre-wrap;
}
.crow__reason {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 10px;
  font-size: 0.8125rem;
  color: var(--error);
}
</style>