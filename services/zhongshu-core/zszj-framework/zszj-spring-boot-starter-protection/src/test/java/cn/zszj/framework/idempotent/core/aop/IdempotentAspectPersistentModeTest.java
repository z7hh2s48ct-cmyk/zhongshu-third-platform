package cn.zszj.framework.idempotent.core.aop;

import cn.hutool.crypto.SecureUtil;
import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.zszj.framework.common.util.log.LogSanitizeUtils;
import cn.zszj.framework.common.util.servlet.ServletUtils;
import cn.zszj.framework.idempotent.core.annotation.Idempotent;
import cn.zszj.framework.idempotent.core.keyresolver.IdempotentKeyResolver;
import cn.zszj.framework.idempotent.core.redis.IdempotentRedisDAO;
import cn.zszj.framework.idempotent.core.persistent.PersistentIdempotentRecord;
import cn.zszj.framework.idempotent.core.persistent.PersistentIdempotentStatus;
import cn.zszj.framework.idempotent.core.persistent.PersistentIdempotentStore;
import cn.zszj.framework.web.core.util.WebFrameworkUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * {@link IdempotentAspect} 持久化幂等模式的单元测试（ZS-SEC-011.B，mock store 合同层）。
 *
 * 覆盖退出条件的切面合同层证据：
 * <ul>
 *     <li><b>丢响应→可重放恢复原结果</b>：SUCCESS + 快照 → 返回原结果且业务不重执行；快照缺失 → 状态级复用（900）；</li>
 *     <li><b>并发→同键只放行一个</b>：store 唯一语义（putIfAbsent 模拟 DB 唯一约束）下并发同键恰 1 次业务执行；</li>
 *     <li><b>不重复写</b>：RUNNING/FAILED 重放一律拒绝，业务不重执行；失败按 deleteKeyWhenException 删记录/留 FAILED；</li>
 *     <li><b>重试重检授权（主体隔离半边）</b>：主体因子强制进 storeKey，不同主体同业务键不共用记录；
 *         方法安全先于幂等切面的次序合同见 infra 的 PersistentIdempotentAuthorizationTest；</li>
 *     <li><b>fail-closed</b>：无 store bean、匿名主体（userId=null）均拒绝，不静默降级放行。</li>
 * </ul>
 * 真实 DB（H2/PG）语义证据见 zszj-module-infra 的 JdbcPersistentIdempotentStoreTest /
 * PersistentIdempotentIntegrationTest 与 scripts/db/run-sec011b-verify.mjs。
 */
@ExtendWith(MockitoExtension.class)
public class IdempotentAspectPersistentModeTest {

    private static final String METHOD_DESC = "OrderService.createOrder(..)";

    /** 可并发复用的业务执行计数器（每个用例自建目标对象） */
    static class CountingService {
        final AtomicInteger executions = new AtomicInteger();

        public String execute(String req) {
            executions.incrementAndGet();
            return "result-" + req;
        }
    }

    @BeforeEach
    public void setUp() {
    }

    @AfterEach
    public void tearDown() {
    }

    // ========== 首次执行与成功快照 ==========

    @Test
    public void firstRequest_executesBusiness_marksSuccessWithSnapshot() throws Throwable {
        MockPersistentStore store = new MockPersistentStore();
        IdempotentAspect aspect = newAspect(store, new FixedKeyResolver());
        CountingService service = new CountingService();

        try (MockedStaticContext ctx = mockSubject(1L, 100L, 2)) {
            Object result = aspect.aroundPointCut(joinPointOf(new Object[]{"A"}, service), persistentIdempotent(true, true));
            assertEquals("result-A", result);
            assertEquals(1, service.executions.get(), "首次请求业务应执行一次");
        }

        // 断言 store 交互：RUNNING 记录（摘要 = 未截断脱敏 MD5）+ markSuccess 携 JSON 快照
        String expectedDigest = SecureUtil.md5(LogSanitizeUtils.sanitizeArgsUntruncated(new Object[]{"A"}));
        assertEquals(1, store.insertAttempts.size(), "应恰好一次抢锁插入");
        assertEquals(expectedDigest, store.insertAttempts.get(0).getRequestDigest(), "请求摘要应为未截断脱敏表示的 MD5");
        assertEquals(PersistentIdempotentStatus.RUNNING, store.insertAttempts.get(0).getStatus());
        assertEquals("\"result-A\"", store.markedSuccessSnapshot, "成功应写结果快照（JSON 字符串字面量）");
    }

