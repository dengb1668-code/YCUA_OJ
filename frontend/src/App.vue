<template>
  <div class="app">
    <!-- 全局顶栏: 白底 + 细分割线 + 导航下划线激活态 -->
    <header class="oj-header">
      <div class="header-inner">
        <div class="header-left">
          <router-link to="/problems" class="logo">
            <span class="logo-mark">&lt;/&gt;</span>
            <span class="logo-text">YCUAoj</span>
          </router-link>
          <nav v-if="userStore.token" class="header-nav">
            <router-link to="/problems" class="nav-link">题库</router-link>
            <router-link to="/contests" class="nav-link">比赛</router-link>
            <router-link to="/submissions" class="nav-link">提交记录</router-link>
            <router-link to="/discussion" class="nav-link">讨论区</router-link>
            <router-link to="/profile" class="nav-link">个人主页</router-link>
          </nav>
        </div>
        <div class="header-right">
          <button
            class="icon-btn"
            :title="isDark ? '切换到亮色模式' : '切换到暗色模式'"
            aria-label="切换主题"
            @click="toggleTheme"
          >
            <el-icon :size="16"><Sunny v-if="isDark" /><Moon v-else /></el-icon>
          </button>
          <button
            v-if="userStore.token"
            class="icon-btn menu-btn"
            title="菜单"
            aria-label="菜单"
            @click="mobileOpen = !mobileOpen"
          >
            <el-icon :size="17"><Menu /></el-icon>
          </button>
          <el-dropdown v-if="userStore.token" trigger="click" @command="handleCommand">
            <span class="user-trigger">
              <span class="avatar">{{ (userStore.username || '?')[0].toUpperCase() }}</span>
              <span class="username">{{ userStore.username }}</span>
              <el-icon class="caret" :size="12"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人主页</el-dropdown-item>
                <el-dropdown-item command="submissions">提交记录</el-dropdown-item>
                <el-dropdown-item v-if="userStore.role === 'OWNER'" command="users" divided>
                  用户管理
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <!-- 移动端导航抽屉 -->
      <transition name="drop">
        <nav v-if="mobileOpen && userStore.token" class="mobile-nav">
          <router-link to="/problems" class="nav-link" @click="mobileOpen = false">题库</router-link>
          <router-link to="/contests" class="nav-link" @click="mobileOpen = false">比赛</router-link>
          <router-link to="/submissions" class="nav-link" @click="mobileOpen = false">提交记录</router-link>
          <router-link to="/discussion" class="nav-link" @click="mobileOpen = false">讨论区</router-link>
          <router-link to="/profile" class="nav-link" @click="mobileOpen = false">个人主页</router-link>
        </nav>
      </transition>
    </header>

    <router-view />

    <footer class="oj-footer">
      <div class="footer-links">
        <a v-for="oj in otherOjs" :key="oj.name" :href="oj.url" target="_blank" rel="noopener">
          {{ oj.name }}
        </a>
      </div>
      <div class="footer-copy">YCUAoj YCUer的算法练习平台</div>
    </footer>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Moon, Sunny, Menu, ArrowDown } from '@element-plus/icons-vue'
import { userStore } from './store/user'

const router = useRouter()

// 暗色模式: 首屏由 index.html 内联脚本设置 html.dark, 这里只读状态并持久化切换
const isDark = ref(document.documentElement.classList.contains('dark'))
const mobileOpen = ref(false)

function toggleTheme() {
  isDark.value = !isDark.value
  document.documentElement.classList.toggle('dark', isDark.value)
  try {
    localStorage.setItem('oj_theme', isDark.value ? 'dark' : 'light')
  } catch (e) {}
  updateThemeColor()
}

function updateThemeColor() {
  const meta = document.querySelector('meta[name="theme-color"]:not([media])')
  if (meta) meta.setAttribute('content', isDark.value ? '#10131a' : '#ffffff')
}

// 其他 OJ 跳转链接(页脚外链)
const otherOjs = [
  { name: '洛谷', url: 'https://www.luogu.com.cn/' },
  { name: '牛客', url: 'https://www.nowcoder.com/' },
  { name: '蓝桥杯', url: 'https://www.lanqiao.cn/' },
  { name: 'PTA', url: 'https://pintia.cn/' },
  { name: 'Codeforces', url: 'https://codeforces.com/' },
  { name: 'AtCoder', url: 'https://atcoder.jp/' }
]

function handleCommand(command) {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  } else if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'submissions') {
    router.push('/submissions')
  } else if (command === 'users') {
    router.push('/admin/users')
  }
}

onMounted(updateThemeColor)
</script>

