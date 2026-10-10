package com.sky.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.blog.entity.Article;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    /**
     * 热门文章。规则（作业 2.2.2 要求在报告里写明）：
     *
     * <pre>
     * 候选：status=PUBLISHED 且未删除 且 published_at 在窗口期内
     * 分值：score = view*wv + like*wl + collect*wc + comment*wm
     * 排序：score DESC，published_at DESC
     * </pre>
     *
     * 自定义 SQL 不会被逻辑删除自动改写，所以 deleted = 0 必须自己带上。
     */
    @Select("""
            SELECT * FROM t_article
            WHERE deleted = 0 AND status = 'PUBLISHED'
              AND published_at IS NOT NULL AND published_at >= #{since}
            ORDER BY (view_count * #{wv} + like_count * #{wl}
                    + collect_count * #{wc} + comment_count * #{wm}) DESC,
                     published_at DESC
            LIMIT #{limit}
            """)
    List<Article> selectHot(@Param("since") String since,
                            @Param("wv") int wv, @Param("wl") int wl,
                            @Param("wc") int wc, @Param("wm") int wm,
                            @Param("limit") int limit);

    /** 阅读量自增：单条 SQL 原子更新，避免"读—改—写"竞态。 */
    @Update("UPDATE t_article SET view_count = view_count + 1 WHERE id = #{id} AND deleted = 0")
    int increaseView(@Param("id") Long id);

    /**
     * 冗余计数增减。用 SQLite 的双参数 max() 兜底，防止计数被减成负数。
     *
     * @param column 只允许 like_count / collect_count / comment_count，由 Service 层常量传入
     */
    @Update("UPDATE t_article SET ${column} = max(${column} + #{delta}, 0) WHERE id = #{id}")
    int changeCounter(@Param("column") String column, @Param("id") Long id, @Param("delta") int delta);

    /** 后台统计用：全站阅读量汇总。COALESCE 保证空库时返回 0 而不是 null。 */
    @Select("SELECT COALESCE(SUM(view_count), 0) FROM t_article WHERE deleted = 0")
    long sumViewCount();
}
