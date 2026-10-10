package com.sky.blog.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sky.blog.enums.ArticleStatus.Transition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 文章状态机的纯单元测试：不启动 Spring，规则本身就能测完。
 *
 * <p>对应作业 2.3.3 的"审核只对当前待审核内容生效"。
 */
class ArticleStatusTest {

    @Test
    @DisplayName("只有待审核的文章能被通过或驳回")
    void onlyPendingIsAuditable() {
        for (ArticleStatus s : ArticleStatus.values()) {
            boolean auditable = s == ArticleStatus.PENDING;
            assertThat(s.can(Transition.APPROVE)).as("APPROVE from " + s).isEqualTo(auditable);
            assertThat(s.can(Transition.REJECT)).as("REJECT from " + s).isEqualTo(auditable);
        }
    }

    @Test
    @DisplayName("只有已发布的文章才需要下架")
    void onlyPublishedCanBeOfflined() {
        for (ArticleStatus s : ArticleStatus.values()) {
            assertThat(s.can(Transition.OFFLINE)).isEqualTo(s == ArticleStatus.PUBLISHED);
        }
    }

    @Test
    @DisplayName("撤回只对待审核有效，所以撤回后再点通过必然被状态机挡住")
    void withdrawThenApproveIsBlocked() {
        ArticleStatus pending = ArticleStatus.PENDING;
        pending.assertCan(Transition.WITHDRAW);

        ArticleStatus withdrawn = ArticleStatus.DRAFT;      // 撤回之后的状态
        assertThat(withdrawn.can(Transition.APPROVE)).isFalse();
        assertThatThrownBy(() -> withdrawn.assertCan(Transition.APPROVE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("不允许执行 APPROVE");
    }

    @Test
    @DisplayName("草稿/驳回/下架都能重新提交；只有已发布不能重复提交")
    void submitSources() {
        assertThat(ArticleStatus.DRAFT.can(Transition.SUBMIT)).isTrue();
        assertThat(ArticleStatus.REJECTED.can(Transition.SUBMIT)).isTrue();
        assertThat(ArticleStatus.REMOVED.can(Transition.SUBMIT)).isTrue();
        assertThat(ArticleStatus.PENDING.can(Transition.SUBMIT)).isFalse();
    }

    @Test
    @DisplayName("前台可见性：只有 PUBLISHED 对游客开放")
    void publicVisibility() {
        for (ArticleStatus s : ArticleStatus.values()) {
            assertThat(s.visibleToPublic()).as(String.valueOf(s)).isEqualTo(s == ArticleStatus.PUBLISHED);
        }
    }
}
