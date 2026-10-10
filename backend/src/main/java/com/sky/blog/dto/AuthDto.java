package com.sky.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 认证与账号相关的入参。 */
public final class AuthDto {

    private AuthDto() {
    }

    /**
     * 注册入参。
     *
     * <p>注意这里<b>故意没有 role 字段</b>：角色一律由服务端写死为 USER，
     * 满足作业 2.3.2"普通注册流程只能创建普通用户，不能通过提交角色字段注册管理员"。
     */
    @Schema(description = "注册请求")
    public record Register(
            @Schema(description = "用户名，注册后不可修改", example = "xiaoming")
            @NotBlank(message = "用户名不能为空")
            @Pattern(regexp = "^[A-Za-z0-9_]{3,20}$",
                    message = "用户名只能是 3-20 位字母、数字或下划线")
            String username,

            @Schema(description = "邮箱，不得重复", example = "xiaoming@example.com")
            @NotBlank(message = "邮箱不能为空")
            @Email(message = "邮箱格式不正确")
            @Size(max = 100, message = "邮箱过长")
            String email,

            @Schema(description = "密码", example = "Abc12345")
            @NotBlank(message = "密码不能为空")
            @Size(min = 6, max = 64, message = "密码长度需在 6-64 位之间")
            String password,

            @Schema(description = "昵称", example = "小明")
            @NotBlank(message = "昵称不能为空")
            @Size(max = 30, message = "昵称最长 30 字")
            String nickname) {
    }

    @Schema(description = "登录请求：account 填用户名或邮箱都可以")
    public record Login(
            @Schema(description = "用户名或邮箱", example = "admin")
            @NotBlank(message = "请输入用户名或邮箱")
            String account,

            @Schema(description = "密码", example = "Admin@123")
            @NotBlank(message = "请输入密码")
            String password) {
    }

    /** 改资料：不含 username（作业要求用户名可设为不可修改）。 */
    @Schema(description = "修改个人资料请求")
    public record ProfileUpdate(
            @Schema(example = "小明同学")
            @NotBlank(message = "昵称不能为空")
            @Size(max = 30, message = "昵称最长 30 字")
            String nickname,

            @Schema(description = "头像相对路径", example = "/uploads/2026-10/a.png")
            @Size(max = 255, message = "头像地址过长")
            String avatar,

            @Schema(description = "个人简介")
            @Size(max = 200, message = "简介最长 200 字")
            String bio) {
    }

    @Schema(description = "修改密码请求：先验原密码，成功后当前会话立即失效")
    public record PasswordUpdate(
            @NotBlank(message = "请输入原密码")
            String oldPassword,

            @NotBlank(message = "请输入新密码")
            @Size(min = 6, max = 64, message = "新密码长度需在 6-64 位之间")
            String newPassword) {
    }
}
