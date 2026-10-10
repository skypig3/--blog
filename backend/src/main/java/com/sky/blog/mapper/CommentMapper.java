package com.sky.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.blog.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

/** 评论：全部走 BaseMapper + 条件构造器，没有自定义 SQL。 */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
