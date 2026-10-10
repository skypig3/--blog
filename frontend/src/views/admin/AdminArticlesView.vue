<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '@/api/admin'
import { ARTICLE_STATUS_OPTIONS, articleStatus, formatDateTime, formatNumber } from '@/utils/format'
import {
  approveArticle,
  rejectArticle,
  offlineArticle,
  deleteArticle
} from '@/composables/adminActions'

const emit = defineEmits(['refresh-badges'])

const keyword = ref('')
const status = ref('PENDING')
const categoryId = ref('')
const authorId = ref('')
const current = ref(1)
const size = 10

const records = ref([])
const total = ref(0)
const loading = ref(true)
const categories = ref([])
const authors = ref([])

const detailOpen = ref(false)
const detail = ref(null)
const detailLoading = ref(false)

async function load() {
  loading.value = true
  try {
    const page = await adminApi.articles({
      keyword: keyword.value.trim() || undefined,
      status: status.value || undefined,
      categoryId: categoryId.value || undefined,
      authorId: authorId.value || undefined,
      current: current.value,
      size
    })
    records.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}

function search(resetPage = true) {
  if (resetPage) current.value = 1
  load()
}
function onPage(p) {
  current.value = p
  load()
}

async function openDetail(a) {
  detailOpen.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await adminApi.articleDetail(a.id)
  } finally {
    detailLoading.value = false
  }
}

async function after(fn) {
  const changed = await fn()
  if (changed) {
    await load()
    emit('refresh-badges')
    if (detailOpen.value && detail.value) {
      try {
        detail.value = await adminApi.articleDetail(detail.value.id)
      } catch {
        detailOpen.value = false
      }
    }
  }
}

onMounted(async () => {
  const [cats, users] = await Promise.all([
    adminApi.categories().catch(() => []),
    adminApi.users({ current: 1, size: 100 }).catch(() => ({ records: [] }))
  ])
  categories.value = cats || []
  authors.value = users?.records || []
  await load()
})
</script>

