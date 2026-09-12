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
session id: 01a0942e-68de-7d43-9d9b-96f20d4f18de
--------
user
commit b62af7d
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'Get-Location; git status --short; git rev-pa
rse --show-toplevel; git show --stat --oneline --decorate --no-renames b62af7d3; Get-ChildItem -Force -Name' in E:\众墅之家
AI赋能平台底座
 succeeded in 79ms:
?? ".omx/plans/2026-09-11-B03-Wave1-\345\205\255\351\241\271\345\271\266\350\241\214\345\274\200\345\217\221\350\256\24
1\345\210\222.md"
?? _zs_sec011a_codex.ps1
?? _zs_sec011a_codex_r1.ps1
?? _zs_sec011a_fixmsg.txt
?? _zs_sec011a_prot.txt
?? _zs_sec011a_tdd.ps1
?? _zs_sec011a_tdd.txt
?? _zs_sec011a_verify.ps1
?? docs/reviews/codex-ZS-SEC-011.A.raw.md
?? outputs/
E:/众墅之家AI赋能平台底座
b62af7d3 (HEAD -> main) fix(sec-011-a): 幂等摘要排除 servlet 入参，堵首次放行即序列化响应破坏二进制输出 [ZS-SEC-011.A]
 .../idempotent/core/aop/IdempotentAspect.java      | 43 ++++++++++++++++++++--
 .../idempotent/core/aop/IdempotentAspectTest.java  | 32 ++++++++++++++++
 2 files changed, 72 insertions(+), 3 deletions(-)
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
_zs_sec011a_fixmsg.txt
_zs_sec011a_prot.txt
_zs_sec011a_tdd.ps1
_zs_sec011a_tdd.txt
_zs_sec011a_verify.ps1



exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command '$OutputEncoding = [Console]::OutputEncoding 
= [System.Text.UTF8Encoding]::new(); git show --format=fuller --no-ext-diff --no-renames b62af7d3; Get-ChildItem -Path 
. -Recurse -Force -File -Include AGENTS.md,AGENTS.override.md -ErrorAction SilentlyContinue | Select-Object -ExpandProp
erty FullName; if(Test-Path '"'C:\\Users\\Administrator\\.codex\\config.toml'){Select-String -Path 'C:\\Users\\Administ
rator\\.codex\\config.toml' -Pattern 'project_doc|fallback' }; Get-Item E:\\AGENTS.md,E:\\AGENTS.override.md -ErrorActi
on SilentlyContinue" in E:\众墅之家AI赋能平台底座
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command '$paths = @('"'E:\\','E:\\众墅之家AI赋能平台底座','E:\\
众墅之家AI赋能平台底座\\services','E:\\众墅之家AI赋能平台底座\\services\\zhongshu-core','E:\\众墅之家AI赋能平台底座\\services\\zhongshu-core\\zszj-fr
amework','E:\\众墅之家AI赋能平台底座\\services\\zhongshu-core\\zszj-framework\\zszj-spring-boot-starter-protection'); foreach ("'
$p in $paths) {foreach($n in @('"'AGENTS.override.md','AGENTS.md')){"'$f=Join-Path $p $n; if(Test-Path -LiteralPath $f)
{Write-Output "--- $f"; Get-Content -LiteralPath $f -Encoding utf8}}}; git ls-files '"'*AGENTS*' '*CLAUDE*'; if(Test-Pa
th 'C:\\Users\\Administrator\\.codex\\config.toml'){Select-String -Path 'C:\\Users\\Administrator\\.codex\\config.toml'
 -Pattern 'project_doc|fallback' }; Get-ChildItem -LiteralPath services/zhongshu-core/zszj-framework/zszj-spring-boot-s
tarter-protection -Recurse -File -Include AGENTS.override.md,AGENTS.md | ForEach-Object { "'$_.FullName; Get-Content -L
iteralPath $_.FullName -Encoding utf8 }' in E:\众墅之家AI赋能平台底座
 succeeded in 438ms:
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\.flattened-pom.xml
<?xml version="1.0" encoding="UTF-8"?>
<project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="htt
p://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <modelVersion>4.0.0</modelVersion>
  <groupId>cn.zszj</groupId>
  <artifactId>zszj-spring-boot-starter-protection</artifactId>
  <version>2026.08-SNAPSHOT</version>
  <name>zszj-spring-boot-starter-protection</name>
  <description>����֤���ṩ�ֲ�ʽ�����ݵȡ��������۶ϡ�API ǩ���ȵȹ���</description>
  <url>https://github.com/YunaiV/ruoyi-vue-pro</url>
  <dependencies>
    <dependency>
      <groupId>cn.zszj</groupId>
      <artifactId>zszj-spring-boot-starter-web</artifactId>
      <version>2026.08-SNAPSHOT</version>
      <scope>provided</scope>
    </dependency>
    <dependency>
      <groupId>cn.zszj</groupId>
      <artifactId>zszj-spring-boot-starter-redis</artifactId>
      <version>2026.08-SNAPSHOT</version>
      <scope>compile</scope>
    </dependency>
    <dependency>
      <groupId>com.baomidou</groupId>
      <artifactId>lock4j-redisson-spring-boot-starter</artifactId>
      <version>2.2.7</version>
      <scope>compile</scope>
      <exclusions>
        <exclusion>
          <groupId>org.redisson</groupId>
          <artifactId>redisson-spring-boot-starter</artifactId>
        </exclusion>
      </exclusions>
      <optional>true</optional>
    </dependency>
  </dependencies>
  <repositories>
    <repository>
      <id>huaweicloud</id>
      <name>huawei</name>
      <url>https://mirrors.huaweicloud.com/repository/maven/</url>
    </repository>
    <repository>
      <id>aliyunmaven</id>
      <name>aliyun</name>
      <url>https://maven.aliyun.com/repository/public</url>
    </repository>
  </repositories>
</project>
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\pom.xml
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
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\idempotent\package-info.java
/**
 * �ݵ�������ο� https://github.com/it4alla/idempotent ��Ŀʵ��
 * ʵ��ԭ���ǣ���ͬ�����ķ�����һ��ʱ���ڣ����ҽ���ִ��һ�Ρ�ͨ�������ķ�ʽ����֤�ݵ��ԡ�
 *
 * ʹ�ó���������˵���û����ٵ�˫����ĳ����ť��ǰ��û�н��øð�ť�����·����������ظ�������
 *
 * �� it4alla/idempotent ����Ĳ���㣬��Ҫ���������㣺
 *  1. ����ȥ���� @Idempotent ע��� delKey ���ԡ�ԭ���ǣ������� delKey Ϊ true ʱ��ʵ�ֵ��Ƿֲ�ʽ��������
 * ��ʱ������ƫ��ʹ�� Lock4j �����ԭ���ϣ�һ�����ֻ�ṩһ�ֵ�һ��������
 *  2. ���ǵ������ͨ���ԣ����ǲ�δ�� it4alla/idempotent ���һ��ʹ�� Redisson RMap �ṹ������ֱ��ʹ�� Redis �� String ���ݸ�ʽ��
 */
package cn.zszj.framework.idempotent;
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\idempotent\config\ZszjIdempotentConfiguration.java
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

    // ========== ���� IdempotentKeyResolver Bean ==========

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
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\idempotent\core\annotation\Idempotent.java
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
 * �ݵ�ע��
 *
 * @author ���Դ��
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /**
     * �ݵȵĳ�ʱʱ�䣬Ĭ��Ϊ 1 ��
     *
     * ע�⣬���ִ��ʱ�䳬�����������ǻ����
     */
    int timeout() default 1;
    /**
     * ʱ�䵥λ��Ĭ��Ϊ SECONDS ��
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * ��ʾ��Ϣ������ִ���е���ʾ
     */
    String message() default "�ظ��������Ժ�����";

    /**
     * ʹ�õ� Key ������
     *
     * @see DefaultIdempotentKeyResolver ȫ�ּ���
     * @see UserIdempotentKeyResolver �û�����
     * @see ExpressionIdempotentKeyResolver �Զ������ʽ��ͨ�� {@link #keyArg()} ����
     */
    Class<? extends IdempotentKeyResolver> keyResolver() default DefaultIdempotentKeyResolver.class;
    /**
     * ʹ�õ� Key ����
     */
    String keyArg() default "";

    /**
     * ɾ�� Key���������쳣ʱ��
     *
     * ���⣺Ϊʲô�����쳣ʱ����Ҫɾ�� Key �أ�
     * �ش𣺷����쳣ʱ��˵��ҵ�������󣬴�ʱ��Ҫɾ�� Key�������´������޷�����ִ�С�
     *
     * ���⣺Ϊʲô���� deleteWhenSuccess ִ�гɹ�ʱ����Ҫɾ�� Key �أ�
     * �ش���������£��������Ƿֲ�ʽ�����Ƽ�ʹ�� @Lock4j ע��
     */
    boolean deleteKeyWhenException() default true;

}
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\idempotent\core\aop\IdempotentAspect.java
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
     * �ų��޷���ȫ���л�����Σ�servlet / spring-web ������ʩ���󣩣��ٽ��� {@link LogSanitizeUtils#sanitizeArgs} ����ժҪ / �����
�־��
     *
     * ������codex r0 P1����SEC-011.A �Ѳ���ժҪ�ӡ����ܾ���֧������Ϊ��ÿ������㡹�����״η��У���
     * {@link LogSanitizeUtils} �ڲ��� Jackson {@code valueToTree} ���л���Σ�����ö���ȫ�� getter��
     * �� {@code HttpServletResponse}��Tomcat ResponseFacade���ᴥ�� {@code getWriter()}����ǰѡ����Ӧ�ַ����ģʽ��
     * ���º��� {@code ServletUtils.writeAttachment} �ȶ���������� {@link IllegalStateException}��
     *
     * �ų��ھ��� {@link cn.zszj.framework.common.util.string.StrUtils#joinMethodArgs} ����һ��
     * ��Ĭ�� Key ���������Ѱ����ų� servlet ���󣬴˴�����ժҪ / ��־·��������ͬһ����������ھ���һ�£���
     *
     * @param args ԭʼ�������
     * @return �޳� servlet / spring-web ��������Σ�����ԭ�����˳��null Ԫ�ر�����
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
            String clazzName = arg.getClass().getName();
            if (StrUtil.startWithAny(clazzName, "javax.servlet", "jakarta.servlet", "org.springframework.web")) {
                continue;
            }
            kept.add(arg);
        }
        return kept.toArray();
    }

}
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\idempotent\core\keyresolver\IdempotentKeyResolver.java
package cn.zszj.framework.idempotent.core.keyresolver;

import cn.zszj.framework.idempotent.core.annotation.Idempotent;
import org.aspectj.lang.JoinPoint;

/**
 * �ݵ� Key �������ӿ�
 *
 * @author ���Դ��
 */
public interface IdempotentKeyResolver {

    /**
     * ����һ�� Key
     *
     * @param idempotent �ݵ�ע��
     * @param joinPoint  AOP ����
     * @return Key
     */
    String resolver(JoinPoint joinPoint, Idempotent idempotent);

}
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\idempotent\core\keyresolver\impl\DefaultIdempotentKeyResolver.java
package cn.zszj.framework.idempotent.core.keyresolver.impl;

import cn.hutool.crypto.SecureUtil;
import cn.zszj.framework.common.util.servlet.ServletUtils;
import cn.zszj.framework.common.util.string.StrUtils;
import cn.zszj.framework.idempotent.core.annotation.Idempotent;
import cn.zszj.framework.idempotent.core.keyresolver.IdempotentKeyResolver;
import cn.zszj.framework.web.core.util.WebFrameworkUtils;
import org.aspectj.lang.JoinPoint;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Ĭ�ϣ�ȫ�ּ����ݵ� Key ��������ʹ�÷����� + �������� + �⻧ + �û����壬��װ��һ�� Key
 *
 * Ϊ�˱��� Key ������ʹ�� MD5 ����"ѹ��"
 *
 * ZS-SEC-011.A�������⻧ + �û�����������ʵ��������루��ͬ����/�⻧�����������ݵȽ����
 *
 * ��������Դ˵������ SEC-010 ͬ��ȣ����⻧/�û�����ȡ�Ե�ǰ����������
 * ��{@link ServletUtils#getRequest()} + {@link WebFrameworkUtils} �� getTenantId/getLoginUserId/getLoginUserType������
 * �⻧���� tenant-id ����ͷ���� TenantContextHolder ͬԴ���� TenantSecurityWebFilter У�飬����α��������⻧����
 * userId/userType ����Ȩ�� request attribute������α�죩��
 * �����������ģ����첽/��ʱ����ʱ���߾��� null ռλƴ�ӣ����� NPE��
 * ע�⣺ʡ�� tenant-id ͷ��õ���ͬ Key��ͬ�������ƹ����ش��ڣ��ǿ�����ԽȨ����
 * ���� argsStr �Ѻ��� Key��ͬ����Ȼͬ�Σ�����ġ�ͬ����γ�ͻ����֧�Ա��������㲻�ɴ
 * ����ȱ�ڵ�Ȩ���⻧Դ��ע�� port �� SubjectScope ͳһ��ȡ�� REC-1����� IdempotentAspect ��ע�͵���֪ȱ�ڵǼǣ���
 *
 * @author ���Դ��
 */
public class DefaultIdempotentKeyResolver implements IdempotentKeyResolver {

    @Override
    public String resolver(JoinPoint joinPoint, Idempotent idempotent) {
        String methodName = joinPoint.getSignature().toString();
        String argsStr = StrUtils.joinMethodArgs(joinPoint);
        // ZS-SEC-011.A�����⻧ + ����������ʵ��������루��ͬ����/�⻧�����������ݵȽ����
        HttpServletRequest request = ServletUtils.getRequest();
        Long tenantId = request != null ? WebFrameworkUtils.getTenantId(request) : null;
        Long userId = request != null ? WebFrameworkUtils.getLoginUserId(request) : null;
        Integer userType = request != null ? WebFrameworkUtils.getLoginUserType(request) : null;
        return SecureUtil.md5(methodName + ":" + tenantId + ":" + userId + ":" + userType + ":" + argsStr);
    }

}
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\idempotent\core\keyresolver\impl\ExpressionIdempotentKeyResolver.java
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
 * ���� Spring EL ����ʽ��
 *
 * @author ���Դ��
 */
public class ExpressionIdempotentKeyResolver implements IdempotentKeyResolver {

    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    private final ExpressionParser expressionParser = new SpelExpressionParser();

    @Override
    public String resolver(JoinPoint joinPoint, Idempotent idempotent) {
        // ��ñ����ط����������б�
        Method method = getMethod(joinPoint);
        Object[] args = joinPoint.getArgs();
        String[] parameterNames = this.parameterNameDiscoverer.getParameterNames(method);
        // ׼�� Spring EL ����ʽ������������
        StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
        if (ArrayUtil.isNotEmpty(parameterNames)) {
            for (int i = 0; i < parameterNames.length; i++) {
                evaluationContext.setVariable(parameterNames[i], args[i]);
            }
        }

        // ��������
        Expression expression = expressionParser.parseExpression(idempotent.keyArg());
        return expression.getValue(evaluationContext, String.class);
    }

    private static Method getMethod(JoinPoint point) {
        // ���������������ϵ����
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        if (!method.getDeclaringClass().isInterface()) {
            return method;
        }

        // �����������ڽӿ��ϵ����
        try {
            return point.getTarget().getClass().getDeclaredMethod(
                    point.getSignature().getName(), method.getParameterTypes());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

}
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\idempotent\core\keyresolver\impl\UserIdempotentKeyResolver.java
package cn.zszj.framework.idempotent.core.keyresolver.impl;

import cn.hutool.crypto.SecureUtil;
import cn.zszj.framework.common.util.string.StrUtils;
import cn.zszj.framework.idempotent.core.annotation.Idempotent;
import cn.zszj.framework.idempotent.core.keyresolver.IdempotentKeyResolver;
import cn.zszj.framework.web.core.util.WebFrameworkUtils;
import org.aspectj.lang.JoinPoint;

/**
 * �û�������ݵ� Key ��������ʹ�÷����� + �������� + userId + userType����װ��һ�� Key
 *
 * Ϊ�˱��� Key ������ʹ�� MD5 ���С�ѹ����
 *
 * @author ���Դ��
 */
public class UserIdempotentKeyResolver implements IdempotentKeyResolver {

    @Override
    public String resolver(JoinPoint joinPoint, Idempotent idempotent) {
        String methodName = joinPoint.getSignature().toString();
        String argsStr = StrUtils.joinMethodArgs(joinPoint);
        Long userId = WebFrameworkUtils.getLoginUserId();
        Integer userType = WebFrameworkUtils.getLoginUserType();
        return SecureUtil.md5(methodName + argsStr + userId + userType);
    }

}
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\idempotent\core\redis\IdempotentRedisDAO.java
package cn.zszj.framework.idempotent.core.redis;

import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * �ݵ� Redis DAO
 *
 * ZS-SEC-011.A��value �洢����ժҪ��digest��������ͬ����γ�ͻ���
 *
 * @author ���Դ��
 */
@AllArgsConstructor
public class IdempotentRedisDAO {

    /**
     * �ݵȲ���
     *
     * KEY ��ʽ��idempotent:%s // ����Ϊ uuid
     * VALUE ��ʽ��String������ժҪ MD5��
     * ����ʱ�䣺���̶�
     */
    private static final String IDEMPOTENT = "idempotent:%s";

    private final StringRedisTemplate redisTemplate;

    /**
     * �����ݵȼ���Я������ժҪ������������������óɹ�
     *
     * @param key      �ݵȼ�
     * @param digest   ����ժҪ��MD5��
     * @param timeout  ��ʱʱ��
     * @param timeUnit ʱ�䵥λ
     * @return �Ƿ����óɹ���true = �״�����false = �ظ�����
     */
    public Boolean setIfAbsent(String key, String digest, long timeout, TimeUnit timeUnit) {
        String redisKey = formatKey(key);
        return redisTemplate.opsForValue().setIfAbsent(redisKey, digest, timeout, timeUnit);
    }

    /**
     * ��ȡ�Ѵ洢�Ĳ���ժҪ
     *
     * @param key �ݵȼ�
     * @return ����ժҪ�������ڷ��� null
     */
    public String getDigest(String key) {
        return redisTemplate.opsForValue().get(formatKey(key));
    }

    public void delete(String key) {
        String redisKey = formatKey(key);
        redisTemplate.delete(redisKey);
    }

    private static String formatKey(String key) {
        return String.format(IDEMPOTENT, key);
    }

}
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\lock4j\package-info.java
/**
 * �ֲ�ʽ�������ʹ�� https://gitee.com/baomidou/lock4j ��Դ��Ŀ
 */
package cn.zszj.framework.lock4j;
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\lock4j\config\ZszjLock4jConfiguration.java
package cn.zszj.framework.lock4j.config;

import cn.zszj.framework.lock4j.core.DefaultLockFailureStrategy;
import com.baomidou.lock.spring.boot.autoconfigure.LockAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(before = LockAutoConfiguration.class)
@ConditionalOnClass(name = "com.baomidou.lock.annotation.Lock4j")
public class ZszjLock4jConfiguration {

    @Bean
    public DefaultLockFailureStrategy lockFailureStrategy() {
        return new DefaultLockFailureStrategy();
    }

}
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\lock4j\core\DefaultLockFailureStrategy.java
package cn.zszj.framework.lock4j.core;

import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants;
import com.baomidou.lock.LockFailureStrategy;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;

/**
 * �Զ����ȡ��ʧ�ܲ��ԣ��׳� {@link ServiceException} �쳣
 */
@Slf4j
public class DefaultLockFailureStrategy implements LockFailureStrategy {

    @Override
    public void onLockFailure(String key, Method method, Object[] arguments) {
        log.debug("[onLockFailure][�߳�:{} ��ȡ��ʧ�ܣ�key:{} ��ȡʧ��:{} ]", Thread.currentThread().getName(), key, argument
s);
        throw new ServiceException(GlobalErrorCodeConstants.LOCKED);
    }
}
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\lock4j\core\Lock4jRedisKeyConstants.java
package cn.zszj.framework.lock4j.core;

/**
 * Lock4j Redis Key ö����
 *
 * @author ���Դ��
 */
public interface Lock4jRedisKeyConstants {

    /**
     * �ֲ�ʽ��
     *
     * KEY ��ʽ��lock4j:%s // �������� DefaultLockKeyBuilder ��
     * VALUE ���ݸ�ʽ��HASH // RLock.class��Redisson �� Lock ����ʹ�� Hash ���ݽṹ
     * ����ʱ�䣺���̶�
     */
    String LOCK4J = "lock4j:%s";

}
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\ratelimiter\package-info.java
/**
 * ������������� Redisson {@link org.redisson.api.RRateLimiter} ����ʵ��
 */
package cn.zszj.framework.ratelimiter;
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\ratelimiter\config\ZszjRateLimiterConfiguration.java
package cn.zszj.framework.ratelimiter.config;

import cn.zszj.framework.ratelimiter.core.aop.RateLimiterAspect;
import cn.zszj.framework.ratelimiter.core.keyresolver.RateLimiterKeyResolver;
import cn.zszj.framework.ratelimiter.core.keyresolver.impl.*;
import cn.zszj.framework.ratelimiter.core.redis.RateLimiterRedisDAO;
import cn.zszj.framework.redis.config.ZszjRedisAutoConfiguration;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.List;

@AutoConfiguration(after = ZszjRedisAutoConfiguration.class)
public class ZszjRateLimiterConfiguration {

    @Bean
    public RateLimiterAspect rateLimiterAspect(List<RateLimiterKeyResolver> keyResolvers, RateLimiterRedisDAO rateLimit
erRedisDAO) {
        return new RateLimiterAspect(keyResolvers, rateLimiterRedisDAO);
    }

    @Bean
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public RateLimiterRedisDAO rateLimiterRedisDAO(RedissonClient redissonClient) {
        return new RateLimiterRedisDAO(redissonClient);
    }

    // ========== ���� RateLimiterRedisDAO Bean ==========

    @Bean
    public DefaultRateLimiterKeyResolver defaultRateLimiterKeyResolver() {
        return new DefaultRateLimiterKeyResolver();
    }

    @Bean
    public UserRateLimiterKeyResolver userRateLimiterKeyResolver() {
        return new UserRateLimiterKeyResolver();
    }

    @Bean
    public ClientIpRateLimiterKeyResolver clientIpRateLimiterKeyResolver() {
        return new ClientIpRateLimiterKeyResolver();
    }

    @Bean
    public ServerNodeRateLimiterKeyResolver serverNodeRateLimiterKeyResolver() {
        return new ServerNodeRateLimiterKeyResolver();
    }

    @Bean
    public ExpressionRateLimiterKeyResolver expressionRateLimiterKeyResolver() {
        return new ExpressionRateLimiterKeyResolver();
    }

}
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\ratelimiter\core\annotation\RateLimiter.java
package cn.zszj.framework.ratelimiter.core.annotation;

import cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.zszj.framework.idempotent.core.keyresolver.impl.ExpressionIdempotentKeyResolver;
import cn.zszj.framework.ratelimiter.core.keyresolver.RateLimiterKeyResolver;
import cn.zszj.framework.ratelimiter.core.keyresolver.impl.ClientIpRateLimiterKeyResolver;
import cn.zszj.framework.ratelimiter.core.keyresolver.impl.DefaultRateLimiterKeyResolver;
import cn.zszj.framework.ratelimiter.core.keyresolver.impl.ServerNodeRateLimiterKeyResolver;
import cn.zszj.framework.ratelimiter.core.keyresolver.impl.UserRateLimiterKeyResolver;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * ����ע��
 *
 * @author ���Դ��
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimiter {

    /**
     * ������ʱ�䣬Ĭ��Ϊ 1 ��
     */
    int time() default 1;
    /**
     * ʱ�䵥λ��Ĭ��Ϊ SECONDS ��
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * ��������
     */
    int count() default 100;

    /**
     * ��ʾ��Ϣ������������ʾ
     *
     * @see GlobalErrorCodeConstants#TOO_MANY_REQUESTS
     */
    String message() default ""; // Ϊ��ʱ��ʹ�� TOO_MANY_REQUESTS ������ʾ

    /**
     * ʹ�õ� Key ������
     *
     * @see DefaultRateLimiterKeyResolver ȫ�ּ���
     * @see UserRateLimiterKeyResolver �û� ID ����
     * @see ClientIpRateLimiterKeyResolver �û� IP ����
     * @see ServerNodeRateLimiterKeyResolver ������ Node ����
     * @see ExpressionIdempotentKeyResolver �Զ������ʽ��ͨ�� {@link #keyArg()} ����
     */
    Class<? extends RateLimiterKeyResolver> keyResolver() default DefaultRateLimiterKeyResolver.class;
    /**
     * ʹ�õ� Key ����
     */
    String keyArg() default "";

    /**
     * �˵㼶�������������������ܾ�ʱ��������ƾ�ݸ�����password / token / secret �ȣ��⣬
     * ������Ҫ����Ĳ��������ֶ������������֤�� code�����������������������ܾ���־��
     *
     * <p>������Сд�����У��ڲ����һ��Сд��ȥ�»��� / ���ַ�����ȷƥ�䡣
     *
     * @see cn.zszj.framework.common.util.log.LogSanitizeUtils
     */
    String[] maskKeys() default {};

}
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\ratelimiter\core\aop\RateLimiterAspect.java
package cn.zszj.framework.ratelimiter.core.aop;

import cn.hutool.core.util.StrUtil;
import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.zszj.framework.common.util.collection.CollectionUtils;
import cn.zszj.framework.common.util.log.LogSanitizeUtils;
import cn.zszj.framework.ratelimiter.core.annotation.RateLimiter;
import cn.zszj.framework.ratelimiter.core.keyresolver.RateLimiterKeyResolver;
import cn.zszj.framework.ratelimiter.core.redis.RateLimiterRedisDAO;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ���������� {@link RateLimiter} ע��ķ�����ʵ����������
 *
 * @author ���Դ��
 */
@Aspect
@Slf4j
public class RateLimiterAspect {

    /**
     * RateLimiterKeyResolver ����
     */
    private final Map<Class<? extends RateLimiterKeyResolver>, RateLimiterKeyResolver> keyResolvers;

    private final RateLimiterRedisDAO rateLimiterRedisDAO;

    public RateLimiterAspect(List<RateLimiterKeyResolver> keyResolvers, RateLimiterRedisDAO rateLimiterRedisDAO) {
        this.keyResolvers = CollectionUtils.convertMap(keyResolvers, RateLimiterKeyResolver::getClass);
        this.rateLimiterRedisDAO = rateLimiterRedisDAO;
    }

    @Before("@annotation(rateLimiter)")
    public void beforePointCut(JoinPoint joinPoint, RateLimiter rateLimiter) {
        // ��� RateLimiterKeyResolver ����
        RateLimiterKeyResolver keyResolver = keyResolvers.get(rateLimiter.keyResolver());
        Assert.notNull(keyResolver, "�Ҳ�����Ӧ�� RateLimiterKeyResolver");
        // ���� Key
        String key = keyResolver.resolver(joinPoint, rateLimiter);

        // ��ȡ 1 ������
        boolean success = rateLimiterRedisDAO.tryAcquire(key,
                rateLimiter.count(), rateLimiter.time(), rateLimiter.timeUnit());
        if (!success) {
            // SEC-010�������ܾ���־���������� -> ֵ������������ƾ�ݣ��� refreshToken������������и������룬
            // �˵㼶�����ֶΣ������ code���� @RateLimiter#maskKeys ��ȷ���룬��������ƾ������־
            log.info("[beforePointCut][����({}) ����({}) �������Ƶ��]", joinPoint.getSignature().toString(),
                    LogSanitizeUtils.sanitizeMap(buildArgMap(joinPoint), rateLimiter.maskKeys()));
            String message = StrUtil.blankToDefault(rateLimiter.message(),
                    GlobalErrorCodeConstants.TOO_MANY_REQUESTS.getMsg());
            throw new ServiceException(GlobalErrorCodeConstants.TOO_MANY_REQUESTS.getCode(), message);
        }
    }

    /**
     * �� joinPoint ��ι���Ϊ�������� -> ����ֵ��������ӳ�䣬�� {@link LogSanitizeUtils#sanitizeMap} ����������֪������
     *
     * <p>������Σ��� refreshToken���޷���ֵ����ʶ�������ԣ��������������һ�� token ���������ɱ��Զ����룻
     * �ò�����������ǩ���� {@link MethodSignature} ������ȱʧ��ʱ�˻�Ϊλ���� {@code argN}�������������
     */
    private static Map<String, Object> buildArgMap(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return Collections.emptyMap();
        }
        Signature signature = joinPoint.getSignature();
        String[] parameterNames = signature instanceof MethodSignature
                ? ((MethodSignature) signature).getParameterNames() : null;
        Map<String, Object> argMap = new LinkedHashMap<>(args.length);
        for (int i = 0; i < args.length; i++) {
            String name = parameterNames != null && i < parameterNames.length && StrUtil.isNotEmpty(parameterNames[i])
                    ? parameterNames[i] : "arg" + i;
            argMap.put(name, args[i]);
        }
        return argMap;
    }

}

E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\ratelimiter\core\keyresolver\RateLimiterKeyResolver.java
package cn.zszj.framework.ratelimiter.core.keyresolver;

import cn.zszj.framework.ratelimiter.core.annotation.RateLimiter;
import org.aspectj.lang.JoinPoint;

/**
 * ���� Key �������ӿ�
 *
 * @author ���Դ��
 */
public interface RateLimiterKeyResolver {

    /**
     * ����һ�� Key
     *
     * @param rateLimiter ����ע��
     * @param joinPoint  AOP ����
     * @return Key
     */
    String resolver(JoinPoint joinPoint, RateLimiter rateLimiter);

}
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\ratelimiter\core\keyresolver\impl\ClientIpRateLimiterKeyResolver.java
package cn.zszj.framework.ratelimiter.core.keyresolver.impl;

import cn.hutool.crypto.SecureUtil;
import cn.zszj.framework.common.util.servlet.ServletUtils;
import cn.zszj.framework.common.util.string.StrUtils;
import cn.zszj.framework.ratelimiter.core.annotation.RateLimiter;
import cn.zszj.framework.ratelimiter.core.keyresolver.RateLimiterKeyResolver;
import org.aspectj.lang.JoinPoint;

/**
 * IP ��������� Key ��������ʹ�÷����� + �������� + IP����װ��һ�� Key
 *
 * Ϊ�˱��� Key ������ʹ�� MD5 ���С�ѹ����
 *
 * @author ���Դ��
 */
public class ClientIpRateLimiterKeyResolver implements RateLimiterKeyResolver {

    @Override
    public String resolver(JoinPoint joinPoint, RateLimiter rateLimiter) {
        String methodName = joinPoint.getSignature().toString();
        String argsStr = StrUtils.joinMethodArgs(joinPoint);
        String clientIp = ServletUtils.getClientIP();
        return SecureUtil.md5(methodName + argsStr + clientIp);
    }

}
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\ratelimiter\core\keyresolver\impl\DefaultRateLimiterKeyResolver.java
package cn.zszj.framework.ratelimiter.core.keyresolver.impl;

import cn.hutool.crypto.SecureUtil;
import cn.zszj.framework.common.util.string.StrUtils;
import cn.zszj.framework.ratelimiter.core.annotation.RateLimiter;
import cn.zszj.framework.ratelimiter.core.keyresolver.RateLimiterKeyResolver;
import org.aspectj.lang.JoinPoint;

/**
 * Ĭ�ϣ�ȫ�ּ������� Key ��������ʹ�÷����� + ������������װ��һ�� Key
 *
 * Ϊ�˱��� Key ������ʹ�� MD5 ���С�ѹ����
 *
 * @author ���Դ��
 */
public class DefaultRateLimiterKeyResolver implements RateLimiterKeyResolver {

    @Override
    public String resolver(JoinPoint joinPoint, RateLimiter rateLimiter) {
        String methodName = joinPoint.getSignature().toString();
        String argsStr = StrUtils.joinMethodArgs(joinPoint);
        return SecureUtil.md5(methodName + argsStr);
    }

}
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\ratelimiter\core\keyresolver\impl\ExpressionRateLimiterKeyResolver.java
package cn.zszj.framework.ratelimiter.core.keyresolver.impl;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.crypto.SecureUtil;
import cn.zszj.framework.common.util.servlet.ServletUtils;
import cn.zszj.framework.ratelimiter.core.annotation.RateLimiter;
import cn.zszj.framework.ratelimiter.core.keyresolver.RateLimiterKeyResolver;
import cn.zszj.framework.web.core.util.WebFrameworkUtils;
import jakarta.servlet.http.HttpServletRequest;
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
 * ���� Spring EL ����ʽ�� {@link RateLimiterKeyResolver} ʵ����
 *
 * ZS-SEC-010���ڱ���ʽ��õġ����塹�����ֻ��š��û��������ƣ�֮�ϣ�׷�� methodName + tenantId ������
 * ���� MD5 ѹ������������˵㡢���⻧����ͬһ��ȡ������⡣
 * ���� Key ֻȡ�����ֶΡ�����������Σ��Ķ����� / ��֤�����ͨ����������� Key���̶������������ɱ���ܣ���
 * ����ͻ��� IP �޹أ�α��ת�� IP �����ƹ�����
 *
 * @author ���Դ��
 */
public class ExpressionRateLimiterKeyResolver implements RateLimiterKeyResolver {

    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    private final ExpressionParser expressionParser = new SpelExpressionParser();

    @Override
    public String resolver(JoinPoint joinPoint, RateLimiter rateLimiter) {
        // ��ñ����ط����������б�
        Method method = getMethod(joinPoint);
        Object[] args = joinPoint.getArgs();
        String[] parameterNames = this.parameterNameDiscoverer.getParameterNames(method);
        // ׼�� Spring EL ����ʽ������������
        StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
        if (ArrayUtil.isNotEmpty(parameterNames)) {
            for (int i = 0; i < parameterNames.length; i++) {
                evaluationContext.setVariable(parameterNames[i], args[i]);
            }
        }

        // ��������������������壨���ֻ��š��û��������ƣ�
        Expression expression = expressionParser.parseExpression(rateLimiter.keyArg());
        String subject = expression.getValue(evaluationContext, String.class);

        // ZS-SEC-010��Key ׷�� methodName + tenantId �����򣬶¿�˵� / ���⻧���ö�ȡ�
        // �⻧��Դ�� TenantContextHolder ͬԴ��tenant-id ����ͷ����ֱ�Ӷ�ͷ������������ protection �������� biz-tenant���ֲ㵹�ã��
�
        // request Ϊ null���� web �����ģ�ʱ tenantId �� null ռλ����������
        HttpServletRequest request = ServletUtils.getRequest();
        Long tenantId = request != null ? WebFrameworkUtils.getTenantId(request) : null;
        String methodName = joinPoint.getSignature().toString();
        return SecureUtil.md5(methodName + ":" + tenantId + ":" + subject);
    }

    private static Method getMethod(JoinPoint point) {
        // ���������������ϵ����
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        if (!method.getDeclaringClass().isInterface()) {
            return method;
        }

        // �����������ڽӿ��ϵ����
        try {
            return point.getTarget().getClass().getDeclaredMethod(
                    point.getSignature().getName(), method.getParameterTypes());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

}
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\ratelimiter\core\keyresolver\impl\ServerNodeRateLimiterKeyResolver.java
package cn.zszj.framework.ratelimiter.core.keyresolver.impl;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.system.SystemUtil;
import cn.zszj.framework.common.util.string.StrUtils;
import cn.zszj.framework.ratelimiter.core.annotation.RateLimiter;
import cn.zszj.framework.ratelimiter.core.keyresolver.RateLimiterKeyResolver;
import org.aspectj.lang.JoinPoint;

/**
 * Server �ڵ㼶������� Key ��������ʹ�÷����� + �������� + IP����װ��һ�� Key
 *
 * Ϊ�˱��� Key ������ʹ�� MD5 ���С�ѹ����
 *
 * @author ���Դ��
 */
public class ServerNodeRateLimiterKeyResolver implements RateLimiterKeyResolver {

    @Override
    public String resolver(JoinPoint joinPoint, RateLimiter rateLimiter) {
        String methodName = joinPoint.getSignature().toString();
        String argsStr = StrUtils.joinMethodArgs(joinPoint);
        String serverNode = String.format("%s@%d", SystemUtil.getHostInfo().getAddress(), SystemUtil.getCurrentPID());
        return SecureUtil.md5(methodName + argsStr + serverNode);
    }

}
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\ratelimiter\core\keyresolver\impl\UserRateLimiterKeyResolver.java
package cn.zszj.framework.ratelimiter.core.keyresolver.impl;

import cn.hutool.crypto.SecureUtil;
import cn.zszj.framework.common.util.string.StrUtils;
import cn.zszj.framework.ratelimiter.core.annotation.RateLimiter;
import cn.zszj.framework.ratelimiter.core.keyresolver.RateLimiterKeyResolver;
import cn.zszj.framework.web.core.util.WebFrameworkUtils;
import org.aspectj.lang.JoinPoint;

/**
 * �û���������� Key ��������ʹ�÷����� + �������� + userId + userType����װ��һ�� Key
 *
 * Ϊ�˱��� Key ������ʹ�� MD5 ���С�ѹ����
 *
 * @author ���Դ��
 */
public class UserRateLimiterKeyResolver implements RateLimiterKeyResolver {

    @Override
    public String resolver(JoinPoint joinPoint, RateLimiter rateLimiter) {
        String methodName = joinPoint.getSignature().toString();
        String argsStr = StrUtils.joinMethodArgs(joinPoint);
        Long userId = WebFrameworkUtils.getLoginUserId();
        Integer userType = WebFrameworkUtils.getLoginUserType();
        return SecureUtil.md5(methodName + argsStr + userId + userType);
    }

}
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\ratelimiter\core\redis\RateLimiterRedisDAO.java
package cn.zszj.framework.ratelimiter.core.redis;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.*;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * ���� Redis DAO
 *
 * @author ���Դ��
 */
@Slf4j
@AllArgsConstructor
public class RateLimiterRedisDAO {

    /**
     * ��������
     *
     * KEY ��ʽ��rate_limiter:%s // ����Ϊ uuid
     * VALUE ��ʽ��String
     * ����ʱ�䣺���̶�
     */
    private static final String RATE_LIMITER = "rate_limiter:%s";

    private final RedissonClient redissonClient;

    public Boolean tryAcquire(String key, int count, int time, TimeUnit timeUnit) {
        try {
            // 1. ��� RRateLimiter�������� rate ����
            RRateLimiter rateLimiter = getRRateLimiter(key, count, time, timeUnit);
            // 2. ���Ի�ȡ 1 ��
            return rateLimiter.tryAcquire();
        } catch (Exception ex) {
            // ZS-SEC-010��Redis ����Ĭ�� fail-open������ + �澯�������⻺�涶������ȫվ��¼��
            // δ������ fail-closed�����ڴ˽��� zszj.ratelimiter.fail-strategy ���Կ��أ�������ʵ�֣�YAGNI����
            log.error("[tryAcquire][���� Key({}) Redis ���ϣ��� fail-open ����]", key, ex);
            return Boolean.TRUE;
        }
    }

    private static String formatKey(String key) {
        return String.format(RATE_LIMITER, key);
    }

    private RRateLimiter getRRateLimiter(String key, long count, int time, TimeUnit timeUnit) {
        String redisKey = formatKey(key);
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(redisKey);
        long rateInterval = timeUnit.toSeconds(time);
        Duration duration = Duration.ofSeconds(rateInterval);
        // 1. ��������ڣ����� rate ����
        RateLimiterConfig config = rateLimiter.getConfig();
        if (config == null) {
            rateLimiter.trySetRate(RateType.OVERALL, count, duration);
            // ԭ��μ� https://t.zsxq.com/lcR0W
            rateLimiter.expire(duration);
            return rateLimiter;
        }
        // 2. ������ڣ�����������ͬ����ֱ�ӷ���
        if (config.getRateType() == RateType.OVERALL
                && Objects.equals(config.getRate(), count)
                && Objects.equals(config.getRateInterval(), TimeUnit.SECONDS.toMillis(rateInterval))) {
            return rateLimiter;
        }
        // 3. ������ڣ��������ò�ͬ��������½�
        rateLimiter.setRate(RateType.OVERALL, count, duration);
        // ԭ��μ� https://t.zsxq.com/lcR0W
        rateLimiter.expire(duration);
        return rateLimiter;
    }

}
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\signature\package-info.java
/**
 * HTTP API ǩ����У�鰲ȫ��
 *
 * @see <a href="https://pay.weixin.qq.com/wiki/doc/api/jsapi.php?chapter=4_3>΢��֧�� ���� ��ȫ�淶</a>
 */
package cn.zszj.framework.signature;
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\signature\config\ZszjApiSignatureAutoConfiguration.java
package cn.zszj.framework.signature.config;

import cn.zszj.framework.redis.config.ZszjRedisAutoConfiguration;
import cn.zszj.framework.signature.core.aop.ApiSignatureAspect;
import cn.zszj.framework.signature.core.redis.ApiSignatureRedisDAO;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * HTTP API ǩ�����Զ�������
 *
 * @author Zhougang
 */
@AutoConfiguration(after = ZszjRedisAutoConfiguration.class)
public class ZszjApiSignatureAutoConfiguration {

    @Bean
    public ApiSignatureAspect signatureAspect(ApiSignatureRedisDAO signatureRedisDAO) {
        return new ApiSignatureAspect(signatureRedisDAO);
    }

    @Bean
    public ApiSignatureRedisDAO signatureRedisDAO(StringRedisTemplate stringRedisTemplate) {
        return new ApiSignatureRedisDAO(stringRedisTemplate);
    }

}
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\signature\core\annotation\ApiSignature.java
package cn.zszj.framework.signature.core.annotation;

import cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;


/**
 * HTTP API ǩ��ע��
 *
 * @author Zhougang
 */
@Inherited
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiSignature {

    /**
     * ͬһ������೤ʱ������Ч Ĭ�� 60 ��
     */
    int timeout() default 60;

    /**
     * ʱ�䵥λ��Ĭ��Ϊ SECONDS ��
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    // ========================== ǩ������ ==========================

    /**
     * ��ʾ��Ϣ��ǩ��ʧ�ܵ���ʾ
     *
     * @see GlobalErrorCodeConstants#BAD_REQUEST
     */
    String message() default "ǩ������ȷ"; // Ϊ��ʱ��ʹ�� BAD_REQUEST ������ʾ

    /**
     * ǩ���ֶΣ�appId Ӧ��ID
     */
    String appId() default "appId";

    /**
     * ǩ���ֶΣ�timestamp ʱ���
     */
    String timestamp() default "timestamp";

    /**
     * ǩ���ֶΣ�nonce �������10 λ����
     */
    String nonce() default "nonce";

    /**
     * sign �ͻ���ǩ��
     */
    String sign() default "sign";

}
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\signature\core\aop\ApiSignatureAspect.java
package cn.zszj.framework.signature.core.aop;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.zszj.framework.common.util.log.LogSanitizeUtils;
import cn.zszj.framework.common.util.servlet.ServletUtils;
import cn.zszj.framework.signature.core.annotation.ApiSignature;
import cn.zszj.framework.signature.core.redis.ApiSignatureRedisDAO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

import static cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;

/**
 * ���������� {@link ApiSignature} ע��ķ�����ʵ��ǩ��
 *
 * @author Zhougang
 */
@Aspect
@Slf4j
@AllArgsConstructor
public class ApiSignatureAspect {

    private final ApiSignatureRedisDAO signatureRedisDAO;

    @Before("@annotation(signature)")
    public void beforePointCut(JoinPoint joinPoint, ApiSignature signature) {
        // 1. ��֤ͨ����ֱ�ӽ���
        if (verifySignature(signature, Objects.requireNonNull(ServletUtils.getRequest()))) {
            return;
        }

        // 2. ��֤��ͨ�����׳��쳣
        // ��ȫ��ZS-SEC-007�������������Ⱦ� LogSanitizeUtils.sanitizeArgs ��������������/���Ƶ�ƾ����ǩ��ʧ����־й¶
        log.error("[beforePointCut][����{} ����({}) ǩ��ʧ��]", joinPoint.getSignature().toString(),
                LogSanitizeUtils.sanitizeArgs(joinPoint.getArgs()));
        throw new ServiceException(BAD_REQUEST.getCode(),
                StrUtil.blankToDefault(signature.message(), BAD_REQUEST.getMsg()));
    }

    public boolean verifySignature(ApiSignature signature, HttpServletRequest request) {
        // 1.1 У�� Header
        if (!verifyHeaders(signature, request)) {
            return false;
        }
        // 1.2 У�� appId �Ƿ��ܻ�ȡ����Ӧ�� appSecret
        String appId = request.getHeader(signature.appId());
        String appSecret = signatureRedisDAO.getAppSecret(appId);
        Assert.notNull(appSecret, "[appId({})] �Ҳ�����Ӧ�� appSecret", appId);

        // 2. У��ǩ������Ҫ����
        String clientSignature = request.getHeader(signature.sign()); // �ͻ���ǩ��
        String serverSignatureString = buildSignatureString(signature, request, appSecret); // �����ǩ���ַ���
        String serverSignature = DigestUtil.sha256Hex(serverSignatureString); // �����ǩ��
        if (ObjUtil.notEqual(clientSignature, serverSignature)) {
            return false;
        }

        // 3. �� nonce ���뻺�棬��ֹ�ظ�ʹ�ã��ص�����˴���Ҫ�� ttl �趨Ϊ���� timestamp ʱ����ֵ x 2 ��
        String nonce = request.getHeader(signature.nonce());
        if (BooleanUtil.isFalse(signatureRedisDAO.setNonce(appId, nonce, signature.timeout() * 2, signature.timeUnit())
)) {
            String timestamp = request.getHeader(signature.timestamp());
            // ��ȫ��ZS-SEC-007����sign Ϊƾ������ֵ�����봦����appId/timestamp/nonce �������ڶ�λ�ظ�����
            log.info("[verifySignature][appId({}) timestamp({}) nonce({}) sign({}) �����ظ�����]", appId, timestamp, non
ce, LogSanitizeUtils.MASK);
            throw new ServiceException(GlobalErrorCodeConstants.REPEATED_REQUESTS.getCode(), "�����ظ�����");
        }
        return true;
    }

    /**
     * У������ͷ��ǩ����
     * <p>
     * 1. appId �Ƿ�Ϊ��
     * 2. timestamp �Ƿ�Ϊ�գ������Ƿ��Ѿ���ʱ��Ĭ�� 10 ����
     * 3. nonce �Ƿ�Ϊ�գ�������Ƿ� 10 λ���ϣ��Ƿ��ڹ涨ʱ�����Ѿ����ʹ���
     * 4. sign �Ƿ�Ϊ��
     *
     * @param signature signature
     * @param request   request
     * @return �Ƿ�У�� Header ͨ��
     */
    private boolean verifyHeaders(ApiSignature signature, HttpServletRequest request) {
        // 1. �ǿ�У��
        String appId = request.getHeader(signature.appId());
        if (StrUtil.isBlank(appId)) {
            return false;
        }
        String timestamp = request.getHeader(signature.timestamp());
        if (StrUtil.isBlank(timestamp)) {
            return false;
        }
        String nonce = request.getHeader(signature.nonce());
        if (StrUtil.length(nonce) < 10) {
            return false;
        }
        String sign = request.getHeader(signature.sign());
        if (StrUtil.isBlank(sign)) {
            return false;
        }

        // 2. ��� timestamp �Ƿ񳬳������ķ�Χ ���ص�һ���˴���Ҫȡ����ֵ��
        long expireTime = signature.timeUnit().toMillis(signature.timeout());
        long requestTimestamp = Long.parseLong(timestamp);
        long timestampDisparity = Math.abs(System.currentTimeMillis() - requestTimestamp);
        if (timestampDisparity > expireTime) {
            return false;
        }

        // 3. ��� nonce �Ƿ���ڣ����ҽ���ʹ��һ��
        return signatureRedisDAO.getNonce(appId, nonce) == null;
    }

    /**
     * ����ǩ���ַ���
     * <p>
     * ��ʽΪ = ������� + ������ + ����ͷ + ��Կ
     *
     * @param signature signature
     * @param request   request
     * @param appSecret appSecret
     * @return ǩ���ַ���
     */
    private String buildSignatureString(ApiSignature signature, HttpServletRequest request, String appSecret) {
        SortedMap<String, String> parameterMap = getRequestParameterMap(request); // ����ͷ
        SortedMap<String, String> headerMap = getRequestHeaderMap(signature, request); // �������
        String requestBody = StrUtil.nullToDefault(ServletUtils.getBody(request), ""); // ������
        return MapUtil.join(parameterMap, "&", "=")
                + requestBody
                + MapUtil.join(headerMap, "&", "=")
                + appSecret;
    }

    /**
     * ��ȡ����ͷ��ǩ���� Map
     *
     * @param request   ����
     * @param signature ǩ��ע��
     * @return signature params
     */
    private static SortedMap<String, String> getRequestHeaderMap(ApiSignature signature, HttpServletRequest request) {
        SortedMap<String, String> sortedMap = new TreeMap<>();
        sortedMap.put(signature.appId(), request.getHeader(signature.appId()));
        sortedMap.put(signature.timestamp(), request.getHeader(signature.timestamp()));
        sortedMap.put(signature.nonce(), request.getHeader(signature.nonce()));
        return sortedMap;
    }

    /**
     * ��ȡ������� Map
     *
     * @param request ����
     * @return queryParams
     */
    private static SortedMap<String, String> getRequestParameterMap(HttpServletRequest request) {
        SortedMap<String, String> sortedMap = new TreeMap<>();
        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            sortedMap.put(entry.getKey(), entry.getValue()[0]);
        }
        return sortedMap;
    }

}
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\signature\core\redis\ApiSignatureRedisDAO.java
package cn.zszj.framework.signature.core.redis;

import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * HTTP API ǩ�� Redis DAO
 *
 * @author Zhougang
 */
@AllArgsConstructor
public class ApiSignatureRedisDAO {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * ��ǩ�����
     * <p>
     * KEY ��ʽ��signature_nonce:%s // ����Ϊ �����
     * VALUE ��ʽ��String
     * ����ʱ�䣺���̶�
     */
    private static final String SIGNATURE_NONCE = "api_signature_nonce:%s:%s";

    /**
     * ǩ����Կ
     * <p>
     * HASH �ṹ
     * KEY ��ʽ��%s // ����Ϊ appid
     * VALUE ��ʽ��String
     * ����ʱ�䣺�������ڣ�Ԥ���ص� Redis��
     */
    private static final String SIGNATURE_APPID = "api_signature_app";

    // ========== ��ǩ����� ==========

    public String getNonce(String appId, String nonce) {
        return stringRedisTemplate.opsForValue().get(formatNonceKey(appId, nonce));
    }

    public Boolean setNonce(String appId, String nonce, int time, TimeUnit timeUnit) {
        return stringRedisTemplate.opsForValue().setIfAbsent(formatNonceKey(appId, nonce), "", time, timeUnit);
    }

    private static String formatNonceKey(String appId, String nonce) {
        return String.format(SIGNATURE_NONCE, appId, nonce);
    }

    // ========== ǩ����Կ ==========

    public String getAppSecret(String appId) {
        return (String) stringRedisTemplate.opsForHash().get(SIGNATURE_APPID, appId);
    }

}
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\resources\MET
A-INF\spring\org.springframework.boot.autoconfigure.AutoConfiguration.imports
cn.zszj.framework.idempotent.config.ZszjIdempotentConfiguration
cn.zszj.framework.lock4j.config.ZszjLock4jConfiguration
cn.zszj.framework.ratelimiter.config.ZszjRateLimiterConfiguration
cn.zszj.framework.signature.config.ZszjApiSignatureAutoConfiguration
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\test\java\cn\zszj\
framework\idempotent\core\aop\IdempotentAspectTest.java
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
        // codex r0 P1 �ع飺@Idempotent �������� servlet ������ʩ��Σ��� HttpServletResponse����
        // �������ժҪʱ������ Jackson ���л����������л������ getWriter()/getOutputStream() �� getter��
        // ��ǰ��ѡ������Ӧ���ģʽ���ƻ������������������ ServletUtils.writeAttachment ���������� IllegalStateException����
        // ���״η������󼴴�����SEC-011.A �� sanitizeArgs �ӡ����ܾ���֧������Ϊ��ÿ�������ժҪ������
        // �޸���ժҪ��ܾ���־��������ų� servlet/spring-web ������ StrUtils.joinMethodArgs �����ų��ھ�һ�£���
        Idempotent idempotent = mock(Idempotent.class);
        doReturn(FixedKeyResolver.class).when(idempotent).keyResolver();
        when(idempotent.timeout()).thenReturn(5);
        when(idempotent.timeUnit()).thenReturn(TimeUnit.SECONDS);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenReturn("OK");
        HttpServletResponse response = mock(HttpServletResponse.class);
        Object businessArg = "orderPayload";
        // ��κ� servlet ��Ӧ���� + ҵ�����
        when(joinPoint.getArgs()).thenReturn(new Object[]{businessArg, response});
        when(idempotentRedisDAO.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);

        Object result = idempotentAspect.aroundPointCut(joinPoint, idempotent);
        assertEquals("OK", result);

        // ժҪӦ���ɡ��ų� servlet ����󡹵�ҵ����μ��㡪���� servlet �Ŀھ���˲�ͬ��RED�����ų���һ�£�GREEN��
        String expectedDigestExcludingServlet = SecureUtil.md5(LogSanitizeUtils.sanitizeArgs(new Object[]{businessArg})
);
        verify(idempotentRedisDAO).setIfAbsent(anyString(), eq(expectedDigestExcludingServlet), eq(5L), eq(TimeUnit.SEC
ONDS));
        // ���������������л����� servlet ��Ӧ�� writer / �������������Ӧ����ǰѡ�� writer���ƻ����������ȶ����������
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

}
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\test\java\cn\zszj\
framework\idempotent\core\keyresolver\impl\DefaultIdempotentKeyResolverTest.java
package cn.zszj.framework.idempotent.core.keyresolver.impl;

import cn.hutool.crypto.SecureUtil;
import cn.zszj.framework.common.util.servlet.ServletUtils;
import cn.zszj.framework.common.util.string.StrUtils;
import cn.zszj.framework.idempotent.core.annotation.Idempotent;
import cn.zszj.framework.web.core.util.WebFrameworkUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import jakarta.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * {@link DefaultIdempotentKeyResolver} ��Ԫ���ԡ�
 *
 * ZS-SEC-011.A����֤�ݵȼ������⻧ + �û�����������
 * ��ͬ����/�⻧���ù����ݵȼ�����������ͬ��������ʽ��ס null ������������Σ�����塣
 */
class DefaultIdempotentKeyResolverTest {

    private static final String METHOD_DESC = "OrderController.createOrder(..)";

    @Test
    void resolver_differentTenant_shouldProduceDifferentKey() {
        JoinPoint jp = mockJoinPoint(METHOD_DESC, new Object[]{"arg1"});
        HttpServletRequest req1 = mock(HttpServletRequest.class);
        HttpServletRequest req2 = mock(HttpServletRequest.class);

        try (MockedStatic<ServletUtils> servletMs = mockStatic(ServletUtils.class);
             MockedStatic<WebFrameworkUtils> webMs = mockStatic(WebFrameworkUtils.class)) {

            // ��һ�ε��ã��⻧1���û�100
            servletMs.when(ServletUtils::getRequest).thenReturn(req1);
            webMs.when(() -> WebFrameworkUtils.getTenantId(req1)).thenReturn(1L);
            webMs.when(() -> WebFrameworkUtils.getLoginUserId(req1)).thenReturn(100L);
            webMs.when(() -> WebFrameworkUtils.getLoginUserType(req1)).thenReturn(2);
            String k1 = new DefaultIdempotentKeyResolver().resolver(jp, mockIdempotent());

            // �ڶ��ε��ã��⻧2���û�100��ͬ�û���ͬ�⻧��
            servletMs.when(ServletUtils::getRequest).thenReturn(req2);
            webMs.when(() -> WebFrameworkUtils.getTenantId(req2)).thenReturn(2L);
            webMs.when(() -> WebFrameworkUtils.getLoginUserId(req2)).thenReturn(100L);
            webMs.when(() -> WebFrameworkUtils.getLoginUserType(req2)).thenReturn(2);
            String k2 = new DefaultIdempotentKeyResolver().resolver(jp, mockIdempotent());

            // C7��MIN-8����ɾ��Ч assertNotNull��MD5 ���� null����assertNotEquals ���㹻
            assertNotEquals(k1, k2, "��ͬ�⻧���ù����ݵȼ���������룩");
        }
    }

    @Test
    void resolver_differentUser_shouldProduceDifferentKey() {
        JoinPoint jp = mockJoinPoint(METHOD_DESC, new Object[]{"arg1"});
        HttpServletRequest req1 = mock(HttpServletRequest.class);
        HttpServletRequest req2 = mock(HttpServletRequest.class);

        try (MockedStatic<ServletUtils> servletMs = mockStatic(ServletUtils.class);
             MockedStatic<WebFrameworkUtils> webMs = mockStatic(WebFrameworkUtils.class)) {

            // ��һ�ε��ã��⻧1���û�100
            servletMs.when(ServletUtils::getRequest).thenReturn(req1);
            webMs.when(() -> WebFrameworkUtils.getTenantId(req1)).thenReturn(1L);
            webMs.when(() -> WebFrameworkUtils.getLoginUserId(req1)).thenReturn(100L);
            webMs.when(() -> WebFrameworkUtils.getLoginUserType(req1)).thenReturn(2);
            String k1 = new DefaultIdempotentKeyResolver().resolver(jp, mockIdempotent());

            // �ڶ��ε��ã��⻧1���û�200��ͬ�⻧��ͬ�û���
            servletMs.when(ServletUtils::getRequest).thenReturn(req2);
            webMs.when(() -> WebFrameworkUtils.getTenantId(req2)).thenReturn(1L);
            webMs.when(() -> WebFrameworkUtils.getLoginUserId(req2)).thenReturn(200L);
            webMs.when(() -> WebFrameworkUtils.getLoginUserType(req2)).thenReturn(2);
            String k2 = new DefaultIdempotentKeyResolver().resolver(jp, mockIdempotent());

            // C7��MIN-8����ɾ��Ч assertNotNull��MD5 ���� null����assertNotEquals ���㹻
            assertNotEquals(k1, k2, "��ͬ�û����ù����ݵȼ���������룩");
        }
    }

    @Test
    void resolver_differentUserType_shouldProduceDifferentKey() {
        // C7��MIN-8������ userType ��������������Ҫ�ش�ǰֻ���� tenant/user��ȱ userType
        JoinPoint jp = mockJoinPoint(METHOD_DESC, new Object[]{"arg1"});
        HttpServletRequest req1 = mock(HttpServletRequest.class);
        HttpServletRequest req2 = mock(HttpServletRequest.class);

        try (MockedStatic<ServletUtils> servletMs = mockStatic(ServletUtils.class);
             MockedStatic<WebFrameworkUtils> webMs = mockStatic(WebFrameworkUtils.class)) {

            // ��һ�ε��ã��⻧1���û�100��userType=1�������Ա��
            servletMs.when(ServletUtils::getRequest).thenReturn(req1);
            webMs.when(() -> WebFrameworkUtils.getTenantId(req1)).thenReturn(1L);
            webMs.when(() -> WebFrameworkUtils.getLoginUserId(req1)).thenReturn(100L);
            webMs.when(() -> WebFrameworkUtils.getLoginUserType(req1)).thenReturn(1);
            String k1 = new DefaultIdempotentKeyResolver().resolver(jp, mockIdempotent());

            // �ڶ��ε��ã�ͬ�⻧��ͬ userId���� userType=2�����Ա��
            servletMs.when(ServletUtils::getRequest).thenReturn(req2);
            webMs.when(() -> WebFrameworkUtils.getTenantId(req2)).thenReturn(1L);
            webMs.when(() -> WebFrameworkUtils.getLoginUserId(req2)).thenReturn(100L);
            webMs.when(() -> WebFrameworkUtils.getLoginUserType(req2)).thenReturn(2);
            String k2 = new DefaultIdempotentKeyResolver().resolver(jp, mockIdempotent());

            assertNotEquals(k1, k2, "ͬ tenant+userId ��ͬ userType ���ù����ݵȼ���������룩");
        }
    }

    @Test
    void resolver_differentArgs_shouldProduceDifferentKey() {
        // C7��MIN-8��������ͬ��� �� ��ͬ Key ��������������ͬ��ɲ��֣�Key �Ѻ��� argsStr��
        HttpServletRequest req = mock(HttpServletRequest.class);
        JoinPoint jp1 = mockJoinPoint(METHOD_DESC, new Object[]{"arg1"});
        JoinPoint jp2 = mockJoinPoint(METHOD_DESC, new Object[]{"arg2"});

        try (MockedStatic<ServletUtils> servletMs = mockStatic(ServletUtils.class);
             MockedStatic<WebFrameworkUtils> webMs = mockStatic(WebFrameworkUtils.class)) {

            servletMs.when(ServletUtils::getRequest).thenReturn(req);
            webMs.when(() -> WebFrameworkUtils.getTenantId(req)).thenReturn(1L);
            webMs.when(() -> WebFrameworkUtils.getLoginUserId(req)).thenReturn(100L);
            webMs.when(() -> WebFrameworkUtils.getLoginUserType(req)).thenReturn(2);
            String k1 = new DefaultIdempotentKeyResolver().resolver(jp1, mockIdempotent());
            String k2 = new DefaultIdempotentKeyResolver().resolver(jp2, mockIdempotent());

            assertNotEquals(k1, k2, "ͬ���岻ͬ��β��ù����ݵȼ���Key �Ѻ��� argsStr��");
        }
    }

    @Test
    void resolver_sameSubject_shouldProduceSameKey() {
        JoinPoint jp = mockJoinPoint(METHOD_DESC, new Object[]{"arg1"});
        HttpServletRequest req1 = mock(HttpServletRequest.class);
        HttpServletRequest req2 = mock(HttpServletRequest.class);

        try (MockedStatic<ServletUtils> servletMs = mockStatic(ServletUtils.class);
             MockedStatic<WebFrameworkUtils> webMs = mockStatic(WebFrameworkUtils.class)) {

            // ���ε��ã�ͬ�⻧��ͬ�û���ͬ���� -> Ӧ������ͬ Key
            servletMs.when(ServletUtils::getRequest).thenReturn(req1);
            webMs.when(() -> WebFrameworkUtils.getTenantId(req1)).thenReturn(1L);
            webMs.when(() -> WebFrameworkUtils.getLoginUserId(req1)).thenReturn(100L);
            webMs.when(() -> WebFrameworkUtils.getLoginUserType(req1)).thenReturn(2);
            String k1 = new DefaultIdempotentKeyResolver().resolver(jp, mockIdempotent());

            servletMs.when(ServletUtils::getRequest).thenReturn(req2);
            webMs.when(() -> WebFrameworkUtils.getTenantId(req2)).thenReturn(1L);
            webMs.when(() -> WebFrameworkUtils.getLoginUserId(req2)).thenReturn(100L);
            webMs.when(() -> WebFrameworkUtils.getLoginUserType(req2)).thenReturn(2);
            String k2 = new DefaultIdempotentKeyResolver().resolver(jp, mockIdempotent());

            assertEquals(k1, k2, "ͬ����ͬ����Ӧ������ͬ�ݵȼ�");
        }
    }

    @Test
    void resolver_noRequestContext_shouldNotCrash() {
        JoinPoint jp = mockJoinPoint(METHOD_DESC, new Object[]{"arg1"});

        try (MockedStatic<ServletUtils> servletMs = mockStatic(ServletUtils.class)) {
            servletMs.when(ServletUtils::getRequest).thenReturn(null);
            String key = new DefaultIdempotentKeyResolver().resolver(jp, mockIdempotent());
            // C6��IMP-7��������ȷ�� Key ��ʽ��tenant/user/type �� null ռλ����
            // ���ǽ� assertNotNull��MD5 ��� null���Ը�ʽ��Լ��������Ч���ԣ�
            String argsStr = StrUtils.joinMethodArgs(jp);
            String expected = SecureUtil.md5(METHOD_DESC + ":null:null:null:" + argsStr);
            assertEquals(expected, key, "�� request ������Ӧ�� null ռλƴ�ӡ��ھ�ȷ���� Key");
        }
    }

    @Test
    void resolver_nullScope_differentAnonymousSubjects_collapseToSameKey() {
        // C6��IMP-7 Σ��������ʽ��Լ������ request ������ʱ tenant/user/type �� null��
        // ��������ͬ�������塹��ͬһ method/args������������ �� �������Ӳ������֣�ȫ����Ϊ null���� Key ��ͬ��
        // ��"null ���������� �� ��ͬ�������干�������ռ�"��Ϊ��Լ�������Ķ������̺�ơ�
        // ���ձ߽磺�����޽���طţ�Σ�����ڿ����ԣ�SEC-011.B ����־û� + �طź�
        // �˴�����Ϊ���������طŷ��գ����ȶ£��� clientIP ��ǿ�������ģ���
        JoinPoint jp1 = mockJoinPoint(METHOD_DESC, new Object[]{"arg1"});
        JoinPoint jp2 = mockJoinPoint(METHOD_DESC, new Object[]{"arg1"});

        try (MockedStatic<ServletUtils> servletMs = mockStatic(ServletUtils.class)) {
            servletMs.when(ServletUtils::getRequest).thenReturn(null);
            String k1 = new DefaultIdempotentKeyResolver().resolver(jp1, mockIdempotent());
            String k2 = new DefaultIdempotentKeyResolver().resolver(jp2, mockIdempotent());
            assertEquals(k1, k2,
                    "null ��������������ͬ�������壨ͬ method/args�����������ģ�����ͬһ�ݵȼ������ռ�");
        }
    }

    // ========== Helper methods ==========

    private JoinPoint mockJoinPoint(String signatureText, Object[] args) {
        JoinPoint jp = mock(JoinPoint.class);
        when(jp.getSignature()).thenReturn(new FixedSignature(signatureText));
        when(jp.getArgs()).thenReturn(args);
        return jp;
    }

    private Idempotent mockIdempotent() {
        // C7��MIN-8����ɾ�� stub when(idem.keyArg())����Default �������Ӳ��� keyArg
        return mock(Idempotent.class);
    }

    /** �����ù̶�����ǩ����Mockito �޷� stub toString��������ʵʵ�֣� */
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
}
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\test\java\cn\zszj\
framework\ratelimiter\core\aop\RateLimiterAspectTest.java
package cn.zszj.framework.ratelimiter.core.aop;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.zszj.framework.ratelimiter.core.annotation.RateLimiter;
import cn.zszj.framework.ratelimiter.core.keyresolver.RateLimiterKeyResolver;
import cn.zszj.framework.ratelimiter.core.redis.RateLimiterRedisDAO;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RedissonClient;
import org.slf4j.LoggerFactory;

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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link RateLimiterAspect} �ĵ�Ԫ���ԡ�
 *
 * ���� SEC-007���������ܾ�ʱ���������������Ⱦ� {@code LogSanitizeUtils.sanitizeArgs} ������д��־��
 * ����ֵ��password/token/Ƕ�� apiKey/������ secret�����ó�����Ӧ����־�У�
 * ͬʱ�����ɶ�λ�ķ���������������ֶΣ����׳�Я����ȷ������� ServiceException��
 *
 * ���� SEC-010��Redis ����ʱ�������� fail-open������ + �澯�������������򻺴涶������ȫվ��¼��
 */
@ExtendWith(MockitoExtension.class)
public class RateLimiterAspectTest {

    private static final String SECRET_PASSWORD = "P@ssw0rd-SECRET-DoNotLog";
    private static final String SECRET_TOKEN = "TOKEN-SECRET-DoNotLog";
    private static final String SECRET_APIKEY = "APIKEY-SECRET-DoNotLog";
    private static final String SECRET_LIST = "LIST-SECRET-DoNotLog";
    private static final String SECRET_CODE = "888888-CODE-SECRET-DoNotLog";
    private static final String SAFE_USERNAME = "zhangsan-user";
    private static final String SAFE_NOTE = "hello-note";
    private static final String METHOD_DESC = "UserService.submitOrder(..)";

    @Mock
    private RateLimiterRedisDAO rateLimiterRedisDAO;

    private RateLimiterAspect rateLimiterAspect;
    private ListAppender<ILoggingEvent> listAppender;
    private Logger aspectLogger;

    @BeforeEach
    public void setUp() {
        rateLimiterAspect = new RateLimiterAspect(List.of(new FixedKeyResolver()), rateLimiterRedisDAO);
        aspectLogger = (Logger) LoggerFactory.getLogger(RateLimiterAspect.class);
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
    public void testBeforePointCut_tooManyRequests_argsSanitizedInLog() {
        // ׼������ע�⣺���ܾ�·�����õ�������
        RateLimiter rateLimiter = mock(RateLimiter.class);
        doReturn(FixedKeyResolver.class).when(rateLimiter).keyResolver();
        when(rateLimiter.count()).thenReturn(100);
        when(rateLimiter.time()).thenReturn(1);
        when(rateLimiter.timeUnit()).thenReturn(TimeUnit.SECONDS);
        when(rateLimiter.message()).thenReturn("");
        // ׼�� joinPoint���ɶ�λ�ķ������� + �����ܵĲ���
        JoinPoint joinPoint = mock(JoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(new FixedSignature(METHOD_DESC));
        when(joinPoint.getArgs()).thenReturn(new Object[]{buildSensitiveArgs()});
        // ��ȡ����ʧ�� -> �����������Ƶ���ܾ�
        when(rateLimiterRedisDAO.tryAcquire(anyString(), anyInt(), anyInt(), any())).thenReturn(false);

        // ���ã������׳� ServiceException �Ҵ����뱣��
        ServiceException ex = assertThrows(ServiceException.class,
                () -> rateLimiterAspect.beforePointCut(joinPoint, rateLimiter));
        assertEquals(GlobalErrorCodeConstants.TOO_MANY_REQUESTS.getCode(), ex.getCode());

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

    /**
     * SEC-010��refresh-token �˵�����Ǳ��� String refreshToken��
     * �� {@code sanitizeArgs} ֻ��������ֶΡ��Ź����������������ɸ��õ�ˢ�����������������ܾ���־��
     * �޸���衸��������֪����refreshToken ��һ�� token �������Զ�����ñ���ƾ�ݡ�
     */
    @Test
    public void testBeforePointCut_tooManyRequests_scalarRefreshTokenMasked() {
        RateLimiter rateLimiter = mock(RateLimiter.class);
        doReturn(FixedKeyResolver.class).when(rateLimiter).keyResolver();
        when(rateLimiter.count()).thenReturn(5);
        when(rateLimiter.time()).thenReturn(60);
        when(rateLimiter.timeUnit()).thenReturn(TimeUnit.SECONDS);
        when(rateLimiter.message()).thenReturn("");
        // joinPoint��MethodSignature �ṩ������ refreshToken�����Ϊ�������ƣ����� controller ��ʵ�����̬��
        JoinPoint joinPoint = mock(JoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        lenient().when(signature.getParameterNames()).thenReturn(new String[]{"refreshToken"});
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{SECRET_TOKEN});
        when(rateLimiterRedisDAO.tryAcquire(anyString(), anyInt(), anyInt(), any())).thenReturn(false);

        assertThrows(ServiceException.class,
                () -> rateLimiterAspect.beforePointCut(joinPoint, rateLimiter));

        String logText = capturedLog();
        assertFalse(logText.contains(SECRET_TOKEN), "���� refreshToken ƾ�ݲ�Ӧ�����������ܾ���־");
        assertTrue(logText.contains("***"), "����ƾ��Ӧ����������֪������");
    }

    /**
     * SEC-010��sms-login / reset-password �˵�� code��������֤�룩��������ƾ�ݸ�����
     * �� {@code sanitizeArgs} δ���ն˵㼶 extraKeys��������֤�������������ܾ���־��
     * �޸��� {@code @RateLimiter(maskKeys = {"code"})} �˵㼶��ȷ���루���� controller ��ʵ�����̬����
     */
    @Test
    public void testBeforePointCut_tooManyRequests_smsCodeMaskedViaMaskKeys() {
        RateLimiter rateLimiter = mock(RateLimiter.class);
        doReturn(FixedKeyResolver.class).when(rateLimiter).keyResolver();
        when(rateLimiter.count()).thenReturn(5);
        when(rateLimiter.time()).thenReturn(60);
        when(rateLimiter.timeUnit()).thenReturn(TimeUnit.SECONDS);
        when(rateLimiter.message()).thenReturn("");
        lenient().when(rateLimiter.maskKeys()).thenReturn(new String[]{"code"});
        // joinPoint��MethodSignature ������ reqVO�����Ϊ�� code �Ķ���������
        JoinPoint joinPoint = mock(JoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        lenient().when(signature.getParameterNames()).thenReturn(new String[]{"reqVO"});
        when(joinPoint.getSignature()).thenReturn(signature);
        Map<String, Object> smsReq = new LinkedHashMap<>();
        smsReq.put("mobile", "13800138000");
        smsReq.put("code", SECRET_CODE);
        when(joinPoint.getArgs()).thenReturn(new Object[]{smsReq});
        when(rateLimiterRedisDAO.tryAcquire(anyString(), anyInt(), anyInt(), any())).thenReturn(false);

        assertThrows(ServiceException.class,
                () -> rateLimiterAspect.beforePointCut(joinPoint, rateLimiter));

        String logText = capturedLog();
        assertFalse(logText.contains(SECRET_CODE), "������֤�� code ��Ӧ�����������ܾ���־");
        assertTrue(logText.contains("***"), "�˵㼶 maskKeys Ӧ�� code ����");
    }

    @Test
    public void testBeforePointCut_allowed_noRejectLog() {
        // δ�����������������У��������ܾ���־
        RateLimiter rateLimiter = mock(RateLimiter.class);
        doReturn(FixedKeyResolver.class).when(rateLimiter).keyResolver();
        JoinPoint joinPoint = mock(JoinPoint.class);
        when(rateLimiterRedisDAO.tryAcquire(anyString(), anyInt(), anyInt(), any())).thenReturn(true);

        rateLimiterAspect.beforePointCut(joinPoint, rateLimiter);

        assertTrue(listAppender.list.isEmpty(), "��������Ӧ�����ܾ���־");
    }

    /**
     * SEC-010��Redis ����ʱ�� fail-open ���������������󣨲��� TOO_MANY_REQUESTS��������¼�澯��־��
     *
     * <p>fail-open ����ʵ������ʵ�� {@link RateLimiterRedisDAO#tryAcquire} �ڣ����� Redisson �쳣���� TRUE����
     * �ʴ˴��á���ʵ DAO + ���쳣�� RedissonClient���ᴩ������·��֤������ mock DAO��mock ���ƹ���ʵ catch����
     */
    @Test
    public void testBeforePointCut_redisDown_shouldFailOpenByDefault() {
        // ��ʵ DAO ����һ�����ʼ����쳣�� RedissonClient��ģ�� Redis ���ӹ���
        RedissonClient redissonClient = mock(RedissonClient.class);
        when(redissonClient.getRateLimiter(anyString())).thenThrow(new RuntimeException("Redis connection refused"));
        RateLimiterRedisDAO realDao = new RateLimiterRedisDAO(redissonClient);
        RateLimiterAspect aspectWithRealDao = new RateLimiterAspect(List.of(new FixedKeyResolver()), realDao);

        // ���� DAO ��־����֤ fail-open �澯��λ
        Logger daoLogger = (Logger) LoggerFactory.getLogger(RateLimiterRedisDAO.class);
        Level originalLevel = daoLogger.getLevel();
        daoLogger.setLevel(Level.ERROR);
        ListAppender<ILoggingEvent> daoAppender = new ListAppender<>();
        daoAppender.start();
        daoLogger.addAppender(daoAppender);
        try {
            RateLimiter rateLimiter = mock(RateLimiter.class);
            doReturn(FixedKeyResolver.class).when(rateLimiter).keyResolver();
            when(rateLimiter.count()).thenReturn(2);
            when(rateLimiter.time()).thenReturn(60);
            when(rateLimiter.timeUnit()).thenReturn(TimeUnit.SECONDS);
            JoinPoint joinPoint = mock(JoinPoint.class);

            // fail-open��Redis ���ϲ����� TOO_MANY_REQUESTS��Ӧ����
            assertDoesNotThrow(() -> aspectWithRealDao.beforePointCut(joinPoint, rateLimiter),
                    "Redis ����Ӧ fail-open ���У����Ǿܾ����쳣����");

            // �澯��λ��DAO ��¼ error ����־����ʽ���� fail-open
            String daoLog = daoAppender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .collect(Collectors.joining("\n"));
            assertTrue(daoLog.contains("fail-open"), "Redis ����Ӧ��¼ fail-open �澯�Ա���ά��֪");
        } finally {
            daoLogger.detachAppender(daoAppender);
            daoAppender.stop();
            daoLogger.setLevel(originalLevel);
        }
    }

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
    static class FixedKeyResolver implements RateLimiterKeyResolver {
        @Override
        public String resolver(JoinPoint joinPoint, RateLimiter rateLimiter) {
            return "rate-fixed-key";
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

}
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\test\java\cn\zszj\
framework\ratelimiter\core\keyresolver\impl\ExpressionRateLimiterKeyResolverTest.java
package cn.zszj.framework.ratelimiter.core.keyresolver.impl;

import cn.zszj.framework.common.util.servlet.ServletUtils;
import cn.zszj.framework.ratelimiter.core.annotation.RateLimiter;
import cn.zszj.framework.web.core.util.WebFrameworkUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * {@link ExpressionRateLimiterKeyResolver} �Ĵ���Ԫ���ԣ�ZS-SEC-010����
 *
 * <p>��֤��ǿ��� Key �߱�������� + ���������� + �⻧���������ظ��룺
 * <ul>
 *     <li>��ͬ�⻧�µ�ͬһ�������õ���ͬ Key�����⻧�����ö�ȣ���</li>
 *     <li>��ͬ�˵㣨�������µ�ͬһ�������õ���ͬ Key����˵㲻���ö�ȣ���</li>
 *     <li>ͬһ���塢���Ķ�������ͨ����������֤�룩ʱ Key ���䣨�̶������������ɱ���ܣ���</li>
 *     <li>����������ȱʧ��request Ϊ null��ʱ��������tenantId �� null ռλ��</li>
 * </ul>
 *
 * <p>�⻧��Դ���� {@code TenantContextHolder} ͬԴ������ {@code tenant-id} ����ͷ��ȡ��
 * ������������ protection �������� biz-tenant���ֲ㵹�ã���
 */
public class ExpressionRateLimiterKeyResolverTest {

    private static final String MOBILE = "13800138000";

    private final ExpressionRateLimiterKeyResolver resolver = new ExpressionRateLimiterKeyResolver();

    // ========== �������� ==========

    @Test
    public void resolver_shouldScopeByMethodAndTenant() {
        try (MockedStatic<ServletUtils> su = mockStatic(ServletUtils.class)) {
            // 1) �⻧���룺ͬһ�ֻ��š�ͬһ�˵㣬��ͬ�⻧ -> ��ͬ Key
            JoinPoint jp = mockJoinPoint("send", MOBILE, "1234");
            RateLimiter rl = mockRateLimiter("#mobile");

            su.when(ServletUtils::getRequest).thenReturn(requestWithTenant("1"));
            String keyTenant1 = resolver.resolver(jp, rl);

            su.when(ServletUtils::getRequest).thenReturn(requestWithTenant("2"));
            String keyTenant2 = resolver.resolver(jp, rl);

            assertNotEquals(keyTenant1, keyTenant2, "��ͬ�⻧���ù����������");
        }

        try (MockedStatic<ServletUtils> su = mockStatic(ServletUtils.class)) {
            // 2) �������룺ͬһ�ֻ��š�ͬһ�⻧����ͬ�˵� -> ��ͬ Key
            su.when(ServletUtils::getRequest).thenReturn(requestWithTenant("1"));
            String keySend = resolver.resolver(mockJoinPoint("send", MOBILE, "1234"), mockRateLimiter("#mobile"));
            String keyOther = resolver.resolver(mockJoinPoint("other", MOBILE, "1234"), mockRateLimiter("#mobile"));

            assertNotEquals(keySend, keyOther, "��ͬ�˵�(����)���ù����������");
        }
    }

    @Test
    public void resolver_sameSubjectDifferentArg_shouldNotChangeKey() {
        try (MockedStatic<ServletUtils> su = mockStatic(ServletUtils.class)) {
            su.when(ServletUtils::getRequest).thenReturn(requestWithTenant("1"));

            // ͬһ�ֻ��ţ����Ķ���֤�루��ͨ������-> Key ���䣬�޷���̶ܹ���������
            String key1 = resolver.resolver(mockJoinPoint("send", MOBILE, "1111"), mockRateLimiter("#mobile"));
            String key2 = resolver.resolver(mockJoinPoint("send", MOBILE, "2222"), mockRateLimiter("#mobile"));

            assertEquals(key1, key2, "����֤�����ͨ�������ø��� Key���̶������������ɹ�ܣ�");
            // Key ��������+�⻧�������ϣ������������ԭֵ������˵�/���⻧���ã�
            assertNotEquals(MOBILE, key1, "Key Ӧ������+�⻧������(MD5)����������ԭֵ");
        }
    }

    @Test
    public void resolver_whenNoRequestContext_shouldNotCrash() {
        try (MockedStatic<ServletUtils> su = mockStatic(ServletUtils.class)) {
            // �� web �����ģ�request Ϊ null����tenantId �� null ռλ����Ӧ�ȶ����� Key�������쳣
            su.when(ServletUtils::getRequest).thenReturn(null);

            String key = assertDoesNotThrow(() ->
                    resolver.resolver(mockJoinPoint("send", MOBILE, "1111"), mockRateLimiter("#mobile")));

            assertNotNull(key, "������������ʱ��Ӧ�����ǿ� Key");
        }
    }

    // ========== ���Խ��ּ� ==========

    private JoinPoint mockJoinPoint(String methodName, Object... args) {
        Method method;
        try {
            method = Fixture.class.getMethod(methodName, String.class, String.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
        MethodSignature signature = new FixedMethodSignature(method);
        JoinPoint joinPoint = mock(JoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(args);
        return joinPoint;
    }

    private RateLimiter mockRateLimiter(String keyArg) {
        RateLimiter rateLimiter = mock(RateLimiter.class);
        when(rateLimiter.keyArg()).thenReturn(keyArg);
        return rateLimiter;
    }

    private MockHttpServletRequest requestWithTenant(String tenantId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (tenantId != null) {
            request.addHeader(WebFrameworkUtils.HEADER_TENANT_ID, tenantId);
        }
        return request;
    }

    /** ���ⷽ�����ڵĹ̶��о��࣬�ṩ��ʵ Method ������������ʹ�ã����� -parameters ���룩 */
    static class Fixture {
        public void send(String mobile, String code) {
        }

        public void other(String mobile, String code) {
        }
    }

    /**
     * ��ʵ�� {@link MethodSignature} ʵ�֣�Mockito �޷� stub {@code toString()}��
     * ���������� {@code getSignature().toString()} ��Ϊ�������������ӣ����践��ȷ���Եķ���������
     */
    @SuppressWarnings({"rawtypes"})
    static class FixedMethodSignature implements MethodSignature {
        private final Method method;

        FixedMethodSignature(Method method) {
            this.method = method;
        }

        @Override
        public Method getMethod() {
            return method;
        }

        @Override
        public Class getReturnType() {
            return method.getReturnType();
        }

        @Override
        public Class[] getParameterTypes() {
            return method.getParameterTypes();
        }

        @Override
        public Class[] getExceptionTypes() {
            return method.getExceptionTypes();
        }

        @Override
        public String[] getParameterNames() {
            java.lang.reflect.Parameter[] params = method.getParameters();
            String[] names = new String[params.length];
            for (int i = 0; i < params.length; i++) {
                names[i] = params[i].getName();
            }
            return names;
        }

        @Override
        public String toString() {
            return method.toString();
        }

        @Override
        public String toShortString() {
            return method.toString();
        }

        @Override
        public String toLongString() {
            return method.toString();
        }

        @Override
        public String getName() {
            return method.getName();
        }

        @Override
        public int getModifiers() {
            return method.getModifiers();
        }

        @Override
        public Class getDeclaringType() {
            return method.getDeclaringClass();
        }

        @Override
        public String getDeclaringTypeName() {
            return method.getDeclaringClass().getName();
        }
    }

}
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\test\java\cn\zszj\
framework\signature\core\ApiSignatureTest.java
package cn.zszj.framework.signature.core;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.zszj.framework.signature.core.annotation.ApiSignature;
import cn.zszj.framework.signature.core.aop.ApiSignatureAspect;
import cn.zszj.framework.signature.core.redis.ApiSignatureRedisDAO;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link ApiSignatureTest} �ĵ�Ԫ����
 */
@ExtendWith(MockitoExtension.class)
public class ApiSignatureTest {

    @InjectMocks
    private ApiSignatureAspect apiSignatureAspect;

    @Mock
    private ApiSignatureRedisDAO signatureRedisDAO;

    @Test
    public void testSignatureGet() throws IOException {
        // ��һ��ǩ��
        Long timestamp = System.currentTimeMillis();
        String nonce = IdUtil.randomUUID();
        String appId = "xxxxxx";
        String appSecret = "yyyyyy";
        String signString = "k1=v1&v1=k1testappId=xxxxxx&nonce=" + nonce + "&timestamp=" + timestamp + "yyyyyy";
        String sign = DigestUtil.sha256Hex(signString);

        // ׼������
        ApiSignature apiSignature = mock(ApiSignature.class);
        when(apiSignature.appId()).thenReturn("appId");
        when(apiSignature.timestamp()).thenReturn("timestamp");
        when(apiSignature.nonce()).thenReturn("nonce");
        when(apiSignature.sign()).thenReturn("sign");
        when(apiSignature.timeout()).thenReturn(60);
        when(apiSignature.timeUnit()).thenReturn(TimeUnit.SECONDS);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(eq("appId"))).thenReturn(appId);
        when(request.getHeader(eq("timestamp"))).thenReturn(String.valueOf(timestamp));
        when(request.getHeader(eq("nonce"))).thenReturn(nonce);
        when(request.getHeader(eq("sign"))).thenReturn(sign);
        when(request.getParameterMap()).thenReturn(MapUtil.<String, String[]>builder()
                .put("v1", new String[]{"k1"}).put("k1", new String[]{"v1"}).build());
        when(request.getContentType()).thenReturn("application/json");
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("test")));
        // mock ����
        when(signatureRedisDAO.getAppSecret(eq(appId))).thenReturn(appSecret);
        when(signatureRedisDAO.setNonce(eq(appId), eq(nonce), eq(120), eq(TimeUnit.SECONDS))).thenReturn(true);

        // ����
        boolean result = apiSignatureAspect.verifySignature(apiSignature, request);
        // ���Խ��
        assertTrue(result);
    }

}
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\test\java\cn\zszj\
framework\signature\core\aop\ApiSignatureAspectTest.java
package cn.zszj.framework.signature.core.aop;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.zszj.framework.signature.core.annotation.ApiSignature;
import cn.zszj.framework.signature.core.redis.ApiSignatureRedisDAO;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link ApiSignatureAspect} �ĵ�Ԫ���ԡ�
 *
 * ���� SEC-007��ǩ��У��ʧ�ܱ��ܾ�ʱ���������������Ⱦ� {@code LogSanitizeUtils.sanitizeArgs} ������д��־��
 * ����ֵ��password/token/Ƕ�� apiKey�����ó�����Ӧ����־�У�
 * ͬʱ�����ɶ�λ�ķ���������������ֶΣ����׳�Я����ȷ������� ServiceException��
 */
@ExtendWith(MockitoExtension.class)
public class ApiSignatureAspectTest {

    private static final String SECRET_PASSWORD = "P@ssw0rd-SECRET-DoNotLog";
    private static final String SECRET_TOKEN = "TOKEN-SECRET-DoNotLog";
    private static final String SECRET_APIKEY = "APIKEY-SECRET-DoNotLog";
    private static final String SAFE_USERNAME = "zhangsan-user";
    private static final String SAFE_NOTE = "hello-note";
    private static final String METHOD_DESC = "OpenApiController.queryOrder(..)";

    @Mock
    private ApiSignatureRedisDAO signatureRedisDAO;

    private ApiSignatureAspect apiSignatureAspect;
    private ListAppender<ILoggingEvent> listAppender;
    private Logger aspectLogger;

    @BeforeEach
    public void setUp() {
        apiSignatureAspect = new ApiSignatureAspect(signatureRedisDAO);
        aspectLogger = (Logger) LoggerFactory.getLogger(ApiSignatureAspect.class);
        aspectLogger.setLevel(Level.INFO);
        listAppender = new ListAppender<>();
        listAppender.start();
        aspectLogger.addAppender(listAppender);
    }

    @AfterEach
    public void tearDown() {
        aspectLogger.detachAppender(listAppender);
        listAppender.stop();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    public void testBeforePointCut_signatureFailed_argsSanitizedInLog() {
        // ׼�����������ģ�ȱʧ��ǩ Header -> verifyHeaders ֱ��ʧ�� -> verifySignature ���� false
        HttpServletRequest request = mock(HttpServletRequest.class);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        // ׼��ǩ��ע�⣺��ʧ��·�����õ�������
        ApiSignature signature = mock(ApiSignature.class);
        when(signature.appId()).thenReturn("appId");
        // ׼�� joinPoint���ɶ�λ�ķ������� + �����ܵĲ���
        JoinPoint joinPoint = mock(JoinPoint.class);
        when(joinPoint.getSignature()).thenReturn(new FixedSignature(METHOD_DESC));
        when(joinPoint.getArgs()).thenReturn(new Object[]{buildSensitiveArgs()});

        // ���ã������׳� ServiceException �Ҵ����뱣��
        ServiceException ex = assertThrows(ServiceException.class,
                () -> apiSignatureAspect.beforePointCut(joinPoint, signature));
        assertEquals(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), ex.getCode());

        // ������־������ֵ�����֣��������ֶ��뷽�����������������ֶα�����
        String logText = capturedLog();
        assertFalse(logText.contains(SECRET_PASSWORD), "password ���ܲ�Ӧ��������־");
        assertFalse(logText.contains(SECRET_TOKEN), "token ���ܲ�Ӧ��������־");
        assertFalse(logText.contains(SECRET_APIKEY), "Ƕ�� apiKey ���ܲ�Ӧ��������־");
        assertTrue(logText.contains(SAFE_USERNAME), "������ username Ӧ����");
        assertTrue(logText.contains(SAFE_NOTE), "������ note Ӧ����");
        assertTrue(logText.contains(METHOD_DESC), "��������Ӧ�����Ա㶨λ");
        assertTrue(logText.contains("***"), "�����ֶ�Ӧ������");
    }

    private static Map<String, Object> buildSensitiveArgs() {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("username", SAFE_USERNAME);
        req.put("password", SECRET_PASSWORD);
        req.put("token", SECRET_TOKEN);
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("apiKey", SECRET_APIKEY);
        nested.put("note", SAFE_NOTE);
        req.put("nested", nested);
        return req;
    }

    private String capturedLog() {
        return listAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.joining("\n"));
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

}
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\classes\cn\zszj
\framework\idempotent\package-info.class
????   =  )cn/zszj/framework/idempotent/package-info  java/lang/Object  package-info.java 
SourceFile               
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\classes\cn\zszj
\framework\idempotent\config\ZszjIdempotentConfiguration.class
????   = 4
      java/lang/Object <init> ()V  6cn/zszj/framework/idempotent/core/aop/IdempotentAspect
  
   O(Ljava/util/List;Lcn/zszj/framework/idempotent/core/redis/IdempotentRedisDAO;)V 
 :cn/zszj/framework/idempotent/core/redis/IdempotentRedisDAO
     <(Lorg/springframework/data/redis/core/StringRedisTemplate;)V  Ocn/zszj/framework/idempotent/core/keyreso
lver/impl/DefaultIdempotentKeyResolver
    Lcn/zszj/framework/idempotent/core/keyresolver/impl/UserIdempotentKeyResolver
    Rcn/zszj/framework/idempotent/core/keyresolver/impl/ExpressionIdempotentKeyResolver
    ?cn/zszj/framework/idempotent/config/ZszjIdempotentConfiguration Code LineNumberTable idempotentAspect 
?(Ljava/util/List;Lcn/zszj/framework/idempotent/core/redis/IdempotentRedisDAO;)Lcn/zszj/framework/idempotent/core/aop/I
dempotentAspect; MethodParameters keyResolvers idempotentRedisDAO 	Signature ?(Ljava/util/List<Lcn/zszj/framewo
rk/idempotent/core/keyresolver/IdempotentKeyResolver;>;Lcn/zszj/framework/idempotent/core/redis/IdempotentRedisDAO;)Lcn
/zszj/framework/idempotent/core/aop/IdempotentAspect; RuntimeVisibleAnnotations -Lorg/springframework/context/annota
tion/Bean; w(Lorg/springframework/data/redis/core/StringRedisTemplate;)Lcn/zszj/framework/idempotent/core/redis/Idempo
tentRedisDAO; stringRedisTemplate defaultIdempotentKeyResolver S()Lcn/zszj/framework/idempotent/core/keyresolver/i
mpl/DefaultIdempotentKeyResolver; userIdempotentKeyResolver P()Lcn/zszj/framework/idempotent/core/keyresolver/impl/U
serIdempotentKeyResolver; expressionIdempotentKeyResolver V()Lcn/zszj/framework/idempotent/core/keyresolver/impl/Exp
ressionIdempotentKeyResolver; 
SourceFile  ZszjIdempotentConfiguration.java :Lorg/springframework/boot/autoconfigure/AutoConfiguration; after ;Lc
n/zszj/framework/redis/config/ZszjRedisAutoConfiguration; !                    *? ?               
    "     
? Y+,? 	?                	 !   "   #    $ %     &    " '     !     	? Y+? ?                 (  
 %     &    ) *           ? Y? ?           ! %     &    + ,           ? Y? ?           & % 
    &    - .           ? Y? ?           + %     &    /    0 %     1  2[ c 3
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\classes\cn\zszj
\framework\idempotent\core\annotation\Idempotent.class
????   = %  7cn/zszj/framework/idempotent/core/annotation/Idempotent  java/lang/Object  java/lang/annotation
/Annotation timeout ()I AnnotationDefault    timeUnit !()Ljava/util/concurrent/TimeUnit; Ljava/util/concur
rent/TimeUnit; SECONDS message ()Ljava/lang/String; �ظ��������Ժ����� keyResolver ()Ljava/lang/Class; QLcn/
zszj/framework/idempotent/core/keyresolver/impl/DefaultIdempotentKeyResolver; 	Signature [()Ljava/lang/Class<+Lcn/zsz
j/framework/idempotent/core/keyresolver/IdempotentKeyResolver;>; keyArg   deleteKeyWhenException ()Z 
SourceFile Idempotent.java RuntimeVisibleAnnotations Ljava/lang/annotation/Target; value "Ljava/lang/annotatio
n/ElementType; METHOD  Ljava/lang/annotation/Retention; &Ljava/lang/annotation/RetentionPolicy; RUNTIME&    
       	   I 
    	   e 
     	   s     	   c          	   s     	   Z 
              [ e   ! "  e # $
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\classes\cn\zszj
\framework\idempotent\core\aop\IdempotentAspect.class
????   =
      java/lang/Object <init> ()V    	 
 apply ()Ljava/util/function/Function;
  
    8cn/zszj/framework/common/util/collection/CollectionUtils 
convertMap D(Ljava/util/Collection;Ljava/util/function/Function;)Ljava/util/Map;	      6cn/zszj/framework/idem
potent/core/aop/IdempotentAspect keyResolvers Ljava/util/Map;	     idempotentRedisDAO <Lcn/zszj/framework/i
dempotent/core/redis/IdempotentRedisDAO;       7cn/zszj/framework/idempotent/core/annotation/Idempotent keyRe
solver ()Ljava/lang/Class; " # $ % & 
java/util/Map get &(Ljava/lang/Object;)Ljava/lang/Object; ( Ccn/zszj/framework/idempotent/core/keyresolver/Idempot
entKeyResolver * (�Ҳ�����Ӧ�� IdempotentKeyResolver
 , - . / 0 org/springframework/util/Assert notNull '(Ljava/lang/Object;Ljava/lang/String;)V ' 2 3 4 resolver
 i(Lorg/aspectj/lang/JoinPoint;Lcn/zszj/framework/idempotent/core/annotation/Idempotent;)Ljava/lang/String; 6 7 8 9
 : $org/aspectj/lang/ProceedingJoinPoint getArgs ()[Ljava/lang/Object;
  < = > serializableArgs (([Ljava/lang/Object;)[Ljava/lang/Object; @ java/lang/String
 B C D E F 2cn/zszj/framework/common/util/log/LogSanitizeUtils sanitizeArgs :([Ljava/lang/Object;[Ljava/lang/Stri
ng;)Ljava/lang/String;
 H I J K L cn/hutool/crypto/SecureUtil md5 &(Ljava/lang/String;)Ljava/lang/String;	 N O P Q R java/lang/Bool
ean TRUE Ljava/lang/Boolean;  T U V timeout ()I  X Y Z timeUnit !()Ljava/util/concurrent/TimeUnit;
 \ ] ^ _ ` :cn/zszj/framework/idempotent/core/redis/IdempotentRedisDAO setIfAbsent Y(Ljava/lang/String;Ljava/lang
/String;JLjava/util/concurrent/TimeUnit;)Ljava/lang/Boolean;
 N b c d equals (Ljava/lang/Object;)Z
 \ f g L 	getDigest i java/lang/Exception	  k l m log Lorg/slf4j/Logger; o J[aroundPointCut][�ݵȼ�({}) ����
ժҪʧ�ܣ��������ظ�����] q r s t u org/slf4j/Logger warn 9(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V
 w x y z { cn/hutool/core/util/StrUtil 
isNotEmpty (Ljava/lang/CharSequence;)Z
 ? b ~ :[aroundPointCut][����({}) ����({}) ͬ����γ�ͻ] 6 ? ? ? getSignature ()Lorg/aspectj/lang/Signature; ? :[
aroundPointCut][����({}) ����({}) ͬ��ͬ���ظ�] q ? ? u info ? <�ݵȼ���ͻ����ͬ�ݵȼ�Я���˲�ͬ��������  ? ? ? message
 ()Ljava/lang/String; ? 3cn/zszj/framework/common/exception/ServiceException	 ? ? ? ? ? Acn/zszj/framework/common
/exception/enums/GlobalErrorCodeConstants REPEATED_REQUESTS .Lcn/zszj/framework/common/exception/ErrorCode;
 ? ? ? ? ? ,cn/zszj/framework/common/exception/ErrorCode getCode ()Ljava/lang/Integer;
 ? ?  ? ((Ljava/lang/Integer;Ljava/lang/String;)V 6 ? ? ? proceed ()Ljava/lang/Object; ? java/lang/Throwabl
e  ? ? ? deleteKeyWhenException ()Z
 \ ? ? ? delete (Ljava/lang/String;)V ? java/util/ArrayList
 ? ?  ? (I)V ? ? ? ? d java/util/List add
  ? ?   getClass
 ? ? ? ? ? java/lang/Class getName ? java/lang/CharSequence ? 
javax.servlet ? jakarta.servlet ? org.springframework.web
 w ? ? ? startWithAny 4(Ljava/lang/CharSequence;[Ljava/lang/CharSequence;)Z ? ? ? : toArray
 ? ? ? ? ? org/slf4j/LoggerFactory 	getLogger %(Ljava/lang/Class;)Lorg/slf4j/Logger; RuntimeInvisibleAnnotation
s Llombok/Generated; CONFLICT_MESSAGE Ljava/lang/String; 
ConstantValue 	Signature ?Ljava/util/Map<Ljava/lang/Class<+Lcn/zszj/framework/idempotent/core/keyresolver/IdempotentK
eyResolver;>;Lcn/zszj/framework/idempotent/core/keyresolver/IdempotentKeyResolver;>; O(Ljava/util/List;Lcn/zszj/framew
ork/idempotent/core/redis/IdempotentRedisDAO;)V Code LineNumberTable MethodParameters ?(Ljava/util/List<Lcn/zszj
/framework/idempotent/core/keyresolver/IdempotentKeyResolver;>;Lcn/zszj/framework/idempotent/core/redis/IdempotentRedis
DAO;)V aroundPointCut s(Lorg/aspectj/lang/ProceedingJoinPoint;Lcn/zszj/framework/idempotent/core/annotation/Idempote
nt;)Ljava/lang/Object; 
StackMapTable 
Exceptions 	joinPoint 
idempotent RuntimeVisibleAnnotations $Lorg/aspectj/lang/annotation/Around; value @annotation(idempotent) ? [L
java/lang/Object; args <clinit> 
SourceFile IdempotentAspect.java $Lorg/aspectj/lang/annotation/Aspect; BootstrapMethods ?
 ? ? ? ? ? "java/lang/invoke/LambdaMetafactory metafactory ?(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/St
ring;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodT
ype;)Ljava/lang/invoke/CallSite; & ? ? X(Lcn/zszj/framework/idempotent/core/keyresolver/IdempotentKeyResolver;)Lja
va/lang/Class; InnerClasses ? %java/lang/invoke/MethodHandles$Lookup java/lang/invoke/MethodHandles Lookup !
       l m  ?     ?    ? ?  ?    ?     ?    ?         ?  ?   ;     *? *+?   ? ? *,? ?   
 ?       =  >  ?  @ ?   	       ?    ?  ? ?  ?  ?  
  *? ,?  ? ! ? 'N-)? +-+,? 1 :+? 5 ? ;? ?? A? G:? M*? ,? S ?,? W ? [? a6? ?*? ? e:? :? jn
? p :? v? ? |? ? 6? &? j}+?  +? 5 ? ;? ?? A? p ? #? j?+?  +? 5 ? ;? ?? A? ? ? ?? 	,
? ? :	? ?Y? ?? ?	? ??+? ? ?:,? ? ? *? ? ??  ] h k h ?  ?   j    E  F  H # P 8 T X V ] Z h ^ k [ m
 \ { ] ~ a ? c ? d ? e ? d ? g ? h ? g ? j ? k p q
 t u w ?   ? ? k   6  ' ? ?  h?  ?@? )	E ?? F ??  ? ?     ? ?   	 ?   ?   ?     ?  ?s
 ? 
 = >  ?   ?     w*? *?? *?? ?Y*?? ?L*M,?>6? P,2:? +? ? W? 4? ?? ?:? ?Y?SY?SY?S? �� ? +
? ? W????+? ? ?    ?   6 
   ? 	 ?  ?  ? ) ? . ? 6 ? 9 ? C ? ^ ? a ? j ? p ? ?   ( 	?   ? ? ?  ?  ? ' ?? ?  ?    ?    ?   ?
   !      	? �� j?    ?       .  ?    ? ?     ?   ?     ?  ? ? ? ?   
  ?  
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\classes\cn\zszj
\framework\idempotent\core\keyresolver\IdempotentKeyResolver.class
????   =   Ccn/zszj/framework/idempotent/core/keyresolver/IdempotentKeyResolver  java/lang/Object resolver i
(Lorg/aspectj/lang/JoinPoint;Lcn/zszj/framework/idempotent/core/annotation/Idempotent;)Ljava/lang/String; MethodParam
eters 	joinPoint 
idempotent 
SourceFile IdempotentKeyResolver.java              	    	    
    
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\classes\cn\zszj
\framework\idempotent\core\keyresolver\impl\DefaultIdempotentKeyResolver.class
????   = [
      java/lang/Object <init> ()V  	 
   org/aspectj/lang/JoinPoint getSignature ()Lorg/aspectj/lang/Signature;      org/aspectj/lang/Sign
ature toString ()Ljava/lang/String;
      -cn/zszj/framework/common/util/string/StrUtils joinMethodArgs 0(Lorg/aspectj/lang/JoinPoint;)Ljava/lan
g/String;
      2cn/zszj/framework/common/util/servlet/ServletUtils 
getRequest +()Ljakarta/servlet/http/HttpServletRequest;
   ! " # $ 1cn/zszj/framework/web/core/util/WebFrameworkUtils getTenantId ;(Ljakarta/servlet/http/HttpServletRequ
est;)Ljava/lang/Long;
   & ' $ getLoginUserId
   ) * + getLoginUserType >(Ljakarta/servlet/http/HttpServletRequest;)Ljava/lang/Integer;   - . / makeConcatWit
hConstants k(Ljava/lang/String;Ljava/lang/Long;Ljava/lang/Long;Ljava/lang/Integer;Ljava/lang/String;)Ljava/lang/String
;
 1 2 3 4 5 cn/hutool/crypto/SecureUtil md5 &(Ljava/lang/String;)Ljava/lang/String; 7 Ocn/zszj/framework/idempo
tent/core/keyresolver/impl/DefaultIdempotentKeyResolver 9 Ccn/zszj/framework/idempotent/core/keyresolver/IdempotentKe
yResolver Code LineNumberTable resolver i(Lorg/aspectj/lang/JoinPoint;Lcn/zszj/framework/idempotent/core/annotat
ion/Idempotent;)Ljava/lang/String; 
StackMapTable @ java/lang/String B 'jakarta/servlet/http/HttpServletRequest D java/lang/Long F java/lang/Int
eger MethodParameters 	joinPoint 
idempotent 
SourceFile !DefaultIdempotentKeyResolver.java BootstrapMethods N
 O P Q . R $java/lang/invoke/StringConcatFactory ?(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/l
ang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; T 	:::: InnerClasses 
W %java/lang/invoke/MethodHandles$Lookup Y java/lang/invoke/MethodHandles Lookup ! 6   8        :      
  *? ?    ;         < =  :   ?  	   Y+?  ? 
 N+? :? :? ? ? :? ? %? :? ? (? :-? ,  ? 0?    ;       #  $  &  ' ' ( 7 ) G 
* >   & ? $ ? ? A@ C?  C@ C?  C@ E G   	 H   I    J    K L     M  S U   
  V X Z 
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\classes\cn\zszj
\framework\idempotent\core\keyresolver\impl\ExpressionIdempotentKeyResolver.class
????   = ?
      java/lang/Object <init> ()V  7org/springframework/core/DefaultParameterNameDiscoverer
  	   
   Rcn/zszj/framework/idempotent/core/keyresolver/impl/ExpressionIdempotentKeyResolver parameterNameDiscoverer 2
Lorg/springframework/core/ParameterNameDiscoverer;  Aorg/springframework/expression/spel/standard/SpelExpressionPars
er
  	     expressionParser 1Lorg/springframework/expression/ExpressionParser;
     	getMethod 8(Lorg/aspectj/lang/JoinPoint;)Ljava/lang/reflect/Method;       org/aspectj/lang/JoinPo
int getArgs ()[Ljava/lang/Object; " # $ % & 0org/springframework/core/ParameterNameDiscoverer getParameterNam
es /(Ljava/lang/reflect/Method;)[Ljava/lang/String; ( Eorg/springframework/expression/spel/support/StandardEvaluatio
nContext
 ' 
 + , - . / cn/hutool/core/util/ArrayUtil 
isNotEmpty ([Ljava/lang/Object;)Z
 ' 1 2 3 setVariable '(Ljava/lang/String;Ljava/lang/Object;)V 5 6 7 8 9 7cn/zszj/framework/idempotent/core/anno
tation/Idempotent keyArg ()Ljava/lang/String; ; < = > ? /org/springframework/expression/ExpressionParser pars
eExpression ?(Ljava/lang/String;)Lorg/springframework/expression/Expression; A java/lang/String C D E F G )org/
springframework/expression/Expression getValue W(Lorg/springframework/expression/EvaluationContext;Ljava/lang/Class;
)Ljava/lang/Object;  I J K getSignature ()Lorg/aspectj/lang/Signature; M (org/aspectj/lang/reflect/MethodSigna
ture L O  P ()Ljava/lang/reflect/Method;
 R S T U V java/lang/reflect/Method getDeclaringClass ()Ljava/lang/Class;
 X Y Z [ \ java/lang/Class isInterface ()Z  ^ _ ` 	getTarget ()Ljava/lang/Object;
  b c V getClass e f g h 9 org/aspectj/lang/Signature getName
 R j k l getParameterTypes ()[Ljava/lang/Class;
 X n o p getDeclaredMethod @(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method; r java/lang/NoSuchM
ethodException t java/lang/RuntimeException
 s v  w (Ljava/lang/Throwable;)V y Ccn/zszj/framework/idempotent/core/keyresolver/IdempotentKeyResolver Code 
LineNumberTable resolver i(Lorg/aspectj/lang/JoinPoint;Lcn/zszj/framework/idempotent/core/annotation/Idempotent;)Lja
va/lang/String; 
StackMapTable ? [Ljava/lang/Object; ? [Ljava/lang/String; MethodParameters 	joinPoint 
idempotent point 
SourceFile $ExpressionIdempotentKeyResolver.java !    x                 z   ;     *? *? Y? 	? 
*? Y? ? ?    {             | }  z   ?     j+? N+?  :*? 
-? ! :? 'Y? ):? *? #6?? 22? 0????*? ,? 4 ? : :@? B ? @?    {   * 
       
 !  # " $ * % 5 & D % J + [ , ~   " ? -    5 R  ? '  ?  ?   	 ?   ?   
    z   ?     C*? H ? LL+? N M,? Q? W? ,?*? ] ? a*? H ? d ,? i? m?N? sY-? u?   8 9 q  {   & 	   1 
 2  3  4  9 ' : 5 9 9 ; : < ~    ?  L R[ q ?    ?    ?    ?
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\classes\cn\zszj
\framework\idempotent\core\keyresolver\impl\UserIdempotentKeyResolver.class
????   = I
      java/lang/Object <init> ()V  	 
   org/aspectj/lang/JoinPoint getSignature ()Lorg/aspectj/lang/Signature;      org/aspectj/lang/Sign
ature toString ()Ljava/lang/String;
      -cn/zszj/framework/common/util/string/StrUtils joinMethodArgs 0(Lorg/aspectj/lang/JoinPoint;)Ljava/lan
g/String;
      1cn/zszj/framework/web/core/util/WebFrameworkUtils getLoginUserId ()Ljava/lang/Long;
    ! " getLoginUserType ()Ljava/lang/Integer;   $ % & makeConcatWithConstants [(Ljava/lang/String;Ljava/lan
g/String;Ljava/lang/Long;Ljava/lang/Integer;)Ljava/lang/String;
 ( ) * + , cn/hutool/crypto/SecureUtil md5 &(Ljava/lang/String;)Ljava/lang/String; . Lcn/zszj/framework/idempo
tent/core/keyresolver/impl/UserIdempotentKeyResolver 0 Ccn/zszj/framework/idempotent/core/keyresolver/IdempotentKeyRe
solver Code LineNumberTable resolver i(Lorg/aspectj/lang/JoinPoint;Lcn/zszj/framework/idempotent/core/annotation
/Idempotent;)Ljava/lang/String; MethodParameters 	joinPoint 
idempotent 
SourceFile UserIdempotentKeyResolver.java BootstrapMethods <
 = > ? % @ $java/lang/invoke/StringConcatFactory ?(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/l
ang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; B  InnerClasses E %j
ava/lang/invoke/MethodHandles$Lookup G java/lang/invoke/MethodHandles Lookup ! -   /        1        *?
 ?    2         3 4  1   T     ,+?  ? 
 N+? :? :? :-? #  ? '?    2                5   	 6   7    8    9 :     ;  A C   
  D F H 
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\classes\cn\zszj
\framework\idempotent\core\redis\IdempotentRedisDAO.class
????   = ?
      :cn/zszj/framework/idempotent/core/redis/IdempotentRedisDAO 	formatKey &(Ljava/lang/String;)Ljava/lang/
String;	   	 
 
redisTemplate 9Lorg/springframework/data/redis/core/StringRedisTemplate;
  
    7org/springframework/data/redis/core/StringRedisTemplate opsForValue 7()Lorg/springframework/data/redis/co
re/ValueOperations;      3org/springframework/data/redis/core/ValueOperations setIfAbsent Y(Ljava/lang/Obje
ct;Ljava/lang/Object;JLjava/util/concurrent/TimeUnit;)Ljava/lang/Boolean;     get &(Ljava/lang/Object;)Ljava/l
ang/Object;  java/lang/String
      delete '(Ljava/lang/Object;)Ljava/lang/Boolean; " 
idempotent:%s $ java/lang/Object
  & ' ( format 9(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;
 # * + , <init> ()V 
IDEMPOTENT Ljava/lang/String; 
ConstantValue Y(Ljava/lang/String;Ljava/lang/String;JLjava/util/concurrent/TimeUnit;)Ljava/lang/Boolean; Code Line
NumberTable MethodParameters key digest timeout timeUnit 	getDigest (Ljava/lang/String;)V <(Lorg/springfr
amework/data/redis/core/StringRedisTemplate;)V RuntimeInvisibleAnnotations Llombok/Generated; 
SourceFile IdempotentRedisDAO.java !  #     - .  /    !  	 
      0  1   5     +? :*? ? ,!?  ?    2   
    '  ( 3    4   5   6   7    8   1   ,     *? ? +? ?  ? ?    2       2 3    4     9  1   /  
   +? M*? ,? W?    2       6  7  8 3    4   
    1   &     !? #Y*S? %?    2       ; 3    4    + :  1   "     
*? )*+? ?    2        3    	  ;     <    =    >
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\classes\cn\zszj
\framework\lock4j\package-info.class
????   =  %cn/zszj/framework/lock4j/package-info  java/lang/Object  package-info.java 
SourceFile               
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\classes\cn\zszj
\framework\lock4j\config\ZszjLock4jConfiguration.class
????   = 
      java/lang/Object <init> ()V  8cn/zszj/framework/lock4j/core/DefaultLockFailureStrategy
    7cn/zszj/framework/lock4j/config/ZszjLock4jConfiguration Code LineNumberTable lockFailureStrategy <()Lc
n/zszj/framework/lock4j/core/DefaultLockFailureStrategy; RuntimeVisibleAnnotations -Lorg/springframework/context/ann
otation/Bean; 
SourceFile ZszjLock4jConfiguration.java :Lorg/springframework/boot/autoconfigure/AutoConfiguration; before CLcom/
baomidou/lock/spring/boot/autoconfigure/LockAutoConfiguration; ELorg/springframework/boot/autoconfigure/condition/Cond
itionalOnClass; name #com.baomidou.lock.annotation.Lock4j ! 
                   *? ?    
                     ? Y? 	?    
                             [ c    [ s 
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\classes\cn\zszj
\framework\lock4j\core\DefaultLockFailureStrategy.class
????   = ?
      java/lang/Object <init> ()V	  	 
   8cn/zszj/framework/lock4j/core/DefaultLockFailureStrategy log Lorg/slf4j/Logger;  D[onLockFailure][�߳�:{}
 ��ȡ��ʧ�ܣ�key:{} ��ȡʧ��:{} ]
      java/lang/Thread 
currentThread ()Ljava/lang/Thread;
     getName ()Ljava/lang/String;      org/slf4j/Logger debug ((Ljava/lang/String;[Ljava/lang/Obj
ect;)V   3cn/zszj/framework/common/exception/ServiceException	 " # $ % & Acn/zszj/framework/common/exception/enums
/GlobalErrorCodeConstants LOCKED .Lcn/zszj/framework/common/exception/ErrorCode;
  (  ) 1(Lcn/zszj/framework/common/exception/ErrorCode;)V
 + , - . / org/slf4j/LoggerFactory 	getLogger %(Ljava/lang/Class;)Lorg/slf4j/Logger; 1 %com/baomidou/lock/LockF
ailureStrategy RuntimeInvisibleAnnotations Llombok/Generated; Code LineNumberTable 
onLockFailure B(Ljava/lang/String;Ljava/lang/reflect/Method;[Ljava/lang/Object;)V MethodParameters key method 	
arguments <clinit> 
SourceFile DefaultLockFailureStrategy.java !    0      2     3        4        *? ?    5      
   6 7  4   F     *? 
? Y? ? SY+SY-S?  ? Y? !? '?    5   
       8   
 9   :   ;    <   4   !      	? *? ?    5       
  =    >
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\classes\cn\zszj
\framework\lock4j\core\Lock4jRedisKeyConstants.class
????   =   5cn/zszj/framework/lock4j/core/Lock4jRedisKeyConstants  java/lang/Object LOCK4J Ljava/lang/Strin
g; 
ConstantValue 	 	lock4j:%s 
SourceFile Lock4jRedisKeyConstants.java                  
    
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\classes\cn\zszj
\framework\ratelimiter\package-info.class
????   =  *cn/zszj/framework/ratelimiter/package-info  java/lang/Object  package-info.java 
SourceFile               
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\classes\cn\zszj
\framework\ratelimiter\config\ZszjRateLimiterConfiguration.class
????   = >
      java/lang/Object <init> ()V  8cn/zszj/framework/ratelimiter/core/aop/RateLimiterAspect
  
   Q(Ljava/util/List;Lcn/zszj/framework/ratelimiter/core/redis/RateLimiterRedisDAO;)V 
 <cn/zszj/framework/ratelimiter/core/redis/RateLimiterRedisDAO
     $(Lorg/redisson/api/RedissonClient;)V  Qcn/zszj/framework/ratelimiter/core/keyresolver/impl/DefaultRateLi
miterKeyResolver
    Ncn/zszj/framework/ratelimiter/core/keyresolver/impl/UserRateLimiterKeyResolver
    Rcn/zszj/framework/ratelimiter/core/keyresolver/impl/ClientIpRateLimiterKeyResolver
    Tcn/zszj/framework/ratelimiter/core/keyresolver/impl/ServerNodeRateLimiterKeyResolver
    Tcn/zszj/framework/ratelimiter/core/keyresolver/impl/ExpressionRateLimiterKeyResolver
   ! Acn/zszj/framework/ratelimiter/config/ZszjRateLimiterConfiguration Code LineNumberTable rateLimiterAspec
t ?(Ljava/util/List;Lcn/zszj/framework/ratelimiter/core/redis/RateLimiterRedisDAO;)Lcn/zszj/framework/ratelimiter/core
/aop/RateLimiterAspect; MethodParameters keyResolvers rateLimiterRedisDAO 	Signature ?(Ljava/util/List<Lcn/zszj
/framework/ratelimiter/core/keyresolver/RateLimiterKeyResolver;>;Lcn/zszj/framework/ratelimiter/core/redis/RateLimiterR
edisDAO;)Lcn/zszj/framework/ratelimiter/core/aop/RateLimiterAspect; RuntimeVisibleAnnotations -Lorg/springframework/
context/annotation/Bean; a(Lorg/redisson/api/RedissonClient;)Lcn/zszj/framework/ratelimiter/core/redis/RateLimiterRedi
sDAO; redissonClient defaultRateLimiterKeyResolver U()Lcn/zszj/framework/ratelimiter/core/keyresolver/impl/Default
RateLimiterKeyResolver; userRateLimiterKeyResolver R()Lcn/zszj/framework/ratelimiter/core/keyresolver/impl/UserRateL
imiterKeyResolver; clientIpRateLimiterKeyResolver V()Lcn/zszj/framework/ratelimiter/core/keyresolver/impl/ClientIpRa
teLimiterKeyResolver;  serverNodeRateLimiterKeyResolver X()Lcn/zszj/framework/ratelimiter/core/keyresolver/impl/Serve
rNodeRateLimiterKeyResolver;  expressionRateLimiterKeyResolver X()Lcn/zszj/framework/ratelimiter/core/keyresolver/imp
l/ExpressionRateLimiterKeyResolver; 
SourceFile !ZszjRateLimiterConfiguration.java :Lorg/springframework/boot/autoconfigure/AutoConfiguration; after ;L
cn/zszj/framework/redis/config/ZszjRedisAutoConfiguration; !             "        *? ?    #         $ %
  "   "     
? Y+,? 	?    #        &   	 '   (   )    * +     ,    ( -  "   !     	? Y+? ?    #        &    .  
 +     ,    / 0  "         ? Y? ?    #         +     ,    1 2  "         ? Y? ?    #       % + 
    ,    3 4  "         ? Y? ?    #       * +     ,    5 6  "         ? Y? ?    #       / +   
  ,    7 8  "         ? Y? ?    #       4 +     ,    9    : +     ;  <[ c =
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\classes\cn\zszj
\framework\ratelimiter\core\annotation\RateLimiter.class
????   = &  9cn/zszj/framework/ratelimiter/core/annotation/RateLimiter  java/lang/Object  java/lang/annotati
on/Annotation time ()I AnnotationDefault    timeUnit !()Ljava/util/concurrent/TimeUnit; Ljava/util/concurr
ent/TimeUnit; SECONDS count   d message ()Ljava/lang/String;   keyResolver ()Ljava/lang/Class; SLcn/zszj
/framework/ratelimiter/core/keyresolver/impl/DefaultRateLimiterKeyResolver; 	Signature ]()Ljava/lang/Class<+Lcn/zszj/
framework/ratelimiter/core/keyresolver/RateLimiterKeyResolver;>; keyArg maskKeys ()[Ljava/lang/String; 
SourceFile RateLimiter.java RuntimeVisibleAnnotations Ljava/lang/annotation/Target; value "Ljava/lang/annotati
on/ElementType; METHOD  Ljava/lang/annotation/Retention; &Ljava/lang/annotation/RetentionPolicy; RUNTIME&    
       	   I 
    	   e 
     	   I     	   s     	   c          	   s     	   [                 
[ e ! " #   e $ %
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\classes\cn\zszj
\framework\ratelimiter\core\aop\RateLimiterAspect.class
????   = ?
      java/lang/Object <init> ()V    	 
 apply ()Ljava/util/function/Function;
  
    8cn/zszj/framework/common/util/collection/CollectionUtils 
convertMap D(Ljava/util/Collection;Ljava/util/function/Function;)Ljava/util/Map;	      8cn/zszj/framework/rate
limiter/core/aop/RateLimiterAspect keyResolvers Ljava/util/Map;	     rateLimiterRedisDAO >Lcn/zszj/framewor
k/ratelimiter/core/redis/RateLimiterRedisDAO;       9cn/zszj/framework/ratelimiter/core/annotation/RateLimiter
 keyResolver ()Ljava/lang/Class; " # $ % & 
java/util/Map get &(Ljava/lang/Object;)Ljava/lang/Object; ( Ecn/zszj/framework/ratelimiter/core/keyresolver/RateLi
miterKeyResolver * )�Ҳ�����Ӧ�� RateLimiterKeyResolver
 , - . / 0 org/springframework/util/Assert notNull '(Ljava/lang/Object;Ljava/lang/String;)V ' 2 3 4 resolver
 k(Lorg/aspectj/lang/JoinPoint;Lcn/zszj/framework/ratelimiter/core/annotation/RateLimiter;)Ljava/lang/String;  6 7 
8 count ()I  : ; 8 time  = > ? timeUnit !()Ljava/util/concurrent/TimeUnit;
 A B C D E <cn/zszj/framework/ratelimiter/core/redis/RateLimiterRedisDAO 
tryAcquire H(Ljava/lang/String;IILjava/util/concurrent/TimeUnit;)Ljava/lang/Boolean;
 G H I J K java/lang/Boolean booleanValue ()Z	  M N O log Lorg/slf4j/Logger; Q :[beforePointCut][����({
}) ����({}) �������Ƶ��] S T U V W org/aspectj/lang/JoinPoint getSignature ()Lorg/aspectj/lang/Signature; Y Z
 [ \ ] org/aspectj/lang/Signature toString ()Ljava/lang/String;
  _ ` a buildArgMap -(Lorg/aspectj/lang/JoinPoint;)Ljava/util/Map;  c d e maskKeys ()[Ljava/lang/String;
 g h i j k 2cn/zszj/framework/common/util/log/LogSanitizeUtils sanitizeMap 6(Ljava/util/Map;[Ljava/lang/String;)L
java/lang/String; m n o p q org/slf4j/Logger info 9(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V 
 s t ] message	 v w x y z Acn/zszj/framework/common/exception/enums/GlobalErrorCodeConstants TOO_MANY_REQUESTS
 .Lcn/zszj/framework/common/exception/ErrorCode;
 | } ~  ] ,cn/zszj/framework/common/exception/ErrorCode getMsg
 ? ? ? ? ? cn/hutool/core/util/StrUtil blankToDefault >(Ljava/lang/CharSequence;Ljava/lang/String;)Ljava/lang/St
ring; ? 3cn/zszj/framework/common/exception/ServiceException
 | ? ? ? getCode ()Ljava/lang/Integer;
 ? ?  ? ((Ljava/lang/Integer;Ljava/lang/String;)V S ? ? ? getArgs ()[Ljava/lang/Object;
 ? ? ? ? ? java/util/Collections emptyMap ()Ljava/util/Map; ? (org/aspectj/lang/reflect/MethodSignature ? ?
 ? e getParameterNames ? java/util/LinkedHashMap
 ? ?  ? (I)V
 ? ? ? ? 
isNotEmpty (Ljava/lang/CharSequence;)Z  ? ? ? makeConcatWithConstants (I)Ljava/lang/String; " ? ? ? put 8
(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
 ? ? ? ? ? org/slf4j/LoggerFactory 	getLogger %(Ljava/lang/Class;)Lorg/slf4j/Logger; RuntimeInvisibleAnnotation
s Llombok/Generated; 	Signature ?Ljava/util/Map<Ljava/lang/Class<+Lcn/zszj/framework/ratelimiter/core/keyresolver/R
ateLimiterKeyResolver;>;Lcn/zszj/framework/ratelimiter/core/keyresolver/RateLimiterKeyResolver;>; Q(Ljava/util/List;Lc
n/zszj/framework/ratelimiter/core/redis/RateLimiterRedisDAO;)V Code LineNumberTable MethodParameters ?(Ljava/uti
l/List<Lcn/zszj/framework/ratelimiter/core/keyresolver/RateLimiterKeyResolver;>;Lcn/zszj/framework/ratelimiter/core/red
is/RateLimiterRedisDAO;)V beforePointCut Z(Lorg/aspectj/lang/JoinPoint;Lcn/zszj/framework/ratelimiter/core/annotatio
n/RateLimiter;)V 
StackMapTable ? java/lang/String 	joinPoint rateLimiter RuntimeVisibleAnnotations $Lorg/aspectj/lang/annotatio
n/Before; value @annotation(rateLimiter) ? [Ljava/lang/Object; ? [Ljava/lang/String; S(Lorg/aspectj/lang/Joi
nPoint;)Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>; <clinit> 
SourceFile RateLimiterAspect.java $Lorg/aspectj/lang/annotation/Aspect; BootstrapMethods ?
 ? ? ? ? ? "java/lang/invoke/LambdaMetafactory metafactory ?(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/St
ring;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodT
ype;)Ljava/lang/invoke/CallSite; & ?
  ? ?   getClass ? Z(Lcn/zszj/framework/ratelimiter/core/keyresolver/RateLimiterKeyResolver;)Ljava/lang/Class; 
?
 ? ? ? ? ? $java/lang/invoke/StringConcatFactory ?(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/l
ang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; ? arg InnerClasses ? %j
ava/lang/invoke/MethodHandles$Lookup ? java/lang/invoke/MethodHandles Lookup !       N O  ?     ?      
 ?    ?         ?  ?   ;     *? *+?   ? ? *,? ?    ?       (  )  *  + ?   	       ?    ?  ? 
?  ?   ?     ?*? ,?  ? ! ? 'N-)? +-+,? 1 :*? ,? 5 ,? 9 ,? < ? @? F6? F? LP+? R ? X +? ^,? b ? f?
 l ,? r ? u? {? ?:? ?Y? u? ?? ???    ?   >    0  1  3 # 6 * 7 ; 6 C 8 H ; Y < e ; j = s > v = { ? ? A ?    ?
 ? ' ? ?   	 ?   ?   ?     ?  ?s ? 
 ` a  ?   ?     ?*? ? L+? +?? ? ??*? R M,? ?? ,? ?? ? ? N? ?Y+?? ?:6+?? <-? -?? -2? ?? 
-2? 
? ?  :+2? ? W?????    ?   6 
   J  K  L  N  O " P 0 Q ; R E S Z T j U x R ~ W ?   % ?  ??  Y@ ??  ? ""F ??  ?    ?   ?    ?  ? 
  ?   !      	? ?? L?    ?         ?    ? ?     ?   ?     ?  ? ? ? ?  ? ?   
  ? ? ? 
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\classes\cn\zszj
\framework\ratelimiter\core\keyresolver\RateLimiterKeyResolver.class
????   =   Ecn/zszj/framework/ratelimiter/core/keyresolver/RateLimiterKeyResolver  java/lang/Object resolver
 k(Lorg/aspectj/lang/JoinPoint;Lcn/zszj/framework/ratelimiter/core/annotation/RateLimiter;)Ljava/lang/String; MethodP
arameters 	joinPoint rateLimiter 
SourceFile RateLimiterKeyResolver.java              	    	    
    
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\classes\cn\zszj
\framework\ratelimiter\core\keyresolver\impl\ClientIpRateLimiterKeyResolver.class
????   = D
      java/lang/Object <init> ()V  	 
   org/aspectj/lang/JoinPoint getSignature ()Lorg/aspectj/lang/Signature;      org/aspectj/lang/Sign
ature toString ()Ljava/lang/String;
      -cn/zszj/framework/common/util/string/StrUtils joinMethodArgs 0(Lorg/aspectj/lang/JoinPoint;)Ljava/lan
g/String;
      2cn/zszj/framework/common/util/servlet/ServletUtils getClientIP      ! makeConcatWithConstants J(
Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
 # $ % & ' cn/hutool/crypto/SecureUtil md5 &(Ljava/lang/String;)Ljava/lang/String; ) Rcn/zszj/framework/rateli
miter/core/keyresolver/impl/ClientIpRateLimiterKeyResolver + Ecn/zszj/framework/ratelimiter/core/keyresolver/RateLimi
terKeyResolver Code LineNumberTable resolver k(Lorg/aspectj/lang/JoinPoint;Lcn/zszj/framework/ratelimiter/core/a
nnotation/RateLimiter;)Ljava/lang/String; MethodParameters 	joinPoint rateLimiter 
SourceFile #ClientIpRateLimiterKeyResolver.java BootstrapMethods 7
 8 9 :   ; $java/lang/invoke/StringConcatFactory ?(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/l
ang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; =  InnerClasses @ %ja
va/lang/invoke/MethodHandles$Lookup B java/lang/invoke/MethodHandles Lookup ! (   *        ,        *? 
?    -         . /  ,   I     %+?  ? 
 N+? :? :-?   ? "?    -              0   	 1   2    3    4 5     6  < >   
  ? A C 
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\classes\cn\zszj
\framework\ratelimiter\core\keyresolver\impl\DefaultRateLimiterKeyResolver.class
????   = ?
      java/lang/Object <init> ()V  	 
   org/aspectj/lang/JoinPoint getSignature ()Lorg/aspectj/lang/Signature;      org/aspectj/lang/Sign
ature toString ()Ljava/lang/String;
      -cn/zszj/framework/common/util/string/StrUtils joinMethodArgs 0(Lorg/aspectj/lang/JoinPoint;)Ljava/lan
g/String;      makeConcatWithConstants 8(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
     ! " cn/hutool/crypto/SecureUtil md5 &(Ljava/lang/String;)Ljava/lang/String; $ Qcn/zszj/framework/rateli
miter/core/keyresolver/impl/DefaultRateLimiterKeyResolver & Ecn/zszj/framework/ratelimiter/core/keyresolver/RateLimit
erKeyResolver Code LineNumberTable resolver k(Lorg/aspectj/lang/JoinPoint;Lcn/zszj/framework/ratelimiter/core/an
notation/RateLimiter;)Ljava/lang/String; MethodParameters 	joinPoint rateLimiter 
SourceFile "DefaultRateLimiterKeyResolver.java BootstrapMethods 2
 3 4 5  6 $java/lang/invoke/StringConcatFactory ?(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/l
ang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; 8  InnerClasses ; %jav
a/lang/invoke/MethodHandles$Lookup = java/lang/invoke/MethodHandles Lookup ! #   %        '        *? 
?    (         ) *  '   >     +?  ? 
 N+? :-?   ? ?    (            +   	 ,   -    .    / 0     1  7 9   
  : < > 
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\classes\cn\zszj
\framework\ratelimiter\core\keyresolver\impl\ExpressionRateLimiterKeyResolver.class
????   = ?
      java/lang/Object <init> ()V  7org/springframework/core/DefaultParameterNameDiscoverer
  	   
   Tcn/zszj/framework/ratelimiter/core/keyresolver/impl/ExpressionRateLimiterKeyResolver parameterNameDiscoverer
 2Lorg/springframework/core/ParameterNameDiscoverer;  Aorg/springframework/expression/spel/standard/SpelExpressionPa
rser
  	     expressionParser 1Lorg/springframework/expression/ExpressionParser;
     	getMethod 8(Lorg/aspectj/lang/JoinPoint;)Ljava/lang/reflect/Method;       org/aspectj/lang/JoinPo
int getArgs ()[Ljava/lang/Object; " # $ % & 0org/springframework/core/ParameterNameDiscoverer getParameterNam
es /(Ljava/lang/reflect/Method;)[Ljava/lang/String; ( Eorg/springframework/expression/spel/support/StandardEvaluatio
nContext
 ' 
 + , - . / cn/hutool/core/util/ArrayUtil 
isNotEmpty ([Ljava/lang/Object;)Z
 ' 1 2 3 setVariable '(Ljava/lang/String;Ljava/lang/Object;)V 5 6 7 8 9 9cn/zszj/framework/ratelimiter/core/ann
otation/RateLimiter keyArg ()Ljava/lang/String; ; < = > ? /org/springframework/expression/ExpressionParser pa
rseExpression ?(Ljava/lang/String;)Lorg/springframework/expression/Expression; A java/lang/String C D E F G )or
g/springframework/expression/Expression getValue W(Lorg/springframework/expression/EvaluationContext;Ljava/lang/Clas
s;)Ljava/lang/Object;
 I J K L M 2cn/zszj/framework/common/util/servlet/ServletUtils 
getRequest +()Ljakarta/servlet/http/HttpServletRequest;
 O P Q R S 1cn/zszj/framework/web/core/util/WebFrameworkUtils getTenantId ;(Ljakarta/servlet/http/HttpServletRequ
est;)Ljava/lang/Long;  U V W getSignature ()Lorg/aspectj/lang/Signature; Y Z [ \ 9 org/aspectj/lang/Signatu
re toString   ^ _ ` makeConcatWithConstants H(Ljava/lang/String;Ljava/lang/Long;Ljava/lang/String;)Ljava/lang/St
ring;
 b c d e f cn/hutool/crypto/SecureUtil md5 &(Ljava/lang/String;)Ljava/lang/String; h (org/aspectj/lang/reflect
/MethodSignature g j  k ()Ljava/lang/reflect/Method;
 m n o p q java/lang/reflect/Method getDeclaringClass ()Ljava/lang/Class;
 s t u v w java/lang/Class isInterface ()Z  y z { 	getTarget ()Ljava/lang/Object;
  } ~ q getClass Y ? ? 9 getName
 m ? ? ? getParameterTypes ()[Ljava/lang/Class;
 s ? ? ? getDeclaredMethod @(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method; ? java/lang/NoSuchM
ethodException ? java/lang/RuntimeException
 ? ?  ? (Ljava/lang/Throwable;)V ? Ecn/zszj/framework/ratelimiter/core/keyresolver/RateLimiterKeyResolver Code
 LineNumberTable resolver k(Lorg/aspectj/lang/JoinPoint;Lcn/zszj/framework/ratelimiter/core/annotation/RateLimiter;
)Ljava/lang/String; 
StackMapTable ? [Ljava/lang/Object; ? [Ljava/lang/String; ? 'jakarta/servlet/http/HttpServletRequest ? java/
lang/Long MethodParameters 	joinPoint rateLimiter point 
SourceFile %ExpressionRateLimiterKeyResolver.java BootstrapMethods ?
 ? ? ? _ ? $java/lang/invoke/StringConcatFactory ?(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/l
ang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; ? :: InnerClasses ? %
java/lang/invoke/MethodHandles$Lookup ? java/lang/invoke/MethodHandles Lookup !    ?                
 ?   ;     *? *? Y? 	? 
*? Y? ? ?    ?         !  #  ? ?  ?        ?+? N+?  :*? 
-? ! :? 'Y? ):? *? #6?? 22? 0????*? ,? 4 ? : :@? B ? @:? H:		? 	? N? :
+? T ? X :
? ]  ? a?    ?   :    (  ) 
 *  , " - * . 5 / D . J 4 [ 5 k : p ; ? < ? = ?   2 ? -    5 m ? ? '  ? ? 2 C @ ?@ ? ?   	 ?   ?   
    ?   ?     C*? T ? gL+? i M,? l? r? ,?*? x ? |*? T ?  ,? ?? ??N? ?Y-? ??   8 9 ?  ?   & 	   B 
 C  D  E  J ' K 5 J 9 L : M ?    ?  g m[ ? ?    ?    ?    ? ?     ?  ? ?   
  ? ? ? 
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\classes\cn\zszj
\framework\ratelimiter\core\keyresolver\impl\ServerNodeRateLimiterKeyResolver.class
????   = \
      java/lang/Object <init> ()V  	 
   org/aspectj/lang/JoinPoint getSignature ()Lorg/aspectj/lang/Signature;      org/aspectj/lang/Sign
ature toString ()Ljava/lang/String;
      -cn/zszj/framework/common/util/string/StrUtils joinMethodArgs 0(Lorg/aspectj/lang/JoinPoint;)Ljava/lan
g/String;  %s@%d
       cn/hutool/system/SystemUtil getHostInfo ()Lcn/hutool/system/HostInfo;
 " # $ %  cn/hutool/system/HostInfo 
getAddress
  ' ( ) 
getCurrentPID ()J
 + , - . / java/lang/Long valueOf (J)Ljava/lang/Long;
 1 2 3 4 5 java/lang/String format 9(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;   7 8 9 makeCo
ncatWithConstants J(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
 ; < = > ? cn/hutool/crypto/SecureUtil md5 &(Ljava/lang/String;)Ljava/lang/String; A Tcn/zszj/framework/rateli
miter/core/keyresolver/impl/ServerNodeRateLimiterKeyResolver C Ecn/zszj/framework/ratelimiter/core/keyresolver/RateLi
miterKeyResolver Code LineNumberTable resolver k(Lorg/aspectj/lang/JoinPoint;Lcn/zszj/framework/ratelimiter/core
/annotation/RateLimiter;)Ljava/lang/String; MethodParameters 	joinPoint rateLimiter 
SourceFile %ServerNodeRateLimiterKeyResolver.java BootstrapMethods O
 P Q R 8 S $java/lang/invoke/StringConcatFactory ?(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/l
ang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; U  InnerClasses X %ja
va/lang/invoke/MethodHandles$Lookup Z java/lang/invoke/MethodHandles Lookup ! @   B        D        *? 
?    E         F G  D   a     =+?  ? 
 N+? :? Y? ? !SY? &? *S? 0:-? 6  ? :?    E            /  H   	 I   J    K    L M     N  T
 V   
  W Y [ 
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\classes\cn\zszj
\framework\ratelimiter\core\keyresolver\impl\UserRateLimiterKeyResolver.class
????   = I
      java/lang/Object <init> ()V  	 
   org/aspectj/lang/JoinPoint getSignature ()Lorg/aspectj/lang/Signature;      org/aspectj/lang/Sign
ature toString ()Ljava/lang/String;
      -cn/zszj/framework/common/util/string/StrUtils joinMethodArgs 0(Lorg/aspectj/lang/JoinPoint;)Ljava/lan
g/String;
      1cn/zszj/framework/web/core/util/WebFrameworkUtils getLoginUserId ()Ljava/lang/Long;
    ! " getLoginUserType ()Ljava/lang/Integer;   $ % & makeConcatWithConstants [(Ljava/lang/String;Ljava/lan
g/String;Ljava/lang/Long;Ljava/lang/Integer;)Ljava/lang/String;
 ( ) * + , cn/hutool/crypto/SecureUtil md5 &(Ljava/lang/String;)Ljava/lang/String; . Ncn/zszj/framework/rateli
miter/core/keyresolver/impl/UserRateLimiterKeyResolver 0 Ecn/zszj/framework/ratelimiter/core/keyresolver/RateLimiterK
eyResolver Code LineNumberTable resolver k(Lorg/aspectj/lang/JoinPoint;Lcn/zszj/framework/ratelimiter/core/annot
ation/RateLimiter;)Ljava/lang/String; MethodParameters 	joinPoint rateLimiter 
SourceFile UserRateLimiterKeyResolver.java BootstrapMethods <
 = > ? % @ $java/lang/invoke/StringConcatFactory ?(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/l
ang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; B  InnerClasses E %j
ava/lang/invoke/MethodHandles$Lookup G java/lang/invoke/MethodHandles Lookup ! -   /        1        *?
 ?    2         3 4  1   T     ,+?  ? 
 N+? :? :? :-? #  ? '?    2                5   	 6   7    8    9 :     ;  A C   
  D F H 
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\classes\cn\zszj
\framework\ratelimiter\core\redis\RateLimiterRedisDAO.class
????   = ?
      <cn/zszj/framework/ratelimiter/core/redis/RateLimiterRedisDAO getRRateLimiter T(Ljava/lang/String;JILj
ava/util/concurrent/TimeUnit;)Lorg/redisson/api/RRateLimiter;  	 
   org/redisson/api/RRateLimiter 
tryAcquire ()Z
      java/lang/Boolean valueOf (Z)Ljava/lang/Boolean;  java/lang/Exception	     log Lorg/slf
4j/Logger;  @[tryAcquire][���� Key({}) Redis ���ϣ��� fail-open ����]       org/slf4j/Logger error 9(Ljav
a/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V	  " # $ TRUE Ljava/lang/Boolean; & rate_limiter:%s ( j
ava/lang/Object
 * + , - . java/lang/String format 9(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;
  0 1 2 	formatKey &(Ljava/lang/String;)Ljava/lang/String;	  4 5 6 redissonClient !Lorg/redisson/api/RedissonC
lient; 8 9 : ; < org/redisson/api/RedissonClient getRateLimiter 3(Ljava/lang/String;)Lorg/redisson/api/RRateLim
iter;
 > ? @ A B java/util/concurrent/TimeUnit 	toSeconds (J)J
 D E F G H java/time/Duration 	ofSeconds (J)Ljava/time/Duration;  J K L 	getConfig &()Lorg/redisson/api/Rat
eLimiterConfig;	 N O P Q R org/redisson/api/RateType OVERALL Lorg/redisson/api/RateType;  T U V 
trySetRate 3(Lorg/redisson/api/RateType;JLjava/time/Duration;)Z  X Y Z expire (Ljava/time/Duration;)Z
 \ ] ^ _ ` "org/redisson/api/RateLimiterConfig getRateType ()Lorg/redisson/api/RateType;
 \ b c d getRate ()Ljava/lang/Long;
 f g h  i java/lang/Long (J)Ljava/lang/Long;
 k l m n o java/util/Objects equals '(Ljava/lang/Object;Ljava/lang/Object;)Z
 \ q r d getRateInterval	 > t u v SECONDS Ljava/util/concurrent/TimeUnit;
 > x y B toMillis  { | } setRate 3(Lorg/redisson/api/RateType;JLjava/time/Duration;)V
 '  ? ? <init> ()V
 ? ? ? ? ? org/slf4j/LoggerFactory 	getLogger %(Ljava/lang/Class;)Lorg/slf4j/Logger; RuntimeInvisibleAnnotation
s Llombok/Generated; RATE_LIMITER Ljava/lang/String; 
ConstantValue H(Ljava/lang/String;IILjava/util/concurrent/TimeUnit;)Ljava/lang/Boolean; Code LineNumberTable 
StackMapTable MethodParameters key count time timeUnit $(Lorg/redisson/api/RedissonClient;)V <clinit> 
SourceFile RateLimiterRedisDAO.java !  '        ?     ?    ? ?  ?    %  5 6      ?  ?   f     **+?
? :?  ? 
?:? +?  ? !?        ?       "  $  %  ( & ) ?    W  ?    ?   ?   ?   ?   
 1 2  ?   &     %? 'Y*S? )?    ?       . ?    ?       ?       ?+? /:*? 3? 7 :?? =7? C:
? I :? ? M 
? S W
? W W?? [? M? +? a ? e? j? ? p? s? w? e? j? ?? M 
? z 
? W W?    ?   B    2  3  4  5 $ 7 - 8 2 9 @ ; J < M ? Z @ i A } B ? E ? G ? H ?   " ? M 
  * > *  D \  2 ?    ?   ?   ?   ?    ? ?  ?   "     
*? ~*+? 3?    ?        ?    5  ?     ?    ? ?  ?   !      	? ?? ?    ?         ?    ?
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\classes\cn\zszj
\framework\signature\package-info.class
????   =  (cn/zszj/framework/signature/package-info  java/lang/Object  package-info.java 
SourceFile               
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\classes\cn\zszj
\framework\signature\config\ZszjApiSignatureAutoConfiguration.class
????   = "
      java/lang/Object <init> ()V  7cn/zszj/framework/signature/core/aop/ApiSignatureAspect
  
   @(Lcn/zszj/framework/signature/core/redis/ApiSignatureRedisDAO;)V 
 ;cn/zszj/framework/signature/core/redis/ApiSignatureRedisDAO
     <(Lorg/springframework/data/redis/core/StringRedisTemplate;)V  Dcn/zszj/framework/signature/config/ZszjAp
iSignatureAutoConfiguration Code LineNumberTable signatureAspect x(Lcn/zszj/framework/signature/core/redis/ApiSi
gnatureRedisDAO;)Lcn/zszj/framework/signature/core/aop/ApiSignatureAspect; MethodParameters signatureRedisDAO Run
timeVisibleAnnotations -Lorg/springframework/context/annotation/Bean; x(Lorg/springframework/data/redis/core/StringRe
disTemplate;)Lcn/zszj/framework/signature/core/redis/ApiSignatureRedisDAO; stringRedisTemplate 
SourceFile &ZszjApiSignatureAutoConfiguration.java :Lorg/springframework/boot/autoconfigure/AutoConfiguration; afte
r ;Lcn/zszj/framework/redis/config/ZszjRedisAutoConfiguration; !                    *? ?            
       !     	? Y+? 	?                                  !     	? Y+? ?             
                            [ c !
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\classes\cn\zszj
\framework\signature\core\annotation\ApiSignature.class
????   = #  8cn/zszj/framework/signature/core/annotation/ApiSignature  java/lang/Object  java/lang/annotatio
n/Annotation timeout ()I AnnotationDefault   < timeUnit !()Ljava/util/concurrent/TimeUnit; Ljava/util/concu
rrent/TimeUnit; SECONDS message ()Ljava/lang/String; ǩ������ȷ appId 	timestamp nonce sign 
SourceFile ApiSignature.java RuntimeVisibleAnnotations  Ljava/lang/annotation/Inherited; !Ljava/lang/annotation/D
ocumented; Ljava/lang/annotation/Target; value "Ljava/lang/annotation/ElementType; METHOD TYPE  Ljava/lang/an
notation/Retention; &Ljava/lang/annotation/RetentionPolicy; RUNTIME&           	   I 
    	   e 
     	   s     	   s     	   s     	   s     	   s           (          
[ e  e      e ! "
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\classes\cn\zszj
\framework\signature\core\aop\ApiSignatureAspect.class
????   =M
      2cn/zszj/framework/common/util/servlet/ServletUtils 
getRequest +()Ljakarta/servlet/http/HttpServletRequest;
  	 
   java/util/Objects requireNonNull &(Ljava/lang/Object;)Ljava/lang/Object;  'jakarta/servlet/http/HttpServl
etRequest
      7cn/zszj/framework/signature/core/aop/ApiSignatureAspect verifySignature f(Lcn/zszj/framework/signatur
e/core/annotation/ApiSignature;Ljakarta/servlet/http/HttpServletRequest;)Z	     log Lorg/slf4j/Logger;  2[b
eforePointCut][����{} ����({}) ǩ��ʧ��]       org/aspectj/lang/JoinPoint getSignature ()Lorg/aspectj/lang/S
ignature; " # $ % & org/aspectj/lang/Signature toString ()Ljava/lang/String;  ( ) * getArgs ()[Ljava/la
ng/Object; , java/lang/String
 . / 0 1 2 2cn/zszj/framework/common/util/log/LogSanitizeUtils sanitizeArgs :([Ljava/lang/Object;[Ljava/lang/Stri
ng;)Ljava/lang/String; 4 5 6 7 8 org/slf4j/Logger error 9(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object
;)V : 3cn/zszj/framework/common/exception/ServiceException	 < = > ? @ Acn/zszj/framework/common/exception/enums/Gl
obalErrorCodeConstants BAD_REQUEST .Lcn/zszj/framework/common/exception/ErrorCode;
 B C D E F ,cn/zszj/framework/common/exception/ErrorCode getCode ()Ljava/lang/Integer; H I J K & 8cn/zszj/fr
amework/signature/core/annotation/ApiSignature message
 B M N & getMsg
 P Q R S T cn/hutool/core/util/StrUtil blankToDefault >(Ljava/lang/CharSequence;Ljava/lang/String;)Ljava/lang/St
ring;
 9 V W X <init> ((Ljava/lang/Integer;Ljava/lang/String;)V
  Z [  
verifyHeaders H ] ^ & appId 
 ` a b 	getHeader &(Ljava/lang/String;)Ljava/lang/String;	  d e f signatureRedisDAO =Lcn/zszj/framework/signatu
re/core/redis/ApiSignatureRedisDAO;
 h i j k b ;cn/zszj/framework/signature/core/redis/ApiSignatureRedisDAO getAppSecret m ([appId({})] �Ҳ�����Ӧ�� a
ppSecret o java/lang/Object
 q r s t u cn/hutool/core/lang/Assert notNull K(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)Ljava/la
ng/Object; H w x & sign
  z { | buildSignatureString ?(Lcn/zszj/framework/signature/core/annotation/ApiSignature;Ljakarta/servlet/http/Htt
pServletRequest;Ljava/lang/String;)Ljava/lang/String;
 ~  ? ? b "cn/hutool/crypto/digest/DigestUtil 	sha256Hex
 ? ? ? ? ? cn/hutool/core/util/ObjUtil notEqual '(Ljava/lang/Object;Ljava/lang/Object;)Z H ? ? & nonce H ?
 ? ? timeout ()I H ? ? ? timeUnit !()Ljava/util/concurrent/TimeUnit;
 h ? ? ? setNonce Y(Ljava/lang/String;Ljava/lang/String;ILjava/util/concurrent/TimeUnit;)Ljava/lang/Boolean;
 ? ? ? ? ? cn/hutool/core/util/BooleanUtil isFalse (Ljava/lang/Boolean;)Z H ? ? & 	timestamp ? P[verifySig
nature][appId({}) timestamp({}) nonce({}) sign({}) �����ظ�����] ? *** 4 ? ? ? info ((Ljava/lang/String;[Ljava/l
ang/Object;)V	 < ? ? @ REPEATED_REQUESTS ? �����ظ�����
 P ? ? ? isBlank (Ljava/lang/CharSequence;)Z
 P ? ? ? length (Ljava/lang/CharSequence;)I
 ? ? ? ? ? java/util/concurrent/TimeUnit toMillis (J)J
 ? ? ? ? ? java/lang/Long 	parseLong (Ljava/lang/String;)J
 ? ? ? ? ? java/lang/System currentTimeMillis ()J
 ? ? ? ? ? java/lang/Math abs
 h ? ? ? getNonce 8(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
  ? ? ? getRequestParameterMap @(Ljakarta/servlet/http/HttpServletRequest;)Ljava/util/SortedMap;
  ? ? ? getRequestHeaderMap z(Lcn/zszj/framework/signature/core/annotation/ApiSignature;Ljakarta/servlet/http/Http
ServletRequest;)Ljava/util/SortedMap;
  ? ? ? getBody =(Ljakarta/servlet/http/HttpServletRequest;)Ljava/lang/String; ?  
 P ? ? T 
nullToDefault ? & ? =
 ? ? ? ? ? cn/hutool/core/map/MapUtil join Z(Ljava/util/Map;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Stri
ng;)Ljava/lang/String;   ? ? ? makeConcatWithConstants \(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lja
va/lang/String;)Ljava/lang/String; ? java/util/TreeMap
 ? ? W ? ()V ? ? ? ? ? java/util/SortedMap put 8(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object; 
 ? ? ? getParameterMap ()Ljava/util/Map; ?  
java/util/Map entrySet ()Ljava/util/Set;	 
java/util/Set iterator ()Ljava/util/Iterator;
 java/util/Iterator hasNext ()Z next ()Ljava/lang/Object; java/util/Map$Entry 
getKey getValue [Ljava/lang/String;
 n ?
 !"#$ org/slf4j/LoggerFactory 	getLogger %(Ljava/lang/Class;)Lorg/slf4j/Logger; RuntimeInvisibleAnnotation
s Llombok/Generated; beforePointCut Y(Lorg/aspectj/lang/JoinPoint;Lcn/zszj/framework/signature/core/annotation/Api
Signature;)V Code LineNumberTable 
StackMapTable MethodParameters 	joinPoint 	signature RuntimeVisibleAnnotations $Lorg/aspectj/lang/annotation/Bef
ore; value @annotation(signature) request 	appSecret 	Signature ?(Lcn/zszj/framework/signature/core/annotation
/ApiSignature;Ljakarta/servlet/http/HttpServletRequest;)Ljava/util/SortedMap<Ljava/lang/String;Ljava/lang/String;>; f(
Ljakarta/servlet/http/HttpServletRequest;)Ljava/util/SortedMap<Ljava/lang/String;Ljava/lang/String;>; @(Lcn/zszj/frame
work/signature/core/redis/ApiSignatureRedisDAO;)V <clinit> 
SourceFile ApiSignatureAspect.java $Lorg/aspectj/lang/annotation/Aspect; BootstrapMethods?
@AB ?C $java/lang/invoke/StringConcatFactory ?(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/l
ang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;E  InnerClasses Entr
yI %java/lang/invoke/MethodHandles$LookupK java/lang/invoke/MethodHandles Lookup !  n       %    &  
  e f    '( )   ?     Q*,? ? ? 
? ? ?? +?  ? ! +? ' ? +? -? 3 ? 9Y? ;? A,? G ? ;? L? O? U?   *       ,  -  2 # 3 / 2 4 4 ? 5+    
,   	-  .  /    0 1s2    )  L  
   ?*+,? Y? ?,+? \ ? _ N*? c-? g:l? nY-S? pW,+? v ? _ :*+,? y:? }:? ?? ?,+? ? ? _ :*? c-+
? ? h+? ? ? ?? ?? B,+? ? ? _ :	? ?? nY-SY	SYSY?S? ? ? 9Y? ?? A?? U??   *   B    : 	 ;  >  ? " @
 2 C @ D J E Q F [ G ] K k L ? M ? O ? P ? R+   ( ? Q   H 
 + + + + +  ? j +,   	.  3    [  )  &  
   ?,+? \ ? _ N-? ?? ?,+? ? ? _ :? ?? ?,+? ? ? _ :? ?
? ?,+? v ? _ :? ?? ?+? ? +? ? ?? ?7? ?7	? ?	e? ?7?? ?*? c-? ?? ? ?   *   J    c 
 d  e  g $ h , i . k < l F m H o V p ^ q ` u r v y w ? x ? y ? }+   # ?  +?  +?  +?  +? -@,   	.  
3    { | )   g     ;,? ?:+,? ?:,? ?? ?:??? +? ???? +? ?-? ?  ?   *       ?  ? 
 ?  ? 1 ? : ?,   
.  3  4   
 ? ? )   }     U? ?Y? ?M,*? \ +*? \ ? _ ? ? W,*? ? +*? ? ? _ ? ? W,*? ? +*? ? ? _ ? ? W,?   *      
 ?  ? ! ? : ? S ?,   	.  3  5   6 
 ? ? )   ?     L? ?Y? ?L*? ? ? ? ? M,?
 ? +,? ?N+-? ? +-? ?2? ? W???+?   *       ?  ? , ? G ? J ?+    ?  ?? 0,   3  5   7 
 W8 )   "     
*?*+? c?   *       $,    e %    &   9 ? )   !      	?? ?   *       # :   ;/    <  
=    > DF     ?G	HJL 
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\classes\cn\zszj
\framework\signature\core\redis\ApiSignatureRedisDAO.class
????   = K	      ;cn/zszj/framework/signature/core/redis/ApiSignatureRedisDAO stringRedisTemplate 9Lorg/spri
ngframework/data/redis/core/StringRedisTemplate;
  	 
   7org/springframework/data/redis/core/StringRedisTemplate opsForValue 7()Lorg/springframework/data/redis/core/
ValueOperations;
     formatNonceKey 8(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;      3org/springframewor
k/data/redis/core/ValueOperations get &(Ljava/lang/Object;)Ljava/lang/Object;  java/lang/String       
 setIfAbsent Y(Ljava/lang/Object;Ljava/lang/Object;JLjava/util/concurrent/TimeUnit;)Ljava/lang/Boolean;   api_sign
ature_nonce:%s:%s " java/lang/Object
  $ % & format 9(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;
  ( ) * 
opsForHash 6()Lorg/springframework/data/redis/core/HashOperations; , api_signature_app . / 0  1 2org/springfra
mework/data/redis/core/HashOperations 8(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
 ! 3 4 5 <init> ()V SIGNATURE_NONCE Ljava/lang/String; 
ConstantValue SIGNATURE_APPID getNonce Code LineNumberTable MethodParameters appId nonce setNonce Y(Lj
ava/lang/String;Ljava/lang/String;ILjava/util/concurrent/TimeUnit;)Ljava/lang/Boolean; time timeUnit getAppSecret
 &(Ljava/lang/String;)Ljava/lang/String; <(Lorg/springframework/data/redis/core/StringRedisTemplate;)V RuntimeInvis
ibleAnnotations Llombok/Generated; 
SourceFile ApiSignatureRedisDAO.java !  !          6 7  8      9 7  8    +   :   ;   -     *? ? 
+,? 
?  ? ?    <       ( =   	 >   ?    @ A  ;   0     *? ? +,? 
??  ?    <       , =    >   ?   B   C   
    ;   *     ? !Y*SY+S? #?    <       0 =   	 >   ?    D E  ;   +     *? ? '++? - ? ?    <   
    6 =    >    4 F  ;   "     
*? 2*+? ?    <       
 =      G     H    I    J
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\classes\META-IN
F\spring\org.springframework.boot.autoconfigure.AutoConfiguration.imports
cn.zszj.framework.idempotent.config.ZszjIdempotentConfiguration
cn.zszj.framework.lock4j.config.ZszjLock4jConfiguration
cn.zszj.framework.ratelimiter.config.ZszjRateLimiterConfiguration
cn.zszj.framework.signature.config.ZszjApiSignatureAutoConfiguration
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\maven-status\ma
ven-compiler-plugin\compile\default-compile\createdFiles.lst
cn\zszj\framework\ratelimiter\core\keyresolver\impl\ExpressionRateLimiterKeyResolver.class
cn\zszj\framework\ratelimiter\core\keyresolver\impl\ClientIpRateLimiterKeyResolver.class
cn\zszj\framework\lock4j\package-info.class
cn\zszj\framework\lock4j\core\DefaultLockFailureStrategy.class
cn\zszj\framework\ratelimiter\core\annotation\RateLimiter.class
cn\zszj\framework\idempotent\package-info.class
cn\zszj\framework\signature\config\ZszjApiSignatureAutoConfiguration.class
cn\zszj\framework\lock4j\core\Lock4jRedisKeyConstants.class
cn\zszj\framework\ratelimiter\core\keyresolver\impl\UserRateLimiterKeyResolver.class
cn\zszj\framework\lock4j\config\ZszjLock4jConfiguration.class
cn\zszj\framework\idempotent\core\keyresolver\impl\UserIdempotentKeyResolver.class
cn\zszj\framework\signature\core\annotation\ApiSignature.class
cn\zszj\framework\idempotent\config\ZszjIdempotentConfiguration.class
cn\zszj\framework\idempotent\core\keyresolver\impl\DefaultIdempotentKeyResolver.class
cn\zszj\framework\ratelimiter\core\keyresolver\impl\DefaultRateLimiterKeyResolver.class
cn\zszj\framework\idempotent\core\annotation\Idempotent.class
cn\zszj\framework\ratelimiter\core\keyresolver\impl\ServerNodeRateLimiterKeyResolver.class
cn\zszj\framework\idempotent\core\keyresolver\IdempotentKeyResolver.class
cn\zszj\framework\ratelimiter\package-info.class
cn\zszj\framework\ratelimiter\core\redis\RateLimiterRedisDAO.class
cn\zszj\framework\ratelimiter\config\ZszjRateLimiterConfiguration.class
cn\zszj\framework\ratelimiter\core\keyresolver\RateLimiterKeyResolver.class
cn\zszj\framework\ratelimiter\core\aop\RateLimiterAspect.class
cn\zszj\framework\signature\core\redis\ApiSignatureRedisDAO.class
cn\zszj\framework\signature\package-info.class
cn\zszj\framework\idempotent\core\keyresolver\impl\ExpressionIdempotentKeyResolver.class
cn\zszj\framework\idempotent\core\aop\IdempotentAspect.class
cn\zszj\framework\idempotent\core\redis\IdempotentRedisDAO.class
cn\zszj\framework\signature\core\aop\ApiSignatureAspect.class
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\maven-status\ma
ven-compiler-plugin\compile\default-compile\inputFiles.lst
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\idempotent\config\ZszjIdempotentConfiguration.java
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\idempotent\core\annotation\Idempotent.java
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\idempotent\core\aop\IdempotentAspect.java
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\idempotent\core\keyresolver\IdempotentKeyResolver.java
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\idempotent\core\keyresolver\impl\DefaultIdempotentKeyResolver.java
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\idempotent\core\keyresolver\impl\ExpressionIdempotentKeyResolver.java
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\idempotent\core\keyresolver\impl\UserIdempotentKeyResolver.java
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\idempotent\core\redis\IdempotentRedisDAO.java
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\idempotent\package-info.java
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\lock4j\config\ZszjLock4jConfiguration.java
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\lock4j\core\DefaultLockFailureStrategy.java
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\lock4j\core\Lock4jRedisKeyConstants.java
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\lock4j\package-info.java
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\ratelimiter\config\ZszjRateLimiterConfiguration.java
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\ratelimiter\core\annotation\RateLimiter.java
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\ratelimiter\core\aop\RateLimiterAspect.java
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\ratelimiter\core\keyresolver\impl\ClientIpRateLimiterKeyResolver.java
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\ratelimiter\core\keyresolver\impl\DefaultRateLimiterKeyResolver.java
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\ratelimiter\core\keyresolver\impl\ExpressionRateLimiterKeyResolver.java
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\ratelimiter\core\keyresolver\impl\ServerNodeRateLimiterKeyResolver.java
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\ratelimiter\core\keyresolver\impl\UserRateLimiterKeyResolver.java
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\ratelimiter\core\keyresolver\RateLimiterKeyResolver.java
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\ratelimiter\core\redis\RateLimiterRedisDAO.java
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\ratelimiter\package-info.java
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\signature\config\ZszjApiSignatureAutoConfiguration.java
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\signature\core\annotation\ApiSignature.java
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\signature\core\aop\ApiSignatureAspect.java
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\signature\core\redis\ApiSignatureRedisDAO.java
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\main\java\cn\zszj\
framework\signature\package-info.java
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\maven-status\ma
ven-compiler-plugin\testCompile\default-testCompile\createdFiles.lst
cn\zszj\framework\ratelimiter\core\keyresolver\impl\ExpressionRateLimiterKeyResolverTest$Fixture.class
cn\zszj\framework\signature\core\aop\ApiSignatureAspectTest.class
cn\zszj\framework\idempotent\core\keyresolver\impl\DefaultIdempotentKeyResolverTest$FixedSignature.class
cn\zszj\framework\idempotent\core\aop\IdempotentAspectTest$FixedKeyResolver.class
cn\zszj\framework\ratelimiter\core\aop\RateLimiterAspectTest$FixedSignature.class
cn\zszj\framework\ratelimiter\core\keyresolver\impl\ExpressionRateLimiterKeyResolverTest.class
cn\zszj\framework\idempotent\core\aop\IdempotentAspectTest$FixedSignature.class
cn\zszj\framework\idempotent\core\keyresolver\impl\DefaultIdempotentKeyResolverTest.class
cn\zszj\framework\signature\core\aop\ApiSignatureAspectTest$FixedSignature.class
cn\zszj\framework\idempotent\core\aop\IdempotentAspectTest.class
cn\zszj\framework\signature\core\ApiSignatureTest.class
cn\zszj\framework\ratelimiter\core\keyresolver\impl\ExpressionRateLimiterKeyResolverTest$FixedMethodSignature.class
cn\zszj\framework\ratelimiter\core\aop\RateLimiterAspectTest$FixedKeyResolver.class
cn\zszj\framework\ratelimiter\core\aop\RateLimiterAspectTest.class
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\maven-status\ma
ven-compiler-plugin\testCompile\default-testCompile\inputFiles.lst
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\test\java\cn\zszj\
framework\idempotent\core\aop\IdempotentAspectTest.java
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\test\java\cn\zszj\
framework\idempotent\core\keyresolver\impl\DefaultIdempotentKeyResolverTest.java
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\test\java\cn\zszj\
framework\ratelimiter\core\aop\RateLimiterAspectTest.java
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\test\java\cn\zszj\
framework\ratelimiter\core\keyresolver\impl\ExpressionRateLimiterKeyResolverTest.java
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\test\java\cn\zszj\
framework\signature\core\aop\ApiSignatureAspectTest.java
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\test\java\cn\zszj\
framework\signature\core\ApiSignatureTest.java
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\2026-09-10T11-01-51_970.dumpstream
# Created at 2026-09-10T11:01:54.237
Boot Manifest-JAR contains absolute paths in classpath 'E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zs
zj-spring-boot-starter-protection\target\test-classes'
Hint: <argLine>-Djdk.net.URLClassPath.disableClassPathURLCheck=true</argLine>
'other' has different root

E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\2026-09-10T11-28-18_669.dumpstream
# Created at 2026-09-10T11:28:25.026
Boot Manifest-JAR contains absolute paths in classpath 'E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zs
zj-spring-boot-starter-protection\target\test-classes'
Hint: <argLine>-Djdk.net.URLClassPath.disableClassPathURLCheck=true</argLine>
'other' has different root

E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\2026-09-10T12-02-07_387.dumpstream
# Created at 2026-09-10T12:02:10.779
Boot Manifest-JAR contains absolute paths in classpath 'E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zs
zj-spring-boot-starter-protection\target\test-classes'
Hint: <argLine>-Djdk.net.URLClassPath.disableClassPathURLCheck=true</argLine>
'other' has different root

E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\2026-09-11T10-32-54_077.dumpstream
# Created at 2026-09-11T10:33:07.909
Boot Manifest-JAR contains absolute paths in classpath 'E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zs
zj-spring-boot-starter-protection\target\test-classes'
Hint: <argLine>-Djdk.net.URLClassPath.disableClassPathURLCheck=true</argLine>
'other' has different root

E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\2026-09-11T10-40-04_187.dumpstream
# Created at 2026-09-11T10:40:18.472
Boot Manifest-JAR contains absolute paths in classpath 'E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zs
zj-spring-boot-starter-protection\target\test-classes'
Hint: <argLine>-Djdk.net.URLClassPath.disableClassPathURLCheck=true</argLine>
'other' has different root

E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\2026-09-11T12-01-12_768.dumpstream
# Created at 2026-09-11T12:01:33.098
Boot Manifest-JAR contains absolute paths in classpath 'E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zs
zj-spring-boot-starter-protection\target\test-classes'
Hint: <argLine>-Djdk.net.URLClassPath.disableClassPathURLCheck=true</argLine>
'other' has different root

E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\2026-09-12T12-21-02_632.dumpstream
# Created at 2026-09-12T12:21:06.163
Boot Manifest-JAR contains absolute paths in classpath 'E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zs
zj-spring-boot-starter-protection\target\test-classes'
Hint: <argLine>-Djdk.net.URLClassPath.disableClassPathURLCheck=true</argLine>
'other' has different root

E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\2026-09-12T12-45-49_289.dumpstream
# Created at 2026-09-12T12:45:53.698
Boot Manifest-JAR contains absolute paths in classpath 'E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zs
zj-spring-boot-starter-protection\target\test-classes'
Hint: <argLine>-Djdk.net.URLClassPath.disableClassPathURLCheck=true</argLine>
'other' has different root

E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\2026-09-12T12-47-27_496.dumpstream
# Created at 2026-09-12T12:47:31.412
Boot Manifest-JAR contains absolute paths in classpath 'E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zs
zj-spring-boot-starter-protection\target\test-classes'
Hint: <argLine>-Djdk.net.URLClassPath.disableClassPathURLCheck=true</argLine>
'other' has different root

E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\2026-09-12T12-47-51_439.dumpstream
# Created at 2026-09-12T12:47:52.665
Boot Manifest-JAR contains absolute paths in classpath 'E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zs
zj-spring-boot-starter-protection\target\test-classes'
Hint: <argLine>-Djdk.net.URLClassPath.disableClassPathURLCheck=true</argLine>
'other' has different root

E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\2026-09-12T13-20-36_092.dumpstream
# Created at 2026-09-12T13:20:43.451
Boot Manifest-JAR contains absolute paths in classpath 'E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zs
zj-spring-boot-starter-protection\target\test-classes'
Hint: <argLine>-Djdk.net.URLClassPath.disableClassPathURLCheck=true</argLine>
'other' has different root

E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\2026-09-12T13-48-06_988.dumpstream
# Created at 2026-09-12T13:48:09.799
Boot Manifest-JAR contains absolute paths in classpath 'E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zs
zj-spring-boot-starter-protection\target\test-classes'
Hint: <argLine>-Djdk.net.URLClassPath.disableClassPathURLCheck=true</argLine>
'other' has different root

E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\2026-09-12T13-51-22_032.dumpstream
# Created at 2026-09-12T13:51:29.229
Boot Manifest-JAR contains absolute paths in classpath 'E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zs
zj-spring-boot-starter-protection\target\test-classes'
Hint: <argLine>-Djdk.net.URLClassPath.disableClassPathURLCheck=true</argLine>
'other' has different root

E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\cn.zszj.framework.idempotent.core.aop.IdempotentAspectTest.txt
-------------------------------------------------------------------------------
Test set: cn.zszj.framework.idempotent.core.aop.IdempotentAspectTest
-------------------------------------------------------------------------------
Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.261 s -- in cn.zszj.framework.idempotent.core.aop.Ide
mpotentAspectTest
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\cn.zszj.framework.idempotent.core.keyresolver.impl.DefaultIdempotentKeyResolverTest.txt
-------------------------------------------------------------------------------
Test set: cn.zszj.framework.idempotent.core.keyresolver.impl.DefaultIdempotentKeyResolverTest
-------------------------------------------------------------------------------
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.045 s -- in cn.zszj.framework.idempotent.core.keyreso
lver.impl.DefaultIdempotentKeyResolverTest
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\cn.zszj.framework.ratelimiter.core.aop.RateLimiterAspectTest.txt
-------------------------------------------------------------------------------
Test set: cn.zszj.framework.ratelimiter.core.aop.RateLimiterAspectTest
-------------------------------------------------------------------------------
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.273 s -- in cn.zszj.framework.ratelimiter.core.aop.Ra
teLimiterAspectTest
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\cn.zszj.framework.ratelimiter.core.keyresolver.impl.ExpressionRateLimiterKeyResolverTest.txt
-------------------------------------------------------------------------------
Test set: cn.zszj.framework.ratelimiter.core.keyresolver.impl.ExpressionRateLimiterKeyResolverTest
-------------------------------------------------------------------------------
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.079 s -- in cn.zszj.framework.ratelimiter.core.keyres
olver.impl.ExpressionRateLimiterKeyResolverTest
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\cn.zszj.framework.signature.core.aop.ApiSignatureAspectTest.txt
-------------------------------------------------------------------------------
Test set: cn.zszj.framework.signature.core.aop.ApiSignatureAspectTest
-------------------------------------------------------------------------------
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.055 s -- in cn.zszj.framework.signature.core.aop.ApiS
ignatureAspectTest
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\cn.zszj.framework.signature.core.ApiSignatureTest.txt
-------------------------------------------------------------------------------
Test set: cn.zszj.framework.signature.core.ApiSignatureTest
-------------------------------------------------------------------------------
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.044 s -- in cn.zszj.framework.signature.core.ApiSigna
tureTest
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\TEST-cn.zszj.framework.idempotent.core.aop.IdempotentAspectTest.xml
<?xml version="1.0" encoding="UTF-8"?>
<testsuite xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="https://maven.apache.or
g/surefire/maven-surefire-plugin/xsd/surefire-test-report.xsd" version="3.0.2" name="cn.zszj.framework.idempotent.core.
aop.IdempotentAspectTest" time="1.261" tests="8" errors="0" skipped="0" failures="0">
  <properties>
    <property name="java.specification.version" value="17"/>
    <property name="sun.cpu.isalist" value="amd64"/>
    <property name="sun.jnu.encoding" value="GBK"/>
    <property name="java.class.path" value="E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-bo
ot-starter-protection\target\test-classes;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot
-starter-protection\target\classes;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starte
r-web\target\classes;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-common\target\classes;C:\Users\A
dministrator\.m2\repository\io\opentelemetry\opentelemetry-api\1.65.0\opentelemetry-api-1.65.0.jar;C:\Users\Administrat
or\.m2\repository\io\opentelemetry\opentelemetry-context\1.49.0\opentelemetry-context-1.49.0.jar;C:\Users\Administrator
\.m2\repository\org\projectlombok\lombok\1.18.46\lombok-1.18.46.jar;C:\Users\Administrator\.m2\repository\org\mapstruct
\mapstruct\1.6.3\mapstruct-1.6.3.jar;C:\Users\Administrator\.m2\repository\org\mapstruct\mapstruct-jdk8\1.6.3\mapstruct
-jdk8-1.6.3.jar;C:\Users\Administrator\.m2\repository\org\mapstruct\mapstruct-processor\1.6.3\mapstruct-processor-1.6.3
.jar;C:\Users\Administrator\.m2\repository\cn\hutool\hutool-all\5.8.47\hutool-all-5.8.47.jar;C:\Users\Administrator\.m2
\repository\com\alibaba\transmittable-thread-local\2.14.5\transmittable-thread-local-2.14.5.jar;C:\Users\Administrator\
.m2\repository\com\alibaba\fastjson\2.0.64\fastjson-2.0.64.jar;C:\Users\Administrator\.m2\repository\com\alibaba\fastjs
on2\fastjson2-extension\2.0.64\fastjson2-extension-2.0.64.jar;C:\Users\Administrator\.m2\repository\com\alibaba\fastjso
n2\fastjson2\2.0.64\fastjson2-2.0.64.jar;C:\Users\Administrator\.m2\repository\org\dromara\easy-trans-anno\3.1.8\easy-t
rans-anno-3.1.8.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-web\3.5.15\sprin
g-boot-starter-web-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter\3.5.15
\spring-boot-starter-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot\3.5.15\sprin
g-boot-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-autoconfigure\3.5.15\sprin
g-boot-autoconfigure-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-logg
ing\3.5.15\spring-boot-starter-logging-3.5.15.jar;C:\Users\Administrator\.m2\repository\ch\qos\logback\logback-classic\
1.5.34\logback-classic-1.5.34.jar;C:\Users\Administrator\.m2\repository\ch\qos\logback\logback-core\1.5.34\logback-core
-1.5.34.jar;C:\Users\Administrator\.m2\repository\org\apache\logging\log4j\log4j-to-slf4j\2.24.3\log4j-to-slf4j-2.24.3.
jar;C:\Users\Administrator\.m2\repository\org\apache\logging\log4j\log4j-api\2.24.3\log4j-api-2.24.3.jar;C:\Users\Admin
istrator\.m2\repository\org\slf4j\jul-to-slf4j\2.0.18\jul-to-slf4j-2.0.18.jar;C:\Users\Administrator\.m2\repository\jak
arta\annotation\jakarta.annotation-api\2.1.1\jakarta.annotation-api-2.1.1.jar;C:\Users\Administrator\.m2\repository\org
\yaml\snakeyaml\2.4\snakeyaml-2.4.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starte
r-json\3.5.15\spring-boot-starter-json-3.5.15.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\datatype\
jackson-datatype-jdk8\2.21.4\jackson-datatype-jdk8-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jacks
on\module\jackson-module-parameter-names\2.21.4\jackson-module-parameter-names-2.21.4.jar;C:\Users\Administrator\.m2\re
pository\org\springframework\boot\spring-boot-starter-tomcat\3.5.15\spring-boot-starter-tomcat-3.5.15.jar;C:\Users\Admi
nistrator\.m2\repository\org\apache\tomcat\embed\tomcat-embed-core\10.1.55\tomcat-embed-core-10.1.55.jar;C:\Users\Admin
istrator\.m2\repository\org\apache\tomcat\embed\tomcat-embed-el\10.1.55\tomcat-embed-el-10.1.55.jar;C:\Users\Administra
tor\.m2\repository\org\apache\tomcat\embed\tomcat-embed-websocket\10.1.55\tomcat-embed-websocket-10.1.55.jar;C:\Users\A
dministrator\.m2\repository\org\springframework\spring-web\6.2.19\spring-web-6.2.19.jar;C:\Users\Administrator\.m2\repo
sitory\org\springframework\spring-beans\6.2.19\spring-beans-6.2.19.jar;C:\Users\Administrator\.m2\repository\io\microme
ter\micrometer-observation\1.15.12\micrometer-observation-1.15.12.jar;C:\Users\Administrator\.m2\repository\io\micromet
er\micrometer-commons\1.15.12\micrometer-commons-1.15.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\
spring-webmvc\6.2.19\spring-webmvc-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-aop\6.2.
19\spring-aop-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-context\6.2.19\spring-context
-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-expression\6.2.19\spring-expression-6.2.19
.jar;C:\Users\Administrator\.m2\repository\com\github\xiaoymin\knife4j-openapi3-jakarta-spring-boot-starter\4.5.0\knife
4j-openapi3-jakarta-spring-boot-starter-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\github\xiaoymin\knife4j-cor
e\4.5.0\knife4j-core-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\github\xiaoymin\knife4j-openapi3-ui\4.5.0\knif
e4j-openapi3-ui-4.5.0.jar;C:\Users\Administrator\.m2\repository\org\springdoc\springdoc-openapi-starter-webmvc-ui\2.8.1
7\springdoc-openapi-starter-webmvc-ui-2.8.17.jar;C:\Users\Administrator\.m2\repository\org\springdoc\springdoc-openapi-
starter-webmvc-api\2.8.17\springdoc-openapi-starter-webmvc-api-2.8.17.jar;C:\Users\Administrator\.m2\repository\org\spr
ingdoc\springdoc-openapi-starter-common\2.8.17\springdoc-openapi-starter-common-2.8.17.jar;C:\Users\Administrator\.m2\r
epository\org\springframework\boot\spring-boot-starter-validation\3.5.15\spring-boot-starter-validation-3.5.15.jar;C:\U
sers\Administrator\.m2\repository\org\hibernate\validator\hibernate-validator\8.0.3.Final\hibernate-validator-8.0.3.Fin
al.jar;C:\Users\Administrator\.m2\repository\org\jboss\logging\jboss-logging\3.6.3.Final\jboss-logging-3.6.3.Final.jar;
C:\Users\Administrator\.m2\repository\com\fasterxml\classmate\1.7.3\classmate-1.7.3.jar;C:\Users\Administrator\.m2\repo
sitory\io\swagger\core\v3\swagger-core-jakarta\2.2.47\swagger-core-jakarta-2.2.47.jar;C:\Users\Administrator\.m2\reposi
tory\io\swagger\core\v3\swagger-annotations-jakarta\2.2.47\swagger-annotations-jakarta-2.2.47.jar;C:\Users\Administrato
r\.m2\repository\io\swagger\core\v3\swagger-models-jakarta\2.2.47\swagger-models-jakarta-2.2.47.jar;C:\Users\Administra
tor\.m2\repository\com\fasterxml\jackson\dataformat\jackson-dataformat-yaml\2.21.4\jackson-dataformat-yaml-2.21.4.jar;C
:\Users\Administrator\.m2\repository\org\webjars\swagger-ui\5.32.2\swagger-ui-5.32.2.jar;C:\Users\Administrator\.m2\rep
ository\org\webjars\webjars-locator-lite\1.1.3\webjars-locator-lite-1.1.3.jar;C:\Users\Administrator\.m2\repository\org
\jspecify\jspecify\1.0.0\jspecify-1.0.0.jar;C:\Users\Administrator\.m2\repository\org\jsoup\jsoup\1.23.2\jsoup-1.23.2.j
ar;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-redis\target\classes;C:\Users\
Administrator\.m2\repository\org\redisson\redisson-spring-boot-starter\4.7.0\redisson-spring-boot-starter-4.7.0.jar;C:\
Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-data-redis\3.5.15\spring-boot-starter-d
ata-redis-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\redisson\redisson\4.7.0\redisson-4.7.0.jar;C:\Users\Admi
nistrator\.m2\repository\io\netty\netty-common\4.2.17.Final\netty-common-4.2.17.Final.jar;C:\Users\Administrator\.m2\re
pository\io\netty\netty-codec\4.2.17.Final\netty-codec-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\
netty-codec-base\4.2.17.Final\netty-codec-base-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-co
dec-compression\4.2.17.Final\netty-codec-compression-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\ne
tty-codec-protobuf\4.2.17.Final\netty-codec-protobuf-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\ne
tty-codec-marshalling\4.2.17.Final\netty-codec-marshalling-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\ne
tty\netty-buffer\4.2.17.Final\netty-buffer-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-transp
ort\4.2.17.Final\netty-transport-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-resolver\4.2.17.
Final\netty-resolver-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-resolver-dns\4.2.17.Final\ne
tty-resolver-dns-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-codec-dns\4.2.17.Final\netty-cod
ec-dns-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-handler\4.2.17.Final\netty-handler-4.2.17.
Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-transport-native-unix-common\4.2.17.Final\netty-transpor
t-native-unix-common-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\javax\cache\cache-api\1.1.1\cache-api-1.1.1
.jar;C:\Users\Administrator\.m2\repository\io\projectreactor\reactor-core\3.7.19\reactor-core-3.7.19.jar;C:\Users\Admin
istrator\.m2\repository\org\reactivestreams\reactive-streams\1.0.4\reactive-streams-1.0.4.jar;C:\Users\Administrator\.m
2\repository\io\reactivex\rxjava3\rxjava\3.1.12\rxjava-3.1.12.jar;C:\Users\Administrator\.m2\repository\com\esotericsof
tware\kryo\5.6.2\kryo-5.6.2.jar;C:\Users\Administrator\.m2\repository\com\esotericsoftware\reflectasm\1.11.9\reflectasm
-1.11.9.jar;C:\Users\Administrator\.m2\repository\com\esotericsoftware\minlog\1.3.1\minlog-1.3.1.jar;C:\Users\Administr
ator\.m2\repository\net\bytebuddy\byte-buddy\1.17.8\byte-buddy-1.17.8.jar;C:\Users\Administrator\.m2\repository\org\jod
d\jodd-util\6.3.0\jodd-util-6.3.0.jar;C:\Users\Administrator\.m2\repository\org\redisson\redisson-spring-data-35\4.7.0\
redisson-spring-data-35-4.7.0.jar;C:\Users\Administrator\.m2\repository\org\springframework\data\spring-data-redis\3.5.
12\spring-data-redis-3.5.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\data\spring-data-keyvalue\3.5
.12\spring-data-keyvalue-3.5.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\data\spring-data-commons\
3.5.12\spring-data-commons-3.5.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-tx\6.2.19\spring
-tx-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-oxm\6.2.19\spring-oxm-6.2.19.jar;C:\Use
rs\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-cache\3.5.15\spring-boot-starter-cache-3.5
.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-context-support\6.2.19\spring-context-support-
6.2.19.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\datatype\jackson-datatype-jsr310\2.21.4\jackson-
datatype-jsr310-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\core\jackson-annotations\2.21\ja
ckson-annotations-2.21.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\core\jackson-core\2.21.4\jackson
-core-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\core\jackson-databind\2.21.4\jackson-datab
ind-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\baomidou\lock4j-redisson-spring-boot-starter\2.2.7\lock4j-redi
sson-spring-boot-starter-2.2.7.jar;C:\Users\Administrator\.m2\repository\com\baomidou\lock4j-core\2.2.7\lock4j-core-2.2
.7.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-aop\3.5.15\spring-boot-starte
r-aop-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\aspectj\aspectjweaver\1.9.25.1\aspectjweaver-1.9.25.1.jar;E:
\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-test\target\classes;E:\����֮��AI���
�ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-mybatis\target\classes;C:\Users\Administrator\.m
2\repository\com\alibaba\druid-spring-boot-3-starter\1.2.28\druid-spring-boot-3-starter-1.2.28.jar;C:\Users\Administrat
or\.m2\repository\com\alibaba\druid\1.2.28\druid-1.2.28.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-
plus-spring-boot3-starter\3.5.17\mybatis-plus-spring-boot3-starter-3.5.17.jar;C:\Users\Administrator\.m2\repository\com
\baomidou\mybatis-plus\3.5.17\mybatis-plus-3.5.17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-c
ore\3.5.17\mybatis-plus-core-3.5.17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-annotation\3.5.
17\mybatis-plus-annotation-3.5.17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-spring\3.5.17\myb
atis-plus-spring-3.5.17.jar;C:\Users\Administrator\.m2\repository\org\mybatis\mybatis\3.5.19\mybatis-3.5.19.jar;C:\User
s\Administrator\.m2\repository\org\mybatis\mybatis-spring\3.0.5\mybatis-spring-3.0.5.jar;C:\Users\Administrator\.m2\rep
ository\com\baomidou\mybatis-plus-spring-boot-autoconfigure\3.5.17\mybatis-plus-spring-boot-autoconfigure-3.5.17.jar;C:
\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-spring-boot-native-image\3.5.17\mybatis-plus-spring-boot-
native-image-3.5.17.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-jdbc\3.5.15\
spring-boot-starter-jdbc-3.5.15.jar;C:\Users\Administrator\.m2\repository\com\zaxxer\HikariCP\6.3.3\HikariCP-6.3.3.jar;
C:\Users\Administrator\.m2\repository\org\springframework\spring-jdbc\6.2.19\spring-jdbc-6.2.19.jar;C:\Users\Administra
tor\.m2\repository\com\baomidou\mybatis-plus-jsqlparser\3.5.17\mybatis-plus-jsqlparser-3.5.17.jar;C:\Users\Administrato
r\.m2\repository\com\github\jsqlparser\jsqlparser\5.2\jsqlparser-5.2.jar;C:\Users\Administrator\.m2\repository\com\baom
idou\mybatis-plus-jsqlparser-common\3.5.17\mybatis-plus-jsqlparser-common-3.5.17.jar;C:\Users\Administrator\.m2\reposit
ory\com\baomidou\mybatis-plus-extension\3.5.17\mybatis-plus-extension-3.5.17.jar;C:\Users\Administrator\.m2\repository\
com\baomidou\dynamic-datasource-spring-boot3-starter\4.5.0\dynamic-datasource-spring-boot3-starter-4.5.0.jar;C:\Users\A
dministrator\.m2\repository\com\baomidou\dynamic-datasource-spring-boot-common\4.5.0\dynamic-datasource-spring-boot-com
mon-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\baomidou\dynamic-datasource-spring\4.5.0\dynamic-datasource-spr
ing-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\baomidou\dynamic-datasource-creator\4.5.0\dynamic-datasource-cr
eator-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\oracle\database\spring\oracle-spring-boot-starter-ucp\23.4.0\
oracle-spring-boot-starter-ucp-23.4.0.jar;C:\Users\Administrator\.m2\repository\com\oracle\database\jdbc\ojdbc11\23.7.0
.25.01\ojdbc11-23.7.0.25.01.jar;C:\Users\Administrator\.m2\repository\com\oracle\database\jdbc\ucp\23.7.0.25.01\ucp-23.
7.0.25.01.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-join-boot-starter\1.5.9\mybatis-p
lus-join-boot-starter-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-join-extension\
1.5.9\mybatis-plus-join-extension-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-joi
n-core\1.5.9\mybatis-plus-join-core-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-j
oin-annotation\1.5.9\mybatis-plus-join-annotation-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\
mybatis-plus-join-adapter-base\1.5.9\mybatis-plus-join-adapter-base-1.5.9.jar;C:\Users\Administrator\.m2\repository\com
\github\yulichang\mybatis-plus-join-adapter-jsqlparser\1.5.9\mybatis-plus-join-adapter-jsqlparser-1.5.9.jar;C:\Users\Ad
ministrator\.m2\repository\com\github\yulichang\mybatis-plus-join-wrapper-ext\1.5.9\mybatis-plus-join-wrapper-ext-1.5.9
.jar;C:\Users\Administrator\.m2\repository\org\dromara\easy-trans-spring-boot-starter\3.1.8\easy-trans-spring-boot-star
ter-3.1.8.jar;C:\Users\Administrator\.m2\repository\org\dromara\easy-trans-service\3.1.8\easy-trans-service-3.1.8.jar;C
:\Users\Administrator\.m2\repository\com\github\ben-manes\caffeine\caffeine\3.2.4\caffeine-3.2.4.jar;C:\Users\Administr
ator\.m2\repository\org\dromara\easy-trans-mybatis-plus-extend\3.1.8\easy-trans-mybatis-plus-extend-3.1.8.jar;C:\Users\
Administrator\.m2\repository\org\mockito\mockito-inline\5.2.0\mockito-inline-5.2.0.jar;C:\Users\Administrator\.m2\repos
itory\org\mockito\mockito-core\5.17.0\mockito-core-5.17.0.jar;C:\Users\Administrator\.m2\repository\net\bytebuddy\byte-
buddy-agent\1.17.8\byte-buddy-agent-1.17.8.jar;C:\Users\Administrator\.m2\repository\org\objenesis\objenesis\3.3\objene
sis-3.3.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-test\3.5.15\spring-boot-
starter-test-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-test\3.5.15\spring-b
oot-test-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-test-autoconfigure\3.5.1
5\spring-boot-test-autoconfigure-3.5.15.jar;C:\Users\Administrator\.m2\repository\com\jayway\jsonpath\json-path\2.9.0\j
son-path-2.9.0.jar;C:\Users\Administrator\.m2\repository\jakarta\xml\bind\jakarta.xml.bind-api\4.0.5\jakarta.xml.bind-a
pi-4.0.5.jar;C:\Users\Administrator\.m2\repository\jakarta\activation\jakarta.activation-api\2.1.4\jakarta.activation-a
pi-2.1.4.jar;C:\Users\Administrator\.m2\repository\net\minidev\json-smart\2.5.2\json-smart-2.5.2.jar;C:\Users\Administr
ator\.m2\repository\net\minidev\accessors-smart\2.5.2\accessors-smart-2.5.2.jar;C:\Users\Administrator\.m2\repository\o
rg\assertj\assertj-core\3.27.7\assertj-core-3.27.7.jar;C:\Users\Administrator\.m2\repository\org\awaitility\awaitility\
4.2.2\awaitility-4.2.2.jar;C:\Users\Administrator\.m2\repository\org\hamcrest\hamcrest\3.0\hamcrest-3.0.jar;C:\Users\Ad
ministrator\.m2\repository\org\junit\jupiter\junit-jupiter\5.12.2\junit-jupiter-5.12.2.jar;C:\Users\Administrator\.m2\r
epository\org\junit\jupiter\junit-jupiter-api\5.12.2\junit-jupiter-api-5.12.2.jar;C:\Users\Administrator\.m2\repository
\org\opentest4j\opentest4j\1.3.0\opentest4j-1.3.0.jar;C:\Users\Administrator\.m2\repository\org\junit\platform\junit-pl
atform-commons\1.12.2\junit-platform-commons-1.12.2.jar;C:\Users\Administrator\.m2\repository\org\apiguardian\apiguardi
an-api\1.1.2\apiguardian-api-1.1.2.jar;C:\Users\Administrator\.m2\repository\org\junit\jupiter\junit-jupiter-params\5.1
2.2\junit-jupiter-params-5.12.2.jar;C:\Users\Administrator\.m2\repository\org\junit\jupiter\junit-jupiter-engine\5.12.2
\junit-jupiter-engine-5.12.2.jar;C:\Users\Administrator\.m2\repository\org\junit\platform\junit-platform-engine\1.12.2\
junit-platform-engine-1.12.2.jar;C:\Users\Administrator\.m2\repository\org\mockito\mockito-junit-jupiter\5.17.0\mockito
-junit-jupiter-5.17.0.jar;C:\Users\Administrator\.m2\repository\org\skyscreamer\jsonassert\1.5.3\jsonassert-1.5.3.jar;C
:\Users\Administrator\.m2\repository\com\vaadin\external\google\android-json\0.0.20131108.vaadin1\android-json-0.0.2013
1108.vaadin1.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-core\6.2.19\spring-core-6.2.19.jar;C:
\Users\Administrator\.m2\repository\org\springframework\spring-jcl\6.2.19\spring-jcl-6.2.19.jar;C:\Users\Administrator\
.m2\repository\org\springframework\spring-test\6.2.19\spring-test-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\
xmlunit\xmlunit-core\2.10.4\xmlunit-core-2.10.4.jar;C:\Users\Administrator\.m2\repository\com\h2database\h2\2.3.232\h2-
2.3.232.jar;C:\Users\Administrator\.m2\repository\com\github\fppt\jedis-mock\1.1.18\jedis-mock-1.1.18.jar;C:\Users\Admi
nistrator\.m2\repository\org\slf4j\slf4j-api\2.0.18\slf4j-api-2.0.18.jar;C:\Users\Administrator\.m2\repository\org\luaj
\luaj-jse\3.0.1\luaj-jse-3.0.1.jar;C:\Users\Administrator\.m2\repository\redis\clients\jedis\6.0.0\jedis-6.0.0.jar;C:\U
sers\Administrator\.m2\repository\org\apache\commons\commons-pool2\2.12.1\commons-pool2-2.12.1.jar;C:\Users\Administrat
or\.m2\repository\org\json\json\20250107\json-20250107.jar;C:\Users\Administrator\.m2\repository\com\google\code\gson\g
son\2.13.2\gson-2.13.2.jar;C:\Users\Administrator\.m2\repository\com\google\errorprone\error_prone_annotations\2.41.0\e
rror_prone_annotations-2.41.0.jar;C:\Users\Administrator\.m2\repository\redis\clients\authentication\redis-authx-core\0
.1.1-beta2\redis-authx-core-0.1.1-beta2.jar;C:\Users\Administrator\.m2\repository\uk\co\jemos\podam\podam\8.0.2.RELEASE
\podam-8.0.2.RELEASE.jar;C:\Users\Administrator\.m2\repository\net\jcip\jcip-annotations\1.0\jcip-annotations-1.0.jar;C
:\Users\Administrator\.m2\repository\jakarta\validation\jakarta.validation-api\3.0.2\jakarta.validation-api-3.0.2.jar;C
:\Users\Administrator\.m2\repository\org\apache\commons\commons-lang3\3.20.0\commons-lang3-3.20.0.jar;"/>
    <property name="java.vm.vendor" value="Eclipse Adoptium"/>
    <property name="sun.arch.data.model" value="64"/>
    <property name="user.variant" value=""/>
    <property name="java.vendor.url" value="https://adoptium.net/"/>
    <property name="user.timezone" value="Asia/Shanghai"/>
    <property name="os.name" value="Windows 10"/>
    <property name="java.vm.specification.version" value="17"/>
    <property name="sun.java.launcher" value="SUN_STANDARD"/>
    <property name="user.country" value="CN"/>
    <property name="sun.boot.library.path" value="E:\����֮��AI����ƽ̨����\tools\jdk-17.0.20.1+1\bin"/>
    <property name="sun.java.command" value="C:\Users\Administrator\AppData\Local\Temp\surefire12053511136342888388\sur
efirebooter-20260912135129227_23.jar C:\Users\Administrator\AppData\Local\Temp\surefire12053511136342888388 2026-09-12T
13-51-22_032-jvmRun1 surefire-20260912135129227_21tmp surefire_4-20260912135129227_22tmp"/>
    <property name="jdk.debug" value="release"/>
    <property name="surefire.test.class.path" value="E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-
spring-boot-starter-protection\target\test-classes;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-sp
ring-boot-starter-protection\target\classes;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-bo
ot-starter-web\target\classes;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-common\target\classes;C
:\Users\Administrator\.m2\repository\io\opentelemetry\opentelemetry-api\1.65.0\opentelemetry-api-1.65.0.jar;C:\Users\Ad
ministrator\.m2\repository\io\opentelemetry\opentelemetry-context\1.49.0\opentelemetry-context-1.49.0.jar;C:\Users\Admi
nistrator\.m2\repository\org\projectlombok\lombok\1.18.46\lombok-1.18.46.jar;C:\Users\Administrator\.m2\repository\org\
mapstruct\mapstruct\1.6.3\mapstruct-1.6.3.jar;C:\Users\Administrator\.m2\repository\org\mapstruct\mapstruct-jdk8\1.6.3\
mapstruct-jdk8-1.6.3.jar;C:\Users\Administrator\.m2\repository\org\mapstruct\mapstruct-processor\1.6.3\mapstruct-proces
sor-1.6.3.jar;C:\Users\Administrator\.m2\repository\cn\hutool\hutool-all\5.8.47\hutool-all-5.8.47.jar;C:\Users\Administ
rator\.m2\repository\com\alibaba\transmittable-thread-local\2.14.5\transmittable-thread-local-2.14.5.jar;C:\Users\Admin
istrator\.m2\repository\com\alibaba\fastjson\2.0.64\fastjson-2.0.64.jar;C:\Users\Administrator\.m2\repository\com\aliba
ba\fastjson2\fastjson2-extension\2.0.64\fastjson2-extension-2.0.64.jar;C:\Users\Administrator\.m2\repository\com\alibab
a\fastjson2\fastjson2\2.0.64\fastjson2-2.0.64.jar;C:\Users\Administrator\.m2\repository\org\dromara\easy-trans-anno\3.1
.8\easy-trans-anno-3.1.8.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-web\3.5
.15\spring-boot-starter-web-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-start
er\3.5.15\spring-boot-starter-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot\3.5
.15\spring-boot-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-autoconfigure\3.5
.15\spring-boot-autoconfigure-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-sta
rter-logging\3.5.15\spring-boot-starter-logging-3.5.15.jar;C:\Users\Administrator\.m2\repository\ch\qos\logback\logback
-classic\1.5.34\logback-classic-1.5.34.jar;C:\Users\Administrator\.m2\repository\ch\qos\logback\logback-core\1.5.34\log
back-core-1.5.34.jar;C:\Users\Administrator\.m2\repository\org\apache\logging\log4j\log4j-to-slf4j\2.24.3\log4j-to-slf4
j-2.24.3.jar;C:\Users\Administrator\.m2\repository\org\apache\logging\log4j\log4j-api\2.24.3\log4j-api-2.24.3.jar;C:\Us
ers\Administrator\.m2\repository\org\slf4j\jul-to-slf4j\2.0.18\jul-to-slf4j-2.0.18.jar;C:\Users\Administrator\.m2\repos
itory\jakarta\annotation\jakarta.annotation-api\2.1.1\jakarta.annotation-api-2.1.1.jar;C:\Users\Administrator\.m2\repos
itory\org\yaml\snakeyaml\2.4\snakeyaml-2.4.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-bo
ot-starter-json\3.5.15\spring-boot-starter-json-3.5.15.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\
datatype\jackson-datatype-jdk8\2.21.4\jackson-datatype-jdk8-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\faster
xml\jackson\module\jackson-module-parameter-names\2.21.4\jackson-module-parameter-names-2.21.4.jar;C:\Users\Administrat
or\.m2\repository\org\springframework\boot\spring-boot-starter-tomcat\3.5.15\spring-boot-starter-tomcat-3.5.15.jar;C:\U
sers\Administrator\.m2\repository\org\apache\tomcat\embed\tomcat-embed-core\10.1.55\tomcat-embed-core-10.1.55.jar;C:\Us
ers\Administrator\.m2\repository\org\apache\tomcat\embed\tomcat-embed-el\10.1.55\tomcat-embed-el-10.1.55.jar;C:\Users\A
dministrator\.m2\repository\org\apache\tomcat\embed\tomcat-embed-websocket\10.1.55\tomcat-embed-websocket-10.1.55.jar;C
:\Users\Administrator\.m2\repository\org\springframework\spring-web\6.2.19\spring-web-6.2.19.jar;C:\Users\Administrator
\.m2\repository\org\springframework\spring-beans\6.2.19\spring-beans-6.2.19.jar;C:\Users\Administrator\.m2\repository\i
o\micrometer\micrometer-observation\1.15.12\micrometer-observation-1.15.12.jar;C:\Users\Administrator\.m2\repository\io
\micrometer\micrometer-commons\1.15.12\micrometer-commons-1.15.12.jar;C:\Users\Administrator\.m2\repository\org\springf
ramework\spring-webmvc\6.2.19\spring-webmvc-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring
-aop\6.2.19\spring-aop-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-context\6.2.19\sprin
g-context-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-expression\6.2.19\spring-expressi
on-6.2.19.jar;C:\Users\Administrator\.m2\repository\com\github\xiaoymin\knife4j-openapi3-jakarta-spring-boot-starter\4.
5.0\knife4j-openapi3-jakarta-spring-boot-starter-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\github\xiaoymin\kn
ife4j-core\4.5.0\knife4j-core-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\github\xiaoymin\knife4j-openapi3-ui\4
.5.0\knife4j-openapi3-ui-4.5.0.jar;C:\Users\Administrator\.m2\repository\org\springdoc\springdoc-openapi-starter-webmvc
-ui\2.8.17\springdoc-openapi-starter-webmvc-ui-2.8.17.jar;C:\Users\Administrator\.m2\repository\org\springdoc\springdoc
-openapi-starter-webmvc-api\2.8.17\springdoc-openapi-starter-webmvc-api-2.8.17.jar;C:\Users\Administrator\.m2\repositor
y\org\springdoc\springdoc-openapi-starter-common\2.8.17\springdoc-openapi-starter-common-2.8.17.jar;C:\Users\Administra
tor\.m2\repository\org\springframework\boot\spring-boot-starter-validation\3.5.15\spring-boot-starter-validation-3.5.15
.jar;C:\Users\Administrator\.m2\repository\org\hibernate\validator\hibernate-validator\8.0.3.Final\hibernate-validator-
8.0.3.Final.jar;C:\Users\Administrator\.m2\repository\org\jboss\logging\jboss-logging\3.6.3.Final\jboss-logging-3.6.3.F
inal.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\classmate\1.7.3\classmate-1.7.3.jar;C:\Users\Administrator
\.m2\repository\io\swagger\core\v3\swagger-core-jakarta\2.2.47\swagger-core-jakarta-2.2.47.jar;C:\Users\Administrator\.
m2\repository\io\swagger\core\v3\swagger-annotations-jakarta\2.2.47\swagger-annotations-jakarta-2.2.47.jar;C:\Users\Adm
inistrator\.m2\repository\io\swagger\core\v3\swagger-models-jakarta\2.2.47\swagger-models-jakarta-2.2.47.jar;C:\Users\A
dministrator\.m2\repository\com\fasterxml\jackson\dataformat\jackson-dataformat-yaml\2.21.4\jackson-dataformat-yaml-2.2
1.4.jar;C:\Users\Administrator\.m2\repository\org\webjars\swagger-ui\5.32.2\swagger-ui-5.32.2.jar;C:\Users\Administrato
r\.m2\repository\org\webjars\webjars-locator-lite\1.1.3\webjars-locator-lite-1.1.3.jar;C:\Users\Administrator\.m2\repos
itory\org\jspecify\jspecify\1.0.0\jspecify-1.0.0.jar;C:\Users\Administrator\.m2\repository\org\jsoup\jsoup\1.23.2\jsoup
-1.23.2.jar;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-redis\target\classes;
C:\Users\Administrator\.m2\repository\org\redisson\redisson-spring-boot-starter\4.7.0\redisson-spring-boot-starter-4.7.
0.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-data-redis\3.5.15\spring-boot-
starter-data-redis-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\redisson\redisson\4.7.0\redisson-4.7.0.jar;C:\U
sers\Administrator\.m2\repository\io\netty\netty-common\4.2.17.Final\netty-common-4.2.17.Final.jar;C:\Users\Administrat
or\.m2\repository\io\netty\netty-codec\4.2.17.Final\netty-codec-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\
io\netty\netty-codec-base\4.2.17.Final\netty-codec-base-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty
\netty-codec-compression\4.2.17.Final\netty-codec-compression-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io
\netty\netty-codec-protobuf\4.2.17.Final\netty-codec-protobuf-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io
\netty\netty-codec-marshalling\4.2.17.Final\netty-codec-marshalling-4.2.17.Final.jar;C:\Users\Administrator\.m2\reposit
ory\io\netty\netty-buffer\4.2.17.Final\netty-buffer-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\net
ty-transport\4.2.17.Final\netty-transport-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-resolve
r\4.2.17.Final\netty-resolver-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-resolver-dns\4.2.17
.Final\netty-resolver-dns-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-codec-dns\4.2.17.Final\
netty-codec-dns-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-handler\4.2.17.Final\netty-handle
r-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-transport-native-unix-common\4.2.17.Final\netty
-transport-native-unix-common-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\javax\cache\cache-api\1.1.1\cache-
api-1.1.1.jar;C:\Users\Administrator\.m2\repository\io\projectreactor\reactor-core\3.7.19\reactor-core-3.7.19.jar;C:\Us
ers\Administrator\.m2\repository\org\reactivestreams\reactive-streams\1.0.4\reactive-streams-1.0.4.jar;C:\Users\Adminis
trator\.m2\repository\io\reactivex\rxjava3\rxjava\3.1.12\rxjava-3.1.12.jar;C:\Users\Administrator\.m2\repository\com\es
otericsoftware\kryo\5.6.2\kryo-5.6.2.jar;C:\Users\Administrator\.m2\repository\com\esotericsoftware\reflectasm\1.11.9\r
eflectasm-1.11.9.jar;C:\Users\Administrator\.m2\repository\com\esotericsoftware\minlog\1.3.1\minlog-1.3.1.jar;C:\Users\
Administrator\.m2\repository\net\bytebuddy\byte-buddy\1.17.8\byte-buddy-1.17.8.jar;C:\Users\Administrator\.m2\repositor
y\org\jodd\jodd-util\6.3.0\jodd-util-6.3.0.jar;C:\Users\Administrator\.m2\repository\org\redisson\redisson-spring-data-
35\4.7.0\redisson-spring-data-35-4.7.0.jar;C:\Users\Administrator\.m2\repository\org\springframework\data\spring-data-r
edis\3.5.12\spring-data-redis-3.5.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\data\spring-data-key
value\3.5.12\spring-data-keyvalue-3.5.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\data\spring-data
-commons\3.5.12\spring-data-commons-3.5.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-tx\6.2.
19\spring-tx-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-oxm\6.2.19\spring-oxm-6.2.19.j
ar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-cache\3.5.15\spring-boot-starter-
cache-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-context-support\6.2.19\spring-context
-support-6.2.19.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\datatype\jackson-datatype-jsr310\2.21.4
\jackson-datatype-jsr310-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\core\jackson-annotation
s\2.21\jackson-annotations-2.21.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\core\jackson-core\2.21.
4\jackson-core-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\core\jackson-databind\2.21.4\jack
son-databind-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\baomidou\lock4j-redisson-spring-boot-starter\2.2.7\lo
ck4j-redisson-spring-boot-starter-2.2.7.jar;C:\Users\Administrator\.m2\repository\com\baomidou\lock4j-core\2.2.7\lock4j
-core-2.2.7.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-aop\3.5.15\spring-bo
ot-starter-aop-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\aspectj\aspectjweaver\1.9.25.1\aspectjweaver-1.9.25
.1.jar;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-test\target\classes;E:\���
�֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-mybatis\target\classes;C:\Users\Adminis
trator\.m2\repository\com\alibaba\druid-spring-boot-3-starter\1.2.28\druid-spring-boot-3-starter-1.2.28.jar;C:\Users\Ad
ministrator\.m2\repository\com\alibaba\druid\1.2.28\druid-1.2.28.jar;C:\Users\Administrator\.m2\repository\com\baomidou
\mybatis-plus-spring-boot3-starter\3.5.17\mybatis-plus-spring-boot3-starter-3.5.17.jar;C:\Users\Administrator\.m2\repos
itory\com\baomidou\mybatis-plus\3.5.17\mybatis-plus-3.5.17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybat
is-plus-core\3.5.17\mybatis-plus-core-3.5.17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-annota
tion\3.5.17\mybatis-plus-annotation-3.5.17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-spring\3
.5.17\mybatis-plus-spring-3.5.17.jar;C:\Users\Administrator\.m2\repository\org\mybatis\mybatis\3.5.19\mybatis-3.5.19.ja
r;C:\Users\Administrator\.m2\repository\org\mybatis\mybatis-spring\3.0.5\mybatis-spring-3.0.5.jar;C:\Users\Administrato
r\.m2\repository\com\baomidou\mybatis-plus-spring-boot-autoconfigure\3.5.17\mybatis-plus-spring-boot-autoconfigure-3.5.
17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-spring-boot-native-image\3.5.17\mybatis-plus-spr
ing-boot-native-image-3.5.17.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-jdb
c\3.5.15\spring-boot-starter-jdbc-3.5.15.jar;C:\Users\Administrator\.m2\repository\com\zaxxer\HikariCP\6.3.3\HikariCP-6
.3.3.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-jdbc\6.2.19\spring-jdbc-6.2.19.jar;C:\Users\A
dministrator\.m2\repository\com\baomidou\mybatis-plus-jsqlparser\3.5.17\mybatis-plus-jsqlparser-3.5.17.jar;C:\Users\Adm
inistrator\.m2\repository\com\github\jsqlparser\jsqlparser\5.2\jsqlparser-5.2.jar;C:\Users\Administrator\.m2\repository
\com\baomidou\mybatis-plus-jsqlparser-common\3.5.17\mybatis-plus-jsqlparser-common-3.5.17.jar;C:\Users\Administrator\.m
2\repository\com\baomidou\mybatis-plus-extension\3.5.17\mybatis-plus-extension-3.5.17.jar;C:\Users\Administrator\.m2\re
pository\com\baomidou\dynamic-datasource-spring-boot3-starter\4.5.0\dynamic-datasource-spring-boot3-starter-4.5.0.jar;C
:\Users\Administrator\.m2\repository\com\baomidou\dynamic-datasource-spring-boot-common\4.5.0\dynamic-datasource-spring
-boot-common-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\baomidou\dynamic-datasource-spring\4.5.0\dynamic-datas
ource-spring-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\baomidou\dynamic-datasource-creator\4.5.0\dynamic-data
source-creator-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\oracle\database\spring\oracle-spring-boot-starter-uc
p\23.4.0\oracle-spring-boot-starter-ucp-23.4.0.jar;C:\Users\Administrator\.m2\repository\com\oracle\database\jdbc\ojdbc
11\23.7.0.25.01\ojdbc11-23.7.0.25.01.jar;C:\Users\Administrator\.m2\repository\com\oracle\database\jdbc\ucp\23.7.0.25.0
1\ucp-23.7.0.25.01.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-join-boot-starter\1.5.9\
mybatis-plus-join-boot-starter-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-join-e
xtension\1.5.9\mybatis-plus-join-extension-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis
-plus-join-core\1.5.9\mybatis-plus-join-core-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybat
is-plus-join-annotation\1.5.9\mybatis-plus-join-annotation-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\y
ulichang\mybatis-plus-join-adapter-base\1.5.9\mybatis-plus-join-adapter-base-1.5.9.jar;C:\Users\Administrator\.m2\repos
itory\com\github\yulichang\mybatis-plus-join-adapter-jsqlparser\1.5.9\mybatis-plus-join-adapter-jsqlparser-1.5.9.jar;C:
\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-join-wrapper-ext\1.5.9\mybatis-plus-join-wrapper-
ext-1.5.9.jar;C:\Users\Administrator\.m2\repository\org\dromara\easy-trans-spring-boot-starter\3.1.8\easy-trans-spring-
boot-starter-3.1.8.jar;C:\Users\Administrator\.m2\repository\org\dromara\easy-trans-service\3.1.8\easy-trans-service-3.
1.8.jar;C:\Users\Administrator\.m2\repository\com\github\ben-manes\caffeine\caffeine\3.2.4\caffeine-3.2.4.jar;C:\Users\
Administrator\.m2\repository\org\dromara\easy-trans-mybatis-plus-extend\3.1.8\easy-trans-mybatis-plus-extend-3.1.8.jar;
C:\Users\Administrator\.m2\repository\org\mockito\mockito-inline\5.2.0\mockito-inline-5.2.0.jar;C:\Users\Administrator\
.m2\repository\org\mockito\mockito-core\5.17.0\mockito-core-5.17.0.jar;C:\Users\Administrator\.m2\repository\net\bytebu
ddy\byte-buddy-agent\1.17.8\byte-buddy-agent-1.17.8.jar;C:\Users\Administrator\.m2\repository\org\objenesis\objenesis\3
.3\objenesis-3.3.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-test\3.5.15\spr
ing-boot-starter-test-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-test\3.5.15
\spring-boot-test-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-test-autoconfig
ure\3.5.15\spring-boot-test-autoconfigure-3.5.15.jar;C:\Users\Administrator\.m2\repository\com\jayway\jsonpath\json-pat
h\2.9.0\json-path-2.9.0.jar;C:\Users\Administrator\.m2\repository\jakarta\xml\bind\jakarta.xml.bind-api\4.0.5\jakarta.x
ml.bind-api-4.0.5.jar;C:\Users\Administrator\.m2\repository\jakarta\activation\jakarta.activation-api\2.1.4\jakarta.act
ivation-api-2.1.4.jar;C:\Users\Administrator\.m2\repository\net\minidev\json-smart\2.5.2\json-smart-2.5.2.jar;C:\Users\
Administrator\.m2\repository\net\minidev\accessors-smart\2.5.2\accessors-smart-2.5.2.jar;C:\Users\Administrator\.m2\rep
ository\org\assertj\assertj-core\3.27.7\assertj-core-3.27.7.jar;C:\Users\Administrator\.m2\repository\org\awaitility\aw
aitility\4.2.2\awaitility-4.2.2.jar;C:\Users\Administrator\.m2\repository\org\hamcrest\hamcrest\3.0\hamcrest-3.0.jar;C:
\Users\Administrator\.m2\repository\org\junit\jupiter\junit-jupiter\5.12.2\junit-jupiter-5.12.2.jar;C:\Users\Administra
tor\.m2\repository\org\junit\jupiter\junit-jupiter-api\5.12.2\junit-jupiter-api-5.12.2.jar;C:\Users\Administrator\.m2\r
epository\org\opentest4j\opentest4j\1.3.0\opentest4j-1.3.0.jar;C:\Users\Administrator\.m2\repository\org\junit\platform
\junit-platform-commons\1.12.2\junit-platform-commons-1.12.2.jar;C:\Users\Administrator\.m2\repository\org\apiguardian\
apiguardian-api\1.1.2\apiguardian-api-1.1.2.jar;C:\Users\Administrator\.m2\repository\org\junit\jupiter\junit-jupiter-p
arams\5.12.2\junit-jupiter-params-5.12.2.jar;C:\Users\Administrator\.m2\repository\org\junit\jupiter\junit-jupiter-engi
ne\5.12.2\junit-jupiter-engine-5.12.2.jar;C:\Users\Administrator\.m2\repository\org\junit\platform\junit-platform-engin
e\1.12.2\junit-platform-engine-1.12.2.jar;C:\Users\Administrator\.m2\repository\org\mockito\mockito-junit-jupiter\5.17.
0\mockito-junit-jupiter-5.17.0.jar;C:\Users\Administrator\.m2\repository\org\skyscreamer\jsonassert\1.5.3\jsonassert-1.
5.3.jar;C:\Users\Administrator\.m2\repository\com\vaadin\external\google\android-json\0.0.20131108.vaadin1\android-json
-0.0.20131108.vaadin1.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-core\6.2.19\spring-core-6.2.
19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-jcl\6.2.19\spring-jcl-6.2.19.jar;C:\Users\Admin
istrator\.m2\repository\org\springframework\spring-test\6.2.19\spring-test-6.2.19.jar;C:\Users\Administrator\.m2\reposi
tory\org\xmlunit\xmlunit-core\2.10.4\xmlunit-core-2.10.4.jar;C:\Users\Administrator\.m2\repository\com\h2database\h2\2.
3.232\h2-2.3.232.jar;C:\Users\Administrator\.m2\repository\com\github\fppt\jedis-mock\1.1.18\jedis-mock-1.1.18.jar;C:\U
sers\Administrator\.m2\repository\org\slf4j\slf4j-api\2.0.18\slf4j-api-2.0.18.jar;C:\Users\Administrator\.m2\repository
\org\luaj\luaj-jse\3.0.1\luaj-jse-3.0.1.jar;C:\Users\Administrator\.m2\repository\redis\clients\jedis\6.0.0\jedis-6.0.0
.jar;C:\Users\Administrator\.m2\repository\org\apache\commons\commons-pool2\2.12.1\commons-pool2-2.12.1.jar;C:\Users\Ad
ministrator\.m2\repository\org\json\json\20250107\json-20250107.jar;C:\Users\Administrator\.m2\repository\com\google\co
de\gson\gson\2.13.2\gson-2.13.2.jar;C:\Users\Administrator\.m2\repository\com\google\errorprone\error_prone_annotations
\2.41.0\error_prone_annotations-2.41.0.jar;C:\Users\Administrator\.m2\repository\redis\clients\authentication\redis-aut
hx-core\0.1.1-beta2\redis-authx-core-0.1.1-beta2.jar;C:\Users\Administrator\.m2\repository\uk\co\jemos\podam\podam\8.0.
2.RELEASE\podam-8.0.2.RELEASE.jar;C:\Users\Administrator\.m2\repository\net\jcip\jcip-annotations\1.0\jcip-annotations-
1.0.jar;C:\Users\Administrator\.m2\repository\jakarta\validation\jakarta.validation-api\3.0.2\jakarta.validation-api-3.
0.2.jar;C:\Users\Administrator\.m2\repository\org\apache\commons\commons-lang3\3.20.0\commons-lang3-3.20.0.jar;"/>
    <property name="sun.cpu.endian" value="little"/>
    <property name="user.home" value="C:\Users\Administrator"/>
    <property name="user.language" value="zh"/>
    <property name="java.specification.vendor" value="Oracle Corporation"/>
    <property name="java.version.date" value="2026-08-18"/>
    <property name="java.home" value="E:\����֮��AI����ƽ̨����\tools\jdk-17.0.20.1+1"/>
    <property name="file.separator" value="\"/>
    <property name="basedir" value="E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-start
er-protection"/>
    <property name="java.vm.compressedOopsMode" value="Zero based"/>
    <property name="line.separator" value="&#10;"/>
    <property name="java.vm.specification.vendor" value="Oracle Corporation"/>
    <property name="java.specification.name" value="Java Platform API Specification"/>
    <property name="surefire.real.class.path" value="C:\Users\Administrator\AppData\Local\Temp\surefire1205351113634288
8388\surefirebooter-20260912135129227_23.jar"/>
    <property name="user.script" value=""/>
    <property name="sun.management.compiler" value="HotSpot 64-Bit Tiered Compilers"/>
    <property name="java.runtime.version" value="17.0.20.1+1"/>
    <property name="user.name" value="Administrator"/>
    <property name="path.separator" value=";"/>
    <property name="os.version" value="10.0"/>
    <property name="java.runtime.name" value="OpenJDK Runtime Environment"/>
    <property name="file.encoding" value="GBK"/>
    <property name="java.vm.name" value="OpenJDK 64-Bit Server VM"/>
    <property name="java.vendor.version" value="Temurin-17.0.20.1+1"/>
    <property name="localRepository" value="C:\Users\Administrator\.m2\repository"/>
    <property name="java.vendor.url.bug" value="https://github.com/adoptium/adoptium-support/issues"/>
    <property name="java.io.tmpdir" value="C:\Users\ADMINI~1\AppData\Local\Temp\"/>
    <property name="java.version" value="17.0.20.1"/>
    <property name="user.dir" value="E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-star
ter-protection"/>
    <property name="os.arch" value="amd64"/>
    <property name="java.vm.specification.name" value="Java Virtual Machine Specification"/>
    <property name="sun.os.patch.level" value=""/>
    <property name="native.encoding" value="GBK"/>
    <property name="java.library.path" value="E:\����֮��AI����ƽ̨����\tools\jdk-17.0.20.1+1\bin;C:\windows\Sun\Java\bin;
C:\windows\system32;C:\windows;E:\����֮��AI����ƽ̨����\tools\jdk-17.0.20.1+1\bin;E:\����֮��AI����ƽ̨����\tools\apache-mav
en-3.9.9\bin;C:\Program Files\Python311\Scripts\;C:\Program Files\Python311\;C:\windows\system32;C:\windows;C:\windows\
System32\Wbem;C:\windows\System32\WindowsPowerShell\v1.0\;C:\windows\System32\OpenSSH\;D:\Program Files (x86)\Tencent\΢
��web�����߹���;;C:\Program Files\Git\cmd;C:\Program Files\nodejs\;C:\Users\Administrator\AppData\Local\Microsoft\Window
sApps;;C:\Users\Administrator\AppData\Local\Programs\DockerDesktop\resources\bin;C:\Users\Administrator\AppData\Roaming
\npm;D:\Program Files\Qoder IDE\bin;C:\Users\Administrator\AppData\Local\Microsoft\WinGet\Links;;."/>
    <property name="java.vm.info" value="mixed mode, sharing"/>
    <property name="java.vendor" value="Eclipse Adoptium"/>
    <property name="java.vm.version" value="17.0.20.1+1"/>
    <property name="java.specification.maintenance.version" value="1"/>
    <property name="sun.io.unicode.encoding" value="UnicodeLittle"/>
    <property name="java.class.version" value="61.0"/>
  </properties>
  <testcase name="testAroundPointCut_firstRequest_proceedsWithoutRejectLog" classname="cn.zszj.framework.idempotent.cor
e.aop.IdempotentAspectTest" time="0.817"/>
  <testcase name="testAroundPointCut_legacyEmptyDigest_shouldTreatAsRepeatNotConflict" classname="cn.zszj.framework.ide
mpotent.core.aop.IdempotentAspectTest" time="0.169">
    <system-out><![CDATA[13:51:30.631 [main] INFO cn.zszj.framework.idempotent.core.aop.IdempotentAspect -- [aroundPoin
tCut][����(UserService.createOrder(..)) ����(["argA"]) ͬ��ͬ���ظ�]
]]></system-out>
  </testcase>
  <testcase name="testAroundPointCut_sameKeyDifferentArgs_shouldThrowConflict" classname="cn.zszj.framework.idempotent.
core.aop.IdempotentAspectTest" time="0.018">
    <system-out><![CDATA[13:51:30.644 [main] WARN cn.zszj.framework.idempotent.core.aop.IdempotentAspect -- [aroundPoin
tCut][����(UserService.createOrder(..)) ����(["argB"]) ͬ����γ�ͻ]
]]></system-out>
  </testcase>
  <testcase name="testAroundPointCut_realDefaultResolver_sameKeyForcesRepeatNotConflict" classname="cn.zszj.framework.i
dempotent.core.aop.IdempotentAspectTest" time="0.128">
    <system-out><![CDATA[13:51:30.784 [main] INFO cn.zszj.framework.idempotent.core.aop.IdempotentAspect -- [aroundPoin
tCut][����(UserService.createOrder(..)) ����(["argA"]) ͬ��ͬ���ظ�]
]]></system-out>
  </testcase>
  <testcase name="testAroundPointCut_getDigestThrows_shouldStillRejectAsRepeatNot500" classname="cn.zszj.framework.idem
potent.core.aop.IdempotentAspectTest" time="0.01">
    <system-out><![CDATA[13:51:30.792 [main] WARN cn.zszj.framework.idempotent.core.aop.IdempotentAspect -- [aroundPoin
tCut][�ݵȼ�(idem-fixed-key) ����ժҪʧ�ܣ��������ظ�����]
java.lang.RuntimeException: redis down
	at cn.zszj.framework.idempotent.core.redis.IdempotentRedisDAO.getDigest(IdempotentRedisDAO.java:50)
	at cn.zszj.framework.idempotent.core.aop.IdempotentAspect.aroundPointCut(IdempotentAspect.java:90)
	at cn.zszj.framework.idempotent.core.aop.IdempotentAspectTest.lambda$testAroundPointCut_getDigestThrows_shouldStillRej
ectAsRepeatNot500$3(IdempotentAspectTest.java:235)
	at org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:53)
	at org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:35)
	at org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3128)
	at cn.zszj.framework.idempotent.core.aop.IdempotentAspectTest.testAroundPointCut_getDigestThrows_shouldStillRejectAsRe
peatNot500(IdempotentAspectTest.java:234)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.base/java.lang.reflect.Method.invoke(Method.java:569)
	at org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:775)
	at org.junit.platform.commons.support.ReflectionSupport.invokeMethod(ReflectionSupport.java:479)
	at org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
	at org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorCha
in.java:131)
	at org.junit.jupiter.engine.extension.TimeoutExtension.intercept(TimeoutExtension.java:161)
	at org.junit.jupiter.engine.extension.TimeoutExtension.interceptTestableMethod(TimeoutExtension.java:152)
	at org.junit.jupiter.engine.extension.TimeoutExtension.interceptTestMethod(TimeoutExtension.java:91)
	at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker$ReflectiveInterceptorCall.lambda$ofVoidMethod$0(In
terceptingExecutableInvoker.java:112)
	at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.lambda$invoke$0(InterceptingExecutableInvoker.java
:94)
	at org.junit.jupiter.engine.execution.InvocationInterceptorChain$InterceptedInvocation.proceed(InvocationInterceptorCh
ain.java:106)
	at org.junit.jupiter.engine.execution.InvocationInterceptorChain.proceed(InvocationInterceptorChain.java:64)
	at org.junit.jupiter.engine.execution.InvocationInterceptorChain.chainAndInvoke(InvocationInterceptorChain.java:45)
	at org.junit.jupiter.engine.execution.InvocationInterceptorChain.invoke(InvocationInterceptorChain.java:37)
	at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.invoke(InterceptingExecutableInvoker.java:93)
	at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.invoke(InterceptingExecutableInvoker.java:87)
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$invokeTestMethod$7(TestMethodTestDescriptor.jav
a:216)
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.invokeTestMethod(TestMethodTestDescriptor.java:212)
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.execute(TestMethodTestDescriptor.java:137)
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.execute(TestMethodTestDescriptor.java:69)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:156)
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:146)
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:144)
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:143)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:100)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1511)
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierar
chicalTestExecutorService.java:41)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:160)
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:146)
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:144)
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:143)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:100)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1511)
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierar
chicalTestExecutorService.java:41)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:160)
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:146)
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:144)
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:143)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:100)
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchi
calTestExecutorService.java:35)
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57)
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54)
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:201)
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:170)
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:94)
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:59)
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.jav
a:142)
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:58)
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103)
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85)
	at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47)
	at org.junit.platform.launcher.core.InterceptingLauncher.lambda$execute$1(InterceptingLauncher.java:39)
	at org.junit.platform.launcher.core.ClasspathAlignmentCheckingLauncherInterceptor.intercept(ClasspathAlignmentChecking
LauncherInterceptor.java:25)
	at org.junit.platform.launcher.core.InterceptingLauncher.execute(InterceptingLauncher.java:38)
	at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47)
	at org.apache.maven.surefire.junitplatform.LazyLauncher.execute(LazyLauncher.java:56)
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.execute(JUnitPlatformProvider.java:194)
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invokeAllTests(JUnitPlatformProvider.java:150)
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invoke(JUnitPlatformProvider.java:124)
	at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:385)
	at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:162)
	at org.apache.maven.surefire.booter.ForkedBooter.run(ForkedBooter.java:507)
	at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:495)
13:51:30.795 [main] INFO cn.zszj.framework.idempotent.core.aop.IdempotentAspect -- [aroundPointCut][����(UserService.cr
eateOrder(..)) ����(["argA"]) ͬ��ͬ���ظ�]
]]></system-out>
  </testcase>
  <testcase name="testAroundPointCut_repeatedRequest_argsSanitizedInLog" classname="cn.zszj.framework.idempotent.core.a
op.IdempotentAspectTest" time="0.033">
    <system-out><![CDATA[13:51:30.828 [main] INFO cn.zszj.framework.idempotent.core.aop.IdempotentAspect -- [aroundPoin
tCut][����(UserService.createOrder(..)) ����([{"username":"zhangsan-user","password":"***","token":"***","nested":{"api
Key":"***","note":"hello-note"},"list":[{"secret":"***"}]}]) ͬ��ͬ���ظ�]
]]></system-out>
  </testcase>
  <testcase name="testAroundPointCut_sameKeySameArgs_shouldThrowRepeat" classname="cn.zszj.framework.idempotent.core.ao
p.IdempotentAspectTest" time="0.004">
    <system-out><![CDATA[13:51:30.834 [main] INFO cn.zszj.framework.idempotent.core.aop.IdempotentAspect -- [aroundPoin
tCut][����(UserService.createOrder(..)) ����(["argA"]) ͬ��ͬ���ظ�]
]]></system-out>
  </testcase>
  <testcase name="testAroundPointCut_firstRequest_servletArgsExcludedFromDigest" classname="cn.zszj.framework.idempoten
t.core.aop.IdempotentAspectTest" time="0.058"/>
</testsuite>
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\TEST-cn.zszj.framework.idempotent.core.keyresolver.impl.DefaultIdempotentKeyResolverTest.xml
<?xml version="1.0" encoding="UTF-8"?>
<testsuite xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="https://maven.apache.or
g/surefire/maven-surefire-plugin/xsd/surefire-test-report.xsd" version="3.0.2" name="cn.zszj.framework.idempotent.core.
keyresolver.impl.DefaultIdempotentKeyResolverTest" time="0.045" tests="7" errors="0" skipped="0" failures="0">
  <properties>
    <property name="java.specification.version" value="17"/>
    <property name="sun.cpu.isalist" value="amd64"/>
    <property name="sun.jnu.encoding" value="GBK"/>
    <property name="java.class.path" value="E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-bo
ot-starter-protection\target\test-classes;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot
-starter-protection\target\classes;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starte
r-web\target\classes;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-common\target\classes;C:\Users\A
dministrator\.m2\repository\io\opentelemetry\opentelemetry-api\1.65.0\opentelemetry-api-1.65.0.jar;C:\Users\Administrat
or\.m2\repository\io\opentelemetry\opentelemetry-context\1.49.0\opentelemetry-context-1.49.0.jar;C:\Users\Administrator
\.m2\repository\org\projectlombok\lombok\1.18.46\lombok-1.18.46.jar;C:\Users\Administrator\.m2\repository\org\mapstruct
\mapstruct\1.6.3\mapstruct-1.6.3.jar;C:\Users\Administrator\.m2\repository\org\mapstruct\mapstruct-jdk8\1.6.3\mapstruct
-jdk8-1.6.3.jar;C:\Users\Administrator\.m2\repository\org\mapstruct\mapstruct-processor\1.6.3\mapstruct-processor-1.6.3
.jar;C:\Users\Administrator\.m2\repository\cn\hutool\hutool-all\5.8.47\hutool-all-5.8.47.jar;C:\Users\Administrator\.m2
\repository\com\alibaba\transmittable-thread-local\2.14.5\transmittable-thread-local-2.14.5.jar;C:\Users\Administrator\
.m2\repository\com\alibaba\fastjson\2.0.64\fastjson-2.0.64.jar;C:\Users\Administrator\.m2\repository\com\alibaba\fastjs
on2\fastjson2-extension\2.0.64\fastjson2-extension-2.0.64.jar;C:\Users\Administrator\.m2\repository\com\alibaba\fastjso
n2\fastjson2\2.0.64\fastjson2-2.0.64.jar;C:\Users\Administrator\.m2\repository\org\dromara\easy-trans-anno\3.1.8\easy-t
rans-anno-3.1.8.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-web\3.5.15\sprin
g-boot-starter-web-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter\3.5.15
\spring-boot-starter-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot\3.5.15\sprin
g-boot-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-autoconfigure\3.5.15\sprin
g-boot-autoconfigure-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-logg
ing\3.5.15\spring-boot-starter-logging-3.5.15.jar;C:\Users\Administrator\.m2\repository\ch\qos\logback\logback-classic\
1.5.34\logback-classic-1.5.34.jar;C:\Users\Administrator\.m2\repository\ch\qos\logback\logback-core\1.5.34\logback-core
-1.5.34.jar;C:\Users\Administrator\.m2\repository\org\apache\logging\log4j\log4j-to-slf4j\2.24.3\log4j-to-slf4j-2.24.3.
jar;C:\Users\Administrator\.m2\repository\org\apache\logging\log4j\log4j-api\2.24.3\log4j-api-2.24.3.jar;C:\Users\Admin
istrator\.m2\repository\org\slf4j\jul-to-slf4j\2.0.18\jul-to-slf4j-2.0.18.jar;C:\Users\Administrator\.m2\repository\jak
arta\annotation\jakarta.annotation-api\2.1.1\jakarta.annotation-api-2.1.1.jar;C:\Users\Administrator\.m2\repository\org
\yaml\snakeyaml\2.4\snakeyaml-2.4.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starte
r-json\3.5.15\spring-boot-starter-json-3.5.15.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\datatype\
jackson-datatype-jdk8\2.21.4\jackson-datatype-jdk8-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jacks
on\module\jackson-module-parameter-names\2.21.4\jackson-module-parameter-names-2.21.4.jar;C:\Users\Administrator\.m2\re
pository\org\springframework\boot\spring-boot-starter-tomcat\3.5.15\spring-boot-starter-tomcat-3.5.15.jar;C:\Users\Admi
nistrator\.m2\repository\org\apache\tomcat\embed\tomcat-embed-core\10.1.55\tomcat-embed-core-10.1.55.jar;C:\Users\Admin
istrator\.m2\repository\org\apache\tomcat\embed\tomcat-embed-el\10.1.55\tomcat-embed-el-10.1.55.jar;C:\Users\Administra
tor\.m2\repository\org\apache\tomcat\embed\tomcat-embed-websocket\10.1.55\tomcat-embed-websocket-10.1.55.jar;C:\Users\A
dministrator\.m2\repository\org\springframework\spring-web\6.2.19\spring-web-6.2.19.jar;C:\Users\Administrator\.m2\repo
sitory\org\springframework\spring-beans\6.2.19\spring-beans-6.2.19.jar;C:\Users\Administrator\.m2\repository\io\microme
ter\micrometer-observation\1.15.12\micrometer-observation-1.15.12.jar;C:\Users\Administrator\.m2\repository\io\micromet
er\micrometer-commons\1.15.12\micrometer-commons-1.15.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\
spring-webmvc\6.2.19\spring-webmvc-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-aop\6.2.
19\spring-aop-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-context\6.2.19\spring-context
-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-expression\6.2.19\spring-expression-6.2.19
.jar;C:\Users\Administrator\.m2\repository\com\github\xiaoymin\knife4j-openapi3-jakarta-spring-boot-starter\4.5.0\knife
4j-openapi3-jakarta-spring-boot-starter-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\github\xiaoymin\knife4j-cor
e\4.5.0\knife4j-core-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\github\xiaoymin\knife4j-openapi3-ui\4.5.0\knif
e4j-openapi3-ui-4.5.0.jar;C:\Users\Administrator\.m2\repository\org\springdoc\springdoc-openapi-starter-webmvc-ui\2.8.1
7\springdoc-openapi-starter-webmvc-ui-2.8.17.jar;C:\Users\Administrator\.m2\repository\org\springdoc\springdoc-openapi-
starter-webmvc-api\2.8.17\springdoc-openapi-starter-webmvc-api-2.8.17.jar;C:\Users\Administrator\.m2\repository\org\spr
ingdoc\springdoc-openapi-starter-common\2.8.17\springdoc-openapi-starter-common-2.8.17.jar;C:\Users\Administrator\.m2\r
epository\org\springframework\boot\spring-boot-starter-validation\3.5.15\spring-boot-starter-validation-3.5.15.jar;C:\U
sers\Administrator\.m2\repository\org\hibernate\validator\hibernate-validator\8.0.3.Final\hibernate-validator-8.0.3.Fin
al.jar;C:\Users\Administrator\.m2\repository\org\jboss\logging\jboss-logging\3.6.3.Final\jboss-logging-3.6.3.Final.jar;
C:\Users\Administrator\.m2\repository\com\fasterxml\classmate\1.7.3\classmate-1.7.3.jar;C:\Users\Administrator\.m2\repo
sitory\io\swagger\core\v3\swagger-core-jakarta\2.2.47\swagger-core-jakarta-2.2.47.jar;C:\Users\Administrator\.m2\reposi
tory\io\swagger\core\v3\swagger-annotations-jakarta\2.2.47\swagger-annotations-jakarta-2.2.47.jar;C:\Users\Administrato
r\.m2\repository\io\swagger\core\v3\swagger-models-jakarta\2.2.47\swagger-models-jakarta-2.2.47.jar;C:\Users\Administra
tor\.m2\repository\com\fasterxml\jackson\dataformat\jackson-dataformat-yaml\2.21.4\jackson-dataformat-yaml-2.21.4.jar;C
:\Users\Administrator\.m2\repository\org\webjars\swagger-ui\5.32.2\swagger-ui-5.32.2.jar;C:\Users\Administrator\.m2\rep
ository\org\webjars\webjars-locator-lite\1.1.3\webjars-locator-lite-1.1.3.jar;C:\Users\Administrator\.m2\repository\org
\jspecify\jspecify\1.0.0\jspecify-1.0.0.jar;C:\Users\Administrator\.m2\repository\org\jsoup\jsoup\1.23.2\jsoup-1.23.2.j
ar;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-redis\target\classes;C:\Users\
Administrator\.m2\repository\org\redisson\redisson-spring-boot-starter\4.7.0\redisson-spring-boot-starter-4.7.0.jar;C:\
Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-data-redis\3.5.15\spring-boot-starter-d
ata-redis-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\redisson\redisson\4.7.0\redisson-4.7.0.jar;C:\Users\Admi
nistrator\.m2\repository\io\netty\netty-common\4.2.17.Final\netty-common-4.2.17.Final.jar;C:\Users\Administrator\.m2\re
pository\io\netty\netty-codec\4.2.17.Final\netty-codec-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\
netty-codec-base\4.2.17.Final\netty-codec-base-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-co
dec-compression\4.2.17.Final\netty-codec-compression-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\ne
tty-codec-protobuf\4.2.17.Final\netty-codec-protobuf-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\ne
tty-codec-marshalling\4.2.17.Final\netty-codec-marshalling-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\ne
tty\netty-buffer\4.2.17.Final\netty-buffer-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-transp
ort\4.2.17.Final\netty-transport-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-resolver\4.2.17.
Final\netty-resolver-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-resolver-dns\4.2.17.Final\ne
tty-resolver-dns-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-codec-dns\4.2.17.Final\netty-cod
ec-dns-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-handler\4.2.17.Final\netty-handler-4.2.17.
Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-transport-native-unix-common\4.2.17.Final\netty-transpor
t-native-unix-common-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\javax\cache\cache-api\1.1.1\cache-api-1.1.1
.jar;C:\Users\Administrator\.m2\repository\io\projectreactor\reactor-core\3.7.19\reactor-core-3.7.19.jar;C:\Users\Admin
istrator\.m2\repository\org\reactivestreams\reactive-streams\1.0.4\reactive-streams-1.0.4.jar;C:\Users\Administrator\.m
2\repository\io\reactivex\rxjava3\rxjava\3.1.12\rxjava-3.1.12.jar;C:\Users\Administrator\.m2\repository\com\esotericsof
tware\kryo\5.6.2\kryo-5.6.2.jar;C:\Users\Administrator\.m2\repository\com\esotericsoftware\reflectasm\1.11.9\reflectasm
-1.11.9.jar;C:\Users\Administrator\.m2\repository\com\esotericsoftware\minlog\1.3.1\minlog-1.3.1.jar;C:\Users\Administr
ator\.m2\repository\net\bytebuddy\byte-buddy\1.17.8\byte-buddy-1.17.8.jar;C:\Users\Administrator\.m2\repository\org\jod
d\jodd-util\6.3.0\jodd-util-6.3.0.jar;C:\Users\Administrator\.m2\repository\org\redisson\redisson-spring-data-35\4.7.0\
redisson-spring-data-35-4.7.0.jar;C:\Users\Administrator\.m2\repository\org\springframework\data\spring-data-redis\3.5.
12\spring-data-redis-3.5.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\data\spring-data-keyvalue\3.5
.12\spring-data-keyvalue-3.5.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\data\spring-data-commons\
3.5.12\spring-data-commons-3.5.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-tx\6.2.19\spring
-tx-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-oxm\6.2.19\spring-oxm-6.2.19.jar;C:\Use
rs\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-cache\3.5.15\spring-boot-starter-cache-3.5
.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-context-support\6.2.19\spring-context-support-
6.2.19.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\datatype\jackson-datatype-jsr310\2.21.4\jackson-
datatype-jsr310-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\core\jackson-annotations\2.21\ja
ckson-annotations-2.21.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\core\jackson-core\2.21.4\jackson
-core-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\core\jackson-databind\2.21.4\jackson-datab
ind-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\baomidou\lock4j-redisson-spring-boot-starter\2.2.7\lock4j-redi
sson-spring-boot-starter-2.2.7.jar;C:\Users\Administrator\.m2\repository\com\baomidou\lock4j-core\2.2.7\lock4j-core-2.2
.7.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-aop\3.5.15\spring-boot-starte
r-aop-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\aspectj\aspectjweaver\1.9.25.1\aspectjweaver-1.9.25.1.jar;E:
\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-test\target\classes;E:\����֮��AI���
�ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-mybatis\target\classes;C:\Users\Administrator\.m
2\repository\com\alibaba\druid-spring-boot-3-starter\1.2.28\druid-spring-boot-3-starter-1.2.28.jar;C:\Users\Administrat
or\.m2\repository\com\alibaba\druid\1.2.28\druid-1.2.28.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-
plus-spring-boot3-starter\3.5.17\mybatis-plus-spring-boot3-starter-3.5.17.jar;C:\Users\Administrator\.m2\repository\com
\baomidou\mybatis-plus\3.5.17\mybatis-plus-3.5.17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-c
ore\3.5.17\mybatis-plus-core-3.5.17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-annotation\3.5.
17\mybatis-plus-annotation-3.5.17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-spring\3.5.17\myb
atis-plus-spring-3.5.17.jar;C:\Users\Administrator\.m2\repository\org\mybatis\mybatis\3.5.19\mybatis-3.5.19.jar;C:\User
s\Administrator\.m2\repository\org\mybatis\mybatis-spring\3.0.5\mybatis-spring-3.0.5.jar;C:\Users\Administrator\.m2\rep
ository\com\baomidou\mybatis-plus-spring-boot-autoconfigure\3.5.17\mybatis-plus-spring-boot-autoconfigure-3.5.17.jar;C:
\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-spring-boot-native-image\3.5.17\mybatis-plus-spring-boot-
native-image-3.5.17.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-jdbc\3.5.15\
spring-boot-starter-jdbc-3.5.15.jar;C:\Users\Administrator\.m2\repository\com\zaxxer\HikariCP\6.3.3\HikariCP-6.3.3.jar;
C:\Users\Administrator\.m2\repository\org\springframework\spring-jdbc\6.2.19\spring-jdbc-6.2.19.jar;C:\Users\Administra
tor\.m2\repository\com\baomidou\mybatis-plus-jsqlparser\3.5.17\mybatis-plus-jsqlparser-3.5.17.jar;C:\Users\Administrato
r\.m2\repository\com\github\jsqlparser\jsqlparser\5.2\jsqlparser-5.2.jar;C:\Users\Administrator\.m2\repository\com\baom
idou\mybatis-plus-jsqlparser-common\3.5.17\mybatis-plus-jsqlparser-common-3.5.17.jar;C:\Users\Administrator\.m2\reposit
ory\com\baomidou\mybatis-plus-extension\3.5.17\mybatis-plus-extension-3.5.17.jar;C:\Users\Administrator\.m2\repository\
com\baomidou\dynamic-datasource-spring-boot3-starter\4.5.0\dynamic-datasource-spring-boot3-starter-4.5.0.jar;C:\Users\A
dministrator\.m2\repository\com\baomidou\dynamic-datasource-spring-boot-common\4.5.0\dynamic-datasource-spring-boot-com
mon-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\baomidou\dynamic-datasource-spring\4.5.0\dynamic-datasource-spr
ing-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\baomidou\dynamic-datasource-creator\4.5.0\dynamic-datasource-cr
eator-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\oracle\database\spring\oracle-spring-boot-starter-ucp\23.4.0\
oracle-spring-boot-starter-ucp-23.4.0.jar;C:\Users\Administrator\.m2\repository\com\oracle\database\jdbc\ojdbc11\23.7.0
.25.01\ojdbc11-23.7.0.25.01.jar;C:\Users\Administrator\.m2\repository\com\oracle\database\jdbc\ucp\23.7.0.25.01\ucp-23.
7.0.25.01.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-join-boot-starter\1.5.9\mybatis-p
lus-join-boot-starter-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-join-extension\
1.5.9\mybatis-plus-join-extension-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-joi
n-core\1.5.9\mybatis-plus-join-core-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-j
oin-annotation\1.5.9\mybatis-plus-join-annotation-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\
mybatis-plus-join-adapter-base\1.5.9\mybatis-plus-join-adapter-base-1.5.9.jar;C:\Users\Administrator\.m2\repository\com
\github\yulichang\mybatis-plus-join-adapter-jsqlparser\1.5.9\mybatis-plus-join-adapter-jsqlparser-1.5.9.jar;C:\Users\Ad
ministrator\.m2\repository\com\github\yulichang\mybatis-plus-join-wrapper-ext\1.5.9\mybatis-plus-join-wrapper-ext-1.5.9
.jar;C:\Users\Administrator\.m2\repository\org\dromara\easy-trans-spring-boot-starter\3.1.8\easy-trans-spring-boot-star
ter-3.1.8.jar;C:\Users\Administrator\.m2\repository\org\dromara\easy-trans-service\3.1.8\easy-trans-service-3.1.8.jar;C
:\Users\Administrator\.m2\repository\com\github\ben-manes\caffeine\caffeine\3.2.4\caffeine-3.2.4.jar;C:\Users\Administr
ator\.m2\repository\org\dromara\easy-trans-mybatis-plus-extend\3.1.8\easy-trans-mybatis-plus-extend-3.1.8.jar;C:\Users\
Administrator\.m2\repository\org\mockito\mockito-inline\5.2.0\mockito-inline-5.2.0.jar;C:\Users\Administrator\.m2\repos
itory\org\mockito\mockito-core\5.17.0\mockito-core-5.17.0.jar;C:\Users\Administrator\.m2\repository\net\bytebuddy\byte-
buddy-agent\1.17.8\byte-buddy-agent-1.17.8.jar;C:\Users\Administrator\.m2\repository\org\objenesis\objenesis\3.3\objene
sis-3.3.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-test\3.5.15\spring-boot-
starter-test-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-test\3.5.15\spring-b
oot-test-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-test-autoconfigure\3.5.1
5\spring-boot-test-autoconfigure-3.5.15.jar;C:\Users\Administrator\.m2\repository\com\jayway\jsonpath\json-path\2.9.0\j
son-path-2.9.0.jar;C:\Users\Administrator\.m2\repository\jakarta\xml\bind\jakarta.xml.bind-api\4.0.5\jakarta.xml.bind-a
pi-4.0.5.jar;C:\Users\Administrator\.m2\repository\jakarta\activation\jakarta.activation-api\2.1.4\jakarta.activation-a
pi-2.1.4.jar;C:\Users\Administrator\.m2\repository\net\minidev\json-smart\2.5.2\json-smart-2.5.2.jar;C:\Users\Administr
ator\.m2\repository\net\minidev\accessors-smart\2.5.2\accessors-smart-2.5.2.jar;C:\Users\Administrator\.m2\repository\o
rg\assertj\assertj-core\3.27.7\assertj-core-3.27.7.jar;C:\Users\Administrator\.m2\repository\org\awaitility\awaitility\
4.2.2\awaitility-4.2.2.jar;C:\Users\Administrator\.m2\repository\org\hamcrest\hamcrest\3.0\hamcrest-3.0.jar;C:\Users\Ad
ministrator\.m2\repository\org\junit\jupiter\junit-jupiter\5.12.2\junit-jupiter-5.12.2.jar;C:\Users\Administrator\.m2\r
epository\org\junit\jupiter\junit-jupiter-api\5.12.2\junit-jupiter-api-5.12.2.jar;C:\Users\Administrator\.m2\repository
\org\opentest4j\opentest4j\1.3.0\opentest4j-1.3.0.jar;C:\Users\Administrator\.m2\repository\org\junit\platform\junit-pl
atform-commons\1.12.2\junit-platform-commons-1.12.2.jar;C:\Users\Administrator\.m2\repository\org\apiguardian\apiguardi
an-api\1.1.2\apiguardian-api-1.1.2.jar;C:\Users\Administrator\.m2\repository\org\junit\jupiter\junit-jupiter-params\5.1
2.2\junit-jupiter-params-5.12.2.jar;C:\Users\Administrator\.m2\repository\org\junit\jupiter\junit-jupiter-engine\5.12.2
\junit-jupiter-engine-5.12.2.jar;C:\Users\Administrator\.m2\repository\org\junit\platform\junit-platform-engine\1.12.2\
junit-platform-engine-1.12.2.jar;C:\Users\Administrator\.m2\repository\org\mockito\mockito-junit-jupiter\5.17.0\mockito
-junit-jupiter-5.17.0.jar;C:\Users\Administrator\.m2\repository\org\skyscreamer\jsonassert\1.5.3\jsonassert-1.5.3.jar;C
:\Users\Administrator\.m2\repository\com\vaadin\external\google\android-json\0.0.20131108.vaadin1\android-json-0.0.2013
1108.vaadin1.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-core\6.2.19\spring-core-6.2.19.jar;C:
\Users\Administrator\.m2\repository\org\springframework\spring-jcl\6.2.19\spring-jcl-6.2.19.jar;C:\Users\Administrator\
.m2\repository\org\springframework\spring-test\6.2.19\spring-test-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\
xmlunit\xmlunit-core\2.10.4\xmlunit-core-2.10.4.jar;C:\Users\Administrator\.m2\repository\com\h2database\h2\2.3.232\h2-
2.3.232.jar;C:\Users\Administrator\.m2\repository\com\github\fppt\jedis-mock\1.1.18\jedis-mock-1.1.18.jar;C:\Users\Admi
nistrator\.m2\repository\org\slf4j\slf4j-api\2.0.18\slf4j-api-2.0.18.jar;C:\Users\Administrator\.m2\repository\org\luaj
\luaj-jse\3.0.1\luaj-jse-3.0.1.jar;C:\Users\Administrator\.m2\repository\redis\clients\jedis\6.0.0\jedis-6.0.0.jar;C:\U
sers\Administrator\.m2\repository\org\apache\commons\commons-pool2\2.12.1\commons-pool2-2.12.1.jar;C:\Users\Administrat
or\.m2\repository\org\json\json\20250107\json-20250107.jar;C:\Users\Administrator\.m2\repository\com\google\code\gson\g
son\2.13.2\gson-2.13.2.jar;C:\Users\Administrator\.m2\repository\com\google\errorprone\error_prone_annotations\2.41.0\e
rror_prone_annotations-2.41.0.jar;C:\Users\Administrator\.m2\repository\redis\clients\authentication\redis-authx-core\0
.1.1-beta2\redis-authx-core-0.1.1-beta2.jar;C:\Users\Administrator\.m2\repository\uk\co\jemos\podam\podam\8.0.2.RELEASE
\podam-8.0.2.RELEASE.jar;C:\Users\Administrator\.m2\repository\net\jcip\jcip-annotations\1.0\jcip-annotations-1.0.jar;C
:\Users\Administrator\.m2\repository\jakarta\validation\jakarta.validation-api\3.0.2\jakarta.validation-api-3.0.2.jar;C
:\Users\Administrator\.m2\repository\org\apache\commons\commons-lang3\3.20.0\commons-lang3-3.20.0.jar;"/>
    <property name="java.vm.vendor" value="Eclipse Adoptium"/>
    <property name="sun.arch.data.model" value="64"/>
    <property name="user.variant" value=""/>
    <property name="java.vendor.url" value="https://adoptium.net/"/>
    <property name="user.timezone" value="Asia/Shanghai"/>
    <property name="os.name" value="Windows 10"/>
    <property name="java.vm.specification.version" value="17"/>
    <property name="sun.java.launcher" value="SUN_STANDARD"/>
    <property name="user.country" value="CN"/>
    <property name="sun.boot.library.path" value="E:\����֮��AI����ƽ̨����\tools\jdk-17.0.20.1+1\bin"/>
    <property name="sun.java.command" value="C:\Users\Administrator\AppData\Local\Temp\surefire12053511136342888388\sur
efirebooter-20260912135129227_23.jar C:\Users\Administrator\AppData\Local\Temp\surefire12053511136342888388 2026-09-12T
13-51-22_032-jvmRun1 surefire-20260912135129227_21tmp surefire_4-20260912135129227_22tmp"/>
    <property name="jdk.debug" value="release"/>
    <property name="surefire.test.class.path" value="E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-
spring-boot-starter-protection\target\test-classes;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-sp
ring-boot-starter-protection\target\classes;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-bo
ot-starter-web\target\classes;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-common\target\classes;C
:\Users\Administrator\.m2\repository\io\opentelemetry\opentelemetry-api\1.65.0\opentelemetry-api-1.65.0.jar;C:\Users\Ad
ministrator\.m2\repository\io\opentelemetry\opentelemetry-context\1.49.0\opentelemetry-context-1.49.0.jar;C:\Users\Admi
nistrator\.m2\repository\org\projectlombok\lombok\1.18.46\lombok-1.18.46.jar;C:\Users\Administrator\.m2\repository\org\
mapstruct\mapstruct\1.6.3\mapstruct-1.6.3.jar;C:\Users\Administrator\.m2\repository\org\mapstruct\mapstruct-jdk8\1.6.3\
mapstruct-jdk8-1.6.3.jar;C:\Users\Administrator\.m2\repository\org\mapstruct\mapstruct-processor\1.6.3\mapstruct-proces
sor-1.6.3.jar;C:\Users\Administrator\.m2\repository\cn\hutool\hutool-all\5.8.47\hutool-all-5.8.47.jar;C:\Users\Administ
rator\.m2\repository\com\alibaba\transmittable-thread-local\2.14.5\transmittable-thread-local-2.14.5.jar;C:\Users\Admin
istrator\.m2\repository\com\alibaba\fastjson\2.0.64\fastjson-2.0.64.jar;C:\Users\Administrator\.m2\repository\com\aliba
ba\fastjson2\fastjson2-extension\2.0.64\fastjson2-extension-2.0.64.jar;C:\Users\Administrator\.m2\repository\com\alibab
a\fastjson2\fastjson2\2.0.64\fastjson2-2.0.64.jar;C:\Users\Administrator\.m2\repository\org\dromara\easy-trans-anno\3.1
.8\easy-trans-anno-3.1.8.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-web\3.5
.15\spring-boot-starter-web-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-start
er\3.5.15\spring-boot-starter-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot\3.5
.15\spring-boot-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-autoconfigure\3.5
.15\spring-boot-autoconfigure-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-sta
rter-logging\3.5.15\spring-boot-starter-logging-3.5.15.jar;C:\Users\Administrator\.m2\repository\ch\qos\logback\logback
-classic\1.5.34\logback-classic-1.5.34.jar;C:\Users\Administrator\.m2\repository\ch\qos\logback\logback-core\1.5.34\log
back-core-1.5.34.jar;C:\Users\Administrator\.m2\repository\org\apache\logging\log4j\log4j-to-slf4j\2.24.3\log4j-to-slf4
j-2.24.3.jar;C:\Users\Administrator\.m2\repository\org\apache\logging\log4j\log4j-api\2.24.3\log4j-api-2.24.3.jar;C:\Us
ers\Administrator\.m2\repository\org\slf4j\jul-to-slf4j\2.0.18\jul-to-slf4j-2.0.18.jar;C:\Users\Administrator\.m2\repos
itory\jakarta\annotation\jakarta.annotation-api\2.1.1\jakarta.annotation-api-2.1.1.jar;C:\Users\Administrator\.m2\repos
itory\org\yaml\snakeyaml\2.4\snakeyaml-2.4.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-bo
ot-starter-json\3.5.15\spring-boot-starter-json-3.5.15.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\
datatype\jackson-datatype-jdk8\2.21.4\jackson-datatype-jdk8-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\faster
xml\jackson\module\jackson-module-parameter-names\2.21.4\jackson-module-parameter-names-2.21.4.jar;C:\Users\Administrat
or\.m2\repository\org\springframework\boot\spring-boot-starter-tomcat\3.5.15\spring-boot-starter-tomcat-3.5.15.jar;C:\U
sers\Administrator\.m2\repository\org\apache\tomcat\embed\tomcat-embed-core\10.1.55\tomcat-embed-core-10.1.55.jar;C:\Us
ers\Administrator\.m2\repository\org\apache\tomcat\embed\tomcat-embed-el\10.1.55\tomcat-embed-el-10.1.55.jar;C:\Users\A
dministrator\.m2\repository\org\apache\tomcat\embed\tomcat-embed-websocket\10.1.55\tomcat-embed-websocket-10.1.55.jar;C
:\Users\Administrator\.m2\repository\org\springframework\spring-web\6.2.19\spring-web-6.2.19.jar;C:\Users\Administrator
\.m2\repository\org\springframework\spring-beans\6.2.19\spring-beans-6.2.19.jar;C:\Users\Administrator\.m2\repository\i
o\micrometer\micrometer-observation\1.15.12\micrometer-observation-1.15.12.jar;C:\Users\Administrator\.m2\repository\io
\micrometer\micrometer-commons\1.15.12\micrometer-commons-1.15.12.jar;C:\Users\Administrator\.m2\repository\org\springf
ramework\spring-webmvc\6.2.19\spring-webmvc-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring
-aop\6.2.19\spring-aop-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-context\6.2.19\sprin
g-context-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-expression\6.2.19\spring-expressi
on-6.2.19.jar;C:\Users\Administrator\.m2\repository\com\github\xiaoymin\knife4j-openapi3-jakarta-spring-boot-starter\4.
5.0\knife4j-openapi3-jakarta-spring-boot-starter-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\github\xiaoymin\kn
ife4j-core\4.5.0\knife4j-core-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\github\xiaoymin\knife4j-openapi3-ui\4
.5.0\knife4j-openapi3-ui-4.5.0.jar;C:\Users\Administrator\.m2\repository\org\springdoc\springdoc-openapi-starter-webmvc
-ui\2.8.17\springdoc-openapi-starter-webmvc-ui-2.8.17.jar;C:\Users\Administrator\.m2\repository\org\springdoc\springdoc
-openapi-starter-webmvc-api\2.8.17\springdoc-openapi-starter-webmvc-api-2.8.17.jar;C:\Users\Administrator\.m2\repositor
y\org\springdoc\springdoc-openapi-starter-common\2.8.17\springdoc-openapi-starter-common-2.8.17.jar;C:\Users\Administra
tor\.m2\repository\org\springframework\boot\spring-boot-starter-validation\3.5.15\spring-boot-starter-validation-3.5.15
.jar;C:\Users\Administrator\.m2\repository\org\hibernate\validator\hibernate-validator\8.0.3.Final\hibernate-validator-
8.0.3.Final.jar;C:\Users\Administrator\.m2\repository\org\jboss\logging\jboss-logging\3.6.3.Final\jboss-logging-3.6.3.F
inal.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\classmate\1.7.3\classmate-1.7.3.jar;C:\Users\Administrator
\.m2\repository\io\swagger\core\v3\swagger-core-jakarta\2.2.47\swagger-core-jakarta-2.2.47.jar;C:\Users\Administrator\.
m2\repository\io\swagger\core\v3\swagger-annotations-jakarta\2.2.47\swagger-annotations-jakarta-2.2.47.jar;C:\Users\Adm
inistrator\.m2\repository\io\swagger\core\v3\swagger-models-jakarta\2.2.47\swagger-models-jakarta-2.2.47.jar;C:\Users\A
dministrator\.m2\repository\com\fasterxml\jackson\dataformat\jackson-dataformat-yaml\2.21.4\jackson-dataformat-yaml-2.2
1.4.jar;C:\Users\Administrator\.m2\repository\org\webjars\swagger-ui\5.32.2\swagger-ui-5.32.2.jar;C:\Users\Administrato
r\.m2\repository\org\webjars\webjars-locator-lite\1.1.3\webjars-locator-lite-1.1.3.jar;C:\Users\Administrator\.m2\repos
itory\org\jspecify\jspecify\1.0.0\jspecify-1.0.0.jar;C:\Users\Administrator\.m2\repository\org\jsoup\jsoup\1.23.2\jsoup
-1.23.2.jar;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-redis\target\classes;
C:\Users\Administrator\.m2\repository\org\redisson\redisson-spring-boot-starter\4.7.0\redisson-spring-boot-starter-4.7.
0.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-data-redis\3.5.15\spring-boot-
starter-data-redis-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\redisson\redisson\4.7.0\redisson-4.7.0.jar;C:\U
sers\Administrator\.m2\repository\io\netty\netty-common\4.2.17.Final\netty-common-4.2.17.Final.jar;C:\Users\Administrat
or\.m2\repository\io\netty\netty-codec\4.2.17.Final\netty-codec-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\
io\netty\netty-codec-base\4.2.17.Final\netty-codec-base-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty
\netty-codec-compression\4.2.17.Final\netty-codec-compression-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io
\netty\netty-codec-protobuf\4.2.17.Final\netty-codec-protobuf-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io
\netty\netty-codec-marshalling\4.2.17.Final\netty-codec-marshalling-4.2.17.Final.jar;C:\Users\Administrator\.m2\reposit
ory\io\netty\netty-buffer\4.2.17.Final\netty-buffer-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\net
ty-transport\4.2.17.Final\netty-transport-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-resolve
r\4.2.17.Final\netty-resolver-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-resolver-dns\4.2.17
.Final\netty-resolver-dns-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-codec-dns\4.2.17.Final\
netty-codec-dns-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-handler\4.2.17.Final\netty-handle
r-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-transport-native-unix-common\4.2.17.Final\netty
-transport-native-unix-common-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\javax\cache\cache-api\1.1.1\cache-
api-1.1.1.jar;C:\Users\Administrator\.m2\repository\io\projectreactor\reactor-core\3.7.19\reactor-core-3.7.19.jar;C:\Us
ers\Administrator\.m2\repository\org\reactivestreams\reactive-streams\1.0.4\reactive-streams-1.0.4.jar;C:\Users\Adminis
trator\.m2\repository\io\reactivex\rxjava3\rxjava\3.1.12\rxjava-3.1.12.jar;C:\Users\Administrator\.m2\repository\com\es
otericsoftware\kryo\5.6.2\kryo-5.6.2.jar;C:\Users\Administrator\.m2\repository\com\esotericsoftware\reflectasm\1.11.9\r
eflectasm-1.11.9.jar;C:\Users\Administrator\.m2\repository\com\esotericsoftware\minlog\1.3.1\minlog-1.3.1.jar;C:\Users\
Administrator\.m2\repository\net\bytebuddy\byte-buddy\1.17.8\byte-buddy-1.17.8.jar;C:\Users\Administrator\.m2\repositor
y\org\jodd\jodd-util\6.3.0\jodd-util-6.3.0.jar;C:\Users\Administrator\.m2\repository\org\redisson\redisson-spring-data-
35\4.7.0\redisson-spring-data-35-4.7.0.jar;C:\Users\Administrator\.m2\repository\org\springframework\data\spring-data-r
edis\3.5.12\spring-data-redis-3.5.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\data\spring-data-key
value\3.5.12\spring-data-keyvalue-3.5.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\data\spring-data
-commons\3.5.12\spring-data-commons-3.5.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-tx\6.2.
19\spring-tx-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-oxm\6.2.19\spring-oxm-6.2.19.j
ar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-cache\3.5.15\spring-boot-starter-
cache-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-context-support\6.2.19\spring-context
-support-6.2.19.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\datatype\jackson-datatype-jsr310\2.21.4
\jackson-datatype-jsr310-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\core\jackson-annotation
s\2.21\jackson-annotations-2.21.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\core\jackson-core\2.21.
4\jackson-core-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\core\jackson-databind\2.21.4\jack
son-databind-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\baomidou\lock4j-redisson-spring-boot-starter\2.2.7\lo
ck4j-redisson-spring-boot-starter-2.2.7.jar;C:\Users\Administrator\.m2\repository\com\baomidou\lock4j-core\2.2.7\lock4j
-core-2.2.7.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-aop\3.5.15\spring-bo
ot-starter-aop-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\aspectj\aspectjweaver\1.9.25.1\aspectjweaver-1.9.25
.1.jar;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-test\target\classes;E:\���
�֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-mybatis\target\classes;C:\Users\Adminis
trator\.m2\repository\com\alibaba\druid-spring-boot-3-starter\1.2.28\druid-spring-boot-3-starter-1.2.28.jar;C:\Users\Ad
ministrator\.m2\repository\com\alibaba\druid\1.2.28\druid-1.2.28.jar;C:\Users\Administrator\.m2\repository\com\baomidou
\mybatis-plus-spring-boot3-starter\3.5.17\mybatis-plus-spring-boot3-starter-3.5.17.jar;C:\Users\Administrator\.m2\repos
itory\com\baomidou\mybatis-plus\3.5.17\mybatis-plus-3.5.17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybat
is-plus-core\3.5.17\mybatis-plus-core-3.5.17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-annota
tion\3.5.17\mybatis-plus-annotation-3.5.17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-spring\3
.5.17\mybatis-plus-spring-3.5.17.jar;C:\Users\Administrator\.m2\repository\org\mybatis\mybatis\3.5.19\mybatis-3.5.19.ja
r;C:\Users\Administrator\.m2\repository\org\mybatis\mybatis-spring\3.0.5\mybatis-spring-3.0.5.jar;C:\Users\Administrato
r\.m2\repository\com\baomidou\mybatis-plus-spring-boot-autoconfigure\3.5.17\mybatis-plus-spring-boot-autoconfigure-3.5.
17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-spring-boot-native-image\3.5.17\mybatis-plus-spr
ing-boot-native-image-3.5.17.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-jdb
c\3.5.15\spring-boot-starter-jdbc-3.5.15.jar;C:\Users\Administrator\.m2\repository\com\zaxxer\HikariCP\6.3.3\HikariCP-6
.3.3.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-jdbc\6.2.19\spring-jdbc-6.2.19.jar;C:\Users\A
dministrator\.m2\repository\com\baomidou\mybatis-plus-jsqlparser\3.5.17\mybatis-plus-jsqlparser-3.5.17.jar;C:\Users\Adm
inistrator\.m2\repository\com\github\jsqlparser\jsqlparser\5.2\jsqlparser-5.2.jar;C:\Users\Administrator\.m2\repository
\com\baomidou\mybatis-plus-jsqlparser-common\3.5.17\mybatis-plus-jsqlparser-common-3.5.17.jar;C:\Users\Administrator\.m
2\repository\com\baomidou\mybatis-plus-extension\3.5.17\mybatis-plus-extension-3.5.17.jar;C:\Users\Administrator\.m2\re
pository\com\baomidou\dynamic-datasource-spring-boot3-starter\4.5.0\dynamic-datasource-spring-boot3-starter-4.5.0.jar;C
:\Users\Administrator\.m2\repository\com\baomidou\dynamic-datasource-spring-boot-common\4.5.0\dynamic-datasource-spring
-boot-common-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\baomidou\dynamic-datasource-spring\4.5.0\dynamic-datas
ource-spring-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\baomidou\dynamic-datasource-creator\4.5.0\dynamic-data
source-creator-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\oracle\database\spring\oracle-spring-boot-starter-uc
p\23.4.0\oracle-spring-boot-starter-ucp-23.4.0.jar;C:\Users\Administrator\.m2\repository\com\oracle\database\jdbc\ojdbc
11\23.7.0.25.01\ojdbc11-23.7.0.25.01.jar;C:\Users\Administrator\.m2\repository\com\oracle\database\jdbc\ucp\23.7.0.25.0
1\ucp-23.7.0.25.01.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-join-boot-starter\1.5.9\
mybatis-plus-join-boot-starter-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-join-e
xtension\1.5.9\mybatis-plus-join-extension-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis
-plus-join-core\1.5.9\mybatis-plus-join-core-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybat
is-plus-join-annotation\1.5.9\mybatis-plus-join-annotation-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\y
ulichang\mybatis-plus-join-adapter-base\1.5.9\mybatis-plus-join-adapter-base-1.5.9.jar;C:\Users\Administrator\.m2\repos
itory\com\github\yulichang\mybatis-plus-join-adapter-jsqlparser\1.5.9\mybatis-plus-join-adapter-jsqlparser-1.5.9.jar;C:
\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-join-wrapper-ext\1.5.9\mybatis-plus-join-wrapper-
ext-1.5.9.jar;C:\Users\Administrator\.m2\repository\org\dromara\easy-trans-spring-boot-starter\3.1.8\easy-trans-spring-
boot-starter-3.1.8.jar;C:\Users\Administrator\.m2\repository\org\dromara\easy-trans-service\3.1.8\easy-trans-service-3.
1.8.jar;C:\Users\Administrator\.m2\repository\com\github\ben-manes\caffeine\caffeine\3.2.4\caffeine-3.2.4.jar;C:\Users\
Administrator\.m2\repository\org\dromara\easy-trans-mybatis-plus-extend\3.1.8\easy-trans-mybatis-plus-extend-3.1.8.jar;
C:\Users\Administrator\.m2\repository\org\mockito\mockito-inline\5.2.0\mockito-inline-5.2.0.jar;C:\Users\Administrator\
.m2\repository\org\mockito\mockito-core\5.17.0\mockito-core-5.17.0.jar;C:\Users\Administrator\.m2\repository\net\bytebu
ddy\byte-buddy-agent\1.17.8\byte-buddy-agent-1.17.8.jar;C:\Users\Administrator\.m2\repository\org\objenesis\objenesis\3
.3\objenesis-3.3.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-test\3.5.15\spr
ing-boot-starter-test-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-test\3.5.15
\spring-boot-test-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-test-autoconfig
ure\3.5.15\spring-boot-test-autoconfigure-3.5.15.jar;C:\Users\Administrator\.m2\repository\com\jayway\jsonpath\json-pat
h\2.9.0\json-path-2.9.0.jar;C:\Users\Administrator\.m2\repository\jakarta\xml\bind\jakarta.xml.bind-api\4.0.5\jakarta.x
ml.bind-api-4.0.5.jar;C:\Users\Administrator\.m2\repository\jakarta\activation\jakarta.activation-api\2.1.4\jakarta.act
ivation-api-2.1.4.jar;C:\Users\Administrator\.m2\repository\net\minidev\json-smart\2.5.2\json-smart-2.5.2.jar;C:\Users\
Administrator\.m2\repository\net\minidev\accessors-smart\2.5.2\accessors-smart-2.5.2.jar;C:\Users\Administrator\.m2\rep
ository\org\assertj\assertj-core\3.27.7\assertj-core-3.27.7.jar;C:\Users\Administrator\.m2\repository\org\awaitility\aw
aitility\4.2.2\awaitility-4.2.2.jar;C:\Users\Administrator\.m2\repository\org\hamcrest\hamcrest\3.0\hamcrest-3.0.jar;C:
\Users\Administrator\.m2\repository\org\junit\jupiter\junit-jupiter\5.12.2\junit-jupiter-5.12.2.jar;C:\Users\Administra
tor\.m2\repository\org\junit\jupiter\junit-jupiter-api\5.12.2\junit-jupiter-api-5.12.2.jar;C:\Users\Administrator\.m2\r
epository\org\opentest4j\opentest4j\1.3.0\opentest4j-1.3.0.jar;C:\Users\Administrator\.m2\repository\org\junit\platform
\junit-platform-commons\1.12.2\junit-platform-commons-1.12.2.jar;C:\Users\Administrator\.m2\repository\org\apiguardian\
apiguardian-api\1.1.2\apiguardian-api-1.1.2.jar;C:\Users\Administrator\.m2\repository\org\junit\jupiter\junit-jupiter-p
arams\5.12.2\junit-jupiter-params-5.12.2.jar;C:\Users\Administrator\.m2\repository\org\junit\jupiter\junit-jupiter-engi
ne\5.12.2\junit-jupiter-engine-5.12.2.jar;C:\Users\Administrator\.m2\repository\org\junit\platform\junit-platform-engin
e\1.12.2\junit-platform-engine-1.12.2.jar;C:\Users\Administrator\.m2\repository\org\mockito\mockito-junit-jupiter\5.17.
0\mockito-junit-jupiter-5.17.0.jar;C:\Users\Administrator\.m2\repository\org\skyscreamer\jsonassert\1.5.3\jsonassert-1.
5.3.jar;C:\Users\Administrator\.m2\repository\com\vaadin\external\google\android-json\0.0.20131108.vaadin1\android-json
-0.0.20131108.vaadin1.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-core\6.2.19\spring-core-6.2.
19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-jcl\6.2.19\spring-jcl-6.2.19.jar;C:\Users\Admin
istrator\.m2\repository\org\springframework\spring-test\6.2.19\spring-test-6.2.19.jar;C:\Users\Administrator\.m2\reposi
tory\org\xmlunit\xmlunit-core\2.10.4\xmlunit-core-2.10.4.jar;C:\Users\Administrator\.m2\repository\com\h2database\h2\2.
3.232\h2-2.3.232.jar;C:\Users\Administrator\.m2\repository\com\github\fppt\jedis-mock\1.1.18\jedis-mock-1.1.18.jar;C:\U
sers\Administrator\.m2\repository\org\slf4j\slf4j-api\2.0.18\slf4j-api-2.0.18.jar;C:\Users\Administrator\.m2\repository
\org\luaj\luaj-jse\3.0.1\luaj-jse-3.0.1.jar;C:\Users\Administrator\.m2\repository\redis\clients\jedis\6.0.0\jedis-6.0.0
.jar;C:\Users\Administrator\.m2\repository\org\apache\commons\commons-pool2\2.12.1\commons-pool2-2.12.1.jar;C:\Users\Ad
ministrator\.m2\repository\org\json\json\20250107\json-20250107.jar;C:\Users\Administrator\.m2\repository\com\google\co
de\gson\gson\2.13.2\gson-2.13.2.jar;C:\Users\Administrator\.m2\repository\com\google\errorprone\error_prone_annotations
\2.41.0\error_prone_annotations-2.41.0.jar;C:\Users\Administrator\.m2\repository\redis\clients\authentication\redis-aut
hx-core\0.1.1-beta2\redis-authx-core-0.1.1-beta2.jar;C:\Users\Administrator\.m2\repository\uk\co\jemos\podam\podam\8.0.
2.RELEASE\podam-8.0.2.RELEASE.jar;C:\Users\Administrator\.m2\repository\net\jcip\jcip-annotations\1.0\jcip-annotations-
1.0.jar;C:\Users\Administrator\.m2\repository\jakarta\validation\jakarta.validation-api\3.0.2\jakarta.validation-api-3.
0.2.jar;C:\Users\Administrator\.m2\repository\org\apache\commons\commons-lang3\3.20.0\commons-lang3-3.20.0.jar;"/>
    <property name="sun.cpu.endian" value="little"/>
    <property name="user.home" value="C:\Users\Administrator"/>
    <property name="user.language" value="zh"/>
    <property name="java.specification.vendor" value="Oracle Corporation"/>
    <property name="java.version.date" value="2026-08-18"/>
    <property name="java.home" value="E:\����֮��AI����ƽ̨����\tools\jdk-17.0.20.1+1"/>
    <property name="file.separator" value="\"/>
    <property name="basedir" value="E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-start
er-protection"/>
    <property name="java.vm.compressedOopsMode" value="Zero based"/>
    <property name="line.separator" value="&#10;"/>
    <property name="java.vm.specification.vendor" value="Oracle Corporation"/>
    <property name="java.specification.name" value="Java Platform API Specification"/>
    <property name="surefire.real.class.path" value="C:\Users\Administrator\AppData\Local\Temp\surefire1205351113634288
8388\surefirebooter-20260912135129227_23.jar"/>
    <property name="user.script" value=""/>
    <property name="sun.management.compiler" value="HotSpot 64-Bit Tiered Compilers"/>
    <property name="java.runtime.version" value="17.0.20.1+1"/>
    <property name="user.name" value="Administrator"/>
    <property name="path.separator" value=";"/>
    <property name="os.version" value="10.0"/>
    <property name="java.runtime.name" value="OpenJDK Runtime Environment"/>
    <property name="file.encoding" value="GBK"/>
    <property name="java.vm.name" value="OpenJDK 64-Bit Server VM"/>
    <property name="java.vendor.version" value="Temurin-17.0.20.1+1"/>
    <property name="localRepository" value="C:\Users\Administrator\.m2\repository"/>
    <property name="java.vendor.url.bug" value="https://github.com/adoptium/adoptium-support/issues"/>
    <property name="java.io.tmpdir" value="C:\Users\ADMINI~1\AppData\Local\Temp\"/>
    <property name="java.version" value="17.0.20.1"/>
    <property name="user.dir" value="E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-star
ter-protection"/>
    <property name="os.arch" value="amd64"/>
    <property name="java.vm.specification.name" value="Java Virtual Machine Specification"/>
    <property name="sun.os.patch.level" value=""/>
    <property name="native.encoding" value="GBK"/>
    <property name="java.library.path" value="E:\����֮��AI����ƽ̨����\tools\jdk-17.0.20.1+1\bin;C:\windows\Sun\Java\bin;
C:\windows\system32;C:\windows;E:\����֮��AI����ƽ̨����\tools\jdk-17.0.20.1+1\bin;E:\����֮��AI����ƽ̨����\tools\apache-mav
en-3.9.9\bin;C:\Program Files\Python311\Scripts\;C:\Program Files\Python311\;C:\windows\system32;C:\windows;C:\windows\
System32\Wbem;C:\windows\System32\WindowsPowerShell\v1.0\;C:\windows\System32\OpenSSH\;D:\Program Files (x86)\Tencent\΢
��web�����߹���;;C:\Program Files\Git\cmd;C:\Program Files\nodejs\;C:\Users\Administrator\AppData\Local\Microsoft\Window
sApps;;C:\Users\Administrator\AppData\Local\Programs\DockerDesktop\resources\bin;C:\Users\Administrator\AppData\Roaming
\npm;D:\Program Files\Qoder IDE\bin;C:\Users\Administrator\AppData\Local\Microsoft\WinGet\Links;;."/>
    <property name="java.vm.info" value="mixed mode, sharing"/>
    <property name="java.vendor" value="Eclipse Adoptium"/>
    <property name="java.vm.version" value="17.0.20.1+1"/>
    <property name="java.specification.maintenance.version" value="1"/>
    <property name="sun.io.unicode.encoding" value="UnicodeLittle"/>
    <property name="java.class.version" value="61.0"/>
  </properties>
  <testcase name="resolver_differentArgs_shouldProduceDifferentKey" classname="cn.zszj.framework.idempotent.core.keyres
olver.impl.DefaultIdempotentKeyResolverTest" time="0.017"/>
  <testcase name="resolver_noRequestContext_shouldNotCrash" classname="cn.zszj.framework.idempotent.core.keyresolver.im
pl.DefaultIdempotentKeyResolverTest" time="0.003"/>
  <testcase name="resolver_differentUser_shouldProduceDifferentKey" classname="cn.zszj.framework.idempotent.core.keyres
olver.impl.DefaultIdempotentKeyResolverTest" time="0.005"/>
  <testcase name="resolver_nullScope_differentAnonymousSubjects_collapseToSameKey" classname="cn.zszj.framework.idempot
ent.core.keyresolver.impl.DefaultIdempotentKeyResolverTest" time="0.003"/>
  <testcase name="resolver_differentTenant_shouldProduceDifferentKey" classname="cn.zszj.framework.idempotent.core.keyr
esolver.impl.DefaultIdempotentKeyResolverTest" time="0.005"/>
  <testcase name="resolver_sameSubject_shouldProduceSameKey" classname="cn.zszj.framework.idempotent.core.keyresolver.i
mpl.DefaultIdempotentKeyResolverTest" time="0.004"/>
  <testcase name="resolver_differentUserType_shouldProduceDifferentKey" classname="cn.zszj.framework.idempotent.core.ke
yresolver.impl.DefaultIdempotentKeyResolverTest" time="0.004"/>
</testsuite>
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\TEST-cn.zszj.framework.ratelimiter.core.aop.RateLimiterAspectTest.xml
<?xml version="1.0" encoding="UTF-8"?>
<testsuite xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="https://maven.apache.or
g/surefire/maven-surefire-plugin/xsd/surefire-test-report.xsd" version="3.0.2" name="cn.zszj.framework.ratelimiter.core
.aop.RateLimiterAspectTest" time="0.273" tests="5" errors="0" skipped="0" failures="0">
  <properties>
    <property name="java.specification.version" value="17"/>
    <property name="sun.cpu.isalist" value="amd64"/>
    <property name="sun.jnu.encoding" value="GBK"/>
    <property name="java.class.path" value="E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-bo
ot-starter-protection\target\test-classes;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot
-starter-protection\target\classes;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starte
r-web\target\classes;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-common\target\classes;C:\Users\A
dministrator\.m2\repository\io\opentelemetry\opentelemetry-api\1.65.0\opentelemetry-api-1.65.0.jar;C:\Users\Administrat
or\.m2\repository\io\opentelemetry\opentelemetry-context\1.49.0\opentelemetry-context-1.49.0.jar;C:\Users\Administrator
\.m2\repository\org\projectlombok\lombok\1.18.46\lombok-1.18.46.jar;C:\Users\Administrator\.m2\repository\org\mapstruct
\mapstruct\1.6.3\mapstruct-1.6.3.jar;C:\Users\Administrator\.m2\repository\org\mapstruct\mapstruct-jdk8\1.6.3\mapstruct
-jdk8-1.6.3.jar;C:\Users\Administrator\.m2\repository\org\mapstruct\mapstruct-processor\1.6.3\mapstruct-processor-1.6.3
.jar;C:\Users\Administrator\.m2\repository\cn\hutool\hutool-all\5.8.47\hutool-all-5.8.47.jar;C:\Users\Administrator\.m2
\repository\com\alibaba\transmittable-thread-local\2.14.5\transmittable-thread-local-2.14.5.jar;C:\Users\Administrator\
.m2\repository\com\alibaba\fastjson\2.0.64\fastjson-2.0.64.jar;C:\Users\Administrator\.m2\repository\com\alibaba\fastjs
on2\fastjson2-extension\2.0.64\fastjson2-extension-2.0.64.jar;C:\Users\Administrator\.m2\repository\com\alibaba\fastjso
n2\fastjson2\2.0.64\fastjson2-2.0.64.jar;C:\Users\Administrator\.m2\repository\org\dromara\easy-trans-anno\3.1.8\easy-t
rans-anno-3.1.8.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-web\3.5.15\sprin
g-boot-starter-web-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter\3.5.15
\spring-boot-starter-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot\3.5.15\sprin
g-boot-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-autoconfigure\3.5.15\sprin
g-boot-autoconfigure-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-logg
ing\3.5.15\spring-boot-starter-logging-3.5.15.jar;C:\Users\Administrator\.m2\repository\ch\qos\logback\logback-classic\
1.5.34\logback-classic-1.5.34.jar;C:\Users\Administrator\.m2\repository\ch\qos\logback\logback-core\1.5.34\logback-core
-1.5.34.jar;C:\Users\Administrator\.m2\repository\org\apache\logging\log4j\log4j-to-slf4j\2.24.3\log4j-to-slf4j-2.24.3.
jar;C:\Users\Administrator\.m2\repository\org\apache\logging\log4j\log4j-api\2.24.3\log4j-api-2.24.3.jar;C:\Users\Admin
istrator\.m2\repository\org\slf4j\jul-to-slf4j\2.0.18\jul-to-slf4j-2.0.18.jar;C:\Users\Administrator\.m2\repository\jak
arta\annotation\jakarta.annotation-api\2.1.1\jakarta.annotation-api-2.1.1.jar;C:\Users\Administrator\.m2\repository\org
\yaml\snakeyaml\2.4\snakeyaml-2.4.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starte
r-json\3.5.15\spring-boot-starter-json-3.5.15.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\datatype\
jackson-datatype-jdk8\2.21.4\jackson-datatype-jdk8-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jacks
on\module\jackson-module-parameter-names\2.21.4\jackson-module-parameter-names-2.21.4.jar;C:\Users\Administrator\.m2\re
pository\org\springframework\boot\spring-boot-starter-tomcat\3.5.15\spring-boot-starter-tomcat-3.5.15.jar;C:\Users\Admi
nistrator\.m2\repository\org\apache\tomcat\embed\tomcat-embed-core\10.1.55\tomcat-embed-core-10.1.55.jar;C:\Users\Admin
istrator\.m2\repository\org\apache\tomcat\embed\tomcat-embed-el\10.1.55\tomcat-embed-el-10.1.55.jar;C:\Users\Administra
tor\.m2\repository\org\apache\tomcat\embed\tomcat-embed-websocket\10.1.55\tomcat-embed-websocket-10.1.55.jar;C:\Users\A
dministrator\.m2\repository\org\springframework\spring-web\6.2.19\spring-web-6.2.19.jar;C:\Users\Administrator\.m2\repo
sitory\org\springframework\spring-beans\6.2.19\spring-beans-6.2.19.jar;C:\Users\Administrator\.m2\repository\io\microme
ter\micrometer-observation\1.15.12\micrometer-observation-1.15.12.jar;C:\Users\Administrator\.m2\repository\io\micromet
er\micrometer-commons\1.15.12\micrometer-commons-1.15.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\
spring-webmvc\6.2.19\spring-webmvc-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-aop\6.2.
19\spring-aop-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-context\6.2.19\spring-context
-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-expression\6.2.19\spring-expression-6.2.19
.jar;C:\Users\Administrator\.m2\repository\com\github\xiaoymin\knife4j-openapi3-jakarta-spring-boot-starter\4.5.0\knife
4j-openapi3-jakarta-spring-boot-starter-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\github\xiaoymin\knife4j-cor
e\4.5.0\knife4j-core-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\github\xiaoymin\knife4j-openapi3-ui\4.5.0\knif
e4j-openapi3-ui-4.5.0.jar;C:\Users\Administrator\.m2\repository\org\springdoc\springdoc-openapi-starter-webmvc-ui\2.8.1
7\springdoc-openapi-starter-webmvc-ui-2.8.17.jar;C:\Users\Administrator\.m2\repository\org\springdoc\springdoc-openapi-
starter-webmvc-api\2.8.17\springdoc-openapi-starter-webmvc-api-2.8.17.jar;C:\Users\Administrator\.m2\repository\org\spr
ingdoc\springdoc-openapi-starter-common\2.8.17\springdoc-openapi-starter-common-2.8.17.jar;C:\Users\Administrator\.m2\r
epository\org\springframework\boot\spring-boot-starter-validation\3.5.15\spring-boot-starter-validation-3.5.15.jar;C:\U
sers\Administrator\.m2\repository\org\hibernate\validator\hibernate-validator\8.0.3.Final\hibernate-validator-8.0.3.Fin
al.jar;C:\Users\Administrator\.m2\repository\org\jboss\logging\jboss-logging\3.6.3.Final\jboss-logging-3.6.3.Final.jar;
C:\Users\Administrator\.m2\repository\com\fasterxml\classmate\1.7.3\classmate-1.7.3.jar;C:\Users\Administrator\.m2\repo
sitory\io\swagger\core\v3\swagger-core-jakarta\2.2.47\swagger-core-jakarta-2.2.47.jar;C:\Users\Administrator\.m2\reposi
tory\io\swagger\core\v3\swagger-annotations-jakarta\2.2.47\swagger-annotations-jakarta-2.2.47.jar;C:\Users\Administrato
r\.m2\repository\io\swagger\core\v3\swagger-models-jakarta\2.2.47\swagger-models-jakarta-2.2.47.jar;C:\Users\Administra
tor\.m2\repository\com\fasterxml\jackson\dataformat\jackson-dataformat-yaml\2.21.4\jackson-dataformat-yaml-2.21.4.jar;C
:\Users\Administrator\.m2\repository\org\webjars\swagger-ui\5.32.2\swagger-ui-5.32.2.jar;C:\Users\Administrator\.m2\rep
ository\org\webjars\webjars-locator-lite\1.1.3\webjars-locator-lite-1.1.3.jar;C:\Users\Administrator\.m2\repository\org
\jspecify\jspecify\1.0.0\jspecify-1.0.0.jar;C:\Users\Administrator\.m2\repository\org\jsoup\jsoup\1.23.2\jsoup-1.23.2.j
ar;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-redis\target\classes;C:\Users\
Administrator\.m2\repository\org\redisson\redisson-spring-boot-starter\4.7.0\redisson-spring-boot-starter-4.7.0.jar;C:\
Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-data-redis\3.5.15\spring-boot-starter-d
ata-redis-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\redisson\redisson\4.7.0\redisson-4.7.0.jar;C:\Users\Admi
nistrator\.m2\repository\io\netty\netty-common\4.2.17.Final\netty-common-4.2.17.Final.jar;C:\Users\Administrator\.m2\re
pository\io\netty\netty-codec\4.2.17.Final\netty-codec-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\
netty-codec-base\4.2.17.Final\netty-codec-base-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-co
dec-compression\4.2.17.Final\netty-codec-compression-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\ne
tty-codec-protobuf\4.2.17.Final\netty-codec-protobuf-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\ne
tty-codec-marshalling\4.2.17.Final\netty-codec-marshalling-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\ne
tty\netty-buffer\4.2.17.Final\netty-buffer-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-transp
ort\4.2.17.Final\netty-transport-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-resolver\4.2.17.
Final\netty-resolver-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-resolver-dns\4.2.17.Final\ne
tty-resolver-dns-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-codec-dns\4.2.17.Final\netty-cod
ec-dns-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-handler\4.2.17.Final\netty-handler-4.2.17.
Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-transport-native-unix-common\4.2.17.Final\netty-transpor
t-native-unix-common-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\javax\cache\cache-api\1.1.1\cache-api-1.1.1
.jar;C:\Users\Administrator\.m2\repository\io\projectreactor\reactor-core\3.7.19\reactor-core-3.7.19.jar;C:\Users\Admin
istrator\.m2\repository\org\reactivestreams\reactive-streams\1.0.4\reactive-streams-1.0.4.jar;C:\Users\Administrator\.m
2\repository\io\reactivex\rxjava3\rxjava\3.1.12\rxjava-3.1.12.jar;C:\Users\Administrator\.m2\repository\com\esotericsof
tware\kryo\5.6.2\kryo-5.6.2.jar;C:\Users\Administrator\.m2\repository\com\esotericsoftware\reflectasm\1.11.9\reflectasm
-1.11.9.jar;C:\Users\Administrator\.m2\repository\com\esotericsoftware\minlog\1.3.1\minlog-1.3.1.jar;C:\Users\Administr
ator\.m2\repository\net\bytebuddy\byte-buddy\1.17.8\byte-buddy-1.17.8.jar;C:\Users\Administrator\.m2\repository\org\jod
d\jodd-util\6.3.0\jodd-util-6.3.0.jar;C:\Users\Administrator\.m2\repository\org\redisson\redisson-spring-data-35\4.7.0\
redisson-spring-data-35-4.7.0.jar;C:\Users\Administrator\.m2\repository\org\springframework\data\spring-data-redis\3.5.
12\spring-data-redis-3.5.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\data\spring-data-keyvalue\3.5
.12\spring-data-keyvalue-3.5.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\data\spring-data-commons\
3.5.12\spring-data-commons-3.5.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-tx\6.2.19\spring
-tx-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-oxm\6.2.19\spring-oxm-6.2.19.jar;C:\Use
rs\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-cache\3.5.15\spring-boot-starter-cache-3.5
.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-context-support\6.2.19\spring-context-support-
6.2.19.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\datatype\jackson-datatype-jsr310\2.21.4\jackson-
datatype-jsr310-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\core\jackson-annotations\2.21\ja
ckson-annotations-2.21.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\core\jackson-core\2.21.4\jackson
-core-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\core\jackson-databind\2.21.4\jackson-datab
ind-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\baomidou\lock4j-redisson-spring-boot-starter\2.2.7\lock4j-redi
sson-spring-boot-starter-2.2.7.jar;C:\Users\Administrator\.m2\repository\com\baomidou\lock4j-core\2.2.7\lock4j-core-2.2
.7.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-aop\3.5.15\spring-boot-starte
r-aop-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\aspectj\aspectjweaver\1.9.25.1\aspectjweaver-1.9.25.1.jar;E:
\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-test\target\classes;E:\����֮��AI���
�ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-mybatis\target\classes;C:\Users\Administrator\.m
2\repository\com\alibaba\druid-spring-boot-3-starter\1.2.28\druid-spring-boot-3-starter-1.2.28.jar;C:\Users\Administrat
or\.m2\repository\com\alibaba\druid\1.2.28\druid-1.2.28.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-
plus-spring-boot3-starter\3.5.17\mybatis-plus-spring-boot3-starter-3.5.17.jar;C:\Users\Administrator\.m2\repository\com
\baomidou\mybatis-plus\3.5.17\mybatis-plus-3.5.17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-c
ore\3.5.17\mybatis-plus-core-3.5.17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-annotation\3.5.
17\mybatis-plus-annotation-3.5.17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-spring\3.5.17\myb
atis-plus-spring-3.5.17.jar;C:\Users\Administrator\.m2\repository\org\mybatis\mybatis\3.5.19\mybatis-3.5.19.jar;C:\User
s\Administrator\.m2\repository\org\mybatis\mybatis-spring\3.0.5\mybatis-spring-3.0.5.jar;C:\Users\Administrator\.m2\rep
ository\com\baomidou\mybatis-plus-spring-boot-autoconfigure\3.5.17\mybatis-plus-spring-boot-autoconfigure-3.5.17.jar;C:
\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-spring-boot-native-image\3.5.17\mybatis-plus-spring-boot-
native-image-3.5.17.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-jdbc\3.5.15\
spring-boot-starter-jdbc-3.5.15.jar;C:\Users\Administrator\.m2\repository\com\zaxxer\HikariCP\6.3.3\HikariCP-6.3.3.jar;
C:\Users\Administrator\.m2\repository\org\springframework\spring-jdbc\6.2.19\spring-jdbc-6.2.19.jar;C:\Users\Administra
tor\.m2\repository\com\baomidou\mybatis-plus-jsqlparser\3.5.17\mybatis-plus-jsqlparser-3.5.17.jar;C:\Users\Administrato
r\.m2\repository\com\github\jsqlparser\jsqlparser\5.2\jsqlparser-5.2.jar;C:\Users\Administrator\.m2\repository\com\baom
idou\mybatis-plus-jsqlparser-common\3.5.17\mybatis-plus-jsqlparser-common-3.5.17.jar;C:\Users\Administrator\.m2\reposit
ory\com\baomidou\mybatis-plus-extension\3.5.17\mybatis-plus-extension-3.5.17.jar;C:\Users\Administrator\.m2\repository\
com\baomidou\dynamic-datasource-spring-boot3-starter\4.5.0\dynamic-datasource-spring-boot3-starter-4.5.0.jar;C:\Users\A
dministrator\.m2\repository\com\baomidou\dynamic-datasource-spring-boot-common\4.5.0\dynamic-datasource-spring-boot-com
mon-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\baomidou\dynamic-datasource-spring\4.5.0\dynamic-datasource-spr
ing-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\baomidou\dynamic-datasource-creator\4.5.0\dynamic-datasource-cr
eator-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\oracle\database\spring\oracle-spring-boot-starter-ucp\23.4.0\
oracle-spring-boot-starter-ucp-23.4.0.jar;C:\Users\Administrator\.m2\repository\com\oracle\database\jdbc\ojdbc11\23.7.0
.25.01\ojdbc11-23.7.0.25.01.jar;C:\Users\Administrator\.m2\repository\com\oracle\database\jdbc\ucp\23.7.0.25.01\ucp-23.
7.0.25.01.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-join-boot-starter\1.5.9\mybatis-p
lus-join-boot-starter-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-join-extension\
1.5.9\mybatis-plus-join-extension-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-joi
n-core\1.5.9\mybatis-plus-join-core-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-j
oin-annotation\1.5.9\mybatis-plus-join-annotation-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\
mybatis-plus-join-adapter-base\1.5.9\mybatis-plus-join-adapter-base-1.5.9.jar;C:\Users\Administrator\.m2\repository\com
\github\yulichang\mybatis-plus-join-adapter-jsqlparser\1.5.9\mybatis-plus-join-adapter-jsqlparser-1.5.9.jar;C:\Users\Ad
ministrator\.m2\repository\com\github\yulichang\mybatis-plus-join-wrapper-ext\1.5.9\mybatis-plus-join-wrapper-ext-1.5.9
.jar;C:\Users\Administrator\.m2\repository\org\dromara\easy-trans-spring-boot-starter\3.1.8\easy-trans-spring-boot-star
ter-3.1.8.jar;C:\Users\Administrator\.m2\repository\org\dromara\easy-trans-service\3.1.8\easy-trans-service-3.1.8.jar;C
:\Users\Administrator\.m2\repository\com\github\ben-manes\caffeine\caffeine\3.2.4\caffeine-3.2.4.jar;C:\Users\Administr
ator\.m2\repository\org\dromara\easy-trans-mybatis-plus-extend\3.1.8\easy-trans-mybatis-plus-extend-3.1.8.jar;C:\Users\
Administrator\.m2\repository\org\mockito\mockito-inline\5.2.0\mockito-inline-5.2.0.jar;C:\Users\Administrator\.m2\repos
itory\org\mockito\mockito-core\5.17.0\mockito-core-5.17.0.jar;C:\Users\Administrator\.m2\repository\net\bytebuddy\byte-
buddy-agent\1.17.8\byte-buddy-agent-1.17.8.jar;C:\Users\Administrator\.m2\repository\org\objenesis\objenesis\3.3\objene
sis-3.3.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-test\3.5.15\spring-boot-
starter-test-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-test\3.5.15\spring-b
oot-test-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-test-autoconfigure\3.5.1
5\spring-boot-test-autoconfigure-3.5.15.jar;C:\Users\Administrator\.m2\repository\com\jayway\jsonpath\json-path\2.9.0\j
son-path-2.9.0.jar;C:\Users\Administrator\.m2\repository\jakarta\xml\bind\jakarta.xml.bind-api\4.0.5\jakarta.xml.bind-a
pi-4.0.5.jar;C:\Users\Administrator\.m2\repository\jakarta\activation\jakarta.activation-api\2.1.4\jakarta.activation-a
pi-2.1.4.jar;C:\Users\Administrator\.m2\repository\net\minidev\json-smart\2.5.2\json-smart-2.5.2.jar;C:\Users\Administr
ator\.m2\repository\net\minidev\accessors-smart\2.5.2\accessors-smart-2.5.2.jar;C:\Users\Administrator\.m2\repository\o
rg\assertj\assertj-core\3.27.7\assertj-core-3.27.7.jar;C:\Users\Administrator\.m2\repository\org\awaitility\awaitility\
4.2.2\awaitility-4.2.2.jar;C:\Users\Administrator\.m2\repository\org\hamcrest\hamcrest\3.0\hamcrest-3.0.jar;C:\Users\Ad
ministrator\.m2\repository\org\junit\jupiter\junit-jupiter\5.12.2\junit-jupiter-5.12.2.jar;C:\Users\Administrator\.m2\r
epository\org\junit\jupiter\junit-jupiter-api\5.12.2\junit-jupiter-api-5.12.2.jar;C:\Users\Administrator\.m2\repository
\org\opentest4j\opentest4j\1.3.0\opentest4j-1.3.0.jar;C:\Users\Administrator\.m2\repository\org\junit\platform\junit-pl
atform-commons\1.12.2\junit-platform-commons-1.12.2.jar;C:\Users\Administrator\.m2\repository\org\apiguardian\apiguardi
an-api\1.1.2\apiguardian-api-1.1.2.jar;C:\Users\Administrator\.m2\repository\org\junit\jupiter\junit-jupiter-params\5.1
2.2\junit-jupiter-params-5.12.2.jar;C:\Users\Administrator\.m2\repository\org\junit\jupiter\junit-jupiter-engine\5.12.2
\junit-jupiter-engine-5.12.2.jar;C:\Users\Administrator\.m2\repository\org\junit\platform\junit-platform-engine\1.12.2\
junit-platform-engine-1.12.2.jar;C:\Users\Administrator\.m2\repository\org\mockito\mockito-junit-jupiter\5.17.0\mockito
-junit-jupiter-5.17.0.jar;C:\Users\Administrator\.m2\repository\org\skyscreamer\jsonassert\1.5.3\jsonassert-1.5.3.jar;C
:\Users\Administrator\.m2\repository\com\vaadin\external\google\android-json\0.0.20131108.vaadin1\android-json-0.0.2013
1108.vaadin1.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-core\6.2.19\spring-core-6.2.19.jar;C:
\Users\Administrator\.m2\repository\org\springframework\spring-jcl\6.2.19\spring-jcl-6.2.19.jar;C:\Users\Administrator\
.m2\repository\org\springframework\spring-test\6.2.19\spring-test-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\
xmlunit\xmlunit-core\2.10.4\xmlunit-core-2.10.4.jar;C:\Users\Administrator\.m2\repository\com\h2database\h2\2.3.232\h2-
2.3.232.jar;C:\Users\Administrator\.m2\repository\com\github\fppt\jedis-mock\1.1.18\jedis-mock-1.1.18.jar;C:\Users\Admi
nistrator\.m2\repository\org\slf4j\slf4j-api\2.0.18\slf4j-api-2.0.18.jar;C:\Users\Administrator\.m2\repository\org\luaj
\luaj-jse\3.0.1\luaj-jse-3.0.1.jar;C:\Users\Administrator\.m2\repository\redis\clients\jedis\6.0.0\jedis-6.0.0.jar;C:\U
sers\Administrator\.m2\repository\org\apache\commons\commons-pool2\2.12.1\commons-pool2-2.12.1.jar;C:\Users\Administrat
or\.m2\repository\org\json\json\20250107\json-20250107.jar;C:\Users\Administrator\.m2\repository\com\google\code\gson\g
son\2.13.2\gson-2.13.2.jar;C:\Users\Administrator\.m2\repository\com\google\errorprone\error_prone_annotations\2.41.0\e
rror_prone_annotations-2.41.0.jar;C:\Users\Administrator\.m2\repository\redis\clients\authentication\redis-authx-core\0
.1.1-beta2\redis-authx-core-0.1.1-beta2.jar;C:\Users\Administrator\.m2\repository\uk\co\jemos\podam\podam\8.0.2.RELEASE
\podam-8.0.2.RELEASE.jar;C:\Users\Administrator\.m2\repository\net\jcip\jcip-annotations\1.0\jcip-annotations-1.0.jar;C
:\Users\Administrator\.m2\repository\jakarta\validation\jakarta.validation-api\3.0.2\jakarta.validation-api-3.0.2.jar;C
:\Users\Administrator\.m2\repository\org\apache\commons\commons-lang3\3.20.0\commons-lang3-3.20.0.jar;"/>
    <property name="java.vm.vendor" value="Eclipse Adoptium"/>
    <property name="sun.arch.data.model" value="64"/>
    <property name="user.variant" value=""/>
    <property name="java.vendor.url" value="https://adoptium.net/"/>
    <property name="user.timezone" value="Asia/Shanghai"/>
    <property name="os.name" value="Windows 10"/>
    <property name="java.vm.specification.version" value="17"/>
    <property name="sun.java.launcher" value="SUN_STANDARD"/>
    <property name="user.country" value="CN"/>
    <property name="sun.boot.library.path" value="E:\����֮��AI����ƽ̨����\tools\jdk-17.0.20.1+1\bin"/>
    <property name="sun.java.command" value="C:\Users\Administrator\AppData\Local\Temp\surefire12053511136342888388\sur
efirebooter-20260912135129227_23.jar C:\Users\Administrator\AppData\Local\Temp\surefire12053511136342888388 2026-09-12T
13-51-22_032-jvmRun1 surefire-20260912135129227_21tmp surefire_4-20260912135129227_22tmp"/>
    <property name="jdk.debug" value="release"/>
    <property name="surefire.test.class.path" value="E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-
spring-boot-starter-protection\target\test-classes;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-sp
ring-boot-starter-protection\target\classes;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-bo
ot-starter-web\target\classes;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-common\target\classes;C
:\Users\Administrator\.m2\repository\io\opentelemetry\opentelemetry-api\1.65.0\opentelemetry-api-1.65.0.jar;C:\Users\Ad
ministrator\.m2\repository\io\opentelemetry\opentelemetry-context\1.49.0\opentelemetry-context-1.49.0.jar;C:\Users\Admi
nistrator\.m2\repository\org\projectlombok\lombok\1.18.46\lombok-1.18.46.jar;C:\Users\Administrator\.m2\repository\org\
mapstruct\mapstruct\1.6.3\mapstruct-1.6.3.jar;C:\Users\Administrator\.m2\repository\org\mapstruct\mapstruct-jdk8\1.6.3\
mapstruct-jdk8-1.6.3.jar;C:\Users\Administrator\.m2\repository\org\mapstruct\mapstruct-processor\1.6.3\mapstruct-proces
sor-1.6.3.jar;C:\Users\Administrator\.m2\repository\cn\hutool\hutool-all\5.8.47\hutool-all-5.8.47.jar;C:\Users\Administ
rator\.m2\repository\com\alibaba\transmittable-thread-local\2.14.5\transmittable-thread-local-2.14.5.jar;C:\Users\Admin
istrator\.m2\repository\com\alibaba\fastjson\2.0.64\fastjson-2.0.64.jar;C:\Users\Administrator\.m2\repository\com\aliba
ba\fastjson2\fastjson2-extension\2.0.64\fastjson2-extension-2.0.64.jar;C:\Users\Administrator\.m2\repository\com\alibab
a\fastjson2\fastjson2\2.0.64\fastjson2-2.0.64.jar;C:\Users\Administrator\.m2\repository\org\dromara\easy-trans-anno\3.1
.8\easy-trans-anno-3.1.8.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-web\3.5
.15\spring-boot-starter-web-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-start
er\3.5.15\spring-boot-starter-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot\3.5
.15\spring-boot-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-autoconfigure\3.5
.15\spring-boot-autoconfigure-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-sta
rter-logging\3.5.15\spring-boot-starter-logging-3.5.15.jar;C:\Users\Administrator\.m2\repository\ch\qos\logback\logback
-classic\1.5.34\logback-classic-1.5.34.jar;C:\Users\Administrator\.m2\repository\ch\qos\logback\logback-core\1.5.34\log
back-core-1.5.34.jar;C:\Users\Administrator\.m2\repository\org\apache\logging\log4j\log4j-to-slf4j\2.24.3\log4j-to-slf4
j-2.24.3.jar;C:\Users\Administrator\.m2\repository\org\apache\logging\log4j\log4j-api\2.24.3\log4j-api-2.24.3.jar;C:\Us
ers\Administrator\.m2\repository\org\slf4j\jul-to-slf4j\2.0.18\jul-to-slf4j-2.0.18.jar;C:\Users\Administrator\.m2\repos
itory\jakarta\annotation\jakarta.annotation-api\2.1.1\jakarta.annotation-api-2.1.1.jar;C:\Users\Administrator\.m2\repos
itory\org\yaml\snakeyaml\2.4\snakeyaml-2.4.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-bo
ot-starter-json\3.5.15\spring-boot-starter-json-3.5.15.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\
datatype\jackson-datatype-jdk8\2.21.4\jackson-datatype-jdk8-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\faster
xml\jackson\module\jackson-module-parameter-names\2.21.4\jackson-module-parameter-names-2.21.4.jar;C:\Users\Administrat
or\.m2\repository\org\springframework\boot\spring-boot-starter-tomcat\3.5.15\spring-boot-starter-tomcat-3.5.15.jar;C:\U
sers\Administrator\.m2\repository\org\apache\tomcat\embed\tomcat-embed-core\10.1.55\tomcat-embed-core-10.1.55.jar;C:\Us
ers\Administrator\.m2\repository\org\apache\tomcat\embed\tomcat-embed-el\10.1.55\tomcat-embed-el-10.1.55.jar;C:\Users\A
dministrator\.m2\repository\org\apache\tomcat\embed\tomcat-embed-websocket\10.1.55\tomcat-embed-websocket-10.1.55.jar;C
:\Users\Administrator\.m2\repository\org\springframework\spring-web\6.2.19\spring-web-6.2.19.jar;C:\Users\Administrator
\.m2\repository\org\springframework\spring-beans\6.2.19\spring-beans-6.2.19.jar;C:\Users\Administrator\.m2\repository\i
o\micrometer\micrometer-observation\1.15.12\micrometer-observation-1.15.12.jar;C:\Users\Administrator\.m2\repository\io
\micrometer\micrometer-commons\1.15.12\micrometer-commons-1.15.12.jar;C:\Users\Administrator\.m2\repository\org\springf
ramework\spring-webmvc\6.2.19\spring-webmvc-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring
-aop\6.2.19\spring-aop-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-context\6.2.19\sprin
g-context-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-expression\6.2.19\spring-expressi
on-6.2.19.jar;C:\Users\Administrator\.m2\repository\com\github\xiaoymin\knife4j-openapi3-jakarta-spring-boot-starter\4.
5.0\knife4j-openapi3-jakarta-spring-boot-starter-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\github\xiaoymin\kn
ife4j-core\4.5.0\knife4j-core-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\github\xiaoymin\knife4j-openapi3-ui\4
.5.0\knife4j-openapi3-ui-4.5.0.jar;C:\Users\Administrator\.m2\repository\org\springdoc\springdoc-openapi-starter-webmvc
-ui\2.8.17\springdoc-openapi-starter-webmvc-ui-2.8.17.jar;C:\Users\Administrator\.m2\repository\org\springdoc\springdoc
-openapi-starter-webmvc-api\2.8.17\springdoc-openapi-starter-webmvc-api-2.8.17.jar;C:\Users\Administrator\.m2\repositor
y\org\springdoc\springdoc-openapi-starter-common\2.8.17\springdoc-openapi-starter-common-2.8.17.jar;C:\Users\Administra
tor\.m2\repository\org\springframework\boot\spring-boot-starter-validation\3.5.15\spring-boot-starter-validation-3.5.15
.jar;C:\Users\Administrator\.m2\repository\org\hibernate\validator\hibernate-validator\8.0.3.Final\hibernate-validator-
8.0.3.Final.jar;C:\Users\Administrator\.m2\repository\org\jboss\logging\jboss-logging\3.6.3.Final\jboss-logging-3.6.3.F
inal.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\classmate\1.7.3\classmate-1.7.3.jar;C:\Users\Administrator
\.m2\repository\io\swagger\core\v3\swagger-core-jakarta\2.2.47\swagger-core-jakarta-2.2.47.jar;C:\Users\Administrator\.
m2\repository\io\swagger\core\v3\swagger-annotations-jakarta\2.2.47\swagger-annotations-jakarta-2.2.47.jar;C:\Users\Adm
inistrator\.m2\repository\io\swagger\core\v3\swagger-models-jakarta\2.2.47\swagger-models-jakarta-2.2.47.jar;C:\Users\A
dministrator\.m2\repository\com\fasterxml\jackson\dataformat\jackson-dataformat-yaml\2.21.4\jackson-dataformat-yaml-2.2
1.4.jar;C:\Users\Administrator\.m2\repository\org\webjars\swagger-ui\5.32.2\swagger-ui-5.32.2.jar;C:\Users\Administrato
r\.m2\repository\org\webjars\webjars-locator-lite\1.1.3\webjars-locator-lite-1.1.3.jar;C:\Users\Administrator\.m2\repos
itory\org\jspecify\jspecify\1.0.0\jspecify-1.0.0.jar;C:\Users\Administrator\.m2\repository\org\jsoup\jsoup\1.23.2\jsoup
-1.23.2.jar;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-redis\target\classes;
C:\Users\Administrator\.m2\repository\org\redisson\redisson-spring-boot-starter\4.7.0\redisson-spring-boot-starter-4.7.
0.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-data-redis\3.5.15\spring-boot-
starter-data-redis-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\redisson\redisson\4.7.0\redisson-4.7.0.jar;C:\U
sers\Administrator\.m2\repository\io\netty\netty-common\4.2.17.Final\netty-common-4.2.17.Final.jar;C:\Users\Administrat
or\.m2\repository\io\netty\netty-codec\4.2.17.Final\netty-codec-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\
io\netty\netty-codec-base\4.2.17.Final\netty-codec-base-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty
\netty-codec-compression\4.2.17.Final\netty-codec-compression-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io
\netty\netty-codec-protobuf\4.2.17.Final\netty-codec-protobuf-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io
\netty\netty-codec-marshalling\4.2.17.Final\netty-codec-marshalling-4.2.17.Final.jar;C:\Users\Administrator\.m2\reposit
ory\io\netty\netty-buffer\4.2.17.Final\netty-buffer-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\net
ty-transport\4.2.17.Final\netty-transport-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-resolve
r\4.2.17.Final\netty-resolver-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-resolver-dns\4.2.17
.Final\netty-resolver-dns-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-codec-dns\4.2.17.Final\
netty-codec-dns-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-handler\4.2.17.Final\netty-handle
r-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-transport-native-unix-common\4.2.17.Final\netty
-transport-native-unix-common-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\javax\cache\cache-api\1.1.1\cache-
api-1.1.1.jar;C:\Users\Administrator\.m2\repository\io\projectreactor\reactor-core\3.7.19\reactor-core-3.7.19.jar;C:\Us
ers\Administrator\.m2\repository\org\reactivestreams\reactive-streams\1.0.4\reactive-streams-1.0.4.jar;C:\Users\Adminis
trator\.m2\repository\io\reactivex\rxjava3\rxjava\3.1.12\rxjava-3.1.12.jar;C:\Users\Administrator\.m2\repository\com\es
otericsoftware\kryo\5.6.2\kryo-5.6.2.jar;C:\Users\Administrator\.m2\repository\com\esotericsoftware\reflectasm\1.11.9\r
eflectasm-1.11.9.jar;C:\Users\Administrator\.m2\repository\com\esotericsoftware\minlog\1.3.1\minlog-1.3.1.jar;C:\Users\
Administrator\.m2\repository\net\bytebuddy\byte-buddy\1.17.8\byte-buddy-1.17.8.jar;C:\Users\Administrator\.m2\repositor
y\org\jodd\jodd-util\6.3.0\jodd-util-6.3.0.jar;C:\Users\Administrator\.m2\repository\org\redisson\redisson-spring-data-
35\4.7.0\redisson-spring-data-35-4.7.0.jar;C:\Users\Administrator\.m2\repository\org\springframework\data\spring-data-r
edis\3.5.12\spring-data-redis-3.5.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\data\spring-data-key
value\3.5.12\spring-data-keyvalue-3.5.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\data\spring-data
-commons\3.5.12\spring-data-commons-3.5.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-tx\6.2.
19\spring-tx-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-oxm\6.2.19\spring-oxm-6.2.19.j
ar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-cache\3.5.15\spring-boot-starter-
cache-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-context-support\6.2.19\spring-context
-support-6.2.19.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\datatype\jackson-datatype-jsr310\2.21.4
\jackson-datatype-jsr310-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\core\jackson-annotation
s\2.21\jackson-annotations-2.21.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\core\jackson-core\2.21.
4\jackson-core-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\core\jackson-databind\2.21.4\jack
son-databind-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\baomidou\lock4j-redisson-spring-boot-starter\2.2.7\lo
ck4j-redisson-spring-boot-starter-2.2.7.jar;C:\Users\Administrator\.m2\repository\com\baomidou\lock4j-core\2.2.7\lock4j
-core-2.2.7.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-aop\3.5.15\spring-bo
ot-starter-aop-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\aspectj\aspectjweaver\1.9.25.1\aspectjweaver-1.9.25
.1.jar;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-test\target\classes;E:\���
�֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-mybatis\target\classes;C:\Users\Adminis
trator\.m2\repository\com\alibaba\druid-spring-boot-3-starter\1.2.28\druid-spring-boot-3-starter-1.2.28.jar;C:\Users\Ad
ministrator\.m2\repository\com\alibaba\druid\1.2.28\druid-1.2.28.jar;C:\Users\Administrator\.m2\repository\com\baomidou
\mybatis-plus-spring-boot3-starter\3.5.17\mybatis-plus-spring-boot3-starter-3.5.17.jar;C:\Users\Administrator\.m2\repos
itory\com\baomidou\mybatis-plus\3.5.17\mybatis-plus-3.5.17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybat
is-plus-core\3.5.17\mybatis-plus-core-3.5.17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-annota
tion\3.5.17\mybatis-plus-annotation-3.5.17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-spring\3
.5.17\mybatis-plus-spring-3.5.17.jar;C:\Users\Administrator\.m2\repository\org\mybatis\mybatis\3.5.19\mybatis-3.5.19.ja
r;C:\Users\Administrator\.m2\repository\org\mybatis\mybatis-spring\3.0.5\mybatis-spring-3.0.5.jar;C:\Users\Administrato
r\.m2\repository\com\baomidou\mybatis-plus-spring-boot-autoconfigure\3.5.17\mybatis-plus-spring-boot-autoconfigure-3.5.
17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-spring-boot-native-image\3.5.17\mybatis-plus-spr
ing-boot-native-image-3.5.17.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-jdb
c\3.5.15\spring-boot-starter-jdbc-3.5.15.jar;C:\Users\Administrator\.m2\repository\com\zaxxer\HikariCP\6.3.3\HikariCP-6
.3.3.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-jdbc\6.2.19\spring-jdbc-6.2.19.jar;C:\Users\A
dministrator\.m2\repository\com\baomidou\mybatis-plus-jsqlparser\3.5.17\mybatis-plus-jsqlparser-3.5.17.jar;C:\Users\Adm
inistrator\.m2\repository\com\github\jsqlparser\jsqlparser\5.2\jsqlparser-5.2.jar;C:\Users\Administrator\.m2\repository
\com\baomidou\mybatis-plus-jsqlparser-common\3.5.17\mybatis-plus-jsqlparser-common-3.5.17.jar;C:\Users\Administrator\.m
2\repository\com\baomidou\mybatis-plus-extension\3.5.17\mybatis-plus-extension-3.5.17.jar;C:\Users\Administrator\.m2\re
pository\com\baomidou\dynamic-datasource-spring-boot3-starter\4.5.0\dynamic-datasource-spring-boot3-starter-4.5.0.jar;C
:\Users\Administrator\.m2\repository\com\baomidou\dynamic-datasource-spring-boot-common\4.5.0\dynamic-datasource-spring
-boot-common-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\baomidou\dynamic-datasource-spring\4.5.0\dynamic-datas
ource-spring-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\baomidou\dynamic-datasource-creator\4.5.0\dynamic-data
source-creator-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\oracle\database\spring\oracle-spring-boot-starter-uc
p\23.4.0\oracle-spring-boot-starter-ucp-23.4.0.jar;C:\Users\Administrator\.m2\repository\com\oracle\database\jdbc\ojdbc
11\23.7.0.25.01\ojdbc11-23.7.0.25.01.jar;C:\Users\Administrator\.m2\repository\com\oracle\database\jdbc\ucp\23.7.0.25.0
1\ucp-23.7.0.25.01.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-join-boot-starter\1.5.9\
mybatis-plus-join-boot-starter-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-join-e
xtension\1.5.9\mybatis-plus-join-extension-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis
-plus-join-core\1.5.9\mybatis-plus-join-core-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybat
is-plus-join-annotation\1.5.9\mybatis-plus-join-annotation-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\y
ulichang\mybatis-plus-join-adapter-base\1.5.9\mybatis-plus-join-adapter-base-1.5.9.jar;C:\Users\Administrator\.m2\repos
itory\com\github\yulichang\mybatis-plus-join-adapter-jsqlparser\1.5.9\mybatis-plus-join-adapter-jsqlparser-1.5.9.jar;C:
\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-join-wrapper-ext\1.5.9\mybatis-plus-join-wrapper-
ext-1.5.9.jar;C:\Users\Administrator\.m2\repository\org\dromara\easy-trans-spring-boot-starter\3.1.8\easy-trans-spring-
boot-starter-3.1.8.jar;C:\Users\Administrator\.m2\repository\org\dromara\easy-trans-service\3.1.8\easy-trans-service-3.
1.8.jar;C:\Users\Administrator\.m2\repository\com\github\ben-manes\caffeine\caffeine\3.2.4\caffeine-3.2.4.jar;C:\Users\
Administrator\.m2\repository\org\dromara\easy-trans-mybatis-plus-extend\3.1.8\easy-trans-mybatis-plus-extend-3.1.8.jar;
C:\Users\Administrator\.m2\repository\org\mockito\mockito-inline\5.2.0\mockito-inline-5.2.0.jar;C:\Users\Administrator\
.m2\repository\org\mockito\mockito-core\5.17.0\mockito-core-5.17.0.jar;C:\Users\Administrator\.m2\repository\net\bytebu
ddy\byte-buddy-agent\1.17.8\byte-buddy-agent-1.17.8.jar;C:\Users\Administrator\.m2\repository\org\objenesis\objenesis\3
.3\objenesis-3.3.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-test\3.5.15\spr
ing-boot-starter-test-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-test\3.5.15
\spring-boot-test-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-test-autoconfig
ure\3.5.15\spring-boot-test-autoconfigure-3.5.15.jar;C:\Users\Administrator\.m2\repository\com\jayway\jsonpath\json-pat
h\2.9.0\json-path-2.9.0.jar;C:\Users\Administrator\.m2\repository\jakarta\xml\bind\jakarta.xml.bind-api\4.0.5\jakarta.x
ml.bind-api-4.0.5.jar;C:\Users\Administrator\.m2\repository\jakarta\activation\jakarta.activation-api\2.1.4\jakarta.act
ivation-api-2.1.4.jar;C:\Users\Administrator\.m2\repository\net\minidev\json-smart\2.5.2\json-smart-2.5.2.jar;C:\Users\
Administrator\.m2\repository\net\minidev\accessors-smart\2.5.2\accessors-smart-2.5.2.jar;C:\Users\Administrator\.m2\rep
ository\org\assertj\assertj-core\3.27.7\assertj-core-3.27.7.jar;C:\Users\Administrator\.m2\repository\org\awaitility\aw
aitility\4.2.2\awaitility-4.2.2.jar;C:\Users\Administrator\.m2\repository\org\hamcrest\hamcrest\3.0\hamcrest-3.0.jar;C:
\Users\Administrator\.m2\repository\org\junit\jupiter\junit-jupiter\5.12.2\junit-jupiter-5.12.2.jar;C:\Users\Administra
tor\.m2\repository\org\junit\jupiter\junit-jupiter-api\5.12.2\junit-jupiter-api-5.12.2.jar;C:\Users\Administrator\.m2\r
epository\org\opentest4j\opentest4j\1.3.0\opentest4j-1.3.0.jar;C:\Users\Administrator\.m2\repository\org\junit\platform
\junit-platform-commons\1.12.2\junit-platform-commons-1.12.2.jar;C:\Users\Administrator\.m2\repository\org\apiguardian\
apiguardian-api\1.1.2\apiguardian-api-1.1.2.jar;C:\Users\Administrator\.m2\repository\org\junit\jupiter\junit-jupiter-p
arams\5.12.2\junit-jupiter-params-5.12.2.jar;C:\Users\Administrator\.m2\repository\org\junit\jupiter\junit-jupiter-engi
ne\5.12.2\junit-jupiter-engine-5.12.2.jar;C:\Users\Administrator\.m2\repository\org\junit\platform\junit-platform-engin
e\1.12.2\junit-platform-engine-1.12.2.jar;C:\Users\Administrator\.m2\repository\org\mockito\mockito-junit-jupiter\5.17.
0\mockito-junit-jupiter-5.17.0.jar;C:\Users\Administrator\.m2\repository\org\skyscreamer\jsonassert\1.5.3\jsonassert-1.
5.3.jar;C:\Users\Administrator\.m2\repository\com\vaadin\external\google\android-json\0.0.20131108.vaadin1\android-json
-0.0.20131108.vaadin1.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-core\6.2.19\spring-core-6.2.
19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-jcl\6.2.19\spring-jcl-6.2.19.jar;C:\Users\Admin
istrator\.m2\repository\org\springframework\spring-test\6.2.19\spring-test-6.2.19.jar;C:\Users\Administrator\.m2\reposi
tory\org\xmlunit\xmlunit-core\2.10.4\xmlunit-core-2.10.4.jar;C:\Users\Administrator\.m2\repository\com\h2database\h2\2.
3.232\h2-2.3.232.jar;C:\Users\Administrator\.m2\repository\com\github\fppt\jedis-mock\1.1.18\jedis-mock-1.1.18.jar;C:\U
sers\Administrator\.m2\repository\org\slf4j\slf4j-api\2.0.18\slf4j-api-2.0.18.jar;C:\Users\Administrator\.m2\repository
\org\luaj\luaj-jse\3.0.1\luaj-jse-3.0.1.jar;C:\Users\Administrator\.m2\repository\redis\clients\jedis\6.0.0\jedis-6.0.0
.jar;C:\Users\Administrator\.m2\repository\org\apache\commons\commons-pool2\2.12.1\commons-pool2-2.12.1.jar;C:\Users\Ad
ministrator\.m2\repository\org\json\json\20250107\json-20250107.jar;C:\Users\Administrator\.m2\repository\com\google\co
de\gson\gson\2.13.2\gson-2.13.2.jar;C:\Users\Administrator\.m2\repository\com\google\errorprone\error_prone_annotations
\2.41.0\error_prone_annotations-2.41.0.jar;C:\Users\Administrator\.m2\repository\redis\clients\authentication\redis-aut
hx-core\0.1.1-beta2\redis-authx-core-0.1.1-beta2.jar;C:\Users\Administrator\.m2\repository\uk\co\jemos\podam\podam\8.0.
2.RELEASE\podam-8.0.2.RELEASE.jar;C:\Users\Administrator\.m2\repository\net\jcip\jcip-annotations\1.0\jcip-annotations-
1.0.jar;C:\Users\Administrator\.m2\repository\jakarta\validation\jakarta.validation-api\3.0.2\jakarta.validation-api-3.
0.2.jar;C:\Users\Administrator\.m2\repository\org\apache\commons\commons-lang3\3.20.0\commons-lang3-3.20.0.jar;"/>
    <property name="sun.cpu.endian" value="little"/>
    <property name="user.home" value="C:\Users\Administrator"/>
    <property name="user.language" value="zh"/>
    <property name="java.specification.vendor" value="Oracle Corporation"/>
    <property name="java.version.date" value="2026-08-18"/>
    <property name="java.home" value="E:\����֮��AI����ƽ̨����\tools\jdk-17.0.20.1+1"/>
    <property name="file.separator" value="\"/>
    <property name="basedir" value="E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-start
er-protection"/>
    <property name="java.vm.compressedOopsMode" value="Zero based"/>
    <property name="line.separator" value="&#10;"/>
    <property name="java.vm.specification.vendor" value="Oracle Corporation"/>
    <property name="java.specification.name" value="Java Platform API Specification"/>
    <property name="surefire.real.class.path" value="C:\Users\Administrator\AppData\Local\Temp\surefire1205351113634288
8388\surefirebooter-20260912135129227_23.jar"/>
    <property name="user.script" value=""/>
    <property name="sun.management.compiler" value="HotSpot 64-Bit Tiered Compilers"/>
    <property name="java.runtime.version" value="17.0.20.1+1"/>
    <property name="user.name" value="Administrator"/>
    <property name="path.separator" value=";"/>
    <property name="os.version" value="10.0"/>
    <property name="java.runtime.name" value="OpenJDK Runtime Environment"/>
    <property name="file.encoding" value="GBK"/>
    <property name="java.vm.name" value="OpenJDK 64-Bit Server VM"/>
    <property name="java.vendor.version" value="Temurin-17.0.20.1+1"/>
    <property name="localRepository" value="C:\Users\Administrator\.m2\repository"/>
    <property name="java.vendor.url.bug" value="https://github.com/adoptium/adoptium-support/issues"/>
    <property name="java.io.tmpdir" value="C:\Users\ADMINI~1\AppData\Local\Temp\"/>
    <property name="java.version" value="17.0.20.1"/>
    <property name="user.dir" value="E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-star
ter-protection"/>
    <property name="os.arch" value="amd64"/>
    <property name="java.vm.specification.name" value="Java Virtual Machine Specification"/>
    <property name="sun.os.patch.level" value=""/>
    <property name="native.encoding" value="GBK"/>
    <property name="java.library.path" value="E:\����֮��AI����ƽ̨����\tools\jdk-17.0.20.1+1\bin;C:\windows\Sun\Java\bin;
C:\windows\system32;C:\windows;E:\����֮��AI����ƽ̨����\tools\jdk-17.0.20.1+1\bin;E:\����֮��AI����ƽ̨����\tools\apache-mav
en-3.9.9\bin;C:\Program Files\Python311\Scripts\;C:\Program Files\Python311\;C:\windows\system32;C:\windows;C:\windows\
System32\Wbem;C:\windows\System32\WindowsPowerShell\v1.0\;C:\windows\System32\OpenSSH\;D:\Program Files (x86)\Tencent\΢
��web�����߹���;;C:\Program Files\Git\cmd;C:\Program Files\nodejs\;C:\Users\Administrator\AppData\Local\Microsoft\Window
sApps;;C:\Users\Administrator\AppData\Local\Programs\DockerDesktop\resources\bin;C:\Users\Administrator\AppData\Roaming
\npm;D:\Program Files\Qoder IDE\bin;C:\Users\Administrator\AppData\Local\Microsoft\WinGet\Links;;."/>
    <property name="java.vm.info" value="mixed mode, sharing"/>
    <property name="java.vendor" value="Eclipse Adoptium"/>
    <property name="java.vm.version" value="17.0.20.1+1"/>
    <property name="java.specification.maintenance.version" value="1"/>
    <property name="sun.io.unicode.encoding" value="UnicodeLittle"/>
    <property name="java.class.version" value="61.0"/>
  </properties>
  <testcase name="testBeforePointCut_tooManyRequests_scalarRefreshTokenMasked" classname="cn.zszj.framework.ratelimiter
.core.aop.RateLimiterAspectTest" time="0.093">
    <system-out><![CDATA[13:51:31.046 [main] INFO cn.zszj.framework.ratelimiter.core.aop.RateLimiterAspect -- [beforePo
intCut][����(Mock for MethodSignature, hashCode: 1515477775) ����({"refreshToken":"***"}) �������Ƶ��]
]]></system-out>
  </testcase>
  <testcase name="testBeforePointCut_tooManyRequests_argsSanitizedInLog" classname="cn.zszj.framework.ratelimiter.core.
aop.RateLimiterAspectTest" time="0.005">
    <system-out><![CDATA[13:51:31.052 [main] INFO cn.zszj.framework.ratelimiter.core.aop.RateLimiterAspect -- [beforePo
intCut][����(UserService.submitOrder(..)) ����({"arg0":{"username":"zhangsan-user","password":"***","token":"***","nest
ed":{"apiKey":"***","note":"hello-note"},"list":[{"secret":"***"}]}}) �������Ƶ��]
]]></system-out>
  </testcase>
  <testcase name="testBeforePointCut_allowed_noRejectLog" classname="cn.zszj.framework.ratelimiter.core.aop.RateLimiter
AspectTest" time="0.002"/>
  <testcase name="testBeforePointCut_tooManyRequests_smsCodeMaskedViaMaskKeys" classname="cn.zszj.framework.ratelimiter
.core.aop.RateLimiterAspectTest" time="0.003">
    <system-out><![CDATA[13:51:31.058 [main] INFO cn.zszj.framework.ratelimiter.core.aop.RateLimiterAspect -- [beforePo
intCut][����(Mock for MethodSignature, hashCode: 1780414044) ����({"reqVO":{"mobile":"13800138000","code":"***"}}) ����
���Ƶ��]
]]></system-out>
  </testcase>
  <testcase name="testBeforePointCut_redisDown_shouldFailOpenByDefault" classname="cn.zszj.framework.ratelimiter.core.a
op.RateLimiterAspectTest" time="0.166">
    <system-out><![CDATA[13:51:31.224 [main] ERROR cn.zszj.framework.ratelimiter.core.redis.RateLimiterRedisDAO -- [try
Acquire][���� Key(rate-fixed-key) Redis ���ϣ��� fail-open ����]
java.lang.RuntimeException: Redis connection refused
	at cn.zszj.framework.ratelimiter.core.redis.RateLimiterRedisDAO.getRRateLimiter(RateLimiterRedisDAO.java:51)
	at cn.zszj.framework.ratelimiter.core.redis.RateLimiterRedisDAO.tryAcquire(RateLimiterRedisDAO.java:34)
	at cn.zszj.framework.ratelimiter.core.aop.RateLimiterAspect.beforePointCut(RateLimiterAspect.java:54)
	at cn.zszj.framework.ratelimiter.core.aop.RateLimiterAspectTest.lambda$testBeforePointCut_redisDown_shouldFailOpenByDe
fault$3(RateLimiterAspectTest.java:225)
	at org.junit.jupiter.api.AssertDoesNotThrow.assertDoesNotThrow(AssertDoesNotThrow.java:49)
	at org.junit.jupiter.api.AssertDoesNotThrow.assertDoesNotThrow(AssertDoesNotThrow.java:40)
	at org.junit.jupiter.api.Assertions.assertDoesNotThrow(Assertions.java:3218)
	at cn.zszj.framework.ratelimiter.core.aop.RateLimiterAspectTest.testBeforePointCut_redisDown_shouldFailOpenByDefault(R
ateLimiterAspectTest.java:225)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.base/java.lang.reflect.Method.invoke(Method.java:569)
	at org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:775)
	at org.junit.platform.commons.support.ReflectionSupport.invokeMethod(ReflectionSupport.java:479)
	at org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
	at org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorCha
in.java:131)
	at org.junit.jupiter.engine.extension.TimeoutExtension.intercept(TimeoutExtension.java:161)
	at org.junit.jupiter.engine.extension.TimeoutExtension.interceptTestableMethod(TimeoutExtension.java:152)
	at org.junit.jupiter.engine.extension.TimeoutExtension.interceptTestMethod(TimeoutExtension.java:91)
	at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker$ReflectiveInterceptorCall.lambda$ofVoidMethod$0(In
terceptingExecutableInvoker.java:112)
	at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.lambda$invoke$0(InterceptingExecutableInvoker.java
:94)
	at org.junit.jupiter.engine.execution.InvocationInterceptorChain$InterceptedInvocation.proceed(InvocationInterceptorCh
ain.java:106)
	at org.junit.jupiter.engine.execution.InvocationInterceptorChain.proceed(InvocationInterceptorChain.java:64)
	at org.junit.jupiter.engine.execution.InvocationInterceptorChain.chainAndInvoke(InvocationInterceptorChain.java:45)
	at org.junit.jupiter.engine.execution.InvocationInterceptorChain.invoke(InvocationInterceptorChain.java:37)
	at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.invoke(InterceptingExecutableInvoker.java:93)
	at org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.invoke(InterceptingExecutableInvoker.java:87)
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$invokeTestMethod$7(TestMethodTestDescriptor.jav
a:216)
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.invokeTestMethod(TestMethodTestDescriptor.java:212)
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.execute(TestMethodTestDescriptor.java:137)
	at org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.execute(TestMethodTestDescriptor.java:69)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:156)
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:146)
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:144)
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:143)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:100)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1511)
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierar
chicalTestExecutorService.java:41)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:160)
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:146)
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:144)
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:143)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:100)
	at java.base/java.util.ArrayList.forEach(ArrayList.java:1511)
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierar
chicalTestExecutorService.java:41)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:160)
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:146)
	at org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:144)
	at org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:143)
	at org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:100)
	at org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchi
calTestExecutorService.java:35)
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57)
	at org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54)
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:201)
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:170)
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:94)
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:59)
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.jav
a:142)
	at org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:58)
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:103)
	at org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:85)
	at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47)
	at org.junit.platform.launcher.core.InterceptingLauncher.lambda$execute$1(InterceptingLauncher.java:39)
	at org.junit.platform.launcher.core.ClasspathAlignmentCheckingLauncherInterceptor.intercept(ClasspathAlignmentChecking
LauncherInterceptor.java:25)
	at org.junit.platform.launcher.core.InterceptingLauncher.execute(InterceptingLauncher.java:38)
	at org.junit.platform.launcher.core.DelegatingLauncher.execute(DelegatingLauncher.java:47)
	at org.apache.maven.surefire.junitplatform.LazyLauncher.execute(LazyLauncher.java:56)
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.execute(JUnitPlatformProvider.java:194)
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invokeAllTests(JUnitPlatformProvider.java:150)
	at org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.invoke(JUnitPlatformProvider.java:124)
	at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:385)
	at org.apache.maven.surefire.booter.ForkedBooter.execute(ForkedBooter.java:162)
	at org.apache.maven.surefire.booter.ForkedBooter.run(ForkedBooter.java:507)
	at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:495)
]]></system-out>
  </testcase>
</testsuite>
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\TEST-cn.zszj.framework.ratelimiter.core.keyresolver.impl.ExpressionRateLimiterKeyResolverTest.xml
<?xml version="1.0" encoding="UTF-8"?>
<testsuite xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="https://maven.apache.or
g/surefire/maven-surefire-plugin/xsd/surefire-test-report.xsd" version="3.0.2" name="cn.zszj.framework.ratelimiter.core
.keyresolver.impl.ExpressionRateLimiterKeyResolverTest" time="0.079" tests="3" errors="0" skipped="0" failures="0">
  <properties>
    <property name="java.specification.version" value="17"/>
    <property name="sun.cpu.isalist" value="amd64"/>
    <property name="sun.jnu.encoding" value="GBK"/>
    <property name="java.class.path" value="E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-bo
ot-starter-protection\target\test-classes;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot
-starter-protection\target\classes;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starte
r-web\target\classes;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-common\target\classes;C:\Users\A
dministrator\.m2\repository\io\opentelemetry\opentelemetry-api\1.65.0\opentelemetry-api-1.65.0.jar;C:\Users\Administrat
or\.m2\repository\io\opentelemetry\opentelemetry-context\1.49.0\opentelemetry-context-1.49.0.jar;C:\Users\Administrator
\.m2\repository\org\projectlombok\lombok\1.18.46\lombok-1.18.46.jar;C:\Users\Administrator\.m2\repository\org\mapstruct
\mapstruct\1.6.3\mapstruct-1.6.3.jar;C:\Users\Administrator\.m2\repository\org\mapstruct\mapstruct-jdk8\1.6.3\mapstruct
-jdk8-1.6.3.jar;C:\Users\Administrator\.m2\repository\org\mapstruct\mapstruct-processor\1.6.3\mapstruct-processor-1.6.3
.jar;C:\Users\Administrator\.m2\repository\cn\hutool\hutool-all\5.8.47\hutool-all-5.8.47.jar;C:\Users\Administrator\.m2
\repository\com\alibaba\transmittable-thread-local\2.14.5\transmittable-thread-local-2.14.5.jar;C:\Users\Administrator\
.m2\repository\com\alibaba\fastjson\2.0.64\fastjson-2.0.64.jar;C:\Users\Administrator\.m2\repository\com\alibaba\fastjs
on2\fastjson2-extension\2.0.64\fastjson2-extension-2.0.64.jar;C:\Users\Administrator\.m2\repository\com\alibaba\fastjso
n2\fastjson2\2.0.64\fastjson2-2.0.64.jar;C:\Users\Administrator\.m2\repository\org\dromara\easy-trans-anno\3.1.8\easy-t
rans-anno-3.1.8.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-web\3.5.15\sprin
g-boot-starter-web-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter\3.5.15
\spring-boot-starter-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot\3.5.15\sprin
g-boot-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-autoconfigure\3.5.15\sprin
g-boot-autoconfigure-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-logg
ing\3.5.15\spring-boot-starter-logging-3.5.15.jar;C:\Users\Administrator\.m2\repository\ch\qos\logback\logback-classic\
1.5.34\logback-classic-1.5.34.jar;C:\Users\Administrator\.m2\repository\ch\qos\logback\logback-core\1.5.34\logback-core
-1.5.34.jar;C:\Users\Administrator\.m2\repository\org\apache\logging\log4j\log4j-to-slf4j\2.24.3\log4j-to-slf4j-2.24.3.
jar;C:\Users\Administrator\.m2\repository\org\apache\logging\log4j\log4j-api\2.24.3\log4j-api-2.24.3.jar;C:\Users\Admin
istrator\.m2\repository\org\slf4j\jul-to-slf4j\2.0.18\jul-to-slf4j-2.0.18.jar;C:\Users\Administrator\.m2\repository\jak
arta\annotation\jakarta.annotation-api\2.1.1\jakarta.annotation-api-2.1.1.jar;C:\Users\Administrator\.m2\repository\org
\yaml\snakeyaml\2.4\snakeyaml-2.4.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starte
r-json\3.5.15\spring-boot-starter-json-3.5.15.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\datatype\
jackson-datatype-jdk8\2.21.4\jackson-datatype-jdk8-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jacks
on\module\jackson-module-parameter-names\2.21.4\jackson-module-parameter-names-2.21.4.jar;C:\Users\Administrator\.m2\re
pository\org\springframework\boot\spring-boot-starter-tomcat\3.5.15\spring-boot-starter-tomcat-3.5.15.jar;C:\Users\Admi
nistrator\.m2\repository\org\apache\tomcat\embed\tomcat-embed-core\10.1.55\tomcat-embed-core-10.1.55.jar;C:\Users\Admin
istrator\.m2\repository\org\apache\tomcat\embed\tomcat-embed-el\10.1.55\tomcat-embed-el-10.1.55.jar;C:\Users\Administra
tor\.m2\repository\org\apache\tomcat\embed\tomcat-embed-websocket\10.1.55\tomcat-embed-websocket-10.1.55.jar;C:\Users\A
dministrator\.m2\repository\org\springframework\spring-web\6.2.19\spring-web-6.2.19.jar;C:\Users\Administrator\.m2\repo
sitory\org\springframework\spring-beans\6.2.19\spring-beans-6.2.19.jar;C:\Users\Administrator\.m2\repository\io\microme
ter\micrometer-observation\1.15.12\micrometer-observation-1.15.12.jar;C:\Users\Administrator\.m2\repository\io\micromet
er\micrometer-commons\1.15.12\micrometer-commons-1.15.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\
spring-webmvc\6.2.19\spring-webmvc-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-aop\6.2.
19\spring-aop-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-context\6.2.19\spring-context
-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-expression\6.2.19\spring-expression-6.2.19
.jar;C:\Users\Administrator\.m2\repository\com\github\xiaoymin\knife4j-openapi3-jakarta-spring-boot-starter\4.5.0\knife
4j-openapi3-jakarta-spring-boot-starter-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\github\xiaoymin\knife4j-cor
e\4.5.0\knife4j-core-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\github\xiaoymin\knife4j-openapi3-ui\4.5.0\knif
e4j-openapi3-ui-4.5.0.jar;C:\Users\Administrator\.m2\repository\org\springdoc\springdoc-openapi-starter-webmvc-ui\2.8.1
7\springdoc-openapi-starter-webmvc-ui-2.8.17.jar;C:\Users\Administrator\.m2\repository\org\springdoc\springdoc-openapi-
starter-webmvc-api\2.8.17\springdoc-openapi-starter-webmvc-api-2.8.17.jar;C:\Users\Administrator\.m2\repository\org\spr
ingdoc\springdoc-openapi-starter-common\2.8.17\springdoc-openapi-starter-common-2.8.17.jar;C:\Users\Administrator\.m2\r
epository\org\springframework\boot\spring-boot-starter-validation\3.5.15\spring-boot-starter-validation-3.5.15.jar;C:\U
sers\Administrator\.m2\repository\org\hibernate\validator\hibernate-validator\8.0.3.Final\hibernate-validator-8.0.3.Fin
al.jar;C:\Users\Administrator\.m2\repository\org\jboss\logging\jboss-logging\3.6.3.Final\jboss-logging-3.6.3.Final.jar;
C:\Users\Administrator\.m2\repository\com\fasterxml\classmate\1.7.3\classmate-1.7.3.jar;C:\Users\Administrator\.m2\repo
sitory\io\swagger\core\v3\swagger-core-jakarta\2.2.47\swagger-core-jakarta-2.2.47.jar;C:\Users\Administrator\.m2\reposi
tory\io\swagger\core\v3\swagger-annotations-jakarta\2.2.47\swagger-annotations-jakarta-2.2.47.jar;C:\Users\Administrato
r\.m2\repository\io\swagger\core\v3\swagger-models-jakarta\2.2.47\swagger-models-jakarta-2.2.47.jar;C:\Users\Administra
tor\.m2\repository\com\fasterxml\jackson\dataformat\jackson-dataformat-yaml\2.21.4\jackson-dataformat-yaml-2.21.4.jar;C
:\Users\Administrator\.m2\repository\org\webjars\swagger-ui\5.32.2\swagger-ui-5.32.2.jar;C:\Users\Administrator\.m2\rep
ository\org\webjars\webjars-locator-lite\1.1.3\webjars-locator-lite-1.1.3.jar;C:\Users\Administrator\.m2\repository\org
\jspecify\jspecify\1.0.0\jspecify-1.0.0.jar;C:\Users\Administrator\.m2\repository\org\jsoup\jsoup\1.23.2\jsoup-1.23.2.j
ar;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-redis\target\classes;C:\Users\
Administrator\.m2\repository\org\redisson\redisson-spring-boot-starter\4.7.0\redisson-spring-boot-starter-4.7.0.jar;C:\
Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-data-redis\3.5.15\spring-boot-starter-d
ata-redis-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\redisson\redisson\4.7.0\redisson-4.7.0.jar;C:\Users\Admi
nistrator\.m2\repository\io\netty\netty-common\4.2.17.Final\netty-common-4.2.17.Final.jar;C:\Users\Administrator\.m2\re
pository\io\netty\netty-codec\4.2.17.Final\netty-codec-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\
netty-codec-base\4.2.17.Final\netty-codec-base-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-co
dec-compression\4.2.17.Final\netty-codec-compression-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\ne
tty-codec-protobuf\4.2.17.Final\netty-codec-protobuf-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\ne
tty-codec-marshalling\4.2.17.Final\netty-codec-marshalling-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\ne
tty\netty-buffer\4.2.17.Final\netty-buffer-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-transp
ort\4.2.17.Final\netty-transport-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-resolver\4.2.17.
Final\netty-resolver-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-resolver-dns\4.2.17.Final\ne
tty-resolver-dns-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-codec-dns\4.2.17.Final\netty-cod
ec-dns-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-handler\4.2.17.Final\netty-handler-4.2.17.
Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-transport-native-unix-common\4.2.17.Final\netty-transpor
t-native-unix-common-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\javax\cache\cache-api\1.1.1\cache-api-1.1.1
.jar;C:\Users\Administrator\.m2\repository\io\projectreactor\reactor-core\3.7.19\reactor-core-3.7.19.jar;C:\Users\Admin
istrator\.m2\repository\org\reactivestreams\reactive-streams\1.0.4\reactive-streams-1.0.4.jar;C:\Users\Administrator\.m
2\repository\io\reactivex\rxjava3\rxjava\3.1.12\rxjava-3.1.12.jar;C:\Users\Administrator\.m2\repository\com\esotericsof
tware\kryo\5.6.2\kryo-5.6.2.jar;C:\Users\Administrator\.m2\repository\com\esotericsoftware\reflectasm\1.11.9\reflectasm
-1.11.9.jar;C:\Users\Administrator\.m2\repository\com\esotericsoftware\minlog\1.3.1\minlog-1.3.1.jar;C:\Users\Administr
ator\.m2\repository\net\bytebuddy\byte-buddy\1.17.8\byte-buddy-1.17.8.jar;C:\Users\Administrator\.m2\repository\org\jod
d\jodd-util\6.3.0\jodd-util-6.3.0.jar;C:\Users\Administrator\.m2\repository\org\redisson\redisson-spring-data-35\4.7.0\
redisson-spring-data-35-4.7.0.jar;C:\Users\Administrator\.m2\repository\org\springframework\data\spring-data-redis\3.5.
12\spring-data-redis-3.5.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\data\spring-data-keyvalue\3.5
.12\spring-data-keyvalue-3.5.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\data\spring-data-commons\
3.5.12\spring-data-commons-3.5.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-tx\6.2.19\spring
-tx-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-oxm\6.2.19\spring-oxm-6.2.19.jar;C:\Use
rs\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-cache\3.5.15\spring-boot-starter-cache-3.5
.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-context-support\6.2.19\spring-context-support-
6.2.19.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\datatype\jackson-datatype-jsr310\2.21.4\jackson-
datatype-jsr310-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\core\jackson-annotations\2.21\ja
ckson-annotations-2.21.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\core\jackson-core\2.21.4\jackson
-core-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\core\jackson-databind\2.21.4\jackson-datab
ind-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\baomidou\lock4j-redisson-spring-boot-starter\2.2.7\lock4j-redi
sson-spring-boot-starter-2.2.7.jar;C:\Users\Administrator\.m2\repository\com\baomidou\lock4j-core\2.2.7\lock4j-core-2.2
.7.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-aop\3.5.15\spring-boot-starte
r-aop-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\aspectj\aspectjweaver\1.9.25.1\aspectjweaver-1.9.25.1.jar;E:
\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-test\target\classes;E:\����֮��AI���
�ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-mybatis\target\classes;C:\Users\Administrator\.m
2\repository\com\alibaba\druid-spring-boot-3-starter\1.2.28\druid-spring-boot-3-starter-1.2.28.jar;C:\Users\Administrat
or\.m2\repository\com\alibaba\druid\1.2.28\druid-1.2.28.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-
plus-spring-boot3-starter\3.5.17\mybatis-plus-spring-boot3-starter-3.5.17.jar;C:\Users\Administrator\.m2\repository\com
\baomidou\mybatis-plus\3.5.17\mybatis-plus-3.5.17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-c
ore\3.5.17\mybatis-plus-core-3.5.17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-annotation\3.5.
17\mybatis-plus-annotation-3.5.17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-spring\3.5.17\myb
atis-plus-spring-3.5.17.jar;C:\Users\Administrator\.m2\repository\org\mybatis\mybatis\3.5.19\mybatis-3.5.19.jar;C:\User
s\Administrator\.m2\repository\org\mybatis\mybatis-spring\3.0.5\mybatis-spring-3.0.5.jar;C:\Users\Administrator\.m2\rep
ository\com\baomidou\mybatis-plus-spring-boot-autoconfigure\3.5.17\mybatis-plus-spring-boot-autoconfigure-3.5.17.jar;C:
\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-spring-boot-native-image\3.5.17\mybatis-plus-spring-boot-
native-image-3.5.17.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-jdbc\3.5.15\
spring-boot-starter-jdbc-3.5.15.jar;C:\Users\Administrator\.m2\repository\com\zaxxer\HikariCP\6.3.3\HikariCP-6.3.3.jar;
C:\Users\Administrator\.m2\repository\org\springframework\spring-jdbc\6.2.19\spring-jdbc-6.2.19.jar;C:\Users\Administra
tor\.m2\repository\com\baomidou\mybatis-plus-jsqlparser\3.5.17\mybatis-plus-jsqlparser-3.5.17.jar;C:\Users\Administrato
r\.m2\repository\com\github\jsqlparser\jsqlparser\5.2\jsqlparser-5.2.jar;C:\Users\Administrator\.m2\repository\com\baom
idou\mybatis-plus-jsqlparser-common\3.5.17\mybatis-plus-jsqlparser-common-3.5.17.jar;C:\Users\Administrator\.m2\reposit
ory\com\baomidou\mybatis-plus-extension\3.5.17\mybatis-plus-extension-3.5.17.jar;C:\Users\Administrator\.m2\repository\
com\baomidou\dynamic-datasource-spring-boot3-starter\4.5.0\dynamic-datasource-spring-boot3-starter-4.5.0.jar;C:\Users\A
dministrator\.m2\repository\com\baomidou\dynamic-datasource-spring-boot-common\4.5.0\dynamic-datasource-spring-boot-com
mon-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\baomidou\dynamic-datasource-spring\4.5.0\dynamic-datasource-spr
ing-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\baomidou\dynamic-datasource-creator\4.5.0\dynamic-datasource-cr
eator-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\oracle\database\spring\oracle-spring-boot-starter-ucp\23.4.0\
oracle-spring-boot-starter-ucp-23.4.0.jar;C:\Users\Administrator\.m2\repository\com\oracle\database\jdbc\ojdbc11\23.7.0
.25.01\ojdbc11-23.7.0.25.01.jar;C:\Users\Administrator\.m2\repository\com\oracle\database\jdbc\ucp\23.7.0.25.01\ucp-23.
7.0.25.01.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-join-boot-starter\1.5.9\mybatis-p
lus-join-boot-starter-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-join-extension\
1.5.9\mybatis-plus-join-extension-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-joi
n-core\1.5.9\mybatis-plus-join-core-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-j
oin-annotation\1.5.9\mybatis-plus-join-annotation-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\
mybatis-plus-join-adapter-base\1.5.9\mybatis-plus-join-adapter-base-1.5.9.jar;C:\Users\Administrator\.m2\repository\com
\github\yulichang\mybatis-plus-join-adapter-jsqlparser\1.5.9\mybatis-plus-join-adapter-jsqlparser-1.5.9.jar;C:\Users\Ad
ministrator\.m2\repository\com\github\yulichang\mybatis-plus-join-wrapper-ext\1.5.9\mybatis-plus-join-wrapper-ext-1.5.9
.jar;C:\Users\Administrator\.m2\repository\org\dromara\easy-trans-spring-boot-starter\3.1.8\easy-trans-spring-boot-star
ter-3.1.8.jar;C:\Users\Administrator\.m2\repository\org\dromara\easy-trans-service\3.1.8\easy-trans-service-3.1.8.jar;C
:\Users\Administrator\.m2\repository\com\github\ben-manes\caffeine\caffeine\3.2.4\caffeine-3.2.4.jar;C:\Users\Administr
ator\.m2\repository\org\dromara\easy-trans-mybatis-plus-extend\3.1.8\easy-trans-mybatis-plus-extend-3.1.8.jar;C:\Users\
Administrator\.m2\repository\org\mockito\mockito-inline\5.2.0\mockito-inline-5.2.0.jar;C:\Users\Administrator\.m2\repos
itory\org\mockito\mockito-core\5.17.0\mockito-core-5.17.0.jar;C:\Users\Administrator\.m2\repository\net\bytebuddy\byte-
buddy-agent\1.17.8\byte-buddy-agent-1.17.8.jar;C:\Users\Administrator\.m2\repository\org\objenesis\objenesis\3.3\objene
sis-3.3.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-test\3.5.15\spring-boot-
starter-test-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-test\3.5.15\spring-b
oot-test-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-test-autoconfigure\3.5.1
5\spring-boot-test-autoconfigure-3.5.15.jar;C:\Users\Administrator\.m2\repository\com\jayway\jsonpath\json-path\2.9.0\j
son-path-2.9.0.jar;C:\Users\Administrator\.m2\repository\jakarta\xml\bind\jakarta.xml.bind-api\4.0.5\jakarta.xml.bind-a
pi-4.0.5.jar;C:\Users\Administrator\.m2\repository\jakarta\activation\jakarta.activation-api\2.1.4\jakarta.activation-a
pi-2.1.4.jar;C:\Users\Administrator\.m2\repository\net\minidev\json-smart\2.5.2\json-smart-2.5.2.jar;C:\Users\Administr
ator\.m2\repository\net\minidev\accessors-smart\2.5.2\accessors-smart-2.5.2.jar;C:\Users\Administrator\.m2\repository\o
rg\assertj\assertj-core\3.27.7\assertj-core-3.27.7.jar;C:\Users\Administrator\.m2\repository\org\awaitility\awaitility\
4.2.2\awaitility-4.2.2.jar;C:\Users\Administrator\.m2\repository\org\hamcrest\hamcrest\3.0\hamcrest-3.0.jar;C:\Users\Ad
ministrator\.m2\repository\org\junit\jupiter\junit-jupiter\5.12.2\junit-jupiter-5.12.2.jar;C:\Users\Administrator\.m2\r
epository\org\junit\jupiter\junit-jupiter-api\5.12.2\junit-jupiter-api-5.12.2.jar;C:\Users\Administrator\.m2\repository
\org\opentest4j\opentest4j\1.3.0\opentest4j-1.3.0.jar;C:\Users\Administrator\.m2\repository\org\junit\platform\junit-pl
atform-commons\1.12.2\junit-platform-commons-1.12.2.jar;C:\Users\Administrator\.m2\repository\org\apiguardian\apiguardi
an-api\1.1.2\apiguardian-api-1.1.2.jar;C:\Users\Administrator\.m2\repository\org\junit\jupiter\junit-jupiter-params\5.1
2.2\junit-jupiter-params-5.12.2.jar;C:\Users\Administrator\.m2\repository\org\junit\jupiter\junit-jupiter-engine\5.12.2
\junit-jupiter-engine-5.12.2.jar;C:\Users\Administrator\.m2\repository\org\junit\platform\junit-platform-engine\1.12.2\
junit-platform-engine-1.12.2.jar;C:\Users\Administrator\.m2\repository\org\mockito\mockito-junit-jupiter\5.17.0\mockito
-junit-jupiter-5.17.0.jar;C:\Users\Administrator\.m2\repository\org\skyscreamer\jsonassert\1.5.3\jsonassert-1.5.3.jar;C
:\Users\Administrator\.m2\repository\com\vaadin\external\google\android-json\0.0.20131108.vaadin1\android-json-0.0.2013
1108.vaadin1.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-core\6.2.19\spring-core-6.2.19.jar;C:
\Users\Administrator\.m2\repository\org\springframework\spring-jcl\6.2.19\spring-jcl-6.2.19.jar;C:\Users\Administrator\
.m2\repository\org\springframework\spring-test\6.2.19\spring-test-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\
xmlunit\xmlunit-core\2.10.4\xmlunit-core-2.10.4.jar;C:\Users\Administrator\.m2\repository\com\h2database\h2\2.3.232\h2-
2.3.232.jar;C:\Users\Administrator\.m2\repository\com\github\fppt\jedis-mock\1.1.18\jedis-mock-1.1.18.jar;C:\Users\Admi
nistrator\.m2\repository\org\slf4j\slf4j-api\2.0.18\slf4j-api-2.0.18.jar;C:\Users\Administrator\.m2\repository\org\luaj
\luaj-jse\3.0.1\luaj-jse-3.0.1.jar;C:\Users\Administrator\.m2\repository\redis\clients\jedis\6.0.0\jedis-6.0.0.jar;C:\U
sers\Administrator\.m2\repository\org\apache\commons\commons-pool2\2.12.1\commons-pool2-2.12.1.jar;C:\Users\Administrat
or\.m2\repository\org\json\json\20250107\json-20250107.jar;C:\Users\Administrator\.m2\repository\com\google\code\gson\g
son\2.13.2\gson-2.13.2.jar;C:\Users\Administrator\.m2\repository\com\google\errorprone\error_prone_annotations\2.41.0\e
rror_prone_annotations-2.41.0.jar;C:\Users\Administrator\.m2\repository\redis\clients\authentication\redis-authx-core\0
.1.1-beta2\redis-authx-core-0.1.1-beta2.jar;C:\Users\Administrator\.m2\repository\uk\co\jemos\podam\podam\8.0.2.RELEASE
\podam-8.0.2.RELEASE.jar;C:\Users\Administrator\.m2\repository\net\jcip\jcip-annotations\1.0\jcip-annotations-1.0.jar;C
:\Users\Administrator\.m2\repository\jakarta\validation\jakarta.validation-api\3.0.2\jakarta.validation-api-3.0.2.jar;C
:\Users\Administrator\.m2\repository\org\apache\commons\commons-lang3\3.20.0\commons-lang3-3.20.0.jar;"/>
    <property name="java.vm.vendor" value="Eclipse Adoptium"/>
    <property name="sun.arch.data.model" value="64"/>
    <property name="user.variant" value=""/>
    <property name="java.vendor.url" value="https://adoptium.net/"/>
    <property name="user.timezone" value="Asia/Shanghai"/>
    <property name="os.name" value="Windows 10"/>
    <property name="java.vm.specification.version" value="17"/>
    <property name="sun.java.launcher" value="SUN_STANDARD"/>
    <property name="user.country" value="CN"/>
    <property name="sun.boot.library.path" value="E:\����֮��AI����ƽ̨����\tools\jdk-17.0.20.1+1\bin"/>
    <property name="sun.java.command" value="C:\Users\Administrator\AppData\Local\Temp\surefire12053511136342888388\sur
efirebooter-20260912135129227_23.jar C:\Users\Administrator\AppData\Local\Temp\surefire12053511136342888388 2026-09-12T
13-51-22_032-jvmRun1 surefire-20260912135129227_21tmp surefire_4-20260912135129227_22tmp"/>
    <property name="jdk.debug" value="release"/>
    <property name="surefire.test.class.path" value="E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-
spring-boot-starter-protection\target\test-classes;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-sp
ring-boot-starter-protection\target\classes;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-bo
ot-starter-web\target\classes;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-common\target\classes;C
:\Users\Administrator\.m2\repository\io\opentelemetry\opentelemetry-api\1.65.0\opentelemetry-api-1.65.0.jar;C:\Users\Ad
ministrator\.m2\repository\io\opentelemetry\opentelemetry-context\1.49.0\opentelemetry-context-1.49.0.jar;C:\Users\Admi
nistrator\.m2\repository\org\projectlombok\lombok\1.18.46\lombok-1.18.46.jar;C:\Users\Administrator\.m2\repository\org\
mapstruct\mapstruct\1.6.3\mapstruct-1.6.3.jar;C:\Users\Administrator\.m2\repository\org\mapstruct\mapstruct-jdk8\1.6.3\
mapstruct-jdk8-1.6.3.jar;C:\Users\Administrator\.m2\repository\org\mapstruct\mapstruct-processor\1.6.3\mapstruct-proces
sor-1.6.3.jar;C:\Users\Administrator\.m2\repository\cn\hutool\hutool-all\5.8.47\hutool-all-5.8.47.jar;C:\Users\Administ
rator\.m2\repository\com\alibaba\transmittable-thread-local\2.14.5\transmittable-thread-local-2.14.5.jar;C:\Users\Admin
istrator\.m2\repository\com\alibaba\fastjson\2.0.64\fastjson-2.0.64.jar;C:\Users\Administrator\.m2\repository\com\aliba
ba\fastjson2\fastjson2-extension\2.0.64\fastjson2-extension-2.0.64.jar;C:\Users\Administrator\.m2\repository\com\alibab
a\fastjson2\fastjson2\2.0.64\fastjson2-2.0.64.jar;C:\Users\Administrator\.m2\repository\org\dromara\easy-trans-anno\3.1
.8\easy-trans-anno-3.1.8.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-web\3.5
.15\spring-boot-starter-web-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-start
er\3.5.15\spring-boot-starter-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot\3.5
.15\spring-boot-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-autoconfigure\3.5
.15\spring-boot-autoconfigure-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-sta
rter-logging\3.5.15\spring-boot-starter-logging-3.5.15.jar;C:\Users\Administrator\.m2\repository\ch\qos\logback\logback
-classic\1.5.34\logback-classic-1.5.34.jar;C:\Users\Administrator\.m2\repository\ch\qos\logback\logback-core\1.5.34\log
back-core-1.5.34.jar;C:\Users\Administrator\.m2\repository\org\apache\logging\log4j\log4j-to-slf4j\2.24.3\log4j-to-slf4
j-2.24.3.jar;C:\Users\Administrator\.m2\repository\org\apache\logging\log4j\log4j-api\2.24.3\log4j-api-2.24.3.jar;C:\Us
ers\Administrator\.m2\repository\org\slf4j\jul-to-slf4j\2.0.18\jul-to-slf4j-2.0.18.jar;C:\Users\Administrator\.m2\repos
itory\jakarta\annotation\jakarta.annotation-api\2.1.1\jakarta.annotation-api-2.1.1.jar;C:\Users\Administrator\.m2\repos
itory\org\yaml\snakeyaml\2.4\snakeyaml-2.4.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-bo
ot-starter-json\3.5.15\spring-boot-starter-json-3.5.15.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\
datatype\jackson-datatype-jdk8\2.21.4\jackson-datatype-jdk8-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\faster
xml\jackson\module\jackson-module-parameter-names\2.21.4\jackson-module-parameter-names-2.21.4.jar;C:\Users\Administrat
or\.m2\repository\org\springframework\boot\spring-boot-starter-tomcat\3.5.15\spring-boot-starter-tomcat-3.5.15.jar;C:\U
sers\Administrator\.m2\repository\org\apache\tomcat\embed\tomcat-embed-core\10.1.55\tomcat-embed-core-10.1.55.jar;C:\Us
ers\Administrator\.m2\repository\org\apache\tomcat\embed\tomcat-embed-el\10.1.55\tomcat-embed-el-10.1.55.jar;C:\Users\A
dministrator\.m2\repository\org\apache\tomcat\embed\tomcat-embed-websocket\10.1.55\tomcat-embed-websocket-10.1.55.jar;C
:\Users\Administrator\.m2\repository\org\springframework\spring-web\6.2.19\spring-web-6.2.19.jar;C:\Users\Administrator
\.m2\repository\org\springframework\spring-beans\6.2.19\spring-beans-6.2.19.jar;C:\Users\Administrator\.m2\repository\i
o\micrometer\micrometer-observation\1.15.12\micrometer-observation-1.15.12.jar;C:\Users\Administrator\.m2\repository\io
\micrometer\micrometer-commons\1.15.12\micrometer-commons-1.15.12.jar;C:\Users\Administrator\.m2\repository\org\springf
ramework\spring-webmvc\6.2.19\spring-webmvc-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring
-aop\6.2.19\spring-aop-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-context\6.2.19\sprin
g-context-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-expression\6.2.19\spring-expressi
on-6.2.19.jar;C:\Users\Administrator\.m2\repository\com\github\xiaoymin\knife4j-openapi3-jakarta-spring-boot-starter\4.
5.0\knife4j-openapi3-jakarta-spring-boot-starter-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\github\xiaoymin\kn
ife4j-core\4.5.0\knife4j-core-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\github\xiaoymin\knife4j-openapi3-ui\4
.5.0\knife4j-openapi3-ui-4.5.0.jar;C:\Users\Administrator\.m2\repository\org\springdoc\springdoc-openapi-starter-webmvc
-ui\2.8.17\springdoc-openapi-starter-webmvc-ui-2.8.17.jar;C:\Users\Administrator\.m2\repository\org\springdoc\springdoc
-openapi-starter-webmvc-api\2.8.17\springdoc-openapi-starter-webmvc-api-2.8.17.jar;C:\Users\Administrator\.m2\repositor
y\org\springdoc\springdoc-openapi-starter-common\2.8.17\springdoc-openapi-starter-common-2.8.17.jar;C:\Users\Administra
tor\.m2\repository\org\springframework\boot\spring-boot-starter-validation\3.5.15\spring-boot-starter-validation-3.5.15
.jar;C:\Users\Administrator\.m2\repository\org\hibernate\validator\hibernate-validator\8.0.3.Final\hibernate-validator-
8.0.3.Final.jar;C:\Users\Administrator\.m2\repository\org\jboss\logging\jboss-logging\3.6.3.Final\jboss-logging-3.6.3.F
inal.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\classmate\1.7.3\classmate-1.7.3.jar;C:\Users\Administrator
\.m2\repository\io\swagger\core\v3\swagger-core-jakarta\2.2.47\swagger-core-jakarta-2.2.47.jar;C:\Users\Administrator\.
m2\repository\io\swagger\core\v3\swagger-annotations-jakarta\2.2.47\swagger-annotations-jakarta-2.2.47.jar;C:\Users\Adm
inistrator\.m2\repository\io\swagger\core\v3\swagger-models-jakarta\2.2.47\swagger-models-jakarta-2.2.47.jar;C:\Users\A
dministrator\.m2\repository\com\fasterxml\jackson\dataformat\jackson-dataformat-yaml\2.21.4\jackson-dataformat-yaml-2.2
1.4.jar;C:\Users\Administrator\.m2\repository\org\webjars\swagger-ui\5.32.2\swagger-ui-5.32.2.jar;C:\Users\Administrato
r\.m2\repository\org\webjars\webjars-locator-lite\1.1.3\webjars-locator-lite-1.1.3.jar;C:\Users\Administrator\.m2\repos
itory\org\jspecify\jspecify\1.0.0\jspecify-1.0.0.jar;C:\Users\Administrator\.m2\repository\org\jsoup\jsoup\1.23.2\jsoup
-1.23.2.jar;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-redis\target\classes;
C:\Users\Administrator\.m2\repository\org\redisson\redisson-spring-boot-starter\4.7.0\redisson-spring-boot-starter-4.7.
0.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-data-redis\3.5.15\spring-boot-
starter-data-redis-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\redisson\redisson\4.7.0\redisson-4.7.0.jar;C:\U
sers\Administrator\.m2\repository\io\netty\netty-common\4.2.17.Final\netty-common-4.2.17.Final.jar;C:\Users\Administrat
or\.m2\repository\io\netty\netty-codec\4.2.17.Final\netty-codec-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\
io\netty\netty-codec-base\4.2.17.Final\netty-codec-base-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty
\netty-codec-compression\4.2.17.Final\netty-codec-compression-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io
\netty\netty-codec-protobuf\4.2.17.Final\netty-codec-protobuf-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io
\netty\netty-codec-marshalling\4.2.17.Final\netty-codec-marshalling-4.2.17.Final.jar;C:\Users\Administrator\.m2\reposit
ory\io\netty\netty-buffer\4.2.17.Final\netty-buffer-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\net
ty-transport\4.2.17.Final\netty-transport-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-resolve
r\4.2.17.Final\netty-resolver-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-resolver-dns\4.2.17
.Final\netty-resolver-dns-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-codec-dns\4.2.17.Final\
netty-codec-dns-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-handler\4.2.17.Final\netty-handle
r-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-transport-native-unix-common\4.2.17.Final\netty
-transport-native-unix-common-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\javax\cache\cache-api\1.1.1\cache-
api-1.1.1.jar;C:\Users\Administrator\.m2\repository\io\projectreactor\reactor-core\3.7.19\reactor-core-3.7.19.jar;C:\Us
ers\Administrator\.m2\repository\org\reactivestreams\reactive-streams\1.0.4\reactive-streams-1.0.4.jar;C:\Users\Adminis
trator\.m2\repository\io\reactivex\rxjava3\rxjava\3.1.12\rxjava-3.1.12.jar;C:\Users\Administrator\.m2\repository\com\es
otericsoftware\kryo\5.6.2\kryo-5.6.2.jar;C:\Users\Administrator\.m2\repository\com\esotericsoftware\reflectasm\1.11.9\r
eflectasm-1.11.9.jar;C:\Users\Administrator\.m2\repository\com\esotericsoftware\minlog\1.3.1\minlog-1.3.1.jar;C:\Users\
Administrator\.m2\repository\net\bytebuddy\byte-buddy\1.17.8\byte-buddy-1.17.8.jar;C:\Users\Administrator\.m2\repositor
y\org\jodd\jodd-util\6.3.0\jodd-util-6.3.0.jar;C:\Users\Administrator\.m2\repository\org\redisson\redisson-spring-data-
35\4.7.0\redisson-spring-data-35-4.7.0.jar;C:\Users\Administrator\.m2\repository\org\springframework\data\spring-data-r
edis\3.5.12\spring-data-redis-3.5.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\data\spring-data-key
value\3.5.12\spring-data-keyvalue-3.5.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\data\spring-data
-commons\3.5.12\spring-data-commons-3.5.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-tx\6.2.
19\spring-tx-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-oxm\6.2.19\spring-oxm-6.2.19.j
ar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-cache\3.5.15\spring-boot-starter-
cache-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-context-support\6.2.19\spring-context
-support-6.2.19.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\datatype\jackson-datatype-jsr310\2.21.4
\jackson-datatype-jsr310-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\core\jackson-annotation
s\2.21\jackson-annotations-2.21.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\core\jackson-core\2.21.
4\jackson-core-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\core\jackson-databind\2.21.4\jack
son-databind-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\baomidou\lock4j-redisson-spring-boot-starter\2.2.7\lo
ck4j-redisson-spring-boot-starter-2.2.7.jar;C:\Users\Administrator\.m2\repository\com\baomidou\lock4j-core\2.2.7\lock4j
-core-2.2.7.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-aop\3.5.15\spring-bo
ot-starter-aop-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\aspectj\aspectjweaver\1.9.25.1\aspectjweaver-1.9.25
.1.jar;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-test\target\classes;E:\���
�֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-mybatis\target\classes;C:\Users\Adminis
trator\.m2\repository\com\alibaba\druid-spring-boot-3-starter\1.2.28\druid-spring-boot-3-starter-1.2.28.jar;C:\Users\Ad
ministrator\.m2\repository\com\alibaba\druid\1.2.28\druid-1.2.28.jar;C:\Users\Administrator\.m2\repository\com\baomidou
\mybatis-plus-spring-boot3-starter\3.5.17\mybatis-plus-spring-boot3-starter-3.5.17.jar;C:\Users\Administrator\.m2\repos
itory\com\baomidou\mybatis-plus\3.5.17\mybatis-plus-3.5.17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybat
is-plus-core\3.5.17\mybatis-plus-core-3.5.17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-annota
tion\3.5.17\mybatis-plus-annotation-3.5.17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-spring\3
.5.17\mybatis-plus-spring-3.5.17.jar;C:\Users\Administrator\.m2\repository\org\mybatis\mybatis\3.5.19\mybatis-3.5.19.ja
r;C:\Users\Administrator\.m2\repository\org\mybatis\mybatis-spring\3.0.5\mybatis-spring-3.0.5.jar;C:\Users\Administrato
r\.m2\repository\com\baomidou\mybatis-plus-spring-boot-autoconfigure\3.5.17\mybatis-plus-spring-boot-autoconfigure-3.5.
17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-spring-boot-native-image\3.5.17\mybatis-plus-spr
ing-boot-native-image-3.5.17.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-jdb
c\3.5.15\spring-boot-starter-jdbc-3.5.15.jar;C:\Users\Administrator\.m2\repository\com\zaxxer\HikariCP\6.3.3\HikariCP-6
.3.3.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-jdbc\6.2.19\spring-jdbc-6.2.19.jar;C:\Users\A
dministrator\.m2\repository\com\baomidou\mybatis-plus-jsqlparser\3.5.17\mybatis-plus-jsqlparser-3.5.17.jar;C:\Users\Adm
inistrator\.m2\repository\com\github\jsqlparser\jsqlparser\5.2\jsqlparser-5.2.jar;C:\Users\Administrator\.m2\repository
\com\baomidou\mybatis-plus-jsqlparser-common\3.5.17\mybatis-plus-jsqlparser-common-3.5.17.jar;C:\Users\Administrator\.m
2\repository\com\baomidou\mybatis-plus-extension\3.5.17\mybatis-plus-extension-3.5.17.jar;C:\Users\Administrator\.m2\re
pository\com\baomidou\dynamic-datasource-spring-boot3-starter\4.5.0\dynamic-datasource-spring-boot3-starter-4.5.0.jar;C
:\Users\Administrator\.m2\repository\com\baomidou\dynamic-datasource-spring-boot-common\4.5.0\dynamic-datasource-spring
-boot-common-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\baomidou\dynamic-datasource-spring\4.5.0\dynamic-datas
ource-spring-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\baomidou\dynamic-datasource-creator\4.5.0\dynamic-data
source-creator-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\oracle\database\spring\oracle-spring-boot-starter-uc
p\23.4.0\oracle-spring-boot-starter-ucp-23.4.0.jar;C:\Users\Administrator\.m2\repository\com\oracle\database\jdbc\ojdbc
11\23.7.0.25.01\ojdbc11-23.7.0.25.01.jar;C:\Users\Administrator\.m2\repository\com\oracle\database\jdbc\ucp\23.7.0.25.0
1\ucp-23.7.0.25.01.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-join-boot-starter\1.5.9\
mybatis-plus-join-boot-starter-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-join-e
xtension\1.5.9\mybatis-plus-join-extension-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis
-plus-join-core\1.5.9\mybatis-plus-join-core-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybat
is-plus-join-annotation\1.5.9\mybatis-plus-join-annotation-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\y
ulichang\mybatis-plus-join-adapter-base\1.5.9\mybatis-plus-join-adapter-base-1.5.9.jar;C:\Users\Administrator\.m2\repos
itory\com\github\yulichang\mybatis-plus-join-adapter-jsqlparser\1.5.9\mybatis-plus-join-adapter-jsqlparser-1.5.9.jar;C:
\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-join-wrapper-ext\1.5.9\mybatis-plus-join-wrapper-
ext-1.5.9.jar;C:\Users\Administrator\.m2\repository\org\dromara\easy-trans-spring-boot-starter\3.1.8\easy-trans-spring-
boot-starter-3.1.8.jar;C:\Users\Administrator\.m2\repository\org\dromara\easy-trans-service\3.1.8\easy-trans-service-3.
1.8.jar;C:\Users\Administrator\.m2\repository\com\github\ben-manes\caffeine\caffeine\3.2.4\caffeine-3.2.4.jar;C:\Users\
Administrator\.m2\repository\org\dromara\easy-trans-mybatis-plus-extend\3.1.8\easy-trans-mybatis-plus-extend-3.1.8.jar;
C:\Users\Administrator\.m2\repository\org\mockito\mockito-inline\5.2.0\mockito-inline-5.2.0.jar;C:\Users\Administrator\
.m2\repository\org\mockito\mockito-core\5.17.0\mockito-core-5.17.0.jar;C:\Users\Administrator\.m2\repository\net\bytebu
ddy\byte-buddy-agent\1.17.8\byte-buddy-agent-1.17.8.jar;C:\Users\Administrator\.m2\repository\org\objenesis\objenesis\3
.3\objenesis-3.3.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-test\3.5.15\spr
ing-boot-starter-test-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-test\3.5.15
\spring-boot-test-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-test-autoconfig
ure\3.5.15\spring-boot-test-autoconfigure-3.5.15.jar;C:\Users\Administrator\.m2\repository\com\jayway\jsonpath\json-pat
h\2.9.0\json-path-2.9.0.jar;C:\Users\Administrator\.m2\repository\jakarta\xml\bind\jakarta.xml.bind-api\4.0.5\jakarta.x
ml.bind-api-4.0.5.jar;C:\Users\Administrator\.m2\repository\jakarta\activation\jakarta.activation-api\2.1.4\jakarta.act
ivation-api-2.1.4.jar;C:\Users\Administrator\.m2\repository\net\minidev\json-smart\2.5.2\json-smart-2.5.2.jar;C:\Users\
Administrator\.m2\repository\net\minidev\accessors-smart\2.5.2\accessors-smart-2.5.2.jar;C:\Users\Administrator\.m2\rep
ository\org\assertj\assertj-core\3.27.7\assertj-core-3.27.7.jar;C:\Users\Administrator\.m2\repository\org\awaitility\aw
aitility\4.2.2\awaitility-4.2.2.jar;C:\Users\Administrator\.m2\repository\org\hamcrest\hamcrest\3.0\hamcrest-3.0.jar;C:
\Users\Administrator\.m2\repository\org\junit\jupiter\junit-jupiter\5.12.2\junit-jupiter-5.12.2.jar;C:\Users\Administra
tor\.m2\repository\org\junit\jupiter\junit-jupiter-api\5.12.2\junit-jupiter-api-5.12.2.jar;C:\Users\Administrator\.m2\r
epository\org\opentest4j\opentest4j\1.3.0\opentest4j-1.3.0.jar;C:\Users\Administrator\.m2\repository\org\junit\platform
\junit-platform-commons\1.12.2\junit-platform-commons-1.12.2.jar;C:\Users\Administrator\.m2\repository\org\apiguardian\
apiguardian-api\1.1.2\apiguardian-api-1.1.2.jar;C:\Users\Administrator\.m2\repository\org\junit\jupiter\junit-jupiter-p
arams\5.12.2\junit-jupiter-params-5.12.2.jar;C:\Users\Administrator\.m2\repository\org\junit\jupiter\junit-jupiter-engi
ne\5.12.2\junit-jupiter-engine-5.12.2.jar;C:\Users\Administrator\.m2\repository\org\junit\platform\junit-platform-engin
e\1.12.2\junit-platform-engine-1.12.2.jar;C:\Users\Administrator\.m2\repository\org\mockito\mockito-junit-jupiter\5.17.
0\mockito-junit-jupiter-5.17.0.jar;C:\Users\Administrator\.m2\repository\org\skyscreamer\jsonassert\1.5.3\jsonassert-1.
5.3.jar;C:\Users\Administrator\.m2\repository\com\vaadin\external\google\android-json\0.0.20131108.vaadin1\android-json
-0.0.20131108.vaadin1.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-core\6.2.19\spring-core-6.2.
19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-jcl\6.2.19\spring-jcl-6.2.19.jar;C:\Users\Admin
istrator\.m2\repository\org\springframework\spring-test\6.2.19\spring-test-6.2.19.jar;C:\Users\Administrator\.m2\reposi
tory\org\xmlunit\xmlunit-core\2.10.4\xmlunit-core-2.10.4.jar;C:\Users\Administrator\.m2\repository\com\h2database\h2\2.
3.232\h2-2.3.232.jar;C:\Users\Administrator\.m2\repository\com\github\fppt\jedis-mock\1.1.18\jedis-mock-1.1.18.jar;C:\U
sers\Administrator\.m2\repository\org\slf4j\slf4j-api\2.0.18\slf4j-api-2.0.18.jar;C:\Users\Administrator\.m2\repository
\org\luaj\luaj-jse\3.0.1\luaj-jse-3.0.1.jar;C:\Users\Administrator\.m2\repository\redis\clients\jedis\6.0.0\jedis-6.0.0
.jar;C:\Users\Administrator\.m2\repository\org\apache\commons\commons-pool2\2.12.1\commons-pool2-2.12.1.jar;C:\Users\Ad
ministrator\.m2\repository\org\json\json\20250107\json-20250107.jar;C:\Users\Administrator\.m2\repository\com\google\co
de\gson\gson\2.13.2\gson-2.13.2.jar;C:\Users\Administrator\.m2\repository\com\google\errorprone\error_prone_annotations
\2.41.0\error_prone_annotations-2.41.0.jar;C:\Users\Administrator\.m2\repository\redis\clients\authentication\redis-aut
hx-core\0.1.1-beta2\redis-authx-core-0.1.1-beta2.jar;C:\Users\Administrator\.m2\repository\uk\co\jemos\podam\podam\8.0.
2.RELEASE\podam-8.0.2.RELEASE.jar;C:\Users\Administrator\.m2\repository\net\jcip\jcip-annotations\1.0\jcip-annotations-
1.0.jar;C:\Users\Administrator\.m2\repository\jakarta\validation\jakarta.validation-api\3.0.2\jakarta.validation-api-3.
0.2.jar;C:\Users\Administrator\.m2\repository\org\apache\commons\commons-lang3\3.20.0\commons-lang3-3.20.0.jar;"/>
    <property name="sun.cpu.endian" value="little"/>
    <property name="user.home" value="C:\Users\Administrator"/>
    <property name="user.language" value="zh"/>
    <property name="java.specification.vendor" value="Oracle Corporation"/>
    <property name="java.version.date" value="2026-08-18"/>
    <property name="java.home" value="E:\����֮��AI����ƽ̨����\tools\jdk-17.0.20.1+1"/>
    <property name="file.separator" value="\"/>
    <property name="basedir" value="E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-start
er-protection"/>
    <property name="java.vm.compressedOopsMode" value="Zero based"/>
    <property name="line.separator" value="&#10;"/>
    <property name="java.vm.specification.vendor" value="Oracle Corporation"/>
    <property name="java.specification.name" value="Java Platform API Specification"/>
    <property name="surefire.real.class.path" value="C:\Users\Administrator\AppData\Local\Temp\surefire1205351113634288
8388\surefirebooter-20260912135129227_23.jar"/>
    <property name="user.script" value=""/>
    <property name="sun.management.compiler" value="HotSpot 64-Bit Tiered Compilers"/>
    <property name="java.runtime.version" value="17.0.20.1+1"/>
    <property name="user.name" value="Administrator"/>
    <property name="path.separator" value=";"/>
    <property name="os.version" value="10.0"/>
    <property name="java.runtime.name" value="OpenJDK Runtime Environment"/>
    <property name="file.encoding" value="GBK"/>
    <property name="java.vm.name" value="OpenJDK 64-Bit Server VM"/>
    <property name="java.vendor.version" value="Temurin-17.0.20.1+1"/>
    <property name="localRepository" value="C:\Users\Administrator\.m2\repository"/>
    <property name="java.vendor.url.bug" value="https://github.com/adoptium/adoptium-support/issues"/>
    <property name="java.io.tmpdir" value="C:\Users\ADMINI~1\AppData\Local\Temp\"/>
    <property name="java.version" value="17.0.20.1"/>
    <property name="user.dir" value="E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-star
ter-protection"/>
    <property name="os.arch" value="amd64"/>
    <property name="java.vm.specification.name" value="Java Virtual Machine Specification"/>
    <property name="sun.os.patch.level" value=""/>
    <property name="native.encoding" value="GBK"/>
    <property name="java.library.path" value="E:\����֮��AI����ƽ̨����\tools\jdk-17.0.20.1+1\bin;C:\windows\Sun\Java\bin;
C:\windows\system32;C:\windows;E:\����֮��AI����ƽ̨����\tools\jdk-17.0.20.1+1\bin;E:\����֮��AI����ƽ̨����\tools\apache-mav
en-3.9.9\bin;C:\Program Files\Python311\Scripts\;C:\Program Files\Python311\;C:\windows\system32;C:\windows;C:\windows\
System32\Wbem;C:\windows\System32\WindowsPowerShell\v1.0\;C:\windows\System32\OpenSSH\;D:\Program Files (x86)\Tencent\΢
��web�����߹���;;C:\Program Files\Git\cmd;C:\Program Files\nodejs\;C:\Users\Administrator\AppData\Local\Microsoft\Window
sApps;;C:\Users\Administrator\AppData\Local\Programs\DockerDesktop\resources\bin;C:\Users\Administrator\AppData\Roaming
\npm;D:\Program Files\Qoder IDE\bin;C:\Users\Administrator\AppData\Local\Microsoft\WinGet\Links;;."/>
    <property name="java.vm.info" value="mixed mode, sharing"/>
    <property name="java.vendor" value="Eclipse Adoptium"/>
    <property name="java.vm.version" value="17.0.20.1+1"/>
    <property name="java.specification.maintenance.version" value="1"/>
    <property name="sun.io.unicode.encoding" value="UnicodeLittle"/>
    <property name="java.class.version" value="61.0"/>
  </properties>
  <testcase name="resolver_shouldScopeByMethodAndTenant" classname="cn.zszj.framework.ratelimiter.core.keyresolver.impl
.ExpressionRateLimiterKeyResolverTest" time="0.065"/>
  <testcase name="resolver_sameSubjectDifferentArg_shouldNotChangeKey" classname="cn.zszj.framework.ratelimiter.core.ke
yresolver.impl.ExpressionRateLimiterKeyResolverTest" time="0.002"/>
  <testcase name="resolver_whenNoRequestContext_shouldNotCrash" classname="cn.zszj.framework.ratelimiter.core.keyresolv
er.impl.ExpressionRateLimiterKeyResolverTest" time="0.003"/>
</testsuite>
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\TEST-cn.zszj.framework.signature.core.aop.ApiSignatureAspectTest.xml
<?xml version="1.0" encoding="UTF-8"?>
<testsuite xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="https://maven.apache.or
g/surefire/maven-surefire-plugin/xsd/surefire-test-report.xsd" version="3.0.2" name="cn.zszj.framework.signature.core.a
op.ApiSignatureAspectTest" time="0.055" tests="1" errors="0" skipped="0" failures="0">
  <properties>
    <property name="java.specification.version" value="17"/>
    <property name="sun.cpu.isalist" value="amd64"/>
    <property name="sun.jnu.encoding" value="GBK"/>
    <property name="java.class.path" value="E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-bo
ot-starter-protection\target\test-classes;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot
-starter-protection\target\classes;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starte
r-web\target\classes;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-common\target\classes;C:\Users\A
dministrator\.m2\repository\io\opentelemetry\opentelemetry-api\1.65.0\opentelemetry-api-1.65.0.jar;C:\Users\Administrat
or\.m2\repository\io\opentelemetry\opentelemetry-context\1.49.0\opentelemetry-context-1.49.0.jar;C:\Users\Administrator
\.m2\repository\org\projectlombok\lombok\1.18.46\lombok-1.18.46.jar;C:\Users\Administrator\.m2\repository\org\mapstruct
\mapstruct\1.6.3\mapstruct-1.6.3.jar;C:\Users\Administrator\.m2\repository\org\mapstruct\mapstruct-jdk8\1.6.3\mapstruct
-jdk8-1.6.3.jar;C:\Users\Administrator\.m2\repository\org\mapstruct\mapstruct-processor\1.6.3\mapstruct-processor-1.6.3
.jar;C:\Users\Administrator\.m2\repository\cn\hutool\hutool-all\5.8.47\hutool-all-5.8.47.jar;C:\Users\Administrator\.m2
\repository\com\alibaba\transmittable-thread-local\2.14.5\transmittable-thread-local-2.14.5.jar;C:\Users\Administrator\
.m2\repository\com\alibaba\fastjson\2.0.64\fastjson-2.0.64.jar;C:\Users\Administrator\.m2\repository\com\alibaba\fastjs
on2\fastjson2-extension\2.0.64\fastjson2-extension-2.0.64.jar;C:\Users\Administrator\.m2\repository\com\alibaba\fastjso
n2\fastjson2\2.0.64\fastjson2-2.0.64.jar;C:\Users\Administrator\.m2\repository\org\dromara\easy-trans-anno\3.1.8\easy-t
rans-anno-3.1.8.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-web\3.5.15\sprin
g-boot-starter-web-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter\3.5.15
\spring-boot-starter-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot\3.5.15\sprin
g-boot-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-autoconfigure\3.5.15\sprin
g-boot-autoconfigure-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-logg
ing\3.5.15\spring-boot-starter-logging-3.5.15.jar;C:\Users\Administrator\.m2\repository\ch\qos\logback\logback-classic\
1.5.34\logback-classic-1.5.34.jar;C:\Users\Administrator\.m2\repository\ch\qos\logback\logback-core\1.5.34\logback-core
-1.5.34.jar;C:\Users\Administrator\.m2\repository\org\apache\logging\log4j\log4j-to-slf4j\2.24.3\log4j-to-slf4j-2.24.3.
jar;C:\Users\Administrator\.m2\repository\org\apache\logging\log4j\log4j-api\2.24.3\log4j-api-2.24.3.jar;C:\Users\Admin
istrator\.m2\repository\org\slf4j\jul-to-slf4j\2.0.18\jul-to-slf4j-2.0.18.jar;C:\Users\Administrator\.m2\repository\jak
arta\annotation\jakarta.annotation-api\2.1.1\jakarta.annotation-api-2.1.1.jar;C:\Users\Administrator\.m2\repository\org
\yaml\snakeyaml\2.4\snakeyaml-2.4.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starte
r-json\3.5.15\spring-boot-starter-json-3.5.15.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\datatype\
jackson-datatype-jdk8\2.21.4\jackson-datatype-jdk8-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jacks
on\module\jackson-module-parameter-names\2.21.4\jackson-module-parameter-names-2.21.4.jar;C:\Users\Administrator\.m2\re
pository\org\springframework\boot\spring-boot-starter-tomcat\3.5.15\spring-boot-starter-tomcat-3.5.15.jar;C:\Users\Admi
nistrator\.m2\repository\org\apache\tomcat\embed\tomcat-embed-core\10.1.55\tomcat-embed-core-10.1.55.jar;C:\Users\Admin
istrator\.m2\repository\org\apache\tomcat\embed\tomcat-embed-el\10.1.55\tomcat-embed-el-10.1.55.jar;C:\Users\Administra
tor\.m2\repository\org\apache\tomcat\embed\tomcat-embed-websocket\10.1.55\tomcat-embed-websocket-10.1.55.jar;C:\Users\A
dministrator\.m2\repository\org\springframework\spring-web\6.2.19\spring-web-6.2.19.jar;C:\Users\Administrator\.m2\repo
sitory\org\springframework\spring-beans\6.2.19\spring-beans-6.2.19.jar;C:\Users\Administrator\.m2\repository\io\microme
ter\micrometer-observation\1.15.12\micrometer-observation-1.15.12.jar;C:\Users\Administrator\.m2\repository\io\micromet
er\micrometer-commons\1.15.12\micrometer-commons-1.15.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\
spring-webmvc\6.2.19\spring-webmvc-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-aop\6.2.
19\spring-aop-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-context\6.2.19\spring-context
-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-expression\6.2.19\spring-expression-6.2.19
.jar;C:\Users\Administrator\.m2\repository\com\github\xiaoymin\knife4j-openapi3-jakarta-spring-boot-starter\4.5.0\knife
4j-openapi3-jakarta-spring-boot-starter-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\github\xiaoymin\knife4j-cor
e\4.5.0\knife4j-core-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\github\xiaoymin\knife4j-openapi3-ui\4.5.0\knif
e4j-openapi3-ui-4.5.0.jar;C:\Users\Administrator\.m2\repository\org\springdoc\springdoc-openapi-starter-webmvc-ui\2.8.1
7\springdoc-openapi-starter-webmvc-ui-2.8.17.jar;C:\Users\Administrator\.m2\repository\org\springdoc\springdoc-openapi-
starter-webmvc-api\2.8.17\springdoc-openapi-starter-webmvc-api-2.8.17.jar;C:\Users\Administrator\.m2\repository\org\spr
ingdoc\springdoc-openapi-starter-common\2.8.17\springdoc-openapi-starter-common-2.8.17.jar;C:\Users\Administrator\.m2\r
epository\org\springframework\boot\spring-boot-starter-validation\3.5.15\spring-boot-starter-validation-3.5.15.jar;C:\U
sers\Administrator\.m2\repository\org\hibernate\validator\hibernate-validator\8.0.3.Final\hibernate-validator-8.0.3.Fin
al.jar;C:\Users\Administrator\.m2\repository\org\jboss\logging\jboss-logging\3.6.3.Final\jboss-logging-3.6.3.Final.jar;
C:\Users\Administrator\.m2\repository\com\fasterxml\classmate\1.7.3\classmate-1.7.3.jar;C:\Users\Administrator\.m2\repo
sitory\io\swagger\core\v3\swagger-core-jakarta\2.2.47\swagger-core-jakarta-2.2.47.jar;C:\Users\Administrator\.m2\reposi
tory\io\swagger\core\v3\swagger-annotations-jakarta\2.2.47\swagger-annotations-jakarta-2.2.47.jar;C:\Users\Administrato
r\.m2\repository\io\swagger\core\v3\swagger-models-jakarta\2.2.47\swagger-models-jakarta-2.2.47.jar;C:\Users\Administra
tor\.m2\repository\com\fasterxml\jackson\dataformat\jackson-dataformat-yaml\2.21.4\jackson-dataformat-yaml-2.21.4.jar;C
:\Users\Administrator\.m2\repository\org\webjars\swagger-ui\5.32.2\swagger-ui-5.32.2.jar;C:\Users\Administrator\.m2\rep
ository\org\webjars\webjars-locator-lite\1.1.3\webjars-locator-lite-1.1.3.jar;C:\Users\Administrator\.m2\repository\org
\jspecify\jspecify\1.0.0\jspecify-1.0.0.jar;C:\Users\Administrator\.m2\repository\org\jsoup\jsoup\1.23.2\jsoup-1.23.2.j
ar;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-redis\target\classes;C:\Users\
Administrator\.m2\repository\org\redisson\redisson-spring-boot-starter\4.7.0\redisson-spring-boot-starter-4.7.0.jar;C:\
Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-data-redis\3.5.15\spring-boot-starter-d
ata-redis-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\redisson\redisson\4.7.0\redisson-4.7.0.jar;C:\Users\Admi
nistrator\.m2\repository\io\netty\netty-common\4.2.17.Final\netty-common-4.2.17.Final.jar;C:\Users\Administrator\.m2\re
pository\io\netty\netty-codec\4.2.17.Final\netty-codec-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\
netty-codec-base\4.2.17.Final\netty-codec-base-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-co
dec-compression\4.2.17.Final\netty-codec-compression-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\ne
tty-codec-protobuf\4.2.17.Final\netty-codec-protobuf-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\ne
tty-codec-marshalling\4.2.17.Final\netty-codec-marshalling-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\ne
tty\netty-buffer\4.2.17.Final\netty-buffer-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-transp
ort\4.2.17.Final\netty-transport-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-resolver\4.2.17.
Final\netty-resolver-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-resolver-dns\4.2.17.Final\ne
tty-resolver-dns-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-codec-dns\4.2.17.Final\netty-cod
ec-dns-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-handler\4.2.17.Final\netty-handler-4.2.17.
Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-transport-native-unix-common\4.2.17.Final\netty-transpor
t-native-unix-common-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\javax\cache\cache-api\1.1.1\cache-api-1.1.1
.jar;C:\Users\Administrator\.m2\repository\io\projectreactor\reactor-core\3.7.19\reactor-core-3.7.19.jar;C:\Users\Admin
istrator\.m2\repository\org\reactivestreams\reactive-streams\1.0.4\reactive-streams-1.0.4.jar;C:\Users\Administrator\.m
2\repository\io\reactivex\rxjava3\rxjava\3.1.12\rxjava-3.1.12.jar;C:\Users\Administrator\.m2\repository\com\esotericsof
tware\kryo\5.6.2\kryo-5.6.2.jar;C:\Users\Administrator\.m2\repository\com\esotericsoftware\reflectasm\1.11.9\reflectasm
-1.11.9.jar;C:\Users\Administrator\.m2\repository\com\esotericsoftware\minlog\1.3.1\minlog-1.3.1.jar;C:\Users\Administr
ator\.m2\repository\net\bytebuddy\byte-buddy\1.17.8\byte-buddy-1.17.8.jar;C:\Users\Administrator\.m2\repository\org\jod
d\jodd-util\6.3.0\jodd-util-6.3.0.jar;C:\Users\Administrator\.m2\repository\org\redisson\redisson-spring-data-35\4.7.0\
redisson-spring-data-35-4.7.0.jar;C:\Users\Administrator\.m2\repository\org\springframework\data\spring-data-redis\3.5.
12\spring-data-redis-3.5.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\data\spring-data-keyvalue\3.5
.12\spring-data-keyvalue-3.5.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\data\spring-data-commons\
3.5.12\spring-data-commons-3.5.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-tx\6.2.19\spring
-tx-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-oxm\6.2.19\spring-oxm-6.2.19.jar;C:\Use
rs\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-cache\3.5.15\spring-boot-starter-cache-3.5
.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-context-support\6.2.19\spring-context-support-
6.2.19.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\datatype\jackson-datatype-jsr310\2.21.4\jackson-
datatype-jsr310-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\core\jackson-annotations\2.21\ja
ckson-annotations-2.21.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\core\jackson-core\2.21.4\jackson
-core-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\core\jackson-databind\2.21.4\jackson-datab
ind-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\baomidou\lock4j-redisson-spring-boot-starter\2.2.7\lock4j-redi
sson-spring-boot-starter-2.2.7.jar;C:\Users\Administrator\.m2\repository\com\baomidou\lock4j-core\2.2.7\lock4j-core-2.2
.7.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-aop\3.5.15\spring-boot-starte
r-aop-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\aspectj\aspectjweaver\1.9.25.1\aspectjweaver-1.9.25.1.jar;E:
\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-test\target\classes;E:\����֮��AI���
�ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-mybatis\target\classes;C:\Users\Administrator\.m
2\repository\com\alibaba\druid-spring-boot-3-starter\1.2.28\druid-spring-boot-3-starter-1.2.28.jar;C:\Users\Administrat
or\.m2\repository\com\alibaba\druid\1.2.28\druid-1.2.28.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-
plus-spring-boot3-starter\3.5.17\mybatis-plus-spring-boot3-starter-3.5.17.jar;C:\Users\Administrator\.m2\repository\com
\baomidou\mybatis-plus\3.5.17\mybatis-plus-3.5.17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-c
ore\3.5.17\mybatis-plus-core-3.5.17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-annotation\3.5.
17\mybatis-plus-annotation-3.5.17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-spring\3.5.17\myb
atis-plus-spring-3.5.17.jar;C:\Users\Administrator\.m2\repository\org\mybatis\mybatis\3.5.19\mybatis-3.5.19.jar;C:\User
s\Administrator\.m2\repository\org\mybatis\mybatis-spring\3.0.5\mybatis-spring-3.0.5.jar;C:\Users\Administrator\.m2\rep
ository\com\baomidou\mybatis-plus-spring-boot-autoconfigure\3.5.17\mybatis-plus-spring-boot-autoconfigure-3.5.17.jar;C:
\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-spring-boot-native-image\3.5.17\mybatis-plus-spring-boot-
native-image-3.5.17.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-jdbc\3.5.15\
spring-boot-starter-jdbc-3.5.15.jar;C:\Users\Administrator\.m2\repository\com\zaxxer\HikariCP\6.3.3\HikariCP-6.3.3.jar;
C:\Users\Administrator\.m2\repository\org\springframework\spring-jdbc\6.2.19\spring-jdbc-6.2.19.jar;C:\Users\Administra
tor\.m2\repository\com\baomidou\mybatis-plus-jsqlparser\3.5.17\mybatis-plus-jsqlparser-3.5.17.jar;C:\Users\Administrato
r\.m2\repository\com\github\jsqlparser\jsqlparser\5.2\jsqlparser-5.2.jar;C:\Users\Administrator\.m2\repository\com\baom
idou\mybatis-plus-jsqlparser-common\3.5.17\mybatis-plus-jsqlparser-common-3.5.17.jar;C:\Users\Administrator\.m2\reposit
ory\com\baomidou\mybatis-plus-extension\3.5.17\mybatis-plus-extension-3.5.17.jar;C:\Users\Administrator\.m2\repository\
com\baomidou\dynamic-datasource-spring-boot3-starter\4.5.0\dynamic-datasource-spring-boot3-starter-4.5.0.jar;C:\Users\A
dministrator\.m2\repository\com\baomidou\dynamic-datasource-spring-boot-common\4.5.0\dynamic-datasource-spring-boot-com
mon-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\baomidou\dynamic-datasource-spring\4.5.0\dynamic-datasource-spr
ing-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\baomidou\dynamic-datasource-creator\4.5.0\dynamic-datasource-cr
eator-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\oracle\database\spring\oracle-spring-boot-starter-ucp\23.4.0\
oracle-spring-boot-starter-ucp-23.4.0.jar;C:\Users\Administrator\.m2\repository\com\oracle\database\jdbc\ojdbc11\23.7.0
.25.01\ojdbc11-23.7.0.25.01.jar;C:\Users\Administrator\.m2\repository\com\oracle\database\jdbc\ucp\23.7.0.25.01\ucp-23.
7.0.25.01.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-join-boot-starter\1.5.9\mybatis-p
lus-join-boot-starter-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-join-extension\
1.5.9\mybatis-plus-join-extension-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-joi
n-core\1.5.9\mybatis-plus-join-core-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-j
oin-annotation\1.5.9\mybatis-plus-join-annotation-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\
mybatis-plus-join-adapter-base\1.5.9\mybatis-plus-join-adapter-base-1.5.9.jar;C:\Users\Administrator\.m2\repository\com
\github\yulichang\mybatis-plus-join-adapter-jsqlparser\1.5.9\mybatis-plus-join-adapter-jsqlparser-1.5.9.jar;C:\Users\Ad
ministrator\.m2\repository\com\github\yulichang\mybatis-plus-join-wrapper-ext\1.5.9\mybatis-plus-join-wrapper-ext-1.5.9
.jar;C:\Users\Administrator\.m2\repository\org\dromara\easy-trans-spring-boot-starter\3.1.8\easy-trans-spring-boot-star
ter-3.1.8.jar;C:\Users\Administrator\.m2\repository\org\dromara\easy-trans-service\3.1.8\easy-trans-service-3.1.8.jar;C
:\Users\Administrator\.m2\repository\com\github\ben-manes\caffeine\caffeine\3.2.4\caffeine-3.2.4.jar;C:\Users\Administr
ator\.m2\repository\org\dromara\easy-trans-mybatis-plus-extend\3.1.8\easy-trans-mybatis-plus-extend-3.1.8.jar;C:\Users\
Administrator\.m2\repository\org\mockito\mockito-inline\5.2.0\mockito-inline-5.2.0.jar;C:\Users\Administrator\.m2\repos
itory\org\mockito\mockito-core\5.17.0\mockito-core-5.17.0.jar;C:\Users\Administrator\.m2\repository\net\bytebuddy\byte-
buddy-agent\1.17.8\byte-buddy-agent-1.17.8.jar;C:\Users\Administrator\.m2\repository\org\objenesis\objenesis\3.3\objene
sis-3.3.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-test\3.5.15\spring-boot-
starter-test-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-test\3.5.15\spring-b
oot-test-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-test-autoconfigure\3.5.1
5\spring-boot-test-autoconfigure-3.5.15.jar;C:\Users\Administrator\.m2\repository\com\jayway\jsonpath\json-path\2.9.0\j
son-path-2.9.0.jar;C:\Users\Administrator\.m2\repository\jakarta\xml\bind\jakarta.xml.bind-api\4.0.5\jakarta.xml.bind-a
pi-4.0.5.jar;C:\Users\Administrator\.m2\repository\jakarta\activation\jakarta.activation-api\2.1.4\jakarta.activation-a
pi-2.1.4.jar;C:\Users\Administrator\.m2\repository\net\minidev\json-smart\2.5.2\json-smart-2.5.2.jar;C:\Users\Administr
ator\.m2\repository\net\minidev\accessors-smart\2.5.2\accessors-smart-2.5.2.jar;C:\Users\Administrator\.m2\repository\o
rg\assertj\assertj-core\3.27.7\assertj-core-3.27.7.jar;C:\Users\Administrator\.m2\repository\org\awaitility\awaitility\
4.2.2\awaitility-4.2.2.jar;C:\Users\Administrator\.m2\repository\org\hamcrest\hamcrest\3.0\hamcrest-3.0.jar;C:\Users\Ad
ministrator\.m2\repository\org\junit\jupiter\junit-jupiter\5.12.2\junit-jupiter-5.12.2.jar;C:\Users\Administrator\.m2\r
epository\org\junit\jupiter\junit-jupiter-api\5.12.2\junit-jupiter-api-5.12.2.jar;C:\Users\Administrator\.m2\repository
\org\opentest4j\opentest4j\1.3.0\opentest4j-1.3.0.jar;C:\Users\Administrator\.m2\repository\org\junit\platform\junit-pl
atform-commons\1.12.2\junit-platform-commons-1.12.2.jar;C:\Users\Administrator\.m2\repository\org\apiguardian\apiguardi
an-api\1.1.2\apiguardian-api-1.1.2.jar;C:\Users\Administrator\.m2\repository\org\junit\jupiter\junit-jupiter-params\5.1
2.2\junit-jupiter-params-5.12.2.jar;C:\Users\Administrator\.m2\repository\org\junit\jupiter\junit-jupiter-engine\5.12.2
\junit-jupiter-engine-5.12.2.jar;C:\Users\Administrator\.m2\repository\org\junit\platform\junit-platform-engine\1.12.2\
junit-platform-engine-1.12.2.jar;C:\Users\Administrator\.m2\repository\org\mockito\mockito-junit-jupiter\5.17.0\mockito
-junit-jupiter-5.17.0.jar;C:\Users\Administrator\.m2\repository\org\skyscreamer\jsonassert\1.5.3\jsonassert-1.5.3.jar;C
:\Users\Administrator\.m2\repository\com\vaadin\external\google\android-json\0.0.20131108.vaadin1\android-json-0.0.2013
1108.vaadin1.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-core\6.2.19\spring-core-6.2.19.jar;C:
\Users\Administrator\.m2\repository\org\springframework\spring-jcl\6.2.19\spring-jcl-6.2.19.jar;C:\Users\Administrator\
.m2\repository\org\springframework\spring-test\6.2.19\spring-test-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\
xmlunit\xmlunit-core\2.10.4\xmlunit-core-2.10.4.jar;C:\Users\Administrator\.m2\repository\com\h2database\h2\2.3.232\h2-
2.3.232.jar;C:\Users\Administrator\.m2\repository\com\github\fppt\jedis-mock\1.1.18\jedis-mock-1.1.18.jar;C:\Users\Admi
nistrator\.m2\repository\org\slf4j\slf4j-api\2.0.18\slf4j-api-2.0.18.jar;C:\Users\Administrator\.m2\repository\org\luaj
\luaj-jse\3.0.1\luaj-jse-3.0.1.jar;C:\Users\Administrator\.m2\repository\redis\clients\jedis\6.0.0\jedis-6.0.0.jar;C:\U
sers\Administrator\.m2\repository\org\apache\commons\commons-pool2\2.12.1\commons-pool2-2.12.1.jar;C:\Users\Administrat
or\.m2\repository\org\json\json\20250107\json-20250107.jar;C:\Users\Administrator\.m2\repository\com\google\code\gson\g
son\2.13.2\gson-2.13.2.jar;C:\Users\Administrator\.m2\repository\com\google\errorprone\error_prone_annotations\2.41.0\e
rror_prone_annotations-2.41.0.jar;C:\Users\Administrator\.m2\repository\redis\clients\authentication\redis-authx-core\0
.1.1-beta2\redis-authx-core-0.1.1-beta2.jar;C:\Users\Administrator\.m2\repository\uk\co\jemos\podam\podam\8.0.2.RELEASE
\podam-8.0.2.RELEASE.jar;C:\Users\Administrator\.m2\repository\net\jcip\jcip-annotations\1.0\jcip-annotations-1.0.jar;C
:\Users\Administrator\.m2\repository\jakarta\validation\jakarta.validation-api\3.0.2\jakarta.validation-api-3.0.2.jar;C
:\Users\Administrator\.m2\repository\org\apache\commons\commons-lang3\3.20.0\commons-lang3-3.20.0.jar;"/>
    <property name="java.vm.vendor" value="Eclipse Adoptium"/>
    <property name="sun.arch.data.model" value="64"/>
    <property name="user.variant" value=""/>
    <property name="java.vendor.url" value="https://adoptium.net/"/>
    <property name="user.timezone" value="Asia/Shanghai"/>
    <property name="os.name" value="Windows 10"/>
    <property name="java.vm.specification.version" value="17"/>
    <property name="sun.java.launcher" value="SUN_STANDARD"/>
    <property name="user.country" value="CN"/>
    <property name="sun.boot.library.path" value="E:\����֮��AI����ƽ̨����\tools\jdk-17.0.20.1+1\bin"/>
    <property name="sun.java.command" value="C:\Users\Administrator\AppData\Local\Temp\surefire12053511136342888388\sur
efirebooter-20260912135129227_23.jar C:\Users\Administrator\AppData\Local\Temp\surefire12053511136342888388 2026-09-12T
13-51-22_032-jvmRun1 surefire-20260912135129227_21tmp surefire_4-20260912135129227_22tmp"/>
    <property name="jdk.debug" value="release"/>
    <property name="surefire.test.class.path" value="E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-
spring-boot-starter-protection\target\test-classes;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-sp
ring-boot-starter-protection\target\classes;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-bo
ot-starter-web\target\classes;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-common\target\classes;C
:\Users\Administrator\.m2\repository\io\opentelemetry\opentelemetry-api\1.65.0\opentelemetry-api-1.65.0.jar;C:\Users\Ad
ministrator\.m2\repository\io\opentelemetry\opentelemetry-context\1.49.0\opentelemetry-context-1.49.0.jar;C:\Users\Admi
nistrator\.m2\repository\org\projectlombok\lombok\1.18.46\lombok-1.18.46.jar;C:\Users\Administrator\.m2\repository\org\
mapstruct\mapstruct\1.6.3\mapstruct-1.6.3.jar;C:\Users\Administrator\.m2\repository\org\mapstruct\mapstruct-jdk8\1.6.3\
mapstruct-jdk8-1.6.3.jar;C:\Users\Administrator\.m2\repository\org\mapstruct\mapstruct-processor\1.6.3\mapstruct-proces
sor-1.6.3.jar;C:\Users\Administrator\.m2\repository\cn\hutool\hutool-all\5.8.47\hutool-all-5.8.47.jar;C:\Users\Administ
rator\.m2\repository\com\alibaba\transmittable-thread-local\2.14.5\transmittable-thread-local-2.14.5.jar;C:\Users\Admin
istrator\.m2\repository\com\alibaba\fastjson\2.0.64\fastjson-2.0.64.jar;C:\Users\Administrator\.m2\repository\com\aliba
ba\fastjson2\fastjson2-extension\2.0.64\fastjson2-extension-2.0.64.jar;C:\Users\Administrator\.m2\repository\com\alibab
a\fastjson2\fastjson2\2.0.64\fastjson2-2.0.64.jar;C:\Users\Administrator\.m2\repository\org\dromara\easy-trans-anno\3.1
.8\easy-trans-anno-3.1.8.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-web\3.5
.15\spring-boot-starter-web-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-start
er\3.5.15\spring-boot-starter-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot\3.5
.15\spring-boot-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-autoconfigure\3.5
.15\spring-boot-autoconfigure-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-sta
rter-logging\3.5.15\spring-boot-starter-logging-3.5.15.jar;C:\Users\Administrator\.m2\repository\ch\qos\logback\logback
-classic\1.5.34\logback-classic-1.5.34.jar;C:\Users\Administrator\.m2\repository\ch\qos\logback\logback-core\1.5.34\log
back-core-1.5.34.jar;C:\Users\Administrator\.m2\repository\org\apache\logging\log4j\log4j-to-slf4j\2.24.3\log4j-to-slf4
j-2.24.3.jar;C:\Users\Administrator\.m2\repository\org\apache\logging\log4j\log4j-api\2.24.3\log4j-api-2.24.3.jar;C:\Us
ers\Administrator\.m2\repository\org\slf4j\jul-to-slf4j\2.0.18\jul-to-slf4j-2.0.18.jar;C:\Users\Administrator\.m2\repos
itory\jakarta\annotation\jakarta.annotation-api\2.1.1\jakarta.annotation-api-2.1.1.jar;C:\Users\Administrator\.m2\repos
itory\org\yaml\snakeyaml\2.4\snakeyaml-2.4.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-bo
ot-starter-json\3.5.15\spring-boot-starter-json-3.5.15.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\
datatype\jackson-datatype-jdk8\2.21.4\jackson-datatype-jdk8-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\faster
xml\jackson\module\jackson-module-parameter-names\2.21.4\jackson-module-parameter-names-2.21.4.jar;C:\Users\Administrat
or\.m2\repository\org\springframework\boot\spring-boot-starter-tomcat\3.5.15\spring-boot-starter-tomcat-3.5.15.jar;C:\U
sers\Administrator\.m2\repository\org\apache\tomcat\embed\tomcat-embed-core\10.1.55\tomcat-embed-core-10.1.55.jar;C:\Us
ers\Administrator\.m2\repository\org\apache\tomcat\embed\tomcat-embed-el\10.1.55\tomcat-embed-el-10.1.55.jar;C:\Users\A
dministrator\.m2\repository\org\apache\tomcat\embed\tomcat-embed-websocket\10.1.55\tomcat-embed-websocket-10.1.55.jar;C
:\Users\Administrator\.m2\repository\org\springframework\spring-web\6.2.19\spring-web-6.2.19.jar;C:\Users\Administrator
\.m2\repository\org\springframework\spring-beans\6.2.19\spring-beans-6.2.19.jar;C:\Users\Administrator\.m2\repository\i
o\micrometer\micrometer-observation\1.15.12\micrometer-observation-1.15.12.jar;C:\Users\Administrator\.m2\repository\io
\micrometer\micrometer-commons\1.15.12\micrometer-commons-1.15.12.jar;C:\Users\Administrator\.m2\repository\org\springf
ramework\spring-webmvc\6.2.19\spring-webmvc-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring
-aop\6.2.19\spring-aop-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-context\6.2.19\sprin
g-context-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-expression\6.2.19\spring-expressi
on-6.2.19.jar;C:\Users\Administrator\.m2\repository\com\github\xiaoymin\knife4j-openapi3-jakarta-spring-boot-starter\4.
5.0\knife4j-openapi3-jakarta-spring-boot-starter-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\github\xiaoymin\kn
ife4j-core\4.5.0\knife4j-core-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\github\xiaoymin\knife4j-openapi3-ui\4
.5.0\knife4j-openapi3-ui-4.5.0.jar;C:\Users\Administrator\.m2\repository\org\springdoc\springdoc-openapi-starter-webmvc
-ui\2.8.17\springdoc-openapi-starter-webmvc-ui-2.8.17.jar;C:\Users\Administrator\.m2\repository\org\springdoc\springdoc
-openapi-starter-webmvc-api\2.8.17\springdoc-openapi-starter-webmvc-api-2.8.17.jar;C:\Users\Administrator\.m2\repositor
y\org\springdoc\springdoc-openapi-starter-common\2.8.17\springdoc-openapi-starter-common-2.8.17.jar;C:\Users\Administra
tor\.m2\repository\org\springframework\boot\spring-boot-starter-validation\3.5.15\spring-boot-starter-validation-3.5.15
.jar;C:\Users\Administrator\.m2\repository\org\hibernate\validator\hibernate-validator\8.0.3.Final\hibernate-validator-
8.0.3.Final.jar;C:\Users\Administrator\.m2\repository\org\jboss\logging\jboss-logging\3.6.3.Final\jboss-logging-3.6.3.F
inal.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\classmate\1.7.3\classmate-1.7.3.jar;C:\Users\Administrator
\.m2\repository\io\swagger\core\v3\swagger-core-jakarta\2.2.47\swagger-core-jakarta-2.2.47.jar;C:\Users\Administrator\.
m2\repository\io\swagger\core\v3\swagger-annotations-jakarta\2.2.47\swagger-annotations-jakarta-2.2.47.jar;C:\Users\Adm
inistrator\.m2\repository\io\swagger\core\v3\swagger-models-jakarta\2.2.47\swagger-models-jakarta-2.2.47.jar;C:\Users\A
dministrator\.m2\repository\com\fasterxml\jackson\dataformat\jackson-dataformat-yaml\2.21.4\jackson-dataformat-yaml-2.2
1.4.jar;C:\Users\Administrator\.m2\repository\org\webjars\swagger-ui\5.32.2\swagger-ui-5.32.2.jar;C:\Users\Administrato
r\.m2\repository\org\webjars\webjars-locator-lite\1.1.3\webjars-locator-lite-1.1.3.jar;C:\Users\Administrator\.m2\repos
itory\org\jspecify\jspecify\1.0.0\jspecify-1.0.0.jar;C:\Users\Administrator\.m2\repository\org\jsoup\jsoup\1.23.2\jsoup
-1.23.2.jar;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-redis\target\classes;
C:\Users\Administrator\.m2\repository\org\redisson\redisson-spring-boot-starter\4.7.0\redisson-spring-boot-starter-4.7.
0.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-data-redis\3.5.15\spring-boot-
starter-data-redis-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\redisson\redisson\4.7.0\redisson-4.7.0.jar;C:\U
sers\Administrator\.m2\repository\io\netty\netty-common\4.2.17.Final\netty-common-4.2.17.Final.jar;C:\Users\Administrat
or\.m2\repository\io\netty\netty-codec\4.2.17.Final\netty-codec-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\
io\netty\netty-codec-base\4.2.17.Final\netty-codec-base-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty
\netty-codec-compression\4.2.17.Final\netty-codec-compression-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io
\netty\netty-codec-protobuf\4.2.17.Final\netty-codec-protobuf-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io
\netty\netty-codec-marshalling\4.2.17.Final\netty-codec-marshalling-4.2.17.Final.jar;C:\Users\Administrator\.m2\reposit
ory\io\netty\netty-buffer\4.2.17.Final\netty-buffer-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\net
ty-transport\4.2.17.Final\netty-transport-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-resolve
r\4.2.17.Final\netty-resolver-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-resolver-dns\4.2.17
.Final\netty-resolver-dns-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-codec-dns\4.2.17.Final\
netty-codec-dns-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-handler\4.2.17.Final\netty-handle
r-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-transport-native-unix-common\4.2.17.Final\netty
-transport-native-unix-common-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\javax\cache\cache-api\1.1.1\cache-
api-1.1.1.jar;C:\Users\Administrator\.m2\repository\io\projectreactor\reactor-core\3.7.19\reactor-core-3.7.19.jar;C:\Us
ers\Administrator\.m2\repository\org\reactivestreams\reactive-streams\1.0.4\reactive-streams-1.0.4.jar;C:\Users\Adminis
trator\.m2\repository\io\reactivex\rxjava3\rxjava\3.1.12\rxjava-3.1.12.jar;C:\Users\Administrator\.m2\repository\com\es
otericsoftware\kryo\5.6.2\kryo-5.6.2.jar;C:\Users\Administrator\.m2\repository\com\esotericsoftware\reflectasm\1.11.9\r
eflectasm-1.11.9.jar;C:\Users\Administrator\.m2\repository\com\esotericsoftware\minlog\1.3.1\minlog-1.3.1.jar;C:\Users\
Administrator\.m2\repository\net\bytebuddy\byte-buddy\1.17.8\byte-buddy-1.17.8.jar;C:\Users\Administrator\.m2\repositor
y\org\jodd\jodd-util\6.3.0\jodd-util-6.3.0.jar;C:\Users\Administrator\.m2\repository\org\redisson\redisson-spring-data-
35\4.7.0\redisson-spring-data-35-4.7.0.jar;C:\Users\Administrator\.m2\repository\org\springframework\data\spring-data-r
edis\3.5.12\spring-data-redis-3.5.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\data\spring-data-key
value\3.5.12\spring-data-keyvalue-3.5.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\data\spring-data
-commons\3.5.12\spring-data-commons-3.5.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-tx\6.2.
19\spring-tx-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-oxm\6.2.19\spring-oxm-6.2.19.j
ar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-cache\3.5.15\spring-boot-starter-
cache-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-context-support\6.2.19\spring-context
-support-6.2.19.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\datatype\jackson-datatype-jsr310\2.21.4
\jackson-datatype-jsr310-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\core\jackson-annotation
s\2.21\jackson-annotations-2.21.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\core\jackson-core\2.21.
4\jackson-core-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\core\jackson-databind\2.21.4\jack
son-databind-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\baomidou\lock4j-redisson-spring-boot-starter\2.2.7\lo
ck4j-redisson-spring-boot-starter-2.2.7.jar;C:\Users\Administrator\.m2\repository\com\baomidou\lock4j-core\2.2.7\lock4j
-core-2.2.7.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-aop\3.5.15\spring-bo
ot-starter-aop-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\aspectj\aspectjweaver\1.9.25.1\aspectjweaver-1.9.25
.1.jar;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-test\target\classes;E:\���
�֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-mybatis\target\classes;C:\Users\Adminis
trator\.m2\repository\com\alibaba\druid-spring-boot-3-starter\1.2.28\druid-spring-boot-3-starter-1.2.28.jar;C:\Users\Ad
ministrator\.m2\repository\com\alibaba\druid\1.2.28\druid-1.2.28.jar;C:\Users\Administrator\.m2\repository\com\baomidou
\mybatis-plus-spring-boot3-starter\3.5.17\mybatis-plus-spring-boot3-starter-3.5.17.jar;C:\Users\Administrator\.m2\repos
itory\com\baomidou\mybatis-plus\3.5.17\mybatis-plus-3.5.17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybat
is-plus-core\3.5.17\mybatis-plus-core-3.5.17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-annota
tion\3.5.17\mybatis-plus-annotation-3.5.17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-spring\3
.5.17\mybatis-plus-spring-3.5.17.jar;C:\Users\Administrator\.m2\repository\org\mybatis\mybatis\3.5.19\mybatis-3.5.19.ja
r;C:\Users\Administrator\.m2\repository\org\mybatis\mybatis-spring\3.0.5\mybatis-spring-3.0.5.jar;C:\Users\Administrato
r\.m2\repository\com\baomidou\mybatis-plus-spring-boot-autoconfigure\3.5.17\mybatis-plus-spring-boot-autoconfigure-3.5.
17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-spring-boot-native-image\3.5.17\mybatis-plus-spr
ing-boot-native-image-3.5.17.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-jdb
c\3.5.15\spring-boot-starter-jdbc-3.5.15.jar;C:\Users\Administrator\.m2\repository\com\zaxxer\HikariCP\6.3.3\HikariCP-6
.3.3.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-jdbc\6.2.19\spring-jdbc-6.2.19.jar;C:\Users\A
dministrator\.m2\repository\com\baomidou\mybatis-plus-jsqlparser\3.5.17\mybatis-plus-jsqlparser-3.5.17.jar;C:\Users\Adm
inistrator\.m2\repository\com\github\jsqlparser\jsqlparser\5.2\jsqlparser-5.2.jar;C:\Users\Administrator\.m2\repository
\com\baomidou\mybatis-plus-jsqlparser-common\3.5.17\mybatis-plus-jsqlparser-common-3.5.17.jar;C:\Users\Administrator\.m
2\repository\com\baomidou\mybatis-plus-extension\3.5.17\mybatis-plus-extension-3.5.17.jar;C:\Users\Administrator\.m2\re
pository\com\baomidou\dynamic-datasource-spring-boot3-starter\4.5.0\dynamic-datasource-spring-boot3-starter-4.5.0.jar;C
:\Users\Administrator\.m2\repository\com\baomidou\dynamic-datasource-spring-boot-common\4.5.0\dynamic-datasource-spring
-boot-common-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\baomidou\dynamic-datasource-spring\4.5.0\dynamic-datas
ource-spring-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\baomidou\dynamic-datasource-creator\4.5.0\dynamic-data
source-creator-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\oracle\database\spring\oracle-spring-boot-starter-uc
p\23.4.0\oracle-spring-boot-starter-ucp-23.4.0.jar;C:\Users\Administrator\.m2\repository\com\oracle\database\jdbc\ojdbc
11\23.7.0.25.01\ojdbc11-23.7.0.25.01.jar;C:\Users\Administrator\.m2\repository\com\oracle\database\jdbc\ucp\23.7.0.25.0
1\ucp-23.7.0.25.01.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-join-boot-starter\1.5.9\
mybatis-plus-join-boot-starter-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-join-e
xtension\1.5.9\mybatis-plus-join-extension-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis
-plus-join-core\1.5.9\mybatis-plus-join-core-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybat
is-plus-join-annotation\1.5.9\mybatis-plus-join-annotation-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\y
ulichang\mybatis-plus-join-adapter-base\1.5.9\mybatis-plus-join-adapter-base-1.5.9.jar;C:\Users\Administrator\.m2\repos
itory\com\github\yulichang\mybatis-plus-join-adapter-jsqlparser\1.5.9\mybatis-plus-join-adapter-jsqlparser-1.5.9.jar;C:
\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-join-wrapper-ext\1.5.9\mybatis-plus-join-wrapper-
ext-1.5.9.jar;C:\Users\Administrator\.m2\repository\org\dromara\easy-trans-spring-boot-starter\3.1.8\easy-trans-spring-
boot-starter-3.1.8.jar;C:\Users\Administrator\.m2\repository\org\dromara\easy-trans-service\3.1.8\easy-trans-service-3.
1.8.jar;C:\Users\Administrator\.m2\repository\com\github\ben-manes\caffeine\caffeine\3.2.4\caffeine-3.2.4.jar;C:\Users\
Administrator\.m2\repository\org\dromara\easy-trans-mybatis-plus-extend\3.1.8\easy-trans-mybatis-plus-extend-3.1.8.jar;
C:\Users\Administrator\.m2\repository\org\mockito\mockito-inline\5.2.0\mockito-inline-5.2.0.jar;C:\Users\Administrator\
.m2\repository\org\mockito\mockito-core\5.17.0\mockito-core-5.17.0.jar;C:\Users\Administrator\.m2\repository\net\bytebu
ddy\byte-buddy-agent\1.17.8\byte-buddy-agent-1.17.8.jar;C:\Users\Administrator\.m2\repository\org\objenesis\objenesis\3
.3\objenesis-3.3.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-test\3.5.15\spr
ing-boot-starter-test-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-test\3.5.15
\spring-boot-test-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-test-autoconfig
ure\3.5.15\spring-boot-test-autoconfigure-3.5.15.jar;C:\Users\Administrator\.m2\repository\com\jayway\jsonpath\json-pat
h\2.9.0\json-path-2.9.0.jar;C:\Users\Administrator\.m2\repository\jakarta\xml\bind\jakarta.xml.bind-api\4.0.5\jakarta.x
ml.bind-api-4.0.5.jar;C:\Users\Administrator\.m2\repository\jakarta\activation\jakarta.activation-api\2.1.4\jakarta.act
ivation-api-2.1.4.jar;C:\Users\Administrator\.m2\repository\net\minidev\json-smart\2.5.2\json-smart-2.5.2.jar;C:\Users\
Administrator\.m2\repository\net\minidev\accessors-smart\2.5.2\accessors-smart-2.5.2.jar;C:\Users\Administrator\.m2\rep
ository\org\assertj\assertj-core\3.27.7\assertj-core-3.27.7.jar;C:\Users\Administrator\.m2\repository\org\awaitility\aw
aitility\4.2.2\awaitility-4.2.2.jar;C:\Users\Administrator\.m2\repository\org\hamcrest\hamcrest\3.0\hamcrest-3.0.jar;C:
\Users\Administrator\.m2\repository\org\junit\jupiter\junit-jupiter\5.12.2\junit-jupiter-5.12.2.jar;C:\Users\Administra
tor\.m2\repository\org\junit\jupiter\junit-jupiter-api\5.12.2\junit-jupiter-api-5.12.2.jar;C:\Users\Administrator\.m2\r
epository\org\opentest4j\opentest4j\1.3.0\opentest4j-1.3.0.jar;C:\Users\Administrator\.m2\repository\org\junit\platform
\junit-platform-commons\1.12.2\junit-platform-commons-1.12.2.jar;C:\Users\Administrator\.m2\repository\org\apiguardian\
apiguardian-api\1.1.2\apiguardian-api-1.1.2.jar;C:\Users\Administrator\.m2\repository\org\junit\jupiter\junit-jupiter-p
arams\5.12.2\junit-jupiter-params-5.12.2.jar;C:\Users\Administrator\.m2\repository\org\junit\jupiter\junit-jupiter-engi
ne\5.12.2\junit-jupiter-engine-5.12.2.jar;C:\Users\Administrator\.m2\repository\org\junit\platform\junit-platform-engin
e\1.12.2\junit-platform-engine-1.12.2.jar;C:\Users\Administrator\.m2\repository\org\mockito\mockito-junit-jupiter\5.17.
0\mockito-junit-jupiter-5.17.0.jar;C:\Users\Administrator\.m2\repository\org\skyscreamer\jsonassert\1.5.3\jsonassert-1.
5.3.jar;C:\Users\Administrator\.m2\repository\com\vaadin\external\google\android-json\0.0.20131108.vaadin1\android-json
-0.0.20131108.vaadin1.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-core\6.2.19\spring-core-6.2.
19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-jcl\6.2.19\spring-jcl-6.2.19.jar;C:\Users\Admin
istrator\.m2\repository\org\springframework\spring-test\6.2.19\spring-test-6.2.19.jar;C:\Users\Administrator\.m2\reposi
tory\org\xmlunit\xmlunit-core\2.10.4\xmlunit-core-2.10.4.jar;C:\Users\Administrator\.m2\repository\com\h2database\h2\2.
3.232\h2-2.3.232.jar;C:\Users\Administrator\.m2\repository\com\github\fppt\jedis-mock\1.1.18\jedis-mock-1.1.18.jar;C:\U
sers\Administrator\.m2\repository\org\slf4j\slf4j-api\2.0.18\slf4j-api-2.0.18.jar;C:\Users\Administrator\.m2\repository
\org\luaj\luaj-jse\3.0.1\luaj-jse-3.0.1.jar;C:\Users\Administrator\.m2\repository\redis\clients\jedis\6.0.0\jedis-6.0.0
.jar;C:\Users\Administrator\.m2\repository\org\apache\commons\commons-pool2\2.12.1\commons-pool2-2.12.1.jar;C:\Users\Ad
ministrator\.m2\repository\org\json\json\20250107\json-20250107.jar;C:\Users\Administrator\.m2\repository\com\google\co
de\gson\gson\2.13.2\gson-2.13.2.jar;C:\Users\Administrator\.m2\repository\com\google\errorprone\error_prone_annotations
\2.41.0\error_prone_annotations-2.41.0.jar;C:\Users\Administrator\.m2\repository\redis\clients\authentication\redis-aut
hx-core\0.1.1-beta2\redis-authx-core-0.1.1-beta2.jar;C:\Users\Administrator\.m2\repository\uk\co\jemos\podam\podam\8.0.
2.RELEASE\podam-8.0.2.RELEASE.jar;C:\Users\Administrator\.m2\repository\net\jcip\jcip-annotations\1.0\jcip-annotations-
1.0.jar;C:\Users\Administrator\.m2\repository\jakarta\validation\jakarta.validation-api\3.0.2\jakarta.validation-api-3.
0.2.jar;C:\Users\Administrator\.m2\repository\org\apache\commons\commons-lang3\3.20.0\commons-lang3-3.20.0.jar;"/>
    <property name="sun.cpu.endian" value="little"/>
    <property name="user.home" value="C:\Users\Administrator"/>
    <property name="user.language" value="zh"/>
    <property name="java.specification.vendor" value="Oracle Corporation"/>
    <property name="java.version.date" value="2026-08-18"/>
    <property name="java.home" value="E:\����֮��AI����ƽ̨����\tools\jdk-17.0.20.1+1"/>
    <property name="file.separator" value="\"/>
    <property name="basedir" value="E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-start
er-protection"/>
    <property name="java.vm.compressedOopsMode" value="Zero based"/>
    <property name="line.separator" value="&#10;"/>
    <property name="java.vm.specification.vendor" value="Oracle Corporation"/>
    <property name="java.specification.name" value="Java Platform API Specification"/>
    <property name="surefire.real.class.path" value="C:\Users\Administrator\AppData\Local\Temp\surefire1205351113634288
8388\surefirebooter-20260912135129227_23.jar"/>
    <property name="user.script" value=""/>
    <property name="sun.management.compiler" value="HotSpot 64-Bit Tiered Compilers"/>
    <property name="java.runtime.version" value="17.0.20.1+1"/>
    <property name="user.name" value="Administrator"/>
    <property name="path.separator" value=";"/>
    <property name="os.version" value="10.0"/>
    <property name="java.runtime.name" value="OpenJDK Runtime Environment"/>
    <property name="file.encoding" value="GBK"/>
    <property name="java.vm.name" value="OpenJDK 64-Bit Server VM"/>
    <property name="java.vendor.version" value="Temurin-17.0.20.1+1"/>
    <property name="localRepository" value="C:\Users\Administrator\.m2\repository"/>
    <property name="java.vendor.url.bug" value="https://github.com/adoptium/adoptium-support/issues"/>
    <property name="java.io.tmpdir" value="C:\Users\ADMINI~1\AppData\Local\Temp\"/>
    <property name="java.version" value="17.0.20.1"/>
    <property name="user.dir" value="E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-star
ter-protection"/>
    <property name="os.arch" value="amd64"/>
    <property name="java.vm.specification.name" value="Java Virtual Machine Specification"/>
    <property name="sun.os.patch.level" value=""/>
    <property name="native.encoding" value="GBK"/>
    <property name="java.library.path" value="E:\����֮��AI����ƽ̨����\tools\jdk-17.0.20.1+1\bin;C:\windows\Sun\Java\bin;
C:\windows\system32;C:\windows;E:\����֮��AI����ƽ̨����\tools\jdk-17.0.20.1+1\bin;E:\����֮��AI����ƽ̨����\tools\apache-mav
en-3.9.9\bin;C:\Program Files\Python311\Scripts\;C:\Program Files\Python311\;C:\windows\system32;C:\windows;C:\windows\
System32\Wbem;C:\windows\System32\WindowsPowerShell\v1.0\;C:\windows\System32\OpenSSH\;D:\Program Files (x86)\Tencent\΢
��web�����߹���;;C:\Program Files\Git\cmd;C:\Program Files\nodejs\;C:\Users\Administrator\AppData\Local\Microsoft\Window
sApps;;C:\Users\Administrator\AppData\Local\Programs\DockerDesktop\resources\bin;C:\Users\Administrator\AppData\Roaming
\npm;D:\Program Files\Qoder IDE\bin;C:\Users\Administrator\AppData\Local\Microsoft\WinGet\Links;;."/>
    <property name="java.vm.info" value="mixed mode, sharing"/>
    <property name="java.vendor" value="Eclipse Adoptium"/>
    <property name="java.vm.version" value="17.0.20.1+1"/>
    <property name="java.specification.maintenance.version" value="1"/>
    <property name="sun.io.unicode.encoding" value="UnicodeLittle"/>
    <property name="java.class.version" value="61.0"/>
  </properties>
  <testcase name="testBeforePointCut_signatureFailed_argsSanitizedInLog" classname="cn.zszj.framework.signature.core.ao
p.ApiSignatureAspectTest" time="0.055">
    <system-out><![CDATA[13:51:31.365 [main] ERROR cn.zszj.framework.signature.core.aop.ApiSignatureAspect -- [beforePo
intCut][����OpenApiController.queryOrder(..) ����([{"username":"zhangsan-user","password":"***","token":"***","nested":
{"apiKey":"***","note":"hello-note"}}]) ǩ��ʧ��]
]]></system-out>
  </testcase>
</testsuite>
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\surefire-report
s\TEST-cn.zszj.framework.signature.core.ApiSignatureTest.xml
<?xml version="1.0" encoding="UTF-8"?>
<testsuite xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="https://maven.apache.or
g/surefire/maven-surefire-plugin/xsd/surefire-test-report.xsd" version="3.0.2" name="cn.zszj.framework.signature.core.A
piSignatureTest" time="0.044" tests="1" errors="0" skipped="0" failures="0">
  <properties>
    <property name="java.specification.version" value="17"/>
    <property name="sun.cpu.isalist" value="amd64"/>
    <property name="sun.jnu.encoding" value="GBK"/>
    <property name="java.class.path" value="E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-bo
ot-starter-protection\target\test-classes;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot
-starter-protection\target\classes;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starte
r-web\target\classes;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-common\target\classes;C:\Users\A
dministrator\.m2\repository\io\opentelemetry\opentelemetry-api\1.65.0\opentelemetry-api-1.65.0.jar;C:\Users\Administrat
or\.m2\repository\io\opentelemetry\opentelemetry-context\1.49.0\opentelemetry-context-1.49.0.jar;C:\Users\Administrator
\.m2\repository\org\projectlombok\lombok\1.18.46\lombok-1.18.46.jar;C:\Users\Administrator\.m2\repository\org\mapstruct
\mapstruct\1.6.3\mapstruct-1.6.3.jar;C:\Users\Administrator\.m2\repository\org\mapstruct\mapstruct-jdk8\1.6.3\mapstruct
-jdk8-1.6.3.jar;C:\Users\Administrator\.m2\repository\org\mapstruct\mapstruct-processor\1.6.3\mapstruct-processor-1.6.3
.jar;C:\Users\Administrator\.m2\repository\cn\hutool\hutool-all\5.8.47\hutool-all-5.8.47.jar;C:\Users\Administrator\.m2
\repository\com\alibaba\transmittable-thread-local\2.14.5\transmittable-thread-local-2.14.5.jar;C:\Users\Administrator\
.m2\repository\com\alibaba\fastjson\2.0.64\fastjson-2.0.64.jar;C:\Users\Administrator\.m2\repository\com\alibaba\fastjs
on2\fastjson2-extension\2.0.64\fastjson2-extension-2.0.64.jar;C:\Users\Administrator\.m2\repository\com\alibaba\fastjso
n2\fastjson2\2.0.64\fastjson2-2.0.64.jar;C:\Users\Administrator\.m2\repository\org\dromara\easy-trans-anno\3.1.8\easy-t
rans-anno-3.1.8.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-web\3.5.15\sprin
g-boot-starter-web-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter\3.5.15
\spring-boot-starter-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot\3.5.15\sprin
g-boot-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-autoconfigure\3.5.15\sprin
g-boot-autoconfigure-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-logg
ing\3.5.15\spring-boot-starter-logging-3.5.15.jar;C:\Users\Administrator\.m2\repository\ch\qos\logback\logback-classic\
1.5.34\logback-classic-1.5.34.jar;C:\Users\Administrator\.m2\repository\ch\qos\logback\logback-core\1.5.34\logback-core
-1.5.34.jar;C:\Users\Administrator\.m2\repository\org\apache\logging\log4j\log4j-to-slf4j\2.24.3\log4j-to-slf4j-2.24.3.
jar;C:\Users\Administrator\.m2\repository\org\apache\logging\log4j\log4j-api\2.24.3\log4j-api-2.24.3.jar;C:\Users\Admin
istrator\.m2\repository\org\slf4j\jul-to-slf4j\2.0.18\jul-to-slf4j-2.0.18.jar;C:\Users\Administrator\.m2\repository\jak
arta\annotation\jakarta.annotation-api\2.1.1\jakarta.annotation-api-2.1.1.jar;C:\Users\Administrator\.m2\repository\org
\yaml\snakeyaml\2.4\snakeyaml-2.4.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starte
r-json\3.5.15\spring-boot-starter-json-3.5.15.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\datatype\
jackson-datatype-jdk8\2.21.4\jackson-datatype-jdk8-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jacks
on\module\jackson-module-parameter-names\2.21.4\jackson-module-parameter-names-2.21.4.jar;C:\Users\Administrator\.m2\re
pository\org\springframework\boot\spring-boot-starter-tomcat\3.5.15\spring-boot-starter-tomcat-3.5.15.jar;C:\Users\Admi
nistrator\.m2\repository\org\apache\tomcat\embed\tomcat-embed-core\10.1.55\tomcat-embed-core-10.1.55.jar;C:\Users\Admin
istrator\.m2\repository\org\apache\tomcat\embed\tomcat-embed-el\10.1.55\tomcat-embed-el-10.1.55.jar;C:\Users\Administra
tor\.m2\repository\org\apache\tomcat\embed\tomcat-embed-websocket\10.1.55\tomcat-embed-websocket-10.1.55.jar;C:\Users\A
dministrator\.m2\repository\org\springframework\spring-web\6.2.19\spring-web-6.2.19.jar;C:\Users\Administrator\.m2\repo
sitory\org\springframework\spring-beans\6.2.19\spring-beans-6.2.19.jar;C:\Users\Administrator\.m2\repository\io\microme
ter\micrometer-observation\1.15.12\micrometer-observation-1.15.12.jar;C:\Users\Administrator\.m2\repository\io\micromet
er\micrometer-commons\1.15.12\micrometer-commons-1.15.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\
spring-webmvc\6.2.19\spring-webmvc-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-aop\6.2.
19\spring-aop-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-context\6.2.19\spring-context
-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-expression\6.2.19\spring-expression-6.2.19
.jar;C:\Users\Administrator\.m2\repository\com\github\xiaoymin\knife4j-openapi3-jakarta-spring-boot-starter\4.5.0\knife
4j-openapi3-jakarta-spring-boot-starter-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\github\xiaoymin\knife4j-cor
e\4.5.0\knife4j-core-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\github\xiaoymin\knife4j-openapi3-ui\4.5.0\knif
e4j-openapi3-ui-4.5.0.jar;C:\Users\Administrator\.m2\repository\org\springdoc\springdoc-openapi-starter-webmvc-ui\2.8.1
7\springdoc-openapi-starter-webmvc-ui-2.8.17.jar;C:\Users\Administrator\.m2\repository\org\springdoc\springdoc-openapi-
starter-webmvc-api\2.8.17\springdoc-openapi-starter-webmvc-api-2.8.17.jar;C:\Users\Administrator\.m2\repository\org\spr
ingdoc\springdoc-openapi-starter-common\2.8.17\springdoc-openapi-starter-common-2.8.17.jar;C:\Users\Administrator\.m2\r
epository\org\springframework\boot\spring-boot-starter-validation\3.5.15\spring-boot-starter-validation-3.5.15.jar;C:\U
sers\Administrator\.m2\repository\org\hibernate\validator\hibernate-validator\8.0.3.Final\hibernate-validator-8.0.3.Fin
al.jar;C:\Users\Administrator\.m2\repository\org\jboss\logging\jboss-logging\3.6.3.Final\jboss-logging-3.6.3.Final.jar;
C:\Users\Administrator\.m2\repository\com\fasterxml\classmate\1.7.3\classmate-1.7.3.jar;C:\Users\Administrator\.m2\repo
sitory\io\swagger\core\v3\swagger-core-jakarta\2.2.47\swagger-core-jakarta-2.2.47.jar;C:\Users\Administrator\.m2\reposi
tory\io\swagger\core\v3\swagger-annotations-jakarta\2.2.47\swagger-annotations-jakarta-2.2.47.jar;C:\Users\Administrato
r\.m2\repository\io\swagger\core\v3\swagger-models-jakarta\2.2.47\swagger-models-jakarta-2.2.47.jar;C:\Users\Administra
tor\.m2\repository\com\fasterxml\jackson\dataformat\jackson-dataformat-yaml\2.21.4\jackson-dataformat-yaml-2.21.4.jar;C
:\Users\Administrator\.m2\repository\org\webjars\swagger-ui\5.32.2\swagger-ui-5.32.2.jar;C:\Users\Administrator\.m2\rep
ository\org\webjars\webjars-locator-lite\1.1.3\webjars-locator-lite-1.1.3.jar;C:\Users\Administrator\.m2\repository\org
\jspecify\jspecify\1.0.0\jspecify-1.0.0.jar;C:\Users\Administrator\.m2\repository\org\jsoup\jsoup\1.23.2\jsoup-1.23.2.j
ar;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-redis\target\classes;C:\Users\
Administrator\.m2\repository\org\redisson\redisson-spring-boot-starter\4.7.0\redisson-spring-boot-starter-4.7.0.jar;C:\
Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-data-redis\3.5.15\spring-boot-starter-d
ata-redis-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\redisson\redisson\4.7.0\redisson-4.7.0.jar;C:\Users\Admi
nistrator\.m2\repository\io\netty\netty-common\4.2.17.Final\netty-common-4.2.17.Final.jar;C:\Users\Administrator\.m2\re
pository\io\netty\netty-codec\4.2.17.Final\netty-codec-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\
netty-codec-base\4.2.17.Final\netty-codec-base-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-co
dec-compression\4.2.17.Final\netty-codec-compression-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\ne
tty-codec-protobuf\4.2.17.Final\netty-codec-protobuf-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\ne
tty-codec-marshalling\4.2.17.Final\netty-codec-marshalling-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\ne
tty\netty-buffer\4.2.17.Final\netty-buffer-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-transp
ort\4.2.17.Final\netty-transport-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-resolver\4.2.17.
Final\netty-resolver-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-resolver-dns\4.2.17.Final\ne
tty-resolver-dns-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-codec-dns\4.2.17.Final\netty-cod
ec-dns-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-handler\4.2.17.Final\netty-handler-4.2.17.
Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-transport-native-unix-common\4.2.17.Final\netty-transpor
t-native-unix-common-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\javax\cache\cache-api\1.1.1\cache-api-1.1.1
.jar;C:\Users\Administrator\.m2\repository\io\projectreactor\reactor-core\3.7.19\reactor-core-3.7.19.jar;C:\Users\Admin
istrator\.m2\repository\org\reactivestreams\reactive-streams\1.0.4\reactive-streams-1.0.4.jar;C:\Users\Administrator\.m
2\repository\io\reactivex\rxjava3\rxjava\3.1.12\rxjava-3.1.12.jar;C:\Users\Administrator\.m2\repository\com\esotericsof
tware\kryo\5.6.2\kryo-5.6.2.jar;C:\Users\Administrator\.m2\repository\com\esotericsoftware\reflectasm\1.11.9\reflectasm
-1.11.9.jar;C:\Users\Administrator\.m2\repository\com\esotericsoftware\minlog\1.3.1\minlog-1.3.1.jar;C:\Users\Administr
ator\.m2\repository\net\bytebuddy\byte-buddy\1.17.8\byte-buddy-1.17.8.jar;C:\Users\Administrator\.m2\repository\org\jod
d\jodd-util\6.3.0\jodd-util-6.3.0.jar;C:\Users\Administrator\.m2\repository\org\redisson\redisson-spring-data-35\4.7.0\
redisson-spring-data-35-4.7.0.jar;C:\Users\Administrator\.m2\repository\org\springframework\data\spring-data-redis\3.5.
12\spring-data-redis-3.5.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\data\spring-data-keyvalue\3.5
.12\spring-data-keyvalue-3.5.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\data\spring-data-commons\
3.5.12\spring-data-commons-3.5.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-tx\6.2.19\spring
-tx-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-oxm\6.2.19\spring-oxm-6.2.19.jar;C:\Use
rs\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-cache\3.5.15\spring-boot-starter-cache-3.5
.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-context-support\6.2.19\spring-context-support-
6.2.19.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\datatype\jackson-datatype-jsr310\2.21.4\jackson-
datatype-jsr310-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\core\jackson-annotations\2.21\ja
ckson-annotations-2.21.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\core\jackson-core\2.21.4\jackson
-core-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\core\jackson-databind\2.21.4\jackson-datab
ind-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\baomidou\lock4j-redisson-spring-boot-starter\2.2.7\lock4j-redi
sson-spring-boot-starter-2.2.7.jar;C:\Users\Administrator\.m2\repository\com\baomidou\lock4j-core\2.2.7\lock4j-core-2.2
.7.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-aop\3.5.15\spring-boot-starte
r-aop-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\aspectj\aspectjweaver\1.9.25.1\aspectjweaver-1.9.25.1.jar;E:
\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-test\target\classes;E:\����֮��AI���
�ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-mybatis\target\classes;C:\Users\Administrator\.m
2\repository\com\alibaba\druid-spring-boot-3-starter\1.2.28\druid-spring-boot-3-starter-1.2.28.jar;C:\Users\Administrat
or\.m2\repository\com\alibaba\druid\1.2.28\druid-1.2.28.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-
plus-spring-boot3-starter\3.5.17\mybatis-plus-spring-boot3-starter-3.5.17.jar;C:\Users\Administrator\.m2\repository\com
\baomidou\mybatis-plus\3.5.17\mybatis-plus-3.5.17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-c
ore\3.5.17\mybatis-plus-core-3.5.17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-annotation\3.5.
17\mybatis-plus-annotation-3.5.17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-spring\3.5.17\myb
atis-plus-spring-3.5.17.jar;C:\Users\Administrator\.m2\repository\org\mybatis\mybatis\3.5.19\mybatis-3.5.19.jar;C:\User
s\Administrator\.m2\repository\org\mybatis\mybatis-spring\3.0.5\mybatis-spring-3.0.5.jar;C:\Users\Administrator\.m2\rep
ository\com\baomidou\mybatis-plus-spring-boot-autoconfigure\3.5.17\mybatis-plus-spring-boot-autoconfigure-3.5.17.jar;C:
\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-spring-boot-native-image\3.5.17\mybatis-plus-spring-boot-
native-image-3.5.17.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-jdbc\3.5.15\
spring-boot-starter-jdbc-3.5.15.jar;C:\Users\Administrator\.m2\repository\com\zaxxer\HikariCP\6.3.3\HikariCP-6.3.3.jar;
C:\Users\Administrator\.m2\repository\org\springframework\spring-jdbc\6.2.19\spring-jdbc-6.2.19.jar;C:\Users\Administra
tor\.m2\repository\com\baomidou\mybatis-plus-jsqlparser\3.5.17\mybatis-plus-jsqlparser-3.5.17.jar;C:\Users\Administrato
r\.m2\repository\com\github\jsqlparser\jsqlparser\5.2\jsqlparser-5.2.jar;C:\Users\Administrator\.m2\repository\com\baom
idou\mybatis-plus-jsqlparser-common\3.5.17\mybatis-plus-jsqlparser-common-3.5.17.jar;C:\Users\Administrator\.m2\reposit
ory\com\baomidou\mybatis-plus-extension\3.5.17\mybatis-plus-extension-3.5.17.jar;C:\Users\Administrator\.m2\repository\
com\baomidou\dynamic-datasource-spring-boot3-starter\4.5.0\dynamic-datasource-spring-boot3-starter-4.5.0.jar;C:\Users\A
dministrator\.m2\repository\com\baomidou\dynamic-datasource-spring-boot-common\4.5.0\dynamic-datasource-spring-boot-com
mon-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\baomidou\dynamic-datasource-spring\4.5.0\dynamic-datasource-spr
ing-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\baomidou\dynamic-datasource-creator\4.5.0\dynamic-datasource-cr
eator-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\oracle\database\spring\oracle-spring-boot-starter-ucp\23.4.0\
oracle-spring-boot-starter-ucp-23.4.0.jar;C:\Users\Administrator\.m2\repository\com\oracle\database\jdbc\ojdbc11\23.7.0
.25.01\ojdbc11-23.7.0.25.01.jar;C:\Users\Administrator\.m2\repository\com\oracle\database\jdbc\ucp\23.7.0.25.01\ucp-23.
7.0.25.01.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-join-boot-starter\1.5.9\mybatis-p
lus-join-boot-starter-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-join-extension\
1.5.9\mybatis-plus-join-extension-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-joi
n-core\1.5.9\mybatis-plus-join-core-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-j
oin-annotation\1.5.9\mybatis-plus-join-annotation-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\
mybatis-plus-join-adapter-base\1.5.9\mybatis-plus-join-adapter-base-1.5.9.jar;C:\Users\Administrator\.m2\repository\com
\github\yulichang\mybatis-plus-join-adapter-jsqlparser\1.5.9\mybatis-plus-join-adapter-jsqlparser-1.5.9.jar;C:\Users\Ad
ministrator\.m2\repository\com\github\yulichang\mybatis-plus-join-wrapper-ext\1.5.9\mybatis-plus-join-wrapper-ext-1.5.9
.jar;C:\Users\Administrator\.m2\repository\org\dromara\easy-trans-spring-boot-starter\3.1.8\easy-trans-spring-boot-star
ter-3.1.8.jar;C:\Users\Administrator\.m2\repository\org\dromara\easy-trans-service\3.1.8\easy-trans-service-3.1.8.jar;C
:\Users\Administrator\.m2\repository\com\github\ben-manes\caffeine\caffeine\3.2.4\caffeine-3.2.4.jar;C:\Users\Administr
ator\.m2\repository\org\dromara\easy-trans-mybatis-plus-extend\3.1.8\easy-trans-mybatis-plus-extend-3.1.8.jar;C:\Users\
Administrator\.m2\repository\org\mockito\mockito-inline\5.2.0\mockito-inline-5.2.0.jar;C:\Users\Administrator\.m2\repos
itory\org\mockito\mockito-core\5.17.0\mockito-core-5.17.0.jar;C:\Users\Administrator\.m2\repository\net\bytebuddy\byte-
buddy-agent\1.17.8\byte-buddy-agent-1.17.8.jar;C:\Users\Administrator\.m2\repository\org\objenesis\objenesis\3.3\objene
sis-3.3.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-test\3.5.15\spring-boot-
starter-test-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-test\3.5.15\spring-b
oot-test-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-test-autoconfigure\3.5.1
5\spring-boot-test-autoconfigure-3.5.15.jar;C:\Users\Administrator\.m2\repository\com\jayway\jsonpath\json-path\2.9.0\j
son-path-2.9.0.jar;C:\Users\Administrator\.m2\repository\jakarta\xml\bind\jakarta.xml.bind-api\4.0.5\jakarta.xml.bind-a
pi-4.0.5.jar;C:\Users\Administrator\.m2\repository\jakarta\activation\jakarta.activation-api\2.1.4\jakarta.activation-a
pi-2.1.4.jar;C:\Users\Administrator\.m2\repository\net\minidev\json-smart\2.5.2\json-smart-2.5.2.jar;C:\Users\Administr
ator\.m2\repository\net\minidev\accessors-smart\2.5.2\accessors-smart-2.5.2.jar;C:\Users\Administrator\.m2\repository\o
rg\assertj\assertj-core\3.27.7\assertj-core-3.27.7.jar;C:\Users\Administrator\.m2\repository\org\awaitility\awaitility\
4.2.2\awaitility-4.2.2.jar;C:\Users\Administrator\.m2\repository\org\hamcrest\hamcrest\3.0\hamcrest-3.0.jar;C:\Users\Ad
ministrator\.m2\repository\org\junit\jupiter\junit-jupiter\5.12.2\junit-jupiter-5.12.2.jar;C:\Users\Administrator\.m2\r
epository\org\junit\jupiter\junit-jupiter-api\5.12.2\junit-jupiter-api-5.12.2.jar;C:\Users\Administrator\.m2\repository
\org\opentest4j\opentest4j\1.3.0\opentest4j-1.3.0.jar;C:\Users\Administrator\.m2\repository\org\junit\platform\junit-pl
atform-commons\1.12.2\junit-platform-commons-1.12.2.jar;C:\Users\Administrator\.m2\repository\org\apiguardian\apiguardi
an-api\1.1.2\apiguardian-api-1.1.2.jar;C:\Users\Administrator\.m2\repository\org\junit\jupiter\junit-jupiter-params\5.1
2.2\junit-jupiter-params-5.12.2.jar;C:\Users\Administrator\.m2\repository\org\junit\jupiter\junit-jupiter-engine\5.12.2
\junit-jupiter-engine-5.12.2.jar;C:\Users\Administrator\.m2\repository\org\junit\platform\junit-platform-engine\1.12.2\
junit-platform-engine-1.12.2.jar;C:\Users\Administrator\.m2\repository\org\mockito\mockito-junit-jupiter\5.17.0\mockito
-junit-jupiter-5.17.0.jar;C:\Users\Administrator\.m2\repository\org\skyscreamer\jsonassert\1.5.3\jsonassert-1.5.3.jar;C
:\Users\Administrator\.m2\repository\com\vaadin\external\google\android-json\0.0.20131108.vaadin1\android-json-0.0.2013
1108.vaadin1.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-core\6.2.19\spring-core-6.2.19.jar;C:
\Users\Administrator\.m2\repository\org\springframework\spring-jcl\6.2.19\spring-jcl-6.2.19.jar;C:\Users\Administrator\
.m2\repository\org\springframework\spring-test\6.2.19\spring-test-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\
xmlunit\xmlunit-core\2.10.4\xmlunit-core-2.10.4.jar;C:\Users\Administrator\.m2\repository\com\h2database\h2\2.3.232\h2-
2.3.232.jar;C:\Users\Administrator\.m2\repository\com\github\fppt\jedis-mock\1.1.18\jedis-mock-1.1.18.jar;C:\Users\Admi
nistrator\.m2\repository\org\slf4j\slf4j-api\2.0.18\slf4j-api-2.0.18.jar;C:\Users\Administrator\.m2\repository\org\luaj
\luaj-jse\3.0.1\luaj-jse-3.0.1.jar;C:\Users\Administrator\.m2\repository\redis\clients\jedis\6.0.0\jedis-6.0.0.jar;C:\U
sers\Administrator\.m2\repository\org\apache\commons\commons-pool2\2.12.1\commons-pool2-2.12.1.jar;C:\Users\Administrat
or\.m2\repository\org\json\json\20250107\json-20250107.jar;C:\Users\Administrator\.m2\repository\com\google\code\gson\g
son\2.13.2\gson-2.13.2.jar;C:\Users\Administrator\.m2\repository\com\google\errorprone\error_prone_annotations\2.41.0\e
rror_prone_annotations-2.41.0.jar;C:\Users\Administrator\.m2\repository\redis\clients\authentication\redis-authx-core\0
.1.1-beta2\redis-authx-core-0.1.1-beta2.jar;C:\Users\Administrator\.m2\repository\uk\co\jemos\podam\podam\8.0.2.RELEASE
\podam-8.0.2.RELEASE.jar;C:\Users\Administrator\.m2\repository\net\jcip\jcip-annotations\1.0\jcip-annotations-1.0.jar;C
:\Users\Administrator\.m2\repository\jakarta\validation\jakarta.validation-api\3.0.2\jakarta.validation-api-3.0.2.jar;C
:\Users\Administrator\.m2\repository\org\apache\commons\commons-lang3\3.20.0\commons-lang3-3.20.0.jar;"/>
    <property name="java.vm.vendor" value="Eclipse Adoptium"/>
    <property name="sun.arch.data.model" value="64"/>
    <property name="user.variant" value=""/>
    <property name="java.vendor.url" value="https://adoptium.net/"/>
    <property name="user.timezone" value="Asia/Shanghai"/>
    <property name="os.name" value="Windows 10"/>
    <property name="java.vm.specification.version" value="17"/>
    <property name="sun.java.launcher" value="SUN_STANDARD"/>
    <property name="user.country" value="CN"/>
    <property name="sun.boot.library.path" value="E:\����֮��AI����ƽ̨����\tools\jdk-17.0.20.1+1\bin"/>
    <property name="sun.java.command" value="C:\Users\Administrator\AppData\Local\Temp\surefire12053511136342888388\sur
efirebooter-20260912135129227_23.jar C:\Users\Administrator\AppData\Local\Temp\surefire12053511136342888388 2026-09-12T
13-51-22_032-jvmRun1 surefire-20260912135129227_21tmp surefire_4-20260912135129227_22tmp"/>
    <property name="jdk.debug" value="release"/>
    <property name="surefire.test.class.path" value="E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-
spring-boot-starter-protection\target\test-classes;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-sp
ring-boot-starter-protection\target\classes;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-bo
ot-starter-web\target\classes;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-common\target\classes;C
:\Users\Administrator\.m2\repository\io\opentelemetry\opentelemetry-api\1.65.0\opentelemetry-api-1.65.0.jar;C:\Users\Ad
ministrator\.m2\repository\io\opentelemetry\opentelemetry-context\1.49.0\opentelemetry-context-1.49.0.jar;C:\Users\Admi
nistrator\.m2\repository\org\projectlombok\lombok\1.18.46\lombok-1.18.46.jar;C:\Users\Administrator\.m2\repository\org\
mapstruct\mapstruct\1.6.3\mapstruct-1.6.3.jar;C:\Users\Administrator\.m2\repository\org\mapstruct\mapstruct-jdk8\1.6.3\
mapstruct-jdk8-1.6.3.jar;C:\Users\Administrator\.m2\repository\org\mapstruct\mapstruct-processor\1.6.3\mapstruct-proces
sor-1.6.3.jar;C:\Users\Administrator\.m2\repository\cn\hutool\hutool-all\5.8.47\hutool-all-5.8.47.jar;C:\Users\Administ
rator\.m2\repository\com\alibaba\transmittable-thread-local\2.14.5\transmittable-thread-local-2.14.5.jar;C:\Users\Admin
istrator\.m2\repository\com\alibaba\fastjson\2.0.64\fastjson-2.0.64.jar;C:\Users\Administrator\.m2\repository\com\aliba
ba\fastjson2\fastjson2-extension\2.0.64\fastjson2-extension-2.0.64.jar;C:\Users\Administrator\.m2\repository\com\alibab
a\fastjson2\fastjson2\2.0.64\fastjson2-2.0.64.jar;C:\Users\Administrator\.m2\repository\org\dromara\easy-trans-anno\3.1
.8\easy-trans-anno-3.1.8.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-web\3.5
.15\spring-boot-starter-web-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-start
er\3.5.15\spring-boot-starter-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot\3.5
.15\spring-boot-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-autoconfigure\3.5
.15\spring-boot-autoconfigure-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-sta
rter-logging\3.5.15\spring-boot-starter-logging-3.5.15.jar;C:\Users\Administrator\.m2\repository\ch\qos\logback\logback
-classic\1.5.34\logback-classic-1.5.34.jar;C:\Users\Administrator\.m2\repository\ch\qos\logback\logback-core\1.5.34\log
back-core-1.5.34.jar;C:\Users\Administrator\.m2\repository\org\apache\logging\log4j\log4j-to-slf4j\2.24.3\log4j-to-slf4
j-2.24.3.jar;C:\Users\Administrator\.m2\repository\org\apache\logging\log4j\log4j-api\2.24.3\log4j-api-2.24.3.jar;C:\Us
ers\Administrator\.m2\repository\org\slf4j\jul-to-slf4j\2.0.18\jul-to-slf4j-2.0.18.jar;C:\Users\Administrator\.m2\repos
itory\jakarta\annotation\jakarta.annotation-api\2.1.1\jakarta.annotation-api-2.1.1.jar;C:\Users\Administrator\.m2\repos
itory\org\yaml\snakeyaml\2.4\snakeyaml-2.4.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-bo
ot-starter-json\3.5.15\spring-boot-starter-json-3.5.15.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\
datatype\jackson-datatype-jdk8\2.21.4\jackson-datatype-jdk8-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\faster
xml\jackson\module\jackson-module-parameter-names\2.21.4\jackson-module-parameter-names-2.21.4.jar;C:\Users\Administrat
or\.m2\repository\org\springframework\boot\spring-boot-starter-tomcat\3.5.15\spring-boot-starter-tomcat-3.5.15.jar;C:\U
sers\Administrator\.m2\repository\org\apache\tomcat\embed\tomcat-embed-core\10.1.55\tomcat-embed-core-10.1.55.jar;C:\Us
ers\Administrator\.m2\repository\org\apache\tomcat\embed\tomcat-embed-el\10.1.55\tomcat-embed-el-10.1.55.jar;C:\Users\A
dministrator\.m2\repository\org\apache\tomcat\embed\tomcat-embed-websocket\10.1.55\tomcat-embed-websocket-10.1.55.jar;C
:\Users\Administrator\.m2\repository\org\springframework\spring-web\6.2.19\spring-web-6.2.19.jar;C:\Users\Administrator
\.m2\repository\org\springframework\spring-beans\6.2.19\spring-beans-6.2.19.jar;C:\Users\Administrator\.m2\repository\i
o\micrometer\micrometer-observation\1.15.12\micrometer-observation-1.15.12.jar;C:\Users\Administrator\.m2\repository\io
\micrometer\micrometer-commons\1.15.12\micrometer-commons-1.15.12.jar;C:\Users\Administrator\.m2\repository\org\springf
ramework\spring-webmvc\6.2.19\spring-webmvc-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring
-aop\6.2.19\spring-aop-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-context\6.2.19\sprin
g-context-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-expression\6.2.19\spring-expressi
on-6.2.19.jar;C:\Users\Administrator\.m2\repository\com\github\xiaoymin\knife4j-openapi3-jakarta-spring-boot-starter\4.
5.0\knife4j-openapi3-jakarta-spring-boot-starter-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\github\xiaoymin\kn
ife4j-core\4.5.0\knife4j-core-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\github\xiaoymin\knife4j-openapi3-ui\4
.5.0\knife4j-openapi3-ui-4.5.0.jar;C:\Users\Administrator\.m2\repository\org\springdoc\springdoc-openapi-starter-webmvc
-ui\2.8.17\springdoc-openapi-starter-webmvc-ui-2.8.17.jar;C:\Users\Administrator\.m2\repository\org\springdoc\springdoc
-openapi-starter-webmvc-api\2.8.17\springdoc-openapi-starter-webmvc-api-2.8.17.jar;C:\Users\Administrator\.m2\repositor
y\org\springdoc\springdoc-openapi-starter-common\2.8.17\springdoc-openapi-starter-common-2.8.17.jar;C:\Users\Administra
tor\.m2\repository\org\springframework\boot\spring-boot-starter-validation\3.5.15\spring-boot-starter-validation-3.5.15
.jar;C:\Users\Administrator\.m2\repository\org\hibernate\validator\hibernate-validator\8.0.3.Final\hibernate-validator-
8.0.3.Final.jar;C:\Users\Administrator\.m2\repository\org\jboss\logging\jboss-logging\3.6.3.Final\jboss-logging-3.6.3.F
inal.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\classmate\1.7.3\classmate-1.7.3.jar;C:\Users\Administrator
\.m2\repository\io\swagger\core\v3\swagger-core-jakarta\2.2.47\swagger-core-jakarta-2.2.47.jar;C:\Users\Administrator\.
m2\repository\io\swagger\core\v3\swagger-annotations-jakarta\2.2.47\swagger-annotations-jakarta-2.2.47.jar;C:\Users\Adm
inistrator\.m2\repository\io\swagger\core\v3\swagger-models-jakarta\2.2.47\swagger-models-jakarta-2.2.47.jar;C:\Users\A
dministrator\.m2\repository\com\fasterxml\jackson\dataformat\jackson-dataformat-yaml\2.21.4\jackson-dataformat-yaml-2.2
1.4.jar;C:\Users\Administrator\.m2\repository\org\webjars\swagger-ui\5.32.2\swagger-ui-5.32.2.jar;C:\Users\Administrato
r\.m2\repository\org\webjars\webjars-locator-lite\1.1.3\webjars-locator-lite-1.1.3.jar;C:\Users\Administrator\.m2\repos
itory\org\jspecify\jspecify\1.0.0\jspecify-1.0.0.jar;C:\Users\Administrator\.m2\repository\org\jsoup\jsoup\1.23.2\jsoup
-1.23.2.jar;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-redis\target\classes;
C:\Users\Administrator\.m2\repository\org\redisson\redisson-spring-boot-starter\4.7.0\redisson-spring-boot-starter-4.7.
0.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-data-redis\3.5.15\spring-boot-
starter-data-redis-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\redisson\redisson\4.7.0\redisson-4.7.0.jar;C:\U
sers\Administrator\.m2\repository\io\netty\netty-common\4.2.17.Final\netty-common-4.2.17.Final.jar;C:\Users\Administrat
or\.m2\repository\io\netty\netty-codec\4.2.17.Final\netty-codec-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\
io\netty\netty-codec-base\4.2.17.Final\netty-codec-base-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty
\netty-codec-compression\4.2.17.Final\netty-codec-compression-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io
\netty\netty-codec-protobuf\4.2.17.Final\netty-codec-protobuf-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io
\netty\netty-codec-marshalling\4.2.17.Final\netty-codec-marshalling-4.2.17.Final.jar;C:\Users\Administrator\.m2\reposit
ory\io\netty\netty-buffer\4.2.17.Final\netty-buffer-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\net
ty-transport\4.2.17.Final\netty-transport-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-resolve
r\4.2.17.Final\netty-resolver-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-resolver-dns\4.2.17
.Final\netty-resolver-dns-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-codec-dns\4.2.17.Final\
netty-codec-dns-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-handler\4.2.17.Final\netty-handle
r-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\io\netty\netty-transport-native-unix-common\4.2.17.Final\netty
-transport-native-unix-common-4.2.17.Final.jar;C:\Users\Administrator\.m2\repository\javax\cache\cache-api\1.1.1\cache-
api-1.1.1.jar;C:\Users\Administrator\.m2\repository\io\projectreactor\reactor-core\3.7.19\reactor-core-3.7.19.jar;C:\Us
ers\Administrator\.m2\repository\org\reactivestreams\reactive-streams\1.0.4\reactive-streams-1.0.4.jar;C:\Users\Adminis
trator\.m2\repository\io\reactivex\rxjava3\rxjava\3.1.12\rxjava-3.1.12.jar;C:\Users\Administrator\.m2\repository\com\es
otericsoftware\kryo\5.6.2\kryo-5.6.2.jar;C:\Users\Administrator\.m2\repository\com\esotericsoftware\reflectasm\1.11.9\r
eflectasm-1.11.9.jar;C:\Users\Administrator\.m2\repository\com\esotericsoftware\minlog\1.3.1\minlog-1.3.1.jar;C:\Users\
Administrator\.m2\repository\net\bytebuddy\byte-buddy\1.17.8\byte-buddy-1.17.8.jar;C:\Users\Administrator\.m2\repositor
y\org\jodd\jodd-util\6.3.0\jodd-util-6.3.0.jar;C:\Users\Administrator\.m2\repository\org\redisson\redisson-spring-data-
35\4.7.0\redisson-spring-data-35-4.7.0.jar;C:\Users\Administrator\.m2\repository\org\springframework\data\spring-data-r
edis\3.5.12\spring-data-redis-3.5.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\data\spring-data-key
value\3.5.12\spring-data-keyvalue-3.5.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\data\spring-data
-commons\3.5.12\spring-data-commons-3.5.12.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-tx\6.2.
19\spring-tx-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-oxm\6.2.19\spring-oxm-6.2.19.j
ar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-cache\3.5.15\spring-boot-starter-
cache-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-context-support\6.2.19\spring-context
-support-6.2.19.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\datatype\jackson-datatype-jsr310\2.21.4
\jackson-datatype-jsr310-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\core\jackson-annotation
s\2.21\jackson-annotations-2.21.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\core\jackson-core\2.21.
4\jackson-core-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\core\jackson-databind\2.21.4\jack
son-databind-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\baomidou\lock4j-redisson-spring-boot-starter\2.2.7\lo
ck4j-redisson-spring-boot-starter-2.2.7.jar;C:\Users\Administrator\.m2\repository\com\baomidou\lock4j-core\2.2.7\lock4j
-core-2.2.7.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-aop\3.5.15\spring-bo
ot-starter-aop-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\aspectj\aspectjweaver\1.9.25.1\aspectjweaver-1.9.25
.1.jar;E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-test\target\classes;E:\���
�֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-mybatis\target\classes;C:\Users\Adminis
trator\.m2\repository\com\alibaba\druid-spring-boot-3-starter\1.2.28\druid-spring-boot-3-starter-1.2.28.jar;C:\Users\Ad
ministrator\.m2\repository\com\alibaba\druid\1.2.28\druid-1.2.28.jar;C:\Users\Administrator\.m2\repository\com\baomidou
\mybatis-plus-spring-boot3-starter\3.5.17\mybatis-plus-spring-boot3-starter-3.5.17.jar;C:\Users\Administrator\.m2\repos
itory\com\baomidou\mybatis-plus\3.5.17\mybatis-plus-3.5.17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybat
is-plus-core\3.5.17\mybatis-plus-core-3.5.17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-annota
tion\3.5.17\mybatis-plus-annotation-3.5.17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-spring\3
.5.17\mybatis-plus-spring-3.5.17.jar;C:\Users\Administrator\.m2\repository\org\mybatis\mybatis\3.5.19\mybatis-3.5.19.ja
r;C:\Users\Administrator\.m2\repository\org\mybatis\mybatis-spring\3.0.5\mybatis-spring-3.0.5.jar;C:\Users\Administrato
r\.m2\repository\com\baomidou\mybatis-plus-spring-boot-autoconfigure\3.5.17\mybatis-plus-spring-boot-autoconfigure-3.5.
17.jar;C:\Users\Administrator\.m2\repository\com\baomidou\mybatis-plus-spring-boot-native-image\3.5.17\mybatis-plus-spr
ing-boot-native-image-3.5.17.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-jdb
c\3.5.15\spring-boot-starter-jdbc-3.5.15.jar;C:\Users\Administrator\.m2\repository\com\zaxxer\HikariCP\6.3.3\HikariCP-6
.3.3.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-jdbc\6.2.19\spring-jdbc-6.2.19.jar;C:\Users\A
dministrator\.m2\repository\com\baomidou\mybatis-plus-jsqlparser\3.5.17\mybatis-plus-jsqlparser-3.5.17.jar;C:\Users\Adm
inistrator\.m2\repository\com\github\jsqlparser\jsqlparser\5.2\jsqlparser-5.2.jar;C:\Users\Administrator\.m2\repository
\com\baomidou\mybatis-plus-jsqlparser-common\3.5.17\mybatis-plus-jsqlparser-common-3.5.17.jar;C:\Users\Administrator\.m
2\repository\com\baomidou\mybatis-plus-extension\3.5.17\mybatis-plus-extension-3.5.17.jar;C:\Users\Administrator\.m2\re
pository\com\baomidou\dynamic-datasource-spring-boot3-starter\4.5.0\dynamic-datasource-spring-boot3-starter-4.5.0.jar;C
:\Users\Administrator\.m2\repository\com\baomidou\dynamic-datasource-spring-boot-common\4.5.0\dynamic-datasource-spring
-boot-common-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\baomidou\dynamic-datasource-spring\4.5.0\dynamic-datas
ource-spring-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\baomidou\dynamic-datasource-creator\4.5.0\dynamic-data
source-creator-4.5.0.jar;C:\Users\Administrator\.m2\repository\com\oracle\database\spring\oracle-spring-boot-starter-uc
p\23.4.0\oracle-spring-boot-starter-ucp-23.4.0.jar;C:\Users\Administrator\.m2\repository\com\oracle\database\jdbc\ojdbc
11\23.7.0.25.01\ojdbc11-23.7.0.25.01.jar;C:\Users\Administrator\.m2\repository\com\oracle\database\jdbc\ucp\23.7.0.25.0
1\ucp-23.7.0.25.01.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-join-boot-starter\1.5.9\
mybatis-plus-join-boot-starter-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-join-e
xtension\1.5.9\mybatis-plus-join-extension-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybatis
-plus-join-core\1.5.9\mybatis-plus-join-core-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\yulichang\mybat
is-plus-join-annotation\1.5.9\mybatis-plus-join-annotation-1.5.9.jar;C:\Users\Administrator\.m2\repository\com\github\y
ulichang\mybatis-plus-join-adapter-base\1.5.9\mybatis-plus-join-adapter-base-1.5.9.jar;C:\Users\Administrator\.m2\repos
itory\com\github\yulichang\mybatis-plus-join-adapter-jsqlparser\1.5.9\mybatis-plus-join-adapter-jsqlparser-1.5.9.jar;C:
\Users\Administrator\.m2\repository\com\github\yulichang\mybatis-plus-join-wrapper-ext\1.5.9\mybatis-plus-join-wrapper-
ext-1.5.9.jar;C:\Users\Administrator\.m2\repository\org\dromara\easy-trans-spring-boot-starter\3.1.8\easy-trans-spring-
boot-starter-3.1.8.jar;C:\Users\Administrator\.m2\repository\org\dromara\easy-trans-service\3.1.8\easy-trans-service-3.
1.8.jar;C:\Users\Administrator\.m2\repository\com\github\ben-manes\caffeine\caffeine\3.2.4\caffeine-3.2.4.jar;C:\Users\
Administrator\.m2\repository\org\dromara\easy-trans-mybatis-plus-extend\3.1.8\easy-trans-mybatis-plus-extend-3.1.8.jar;
C:\Users\Administrator\.m2\repository\org\mockito\mockito-inline\5.2.0\mockito-inline-5.2.0.jar;C:\Users\Administrator\
.m2\repository\org\mockito\mockito-core\5.17.0\mockito-core-5.17.0.jar;C:\Users\Administrator\.m2\repository\net\bytebu
ddy\byte-buddy-agent\1.17.8\byte-buddy-agent-1.17.8.jar;C:\Users\Administrator\.m2\repository\org\objenesis\objenesis\3
.3\objenesis-3.3.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-starter-test\3.5.15\spr
ing-boot-starter-test-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-test\3.5.15
\spring-boot-test-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-test-autoconfig
ure\3.5.15\spring-boot-test-autoconfigure-3.5.15.jar;C:\Users\Administrator\.m2\repository\com\jayway\jsonpath\json-pat
h\2.9.0\json-path-2.9.0.jar;C:\Users\Administrator\.m2\repository\jakarta\xml\bind\jakarta.xml.bind-api\4.0.5\jakarta.x
ml.bind-api-4.0.5.jar;C:\Users\Administrator\.m2\repository\jakarta\activation\jakarta.activation-api\2.1.4\jakarta.act
ivation-api-2.1.4.jar;C:\Users\Administrator\.m2\repository\net\minidev\json-smart\2.5.2\json-smart-2.5.2.jar;C:\Users\
Administrator\.m2\repository\net\minidev\accessors-smart\2.5.2\accessors-smart-2.5.2.jar;C:\Users\Administrator\.m2\rep
ository\org\assertj\assertj-core\3.27.7\assertj-core-3.27.7.jar;C:\Users\Administrator\.m2\repository\org\awaitility\aw
aitility\4.2.2\awaitility-4.2.2.jar;C:\Users\Administrator\.m2\repository\org\hamcrest\hamcrest\3.0\hamcrest-3.0.jar;C:
\Users\Administrator\.m2\repository\org\junit\jupiter\junit-jupiter\5.12.2\junit-jupiter-5.12.2.jar;C:\Users\Administra
tor\.m2\repository\org\junit\jupiter\junit-jupiter-api\5.12.2\junit-jupiter-api-5.12.2.jar;C:\Users\Administrator\.m2\r
epository\org\opentest4j\opentest4j\1.3.0\opentest4j-1.3.0.jar;C:\Users\Administrator\.m2\repository\org\junit\platform
\junit-platform-commons\1.12.2\junit-platform-commons-1.12.2.jar;C:\Users\Administrator\.m2\repository\org\apiguardian\
apiguardian-api\1.1.2\apiguardian-api-1.1.2.jar;C:\Users\Administrator\.m2\repository\org\junit\jupiter\junit-jupiter-p
arams\5.12.2\junit-jupiter-params-5.12.2.jar;C:\Users\Administrator\.m2\repository\org\junit\jupiter\junit-jupiter-engi
ne\5.12.2\junit-jupiter-engine-5.12.2.jar;C:\Users\Administrator\.m2\repository\org\junit\platform\junit-platform-engin
e\1.12.2\junit-platform-engine-1.12.2.jar;C:\Users\Administrator\.m2\repository\org\mockito\mockito-junit-jupiter\5.17.
0\mockito-junit-jupiter-5.17.0.jar;C:\Users\Administrator\.m2\repository\org\skyscreamer\jsonassert\1.5.3\jsonassert-1.
5.3.jar;C:\Users\Administrator\.m2\repository\com\vaadin\external\google\android-json\0.0.20131108.vaadin1\android-json
-0.0.20131108.vaadin1.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-core\6.2.19\spring-core-6.2.
19.jar;C:\Users\Administrator\.m2\repository\org\springframework\spring-jcl\6.2.19\spring-jcl-6.2.19.jar;C:\Users\Admin
istrator\.m2\repository\org\springframework\spring-test\6.2.19\spring-test-6.2.19.jar;C:\Users\Administrator\.m2\reposi
tory\org\xmlunit\xmlunit-core\2.10.4\xmlunit-core-2.10.4.jar;C:\Users\Administrator\.m2\repository\com\h2database\h2\2.
3.232\h2-2.3.232.jar;C:\Users\Administrator\.m2\repository\com\github\fppt\jedis-mock\1.1.18\jedis-mock-1.1.18.jar;C:\U
sers\Administrator\.m2\repository\org\slf4j\slf4j-api\2.0.18\slf4j-api-2.0.18.jar;C:\Users\Administrator\.m2\repository
\org\luaj\luaj-jse\3.0.1\luaj-jse-3.0.1.jar;C:\Users\Administrator\.m2\repository\redis\clients\jedis\6.0.0\jedis-6.0.0
.jar;C:\Users\Administrator\.m2\repository\org\apache\commons\commons-pool2\2.12.1\commons-pool2-2.12.1.jar;C:\Users\Ad
ministrator\.m2\repository\org\json\json\20250107\json-20250107.jar;C:\Users\Administrator\.m2\repository\com\google\co
de\gson\gson\2.13.2\gson-2.13.2.jar;C:\Users\Administrator\.m2\repository\com\google\errorprone\error_prone_annotations
\2.41.0\error_prone_annotations-2.41.0.jar;C:\Users\Administrator\.m2\repository\redis\clients\authentication\redis-aut
hx-core\0.1.1-beta2\redis-authx-core-0.1.1-beta2.jar;C:\Users\Administrator\.m2\repository\uk\co\jemos\podam\podam\8.0.
2.RELEASE\podam-8.0.2.RELEASE.jar;C:\Users\Administrator\.m2\repository\net\jcip\jcip-annotations\1.0\jcip-annotations-
1.0.jar;C:\Users\Administrator\.m2\repository\jakarta\validation\jakarta.validation-api\3.0.2\jakarta.validation-api-3.
0.2.jar;C:\Users\Administrator\.m2\repository\org\apache\commons\commons-lang3\3.20.0\commons-lang3-3.20.0.jar;"/>
    <property name="sun.cpu.endian" value="little"/>
    <property name="user.home" value="C:\Users\Administrator"/>
    <property name="user.language" value="zh"/>
    <property name="java.specification.vendor" value="Oracle Corporation"/>
    <property name="java.version.date" value="2026-08-18"/>
    <property name="java.home" value="E:\����֮��AI����ƽ̨����\tools\jdk-17.0.20.1+1"/>
    <property name="file.separator" value="\"/>
    <property name="basedir" value="E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-start
er-protection"/>
    <property name="java.vm.compressedOopsMode" value="Zero based"/>
    <property name="line.separator" value="&#10;"/>
    <property name="java.vm.specification.vendor" value="Oracle Corporation"/>
    <property name="java.specification.name" value="Java Platform API Specification"/>
    <property name="surefire.real.class.path" value="C:\Users\Administrator\AppData\Local\Temp\surefire1205351113634288
8388\surefirebooter-20260912135129227_23.jar"/>
    <property name="user.script" value=""/>
    <property name="sun.management.compiler" value="HotSpot 64-Bit Tiered Compilers"/>
    <property name="java.runtime.version" value="17.0.20.1+1"/>
    <property name="user.name" value="Administrator"/>
    <property name="path.separator" value=";"/>
    <property name="os.version" value="10.0"/>
    <property name="java.runtime.name" value="OpenJDK Runtime Environment"/>
    <property name="file.encoding" value="GBK"/>
    <property name="java.vm.name" value="OpenJDK 64-Bit Server VM"/>
    <property name="java.vendor.version" value="Temurin-17.0.20.1+1"/>
    <property name="localRepository" value="C:\Users\Administrator\.m2\repository"/>
    <property name="java.vendor.url.bug" value="https://github.com/adoptium/adoptium-support/issues"/>
    <property name="java.io.tmpdir" value="C:\Users\ADMINI~1\AppData\Local\Temp\"/>
    <property name="java.version" value="17.0.20.1"/>
    <property name="user.dir" value="E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-star
ter-protection"/>
    <property name="os.arch" value="amd64"/>
    <property name="java.vm.specification.name" value="Java Virtual Machine Specification"/>
    <property name="sun.os.patch.level" value=""/>
    <property name="native.encoding" value="GBK"/>
    <property name="java.library.path" value="E:\����֮��AI����ƽ̨����\tools\jdk-17.0.20.1+1\bin;C:\windows\Sun\Java\bin;
C:\windows\system32;C:\windows;E:\����֮��AI����ƽ̨����\tools\jdk-17.0.20.1+1\bin;E:\����֮��AI����ƽ̨����\tools\apache-mav
en-3.9.9\bin;C:\Program Files\Python311\Scripts\;C:\Program Files\Python311\;C:\windows\system32;C:\windows;C:\windows\
System32\Wbem;C:\windows\System32\WindowsPowerShell\v1.0\;C:\windows\System32\OpenSSH\;D:\Program Files (x86)\Tencent\΢
��web�����߹���;;C:\Program Files\Git\cmd;C:\Program Files\nodejs\;C:\Users\Administrator\AppData\Local\Microsoft\Window
sApps;;C:\Users\Administrator\AppData\Local\Programs\DockerDesktop\resources\bin;C:\Users\Administrator\AppData\Roaming
\npm;D:\Program Files\Qoder IDE\bin;C:\Users\Administrator\AppData\Local\Microsoft\WinGet\Links;;."/>
    <property name="java.vm.info" value="mixed mode, sharing"/>
    <property name="java.vendor" value="Eclipse Adoptium"/>
    <property name="java.vm.version" value="17.0.20.1+1"/>
    <property name="java.specification.maintenance.version" value="1"/>
    <property name="sun.io.unicode.encoding" value="UnicodeLittle"/>
    <property name="java.class.version" value="61.0"/>
  </properties>
  <testcase name="testSignatureGet" classname="cn.zszj.framework.signature.core.ApiSignatureTest" time="0.043"/>
</testsuite>
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\test-classes\cn
\zszj\framework\idempotent\core\aop\IdempotentAspectTest$FixedKeyResolver.class
????   = 
      java/lang/Object <init> ()V  idem-fixed-key 
 Kcn/zszj/framework/idempotent/core/aop/IdempotentAspectTest$FixedKeyResolver  Ccn/zszj/framework/idempotent/core/k
eyresolver/IdempotentKeyResolver Code LineNumberTable resolver i(Lorg/aspectj/lang/JoinPoint;Lcn/zszj/framework/
idempotent/core/annotation/Idempotent;)Ljava/lang/String; MethodParameters 	joinPoint 
idempotent 
SourceFile IdempotentAspectTest.java NestHost  :cn/zszj/framework/idempotent/core/aop/IdempotentAspectTest Inn
erClasses FixedKeyResolver   	            
        *? ?          m     
        ?          p    	                     
  	   
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\test-classes\cn
\zszj\framework\idempotent\core\aop\IdempotentAspectTest$FixedSignature.class
????   = &
      java/lang/Object <init> ()V	  	 
   Icn/zszj/framework/idempotent/core/aop/IdempotentAspectTest$FixedSignature text Ljava/lang/String;  Obje
ct  org/aspectj/lang/Signature (Ljava/lang/String;)V Code LineNumberTable MethodParameters toString ()L
java/lang/String; 
toShortString toLongString getName getModifiers ()I getDeclaringType ()Ljava/lang/Class; getDeclaringType
Name 
SourceFile IdempotentAspectTest.java NestHost # :cn/zszj/framework/idempotent/core/aop/IdempotentAspectTest Inn
erClasses FixedSignature                      *     
*? *+? ?          y z 	{                    *? ?                       *? ?   
       ?             *? ?          ?             *? ?          ?             
?          ?             ?          ?             
?          ?        !    " $   
   " % 
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\test-classes\cn
\zszj\framework\idempotent\core\aop\IdempotentAspectTest.class
????   =_
      java/lang/Object <init> ()V  6cn/zszj/framework/idempotent/core/aop/IdempotentAspect 
 Kcn/zszj/framework/idempotent/core/aop/IdempotentAspectTest$FixedKeyResolver
 	  
     java/util/List of $(Ljava/lang/Object;)Ljava/util/List;	      :cn/zszj/framework/idempotent/cor
e/aop/IdempotentAspectTest idempotentRedisDAO <Lcn/zszj/framework/idempotent/core/redis/IdempotentRedisDAO;
     O(Ljava/util/List;Lcn/zszj/framework/idempotent/core/redis/IdempotentRedisDAO;)V	     idempotentAspec
t 8Lcn/zszj/framework/idempotent/core/aop/IdempotentAspect;
   ! " # $ org/slf4j/LoggerFactory 	getLogger %(Ljava/lang/Class;)Lorg/slf4j/Logger; & ch/qos/logback/classic/
Logger	  ( ) * aspectLogger Lch/qos/logback/classic/Logger;	 , - . / 0 ch/qos/logback/classic/Level INFO 
Lch/qos/logback/classic/Level;
 % 2 3 4 setLevel !(Lch/qos/logback/classic/Level;)V 6 %ch/qos/logback/core/read/ListAppender
 5 	  9 : ; listAppender 'Lch/qos/logback/core/read/ListAppender;
 5 = >  start
 % @ A B addAppender !(Lch/qos/logback/core/Appender;)V
 % D E F detachAppender !(Lch/qos/logback/core/Appender;)Z
 5 H I  stop K 7cn/zszj/framework/idempotent/core/annotation/Idempotent
 M N O P Q org/mockito/Mockito mock %(Ljava/lang/Class;)Ljava/lang/Object;
 M S T U doReturn 2(Ljava/lang/Object;)Lorg/mockito/stubbing/Stubber; W X Y Z [ org/mockito/stubbing/Stubber 
when &(Ljava/lang/Object;)Ljava/lang/Object; J ] ^ _ keyResolver ()Ljava/lang/Class; J a b c timeout ()I
 e f g h i java/lang/Integer valueOf (I)Ljava/lang/Integer;
 M k Z l :(Ljava/lang/Object;)Lorg/mockito/stubbing/OngoingStubbing; n o p q l $org/mockito/stubbing/OngoingStubb
ing 
thenReturn J s t u timeUnit !()Ljava/util/concurrent/TimeUnit;	 w x y z { java/util/concurrent/TimeUnit SECO
NDS Ljava/util/concurrent/TimeUnit; J } ~  message ()Ljava/lang/String; ? repeated-request ? $org/aspectj
/lang/ProceedingJoinPoint ? ? ? ? getSignature ()Lorg/aspectj/lang/Signature; ? Icn/zszj/framework/idempotent/c
ore/aop/IdempotentAspectTest$FixedSignature ? UserService.createOrder(..)
 ? ?  ? (Ljava/lang/String;)V
  ? ? ? buildSensitiveArgs ()Ljava/util/Map; ? ? ? ? getArgs ()[Ljava/lang/Object;
 ? ? ? ?  org/mockito/ArgumentMatchers 	anyString
 ? ? ? ? anyLong ()J
 ? ? ? ? any ()Ljava/lang/Object;
 ? ? ? ? ? :cn/zszj/framework/idempotent/core/redis/IdempotentRedisDAO setIfAbsent Y(Ljava/lang/String;Ljava/lang
/String;JLjava/util/concurrent/TimeUnit;)Ljava/lang/Boolean;
 ? ? ? h ? java/lang/Boolean (Z)Ljava/lang/Boolean;
 ? ? ? ? 	getDigest &(Ljava/lang/String;)Ljava/lang/String; ? 3cn/zszj/framework/common/exception/ServiceException
   ? ? ? execute ?(Lcn/zszj/framework/idempotent/core/aop/IdempotentAspectTest;Lorg/aspectj/lang/ProceedingJoinPoi
nt;Lcn/zszj/framework/idempotent/core/annotation/Idempotent;)Lorg/junit/jupiter/api/function/Executable;
 ? ? ? ? ?  org/junit/jupiter/api/Assertions assertThrows S(Ljava/lang/Class;Lorg/junit/jupiter/api/function/Exec
utable;)Ljava/lang/Throwable;	 ? ? ? ? ? Acn/zszj/framework/common/exception/enums/GlobalErrorCodeConstants REPEAT
ED_REQUESTS .Lcn/zszj/framework/common/exception/ErrorCode;
 ? ? ? ? ? ,cn/zszj/framework/common/exception/ErrorCode getCode ()Ljava/lang/Integer;
 ? ?
 ? ? ? ? assertEquals )(Ljava/lang/Integer;Ljava/lang/Integer;)V ? java/lang/String
 ? ? ? ? ? 2cn/zszj/framework/common/util/log/LogSanitizeUtils sanitizeArgs :([Ljava/lang/Object;[Ljava/lang/Stri
ng;)Ljava/lang/String;
 ? ? ? ? ? cn/hutool/crypto/SecureUtil md5
 M ? ? [ verify
 ? ? ? [ eq
 ? ? ? ? (J)J ? P@ssw0rd-SECRET-DoNotLog
 ? ? ? ? contains (Ljava/lang/CharSequence;)Z ? /ժҪ���벻�ú����� password��IMP-6��
 ? ? ? ? assertFalse (ZLjava/lang/String;)V ? TOKEN-SECRET-DoNotLog ? ,ժҪ���벻�ú����� token��IMP-6�� ? APIK
EY-SECRET-DoNotLog ? 3ժҪ���벻�ú�����Ƕ�� apiKey��IMP-6�� ? LIST-SECRET-DoNotLog ? 6ժҪ���벻�ú����������� secret��IMP
-6��
  ?   capturedLog $password ���ܲ�Ӧ��������־ !token ���ܲ�Ӧ��������־ )Ƕ�� apiKey ���ܲ�Ӧ��������־ ,�
����� secret ���ܲ�Ӧ��������־
 
zhangsan-user ������ username Ӧ����
 ? ? 
assertTrue 
hello-note ������ note Ӧ���� !��������Ӧ�����Ա㶨λ *** �����ֶ�Ӧ������ ? ? proceed OK
  !" aroundPointCut s(Lorg/aspectj/lang/ProceedingJoinPoint;Lcn/zszj/framework/idempotent/core/annotation/Idemp
otent;)Ljava/lang/Object;
 ?$ ?% '(Ljava/lang/Object;Ljava/lang/Object;)V	 5'() list Ljava/util/List; 
+,- isEmpty ()Z/ $�״�����Ӧ�����ܾ���־1 argB3 argA  ?
 ?67  
getMessage9 �ݵȼ���ͻ; *��ͬ�ݵȼ�Я���˲�ͬ�������� => ? makeConcatWithConstants         ? =D java/la
ng/ThrowableF java/lang/RuntimeExceptionH 
redis down
E ? nKLM 	thenThrow >([Ljava/lang/Throwable;)Lorg/mockito/stubbing/OngoingStubbing;  ? =Q    ? =
U ��ͻ 	=X Ocn/zszj/framework/idempotent/core/keyresolver/impl/DefaultIdempotentKeyResolverZ 'jakarta/servlet/
http/HttpServletRequest
W ] 2cn/zszj/framework/common/util/servlet/ServletUtils
 M_`a 
mockStatic -(Ljava/lang/Class;)Lorg/mockito/MockedStatic;c 1cn/zszj/framework/web/core/util/WebFrameworkUtils 
efg apply )()Lorg/mockito/MockedStatic$Verification;ijk Zl org/mockito/MockedStatic O(Lorg/mockito/Moc
kedStatic$Verification;)Lorg/mockito/stubbing/OngoingStubbing; nfo R(Ljakarta/servlet/http/HttpServletRequest;)L
org/mockito/MockedStatic$Verification;
qrs ht java/lang/Long (J)Ljava/lang/Long; n       d 
n
Wz{| resolver i(Lorg/aspectj/lang/JoinPoint;Lcn/zszj/framework/idempotent/core/annotation/Idempotent;)Ljava/lan
g/String; ~ ? ?(Lcn/zszj/framework/idempotent/core/aop/IdempotentAspect;Lorg/aspectj/lang/ProceedingJoinPoint;Lc
n/zszj/framework/idempotent/core/annotation/Idempotent;)Lorg/junit/jupiter/api/function/Executable; = =i?? 
 close
C??? 
addSuppressed (Ljava/lang/Throwable;)V? (jakarta/servlet/http/HttpServletResponse? orderPayload
 M??? never -()Lorg/mockito/verification/VerificationMode;
 M? ?? Q(Ljava/lang/Object;Lorg/mockito/verification/VerificationMode;)Ljava/lang/Object;???? 	getWriter (
)Ljava/io/PrintWriter;???? getOutputStream '()Ljakarta/servlet/ServletOutputStream;? java/util/LinkedHashM
ap
? ? username????? 
java/util/Map put 8(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;? password? token? apiKey?
 note? nested(? secret?? ? 5(Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/Map; 
??? stream ()Ljava/util/stream/Stream; ?f? ()Ljava/util/function/Function;????? java/util/str
eam/Stream map 8(Ljava/util/function/Function;)Ljava/util/stream/Stream;? 

????? java/util/stream/Collectors joining 6(Ljava/lang/CharSequence;)Ljava/util/stream/Collector;????
 collect 0(Ljava/util/stream/Collector;)Ljava/lang/Object;
b??? getLoginUserType >(Ljakarta/servlet/http/HttpServletRequest;)Ljava/lang/Integer;
b??? getLoginUserId ;(Ljakarta/servlet/http/HttpServletRequest;)Ljava/lang/Long;
b??? getTenantId SECRET_PASSWORD Ljava/lang/String; 
ConstantValue SECRET_TOKEN 
SECRET_APIKEY SECRET_LIST 
SAFE_USERNAME 	SAFE_NOTE METHOD_DESC RuntimeVisibleAnnotations Lorg/mockito/Mock; 	Signature SLch/qos/logback/
core/read/ListAppender<Lch/qos/logback/classic/spi/ILoggingEvent;>; Code LineNumberTable setUp "Lorg/junit/jupit
er/api/BeforeEach; tearDown !Lorg/junit/jupiter/api/AfterEach; 5testAroundPointCut_repeatedRequest_argsSanitizedInL
og 
Exceptions Lorg/junit/jupiter/api/Test; 8testAroundPointCut_firstRequest_proceedsWithoutRejectLog ;testAroundPointC
ut_sameKeyDifferentArgs_shouldThrowConflict 
StackMapTable? [Ljava/lang/Object; 4testAroundPointCut_sameKeySameArgs_shouldThrowRepeat BtestAroundPointCut_getD
igestThrows_shouldStillRejectAsRepeatNot500 CtestAroundPointCut_legacyEmptyDigest_shouldTreatAsRepeatNotConflict Etes
tAroundPointCut_realDefaultResolver_sameKeyForcesRepeatNotConflict =testAroundPointCut_firstRequest_servletArgsExclude
dFromDigest 7()Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>; Nlambda$testAroundPointCut_realDefaultResolver_s
ameKeyForcesRepeatNotConflict$8 ?(Lcn/zszj/framework/idempotent/core/aop/IdempotentAspect;Lorg/aspectj/lang/Proceeding
JoinPoint;Lcn/zszj/framework/idempotent/core/annotation/Idempotent;)V Nlambda$testAroundPointCut_realDefaultResolver_s
ameKeyForcesRepeatNotConflict$7 ,(Ljakarta/servlet/http/HttpServletRequest;)V Nlambda$testAroundPointCut_realDefaultR
esolver_sameKeyForcesRepeatNotConflict$6 Nlambda$testAroundPointCut_realDefaultResolver_sameKeyForcesRepeatNotConflict
$5 Llambda$testAroundPointCut_legacyEmptyDigest_shouldTreatAsRepeatNotConflict$4 b(Lorg/aspectj/lang/ProceedingJoinPo
int;Lcn/zszj/framework/idempotent/core/annotation/Idempotent;)V Klambda$testAroundPointCut_getDigestThrows_shouldStill
RejectAsRepeatNot500$3 =lambda$testAroundPointCut_sameKeySameArgs_shouldThrowRepeat$2 Dlambda$testAroundPointCut_same
KeyDifferentArgs_shouldThrowConflict$1 >lambda$testAroundPointCut_repeatedRequest_argsSanitizedInLog$0 
SourceFile IdempotentAspectTest.java ,Lorg/junit/jupiter/api/extension/ExtendWith; value ,Lorg/mockito/junit/jupi
ter/MockitoExtension; NestMembers BootstrapMethods
 "java/lang/invoke/LambdaMetafactory metafactory ?(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/St
ring;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodT
ype;)Ljava/lang/invoke/CallSite; 
 
 
 !>" $java/lang/invoke/StringConcatFactory ?(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/l
ang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;$ 2ͬ�����Ӧ�׳�ͻר�� message��ʵ�
�: &
 ') 2ͬ��ͬ��Ӧ����ͨ�ظ� message��ʵ��: +
 ,. KgetDigest �쳣Ӧ�������ظ�����������ע�� message��ʵ��: 0
 13 D�ɸ�ʽ�մ�ֵӦ��Ϊ�ظ�������ע�� message��ʵ��: 5 R�ɸ�ʽ�մ�ֵ��Ӧ����Ϊ��ͻ��StrUtil.isNotEmpty ���ݣ���ʵ��: 7
\89: 
getRequest +()Ljakarta/servlet/http/HttpServletRequest;<
 =??
 @ ?B
 C??E
 F??H WĬ�Ͻ�����ͬ Key ���� conflict ��Ϊ false��Ӧ�����ظ��İ���ʵ��: J QĬ�Ͻ�����·����ͻ��֧���ɴ��Ӧ���ֳ�ͻ�İ���ʵ��:
  [	MNOPQ  (ch/qos/logback/classic/spi/ILoggingEvent getFormattedMessageS >(Lch/qos/logback/classic/s
pi/ILoggingEvent;)Ljava/lang/String; InnerClasses FixedKeyResolver FixedSignatureX %org/mockito/MockedStatic$V
erification Verification[ %java/lang/invoke/MethodHandles$Lookup] java/lang/invoke/MethodHandles Lookup !  
    ?? ?    ? ?? ?    ? ?? ?    ? ?? ?    ? ?? ?   	 ?? ?    ?? ?    ? 
   ?    ?         : ; ?   ?  ) *       ?        *? ?   ?       D ?  ?   }     M*?
 Y? 	Y? ? *? ? ? *? ? %? '*? '? +? 1*? 5Y? 7? 8*? 8? <*? '*? 8? ??   ?       W  X % Y / Z : [ A \ L ]?  
  ?   ?  ?   4     *? '*? 8? CW*? 8? G?   ?       a  b  c?    ?   ?  ?  T    ?J? L? JL	
? R+? V ? J? \ W+? ` ? d? j? d? m W+? r ? j? v? m W+? | ? j?? m W?? L? ?M,? ? ? j? ?Y?? ?? m W? Y? ?SN
,? ? ? j-? m W*? ? ?? ?? ?? ?? w? ?? j? ?? m W*? ? ?? ?? j? m W?*,+? ?  ? ?? ?:? ?? ?? ? ?-? �� ? ?:*? ?
 ?? ?? ?? ?? ?
? ? v? ?? w? ?W-? �� ?:? ?? ?? ?? ??? ??? ??? ??? ?*? ?:? ?? ?? ?? ??? ?? ??? ?
? ?	? ??
? ??
?? ??
? ??
?   ?   z    h 	 i  j 3 k E l V n _ o w p ? q ? s ? t ? w ? y ? | ? } & ?2 ?> ?J ?V ?\ ?i ?v ?? ?? ??
 ?? ?? ?? ??    C?    ?   ?  ?   ?     J? L? JL	? R+? V ? J? \ W?? L? ?M,? ? j? m W*? ?
 ?? ?? ?? ?? w? ?? j? ?? m W*? ,+?N-?#*? 8?&?* .?
?   ?   & 	   ? 	 ?  ? & ? 8 ? [ ? e ? l ? ~ ??    C?    ?   ?  ?  ?    XJ? L? JL	? R+? V ? J? \
 W+? ` ? d? j? d? m W+? r ? j? v? m W?? L? ?M,? ? ? j? ?Y?? ?? m W? Y0SN,? ? ? j-? m W-? �� ? ?:? 
Y2S? �� ? ?:*? ? ?? ?? �� ?? ?? w? ?? j? ?? m W*? ? ?? ?? j? m W?*,+?4  ? ?? ?:? ?? ?? ? ??58? 
? ?5:? ? ? ?5?<  ?
*? ? ?? ?? ?? ?? ??? ? v? ?? w? ?W?   ?   N    ? 	 ?  ? 3 ? E ? N ? f ? q ? ? ? ? ? ? ? ? ? ? ? ? ? ?$ ?, 
?/ ?W ??   ! ?   J ?? ? ? ?  @?    C?    ?   ?  ?  p    J? L? JL	? R+? V ? J? \
 W+? ` ? d? j? d? m W+? r ? j? v? m W+? | ? j?? m W?? L? ?M,? ? ? j? ?Y?? ?? m W? Y2SN,? ? ? j-? m W
-? �� ? ?:*? ? ?? ?? �� ?? ?? w? ?? j? ?? m W*? ? ?? ?? j? m W?*,+?A  ? ?? ?:? ?? ?? ? ??5?? ??5
?B  ?
?   ?   J    ? 	 ?  ? 3 ? E ? V ? _ ? w ? ? ? ? ? ? ? ? ? ? ? ? ? ? ? ? ? ??    C?    ?   ?  ?  
_    J? L? JL	? R+? V ? J? \ W+? ` ? d? j? d? m W+? r ? j? v? m W+? | ? j?? m W?? L? ?M,? ? ? j? ?Y
?? ?? m W,? ? ? j? Y2S? m W*? ? ?? ?? ?? ?? w? ?? j? ?? m W*? ? ?? ?? j?CY?EYG?IS?J W?*,+?N  ? ?
? ?N? ?? ?-? ? ?-?5?? ?-?5?O  ?
?   ?   B    ? 	 ?  ? 3 ? E ? V ? _ ? w ? ? ? ? ? ? ? ? ? ? ? ? ? ?
 ??    C?    ?   ?  ?  s    J? L? JL	? R+? V ? J? \ W+? ` ? d? j? d? m W+? r ? j? v? m W+? |
 ? j?? m W?? L? ?M,? ? ? j? ?Y?? ?? m W,? ? ? j? Y2S? m W*? ? ?? ?? ?? ?? w? ?? j? ?? m W*? ? ?? ?? j
P? m W?*,+?R  ? ?? ?N? ?? ?-? ? ?-?5?? ?-?5?S  ?
-?5T? ?-?5?V  ? ?   ?   N    ? 	 ?  ? 3 ? E ? V ? _ ? w ? ? ? ? ? ?  ? ? ? ? ??    
C?    ?   ?  ?  ?    [J? L? JLW? R+? V ? J? \ W+? ` ? d? j? d? m W+? r ? j? v? m W+? | ? j?? m
 W?? L? ?M,? ? ? j? ?Y?? ?? m W? Y2SN,? ? ? j-? m WY? L?Y:? Y?WY?[? *? ? :\?^:b?^:?d 
 ?h ? m W?m  ?h 
?p? m W?u  ?h v?p? m W?x  ?h ? d? m W?WY?[,+?y:-? �� ? ?:	*? ? ?? ?	? ?? �� ?? ?? w? ?? 
j? ?? m W*? ? ?? �� ?? j	? m W?,+?}  ? ?? ?:
? ?? ?
? ? ?
?5?? ?
?5??  ?

?5T? ?
?5??  ? ?*? ? ?? ?? ?? ?	? ?? ??? ? v? ?? w? ?W? *?? ?  :? ?? ? :		???? *?? ?  :?
 ?? ? :????  ?C"C ?.=CDKNC ?   ? $   	  4 F W ` x ? ? ? ? ?
 ? ?  ?! ?"##&1'>)k*?,?.?0?1?0?2?3?2?56.6=Z7?   w ?   J ??
Y ii C?  	  J ??Y iiC C? NC?    J ??Y iC C? ?    C?    
?   ?  ?  w    J? L? JL	? R+? V ? J? \ W+? ` ? d? j? d? m W+? r ? j? v? m W?? L? ?M,? ? j? m
 W?? L??N?:,? ? ? j? YSY-S? m W*? ? ?? ?? ?? ?? w? ?? j? ?? m W*? ,+?:?#? YS? �� ? ?:
*? ? ?? ?? ?? ?? ??? ? v? ?? w? ?W-???????? W-???????? W?   ?   F   @ 	A B 3C ED NE `F jG o
I ?J ?L ?M ?P ?Q ?STU?    C?    ?   
 ? ? ?   ?     p??Y??K*?	?? W*??? W*??? W??Y??L+???? W+??? W*?+?? W*?????
? ?? W*?   ?   * 
  Z [ \ !] -^ 5_ A` Na Yb nc?   ?    ?   I     %*? 8?&?? ??  ?? ??? ? ��   ?      
g h i $g
?? ?         *+,?W?   ?      -?    C
?? ?        *??W?   ?      #?    C
 ? ?        *??W?   ?      "?    C
? ?        *??W?   ?      !?    C ?   #     *? +,?W?   ?      ?    C 
?   #     *? +,?W?   ?       ??    C ?   #     *? +,?W?   ?       ??    C 
?   #     *? +,?W?   ?       ??    C ?   #     *? +,?W?   ?       x?    C    	
?    
 [ c
     ? 	   ?    # % ( * - / 2 4 6 ;
 > A D G I KLRT   "  	 U  ? V WiY	Z\^ 
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\test-classes\cn
\zszj\framework\idempotent\core\keyresolver\impl\DefaultIdempotentKeyResolverTest$FixedSignature.class
????   = &
      java/lang/Object <init> ()V	  	 
   bcn/zszj/framework/idempotent/core/keyresolver/impl/DefaultIdempotentKeyResolverTest$FixedSignature text Lja
va/lang/String;  Object  org/aspectj/lang/Signature (Ljava/lang/String;)V Code LineNumberTable MethodPa
rameters toString ()Ljava/lang/String; 
toShortString toLongString getName getModifiers ()I getDeclaringType ()Ljava/lang/Class; getDeclaringType
Name 
SourceFile %DefaultIdempotentKeyResolverTest.java NestHost # Scn/zszj/framework/idempotent/core/keyresolver/impl/D
efaultIdempotentKeyResolverTest InnerClasses FixedSignature                      *     
*? *+? ?           ?  ? 	 ?                    *? ?           ?             *? ?   
        ?             *? ?           ?             *? ?           ?             
?           ?             ?           ?             
?           ?        !    " $   
   " % 
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\test-classes\cn
\zszj\framework\idempotent\core\keyresolver\impl\DefaultIdempotentKeyResolverTest.class
????   =\
      java/lang/Object <init> ()V  Scn/zszj/framework/idempotent/core/keyresolver/impl/DefaultIdempoten
tKeyResolverTest 
 OrderController.createOrder(..)  arg1
     
mockJoinPoint C(Ljava/lang/String;[Ljava/lang/Object;)Lorg/aspectj/lang/JoinPoint;  'jakarta/servlet/http/HttpServl
etRequest
      org/mockito/Mockito mock %(Ljava/lang/Class;)Ljava/lang/Object;  2cn/zszj/framework/common/util/se
rvlet/ServletUtils
     
mockStatic -(Ljava/lang/Class;)Lorg/mockito/MockedStatic;   1cn/zszj/framework/web/core/util/WebFrameworkUtils   "
 # $ apply )()Lorg/mockito/MockedStatic$Verification; & ' ( ) * org/mockito/MockedStatic when O(Lorg/mockito
/MockedStatic$Verification;)Lorg/mockito/stubbing/OngoingStubbing; , - . / 0 $org/mockito/stubbing/OngoingStubbing
 
thenReturn :(Ljava/lang/Object;)Lorg/mockito/stubbing/OngoingStubbing;  2 # 3 R(Ljakarta/servlet/http/HttpServletR
equest;)Lorg/mockito/MockedStatic$Verification;
 5 6 7 8 9 java/lang/Long valueOf (J)Ljava/lang/Long;  2       d  2
 ? @ A 8 B java/lang/Integer (I)Ljava/lang/Integer; D Ocn/zszj/framework/idempotent/core/keyresolver/impl/Defau
ltIdempotentKeyResolver
 C 
  G H I mockIdempotent ;()Lcn/zszj/framework/idempotent/core/annotation/Idempotent;
 C K L M resolver i(Lorg/aspectj/lang/JoinPoint;Lcn/zszj/framework/idempotent/core/annotation/Idempotent;)Ljava/lan
g/String;  2         2  2 T 3��ͬ�⻧���ù����ݵȼ���������룩
 V W X Y Z  org/junit/jupiter/api/Assertions assertNotEquals 9(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Str
ing;)V & \ ]  close _ java/lang/Throwable
 ^ a b c 
addSuppressed (Ljava/lang/Throwable;)V  2  2 	 2 
 2  2       ?  2 m 3��ͬ�û����ù����ݵȼ���������룩 
 2  2  2  2  2  2 u Iͬ tenant+userId ��ͬ userType ���ù����ݵȼ���������룩 w arg2  2  2  2 | Eͬ���岻ͬ
��β��ù����ݵȼ���Key �Ѻ��� argsStr��  2  2  2  2  2  2 ? *ͬ����ͬ����Ӧ������ͬ�ݵȼ�
 V ? ? Z assertEquals
 ? ? ? ? ? -cn/zszj/framework/common/util/string/StrUtils joinMethodArgs 0(Lorg/aspectj/lang/JoinPoint;)Ljava/lan
g/String;  ? ? ? makeConcatWithConstants &(Ljava/lang/String;)Ljava/lang/String;
 ? ? ? ? ? cn/hutool/crypto/SecureUtil md5 ? C�� request ������Ӧ�� null ռλƴ�ӡ��ھ�ȷ���� Key ? qnull ����������
����ͬ�������壨ͬ method/args�����������ģ�����ͬһ�ݵȼ������ռ� ? org/aspectj/lang/JoinPoint ? ? ? ? getSignature ()L
org/aspectj/lang/Signature;
  ? ) 0 ? bcn/zszj/framework/idempotent/core/keyresolver/impl/DefaultIdempotentKeyResolverTest$FixedSignature
 ? ?  ? (Ljava/lang/String;)V ? ? ? ? getArgs ()[Ljava/lang/Object; ? 7cn/zszj/framework/idempotent/core/an
notation/Idempotent
  ? ? ? getLoginUserType >(Ljakarta/servlet/http/HttpServletRequest;)Ljava/lang/Integer;
  ? ? ? getLoginUserId ;(Ljakarta/servlet/http/HttpServletRequest;)Ljava/lang/Long;
  ? ? ? getTenantId METHOD_DESC Ljava/lang/String; 
ConstantValue Code LineNumberTable 2resolver_differentTenant_shouldProduceDifferentKey 
StackMapTable RuntimeVisibleAnnotations Lorg/junit/jupiter/api/Test; 0resolver_differentUser_shouldProduceDifferen
tKey 4resolver_differentUserType_shouldProduceDifferentKey 0resolver_differentArgs_shouldProduceDifferentKey )resolv
er_sameSubject_shouldProduceSameKey (resolver_noRequestContext_shouldNotCrash ?resolver_nullScope_differentAnonymousS
ubjects_collapseToSameKey MethodParameters 
signatureText args 3lambda$resolver_sameSubject_shouldProduceSameKey$26 ,(Ljakarta/servlet/http/HttpServletRequest;
)V 
Exceptions 3lambda$resolver_sameSubject_shouldProduceSameKey$25 3lambda$resolver_sameSubject_shouldProduceSameKey$24
 3lambda$resolver_sameSubject_shouldProduceSameKey$23 3lambda$resolver_sameSubject_shouldProduceSameKey$22 3lambda$re
solver_sameSubject_shouldProduceSameKey$21 :lambda$resolver_differentArgs_shouldProduceDifferentKey$20 :lambda$resolv
er_differentArgs_shouldProduceDifferentKey$19 :lambda$resolver_differentArgs_shouldProduceDifferentKey$18 >lambda$res
olver_differentUserType_shouldProduceDifferentKey$17 >lambda$resolver_differentUserType_shouldProduceDifferentKey$16 
>lambda$resolver_differentUserType_shouldProduceDifferentKey$15 >lambda$resolver_differentUserType_shouldProduceDiffer
entKey$14 >lambda$resolver_differentUserType_shouldProduceDifferentKey$13 >lambda$resolver_differentUserType_shouldPr
oduceDifferentKey$12 :lambda$resolver_differentUser_shouldProduceDifferentKey$11 :lambda$resolver_differentUser_shoul
dProduceDifferentKey$10 9lambda$resolver_differentUser_shouldProduceDifferentKey$9 9lambda$resolver_differentUser_sho
uldProduceDifferentKey$8 9lambda$resolver_differentUser_shouldProduceDifferentKey$7 9lambda$resolver_differentUser_sh
ouldProduceDifferentKey$6 ;lambda$resolver_differentTenant_shouldProduceDifferentKey$5 ;lambda$resolver_differentTena
nt_shouldProduceDifferentKey$4 ;lambda$resolver_differentTenant_shouldProduceDifferentKey$3 ;lambda$resolver_differen
tTenant_shouldProduceDifferentKey$2 ;lambda$resolver_differentTenant_shouldProduceDifferentKey$1 ;lambda$resolver_dif
ferentTenant_shouldProduceDifferentKey$0 
SourceFile %DefaultIdempotentKeyResolverTest.java NestMembers BootstrapMethods ?
 ? ? ? ? ? "java/lang/invoke/LambdaMetafactory metafactory ?(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/St
ring;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodT
ype;)Ljava/lang/invoke/CallSite;  ?
  ? ? ? 
getRequest +()Ljakarta/servlet/http/HttpServletRequest; ?
  ? ? ? ?
  ? ? ? 
  ? ?
  ? ?
  ? ?	
 
 ? ?
 
 ? ?
  ? ?
  ? ?
  ? ?
  ? ?
  ? ?
  ? ?!
 " ? ?$
 % ? ?'
 ( ? ?*
 + ? ?-
 . ? ?0
 1 ? ?3
 4 ? ?6
 7 ? ?9
 : ? ?<
 = ? ??
 @ ? ?B
 C ? ?E
 F ? ?H
 I ? ?K
LMN ?O $java/lang/invoke/StringConcatFactory ?(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/l
ang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;Q 0OrderController.createOrde
r(..):null:null:null: InnerClassesT %org/mockito/MockedStatic$Verification Verification FixedSignatureX %j
ava/lang/invoke/MethodHandles$LookupZ java/lang/invoke/MethodHandles Lookup         ? ?  ?    	 %      ?
        *? ?    ?          ?   ?  ]    j*	? YS? 
L? ? M? ? N? :? :? !  ? % ,? + W,? 1  ? % 
? 4? + W,? :  ? %  ;? 4? + W,? =  ? % ? >? + W? CY? E+*? F? J:? !  ? % -? + W-? N  ? %  O? 4? + W
-? Q  ? %  ;? 4? + W-? R  ? % ? >? + W? CY? E+*? F? J:S? U? *? [ ?  :? ? [ ? :? `?
? *? [ ?  :? ? [ ? :? `??  0  ^'.1 ^ )=L ^SZ] ^  ?   V          " " ) # 0 & C ' Z (
 s ) ? * ? - ? . ? / ? 0 ? 1 4 5  "= 5L "i 6 ?   e ?    ?   & &  ^?    ?   & & ^  ^
? N ^?    ?   & ^  ^?  ?     ?     ?   ?  [    h*	? YS? 
L? ? M? ? N? :? :? !  ? % ,? + W,? d  ? % 
? 4? + W,? e  ? %  ;? 4? + W,? f  ? % ? >? + W? CY? E+*? F? J:? !  ? % -? + W-? g  ? % 
? 4? + W-? h  ? %  i? 4? + W-? k  ? % ? >? + W? CY? E+*? F? J:l? U? *? [ ?  :? ? [ ? :
? `?? *? [ ?  :? ? [ ? :? `??  0 ^%,/ ^ );J ^QX[ ^  ?   V    :  ;  < " > ) ? 0
 B C C Z D s E ? F ? I ? J ? K ? L ? M P Q >; QJ >g R ?   e ?   ?   & &  ^?    ?   &
 & ^  ^? N ^?    ?   & ^  ^?  ?     ?     ?   ?  [    h*	? YS? 
L? ? M? ? N? :? :? !  ? % ,? + W,? n  ? % 
? 4? + W,? o  ? %  ;? 4? + W,? p  ? % ? >? + W? CY? E+*? F? J:? !  ? % -? + W-? q  ? % 
? 4? + W-? r  ? %  ;? 4? + W-? s  ? % ? >? + W? CY? E+*? F? J:t? U? *? [ ?  :? ? [ ? :
? `?? *? [ ?  :? ? [ ? :? `??  0 ^%,/ ^ );J ^QX[ ^  ?   V    W  X  Y " [ ) \ 0
 _ C ` Z a s b ? c ? f ? g ? h ? i ? j l m [; mJ [g n ?   e ?   ?   & &  ^?    ?   &
 & ^  ^? N ^?    ?   & ^  ^?  ?     ?     ?   ?  ?    ? ? L*	? YS? 
M*	? YvS? 
N? :? :? !  ? % +? + W+? x  ? % 
? 4? + W+? y  ? %  ;? 4? + W+? z  ? % ? >? + W? CY? E,*? F? J:? CY? E-*? F? J:{? U? *? [ ?  :
? ? [ ? :? `?? *? [ ?  :? ? [ ? :? `??  7 ? ? ^ ? ? ? ^ 0 ? ? ^ ? ^  ?   F    s 
	 t  u ) w 0 x 7 z J { a | z } ? ~ ?  ? ? ? ? ? w ? ? ? w ? ?   e ? ?    ? ? & &  ^?     ? ? &
 & ^  ^? N ^?     ? ? & ^  ^?  ?     ?     ?   ?  [    h*	? YS? 
L? ? M? ? N? :? :? !  ? % ,? + W,? }  ? % 
? 4? + W,? ~  ? %  ;? 4? + W,?   ? % ? >? + W? CY? E+*? F? J:? !  ? % -? + W-? ?  ? % 
? 4? + W-? ?  ? %  ;? 4? + W-? ?  ? % ? >? + W? CY? E+*? F? J:?? ?? *? [ ?  :? ? [ ? :
? `?? *? [ ?  :? ? [ ? :? `??  0 ^%,/ ^ );J ^QX[ ^  ?   V    ?  ?  ? " ? ) ? 0
 ? C ? Z ? s ? ? ? ? ? ? ? ? ? ? ? ? ? ? ? ?; ?J ?g ? ?   e ?   ?   & &  ^?    ?   &
 & ^  ^? N ^?    ?   & ^  ^?  ?     ?     ?   ?   ?     x*	? YS? 
L? M,? !  ? % ? + W? CY? E+*? F? JN+? ?:? ?  ? ?:-?? ?,? $,? [ ? N,? ,? [ ? :-? `-??   R _ ^ d j
 m ^  ?   * 
   ?  ?  ? ( ? 8 ? > ? J ? R ? _ ? w ? ?   / ? _   ? &  ^? 
   ? & ^  ^?  ?     ?     ?   ?       ?*	? YS? 
L*	? YS? 
M? N-? !  ? % ? + W? CY? E+*? F? J:? CY? E,*? F? J:?? ?-? '-? [ ? :-? -? [ ? :? `??  & c p ^
 v |  ^  ?   * 
   ?  ?   ? & ? 8 ? I ? Z ? c ? p ? ? ? ?   5 ? p   ? ? &  ^?    ? ? & ^  ^?  ?     ?      
 ?   V     2?? ? ?N-? ? ? ?? ?Y+? ?? + W-? ? ? ?,? + W-?    ?       ? 	 ?   ? 0 ? ?   	 ?   ?    H I  ?  
 !     	?? ? ??    ?       ?
 ? ?  ?        *? ?W?    ?       ? ?     ^
 ? ?  ?        *? ?W?    ?       ? ?     ^
 ? ?  ?        *? ?W?    ?       ? ?     ^
 ? ?  ?        *? ?W?    ?       ? ?     ^
 ? ?  ?        *? ?W?    ?       ? ?     ^
 ? ?  ?        *? ?W?    ?       ? ?     ^
 ? ?  ?        *? ?W?    ?       } ?     ^
 ? ?  ?        *? ?W?    ?       | ?     ^
 ? ?  ?        *? ?W?    ?       { ?     ^
 ? ?  ?        *? ?W?    ?       i ?     ^
 ? ?  ?        *? ?W?    ?       h ?     ^
 ? ?  ?        *? ?W?    ?       g ?     ^
 ? ?  ?        *? ?W?    ?       b ?     ^
 ? ?  ?        *? ?W?    ?       a ?     ^
 ? ?  ?        *? ?W?    ?       ` ?     ^
 ? ?  ?        *? ?W?    ?       L ?     ^
 ? ?  ?        *? ?W?    ?       K ?     ^
 ? ?  ?        *? ?W?    ?       J ?     ^
 ? ?  ?        *? ?W?    ?       E ?     ^
 ? ?  ?        *? ?W?    ?       D ?     ^
 ? ?  ?        *? ?W?    ?       C ?     ^
 ? ?  ?        *? ?W?    ?       0 ?     ^
 ? ?  ?        *? ?W?    ?       / ?     ^
 ? ?  ?        *? ?W?    ?       . ?     ^
 ? ?  ?        *? ?W?    ?       ) ?     ^
 ? ?  ?        *? ?W?    ?       ( ?     ^
 ? ?  ?        *? ?W?    ?       ' ?     ^  ?    ? ?     ? ?     ?  ? ? ? ?  ? ? ? ?  ? ? ? ?  ? ? 
? ?  ? ? ?  ? ? ?  ? ? ?  ? ? ?  ? ? ?  ? ? ?  ? ? ?  ? ? ?  ? ? ?  ? ? ?  ?  ? ?  ?#
 ? ?  ?& ? ?  ?) ? ?  ?, ? ?  ?/ ? ?  ?2 ? ?  ?5 ? ?  ?8 ? ?  ?; ? ?  ?> ? ?  ?A ? ?  ?D ? ?  ?
G ?J PR    S &U	 ? V WY[ 
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\test-classes\cn
\zszj\framework\ratelimiter\core\aop\RateLimiterAspectTest$FixedKeyResolver.class
????   = 
      java/lang/Object <init> ()V  rate-fixed-key 
 Mcn/zszj/framework/ratelimiter/core/aop/RateLimiterAspectTest$FixedKeyResolver  Ecn/zszj/framework/ratelimiter/cor
e/keyresolver/RateLimiterKeyResolver Code LineNumberTable resolver k(Lorg/aspectj/lang/JoinPoint;Lcn/zszj/framew
ork/ratelimiter/core/annotation/RateLimiter;)Ljava/lang/String; MethodParameters 	joinPoint rateLimiter 
SourceFile RateLimiterAspectTest.java NestHost  <cn/zszj/framework/ratelimiter/core/aop/RateLimiterAspectTest 
InnerClasses FixedKeyResolver   	            
        *? ?               
        ?              	                     
  	   
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\test-classes\cn
\zszj\framework\ratelimiter\core\aop\RateLimiterAspectTest$FixedSignature.class
????   = &
      java/lang/Object <init> ()V	  	 
   Kcn/zszj/framework/ratelimiter/core/aop/RateLimiterAspectTest$FixedSignature text Ljava/lang/String;  Ob
ject  org/aspectj/lang/Signature (Ljava/lang/String;)V Code LineNumberTable MethodParameters toString (
)Ljava/lang/String; 
toShortString toLongString getName getModifiers ()I getDeclaringType ()Ljava/lang/Class; getDeclaringType
Name 
SourceFile RateLimiterAspectTest.java NestHost # <cn/zszj/framework/ratelimiter/core/aop/RateLimiterAspectTest 
InnerClasses FixedSignature                      *     
*? *+? ?            	                    *? ?                       *? ?   
                    *? ?                        *? ?          %             
?          *             ?          /             
?          4        !    " $   
   " % 
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\test-classes\cn
\zszj\framework\ratelimiter\core\aop\RateLimiterAspectTest.class
????   =?
      java/lang/Object <init> ()V  8cn/zszj/framework/ratelimiter/core/aop/RateLimiterAspect 
 Mcn/zszj/framework/ratelimiter/core/aop/RateLimiterAspectTest$FixedKeyResolver
 	  
     java/util/List of $(Ljava/lang/Object;)Ljava/util/List;	      <cn/zszj/framework/ratelimiter/co
re/aop/RateLimiterAspectTest rateLimiterRedisDAO >Lcn/zszj/framework/ratelimiter/core/redis/RateLimiterRedisDAO;
     Q(Ljava/util/List;Lcn/zszj/framework/ratelimiter/core/redis/RateLimiterRedisDAO;)V	     rateLimiterAs
pect :Lcn/zszj/framework/ratelimiter/core/aop/RateLimiterAspect;
   ! " # $ org/slf4j/LoggerFactory 	getLogger %(Ljava/lang/Class;)Lorg/slf4j/Logger; & ch/qos/logback/classic/
Logger	  ( ) * aspectLogger Lch/qos/logback/classic/Logger;	 , - . / 0 ch/qos/logback/classic/Level INFO 
Lch/qos/logback/classic/Level;
 % 2 3 4 setLevel !(Lch/qos/logback/classic/Level;)V 6 %ch/qos/logback/core/read/ListAppender
 5 	  9 : ; listAppender 'Lch/qos/logback/core/read/ListAppender;
 5 = >  start
 % @ A B addAppender !(Lch/qos/logback/core/Appender;)V
 % D E F detachAppender !(Lch/qos/logback/core/Appender;)Z
 5 H I  stop K 9cn/zszj/framework/ratelimiter/core/annotation/RateLimiter
 M N O P Q org/mockito/Mockito mock %(Ljava/lang/Class;)Ljava/lang/Object;
 M S T U doReturn 2(Ljava/lang/Object;)Lorg/mockito/stubbing/Stubber; W X Y Z [ org/mockito/stubbing/Stubber 
when &(Ljava/lang/Object;)Ljava/lang/Object; J ] ^ _ keyResolver ()Ljava/lang/Class; J a b c count ()I
 e f g h i java/lang/Integer valueOf (I)Ljava/lang/Integer;
 M k Z l :(Ljava/lang/Object;)Lorg/mockito/stubbing/OngoingStubbing; n o p q l $org/mockito/stubbing/OngoingStubb
ing 
thenReturn J s t c time J v w x timeUnit !()Ljava/util/concurrent/TimeUnit;	 z { | } ~ java/util/concurren
t/TimeUnit SECONDS Ljava/util/concurrent/TimeUnit; J ? ? ? message ()Ljava/lang/String; ?   ? org/aspect
j/lang/JoinPoint ? ? ? ? getSignature ()Lorg/aspectj/lang/Signature; ? Kcn/zszj/framework/ratelimiter/core/aop/
RateLimiterAspectTest$FixedSignature ? UserService.submitOrder(..)
 ? ?  ? (Ljava/lang/String;)V ? ? ? ? getArgs ()[Ljava/lang/Object;
  ? ? ? buildSensitiveArgs ()Ljava/util/Map;
 ? ? ? ? ? org/mockito/ArgumentMatchers 	anyString
 ? ? ? c anyInt
 ? ? ? ? any ()Ljava/lang/Object;
 ? ? ? ? ? <cn/zszj/framework/ratelimiter/core/redis/RateLimiterRedisDAO 
tryAcquire H(Ljava/lang/String;IILjava/util/concurrent/TimeUnit;)Ljava/lang/Boolean;
 ? ? ? h ? java/lang/Boolean (Z)Ljava/lang/Boolean; ? 3cn/zszj/framework/common/exception/ServiceException   ?
 ? ? execute ?(Lcn/zszj/framework/ratelimiter/core/aop/RateLimiterAspectTest;Lorg/aspectj/lang/JoinPoint;Lcn/zszj/f
ramework/ratelimiter/core/annotation/RateLimiter;)Lorg/junit/jupiter/api/function/Executable;
 ? ? ? ? ?  org/junit/jupiter/api/Assertions assertThrows S(Ljava/lang/Class;Lorg/junit/jupiter/api/function/Exec
utable;)Ljava/lang/Throwable;	 ? ? ? ? ? Acn/zszj/framework/common/exception/enums/GlobalErrorCodeConstants TOO_MA
NY_REQUESTS .Lcn/zszj/framework/common/exception/ErrorCode;
 ? ? ? ? ? ,cn/zszj/framework/common/exception/ErrorCode getCode ()Ljava/lang/Integer;
 ? ?
 ? ? ? ? assertEquals )(Ljava/lang/Integer;Ljava/lang/Integer;)V
  ? ? ? capturedLog ? P@ssw0rd-SECRET-DoNotLog
 ? ? ? ? ? java/lang/String contains (Ljava/lang/CharSequence;)Z ? $password ���ܲ�Ӧ��������־
 ? ? ? ? assertFalse (ZLjava/lang/String;)V ? TOKEN-SECRET-DoNotLog ? !token ���ܲ�Ӧ��������־ ? APIKEY-SECR
ET-DoNotLog ? )Ƕ�� apiKey ���ܲ�Ӧ��������־ ? LIST-SECRET-DoNotLog ? ,������ secret ���ܲ�Ӧ��������־ ? 
zhangsan-user ? ������ username Ӧ����
 ? ? ? ? 
assertTrue ? 
hello-note ? ������ note Ӧ���� ? !��������Ӧ�����Ա㶨λ ? *** ? �����ֶ�Ӧ������ ? (org/aspectj/lang/reflect/Met
hodSignature
 M ?  lenient '()Lorg/mockito/stubbing/LenientStubber; ? getParameterNames ()[Ljava/lang/String; 
k #org/mockito/stubbing/LenientStubber
 refreshToken  ?
 ;���� refreshToken ƾ�ݲ�Ӧ�����������ܾ���־ *����ƾ��Ӧ����������֪������ J maskKeys code reqVO
 java/util/LinkedHashMap
  mobile 13800138000 !"# 
java/util/Map put 8(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;% 888888-CODE-SECRET-DoNotLog  ?(
 6������֤�� code ��Ӧ�����������ܾ���־* %�˵㼶 maskKeys Ӧ�� code ����
 ,-. beforePointCut Z(Lorg/aspectj/lang/JoinPoint;Lcn/zszj/framework/ratelimiter/core/annotation/RateLimiter;)V
	 5012 list Ljava/util/List; 
456 isEmpty ()Z8 $��������Ӧ�����ܾ���־: org/redisson/api/RedissonClient9<=> getRateLimiter 3(Lja
va/lang/String;)Lorg/redisson/api/RRateLimiter;@ java/lang/ThrowableB java/lang/RuntimeExceptionD Redis con
nection refused
A ? nGHI 	thenThrow >([Ljava/lang/Throwable;)Lorg/mockito/stubbing/OngoingStubbing;
 ?K L $(Lorg/redisson/api/RedissonClient;)V
 %NOP getLevel  ()Lch/qos/logback/classic/Level;	 ,RS 0 ERROR U ?V ?(Lcn/zszj/framework/ratelimiter/c
ore/aop/RateLimiterAspect;Lorg/aspectj/lang/JoinPoint;Lcn/zszj/framework/ratelimiter/core/annotation/RateLimiter;)Lorg/
junit/jupiter/api/function/Executable;X >Redis ����Ӧ fail-open ���У����Ǿܾ����쳣����
 ?Z[\ assertDoesNotThrow @(Lorg/junit/jupiter/api/function/Executable;Ljava/lang/String;)V 
^_` stream ()Ljava/util/stream/Stream; bcd apply ()Ljava/util/function/Function;fghij java/
util/stream/Stream map 8(Ljava/util/function/Function;)Ljava/util/stream/Stream;l 

nopqr java/util/stream/Collectors joining 6(Ljava/lang/CharSequence;)Ljava/util/stream/Collector;ftuv
 collect 0(Ljava/util/stream/Collector;)Ljava/lang/Object;x 	fail-openz 8Redis ����Ӧ��¼ fail-open �澯�Ա���ά��֪
| username~ password? token? apiKey? note? nested1? secret? ? 5(Ljava/lang/Object;
Ljava/lang/Object;)Ljava/util/Map; SECRET_PASSWORD Ljava/lang/String; 
ConstantValue SECRET_TOKEN 
SECRET_APIKEY SECRET_LIST SECRET_CODE 
SAFE_USERNAME 	SAFE_NOTE METHOD_DESC RuntimeVisibleAnnotations Lorg/mockito/Mock; 	Signature SLch/qos/logback/
core/read/ListAppender<Lch/qos/logback/classic/spi/ILoggingEvent;>; Code LineNumberTable setUp "Lorg/junit/jupit
er/api/BeforeEach; tearDown !Lorg/junit/jupiter/api/AfterEach; 5testBeforePointCut_tooManyRequests_argsSanitizedInL
og Lorg/junit/jupiter/api/Test; ;testBeforePointCut_tooManyRequests_scalarRefreshTokenMasked ;testBeforePointCut_to
oManyRequests_smsCodeMaskedViaMaskKeys &testBeforePointCut_allowed_noRejectLog 4testBeforePointCut_redisDown_shouldFa
ilOpenByDefault 
StackMapTable 7()Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>; =lambda$testBeforePointCut_redisDown_shouldFai
lOpenByDefault$3 ?(Lcn/zszj/framework/ratelimiter/core/aop/RateLimiterAspect;Lorg/aspectj/lang/JoinPoint;Lcn/zszj/fram
ework/ratelimiter/core/annotation/RateLimiter;)V 
Exceptions Dlambda$testBeforePointCut_tooManyRequests_smsCodeMaskedViaMaskKeys$2 Dlambda$testBeforePointCut_tooManyRe
quests_scalarRefreshTokenMasked$1 >lambda$testBeforePointCut_tooManyRequests_argsSanitizedInLog$0 
SourceFile RateLimiterAspectTest.java ,Lorg/junit/jupiter/api/extension/ExtendWith; value ,Lorg/mockito/junit/jup
iter/MockitoExtension; NestMembers BootstrapMethods?
????? "java/lang/invoke/LambdaMetafactory metafactory ?(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/St
ring;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodT
ype;)Ljava/lang/invoke/CallSite; ?
 ??.?
 ??.?
 ??.?
 ??? [	????? ? (ch/qos/logback/classic/spi/ILoggingEvent getFormattedMessage? >(Lch/qos/logback/cl
assic/spi/ILoggingEvent;)Ljava/lang/String; InnerClasses FixedKeyResolver FixedSignature? %java/lang/invoke/Me
thodHandles$Lookup? java/lang/invoke/MethodHandles Lookup !      ?? ?    ? ?? ?    ? ?? ?   
 ? ?? ?    ? ?? ?   $ ?? ?    ? ?? ?    ? ?? ?    ?    ?    ?         : ; 
?   ?  ) *       ?        *? ?   ?       5 ?  ?   }     M*? Y? 	Y? ? *? ? ? *? ? %? '
*? '? +? 1*? 5Y? 7? 8*? 8? <*? '*? 8? ??   ?       I  J % K / L : M A N L O?    ?   ?  ?   4     *? '*
? 8? CW*? 8? G?   ?       S  T  U?    ?   ?  ?  ?    OJ? L? JL	? R+? V ? J? \ W+? ` ? d? jd? d
? m W+? r ? d? j? d? m W+? u ? j? y? m W+?  ? j?? m W?? L? ?M,? ? ? j? ?Y?? ?? m W,? ? ? j? Y? ?S? m
 W*? ? ?? ?? ?? ?? z? ?? j? ?? m W?*,+? ?  ? ?? ?N? ?? ?-? ? ?*? ?:? ?? ?? ?? ?? ?? ?? ?? ?? ?
? ?? ?? ??? ??? ??? ??? ?   ?   Z    Z 	 [  \ 4 ] J ^ \ _ m a v b ? c ? e ? h ? j ? m ? n ? o p q
 r* s6 tB uN v?    ?   ?  ?  o    J? L? JL	? R+? V ? J? \ W+? ` ? d? j? d? m W+? r ? d? j<? 
d? m W+? u ? j? y? m W+?  ? j?? m W?? L? ?M?? L? ?N? ?-? ? ? ?Y	S? m W,? ? ? j-? m W,? ? ? j? 
Y?S? m W*? ? ?? ?? ?? ?? z? ?? j? ?? m W?*,+?  ? ?W*? ?:? ?? ??? ?? ?   ?   F     	 ?  ? 3 ?
 J ? \ ? m ? v ?  ? ? ? ? ? ? ? ? ? ? ? ? ?	 ? ??    ?   ?  ?  ?    [J? L? JL	? R+? V ? J? \ W+? `
 ? d? j? d? m W+? r ? d? j<? d? m W+? u ? j? y? m W+?  ? j?? m W? ?+? ? ? ?YS? m W?? L? ?M?? 
L? ?N? ?-? ? ? ?YS? m W,? ? ? j-? m W?Y?:? W$? W,? ? ? j? YS? m W*? ? ?
? ?? ?? ?? z? ?? j? ?? m W?*,+?&  ? ?W*? ?:$? ?'? ??? ?)? ?   ?   V    ? 	 ?  ? 3 ? J ? \ ? m ? ? ?
 ? ? ? ? ? ? ? ? ? ? ? ? ? ? ?+ ?9 ?? ?M ?Z ??    ?   ?  ?   ?     eJ? L? JL	? R+? V ? J? \ W?? 
L? ?M*? ? ?? ?? ?? ?? z? ?? j? ?? m W*? ,+?+*? 8?/?3 7? ?   ?       ? 	 ?  ? & ? I ? R ? d ??    ?  
 ?  ?      U9? L?9L+? ??; ? j??Y?AYC?ES?F W? ?Y+?JM? Y? 	Y? ? ,? N?? ? %:?M:?Q? 
1? 5Y? 7:? <? ?J? L? J:	? R? V ? J? \ W? ` ? d? j? d? m W? r ? d? j<? d? m W? u ? j? y? m W
?? L? ?:-?T  W?Y?/?] ?a  ?e k?m?s ? ?:		w? ?y? ?? CW? G? 1? :
? CW? G? 1
??  w$;  ;=;   ?   z    ? 
 ? - ? 6 ? I ? S ? Z ? b ? k ? p ? w ? ? ? ? ? ? ? ? ? ? ? ? ? ? ? ?	 ? ?$ ?, ?1 ?8 ?; ?E ?J ?Q ?T ??   
" ?;  9 ?  % , 5 ??    ?   
 ? ? ?   ?     n?Y?K*{? W*}? W*? W?Y?L+?? W+?? W*?+? W*????? ?
 W*?   ?   * 
   ?  ?  ?   ? , ? 4 ? @ ? L ? W ? l ??   ?  ? ? ?   I     %*? 8?/?] ?a  ?e k?m?s ? ?   ?     
  ?  ?   $ ?
?? ?        *+,?+?   ?       ??    ??. ?   "     
*? +,?+?   ?       ??    ??. ?   "     
*? +,?+?   ?       ??    ??. ?   "     
*? +,?+?   ?       i?    ? ?   ??    ? ?[ c??     ? 	?   4 ? ???? ???? ???? 
???? ????     	 ?  ? ? ??? 
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\test-classes\cn
\zszj\framework\ratelimiter\core\keyresolver\impl\ExpressionRateLimiterKeyResolverTest$FixedMethodSignature.class
????   = N
      java/lang/Object <init> ()V	  	 
   mcn/zszj/framework/ratelimiter/core/keyresolver/impl/ExpressionRateLimiterKeyResolverTest$FixedMethodSignature 
method Ljava/lang/reflect/Method;
      java/lang/reflect/Method 
getReturnType ()Ljava/lang/Class;
     getParameterTypes ()[Ljava/lang/Class;
     getExceptionTypes
     
getParameters  ()[Ljava/lang/reflect/Parameter;  java/lang/String
 ! " # $ % java/lang/reflect/Parameter getName ()Ljava/lang/String;
  ' ( % toString
  "
  + , - getModifiers ()I
  / 0  getDeclaringClass
 2 " 3 java/lang/Class 5 (org/aspectj/lang/reflect/MethodSignature (Ljava/lang/reflect/Method;)V Code LineNu
mberTable MethodParameters 	getMethod ()Ljava/lang/reflect/Method; getParameterNames ()[Ljava/lang/String; 
StackMapTable @ [Ljava/lang/reflect/Parameter; B [Ljava/lang/String; 
toShortString toLongString getDeclaringType getDeclaringTypeName 
SourceFile )ExpressionRateLimiterKeyResolverTest.java NestHost K Xcn/zszj/framework/ratelimiter/core/keyresolver/i
mpl/ExpressionRateLimiterKeyResolverTest InnerClasses FixedMethodSignature      4       
    6  7   *     
*? *+? ?    8       ?  ? 	 ? 9        : ;  7        *? ?    8       ?     7         *? ? 
?    8       ?     7         *? ? ?    8       ?     7         *? ? ?    8       ?  < =  7
   h     '*? ? L+?? M>+?? ,+2?  S????,?    8       ?  ?  ?  ?  ? % ? >    ?  ? A?   ( %  7 
        *? ? &?    8       ?  C %  7         *? ? &?    8       ?  D %  7         *? ? &?    8  
     ?  $ %  7         *? ? )?    8       ?  , -  7         *? ? *?    8       ?  E   7       
  *? ? .?    8       ?  F %  7   #     *? ? .? 1?    8       ?  G    H I    J L   
   J M 
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\test-classes\cn
\zszj\framework\ratelimiter\core\keyresolver\impl\ExpressionRateLimiterKeyResolverTest$Fixture.class
????   = 
      java/lang/Object <init> ()V  `cn/zszj/framework/ratelimiter/core/keyresolver/impl/ExpressionRateL
imiterKeyResolverTest$Fixture Code LineNumberTable send '(Ljava/lang/String;Ljava/lang/String;)V MethodParamet
ers mobile code other 
SourceFile )ExpressionRateLimiterKeyResolverTest.java NestHost  Xcn/zszj/framework/ratelimiter/core/keyresolver/i
mpl/ExpressionRateLimiterKeyResolverTest InnerClasses Fixture               	        *? ?    
       ?     	         ?    
       ? 
   	           	         ?    
       ? 
   	                     
     
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\test-classes\cn
\zszj\framework\ratelimiter\core\keyresolver\impl\ExpressionRateLimiterKeyResolverTest.class
????   = ?
      java/lang/Object <init> ()V  Tcn/zszj/framework/ratelimiter/core/keyresolver/impl/ExpressionRateL
imiterKeyResolver
  	   
   Xcn/zszj/framework/ratelimiter/core/keyresolver/impl/ExpressionRateLimiterKeyResolverTest resolver VLcn/zszj/
framework/ratelimiter/core/keyresolver/impl/ExpressionRateLimiterKeyResolver;  2cn/zszj/framework/common/util/servle
t/ServletUtils
      org/mockito/Mockito 
mockStatic -(Ljava/lang/Class;)Lorg/mockito/MockedStatic;  send  13800138000  1234
     ! 
mockJoinPoint C(Ljava/lang/String;[Ljava/lang/Object;)Lorg/aspectj/lang/JoinPoint; # #mobile
  % & ' mockRateLimiter O(Ljava/lang/String;)Lcn/zszj/framework/ratelimiter/core/annotation/RateLimiter;   ) * +
 apply )()Lorg/mockito/MockedStatic$Verification; - . / 0 1 org/mockito/MockedStatic when O(Lorg/mockito/Moc
kedStatic$Verification;)Lorg/mockito/stubbing/OngoingStubbing; 3 1
  5 6 7 requestWithTenant I(Ljava/lang/String;)Lorg/springframework/mock/web/MockHttpServletRequest; 9 : ; < =
 $org/mockito/stubbing/OngoingStubbing 
thenReturn :(Ljava/lang/Object;)Lorg/mockito/stubbing/OngoingStubbing;
  ?  @ k(Lorg/aspectj/lang/JoinPoint;Lcn/zszj/framework/ratelimiter/core/annotation/RateLimiter;)Ljava/lang/String;
 B 2 D $��ͬ�⻧���ù����������
 F G H I J  org/junit/jupiter/api/Assertions assertNotEquals 9(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Str
ing;)V - L M  close O java/lang/Throwable
 N Q R S 
addSuppressed (Ljava/lang/Throwable;)V U other W ,��ͬ�˵�(����)���ù���������� Y 1111 [ 2222 ] O����֤�����
ͨ�������ø��� Key���̶������������ɹ�ܣ�
 F _ ` J assertEquals b :Key Ӧ������+�⻧������(MD5)����������ԭֵ  d e f get ?(Lcn/zszj/framework/ratelimiter/c
ore/keyresolver/impl/ExpressionRateLimiterKeyResolverTest;)Lorg/junit/jupiter/api/function/ThrowingSupplier;
 F h i j assertDoesNotThrow E(Lorg/junit/jupiter/api/function/ThrowingSupplier;)Ljava/lang/Object; l java/lang/S
tring n +������������ʱ��Ӧ�����ǿ� Key
 F p q r 
assertNotNull '(Ljava/lang/Object;Ljava/lang/String;)V t `cn/zszj/framework/ratelimiter/core/keyresolver/impl/Expres
sionRateLimiterKeyResolverTest$Fixture v java/lang/Class
 u x y z 	getMethod @(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method; | java/lang/NoSuchMethodExc
eption ~ java/lang/IllegalStateException
 } ?  S ? mcn/zszj/framework/ratelimiter/core/keyresolver/impl/ExpressionRateLimiterKeyResolverTest$FixedMethodSign
ature
 ? ?  ? (Ljava/lang/reflect/Method;)V ? org/aspectj/lang/JoinPoint
  ? ? ? mock %(Ljava/lang/Class;)Ljava/lang/Object; ? ? ? ? getSignature ()Lorg/aspectj/lang/Signature;
  ? 0 = ? ? ? ? getArgs ()[Ljava/lang/Object; ? 9cn/zszj/framework/ratelimiter/core/annotation/RateLimiter ?
 ? ? ? keyArg ()Ljava/lang/String; ? 3org/springframework/mock/web/MockHttpServletRequest
 ?  ? 1cn/zszj/framework/web/core/util/WebFrameworkUtils ? 	tenant-id
 ? ? ? ? 	addHeader '(Ljava/lang/String;Ljava/lang/Object;)V MOBILE Ljava/lang/String; 
ConstantValue Code LineNumberTable %resolver_shouldScopeByMethodAndTenant 
StackMapTable RuntimeVisibleAnnotations Lorg/junit/jupiter/api/Test; 3resolver_sameSubjectDifferentArg_shouldNotCh
angeKey ,resolver_whenNoRequestContext_shouldNotCrash ? java/lang/reflect/Method MethodParameters 
methodName args tenantId 5lambda$resolver_whenNoRequestContext_shouldNotCrash$0 
Exceptions 
SourceFile )ExpressionRateLimiterKeyResolverTest.java NestMembers BootstrapMethods ?
 ? ? ? ? ? "java/lang/invoke/LambdaMetafactory metafactory ?(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/St
ring;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodT
ype;)Ljava/lang/invoke/CallSite;  ?
  ? ? ? 
getRequest +()Ljakarta/servlet/http/HttpServletRequest; ? ()Ljava/lang/Object; ?
  ? ? ? ? InnerClasses ? %org/mockito/MockedStatic$Verification Verification Fixture FixedMethodSignature
 ? %java/lang/invoke/MethodHandles$Lookup ? java/lang/invoke/MethodHandles Lookup !       ? ?  ?       
        ?   ,     *? *? Y? 	? 
?    ?   
    $  (  ?   ?  ?    ? L*? YSYS? M*"? $N+? (  ? , *2? 4? 8 W*? 
,-? >:+? (  ? , *A? 4? 8 W*? 
,-? >:C? E+? "+? K ? M+? +? K ? 	N,-? P,?? L+? (  ? , *2? 4? 8 W*? 
*? YSYS? *"? $? >M*? 
*T? YSYS? *"? $? >N,-V? E+? "+? K ? M+? +? K ? 	N,-? P,??   o | N ? ? ? N ? ? N N  ?   J   
 .  0  1 " 3 9 4 D 6 [ 7 f 9 o : | . ? < ? > ? ? ? @ ? B ? C < D ?   P ? |   -  N? 
   - N  N? ? t   -  N? 
   - N  N?  ?     ?    ?   ?       ?? L+? (  ? , *2? 4? 8 W*? 
*? YSYXS? *"? $? >M*? 
*? YSYZS? *"? $? >N,-\? ^,a? E+? "+? K ? M+? +? K ? 	N,-? P,??   p } N ? ? ? N  ?   & 	   H  I 
 L ? M a O h Q p R } H ? S ?   ) ? }   -  N? 
   - N  N?  ?     ?    ?   ?   ?     O? L+? (  ? , ? 8 W*? c  ? g? kM,m? o+? "+? K ? M+? +? K
 ? 	N,-? P,??   + 8 N = C F N  ?       W  Y  [ % ^ + _ 8 W N ` ?   ) ? 8   -  N? 
   - N  N?  ?     ?   ?   !  ?   ?     ^s+? uYkSYkS? wN? :? }Y? ?? ?Y-? ?:?? ?? ?:? ? ? 
?? 8 W? ? ? ?,? 8 W?      {  ?   & 	   g  j  h  i $ k . l 8 m J n [ o ?    X {?  ? ?   	 ?   ?   
 & '  ?   ;     ?? ?? ?M,? ? ? ?+? 8 W,?    ?       s 	 t  u ?    ?    6 7  ?   G     ? ?Y? ?M+? 
,?+? ?,?    ?       y  z  {  } ?    ?  ? ?    ?   ? ?  ?   :     "*? 
*? YSYXS? *"? $? >?    ?       \ ?     N  ?    ? ?     ? s ?     ?  ? ? ? ?  ? ? ? ?   "  ? - 
?	 s  ?  ?  ?  ? ? ? 
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\test-classes\cn
\zszj\framework\signature\core\ApiSignatureTest.class
????   = ?
      java/lang/Object <init> ()V
  	 
   java/lang/System currentTimeMillis ()J
      java/lang/Long valueOf (J)Ljava/lang/Long;
      cn/hutool/core/util/IdUtil 
randomUUID ()Ljava/lang/String;  xxxxxx  yyyyyy       makeConcatWithConstants 6(Ljava/lang/String;Ljav
a/lang/Long;)Ljava/lang/String;
 " # $ % & "cn/hutool/crypto/digest/DigestUtil 	sha256Hex &(Ljava/lang/String;)Ljava/lang/String; ( 8cn/zszj/fra
mework/signature/core/annotation/ApiSignature
 * + , - . org/mockito/Mockito mock %(Ljava/lang/Class;)Ljava/lang/Object; ' 0 1  appId
 * 3 4 5 when :(Ljava/lang/Object;)Lorg/mockito/stubbing/OngoingStubbing; 1 8 9 : ; 5 $org/mockito/stubbing/On
goingStubbing 
thenReturn ' = >  	timestamp > ' A B  nonce B ' E F  sign F ' I J K timeout ()I
 M N O  P java/lang/Integer (I)Ljava/lang/Integer; ' R S T timeUnit !()Ljava/util/concurrent/TimeUnit;	 V W
 X Y Z java/util/concurrent/TimeUnit SECONDS Ljava/util/concurrent/TimeUnit; \ 'jakarta/servlet/http/HttpServ
letRequest
 ^ _ ` a b org/mockito/ArgumentMatchers eq &(Ljava/lang/Object;)Ljava/lang/Object; d java/lang/String [ f g
 & 	getHeader
 c i  j &(Ljava/lang/Object;)Ljava/lang/String; [ l m n getParameterMap ()Ljava/util/Map;
 p q r s t cn/hutool/core/map/MapUtil builder !()Lcn/hutool/core/map/MapBuilder; v v1 x k1
 z { | } ~ cn/hutool/core/map/MapBuilder put E(Ljava/lang/Object;Ljava/lang/Object;)Lcn/hutool/core/map/MapBuild
er;
 z ? ? n build [ ? ?  getContentType ? application/json [ ? ? ? 	getReader ()Ljava/io/BufferedReader; 
? java/io/BufferedReader ? java/io/StringReader ? test
 ? ?  ? (Ljava/lang/String;)V
 ? ?  ? (Ljava/io/Reader;)V	 ? ? ? ? ? 1cn/zszj/framework/signature/core/ApiSignatureTest signatureRedisDAO =
Lcn/zszj/framework/signature/core/redis/ApiSignatureRedisDAO;
 ? ? ? ? & ;cn/zszj/framework/signature/core/redis/ApiSignatureRedisDAO getAppSecret
 ^ ? a ? (I)I
 ? ? ? ? setNonce Y(Ljava/lang/String;Ljava/lang/String;ILjava/util/concurrent/TimeUnit;)Ljava/lang/Boolean;
 ? ? ?  ? java/lang/Boolean (Z)Ljava/lang/Boolean;	 ? ? ? ? apiSignatureAspect 9Lcn/zszj/framework/signature
/core/aop/ApiSignatureAspect;
 ? ? ? ? ? 7cn/zszj/framework/signature/core/aop/ApiSignatureAspect verifySignature f(Lcn/zszj/framework/signatur
e/core/annotation/ApiSignature;Ljakarta/servlet/http/HttpServletRequest;)Z
 ? ? ? ? ?  org/junit/jupiter/api/Assertions 
assertTrue (Z)V RuntimeVisibleAnnotations Lorg/mockito/InjectMocks; Lorg/mockito/Mock; Code LineNumberTable
 testSignatureGet 
Exceptions ? java/io/IOException Lorg/junit/jupiter/api/Test; 
SourceFile ApiSignatureTest.java ,Lorg/junit/jupiter/api/extension/ExtendWith; value ,Lorg/mockito/junit/jupiter/
MockitoExtension; BootstrapMethods ?
 ? ? ?  ? $java/lang/invoke/StringConcatFactory ?(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/l
ang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite; ? 5k1=v1&v1=k1testappId=xxxxx
x&nonce=&timestamp=yyyyyy InnerClasses ? %java/lang/invoke/MethodHandles$Lookup ? java/lang/invoke/MethodHandl
es Lookup ! ?      ? ?  ?     ?    ? ?  ?     ?        ?        *? ?    ?         ?   ?  
U  
  ? ? 
L? MN:,+?   :? !:'? )? ':? / ? 26? 7 W? < ? 2?? 7 W? @ ? 2C? 7 W? D ? 2G? 7 W? H ? 
L? 2<? L? 7 W? Q ? 2? U? 7 W[? )? [:6? ]? c? e ? 2-? 7 W?? ]? c? e ? 2+? h? 7 WC? ]? c? e ? 2,? 7
 WG? ]? c? e ? 2? 7 W? k ? 2? ou? cYwS? yw? cYuS? y? ? 7 W? ? ? 2?? 7 W? ? ? 2? ?Y? ?Y?? 
?? ?? 7 W*? ?-? ]? c? ?? 2? 7 W*? ?-? ]? c,? ]? cx? ?? U? ]? V? ?? 2? ?? 7 W*? ?? ?6		? ??    ?   r    ( 
 )  *  +  ,  - " 0 , 1 > 2 P 3 b 4 t 5 ? 6 ? 7 ? 8 ? 9 ? : ? ; <) == <C >U ?u A? B? E? G? H ?     ? ?
     ?    ?    ? ?     ?  ?[ c ? ?     ?  ? ?   
  ? ? ? 
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\test-classes\cn
\zszj\framework\signature\core\aop\ApiSignatureAspectTest$FixedSignature.class
????   = &
      java/lang/Object <init> ()V	  	 
   Jcn/zszj/framework/signature/core/aop/ApiSignatureAspectTest$FixedSignature text Ljava/lang/String;  Obj
ect  org/aspectj/lang/Signature (Ljava/lang/String;)V Code LineNumberTable MethodParameters toString ()
Ljava/lang/String; 
toShortString toLongString getName getModifiers ()I getDeclaringType ()Ljava/lang/Class; getDeclaringType
Name 
SourceFile ApiSignatureAspectTest.java NestHost # ;cn/zszj/framework/signature/core/aop/ApiSignatureAspectTest 
InnerClasses FixedSignature                      *     
*? *+? ?           ?  ? 	 ?                    *? ?           ?             *? ?   
        ?             *? ?           ?             *? ?           ?             
?           ?             ?           ?             
?           ?        !    " $   
   " % 
E:\����֮��AI����ƽ̨����\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\target\test-classes\cn
\zszj\framework\signature\core\aop\ApiSignatureAspectTest.class
????   =8
      java/lang/Object <init> ()V  7cn/zszj/framework/signature/core/aop/ApiSignatureAspect	 
   
  ;cn/zszj/framework/signature/core/aop/ApiSignatureAspectTest signatureRedisDAO =Lcn/zszj/framework/signature/cor
e/redis/ApiSignatureRedisDAO;
     @(Lcn/zszj/framework/signature/core/redis/ApiSignatureRedisDAO;)V	 
    apiSignatureAspect 9Lcn/zszj/framework/signature/core/aop/ApiSignatureAspect;
      org/slf4j/LoggerFactory 	getLogger %(Ljava/lang/Class;)Lorg/slf4j/Logger;  ch/qos/logback/classic/
Logger	 
    ! aspectLogger Lch/qos/logback/classic/Logger;	 # $ % & ' ch/qos/logback/classic/Level INFO Lch/qos/l
ogback/classic/Level;
  ) * + setLevel !(Lch/qos/logback/classic/Level;)V - %ch/qos/logback/core/read/ListAppender
 , 	 
 0 1 2 listAppender 'Lch/qos/logback/core/read/ListAppender;
 , 4 5  start
  7 8 9 addAppender !(Lch/qos/logback/core/Appender;)V
  ; < = detachAppender !(Lch/qos/logback/core/Appender;)Z
 , ? @  stop
 B C D E  <org/springframework/web/context/request/RequestContextHolder resetRequestAttributes G 'jakarta/servl
et/http/HttpServletRequest
 I J K L M org/mockito/Mockito mock %(Ljava/lang/Class;)Ljava/lang/Object; O @org/springframework/web/context/
request/ServletRequestAttributes
 N Q  R ,(Ljakarta/servlet/http/HttpServletRequest;)V
 B T U V setRequestAttributes >(Lorg/springframework/web/context/request/RequestAttributes;)V X 8cn/zszj/framewor
k/signature/core/annotation/ApiSignature W Z [ \ appId ()Ljava/lang/String;
 I ^ _ ` when :(Ljava/lang/Object;)Lorg/mockito/stubbing/OngoingStubbing; [ c d e f ` $org/mockito/stubbing/On
goingStubbing 
thenReturn h org/aspectj/lang/JoinPoint g j k l getSignature ()Lorg/aspectj/lang/Signature; n Jcn/zszj/frame
work/signature/core/aop/ApiSignatureAspectTest$FixedSignature p  OpenApiController.queryOrder(..)
 m r  s (Ljava/lang/String;)V g u v w getArgs ()[Ljava/lang/Object;
 
 y z { buildSensitiveArgs ()Ljava/util/Map; } 3cn/zszj/framework/common/exception/ServiceException    ? ? e
xecute ?(Lcn/zszj/framework/signature/core/aop/ApiSignatureAspectTest;Lorg/aspectj/lang/JoinPoint;Lcn/zszj/framework/s
ignature/core/annotation/ApiSignature;)Lorg/junit/jupiter/api/function/Executable;
 ? ? ? ? ?  org/junit/jupiter/api/Assertions assertThrows S(Ljava/lang/Class;Lorg/junit/jupiter/api/function/Exec
utable;)Ljava/lang/Throwable;	 ? ? ? ? ? Acn/zszj/framework/common/exception/enums/GlobalErrorCodeConstants BAD_RE
QUEST .Lcn/zszj/framework/common/exception/ErrorCode;
 ? ? ? ? ? ,cn/zszj/framework/common/exception/ErrorCode getCode ()Ljava/lang/Integer;
 | ?
 ? ? ? ? assertEquals )(Ljava/lang/Integer;Ljava/lang/Integer;)V
 
 ? ? \ capturedLog ? P@ssw0rd-SECRET-DoNotLog
 ? ? ? ? ? java/lang/String contains (Ljava/lang/CharSequence;)Z ? $password ���ܲ�Ӧ��������־
 ? ? ? ? assertFalse (ZLjava/lang/String;)V ? TOKEN-SECRET-DoNotLog ? !token ���ܲ�Ӧ��������־ ? APIKEY-SECR
ET-DoNotLog ? )Ƕ�� apiKey ���ܲ�Ӧ��������־ ? 
zhangsan-user ? ������ username Ӧ����
 ? ? ? ? 
assertTrue ? 
hello-note ? ������ note Ӧ���� ? !��������Ӧ�����Ա㶨λ ? *** ? �����ֶ�Ӧ������ ? java/util/LinkedHashMap
 ?  ? username ? ? ? ? ? 
java/util/Map put 8(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object; ? password ? token ? apiKey ?
 note ? nested	 , ? ? ? list Ljava/util/List; ? ? ? ? ? java/util/List stream ()Ljava/util/stream/Str
eam;  ? ? ? apply ()Ljava/util/function/Function; ? ? ? ? ? java/util/stream/Stream map 8(Ljava/util/fun
ction/Function;)Ljava/util/stream/Stream; ? 

 ? ? ? ? ? java/util/stream/Collectors joining 6(Ljava/lang/CharSequence;)Ljava/util/stream/Collector; ? ? ? ?
 collect 0(Ljava/util/stream/Collector;)Ljava/lang/Object;
  ? ? ? beforePointCut Y(Lorg/aspectj/lang/JoinPoint;Lcn/zszj/framework/signature/core/annotation/ApiSignature;)V
 SECRET_PASSWORD Ljava/lang/String; 
ConstantValue SECRET_TOKEN 
SECRET_APIKEY 
SAFE_USERNAME 	SAFE_NOTE METHOD_DESC RuntimeVisibleAnnotations Lorg/mockito/Mock; 	Signature SLch/qos/logback/
core/read/ListAppender<Lch/qos/logback/classic/spi/ILoggingEvent;>; Code LineNumberTable setUp "Lorg/junit/jupit
er/api/BeforeEach; tearDown !Lorg/junit/jupiter/api/AfterEach; 5testBeforePointCut_signatureFailed_argsSanitizedInL
og Lorg/junit/jupiter/api/Test; 7()Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>; >lambda$testBeforePointCut
_signatureFailed_argsSanitizedInLog$0 
Exceptions java/lang/Throwable 
SourceFile ApiSignatureAspectTest.java ,Lorg/junit/jupiter/api/extension/ExtendWith; value ,Lorg/mockito/junit/ju
piter/MockitoExtension; NestMembers BootstrapMethods
 !" "java/lang/invoke/LambdaMetafactory metafactory ?(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/St
ring;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodT
ype;)Ljava/lang/invoke/CallSite; %
 
& ?( &(Ljava/lang/Object;)Ljava/lang/Object;	*+,-. \ (ch/qos/logback/classic/spi/ILoggingEvent getFo
rmattedMessage0 >(Lch/qos/logback/classic/spi/ILoggingEvent;)Ljava/lang/String; InnerClasses FixedSignature4 
%java/lang/invoke/MethodHandles$Lookup6 java/lang/invoke/MethodHandles Lookup ! 
    
  ? ?  ?    ?  ? ?  ?    ?   ?  ?    ?  ?  ?    ?  ?  ?    ?  ?  ?    o  
               1 2        !               *? ?   	       + 
     s     C*? Y*? 	? ? *? ? ? *? ? "? (*? ,Y? .? /*? /? 3*? *? /? 6?   	       =  >  ? % @ 0 A 7
 B B C            ;     *? *? /? :W*? /? >? A?   	       G  H  I  J    
       ?     ?F? H? FL? NY+? P? SW? H? WM,? Y ? ]a? b Wg? H? gN-? i ? ]? mYo? q? b W-? t ? ]? Y?
 xS? b W|*-,? ~  ? ?? |:? ?? ?? ?? ?*? ?:?? ??? ??? ??? ??? ??? ??? ??? ??? ??? ?o? ??? ?
?? ??? ??   	   J    O 	 P  R  S . U 7 V O W h Z z \ ? _ ? ` ? a ? b ? c ? d ? e ? f ? g       
 z {    ?     S? ?Y? ?K*??? ? W*??? ? W*??? ? W? ?Y? ?L+??? ? W+??? ? W*?+? ? W*?   	   & 	   j
  k  l  m ) n 1 o < p G q Q r     ? \    H     $*? /? ? ? ? ?  ? ? ? ? ? ? ??   	       v  w  x
 # v ?    "     
*? +,? ??   	       [             [ c     m     #$# ')/1     m 
2 357 

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "git ls-files '*AGENTS*' '*CLAUDE*'; Get-Chil
dItem -Path services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection -Recurse -Force -File | Where-Obj
ect { "'$_.Name -eq '"'AGENTS.md' -or "'$_.Name -eq '"'AGENTS.override.md' } | Select-Object -ExpandProperty FullName; 
git show b62af7d3:services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/frame
work/idempotent/core/aop/IdempotentAspect.java; git show b62af7d3:services/zhongshu-core/zszj-common/src/main/java/cn/z
szj/framework/common/util/string/StrUtils.java; git ls-files '*LogSanitizeUtils.java' '*DefaultIdempotentKeyResolver.ja
va' '*IdempotentRedisDAO.java'; Get-Command java,jshell,mvn,python -ErrorAction SilentlyContinue | Select-Object Name,S
ource" in E:\众墅之家AI赋能平台底座
 exited 1 in 202ms:
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 拦截声明了 {@link Idempotent} 注解的方法，实现幂等操作
 *
 * ZS-SEC-011.A：增加同键异参冲突检测，区分"重复请求"与"幂等键冲突"两种拒绝原因
 *
 * ZS-SEC-011.A 边界声明：本幂等能力是「窗口锁短时防重」（基于 Redis TTL 窗口），
 * 不是持久化幂等——进程重启、窗口超时、Redis 缓存故障后，不保证返回原业务结果，
 * 仅保证窗口内同键同参不重复执行；「同键异参冲突拒绝」仅在 Key 不含入参的解析器（ExpressionIdempotentKeyResolver）下生效——
 * DefaultIdempotentKeyResolver/UserIdempotentKeyResolver 已把 argsStr 烘入 Key，同键必然同参，conflict 恒为 false（构造上不可达）。
 * 让冲突检测对默认路径也有意义的 Key/Value 职责切分（Key 用业务幂等号、Value 存全量入参摘要）归 ZS-SEC-011.B。
 * 持久化幂等（落库 + HTTP 重试联验）归 ZS-SEC-011.B（B05 批次）范围。
 * 获准的重试仍会重新走完整鉴权链（幂等不替代鉴权）。
 *
 * ZS-SEC-011.A 已知缺口（本批 spec §4 文件清单仅含 DefaultIdempotentKeyResolver，以下登记不留白，挂后续任务）：
 * - [IMP-4 → REC-1/SEC-011.B] ExpressionIdempotentKeyResolver（Key=裸 SpEL 值，无 method/tenant/user 作用域）与
 *   UserIdempotentKeyResolver（有 user 无 tenant）仍缺租户隔离，与 SEC-010 已给 ExpressionRateLimiterKeyResolver 加租户作用域的先例不一致；
 *   且冲突检测唯一生效路径恰是 Expression 解析器 → 存在跨租户撞键 + 存在性侧信道风险。统一 SubjectScope 抽取归 REC-1（SEC-010+011.A 合并后跟进）。
 * - [IMP-5 → REC-1] 租户因子取自可选请求头 tenant-id（WebFrameworkUtils.getTenantId），非权威 TenantContextHolder（避免 protection 反依赖 biz
-tenant，控制器授权偏离）；
 *   省略头会产生不同 Key → 同一主体自绕过防重窗口（非跨主体：userId/userType 取自权威 request attribute 不可伪造，tenant-id 头受 TenantSecurityWebFilter 校
验不可伪造成他人租户）。
 *   权威租户源的可注入 port 归 REC-1。与 SEC-010 同性质。
 *
 * @author 芋道源码
 */
@Aspect
@Slf4j
public class IdempotentAspect {

    /**
     * MIN-3：同键异参冲突拒绝文案，提为常量便于统一维护与测试对齐
     */
    private static final String CONFLICT_MESSAGE = "幂等键冲突：相同幂等键携带了不同请求内容";

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

        // ZS-SEC-011.A：计算参数摘要，用于同键异参冲突检测
        // IMP-6：摘要输入改为「脱敏后」入参，避免未脱敏原文（含密码/令牌）MD5 后落 Redis value 被离线爆破（SEC-007 在 Redis 侧的对称缺口）；
        //        敏感字段差异被视为同参，对幂等语义无害
        // codex r0 P1：摘要「每请求必算」（含首次放行），须先经 serializableArgs 排除 servlet/spring-web 基础设施入参——
        //        LogSanitizeUtils 内部用 Jackson valueToTree 序列化会调用全部 getter，对 HttpServletResponse 触发 getWriter()，
        //        提前选定响应字符输出模式，破坏后续 ServletUtils.writeAttachment 等二进制输出（抛 IllegalStateException）
        String argsDigest = SecureUtil.md5(LogSanitizeUtils.sanitizeArgs(serializableArgs(joinPoint.getArgs())));

        // 1. 锁定 Key（携带参数摘要）
        // MIN-2：DAO 返回 Boolean，pipeline/transaction 场景可能返回 null，用 Boolean.TRUE.equals 防拆箱 NPE（null 视为未拿到锁 → 拒绝，fail-cl
osed）
        boolean success = Boolean.TRUE.equals(idempotentRedisDAO.setIfAbsent(key, argsDigest, idempotent.timeout(), ide
mpotent.timeUnit()));
        // 锁定失败，区分冲突与重复
        if (!success) {
            // IMP-2：getDigest 是「已决策拒绝」后的额外 GET，防 Redis 抖动把干净的 900 升级为 500；异常降级 null（digest 只影响文案，零语义损失）
            String storedDigest;
            try {
                storedDigest = idempotentRedisDAO.getDigest(key);
            } catch (Exception ex) {
                log.warn("[aroundPointCut][幂等键({}) 读回摘要失败，降级按重复处理]", key, ex);
                storedDigest = null;
            }
            // IMP-3：StrUtil.isNotEmpty 兼容滚动升级期旧格式空串值（旧实现存 ""），避免误判冲突
            // IMP-1：默认路径（Default/User 解析器）Key 已烘入 argsStr，同键必然同参，conflict 恒 false（构造上不可达）；冲突分支仅对 Expression 解析器等 Key 不
含入参的解析器生效
            boolean conflict = StrUtil.isNotEmpty(storedDigest) && !storedDigest.equals(argsDigest);
            // MIN-4：冲突通常是客户端 bug/探测（尤其 Expression 解析器下可能跨租户撞键），用 warn 提升安全信号；重复是常态（如双击），用 info
            if (conflict) {
                log.warn("[aroundPointCut][方法({}) 参数({}) 同键异参冲突]",
                        joinPoint.getSignature(), LogSanitizeUtils.sanitizeArgs(serializableArgs(joinPoint.getArgs())))
;
            } else {
                log.info("[aroundPointCut][方法({}) 参数({}) 同键同参重复]",
                        joinPoint.getSignature(), LogSanitizeUtils.sanitizeArgs(serializableArgs(joinPoint.getArgs())))
;
            }
            String msg = conflict ? CONFLICT_MESSAGE : idempotent.message();
            throw new ServiceException(GlobalErrorCodeConstants.REPEATED_REQUESTS.getCode(), msg);
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

    /**
     * 排除无法安全序列化的入参（servlet / spring-web 基础设施对象），再交给 {@link LogSanitizeUtils#sanitizeArgs} 计算摘要 / 脱敏日志。
     *
     * 背景（codex r0 P1）：SEC-011.A 把参数摘要从「仅拒绝分支」提升为「每请求必算」（含首次放行）。
     * {@link LogSanitizeUtils} 内部用 Jackson {@code valueToTree} 序列化入参，会调用对象全部 getter；
     * 对 {@code HttpServletResponse}（Tomcat ResponseFacade）会触发 {@code getWriter()}，提前选定响应字符输出模式，
     * 导致后续 {@code ServletUtils.writeAttachment} 等二进制输出抛 {@link IllegalStateException}。
     *
     * 排除口径与 {@link cn.zszj.framework.common.util.string.StrUtils#joinMethodArgs} 保持一致
     * （默认 Key 解析器早已按此排除 servlet 对象，此处对齐摘要 / 日志路径，避免同一批入参两条口径不一致）。
     *
     * @param args 原始方法入参
     * @return 剔除 servlet / spring-web 对象后的入参（保持原有相对顺序，null 元素保留）
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
            String clazzName = arg.getClass().getName();
            if (StrUtil.startWithAny(clazzName, "javax.servlet", "jakarta.servlet", "org.springframework.web")) {
                continue;
            }
            kept.add(arg);
        }
        return kept.toArray();
    }

}
fatal: path 'services/zhongshu-core/zszj-common/src/main/java/cn/zszj/framework/common/util/string/StrUtils.java' does 
not exist in 'b62af7d3'
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framework/common/util/log/LogSanitizeUtils.java
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/idempotent/co
re/keyresolver/impl/DefaultIdempotentKeyResolver.java
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/idempotent/co
re/redis/IdempotentRedisDAO.java

Name       Source                               
----       ------                               
python.exe C:\Program Files\Python311\python.exe



 exited 1 in 41123ms:
commit b62af7d3aa9de00e7f78d69de7d8a07e29875ae2
Author:     hlong026 <peihr85@gmail.com>
AuthorDate: Sat Sep 12 13:52:47 2026 +0800
Commit:     hlong026 <peihr85@gmail.com>
CommitDate: Sat Sep 12 13:52:47 2026 +0800

    fix(sec-011-a): 幂等摘要排除 servlet 入参，堵首次放行即序列化响应破坏二进制输出 [ZS-SEC-011.A]
    
    codex r0 P1：SEC-011.A 把参数摘要从「仅拒绝分支」提升为「每请求必算」（IdempotentAspect:76），含首次放行。
    LogSanitizeUtils 内部用 Jackson valueToTree 序列化入参会调用全部 getter，对 HttpServletResponse
    （Tomcat ResponseFacade）触发 getWriter()，提前选定响应字符输出模式，破坏后续 ServletUtils.writeAttachment
    等二进制输出（抛 IllegalStateException）。
    
    修复：加 serializableArgs helper，摘要与拒绝日志的入参先排除 servlet/spring-web 对象
    （javax.servlet/jakarta.servlet/org.springframework.web 前缀），排除口径与 StrUtils.joinMethodArgs
    （默认 Key 解析器算 Key 用）保持一致，消除同一批入参两条口径不一致。
    
    TDD：新增 testAroundPointCut_firstRequest_servletArgsExcludedFromDigest——mock HttpServletResponse +
    业务参数首次放行，断言 digest=md5(sanitizeArgs([业务参数])) 排除 servlet（RED 时含 servlet 序列化不匹配），
    且 verify(response,never()).getWriter()/getOutputStream() 钉死无副作用。
    protection 24→25 全绿，IdempotentAspectTest 7→8 全绿，上游 common43/web37/security18/mybatis13 全绿。
    
    延后 SEC-011.B：P2-1（sanitizeArgs 2048 截断致等长异尾入参碰撞）、P2-2（line88 Redis 读回失败告警裸 key）。

diff --git a/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/
idempotent/core/aop/IdempotentAspect.java b/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/s
rc/main/java/cn/zszj/framework/idempotent/core/aop/IdempotentAspect.java
index 2b14db17..a1ce4531 100644
--- a/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/idempot
ent/core/aop/IdempotentAspect.java
+++ b/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/idempot
ent/core/aop/IdempotentAspect.java
@@ -15,6 +15,7 @@ import org.aspectj.lang.annotation.Around;
 import org.aspectj.lang.annotation.Aspect;
 import org.springframework.util.Assert;
 
+import java.util.ArrayList;
 import java.util.List;
 import java.util.Map;
 
@@ -73,7 +74,10 @@ public class IdempotentAspect {
         // ZS-SEC-011.A：计算参数摘要，用于同键异参冲突检测
         // IMP-6：摘要输入改为「脱敏后」入参，避免未脱敏原文（含密码/令牌）MD5 后落 Redis value 被离线爆破（SEC-007 在 Redis 侧的对称缺口）；
         //        敏感字段差异被视为同参，对幂等语义无害
-        String argsDigest = SecureUtil.md5(LogSanitizeUtils.sanitizeArgs(joinPoint.getArgs()));
+        // codex r0 P1：摘要「每请求必算」（含首次放行），须先经 serializableArgs 排除 servlet/spring-web 基础设施入参——
+        //        LogSanitizeUtils 内部用 Jackson valueToTree 序列化会调用全部 getter，对 HttpServletResponse 触发 getWriter()，
+        //        提前选定响应字符输出模式，破坏后续 ServletUtils.writeAttachment 等二进制输出（抛 IllegalStateException）
+        String argsDigest = SecureUtil.md5(LogSanitizeUtils.sanitizeArgs(serializableArgs(joinPoint.getArgs())));
 
         // 1. 锁定 Key（携带参数摘要）
         // MIN-2：DAO 返回 Boolean，pipeline/transaction 场景可能返回 null，用 Boolean.TRUE.equals 防拆箱 NPE（null 视为未拿到锁 → 拒绝，fail-c
losed）
@@ -94,10 +98,10 @@ public class IdempotentAspect {
             // MIN-4：冲突通常是客户端 bug/探测（尤其 Expression 解析器下可能跨租户撞键），用 warn 提升安全信号；重复是常态（如双击），用 info
             if (conflict) {
                 log.warn("[aroundPointCut][方法({}) 参数({}) 同键异参冲突]",
-                        joinPoint.getSignature(), LogSanitizeUtils.sanitizeArgs(joinPoint.getArgs()));
+                        joinPoint.getSignature(), LogSanitizeUtils.sanitizeArgs(serializableArgs(joinPoint.getArgs()))
);
             } else {
                 log.info("[aroundPointCut][方法({}) 参数({}) 同键同参重复]",
-                        joinPoint.getSignature(), LogSanitizeUtils.sanitizeArgs(joinPoint.getArgs()));
+                        joinPoint.getSignature(), LogSanitizeUtils.sanitizeArgs(serializableArgs(joinPoint.getArgs()))
);
             }
             String msg = conflict ? CONFLICT_MESSAGE : idempotent.message();
             throw new ServiceException(GlobalErrorCodeConstants.REPEATED_REQUESTS.getCode(), msg);
@@ -116,4 +120,37 @@ public class IdempotentAspect {
         }
     }
 
+    /**
+     * 排除无法安全序列化的入参（servlet / spring-web 基础设施对象），再交给 {@link LogSanitizeUtils#sanitizeArgs} 计算摘要 / 脱敏日志。
+     *
+     * 背景（codex r0 P1）：SEC-011.A 把参数摘要从「仅拒绝分支」提升为「每请求必算」（含首次放行）。
+     * {@link LogSanitizeUtils} 内部用 Jackson {@code valueToTree} 序列化入参，会调用对象全部 getter；
+     * 对 {@code HttpServletResponse}（Tomcat ResponseFacade）会触发 {@code getWriter()}，提前选定响应字符输出模式，
+     * 导致后续 {@code ServletUtils.writeAttachment} 等二进制输出抛 {@link IllegalStateException}。
+     *
+     * 排除口径与 {@link cn.zszj.framework.common.util.string.StrUtils#joinMethodArgs} 保持一致
+     * （默认 Key 解析器早已按此排除 servlet 对象，此处对齐摘要 / 日志路径，避免同一批入参两条口径不一致）。
+     *
+     * @param args 原始方法入参
+     * @return 剔除 servlet / spring-web 对象后的入参（保持原有相对顺序，null 元素保留）
+     */
+    private static Object[] serializableArgs(Object[] args) {
+        if (args == null || args.length == 0) {
+            return args;
+        }
+        List<Object> kept = new ArrayList<>(args.length);
+        for (Object arg : args) {
+            if (arg == null) {
+                kept.add(null);
+                continue;
+            }
+            String clazzName = arg.getClass().getName();
+            if (StrUtil.startWithAny(clazzName, "javax.servlet", "jakarta.servlet", "org.springframework.web")) {
+                continue;
+            }
+            kept.add(arg);
+        }
+        return kept.toArray();
+    }
+
 }
diff --git a/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/test/java/cn/zszj/framework/
idempotent/core/aop/IdempotentAspectTest.java b/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protecti
on/src/test/java/cn/zszj/framework/idempotent/core/aop/IdempotentAspectTest.java
index 9bd32dcc..82b0826f 100644
--- a/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/test/java/cn/zszj/framework/idempot
ent/core/aop/IdempotentAspectTest.java
+++ b/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/test/java/cn/zszj/framework/idempot
ent/core/aop/IdempotentAspectTest.java
@@ -15,6 +15,7 @@ import cn.zszj.framework.idempotent.core.keyresolver.impl.DefaultIdempotentKeyRe
 import cn.zszj.framework.idempotent.core.redis.IdempotentRedisDAO;
 import cn.zszj.framework.web.core.util.WebFrameworkUtils;
 import jakarta.servlet.http.HttpServletRequest;
+import jakarta.servlet.http.HttpServletResponse;
 import org.aspectj.lang.JoinPoint;
 import org.aspectj.lang.ProceedingJoinPoint;
 import org.aspectj.lang.Signature;
@@ -44,6 +45,7 @@ import static org.mockito.ArgumentMatchers.eq;
 import static org.mockito.Mockito.doReturn;
 import static org.mockito.Mockito.mock;
 import static org.mockito.Mockito.mockStatic;
+import static org.mockito.Mockito.never;
 import static org.mockito.Mockito.verify;
 import static org.mockito.Mockito.when;
 
@@ -308,6 +310,36 @@ public class IdempotentAspectTest {
         }
     }
 
+    @Test
+    public void testAroundPointCut_firstRequest_servletArgsExcludedFromDigest() throws Throwable {
+        // codex r0 P1 回归：@Idempotent 方法若含 servlet 基础设施入参（如 HttpServletResponse），
+        // 计算参数摘要时不得用 Jackson 序列化它——序列化会调用 getWriter()/getOutputStream() 等 getter，
+        // 提前「选定」响应输出模式，破坏后续二进制输出（如 ServletUtils.writeAttachment 附件下载抛 IllegalStateException），
+        // 且首次放行请求即触发（SEC-011.A 把 sanitizeArgs 从「仅拒绝分支」提升为「每请求必算摘要」）。
+        // 修复：摘要与拒绝日志的入参先排除 servlet/spring-web 对象（与 StrUtils.joinMethodArgs 既有排除口径一致）。
+        Idempotent idempotent = mock(Idempotent.class);
+        doReturn(FixedKeyResolver.class).when(idempotent).keyResolver();
+        when(idempotent.timeout()).thenReturn(5);
+        when(idempotent.timeUnit()).thenReturn(TimeUnit.SECONDS);
+        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
+        when(joinPoint.proceed()).thenReturn("OK");
+        HttpServletResponse response = mock(HttpServletResponse.class);
+        Object businessArg = "orderPayload";
+        // 入参含 servlet 响应对象 + 业务参数
+        when(joinPoint.getArgs()).thenReturn(new Object[]{businessArg, response});
+        when(idempotentRedisDAO.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);
+
+        Object result = idempotentAspect.aroundPointCut(joinPoint, idempotent);
+        assertEquals("OK", result);
+
+        // 摘要应仅由「排除 servlet 对象后」的业务入参计算——含 servlet 的口径与此不同（RED），排除后一致（GREEN）
+        String expectedDigestExcludingServlet = SecureUtil.md5(LogSanitizeUtils.sanitizeArgs(new Object[]{businessArg}
));
+        verify(idempotentRedisDAO).setIfAbsent(anyString(), eq(expectedDigestExcludingServlet), eq(5L), eq(TimeUnit.SE
CONDS));
+        // 钉死：绝不因序列化触碰 servlet 响应的 writer / 输出流（避免响应被提前选定 writer，破坏后续附件等二进制输出）
+        verify(response, never()).getWriter();
+        verify(response, never()).getOutputStream();
+    }
+
     // ========== Helper methods ==========
 
     private static Map<String, Object> buildSensitiveArgs() {

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "git show HEAD:services/zhongshu-core/zszj-fr
amework/zszj-common/src/main/java/cn/zszj/framework/common/util/string/StrUtils.java; git show HEAD:services/zhongshu-c
ore/zszj-framework/zszj-common/src/main/java/cn/zszj/framework/common/util/log/LogSanitizeUtils.java; git show HEAD:ser
vices/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/idempotent/core/
keyresolver/impl/DefaultIdempotentKeyResolver.java; git grep -n '@Idempotent' -- services; git grep -n 'tomcat\\|undert
ow\\|jetty' -- services/zhongshu-core/pom.xml services/zhongshu-core/zszj-server/pom.xml services/zhongshu-core/zszj-fr
amework/zszj-spring-boot-starter-web/pom.xml; Get-ChildItem -Path tools -Name; Get-ChildItem -Path .tmp-sec011 -Name; g
it log -5 --oneline" in E:\众墅之家AI赋能平台底座
 succeeded in 300ms:
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/idempotent/pa
ckage-info.java:8: *  1. 我们去掉了 @Idempotent 注解的 delKey 属性。原因是，本质上 delKey 为 true 时，实现的是分布式锁的能力
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/test/java/cn/zszj/framework/idempotent/co
re/aop/IdempotentAspectTest.java:315:        // codex r0 P1 回归：@Idempotent 方法若含 servlet 基础设施入参（如 HttpServletResponse），
apache-maven-3.9.9
jdk-17.0.20.1+1
env.sh
jdk17.zip
maven.zip
b62af7d3 fix(sec-011-a): 幂等摘要排除 servlet 入参，堵首次放行即序列化响应破坏二进制输出 [ZS-SEC-011.A]
957cbc95 Merge branch 'feat/sec-011-a'
1bc373e1 docs(sec-010): ZS-SEC-010 收口文档同步 + codex r0→r1 两弧评审处置入库 + docs/05 V1.25
0eba03aa fix(sec-010): 限流拒绝日志参数名感知脱敏，堵标量 refreshToken 与短信 code 明文泄露 [ZS-SEC-010]
efbfc0b2 Merge branch 'feat/sec-010'

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "python -c \"import os,pathlib; root=pathlib.
Path.cwd(); targets=[root/'services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/z
szj/framework/idempotent/core/aop', root/'services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src
/test/java/cn/zszj/framework/idempotent/core/aop']; parents=sorted(set(p for t in targets for p in [t,*t.parents]),key=
str); print('Instruction files:'); [(print(str(p/n)), print((p/n).read_text(encoding='utf-8'))) for p in parents for n 
in ['AGENTS.override.md','AGENTS.md'] if (p/n).is_file()]; cfg=pathlib.Path.home()/'.codex/config.toml'; print('Config 
fallback lines:'); print('\\n'.join(l for l in cfg.read_text(encoding='utf-8').splitlines() if 'project_doc' in l or 'f
allback' in l) if cfg.exists() else '(no config)')\"; Get-ChildItem tools/jdk-17.0.20.1+1/bin -Name | Select-Object -Fi
rst 12; git show HEAD:services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/test/java/cn/zszj/f
ramework/idempotent/core/aop/IdempotentAspectTest.java; git show HEAD:services/zhongshu-core/zszj-framework/zszj-spring
-boot-starter-web/pom.xml" in E:\众墅之家AI赋能平台底座
 succeeded in 51ms:
server
api-ms-win-core-console-l1-1-0.dll
api-ms-win-core-console-l1-2-0.dll
api-ms-win-core-datetime-l1-1-0.dll
api-ms-win-core-debug-l1-1-0.dll
api-ms-win-core-errorhandling-l1-1-0.dll
api-ms-win-core-fibers-l1-1-0.dll
api-ms-win-core-fibers-l1-1-1.dll
api-ms-win-core-file-l1-1-0.dll
api-ms-win-core-file-l1-2-0.dll
api-ms-win-core-file-l2-1-0.dll
api-ms-win-core-handle-l1-1-0.dll
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
        // codex r0 P1 回归：@Idempotent 方法若含 servlet 基础设施入参（如 HttpServletResponse），
        // 计算参数摘要时不得用 Jackson 序列化它——序列化会调用 getWriter()/getOutputStream() 等 getter，
        // 提前「选定」响应输出模式，破坏后续二进制输出（如 ServletUtils.writeAttachment 附件下载抛 IllegalStateException），
        // 且首次放行请求即触发（SEC-011.A 把 sanitizeArgs 从「仅拒绝分支」提升为「每请求必算摘要」）。
        // 修复：摘要与拒绝日志的入参先排除 servlet/spring-web 对象（与 StrUtils.joinMethodArgs 既有排除口径一致）。
        Idempotent idempotent = mock(Idempotent.class);
        doReturn(FixedKeyResolver.class).when(idempotent).keyResolver();
        when(idempotent.timeout()).thenReturn(5);
        when(idempotent.timeUnit()).thenReturn(TimeUnit.SECONDS);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenReturn("OK");
        HttpServletResponse response = mock(HttpServletResponse.class);
        Object businessArg = "orderPayload";
        // 入参含 servlet 响应对象 + 业务参数
        when(joinPoint.getArgs()).thenReturn(new Object[]{businessArg, response});
        when(idempotentRedisDAO.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);

        Object result = idempotentAspect.aroundPointCut(joinPoint, idempotent);
        assertEquals("OK", result);

        // 摘要应仅由「排除 servlet 对象后」的业务入参计算——含 servlet 的口径与此不同（RED），排除后一致（GREEN）
        String expectedDigestExcludingServlet = SecureUtil.md5(LogSanitizeUtils.sanitizeArgs(new Object[]{businessArg})
);
        verify(idempotentRedisDAO).setIfAbsent(anyString(), eq(expectedDigestExcludingServlet), eq(5L), eq(TimeUnit.SEC
ONDS));
        // 钉死：绝不因序列化触碰 servlet 响应的 writer / 输出流（避免响应被提前选定 writer，破坏后续附件等二进制输出）
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

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "python -c \"import pathlib,xml.etree.Element
Tree as E; p=pathlib.Path('services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/target/surefire-re
ports/TEST-cn.zszj.framework.idempotent.core.aop.IdempotentAspectTest.xml'); print('report exists',p.exists()); root=E.
parse(p).getroot(); print('report status',root.attrib); props={x.get('name'):x.get('value') for x in root.find('propert
ies')}; cp=props.get('java.class.path',''); print('\\n'.join(x for x in cp.split(';') if any(s in x for s in ['tomcat',
'jackson','hutool','mockito','test-classes','target','junit-platform']))); print('java',props.get('java.home'))\"; git 
diff b62af7d3"'^ b62af7d3 --check; git show HEAD:services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zsz
j/framework/common/util/json/JsonUtils.java' in E:\众墅之家AI赋能平台底座
 succeeded in 9ms:
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "@'
import pathlib,subprocess,xml.etree.ElementTree as E
p=pathlib.Path('services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/target/surefire-reports/TEST-
cn.zszj.framework.idempotent.core.aop.IdempotentAspectTest.xml')
props={x.get('name'):x.get('value') for x in E.parse(p).getroot().find('properties')}
cp=props['java.class.path']
source='''
import cn.zszj.framework.idempotent.core.aop.IdempotentAspect;
import cn.zszj.framework.common.util.log.LogSanitizeUtils;
var filter=IdempotentAspect.class.getDeclaredMethod(\"serializableArgs\",Object[].class);
filter.setAccessible(true);
var response=new org.apache.catalina.connector.Response();
response.setCoyoteResponse(new org.apache.coyote.Response());
response.setCharacterEncoding(\"UTF-8\");
var facade=response.getResponse();
System.out.println(\"CONCRETE_TYPE=\"+facade.getClass().getName());
Object[] kept=(Object[])filter.invoke(null,(Object)new Object[]{\"orderPayload\",facade});
System.out.println(\"KEPT_ARGS=\"+kept.length);
System.out.println(\"SANITIZED=\"+LogSanitizeUtils.sanitizeArgs(kept));
try {facade.getOutputStream(); System.out.println(\"OUTPUT_STREAM_OK\");}catch(Throwable t){System.out.println(\"OUTPUT
_STREAM_ERROR=\"+t);}
/exit
'''
cmd=[str(pathlib.Path(props['java.home'])/'bin/jshell.exe'),'--execution','local','--class-path',cp,'-']
r=subprocess.run(cmd,input=source,text=True,encoding='utf-8',errors='replace',stdout=subprocess.PIPE,stderr=subprocess.
STDOUT,timeout=70)
print(r.stdout)
print('EXIT_CODE=',r.returncode)
'@ | python -" in E:\众墅之家AI赋能平台底座
 exited 1 in 3025ms:
Traceback (most recent call last):
  File "<stdin>", line 23, in <module>
UnicodeEncodeError: 'gbk' codec can't encode character '\ufffd' in position 1: illegal multibyte sequence

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "@'
import pathlib,subprocess,xml.etree.ElementTree as E
p=pathlib.Path('services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/target/surefire-reports/TEST-
cn.zszj.framework.idempotent.core.aop.IdempotentAspectTest.xml')
props={x.get('name'):x.get('value') for x in E.parse(p).getroot().find('properties')}
source='''
import cn.zszj.framework.idempotent.core.aop.IdempotentAspect;
import cn.zszj.framework.common.util.log.LogSanitizeUtils;
var filter=IdempotentAspect.class.getDeclaredMethod(\"serializableArgs\",Object[].class);
filter.setAccessible(true);
var response=new org.apache.catalina.connector.Response();
response.setCoyoteResponse(new org.apache.coyote.Response());
response.setCharacterEncoding(\"UTF-8\");
var facade=response.getResponse();
System.out.println(\"CONCRETE_TYPE=\"+facade.getClass().getName());
Object[] kept=(Object[])filter.invoke(null,(Object)new Object[]{\"orderPayload\",facade});
System.out.println(\"KEPT_ARGS=\"+kept.length);
System.out.println(\"SANITIZED=\"+LogSanitizeUtils.sanitizeArgs(kept));
try {facade.getOutputStream(); System.out.println(\"OUTPUT_STREAM_OK\");}catch(Throwable t){System.out.println(\"OUTPUT
_STREAM_ERROR=\"+t);}
/exit
'''
cmd=[str(pathlib.Path(props['java.home'])/'bin/jshell.exe'),'-J-Dfile.encoding=UTF-8','--execution','local','--class-pa
th',props['java.class.path'],'-']
r=subprocess.run(cmd,input=source,text=True,encoding='utf-8',errors='replace',stdout=subprocess.PIPE,stderr=subprocess.
STDOUT,timeout=70)
print(r.stdout.encode('ascii','backslashreplace').decode('ascii'))
print('EXIT_CODE=',r.returncode)
'@ | python -" in E:\众墅之家AI赋能平台底座
 succeeded in 4751ms:
9\u6708 12, 2026 1:57:08 \u4e0b\u5348 java.util.prefs.WindowsPreferences <init>
WARNING: Could not open/create prefs root node Software\JavaSoft\Prefs at root 0xffffffff80000001. Windows RegCreateKey
Ex(...) returned error code 5.
9\u6708 12, 2026 1:57:08 \u4e0b\u5348 java.util.prefs.WindowsPreferences WindowsRegOpenKey1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs at root 0xffffffff80000001.
9\u6708 12, 2026 1:57:08 \u4e0b\u5348 java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs at root 0xffffffff80000001. Windows RegOpenKey(..
.) returned error code 2.
9\u6708 12, 2026 1:57:08 \u4e0b\u5348 java.util.prefs.WindowsPreferences WindowsRegOpenKey1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs\tool at root 0xffffffff80000001.
9\u6708 12, 2026 1:57:08 \u4e0b\u5348 java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs\tool at root 0xffffffff80000001. Windows RegOpenK
ey(...) returned error code 2.
9\u6708 12, 2026 1:57:08 \u4e0b\u5348 java.util.prefs.WindowsPreferences WindowsRegOpenKey1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001.
9\u6708 12, 2026 1:57:08 \u4e0b\u5348 java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001. Windows
 RegOpenKey(...) returned error code 2.
9\u6708 12, 2026 1:57:08 \u4e0b\u5348 java.util.prefs.WindowsPreferences WindowsRegOpenKey1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001.
9\u6708 12, 2026 1:57:08 \u4e0b\u5348 java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001. Windows
 RegOpenKey(...) returned error code 2.
9\u6708 12, 2026 1:57:09 \u4e0b\u5348 java.util.prefs.WindowsPreferences WindowsRegOpenKey1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001.
9\u6708 12, 2026 1:57:09 \u4e0b\u5348 java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001. Windows
 RegOpenKey(...) returned error code 2.
\u5f02\u5e38\u9519\u8bef java.lang.NoClassDefFoundError\uff1acn/zszj/framework/idempotent/core/aop/IdempotentAspect
      at do_it$Aux (#3:1)
      at (#3:1)
\u539f\u56e0: java.lang.ClassNotFoundException\uff1a cn.zszj.framework.idempotent.core.aop.IdempotentAspect
      at URLClassLoader.findClass (URLClassLoader.java:445)
      at DefaultLoaderDelegate$RemoteClassLoader.findClass (DefaultLoaderDelegate.java:154)
      at ClassLoader.loadClass (ClassLoader.java:592)
      at ClassLoader.loadClass (ClassLoader.java:525)
      ...
\u5f02\u5e38\u9519\u8bef java.lang.NullPointerException\uff1aCannot invoke "java.lang.reflect.Method.setAccessible(bool
ean)" because "REPL.$JShell$13.filter" is null
      at (#4:1)
\u5f02\u5e38\u9519\u8bef java.lang.NoClassDefFoundError\uff1aorg/apache/catalina/connector/Response
      at do_it$Aux (#5:1)
      at (#5:1)
\u539f\u56e0: java.lang.ClassNotFoundException\uff1a org.apache.catalina.connector.Response
      at URLClassLoader.findClass (URLClassLoader.java:445)
      at DefaultLoaderDelegate$RemoteClassLoader.findClass (DefaultLoaderDelegate.java:154)
      at ClassLoader.loadClass (ClassLoader.java:592)
      at ClassLoader.loadClass (ClassLoader.java:525)
      ...
\u5f02\u5e38\u9519\u8bef java.lang.NoClassDefFoundError\uff1aorg/apache/coyote/Response
      at (#6:1)
\u539f\u56e0: java.lang.ClassNotFoundException\uff1a org.apache.coyote.Response
      at URLClassLoader.findClass (URLClassLoader.java:445)
      at DefaultLoaderDelegate$RemoteClassLoader.findClass (DefaultLoaderDelegate.java:154)
      at ClassLoader.loadClass (ClassLoader.java:592)
      at ClassLoader.loadClass (ClassLoader.java:525)
      ...
\u5f02\u5e38\u9519\u8bef java.lang.NoClassDefFoundError\uff1aorg/apache/catalina/connector/Response
      at (#7:1)
\u539f\u56e0: java.lang.ClassNotFoundException\uff1a org.apache.catalina.connector.Response
      at URLClassLoader.findClass (URLClassLoader.java:445)
      at DefaultLoaderDelegate$RemoteClassLoader.findClass (DefaultLoaderDelegate.java:154)
      at ClassLoader.loadClass (ClassLoader.java:592)
      at ClassLoader.loadClass (ClassLoader.java:525)
      ...
\u5f02\u5e38\u9519\u8bef java.lang.NoClassDefFoundError\uff1aorg/apache/catalina/connector/Response
      at do_it$Aux (#8:1)
      at (#8:1)
\u539f\u56e0: java.lang.ClassNotFoundException\uff1a org.apache.catalina.connector.Response
      at URLClassLoader.findClass (URLClassLoader.java:445)
      at DefaultLoaderDelegate$RemoteClassLoader.findClass (DefaultLoaderDelegate.java:154)
      at ClassLoader.loadClass (ClassLoader.java:592)
      at ClassLoader.loadClass (ClassLoader.java:525)
      ...
\u5f02\u5e38\u9519\u8bef java.lang.NullPointerException\uff1aCannot invoke "Object.getClass()" because "REPL.$JShell$18
.facade" is null
      at (#9:1)
\u5f02\u5e38\u9519\u8bef java.lang.NullPointerException\uff1aCannot invoke "java.lang.reflect.Method.invoke(Object, Obj
ect[])" because "REPL.$JShell$13.filter" is null
      at (#10:1)
\u5f02\u5e38\u9519\u8bef java.lang.NullPointerException\uff1aCannot read the array length because "REPL.$JShell$20.kept
" is null
      at (#11:1)
\u5f02\u5e38\u9519\u8bef java.lang.NoClassDefFoundError\uff1acn/zszj/framework/common/util/log/LogSanitizeUtils
      at (#12:1)
\u539f\u56e0: java.lang.ClassNotFoundException\uff1a cn.zszj.framework.common.util.log.LogSanitizeUtils
      at URLClassLoader.findClass (URLClassLoader.java:445)
      at DefaultLoaderDelegate$RemoteClassLoader.findClass (DefaultLoaderDelegate.java:154)
      at ClassLoader.loadClass (ClassLoader.java:592)
      at ClassLoader.loadClass (ClassLoader.java:525)
      ...
OUTPUT_STREAM_ERROR=java.lang.NoClassDefFoundError: jakarta/servlet/http/HttpServletResponse

EXIT_CODE= 0

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "@'
import pathlib,subprocess,xml.etree.ElementTree as E
p=pathlib.Path('services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/target/surefire-reports/TEST-
cn.zszj.framework.idempotent.core.aop.IdempotentAspectTest.xml')
props={x.get('name'):x.get('value') for x in E.parse(p).getroot().find('properties')}
cp=props['java.class.path'].replace('\\\\','/')
print('CP_missing_count=',sum(not pathlib.Path(x).exists() for x in cp.split(';')))
source='''
import cn.zszj.framework.idempotent.core.aop.IdempotentAspect;
import cn.zszj.framework.common.util.log.LogSanitizeUtils;
var filter=IdempotentAspect.class.getDeclaredMethod(\"serializableArgs\",Object[].class);
filter.setAccessible(true);
var response=new org.apache.catalina.connector.Response();
response.setCoyoteResponse(new org.apache.coyote.Response());
response.setCharacterEncoding(\"UTF-8\");
var facade=response.getResponse();
System.out.println(\"CONCRETE_TYPE=\"+facade.getClass().getName());
Object[] kept=(Object[])filter.invoke(null,(Object)new Object[]{\"orderPayload\",facade});
System.out.println(\"KEPT_ARGS=\"+kept.length);
System.out.println(\"SANITIZED=\"+LogSanitizeUtils.sanitizeArgs(kept));
try {facade.getOutputStream(); System.out.println(\"OUTPUT_STREAM_OK\");}catch(Throwable t){System.out.println(\"OUTPUT
_STREAM_ERROR=\"+t);}
/exit
'''
cmd=[str(pathlib.Path(props['java.home'])/'bin/jshell.exe'),'-J-Dfile.encoding=UTF-8','--execution','local','--class-pa
th',cp,'-']
r=subprocess.run(cmd,input=source,text=True,encoding='utf-8',errors='replace',stdout=subprocess.PIPE,stderr=subprocess.
STDOUT,timeout=70)
print(r.stdout.encode('ascii','backslashreplace').decode('ascii'))
print('EXIT_CODE=',r.returncode)
'@ | python -" in E:\众墅之家AI赋能平台底座
 succeeded in 4388ms:
CP_missing_count= 0
9\u6708 12, 2026 1:57:34 \u4e0b\u5348 java.util.prefs.WindowsPreferences <init>
WARNING: Could not open/create prefs root node Software\JavaSoft\Prefs at root 0xffffffff80000001. Windows RegCreateKey
Ex(...) returned error code 5.
9\u6708 12, 2026 1:57:34 \u4e0b\u5348 java.util.prefs.WindowsPreferences WindowsRegOpenKey1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs at root 0xffffffff80000001.
9\u6708 12, 2026 1:57:34 \u4e0b\u5348 java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs at root 0xffffffff80000001. Windows RegOpenKey(..
.) returned error code 2.
9\u6708 12, 2026 1:57:34 \u4e0b\u5348 java.util.prefs.WindowsPreferences WindowsRegOpenKey1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs\tool at root 0xffffffff80000001.
9\u6708 12, 2026 1:57:34 \u4e0b\u5348 java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs\tool at root 0xffffffff80000001. Windows RegOpenK
ey(...) returned error code 2.
9\u6708 12, 2026 1:57:34 \u4e0b\u5348 java.util.prefs.WindowsPreferences WindowsRegOpenKey1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001.
9\u6708 12, 2026 1:57:34 \u4e0b\u5348 java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001. Windows
 RegOpenKey(...) returned error code 2.
9\u6708 12, 2026 1:57:34 \u4e0b\u5348 java.util.prefs.WindowsPreferences WindowsRegOpenKey1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001.
9\u6708 12, 2026 1:57:34 \u4e0b\u5348 java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001. Windows
 RegOpenKey(...) returned error code 2.
9\u6708 12, 2026 1:57:35 \u4e0b\u5348 java.util.prefs.WindowsPreferences WindowsRegOpenKey1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001.
9\u6708 12, 2026 1:57:35 \u4e0b\u5348 java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001. Windows
 RegOpenKey(...) returned error code 2.
\u5f02\u5e38\u9519\u8bef java.lang.NoClassDefFoundError\uff1acn/zszj/framework/idempotent/core/aop/IdempotentAspect
      at do_it$Aux (#3:1)
      at (#3:1)
\u539f\u56e0: java.lang.ClassNotFoundException\uff1a cn.zszj.framework.idempotent.core.aop.IdempotentAspect
      at URLClassLoader.findClass (URLClassLoader.java:445)
      at DefaultLoaderDelegate$RemoteClassLoader.findClass (DefaultLoaderDelegate.java:154)
      at ClassLoader.loadClass (ClassLoader.java:592)
      at ClassLoader.loadClass (ClassLoader.java:525)
      ...
\u5f02\u5e38\u9519\u8bef java.lang.NullPointerException\uff1aCannot invoke "java.lang.reflect.Method.setAccessible(bool
ean)" because "REPL.$JShell$13.filter" is null
      at (#4:1)
\u5f02\u5e38\u9519\u8bef java.lang.NoClassDefFoundError\uff1aorg/apache/catalina/connector/Response
      at do_it$Aux (#5:1)
      at (#5:1)
\u539f\u56e0: java.lang.ClassNotFoundException\uff1a org.apache.catalina.connector.Response
      at URLClassLoader.findClass (URLClassLoader.java:445)
      at DefaultLoaderDelegate$RemoteClassLoader.findClass (DefaultLoaderDelegate.java:154)
      at ClassLoader.loadClass (ClassLoader.java:592)
      at ClassLoader.loadClass (ClassLoader.java:525)
      ...
\u5f02\u5e38\u9519\u8bef java.lang.NoClassDefFoundError\uff1aorg/apache/coyote/Response
      at (#6:1)
\u539f\u56e0: java.lang.ClassNotFoundException\uff1a org.apache.coyote.Response
      at URLClassLoader.findClass (URLClassLoader.java:445)
      at DefaultLoaderDelegate$RemoteClassLoader.findClass (DefaultLoaderDelegate.java:154)
      at ClassLoader.loadClass (ClassLoader.java:592)
      at ClassLoader.loadClass (ClassLoader.java:525)
      ...
\u5f02\u5e38\u9519\u8bef java.lang.NoClassDefFoundError\uff1aorg/apache/catalina/connector/Response
      at (#7:1)
\u539f\u56e0: java.lang.ClassNotFoundException\uff1a org.apache.catalina.connector.Response
      at URLClassLoader.findClass (URLClassLoader.java:445)
      at DefaultLoaderDelegate$RemoteClassLoader.findClass (DefaultLoaderDelegate.java:154)
      at ClassLoader.loadClass (ClassLoader.java:592)
      at ClassLoader.loadClass (ClassLoader.java:525)
      ...
\u5f02\u5e38\u9519\u8bef java.lang.NoClassDefFoundError\uff1aorg/apache/catalina/connector/Response
      at do_it$Aux (#8:1)
      at (#8:1)
\u539f\u56e0: java.lang.ClassNotFoundException\uff1a org.apache.catalina.connector.Response
      at URLClassLoader.findClass (URLClassLoader.java:445)
      at DefaultLoaderDelegate$RemoteClassLoader.findClass (DefaultLoaderDelegate.java:154)
      at ClassLoader.loadClass (ClassLoader.java:592)
      at ClassLoader.loadClass (ClassLoader.java:525)
      ...
\u5f02\u5e38\u9519\u8bef java.lang.NullPointerException\uff1aCannot invoke "Object.getClass()" because "REPL.$JShell$18
.facade" is null
      at (#9:1)
\u5f02\u5e38\u9519\u8bef java.lang.NullPointerException\uff1aCannot invoke "java.lang.reflect.Method.invoke(Object, Obj
ect[])" because "REPL.$JShell$13.filter" is null
      at (#10:1)
\u5f02\u5e38\u9519\u8bef java.lang.NullPointerException\uff1aCannot read the array length because "REPL.$JShell$20.kept
" is null
      at (#11:1)
\u5f02\u5e38\u9519\u8bef java.lang.NoClassDefFoundError\uff1acn/zszj/framework/common/util/log/LogSanitizeUtils
      at (#12:1)
\u539f\u56e0: java.lang.ClassNotFoundException\uff1a cn.zszj.framework.common.util.log.LogSanitizeUtils
      at URLClassLoader.findClass (URLClassLoader.java:445)
      at DefaultLoaderDelegate$RemoteClassLoader.findClass (DefaultLoaderDelegate.java:154)
      at ClassLoader.loadClass (ClassLoader.java:592)
      at ClassLoader.loadClass (ClassLoader.java:525)
      ...
OUTPUT_STREAM_ERROR=java.lang.NoClassDefFoundError: jakarta/servlet/http/HttpServletResponse

EXIT_CODE= 0

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "@'
import pathlib,subprocess,xml.etree.ElementTree as E,json
p=pathlib.Path('services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/target/surefire-reports/TEST-
cn.zszj.framework.idempotent.core.aop.IdempotentAspectTest.xml')
props={x.get('name'):x.get('value') for x in E.parse(p).getroot().find('properties')}
source='String cp='+json.dumps(props['java.class.path'])+';\\n'+'''
var urls = new java.util.ArrayList<java.net.URL>();
for(String entry: cp.split(\";\")) urls.add(new java.io.File(entry).toURI().toURL());
var loader=new java.net.URLClassLoader(urls.toArray(new java.net.URL[0]));
var cls=loader.loadClass(\"cn.zszj.framework.idempotent.core.aop.IdempotentAspect\");
var filter=cls.getDeclaredMethod(\"serializableArgs\",Object[].class);
filter.setAccessible(true);
var responseCls=loader.loadClass(\"org.apache.catalina.connector.Response\");
var response=responseCls.getConstructor().newInstance();
var coyoteCls=loader.loadClass(\"org.apache.coyote.Response\");
responseCls.getMethod(\"setCoyoteResponse\",coyoteCls).invoke(response,coyoteCls.getConstructor().newInstance());
responseCls.getMethod(\"setCharacterEncoding\",String.class).invoke(response,\"UTF-8\");
var facade=responseCls.getMethod(\"getResponse\").invoke(response);
System.out.println(\"CONCRETE_TYPE=\"+facade.getClass().getName());
Object[] kept=(Object[])filter.invoke(null,(Object)new Object[]{\"orderPayload\",facade});
System.out.println(\"KEPT_ARGS=\"+kept.length);
var sanitize=loader.loadClass(\"cn.zszj.framework.common.util.log.LogSanitizeUtils\").getMethod(\"sanitizeArgs\",Object
[].class,String[].class);
System.out.println(\"SANITIZED=\"+sanitize.invoke(null,(Object)kept,new String[0]));
try {facade.getClass().getMethod(\"getOutputStream\").invoke(facade); System.out.println(\"OUTPUT_STREAM_OK\");}catch(T
hrowable t){System.out.println(\"OUTPUT_STREAM_ERROR=\"+t.getCause());}
/exit
'''
cmd=[str(pathlib.Path(props['java.home'])/'bin/jshell.exe'),'-J-Dfile.encoding=UTF-8','--execution','local','-']
r=subprocess.run(cmd,input=source,text=True,encoding='utf-8',errors='replace',stdout=subprocess.PIPE,stderr=subprocess.
STDOUT,timeout=70)
out=r.stdout
print(out.encode('ascii','backslashreplace').decode('ascii'))
print('EXIT_CODE=',r.returncode)
'@ | python -" in E:\众墅之家AI赋能平台底座
 succeeded in 2979ms:
9\u6708 12, 2026 1:58:13 \u4e0b\u5348 java.util.prefs.WindowsPreferences <init>
WARNING: Could not open/create prefs root node Software\JavaSoft\Prefs at root 0xffffffff80000001. Windows RegCreateKey
Ex(...) returned error code 5.
9\u6708 12, 2026 1:58:13 \u4e0b\u5348 java.util.prefs.WindowsPreferences WindowsRegOpenKey1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs at root 0xffffffff80000001.
9\u6708 12, 2026 1:58:13 \u4e0b\u5348 java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs at root 0xffffffff80000001. Windows RegOpenKey(..
.) returned error code 2.
9\u6708 12, 2026 1:58:13 \u4e0b\u5348 java.util.prefs.WindowsPreferences WindowsRegOpenKey1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs\tool at root 0xffffffff80000001.
9\u6708 12, 2026 1:58:13 \u4e0b\u5348 java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs\tool at root 0xffffffff80000001. Windows RegOpenK
ey(...) returned error code 2.
9\u6708 12, 2026 1:58:13 \u4e0b\u5348 java.util.prefs.WindowsPreferences WindowsRegOpenKey1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001.
9\u6708 12, 2026 1:58:13 \u4e0b\u5348 java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001. Windows
 RegOpenKey(...) returned error code 2.
9\u6708 12, 2026 1:58:13 \u4e0b\u5348 java.util.prefs.WindowsPreferences WindowsRegOpenKey1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001.
9\u6708 12, 2026 1:58:13 \u4e0b\u5348 java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001. Windows
 RegOpenKey(...) returned error code 2.
9\u6708 12, 2026 1:58:13 \u4e0b\u5348 java.util.prefs.WindowsPreferences WindowsRegOpenKey1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001.
9\u6708 12, 2026 1:58:13 \u4e0b\u5348 java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001. Windows
 RegOpenKey(...) returned error code 2.
CONCRETE_TYPE=org.apache.catalina.connector.ResponseFacade
KEPT_ARGS=2
SANITIZED=["orderPayload", <ResponseFacade>]
OUTPUT_STREAM_OK

EXIT_CODE= 0

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "@'
import pathlib,subprocess,xml.etree.ElementTree as E,json
p=pathlib.Path('services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/target/surefire-reports/TEST-
cn.zszj.framework.idempotent.core.aop.IdempotentAspectTest.xml')
props={x.get('name'):x.get('value') for x in E.parse(p).getroot().find('properties')}
source='String cp='+json.dumps(props['java.class.path'])+';\\n'+'''
var urls = new java.util.ArrayList<java.net.URL>();
for(String entry: cp.split(\";\")) urls.add(new java.io.File(entry).toURI().toURL());
var loader=new java.net.URLClassLoader(urls.toArray(new java.net.URL[0]));
var filter=loader.loadClass(\"cn.zszj.framework.idempotent.core.aop.IdempotentAspect\").getDeclaredMethod(\"serializabl
eArgs\",Object[].class);
filter.setAccessible(true);
var responseCls=loader.loadClass(\"org.apache.catalina.connector.Response\");
var response=responseCls.getConstructor().newInstance();
var cyResponseCls=loader.loadClass(\"org.apache.coyote.Response\");
var cyResponse=cyResponseCls.getConstructor().newInstance();
responseCls.getMethod(\"setCoyoteResponse\",cyResponseCls).invoke(response,cyResponse);
var requestCls=loader.loadClass(\"org.apache.catalina.connector.Request\");
var connectorCls=loader.loadClass(\"org.apache.catalina.connector.Connector\");
var request=requestCls.getConstructor(connectorCls).newInstance(connectorCls.getConstructor().newInstance());
responseCls.getMethod(\"setRequest\",requestCls).invoke(response,request);
requestCls.getMethod(\"setResponse\",responseCls).invoke(request,response);
var cyRequestCls=loader.loadClass(\"org.apache.coyote.Request\");
var cyRequest=cyRequestCls.getConstructor().newInstance();
requestCls.getMethod(\"setCoyoteRequest\",cyRequestCls).invoke(request,cyRequest);
cyResponseCls.getMethod(\"setRequest\",cyRequestCls).invoke(cyResponse,cyRequest);
cyRequestCls.getMethod(\"setResponse\",cyResponseCls).invoke(cyRequest,cyResponse);
responseCls.getMethod(\"setCharacterEncoding\",String.class).invoke(response,\"UTF-8\");
var facade=responseCls.getMethod(\"getResponse\").invoke(response);
System.out.println(\"CONCRETE_TYPE=\"+facade.getClass().getName());
Object[] kept=(Object[])filter.invoke(null,(Object)new Object[]{\"orderPayload\",facade});
System.out.println(\"KEPT_ARGS=\"+kept.length);
var mapper=loader.loadClass(\"cn.zszj.framework.common.util.json.JsonUtils\").getMethod(\"getObjectMapper\").invoke(nul
l);
try {mapper.getClass().getMethod(\"valueToTree\",Object.class).invoke(mapper,facade);}catch(Throwable t){System.out.pri
ntln(\"SERIALIZATION_ERROR=\"+t.getCause());}
try {facade.getClass().getMethod(\"getOutputStream\").invoke(facade); System.out.println(\"OUTPUT_STREAM_OK\");}catch(T
hrowable t){System.out.println(\"OUTPUT_STREAM_ERROR=\"+t.getCause());}
/exit
'''
cmd=[str(pathlib.Path(props['java.home'])/'bin/jshell.exe'),'-J-Dfile.encoding=UTF-8','--execution','local','-']
r=subprocess.run(cmd,input=source,text=True,encoding='utf-8',errors='replace',stdout=subprocess.PIPE,stderr=subprocess.
STDOUT,timeout=70)
print(r.stdout.encode('ascii','backslashreplace').decode('ascii'))
print('EXIT_CODE=',r.returncode)
'@ | python -" in E:\众墅之家AI赋能平台底座
 succeeded in 3279ms:
9\u6708 12, 2026 1:58:51 \u4e0b\u5348 java.util.prefs.WindowsPreferences <init>
WARNING: Could not open/create prefs root node Software\JavaSoft\Prefs at root 0xffffffff80000001. Windows RegCreateKey
Ex(...) returned error code 5.
9\u6708 12, 2026 1:58:51 \u4e0b\u5348 java.util.prefs.WindowsPreferences WindowsRegOpenKey1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs at root 0xffffffff80000001.
9\u6708 12, 2026 1:58:51 \u4e0b\u5348 java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs at root 0xffffffff80000001. Windows RegOpenKey(..
.) returned error code 2.
9\u6708 12, 2026 1:58:51 \u4e0b\u5348 java.util.prefs.WindowsPreferences WindowsRegOpenKey1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs\tool at root 0xffffffff80000001.
9\u6708 12, 2026 1:58:51 \u4e0b\u5348 java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs\tool at root 0xffffffff80000001. Windows RegOpenK
ey(...) returned error code 2.
9\u6708 12, 2026 1:58:51 \u4e0b\u5348 java.util.prefs.WindowsPreferences WindowsRegOpenKey1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001.
9\u6708 12, 2026 1:58:51 \u4e0b\u5348 java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001. Windows
 RegOpenKey(...) returned error code 2.
9\u6708 12, 2026 1:58:51 \u4e0b\u5348 java.util.prefs.WindowsPreferences WindowsRegOpenKey1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001.
9\u6708 12, 2026 1:58:51 \u4e0b\u5348 java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001. Windows
 RegOpenKey(...) returned error code 2.
9\u6708 12, 2026 1:58:52 \u4e0b\u5348 java.util.prefs.WindowsPreferences WindowsRegOpenKey1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001.
9\u6708 12, 2026 1:58:52 \u4e0b\u5348 java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs\tool\/J/Shell at root 0xffffffff80000001. Windows
 RegOpenKey(...) returned error code 2.
CONCRETE_TYPE=org.apache.catalina.connector.ResponseFacade
KEPT_ARGS=2
SERIALIZATION_ERROR=java.lang.IllegalArgumentException: getWriter() has already been called for this response (through 
reference chain: org.apache.catalina.connector.ResponseFacade["outputStream"])
OUTPUT_STREAM_ERROR=java.lang.IllegalStateException: getWriter() has already been called for this response

EXIT_CODE= 0

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "@'
import pathlib,subprocess
p=pathlib.Path('services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framewo
rk/idempotent/core/aop/IdempotentAspect.java')
for n,line in enumerate(p.read_text(encoding='utf-8').splitlines(),1):
    if 137<=n<=154: print(f'{n}: {line}')
print(subprocess.run(['git','status','--short'],capture_output=True).stdout.decode('utf-8','replace').encode('ascii','b
ackslashreplace').decode('ascii'))
'@ | python -; git grep -n -A 22 'void writeAttachment' -- services/zhongshu-core/zszj-framework/zszj-common/src/main/j
ava/cn/zszj/framework/common/util/servlet/ServletUtils.java" in E:\众墅之家AI赋能平台底座
 succeeded in 141ms:
137:     private static Object[] serializableArgs(Object[] args) {
138:         if (args == null || args.length == 0) {
139:             return args;
140:         }
141:         List<Object> kept = new ArrayList<>(args.length);
142:         for (Object arg : args) {
143:             if (arg == null) {
144:                 kept.add(null);
145:                 continue;
146:             }
147:             String clazzName = arg.getClass().getName();
148:             if (StrUtil.startWithAny(clazzName, "javax.servlet", "jakarta.servlet", "org.springframework.web")) {
149:                 continue;
150:             }
151:             kept.add(arg);
152:         }
153:         return kept.toArray();
154:     }
?? ".omx/plans/2026-09-11-B03-Wave1-\345\205\255\351\241\271\345\271\266\350\241\214\345\274\200\345\217\221\350\256\24
1\345\210\222.md"
?? _zs_sec011a_codex.ps1
?? _zs_sec011a_codex_r1.ps1
?? _zs_sec011a_fixmsg.txt
?? _zs_sec011a_prot.txt
?? _zs_sec011a_tdd.ps1
?? _zs_sec011a_tdd.txt
?? _zs_sec011a_verify.ps1
?? docs/reviews/codex-ZS-SEC-011.A.raw.md
?? outputs/

services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framework/common/util/servlet/ServletUtils.java
:107:    public static void writeAttachment(HttpServletResponse response, String filename, byte[] content)
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framework/common/util/servlet/ServletUtils.java
-108-            throws IOException {
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framework/common/util/servlet/ServletUtils.java
-109-        response.getOutputStream().write(content);
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framework/common/util/servlet/ServletUtils.java
-110-        response.addHeader("Content-Disposition", "attachment;filename=" + HttpUtils.encodeUtf8(filename));
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framework/common/util/servlet/ServletUtils.java
-111-        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framework/common/util/servlet/ServletUtils.java
-112-    }
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framework/common/util/servlet/ServletUtils.java
-113-
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framework/common/util/servlet/ServletUtils.java
-114-}

codex
The new exclusion logic misses actual Tomcat servlet implementations, leaving the binary-response failure this commit i
ntends to fix. The added interface-mock test does not cover that runtime case.

Review comment:

- [P1] Exclude servlet implementations by type rather than package — E:/众墅之家AI赋能平台底座/services/zhongshu-core/zszj-framew
ork/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/idempotent/core/aop/IdempotentAspect.java:147-1
49
  For an `@Idempotent` method receiving `HttpServletResponse` on Tomcat, the runtime class is `org.apache.catalina.conn
ector.ResponseFacade`, which matches none of these prefixes. The new filter therefore retains it, allowing digest seria
lization to call `getWriter()` before the method executes. With a real Tomcat response, this causes subsequent binary o
utput through `ServletUtils.writeAttachment()` to throw `IllegalStateException`. Use servlet interface/type checks that
 also cover wrappers, and test a concrete container response rather than only an interface mock.
The new exclusion logic misses actual Tomcat servlet implementations, leaving the binary-response failure this commit intends to fix. The added interface-mock test does not cover that runtime case.

Review comment:

- [P1] Exclude servlet implementations by type rather than package — E:/众墅之家AI赋能平台底座/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-protection/src/main/java/cn/zszj/framework/idempotent/core/aop/IdempotentAspect.java:147-149
  For an `@Idempotent` method receiving `HttpServletResponse` on Tomcat, the runtime class is `org.apache.catalina.connector.ResponseFacade`, which matches none of these prefixes. The new filter therefore retains it, allowing digest serialization to call `getWriter()` before the method executes. With a real Tomcat response, this causes subsequent binary output through `ServletUtils.writeAttachment()` to throw `IllegalStateException`. Use servlet interface/type checks that also cover wrappers, and test a concrete container response rather than only an interface mock.
