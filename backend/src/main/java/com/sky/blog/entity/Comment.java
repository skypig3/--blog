package com.sky.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.sky.blog.enums.CommentStatus;
import lombok.Data;

/**
 * 评论 / 回复。
 *
 * <p>回复和顶层评论共用本表，用 {@code parentId} 区分，
 * 所以作业要求的"评论审核应覆盖回复内容"天然成立，不需要第二套逻辑。
 */
@Data
@TableName("t_comment")
public class Comment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long articleId;

    /** null = 顶层评论；非 null = 对该评论的回复 */
    private Long parentId;

    private Long userId;

    private String content;

    private CommentStatus status;

    private String rejectReason;

    private Long auditUserId;

    private String auditedAt;

    @TableLogic
    private Integer deleted;

    private String createdAt;
}
