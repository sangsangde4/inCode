package com.company.tooldashboard.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.tooldashboard.entity.Tool;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工具Mapper接口
 */
@Mapper
public interface ToolMapper extends BaseMapper<Tool> {
}
