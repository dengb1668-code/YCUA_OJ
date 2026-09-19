import { Marked } from 'marked'
import markedKatex from 'marked-katex-extension'
import DOMPurify from 'dompurify'

// 支持 $...$ 行内与 $$...$$ 块级 LaTeX; 公式写错显示红色错误文本而不中断渲染
const marked = new Marked(markedKatex({ throwOnError: false }))

/**
 * Markdown + LaTeX 渲染(全站共用: 题面/帖子/题解/回复/比赛说明)
 * marked 会原样保留 Markdown 中嵌入的 HTML, 必须经 DOMPurify 消毒防止 XSS
 */
export function renderMarkdown(text) {
  if (!text) return ''
  return DOMPurify.sanitize(marked.parse(text))
}