    @Test
    public void replaySuccessWithSnapshot_returnsOriginalResult_withoutBusinessExecution() throws Throwable {
        MockPersistentStore store = new MockPersistentStore();
        IdempotentAspect aspect = newAspect(store, new FixedKeyResolver());
        CountingService service = new CountingService();

        // 预置：上一次成功记录（同主体同键同摘要 + 快照）——模拟「响应丢失后重放」/「重启后重放」
        String digest = SecureUtil.md5(LogSanitizeUtils.sanitizeArgsUntruncated(new Object[]{"A"}));
        String storeKey = storeKeyOf(new FixedKeyResolver().resolver(null, null), 1L, 100L, 2);
        PersistentIdempotentRecord record = new PersistentIdempotentRecord();
        record.setIdempotentKey(storeKey);
        record.setRequestDigest(digest);
        record.setStatus(PersistentIdempotentStatus.SUCCESS);
        record.setResultSnapshot("\"result-A\"");
        store.rows.put(storeKey, record);

        try (MockedStaticContext ctx = mockSubject(1L, 100L, 2)) {
            Object result = aspect.aroundPointCut(joinPointOf(new Object[]{"A"}, service), persistentIdempotent(true, true));
            assertEquals("result-A", result, "重放应复用原结果（丢响应恢复原结果）");
            assertEquals(0, service.executions.get(), "重放不得重执行业务（不重复写）");
        }
        verifyStoreUntouchedForWrite(store);
    }

