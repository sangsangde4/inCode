package com.company.tooldashboard.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token黑名单服务
 * 用于管理已失效的token，防止退出登录后token仍然有效的安全问题
 */
@Service
public class TokenBlacklistService {
    
    /**
     * 黑名单存储
     * key: token
     * value: 过期时间戳（毫秒）
     */
    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();
    
    /**
     * 将token加入黑名单
     * @param token JWT token
     * @param expirationTime token的过期时间戳（毫秒）
     */
    public void addToBlacklist(String token, Long expirationTime) {
        blacklist.put(token, expirationTime);
        // 清理过期的黑名单记录
        cleanExpiredTokens();
    }
    
    /**
     * 检查token是否在黑名单中
     * @param token JWT token
     * @return true表示在黑名单中，false表示不在
     */
    public boolean isBlacklisted(String token) {
        Long expirationTime = blacklist.get(token);
        if (expirationTime == null) {
            return false;
        }
        
        // 如果已过期，从黑名单中移除并返回false
        if (System.currentTimeMillis() > expirationTime) {
            blacklist.remove(token);
            return false;
        }
        
        return true;
    }
    
    /**
     * 清理已过期的token记录
     * 减少内存占用
     */
    private void cleanExpiredTokens() {
        long currentTime = System.currentTimeMillis();
        blacklist.entrySet().removeIf(entry -> entry.getValue() < currentTime);
    }
}
