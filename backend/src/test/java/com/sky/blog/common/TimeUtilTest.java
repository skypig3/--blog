package com.sky.blog.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 时间格式的单元测试。
 *
 * <p>这条测试专门守住设计文档 §4.4 第 6 条那个实测出来的坑：
 * Java 侧写入的字符串格式必须和 SQLite 的 {@code datetime('now','localtime')} 默认值<b>逐字符一致</b>，
 * 否则同一列里混进两种格式，按时间排序就会错乱。
 */
class TimeUtilTest {

    @Test
    @DisplayName("TimeUtil.now() 与 SQLite datetime 默认值格式一致")
    void matchesSqliteDefaultFormat() throws Exception {
        String javaNow = TimeUtil.now();
        String sqliteNow;
        try (Connection c = DriverManager.getConnection("jdbc:sqlite::memory:");
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT datetime('now','localtime')")) {
            r.next();
            sqliteNow = r.getString(1);
        }
        assertThat(javaNow).hasSize(19).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
        // 长度和分隔符位置必须一致（秒级可能差 1，这里比格式不比时刻）
        assertThat(javaNow.charAt(10)).isEqualTo(' ').isEqualTo(sqliteNow.charAt(10));
        assertThat(javaNow.substring(0, 7)).isEqualTo(sqliteNow.substring(0, 7));
    }

    @Test
    @DisplayName("字典序等于时间序，所以列表可以直接按字符串排序")
    void lexicographicEqualsChronological() {
        assertThat(TimeUtil.now().compareTo("2000-01-01 00:00:00")).isGreaterThan(0);
        assertThat("2026-10-09 09:00:00".compareTo("2026-10-09 10:00:00")).isLessThan(0);
        assertThat("2026-10-09 10:00:00".compareTo("2026-10-10 09:00:00")).isLessThan(0);
    }

    @Test
    @DisplayName("读取时容错：空格和 T 两种分隔符都能解析")
    void parseTolerant() {
        assertThat(TimeUtil.parse("2026-10-09 16:54:20"))
                .isEqualTo(TimeUtil.parse("2026-10-09T16:54:20"));
        assertThat(TimeUtil.parse("2026-10-09T16:54:20.123"))
                .isEqualTo(TimeUtil.parse("2026-10-09 16:54:20"));
        assertThat(TimeUtil.parse(null)).isNull();
        assertThat(TimeUtil.parse("  ")).isNull();
    }

    @Test
    @DisplayName("daysAgo 落在 now 之前，热门窗口靠它算边界")
    void daysAgoIsBeforeNow() {
        assertThat(TimeUtil.daysAgo(30)).isLessThan(TimeUtil.now());
        assertThat(TimeUtil.daysAgo(0).substring(0, 10)).isEqualTo(TimeUtil.now().substring(0, 10));
    }
}
