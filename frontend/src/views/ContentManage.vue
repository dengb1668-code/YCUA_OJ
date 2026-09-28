<template>
  <div class="content-manage">
    <div class="page-head">
      <h2 class="page-title">内容管理</h2>
      <span class="muted">管理全站讨论、题解与博客(负责人/站长)</span>
    </div>

    <el-tabs v-model="activeType" @tab-change="handleTabChange">
      <el-tab-pane label="讨论" name="DISCUSSION" />
      <el-tab-pane label="题解" name="SOLUTION" />
      <el-tab-pane label="博客" name="BLOG" />
    </el-tabs>

    <div class="table-scroll">
      <el-table v-loading="loading" :data="posts">
        <el-table-column label="ID" width="80">
          <template #default="{ row }">
            <span class="muted mono">{{ row.id }}</span>
          </template>
        </el-table-column>
        <el-table-column label="标题" min-width="320">
          <template #default="{ row }">
            <el-link type="primary" underline="never" @click="router.push(`/post/${row.id}`)">
              {{ row.title }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column label="关联题目" width="220">
          <template #default="{ row }">
            <el-link
              v-if="row.problemId"
              type="primary"
              underline="never"
              @click="router.push(`/problems/${row.problemId}`)"
            >
              #{{ row.problemId }} {{ row.problemTitle }}
            </el-link>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="作者" width="150">
          <template #default="{ row }">{{ row.authorName }}</template>
        </el-table-column>
        <el-table-column label="回复" width="80" align="center">
          <template #default="{ row }">{{ row.replyCount }}</template>
        </el-table-column>
        <el-table-column label="发布时间" width="170">
          <template #default="{ row }">
            <span class="muted">{{ formatTime(row.createTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center">
          <template #default="{ row }">
            <el-button size="small" plain type="danger" @click="handleDelete(row)">删除</el-button>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { deletePost, getPostPage } from '../api/post'

const router = useRouter()

const activeType = ref('DISCUSSION')
const loading = ref(false)
const posts = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

function formatTime(t) {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 19)
}

function handleTabChange() {
  pageNum.value = 1
  fetchList()
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除帖子「${row.title}」? 回复将一并删除`, '删除确认', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  try {
    await deletePost(row.id)
    ElMessage.success('已删除')
    fetchList()
  } catch (e) {
  }
}

async function fetchList() {
  loading.value = true
  try {
    const data = await getPostPage({
      type: activeType.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    posts.value = data.records
    total.value = data.total
  } catch (e) {
  } finally {
    loading.value = false
  }
}

onMounted(fetchList)
</script>

<style scoped>
.content-manage {
  max-width: 1100px;
  margin: 0 auto;
  padding: 28px 16px 64px;
}

.page-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 14px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  margin: 0;
}

.muted {
  color: var(--text-3);
}

.mono {
  font-family: var(--font-mono);
  font-size: 13px;
}

.content-manage :deep(.el-pagination) {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
