import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'

/**
 * Markdown 渲染：markdown-it 关闭 html（用户写的原始标签会被转义）→ DOMPurify 消毒，
 * 双层防 XSS。正文里允许的链接、图片等仍能正常显示。
 */
const md = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
  typographer: false
})

// 外链在新标签页打开，并加 rel 防止 tabnabbing
const defaultLinkOpen =
  md.renderer.rules.link_open ||
  ((tokens, idx, options, env, self) => self.renderToken(tokens, idx, options))
md.renderer.rules.link_open = (tokens, idx, options, env, self) => {
  const token = tokens[idx]
  const href = token.attrGet('href') || ''
  if (/^https?:\/\//.test(href)) {
    token.attrSet('target', '_blank')
    token.attrSet('rel', 'noopener noreferrer')
  }
  return defaultLinkOpen(tokens, idx, options, env, self)
}

export function renderMarkdown(source) {
  if (!source) return ''
  const raw = md.render(source)
  return DOMPurify.sanitize(raw, {
    ADD_ATTR: ['target', 'rel'],
    FORBID_TAGS: ['style', 'form', 'input', 'button', 'iframe', 'script'],
    FORBID_ATTR: ['style', 'onerror', 'onload', 'onclick']
  })
}

/** 从 Markdown 正文截取纯文本摘要（用于编辑器预览 / 明确的空摘要兜底）。 */
export function plainExcerpt(source, len = 120) {
  if (!source) return ''
  const text = source
    .replace(/```[\s\S]*?```/g, ' ')
    .replace(/`[^`]*`/g, ' ')
    .replace(/!\[[^\]]*\]\([^)]*\)/g, ' ')
    .replace(/\[([^\]]*)\]\([^)]*\)/g, '$1')
    .replace(/[#>*_~\-]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
  return text.length > len ? `${text.slice(0, len)}…` : text
}