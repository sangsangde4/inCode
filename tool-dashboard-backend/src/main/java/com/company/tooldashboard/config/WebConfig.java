package com.company.tooldashboard.config;

import com.company.tooldashboard.interceptor.AuthInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

/**
 * Web配置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    private static final Logger logger = LoggerFactory.getLogger(WebConfig.class);
    
    @Autowired
    private AuthInterceptor authInterceptor;
    
    /**
     * 公开访问路径配置（无需权限验证）
     * 
     * 配置原则：
     * 1. 只配置完全公开的路径
     * 2. 对于同一路径下有公开和需要权限的操作，在 Controller 方法上用 @RequireAdmin 区分
     * 3. 不要使用过于宽泛的通配符（如 /tools/*），避免误排除需要权限的操作
     */
    private static final List<String> PUBLIC_PATTERNS = Arrays.asList(
        // 认证
        "/auth/login",
        
        // 文件访问
        "/icon/**",
        "/files/tool/**",
        "/files/page",
        "/files/download/**",
        "/files/download-by-path/**",
        
        // 工具查询
        "/tools/list",
        "/tools/page",
        "/tools/groups",
        
        // 变更日志查询
        "/changelogs/tool/**",
        "/changelogs/page"
    );
    
    @PostConstruct
    public void init() {
        logger.info("权限拦截器配置: 拦截所有请求 (/**), 公开路径: {} 个", PUBLIC_PATTERNS.size());
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(PUBLIC_PATTERNS.toArray(new String[0]));
        
        logger.info("✓ AuthInterceptor 已注册");
    }
}
