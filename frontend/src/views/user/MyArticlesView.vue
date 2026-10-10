<script setup>
import { ref, computed, onMounted } from 'vue'
import { myApi, articleApi } from '@/api/articles'
import { articleStatus, formatDateTime, formatNumber } from '@/utils/format'
import { toast, confirmDialog } from '@/composables/feedback'

const status = ref('')
const current = ref(1)
const size = 10
const records = ref([])
const total = ref(0)
const loading = ref(true)
const busyId = ref(null)

const TABS = [
  { label: '全部', value: '' },
  { label: '草稿', value: 'DRAFT' },
  { label: '待审核', value: 'PENDING' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '已驳回', value: 'REJECTED' },
  { label: '已下架', value: 'REMOVED' }
]

const canSubmit = (s) => ['DRAFT', 'REJECTED', 'REMOVED', 'PUBLISHED'].includes(s)
const canEdit = (s) => ['DRAFT', 'REJECTED', 'REMOVED', 'PUBLISHED'].includes(s)

async function load() {
  loading.value = true
  try {
    const page = await myApi.articles({ status: status.value || undefined, current: current.value, size })
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

async function submit(a) {
  busyId.value = a.id
  try {
    await articleApi.submit(a.id)
    toast.success('已提交审核')
    await load()
  } finally {
    busyId.value = null
  }
}

async function withdraw(a) {
  busyId.value = a.id
  try {
    await articleApi.withdraw(a.id)
    toast.success('已撤回，回到草稿')
    await load()
  } finally {
    busyId.value = null
  }
}

async function remove(a) {
  const ok = await confirmDialog({
    title: '删除文章',
    content: `确定删除《${a.title}》吗？`,
    confirmText: '删除',
    danger: true
  })
  if (!ok) return
  busyId.value = a.id
  try {
    await articleApi.remove(a.id)
    toast.success('已删除')
    await load()
  } finally {
    busyId.value = null
  }
}

onMounted(load)
</script>

<template>
  <div>
    <header class="uc-head">
      <div>
        <h1 class="display-3">我的文章</h1>
        <p class="caption">共 {{ total }} 篇</p>
      </div>
      <UiButton variant="primary" :to="{ name: 'editor' }" icon="plus">写文章</UiButton>
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
      <div v-for="a in records" :key="a.id" class="row-item">
        <div class="row-item__main">
          <div class="row-item__titleline">
            <RouterLink class="row-item__title" :to="{ name: 'article-detail', params: { id: a.id } }">
              {{ a.title }}
            </RouterLink>
            <UiBadge :variant="articleStatus(a.status).variant" size="sm">
              {{ articleStatus(a.status).label }}
            </UiBadge>
          </div>
          <div class="row-item__meta">
            <span v-if="a.categoryName"><UiIcon name="folder" :size="14" />{{ a.categoryName }}</span>
            <span><UiIcon name="calendar" :size="14" />{{ formatDateTime(a.publishedAt || a.createdAt) }}</span>
            <span><UiIcon name="eye" :size="14" />{{ formatNumber(a.viewCount) }}</span>
            <span><UiIcon name="heart" :size="14" />{{ formatNumber(a.likeCount) }}</span>
            <span><UiIcon name="message-circle" :size="14" />{{ formatNumber(a.commentCount) }}</span>
          </div>
          <p v-if="a.status === 'REJECTED' || a.status === 'REMOVED'" class="row-item__hint">
            <UiIcon name="alert-circle" :size="14" />
            这篇文章被{{ a.status === 'REJECTED' ? '驳回' : '下架' }}，原因见
            <RouterLink class="link" :to="{ name: 'article-detail', params: { id: a.id } }">文章详情</RouterLink>
            或<RouterLink class="link" :to="{ name: 'editor', params: { id: a.id } }">编辑页</RouterLink>。
          </p>
        </div>
        <div class="row-item__actions">
          <UiButton v-if="canEdit(a.status)" variant="ghost" size="sm" :to="{ name: 'editor', params: { id: a.id } }">
            编辑
          </UiButton>
          <UiButton
            v-if="canSubmit(a.status)"
            variant="secondary"
            size="sm"
            :loading="busyId === a.id"
            @click="submit(a)"
          >
            提交审核
          </UiButton>
          <UiButton v-if="a.status === 'PENDING'" variant="secondary" size="sm" :loading="busyId === a.id" @click="withdraw(a)">
            撤回
          </UiButton>
          <UiButton variant="ghost" size="sm" @click="remove(a)">删除</UiButton>
        </div>
      </div>
      <UiPagination :current="current" :total="total" :size="size" @change="onPage" />
    </div>

    <UiEmpty v-else icon="file-text" title="这里还没有文章" description="点击右上角开始创作，存为草稿或直接提交审核。">
      <UiButton variant="primary" :to="{ name: 'editor' }">写文章</UiButton>
    </UiEmpty>
  </div>
</template>

<style scoped>
.uc-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
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
.row-item {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  justify-content: space-between;
  padding: 18px 20px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--r-lg);
}
.row-item__main {
  min-width: 0;
}
.row-item__titleline {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.row-item__title {
  font-family: var(--font-display);
  font-size: 1.1875rem;
  font-weight: var(--display-weight);
  letter-spacing: -0.2px;
  color: var(--text);
}
.row-item__title:hover {
  color: var(--primary);
}
.row-item__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 8px;
  font-size: 0.8125rem;
  color: var(--text-subtle);
}
.row-item__meta span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}
.row-item__hint {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 10px;
  font-size: 0.8125rem;
  color: var(--text-muted);
}
.row-item__actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
  flex-wrap: wrap;
  justify-content: flex-end;
}
.link {
  color: var(--primary);
}
@media (max-width: 760px) {
  .row-item {
    flex-direction: column;
  }
  .row-item__actions {
    justify-content: flex-start;
  }
}
</style>