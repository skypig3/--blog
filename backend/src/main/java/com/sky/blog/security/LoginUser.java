package com.sky.blog.security;

import java.io.Serializable;

/**
 * 会话里存的东西。
 *
 * <p>只存 {@code userId} 和登录那一刻的 {@code credentialVersion}，
 * <b>不存 role</b>：角色必须在每次请求时回库取最新的，
 * 否则管理员被降级、账号被禁用之后，旧会话还能继续干坏事（作业 2.2.1 明确禁止）。
 */
public record LoginUser(Long userId, Integer credentialVersion) implements Serializable {

    /** 会话中存放当前登录信息用的 key */
    public static final String SESSION_KEY = "blog.loginUser";

    public static LoginUser of(Long userId, Integer credentialVersion) {
        return new LoginUser(userId, credentialVersion == null ? 0 : credentialVersion);
    }

    public boolean credentialMatches(Integer currentVersion) {
        return credentialVersion != null && credentialVersion.equals(currentVersion);
    }

    /** 给日志/异常信息用，避免出现任何凭据字段 */
    public String describe() {
        return "user#" + userId;
    }
}
