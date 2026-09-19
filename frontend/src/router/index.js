import { createRouter, createWebHistory } from 'vue-router'
import ProblemList from '../views/ProblemList.vue'
import ProblemDetail from '../views/ProblemDetail.vue'
import ProblemCreate from '../views/ProblemCreate.vue'
import ProblemManage from '../views/ProblemManage.vue'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import ForgotPassword from '../views/ForgotPassword.vue'
import SubmissionList from '../views/SubmissionList.vue'
import Profile from '../views/Profile.vue'
import DiscussionList from '../views/DiscussionList.vue'
import ProblemDiscussion from '../views/ProblemDiscussion.vue'
import ProblemSolution from '../views/ProblemSolution.vue'
import PostDetail from '../views/PostDetail.vue'
import UserManage from '../views/UserManage.vue'
import { userStore } from '../store/user'

const routes = [
  { path: '/', redirect: '/problems' },
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: Register,
    meta: { title: '注册' }
  },
  {
    path: '/forgot',
    name: 'ForgotPassword',
    component: ForgotPassword,
    meta: { title: '找回密码' }
  },
  {
    path: '/problems',
    name: 'ProblemList',
    component: ProblemList,
    meta: { title: '题目列表' }
  },
  // 注意: /problems/create 必须放在 /problems/:id 之前, 否则会被当作题目ID匹配
  {
    path: '/problems/create',
    name: 'ProblemCreate',
    component: ProblemCreate,
    meta: { title: '创建题目' }
  },
  // 注意: /problems/:id/discussion、/problems/:id/solution 必须放在 /problems/:id 之前
  {
    path: '/problems/:id/discussion',
    name: 'ProblemDiscussion',
    component: ProblemDiscussion,
    meta: { title: '题目讨论' }
  },
  {
    path: '/problems/:id/solution',
    name: 'ProblemSolution',
    component: ProblemSolution,
    meta: { title: '题解' }
  },
  {
    path: '/problems/:id/manage',
    name: 'ProblemManage',
    component: ProblemManage,
    meta: { title: '题目管理' }
  },
  {
    path: '/problems/:id',
    name: 'ProblemDetail',
    component: ProblemDetail,
    meta: { title: '题目详情' }
  },
  {
    path: '/submissions',
    name: 'SubmissionList',
    component: SubmissionList,
    meta: { title: '提交记录' }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: Profile,
    meta: { title: '个人主页' }
  },
  {
    path: '/discussion',
    name: 'DiscussionList',
    component: DiscussionList,
    meta: { title: '讨论区' }
  },
  {
    path: '/post/:id',
    name: 'PostDetail',
    component: PostDetail,
    meta: { title: '帖子详情' }
  },
  {
    path: '/admin/users',
    name: 'UserManage',
    component: UserManage,
    meta: { title: '用户管理', requiresOwner: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局守卫: 未登录一律去登录页(登录/注册/找回密码页本身除外)
router.beforeEach((to) => {
  if (to.path !== '/login' && to.path !== '/register' && to.path !== '/forgot' && !userStore.token) {
    return { path: '/login' }
  }
  // 仅站长可访问的页面
  if (to.meta.requiresOwner && userStore.role !== 'OWNER') {
    return { path: '/problems' }
  }
})

export default router
