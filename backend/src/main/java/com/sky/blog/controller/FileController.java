package com.sky.blog.controller;

import com.sky.blog.common.ApiResult;
import com.sky.blog.security.RequireLogin;
import com.sky.blog.service.FileStorageService;
import com.sky.blog.vo.SiteVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 图片上传：头像、封面、正文配图。 */
@Tag(name = "60 文件", description = "需登录")
@RequireLogin
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Operation(summary = "上传图片",
            description = "multipart/form-data；扩展名白名单 + 大小上限 + 文件头魔数三重校验；"
                    + "落盘文件名由服务端用 UUID 生成，返回的 url 可直接给 img src 使用")
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResult<SiteVo.UploadedImage> uploadImage(@RequestPart("file") MultipartFile file) {
        return ApiResult.ok(fileStorageService.store(file), "上传成功");
    }
}
