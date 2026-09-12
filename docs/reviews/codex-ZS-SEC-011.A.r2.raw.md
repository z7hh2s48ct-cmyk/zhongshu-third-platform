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
session id: 01a0943d-dbf0-7ac0-a1da-bc6d5005a9e0
--------
user
commit d02f812
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'Get-Location; git status --short; git rev-pa
rse --show-toplevel; git show --stat --oneline --decorate --no-renames d02f8129; Get-ChildItem -Force -Name' in E:\众墅之家
AI赋能平台底座
 succeeded in 87ms:
warning: unable to access 'C:\Users\Administrator/.config/git/ignore': Permission denied
warning: unable to access 'C:\Users\Administrator/.config/git/ignore': Permission denied
?? ".omx/plans/2026-09-11-B03-Wave1-\345\205\255\351\241\271\345\271\266\350\241\214\345\274\200\345\217\221\350\256\24
1\345\210\222.md"
?? _zs_sec011a_codex.ps1
?? _zs_sec011a_codex_r1.ps1
?? _zs_sec011a_codex_r2.ps1
?? _zs_sec011a_fixmsg.txt
?? _zs_sec011a_fixmsg_r1.txt
?? _zs_sec011a_prot.txt
?? _zs_sec011a_tdd.ps1
?? _zs_sec011a_tdd.txt
?? _zs_sec011a_verify.ps1
?? docs/reviews/codex-ZS-SEC-011.A.r1.raw.md
?? docs/reviews/codex-ZS-SEC-011.A.raw.md
?? outputs/
E:/众墅之家AI赋能平台底座
d02f8129 (HEAD -> main) fix(sec-011-a): 幂等摘要按类型排除 servlet 响应，覆盖 Tomcat ResponseFacade [ZS-SEC-011.A]
 .../idempotent/core/aop/IdempotentAspect.java      | 20 +++++++++---
 .../idempotent/core/aop/IdempotentAspectTest.java  | 36 +++++++++++++++-------
 2 files changed, 40 insertions(+), 16 deletions(-)
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
_zs_sec011a_fixmsg.txt
_zs_sec011a_fixmsg_r1.txt
_zs_sec011a_prot.txt
_zs_sec011a_tdd.ps1
_zs_sec011a_tdd.txt
_zs_sec011a_verify.ps1



exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command '$OutputEncoding = [Console]::OutputEncoding 
= [System.Text.UTF8Encoding]::new(); git show --format=fuller --no-ext-diff --no-renames d02f8129; Get-ChildItem -Path 
. -Recurse -Force -File -Include AGENTS.md,AGENTS.override.md -ErrorAction SilentlyContinue | Select-Object -ExpandProp
erty FullName; Get-ChildItem E:'"\\ -Force -File -Filter AGENTS* -ErrorAction SilentlyContinue | Select-Object -ExpandP
roperty FullName" in E:\众墅之家AI赋能平台底座
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command '$paths = @('"'E:\\AGENTS.override.md','E:\\A
GENTS.md','AGENTS.override.md','AGENTS.md','services\\AGENTS.override.md','services\\AGENTS.md','services\\zhongshu-cor
e\\AGENTS.override.md','services\\zhongshu-core\\AGENTS.md','services\\zhongshu-core\\zszj-framework\\AGENTS.override.m
d','services\\zhongshu-core\\zszj-framework\\AGENTS.md'); foreach ("'$p in $paths) { if (Test-Path -LiteralPath $p) { W
rite-Output "--- $p"; Get-Content -LiteralPath $p -Encoding UTF8 } }; git ls-files '"'*AGENTS*' '*codex*' '*CLAUDE*'; G
et-Content -LiteralPath 'services\\zhongshu-core\\zszj-framework\\zszj-spring-boot-starter-protection\\pom.xml' -Encodi
ng UTF8; Get-Content -LiteralPath 'services\\zhongshu-core\\zszj-framework\\zszj-spring-boot-starter-protection\\src\\m
ain\\java\\cn\\zszj\\framework\\idempotent\\core\\aop\\IdempotentAspect.java' -Encoding UTF8" in E:\众墅之家AI赋能平台底座
 succeeded in 18ms:
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

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "Get-ChildItem -LiteralPath 'services\\zhongs
hu-core\\zszj-framework\\zszj-spring-boot-starter-protection' -Recurse -Force -File -Include AGENTS.md,AGENTS.override.
md -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName; git show d02f8129:services/zhongshu-core/zsz
j-framework/zszj-spring-boot-starter-protection/src/test/java/cn/zszj/framework/idempotent/core/aop/IdempotentAspectTes
t.java; git show d02f8129:services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zs
zj/framework/idempotent/config/ZszjIdempotentConfiguration.java; git show d02f8129:services/zhongshu-core/zszj-framewor
k/zszj-spring-boot-starter-web/pom.xml" in E:\众墅之家AI赋能平台底座
 succeeded in 71ms:
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\maven-status\ma
ven-compiler-plugin\compile\default-compile\createdFiles.lst
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\maven-status\ma
ven-compiler-plugin\compile\default-compile\inputFiles.lst
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\maven-status\ma
ven-compiler-plugin\testCompile\default-testCompile\createdFiles.lst
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\maven-status\ma
ven-compiler-plugin\testCompile\default-testCompile\inputFiles.lst
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\2026-09-10T11-01-51_970.dumpstream
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\2026-09-10T11-28-18_669.dumpstream
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\2026-09-10T12-02-07_387.dumpstream
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\2026-09-11T10-32-54_077.dumpstream
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\2026-09-11T10-40-04_187.dumpstream
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\2026-09-11T12-01-12_768.dumpstream
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\2026-09-12T12-21-02_632.dumpstream
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\2026-09-12T12-45-49_289.dumpstream
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\2026-09-12T12-47-27_496.dumpstream
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\2026-09-12T12-47-51_439.dumpstream
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\2026-09-12T13-20-36_092.dumpstream
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\2026-09-12T13-48-06_988.dumpstream
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\2026-09-12T13-51-22_032.dumpstream
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\2026-09-12T14-07-15_254.dumpstream
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\2026-09-12T14-07-58_602.dumpstream
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\2026-09-12T14-09-24_962.dumpstream
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\cn.zszj.framework.idempotent.core.aop.IdempotentAspectTest.txt
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\cn.zszj.framework.idempotent.core.keyresolver.impl.DefaultIdempotentKeyResolverTest.txt
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\cn.zszj.framework.ratelimiter.core.aop.RateLimiterAspectTest.txt
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\cn.zszj.framework.ratelimiter.core.keyresolver.impl.ExpressionRateLimiterKeyResolverTest.txt
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\cn.zszj.framework.signature.core.aop.ApiSignatureAspectTest.txt
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\cn.zszj.framework.signature.core.ApiSignatureTest.txt
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\TEST-cn.zszj.framework.idempotent.core.aop.IdempotentAspectTest.xml
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\TEST-cn.zszj.framework.idempotent.core.keyresolver.impl.DefaultIdempotentKeyResolverTest.xml
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\TEST-cn.zszj.framework.ratelimiter.core.aop.RateLimiterAspectTest.xml
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\TEST-cn.zszj.framework.ratelimiter.core.keyresolver.impl.ExpressionRateLimiterKeyResolverTest.xml
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\TEST-cn.zszj.framework.signature.core.aop.ApiSignatureAspectTest.xml
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\TEST-cn.zszj.framework.signature.core.ApiSignatureTest.xml
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\test-classes\cn
\zszj\framework\idempotent\core\aop\IdempotentAspectTest$ContainerLikeResponse.class
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\test-classes\cn
\zszj\framework\idempotent\core\aop\IdempotentAspectTest$FixedKeyResolver.class
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\test-classes\cn
\zszj\framework\idempotent\core\aop\IdempotentAspectTest$FixedSignature.class
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\test-classes\cn
\zszj\framework\idempotent\core\aop\IdempotentAspectTest.class
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\test-classes\cn
\zszj\framework\idempotent\core\keyresolver\impl\DefaultIdempotentKeyResolverTest$FixedSignature.class
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\test-classes\cn
\zszj\framework\idempotent\core\keyresolver\impl\DefaultIdempotentKeyResolverTest.class
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\test-classes\cn
\zszj\framework\ratelimiter\core\aop\RateLimiterAspectTest$FixedKeyResolver.class
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\test-classes\cn
\zszj\framework\ratelimiter\core\aop\RateLimiterAspectTest$FixedSignature.class
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\test-classes\cn
\zszj\framework\ratelimiter\core\aop\RateLimiterAspectTest.class
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\test-classes\cn
\zszj\framework\ratelimiter\core\keyresolver\impl\ExpressionRateLimiterKeyResolverTest$FixedMethodSignature.class
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\test-classes\cn
\zszj\framework\ratelimiter\core\keyresolver\impl\ExpressionRateLimiterKeyResolverTest$Fixture.class
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\test-classes\cn
\zszj\framework\ratelimiter\core\keyresolver\impl\ExpressionRateLimiterKeyResolverTest.class
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\test-classes\cn
\zszj\framework\signature\core\ApiSignatureTest.class
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\test-classes\cn
\zszj\framework\signature\core\aop\ApiSignatureAspectTest$FixedSignature.class
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\test-classes\cn
\zszj\framework\signature\core\aop\ApiSignatureAspectTest.class
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
 * IMP-6：摘要输入统一走 sanitizeArgs 脱敏口径，测试期望 digest 亦按此口径重算。
 *
 * ZS-SEC-011.A 合同测试边界：本测试类验证「窗口锁短时防重」合同——
 * 同键同参重复拒绝、同键异参冲突拒绝、主体/租户隔离。
 * 显式声明：窗口锁不承诺持久化幂等（重启/超时/缓存故障后不保证返回原业务结果），
 * 持久化幂等归 ZS-SEC-011.B。本批不对持久化结果做断言。
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

        // C1（IMP-6 验证）：钉死传给 DAO 的摘要 = MD5(脱敏后入参)，而非 MD5(原文)
        String expectedDigest = SecureUtil.md5(LogSanitizeUtils.sanitizeArgs(sensitiveArgs));
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
        // IMP-6：期望 digest 按 sanitizeArgs 脱敏口径重算
        String argsDigestB = SecureUtil.md5(LogSanitizeUtils.sanitizeArgs(argsB));
        String storedDigestA = SecureUtil.md5(LogSanitizeUtils.sanitizeArgs(new Object[]{"argA"}));
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
        // IMP-6：期望 digest 按 sanitizeArgs 脱敏口径重算
        String argsDigestA = SecureUtil.md5(LogSanitizeUtils.sanitizeArgs(argsA));
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
        // 默认解析器把 argsStr 烘入 Key → 同 Key 必然同参 → 切面 conflict 分支恒不可达。
        // 构造同 Key 场景（同 method/tenant/user/type/args），把"默认路径冲突不可达"钉成可执行文档。
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
                List.of(new DefaultIdempotentKeyResolver()), idempotentRedisDAO);

        try (MockedStatic<ServletUtils> servletMs = mockStatic(ServletUtils.class);
             MockedStatic<WebFrameworkUtils> webMs = mockStatic(WebFrameworkUtils.class)) {
            servletMs.when(ServletUtils::getRequest).thenReturn(request);
            webMs.when(() -> WebFrameworkUtils.getTenantId(request)).thenReturn(1L);
            webMs.when(() -> WebFrameworkUtils.getLoginUserId(request)).thenReturn(100L);
            webMs.when(() -> WebFrameworkUtils.getLoginUserType(request)).thenReturn(2);

            // 真实解析器算出的 Key（固定主体上下文 → 唯一确定），与切面内部解析结果必然一致
            String expectedKey = new DefaultIdempotentKeyResolver().resolver(joinPoint, idempotent);
            String argsDigest = SecureUtil.md5(LogSanitizeUtils.sanitizeArgs(args));
            // 同 Key 已存在 → setIfAbsent 返回 false；默认路径 storedDigest 必等于 argsDigest
            when(idempotentRedisDAO.setIfAbsent(eq(expectedKey), eq(argsDigest), anyLong(), any())).thenReturn(false);
            when(idempotentRedisDAO.getDigest(eq(expectedKey))).thenReturn(argsDigest);

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> realAspect.aroundPointCut(joinPoint, idempotent));
            assertEquals(GlobalErrorCodeConstants.REPEATED_REQUESTS.getCode(), ex.getCode());
            // conflict 恒 false → message 为重复文案，非冲突文案（默认解析器路径冲突分支不可达）
            assertTrue(ex.getMessage().contains("repeated-request"),
                    "默认解析器同 Key 场景 conflict 必为 false，应回落重复文案，实际: " + ex.getMessage());
            assertFalse(ex.getMessage().contains("冲突"),
                    "默认解析器路径冲突分支不可达，不应出现冲突文案，实际: " + ex.getMessage());
            // 钉死：真实解析器算出的 Key 与脱敏摘要确被用于 setIfAbsent
            verify(idempotentRedisDAO).setIfAbsent(eq(expectedKey), eq(argsDigest), eq(5L), eq(TimeUnit.SECONDS));
        }
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
        // 具体容器响应（非接口 mock）：模拟 Tomcat ResponseFacade 的「非 servlet 包名 + servlet 类型」运行时特征
        HttpServletResponse response = new ContainerLikeResponse();
        Object businessArg = "orderPayload";
        // 入参含 servlet 响应对象 + 业务参数
        when(joinPoint.getArgs()).thenReturn(new Object[]{businessArg, response});
        when(idempotentRedisDAO.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);

        Object result = idempotentAspect.aroundPointCut(joinPoint, idempotent);
        assertEquals("OK", result);

        // C1：摘要应仅由「排除 servlet 对象后」的业务入参计算——含 servlet 的口径与此不同（RED），排除后一致（GREEN）
        String expectedDigestExcludingServlet = SecureUtil.md5(LogSanitizeUtils.sanitizeArgs(new Object[]{businessArg})
);
        verify(idempotentRedisDAO).setIfAbsent(anyString(), eq(expectedDigestExcludingServlet), eq(5L), eq(TimeUnit.SEC
ONDS));
        // C2：端到端复现生产症状——摘要计算后业务仍能安全走二进制输出。若摘要序列化提前调用了 getWriter()
        //（前缀漏排 Tomcat 式实现时会发生），MockHttpServletResponse 置 usingWriter，此处 getOutputStream() 将抛
        // IllegalStateException（正是 ServletUtils.writeAttachment 的生产故障）；类型排除生效后 response 未被触碰，故不抛。
        assertDoesNotThrow(response::getOutputStream,
                "摘要计算不得提前选定 response 的 writer，否则后续二进制输出（writeAttachment）会抛 IllegalStateException");
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
package cn.zszj.framework.idempotent.config;

