<template>
  <div class="user-manage">
    <div class="page-head">
      <h2 class="page-title">用户管理</h2>
      <div v-if="activeTab === 'users'" class="search-box">
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
      <div v-else class="search-box">
        <el-select
          v-model="certFilter"
          placeholder="全部状态"
          clearable
          style="width: 160px"
          @change="handleCertSearch"
        >
          <el-option label="待审核" value="PENDING" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
      </div>
    </div>

    <el-tabs v-model="activeTab">
      <!-- 用户列表 -->
      <el-tab-pane label="用户列表" name="users">
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
                    设为负责人
                  </el-button>
                  <el-button v-else size="small" plain type="danger" @click="setRole(row, 0)">
                    取消负责人
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
      </el-tab-pane>

      <!-- 学生认证审核 -->
      <el-tab-pane name="cert">
        <template #label>
          认证审核
          <span v-if="pendingCount > 0" class="pending-dot">{{ pendingCount }}</span>
        </template>
        <div class="table-scroll">
          <el-table v-loading="certLoading" :data="certList">
            <el-table-column label="用户名" min-width="140">
              <template #default="{ row }">
                <span class="user-cell">
                  <span class="mini-avatar">{{ (row.username || '?')[0].toUpperCase() }}</span>
                  {{ row.username }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="姓名" width="110">
              <template #default="{ row }">{{ row.realName || '—' }}</template>
            </el-table-column>
            <el-table-column label="年级" width="100">
              <template #default="{ row }">{{ row.grade || '—' }}</template>
            </el-table-column>
            <el-table-column label="专业" min-width="160">
              <template #default="{ row }">{{ row.major || '—' }}</template>
            </el-table-column>
            <el-table-column label="申请时间" width="170">
              <template #default="{ row }">
                <span class="muted">{{ formatTime(row.certApplyTime) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <span class="cert-status" :class="`cert-status-${row.certStatus}`">
                  {{ certLabel(row.certStatus) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160" align="center">
              <template #default="{ row }">
                <template v-if="row.certStatus === 'PENDING'">
                  <el-button size="small" type="success" plain @click="handleReview(row, true)">通过</el-button>
                  <el-button size="small" type="danger" plain @click="handleReview(row, false)">驳回</el-button>
                </template>
                <span v-else class="muted">—</span>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <el-pagination
          v-model:current-page="certPageNum"
          :page-size="certPageSize"
          :total="certTotal"
          layout="total, prev, pager, next"
          @current-change="fetchCertList"
        />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserPage, updateUserRole, pageCert, reviewCert } from '../api/user'

const activeTab = ref('users')

const loading = ref(false)
const users = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const keyword = ref('')

// 学生认证审核
const certLoading = ref(false)
const certList = ref([])
const certTotal = ref(0)
const certPageNum = ref(1)
const certPageSize = ref(10)
const certFilter = ref('')
const pendingCount = ref(0)

function roleLabel(role) {
  return { USER: '学生', ADMIN: '集训队负责人', OWNER: '站长' }[role] ?? role
}

function certLabel(status) {
  return { NONE: '未认证', PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' }[status] ?? status
}

function formatTime(t) {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 19)
}

function handleSearch() {
  pageNum.value = 1
  fetchList()
}

function handleCertSearch() {
  certPageNum.value = 1
  fetchCertList()
}

async function setRole(row, role) {
  const action = role === 1 ? '设为负责人' : '取消负责人'
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

async function fetchCertList() {
  certLoading.value = true
  try {
    const data = await pageCert({
      status: certFilter.value || undefined,
      pageNum: certPageNum.value,
      pageSize: certPageSize.value
    })
    certList.value = data.records
    certTotal.value = data.total
  } catch (e) {
  } finally {
    certLoading.value = false
  }
}

/** 待审核数量(角标提示) */
async function fetchPendingCount() {
  try {
    const data = await pageCert({ status: 'PENDING', pageNum: 1, pageSize: 1 })
    pendingCount.value = data.total
  } catch (e) {
  }
}

async function handleReview(row, approve) {
  let reason = ''
  if (!approve) {
    try {
      const { value } = await ElMessageBox.prompt(
        `驳回 ${row.username}(${row.realName}) 的认证申请, 请填写驳回原因`,
        '驳回认证申请',
        { confirmButtonText: '驳回', cancelButtonText: '取消', inputPlaceholder: '如: 信息不真实, 请重新填写' }
      )
      reason = (value || '').trim()
    } catch (e) {
      return
    }
  } else {
    try {
      await ElMessageBox.confirm(`通过 ${row.username}(${row.realName}) 的认证申请?`, '通过认证申请', {
        type: 'warning',
        confirmButtonText: '通过',
        cancelButtonText: '取消'
      })
    } catch (e) {
      return
    }
  }
  try {
    await reviewCert({ userId: row.userId, approve, reason })
    ElMessage.success(approve ? '已通过认证' : '已驳回申请')
    fetchCertList()
    fetchPendingCount()
  } catch (e) {
  }
}

onMounted(() => {
  fetchList()
  fetchCertList()
  fetchPendingCount()
})
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

/* 认证审核 */
.pending-dot {
  display: inline-block;
  min-width: 16px;
  padding: 0 4px;
  margin-left: 4px;
  border-radius: 999px;
  background: var(--bad);
  color: #fff;
  font-size: 11px;
  line-height: 16px;
  text-align: center;
}

.cert-status {
  font-size: 12px;
  font-weight: 600;
}

.cert-status-PENDING {
  color: var(--brand);
}

.cert-status-APPROVED {
  color: var(--ok);
}

.cert-status-REJECTED {
  color: var(--bad);
}

.user-manage :deep(.el-pagination) {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
