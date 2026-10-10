package com.sky.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 文章分类。删除前必须确认没有文章在用它（Service 层校验）。 */
@Data
@TableName("t_category")
public class Category {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 前台导航排序 */
    private Integer sortOrder;

    private String createdAt;

    /** 展示用：该分类下"已发布"文章数。不是表字段，所以 exist=false。 */
    @TableField(exist = false)
    private Long articleCount;
}
