package cn.zszj.framework.idempotent.core.aop;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.hutool.crypto.SecureUtil;
import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.zszj.framework.common.util.log.LogSanitizeUtils;
import cn.zszj.framework.common.util.servlet.ServletUtils;
import cn.zszj.framework.idempotent.core.annotation.Idempotent;
import cn.zszj.framework.idempotent.core.keyresolver.IdempotentKeyResolver;
import cn.zszj.framework.idempotent.core.keyresolver.impl.DefaultIdempotentKeyResolver;
import cn.zszj.framework.idempotent.core.redis.IdempotentRedisDAO;
import cn.zszj.framework.web.core.util.WebFrameworkUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link IdempotentAspect} 的单元测试。
 *
 * 覆盖 SEC-007：重复提交被拒绝时，方法参数必须先经 {@code LogSanitizeUtils.sanitizeArgs} 脱敏再写日志，
 * 秘密值（password/token/嵌套 apiKey/数组内 secret）不得出现在应用日志中；
 * 同时保留可定位的方法描述与非敏感字段，并抛出携带正确错误码的 ServiceException。
 *
 * ZS-SEC-011.A：同键异参冲突检测 + 同键同参重复请求检测。
 * IMP-6：摘要输入统一走脱敏口径，测试期望 digest 亦按此口径重算。
 * ZS-SEC-011.B：摘要口径升级为「未截断脱敏表示」（P2-1 修复），读回失败告警携 key 指纹（P2-2 修复）。
 *
 * ZS-SEC-011.A 合同测试边界：本测试类验证「窗口锁短时防重」合同——
 * 同键同参重复拒绝、同键异参冲突拒绝、主体/租户隔离。
 * 显式声明：窗口锁不承诺持久化幂等（重启/超时/缓存故障后不保证返回原业务结果），
 * 持久化幂等合同见 IdempotentAspectPersistentModeTest（切面层）与 zszj-module-infra 的
 * JdbcPersistentIdempotentStoreTest / PersistentIdempotentIntegrationTest（DB 层）及 PG 定向验证脚本。
 */
@ExtendWith(MockitoExtension.class)
public class IdempotentAspectTest {

    private static final String SECRET_PASSWORD = "P@ssw0rd-SECRET-DoNotLog";
    private static final String SECRET_TOKEN = "TOKEN-SECRET-DoNotLog";
    private static final String SECRET_APIKEY = "APIKEY-SECRET-DoNotLog";
    private static final String SECRET_LIST = "LIST-SECRET-DoNotLog";
    private static final String SAFE_USERNAME = "zhangsan-user";
    private static final String SAFE_NOTE = "hello-note";
    private static final String METHOD_DESC = "UserService.createOrder(..)";

    @Mock
    private IdempotentRedisDAO idempotentRedisDAO;

    private IdempotentAspect idempotentAspect;
    private ListAppender<ILoggingEvent> listAppender;
    private Logger aspectLogger;

    @BeforeEach
    public void setUp() {
        // ZS-SEC-011.B：构造器新增 ObjectProvider<PersistentIdempotentStore>（Redis 窗口路径不消费，null 即可）
        idempotentAspect = new IdempotentAspect(List.of(new FixedKeyResolver()), idempotentRedisDAO,
                IdempotentAspectPersistentModeTest.providerOf(null));
        aspectLogger = (Logger) LoggerFactory.getLogger(IdempotentAspect.class);
        aspectLogger.setLevel(Level.INFO);
        listAppender = new ListAppender<>();
        listAppender.start();
        aspectLogger.addAppender(listAppender);
    }

    @AfterEach
    public void tearDown() {
        aspectLogger.detachAppender(listAppender);
        listAppender.stop();
    }

