<template>
  <el-container class="admin-layout">
    <el-aside width="200px">
      <div class="logo">
        <h3>后台管理</h3>
      </div>
      <el-menu
        :default-active="route.path"
        router
        class="admin-menu"
      >
        <el-menu-item index="/admin/tools" class="menu-item">
          <div class="menu-item-content">
            <el-icon class="menu-icon"><Tools /></el-icon>
            <span>工具管理</span>
          </div>
        </el-menu-item>
        <el-menu-item index="/admin/files" class="menu-item">
          <div class="menu-item-content">
            <el-icon class="menu-icon"><Document /></el-icon>
            <span>文件管理</span>
          </div>
        </el-menu-item>
        <el-menu-item index="/admin/changelogs" class="menu-item">
          <div class="menu-item-content">
            <el-icon class="menu-icon"><Tickets /></el-icon>
            <span>日志管理</span>
          </div>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header>
        <div class="header-content">
          <span>工具看板后台管理系统</span>
          <div class="header-right">
            <el-button 
              @click="themeStore.toggleTheme()" 
              :icon="themeStore.theme === 'dark' ? Sunny : Moon"
              circle
              class="theme-toggle"
              :title="themeStore.theme === 'dark' ? '切换为亮色主题' : '切换为暗色主题'"
            />
            <el-dropdown>
              <span class="user-info">
                <el-icon><User /></el-icon>
                {{ userStore.realName }}
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="router.push('/')">返回首页</el-dropdown-item>
                  <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </el-header>

      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import { ElMessage } from 'element-plus'
import { Sunny, Moon } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const themeStore = useThemeStore()

const handleLogout = () => {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
.admin-layout {
  height: 100vh;
  overflow: hidden;
}

.el-aside {
  background: linear-gradient(180deg, var(--menu-bg) 0%, var(--bg-secondary) 100%);
  overflow: hidden;
  border-right: none;
  box-shadow: 2px 0 12px rgba(0,0,0,0.15);
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  background: linear-gradient(135deg, var(--color-primary) 0%, #ff6600 100%);
  border-bottom: none;
  position: relative;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.2);
}

.logo::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255,255,255,0.1) 0%, transparent 70%);
  animation: logoShine 3s infinite;
}

@keyframes logoShine {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(10%, 10%); }
}

.logo h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  position: relative;
  z-index: 1;
  text-shadow: 0 2px 4px rgba(0,0,0,0.2);
}

.el-header {
  background: var(--card-bg);
  border-bottom: none;
  display: flex;
  align-items: center;
  padding: 0 24px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
}

.header-content {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: var(--text-primary);
}

.header-content > span {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-primary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.theme-toggle {
  background: rgba(0, 217, 255, 0.1) !important;
  border: 1px solid var(--border-primary) !important;
  color: var(--color-primary) !important;
  transition: all 0.3s ease;
}

.theme-toggle:hover {
  background: rgba(0, 217, 255, 0.2) !important;
  transform: rotate(180deg);
}

.user-info {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.user-info:hover {
  background: rgba(0, 217, 255, 0.1);
}

.el-main {
  background: linear-gradient(to bottom, var(--bg-primary) 0%, var(--bg-secondary) 100%);
  padding: 24px;
  min-height: calc(100vh - 60px);
}

/* 菜单样式 */
.admin-menu {
  border-right: none !important;
  background: transparent !important;
  padding: 8px;
}

.menu-item {
  margin: 8px 0;
  border-radius: 12px !important;
  transition: all 0.3s ease;
  height: 48px;
  line-height: 48px;
}

.menu-item-content {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 8px;
}

.menu-icon {
  font-size: 18px;
  transition: all 0.3s ease;
}

.menu-item:hover {
  background: rgba(0, 217, 255, 0.1) !important;
}

.menu-item:hover .menu-icon {
  transform: scale(1.2);
  color: var(--color-primary);
}

.menu-item.is-active {
  background: linear-gradient(135deg, var(--color-primary) 0%, #ff6600 100%) !important;
  color: white !important;
  box-shadow: 0 4px 12px rgba(0, 217, 255, 0.25);
}

.menu-item.is-active .menu-icon {
  color: white;
}
</style>
