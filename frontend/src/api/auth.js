import request from './request'

/** 认证：注册 / 登录 / 退出 / 当前用户。 */
export const authApi = {
  register: (payload) => request.post('/auth/register', payload),
  login: (payload) => request.post('/auth/login', payload),
  logout: () => request.post('/auth/logout'),
  // 判断登录态时失败很常见（游客），静默处理，不弹提示
  me: () => request.get('/auth/me', { silent: true })
}