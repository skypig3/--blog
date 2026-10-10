package com.sky.blog.enums;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 文章状态机 —— 全系统唯一允许的状态流转表，Service 层必须走 {@link #assertCan} 再改状态。
 *
 * <pre>
 *   DRAFT ──提交──▶ PENDING ──通过──▶ PUBLISHED ──下架──▶ REMOVED
 *     ▲                │  │                                  │
 *     │             驳回 │  │ 撤回（作者）                      │ 作者改后再提交
 *     └────────────────  └──────────▶ DRAFT                 ▼
 *   REJECTED ──改后提交──▶ PENDING ◀───────────────────────────┘
 *   PUBLISHED ──作者再编辑──▶ 保持 PUBLISHED 直到再次提交审核（见 EDIT 说明）
 * </pre>
 *
 * <p>每次流转都用 {@code UPDATE ... WHERE status = 期望的原状态} 落地（CAS），
 * 影响行数为 0 就说明状态已被别人改过 —— 这正好满足作业要求的
 * "审核只对当前待审核内容生效，不得通过重复点击将已撤回或已删除的文章错误发布"。
 */
public enum ArticleStatus {

    /** 草稿，仅作者可见 */
    DRAFT,
    /** 待审核，仅作者和管理员可见 */
    PENDING,
    /** 已发布，前台可见 */
    PUBLISHED,
    /** 被驳回，带原因，仅作者可见 */
    REJECTED,
    /** 已下架（违规），带原因，前台不可见 */
    REMOVED;

    /** 触发流转的动作 */
    public enum Transition {
        /** 作者提交审核 */
        SUBMIT,
        /** 作者撤回（仅待审核状态可撤） */
        WITHDRAW,
        /** 管理员审核通过 */
        APPROVE,
        /** 管理员驳回，必须填原因 */
        REJECT,
        /** 管理员下架违规文章，必须填原因 */
        OFFLINE,
        /** 作者编辑内容 */
        EDIT
    }

    private static final Map<Transition, Set<ArticleStatus>> ALLOWED_FROM = Map.of(
            // 草稿、被驳回、已下架都可以提交；已发布的再编辑由 Service 先退回 DRAFT 语义处理
            Transition.SUBMIT,   EnumSet.of(DRAFT, REJECTED, REMOVED, PUBLISHED),
            Transition.WITHDRAW, EnumSet.of(PENDING),
            Transition.APPROVE,  EnumSet.of(PENDING),
            Transition.REJECT,   EnumSet.of(PENDING),
            Transition.OFFLINE,  EnumSet.of(PUBLISHED),
            // 只有未公开的内容允许直接改；已发布的走"编辑后重新提交"
            Transition.EDIT,     EnumSet.of(DRAFT, REJECTED, REMOVED, PUBLISHED)
    );

    public boolean can(Transition transition) {
        return ALLOWED_FROM.getOrDefault(transition, EnumSet.noneOf(ArticleStatus.class)).contains(this);
    }

    /** 不允许就抛业务异常，避免各 Service 里散落 if。 */
    public void assertCan(Transition transition) {
        if (!can(transition)) {
            throw new IllegalStateException(
                    "文章当前状态为 " + this + "，不允许执行 " + transition);
        }
    }

    /** 前台是否可见 */
    public boolean visibleToPublic() {
        return this == PUBLISHED;
    }
}
