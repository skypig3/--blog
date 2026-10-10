package com.sky.blog.controller;

import com.sky.blog.common.ApiResult;
import com.sky.blog.enums.AuditAction;
import com.sky.blog.enums.AuditTarget;
import com.sky.blog.enums.Role;
import com.sky.blog.security.RequireRole;
import com.sky.blog.service.AuditLogService;
import com.sky.blog.service.StatsService;
import com.sky.blog.vo.AdminVo;
import com.sky.blog.vo.PageVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 后台首页统计与操作日志（作业 2.3.1 / 2.3.3）。 */
@Tag(name = "94 后台-统计与日志", description = "仅 ADMIN 可访问")
@RequireRole(Role.ADMIN)
@RestController
@RequestMapping("/api/admin")
public class AdminStatsController {

    private final StatsService statsService;
    private final AuditLogService auditLogService;

    public AdminStatsController(StatsService statsService, AuditLogService auditLogService) {
        this.statsService = statsService;
        this.auditLogService = auditLogService;
    }

    @Operation(summary = "七项统计",
            description = "用户总数 / 文章总数 / 已发布文章数 / 待审核文章数 / 评论总数 / "
                    + "待审核评论数 / 阅读量汇总。全部实时 COUNT、SUM 自 SQLite，无写死数字")
    @GetMapping("/stats")
    public ApiResult<AdminVo.Stats> stats() {
        return ApiResult.ok(statsService.stats());
    }

    @Operation(summary = "后台首页", description = "统计 + 近期文章 + 待处理审核任务")
    @GetMapping("/dashboard")
    public ApiResult<AdminVo.Dashboard> dashboard() {
        return ApiResult.ok(statsService.dashboard());
    }

    @Operation(summary = "审核与操作记录", description = "记录操作者、目标内容、操作结果、说明和时间；只追加不修改")
    @GetMapping("/audit-logs")
    public ApiResult<PageVo<AdminVo.AuditLogItem>> auditLogs(
            @RequestParam(required = false) AuditTarget targetType,
            @RequestParam(required = false) AuditAction action,
            @Parameter(description = "匹配目标标题、操作者或说明") @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResult.ok(auditLogService.page(current, Math.min(size, 100), targetType, action, keyword));
    }
}
