<script setup>
import { ref, onMounted } from 'vue'
import { myApi, interactionApi } from '@/api/articles'
import { toast, confirmDialog } from '@/composables/feedback'
import ArticleCard from '@/components/portal/ArticleCard.vue'

const current = ref(1)
const size = 9
const records = ref([])
const total = ref(0)
const loading = ref(true)

async function load() {
  loading.value = true
  try {
    const page = await myApi.collects({ current: current.value, size })
    records.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}

function onPage(p) {
  current.value = p
  load()
}

async function uncollect(a) {
  const ok = await confirmDialog({ title: '取消收藏', content: `将《${a.title}》从收藏中移除？`, confirmText: '取消收藏' })
  if (!ok) return
  await interactionApi.toggleCollect(a.id)
  toast.success('已取消收藏')
  await load()
}

onMounted(load)
</script>

<template>
  <div>
    <header class="uc-head">
      <h1 class="display-3">我的收藏</h1>
      <p class="caption">共 {{ total }} 篇</p>
    </header>

    <div v-if="loading" class="grid3">
      <UiSkeleton v-for="i in 3" :key="i" height="320px" radius="12px" />
    </div>

    <template v-else-if="records.length">
      <div class="grid3">
        <div v-for="a in records" :key="a.id" class="collect">
          <ArticleCard :article="a" />
          <button class="collect__off" type="button" @click="uncollect(a)">
            <UiIcon name="bookmark" :size="14" />取消收藏
          </button>
        </div>
      </div>
      <div class="pager"><UiPagination :current="current" :total="total" :size="size" @change="onPage" /></div>
    </template>

    <UiEmpty v-else icon="bookmark" title="还没有收藏" description="在文章详情页点「收藏」，这里就会显示。">
      <UiButton variant="primary" :to="{ name: 'articles' }">去逛逛</UiButton>
    </UiEmpty>
  </div>
</template>

<style scoped>
.uc-head {
  margin-bottom: var(--sp-lg);
}
.uc-head .caption {
  margin-top: 6px;
}
.grid3 {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--sp-lg);
}
.collect {
  position: relative;
  display: flex;
  flex-direction: column;
}
.collect__off {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 10px;
  padding: 8px;
  border: 1px solid var(--border);
  border-radius: var(--r-md);
  background: transparent;
  color: var(--text-muted);
  font-size: 0.8125rem;
  font-family: inherit;
}
.collect__off:hover {
  border-color: var(--error);
  color: var(--error);
}
.pager {
  margin-top: var(--sp-xl);
}
@media (max-width: 700px) {
  .grid3 {
    grid-template-columns: 1fr;
  }
}
</style>