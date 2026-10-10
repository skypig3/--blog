package demo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootApplication
@org.mybatis.spring.annotation.MapperScan("demo")
public class App implements CommandLineRunner {

    static StringBuilder log = new StringBuilder();

    static void p(String k, Object v) {
        log.append(k).append(" = ").append(v).append('\n');
    }

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Value("${spring.datasource.url}")
    String jdbcUrl;

    @Override
    public void run(String... args) throws Exception {
        p("spring_boot_version", SpringBootVersion.getVersion());
        p("mybatis_plus_version", com.baomidou.mybatisplus.core.metadata.IPage.class
                .getPackage().getImplementationVersion());
        p("java_version", System.getProperty("java.version"));
        p("cwd", System.getProperty("user.dir"));
        p("cwd_has_non_ascii", System.getProperty("user.dir").chars().anyMatch(c -> c > 127));
        p("datasource_url", jdbcUrl);

        List<Long> ids = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            Article a = new Article();
            a.setTitle("article-" + i);
            a.setStatus(i % 2 == 0 ? "PUBLISHED" : "PENDING");
            a.setViewCount(i);
            mapper.insert(a);
            ids.add(a.getId());
        }
        p("insert_25_ok", true);
        p("id_backfilled_first_last", ids.get(0) + " / " + ids.get(ids.size() - 1));
        p("autoincrement_not_zero", ids.get(0) != null && ids.get(0) > 0);

        // default value from schema.sql DDL
        Article one = mapper.selectById(ids.get(0));
        p("ddl_default_created_at", one.getCreatedAt());

        // --- pagination (the risky part: DbType.SQLITE -> which IDialect?) ---
        LambdaQueryWrapper<Article> q = new LambdaQueryWrapper<>();
        q.like(Article::getTitle, "article-").orderByDesc(Article::getId);
        Page<Article> pg = mapper.selectPage(new Page<>(3, 10), q);
        p("page_no_size", pg.getCurrent() + " x " + pg.getSize());
        p("page_records_on_page3", pg.getRecords().size());
        p("page_total", pg.getTotal());
        p("page_pages", pg.getPages());
        p("page_first_row_id_page3",
                pg.getRecords().isEmpty() ? "EMPTY" : pg.getRecords().get(0).getId());

        // count-only query with the same wrapper
        p("count_published", mapper.selectCount(
                new LambdaQueryWrapper<Article>().eq(Article::getStatus, "PUBLISHED")));

        // --- CAS style audit transition (double-click protection) ---
        // ids.get(2) == article-3, which was inserted as PENDING (odd index)
        LambdaUpdateWrapper<Article> cas = new LambdaUpdateWrapper<>();
        cas.eq(Article::getId, ids.get(2)).eq(Article::getStatus, "PENDING")
           .set(Article::getStatus, "PUBLISHED");
        int first = mapper.update(null, cas);
        int second = mapper.update(null, cas);
        p("cas_target_status_after_both", mapper.selectById(ids.get(2)).getStatus());
        p("cas_first_update_rows_expect_1", first);
        p("cas_repeat_update_rows_must_be_0", second);

        // --- group by / aggregate for admin stats ---
        try (Connection c = DriverManager.getConnection(jdbcUrl);
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT status, COUNT(*) n FROM t_article GROUP BY status")) {
            StringBuilder sb = new StringBuilder();
            while (r.next()) sb.append(r.getString("status")).append(':').append(r.getInt("n")).append(' ');
            p("group_by_status", sb.toString().trim());
        }

