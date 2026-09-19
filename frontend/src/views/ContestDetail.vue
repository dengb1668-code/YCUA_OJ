<template>
  <div class="contest-detail">
    <div class="container">
      <el-page-header @back="router.push('/contests')" class="back" />

      <div v-loading="loading">
        <template v-if="contest">
          <!-- 信息条 -->
          <div class="head">
            <h2 class="title">{{ contest.title }}</h2>
            <div class="tags">
              <span class="type-tag" :class="`type-${contest.type}`">{{ contest.type }}</span>
              <span class="status" :class="`status-${contest.status}`">{{ statusLabel(contest.status) }}</span>
              <span v-if="contest.hasPassword" class="muted">密码保护</span>
            </div>
            <div class="meta">
              <span>创建者: {{ contest.creatorName }}</span>
              <span>开始: {{ formatTime(contest.startTime) }}</span>
              <span>结束: {{ formatTime(contest.endTime) }}</span>
              <span v-if="contest.status === 'RUNNING'" class="countdown">
                {{ countdownText }}
              </span>
            </div>
            <div v-if="contest.description" class="markdown-body desc" v-html="renderMarkdown(contest.description)"></div>
          </div>

          <!-- 管理区(创建者/管理端) -->
          <div v-if="contest.canManage" class="manage-bar">
            <el-button size="small" plain @click="openEdit">编辑比赛</el-button>
            <el-button v-if="contest.status === 'NOT_STARTED'" size="small" plain @click="openManageProblems">
              题目管理
            </el-button>
            <el-button size="small" plain type="danger" @click="handleDelete">删除比赛</el-button>
          </div>

          <!-- 密码门 -->
          <div v-if="needPassword" class="password-gate">
            <el-input v-model="passwordInput" type="password" placeholder="请输入比赛密码" style="width: 260px" />
            <el-button type="primary" :loading="joining" @click="handleJoin">进入比赛</el-button>
          </div>

          <!-- 内容区 -->
          <el-tabs v-else v-model="activeTab" class="tabs">
            <el-tab-pane label="题目" name="problems">
              <el-table :data="contest.problems ?? []" v-loading="problemsLoading">
                <el-table-column label="题号" width="80">
                  <template #default="{ row }">
                    <span class="display-id">{{ row.displayId }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="标题" min-width="300">
                  <template #default="{ row }">
                    <el-link
                      type="primary"
                      @click="router.push(`/problems/${row.problemId}?contest=${contest.id}`)"
                    >
                      {{ row.title }}
                    </el-link>
                  </template>
                </el-table-column>
                <el-table-column label="难度" width="90" align="center">
                  <template #default="{ row }">
                    <span :style="{ color: ratingColor(row.difficulty) }">{{ row.difficulty }}</span>
                  </template>
                </el-table-column>
              </el-table>
              <div v-if="!contest.problems?.length && !problemsLoading" class="empty-tip">暂无题目</div>
            </el-tab-pane>

            <el-tab-pane label="榜单" name="standings">
              <div v-if="standings?.hidden" class="empty-tip">
                比赛进行中, 榜单将在比赛结束后公布
              </div>
              <div v-else>
                <table class="standings-table" v-if="standings?.rows?.length">
                  <thead>
                    <tr>
                      <th>#</th>
                      <th>用户</th>
                      <th v-for="p in standings.problems" :key="p.problemId" class="prob-col">{{ p.displayId }}</th>
                      <th v-if="contest.type === 'ICPC'">解题</th>
                      <th v-if="contest.type === 'ICPC'">罚时</th>
                      <th v-if="contest.type !== 'ICPC'">总分</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="row in standings.rows" :key="row.userId">
                      <td>{{ row.rank }}</td>
                      <td class="user-cell">{{ row.username }}</td>
                      <template v-if="contest.type === 'ICPC'">
                        <td v-for="(st, i) in row.problemStates" :key="i" class="prob-col">
                          <span v-if="st" :class="st.startsWith('+') ? 'cell-ac' : 'cell-wa'">{{ st }}</span>
                        </td>
                        <td>{{ row.solved }}</td>
                        <td>{{ row.penalty }}</td>
                      </template>
                      <template v-else>
                        <td v-for="(sc, i) in row.problemScores" :key="i" class="prob-col">
                          <span v-if="sc != null">{{ sc }}</span>
                        </td>
                        <td class="total-cell">{{ row.totalScore }}</td>
                      </template>
                    </tr>
                  </tbody>
                </table>
                <div v-else class="empty-tip">暂无提交</div>
              </div>
            </el-tab-pane>

            <el-tab-pane label="提交记录" name="submissions">
              <SubmissionListPanel mode="contest" :contest-id="contest.id" :contest-token="contestToken" />
            </el-tab-pane>
          </el-tabs>
        </template>
      </div>
    </div>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editVisible" title="编辑比赛" width="640px" align-center>
      <el-form label-width="100px">
        <el-form-item label="标题">
          <el-input v-model="editForm.title" maxlength="100" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="editForm.description" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker
            v-model="editForm.endTime"
            type="datetime"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="结束时间"
          />
        </el-form-item>
        <el-form-item label="参赛密码">
          <el-input v-model="editForm.password" placeholder="留空=公开, 输入新密码=更换" style="width: 260px" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 题目管理弹窗 -->
    <el-dialog v-model="manageVisible" title="题目管理(顺序即题号 A/B/C...)" width="640px" align-center>
      <el-select
        v-model="manageProblemIds"
        multiple
        filterable
        remote
        reserve-keyword
        :remote-method="searchProblems"
        :loading="searching"
        placeholder="搜索并选择题目"
        style="width: 100%"
      >
        <el-option v-for="p in searchOptions" :key="p.id" :label="`#${p.id} ${p.title}`" :value="p.id" />
      </el-select>
      <div v-if="manageProblemIds.length" class="selected-list">
        <div v-for="(pid, idx) in manageProblemIds" :key="pid" class="selected-row">
          <span class="letter">{{ toLetters(idx) }}</span>
          <span class="ptitle">{{ problemTitle(pid) }}</span>
          <span class="ops">
            <el-button size="small" text :disabled="idx === 0" @click="moveManage(idx, -1)">上移</el-button>
            <el-button
              size="small"
              text
              :disabled="idx === manageProblemIds.length - 1"
              @click="moveManage(idx, 1)"
            >
              下移
            </el-button>
            <el-button size="small" text type="danger" @click="manageProblemIds.splice(idx, 1)">移除</el-button>
          </span>
        </div>
      </div>
      <template #footer>
        <el-button @click="manageVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingProblems" @click="handleSaveProblems">保存题目</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  deleteContest,
  getContestDetail,
  getStandings,
  joinContest,
  updateContest,
  updateContestProblems
} from '../api/contest'
import { getProblemPage } from '../api/problem'
import { getContestToken, setContestToken, clearContestToken } from '../utils/contestToken'
import { formatTime } from '../utils/verdict'
import { renderMarkdown } from '../utils/markdown'
import { ratingColor } from '../utils/rating'
import SubmissionListPanel from '../components/SubmissionListPanel.vue'

