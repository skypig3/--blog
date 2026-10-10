<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '@/api/admin'
import { articleStatus, formatDateTime, formatNumber } from '@/utils/format'
import { approveArticle, rejectArticle, approveComment, rejectComment } from '@/composables/adminActions'

const emit = defineEmits(['refresh-badges'])

const data = ref(null)
const loading = ref(true)

async function load() {
  loading.value = true
  try {
    data.value = await adminApi.dashboard()
  } finally {
    loading.value = false
  }
}

async function after(fn) {
  const changed = await fn()
  if (changed) {
    await load()
    emit('refresh-badges')
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="a-head">
      <div>
        <h1>数据总览</h1>
        <p class="caption">全部数字实时来自 SQLite，不是写死的统计图表。</p>
      </div>
      <UiButton variant="secondary" size="sm" icon="activity" @click="load">刷新</UiButton>
    </div>

    <div v-if="loading" class="a-stats">
      <UiSkeleton v-for="i in 7" :key="i" height="98px" radius="12px" />
    </div>

    <template v-else-if="data">
      <div class="a-stats">
        <div class="a-stat">
          <div class="a-stat__label"><UiIcon name="users" :size="14" />用户总数</div>
          <div class="a-stat__value">{{ data.stats.userCount }}</div>
        </div>
        <div class="a-stat">
          <div class="a-stat__label"><UiIcon name="file-text" :size="14" />文章总数</div>
          <div class="a-stat__value">{{ data.stats.articleCount }}</div>
        </div>
        <div class="a-stat">
          <div class="a-stat__label"><UiIcon name="check-circle" :size="14" />已发布文章</div>
          <div class="a-stat__value">{{ data.stats.publishedArticleCount }}</div>
        </div>
        <div class="a-stat a-stat--warn">
          <div class="a-stat__label"><UiIcon name="clock" :size="14" />待审核文章</div>
          <div class="a-stat__value">{{ data.stats.pendingArticleCount }}</div>
          <div class="a-stat__hint">需要处理</div>
        </div>
        <div class="a-stat">
          <div class="a-stat__label"><UiIcon name="message-circle" :size="14" />评论总数</div>
          <div class="a-stat__value">{{ data.stats.commentCount }}</div>
        </div>
        <div class="a-stat a-stat--warn">
          <div class="a-stat__label"><UiIcon name="alert-circle" :size="14" />待审核评论</div>
          <div class="a-stat__value">{{ data.stats.pendingCommentCount }}</div>
          <div class="a-stat__hint">需要处理</div>
        </div>
        <div class="a-stat a-stat--accent">
          <div class="a-stat__label"><UiIcon name="eye" :size="14" />阅读量汇总</div>
          <div class="a-stat__value">{{ formatNumber(data.stats.totalViewCount) }}</div>
          <div class="a-stat__hint">{{ data.stats.totalViewCount }} 次阅读</div>
        </div>
      </div>

      <div class="a-grid-2" style="margin-bottom: var(--sp-lg)">
        <!-- 待审核文章 -->
        <div class="a-panel">
          <div class="a-panel__head">
            <span class="a-panel__title">待审核文章</span>
            <UiBadge variant="warning" size="sm">{{ data.pendingArticles.length }}</UiBadge>
          </div>
          <div v-if="data.pendingArticles.length" class="a-list">
            <div v-for="a in data.pendingArticles" :key="a.id" class="a-list__item">
              <div class="a-list__main">
                <RouterLink class="a-list__title cell-link" :to="{ name: 'admin-articles' }">{{ a.title }}</RouterLink>
                <div class="a-list__meta">
                  <span>{{ a.author?.nickname }}</span>
                  <span>{{ formatDateTime(a.createdAt) }}</span>
                </div>
              </div>
              <div class="a-actions">
                <UiButton variant="primary" size="sm" @click="after(() => approveArticle(a))">通过</UiButton>
                <UiButton variant="secondary" size="sm" @click="after(() => rejectArticle(a))">驳回</UiButton>
              </div>
            </div>
          </div>
          <UiEmpty v-else title="没有待审文章" description="所有文章都已处理。" />
        </div>

        <!-- 待审核评论 -->
        <div class="a-panel">
          <div class="a-panel__head">
            <span class="a-panel__title">待审核评论</span>
            <UiBadge variant="warning" size="sm">{{ data.pendingComments.length }}</UiBadge>
          </div>
          <div v-if="data.pendingComments.length" class="a-list">
            <div v-for="c in data.pendingComments" :key="c.id" class="a-list__item">
              <div class="a-list__main">
                <p class="a-list__title">{{ c.content }}</p>
                <div class="a-list__meta">
                  <span>{{ c.author?.nickname }}</span>
                  <span>《{{ c.articleTitle }}》</span>
                  <span>{{ formatDateTime(c.createdAt) }}</span>
                </div>
              </div>
              <div class="a-actions">
                <UiButton variant="primary" size="sm" @click="after(() => approveComment(c))">通过</UiButton>
                <UiButton variant="secondary" size="sm" @click="after(() => rejectComment(c))">驳回</UiButton>
              </div>
            </div>
          </div>
          <UiEmpty v-else title="没有待审评论" description="所有评论都已处理。" />
        </div>
      </div>

      <!-- 近期文章 -->
      <div class="a-panel">
        <div class="a-panel__head">
          <span class="a-panel__title">近期文章</span>
          <RouterLink class="cell-link" :to="{ name: 'admin-articles' }" style="font-size: 0.8125rem">
            全部文章 →
          </RouterLink>
        </div>
        <div class="a-table-wrap">
          <table class="a-table">
            <thead>
              <tr>
                <th>标题</th>
                <th>作者</th>
                <th>分类</th>
                <th>状态</th>
                <th>阅读</th>
                <th>评论</th>
                <th>时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="a in data.recentArticles" :key="a.id">
                <td class="cell-strong">{{ a.title }}</td>
                <td class="cell-muted">{{ a.author?.nickname }}</td>
                <td class="cell-muted">{{ a.categoryName || '—' }}</td>
                <td>
                  <UiBadge :variant="articleStatus(a.status).variant" size="sm">
                    {{ articleStatus(a.status).label }}
                  </UiBadge>
                </td>
                <td class="cell-num">{{ a.viewCount }}</td>
                <td class="cell-num">{{ a.commentCount }}</td>
                <td class="cell-muted">{{ formatDateTime(a.publishedAt || a.createdAt) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <UiEmpty v-if="!data.recentArticles.length" title="还没有文章" />
      </div>
    </template>
  </div>
</template>