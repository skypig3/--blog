package com.sky.blog.vo;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.function.Function;

/**
 * 统一分页返回体。
 *
 * <p>把 MyBatis-Plus 的 {@code IPage} 转成固定结构，避免把框架对象直接暴露给前端。
 */
@Schema(description = "分页结果")
public record PageVo<T>(
        List<T> records,
        long total,
        long current,
        long size,
        long pages) {

    public static <E, T> PageVo<T> of(IPage<E> page, Function<E, T> mapper) {
        return new PageVo<>(page.getRecords().stream().map(mapper).toList(),
                page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    public static <T> PageVo<T> of(IPage<T> page) {
        return new PageVo<>(page.getRecords(), page.getTotal(), page.getCurrent(),
                page.getSize(), page.getPages());
    }

    public static <T> PageVo<T> empty(long current, long size) {
        return new PageVo<>(List.of(), 0, current, size, 0);
    }
}
