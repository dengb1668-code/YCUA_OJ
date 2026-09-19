<template>
  <div class="profile">
    <div class="container">
      <h2>个人主页</h2>

      <!-- 统计卡片 -->
      <div class="stat-cards">
        <div class="stat-card">
          <div class="stat-num">{{ stats.totalSubmissions ?? '—' }}</div>
          <div class="stat-label">总提交</div>
        </div>
        <div class="stat-card">
          <div class="stat-num green">{{ stats.solvedProblems ?? '—' }}</div>
          <div class="stat-label">已解决题目</div>
        </div>
        <div class="stat-card">
          <div class="stat-num orange">{{ stats.attemptedProblems ?? '—' }}</div>
          <div class="stat-label">尝试中题目</div>
        </div>
        <div class="stat-card">
          <div class="stat-num">{{ stats.acRate ?? '—' }}</div>
          <div class="stat-label">提交通过率</div>
        </div>
      </div>

      <!-- 已解决题目的 rating 分布 -->
      <div class="block">
        <div class="block-title">已解决题目的难度分布</div>
        <div v-if="(stats.ratingDistribution || []).length" class="rating-dist">
          <div v-for="item in stats.ratingDistribution" :key="item.rating" class="rating-row">
            <span class="rating-num" :style="{ color: ratingColor(item.rating) }">
              {{ item.rating }}
            </span>
            <div class="rating-bar-bg">
              <div
                class="rating-bar"
                :style="{ width: barWidth(item.count), background: ratingColor(item.rating) }"
              ></div>
            </div>
            <span class="rating-count">{{ item.count }} 题</span>
          </div>
        </div>
        <div v-else class="empty-tip">还没有通过任何题目, 去刷题吧!</div>
      </div>

      <!-- 已解决题目的难度占比(环形图) -->
      <div class="block">
        <div class="block-title">已解决题目的难度占比</div>
        <div v-if="donutSegments.length" class="donut-wrap">
          <svg viewBox="0 0 120 120" class="donut">
            <circle cx="60" cy="60" r="50" fill="none" stroke="#f0f0f0" stroke-width="20" />
            <circle
              v-for="(s, i) in donutSegments"
              :key="i"
              cx="60"
              cy="60"
              r="50"
              fill="none"
              :stroke="ratingColor(s.rating)"
              stroke-width="20"
              :stroke-dasharray="`${s.dash} ${DONUT_CIRC - s.dash}`"
              :stroke-dashoffset="-s.offset"
              transform="rotate(-90 60 60)"
            />
            <text x="60" y="56" text-anchor="middle" class="donut-num">{{ stats.solvedProblems }}</text>
            <text x="60" y="72" text-anchor="middle" class="donut-label">已解决</text>
          </svg>
          <div class="donut-legend">
            <div v-for="s in donutSegments" :key="s.rating" class="legend-row">
              <span class="legend-dot" :style="{ background: ratingColor(s.rating) }"></span>
              <span class="legend-rating" :style="{ color: ratingColor(s.rating) }">{{ s.rating }}</span>
              <span class="legend-count">{{ s.count }} 题</span>
              <span class="legend-pct">{{ Math.round(s.frac * 100) }}%</span>
            </div>
          </div>
        </div>
        <div v-else class="empty-tip">还没有通过任何题目, 去刷题吧!</div>
      </div>

      <!-- 年度 AC 热力图(Codeforces 风格小方格) -->
      <div class="block">
        <div class="block-title">提交热力图(最近一年)</div>
        <div class="heatmap">
          <div class="heatmap-months">
            <span
              v-for="(m, i) in monthLabels"
              :key="i"
              class="month-label"
              :style="{ marginLeft: m.marginLeft }"
            >{{ m.label }}</span>
          </div>
          <div class="heatmap-grid">
            <div v-for="(col, ci) in columns" :key="ci" class="heat-col">
              <div
                v-for="(cell, ri) in col"
                :key="ri"
                class="heat-cell"
                :class="{ 'cell-empty': !cell }"
                :style="{ background: cell ? heatColor(cell.count) : 'transparent' }"
                :title="cell ? `${cell.key} : ${cell.count} 次 AC` : ''"
              ></div>
            </div>
          </div>
          <div class="heat-legend">
            <span class="legend-text">少</span>
            <span class="legend-cell" style="background: #ebedf0"></span>
            <span class="legend-cell" style="background: #9be9a8"></span>
            <span class="legend-cell" style="background: #40c463"></span>
            <span class="legend-cell" style="background: #30a14e"></span>
            <span class="legend-text">多</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { getMyStats } from '../api/submission'
import { ratingColor } from '../utils/rating'

const stats = ref({})

onMounted(async () => {
  try {
    stats.value = await getMyStats()
  } catch (e) {
    // request.js 拦截器已统一提示
  }
})

/** 热力图颜色: 0=灰, 1=浅绿, 2=绿, 3+=深绿(之后不再加深) */
function heatColor(count) {
  if (count <= 0) return '#ebedf0'
  if (count === 1) return '#9be9a8'
  if (count === 2) return '#40c463'
  return '#30a14e'
}

