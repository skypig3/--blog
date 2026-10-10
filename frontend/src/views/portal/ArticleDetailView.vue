<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { articleApi } from '@/api/articles'
import { commentApi } from '@/api/articles'
import { interactionApi } from '@/api/articles'
import { useUserStore } from '@/stores/user'
import { articleStatus, formatDateTime, fromNow, formatNumber } from '@/utils/format'
import { toast, confirmDialog } from '@/composables/feedback'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

const article = ref(null)
const comments = ref([])
const loading = ref(true)
const notFound = ref(false)

const liked = ref(false)
const collected = ref(false)
const likeCount = ref(0)
const collectCount = ref(0)
const busyLike = ref(false)
const busyCollect = ref(false)

const commentText = ref('')
const replyTo = ref(null)
const replyText = ref('')
const posting = ref(false)

const isOwner = computed(() => store.user && article.value && store.user.id === article.value.authorId)
const status = computed(() => articleStatus(article.value?.status))
const ownerActionsVisible = computed(() => isOwner.value)
const canSubmit = computed(() =>
  ['DRAFT', 'REJECTED', 'REMOVED', 'PUBLISHED'].includes(article.value?.status)
)
const canWithdraw = computed(() => article.value?.status === 'PENDING')
const canEdit = computed(() =>
  ['DRAFT', 'REJECTED', 'REMOVED', 'PUBLISHED'].includes(article.value?.status)
)

async function load() {
  loading.value = true
  notFound.value = false
  try {
    const data = await articleApi.detail(route.params.id)
    article.value = data
    liked.value = !!data.liked
    collected.value = !!data.collected
    likeCount.value = data.likeCount || 0
    collectCount.value = data.collectCount || 0
    await loadComments()
  } catch (e) {
    if (e?.status === 404) notFound.value = true
    else article.value = null
  } finally {
    loading.value = false
  }
}

async function loadComments() {
  try {
    comments.value = await commentApi.listByArticle(route.params.id)
  } catch {
    comments.value = []
  }
}

function requireLogin() {
  if (!store.isLoggedIn) {
    toast.warning('登录后才能进行这个操作')
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return false
  }
  return true
}

async function toggleLike() {
  if (!requireLogin() || busyLike.value) return
  busyLike.value = true
  try {
    const t = await interactionApi.toggleLike(article.value.id)
    liked.value = t.active
    likeCount.value = t.count
  } finally {
    busyLike.value = false
  }
}

async function toggleCollect() {
  if (!requireLogin() || busyCollect.value) return
  busyCollect.value = true
  try {
    const t = await interactionApi.toggleCollect(article.value.id)
    collected.value = t.active
    collectCount.value = t.count
  } finally {
    busyCollect.value = false
  }
}

async function submitComment(parentId = null) {
  if (!requireLogin()) return
  const content = parentId ? replyText.value.trim() : commentText.value.trim()
  if (!content) {
    toast.warning('评论内容不能为空')
    return
  }
  posting.value = true
  try {
    await commentApi.create(article.value.id, { content, parentId })
    toast.success('已提交，等待管理员审核')
    if (parentId) {
      replyText.value = ''
      replyTo.value = null
    } else {
      commentText.value = ''
    }
    await loadComments()
    // 计数在审核通过后才变，这里刷新详情拿最新值
    const fresh = await articleApi.detail(article.value.id)
    article.value = fresh
  } finally {
    posting.value = false
  }
}

async function removeComment(c) {
  const ok = await confirmDialog({
    title: '删除评论',
    content: '删除后不可恢复，确定要删除这条评论吗？',
    confirmText: '删除',
    danger: true
  })
  if (!ok) return
  await commentApi.remove(article.value.id, c.id)
  toast.success('已删除')
  await loadComments()
}

async function submitArticle() {
  const ok = await confirmDialog({
    title: '提交审核',
    content: `将《${article.value.title}》提交审核后，管理员会收到待审任务，通过后立即对游客可见。`,
    confirmText: '提交审核'
  })
  if (!ok) return
  article.value = await articleApi.submit(article.value.id)
  toast.success('已提交审核')
}

async function withdrawArticle() {
  article.value = await articleApi.withdraw(article.value.id)
  toast.success('已撤回，回到草稿')
}

