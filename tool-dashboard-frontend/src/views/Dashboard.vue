<template>
  <div class="dashboard">
    <header class="header">
      <div class="header-bg-pattern"></div>
      <div class="container">
        <div class="header-left">
          <div class="logo">
            <div class="logo-icon">
              <el-icon :size="36"><Operation /></el-icon>
            </div>
            <div class="logo-text">
              <h1>工具看板系统</h1>
              <p class="logo-subtitle">Tool Dashboard Platform</p>
            </div>
          </div>
          <div class="stats">
            <div class="stat-item">
              <div class="stat-icon">
                <el-icon :size="24"><Memo /></el-icon>
              </div>
              <div class="stat-content">
                <span class="stat-value">{{ totalCount }}</span>
                <span class="stat-label">工具总数</span>
              </div>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <div class="stat-icon">
                <el-icon :size="24"><FolderOpened /></el-icon>
              </div>
              <div class="stat-content">
                <span class="stat-value">{{ groups.length }}</span>
                <span class="stat-label">分类数量</span>
              </div>
            </div>
          </div>
        </div>
        <div class="header-actions">
          <el-button 
            @click="themeStore.toggleTheme()" 
            :icon="themeStore.theme === 'dark' ? Sunny : Moon"
            circle
            size="large"
            class="theme-toggle"
            :title="themeStore.theme === 'dark' ? '切换为亮色主题' : '切换为暗色主题'"
          />
          <el-button v-if="!userStore.isLoggedIn()" type="primary" size="large" @click="router.push('/login')">
            <el-icon><User /></el-icon>
            管理员登录
          </el-button>
          <el-dropdown v-else>
            <el-button type="primary" size="large">
              <el-icon><User /></el-icon>
              {{ userStore.realName }}
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/admin/tools')">
                  <el-icon><Setting /></el-icon>
                  后台管理
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <div class="container main-content">
      <div class="search-section">
        <div class="search-wrapper">
          <el-input
            v-model="keyword"
            placeholder="搜索工具、平台或系统..."
            size="large"
            clearable
            @input="handleSearch"
            class="search-input"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <div class="search-tips">
            <el-icon><InfoFilled /></el-icon>
            <span>支持按名称或描述搜索</span>
          </div>
        </div>
      </div>

      <div v-loading="loading" class="groups-container">
        <div v-for="group in filteredGroups" :key="group.type" class="tool-group">
          <div class="group-header">
            <div class="group-header-left">
              <div class="group-icon-wrapper">
                <el-icon :size="28" :class="getTypeIcon(group.type)"></el-icon>
              </div>
              <div class="group-info">
                <div class="group-title">
                  <h2>{{ group.typeName }}</h2>
                  <el-tag class="group-count" size="large" :type="getTypeColor(group.type)">{{ group.count }} 个工具</el-tag>
                </div>
                <div class="group-description">
                  {{ getTypeDescription(group.type) }}
                </div>
              </div>
            </div>
          </div>
          
          <el-row :gutter="20" class="group-tools">
            <el-col
              v-for="tool in group.tools"
              :key="tool.id"
              :xs="24"
              :sm="12"
              :md="8"
              :lg="6"
            >
              <div class="tool-card-wrapper">
                <el-card class="tool-card" @click="router.push(`/tool/${tool.id}`)">
                  <div class="tool-card-bg"></div>
                  <div class="tool-icon">
                    <div class="icon-wrapper">
                      <el-avatar
                        v-if="tool.iconUrl"
                        :src="getFullIconUrl(tool.iconUrl)"
                        :size="72"
                        shape="square"
                      />
                      <el-avatar v-else :size="72" shape="square" class="default-avatar">
                        <span class="avatar-text">{{ tool.name?.charAt(0) }}</span>
                      </el-avatar>
                    </div>
                    <el-tag
                      :type="getStatusType(tool.status)"
                      size="small"
                      class="status-badge"
                      effect="dark"
                    >
                      <el-icon><CircleCheck v-if="tool.status === 1" /><Warning v-else-if="tool.status === 2" /><CircleClose v-else /></el-icon>
                      {{ getStatusText(tool.status) }}
                    </el-tag>
                  </div>
                  <h3 class="tool-name">{{ tool.name }}</h3>
                  <p class="tool-description">{{ tool.description || '暂无描述' }}</p>
                  <div class="tool-footer">
                    <div class="tool-meta">
                      <div class="meta-item" v-if="tool.currentVersion">
                        <el-icon><PriceTag /></el-icon>
                        <span>{{ tool.currentVersion }}</span>
                      </div>
                      <div class="meta-item" v-if="tool.owner">
                        <el-icon><User /></el-icon>
                        <span>{{ tool.owner }}</span>
                      </div>
                    </div>
                    <div class="tool-action">
                      <el-icon class="action-icon"><ArrowRight /></el-icon>
                    </div>
                  </div>
                </el-card>
              </div>
            </el-col>
          </el-row>
        </div>
      </div>

      <div v-if="!loading && filteredGroups.length === 0" class="empty">
        <el-empty description="暂无工具数据">
          <el-button type="primary" @click="keyword = ''; loadToolGroups()">重置搜索</el-button>
        </el-empty>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getToolsByGroup } from '@/api/tool'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'
