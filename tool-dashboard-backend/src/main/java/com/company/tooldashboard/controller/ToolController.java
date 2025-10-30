package com.company.tooldashboard.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.tooldashboard.annotation.RequireAdmin;
import com.company.tooldashboard.common.Result;
import com.company.tooldashboard.entity.Tool;
import com.company.tooldashboard.service.ToolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 工具控制器
 */
@RestController
@RequestMapping("/tools")
@Validated
public class ToolController {
    
    @Autowired
    private ToolService toolService;
    
    /**
     * 分页查询工具列表（公开接口）
     */
    @GetMapping("/page")
    public Result<Page<Tool>> getToolPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        Page<Tool> page = toolService.getToolPage(pageNum, pageSize, keyword);
        return Result.success(page);
    }
    
    /**
     * 获取所有工具列表（公开接口）
     */
    @GetMapping("/list")
    public Result<List<Tool>> getToolList() {
        List<Tool> list = toolService.list();
        return Result.success(list);
    }
    
    /**
     * 按类型分组查询工具列表（公开接口）
     */
    @GetMapping("/groups")
    public Result<List<com.company.tooldashboard.dto.ToolGroupDTO>> getToolsByGroup() {
        List<com.company.tooldashboard.dto.ToolGroupDTO> groups = toolService.getToolsByGroup();
        return Result.success(groups);
    }
    
    /**
     * 获取工具详情（公开接口）
     */
    @GetMapping("/{id}")
    public Result<Tool> getToolDetail(@PathVariable Long id) {
        Tool tool = toolService.getToolDetail(id);
        return Result.success(tool);
    }
    
    /**
     * 新增工具（需要管理员权限）
     */
    @RequireAdmin
    @PostMapping
    public Result<Void> addTool(@Valid @RequestBody Tool tool) {
        toolService.save(tool);
        return Result.success();
    }
    
    /**
     * 更新工具（需要管理员权限）
     */
    @RequireAdmin
    @PutMapping("/{id}")
    public Result<Void> updateTool(@PathVariable Long id, @Valid @RequestBody Tool tool) {
        tool.setId(id);
        toolService.updateById(tool);
        return Result.success();
    }
    
    /**
     * 删除工具（需要管理员权限）
     */
    @RequireAdmin
    @DeleteMapping("/{id}")
    public Result<Void> deleteTool(@PathVariable Long id) {
        toolService.removeById(id);
        return Result.success();
    }
    
    /**
     * 获取指定工具的最新版本号（公开接口）
     * 
     * @param toolName 工具名称
     * @return 最新版本号
     */
    @GetMapping("/latest-version")
    public Result<String> getLatestVersion(@RequestParam String toolName) {
        String latestVersion = toolService.getLatestVersion(toolName);
        if (latestVersion == null) {
            return Result.error("工具不存在或没有版本信息");
        }
        return Result.success(latestVersion);
    }
}