import cn.zszj.framework.idempotent.core.aop.IdempotentAspect;
import cn.zszj.framework.idempotent.core.keyresolver.impl.DefaultIdempotentKeyResolver;
import cn.zszj.framework.idempotent.core.keyresolver.impl.ExpressionIdempotentKeyResolver;
import cn.zszj.framework.idempotent.core.keyresolver.IdempotentKeyResolver;
import cn.zszj.framework.idempotent.core.keyresolver.impl.UserIdempotentKeyResolver;
import cn.zszj.framework.idempotent.core.redis.IdempotentRedisDAO;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import cn.zszj.framework.redis.config.ZszjRedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

@AutoConfiguration(after = ZszjRedisAutoConfiguration.class)
public class ZszjIdempotentConfiguration {

    @Bean
    public IdempotentAspect idempotentAspect(List<IdempotentKeyResolver> keyResolvers, IdempotentRedisDAO idempotentRed
isDAO) {
        return new IdempotentAspect(keyResolvers, idempotentRedisDAO);
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
    <artifactId>zszj-spring-boot-starter-web</artifactId>
    <packaging>jar</packaging>

    <name>${project.artifactId}</name>
    <description>Web 框架，全局异常、API 日志、脱敏、错误码等</description>
    <url>https://github.com/YunaiV/ruoyi-vue-pro</url>

    <dependencies>
        <dependency>
            <groupId>cn.zszj</groupId>
            <artifactId>zszj-common</artifactId>
        </dependency>

        <!-- Web 相关 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <!-- spring boot 配置所需依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.aspectj</groupId>
            <artifactId>aspectjweaver</artifactId>
            <scope>provided</scope> <!-- 解决工具类 SpringExpressionUtils 加载的时候访问不到 org.aspectj.lang.JoinPoint 问题 -->
        </dependency>

        <dependency>
            <groupId>com.github.xiaoymin</groupId>
            <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-core</artifactId>
            <scope>provided</scope> <!-- 设置为 provided，主要是 GlobalExceptionHandler 使用 -->
        </dependency>

        <!-- 工具类相关 -->
        <dependency>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
            <scope>provided</scope> <!-- 设置为 provided，只有工具类需要使用到 -->
        </dependency>

        <dependency>
            <groupId>org.jsoup</groupId>
            <artifactId>jsoup</artifactId>
        </dependency>

        <!-- Test 测试相关 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-inline</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

</project>

 succeeded in 40723ms:
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command '$changed = git diff-tree --no-commit-id --na
me-only -r d02f8129; $dirs = @('"'.'); foreach ("'$file in $changed) { $dir = Split-Path -Parent $file; while ($dir) { 
$dirs += $dir; $dir = Split-Path -Parent $dir } }; foreach ($dir in ($dirs | Sort-Object -Unique)) { foreach ($name in 
@('"'AGENTS.override.md','AGENTS.md')) { "'$p = Join-Path $dir $name; if (Test-Path -LiteralPath $p) { Write-Output "--
- $p"; Get-Content -LiteralPath $p -Encoding UTF8 } } }; git grep -n '"'@Idempotent' d02f8129 -- services/zhongshu-core
 ':"'!**/target/**'"'; git show d02f8129:services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framew
ork/common/util/log/LogSanitizeUtils.java; git show d02f8129:services/zhongshu-core/zszj-framework/zszj-common/src/main
/java/cn/zszj/framework/common/util/string/StrUtils.java; git log -5 --oneline" in E:\众墅之家AI赋能平台底座
 succeeded in 239ms:
d02f8129:services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/idem
potent/package-info.java:8: *  1. 我们去掉了 @Idempotent 注解的 delKey 属性。原因是，本质上 delKey 为 true 时，实现的是分布式锁的能力
d02f8129:services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/test/java/cn/zszj/framework/idem
potent/core/aop/IdempotentAspectTest.java:316:        // codex r0 P1 + r1 P1 回归：@Idempotent 方法若含 servlet 响应入参，计算「每请求必算」
的参数摘要时不得用 Jackson 序列化它——
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
package cn.zszj.framework.common.util.string;

import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.pinyin.PinyinUtil;
import org.aspectj.lang.JoinPoint;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 字符串工具类
 *
 * @author 芋道源码
 */
public class StrUtils {

    public static String maxLength(CharSequence str, int maxLength) {
        return StrUtil.maxLength(str, maxLength - 3); // -3 的原因，是该方法会补充 ... 恰好
    }

    /**
     * 给定字符串是否以任何一个字符串开始
     * 给定字符串和数组为空都返回 false
     *
     * @param str      给定字符串
     * @param prefixes 需要检测的开始字符串
     * @since 3.0.6
     */
    public static boolean startWithAny(String str, Collection<String> prefixes) {
        if (StrUtil.isEmpty(str) || ArrayUtil.isEmpty(prefixes)) {
            return false;
        }

        for (CharSequence suffix : prefixes) {
            if (StrUtil.startWith(str, suffix, false)) {
                return true;
            }
        }
        return false;
    }

    public static List<Long> splitToLong(String value, CharSequence separator) {
        long[] longs = StrUtil.splitToLong(value, separator);
        return Arrays.stream(longs).boxed().collect(Collectors.toList());
    }

    public static Set<Long> splitToLongSet(String value) {
        return splitToLongSet(value, StrPool.COMMA);
    }

    public static Set<Long> splitToLongSet(String value, CharSequence separator) {
        long[] longs = StrUtil.splitToLong(value, separator);
        return Arrays.stream(longs).boxed().collect(Collectors.toSet());
    }

    public static List<Integer> splitToInteger(String value, CharSequence separator) {
        int[] integers = StrUtil.splitToInt(value, separator);
        return Arrays.stream(integers).boxed().collect(Collectors.toList());
    }

    /**
     * 移除字符串中，包含指定字符串的行
     *
     * @param content 字符串
     * @param sequence 包含的字符串
     * @return 移除后的字符串
     */
    public static String removeLineContains(String content, String sequence) {
        if (StrUtil.isEmpty(content) || StrUtil.isEmpty(sequence)) {
            return content;
        }
        return Arrays.stream(content.split("\n"))
                .filter(line -> !line.contains(sequence))
                .collect(Collectors.joining("\n"));
    }

    /**
     * 转小写拼音，字之间以空格分隔，便于调用方按需拼接 / 取首字母 / 拼音搜索
     *
     * 例：「老张」→ "lao zhang"、「ZhangSan」→ "zhangsan"
     * 英文 / 数字 / 符号原样返回，空值返回 null
     *
     * 注意：底层依赖 hutool-extra 的 {@link PinyinUtil}，需要业务模块自行引入拼音引擎依赖
     * （pinyin4j / TinyPinyin / Bopomofo4j 任选其一），否则运行时会抛 NoClassDefFoundError
     *
     * @param str 字符串
     * @return 拼音串（保留空格分隔）
     */
    public static String toPinyin(String str) {
        if (StrUtil.isBlank(str)) {
            return null;
        }
        return PinyinUtil.getPinyin(str);
    }

    /**
     * 拼接方法的参数
     *
     * 特殊：排除一些无法序列化的参数，如 ServletRequest、ServletResponse、MultipartFile
     *
     * @param joinPoint 连接点
     * @return 拼接后的参数
     */
    public static String joinMethodArgs(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (ArrayUtil.isEmpty(args)) {
            return "";
        }
        return ArrayUtil.join(args, ",", item -> {
            if (item == null) {
                return "";
            }
            // 讨论可见：https://t.zsxq.com/XUJVk、https://t.zsxq.com/MnKcL
            String clazzName = item.getClass().getName();
            if (StrUtil.startWithAny(clazzName, "javax.servlet", "jakarta.servlet", "org.springframework.web")) {
                return "";
            }
            return item;
        });
    }

}
d02f8129 fix(sec-011-a): 幂等摘要按类型排除 servlet 响应，覆盖 Tomcat ResponseFacade [ZS-SEC-011.A]
b62af7d3 fix(sec-011-a): 幂等摘要排除 servlet 入参，堵首次放行即序列化响应破坏二进制输出 [ZS-SEC-011.A]
957cbc95 Merge branch 'feat/sec-011-a'
1bc373e1 docs(sec-010): ZS-SEC-010 收口文档同步 + codex r0→r1 两弧评审处置入库 + docs/05 V1.25
0eba03aa fix(sec-010): 限流拒绝日志参数名感知脱敏，堵标量 refreshToken 与短信 code 明文泄露 [ZS-SEC-010]

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "git show d02f8129:services/zhongshu-core/zsz
j-framework/zszj-spring-boot-starter-protection/src/test/java/cn/zszj/framework/idempotent/core/aop/IdempotentAspectTes
t.java | Select-Object -First 150; git grep -n 'jakarta.servlet' d02f8129 -- services/zhongshu-core/zszj-framework/zszj
-spring-boot-starter-protection/src/main; git show d02f8129:services/zhongshu-core/zszj-framework/zszj-spring-boot-star
ter-test/pom.xml; git diff d02f8129"'^ d02f8129 --check; Get-ChildItem -LiteralPath '"'services\\zhongshu-core\\zszj-fr
amework\\zszj-spring-boot-starter-protection\\target\\surefire-reports' -Filter '*IdempotentAspectTest.txt' | Get-Conte
nt" in E:\众墅之家AI赋能平台底座
 succeeded in 76ms:
d02f8129:services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/idem
potent/core/aop/IdempotentAspect.java:12:import jakarta.servlet.Servlet;
d02f8129:services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/idem
potent/core/aop/IdempotentAspect.java:13:import jakarta.servlet.ServletRequest;
d02f8129:services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/idem
potent/core/aop/IdempotentAspect.java:14:import jakarta.servlet.ServletResponse;
d02f8129:services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/idem
potent/core/aop/IdempotentAspect.java:158:            if (StrUtil.startWithAny(clazzName, "javax.servlet", "jakarta.ser
vlet", "org.springframework.web")) {
d02f8129:services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/idem
potent/core/keyresolver/impl/DefaultIdempotentKeyResolver.java:11:import jakarta.servlet.http.HttpServletRequest;
d02f8129:services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/rate
limiter/core/keyresolver/impl/ExpressionRateLimiterKeyResolver.java:9:import jakarta.servlet.http.HttpServletRequest;
d02f8129:services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/sign
ature/core/aop/ApiSignatureAspect.java:15:import jakarta.servlet.http.HttpServletRequest;
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
-------------------------------------------------------------------------------
Test set: cn.zszj.framework.idempotent.core.aop.IdempotentAspectTest
-------------------------------------------------------------------------------
Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.247 s -- in cn.zszj.framework.idempotent.core.aop.Ide
mpotentAspectTest

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "Get-Command java,javap -ErrorAction Silently
Continue | Select-Object Name,Source; git grep -n -e 'spring-boot.version' -e 'spring.version' -- services/zhongshu-cor
e/zszj-dependencies/pom.xml services/zhongshu-core/pom.xml; "'$report = '"'services\\zhongshu-core\\zszj-framework\\zsz
j-spring-boot-starter-protection\\target\\surefire-reports\\TEST-cn.zszj.framework.idempotent.core.aop.IdempotentAspect
Test.xml'; "'$line = Get-Content -LiteralPath $report | Where-Object { $_ -like '"'*name=\"java.class.path\"*' }; "'$li
ne -split '"';' | Where-Object { "'$_ -match '"'spring-test|tomcat-embed-core' }; Get-ChildItem -LiteralPath 'tools' -N
ame" in E:\众墅之家AI赋能平台底座
 succeeded in 28ms:
C:\Users\Administrator\.m2\repository\org\apache\tomcat\embed\tomcat-embed-core\10.1.55\tomcat-embed-core-10.1.55.jar
C:\Users\Administrator\.m2\repository\org\springframework\spring-test\6.2.19\spring-test-6.2.19.jar
apache-maven-3.9.9
jdk-17.0.20.1+1
env.sh
jdk17.zip
maven.zip

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "& 'tools\\jdk-17.0.20.1+1\\bin\\javap.exe' -
classpath 'C:\\Users\\Administrator\\.m2\\repository\\org\\springframework\\spring-test\\6.2.19\\spring-test-6.2.19.jar
' -c -private org.springframework.mock.web.MockHttpServletResponse" in E:\众墅之家AI赋能平台底座
 exited 1 in 171ms:
Compiled from "MockHttpServletResponse.java"
public class org.springframework.mock.web.MockHttpServletResponse implements jakarta.servlet.http.HttpServletResponse {
  private static final java.lang.String CHARSET_PREFIX;

  private static final java.lang.String DATE_FORMAT;

  private static final java.util.TimeZone GMT;

  private static final org.springframework.http.MediaType APPLICATION_PLUS_JSON;

  private boolean outputStreamAccessAllowed;

  private boolean writerAccessAllowed;

  private java.lang.String defaultCharacterEncoding;

  private java.lang.String characterEncoding;

  private boolean characterEncodingSet;

  private final java.io.ByteArrayOutputStream content;

  private final jakarta.servlet.ServletOutputStream outputStream;

  private java.io.PrintWriter writer;

  private long contentLength;

  private java.lang.String contentType;

  private int bufferSize;

  private boolean committed;

  private java.util.Locale locale;

  private final java.util.List<jakarta.servlet.http.Cookie> cookies;

  private final java.util.Map<java.lang.String, org.springframework.mock.web.HeaderValueHolder> headers;

  private int status;

  private java.lang.String errorMessage;

  private java.lang.String forwardedUrl;

  private final java.util.List<java.lang.String> includedUrls;

  public org.springframework.mock.web.MockHttpServletResponse();
    Code:
       0: aload_0
       1: invokespecial #1                  // Method java/lang/Object."<init>":()V
       4: aload_0
       5: iconst_1
       6: putfield      #7                  // Field outputStreamAccessAllowed:Z
       9: aload_0
      10: iconst_1
      11: putfield      #13                 // Field writerAccessAllowed:Z
      14: aload_0
      15: ldc           #18                 // String ISO-8859-1
      17: putfield      #20                 // Field defaultCharacterEncoding:Ljava/lang/String;
      20: aload_0
      21: aload_0
      22: getfield      #20                 // Field defaultCharacterEncoding:Ljava/lang/String;
      25: putfield      #24                 // Field characterEncoding:Ljava/lang/String;
      28: aload_0
      29: iconst_0
      30: putfield      #27                 // Field characterEncodingSet:Z
      33: aload_0
      34: new           #30                 // class java/io/ByteArrayOutputStream
      37: dup
      38: sipush        1024
      41: invokespecial #32                 // Method java/io/ByteArrayOutputStream."<init>":(I)V
      44: putfield      #35                 // Field content:Ljava/io/ByteArrayOutputStream;
      47: aload_0
      48: new           #39                 // class org/springframework/mock/web/MockHttpServletResponse$ResponseServl
etOutputStream
      51: dup
      52: aload_0
      53: aload_0
      54: getfield      #35                 // Field content:Ljava/io/ByteArrayOutputStream;
      57: invokespecial #41                 // Method org/springframework/mock/web/MockHttpServletResponse$ResponseServ
letOutputStream."<init>":(Lorg/springframework/mock/web/MockHttpServletResponse;Ljava/io/OutputStream;)V
      60: putfield      #44                 // Field outputStream:Ljakarta/servlet/ServletOutputStream;
      63: aload_0
      64: lconst_0
      65: putfield      #48                 // Field contentLength:J
      68: aload_0
      69: sipush        4096
      72: putfield      #52                 // Field bufferSize:I
      75: aload_0
      76: invokestatic  #56                 // Method java/util/Locale.getDefault:()Ljava/util/Locale;
      79: putfield      #62                 // Field locale:Ljava/util/Locale;
      82: aload_0
      83: new           #66                 // class java/util/ArrayList
      86: dup
      87: invokespecial #68                 // Method java/util/ArrayList."<init>":()V
      90: putfield      #69                 // Field cookies:Ljava/util/List;
      93: aload_0
      94: new           #73                 // class org/springframework/util/LinkedCaseInsensitiveMap
      97: dup
      98: invokespecial #75                 // Method org/springframework/util/LinkedCaseInsensitiveMap."<init>":()V
     101: putfield      #76                 // Field headers:Ljava/util/Map;
     104: aload_0
     105: sipush        200
     108: putfield      #82                 // Field status:I
     111: aload_0
     112: new           #66                 // class java/util/ArrayList
     115: dup
     116: invokespecial #68                 // Method java/util/ArrayList."<init>":()V
     119: putfield      #85                 // Field includedUrls:Ljava/util/List;
     122: return

  public void setOutputStreamAccessAllowed(boolean);
    Code:
       0: aload_0
       1: iload_1
       2: putfield      #7                  // Field outputStreamAccessAllowed:Z
       5: return

  public boolean isOutputStreamAccessAllowed();
    Code:
       0: aload_0
       1: getfield      #7                  // Field outputStreamAccessAllowed:Z
       4: ireturn

  public void setWriterAccessAllowed(boolean);
    Code:
       0: aload_0
       1: iload_1
       2: putfield      #13                 // Field writerAccessAllowed:Z
       5: return

  public boolean isWriterAccessAllowed();
    Code:
       0: aload_0
       1: getfield      #13                 // Field writerAccessAllowed:Z
       4: ireturn

  public void setDefaultCharacterEncoding(java.lang.String);
    Code:
       0: aload_1
       1: ldc           #88                 // String \'characterEncoding\' must not be null
       3: invokestatic  #90                 // Method org/springframework/util/Assert.notNull:(Ljava/lang/Object;Ljava/
lang/String;)V
       6: aload_0
       7: aload_1
       8: putfield      #20                 // Field defaultCharacterEncoding:Ljava/lang/String;
      11: aload_0
      12: getfield      #27                 // Field characterEncodingSet:Z
      15: ifne          23
      18: aload_0
      19: aload_1
      20: putfield      #24                 // Field characterEncoding:Ljava/lang/String;
      23: return

  public boolean isCharset();
    Code:
       0: aload_0
       1: getfield      #27                 // Field characterEncodingSet:Z
       4: ireturn

  public void setCharacterEncoding(java.lang.String);
    Code:
       0: aload_0
       1: aload_1
       2: invokevirtual #96                 // Method setExplicitCharacterEncoding:(Ljava/lang/String;)V
       5: aload_0
       6: invokevirtual #100                // Method updateContentTypePropertyAndHeader:()V
       9: return

  private void setExplicitCharacterEncoding(java.lang.String);
    Code:
       0: aload_1
       1: ifnonnull     154
       4: aload_0
       5: aload_0
       6: getfield      #20                 // Field defaultCharacterEncoding:Ljava/lang/String;
       9: putfield      #24                 // Field characterEncoding:Ljava/lang/String;
      12: aload_0
      13: iconst_0
      14: putfield      #27                 // Field characterEncodingSet:Z
      17: aload_0
      18: getfield      #103                // Field contentType:Ljava/lang/String;
      21: ifnull        164
      24: aload_0
      25: getfield      #103                // Field contentType:Ljava/lang/String;
      28: invokestatic  #106                // Method org/springframework/http/MediaType.parseMediaType:(Ljava/lang/Str
ing;)Lorg/springframework/http/MediaType;
      31: astore_2
      32: aload_2
      33: invokevirtual #112                // Method org/springframework/http/MediaType.getCharset:()Ljava/nio/charset
/Charset;
      36: ifnull        85
      39: new           #116                // class java/util/LinkedHashMap
      42: dup
      43: aload_2
      44: invokevirtual #118                // Method org/springframework/http/MediaType.getParameters:()Ljava/util/Map
;
      47: invokespecial #122                // Method java/util/LinkedHashMap."<init>":(Ljava/util/Map;)V
      50: astore_3
      51: aload_3
      52: ldc           #125                // String charset
      54: invokeinterface #127,  2          // InterfaceMethod java/util/Map.remove:(Ljava/lang/Object;)Ljava/lang/Obje
ct;
      59: pop
      60: new           #107                // class org/springframework/http/MediaType
      63: dup
      64: aload_2
      65: invokevirtual #133                // Method org/springframework/http/MediaType.getType:()Ljava/lang/String;
      68: aload_2
      69: invokevirtual #137                // Method org/springframework/http/MediaType.getSubtype:()Ljava/lang/String
;
      72: aload_3
      73: invokespecial #140                // Method org/springframework/http/MediaType."<init>":(Ljava/lang/String;Lj
ava/lang/String;Ljava/util/Map;)V
      76: astore_2
      77: aload_0
      78: aload_2
      79: invokevirtual #143                // Method org/springframework/http/MediaType.toString:()Ljava/lang/String;
      82: putfield      #103                // Field contentType:Ljava/lang/String;
      85: goto          164
      88: astore_2
      89: aload_0
      90: getfield      #103                // Field contentType:Ljava/lang/String;
      93: astore_3
      94: aload_3
      95: getstatic     #148                // Field java/util/Locale.ROOT:Ljava/util/Locale;
      98: invokevirtual #151                // Method java/lang/String.toLowerCase:(Ljava/util/Locale;)Ljava/lang/Strin
g;
     101: ldc           #157                // String charset=
     103: invokevirtual #159                // Method java/lang/String.indexOf:(Ljava/lang/String;)I
     106: istore        4
     108: iload         4
     110: iconst_m1
     111: if_icmpeq     151
     114: aload_3
     115: iconst_0
     116: iload         4
     118: invokevirtual #163                // Method java/lang/String.substring:(II)Ljava/lang/String;
     121: invokevirtual #167                // Method java/lang/String.trim:()Ljava/lang/String;
     124: astore_3
     125: aload_3
     126: ldc           #170                // String ;
     128: invokevirtual #172                // Method java/lang/String.endsWith:(Ljava/lang/String;)Z
     131: ifeq          146
     134: aload_3
     135: iconst_0
     136: aload_3
     137: invokevirtual #176                // Method java/lang/String.length:()I
     140: iconst_1
     141: isub
     142: invokevirtual #163                // Method java/lang/String.substring:(II)Ljava/lang/String;
     145: astore_3
     146: aload_0
     147: aload_3
     148: putfield      #103                // Field contentType:Ljava/lang/String;
     151: goto          164
     154: aload_0
     155: aload_1
     156: putfield      #24                 // Field characterEncoding:Ljava/lang/String;
     159: aload_0
     160: iconst_1
     161: putfield      #27                 // Field characterEncodingSet:Z
     164: return
    Exception table:
       from    to  target type
          24    85    88   Class java/lang/Exception

  private void updateContentTypePropertyAndHeader();
    Code:
       0: aload_0
       1: getfield      #103                // Field contentType:Ljava/lang/String;
       4: ifnull        58
       7: aload_0
       8: getfield      #103                // Field contentType:Ljava/lang/String;
      11: astore_1
      12: aload_0
      13: getfield      #27                 // Field characterEncodingSet:Z
      16: ifeq          50
      19: aload_1
      20: getstatic     #148                // Field java/util/Locale.ROOT:Ljava/util/Locale;
      23: invokevirtual #151                // Method java/lang/String.toLowerCase:(Ljava/util/Locale;)Ljava/lang/Strin
g;
      26: ldc           #157                // String charset=
      28: invokevirtual #180                // Method java/lang/String.contains:(Ljava/lang/CharSequence;)Z
      31: ifne          50
      34: aload_1
      35: aload_0
      36: invokevirtual #184                // Method getCharacterEncoding:()Ljava/lang/String;
      39: invokedynamic #187,  0            // InvokeDynamic #0:makeConcatWithConstants:(Ljava/lang/String;Ljava/lang/S
tring;)Ljava/lang/String;
      44: astore_1
      45: aload_0
      46: aload_1
      47: putfield      #103                // Field contentType:Ljava/lang/String;
      50: aload_0
      51: ldc           #193                // String Content-Type
      53: aload_1
      54: iconst_1
      55: invokevirtual #195                // Method doAddHeaderValue:(Ljava/lang/String;Ljava/lang/Object;Z)V
      58: return

  public java.lang.String getCharacterEncoding();
    Code:
       0: aload_0
       1: getfield      #24                 // Field characterEncoding:Ljava/lang/String;
       4: areturn

  public jakarta.servlet.ServletOutputStream getOutputStream();
    Code:
       0: aload_0
       1: getfield      #7                  // Field outputStreamAccessAllowed:Z
       4: ldc           #199                // String OutputStream access not allowed
       6: invokestatic  #201                // Method org/springframework/util/Assert.state:(ZLjava/lang/String;)V
       9: aload_0
      10: getfield      #44                 // Field outputStream:Ljakarta/servlet/ServletOutputStream;
      13: areturn

  public java.io.PrintWriter getWriter() throws java.io.UnsupportedEncodingException;
    Code:
       0: aload_0
       1: getfield      #13                 // Field writerAccessAllowed:Z
       4: ldc           #205                // String Writer access not allowed
       6: invokestatic  #201                // Method org/springframework/util/Assert.state:(ZLjava/lang/String;)V
       9: aload_0
      10: getfield      #207                // Field writer:Ljava/io/PrintWriter;
      13: ifnonnull     45
      16: new           #211                // class java/io/OutputStreamWriter
      19: dup
      20: aload_0
      21: getfield      #35                 // Field content:Ljava/io/ByteArrayOutputStream;
      24: aload_0
      25: invokevirtual #184                // Method getCharacterEncoding:()Ljava/lang/String;
      28: invokespecial #213                // Method java/io/OutputStreamWriter."<init>":(Ljava/io/OutputStream;Ljava/
lang/String;)V
      31: astore_1
      32: aload_0
      33: new           #216                // class org/springframework/mock/web/MockHttpServletResponse$ResponsePrint
Writer
      36: dup
      37: aload_0
      38: aload_1
      39: invokespecial #218                // Method org/springframework/mock/web/MockHttpServletResponse$ResponsePrin
tWriter."<init>":(Lorg/springframework/mock/web/MockHttpServletResponse;Ljava/io/Writer;)V
      42: putfield      #207                // Field writer:Ljava/io/PrintWriter;
      45: aload_0
      46: getfield      #207                // Field writer:Ljava/io/PrintWriter;
      49: areturn

  public byte[] getContentAsByteArray();
    Code:
       0: aload_0
       1: getfield      #35                 // Field content:Ljava/io/ByteArrayOutputStream;
       4: invokevirtual #221                // Method java/io/ByteArrayOutputStream.toByteArray:()[B
       7: areturn

  public java.lang.String getContentAsString() throws java.io.UnsupportedEncodingException;
    Code:
       0: aload_0
       1: getfield      #35                 // Field content:Ljava/io/ByteArrayOutputStream;
       4: aload_0
       5: invokevirtual #184                // Method getCharacterEncoding:()Ljava/lang/String;
       8: invokevirtual #225                // Method java/io/ByteArrayOutputStream.toString:(Ljava/lang/String;)Ljava/
lang/String;
      11: areturn

  public java.lang.String getContentAsString(java.nio.charset.Charset) throws java.io.UnsupportedEncodingException;
    Code:
       0: aload_0
       1: getfield      #27                 // Field characterEncodingSet:Z
       4: ifeq          19
       7: aload_0
       8: getfield      #35                 // Field content:Ljava/io/ByteArrayOutputStream;
      11: aload_0
      12: invokevirtual #184                // Method getCharacterEncoding:()Ljava/lang/String;
      15: invokevirtual #225                // Method java/io/ByteArrayOutputStream.toString:(Ljava/lang/String;)Ljava/
lang/String;
      18: areturn
      19: aload_0
      20: getfield      #35                 // Field content:Ljava/io/ByteArrayOutputStream;
      23: aload_1
      24: invokevirtual #228                // Method java/io/ByteArrayOutputStream.toString:(Ljava/nio/charset/Charset
;)Ljava/lang/String;
      27: areturn

  public void setContentLength(int);
    Code:
       0: aload_0
       1: iload_1
       2: i2l
       3: putfield      #48                 // Field contentLength:J
       6: aload_0
       7: ldc           #231                // String Content-Length
       9: iload_1
      10: invokestatic  #233                // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;
      13: iconst_1
      14: invokevirtual #195                // Method doAddHeaderValue:(Ljava/lang/String;Ljava/lang/Object;Z)V
      17: return

  public int getContentLength();
    Code:
       0: aload_0
       1: getfield      #48                 // Field contentLength:J
       4: l2i
       5: ireturn

  public void setContentLengthLong(long);
    Code:
       0: aload_0
       1: lload_1
       2: putfield      #48                 // Field contentLength:J
       5: aload_0
       6: ldc           #231                // String Content-Length
       8: lload_1
       9: invokestatic  #239                // Method java/lang/Long.valueOf:(J)Ljava/lang/Long;
      12: iconst_1
      13: invokevirtual #195                // Method doAddHeaderValue:(Ljava/lang/String;Ljava/lang/Object;Z)V
      16: return

  public long getContentLengthLong();
    Code:
       0: aload_0
       1: getfield      #48                 // Field contentLength:J
       4: lreturn

  public void setContentType(java.lang.String);
    Code:
       0: aload_0
       1: aload_1
       2: putfield      #103                // Field contentType:Ljava/lang/String;
       5: aload_1
       6: ifnull        106
       9: aload_1
      10: invokestatic  #106                // Method org/springframework/http/MediaType.parseMediaType:(Ljava/lang/Str
ing;)Lorg/springframework/http/MediaType;
      13: astore_2
      14: aload_2
      15: invokevirtual #112                // Method org/springframework/http/MediaType.getCharset:()Ljava/nio/charset
/Charset;
      18: ifnull        35
      21: aload_0
      22: aload_2
      23: invokevirtual #112                // Method org/springframework/http/MediaType.getCharset:()Ljava/nio/charset
/Charset;
      26: invokevirtual #244                // Method java/nio/charset/Charset.name:()Ljava/lang/String;
      29: invokevirtual #96                 // Method setExplicitCharacterEncoding:(Ljava/lang/String;)V
      32: goto          65
      35: aload_2
      36: getstatic     #249                // Field org/springframework/http/MediaType.APPLICATION_JSON:Lorg/springfra
mework/http/MediaType;
      39: invokevirtual #253                // Method org/springframework/http/MediaType.isCompatibleWith:(Lorg/springf
ramework/http/MediaType;)Z
      42: ifne          55
      45: aload_2
      46: getstatic     #257                // Field APPLICATION_PLUS_JSON:Lorg/springframework/http/MediaType;
      49: invokevirtual #253                // Method org/springframework/http/MediaType.isCompatibleWith:(Lorg/springf
ramework/http/MediaType;)Z
      52: ifeq          65
      55: aload_0
      56: getstatic     #260                // Field java/nio/charset/StandardCharsets.UTF_8:Ljava/nio/charset/Charset;
      59: invokevirtual #244                // Method java/nio/charset/Charset.name:()Ljava/lang/String;
      62: putfield      #24                 // Field characterEncoding:Ljava/lang/String;
      65: goto          102
      68: astore_2
      69: aload_1
      70: getstatic     #148                // Field java/util/Locale.ROOT:Ljava/util/Locale;
      73: invokevirtual #151                // Method java/lang/String.toLowerCase:(Ljava/util/Locale;)Ljava/lang/Strin
g;
      76: ldc           #157                // String charset=
      78: invokevirtual #159                // Method java/lang/String.indexOf:(Ljava/lang/String;)I
      81: istore_3
      82: iload_3
      83: iconst_m1
      84: if_icmpeq     102
      87: aload_0
      88: aload_1
      89: iload_3
      90: ldc           #157                // String charset=
      92: invokevirtual #176                // Method java/lang/String.length:()I
      95: iadd
      96: invokevirtual #266                // Method java/lang/String.substring:(I)Ljava/lang/String;
      99: invokevirtual #96                 // Method setExplicitCharacterEncoding:(Ljava/lang/String;)V
     102: aload_0
     103: invokevirtual #100                // Method updateContentTypePropertyAndHeader:()V
     106: return
    Exception table:
       from    to  target type
           9    65    68   Class java/lang/Exception

