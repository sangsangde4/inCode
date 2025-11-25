<template>
  <div class="file-browser">
    <!-- 面包屑导航 -->
    <el-breadcrumb separator="/" class="breadcrumb">
      <el-breadcrumb-item @click="navigateTo([])">全部文件</el-breadcrumb-item>
      <el-breadcrumb-item
        v-for="(item, index) in currentPath"
        :key="index"
        @click="navigateTo(currentPath.slice(0, index + 1))"
      >
        {{ item }}
      </el-breadcrumb-item>
    </el-breadcrumb>

    <!-- 文件列表 -->
    <el-table :data="currentItems" v-loading="loading" stripe border :resizable="true" class="file-table">
      <el-table-column label="名称" min-width="300">
        <template #default="{ row }">
          <div class="file-item" @click="handleItemClick(row)">
            <el-icon v-if="row.isFolder" class="folder-icon"><Folder /></el-icon>
            <el-icon v-else class="file-icon"><Document /></el-icon>
            <span class="item-name">{{ row.name }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="大小" width="120">
        <template #default="{ row }">
          <span v-if="!row.isFolder">{{ formatFileSize(row.size) }}</span>
          <span v-else class="folder-size">-</span>
        </template>
      </el-table-column>
      <el-table-column label="上传时间" width="180">
        <template #default="{ row }">
          <span v-if="!row.isFolder">{{ row.uploadTime }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <template v-if="!row.isFolder">
            <el-button type="primary" link @click.stop="handleDownload(row)">下载</el-button>
            <el-button v-if="isAdmin" type="danger"link @click.stop="handleDelete(row)">删除</el-button>
          </template>
          <template v-if="row.isFolder && isAdmin">
            <el-button type="danger" link @click.stop="handleDeleteFolder(row)">删除文件夹</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Folder, Document } from '@element-plus/icons-vue'
import type { ToolFile } from '@/types'

interface FileItem {
  name: string
  isFolder: boolean
  size?: number
  uploadTime?: string
  downloadUrl?: string
  fileData?: ToolFile
}

interface Props {
  files: ToolFile[]
  loading?: boolean
  skipLevels?: number  // 跳过前面几层路径，默认0（显示全部）
  isAdmin?: boolean
}

const props = defineProps<Props>()
const emit = defineEmits(['download', 'delete', 'deleteFolder'])

const currentPath = ref<string[]>([])

// 构建文件树结构
const buildFileTree = (files: ToolFile[]) => {
  const tree: Record<string, any> = {}
  const skipLevels = props.skipLevels || 0

  files.forEach(file => {
    // 解析文件路径: toolType/toolName/version/architecture/fileName
    // 或: toolType/toolName/version/fileName (无架构)
    const parts = file.filePath.split('/')
    let current = tree

    // 遍历路径的每一部分（除了最后的文件名）
    // 从 skipLevels 开始，跳过前面的层级
    for (let i = skipLevels; i < parts.length - 1; i++) {
      const part = parts[i]
      if (!current[part]) {
        current[part] = {}
      }
      current = current[part]
    }

    // 添加文件
    const fileName = parts[parts.length - 1]
    current[fileName] = file
  })

  return tree
}

// 获取当前目录的内容
const currentItems = computed(() => {
  const tree = buildFileTree(props.files)
  let current: any = tree

  // 根据当前路径导航到对应位置
  for (const pathPart of currentPath.value) {
    if (current[pathPart]) {
      current = current[pathPart]
    } else {
      return []
    }
  }

  // 转换为显示项
  const items: FileItem[] = []
  for (const [key, value] of Object.entries(current)) {
    if (typeof value === 'object' && value.id) {
      // 这是一个文件
      const file = value as ToolFile
      items.push({
        name: key,
        isFolder: false,
        size: file.fileSize,
        uploadTime: file.createTime,
        downloadUrl: file.downloadUrlByPath || file.downloadUrl,
        fileData: file
      })
    } else {
      // 这是一个文件夹
      items.push({
        name: key,
        isFolder: true
      })
    }
  }

  // 文件夹在前，文件在后
  return items.sort((a, b) => {
    if (a.isFolder && !b.isFolder) return -1
    if (!a.isFolder && b.isFolder) return 1
    return a.name.localeCompare(b.name)
  })
})

// 格式化文件大小
const formatFileSize = (size?: number) => {
  if (!size) return '-'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(2) + ' KB'
  if (size < 1024 * 1024 * 1024) return (size / 1024 / 1024).toFixed(2) + ' MB'
  return (size / 1024 / 1024 / 1024).toFixed(2) + ' GB'
}

// 导航到指定路径
const navigateTo = (path: string[]) => {
  currentPath.value = [...path]
}

// 处理项目点击
const handleItemClick = (item: FileItem) => {
  if (item.isFolder) {
    currentPath.value.push(item.name)
  }
}

// 处理下载
const handleDownload = (item: FileItem) => {
  if (item.fileData) {
    emit('download', item.fileData)
  }
}

// 处理删除
const handleDelete = (item: FileItem) => {
  if (item.fileData) {
    emit('delete', item.fileData)
  }
}

// 处理删除文件夹
const handleDeleteFolder = (item: FileItem) => {
  // 计算当前文件夹的URL风格路径：currentPath + item.name
  const folderParts = [...currentPath.value, item.name]
  // 跳过前置层级（如果配置了 skipLevels）
  const start = props.skipLevels || 0
  const parts = folderParts.slice(start)
  const urlPath = parts.join('/')
  emit('deleteFolder', urlPath)
}

// 监听files变化，重置路径
watch(() => props.files, () => {
  currentPath.value = []
}, { deep: true })
</script>

<style scoped>
.file-browser {
  width: 100%;
}

.breadcrumb {
  padding: 16px 0;
  font-size: 14px;
}

.breadcrumb :deep(.el-breadcrumb__item) {
  cursor: pointer;
}

.breadcrumb :deep(.el-breadcrumb__item):hover {
  color: var(--el-color-primary);
}

.file-table {
  margin-top: 12px;
}

.file-item {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 4px 0;
}

.file-item:hover {
  color: var(--el-color-primary);
}

.folder-icon {
  color: #ff8c00;
  font-size: 20px;
  margin-right: 8px;
}

.file-icon {
  color: #909399;
  font-size: 20px;
  margin-right: 8px;
}

.item-name {
  font-weight: 500;
}

.folder-size {
  color: #909399;
}
</style>
