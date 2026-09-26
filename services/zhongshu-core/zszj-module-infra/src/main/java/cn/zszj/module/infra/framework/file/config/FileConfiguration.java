package cn.zszj.module.infra.framework.file.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 文件模块配置类（ZS-FILE-002：上传限额与类型合同；ZS-FILE-005.B：补偿与孤儿清理配置；
 * ZS-FILE-004.B：导出生成与用途保留期清理配置）
 *
 * @author ZS-FILE-002
 */
@Configuration
@EnableConfigurationProperties({FileProperties.class, FileCompensationProperties.class, FileExportProperties.class})
public class FileConfiguration {
}
