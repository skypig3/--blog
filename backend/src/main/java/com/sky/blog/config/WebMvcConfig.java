package com.sky.blog.config;

import com.sky.blog.security.AuthInterceptor;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 层装配：拦截器注册、上传目录静态映射、本地开发跨域。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebMvcConfig.class);

    private final AuthInterceptor authInterceptor;
    private final BlogProperties properties;

    public WebMvcConfig(AuthInterceptor authInterceptor, BlogProperties properties) {
        this.authInterceptor = authInterceptor;
        this.properties = properties;
    }

    /**
     * 只拦 /api/**：静态资源、Swagger UI（/swagger-ui/**）、OpenAPI JSON（/v3/api-docs）都不进拦截器，
     * 否则文档页面自己都会被 401 挡掉。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login", "/api/auth/register");
    }

    /** 上传的图片通过 /uploads/** 访问，物理路径在 data/uploads 下（不打进 jar）。 */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadDir = Path.of(properties.getUploadDir()).toAbsolutePath().normalize();
        log.info("上传目录静态映射：/uploads/** -> {}", uploadDir);
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadDir.toUri().toString());
    }

    /**
     * 本地开发跨域。
     *
     * <p>推荐前端用 Vite 代理（同源，不需要这段）；如果前端直连 8080，
     * 这里放行 localhost 任意端口并允许带 Cookie，Session 认证才能工作。
     * 生产交付时前端 dist 直接由本服务托管，也不涉及跨域。
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("http://localhost:[*]", "http://127.0.0.1:[*]")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
