package com.company.tooldashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.tooldashboard.dto.ToolGroupDTO;
import com.company.tooldashboard.entity.Tool;
import com.company.tooldashboard.mapper.ToolMapper;
import com.company.tooldashboard.service.ToolService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 工具服务实现类
 */
@Service
public class ToolServiceImpl extends ServiceImpl<ToolMapper, Tool> implements ToolService {
    
    @Override
    public Page<Tool> getToolPage(Integer pageNum, Integer pageSize, String keyword) {
        Page<Tool> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Tool> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Tool::getName, keyword)
                    .or()
                    .like(Tool::getDescription, keyword);
        }
        
        wrapper.orderByAsc(Tool::getSortOrder)
                .orderByDesc(Tool::getCreateTime);
        
        return this.page(page, wrapper);
    }
    
    @Override
    public Tool getToolDetail(Long id) {
        return this.getById(id);
    }
    
    @Override
    public List<ToolGroupDTO> getToolsByGroup() {
        // 查询所有工具
        LambdaQueryWrapper<Tool> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Tool::getSortOrder)
                .orderByDesc(Tool::getCreateTime);
        List<Tool> allTools = this.list(wrapper);
        
        // 按类型分组
        Map<String, List<Tool>> groupMap = allTools.stream()
                .collect(Collectors.groupingBy(
                        tool -> StringUtils.hasText(tool.getType()) ? tool.getType() : "其他",
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        
        // 类型显示名称映射
        Map<String, String> typeNameMap = new HashMap<>();
        typeNameMap.put("平台", "平台");
        typeNameMap.put("工具", "工具");
        typeNameMap.put("系统", "系统");
        typeNameMap.put("其他", "其他");
        
        // 转换为DTO列表
        List<ToolGroupDTO> result = new ArrayList<>();
        for (Map.Entry<String, List<Tool>> entry : groupMap.entrySet()) {
            String type = entry.getKey();
            List<Tool> tools = entry.getValue();
            
            ToolGroupDTO dto = new ToolGroupDTO();
            dto.setType(type);
            dto.setTypeName(typeNameMap.getOrDefault(type, type));
            dto.setTools(tools);
            dto.setCount(tools.size());
            
            result.add(dto);
        }
        
        return result;
    }
}
