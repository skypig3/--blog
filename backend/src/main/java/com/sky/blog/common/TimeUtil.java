package com.sky.blog.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 时间统一按 {@code yyyy-MM-dd HH:mm:ss} 字符串存取。
 *
 * <p>为什么不用 LocalDateTime 直接映射（实测结论）：
 * <ul>
 *   <li>建表脚本里的默认值是 {@code datetime('now','localtime')}，产出 {@code 2026-10-09 16:54:20}（中间是空格）；</li>
 *   <li>而 sqlite-jdbc 的 {@code setObject(LocalDateTime)} 会写成 {@code 2026-10-09T12:34:56}（中间是 T）。</li>
 * </ul>
 * 同一列混进两种格式，字符串比较和排序就会错乱（空格 0x20 排在 T 0x54 前面）。
 * 所以写入一律走本类的 {@link #now()}，格式和数据库默认值完全一致；
 * 读取时 {@link #parse(String)} 两种分隔符都容错。
 */
public final class TimeUtil {

    public static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private TimeUtil() {
    }

    /** 当前时间，格式与 SQLite 的 datetime('now','localtime') 一致 */
    public static String now() {
        return LocalDateTime.now().format(FMT);
    }

    /** 相对当前时间偏移若干天（热门窗口、统计用） */
    public static String daysAgo(int days) {
        return LocalDateTime.now().minusDays(days).format(FMT);
    }

    /** 容错解析：空格或 T 分隔都能读；空串返回 null */
    public static LocalDateTime parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim().replace(' ', 'T');
        int dot = v.indexOf('.');
        if (dot > 0) {
            v = v.substring(0, dot);
        }
        return LocalDateTime.parse(v);
    }

    /** 展示用：把存储值格式化成前端友好形式（这里等价于原样返回） */
    public static String display(String value) {
        return value == null ? null : value.trim();
    }
}
