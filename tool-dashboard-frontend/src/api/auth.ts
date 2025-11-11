import { request } from '@/utils/request'
import type { LoginRequest, LoginResponse } from '@/types'


// 管理员登录
export const login = (data: LoginRequest) => {
  return request.post<LoginResponse>('/auth/login', data)
}

// 管理员登出
export const logout = () => {
  return request.post<void>('/auth/logout')
}
