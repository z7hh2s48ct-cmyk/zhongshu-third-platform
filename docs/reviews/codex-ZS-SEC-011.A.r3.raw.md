node.exe : OpenAI Codex v0.154.0
所在位置 C:\Users\Administrator\AppData\Roaming\npm\codex.ps1:24 字符: 5
+     & "node$exe"  "$basedir/node_modules/@openai/codex/bin/codex.js"  ...
+     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    + CategoryInfo          : NotSpecified: (OpenAI Codex v0.154.0:String) [], RemoteException
    + FullyQualifiedErrorId : NativeCommandError
 
--------
workdir: E:\众墅之家AI赋能平台底座
model: gpt-6-astra
provider: openai
approval: never
sandbox: read-only
reasoning effort: xhigh
reasoning summaries: none
session id: 01a09446-183f-7560-8507-bcca7860e01f
--------
user
commit c42b15c
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "Get-Location; git status --short; git rev-pa
rse --show-toplevel; git show --stat --oneline --decorate --no-renames c42b15c6; Get-ChildItem -Force -Name; Get-ChildI
tem -Path E:\\ -Force -File -Filter 'AGENTS*' | Select-Object FullName" in E:\众墅之家AI赋能平台底座
 succeeded in 148ms:
?? ".omx/plans/2026-09-11-B03-Wave1-\345\205\255\351\241\271\345\271\266\350\241\214\345\274\200\345\217\221\350\256\24
1\345\210\222.md"
?? _zs_sec011a_codex.ps1
?? _zs_sec011a_codex_r1.ps1
?? _zs_sec011a_codex_r2.ps1
?? _zs_sec011a_codex_r3.ps1
?? _zs_sec011a_fixmsg.txt
?? _zs_sec011a_fixmsg_r1.txt
?? _zs_sec011a_fixmsg_r2.txt
?? _zs_sec011a_prot.txt
?? _zs_sec011a_tdd.ps1
?? _zs_sec011a_tdd.txt
?? _zs_sec011a_verify.ps1
?? docs/reviews/codex-ZS-SEC-011.A.r1.raw.md
?? docs/reviews/codex-ZS-SEC-011.A.r2.raw.md
?? docs/reviews/codex-ZS-SEC-011.A.raw.md
?? outputs/
E:/众墅之家AI赋能平台底座
c42b15c6 (HEAD -> main) test(sec-011-a): 幂等 servlet 排除回归测试改用 spy + verify(never) 直接检测响应访问 [ZS-SEC-011.A]
 .../idempotent/core/aop/IdempotentAspectTest.java     | 19 +++++++++++--------
 1 file changed, 11 insertions(+), 8 deletions(-)
Path                     
----                     
E:\����֮��AI����ƽ̨����
.git
.github
.omx
.tmp-iam003
.tmp-sec011
apps
docs
outputs
reference
scripts
services
third_party
tools
.gitignore
README.md
THIRD_PARTY_NOTICES.md
_zs_sec011a_codex.ps1
_zs_sec011a_codex_r1.ps1
_zs_sec011a_codex_r2.ps1
_zs_sec011a_codex_r3.ps1
_zs_sec011a_fixmsg.txt
_zs_sec011a_fixmsg_r1.txt
_zs_sec011a_fixmsg_r2.txt
_zs_sec011a_prot.txt
_zs_sec011a_tdd.ps1
_zs_sec011a_tdd.txt
_zs_sec011a_verify.ps1



exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "[Console]::OutputEncoding = [System.Text.Enc
oding]::UTF8; git -c core.quotepath=false show --format=fuller --no-ext-diff --no-renames c42b15c6; rg --files --hidden
 -g 'AGENTS.md' -g 'AGENTS.override.md' -g '*INSTRUCTIONS*' -g 'CLAUDE.md' -g '"'!**/.git/**'"'; if (Test-Path 'C:\\Use
rs\\Administrator\\.codex\\config.toml') { Select-String -Path 'C:\\Users\\Administrator\\.codex\\config.toml' -Pattern
 'project_doc|fallback' }" in E:\众墅之家AI赋能平台底座
 succeeded in 76ms:
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "Get-Content -LiteralPath 'services/zhongshu-
core/zszj-framework/zszj-spring-boot-starter-protection/src/test/java/cn/zszj/framework/idempotent/core/aop/IdempotentA
spectTest.java' -Encoding UTF8; Get-Content -LiteralPath 'services/zhongshu-core/zszj-framework/zszj-spring-boot-starte
r-protection/src/main/java/cn/zszj/framework/idempotent/core/aop/IdempotentAspect.java' -Encoding UTF8; Get-Content -Li
teralPath 'services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/pom.xml' -Encoding UTF8; rg -n 'mo
ck-maker|mockito|maven-surefire|java.version|maven.compiler' services/zhongshu-core/pom.xml services/zhongshu-core/zszj
-dependencies services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test; rg --files --hidden -g '*AGENTS*' -g
 '"'!**/.git/**'"'; Get-Command java,mvn,python -ErrorAction SilentlyContinue | Select-Object Name,Source" in E:\众墅之家AI
赋能平台底座
 exited 1 in 422ms:
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
 * {@link IdempotentAspect} �ĵ�Ԫ���ԡ�
 *
 * ���� SEC-007���ظ��ύ���ܾ�ʱ���������������Ⱦ� {@code LogSanitizeUtils.sanitizeArgs} ������д��־��
 * ����ֵ��password/token/Ƕ�� apiKey/������ secret�����ó�����Ӧ����־�У�
 * ͬʱ�����ɶ�λ�ķ���������������ֶΣ����׳�Я����ȷ������� ServiceException��
 *
 * ZS-SEC-011.A��ͬ����γ�ͻ��� + ͬ��ͬ���ظ������⡣
 * IMP-6��ժҪ����ͳһ�� sanitizeArgs �����ھ����������� digest �ఴ�˿ھ����㡣
 *
 * ZS-SEC-011.A ��ͬ���Ա߽磺����������֤����������ʱ���ء���ͬ����
 * ͬ��ͬ���ظ��ܾ���ͬ����γ�ͻ�ܾ�������/�⻧���롣
 * ��ʽ����������������ŵ�־û��ݵȣ�����/��ʱ/������Ϻ󲻱�֤����ԭҵ��������
 * �־û��ݵȹ� ZS-SEC-011.B���������Գ־û���������ԡ�
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
        idempotentAspect = new IdempotentAspect(List.of(new FixedKeyResolver()), idempotentRedisDAO);
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
        // ׼���ݵ�ע�⣺���ܾ�·�����õ�������
        Idempotent idempotent = mock(Idempotent.class);
        doReturn(FixedKeyResolver.class).when(idempotent).keyResolver();
        when(idempotent.timeout()).thenReturn(1);
        when(idempotent.timeUnit()).thenReturn(TimeUnit.SECONDS);
        when(idempotent.message()).thenReturn("repeated-request");
        // ׼�� joinPoint���ɶ�λ�ķ������� + �����ܵĲ���
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(new FixedSignature(METHOD_DESC));
        Object[] sensitiveArgs = new Object[]{buildSensitiveArgs()};
        when(joinPoint.getArgs()).thenReturn(sensitiveArgs);
        // ����ʧ�� -> �����ظ�����ܾ���ͬ��ͬ�Σ�getDigest ���� null��
        when(idempotentRedisDAO.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(false);
        when(idempotentRedisDAO.getDigest(anyString())).thenReturn(null);

        // ���ã������׳� ServiceException �Ҵ����뱣��
        ServiceException ex = assertThrows(ServiceException.class,
                () -> idempotentAspect.aroundPointCut(joinPoint, idempotent));
        assertEquals(GlobalErrorCodeConstants.REPEATED_REQUESTS.getCode(), ex.getCode());

        // C1��IMP-6 ��֤������������ DAO ��ժҪ = MD5(���������)������ MD5(ԭ��)
        String expectedDigest = SecureUtil.md5(LogSanitizeUtils.sanitizeArgs(sensitiveArgs));
        verify(idempotentRedisDAO).setIfAbsent(anyString(), eq(expectedDigest), eq(1L), eq(TimeUnit.SECONDS));
        // C1��MIN-5�����������ժҪ���봮���ú��������ܣ�֤�� IMP-6 ��Ч��ժҪ������������
        String sanitized = LogSanitizeUtils.sanitizeArgs(sensitiveArgs);
        assertFalse(sanitized.contains(SECRET_PASSWORD), "ժҪ���벻�ú����� password��IMP-6��");
        assertFalse(sanitized.contains(SECRET_TOKEN), "ժҪ���벻�ú����� token��IMP-6��");
        assertFalse(sanitized.contains(SECRET_APIKEY), "ժҪ���벻�ú�����Ƕ�� apiKey��IMP-6��");
        assertFalse(sanitized.contains(SECRET_LIST), "ժҪ���벻�ú����������� secret��IMP-6��");

        // ������־������ֵ�����֣��������ֶ��뷽�����������������ֶα�����
        String logText = capturedLog();
        assertFalse(logText.contains(SECRET_PASSWORD), "password ���ܲ�Ӧ��������־");
        assertFalse(logText.contains(SECRET_TOKEN), "token ���ܲ�Ӧ��������־");
        assertFalse(logText.contains(SECRET_APIKEY), "Ƕ�� apiKey ���ܲ�Ӧ��������־");
        assertFalse(logText.contains(SECRET_LIST), "������ secret ���ܲ�Ӧ��������־");
        assertTrue(logText.contains(SAFE_USERNAME), "������ username Ӧ����");
        assertTrue(logText.contains(SAFE_NOTE), "������ note Ӧ����");
        assertTrue(logText.contains(METHOD_DESC), "��������Ӧ�����Ա㶨λ");
        assertTrue(logText.contains("***"), "�����ֶ�Ӧ������");
    }

    @Test
    public void testAroundPointCut_firstRequest_proceedsWithoutRejectLog() throws Throwable {
        // �״����������ɹ����������У��������ظ�������־
        Idempotent idempotent = mock(Idempotent.class);
        doReturn(FixedKeyResolver.class).when(idempotent).keyResolver();
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenReturn("OK");
        when(idempotentRedisDAO.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);

        Object result = idempotentAspect.aroundPointCut(joinPoint, idempotent);

        assertEquals("OK", result);
        assertTrue(listAppender.list.isEmpty(), "�״�����Ӧ�����ܾ���־");
    }

    // ========== ZS-SEC-011.A �������ԣ�ͬ����γ�ͻ + ͬ��ͬ���ظ� ==========

    @Test
    public void testAroundPointCut_sameKeyDifferentArgs_shouldThrowConflict() throws Throwable {
        // ׼�����ݵ�ע��
        Idempotent idempotent = mock(Idempotent.class);
        doReturn(FixedKeyResolver.class).when(idempotent).keyResolver();
        when(idempotent.timeout()).thenReturn(5);
        when(idempotent.timeUnit()).thenReturn(TimeUnit.SECONDS);
        // ׼����joinPoint ������ argB
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(new FixedSignature(METHOD_DESC));
        Object[] argsB = new Object[]{"argB"};
        when(joinPoint.getArgs()).thenReturn(argsB);

        // ģ�⣺setIfAbsent ���� false��key �Ѵ��ڣ���getDigest ���ز�ͬ��ժҪ��stored=argA��
        // IMP-6������ digest �� sanitizeArgs �����ھ�����
        String argsDigestB = SecureUtil.md5(LogSanitizeUtils.sanitizeArgs(argsB));
        String storedDigestA = SecureUtil.md5(LogSanitizeUtils.sanitizeArgs(new Object[]{"argA"}));
        when(idempotentRedisDAO.setIfAbsent(anyString(), eq(argsDigestB), anyLong(), any())).thenReturn(false);
        when(idempotentRedisDAO.getDigest(anyString())).thenReturn(storedDigestA);

        // ���ã������׳� ServiceException �� message ������ͻ��Ϣ
        ServiceException ex = assertThrows(ServiceException.class,
                () -> idempotentAspect.aroundPointCut(joinPoint, idempotent));
        assertEquals(GlobalErrorCodeConstants.REPEATED_REQUESTS.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("�ݵȼ���ͻ") || ex.getMessage().contains("��ͬ�ݵȼ�Я���˲�ͬ��������"),
                "ͬ�����Ӧ�׳�ͻר�� message��ʵ��: " + ex.getMessage());
        // C2��MIN-6������������ DAO ��ժҪȷΪ MD5(���� argB)��ʹ"ժҪ�Ƿ��水�ھ� MD5"�ɱ�����ԭ stub �Դ���ä�ģ�
        verify(idempotentRedisDAO).setIfAbsent(anyString(), eq(argsDigestB), eq(5L), eq(TimeUnit.SECONDS));
    }

    @Test
    public void testAroundPointCut_sameKeySameArgs_shouldThrowRepeat() throws Throwable {
        // ׼�����ݵ�ע��
        Idempotent idempotent = mock(Idempotent.class);
        doReturn(FixedKeyResolver.class).when(idempotent).keyResolver();
        when(idempotent.timeout()).thenReturn(5);
        when(idempotent.timeUnit()).thenReturn(TimeUnit.SECONDS);
        when(idempotent.message()).thenReturn("repeated-request");
        // ׼����joinPoint ������ argA
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(new FixedSignature(METHOD_DESC));
        Object[] argsA = new Object[]{"argA"};
        when(joinPoint.getArgs()).thenReturn(argsA);

        // ģ�⣺setIfAbsent ���� false��getDigest ������ͬ��ժҪ
        // IMP-6������ digest �� sanitizeArgs �����ھ�����
        String argsDigestA = SecureUtil.md5(LogSanitizeUtils.sanitizeArgs(argsA));
        when(idempotentRedisDAO.setIfAbsent(anyString(), eq(argsDigestA), anyLong(), any())).thenReturn(false);
        when(idempotentRedisDAO.getDigest(anyString())).thenReturn(argsDigestA);

        // ���ã������׳� ServiceException �� message Ϊ��ͨ�ظ���ʾ
        ServiceException ex = assertThrows(ServiceException.class,
                () -> idempotentAspect.aroundPointCut(joinPoint, idempotent));
        assertEquals(GlobalErrorCodeConstants.REPEATED_REQUESTS.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("repeated-request"),
                "ͬ��ͬ��Ӧ����ͨ�ظ� message��ʵ��: " + ex.getMessage());
    }

    @Test
    public void testAroundPointCut_getDigestThrows_shouldStillRejectAsRepeatNot500() throws Throwable {
        // C3��IMP-2 �ع飩��getDigest ���쳣��Redis �������� ��Ϊ�ɾ��� 900����������Ϊ 500/RuntimeException ����
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
        // digest ���� null �� conflict=false �� �� 900��message ����ע�� message()
        assertEquals(GlobalErrorCodeConstants.REPEATED_REQUESTS.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("repeated-request"),
                "getDigest �쳣Ӧ�������ظ�����������ע�� message��ʵ��: " + ex.getMessage());
    }

    @Test
    public void testAroundPointCut_legacyEmptyDigest_shouldTreatAsRepeatNotConflict() throws Throwable {
        // C4��IMP-3 �ع飩�����������ھɸ�ʽ�մ�ֵ����ʵ�ִ� ""���� ��Ϊ�ظ����ǳ�ͻ
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
                "�ɸ�ʽ�մ�ֵӦ��Ϊ�ظ�������ע�� message��ʵ��: " + ex.getMessage());
        assertFalse(ex.getMessage().contains("��ͻ"),
                "�ɸ�ʽ�մ�ֵ��Ӧ����Ϊ��ͻ��StrUtil.isNotEmpty ���ݣ���ʵ��: " + ex.getMessage());
    }

    @Test
    public void testAroundPointCut_realDefaultResolver_sameKeyForcesRepeatNotConflict() throws Throwable {
        // C5��MIN-7 + IMP-1 ��ִ����Լ��������ʵ DefaultIdempotentKeyResolver �������档
        // Ĭ�Ͻ������� argsStr ���� Key �� ͬ Key ��Ȼͬ�� �� ���� conflict ��֧�㲻�ɴ
        // ����ͬ Key ������ͬ method/tenant/user/type/args������"Ĭ��·����ͻ���ɴ�"���ɿ�ִ���ĵ���
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
        // ����ʵ����������������棨������ setUp �� FixedKeyResolver �����棩
        IdempotentAspect realAspect = new IdempotentAspect(
                List.of(new DefaultIdempotentKeyResolver()), idempotentRedisDAO);

        try (MockedStatic<ServletUtils> servletMs = mockStatic(ServletUtils.class);
             MockedStatic<WebFrameworkUtils> webMs = mockStatic(WebFrameworkUtils.class)) {
            servletMs.when(ServletUtils::getRequest).thenReturn(request);
            webMs.when(() -> WebFrameworkUtils.getTenantId(request)).thenReturn(1L);
            webMs.when(() -> WebFrameworkUtils.getLoginUserId(request)).thenReturn(100L);
            webMs.when(() -> WebFrameworkUtils.getLoginUserType(request)).thenReturn(2);

            // ��ʵ����������� Key���̶����������� �� Ψһȷ�������������ڲ����������Ȼһ��
            String expectedKey = new DefaultIdempotentKeyResolver().resolver(joinPoint, idempotent);
            String argsDigest = SecureUtil.md5(LogSanitizeUtils.sanitizeArgs(args));
            // ͬ Key �Ѵ��� �� setIfAbsent ���� false��Ĭ��·�� storedDigest �ص��� argsDigest
            when(idempotentRedisDAO.setIfAbsent(eq(expectedKey), eq(argsDigest), anyLong(), any())).thenReturn(false);
            when(idempotentRedisDAO.getDigest(eq(expectedKey))).thenReturn(argsDigest);

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> realAspect.aroundPointCut(joinPoint, idempotent));
            assertEquals(GlobalErrorCodeConstants.REPEATED_REQUESTS.getCode(), ex.getCode());
            // conflict �� false �� message Ϊ�ظ��İ����ǳ�ͻ�İ���Ĭ�Ͻ�����·����ͻ��֧���ɴ
            assertTrue(ex.getMessage().contains("repeated-request"),
                    "Ĭ�Ͻ�����ͬ Key ���� conflict ��Ϊ false��Ӧ�����ظ��İ���ʵ��: " + ex.getMessage());
            assertFalse(ex.getMessage().contains("��ͻ"),
                    "Ĭ�Ͻ�����·����ͻ��֧���ɴ��Ӧ���ֳ�ͻ�İ���ʵ��: " + ex.getMessage());
            // ��������ʵ����������� Key ������ժҪȷ������ setIfAbsent
            verify(idempotentRedisDAO).setIfAbsent(eq(expectedKey), eq(argsDigest), eq(5L), eq(TimeUnit.SECONDS));
        }
    }

    @Test
    public void testAroundPointCut_firstRequest_servletArgsExcludedFromDigest() throws Throwable {
        // codex r0 P1 + r1 P1 �ع飺@Idempotent �������� servlet ��Ӧ��Σ����㡸ÿ������㡹�Ĳ���ժҪʱ������ Jackson ���л�������
        // ���л������ getWriter()/getOutputStream() �� getter����ǰ��ѡ������Ӧ���ģʽ���ƻ��������������
        //���� ServletUtils.writeAttachment ���������� IllegalStateException�������״η������󼴴���
        //��SEC-011.A �� sanitizeArgs �ӡ����ܾ���֧������Ϊ��ÿ�������ժҪ������
        // r1 �ؼ�������ʱʵ���ࣨTomcat org.apache.catalina.connector.ResponseFacade������ servlet ������ͷ��
        //��������ǰ׺�ų�����©�����������롸������ instanceof �ų��������� ContainerLikeResponse������ʵ�� +
        // ����ʱ��������ҵ��� cn.zszj �� + IS-A ServletResponse����ȷ���ָ��������ӿ� mock��ByteBuddy ���� jakarta.servlet 
��ͷ�����ǲ���������ʱ������
        Idempotent idempotent = mock(Idempotent.class);
        doReturn(FixedKeyResolver.class).when(idempotent).keyResolver();
        when(idempotent.timeout()).thenReturn(5);
        when(idempotent.timeUnit()).thenReturn(TimeUnit.SECONDS);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenReturn("OK");
        // ����������Ӧ�� spy���Ǵ��ӿ� mock����ContainerLikeResponse ����ʱ������ҵ��� cn.zszj �£����� servlet ������ͷ����
        // ��ȷ���� Tomcat ResponseFacade �ġ��� servlet ���� + servlet ���͡��������� spy �Ա� verify ժҪ�����Ƿ��� writer/�
����
        HttpServletResponse response = spy(new ContainerLikeResponse());
        Object businessArg = "orderPayload";
        // ��κ� servlet ��Ӧ���� + ҵ�����
        when(joinPoint.getArgs()).thenReturn(new Object[]{businessArg, response});
        when(idempotentRedisDAO.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);

        Object result = idempotentAspect.aroundPointCut(joinPoint, idempotent);
        assertEquals("OK", result);

        // C1��ժҪӦ���ɡ��ų� servlet ����󡹵�ҵ����μ��㡪���� servlet �Ŀھ���˲�ͬ��RED�����ų���һ�£�GREEN��
        String expectedDigestExcludingServlet = SecureUtil.md5(LogSanitizeUtils.sanitizeArgs(new Object[]{businessArg})
);
        verify(idempotentRedisDAO).setIfAbsent(anyString(), eq(expectedDigestExcludingServlet), eq(5L), eq(TimeUnit.SEC
ONDS));
        // C2��codex r2 P2 ��������ֱ�Ӷ���ժҪ�������������Ӧ�� writer/�������
        // Spring 6.2 �� MockHttpServletResponse �� getWriter()/getOutputStream() ���ʱ�־�໥���������������쳣����
        // �ʲ��ܿ���getOutputStream �Ƿ����쳣������ж������� spy + verify(never) ֱ�Ӽ�� getter �Ƿ񱻵��á�
        // �����ų���Ч�� response δ���� sanitizeArgs �� Jackson �Ӳ����л��� �� getWriter/getOutputStream ��δ�����á�
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

    /** �����ù̶� Key �����������س��� Key���ܿ��� JoinPoint ���ݵ����� */
    static class FixedKeyResolver implements IdempotentKeyResolver {
        @Override
        public String resolver(JoinPoint joinPoint, Idempotent idempotent) {
            return "idem-fixed-key";
        }
    }

    /** �����ù̶�����ǩ����toString ���ؿɶ�λ�ķ���������Mockito �޷� stub toString��������ʵʵ�֣� */
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
     * ģ�� Tomcat ����ʱ��Ӧʵ�֣�{@code org.apache.catalina.connector.ResponseFacade}���Ĺؼ�������
     * ����ʵ�֣��ǽӿ� mock��+ ����ʱ��������ҵ��� {@code cn.zszj} �£������κ� servlet ������ͷ��+ IS-A {@code ServletResponse
}��
     * ����֤����������ǰ׺�ų�����©����ʵ������Ӧ��RED����ֻ�С������� instanceof �ų��������У�GREEN����
     */
    static class ContainerLikeResponse extends MockHttpServletResponse {
    }

}
package cn.zszj.framework.idempotent.core.aop;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.zszj.framework.common.util.collection.CollectionUtils;
import cn.zszj.framework.common.util.log.LogSanitizeUtils;
import cn.zszj.framework.idempotent.core.annotation.Idempotent;
import cn.zszj.framework.idempotent.core.keyresolver.IdempotentKeyResolver;
import cn.zszj.framework.idempotent.core.redis.IdempotentRedisDAO;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ���������� {@link Idempotent} ע��ķ�����ʵ���ݵȲ���
 *
 * ZS-SEC-011.A������ͬ����γ�ͻ��⣬����"�ظ�����"��"�ݵȼ���ͻ"���־ܾ�ԭ��
 *
 * ZS-SEC-011.A �߽����������ݵ������ǡ���������ʱ���ء������� Redis TTL ���ڣ���
 * ���ǳ־û��ݵȡ����������������ڳ�ʱ��Redis ������Ϻ󣬲���֤����ԭҵ������
 * ����֤������ͬ��ͬ�β��ظ�ִ�У���ͬ����γ�ͻ�ܾ������� Key ������εĽ�������ExpressionIdempotentKeyResolver������Ч����
 * DefaultIdempotentKeyResolver/UserIdempotentKeyResolver �Ѱ� argsStr ���� Key��ͬ����Ȼͬ�Σ�conflict ��Ϊ false�������ϲ��ɴ
