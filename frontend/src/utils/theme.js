/**
 * 主题切换：前台（Claude 暖色）/ 后台（Linear 深色）。
 *
 * 用 <body> 上的 class 而不是 <html data-theme="…">——某些宿主会往 <html> 注入自己的
 * data-theme（实测遇到过注入 "dark"），那样会把我们的值覆盖掉；body 的 class 不会被干扰。
 */
export function applyTheme(isAdmin) {
  const body = document.body
  body.classList.toggle('theme-admin', isAdmin)
  body.classList.toggle('theme-portal', !isAdmin)
}

/** 首屏按路径猜测主题（index.html 里的内联脚本也做同样的事，避免深链到 /admin 时闪浅色）。 */
export function themeForPath(path) {
  return typeof path === 'string' && path.startsWith('/admin')
}