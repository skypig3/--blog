<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { toast } from '@/composables/feedback'
import AuthPanel from '@/components/portal/AuthPanel.vue'

const store = useUserStore()
const route = useRoute()
const router = useRouter()

const account = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

const DEMO = [
  { label: '管理员', account: 'admin', password: 'Admin@123' },
  { label: '普通用户', account: 'xiaoming', password: 'User@123' }
]

function fill(d) {
  account.value = d.account
  password.value = d.password
}

async function onSubmit() {
  error.value = ''
  if (!account.value.trim() || !password.value) {
    error.value = '请输入用户名/邮箱与密码'
    return
  }
  loading.value = true
  try {
    await store.login({ account: account.value.trim(), password: password.value })
    toast.success('登录成功')
    const redirect = route.query.redirect
    router.replace(typeof redirect === 'string' && redirect ? redirect : { name: 'home' })
  } catch (e) {
    error.value = e?.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthPanel eyebrow="欢迎回来" title="登录" subtitle="支持用户名或邮箱登录。登录后才能创作、评论与点赞收藏。">
    <form class="form" @submit.prevent="onSubmit">
      <UiField label="用户名或邮箱">
        <UiInput v-model="account" icon="user" placeholder="admin 或 admin@example.com" autocomplete="username" />
      </UiField>
      <UiField label="密码">
        <UiInput v-model="password" type="password" icon="shield" placeholder="请输入密码" autocomplete="current-password" @enter="onSubmit" />
      </UiField>

      <p v-if="error" class="form__error">{{ error }}</p>

      <UiButton variant="primary" size="lg" block :loading="loading" @click="onSubmit">登录</UiButton>
    </form>

    <div class="demo">
      <span class="demo__label">演示账号（仅本地测试）</span>
      <div class="demo__btns">
        <button v-for="d in DEMO" :key="d.account" type="button" class="demo__btn" @click="fill(d)">
          <strong>{{ d.label }}</strong>
          <span>{{ d.account }} / {{ d.password }}</span>
        </button>
      </div>
    </div>

    <template #footer>
      还没有账号？<RouterLink class="link" :to="{ name: 'register' }">立即注册</RouterLink>
    </template>
  </AuthPanel>
</template>

<style scoped>
.form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.form__error {
  padding: 10px 14px;
  border-radius: var(--r-md);
  background: color-mix(in srgb, var(--error) 12%, transparent);
  border: 1px solid color-mix(in srgb, var(--error) 40%, transparent);
  color: var(--error);
  font-size: 0.875rem;
}
.demo {
  margin-top: var(--sp-lg);
  padding-top: var(--sp-lg);
  border-top: 1px solid var(--border);
}
.demo__label {
  display: block;
  font-size: 0.8125rem;
  color: var(--text-subtle);
  margin-bottom: 10px;
}
.demo__btns {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.demo__btn {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: var(--r-md);
  background: var(--input-bg);
  text-align: left;
  color: var(--text-body);
}
.demo__btn:hover {
  border-color: var(--primary);
}
.demo__btn strong {
  color: var(--text);
  font-size: 0.875rem;
  font-weight: 500;
}
.demo__btn span {
  font-size: 0.75rem;
  color: var(--text-subtle);
  font-family: var(--font-mono);
}
.link {
  color: var(--primary);
}
</style>