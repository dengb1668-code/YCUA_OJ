<template>
  <div class="app">
    <!-- HDOJ(杭电 OJ)风格全局头部: 航拍横幅 + 蓝色菜单条, 所有页面(含登录)都显示 -->
    <header class="hdoj-header">
      <!-- HDOJ 布局: 左淡(校徽+校名+ICPC小logo)右浓(航拍)横幅 -->
      <div class="banner">
        <img src="./assets/aerial-banner.png" class="banner-img" alt="校园航拍" />
        <div class="banner-overlay">
          <img src="./assets/ycu-logo.png" class="banner-emblem" alt="宜春学院" />
          <!-- 校名组: 绝对居中 -->
          <div class="banner-texts">
            <!-- 毛笔字校名 -->
            <div class="banner-title">宜春学院</div>
            <!-- 左: ICPC logo + 英文; 右: Online Judge -->
            <div class="banner-title-row">
              <div class="banner-row-left">
                <img src="./assets/icpc-logo-full.png" class="banner-icpc-logo" alt="ICPC" />
                <span class="banner-title-en">YICHUN UNIVERSITY</span>
              </div>
              <span class="banner-oj">Online Judge</span>
            </div>
          </div>
        </div>
      </div>
      <!-- 未登录不显示菜单条(避免看到提交记录等入口) -->
      <nav v-if="userStore.token" class="menu-bar">
        <div class="menu-left">
          <!-- ICPC 导航 logo(白底片衬托, 仿 HDOJ logo 位) -->
          <img src="./assets/icpc-logo-nav.png" class="menu-icpc-logo" alt="ICPC" />
          <router-link to="/problems" class="menu-link">题目列表</router-link>
          <router-link to="/contests" class="menu-link">比赛</router-link>
          <router-link to="/submissions" class="menu-link">提交记录</router-link>
          <router-link to="/profile" class="menu-link">个人主页</router-link>
          <router-link to="/discussion" class="menu-link">讨论区</router-link>
          <el-dropdown trigger="hover" class="menu-dropdown" @command="openOj">
            <span class="menu-link menu-oj">
              其他 OJ
              <span class="caret">▾</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="oj in otherOjs" :key="oj.name" :command="oj.url">
                  {{ oj.name }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <div class="menu-right">
          <!-- 未登录时不显示用户菜单 -->
          <el-dropdown v-if="userStore.token" trigger="click" @command="handleCommand">
            <span class="user-trigger">
              <span class="avatar">{{ (userStore.username || '?')[0].toUpperCase() }}</span>
              <span class="username">{{ userStore.username }}</span>
              <span class="caret">▾</span>
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
      </nav>
    </header>
    <router-view />
    <!-- 全局点击特效: 水滴落地 + 涟漪 -->
    <ClickRipple />
  </div>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { userStore } from './store/user'
import ClickRipple from './components/ClickRipple.vue'

const route = useRoute()
const router = useRouter()

// 其他 OJ 跳转链接(新标签页打开)
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

function openOj(url) {
  window.open(url, '_blank')
}



</script>

<style>
/* ============ 全局主题: HDOJ(杭电 OJ)蓝 ============ */
:root {
  /* 主色: HDOJ 经典蓝 #1A5CC8 */
  --el-color-primary: #1a5cc8;
  --el-color-primary-light-3: #4a7dd3;
  --el-color-primary-light-5: #7b9fdf;
  --el-color-primary-light-7: #a8c0eb;
  --el-color-primary-light-8: #bcd0f1;
  --el-color-primary-light-9: #e0eafa;
  --el-color-primary-dark-2: #154aa0;
}

body {
  margin: 0;
  background: #fff;
  font-family: 'Helvetica Neue', Helvetica, Arial, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 14px;
  color: #333;
}

a {
  color: #1a5cc8;
  text-decoration: none;
}

a:hover {
  text-decoration: underline;
}

/* HDOJ 风格表格: 蓝底白字表头 */
.el-table th.el-table__cell {
  background: #1a5cc8 !important;
  color: #fff !important;
  font-weight: bold;
}

/* 悬浮特效: 表格行 hover 上浮 + 高亮 */
.el-table .el-table__body tr {
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.el-table .el-table__body tr:hover {
  transform: translateY(-2px);
  position: relative;
  z-index: 1;
}

.el-table .el-table__body tr:hover > td.el-table__cell {
  background: #eef4ff !important;
}

/* ============ HDOJ 头部: 蓝边框 + 航拍横幅 + 蓝菜单条 ============ */
.app .hdoj-header {
  max-width: 1100px;
  margin: 0 auto;
  border: 1px solid #1a5cc8;
}

/* 航拍蓝渐变横幅 */
.app .banner {
  position: relative;
  height: 220px;
  background: #fff;
  overflow: hidden;
}

.app .banner-img {
  width: 100%;
  height: 220px;
  object-fit: cover;
  display: block;
}

.app .banner-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 0 24px;
}

/* 左侧淡区: ICPC logo + 校名(英文对齐中文) */
.app .banner-left {
  display: flex;
  align-items: center;
  gap: 18px;
}

.app .banner-texts {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

/* ICPC 导航 logo(HDOJ logo 位) */
.app .banner-icpc-logo {
  height: 52px;
  width: auto;
  flex-shrink: 0;
  filter: drop-shadow(0 0 3px rgba(255, 255, 255, 0.95));
}

/* 校徽圆形徽章(右侧, HDOJ 右彩标位) */
.app .banner-emblem {
  width: 128px;
  height: 128px;
  object-fit: cover;
  border-radius: 50%;
  border: 3px solid #fff;
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.2);
}

/* 校名深色字(左侧淡区可读), 英文对齐中文 */
.app .banner-title {
  color: #0f172a;
  font-size: 56px;
  font-family: 'STXingkai', '华文行楷', 'Xingkai SC', 'STKaiti', 'KaiTi', '楷体', 'Microsoft YaHei', serif;
  letter-spacing: 16px;
  text-indent: 16px;
  line-height: 1.15;
  /* 黑体淡白边: 白色细描边 */
  text-shadow:
    -1px -1px 0 rgba(255, 255, 255, 0.85),
    1px -1px 0 rgba(255, 255, 255, 0.85),
    -1px 1px 0 rgba(255, 255, 255, 0.85),
    1px 1px 0 rgba(255, 255, 255, 0.85),
    0 0 8px rgba(255, 255, 255, 0.7);
}

.app .banner-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  flex-wrap: nowrap;
}

