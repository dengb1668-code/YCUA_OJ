<template>
  <div class="problem-list">
    <div class="page-head">
      <h2 class="page-title">题库</h2>
      <div class="head-actions">
        <el-select
          v-model="selectedTags"
          multiple
          filterable
          collapse-tags
          placeholder="按标签筛选"
          clearable
          style="width: 240px"
          @change="handleSearch"
        >
          <el-option v-for="t in allTags" :key="t" :label="t" :value="t" />
        </el-select>
        <el-input
          v-model="keyword"
          placeholder="按标题搜索"
          clearable
          style="width: 200px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-button plain @click="handleSearch">搜索</el-button>
        <el-button type="primary" plain @click="router.push('/problems/create')">
          创建题目
        </el-button>
      </div>
    </div>

    <div class="table-scroll">
      <el-table v-loading="loading" :data="problems" @row-click="goDetail" class="clickable-table">
        <el-table-column label="#" width="70">
          <template #default="{ row }">
            <span class="mono prob-id">{{ row.id }}</span>
          </template>
        </el-table-column>
        <el-table-column label="标题" min-width="320">
          <template #default="{ row }">
            <el-link type="primary" :underline="false" class="prob-link" @click.stop="goDetail(row.id)">
              {{ row.title }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column label="来源" width="140">
          <template #default="{ row }">
            <span class="muted">{{ row.source || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="难度" width="100" align="center">
          <template #default="{ row }">
            <span class="rating-text" :style="{ color: ratingColor(row.difficulty) }">
              {{ row.difficulty }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="130" align="center">
          <template #default="{ row }">
            <span class="status" :class="`status-${statusClass(row.status)}`">
              <span class="status-glyph">{{ statusGlyph(row.status) }}</span>
              {{ statusLabel(row.status) }}
            </span>
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
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getProblemPage, getProblemTags } from '../api/problem'
import { ratingColor } from '../utils/rating'

const router = useRouter()

const loading = ref(false)
const problems = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const selectedTags = ref([])
const allTags = ref([])

// 后端 ProblemStatus 枚举名 -> 展示文案与颜色(CF 风格: 错=红, CE=黄, AC=绿)
const statusMap = {
  NOT_ATTEMPTED: { label: '未开始', cls: 'gray', glyph: '—' },
  ATTEMPTED: { label: '未通过', cls: 'red', glyph: '✕' },
  COMPILE_ERROR: { label: '编译错误', cls: 'yellow', glyph: '!' },
  SOLVED: { label: '已通过', cls: 'green', glyph: '✓' }
}

function statusLabel(status) {
  return statusMap[status]?.label ?? '未开始'
}

function statusClass(status) {
  return statusMap[status]?.cls ?? 'gray'
}

function statusGlyph(status) {
  return statusMap[status]?.glyph ?? '—'
}

/** 整行可点击跳转题目详情 */
function goDetail(row) {
  router.push(`/problems/${row.id}`)
}

function handleSearch() {
  pageNum.value = 1
  fetchList()
}

async function fetchList() {
  loading.value = true
  try {
    const data = await getProblemPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value.trim(),
      tags: selectedTags.value
    })
    problems.value = data.records
    total.value = data.total
  } catch (e) {
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchList()
  // 标签集加载失败不阻塞列表
  getProblemTags().then((tags) => (allTags.value = tags)).catch(() => {})
})
</script>

<style scoped>
.problem-list {
  max-width: 1100px;
  margin: 0 auto;
  padding: 28px 16px 64px;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  margin: 0;
}

.head-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.muted {
  color: var(--text-3);
}

.prob-id {
  color: var(--text-3);
  font-size: 13px;
}

.prob-link {
  font-size: 14px;
}

/* 整行可点击 */
.clickable-table :deep(.el-table__row) {
  cursor: pointer;
}

/* CF 式难度彩色文字 */
.rating-text {
  font-weight: 600;
  font-family: var(--font-mono);
  font-size: 13.5px;
}

/* 做题状态: 彩色文字 + 小符号 */
.status {
  font-size: 13px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.status-glyph {
  font-weight: 700;
}

.status-gray {
  color: var(--text-3);
}

.status-red {
  color: var(--bad);
}

.status-yellow {
  color: var(--wa);
}

.status-green {
  color: var(--ok);
}

.problem-list :deep(.el-pagination) {
  margin-top: 16px;
  justify-content: flex-end;
}

@media (max-width: 640px) {
  .head-actions {
    width: 100%;
  }

  .head-actions .el-select,
  .head-actions .el-input {
    flex: 1;
    width: auto !important;
    min-width: 0;
  }
}
</style>
