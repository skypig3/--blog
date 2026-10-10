-- ============================================================
--  ⚠⚠⚠ 演示数据脚本：会清空并重建全部业务数据 ⚠⚠
--
--  这个脚本<b>不是</b>自动执行的，需要手动跑一次（README 里有命令）。
--  开头的一组 DELETE 会清掉用户自己产生的数据，只保留 admin/xiaoming/xiaohong
--  三个账号以外的内容也会被清掉，请在演示前用、不要在演示中途用。
--
--  数据规模（对应作业要求能演示到的每种状态）：
--    文章 12 篇 = 7 已发布 + 3 待审核 + 2 被驳回
--    评论 9 条 = 6 已通过（含 1 条回复）+ 2 待审核 + 1 被驳回
--    点赞 8 条 / 收藏 5 条 / 操作日志 6 条
--
--  时间用 datetime('now', '-N day') 生成，所以任何时候导入，
--  "最近 30 天"的热门窗口内都有内容，不会出现首页热门区空白。
--
--  计数一致性：t_article 上的 like_count / collect_count / comment_count
--  与关系表、评论表的行数<b>严格对应</b>，后台统计可以直接用 SQL 复核。
-- ============================================================

DELETE FROM t_article_tag;
DELETE FROM t_article_like;
DELETE FROM t_article_collect;
DELETE FROM t_comment;
DELETE FROM t_audit_log;
DELETE FROM t_article;
DELETE FROM t_tag;
DELETE FROM t_category;
DELETE FROM t_user;

-- ------------------------------------------------------------ 用户（与 seed.sql 保持一致）

INSERT INTO t_user
    (id, username, email, password_hash, nickname, bio, role, status, credential_version, deleted, created_at)
VALUES
    (1, 'admin', 'admin@example.com',
     '$2a$10$xXUFHOksofUz73PFgXEz9uX929eeAm1jD.5ovHm95X//YO4Sd9Any',
     '站长', '本站管理员（演示账号）', 'ADMIN', 'ACTIVE', 0, 0, datetime('now','localtime','-40 day')),
    (2, 'xiaoming', 'xiaoming@example.com',
     '$2a$10$d0OR18sOf66Lg6f/L5ydGeDdLH59R7EqlnUeoJGhpRgK9WzCHClKy',
     '小明', '前端爱好者，正在学 Vue 3', 'USER', 'ACTIVE', 0, 0, datetime('now','localtime','-35 day')),
    (3, 'xiaohong', 'xiaohong@example.com',
     '$2a$10$d0OR18sOf66Lg6f/L5ydGeDdLH59R7EqlnUeoJGhpRgK9WzCHClKy',
     '小红', '后端方向，喜欢折腾数据库', 'USER', 'ACTIVE', 0, 0, datetime('now','localtime','-30 day'));

INSERT INTO t_category (id, name, sort_order) VALUES
    (1, '技术笔记', 10), (2, '项目实战', 20), (3, '读书笔记', 30), (4, '生活随笔', 40);

INSERT INTO t_tag (id, name) VALUES
    (1, 'Vue'), (2, 'Spring Boot'), (3, 'SQLite'),
    (4, 'MyBatis-Plus'), (5, '数据库设计'), (6, '前端');

-- ------------------------------------------------------------ 文章

-- 7 篇已发布（published_at 依次往前推，方便看到排序效果）
INSERT INTO t_article
    (id, author_id, category_id, title, summary, content_md, cover, status,
     view_count, like_count, collect_count, comment_count,
     submitted_at, audit_user_id, audited_at, published_at, created_at, updated_at, deleted)
