-- ============================================================
--  初始化数据（幂等，可重复执行；随每次启动由 spring.sql.init 自动跑）
--
--  ⚠ 演示账号仅用于本地测试，密码哈希是真实 BCrypt 生成后写死的，
--    不要把这套凭据用到任何公网环境。
--    admin    / Admin@123   管理员
--    xiaoming / User@123    普通用户
--    xiaohong / User@123    普通用户
--
--  写法说明：显式指定 id + INSERT OR IGNORE，
--  所以重复启动不会产生第二个 admin，也不会覆盖已被改过的同名记录。
-- ============================================================

INSERT OR IGNORE INTO t_user
    (id, username, email, password_hash, nickname, bio, role, status, credential_version, deleted)
VALUES
    (1, 'admin', 'admin@example.com',
     '$2a$10$xXUFHOksofUz73PFgXEz9uX929eeAm1jD.5ovHm95X//YO4Sd9Any',
     '站长', '本站管理员（演示账号）', 'ADMIN', 'ACTIVE', 0, 0),
    (2, 'xiaoming', 'xiaoming@example.com',
     '$2a$10$d0OR18sOf66Lg6f/L5ydGeDdLH59R7EqlnUeoJGhpRgK9WzCHClKy',
     '小明', '前端爱好者，正在学 Vue 3', 'USER', 'ACTIVE', 0, 0),
    (3, 'xiaohong', 'xiaohong@example.com',
     '$2a$10$d0OR18sOf66Lg6f/L5ydGeDdLH59R7EqlnUeoJGhpRgK9WzCHClKy',
     '小红', '后端方向，喜欢折腾数据库', 'USER', 'ACTIVE', 0, 0);

INSERT OR IGNORE INTO t_category (id, name, sort_order) VALUES
    (1, '技术笔记', 10),
    (2, '项目实战', 20),
    (3, '读书笔记', 30),
    (4, '生活随笔', 40);

INSERT OR IGNORE INTO t_tag (id, name) VALUES
    (1, 'Vue'),
    (2, 'Spring Boot'),
    (3, 'SQLite'),
    (4, 'MyBatis-Plus'),
    (5, '数据库设计'),
    (6, '前端');
