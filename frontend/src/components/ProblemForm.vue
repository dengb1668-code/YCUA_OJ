<template>
  <div class="problem-form">
    <!-- 左: 编辑表单 -->
    <div class="form-col" :class="{ 'full-width': previewMode === 'edit' }">
      <div class="preview-switch">
        <el-radio-group v-model="previewMode" size="small">
          <el-radio value="split">分屏实时预览</el-radio>
          <el-radio value="edit">仅编辑</el-radio>
        </el-radio-group>
      </div>
      <el-form label-width="110px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="如: A+B Problem" />
        </el-form-item>
        <el-form-item label="来源">
          <el-input v-model="form.source" placeholder="可选, 如: 洛谷 P1001" />
        </el-form-item>
        <el-form-item label="题目描述" required>
          <MdToolbar v-model="form.description" :rows="8" placeholder="支持 Markdown 与 $LaTeX$ 公式" />
        </el-form-item>
        <el-form-item label="输入格式">
          <MdToolbar v-model="form.inputDescription" :rows="3" placeholder="支持 Markdown 与 $LaTeX$" />
        </el-form-item>
        <el-form-item label="输出格式">
          <MdToolbar v-model="form.outputDescription" :rows="3" placeholder="支持 Markdown 与 $LaTeX$" />
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
                <MdToolbar
                  v-model="s.explanation"
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
        <el-form-item label="判题模式">
          <el-radio-group v-model="form.judgeMode">
            <el-radio value="ICPC">ICPC</el-radio>
            <el-radio value="IOI">IOI</el-radio>
          </el-radio-group>
          <div class="mode-hint">ICPC: 无部分分, 第一个失败测试点即停(CF 式); IOI: 逐测试点部分分</div>
        </el-form-item>
        <el-form-item label="标签">
          <el-select
            v-model="form.tags"
            multiple
            filterable
            :multiple-limit="5"
            placeholder="最多选 5 个标签"
            style="width: 100%"
          >
            <el-option v-for="t in allTags" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">{{ submitText }}</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 右: 实时预览(可切换为仅编辑模式) -->
    <div v-if="previewMode === 'split'" class="preview-col">
      <div class="preview-panel">
        <div class="preview-head">实时预览</div>
        <h2 class="problem-title">{{ form.title.trim() || '题目标题' }}</h2>
        <div v-if="form.source" class="problem-source">{{ form.source }}</div>
        <div class="limits">
          <span>
            时间限制
            <b class="mono">{{ (form.timeLimit / 1000).toFixed(2) }} s</b>
          </span>
          <span>
            内存限制
            <b class="mono">{{ form.memoryLimit }} MB</b>
          </span>
          <span>{{ form.judgeMode }}</span>
          <span>
            难度
            <b class="mono rating" :style="{ color: ratingColor(form.difficulty) }">{{ form.difficulty }}</b>
          </span>
        </div>

        <div v-if="form.tags.length" class="preview-tags">
          <el-tag v-for="t in form.tags" :key="t" size="small" class="preview-tag">{{ t }}</el-tag>
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
          <div v-for="(sample, idx) in filledSamples" :key="idx" class="sample-card">
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { renderMarkdown } from '../utils/markdown'
import { ratingColor } from '../utils/rating'
import { getProblemTags } from '../api/problem'
import MdToolbar from './MdToolbar.vue'

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

// 固定中文标签集(后端返回; 加载失败不阻塞表单)
const allTags = ref([])
onMounted(async () => {
  try {
    allTags.value = await getProblemTags()
  } catch {
    /* 忽略, 表单其余功能不受影响 */
  }
})

const defaults = () => ({
  title: '',
  source: '',
  description: '',
  inputDescription: '',
  outputDescription: '',
  samples: [{ input: '', output: '' }],
  timeLimit: 1000,
  memoryLimit: 256,
  difficulty: 800,
  judgeMode: 'ICPC',
  tags: []
})

const form = reactive(defaults())

// 预览模式: split=分屏实时预览 / edit=仅编辑
const previewMode = ref('split')

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
    form.judgeMode = v.judgeMode ?? 'ICPC'
    form.tags = v.tags?.length ? [...v.tags] : []
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
    difficulty: form.difficulty,
    judgeMode: form.judgeMode,
    tags: form.tags
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

/* 仅编辑模式: 表单占满整行 */
.form-col.full-width {
  max-width: 100%;
}

.preview-col {
  flex: 1;
  min-width: 0;
}

/* 预览模式切换: 表单右上角 */
.preview-switch {
  text-align: right;
  margin-bottom: 12px;
}

.preview-panel {
  position: sticky;
  top: calc(var(--header-height) + 12px);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 16px 20px 24px;
  background: var(--bg);
  box-shadow: var(--shadow-sm);
  max-height: calc(100vh - var(--header-height) - 28px);
  overflow-y: auto;
}

.preview-head {
  font-size: 12px;
  color: var(--brand);
  font-weight: 700;
  text-align: right;
  margin-bottom: 4px;
  letter-spacing: 0.04em;
}

.problem-title {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.01em;
  margin: 8px 0 4px;
}

.problem-source {
  color: var(--text-3);
  font-size: 13px;
  margin: 0 0 14px;
}

.rating {
  font-weight: 700;
}

.limits {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 18px;
  color: var(--text-2);
  font-size: 13px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border);
  margin-bottom: 16px;
}

.limits b {
  color: var(--text);
  font-weight: 600;
}

.section {
  margin-top: 20px;
}

.section h3 {
  font-size: 16px;
  font-weight: 600;
  border-bottom: 1px solid var(--border);
  padding-bottom: 6px;
  margin: 0 0 10px;
}

.sample-card {
  margin-bottom: 16px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  overflow: hidden;
  background: var(--bg);
}

.sample-heading {
  font-size: 14px;
  font-weight: 600;
  padding: 9px 14px;
  background: var(--bg-soft);
  border-bottom: 1px solid var(--border);
}

.sample-item {
  padding: 10px 14px;
}

.sample-item + .sample-item {
  border-top: 1px solid var(--border);
}

.sample-label-line {
  font-size: 12px;
  color: var(--text-3);
  margin-bottom: 4px;
}

.sample-content {
  margin: 0;
  padding: 10px 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--bg-soft);
  font-family: var(--font-mono);
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
  color: var(--text);
}

.sample-explain {
  padding: 10px 14px;
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
  border: 1px solid var(--border);
  border-radius: var(--radius);
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
  color: var(--text-2);
  margin-bottom: 4px;
}

.unit {
  margin-left: 8px;
  color: var(--text-3);
  font-size: 13px;
}

.mode-hint {
  margin-top: 4px;
  color: var(--text-3);
  font-size: 12px;
  line-height: 1.5;
}

.preview-tags {
  margin: -8px 0 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.preview-tag {
  margin-right: 0;
}

/* 窄屏隐藏预览, 表单占满 */
@media (max-width: 1000px) {
  .preview-col {
    display: none;
  }
}
</style>
