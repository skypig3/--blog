package com.sky.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.blog.entity.Article;
import com.sky.blog.entity.Comment;
import com.sky.blog.entity.User;
import com.sky.blog.enums.ArticleStatus;
import com.sky.blog.enums.CommentStatus;
import com.sky.blog.mapper.ArticleMapper;
import com.sky.blog.mapper.CommentMapper;
import com.sky.blog.mapper.UserMapper;
import com.sky.blog.vo.AdminVo;
import com.sky.blog.vo.CommentVo;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 后台统计（作业 2.3.1）。
 *
 * <p>作业明确要求"统计数据必须来自实际数据库，不得以写死的数字或静态图表代替"，
 * 所以这里七个数字全部是 COUNT / SUM 查询，代码里没有任何常量。
 *
 * <p>测试方法：改完数据后对比 {@code SELECT COUNT(*) FROM t_article WHERE status='PENDING'}
 * 与接口返回值，两者必须一致。
 */
@Service
public class StatsService {

    private static final int RECENT_LIMIT = 5;

    private final UserMapper userMapper;
    private final ArticleMapper articleMapper;
    private final CommentMapper commentMapper;
    private final ArticleService articleService;
    private final CommentService commentService;

    public StatsService(UserMapper userMapper, ArticleMapper articleMapper, CommentMapper commentMapper,
                        ArticleService articleService, CommentService commentService) {
        this.userMapper = userMapper;
        this.articleMapper = articleMapper;
        this.commentMapper = commentMapper;
        this.articleService = articleService;
        this.commentService = commentService;
    }

    @Transactional(readOnly = true)
    public AdminVo.Stats stats() {
        return new AdminVo.Stats(
                userMapper.selectCount(null),
                articleMapper.selectCount(null),
                articleMapper.selectCount(new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, ArticleStatus.PUBLISHED)),
                articleMapper.selectCount(new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, ArticleStatus.PENDING)),
                commentMapper.selectCount(null),
                commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getStatus, CommentStatus.PENDING)),
                articleMapper.sumViewCount());
    }

    /** 后台首页：统计 + 近期文章 + 待处理审核任务 */
    @Transactional(readOnly = true)
    public AdminVo.Dashboard dashboard() {
        List<Article> recent = articleMapper.selectList(new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, ArticleStatus.PUBLISHED)
                .orderByDesc(Article::getPublishedAt).orderByDesc(Article::getId)
                .last("LIMIT " + RECENT_LIMIT));
        List<Article> pendingArticles = articleMapper.selectList(new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, ArticleStatus.PENDING)
                .orderByAsc(Article::getSubmittedAt).orderByAsc(Article::getId)
                .last("LIMIT " + RECENT_LIMIT));
        AdminVo.Stats stats = stats();
        List<CommentVo.Item> pendingComments = commentService.adminListRaw(
                null, null, CommentStatus.PENDING, null, RECENT_LIMIT);
        return new AdminVo.Dashboard(stats, articleService.toCards(recent),
                articleService.toCards(pendingArticles), pendingComments);
    }

    /** 待审核文章条数，前端角标用 */
    @Transactional(readOnly = true)
    public long pendingArticleCount() {
        return articleMapper.selectCount(new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, ArticleStatus.PENDING));
    }

    @Transactional(readOnly = true)
    public long pendingCommentCount() {
        return commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getStatus, CommentStatus.PENDING));
    }

    @Transactional(readOnly = true)
    public long activeUserCount() {
        return userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getStatus, com.sky.blog.enums.UserStatus.ACTIVE));
    }
}
