package com.company.tooldashboard.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.company.tooldashboard.validation.SemanticVersion;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 工具文件实体类
 */
@Data
@TableName("tb_tool_file")
public class ToolFile {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 关联工具ID
     */
    private Long toolId;
    
    /**
     * 文件名称
     */
    private String fileName;
    
    /**
     * 文件原始名称
     */
    private String originalName;
    
    /**
     * 文件路径
     */
    private String filePath;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 文件类型
     */
    private String fileType;
    
    /**
     * 版本号
     */
    @SemanticVersion(message = "文件版本号格式不正确，必须符合语义化版本规范（如：1.0.0）", allowNull = false)
    private String version;
    
    /**
     * 架构类型（如：linux_x64, windows_x64, macos_arm64等）
     */
    private String architecture;
    
    /**
     * 下载次数
     */
    private Integer downloadCount;
    
    /**
     * 文件说明
     */
    private String description;
    
    /**
     * 上传者
     */
    private String uploader;
    
    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer deleted;
    
    // ========== 非数据库字段 ==========
    
    /**
     * 路径下载URL（非数据库字段）
     * 格式: /api/files/download-by-path/{toolType}/{toolName}/{fileName}
     */
    @TableField(exist = false)
    private String downloadUrlByPath;
    
    /**
     * ID下载URL（非数据库字段，兼容旧方式）
     * 格式: /api/files/download/{id}
     */
    @TableField(exist = false)
    private String downloadUrl;
}
