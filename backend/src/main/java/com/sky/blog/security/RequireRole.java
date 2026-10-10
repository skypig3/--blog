package com.sky.blog.security;

import com.sky.blog.enums.Role;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 标在 Controller 方法或类上：要求当前用户具备指定角色（后台接口统一用 ADMIN）。 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    Role value() default Role.ADMIN;
}
