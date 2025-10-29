package com.company.tooldashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.tooldashboard.dto.LoginRequest;
import com.company.tooldashboard.dto.LoginResponse;
import com.company.tooldashboard.entity.Admin;
import com.company.tooldashboard.mapper.AdminMapper;
import com.company.tooldashboard.service.AdminService;
import com.company.tooldashboard.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;

/**
 * 管理员服务实现类
 */
@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminService {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Override
    public LoginResponse login(LoginRequest request) {
        // 查询管理员
        Admin admin = getByUsername(request.getUsername());
        if (admin == null) {
            throw new RuntimeException("用户名或密码错误");
        }
        
        // 验证密码（这里使用MD5加密，实际应使用BCrypt）
        String encryptedPassword = DigestUtils.md5DigestAsHex(request.getPassword().getBytes());
        if (!encryptedPassword.equals(admin.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }
        
        // 检查账号状态
        if (admin.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用");
        }
        
        // 更新最后登录时间
        admin.setLastLoginTime(LocalDateTime.now());
        this.updateById(admin);
        
        // 生成Token
        String token = jwtUtil.generateToken(admin.getUsername());
        
        return new LoginResponse(token, admin.getUsername(), admin.getRealName());
    }
    
    @Override
    public Admin getByUsername(String username) {
        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Admin::getUsername, username);
        return this.getOne(wrapper);
    }
}
