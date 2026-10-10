package com.sky.blog.controller;

import com.sky.blog.common.ApiResult;
import com.sky.blog.dto.AuthDto;
import com.sky.blog.security.Principal;
import com.sky.blog.security.RequireLogin;
import com.sky.blog.service.UserService;
import com.sky.blog.vo.UserVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 个人中心：查看/修改资料、修改密码。全部要求登录，且只能动自己的数据。 */
@Tag(name = "02 个人中心", description = "资料查看与修改、改密码")
@RequireLogin
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "查看我的资料")
    @GetMapping("/profile")
    public ApiResult<UserVo.Detail> profile() {
        return ApiResult.ok(userService.detail(Principal.Holder.requireUserId()));
    }

    @Operation(summary = "修改我的资料", description = "用户名不可修改，所以入参里没有 username 字段")
    @PutMapping("/profile")
    public ApiResult<UserVo.Detail> updateProfile(@Valid @RequestBody AuthDto.ProfileUpdate dto) {
        return ApiResult.ok(userService.updateProfile(Principal.Holder.requireUserId(), dto), "资料已更新");
    }

    @Operation(summary = "修改密码",
            description = "需校验原密码；成功后 credential_version 加一并立即销毁当前 Session，"
                    + "所有旧会话在下一次请求时收到 401")
    @PutMapping("/password")
    public ApiResult<Void> changePassword(@Valid @RequestBody AuthDto.PasswordUpdate dto, HttpSession session) {
        userService.changePassword(Principal.Holder.requireUserId(), dto, session);
        return ApiResult.ok(null, "密码已修改，请重新登录");
    }
}