VALUES
    (101, 2, 1, 'Vue 3 的响应式到底在响应什么',
     '从 reactive 和 ref 的区别入手，说清楚 Proxy 拦截了哪些操作。',
     '## 为什么要关心响应式

写第一个 Vue 项目时我完全不懂为什么 `let count = 0` 改了界面不动，加上 `ref` 就动了。

## 三句话结论

1. `ref` 包了一个带 `.value` 的对象，赋值时触发 setter；
2. `reactive` 用 `Proxy` 拦截整个对象的读写；
3. 解构会丢失响应性，因为拿到的已经是普通值。

```js
const state = reactive({ count: 0 })
let { count } = state   // 这样 count 就不再响应了
```

## 小结

响应式不是魔法，本质就是"在赋值的地方挂一个钩子"。',
     NULL, 'PUBLISHED', 320, 2, 1, 3,
     datetime('now','localtime','-12 day'), 1, datetime('now','localtime','-11 day'),
     datetime('now','localtime','-11 day'), datetime('now','localtime','-12 day'),
     datetime('now','localtime','-11 day'), 0),

    (102, 2, 2, '用 Spring Boot 4 + MyBatis-Plus 写博客后端',
     '记录一次完整的后端搭建：版本怎么定的、踩了哪些坑、分页方言为什么能工作。',
     '## 选型不是比谁新

这次最花时间的是确认 MyBatis-Plus 有没有 Spring Boot 4 专用的 starter。
结论是有：`mybatis-plus-spring-boot4-starter`。

## 三个真实坑

1. `mybatis-plus-jsqlparser` 在 POM 里是 optional，不显式加就没有分页插件；
2. Mapper 不会被自动扫描，必须写 `@MapperScan`；
3. SQLite 不会自动创建父目录，目录不存在时报错还很难看懂。

## 分页方言

MP 里没有 SqliteDialect 这个类，但 `DbType.SQLITE` 落在
`postgresqlSameType()` 名单里，最终走 PostgreDialect 生成 `LIMIT ? OFFSET ?`。',
     NULL, 'PUBLISHED', 210, 3, 2, 1,
     datetime('now','localtime','-9 day'), 1, datetime('now','localtime','-8 day'),
     datetime('now','localtime','-8 day'), datetime('now','localtime','-9 day'),
     datetime('now','localtime','-8 day'), 0),

    (103, 3, 1, 'SQLite 的并发写到底会怎样',
     '两个连接同时写会拿到 SQLITE_BUSY，这不是驱动的问题，是 SQLite 的单写者模型。',
     '## 实验

开两个连接，A 起一个写事务不提交，B 也去写。

## 结果

B 等满 `busy_timeout`（默认 3000 毫秒）之后抛：

```
[SQLITE_BUSY] The database file is locked
```

## 结论

连接池开大不会改变"同一时刻只能有一个写事务"这个事实，
所以这个小项目里 HikariCP 直接把 maximum-pool-size 设成 1。',
     NULL, 'PUBLISHED', 150, 1, 1, 2,
     datetime('now','localtime','-6 day'), 1, datetime('now','localtime','-5 day'),
     datetime('now','localtime','-5 day'), datetime('now','localtime','-6 day'),
     datetime('now','localtime','-5 day'), 0),

    (104, 1, 3, '《重构》读书笔记：代码坏味道的七个信号',
     '注释不是好代码的标志；重复代码、过长函数、发散式变化才是需要盯的东西。',
     '## 让我改变看法的一条

"好的代码自己会解释自己"这句话我以前觉得是口号。

## 七个信号

1. 重复代码
2. 过长函数
3. 过大的类
4. 发散式变化
5. 霰弹式修改
6. 依恋情结
7. 数据泥团

## 实践

每次提交前问自己一句：这段代码三个月后的我读得懂吗。',
     NULL, 'PUBLISHED', 90, 1, 0, 0,
     datetime('now','localtime','-4 day'), 1, datetime('now','localtime','-3 day'),
     datetime('now','localtime','-3 day'), datetime('now','localtime','-4 day'),
     datetime('now','localtime','-3 day'), 0),

    (105, 3, 1, '数据库设计作业：ER 图怎么画才不返工',
     '先定关系再定字段，多对多一定要先想清楚关系表上还要不要挂业务字段。',
     '## 顺序很重要

先画实体和关系，再往里填字段。反过来做一定会返工。

## 多对多

文章和标签是多对多，关系表 `t_article_tag` 用复合主键，
不需要额外的 id。

## 索引

按状态 + 时间的组合索引是列表页的命脉。',
     NULL, 'PUBLISHED', 60, 0, 0, 0,
     datetime('now','localtime','-3 day'), 1, datetime('now','localtime','-2 day'),
     datetime('now','localtime','-2 day'), datetime('now','localtime','-3 day'),
     datetime('now','localtime','-2 day'), 0),

    (106, 2, 4, '一周把 Git 的常用操作理顺',
     'rebase 不是禁忌，但推到共享分支之前必须先想清楚影响半径。',
     '## 我的三条规矩

1. 只在本地未推送的提交上用 rebase；
2. 共享分支一律 merge；
3. 改错了先 `git stash`，别急着 hard reset。

## 最有用的一条

`git log --oneline --graph` 让人对仓库状态有全局感。',
     NULL, 'PUBLISHED', 40, 1, 1, 0,
     datetime('now','localtime','-2 day'), 1, datetime('now','localtime','-1 day'),
     datetime('now','localtime','-1 day'), datetime('now','localtime','-2 day'),
     datetime('now','localtime','-1 day'), 0),

    (107, 3, 2, '课程项目怎么拆里程碑',
     '每个阶段都要有"能演示的东西"，不要把所有可见成果堆到最后一次。',
     '## 拆法

先做骨架和权限，再做内容，最后做展示层。

## 为什么

权限和状态机塌了，后面写多少页面都是白干。',
     NULL, 'PUBLISHED', 25, 0, 0, 0,
     datetime('now','localtime','-1 day'), 1, datetime('now','localtime'),
     datetime('now','localtime'), datetime('now','localtime','-1 day'),
     datetime('now','localtime'), 0);

