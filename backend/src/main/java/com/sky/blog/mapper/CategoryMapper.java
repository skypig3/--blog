package com.sky.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.blog.entity.Category;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

    /**
     * 分类导航：带上每个分类下"已发布"文章数。
     * LEFT JOIN 保证 0 篇的分类也要出现在导航里。
     */
    @Select("""
            SELECT c.id, c.name, c.sort_order, c.created_at,
                   COUNT(a.id) AS article_count
            FROM t_category c
            LEFT JOIN t_article a
                   ON a.category_id = c.id AND a.status = 'PUBLISHED' AND a.deleted = 0
            GROUP BY c.id, c.name, c.sort_order, c.created_at
            ORDER BY c.sort_order ASC, c.id ASC
            """)
    List<Category> selectAllWithCount();
}
