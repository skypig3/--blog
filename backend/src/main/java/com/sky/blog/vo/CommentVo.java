package com.sky.blog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 评论出参：顶层评论带 children，回复挂在 children 上。 */
public final class CommentVo {

    private CommentVo() {
    }

    @Schema(description = "评论 / 回复")
    public record Item(
            Long id,
            Long articleId,
            @Schema(description = "所属文章标题，后台列表用")
            String articleTitle,
            Long parentId,
            UserVo.Author author,
            String content,
            @Schema(description = "PENDING/PUBLISHED/REJECTED；游客只看到 PUBLISHED")
            String status,
            @Schema(description = "驳回原因，仅本人和管理员可见")
            String rejectReason,
            String createdAt,
            @Schema(description = "回复列表；前台只返回一层")
            List<Item> children) {
    }
}
