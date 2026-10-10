package com.sky.blog.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/** 站点元数据出参：分类、标签、上传结果。 */
public final class SiteVo {

    private SiteVo() {
    }

    @Schema(description = "分类（含该分类下已发布文章数）")
    public record Category(Long id, String name, Integer sortOrder, Long articleCount) {
    }

    @Schema(description = "标签（含该标签下已发布文章数）")
    public record Tag(Long id, String name, Long articleCount) {
    }

    @Schema(description = "图片上传结果")
    public record UploadedImage(
            @Schema(description = "可直接用于 img src 的相对路径", example = "/uploads/2026-10/6f1c.png")
            String url,
            @Schema(description = "原始文件名（仅回显，不落地存储）")
            String originalName,
            Long size) {
    }
}
