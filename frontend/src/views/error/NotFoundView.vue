<template>
  <div class="container err">
    <span class="err__code display-1">404</span>
    <h1 class="display-3">页面不存在</h1>
    <p class="err__text">你访问的地址没有对应的页面，可能链接已经失效，或者内容已被删除、下架。</p>
    <div class="err__actions">
      <UiButton variant="primary" :to="{ name: 'home' }">回到首页</UiButton>
      <UiButton variant="secondary" :to="{ name: 'articles' }">浏览文章</UiButton>
    </div>
  </div>
</template>

<style scoped>
.err {
  padding: var(--sp-section) var(--sp-lg);
  text-align: center;
  max-width: 620px;
}
.err__code {
  color: var(--primary);
  display: block;
  line-height: 1;
}
.err__text {
  margin: 14px auto 0;
  color: var(--text-muted);
  max-width: 48ch;
  line-height: 1.7;
}
.err__actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-top: var(--sp-xl);
  flex-wrap: wrap;
}
</style>