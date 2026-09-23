<template>
  <div class="contest-detail">
    <div class="container">
      <el-page-header @back="router.push('/contests')" class="back" />

      <div v-loading="loading">
        <template v-if="contest">
          <!-- 头部信息卡 -->
          <div class="oj-card head-card">
            <div class="head-top">
              <h2 class="title">{{ contest.title }}</h2>
              <div class="tags">
                <span class="type-tag" :class="`type-${contest.type}`">{{ contest.type }}</span>
                <span class="status" :class="`status-${contest.status}`">{{ statusLabel(contest.status) }}</span>
                <span v-if="contest.hasPassword" class="lock-hint">
                  <el-icon :size="13"><Lock /></el-icon>密码保护
                </span>
              </div>
            </div>
            <div class="meta">
              <span>创建者 <b>{{ contest.creatorName }}</b></span>
              <span>开始 <b class="mono">{{ formatTime(contest.startTime) }}</b></span>
              <span>结束 <b class="mono">{{ formatTime(contest.endTime) }}</b></span>
              <span v-if="contest.status === 'RUNNING'" class="countdown mono">
                {{ countdownText }}
              </span>
            </div>
            <div
              v-if="contest.description"
              class="markdown-body desc"
              v-html="renderMarkdown(contest.description)"
            ></div>
            <!-- 管理区(创建者/管理端) -->
            <div v-if="contest.canManage" class="manage-bar">
              <el-button size="small" plain @click="openEdit">编辑比赛</el-button>
              <el-button v-if="contest.status === 'NOT_STARTED'" size="small" plain @click="openManageProblems">
                题目管理
              </el-button>
              <el-button size="small" plain type="danger" @click="handleDelete">删除比赛</el-button>
            </div>
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
                <el-table-column label="题号" width="90">
                  <template #default="{ row }">
                    <span class="display-id mono">{{ row.displayId }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="标题" min-width="300">
                  <template #default="{ row }">
                    <el-link
                      type="primary"
                      :underline="false"
                      @click="router.push(`/problems/${row.problemId}?contest=${contest.id}`)"
                    >
                      {{ row.title }}
                    </el-link>
                  </template>
                </el-table-column>
                <el-table-column label="难度" width="110" align="center">
                  <template #default="{ row }">
                    <span class="rating mono" :style="{ color: ratingColor(row.difficulty) }">{{ row.difficulty }}</span>
                  </template>
                </el-table-column>
              </el-table>
              <div v-if="!contest.problems?.length && !problemsLoading" class="empty-tip">暂无题目</div>
            </el-tab-pane>

            <el-tab-pane label="榜单" name="standings">
              <div v-if="standings?.hidden" class="empty-tip">
                <el-icon :size="22"><Hide /></el-icon>
                比赛进行中, 榜单将在比赛结束后公布
              </div>
              <div v-else>
                <div v-if="standings?.rows?.length" class="table-scroll">
                  <table class="standings-table">
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
                      <tr
                        v-for="row in standings.rows"
                        :key="row.userId"
                        :class="`rank-${row.rank}`"
                      >
                        <td><span class="rank-badge" :class="`medal-${row.rank}`">{{ row.rank }}</span></td>
                        <td class="user-cell">
                          <span class="mini-avatar">{{ (row.username || '?')[0].toUpperCase() }}</span>
                          {{ row.username }}
                        </td>
                        <template v-if="contest.type === 'ICPC'">
                          <td v-for="(st, i) in row.problemStates" :key="i" class="prob-col">
                            <span
                              v-if="st"
                              class="cell-state"
                              :class="st.startsWith('+') ? 'cell-ac' : 'cell-wa'"
                            >{{ st }}</span>
                          </td>
                          <td class="mono"><b>{{ row.solved }}</b></td>
                          <td class="mono">{{ row.penalty }}</td>
                        </template>
                        <template v-else>
                          <td v-for="(sc, i) in row.problemScores" :key="i" class="prob-col">
                            <span v-if="sc != null" class="mono score-cell" :class="{ full: sc >= 100 }">{{ sc }}</span>
                          </td>
                          <td class="total-cell mono"><b>{{ row.totalScore }}</b></td>
                        </template>
                      </tr>
                    </tbody>
                  </table>
                </div>
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
          <span class="letter mono">{{ toLetters(idx) }}</span>
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
import { Lock, Hide } from '@element-plus/icons-vue'
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

/* 头部信息卡 */
.head-card {
  padding: 20px 24px;
}

.head-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
}