import type { ToolGroup } from '@/types'
import { 
  Sunny, Moon, Memo, FolderOpened, InfoFilled, CircleCheck, Warning, CircleClose, 
  PriceTag, User, ArrowRight, Operation, Search, Setting, SwitchButton
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const themeStore = useThemeStore()

const keyword = ref('')
const groups = ref<ToolGroup[]>([])
const loading = ref(false)

const totalCount = computed(() => {
  return groups.value.reduce((sum, group) => sum + group.count, 0)
})

const filteredGroups = computed(() => {
  if (!keyword.value) return groups.value
  
  return groups.value
    .map(group => ({
      ...group,
      tools: group.tools.filter(tool => 
        tool.name?.toLowerCase().includes(keyword.value.toLowerCase()) ||
        tool.description?.toLowerCase().includes(keyword.value.toLowerCase())
      )
    }))
    .filter(group => group.tools.length > 0)
    .map(group => ({
      ...group,
      count: group.tools.length
    }))
})

const loadToolGroups = async () => {
  loading.value = true
  try {
    const res = await getToolsByGroup()
    if (res.data) {
      groups.value = res.data
    }
  } catch (error) {
    console.error('加载工具分组失败', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}


const handleSearch = () => {
  // 搜索已通过computed实现
}

const getTypeIcon = (type: string) => {
  const iconMap: Record<string, string> = {
    '平台': 'Platform',
    '工具': 'Tools',
    '系统': 'Setting',
    '其他': 'Grid'
  }
  return iconMap[type] || 'Grid'
}

const getTypeDescription = (type: string) => {
  const descMap: Record<string, string> = {
    '平台': '企业级应用平台，提供完整的解决方案',
    '工具': '实用工具集，提升开发效率',
    '系统': '系统级应用，支撑业务运行',
    '其他': '其他类型的工具和应用'
  }
  return descMap[type] || ''
}

const getTypeColor = (type: string) => {
  const colorMap: Record<string, string> = {
    '平台': 'primary',
    '工具': 'success',
    '系统': 'warning',
    '其他': 'info'
  }
  return colorMap[type] || 'info'
}

const getStatusType = (status?: number) => {
  switch (status) {
    case 0: return 'info'
    case 1: return 'success'
    case 2: return 'warning'
    default: return ''
  }
}

const getStatusText = (status?: number) => {
  switch (status) {
    case 0: return '已下线'
    case 1: return '运行中'
    case 2: return '维护中'
    default: return '未知'
  }
}

const getFullIconUrl = (url?: string) => {
  if (!url) return ''
  
  if (url.startsWith('http://') || url.startsWith('https://')) {
    try {
      return new URL(url).pathname
    } catch {
      return ''
    }
  }
  
  if (url.startsWith('/api/icon/')) {
    return url
  }
  
  return `/api/icon/${url}`
}

const handleLogout = () => {
  userStore.logout()
  ElMessage.success('已退出登录')
}

onMounted(() => {
  loadToolGroups()
})
</script>

<style scoped>
.dashboard {
  min-height: 100vh;
  background: var(--bg-secondary);
  position: relative;
}

.header {
  position: relative;
  background: var(--header-bg);
  color: white;
  padding: 28px 0;
  box-shadow: 0 8px 24px rgba(0,0,0,0.15);
  border-bottom: 3px solid var(--header-border);
  overflow: hidden;
}

.header-bg-pattern {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: 
    radial-gradient(circle at 20% 50%, rgba(255,140,0,0.1) 0%, transparent 50%),
    radial-gradient(circle at 80% 80%, rgba(255,140,0,0.08) 0%, transparent 50%);
  pointer-events: none;
}

.container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 24px;
}

.header .container {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 40px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.theme-toggle {
  background: rgba(255, 255, 255, 0.1) !important;
  border: 1px solid rgba(255, 255, 255, 0.2) !important;
  color: white !important;
  transition: all 0.3s ease;
}

.theme-toggle:hover {
  background: rgba(255, 140, 0, 0.3) !important;
  border-color: var(--color-primary) !important;
  transform: rotate(180deg);
}

.logo {
  display: flex;
  align-items: center;
  gap: 16px;
}

.logo-icon {
  width: 56px;
  height: 56px;
  background: rgba(255,255,255,0.15);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(10px);
  border: 2px solid rgba(255,255,255,0.2);
  transition: all 0.3s ease;
}

.logo-icon:hover {
  transform: scale(1.05) rotate(5deg);
  background: rgba(255,140,0,0.3);
}

.logo-text h1 {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.logo-subtitle {
  margin: 4px 0 0 0;
  font-size: 12px;
  opacity: 0.85;
  letter-spacing: 1px;
  font-weight: 300;
}

.stats {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 12px 24px;
  background: rgba(255,255,255,0.1);
  border-radius: 16px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255,255,255,0.15);
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  background: rgba(255,140,0,0.2);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.stat-item:hover .stat-icon {
  background: rgba(255,140,0,0.3);
  transform: scale(1.1) rotate(360deg);
}

.stat-content {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.2;
  color: white;
}

.stat-label {
  font-size: 12px;
  opacity: 0.85;
  margin-top: 2px;
}

.stat-divider {
  width: 1px;
  height: 40px;
  background: rgba(255,255,255,0.2);
}

.main-content {
  padding: 40px 24px;
  background: transparent;
}

.search-section {
  margin-bottom: 40px;
  display: flex;
  justify-content: center;
}

.search-wrapper {
  max-width: 680px;
  width: 100%;
  margin: 0 auto;
}

.search-input {
  box-shadow: 0 4px 16px rgba(0,0,0,0.08);
  border-radius: 16px;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 16px;
  padding: 12px 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.search-tips {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  font-size: 13px;
  color: var(--text-tertiary);
  padding-left: 8px;
}

.search-tips .el-icon {
  font-size: 14px;
}

.groups-container {
  min-height: 400px;
}

.tool-group {
  margin-bottom: 48px;
}

.group-header {
  margin-bottom: 28px;
  padding: 20px 24px;
  background: linear-gradient(135deg, rgba(255,140,0,0.1) 0%, rgba(255,140,0,0.04) 100%);
  border-radius: 16px;
  border-left: 5px solid var(--color-primary);
  box-shadow: 
    0 2px 8px rgba(0,0,0,0.05),
    inset 0 1px 0 rgba(255,255,255,0.05);
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.group-header::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent 0%, rgba(255,140,0,0.05) 50%, transparent 100%);
  transform: translateX(-100%);
  transition: transform 0.6s ease;
}

.group-header:hover::before {
  transform: translateX(100%);
}

.group-header:hover {
  box-shadow: 
    0 4px 16px rgba(255,140,0,0.2),
    inset 0 1px 0 rgba(255,255,255,0.08);
  transform: translateX(6px);
  border-left-width: 6px;
}

.group-header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.group-icon-wrapper {
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, var(--color-primary) 0%, #ff6600 100%);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 4px 12px rgba(255,140,0,0.3);
}

.group-info {
  flex: 1;
}

.group-title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.group-title h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: var(--color-primary);
}

.group-count {
  font-weight: 600;
}

.group-description {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.group-tools {
  padding-top: 8px;
}

.tool-card-wrapper {
  height: 100%;
  margin-bottom: 20px;
}

.tool-card {
  height: 100%;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  border-radius: 16px;
  overflow: visible;
  background: var(--card-bg) !important;
  border: 2px solid var(--card-border) !important;
  position: relative;
  backdrop-filter: blur(10px);
}

.tool-card::before {
  content: '';
  position: absolute;
  inset: -2px;
  border-radius: 16px;
  padding: 2px;
  background: linear-gradient(135deg, rgba(255,140,0,0.3) 0%, rgba(255,102,0,0.1) 100%);
  -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  opacity: 0;
  transition: opacity 0.4s ease;
  pointer-events: none;
}

.tool-card:hover::before {
  opacity: 1;
}

.tool-card-bg {
  position: absolute;
  top: 0;
  right: 0;
  width: 120px;
  height: 120px;
  background: radial-gradient(circle, rgba(255,140,0,0.08) 0%, transparent 70%);
  pointer-events: none;
  transition: all 0.4s ease;
  z-index: 0;
}

.tool-card:hover {
  transform: translateY(-12px) scale(1.02);
  box-shadow: 
    0 20px 40px rgba(255,140,0,0.25),
    0 10px 20px rgba(0,0,0,0.2),
    0 0 0 1px rgba(255,140,0,0.1) inset !important;
  border-color: rgba(255,140,0,0.5) !important;
}

.tool-card:hover .tool-card-bg {
  width: 220px;
  height: 220px;
  opacity: 1.2;
}

.tool-card:hover .action-icon {
  transform: translateX(4px) scale(1.1);
}

.tool-card > :not(.tool-card-bg) {
  position: relative;
  z-index: 1;
}

.tool-icon {
  position: relative;
  display: flex;
  justify-content: center;
  margin-bottom: 20px;
}

.icon-wrapper {
  position: relative;
  padding: 10px;
  background: linear-gradient(135deg, rgba(255,140,0,0.12) 0%, rgba(255,140,0,0.06) 100%);
  border-radius: 20px;
  transition: all 0.3s ease;
  box-shadow: 
    0 4px 12px rgba(0,0,0,0.1),
    inset 0 1px 0 rgba(255,255,255,0.1);
}

.tool-card:hover .icon-wrapper {
  transform: scale(1.08) rotate(-2deg);
  background: linear-gradient(135deg, rgba(255,140,0,0.18) 0%, rgba(255,140,0,0.1) 100%);
  box-shadow: 
    0 6px 20px rgba(255,140,0,0.3),
    inset 0 1px 0 rgba(255,255,255,0.15);
}

.icon-wrapper .el-avatar {
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  border: 2px solid rgba(255,255,255,0.1);
}

.default-avatar {
  background: linear-gradient(135deg, var(--color-primary) 0%, #ff6600 100%);
  font-size: 32px;
  font-weight: 700;
  position: relative;
}

.default-avatar::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(255,255,255,0.2) 0%, transparent 50%);
  border-radius: inherit;
}

.avatar-text {
  font-size: 32px;
  position: relative;
  z-index: 1;
}

.status-badge {
  position: absolute;
  top: -8px;
  right: -8px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 4px;
  box-shadow: 
    0 4px 12px rgba(0,0,0,0.2),
    0 0 0 3px var(--card-bg);
  padding: 4px 10px;
  backdrop-filter: blur(10px);
}

.tool-name {
  margin: 0 0 12px 0;
  font-size: 19px;
  font-weight: 700;
  color: var(--text-primary);
  text-align: center;
  line-height: 1.3;
  min-height: 25px;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: all 0.3s ease;
  text-shadow: 0 1px 2px rgba(0,0,0,0.1);
}

.tool-card:hover .tool-name {
  color: var(--color-primary);
  text-shadow: 0 2px 4px rgba(255,140,0,0.3);
  transform: translateY(-1px);
}

.tool-description {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.7;
  min-height: 44px;
  margin-bottom: 20px;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  text-align: center;
}

.tool-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  border-top: 2px solid var(--border-secondary);
}

.tool-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-secondary);
  transition: color 0.3s ease;
}

