package com.company.tooldashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.tooldashboard.dto.ToolGroupDTO;
import com.company.tooldashboard.entity.Tool;
import com.company.tooldashboard.entity.ToolFile;
import com.company.tooldashboard.mapper.ToolFileMapper;
import com.company.tooldashboard.mapper.ToolMapper;
import com.company.tooldashboard.service.ToolService;
import com.company.tooldashboard.util.SemanticVersionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 工具服务实现类
 */
@Service
public class ToolServiceImpl extends ServiceImpl<ToolMapper, Tool> implements ToolService {
    
    @Autowired
    private ToolFileMapper toolFileMapper;
    
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
    
    @Override
    public String getLatestVersion(String toolName) {
        if (!StringUtils.hasText(toolName)) {
            return null;
        }
        
        // 根据工具名称查询工具
        LambdaQueryWrapper<Tool> toolWrapper = new LambdaQueryWrapper<>();
        toolWrapper.eq(Tool::getName, toolName);
        Tool tool = this.getOne(toolWrapper);
        
        if (tool == null) {
            return null;
        }
        
        // 查询该工具的所有文件版本
        LambdaQueryWrapper<ToolFile> fileWrapper = new LambdaQueryWrapper<>();
        fileWrapper.eq(ToolFile::getToolId, tool.getId())
                   .isNotNull(ToolFile::getVersion);
        List<ToolFile> toolFiles = toolFileMapper.selectList(fileWrapper);
        
        if (toolFiles == null || toolFiles.isEmpty()) {
            // 如果没有文件版本，返回工具当前版本
            return tool.getCurrentVersion();
        }
        
        // 找出所有有效的版本号，并排序找到最新版本
        String latestVersion = null;
        for (ToolFile file : toolFiles) {
            String version = file.getVersion();
            if (version != null && SemanticVersionUtil.isValidSemanticVersion(version)) {
                if (latestVersion == null) {
                    latestVersion = version;
                } else if (SemanticVersionUtil.compareVersions(version, latestVersion) > 0) {
                    latestVersion = version;
                }
            }
        }
        
        // 如果找到了文件中的最新版本，返回它；否则返回工具的当前版本
        return latestVersion != null ? latestVersion : tool.getCurrentVersion();
    }

    @Override
    public List<String> getAllVersions(String toolName) {
        if (!StringUtils.hasText(toolName)) {
            return Collections.emptyList();
        }
        // 查工具
        LambdaQueryWrapper<Tool> toolWrapper = new LambdaQueryWrapper<>();
        toolWrapper.eq(Tool::getName, toolName);
        Tool tool = this.getOne(toolWrapper);
        if (tool == null) {
            return Collections.emptyList();
        }
        // 查文件版本
        LambdaQueryWrapper<ToolFile> fileWrapper = new LambdaQueryWrapper<>();
        fileWrapper.eq(ToolFile::getToolId, tool.getId())
                   .isNotNull(ToolFile::getVersion);
        List<ToolFile> toolFiles = toolFileMapper.selectList(fileWrapper);

        // 使用 TreeSet + 语义化版本比较器，按版本从高到低排序并去重
        Set<String> versions = new TreeSet<>((a, b) -> {
            if (a == null && b == null) return 0;
            if (a == null) return 1;
            if (b == null) return -1;
            if (SemanticVersionUtil.isValidSemanticVersion(a) && SemanticVersionUtil.isValidSemanticVersion(b)) {
                // 高版本排在前面
                return -Integer.signum(SemanticVersionUtil.compareVersions(a, b));
            }
            return a.compareTo(b);
        });

        if (toolFiles != null) {
            for (ToolFile f : toolFiles) {
                String v = f.getVersion();
                if (SemanticVersionUtil.isValidSemanticVersion(v)) {
                    versions.add(v);
                }
            }
        }

        // 如无文件版本但工具有当前版本，则返回当前版本
        if (versions.isEmpty() && StringUtils.hasText(tool.getCurrentVersion())) {
            versions.add(tool.getCurrentVersion());
        }

        return new ArrayList<>(versions);
    }
}
