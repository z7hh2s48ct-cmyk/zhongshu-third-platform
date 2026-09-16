package cn.zszj.module.infra.framework.idempotent;

import cn.hutool.crypto.SecureUtil;
import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.zszj.framework.common.util.log.LogSanitizeUtils;
import cn.zszj.framework.common.util.servlet.ServletUtils;
import cn.zszj.framework.idempotent.core.aop.IdempotentAspect;
import cn.zszj.framework.idempotent.core.annotation.Idempotent;
import cn.zszj.framework.idempotent.core.keyresolver.IdempotentKeyResolver;
import cn.zszj.framework.idempotent.core.keyresolver.impl.DefaultIdempotentKeyResolver;
import cn.zszj.framework.idempotent.core.persistent.PersistentIdempotentRecord;
import cn.zszj.framework.idempotent.core.persistent.PersistentIdempotentStatus;
import cn.zszj.framework.idempotent.core.persistent.PersistentIdempotentStore;
import cn.zszj.framework.idempotent.core.redis.IdempotentRedisDAO;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.framework.web.core.util.WebFrameworkUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

/**
 * 持久化幂等端到端集成测试（ZS-SEC-011.B，H2：真切面 + 真 {@link JdbcPersistentIdempotentStore} + @Transactional 服务）。
 *
 * <p>覆盖退出条件的应用层证据：
 * <ul>
 *     <li><b>丢响应→可重放恢复原结果</b>：首次成功落 SUCCESS+快照，同键重放返回原结果且业务不重执行；</li>
 *     <li><b>重启不重复写</b>：以「全新 store/切面/目标实例（状态只在 DB）」模拟新进程重放（真实跨进程 SQL 证据见
 *         scripts/db/run-sec011b-verify.mjs）；</li>
 *     <li><b>并发→只放行一个</b>：同键并发业务恰执行一次（DB 唯一约束兜底；败者要么 900 拒绝、要么复用已提交结果，
 *         二者都是「只放行一个」的合同形态）；</li>
 *     <li><b>fail-closed</b>：无事务调用（MANDATORY）抛 IllegalTransactionStateException，不静默自提交、零残留。</li>
 * </ul>
 * 授权重检（方法安全次序合同）见 {@link PersistentIdempotentAuthorizationTest}。
 *
 * <p>用法合同（与注解 Javadoc 一致）：@Idempotent(persistent=true) 落在内层执行器方法，
 * 事务边界在外层 @Transactional 入口——切面经 MANDATORY 参与该事务，INSERT RUNNING → 业务 → markSuccess
 * 与业务同生共死。
 */
@Import({JdbcPersistentIdempotentStore.class, PersistentIdempotentIntegrationTest.IdempotentFixtureConfiguration.class})
public class PersistentIdempotentIntegrationTest extends BaseDbUnitTest {

    @Resource
    private OrderEntry orderEntry;

    @Resource
    private OrderExecutor orderExecutor;

    @Resource
    private PersistentIdempotentStore store;

    @Resource
    private DataSource dataSource;

    @Resource
    private PlatformTransactionManager transactionManager;

    @org.junit.jupiter.api.BeforeEach
    void resetFixture() {
        orderExecutor.reset();
    }

    // ========== 丢响应 → 可重放恢复原结果 ==========

    @Test
    void lostResponse_replayReturnsOriginalResult_withoutReexecution() {
        try (SubjectContext ctx = subject(1L, 100L, 2)) {
            String first = orderEntry.create("A");
            assertEquals("result-A", first);
            assertEquals(1, orderExecutor.executions(), "首次请求业务执行一次");

            // 模拟「响应丢失后客户端重试同键」：应复用 SUCCESS 快照，业务不重执行
            String replay = orderEntry.create("A");
            assertEquals("result-A", replay, "重放应返回原结果（丢响应恢复）");
            assertEquals(1, orderExecutor.executions(), "重放不得重执行业务（不重复写）");
        }

        Optional<PersistentIdempotentRecord> found = onlyRow();
        assertTrue(found.isPresent(), "应有且仅有一条持久化记录");
        PersistentIdempotentRecord record = found.get();
        assertEquals(PersistentIdempotentStatus.SUCCESS, record.getStatus());
        assertEquals("\"result-A\"", record.getResultSnapshot(), "快照应为返回值的 JSON 形态");
        assertEquals(expectedDigest("A"), record.getRequestDigest(), "摘要应为未截断脱敏表示的 MD5（P2-1 口径）");
        assertEquals("100", record.getSubjectId());
    }

    // ========== 重启不重复写（新进程模拟：全新实例，状态只在 DB） ==========

