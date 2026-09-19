import request from './request'

/**
 * 提交代码
 * @param {Object} data { problemId, language, code, userId }
 * @returns 提交记录ID
 */
export function submitCode(data) {
  return request.post('/submission', data)
}

/**
 * 查询提交记录(提交后轮询判题结果)
 */
export function getSubmission(id) {
  return request.get(`/submission/${id}`)
}

/**
 * 分页获取当前用户的提交记录
 * @param {Object} params { pageNum, pageSize }
 */
export function getMySubmissions(params) {
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
