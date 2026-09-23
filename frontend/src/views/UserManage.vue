<template>
  <div class="user-manage">
    <div class="page-head">
      <h2 class="page-title">用户管理</h2>
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

    <div class="table-scroll">
      <el-table v-loading="loading" :data="users">
        <el-table-column label="ID" width="80">
          <template #default="{ row }">
            <span class="muted mono">{{ row.id }}</span>
          </template>
        </el-table-column>
        <el-table-column label="用户名" min-width="160">
          <template #default="{ row }">
            <span class="user-cell">
              <span class="mini-avatar">{{ (row.username || '?')[0].toUpperCase() }}</span>
              {{ row.username }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="昵称" min-width="140">
          <template #default="{ row }">{{ row.nickname || '—' }}</template>
        </el-table-column>
        <el-table-column label="手机号" width="140">
          <template #default="{ row }">
            <span class="muted mono">{{ row.phone || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="120" align="center">
          <template #default="{ row }">
            <span class="role-badge" :class="`role-${row.role}`">{{ roleLabel(row.role) }}</span>
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
  padding: 28px 16px 64px;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  margin: 0;
}

.search-box {
  display: flex;
  gap: 8px;
}

.muted {
  color: var(--text-3);
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

.role-badge {
  padding: 1px 9px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  line-height: 19px;
}

.role-OWNER {
  background: var(--bad-soft);
  color: var(--bad);
}

.role-ADMIN {
  background: var(--brand-soft);
  color: var(--brand);
}

.role-USER {
  background: var(--mute-soft);
  color: var(--mute);
}

.user-manage :deep(.el-pagination) {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