-- 3 篇待审核（后台"待处理审核任务"里能看到）
INSERT INTO t_article
    (id, author_id, category_id, title, summary, content_md, status,
     view_count, like_count, collect_count, comment_count, submitted_at, created_at, updated_at, deleted)
VALUES
    (108, 2, 1, 'Pinia 和 Vuex 我该学哪个',
     '新项目一律 Pinia，Vuex 只在维护老项目时才需要碰。',
     '## 结论

Pinia 的 API 更小，没有 mutations 这一层。

## 理由

状态管理真正难的是"谁改的"，而不是"改了几层"。',
     'PENDING', 0, 0, 0, 0, datetime('now','localtime','-1 day'),
     datetime('now','localtime','-1 day'), datetime('now','localtime','-1 day'), 0),

    (109, 3, 2, '把接口文档当成测试用例来写',
     '每个接口一份 .http 文件，点一下就能复现，比口头描述"我测过了"可靠。',
     '## 做法

接口文档里写清楚请求、响应示例和典型错误码，
然后照着这些错误码写一遍手工测试。

## 收获

能立刻发现"错误提示写得人看不懂"这类问题。',
     'PENDING', 0, 0, 0, 0, datetime('now','localtime','-1 day'),
     datetime('now','localtime','-1 day'), datetime('now','localtime','-1 day'), 0),

    (110, 2, 4, 'Vite 代理配置的一条经验',
     '开发期用代理做成同源，Cookie 相关的问题会一次性消失。',
     '## 配置

`server.proxy[''/api'']` 指向后端端口。

## 好处

不用配 CORS，不用调 SameSite，Session Cookie 天然能带。',
     'PENDING', 0, 0, 0, 0, datetime('now','localtime'),
     datetime('now','localtime','-12 hour'), datetime('now','localtime'), 0);

-- 2 篇被驳回（作者能在"我的文章"里看到驳回原因）
INSERT INTO t_article
    (id, author_id, category_id, title, summary, content_md, status,
     reject_reason, view_count, like_count, collect_count, comment_count,
     submitted_at, audit_user_id, audited_at, created_at, updated_at, deleted)
VALUES
    (111, 3, 3, '论某某框架一定会取代某某（待定）',
     '标题下结论太快，正文没有支撑。',
     '## 观点

我觉得 X 框架明年就会淘汰 Y。

## （正文待补）',
     'REJECTED', '标题是结论式表述但正文没有论据，请补充实测数据或删掉结论。',
     0, 0, 0, 0, datetime('now','localtime','-2 day'),
     1, datetime('now','localtime','-2 day'),
     datetime('now','localtime','-2 day'), datetime('now','localtime','-2 day'), 0),

    (112, 2, 1, '抄一段网上教程（未注明出处）',
     '内容与他人文章高度相似。',
     '## 正文

（此处为整段搬运，未标注来源）',
     'REJECTED', '内容与已有文章高度重复且未注明出处，请改写为自己的实践记录并补充来源。',
     0, 0, 0, 0, datetime('now','localtime','-3 day'),
     1, datetime('now','localtime','-3 day'),
     datetime('now','localtime','-3 day'), datetime('now','localtime','-3 day'), 0);

-- ------------------------------------------------------------ 文章-标签

INSERT INTO t_article_tag (article_id, tag_id) VALUES
    (101, 1), (101, 6),
    (102, 2), (102, 4),
    (103, 3), (103, 5),
    (104, 5),
    (105, 3), (105, 5),
    (106, 6),
    (107, 2),
    (108, 1), (108, 6),
    (109, 2),
    (110, 1), (110, 6),
    (111, 2),
    (112, 1);

-- ------------------------------------------------------------ 点赞（8 条，与 like_count 对应）

INSERT INTO t_article_like (user_id, article_id, created_at) VALUES
    (2, 101, datetime('now','localtime','-10 day')),
    (3, 101, datetime('now','localtime','-9 day')),
    (1, 102, datetime('now','localtime','-7 day')),
    (2, 102, datetime('now','localtime','-7 day')),
    (3, 102, datetime('now','localtime','-6 day')),
    (2, 103, datetime('now','localtime','-5 day')),
    (3, 104, datetime('now','localtime','-3 day')),
    (1, 106, datetime('now','localtime','-1 day'));

-- ------------------------------------------------------------ 收藏（5 条，与 collect_count 对应）

INSERT INTO t_article_collect (user_id, article_id, created_at) VALUES
    (3, 101, datetime('now','localtime','-9 day')),
    (2, 102, datetime('now','localtime','-7 day')),
    (3, 102, datetime('now','localtime','-6 day')),
    (1, 103, datetime('now','localtime','-5 day')),
    (2, 106, datetime('now','localtime','-1 day'));

