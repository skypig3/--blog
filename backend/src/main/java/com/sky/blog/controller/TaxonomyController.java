package com.sky.blog.controller;

import com.sky.blog.common.ApiResult;
import com.sky.blog.service.TaxonomyService;
import com.sky.blog.vo.SiteVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 前台的分类导航与标签入口（公开）。 */
@Tag(name = "10 分类与标签", description = "公开接口")
@RestController
public class TaxonomyController {

    private final TaxonomyService taxonomyService;

    public TaxonomyController(TaxonomyService taxonomyService) {
        this.taxonomyService = taxonomyService;
    }

    @Operation(summary = "分类列表", description = "含每个分类下已发布文章数，按 sortOrder 升序")
    @GetMapping("/api/categories")
    public ApiResult<List<SiteVo.Category>> categories() {
        return ApiResult.ok(taxonomyService.categoriesWithCount());
    }

    @Operation(summary = "标签列表", description = "含每个标签下已发布文章数，按使用量降序")
    @GetMapping("/api/tags")
    public ApiResult<List<SiteVo.Tag>> tags() {
        return ApiResult.ok(taxonomyService.tagsWithCount());
    }
}
