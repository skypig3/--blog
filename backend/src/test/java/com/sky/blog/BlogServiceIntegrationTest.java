package com.sky.blog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.blog.common.BizException;
import com.sky.blog.common.ErrorCode;
import com.sky.blog.dto.AdminDto;
import com.sky.blog.dto.ArticleDto;
import com.sky.blog.dto.AuthDto;
import com.sky.blog.dto.CommentDto;
import com.sky.blog.entity.Article;
import com.sky.blog.entity.Comment;
import com.sky.blog.entity.User;
import com.sky.blog.enums.ArticleStatus;
import com.sky.blog.enums.Role;
import com.sky.blog.enums.UserStatus;
import com.sky.blog.mapper.ArticleMapper;
import com.sky.blog.mapper.CommentMapper;
import com.sky.blog.mapper.UserMapper;
import com.sky.blog.security.Principal;
import com.sky.blog.service.ArticleService;
import com.sky.blog.service.CommentService;
import com.sky.blog.service.InteractionService;
import com.sky.blog.service.StatsService;
import com.sky.blog.service.UserService;
import com.sky.blog.vo.ArticleVo;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * 服务层集成测试：跑真 SQLite、真 MyBatis-Plus、真事务。
 *
 * <p>用<b>独立的库文件</b> {@code target/test-blog.db}，不碰 {@code data/blog.db}，
 * 所以执行测试不会污染演示数据。每个用例带 {@code @Transactional}，跑完自动回滚。
 *
 * <p>执行：{@code mvnw test}（或 IDE 里右键 Run）。
 */
@SpringBootTest
@Transactional
@TestMethodOrder(OrderAnnotation.class)
@DisplayName("服务层集成测试（真库）")
class BlogServiceIntegrationTest {

