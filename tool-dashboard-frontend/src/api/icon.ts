import request from '@/utils/request'

/**
 * 获取图标数据
 */
export const getIconData = (path: string) => {
  return request({
    url: `/icon/${path}`,
    method: 'get',
    responseType: 'blob'
  })
}

/**
 * 将blob转换为base64
 */
export const blobToBase64 = (blob: Blob): Promise<string> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onloadend = () => resolve(reader.result as string)
    reader.onerror = reject
    reader.readAsDataURL(blob)
  })
}

/**
 * 获取图标的base64数据
 */
export const getIconBase64 = async (url: string): Promise<string> => {
  try {
    // 如果是完整URL，直接返回
    if (url.startsWith('http://') || url.startsWith('https://')) {
      return url
    }
    
    // 提取相对路径
    // 数据库存储的格式是: /files/icons/2025/10/27/xxx.png
    // 需要转换为: icons/2025/10/27/xxx.png (去掉/files/)
    let relativePath = url
    if (url.startsWith('/files/')) {
      relativePath = url.substring(7)
    } else if (url.startsWith('/icon/')) {
      relativePath = url.substring(6)
    }
    
    // 调用接口获取blob数据
    const response = await getIconData(relativePath)
    
    // 转换为base64
    const base64 = await blobToBase64(response.data)
    return base64
    
  } catch (error) {
    console.error('[Icon API] 获取图标失败:', error)
    return ''
  }
}
