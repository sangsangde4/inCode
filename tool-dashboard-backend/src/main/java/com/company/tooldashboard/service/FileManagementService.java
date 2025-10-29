package com.company.tooldashboard.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.company.tooldashboard.entity.ToolFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件管理服务接口
 * 负责工具文件的业务逻辑处理
 * 
 * 注：图标文件上传逻辑较简单，直接在FileManagementController层处理
 */
public interface FileManagementService extends IService<ToolFile> {
    
    /**
     * 根据工具ID查询文件列表
     */
    List<ToolFile> getFilesByToolId(Long toolId);
    
    /**
     * 分页查询文件列表
     */
    Page<ToolFile> getFilePage(Integer pageNum, Integer pageSize, Long toolId);
    
    /**
     * 上传文件
     */
    ToolFile uploadFile(MultipartFile file, Long toolId, String version, String description, String uploader);
    
    /**
     * 增加下载次数
     */
    void increaseDownloadCount(Long fileId);
}
