package com.sky.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 文章相关入参。 */
public final class ArticleDto {

    private ArticleDto() {
    }

    @Schema(description = "新建/编辑文章请求")
    public record Save(
            @NotBlank(message = "标题不能为空")
            @Size(max = 120, message = "标题最长 120 字")
            String title,

            @Schema(description = "摘要；留空时服务端从正文自动截取前 120 字")
            @Size(max = 300, message = "摘要最长 300 字")
            String summary,

            @NotBlank(message = "正文不能为空")
            @Size(max = 200_000, message = "正文过长")
            String contentMd,

            @Schema(description = "封面图相对路径", example = "/uploads/2026-10/cover.png")
            @Size(max = 255)
            String cover,

            Long categoryId,

            List<Long> tagIds,

            @Schema(description = "true=保存并提交审核，false=仅存草稿", example = "false")
            Boolean submit) {
    }

    @Schema(description = "审核请求（文章/评论通用）")
    public record Audit(
            @Schema(description = "true=通过，false=驳回")
            @NotNull(message = "必须指明通过还是驳回")
            Boolean pass,

            @Schema(description = "驳回原因，驳回时必填")
            @Size(max = 300, message = "原因最长 300 字")
            String reason) {
    }

    @Schema(description = "下架请求")
    public record Offline(
            @NotBlank(message = "下架必须填写原因")
            @Size(max = 300, message = "原因最长 300 字")
            String reason) {
    }
}
