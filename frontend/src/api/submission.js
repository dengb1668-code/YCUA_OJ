import request from './request'

/**
 * 提交代码
 * @param {Object} data { problemId, contestId, language, code }
 * @param {String} contestToken 比赛内提交必传(X-Contest-Token 头)
 * @returns 提交记录ID
 */
export function submitCode(data, contestToken) {
  if (contestToken) {
    return request.post('/submission', data, { headers: { 'X-Contest-Token': contestToken } })
  }
  return request.post('/submission', data)
}

/**
 * 查询提交记录(提交后轮询判题结果; 无代码查看权时 code 为 null)
 */
export function getSubmission(id) {
  return request.get(`/submission/${id}`)
}

/**
 * 分页获取提交记录
 * @param {Object} params { scope: 'mine'|'all', problemId, status, pageNum, pageSize }
 */
export function getSubmissions(params) {
  return request.get('/submission/page', { params })
}

/**
 * 当前用户的做题统计(总提交/通过情况/近30天每日提交数)
 */
export function getMyStats() {
  return request.get('/submission/stats')
}

/**
 * 自定义测试: 用给定输入在本机真实运行代码
 * @param {Object} data { language, code, input }
 */
export function customTest(data) {
  return request.post('/submission/custom-test', data)
}
