<template>
  <div class="user-manage">
    <div class="page-head">
      <h2>用户管理</h2>
      <div class="search-box">
        <el-input
          v-model="keyword"
          placeholder="搜索用户名/昵称"
          clearable
          style="width: 220px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-button type="primary" plain @click="handleSearch">搜索</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="users">
      <el-table-column label="ID" width="80">
        <template #default="{ row }">
          <span class="muted">{{ row.id }}</span>
        </template>
      </el-table-column>
      <el-table-column label="用户名" min-width="160">
        <template #default="{ row }">{{ row.username }}</template>
      </el-table-column>
      <el-table-column label="昵称" min-width="140">
        <template #default="{ row }">{{ row.nickname || '—' }}</template>
      </el-table-column>
      <el-table-column label="手机号" width="140">
        <template #default="{ row }">
          <span class="muted">{{ row.phone || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="角色" width="110" align="center">
        <template #default="{ row }">
          <span class="role" :class="`role-${row.role}`">{{ roleLabel(row.role) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="注册时间" width="170">
        <template #default="{ row }">
          <span class="muted">{{ formatTime(row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" align="center">
        <template #default="{ row }">
          <template v-if="row.role === 'OWNER'">
            <span class="muted">—</span>
          </template>
          <template v-else>
            <el-button
              v-if="row.role === 'USER'"
              size="small"
              type="primary"
              plain
              @click="setRole(row, 1)"
            >
              设为管理员
            </el-button>
            <el-button v-else size="small" plain type="danger" @click="setRole(row, 0)">
              取消管理员
            </el-button>
          </template>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserPage, updateUserRole } from '../api/user'

const loading = ref(false)
const users = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const keyword = ref('')

function roleLabel(role) {
  return { USER: '普通用户', ADMIN: '管理员', OWNER: '站长' }[role] ?? role
}

function formatTime(t) {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 19)
}

function handleSearch() {
  pageNum.value = 1
  fetchList()
}

async function setRole(row, role) {
  const action = role === 1 ? '设为管理员' : '取消管理员'
  try {
    await ElMessageBox.confirm(`确定将 ${row.username} ${action}?`, action, {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  try {
    await updateUserRole(row.id, { role })
    ElMessage.success(`${action}成功`)
    fetchList()
  } catch (e) {
    // request.js 拦截器已统一弹出错误提示
  }
}

async function fetchList() {
  loading.value = true
  try {
    const data = await getUserPage({
      keyword: keyword.value.trim(),
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    users.value = data.records
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
.user-manage {
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

.search-box {
  display: flex;
  gap: 8px;
}

.muted {
  color: #888;
}

.role {
  font-size: 13px;
}

.role-OWNER {
  color: #d9534f;
  font-weight: 600;
}

.role-ADMIN {
  color: #1a5cc8;
  font-weight: 600;
}

.role-USER {
  color: #666;
}

/* 表格: AtCoder 细边框风格(与题目列表一致) */
.user-manage :deep(.el-table) {
  border: 1px solid #ddd;
}

.user-manage :deep(.el-table th.el-table__cell) {
  background: #eee;
  color: #333;
  font-weight: 600;
}

.user-manage :deep(.el-table td.el-table__cell) {
  border-bottom: 1px solid #ddd;
}

.user-manage :deep(.el-table .cell) {
  padding: 8px 12px;
  font-size: 14px;
}

.user-manage :deep(.el-pagination) {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