.meta-item .el-icon {
  font-size: 14px;
  color: var(--color-primary);
}

.tool-card:hover .meta-item {
  color: var(--text-primary);
}

.tool-action {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary) 0%, #ff6600 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  box-shadow: 
    0 2px 8px rgba(255,140,0,0.3),
    inset 0 1px 0 rgba(255,255,255,0.2);
  position: relative;
  overflow: hidden;
}

.tool-action::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.3), transparent);
  transition: left 0.5s ease;
}

.tool-card:hover .tool-action::before {
  left: 100%;
}

.action-icon {
  font-size: 18px;
  color: white;
  transition: transform 0.3s ease;
  filter: drop-shadow(0 1px 2px rgba(0,0,0,0.2));
}

.tool-card:hover .tool-action {
  box-shadow: 
    0 6px 16px rgba(255,140,0,0.5),
    inset 0 1px 0 rgba(255,255,255,0.3);
  transform: scale(1.15) rotate(90deg);
}

.empty {
  padding: 80px 20px;
  text-align: center;
}

@media (max-width: 768px) {
  .header-left {
    flex-direction: column;
    align-items: flex-start;
    gap: 20px;
  }
  
  .stats {
    width: 100%;
    justify-content: space-around;
  }
  
  .stat-value {
    font-size: 20px;
  }
  
  .logo-icon {
    width: 48px;
    height: 48px;
  }
  
  .logo-text h1 {
    font-size: 24px;
  }
  
  .group-header {
    padding: 16px;
  }
  
  .group-icon-wrapper {
    width: 48px;
    height: 48px;
  }
  
  .tool-card:hover {
    transform: translateY(-6px) scale(1.01);
  }
}
</style>
