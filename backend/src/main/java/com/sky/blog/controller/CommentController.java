package com.sky.blog.controller;

import com.sky.blog.common.ApiResult;
import com.sky.blog.dto.CommentDto;
import com.sky.blog.security.Principal;
import com.sky.blog.security.RequireLogin;
import com.sky.blog.service.CommentService;
import com.sky.blog.vo.CommentVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 评论区：游客可读，登录用户可发/可回复。 */
@Tag(name = "30 评论", description = "读取公开；发表与删除需登录")
@RestController
@RequestMapping("/api/articles/{articleId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @Operation(summary = "评论列表（公开）",
            description = "两层结构：顶层评论 + children 回复；只返回已审核通过的内容")
    @GetMapping
    public ApiResult<List<CommentVo.Item>> list(@PathVariable Long articleId) {
        return ApiResult.ok(commentService.visibleTree(articleId));
    }

    @RequireLogin
    @Operation(summary = "发表评论或回复",
            description = "新评论一律进入待审核（PENDING），管理员通过后才在前台可见；parentId 为空即顶层评论")
    @PostMapping
    public ApiResult<CommentVo.Item> create(@PathVariable Long articleId,
                                            @Valid @RequestBody CommentDto.Create dto) {
        return ApiResult.ok(commentService.create(articleId, Principal.Holder.requireUserId(), dto),
                "已提交，等待管理员审核");
    }

    @RequireLogin
    @Operation(summary = "删除评论", description = "评论作者本人或管理员可删")
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        commentService.delete(id, Principal.Holder.require());
        return ApiResult.ok(null, "已删除");
    }
}
