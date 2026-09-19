import axios from 'axios'
import { ElMessage } from 'element-plus'
import { userStore } from '../store/user'

// 统一 axios 实例, baseURL 为 /api, 开发期由 vite proxy 转发到后端
// timeout 给足 30 秒: 自定义测试/判题需要本机编译, 慢机器上可能超过 10 秒
const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 请求拦截器: 自动携带登录令牌
request.interceptors.request.use((config) => {
  if (userStore.token) {
    config.headers.Authorization = `Bearer ${userStore.token}`
  }
  return config
})

// 响应拦截器: 拆包后端统一响应 Result{code, message, data}
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 0) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    // 直接返回 data, 组件里拿到的就是业务数据
    return res.data
  },
  (error) => {
    if (error.response?.status === 401) {
      // 登录过期: 清空登录态并回登录页
      userStore.logout()
      ElMessage.error('未登录或登录已过期, 请重新登录')
      window.location.href = '/login'
    } else {
      ElMessage.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
