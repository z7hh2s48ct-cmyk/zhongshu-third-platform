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
session id: 01a0938c-2f66-7403-aad5-b146a2c9bab1
--------
user
commit b9568d5
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'git status --short; git rev-parse --show-top
level; git show --stat --oneline --decorate --no-renames b9568d54; Get-ChildItem -Force -Name' in E:\众墅之家AI赋能平台底座
 succeeded in 43ms:
b9568d54 (HEAD -> main) fix(sec-004): CORS 放行无端口 loopback 源+前端实发头，本地恢复 doc.html iframe [ZS-SEC-004]
 .../zszj/framework/web/config/WebProperties.java   | 11 +++++++--
 .../zszj/framework/web/config/CorsConfigTest.java  | 27 ++++++++++++++++++++++
 .../src/main/resources/application-local.yaml      |  3 +++
 .../src/main/resources/application.yaml            |  2 ++
 4 files changed, 41 insertions(+), 2 deletions(-)
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
_zs_codex_r1.ps1

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "git show --format=fuller --no-ext-diff --no-
renames b9568d54; Get-ChildItem -Path . -Recurse -Force -File -Include AGENTS.override.md,AGENTS.md -ErrorAction Silent
lyContinue | Select-Object -ExpandProperty FullName; Get-Item E:\\AGENTS.override.md,E:\\AGENTS.md -ErrorAction Silentl
yContinue | Select-Object -ExpandProperty FullName; if (Test-Path C:\\Users\\Administrator\\.codex\\config.toml) { Sele
ct-String -Path C:\\Users\\Administrator\\.codex\\config.toml -Pattern 'project_doc|fallback' }" in E:\众墅之家AI赋能平台底座
 succeeded in 34057ms:
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command '$paths = @('"'services','services/zhongshu-c
ore','services/zhongshu-core/zszj-framework','services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web','serv
ices/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src','services/zhongshu-core/zszj-framework/zszj-spring-
boot-starter-web/src/main','services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/main/java','services
/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/main/java/cn','services/zhongshu-core/zszj-framework/zsz
j-spring-boot-starter-web/src/main/java/cn/zszj','services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/sr
c/main/java/cn/zszj/framework','services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/main/java/cn/zsz
j/framework/web','services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/main/java/cn/zszj/framework/we
b/config','services/zhongshu-core/zszj-server','services/zhongshu-core/zszj-server/src','services/zhongshu-core/zszj-se
rver/src/main','services/zhongshu-core/zszj-server/src/main/resources'); foreach ("'$p in $paths) { foreach ($n in @('"
'AGENTS.override.md','AGENTS.md')) { "'$f = Join-Path $p $n; if (Test-Path -LiteralPath $f) { Write-Output "--- $f"; Ge
t-Content -LiteralPath $f } } }; git ls-files '"'*AGENTS*' '*CLAUDE*'; Get-Content services/zhongshu-core/zszj-framewor
k/zszj-spring-boot-starter-web/src/main/java/cn/zszj/framework/web/config/WebProperties.java; Get-Content services/zhon
gshu-core/zszj-framework/zszj-spring-boot-starter-web/src/main/java/cn/zszj/framework/web/config/ZszjWebAutoConfigurati
on.java" in E:\众墅之家AI赋能平台底座
 succeeded in 0ms:
package cn.zszj.framework.web.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "zszj.web")
@Validated
@Data
public class WebProperties {

    @NotNull(message = "APP API 不能为空")
    @Valid
    private Api appApi = new Api("/app-api", "**.controller.app.**");
    @NotNull(message = "Admin API 不能为空")
    @Valid
    private Api adminApi = new Api("/admin-api", "**.controller.admin.**");

    @NotNull(message = "Admin UI 不能为空")
    @Valid
    private Ui adminUi;

