<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  align: { type: String, default: 'end' }, // start | end
  width: { type: String, default: 'auto' }
})

const open = ref(false)
const root = ref(null)

function toggle() {
  open.value = !open.value
}
function closeMenu() {
  open.value = false
}
function onClickOutside(e) {
  if (root.value && !root.value.contains(e.target)) closeMenu()
}
function onKey(e) {
  if (e.key === 'Escape') closeMenu()
}

onMounted(() => {
  document.addEventListener('click', onClickOutside)
  document.addEventListener('keydown', onKey)
})
onBeforeUnmount(() => {
  document.removeEventListener('click', onClickOutside)
  document.removeEventListener('keydown', onKey)
})

defineExpose({ close: closeMenu })
</script>

<template>
  <div ref="root" class="ui-dropdown">
    <div class="ui-dropdown__trigger" @click="toggle">
      <slot name="trigger" :open="open" />
    </div>
    <Transition name="fade">
      <div
        v-if="open"
        class="ui-dropdown__menu"
        :class="`ui-dropdown__menu--${align}`"
        :style="{ minWidth: width === 'auto' ? undefined : width }"
        @click="closeMenu"
      >
        <slot :close="closeMenu" />
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.ui-dropdown {
  position: relative;
  display: inline-flex;
}
.ui-dropdown__trigger {
  display: inline-flex;
  cursor: pointer;
}
.ui-dropdown__menu {
  position: absolute;
  top: calc(100% + 8px);
  z-index: 60;
  min-width: 180px;
  padding: 6px;
  background: var(--bg);
  border: 1px solid var(--border-strong);
  border-radius: var(--r-lg);
  box-shadow: var(--shadow-pop);
}
.ui-dropdown__menu--end {
  right: 0;
}
.ui-dropdown__menu--start {
  left: 0;
}
</style>