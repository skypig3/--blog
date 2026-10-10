import request from './request'

/** 文章：公开浏览 + 作者创作。 */
export const articleApi = {
  /** 公开列表：keyword / categoryId / tagId / sort(latest|hot) / current / size */
  list: (params) => request.get('/articles', { params }),
  hot: () => request.get('/articles/hot'),
  detail: (id) => request.get(`/articles/${id}`),

  create: (payload) => request.post('/articles', payload),
  update: (id, payload) => request.put(`/articles/${id}`, payload),
  remove: (id) => request.delete(`/articles/${id}`),
  submit: (id) => request.post(`/articles/${id}/submit`),
  withdraw: (id) => request.post(`/articles/${id}/withdraw`)
}

/** 个人中心：我的文章 / 评论 / 收藏。 */
export const myApi = {
  articles: (params) => request.get('/my/articles', { params }),
  article: (id) => request.get(`/my/articles/${id}`),
  comments: (params) => request.get('/my/comments', { params }),
  collects: (params) => request.get('/my/collects', { params })
}

/** 评论：读取公开，发表与删除需登录。 */
export const commentApi = {
  listByArticle: (articleId) => request.get(`/articles/${articleId}/comments`),
  create: (articleId, payload) => request.post(`/articles/${articleId}/comments`, payload),
  remove: (articleId, id) => request.delete(`/articles/${articleId}/comments/${id}`)
}

/** 点赞 / 收藏（开关语义）。 */
export const interactionApi = {
  toggleLike: (articleId) => request.post(`/articles/${articleId}/like`),
  toggleCollect: (articleId) => request.post(`/articles/${articleId}/collect`)
}