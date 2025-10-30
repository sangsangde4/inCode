// 工具类型
export interface Tool {
  id?: number
  name: string
  description?: string
  type?: string
  iconUrl?: string
  accessUrl?: string
  currentVersion?: string
  owner?: string
  status?: number
  sortOrder?: number
  createTime?: string
  updateTime?: string
}

// 工具文件类型
export interface ToolFile {
  id?: number
  toolId: number
  fileName: string
  originalName: string
  filePath: string
  fileSize?: number
  fileType?: string
  version?: string
  architecture?: string            // 架构类型（如：linux_x64, windows_x64等）
  downloadCount?: number
  description?: string
  uploader?: string
  createTime?: string
  updateTime?: string
  // 下载URL（后端自动生成）
  downloadUrl?: string              // 通过ID下载
  downloadUrlByPath?: string        // 通过路径下载（推荐）
}

// 变更日志类型
export interface ChangeLog {
  id?: number
  toolId: number
  version?: string
  changeType?: string
  content?: string
  changer?: string
  changeTime?: string
  createTime?: string
  updateTime?: string
}

// 管理员类型
export interface Admin {
  id?: number
  username: string
  password?: string
  realName?: string
  email?: string
  status?: number
  lastLoginTime?: string
  createTime?: string
  updateTime?: string
}

// 登录请求
export interface LoginRequest {
  username: string
  password: string
}

// 登录响应
export interface LoginResponse {
  token: string
  username: string
  realName: string
}

// 工具分组类型
export interface ToolGroup {
  type: string
  typeName: string
  tools: Tool[]
  count: number
}

// 通用响应
export interface ApiResponse<T = any> {
  code: number
  message: string
  data?: T
}

// 分页响应
export interface PageResponse<T = any> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}
