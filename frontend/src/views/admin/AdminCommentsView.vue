<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '@/api/admin'
import { COMMENT_STATUS_OPTIONS, commentStatus, formatDateTime } from '@/utils/format'
import { approveComment, rejectComment, deleteComment } from '@/composables/adminActions'

const emit = defineEmits(['refresh-badges'])

const keyword = ref('')
const status = ref('PENDING')
const userId = ref('')
const current = ref(1)
const size = 10

const records = ref([])
const total = ref(0)
const loading = ref(true)
const authors = ref([])

const detailOpen = ref(false)
const detail = ref(null)

async function load() {
  loading.value = true
  try {
    const page = await adminApi.comments({
      keyword: keyword.value.trim() || undefined,
      status: status.value || undefined,
      userId: userId.value || undefined,
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

function openDetail(c) {
  detail.value = c
  detailOpen.value = true
}

async function after(fn) {
  const changed = await fn()
  if (changed) {
    await load()
    emit('refresh-badges')
    if (detailOpen.value) detailOpen.value = false
  }
}

onMounted(async () => {
  const users = await adminApi.users({ current: 1, size: 100 }).catch(() => ({ records: [] }))
  authors.value = users?.records || []
  await load()
})
</script>

<template>
  <div>
    <div class="a-head">
      <div>
        <h1>评论审核</h1>
        <p class="caption">回复与顶层评论共用同一套审核；可按内容、作者、状态筛选，通过后会计入文章评论数。</p>
      </div>
      <UiButton variant="secondary" size="sm" icon="activity" @click="load">刷新</UiButton>
    </div>

    <div class="a-toolbar">
      <div class="a-toolbar__grow">
        <UiInput v-model="keyword" icon="search" placeholder="搜索评论内容…" @enter="search()" />
      </div>
      <UiSelect v-model="status" :options="COMMENT_STATUS_OPTIONS" placeholder="全部状态" @update:model-value="search()" />
      <UiSelect
        v-model="userId"
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
              <th>评论内容</th>
              <th>评论者</th>
              <th>所属文章</th>
              <th>类型</th>
              <th>状态</th>
              <th>时间</th>
              <th style="text-align: right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in records" :key="c.id">
              <td class="cell-num">#{{ c.id }}</td>
              <td class="cell-strong cell-clamp">
                <span class="clamp-2">{{ c.content }}</span>
              </td>
              <td class="cell-muted">{{ c.author?.nickname }}</td>
              <td class="cell-muted cell-clamp">
                <span class="clamp-2">{{ c.articleTitle || `#${c.articleId}` }}</span>
              </td>
              <td class="cell-muted">{{ c.parentId ? '回复' : '顶层' }}</td>
              <td>
                <UiBadge :variant="commentStatus(c.status).variant" size="sm">
                  {{ commentStatus(c.status).label }}
                </UiBadge>
              </td>
              <td class="cell-muted">{{ formatDateTime(c.createdAt) }}</td>
              <td>
                <div class="a-actions">
                  <UiButton variant="ghost" size="sm" @click="openDetail(c)">查看</UiButton>
                  <UiButton
                    v-if="c.status === 'PENDING' || c.status === 'REJECTED'"
                    variant="primary"
                    size="sm"
                    @click="after(() => approveComment(c))"
                  >
                    通过
                  </UiButton>
                  <UiButton v-if="c.status === 'PENDING'" variant="secondary" size="sm" @click="after(() => rejectComment(c))">
                    驳回
                  </UiButton>
                  <UiButton variant="ghost" size="sm" @click="after(() => deleteComment(c))">删除</UiButton>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <UiEmpty v-if="!loading && !records.length" title="没有符合条件的评论" />
      <UiSpinner v-if="loading" label="加载中…" />
    </div>

    <div class="a-pager">
      <UiPagination :current="current" :total="total" :size="size" @change="onPage" />
    </div>

    <UiModal v-model="detailOpen" title="评论详情" size="md">
      <div v-if="detail" class="cdet">
        <div class="cdet__meta">
          <UiBadge :variant="commentStatus(detail.status).variant">{{ commentStatus(detail.status).label }}</UiBadge>
          <span>{{ detail.author?.nickname }}</span>
          <span>{{ formatDateTime(detail.createdAt) }}</span>
        </div>
        <p class="cdet__article">
          所属文章：<RouterLink class="cell-link" :to="{ name: 'admin-articles' }">{{ detail.articleTitle || `#${detail.articleId}` }}</RouterLink>
        </p>
        <p class="cdet__content">{{ detail.content }}</p>
        <div v-if="detail.rejectReason" class="a-notice a-notice--error">驳回原因：{{ detail.rejectReason }}</div>
        <div v-if="detail.children?.length" class="cdet__replies">
          <span class="caption">该评论有 {{ detail.children.length }} 条回复</span>
          <div v-for="r in detail.children" :key="r.id" class="cdet__reply">
            <strong>{{ r.author?.nickname }}</strong>
            <span>{{ r.content }}</span>
          </div>
        </div>
      </div>
      <template #footer>
        <UiButton variant="ghost" @click="detailOpen = false">关闭</UiButton>
      </template>
    </UiModal>
  </div>
</template>

<style scoped>
.cdet__meta {
  display: flex;
  gap: 12px;
  align-items: center;
  font-size: 0.8125rem;
  color: var(--text-subtle);
}
.cdet__article {
  margin-top: 12px;
  font-size: 0.875rem;
  color: var(--text-muted);
}
.cdet__content {
  margin-top: 14px;
  padding: 14px 16px;
  background: var(--surface-2);
  border: 1px solid var(--border);
  border-radius: var(--r-md);
  color: var(--text-body);
  line-height: 1.7;
  white-space: pre-wrap;
}
.cdet__replies {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.cdet__reply {
  display: flex;
  gap: 8px;
  font-size: 0.875rem;
  color: var(--text-muted);
  padding: 8px 12px;
  background: var(--surface-3);
  border-radius: var(--r-sm);
}
.cdet__reply strong {
  color: var(--text);
  font-weight: 500;
}
</style>