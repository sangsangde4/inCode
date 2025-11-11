import { request } from '@/utils/request'
import type { ToolFile, PageResponse } from '@/types'

// 根据工具ID查询文件列表
export const getFilesByToolId = (toolId: number) => {
  return request.get<ToolFile[]>(`/files/tool/${toolId}`)
}

// 分页查询文件列表
export const getFilePage = (params: { pageNum: number; pageSize: number; toolId?: number }) => {
  return request.get<PageResponse<ToolFile>>('/files/page', { params })
}

// 上传文件（支持进度回调）
export const uploadFile = (formData: FormData, onUploadProgress?: (progressEvent: any) => void) => {
  return request.post<ToolFile>('/files/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 600000, // 10分钟超时，支持大文件上传
    onUploadProgress: onUploadProgress
  })
}

// 批量上传文件（支持进度回调，返回列表）
export const uploadFiles = (formData: FormData, onUploadProgress?: (progressEvent: any) => void) => {
  return request.post<ToolFile[]>('/files/uploads', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 600000,
    onUploadProgress
  })
}

// 删除文件
export const deleteFile = (id: number) => {
  return request.delete(`/files/${id}`)
}

// 删除文件夹（path 为 URL 风格前缀，如 platform/toolA/1.0.0 或 platform/toolA/1.0.0/linux_x64）
export const deleteFolder = (path: string) => {
  return request.delete('/files/folder', { params: { path } })
}

// 获取文件下载地址（通过ID）
export const getDownloadUrl = (id: number) => {
  return `/api/files/download/${id}`
}

/**
 * 文件名安全化处理，与后端保持一致
 * 移除或替换文件系统不支持的字符
 */
export const sanitizeFileName = (name: string): string => {
  if (!name) return 'unknown'
  return name
    .replace(/[\\/:*?"<>|]/g, '_')  // 替换特殊字符
    .replace(/\s+/g, '_')            // 替换空格
    .toLowerCase()                   // 转小写
}

/**
 * 获取文件下载地址（通过路径）
 * @param toolType 工具类型
 * @param toolName 工具名称（会自动进行安全化处理）
 * @param fileName 文件名
 */
export const getDownloadUrlByPath = (toolType: string, toolName: string, fileName: string) => {
  const sanitizedType = sanitizeFileName(toolType)
  const sanitizedName = sanitizeFileName(toolName)
  return `/api/files/download-by-path/${sanitizedType}/${sanitizedName}/${fileName}`
}

/**
 * 通过路径下载文件
 * @param toolType 工具类型
 * @param toolName 工具名称
 * @param fileName 文件名
 */
export const downloadFileByPath = (toolType: string, toolName: string, fileName: string) => {
  const url = getDownloadUrlByPath(toolType, toolName, fileName)
  window.open(url, '_blank')
}
