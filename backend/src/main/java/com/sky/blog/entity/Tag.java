package com.sky.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 标签。与文章多对多，关系表见 {@link ArticleTag}。 */
@Data
@TableName("t_tag")
public class Tag {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String createdAt;

    /** 展示用：该标签下"已发布"文章数。不是表字段，所以 exist=false。 */
    @TableField(exist = false)
    private Long articleCount;
}