    @Test
    public void replaySuccessWithoutSnapshot_throwsStatusLevelRepeated() throws Throwable {
        MockPersistentStore store = new MockPersistentStore();
        IdempotentAspect aspect = newAspect(store, new FixedKeyResolver());
        CountingService service = new CountingService();

        String digest = SecureUtil.md5(LogSanitizeUtils.sanitizeArgsUntruncated(new Object[]{"A"}));
        String storeKey = storeKeyOf(new FixedKeyResolver().resolver(null, null), 1L, 100L, 2);
        PersistentIdempotentRecord record = new PersistentIdempotentRecord();
        record.setIdempotentKey(storeKey);
        record.setRequestDigest(digest);
        record.setStatus(PersistentIdempotentStatus.SUCCESS);
        record.setResultSnapshot(null); // 快照缺失：序列化失败时的降级形态
        store.rows.put(storeKey, record);

        try (MockedStaticContext ctx = mockSubject(1L, 100L, 2)) {
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> aspect.aroundPointCut(joinPointOf(new Object[]{"A"}, service), persistentIdempotent(true, true)));
            assertEquals(GlobalErrorCodeConstants.REPEATED_REQUESTS.getCode(), ex.getCode(), "状态级复用应为 900");
        }
        assertEquals(0, service.executions.get(), "状态级复用同样不得重执行业务");
        verifyStoreUntouchedForWrite(store);
    }

    @Test
    public void replayCorruptedSnapshot_degradesToStatusLevelRepeated() throws Throwable {
        MockPersistentStore store = new MockPersistentStore();
        IdempotentAspect aspect = newAspect(store, new FixedKeyResolver());
        CountingService service = new CountingService();

        String digest = SecureUtil.md5(LogSanitizeUtils.sanitizeArgsUntruncated(new Object[]{"A"}));
        String storeKey = storeKeyOf(new FixedKeyResolver().resolver(null, null), 1L, 100L, 2);
        PersistentIdempotentRecord record = new PersistentIdempotentRecord();
        record.setIdempotentKey(storeKey);
        record.setRequestDigest(digest);
        record.setStatus(PersistentIdempotentStatus.SUCCESS);
        record.setResultSnapshot("{corrupted-json"); // 快照损坏：绝不重执行业务，降级 900
        store.rows.put(storeKey, record);

        try (MockedStaticContext ctx = mockSubject(1L, 100L, 2)) {
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> aspect.aroundPointCut(joinPointOf(new Object[]{"A"}, service), persistentIdempotent(true, true)));
            assertEquals(GlobalErrorCodeConstants.REPEATED_REQUESTS.getCode(), ex.getCode());
        }
        assertEquals(0, service.executions.get(), "快照损坏只可降级拒绝，不得重执行业务");
    }

    // ========== 不重复写：RUNNING / FAILED / 冲突 ==========

    @Test
    public void replayRunning_throwsRepeated_withoutBusinessExecution() throws Throwable {
        MockPersistentStore store = new MockPersistentStore();
        IdempotentAspect aspect = newAspect(store, new FixedKeyResolver());
        CountingService service = new CountingService();

        String digest = SecureUtil.md5(LogSanitizeUtils.sanitizeArgsUntruncated(new Object[]{"A"}));
        String storeKey = storeKeyOf(new FixedKeyResolver().resolver(null, null), 1L, 100L, 2);
        PersistentIdempotentRecord record = new PersistentIdempotentRecord();
        record.setIdempotentKey(storeKey);
        record.setRequestDigest(digest);
        record.setStatus(PersistentIdempotentStatus.RUNNING);
        store.rows.put(storeKey, record);

        try (MockedStaticContext ctx = mockSubject(1L, 100L, 2)) {
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> aspect.aroundPointCut(joinPointOf(new Object[]{"A"}, service), persistentIdempotent(true, true)));
            assertEquals(GlobalErrorCodeConstants.REPEATED_REQUESTS.getCode(), ex.getCode());
        }
        assertEquals(0, service.executions.get(), "RUNNING 重放不得重执行业务（并发不重复写）");
    }

    @Test
    public void replayFailed_throwsRepeated_withoutBusinessExecution() throws Throwable {
        MockPersistentStore store = new MockPersistentStore();
        IdempotentAspect aspect = newAspect(store, new FixedKeyResolver());
        CountingService service = new CountingService();

        String digest = SecureUtil.md5(LogSanitizeUtils.sanitizeArgsUntruncated(new Object[]{"A"}));
        String storeKey = storeKeyOf(new FixedKeyResolver().resolver(null, null), 1L, 100L, 2);
        PersistentIdempotentRecord record = new PersistentIdempotentRecord();
        record.setIdempotentKey(storeKey);
        record.setRequestDigest(digest);
        record.setStatus(PersistentIdempotentStatus.FAILED); // deleteKeyWhenException=false 保留的失败记录
        store.rows.put(storeKey, record);

        try (MockedStaticContext ctx = mockSubject(1L, 100L, 2)) {
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> aspect.aroundPointCut(joinPointOf(new Object[]{"A"}, service), persistentIdempotent(true, true)));
            assertEquals(GlobalErrorCodeConstants.REPEATED_REQUESTS.getCode(), ex.getCode());
        }
        assertEquals(0, service.executions.get(), "FAILED（保留键）重放不得重执行业务");
    }

    @Test
    public void replayDifferentDigest_throwsConflict() throws Throwable {
        MockPersistentStore store = new MockPersistentStore();
        IdempotentAspect aspect = newAspect(store, new FixedKeyResolver());
        CountingService service = new CountingService();

        String otherDigest = SecureUtil.md5(LogSanitizeUtils.sanitizeArgsUntruncated(new Object[]{"OTHER-PAYLOAD"}));
        String storeKey = storeKeyOf(new FixedKeyResolver().resolver(null, null), 1L, 100L, 2);
        PersistentIdempotentRecord record = new PersistentIdempotentRecord();
        record.setIdempotentKey(storeKey);
        record.setRequestDigest(otherDigest);
        record.setStatus(PersistentIdempotentStatus.SUCCESS);
        store.rows.put(storeKey, record);

        try (MockedStaticContext ctx = mockSubject(1L, 100L, 2)) {
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> aspect.aroundPointCut(joinPointOf(new Object[]{"A"}, service), persistentIdempotent(true, true)));
            assertEquals(GlobalErrorCodeConstants.REPEATED_REQUESTS.getCode(), ex.getCode());
            assertTrue(ex.getMessage().contains("幂等键冲突"),
                    "同键异参重放应为冲突文案，实际: " + ex.getMessage());
        }
        assertEquals(0, service.executions.get());
    }

    // ========== 失败路径：删记录 / 留 FAILED ==========

    @Test
    public void businessFailure_deleteKeyWhenException_deletesRecordAndRethrows() throws Throwable {
        MockPersistentStore store = new MockPersistentStore();
        IdempotentAspect aspect = newAspect(store, new FixedKeyResolver());
        FailingService service = new FailingService();

        try (MockedStaticContext ctx = mockSubject(1L, 100L, 2)) {
            assertThrows(IllegalStateException.class,
                    () -> aspect.aroundPointCut(joinPointOfFailure(new Object[]{"A"}, service), persistentIdempotent(true, true)));
        }
        assertTrue(store.deletedRunning, "deleteKeyWhenException=true 失败应删 RUNNING 记录（客户端可重试）");
        assertFalse(store.markFailedCalled, "删除路径不应再 markFailed");
    }

    @Test
    public void businessFailure_keepKey_marksFailedAndRethrows() throws Throwable {
        MockPersistentStore store = new MockPersistentStore();
        IdempotentAspect aspect = newAspect(store, new FixedKeyResolver());
        FailingService service = new FailingService();

        try (MockedStaticContext ctx = mockSubject(1L, 100L, 2)) {
            assertThrows(IllegalStateException.class,
                    () -> aspect.aroundPointCut(joinPointOfFailure(new Object[]{"A"}, service), persistentIdempotent(true, false)));
        }
        assertTrue(store.markFailedCalled, "deleteKeyWhenException=false 失败应保留记录置 FAILED");
        assertFalse(store.deletedRunning, "保留路径不应删记录");
    }

    static class FailingService {
        public String execute(String req) {
            throw new IllegalStateException("boom");
        }
    }

    // ========== 快照序列化失败：业务不因快照失败而失败 ==========

    @Test
    public void snapshotSerializationFailure_businessStillSucceeds_marksSuccessWithoutSnapshot() throws Throwable {
        MockPersistentStore store = new MockPersistentStore();
        IdempotentAspect aspect = newAspect(store, new FixedKeyResolver());

        MethodSignature signature = methodSignature(EvilResult.class);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"A"});
        when(joinPoint.proceed()).thenReturn(new EvilResult());

        try (MockedStaticContext ctx = mockSubject(1L, 100L, 2)) {
            Object result = aspect.aroundPointCut(joinPoint, persistentIdempotent(true, true));
            assertNotNull(result, "业务结果应正常返回（快照失败不连坐业务）");
        }
        assertTrue(store.markedSuccessCalled, "业务成功仍应 markSuccess");
        assertEquals(null, store.markedSuccessSnapshot, "无法快照时以 null 快照 markSuccess（重放退化为状态级复用）");
    }

    /** getter 抛异常的对象：Jackson 序列化必失败（快照降级路径的确定性驱动） */
    static class EvilResult {
        public String getEvil() {
            throw new IllegalStateException("serialize-me-not");
        }
    }

    // ========== fail-closed：无 store / 匿名主体 ==========

    @Test
    public void persistentWithoutStoreBean_throwsIllegalState() {
        IdempotentAspect aspect = newAspect(null, new FixedKeyResolver());
        CountingService service = new CountingService();

        try (MockedStaticContext ctx = mockSubject(1L, 100L, 2)) {
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> aspect.aroundPointCut(joinPointOf(new Object[]{"A"}, service), persistentIdempotent(true, true)));
            assertTrue(ex.getMessage().contains("PersistentIdempotentStore"),
                    "缺 store bean 应 fail-fast 并指明缺失组件，实际: " + ex.getMessage());
        }
        assertEquals(0, service.executions.get());
    }

    @Test
    public void persistentWithAnonymousSubject_failClosed() throws Throwable {
        MockPersistentStore store = new MockPersistentStore();
        IdempotentAspect aspect = newAspect(store, new FixedKeyResolver());
        CountingService service = new CountingService();

        // 匿名主体（userId=null）：持久化记录带结果快照，匿名塌缩即跨主体结果重放风险 → fail-closed
        try (MockedStaticContext ctx = mockSubject(1L, null, null)) {
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> aspect.aroundPointCut(joinPointOf(new Object[]{"A"}, service), persistentIdempotent(true, true)));
            assertEquals(GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR.getCode(), ex.getCode(),
                    "匿名主体应 fail-closed 拒绝（使用错误，500 语义）");
        }
        assertEquals(0, service.executions.get(), "匿名主体不得触发业务");
        assertTrue(store.insertAttempts.isEmpty(), "匿名主体不得触碰持久化存储（堵 .A collapse 危险语义）");
    }

    @Test
    public void persistentFalse_keepsRedisWindowPath() throws Throwable {
        // 非 persistent 注解不得触碰持久化存储（Redis 窗口路径行为不变）
        MockPersistentStore store = new MockPersistentStore();
        CountingService service = new CountingService();

        IdempotentRedisDAO redisDAO = mock(IdempotentRedisDAO.class);
        when(redisDAO.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);
        IdempotentAspect aspect = newAspect(store, new FixedKeyResolver(), redisDAO);

        try (MockedStaticContext ctx = mockSubject(1L, 100L, 2)) {
            Object result = aspect.aroundPointCut(joinPointOf(new Object[]{"A"}, service), persistentIdempotent(false, true));
            assertEquals("result-A", result);
        }
        assertTrue(store.insertAttempts.isEmpty(), "persistent=false 不得触碰持久化存储");
        assertEquals(1, service.executions.get());
    }

    // ========== 主体隔离：主体因子强制进 storeKey ==========

    @Test
    public void differentSubject_sameBusinessKey_differentStoreKey() throws Throwable {
        MockPersistentStore store = new MockPersistentStore();
        IdempotentAspect aspect = newAspect(store, new FixedKeyResolver());
        CountingService service = new CountingService();

        try (MockedStaticContext ctx = mockSubject(1L, 100L, 2)) {
            aspect.aroundPointCut(joinPointOf(new Object[]{"A"}, service), persistentIdempotent(true, true));
        }
        try (MockedStaticContext ctx = mockSubject(1L, 200L, 2)) {
            // 同业务键（FixedKeyResolver 恒定返回），不同主体 → storeKey 必须不同 → 各自执行，不误用他人结果
            Object result = aspect.aroundPointCut(joinPointOf(new Object[]{"A"}, service), persistentIdempotent(true, true));
            assertEquals("result-A", result);
        }
        assertEquals(2, service.executions.get(), "不同主体不得复用他人记录，应各自执行");
        assertEquals(2, store.insertAttempts.size());
        assertNotEquals(store.insertAttempts.get(0).getIdempotentKey(), store.insertAttempts.get(1).getIdempotentKey(),
                "主体因子必须参与 storeKey 派生（不同主体 → 不同键）");
    }

    // ========== 并发：同键并发只放行一个（store 唯一语义模拟 DB 唯一约束兜底） ==========

    @Test
    public void concurrentSameKey_onlyOneProceeds() throws Throwable {
        MockPersistentStore store = new MockPersistentStore();
        IdempotentAspect aspect = newAspect(store, new FixedKeyResolver());

        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch start = new CountDownLatch(1);
            CountingService service = new CountingService();
            List<Future<Object>> futures = IntStream.range(0, threads).mapToObj(i -> pool.submit(() -> {
                start.await();
                try (MockedStaticContext ctx = mockSubject(1L, 100L, 2)) {
                    try {
                        return aspect.aroundPointCut(joinPointOfSlow(new Object[]{"A"}, service, 50), persistentIdempotent(true, true));
                    } catch (ServiceException e) {
                        throw e; // 保持拒绝类型（Future.get 包装为 ExecutionException）
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                }
            })).collect(Collectors.toList());
            start.countDown();
            int success = 0, rejected = 0;
            for (Future<Object> future : futures) {
                try {
                    future.get(10, TimeUnit.SECONDS);
                    success++;
                } catch (java.util.concurrent.ExecutionException e) {
                    if (e.getCause() instanceof ServiceException
                            && GlobalErrorCodeConstants.REPEATED_REQUESTS.getCode().equals(((ServiceException) e.getCause()).getCode())) {
                        rejected++;
                    } else {
                        throw e;
                    }
                }
            }
            assertEquals(1, success, "同键并发只放行一个");
            assertEquals(threads - 1, rejected, "其余应被 900 拒绝");
            assertEquals(1, service.executions.get(), "业务只执行一次（不重复写）");
            assertEquals(1, store.rows.size(), "只留一条记录");
        } finally {
            pool.shutdownNow();
        }
    }

    // ========== Fixtures ==========

    record MockedStaticContext(AutoCloseable servlet, AutoCloseable web) implements AutoCloseable {
        @Override
        public void close() {
            try {
                servlet.close();
            } catch (Exception ignore) {
                // 静默：测试夹具关闭
            }
            try {
                web.close();
            } catch (Exception ignore) {
                // 静默：测试夹具关闭
            }
        }
    }

    private MockedStaticContext mockSubject(Long tenantId, Long userId, Integer userType) {
        MockedStatic<ServletUtils> servletMs = mockStatic(ServletUtils.class);
        MockedStatic<WebFrameworkUtils> webMs = mockStatic(WebFrameworkUtils.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        servletMs.when(ServletUtils::getRequest).thenReturn(request);
        webMs.when(() -> WebFrameworkUtils.getTenantId(request)).thenReturn(tenantId);
        webMs.when(() -> WebFrameworkUtils.getLoginUserId(request)).thenReturn(userId);
        webMs.when(() -> WebFrameworkUtils.getLoginUserType(request)).thenReturn(userType);
        return new MockedStaticContext(servletMs, webMs);
    }

    private IdempotentAspect newAspect(PersistentIdempotentStore store, IdempotentKeyResolver resolver) {
        return newAspect(store, resolver, mock(IdempotentRedisDAO.class));
    }

    private IdempotentAspect newAspect(PersistentIdempotentStore store, IdempotentKeyResolver resolver, IdempotentRedisDAO redisDAO) {
        return new IdempotentAspect(List.of(resolver), redisDAO, providerOf(store));
    }

    static ObjectProvider<PersistentIdempotentStore> providerOf(PersistentIdempotentStore store) {
        return new ObjectProvider<>() {
            @Override
            public PersistentIdempotentStore getObject() {
                if (store == null) {
                    throw new IllegalStateException("no store");
                }
                return store;
            }

            @Override
            public PersistentIdempotentStore getIfAvailable() {
                return store;
            }
        };
    }

    private Idempotent persistentIdempotent(boolean persistent, boolean deleteKeyWhenException) {
        Idempotent idempotent = mock(Idempotent.class);
        doReturn(FixedKeyResolver.class).when(idempotent).keyResolver();
        doReturn(persistent).when(idempotent).persistent();
        // persistent 路径不读 deleteKeyWhenException（拒绝路径）/timeout/timeUnit/message（部分用例），lenient 避免 STRICT_STUBS 误报
        lenient().doReturn(deleteKeyWhenException).when(idempotent).deleteKeyWhenException();
        lenient().when(idempotent.message()).thenReturn("repeated-request");
        lenient().when(idempotent.timeout()).thenReturn(1);
        lenient().when(idempotent.timeUnit()).thenReturn(TimeUnit.SECONDS);
        return idempotent;
    }

    private ProceedingJoinPoint joinPointOf(Object[] args, CountingService service) throws Throwable {
        // 注意：helper 内 stub 全部 lenient——拒绝/复用路径不会消费 proceed 等调用，避免 STRICT_STUBS 误报
        MethodSignature signature = methodSignature(CountingService.class);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        lenient().when(joinPoint.getSignature()).thenReturn(signature);
        lenient().when(joinPoint.getArgs()).thenReturn(args);
        lenient().when(joinPoint.proceed()).thenAnswer(invocation -> service.execute((String) args[0]));
        return joinPoint;
    }

    private ProceedingJoinPoint joinPointOfFailure(Object[] args, FailingService service) throws Throwable {
        MethodSignature signature = methodSignature(FailingService.class);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        lenient().when(joinPoint.getSignature()).thenReturn(signature);
        lenient().when(joinPoint.getArgs()).thenReturn(args);
        lenient().when(joinPoint.proceed()).thenAnswer(invocation -> service.execute((String) args[0]));
        return joinPoint;
    }

    private ProceedingJoinPoint joinPointOfSlow(Object[] args, CountingService service, long sleepMs) throws Throwable {
        MethodSignature signature = methodSignature(CountingService.class);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        lenient().when(joinPoint.getSignature()).thenReturn(signature);
        lenient().when(joinPoint.getArgs()).thenReturn(args);
        lenient().when(joinPoint.proceed()).thenAnswer(invocation -> {
            Thread.sleep(sleepMs);
            return service.execute((String) args[0]);
        });
        return joinPoint;
    }

    /** 持久化路径需要真实 Method（反序列化快照按声明返回类型），故用 MethodSignature mock */
    static MethodSignature methodSignature(Class<?> declaringClass) {
        Method method = null;
        for (Method candidate : declaringClass.getMethods()) {
            if ("execute".equals(candidate.getName())) {
                method = candidate;
                break;
            }
        }
        MethodSignature signature = mock(MethodSignature.class);
        lenient().when(signature.getMethod()).thenReturn(method);
        lenient().when(signature.toString()).thenReturn(METHOD_DESC);
        return signature;
    }

    static String storeKeyOf(String resolvedKey, Long tenantId, Long userId, Integer userType) {
        return SecureUtil.md5(resolvedKey + ":" + tenantId + ":" + userId + ":" + userType);
    }

    private void verifyStoreUntouchedForWrite(MockPersistentStore store) {
        assertTrue(store.insertAttempts.isEmpty(), "重放路径不得再抢锁插入");
        assertTrue(!store.markedSuccessCalled, "重放路径不得 markSuccess");
        assertTrue(!store.markFailedCalled, "重放路径不得 markFailed");
        assertTrue(!store.deletedRunning, "重放路径不得删记录");
    }

    /** 模拟 DB 唯一约束语义的内存 store（putIfAbsent 原子性；真实 DB 语义见 infra/PG 证据） */
    static class MockPersistentStore implements PersistentIdempotentStore {

        final Map<String, PersistentIdempotentRecord> rows = new ConcurrentHashMap<>();
        final List<PersistentIdempotentRecord> insertAttempts = new java.util.concurrent.CopyOnWriteArrayList<>();
        volatile String markedSuccessSnapshot;
        volatile boolean markedSuccessCalled;
        volatile boolean markFailedCalled;
        volatile boolean deletedRunning;

        @Override
        public boolean tryInsertRunning(PersistentIdempotentRecord record) {
            // 记录插入时刻的快照副本（rows 中的同一引用会被 markSuccess 就地置 SUCCESS）
            PersistentIdempotentRecord attempt = new PersistentIdempotentRecord();
            attempt.setIdempotentKey(record.getIdempotentKey());
            attempt.setTenantId(record.getTenantId());
            attempt.setSubjectType(record.getSubjectType());
            attempt.setSubjectId(record.getSubjectId());
            attempt.setActionScope(record.getActionScope());
            attempt.setRequestDigest(record.getRequestDigest());
            attempt.setStatus(record.getStatus());
            insertAttempts.add(attempt);
            record.setStatus(PersistentIdempotentStatus.RUNNING);
            return rows.putIfAbsent(record.getIdempotentKey(), record) == null;
        }

        @Override
        public java.util.Optional<PersistentIdempotentRecord> findByIdempotentKey(String idempotentKey) {
            return java.util.Optional.ofNullable(rows.get(idempotentKey));
        }

        @Override
        public void markSuccess(String idempotentKey, String resultSnapshot) {
            markedSuccessCalled = true;
            markedSuccessSnapshot = resultSnapshot;
            PersistentIdempotentRecord record = rows.get(idempotentKey);
            if (record != null) {
                record.setStatus(PersistentIdempotentStatus.SUCCESS);
                record.setResultSnapshot(resultSnapshot);
            }
        }

        @Override
        public void markFailed(String idempotentKey) {
            markFailedCalled = true;
        }

        @Override
        public boolean deleteRunning(String idempotentKey) {
            deletedRunning = true;
            return rows.remove(idempotentKey) != null;
        }
    }

    /** 测试用固定 Key 解析器（返回常量业务键，主体隔离由切面派生 storeKey 承担） */
    static class FixedKeyResolver implements IdempotentKeyResolver {
        @Override
        public String resolver(JoinPoint joinPoint, Idempotent idempotent) {
            return "biz-fixed-key";
        }
    }

}
