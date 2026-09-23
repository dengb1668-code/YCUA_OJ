<template>
  <div class="contest-list">
    <div class="page-head">
      <h2 class="page-title">比赛</h2>
      <el-button type="primary" plain @click="router.push('/contests/create')">创建比赛</el-button>
    </div>

    <div v-loading="loading" class="contest-grid">
      <div
        v-for="c in contests"
        :key="c.id"
        class="oj-card contest-card"
        :class="`st-${c.status}`"
        @click="router.push(`/contests/${c.id}`)"
      >
        <div class="card-top">
          <span class="type-tag" :class="`type-${c.type}`">{{ c.type }}</span>
          <span class="status-dot" :class="`status-${c.status}`">
            <span class="dot"></span>{{ statusLabel(c.status) }}
          </span>
        </div>
        <div class="contest-title">{{ c.title }}</div>
        <div class="contest-times">
          <div class="time-row">
            <span class="time-label">开始</span>
            <span class="mono">{{ formatTime(c.startTime) }}</span>
          </div>
          <div class="time-row">
            <span class="time-label">结束</span>
            <span class="mono">{{ formatTime(c.endTime) }}</span>
          </div>
        </div>
        <div class="card-bottom">
          <span class="creator">
            <span class="mini-avatar">{{ (c.creatorName || '?')[0].toUpperCase() }}</span>
            {{ c.creatorName }}
          </span>
          <span v-if="c.hasPassword" class="lock-hint">
            <el-icon :size="13"><Lock /></el-icon>
            加密
          </span>
        </div>
      </div>
    </div>

    <div v-if="!loading && !contests.length" class="empty-tip">暂无比赛</div>

    <el-pagination
      v-model:current-page="pageNum"
      :page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      @current-change="fetchList"
    />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Lock } from '@element-plus/icons-vue'
import { getContestPage } from '../api/contest'
import { formatTime } from '../utils/verdict'

const router = useRouter()

const loading = ref(false)
const contests = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const statusMap = {
  NOT_STARTED: { label: '未开始', cls: 'gray' },
  RUNNING: { label: '进行中', cls: 'green' },
  ENDED: { label: '已结束', cls: 'red' }
}

function statusLabel(status) {
  return statusMap[status]?.label ?? status
}

async function fetchList() {
  loading.value = true
  try {
    const data = await getContestPage({ pageNum: pageNum.value, pageSize: pageSize.value })
    contests.value = data.records
    total.value = data.total
  } catch (e) {
  } finally {
    loading.value = false
  }
}

onMounted(fetchList)
</script>

<style scoped>
.contest-list {
  max-width: 1100px;
  margin: 0 auto;
  padding: 28px 16px 64px;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  margin: 0;
}

/* 比赛卡片网格 */
.contest-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 14px;
  min-height: 80px;
}

.contest-card {
  padding: 16px 18px;
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s, transform 0.15s;
  position: relative;
  overflow: hidden;
}

.contest-card:hover {
  border-color: var(--brand);
  box-shadow: var(--shadow-md);
}

/* 左侧状态色条 */
.contest-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
}

.contest-card.st-RUNNING::before {
  background: var(--ok);
}

.contest-card.st-NOT_STARTED::before {
  background: var(--mute);
}

.contest-card.st-ENDED::before {
  background: var(--border-strong);
}

.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.type-tag {
  padding: 1px 8px;
  border-radius: 4px;
  font-size: 11.5px;
  font-weight: 600;
  color: #fff;
  font-family: var(--font-mono);
}

.type-ICPC {
  background: var(--brand);
}

.type-OI {
  background: #5cb85c;
}

.type-IOI {
  background: #f0ad4e;
}

.status-dot {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12.5px;
  color: var(--text-3);
}

.status-dot .dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--mute);
}

.status-dot.status-RUNNING .dot {
  background: var(--ok);
  box-shadow: 0 0 0 3px var(--ok-soft);
}

.status-dot.status-RUNNING {
  color: var(--ok);
  font-weight: 600;
}

.contest-title {
  font-size: 15.5px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.contest-times {
  display: flex;
  flex-direction: column;
  gap: 5px;
  margin-bottom: 14px;
}

.time-row {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12.5px;
  color: var(--text-2);
}

.time-label {
  width: 30px;
  color: var(--text-3);
  flex-shrink: 0;
}

.card-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1px solid var(--border);
  padding-top: 10px;
}

.creator {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 12.5px;
  color: var(--text-2);
}

.mini-avatar {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--brand-soft);
  color: var(--brand);
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.lock-hint {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-3);
}

.contest-list :deep(.el-pagination) {
  margin-top: 18px;
  justify-content: flex-end;
}
</style>
