<script setup>
/**
 * 内联 SVG 图标集（描边风格，24×24 视图框）。
 * 用 name 选择图标；带填充的 brand 标记（spike）单独处理。
 */
const props = defineProps({
  name: { type: String, required: true },
  size: { type: [Number, String], default: 20 },
  strokeWidth: { type: [Number, String], default: 1.75 }
})

// 每个图标是一段 <path>/<circle>/… 的原始 SVG 内容
const ICONS = {
  search: '<circle cx="11" cy="11" r="7"/><path d="M20 20l-3.2-3.2"/>',
  'arrow-right': '<path d="M5 12h14"/><path d="M13 6l6 6-6 6"/>',
  'arrow-left': '<path d="M19 12H5"/><path d="M11 18l-6-6 6-6"/>',
  'arrow-up-right': '<path d="M7 17L17 7"/><path d="M8 7h9v9"/>',
  'chevron-down': '<path d="M6 9l6 6 6-6"/>',
  'chevron-up': '<path d="M6 15l6-6 6 6"/>',
  'chevron-left': '<path d="M15 6l-6 6 6 6"/>',
  'chevron-right': '<path d="M9 6l6 6-6 6"/>',
  check: '<path d="M5 13l4 4L19 7"/>',
  x: '<path d="M6 6l12 12"/><path d="M18 6L6 18"/>',
  menu: '<path d="M3 6h18"/><path d="M3 12h18"/><path d="M3 18h18"/>',
  heart:
    '<path d="M12 20s-7-4.6-9.3-9C1 7.6 3 4.5 6.2 4.5c1.9 0 3.3 1 4.3 2.3.4.5 1 .5 1.4 0 1-1.3 2.4-2.3 4.3-2.3C19.7 4.5 21.7 7.6 20 11c-2.3 4.4-8 9-8 9z"/>',
  bookmark: '<path d="M6 4h12a1 1 0 011 1v15l-7-4-7 4V5a1 1 0 011-1z"/>',
  eye: '<path d="M2 12s3.6-6 10-6 10 6 10 6-3.6 6-10 6-10-6-10-6z"/><circle cx="12" cy="12" r="2.6"/>',
  'message-circle': '<path d="M21 11.5a8.4 8.4 0 01-9 8.4 9 9 0 01-3.6-.8L3 21l1.9-5.1A8.4 8.4 0 0112 3.1a8.4 8.4 0 019 8.4z"/>',
  user: '<circle cx="12" cy="8" r="3.6"/><path d="M4.5 20a7.5 7.5 0 0115 0"/>',
  users:
    '<circle cx="9" cy="8" r="3.2"/><path d="M2.8 19.5a6.2 6.2 0 0112.4 0"/><path d="M16.5 5.4a3.2 3.2 0 010 6.2"/><path d="M18 14.2a6.2 6.2 0 013.2 5.3"/>',
  settings:
    '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.6 1.6 0 00.3 1.8l.1.1a2 2 0 11-2.8 2.8l-.1-.1a1.6 1.6 0 00-2.7 1.1V21a2 2 0 11-4 0v-.1A1.6 1.6 0 007.4 19l-.1.1a2 2 0 11-2.8-2.8l.1-.1a1.6 1.6 0 00-1.1-2.7H3a2 2 0 110-4h.1A1.6 1.6 0 004.6 8.4l-.1-.1a2 2 0 112.8-2.8l.1.1A1.6 1.6 0 0010 4.6V4a2 2 0 114 0v.1a1.6 1.6 0 002.7 1.1l.1-.1a2 2 0 112.8 2.8l-.1.1a1.6 1.6 0 001.1 2.7H21a2 2 0 110 4h-.1a1.6 1.6 0 00-1.5 1z"/>',
  'log-out': '<path d="M15 4h3a2 2 0 012 2v12a2 2 0 01-2 2h-3"/><path d="M10 17l-5-5 5-5"/><path d="M5 12h11"/>',
  edit: '<path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 013 3L7 19l-4 1 1-4z"/>',
  trash: '<path d="M3 6h18"/><path d="M8 6V4h8v2"/><path d="M6 6l1 14h10l1-14"/>',
  plus: '<path d="M12 5v14"/><path d="M5 12h14"/>',
  upload:
    '<path d="M12 16V4"/><path d="M7 9l5-5 5 5"/><path d="M4 16v3a1 1 0 001 1h14a1 1 0 001-1v-3"/>',
  image:
    '<rect x="3" y="4" width="18" height="16" rx="2"/><circle cx="8.5" cy="9.5" r="1.5"/><path d="M21 16l-5-5-6 6-3-3-4 4"/>',
  grid: '<rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/><rect x="3" y="14" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/>',
  'file-text':
    '<path d="M14 3H7a2 2 0 00-2 2v14a2 2 0 002 2h10a2 2 0 002-2V8z"/><path d="M14 3v5h5"/><path d="M9 13h6"/><path d="M9 17h6"/>',
  folder: '<path d="M3 7a2 2 0 012-2h4l2 2h8a2 2 0 012 2v8a2 2 0 01-2 2H5a2 2 0 01-2-2z"/>',
  tag: '<path d="M20.6 13.4l-7.2 7.2a2 2 0 01-2.8 0l-7-7A2 2 0 013 12.2V5a2 2 0 012-2h7.2a2 2 0 011.4.6l7 7a2 2 0 010 2.8z"/><circle cx="7.5" cy="7.5" r="1.3"/>',
  list: '<path d="M8 6h13"/><path d="M8 12h13"/><path d="M8 18h13"/><path d="M3.5 6h.01"/><path d="M3.5 12h.01"/><path d="M3.5 18h.01"/>',
  shield: '<path d="M12 3l7 3v5c0 4.4-3 8.3-7 10-4-1.7-7-5.6-7-10V6z"/><path d="M9.5 12l2 2 3.5-4"/>',
  clipboard:
    '<rect x="8" y="3" width="8" height="4" rx="1"/><path d="M16 5h2a2 2 0 012 2v12a2 2 0 01-2 2H6a2 2 0 01-2-2V7a2 2 0 012-2h2"/>',
  activity: '<path d="M3 12h4l2.5-7 5 14L17 12h4"/>',
  'alert-circle': '<circle cx="12" cy="12" r="9"/><path d="M12 8v5"/><path d="M12 16.5h.01"/>',
  'alert-triangle':
    '<path d="M10.3 3.9L2.4 17.5A2 2 0 004.1 20.5h15.8a2 2 0 001.7-3L13.7 3.9a2 2 0 00-3.4 0z"/><path d="M12 9v4.5"/><path d="M12 17h.01"/>',
  info: '<circle cx="12" cy="12" r="9"/><path d="M12 11v5"/><path d="M12 7.5h.01"/>',
  'check-circle': '<circle cx="12" cy="12" r="9"/><path d="M8 12.5l2.6 2.6L16 9.5"/>',
  calendar:
    '<rect x="3" y="5" width="18" height="16" rx="2"/><path d="M3 10h18"/><path d="M8 3v4"/><path d="M16 3v4"/>',
  clock: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
  sparkles:
    '<path d="M12 3l1.8 4.9L18.7 9.7l-4.9 1.8L12 16.4l-1.8-4.9L5.3 9.7l4.9-1.8z"/><path d="M18.5 15l.8 2.2 2.2.8-2.2.8-.8 2.2-.8-2.2-2.2-.8 2.2-.8z"/>',
  'more-horizontal': '<circle cx="5" cy="12" r="1.4"/><circle cx="12" cy="12" r="1.4"/><circle cx="19" cy="12" r="1.4"/>',
  filter: '<path d="M3 5h18l-7 8v6l-4-2v-4z"/>',
  'trending-up': '<path d="M3 17l6-6 4 4 8-8"/><path d="M15 7h6v6"/>',
  feather: '<path d="M20 4L8 16"/><path d="M14 4h6v6"/><path d="M4 20l4-1 9-9a2.8 2.8 0 00-4-4l-9 9z"/>'
}

const body = () => ICONS[props.name] || ICONS.info
</script>

<template>
  <!-- 品牌放射状标记（Anthropic spike mark） -->
  <svg
    v-if="name === 'spike'"
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="currentColor"
    aria-hidden="true"
    class="icon"
  >
    <path
      d="M12 1.5c.5 3.4.9 5.4 2 6.6 1.2 1.2 3.2 1.6 6.6 2.1v3.6c-3.4.5-5.4.9-6.6 2.1-1.1 1.2-1.5 3.2-2 6.6h-3.6c-.5-3.4-.9-5.4-2.1-6.6-1.1-1.2-3.1-1.6-6.5-2.1v-3.6c3.4-.5 5.4-.9 6.5-2.1 1.2-1.2 1.6-3.2 2.1-6.6z"
    />
  </svg>

  <svg
    v-else
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="none"
    :stroke-width="strokeWidth"
    stroke="currentColor"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
    class="icon"
    v-html="body()"
  />
</template>

<style scoped>
.icon {
  display: inline-block;
  flex-shrink: 0;
  vertical-align: middle;
}
</style>