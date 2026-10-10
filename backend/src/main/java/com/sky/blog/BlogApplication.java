package com.sky.blog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 个人博客系统后端入口。
 *
 * <p>三件必须存在但容易被忽略的事：
 * <ol>
 *   <li>{@code @MapperScan}：Boot 4 + mybatis-plus-spring-boot4-starter 下 Mapper 不会被自动扫描，
 *       不加会报 {@code No qualifying bean of type ...Mapper}（实测）。</li>
 *   <li>数据目录必须先存在：SQLite 不会自动建父目录，缺目录只报
 *       {@code [SQLITE_CANTOPEN]}，Spring 侧还显示 "Failed to execute database script"（实测）。</li>
 *   <li>{@link #resolveDataDir()}：把数据库固定落在 {@code backend/data}，
 *       <b>不随启动目录变化</b>。原因见下面注释里的真实事故。</li>
 * </ol>
 */
@SpringBootApplication
@MapperScan("com.sky.blog.mapper")
public class BlogApplication {

    private static final Logger log = LoggerFactory.getLogger(BlogApplication.class);

    public static void main(String[] args) throws IOException {
        Path dataDir = resolveDataDir();
        Files.createDirectories(dataDir);
        Files.createDirectories(dataDir.resolve("uploads"));
        // 系统属性优先级高于 application.yml，所以 yml 里的 ${blog.data-dir} 会取到这个绝对路径
        System.setProperty("blog.data-dir", dataDir.toAbsolutePath().normalize().toString().replace('\\', '/'));

        SpringApplication app = new SpringApplication(BlogApplication.class);
        if (isDemoLoad(args)) {
            Properties only = new Properties();
            only.setProperty("spring.main.web-application-type", "none");
            app.setDefaultProperties(only);
        }
        app.run(args);
    }

    /**
     * 无论从哪里启动，数据库都指向 {@code backend/data/blog.db}。
     *
     * <p>真实事故（2026-10-09）：命令行 {@code java -jar} 的工作目录是 {@code backend/}，
     * 而 IntelliJ 的运行配置默认工作目录是<b>项目根</b>。
     * 用相对路径 {@code ./data/blog.db} 时，两边各自打开了一个不同的库文件：
     * 命令行那份灌了演示数据，IDEA 那份只有初始账号，
     * 表现就是"我明明导入过数据，页面却是空的"，而且两个进程还互相看不见对方的写入。
     *
     * <p>判定方式：看当前目录里有没有 {@code pom.xml}（那就是 backend），
     * 否则看 {@code backend/pom.xml} 在不在（那就是项目根）。
     */
    static Path resolveDataDir() {
        Path cwd = Path.of("").toAbsolutePath().normalize();
        if (Files.isRegularFile(cwd.resolve("pom.xml"))) {
            return cwd.resolve("data");
        }
        Path backend = cwd.resolve("backend");
        if (Files.isRegularFile(backend.resolve("pom.xml"))) {
            return backend.resolve("data");
        }
        return cwd.resolve("data");
    }

    /** 判断是不是"导入演示数据"模式 */
    private static boolean isDemoLoad(String[] args) {
        return Arrays.asList(args).contains("--blog.load-demo-data=true");
    }
}