  public java.lang.String getContentType();
    Code:
       0: aload_0
       1: getfield      #103                // Field contentType:Ljava/lang/String;
       4: areturn

  public void setBufferSize(int);
    Code:
       0: aload_0
       1: iload_1
       2: putfield      #52                 // Field bufferSize:I
       5: return

  public int getBufferSize();
    Code:
       0: aload_0
       1: getfield      #52                 // Field bufferSize:I
       4: ireturn

  public void flushBuffer();
    Code:
       0: aload_0
       1: iconst_1
       2: invokevirtual #269                // Method setCommitted:(Z)V
       5: return

  public void resetBuffer();
    Code:
       0: aload_0
       1: invokevirtual #273                // Method isCommitted:()Z
       4: ifne          11
       7: iconst_1
       8: goto          12
      11: iconst_0
      12: ldc_w         #277                // String Cannot reset buffer - response is already committed
      15: invokestatic  #201                // Method org/springframework/util/Assert.state:(ZLjava/lang/String;)V
      18: aload_0
      19: getfield      #35                 // Field content:Ljava/io/ByteArrayOutputStream;
      22: invokevirtual #279                // Method java/io/ByteArrayOutputStream.reset:()V
      25: return

  private void setCommittedIfBufferSizeExceeded();
    Code:
       0: aload_0
       1: invokevirtual #282                // Method getBufferSize:()I
       4: istore_1
       5: iload_1
       6: ifle          25
       9: aload_0
      10: getfield      #35                 // Field content:Ljava/io/ByteArrayOutputStream;
      13: invokevirtual #285                // Method java/io/ByteArrayOutputStream.size:()I
      16: iload_1
      17: if_icmple     25
      20: aload_0
      21: iconst_1
      22: invokevirtual #269                // Method setCommitted:(Z)V
      25: return

