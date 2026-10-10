package com.sky.blog.enums;

/** 审核/管理操作类型，写进 t_audit_log.action。 */
public enum AuditAction {
    ARTICLE_APPROVE,
    ARTICLE_REJECT,
    ARTICLE_OFFLINE,
    ARTICLE_DELETE,
    COMMENT_APPROVE,
    COMMENT_REJECT,
    COMMENT_DELETE,
    USER_DISABLE,
    USER_ENABLE,
    USER_ROLE_CHANGE,
    CATEGORY_CREATE,
    CATEGORY_UPDATE,
    CATEGORY_DELETE,
    TAG_CREATE,
    TAG_UPDATE,
    TAG_DELETE
}
