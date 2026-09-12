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
session id: 01a09419-1a5c-7fd2-98aa-6f081765357a
--------
user
commit 3b5d3b8
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'git status --short; git rev-parse --show-top
level; git show --stat --oneline --decorate --no-renames 3b5d3b8f; Get-ChildItem -Force -Name; Get-ChildItem -Path .. -
Filter AGENTS* -Force' in E:\众墅之家AI赋能平台底座
 succeeded in 102ms:
E:/众墅之家AI赋能平台底座
3b5d3b8f (feat/sec-011-a) feat(sec-011-a): 幂等键加租户/主体作用域 + 同键异参冲突检测合同 [ZS-SEC-011.A]
 .../idempotent/core/aop/IdempotentAspect.java      |  60 ++++-
 .../impl/DefaultIdempotentKeyResolver.java         |  26 ++-
 .../idempotent/core/redis/IdempotentRedisDAO.java  |  29 ++-
 .../idempotent/core/aop/IdempotentAspectTest.java  | 193 +++++++++++++++-
 .../impl/DefaultIdempotentKeyResolverTest.java     | 251 +++++++++++++++++++++
 5 files changed, 543 insertions(+), 16 deletions(-)
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
_zs_sec011a_prot.txt
_zs_sec011a_verify.ps1

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "git log -6 --oneline; git show --format=full
er --no-ext-diff --no-renames 3b5d3b8f; Get-ChildItem -Path . -Recurse -Force -File -Include AGENTS.md,AGENTS.override.
md -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName; if (Test-Path 'E:\\AGENTS.md') { Get-Content
 'E:\\AGENTS.md' }; if (Test-Path 'E:\\AGENTS.override.md') { Get-Content 'E:\\AGENTS.override.md' }" in E:\众墅之家AI赋能平台底
座
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command '$paths = @('"'E:\\', '.', 'services', 'servi
ces/zhongshu-core', 'services/zhongshu-core/zszj-framework', 'services/zhongshu-core/zszj-framework/zszj-spring-boot-st
arter-protection'); foreach ("'$p in $paths) { Get-ChildItem -LiteralPath $p -Force -File -ErrorAction SilentlyContinue
 | Where-Object { $_.Name -match '"'"'^(AGENTS('"\\.override)?\\.md|CLAUDE\\.md|GEMINI\\.md)"'$'"' } | ForEach-Object {
 "'$_.FullName; Get-Content -LiteralPath $_.FullName } }; git ls-files '"'*AGENTS*' '*CLAUDE.md' '*GEMINI.md'; rg -n --
glob '*.java' '@Idempotent|IdempotentRedisDAO|sanitizeArgs|joinMethodArgs' services/zhongshu-core/zszj-framework/zszj-s
pring-boot-starter-protection services/zhongshu-core/zszj-framework/zszj-common" in E:\众墅之家AI赋能平台底座
 succeeded in 0ms:
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "Get-Content -Encoding UTF8 services/zhongshu
-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framework/common/util/log/LogSanitizeUtils.java; Get-Content -En
coding UTF8 services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framework/common/util/string/StrUti
ls.java; rg -n '@Idempotent|ExpressionIdempotentKeyResolver' services --glob '*.java'; Get-Content -Encoding UTF8 servi
ces/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/idempotent/core/ao
p/IdempotentAspect.java" in E:\众墅之家AI赋能平台底座
 succeeded in 182ms:
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
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.util.Assert;

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
        String argsDigest = SecureUtil.md5(LogSanitizeUtils.sanitizeArgs(joinPoint.getArgs()));

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
                        joinPoint.getSignature(), LogSanitizeUtils.sanitizeArgs(joinPoint.getArgs()));
            } else {
                log.info("[aroundPointCut][����({}) ����({}) ͬ��ͬ���ظ�]",
                        joinPoint.getSignature(), LogSanitizeUtils.sanitizeArgs(joinPoint.getArgs()));
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

}

 succeeded in 35856ms:
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "Get-ChildItem -Path services/zhongshu-core/z
szj-framework/zszj-spring-boot-starter-protection -Recurse -Force -File -Include AGENTS.md,AGENTS.override.md,CLAUDE.md
,GEMINI.md | Select-Object -ExpandProperty FullName; git show 3b5d3b8f:services/zhongshu-core/zszj-framework/zszj-sprin
g-boot-starter-protection/pom.xml; git show 3b5d3b8f:services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web
/src/main/java/cn/zszj/framework/web/core/util/WebFrameworkUtils.java; git show 3b5d3b8f:services/zhongshu-core/zszj-fr
amework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/idempotent/core/keyresolver/impl/Expression
IdempotentKeyResolver.java; git grep -n '@Idempotent' 3b5d3b8f -- ':"'!third_party'"' ':"'!reference'"'; git config --g
et core.attributesfile; git ls-files '*attributes' '*config.toml' '*.instructions.md'" in E:\众墅之家AI赋能平台底座
 succeeded in 576ms:
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
    <description>服务保证，提供分布式锁、幂等、限流、熔断、API 签名等等功能</description>
    <url>https://github.com/YunaiV/ruoyi-vue-pro</url>

    <dependencies>
        <!-- Web 相关 -->
        <dependency>
            <groupId>cn.zszj</groupId>
            <artifactId>zszj-spring-boot-starter-web</artifactId>
            <scope>provided</scope> <!-- 设置为 provided，只有限流、幂等使用到 -->
        </dependency>

        <!-- DB 相关 -->
        <dependency>
            <groupId>cn.zszj</groupId>
            <artifactId>zszj-spring-boot-starter-redis</artifactId>
        </dependency>

        <!-- 服务保障相关 -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>lock4j-redisson-spring-boot-starter</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Test 测试相关 -->
        <dependency>
            <groupId>cn.zszj</groupId>
            <artifactId>zszj-spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

</project>
package cn.zszj.framework.web.core.util;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.zszj.framework.common.enums.TerminalEnum;
import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.zszj.framework.common.pojo.CommonResult;
import cn.zszj.framework.common.util.servlet.ServletUtils;
import cn.zszj.framework.web.config.WebProperties;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception0;

/**
 * 专属于 web 包的工具类
 *
 * @author 芋道源码
 */
public class WebFrameworkUtils {

    private static final String REQUEST_ATTRIBUTE_LOGIN_USER_ID = "login_user_id";
    private static final String REQUEST_ATTRIBUTE_LOGIN_USER_TYPE = "login_user_type";

    private static final String REQUEST_ATTRIBUTE_COMMON_RESULT = "common_result";

    public static final String HEADER_TENANT_ID = "tenant-id";
    public static final String HEADER_VISIT_TENANT_ID = "visit-tenant-id";

    /**
     * 终端的 Header
     *
     * @see cn.zszj.framework.common.enums.TerminalEnum
     */
    public static final String HEADER_TERMINAL = "terminal";

    private static WebProperties properties;

    public WebFrameworkUtils(WebProperties webProperties) {
        WebFrameworkUtils.properties = webProperties;
    }

    /**
     * 获得租户编号，从 header 中
     * 考虑到其它 framework 组件也会使用到租户编号，所以不得不放在 WebFrameworkUtils 统一提供
     *
     * @param request 请求
     * @return 租户编号
     */
    public static Long getTenantId(HttpServletRequest request) {
        return parseTenantIdHeader(request.getHeader(HEADER_TENANT_ID), HEADER_TENANT_ID);
    }

    /**
     * 获得访问的租户编号，从 header 中
     * 考虑到其它 framework 组件也会使用到租户编号，所以不得不放在 WebFrameworkUtils 统一提供
     *
     * @param request 请求
     * @return 租户编号
     */
    public static Long getVisitTenantId(HttpServletRequest request) {
        return parseTenantIdHeader(request.getHeader(HEADER_VISIT_TENANT_ID), HEADER_VISIT_TENANT_ID);
    }

    /**
     * 严格解析租户类上下文头（ZS-SEC-008）。
     *
     * <p>规则：缺失 / 空白 → {@code null}（视为未传递，保持既有兜底语义）；present-but-malformed
     * （含符号、小数点、十六进制、科学计数、内嵌空白、非数字，或十进制数字串超出 {@link Long} 范围）
     * → 抛受控 {@link cn.zszj.framework.common.exception.ServiceException}（业务码 400），由统一异常出口稳定拒绝、
     * 不泄露栈。替代原 {@code NumberUtil.isNumber(...) + Long.valueOf(...)} 组合——后者对 {@code "1.5"}、{@code "0x1F"}、
     * {@code "1e5"} 等 isNumber 通过但 Long.valueOf 失败的输入会抛 {@link NumberFormatException}，
     * 在 MVC 外的过滤器中逃逸为容器 500 + 栈泄露。
     *
     * @param rawValue   上下文头原始值
     * @param headerName 头名，仅用于错误提示
     * @return 合法租户编号；缺失 / 空白返回 {@code null}
     */
    private static Long parseTenantIdHeader(String rawValue, String headerName) {
        if (rawValue == null) {
            return null;
        }
        String value = rawValue.trim();
        if (value.isEmpty()) {
            return null;
        }
        // 仅接受纯十进制数字（拒绝符号、小数点、十六进制、科学计数、内嵌空白等一切非数字字符）
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                throw exception0(GlobalErrorCodeConstants.BAD_REQUEST.getCode(),
                        StrUtil.format("请求头 {} 格式非法，必须为十进制非负整数", headerName));
            }
        }
        try {
            // 纯数字串但超出 Long 范围（如 99999999999999999999）→ NumberFormatException → 受控 400
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw exception0(GlobalErrorCodeConstants.BAD_REQUEST.getCode(),
                    StrUtil.format("请求头 {} 超出取值范围", headerName));
        }
    }

    public static void setLoginUserId(ServletRequest request, Long userId) {
        request.setAttribute(REQUEST_ATTRIBUTE_LOGIN_USER_ID, userId);
    }

    /**
     * 设置用户类型
     *
     * @param request 请求
     * @param userType 用户类型
     */
    public static void setLoginUserType(ServletRequest request, Integer userType) {
        request.setAttribute(REQUEST_ATTRIBUTE_LOGIN_USER_TYPE, userType);
    }

    /**
     * 获得当前用户的编号，从请求中
     * 注意：该方法仅限于 framework 框架使用！！！
     *
     * @param request 请求
     * @return 用户编号
     */
    public static Long getLoginUserId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return (Long) request.getAttribute(REQUEST_ATTRIBUTE_LOGIN_USER_ID);
    }

    /**
     * 获得当前用户的类型
     * 注意：该方法仅限于 web 相关的 framework 组件使用！！！
     *
     * @param request 请求
     * @return 用户编号
     */
    public static Integer getLoginUserType(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        // 1. 优先，从 Attribute 中获取
        Integer userType = (Integer) request.getAttribute(REQUEST_ATTRIBUTE_LOGIN_USER_TYPE);
        if (userType != null) {
            return userType;
        }
        // 2. 其次，基于 URL 前缀的约定
        if (request.getServletPath().startsWith(properties.getAdminApi().getPrefix())) {
            return UserTypeEnum.ADMIN.getValue();
        }
        if (request.getServletPath().startsWith(properties.getAppApi().getPrefix())) {
            return UserTypeEnum.MEMBER.getValue();
        }
        return null;
    }

    public static Integer getLoginUserType() {
        HttpServletRequest request = getRequest();
        return getLoginUserType(request);
    }

    public static Long getLoginUserId() {
        HttpServletRequest request = getRequest();
        return getLoginUserId(request);
    }

    public static Integer getTerminal() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return TerminalEnum.UNKNOWN.getTerminal();
        }
        String terminalValue = request.getHeader(HEADER_TERMINAL);
        return NumberUtil.parseInt(terminalValue, TerminalEnum.UNKNOWN.getTerminal());
    }

    public static void setCommonResult(ServletRequest request, CommonResult<?> result) {
        request.setAttribute(REQUEST_ATTRIBUTE_COMMON_RESULT, result);
    }

    public static CommonResult<?> getCommonResult(ServletRequest request) {
        return (CommonResult<?>) request.getAttribute(REQUEST_ATTRIBUTE_COMMON_RESULT);
    }

    /**
     * 统一写出 {@link CommonResult} 响应，并登记到请求属性，供 API 访问日志按业务码记录结果。
     *
     * 底座 API 契约：HTTP 传输层固定 200，业务结果由 {@link CommonResult#getCode()} 表达（业务码刻意镜像 HTTP 语义，
     * 如 401/403/429/500）。因此本方法不修改 HTTP 状态码，仅统一 filter 直接写出（认证 401 / 权限 403 / 租户 / Token /
     * Demo / 加密）的出口：这些路径未经 MVC 的 GlobalResponseBodyHandler 登记 common_result，会导致访问日志误记为成功。
     *
     * @param request  请求，用于登记 common_result 属性（供 ApiAccessLogFilter 读取）
     * @param response 响应，写出 JSON 体
     * @param result   业务结果
     */
    public static void writeJSON(HttpServletRequest request, HttpServletResponse response, CommonResult<?> result) {
        setCommonResult(request, result);
        ServletUtils.writeJSON(response, result);
    }

    @SuppressWarnings("PatternVariableCanBeUsed")
    public static HttpServletRequest getRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (!(requestAttributes instanceof ServletRequestAttributes)) {
            return null;
        }
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) requestAttributes;
        return servletRequestAttributes.getRequest();
    }

}
package cn.zszj.framework.idempotent.core.keyresolver.impl;

import cn.hutool.core.util.ArrayUtil;
import cn.zszj.framework.idempotent.core.annotation.Idempotent;
import cn.zszj.framework.idempotent.core.keyresolver.IdempotentKeyResolver;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;

/**
 * 基于 Spring EL 表达式，
 *
 * @author 芋道源码
 */
public class ExpressionIdempotentKeyResolver implements IdempotentKeyResolver {

    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    private final ExpressionParser expressionParser = new SpelExpressionParser();

    @Override
    public String resolver(JoinPoint joinPoint, Idempotent idempotent) {
        // 获得被拦截方法参数名列表
        Method method = getMethod(joinPoint);
        Object[] args = joinPoint.getArgs();
        String[] parameterNames = this.parameterNameDiscoverer.getParameterNames(method);
        // 准备 Spring EL 表达式解析的上下文
        StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
        if (ArrayUtil.isNotEmpty(parameterNames)) {
            for (int i = 0; i < parameterNames.length; i++) {
                evaluationContext.setVariable(parameterNames[i], args[i]);
            }
        }

        // 解析参数
        Expression expression = expressionParser.parseExpression(idempotent.keyArg());
        return expression.getValue(evaluationContext, String.class);
    }

    private static Method getMethod(JoinPoint point) {
        // 处理，声明在类上的情况
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        if (!method.getDeclaringClass().isInterface()) {
            return method;
        }

        // 处理，声明在接口上的情况
        try {
            return point.getTarget().getClass().getDeclaredMethod(
                    point.getSignature().getName(), method.getParameterTypes());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

}
3b5d3b8f:services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/idem
potent/package-info.java:8: *  1. 我们去掉了 @Idempotent 注解的 delKey 属性。原因是，本质上 delKey 为 true 时，实现的是分布式锁的能力

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'git show 3b5d3b8f:services/zhongshu-core/zsz
j-framework/zszj-spring-boot-starter-protection/src/test/java/cn/zszj/framework/idempotent/core/aop/IdempotentAspectTes
t.java; git show 3b5d3b8f:services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-biz-tenant/src/main/java/cn/zs
zj/framework/tenant/core/security/TenantSecurityWebFilter.java; Get-ChildItem .tmp-sec011 -Force; Get-Command java,jshe
ll,mvn -ErrorAction SilentlyContinue | Select-Object Name,Source; Get-ChildItem services/zhongshu-core/zszj-framework/z
szj-spring-boot-starter-protection/target -ErrorAction SilentlyContinue | Select-Object Name; Get-ChildItem services/zh
ongshu-core/zszj-framework/zszj-common/target -ErrorAction SilentlyContinue | Select-Object Name' in E:\众墅之家AI赋能平台底座
 succeeded in 64ms:

Name                            
----                            
classes                         
generated-sources               
generated-test-sources          
maven-status                    
surefire-reports                
test-classes                    
classes                         
generated-sources               
generated-test-sources          
maven-archiver                  
maven-status                    
surefire-reports                
test-classes                    
zszj-common-2026.08-SNAPSHOT.jar



exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "Get-ChildItem tools -Force | Select-Object M
ode,Name; Get-ChildItem .tmp-sec011 -Force | Select-Object Mode,Name; Get-ChildItem services/zhongshu-core -Force | Sel
ect-Object Mode,Name; Get-ChildItem 'C:/Program Files/Java' -ErrorAction SilentlyContinue | Select-Object FullName; git
 show 3b5d3b8f:services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framework/common/util/json/JsonU
tils.java; git show 3b5d3b8f:services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn
/zszj/framework/idempotent/core/annotation/Idempotent.java" in E:\众墅之家AI赋能平台底座
 succeeded in 142ms:

package cn.zszj.framework.common.util.json;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.zszj.framework.common.util.json.databind.TimestampLocalDateTimeDeserializer;
import cn.zszj.framework.common.util.json.databind.TimestampLocalDateTimeSerializer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JSON 工具类
 *
 * @author 芋道源码
 */
@Slf4j
public class JsonUtils {

    @Getter
    private static ObjectMapper objectMapper = buildObjectMapper();

    private static ObjectMapper buildObjectMapper() {
        SimpleModule simpleModule = new JavaTimeModule()
                // 解决 LocalDateTime 的序列化
                .addSerializer(LocalDateTime.class, TimestampLocalDateTimeSerializer.INSTANCE)
                .addDeserializer(LocalDateTime.class, TimestampLocalDateTimeDeserializer.INSTANCE);
        return JsonMapper.builder()
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .defaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include
.NON_NULL))
                .addModule(simpleModule)
                .build();
    }

