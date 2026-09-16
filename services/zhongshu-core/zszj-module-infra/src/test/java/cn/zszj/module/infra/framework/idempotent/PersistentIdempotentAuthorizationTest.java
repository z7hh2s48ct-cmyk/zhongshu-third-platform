package cn.zszj.module.infra.framework.idempotent;

import cn.zszj.framework.idempotent.core.aop.IdempotentAspect;
import cn.zszj.framework.idempotent.core.annotation.Idempotent;
import cn.zszj.framework.idempotent.core.keyresolver.IdempotentKeyResolver;
import cn.zszj.framework.idempotent.core.keyresolver.impl.DefaultIdempotentKeyResolver;
import cn.zszj.framework.idempotent.core.persistent.PersistentIdempotentRecord;
import cn.zszj.framework.idempotent.core.persistent.PersistentIdempotentStatus;
import cn.zszj.framework.idempotent.core.persistent.PersistentIdempotentStore;
import cn.zszj.framework.idempotent.core.redis.IdempotentRedisDAO;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 「重试重检授权」合同测试（ZS-SEC-011.B，H2 + Spring Security 方法安全）。
 *
 * <p>证明两层合同：
 * <ol>
 *     <li><b>次序合同</b>：@PreAuthorize（方法安全拦截器 order≈400）恒先于幂等切面
 *         （{@link IdempotentAspect} 显式 {@code @Order(LOWEST_PRECEDENCE)}）——撤权主体重放同键请求在
 *         授权层即被 AccessDeniedException 拒绝，幂等快照<b>不被返回</b>、业务不重执行；
 *         幂等记录/快照不构成授权缓存；</li>
 *     <li><b>恢复复用</b>：重新授权后重放 → 过授权 → 复用原结果（业务仍不重执行）——
 *         重试每次都重新过完整安全链，不把首次成功当永久授权。</li>
 * </ol>
 * HTTP 过滤器链（认证/租户）重放联验的端到端面归 ZS-SEC-012 既有全链回归入口；本测试钉方法安全与幂等切面的次序。
 */
@Import({JdbcPersistentIdempotentStore.class, PersistentIdempotentAuthorizationTest.AuthorizationFixtureConfiguration.class})
public class PersistentIdempotentAuthorizationTest extends BaseDbUnitTest {

    private static final String PERMISSION = "idem:test:execute";

    /** 测试用摘要 pepper（≥32 字符，模拟部署期注入 ZSZJ_SECURITY_IDEMPOTENT_DIGEST_SECRET） */
    private static final String TEST_DIGEST_SECRET = "authorization-test-digest-secret-0123456789abcdef";

    @Resource
    private SecuredOrderEntry securedOrderEntry;

    @Resource
    private SecuredOrderExecutor securedOrderExecutor;

    @Resource
    private PersistentIdempotentStore store;

    @Resource
    private javax.sql.DataSource dataSource;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @org.junit.jupiter.api.BeforeEach
    void resetFixture() {
        securedOrderExecutor.reset();
    }

    @Test
    void grantedSubject_firstCall_recordsExecution() {
        login(PERMISSION);
        // 持久化幂等要求登录主体（aspect fail-closed 合同），须带请求主体上下文
        try (PersistentIdempotentIntegrationTest.SubjectContext ctx =
                     PersistentIdempotentIntegrationTest.SubjectContext.of(1L, 100L, 2)) {
            assertEquals("result-A", securedOrderEntry.create("A"));
        }
        assertEquals(1, securedOrderExecutor.executions());
        assertEquals(PersistentIdempotentStatus.SUCCESS, onlyRow().orElseThrow().getStatus());
    }

