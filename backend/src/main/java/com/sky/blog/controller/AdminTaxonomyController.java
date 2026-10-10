package com.sky.blog.controller;

import com.sky.blog.common.ApiResult;
import com.sky.blog.dto.AdminDto;
import com.sky.blog.enums.Role;
import com.sky.blog.security.Principal;
import com.sky.blog.security.RequireRole;
import com.sky.blog.service.TaxonomyService;
import com.sky.blog.vo.SiteVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 后台的分类与标签维护。 */
@Tag(name = "91 后台-分类标签", description = "仅 ADMIN 可访问；公开读取走 /api/categories 与 /api/tags")
@RestController
@RequestMapping("/api/admin")
public class AdminTaxonomyController {

    private final TaxonomyService taxonomyService;

    public AdminTaxonomyController(TaxonomyService taxonomyService) {
        this.taxonomyService = taxonomyService;
    }

    @Operation(summary = "分类全量（后台）")
    @RequireRole(Role.ADMIN)
    @GetMapping("/categories")
    public ApiResult<List<SiteVo.Category>> categories() {
        return ApiResult.ok(taxonomyService.categoriesWithCount());
    }

    @RequireRole(Role.ADMIN)
    @Operation(summary = "新建分类")
    @PostMapping("/categories")
    public ApiResult<SiteVo.Category> createCategory(@Valid @RequestBody AdminDto.CategorySave dto) {
        return ApiResult.ok(taxonomyService.createCategory(Principal.Holder.require(), dto), "分类已创建");
    }

    @RequireRole(Role.ADMIN)
    @Operation(summary = "修改分类")
    @PutMapping("/categories/{id}")
    public ApiResult<SiteVo.Category> updateCategory(@PathVariable Long id,
                                                     @Valid @RequestBody AdminDto.CategorySave dto) {
        return ApiResult.ok(taxonomyService.updateCategory(Principal.Holder.require(), id, dto), "分类已更新");
    }

    @RequireRole(Role.ADMIN)
    @Operation(summary = "删除分类", description = "分类下还有已发布文章时返回 422")
    @DeleteMapping("/categories/{id}")
    public ApiResult<Void> deleteCategory(@PathVariable Long id) {
        taxonomyService.deleteCategory(Principal.Holder.require(), id);
        return ApiResult.ok(null, "分类已删除");
    }

    @RequireRole(Role.ADMIN)
    @Operation(summary = "新建标签")
    @PostMapping("/tags")
    public ApiResult<SiteVo.Tag> createTag(@Valid @RequestBody AdminDto.TagSave dto) {
        return ApiResult.ok(taxonomyService.createTag(Principal.Holder.require(), dto), "标签已创建");
    }

    @RequireRole(Role.ADMIN)
    @Operation(summary = "修改标签")
    @PutMapping("/tags/{id}")
    public ApiResult<SiteVo.Tag> updateTag(@PathVariable Long id, @Valid @RequestBody AdminDto.TagSave dto) {
        return ApiResult.ok(taxonomyService.updateTag(Principal.Holder.require(), id, dto), "标签已更新");
    }

    @RequireRole(Role.ADMIN)
    @Operation(summary = "删除标签", description = "仍被文章引用时返回 422")
    @DeleteMapping("/tags/{id}")
    public ApiResult<Void> deleteTag(@PathVariable Long id) {
        taxonomyService.deleteTag(Principal.Holder.require(), id);
        return ApiResult.ok(null, "标签已删除");
    }
}
