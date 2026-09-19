import request from './request'

/**
 * 获取登录图片验证码
 * @returns {Object} { captchaId, image(base64 data URL) }
 */
export function getCaptcha() {
  return request.get('/captcha')
}

/**
 * 注册(成功后自动登录, 返回 token + 用户信息)
 * @param {Object} data { username, password, phone }
 */
export function register(data) {
  return request.post('/user/register', data)
}

/**
 * 登录
 * @param {Object} data { username, password, captchaId, captchaCode }
 */
export function login(data) {
  return request.post('/user/login', data)
}

/**
 * 找回密码(用户名 + 手机号 + 图形验证码)
 * @param {Object} data { username, phone, captchaId, captchaCode, newPassword }
 */
export function forgotPassword(data) {
  return request.post('/user/forgot-password', data)
}

/**
 * 用户管理分页(仅站长)
 * @param {Object} params { keyword, pageNum, pageSize }
 */
export function getUserPage(params) {
  return request.get('/user/page', { params })
}

/**
 * 修改用户角色(仅站长)
 * @param {Number} id 用户ID
 * @param {Object} data { role: 0|1 }
 */
export function updateUserRole(id, data) {
  return request.put(`/user/${id}/role`, data)
}