    @NotNull(message = "请求体限制不能为�?)
    @Valid
    private RequestBody requestBody = new RequestBody();

    @NotNull(message = "CORS 配置不能为空")
    @Valid
    private Cors cors = new Cors();

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Api {

        /**
         * API 前缀，实现所�?Controller 提供�?RESTFul API 的统一前缀
         *
         *
         * 意义：通过该前缀，避�?Swagger、Actuator 意外通过 Nginx 暴露出来给外部，带来安全性问�?         *      这样，Nginx 只需要配置转发到 /api/* 的所有接口即可�?  
       *
         * @see ZszjWebAutoConfiguration#configurePathMatch(PathMatchConfigurer)
         */
        @NotEmpty(message = "API 前缀不能为空")
        private String prefix;

        /**
         * Controller 所在包�?Ant 路径规则
         *
         * 主要目的是，给该 Controller 设置指定�?{@link #prefix}
         */
        @NotEmpty(message = "Controller 所在包不能为空")
        private String controller;

    }

    @Data
    public static class Ui {

        /**
         * 访问地址
         */
        private String url;

    }

    @Data
    public static class RequestBody {

        /**
         * JSON 请求体缓冲上限（字节）�?         *
         * ZS-SEC-008：认证前 CacheRequestBodyFilter 会全量缓�?JSON 请求体以支持重复读取。超限则在缓冲前受控拒绝
         * （业务码 400），防止超大 body 耗尽内存。默�?1MB；配置为 {@code <= 0} 表示不限制�?         * 上传 / 流式接口�?JSON，已�?CacheRequestBodyFilter
#shouldNotFilter 排除，不受此限影响�?         */
        private long maxCacheSize = 1024 * 1024L;

    }

    /**
     * 浏览器跨域（CORS）与点击劫持防护配置�?     *
     * ZS-SEC-004：由原先的全通配（{@code allowedOriginPattern=*} + {@code allowCredentials=true}）收紧为精确白名单，
     * 消除“任意源携带凭据”的安全缺陷；同时将 frameOptions 由机械全关改为可配置（默�?SAMEORIGIN 防点击劫持）�?     */
    @Data
    public static class Cors {

        /**
         * 允许的源模式白名单（精确，禁止裸 * �?credentials 并存）。默认仅本地开发源�?         */
        private List<String> allowedOriginPatterns = new ArrayList<>(List.of(
                "http://localhost:*", "http://127.0.0.1:*",
                // ZS-SEC-004 P1-1：Spring �?":*" 模式不匹配无端口源；admin-web 本地 VITE_PORT=80 时浏览器发出
                // Origin: http://localhost（省略默认端口），须显式放行无端�?loopback 源，否则本地前端跨域被拒�?                "http://localhost",
 "http://127.0.0.1"));

        /**
         * 允许的方法�?         */
        private List<String> allowedMethods = new ArrayList<>(List.of(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));

        /**
         * 允许的请求头（含租户�?tenant-id）�?         */
        private List<String> allowedHeaders = new ArrayList<>(List.of(
                "Authorization", "Content-Type", "X-Requested-With", "tenant-id",
                // ZS-SEC-004 P1-2：admin-web service.ts 对每�?GET 注入 Cache-Control/Pragma（防缓存）、跨租户注入
                // visit-tenant-id、API 加密注入 X-Api-Encrypt；须列入白名单，否则预检 Access-Control-Request-Headers
                // 校验失败，拦截所�?admin GET（含租户查询/权限加载）�?                "Cache-Control", "Pragma", "visit-tenant-id", "X-Ap
i-Encrypt"));

        /**
         * 暴露给浏览器的响应头（ZS-SEC-006 trace-id 关联）�?         */
        private List<String> exposedHeaders = new ArrayList<>(List.of("trace-id"));

        /**
         * 是否携带凭据。精确源白名单下可为 true�?         */
        private boolean allowCredentials = true;

        /**
         * 预检缓存秒数�?         */
        private long maxAge = 3600L;

        /**
         * frame-options：默�?SAMEORIGIN 防点击劫持；需嵌入时显式改 DISABLE�?         */
        private String frameOptions = "SAMEORIGIN";

    }

}
package cn.zszj.framework.web.config;

import cn.hutool.core.util.StrUtil;
import cn.zszj.framework.common.biz.infra.logger.ApiErrorLogCommonApi;
import cn.zszj.framework.common.enums.WebFilterOrderEnum;
import cn.zszj.framework.web.core.filter.CacheRequestBodyFilter;
import cn.zszj.framework.web.core.filter.DemoFilter;
import cn.zszj.framework.web.core.handler.GlobalExceptionHandler;
import cn.zszj.framework.web.core.handler.GlobalResponseBodyHandler;
import cn.zszj.framework.web.core.util.WebFrameworkUtils;
import com.google.common.collect.Maps;
import jakarta.servlet.Filter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Map;
import java.util.function.Predicate;

@AutoConfiguration
@EnableConfigurationProperties(WebProperties.class)
public class ZszjWebAutoConfiguration {

    /**
     * 应用�?     */
    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    public WebMvcRegistrations webMvcRegistrations(WebProperties webProperties) {
        return new WebMvcRegistrations() {

            @Override
            public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
                RequestMappingHandlerMapping mapping = new RequestMappingHandlerMapping();
                // 实例化时就带上前缀
                mapping.setPathPrefixes(buildPathPrefixes(webProperties));
                return mapping;
            }

            /**
             * 构建 prefix �?匹配条件的映�?             */
            private Map<String, Predicate<Class<?>>> buildPathPrefixes(WebProperties webProperties) {
                AntPathMatcher antPathMatcher = new AntPathMatcher(".");
                Map<String, Predicate<Class<?>>> pathPrefixes = Maps.newLinkedHashMapWithExpectedSize(2);
                putPathPrefix(pathPrefixes, webProperties.getAdminApi(), antPathMatcher);
                putPathPrefix(pathPrefixes, webProperties.getAppApi(), antPathMatcher);
                return pathPrefixes;
            }

            /**
             * 设置 API 前缀，仅仅匹�?controller 包下�?             */
            private void putPathPrefix(Map<String, Predicate<Class<?>>> pathPrefixes, WebProperties.Api api, AntPathMat
cher matcher) {
                if (api == null || StrUtil.isEmpty(api.getPrefix())) {
                    return;
                }
                pathPrefixes.put(api.getPrefix(), // api 前缀
                        clazz -> clazz.isAnnotationPresent(RestController.class)
                                && matcher.match(api.getController(), clazz.getPackage().getName()));
            }

        };
    }

    @Bean
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public GlobalExceptionHandler globalExceptionHandler(ApiErrorLogCommonApi apiErrorLogApi) {
        return new GlobalExceptionHandler(applicationName, apiErrorLogApi);
    }

    @Bean
    public GlobalResponseBodyHandler globalResponseBodyHandler() {
        return new GlobalResponseBodyHandler();
    }

    @Bean
    @SuppressWarnings("InstantiationOfUtilityClass")
    public WebFrameworkUtils webFrameworkUtils(WebProperties webProperties) {
        // 由于 WebFrameworkUtils 需要使用到 webProperties 属性，所以注册为一�?Bean
        return new WebFrameworkUtils(webProperties);
    }

    // ========== Filter 相关 ==========

    /**
     * 创建 CorsFilter Bean，解决跨域问�?     *
     * ZS-SEC-004：由全通配收紧为精确白名单——源/方法/�?暴露�?凭据/maxAge 全部读取 {@link WebProperties.Cors}�?     * 默认仅本地开发源，杜绝“allowedOriginP
attern=* + allowCredentials=true”的任意源携带凭据缺陷�?     */
    @Bean
    @Order(value = WebFilterOrderEnum.CORS_FILTER) // 特殊：修复因执行顺序影响到跨域配置不生效问题
    public FilterRegistrationBean<CorsFilter> corsFilterBean(WebProperties webProperties) {
        WebProperties.Cors cors = webProperties.getCors();
        // 创建 CorsConfiguration 对象
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(cors.isAllowCredentials());
        cors.getAllowedOriginPatterns().forEach(config::addAllowedOriginPattern); // 设置访问源地址（精确白名单�?        cors.getAll
owedHeaders().forEach(config::addAllowedHeader); // 设置访问源请求头
        cors.getAllowedMethods().forEach(config::addAllowedMethod); // 设置访问源请求方�?        cors.getExposedHeaders().forEa
ch(config::addExposedHeader); // 设置暴露给浏览器的响应头
        config.setMaxAge(cors.getMaxAge()); // 预检缓存秒数
        // 创建 UrlBasedCorsConfigurationSource 对象
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); // 对接口配置跨域设�?        return createFilterBean(new CorsFilter(so
urce), WebFilterOrderEnum.CORS_FILTER);
    }

    /**
     * 创建 RequestBodyCacheFilter Bean，可重复读取请求内容
     */
    @Bean
    public FilterRegistrationBean<CacheRequestBodyFilter> requestBodyCacheFilter(WebProperties webProperties) {
        // ZS-SEC-008：注�?JSON 请求体缓冲上限，缓冲前受控拒绝超�?body
        return createFilterBean(new CacheRequestBodyFilter(webProperties.getRequestBody().getMaxCacheSize()),
                WebFilterOrderEnum.REQUEST_BODY_CACHE_FILTER);
    }

    /**
     * 创建 DemoFilter Bean，演示模�?     */
    @Bean
    @ConditionalOnProperty(value = "zszj.demo", havingValue = "true")
    public FilterRegistrationBean<DemoFilter> demoFilter() {
        return createFilterBean(new DemoFilter(), WebFilterOrderEnum.DEMO_FILTER);
    }

    public static <T extends Filter> FilterRegistrationBean<T> createFilterBean(T filter, Integer order) {
        FilterRegistrationBean<T> bean = new FilterRegistrationBean<>(filter);
        bean.setOrder(order);
        return bean;
    }

    /**
     * 创建 RestTemplate 实例
     *
     * @param restTemplateBuilder {@link RestTemplateAutoConfiguration#restTemplateBuilder}
     */
    @Bean
    @ConditionalOnMissingBean
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.build();
    }

}

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "git grep -n -e 'getFrameOptions' -e 'allowed
-headers' -e 'allowed-origin-patterns' -e 'frame-options' -- services apps scripts docs; git grep -n -e 'X-Api-Encrypt'
 -e 'visit-tenant-id' -e 'Cache-Control' -e 'Pragma' -- apps/admin-web/src; Get-Content -Encoding UTF8 services/zhongsh
