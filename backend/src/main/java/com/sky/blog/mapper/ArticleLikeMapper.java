package com.sky.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.blog.entity.ArticleLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ArticleLikeMapper extends BaseMapper<ArticleLike> {

    /** 表上有 UNIQUE(user_id, article_id)，所以有记录就等于点过赞 */
    @Select("SELECT COUNT(*) FROM t_article_like WHERE user_id = #{userId} AND article_id = #{articleId}")
    boolean exists(@Param("userId") Long userId, @Param("articleId") Long articleId);
}