const route = useRoute()
const router = useRouter()
const contestId = Number(route.params.id)

const loading = ref(false)
const contest = ref(null)
const contestToken = ref('')
const activeTab = ref('problems')

// 密码门
const needPassword = computed(() =>
  contest.value?.hasPassword && !contest.value?.canEnter && !contest.value?.canManage
)
const passwordInput = ref('')
const joining = ref(false)

// 题目列表加载态
const problemsLoading = ref(false)

// 倒计时
const now = ref(Date.now())
let timer = null

const countdownText = computed(() => {
  if (!contest.value) return ''
  const end = new Date(String(contest.value.endTime).replace(' ', 'T')).getTime()
  const diff = end - now.value
  if (diff <= 0) return '已结束'
  const h = Math.floor(diff / 3600000)
  const m = Math.floor((diff % 3600000) / 60000)
  const s = Math.floor((diff % 60000) / 1000)
  return `距结束 ${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

// 榜单
const standings = ref(null)
let standingsTimer = null

// 编辑
const editVisible = ref(false)
const saving = ref(false)
const editForm = reactive({ title: '', description: '', endTime: '', password: '' })

// 题目管理
const manageVisible = ref(false)
const manageProblemIds = ref([])
const savingProblems = ref(false)
const searching = ref(false)
const searchOptions = ref([])
const problemTitleMap = new Map()

const statusMap = {
  NOT_STARTED: { label: '未开始', cls: 'gray' },
  RUNNING: { label: '进行中', cls: 'green' },
  ENDED: { label: '已结束', cls: 'red' }
}

function statusLabel(status) {
  return statusMap[status]?.label ?? status
}

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

function moveManage(idx, delta) {
  const arr = manageProblemIds.value
  const target = idx + delta
  if (target < 0 || target >= arr.length) return
  const tmp = arr[idx]
  arr[idx] = arr[target]
  arr[target] = tmp
}

async function fetchDetail() {
  loading.value = true
  try {
    contest.value = await getContestDetail(contestId, contestToken.value)
    // 公开比赛且未持有 token: 自动加入获取访问 token(榜单/提交需要)
    if (contest.value && !contest.value.hasPassword && !contest.value.canManage && !contestToken.value) {
      try {
        const data = await joinContest(contestId, {})
        contestToken.value = data.token
        setContestToken(contestId, data.token)
        contest.value = await getContestDetail(contestId, contestToken.value)
      } catch (e) {
        // 自动加入失败不阻断浏览
      }
    }
  } catch (e) {
  } finally {
    loading.value = false
  }
}

async function handleJoin() {
  if (!passwordInput.value) {
    ElMessage.warning('请输入比赛密码')
    return
  }
  joining.value = true
  try {
    const data = await joinContest(contestId, { password: passwordInput.value })
    contestToken.value = data.token
    setContestToken(contestId, data.token)
    ElMessage.success('已加入比赛')
    fetchDetail()
  } catch (e) {
  } finally {
    joining.value = false
  }
}

async function fetchStandings() {
  if (!contest.value?.canEnter && !contest.value?.canManage) return
  try {
    standings.value = await getStandings(contestId, contestToken.value)
  } catch (e) {
  }
}

function startStandingsPolling() {
  if (standingsTimer) return
  standingsTimer = setInterval(() => {
    if (contest.value?.status === 'RUNNING' || contest.value?.status === 'NOT_STARTED') {
      fetchStandings()
    }
  }, 30000)
}

function openEdit() {
  editForm.title = contest.value.title
  editForm.description = contest.value.description ?? ''
  editForm.endTime = String(contest.value.endTime).slice(0, 19)
  editForm.password = ''
  editVisible.value = true
}

async function handleSave() {
  if (!editForm.title.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  saving.value = true
  try {
    await updateContest(contestId, {
      title: editForm.title.trim(),
      description: editForm.description,
      endTime: editForm.endTime,
      password: editForm.password !== '' ? editForm.password : null
    })
    ElMessage.success('已保存')
    editVisible.value = false
    fetchDetail()
  } catch (e) {
  } finally {
    saving.value = false
  }
}

async function openManageProblems() {
  manageProblemIds.value = (contest.value.problems ?? []).map((p) => p.problemId)
  manageVisible.value = true
  searchProblems('')
}

async function handleSaveProblems() {
  savingProblems.value = true
  try {
    await updateContestProblems(contestId, { problemIds: manageProblemIds.value })
    ElMessage.success('题目已更新')
    manageVisible.value = false
    fetchDetail()
  } catch (e) {
  } finally {
    savingProblems.value = false
  }
}

async function handleDelete() {
  try {
    await ElMessageBox.confirm('删除比赛后不可恢复(提交记录保留), 确定删除?', '删除比赛', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch (e) {
    return
  }
  try {
    await deleteContest(contestId)
    ElMessage.success('已删除')
    clearContestToken(contestId)
    router.push('/contests')
  } catch (e) {
  }
}

// 切到榜单 tab 时加载榜单并开始轮询
watch(activeTab, (tab) => {
  if (tab === 'standings') {
    fetchStandings()
    startStandingsPolling()
  }
})

onMounted(() => {
  contestToken.value = getContestToken(contestId)
  fetchDetail()
  timer = setInterval(() => {
    now.value = Date.now()
  }, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  if (standingsTimer) clearInterval(standingsTimer)
})
</script>

<style scoped>
.contest-detail {
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

.title {
  font-size: 22px;
  font-weight: normal;
  margin: 8px 0 10px;
}

.tags {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.type-tag {
  padding: 2px 8px;
  border-radius: 3px;
  font-size: 12px;
  color: #fff;
}

.type-ICPC {
  background: #1a5cc8;
}

.type-OI {
  background: #5cb85c;
}

.type-IOI {
  background: #f0ad4e;
}

.status {
  font-size: 13px;
}

.status-gray {
  color: #999;
}

.status-green {
  color: #5cb85c;
  font-weight: 600;
}

.status-red {
  color: #d9534f;
}

.meta {
  display: flex;
  gap: 18px;
  color: #666;
  font-size: 13px;
  border-bottom: 1px solid #eee;
  padding-bottom: 12px;
}

.countdown {
  color: #d9534f;
  font-weight: 600;
}

.desc {
  margin-top: 12px;
  font-size: 14px;
}

.manage-bar {
  margin: 14px 0;
}

.password-gate {
  margin: 24px 0;
  display: flex;
  gap: 10px;
  align-items: center;
}

.tabs {
  margin-top: 12px;
}

.display-id {
  font-weight: 600;
  color: #1a5cc8;
}

.empty-tip {
  color: #999;
  text-align: center;
  padding: 40px 0;
  font-size: 14px;
}

/* 榜单 */
.standings-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.standings-table th {
  background: #eee;
  border: 1px solid #ddd;
  padding: 6px 10px;
  font-weight: 600;
  text-align: center;
}

.standings-table td {
  border: 1px solid #ddd;
  padding: 6px 10px;
  text-align: center;
}

.standings-table .user-cell {
  text-align: left;
}

.standings-table .prob-col {
  width: 56px;
}

.cell-ac {
  color: #5cb85c;
  font-weight: 600;
}

.cell-wa {
  color: #d9534f;
}

.total-cell {
  font-weight: 600;
  color: #1a5cc8;
}

/* 题目管理弹窗 */
.selected-list {
  width: 100%;
  border: 1px solid #ddd;
  margin-top: 10px;
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

.ptitle {
  flex: 1;
}

.ops {
  display: flex;
  gap: 4px;
}
</style>
