package com.company.tooldashboard.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.tooldashboard.dto.LoginRequest;
import com.company.tooldashboard.dto.LoginResponse;
import com.company.tooldashboard.entity.Admin;

/**
 * 管理员服务接口
 */
public interface AdminService extends IService<Admin> {
    
    /**
     * 管理员登录
     */
    LoginResponse login(LoginRequest request);
    
    /**
     * 根据用户名查询管理员
     */
    Admin getByUsername(String username);
}
