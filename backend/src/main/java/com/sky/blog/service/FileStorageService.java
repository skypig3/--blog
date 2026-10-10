package com.sky.blog.service;

import com.sky.blog.common.BizException;
import com.sky.blog.common.ErrorCode;
import com.sky.blog.config.BlogProperties;
import com.sky.blog.vo.SiteVo;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图片上传（头像、文章封面、正文配图）。
 *
 * <p>三道校验，缺一不可：
 * <ol>
 *   <li>扩展名白名单（配置在 {@code blog.upload-allowed-ext}）；</li>
 *   <li>大小上限（配置在 {@code blog.upload-max-size-mb}）；</li>
 *   <li><b>文件头魔数</b>：只把扩展名当依据的话，把 {@code shell.txt} 改名成 {@code shell.png}
 *       就能绕过，所以这里再读前几个字节确认真是图片。</li>
 * </ol>
 *
 * <p>落盘文件名一律用服务端生成的 UUID，<b>不使用客户端给的文件名</b>，
 * 从根上避免路径穿越（{@code ../../}）和同名覆盖。
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    private final BlogProperties properties;

    public FileStorageService(BlogProperties properties) {
        this.properties = properties;
    }

    public SiteVo.UploadedImage store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "请选择要上传的文件");
        }
        long maxBytes = (long) properties.getUploadMaxSizeMb() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new BizException(ErrorCode.BAD_REQUEST,
                    "文件超过 " + properties.getUploadMaxSizeMb() + "MB 限制");
        }

        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = extensionOf(original);
        if (!properties.getUploadAllowedExt().contains(ext)) {
            throw new BizException(ErrorCode.BAD_REQUEST,
                    "只允许上传图片：" + String.join("、", properties.getUploadAllowedExt()));
        }
        byte[] header = readHeader(file);
        if (!looksLikeImage(ext, header)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "文件内容与扩展名不符，可能是伪造的图片");
        }

        String month = LocalDate.now().format(MONTH);
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path dir = Path.of(properties.getUploadDir(), month).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, dir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("写入上传文件失败 dir={} name={}", dir, filename, e);
            throw new BizException(ErrorCode.SERVER_ERROR, "文件保存失败，请重试");
        }
        String url = "/uploads/" + month + "/" + filename;
        log.info("上传成功 {} ({} bytes)", url, file.getSize());
        return new SiteVo.UploadedImage(url, original, file.getSize());
    }

    private static String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        // 只取最后一个点之后的部分，并强制小写；出现 / \ .. 等字符一律视为非法
        String ext = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (ext.contains("/") || ext.contains("\\") || ext.contains("..")) {
            throw new BizException(ErrorCode.BAD_REQUEST, "文件名不合法");
        }
        return ext;
    }

    private static byte[] readHeader(MultipartFile file) {
        byte[] buf = new byte[12];
        try (InputStream in = file.getInputStream()) {
            int read = in.read(buf);
            return read <= 0 ? new byte[0] : java.util.Arrays.copyOf(buf, read);
        } catch (IOException e) {
            throw new BizException(ErrorCode.BAD_REQUEST, "无法读取文件内容");
        }
    }

    /** 按文件头判断是否真是图片；gif/webp 也各自校验特征字节 */
    private static boolean looksLikeImage(String ext, byte[] h) {
        if (h.length < 4) {
            return false;
        }
        return switch (ext) {
            case "png" -> (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G';
            case "jpg", "jpeg" -> (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF;
            case "gif" -> h[0] == 'G' && h[1] == 'I' && h[2] == 'F' && h[3] == '8';
            case "webp" -> h.length >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                    && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P';
            default -> false;
        };
    }
}
