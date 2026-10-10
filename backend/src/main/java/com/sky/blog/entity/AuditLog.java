package com.sky.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.sky.blog.enums.AuditAction;
import com.sky.blog.enums.AuditTarget;
import lombok.Data;

/**
 * 审核 / 管理操作日志。只追加、不修改、不删除。
 *
 * <p>{@code operatorName} 和 {@code targetTitle} 是<b>快照字段</b>：
 * 用户改名或注销、文章被删之后，日志仍然能完整读出"谁对什么做了什么"
 * （作业 2.3.3 要求记录操作者、目标内容、操作结果、说明和时间）。
 */
@Data
@TableName("t_audit_log")
public class AuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long operatorId;

    private String operatorName;

    private AuditAction action;

    private AuditTarget targetType;

    private Long targetId;

    private String targetTitle;

    /** SUCCESS / FAIL */
    private String result;

    /** 说明：驳回原因、下架原因、禁用理由等 */
    private String remark;

    private String createdAt;
}
