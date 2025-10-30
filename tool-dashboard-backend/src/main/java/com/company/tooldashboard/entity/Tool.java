package com.company.tooldashboard.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.company.tooldashboard.validation.SemanticVersion;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 工具/平台实体类
 */
@Data
@TableName("tb_tool")
public class Tool {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 工具名称
     */
    private String name;
    
    /**
     * 工具描述
     */
    private String description;
    
    /**
     * 工具类型（工具/平台）
     */
    private String type;
    
    /**
     * 工具图标URL
     */
    private String iconUrl;
    
    /**
     * 工具访问地址
     */
    private String accessUrl;
    
    /**
     * 当前版本
     */
    @SemanticVersion(message = "工具版本号格式不正确，必须符合语义化版本规范（如：1.0.0）")
    private String currentVersion;
    
    /**
     * 负责人
     */
    private String owner;
    
    /**
     * 状态（0-已下线 1-运行中 2-维护中）
     */
    private Integer status;
    
    /**
     * 排序号
     */
    private Integer sortOrder;
    
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
}