<template>
  <div>
    <div class="a-head">
      <div>
        <h1>文章管理与审核</h1>
        <p class="caption">可按关键字、分类、作者、状态筛选；审核操作会写入操作日志。</p>
      </div>
      <UiButton variant="secondary" size="sm" icon="activity" @click="load">刷新</UiButton>
    </div>

    <div class="a-toolbar">
      <div class="a-toolbar__grow">
        <UiInput v-model="keyword" icon="search" placeholder="搜索标题…" @enter="search()" />
      </div>
      <UiSelect v-model="status" :options="ARTICLE_STATUS_OPTIONS" placeholder="全部状态" @update:model-value="search()" />
      <UiSelect
        v-model="categoryId"
        :options="categories.map((c) => ({ label: c.name, value: c.id }))"
        placeholder="全部分类"
        @update:model-value="search()"
      />
      <UiSelect
        v-model="authorId"
        :options="authors.map((u) => ({ label: u.nickname || u.username, value: u.id }))"
        placeholder="全部作者"
        @update:model-value="search()"
      />
      <UiButton variant="primary" icon="search" @click="search()">查询</UiButton>
    </div>

    <div class="a-panel">
      <div class="a-table-wrap">
        <table class="a-table">
          <thead>
            <tr>
              <th style="width: 56px">ID</th>
              <th>标题</th>
              <th>作者</th>
              <th>分类</th>
              <th>状态</th>
              <th>阅读</th>
              <th>时间</th>
              <th style="text-align: right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="a in records" :key="a.id">
              <td class="cell-num">#{{ a.id }}</td>
              <td class="cell-strong cell-clamp">
                <span class="clamp-2">{{ a.title }}</span>
              </td>
              <td class="cell-muted">{{ a.author?.nickname }}</td>
              <td class="cell-muted">{{ a.categoryName || '—' }}</td>
              <td>
                <UiBadge :variant="articleStatus(a.status).variant" size="sm">
                  {{ articleStatus(a.status).label }}
                </UiBadge>
              </td>
              <td class="cell-num">{{ formatNumber(a.viewCount) }}</td>
              <td class="cell-muted">{{ formatDateTime(a.createdAt) }}</td>
              <td>
                <div class="a-actions">
                  <UiButton variant="ghost" size="sm" @click="openDetail(a)">查看</UiButton>
                  <UiButton v-if="a.status === 'PENDING'" variant="primary" size="sm" @click="after(() => approveArticle(a))">
                    通过
                  </UiButton>
                  <UiButton v-if="a.status === 'PENDING'" variant="secondary" size="sm" @click="after(() => rejectArticle(a))">
                    驳回
                  </UiButton>
                  <UiButton v-if="a.status === 'PUBLISHED'" variant="secondary" size="sm" @click="after(() => offlineArticle(a))">
                    下架
                  </UiButton>
                  <UiButton variant="ghost" size="sm" @click="after(() => deleteArticle(a))">删除</UiButton>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <UiEmpty v-if="!loading && !records.length" title="没有符合条件的文章" description="换个筛选条件试试。" />
      <UiSpinner v-if="loading" label="加载中…" />
    </div>

    <div class="a-pager">
      <UiPagination :current="current" :total="total" :size="size" @change="onPage" />
    </div>

    <!-- 详情弹窗 -->
    <UiModal v-model="detailOpen" title="文章详情" size="lg">
      <UiSpinner v-if="detailLoading" label="加载中…" />
      <div v-else-if="detail" class="adet">
        <div class="adet__meta">
          <UiBadge :variant="articleStatus(detail.status).variant">{{ articleStatus(detail.status).label }}</UiBadge>
          <span v-if="detail.categoryName">分类：{{ detail.categoryName }}</span>
          <span>作者：{{ detail.author?.nickname }}</span>
          <span>{{ formatDateTime(detail.publishedAt || detail.createdAt) }}</span>
        </div>
        <h3 class="adet__title">{{ detail.title }}</h3>
        <p v-if="detail.summary" class="adet__summary">{{ detail.summary }}</p>

        <div v-if="detail.tags?.length" class="adet__tags">
          <span v-for="t in detail.tags" :key="t"># {{ t }}</span>
        </div>

        <div v-if="detail.rejectReason" class="a-notice a-notice--error">驳回原因：{{ detail.rejectReason }}</div>
        <div v-if="detail.offlineReason" class="a-notice a-notice--warn">下架原因：{{ detail.offlineReason }}</div>

        <img v-if="detail.cover" class="adet__cover" :src="detail.cover" :alt="detail.title" />
        <div class="adet__content"><MarkdownView :source="detail.contentMd" /></div>
      </div>

      <template #footer>
        <UiButton variant="ghost" @click="detailOpen = false">关闭</UiButton>
        <template v-if="detail">
          <UiButton v-if="detail.status === 'PENDING'" variant="secondary" @click="after(() => rejectArticle(detail))">驳回</UiButton>
          <UiButton v-if="detail.status === 'PENDING'" variant="primary" @click="after(() => approveArticle(detail))">通过</UiButton>
          <UiButton v-if="detail.status === 'PUBLISHED'" variant="secondary" @click="after(() => offlineArticle(detail))">下架</UiButton>
        </template>
      </template>
    </UiModal>
  </div>
</template>

<style scoped>
.adet__meta {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
  align-items: center;
  font-size: 0.8125rem;
  color: var(--text-subtle);
  margin-bottom: 14px;
}
.adet__title {
  font-family: var(--font-display);
  font-size: 1.375rem;
  font-weight: var(--display-weight);
  letter-spacing: -0.3px;
  color: var(--text);
}
.adet__summary {
  margin-top: 10px;
  color: var(--text-muted);
  line-height: 1.7;
}
.adet__tags {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 12px;
  font-size: 0.8125rem;
  color: var(--text-subtle);
}
.adet__cover {
  width: 100%;
  border-radius: var(--r-lg);
  margin: 16px 0;
  border: 1px solid var(--border);
}
.adet__content {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--border);
}
</style>