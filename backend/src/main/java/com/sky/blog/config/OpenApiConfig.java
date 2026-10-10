package com.sky.blog.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI 文档元信息。
 *
 * <p>认证方式是 Servlet Session（Cookie: JSESSIONID），所以这里声明的是 apiKey-in-cookie，
 * 而不是 Bearer Token。在 Swagger UI 上点 Authorize 不需要填东西，
 * 只要先用 /api/auth/login 登录过（同一个浏览器会带上 Cookie），受保护接口就能直接试。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI blogOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("个人博客系统 API")
                        .version("1.0.0")
                        .description("""
                                游客可浏览已发布文章；注册用户可发文/评论/点赞/收藏；管理员可审核与后台管理。

                                认证：登录接口写入服务端 Session，浏览器自动带 JSESSIONID Cookie。
                                错误约定：HTTP 状态码表达类别，响应体 code 表达具体规则。
                                401=未登录或会话失效；403=无权限；404=不存在或不可见；
                                409=状态冲突（如重复审核）；422=业务规则拒绝（如驳回未填原因）。
                                """)
                        .contact(new Contact().name("学生项目 · 个人博客系统")))
                .components(new Components().addSecuritySchemes("sessionCookie",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("JSESSIONID")
                                .description("服务端 Session，调用 /api/auth/login 后由浏览器自动携带")))
                .addSecurityItem(new SecurityRequirement().addList("sessionCookie"));
    }
}
