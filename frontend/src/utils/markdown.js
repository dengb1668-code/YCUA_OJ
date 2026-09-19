import { marked } from 'marked'
import DOMPurify from 'dompurify'

/**
 * Markdown 渲染(全站共用: 题面/帖子/题解/回复)
 * marked 会原样保留 Markdown 中嵌入的 HTML, 必须经 DOMPurify 消毒防止 XSS
 */
export function renderMarkdown(text) {
  if (!text) return ''
  return DOMPurify.sanitize(marked.parse(text))
}
