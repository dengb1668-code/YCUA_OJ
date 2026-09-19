<template>
  <div class="contest-list">
    <div class="page-head">
      <h2>比赛</h2>
      <el-button type="primary" plain @click="router.push('/contests/create')">创建比赛</el-button>
    </div>

    <el-table v-loading="loading" :data="contests">
      <el-table-column label="标题" min-width="280">
        <template #default="{ row }">
          <el-link type="primary" @click="router.push(`/contests/${row.id}`)">{{ row.title }}</el-link>
        </template>
      </el-table-column>
      <el-table-column label="赛制" width="90" align="center">
        <template #default="{ row }">
          <span class="type-tag" :class="`type-${row.type}`">{{ row.type }}</span>
        </template>
      </el-table-column>
      <el-table-column label="开始时间" width="170">
        <template #default="{ row }">
          <span class="muted">{{ formatTime(row.startTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="结束时间" width="170">
        <template #default="{ row }">
          <span class="muted">{{ formatTime(row.endTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <span class="status" :class="`status-${row.status}`">{{ statusLabel(row.status) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="创建者" width="140">
        <template #default="{ row }">
          <span>{{ row.creatorName }}</span>
        </template>
      </el-table-column>
      <el-table-column label="加密" width="70" align="center">
        <template #default="{ row }">
          <span v-if="row.hasPassword" class="muted">是</span>
          <span v-else class="muted">—</span>
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
  padding: 24px 16px 60px;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.page-head h2 {
  font-size: 20px;
  font-weight: normal;
  margin: 0;
}

.muted {
  color: #888;
}

.type-tag {
  padding: 2px 8px;
  border-radius: 3px;
  font-size: 12px;
  color: #fff;
}

.type-ICPC {
  background: #1a5cc8;
}

.type-OI {
  background: #5cb85c;
}

.type-IOI {
  background: #f0ad4e;
}

.status {
  font-size: 13px;
}

.status-gray {
  color: #999;
}

.status-green {
  color: #5cb85c;
  font-weight: 600;
}

.status-red {
  color: #d9534f;
}

/* 表格: AtCoder 细边框风格 */
.contest-list :deep(.el-table) {
  border: 1px solid #ddd;
}

.contest-list :deep(.el-table th.el-table__cell) {
  background: #eee;
  color: #333;
  font-weight: 600;
}

.contest-list :deep(.el-table td.el-table__cell) {
  border-bottom: 1px solid #ddd;
}

.contest-list :deep(.el-table .cell) {
  padding: 8px 12px;
  font-size: 14px;
}

.contest-list :deep(.el-pagination) {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
