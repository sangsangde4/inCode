package com.company.tooldashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.tooldashboard.entity.ChangeLog;
import com.company.tooldashboard.mapper.ChangeLogMapper;
import com.company.tooldashboard.service.ChangeLogService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 变更日志服务实现类
 */
@Service
public class ChangeLogServiceImpl extends ServiceImpl<ChangeLogMapper, ChangeLog> implements ChangeLogService {
    
    @Override
    public List<ChangeLog> getLogsByToolId(Long toolId) {
        LambdaQueryWrapper<ChangeLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChangeLog::getToolId, toolId)
                .orderByDesc(ChangeLog::getChangeTime);
        return this.list(wrapper);
    }
    
    @Override
    public Page<ChangeLog> getLogPage(Integer pageNum, Integer pageSize, Long toolId) {
        Page<ChangeLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ChangeLog> wrapper = new LambdaQueryWrapper<>();
        
        if (toolId != null) {
            wrapper.eq(ChangeLog::getToolId, toolId);
        }
        
        wrapper.orderByDesc(ChangeLog::getChangeTime);
        return this.page(page, wrapper);
    }
}
