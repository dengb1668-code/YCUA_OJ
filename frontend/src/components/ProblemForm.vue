<template>
  <div class="problem-form">
    <!-- 左: 编辑表单 -->
    <div class="form-col">
      <el-form label-width="110px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="如: A+B Problem" />
        </el-form-item>
        <el-form-item label="来源">
          <el-input v-model="form.source" placeholder="可选, 如: 洛谷 P1001" />
        </el-form-item>
        <el-form-item label="题目描述" required>
          <el-input v-model="form.description" type="textarea" :rows="8" placeholder="支持 Markdown 与 $LaTeX$ 公式" />
        </el-form-item>
        <el-form-item label="输入格式">
          <el-input v-model="form.inputDescription" type="textarea" :rows="3" placeholder="支持 Markdown 与 $LaTeX$" />
        </el-form-item>
        <el-form-item label="输出格式">
          <el-input v-model="form.outputDescription" type="textarea" :rows="3" placeholder="支持 Markdown 与 $LaTeX$" />
        </el-form-item>

        <el-form-item label="样例">
          <div class="samples">
            <div v-for="(s, idx) in form.samples" :key="idx" class="sample-row">
              <div class="sample-row-top">
                <div class="sample-col">
                  <div class="col-label">样例 {{ idx + 1 }} 输入</div>
                  <el-input v-model="s.input" type="textarea" :rows="3" />
                </div>
                <div class="sample-col">
                  <div class="col-label">样例 {{ idx + 1 }} 输出</div>
                  <el-input v-model="s.output" type="textarea" :rows="3" />
                </div>
                <el-button link type="danger" @click="form.samples.splice(idx, 1)">删除</el-button>
              </div>
              <div class="sample-explain-input">
                <div class="col-label">样例 {{ idx + 1 }} 解释(可选, 支持 Markdown)</div>
                <el-input
                  v-model="s.explanation"
                  type="textarea"
                  :rows="2"
                  placeholder="如: 1 + 2 = 3，直接输出两数之和。"
                />
              </div>
            </div>
            <el-button @click="form.samples.push({ input: '', output: '' })">+ 添加样例</el-button>
          </div>
        </el-form-item>

        <el-form-item label="时间限制">
          <el-input-number v-model="form.timeLimit" :min="100" :step="100" :max="10000" />
          <span class="unit">毫秒(各测试点未单独设置时限时生效)</span>
        </el-form-item>
        <el-form-item label="内存限制">
          <el-input-number v-model="form.memoryLimit" :min="16" :step="64" :max="4096" />
          <span class="unit">MB(各测试点未单独设置内存时生效)</span>
        </el-form-item>
        <el-form-item label="难度(Rating)">
          <el-input-number
            v-model="form.difficulty"
            :min="800"
            :max="3500"
            :step="100"
          />
          <span class="unit">Codeforces 难度分, 如 800 / 1200 / 1900 / 2400</span>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">{{ submitText }}</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 右: 实时预览 -->
    <div class="preview-col">
      <div class="preview-panel">
        <div class="preview-head">实时预览</div>
        <h2 class="problem-title">{{ form.title.trim() || '题目标题' }}</h2>
        <div v-if="form.source" class="problem-source">{{ form.source }}</div>
        <div class="limits">
          <div>
            难度:
            <span class="rating" :style="{ color: ratingColor(form.difficulty) }">{{ form.difficulty }}</span>
          </div>
          <div>时间限制: {{ (form.timeLimit / 1000).toFixed(2) }} sec</div>
          <div>内存限制: {{ form.memoryLimit }} MB</div>
        </div>

        <div class="markdown-body" v-html="renderMarkdown(form.description)"></div>

        <div v-if="form.inputDescription" class="section">
          <h3>输入格式</h3>
          <div class="markdown-body" v-html="renderMarkdown(form.inputDescription)"></div>
        </div>

        <div v-if="form.outputDescription" class="section">
          <h3>输出格式</h3>
          <div class="markdown-body" v-html="renderMarkdown(form.outputDescription)"></div>
        </div>

        <div v-if="filledSamples.length" class="section">
          <h3>样例</h3>
          <div v-for="(sample, idx) in filledSamples" :key="idx" class="sample">
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
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { renderMarkdown } from '../utils/markdown'
import { ratingColor } from '../utils/rating'

/**
 * 题面表单(创建与编辑共用): 左侧编辑 + 右侧实时预览
 * props.initial: 预填的题目对象(编辑模式); 不传则为空表单(创建模式)
 * emit('submit', payload): 校验通过后抛出待提交的题面字段
 */
