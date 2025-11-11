package com.company.tooldashboard.interceptor;

import com.company.tooldashboard.annotation.RequireAdmin;
import com.company.tooldashboard.service.TokenBlacklistService;
import com.company.tooldashboard.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 权限拦截器
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthInterceptor.class);
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private TokenBlacklistService tokenBlacklistService;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // OPTIONS 请求直接放行（CORS 预检）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        
        // 如果不是方法处理器，直接放行
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        
        // 检查方法是否有 @RequireAdmin 注解
        RequireAdmin requireAdmin = handlerMethod.getMethodAnnotation(RequireAdmin.class);
        if (requireAdmin == null) {
            // 没有注解，直接放行
            return true;
        }
        
        // 验证 Authorization 头
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            logger.warn("未授权访问: {} {}", request.getMethod(), request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录或登录已过期\",\"data\":null}");
            return false;
        }
        
        // 去掉 "Bearer " 前缀
        token = token.substring(7);
        
        // 检查 token 是否已失效
        if (tokenBlacklistService.isBlacklisted(token)) {
            logger.warn("使用已失效的token访问: {} {}", request.getMethod(), request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"账号已在其他地方登出，请重新登录\",\"data\":null}");
            return false;
        }
        
        // 验证 token 有效性
        if (!jwtUtil.validateToken(token)) {
            logger.warn("token验证失败: {} {}", request.getMethod(), request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"登录已过期，请重新登录\",\"data\":null}");
            return false;
        }
        
        // 将用户名存入请求属性，供后续使用
        String username = jwtUtil.getUsernameFromToken(token);
        request.setAttribute("username", username);
        
        return true;
    }
}
