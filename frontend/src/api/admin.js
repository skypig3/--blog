import request from './request'

/** 后台管理接口（全部要求 ADMIN）。 */
export const adminApi = {
  /* 统计与日志 */
  stats: () => request.get('/admin/stats'),
  dashboard: () => request.get('/admin/dashboard'),
  auditLogs: (params) => request.get('/admin/audit-logs', { params }),

  /* 文章 */
  articles: (params) => request.get('/admin/articles', { params }),
  articleDetail: (id) => request.get(`/admin/articles/${id}`),
  auditArticle: (id, payload) => request.post(`/admin/articles/${id}/audit`, payload),
  offlineArticle: (id, payload) => request.post(`/admin/articles/${id}/offline`, payload),
  deleteArticle: (id, payload) =>
    request.delete(`/admin/articles/${id}`, { data: payload || {} }),

  /* 评论 */
  comments: (params) => request.get('/admin/comments', { params }),
  commentDetail: (id) => request.get(`/admin/comments/${id}`),
  auditComment: (id, payload) => request.post(`/admin/comments/${id}/audit`, payload),
  deleteComment: (id) => request.delete(`/admin/comments/${id}`),

  /* 用户 */
  users: (params) => request.get('/admin/users', { params }),
  userDetail: (id) => request.get(`/admin/users/${id}`),
  setUserStatus: (id, payload) => request.patch(`/admin/users/${id}/status`, payload),
  setUserRole: (id, payload) => request.patch(`/admin/users/${id}/role`, payload),

  /* 分类 */
  categories: () => request.get('/admin/categories'),
  createCategory: (payload) => request.post('/admin/categories', payload),
  updateCategory: (id, payload) => request.put(`/admin/categories/${id}`, payload),
  deleteCategory: (id) => request.delete(`/admin/categories/${id}`),

  /* 标签（后端没有单独的 GET /api/admin/tags，列表复用公开接口 /api/tags） */
  tags: () => request.get('/tags'),
  createTag: (payload) => request.post('/admin/tags', payload),
  updateTag: (id, payload) => request.put(`/admin/tags/${id}`, payload),
  deleteTag: (id) => request.delete(`/admin/tags/${id}`)
}