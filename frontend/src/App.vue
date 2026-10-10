<script setup>
import { RouterView } from 'vue-router'
import FeedbackHost from '@/components/ui/FeedbackHost.vue'
</script>

<template>
  <RouterView v-slot="{ Component }">
    <component :is="Component" />
  </RouterView>
  <FeedbackHost />
</template>