    private static final Path TEST_DB = Path.of("target", "test-blog.db");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        // 上下文创建前就把上一次的测试库删掉，保证从干净状态开始
        try {
            Files.deleteIfExists(TEST_DB);
            Files.deleteIfExists(Path.of(TEST_DB + "-wal"));
            Files.deleteIfExists(Path.of(TEST_DB + "-shm"));
        } catch (IOException ignored) {
            // 文件被占用时忽略，用例本身带事务回滚
        }
        registry.add("spring.datasource.url",
                () -> "jdbc:sqlite:./target/test-blog.db?foreign_keys=on&busy_timeout=5000&journal_mode=WAL");
    }

    private static final Principal ADMIN = new Principal(1L, "admin", Role.ADMIN);
    private static final Principal MING = new Principal(2L, "xiaoming", Role.USER);
    private static final Principal HONG = new Principal(3L, "xiaohong", Role.USER);

    @Autowired UserService userService;
    @Autowired ArticleService articleService;
    @Autowired CommentService commentService;
    @Autowired InteractionService interactionService;
    @Autowired StatsService statsService;
    @Autowired UserMapper userMapper;
    @Autowired ArticleMapper articleMapper;
    @Autowired CommentMapper commentMapper;

    private static final ArticleDto.Save DRAFT = new ArticleDto.Save(
            "集成测试文章", "", "## 正文\n\n这是集成测试创建的 Markdown 正文。",
            null, null, List.of(), false);
    private static final ArticleDto.Save SUBMIT = new ArticleDto.Save(
            "集成测试文章", "", "## 正文\n\n提交审核。", null, null, List.of(), true);

    private Long newArticle(Principal author, ArticleDto.Save dto) {
        return articleService.create(author.userId(), dto).id();
    }

    // ---------------------------------------------------------------- 账号规则

    @Test
    @Order(1)
    @DisplayName("注册只能产出普通用户，且用户名/邮箱不得重复")
    void registerRules() {
        var created = userService.register(new AuthDto.Register(
                "it_user01", "it01@example.com", "It@12345", "集成用户"));
        assertThat(created.role()).isEqualTo("USER");
        assertThatThrownBy(() -> userService.register(new AuthDto.Register(
                "it_user01", "other@example.com", "It@12345", "重名")))
                .isInstanceOf(BizException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE);
        assertThatThrownBy(() -> userService.register(new AuthDto.Register(
                "it_user02", "IT01@example.com", "It@12345", "重邮箱")))
                .isInstanceOf(BizException.class);
    }

    @Test
    @Order(2)
    @DisplayName("密码存的是 BCrypt 哈希，且改密会让凭据版本号 +1")
    void passwordIsHashedAndVersionBumps() {
        var created = userService.register(new AuthDto.Register(
                "it_pwd01", "pwd01@example.com", "Pwd@12345", "改密测试"));
        User row = userMapper.selectById(created.id());
        assertThat(row.getPasswordHash()).startsWith("$2a$").hasSize(60).isNotEqualTo("Pwd@12345");
        assertThat(row.getCredentialVersion()).isZero();

        // 用 HttpSession 太重，这里直接验证服务层规则：原密码错 -> 拒绝
        var session = new org.springframework.mock.web.MockHttpSession();
        assertThatThrownBy(() -> userService.changePassword(created.id(),
                new AuthDto.PasswordUpdate("WRONG", "New@12345"), session))
                .isInstanceOf(BizException.class);

        userService.changePassword(created.id(),
                new AuthDto.PasswordUpdate("Pwd@12345", "New@12345"), session);
        User after = userMapper.selectById(created.id());
        assertThat(after.getCredentialVersion()).isEqualTo(1);
        assertThat(session.isInvalid()).as("改密后当前会话必须被销毁").isTrue();
    }

    @Test
    @Order(3)
    @DisplayName("系统必须保留至少一个启用状态的管理员")
    void lastAdminIsProtected() {
        assertThat(userMapper.countOtherActiveAdmins(1L))
                .as("初始化数据里 admin(id=1) 是唯一管理员").isZero();

        // 唯一管理员：禁用和降级都被挡，且提示的是"为什么"而不是含糊的"不能动自己"
        assertThatThrownBy(() -> userService.adminSetStatus(ADMIN, 1L,
                new AdminDto.UserStatusUpdate("DISABLED", "试着禁用唯一管理员")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("至少一个启用状态的管理员");
        assertThatThrownBy(() -> userService.adminSetRole(ADMIN, 1L,
                new AdminDto.UserRoleUpdate("USER", "试着降级唯一管理员")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("至少一个启用状态的管理员");

        // 有了第二个管理员之后，禁用自己就只是"不建议"，提示换成自禁用那条
        userService.adminSetRole(ADMIN, 2L, new AdminDto.UserRoleUpdate("ADMIN", "提升小明"));
        assertThatThrownBy(() -> userService.adminSetStatus(ADMIN, 1L,
                new AdminDto.UserStatusUpdate("DISABLED", "有备份了")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不能禁用自己的账号");
        // 但禁用"另一个"管理员是允许的，因为还剩一个
        userService.adminSetStatus(ADMIN, 2L, new AdminDto.UserStatusUpdate("DISABLED", "降权备份"));
        assertThat(userMapper.selectById(2L).getStatus()).isEqualTo(UserStatus.DISABLED);
    }

    // ---------------------------------------------------------------- 文章与审核

    @Test
    @Order(4)
    @DisplayName("草稿对游客不可见，提交并审核通过后才公开")
    void draftToPublished() {
        Long id = newArticle(MING, DRAFT);
        assertThat(articleMapper.selectById(id).getStatus()).isEqualTo(ArticleStatus.DRAFT);
        assertThatThrownBy(() -> articleService.publicDetail(id, HONG))
                .isInstanceOf(BizException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);

        articleService.submit(MING.userId(), id);
        assertThat(articleMapper.selectById(id).getStatus()).isEqualTo(ArticleStatus.PENDING);
        assertThatThrownBy(() -> articleService.publicDetail(id, null))
                .isInstanceOf(BizException.class);

        articleService.audit(ADMIN, id, new ArticleDto.Audit(true, null));
        assertThat(articleMapper.selectById(id).getStatus()).isEqualTo(ArticleStatus.PUBLISHED);
        assertThat(articleMapper.selectById(id).getPublishedAt()).isNotBlank();
        ArticleVo.Detail visible = articleService.publicDetail(id, null);
        assertThat(visible.status()).isEqualTo("PUBLISHED");
    }

    @Test
    @Order(5)
    @DisplayName("重复审核必须失败；作者撤回后管理员再点通过也必须失败（作业 2.3.3）")
    void doubleAuditIsRejected() {
        Long id = newArticle(MING, SUBMIT);

        articleService.audit(ADMIN, id, new ArticleDto.Audit(true, null));
        assertThatThrownBy(() -> articleService.audit(ADMIN, id, new ArticleDto.Audit(true, null)))
                .isInstanceOf(BizException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CONFLICT);

        // 撤回 -> 再提交 -> 撤回 -> 审核：每一步状态都必须对得上
        Long id2 = newArticle(MING, SUBMIT);
        articleService.withdraw(MING.userId(), id2);
        assertThat(articleMapper.selectById(id2).getStatus()).isEqualTo(ArticleStatus.DRAFT);
        assertThatThrownBy(() -> articleService.audit(ADMIN, id2, new ArticleDto.Audit(true, null)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("不是待审核状态");
    }

    @Test
    @Order(6)
    @DisplayName("驳回必须填原因，且原因只有作者能看到")
    void rejectNeedsReason() {
        Long id = newArticle(MING, SUBMIT);
        assertThatThrownBy(() -> articleService.audit(ADMIN, id, new ArticleDto.Audit(false, "  ")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("驳回必须填写原因");

        articleService.audit(ADMIN, id, new ArticleDto.Audit(false, "正文缺少论据"));
        assertThat(articleMapper.selectById(id).getRejectReason()).isEqualTo("正文缺少论据");
        assertThat(articleService.authorDetail(id, MING.userId()).rejectReason()).isEqualTo("正文缺少论据");
        // 别人既看不到文章，也拿不到原因
        assertThatThrownBy(() -> articleService.authorDetail(id, HONG.userId()))
                .isInstanceOf(BizException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_OWNER);
    }

    @Test
    @Order(7)
    @DisplayName("已发布文章被编辑后不再公开（未经审核的内容不得公开展示）")
    void editingPublishedTakesItOffline() {
        Long id = newArticle(MING, SUBMIT);
        articleService.audit(ADMIN, id, new ArticleDto.Audit(true, null));
        assertThat(articleService.publicDetail(id, null).status()).isEqualTo("PUBLISHED");

        articleService.update(MING.userId(), id, new ArticleDto.Save(
                "改过的标题", "摘要", "改过的正文", null, null, List.of(), false));
        assertThat(articleMapper.selectById(id).getStatus()).isEqualTo(ArticleStatus.DRAFT);
        assertThatThrownBy(() -> articleService.publicDetail(id, null))
                .isInstanceOf(BizException.class);
    }

    @Test
    @Order(8)
    @DisplayName("归属校验：非作者不能改、不能看别人的未发布文章")
    void ownershipEnforced() {
        Long id = newArticle(MING, DRAFT);
        assertThatThrownBy(() -> articleService.update(HONG.userId(), id, DRAFT))
                .isInstanceOf(BizException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_OWNER);
        assertThatThrownBy(() -> articleService.submit(HONG.userId(), id))
                .isInstanceOf(BizException.class);
        assertThatThrownBy(() -> articleService.deleteByAuthor(HONG.userId(), id))
                .isInstanceOf(BizException.class);
    }

    @Test
    @Order(9)
    @DisplayName("下架需要原因、只有已发布能下架，作者能看到下架原因")
    void offlineFlow() {
        Long id = newArticle(MING, SUBMIT);
        articleService.audit(ADMIN, id, new ArticleDto.Audit(true, null));
        assertThatThrownBy(() -> articleService.offline(ADMIN, id, new ArticleDto.Offline("   ")))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("下架必须填写原因");
        articleService.offline(ADMIN, id, new ArticleDto.Offline("含违规内容"));
        assertThat(articleMapper.selectById(id).getStatus()).isEqualTo(ArticleStatus.REMOVED);
        assertThatThrownBy(() -> articleService.publicDetail(id, null)).isInstanceOf(BizException.class);
        assertThat(articleService.authorDetail(id, MING.userId()).offlineReason()).isEqualTo("含违规内容");
    }

    @Test
    @Order(10)
    @DisplayName("摘要留空时从正文自动生成，且不会带 Markdown 标记")
    void summaryAutoGenerated() {
        Long id = newArticle(MING, new ArticleDto.Save("摘要测试", null,
                "# 标题\n\n这是第一段正文，用来验证自动摘要会去掉 Markdown 标记并按长度截取。",
                null, null, List.of(), false));
        String summary = articleMapper.selectById(id).getSummary();
        assertThat(summary).isNotBlank().doesNotContain("#").doesNotContain("\n");
    }

    // ---------------------------------------------------------------- 评论

    @Test
    @Order(11)
    @DisplayName("评论与回复都要过审；通过后计数才变；重复审核失败")
    void commentAuditFlow() {
        Long articleId = newArticle(MING, SUBMIT);
        articleService.audit(ADMIN, articleId, new ArticleDto.Audit(true, null));

        var top = commentService.create(articleId, HONG.userId(), new CommentDto.Create("顶层评论", null));
        assertThat(top.status()).isEqualTo("PENDING");
        assertThat(commentService.visibleTree(articleId)).isEmpty();

        commentService.audit(ADMIN, top.id(), new ArticleDto.Audit(true, null));
        assertThat(commentService.visibleTree(articleId)).hasSize(1);
        assertThat(articleMapper.selectById(articleId).getCommentCount()).isEqualTo(1);
        assertThatThrownBy(() -> commentService.audit(ADMIN, top.id(), new ArticleDto.Audit(true, null)))
                .isInstanceOf(BizException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CONFLICT);

        // 回复同样要过审 —— 这就是作业 2.3.4 的"审核应覆盖回复内容"
        var reply = commentService.create(articleId, MING.userId(),
                new CommentDto.Create("回复内容", top.id()));
        assertThat(reply.status()).isEqualTo("PENDING");
        assertThat(commentService.visibleTree(articleId).get(0).children()).isEmpty();
        commentService.audit(ADMIN, reply.id(), new ArticleDto.Audit(true, null));
        assertThat(commentService.visibleTree(articleId).get(0).children()).hasSize(1);
        assertThat(articleMapper.selectById(articleId).getCommentCount()).isEqualTo(2);
    }

    @Test
    @Order(12)
    @DisplayName("驳回评论要填原因，删除已发布评论会把计数扣回去")
    void commentRejectAndDelete() {
        Long articleId = newArticle(MING, SUBMIT);
        articleService.audit(ADMIN, articleId, new ArticleDto.Audit(true, null));
        var c = commentService.create(articleId, HONG.userId(), new CommentDto.Create("待处理", null));

        assertThatThrownBy(() -> commentService.audit(ADMIN, c.id(), new ArticleDto.Audit(false, null)))
                .isInstanceOf(BizException.class);
        commentService.audit(ADMIN, c.id(), new ArticleDto.Audit(false, "与主题无关"));
        assertThat(commentMapper.selectById(c.id()).getRejectReason()).isEqualTo("与主题无关");
        assertThat(articleMapper.selectById(articleId).getCommentCount()).isZero();

        // 误驳回的评论可以被重新放行（否则它永远发不出去）
        commentService.audit(ADMIN, c.id(), new ArticleDto.Audit(true, null));
        assertThat(articleMapper.selectById(articleId).getCommentCount()).isEqualTo(1);
        // 已发布的评论不再属于"可审核状态"，要改动它走删除
        assertThatThrownBy(() -> commentService.audit(ADMIN, c.id(), new ArticleDto.Audit(true, null)))
                .isInstanceOf(BizException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CONFLICT);
        commentService.delete(c.id(), HONG);
        assertThat(articleMapper.selectById(articleId).getCommentCount()).isZero();
    }

    @Test
    @Order(13)
    @DisplayName("未发布的文章不能评论")
    void cannotCommentOnUnpublished() {
        Long articleId = newArticle(MING, DRAFT);
        assertThatThrownBy(() -> commentService.create(articleId, HONG.userId(),
                new CommentDto.Create("评论", null)))
                .isInstanceOf(BizException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
    }

    // ---------------------------------------------------------------- 互动

    @Test
    @Order(14)
    @DisplayName("点赞与收藏是幂等开关，计数不会重复累加")
    void likeAndCollectAreIdempotent() {
        Long articleId = newArticle(MING, SUBMIT);
        articleService.audit(ADMIN, articleId, new ArticleDto.Audit(true, null));

        assertThat(articleMapper.selectById(articleId).getLikeCount()).isZero();
        interactionService.toggleLike(HONG.userId(), articleId);
        interactionService.toggleLike(HONG.userId(), articleId);
        interactionService.toggleLike(HONG.userId(), articleId);
        Article after = articleMapper.selectById(articleId);
        assertThat(after.getLikeCount()).isEqualTo(1);
        assertThat(articleService.publicDetail(articleId, HONG).liked()).isTrue();

        interactionService.toggleCollect(HONG.userId(), articleId);
        assertThat(articleMapper.selectById(articleId).getCollectCount()).isEqualTo(1);
        assertThat(interactionService.myCollects(HONG.userId(), 1, 10).total()).isEqualTo(1);
    }

    // ---------------------------------------------------------------- 统计

    @Test
    @Order(15)
    @DisplayName("后台统计与库里的 COUNT 完全一致（作业 2.3.1）")
    void statsMatchDatabase() {
        var stats = statsService.stats();
        Long sqlUsers = userMapper.selectCount(null);
        Long sqlArticles = articleMapper.selectCount(null);
        Long sqlPending = articleMapper.selectCount(new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, ArticleStatus.PENDING));
        Long sqlComments = commentMapper.selectCount(null);
        assertThat(stats.userCount()).isEqualTo(sqlUsers);
        assertThat(stats.articleCount()).isEqualTo(sqlArticles);
        assertThat(stats.pendingArticleCount()).isEqualTo(sqlPending);
        assertThat(stats.commentCount()).isEqualTo(sqlComments);
        assertThat(stats.totalViewCount()).isNotEqualTo(12345L);
    }

    @Test
    @Order(16)
    @DisplayName("热门查询真的走 SQLite 方言，且只返回已发布内容")
    void hotQueryUsesSqliteDialect() {
        Long id = newArticle(MING, SUBMIT);
        articleService.audit(ADMIN, id, new ArticleDto.Audit(true, null));
        articleMapper.increaseView(id);
        articleMapper.increaseView(id);
        List<ArticleVo.Card> hot = articleService.hot();
        assertThat(hot).isNotEmpty();
        assertThat(hot.stream().anyMatch(a -> a.id().equals(id))).isTrue();
        assertThat(hot).allMatch(a -> "PUBLISHED".equals(a.status()));
    }

}
