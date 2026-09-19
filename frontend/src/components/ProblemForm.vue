<template>
  <el-form label-width="110px">
    <el-form-item label="标题" required>
      <el-input v-model="form.title" placeholder="如: A+B Problem" />
    </el-form-item>
    <el-form-item label="来源">
      <el-input v-model="form.source" placeholder="可选, 如: 洛谷 P1001" />
    </el-form-item>
    <el-form-item label="题目描述" required>
      <el-input v-model="form.description" type="textarea" :rows="8" placeholder="支持 Markdown 语法" />
    </el-form-item>
    <el-form-item label="输入格式">
      <el-input v-model="form.inputDescription" type="textarea" :rows="3" placeholder="支持 Markdown" />
    </el-form-item>
    <el-form-item label="输出格式">
      <el-input v-model="form.outputDescription" type="textarea" :rows="3" placeholder="支持 Markdown" />
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
</template>

<script setup>
import { reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'

/**
 * 题面表单(创建与编辑共用)
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

// 编辑模式: initial 就绪后回填(只取题面相关字段)
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
    // 过滤掉空样例
    samples: form.samples.filter((s) => s.input.trim() || s.output.trim()),
    timeLimit: form.timeLimit,
    memoryLimit: form.memoryLimit,
    difficulty: form.difficulty
  })
}
</script>

<style scoped>
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
</style>
