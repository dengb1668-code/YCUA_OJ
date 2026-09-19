<template>
  <div class="login-page">
    <VantaBackground bg-color="#ffffff" />
    <div class="school-line">YiChun University Algorithm Team</div>

    <!-- HDOJ 风格登录框: 蓝边框 + 蓝条标题 + 白底表单 -->
    <div class="login-frame">
      <div class="frame-title">Online Judge 登录</div>
      <div class="frame-body">
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
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCaptcha, login } from '../api/user'
import { userStore } from '../store/user'
import VantaBackground from '../components/VantaBackground.vue'

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
/* HDOJ(杭电 OJ)风格登录页: 飞鸟特效背景 + 蓝边框卡片 */
.login-page {
  background: transparent;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 30px 16px 60px;
  min-height: calc(100vh - 187px);
  box-sizing: border-box;
}

.school-line {
  color: #555;
  font-size: 15px;
  letter-spacing: 3px;
  margin-bottom: 18px;
}

/* HDOJ 登录框: 1px 蓝边框 + 蓝条标题 */
.login-frame {
  width: 420px;
  max-width: 92vw;
  background: #fff;
  border: 1px solid #1a5cc8;
  box-sizing: border-box;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.login-frame:hover, .register-frame:hover, .forgot-frame:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 30px rgba(26, 92, 200, 0.15);
}

.frame-title {
  background: #1a5cc8;
  color: #fff;
  font-weight: bold;
  font-size: 15px;
  padding: 9px 16px;
}

.frame-body {
  padding: 22px 26px 16px;
}

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
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  background: #fafafa;
}

.switch-line {
  margin-top: 16px;
  text-align: center;
  color: #666;
  font-size: 13px;
}

.switch-link {
  color: #1a5cc8;
  margin-left: 2px;
}

.divider {
  margin: 0 10px;
  color: #ccc;
}
</style>
