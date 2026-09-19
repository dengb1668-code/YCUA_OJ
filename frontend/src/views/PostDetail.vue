<template>
  <div class="post-detail">
    <div class="container">
      <el-page-header @back="router.back()" class="back" />

      <div v-loading="loading">
        <template v-if="post">
          <!-- 标题与操作 -->
          <div class="post-head">
            <h2 class="post-title">{{ post.title }}</h2>
            <div v-if="post.canEdit || post.canDelete" class="post-actions">
              <el-button v-if="post.canEdit" size="small" plain @click="openEdit">编辑</el-button>
              <el-button v-if="post.canDelete" size="small" plain type="danger" @click="handleDelete">
                删除
              </el-button>
            </div>
          </div>

          <!-- 元信息 -->
          <div class="post-meta">
            <span class="type-tag" :class="post.type === 'SOLUTION' ? 'tag-solution' : 'tag-discussion'">
              {{ post.type === 'SOLUTION' ? '题解' : '讨论' }}
            </span>
            <span class="meta-item">作者: {{ post.authorName }}</span>
            <span class="meta-item">{{ formatTime(post.createTime) }}</span>
            <el-link
              v-if="post.problemId"
              type="primary"
              class="meta-item"
              @click="router.push(`/problems/${post.problemId}`)"
            >
              {{ post.problemTitle }}
            </el-link>
          </div>

          <!-- 正文 -->
          <div class="markdown-body post-content" v-html="renderMarkdown(post.content)"></div>

          <!-- 回复区 -->
          <div class="reply-section">
            <h3>回复 ({{ post.replies?.length ?? 0 }})</h3>

            <div v-if="post.replies?.length" class="reply-list">
              <div v-for="(reply, idx) in post.replies" :key="reply.id" class="reply-item">
                <div class="reply-head">
                  <span class="reply-floor">#{{ idx + 1 }}</span>
                  <span class="reply-author">{{ reply.authorName }}</span>
                  <span class="reply-time">{{ formatTime(reply.createTime) }}</span>
                  <el-button
                    v-if="reply.canDelete"
                    size="small"
                    text
                    type="danger"
                    @click="handleDeleteReply(reply.id)"
                  >
                    删除
                  </el-button>
                </div>
                <div class="markdown-body reply-content" v-html="renderMarkdown(reply.content)"></div>
              </div>
            </div>
            <div v-else class="no-reply">暂无回复</div>

            <div class="reply-box">
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
    // request.js 拦截器已统一弹出错误提示
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
    // request.js 拦截器已统一弹出错误提示
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
    // request.js 拦截器已统一弹出错误提示
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
    // request.js 拦截器已统一弹出错误提示
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
    // request.js 拦截器已统一弹出错误提示
  } finally {
    replying.value = false
  }
}

onMounted(fetchDetail)
</script>

<style scoped>
.post-detail {
  background: #fff;
  min-height: calc(100vh - 44px);
}

.container {
  max-width: 960px;
  margin: 0 auto;
  padding: 16px 16px 80px;
}

.back {
  margin-bottom: 8px;
}

.post-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.post-title {
  font-size: 22px;
  font-weight: normal;
  margin: 8px 0 12px;
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
  color: #888;
  font-size: 13px;
  border-bottom: 1px solid #eee;
  padding-bottom: 12px;
  margin-bottom: 16px;
}

.type-tag {
  padding: 2px 8px;
  border-radius: 3px;
  font-size: 12px;
  color: #fff;
}

.tag-discussion {
  background: #1a5cc8;
}

.tag-solution {
  background: #5cb85c;
}

.meta-item {
  font-size: 13px;
}

.post-content {
  min-height: 120px;
}

/* 回复区 */
.reply-section {
  margin-top: 36px;
  border-top: 2px solid #eee;
  padding-top: 20px;
}

.reply-section h3 {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 12px;
}

.reply-item {
  border-bottom: 1px solid #eee;
  padding: 10px 0;
}

.reply-head {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: #666;
  margin-bottom: 6px;
}

.reply-floor {
  font-weight: 600;
  color: #1a5cc8;
}

.reply-author {
  font-weight: 600;
  color: #333;
}

.reply-time {
  color: #aaa;
}

.reply-head .el-button {
  margin-left: auto;
}

.reply-content {
  font-size: 14px;
}

.no-reply {
  color: #999;
  padding: 12px 0;
  font-size: 14px;
}

.reply-box {
  margin-top: 20px;
}

.reply-actions {
  margin-top: 10px;
  text-align: right;
}
</style>
