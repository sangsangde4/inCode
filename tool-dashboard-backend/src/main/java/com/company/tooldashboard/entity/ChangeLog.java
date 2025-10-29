package com.company.tooldashboard.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 变更日志实体类
 */
@Data
@TableName("tb_change_log")
public class ChangeLog {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 关联工具ID
     */
    private Long toolId;
    
    /**
     * 版本号
     */
    private String version;
    
    /**
     * 变更类型（新增/修复/优化/删除）
     */
    private String changeType;
    
    /**
     * 变更内容
     */
    private String content;
    
    /**
     * 变更人
     */
    private String changer;
    
    /**
     * 变更时间
     */
    private LocalDateTime changeTime;
    
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
