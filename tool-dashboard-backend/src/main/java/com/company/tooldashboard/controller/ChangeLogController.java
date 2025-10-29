package com.company.tooldashboard.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.tooldashboard.annotation.RequireAdmin;
import com.company.tooldashboard.common.Result;
import com.company.tooldashboard.entity.ChangeLog;
import com.company.tooldashboard.service.ChangeLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 变更日志控制器
 */
@RestController
@RequestMapping("/changelogs")
public class ChangeLogController {
    
    @Autowired
    private ChangeLogService changeLogService;
    
    /**
     * 根据工具ID查询变更日志（公开接口）
     */
    @GetMapping("/tool/{toolId}")
    public Result<List<ChangeLog>> getLogsByToolId(@PathVariable Long toolId) {
        List<ChangeLog> logs = changeLogService.getLogsByToolId(toolId);
        return Result.success(logs);
    }
    
    /**
     * 分页查询变更日志（公开接口）
     */
    @GetMapping("/page")
    public Result<Page<ChangeLog>> getLogPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long toolId) {
        Page<ChangeLog> page = changeLogService.getLogPage(pageNum, pageSize, toolId);
        return Result.success(page);
    }
    
    /**
     * 新增变更日志（需要管理员权限）
     */
    @RequireAdmin
    @PostMapping
    public Result<Void> addChangeLog(@RequestBody ChangeLog changeLog) {
        changeLogService.save(changeLog);
        return Result.success();
    }
    
    /**
     * 更新变更日志（需要管理员权限）
     */
    @RequireAdmin
    @PutMapping("/{id}")
    public Result<Void> updateChangeLog(@PathVariable Long id, @RequestBody ChangeLog changeLog) {
        changeLog.setId(id);
        changeLogService.updateById(changeLog);
        return Result.success();
    }
    
    /**
     * 删除变更日志（需要管理员权限）
     */
    @RequireAdmin
    @DeleteMapping("/{id}")
    public Result<Void> deleteChangeLog(@PathVariable Long id) {
        changeLogService.removeById(id);
        return Result.success();
    }
}
