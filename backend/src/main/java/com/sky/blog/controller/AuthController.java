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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 注册、登录、退出、当前用户。 */
@Tag(name = "01 认证", description = "注册 / 登录 / 退出 / 当前登录用户")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "注册", description = "用户名与邮箱都不得重复；角色由服务端固定为 USER，不接受客户端传入")
    @PostMapping("/register")
    public ApiResult<UserVo.Detail> register(@Valid @RequestBody AuthDto.Register dto) {
        return ApiResult.ok(userService.register(dto), "注册成功");
    }

    @Operation(summary = "登录", description = "account 填用户名或邮箱均可；成功后写入服务端 Session（JSESSIONID Cookie）")
    @PostMapping("/login")
    public ApiResult<UserVo.Detail> login(@Valid @RequestBody AuthDto.Login dto, HttpSession session) {
        return ApiResult.ok(userService.login(dto, session), "登录成功");
    }

    @Operation(summary = "退出", description = "销毁 Session；退出后不得继续执行需要登录的操作")
    @RequireLogin
    @PostMapping("/logout")
    public ApiResult<Void> logout(HttpSession session) {
        userService.logout(session);
        return ApiResult.ok(null, "已退出登录");
    }

    @Operation(summary = "当前登录用户", description = "前端启动时用它判断登录态；未登录返回 401")
    @RequireLogin
    @GetMapping("/me")
    public ApiResult<UserVo.Detail> me() {
        return ApiResult.ok(userService.detail(Principal.Holder.requireUserId()));
    }
}
