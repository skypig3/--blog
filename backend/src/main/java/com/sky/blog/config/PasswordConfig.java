package com.sky.blog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码编码器。
 *
 * <p>这里<b>只</b>引入 spring-security-crypto 这一个 jar 里的 BCrypt，
 * 不引入 Spring Security 的过滤器链（设计文档 ADR-001）。
 * 实测：BCrypt 输出 {@code $2a$10$} 开头、定长 60 字符，自带随机盐，
 * 同一密码两次编码结果不同，中文密码同样能正确匹配。
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
