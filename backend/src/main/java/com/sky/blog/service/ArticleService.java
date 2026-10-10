package com.sky.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.blog.common.BizException;
import com.sky.blog.common.TimeUtil;
import com.sky.blog.config.BlogProperties;
import com.sky.blog.dto.ArticleDto;
import com.sky.blog.entity.Article;
import com.sky.blog.entity.Category;
import com.sky.blog.entity.User;
import com.sky.blog.enums.ArticleStatus;
import com.sky.blog.enums.AuditAction;
import com.sky.blog.enums.AuditTarget;
import com.sky.blog.mapper.ArticleCollectMapper;
import com.sky.blog.mapper.ArticleLikeMapper;
import com.sky.blog.mapper.ArticleMapper;
import com.sky.blog.mapper.ArticleTagMapper;
import com.sky.blog.security.Principal;
import com.sky.blog.vo.ArticleVo;
import com.sky.blog.vo.PageVo;
import com.sky.blog.vo.UserVo;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 文章域：创作、状态流转、审核、下架、前台查询。
 *
 * <p>作业 6.2 要求指明"文章审核"的关键逻辑位置：状态流转在本类的
 * {@link #audit} / {@link #offline} / {@link #submit} / {@link #withdraw}，
 * 数据归属校验在 {@link #assertOwned}。
 *
 * <p><b>所有状态变更都用 CAS 写法</b>：把"当前状态"写进 UPDATE 的 WHERE 里，
 * 影响行数为 0 就说明状态已被别人改过。这样才满足作业 2.3.3 的要求
 * "审核只对当前待审核内容生效，不得通过重复点击将已撤回或已删除的文章错误发布"。
 * 实测：同一条 PENDING 记录连点两次通过，第一次影响 1 行、第二次 0 行。
 */
@Service
public class ArticleService {

    private static final Logger log = LoggerFactory.getLogger(ArticleService.class);
    private static final int SUMMARY_MAX = 120;

    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;
    private final ArticleLikeMapper likeMapper;
    private final ArticleCollectMapper collectMapper;
    private final UserService userService;
    private final TaxonomyService taxonomyService;
    private final AuditLogService auditLogService;
    private final BlogProperties properties;

    public ArticleService(ArticleMapper articleMapper, ArticleTagMapper articleTagMapper,
                          ArticleLikeMapper likeMapper, ArticleCollectMapper collectMapper,
                          UserService userService, TaxonomyService taxonomyService,
                          AuditLogService auditLogService, BlogProperties properties) {
        this.articleMapper = articleMapper;
        this.articleTagMapper = articleTagMapper;
        this.likeMapper = likeMapper;
        this.collectMapper = collectMapper;
        this.userService = userService;
        this.taxonomyService = taxonomyService;
        this.auditLogService = auditLogService;
        this.properties = properties;
    }

    // ============================================================ 前台查询

    /**
     * 公开列表：只返回 PUBLISHED 且未删除的文章，按发布时间倒序分页。
     *
     * <p>热门区不在这里翻页 —— 它是一条带加权表达式的独立查询，走 {@link #hot()} 和
     * {@code GET /api/articles/hot}，避免"热门"这种固定条目的区块被硬塞进分页语义里。
     *
     * @param sort 目前只有 latest（默认）；保留参数是为了接口向前兼容
     */
    @Transactional(readOnly = true)
    public PageVo<ArticleVo.Card> publicList(String keyword, Long categoryId, Long tagId,
                                             String sort, long current, long size) {
        LambdaQueryWrapper<Article> q = new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, ArticleStatus.PUBLISHED);
        applyFilters(q, keyword, categoryId, tagId);
        q.orderByDesc(Article::getPublishedAt).orderByDesc(Article::getId);
        Page<Article> page = articleMapper.selectPage(new Page<>(current, Math.min(size, 50)), q);
        return new PageVo<>(toCards(page.getRecords()), page.getTotal(),
                page.getCurrent(), page.getSize(), page.getPages());
    }

    /** 首页热门区 */
    @Transactional(readOnly = true)
    public List<ArticleVo.Card> hot() {
        List<Article> rows = articleMapper.selectHot(
                TimeUtil.daysAgo(properties.getHotWindowDays()),
                properties.getHotWeightView(), properties.getHotWeightLike(),
                properties.getHotWeightCollect(), properties.getHotWeightComment(),
                properties.getHotLimit());
        if (rows.isEmpty()) {
            // 兜底：新库里 30 天内可能一篇都没有，退化成"最新 N 篇"，避免首页空区
            rows = articleMapper.selectList(new LambdaQueryWrapper<Article>()
                    .eq(Article::getStatus, ArticleStatus.PUBLISHED)
                    .orderByDesc(Article::getPublishedAt).orderByDesc(Article::getId)
                    .last("LIMIT " + properties.getHotLimit()));
        }
        return toCards(rows);
    }

    /**
     * 公开详情。
     *
     * <p>只有 PUBLISHED 对游客开放；作者本人可以预览自己的其它状态（返回体里带 status）。
     * 阅读量用单条 SQL 原子自增，不做"读—改—写"。作者本人预览不计阅读量。
     */
    @Transactional(rollbackFor = Exception.class)
    public ArticleVo.Detail publicDetail(Long id, Principal viewer) {
        Article article = mustFind(id);
        boolean owner = viewer != null && viewer.userId().equals(article.getAuthorId());
        boolean admin = viewer != null && viewer.isAdmin();
        if (!owner && !admin) {
            if (article.getStatus() != ArticleStatus.PUBLISHED) {
                throw BizException.notFound();
            }
            articleMapper.increaseView(id);
            article.setViewCount(nz(article.getViewCount()) + 1);
        }
        return toDetail(article, viewer, owner);
    }

    // ============================================================ 作者侧

    /** 我的文章列表，可按状态筛选 */
    @Transactional(readOnly = true)
    public PageVo<ArticleVo.Card> myArticles(Long userId, ArticleStatus status,
                                             long current, long size) {
        LambdaQueryWrapper<Article> q = new LambdaQueryWrapper<Article>()
                .eq(Article::getAuthorId, userId);
        if (status != null) {
            q.eq(Article::getStatus, status);
        }
        q.orderByDesc(Article::getUpdatedAt).orderByDesc(Article::getId);
        Page<Article> page = articleMapper.selectPage(new Page<>(current, Math.min(size, 50)), q);
        return new PageVo<>(toCards(page.getRecords()), page.getTotal(),
                page.getCurrent(), page.getSize(), page.getPages());
    }

    /** 作者查看自己的文章（含未发布状态的完整正文） */
    @Transactional(readOnly = true)
    public ArticleVo.Detail authorDetail(Long id, Long userId) {
        Article article = mustFind(id);
        assertOwned(article, userId);
        return toDetail(article, null, true);
    }

    @Transactional(rollbackFor = Exception.class)
    public ArticleVo.Detail create(Long userId, ArticleDto.Save dto) {
        taxonomyService.assertIdsExist(dto.categoryId(), dto.tagIds());
        Article article = new Article();
        article.setAuthorId(userId);
        article.setCategoryId(dto.categoryId());
        article.setTitle(dto.title().trim());
        article.setSummary(resolveSummary(dto.summary(), dto.contentMd()));
        article.setContentMd(dto.contentMd());
        article.setCover(trimToNull(dto.cover()));
        boolean submit = Boolean.TRUE.equals(dto.submit());
        article.setStatus(submit ? ArticleStatus.PENDING : ArticleStatus.DRAFT);
        article.setViewCount(0);
        article.setLikeCount(0);
        article.setCollectCount(0);
        article.setCommentCount(0);
        article.setDeleted(0);
        if (submit) {
            article.setSubmittedAt(TimeUtil.now());
        }
        articleMapper.insert(article);
        replaceTags(article.getId(), dto.tagIds());
        log.info("新建文章 id={} authorId={} status={}", article.getId(), userId, article.getStatus());
        return toDetail(article, null, true);
    }

    /**
     * 编辑文章。
     *
     * <p>设计决策（设计文档 §9 第 3 条）：<b>已发布文章被编辑后不再保持公开</b>，
     * 因为作业 1.4 明确"未经审核的内容不得公开展示"。
     * 所以编辑后按 submit 参数回到 DRAFT 或 PENDING，需要重新过审才会再出现在前台。
     */
    @Transactional(rollbackFor = Exception.class)
    public ArticleVo.Detail update(Long userId, Long id, ArticleDto.Save dto) {
        Article article = mustFind(id);
        assertOwned(article, userId);
        if (!article.getStatus().can(ArticleStatus.Transition.EDIT)) {
            throw BizException.unprocessable("当前状态（" + article.getStatus() + "）不允许编辑");
        }
        taxonomyService.assertIdsExist(dto.categoryId(), dto.tagIds());

        boolean submit = Boolean.TRUE.equals(dto.submit());
        ArticleStatus before = article.getStatus();
        article.setCategoryId(dto.categoryId());
        article.setTitle(dto.title().trim());
        article.setSummary(resolveSummary(dto.summary(), dto.contentMd()));
        article.setContentMd(dto.contentMd());
        article.setCover(trimToNull(dto.cover()));
        article.setStatus(submit ? ArticleStatus.PENDING : ArticleStatus.DRAFT);
        article.setUpdatedAt(TimeUtil.now());
        if (submit) {
            article.setSubmittedAt(TimeUtil.now());
            article.setRejectReason(null);
        }
        int rows = articleMapper.update(article, new LambdaUpdateWrapper<Article>()
                .eq(Article::getId, id)
                .eq(Article::getStatus, before));
        if (rows == 0) {
            throw BizException.conflict("文章状态刚刚发生变化，请刷新后重试");
        }
        replaceTags(id, dto.tagIds());
        return toDetail(article, null, true);
    }

    /** 作者删除自己的文章：逻辑删除，数据保留 */
    @Transactional(rollbackFor = Exception.class)
    public void deleteByAuthor(Long userId, Long id) {
        Article article = mustFind(id);
        assertOwned(article, userId);
        articleMapper.deleteById(id);
        log.info("作者删除文章 id={} authorId={}", id, userId);
    }

    /** 提交审核：DRAFT / REJECTED / REMOVED → PENDING */
    @Transactional(rollbackFor = Exception.class)
    public ArticleVo.Detail submit(Long userId, Long id) {
        Article article = mustFind(id);
        assertOwned(article, userId);
        article.getStatus().assertCan(ArticleStatus.Transition.SUBMIT);
        int rows = articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                .eq(Article::getId, id)
                .eq(Article::getStatus, article.getStatus())
                .set(Article::getStatus, ArticleStatus.PENDING)
                .set(Article::getSubmittedAt, TimeUtil.now())
                .set(Article::getRejectReason, null)
                .set(Article::getOfflineReason, null)
                .set(Article::getUpdatedAt, TimeUtil.now()));
        if (rows == 0) {
            throw BizException.conflict("文章状态刚刚发生变化，请刷新后重试");
        }
        return authorDetail(id, userId);
    }

    /** 撤回：PENDING → DRAFT。撤回后管理员再点"通过"必须失败，这正是 CAS 要挡的场景。 */
    @Transactional(rollbackFor = Exception.class)
    public ArticleVo.Detail withdraw(Long userId, Long id) {
        Article article = mustFind(id);
        assertOwned(article, userId);
        article.getStatus().assertCan(ArticleStatus.Transition.WITHDRAW);
        int rows = articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                .eq(Article::getId, id)
                .eq(Article::getStatus, ArticleStatus.PENDING)
                .set(Article::getStatus, ArticleStatus.DRAFT)
                .set(Article::getUpdatedAt, TimeUtil.now()));
        if (rows == 0) {
            throw BizException.conflict("该文章已不在待审核状态，撤回失败");
        }
        return authorDetail(id, userId);
    }

    // ============================================================ 后台审核

    @Transactional(readOnly = true)
    public PageVo<ArticleVo.Card> adminList(Long authorId, Long categoryId, ArticleStatus status,
                                            String keyword, long current, long size) {
        LambdaQueryWrapper<Article> q = new LambdaQueryWrapper<>();
        if (authorId != null) {
            q.eq(Article::getAuthorId, authorId);
        }
        if (categoryId != null) {
            q.eq(Article::getCategoryId, categoryId);
        }
        if (status != null) {
            q.eq(Article::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            q.like(Article::getTitle, keyword.trim());
        }
        q.orderByDesc(Article::getId);
        Page<Article> page = articleMapper.selectPage(new Page<>(current, Math.min(size, 100)), q);
        return new PageVo<>(toCards(page.getRecords()), page.getTotal(),
                page.getCurrent(), page.getSize(), page.getPages());
    }

    /** 管理员查看任意状态的文章全文（含待审核正文） */
    @Transactional(readOnly = true)
    public ArticleVo.Detail adminDetail(Long id) {
        return toDetail(mustFind(id), null, true);
    }

    /**
     * 审核文章（作业 2.3.3）。
     *
     * <p>驳回必须填原因，且原因要让作者看得到；通过则写入 publishedAt 供排序使用。
     */
    @Transactional(rollbackFor = Exception.class)
    public ArticleVo.Detail audit(Principal operator, Long id, ArticleDto.Audit dto) {
        Article article = mustFind(id);
        if (article.getStatus() != ArticleStatus.PENDING) {
            throw BizException.conflict("该文章当前不是待审核状态，请刷新后重试");
        }
        boolean pass = Boolean.TRUE.equals(dto.pass());
        if (!pass && (dto.reason() == null || dto.reason().isBlank())) {
            throw BizException.unprocessable("驳回必须填写原因");
        }
        // 注意：LambdaUpdateWrapper 的 set(...) 参数是<b>提前求值</b>的，
        // 通过时 reason 是 null，直接写 dto.reason().trim() 会 NPE —— 先做空值归一。
        String reason = dto.reason() == null ? null : dto.reason().trim();
        ArticleStatus target = pass ? ArticleStatus.PUBLISHED : ArticleStatus.REJECTED;
        String now = TimeUtil.now();

        int rows = articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                .eq(Article::getId, id)
                .eq(Article::getStatus, ArticleStatus.PENDING)   // ← CAS 条件，防重复点击
                .set(Article::getStatus, target)
                .set(Article::getAuditUserId, operator.userId())
                .set(Article::getAuditedAt, now)
                .set(pass, Article::getPublishedAt, now)
                .set(pass, Article::getRejectReason, null)
                .set(!pass, Article::getRejectReason, reason)
                .set(Article::getUpdatedAt, now));
        if (rows == 0) {
            throw BizException.conflict("该文章已被其他操作改变状态，请刷新后重试");
        }
        auditLogService.record(operator,
                pass ? AuditAction.ARTICLE_APPROVE : AuditAction.ARTICLE_REJECT,
                AuditTarget.ARTICLE, id, article.getTitle(), pass ? null : reason);
        log.info("文章审核 id={} 结果={} 操作者={}", id, target, operator.username());
        return toDetail(mustFind(id), null, true);
    }

    /** 下架违规文章（作业 2.3.3：必须填写下架原因） */
    @Transactional(rollbackFor = Exception.class)
    public ArticleVo.Detail offline(Principal operator, Long id, ArticleDto.Offline dto) {
        Article article = mustFind(id);
        if (article.getStatus() != ArticleStatus.PUBLISHED) {
            throw BizException.conflict("只有已发布的文章才需要下架");
        }
        // 作业 2.3.3 要求下架必须写原因。@Valid 只在 Controller 入口生效，
        // 这里再挡一道：Service 被别的调用方复用时规则同样成立。
        if (dto.reason() == null || dto.reason().isBlank()) {
            throw BizException.unprocessable("下架必须填写原因");
        }
        String now = TimeUtil.now();
        int rows = articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                .eq(Article::getId, id)
                .eq(Article::getStatus, ArticleStatus.PUBLISHED)
                .set(Article::getStatus, ArticleStatus.REMOVED)
                .set(Article::getOfflineReason, dto.reason().trim())
                .set(Article::getAuditUserId, operator.userId())
                .set(Article::getAuditedAt, now)
                .set(Article::getUpdatedAt, now));
        if (rows == 0) {
            throw BizException.conflict("该文章状态已变化，请刷新后重试");
        }
        auditLogService.record(operator, AuditAction.ARTICLE_OFFLINE, AuditTarget.ARTICLE,
                id, article.getTitle(), dto.reason().trim());
        return toDetail(mustFind(id), null, true);
    }

    /** 管理员删除文章 */
    @Transactional(rollbackFor = Exception.class)
    public void adminDelete(Principal operator, Long id, String reason) {
        Article article = mustFind(id);
        articleMapper.deleteById(id);
        auditLogService.record(operator, AuditAction.ARTICLE_DELETE, AuditTarget.ARTICLE,
                id, article.getTitle(), reason);
    }

    // ============================================================ 内部工具

    private void applyFilters(LambdaQueryWrapper<Article> q, String keyword, Long categoryId, Long tagId) {
        if (keyword != null && !keyword.isBlank()) {
            q.like(Article::getTitle, keyword.trim());
        }
        if (categoryId != null) {
            q.eq(Article::getCategoryId, categoryId);
        }
        if (tagId != null) {
            // 标签是多对多，用子查询过滤，避免"先查文章 id 列表再拼 IN"可能超长的问题。
            // tagId 已由 Spring 转成 Long，非数字在参数绑定阶段就被拒了，不存在注入面。
            q.inSql(Article::getId,
                    "SELECT article_id FROM t_article_tag WHERE tag_id = " + tagId);
        }
    }

    /**
     * 数据归属校验（作业 1.5"数据归属校验"、6.2 要求指明位置）。
     *
     * <p>规则：当前登录用户必须就是作者，管理员例外。
     * 用户 id 一律从 Session 解析出来的 {@link Principal} 取，<b>不接受前端传参</b>。
     */
    private void assertOwned(Article article, Long userId) {
        if (userId == null || !userId.equals(article.getAuthorId())) {
            throw BizException.notOwner();
        }
    }

    private Article mustFind(Long id) {
        Article article = id == null ? null : articleMapper.selectById(id);
        if (article == null) {
            throw BizException.notFound();
        }
        return article;
    }

    private void replaceTags(Long articleId, List<Long> tagIds) {
        articleTagMapper.deleteByArticle(articleId);
        List<Long> distinct = tagIds == null ? List.of()
                : tagIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (!distinct.isEmpty()) {
            articleTagMapper.insertBatch(articleId, distinct);
        }
    }

    /** 摘要留空时，从 Markdown 正文里去掉常见标记后截取前 120 字 */
    private String resolveSummary(String summary, String contentMd) {
        if (summary != null && !summary.isBlank()) {
            return summary.trim();
        }
        String plain = contentMd == null ? "" : contentMd
                .replaceAll("(?s)```.*?```", " ")
                .replaceAll("[#>*_`~\\[\\]()!-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return plain.length() <= SUMMARY_MAX ? plain : plain.substring(0, SUMMARY_MAX) + "…";
    }

    /** 列表批量转 VO：作者、分类、标签各自一次批量查询，避免 N+1。收藏列表等场景也复用它。 */
    public List<ArticleVo.Card> toCards(List<Article> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<Long, UserVo.Author> authors = userService.authorsByIds(rows.stream().map(Article::getAuthorId).toList());
        Map<Long, Category> categories = taxonomyService.categoriesByIds(
                rows.stream().map(Article::getCategoryId).toList());
        Map<Long, List<String>> tags = taxonomyService.tagNamesByArticleIds(
                rows.stream().map(Article::getId).toList());

        return rows.stream().map(a -> new ArticleVo.Card(
                a.getId(), a.getTitle(), a.getSummary(), a.getCover(),
                authors.getOrDefault(a.getAuthorId(), new UserVo.Author(a.getAuthorId(), "已注销用户", null)),
                a.getCategoryId(),
                a.getCategoryId() == null ? null
                        : (categories.get(a.getCategoryId()) == null ? null : categories.get(a.getCategoryId()).getName()),
                tags.getOrDefault(a.getId(), List.of()),
                a.getStatus() == null ? null : a.getStatus().name(),
                a.getPublishedAt(), a.getCreatedAt(),
                nz(a.getViewCount()), nz(a.getLikeCount()), nz(a.getCollectCount()), nz(a.getCommentCount())))
                .toList();
    }

    private ArticleVo.Detail toDetail(Article a, Principal viewer, boolean includePrivate) {
        Map<Long, User> author = userService.usersByIds(List.of(a.getAuthorId()));
        User authorUser = author.get(a.getAuthorId());
        Map<Long, List<String>> tags = taxonomyService.tagNamesByArticleIds(List.of(a.getId()));
        Category category = a.getCategoryId() == null ? null
                : taxonomyService.categoriesByIds(List.of(a.getCategoryId())).get(a.getCategoryId());
        Long viewerId = viewer == null ? null : viewer.userId();

        Boolean liked = viewerId == null ? null : likeMapper.exists(viewerId, a.getId());
        Boolean collected = viewerId == null ? null : collectMapper.exists(viewerId, a.getId());

        return new ArticleVo.Detail(
                a.getId(), a.getTitle(), a.getSummary(), a.getContentMd(),
                a.getCover(), a.getAuthorId(), UserVo.Author.of(authorUser),
                a.getCategoryId(), category == null ? null : category.getName(),
                tags.getOrDefault(a.getId(), List.of()),
                a.getStatus() == null ? null : a.getStatus().name(),
                includePrivate ? a.getRejectReason() : null,
                includePrivate ? a.getOfflineReason() : null,
                includePrivate ? a.getSubmittedAt() : null,
                includePrivate ? a.getAuditedAt() : null,
                a.getPublishedAt(), a.getCreatedAt(), a.getUpdatedAt(),
                nz(a.getViewCount()), nz(a.getLikeCount()), nz(a.getCollectCount()), nz(a.getCommentCount()),
                liked, collected);
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }

    private static String trimToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    /** 给评论/互动服务用的只读入口 */
    @Transactional(readOnly = true)
    public Article find(Long id) {
        return id == null ? null : articleMapper.selectById(id);
    }

    /** 计数字段名常量：changeCounter 的列参数只能取这三个值，杜绝外部输入拼进 SQL */
    public static final String COL_LIKE = "like_count";
    public static final String COL_COLLECT = "collect_count";
    public static final String COL_COMMENT = "comment_count";

    public void changeCounter(String column, Long articleId, int delta) {
        if (!List.of(COL_LIKE, COL_COLLECT, COL_COMMENT).contains(column)) {
            throw new IllegalArgumentException("非法计数列：" + column);
        }
        articleMapper.changeCounter(column, articleId, delta);
    }
}
