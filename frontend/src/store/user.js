import { reactive } from 'vue'

const TOKEN_KEY = 'oj_token'
const USERNAME_KEY = 'oj_username'
const USERID_KEY = 'oj_userId'
const ROLE_KEY = 'oj_role'
const CERT_KEY = 'oj_certStatus'

/**
 * 登录状态(简单响应式 store, 持久化到 localStorage)
 * 后续如引入 Pinia 可平滑替换
 */
export const userStore = reactive({
  token: localStorage.getItem(TOKEN_KEY) || '',
  username: localStorage.getItem(USERNAME_KEY) || '',
  userId: localStorage.getItem(USERID_KEY) || null,
  // 后端 UserRole: USER/ADMIN/OWNER; 旧登录数据无此键, 按 USER 处理
  role: localStorage.getItem(ROLE_KEY) || 'USER',
  // 学生认证状态: NONE/PENDING/APPROVED/REJECTED; 旧登录数据默认 NONE(重新登录/进入个人页会刷新)
  certStatus: localStorage.getItem(CERT_KEY) || 'NONE',

  setAuth({ token, userId, username, role, certStatus }) {
    this.token = token
    this.userId = userId
    this.username = username
    this.role = role || 'USER'
    this.certStatus = certStatus || 'NONE'
    localStorage.setItem(TOKEN_KEY, token)
    localStorage.setItem(USERID_KEY, String(userId))
    localStorage.setItem(USERNAME_KEY, username)
    localStorage.setItem(ROLE_KEY, this.role)
    localStorage.setItem(CERT_KEY, this.certStatus)
  },

  setCertStatus(status) {
    this.certStatus = status
    localStorage.setItem(CERT_KEY, status)
  },

  logout() {
    this.token = ''
    this.userId = null
    this.username = ''
    this.role = 'USER'
    this.certStatus = 'NONE'
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USERID_KEY)
    localStorage.removeItem(USERNAME_KEY)
    localStorage.removeItem(ROLE_KEY)
    localStorage.removeItem(CERT_KEY)
  }
})

/** 普通用户是否已完成学生认证(负责人/站长视为已认证) */
export function isCertified() {
  return userStore.role !== 'USER' || userStore.certStatus === 'APPROVED'
}
