import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { LoginRequest, LoginResponse } from '@/types'
import { login as loginApi, logout as logoutApi } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const username = ref<string>(localStorage.getItem('username') || '')
  const realName = ref<string>(localStorage.getItem('realName') || '')
  
  // 登录
  const login = async (data: LoginRequest) => {
    const res = await loginApi(data)
    if (res.data) {
      token.value = res.data.token
      username.value = res.data.username
      realName.value = res.data.realName
      
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('username', res.data.username)
      localStorage.setItem('realName', res.data.realName)
    }
    return res
  }
  
  // 登出
  const logout = async () => {
    try {
      // 调用后端登出接口，将token加入黑名单
      await logoutApi()
    } catch (error) {
      // 即使后端调用失败，也清除本地状态
      console.error('登出接口调用失败:', error)
    } finally {
      // 清除本地状态
      token.value = ''
      username.value = ''
      realName.value = ''
      
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      localStorage.removeItem('realName')
    }
  }
  
  // 是否已登录
  const isLoggedIn = () => {
    return !!token.value
  }
  
  return {
    token,
    username,
    realName,
    login,
    logout,
    isLoggedIn
  }
})
