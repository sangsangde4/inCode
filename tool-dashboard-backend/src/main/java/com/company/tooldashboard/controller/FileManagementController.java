package com.company.tooldashboard.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.tooldashboard.annotation.RequireAdmin;
import com.company.tooldashboard.common.Result;
import com.company.tooldashboard.entity.ToolFile;
import com.company.tooldashboard.service.FileManagementService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 统一文件管理控制器
 * 负责所有文件相关操作：工具文件、图标文件的上传、下载、访问
 * 
 * 合并了原来的：
 * - FileController (图标访问)
 * - FileUploadController (图标上传)
 * - ToolFileController (工具文件管理)
 */
@RestController
public class FileManagementController {
    
    private static final Logger logger = LoggerFactory.getLogger(FileManagementController.class);
    
    @Autowired
    private FileManagementService fileManagementService;
    
    @Value("${file.upload-path}")
    private String uploadPath;
    
    @Value("${file.access-url}")
    private String accessUrl;
    
    // ==================== 工具文件管理接口 ====================
    
    /**
     * 根据工具ID查询文件列表（公开接口）
     */
    @GetMapping("/files/tool/{toolId}")
    public Result<List<ToolFile>> getFilesByToolId(@PathVariable Long toolId) {
        List<ToolFile> files = fileManagementService.getFilesByToolId(toolId);
        return Result.success(files);
    }
    