  public void setCommitted(boolean);
    Code:
       0: aload_0
       1: iload_1
       2: putfield      #288                // Field committed:Z
       5: return

  public boolean isCommitted();
    Code:
       0: aload_0
       1: getfield      #288                // Field committed:Z
       4: ireturn

  public void reset();
    Code:
       0: aload_0
       1: invokevirtual #291                // Method resetBuffer:()V
       4: aload_0
       5: aload_0
       6: getfield      #20                 // Field defaultCharacterEncoding:Ljava/lang/String;
       9: putfield      #24                 // Field characterEncoding:Ljava/lang/String;
      12: aload_0
      13: iconst_0
      14: putfield      #27                 // Field characterEncodingSet:Z
      17: aload_0
      18: lconst_0
      19: putfield      #48                 // Field contentLength:J
      22: aload_0
      23: aconst_null
      24: putfield      #103                // Field contentType:Ljava/lang/String;
      27: aload_0
      28: invokestatic  #56                 // Method java/util/Locale.getDefault:()Ljava/util/Locale;
      31: putfield      #62                 // Field locale:Ljava/util/Locale;
      34: aload_0
      35: getfield      #69                 // Field cookies:Ljava/util/List;
      38: invokeinterface #294,  1          // InterfaceMethod java/util/List.clear:()V
      43: aload_0
      44: getfield      #76                 // Field headers:Ljava/util/Map;
      47: invokeinterface #299,  1          // InterfaceMethod java/util/Map.clear:()V
      52: aload_0
      53: sipush        200
      56: putfield      #82                 // Field status:I
      59: aload_0
      60: aconst_null
      61: putfield      #300                // Field errorMessage:Ljava/lang/String;
      64: return

