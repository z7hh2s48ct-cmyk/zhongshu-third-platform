package cn.zszj.framework.redis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

/**
 * Cache 配置项
 *
 * @author Wanwan
 */
@ConfigurationProperties("zszj.cache")
@Data
@Validated
public class ZszjCacheProperties {

    /**
     * {@link #redisScanBatchSize} 默认值
     */
    private static final Integer REDIS_SCAN_BATCH_SIZE_DEFAULT = 30;

    /**
     * redis scan 一次返回数量
     */
    private Integer redisScanBatchSize = REDIS_SCAN_BATCH_SIZE_DEFAULT;

    /**
     * ZS-PERM-004.C：一致性受管缓存白名单——命中者启用「驱逐失败补偿记录 + 版本校验防旧值写回」；
     * 默认空 = 框架行为与现状完全一致（零回归面）。框架层不硬编码业务缓存名（由应用层显式声明）。
     */
    private List<String> consistencyGuardedCacheNames = new ArrayList<>();

}
