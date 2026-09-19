/**
 * Codeforces Rating 颜色(全站共用)
 */
export function ratingColor(rating) {
  if (!rating) return '#808080'
  if (rating < 1200) return '#808080'
  if (rating < 1400) return '#008000'
  if (rating < 1600) return '#03a89e'
  if (rating < 1900) return '#0000ff'
  if (rating < 2100) return '#aa00aa'
  if (rating < 2400) return '#ff8c00'
  return '#ff0000'
}