.app .banner-row-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.app .banner-title-en {
  color: #0f172a;
  font-size: 20px;
  letter-spacing: 3px;
  white-space: nowrap;
  flex-shrink: 0;
  font-family: Roboto, Helvetica, Arial, sans-serif;
  text-shadow:
    -1px -1px 0 rgba(255, 255, 255, 0.85),
    1px -1px 0 rgba(255, 255, 255, 0.85),
    -1px 1px 0 rgba(255, 255, 255, 0.85),
    1px 1px 0 rgba(255, 255, 255, 0.85),
    0 0 8px rgba(255, 255, 255, 0.7);
}

/* Online Judge: 白色字体, 在英文右边 */
.app .banner-oj {
  color: #fff;
  font-size: 32px;
  font-weight: bold;
  letter-spacing: 2px;
  white-space: nowrap;
  flex-shrink: 0;
  font-family: Roboto, Helvetica, Arial, sans-serif;
  text-shadow:
    0 1px 3px rgba(0, 0, 0, 0.55),
    0 0 10px rgba(0, 0, 0, 0.35);
}

/* 蓝色菜单条 */
.app .menu-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #1a5cc8;
  height: 34px;
  padding: 0 6px;
}

.app .menu-left {
  display: flex;
  align-items: center;
}

/* 菜单条 ICPC 小 logo: 白底圆角片 */
.app .menu-icpc-logo {
  height: 22px;
  width: auto;
  background: #fff;
  border-radius: 3px;
  padding: 2px 4px;
  margin: 0 10px 0 4px;
}

.app .menu-link {
  color: #fff;
  font-size: 14px;
  line-height: 34px;
  padding: 0 14px;
  display: inline-block;
  position: relative;
  transition: transform 0.25s ease, background 0.25s ease, box-shadow 0.25s ease;
}

.app .menu-link:hover,
.app .menu-link.router-link-active {
  background: #154aa0;
  text-decoration: none;
  color: #fff;
}

/* 悬浮特效: 菜单项轻微上浮 + 底部亮蓝下划线动画 */
.app .menu-link:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.25);
}

.app .menu-link::after {
  content: '';
  position: absolute;
  left: 14px;
  right: 14px;
  bottom: 2px;
  height: 2px;
  background: #4da3ff;
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.25s ease;
}

.app .menu-link:hover::after,
.app .menu-link.router-link-active::after {
  transform: scaleX(1);
}

.app .menu-oj::after {
  display: none;
}

.app .menu-dropdown {
  display: flex;
  align-items: center;
  height: 34px;
}

.app .menu-oj {
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  outline: none;
}

.app .menu-right {
  display: flex;
  align-items: center;
}

.app .user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #fff;
  outline: none;
  padding: 0 10px;
  line-height: 34px;
}

.app .avatar {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: #fff;
  color: #1a5cc8;
  font-size: 13px;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
}

.app .username {
  color: #fff;
  font-size: 14px;
}

.app .caret {
  color: rgba(255, 255, 255, 0.75);
  font-size: 11px;
}
</style>
