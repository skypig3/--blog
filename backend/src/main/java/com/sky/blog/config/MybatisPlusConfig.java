package com.sky.blog.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 插件配置。
 *
 * <p>分页方言：MP 3.5.17 的 dialects 包里<b>没有</b> SqliteDialect 类，但
 * {@code DbType.SQLITE} 落在 {@code postgresqlSameType()} 名单里，
 * {@code DialectFactory} 会兜底到 PostgreDialect，生成 {@code LIMIT ? OFFSET ?}。
 * 实测该写法 SQLite 原生支持且分页结果正确（见 docs/spike/）。
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.SQLITE);
        // 前台 50 / 后台 100，防止翻页炸弹；超过上限直接按上限截断
        pagination.setMaxLimit(100L);
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}