��
 * �ó�ͻ����Ĭ��·��Ҳ������� Key/Value ְ���з֣�Key ��ҵ���ݵȺš�Value ��ȫ�����ժҪ���� ZS-SEC-011.B��
 * �־û��ݵȣ���� + HTTP �������飩�� ZS-SEC-011.B��B05 ���Σ���Χ��
 * ��׼�������Ի�������������Ȩ�����ݵȲ������Ȩ����
 *
 * ZS-SEC-011.A ��֪ȱ�ڣ����� spec ��4 �ļ��嵥���� DefaultIdempotentKeyResolver�����µǼǲ����ף��Һ������񣩣�
 * - [IMP-4 �� REC-1/SEC-011.B] ExpressionIdempotentKeyResolver��Key=�� SpEL ֵ���� method/tenant/user ��������
 *   UserIdempotentKeyResolver���� user �� tenant����ȱ�⻧���룬�� SEC-010 �Ѹ� ExpressionRateLimiterKeyResolver ���⻧�������
������һ�£�
 *   �ҳ�ͻ���Ψһ��Ч·��ǡ�� Expression ������ �� ���ڿ��⻧ײ�� + �����Բ��ŵ����ա�ͳһ SubjectScope ��ȡ�� REC-1��SEC-010+011.A �ϲ�
