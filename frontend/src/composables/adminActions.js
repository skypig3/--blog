import { adminApi } from '@/api/admin'
import { confirmDialog, promptDialog, toast } from '@/composables/feedback'

/**
 * 后台审核/管理动作的统一封装。
 * 驳回、下架必须填写原因（与后端 ArticleDto.Audit / Offline 的校验一致）；
 * 每个动作成功都刷新列表并触发侧栏角标刷新（由页面负责 reload，这里只返回 true/false）。
 */

export async function approveArticle(article) {
  const ok = await confirmDialog({
    title: '审核通过',
    content: `确定通过《${article.title}》吗？通过后文章立即对游客可见。`,
    confirmText: '通过'
  })
  if (!ok) return false
  await adminApi.auditArticle(article.id, { pass: true })
  toast.success('已通过审核')
  return true
}

export async function rejectArticle(article) {
  const reason = await promptDialog({
    title: '驳回文章',
    content: `驳回《${article.title}》，请填写原因（作者可见）。`,
    inputLabel: '驳回原因',
    inputPlaceholder: '例如：内容与主题不符，请补充示例后再提交',
    confirmText: '驳回',
    danger: true
  })
  if (reason === null) return false
  await adminApi.auditArticle(article.id, { pass: false, reason })
  toast.success('已驳回')
  return true
}

export async function offlineArticle(article) {
  const reason = await promptDialog({
    title: '下架文章',
    content: `下架《${article.title}》，下架后前台不再展示，请填写原因（作者可见）。`,
    inputLabel: '下架原因',
    inputPlaceholder: '例如：含违规信息',
    confirmText: '下架',
    danger: true
  })
  if (reason === null) return false
  await adminApi.offlineArticle(article.id, { reason })
  toast.success('已下架')
  return true
}

export async function deleteArticle(article) {
  const remark = await promptDialog({
    title: '删除文章',
    content: `删除《${article.title}》（逻辑删除，数据保留）。可填写说明，留空则直接删除。`,
    inputLabel: '删除说明（选填）',
    inputPlaceholder: '例如：重复投稿',
    confirmText: '删除',
    danger: true,
    required: false
  })
  if (remark === null) return false
  await adminApi.deleteArticle(article.id, remark ? { remark } : {})
  toast.success('已删除')
  return true
}

export async function approveComment(comment) {
  const ok = await confirmDialog({
    title: '通过评论',
    content: '通过后评论立即在前台可见，并计入文章的评论数。',
    confirmText: '通过'
  })
  if (!ok) return false
  await adminApi.auditComment(comment.id, { pass: true })
  toast.success('已通过审核')
  return true
}

export async function rejectComment(comment) {
  const reason = await promptDialog({
    title: '驳回评论',
    content: '请填写驳回原因（评论作者可见）。',
    inputLabel: '驳回原因',
    inputPlaceholder: '例如：包含不当言论',
    confirmText: '驳回',
    danger: true
  })
  if (reason === null) return false
  await adminApi.auditComment(comment.id, { pass: false, reason })
  toast.success('已驳回')
  return true
}

export async function deleteComment(comment) {
  const ok = await confirmDialog({
    title: '删除评论',
    content: '删除后不可恢复，确定删除这条评论吗？',
    confirmText: '删除',
    danger: true
  })
  if (!ok) return false
  await adminApi.deleteComment(comment.id)
  toast.success('已删除')
  return true
}