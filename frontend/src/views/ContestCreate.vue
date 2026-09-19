<template>
  <div class="contest-create">
    <div class="container">
      <el-page-header @back="router.back()" class="back" />
      <h2>创建比赛</h2>

      <el-form label-width="90px" class="form">
        <el-form-item label="标题">
          <el-input v-model="form.title" maxlength="100" placeholder="比赛标题" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            placeholder="比赛说明(支持 Markdown, 可选)"
          />
        </el-form-item>
        <el-form-item label="赛制">
          <el-radio-group v-model="form.type">
            <el-radio value="ICPC">ICPC(按解题数+罚时排名)</el-radio>
            <el-radio value="OI">OI(按总分排名, 赛期隐藏榜单)</el-radio>
            <el-radio value="IOI">IOI(按总分排名, 实时榜单)</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="时间">
          <el-date-picker
            v-model="timeRange"
            type="datetimerange"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="参赛密码">
          <el-input
            v-model="form.password"
            placeholder="留空为公开比赛"
            show-password
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item label="题目">
          <el-select
            v-model="selectedProblemIds"
            multiple
            filterable
            remote
            reserve-keyword
            :remote-method="searchProblems"
            :loading="searching"
            placeholder="搜索并选择题目(选择顺序即比赛题号 A/B/C...)"
            style="width: 100%"
          >
            <el-option
              v-for="p in searchOptions"
              :key="p.id"
              :label="`#${p.id} ${p.title}`"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="selectedProblemIds.length" label="题目顺序">
          <div class="selected-list">
            <div v-for="(pid, idx) in selectedProblemIds" :key="pid" class="selected-row">
              <span class="letter">{{ letters[idx] }}</span>
              <span class="title">{{ problemTitle(pid) }}</span>
              <span class="ops">
                <el-button size="small" text :disabled="idx === 0" @click="move(idx, -1)">上移</el-button>
                <el-button
                  size="small"
                  text
                  :disabled="idx === selectedProblemIds.length - 1"
                  @click="move(idx, 1)"
                >
                  下移
                </el-button>
                <el-button size="small" text type="danger" @click="removeProblem(idx)">移除</el-button>
              </span>
            </div>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="creating" @click="handleCreate">创建比赛</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createContest } from '../api/contest'
import { getProblemPage } from '../api/problem'

const router = useRouter()

const form = reactive({ title: '', description: '', type: 'ICPC', password: '' })
const timeRange = ref(null)

// 远程搜索选题
const searching = ref(false)
const searchOptions = ref([])
const selectedProblemIds = ref([])
const problemTitleMap = new Map()

const letters = computed(() => selectedProblemIds.value.map((_, i) => toLetters(i)))

function toLetters(i) {
  let s = ''
  let n = i
  while (true) {
    s = String.fromCharCode(65 + (n % 26)) + s
    n = Math.floor(n / 26) - 1
    if (n < 0) break
  }
  return s
}

async function searchProblems(keyword) {
  searching.value = true
  try {
    const data = await getProblemPage({ pageNum: 1, pageSize: 20, keyword: keyword || '' })
    searchOptions.value = data.records
    data.records.forEach((p) => problemTitleMap.set(p.id, p.title))
  } catch (e) {
  } finally {
    searching.value = false
  }
}

function problemTitle(pid) {
  return problemTitleMap.get(pid) ?? `#${pid}`
}

function move(idx, delta) {
  const arr = selectedProblemIds.value
  const target = idx + delta
  if (target < 0 || target >= arr.length) return
  const tmp = arr[idx]
  arr[idx] = arr[target]
  arr[target] = tmp
}

function removeProblem(idx) {
  selectedProblemIds.value.splice(idx, 1)
}

async function handleCreate() {
  if (!form.title.trim()) {
    ElMessage.warning('请输入比赛标题')
    return
  }
  if (!timeRange.value || timeRange.value.length !== 2) {
    ElMessage.warning('请选择开始与结束时间')
    return
  }
  if (new Date(timeRange.value[1]) <= new Date(timeRange.value[0])) {
    ElMessage.warning('结束时间必须晚于开始时间')
    return
  }
  creating.value = true
  try {
    const id = await createContest({
      title: form.title.trim(),
      description: form.description,
      type: form.type,
      startTime: timeRange.value[0],
      endTime: timeRange.value[1],
      password: form.password || null,
      problemIds: selectedProblemIds.value
    })
    ElMessage.success('比赛创建成功')
    router.push(`/contests/${id}`)
  } catch (e) {
  } finally {
    creating.value = false
  }
}

// 初始加载一次题目列表, 方便直接选择
searchProblems('')
</script>

<style scoped>
.contest-create {
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

h2 {
  font-size: 20px;
  font-weight: normal;
  margin: 0 0 20px;
}

.form {
  max-width: 720px;
}

.selected-list {
  width: 100%;
  border: 1px solid #ddd;
}

.selected-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 12px;
  border-bottom: 1px solid #eee;
}

.selected-row:last-child {
  border-bottom: none;
}

.letter {
  font-weight: 600;
  color: #1a5cc8;
  min-width: 24px;
}

.title {
  flex: 1;
}

.ops {
  display: flex;
  gap: 4px;
}
</style>