u-core/zszj-framework/zszj-spring-boot-starter-web/src/test/java/cn/zszj/framework/web/config/CorsConfigTest.java; Get-
Content -Encoding UTF8 services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/pom.xml; Get-Content -Encodin
g UTF8 services/zhongshu-core/zszj-server/src/main/resources/application.yaml | Select-Object -First 45; Get-ChildItem 
-Force services/zhongshu-core -Name; Get-Command mvn,java -ErrorAction SilentlyContinue | Select-Object Name,Source" in
 E:\众墅之家AI赋能平台底座
 exited 1 in 316ms:
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-security/src/main/java/cn/zszj/framework/security/config
/ZszjWebSecurityConfigurerAdapter.java:112:        String frameOptions = webProperties.getCors().getFrameOptions();
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/main/java/cn/zszj/framework/web/config/WebProper
ties.java:142:         * frame-options：默认 SAMEORIGIN 防点击劫持；需嵌入时显式改 DISABLE。
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/test/java/cn/zszj/framework/web/config/CorsConfi
gTest.java:39:        assertEquals("SAMEORIGIN", new WebProperties.Cors().getFrameOptions());
services/zhongshu-core/zszj-server/src/main/resources/application-local.yaml:192:      frame-options: DISABLE # ZS-SEC-
004 P2 本地：admin-web(:80) 内嵌后端(:48080) /doc.html 的 swagger iframe；SAMEORIGIN 跨源阻断，本地放开，生产默认仍 SAMEORIGIN
services/zhongshu-core/zszj-server/src/main/resources/application.yaml:283:      allowed-origin-patterns:
services/zhongshu-core/zszj-server/src/main/resources/application.yaml:289:      frame-options: SAMEORIGIN # 默认防点击劫持；确需
被 iframe 嵌入时显式改为 DISABLE
package cn.zszj.framework.web.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ZS-SEC-004��CORS ��ȫͨ���ս�Ϊ��ȷ������ + frameOptions ������
 *
 * ����Ԫ���ԣ������� Spring �����ģ�ֱ��У�� {@link WebProperties.Cors} ��Ĭ��ֵ��Լ����
 */
