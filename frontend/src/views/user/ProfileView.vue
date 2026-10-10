<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { userApi } from '@/api/site'
import { useUserStore } from '@/stores/user'
import { toast } from '@/composables/feedback'
import ImageUploader from '@/components/portal/ImageUploader.vue'

const store = useUserStore()
const router = useRouter()

const profile = ref({ nickname: '', bio: '', avatar: '' })
const savingProfile = ref(false)

const pwd = ref({ oldPassword: '', newPassword: '', confirm: '' })
const savingPwd = ref(false)
const pwdError = ref('')

onMounted(() => {
  const u = store.user
  if (u) profile.value = { nickname: u.nickname || '', bio: u.bio || '', avatar: u.avatar || '' }
})

async function saveProfile() {
  if (!profile.value.nickname.trim()) {
    toast.warning('昵称不能为空')
    return
  }
  savingProfile.value = true
  try {
    const updated = await userApi.updateProfile({
      nickname: profile.value.nickname.trim(),
      bio: profile.value.bio,
      avatar: profile.value.avatar
    })
    store.patchUser(updated)
    toast.success('资料已更新')
  } finally {
    savingProfile.value = false
  }
}

async function changePassword() {
  pwdError.value = ''
  if (!pwd.value.oldPassword || !pwd.value.newPassword) {
    pwdError.value = '请填写原密码与新密码'
    return
  }
  if (pwd.value.newPassword.length < 6 || pwd.value.newPassword.length > 64) {
    pwdError.value = '新密码长度需在 6-64 位之间'
    return
  }
  if (pwd.value.newPassword !== pwd.value.confirm) {
    pwdError.value = '两次输入的新密码不一致'
    return
  }
  savingPwd.value = true
  try {
    await userApi.changePassword({
      oldPassword: pwd.value.oldPassword,
      newPassword: pwd.value.newPassword
    })
    // 后端改密后当前会话立即失效
    store.reset()
    toast.success('密码已修改，请重新登录')
    router.replace({ name: 'login' })
  } catch (e) {
    pwdError.value = e?.message || '修改失败'
  } finally {
    savingPwd.value = false
  }
}
</script>

<template>
  <div class="stack-lg">
    <div class="pcard">
      <div class="pcard__head">
        <h2 class="display-4">基本资料</h2>
        <p class="caption">用户名不可修改；头像与简介会展示在文章与评论上。</p>
      </div>

      <div class="pcard__row">
        <span class="pcard__label">头像</span>
        <ImageUploader v-model="profile.avatar" shape="circle" :hint="''" />
      </div>

      <div class="pcard__row">
        <span class="pcard__label">用户名</span>
        <UiInput :model-value="store.user?.username" disabled />
      </div>

      <div class="pcard__row">
        <span class="pcard__label">邮箱</span>
        <UiInput :model-value="store.user?.email" disabled />
      </div>

      <div class="pcard__row">
        <span class="pcard__label">昵称</span>
        <UiInput v-model="profile.nickname" :maxlength="30" placeholder="昵称" />
      </div>

      <div class="pcard__row">
        <span class="pcard__label">个人简介</span>
        <UiTextarea v-model="profile.bio" :rows="3" :maxlength="200" placeholder="介绍一下自己（可选）" />
      </div>

      <div class="pcard__actions">
        <UiButton variant="primary" :loading="savingProfile" @click="saveProfile">保存资料</UiButton>
      </div>
    </div>

    <div class="pcard">
      <div class="pcard__head">
        <h2 class="display-4">修改密码</h2>
        <p class="caption">验证原密码后设置新密码；修改成功后当前会话会立即失效，需要重新登录。</p>
      </div>

      <div class="pcard__row">
        <span class="pcard__label">原密码</span>
        <UiInput v-model="pwd.oldPassword" type="password" placeholder="请输入原密码" />
      </div>
      <div class="pcard__row">
        <span class="pcard__label">新密码</span>
        <UiInput v-model="pwd.newPassword" type="password" placeholder="6-64 位" />
      </div>
      <div class="pcard__row">
        <span class="pcard__label">确认新密码</span>
        <UiInput v-model="pwd.confirm" type="password" placeholder="再输入一次" @enter="changePassword" />
      </div>

      <p v-if="pwdError" class="pcard__error">{{ pwdError }}</p>

      <div class="pcard__actions">
        <UiButton variant="secondary" :loading="savingPwd" @click="changePassword">修改密码</UiButton>
      </div>
    </div>
  </div>
</template>

<style scoped>
.stack-lg {
  display: flex;
  flex-direction: column;
  gap: var(--sp-lg);
}
.pcard {
  padding: var(--sp-xl);
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--r-lg);
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.pcard__head {
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border);
}
.pcard__head .caption {
  margin-top: 6px;
}
.pcard__row {
  display: grid;
  grid-template-columns: 120px 1fr;
  gap: 16px;
  align-items: center;
}
.pcard__label {
  font-size: 0.875rem;
  color: var(--text-muted);
}
.pcard__error {
  color: var(--error);
  font-size: 0.875rem;
}
.pcard__actions {
  display: flex;
  justify-content: flex-end;
  padding-top: 4px;
}
@media (max-width: 640px) {
  .pcard__row {
    grid-template-columns: 1fr;
    gap: 6px;
  }
}
</style>