<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '@/api/admin'
import {
  AUDIT_ACTION_OPTIONS,
  AUDIT_TARGET_OPTIONS,
  auditActionLabel,
  auditActionVariant,
  auditTargetLabel,
  formatDateTime
} from '@/utils/format'

const targetType = ref('')
const action = ref('')
const keyword = ref('')
const current = ref(1)
const size = 10

const records = ref([])
const total = ref(0)
const loading = ref(true)

async function load() {
  loading.value = true
  try {
    const page = await adminApi.auditLogs({
      targetType: targetType.value || undefined,
      action: action.value || undefined,
      keyword: keyword.value.trim() || undefined,
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

onMounted(load)
</script>

<template>
  <div>
    <div class="a-head">
      <div>
        <h1>审核与操作记录</h1>
        <p class="caption">记录操作者、目标内容、结果、说明和时间；日志只追加不修改。</p>
      </div>
      <UiButton variant="secondary" size="sm" icon="activity" @click="load">刷新</UiButton>
    </div>

    <div class="a-toolbar">
      <div class="a-toolbar__grow">
        <UiInput v-model="keyword" icon="search" placeholder="搜索目标标题 / 操作者 / 说明…" @enter="search()" />
      </div>
      <UiSelect v-model="targetType" :options="AUDIT_TARGET_OPTIONS" placeholder="全部类型" @update:model-value="search()" />
      <UiSelect v-model="action" :options="AUDIT_ACTION_OPTIONS" placeholder="全部动作" @update:model-value="search()" />
      <UiButton variant="primary" icon="search" @click="search()">查询</UiButton>
    </div>

    <div class="a-panel">
      <div class="a-table-wrap">
        <table class="a-table">
          <thead>
            <tr>
              <th style="width: 56px">ID</th>
              <th>时间</th>
              <th>操作者</th>
              <th>动作</th>
              <th>目标类型</th>
              <th>目标内容</th>
              <th>结果</th>
              <th>说明</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="l in records" :key="l.id">
              <td class="cell-num">#{{ l.id }}</td>
              <td class="cell-muted">{{ formatDateTime(l.createdAt) }}</td>
              <td class="cell-strong">{{ l.operatorName }}</td>
              <td>
                <UiBadge :variant="auditActionVariant(l.action)" size="sm">{{ auditActionLabel(l.action) }}</UiBadge>
              </td>
              <td class="cell-muted">{{ auditTargetLabel(l.targetType) }}</td>
              <td class="cell-clamp">
                <span class="clamp-2">{{ l.targetTitle || `#${l.targetId}` }}</span>
              </td>
              <td class="cell-muted">{{ l.result === 'SUCCESS' ? '成功' : l.result }}</td>
              <td class="cell-muted cell-clamp">
                <span class="clamp-2">{{ l.remark || '—' }}</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <UiEmpty v-if="!loading && !records.length" title="没有匹配的记录" description="换个筛选条件试试。" />
      <UiSpinner v-if="loading" label="加载中…" />
    </div>

    <div class="a-pager">
      <UiPagination :current="current" :total="total" :size="size" @change="onPage" />
    </div>
  </div>
</template>

<style scoped>
.cell-clamp {
  max-width: 260px;
}
</style>