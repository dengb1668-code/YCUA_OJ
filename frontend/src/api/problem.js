import request from './request'

/**
 * 分页获取题目列表(含当前用户做题状态)
 * @param {Object} params { pageNum, pageSize, userId }
 */
export function getProblemPage(params) {
  return request.get('/problem/page', { params })
}

/**
 * 获取单道题目详情
 */
export function getProblemDetail(id) {
  return request.get(`/problem/${id}`)
}

/**
 * 创建题目
 * @param {Object} data { title, source, description, inputDescription, outputDescription, samples, timeLimit, memoryLimit, difficulty }
 * @returns 新题目ID
 */
export function createProblem(data) {
  return request.post('/problem', data)
}

/**
 * 编辑题目题面(仅创建者/管理员)
 */
export function updateProblem(id, data) {
  return request.put(`/problem/${id}`, data)
}

/**
 * 删除题目(创建者/管理端; 被比赛引用的题目不可删)
 */
export function deleteProblem(id) {
  return request.delete(`/problem/${id}`)
}

/**
 * 测试点列表(仅元信息)
 */
export function getTestCases(problemId) {
  return request.get(`/problem/${problemId}/testcases`)
}

/**
 * 单个测试点内容(编辑表单回填)
 */
export function getTestCase(problemId, index) {
  return request.get(`/problem/${problemId}/testcases/${index}`)
}

/**
 * 追加测试点, 返回新序号
 */
export function addTestCase(problemId, data) {
  return request.post(`/problem/${problemId}/testcases`, data)
}

/**
 * 覆盖更新测试点
 */
export function updateTestCase(problemId, index, data) {
  return request.put(`/problem/${problemId}/testcases/${index}`, data)
}

/**
 * 行内更新测试点计分配置(分值/独立时限/内存)
 */
export function updateTestCaseConfig(problemId, index, data) {
  return request.put(`/problem/${problemId}/testcases/${index}/config`, data)
}

/**
 * 删除测试点
 */
export function deleteTestCase(problemId, index) {
  return request.delete(`/problem/${problemId}/testcases/${index}`)
}

/**
 * 上传 zip 替换全部测试点, 返回导入数量
 * 注意: 不要手动设置 Content-Type, axios 会自动带 multipart boundary
 */
export function uploadTestCases(problemId, formData) {
  return request.post(`/problem/${problemId}/testcases/upload`, formData, { timeout: 60000 })
}
