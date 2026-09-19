<template>
  <div class="problem-list">
    <div class="page-head">
      <h2>题目列表</h2>
      <el-button type="primary" plain @click="router.push('/problems/create')">创建题目</el-button>
    </div>

    <el-table v-loading="loading" :data="problems">
      <el-table-column label="ID" width="90">
        <template #default="{ row }">
          <span class="prob-id">{{ row.id }}</span>
        </template>
      </el-table-column>
      <el-table-column label="标题" min-width="300">
        <template #default="{ row }">
          <el-link type="primary" @click="goDetail(row.id)">{{ row.title }}</el-link>
        </template>
      </el-table-column>
      <el-table-column label="来源" width="130">
        <template #default="{ row }">
          <span class="muted">{{ row.source || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="难度" width="90" align="center">
        <template #default="{ row }">
          <span class="rating" :style="{ color: ratingColor(row.difficulty) }">
            {{ row.difficulty }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <span class="status" :class="`status-${statusClass(row.status)}`">
            {{ statusLabel(row.status) }}
          </span>
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
import { getProblemPage } from '../api/problem'
import { ratingColor } from '../utils/rating'

const router = useRouter()

const loading = ref(false)
const problems = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

// 后端 ProblemStatus 枚举名 -> 展示文案与文字颜色(AtCoder 风格)
const statusMap = {
  NOT_ATTEMPTED: { label: '未开始', cls: 'gray' },
  ATTEMPTED: { label: '尝试中', cls: 'orange' },
  SOLVED: { label: '已通过', cls: 'green' }
}

function statusLabel(status) {
  return statusMap[status]?.label ?? '未开始'
}

function statusClass(status) {
  return statusMap[status]?.cls ?? 'gray'
}

/** 跳转题目详情页 */
function goDetail(id) {
  router.push(`/problems/${id}`)
}

async function fetchList() {
  loading.value = true
  try {
    // userId 由后端从登录令牌解析, 前端不再传
    const data = await getProblemPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    problems.value = data.records
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
.problem-list {
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

.problem-list h2 {
  font-size: 20px;
  font-weight: normal;
  margin: 0;
}

.muted {
  color: #888;
}

.rating {
  font-weight: 600;
  font-family: Helvetica, Arial, sans-serif;
}

.prob-id {
  color: #888;
}

/* 做题状态: AtCoder 式彩色文字 */
.status {
  font-size: 14px;
}

.status-gray {
  color: #999;
}

.status-orange {
  color: #f0ad4e;
}

.status-green {
  color: #5cb85c;
  font-weight: 600;
}

/* 表格: AtCoder 细边框风格 */
.problem-list :deep(.el-table) {
  border: 1px solid #ddd;
}

.problem-list :deep(.el-table th.el-table__cell) {
  background: #eee;
  color: #333;
  font-weight: 600;
}

.problem-list :deep(.el-table td.el-table__cell) {
  border-bottom: 1px solid #ddd;
}

.problem-list :deep(.el-table .cell) {
  padding: 8px 12px;
  font-size: 14px;
}

.problem-list :deep(.el-pagination) {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
