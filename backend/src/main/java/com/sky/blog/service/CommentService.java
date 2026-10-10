package com.sky.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.blog.common.BizException;
import com.sky.blog.common.TimeUtil;
import com.sky.blog.dto.ArticleDto;
import com.sky.blog.dto.CommentDto;
import com.sky.blog.entity.Article;
import com.sky.blog.entity.Comment;
import com.sky.blog.entity.User;
import com.sky.blog.enums.ArticleStatus;
import com.sky.blog.enums.AuditAction;
import com.sky.blog.enums.AuditTarget;
import com.sky.blog.enums.CommentStatus;
import com.sky.blog.mapper.ArticleMapper;
import com.sky.blog.mapper.CommentMapper;
import com.sky.blog.security.Principal;
import com.sky.blog.vo.CommentVo;
import com.sky.blog.vo.PageVo;
import com.sky.blog.vo.UserVo;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 评论与回复（作业 2.3.4）。
 *
 * <p>回复和顶层评论共用 {@code t_comment}，用 {@code parentId} 区分，
 * 所以"评论审核应覆盖回复内容"这条要求不需要第二套逻辑：回复走的就是同一张表、同一个审核方法。
 *
 * <p>前台可见条件：文章 PUBLISHED <b>且</b> 评论 PUBLISHED <b>且</b> 未被删除。
 * 父评论被驳回或删除时，其下回复一并隐藏，避免出现"悬空回复"。
 */
