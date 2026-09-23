<template>
  <div class="problem-detail">
    <div class="container">
      <el-page-header @back="router.back()" class="back" />

      <!-- 标题行: 左对齐标题 + 右侧功能按钮 -->
      <div v-if="problem" class="title-row">
        <h1 class="problem-title">{{ problem.title }}</h1>
        <div class="title-actions">
          <el-button size="small" plain @click="router.push(`/problems/${problemId}/discussion`)">
            讨论
          </el-button>
          <el-button size="small" plain @click="router.push(`/problems/${problemId}/solution`)">
            题解
          </el-button>
          <el-button size="small" plain @click="router.push(`/problems/${problemId}/submissions`)">
            提交记录
          </el-button>
          <el-button
            v-if="problem.canManage"
            size="small"
            plain
            @click="router.push(`/problems/${problemId}/manage`)"
          >
            管理题目{{ problem.testCaseCount ? ` (${problem.testCaseCount})` : '' }}
          </el-button>
        </div>
      </div>

      <!-- 元信息行: 来源 / 限制 / 判题模式 / 难度 -->
      <div v-if="problem" class="meta-line">
        <span v-if="problem.source" class="meta-item">{{ problem.source }}</span>
        <span class="meta-item">
          时间限制
          <span class="mono meta-val">{{ (problem.timeLimit / 1000).toFixed(2) }} s</span>
        </span>
        <span class="meta-item">
          内存限制
          <span class="mono meta-val">{{ problem.memoryLimit }} MB</span>
        </span>
        <span class="meta-item">{{ problem.judgeMode ?? 'ICPC' }}</span>
        <span class="meta-item">
          难度
          <span class="meta-val rating" :style="{ color: ratingColor(problem.difficulty) }">
            {{ problem.difficulty }}
          </span>
        </span>
      </div>

      <!-- 题目标签(洛谷式: 默认隐藏, 手动选择显示, 防止剧透思路) -->
      <div v-if="problem?.tags?.length" class="detail-tags-area">
        <el-link type="primary" :underline="false" class="tags-toggle" @click="showTags = !showTags">
          {{ showTags ? '隐藏标签' : '显示标签' }}
        </el-link>
        <div v-if="showTags" class="detail-tags">
          <el-tag v-for="t in problem.tags" :key="t" size="small" class="tag-chip">{{ t }}</el-tag>
        </div>
      </div>

      <!-- 比赛提交模式横幅 -->
      <div v-if="contestId" class="contest-banner">
        <el-icon><Trophy /></el-icon>
        <span>比赛提交模式: 本页提交将计入比赛 #{{ contestId }}</span>
        <el-link type="primary" :underline="false" class="banner-link" @click="router.push(`/contests/${contestId}`)">
          返回比赛
        </el-link>
      </div>

      <!-- 题面 -->
      <div v-loading="loading" class="statement">
        <template v-if="problem">
          <div class="markdown-body" v-html="renderMarkdown(problem.description)"></div>

          <div v-if="problem.inputDescription" class="section">
            <h3>输入格式</h3>
            <div class="markdown-body" v-html="renderMarkdown(problem.inputDescription)"></div>
          </div>

          <div v-if="problem.outputDescription" class="section">
            <h3>输出格式</h3>
            <div class="markdown-body" v-html="renderMarkdown(problem.outputDescription)"></div>
          </div>

          <div v-if="problem.samples?.length" class="section">
            <h3>样例</h3>
            <div v-for="(sample, idx) in problem.samples" :key="idx" class="sample-card">
              <div class="sample-head">
                <span class="sample-name">样例 {{ idx + 1 }}</span>
                <span class="sample-copy">
                  <el-link type="primary" :underline="false" @click="copyText(sample.input)">
                    复制输入
                  </el-link>
                  <el-link type="primary" :underline="false" @click="copyText(sample.output)">
                    复制输出
                  </el-link>
                </span>
              </div>
              <div class="sample-io">
                <div class="sample-item">
                  <div class="sample-label">输入</div>
                  <pre class="sample-content">{{ sample.input }}</pre>
                </div>
                <div class="sample-item">
                  <div class="sample-label">输出</div>
                  <pre class="sample-content">{{ sample.output }}</pre>
                </div>
              </div>
              <div v-if="sample.explanation" class="sample-explain">
                <div class="explain-label">样例解释</div>
                <div class="markdown-body" v-html="renderMarkdown(sample.explanation)"></div>
              </div>
            </div>
          </div>
        </template>
      </div>

      <!-- 提交区: 编辑器卡片 -->
      <div class="section submit-section">
        <h3>代码提交</h3>
        <div class="editor-card">
          <div class="editor-head">
            <el-select v-model="language" style="width: 150px">
              <el-option
                v-for="opt in languageOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
            <span class="editor-hint">切换语言将重置为对应模板</span>
          </div>
          <div class="editor-wrapper">
            <vue-monaco-editor
              v-model:value="code"
              :language="monacoLang"
              :options="editorOptions"
              height="420px"
            />
          </div>
          <div class="submit-bar">
            <el-button
              type="primary"
              :loading="submitting"
              :disabled="judging"
              @click="handleSubmit"
            >
              {{ judging ? '判题中...' : '提交' }}
            </el-button>
          </div>
        </div>
      </div>

      <!-- 自定义测试卡片 -->
      <div class="section custom-test-section">
        <h3>自定义测试</h3>
        <div class="test-card">
          <div class="sample-label">输入</div>
          <el-input
            v-model="testInput"
            type="textarea"
            :rows="4"
            placeholder="在这里输入测试数据(标准输入)..."
          />
          <div class="test-actions">
            <el-button type="primary" plain :loading="testing" @click="handleCustomTest">
              运行自测
            </el-button>
          </div>
          <template v-if="testResult">
            <div class="sample-label" :class="{ 'error-label': testResult.error }">
              {{ testResult.error ? '错误信息' : '标准输出' }}
            </div>
            <pre :class="{ 'error-text': testResult.error }" class="test-result">
              {{ testResult.error || testResult.output || '(无输出)' }}
            </pre>
          </template>
        </div>
      </div>
    </div>

    <!-- 判题结果弹窗 -->
    <el-dialog v-model="resultVisible" title="判题结果" width="520px" align-center>
      <div v-if="lastResult" class="verdict">
        <div class="verdict-short" :style="{ color: verdictOf(lastResult.status).color }">
          {{ verdictOf(lastResult.status).short }}
        </div>
        <div class="verdict-full">{{ verdictText(lastResult.status, lastResult.failedTestIndex) }}</div>
        <div class="verdict-meta">
          <span v-if="lastResult.timeUsed != null" class="mono">{{ lastResult.timeUsed }} ms</span>
          <span v-if="lastResult.memoryUsed != null" class="mono">{{ lastResult.memoryUsed }} KB</span>
          <span v-if="totalFullScore != null" class="mono">
            得分 {{ lastResult.score ?? 0 }} / {{ totalFullScore }}
          </span>
        </div>
        <div v-if="lastResult.judgeDetail?.length" class="case-summary">
          <template v-if="lastResult.failedTestIndex != null">
            已测 {{ lastResult.judgeDetail.length }} 个测试点
          </template>
          <template v-else>
            通过 {{ passedCount }} / {{ lastResult.judgeDetail.length }}
          </template>
        </div>
        <div v-if="lastResult.judgeDetail?.length" class="case-table-wrap">
          <table class="case-table">
            <thead>
              <tr>
                <th>用例</th>
                <th>结果</th>
                <th>得分</th>
                <th>耗时</th>
                <th>内存</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="c in lastResult.judgeDetail" :key="c.caseName">
                <td class="mono">{{ c.caseName }}</td>
                <td>
                  <span class="vpill" :class="verdictPillClass(c.status)">
                    {{ verdictOf(c.status).short }}
                  </span>
                </td>
                <td class="mono">{{ c.fullScore != null ? `${c.score} / ${c.fullScore}` : '—' }}</td>
                <td class="mono">{{ c.timeUsed != null ? c.timeUsed + ' ms' : '—' }}</td>
                <td class="mono">{{ c.memoryUsed != null ? c.memoryUsed + ' KB' : '—' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-if="lastResult.errorMessage" class="verdict-error">
          <div class="sample-label error-label">错误信息</div>
          <pre>{{ lastResult.errorMessage }}</pre>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Trophy } from '@element-plus/icons-vue'
import { VueMonacoEditor } from '@guolao/vue-monaco-editor'
import { getProblemDetail } from '../api/problem'
import { customTest, getSubmission, submitCode } from '../api/submission'
import { ratingColor } from '../utils/rating'
import { renderMarkdown } from '../utils/markdown'
import { verdictOf, verdictText } from '../utils/verdict'
import { getContestDetail } from '../api/contest'
import { getContestToken } from '../utils/contestToken'

const route = useRoute()
const router = useRouter()
const problemId = Number(route.params.id)

// 比赛提交模式: /problems/:id?contest=比赛ID
const contestId = ref(null)

const loading = ref(false)
const problem = ref(null)

// 标签显隐(洛谷式: 默认隐藏)
const showTags = ref(false)

// 语言选项: value 与后端 Language 枚举名一致
const languageOptions = [
  { value: 'CPP', label: 'C++' },
  { value: 'JAVA', label: 'Java' },
  { value: 'C', label: 'C' },
  { value: 'PYTHON3', label: 'Python3' },
  { value: 'GO', label: 'Go' },
  { value: 'JAVASCRIPT', label: 'JavaScript' }
]

// 默认 C++
const language = ref('CPP')

// 后端语言枚举名 -> Monaco 语言 ID
const monacoLangMap = {
  CPP: 'cpp',
  JAVA: 'java',
  C: 'c',
  PYTHON3: 'python',
  GO: 'go',
  JAVASCRIPT: 'javascript'
}
const monacoLang = computed(() => monacoLangMap[language.value] ?? 'cpp')

// 各语言初始代码模板
const codeTemplates = {
  CPP: '#include <bits/stdc++.h>\nusing namespace std;\n\nint main() {\n    \n    return 0;\n}\n',
  JAVA: 'import java.util.*;\n\npublic class Main {\n    public static void main(String[] args) {\n        \n    }\n}\n',
  C: '#include <stdio.h>\n\nint main() {\n    \n    return 0;\n}\n',
  PYTHON3: 'import sys\n\ndef main():\n    pass\n\nif __name__ == "__main__":\n    main()\n',
  GO: 'package main\n\nfunc main() {\n    \n}\n',
  JAVASCRIPT: 'function main() {\n    \n}\n\nmain();\n'
}
const code = ref(codeTemplates.CPP)

// 切换语言时替换为对应模板(简化处理; 真实 OJ 通常按语言分别缓存代码)
watch(language, (val) => {
  code.value = codeTemplates[val] ?? ''
})

// 编辑器主题跟随全站明暗
const editorTheme = computed(() =>
  document.documentElement.classList.contains('dark') ? 'vs-dark' : 'vs'
)

const editorOptions = computed(() => ({
  theme: editorTheme.value,
  fontSize: 14,
  minimap: { enabled: false },
  automaticLayout: true
}))

const submitting = ref(false)
const judging = ref(false)

// 判题终态(其余 PENDING/JUDGING 为进行中)
const TERMINAL_STATUSES = [
  'ACCEPTED', 'WRONG_ANSWER', 'TIME_LIMIT_EXCEEDED', 'MEMORY_LIMIT_EXCEEDED',
  'RUNTIME_ERROR', 'COMPILE_ERROR', 'SYSTEM_ERROR'
]

// 判题结果弹窗状态
const resultVisible = ref(false)
const lastResult = ref(null)

// 弹窗汇总: 通过的用例数
const passedCount = computed(() =>
  (lastResult.value?.judgeDetail ?? []).filter((c) => c.status === 'ACCEPTED').length
)

// 弹窗满分(计分制): 各用例满分之和; 样例回退模式下 fullScore 为 null -> 不显示得分
const totalFullScore = computed(() => {
  const detail = lastResult.value?.judgeDetail ?? []
  if (!detail.some((c) => c.fullScore != null)) return null
  return detail.reduce((sum, c) => sum + (c.fullScore ?? 0), 0)
})

// 判定 -> vpill 样式类(base.css 定义)
const PILL_CLASS = {
  ACCEPTED: 'v-ac',
  WRONG_ANSWER: 'v-wa',
  TIME_LIMIT_EXCEEDED: 'v-tle',
  MEMORY_LIMIT_EXCEEDED: 'v-mle',
  RUNTIME_ERROR: 'v-re',
  COMPILE_ERROR: 'v-ce',
  SYSTEM_ERROR: 'v-se',
  PENDING: 'v-pending',
  JUDGING: 'v-judging'
}

function verdictPillClass(status) {
  return PILL_CLASS[status] ?? 'v-ce'
}

// 自定义测试状态
const testInput = ref('')
const testing = ref(false)
const testResult = ref(null)

/** 复制文本到剪贴板(样例复制) */
async function copyText(text) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板')
  } catch (e) {
    ElMessage.error('复制失败, 请手动复制')
  }
}