    @Test
    void restart_newInstances_replayFromDbWithoutReexecution() {
        try (SubjectContext ctx = subject(1L, 100L, 2)) {
            assertEquals("result-A", orderEntry.create("A"));
        }
        assertEquals(1, countRows());

        // 新「进程」：全新 store/切面/目标实例——与旧实例零共享内存状态，只共享持久层
        JdbcPersistentIdempotentStore restartedStore = new JdbcPersistentIdempotentStore(dataSource, transactionManager);
        IdempotentAspect restartedAspect = new IdempotentAspect(
                List.of(new DefaultIdempotentKeyResolver()),
                new IdempotentRedisDAO(new StringRedisTemplate()),
                providerOf(restartedStore));
        OrderExecutor freshExecutor = new OrderExecutor();
        AspectJProxyFactory factory = new AspectJProxyFactory(freshExecutor);
        factory.addAspect(restartedAspect);
        OrderExecutor restartedProxy = factory.getProxy();

        try (SubjectContext ctx = subject(1L, 100L, 2)) {
            String replay = restartedProxy.execute("A");
            assertEquals("result-A", replay, "重启后重放应命中持久化记录复用原结果");
            assertEquals(0, freshExecutor.executions(), "重启后不得重执行业务（状态只在 DB）");
        }
        assertEquals(1, orderExecutor.executions(), "旧实例计数不变");
    }

    // ========== 业务回滚 → 无残留记录，可安全重试 ==========

    @Test
    void businessRollback_leavesNoRecord_andRetryExecutesFresh() {
        orderExecutor.failNext();
        try (SubjectContext ctx = subject(1L, 100L, 2)) {
            assertThrows(IllegalStateException.class, () -> orderEntry.create("B"));
            assertEquals(0, countRows(), "业务回滚不得残留记录（与业务同事务回滚）");

            String retried = orderEntry.create("B");
            assertEquals("result-B", retried, "失败后同键重试应正常执行");
            assertEquals(1, orderExecutor.executions(), "重试是首次成功执行（此前失败未计数）");
        }
    }

    // ========== 并发 → 同键并发只放行一个（DB 唯一约束兜底） ==========

