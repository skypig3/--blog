package com.sky.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 点赞记录。表上有 {@code UNIQUE(user_id, article_id)}，
 * 所以点赞是幂等开关：重复点不会产生第二行，也就刷不了 like_count。
 */
@Data
@TableName("t_article_like")
public class ArticleLike {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long articleId;

    private String createdAt;
}
