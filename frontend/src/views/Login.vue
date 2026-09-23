<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-logo">
        <span class="logo-mark">&lt;/&gt;</span>
        <span class="logo-text">YCUAoj</span>
      </div>
      <h2 class="auth-title">登录</h2>
      <p class="auth-subtitle">欢迎回来, 继续刷题之旅</p>
      <el-form @keyup.enter="handleLogin">
        <el-form-item>
          <el-input v-model="loginForm.username" placeholder="用户名" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="密码"
            size="large"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <div class="captcha-row">
            <el-input
              v-model="loginForm.captchaCode"
              placeholder="验证码"
              size="large"
              class="captcha-input"
            />
            <img
              v-if="captchaImage"
              :src="captchaImage"
              class="captcha-img"
              title="点击刷新验证码"
              alt="验证码"
              @click="refreshCaptcha"
            />
          </div>
        </el-form-item>
        <el-button
          type="primary"
          size="large"
          class="submit-btn"
          :loading="loading"
          @click="handleLogin"
        >
          登录
        </el-button>
      </el-form>

      <div class="switch-line">
        还没有账号?
        <router-link to="/register" class="switch-link">立即注册</router-link>
        <span class="divider">|</span>
        <router-link to="/forgot" class="switch-link">忘记密码</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCaptcha, login } from '../api/user'
import { userStore } from '../store/user'

const router = useRouter()
const loading = ref(false)

const loginForm = reactive({ username: '', password: '', captchaId: '', captchaCode: '' })
const captchaImage = ref('')

async function refreshCaptcha() {
  try {
    const data = await getCaptcha()
    loginForm.captchaId = data.captchaId
    captchaImage.value = data.image
    loginForm.captchaCode = ''
  } catch (e) {
  }
}

async function handleLogin() {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  if (!loginForm.captchaCode) {
    ElMessage.warning('请输入验证码')
    return
  }
  loading.value = true
  try {
    const data = await login({
      username: loginForm.username,
      password: loginForm.password,
      captchaId: loginForm.captchaId,
      captchaCode: loginForm.captchaCode
    })
    userStore.setAuth(data)
    ElMessage.success(`欢迎回来, ${data.username}!`)
    router.push('/problems')
  } catch (e) {
    // 登录失败刷新验证码(一次性, 已用掉的码不能再提交)
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(refreshCaptcha)
</script>

<style scoped>
.submit-btn {
  width: 100%;
  margin-top: 4px;
}

/* 验证码: 输入框 + 可点击刷新的图片 */
.captcha-row {
  display: flex;
  gap: 10px;
  width: 100%;
}

.captcha-input {
  flex: 1;
}

.captcha-img {
  height: 40px;
  width: 130px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  cursor: pointer;
  background: var(--bg-soft);
  flex-shrink: 0;
}

.switch-line {
  margin-top: 16px;
  text-align: center;
  color: var(--text-2);
  font-size: 13px;
}

.switch-link {
  color: var(--brand);
  margin-left: 2px;
  font-weight: 600;
}

.divider {
  margin: 0 10px;
  color: var(--border-strong);
}
</style>
