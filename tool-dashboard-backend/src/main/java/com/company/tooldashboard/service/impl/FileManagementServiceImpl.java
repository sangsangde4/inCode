package com.company.tooldashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.tooldashboard.entity.Tool;
import com.company.tooldashboard.entity.ToolFile;
import com.company.tooldashboard.mapper.ToolFileMapper;
import com.company.tooldashboard.service.FileManagementService;
import com.company.tooldashboard.service.ToolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 文件管理服务实现类
 * 负责工具文件的业务逻辑处理，包括上传、查询、下载计数等
 */
@Service
public class FileManagementServiceImpl extends ServiceImpl<ToolFileMapper, ToolFile> implements FileManagementService {
    
    private static final Logger logger = LoggerFactory.getLogger(FileManagementServiceImpl.class);
    
    @Value("${file.upload-path}")
    private String uploadPath;
    
    @Autowired
    private ToolService toolService;
    
    @Override
    public List<ToolFile> getFilesByToolId(Long toolId) {
        LambdaQueryWrapper<ToolFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ToolFile::getToolId, toolId)
                .orderByDesc(ToolFile::getCreateTime);
        List<ToolFile> files = this.list(wrapper);
        // 填充下载URL
        files.forEach(this::fillDownloadUrls);
        return files;
    }
    
    @Override
    public Page<ToolFile> getFilePage(Integer pageNum, Integer pageSize, Long toolId) {
        Page<ToolFile> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ToolFile> wrapper = new LambdaQueryWrapper<>();
        
        if (toolId != null) {
            wrapper.eq(ToolFile::getToolId, toolId);
        }
        
        wrapper.orderByDesc(ToolFile::getCreateTime);
        Page<ToolFile> result = this.page(page, wrapper);
        // 填充下载URL
        result.getRecords().forEach(this::fillDownloadUrls);
        return result;
    }
    
    @Override
    public ToolFile uploadFile(MultipartFile file, Long toolId, String version, String description, String uploader) {
        if (file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }
        
        // 获取工具信息
        Tool tool = toolService.getById(toolId);
        if (tool == null) {
            throw new RuntimeException("工具不存在，工具ID: " + toolId);
        }
        
        try {
            // 获取原始文件名
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                originalFilename = "file_" + System.currentTimeMillis();
            }
            
            // 安全化文件名（移除特殊字符，但保留可读性）
            String safeFileName = sanitizeFileNameKeepReadable(originalFilename);
            
            String fileName;
            
            // 处理文件名和扩展名
            int lastDotIndex = safeFileName.lastIndexOf('.');
            if (lastDotIndex > 0 && lastDotIndex < safeFileName.length() - 1) {
                // 有扩展名
                String nameWithoutExt = safeFileName.substring(0, lastDotIndex);
                String extension = safeFileName.substring(lastDotIndex);
                fileName = nameWithoutExt + extension;
            } else {
                // 没有扩展名
                fileName = safeFileName;
            }
            
            // 根据工具类型和名称分目录存储
            // 文件路径格式: {toolType}/{toolName}/{fileName}
            // 工具名称需要进行文件名安全化处理
            String toolType = sanitizeFileName(tool.getType() != null ? tool.getType() : "default");
            String toolName = sanitizeFileName(tool.getName());
            
            // URL路径：使用正斜杠（用于数据库存储和URL访问）
            String urlPath = toolType + "/" + toolName;
            
            // 文件系统路径：使用系统分隔符（用于实际文件存储）
            String relativePath = toolType + File.separator + toolName;
            String fullPath = uploadPath + File.separator + relativePath;
            
            // 创建目录
            Path directory = Paths.get(fullPath);
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
                logger.info("创建上传目录: {}", directory);
            }
            
            // 保存文件
            Path filePath = Paths.get(fullPath, fileName);
            Files.copy(file.getInputStream(), filePath);
            
            logger.info("工具文件上传成功 - 工具: {}/{}, 文件名: {}, 大小: {} bytes", 
                       toolType, toolName, originalFilename, file.getSize());
            
            // 保存文件信息到数据库
            ToolFile toolFile = new ToolFile();
            toolFile.setToolId(toolId);
            toolFile.setFileName(fileName);
            toolFile.setOriginalName(originalFilename);
            // 数据库存储使用URL格式的路径（正斜杠）
            toolFile.setFilePath(urlPath + "/" + fileName);
            toolFile.setFileSize(file.getSize());
            toolFile.setFileType(file.getContentType());
            toolFile.setVersion(version);
            toolFile.setDescription(description);
            toolFile.setUploader(uploader);
            toolFile.setDownloadCount(0);
            
            this.save(toolFile);
            // 填充下载URL
            fillDownloadUrls(toolFile);
            return toolFile;
            
        } catch (IOException e) {
            logger.error("工具文件上传失败 - 工具ID: {}, 原因: {}", toolId, e.getMessage(), e);
            throw new RuntimeException("文件上传失败：" + e.getMessage());
        }
    }
    
    /**
     * 文件名安全化处理，移除不安全的字符（用于目录名）
     */
    private String sanitizeFileName(String name) {
        if (name == null || name.isEmpty()) {
            return "unknown";
        }
        // 移除或替换文件系统不支持的字符
        return name.replaceAll("[\\\\/:*?\"<>|]", "_")
                   .replaceAll("\\s+", "_")
                   .toLowerCase();
    }
    
    /**
     * 文件名安全化处理，保留可读性（用于实际文件名）
     * 只替换文件系统不支持的字符，保留中文、数字、字母等
     */
    private String sanitizeFileNameKeepReadable(String name) {
        if (name == null || name.isEmpty()) {
            return "unknown.file";
        }
        // 只替换文件系统不支持的特殊字符，保留其他字符
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
    
    /**
     * 填充下载URL字段
     */
    private void fillDownloadUrls(ToolFile toolFile) {
        if (toolFile == null) {
            return;
        }
        
        // 填充ID下载URL
        if (toolFile.getId() != null) {
            toolFile.setDownloadUrl("/api/files/download/" + toolFile.getId());
        }
        
        // 填充路径下载URL
        if (toolFile.getFilePath() != null && !toolFile.getFilePath().isEmpty()) {
            // 确保路径使用正斜杠（URL标准格式）
            String urlPath = toolFile.getFilePath().replace("\\", "/");
            toolFile.setDownloadUrlByPath("/api/files/download-by-path/" + urlPath);
        }
    }
    
    @Override
    public void increaseDownloadCount(Long fileId) {
        ToolFile toolFile = this.getById(fileId);
        if (toolFile != null) {
            toolFile.setDownloadCount(toolFile.getDownloadCount() + 1);
            this.updateById(toolFile);
            logger.debug("文件下载计数增加 - 文件ID: {}, 文件名: {}, 当前计数: {}", 
                        fileId, toolFile.getOriginalName(), toolFile.getDownloadCount());
        } else {
            logger.warn("尝试增加不存在文件的下载计数 - 文件ID: {}", fileId);
        }
    }
}
