package com.company.tooldashboard.controller;

import com.company.tooldashboard.annotation.RequireAdmin;
import com.company.tooldashboard.common.Result;
import com.company.tooldashboard.dto.LoginRequest;
import com.company.tooldashboard.dto.LoginResponse;
import com.company.tooldashboard.service.AdminService;
import com.company.tooldashboard.service.TokenBlacklistService;
import com.company.tooldashboard.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/auth")
public class AuthController {
    
    @Autowired
    private AdminService adminService;
    
    @Autowired
    private TokenBlacklistService tokenBlacklistService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    /**
     * 管理员登录
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Validated @RequestBody LoginRequest request) {
        try {
            LoginResponse response = adminService.login(request);
            return Result.success(response);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 管理员登出
     */
    @RequireAdmin
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            // 提取token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Result.error("无效的Authorization头");
            }
            
            String token = authHeader.substring(7);
            
            // 获取token的过期时间
            Long expirationTime = jwtUtil.getExpirationTime(token);
            if (expirationTime == null) {
                return Result.error("无效的token");
            }
            
            // 将token加入黑名单
            tokenBlacklistService.addToBlacklist(token, expirationTime);
            
            return Result.success("登出成功", null);
        } catch (Exception e) {
            return Result.error("登出失败: " + e.getMessage());
        }
    }
}