async function removeArticle() {
  const ok = await confirmDialog({
    title: '删除文章',
    content: '删除后文章不会再出现在任何列表里（逻辑删除，数据仍保留）。确定删除？',
    confirmText: '删除',
    danger: true
  })
  if (!ok) return
  await articleApi.remove(article.value.id)
  toast.success('已删除')
  router.push({ name: 'my-articles' })
}

watch(() => route.params.id, load)
onMounted(load)
</script>

<template>
  <div class="container adetail">
    <template v-if="loading">
      <UiSpinner label="正在加载文章…" />
    </template>

    <template v-else-if="notFound">
      <UiEmpty icon="file-text" title="文章不存在或尚未公开" description="可能已被删除、下架，或还没有通过审核。">
        <UiButton variant="secondary" :to="{ name: 'articles' }">去看其他文章</UiButton>
      </UiEmpty>
    </template>

    <template v-else-if="article">
      <RouterLink class="adetail__back" :to="{ name: 'articles' }">
        <UiIcon name="arrow-left" :size="16" /> 返回列表
      </RouterLink>

      <article class="adetail__article">
        <header class="adetail__head">
          <div class="adetail__meta-top">
            <RouterLink
              v-if="article.categoryId"
              class="adetail__cat"
              :to="{ name: 'category', params: { id: article.categoryId } }"
            >
              <UiIcon name="folder" :size="14" />{{ article.categoryName }}
            </RouterLink>
            <UiBadge v-if="isOwner || article.status !== 'PUBLISHED'" :variant="status.variant">
              {{ status.label }}
            </UiBadge>
          </div>

          <h1 class="display-1 adetail__title">{{ article.title }}</h1>

          <div class="adetail__meta">
            <span class="adetail__author">
              <UiAvatar :src="article.author?.avatar" :name="article.author?.nickname" :size="30" />
              <span>{{ article.author?.nickname }}</span>
            </span>
            <span class="adetail__dot">·</span>
            <time>{{ formatDateTime(article.publishedAt || article.createdAt) }}</time>
            <span class="adetail__stats">
              <span><UiIcon name="eye" :size="15" />{{ formatNumber(article.viewCount) }}</span>
              <span><UiIcon name="heart" :size="15" />{{ formatNumber(likeCount) }}</span>
              <span><UiIcon name="bookmark" :size="15" />{{ formatNumber(collectCount) }}</span>
              <span><UiIcon name="message-circle" :size="15" />{{ formatNumber(article.commentCount) }}</span>
            </span>
          </div>
        </header>

        <!-- 作者可见的审核意见 -->
        <div v-if="isOwner && article.rejectReason" class="notice notice--error">
          <UiIcon name="alert-circle" :size="18" />
          <div>
            <strong>这篇文章被驳回</strong>
            <p>{{ article.rejectReason }}</p>
          </div>
        </div>
        <div v-if="isOwner && article.offlineReason" class="notice notice--warn">
          <UiIcon name="alert-triangle" :size="18" />
          <div>
            <strong>这篇文章已被下架</strong>
            <p>{{ article.offlineReason }}</p>
          </div>
        </div>
        <div v-if="isOwner && article.status === 'PENDING'" class="notice notice--info">
          <UiIcon name="clock" :size="18" />
          <div>
            <strong>正在等待管理员审核</strong>
            <p>通过后文章会立即对游客可见；如需修改，可以先撤回。</p>
          </div>
        </div>

        <img v-if="article.cover" class="adetail__cover" :src="article.cover" :alt="article.title" />

        <MarkdownView :source="article.contentMd" />

        <div v-if="article.tags && article.tags.length" class="adetail__tags">
          <span v-for="t in article.tags" :key="t" class="adetail__tag"># {{ t }}</span>
        </div>

        <!-- 作者操作 -->
        <div v-if="ownerActionsVisible" class="adetail__owner">
          <span class="adetail__owner-label">作者操作</span>
          <div class="adetail__owner-btns">
            <UiButton v-if="canEdit" variant="secondary" :to="{ name: 'editor', params: { id: article.id } }" icon="edit">
              编辑
            </UiButton>
            <UiButton v-if="canSubmit" variant="primary" @click="submitArticle">提交审核</UiButton>
            <UiButton v-if="canWithdraw" variant="secondary" @click="withdrawArticle">撤回审核</UiButton>
            <UiButton variant="ghost" @click="removeArticle"><UiIcon name="trash" :size="16" />删除</UiButton>
          </div>
        </div>

        <!-- 互动 -->
        <div class="adetail__actions">
          <UiButton
            :variant="liked ? 'primary' : 'secondary'"
            :icon="liked ? 'heart' : 'heart'"
            :loading="busyLike"
            @click="toggleLike"
          >
            点赞 {{ likeCount }}
          </UiButton>
          <UiButton
            :variant="collected ? 'primary' : 'secondary'"
            icon="bookmark"
            :loading="busyCollect"
            @click="toggleCollect"
          >
            {{ collected ? '已收藏' : '收藏' }} {{ collectCount }}
          </UiButton>
        </div>
      </article>

      <!-- 评论区 -->
      <section class="comments">
        <h2 class="display-3 comments__h">评论 <span class="comments__n">{{ comments.length }}</span></h2>

        <div v-if="store.isLoggedIn" class="comments__form">
          <UiTextarea v-model="commentText" :rows="3" placeholder="写下你的看法…（新评论需管理员审核通过后才会公开）" />
          <div class="comments__form-foot">
            <span class="caption">评论会先进入待审核队列</span>
            <UiButton variant="primary" :loading="posting" @click="submitComment()">发表评论</UiButton>
          </div>
        </div>
        <div v-else class="comments__login">
          <UiIcon name="message-circle" :size="18" />
          <span>登录后才能评论 ·</span>
          <RouterLink class="link" :to="{ name: 'login', query: { redirect: route.fullPath } }">去登录</RouterLink>
        </div>

        <div v-if="comments.length" class="comments__list">
          <div v-for="c in comments" :key="c.id" class="comment">
            <div class="comment__head">
              <UiAvatar :src="c.author?.avatar" :name="c.author?.nickname" :size="34" />
              <div class="comment__who">
                <span class="comment__name">{{ c.author?.nickname }}</span>
                <span class="caption">{{ fromNow(c.createdAt) }}</span>
              </div>
              <button
                v-if="store.user && c.author?.id === store.user.id"
                class="comment__del"
                type="button"
                @click="removeComment(c)"
              >
                删除
              </button>
            </div>
            <p class="comment__text">{{ c.content }}</p>

            <div class="comment__replies" v-if="c.children && c.children.length">
              <div v-for="r in c.children" :key="r.id" class="comment comment--reply">
                <div class="comment__head">
                  <UiAvatar :src="r.author?.avatar" :name="r.author?.nickname" :size="28" />
                  <div class="comment__who">
                    <span class="comment__name">{{ r.author?.nickname }}</span>
                    <span class="caption">{{ fromNow(r.createdAt) }}</span>
                  </div>
                  <button
                    v-if="store.user && r.author?.id === store.user.id"
                    class="comment__del"
                    type="button"
                    @click="removeComment(r)"
                  >
                    删除
                  </button>
                </div>
                <p class="comment__text">{{ r.content }}</p>
              </div>
            </div>

            <button
              v-if="store.isLoggedIn"
              class="comment__reply-btn"
              type="button"
              @click="replyTo = replyTo === c.id ? null : c.id"
            >
              {{ replyTo === c.id ? '取消回复' : '回复' }}
            </button>

            <div v-if="replyTo === c.id" class="comment__reply-form">
              <UiTextarea v-model="replyText" :rows="2" :placeholder="`回复 @${c.author?.nickname}`" />
              <div class="comments__form-foot">
                <UiButton variant="primary" size="sm" :loading="posting" @click="submitComment(c.id)">
                  提交回复
                </UiButton>
              </div>
            </div>
          </div>
        </div>
        <UiEmpty v-else title="还没有评论" description="成为第一个留下想法的人。" />
      </section>
    </template>
  </div>
