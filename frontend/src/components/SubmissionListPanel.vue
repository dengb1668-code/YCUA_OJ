<template>
  <div class="submission-panel">
    <div class="filter-bar">
      <el-select v-model="statusFilter" style="width: 150px" @change="handleFilterChange">
        <el-option label="全部状态" value="" />
        <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>
      <el-button plain :icon="Refresh" circle title="刷新" @click="fetchList" />
    </div>

    <div class="table-scroll">
      <el-table v-loading="loading" :data="submissions">
        <el-table-column label="ID" width="80">
          <template #default="{ row }">
            <span class="mono sub-id">#{{ row.id }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="mode !== 'mine'" label="用户" width="130">
          <template #default="{ row }">
            <span class="user-cell">
              <span class="mini-avatar">{{ (row.username || '?')[0].toUpperCase() }}</span>
              {{ row.username }}
            </span>
          </template>
        </el-table-column>
        <el-table-column v-if="mode === 'mine'" label="题目" min-width="220">
          <template #default="{ row }">
            <el-link type="primary" :underline="false" @click="router.push(`/problems/${row.problemId}`)">
              {{ row.problemTitle }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column v-else label="题目" min-width="220">
          <template #default="{ row }">
            <el-link type="primary" :underline="false" @click="router.push(`/problems/${row.problemId}`)">
              <span v-if="row.displayId" class="mono display-id">{{ row.displayId }}</span>
              {{ row.problemTitle }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column label="语言" width="110">
          <template #default="{ row }">
            <span class="lang-tag">{{ languageLabel(row.language) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="判定" width="110">
          <template #default="{ row }">
            <el-tooltip
              :disabled="row.failedTestIndex == null"
              :content="verdictText(row.status, row.failedTestIndex)"
              placement="top"
            >
              <span class="vpill" :class="pillClass(row.status)">
                {{ verdictOf(row.status).short }}
              </span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="得分" width="90">
          <template #default="{ row }">
            <span class="mono muted">{{ row.score != null ? row.score : '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="110">
          <template #default="{ row }">
            <span class="mono muted">{{ row.timeUsed != null ? row.timeUsed + ' ms' : '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="内存" width="110">
          <template #default="{ row }">
            <span class="mono muted">{{ row.memoryUsed != null ? row.memoryUsed + ' KB' : '—' }}</span>
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
    </div>

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
          <span class="mono">#{{ codeDetail.id }}</span>
          <span class="lang-tag">{{ languageLabel(codeDetail.language) }}</span>
          <span
            v-if="codeDetail.status"
            class="vpill"
            :class="pillClass(codeDetail.status)"
          >
            {{ verdictOf(codeDetail.status).short }}
          </span>
          <span class="mono muted" v-if="codeDetail.timeUsed != null">{{ codeDetail.timeUsed }} ms</span>
          <span class="mono muted" v-if="codeDetail.memoryUsed != null">{{ codeDetail.memoryUsed }} KB</span>
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
                <th>内存</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="c in codeDetail.judgeDetail" :key="c.caseName">
                <td class="mono">{{ c.caseName }}</td>
                <td>
                  <span class="vpill" :class="pillClass(c.status)">
                    {{ verdictOf(c.status).short }}
                  </span>
                </td>
                <td class="mono">{{ c.fullScore != null ? `${c.score} / ${c.fullScore}` : '—' }}</td>
                <td class="mono">{{ c.timeUsed != null ? c.timeUsed + ' ms' : '—' }}</td>
                <td class="mono">{{ c.memoryUsed != null ? c.memoryUsed + ' KB' : '—' }}</td>
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
import { Refresh } from '@element-plus/icons-vue'
import { getSubmission, getSubmissions } from '../api/submission'
import { formatTime, languageLabel, verdictOf, verdictText } from '../utils/verdict'

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

// 判定 -> vpill 样式类(base.css 定义)
const PILL_CLASS = {
  ACCEPTED: 'v-ac',
  WRONG_ANSWER: 'v-wa',
  TIME_LIMIT_EXCEEDED: 'v-tle',
  MEMORY_LIMIT_EXCEEDED: 'v-mle',
  RUNTIME_ERROR: 'v-re',
  COMPILE_ERROR: 'v-ce',
  SYSTEM_ERROR: 'v-se',
  PENDING: 'v-pending',
  JUDGING: 'v-judging'
}

function pillClass(status) {
  return PILL_CLASS[status] ?? 'v-ce'
}

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
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.muted {
  color: var(--text-3);
}

.sub-id {
  color: var(--text-3);
  font-size: 13px;
}

.display-id {
  color: var(--brand);
  font-weight: 600;
  margin-right: 4px;
}

.user-cell {
  display: inline-flex;
  align-items: center;
  gap: 7px;
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

.submission-panel :deep(.el-pagination) {
  margin-top: 16px;
  justify-content: flex-end;
}

/* 代码弹窗 */
.code-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--text-2);
  font-size: 13px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.block-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-2);
  margin: 14px 0 8px;
}

.error-block pre {
  margin: 0;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--bg-soft);
  color: var(--bad);
  font-family: var(--font-mono);
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
  border: 1px solid var(--border);
  border-radius: var(--radius);
  overflow: hidden;
}

.case-table th {
  background: var(--bg-soft);
  border-bottom: 1px solid var(--border);
  padding: 7px 10px;
  font-weight: 600;
  color: var(--text-2);
  font-size: 12px;
}

.case-table td {
  border-bottom: 1px solid var(--border);
  padding: 7px 10px;
  text-align: center;
  color: var(--text);
}

.case-table tbody tr:last-child td {
  border-bottom: none;
}
</style>
