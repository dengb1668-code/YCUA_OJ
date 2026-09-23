<template>
  <div class="md-editor">
    <!-- 洛谷式 Markdown 工具栏: 不熟悉 MD 语法也能点按钮插入 -->
    <div class="md-toolbar">
      <button type="button" title="加粗" @mousedown.prevent @click="wrap('**', '**')"><b>B</b></button>
      <button type="button" title="斜体" @mousedown.prevent @click="wrap('*', '*')"><i>I</i></button>
      <button type="button" title="删除线" @mousedown.prevent @click="wrap('~~', '~~')"><s>S</s></button>
      <span class="sep" />
      <button type="button" title="一级标题" @mousedown.prevent @click="linePrefix('# ')">H1</button>
      <button type="button" title="二级标题" @mousedown.prevent @click="linePrefix('## ')">H2</button>
      <button type="button" title="三级标题" @mousedown.prevent @click="linePrefix('### ')">H3</button>
      <span class="sep" />
      <button type="button" title="无序列表" @mousedown.prevent @click="linePrefix('- ')">列表</button>
      <button type="button" title="有序列表" @mousedown.prevent @click="linePrefix('1. ')">编号</button>
      <button type="button" title="引用" @mousedown.prevent @click="linePrefix('> ')">引用</button>
      <span class="sep" />
      <button type="button" title="行内代码" @mousedown.prevent @click="wrap('`', '`')">代码</button>
      <button type="button" title="代码块" @mousedown.prevent @click="codeBlock">代码块</button>
      <button type="button" title="链接" @mousedown.prevent @click="insertLink">链接</button>
      <button type="button" title="行内公式($...$)" @mousedown.prevent @click="wrap('$', '$')">公式</button>
      <button type="button" title="表格" @mousedown.prevent @click="insertTable">表格</button>
      <button type="button" title="分割线" @mousedown.prevent @click="insertAtCursor('\n---\n')">分割线</button>
    </div>
    <el-input
      ref="inputRef"
      :model-value="modelValue"
      type="textarea"
      :rows="rows"
      :placeholder="placeholder"
      class="md-textarea"
      @update:model-value="$emit('update:modelValue', $event)"
    />
  </div>
</template>

<script setup>
import { nextTick, ref } from 'vue'

/**
 * Markdown 编辑器(文本域 + 洛谷式工具栏)
 * v-model 与 el-input textarea 行为一致, 工具栏按钮在光标处插入语法或包裹选中文本
 */
const props = defineProps({
  modelValue: { type: String, default: '' },
  rows: { type: Number, default: 6 },
  placeholder: { type: String, default: '' }
})
const emit = defineEmits(['update:modelValue'])

const inputRef = ref(null)

/** 拿到原生 textarea(el-input 的 $el 是其外层 div) */
function nativeTextarea() {
  return inputRef.value?.$el?.querySelector?.('textarea') ?? null
}

/** 应用一次编辑: 用 text 替换 [start, end) 区间, 之后光标落在 start + cursorPos */
function applyEdit(start, end, text, cursorPos) {
  const el = nativeTextarea()
  if (!el) return
  const val = props.modelValue ?? ''
  const next = val.slice(0, start) + text + val.slice(end)
  emit('update:modelValue', next)
  nextTick(() => {
    el.focus()
    el.setSelectionRange(start + cursorPos, start + cursorPos)
  })
}

/** 包裹选中文本(无选中时插入空包裹, 光标落在中间) */
function wrap(prefix, suffix) {
  const el = nativeTextarea()
  if (!el) return
  const start = el.selectionStart
  const end = el.selectionEnd
  const selected = props.modelValue.slice(start, end)
  if (selected) {
    applyEdit(start, end, prefix + selected + suffix, prefix.length + selected.length + suffix.length)
  } else {
    applyEdit(start, end, prefix + suffix, prefix.length)
  }
}

/** 行首插入前缀(标题/列表/引用), 光标移到行内容后 */
function linePrefix(prefix) {
  const el = nativeTextarea()
  if (!el) return
  const val = props.modelValue ?? ''
  const pos = el.selectionStart
  const lineStart = val.lastIndexOf('\n', pos - 1) + 1
  applyEdit(lineStart, lineStart, prefix, prefix.length + (pos - lineStart))
}

/** 光标处插入文本, 光标落在插入内容之后 */
function insertAtCursor(text) {
  const el = nativeTextarea()
  if (!el) return
  applyEdit(el.selectionStart, el.selectionEnd, text, text.length)
}

/** 插入代码块, 光标落在两个 ``` 之间 */
function codeBlock() {
  const el = nativeTextarea()
  if (!el) return
  const selected = props.modelValue.slice(el.selectionStart, el.selectionEnd)
  const text = selected ? '```\n' + selected + '\n```' : '```\n\n```'
  applyEdit(el.selectionStart, el.selectionEnd, text, selected ? text.length : text.length - 4)
}

/** 插入链接模板: 有选中文本则直接包成链接, 否则插入模板并选中"链接文字" */
function insertLink() {
  const el = nativeTextarea()
  if (!el) return
  const selected = props.modelValue.slice(el.selectionStart, el.selectionEnd)
  if (selected) {
    applyEdit(el.selectionStart, el.selectionEnd, '[' + selected + '](https://)', selected.length + 3)
  } else {
    applyEdit(el.selectionStart, el.selectionEnd, '[链接文字](https://)', 1)
    // 插入后选中"链接文字"四个字, 直接输入即替换
    nextTick(() => {
      const ta = nativeTextarea()
      if (!ta) return
      const p = ta.selectionStart
      ta.setSelectionRange(p, p + 4)
    })
  }
}

/** 插入 2x2 表格模板 */
function insertTable() {
  insertAtCursor('\n| 列1 | 列2 |\n| --- | --- |\n| 内容 | 内容 |\n')
}
</script>

<style scoped>
.md-editor {
  width: 100%;
}

/* 工具栏: 浅灰底细边框, 与全站风格一致 */
.md-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 2px;
  padding: 4px 6px;
  background: var(--bg-soft);
  border: 1px solid var(--border);
  border-bottom: none;
  border-radius: 4px 4px 0 0;
}

.md-toolbar button {
  min-width: 26px;
  height: 24px;
  padding: 0 7px;
  border: 1px solid transparent;
  border-radius: 3px;
  background: transparent;
  color: var(--text-2);
  font-size: 12px;
  cursor: pointer;
  line-height: 1;
}

.md-toolbar button:hover {
  background: var(--brand-soft);
  border-color: var(--brand);
  color: var(--brand);
}

.md-toolbar .sep {
  width: 1px;
  height: 14px;
  background: var(--border);
  margin: 0 4px;
}

/* 文本域圆角与工具栏衔接 */
.md-textarea :deep(.el-textarea__inner) {
  border-radius: 0 0 4px 4px;
}
</style>
