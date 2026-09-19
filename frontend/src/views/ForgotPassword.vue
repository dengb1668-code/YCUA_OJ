<template>
  <div class="forgot-page">
    <!-- 队名(学校名已在全局横幅中) -->
    <div class="school-line">YiChun University Algorithm Team</div>

    <!-- HDOJ 风格卡片: 蓝边框 + 蓝条标题 + 白底表单 -->
    <div class="forgot-frame">
      <div class="frame-title">Online Judge 找回密码</div>
      <div class="frame-body">
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
    // request.js 拦截器已统一弹出错误提示
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
/* HDOJ(杭电 OJ)风格: 与登录/注册页一致 */
.forgot-page {
  background: #fff;
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

.forgot-frame {
  width: 420px;
  max-width: 92vw;
  background: #fff;
  border: 1px solid #1a5cc8;
  box-sizing: border-box;
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
</style>
