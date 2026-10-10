package com.sky.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.sky.blog.enums.Role;
import com.sky.blog.enums.UserStatus;
import lombok.Data;

/**
 * 用户。
 *
 * <p>安全红线（作业 2.2.1）：{@code passwordHash} 永远不出现在任何出参 VO 里，也不写日志。
 * 对外一律走 {@code UserVo}，字段结构上就不存在密码。
 *
 * <p>时间字段用 String 而不是 LocalDateTime，原因见 {@code TimeUtil} 的类注释（实测格式冲突）。
 */
@Data
@TableName("t_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 注册后不可修改（作业 2.2.1） */
    private String username;

    private String email;

    /** BCrypt 哈希，$2a$10$ 开头、定长 60 */
    @TableField("password_hash")
    private String passwordHash;

    private String nickname;

    private String avatar;

    private String bio;

    private Role role;

    private UserStatus status;

    /** 改密码时 +1；会话里存的是登录那一刻的值，不一致就判定会话失效 */
    private Integer credentialVersion;

    @TableLogic
    private Integer deleted;

    private String createdAt;

    private String updatedAt;

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }
}
