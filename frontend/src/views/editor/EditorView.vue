<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { articleApi, myApi } from '@/api/articles'
import { taxonomyApi } from '@/api/site'
import { articleStatus } from '@/utils/format'
import { plainExcerpt } from '@/utils/markdown'
import { toast, confirmDialog } from '@/composables/feedback'
import ImageUploader from '@/components/portal/ImageUploader.vue'

const route = useRoute()
const router = useRouter()

const articleId = computed(() => route.params.id)

const form = ref({
  title: '',
  summary: '',
  contentMd: '',
  cover: '',
  categoryId: '',
  tagIds: []
})

const categories = ref([])
const tags = ref([])
const status = ref('DRAFT')
const rejectReason = ref('')
const offlineReason = ref('')
const loading = ref(false)
const saving = ref(false)
const tab = ref('edit') // edit | preview

const isNew = computed(() => !articleId.value)
const canSubmit = computed(() => ['DRAFT', 'REJECTED', 'REMOVED', 'PUBLISHED'].includes(status.value))
const canWithdraw = computed(() => status.value === 'PENDING')

async function loadArticle() {
  loading.value = true
  try {
    const a = await myApi.article(articleId.value)
    form.value = {
      title: a.title,
      summary: a.summary || '',
      contentMd: a.contentMd || '',
      cover: a.cover || '',
      categoryId: a.categoryId ?? '',
      tagIds: (a.tags || []).map((name) => tags.value.find((t) => t.name === name)?.id).filter(Boolean)
    }
    status.value = a.status
    rejectReason.value = a.rejectReason || ''
    offlineReason.value = a.offlineReason || ''
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  const [cats, tgs] = await Promise.all([
    taxonomyApi.categories().catch(() => []),
    taxonomyApi.tags().catch(() => [])
  ])
  categories.value = cats || []
  tags.value = tgs || []
  if (articleId.value) await loadArticle()
})

function toggleTag(id) {
  const i = form.value.tagIds.indexOf(id)
  if (i >= 0) form.value.tagIds.splice(i, 1)
  else form.value.tagIds.push(id)
}

function validate() {
  if (!form.value.title.trim()) {
    toast.warning('标题不能为空')
    return false
  }
  if (!form.value.contentMd.trim()) {
    toast.warning('正文不能为空')
    return false
  }
  return true
}

async function save(submit) {
  if (!validate()) return
  saving.value = true
  try {
    const payload = {
      title: form.value.title.trim(),
      summary: form.value.summary.trim(),
      contentMd: form.value.contentMd,
      cover: form.value.cover,
      categoryId: form.value.categoryId || null,
      tagIds: form.value.tagIds,
      submit: !!submit
    }
    const saved = articleId.value
      ? await articleApi.update(articleId.value, payload)
      : await articleApi.create(payload)

    toast.success(submit ? '已提交审核' : '草稿已保存')

    if (isNew.value && saved?.id) {
      router.replace({ name: 'editor', params: { id: saved.id } })
    }
    status.value = saved?.status || status.value
    rejectReason.value = saved?.rejectReason || ''
  } finally {
    saving.value = false
  }
}

async function withdraw() {
  const res = await articleApi.withdraw(articleId.value)
  status.value = res.status
  toast.success('已撤回，回到草稿')
}

async function removeArticle() {
  const ok = await confirmDialog({
    title: '删除文章',
    content: '确定要删除这篇文章吗？删除后不会再出现在任何列表里。',
    confirmText: '删除',
    danger: true
  })
  if (!ok) return
  await articleApi.remove(articleId.value)
  toast.success('已删除')
  router.push({ name: 'my-articles' })
}
</script>

