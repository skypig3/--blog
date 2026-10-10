package com.sky.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文章-标签关系（复合主键，无单一 id）。
 *
 * <p>因为主键是两列，不走 BaseMapper 的 selectById 系列，
 * 对应的 {@code ArticleTagMapper} 用注解 SQL 实现增删查。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_article_tag")
public class ArticleTag {

    private Long articleId;

    private Long tagId;

    private String createdAt;

    public ArticleTag(Long articleId, Long tagId) {
        this.articleId = articleId;
        this.tagId = tagId;
    }
}
