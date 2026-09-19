import { reactive } from 'vue'

const TOKEN_KEY = 'oj_token'
const USERNAME_KEY = 'oj_username'
const USERID_KEY = 'oj_userId'
const ROLE_KEY = 'oj_role'

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

  setAuth({ token, userId, username, role }) {
    this.token = token
    this.userId = userId
    this.username = username
    this.role = role || 'USER'
    localStorage.setItem(TOKEN_KEY, token)
    localStorage.setItem(USERID_KEY, String(userId))
    localStorage.setItem(USERNAME_KEY, username)
    localStorage.setItem(ROLE_KEY, this.role)
  },

  logout() {
    this.token = ''
    this.userId = null
    this.username = ''
    this.role = 'USER'
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USERID_KEY)
    localStorage.removeItem(USERNAME_KEY)
    localStorage.removeItem(ROLE_KEY)
  }
})