    /**
     * 分页查询文件列表（公开接口）
     */
    @GetMapping("/files/page")
    public Result<Page<ToolFile>> getFilePage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long toolId) {
        Page<ToolFile> page = fileManagementService.getFilePage(pageNum, pageSize, toolId);
        return Result.success(page);
    }
    
    /**
     * 工具文件下载（公开接口）- 通过文件ID下载
     */
    @GetMapping("/files/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        try {
            ToolFile toolFile = fileManagementService.getById(id);
            if (toolFile == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 增加下载次数
            fileManagementService.increaseDownloadCount(id);
            
            // 读取文件（将URL路径转换为文件系统路径）
            String filePathStr = toolFile.getFilePath().replace("/", File.separator);
            Path filePath = Paths.get(uploadPath, filePathStr);
            
            
            Resource resource = new UrlResource(filePath.toUri());
            
            if (!resource.exists()) {
                logger.error("文件不存在: {}", filePath.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }
            
            // 设置响应头，支持中文文件名
            String filename = toolFile.getOriginalName();
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                    .replace("+", "%20");
            
            // 同时设置 filename 和 filename*，确保兼容性
            String contentDisposition = String.format(
                "attachment; filename=\"%s\"; filename*=UTF-8''%s",
                new String(filename.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1),
                encodedFilename
            );
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .body(resource);
                    
        } catch (Exception e) {
            logger.error("文件下载失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 通过路径+文件名下载文件（公开接口）
     * 支持路径格式: /files/download-by-path/{toolType}/{toolName}/{fileName}
     * 例如: /files/download-by-path/platform/tool1/abc123.zip
     */
    @GetMapping("/files/download-by-path/**")
    public ResponseEntity<Resource> downloadFileByPath(HttpServletRequest request) {
        try {
            // 获取完整路径（去掉 /files/download-by-path/ 前缀）
            String fullPath = request.getRequestURI();
            String encodedUrlPath = fullPath.substring("/api/files/download-by-path/".length());
            
            // URL解码，处理中文文件名
            String urlPath = java.net.URLDecoder.decode(encodedUrlPath, StandardCharsets.UTF_8);
            
            // 将URL路径转换为文件系统路径
            String fileSystemPath = urlPath.replace("/", File.separator);
            Path filePath = Paths.get(uploadPath, fileSystemPath);
            File file = filePath.toFile();
            
            if (!file.exists()) {
                logger.warn("文件不存在: {}", filePath.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new UrlResource(filePath.toUri());
            
            // 从URL路径中提取原始文件名（最后一个/后面的部分）
            String fileName = urlPath.substring(urlPath.lastIndexOf('/') + 1);
            
            // 尝试从数据库获取原始文件名（更准确）
            try {
                // 使用精确查询：根据文件路径查询数据库
                ToolFile toolFile = fileManagementService.lambdaQuery()
                    .eq(ToolFile::getFilePath, urlPath)
                    .last("LIMIT 1")
                    .one();
                
                if (toolFile != null && toolFile.getOriginalName() != null) {
                    fileName = toolFile.getOriginalName();
                }
            } catch (Exception e) {
                logger.error("获取原始文件名失败", e);
            }
            
            String encodedFilename = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replace("+", "%20");
            
            // 同时设置 filename 和 filename*，确保兼容性
            String contentDisposition = String.format(
                "attachment; filename=\"%s\"; filename*=UTF-8''%s",
                new String(fileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1),
                encodedFilename
            );
            
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .body(resource);
                    
        } catch (Exception e) {
            logger.error("通过路径下载文件失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 上传工具文件（需要管理员权限）
     */
    @RequireAdmin
    @PostMapping("/files/upload")
    public Result<ToolFile> uploadToolFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam Long toolId,
            @RequestParam(required = false) String version,
            @RequestParam(required = false) String architecture,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String uploader) {
        try {
            ToolFile toolFile = fileManagementService.uploadFile(file, toolId, version, architecture, description, uploader);
            return Result.success(toolFile);
        } catch (Exception e) {
            logger.error("工具文件上传失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 删除工具文件（需要管理员权限）
     */
    @RequireAdmin
    @DeleteMapping("/files/{id}")
    public Result<Void> deleteFile(@PathVariable Long id) {
        ToolFile toolFile = fileManagementService.getById(id);
        if (toolFile != null) {
            // 删除物理文件
            try {
                Path filePath = Paths.get(uploadPath, toolFile.getFilePath());
                File file = filePath.toFile();
                if (file.exists()) {
                    file.delete();
                    logger.info("已删除物理文件: {}", filePath);
                }
            } catch (Exception e) {
                logger.warn("删除物理文件失败，但继续删除数据库记录", e);
            }
            
            // 删除数据库记录
            fileManagementService.removeById(id);
        }
        return Result.success();
    }
    
    // ==================== 图标文件管理接口 ====================
    
    /**
     * 上传工具图标（需要管理员权限）
     * 支持可选的category参数来指定分类文件夹
     */
    @RequireAdmin
    @PostMapping("/upload/icon")
    public Result<Map<String, String>> uploadIcon(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String category) {
        // 验证文件
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }
        
        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error("只能上传图片文件");
        }
        
        // 验证文件大小 (限制2MB)
        if (file.getSize() > 2 * 1024 * 1024) {
            return Result.error("图片大小不能超过2MB");
        }
        
        // 验证文件扩展名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return Result.error("文件名无效");
        }
        
        String extension = getFileExtension(originalFilename);
        if (!isValidImageExtension(extension)) {
            return Result.error("只支持 jpg, jpeg, png, gif, svg 格式的图片");
        }
        
        try {
            // 生成文件名和路径
            String fileName = UUID.randomUUID().toString() + "." + extension;
            
            // 根据category参数组织文件夹，如果没有指定则使用日期分类
            String relativePath;
            if (category != null && !category.isEmpty()) {
                // 使用自定义分类: icons/{category}/{fileName}
                String sanitizedCategory = sanitizeFileName(category);
                relativePath = "icons/" + sanitizedCategory;
            } else {
                // 使用日期分类: icons/{yyyy/MM/dd}/{fileName}
                String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
                relativePath = "icons/" + datePath;
            }
            
            // 创建目录
            String fullPath = uploadPath + "/" + relativePath;
            File dir = new File(fullPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            // 保存文件
            Path filePath = Paths.get(fullPath, fileName);
            Files.write(filePath, file.getBytes());
            
            logger.info("图标上传成功 - 分类: {}, 文件: {}", 
                       category != null ? category : "默认(日期)", filePath);
            
            // 返回访问URL
            String fileUrl = accessUrl + "/" + relativePath + "/" + fileName;
            
            Map<String, String> result = new HashMap<>();
            result.put("url", fileUrl);
            result.put("fileName", fileName);
            result.put("originalName", originalFilename);
            result.put("relativePath", relativePath + "/" + fileName);
            
            return Result.success(result);
            
        } catch (IOException e) {
            logger.error("图标上传失败", e);
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }
    
    /**
     * 文件名安全化处理
     */
    private String sanitizeFileName(String name) {
        if (name == null || name.isEmpty()) {
            return "unknown";
        }
        return name.replaceAll("[\\\\/:*?\"<>|]", "_")
                   .replaceAll("\\s+", "_")
                   .toLowerCase();
    }
    
    /**
     * 获取图标文件（公开接口）
     * 支持子目录路径，例如: /icon/icons/2025/10/27/xxx.png
     */
    @GetMapping("/icon/**")
    public ResponseEntity<byte[]> getIcon(HttpServletRequest request) {
        // 获取完整路径（去掉 /icon/ 前缀）
        String fullPath = request.getRequestURI();
        String encodedPath = fullPath.substring("/api/icon/".length());
        
        // URL解码，处理中文路径
        String relativePath;
        try {
            relativePath = java.net.URLDecoder.decode(encodedPath, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("URL解码失败: {}", encodedPath);
            return ResponseEntity.badRequest().build();
        }
        
        try {
            // 将URL路径转换为文件系统路径
            String fileSystemPath = relativePath.replace("/", File.separator);
            Path filePath = Paths.get(uploadPath, fileSystemPath);
            File file = filePath.toFile();
            
            if (!file.exists()) {
                logger.warn("图标文件不存在: {}", filePath.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }

            // 读取文件内容
            byte[] imageBytes = Files.readAllBytes(filePath);
            
            // 根据文件名获取Content-Type
            String contentType = getImageContentType(file.getName());
            
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(imageBytes);
                    
        } catch (IOException e) {
            logger.error("读取图标文件失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // ==================== 私有工具方法 ====================
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }
    
    /**
     * 验证是否为有效的图片扩展名
     */
    private boolean isValidImageExtension(String extension) {
        return extension.equals("jpg") || 
               extension.equals("jpeg") || 
               extension.equals("png") || 
               extension.equals("gif") || 
               extension.equals("svg");
    }
    
    /**
     * 根据文件名获取图片Content-Type
     */
    private String getImageContentType(String filename) {
        String extension = getFileExtension(filename);
        
        switch (extension) {
            case "png":
                return "image/png";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "gif":
                return "image/gif";
            case "svg":
                return "image/svg+xml";
            case "webp":
                return "image/webp";
            case "bmp":
                return "image/bmp";
            case "ico":
                return "image/x-icon";
            default:
                return "application/octet-stream";
        }
    }
}