async function fetchDetail() {
  loading.value = true
  try {
    problem.value = await getProblemDetail(problemId)
    // 换题后标签恢复默认隐藏
    showTags.value = false
  } catch (e) {
  } finally {
    loading.value = false
  }
}

// 检测比赛上下文: 有 contest 参数且持有有效访问 token 才进入比赛提交模式
async function setupContestMode() {
  const cidRaw = route.query.contest
  if (!cidRaw) return
  const cid = Number(cidRaw)
  if (!Number.isInteger(cid) || cid <= 0) return
  const token = getContestToken(cid)
  if (!token) return
  try {
    await getContestDetail(cid, token)
    contestId.value = cid
  } catch (e) {
    // 无权限或比赛不存在, 按普通模式处理
  }
}

async function handleSubmit() {
  if (!code.value.trim()) {
    ElMessage.warning('代码不能为空')
    return
  }
  submitting.value = true
  try {
    // userId 由后端从登录令牌解析, 前端不再传; 比赛模式带 contestId 与访问 token
    const submissionId = await submitCode(
      {
        problemId,
        contestId: contestId.value ?? undefined,
        language: language.value,
        code: code.value
      },
      contestId.value ? getContestToken(contestId.value) : ''
    )
    judging.value = true
    ElMessage.success(`提交成功(提交ID: ${submissionId}), 正在判题...`)
    const result = await pollResult(submissionId)
    judging.value = false
    if (result) {
      showResult(result)
    } else {
      ElMessageBox.alert('判题超时, 请稍后在提交记录中查看结果', '判题结果', {
        type: 'warning',
        confirmButtonText: '确定'
      })
    }
  } catch (e) {
    judging.value = false
  } finally {
    submitting.value = false
  }
}

