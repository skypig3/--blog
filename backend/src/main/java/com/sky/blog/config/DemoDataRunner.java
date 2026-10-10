package com.sky.blog.config;

import java.nio.charset.StandardCharsets;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

/**
 * 演示数据导入器。
 *
 * <p>只有显式带上 {@code --blog.load-demo-data=true} 才会启用，
 * 因为 {@code db/demo-data.sql} 的开头是一组 DELETE —— 它会<b>清空全部业务数据</b>再灌入演示内容。
 * 正常启动绝不会碰到它。
 *
 * <p>用法（对应 README 的"导入演示数据"一节）：
 * <pre>
 *   java -jar target/blog-backend-1.0.0.jar --blog.load-demo-data=true
 *   或 mvnw spring-boot:run -Dspring-boot.run.arguments=--blog.load-demo-data=true
 * </pre>
 * 导入完会打印一组自检数字，然后由 BlogApplication 退出。
 */
@Component
@ConditionalOnProperty(prefix = "blog", name = "load-demo-data", havingValue = "true")
public class DemoDataRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataRunner.class);

    private final DataSource dataSource;

    public DemoDataRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.warn("即将执行 demo-data.sql —— 该脚本会先删除全部业务数据，再导入演示内容。");
        ClassPathResource resource = new ClassPathResource("db/demo-data.sql");
        try (java.sql.Connection c = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(c, new EncodedResource(resource, StandardCharsets.UTF_8));
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("执行 demo-data.sql 失败", e);
        }
        verify();
        log.info("演示数据导入完成。正常启动服务后即可用 admin / Admin@123 登录。");
    }

    /** 导入后自检：冗余计数必须与关系表行数一致，否则后台统计就是假的。 */
    private void verify() {
        String[] checks = {
                "SELECT 'users=' || COUNT(*) FROM t_user",
                "SELECT 'articles=' || COUNT(*) FROM t_article",
                "SELECT 'by_status=' || group_concat(s || ':' || c, ' ') "
                        + "FROM (SELECT status s, COUNT(*) c FROM t_article GROUP BY status)",
                "SELECT 'comments=' || COUNT(*) FROM t_comment",
                "SELECT 'likes=' || COUNT(*) FROM t_article_like",
                "SELECT 'collects=' || COUNT(*) FROM t_article_collect",
                "SELECT 'audit_logs=' || COUNT(*) FROM t_audit_log",
                "SELECT 'view_sum=' || SUM(view_count) FROM t_article WHERE deleted = 0",
                "SELECT 'like_count_mismatch=' || COUNT(*) FROM t_article a "
                        + "WHERE a.like_count <> (SELECT COUNT(*) FROM t_article_like l WHERE l.article_id = a.id)",
                "SELECT 'collect_count_mismatch=' || COUNT(*) FROM t_article a "
                        + "WHERE a.collect_count <> (SELECT COUNT(*) FROM t_article_collect l WHERE l.article_id = a.id)",
                "SELECT 'comment_count_mismatch=' || COUNT(*) FROM t_article a "
                        + "WHERE a.comment_count <> (SELECT COUNT(*) FROM t_comment cm "
                        + "WHERE cm.article_id = a.id AND cm.status = 'PUBLISHED' AND cm.deleted = 0)"
        };
        log.info("演示数据自检 --------------------------------");
        try (java.sql.Connection c = dataSource.getConnection();
             java.sql.Statement s = c.createStatement()) {
            for (String sql : checks) {
                try (java.sql.ResultSet r = s.executeQuery(sql)) {
                    if (r.next()) {
                        log.info("  {}", r.getString(1));
                    }
                }
            }
        } catch (java.sql.SQLException e) {
            log.error("自检查询失败", e);
        }
    }
}
