package com.sky.blog.config;

import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 业务自定义配置，对应 application.yml 里的 blog.* */
@Data
@Component
@ConfigurationProperties(prefix = "blog")
public class BlogProperties {

    private static final Logger log = LoggerFactory.getLogger(BlogProperties.class);

    /** 启动时把真实落地的路径打出来，避免"改了数据页面却没变"时找不到原因 */
    @PostConstruct
    void logResolvedPaths() {
        log.info("数据目录 = {}", dataDir);
        log.info("上传目录 = {}  (访问前缀 /uploads/**)", uploadDir);
    }

    /** 数据目录（SQLite 文件所在处） */
    private String dataDir = "./data";

    /** 图片上传目录 */
    private String uploadDir = "./data/uploads";

    /** 单文件大小上限（MB） */
    private int uploadMaxSizeMb = 2;

    /** 允许的图片扩展名（小写，不含点） */
    private List<String> uploadAllowedExt = List.of("jpg", "jpeg", "png", "gif", "webp");

    /** 热门文章统计窗口（天） */
    private int hotWindowDays = 30;

    /** 热门文章条数 */
    private int hotLimit = 10;

    /** 热门评分权重：score = view*w1 + like*w2 + collect*w3 + comment*w4 */
    private int hotWeightView = 1;
    private int hotWeightLike = 2;
    private int hotWeightCollect = 3;
    private int hotWeightComment = 1;
}
