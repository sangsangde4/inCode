package com.company.tooldashboard.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.company.tooldashboard.dto.ToolGroupDTO;
import com.company.tooldashboard.entity.Tool;

import java.util.List;

/**
 * 工具服务接口
 */
public interface ToolService extends IService<Tool> {
    
    /**
     * 分页查询工具列表
     */
    Page<Tool> getToolPage(Integer pageNum, Integer pageSize, String keyword);
    
    /**
     * 获取工具详情
     */
    Tool getToolDetail(Long id);
    
    /**
     * 按类型分组查询工具列表
     */
    List<ToolGroupDTO> getToolsByGroup();
    
    /**
     * 获取指定工具的最新版本号
     * 
     * @param toolName 工具名称
     * @return 最新版本号，如果工具不存在或没有版本信息则返回null
     */
    String getLatestVersion(String toolName);
}
