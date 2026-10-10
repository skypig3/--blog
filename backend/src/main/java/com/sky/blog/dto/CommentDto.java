package com.sky.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 评论入参。 */
public final class CommentDto {

    private CommentDto() {
    }

    @Schema(description = "发表评论或回复：parentId 为空表示顶层评论")
    public record Create(
            @NotBlank(message = "评论内容不能为空")
            @Size(max = 1000, message = "单条评论最长 1000 字")
            String content,

            @Schema(description = "被回复的评论 id，回复时填")
            Long parentId) {
    }
}
