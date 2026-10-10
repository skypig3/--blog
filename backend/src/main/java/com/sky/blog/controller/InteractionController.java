package com.sky.blog.controller;

import com.sky.blog.common.ApiResult;
import com.sky.blog.security.Principal;
import com.sky.blog.security.RequireLogin;
import com.sky.blog.service.InteractionService;
import com.sky.blog.vo.Toggle;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 点赞与收藏。都是开关语义：再点一次就是取消。 */
@Tag(name = "40 点赞与收藏", description = "需登录")
@RequireLogin
@RestController
@RequestMapping("/api/articles/{articleId}")
public class InteractionController {

    private final InteractionService interactionService;

    public InteractionController(InteractionService interactionService) {
        this.interactionService = interactionService;
    }

    @Operation(summary = "点赞/取消点赞",
            description = "表上有 UNIQUE(user_id, article_id)，重复点击不会产生第二条记录，也就刷不了计数")
    @PostMapping("/like")
    public ApiResult<Toggle> like(@PathVariable Long articleId) {
        Toggle t = interactionService.toggleLike(Principal.Holder.requireUserId(), articleId);
        return ApiResult.ok(t, t.active() ? "已点赞" : "已取消点赞");
    }

    @Operation(summary = "收藏/取消收藏")
    @PostMapping("/collect")
    public ApiResult<Toggle> collect(@PathVariable Long articleId) {
        Toggle t = interactionService.toggleCollect(Principal.Holder.requireUserId(), articleId);
        return ApiResult.ok(t, t.active() ? "已收藏" : "已取消收藏");
    }
}
