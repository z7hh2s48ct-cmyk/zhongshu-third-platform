package cn.zszj.module.infra.framework.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 导出生成与用途保留期清理配置（ZS-FILE-004.B）。
 *
 * <p>全部取值为「保守占位」，待真实对象存储接入与运行数据积累后实测回填；
 * 禁止被解读为容量/清理时效承诺（循 ZS-FILE-005.B 阈值占位同一红线）。</p>
 *
 * @author ZS-FILE-004.B
 */
@ConfigurationProperties(prefix = "infra.file.export")
@Validated
@Data
public class FileExportProperties {

    /**
     * 导出件保留期（天）：生成时刻 + 该天数 = retention_expire_time。
     * 保守占位 7 天，待实测回填。
     */
    private Integer retentionDays = 7;

    /**
     * 预览单次返回候选上限（超过即如实标注截断）。保守占位 200，待实测回填。
     */
    private Integer previewMaxItems = 200;

    /**
     * 单次授权清理 id 上限（有界爆炸半径，操作方分批授权）。保守占位 100，待实测回填。
     */
    private Integer cleanupMaxIds = 100;

}