/** 按周(周日开头)组织最近 365 天为列 */
const columns = computed(() => {
  const map = {}
  ;(stats.value.dailyAccepted || []).forEach((d) => {
    map[d.date] = d.count
  })

  // 今天 0 点
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  // 364 天前
  const start = new Date(today)
  start.setDate(start.getDate() - 364)
  // 对齐到周日(周列从周日开始)
  const firstSunday = new Date(start)
  firstSunday.setDate(firstSunday.getDate() - firstSunday.getDay())

  const cols = []
  let cur = new Date(firstSunday)
  while (cur <= today) {
    const col = []
    for (let i = 0; i < 7; i++) {
      const d = new Date(cur)
      d.setDate(d.getDate() + i)
      if (d >= start && d <= today) {
        const key =
          `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
        col.push({ key, count: map[key] || 0 })
      } else {
        col.push(null)
      }
    }
    cols.push(col)
    cur.setDate(cur.getDate() + 7)
  }
  return cols
})

/** 月份标签: 记录月份变化出现的列位置 */
const monthLabels = computed(() => {
  const labels = []
  const cellW = 15 // 12px 格子 + 3px 间距
  columns.value.forEach((col, ci) => {
    const firstDay = col.find((c) => c)
    if (!firstDay) return
    const month = firstDay.key.slice(5, 7)
    const prev = labels[labels.length - 1]
    if (!prev || prev.month !== month) {
      labels.push({ month, label: `${Number(month)}月`, marginLeft: prev ? (ci - prev.col) * cellW : '0px' })
      // 记录该月起始列
      labels[labels.length - 1].col = ci
    }
  })
  return labels
})

/** rating 分布条形宽度(相对最大值) */
function barWidth(count) {
  const max = Math.max(...(stats.value.ratingDistribution || []).map((r) => r.count), 1)
  return `${Math.max((count / max) * 100, 8)}%`
}

/** 环形图: 各 rating 分段的角度弧长 */
const DONUT_CIRC = 2 * Math.PI * 50

const donutSegments = computed(() => {
  const dist = stats.value.ratingDistribution || []
  const total = dist.reduce((s, r) => s + r.count, 0)
  if (total === 0) return []
  let offset = 0
  return dist.map((r) => {
    const frac = r.count / total
    const seg = { ...r, frac, dash: frac * DONUT_CIRC, offset }
    offset += frac * DONUT_CIRC
    return seg
  })
})
</script>

<style scoped>
.profile {
  min-height: calc(100vh - 44px);
}

.container {
  max-width: 1100px;
  margin: 0 auto;
  padding: 24px 16px 80px;
}

.profile h2 {
  font-size: 20px;
  font-weight: normal;
  margin: 0 0 16px;
}

/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  flex: 1;
  border: 1px solid #ddd;
  padding: 16px;
  text-align: center;
  background: #fff;
}

.stat-num {
  font-size: 28px;
  font-weight: bold;
  font-family: Helvetica, Arial, sans-serif;
}

.stat-num.green {
  color: #5cb85c;
}

.stat-num.orange {
  color: #f0ad4e;
}

.stat-label {
  margin-top: 4px;
  font-size: 13px;
  color: #888;
}

/* 区块卡片 */
.block {
  border: 1px solid #ddd;
  padding: 16px;
  margin-bottom: 24px;
}

.block-title {
  font-weight: 600;
  margin-bottom: 12px;
}

.empty-tip {
  color: #999;
  font-size: 14px;
  padding: 8px 0;
}

/* rating 分布 */
.rating-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.rating-num {
  width: 52px;
  font-weight: bold;
  font-family: Helvetica, Arial, sans-serif;
  text-align: right;
}

.rating-bar-bg {
  flex: 1;
  height: 10px;
  background: #f0f0f0;
  border-radius: 5px;
  overflow: hidden;
}

.rating-bar {
  height: 100%;
  border-radius: 5px;
  min-width: 2px;
}

.rating-count {
  width: 52px;
  color: #666;
  font-size: 13px;
}

/* 难度占比环形图 */
.donut-wrap {
  display: flex;
  align-items: center;
  gap: 36px;
}

.donut {
  width: 180px;
  flex-shrink: 0;
}

.donut-num {
  font-size: 22px;
  font-weight: bold;
  font-family: Helvetica, Arial, sans-serif;
  fill: #333;
}

.donut-label {
  font-size: 11px;
  fill: #888;
}

.donut-legend {
  flex: 1;
}

.legend-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
  font-size: 14px;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.legend-rating {
  width: 48px;
  font-weight: bold;
  font-family: Helvetica, Arial, sans-serif;
}

.legend-count {
  color: #555;
}

.legend-pct {
  margin-left: auto;
  color: #999;
  font-size: 13px;
}

/* 年度热力图 */
.heatmap {
  overflow-x: auto;
}

.heatmap-months {
  display: flex;
  margin-bottom: 6px;
  font-size: 12px;
  color: #888;
  white-space: nowrap;
}

.heatmap-grid {
  display: flex;
  gap: 3px;
}

.heat-col {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.heat-cell {
  width: 12px;
  height: 12px;
  border-radius: 2px;
}

.cell-empty {
  background: transparent !important;
}

.heat-legend {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 10px;
  font-size: 12px;
  color: #888;
}

.legend-cell {
  width: 12px;
  height: 12px;
  border-radius: 2px;
}

.legend-text {
  margin: 0 2px;
}
</style>
