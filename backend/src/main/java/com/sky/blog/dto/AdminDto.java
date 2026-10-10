package com.sky.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 后台管理入参。 */
public final class AdminDto {

    private AdminDto() {
    }

    @Schema(description = "启用/禁用账号")
    public record UserStatusUpdate(
            @Schema(allowableValues = {"ACTIVE", "DISABLED"})
            @NotBlank(message = "必须指明状态")
            String status,

            @Size(max = 200)
            String remark) {
    }

    @Schema(description = "修改用户角色")
    public record UserRoleUpdate(
            @Schema(allowableValues = {"USER", "ADMIN"})
            @NotBlank(message = "必须指明角色")
            String role,

            @Size(max = 200)
            String remark) {
    }

    @Schema(description = "新建/修改分类")
    public record CategorySave(
            @NotBlank(message = "分类名不能为空")
            @Size(max = 30, message = "分类名最长 30 字")
            String name,

            Integer sortOrder) {
    }

    @Schema(description = "新建/修改标签")
    public record TagSave(
            @NotBlank(message = "标签名不能为空")
            @Size(max = 30, message = "标签名最长 30 字")
            String name) {
    }

    @Schema(description = "删除类操作的说明")
    public record Remark(
            @Size(max = 200)
            String remark) {
    }
}
