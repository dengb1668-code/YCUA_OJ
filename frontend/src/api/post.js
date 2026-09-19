import request from './request'

/**
 * 发帖(讨论/题解)
 * @param {Object} data { type: 'DISCUSSION'|'SOLUTION', problemId, title, content }
 * @returns 新帖子ID
 */
export function createPost(data) {
  return request.post('/post', data)
}

/**
 * 帖子分页
 * @param {Object} params { type, problemId, pageNum, pageSize }
 */
export function getPostPage(params) {
  return request.get('/post/page', { params })
}

/**
 * 帖子详情(含全部回复与当前用户权限标志)
 */
export function getPostDetail(id) {
  return request.get(`/post/${id}`)
}

/**
 * 编辑帖子(作者本人或管理端)
 */
export function updatePost(id, data) {
  return request.put(`/post/${id}`, data)
}

/**
 * 删除帖子(作者本人或管理端, 级联删除回复)
 */
export function deletePost(id) {
  return request.delete(`/post/${id}`)
}

/**
 * 回复帖子
 * @param {Number} id 帖子ID
 * @param {Object} data { content }
 */
export function addReply(id, data) {
  return request.post(`/post/${id}/reply`, data)
}

/**
 * 删除回复(作者本人或管理端)
 */
export function deleteReply(id) {
  return request.delete(`/reply/${id}`)
}
