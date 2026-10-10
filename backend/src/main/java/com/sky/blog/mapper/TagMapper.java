package com.sky.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.blog.entity.Tag;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {

    /** 标签入口：带上每个标签下"已发布"文章数，按使用量降序。 */
    @Select("""
            SELECT t.id, t.name, t.created_at,
                   COUNT(DISTINCT CASE WHEN a.status = 'PUBLISHED' AND a.deleted = 0
                                       THEN atg.article_id END) AS article_count
            FROM t_tag t
            LEFT JOIN t_article_tag atg ON atg.tag_id = t.id
            LEFT JOIN t_article a ON a.id = atg.article_id
            GROUP BY t.id, t.name, t.created_at
            ORDER BY article_count DESC, t.id ASC
            """)
    List<Tag> selectAllWithCount();
}
