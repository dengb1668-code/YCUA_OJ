<template>
  <div class="problem-manage">
    <div class="container">
      <el-page-header @back="router.back()" class="back" />
      <h2 class="page-title">题目管理</h2>

      <!-- 题面编辑 -->
      <div v-if="problem" class="oj-card manage-section">
        <h3 class="section-h">题面</h3>
        <ProblemForm
          :initial="problem"
          :submitting="savingProblem"
          submit-text="保存修改"
          @submit="handleUpdateProblem"
        />
      </div>

      <!-- 测试数据配置(洛谷式数据点配置) -->
      <div class="oj-card manage-section">
        <h3 class="section-h">
          测试数据
          <span class="total-score">满分 {{ totalScore }}</span>
        </h3>

        <el-alert
          v-if="!loading && !cases.length"
          title="未设置测试点, 判题时将使用题目公开样例比对"
          type="warning"
          :closable="false"
          class="empty-alert"
        />

        <div class="toolbar">
          <el-button type="primary" @click="openAdd">添加测试点</el-button>
          <el-upload
            ref="uploadRef"
            class="upload-inline"
            :auto-upload="false"
            :limit="1"
            accept=".zip"
            :on-change="onZipChange"
            :on-remove="() => (zipFile = null)"
          >
            <el-button plain>选择 zip 数据包</el-button>
          </el-upload>
          <el-button type="warning" plain :disabled="!zipFile" @click="handleUpload">
            上传并替换全部测试点
          </el-button>
          <el-button type="success" plain :disabled="!dirtyCount" @click="handleSaveConfigs">
            保存配置{{ dirtyCount ? ` (${dirtyCount})` : '' }}
          </el-button>
        </div>
        <div class="upload-hint">
          zip 内 N.in/N.out 须位于根目录且文件名恰一段数字(如 1.in/1.out, game001.in);
          支持洛谷 config.yml(timeLimit/memoryLimit/score, 不支持 subtaskId);
          分值未指定时自动均分, 满分默认 100
        </div>

        <el-table v-loading="loading" :data="cases" border class="case-table">
          <el-table-column prop="index" label="#" width="60" />
          <el-table-column label="输入文件" min-width="120">
            <template #default="{ row }">{{ formatSize(row.inputSize) }}</template>
          </el-table-column>
          <el-table-column label="输出文件" min-width="120">
            <template #default="{ row }">{{ formatSize(row.outputSize) }}</template>
          </el-table-column>
          <el-table-column label="分值" width="130">
            <template #default="{ row }">
              <el-input-number
                v-model="row.score"
                :min="0"
                :max="10000"
                size="small"
                controls-position="right"
                style="width: 95px"
                @change="row._dirty = true"
              />
            </template>
          </el-table-column>
          <el-table-column label="时限(ms)" width="150">
            <template #default="{ row }">
              <el-input-number
                v-model="row.timeLimit"
                :min="1"
                :max="100000"
                size="small"
                controls-position="right"
                style="width: 115px"
                :placeholder="`默认 ${problem?.timeLimit ?? '-'}`"
                @change="row._dirty = true"
              />
            </template>
          </el-table-column>
          <el-table-column label="内存(MB)" width="150">
            <template #default="{ row }">
              <el-input-number
                v-model="row.memoryLimit"
                :min="1"
                :max="4096"
                size="small"
                controls-position="right"
                style="width: 115px"
                :placeholder="`默认 ${problem?.memoryLimit ?? '-'}`"
                @change="row._dirty = true"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button size="small" @click="openEdit(row)">编辑数据</el-button>
              <el-button size="small" type="danger" plain @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 危险操作 -->
      <div class="oj-card manage-section">
        <h3 class="section-h">危险操作</h3>
        <el-button type="danger" plain @click="handleDeleteProblem">删除题目</el-button>
        <div class="danger-hint">
          删除将同时清除该题的全部提交记录、讨论/题解与测试点数据, 不可恢复; 被比赛引用的题目无法删除
        </div>
      </div>
    </div>

    <!-- 添加测试点对话框 -->
    <el-dialog v-model="addVisible" title="添加测试点" width="640px" align-center>
      <div class="case-form">
        <div class="sample-label">输入</div>
        <el-input v-model="addForm.input" type="textarea" :rows="6" class="mono-input" placeholder="测试点输入(标准输入)..." />
        <div class="sample-label">期望输出</div>
        <el-input v-model="addForm.output" type="textarea" :rows="6" class="mono-input" placeholder="期望输出..." />
        <div class="config-row">
          <div>
            <div class="sample-label">分值</div>
            <el-input-number v-model="addForm.score" :min="0" :max="10000" controls-position="right" placeholder="留空=全部均分100" style="width: 100%" />
          </div>
          <div>
            <div class="sample-label">时限(ms)</div>
            <el-input-number v-model="addForm.timeLimit" :min="1" :max="100000" controls-position="right" :placeholder="`默认 ${problem?.timeLimit ?? '-'}`" style="width: 100%" />
          </div>
          <div>
            <div class="sample-label">内存(MB)</div>
            <el-input-number v-model="addForm.memoryLimit" :min="1" :max="4096" controls-position="right" :placeholder="`默认 ${problem?.memoryLimit ?? '-'}`" style="width: 100%" />
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleAdd">添加</el-button>
      </template>
    </el-dialog>

    <!-- 编辑测试点数据对话框 -->
    <el-dialog v-model="editVisible" :title="`编辑测试点 ${editingIndex}`" width="640px" align-center>
      <div class="case-form">
        <div class="sample-label">输入</div>
        <el-input v-model="editForm.input" type="textarea" :rows="6" class="mono-input" />
        <div class="sample-label">期望输出</div>
        <el-input v-model="editForm.output" type="textarea" :rows="6" class="mono-input" />
      </div>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleEditSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import ProblemForm from '../components/ProblemForm.vue'