  public void setLocale(java.util.Locale);
    Code:
       0: aload_1
       1: ifnonnull     5
       4: return
       5: aload_0
       6: aload_1
       7: putfield      #62                 // Field locale:Ljava/util/Locale;
      10: aload_0
      11: ldc_w         #303                // String Content-Language
      14: aload_1
      15: invokevirtual #305                // Method java/util/Locale.toLanguageTag:()Ljava/lang/String;
      18: iconst_1
      19: invokevirtual #195                // Method doAddHeaderValue:(Ljava/lang/String;Ljava/lang/Object;Z)V
      22: return

  public java.util.Locale getLocale();
    Code:
       0: aload_0
       1: getfield      #62                 // Field locale:Ljava/util/Locale;
       4: areturn

  public void addCookie(jakarta.servlet.http.Cookie);
    Code:
       0: aload_1
       1: ldc_w         #308                // String Cookie must not be null
       4: invokestatic  #90                 // Method org/springframework/util/Assert.notNull:(Ljava/lang/Object;Ljava/
lang/String;)V
       7: aload_0
       8: getfield      #69                 // Field cookies:Ljava/util/List;
      11: aload_1
      12: invokeinterface #310,  2          // InterfaceMethod java/util/List.add:(Ljava/lang/Object;)Z
      17: pop
      18: aload_0
      19: ldc_w         #314                // String Set-Cookie
      22: aload_0
      23: aload_1
      24: invokevirtual #316                // Method getCookieHeader:(Ljakarta/servlet/http/Cookie;)Ljava/lang/String;
      27: iconst_0
      28: invokevirtual #195                // Method doAddHeaderValue:(Ljava/lang/String;Ljava/lang/Object;Z)V
      31: return

  private java.lang.String getCookieHeader(jakarta.servlet.http.Cookie);
    Code:
       0: new           #320                // class java/lang/StringBuilder
       3: dup
       4: invokespecial #322                // Method java/lang/StringBuilder."<init>":()V
       7: astore_2
       8: aload_2
       9: aload_1
      10: invokevirtual #323                // Method jakarta/servlet/http/Cookie.getName:()Ljava/lang/String;
      13: invokevirtual #328                // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/Str
ingBuilder;
      16: bipush        61
      18: invokevirtual #332                // Method java/lang/StringBuilder.append:(C)Ljava/lang/StringBuilder;
      21: aload_1
      22: invokevirtual #335                // Method jakarta/servlet/http/Cookie.getValue:()Ljava/lang/String;
      25: ifnonnull     34
      28: ldc_w         #338                // String
      31: goto          38
      34: aload_1
      35: invokevirtual #335                // Method jakarta/servlet/http/Cookie.getValue:()Ljava/lang/String;
      38: invokevirtual #328                // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/Str
ingBuilder;
      41: pop
      42: aload_1
      43: invokevirtual #340                // Method jakarta/servlet/http/Cookie.getPath:()Ljava/lang/String;
      46: invokestatic  #343                // Method org/springframework/util/StringUtils.hasText:(Ljava/lang/String;)
Z
      49: ifeq          67
      52: aload_2
      53: ldc_w         #348                // String ; Path=
      56: invokevirtual #328                // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/Str
ingBuilder;
      59: aload_1
      60: invokevirtual #340                // Method jakarta/servlet/http/Cookie.getPath:()Ljava/lang/String;
      63: invokevirtual #328                // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/Str
ingBuilder;
      66: pop
      67: aload_1
      68: invokevirtual #350                // Method jakarta/servlet/http/Cookie.getDomain:()Ljava/lang/String;
      71: invokestatic  #343                // Method org/springframework/util/StringUtils.hasText:(Ljava/lang/String;)
Z
      74: ifeq          92
      77: aload_2
      78: ldc_w         #353                // String ; Domain=
      81: invokevirtual #328                // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/Str
ingBuilder;
      84: aload_1
      85: invokevirtual #350                // Method jakarta/servlet/http/Cookie.getDomain:()Ljava/lang/String;
      88: invokevirtual #328                // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/Str
ingBuilder;
      91: pop
      92: aload_1
      93: invokevirtual #355                // Method jakarta/servlet/http/Cookie.getMaxAge:()I
      96: istore_3
      97: aload_1
      98: instanceof    #358                // class org/springframework/mock/web/MockCookie
     101: ifeq          118
     104: aload_1
     105: checkcast     #358                // class org/springframework/mock/web/MockCookie
     108: astore        5
     110: aload         5
     112: invokevirtual #360                // Method org/springframework/mock/web/MockCookie.getExpires:()Ljava/time/Z
onedDateTime;
     115: goto          119
     118: aconst_null
     119: astore        4
     121: iload_3
     122: iflt          214
     125: aload_2
     126: ldc_w         #364                // String ; Max-Age=
     129: invokevirtual #328                // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/Str
ingBuilder;
     132: iload_3
     133: invokevirtual #366                // Method java/lang/StringBuilder.append:(I)Ljava/lang/StringBuilder;
     136: pop
     137: aload_2
     138: ldc_w         #369                // String ; Expires=
     141: invokevirtual #328                // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/Str
ingBuilder;
     144: pop
     145: aload         4
     147: ifnull        166
     150: aload_2
     151: aload         4
     153: getstatic     #371                // Field java/time/format/DateTimeFormatter.RFC_1123_DATE_TIME:Ljava/time/f
ormat/DateTimeFormatter;
     156: invokevirtual #377                // Method java/time/ZonedDateTime.format:(Ljava/time/format/DateTimeFormatt
er;)Ljava/lang/String;
     159: invokevirtual #328                // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/Str
