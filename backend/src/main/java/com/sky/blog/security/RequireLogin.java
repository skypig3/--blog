package com.sky.blog.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标在 Controller 方法或类上：必须登录才能访问。
 *
 * <p>没登录时由 {@link AuthInterceptor} 直接抛 401，请求根本进不到业务方法，
 * 对应作业 2.2.1"未登录用户执行受限操作时，提示登录，不得直接完成业务操作"。
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireLogin {
}
