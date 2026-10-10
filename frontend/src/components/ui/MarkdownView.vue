<script setup>
import { computed } from 'vue'
import { renderMarkdown } from '@/utils/markdown'

const props = defineProps({
  source: { type: String, default: '' }
})

const html = computed(() => renderMarkdown(props.source))
</script>

<template>
  <!-- 内容已经过 DOMPurify 消毒（见 utils/markdown.js），可安全 v-html -->
  <div class="markdown-body" v-html="html"></div>
</template>