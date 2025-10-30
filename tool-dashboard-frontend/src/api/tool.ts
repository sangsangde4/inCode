import { request } from '@/utils/request'
import type { Tool, PageResponse, ToolGroup } from '@/types'

// 分页查询工具列表
export const getToolPage = (params: { pageNum: number; pageSize: number; keyword?: string }) => {
  return request.get<PageResponse<Tool>>('/tools/page', { params })
}

// 获取所有工具列表
export const getToolList = () => {
  return request.get<Tool[]>('/tools/list')
}

// 按类型分组查询工具列表
export const getToolsByGroup = () => {
  return request.get<ToolGroup[]>('/tools/groups')
}

// 获取工具详情
export const getToolDetail = (id: number) => {
  return request.get<Tool>(`/tools/${id}`)
}

// 新增工具
export const addTool = (data: Tool) => {
  return request.post('/tools', data)
}

// 更新工具
export const updateTool = (id: number, data: Tool) => {
  return request.put(`/tools/${id}`, data)
}

// 删除工具
export const deleteTool = (id: number) => {
  return request.delete(`/tools/${id}`)
}

// 获取指定工具的最新版本号
export const getLatestVersion = (toolName: string) => {
  return request.get<string>('/tools/latest-version', { params: { toolName } })
}