    /**
     * 初始化 objectMapper 属性
     * <p>
     * 通过这样的方式，使用 Spring 创建的 ObjectMapper Bean
     *
     * @param objectMapper ObjectMapper 对象
     */
    public static void init(ObjectMapper objectMapper) {
        JsonUtils.objectMapper = objectMapper;
    }

    @SneakyThrows
    public static String toJsonString(Object object) {
        return objectMapper.writeValueAsString(object);
    }

    @SneakyThrows
    public static byte[] toJsonByte(Object object) {
        return objectMapper.writeValueAsBytes(object);
    }

    @SneakyThrows
    public static String toJsonPrettyString(Object object) {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
    }

    public static <T> T parseObject(String text, Class<T> clazz) {
        if (StrUtil.isEmpty(text)) {
            return null;
        }
        try {
            return objectMapper.readValue(text, clazz);
        } catch (JacksonException e) {
            log.error("json parse err,json:{}", text, e);
            throw new RuntimeException(e);
        }
    }

    public static <T> T parseObject(String text, String path, Class<T> clazz) {
        if (StrUtil.isEmpty(text)) {
            return null;
        }
        try {
            JsonNode treeNode = objectMapper.readTree(text);
            JsonNode pathNode = treeNode.path(path);
            return objectMapper.readValue(pathNode.toString(), clazz);
        } catch (JacksonException e) {
            log.error("json parse err,json:{}", text, e);
            throw new RuntimeException(e);
        }
    }

    public static <T> T parseObject(String text, Type type) {
        if (StrUtil.isEmpty(text)) {
            return null;
        }
        try {
            return objectMapper.readValue(text, objectMapper.getTypeFactory().constructType(type));
        } catch (JacksonException e) {
            log.error("json parse err,json:{}", text, e);
            throw new RuntimeException(e);
        }
    }

