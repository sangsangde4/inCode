import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '@/types'

// 创建axios实例
const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    // 添加token
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    console.error('请求错误：', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    // 如果是blob类型的响应（如文件下载、图片），直接返回完整响应
    if (response.config.responseType === 'blob') {
      return response
    }
    
    const res = response.data
    
    // 根据code判断
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      
      // 401: 未登录 → 带上当前地址做回跳
      if (res.code === 401) {
        localStorage.removeItem('token')
        try {
          const current = window.location.pathname + window.location.search + window.location.hash
          const redirect = encodeURIComponent(current)
          window.location.href = `/login?redirect=${redirect}`
        } catch {
          window.location.href = '/login'
        }
      }
      
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    
    return res
  },
  (error) => {
    console.error('响应错误：', error)
    // 处理 HTTP 401（如网关直返401或非标准返回体）
    const status = error?.response?.status
    if (status === 401) {
      localStorage.removeItem('token')
      try {
        const current = window.location.pathname + window.location.search + window.location.hash
        const redirect = encodeURIComponent(current)
        window.location.href = `/login?redirect=${redirect}`
      } catch {
        window.location.href = '/login'
      }
      return Promise.reject(error)
    }
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

// 导出请求方法
export const request = {
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return service.get(url, config)
  },
  
  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return service.post(url, data, config)
  },
  
  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return service.put(url, data, config)
  },
  
  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return service.delete(url, config)
  }
}

export default service
