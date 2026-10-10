package com.sky.blog.controller;

import com.sky.blog.common.ApiResult;
import com.sky.blog.dto.ArticleDto;
import com.sky.blog.security.Principal;
import com.sky.blog.security.RequireLogin;
import com.sky.blog.service.ArticleService;
import com.sky.blog.vo.ArticleVo;
import com.sky.blog.vo.PageVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 文章的公开读取 + 作者侧创作。 */
@Tag(name = "20 文章", description = "公开浏览与作者创作；后台审核在 92 组")
@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @Operation(summary = "文章列表（公开）",
            description = "只返回已发布文章；支持标题关键词、分类、标签筛选，按发布时间倒序分页")
    @GetMapping
    public ApiResult<PageVo<ArticleVo.Card>> list(
            @Parameter(description = "标题关键词，SQLite 的 LIKE 对英文大小写不敏感") @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false, defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResult.ok(articleService.publicList(keyword, categoryId, tagId, sort, current, size));
    }

    @Operation(summary = "热门文章",
            description = "规则：score = view*1 + like*2 + collect*3 + comment*1，"
                    + "取最近 30 天已发布文章前 10 条；窗口内无文章时退化为最新 10 条")
    @GetMapping("/hot")
    public ApiResult<List<ArticleVo.Card>> hot() {
        return ApiResult.ok(articleService.hot());
    }

    @Operation(summary = "文章详情（公开）",
            description = "非已发布状态对游客返回 404；作者本人可预览自己的未发布内容，"
                    + "且本人预览不增加阅读量")
    @GetMapping("/{id}")
    public ApiResult<ArticleVo.Detail> detail(@PathVariable Long id) {
        return ApiResult.ok(articleService.publicDetail(id, Principal.Holder.current()));
    }

    @RequireLogin
    @Operation(summary = "新建文章",
            description = "submit=false 存为草稿，submit=true 直接进入待审核")
    @PostMapping
    public ApiResult<ArticleVo.Detail> create(@Valid @RequestBody ArticleDto.Save dto) {
        return ApiResult.ok(articleService.create(Principal.Holder.requireUserId(), dto),
                Boolean.TRUE.equals(dto.submit()) ? "已提交审核" : "草稿已保存");
    }

    @RequireLogin
    @Operation(summary = "编辑文章",
            description = "已发布文章被编辑后不再保持公开（作业 1.4：未经审核的内容不得公开展示），"
                    + "按 submit 回到 DRAFT 或 PENDING")
    @PutMapping("/{id}")
    public ApiResult<ArticleVo.Detail> update(@PathVariable Long id, @Valid @RequestBody ArticleDto.Save dto) {
        return ApiResult.ok(articleService.update(Principal.Holder.requireUserId(), id, dto), "已保存");
    }

    @RequireLogin
    @Operation(summary = "删除我的文章", description = "逻辑删除，数据保留在库中")
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        articleService.deleteByAuthor(Principal.Holder.requireUserId(), id);
        return ApiResult.ok(null, "已删除");
    }

    @RequireLogin
    @Operation(summary = "提交审核", description = "DRAFT / REJECTED / REMOVED → PENDING")
    @PostMapping("/{id}/submit")
    public ApiResult<ArticleVo.Detail> submit(@PathVariable Long id) {
        return ApiResult.ok(articleService.submit(Principal.Holder.requireUserId(), id), "已提交审核");
    }

    @RequireLogin
    @Operation(summary = "撤回审核", description = "PENDING → DRAFT；撤回后管理员再点通过会收到 409")
    @PostMapping("/{id}/withdraw")
    public ApiResult<ArticleVo.Detail> withdraw(@PathVariable Long id) {
        return ApiResult.ok(articleService.withdraw(Principal.Holder.requireUserId(), id), "已撤回");
    }
}
