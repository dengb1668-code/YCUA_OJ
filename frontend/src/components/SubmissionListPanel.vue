<template>
  <div class="submission-panel">
    <div class="filter-bar">
      <el-select v-model="statusFilter" style="width: 150px" @change="handleFilterChange">
        <el-option label="全部状态" value="" />
        <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
    </div>

    <el-table v-loading="loading" :data="submissions">
      <el-table-column label="提交ID" width="90">
        <template #default="{ row }">
          <span class="muted">#{{ row.id }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="mode !== 'mine'" label="用户" width="140">
        <template #default="{ row }">
          <span>{{ row.username }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="mode === 'mine'" label="题目" min-width="220">
        <template #default="{ row }">
          <el-link type="primary" @click="router.push(`/problems/${row.problemId}`)">
            {{ row.problemTitle }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column v-else label="题目" min-width="220">
        <template #default="{ row }">
          <el-link type="primary" @click="router.push(`/problems/${row.problemId}`)">
            {{ row.displayId ? row.displayId + ' - ' : '' }}{{ row.problemTitle }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column label="语言" width="100">
        <template #default="{ row }">{{ languageLabel(row.language) }}</template>
      </el-table-column>
      <el-table-column label="判定" width="100">
        <template #default="{ row }">
          <span class="case-status" :style="{ color: verdictOf(row.status).color }">
            {{ verdictOf(row.status).short }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="得分" width="80">
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
      <el-table-column v-if="mode !== 'mine'" label="操作" width="110" align="center">
        <template #default="{ row }">
          <el-button size="small" plain :disabled="!row.canViewCode" @click="openCode(row)">
            {{ row.canViewCode ? '查看代码' : 'AC后可看' }}
          </el-button>
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

    <!-- 查看代码弹窗 -->
    <el-dialog v-model="codeVisible" title="提交代码" width="720px" align-center>
      <div v-if="codeDetail">
        <div class="code-meta">
          <span>提交 #{{ codeDetail.id }}</span>
          <span>{{ languageLabel(codeDetail.language) }}</span>
          <span v-if="codeDetail.status" :style="{ color: verdictOf(codeDetail.status).color }">
            {{ verdictOf(codeDetail.status).label }}
          </span>
        </div>
        <pre class="code-block">{{ codeDetail.code || '(无权限查看代码)' }}</pre>
        <div v-if="codeDetail.errorMessage" class="error-block">
          <div class="block-label">错误信息</div>
          <pre>{{ codeDetail.errorMessage }}</pre>
        </div>
        <div v-if="codeDetail.judgeDetail?.length" class="judge-block">
          <div class="block-label">用例结果</div>
          <table class="case-table">
            <thead>
              <tr>
                <th>用例</th>
                <th>结果</th>
                <th>得分</th>
                <th>耗时</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="c in codeDetail.judgeDetail" :key="c.caseName">
                <td>{{ c.caseName }}</td>
                <td>
                  <span :style="{ color: verdictOf(c.status).color }">
                    {{ verdictOf(c.status).short }}
                  </span>
                </td>
                <td>{{ c.fullScore != null ? `${c.score} / ${c.fullScore}` : '—' }}</td>
                <td>{{ c.timeUsed != null ? c.timeUsed + ' ms' : '—' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getSubmission, getSubmissions } from '../api/submission'
import { formatTime, languageLabel, verdictOf } from '../utils/verdict'

const props = defineProps({
  /** 'mine' | 'problem' | 'contest' */
  mode: { type: String, default: 'mine' },
  problemId: { type: Number, default: null },
  contestId: { type: Number, default: null },
  /** 比赛访问 token(contest 模式必传) */
  contestToken: { type: String, default: '' }
})

const router = useRouter()

const loading = ref(false)
const submissions = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const statusFilter = ref('')

const statusOptions = [
  { value: 'ACCEPTED', label: 'AC' },
  { value: 'WRONG_ANSWER', label: 'WA' },
  { value: 'TIME_LIMIT_EXCEEDED', label: 'TLE' },
  { value: 'MEMORY_LIMIT_EXCEEDED', label: 'MLE' },
  { value: 'RUNTIME_ERROR', label: 'RE' },
  { value: 'COMPILE_ERROR', label: 'CE' },
  { value: 'SYSTEM_ERROR', label: 'SE' },
  { value: 'PENDING', label: 'Pending' }
]

// 查看代码弹窗
const codeVisible = ref(false)
const codeDetail = ref(null)

function handleFilterChange() {
  pageNum.value = 1
  fetchList()
}

async function openCode(row) {
  try {
    codeDetail.value = await getSubmission(row.id)
    codeVisible.value = true
  } catch (e) {
  }
}

async function fetchList() {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (props.mode === 'mine') {
      params.scope = 'mine'
    } else {
      params.scope = 'all'
      if (props.mode === 'problem') {
        params.problemId = props.problemId
      } else if (props.mode === 'contest') {
        params.contestId = props.contestId
      }
    }
    if (statusFilter.value) {
      params.status = statusFilter.value
    }
    const data = await getSubmissions(params)
    submissions.value = data.records
    total.value = data.total
  } catch (e) {
  } finally {
    loading.value = false
  }
}

onMounted(fetchList)
</script>

<style scoped>
.filter-bar {
  margin-bottom: 12px;
}

.muted {
  color: #888;
}

.case-status {
  font-weight: 600;
}

/* 表格: AtCoder 细边框风格(与题目列表一致) */
.submission-panel :deep(.el-table) {
  border: 1px solid #ddd;
}

.submission-panel :deep(.el-table th.el-table__cell) {
  background: #eee;
  color: #333;
  font-weight: 600;
}

.submission-panel :deep(.el-table td.el-table__cell) {
  border-bottom: 1px solid #ddd;
}

.submission-panel :deep(.el-table .cell) {
  padding: 8px 12px;
  font-size: 14px;
}

.submission-panel :deep(.el-pagination) {
  margin-top: 16px;
  justify-content: flex-end;
}

/* 代码弹窗 */
.code-meta {
  display: flex;
  gap: 16px;
  color: #666;
  font-size: 13px;
  margin-bottom: 8px;
}

.code-block {
  margin: 0;
  padding: 12px;
  background: #f5f5f5;
  border: 1px solid #ddd;
  font-family: Consolas, Monaco, monospace;
  font-size: 13px;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 400px;
  overflow-y: auto;
}

.block-label {
  font-size: 13px;
  font-weight: 600;
  color: #555;
  margin: 12px 0 6px;
}

.error-block pre {
  margin: 0;
  padding: 10px 12px;
  border: 1px solid #ddd;
  background: #fff;
  color: #a94442;
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 160px;
  overflow-y: auto;
}

.case-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.case-table th {
  background: #eee;
  border: 1px solid #ddd;
  padding: 6px 10px;
  font-weight: 600;
}

.case-table td {
  border: 1px solid #ddd;
  padding: 6px 10px;
  text-align: center;
}
</style>
