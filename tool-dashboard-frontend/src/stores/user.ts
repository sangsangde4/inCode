import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { LoginRequest, LoginResponse } from '@/types'
import { login as loginApi } from '@/api/auth'

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
  const logout = () => {
    token.value = ''
    username.value = ''
    realName.value = ''
    
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('realName')
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