���������
 * - [IMP-5 �� REC-1] �⻧����ȡ�Կ�ѡ����ͷ tenant-id��WebFrameworkUtils.getTenantId������Ȩ�� TenantContextHolder������ prot
ection ������ biz-tenant����������Ȩƫ�룩��
 *   ʡ��ͷ�������ͬ Key �� ͬһ�������ƹ����ش��ڣ��ǿ����壺userId/userType ȡ��Ȩ�� request attribute ����α�죬tenant-id ͷ�� Tenant
SecurityWebFilter У�鲻��α��������⻧����
 *   Ȩ���⻧Դ�Ŀ�ע�� port �� REC-1���� SEC-010 ͬ���ʡ�
 *
 * @author ���Դ��
 */
@Aspect
@Slf4j
public class IdempotentAspect {

    /**
     * MIN-3��ͬ����γ�ͻ�ܾ��İ�����Ϊ��������ͳһά������Զ���
     */
    private static final String CONFLICT_MESSAGE = "�ݵȼ���ͻ����ͬ�ݵȼ�Я���˲�ͬ��������";

    /**
     * IdempotentKeyResolver ����
     */
    private final Map<Class<? extends IdempotentKeyResolver>, IdempotentKeyResolver> keyResolvers;

    private final IdempotentRedisDAO idempotentRedisDAO;

    public IdempotentAspect(List<IdempotentKeyResolver> keyResolvers, IdempotentRedisDAO idempotentRedisDAO) {
        this.keyResolvers = CollectionUtils.convertMap(keyResolvers, IdempotentKeyResolver::getClass);
        this.idempotentRedisDAO = idempotentRedisDAO;
    }

