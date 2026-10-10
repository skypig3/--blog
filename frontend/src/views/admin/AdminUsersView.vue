<script setup>
import { ref, computed, onMounted } from 'vue'
import { adminApi } from '@/api/admin'
import { useUserStore } from '@/stores/user'
import { ROLE_OPTIONS, USER_STATUS_OPTIONS, roleLabel, roleVariant, userStatus, formatDateTime } from '@/utils/format'
import { confirmDialog, promptDialog, toast } from '@/composables/feedback'

const store = useUserStore()

const keyword = ref('')
const role = ref('')
const status = ref('')
const current = ref(1)
const size = 10

const records = ref([])
const total = ref(0)
const loading = ref(true)
const busyId = ref(null)

const detailOpen = ref(false)
const detail = ref(null)

async function load() {
  loading.value = true
  try {
    const page = await adminApi.users({
      keyword: keyword.value.trim() || undefined,
      role: role.value || undefined,
      status: status.value || undefined,
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

function isSelf(u) {
  return store.user?.id === u.id
}

async function toggleStatus(u) {
  if (isSelf(u)) {
    toast.warning('不能禁用自己')
    return
  }
  const disabling = u.status === 'ACTIVE'
  let remark = ''
  if (disabling) {
    const input = await promptDialog({
      title: '禁用账号',
      content: `禁用后 ${u.nickname}（@${u.username}）将无法登录，已有会话也会被立即挡掉。可填写理由。`,
      inputLabel: '禁用理由（选填）',
      confirmText: '禁用',
      danger: true,
      required: false
    })
    if (input === null) return
    remark = input
  } else {
    const ok = await confirmDialog({ title: '恢复账号', content: `恢复 ${u.nickname}（@${u.username}）的登录权限？`, confirmText: '恢复' })
    if (!ok) return
  }
  busyId.value = u.id
  try {
    await adminApi.setUserStatus(u.id, { status: disabling ? 'DISABLED' : 'ACTIVE', remark })
    toast.success(disabling ? '已禁用' : '已恢复')
    await load()
  } finally {
    busyId.value = null
  }
}

async function toggleRole(u) {
  if (isSelf(u)) {
    toast.warning('不能修改自己的角色')
    return
  }
  const toAdmin = u.role !== 'ADMIN'
  const ok = await confirmDialog({
    title: toAdmin ? '设为管理员' : '取消管理员',
    content: toAdmin
      ? `将 ${u.nickname}（@${u.username}）提升为管理员？该账号将能进入后台审核内容。`
      : `将 ${u.nickname}（@${u.username}）降级为普通用户？`,
    confirmText: toAdmin ? '设为管理员' : '确认降级'
  })
  if (!ok) return
  busyId.value = u.id
  try {
    await adminApi.setUserRole(u.id, { role: toAdmin ? 'ADMIN' : 'USER' })
    toast.success('角色已更新')
    await load()
  } finally {
    busyId.value = null
  }
}

async function openDetail(u) {
  detailOpen.value = true
  detail.value = u
  try {
    detail.value = await adminApi.userDetail(u.id)
  } catch {
    /* 保留列表里的数据 */
  }
}

const selfId = computed(() => store.user?.id)

onMounted(load)
</script>

<template>
  <div>
    <div class="a-head">
      <div>
        <h1>用户管理</h1>
        <p class="caption">接口与页面都不会返回密码或密码哈希；系统会保护最后一个启用状态的管理员。</p>
      </div>
      <UiButton variant="secondary" size="sm" icon="activity" @click="load">刷新</UiButton>
    </div>

    <div class="a-toolbar">
      <div class="a-toolbar__grow">
        <UiInput v-model="keyword" icon="search" placeholder="搜索用户名 / 邮箱 / 昵称…" @enter="search()" />
      </div>
      <UiSelect v-model="role" :options="ROLE_OPTIONS" placeholder="全部角色" @update:model-value="search()" />
      <UiSelect v-model="status" :options="USER_STATUS_OPTIONS" placeholder="全部状态" @update:model-value="search()" />
      <UiButton variant="primary" icon="search" @click="search()">查询</UiButton>
    </div>

    <div class="a-panel">
      <div class="a-table-wrap">
        <table class="a-table">
          <thead>
            <tr>
              <th style="width: 56px">ID</th>
              <th>用户名</th>
              <th>昵称</th>
              <th>邮箱</th>
              <th>角色</th>
              <th>状态</th>
              <th>注册时间</th>
              <th style="text-align: right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="u in records" :key="u.id">
              <td class="cell-num">#{{ u.id }}</td>
              <td class="cell-strong">{{ u.username }}</td>
              <td>{{ u.nickname }}</td>
              <td class="cell-muted">{{ u.email }}</td>
              <td>
                <UiBadge :variant="roleVariant(u.role)" size="sm">{{ roleLabel(u.role) }}</UiBadge>
              </td>
              <td>
                <UiBadge :variant="userStatus(u.status).variant" size="sm" dot>
                  {{ userStatus(u.status).label }}
                </UiBadge>
              </td>
              <td class="cell-muted">{{ formatDateTime(u.createdAt) }}</td>
              <td>
                <div class="a-actions">
                  <UiButton variant="ghost" size="sm" @click="openDetail(u)">查看</UiButton>
                  <UiButton
                    variant="secondary"
                    size="sm"
                    :disabled="isSelf(u)"
                    :loading="busyId === u.id"
                    @click="toggleStatus(u)"
                  >
                    {{ u.status === 'ACTIVE' ? '禁用' : '恢复' }}
                  </UiButton>
                  <UiButton variant="ghost" size="sm" :disabled="isSelf(u)" @click="toggleRole(u)">
                    {{ u.role === 'ADMIN' ? '取消管理员' : '设为管理员' }}
                  </UiButton>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <UiEmpty v-if="!loading && !records.length" title="没有符合条件的用户" />
      <UiSpinner v-if="loading" label="加载中…" />
    </div>

    <div class="a-pager">
      <UiPagination :current="current" :total="total" :size="size" @change="onPage" />
    </div>

    <UiModal v-model="detailOpen" title="用户资料" size="sm">
      <div v-if="detail" class="udet">
        <div class="udet__top">
          <UiAvatar :src="detail.avatar" :name="detail.nickname" :size="48" />
          <div>
            <p class="udet__name">{{ detail.nickname }}</p>
            <p class="caption">@{{ detail.username }}</p>
          </div>
        </div>
        <dl class="udet__list">
          <div><dt>用户 ID</dt><dd>#{{ detail.id }}</dd></div>
          <div><dt>邮箱</dt><dd>{{ detail.email }}</dd></div>
          <div><dt>角色</dt><dd>{{ roleLabel(detail.role) }}</dd></div>
          <div><dt>状态</dt><dd>{{ userStatus(detail.status).label }}</dd></div>
          <div><dt>注册时间</dt><dd>{{ formatDateTime(detail.createdAt) }}</dd></div>
          <div><dt>个人简介</dt><dd>{{ detail.bio || '—' }}</dd></div>
        </dl>
      </div>
      <template #footer>
        <UiButton variant="ghost" @click="detailOpen = false">关闭</UiButton>
      </template>
    </UiModal>
  </div>
</template>

<style scoped>
.udet__top {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border);
}
.udet__name {
  font-family: var(--font-display);
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--text);
}
.udet__list {
  margin: 16px 0 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.udet__list > div {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  font-size: 0.875rem;
}
.udet__list dt {
  color: var(--text-subtle);
}
.udet__list dd {
  margin: 0;
  color: var(--text-body);
  text-align: right;
}
</style>