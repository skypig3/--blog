package com.sky.blog.controller;

import com.sky.blog.common.ApiResult;
import com.sky.blog.dto.AdminDto;
import com.sky.blog.dto.ArticleDto;
import com.sky.blog.enums.ArticleStatus;
import com.sky.blog.enums.Role;
import com.sky.blog.security.Principal;
import com.sky.blog.security.RequireRole;
import com.sky.blog.service.ArticleService;
import com.sky.blog.vo.ArticleVo;
import com.sky.blog.vo.PageVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 后台文章管理与审核（作业 2.3.3）。 */
@Tag(name = "92 后台-文章管理与审核", description = "仅 ADMIN 可访问")
@RequireRole(Role.ADMIN)
@RestController
@RequestMapping("/api/admin/articles")
public class AdminArticleController {

    private final ArticleService articleService;

    public AdminArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @Operation(summary = "文章列表（后台）", description = "可按作者、分类、状态筛选，能看到所有状态")
    @GetMapping
    public ApiResult<PageVo<ArticleVo.Card>> page(
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) ArticleStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResult.ok(articleService.adminList(authorId, categoryId, status, keyword, current, size));
    }

    @Operation(summary = "查看完整待审核内容")
    @GetMapping("/{id}")
    public ApiResult<ArticleVo.Detail> detail(@PathVariable Long id) {
        return ApiResult.ok(articleService.adminDetail(id));
    }

    @Operation(summary = "审核文章",
            description = "通过：PENDING → PUBLISHED；驳回：PENDING → REJECTED 且必须填原因。"
                    + "状态判断写在 UPDATE 的 WHERE 里（CAS），重复点击或文章已被撤回都会得到 409")
    @PostMapping("/{id}/audit")
    public ApiResult<ArticleVo.Detail> audit(@PathVariable Long id, @Valid @RequestBody ArticleDto.Audit dto) {
        Principal op = Principal.Holder.require();
        return ApiResult.ok(articleService.audit(op, id, dto),
                Boolean.TRUE.equals(dto.pass()) ? "已通过审核" : "已驳回");
    }

    @Operation(summary = "下架违规文章", description = "PUBLISHED → REMOVED，必须填写下架原因；作者可见")
    @PostMapping("/{id}/offline")
    public ApiResult<ArticleVo.Detail> offline(@PathVariable Long id,
                                               @Valid @RequestBody ArticleDto.Offline dto) {
        return ApiResult.ok(articleService.offline(Principal.Holder.require(), id, dto), "已下架");
    }

    @Operation(summary = "删除文章", description = "逻辑删除，并写入操作日志")
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id, @Valid @RequestBody(required = false) AdminDto.Remark dto) {
        articleService.adminDelete(Principal.Holder.require(), id, dto == null ? null : dto.remark());
        return ApiResult.ok(null, "已删除");
    }
}