ingBuilder;
     162: pop
     163: goto          240
     166: new           #191                // class org/springframework/http/HttpHeaders
     169: dup
     170: invokespecial #383                // Method org/springframework/http/HttpHeaders."<init>":()V
     173: astore        5
     175: aload         5
     177: iload_3
     178: ifle          194
     181: invokestatic  #384                // Method java/lang/System.currentTimeMillis:()J
     184: ldc2_w        #390                // long 1000l
     187: iload_3
     188: i2l
     189: lmul
     190: ladd
     191: goto          195
     194: lconst_0
     195: invokevirtual #392                // Method org/springframework/http/HttpHeaders.setExpires:(J)V
     198: aload_2
     199: aload         5
     201: ldc_w         #396                // String Expires
     204: invokevirtual #398                // Method org/springframework/http/HttpHeaders.getFirst:(Ljava/lang/String;
)Ljava/lang/String;
     207: invokevirtual #328                // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/Str
ingBuilder;
     210: pop
     211: goto          240
     214: aload         4
     216: ifnull        240
     219: aload_2
     220: ldc_w         #369                // String ; Expires=
     223: invokevirtual #328                // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/Str
ingBuilder;
     226: pop
     227: aload_2
     228: aload         4
     230: getstatic     #371                // Field java/time/format/DateTimeFormatter.RFC_1123_DATE_TIME:Ljava/time/f
ormat/DateTimeFormatter;
     233: invokevirtual #377                // Method java/time/ZonedDateTime.format:(Ljava/time/format/DateTimeFormatt
er;)Ljava/lang/String;
     236: invokevirtual #328                // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/Str
ingBuilder;
     239: pop
     240: aload_1
     241: invokevirtual #401                // Method jakarta/servlet/http/Cookie.getSecure:()Z
     244: ifeq          255
     247: aload_2
     248: ldc_w         #404                // String ; Secure
     251: invokevirtual #328                // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/Str
ingBuilder;
     254: pop
     255: aload_1
     256: invokevirtual #406                // Method jakarta/servlet/http/Cookie.isHttpOnly:()Z
     259: ifeq          270
     262: aload_2
     263: ldc_w         #409                // String ; HttpOnly
     266: invokevirtual #328                // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/Str
ingBuilder;
     269: pop
     270: aload_1
     271: ldc_w         #411                // String Partitioned
     274: invokevirtual #413                // Method jakarta/servlet/http/Cookie.getAttribute:(Ljava/lang/String;)Ljav
a/lang/String;
     277: ifnull        288
     280: aload_2
     281: ldc_w         #416                // String ; Partitioned
     284: invokevirtual #328                // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/Str
ingBuilder;
     287: pop
     288: aload_1
     289: instanceof    #358                // class org/springframework/mock/web/MockCookie
     292: ifeq          328
     295: aload_1
     296: checkcast     #358                // class org/springframework/mock/web/MockCookie
     299: astore        5
     301: aload         5
     303: invokevirtual #418                // Method org/springframework/mock/web/MockCookie.getSameSite:()Ljava/lang/
String;
     306: invokestatic  #343                // Method org/springframework/util/StringUtils.hasText:(Ljava/lang/String;)
Z
     309: ifeq          328
     312: aload_2
     313: ldc_w         #421                // String ; SameSite=
     316: invokevirtual #328                // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/Str
ingBuilder;
     319: aload         5
     321: invokevirtual #418                // Method org/springframework/mock/web/MockCookie.getSameSite:()Ljava/lang/
String;
     324: invokevirtual #328                // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/Str
ingBuilder;
     327: pop
     328: aload_1
     329: invokevirtual #423                // Method jakarta/servlet/http/Cookie.getComment:()Ljava/lang/String;
     332: invokestatic  #343                // Method org/springframework/util/StringUtils.hasText:(Ljava/lang/String;)
Z
     335: ifeq          353
     338: aload_2
     339: ldc_w         #426                // String ; Comment=
     342: invokevirtual #328                // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/Str
ingBuilder;
     345: aload_1
     346: invokevirtual #423                // Method jakarta/servlet/http/Cookie.getComment:()Ljava/lang/String;
     349: invokevirtual #328                // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/Str
ingBuilder;
     352: pop
     353: aload_2
     354: invokevirtual #428                // Method java/lang/StringBuilder.toString:()Ljava/lang/String;
     357: areturn

  public jakarta.servlet.http.Cookie[] getCookies();
    Code:
       0: aload_0
       1: getfield      #69                 // Field cookies:Ljava/util/List;
       4: iconst_0
       5: anewarray     #324                // class jakarta/servlet/http/Cookie
       8: invokeinterface #429,  2          // InterfaceMethod java/util/List.toArray:([Ljava/lang/Object;)[Ljava/lang/
Object;
      13: checkcast     #433                // class "[Ljakarta/servlet/http/Cookie;"
      16: areturn

  public jakarta.servlet.http.Cookie getCookie(java.lang.String);
    Code:
       0: aload_1
       1: ldc_w         #435                // String Cookie name must not be null
       4: invokestatic  #90                 // Method org/springframework/util/Assert.notNull:(Ljava/lang/Object;Ljava/
lang/String;)V
       7: aload_0
       8: getfield      #69                 // Field cookies:Ljava/util/List;
      11: invokeinterface #437,  1          // InterfaceMethod java/util/List.iterator:()Ljava/util/Iterator;
      16: astore_2
      17: aload_2
      18: invokeinterface #441,  1          // InterfaceMethod java/util/Iterator.hasNext:()Z
      23: ifeq          52
      26: aload_2
      27: invokeinterface #446,  1          // InterfaceMethod java/util/Iterator.next:()Ljava/lang/Object;
      32: checkcast     #324                // class jakarta/servlet/http/Cookie
      35: astore_3
      36: aload_1
      37: aload_3
      38: invokevirtual #323                // Method jakarta/servlet/http/Cookie.getName:()Ljava/lang/String;
      41: invokevirtual #450                // Method java/lang/String.equals:(Ljava/lang/Object;)Z
      44: ifeq          49
      47: aload_3
      48: areturn
      49: goto          17
      52: aconst_null
      53: areturn

  public boolean containsHeader(java.lang.String);
    Code:
       0: aload_0
       1: getfield      #76                 // Field headers:Ljava/util/Map;
       4: aload_1
       5: invokeinterface #453,  2          // InterfaceMethod java/util/Map.containsKey:(Ljava/lang/Object;)Z
      10: ireturn

  public java.util.Collection<java.lang.String> getHeaderNames();
    Code:
       0: aload_0
       1: getfield      #76                 // Field headers:Ljava/util/Map;
       4: invokeinterface #456,  1          // InterfaceMethod java/util/Map.keySet:()Ljava/util/Set;
       9: areturn

  public java.lang.String getHeader(java.lang.String);
    Code:
       0: aload_0
       1: getfield      #76                 // Field headers:Ljava/util/Map;
       4: aload_1
       5: invokeinterface #460,  2          // InterfaceMethod java/util/Map.get:(Ljava/lang/Object;)Ljava/lang/Object;
      10: checkcast     #463                // class org/springframework/mock/web/HeaderValueHolder
      13: astore_2
      14: aload_2
      15: ifnull        25
      18: aload_2
      19: invokevirtual #465                // Method org/springframework/mock/web/HeaderValueHolder.getStringValue:()L
java/lang/String;
      22: goto          26
      25: aconst_null
      26: areturn

  public java.util.List<java.lang.String> getHeaders(java.lang.String);
    Code:
       0: aload_0
       1: getfield      #76                 // Field headers:Ljava/util/Map;
       4: aload_1
       5: invokeinterface #460,  2          // InterfaceMethod java/util/Map.get:(Ljava/lang/Object;)Ljava/lang/Object;
      10: checkcast     #463                // class org/springframework/mock/web/HeaderValueHolder
      13: astore_2
      14: aload_2
      15: ifnull        23
      18: aload_2
      19: invokevirtual #468                // Method org/springframework/mock/web/HeaderValueHolder.getStringValues:()
Ljava/util/List;
      22: areturn
      23: invokestatic  #472                // Method java/util/Collections.emptyList:()Ljava/util/List;
      26: areturn

  public java.lang.Object getHeaderValue(java.lang.String);
    Code:
       0: aload_0
       1: getfield      #76                 // Field headers:Ljava/util/Map;
       4: aload_1
       5: invokeinterface #460,  2          // InterfaceMethod java/util/Map.get:(Ljava/lang/Object;)Ljava/lang/Object;
      10: checkcast     #463                // class org/springframework/mock/web/HeaderValueHolder
      13: astore_2
      14: aload_2
      15: ifnull        25
      18: aload_2
      19: invokevirtual #477                // Method org/springframework/mock/web/HeaderValueHolder.getValue:()Ljava/l
ang/Object;
      22: goto          26
      25: aconst_null
      26: areturn

  public java.util.List<java.lang.Object> getHeaderValues(java.lang.String);
    Code:
       0: aload_0
       1: getfield      #76                 // Field headers:Ljava/util/Map;
       4: aload_1
       5: invokeinterface #460,  2          // InterfaceMethod java/util/Map.get:(Ljava/lang/Object;)Ljava/lang/Object;
      10: checkcast     #463                // class org/springframework/mock/web/HeaderValueHolder
      13: astore_2
      14: aload_2
      15: ifnull        23
      18: aload_2
      19: invokevirtual #479                // Method org/springframework/mock/web/HeaderValueHolder.getValues:()Ljava/
util/List;
      22: areturn
      23: invokestatic  #472                // Method java/util/Collections.emptyList:()Ljava/util/List;
      26: areturn

  public java.lang.String encodeURL(java.lang.String);
    Code:
       0: aload_1
       1: areturn

  public java.lang.String encodeRedirectURL(java.lang.String);
    Code:
       0: aload_0
       1: aload_1
       2: invokevirtual #482                // Method encodeURL:(Ljava/lang/String;)Ljava/lang/String;
       5: areturn

  public void sendError(int, java.lang.String) throws java.io.IOException;
    Code:
       0: aload_0
       1: invokevirtual #273                // Method isCommitted:()Z
       4: ifne          11
       7: iconst_1
       8: goto          12
      11: iconst_0
      12: ldc_w         #485                // String Cannot set error status - response is already committed
      15: invokestatic  #201                // Method org/springframework/util/Assert.state:(ZLjava/lang/String;)V
      18: aload_0
      19: iload_1
      20: putfield      #82                 // Field status:I
      23: aload_0
      24: aload_2
      25: putfield      #300                // Field errorMessage:Ljava/lang/String;
      28: aload_0
      29: iconst_1
      30: invokevirtual #269                // Method setCommitted:(Z)V
      33: return

  public void sendError(int) throws java.io.IOException;
    Code:
       0: aload_0
       1: invokevirtual #273                // Method isCommitted:()Z
       4: ifne          11
       7: iconst_1
       8: goto          12
      11: iconst_0
      12: ldc_w         #485                // String Cannot set error status - response is already committed
      15: invokestatic  #201                // Method org/springframework/util/Assert.state:(ZLjava/lang/String;)V
      18: aload_0
      19: iload_1
      20: putfield      #82                 // Field status:I
      23: aload_0
      24: iconst_1
      25: invokevirtual #269                // Method setCommitted:(Z)V
      28: return

  public void sendRedirect(java.lang.String) throws java.io.IOException;
    Code:
       0: aload_0
       1: aload_1
       2: sipush        302
       5: iconst_1
       6: invokevirtual #487                // Method sendRedirect:(Ljava/lang/String;IZ)V
       9: return

  public void sendRedirect(java.lang.String, int, boolean) throws java.io.IOException;
    Code:
       0: aload_0
       1: invokevirtual #273                // Method isCommitted:()Z
       4: ifne          11
       7: iconst_1
       8: goto          12
      11: iconst_0
      12: ldc_w         #491                // String Cannot send redirect - response is already committed
      15: invokestatic  #201                // Method org/springframework/util/Assert.state:(ZLjava/lang/String;)V
      18: aload_1
      19: ldc_w         #493                // String Redirect URL must not be null
      22: invokestatic  #90                 // Method org/springframework/util/Assert.notNull:(Ljava/lang/Object;Ljava/
