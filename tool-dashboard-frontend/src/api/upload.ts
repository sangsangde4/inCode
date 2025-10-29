import { request } from '@/utils/request'

/**
 * 上传工具图标
 * @param file 图标文件
 * @param category 可选的分类名称（如工具名、平台名等），用于组织文件夹
 */
export const uploadIcon = (file: File, category?: string) => {
  const formData = new FormData()
  formData.append('file', file)
  if (category) {
    formData.append('category', category)
  }
  
  return request.post<{ 
    url: string
    fileName: string
    originalName: string
    relativePath: string
  }>(
    '/upload/icon',
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    }
  )
}
