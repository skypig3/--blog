import { reactive } from 'vue'

/**
 * 全局轻量反馈：消息提示（toast）与确认框 / 输入框（confirm / prompt）。
 * 由 <FeedbackHost> 渲染，挂载在 App.vue 根部，因此切换主题时也跟随 data-theme。
 */

let seed = 0

/** 消息栈 */
export const toasts = reactive([])

export const toast = {
  show(message, type = 'info', duration = 2800) {
    const id = ++seed
    toasts.push({ id, message, type })
    window.setTimeout(() => dismiss(id), duration)
    return id
  },
  success(message) {
    return toast.show(message, 'success')
  },
  error(message) {
    return toast.show(message, 'error', 3600)
  },
  warning(message) {
    return toast.show(message, 'warning')
  },
  info(message) {
    return toast.show(message, 'info')
  }
}

export function dismiss(id) {
  const i = toasts.findIndex((t) => t.id === id)
  if (i >= 0) toasts.splice(i, 1)
}

/** 确认框状态 */
export const dialog = reactive({
  open: false,
  mode: 'confirm', // confirm | prompt
  title: '',
  content: '',
  inputLabel: '',
  inputPlaceholder: '',
  inputValue: '',
  inputRows: 3,
  required: true,
  confirmText: '确定',
  cancelText: '取消',
  danger: false,
  requireInput: false,
  _resolve: null,
  _close(value) {
    this.open = false
    const resolve = this._resolve
    this._resolve = null
    if (resolve) resolve(value)
  }
})

/**
 * 确认框：resolve(true/false)。
 */
export function confirmDialog(options = {}) {
  Object.assign(dialog, {
    mode: 'confirm',
    title: options.title || '确认操作',
    content: options.content || '',
    confirmText: options.confirmText || '确定',
    cancelText: options.cancelText || '取消',
    danger: Boolean(options.danger),
    inputLabel: '',
    inputValue: '',
    open: true
  })
  return new Promise((resolve) => {
    dialog._resolve = (v) => resolve(v === true)
  })
}

/**
 * 输入框：resolve(输入的文本) 或 resolve(null)（取消）。
 * 用于「驳回原因」「下架原因」「删除说明」等需要填写文本的场景。
 */
export function promptDialog(options = {}) {
  Object.assign(dialog, {
    mode: 'prompt',
    title: options.title || '请输入',
    content: options.content || '',
    inputLabel: options.inputLabel || '',
    inputPlaceholder: options.inputPlaceholder || '',
    inputValue: options.defaultValue || '',
    inputRows: options.rows || 3,
    required: options.required !== false,
    confirmText: options.confirmText || '确定',
    cancelText: options.cancelText || '取消',
    danger: Boolean(options.danger),
    open: true
  })
  return new Promise((resolve) => {
    dialog._resolve = (v) => resolve(v)
  })
}