const props = defineProps({
  initial: { type: Object, default: null },
  submitText: { type: String, default: '保存' },
  submitting: { type: Boolean, default: false }
})
const emit = defineEmits(['submit'])

const defaults = () => ({
  title: '',
  source: '',
  description: '',
  inputDescription: '',
  outputDescription: '',
  samples: [{ input: '', output: '' }],
  timeLimit: 1000,
  memoryLimit: 256,
  difficulty: 800
})

const form = reactive(defaults())

// 预览只展示非空样例
const filledSamples = computed(() =>
  form.samples.filter((s) => s.input.trim() || s.output.trim())
)

watch(
  () => props.initial,
  (v) => {
    if (!v) return
    form.title = v.title ?? ''
    form.source = v.source ?? ''
    form.description = v.description ?? ''
    form.inputDescription = v.inputDescription ?? ''
    form.outputDescription = v.outputDescription ?? ''
    form.samples = v.samples?.length ? v.samples.map((s) => ({ ...s })) : [{ input: '', output: '' }]
    form.timeLimit = v.timeLimit ?? 1000
    form.memoryLimit = v.memoryLimit ?? 256
    form.difficulty = v.difficulty ?? 800
  },
  { immediate: true }
)

function handleSubmit() {
  if (!form.title.trim() || !form.description.trim()) {
    ElMessage.warning('标题和题目描述不能为空')
    return
  }
  emit('submit', {
    title: form.title,
    source: form.source,
    description: form.description,
    inputDescription: form.inputDescription,
    outputDescription: form.outputDescription,
    samples: form.samples.filter((s) => s.input.trim() || s.output.trim()),
    timeLimit: form.timeLimit,
    memoryLimit: form.memoryLimit,
    difficulty: form.difficulty
  })
}
</script>

<style scoped>
.problem-form {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.form-col {
  flex: 1;
  min-width: 0;
}

.preview-col {
  flex: 1;
  min-width: 0;
}

.preview-panel {
  position: sticky;
  top: 16px;
  border: 1px solid #eee;
  border-radius: 4px;
  padding: 16px 20px 24px;
  background: #fafafa;
  max-height: calc(100vh - 40px);
  overflow-y: auto;
}

.preview-head {
  font-size: 13px;
  color: #1a5cc8;
  font-weight: 600;
  text-align: right;
  margin-bottom: 4px;
}

.problem-title {
  text-align: center;
  font-size: 22px;
  font-weight: normal;
  margin: 8px 0 16px;
}

.problem-source {
  text-align: center;
  color: #888;
  font-size: 13px;
  margin: -10px 0 14px;
}

.rating {
  font-weight: 600;
  font-family: Helvetica, Arial, sans-serif;
}

.limits {
  border: 1px solid #ddd;
  background: #fff;
  padding: 8px 16px;
  margin-bottom: 20px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 28px;
  color: #333;
  font-size: 13px;
}

.section {
  margin-top: 20px;
}

.section h3 {
  font-size: 16px;
  font-weight: 600;
  border-bottom: 1px solid #ddd;
  padding-bottom: 6px;
  margin: 0 0 10px;
}

.sample {
  margin-bottom: 20px;
  background: #fff;
}

.sample-heading {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 10px;
  padding-bottom: 6px;
  border-bottom: 1px solid #ddd;
}

.sample-item {
  margin-bottom: 8px;
}

.sample-label-line {
  font-size: 13px;
  color: #666;
  margin-bottom: 4px;
}

.sample-content {
  margin: 0;
  padding: 10px 14px;
  border: 1px solid #ddd;
  border-radius: 3px;
  background: #fafafa;
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}

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
}

.sample-explain :deep(p) {
  margin: 0;
}

/* 表单区样式 */
.samples {
  width: 100%;
}

.sample-row {
  margin-bottom: 12px;
  border: 1px solid #eee;
  border-radius: 4px;
  padding: 10px;
}

.sample-row-top {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 8px;
}

.sample-col {
  flex: 1;
}

.col-label {
  font-size: 13px;
  color: #666;
  margin-bottom: 4px;
}

.unit {
  margin-left: 8px;
  color: #888;
  font-size: 13px;
}

/* 窄屏隐藏预览, 表单占满 */
@media (max-width: 1000px) {
  .preview-col {
    display: none;
  }
}
</style>
