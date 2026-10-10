package com.sky.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.blog.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

/** 操作日志：只追加。业务代码里没有任何 update / delete 入口。 */
@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