    @Around(value = "@annotation(idempotent)")
    public Object aroundPointCut(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        // ��� IdempotentKeyResolver
        IdempotentKeyResolver keyResolver = keyResolvers.get(idempotent.keyResolver());
        Assert.notNull(keyResolver, "�Ҳ�����Ӧ�� IdempotentKeyResolver");
        // ���� Key
        String key = keyResolver.resolver(joinPoint, idempotent);

        // ZS-SEC-011.A���������ժҪ������ͬ����γ�ͻ���
        // IMP-6��ժҪ�����Ϊ����������Σ�����δ����ԭ�ģ�������/���ƣ�MD5 ���� Redis value �����߱��ƣ�SEC-007 �� Redis ��ĶԳ�ȱ�ڣ
���
        //        �����ֶβ��챻��Ϊͬ�Σ����ݵ������޺�
        // codex r0 P1��ժҪ��ÿ������㡹�����״η��У������Ⱦ� serializableArgs �ų� servlet/spring-web ������ʩ��Ρ���
        //        LogSanitizeUtils �ڲ��� Jackson valueToTree ���л������ȫ�� getter���� HttpServletResponse ���� getWrite
r()��
        //        ��ǰѡ����Ӧ�ַ����ģʽ���ƻ����� ServletUtils.writeAttachment �ȶ������������ IllegalStateException��
        String argsDigest = SecureUtil.md5(LogSanitizeUtils.sanitizeArgs(serializableArgs(joinPoint.getArgs())));

        // 1. ���� Key��Я������ժҪ��
        // MIN-2��DAO ���� Boolean��pipeline/transaction �������ܷ��� null���� Boolean.TRUE.equals ������ NPE��null ��Ϊδ
�õ��� �� �ܾ���fail-closed��
        boolean success = Boolean.TRUE.equals(idempotentRedisDAO.setIfAbsent(key, argsDigest, idempotent.timeout(), ide
mpotent.timeUnit()));
        // ����ʧ�ܣ����ֳ�ͻ���ظ�
        if (!success) {
            // IMP-2��getDigest �ǡ��Ѿ��߾ܾ�����Ķ��� GET���� Redis �����Ѹɾ��� 900 ����Ϊ 500���쳣���� null��digest ֻӰ���İ��
���������ʧ��
            String storedDigest;
            try {
                storedDigest = idempotentRedisDAO.getDigest(key);
            } catch (Exception ex) {
                log.warn("[aroundPointCut][�ݵȼ�({}) ����ժҪʧ�ܣ��������ظ�����]", key, ex);
                storedDigest = null;
            }
            // IMP-3��StrUtil.isNotEmpty ���ݹ��������ھɸ�ʽ�մ�ֵ����ʵ�ִ� ""�����������г�ͻ
            // IMP-1��Ĭ��·����Default/User ��������Key �Ѻ��� argsStr��ͬ����Ȼͬ�Σ�conflict �� false�������ϲ��ɴ����ͻ��֧��
�� Expression �������� Key ������εĽ�������Ч
            boolean conflict = StrUtil.isNotEmpty(storedDigest) && !storedDigest.equals(argsDigest);
            // MIN-4����ͻͨ���ǿͻ��� bug/̽�⣨���� Expression �������¿��ܿ��⻧ײ�������� warn ������ȫ�źţ��ظ��ǳ�̬����˫�������� 
info
            if (conflict) {
                log.warn("[aroundPointCut][����({}) ����({}) ͬ����γ�ͻ]",
                        joinPoint.getSignature(), LogSanitizeUtils.sanitizeArgs(serializableArgs(joinPoint.getArgs())))
;
            } else {
                log.info("[aroundPointCut][����({}) ����({}) ͬ��ͬ���ظ�]",
                        joinPoint.getSignature(), LogSanitizeUtils.sanitizeArgs(serializableArgs(joinPoint.getArgs())))
;
            }
            String msg = conflict ? CONFLICT_MESSAGE : idempotent.message();
            throw new ServiceException(GlobalErrorCodeConstants.REPEATED_REQUESTS.getCode(), msg);
        }

        // 2. ִ���߼�
        try {
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            // 3. �쳣ʱ��ɾ�� Key
            // �ο����� GTIS ˼·��https://tech.meituan.com/2016/09/29/distributed-system-mutually-exclusive-idempotence-c
erberus-gtis.html
            if (idempotent.deleteKeyWhenException()) {
                idempotentRedisDAO.delete(key);
            }
            throw throwable;
        }
    }

    /**
     * �ų��޷���ȫ���л�����Σ�servlet ���� / ��Ӧ�Ȼ�����ʩ���󣩣��ٽ��� {@link LogSanitizeUtils#sanitizeArgs} ����ժҪ / ������־
��
     *
     * ������codex r0 P1����SEC-011.A �Ѳ���ժҪ�ӡ����ܾ���֧������Ϊ��ÿ������㡹�����״η��У���
     * {@link LogSanitizeUtils} �ڲ��� Jackson {@code valueToTree} ���л���Σ�����ö���ȫ�� getter��
     * �� {@code HttpServletResponse} �ᴥ�� {@code getWriter()}����ǰѡ����Ӧ�ַ����ģʽ��
     * ���º��� {@code ServletUtils.writeAttachment} �ȶ���������� {@link IllegalStateException}��
     *
     * �ų��ھ���codex r1 P1 ���������������͡����ǡ�����ǰ׺���ж���������ʱʵ����
     * ��Tomcat {@code org.apache.catalina.connector.ResponseFacade}������ {@code HttpServlet*Wrapper}�������� servlet 
������ͷ��
     * ��������ǰ׺��{@link cn.zszj.framework.common.util.string.StrUtils#joinMethodArgs} ���ÿھ�����©�����ǣ�
     * {@code instanceof} ������������ʵ�����װ��������������ǰ׺�����ף��ų� {@code MultipartFile} ������ spring-web ������ʩ
����
     *
     * @param args ԭʼ�������
     * @return �޳� servlet / spring-web ������ʩ��������Σ�����ԭ�����˳��null Ԫ�ر�����
     */
    private static Object[] serializableArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return args;
        }
        List<Object> kept = new ArrayList<>(args.length);
        for (Object arg : args) {
            if (arg == null) {
                kept.add(null);
                continue;
            }
            // �������ų���������������ʵ�֣�Tomcat ResponseFacade �ȣ����װ����HttpServlet*Wrapper��������ʱ�������� servlet
 ����Ҳ������
            if (arg instanceof Servlet || arg instanceof ServletRequest || arg instanceof ServletResponse) {
                continue;
            }
            // ���װ�����ǰ׺�ų������಻�� Jackson ���л��� spring-web ������ʩ������ MultipartFile�����ھ��� StrUtils.joinMeth
odArgs һ��
            String clazzName = arg.getClass().getName();
            if (StrUtil.startWithAny(clazzName, "javax.servlet", "jakarta.servlet", "org.springframework.web")) {
                continue;
            }
            kept.add(arg);
        }
        return kept.toArray();
    }

}
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>cn.zszj</groupId>
        <artifactId>zszj-framework</artifactId>
        <version>${revision}</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>
    <artifactId>zszj-spring-boot-starter-protection</artifactId>
    <packaging>jar</packaging>

    <name>${project.artifactId}</name>
    <description>����֤���ṩ�ֲ�ʽ�����ݵȡ��������۶ϡ�API ǩ���ȵȹ���</description>
    <url>https://github.com/YunaiV/ruoyi-vue-pro</url>

    <dependencies>
        <!-- Web ��� -->
        <dependency>
            <groupId>cn.zszj</groupId>
            <artifactId>zszj-spring-boot-starter-web</artifactId>
            <scope>provided</scope> <!-- ����Ϊ provided��ֻ���������ݵ�ʹ�õ� -->
        </dependency>

        <!-- DB ��� -->
        <dependency>
            <groupId>cn.zszj</groupId>
            <artifactId>zszj-spring-boot-starter-redis</artifactId>
        </dependency>

        <!-- ��������� -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>lock4j-redisson-spring-boot-starter</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Test ������� -->
        <dependency>
            <groupId>cn.zszj</groupId>
            <artifactId>zszj-spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