.title {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.01em;
  margin: 0;
  word-break: break-word;
}

.tags {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.type-tag {
  padding: 1px 8px;
  border-radius: 4px;
  font-size: 11.5px;
  font-weight: 600;
  color: #fff;
  font-family: var(--font-mono);
}

.type-ICPC {
  background: var(--brand);
}

.type-OI {
  background: #5cb85c;
}

.type-IOI {
  background: #f0ad4e;
}

.status {
  font-size: 13px;
  font-weight: 600;
}

.status-gray {
  color: var(--text-3);
}

.status-green {
  color: var(--ok);
}

.status-red {
  color: var(--bad);
}

.lock-hint {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12.5px;
  color: var(--text-3);
}

.meta {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
  color: var(--text-2);
  font-size: 13px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border);
}

.meta b {
  color: var(--text);
  font-weight: 600;
}

.countdown {
  color: var(--bad);
  font-weight: 700;
}

.desc {
  margin-top: 12px;
  font-size: 14px;
}

.manage-bar {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid var(--border);
}

.password-gate {
  margin: 24px 0;
  display: flex;
  gap: 10px;
  align-items: center;
}

.tabs {
  margin-top: 16px;
}

.display-id {
  font-weight: 700;
  color: var(--brand);
}

.rating {
  font-weight: 700;
  font-size: 13px;
}

/* 榜单 */
.standings-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  font-size: 13px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  overflow: hidden;
}

.standings-table th {
  background: var(--bg-soft);
  border-bottom: 1px solid var(--border);
  padding: 9px 10px;
  font-weight: 600;
  text-align: center;
  color: var(--text-2);
  font-size: 12px;
  position: sticky;
  top: 0;
}

.standings-table td {
  border-bottom: 1px solid var(--border);
  padding: 8px 10px;
  text-align: center;
  color: var(--text);
}

.standings-table tbody tr:last-child td {
  border-bottom: none;
}

.standings-table tbody tr:hover td {
  background: var(--bg-hover);
}

/* 前三名奖牌底色(行) */
.standings-table tbody tr.rank-1 td {
  background: rgba(240, 196, 64, 0.1);
}

.standings-table tbody tr.rank-2 td {
  background: rgba(160, 170, 186, 0.12);
}

.standings-table tbody tr.rank-3 td {
  background: rgba(196, 128, 68, 0.1);
}

html.dark .standings-table tbody tr.rank-1 td {
  background: rgba(240, 196, 64, 0.08);
}

html.dark .standings-table tbody tr.rank-2 td {
  background: rgba(160, 170, 186, 0.07);
}

html.dark .standings-table tbody tr.rank-3 td {
  background: rgba(196, 128, 68, 0.08);
}

.standings-table tbody tr:hover td {
  background: var(--bg-hover);
}

.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 20px;
  padding: 0 6px;
  border-radius: 999px;
  font-family: var(--font-mono);
  font-size: 12px;
  font-weight: 700;
  background: var(--bg-soft);
  color: var(--text-2);
}

.medal-1 {
  background: #f0c440;
  color: #5b4500;
}

.medal-2 {
  background: #b8c2d0;
  color: #3d4a5c;
}

.medal-3 {
  background: #d8a06a;
  color: #5e3a15;
}

.user-cell {
  text-align: left !important;
}

.user-cell .mini-avatar {
  display: inline-flex;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--brand-soft);
  color: var(--brand);
  font-size: 11px;
  font-weight: 700;
  align-items: center;
  justify-content: center;
  margin-right: 7px;
  vertical-align: middle;
}

.prob-col {
  width: 64px;
}

.cell-state {
  font-family: var(--font-mono);
  font-size: 12.5px;
  font-weight: 700;
  display: inline-block;
  min-width: 26px;
  padding: 1px 6px;
  border-radius: 4px;
}

.cell-ac {
  color: var(--ok);
  background: var(--ok-soft);
}

.cell-wa {
  color: var(--bad);
}

.score-cell {
  font-weight: 600;
}

.score-cell.full {
  color: var(--ok);
}

.total-cell {
  font-weight: 700;
  color: var(--brand);
}

/* 题目管理弹窗 */
.selected-list {
  width: 100%;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  margin-top: 10px;
}

.selected-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border-bottom: 1px solid var(--border);
}

.selected-row:last-child {
  border-bottom: none;
}

.letter {
  font-weight: 700;
  color: var(--brand);
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
