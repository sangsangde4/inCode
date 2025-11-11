/**
 * 文件上传限制配置
 * 与后端配置保持一致
 */

// 文件大小常量（字节）
export const FILE_SIZE = {
  // 单个文件最大 2GB
  MAX_FILE_SIZE: 2 * 1024 * 1024 * 1024,
  
  // 多文件上传总大小最大 10GB
  MAX_TOTAL_SIZE: 10 * 1024 * 1024 * 1024,
  
  // 图标文件最大 2MB
  MAX_ICON_SIZE: 2 * 1024 * 1024
}

// 文件数量限制
export const FILE_COUNT = {
  // 一次最多上传10个文件
  MAX_FILES: 10
}

// 格式化文件大小显示
export const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i]
}

// 验证单个文件大小
export const validateFileSize = (file: File, maxSize: number = FILE_SIZE.MAX_FILE_SIZE): boolean => {
  return file.size <= maxSize
}

// 验证多文件总大小
export const validateTotalSize = (files: File[], maxSize: number = FILE_SIZE.MAX_TOTAL_SIZE): boolean => {
  const totalSize = files.reduce((sum, file) => sum + file.size, 0)
  return totalSize <= maxSize
}

// 获取文件限制提示文本
export const getFileLimitHints = () => {
  return {
    single: `单个文件最大 2GB`,
    total: `总大小最大 10GB`,
    count: `最多一次选择 ${FILE_COUNT.MAX_FILES} 个文件`,
    icon: `图标文件最大 2MB`
  }
}