        // --- sqlite-jdbc URL query params (pragmas via url) ---
        String urlWithParams = jdbcUrl + "&synchronous=NORMAL&cache_size=-8000";
        try (Connection c = DriverManager.getConnection(urlWithParams);
             Statement s = c.createStatement()) {
            try (ResultSet r = s.executeQuery("PRAGMA foreign_keys")) {
                r.next();
                p("pragma_foreign_keys_from_url", r.getInt(1));
            }
            try (ResultSet r = s.executeQuery("PRAGMA busy_timeout")) {
                r.next();
                p("pragma_busy_timeout_from_url", r.getInt(1));
            }
            try (ResultSet r = s.executeQuery("PRAGMA synchronous")) {
                r.next();
                p("pragma_synchronous_from_url", r.getInt(1));
            }
            try (ResultSet r = s.executeQuery("PRAGMA cache_size")) {
                r.next();
                p("pragma_cache_size_from_url", r.getInt(1));
            }
            try (ResultSet r = s.executeQuery("PRAGMA journal_mode")) {
                r.next();
                p("pragma_journal_mode_from_url", r.getString(1));
            }
        } catch (Exception e) {
            p("pragma_url_error", e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        // --- pragmas on the pool's own connection (hikari connection-init-sql) ---
        try (Connection c = dsConn();
             Statement s = c.createStatement()) {
            try (ResultSet r = s.executeQuery("PRAGMA foreign_keys")) {
                r.next();
                p("pool_pragma_foreign_keys", r.getInt(1));
            }
            try (ResultSet r = s.executeQuery("PRAGMA journal_mode")) {
                r.next();
                p("pool_pragma_journal_mode", r.getString(1));
            }
            try (ResultSet r = s.executeQuery("PRAGMA busy_timeout")) {
                r.next();
                p("pool_pragma_busy_timeout", r.getInt(1));
            }
        }

        // --- Spring transaction rollback on SQLite ---
        long before = mapper.selectCount(null);
        try {
            txTemplate.executeWithoutResult(st -> {
                Article a = new Article();
                a.setTitle("rollback-me");
                a.setStatus("PENDING");
                a.setViewCount(0);
                mapper.insert(a);
                throw new RuntimeException("force rollback");
            });
        } catch (RuntimeException expected) {
            p("tx_rollback_exception_seen", expected.getMessage());
        }
        p("count_before_after_rollback", before + " / " + mapper.selectCount(null));

        // --- BCrypt via spring-security-crypto only (no Spring Security filter chain) ---
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder enc =
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        String hash = enc.encode("Admin@123");
        p("bcrypt_hash_prefix", hash.substring(0, 7));
        p("bcrypt_hash_length", hash.length());
        p("bcrypt_matches", enc.matches("Admin@123", hash));
        p("bcrypt_rejects_wrong", !enc.matches("admin123", hash));
        p("bcrypt_salt_differs_each_encode", !hash.equals(enc.encode("Admin@123")));
        String zhPwd = "中文密码！";
        p("bcrypt_unicode_password_matches", enc.matches(zhPwd, enc.encode(zhPwd)));

        p("db_file", new File(new File("data"), "blog.db").getAbsolutePath());
        p("db_file_exists", new File(new File("data"), "blog.db").exists());

        // --- Chinese (non-ASCII) round trip through MP + SQLite ---
        String zhTitle = "\u4e2d\u6587\u6807\u9898-\u535a\u5ba2\u7cfb\u7edf";   // 中文标题-博客系统
        String zhPart  = "\u535a\u5ba2";                                        // 博客
        Article zh = new Article();
        zh.setTitle(zhTitle);
        zh.setStatus("PUBLISHED");
        zh.setViewCount(7);
        mapper.insert(zh);
        Article zhBack = mapper.selectById(zh.getId());
        p("zh_insert_id", zh.getId());
        p("zh_roundtrip_equals", zhTitle.equals(zhBack.getTitle()));
        p("zh_roundtrip_bytes_ok", zhBack.getTitle().length() == zhTitle.length());
        p("zh_like_count", mapper.selectCount(
                new LambdaQueryWrapper<Article>().like(Article::getTitle, zhPart)));
        p("ascii_like_is_case_insensitive", mapper.selectCount(
                new LambdaQueryWrapper<Article>().like(Article::getTitle, "ARTICLE-1")));
        try (Connection c = DriverManager.getConnection(jdbcUrl);
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery(
                     "SELECT length(title) chars, length(CAST(title AS BLOB)) bytes, "
                   + "substr(title,1,2) head2, instr(title,'\u535a\u5ba2') pos FROM t_article WHERE id="
                   + zh.getId())) {
            if (r.next()) {
                p("sqlite_length_is_chars", r.getInt("chars"));
                p("sqlite_length_is_bytes", r.getInt("bytes"));
                p("sqlite_substr_head2_len", r.getString("head2").length());
                p("sqlite_instr_pos_of_zh_substring", r.getInt("pos"));
            }
        }
        // ordering of Chinese text (no ICU collation in SQLite)
        try (Connection c = DriverManager.getConnection(jdbcUrl);
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery(
                     "SELECT group_concat(id) ids FROM (SELECT id FROM t_article "
                   + "WHERE title LIKE '\u4e2d%' ORDER BY title COLLATE BINARY)")) {
            r.next();
            p("zh_order_by_binary_ok", r.getString(1));
        }

        try (PrintStream w = new PrintStream(
                new FileOutputStream("stacktest-spike.txt"), true, StandardCharsets.UTF_8)) {
            w.println(log);
        }
        System.out.println("SPIKE_DONE");
    }

    @org.springframework.beans.factory.annotation.Autowired
    ArticleMapper mapper;

    @org.springframework.beans.factory.annotation.Autowired
    javax.sql.DataSource dataSource;

    @org.springframework.beans.factory.annotation.Autowired
    TransactionTemplate txTemplate;

    private Connection dsConn() throws Exception {
        return dataSource.getConnection();
    }
}
