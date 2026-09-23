<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-logo">
        <span class="logo-mark">&lt;/&gt;</span>
        <span class="logo-text">YCUAoj</span>
      </div>
      <h2 class="auth-title">找回密码</h2>
      <p class="auth-subtitle">验证身份后重置密码</p>
      <el-form @keyup.enter="handleSubmit">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.phone" placeholder="注册时填写的手机号" size="large" />
        </el-form-item>
        <el-form-item>
          <div class="captcha-row">
            <el-input
              v-model="form.captchaCode"
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
        <el-form-item>
          <el-input
            v-model="form.newPassword"
            type="password"
            placeholder="新密码(至少6位)"
            size="large"
            show-password
          />
        </el-form-item>
        <el-button
          type="primary"
          size="large"
          class="submit-btn"
          :loading="loading"
          @click="handleSubmit"
        >
          重置密码
        </el-button>
      </el-form>

      <div class="switch-line">
        想起密码了?
        <router-link to="/login" class="switch-link">返回登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { forgotPassword, getCaptcha } from '../api/user'

const router = useRouter()
const loading = ref(false)

const form = reactive({
  username: '',
  phone: '',
  captchaId: '',
  captchaCode: '',
  newPassword: ''
})
const captchaImage = ref('')

async function refreshCaptcha() {
  try {
    const data = await getCaptcha()
    form.captchaId = data.captchaId
    captchaImage.value = data.image
    form.captchaCode = ''
  } catch (e) {
  }
}

async function handleSubmit() {
  if (!form.username || !form.phone) {
    ElMessage.warning('请输入用户名和手机号')
    return
  }
  if (!/^1[3-9]\d{9}$/.test(form.phone)) {
    ElMessage.warning('请输入正确的 11 位手机号')
    return
  }
  if (!form.captchaCode) {
    ElMessage.warning('请输入验证码')
    return
  }
  if (form.newPassword.length < 6) {
    ElMessage.warning('新密码至少 6 位')
    return
  }
  loading.value = true
  try {
    await forgotPassword({
      username: form.username,
      phone: form.phone,
      captchaId: form.captchaId,
      captchaCode: form.captchaCode,
      newPassword: form.newPassword
    })
    ElMessage.success('密码重置成功, 请用新密码登录')
    router.push('/login')
  } catch (e) {
    // 校验失败刷新验证码(一次性, 已用掉的码不能再提交)
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
</style>