// 轮询判题结果: 每秒查一次, 最长 180 秒(编译较慢且测试点多时判题时间会拉长)
async function pollResult(submissionId) {
  const deadline = Date.now() + 180000
  while (Date.now() < deadline) {
    await sleep(1000)
    try {
      const sub = await getSubmission(submissionId)
      if (TERMINAL_STATUSES.includes(sub.status)) {
        return sub
      }
    } catch (e) {
      // 单次轮询失败(如网络抖动), 继续重试直到超时
    }
  }
  return null
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

// 打开 AtCoder 风格结果弹窗
function showResult(sub) {
  lastResult.value = sub
  resultVisible.value = true
}

// 自定义测试: 用输入的测试数据真实运行代码
async function handleCustomTest() {
  if (!code.value.trim()) {
    ElMessage.warning('代码不能为空')
    return
  }
  testing.value = true
  testResult.value = null
  try {
    testResult.value = await customTest({
      language: language.value,
      code: code.value,
      input: testInput.value
    })
  } catch (e) {
  } finally {
    testing.value = false
  }
}

onMounted(() => {
  fetchDetail()
  setupContestMode()
})
</script>

<style scoped>
.problem-detail {
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

/* 标题行: 左对齐 + 右侧操作按钮 */
.title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.problem-title {
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.01em;
  margin: 0;
  word-break: break-word;
  flex: 1;
  min-width: 240px;
}

.title-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-shrink: 0;
}

/* 元信息行 */
.meta-line {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 20px;
  margin: 10px 0 0;
  color: var(--text-2);
  font-size: 13px;
}

.meta-val {
  color: var(--text);
  font-weight: 600;
}

.rating {
  font-weight: 700;
}

/* 标签 */
.detail-tags-area {
  margin: 12px 0 0;
}

.tags-toggle {
  font-size: 13px;
}

.detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.tag-chip {
  margin-right: 0;
}

/* 比赛提交模式横幅 */
.contest-banner {
  margin: 16px 0 0;
  padding: 10px 14px;
  background: var(--brand-soft);
  border: 1px solid var(--brand);
  border-left-width: 3px;
  border-radius: var(--radius);
  color: var(--text);
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.contest-banner .el-icon {
  color: var(--brand);
}

.banner-link {
  font-size: 13px;
  margin-left: auto;
}

/* 题面 */
.statement {
  margin-top: 20px;
  min-height: 80px;
}

/* 章节标题: 底部细线 */
.section {
  margin-top: 32px;
}

.section h3 {
  font-size: 17px;
  font-weight: 600;
  border-bottom: 1px solid var(--border);
  padding-bottom: 8px;
  margin: 0 0 14px;
}

/* 样例卡片 */
.sample-card {
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--bg);
  box-shadow: var(--shadow-sm);
  margin-bottom: 20px;
  overflow: hidden;
}

.sample-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background: var(--bg-soft);
  border-bottom: 1px solid var(--border);
}

