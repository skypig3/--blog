/** 文案与格式化工具：状态映射、时间、数字。文案与后端枚举一一对应。 */

const pad = (n) => String(n).padStart(2, '0')

/** 后端返回 "YYYY-MM-DD HH:mm:ss"（SQLite datetime）。统一格式化为 "YYYY-MM-DD HH:mm"。 */
export function formatDateTime(value, withTime = true) {
  if (!value) return ''
  const s = String(value).replace('T', ' ')
  const date = s.slice(0, 10)
  if (!withTime) return date
  const time = s.slice(11, 16)
  return time ? `${date} ${time}` : date
}

export function formatDate(value) {
  return formatDateTime(value, false)
}

/** 相对时间：刚刚 / N 分钟前 / N 小时前 / N 天前 / 具体日期。 */
export function fromNow(value) {
  if (!value) return ''
  const t = Date.parse(String(value).replace(' ', 'T'))
  if (Number.isNaN(t)) return formatDateTime(value)
  const diff = Date.now() - t
  const min = Math.floor(diff / 60000)
  if (min < 1) return '刚刚'
  if (min < 60) return `${min} 分钟前`
  const hour = Math.floor(min / 60)
  if (hour < 24) return `${hour} 小时前`
  const day = Math.floor(hour / 24)
  if (day < 30) return `${day} 天前`
  return formatDate(value)
}

export function formatNumber(n) {
  const v = Number(n || 0)
  if (v >= 10000) return `${(v / 10000).toFixed(1)}w`
  if (v >= 1000) return `${(v / 1000).toFixed(1)}k`
  return String(v)
}

/** 当前时间的输入框默认值，形如 2026-10-09 */
export function today() {
  const d = new Date()
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

/* ---------------------------------------------------------------- 状态映射 */

const ARTICLE_STATUS = {
  DRAFT: { label: '草稿', variant: 'neutral' },
  PENDING: { label: '待审核', variant: 'warning' },
  PUBLISHED: { label: '已发布', variant: 'success' },
  REJECTED: { label: '已驳回', variant: 'error' },
  REMOVED: { label: '已下架', variant: 'dark' }
}

export function articleStatus(status) {
  return ARTICLE_STATUS[status] || { label: status || '未知', variant: 'neutral' }
}

const COMMENT_STATUS = {
  PENDING: { label: '待审核', variant: 'warning' },
  PUBLISHED: { label: '已通过', variant: 'success' },
  REJECTED: { label: '已驳回', variant: 'error' }
}

export function commentStatus(status) {
  return COMMENT_STATUS[status] || { label: status || '未知', variant: 'neutral' }
}

const USER_STATUS = {
  ACTIVE: { label: '正常', variant: 'success' },
  DISABLED: { label: '已禁用', variant: 'error' }
}

export function userStatus(status) {
  return USER_STATUS[status] || { label: status || '未知', variant: 'neutral' }
}

export function roleLabel(role) {
  return role === 'ADMIN' ? '管理员' : '普通用户'
}

export function roleVariant(role) {
  return role === 'ADMIN' ? 'primary' : 'outline'
}

/* ---------------------------------------------------------------- 日志映射 */

const AUDIT_ACTION = {
  ARTICLE_APPROVE: '通过文章',
  ARTICLE_REJECT: '驳回文章',
  ARTICLE_OFFLINE: '下架文章',
  ARTICLE_DELETE: '删除文章',
  COMMENT_APPROVE: '通过评论',
  COMMENT_REJECT: '驳回评论',
  COMMENT_DELETE: '删除评论',
  USER_DISABLE: '禁用账号',
  USER_ENABLE: '恢复账号',
  USER_ROLE_CHANGE: '变更角色',
  CATEGORY_CREATE: '新建分类',
  CATEGORY_UPDATE: '修改分类',
  CATEGORY_DELETE: '删除分类',
  TAG_CREATE: '新建标签',
  TAG_UPDATE: '修改标签',
  TAG_DELETE: '删除标签'
}

export function auditActionLabel(action) {
  return AUDIT_ACTION[action] || action || ''
}

const AUDIT_TARGET = { ARTICLE: '文章', COMMENT: '评论', USER: '用户', CATEGORY: '分类', TAG: '标签' }

export function auditTargetLabel(target) {
  return AUDIT_TARGET[target] || target || ''
}

/** 日志动作 → 徽章色调 */
export function auditActionVariant(action) {
  if (!action) return 'neutral'
  if (action.endsWith('_APPROVE') || action.endsWith('_ENABLE') || action.endsWith('_CREATE')) {
    return 'success'
  }
  if (action.endsWith('_REJECT') || action.endsWith('_OFFLINE') || action.endsWith('_DELETE') || action.endsWith('_DISABLE')) {
    return 'error'
  }
  return 'info'
}

export const ARTICLE_STATUS_OPTIONS = [
  { label: '草稿', value: 'DRAFT' },
  { label: '待审核', value: 'PENDING' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '已驳回', value: 'REJECTED' },
  { label: '已下架', value: 'REMOVED' }
]

export const COMMENT_STATUS_OPTIONS = [
  { label: '待审核', value: 'PENDING' },
  { label: '已通过', value: 'PUBLISHED' },
  { label: '已驳回', value: 'REJECTED' }
]

export const USER_STATUS_OPTIONS = [
  { label: '正常', value: 'ACTIVE' },
  { label: '已禁用', value: 'DISABLED' }
]

export const ROLE_OPTIONS = [
  { label: '普通用户', value: 'USER' },
  { label: '管理员', value: 'ADMIN' }
]

export const AUDIT_ACTION_OPTIONS = Object.entries(AUDIT_ACTION).map(([value, label]) => ({
  value,
  label
}))

export const AUDIT_TARGET_OPTIONS = Object.entries(AUDIT_TARGET).map(([value, label]) => ({
  value,
  label
}))