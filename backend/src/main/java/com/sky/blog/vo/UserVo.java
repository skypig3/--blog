package com.sky.blog.vo;

import com.sky.blog.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用户出参。
 *
 * <p>作业 2.3.2 明确"用户管理页面及接口不得返回密码或密码哈希"——
 * 这里<b>结构上就不存在</b> passwordHash 字段，靠"忘了排除"来防泄漏的可能为零。
 */
public final class UserVo {

    private UserVo() {
    }

    @Schema(description = "当前登录用户 / 用户管理列表项")
    public record Detail(
            Long id,
            String username,
            String email,
            String nickname,
            String avatar,
            String bio,
            @Schema(description = "USER / ADMIN") String role,
            @Schema(description = "ACTIVE / DISABLED") String status,
            String createdAt) {

        public static Detail of(User u) {
            if (u == null) {
                return null;
            }
            return new Detail(u.getId(), u.getUsername(), u.getEmail(), u.getNickname(),
                    u.getAvatar(), u.getBio(),
                    u.getRole() == null ? null : u.getRole().name(),
                    u.getStatus() == null ? null : u.getStatus().name(),
                    u.getCreatedAt());
        }
    }

    @Schema(description = "文章/评论里挂的作者信息，不含邮箱等敏感字段")
    public record Author(Long id, String nickname, String avatar) {

        public static Author of(User u) {
            return u == null ? new Author(null, "已注销用户", null)
                    : new Author(u.getId(), u.getNickname(), u.getAvatar());
        }
    }
}