import {
  addTestCase,
  deleteProblem,
  deleteTestCase,
  getProblemDetail,
  getTestCase,
  getTestCases,
  updateProblem,
  updateTestCase,
  updateTestCaseConfig,
  uploadTestCases
} from '../api/problem'

const route = useRoute()
const router = useRouter()
const problemId = Number(route.params.id)

const loading = ref(false)
const problem = ref(null)
const cases = ref([])

const savingProblem = ref(false)
const saving = ref(false)

const uploadRef = ref(null)
const zipFile = ref(null)

const addVisible = ref(false)
const addForm = reactive({ input: '', output: '', score: null, timeLimit: null, memoryLimit: null })

const editVisible = ref(false)
const editingIndex = ref(null)
const editForm = reactive({ input: '', output: '' })

const totalScore = computed(() => cases.value.reduce((sum, c) => sum + (c.score || 0), 0))
const dirtyCount = computed(() => cases.value.filter((c) => c._dirty).length)

function formatSize(bytes) {
  if (bytes == null) return '—'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(2) + ' MB'
}

async function fetchAll() {
  loading.value = true
  try {
    problem.value = await getProblemDetail(problemId)
    if (!problem.value.canManage) {
      ElMessage.error('无权限管理该题目')
      router.replace(`/problems/${problemId}`)
      return
    }
    cases.value = (await getTestCases(problemId)).map((c) => ({ ...c, _dirty: false }))
  } catch (e) {
  } finally {
    loading.value = false
  }
}

// ---- 题面 ----
async function handleUpdateProblem(payload) {
  savingProblem.value = true
  try {
    await updateProblem(problemId, payload)
    ElMessage.success('题面已保存')
    await fetchAll()
  } catch (e) {
  } finally {
    savingProblem.value = false
  }
}