lang/String;)V
      25: aload_0
      26: ldc_w         #495                // String Location
      29: aload_1
      30: invokevirtual #497                // Method setHeader:(Ljava/lang/String;Ljava/lang/String;)V
      33: aload_0
      34: iload_2
      35: invokevirtual #501                // Method setStatus:(I)V
      38: aload_0
      39: iconst_1
      40: invokevirtual #269                // Method setCommitted:(Z)V
      43: return

  public java.lang.String getRedirectedUrl();
    Code:
       0: aload_0
       1: ldc_w         #495                // String Location
       4: invokevirtual #504                // Method getHeader:(Ljava/lang/String;)Ljava/lang/String;
       7: areturn

  public void setDateHeader(java.lang.String, long);
    Code:
       0: aload_0
       1: aload_1
       2: aload_0
       3: lload_2
       4: invokevirtual #507                // Method formatDate:(J)Ljava/lang/String;
       7: invokevirtual #511                // Method setHeaderValue:(Ljava/lang/String;Ljava/lang/Object;)V
      10: return

  public void addDateHeader(java.lang.String, long);
    Code:
       0: aload_0
       1: aload_1
       2: aload_0
       3: lload_2
       4: invokevirtual #507                // Method formatDate:(J)Ljava/lang/String;
       7: invokevirtual #515                // Method addHeaderValue:(Ljava/lang/String;Ljava/lang/Object;)V
      10: return

  public long getDateHeader(java.lang.String);
    Code:
       0: aload_0
       1: aload_1
       2: invokevirtual #504                // Method getHeader:(Ljava/lang/String;)Ljava/lang/String;
       5: astore_2
       6: aload_2
       7: ifnonnull     14
      10: ldc2_w        #518                // long -1l
      13: lreturn
      14: aload_0
      15: invokevirtual #520                // Method newDateFormat:()Ljava/text/DateFormat;
      18: aload_0
      19: aload_1
      20: invokevirtual #504                // Method getHeader:(Ljava/lang/String;)Ljava/lang/String;
      23: invokevirtual #524                // Method java/text/DateFormat.parse:(Ljava/lang/String;)Ljava/util/Date;
      26: invokevirtual #530                // Method java/util/Date.getTime:()J
      29: lreturn
      30: astore_3
      31: new           #537                // class java/lang/IllegalArgumentException
      34: dup
      35: aload_1
      36: aload_2
      37: invokedynamic #539,  0            // InvokeDynamic #1:makeConcatWithConstants:(Ljava/lang/String;Ljava/lang/S
tring;)Ljava/lang/String;
      42: invokespecial #540                // Method java/lang/IllegalArgumentException."<init>":(Ljava/lang/String;)V
      45: athrow
    Exception table:
       from    to  target type
          14    29    30   Class java/text/ParseException

  private java.lang.String formatDate(long);
    Code:
       0: aload_0
       1: invokevirtual #520                // Method newDateFormat:()Ljava/text/DateFormat;
       4: new           #531                // class java/util/Date
       7: dup
       8: lload_1
       9: invokespecial #542                // Method java/util/Date."<init>":(J)V
      12: invokevirtual #544                // Method java/text/DateFormat.format:(Ljava/util/Date;)Ljava/lang/String;
      15: areturn

  private java.text.DateFormat newDateFormat();
    Code:
       0: new           #547                // class java/text/SimpleDateFormat
       3: dup
       4: ldc_w         #549                // String EEE, dd MMM yyyy HH:mm:ss zzz
       7: getstatic     #551                // Field java/util/Locale.US:Ljava/util/Locale;
      10: invokespecial #554                // Method java/text/SimpleDateFormat."<init>":(Ljava/lang/String;Ljava/util
/Locale;)V
      13: astore_1
      14: aload_1
      15: getstatic     #557                // Field GMT:Ljava/util/TimeZone;
      18: invokevirtual #561                // Method java/text/SimpleDateFormat.setTimeZone:(Ljava/util/TimeZone;)V
      21: aload_1
      22: areturn

  public void setHeader(java.lang.String, java.lang.String);
    Code:
       0: aload_2
       1: ifnonnull     18
       4: aload_0
       5: getfield      #76                 // Field headers:Ljava/util/Map;
       8: aload_1
       9: invokeinterface #127,  2          // InterfaceMethod java/util/Map.remove:(Ljava/lang/Object;)Ljava/lang/Obje
ct;
      14: pop
      15: goto          24
      18: aload_0
      19: aload_1
      20: aload_2
      21: invokevirtual #511                // Method setHeaderValue:(Ljava/lang/String;Ljava/lang/Object;)V
      24: return

  public void addHeader(java.lang.String, java.lang.String);
    Code:
       0: aload_0
       1: aload_1
       2: aload_2
       3: invokevirtual #515                // Method addHeaderValue:(Ljava/lang/String;Ljava/lang/Object;)V
       6: return

  public void setIntHeader(java.lang.String, int);
    Code:
       0: aload_0
       1: aload_1
       2: iload_2
       3: invokestatic  #233                // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;
       6: invokevirtual #511                // Method setHeaderValue:(Ljava/lang/String;Ljava/lang/Object;)V
       9: return

  public void addIntHeader(java.lang.String, int);
    Code:
       0: aload_0
       1: aload_1
       2: iload_2
       3: invokestatic  #233                // Method java/lang/Integer.valueOf:(I)Ljava/lang/Integer;
       6: invokevirtual #515                // Method addHeaderValue:(Ljava/lang/String;Ljava/lang/Object;)V
       9: return

  private void setHeaderValue(java.lang.String, java.lang.Object);
    Code:
       0: aload_2
       1: ifnonnull     5
       4: return
       5: iconst_1
       6: istore_3
       7: aload_0
       8: aload_1
       9: aload_2
      10: iload_3
      11: invokevirtual #565                // Method setSpecialHeader:(Ljava/lang/String;Ljava/lang/Object;Z)Z
      14: ifeq          18
      17: return
      18: aload_0
      19: aload_1
      20: aload_2
      21: iload_3
      22: invokevirtual #195                // Method doAddHeaderValue:(Ljava/lang/String;Ljava/lang/Object;Z)V
      25: return

  private void addHeaderValue(java.lang.String, java.lang.Object);
    Code:
       0: aload_2
       1: ifnonnull     5
       4: return
       5: iconst_0
       6: istore_3
       7: aload_0
       8: aload_1
       9: aload_2
      10: iload_3
      11: invokevirtual #565                // Method setSpecialHeader:(Ljava/lang/String;Ljava/lang/Object;Z)Z
      14: ifeq          18
      17: return
      18: aload_0
      19: aload_1
      20: aload_2
      21: iload_3
      22: invokevirtual #195                // Method doAddHeaderValue:(Ljava/lang/String;Ljava/lang/Object;Z)V
      25: return

  private boolean setSpecialHeader(java.lang.String, java.lang.Object, boolean);
    Code:
       0: ldc           #193                // String Content-Type
       2: aload_1
       3: invokevirtual #569                // Method java/lang/String.equalsIgnoreCase:(Ljava/lang/String;)Z
       6: ifeq          19
       9: aload_0
      10: aload_2
      11: invokevirtual #572                // Method java/lang/Object.toString:()Ljava/lang/String;
      14: invokevirtual #573                // Method setContentType:(Ljava/lang/String;)V
      17: iconst_1
      18: ireturn
      19: ldc           #231                // String Content-Length
      21: aload_1
      22: invokevirtual #569                // Method java/lang/String.equalsIgnoreCase:(Ljava/lang/String;)Z
      25: ifeq          62
      28: aload_0
      29: aload_2
      30: instanceof    #576                // class java/lang/Number
      33: ifeq          50
      36: aload_2
      37: checkcast     #576                // class java/lang/Number
      40: astore        4
      42: aload         4
      44: invokevirtual #578                // Method java/lang/Number.intValue:()I
      47: goto          57
      50: aload_2
      51: invokevirtual #572                // Method java/lang/Object.toString:()Ljava/lang/String;
      54: invokestatic  #581                // Method java/lang/Integer.parseInt:(Ljava/lang/String;)I
      57: invokevirtual #584                // Method setContentLength:(I)V
      60: iconst_1
      61: ireturn
      62: ldc_w         #303                // String Content-Language
      65: aload_1
      66: invokevirtual #569                // Method java/lang/String.equalsIgnoreCase:(Ljava/lang/String;)Z
      69: ifeq          165
      72: aload_2
      73: invokevirtual #572                // Method java/lang/Object.toString:()Ljava/lang/String;
      76: astore        4
      78: iload_3
      79: ifne          97
      82: aload_0
      83: getfield      #76                 // Field headers:Ljava/util/Map;
      86: ldc_w         #303                // String Content-Language
      89: invokeinterface #453,  2          // InterfaceMethod java/util/Map.containsKey:(Ljava/lang/Object;)Z
      94: ifne          153
      97: new           #191                // class org/springframework/http/HttpHeaders
     100: dup
     101: invokespecial #383                // Method org/springframework/http/HttpHeaders."<init>":()V
     104: astore        5
     106: aload         5
     108: ldc_w         #303                // String Content-Language
     111: aload         4
     113: invokevirtual #587                // Method org/springframework/http/HttpHeaders.add:(Ljava/lang/String;Ljava
/lang/String;)V
     116: aload         5
     118: invokevirtual #589                // Method org/springframework/http/HttpHeaders.getContentLanguage:()Ljava/u
til/Locale;
     121: astore        6
     123: aload_0
     124: aload         6
     126: ifnull        134
     129: aload         6
     131: goto          137
     134: invokestatic  #56                 // Method java/util/Locale.getDefault:()Ljava/util/Locale;
     137: putfield      #62                 // Field locale:Ljava/util/Locale;
     140: aload_0
     141: ldc_w         #303                // String Content-Language
     144: aload         4
     146: iload_3
     147: invokevirtual #195                // Method doAddHeaderValue:(Ljava/lang/String;Ljava/lang/Object;Z)V
     150: goto          163
     153: aload_0
     154: ldc_w         #303                // String Content-Language
     157: aload         4
     159: iconst_0
     160: invokevirtual #195                // Method doAddHeaderValue:(Ljava/lang/String;Ljava/lang/Object;Z)V
     163: iconst_1
     164: ireturn
     165: ldc_w         #314                // String Set-Cookie
     168: aload_1
     169: invokevirtual #569                // Method java/lang/String.equalsIgnoreCase:(Ljava/lang/String;)Z
     172: ifeq          205
     175: aload_2
     176: invokevirtual #572                // Method java/lang/Object.toString:()Ljava/lang/String;
     179: invokestatic  #592                // Method org/springframework/mock/web/MockCookie.parse:(Ljava/lang/String;
)Lorg/springframework/mock/web/MockCookie;
     182: astore        4
     184: iload_3
     185: ifeq          197
     188: aload_0
     189: aload         4
     191: invokevirtual #595                // Method setCookie:(Ljakarta/servlet/http/Cookie;)V
     194: goto          203
     197: aload_0
     198: aload         4
     200: invokevirtual #599                // Method addCookie:(Ljakarta/servlet/http/Cookie;)V
     203: iconst_1
     204: ireturn
     205: iconst_0
     206: ireturn

  private void doAddHeaderValue(java.lang.String, java.lang.Object, boolean);
    Code:
       0: aload_2
       1: ldc_w         #602                // String Header value must not be null
       4: invokestatic  #90                 // Method org/springframework/util/Assert.notNull:(Ljava/lang/Object;Ljava/