    @Test
    void concurrentSameKey_businessExecutesExactlyOnce() throws Exception {
        int threads = 4;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch start = new CountDownLatch(1);
            AtomicInteger successes = new AtomicInteger();
            AtomicInteger rejected = new AtomicInteger();
            List<Future<String>> futures = IntStream.range(0, threads).mapToObj(i -> pool.submit(() -> {
                start.await();
                try (SubjectContext ctx = subject(1L, 100L, 2)) {
                    return orderEntry.create("C");
                } catch (ServiceException e) {
                    if (!GlobalErrorCodeConstants.REPEATED_REQUESTS.getCode().equals(e.getCode())) {
                        throw new AssertionError("并发拒绝只应是 900，实际 code=" + e.getCode(), e);
                    }
                    rejected.incrementAndGet();
                    return null;
                }
            })).collect(Collectors.toList());
            start.countDown();
            for (Future<String> future : futures) {
                String result = future.get(15, TimeUnit.SECONDS);
                if ("result-C".equals(result)) {
                    successes.incrementAndGet();
                }
            }
            assertEquals(threads, successes.get() + rejected.get(), "每个并发响应必为「原结果」或「900 拒绝」二选一");
            assertEquals(1, orderExecutor.executions(), "同键并发业务只执行一次（不重复写，DB 唯一约束兜底）");
            assertTrue(successes.get() >= 1, "胜者应拿到原结果");
            assertEquals(1, countRows(), "只留一条记录");
        } finally {
            pool.shutdownNow();
        }
    }

    // ========== fail-closed：无事务调用（MANDATORY） ==========

    @Test
    void callWithoutTransaction_failsClosed() {
        // 直接调内层执行器（绕过 @Transactional 入口）：tryInsertRunning 必须 MANDATORY 拒绝，不静默自提交
        JdbcPersistentIdempotentStore directStore = new JdbcPersistentIdempotentStore(dataSource, transactionManager);
        IdempotentAspect aspect = new IdempotentAspect(
                List.of(new DefaultIdempotentKeyResolver()),
                new IdempotentRedisDAO(new StringRedisTemplate()),
                providerOf(directStore));
        OrderExecutor executor = new OrderExecutor();
        AspectJProxyFactory factory = new AspectJProxyFactory(executor);
        factory.addAspect(aspect);
        OrderExecutor proxied = factory.getProxy();

        try (SubjectContext ctx = subject(1L, 100L, 2)) {
            assertThrows(IllegalTransactionStateException.class,
                    () -> proxied.execute("NO-TX"));
        }
        assertEquals(0, executor.executions(), "无事务调用不得执行业务");
        assertEquals(0, countRows(), "无事务调用不得落任何记录（无 autocommit 残留）");
    }

    // ========== 状态级复用：快照缺失的重放拒绝而非重执行 ==========

    @Test
    void replayWithoutSnapshot_degradesToStatusLevelRejection() {
        try (SubjectContext ctx = subject(1L, 100L, 2)) {
            assertEquals("result-A", orderEntry.create("A"));
        }
        // 模拟快照不可用的记录（序列化失败时 markSuccess(null) 的形态）：本测试仅此一条记录
        new JdbcTemplate(dataSource).update("UPDATE infra_persistent_idempotent SET result_snapshot = NULL");
        try (SubjectContext ctx = subject(1L, 100L, 2)) {
            ServiceException ex = assertThrows(ServiceException.class, () -> orderEntry.create("A"));
            assertEquals(GlobalErrorCodeConstants.REPEATED_REQUESTS.getCode(), ex.getCode(), "快照缺失应状态级拒绝");
        }
        assertEquals(1, orderExecutor.executions(), "状态级复用不得重执行业务");
    }

    // ========== Fixture ==========

    @Configuration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy
    static class IdempotentFixtureConfiguration {

        @Bean
        public DefaultIdempotentKeyResolver defaultIdempotentKeyResolver() {
            return new DefaultIdempotentKeyResolver();
        }

        @Bean
        public IdempotentAspect idempotentAspect(List<IdempotentKeyResolver> keyResolvers,
                                                 ObjectProvider<PersistentIdempotentStore> persistentStoreProvider) {
            // 持久化路径不触碰 Redis：StringRedisTemplate 仅满足构造签名（无连接、不初始化）
            return new IdempotentAspect(keyResolvers, new IdempotentRedisDAO(new StringRedisTemplate()), persistentStoreProvider);
        }

        @Bean
        public OrderExecutor orderExecutor() {
            return new OrderExecutor();
        }

        @Bean
        public OrderEntry orderEntry(OrderExecutor orderExecutor) {
            return new OrderEntry(orderExecutor);
        }
    }

    /** 业务执行器：@Idempotent(persistent=true) 落在内层方法，事务边界在外层入口（MANDATORY 参与的用法合同） */
    static class OrderExecutor {

        private final AtomicInteger executions = new AtomicInteger();
        private final AtomicBoolean failOnce = new AtomicBoolean(false);

        @Idempotent(persistent = true)
        public String execute(String req) {
            if (failOnce.compareAndSet(true, false)) {
                throw new IllegalStateException("boom");
            }
            executions.incrementAndGet();
            return "result-" + req;
        }

        int executions() {
            return executions.get();
        }

        void failNext() {
            failOnce.set(true);
        }

        /** 单例 bean 的计数跨测试方法共享上下文，须在每个用例前归零（DB 由 clean.sql 清理，内存态须对称清理） */
        void reset() {
            executions.set(0);
            failOnce.set(false);
        }
    }

    /** 事务入口：INSERT RUNNING → 业务 → markSuccess 在本方法事务内原子提交（切面 MANDATORY 参与） */
    static class OrderEntry {

        private final OrderExecutor executor;

        OrderEntry(OrderExecutor executor) {
            this.executor = executor;
        }

        @Transactional
        public String create(String req) {
            return executor.execute(req);
        }
    }

    // ========== Helpers ==========

    /** 与切面同口径的期望摘要（未截断脱敏表示的 MD5，P2-1 口径） */
    private static String expectedDigest(String req) {
        return SecureUtil.md5(LogSanitizeUtils.sanitizeArgsUntruncated(new Object[]{req}));
    }

    /** 本测试唯一的持久化记录行（clean.sql 保证用例间隔离） */
    private Optional<PersistentIdempotentRecord> onlyRow() {
        String key = new JdbcTemplate(dataSource).queryForObject(
                "SELECT idempotent_key FROM infra_persistent_idempotent", String.class);
        return store.findByIdempotentKey(key);
    }

    private int countRows() {
        Integer count = new JdbcTemplate(dataSource).queryForObject(
                "SELECT COUNT(*) FROM infra_persistent_idempotent", Integer.class);
        return count == null ? 0 : count;
    }

    static ObjectProvider<PersistentIdempotentStore> providerOf(PersistentIdempotentStore store) {
        return new ObjectProvider<>() {
            @Override
            public PersistentIdempotentStore getObject() {
                return store;
            }

            @Override
            public PersistentIdempotentStore getIfAvailable() {
                return store;
            }
        };
    }

    /** 切面/解析器的主体上下文（ServletUtils.getRequest() + WebFrameworkUtils 权威 attribute 口径） */
    record SubjectContext(MockedStatic<ServletUtils> servlet, MockedStatic<WebFrameworkUtils> web) implements AutoCloseable {

        static SubjectContext of(Long tenantId, Long userId, Integer userType) {
            MockedStatic<ServletUtils> servletMs = mockStatic(ServletUtils.class);
            MockedStatic<WebFrameworkUtils> webMs = mockStatic(WebFrameworkUtils.class);
            HttpServletRequest request = mock(HttpServletRequest.class);
            servletMs.when(ServletUtils::getRequest).thenReturn(request);
            webMs.when(() -> WebFrameworkUtils.getTenantId(request)).thenReturn(tenantId);
            webMs.when(() -> WebFrameworkUtils.getLoginUserId(request)).thenReturn(userId);
            webMs.when(() -> WebFrameworkUtils.getLoginUserType(request)).thenReturn(userType);
            return new SubjectContext(servletMs, webMs);
        }

        @Override
        public void close() {
            try {
                servlet.close();
            } catch (Throwable ignore) {
                // 静默：测试夹具关闭
            }
            try {
                web.close();
            } catch (Throwable ignore) {
                // 静默：测试夹具关闭
            }
        }
    }

    private SubjectContext subject(Long tenantId, Long userId, Integer userType) {
        return SubjectContext.of(tenantId, userId, userType);
    }

}
