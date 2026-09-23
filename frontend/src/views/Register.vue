<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-logo">
        <span class="logo-mark">&lt;/&gt;</span>
        <span class="logo-text">YCUAoj</span>
      </div>
      <h2 class="auth-title">注册</h2>
      <p class="auth-subtitle">创建账号, 开始你的算法之路</p>
      <el-form @keyup.enter="handleRegister">
        <el-form-item>
          <el-input v-model="registerForm.username" placeholder="用户名(3-20位)" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="密码(至少6位)"
            size="large"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="确认密码"
            size="large"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-input v-model="registerForm.phone" placeholder="手机号(找回密码用)" size="large" />
        </el-form-item>
        <el-button
          type="primary"
          size="large"
          class="submit-btn"
          :loading="loading"
          @click="handleRegister"
        >
          注册
        </el-button>
      </el-form>

      <div class="switch-line">
        已有账号?
        <router-link to="/login" class="switch-link">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '../api/user'
import { userStore } from '../store/user'

const router = useRouter()
const loading = ref(false)

const registerForm = reactive({ username: '', password: '', confirmPassword: '', phone: '' })

async function handleRegister() {
  if (!registerForm.username || !registerForm.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  if (registerForm.username.length < 3 || registerForm.username.length > 20) {
    ElMessage.warning('用户名长度需在 3-20 之间')
    return
  }
  if (registerForm.password.length < 6) {
    ElMessage.warning('密码至少 6 位')
    return
  }
  if (registerForm.password !== registerForm.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  if (!/^1[3-9]\d{9}$/.test(registerForm.phone)) {
    ElMessage.warning('请输入正确的 11 位手机号')
    return
  }
  loading.value = true
  try {
    const data = await register({
      username: registerForm.username,
      password: registerForm.password,
      phone: registerForm.phone
    })
    userStore.setAuth(data)
    ElMessage.success('注册成功, 已自动登录!')
    router.push('/problems')
  } catch (e) {
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.submit-btn {
  width: 100%;
  margin-top: 4px;
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
</style>
