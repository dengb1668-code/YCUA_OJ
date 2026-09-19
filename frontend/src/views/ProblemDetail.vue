<template>
  <div class="problem-detail">
    <div class="container">
      <el-page-header @back="router.back()" class="back" />

      <!-- 标题与限制(AtCoder 风格: 居中标题 + 边框限制框) -->
      <h2 class="problem-title">{{ problem?.title ?? '题目详情' }}</h2>
      <div v-if="problem?.source" class="problem-source">{{ problem.source }}</div>
      <div v-if="problem" class="limits">
        <div>
          难度:
          <span class="rating" :style="{ color: ratingColor(problem.difficulty) }">
            {{ problem.difficulty }}
          </span>
        </div>
        <div>时间限制: {{ (problem.timeLimit / 1000).toFixed(2) }} sec</div>
        <div>内存限制: {{ problem.memoryLimit }} MB</div>
        <div class="limits-actions">
          <el-button
            size="small"
            plain
            @click="router.push(`/problems/${problemId}/discussion`)"
          >
            讨论
          </el-button>
          <el-button
            size="small"
            plain
            @click="router.push(`/problems/${problemId}/solution`)"
          >
            题解
          </el-button>
          <el-button
            size="small"
            plain
            @click="router.push(`/problems/${problemId}/submissions`)"
          >
            提交记录
          </el-button>
          <el-button
            v-if="problem.canManage"
            size="small"
            plain
            @click="router.push(`/problems/${problemId}/manage`)"
          >
            管理题目{{ problem.testCaseCount ? ` (${problem.testCaseCount} 个测试点)` : '' }}
          </el-button>
        </div>
      </div>

      <!-- 比赛提交模式横幅 -->
      <div v-if="contestId" class="contest-banner">
        比赛提交模式: 本页提交将计入比赛 #{{ contestId }}
        <el-link type="primary" class="banner-link" @click="router.push(`/contests/${contestId}`)">
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
            <div v-for="(sample, idx) in problem.samples" :key="idx" class="sample">
              <div class="sample-heading">样例 {{ idx + 1 }}</div>
              <div class="sample-item">
                <div class="sample-label-line">输入</div>
                <pre class="sample-content">{{ sample.input }}</pre>
              </div>
              <div class="sample-item">
                <div class="sample-label-line">输出</div>
                <pre class="sample-content">{{ sample.output }}</pre>
              </div>
              <div v-if="sample.explanation" class="sample-explain">
                <div class="explain-label">样例解释</div>
                <div class="markdown-body" v-html="renderMarkdown(sample.explanation)"></div>
              </div>
            </div>
          </div>
        </template>
      </div>

      <!-- 提交区 -->
      <div class="section submit-section">
        <h3>代码提交</h3>
        <div class="toolbar">
          <el-select v-model="language" style="width: 160px">
            <el-option
              v-for="opt in languageOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
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

      <!-- 自定义测试(AtCoder コードテスト) -->
      <div class="section custom-test-section">
        <h3>自定义测试</h3>
        <div class="test-input">
          <div class="sample-label">输入</div>
          <el-input
            v-model="testInput"
            type="textarea"
            :rows="4"
            placeholder="在这里输入测试数据(标准输入)..."
          />
        </div>
        <div class="test-actions">
          <el-button type="primary" plain :loading="testing" @click="handleCustomTest">
            运行自测
          </el-button>
        </div>
        <div v-if="testResult" class="test-output">
          <div v-if="testResult.error" class="sample-label error-label">错误信息</div>
          <div v-else class="sample-label">标准输出</div>
          <pre :class="{ 'error-text': testResult.error }">{{ testResult.error || testResult.output || '(无输出)' }}</pre>
        </div>
      </div>
    </div>

    <!-- AtCoder 风格判题结果弹窗 -->
    <el-dialog v-model="resultVisible" title="判题结果" width="520px" align-center>
      <div v-if="lastResult" class="verdict">
        <div class="verdict-short" :style="{ color: verdictOf(lastResult.status).color }">
          {{ verdictOf(lastResult.status).short }}
        </div>
        <div class="verdict-full">{{ verdictOf(lastResult.status).label }}</div>
        <div class="verdict-meta">
          耗时 {{ lastResult.timeUsed != null ? lastResult.timeUsed + ' ms' : '—' }}
          / 内存 {{ lastResult.memoryUsed != null ? lastResult.memoryUsed + ' KB' : '—' }}
          <span v-if="totalFullScore != null"> / 得分 {{ lastResult.score ?? 0 }} / {{ totalFullScore }}</span>
        </div>
        <div v-if="lastResult.judgeDetail?.length" class="case-summary">
          通过 {{ passedCount }} / {{ lastResult.judgeDetail.length }}
        </div>
        <div v-if="lastResult.judgeDetail?.length" class="case-table-wrap">
          <table class="case-table">
            <thead>
              <tr>
                <th>用例</th>
                <th>结果</th>
                <th>得分</th>
                <th>耗时</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="c in lastResult.judgeDetail" :key="c.caseName">
                <td>{{ c.caseName }}</td>
                <td>
                  <span class="case-status" :style="{ color: verdictOf(c.status).color }">
                    {{ verdictOf(c.status).short }}
                  </span>
                </td>
                <td>{{ c.fullScore != null ? `${c.score} / ${c.fullScore}` : '—' }}</td>
                <td>{{ c.timeUsed != null ? c.timeUsed + ' ms' : '—' }}</td>
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
import { VueMonacoEditor } from '@guolao/vue-monaco-editor'
import { getProblemDetail } from '../api/problem'
import { customTest, getSubmission, submitCode } from '../api/submission'
import { ratingColor } from '../utils/rating'
import { renderMarkdown } from '../utils/markdown'
import { verdictOf } from '../utils/verdict'
import { getContestDetail } from '../api/contest'
import { getContestToken } from '../utils/contestToken'

