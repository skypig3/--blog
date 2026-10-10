-- ============================================================
--  个人博客系统 · 建表脚本（SQLite）
--  由 spring.sql.init.mode=always 每次启动自动执行，必须幂等：
--  全部使用 CREATE TABLE / CREATE INDEX ... IF NOT EXISTS。
--  外键靠 JDBC URL 上的 foreign_keys=on 生效（SQLite 默认不开）。
-- ============================================================

-- 1. 用户
CREATE TABLE IF NOT EXISTS t_user (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    username            TEXT    NOT NULL UNIQUE,
    email               TEXT    NOT NULL UNIQUE,
    password_hash       TEXT    NOT NULL,
    nickname            TEXT    NOT NULL,
    avatar              TEXT,
    bio                 TEXT,
    role                TEXT    NOT NULL DEFAULT 'USER',      -- USER / ADMIN
    status              TEXT    NOT NULL DEFAULT 'ACTIVE',    -- ACTIVE / DISABLED
    credential_version  INTEGER NOT NULL DEFAULT 0,           -- 改密码 +1，用于踢掉旧会话
    deleted             INTEGER NOT NULL DEFAULT 0,
    created_at          TEXT    NOT NULL DEFAULT (datetime('now','localtime')),
    updated_at          TEXT    NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE INDEX IF NOT EXISTS idx_user_role_status ON t_user(role, status);

-- 2. 分类
CREATE TABLE IF NOT EXISTS t_category (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT    NOT NULL UNIQUE,
    sort_order  INTEGER NOT NULL DEFAULT 0,
    created_at  TEXT    NOT NULL DEFAULT (datetime('now','localtime'))
);

-- 3. 标签
CREATE TABLE IF NOT EXISTS t_tag (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT    NOT NULL UNIQUE,
    created_at  TEXT    NOT NULL DEFAULT (datetime('now','localtime'))
);

-- 4. 文章
CREATE TABLE IF NOT EXISTS t_article (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    author_id       INTEGER NOT NULL REFERENCES t_user(id),
    category_id     INTEGER REFERENCES t_category(id),
    title           TEXT    NOT NULL,
    summary         TEXT    NOT NULL DEFAULT '',
    content_md      TEXT    NOT NULL DEFAULT '',
    cover           TEXT,
    status          TEXT    NOT NULL DEFAULT 'DRAFT',   -- DRAFT/PENDING/PUBLISHED/REJECTED/REMOVED
    reject_reason   TEXT,
    offline_reason  TEXT,
    view_count      INTEGER NOT NULL DEFAULT 0,
    like_count      INTEGER NOT NULL DEFAULT 0,
    collect_count   INTEGER NOT NULL DEFAULT 0,
    comment_count   INTEGER NOT NULL DEFAULT 0,
    submitted_at    TEXT,
    audit_user_id   INTEGER,
    audited_at      TEXT,
    published_at    TEXT,
    deleted         INTEGER NOT NULL DEFAULT 0,
    created_at      TEXT    NOT NULL DEFAULT (datetime('now','localtime')),
    updated_at      TEXT    NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE INDEX IF NOT EXISTS idx_article_status_pub ON t_article(status, published_at);
CREATE INDEX IF NOT EXISTS idx_article_author     ON t_article(author_id, status);
CREATE INDEX IF NOT EXISTS idx_article_category   ON t_article(category_id, status);

-- 5. 文章-标签（多对多）
CREATE TABLE IF NOT EXISTS t_article_tag (
    article_id  INTEGER NOT NULL REFERENCES t_article(id),
    tag_id      INTEGER NOT NULL REFERENCES t_tag(id),
    created_at  TEXT    NOT NULL DEFAULT (datetime('now','localtime')),
    PRIMARY KEY (article_id, tag_id)
);
CREATE INDEX IF NOT EXISTS idx_article_tag_tag ON t_article_tag(tag_id);

-- 6. 评论 / 回复（同表，parent_id 区分）
CREATE TABLE IF NOT EXISTS t_comment (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    article_id      INTEGER NOT NULL REFERENCES t_article(id),
    parent_id       INTEGER REFERENCES t_comment(id),
    user_id         INTEGER NOT NULL REFERENCES t_user(id),
    content         TEXT    NOT NULL,
    status          TEXT    NOT NULL DEFAULT 'PENDING',  -- PENDING/PUBLISHED/REJECTED
    reject_reason   TEXT,
    audit_user_id   INTEGER,
    audited_at      TEXT,
    deleted         INTEGER NOT NULL DEFAULT 0,
    created_at      TEXT    NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE INDEX IF NOT EXISTS idx_comment_article_st ON t_comment(article_id, status, parent_id);
CREATE INDEX IF NOT EXISTS idx_comment_user       ON t_comment(user_id, status);

-- 7. 点赞（UNIQUE 保证幂等，重复点不会刷计数）
CREATE TABLE IF NOT EXISTS t_article_like (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id     INTEGER NOT NULL REFERENCES t_user(id),
    article_id  INTEGER NOT NULL REFERENCES t_article(id),
    created_at  TEXT    NOT NULL DEFAULT (datetime('now','localtime')),
    UNIQUE (user_id, article_id)
);
CREATE INDEX IF NOT EXISTS idx_like_article ON t_article_like(article_id);

-- 8. 收藏
CREATE TABLE IF NOT EXISTS t_article_collect (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id     INTEGER NOT NULL REFERENCES t_user(id),
    article_id  INTEGER NOT NULL REFERENCES t_article(id),
    created_at  TEXT    NOT NULL DEFAULT (datetime('now','localtime')),
    UNIQUE (user_id, article_id)
);
CREATE INDEX IF NOT EXISTS idx_collect_user ON t_article_collect(user_id);

-- 9. 审核 / 管理操作日志（只追加，不修改）
CREATE TABLE IF NOT EXISTS t_audit_log (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    operator_id     INTEGER NOT NULL,
    operator_name   TEXT    NOT NULL,               -- 快照：改名/删号后日志仍可读
    action          TEXT    NOT NULL,               -- ARTICLE_APPROVE / ARTICLE_REJECT / ...
    target_type     TEXT    NOT NULL,               -- ARTICLE / COMMENT / USER / CATEGORY / TAG
    target_id       INTEGER NOT NULL,
    target_title    TEXT,                           -- 快照
    result          TEXT    NOT NULL DEFAULT 'SUCCESS',
    remark          TEXT,
    created_at      TEXT    NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE INDEX IF NOT EXISTS idx_audit_target  ON t_audit_log(target_type, target_id);
CREATE INDEX IF NOT EXISTS idx_audit_created ON t_audit_log(created_at);