lang/String;)V
       7: aload_0
       8: getfield      #76                 // Field headers:Ljava/util/Map;
      11: aload_1
      12: invokedynamic #604,  0            // InvokeDynamic #2:apply:()Ljava/util/function/Function;
      17: invokeinterface #608,  3          // InterfaceMethod java/util/Map.computeIfAbsent:(Ljava/lang/Object;Ljava/u
til/function/Function;)Ljava/lang/Object;
      22: checkcast     #463                // class org/springframework/mock/web/HeaderValueHolder
      25: astore        4
      27: iload_3
      28: ifeq          40
      31: aload         4
      33: aload_2
      34: invokevirtual #612                // Method org/springframework/mock/web/HeaderValueHolder.setValue:(Ljava/la
ng/Object;)V
      37: goto          46
      40: aload         4
      42: aload_2
      43: invokevirtual #616                // Method org/springframework/mock/web/HeaderValueHolder.addValue:(Ljava/la
ng/Object;)V
      46: return

  private void setCookie(jakarta.servlet.http.Cookie);
    Code:
       0: aload_1
       1: ldc_w         #308                // String Cookie must not be null
       4: invokestatic  #90                 // Method org/springframework/util/Assert.notNull:(Ljava/lang/Object;Ljava/
lang/String;)V
       7: aload_0
       8: getfield      #69                 // Field cookies:Ljava/util/List;
      11: invokeinterface #294,  1          // InterfaceMethod java/util/List.clear:()V
      16: aload_0
      17: getfield      #69                 // Field cookies:Ljava/util/List;
      20: aload_1
      21: invokeinterface #310,  2          // InterfaceMethod java/util/List.add:(Ljava/lang/Object;)Z
      26: pop
      27: aload_0
      28: ldc_w         #314                // String Set-Cookie
      31: aload_0
      32: aload_1
      33: invokevirtual #316                // Method getCookieHeader:(Ljakarta/servlet/http/Cookie;)Ljava/lang/String;
      36: iconst_1
      37: invokevirtual #195                // Method doAddHeaderValue:(Ljava/lang/String;Ljava/lang/Object;Z)V
      40: return

  public void setStatus(int);
    Code:
       0: aload_0
       1: invokevirtual #273                // Method isCommitted:()Z
       4: ifne          12
       7: aload_0
       8: iload_1
       9: putfield      #82                 // Field status:I
      12: return

  public int getStatus();
    Code:
       0: aload_0
       1: getfield      #82                 // Field status:I
       4: ireturn

  public java.lang.String getErrorMessage();
    Code:
       0: aload_0
       1: getfield      #300                // Field errorMessage:Ljava/lang/String;
       4: areturn

  public void setForwardedUrl(java.lang.String);
    Code:
       0: aload_0
       1: aload_1
       2: putfield      #619                // Field forwardedUrl:Ljava/lang/String;
       5: return

  public java.lang.String getForwardedUrl();
    Code:
       0: aload_0
       1: getfield      #619                // Field forwardedUrl:Ljava/lang/String;
       4: areturn

  public void setIncludedUrl(java.lang.String);
    Code:
       0: aload_0
       1: getfield      #85                 // Field includedUrls:Ljava/util/List;
       4: invokeinterface #294,  1          // InterfaceMethod java/util/List.clear:()V
       9: aload_1
      10: ifnull        24
      13: aload_0
      14: getfield      #85                 // Field includedUrls:Ljava/util/List;
      17: aload_1
      18: invokeinterface #310,  2          // InterfaceMethod java/util/List.add:(Ljava/lang/Object;)Z
      23: pop
      24: return

  public java.lang.String getIncludedUrl();
    Code:
       0: aload_0
       1: getfield      #85                 // Field includedUrls:Ljava/util/List;
       4: invokeinterface #622,  1          // InterfaceMethod java/util/List.size:()I
       9: istore_1
      10: iload_1
      11: iconst_1
      12: if_icmpgt     19
      15: iconst_1
      16: goto          20
      19: iconst_0
      20: aload_0
      21: invokedynamic #623,  0            // InvokeDynamic #3:get:(Lorg/springframework/mock/web/MockHttpServletRespo
nse;)Ljava/util/function/Supplier;
      26: invokestatic  #626                // Method org/springframework/util/Assert.state:(ZLjava/util/function/Suppl
ier;)V
      29: iload_1
      30: iconst_1
      31: if_icmpne     50
      34: aload_0
      35: getfield      #85                 // Field includedUrls:Ljava/util/List;
      38: iconst_0
      39: invokeinterface #629,  2          // InterfaceMethod java/util/List.get:(I)Ljava/lang/Object;
      44: checkcast     #152                // class java/lang/String
      47: goto          51
      50: aconst_null
      51: areturn

  public void addIncludedUrl(java.lang.String);
    Code:
       0: aload_1
       1: ldc_w         #632                // String Included URL must not be null
       4: invokestatic  #90                 // Method org/springframework/util/Assert.notNull:(Ljava/lang/Object;Ljava/
lang/String;)V
       7: aload_0
       8: getfield      #85                 // Field includedUrls:Ljava/util/List;
      11: aload_1
      12: invokeinterface #310,  2          // InterfaceMethod java/util/List.add:(Ljava/lang/Object;)Z
      17: pop
      18: return

  public java.util.List<java.lang.String> getIncludedUrls();
    Code:
       0: aload_0
       1: getfield      #85                 // Field includedUrls:Ljava/util/List;
       4: areturn

  public java.util.Collection getHeaders(java.lang.String);
    Code:
       0: aload_0
       1: aload_1
       2: invokevirtual #634                // Method getHeaders:(Ljava/lang/String;)Ljava/util/List;
       5: areturn

  private java.lang.String lambda$getIncludedUrl$1();
    Code:
       0: aload_0
       1: getfield      #85                 // Field includedUrls:Ljava/util/List;
       4: invokestatic  #638                // Method java/lang/String.valueOf:(Ljava/lang/Object;)Ljava/lang/String;
       7: invokedynamic #641,  0            // InvokeDynamic #4:makeConcatWithConstants:(Ljava/lang/String;)Ljava/lang/
String;
      12: areturn

  private static org.springframework.mock.web.HeaderValueHolder lambda$doAddHeaderValue$0(java.lang.String);
    Code:
       0: new           #463                // class org/springframework/mock/web/HeaderValueHolder
       3: dup
       4: invokespecial #643                // Method org/springframework/mock/web/HeaderValueHolder."<init>":()V
       7: areturn

  static {};
    Code:
       0: ldc_w         #644                // String GMT
       3: invokestatic  #645                // Method java/util/TimeZone.getTimeZone:(Ljava/lang/String;)Ljava/util/Tim
eZone;
       6: putstatic     #557                // Field GMT:Ljava/util/TimeZone;
       9: new           #107                // class org/springframework/http/MediaType
      12: dup
      13: ldc_w         #651                // String application
      16: ldc_w         #653                // String *+json
      19: invokespecial #655                // Method org/springframework/http/MediaType."<init>":(Ljava/lang/String;Lj
ava/lang/String;)V
      22: putstatic     #257                // Field APPLICATION_PLUS_JSON:Lorg/springframework/http/MediaType;
      25: return
}
����: �ڲ�����: java.nio.file.AccessDeniedException: C:\Users\Administrator\.m2\repository\org\springframework\spring-te
st\6.2.19\spring-test-6.2.19.jar {1} {2}

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "git grep -n -A 1 -B 3 'assertDoesNotThrow(re
sponse::getOutputStream' d02f8129 -- services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/test
/java/cn/zszj/framework/idempotent/core/aop/IdempotentAspectTest.java" in E:\众墅之家AI赋能平台底座
 succeeded in 0ms:
d02f8129:services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/test/java/cn/zszj/framework/idem
potent/core/aop/IdempotentAspectTest.java-342-        // C2：端到端复现生产症状——摘要计算后业务仍能安全走二进制输出。若摘要序列化提前调用了 getWriter()
d02f8129:services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/test/java/cn/zszj/framework/idem
potent/core/aop/IdempotentAspectTest.java-343-        //（前缀漏排 Tomcat 式实现时会发生），MockHttpServletResponse 置 usingWriter，此处 
getOutputStream() 将抛
d02f8129:services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/test/java/cn/zszj/framework/idem
potent/core/aop/IdempotentAspectTest.java-344-        // IllegalStateException（正是 ServletUtils.writeAttachment 的生产故障）；类
型排除生效后 response 未被触碰，故不抛。
d02f8129:services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/test/java/cn/zszj/framework/idem
potent/core/aop/IdempotentAspectTest.java:345:        assertDoesNotThrow(response::getOutputStream,
d02f8129:services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/test/java/cn/zszj/framework/idem
potent/core/aop/IdempotentAspectTest.java-346-                "摘要计算不得提前选定 response 的 writer，否则后续二进制输出（writeAttachment）会
抛 IllegalStateException");

codex
The production type-based exclusion appears correct, but the replacement test assertion cannot detect the response-outp
ut side effect it claims to verify and removes the previous explicit checks.

Review comment:

- [P2] Preserve assertions that detect response output access — E:/众墅之家AI赋能平台底座/services/zhongshu-core/zszj-framework/z
szj-spring-boot-starter-protection/src/test/java/cn/zszj/framework/idempotent/core/aop/IdempotentAspectTest.java:345-34
6
  With Spring 6.2.19's `MockHttpServletResponse`, calling `getWriter()` does not prevent subsequent `getOutputStream()`
 access: their access flags are independent. Consequently, this assertion passes even if the aspect has already accesse
d the writer. The digest assertion checks only which arguments contribute to the digest, so replacing the previous `nev
er()` verifications loses the explicit protection against response side effects. Use a spy on the concrete response and
 retain those verifications, or use a response double that enforces writer/output-stream mutual exclusion.
The production type-based exclusion appears correct, but the replacement test assertion cannot detect the response-output side effect it claims to verify and removes the previous explicit checks.

Review comment:

- [P2] Preserve assertions that detect response output access — E:/众墅之家AI赋能平台底座/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/test/java/cn/zszj/framework/idempotent/core/aop/IdempotentAspectTest.java:345-346
  With Spring 6.2.19's `MockHttpServletResponse`, calling `getWriter()` does not prevent subsequent `getOutputStream()` access: their access flags are independent. Consequently, this assertion passes even if the aspect has already accessed the writer. The digest assertion checks only which arguments contribute to the digest, so replacing the previous `never()` verifications loses the explicit protection against response side effects. Use a spy on the concrete response and retain those verifications, or use a response double that enforces writer/output-stream mutual exclusion.
