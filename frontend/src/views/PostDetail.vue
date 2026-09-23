<template>
  <div class="post-detail">
    <div class="container">
      <el-page-header @back="router.back()" class="back" />

      <div v-loading="loading">
        <template v-if="post">
          <!-- 帖子主体卡 -->
          <div class="oj-card post-card">
            <div class="post-head">
              <h2 class="post-title">{{ post.title }}</h2>
              <div v-if="post.canEdit || post.canDelete" class="post-actions">
                <el-button v-if="post.canEdit" size="small" plain @click="openEdit">编辑</el-button>
                <el-button v-if="post.canDelete" size="small" plain type="danger" @click="handleDelete">
                  删除
                </el-button>
              </div>
            </div>

            <div class="post-meta">
              <span class="type-tag" :class="post.type === 'SOLUTION' ? 'tag-solution' : 'tag-discussion'">
                {{ post.type === 'SOLUTION' ? '题解' : '讨论' }}
              </span>
              <span class="user-cell">
                <span class="mini-avatar">{{ (post.authorName || '?')[0].toUpperCase() }}</span>
                {{ post.authorName }}
              </span>
              <span class="meta-item muted">{{ formatTime(post.createTime) }}</span>
              <el-link
                v-if="post.problemId"
                type="primary"
                :underline="false"
                class="meta-item"
                @click="router.push(`/problems/${post.problemId}`)"
              >
                {{ post.problemTitle }}
              </el-link>
            </div>

            <div class="markdown-body post-content" v-html="renderMarkdown(post.content)"></div>
          </div>

          <!-- 回复区 -->
          <div class="reply-section">
            <h3 class="reply-title">回复 ({{ post.replies?.length ?? 0 }})</h3>

            <div v-if="post.replies?.length" class="reply-list">
              <div v-for="(reply, idx) in post.replies" :key="reply.id" class="oj-card reply-item">
                <div class="reply-head">
                  <span class="floor-badge mono">#{{ idx + 1 }}</span>
                  <span class="mini-avatar">{{ (reply.authorName || '?')[0].toUpperCase() }}</span>
                  <span class="reply-author">{{ reply.authorName }}</span>
                  <span class="reply-time">{{ formatTime(reply.createTime) }}</span>
                  <el-button
                    v-if="reply.canDelete"
                    size="small"
                    text
                    type="danger"
                    class="reply-del"
                    @click="handleDeleteReply(reply.id)"
                  >
                    删除
                  </el-button>
                </div>
                <div class="markdown-body reply-content" v-html="renderMarkdown(reply.content)"></div>
              </div>
            </div>
            <div v-else class="no-reply">暂无回复, 来抢沙发</div>

            <div class="oj-card reply-box">
              <div class="reply-box-head">
                <span class="mini-avatar">{{ (userStore.username || '?')[0].toUpperCase() }}</span>
                <span class="reply-as">{{ userStore.username }}</span>
              </div>
              <el-input
                v-model="replyContent"
                type="textarea"
                :rows="4"
                placeholder="写下你的回复(支持 Markdown)..."
              />
              <div class="reply-actions">
                <el-button type="primary" :loading="replying" @click="handleReply">回复</el-button>
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editVisible" title="编辑帖子" width="640px" align-center>
      <el-form label-width="90px">
        <el-form-item label="标题">
          <el-input v-model="editForm.title" maxlength="100" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="editForm.content" type="textarea" :rows="10" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addReply, deletePost, deleteReply, getPostDetail, updatePost } from '../api/post'
import { renderMarkdown } from '../utils/markdown'
import { userStore } from '../store/user'

const route = useRoute()
const router = useRouter()
const postId = Number(route.params.id)

const loading = ref(false)
const post = ref(null)

const replyContent = ref('')
const replying = ref(false)

const editVisible = ref(false)
const saving = ref(false)
const editForm = reactive({ title: '', content: '' })

function formatTime(t) {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 19)
}

async function fetchDetail() {
  loading.value = true
  try {
    post.value = await getPostDetail(postId)
  } catch (e) {
  } finally {
    loading.value = false
  }
}