<style>
/* ============ 全局: 布局骨架 ============ */
.app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app .oj-header {
  background: var(--bg);
  border-bottom: 1px solid var(--border);
  position: sticky;
  top: 0;
  z-index: 100;
}

.app .header-inner {
  max-width: 1200px;
  margin: 0 auto;
  height: var(--header-height);
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.app .header-left {
  display: flex;
  align-items: center;
  gap: 28px;
  min-width: 0;
}

/* ---- logo ---- */
.app .logo {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text);
  white-space: nowrap;
  flex-shrink: 0;
}

.app .logo:hover {
  text-decoration: none;
}

.app .logo-mark {
  width: 26px;
  height: 26px;
  border-radius: 6px;
  background: var(--brand);
  color: #fff;
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  letter-spacing: -0.5px;
}

.app .logo-text {
  font-size: 17px;
  font-weight: 700;
  letter-spacing: -0.01em;
}

/* ---- 导航: 下划线激活态 ---- */
.app .header-nav {
  display: flex;
  align-items: center;
  gap: 2px;
  min-width: 0;
}

.app .nav-link {
  position: relative;
  color: var(--text-2);
  font-size: 14px;
  padding: 13px 12px;
  white-space: nowrap;
  transition: color 0.15s;
}

.app .nav-link:hover {
  color: var(--text);
  text-decoration: none;
}

.app .nav-link.router-link-active {
  color: var(--brand);
  font-weight: 600;
}

.app .nav-link.router-link-active::after {
  content: '';
  position: absolute;
  left: 12px;
  right: 12px;
  bottom: 0;
  height: 2px;
  border-radius: 1px;
  background: var(--brand);
}

/* ---- 右侧: 主题开关 + 用户 ---- */
.app .header-right {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.app .icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--text-2);
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.app .icon-btn:hover {
  background: var(--bg-hover);
  color: var(--text);
}

.app .menu-btn {
  display: none;
}

.app .user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: var(--text);
  outline: none;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background 0.15s;
}

.app .user-trigger:hover {
  background: var(--bg-hover);
}

.app .avatar {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: var(--brand);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.app .username {
  font-size: 14px;
  font-weight: 600;
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app .caret {
  color: var(--text-3);
}

/* ---- 移动端导航抽屉 ---- */
.app .mobile-nav {
  display: none;
  flex-direction: column;
  padding: 6px 12px 10px;
  border-top: 1px solid var(--border);
  background: var(--bg);
}

.app .mobile-nav .nav-link {
  padding: 10px 12px;
  border-radius: 6px;
}

.app .mobile-nav .nav-link.router-link-active {
  background: var(--brand-soft);
}

.app .mobile-nav .nav-link.router-link-active::after {
  display: none;
}

.drop-enter-active,
.drop-leave-active {
  transition: opacity 0.15s;
}

.drop-enter-from,
.drop-leave-to {
  opacity: 0;
}

/* ---- 页脚 ---- */
.app .oj-footer {
  margin-top: auto;
  padding: 24px 16px 32px;
  border-top: 1px solid var(--border);
  text-align: center;
}

.app .footer-links {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 6px 20px;
  margin-bottom: 10px;
}

.app .footer-links a {
  color: var(--text-3);
  font-size: 13px;
}

.app .footer-links a:hover {
  color: var(--brand);
}

.app .footer-copy {
  color: var(--text-3);
  font-size: 12px;
  font-family: var(--font-mono);
}

/* ---- 认证页共享卡片(Login/Register/Forgot 共用) ---- */
.auth-page {
  flex: 1;
  background: var(--bg-soft);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 64px 16px 80px;
}

.auth-card {
  width: 400px;
  max-width: 100%;
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 32px 32px 28px;
  box-shadow: var(--shadow-md);
}

.auth-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 18px;
}

.auth-logo .logo-mark {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--brand);
  color: #fff;
  font-family: var(--font-mono);
  font-size: 13px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  letter-spacing: -0.5px;
}

.auth-logo .logo-text {
  font-size: 18px;
  font-weight: 700;
  color: var(--text);
}

.auth-title {
  text-align: center;
  font-size: 20px;
  font-weight: 700;
  color: var(--text);
  margin: 0 0 6px;
}

.auth-subtitle {
  text-align: center;
  color: var(--text-3);
  font-size: 13px;
  margin: 0 0 24px;
}

/* ---- 移动端: 隐藏行内导航, 显示菜单按钮 ---- */
@media (max-width: 820px) {
  .app .header-nav {
    display: none;
  }

  .app .menu-btn {
    display: flex;
  }

  .app .mobile-nav {
    display: flex;
  }
}

@media (max-width: 480px) {
  .app .username {
    display: none;
  }

  .app .logo-text {
    font-size: 16px;
  }
}
</style>
