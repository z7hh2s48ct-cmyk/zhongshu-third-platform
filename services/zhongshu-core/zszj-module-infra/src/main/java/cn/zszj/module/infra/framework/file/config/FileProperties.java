package cn.zszj.module.infra.framework.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * 文件上传配置（ZS-FILE-002：限额与类型合同，具体限额进入配置合同）
 *
 * @author ZS-FILE-002
 */
@ConfigurationProperties(prefix = "zszj.file")
@Validated
@Data
public class FileProperties {

    /**
     * 单文件大小上限（字节）。默认 16MB，与 spring.servlet.multipart.max-file-size 对齐。
     * service 层显式校验，不依赖 multipart 兜底。
     */
    private Long maxSize = 16 * 1024 * 1024L;

    /**
     * 危险扩展名黑名单（按最终扩展名精确匹配，忽略大小写）：命中即拒绝上传（隔离语义）。
     * codex r0 P1：svg 并入黑名单——SVG 可携带脚本，经 writeAttachment 内联渲染存在存储型 XSS 面。
     */
    private List<String> dangerExtensions = List.of(
            "exe", "dll", "bat", "cmd", "sh", "js", "vbs", "msi", "com", "scr", "ps1", "svg");

    /**
     * 公开素材（PUBLIC）允许的类型白名单（精确匹配，忽略大小写）。转 PUBLIC 时校验；
     * codex r0 P1：由前缀 "image/" 改为显式安全 MIME 枚举——前缀 "image/" 会放进
     * image/svg+xml（可携带脚本），不做净化不得公开。空列表表示不限制。
     */
    private List<String> publicAllowedTypes = List.of(
            "image/png", "image/jpeg", "image/gif", "image/webp", "application/pdf", "text/plain");

    /**
     * 上传并发上限（ZS-FILE-002 codex r0 P2：批量/内存占用预算）——
     * 在途上传许可数，超出即拒绝（失败不返回成功资产）。
     */
    private Integer maxConcurrentUploads = 32;

    /**
     * 外部存储（S3 兼容）单次 API 调用总超时秒数（codex r0 P2 登记合同；接线见 S3FileClient）。
     */
    private Long s3ApiCallTimeoutSeconds = 60L;

}
