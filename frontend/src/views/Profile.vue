<template>
  <div class="profile">
    <div class="container">
      <!-- 用户信息卡 -->
      <div class="oj-card user-card">
        <div class="user-avatar">{{ (userStore.username || '?')[0].toUpperCase() }}</div>
        <div class="user-info">
          <div class="user-name-row">
            <span class="user-name">{{ userStore.username }}</span>
            <span class="role-badge" :class="`role-${userStore.role}`">{{ roleLabel }}</span>
            <span v-if="certBadge" class="cert-badge" :class="`cert-${userStore.certStatus}`">{{ certBadge }}</span>
          </div>
          <div class="user-sub">坚持刷题, 每天进步一点点</div>
        </div>
      </div>

      <!-- 学生认证卡(普通用户; 负责人/站长无需认证) -->
      <div v-if="userStore.role === 'USER'" class="oj-card block cert-card">
        <div class="card-title">
          <el-icon :size="15" color="var(--brand)"><Postcard /></el-icon>
          学生认证
        </div>

        <!-- 已通过 -->
        <div v-if="cert.certStatus === 'APPROVED'" class="cert-ok">
          <span class="cert-ok-icon"><el-icon :size="14"><CircleCheck /></el-icon></span>
          认证已通过 — {{ cert.realName }} · {{ cert.grade }} · {{ cert.major }}
        </div>

        <!-- 审核中 -->
        <div v-else-if="cert.certStatus === 'PENDING'" class="cert-pending">
          <el-icon :size="15"><Clock /></el-icon>
          认证申请审核中, 请耐心等待({{ cert.realName }} · {{ cert.grade }} · {{ cert.major }})
        </div>

        <!-- 未认证 / 已驳回(可重新申请) -->
        <div v-else>
          <p class="cert-tip">提交代码前需要完成学生认证, 填写真实信息后由集训队负责人/站长审核。</p>
          <div v-if="cert.certStatus === 'REJECTED'" class="cert-reject">
            上次申请被驳回{{ cert.certRejectReason ? `: ${cert.certRejectReason}` : '' }}, 请修改后重新提交
          </div>
          <el-form label-width="60px" class="cert-form">
            <el-form-item label="姓名">
              <el-input v-model="certForm.realName" maxlength="20" placeholder="真实姓名" style="width: 220px" />
            </el-form-item>
            <el-form-item label="年级">
              <el-select v-model="certForm.grade" placeholder="选择年级" style="width: 220px">
                <el-option v-for="g in gradeOptions" :key="g" :label="g" :value="g" />
              </el-select>
            </el-form-item>
            <el-form-item label="专业">
              <el-input v-model="certForm.major" maxlength="30" placeholder="如: 计算机科学与技术" style="width: 220px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="applying" @click="handleApplyCert">提交认证申请</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>

      <!-- 统计卡片 -->
      <div class="stat-cards">
        <div class="oj-card stat-card">
          <div class="stat-icon icon-blue"><el-icon :size="18"><Document /></el-icon></div>
          <div>
            <div class="stat-num mono">{{ stats.totalSubmissions ?? '—' }}</div>
            <div class="stat-label">总提交</div>
          </div>
        </div>
        <div class="oj-card stat-card">
          <div class="stat-icon icon-green"><el-icon :size="18"><CircleCheck /></el-icon></div>
          <div>
            <div class="stat-num mono ok">{{ stats.solvedProblems ?? '—' }}</div>
            <div class="stat-label">已解决题目</div>
          </div>
        </div>
        <div class="oj-card stat-card">
          <div class="stat-icon icon-orange"><el-icon :size="18"><EditPen /></el-icon></div>
          <div>
            <div class="stat-num mono warn">{{ stats.attemptedProblems ?? '—' }}</div>
            <div class="stat-label">尝试中题目</div>
          </div>
        </div>
        <div class="oj-card stat-card">
          <div class="stat-icon icon-purple"><el-icon :size="18"><TrendCharts /></el-icon></div>
          <div>
            <div class="stat-num mono">{{ stats.acRate ?? '—' }}</div>
            <div class="stat-label">提交通过率</div>
          </div>
        </div>
      </div>

      <!-- 图表区: 难度占比环形图 + 难度分布条形图 -->
      <div class="charts-grid">
        <div class="oj-card block">
          <div class="card-title">
            <el-icon :size="15" color="var(--brand)"><PieChart /></el-icon>
            已解决题目的难度占比
          </div>
          <div v-if="donutSegments.length" class="donut-wrap">
            <svg viewBox="0 0 120 120" class="donut">
              <circle cx="60" cy="60" r="50" fill="none" stroke="var(--bg-soft)" stroke-width="20" />
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
                <span class="legend-rating mono" :style="{ color: ratingColor(s.rating) }">{{ s.rating }}</span>
                <span class="legend-count">{{ s.count }} 题</span>
                <span class="legend-pct mono">{{ Math.round(s.frac * 100) }}%</span>
              </div>
            </div>
          </div>
          <div v-else class="empty-tip">
            还没有通过任何题目,
            <router-link to="/problems">去刷题</router-link>
          </div>
        </div>

        <div class="oj-card block">
          <div class="card-title">
            <el-icon :size="15" color="var(--brand)"><Histogram /></el-icon>
            已解决题目的难度分布
          </div>
          <div v-if="(stats.ratingDistribution || []).length" class="rating-dist">
            <div v-for="item in stats.ratingDistribution" :key="item.rating" class="rating-row">
              <span class="rating-num mono" :style="{ color: ratingColor(item.rating) }">
                {{ item.rating }}
              </span>
              <div class="rating-bar-bg">
                <div
                  class="rating-bar"
                  :style="{ width: barWidth(item.count), background: ratingColor(item.rating) }"
                ></div>
              </div>
              <span class="rating-count mono">{{ item.count }} 题</span>
            </div>
          </div>
          <div v-else class="empty-tip">
            还没有通过任何题目,
            <router-link to="/problems">去刷题</router-link>
          </div>
        </div>
      </div>

      <!-- 年度 AC 热力图(Codeforces 风格小方格) -->
      <div class="oj-card block">
        <div class="card-title">
          <el-icon :size="15" color="var(--brand)"><Calendar /></el-icon>
          提交热力图(最近一年)
        </div>
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
                :class="cell ? `h${heatLevel(cell.count)}` : 'h-empty'"
                :title="cell ? `${cell.key} : ${cell.count} 次 AC` : ''"
              ></div>
            </div>
          </div>
          <div class="heat-legend">
            <span class="legend-text">少</span>
            <span class="legend-cell h0"></span>
            <span class="legend-cell h1"></span>
            <span class="legend-cell h2"></span>
            <span class="legend-cell h3"></span>
            <span class="legend-text">多</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import {
  Document, CircleCheck, EditPen, TrendCharts, PieChart, Histogram, Calendar,
  Postcard, Clock
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getMyStats } from '../api/submission'
import { applyCert, getMyCert } from '../api/user'
import { ratingColor } from '../utils/rating'
import { userStore, isCertified } from '../store/user'

