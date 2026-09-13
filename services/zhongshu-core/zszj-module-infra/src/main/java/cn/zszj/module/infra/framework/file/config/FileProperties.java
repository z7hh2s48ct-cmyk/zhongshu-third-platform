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
     */
    private List<String> dangerExtensions = List.of(
            "exe", "dll", "bat", "cmd", "sh", "js", "vbs", "msi", "com", "scr", "ps1");

    /**
     * 公开素材（PUBLIC）允许的类型白名单（前缀匹配，如 image/）。转 PUBLIC 时校验；
     * 空列表表示不限制。
     */
    private List<String> publicAllowedTypes = List.of("image/", "application/pdf", "text/plain");

}