    @Test
    void revokedSubject_replay_deniedBeforeIdempotentReuse() {
        // 首次：有权执行并落 SUCCESS 记录
        login(PERMISSION);
        try (PersistentIdempotentIntegrationTest.SubjectContext ctx =
                     PersistentIdempotentIntegrationTest.SubjectContext.of(1L, 100L, 2)) {
            assertEquals("result-A", securedOrderEntry.create("A"));
        }

        // 撤权后同主体同键重放：授权层先拒绝——快照不返回、业务不重执行、记录不被改写
        login(); // 无任何权限
        try (PersistentIdempotentIntegrationTest.SubjectContext ctx =
                     PersistentIdempotentIntegrationTest.SubjectContext.of(1L, 100L, 2)) {
            assertThrows(AccessDeniedException.class, () -> securedOrderEntry.create("A"));
        }
        assertEquals(1, securedOrderExecutor.executions(), "无权重放不得执行业务");
        PersistentIdempotentRecord record = onlyRow().orElseThrow();
        assertEquals(PersistentIdempotentStatus.SUCCESS, record.getStatus(), "拒绝发生在幂等层之前，记录原样保留");
        assertEquals("\"result-A\"", record.getResultSnapshot(), "快照原样保留（未因拒绝被改写）");
    }

    @Test
    void regrantedSubject_replay_passesAuthThenReusesOriginalResult() {
        login(PERMISSION);
        try (PersistentIdempotentIntegrationTest.SubjectContext ctx =
                     PersistentIdempotentIntegrationTest.SubjectContext.of(1L, 100L, 2)) {
            assertEquals("result-A", securedOrderEntry.create("A"));
        }

        login(PERMISSION); // 重新授权（恢复场景）
        try (PersistentIdempotentIntegrationTest.SubjectContext ctx =
                     PersistentIdempotentIntegrationTest.SubjectContext.of(1L, 100L, 2)) {
            assertEquals("result-A", securedOrderEntry.create("A"), "重新授权后重放应过授权并复用原结果");
        }
        assertEquals(1, securedOrderExecutor.executions(), "重放复用快照，业务不重执行");
    }

    // ========== Fixture ==========

    @Configuration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy
    @org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
    static class AuthorizationFixtureConfiguration {

        @Bean
        public DefaultIdempotentKeyResolver defaultIdempotentKeyResolver() {
            return new DefaultIdempotentKeyResolver();
        }

        @Bean
        public IdempotentAspect idempotentAspect(List<IdempotentKeyResolver> keyResolvers,
                                                 ObjectProvider<PersistentIdempotentStore> persistentStoreProvider) {
            // 第 4 参为部署期注入的摘要 pepper（r1 P2-C，测试以常量模拟）
            return new IdempotentAspect(keyResolvers, new IdempotentRedisDAO(new StringRedisTemplate()),
                    persistentStoreProvider, TEST_DIGEST_SECRET);
        }

        @Bean
        public SecuredOrderExecutor securedOrderExecutor() {
            return new SecuredOrderExecutor();
        }

        @Bean
        public SecuredOrderEntry securedOrderEntry(SecuredOrderExecutor executor) {
            return new SecuredOrderEntry(executor);
        }
    }

    /**
     * @PreAuthorize 与 @Idempotent(persistent=true) 落在同一方法——钉「方法安全先于幂等切面」次序合同。
     * （事务边界仍在外层入口：授权与幂等同方法、事务在调用方，互不依赖切面次序。）
     */
    static class SecuredOrderExecutor {

        private final AtomicInteger executions = new AtomicInteger();

        @Idempotent(persistent = true)
        @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('" + PERMISSION + "')")
        public String execute(String req) {
            executions.incrementAndGet();
            return "result-" + req;
        }

        int executions() {
            return executions.get();
        }

        /** 单例 bean 的计数跨测试方法共享上下文，须在每个用例前归零 */
        void reset() {
            executions.set(0);
        }
    }

    static class SecuredOrderEntry {

        private final SecuredOrderExecutor executor;

        SecuredOrderEntry(SecuredOrderExecutor executor) {
            this.executor = executor;
        }

        @Transactional
        public String create(String req) {
            return executor.execute(req);
        }
    }

    // ========== Helpers ==========

    private void login(String... authorities) {
        List<SimpleGrantedAuthority> granted = java.util.Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .toList();
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("user-100", "n/a", granted));
    }

    private Optional<PersistentIdempotentRecord> onlyRow() {
        // 断言用的直查（同主体同键只有一条记录；clean.sql 保证用例间隔离）
        String key = new org.springframework.jdbc.core.JdbcTemplate(dataSource).queryForObject(
                "SELECT idempotent_key FROM infra_persistent_idempotent", String.class);
        return store.findByIdempotentKey(key);
    }

}
