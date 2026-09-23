/**
 * Codeforces Rating 颜色(全站共用)
 * 返回 CSS 变量引用, 自动适配亮/暗主题(base.css 定义 --rt-*)
 */
export function ratingColor(rating) {
  if (!rating) return 'var(--rt-gray)'
  if (rating < 1200) return 'var(--rt-gray)'
  if (rating < 1400) return 'var(--rt-green)'
  if (rating < 1600) return 'var(--rt-cyan)'
  if (rating < 1900) return 'var(--rt-blue)'
  if (rating < 2100) return 'var(--rt-purple)'
  if (rating < 2400) return 'var(--rt-orange)'
  return 'var(--rt-red)'
}
