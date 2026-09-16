package cn.zszj.framework.idempotent.config;

import cn.zszj.framework.idempotent.core.aop.IdempotentAspect;
import cn.zszj.framework.idempotent.core.keyresolver.impl.DefaultIdempotentKeyResolver;
import cn.zszj.framework.idempotent.core.keyresolver.impl.ExpressionIdempotentKeyResolver;
import cn.zszj.framework.idempotent.core.keyresolver.IdempotentKeyResolver;
import cn.zszj.framework.idempotent.core.keyresolver.impl.UserIdempotentKeyResolver;
import cn.zszj.framework.idempotent.core.persistent.PersistentIdempotentStore;
import cn.zszj.framework.idempotent.core.redis.IdempotentRedisDAO;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import cn.zszj.framework.redis.config.ZszjRedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

@AutoConfiguration(after = ZszjRedisAutoConfiguration.class)
public class ZszjIdempotentConfiguration {

    /**
     * persistentStoreProvider（ZS-SEC-011.B）：持久化幂等存储由业务基建模块按需提供
     * （如 zszj-module-infra 的 JdbcPersistentIdempotentStore），starter 不反向依赖业务模块；
     * 未提供时仅 persistent=true 的注解会 fail-fast，Redis 窗口路径零感知。
     *
     * digestSecret（codex r1 P2-C）：部署期注入的摘要 pepper（zszj.security.idempotent.digest.secret，
     * 环境变量 ZSZJ_SECURITY_IDEMPOTENT_DIGEST_SECRET，须 ≥32 字符）——不硬编码；切面构造期校验：
     * 配置了但过短即启动失败；未配置则持久化幂等模式拒绝启用（Redis 窗口模式不受影响），不静默降级
     */
    @Bean
    public IdempotentAspect idempotentAspect(List<IdempotentKeyResolver> keyResolvers, IdempotentRedisDAO idempotentRedisDAO,
                                             ObjectProvider<PersistentIdempotentStore> persistentStoreProvider,
                                             @Value("${zszj.security.idempotent.digest.secret:}") String digestSecret) {
        return new IdempotentAspect(keyResolvers, idempotentRedisDAO, persistentStoreProvider, digestSecret);
    }

    @Bean
    public IdempotentRedisDAO idempotentRedisDAO(StringRedisTemplate stringRedisTemplate) {
        return new IdempotentRedisDAO(stringRedisTemplate);
    }

    // ========== 各种 IdempotentKeyResolver Bean ==========

    @Bean
    public DefaultIdempotentKeyResolver defaultIdempotentKeyResolver() {
        return new DefaultIdempotentKeyResolver();
    }

    @Bean
    public UserIdempotentKeyResolver userIdempotentKeyResolver() {
        return new UserIdempotentKeyResolver();
    }

    @Bean
    public ExpressionIdempotentKeyResolver expressionIdempotentKeyResolver() {
        return new ExpressionIdempotentKeyResolver();
    }

}