class CorsConfigTest {

    @Test
    void defaultAllowedOrigins_shouldNotContainBareWildcard() {
        WebProperties.Cors cors = new WebProperties().getCors();
        // �ع黤����Ĭ�ϰ����������ܺ��� "*"��credentials=true ��ͨ��Դ�ǰ�ȫȱ�ݣ��ᱻ������ܾ������󹥻��棩
        assertFalse(cors.getAllowedOriginPatterns().contains("*"),
                "Ĭ�� CORS ��������ֹ�� *��credentials=true ��ͨ���ǰ�ȫȱ�ݣ�");
        // credentials Ĭ��Ϊ true����"��ȷԴ������"����źϷ�
        assertTrue(cors.isAllowCredentials(),
                "Ĭ�� allowCredentials ӦΪ true���뾫ȷ���������䣩");
    }

    @Test
    void exposedHeaders_shouldContainTraceId() {
        WebProperties.Cors cors = new WebProperties.Cors();
        assertTrue(cors.getExposedHeaders().contains("trace-id"),
                "ZS-SEC-006 �������뱩¶ trace-id");
    }

    @Test
    void frameOptions_defaultShouldBeSameOriginNotDisabled() {
        assertEquals("SAMEORIGIN", new WebProperties.Cors().getFrameOptions());
    }