</template>

<style scoped>
.adetail {
  padding-top: var(--sp-xl);
  padding-bottom: var(--sp-xxl);
  max-width: 820px;
}
.adetail__back {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 0.875rem;
  color: var(--text-muted);
  margin-bottom: var(--sp-lg);
}
.adetail__back:hover {
  color: var(--text);
}
.adetail__head {
  margin-bottom: var(--sp-lg);
}
.adetail__meta-top {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 14px;
}
.adetail__cat {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 0.8125rem;
  color: var(--primary);
}
.adetail__title {
  margin-bottom: 18px;
}
.adetail__meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  color: var(--text-subtle);
  font-size: 0.875rem;
}
.adetail__author {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--text-muted);
}
.adetail__dot {
  opacity: 0.6;
}
.adetail__stats {
  display: inline-flex;
  gap: 14px;
  margin-left: auto;
}
.adetail__stats span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}
.adetail__cover {
  width: 100%;
  border-radius: var(--r-xl);
  margin: var(--sp-lg) 0;
  border: 1px solid var(--border);
}
.adetail__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: var(--sp-xl) 0;
}
.adetail__tag {
  font-size: 0.875rem;
  color: var(--text-muted);
  padding: 4px 12px;
  border: 1px solid var(--border);
  border-radius: var(--r-pill);
}
.adetail__owner {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--r-lg);
  margin-bottom: var(--sp-lg);
  flex-wrap: wrap;
}
.adetail__owner-label {
  font-size: 0.75rem;
  letter-spacing: 1px;
  text-transform: uppercase;
  color: var(--text-subtle);
}
.adetail__owner-btns {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.adetail__actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  padding: var(--sp-xl) 0;
  border-top: 1px solid var(--border);
  flex-wrap: wrap;
}

