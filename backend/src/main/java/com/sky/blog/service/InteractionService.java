package com.sky.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.blog.common.BizException;
import com.sky.blog.common.TimeUtil;
import com.sky.blog.entity.Article;
import com.sky.blog.entity.ArticleCollect;
import com.sky.blog.entity.ArticleLike;
import com.sky.blog.enums.ArticleStatus;
import com.sky.blog.mapper.ArticleCollectMapper;
import com.sky.blog.mapper.ArticleLikeMapper;
import com.sky.blog.mapper.ArticleMapper;
import com.sky.blog.vo.ArticleVo;
import com.sky.blog.vo.PageVo;
import com.sky.blog.vo.Toggle;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 点赞与收藏（作业 1.4 必做项）。
 *
 * <p>两个动作都是<b>幂等开关</b>：表上有 {@code UNIQUE(user_id, article_id)}，
 * 所以重复点击只会"插入失败 → 改成删除"，不会产生第二行记录，也就刷不了计数。
 *
 * <p>关系表和 {@code t_article} 上的冗余计数在<b>同一个事务</b>里改，
 * 保证不会出现"点赞记录写进去了、计数没加"的半套状态。
 */
@Service
public class InteractionService {

    private final ArticleMapper articleMapper;
    private final ArticleLikeMapper likeMapper;
    private final ArticleCollectMapper collectMapper;
    private final ArticleService articleService;

    public InteractionService(ArticleMapper articleMapper, ArticleLikeMapper likeMapper,
                             ArticleCollectMapper collectMapper, ArticleService articleService) {
        this.articleMapper = articleMapper;
        this.likeMapper = likeMapper;
        this.collectMapper = collectMapper;
        this.articleService = articleService;
    }

    @Transactional(rollbackFor = Exception.class)
    public Toggle toggleLike(Long userId, Long articleId) {
        requirePublished(articleId);
        boolean active;
        if (likeMapper.exists(userId, articleId)) {
            likeMapper.delete(new LambdaQueryWrapper<ArticleLike>()
                    .eq(ArticleLike::getUserId, userId).eq(ArticleLike::getArticleId, articleId));
            active = false;
        } else {
            ArticleLike row = new ArticleLike();
            row.setUserId(userId);
            row.setArticleId(articleId);
            row.setCreatedAt(TimeUtil.now());
            likeMapper.insert(row);
            active = true;
        }
        articleService.changeCounter(ArticleService.COL_LIKE, articleId, active ? +1 : -1);
        return new Toggle(active, current(articleId, true), articleId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Toggle toggleCollect(Long userId, Long articleId) {
        requirePublished(articleId);
        boolean active;
        if (collectMapper.exists(userId, articleId)) {
            collectMapper.delete(new LambdaQueryWrapper<ArticleCollect>()
                    .eq(ArticleCollect::getUserId, userId).eq(ArticleCollect::getArticleId, articleId));
            active = false;
        } else {
            ArticleCollect row = new ArticleCollect();
            row.setUserId(userId);
            row.setArticleId(articleId);
            row.setCreatedAt(TimeUtil.now());
            collectMapper.insert(row);
            active = true;
        }
        articleService.changeCounter(ArticleService.COL_COLLECT, articleId, active ? +1 : -1);
        return new Toggle(active, current(articleId, false), articleId);
    }

    /** 我的收藏列表（按收藏时间倒序） */
    @Transactional(readOnly = true)
    public PageVo<ArticleVo.Card> myCollects(Long userId, long current, long size) {
        Page<ArticleCollect> page = collectMapper.selectPage(
                new Page<>(current, Math.min(size, 50)),
                new LambdaQueryWrapper<ArticleCollect>()
                        .eq(ArticleCollect::getUserId, userId)
                        .orderByDesc(ArticleCollect::getId));
        List<Long> ids = page.getRecords().stream().map(ArticleCollect::getArticleId).toList();
        if (ids.isEmpty()) {
            return PageVo.empty(current, size);
        }
        List<Article> articles = articleMapper.selectBatchIds(ids);
        // 按收藏顺序排（selectBatchIds 不保证顺序）
        List<Article> ordered = ids.stream()
                .map(id -> articles.stream().filter(a -> a.getId().equals(id)).findFirst().orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
        return new PageVo<>(articleService.toCards(ordered), page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    /** 我是否点过赞 / 收藏过，给详情页用 */
    @Transactional(readOnly = true)
    public boolean liked(Long userId, Long articleId) {
        return userId != null && likeMapper.exists(userId, articleId);
    }

    @Transactional(readOnly = true)
    public boolean collected(Long userId, Long articleId) {
        return userId != null && collectMapper.exists(userId, articleId);
    }

    private void requirePublished(Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null || article.getStatus() != ArticleStatus.PUBLISHED) {
            throw BizException.notFound();
        }
    }

    private int current(Long articleId, boolean like) {
        Article article = articleMapper.selectById(articleId);
        Integer v = article == null ? null : (like ? article.getLikeCount() : article.getCollectCount());
        return v == null ? 0 : v;
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }
}
