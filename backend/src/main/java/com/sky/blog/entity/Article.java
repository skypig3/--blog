package com.sky.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.sky.blog.enums.ArticleStatus;
import lombok.Data;

/**
 * 文章。
 *
 * <p>状态流转只允许走 {@link ArticleStatus} 里的表；每次流转都用
 * {@code UPDATE ... WHERE status = 原状态} 落地（CAS），影响 0 行即代表状态已被他人改动。
 *
 * <p>{@code likeCount / collectCount / commentCount / viewCount} 是冗余计数，
 * 供列表展示和热门排序使用，必须与关系表在同一事务内增减。
 */
@Data
@TableName("t_article")
public class Article {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 数据归属校验的依据：只有作者本人（或管理员）能改 */
    private Long authorId;

    private Long categoryId;

    private String title;

    /** 列表摘要；作者留空时由 Service 从正文自动截取 */
    private String summary;

    /** Markdown 原文，渲染在前端做（markdown-it + DOMPurify） */
    private String contentMd;

    private String cover;

    private ArticleStatus status;

    /** 驳回原因，作者可见 */
    private String rejectReason;

    /** 下架原因，作者可见 */
    private String offlineReason;

    private Integer viewCount;

    private Integer likeCount;

    private Integer collectCount;

    private Integer commentCount;

    private String submittedAt;

    private Long auditUserId;

    private String auditedAt;

    /** 最近一次审核通过时间，列表排序和热门窗口都用它 */
    private String publishedAt;

    @TableLogic
    private Integer deleted;

    private String createdAt;

    private String updatedAt;

    /** 作者当前是否允许编辑正文（状态机判断，不落库、不入库） */
    public boolean isEditableByAuthor() {
        return status != null && status.can(ArticleStatus.Transition.EDIT);
    }

    /** 前台是否可见 */
    public boolean isPubliclyVisible() {
        return status != null && status.visibleToPublic();
    }
}