const stats = ref({})

// 角色展示文案
const ROLE_LABELS = { OWNER: '站长', ADMIN: '集训队负责人', USER: '学生' }
const roleLabel = computed(() => ROLE_LABELS[userStore.role] ?? '学生')

// 学生认证
const cert = ref({ certStatus: userStore.certStatus })
const certForm = ref({ realName: '', grade: '', major: '' })
const applying = ref(false)
const gradeOptions = ['2020级', '2021级', '2022级', '2023级', '2024级', '2025级', '2026级', '2027级', '2028级']

const CERT_LABELS = { NONE: '未认证', PENDING: '审核中', APPROVED: '已认证', REJECTED: '已驳回' }
const certBadge = computed(() =>
  userStore.role === 'USER' ? CERT_LABELS[userStore.certStatus] ?? '' : ''
)

async function fetchCert() {
  try {
    const data = await getMyCert()
    cert.value = data
    certForm.value = { realName: data.realName || '', grade: data.grade || '', major: data.major || '' }
    userStore.setCertStatus(data.certStatus)
  } catch (e) {
  }
}

async function handleApplyCert() {
  if (!certForm.value.realName.trim() || !certForm.value.grade || !certForm.value.major.trim()) {
    ElMessage.warning('请填写姓名、年级和专业')
    return
  }
  applying.value = true
  try {
    await applyCert({
      realName: certForm.value.realName.trim(),
      grade: certForm.value.grade,
      major: certForm.value.major.trim()
    })
    ElMessage.success('认证申请已提交, 请等待审核')
    await fetchCert()
  } catch (e) {
  } finally {
    applying.value = false
  }
}

onMounted(async () => {
  if (userStore.token && userStore.role === 'USER') {
    await fetchCert()
  }
  // 未认证用户不拉统计(接口会被认证拦截器拒绝)
  if (isCertified()) {
    try {
      stats.value = await getMyStats()
    } catch (e) {
    }
  }
})

/** 热力图等级: 0=灰, 1=浅绿, 2=绿, 3+=深绿 */
function heatLevel(count) {
  if (count <= 0) return 0
  if (count === 1) return 1
  if (count === 2) return 2
  return 3
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
  min-height: calc(100vh - var(--header-height));
}

.container {
  max-width: 1100px;
  margin: 0 auto;
  padding: 28px 16px 64px;
}

/* 用户信息卡 */
.user-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  margin-bottom: 20px;
}

