<template>
  <div class="submission-list">
    <h2>提交记录</h2>

    <el-table v-loading="loading" :data="submissions">
      <el-table-column label="提交ID" width="90">
        <template #default="{ row }">
          <span class="muted">#{{ row.id }}</span>
        </template>
      </el-table-column>
      <el-table-column label="题目" min-width="260">
        <template #default="{ row }">
          <el-link type="primary" @click="router.push(`/problems/${row.problemId}`)">
            {{ row.problemTitle }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column label="语言" width="100">
        <template #default="{ row }">{{ languageLabel(row.language) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <span
            class="case-status"
            :style="{ color: verdictOf(row.status).color }"
          >
            {{ verdictOf(row.status).short }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="得分" width="90">
        <template #default="{ row }">
          {{ row.score != null ? row.score : '—' }}
        </template>
      </el-table-column>
      <el-table-column label="耗时" width="110">
        <template #default="{ row }">
          {{ row.timeUsed != null ? row.timeUsed + ' ms' : '—' }}
        </template>
      </el-table-column>
      <el-table-column label="提交时间" width="170">
        <template #default="{ row }">
          <span class="muted">{{ formatTime(row.createTime) }}</span>
        </template>
      </el-table-column>
    </el-table>

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
import { getMySubmissions } from '../api/submission'

const router = useRouter()

const loading = ref(false)
const submissions = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const languageMap = {
  CPP: 'C++',
  JAVA: 'Java',
  C: 'C',
  PYTHON3: 'Python3',
  GO: 'Go',
  JAVASCRIPT: 'JavaScript'
}

// AtCoder 式判定配色(与题目详情页弹窗一致)
const verdictMap = {
  ACCEPTED: { short: 'AC', color: '#5cb85c' },
  WRONG_ANSWER: { short: 'WA', color: '#f0ad4e' },
  TIME_LIMIT_EXCEEDED: { short: 'TLE', color: '#e67e22' },
  MEMORY_LIMIT_EXCEEDED: { short: 'MLE', color: '#e67e22' },
  RUNTIME_ERROR: { short: 'RE', color: '#d9534f' },
  COMPILE_ERROR: { short: 'CE', color: '#999' },
  SYSTEM_ERROR: { short: 'SE', color: '#d9534f' },
  PENDING: { short: 'Pending', color: '#999' },
  JUDGING: { short: 'Judging', color: '#999' }
}

function verdictOf(status) {
  return verdictMap[status] ?? { short: status, color: '#999' }
}

function languageLabel(language) {
  return languageMap[language] ?? language
}

function formatTime(iso) {
  if (!iso) return '—'
  return iso.replace('T', ' ').slice(0, 19)
}

async function fetchList() {
  loading.value = true
  try {
    const data = await getMySubmissions({
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    submissions.value = data.records
    total.value = data.total
  } catch (e) {
    // request.js 拦截器已统一弹出错误提示
  } finally {
    loading.value = false
  }
}

onMounted(fetchList)
</script>

<style scoped>
.submission-list {
  max-width: 1100px;
  margin: 0 auto;
  padding: 24px 16px 60px;
}

.submission-list h2 {
  font-size: 20px;
  font-weight: normal;
  margin: 0 0 16px;
}

.muted {
  color: #888;
}

.case-status {
  font-weight: 600;
}

/* 表格: AtCoder 细边框风格(与题目列表一致) */
.submission-list :deep(.el-table) {
  border: 1px solid #ddd;
}

.submission-list :deep(.el-table th.el-table__cell) {
  background: #eee;
  color: #333;
  font-weight: 600;
}

.submission-list :deep(.el-table td.el-table__cell) {
  border-bottom: 1px solid #ddd;
}

.submission-list :deep(.el-table .cell) {
  padding: 8px 12px;
  font-size: 14px;
}

.submission-list :deep(.el-pagination) {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
