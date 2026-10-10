package com.sky.blog.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/** 点赞 / 收藏的返回：操作后的状态与最新计数。 */
@Schema(description = "点赞/收藏开关结果")
public record Toggle(
        @Schema(description = "操作后是否为已点赞/已收藏状态") boolean active,
        @Schema(description = "文章上的最新计数") int count,
        Long articleId) {
}
