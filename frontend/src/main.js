import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import { configureRequest } from './api/request'
import { useUserStore } from './stores/user'
import { toast } from './composables/feedback'

import './styles/tokens.css'
import './styles/base.css'
import './styles/admin.css'

import UiIcon from './components/ui/UiIcon.vue'
import UiButton from './components/ui/UiButton.vue'
import UiInput from './components/ui/UiInput.vue'
import UiTextarea from './components/ui/UiTextarea.vue'
import UiSelect from './components/ui/UiSelect.vue'
import UiField from './components/ui/UiField.vue'
import UiCard from './components/ui/UiCard.vue'
import UiBadge from './components/ui/UiBadge.vue'
import UiAvatar from './components/ui/UiAvatar.vue'
import UiModal from './components/ui/UiModal.vue'
import UiPagination from './components/ui/UiPagination.vue'
import UiEmpty from './components/ui/UiEmpty.vue'
import UiSkeleton from './components/ui/UiSkeleton.vue'
import UiSpinner from './components/ui/UiSpinner.vue'
import UiDropdown from './components/ui/UiDropdown.vue'
import UiMenuItem from './components/ui/UiMenuItem.vue'
import MarkdownView from './components/ui/MarkdownView.vue'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

// 基础组件全局注册，视图里可直接使用，无需逐个 import
const globalComponents = {
  UiIcon,
  UiButton,
  UiInput,
  UiTextarea,
  UiSelect,
  UiField,
  UiCard,
  UiBadge,
  UiAvatar,
  UiModal,
  UiPagination,
  UiEmpty,
  UiSkeleton,
  UiSpinner,
  UiDropdown,
  UiMenuItem,
  MarkdownView
}
for (const [name, comp] of Object.entries(globalComponents)) {
  app.component(name, comp)
}

// 注入 401 处理：清登录态 + 跳登录（/auth/me 自身失败属正常游客态，不跳转）
const store = useUserStore()
configureRequest(({ message, url }) => {
  const wasLoggedIn = store.isLoggedIn
  store.reset()
  if (url.includes('/auth/me')) return
  const current = router.currentRoute.value
  if (current.name !== 'login') {
    if (wasLoggedIn) toast.warning(message || '登录状态已失效，请重新登录')
    router.push({ name: 'login', query: { redirect: current.fullPath } })
  }
})

app.mount('#app')