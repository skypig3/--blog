package com.sky.blog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 文章出参：列表用 Card，详情用 Detail。 */
public final class ArticleVo {

    private ArticleVo() {
    }

    @Schema(description = "列表卡片")
    public record Card(
            Long id,
            String title,
            String summary,
            String cover,
            UserVo.Author author,
            Long categoryId,
            String categoryName,
            List<String> tags,
            @Schema(description = "DRAFT/PENDING/PUBLISHED/REJECTED/REMOVED；游客只会看到 PUBLISHED")
            String status,
            String publishedAt,
            String createdAt,
            Integer viewCount,
            Integer likeCount,
            Integer collectCount,
            Integer commentCount) {
    }

    @Schema(description = "文章详情")
    public record Detail(
            Long id,
            String title,
            String summary,
            @Schema(description = "Markdown 原文，由前端渲染并消毒")
            String contentMd,
            String cover,
            Long authorId,
            UserVo.Author author,
            Long categoryId,
            String categoryName,
            List<String> tags,
            String status,
            @Schema(description = "驳回原因，仅作者本人可见")
            String rejectReason,
            @Schema(description = "下架原因，仅作者本人可见")
            String offlineReason,
            String submittedAt,
            String auditedAt,
            String publishedAt,
            String createdAt,
            String updatedAt,
            Integer viewCount,
            Integer likeCount,
            Integer collectCount,
            Integer commentCount,
            @Schema(description = "当前登录用户是否已点赞") Boolean liked,
            @Schema(description = "当前登录用户是否已收藏") Boolean collected) {
    }
}
