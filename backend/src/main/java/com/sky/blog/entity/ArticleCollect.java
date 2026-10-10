package com.sky.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 收藏记录，语义与点赞一致：{@code UNIQUE(user_id, article_id)} 保证幂等。 */
@Data
@TableName("t_article_collect")
public class ArticleCollect {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long articleId;

    private String createdAt;
}
