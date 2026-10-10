import axios from 'axios'
import { toast } from '@/composables/feedback'

/**
 * 统一请求层。
 * - 前后端同源（Vite 代理），Cookie 自动携带，所以 withCredentials + baseURL '/api'。
 * - 后端约定 { code, message, data }，code===0 视为成功，直接返回 data。
 * - 非 0 或 HTTP 错误统一在拦截器里提示；401 交给上层注入的处理函数（清登录态 + 跳登录）。
 *
 * 依赖注入：为避免 store / router 与 api 模块之间的循环引用，
 * 这里不直接 import，而是由 main.js 调用 configureRequest 注入。
 */
let unauthorizedHandler = () => {}

export function configureRequest(handler) {
  unauthorizedHandler = handler || (() => {})
}

const HTTP_FALLBACK = {
  400: '请求参数有误',
  401: '请先登录',
  403: '没有权限执行该操作',
  404: '内容不存在或已下架',
  409: '状态已变化，请刷新后重试',
  422: '操作被业务规则拒绝',
  500: '服务器内部错误'
}

const request = axios.create({
  baseURL: '/api',
  withCredentials: true,
  timeout: 15000
})

request.interceptors.response.use(
  (response) => {
    const body = response.data
    // 非标准结构（例如静态资源）原样返回
    if (!body || typeof body !== 'object' || !('code' in body)) return body
    if (body.code === 0) return body.data
    const err = { status: response.status, code: body.code, message: body.message || '操作失败' }
    if (!response.config?.silent) toast.error(err.message)
    return Promise.reject(err)
  },
  (error) => {
    const response = error.response
    const status = response?.status
    const message =
      response?.data?.message ||
      HTTP_FALLBACK[status] ||
      (error.code === 'ECONNABORTED' ? '请求超时，请检查网络' : '网络异常，请稍后重试')

    if (status === 401) {
      unauthorizedHandler({
        code: response?.data?.code,
        message,
        url: error.config?.url || ''
      })
      return Promise.reject({ status, code: response?.data?.code, message })
    }

    const err = { status, code: response?.data?.code, message }
    if (!error.config?.silent) toast.error(message)
    return Promise.reject(err)
  }
)

export default request