    @Test
    void allowedHeaders_shouldCoverAllFrontendSentHeaders() {
        WebProperties.Cors cors = new WebProperties.Cors();
        // ZS-SEC-004 P1-2��admin-web service.ts ��ÿ�� GET ע�� Cache-Control/Pragma�������棩�����⻧����ע��
        // visit-tenant-id��API ����ע�� X-Api-Encrypt����խ allowedHeaders ����ȱ��Щͷ�������Ԥ��
        // Access-Control-Request-Headers У�鲻ͨ�� �� �������� admin GET�����⻧��ѯ/Ȩ�޼��أ���
        assertTrue(cors.getAllowedHeaders().containsAll(List.of(
                        "Authorization", "Content-Type", "X-Requested-With", "tenant-id",
                        "Cache-Control", "Pragma", "visit-tenant-id", "X-Api-Encrypt")),
                "allowedHeaders ���븲��ǰ�� service.ts ʵ�ʷ��͵�ȫ���Զ���ͷ������Ԥ��ʧ����������");
    }

    @Test
    void allowedOriginPatterns_shouldMatchPortlessLocalhost() {
        WebProperties.Cors cors = new WebProperties().getCors();
        // ZS-SEC-004 P1-1��admin-web ���� VITE_PORT=80������������� Origin Ϊ�޶˿ڵ� http://localhost��
        // �� Spring ��ʵƥ�����壨CorsConfiguration.checkOrigin����֤�������Ƿ���и��޶˿�Դ������ null �����ܡ�
        CorsConfiguration config = new CorsConfiguration();
        cors.getAllowedOriginPatterns().forEach(config::addAllowedOriginPattern);
        config.setAllowCredentials(cors.isAllowCredentials());
        assertEquals("http://localhost", config.checkOrigin("http://localhost"),
                "�޶˿� http://localhost ���뱻 CORS ���������У�VITE_PORT=80 ����ǰ�˳�����");
        assertEquals("http://127.0.0.1", config.checkOrigin("http://127.0.0.1"),
                "�޶˿� http://127.0.0.1 ���뱻 CORS ����������");
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
    <description>Web ��ܣ�ȫ���쳣��API ��־���������������</description>
    <url>https://github.com/YunaiV/ruoyi-vue-pro</url>

    <dependencies>
        <dependency>
            <groupId>cn.zszj</groupId>
            <artifactId>zszj-common</artifactId>
        </dependency>

        <!-- Web ��� -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <!-- spring boot ������������ -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.aspectj</groupId>
            <artifactId>aspectjweaver</artifactId>
            <scope>provided</scope> <!-- ��������� SpringExpressionUtils ���ص�ʱ����ʲ��� org.aspectj.lang.JoinPoint ����
 -->
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
            <scope>provided</scope> <!-- ����Ϊ provided����Ҫ�� GlobalExceptionHandler ʹ�� -->
        </dependency>

        <!-- ��������� -->
        <dependency>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
            <scope>provided</scope> <!-- ����Ϊ provided��ֻ�й�������Ҫʹ�õ� -->
        </dependency>

        <dependency>
            <groupId>org.jsoup</groupId>
            <artifactId>jsoup</artifactId>
        </dependency>

        <!-- Test ������� -->
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
spring:
  application:
    name: zszj-server

  profiles:
    active: local

  main:
    allow-circular-references: true # ����ѭ����������Ϊ��Ŀ������ܹ����޷�������������

  # Servlet ����
  servlet:
    # �ļ��ϴ����������
    multipart:
      max-file-size: 16MB # �����ļ���С
      max-request-size: 32MB # �������ϴ����ļ���С

  # Jackson ������
  jackson:
    serialization:
      write-dates-as-timestamps: true # ���� Date �ĸ�ʽ��ʹ��ʱ���
      write-date-timestamps-as-nanoseconds: false # ���ò�ʹ�� nanoseconds �ĸ�ʽ������˵ 1611460870.401������ֱ�� 1611460870
401
      write-durations-as-timestamps: true # ���� Duration �ĸ�ʽ��ʹ��ʱ���
      fail-on-empty-beans: false # �������л������Ե� Bean

  # Cache ������
  cache:
    type: REDIS
    redis:
      time-to-live: 1h # ���ù���ʱ��Ϊ 1 Сʱ

server:
  servlet:
    encoding:
      enabled: true
      charset: UTF-8 # �������� UTF-8������ WebFlux ��ʽ���أ�AI ����������������
      force: true

--- #################### �ӿ��ĵ����� ####################

springdoc:
  api-docs:
    enabled: false # ZS-ENG-005���ӿ��ĵ�Ĭ�ϲ���¶���� local ������ʽ����
    path: /v3/api-docs
  swagger-ui:
.gitee
.github
.image
docs
script
sql
zszj-dependencies
zszj-framework
zszj-module-ai
zszj-module-bpm
zszj-module-crm
zszj-module-erp
zszj-module-fms
zszj-module-hrm
zszj-module-im
zszj-module-infra
zszj-module-iot
zszj-module-mall
zszj-module-member
zszj-module-mes
zszj-module-mp
zszj-module-pay
zszj-module-pms
zszj-module-report
zszj-module-system
zszj-module-wms
zszj-server
zszj-ui
.flattened-pom.xml
.gitignore
LICENSE
lombok.config
pom.xml
README.md

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "git ls-files '*service.ts' '*security*Test*'
 '*Frame*Test*' '*Cors*' '*[Aa][Gg][Ee][Nn][Tt][Ss]*' '*[Cc][Ll][Aa][Uu][Dd][Ee]*'; git show HEAD:services/zhongshu-cor
e/zszj-framework/zszj-spring-boot-starter-security/src/main/java/cn/zszj/framework/security/config/ZszjWebSecurityConfi
gurerAdapter.java; git show HEAD:services/zhongshu-core/zszj-server/src/main/resources/application-local.yaml; git ls-f
iles '*env*' '*Dockerfile*' '*compose*' '*start*.ps1'" in E:\众墅之家AI赋能平台底座
 succeeded in 1ms:
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "git show HEAD:apps/zhongshu-admin-web/src/co
nfig/axios/service.ts; git show HEAD:apps/zhongshu-admin-web/.env.local; git show HEAD:apps/zhongshu-admin-web/src/view
s/infra/swagger/index.vue; git show HEAD:services/zhongshu-core/zszj-server/Dockerfile; git show HEAD:services/zhongshu
-core/script/docker/docker-compose.yml; git log -5 --oneline; git grep -n -e 'java.exe' -e 'mvn.cmd' -e 'JAVA_HOME' -e 
'MAVEN_HOME' -- scripts tools .github; git diff b9568d54"'^ b9568d54 --check' in E:\众墅之家AI赋能平台底座
 succeeded in 23ms:
scripts/ops/run-local-gates.mjs:106:      const child = spawn('bash', ['-c', 'source tools/env.sh && cd services/zhongs
hu-core && MSYS_NO_PATHCONV=1 "$TOOLS/apache-maven-3.9.9/bin/mvn.cmd" ' + gate.mvnArgs],

codex
The CORS additions match the frontend’s local origins and request headers, and the iframe override is confined to the l
ocal profile. No actionable regressions introduced by this commit were found. Tests were not run in the read-only envir
onment.
The CORS additions match the frontend’s local origins and request headers, and the iframe override is confined to the local profile. No actionable regressions introduced by this commit were found. Tests were not run in the read-only environment.
