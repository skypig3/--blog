<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { toast } from '@/composables/feedback'
import AuthPanel from '@/components/portal/AuthPanel.vue'

const store = useUserStore()
const router = useRouter()

const form = ref({ username: '', email: '', nickname: '', password: '', confirm: '' })
const errors = ref({})
const loading = ref(false)
const apiError = ref('')

const USERNAME_RE = /^[A-Za-z0-9_]{3,20}$/
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function validate() {
  const e = {}
  if (!form.value.username) e.username = '用户名不能为空'
  else if (!USERNAME_RE.test(form.value.username)) e.username = '3-20 位字母、数字或下划线'

  if (!form.value.email) e.email = '邮箱不能为空'
  else if (!EMAIL_RE.test(form.value.email)) e.email = '邮箱格式不正确'

  if (!form.value.nickname.trim()) e.nickname = '昵称不能为空'
  else if (form.value.nickname.length > 30) e.nickname = '昵称最长 30 字'

  if (!form.value.password) e.password = '密码不能为空'
  else if (form.value.password.length < 6 || form.value.password.length > 64)
    e.password = '密码长度需在 6-64 位之间'

  if (form.value.confirm !== form.value.password) e.confirm = '两次输入的密码不一致'

  errors.value = e
  return Object.keys(e).length === 0
}

async function onSubmit() {
  apiError.value = ''
  if (!validate()) return
  loading.value = true
  try {
    await store.register({
      username: form.value.username.trim(),
      email: form.value.email.trim(),
      nickname: form.value.nickname.trim(),
      password: form.value.password
    })
    toast.success('注册成功，已自动登录')
    router.replace({ name: 'home' })
  } catch (e) {
    apiError.value = e?.message || '注册失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthPanel
    eyebrow="加入我们"
    title="注册账号"
    subtitle="注册后即可创作投稿、评论与点赞收藏。普通注册只能创建普通用户。"
  >
    <form class="form" @submit.prevent="onSubmit">
      <UiField label="用户名" required :error="errors.username" hint="注册后不可修改">
        <UiInput v-model="form.username" icon="user" :invalid="!!errors.username" placeholder="3-20 位字母、数字或下划线" />
      </UiField>
      <UiField label="邮箱" required :error="errors.email">
        <UiInput v-model="form.email" icon="message-circle" :invalid="!!errors.email" placeholder="you@example.com" />
      </UiField>
      <UiField label="昵称" required :error="errors.nickname">
        <UiInput v-model="form.nickname" icon="feather" :invalid="!!errors.nickname" placeholder="展示在文章与评论上的名字" />
      </UiField>
      <UiField label="密码" required :error="errors.password" hint="6-64 位">
        <UiInput v-model="form.password" type="password" icon="shield" :invalid="!!errors.password" placeholder="请输入密码" />
      </UiField>
      <UiField label="确认密码" required :error="errors.confirm">
        <UiInput v-model="form.confirm" type="password" icon="shield" :invalid="!!errors.confirm" placeholder="再输入一次" @enter="onSubmit" />
      </UiField>

      <p v-if="apiError" class="form__error">{{ apiError }}</p>

      <UiButton variant="primary" size="lg" block :loading="loading" @click="onSubmit">注册并登录</UiButton>
    </form>

    <template #footer>
      已经有账号了？<RouterLink class="link" :to="{ name: 'login' }">去登录</RouterLink>
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
.link {
  color: var(--primary);
}
</style>