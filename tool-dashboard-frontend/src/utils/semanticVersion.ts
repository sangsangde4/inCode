/**
 * 语义化版本工具类
 * 遵循语义化版本规范 2.0.0 (https://semver.org/)
 * 版本号格式：MAJOR.MINOR.PATCH[-PRERELEASE][+BUILD]
 * 例如：1.0.0, 1.2.3, 2.0.0-alpha, 1.0.0-beta.1+20130313144700
 */

/**
 * 语义化版本正则表达式
 * 格式：MAJOR.MINOR.PATCH[-PRERELEASE][+BUILD]
 */
const SEMVER_REGEX = /^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?$/

/**
 * 版本号各部分
 */
interface VersionParts {
  major: number
  minor: number
  patch: number
  preRelease?: string
}

/**
 * 校验版本号是否符合语义化版本规范
 * 
 * @param version 版本号字符串
 * @returns true 如果版本号符合规范，否则返回 false
 */
export function isValidSemanticVersion(version: string | null | undefined): boolean {
  if (!version || typeof version !== 'string') {
    return false
  }
  return SEMVER_REGEX.test(version.trim())
}

/**
 * 解析版本号字符串
 */
function parseVersion(version: string): VersionParts {
  let cleanVersion = version.trim()
  
  // 移除构建元数据（+后面的部分）
  const plusIndex = cleanVersion.indexOf('+')
  if (plusIndex > 0) {
    cleanVersion = cleanVersion.substring(0, plusIndex)
  }
  
  // 分离预发布版本号
  let preRelease: string | undefined
  const dashIndex = cleanVersion.indexOf('-')
  if (dashIndex > 0) {
    preRelease = cleanVersion.substring(dashIndex + 1)
    cleanVersion = cleanVersion.substring(0, dashIndex)
  }
  
  // 解析主版本号、次版本号、修订号
  const parts = cleanVersion.split('.')
  const major = parseInt(parts[0], 10)
  const minor = parseInt(parts[1], 10)
  const patch = parseInt(parts[2], 10)
  
  return { major, minor, patch, preRelease }
}

/**
 * 比较两个语义化版本号的大小
 * 
 * @param version1 版本号1
 * @param version2 版本号2
 * @returns 正数表示 version1 > version2，0表示相等，负数表示 version1 < version2
 * @throws Error 如果版本号格式不正确
 */
export function compareVersions(version1: string, version2: string): number {
  if (!isValidSemanticVersion(version1)) {
    throw new Error(`Invalid semantic version: ${version1}`)
  }
  if (!isValidSemanticVersion(version2)) {
    throw new Error(`Invalid semantic version: ${version2}`)
  }
  
  // 解析版本号
  const v1 = parseVersion(version1)
  const v2 = parseVersion(version2)
  
  // 比较主版本号
  if (v1.major !== v2.major) {
    return v1.major - v2.major
  }
  
  // 比较次版本号
  if (v1.minor !== v2.minor) {
    return v1.minor - v2.minor
  }
  
  // 比较修订号
  if (v1.patch !== v2.patch) {
    return v1.patch - v2.patch
  }
  
  // 比较预发布版本号
  // 没有预发布版本号的版本优先级更高
  if (!v1.preRelease && !v2.preRelease) {
    return 0
  }
  if (!v1.preRelease) {
    return 1
  }
  if (!v2.preRelease) {
    return -1
  }
  
  // 比较预发布版本号字符串
  return v1.preRelease.localeCompare(v2.preRelease)
}

/**
 * 获取版本号说明信息
 * 
 * @param version 版本号
 * @returns 版本号说明
 */
export function getVersionDescription(version: string): string {
  if (!isValidSemanticVersion(version)) {
    return '无效的版本号格式'
  }
  
  const parts = parseVersion(version)
  let desc = `主版本: ${parts.major}, 次版本: ${parts.minor}, 修订号: ${parts.patch}`
  
  if (parts.preRelease) {
    desc += `, 预发布: ${parts.preRelease}`
  }
  
  return desc
}

/**
 * 生成版本号校验规则（适用于 Element Plus 等表单验证）
 * 
 * @param required 是否必填
 * @returns 表单验证规则
 */
export function createVersionValidationRule(required = true) {
  return {
    required,
    validator: (_rule: any, value: string, callback: (error?: Error) => void) => {
      if (!value || value.trim() === '') {
        if (required) {
          callback(new Error('请输入版本号'))
        } else {
          callback()
        }
        return
      }
      
      if (!isValidSemanticVersion(value)) {
        callback(new Error('版本号格式不正确，必须符合语义化版本规范（如：1.0.0）'))
      } else {
        callback()
      }
    },
    trigger: 'blur'
  }
}
