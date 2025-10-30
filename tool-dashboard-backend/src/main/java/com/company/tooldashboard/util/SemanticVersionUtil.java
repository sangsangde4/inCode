package com.company.tooldashboard.util;

import java.util.regex.Pattern;

/**
 * 语义化版本工具类
 * 遵循语义化版本规范 2.0.0 (https://semver.org/)
 * 版本号格式：MAJOR.MINOR.PATCH[-PRERELEASE][+BUILD]
 * 例如：1.0.0, 1.2.3, 2.0.0-alpha, 1.0.0-beta.1+20130313144700
 */
public class SemanticVersionUtil {
    
    /**
     * 语义化版本正则表达式
     * 格式：MAJOR.MINOR.PATCH[-PRERELEASE][+BUILD]
     */
    private static final String SEMVER_REGEX = 
        "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)" +
        "(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)" +
        "(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?" +
        "(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$";
    
    private static final Pattern SEMVER_PATTERN = Pattern.compile(SEMVER_REGEX);
    
    /**
     * 校验版本号是否符合语义化版本规范
     * 
     * @param version 版本号字符串
     * @return true 如果版本号符合规范，否则返回 false
     */
    public static boolean isValidSemanticVersion(String version) {
        if (version == null || version.trim().isEmpty()) {
            return false;
        }
        return SEMVER_PATTERN.matcher(version.trim()).matches();
    }
    
    /**
     * 比较两个语义化版本号的大小
     * 
     * @param version1 版本号1
     * @param version2 版本号2
     * @return 正数表示 version1 > version2，0表示相等，负数表示 version1 < version2
     * @throws IllegalArgumentException 如果版本号格式不正确
     */
    public static int compareVersions(String version1, String version2) {
        if (!isValidSemanticVersion(version1)) {
            throw new IllegalArgumentException("Invalid semantic version: " + version1);
        }
        if (!isValidSemanticVersion(version2)) {
            throw new IllegalArgumentException("Invalid semantic version: " + version2);
        }
        
        // 解析版本号
        VersionParts v1 = parseVersion(version1);
        VersionParts v2 = parseVersion(version2);
        
        // 比较主版本号
        int majorCompare = Integer.compare(v1.major, v2.major);
        if (majorCompare != 0) {
            return majorCompare;
        }
        
        // 比较次版本号
        int minorCompare = Integer.compare(v1.minor, v2.minor);
        if (minorCompare != 0) {
            return minorCompare;
        }
        
        // 比较修订号
        int patchCompare = Integer.compare(v1.patch, v2.patch);
        if (patchCompare != 0) {
            return patchCompare;
        }
        
        // 比较预发布版本号
        // 没有预发布版本号的版本优先级更高
        if (v1.preRelease == null && v2.preRelease == null) {
            return 0;
        }
        if (v1.preRelease == null) {
            return 1;
        }
        if (v2.preRelease == null) {
            return -1;
        }
        
        // 比较预发布版本号字符串
        return v1.preRelease.compareTo(v2.preRelease);
    }
    
    /**
     * 解析版本号字符串
     */
    private static VersionParts parseVersion(String version) {
        String cleanVersion = version.trim();
        
        // 移除构建元数据（+后面的部分）
        int plusIndex = cleanVersion.indexOf('+');
        if (plusIndex > 0) {
            cleanVersion = cleanVersion.substring(0, plusIndex);
        }
        
        // 分离预发布版本号
        String preRelease = null;
        int dashIndex = cleanVersion.indexOf('-');
        if (dashIndex > 0) {
            preRelease = cleanVersion.substring(dashIndex + 1);
            cleanVersion = cleanVersion.substring(0, dashIndex);
        }
        
        // 解析主版本号、次版本号、修订号
        String[] parts = cleanVersion.split("\\.");
        int major = Integer.parseInt(parts[0]);
        int minor = Integer.parseInt(parts[1]);
        int patch = Integer.parseInt(parts[2]);
        
        return new VersionParts(major, minor, patch, preRelease);
    }
    
    /**
     * 版本号各部分
     */
    private static class VersionParts {
        int major;
        int minor;
        int patch;
        String preRelease;
        
        VersionParts(int major, int minor, int patch, String preRelease) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
            this.preRelease = preRelease;
        }
    }
    
    /**
     * 获取版本号说明信息
     * 
     * @param version 版本号
     * @return 版本号说明
     */
    public static String getVersionDescription(String version) {
        if (!isValidSemanticVersion(version)) {
            return "无效的版本号格式";
        }
        
        VersionParts parts = parseVersion(version);
        StringBuilder desc = new StringBuilder();
        desc.append("主版本: ").append(parts.major)
            .append(", 次版本: ").append(parts.minor)
            .append(", 修订号: ").append(parts.patch);
        
        if (parts.preRelease != null) {
            desc.append(", 预发布: ").append(parts.preRelease);
        }
        
        return desc.toString();
    }
}
