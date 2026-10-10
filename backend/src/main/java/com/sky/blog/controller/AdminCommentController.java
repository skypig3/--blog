package com.sky.blog.controller;

import com.sky.blog.common.ApiResult;
import com.sky.blog.dto.ArticleDto;
import com.sky.blog.enums.CommentStatus;
import com.sky.blog.enums.Role;
import com.sky.blog.security.Principal;
import com.sky.blog.security.RequireRole;
import com.sky.blog.service.CommentService;
import com.sky.blog.vo.CommentVo;
import com.sky.blog.vo.PageVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

/** 后台评论管理与审核（作业 2.3.4）。 */
@Tag(name = "93 后台-评论审核", description = "仅 ADMIN 可访问；回复与顶层评论共用同一套审核")
@RequireRole(Role.ADMIN)
@RestController
@RequestMapping("/api/admin/comments")
public class AdminCommentController {

    private final CommentService commentService;

    public AdminCommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @Operation(summary = "评论列表", description = "可按作者、内容关键字、状态、所属文章筛选，并带出所属文章标题")
    @GetMapping
    public ApiResult<PageVo<CommentVo.Item>> page(
            @Parameter(description = "评论作者 id") @RequestParam(required = false) Long userId,
            @Parameter(description = "评论内容关键字") @RequestParam(required = false) String keyword,
            @RequestParam(required = false) CommentStatus status,
            @RequestParam(required = false) Long articleId,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResult.ok(commentService.adminList(userId, keyword, status, articleId, current, size));
    }

    @Operation(summary = "查看评论详情（含所属文章）")
    @GetMapping("/{id}")
    public ApiResult<CommentVo.Item> detail(@PathVariable Long id) {
        return ApiResult.ok(commentService.adminDetail(id));
    }

    @Operation(summary = "审核评论",
            description = "通过：PENDING → PUBLISHED，并把文章的评论数 +1；"
                    + "驳回：PENDING → REJECTED，必须填原因。重复点击得到 409")
    @PostMapping("/{id}/audit")
    public ApiResult<CommentVo.Item> audit(@PathVariable Long id, @Valid @RequestBody ArticleDto.Audit dto) {
        Principal op = Principal.Holder.require();
        return ApiResult.ok(commentService.audit(op, id, dto),
                Boolean.TRUE.equals(dto.pass()) ? "已通过审核" : "已驳回");
    }

    @Operation(summary = "删除违规评论", description = "逻辑删除；已发布的评论被删时评论数 -1")
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        commentService.delete(id, Principal.Holder.require());
        return ApiResult.ok(null, "已删除");
    }
}
