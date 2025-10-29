import { defineStore } from 'pinia'
import { ref } from 'vue'

export type ThemeType = 'light' | 'dark'

export const useThemeStore = defineStore('theme', () => {
  // 从localStorage读取主题，默认为黑橙主题
  const theme = ref<ThemeType>((localStorage.getItem('theme') as ThemeType) || 'dark')
  
  // 切换主题
  const toggleTheme = () => {
    theme.value = theme.value === 'light' ? 'dark' : 'light'
    saveTheme()
    applyTheme()
  }
  
  // 设置主题
  const setTheme = (newTheme: ThemeType) => {
    theme.value = newTheme
    saveTheme()
    applyTheme()
  }
  
  // 保存到localStorage
  const saveTheme = () => {
    localStorage.setItem('theme', theme.value)
  }
  
  // 应用主题到DOM
  const applyTheme = () => {
    document.documentElement.setAttribute('data-theme', theme.value)
    
    // 同时添加class以兼容某些组件
    if (theme.value === 'dark') {
      document.documentElement.classList.add('dark-theme')
      document.documentElement.classList.remove('light-theme')
    } else {
      document.documentElement.classList.add('light-theme')
      document.documentElement.classList.remove('dark-theme')
    }
  }
  
  // 初始化主题
  const initTheme = () => {
    applyTheme()
  }
  
  return {
    theme,
    toggleTheme,
    setTheme,
    initTheme
  }
})
