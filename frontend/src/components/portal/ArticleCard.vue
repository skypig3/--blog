<script setup>
import { computed } from 'vue'
import { articleStatus, formatDate, fromNow, formatNumber } from '@/utils/format'

const props = defineProps({
  article: { type: Object, required: true },
  layout: { type: String, default: 'grid' }, // grid | row | compact
  showStatus: Boolean,
  hideCover: Boolean
})

const status = computed(() => articleStatus(props.article.status))
const to = computed(() => ({ name: 'article-detail', params: { id: props.article.id } }))
</script>

<template>
  <RouterLink class="acard" :class="[`acard--${layout}`, { 'no-cover': hideCover }]" :to="to">
    <div v-if="!hideCover" class="acard__cover">
      <img v-if="article.cover" :src="article.cover" :alt="article.title" loading="lazy" />
      <div v-else class="acard__pattern" aria-hidden="true">
        <svg viewBox="0 0 320 200" preserveAspectRatio="none">
          <g fill="none" stroke="currentColor" stroke-width="1.4">
            <path d="M-10 150 C 60 110, 110 180, 180 140 S 300 90, 340 130" />
            <path d="M-10 120 C 70 80, 120 150, 190 110 S 310 60, 340 100" opacity="0.5" />
            <circle cx="252" cy="58" r="22" />
            <path d="M232 58h40M252 38v40" opacity="0.55" />
          </g>
        </svg>
      </div>
      <span v-if="article.categoryName" class="acard__cat">{{ article.categoryName }}</span>
    </div>

    <div class="acard__body">
      <div class="acard__top">
        <h3 class="acard__title">{{ article.title }}</h3>
        <UiBadge v-if="showStatus" :variant="status.variant" size="sm">{{ status.label }}</UiBadge>
      </div>

      <p v-if="article.summary" class="acard__summary clamp-2">{{ article.summary }}</p>

      <div v-if="article.tags && article.tags.length" class="acard__tags">
        <span v-for="t in article.tags.slice(0, 3)" :key="t" class="acard__tag">#{{ t }}</span>
      </div>

      <div class="acard__foot">
        <span class="acard__author">
          <UiAvatar :src="article.author?.avatar" :name="article.author?.nickname" :size="22" />
          {{ article.author?.nickname }}
        </span>
        <span class="acard__dot">·</span>
        <time>{{ fromNow(article.publishedAt || article.createdAt) }}</time>
        <span class="acard__stats">
          <span><UiIcon name="eye" :size="14" />{{ formatNumber(article.viewCount) }}</span>
          <span><UiIcon name="heart" :size="14" />{{ formatNumber(article.likeCount) }}</span>
          <span><UiIcon name="message-circle" :size="14" />{{ formatNumber(article.commentCount) }}</span>
        </span>
      </div>
    </div>
  </RouterLink>
</template>

<style scoped>
.acard {
  display: flex;
  flex-direction: column;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--r-lg);
  overflow: hidden;
  color: inherit;
  transition: border-color 0.16s ease, transform 0.16s ease, background-color 0.16s ease;
}
.acard:hover {
  border-color: var(--border-strong);
  transform: translateY(-2px);
}
.acard__cover {
  position: relative;
  aspect-ratio: 16 / 10;
  background: var(--surface-2);
  overflow: hidden;
}
.acard__cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.acard__pattern {
  width: 100%;
  height: 100%;
  color: var(--primary);
  opacity: 0.55;
  background: linear-gradient(140deg, var(--surface-2), var(--surface));
}
.acard__pattern svg {
  width: 100%;
  height: 100%;
}
.acard__cat {
  position: absolute;
  top: 12px;
  left: 12px;
  padding: 3px 10px;
  border-radius: var(--r-pill);
  background: color-mix(in srgb, var(--bg) 82%, transparent);
  backdrop-filter: blur(6px);
  border: 1px solid var(--border);
  font-size: 0.75rem;
  font-weight: 500;
  color: var(--text);
}
.acard__body {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 20px;
  flex: 1;
}
.acard__top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}
.acard__title {
  font-family: var(--font-display);
  font-size: 1.3125rem;
  font-weight: var(--display-weight);
  letter-spacing: -0.3px;
  line-height: 1.25;
  color: var(--text);
}
.acard__summary {
  font-size: 0.9375rem;
  color: var(--text-muted);
  line-height: 1.6;
}
.acard__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.acard__tag {
  font-size: 0.8125rem;
  color: var(--text-subtle);
}
.acard__foot {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: auto;
  padding-top: 6px;
  font-size: 0.8125rem;
  color: var(--text-subtle);
  flex-wrap: wrap;
}
.acard__author {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--text-muted);
}
.acard__dot {
  opacity: 0.6;
}
.acard__stats {
  display: inline-flex;
  gap: 12px;
  margin-left: auto;
}
.acard__stats span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

/* 横向布局（列表用） */
.acard--row {
  flex-direction: row;
}
.acard--row .acard__cover {
  width: 240px;
  flex-shrink: 0;
  aspect-ratio: auto;
}
.acard--row.no-cover .acard__body {
  padding-left: 20px;
}

/* 紧凑布局（侧栏 / 后台） */
.acard--compact .acard__body {
  padding: 14px 0;
}
.acard--compact {
  background: transparent;
  border: 0;
  border-radius: 0;
}
.acard--compact:hover {
  transform: none;
}

@media (max-width: 640px) {
  .acard--row {
    flex-direction: column;
  }
  .acard--row .acard__cover {
    width: 100%;
    aspect-ratio: 16 / 10;
  }
}
</style>