// ---- 删除题目 ----
async function handleDeleteProblem() {
  try {
    await ElMessageBox.confirm(
      `确定删除题目「${problem.value.title}」? 该题的全部提交记录、讨论/题解与测试点将被清除, 不可恢复!`,
      '删除题目',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  try {
    await deleteProblem(problemId)
    ElMessage.success('题目已删除')
    router.push('/problems')
  } catch (e) {
  }
}

// ---- 测试点 ----
function openAdd() {
  addForm.input = ''
  addForm.output = ''
  addForm.score = null
  addForm.timeLimit = null
  addForm.memoryLimit = null
  addVisible.value = true
}

async function handleAdd() {
  saving.value = true
  try {
    const index = await addTestCase(problemId, {
      input: addForm.input,
      output: addForm.output,
      score: addForm.score,
      timeLimit: addForm.timeLimit,
      memoryLimit: addForm.memoryLimit
    })
    ElMessage.success(`测试点 ${index} 已添加`)
    addVisible.value = false
    await fetchAll()
  } catch (e) {
  } finally {
    saving.value = false
  }
}

async function openEdit(row) {
  try {
    const content = await getTestCase(problemId, row.index)
    editingIndex.value = row.index
    editForm.input = content.input
    editForm.output = content.output
    editVisible.value = true
  } catch (e) {
  }
}

async function handleEditSave() {
  saving.value = true
  try {
    await updateTestCase(problemId, editingIndex.value, {
      input: editForm.input,
      output: editForm.output
    })
    ElMessage.success(`测试点 ${editingIndex.value} 已更新`)
    editVisible.value = false
    await fetchAll()
  } catch (e) {
  } finally {
    saving.value = false
  }
}

async function handleSaveConfigs() {
  const dirty = cases.value.filter((c) => c._dirty)
  try {
    for (const row of dirty) {
      await updateTestCaseConfig(problemId, row.index, {
        score: row.score,
        timeLimit: row.timeLimit ?? null,
        memoryLimit: row.memoryLimit ?? null
      })
    }
    ElMessage.success(`已保存 ${dirty.length} 个测试点配置`)
    await fetchAll()
  } catch (e) {
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除测试点 ${row.index} 吗? 后续测试点序号会前移`,
      '删除测试点',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  try {
    await deleteTestCase(problemId, row.index)
    ElMessage.success('已删除')
    await fetchAll()
  } catch (e) {
  }
}

function onZipChange(file) {
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('zip 文件超过 10MB')
    zipFile.value = null
    uploadRef.value?.clearFiles()
    return
  }
  zipFile.value = file.raw
}

async function handleUpload() {
  try {
    await ElMessageBox.confirm(
      '上传将删除现有全部测试点并替换为 zip 内容, 确定继续?',
      '替换测试点',
      { type: 'warning', confirmButtonText: '确定替换', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  const fd = new FormData()
  fd.append('file', zipFile.value)
  try {
    const result = await uploadTestCases(problemId, fd)
    ElMessage.success(`已导入 ${result.count} 个测试点, 满分 ${result.totalScore}`)
    zipFile.value = null
    uploadRef.value?.clearFiles()
    await fetchAll()
  } catch (e) {
  }
}

onMounted(fetchAll)
</script>

<style scoped>
.problem-manage {
  background: var(--bg);
  min-height: calc(100vh - var(--header-height));
}

.container {
  max-width: 1280px;
  margin: 0 auto;
  padding: 16px 16px 80px;
}

.back {
  margin-bottom: 12px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  margin: 0 0 18px;
}

.manage-section {
  margin-bottom: 20px;
  padding: 20px 24px;
}

.danger-hint {
  margin-top: 8px;
  color: var(--text-3);
  font-size: 13px;
}

.section-h {
  font-size: 16px;
  font-weight: 700;
  border-bottom: 1px solid var(--border);
  padding-bottom: 10px;
  margin: 0 0 14px;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.total-score {
  font-size: 13px;
  font-weight: normal;
  color: var(--text-3);
  font-family: var(--font-mono);
}

.empty-alert {
  margin-bottom: 16px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.upload-hint {
  font-size: 12px;
  color: var(--text-3);
  margin-bottom: 16px;
}

.case-table {
  margin-top: 8px;
}

/* 灰条标签 + 等宽输入框(与题目详情页样式一致) */
.sample-label {
  background: var(--bg-soft);
  color: var(--text-2);
  padding: 5px 10px;
  font-size: 12px;
  font-weight: 600;
}

.case-form {
  border: 1px solid var(--border);
  border-radius: var(--radius);
  overflow: hidden;
}

.mono-input :deep(.el-textarea__inner) {
  border: none;
  border-radius: 0;
  box-shadow: none;
  font-family: var(--font-mono);
  font-size: 13px;
  background: var(--bg);
  color: var(--text);
}

.config-row {
  display: flex;
  gap: 12px;
  padding: 10px 12px;
  border-top: 1px solid var(--border);
}

.config-row > div {
  flex: 1;
}

.config-row .sample-label {
  margin-bottom: 6px;
  background: transparent;
  padding-left: 0;
}

@media (max-width: 640px) {
  .manage-section {
    padding: 16px 14px;
  }
}
</style>
