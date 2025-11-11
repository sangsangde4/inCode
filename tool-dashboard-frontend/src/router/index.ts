import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Dashboard',
    component: () => import('@/views/Dashboard.vue'),
    meta: { title: '工具看板' }
  },
  {
    path: '/tool/:id',
    name: 'ToolDetail',
    component: () => import('@/views/ToolDetail.vue'),
    meta: { title: '工具详情' }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '管理员登录' }
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('@/views/admin/Layout.vue'),
    meta: { title: '后台管理', requireAuth: true },
    children: [
      {
        path: 'tools',
        name: 'AdminTools',
        component: () => import('@/views/admin/Tools.vue'),
        meta: { title: '工具管理' }
      },
      {
        path: 'files',
        name: 'AdminFiles',
        component: () => import('@/views/admin/Files.vue'),
        meta: { title: '文件管理' }
      },
      {
        path: 'changelogs',
        name: 'AdminChangelogs',
        component: () => import('@/views/admin/Changelogs.vue'),
        meta: { title: '变更日志管理' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  document.title = (to.meta.title as string) || '工具看板系统'
  
  // 检查是否需要登录
  if (to.meta.requireAuth) {
    const userStore = useUserStore()
    const token = userStore.token || localStorage.getItem('token')
    if (!token) {
      const redirect = encodeURIComponent(to.fullPath)
      next(`/login?redirect=${redirect}`)
      return
    }
  }
  
  next()
})

export default router
