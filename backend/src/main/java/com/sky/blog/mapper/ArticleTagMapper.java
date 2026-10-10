package com.sky.blog.mapper;

import com.sky.blog.entity.ArticleTag;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 文章-标签关系表。
 *
 * <p><b>故意不继承 BaseMapper</b>：这张表的主键是 {@code (article_id, tag_id)} 两列，
 * 没有单一 id，继承 BaseMapper 只会带来一堆用不了的 {@code xxById} 方法
 * 和一条启动告警（"Not found @TableId annotation"）。
 * 所以这里只写真正需要的四条注解 SQL。
 */
@Mapper
public interface ArticleTagMapper {

    @Delete("DELETE FROM t_article_tag WHERE article_id = #{articleId}")
    int deleteByArticle(@Param("articleId") Long articleId);

    /** 整体替换语义：先 deleteByArticle 再 insertBatch */
    @Insert("<script>INSERT INTO t_article_tag(article_id, tag_id) VALUES "
            + "<foreach collection='tagIds' item='tid' separator=','>"
            + "(#{articleId}, #{tid})</foreach></script>")
    int insertBatch(@Param("articleId") Long articleId, @Param("tagIds") List<Long> tagIds);

    @Select("SELECT tag_id FROM t_article_tag WHERE article_id = #{articleId}")
    List<Long> selectTagIds(@Param("articleId") Long articleId);

    /** 列表页批量取标签，避免 N+1：一次查回多篇的 (article_id, tag_id) 再在内存分组 */
    @Select("<script>SELECT article_id, tag_id FROM t_article_tag WHERE article_id IN "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + "</script>")
    List<ArticleTag> selectByArticleIds(@Param("ids") List<Long> ids);

    /** 删除标签前的占用检查 */
    @Select("SELECT COUNT(*) FROM t_article_tag WHERE tag_id = #{tagId}")
    long countByTag(@Param("tagId") Long tagId);
}