.user-avatar {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: var(--brand);
  color: #fff;
  font-size: 22px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.user-name-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-name {
  font-size: 18px;
  font-weight: 700;
  color: var(--text);
}

.role-badge {
  padding: 1px 8px;
  border-radius: 999px;
  font-size: 11.5px;
  font-weight: 600;
  line-height: 18px;
}

.role-OWNER {
  background: var(--brand-soft);
  color: var(--brand);
}

.role-ADMIN {
  background: var(--ok-soft);
  color: var(--ok);
}

.role-USER {
  background: var(--mute-soft);
  color: var(--mute);
}

.user-sub {
  margin-top: 3px;
  color: var(--text-3);
  font-size: 13px;
}

/* 学生认证 */
.cert-card {
  margin-bottom: 20px;
}

.cert-badge {
  padding: 1px 8px;
  border-radius: 999px;
  font-size: 11.5px;
  font-weight: 600;
  line-height: 18px;
}

.cert-APPROVED {
  background: var(--ok-soft);
  color: var(--ok);
}

.cert-PENDING {
  background: var(--brand-soft);
  color: var(--brand);
}

.cert-NONE,
.cert-REJECTED {
  background: var(--wa-soft);
  color: var(--wa);
}

.cert-ok {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--ok);
  font-weight: 600;
  font-size: 14px;
}

.cert-ok-icon {
  display: inline-flex;
}

.cert-pending {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--brand);
  font-weight: 600;
  font-size: 14px;
}

.cert-tip {
  margin: 0 0 12px;
  color: var(--text-2);
  font-size: 13.5px;
}

.cert-reject {
  margin-bottom: 12px;
  padding: 8px 12px;
  border-radius: 6px;
  background: var(--bad-soft);
  color: var(--bad);
  font-size: 13px;
}

.cert-form {
  margin-top: 4px;
}

/* 统计卡片 */
.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
}

.stat-icon {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.icon-blue {
  background: var(--brand-soft);
  color: var(--brand);
}

.icon-green {
  background: var(--ok-soft);
  color: var(--ok);
}

.icon-orange {
  background: var(--wa-soft);
  color: var(--wa);
}

.icon-purple {
  background: var(--bad-soft);
  color: var(--bad);
}

.stat-num {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.2;
}

.stat-num.ok {
  color: var(--ok);
}

.stat-num.warn {
  color: var(--wa);
}

.stat-label {
  font-size: 12.5px;
  color: var(--text-3);
}

/* 图表区 */
.charts-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin-bottom: 20px;
}

.block {
  padding: 20px 22px;
}

.block .card-title {
  margin-bottom: 16px;
}

.empty-tip a {
  font-weight: 600;
}

/* 环形图 */
.donut-wrap {
  display: flex;
  align-items: center;
  gap: 28px;
}

.donut {
  width: 160px;
  flex-shrink: 0;
}

.donut-num {
  font-size: 22px;
  font-weight: 700;
  font-family: var(--font-mono);
  fill: var(--text);
}

.donut-label {
  font-size: 11px;
  fill: var(--text-3);
}

.donut-legend {
  flex: 1;
  min-width: 0;
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
  font-weight: 700;
  font-size: 13px;
}

.legend-count {
  color: var(--text-2);
  font-size: 13px;
}

.legend-pct {
  margin-left: auto;
  color: var(--text-3);
  font-size: 12.5px;
}

/* rating 分布条形 */
.rating-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.rating-num {
  width: 52px;
  font-weight: 700;
  font-size: 13px;
  text-align: right;
}

.rating-bar-bg {
  flex: 1;
  height: 10px;
  background: var(--bg-soft);
  border-radius: 5px;
  overflow: hidden;
}

.rating-bar {
  height: 100%;
  border-radius: 5px;
  min-width: 2px;
  transition: width 0.4s ease;
}

.rating-count {
  width: 52px;
  color: var(--text-2);
  font-size: 12.5px;
}

/* 热力图 */
.heatmap {
  overflow-x: auto;
}

.heatmap-months {
  display: flex;
  margin-bottom: 6px;
  font-size: 12px;
  color: var(--text-3);
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
  border-radius: 3px;
}

/* GitHub 绿热力等级(亮/暗两套) */
.heat-cell.h0 { background: var(--heat-0); }
.heat-cell.h1 { background: var(--heat-1); }
.heat-cell.h2 { background: var(--heat-2); }
.heat-cell.h3 { background: var(--heat-3); }
.heat-cell.h-empty { background: transparent; }

.heat-legend {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 10px;
  font-size: 12px;
  color: var(--text-3);
}

.legend-cell {
  width: 12px;
  height: 12px;
  border-radius: 3px;
}

.legend-text {
  margin: 0 2px;
}

/* 热力图颜色: 亮色 */
.profile {
  --heat-0: #ebedf0;
  --heat-1: #9be9a8;
  --heat-2: #40c463;
  --heat-3: #30a14e;
}

html.dark .profile {
  --heat-0: #252b36;
  --heat-1: #1c5a33;
  --heat-2: #2f9e50;
  --heat-3: #3fb950;
}

/* 移动端 */
@media (max-width: 900px) {
  .stat-cards {
    grid-template-columns: repeat(2, 1fr);
  }

  .charts-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .stat-cards {
    grid-template-columns: 1fr;
  }

  .donut-wrap {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