.notice {
  display: flex;
  gap: 12px;
  padding: 14px 18px;
  border-radius: var(--r-lg);
  margin-bottom: var(--sp-md);
  border: 1px solid var(--border);
  background: var(--surface);
}
.notice p {
  margin-top: 3px;
  font-size: 0.875rem;
  color: var(--text-muted);
}
.notice strong {
  color: var(--text);
  font-size: 0.9375rem;
}
.notice--error {
  border-color: color-mix(in srgb, var(--error) 40%, var(--border));
  color: var(--error);
}
.notice--warn {
  border-color: color-mix(in srgb, var(--warning) 40%, var(--border));
  color: var(--warning);
}
.notice--info {
  border-color: color-mix(in srgb, var(--primary) 40%, var(--border));
  color: var(--primary);
}

/* 评论 */
.comments {
  margin-top: var(--sp-xxl);
  padding-top: var(--sp-xl);
  border-top: 1px solid var(--border);
}
.comments__h {
  margin-bottom: var(--sp-lg);
}
.comments__n {
  font-family: var(--font-body);
  font-size: 1rem;
  color: var(--text-subtle);
}
.comments__form {
  margin-bottom: var(--sp-xl);
}
.comments__form-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
  gap: 12px;
}
.comments__login {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 18px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--r-lg);
  color: var(--text-muted);
  font-size: 0.9375rem;
  margin-bottom: var(--sp-xl);
}
.link {
  color: var(--primary);
}
.comments__list {
  display: flex;
  flex-direction: column;
  gap: var(--sp-lg);
}
.comment {
  padding: 18px 20px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--r-lg);
}
.comment--reply {
  background: transparent;
  border: 0;
  border-left: 2px solid var(--border);
  border-radius: 0;
  padding: 10px 0 0 16px;
}
.comment__head {
  display: flex;
  align-items: center;
  gap: 10px;
}
.comment__who {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.comment__name {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--text);
}
.comment__del {
  margin-left: auto;
  border: 0;
  background: transparent;
  color: var(--text-subtle);
  font-size: 0.8125rem;
}
.comment__del:hover {
  color: var(--error);
}
.comment__text {
  margin-top: 10px;
  font-size: 0.9375rem;
  line-height: 1.7;
  color: var(--text-body);
  white-space: pre-wrap;
}
.comment__replies {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.comment__reply-btn {
  margin-top: 10px;
  border: 0;
  background: transparent;
  color: var(--primary);
  font-size: 0.8125rem;
  padding: 0;
}
.comment__reply-form {
  margin-top: 10px;
}
</style>