<template>
  <div class="container editor">
    <header class="editor__head">
      <div>
        <span class="eyebrow">{{ isNew ? '创作' : '编辑' }}</span>
        <h1 class="display-3">{{ isNew ? '写一篇新文章' : '编辑文章' }}</h1>
      </div>
      <div class="editor__status">
        <UiBadge :variant="articleStatus(status).variant">{{ articleStatus(status).label }}</UiBadge>
      </div>
    </header>

    <div v-if="rejectReason" class="notice notice--error">
      <UiIcon name="alert-circle" :size="18" />
      <div><strong>被驳回</strong><p>{{ rejectReason }}</p></div>
    </div>
    <div v-if="offlineReason" class="notice notice--warn">
      <UiIcon name="alert-triangle" :size="18" />
      <div><strong>已被下架</strong><p>{{ offlineReason }}</p></div>
    </div>

    <div v-if="loading" class="editor__loading"><UiSpinner label="加载文章…" /></div>

    <div v-else class="editor__grid">
      <div class="editor__main">
        <UiField label="标题" required>
          <UiInput v-model="form.title" size="lg" placeholder="给文章起个标题" :maxlength="120" />
        </UiField>

        <UiField label="摘要" hint="留空时后端会从正文自动截取前 120 字">
          <UiTextarea v-model="form.summary" :rows="2" placeholder="一句话概括（可选）" :maxlength="300" />
        </UiField>

        <div class="editor__content block">
          <div class="editor__content-head">
            <span class="editor__label">正文（Markdown）</span>
            <div class="editor__tabs">
              <button
                type="button"
                class="editor__tab"
                :class="{ 'is-active': tab === 'edit' }"
                @click="tab = 'edit'"
              >
                编辑
              </button>
              <button
                type="button"
                class="editor__tab"
                :class="{ 'is-active': tab === 'preview' }"
                @click="tab = 'preview'"
              >
                预览
              </button>
            </div>
          </div>
          <UiTextarea
            v-if="tab === 'edit'"
            v-model="form.contentMd"
            :rows="18"
            placeholder="用 Markdown 写正文，支持标题、列表、代码块、引用…"
          />
          <div v-else class="editor__preview">
            <MarkdownView v-if="form.contentMd" :source="form.contentMd" />
            <p v-else class="text-subtle">还没有内容，切回「编辑」写点什么吧。</p>
          </div>
        </div>

        <div class="editor__actions">
          <UiButton variant="primary" :loading="saving" @click="save(false)">保存草稿</UiButton>
          <UiButton v-if="canSubmit" variant="secondary" :loading="saving" @click="save(true)">
            保存并提交审核
          </UiButton>
          <UiButton v-if="canWithdraw" variant="secondary" @click="withdraw">撤回审核</UiButton>
          <UiButton v-if="!isNew" variant="ghost" @click="removeArticle">
            <UiIcon name="trash" :size="16" />删除
          </UiButton>
          <span class="editor__actions-hint">
            已发布文章被编辑后会退出公开状态，需要重新过审。
          </span>
        </div>
      </div>

      <aside class="editor__side">
        <div class="block">
          <span class="editor__label">封面</span>
          <ImageUploader v-model="form.cover" shape="square" height="150px" />
        </div>

        <div class="block">
          <span class="editor__label">分类</span>
          <UiSelect
            v-model="form.categoryId"
            :options="categories.map((c) => ({ label: c.name, value: c.id }))"
            placeholder="不选分类"
          />
        </div>

        <div class="block">
          <span class="editor__label">标签</span>
          <div class="editor__tags">
            <button
              v-for="t in tags"
              :key="t.id"
              type="button"
              class="editor__tag"
              :class="{ 'is-on': form.tagIds.includes(t.id) }"
              @click="toggleTag(t.id)"
            >
              {{ t.name }}
            </button>
          </div>
          <p v-if="!tags.length" class="caption">暂无标签</p>
        </div>

        <div class="block editor__tip">
          <UiIcon name="info" :size="16" />
          <p>正文最少需要标题与内容；摘要与封面可留空。提交后进入待审核，管理员通过即公开。</p>
        </div>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.editor {
  padding-top: var(--sp-xl);
  padding-bottom: var(--sp-xxl);
}
.editor__head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: var(--sp-lg);
}
.editor__grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: var(--sp-xl);
  align-items: start;
}
.editor__main {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-width: 0;
}
.editor__side {
  display: flex;
  flex-direction: column;
  gap: 20px;
  position: sticky;
  top: calc(var(--nav-h) + 20px);
}
.block {
  padding: 18px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--r-lg);
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.editor__label {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--text);
}
.editor__content-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.editor__tabs {
  display: inline-flex;
  gap: 2px;
  padding: 3px;
  background: var(--surface-3);
  border-radius: var(--r-md);
}
.editor__tab {
  border: 0;
  background: transparent;
  padding: 5px 14px;
  border-radius: var(--r-sm);
  font-size: 0.8125rem;
  color: var(--text-muted);
  font-family: inherit;
}
.editor__tab.is-active {
  background: var(--bg);
  color: var(--text);
  border: 1px solid var(--border);
}
.editor__preview {
  min-height: 320px;
  padding: 20px;
  border: 1px solid var(--border);
  border-radius: var(--r-md);
  background: var(--input-bg);
}
.editor__actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.editor__actions-hint {
  font-size: 0.75rem;
  color: var(--text-subtle);
  flex-basis: 100%;
}
.editor__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.editor__tag {
  padding: 5px 12px;
  border: 1px solid var(--border);
  border-radius: var(--r-pill);
  background: var(--input-bg);
  color: var(--text-muted);
  font-size: 0.8125rem;
  font-family: inherit;
}
.editor__tag.is-on {
  background: var(--primary-soft);
  border-color: var(--primary);
  color: var(--primary);
}
.editor__tip {
  flex-direction: row;
  gap: 10px;
  color: var(--text-subtle);
  font-size: 0.8125rem;
  line-height: 1.6;
}
.editor__tip :deep(.icon) {
  margin-top: 2px;
}
.editor__loading {
  padding: var(--sp-xxl) 0;
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
}
.notice--error {
  border-color: color-mix(in srgb, var(--error) 40%, var(--border));
  color: var(--error);
}
.notice--warn {
  border-color: color-mix(in srgb, var(--warning) 40%, var(--border));
  color: var(--warning);
}

@media (max-width: 960px) {
  .editor__grid {
    grid-template-columns: 1fr;
  }
  .editor__side {
    position: static;
  }
}
</style>