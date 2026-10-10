package com.sky.blog.controller;

import com.sky.blog.common.ApiResult;
import com.sky.blog.config.BlogProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 系统信息：M0 阶段用来验证"服务起来了 + JSON 序列化正常 + Swagger 能扫到接口"。 */
@Tag(name = "00 系统", description = "运行状态与规则说明（公开）")
@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final BlogProperties properties;

    public SystemController(BlogProperties properties) {
        this.properties = properties;
    }

    @Operation(summary = "服务状态", description = "返回应用名与当前生效的热门排序规则，用于自检")
    @GetMapping("/info")
    public ApiResult<Map<String, Object>> info() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("application", "personal-blog-backend");
        data.put("status", "UP");
        data.put("hotWindowDays", properties.getHotWindowDays());
        data.put("hotLimit", properties.getHotLimit());
        data.put("hotRule", "score = view*%d + like*%d + collect*%d + comment*%d，"
                        .formatted(properties.getHotWeightView(), properties.getHotWeightLike(),
                                properties.getHotWeightCollect(), properties.getHotWeightComment())
                + "取最近 " + properties.getHotWindowDays() + " 天内的已发布文章前 "
                + properties.getHotLimit() + " 篇");
        data.put("uploadMaxSizeMb", properties.getUploadMaxSizeMb());
        data.put("uploadAllowedExt", properties.getUploadAllowedExt());
        return ApiResult.ok(data);
    }
}