function openEdit() {
  editForm.title = post.value.title
  editForm.content = post.value.content
  editVisible.value = true
}

async function handleSave() {
  if (!editForm.title.trim() || !editForm.content.trim()) {
    ElMessage.warning('标题和内容不能为空')
    return
  }
  saving.value = true
  try {
    await updatePost(postId, { title: editForm.title.trim(), content: editForm.content })
    ElMessage.success('已保存')
    editVisible.value = false
    fetchDetail()
  } catch (e) {
  } finally {
    saving.value = false
  }
}

async function handleDelete() {
  try {
    await ElMessageBox.confirm('删除后不可恢复(含全部回复), 确定删除该帖?', '删除帖子', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  try {
    await deletePost(postId)
    ElMessage.success('已删除')
    router.back()
  } catch (e) {
  }
}

async function handleDeleteReply(replyId) {
  try {
    await ElMessageBox.confirm('确定删除该回复?', '删除回复', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  try {
    await deleteReply(replyId)
    ElMessage.success('已删除')
    fetchDetail()
  } catch (e) {
  }
}

async function handleReply() {
  if (!replyContent.value.trim()) {
    ElMessage.warning('回复内容不能为空')
    return
  }
  replying.value = true
  try {
    await addReply(postId, { content: replyContent.value.trim() })
    ElMessage.success('回复成功')
    replyContent.value = ''
    fetchDetail()
  } catch (e) {
  } finally {
    replying.value = false
  }
}

onMounted(fetchDetail)
</script>

<style scoped>
.post-detail {
  background: var(--bg);
  min-height: calc(100vh - var(--header-height));
}

.container {
  max-width: 960px;
  margin: 0 auto;
  padding: 16px 16px 80px;
}

.back {
  margin-bottom: 12px;
}

/* 帖子主体卡 */
.post-card {
  padding: 22px 26px;
}

.post-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.post-title {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.01em;
  margin: 0;
  word-break: break-word;
}

.post-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.post-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  color: var(--text-2);
  font-size: 13px;
  border-bottom: 1px solid var(--border);
  padding: 12px 0 14px;
  margin: 12px 0 16px;
}

.type-tag {
  padding: 1px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
}

.tag-discussion {
  background: var(--brand);
}

.tag-solution {
  background: #5cb85c;
}

.user-cell {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-weight: 600;
  color: var(--text);
}

.mini-avatar {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--brand-soft);
  color: var(--brand);
  font-size: 11px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.meta-item {
  font-size: 13px;
}

.muted {
  color: var(--text-3);
}

.post-content {
  min-height: 120px;
  font-size: 15px;
}

/* 回复区 */
.reply-section {
  margin-top: 28px;
}

.reply-title {
  font-size: 17px;
  font-weight: 600;
  margin: 0 0 14px;
}

.reply-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.reply-item {
  padding: 14px 18px;
}

.reply-head {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 13px;
  color: var(--text-2);
  margin-bottom: 8px;
}

.floor-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 26px;
  height: 18px;
  padding: 0 6px;
  border-radius: 999px;
  background: var(--brand-soft);
  color: var(--brand);
  font-size: 11.5px;
  font-weight: 700;
}

.reply-author {
  font-weight: 600;
  color: var(--text);
}

.reply-time {
  color: var(--text-3);
  font-size: 12.5px;
}

.reply-del {
  margin-left: auto;
}

.reply-content {
  font-size: 14px;
}

.no-reply {
  color: var(--text-3);
  padding: 24px 0;
  font-size: 14px;
  text-align: center;
}

.reply-box {
  padding: 14px 18px 16px;
}

.reply-box-head {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-bottom: 10px;
}

.reply-as {
  font-weight: 600;
  font-size: 13.5px;
  color: var(--text);
}

.reply-box :deep(.el-textarea__inner) {
  font-size: 14px;
}

.reply-actions {
  margin-top: 10px;
  text-align: right;
}

@media (max-width: 640px) {
  .post-card {
    padding: 18px 16px;
  }
}
</style>
