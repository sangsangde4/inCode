package com.company.tooldashboard.dto;

import com.company.tooldashboard.entity.Tool;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 工具分组DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolGroupDTO {
    
    /**
     * 类型名称
     */
    private String type;
    
    /**
     * 类型显示名称
     */
    private String typeName;
    
    /**
     * 工具列表
     */
    private List<Tool> tools;
    
    /**
     * 工具数量
     */
    private Integer count;
}
