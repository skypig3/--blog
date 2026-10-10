package com.sky.blog.controller;

import com.sky.blog.common.ApiResult;
import com.sky.blog.dto.AdminDto;
import com.sky.blog.enums.Role;
import com.sky.blog.enums.UserStatus;
import com.sky.blog.security.Principal;
import com.sky.blog.security.RequireRole;
import com.sky.blog.service.UserService;
import com.sky.blog.vo.PageVo;
import com.sky.blog.vo.UserVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 后台用户管理。类上标了 @RequireRole(ADMIN)，本包下所有方法都要求管理员身份。 */
@Tag(name = "90 后台-用户管理", description = "仅 ADMIN 可访问；接口不返回密码与密码哈希")
@RequireRole(Role.ADMIN)
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "用户列表", description = "支持按用户名/邮箱/昵称模糊搜索，可按角色和状态筛选")
    @GetMapping
    public ApiResult<PageVo<UserVo.Detail>> page(
            @Parameter(description = "用户名、邮箱或昵称关键字") @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResult.ok(userService.adminPage(current, Math.min(size, 100), keyword, role, status));
    }

    @Operation(summary = "查看用户基本资料")
    @GetMapping("/{id}")
    public ApiResult<UserVo.Detail> detail(@PathVariable Long id) {
        return ApiResult.ok(userService.detail(id));
    }

    @Operation(summary = "禁用 / 恢复账号",
            description = "禁用最后一个启用状态的管理员会被拒绝（422）；也不能禁用自己")
    @PatchMapping("/{id}/status")
    public ApiResult<UserVo.Detail> setStatus(@PathVariable Long id,
                                              @Valid @RequestBody AdminDto.UserStatusUpdate dto) {
        return ApiResult.ok(userService.adminSetStatus(Principal.Holder.require(), id, dto), "状态已更新");
    }

    @Operation(summary = "修改用户角色", description = "降级最后一个启用状态的管理员会被拒绝（422）")
    @PatchMapping("/{id}/role")
    public ApiResult<UserVo.Detail> setRole(@PathVariable Long id,
                                            @Valid @RequestBody AdminDto.UserRoleUpdate dto) {
        return ApiResult.ok(userService.adminSetRole(Principal.Holder.require(), id, dto), "角色已更新");
    }
}
