<script setup>
import { ref } from 'vue'
import { fileApi } from '@/api/site'
import { toast } from '@/composables/feedback'

const props = defineProps({
  modelValue: { type: String, default: '' },
  shape: { type: String, default: 'square' }, // square | circle
  hint: { type: String, default: '支持 jpg / png / gif / webp，单张不超过 2MB' },
  height: { type: String, default: '160px' }
})
const emit = defineEmits(['update:modelValue'])

const input = ref(null)
const uploading = ref(false)

function pick() {
  input.value?.click()
}

async function onChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  uploading.value = true
  try {
    const res = await fileApi.upload(file)
    emit('update:modelValue', res.url)
    toast.success('图片已上传')
  } catch {
    /* 错误提示已由请求层统一处理 */
  } finally {
    uploading.value = false
    if (input.value) input.value.value = ''
  }
}

function clear() {
  emit('update:modelValue', '')
}
</script>

<template>
  <div class="uploader">
    <div class="uploader__preview" :class="`is-${shape}`" :style="{ height: shape === 'square' ? height : undefined }">
      <img v-if="modelValue" :src="modelValue" alt="预览" />
      <div v-else class="uploader__placeholder">
        <UiIcon name="image" :size="22" />
        <span>未选择图片</span>
      </div>
    </div>
    <div class="uploader__side">
      <div class="uploader__actions">
        <UiButton variant="secondary" size="sm" icon="upload" :loading="uploading" @click="pick">
          {{ modelValue ? '更换图片' : '上传图片' }}
        </UiButton>
        <UiButton v-if="modelValue" variant="ghost" size="sm" @click="clear">移除</UiButton>
      </div>
      <p v-if="hint" class="caption uploader__hint">{{ hint }}</p>
    </div>
    <input ref="input" type="file" accept="image/jpeg,image/png,image/gif,image/webp" hidden @change="onChange" />
  </div>
</template>

<style scoped>
.uploader {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}
.uploader__preview {
  width: 200px;
  flex-shrink: 0;
  border: 1px dashed var(--border-strong);
  border-radius: var(--r-lg);
  background: var(--surface-3);
  overflow: hidden;
  display: grid;
  place-items: center;
}
.uploader__preview.is-circle {
  width: 96px;
  height: 96px;
  border-radius: 50%;
}
.uploader__preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.uploader__placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: var(--text-subtle);
  font-size: 0.8125rem;
}
.uploader__side {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.uploader__actions {
  display: flex;
  gap: 8px;
}
.uploader__hint {
  max-width: 28ch;
  line-height: 1.6;
}
</style>