.sample-name {
  font-weight: 600;
  font-size: 14px;
}

.sample-copy {
  display: flex;
  gap: 14px;
}

.sample-copy .el-link {
  font-size: 12.5px;
}

.sample-io {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0;
}

.sample-item {
  min-width: 0;
}

.sample-item + .sample-item {
  border-left: 1px solid var(--border);
}

.sample-label {
  background: var(--bg-soft);
  padding: 5px 16px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-2);
  border-bottom: 1px solid var(--border);
}

.sample-content {
  margin: 0;
  padding: 12px 16px;
  background: var(--bg);
  font-family: var(--font-mono);
  font-size: 13.5px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
  color: var(--text);
  max-height: 320px;
  overflow: auto;
}

/* 样例解释: 浅蓝提示块 */
.sample-explain {
  padding: 12px 16px;
  background: var(--brand-soft);
  border-top: 1px solid var(--border);
  color: var(--text);
  line-height: 1.7;
}

.explain-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--brand);
  margin-bottom: 4px;
  letter-spacing: 0.5px;
}

.sample-explain :deep(p) {
  margin: 0;
}

.sample-explain :deep(p + p) {
  margin-top: 6px;
}

/* 编辑器卡片 */
.editor-card {
  border: 1px solid var(--border);
  border-radius: var(--radius);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}

