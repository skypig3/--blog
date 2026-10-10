package com.sky.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.blog.entity.ArticleCollect;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ArticleCollectMapper extends BaseMapper<ArticleCollect> {

    @Select("SELECT COUNT(*) FROM t_article_collect WHERE user_id = #{userId} AND article_id = #{articleId}")
    boolean exists(@Param("userId") Long userId, @Param("articleId") Long articleId);
}