    public static <T> T parseObject(byte[] text, Type type) {
        if (ArrayUtil.isEmpty(text)) {
            return null;
        }
        try {
            return objectMapper.readValue(text, objectMapper.getTypeFactory().constructType(type));
        } catch (IOException e) {
            log.error("json parse err,json:{}", text, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 将字符串解析成指定类型的对象
     * 使用 {@link #parseObject(String, Class)} 时，在@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS) 的场景下，
     * 如果 text 没有 class 属性，则会报错。此时，使用这个方法，可以解决。
     *
     * @param text 字符串
     * @param clazz 类型
     * @return 对象
     */
    public static <T> T parseObject2(String text, Class<T> clazz) {
        if (StrUtil.isEmpty(text)) {
            return null;
        }
        return JSONUtil.toBean(text, clazz);
    }

    public static <T> T parseObject(byte[] bytes, Class<T> clazz) {
        if (ArrayUtil.isEmpty(bytes)) {
            return null;
        }
        try {
            return objectMapper.readValue(bytes, clazz);
        } catch (IOException e) {
            log.error("json parse err,json:{}", bytes, e);
            throw new RuntimeException(e);
        }
    }

    public static <T> T parseObject(String text, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(text, typeReference);
        } catch (JacksonException e) {
            log.error("json parse err,json:{}", text, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 解析 JSON 字符串成指定类型的对象，如果解析失败，则返回 null
     *
     * @param text 字符串
     * @param typeReference 类型引用
     * @return 指定类型的对象
     */
    public static <T> T parseObjectQuietly(String text, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(text, typeReference);
        } catch (JacksonException e) {
            return null;
        }
    }

    /**
     * 解析 JSON 字符串成 Map，空字符串或解析失败返回 null
     *
     * @param text JSON 字符串
     * @return Map 对象
     */
    public static Map<String, Object> parseMap(String text) {
        if (StrUtil.isEmpty(text)) {
            return null;
        }
        try {
            return objectMapper.readValue(text, new TypeReference<Map<String, Object>>() {});
        } catch (JacksonException e) {
            return null;
        }
    }

    /**
     * 解析 JSON 字符串成指定类型的对象，如果解析失败，则返回 null
     *
     * @param text 字符串
     * @param clazz 类型
     * @return 指定类型的对象
     */
    public static <T> T parseObjectQuietly(String text, Class<T> clazz) {
        if (StrUtil.isEmpty(text)) {
            return null;
        }
        try {
            return objectMapper.readValue(text, clazz);
        } catch (JacksonException e) {
            return null;
        }
    }

    public static <T> List<T> parseArray(String text, Class<T> clazz) {
        if (StrUtil.isEmpty(text)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(text, objectMapper.getTypeFactory().constructCollectionType(List.class, clazz
));
        } catch (JacksonException e) {
            log.error("json parse err,json:{}", text, e);
            throw new RuntimeException(e);
        }
    }

    public static <T> List<T> parseArray(String text, String path, Class<T> clazz) {
        if (StrUtil.isEmpty(text)) {
            return null;
        }
        try {
            JsonNode treeNode = objectMapper.readTree(text);
            JsonNode pathNode = treeNode.path(path);
            return objectMapper.readValue(pathNode.toString(), objectMapper.getTypeFactory().constructCollectionType(Li
st.class, clazz));
        } catch (JacksonException e) {
            log.error("json parse err,json:{}", text, e);
            throw new RuntimeException(e);
        }
    }

    public static JsonNode parseTree(String text) {
        try {
            return objectMapper.readTree(text);
        } catch (JacksonException e) {
            log.error("json parse err,json:{}", text, e);
            throw new RuntimeException(e);
        }
    }

    public static JsonNode parseTree(byte[] text) {
        try {
            return objectMapper.readTree(text);
        } catch (IOException e) {
            log.error("json parse err,json:{}", text, e);
            throw new RuntimeException(e);
        }
    }

    public static String getText(JsonNode node, String fieldName) {
        if (node == null) {
            return null;
        }
        JsonNode value = node.get(fieldName);
        return value != null && !value.isNull() ? value.asText() : null;
    }

    public static boolean isJson(String text) {
        return JSONUtil.isTypeJSON(text);
    }

    /**
     * 判断字符串是否为 JSON 类型的字符串
     * @param str 字符串
     */
    public static boolean isJsonObject(String str) {
        return JSONUtil.isTypeJSONObject(str);
    }

    /**
     * 将 Object 转换为目标类型
     * <p>
     * 避免先转 jsonString 再 parseObject 的性能损耗
     *
     * @param obj   源对象（可以是 Map、POJO 等）
     * @param clazz 目标类型
     * @return 转换后的对象
     */
    public static <T> T convertObject(Object obj, Class<T> clazz) {
        if (obj == null) {
            return null;
        }
        if (clazz.isInstance(obj)) {
            return clazz.cast(obj);
        }
        return objectMapper.convertValue(obj, clazz);
    }

    /**
     * 将 Object 转换为目标类型（支持泛型）
     *
     * @param obj           源对象
     * @param typeReference 目标类型引用
     * @return 转换后的对象
     */
    public static <T> T convertObject(Object obj, TypeReference<T> typeReference) {
        if (obj == null) {
            return null;
        }
        return objectMapper.convertValue(obj, typeReference);
    }

    /**
     * 将 Object 转换为 List 类型
     * <p>
     * 避免先转 jsonString 再 parseArray 的性能损耗
     *
     * @param obj   源对象（可以是 List、数组等）
     * @param clazz 目标元素类型
     * @return 转换后的 List
     */
    public static <T> List<T> convertList(Object obj, Class<T> clazz) {
        if (obj == null) {
            return new ArrayList<>();
        }
        return objectMapper.convertValue(obj, objectMapper.getTypeFactory().constructCollectionType(List.class, clazz))
;
    }

}
package cn.zszj.framework.idempotent.core.annotation;

import cn.zszj.framework.idempotent.core.keyresolver.impl.DefaultIdempotentKeyResolver;
import cn.zszj.framework.idempotent.core.keyresolver.IdempotentKeyResolver;
import cn.zszj.framework.idempotent.core.keyresolver.impl.ExpressionIdempotentKeyResolver;
import cn.zszj.framework.idempotent.core.keyresolver.impl.UserIdempotentKeyResolver;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 幂等注解
 *
 * @author 芋道源码
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /**
     * 幂等的超时时间，默认为 1 秒
     *
     * 注意，如果执行时间超过它，请求还是会进来
     */
    int timeout() default 1;
    /**
     * 时间单位，默认为 SECONDS 秒
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 提示信息，正在执行中的提示
     */
    String message() default "重复请求，请稍后重试";

    /**
     * 使用的 Key 解析器
     *
     * @see DefaultIdempotentKeyResolver 全局级别
     * @see UserIdempotentKeyResolver 用户级别
     * @see ExpressionIdempotentKeyResolver 自定义表达式，通过 {@link #keyArg()} 计算
     */
    Class<? extends IdempotentKeyResolver> keyResolver() default DefaultIdempotentKeyResolver.class;
    /**
     * 使用的 Key 参数
     */
    String keyArg() default "";

    /**
     * 删除 Key，当发生异常时候
     *
     * 问题：为什么发生异常时，需要删除 Key 呢？
     * 回答：发生异常时，说明业务发生错误，此时需要删除 Key，避免下次请求无法正常执行。
     *
     * 问题：为什么不搞 deleteWhenSuccess 执行成功时，需要删除 Key 呢？
     * 回答：这种情况下，本质上是分布式锁，推荐使用 @Lock4j 注解
     */
    boolean deleteKeyWhenException() default true;

}
Mode   Name              
----   ----              
d----- apache-maven-3.9.9
d----- jdk-17.0.20.1+1   
-a---- env.sh            
-a---- jdk17.zip         
-a---- maven.zip         
d----- .gitee            
d----- .github           
d----- .image            
d----- docs              
d----- script            
d----- sql               
d----- zszj-dependencies 
d----- zszj-framework    
d----- zszj-module-ai    
d----- zszj-module-bpm   
d----- zszj-module-crm   
d----- zszj-module-erp   
d----- zszj-module-fms   
d----- zszj-module-hrm   
d----- zszj-module-im    
d----- zszj-module-infra 
d----- zszj-module-iot   
d----- zszj-module-mall  
d----- zszj-module-member
d----- zszj-module-mes   
d----- zszj-module-mp    
d----- zszj-module-pay   
d----- zszj-module-pms   
d----- zszj-module-report
d----- zszj-module-system
d----- zszj-module-wms   
d----- zszj-server       
d----- zszj-ui           
-a---- .flattened-pom.xml
-a---- .gitignore        
-a---- LICENSE           
-a---- lombok.config     
-a---- pom.xml           
-a---- README.md         



exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command '$x = [xml](Get-Content -Raw services/zhongsh
u-core/zszj-framework/zszj-spring-boot-starter-protection/target/surefire-reports/TEST-cn.zszj.framework.idempotent.cor
e.aop.IdempotentAspectTest.xml); $x.testsuite.properties.property | Where-Object {$_.name -in @('"'java.class.path','ja
va.home')} | Format-List; git diff 3b5d3b8f HEAD -- services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-prot
ection; git show 3b5d3b8f:services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framework/common/util
/servlet/ServletUtils.java | Select-String -Pattern 'getRequest' -Context 2,10; git ls-files '*instructions*' '*AGENT*'
 '*copilot*' '*RULE*' '*rules*'" in E:\众墅之家AI赋能平台底座
 succeeded in 395ms:


name  : java.class.path
value : E:\众墅之家AI赋能平台底座\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\tar
        get\test-classes;E:\众墅之家AI赋能平台底座\services\zhongshu-core\zszj-framework\zszj-spring-boot-start
        er-protection\target\classes;E:\众墅之家AI赋能平台底座\services\zhongshu-core\zszj-framework\zszj-sprin
        g-boot-starter-web\target\classes;E:\众墅之家AI赋能平台底座\services\zhongshu-core\zszj-framework\zszj-
        common\target\classes;C:\Users\Administrator\.m2\repository\io\opentelemetry\opentelemetry-api\1.65.0\opentelem
        etry-api-1.65.0.jar;C:\Users\Administrator\.m2\repository\io\opentelemetry\opentelemetry-context\1.49.0\opentel
        emetry-context-1.49.0.jar;C:\Users\Administrator\.m2\repository\org\projectlombok\lombok\1.18.46\lombok-1.18.46
        .jar;C:\Users\Administrator\.m2\repository\org\mapstruct\mapstruct\1.6.3\mapstruct-1.6.3.jar;C:\Users\Administr
        ator\.m2\repository\org\mapstruct\mapstruct-jdk8\1.6.3\mapstruct-jdk8-1.6.3.jar;C:\Users\Administrator\.m2\repo
        sitory\org\mapstruct\mapstruct-processor\1.6.3\mapstruct-processor-1.6.3.jar;C:\Users\Administrator\.m2\reposit
        ory\cn\hutool\hutool-all\5.8.47\hutool-all-5.8.47.jar;C:\Users\Administrator\.m2\repository\com\alibaba\transmi
        ttable-thread-local\2.14.5\transmittable-thread-local-2.14.5.jar;C:\Users\Administrator\.m2\repository\com\alib
        aba\fastjson\2.0.64\fastjson-2.0.64.jar;C:\Users\Administrator\.m2\repository\com\alibaba\fastjson2\fastjson2-e
        xtension\2.0.64\fastjson2-extension-2.0.64.jar;C:\Users\Administrator\.m2\repository\com\alibaba\fastjson2\fast
        json2\2.0.64\fastjson2-2.0.64.jar;C:\Users\Administrator\.m2\repository\org\dromara\easy-trans-anno\3.1.8\easy-
        trans-anno-3.1.8.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-web\3.5
        .15\spring-boot-starter-web-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-bo
        ot-starter\3.5.15\spring-boot-starter-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot
        \spring-boot\3.5.15\spring-boot-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\sprin
        g-boot-autoconfigure\3.5.15\spring-boot-autoconfigure-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\spri
        ngframework\boot\spring-boot-starter-logging\3.5.15\spring-boot-starter-logging-3.5.15.jar;C:\Users\Administrat
        or\.m2\repository\ch\qos\logback\logback-classic\1.5.34\logback-classic-1.5.34.jar;C:\Users\Administrator\.m2\r
        epository\ch\qos\logback\logback-core\1.5.34\logback-core-1.5.34.jar;C:\Users\Administrator\.m2\repository\org\
        apache\logging\log4j\log4j-to-slf4j\2.24.3\log4j-to-slf4j-2.24.3.jar;C:\Users\Administrator\.m2\repository\org\
        apache\logging\log4j\log4j-api\2.24.3\log4j-api-2.24.3.jar;C:\Users\Administrator\.m2\repository\org\slf4j\jul-
        to-slf4j\2.0.18\jul-to-slf4j-2.0.18.jar;C:\Users\Administrator\.m2\repository\jakarta\annotation\jakarta.annota
        tion-api\2.1.1\jakarta.annotation-api-2.1.1.jar;C:\Users\Administrator\.m2\repository\org\yaml\snakeyaml\2.4\sn
        akeyaml-2.4.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-json\3.5.15\
        spring-boot-starter-json-3.5.15.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\datatype\jackso
        n-datatype-jdk8\2.21.4\jackson-datatype-jdk8-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jac
        kson\module\jackson-module-parameter-names\2.21.4\jackson-module-parameter-names-2.21.4.jar;C:\Users\Administra
        tor\.m2\repository\org\springframework\boot\spring-boot-starter-tomcat\3.5.15\spring-boot-starter-tomcat-3.5.15
        .jar;C:\Users\Administrator\.m2\repository\org\apache\tomcat\embed\tomcat-embed-core\10.1.55\tomcat-embed-core-
        10.1.55.jar;C:\Users\Administrator\.m2\repository\org\apache\tomcat\embed\tomcat-embed-el\10.1.55\tomcat-embed-
        el-10.1.55.jar;C:\Users\Administrator\.m2\repository\org\apache\tomcat\embed\tomcat-embed-websocket\10.1.55\tom
        cat-embed-websocket-10.1.55.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-web\6.2.19\spr
        ing-web-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-beans\6.2.19\spring-beans-6
        .2.19.jar;C:\Users\Administrator\.m2\repository\io\micrometer\micrometer-observation\1.15.12\micrometer-observa
        tion-1.15.12.jar;C:\Users\Administrator\.m2\repository\io\micrometer\micrometer-commons\1.15.12\micrometer-comm
        ons-1.15.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-webmvc\6.2.19\spring-webmvc-6.
        2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-aop\6.2.19\spring-aop-6.2.19.jar;C:\U
        sers\Administrator\.m2\repository\org\springframework\spring-context\6.2.19\spring-context-6.2.19.jar;C:\Users\
        Administrator\.m2\repository\org\springframework\spring-expression\6.2.19\spring-expression-6.2.19.jar;C:\Users
        \Administrator\.m2\repository\com\github\xiaoymin\knife4j-openapi3-jakarta-spring-boot-starter\4.5.0\knife4j-op
        enapi3-jakarta-spring-boot-starter-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\github\xiaoymin\knife4j-
        core\4.5.0\knife4j-core-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\github\xiaoymin\knife4j-openapi3-ui
        \4.5.0\knife4j-openapi3-ui-4.5.0.jar;C:\Users\Administrator\.m2\repository\org\springdoc\springdoc-openapi-star
        ter-webmvc-ui\2.8.17\springdoc-openapi-starter-webmvc-ui-2.8.17.jar;C:\Users\Administrator\.m2\repository\org\s
        pringdoc\springdoc-openapi-starter-webmvc-api\2.8.17\springdoc-openapi-starter-webmvc-api-2.8.17.jar;C:\Users\A
        dministrator\.m2\repository\org\springdoc\springdoc-openapi-starter-common\2.8.17\springdoc-openapi-starter-com
        mon-2.8.17.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-validation\3.
        5.15\spring-boot-starter-validation-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\hibernate\validator\hi
        bernate-validator\8.0.3.Final\hibernate-validator-8.0.3.Final.jar;C:\Users\Administrator\.m2\repository\org\jbo
        ss\logging\jboss-logging\3.6.3.Final\jboss-logging-3.6.3.Final.jar;C:\Users\Administrator\.m2\repository\com\fa
        sterxml\classmate\1.7.3\classmate-1.7.3.jar;C:\Users\Administrator\.m2\repository\io\swagger\core\v3\swagger-co
        re-jakarta\2.2.47\swagger-core-jakarta-2.2.47.jar;C:\Users\Administrator\.m2\repository\io\swagger\core\v3\swag
        ger-annotations-jakarta\2.2.47\swagger-annotations-jakarta-2.2.47.jar;C:\Users\Administrator\.m2\repository\io\
        swagger\core\v3\swagger-models-jakarta\2.2.47\swagger-models-jakarta-2.2.47.jar;C:\Users\Administrator\.m2\repo
        sitory\com\fasterxml\jackson\dataformat\jackson-dataformat-yaml\2.21.4\jackson-dataformat-yaml-2.21.4.jar;C:\Us
        ers\Administrator\.m2\repository\org\webjars\swagger-ui\5.32.2\swagger-ui-5.32.2.jar;C:\Users\Administrator\.m2
        \repository\org\webjars\webjars-locator-lite\1.1.3\webjars-locator-lite-1.1.3.jar;C:\Users\Administrator\.m2\re
        pository\org\jspecify\jspecify\1.0.0\jspecify-1.0.0.jar;C:\Users\Administrator\.m2\repository\org\jsoup\jsoup\1
        .23.2\jsoup-1.23.2.jar;E:\众墅之家AI赋能平台底座\services\zhongshu-core\zszj-framework\zszj-spring-boot
        -starter-redis\target\classes;C:\Users\Administrator\.m2\repository\org\redisson\redisson-spring-boot-starter\4
        .7.0\redisson-spring-boot-starter-4.7.0.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spri
        ng-boot-starter-data-redis\3.5.15\spring-boot-starter-data-redis-3.5.15.jar;C:\Users\Administrator\.m2\reposito
        ry\org\redisson\redisson\4.7.0\redisson-4.7.0.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-common\4
        .2.17.Final\netty-common-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-codec\4.2.17.Fin
        al\netty-codec-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-codec-base\4.2.17.Final\ne
        tty-codec-base-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-codec-compression\4.2.17.F
        inal\netty-codec-compression-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-codec-protob
        uf\4.2.17.Final\netty-codec-protobuf-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-code
        c-marshalling\4.2.17.Final\netty-codec-marshalling-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\ne
        tty\netty-buffer\4.2.17.Final\netty-buffer-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\nett
        y-transport\4.2.17.Final\netty-transport-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-
        resolver\4.2.17.Final\netty-resolver-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-reso
        lver-dns\4.2.17.Final\netty-resolver-dns-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-
        codec-dns\4.2.17.Final\netty-codec-dns-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-ha
        ndler\4.2.17.Final\netty-handler-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-transpor
        t-native-unix-common\4.2.17.Final\netty-transport-native-unix-common-4.2.17.Final.jar;C:\Users\Administrator\.m
        2\repository\javax\cache\cache-api\1.1.1\cache-api-1.1.1.jar;C:\Users\Administrator\.m2\repository\io\projectre
        actor\reactor-core\3.7.19\reactor-core-3.7.19.jar;C:\Users\Administrator\.m2\repository\org\reactivestreams\rea
        ctive-streams\1.0.4\reactive-streams-1.0.4.jar;C:\Users\Administrator\.m2\repository\io\reactivex\rxjava3\rxjav
        a\3.1.12\rxjava-3.1.12.jar;C:\Users\Administrator\.m2\repository\com\esotericsoftware\kryo\5.6.2\kryo-5.6.2.jar
        ;C:\Users\Administrator\.m2\repository\com\esotericsoftware\reflectasm\1.11.9\reflectasm-1.11.9.jar;C:\Users\Ad
        ministrator\.m2\repository\com\esotericsoftware\minlog\1.3.1\minlog-1.3.1.jar;C:\Users\Administrator\.m2\reposi
        tory\net\bytebuddy\byte-buddy\1.17.8\byte-buddy-1.17.8.jar;C:\Users\Administrator\.m2\repository\org\jodd\jodd-
        util\6.3.0\jodd-util-6.3.0.jar;C:\Users\Administrator\.m2\repository\org\redisson\redisson-spring-data-35\4.7.0
        \redisson-spring-data-35-4.7.0.jar;C:\Users\Administrator\.m2\repository\org\springframework\data\spring-data-r
        edis\3.5.12\spring-data-redis-3.5.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\data\spring-
        data-keyvalue\3.5.12\spring-data-keyvalue-3.5.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\
        data\spring-data-commons\3.5.12\spring-data-commons-3.5.12.jar;C:\Users\Administrator\.m2\repository\org\spring
        framework\spring-tx\6.2.19\spring-tx-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\sprin
        g-oxm\6.2.19\spring-oxm-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-s
        tarter-cache\3.5.15\spring-boot-starter-cache-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframew
        ork\spring-context-support\6.2.19\spring-context-support-6.2.19.jar;C:\Users\Administrator\.m2\repository\com\f
        asterxml\jackson\datatype\jackson-datatype-jsr310\2.21.4\jackson-datatype-jsr310-2.21.4.jar;C:\Users\Administra
        tor\.m2\repository\com\fasterxml\jackson\core\jackson-annotations\2.21\jackson-annotations-2.21.jar;C:\Users\Ad
        ministrator\.m2\repository\com\fasterxml\jackson\core\jackson-core\2.21.4\jackson-core-2.21.4.jar;C:\Users\Admi
        nistrator\.m2\repository\com\fasterxml\jackson\core\jackson-databind\2.21.4\jackson-databind-2.21.4.jar;C:\User
        s\Administrator\.m2\repository\com\baomidou\lock4j-redisson-spring-boot-starter\2.2.7\lock4j-redisson-spring-bo
        ot-starter-2.2.7.jar;C:\Users\Administrator\.m2\repository\com\baomidou\lock4j-core\2.2.7\lock4j-core-2.2.7.jar
        ;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-aop\3.5.15\spring-boot-star
        ter-aop-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\aspectj\aspectjweaver\1.9.25.1\aspectjweaver-1.9.2
        5.1.jar;E:\众墅之家AI赋能平台底座\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-test\t
        arget\classes;E:\众墅之家AI赋能平台底座\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-
        mybatis\target\classes;C:\Users\Administrator\.m2\repository\com\alibaba\druid-spring-boot-3-starter\1.2.28\dru
        id-spring-boot-3-starter-1.2.28.jar;C:\Users\Administrator\.m2\repository\com\alibaba\druid\1.2.28\druid-1.2.28
        .jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-spring-boot3-starter\3.5.17\mybatis-plus-s
        pring-boot3-starter-3.5.17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus\3.5.17\mybatis-p
        lus-3.5.17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-core\3.5.17\mybatis-plus-core-3.
        5.17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-annotation\3.5.17\mybatis-plus-annotat
        ion-3.5.17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-spring\3.5.17\mybatis-plus-sprin
        g-3.5.17.jar;C:\Users\Administrator\.m2\repository\org\mybatis\mybatis\3.5.19\mybatis-3.5.19.jar;C:\Users\Admin
        istrator\.m2\repository\org\mybatis\mybatis-spring\3.0.5\mybatis-spring-3.0.5.jar;C:\Users\Administrator\.m2\re
        pository\com\baomidou\mybatis-plus-spring-boot-autoconfigure\3.5.17\mybatis-plus-spring-boot-autoconfigure-3.5.
        17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-spring-boot-native-image\3.5.17\mybatis-
        plus-spring-boot-native-image-3.5.17.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-
        boot-starter-jdbc\3.5.15\spring-boot-starter-jdbc-3.5.15.jar;C:\Users\Administrator\.m2\repository\com\zaxxer\H
        ikariCP\6.3.3\HikariCP-6.3.3.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-jdbc\6.2.19\s
        pring-jdbc-6.2.19.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-jsqlparser\3.5.17\mybatis
        -plus-jsqlparser-3.5.17.jar;C:\Users\Administrator\.m2\repository\com\github\jsqlparser\jsqlparser\5.2\jsqlpars
        er-5.2.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-jsqlparser-common\3.5.17\mybatis-plu
        s-jsqlparser-common-3.5.17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-extension\3.5.17
        \mybatis-plus-extension-3.5.17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\dynamic-datasource-spring
        -boot3-starter\4.5.0\dynamic-datasource-spring-boot3-starter-4.5.0.jar;C:\Users\Administrator\.m2\repository\co
        m\baomidou\dynamic-datasource-spring-boot-common\4.5.0\dynamic-datasource-spring-boot-common-4.5.0.jar;C:\Users
        \Administrator\.m2\repository\com\baomidou\dynamic-datasource-spring\4.5.0\dynamic-datasource-spring-4.5.0.jar;
        C:\Users\Administrator\.m2\repository\com\baomidou\dynamic-datasource-creator\4.5.0\dynamic-datasource-creator-
        4.5.0.jar;C:\Users\Administrator\.m2\repository\com\oracle\database\spring\oracle-spring-boot-starter-ucp\23.4.
        0\oracle-spring-boot-starter-ucp-23.4.0.jar;C:\Users\Administrator\.m2\repository\com\oracle\database\jdbc\ojdb
        c11\23.7.0.25.01\ojdbc11-23.7.0.25.01.jar;C:\Users\Administrator\.m2\repository\com\oracle\database\jdbc\ucp\23
        .7.0.25.01\ucp-23.7.0.25.01.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-join-bo
        ot-starter\1.5.9\mybatis-plus-join-boot-starter-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yuli
        chang\mybatis-plus-join-extension\1.5.9\mybatis-plus-join-extension-1.5.9.jar;C:\Users\Administrator\.m2\reposi
        tory\com\github\yulichang\mybatis-plus-join-core\1.5.9\mybatis-plus-join-core-1.5.9.jar;C:\Users\Administrator\
        .m2\repository\com\github\yulichang\mybatis-plus-join-annotation\1.5.9\mybatis-plus-join-annotation-1.5.9.jar;C
        :\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-join-adapter-base\1.5.9\mybatis-plus-joi
        n-adapter-base-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-join-adapter-j
        sqlparser\1.5.9\mybatis-plus-join-adapter-jsqlparser-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github
        \yulichang\mybatis-plus-join-wrapper-ext\1.5.9\mybatis-plus-join-wrapper-ext-1.5.9.jar;C:\Users\Administrator\.
        m2\repository\org\dromara\easy-trans-spring-boot-starter\3.1.8\easy-trans-spring-boot-starter-3.1.8.jar;C:\User
        s\Administrator\.m2\repository\org\dromara\easy-trans-service\3.1.8\easy-trans-service-3.1.8.jar;C:\Users\Admin
        istrator\.m2\repository\com\github\ben-manes\caffeine\caffeine\3.2.4\caffeine-3.2.4.jar;C:\Users\Administrator\
        .m2\repository\org\dromara\easy-trans-mybatis-plus-extend\3.1.8\easy-trans-mybatis-plus-extend-3.1.8.jar;C:\Use
        rs\Administrator\.m2\repository\org\mockito\mockito-inline\5.2.0\mockito-inline-5.2.0.jar;C:\Users\Administrato
        r\.m2\repository\org\mockito\mockito-core\5.17.0\mockito-core-5.17.0.jar;C:\Users\Administrator\.m2\repository\
        net\bytebuddy\byte-buddy-agent\1.17.8\byte-buddy-agent-1.17.8.jar;C:\Users\Administrator\.m2\repository\org\obj
        enesis\objenesis\3.3\objenesis-3.3.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-bo
        ot-starter-test\3.5.15\spring-boot-starter-test-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springfram
        ework\boot\spring-boot-test\3.5.15\spring-boot-test-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\spring
        framework\boot\spring-boot-test-autoconfigure\3.5.15\spring-boot-test-autoconfigure-3.5.15.jar;C:\Users\Adminis
        trator\.m2\repository\com\jayway\jsonpath\json-path\2.9.0\json-path-2.9.0.jar;C:\Users\Administrator\.m2\reposi
        tory\jakarta\xml\bind\jakarta.xml.bind-api\4.0.5\jakarta.xml.bind-api-4.0.5.jar;C:\Users\Administrator\.m2\repo
        sitory\jakarta\activation\jakarta.activation-api\2.1.4\jakarta.activation-api-2.1.4.jar;C:\Users\Administrator\
        .m2\repository\net\minidev\json-smart\2.5.2\json-smart-2.5.2.jar;C:\Users\Administrator\.m2\repository\net\mini
        dev\accessors-smart\2.5.2\accessors-smart-2.5.2.jar;C:\Users\Administrator\.m2\repository\org\assertj\assertj-c
        ore\3.27.7\assertj-core-3.27.7.jar;C:\Users\Administrator\.m2\repository\org\awaitility\awaitility\4.2.2\awaiti
        lity-4.2.2.jar;C:\Users\Administrator\.m2\repository\org\hamcrest\hamcrest\3.0\hamcrest-3.0.jar;C:\Users\Admini
        strator\.m2\repository\org\junit\jupiter\junit-jupiter\5.12.2\junit-jupiter-5.12.2.jar;C:\Users\Administrator\.
        m2\repository\org\junit\jupiter\junit-jupiter-api\5.12.2\junit-jupiter-api-5.12.2.jar;C:\Users\Administrator\.m
        2\repository\org\opentest4j\opentest4j\1.3.0\opentest4j-1.3.0.jar;C:\Users\Administrator\.m2\repository\org\jun
        it\platform\junit-platform-commons\1.12.2\junit-platform-commons-1.12.2.jar;C:\Users\Administrator\.m2\reposito
        ry\org\apiguardian\apiguardian-api\1.1.2\apiguardian-api-1.1.2.jar;C:\Users\Administrator\.m2\repository\org\ju
        nit\jupiter\junit-jupiter-params\5.12.2\junit-jupiter-params-5.12.2.jar;C:\Users\Administrator\.m2\repository\o
        rg\junit\jupiter\junit-jupiter-engine\5.12.2\junit-jupiter-engine-5.12.2.jar;C:\Users\Administrator\.m2\reposit
        ory\org\junit\platform\junit-platform-engine\1.12.2\junit-platform-engine-1.12.2.jar;C:\Users\Administrator\.m2
        \repository\org\mockito\mockito-junit-jupiter\5.17.0\mockito-junit-jupiter-5.17.0.jar;C:\Users\Administrator\.m
        2\repository\org\skyscreamer\jsonassert\1.5.3\jsonassert-1.5.3.jar;C:\Users\Administrator\.m2\repository\com\va
        adin\external\google\android-json\0.0.20131108.vaadin1\android-json-0.0.20131108.vaadin1.jar;C:\Users\Administr
        ator\.m2\repository\org\springframework\spring-core\6.2.19\spring-core-6.2.19.jar;C:\Users\Administrator\.m2\re
        pository\org\springframework\spring-jcl\6.2.19\spring-jcl-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\
        springframework\spring-test\6.2.19\spring-test-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\xmlunit\xml
        unit-core\2.10.4\xmlunit-core-2.10.4.jar;C:\Users\Administrator\.m2\repository\com\h2database\h2\2.3.232\h2-2.3
        .232.jar;C:\Users\Administrator\.m2\repository\com\github\fppt\jedis-mock\1.1.18\jedis-mock-1.1.18.jar;C:\Users
        \Administrator\.m2\repository\org\slf4j\slf4j-api\2.0.18\slf4j-api-2.0.18.jar;C:\Users\Administrator\.m2\reposi
        tory\org\luaj\luaj-jse\3.0.1\luaj-jse-3.0.1.jar;C:\Users\Administrator\.m2\repository\redis\clients\jedis\6.0.0
        \jedis-6.0.0.jar;C:\Users\Administrator\.m2\repository\org\apache\commons\commons-pool2\2.12.1\commons-pool2-2.
        12.1.jar;C:\Users\Administrator\.m2\repository\org\json\json\20250107\json-20250107.jar;C:\Users\Administrator\
        .m2\repository\com\google\code\gson\gson\2.13.2\gson-2.13.2.jar;C:\Users\Administrator\.m2\repository\com\googl
        e\errorprone\error_prone_annotations\2.41.0\error_prone_annotations-2.41.0.jar;C:\Users\Administrator\.m2\repos
        itory\redis\clients\authentication\redis-authx-core\0.1.1-beta2\redis-authx-core-0.1.1-beta2.jar;C:\Users\Admin
        istrator\.m2\repository\uk\co\jemos\podam\podam\8.0.2.RELEASE\podam-8.0.2.RELEASE.jar;C:\Users\Administrator\.m
        2\repository\net\jcip\jcip-annotations\1.0\jcip-annotations-1.0.jar;C:\Users\Administrator\.m2\repository\jakar
        ta\validation\jakarta.validation-api\3.0.2\jakarta.validation-api-3.0.2.jar;C:\Users\Administrator\.m2\reposito
        ry\org\apache\commons\commons-lang3\3.20.0\commons-lang3-3.20.0.jar;

name  : java.home
value : E:\众墅之家AI赋能平台底座\tools\jdk-17.0.20.1+1



diff --git a/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/
ratelimiter/core/annotation/RateLimiter.java b/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protectio
n/src/main/java/cn/zszj/framework/ratelimiter/core/annotation/RateLimiter.java
index d9443ec6..39488d5c 100644
--- a/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/ratelim
iter/core/annotation/RateLimiter.java
+++ b/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/ratelim
iter/core/annotation/RateLimiter.java
@@ -59,4 +59,14 @@ public @interface RateLimiter {
      */
     String keyArg() default "";
 
+    /**
+     * 端点级附加脱敏键：限流被拒绝时，除内置凭据根集（password / token / secret 等）外，
+     * 额外需要掩码的参数名或字段名（如短信验证码 code），避免其明文落入限流拒绝日志。
+     *
+     * <p>键名大小写不敏感，内部会归一（小写、去下划线 / 连字符）后精确匹配。
+     *
+     * @see cn.zszj.framework.common.util.log.LogSanitizeUtils
+     */
+    String[] maskKeys() default {};
+
 }
diff --git a/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/
ratelimiter/core/aop/RateLimiterAspect.java b/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection
/src/main/java/cn/zszj/framework/ratelimiter/core/aop/RateLimiterAspect.java
index 021ae85f..d1142d8b 100644
--- a/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/ratelim
iter/core/aop/RateLimiterAspect.java
+++ b/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/ratelim
iter/core/aop/RateLimiterAspect.java
@@ -10,10 +10,14 @@ import cn.zszj.framework.ratelimiter.core.keyresolver.RateLimiterKeyResolver;
 import cn.zszj.framework.ratelimiter.core.redis.RateLimiterRedisDAO;
 import lombok.extern.slf4j.Slf4j;
 import org.aspectj.lang.JoinPoint;
+import org.aspectj.lang.Signature;
 import org.aspectj.lang.annotation.Aspect;
 import org.aspectj.lang.annotation.Before;
+import org.aspectj.lang.reflect.MethodSignature;
 import org.springframework.util.Assert;
 
+import java.util.Collections;
+import java.util.LinkedHashMap;
 import java.util.List;
 import java.util.Map;
 
@@ -50,12 +54,38 @@ public class RateLimiterAspect {
         boolean success = rateLimiterRedisDAO.tryAcquire(key,
                 rateLimiter.count(), rateLimiter.time(), rateLimiter.timeUnit());
         if (!success) {
-            log.info("[beforePointCut][方法({}) 参数({}) 请求过于频繁]", joinPoint.getSignature().toString(), LogSanitizeUtils.s
anitizeArgs(joinPoint.getArgs()));
+            // SEC-010：限流拒绝日志按「参数名 -> 值」脱敏，标量凭据（如 refreshToken）借参数名命中根集掩码，
+            // 端点级敏感字段（如短信 code）经 @RateLimiter#maskKeys 精确掩码，避免明文凭据落日志
+            log.info("[beforePointCut][方法({}) 参数({}) 请求过于频繁]", joinPoint.getSignature().toString(),
+                    LogSanitizeUtils.sanitizeMap(buildArgMap(joinPoint), rateLimiter.maskKeys()));
             String message = StrUtil.blankToDefault(rateLimiter.message(),
                     GlobalErrorCodeConstants.TOO_MANY_REQUESTS.getMsg());
             throw new ServiceException(GlobalErrorCodeConstants.TOO_MANY_REQUESTS.getCode(), message);
         }
     }
 
+    /**
+     * 将 joinPoint 入参构建为「参数名 -> 参数值」的有序映射，供 {@link LogSanitizeUtils#sanitizeMap} 做参数名感知脱敏。
+     *
+     * <p>标量入参（如 refreshToken）无法靠值本身识别敏感性，但借参数名（归一后含 token 根集）即可被自动掩码；
+     * 拿不到参数名（签名非 {@link MethodSignature} 或名称缺失）时退化为位置名 {@code argN}，不阻断脱敏。
+     */
+    private static Map<String, Object> buildArgMap(JoinPoint joinPoint) {
+        Object[] args = joinPoint.getArgs();
+        if (args == null || args.length == 0) {
+            return Collections.emptyMap();
+        }
+        Signature signature = joinPoint.getSignature();
+        String[] parameterNames = signature instanceof MethodSignature
+                ? ((MethodSignature) signature).getParameterNames() : null;
+        Map<String, Object> argMap = new LinkedHashMap<>(args.length);
+        for (int i = 0; i < args.length; i++) {
+            String name = parameterNames != null && i < parameterNames.length && StrUtil.isNotEmpty(parameterNames[i])
+                    ? parameterNames[i] : "arg" + i;
+            argMap.put(name, args[i]);
+        }
+        return argMap;
+    }
+
 }
 
diff --git a/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/
ratelimiter/core/keyresolver/impl/ExpressionRateLimiterKeyResolver.java b/services/zhongshu-core/zszj-framework/zszj-sp
ring-boot-starter-protection/src/main/java/cn/zszj/framework/ratelimiter/core/keyresolver/impl/ExpressionRateLimiterKey
Resolver.java
index f4cac3eb..9fe4b268 100644
--- a/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/ratelim
iter/core/keyresolver/impl/ExpressionRateLimiterKeyResolver.java
+++ b/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/ratelim
iter/core/keyresolver/impl/ExpressionRateLimiterKeyResolver.java
@@ -1,8 +1,12 @@
 package cn.zszj.framework.ratelimiter.core.keyresolver.impl;
 
 import cn.hutool.core.util.ArrayUtil;
+import cn.hutool.crypto.SecureUtil;
+import cn.zszj.framework.common.util.servlet.ServletUtils;
 import cn.zszj.framework.ratelimiter.core.annotation.RateLimiter;
 import cn.zszj.framework.ratelimiter.core.keyresolver.RateLimiterKeyResolver;
+import cn.zszj.framework.web.core.util.WebFrameworkUtils;
+import jakarta.servlet.http.HttpServletRequest;
 import org.aspectj.lang.JoinPoint;
 import org.aspectj.lang.reflect.MethodSignature;
 import org.springframework.core.DefaultParameterNameDiscoverer;
@@ -17,6 +21,11 @@ import java.lang.reflect.Method;
 /**
  * 基于 Spring EL 表达式的 {@link RateLimiterKeyResolver} 实现类
  *
+ * ZS-SEC-010：在表达式求得的「主体」（如手机号、用户名、令牌）之上，追加 methodName + tenantId 作用域，
+ * 并做 MD5 压缩，堵死“跨端点、跨租户共用同一额度”的问题。
+ * 由于 Key 只取主体字段、不含其余入参，改动密码 / 验证码等普通参数不会更换 Key（固定主体限流不可被规避），
+ * 且与客户端 IP 无关（伪造转发 IP 不能绕过）。
+ *
  * @author 芋道源码
  */
 public class ExpressionRateLimiterKeyResolver implements RateLimiterKeyResolver {
@@ -39,9 +48,17 @@ public class ExpressionRateLimiterKeyResolver implements RateLimiterKeyResolver
             }
         }
 
-        // 解析参数
+        // 解析参数，获得限流主体（如手机号、用户名、令牌）
         Expression expression = expressionParser.parseExpression(rateLimiter.keyArg());
-        return expression.getValue(evaluationContext, String.class);
+        String subject = expression.getValue(evaluationContext, String.class);
+
+        // ZS-SEC-010：Key 追加 methodName + tenantId 作用域，堵跨端点 / 跨租户共用额度。
+        // 租户来源与 TenantContextHolder 同源（tenant-id 请求头），直接读头避免让轻量的 protection 反向依赖 biz-tenant（分层倒置）；
+        // request 为 null（非 web 上下文）时 tenantId 以 null 占位，不崩溃。
+        HttpServletRequest request = ServletUtils.getRequest();
+        Long tenantId = request != null ? WebFrameworkUtils.getTenantId(request) : null;
+        String methodName = joinPoint.getSignature().toString();
+        return SecureUtil.md5(methodName + ":" + tenantId + ":" + subject);
     }
 
     private static Method getMethod(JoinPoint point) {
diff --git a/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/
ratelimiter/core/redis/RateLimiterRedisDAO.java b/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protec
tion/src/main/java/cn/zszj/framework/ratelimiter/core/redis/RateLimiterRedisDAO.java
index be7b786d..e513451c 100644
--- a/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/ratelim
iter/core/redis/RateLimiterRedisDAO.java
+++ b/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/ratelim
iter/core/redis/RateLimiterRedisDAO.java
@@ -1,6 +1,7 @@
 package cn.zszj.framework.ratelimiter.core.redis;
 
 import lombok.AllArgsConstructor;
+import lombok.extern.slf4j.Slf4j;
 import org.redisson.api.*;
 
 import java.time.Duration;
@@ -12,6 +13,7 @@ import java.util.concurrent.TimeUnit;
  *
  * @author 芋道源码
  */
+@Slf4j
 @AllArgsConstructor
 public class RateLimiterRedisDAO {
 
@@ -27,10 +29,17 @@ public class RateLimiterRedisDAO {
     private final RedissonClient redissonClient;
 
     public Boolean tryAcquire(String key, int count, int time, TimeUnit timeUnit) {
-        // 1. 获得 RRateLimiter，并设置 rate 速率
-        RRateLimiter rateLimiter = getRRateLimiter(key, count, time, timeUnit);
-        // 2. 尝试获取 1 个
-        return rateLimiter.tryAcquire();
+        try {
+            // 1. 获得 RRateLimiter，并设置 rate 速率
+            RRateLimiter rateLimiter = getRRateLimiter(key, count, time, timeUnit);
+            // 2. 尝试获取 1 个
+            return rateLimiter.tryAcquire();
+        } catch (Exception ex) {
+            // ZS-SEC-010：Redis 故障默认 fail-open（放行 + 告警），避免缓存抖动锁死全站登录。
+            // 未来如需 fail-closed，可在此接入 zszj.ratelimiter.fail-strategy 策略开关（本批不实现，YAGNI）。
+            log.error("[tryAcquire][限流 Key({}) Redis 故障，按 fail-open 放行]", key, ex);
+            return Boolean.TRUE;
+        }
     }
 
     private static String formatKey(String key) {
diff --git a/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/test/java/cn/zszj/framework/
ratelimiter/core/aop/RateLimiterAspectTest.java b/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protec
tion/src/test/java/cn/zszj/framework/ratelimiter/core/aop/RateLimiterAspectTest.java
index e61abef8..154e8290 100644
--- a/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/test/java/cn/zszj/framework/ratelim
iter/core/aop/RateLimiterAspectTest.java
+++ b/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/test/java/cn/zszj/framework/ratelim
iter/core/aop/RateLimiterAspectTest.java
@@ -11,12 +11,14 @@ import cn.zszj.framework.ratelimiter.core.keyresolver.RateLimiterKeyResolver;
 import cn.zszj.framework.ratelimiter.core.redis.RateLimiterRedisDAO;
 import org.aspectj.lang.JoinPoint;
 import org.aspectj.lang.Signature;
+import org.aspectj.lang.reflect.MethodSignature;
 import org.junit.jupiter.api.AfterEach;
 import org.junit.jupiter.api.BeforeEach;
 import org.junit.jupiter.api.Test;
 import org.junit.jupiter.api.extension.ExtendWith;
 import org.mockito.Mock;
 import org.mockito.junit.jupiter.MockitoExtension;
+import org.redisson.api.RedissonClient;
 import org.slf4j.LoggerFactory;
 
 import java.util.LinkedHashMap;
@@ -25,6 +27,7 @@ import java.util.Map;
 import java.util.concurrent.TimeUnit;
 import java.util.stream.Collectors;
 
+import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
 import static org.junit.jupiter.api.Assertions.assertEquals;
 import static org.junit.jupiter.api.Assertions.assertFalse;
 import static org.junit.jupiter.api.Assertions.assertThrows;
@@ -33,6 +36,7 @@ import static org.mockito.ArgumentMatchers.any;
 import static org.mockito.ArgumentMatchers.anyInt;
 import static org.mockito.ArgumentMatchers.anyString;
 import static org.mockito.Mockito.doReturn;
+import static org.mockito.Mockito.lenient;
 import static org.mockito.Mockito.mock;
 import static org.mockito.Mockito.when;
 
@@ -42,6 +46,8 @@ import static org.mockito.Mockito.when;
  * 覆盖 SEC-007：限流被拒绝时，方法参数必须先经 {@code LogSanitizeUtils.sanitizeArgs} 脱敏再写日志，
  * 秘密值（password/token/嵌套 apiKey/数组内 secret）不得出现在应用日志中；
  * 同时保留可定位的方法描述与非敏感字段，并抛出携带正确错误码的 ServiceException。
+ *
+ * 覆盖 SEC-010：Redis 故障时，限流按 fail-open（放行 + 告警）降级，不得因缓存抖动锁死全站登录。
  */
 @ExtendWith(MockitoExtension.class)
 public class RateLimiterAspectTest {
@@ -50,6 +56,7 @@ public class RateLimiterAspectTest {
     private static final String SECRET_TOKEN = "TOKEN-SECRET-DoNotLog";
     private static final String SECRET_APIKEY = "APIKEY-SECRET-DoNotLog";
     private static final String SECRET_LIST = "LIST-SECRET-DoNotLog";
+    private static final String SECRET_CODE = "888888-CODE-SECRET-DoNotLog";
     private static final String SAFE_USERNAME = "zhangsan-user";
     private static final String SAFE_NOTE = "hello-note";
     private static final String METHOD_DESC = "UserService.submitOrder(..)";
@@ -110,6 +117,68 @@ public class RateLimiterAspectTest {
         assertTrue(logText.contains("***"), "敏感字段应被掩码");
     }
 
+    /**
+     * SEC-010：refresh-token 端点入参是标量 String refreshToken。
+     * 旧 {@code sanitizeArgs} 只掩码对象字段、放过标量，导致完整可复用的刷新令牌明文落限流拒绝日志。
+     * 修复后借「参数名感知」（refreshToken 归一后含 token 根集）自动掩码该标量凭据。
+     */
+    @Test
+    public void testBeforePointCut_tooManyRequests_scalarRefreshTokenMasked() {
+        RateLimiter rateLimiter = mock(RateLimiter.class);
+        doReturn(FixedKeyResolver.class).when(rateLimiter).keyResolver();
+        when(rateLimiter.count()).thenReturn(5);
+        when(rateLimiter.time()).thenReturn(60);
+        when(rateLimiter.timeUnit()).thenReturn(TimeUnit.SECONDS);
+        when(rateLimiter.message()).thenReturn("");
+        // joinPoint：MethodSignature 提供参数名 refreshToken，入参为标量令牌（复刻 controller 真实入参形态）
+        JoinPoint joinPoint = mock(JoinPoint.class);
+        MethodSignature signature = mock(MethodSignature.class);
+        lenient().when(signature.getParameterNames()).thenReturn(new String[]{"refreshToken"});
+        when(joinPoint.getSignature()).thenReturn(signature);
+        when(joinPoint.getArgs()).thenReturn(new Object[]{SECRET_TOKEN});
+        when(rateLimiterRedisDAO.tryAcquire(anyString(), anyInt(), anyInt(), any())).thenReturn(false);
+
+        assertThrows(ServiceException.class,
+                () -> rateLimiterAspect.beforePointCut(joinPoint, rateLimiter));
+
+        String logText = capturedLog();
+        assertFalse(logText.contains(SECRET_TOKEN), "标量 refreshToken 凭据不应出现在限流拒绝日志");
+        assertTrue(logText.contains("***"), "标量凭据应经参数名感知被掩码");
+    }
+
+    /**
+     * SEC-010：sms-login / reset-password 端点的 code（短信验证码）不在内置凭据根集，
+     * 旧 {@code sanitizeArgs} 未接收端点级 extraKeys，导致验证码明文落限流拒绝日志。
+     * 修复后经 {@code @RateLimiter(maskKeys = {"code"})} 端点级精确掩码（复刻 controller 真实入参形态）。
+     */
+    @Test
+    public void testBeforePointCut_tooManyRequests_smsCodeMaskedViaMaskKeys() {
+        RateLimiter rateLimiter = mock(RateLimiter.class);
+        doReturn(FixedKeyResolver.class).when(rateLimiter).keyResolver();
+        when(rateLimiter.count()).thenReturn(5);
+        when(rateLimiter.time()).thenReturn(60);
+        when(rateLimiter.timeUnit()).thenReturn(TimeUnit.SECONDS);
+        when(rateLimiter.message()).thenReturn("");
+        lenient().when(rateLimiter.maskKeys()).thenReturn(new String[]{"code"});
+        // joinPoint：MethodSignature 参数名 reqVO，入参为含 code 的短信请求体
+        JoinPoint joinPoint = mock(JoinPoint.class);
+        MethodSignature signature = mock(MethodSignature.class);
+        lenient().when(signature.getParameterNames()).thenReturn(new String[]{"reqVO"});
+        when(joinPoint.getSignature()).thenReturn(signature);
+        Map<String, Object> smsReq = new LinkedHashMap<>();
+        smsReq.put("mobile", "13800138000");
+        smsReq.put("code", SECRET_CODE);
+        when(joinPoint.getArgs()).thenReturn(new Object[]{smsReq});
+        when(rateLimiterRedisDAO.tryAcquire(anyString(), anyInt(), anyInt(), any())).thenReturn(false);
+
+        assertThrows(ServiceException.class,
+                () -> rateLimiterAspect.beforePointCut(joinPoint, rateLimiter));
+
+        String logText = capturedLog();
+        assertFalse(logText.contains(SECRET_CODE), "短信验证码 code 不应出现在限流拒绝日志");
+        assertTrue(logText.contains("***"), "端点级 maskKeys 应将 code 掩码");
+    }
+
     @Test
     public void testBeforePointCut_allowed_noRejectLog() {
         // 未触发限流：正常放行，不产生拒绝日志
@@ -123,6 +192,51 @@ public class RateLimiterAspectTest {
         assertTrue(listAppender.list.isEmpty(), "放行请求不应产生拒绝日志");
     }
 
+    /**
+     * SEC-010：Redis 故障时按 fail-open 降级——放行请求（不抛 TOO_MANY_REQUESTS），并记录告警日志。
+     *
+     * <p>fail-open 策略实现在真实的 {@link RateLimiterRedisDAO#tryAcquire} 内（捕获 Redisson 异常返回 TRUE），
+     * 故此处用「真实 DAO + 抛异常的 RedissonClient」贯穿切面链路验证，而非 mock DAO（mock 会绕过真实 catch）。
+     */
+    @Test
+    public void testBeforePointCut_redisDown_shouldFailOpenByDefault() {
+        // 真实 DAO 包裹一个访问即抛异常的 RedissonClient，模拟 Redis 连接故障
+        RedissonClient redissonClient = mock(RedissonClient.class);
+        when(redissonClient.getRateLimiter(anyString())).thenThrow(new RuntimeException("Redis connection refused"));
+        RateLimiterRedisDAO realDao = new RateLimiterRedisDAO(redissonClient);
+        RateLimiterAspect aspectWithRealDao = new RateLimiterAspect(List.of(new FixedKeyResolver()), realDao);
+
+        // 捕获 DAO 日志，验证 fail-open 告警到位
+        Logger daoLogger = (Logger) LoggerFactory.getLogger(RateLimiterRedisDAO.class);
+        Level originalLevel = daoLogger.getLevel();
+        daoLogger.setLevel(Level.ERROR);
+        ListAppender<ILoggingEvent> daoAppender = new ListAppender<>();
+        daoAppender.start();
+        daoLogger.addAppender(daoAppender);
+        try {
+            RateLimiter rateLimiter = mock(RateLimiter.class);
+            doReturn(FixedKeyResolver.class).when(rateLimiter).keyResolver();
+            when(rateLimiter.count()).thenReturn(2);
+            when(rateLimiter.time()).thenReturn(60);
+            when(rateLimiter.timeUnit()).thenReturn(TimeUnit.SECONDS);
+            JoinPoint joinPoint = mock(JoinPoint.class);
+
+            // fail-open：Redis 故障不得抛 TOO_MANY_REQUESTS，应放行
+            assertDoesNotThrow(() -> aspectWithRealDao.beforePointCut(joinPoint, rateLimiter),
+                    "Redis 故障应 fail-open 放行，而非拒绝或异常逃逸");
+
+            // 告警到位：DAO 记录 error 级日志，显式标明 fail-open
+            String daoLog = daoAppender.list.stream()
+                    .map(ILoggingEvent::getFormattedMessage)
+                    .collect(Collectors.joining("\n"));
+            assertTrue(daoLog.contains("fail-open"), "Redis 故障应记录 fail-open 告警以便运维感知");
+        } finally {
+            daoLogger.detachAppender(daoAppender);
+            daoAppender.stop();
+            daoLogger.setLevel(originalLevel);
+        }
+    }
+
     private static Map<String, Object> buildSensitiveArgs() {
         Map<String, Object> req = new LinkedHashMap<>();
         req.put("username", SAFE_USERNAME);
diff --git a/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/test/java/cn/zszj/framework/
ratelimiter/core/keyresolver/impl/ExpressionRateLimiterKeyResolverTest.java b/services/zhongshu-core/zszj-framework/zsz
j-spring-boot-starter-protection/src/test/java/cn/zszj/framework/ratelimiter/core/keyresolver/impl/ExpressionRateLimite
rKeyResolverTest.java
new file mode 100644
index 00000000..491d41ca
--- /dev/null
+++ b/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/test/java/cn/zszj/framework/ratelim
iter/core/keyresolver/impl/ExpressionRateLimiterKeyResolverTest.java
@@ -0,0 +1,215 @@
+package cn.zszj.framework.ratelimiter.core.keyresolver.impl;
+
+import cn.zszj.framework.common.util.servlet.ServletUtils;
+import cn.zszj.framework.ratelimiter.core.annotation.RateLimiter;
+import cn.zszj.framework.web.core.util.WebFrameworkUtils;
+import org.aspectj.lang.JoinPoint;
+import org.aspectj.lang.reflect.MethodSignature;
+import org.junit.jupiter.api.Test;
+import org.mockito.MockedStatic;
+import org.springframework.mock.web.MockHttpServletRequest;
+
+import java.lang.reflect.Method;
+
+import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
+import static org.junit.jupiter.api.Assertions.assertEquals;
+import static org.junit.jupiter.api.Assertions.assertNotEquals;
+import static org.junit.jupiter.api.Assertions.assertNotNull;
+import static org.mockito.Mockito.mock;
+import static org.mockito.Mockito.mockStatic;
+import static org.mockito.Mockito.when;
+
+/**
+ * {@link ExpressionRateLimiterKeyResolver} 的纯单元测试（ZS-SEC-010）。
+ *
+ * <p>验证增强后的 Key 具备「主体绑定 + 方法作用域 + 租户作用域」三重隔离：
+ * <ul>
+ *     <li>不同租户下的同一主体必须得到不同 Key（跨租户不共用额度）；</li>
+ *     <li>不同端点（方法）下的同一主体必须得到不同 Key（跨端点不共用额度）；</li>
+ *     <li>同一主体、仅改动其它普通参数（如验证码）时 Key 不变（固定主体限流不可被规避）；</li>
+ *     <li>请求上下文缺失（request 为 null）时不崩溃，tenantId 以 null 占位。</li>
+ * </ul>
+ *
+ * <p>租户来源与框架 {@code TenantContextHolder} 同源——从 {@code tenant-id} 请求头读取，
+ * 避免让轻量的 protection 反向依赖 biz-tenant（分层倒置）。
+ */
+public class ExpressionRateLimiterKeyResolverTest {
+
+    private static final String MOBILE = "13800138000";
+
+    private final ExpressionRateLimiterKeyResolver resolver = new ExpressionRateLimiterKeyResolver();
+
+    // ========== 核心用例 ==========
+
+    @Test
+    public void resolver_shouldScopeByMethodAndTenant() {
+        try (MockedStatic<ServletUtils> su = mockStatic(ServletUtils.class)) {
+            // 1) 租户隔离：同一手机号、同一端点，不同租户 -> 不同 Key
+            JoinPoint jp = mockJoinPoint("send", MOBILE, "1234");
+            RateLimiter rl = mockRateLimiter("#mobile");
+
+            su.when(ServletUtils::getRequest).thenReturn(requestWithTenant("1"));
+            String keyTenant1 = resolver.resolver(jp, rl);
+
+            su.when(ServletUtils::getRequest).thenReturn(requestWithTenant("2"));
+            String keyTenant2 = resolver.resolver(jp, rl);
+
+            assertNotEquals(keyTenant1, keyTenant2, "不同租户不得共用限流额度");
+        }
+
+        try (MockedStatic<ServletUtils> su = mockStatic(ServletUtils.class)) {
+            // 2) 方法隔离：同一手机号、同一租户，不同端点 -> 不同 Key
+            su.when(ServletUtils::getRequest).thenReturn(requestWithTenant("1"));
+            String keySend = resolver.resolver(mockJoinPoint("send", MOBILE, "1234"), mockRateLimiter("#mobile"));
+            String keyOther = resolver.resolver(mockJoinPoint("other", MOBILE, "1234"), mockRateLimiter("#mobile"));
+
+            assertNotEquals(keySend, keyOther, "不同端点(方法)不得共用限流额度");
+        }
+    }
+
+    @Test
+    public void resolver_sameSubjectDifferentArg_shouldNotChangeKey() {
+        try (MockedStatic<ServletUtils> su = mockStatic(ServletUtils.class)) {
+            su.when(ServletUtils::getRequest).thenReturn(requestWithTenant("1"));
+
+            // 同一手机号，仅改动验证码（普通参数）-> Key 不变，无法规避固定主体限流
+            String key1 = resolver.resolver(mockJoinPoint("send", MOBILE, "1111"), mockRateLimiter("#mobile"));
+            String key2 = resolver.resolver(mockJoinPoint("send", MOBILE, "2222"), mockRateLimiter("#mobile"));
+
+            assertEquals(key1, key2, "改验证码等普通参数不得更换 Key（固定主体限流不可规避）");
+            // Key 已做方法+租户作用域哈希，不等于主体原值（防跨端点/跨租户共用）
+            assertNotEquals(MOBILE, key1, "Key 应含方法+租户作用域(MD5)，而非主体原值");
+        }
+    }
+
+    @Test
+    public void resolver_whenNoRequestContext_shouldNotCrash() {
+        try (MockedStatic<ServletUtils> su = mockStatic(ServletUtils.class)) {
+            // 非 web 上下文（request 为 null）：tenantId 以 null 占位，仍应稳定产出 Key，不抛异常
+            su.when(ServletUtils::getRequest).thenReturn(null);
+
+            String key = assertDoesNotThrow(() ->
+                    resolver.resolver(mockJoinPoint("send", MOBILE, "1111"), mockRateLimiter("#mobile")));
+
+            assertNotNull(key, "无请求上下文时仍应产出非空 Key");
+        }
+    }
+
+    // ========== 测试脚手架 ==========
+
+    private JoinPoint mockJoinPoint(String methodName, Object... args) {
+        Method method;
+        try {
+            method = Fixture.class.getMethod(methodName, String.class, String.class);
+        } catch (NoSuchMethodException e) {
+            throw new IllegalStateException(e);
+        }
+        MethodSignature signature = new FixedMethodSignature(method);
+        JoinPoint joinPoint = mock(JoinPoint.class);
+        when(joinPoint.getSignature()).thenReturn(signature);
+        when(joinPoint.getArgs()).thenReturn(args);
+        return joinPoint;
+    }
+
+    private RateLimiter mockRateLimiter(String keyArg) {
+        RateLimiter rateLimiter = mock(RateLimiter.class);
+        when(rateLimiter.keyArg()).thenReturn(keyArg);
+        return rateLimiter;
+    }
+
+    private MockHttpServletRequest requestWithTenant(String tenantId) {
+        MockHttpServletRequest request = new MockHttpServletRequest();
+        if (tenantId != null) {
+            request.addHeader(WebFrameworkUtils.HEADER_TENANT_ID, tenantId);
+        }
+        return request;
+    }
+
+    /** 被测方法所在的固定夹具类，提供真实 Method 供参数名发现使用（依赖 -parameters 编译） */
+    static class Fixture {
+        public void send(String mobile, String code) {
+        }
+
+        public void other(String mobile, String code) {
+        }
+    }
+
+    /**
+     * 真实的 {@link MethodSignature} 实现：Mockito 无法 stub {@code toString()}，
+     * 而解析器用 {@code getSignature().toString()} 作为方法作用域因子，故需返回确定性的方法描述。
+     */
+    @SuppressWarnings({"rawtypes"})
+    static class FixedMethodSignature implements MethodSignature {
+        private final Method method;
+
+        FixedMethodSignature(Method method) {
+            this.method = method;
+        }
+
+        @Override
+        public Method getMethod() {
+            return method;
+        }
+
+        @Override
+        public Class getReturnType() {
+            return method.getReturnType();
+        }
+
+        @Override
+        public Class[] getParameterTypes() {
+            return method.getParameterTypes();
+        }
+
+        @Override
+        public Class[] getExceptionTypes() {
+            return method.getExceptionTypes();
+        }
+
+        @Override
+        public String[] getParameterNames() {
+            java.lang.reflect.Parameter[] params = method.getParameters();
+            String[] names = new String[params.length];
+            for (int i = 0; i < params.length; i++) {
+                names[i] = params[i].getName();
+            }
+            return names;
+        }
+
+        @Override
+        public String toString() {
+            return method.toString();
+        }
+
+        @Override
+        public String toShortString() {
+            return method.toString();
+        }
+
+        @Override
+        public String toLongString() {
+            return method.toString();
+        }
+
+        @Override
+        public String getName() {
+            return method.getName();
+        }
+
+        @Override
+        public int getModifiers() {
+            return method.getModifiers();
+        }
+
+        @Override
+        public Class getDeclaringType() {
+            return method.getDeclaringClass();
+        }
+
+        @Override
+        public String getDeclaringTypeName() {
+            return method.getDeclaringClass().getName();
+        }
+    }
+
+}

       * @return HttpServletRequest
       */
>     public static HttpServletRequest getRequest() {
>         RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
          if (!(requestAttributes instanceof ServletRequestAttributes)) {
              return null;
          }
>         return ((ServletRequestAttributes) requestAttributes).getRequest();
      }
  
      public static String getUserAgent() {
>         HttpServletRequest request = getRequest();
          if (request == null) {
              return null;
          }
          return getUserAgent(request);
      }
  
      public static String getClientIP() {
>         HttpServletRequest request = getRequest();
          if (request == null) {
              return null;
          }
          return JakartaServletUtil.getClientIP(request);
      }
  
      public static boolean isJsonRequest(ServletRequest request) {
          return StrUtil.startWithIgnoreCase(request.getContentType(), MediaType.APPLICATION_JSON_VALUE);
      }
  
apps/zhongshu-admin-web/src/components/bpmnProcessDesigner/src/modules/rules/CustomRules.js
apps/zhongshu-admin-web/src/components/bpmnProcessDesigner/src/modules/rules/index.js
apps/zhongshu-miniapp/.cursor/rules/api-http-patterns.mdc
apps/zhongshu-miniapp/.cursor/rules/development-workflow.mdc
apps/zhongshu-miniapp/.cursor/rules/project-overview.mdc
apps/zhongshu-miniapp/.cursor/rules/styling-css-patterns.mdc
apps/zhongshu-miniapp/.cursor/rules/uni-app-patterns.mdc
apps/zhongshu-miniapp/.cursor/rules/vue-typescript-patterns.mdc
scripts/brand/naming-rules.mjs



exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command '$x = [xml](Get-Content -Raw services/zhongsh
u-core/zszj-framework/zszj-spring-boot-starter-protection/target/surefire-reports/TEST-cn.zszj.framework.idempotent.cor
e.aop.IdempotentAspectTest.xml); $cp = ($x.testsuite.properties.property | Where-Object name -eq '"'java.class.path').v
alue; "'$cpSmall = ($cp -split '"';' | Where-Object { "'$_ -match '"'(target\\\\classes|hutool-all|jackson-|slf4j-api|s
pring-core|spring-jcl|spring-test|spring-web-|aspectjweaver|tomcat-embed-core)' }) -join ';'; @'
import cn.zszj.framework.common.util.log.LogSanitizeUtils;
import cn.hutool.crypto.SecureUtil;
import java.util.*;
String prefix = \"x\".repeat(2100);
String a = LogSanitizeUtils.sanitizeArgs(new Object[]{Map.of(\"description\",prefix+\"A\")});
String b = LogSanitizeUtils.sanitizeArgs(new Object[]{Map.of(\"description\",prefix+\"B\")});
System.out.println(\"TRUNCATION: distinct content, same digest = \" + SecureUtil.md5(a).equals(SecureUtil.md5(b)) + \";
 output length=\" + a.length());
System.out.println(\"SCALAR: \" + LogSanitizeUtils.sanitizeArgs(new Object[]{\"123456\"}));
/exit
'@ | & './tools/jdk-17.0.20.1+1/bin/jshell.exe' --execution local --class-path "'$cpSmall --feedback concise -' in E:\众
墅之家AI赋能平台底座
 exited 1 in 1696ms:
9�� 12, 2026 1:33:19 ���� java.util.prefs.WindowsPreferences <init>
WARNING: Could not open/create prefs root node Software\JavaSoft\Prefs at root 0xffffffff80000001. Windows RegCreateKey
Ex(...) returned error code 5.
9�� 12, 2026 1:33:19 ���� java.util.prefs.WindowsPreferences WindowsRegOpenKey1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs at root 0xffffffff80000001.
9�� 12, 2026 1:33:19 ���� java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs at root 0xffffffff80000001. Windows RegOpenKey(..
.) returned error code 2.
9�� 12, 2026 1:33:19 ���� java.util.prefs.WindowsPreferences WindowsRegOpenKey1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs\tool at root 0xffffffff80000001.
9�� 12, 2026 1:33:19 ���� java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs\tool at root 0xffffffff80000001. Windows RegOpenK
ey(...) returned error code 2.
9�� 12, 2026 1:33:19 ���� java.util.prefs.WindowsPreferences WindowsRegOpenKey1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001.
9�� 12, 2026 1:33:19 ���� java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001. Windows
 RegOpenKey(...) returned error code 2.
�Ҳ��� '--class-path' ���ļ� 'E:\众墅之家AI赋能平台底座\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\t
arget\classes'��
�Ҳ��� '--class-path' ���ļ� 'E:\众墅之家AI赋能平台底座\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\target\c
lasses'��
�Ҳ��� '--class-path' ���ļ� 'E:\众墅之家AI赋能平台底座\services\zhongshu-core\zszj-framework\zszj-common\target\classes'��
�Ҳ��� '--class-path' ���ļ� 'E:\众墅之家AI赋能平台底座\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-redis\target
\classes'��
�Ҳ��� '--class-path' ���ļ� 'E:\众墅之家AI赋能平台底座\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-test\target\
classes'��
�Ҳ��� '--class-path' ���ļ� 'E:\众墅之家AI赋能平台底座\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-mybatis\targ
et\classes'��

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command '$x = [xml](Get-Content -Raw services/zhongsh
u-core/zszj-framework/zszj-spring-boot-starter-protection/target/surefire-reports/TEST-cn.zszj.framework.idempotent.cor
e.aop.IdempotentAspectTest.xml); $cp = ($x.testsuite.properties.property | Where-Object name -eq '"'java.class.path').v
alue; "'$cpSmall = @('"'services/zhongshu-core/zszj-framework/zszj-common/target/classes','services/zhongshu-core/zszj-
framework/zszj-spring-boot-starter-protection/target/classes') + ("'$cp -split '"';' | Where-Object { "'$_ -match '"'(h
utool-all|jackson-|slf4j-api|spring-core|spring-jcl|spring-test|spring-web-|aspectjweaver|tomcat-embed-core)' }); "'$cp
Small = $cpSmall -join '"';'; @'
import cn.zszj.framework.common.util.log.LogSanitizeUtils;
import cn.hutool.crypto.SecureUtil;
import java.util.*;
String prefix = \"x\".repeat(2100);
String a = LogSanitizeUtils.sanitizeArgs(new Object[]{Map.of(\"description\",prefix+\"A\")});
String b = LogSanitizeUtils.sanitizeArgs(new Object[]{Map.of(\"description\",prefix+\"B\")});
System.out.println(\"TRUNCATION: distinct content, same digest = \" + SecureUtil.md5(a).equals(SecureUtil.md5(b)) + \";
 output length=\" + a.length());
System.out.println(\"SCALAR: \" + LogSanitizeUtils.sanitizeArgs(new Object[]{\"123456\"}));
/exit
'@ | & './tools/jdk-17.0.20.1+1/bin/jshell.exe' --execution local --class-path "'$cpSmall --feedback concise - 2>$null'
 in E:\众墅之家AI赋能平台底座
 exited 1 in 2349ms:
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command '$x = [xml](Get-Content -Raw services/zhongsh
u-core/zszj-framework/zszj-spring-boot-starter-protection/target/surefire-reports/TEST-cn.zszj.framework.idempotent.cor
e.aop.IdempotentAspectTest.xml); $cp = ($x.testsuite.properties.property | Where-Object name -eq '"'java.class.path').v
alue; "'$cpSmall = @('"'services/zhongshu-core/zszj-framework/zszj-common/target/classes') + ("'$cp -split '"';' | Wher
e-Object { "'$_ -match '"'(hutool-all|jackson-|slf4j-api|spring-core|spring-jcl)' }); "'$cpSmall = $cpSmall -join '"';'
; @'
System.out.println(\"START\");
import cn.zszj.framework.common.util.log.LogSanitizeUtils;
import cn.hutool.crypto.SecureUtil;
import java.util.*;
String prefix = \"x\".repeat(2100);
String a = LogSanitizeUtils.sanitizeArgs(new Object[]{Map.of(\"description\",prefix+\"A\")});
String b = LogSanitizeUtils.sanitizeArgs(new Object[]{Map.of(\"description\",prefix+\"B\")});
System.out.println(\"TRUNCATION: distinct content, same digest = \" + SecureUtil.md5(a).equals(SecureUtil.md5(b)) + \";
 output length=\" + a.length());
/exit
'@ | & './tools/jdk-17.0.20.1+1/bin/jshell.exe' --execution local --class-path "'$cpSmall --feedback concise -' in E:\众
墅之家AI赋能平台底座
 succeeded in 2567ms:
9�� 12, 2026 1:33:58 ���� java.util.prefs.WindowsPreferences <init>
WARNING: Could not open/create prefs root node Software\JavaSoft\Prefs at root 0xffffffff80000001. Windows RegCreateKey
Ex(...) returned error code 5.
9�� 12, 2026 1:33:58 ���� java.util.prefs.WindowsPreferences WindowsRegOpenKey1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs at root 0xffffffff80000001.
9�� 12, 2026 1:33:58 ���� java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs at root 0xffffffff80000001. Windows RegOpenKey(..
.) returned error code 2.
9�� 12, 2026 1:33:58 ���� java.util.prefs.WindowsPreferences WindowsRegOpenKey1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs\tool at root 0xffffffff80000001.
9�� 12, 2026 1:33:58 ���� java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs\tool at root 0xffffffff80000001. Windows RegOpenK
ey(...) returned error code 2.
9�� 12, 2026 1:33:58 ���� java.util.prefs.WindowsPreferences WindowsRegOpenKey1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001.
9�� 12, 2026 1:33:58 ���� java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001. Windows
 RegOpenKey(...) returned error code 2.
9�� 12, 2026 1:33:58 ���� java.util.prefs.WindowsPreferences WindowsRegOpenKey1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001.
9�� 12, 2026 1:33:58 ���� java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001. Windows
 RegOpenKey(...) returned error code 2.
9�� 12, 2026 1:33:59 ���� java.util.prefs.WindowsPreferences WindowsRegOpenKey1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001.
9�� 12, 2026 1:33:59 ���� java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001. Windows
 RegOpenKey(...) returned error code 2.
START
�쳣���� java.lang.NoClassDefFoundError��cn/zszj/framework/common/util/log/LogSanitizeUtils
      at (#6:1)
ԭ��: java.lang.ClassNotFoundException�� cn.zszj.framework.common.util.log.LogSanitizeUtils
      at URLClassLoader.findClass (URLClassLoader.java:445)
      at DefaultLoaderDelegate$RemoteClassLoader.findClass (DefaultLoaderDelegate.java:154)
      at ClassLoader.loadClass (ClassLoader.java:592)
      at ClassLoader.loadClass (ClassLoader.java:525)
      ...
�쳣���� java.lang.NoClassDefFoundError��cn/zszj/framework/common/util/log/LogSanitizeUtils
      at (#7:1)
ԭ��: java.lang.ClassNotFoundException�� cn.zszj.framework.common.util.log.LogSanitizeUtils
      at URLClassLoader.findClass (URLClassLoader.java:445)
      at DefaultLoaderDelegate$RemoteClassLoader.findClass (DefaultLoaderDelegate.java:154)
      at ClassLoader.loadClass (ClassLoader.java:592)
      at ClassLoader.loadClass (ClassLoader.java:525)
      ...
�쳣���� java.lang.NoClassDefFoundError��cn/hutool/crypto/SecureUtil
      at (#8:1)
ԭ��: java.lang.ClassNotFoundException�� cn.hutool.crypto.SecureUtil
      at URLClassLoader.findClass (URLClassLoader.java:445)
      at DefaultLoaderDelegate$RemoteClassLoader.findClass (DefaultLoaderDelegate.java:154)
      at ClassLoader.loadClass (ClassLoader.java:592)
      at ClassLoader.loadClass (ClassLoader.java:525)
      ...

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command '$x = [xml](Get-Content -Raw services/zhongsh
u-core/zszj-framework/zszj-spring-boot-starter-protection/target/surefire-reports/TEST-cn.zszj.framework.idempotent.cor
e.aop.IdempotentAspectTest.xml); $paths = @('"'services/zhongshu-core/zszj-framework/zszj-common/target/classes') + (((
"'$x.testsuite.properties.property | Where-Object name -eq '"'java.class.path').value) -split ';' | Where-Object { "'$_
 -match '"'(hutool-all|jackson-|slf4j-api|spring-core|spring-jcl|spring-test|spring-web-|tomcat-embed-core)' }); "'$pat
hText = ($paths -join '"';').Replace('\\','/'); "'$code = '"'var urls = java.util.Arrays.stream(\"'+"'$pathText+'"'\".s
plit(\";\")).map(p -> {try { return new java.io.File(p).toURI().toURL(); } catch(Exception e) {throw new RuntimeExcepti
on(e);}}).toArray(java.net.URL[]::new);' + \""'`n" + @'"'
var loader = new java.net.URLClassLoader(urls);
System.out.println(\"LOADER: \"+loader.loadClass(\"cn.zszj.framework.common.util.log.LogSanitizeUtils\"));
var sanitize = loader.loadClass(\"cn.zszj.framework.common.util.log.LogSanitizeUtils\").getMethod(\"sanitizeArgs\",Obje
ct[].class,String[].class);
String prefix = \"x\".repeat(2100);
String a = (String) sanitize.invoke(null,new Object[]{java.util.Map.of(\"description\",prefix+\"A\")},new String[0]);
String b = (String) sanitize.invoke(null,new Object[]{java.util.Map.of(\"description\",prefix+\"B\")},new String[0]);
System.out.println(\"TRUNCATION: distinct content, identical digest inputs = \"+a.equals(b)+\"; length=\"+a.length());
var response = loader.loadClass(\"org.springframework.mock.web.MockHttpServletResponse\").getConstructor().newInstance(
);
System.out.println(\"RESPONSE: \"+sanitize.invoke(null,new Object[]{response},new String[0]));
var request = loader.loadClass(\"org.springframework.mock.web.MockHttpServletRequest\").getConstructor().newInstance();
System.out.println(\"REQUEST: \"+sanitize.invoke(null,new Object[]{request},new String[0]));
System.out.println(\"SESSION CREATED: \"+(request.getClass().getMethod(\"getSession\",boolean.class).invoke(request,fal
se)"'!=null));
/exit
'"'@; "'$code | & '"'./tools/jdk-17.0.20.1+1/bin/jshell.exe' --execution local --feedback concise -" in E:\众墅之家AI赋能平台底座
 succeeded in 2724ms:
9�� 12, 2026 1:34:37 ���� java.util.prefs.WindowsPreferences <init>
WARNING: Could not open/create prefs root node Software\JavaSoft\Prefs at root 0xffffffff80000001. Windows RegCreateKey
Ex(...) returned error code 5.
9�� 12, 2026 1:34:37 ���� java.util.prefs.WindowsPreferences WindowsRegOpenKey1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs at root 0xffffffff80000001.
9�� 12, 2026 1:34:37 ���� java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs at root 0xffffffff80000001. Windows RegOpenKey(..
.) returned error code 2.
9�� 12, 2026 1:34:37 ���� java.util.prefs.WindowsPreferences WindowsRegOpenKey1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs\tool at root 0xffffffff80000001.
9�� 12, 2026 1:34:37 ���� java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs\tool at root 0xffffffff80000001. Windows RegOpenK
ey(...) returned error code 2.
9�� 12, 2026 1:34:37 ���� java.util.prefs.WindowsPreferences WindowsRegOpenKey1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001.
9�� 12, 2026 1:34:37 ���� java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001. Windows
 RegOpenKey(...) returned error code 2.
9�� 12, 2026 1:34:37 ���� java.util.prefs.WindowsPreferences WindowsRegOpenKey1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001.
9�� 12, 2026 1:34:37 ���� java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001. Windows
 RegOpenKey(...) returned error code 2.
9�� 12, 2026 1:34:37 ���� java.util.prefs.WindowsPreferences WindowsRegOpenKey1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001.
9�� 12, 2026 1:34:37 ���� java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001. Windows
 RegOpenKey(...) returned error code 2.
LOADER: class cn.zszj.framework.common.util.log.LogSanitizeUtils
SLF4J(W): No SLF4J providers were found.
SLF4J(W): Defaulting to no-operation (NOP) logger implementation
SLF4J(W): See https://www.slf4j.org/codes.html#noProviders for further details.
TRUNCATION: distinct content, identical digest inputs = true; length=2073
RESPONSE: [{"outputStreamAccessAllowed":true,"writerAccessAllowed":true,"characterEncoding":"ISO-8859-1","outputStream"
:{"targetStream":{},"ready":true},"writer":{},"contentLength":0,"bufferSize":4096,"committed":false,"locale":"zh_CN","c
ookies":"***","status":200,"includedUrls":[],"contentLengthLong":0,"contentAsByteArray":"","charset":false,"contentAsSt
ring":"","headerNames":[]}]
REQUEST: [<MockHttpServletRequest>]
SESSION CREATED: false

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command '$x = [xml](Get-Content -Raw services/zhongsh
u-core/zszj-framework/zszj-spring-boot-starter-protection/target/surefire-reports/TEST-cn.zszj.framework.idempotent.cor
e.aop.IdempotentAspectTest.xml); $paths = @('"'services/zhongshu-core/zszj-framework/zszj-common/target/classes') + (((
"'$x.testsuite.properties.property | Where-Object name -eq '"'java.class.path').value) -split ';' | Where-Object { "'$_
 -match '"'(hutool-all|jackson-|slf4j-api|spring-core|spring-jcl|spring-test|spring-web-|tomcat-embed-core)' }); "'$pat
hText = ($paths -join '"';').Replace('\\','/'); "'$code = '"'var urls = java.util.Arrays.stream(\"'+"'$pathText+'"'\".s
plit(\";\")).map(p -> {try { return new java.io.File(p).toURI().toURL(); } catch(Exception e) {throw new RuntimeExcepti
on(e);}}).toArray(java.net.URL[]::new);' + \""'`n" + @'"'
var loader = new java.net.URLClassLoader(urls);
var sanitize = loader.loadClass(\"cn.zszj.framework.common.util.log.LogSanitizeUtils\").getMethod(\"sanitizeArgs\",Obje
ct[].class,String[].class);
var responseClass = loader.loadClass(\"org.apache.catalina.connector.Response\");
System.out.println(java.util.Arrays.toString(responseClass.getConstructors()));
var response = responseClass.getConstructor().newInstance();
var coyoteClass = loader.loadClass(\"org.apache.coyote.Response\");
responseClass.getMethod(\"setCoyoteResponse\",coyoteClass).invoke(response,coyoteClass.getConstructor().newInstance());
var facade = responseClass.getMethod(\"getResponse\").invoke(response);
System.out.println(\"BEFORE: writerUsed=\"+responseClass.getMethod(\"isUsingWriter\").invoke(response));
System.out.println(\"SANITIZED: \"+sanitize.invoke(null,new Object[]{facade},new String[0]));
System.out.println(\"AFTER: writerUsed=\"+responseClass.getMethod(\"isUsingWriter\").invoke(response));
try {facade.getClass().getMethod(\"getWriter\").invoke(facade); System.out.println(\"GETWRITER OK\");} catch(Exception 
e) {System.out.println(\"GETWRITER FAILED: \"+e.getCause());}
try {facade.getClass().getMethod(\"getOutputStream\").invoke(facade); System.out.println(\"GETOUTPUTSTREAM OK\");} catc
h(Exception e) {System.out.println(\"GETOUTPUTSTREAM FAILED: \"+e.getCause());}
/exit
'@; "'$code | & '"'./tools/jdk-17.0.20.1+1/bin/jshell.exe' --execution local --feedback concise -" in E:\众墅之家AI赋能平台底座
 succeeded in 2727ms:
9�� 12, 2026 1:35:09 ���� java.util.prefs.WindowsPreferences <init>
WARNING: Could not open/create prefs root node Software\JavaSoft\Prefs at root 0xffffffff80000001. Windows RegCreateKey
Ex(...) returned error code 5.
9�� 12, 2026 1:35:09 ���� java.util.prefs.WindowsPreferences WindowsRegOpenKey1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs at root 0xffffffff80000001.
9�� 12, 2026 1:35:09 ���� java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs at root 0xffffffff80000001. Windows RegOpenKey(..
.) returned error code 2.
9�� 12, 2026 1:35:09 ���� java.util.prefs.WindowsPreferences WindowsRegOpenKey1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs\tool at root 0xffffffff80000001.
9�� 12, 2026 1:35:09 ���� java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs\tool at root 0xffffffff80000001. Windows RegOpenK
ey(...) returned error code 2.
9�� 12, 2026 1:35:09 ���� java.util.prefs.WindowsPreferences WindowsRegOpenKey1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001.
9�� 12, 2026 1:35:09 ���� java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001. Windows
 RegOpenKey(...) returned error code 2.
9�� 12, 2026 1:35:09 ���� java.util.prefs.WindowsPreferences WindowsRegOpenKey1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001.
9�� 12, 2026 1:35:09 ���� java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001. Windows
 RegOpenKey(...) returned error code 2.
9�� 12, 2026 1:35:10 ���� java.util.prefs.WindowsPreferences WindowsRegOpenKey1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001.
9�� 12, 2026 1:35:10 ���� java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001. Windows
 RegOpenKey(...) returned error code 2.
[public org.apache.catalina.connector.Response(), public org.apache.catalina.connector.Response(int)]
�쳣���� java.lang.NoSuchMethodException��org.apache.catalina.connector.Response.isUsingWriter()
      at Class.getMethod (Class.java:2229)
      at (#10:1)
SLF4J(W): No SLF4J providers were found.
SLF4J(W): Defaulting to no-operation (NOP) logger implementation
SLF4J(W): See https://www.slf4j.org/codes.html#noProviders for further details.
SANITIZED: [<ResponseFacade>]
�쳣���� java.lang.NoSuchMethodException��org.apache.catalina.connector.Response.isUsingWriter()
      at Class.getMethod (Class.java:2229)
      at (#12:1)
GETWRITER FAILED: java.lang.NullPointerException: Cannot invoke "org.apache.catalina.connector.Request.getConnector()" 
because "this.request" is null
GETOUTPUTSTREAM OK

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command '$x = [xml](Get-Content -Raw services/zhongsh
u-core/zszj-framework/zszj-spring-boot-starter-protection/target/surefire-reports/TEST-cn.zszj.framework.idempotent.cor
e.aop.IdempotentAspectTest.xml); $paths = @('"'services/zhongshu-core/zszj-framework/zszj-common/target/classes') + (((
"'$x.testsuite.properties.property | Where-Object name -eq '"'java.class.path').value) -split ';' | Where-Object { "'$_
 -match '"'(hutool-all|jackson-|slf4j-api|spring-core|spring-jcl|tomcat-embed-core)' }); "'$pathText = ($paths -join '"
';').Replace('\\','/'); "'$code = '"'var urls = java.util.Arrays.stream(\"'+"'$pathText+'"'\".split(\";\")).map(p -> {t
ry { return new java.io.File(p).toURI().toURL(); } catch(Exception e) {throw new RuntimeException(e);}}).toArray(java.n
et.URL[]::new);' + \""'`n" + @'"'
var loader = new java.net.URLClassLoader(urls);
var sanitize = loader.loadClass(\"cn.zszj.framework.common.util.log.LogSanitizeUtils\").getMethod(\"sanitizeArgs\",Obje
ct[].class,String[].class);
var responseClass = loader.loadClass(\"org.apache.catalina.connector.Response\");
var response = responseClass.getConstructor().newInstance();
var coyoteClass = loader.loadClass(\"org.apache.coyote.Response\");
responseClass.getMethod(\"setCoyoteResponse\",coyoteClass).invoke(response,coyoteClass.getConstructor().newInstance());
var connectorClass = loader.loadClass(\"org.apache.catalina.connector.Connector\");
var connector = connectorClass.getConstructor().newInstance();
var requestClass = loader.loadClass(\"org.apache.catalina.connector.Request\");
System.out.println(java.util.Arrays.toString(requestClass.getConstructors()));
var request = requestClass.getConstructor(connectorClass).newInstance(connector);
responseClass.getMethod(\"setRequest\",requestClass).invoke(response,request);
var facade = responseClass.getMethod(\"getResponse\").invoke(response);
System.out.println(\"SANITIZED: \"+sanitize.invoke(null,new Object[]{facade},new String[0]));
try {facade.getClass().getMethod(\"getWriter\").invoke(facade); System.out.println(\"GETWRITER OK\");} catch(Exception 
e) {System.out.println(\"GETWRITER FAILED: \"+e.getCause());}
try {facade.getClass().getMethod(\"getOutputStream\").invoke(facade); System.out.println(\"GETOUTPUTSTREAM OK\");} catc
h(Exception e) {System.out.println(\"GETOUTPUTSTREAM FAILED: \"+e.getCause());}
/exit
'@; "'$code | & '"'./tools/jdk-17.0.20.1+1/bin/jshell.exe' --execution local --feedback concise -" in E:\众墅之家AI赋能平台底座
 succeeded in 2632ms:
9�� 12, 2026 1:35:40 ���� java.util.prefs.WindowsPreferences <init>
WARNING: Could not open/create prefs root node Software\JavaSoft\Prefs at root 0xffffffff80000001. Windows RegCreateKey
Ex(...) returned error code 5.
9�� 12, 2026 1:35:40 ���� java.util.prefs.WindowsPreferences WindowsRegOpenKey1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs at root 0xffffffff80000001.
9�� 12, 2026 1:35:40 ���� java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs at root 0xffffffff80000001. Windows RegOpenKey(..
.) returned error code 2.
9�� 12, 2026 1:35:40 ���� java.util.prefs.WindowsPreferences WindowsRegOpenKey1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs\tool at root 0xffffffff80000001.
9�� 12, 2026 1:35:40 ���� java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs\tool at root 0xffffffff80000001. Windows RegOpenK
ey(...) returned error code 2.
9�� 12, 2026 1:35:40 ���� java.util.prefs.WindowsPreferences WindowsRegOpenKey1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001.
9�� 12, 2026 1:35:40 ���� java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001. Windows
 RegOpenKey(...) returned error code 2.
9�� 12, 2026 1:35:40 ���� java.util.prefs.WindowsPreferences WindowsRegOpenKey1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001.
9�� 12, 2026 1:35:40 ���� java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001. Windows
 RegOpenKey(...) returned error code 2.
9�� 12, 2026 1:35:41 ���� java.util.prefs.WindowsPreferences WindowsRegOpenKey1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001.
9�� 12, 2026 1:35:41 ���� java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001. Windows
 RegOpenKey(...) returned error code 2.
[public org.apache.catalina.connector.Request(org.apache.catalina.connector.Connector)]
SLF4J(W): No SLF4J providers were found.
SLF4J(W): Defaulting to no-operation (NOP) logger implementation
SLF4J(W): See https://www.slf4j.org/codes.html#noProviders for further details.
SANITIZED: [<ResponseFacade>]
GETWRITER OK
GETOUTPUTSTREAM FAILED: java.lang.IllegalStateException: getWriter() has already been called for this response

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "git show 3b5d3b8f:services/zhongshu-core/zsz
j-framework/zszj-common/src/main/java/cn/zszj/framework/common/util/servlet/ServletUtils.java | Select-String -Pattern 
'writeAttachment|writeJSON|getOutputStream|getWriter' -Context 4,18; git show 3b5d3b8f:services/zhongshu-core/zszj-fram
ework/zszj-common/src/main/java/cn/zszj/framework/common/util/log/LogSanitizeUtils.java | Select-String -Pattern 'MAX_L
ENGTH|sanitizeArgs|sanitizeArgValue|truncate' -Context 0,16; git show 3b5d3b8f"'^:services/zhongshu-core/zszj-framework
/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/idempotent/core/aop/IdempotentAspect.java; $i=0; g
it show 3b5d3b8f:services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framew
ork/idempotent/core/aop/IdempotentAspect.java | ForEach-Object {$i++; if ($i -ge 65 -and $i -le 106) { '"'{0,4} {1}' -f
 "'$i,$_ }}' in E:\众墅之家AI赋能平台底座
 succeeded in 119ms:

       * 返回 JSON 字符�?     *
       * @param response 响应
       * @param object   对象，会序列化成 JSON 字符�?     */
      @SuppressWarnings("deprecation") // 必须使用 APPLICATION_JSON_UTF8_VALUE，否则会乱码
>     public static void writeJSON(HttpServletResponse response, Object object) {
          String content = JsonUtils.toJsonString(object);
          JakartaServletUtil.write(response, content, MediaType.APPLICATION_JSON_UTF8_VALUE);
      }
  
      /**
       * @param request 请求
       * @return ua
       */
      public static String getUserAgent(HttpServletRequest request) {
          String ua = request.getHeader("User-Agent");
          return ua != null ? ua : "";
      }
  
      /**
       * 获得请求
       *
       * @return HttpServletRequest
       */
      public static Map<String, String> getHeaderMap(HttpServletRequest request) {
          return JakartaServletUtil.getHeaderMap(request);
      }
  
>     public static void writeAttachment(HttpServletResponse response, String filename, byte[] content)
              throws IOException {
>         response.getOutputStream().write(content);
          response.addHeader("Content-Disposition", "attachment;filename=" + HttpUtils.encodeUtf8(filename));
          response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
      }
  
  }
>  * 3. 递归脱敏：对象与数组逐层递归，命中敏感键的值统一掩码�?{@code ***}，保留字段名以�
��审计定位�? * 4. 失败不回退原文：解�?/ 序列化异常时只记录摘要（长度或类�?+ 原因类别）�
��绝不输出未净化的原始内容�? * 5. 体积上限：结果超�?{@link #MAX_LENGTH} 时截断并附总长度，避�
��超长正文撑爆日志表�? *
   * 该工具不依赖 servlet / web，可�?web �?protection 两个 starter 共同复用�? *
   * @author 众墅之家
   */
  @Slf4j
  public class LogSanitizeUtils {
  
      /**
       * 敏感值掩码，保留字段名、隐藏具体值�?     * 公开以便调用方（如签名切面）对单个凭
据派生值就地掩码，复用统一掩码标记�?     */
      public static final String MASK = "***";
  
      /**
       * 单条日志内容的长度上限，超出部分截断
       */
>     private static final int MAX_LENGTH = 2048;
  
      /**
       * 内置凭据敏感键根集（已归一：小写、无下划�?/ 连字符），使�?contains 匹配
       */
      private static final Set<String> SENSITIVE_KEY_ROOTS = Set.of(
              "password", "passwd", "pwd", "token", "secret", "authorization",
              "credential", "privatekey", "apikey", "accesskey", "secretkey",
              "cookie", "session", "otp");
  
      private LogSanitizeUtils() {
      }
  
      /**
       * 脱敏 JSON 字符串（如请求正文）�?     *
       * @param json      原始 JSON 字符�?     * @param extraKeys 端点级附加敏感键
       * @return 脱敏后的 JSON 字符串；入参为空返回 {@code null}；无法解析时返回摘要（不含原文�
��
>             return truncate(mapper().writeValueAsString(node));
          } catch (Throwable t) {
              return unparseable(json, t);
          }
      }
  
      /**
       * 脱敏键值映射（�?query 参数）�?     *
       * @param map       原始映射
       * @param extraKeys 端点级附加敏感键
       * @return 脱敏后的 JSON 字符串；入参为空返回 {@code null}
       */
      public static String sanitizeMap(Map<String, ?> map, String... extraKeys) {
          if (CollUtil.isEmpty(map)) {
              return null;
          }
          return sanitizeObject(map, extraKeys);
>     public static String sanitizeArgs(Object[] args, String... extraKeys) {
          if (ArrayUtil.isEmpty(args)) {
              return "[]";
          }
          Set<String> extra = normalizeKeys(extraKeys);
          List<String> parts = new ArrayList<>(args.length);
          for (Object arg : args) {
>             parts.add(sanitizeArgValue(arg, extra));
          }
>         return truncate("[" + String.join(", ", parts) + "]");
      }
  
      /**
       * 脱敏响应体（�?CommonResult），仅净�?data 字段，保�?code / msg 以便审计定位�?     *
       * @param result    响应对象
       * @param extraKeys 端点级附加敏感键
       * @return 脱敏后的 JSON 字符串；入参�?{@code null} 返回 {@code null}
       */
      public static String sanitizeResponseBody(Object result, String... extraKeys) {
          if (result == null) {
              return null;
          }
          Set<String> extra = normalizeKeys(extraKeys);
          try {
              JsonNode node = mapper().valueToTree(result);
              JsonNode data = node.get("data");
>             return truncate(mapper().writeValueAsString(node));
          } catch (Throwable t) {
              return unserializable(result, t);
          }
      }
  
      private static String sanitizeObject(Object obj, String... extraKeys) {
          Set<String> extra = normalizeKeys(extraKeys);
          try {
              JsonNode node = mapper().valueToTree(obj);
              sanitizeNode(node, extra);
>             return truncate(mapper().writeValueAsString(node));
          } catch (Throwable t) {
              return unserializable(obj, t);
          }
      }
  
>     private static String sanitizeArgValue(Object arg, Set<String> extra) {
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
       * 递归脱敏 JSON 节点：数组逐元素递归，对象命中敏感键则掩码、否则递归其�?     */
      private static void sanitizeNode(JsonNode node, Set<String> extra) {
          if (node == null) {
>     private static String truncate(String text) {
>         if (text == null || text.length() <= MAX_LENGTH) {
              return text;
          }
>         return text.substring(0, MAX_LENGTH) + "...[truncated,total=" + text.length() + "]";
      }
  
      private static String unparseable(String raw, Throwable t) {
          int len = raw == null ? 0 : raw.length();
          String cause = t.getClass().getSimpleName();
          log.warn("[sanitizeJson][无法解析正文，已降级为摘�?len={} cause={}]", len, cause);
          return "<unparseable:len=" + len + ",cause=" + cause + ">";
      }
  
      private static String unserializable(Object obj, Throwable t) {
          String type = obj == null ? "null" : obj.getClass().getSimpleName();
          String cause = t.getClass().getSimpleName();
          log.warn("[sanitize][无法序列化对象，已降级为摘要 type={} cause={}]", type, cause);
          return "<unserializable:" + type + ">";
      }
  
package cn.zszj.framework.idempotent.core.aop;

import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.zszj.framework.common.util.collection.CollectionUtils;
import cn.zszj.framework.common.util.log.LogSanitizeUtils;
import cn.zszj.framework.idempotent.core.annotation.Idempotent;
import cn.zszj.framework.idempotent.core.keyresolver.IdempotentKeyResolver;
import cn.zszj.framework.idempotent.core.redis.IdempotentRedisDAO;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;

/**
 * 拦截声明了 {@link Idempotent} 注解的方法，实现幂等操作
 *
 * @author 芋道源码
 */
@Aspect
@Slf4j
public class IdempotentAspect {

    /**
     * IdempotentKeyResolver 集合
     */
    private final Map<Class<? extends IdempotentKeyResolver>, IdempotentKeyResolver> keyResolvers;

    private final IdempotentRedisDAO idempotentRedisDAO;

    public IdempotentAspect(List<IdempotentKeyResolver> keyResolvers, IdempotentRedisDAO idempotentRedisDAO) {
        this.keyResolvers = CollectionUtils.convertMap(keyResolvers, IdempotentKeyResolver::getClass);
        this.idempotentRedisDAO = idempotentRedisDAO;
    }

    @Around(value = "@annotation(idempotent)")
    public Object aroundPointCut(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        // 获得 IdempotentKeyResolver
        IdempotentKeyResolver keyResolver = keyResolvers.get(idempotent.keyResolver());
        Assert.notNull(keyResolver, "找不到对应的 IdempotentKeyResolver");
        // 解析 Key
        String key = keyResolver.resolver(joinPoint, idempotent);

        // 1. 锁定 Key
        boolean success = idempotentRedisDAO.setIfAbsent(key, idempotent.timeout(), idempotent.timeUnit());
        // 锁定失败，抛出异常
        if (!success) {
            log.info("[aroundPointCut][方法({}) 参数({}) 存在重复请求]", joinPoint.getSignature().toString(), LogSanitizeUtils.sa
nitizeArgs(joinPoint.getArgs()));
            throw new ServiceException(GlobalErrorCodeConstants.REPEATED_REQUESTS.getCode(), idempotent.message());
        }

        // 2. 执行逻辑
        try {
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            // 3. 异常时，删除 Key
            // 参考美团 GTIS 思路：https://tech.meituan.com/2016/09/29/distributed-system-mutually-exclusive-idempotence-cerbe
rus-gtis.html
            if (idempotent.deleteKeyWhenException()) {
                idempotentRedisDAO.delete(key);
            }
            throw throwable;
        }
    }

}
  65         // 1. 锁定 Key（携带参数摘要）
  66         // MIN-2：DAO 返回 Boolean，pipeline/transaction 场景可能返回 null，用 Boolean.TRUE.equals 防拆�?NPE（null 视为未拿到锁 �?拒绝，fa
il-closed�?        boolean success = Boolean.TRUE.equals(idempotentRedisDAO.setIfAbsent(key, argsDigest, idempotent.tim
eout(), idempotent.timeUnit()));
  67         // 锁定失败，区分冲突与重复
  68         if (!success) {
  69             // IMP-2：getDigest 是「已决策拒绝」后的额�?GET，防 Redis 抖动把干净�?900 升级�?500；异常降�?null（digest 只影响文案，零语义损失）
  70             String storedDigest;
  71             try {
  72                 storedDigest = idempotentRedisDAO.getDigest(key);
  73             } catch (Exception ex) {
  74                 log.warn("[aroundPointCut][幂等�?{}) 读回摘要失败，降级按重复处理]", key, ex);
  75                 storedDigest = null;
  76             }
  77             // IMP-3：StrUtil.isNotEmpty 兼容滚动升级期旧格式空串值（旧实现存 ""），避免误判冲突
  78             // IMP-1：默认路径（Default/User 解析器）Key 已烘�?argsStr，同键必然同参，conflict �?false（构造上不可达）；冲突分支仅�?Expression 解析器等 
Key 不含入参的解析器生效
  79             boolean conflict = StrUtil.isNotEmpty(storedDigest) && !storedDigest.equals(argsDigest);
  80             // MIN-4：冲突通常是客户端 bug/探测（尤�?Expression 解析器下可能跨租户撞键），用 warn 提升安全信号；重复是常态（如双击），用 info
  81             if (conflict) {
  82                 log.warn("[aroundPointCut][方法({}) 参数({}) 同键异参冲突]",
  83                         joinPoint.getSignature(), LogSanitizeUtils.sanitizeArgs(joinPoint.getArgs()));
  84             } else {
  85                 log.info("[aroundPointCut][方法({}) 参数({}) 同键同参重复]",
  86                         joinPoint.getSignature(), LogSanitizeUtils.sanitizeArgs(joinPoint.getArgs()));
  87             }
  88             String msg = conflict ? CONFLICT_MESSAGE : idempotent.message();
  89             throw new ServiceException(GlobalErrorCodeConstants.REPEATED_REQUESTS.getCode(), msg);
  90         }
  91 
  92         // 2. 执行逻辑
  93         try {
  94             return joinPoint.proceed();
  95         } catch (Throwable throwable) {
  96             // 3. 异常时，删除 Key
  97             // 参考美�?GTIS 思路：https://tech.meituan.com/2016/09/29/distributed-system-mutually-exclusive-idempotence-
cerberus-gtis.html
  98             if (idempotent.deleteKeyWhenException()) {
  99                 idempotentRedisDAO.delete(key);
 100             }
 101             throw throwable;
 102         }
 103     }
 104 
 105 }



exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command '[Console]::OutputEncoding = [System.Text.UTF
8Encoding]::new($false); $OutputEncoding = [Console]::OutputEncoding; $p = '"'services/zhongshu-core/zszj-framework/zsz
j-spring-boot-starter-protection/src/main/java/cn/zszj/framework/idempotent/core/aop/IdempotentAspect.java'; "'$i=0; gi
t show "3b5d3b8f:$p" | ForEach-Object { $i++; if ($i -ge 70 -and $i -le 103) { '"'{0,4} {1}' -f "'$i,$_ } }; git diff -
-exit-code 3b5d3b8f HEAD -- $p services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framework/common
/util/log/LogSanitizeUtils.java; git status --short' in E:\众墅之家AI赋能平台底座
 succeeded in 204ms:
�޷��������͡�������ģʽ��֧�ֺ������͡�
����λ�� ��:2 �ַ�: 1
+ [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false);  ...
+ ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    + CategoryInfo          : InvalidOperation: (:) []��RuntimeException
    + FullyQualifiedErrorId : CannotCreateTypeConstrainedLanguage
 
  70             String storedDigest;
  71             try {
  72                 storedDigest = idempotentRedisDAO.getDigest(key);
  73             } catch (Exception ex) {
  74                 log.warn("[aroundPointCut][幂等�?{}) 读回摘要失败，降级按重复处理]", key, ex);
  75                 storedDigest = null;
  76             }
  77             // IMP-3：StrUtil.isNotEmpty 兼容滚动升级期旧格式空串值（旧实现存 ""），避免误判冲突
  78             // IMP-1：默认路径（Default/User 解析器）Key 已烘�?argsStr，同键必然同参，conflict �?false（构造上不可达）；冲突分支仅�?Expression 解析器等 
Key 不含入参的解析器生效
  79             boolean conflict = StrUtil.isNotEmpty(storedDigest) && !storedDigest.equals(argsDigest);
  80             // MIN-4：冲突通常是客户端 bug/探测（尤�?Expression 解析器下可能跨租户撞键），用 warn 提升安全信号；重复是常态（如双击），用 info
  81             if (conflict) {
  82                 log.warn("[aroundPointCut][方法({}) 参数({}) 同键异参冲突]",
  83                         joinPoint.getSignature(), LogSanitizeUtils.sanitizeArgs(joinPoint.getArgs()));
  84             } else {
  85                 log.info("[aroundPointCut][方法({}) 参数({}) 同键同参重复]",
  86                         joinPoint.getSignature(), LogSanitizeUtils.sanitizeArgs(joinPoint.getArgs()));
  87             }
  88             String msg = conflict ? CONFLICT_MESSAGE : idempotent.message();
  89             throw new ServiceException(GlobalErrorCodeConstants.REPEATED_REQUESTS.getCode(), msg);
  90         }
  91 
  92         // 2. 执行逻辑
  93         try {
  94             return joinPoint.proceed();
  95         } catch (Throwable throwable) {
  96             // 3. 异常时，删除 Key
  97             // 参考美�?GTIS 思路：https://tech.meituan.com/2016/09/29/distributed-system-mutually-exclusive-idempotence-
cerberus-gtis.html
  98             if (idempotent.deleteKeyWhenException()) {
  99                 idempotentRedisDAO.delete(key);
 100             }
 101             throw throwable;
 102         }
 103     }
warning: unable to access 'C:\Users\Administrator/.config/git/ignore': Permission denied
warning: unable to access 'C:\Users\Administrator/.config/git/ignore': Permission denied
?? ".omx/plans/2026-09-11-B03-Wave1-\345\205\255\351\241\271\345\271\266\350\241\214\345\274\200\345\217\221\350\256\24
1\345\210\222.md"
?? _zs_sec011a_codex.ps1
?? _zs_sec011a_prot.txt
?? _zs_sec011a_verify.ps1
?? docs/reviews/codex-ZS-SEC-011.A.raw.md
?? outputs/

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "git grep -n -F -e 'String argsDigest =' -e '
log.warn(' -e 'storedDigest = idempotentRedisDAO' 3b5d3b8f -- services/zhongshu-core/zszj-framework/zszj-spring-boot-st
arter-protection/src/main/java/cn/zszj/framework/idempotent/core/aop/IdempotentAspect.java; git grep -n -F -e 'MAX_LENG
TH = 2048' -e 'return truncate(\"[\"' -e 'valueToTree(arg)' -e 'response.getOutputStream().write(content)' 3b5d3b8f -- 
services/zhongshu-core/zszj-framework/zszj-common/src/main/java; git show --format= --numstat 3b5d3b8f" in E:\众墅之家AI赋能平
台底座
 succeeded in 33ms:
55	5	services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/idempote
nt/core/aop/IdempotentAspect.java
23	3	services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/idempote
nt/core/keyresolver/impl/DefaultIdempotentKeyResolver.java
25	4	services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/idempote
nt/core/redis/IdempotentRedisDAO.java
189	4	services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/test/java/cn/zszj/framework/idempot
ent/core/aop/IdempotentAspectTest.java
251	0	services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/test/java/cn/zszj/framework/idempot
ent/core/keyresolver/impl/DefaultIdempotentKeyResolverTest.java

codex
The digest calculation can break servlet response handling and miss conflicts for long payloads. The new Redis-error wa
rning also exposes potentially sensitive expression keys.

Full review comments:

- [P1] Exclude servlet objects from eager digest serialization — E:/众墅之家AI赋能平台底座/services/zhongshu-core/zszj-framework/
zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/idempotent/core/aop/IdempotentAspect.java:76-76
  For an annotated controller accepting `HttpServletResponse`, this now serializes the live response before business ex
ecution, including on the first request. Jackson invokes its getters: with Tomcat's `ResponseFacade`, this calls `getWr
iter()` before falling back to a placeholder. Subsequent binary output, such as `ServletUtils.writeAttachment()`, then 
throws `IllegalStateException` because the writer was already selected. Exclude servlet request/response objects and ot
her infrastructure arguments before computing the digest.

- [P2] Compute the digest before applying log-length truncation — E:/众墅之家AI赋能平台底座/services/zhongshu-core/zszj-framework
/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/idempotent/core/aop/IdempotentAspect.java:76-76
  With an expression-based key, equal-length payloads differing only after character 2,048 are incorrectly classified a
s identical requests. `LogSanitizeUtils.sanitizeArgs()` truncates the serialized content and appends only its original 
length, so these distinct payloads produce identical MD5 inputs. This also affects non-sensitive fields and defeats the
 new conflict detection. Hash an untruncated sanitized representation and add a regression test covering differing suff
ixes beyond the logging limit.

- [P2] Avoid logging raw expression-resolver keys — E:/众墅之家AI赋能平台底座/services/zhongshu-core/zszj-framework/zszj-spring-b
oot-starter-protection/src/main/java/cn/zszj/framework/idempotent/core/aop/IdempotentAspect.java:88-88
  When an expression uses a sensitive field such as `#request.token` as its key and the follow-up Redis GET fails, this
 warning writes that token directly to application logs. `ExpressionIdempotentKeyResolver` returns the expression value
 unchanged, so keys are not necessarily hashes, and argument sanitization elsewhere does not protect this path. Omit th
e key or replace it with a safe opaque identifier.
The digest calculation can break servlet response handling and miss conflicts for long payloads. The new Redis-error warning also exposes potentially sensitive expression keys.

Full review comments:

- [P1] Exclude servlet objects from eager digest serialization — E:/众墅之家AI赋能平台底座/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/idempotent/core/aop/IdempotentAspect.java:76-76
  For an annotated controller accepting `HttpServletResponse`, this now serializes the live response before business execution, including on the first request. Jackson invokes its getters: with Tomcat's `ResponseFacade`, this calls `getWriter()` before falling back to a placeholder. Subsequent binary output, such as `ServletUtils.writeAttachment()`, then throws `IllegalStateException` because the writer was already selected. Exclude servlet request/response objects and other infrastructure arguments before computing the digest.

- [P2] Compute the digest before applying log-length truncation — E:/众墅之家AI赋能平台底座/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/idempotent/core/aop/IdempotentAspect.java:76-76
  With an expression-based key, equal-length payloads differing only after character 2,048 are incorrectly classified as identical requests. `LogSanitizeUtils.sanitizeArgs()` truncates the serialized content and appends only its original length, so these distinct payloads produce identical MD5 inputs. This also affects non-sensitive fields and defeats the new conflict detection. Hash an untruncated sanitized representation and add a regression test covering differing suffixes beyond the logging limit.

- [P2] Avoid logging raw expression-resolver keys — E:/众墅之家AI赋能平台底座/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/idempotent/core/aop/IdempotentAspect.java:88-88
  When an expression uses a sensitive field such as `#request.token` as its key and the follow-up Redis GET fails, this warning writes that token directly to application logs. `ExpressionIdempotentKeyResolver` returns the expression value unchanged, so keys are not necessarily hashes, and argument sanitization elsewhere does not protect this path. Omit the key or replace it with a safe opaque identifier.
