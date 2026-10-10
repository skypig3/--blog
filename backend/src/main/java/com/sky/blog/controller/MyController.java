package com.sky.blog.controller;

import com.sky.blog.common.ApiResult;
import com.sky.blog.enums.ArticleStatus;
import com.sky.blog.enums.CommentStatus;
import com.sky.blog.security.Principal;
import com.sky.blog.security.RequireLogin;
import com.sky.blog.service.ArticleService;
import com.sky.blog.service.CommentService;
import com.sky.blog.service.InteractionService;
import com.sky.blog.vo.ArticleVo;
import com.sky.blog.vo.CommentVo;
import com.sky.blog.vo.PageVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 个人中心：我的文章 / 我的评论 / 我的收藏。
 *
 * <p>这里的查询都<b>强制带上当前登录用户的 id</b>（从 Session 解析，不接受前端传参），
 * 所以"查看自己的审核结果"这条要求在服务端就成立，不依赖前端隐藏按钮。
 */
@Tag(name = "50 我的内容", description = "需登录；只能看到自己的数据")
@RequireLogin
@RestController
@RequestMapping("/api/my")
public class MyController {

    private final ArticleService articleService;
    private final CommentService commentService;
    private final InteractionService interactionService;

    public MyController(ArticleService articleService, CommentService commentService,
                        InteractionService interactionService) {
        this.articleService = articleService;
        this.commentService = commentService;
        this.interactionService = interactionService;
    }

    @Operation(summary = "我的文章", description = "可按状态筛选；返回体里带驳回原因和下架原因")
    @GetMapping("/articles")
    public ApiResult<PageVo<ArticleVo.Card>> articles(
            @Parameter(description = "不填=全部") @RequestParam(required = false) ArticleStatus status,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResult.ok(articleService.myArticles(Principal.Holder.requireUserId(), status, current, size));
    }

    @Operation(summary = "查看我的某篇文章（含未发布正文与审核意见）")
    @GetMapping("/articles/{id}")
    public ApiResult<ArticleVo.Detail> article(@PathVariable Long id) {
        return ApiResult.ok(articleService.authorDetail(id, Principal.Holder.requireUserId()));
    }

    @Operation(summary = "我的评论及审核结果")
    @GetMapping("/comments")
    public ApiResult<PageVo<CommentVo.Item>> comments(
            @RequestParam(required = false) CommentStatus status,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResult.ok(commentService.myComments(Principal.Holder.requireUserId(), status, current, size));
    }

    @Operation(summary = "我的收藏")
    @GetMapping("/collects")
    public ApiResult<PageVo<ArticleVo.Card>> collects(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResult.ok(interactionService.myCollects(Principal.Holder.requireUserId(), current, size));
    }
}
