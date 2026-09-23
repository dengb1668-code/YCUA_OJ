<template>
  <div class="post-list-panel">
    <div class="page-head">
      <h2 class="page-title">{{ type === 'SOLUTION' ? '题解' : '讨论区' }}</h2>
      <el-button type="primary" plain @click="openCreate">
        发布{{ type === 'SOLUTION' ? '题解' : '帖子' }}
      </el-button>
    </div>

    <div class="table-scroll">
      <el-table v-loading="loading" :data="posts">
        <el-table-column label="标题" min-width="360">
          <template #default="{ row }">
            <el-link type="primary" :underline="false" class="post-link" @click="router.push(`/post/${row.id}`)">
              {{ row.title }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column v-if="!problemId" label="关联题目" width="220">
          <template #default="{ row }">
            <el-link v-if="row.problemId" type="primary" :underline="false" @click="router.push(`/problems/${row.problemId}`)">
              {{ row.problemTitle }}
            </el-link>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="作者" width="150">
          <template #default="{ row }">
            <span class="user-cell">
              <span class="mini-avatar">{{ (row.authorName || '?')[0].toUpperCase() }}</span>
              {{ row.authorName }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="回复" width="90" align="center">
          <template #default="{ row }">
            <span class="reply-badge" :class="{ zero: !row.replyCount }">{{ row.replyCount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="170">
          <template #default="{ row }">
            <span class="muted">{{ formatTime(row.createTime) }}</span>
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

    <!-- 发帖弹窗 -->
    <el-dialog
      v-model="createVisible"
      :title="type === 'SOLUTION' ? '发布题解' : '发布帖子'"
      width="640px"
      align-center
    >
      <el-form label-width="90px">
        <el-form-item label="标题">
          <el-input v-model="createForm.title" maxlength="100" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item v-if="!problemId" label="关联题目">
          <el-input
            v-model="createForm.problemId"
            placeholder="题目ID(可选, 留空为全局讨论)"
            class="problem-id-input"
          />
        </el-form-item>
        <el-form-item label="内容">
          <el-input
            v-model="createForm.content"
            type="textarea"
            :rows="10"
            placeholder="支持 Markdown 语法"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createPost, getPostPage } from '../api/post'

const props = defineProps({
  /** 'DISCUSSION' | 'SOLUTION' */
  type: { type: String, required: true },
  /** 题目ID; null 表示全局讨论区 */
  problemId: { type: Number, default: null }
})

const router = useRouter()

const loading = ref(false)
const posts = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const createVisible = ref(false)
const creating = ref(false)
const createForm = reactive({ title: '', problemId: '', content: '' })

function openCreate() {
  createForm.title = ''
  createForm.problemId = ''
  createForm.content = ''
  createVisible.value = true
}

async function handleCreate() {
  if (!createForm.title.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  if (!createForm.content.trim()) {
    ElMessage.warning('请输入内容')
    return
  }
  let problemId = props.problemId
  if (!problemId && createForm.problemId.trim()) {
    problemId = Number(createForm.problemId.trim())
    if (!Number.isInteger(problemId) || problemId <= 0) {
      ElMessage.warning('题目ID需为正整数')
      return
    }
  }
  creating.value = true
  try {
    await createPost({
      type: props.type,
      problemId: problemId ?? null,
      title: createForm.title.trim(),
      content: createForm.content
    })
    ElMessage.success('发布成功')
    createVisible.value = false
    pageNum.value = 1
    fetchList()
  } catch (e) {
  } finally {
    creating.value = false
  }
}

function formatTime(t) {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 19)
}

async function fetchList() {
  loading.value = true
  try {
    const data = await getPostPage({
      type: props.type,
      problemId: props.problemId ?? undefined,
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
.post-list-panel {
  max-width: 1100px;
  margin: 0 auto;
  padding: 28px 16px 64px;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  margin: 0;
}

.muted {
  color: var(--text-3);
}

.post-link {
  font-size: 14px;
}

.user-cell {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: var(--text-2);
  font-size: 13px;
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

.reply-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 20px;
  padding: 0 7px;
  border-radius: 999px;
  background: var(--brand-soft);
  color: var(--brand);
  font-family: var(--font-mono);
  font-size: 12px;
  font-weight: 600;
}

.reply-badge.zero {
  background: var(--mute-soft);
  color: var(--mute);
}

.problem-id-input {
  width: 200px;
}

.post-list-panel :deep(.el-pagination) {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
