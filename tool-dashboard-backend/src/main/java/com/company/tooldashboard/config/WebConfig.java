package com.company.tooldashboard.config;

import com.company.tooldashboard.interceptor.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Autowired
    private AuthInterceptor authInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                    // 认证相关
                    "/auth/**",
                    
                    // 文件访问（公开）
                    "/icon/**",              // 图标文件访问
                    "/files/tool/**",        // 根据工具ID查询文件
                    "/files/page",           // 分页查询文件
                    "/files/download/**",    // 文件下载
                    
                    // 工具相关查询（公开）
                    "/tools/list",
                    "/tools/page",
                    "/tools/groups",
                    "/tools/*",              // 工具详情 (GET /tools/{id})
                    
                    // 变更日志查询（公开）
                    "/changelogs/tool/**",
                    "/changelogs/page"
                );
    }
}
