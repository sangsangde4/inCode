package com.company.tooldashboard.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.company.tooldashboard.entity.ChangeLog;

import java.util.List;

/**
 * 变更日志服务接口
 */
public interface ChangeLogService extends IService<ChangeLog> {
    
    /**
     * 根据工具ID查询变更日志
     */
    List<ChangeLog> getLogsByToolId(Long toolId);
    
    /**
     * 分页查询变更日志
     */
    Page<ChangeLog> getLogPage(Integer pageNum, Integer pageSize, Long toolId);
}