const route = useRoute()
const router = useRouter()
const problemId = Number(route.params.id)

// 比赛提交模式: /problems/:id?contest=比赛ID
const contestId = ref(null)

const loading = ref(false)
const problem = ref(null)

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

// 浅色主题, 与 AtCoder 页面风格一致
const editorOptions = {
  theme: 'vs',
  fontSize: 14,
  minimap: { enabled: false },
  automaticLayout: true
}

const submitting = ref(false)
const judging = ref(false)

// 判题终态(其余 PENDING/JUDGING 为进行中)
const TERMINAL_STATUSES = [
  'ACCEPTED', 'WRONG_ANSWER', 'TIME_LIMIT_EXCEEDED', 'MEMORY_LIMIT_EXCEEDED',
  'RUNTIME_ERROR', 'COMPILE_ERROR', 'SYSTEM_ERROR'
]

// AtCoder 式判定展示(共用 utils/verdict.js)

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

// 自定义测试状态
const testInput = ref('')
const testing = ref(false)
const testResult = ref(null)

async function fetchDetail() {
  loading.value = true
  try {
    problem.value = await getProblemDetail(problemId)
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

/* 居中标题 + 限制信息框 */
.problem-title {
  text-align: center;
  font-size: 24px;
  font-weight: normal;
  margin: 8px 0 20px;
}

.problem-source {
  text-align: center;
  color: #888;
  font-size: 13px;
  margin: -12px 0 16px;
}

.rating {
  font-weight: 600;
  font-family: Helvetica, Arial, sans-serif;
}

.limits {
  border: 1px solid #ddd;
  padding: 10px 16px;
  margin-bottom: 28px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 48px;
  color: #333;
}

/* 讨论/题解/提交记录/管理按钮组贴右侧 */
.limits-actions {
  margin-left: auto;
  display: flex;
  gap: 8px;
  align-items: center;
}

/* 比赛提交模式横幅 */
.contest-banner {
  margin: -14px 0 20px;
  padding: 8px 14px;
  background: #f5f9ff;
  border-left: 3px solid #1a5cc8;
  border-radius: 0 4px 4px 0;
  color: #333;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.banner-link {
  font-size: 13px;
}

/* 章节标题: 底部细线 */
.section {
  margin-top: 28px;
}

.section h3 {
  font-size: 16px;
  font-weight: 600;
  border-bottom: 1px solid #eee;
  padding-bottom: 6px;
  margin: 0 0 12px;
}

/* 样例: 样例 N 为组标题, 输入/输出为行内小标签, 内容为圆角边框框 */
.sample {
  margin-bottom: 28px;
}

.sample-heading {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 10px;
  padding-bottom: 6px;
  border-bottom: 1px solid #eee;
}

.sample-item {
  margin-bottom: 10px;
}

.sample-label-line {
  font-size: 13px;
  color: #666;
  margin-bottom: 5px;
}

.sample-content {
  margin: 0;
  padding: 10px 14px;
  border: 1px solid #ddd;
  border-radius: 3px;
  background: #fafafa;
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}

/* 样例解释: 浅蓝提示块(主题蓝左边线 + 浅蓝底 + 圆角) */
.sample-explain {
  margin-top: 2px;
  padding: 10px 14px;
  background: #f5f9ff;
  border-left: 3px solid #1a5cc8;
  border-radius: 0 4px 4px 0;
  color: #333;
  line-height: 1.7;
}

.explain-label {
  font-size: 13px;
  font-weight: 600;
  color: #1a5cc8;
  margin-bottom: 4px;
  letter-spacing: 0.5px;
}

.sample-explain .markdown-body {
  font-size: 15px;
}

.sample-explain :deep(p) {
  margin: 0;
}

.sample-explain :deep(p + p) {
  margin-top: 6px;
}

/* 灰条标签(自定义测试区/判题弹窗用, 与样例样式区分) */
.sample-label {
  background: #eee;
  padding: 5px 10px;
  font-size: 13px;
  font-weight: 600;
}

/* 提交区 */
.submit-section {
  border-top: 2px solid #eee;
  padding-top: 20px;
}

.toolbar {
  margin-bottom: 12px;
}

.editor-wrapper {
  border: 1px solid #ddd;
}

.submit-bar {
  margin-top: 16px;
  text-align: right;
}

/* 自定义测试区 */
.test-input {
  border: 1px solid #ddd;
  margin-bottom: 12px;
}

.test-input :deep(.el-textarea__inner) {
  border: none;
  box-shadow: none;
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 13px;
}

.test-actions {
  margin-bottom: 12px;
}

.test-output pre {
  margin: 0;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-top: none;
  background: #fff;
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 13px;
  white-space: pre-wrap;
  word-break: break-all;
}

.error-label {
  background: #f2dede;
  color: #a94442;
}

.error-text {
  color: #d9534f;
}

/* 判题结果弹窗(AtCoder 风格) */
.verdict {
  text-align: center;
}

.verdict-short {
  font-size: 42px;
  font-weight: bold;
  font-family: Helvetica, Arial, sans-serif;
}

.verdict-full {
  font-size: 14px;
  color: #666;
  margin: 4px 0 8px;
}

.verdict-meta {
  font-size: 13px;
  color: #888;
  margin-bottom: 16px;
}

/* 用例汇总行 */
.case-summary {
  font-size: 13px;
  color: #555;
  margin-bottom: 6px;
}

/* 用例多时表格区域滚动 */
.case-table-wrap {
  max-height: 280px;
  overflow-y: auto;
}

.case-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.case-table th {
  background: #eee;
  border: 1px solid #ddd;
  padding: 6px 10px;
  font-weight: 600;
}

.case-table td {
  border: 1px solid #ddd;
  padding: 6px 10px;
  text-align: center;
}

.case-status {
  font-weight: bold;
}

.verdict-error {
  text-align: left;
  margin-top: 12px;
}

.verdict-error pre {
  margin: 0;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-top: none;
  background: #fff;
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 200px;
  overflow-y: auto;
  color: #a94442;
}
</style>
