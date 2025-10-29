import { request } from '@/utils/request'
import type { ChangeLog, PageResponse } from '@/types'

// 根据工具ID查询变更日志
export const getLogsByToolId = (toolId: number) => {
  return request.get<ChangeLog[]>(`/changelogs/tool/${toolId}`)
}

// 分页查询变更日志
export const getLogPage = (params: { pageNum: number; pageSize: number; toolId?: number }) => {
  return request.get<PageResponse<ChangeLog>>('/changelogs/page', { params })
}

// 新增变更日志
export const addChangeLog = (data: ChangeLog) => {
  return request.post('/changelogs', data)
}

// 更新变更日志
export const updateChangeLog = (id: number, data: ChangeLog) => {
  return request.put(`/changelogs/${id}`, data)
}

// 删除变更日志
export const deleteChangeLog = (id: number) => {
  return request.delete(`/changelogs/${id}`)
}
