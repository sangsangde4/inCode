package com.company.tooldashboard.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.tooldashboard.entity.ChangeLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 变更日志Mapper接口
 */
@Mapper
public interface ChangeLogMapper extends BaseMapper<ChangeLog> {
}