-- ------------------------------------------------------------ 评论（6 已通过含 1 回复，2 待审核，1 驳回）

INSERT INTO t_comment
    (id, article_id, parent_id, user_id, content, status, reject_reason, audit_user_id, audited_at, created_at, deleted)
VALUES
    (201, 101, NULL, 3, '解构丢失响应性这个坑我也踩过，文章里给的例子很直观。',
     'PUBLISHED', NULL, 1, datetime('now','localtime','-10 day'), datetime('now','localtime','-10 day'), 0),
    (202, 101, NULL, 1, '补充一句：需要保留响应性时可以用 toRefs。',
     'PUBLISHED', NULL, 1, datetime('now','localtime','-10 day'), datetime('now','localtime','-10 day'), 0),
    (203, 101, 202, 3, '谢谢，toRefs 这个我回去试试。',
     'PUBLISHED', NULL, 1, datetime('now','localtime','-9 day'), datetime('now','localtime','-9 day'), 0),
    (204, 102, NULL, 3, '那个 optional 依赖的坑确实隐蔽，报错还看不出原因。',
     'PUBLISHED', NULL, 1, datetime('now','localtime','-7 day'), datetime('now','localtime','-7 day'), 0),
    (205, 103, NULL, 2, '请问 busy_timeout 设成 5000 之后，写冲突会变成排队还是仍然报错？',
     'PUBLISHED', NULL, 1, datetime('now','localtime','-5 day'), datetime('now','localtime','-5 day'), 0),
    (206, 103, 205, 3, '等满 5 秒还是拿不到锁就会报错，只是把立刻失败变成了稍等再失败。',
     'PUBLISHED', NULL, 1, datetime('now','localtime','-5 day'), datetime('now','localtime','-5 day'), 0),
    (207, 101, NULL, 2, '想问一下 reactive 对数组的处理，这个还没太看明白。',
     'PENDING', NULL, NULL, NULL, datetime('now','localtime','-1 day'), 0),
    (208, 102, NULL, 3, '能不能再写一篇讲事务边界的？',
     'PENDING', NULL, NULL, NULL, datetime('now','localtime'), 0),
    (209, 104, NULL, 2, '（广告内容）加我微信领取资料。',
     'REJECTED', '与文章内容无关的推广信息。', 1, datetime('now','localtime','-2 day'),
     datetime('now','localtime','-2 day'), 0);

-- ------------------------------------------------------------ 操作日志（与上面的审核结果对应）

INSERT INTO t_audit_log
    (operator_id, operator_name, action, target_type, target_id, target_title, result, remark, created_at)
VALUES
    (1, 'admin', 'ARTICLE_APPROVE', 'ARTICLE', 101, 'Vue 3 的响应式到底在响应什么', 'SUCCESS', NULL,
     datetime('now','localtime','-11 day')),
    (1, 'admin', 'ARTICLE_APPROVE', 'ARTICLE', 102, '用 Spring Boot 4 + MyBatis-Plus 写博客后端',
     'SUCCESS', NULL, datetime('now','localtime','-8 day')),
    (1, 'admin', 'ARTICLE_REJECT', 'ARTICLE', 111, '论某某框架一定会取代某某（待定）',
     'SUCCESS', '标题是结论式表述但正文没有论据，请补充实测数据或删掉结论。',
     datetime('now','localtime','-2 day')),
    (1, 'admin', 'ARTICLE_REJECT', 'ARTICLE', 112, '抄一段网上教程（未注明出处）',
     'SUCCESS', '内容与已有文章高度重复且未注明出处，请改写为自己的实践记录并补充来源。',
     datetime('now','localtime','-3 day')),
    (1, 'admin', 'COMMENT_APPROVE', 'COMMENT', 201, '评论#201', 'SUCCESS', NULL,
     datetime('now','localtime','-10 day')),
    (1, 'admin', 'COMMENT_REJECT', 'COMMENT', 209, '评论#209', 'SUCCESS', '与文章内容无关的推广信息。',
     datetime('now','localtime','-2 day'));

-- ------------------------------------------------------------ 自检（可选）
-- 导入后可以直接用这几条 SQL 复核统计数字是否与接口一致：
--   SELECT status, COUNT(*) FROM t_article GROUP BY status;
--   SELECT COUNT(*) FROM t_article WHERE status='PENDING';
--   SELECT SUM(view_count) FROM t_article WHERE deleted=0;
--   SELECT a.id, a.like_count, COUNT(l.id) FROM t_article a
--     LEFT JOIN t_article_like l ON l.article_id=a.id GROUP BY a.id HAVING a.like_count <> COUNT(l.id);
