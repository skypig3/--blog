import { defineStore } from 'pinia'
import { authApi } from '@/api/auth'

/**
 * 登录态。
 *
 * 关键点（踩过的坑）：刷新页面后 Pinia 是空的，但 Cookie 还在。
 * 所以只要 `checked` 为 false 就要向 /api/auth/me 问一次（无论当前路由是否要求登录），
 * 否则公开页面（首页、详情）会把已登录用户当成游客。
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    user: null,
    checked: false,
    loading: false
  }),

  getters: {
    isLoggedIn: (s) => !!s.user,
    isAdmin: (s) => s.user?.role === 'ADMIN',
    displayName: (s) => s.user?.nickname || s.user?.username || '访客'
  },

  actions: {
    /** 每个页面生命周期只查一次；force=true 时强制刷新。 */
    async fetchMe(force = false) {
      if (this.checked && !force) return this.user
      try {
        this.user = await authApi.me()
      } catch {
        this.user = null
      } finally {
        this.checked = true
      }
      return this.user
    },

    async login(payload) {
      this.user = await authApi.login(payload)
      this.checked = true
      return this.user
    },

    async register(payload) {
      this.user = await authApi.register(payload)
      this.checked = true
      return this.user
    },

    async logout() {
      try {
        await authApi.logout()
      } finally {
        this.reset()
      }
    },

    /** 只清本地状态（退出、401 时调用）。 */
    reset() {
      this.user = null
      this.checked = true
    },

    /** 资料更新后同步本地。 */
    patchUser(partial) {
      if (this.user) this.user = { ...this.user, ...partial }
    }
  }
})