    @Test
    public void testAroundPointCut_repeatedRequest_argsSanitizedInLog() throws Throwable {
        // 准备幂等注解：仅拒绝路径会用到的属性
        Idempotent idempotent = mock(Idempotent.class);
        doReturn(FixedKeyResolver.class).when(idempotent).keyResolver();
        when(idempotent.timeout()).thenReturn(1);
        when(idempotent.timeUnit()).thenReturn(TimeUnit.SECONDS);
        when(idempotent.message()).thenReturn("repeated-request");
        // 准备 joinPoint：可定位的方法描述 + 含秘密的参数
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(new FixedSignature(METHOD_DESC));
        Object[] sensitiveArgs = new Object[]{buildSensitiveArgs()};
        when(joinPoint.getArgs()).thenReturn(sensitiveArgs);
        // 锁定失败 -> 触发重复请求拒绝（同键同参，getDigest 返回 null）
        when(idempotentRedisDAO.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(false);
        when(idempotentRedisDAO.getDigest(anyString())).thenReturn(null);

        // 调用，断言抛出 ServiceException 且错误码保留
        ServiceException ex = assertThrows(ServiceException.class,
                () -> idempotentAspect.aroundPointCut(joinPoint, idempotent));
        assertEquals(GlobalErrorCodeConstants.REPEATED_REQUESTS.getCode(), ex.getCode());

        // C1（IMP-6 验证）：钉死传给 DAO 的摘要 = MD5(未截断脱敏入参)（ZS-SEC-011.B P2-1 切分），而非 MD5(原文)
        String expectedDigest = SecureUtil.md5(LogSanitizeUtils.sanitizeArgsUntruncated(sensitiveArgs));
        verify(idempotentRedisDAO).setIfAbsent(anyString(), eq(expectedDigest), eq(1L), eq(TimeUnit.SECONDS));
        // C1（MIN-5）：脱敏后的摘要输入串不得含明文秘密，证明 IMP-6 生效（摘要输入已脱敏）
        String sanitized = LogSanitizeUtils.sanitizeArgs(sensitiveArgs);
        assertFalse(sanitized.contains(SECRET_PASSWORD), "摘要输入不得含明文 password（IMP-6）");
        assertFalse(sanitized.contains(SECRET_TOKEN), "摘要输入不得含明文 token（IMP-6）");
        assertFalse(sanitized.contains(SECRET_APIKEY), "摘要输入不得含明文嵌套 apiKey（IMP-6）");
        assertFalse(sanitized.contains(SECRET_LIST), "摘要输入不得含明文数组内 secret（IMP-6）");

        // 断言日志：秘密值不出现，非敏感字段与方法描述保留，敏感字段被掩码
        String logText = capturedLog();
        assertFalse(logText.contains(SECRET_PASSWORD), "password 秘密不应出现在日志");
        assertFalse(logText.contains(SECRET_TOKEN), "token 秘密不应出现在日志");
        assertFalse(logText.contains(SECRET_APIKEY), "嵌套 apiKey 秘密不应出现在日志");
        assertFalse(logText.contains(SECRET_LIST), "数组内 secret 秘密不应出现在日志");
        assertTrue(logText.contains(SAFE_USERNAME), "非敏感 username 应保留");
        assertTrue(logText.contains(SAFE_NOTE), "非敏感 note 应保留");
        assertTrue(logText.contains(METHOD_DESC), "方法描述应保留以便定位");
        assertTrue(logText.contains("***"), "敏感字段应被掩码");
    }

    @Test
    public void testAroundPointCut_firstRequest_proceedsWithoutRejectLog() throws Throwable {
        // 首次请求：锁定成功，正常放行，不产生重复请求日志
        Idempotent idempotent = mock(Idempotent.class);
        doReturn(FixedKeyResolver.class).when(idempotent).keyResolver();
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenReturn("OK");
        when(idempotentRedisDAO.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);

        Object result = idempotentAspect.aroundPointCut(joinPoint, idempotent);

        assertEquals("OK", result);
        assertTrue(listAppender.list.isEmpty(), "首次请求不应产生拒绝日志");
    }

    // ========== ZS-SEC-011.A 新增测试：同键异参冲突 + 同键同参重复 ==========

    @Test
    public void testAroundPointCut_sameKeyDifferentArgs_shouldThrowConflict() throws Throwable {
        // 准备：幂等注解
        Idempotent idempotent = mock(Idempotent.class);
        doReturn(FixedKeyResolver.class).when(idempotent).keyResolver();
        when(idempotent.timeout()).thenReturn(5);
        when(idempotent.timeUnit()).thenReturn(TimeUnit.SECONDS);
        // 准备：joinPoint 带参数 argB
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(new FixedSignature(METHOD_DESC));
        Object[] argsB = new Object[]{"argB"};
        when(joinPoint.getArgs()).thenReturn(argsB);

        // 模拟：setIfAbsent 返回 false（key 已存在），getDigest 返回不同的摘要（stored=argA）
        // IMP-6/ZS-SEC-011.B P2-1：期望 digest 按未截断脱敏口径重算
        String argsDigestB = SecureUtil.md5(LogSanitizeUtils.sanitizeArgsUntruncated(argsB));
        String storedDigestA = SecureUtil.md5(LogSanitizeUtils.sanitizeArgsUntruncated(new Object[]{"argA"}));
        when(idempotentRedisDAO.setIfAbsent(anyString(), eq(argsDigestB), anyLong(), any())).thenReturn(false);
        when(idempotentRedisDAO.getDigest(anyString())).thenReturn(storedDigestA);

        // 调用，断言抛出 ServiceException 且 message 包含冲突信息
        ServiceException ex = assertThrows(ServiceException.class,
                () -> idempotentAspect.aroundPointCut(joinPoint, idempotent));
        assertEquals(GlobalErrorCodeConstants.REPEATED_REQUESTS.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("幂等键冲突") || ex.getMessage().contains("相同幂等键携带了不同请求内容"),
                "同键异参应抛冲突专属 message，实际: " + ex.getMessage());
        // C2（MIN-6）：钉死传给 DAO 的摘要确为 MD5(脱敏 argB)，使"摘要是否真按口径 MD5"可被捕获（原 stub 对此是盲的）
        verify(idempotentRedisDAO).setIfAbsent(anyString(), eq(argsDigestB), eq(5L), eq(TimeUnit.SECONDS));
    }

    @Test
    public void testAroundPointCut_sameKeySameArgs_shouldThrowRepeat() throws Throwable {
        // 准备：幂等注解
        Idempotent idempotent = mock(Idempotent.class);
        doReturn(FixedKeyResolver.class).when(idempotent).keyResolver();
        when(idempotent.timeout()).thenReturn(5);
        when(idempotent.timeUnit()).thenReturn(TimeUnit.SECONDS);
        when(idempotent.message()).thenReturn("repeated-request");
        // 准备：joinPoint 带参数 argA
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(new FixedSignature(METHOD_DESC));
        Object[] argsA = new Object[]{"argA"};
        when(joinPoint.getArgs()).thenReturn(argsA);

        // 模拟：setIfAbsent 返回 false，getDigest 返回相同的摘要
        // IMP-6/ZS-SEC-011.B P2-1：期望 digest 按未截断脱敏口径重算
        String argsDigestA = SecureUtil.md5(LogSanitizeUtils.sanitizeArgsUntruncated(argsA));
        when(idempotentRedisDAO.setIfAbsent(anyString(), eq(argsDigestA), anyLong(), any())).thenReturn(false);
        when(idempotentRedisDAO.getDigest(anyString())).thenReturn(argsDigestA);

        // 调用，断言抛出 ServiceException 且 message 为普通重复提示
        ServiceException ex = assertThrows(ServiceException.class,
                () -> idempotentAspect.aroundPointCut(joinPoint, idempotent));
        assertEquals(GlobalErrorCodeConstants.REPEATED_REQUESTS.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("repeated-request"),
                "同键同参应抛普通重复 message，实际: " + ex.getMessage());
    }

    @Test
    public void testAroundPointCut_getDigestThrows_shouldStillRejectAsRepeatNot500() throws Throwable {
        // C3（IMP-2 回归）：getDigest 抛异常（Redis 抖动）→ 仍为干净的 900，不得升级为 500/RuntimeException 逃逸
        Idempotent idempotent = mock(Idempotent.class);
        doReturn(FixedKeyResolver.class).when(idempotent).keyResolver();
        when(idempotent.timeout()).thenReturn(5);
        when(idempotent.timeUnit()).thenReturn(TimeUnit.SECONDS);
        when(idempotent.message()).thenReturn("repeated-request");
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(new FixedSignature(METHOD_DESC));
        when(joinPoint.getArgs()).thenReturn(new Object[]{"argA"});
        when(idempotentRedisDAO.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(false);
        when(idempotentRedisDAO.getDigest(anyString())).thenThrow(new RuntimeException("redis down"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> idempotentAspect.aroundPointCut(joinPoint, idempotent));
        // digest 降级 null → conflict=false → 仍 900，message 回落注解 message()
        assertEquals(GlobalErrorCodeConstants.REPEATED_REQUESTS.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("repeated-request"),
                "getDigest 异常应降级按重复处理，回落注解 message，实际: " + ex.getMessage());
    }

    @Test
    public void testAroundPointCut_legacyEmptyDigest_shouldTreatAsRepeatNotConflict() throws Throwable {
        // C4（IMP-3 回归）：滚动升级期旧格式空串值（旧实现存 ""）→ 判为重复而非冲突
        Idempotent idempotent = mock(Idempotent.class);
        doReturn(FixedKeyResolver.class).when(idempotent).keyResolver();
        when(idempotent.timeout()).thenReturn(5);
        when(idempotent.timeUnit()).thenReturn(TimeUnit.SECONDS);
        when(idempotent.message()).thenReturn("repeated-request");
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(new FixedSignature(METHOD_DESC));
        when(joinPoint.getArgs()).thenReturn(new Object[]{"argA"});
        when(idempotentRedisDAO.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(false);
        when(idempotentRedisDAO.getDigest(anyString())).thenReturn("");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> idempotentAspect.aroundPointCut(joinPoint, idempotent));
        assertEquals(GlobalErrorCodeConstants.REPEATED_REQUESTS.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("repeated-request"),
                "旧格式空串值应判为重复，回落注解 message，实际: " + ex.getMessage());
        assertFalse(ex.getMessage().contains("冲突"),
                "旧格式空串值不应误判为冲突（StrUtil.isNotEmpty 兼容），实际: " + ex.getMessage());
    }

    @Test
    public void testAroundPointCut_realDefaultResolver_sameKeyForcesRepeatNotConflict() throws Throwable {
        // C5（MIN-7 + IMP-1 可执行契约）：用真实 DefaultIdempotentKeyResolver 驱动切面。
        // ZS-SEC-011.B Key/Value 切分后：Key 不含 argsStr（Key=method+作用域稳定短键），
        // 入参差异由 Value 中的全量摘要承担——同 Key 不同参在切面层可比对（conflict 分支默认路径可达）。
        // 本用例钉「同主体同键重复」路径：同 method/tenant/user/type/args → 同 Key 同摘要 → 重复文案。
        Idempotent idempotent = mock(Idempotent.class);
        doReturn(DefaultIdempotentKeyResolver.class).when(idempotent).keyResolver();
        when(idempotent.timeout()).thenReturn(5);
        when(idempotent.timeUnit()).thenReturn(TimeUnit.SECONDS);
        when(idempotent.message()).thenReturn("repeated-request");

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(new FixedSignature(METHOD_DESC));
        Object[] args = new Object[]{"argA"};
        when(joinPoint.getArgs()).thenReturn(args);

        HttpServletRequest request = mock(HttpServletRequest.class);
        // 用真实解析器构造独立切面（不复用 setUp 里 FixedKeyResolver 的切面）
        IdempotentAspect realAspect = new IdempotentAspect(
                List.of(new DefaultIdempotentKeyResolver()), idempotentRedisDAO,
                IdempotentAspectPersistentModeTest.providerOf(null));

        try (MockedStatic<ServletUtils> servletMs = mockStatic(ServletUtils.class);
             MockedStatic<WebFrameworkUtils> webMs = mockStatic(WebFrameworkUtils.class)) {
            servletMs.when(ServletUtils::getRequest).thenReturn(request);
            webMs.when(() -> WebFrameworkUtils.getTenantId(request)).thenReturn(1L);
            webMs.when(() -> WebFrameworkUtils.getLoginUserId(request)).thenReturn(100L);
            webMs.when(() -> WebFrameworkUtils.getLoginUserType(request)).thenReturn(2);

        // 真实解析器算出的 Key（固定主体上下文 → 唯一确定），与切面内部解析结果必然一致
        String expectedKey = new DefaultIdempotentKeyResolver().resolver(joinPoint, idempotent);
        String argsDigest = SecureUtil.md5(LogSanitizeUtils.sanitizeArgsUntruncated(args));
            // 同 Key 已存在 → setIfAbsent 返回 false；默认路径 storedDigest 必等于 argsDigest
            when(idempotentRedisDAO.setIfAbsent(eq(expectedKey), eq(argsDigest), anyLong(), any())).thenReturn(false);
            when(idempotentRedisDAO.getDigest(eq(expectedKey))).thenReturn(argsDigest);

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> realAspect.aroundPointCut(joinPoint, idempotent));
            assertEquals(GlobalErrorCodeConstants.REPEATED_REQUESTS.getCode(), ex.getCode());
            // 同键同参 → message 为重复文案（ZS-SEC-011.B Key/Value 切分后默认路径冲突分支可达，
            // 但本用例入参相同、摘要相同，仍应命中重复而非冲突）
            assertTrue(ex.getMessage().contains("repeated-request"),
                    "默认解析器同 Key 同摘要场景应命中重复文案，实际: " + ex.getMessage());
            assertFalse(ex.getMessage().contains("冲突"),
                    "同键同参不应误判为冲突，实际: " + ex.getMessage());
            // 钉死：真实解析器算出的 Key 与脱敏摘要确被用于 setIfAbsent
            verify(idempotentRedisDAO).setIfAbsent(eq(expectedKey), eq(argsDigest), eq(5L), eq(TimeUnit.SECONDS));
        }
    }

    // ========== ZS-SEC-011.B：P2-1 截断碰撞回归 + P2-2 裸 key 日志脱敏回归 ==========

    @Test
    public void testAroundPointCut_argsBeyondTruncationLength_differentTails_conflictNotRepeat() throws Throwable {
        // ZS-SEC-011.B P2-1 回归（codex 011.A r0 P2-1）：摘要此前在 LogSanitizeUtils 的 2048 日志截断后计算，
        // 「等长、仅第 2048 字符后不同」的两个入参产生相同 MD5 → 异参被误判为重复。
        // 修复后摘要走「未截断的脱敏表示」，异尾入参必须被判为冲突（同键异参）而非重复。
        Idempotent idempotent = mock(Idempotent.class);
        doReturn(FixedKeyResolver.class).when(idempotent).keyResolver();
        when(idempotent.timeout()).thenReturn(5);
        when(idempotent.timeUnit()).thenReturn(TimeUnit.SECONDS);

        // 两个等长（>2048）且仅尾部不同的入参
        String base = "x".repeat(2100);
        Object[] argsA = new Object[]{base + "-tail-A"};
        Object[] argsB = new Object[]{base + "-tail-B"};

        // 先证明两条入参的未截断摘要确实不同（修复的根基）
        String digestA = SecureUtil.md5(LogSanitizeUtils.sanitizeArgsUntruncated(argsA));
        String digestB = SecureUtil.md5(LogSanitizeUtils.sanitizeArgsUntruncated(argsB));
        assertNotEquals(digestA, digestB, "未截断摘要不得因日志截断而碰撞（P2-1 根基）");

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(new FixedSignature(METHOD_DESC));
        when(joinPoint.getArgs()).thenReturn(argsB);

        // 首请求已携 digestA 锁定同键；携带 argsB 的重放应被识别为「同键异参冲突」
        when(idempotentRedisDAO.setIfAbsent(anyString(), eq(digestB), anyLong(), any())).thenReturn(false);
        when(idempotentRedisDAO.getDigest(anyString())).thenReturn(digestA);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> idempotentAspect.aroundPointCut(joinPoint, idempotent));
        assertTrue(ex.getMessage().contains("幂等键冲突"),
                "等长异尾（>2048）入参应判冲突而非重复（P2-1），实际: " + ex.getMessage());
        // 钉死传给 DAO 的摘要确为未截断口径的 MD5
        verify(idempotentRedisDAO).setIfAbsent(anyString(), eq(digestB), eq(5L), eq(TimeUnit.SECONDS));
    }

    @Test
    public void testAroundPointCut_getDigestThrows_sensitiveExpressionKey_notLoggedRaw() throws Throwable {
        // ZS-SEC-011.B P2-2 回归（codex 011.A r0 P2-2）：Expression 解析器 Key 可能是敏感原值（如 #request.token），
        // Redis 读回摘要失败分支的 warn 日志此前直接落 key 原文；修复后只落 md5 指纹（可关联、不可逆）。
        IdempotentKeyResolver secretKeyResolver = (joinPoint, idempotent) -> SECRET_TOKEN;
        IdempotentAspect secretAspect = new IdempotentAspect(List.of(secretKeyResolver), idempotentRedisDAO,
                IdempotentAspectPersistentModeTest.providerOf(null));

        Idempotent idempotent = mock(Idempotent.class);
        doReturn(secretKeyResolver.getClass()).when(idempotent).keyResolver();
        when(idempotent.timeout()).thenReturn(5);
        when(idempotent.timeUnit()).thenReturn(TimeUnit.SECONDS);

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(new FixedSignature(METHOD_DESC));
        when(joinPoint.getArgs()).thenReturn(new Object[]{"argA"});
        when(idempotentRedisDAO.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(false);
        when(idempotentRedisDAO.getDigest(anyString())).thenThrow(new RuntimeException("redis down"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> secretAspect.aroundPointCut(joinPoint, idempotent));
        assertEquals(GlobalErrorCodeConstants.REPEATED_REQUESTS.getCode(), ex.getCode(),
                "读回失败仍应降级为干净 900");

        String logText = capturedLog();
        assertFalse(logText.contains(SECRET_TOKEN), "读回失败告警不得落 Key 原文（P2-2 脱敏）");
        assertTrue(logText.contains(SecureUtil.md5(SECRET_TOKEN)), "告警应携 Key 的 md5 指纹以便关联定位");
    }

    @Test
    public void testAroundPointCut_firstRequest_servletArgsExcludedFromDigest() throws Throwable {
        // codex r0 P1 + r1 P1 回归：@Idempotent 方法若含 servlet 响应入参，计算「每请求必算」的参数摘要时不得用 Jackson 序列化它——
        // 序列化会调用 getWriter()/getOutputStream() 等 getter，提前「选定」响应输出模式，破坏后续二进制输出
        //（如 ServletUtils.writeAttachment 附件下载抛 IllegalStateException），且首次放行请求即触发
        //（SEC-011.A 把 sanitizeArgs 从「仅拒绝分支」提升为「每请求必算摘要」）。
        // r1 关键：运行时实现类（Tomcat org.apache.catalina.connector.ResponseFacade）不以 servlet 包名开头，
        //「按包名前缀排除」会漏排它——必须「按类型 instanceof 排除」。故用 ContainerLikeResponse（具体实现 +
        // 运行时类名落在业务包 cn.zszj 下 + IS-A ServletResponse）精确复现该特征，接口 mock（ByteBuddy 名以 jakarta.servlet 开头）覆盖不到此运行时场景。
        Idempotent idempotent = mock(Idempotent.class);
        doReturn(FixedKeyResolver.class).when(idempotent).keyResolver();
        when(idempotent.timeout()).thenReturn(5);
        when(idempotent.timeUnit()).thenReturn(TimeUnit.SECONDS);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenReturn("OK");
        // 具体容器响应的 spy（非纯接口 mock）：ContainerLikeResponse 运行时类名落业务包 cn.zszj 下（不以 servlet 包名开头），
        // 精确复现 Tomcat ResponseFacade 的「非 servlet 包名 + servlet 类型」特征；用 spy 以便 verify 摘要计算是否触碰 writer/输出流
        HttpServletResponse response = spy(new ContainerLikeResponse());
        Object businessArg = "orderPayload";
        // 入参含 servlet 响应对象 + 业务参数
        when(joinPoint.getArgs()).thenReturn(new Object[]{businessArg, response});
        when(idempotentRedisDAO.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);

        Object result = idempotentAspect.aroundPointCut(joinPoint, idempotent);
        assertEquals("OK", result);

        // C1：摘要应仅由「排除 servlet 对象后」的业务入参计算——含 servlet 的口径与此不同（RED），排除后一致（GREEN）
        String expectedDigestExcludingServlet = SecureUtil.md5(LogSanitizeUtils.sanitizeArgs(new Object[]{businessArg}));
        verify(idempotentRedisDAO).setIfAbsent(anyString(), eq(expectedDigestExcludingServlet), eq(5L), eq(TimeUnit.SECONDS));
        // C2（codex r2 P2 修正）：直接钉死摘要计算绝不触碰响应的 writer/输出流。
        // Spring 6.2 的 MockHttpServletResponse 中 getWriter()/getOutputStream() 访问标志相互独立（不互斥抛异常），
        // 故不能靠「getOutputStream 是否抛异常」间接判定；改用 spy + verify(never) 直接检测 getter 是否被调用。
        // 类型排除生效后 response 未进入 sanitizeArgs → Jackson 从不序列化它 → getWriter/getOutputStream 从未被调用。
        verify(response, never()).getWriter();
        verify(response, never()).getOutputStream();
    }

    // ========== Helper methods ==========

    private static Map<String, Object> buildSensitiveArgs() {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("username", SAFE_USERNAME);
        req.put("password", SECRET_PASSWORD);
        req.put("token", SECRET_TOKEN);
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("apiKey", SECRET_APIKEY);
        nested.put("note", SAFE_NOTE);
        req.put("nested", nested);
        req.put("list", List.of(Map.of("secret", SECRET_LIST)));
        return req;
    }

    private String capturedLog() {
        return listAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.joining("\n"));
    }

    /** 测试用固定 Key 解析器，返回常量 Key，避开对 JoinPoint 内容的依赖 */
    static class FixedKeyResolver implements IdempotentKeyResolver {
        @Override
        public String resolver(JoinPoint joinPoint, Idempotent idempotent) {
            return "idem-fixed-key";
        }
    }

    /** 测试用固定方法签名，toString 返回可定位的方法描述（Mockito 无法 stub toString，故用真实实现） */
    @SuppressWarnings({"rawtypes", "unchecked"})
    static class FixedSignature implements Signature {
        private final String text;

        FixedSignature(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }

        @Override
        public String toShortString() {
            return text;
        }

        @Override
        public String toLongString() {
            return text;
        }

        @Override
        public String getName() {
            return text;
        }

        @Override
        public int getModifiers() {
            return 1;
        }

        @Override
        public Class getDeclaringType() {
            return Object.class;
        }

        @Override
        public String getDeclaringTypeName() {
            return "Object";
        }
    }

    /**
     * 模拟 Tomcat 运行时响应实现（{@code org.apache.catalina.connector.ResponseFacade}）的关键特征：
     * 具体实现（非接口 mock）+ 运行时类名落在业务包 {@code cn.zszj} 下（不以任何 servlet 包名开头）+ IS-A {@code ServletResponse}。
     * 用于证明「按包名前缀排除」会漏排真实容器响应（RED），只有「按类型 instanceof 排除」能命中（GREEN）。
     */
    static class ContainerLikeResponse extends MockHttpServletResponse {
    }

}