.editor-head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  background: var(--bg-soft);
  border-bottom: 1px solid var(--border);
}

.editor-hint {
  color: var(--text-3);
  font-size: 12.5px;
}

.editor-wrapper {
  border-bottom: 1px solid var(--border);
}

.submit-bar {
  padding: 12px 14px;
  text-align: right;
  background: var(--bg);
}

.submit-bar .el-button {
  min-width: 110px;
}

/* 自定义测试卡片 */
.test-card {
  border: 1px solid var(--border);
  border-radius: var(--radius);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}

.test-card .sample-label {
  border-radius: 0;
}

.test-card :deep(.el-textarea__inner) {
  border: none;
  border-radius: 0;
  box-shadow: none;
  font-family: var(--font-mono);
  font-size: 13px;
  padding: 12px 16px;
}

.test-actions {
  padding: 12px 16px;
  border-top: 1px solid var(--border);
}

.test-result {
  margin: 0;
  padding: 12px 16px;
  border-top: 1px solid var(--border);
  background: var(--bg);
  font-family: var(--font-mono);
  font-size: 13px;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 300px;
  overflow: auto;
  color: var(--text);
}

.error-label {
  background: var(--bad-soft);
  color: var(--bad);
}

.error-text {
  color: var(--bad);
}

/* 判题结果弹窗 */
.verdict {
  text-align: center;
}

.verdict-short {
  font-size: 44px;
  font-weight: 700;
  font-family: var(--font-mono);
  letter-spacing: 1px;
}

.verdict-full {
  font-size: 14px;
  color: var(--text-2);
  margin: 4px 0 8px;
}

.verdict-meta {
  display: flex;
  justify-content: center;
  gap: 18px;
  font-size: 13px;
  color: var(--text-2);
  margin-bottom: 16px;
}

/* 用例汇总行 */
.case-summary {
  font-size: 13px;
  color: var(--text-2);
  margin-bottom: 8px;
}

/* 用例多时表格区域滚动 */
.case-table-wrap {
  max-height: 280px;
  overflow-y: auto;
  border: 1px solid var(--border);
  border-radius: var(--radius);
}

.case-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.case-table th {
  background: var(--bg-soft);
  border-bottom: 1px solid var(--border);
  padding: 7px 10px;
  font-weight: 600;
  color: var(--text-2);
  font-size: 12px;
  position: sticky;
  top: 0;
}

.case-table td {
  border-bottom: 1px solid var(--border);
  padding: 7px 10px;
  text-align: center;
  color: var(--text);
}

.case-table tbody tr:last-child td {
  border-bottom: none;
}

.case-table tbody tr:hover td {
  background: var(--bg-hover);
}

.verdict-error {
  text-align: left;
  margin-top: 12px;
}

.verdict-error pre {
  margin: 0;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: 0 0 var(--radius) var(--radius);
  background: var(--bg);
  font-family: var(--font-mono);
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 200px;
  overflow-y: auto;
  color: var(--bad);
}

/* 移动端: 样例并排改上下 */
@media (max-width: 640px) {
  .sample-io {
    grid-template-columns: 1fr;
  }

  .sample-item + .sample-item {
    border-left: none;
    border-top: 1px solid var(--border);
  }

  .title-actions {
    width: 100%;
  }
}
</style>
