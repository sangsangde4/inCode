<template>
  <div class="tool-detail">
    <div class="detail-header">
      <div class="container header-content">
        <el-button @click="router.back()" class="back-btn" circle>
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <div class="breadcrumb">
          <span class="breadcrumb-item" @click="router.push('/')">首页</span>
          <el-icon><ArrowRight /></el-icon>
          <span class="breadcrumb-current">工具详情</span>
        </div>
      </div>
    </div>

    <div class="container">
      <el-card v-loading="loading" class="info-card" shadow="never">
        <div class="card-bg-pattern"></div>
        <div class="tool-header">
          <div class="avatar-wrapper">
            <el-avatar
              v-if="iconUrl"
              :src="iconUrl"
              :size="100"
              shape="square"
            />
            <el-avatar v-else :size="100" shape="square" class="default-avatar">
              {{ tool.name?.charAt(0) }}
            </el-avatar>
          </div>
          <div class="tool-info">
            <div class="tool-title-row">
              <h2>{{ tool.name }}</h2>
              <el-tag 
                :type="getStatusType(tool.status)"
                size="large"
                class="status-tag"
                effect="dark"
              >
                <el-icon><CircleCheck v-if="tool.status === 1" /><Warning v-else-if="tool.status === 2" /><CircleClose v-else /></el-icon>
                {{ getStatusText(tool.status) }}
              </el-tag>
            </div>
            <p class="tool-description">{{ tool.description }}</p>
            <div class="tool-meta">
              <div class="meta-item" v-if="tool.currentVersion">
                <el-icon><PriceTag /></el-icon>
                <span>版本 {{ tool.currentVersion }}</span>
              </div>
              <div class="meta-divider" v-if="tool.currentVersion && tool.owner"></div>
              <div class="meta-item" v-if="tool.owner">
                <el-icon><User /></el-icon>
                <span>负责人: {{ tool.owner }}</span>
              </div>
              <div class="meta-divider" v-if="tool.type"></div>
              <div class="meta-item" v-if="tool.type">
                <el-icon><FolderOpened /></el-icon>
                <span>类型: {{ tool.type }}</span>
              </div>
            </div>
            <el-button
              v-if="tool.accessUrl"
              type="primary"
              size="large"
              class="access-btn"
              @click="openUrl(tool.accessUrl)"
            >
              <el-icon><Link /></el-icon>
              访问工具
            </el-button>
          </div>
        </div>
      </el-card>

      <el-card class="content-card" shadow="never">
        <el-tabs v-model="activeTab" class="detail-tabs" type="card">
          <el-tab-pane name="files">
            <template #label>
              <div class="tab-label">
                <el-icon><Document /></el-icon>
                <span>文件下载</span>
                <el-badge :value="files.length" class="tab-badge" v-if="files.length > 0" />
              </div>
            </template>
            <el-table :data="files" v-loading="filesLoading" stripe>
              <el-table-column prop="originalName" label="文件名" min-width="200">
                <template #default="{ row }">
                  <div class="file-name">
                    <el-icon class="file-icon"><DocumentCopy /></el-icon>
                    <span>{{ row.originalName }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="version" label="版本" width="120">
                <template #default="{ row }">
                  <el-tag size="small" type="info">{{ row.version }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="fileSize" label="文件大小" width="120">
                <template #default="{ row }">
                  <span class="file-size">{{ formatFileSize(row.fileSize) }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="downloadCount" label="下载次数" width="120">
                <template #default="{ row }">
                  <el-tag size="small" type="warning">
                    <el-icon><Download /></el-icon>
                    {{ row.downloadCount }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="上传时间" width="180" />
              <el-table-column label="操作" width="120" fixed="right">
                <template #default="{ row }">
                  <el-button type="primary" size="small" @click="downloadFile(row)">
                    <el-icon><Download /></el-icon>
                    下载
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="files.length === 0" description="暂无文件" />
          </el-tab-pane>

          <el-tab-pane name="changelogs">
            <template #label>
              <div class="tab-label">
                <el-icon><Tickets /></el-icon>
                <span>变更日志</span>
                <el-badge :value="changelogs.length" class="tab-badge" v-if="changelogs.length > 0" />
              </div>
            </template>
            <el-timeline class="changelog-timeline">
              <el-timeline-item
                v-for="log in changelogs"
                :key="log.id"
                :timestamp="log.changeTime"
                placement="top"
                type="primary"
                size="large"
              >
                <el-card class="log-card" shadow="hover">
                  <div class="log-header">
                    <div class="log-left">
                      <el-tag v-if="log.version" type="success" size="large" effect="dark">
                        <el-icon><PriceTag /></el-icon>
                        {{ log.version }}
                      </el-tag>
                      <el-tag v-if="log.changeType" :type="getChangeTypeColor(log.changeType)" size="large">
                        {{ log.changeType }}
                      </el-tag>
                    </div>
                    <div class="log-right" v-if="log.changer">
                      <el-icon><User /></el-icon>
                      <span>{{ log.changer }}</span>
                    </div>
                  </div>
                  <p class="log-content">{{ log.content }}</p>
                </el-card>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-if="changelogs.length === 0" description="暂无变更日志" />
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getToolDetail } from '@/api/tool'
import { getFilesByToolId, getDownloadUrl } from '@/api/file'
import { getLogsByToolId } from '@/api/changelog'
import { Link, CircleCheck, Warning, CircleClose, PriceTag, User, FolderOpened, Document, DocumentCopy, Download, Tickets, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import type { Tool, ToolFile, ChangeLog } from '@/types'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const filesLoading = ref(false)
const activeTab = ref('files')
const tool = ref<Tool>({} as Tool)
const files = ref<ToolFile[]>([])
const changelogs = ref<ChangeLog[]>([])
const iconUrl = ref<string>('')

const loadToolDetail = async () => {
  loading.value = true
  try {
    const id = Number(route.params.id)
    const res = await getToolDetail(id)
    if (res.data) {
      tool.value = res.data
      // 处理图标URL
      if (tool.value.icon) {
        iconUrl.value = getFullIconUrl(tool.value.icon)
      }
    }
    await Promise.all([loadFiles(id), loadChangelogs(id)])
  } catch (error) {
    console.error('加载工具详情失败', error)
  } finally {
    loading.value = false
  }
}

const loadFiles = async (toolId: number) => {
  filesLoading.value = true
  try {
    const res = await getFilesByToolId(toolId)
    if (res.data) {
      files.value = res.data
    }
  } catch (error) {
    console.error('加载文件列表失败', error)
  } finally {
    filesLoading.value = false
  }
}

const loadChangelogs = async (toolId: number) => {
  try {
    const res = await getLogsByToolId(toolId)
    if (res.data) {
      changelogs.value = res.data
    }
  } catch (error) {
    console.error('加载变更日志失败', error)
  }
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

const formatFileSize = (size?: number) => {
  if (!size) return '-'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(2) + ' KB'
  if (size < 1024 * 1024 * 1024) return (size / 1024 / 1024).toFixed(2) + ' MB'
  return (size / 1024 / 1024 / 1024).toFixed(2) + ' GB'
}

const downloadFile = (fileOrId: ToolFile | number) => {
  // 如果传入的是文件对象，优先使用路径下载
  if (typeof fileOrId === 'object' && fileOrId.downloadUrlByPath) {
    window.open(fileOrId.downloadUrlByPath, '_blank')
    console.log('使用路径下载:', fileOrId.downloadUrlByPath)
  } 
  // 如果传入的是ID或没有路径下载URL，使用ID下载
  else {
    const id = typeof fileOrId === 'number' ? fileOrId : fileOrId.id!
    window.open(getDownloadUrl(id), '_blank')
    console.log('使用ID下载:', id)
  }
}

const openUrl = (url?: string) => {
  if (url) {
    window.open(url, '_blank')
  }
}

const getChangeTypeColor = (type?: string) => {
  const typeMap: Record<string, string> = {
    '新增': 'success',
    '优化': 'primary',
    '修复': 'warning',
    '删除': 'danger'
  }
  return typeMap[type || ''] || 'info'
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

onMounted(() => {
  loadToolDetail()
})
</script>

<style scoped>
.tool-detail {
  min-height: 100vh;
  background: linear-gradient(to bottom, var(--bg-secondary) 0%, var(--bg-primary) 100%);
  padding-bottom: 40px;
}

.detail-header {
  background: var(--header-bg);
  padding: 20px 0;
  margin-bottom: 30px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  border-bottom: 2px solid var(--header-border);
}

.header-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-btn {
  background: rgba(255,255,255,0.1) !important;
  border: 1px solid rgba(255,255,255,0.2) !important;
  color: white !important;
}

.back-btn:hover {
  background: rgba(255,140,0,0.3) !important;
  transform: translateX(-4px);
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  color: rgba(255,255,255,0.8);
  font-size: 14px;
}

.breadcrumb-item {
  cursor: pointer;
  transition: color 0.3s ease;
}

.breadcrumb-item:hover {
  color: var(--color-primary);
}

.breadcrumb-current {
  color: white;
  font-weight: 600;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.info-card {
  background: var(--card-bg) !important;
  border-radius: 16px !important;
  border: 2px solid var(--card-border) !important;
  margin-bottom: 24px;
  overflow: hidden;
  position: relative;
}

.card-bg-pattern {
  position: absolute;
  top: 0;
  right: 0;
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(255,140,0,0.06) 0%, transparent 70%);
  pointer-events: none;
}

.tool-header {
  display: flex;
  gap: 32px;
  align-items: flex-start;
  position: relative;
  z-index: 1;
}

.avatar-wrapper {
  padding: 12px;
  background: linear-gradient(135deg, rgba(255,140,0,0.1) 0%, rgba(255,140,0,0.05) 100%);
  border-radius: 20px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.1);
}

.avatar-wrapper .el-avatar {
  border-radius: 16px;
}

.default-avatar {
  background: linear-gradient(135deg, var(--color-primary) 0%, #ff6600 100%);
  font-size: 40px;
  font-weight: 700;
}

.tool-info {
  flex: 1;
}

.tool-title-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
}

.tool-info h2 {
  margin: 0;
  font-size: 32px;
  font-weight: 700;
  color: var(--color-primary);
  line-height: 1.2;
}

.status-tag {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  font-weight: 600;
}

.tool-description {
  color: var(--text-secondary);
  margin: 0 0 20px 0;
  line-height: 1.8;
  font-size: 15px;
}

.tool-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: rgba(255,140,0,0.08);
  border-radius: 8px;
  font-size: 14px;
  color: var(--text-primary);
  border: 1px solid rgba(255,140,0,0.15);
}

.meta-item .el-icon {
  color: var(--color-primary);
  font-size: 16px;
}

.meta-divider {
  width: 1px;
  height: 20px;
  background: var(--border-primary);
}

.access-btn {
  padding: 12px 32px;
  font-size: 16px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(255,140,0,0.3);
}

.access-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(255,140,0,0.4);
}

.content-card {
  background: var(--card-bg) !important;
  border-radius: 16px !important;
  border: 2px solid var(--card-border) !important;
}

.detail-tabs {
  --el-tabs-header-height: 56px;
}

.detail-tabs :deep(.el-tabs__header) {
  margin: 0;
  background: var(--bg-tertiary);
  padding: 8px;
  border-radius: 8px;
}

.detail-tabs :deep(.el-tabs__nav) {
  border: none;
}

.detail-tabs :deep(.el-tabs__item) {
  border: 2px solid transparent !important;
  border-radius: 6px !important;
  margin-right: 8px;
  transition: all 0.3s ease;
}

.detail-tabs :deep(.el-tabs__item:hover) {
  background: rgba(255,140,0,0.1) !important;
  border-color: rgba(255,140,0,0.3) !important;
}

.detail-tabs :deep(.el-tabs__item.is-active) {
  background: var(--color-primary) !important;
  color: white !important;
  border-color: var(--color-primary) !important;
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 500;
}

.tab-badge {
  margin-left: 4px;
}

.file-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-icon {
  color: var(--color-primary);
  font-size: 18px;
}

.file-size {
  font-weight: 600;
  color: var(--text-primary);
}

.changelog-timeline {
  padding: 20px 0;
}

.log-card {
  background: var(--card-bg) !important;
  border: 1px solid var(--card-border) !important;
  border-radius: 12px;
  transition: all 0.3s ease;
}

.log-card:hover {
  transform: translateX(4px);
  border-color: var(--color-primary) !important;
}

.log-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-secondary);
}

.log-left {
  display: flex;
  gap: 10px;
  align-items: center;
}

.log-right {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--text-secondary);
  font-size: 13px;
}

.log-content {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.8;
  white-space: pre-wrap;
  font-size: 14px;
}

@media (max-width: 768px) {
  .tool-header {
    flex-direction: column;
    gap: 20px;
  }
  
  .tool-title-row {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .tool-info h2 {
    font-size: 24px;
  }
  
  .tool-meta {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