@Service
public class CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentService.class);

    private final CommentMapper commentMapper;
    private final ArticleMapper articleMapper;
    private final ArticleService articleService;
    private final UserService userService;
    private final AuditLogService auditLogService;

    public CommentService(CommentMapper commentMapper, ArticleMapper articleMapper,
                          ArticleService articleService, UserService userService,
                          AuditLogService auditLogService) {
        this.commentMapper = commentMapper;
        this.articleMapper = articleMapper;
        this.articleService = articleService;
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    // ============================================================ 前台

    /**
     * 某篇文章下的评论树（只含已发布内容）。
     *
     * <p>一次把这篇文章的已发布评论全查出来，在内存里分两层，避免递归查询。
     */
    @Transactional(readOnly = true)
    public List<CommentVo.Item> visibleTree(Long articleId) {
        List<Comment> rows = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getArticleId, articleId)
                .eq(Comment::getStatus, CommentStatus.PUBLISHED)
                .orderByAsc(Comment::getId));
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<Long, UserVo.Author> authors = userService.authorsByIds(
                rows.stream().map(Comment::getUserId).toList());

        Map<Long, List<Comment>> childrenByParent = new LinkedHashMap<>();
        List<Comment> tops = new ArrayList<>();
        Map<Long, Comment> byId = new LinkedHashMap<>();
        rows.forEach(r -> byId.put(r.getId(), r));

        for (Comment row : rows) {
            Long parentId = row.getParentId();
            // 父评论不在"已发布集合"里（被驳回/删除）时，回复整体不展示
            if (parentId == null) {
                tops.add(row);
            } else if (byId.containsKey(parentId)) {
                childrenByParent.computeIfAbsent(parentId, k -> new ArrayList<>()).add(row);
            }
        }
        tops.sort(Comparator.comparing(Comment::getId).reversed());

        return tops.stream().map(top -> new CommentVo.Item(
                top.getId(), top.getArticleId(), null, top.getParentId(),
                authors.getOrDefault(top.getUserId(), new UserVo.Author(top.getUserId(), "已注销用户", null)),
                top.getContent(), top.getStatus().name(), null, top.getCreatedAt(),
                childrenByParent.getOrDefault(top.getId(), List.of()).stream()
                        .map(c -> new CommentVo.Item(c.getId(), c.getArticleId(), null, c.getParentId(),
                                authors.getOrDefault(c.getUserId(),
                                        new UserVo.Author(c.getUserId(), "已注销用户", null)),
                                c.getContent(), c.getStatus().name(), null, c.getCreatedAt(), List.of()))
                        .toList()))
                .toList();
    }

    /** 发表评论或回复。要求登录，且文章必须已发布。 */
    @Transactional(rollbackFor = Exception.class)
    public CommentVo.Item create(Long articleId, Long userId, CommentDto.Create dto) {
        Article article = articleMapper.selectById(articleId);
        if (article == null || article.getStatus() != ArticleStatus.PUBLISHED) {
            throw BizException.notFound();
        }
        if (dto.parentId() != null) {
            Comment parent = commentMapper.selectById(dto.parentId());
            if (parent == null || !parent.getArticleId().equals(articleId)) {
                throw new BizException(com.sky.blog.common.ErrorCode.BAD_REQUEST, "被回复的评论不存在");
            }
            if (parent.getStatus() != CommentStatus.PUBLISHED) {
                throw BizException.unprocessable("该评论尚未通过审核，不能回复");
            }
            if (parent.getParentId() != null) {
                throw BizException.unprocessable("只支持两级评论，不能回复一条回复");
            }
        }

        Comment row = new Comment();
        row.setArticleId(articleId);
        row.setParentId(dto.parentId());
        row.setUserId(userId);
        row.setContent(dto.content().trim());
        row.setStatus(CommentStatus.PENDING);
        row.setDeleted(0);
        commentMapper.insert(row);
        log.info("新评论 id={} articleId={} userId={} status=PENDING", row.getId(), articleId, userId);
        return toItem(row, UserVo.Author.of(userService.mustFind(userId)), article.getTitle());
    }

    /** 删除评论：作者本人或管理员可删；删的是已发布评论时同步扣减文章计数 */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Principal actor) {
        Comment row = mustFind(id);
        boolean owner = row.getUserId().equals(actor.userId());
        if (!owner && !actor.isAdmin()) {
            throw BizException.notOwner();
        }
        commentMapper.deleteById(id);
        if (row.getStatus() == CommentStatus.PUBLISHED) {
            articleService.changeCounter(ArticleService.COL_COMMENT, row.getArticleId(), -1);
        }
        if (actor.isAdmin()) {
            auditLogService.record(actor, AuditAction.COMMENT_DELETE, AuditTarget.COMMENT,
                    id, "评论#" + id, "管理员删除");
        }
    }

    // ============================================================ 个人中心

    @Transactional(readOnly = true)
    public PageVo<CommentVo.Item> myComments(Long userId, CommentStatus status, long current, long size) {
        LambdaQueryWrapper<Comment> q = new LambdaQueryWrapper<Comment>().eq(Comment::getUserId, userId);
        if (status != null) {
            q.eq(Comment::getStatus, status);
        }
        q.orderByDesc(Comment::getId);
        Page<Comment> page = commentMapper.selectPage(new Page<>(current, Math.min(size, 50)), q);
        return new PageVo<>(toItems(page.getRecords(), true), page.getTotal(),
                page.getCurrent(), page.getSize(), page.getPages());
    }

    // ============================================================ 后台

    @Transactional(readOnly = true)
    public PageVo<CommentVo.Item> adminList(Long userId, String keyword, CommentStatus status,
                                            Long articleId, long current, long size) {
        LambdaQueryWrapper<Comment> q = new LambdaQueryWrapper<>();
        if (userId != null) {
            q.eq(Comment::getUserId, userId);
        }
        if (articleId != null) {
            q.eq(Comment::getArticleId, articleId);
        }
        if (status != null) {
            q.eq(Comment::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            q.like(Comment::getContent, keyword.trim());
        }
        q.orderByDesc(Comment::getId);
        Page<Comment> page = commentMapper.selectPage(new Page<>(current, Math.min(size, 100)), q);
        return new PageVo<>(toItems(page.getRecords(), true), page.getTotal(),
                page.getCurrent(), page.getSize(), page.getPages());
    }

    @Transactional(readOnly = true)
    public CommentVo.Item adminDetail(Long id) {
        Comment row = mustFind(id);
        Article article = articleMapper.selectById(row.getArticleId());
        return toItem(row, UserVo.Author.of(userService.mustFind(row.getUserId())),
                article == null ? "（文章已删除）" : article.getTitle());
    }

    /** 后台首页的"待处理审核任务"：取最近 N 条待审核评论 */
    @Transactional(readOnly = true)
    public List<CommentVo.Item> adminListRaw(Long userId, String keyword, CommentStatus status,
                                             Long articleId, int limit) {
        LambdaQueryWrapper<Comment> q = new LambdaQueryWrapper<>();
        if (userId != null) {
            q.eq(Comment::getUserId, userId);
        }
        if (articleId != null) {
            q.eq(Comment::getArticleId, articleId);
        }
        if (status != null) {
            q.eq(Comment::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            q.like(Comment::getContent, keyword.trim());
        }
        q.orderByDesc(Comment::getId).last("LIMIT " + Math.max(1, Math.min(limit, 50)));
        return toItems(commentMapper.selectList(q), true);
    }

    /**
     * 审核评论（作业 2.3.4：审核结果必须影响前台展示，并记录操作人、结果和时间；驳回应填写原因）。
     *
     * <p>同样用 CAS：{@code WHERE status = 'PENDING'}，重复点击第二次影响 0 行 → 409。
     */
    @Transactional(rollbackFor = Exception.class)
    public CommentVo.Item audit(Principal operator, Long id, ArticleDto.Audit dto) {
        Comment row = mustFind(id);
        // 待审核和已驳回都可以审核：被误驳回的评论否则永远发不出去（作业没要求，但这是缺口）
        if (!CommentStatus.AUDITABLE.contains(row.getStatus())) {
            throw BizException.conflict("该评论当前不是可审核状态，请刷新后重试");
        }
        boolean pass = Boolean.TRUE.equals(dto.pass());
        if (!pass && (dto.reason() == null || dto.reason().isBlank())) {
            throw BizException.unprocessable("驳回必须填写原因");
        }
        CommentStatus from = row.getStatus();
        CommentStatus target = pass ? CommentStatus.PUBLISHED : CommentStatus.REJECTED;
        // set(...) 的参数是提前求值的，通过时 reason 为 null，先做空值归一再交给 wrapper
        String reason = dto.reason() == null ? null : dto.reason().trim();
        String now = TimeUtil.now();

        int rows = commentMapper.update(null, new LambdaUpdateWrapper<Comment>()
                .eq(Comment::getId, id)
                .eq(Comment::getStatus, from)                      // ← CAS 条件：进入时的状态
                .set(Comment::getStatus, target)
                .set(Comment::getAuditUserId, operator.userId())
                .set(Comment::getAuditedAt, now)
                .set(!pass, Comment::getRejectReason, reason));
        if (rows == 0) {
            throw BizException.conflict("该评论状态已变化，请刷新后重试");
        }
        if (pass) {
            articleService.changeCounter(ArticleService.COL_COMMENT, row.getArticleId(), +1);
        }
        auditLogService.record(operator,
                pass ? AuditAction.COMMENT_APPROVE : AuditAction.COMMENT_REJECT,
                AuditTarget.COMMENT, id, "评论#" + id, pass ? null : reason);
        log.info("评论审核 id={} 结果={} 操作者={}", id, target, operator.username());
        return adminDetail(id);
    }

    // ============================================================ 内部

    private Comment mustFind(Long id) {
        Comment row = id == null ? null : commentMapper.selectById(id);
        if (row == null) {
            throw BizException.notFound();
        }
        return row;
    }

    private List<CommentVo.Item> toItems(List<Comment> rows, boolean withArticleTitle) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<Long, UserVo.Author> authors = userService.authorsByIds(
                rows.stream().map(Comment::getUserId).toList());
        Map<Long, String> titles = Map.of();
        if (withArticleTitle) {
            List<Long> ids = rows.stream().map(Comment::getArticleId).distinct().toList();
            titles = articleMapper.selectBatchIds(ids).stream()
                    .collect(LinkedHashMap::new, (m, a) -> m.put(a.getId(), a.getTitle()), Map::putAll);
        }
        Map<Long, String> finalTitles = titles;
        return rows.stream()
                .map(r -> toItem(r, authors.getOrDefault(r.getUserId(),
                        new UserVo.Author(r.getUserId(), "已注销用户", null)),
                        finalTitles.get(r.getArticleId())))
                .toList();
    }

    private CommentVo.Item toItem(Comment row, UserVo.Author author, String articleTitle) {
        return new CommentVo.Item(row.getId(), row.getArticleId(), articleTitle, row.getParentId(),
                author, row.getContent(),
                row.getStatus() == null ? null : row.getStatus().name(),
                row.getRejectReason(), row.getCreatedAt(), List.of());
    }
}
