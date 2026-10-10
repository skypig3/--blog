<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '@/api/admin'
import { confirmDialog, toast } from '@/composables/feedback'

const categories = ref([])
const tags = ref([])
const loading = ref(true)

const catModal = ref(false)
const catForm = ref({ id: null, name: '', sortOrder: 0 })
const savingCat = ref(false)

const tagModal = ref(false)
const tagForm = ref({ id: null, name: '' })
const savingTag = ref(false)

async function load() {
  loading.value = true
  try {
    const [cats, tgs] = await Promise.all([adminApi.categories(), adminApi.tags()])
    categories.value = cats
    tags.value = tgs
  } finally {
    loading.value = false
  }
}

function openCat(c = null) {
  catForm.value = c ? { id: c.id, name: c.name, sortOrder: c.sortOrder ?? 0 } : { id: null, name: '', sortOrder: 0 }
  catModal.value = true
}

async function saveCat() {
  if (!catForm.value.name.trim()) {
    toast.warning('分类名不能为空')
    return
  }
  savingCat.value = true
  try {
    const payload = { name: catForm.value.name.trim(), sortOrder: Number(catForm.value.sortOrder) || 0 }
    if (catForm.value.id) await adminApi.updateCategory(catForm.value.id, payload)
    else await adminApi.createCategory(payload)
    toast.success(catForm.value.id ? '分类已更新' : '分类已创建')
    catModal.value = false
    await load()
  } finally {
    savingCat.value = false
  }
}

async function removeCat(c) {
  const ok = await confirmDialog({
    title: '删除分类',
    content: `删除分类「${c.name}」？若该分类下还有已发布文章，后端会拒绝删除。`,
    confirmText: '删除',
    danger: true
  })
  if (!ok) return
  await adminApi.deleteCategory(c.id)
  toast.success('分类已删除')
  await load()
}

function openTag(t = null) {
  tagForm.value = t ? { id: t.id, name: t.name } : { id: null, name: '' }
  tagModal.value = true
}

async function saveTag() {
  if (!tagForm.value.name.trim()) {
    toast.warning('标签名不能为空')
    return
  }
  savingTag.value = true
  try {
    const payload = { name: tagForm.value.name.trim() }
    if (tagForm.value.id) await adminApi.updateTag(tagForm.value.id, payload)
    else await adminApi.createTag(payload)
    toast.success(tagForm.value.id ? '标签已更新' : '标签已创建')
    tagModal.value = false
    await load()
  } finally {
    savingTag.value = false
  }
}

async function removeTag(t) {
  const ok = await confirmDialog({
    title: '删除标签',
    content: `删除标签「${t.name}」？仍被文章引用时后端会拒绝删除。`,
    confirmText: '删除',
    danger: true
  })
  if (!ok) return
  await adminApi.deleteTag(t.id)
  toast.success('标签已删除')
  await load()
}

onMounted(load)
</script>

<template>
  <div>
    <div class="a-head">
      <div>
        <h1>分类与标签</h1>
        <p class="caption">分类决定导航结构，标签用于交叉检索；删除受引用关系保护。</p>
      </div>
      <UiButton variant="secondary" size="sm" icon="activity" @click="load">刷新</UiButton>
    </div>

    <div class="a-grid-2">
      <!-- 分类 -->
      <div class="a-panel">
        <div class="a-panel__head">
          <span class="a-panel__title">分类（{{ categories.length }}）</span>
          <UiButton variant="primary" size="sm" icon="plus" @click="openCat()">新建分类</UiButton>
        </div>
        <div class="a-table-wrap">
          <table class="a-table">
            <thead>
              <tr>
                <th>名称</th>
                <th>排序</th>
                <th>已发布文章</th>
                <th style="text-align: right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="c in categories" :key="c.id">
                <td class="cell-strong">{{ c.name }}</td>
                <td class="cell-num">{{ c.sortOrder }}</td>
                <td class="cell-num">{{ c.articleCount }}</td>
                <td>
                  <div class="a-actions">
                    <UiButton variant="ghost" size="sm" @click="openCat(c)">编辑</UiButton>
                    <UiButton variant="ghost" size="sm" @click="removeCat(c)">删除</UiButton>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <UiEmpty v-if="!loading && !categories.length" title="还没有分类" />
        <UiSpinner v-if="loading" label="加载中…" />
      </div>

      <!-- 标签 -->
      <div class="a-panel">
        <div class="a-panel__head">
          <span class="a-panel__title">标签（{{ tags.length }}）</span>
          <UiButton variant="primary" size="sm" icon="plus" @click="openTag()">新建标签</UiButton>
        </div>
        <div class="a-table-wrap">
          <table class="a-table">
            <thead>
              <tr>
                <th>名称</th>
                <th>已发布文章</th>
                <th style="text-align: right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="t in tags" :key="t.id">
                <td class="cell-strong"># {{ t.name }}</td>
                <td class="cell-num">{{ t.articleCount }}</td>
                <td>
                  <div class="a-actions">
                    <UiButton variant="ghost" size="sm" @click="openTag(t)">编辑</UiButton>
                    <UiButton variant="ghost" size="sm" @click="removeTag(t)">删除</UiButton>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <UiEmpty v-if="!loading && !tags.length" title="还没有标签" />
      </div>
    </div>

    <!-- 分类弹窗 -->
    <UiModal v-model="catModal" :title="catForm.id ? '编辑分类' : '新建分类'" size="sm">
      <div class="stack">
        <UiField label="分类名" required>
          <UiInput v-model="catForm.name" placeholder="例如：技术笔记" :maxlength="30" />
        </UiField>
        <UiField label="排序值" hint="数字越小越靠前">
          <UiInput v-model="catForm.sortOrder" type="number" placeholder="0" />
        </UiField>
      </div>
      <template #footer>
        <UiButton variant="ghost" @click="catModal = false">取消</UiButton>
        <UiButton variant="primary" :loading="savingCat" @click="saveCat">保存</UiButton>
      </template>
    </UiModal>

    <!-- 标签弹窗 -->
    <UiModal v-model="tagModal" :title="tagForm.id ? '编辑标签' : '新建标签'" size="sm">
      <UiField label="标签名" required>
        <UiInput v-model="tagForm.name" placeholder="例如：Vue" :maxlength="30" @enter="saveTag" />
      </UiField>
      <template #footer>
        <UiButton variant="ghost" @click="tagModal = false">取消</UiButton>
        <UiButton variant="primary" :loading="savingTag" @click="saveTag">保存</UiButton>
      </template>
    </UiModal>
  </div>
</template>

<style scoped>
.stack {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
</style>