</project>
services/zhongshu-core/pom.xml:44:        <java.version>17</java.version>
services/zhongshu-core/pom.xml:45:        <maven.compiler.source>${java.version}</maven.compiler.source>
services/zhongshu-core/pom.xml:46:        <maven.compiler.target>${java.version}</maven.compiler.target>
services/zhongshu-core/pom.xml:47:        <maven-surefire-plugin.version>3.5.3</maven-surefire-plugin.version>
services/zhongshu-core/pom.xml:48:        <maven-compiler-plugin.version>3.14.0</maven-compiler-plugin.version>
services/zhongshu-core/pom.xml:50:        <!-- maven-surefire-plugin 暂时无法通过 bom 的依赖读取（兼容老版本 IDEA 2024 及以前版本） -->
services/zhongshu-core/pom.xml:72:                <!-- maven-surefire-plugin 插件，用于运行单元测试。 -->
services/zhongshu-core/pom.xml:76:                    <artifactId>maven-surefire-plugin</artifactId>
services/zhongshu-core/pom.xml:77:                    <version>${maven-surefire-plugin.version}</version>
services/zhongshu-core/pom.xml:79:                <!-- maven-compiler-plugin 插件，解决 spring-boot-configuration-processor 
+ Lombok + MapStruct 组合 -->
services/zhongshu-core/pom.xml:83:                    <artifactId>maven-compiler-plugin</artifactId>
services/zhongshu-core/pom.xml:84:                    <version>${maven-compiler-plugin.version}</version>
services/zhongshu-core/zszj-dependencies\pom.xml:48:        <mockito-inline.version>5.2.0</mockito-inline.version>
services/zhongshu-core/zszj-dependencies\pom.xml:84:        <weixin-java.version>4.8.6-20260825.155844</weixin-java.ver
sion>
services/zhongshu-core/zszj-dependencies\pom.xml:86:        <alipay-sdk-java.version>4.40.978.ALL</alipay-sdk-java.vers
ion>
services/zhongshu-core/zszj-dependencies\pom.xml:405:                <groupId>org.mockito</groupId>
services/zhongshu-core/zszj-dependencies\pom.xml:406:                <artifactId>mockito-inline</artifactId>
services/zhongshu-core/zszj-dependencies\pom.xml:407:                <version>${mockito-inline.version}</version> <!-- 
支持 Mockito 的 final 类与 static 方法的 mock -->
services/zhongshu-core/zszj-dependencies\pom.xml:420:                        <groupId>org.mockito</groupId>
services/zhongshu-core/zszj-dependencies\pom.xml:421:                        <artifactId>mockito-core</artifactId>
services/zhongshu-core/zszj-dependencies\pom.xml:682:                <version>${alipay-sdk-java.version}</version>
services/zhongshu-core/zszj-dependencies\pom.xml:716:                <version>${weixin-java.version}</version>
services/zhongshu-core/zszj-dependencies\pom.xml:721:                <version>${weixin-java.version}</version>
services/zhongshu-core/zszj-dependencies\pom.xml:726:                <version>${weixin-java.version}</version>
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test\pom.xml:37:            <groupId>org.mockito</groupI
d>
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test\pom.xml:38:            <artifactId>mockito-inline</
artifactId>
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test\src\main\java\cn\zszj\framework\test\core\ut\BaseMo
ckitoUnitTest.java:4:import org.mockito.junit.jupiter.MockitoExtension;

Name       Source                               
----       ------                               
python.exe C:\Program Files\Python311\python.exe



exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command '$env:PYTHONIOENCODING='"'utf-8'; @'
from pathlib import Path
p=Path('services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/test/java/cn/zszj/framework/idemp
otent/core/aop/IdempotentAspectTest.java').resolve()
fallback=[]
config=Path.home()/'.codex/config.toml'
try:
 import tomllib
 data=tomllib.loads(config.read_text(encoding='utf-8'))
 fallback=data.get('project_doc_fallback_filenames', [])
 print('Configured fallback instruction files:',fallback)
except Exception as e:
 print('Config check:',str(e))
for d in reversed(p.parents):
 for name in ['AGENTS.override.md','AGENTS.md']+fallback:
  f=d/name
  if f.is_file():
   print('\\nINSTRUCTIONS',f)
   for i,line in enumerate(f.read_text(encoding='utf-8').splitlines(),1):print(f'{i}: {line}')
print('\\nCHANGED TEST CONTEXT')
for i,line in enumerate(p.read_text(encoding='utf-8').splitlines(),1):
 if 308<=i<=365 or i>=413: print(f'{i}: {line}')
for name in ['services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/pom.xml','services/zhongshu-core/zszj
-framework/zszj-common/src/main/java/cn/zszj/framework/common/util/log/LogSanitizeUtils.java']:
 f=Path(name)
 print('\\nFILE',f)
 print(f.read_text(encoding='utf-8'))
'@ | python -" in E:\众墅之家AI赋能平台底座
 succeeded in 36ms:
Configured fallback instruction files: []

CHANGED TEST CONTEXT
308:             assertFalse(ex.getMessage().contains("冲突"),
309:                     "默认解析器路径冲突分支不可达，不应出现冲突文案，实际: " + ex.getMessage());
310:             // 钉死：真实解析器算出的 Key 与脱敏摘要确被用于 setIfAbsent
311:             verify(idempotentRedisDAO).setIfAbsent(eq(expectedKey), eq(argsDigest), eq(5L), eq(TimeUnit.SECONDS));
312:         }
313:     }
314: 
315:     @Test
316:     public void testAroundPointCut_firstRequest_servletArgsExcludedFromDigest() throws Throwable {
317:         // codex r0 P1 + r1 P1 回归：@Idempotent 方法若含 servlet 响应入参，计算「每请求必算」的参数摘要时不得用 Jackson 序列化它——
318:         // 序列化会调用 getWriter()/getOutputStream() 等 getter，提前「选定」响应输出模式，破坏后续二进制输出
319:         //（如 ServletUtils.writeAttachment 附件下载抛 IllegalStateException），且首次放行请求即触发
320:         //（SEC-011.A 把 sanitizeArgs 从「仅拒绝分支」提升为「每请求必算摘要」）。
321:         // r1 关键：运行时实现类（Tomcat org.apache.catalina.connector.ResponseFacade）不以 servlet 包名开头，
322:         //「按包名前缀排除」会漏排它——必须「按类型 instanceof 排除」。故用 ContainerLikeResponse（具体实现 +
323:         // 运行时类名落在业务包 cn.zszj 下 + IS-A ServletResponse）精确复现该特征，接口 mock（ByteBuddy 名以 jakarta.servlet 开头）覆盖不到此运行时场景。
324:         Idempotent idempotent = mock(Idempotent.class);
325:         doReturn(FixedKeyResolver.class).when(idempotent).keyResolver();
326:         when(idempotent.timeout()).thenReturn(5);
327:         when(idempotent.timeUnit()).thenReturn(TimeUnit.SECONDS);
328:         ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
329:         when(joinPoint.proceed()).thenReturn("OK");
330:         // 具体容器响应的 spy（非纯接口 mock）：ContainerLikeResponse 运行时类名落业务包 cn.zszj 下（不以 servlet 包名开头），
331:         // 精确复现 Tomcat ResponseFacade 的「非 servlet 包名 + servlet 类型」特征；用 spy 以便 verify 摘要计算是否触碰 writer/输出流
332:         HttpServletResponse response = spy(new ContainerLikeResponse());
333:         Object businessArg = "orderPayload";
334:         // 入参含 servlet 响应对象 + 业务参数
335:         when(joinPoint.getArgs()).thenReturn(new Object[]{businessArg, response});
336:         when(idempotentRedisDAO.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);
337: 
338:         Object result = idempotentAspect.aroundPointCut(joinPoint, idempotent);
339:         assertEquals("OK", result);
340: 
341:         // C1：摘要应仅由「排除 servlet 对象后」的业务入参计算——含 servlet 的口径与此不同（RED），排除后一致（GREEN）
342:         String expectedDigestExcludingServlet = SecureUtil.md5(LogSanitizeUtils.sanitizeArgs(new Object[]{business
Arg}));
343:         verify(idempotentRedisDAO).setIfAbsent(anyString(), eq(expectedDigestExcludingServlet), eq(5L), eq(TimeUni
t.SECONDS));
344:         // C2（codex r2 P2 修正）：直接钉死摘要计算绝不触碰响应的 writer/输出流。
345:         // Spring 6.2 的 MockHttpServletResponse 中 getWriter()/getOutputStream() 访问标志相互独立（不互斥抛异常），
346:         // 故不能靠「getOutputStream 是否抛异常」间接判定；改用 spy + verify(never) 直接检测 getter 是否被调用。
347:         // 类型排除生效后 response 未进入 sanitizeArgs → Jackson 从不序列化它 → getWriter/getOutputStream 从未被调用。
348:         verify(response, never()).getWriter();
349:         verify(response, never()).getOutputStream();
350:     }
351: 
352:     // ========== Helper methods ==========
353: 
354:     private static Map<String, Object> buildSensitiveArgs() {
355:         Map<String, Object> req = new LinkedHashMap<>();
356:         req.put("username", SAFE_USERNAME);
357:         req.put("password", SECRET_PASSWORD);
358:         req.put("token", SECRET_TOKEN);
359:         Map<String, Object> nested = new LinkedHashMap<>();
360:         nested.put("apiKey", SECRET_APIKEY);
361:         nested.put("note", SAFE_NOTE);
362:         req.put("nested", nested);
363:         req.put("list", List.of(Map.of("secret", SECRET_LIST)));
364:         return req;
365:     }
413:         }
414: 
415:         @Override
416:         public Class getDeclaringType() {
417:             return Object.class;
418:         }
419: 
420:         @Override
421:         public String getDeclaringTypeName() {
422:             return "Object";
423:         }
424:     }
425: 
426:     /**
427:      * 模拟 Tomcat 运行时响应实现（{@code org.apache.catalina.connector.ResponseFacade}）的关键特征：
428:      * 具体实现（非接口 mock）+ 运行时类名落在业务包 {@code cn.zszj} 下（不以任何 servlet 包名开头）+ IS-A {@code ServletResponse}。
429:      * 用于证明「按包名前缀排除」会漏排真实容器响应（RED），只有「按类型 instanceof 排除」能命中（GREEN）。
430:      */
431:     static class ContainerLikeResponse extends MockHttpServletResponse {
432:     }
433: 
434: }

FILE services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-test\pom.xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>cn.zszj</groupId>
        <artifactId>zszj-framework</artifactId>
        <version>${revision}</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>
    <artifactId>zszj-spring-boot-starter-test</artifactId>
    <packaging>jar</packaging>

    <name>${project.artifactId}</name>
    <description>测试组件，用于单元测试、集成测试</description>
    <url>https://github.com/YunaiV/ruoyi-vue-pro</url>

    <dependencies>
        <dependency>
            <groupId>cn.zszj</groupId>
            <artifactId>zszj-common</artifactId>
        </dependency>

        <!-- DB 相关 -->
        <dependency>
            <groupId>cn.zszj</groupId>
            <artifactId>zszj-spring-boot-starter-mybatis</artifactId>
        </dependency>

        <dependency>
            <groupId>cn.zszj</groupId>
            <artifactId>zszj-spring-boot-starter-redis</artifactId>
        </dependency>

        <!-- Test 测试相关 -->
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-inline</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
        </dependency>

        <dependency>
            <groupId>com.h2database</groupId> <!-- 单元测试，我们采用 H2 作为数据库 -->
            <artifactId>h2</artifactId>
        </dependency>

        <dependency>
            <groupId>com.github.fppt</groupId> <!-- 单元测试，我们采用内嵌的 Redis 数据库 -->
            <artifactId>jedis-mock</artifactId>
        </dependency>

        <dependency>
            <groupId>uk.co.jemos.podam</groupId> <!-- 单元测试，随机生成 POJO 类 -->
            <artifactId>podam</artifactId>
        </dependency>
    </dependencies>
</project>


