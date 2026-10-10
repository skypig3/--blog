package com.sky.blog.enums;

import java.util.EnumSet;
import java.util.Set;

/** 评论状态。回复与顶层评论共用这套状态，所以"审核覆盖回复"天然成立。 */
public enum CommentStatus {

    PENDING, PUBLISHED, REJECTED;

    /** 管理员可审核的状态：待审核，以及被驳回后需要重新放行的 */
    public static final Set<CommentStatus> AUDITABLE = EnumSet.of(PENDING, REJECTED);

    public boolean visibleToPublic() {
        return this == PUBLISHED;
    }
}
