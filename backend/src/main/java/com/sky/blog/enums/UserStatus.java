package com.sky.blog.enums;

/** 账号状态。DISABLED 的账号既不能登录，已有会话也会被拦截器立即挡掉。 */
public enum UserStatus {
    ACTIVE, DISABLED
}
