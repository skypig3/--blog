package com.sky.blog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 后台出参：统计面板与操作日志。 */
public final class AdminVo {

    private AdminVo() {
    }

    /**
     * 后台首页统计。
     *
     * <p>作业 2.3.1 要求"统计数据必须来自实际数据库，不得以写死的数字或静态图表代替"，
     * 所以这七个字段全部由 COUNT / SUM 查询得出，没有任何常量。
     */
    @Schema(description = "后台统计（全部实时查库）")
    public record Stats(
            long userCount,
            long articleCount,
            long publishedArticleCount,
            long pendingArticleCount,
            long commentCount,
            long pendingCommentCount,
            long totalViewCount) {
    }

    @Schema(description = "后台首页：统计 + 近期文章 + 待处理审核任务")
    public record Dashboard(
            Stats stats,
            List<ArticleVo.Card> recentArticles,
            List<ArticleVo.Card> pendingArticles,
            List<CommentVo.Item> pendingComments) {
    }

    @Schema(description = "审核/管理操作日志")
    public record AuditLogItem(
            Long id,
            Long operatorId,
            @Schema(description = "操作者姓名快照") String operatorName,
            String action,
            String targetType,
            Long targetId,
            @Schema(description = "目标标题快照") String targetTitle,
            String result,
            @Schema(description = "说明：驳回原因/下架原因/禁用理由等") String remark,
            String createdAt) {
    }
}
