package com.sky.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.blog.entity.AuditLog;
import com.sky.blog.enums.AuditAction;
import com.sky.blog.enums.AuditTarget;
import com.sky.blog.mapper.AuditLogMapper;
import com.sky.blog.security.Principal;
import com.sky.blog.vo.AdminVo;
import com.sky.blog.vo.PageVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 操作日志（作业 2.3.3：审核、下架及相关管理操作要记录操作者、目标内容、操作结果、说明和时间）。
 *
 * <p>日志只追加，不提供修改和删除入口。{@code operatorName} 与 {@code targetTitle}
 * 写入的是<b>当时的快照</b>，用户之后改名或被删除都不影响历史日志的可读性。
 */
@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogMapper auditLogMapper;

    public AuditLogService(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public void record(Principal operator, AuditAction action, AuditTarget targetType,
                       Long targetId, String targetTitle, String remark) {
        AuditLog row = new AuditLog();
        row.setOperatorId(operator.userId());
        row.setOperatorName(operator.username());
        row.setAction(action);
        row.setTargetType(targetType);
        row.setTargetId(targetId);
        row.setTargetTitle(targetTitle);
        row.setResult("SUCCESS");
        row.setRemark(remark);
        auditLogMapper.insert(row);
        log.info("操作日志 operator={} action={} target={}#{}", operator.username(), action, targetType, targetId);
    }

    /** 后台"审核记录"列表，可按目标类型和动作筛选 */
    public PageVo<AdminVo.AuditLogItem> page(long current, long size, AuditTarget targetType,
                                             AuditAction action, String keyword) {
        LambdaQueryWrapper<AuditLog> q = new LambdaQueryWrapper<>();
        if (targetType != null) {
            q.eq(AuditLog::getTargetType, targetType);
        }
        if (action != null) {
            q.eq(AuditLog::getAction, action);
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            q.and(w -> w.like(AuditLog::getTargetTitle, kw)
                    .or().like(AuditLog::getOperatorName, kw)
                    .or().like(AuditLog::getRemark, kw));
        }
        q.orderByDesc(AuditLog::getId);
        Page<AuditLog> page = auditLogMapper.selectPage(new Page<>(current, size), q);
        return PageVo.of(page, row -> new AdminVo.AuditLogItem(
                row.getId(), row.getOperatorId(), row.getOperatorName(),
                row.getAction() == null ? null : row.getAction().name(),
                row.getTargetType() == null ? null : row.getTargetType().name(),
                row.getTargetId(), row.getTargetTitle(), row.getResult(), row.getRemark(),
                row.getCreatedAt()));
    }
}
