import request from './request'

/** 比赛访问 token 放入 X-Contest-Token 请求头 */
function withToken(token) {
  return token ? { headers: { 'X-Contest-Token': token } } : {}
}

/**
 * 创建比赛
 * @param {Object} data { title, description, type, startTime, endTime, password, problemIds }
 * @returns 比赛ID
 */
export function createContest(data) {
  return request.post('/contest', data)
}

/**
 * 比赛分页列表
 * @param {Object} params { pageNum, pageSize }
 */
export function getContestPage(params) {
  return request.get('/contest/page', { params })
}

/**
 * 比赛详情(基本信息无需 token)
 */
export function getContestDetail(id, contestToken) {
  return request.get(`/contest/${id}`, withToken(contestToken))
}

/**
 * 加入比赛(密码制需密码), 返回 { contestId, token }
 */
export function joinContest(id, data) {
  return request.post(`/contest/${id}/join`, data)
}

/**
 * 编辑比赛(创建者或管理端)
 */
export function updateContest(id, data) {
  return request.put(`/contest/${id}`, data)
}

/**
 * 更新比赛题目列表(仅未开赛)
 */
export function updateContestProblems(id, data) {
  return request.put(`/contest/${id}/problems`, data)
}

/**
 * 删除比赛
 */
export function deleteContest(id) {
  return request.delete(`/contest/${id}`)
}

/**
 * 比赛题目列表(需比赛访问 token)
 */
export function getContestProblems(id, contestToken) {
  return request.get(`/contest/${id}/problems`, withToken(contestToken))
}

/**
 * 榜单(需比赛访问 token; OI 赛期非创建者/管理端 hidden=true)
 */
export function getStandings(id, contestToken) {
  return request.get(`/contest/${id}/standings`, withToken(contestToken))
}