FILE services\zhongshu-core\zszj-framework\zszj-common\src\main\java\cn\zszj\framework\common\util\log\LogSanitizeUtils
.java
package cn.zszj.framework.common.util.log;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.zszj.framework.common.util.json.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 日志脱敏工具类（ZS-SEC-007）
 *
 * 统一访问日志、异常日志与保护切面（幂等 / 限流）四类写点的日志净化能力，核心策略：
 * 1. 键归一：小写并去除下划线 / 连字符后做大小写不敏感匹配，兼容 password / PASSWORD / user_password / access-token 等写法；
 * 2. 命中判定：内置凭据根集（password、token、secret、authorization 等）使用 contains 模糊匹配，端点级 extraKeys 使用归一后精确匹配；
 * 3. 递归脱敏：对象与数组逐层递归，命中敏感键的值统一掩码为 {@code ***}，保留字段名以便审计定位；
 * 4. 失败不回退原文：解析 / 序列化异常时只记录摘要（长度或类型 + 原因类别），绝不输出未净化的原始内容；
 * 5. 体积上限：结果超过 {@link #MAX_LENGTH} 时截断并附总长度，避免超长正文撑爆日志表。
 *
 * 该工具不依赖 servlet / web，可被 web 与 protection 两个 starter 共同复用。
 *
 * @author 众墅之家
 */
@Slf4j
public class LogSanitizeUtils {

    /**
     * 敏感值掩码，保留字段名、隐藏具体值。
     * 公开以便调用方（如签名切面）对单个凭据派生值就地掩码，复用统一掩码标记。
     */
    public static final String MASK = "***";

    /**
     * 单条日志内容的长度上限，超出部分截断
     */
    private static final int MAX_LENGTH = 2048;

    /**
     * 内置凭据敏感键根集（已归一：小写、无下划线 / 连字符），使用 contains 匹配
     */
    private static final Set<String> SENSITIVE_KEY_ROOTS = Set.of(
            "password", "passwd", "pwd", "token", "secret", "authorization",
            "credential", "privatekey", "apikey", "accesskey", "secretkey",
            "cookie", "session", "otp");

    private LogSanitizeUtils() {
    }

    /**
     * 脱敏 JSON 字符串（如请求正文）。
     *
     * @param json      原始 JSON 字符串
     * @param extraKeys 端点级附加敏感键
     * @return 脱敏后的 JSON 字符串；入参为空返回 {@code null}；无法解析时返回摘要（不含原文）
     */
    public static String sanitizeJson(String json, String... extraKeys) {
        if (StrUtil.isEmpty(json)) {
            return null;
        }
        Set<String> extra = normalizeKeys(extraKeys);
        try {
            JsonNode node = mapper().readTree(json);
            sanitizeNode(node, extra);
            return truncate(mapper().writeValueAsString(node));
        } catch (Throwable t) {
            return unparseable(json, t);
        }
    }

    /**
     * 脱敏键值映射（如 query 参数）。
     *
     * @param map       原始映射
     * @param extraKeys 端点级附加敏感键
     * @return 脱敏后的 JSON 字符串；入参为空返回 {@code null}
     */
    public static String sanitizeMap(Map<String, ?> map, String... extraKeys) {
        if (CollUtil.isEmpty(map)) {
            return null;
        }
        return sanitizeObject(map, extraKeys);
    }

    /**
     * 脱敏方法入参数组（如幂等 / 限流切面的 joinPoint.getArgs()）。
     *
     * @param args      原始入参数组
     * @param extraKeys 端点级附加敏感键
     * @return 脱敏后的形如 {@code [arg1, arg2]} 的字符串
     */
    public static String sanitizeArgs(Object[] args, String... extraKeys) {
        if (ArrayUtil.isEmpty(args)) {
            return "[]";
        }
        Set<String> extra = normalizeKeys(extraKeys);
        List<String> parts = new ArrayList<>(args.length);
        for (Object arg : args) {
            parts.add(sanitizeArgValue(arg, extra));
        }
        return truncate("[" + String.join(", ", parts) + "]");
    }

    /**
     * 脱敏响应体（如 CommonResult），仅净化 data 字段，保留 code / msg 以便审计定位。
     *
     * @param result    响应对象
     * @param extraKeys 端点级附加敏感键
     * @return 脱敏后的 JSON 字符串；入参为 {@code null} 返回 {@code null}
     */
    public static String sanitizeResponseBody(Object result, String... extraKeys) {
        if (result == null) {
            return null;
        }
        Set<String> extra = normalizeKeys(extraKeys);
        try {
            JsonNode node = mapper().valueToTree(result);
            JsonNode data = node.get("data");
            if (data != null) {
                sanitizeNode(data, extra);
            }
            return truncate(mapper().writeValueAsString(node));
        } catch (Throwable t) {
            return unserializable(result, t);
        }
    }

    private static String sanitizeObject(Object obj, String... extraKeys) {
        Set<String> extra = normalizeKeys(extraKeys);
        try {
            JsonNode node = mapper().valueToTree(obj);
            sanitizeNode(node, extra);
            return truncate(mapper().writeValueAsString(node));
        } catch (Throwable t) {
            return unserializable(obj, t);
        }
    }

    private static String sanitizeArgValue(Object arg, Set<String> extra) {
        if (arg == null) {
            return "null";
        }
        try {
            JsonNode node = mapper().valueToTree(arg);
            sanitizeNode(node, extra);
            return mapper().writeValueAsString(node);
        } catch (Throwable t) {
            return "<" + arg.getClass().getSimpleName() + ">";
        }
    }

    /**
     * 递归脱敏 JSON 节点：数组逐元素递归，对象命中敏感键则掩码、否则递归其值
     */
    private static void sanitizeNode(JsonNode node, Set<String> extra) {
        if (node == null) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                sanitizeNode(child, extra);
            }
            return;
        }
        if (!node.isObject()) {
            return;
        }
        ObjectNode objectNode = (ObjectNode) node;
        List<String> sensitiveKeys = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> iterator = objectNode.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            if (isSensitiveKey(entry.getKey(), extra)) {
                sensitiveKeys.add(entry.getKey());
            } else {
                sanitizeNode(entry.getValue(), extra);
            }
        }
        for (String key : sensitiveKeys) {
            objectNode.put(key, MASK);
        }
    }

    private static boolean isSensitiveKey(String key, Set<String> extra) {
        if (StrUtil.isEmpty(key)) {
            return false;
        }
        String normalized = normalizeKey(key);
        if (extra.contains(normalized)) {
            return true;
        }
        for (String root : SENSITIVE_KEY_ROOTS) {
            if (normalized.contains(root)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> normalizeKeys(String[] keys) {
        if (ArrayUtil.isEmpty(keys)) {
            return Set.of();
        }
        return Arrays.stream(keys)
                .filter(StrUtil::isNotEmpty)
                .map(LogSanitizeUtils::normalizeKey)
                .collect(Collectors.toSet());
    }

    private static String normalizeKey(String key) {
        return key.toLowerCase().replace("_", "").replace("-", "");
    }

    private static String truncate(String text) {
        if (text == null || text.length() <= MAX_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_LENGTH) + "...[truncated,total=" + text.length() + "]";
    }

    private static String unparseable(String raw, Throwable t) {
        int len = raw == null ? 0 : raw.length();
        String cause = t.getClass().getSimpleName();
        log.warn("[sanitizeJson][无法解析正文，已降级为摘要 len={} cause={}]", len, cause);
        return "<unparseable:len=" + len + ",cause=" + cause + ">";
    }

    private static String unserializable(Object obj, Throwable t) {
        String type = obj == null ? "null" : obj.getClass().getSimpleName();
        String cause = t.getClass().getSimpleName();
        log.warn("[sanitize][无法序列化对象，已降级为摘要 type={} cause={}]", type, cause);
        return "<unserializable:" + type + ">";
    }

    private static ObjectMapper mapper() {
        return JsonUtils.getObjectMapper();
    }

}


codex
The change strengthens the regression test by directly verifying that neither response getter is called, while preservi
ng the servlet-exclusion scenario. No introduced defects were found. Tests were not run in the read-only environment.
The change strengthens the regression test by directly verifying that neither response getter is called, while preserving the servlet-exclusion scenario. No introduced defects were found. Tests were not run in the read-only environment.
