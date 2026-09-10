codex.cmd : OpenAI Codex v0.153.4
所在位置 行:1 字符: 100
+ ... 家AI赋能平台底座'; & "$env:APPDATA\npm\codex.cmd" review --commit 76da2a2f 2 ...
+                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    + CategoryInfo          : NotSpecified: (OpenAI Codex v0.153.4:String) [], 
    RemoteException
    + FullyQualifiedErrorId : NativeCommandError
 
--------
workdir: E:\众墅之家AI赋能平台底座
model: gpt-6-astra
provider: openai
approval: never
sandbox: workspace-write [workdir, /tmp, $TMPDIR]
reasoning effort: xhigh
reasoning summaries: none
session id: 01a08af3-2098-7ba3-9d6e-3560f81b1e8f
--------
user
commit 76da2a2
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'Get-
Location; git status --short; git rev-parse --show-toplevel; git show --stat --
oneline --decorate --no-renames 76da2a2f; Get-ChildItem -Force -Name' in E:\众墅之
家AI赋能平台底座
 succeeded in 40ms:
E:/众墅之家AI赋能平台底座
76da2a2f (HEAD -> main) ZS-SEC-008: 补齐参数校验、上下文头解析与请求资源限制（@RequestBody 参数级 @Vali
d 契约 + parseTenantIdHeader 严格解析 + 过滤器异常统一出口 + JSON 缓冲上限）
 README.md                                          |   4 +-
 ...273\273\345\212\241\346\270\205\345\215\225.md" |   8 +-
 ...231\220\345\210\266\350\247\204\350\214\203.md" | 102 ++++++++
 .../tenant/core/web/TenantContextWebFilter.java    |  14 +-
 .../zszj/framework/web/config/WebProperties.java   |  18 ++
 .../web/config/ZszjWebAutoConfiguration.java       |   6 +-
 .../web/core/filter/CacheRequestBodyFilter.java    |  26 ++
 .../framework/web/core/util/WebFrameworkUtils.java |  48 +++-
 .../core/filter/CacheRequestBodyFilterTest.java    | 144 +++++++++++
 .../web/core/util/WebFrameworkUtilsTest.java       | 124 ++++++++++
 .../admin/social/SocialClientController.java       |   2 +-
 .../admin/social/SocialUserController.java         |   2 +-
 .../cn/zszj/server/ValidationContractTest.java     | 262 +++++++++++++++++++++
 13 files changed, 745 insertions(+), 15 deletions(-)
Path           
----           
E:\众墅之家AI赋能平台底座
.git
.github
.omx
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



exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "git 
-c core.quotepath=false show --format=fuller --name-only 76da2a2f; Get-ChildIte
m -LiteralPath E:\\ -Force -File -Filter 'AGENTS*'; Get-ChildItem -Path . -Filt
er 'AGENTS*' -Recurse -Force -File | Select-Object -ExpandProperty FullName; if
 (Test-Path .codex) { Get-ChildItem .codex -Force }" in E:\众墅之家AI赋能平台底座
 succeeded in 3670ms:
E:\众墅之家AI赋能平台底座\apps\zhongshu-admin-web\node_modules\.pnpm\caniuse-lite@1.0.300
01775\node_modules\caniuse-lite\data\agents.js
E:\众墅之家AI赋能平台底座\apps\zhongshu-admin-web\node_modules\.pnpm\caniuse-lite@1.0.300
01775\node_modules\caniuse-lite\dist\unpacker\agents.js
E:\众墅之家AI赋能平台底座\apps\zhongshu-admin-web\node_modules\.pnpm\caniuse-lite@1.0.300
01791\node_modules\caniuse-lite\data\agents.js
E:\众墅之家AI赋能平台底座\apps\zhongshu-admin-web\node_modules\.pnpm\caniuse-lite@1.0.300
01791\node_modules\caniuse-lite\dist\unpacker\agents.js
E:\众墅之家AI赋能平台底座\apps\zhongshu-miniapp\node_modules\.pnpm\caniuse-lite@1.0.30001
739\node_modules\caniuse-lite\data\agents.js
E:\众墅之家AI赋能平台底座\apps\zhongshu-miniapp\node_modules\.pnpm\caniuse-lite@1.0.30001
739\node_modules\caniuse-lite\dist\unpacker\agents.js
E:\众墅之家AI赋能平台底座\apps\zhongshu-miniapp\node_modules\.pnpm\caniuse-lite@1.0.30001
749\node_modules\caniuse-lite\data\agents.js
E:\众墅之家AI赋能平台底座\apps\zhongshu-miniapp\node_modules\.pnpm\caniuse-lite@1.0.30001
749\node_modules\caniuse-lite\dist\unpacker\agents.js

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "git 
-c core.quotepath=false show --format= --no-ext-diff 76da2a2f -- '*.java'; Get-
Content services/zhongshu-core/docs/参数校验与请求资源限制规范.md -Encoding UTF8; Get-ChildI
tem -LiteralPath C:\\Users\\Administrator\\.codex -Name -ErrorAction SilentlyCo
ntinue; if (Test-Path C:\\Users\\Administrator\\.codex\\config.toml) { Select-S
tring -Path C:\\Users\\Administrator\\.codex\\config.toml -Pattern 'project_doc
|fallback' }" in E:\众墅之家AI赋能平台底座
 succeeded in 0ms:
diff --git a/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-biz
-tenant/src/main/java/cn/zszj/framework/tenant/core/web/TenantContextWebFilter.
java b/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-biz-tenan
t/src/main/java/cn/zszj/framework/tenant/core/web/TenantContextWebFilter.java
index 5c26e7f4..cb17576b 100644
--- a/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-biz-tenant
/src/main/java/cn/zszj/framework/tenant/core/web/TenantContextWebFilter.java
+++ b/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-biz-tenant
/src/main/java/cn/zszj/framework/tenant/core/web/TenantContextWebFilter.java
@@ -1,5 +1,7 @@
 package cn.zszj.framework.tenant.core.web;
 
+import cn.zszj.framework.common.exception.ServiceException;
+import cn.zszj.framework.common.pojo.CommonResult;
 import cn.zszj.framework.tenant.core.context.TenantContextHolder;
 import cn.zszj.framework.web.core.util.WebFrameworkUtils;
 import org.springframework.web.filter.OncePerRequestFilter;
@@ -21,8 +23,16 @@ public class TenantContextWebFilter extends OncePerRequestFi
lter {
     @Override
     protected void doFilterInternal(HttpServletRequest request, HttpServletRes
ponse response, FilterChain chain)
             throws ServletException, IOException {
-        // 设置
-        Long tenantId = WebFrameworkUtils.getTenantId(request);
+        // 设置。ZS-SEC-008：上下文头严格解析——畸形/溢出 tenant-id 会抛受控 ServiceException（业务码 4
00）。
+        // 本过滤器位于 MVC 之外，GlobalExceptionHandler（@RestControllerAdvice）无法捕获，需就地
转统一出口 writeJSON，
+        // 避免 NumberFormatException / ServiceException 逃逸为容器 500 + 栈泄露。
+        Long tenantId;
+        try {
+            tenantId = WebFrameworkUtils.getTenantId(request);
+        } catch (ServiceException ex) {
+            WebFrameworkUtils.writeJSON(request, response, CommonResult.error(
ex.getCode(), ex.getMessage()));
+            return;
+        }
         if (tenantId != null) {
             TenantContextHolder.setTenantId(tenantId);
         }
diff --git a/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web
/src/main/java/cn/zszj/framework/web/config/WebProperties.java b/services/zhong
shu-core/zszj-framework/zszj-spring-boot-starter-web/src/main/java/cn/zszj/fram
ework/web/config/WebProperties.java
index be647392..0b1f46d5 100644
--- a/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/ma
in/java/cn/zszj/framework/web/config/WebProperties.java
+++ b/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/ma
in/java/cn/zszj/framework/web/config/WebProperties.java
@@ -27,6 +27,10 @@ public class WebProperties {
     @Valid
     private Ui adminUi;
 
+    @NotNull(message = "请求体限制不能为空")
+    @Valid
+    private RequestBody requestBody = new RequestBody();
+
     @Data
     @AllArgsConstructor
     @NoArgsConstructor
@@ -64,4 +68,18 @@ public class WebProperties {
 
     }
 
+    @Data
+    public static class RequestBody {
+
+        /**
+         * JSON 请求体缓冲上限（字节）。
+         *
+         * ZS-SEC-008：认证前 CacheRequestBodyFilter 会全量缓冲 JSON 请求体以支持重复读取。超限则在缓冲前
受控拒绝
+         * （业务码 400），防止超大 body 耗尽内存。默认 1MB；配置为 {@code <= 0} 表示不限制。
+         * 上传 / 流式接口非 JSON，已被 CacheRequestBodyFilter#shouldNotFilter 排除，不受此限影响
。
+         */
+        private long maxCacheSize = 1024 * 1024L;
+
+    }
+
 }
diff --git a/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web
/src/main/java/cn/zszj/framework/web/config/ZszjWebAutoConfiguration.java b/ser
vices/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/main/java/c
n/zszj/framework/web/config/ZszjWebAutoConfiguration.java
index bb7c8ba2..b5d04203 100644
--- a/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/ma
in/java/cn/zszj/framework/web/config/ZszjWebAutoConfiguration.java
+++ b/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/ma
in/java/cn/zszj/framework/web/config/ZszjWebAutoConfiguration.java
@@ -122,8 +122,10 @@ public class ZszjWebAutoConfiguration {
      * 创建 RequestBodyCacheFilter Bean，可重复读取请求内容
      */
     @Bean
-    public FilterRegistrationBean<CacheRequestBodyFilter> requestBodyCacheFilt
er() {
-        return createFilterBean(new CacheRequestBodyFilter(), WebFilterOrderEn
um.REQUEST_BODY_CACHE_FILTER);
+    public FilterRegistrationBean<CacheRequestBodyFilter> requestBodyCacheFilt
er(WebProperties webProperties) {
+        // ZS-SEC-008：注入 JSON 请求体缓冲上限，缓冲前受控拒绝超大 body
+        return createFilterBean(new CacheRequestBodyFilter(webProperties.getRe
questBody().getMaxCacheSize()),
+                WebFilterOrderEnum.REQUEST_BODY_CACHE_FILTER);
     }
 
     /**
diff --git a/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web
/src/main/java/cn/zszj/framework/web/core/filter/CacheRequestBodyFilter.java b/
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/main/jav
a/cn/zszj/framework/web/core/filter/CacheRequestBodyFilter.java
index 1bd75a76..d3707d67 100644
--- a/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/ma
in/java/cn/zszj/framework/web/core/filter/CacheRequestBodyFilter.java
+++ b/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/ma
in/java/cn/zszj/framework/web/core/filter/CacheRequestBodyFilter.java
@@ -1,7 +1,10 @@
 package cn.zszj.framework.web.core.filter;
 
 import cn.hutool.core.util.StrUtil;
+import cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants;
+import cn.zszj.framework.common.pojo.CommonResult;
 import cn.zszj.framework.common.util.servlet.ServletUtils;
+import cn.zszj.framework.web.core.util.WebFrameworkUtils;
 import org.springframework.web.filter.OncePerRequestFilter;
 
 import jakarta.servlet.FilterChain;
@@ -25,9 +28,32 @@ public class CacheRequestBodyFilter extends OncePerRequestFi
lter {
      */
     private static final String[] IGNORE_URIS = {"/admin/", "/actuator/"};
 
+    /**
+     * JSON 请求体缓冲上限（字节），{@code <= 0} 表示不限制。
+     *
+     * ZS-SEC-008：在缓冲前按声明的 Content-Length 约束 JSON 大小，超限受控拒绝（业务码 400），
+     * 防止认证前全量缓冲超大 body 耗尽内存。由 {@link cn.zszj.framework.web.config.WebProperti
es.RequestBody#getMaxCacheSize()} 注入。
+     */
+    private final long maxCacheSize;
+
+    public CacheRequestBodyFilter() {
+        this(-1L); // 向后兼容：无参构造不施加额外大小限制
+    }
+
+    public CacheRequestBodyFilter(long maxCacheSize) {
+        this.maxCacheSize = maxCacheSize;
+    }
+
     @Override
     protected void doFilterInternal(HttpServletRequest request, HttpServletRes
ponse response, FilterChain filterChain)
             throws IOException, ServletException {
+        // ZS-SEC-008：缓冲前约束 JSON 大小。声明的 Content-Length 超过上限则受控拒绝，不进入全量缓冲。
+        // 上传 / 流式接口非 JSON，已被 shouldNotFilter 排除，不会进入此处。
+        if (maxCacheSize > 0 && request.getContentLengthLong() > maxCacheSize)
 {
+            WebFrameworkUtils.writeJSON(request, response, CommonResult.error(
+                    GlobalErrorCodeConstants.BAD_REQUEST.getCode(), "请求体大小超过上限
"));
+            return;
+        }
         filterChain.doFilter(new CacheRequestBodyWrapper(request), response);
     }
 
diff --git a/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web
/src/main/java/cn/zszj/framework/web/core/util/WebFrameworkUtils.java b/service
s/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/main/java/cn/zs
zj/framework/web/core/util/WebFrameworkUtils.java
index d9650e8d..9dc79851 100644
--- a/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/ma
in/java/cn/zszj/framework/web/core/util/WebFrameworkUtils.java
+++ b/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/ma
in/java/cn/zszj/framework/web/core/util/WebFrameworkUtils.java
@@ -1,8 +1,10 @@
 package cn.zszj.framework.web.core.util;
 
 import cn.hutool.core.util.NumberUtil;
+import cn.hutool.core.util.StrUtil;
 import cn.zszj.framework.common.enums.TerminalEnum;
 import cn.zszj.framework.common.enums.UserTypeEnum;
+import cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants;
 import cn.zszj.framework.common.pojo.CommonResult;
 import cn.zszj.framework.common.util.servlet.ServletUtils;
 import cn.zszj.framework.web.config.WebProperties;
@@ -13,6 +15,8 @@ import org.springframework.web.context.request.RequestAttribu
tes;
 import org.springframework.web.context.request.RequestContextHolder;
 import org.springframework.web.context.request.ServletRequestAttributes;
 
+import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exc
eption0;
+
 /**
  * 专属于 web 包的工具类
  *
@@ -49,8 +53,7 @@ public class WebFrameworkUtils {
      * @return 租户编号
      */
     public static Long getTenantId(HttpServletRequest request) {
-        String tenantId = request.getHeader(HEADER_TENANT_ID);
-        return NumberUtil.isNumber(tenantId) ? Long.valueOf(tenantId) : null;
+        return parseTenantIdHeader(request.getHeader(HEADER_TENANT_ID), HEADER
_TENANT_ID);
     }
 
     /**
@@ -61,8 +64,45 @@ public class WebFrameworkUtils {
      * @return 租户编号
      */
     public static Long getVisitTenantId(HttpServletRequest request) {
-        String tenantId = request.getHeader(HEADER_VISIT_TENANT_ID);
-        return NumberUtil.isNumber(tenantId)? Long.valueOf(tenantId) : null;
+        return parseTenantIdHeader(request.getHeader(HEADER_VISIT_TENANT_ID), 
HEADER_VISIT_TENANT_ID);
+    }
+
+    /**
+     * 严格解析租户类上下文头（ZS-SEC-008）。
+     *
+     * <p>规则：缺失 / 空白 → {@code null}（视为未传递，保持既有兜底语义）；present-but-malformed
+     * （含符号、小数点、十六进制、科学计数、内嵌空白、非数字，或十进制数字串超出 {@link Long} 范围）
+     * → 抛受控 {@link cn.zszj.framework.common.exception.ServiceException}（业务码 4
00），由统一异常出口稳定拒绝、
+     * 不泄露栈。替代原 {@code NumberUtil.isNumber(...) + Long.valueOf(...)} 组合——后者对 {
@code "1.5"}、{@code "0x1F"}、
+     * {@code "1e5"} 等 isNumber 通过但 Long.valueOf 失败的输入会抛 {@link NumberFormatEx
ception}，
+     * 在 MVC 外的过滤器中逃逸为容器 500 + 栈泄露。
+     *
+     * @param rawValue   上下文头原始值
+     * @param headerName 头名，仅用于错误提示
+     * @return 合法租户编号；缺失 / 空白返回 {@code null}
+     */
+    private static Long parseTenantIdHeader(String rawValue, String headerName
) {
+        if (rawValue == null) {
+            return null;
+        }
+        String value = rawValue.trim();
+        if (value.isEmpty()) {
+            return null;
+        }
+        // 仅接受纯十进制数字（拒绝符号、小数点、十六进制、科学计数、内嵌空白等一切非数字字符）
+        for (int i = 0; i < value.length(); i++) {
+            if (!Character.isDigit(value.charAt(i))) {
+                throw exception0(GlobalErrorCodeConstants.BAD_REQUEST.getCode(
),
+                        StrUtil.format("请求头 {} 格式非法，必须为十进制非负整数", headerName));
+            }
+        }
+        try {
+            // 纯数字串但超出 Long 范围（如 99999999999999999999）→ NumberFormatException 
→ 受控 400
+            return Long.parseLong(value);
+        } catch (NumberFormatException ex) {
+            throw exception0(GlobalErrorCodeConstants.BAD_REQUEST.getCode(),
+                    StrUtil.format("请求头 {} 超出取值范围", headerName));
+        }
     }
 
     public static void setLoginUserId(ServletRequest request, Long userId) {
diff --git a/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web
/src/test/java/cn/zszj/framework/web/core/filter/CacheRequestBodyFilterTest.jav
a b/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/test
/java/cn/zszj/framework/web/core/filter/CacheRequestBodyFilterTest.java
new file mode 100644
index 00000000..4b910c8a
--- /dev/null
+++ b/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/te
st/java/cn/zszj/framework/web/core/filter/CacheRequestBodyFilterTest.java
@@ -0,0 +1,144 @@
+package cn.zszj.framework.web.core.filter;
+
+import cn.zszj.framework.web.config.WebProperties;
+import cn.zszj.framework.web.core.util.WebFrameworkUtils;
+import jakarta.servlet.ServletRequest;
+import org.junit.jupiter.api.BeforeEach;
+import org.junit.jupiter.api.DisplayName;
+import org.junit.jupiter.api.Test;
+import org.springframework.mock.web.MockFilterChain;
+import org.springframework.mock.web.MockHttpServletRequest;
+import org.springframework.mock.web.MockHttpServletResponse;
+
+import java.nio.charset.StandardCharsets;
+import java.util.Arrays;
+
+import static org.junit.jupiter.api.Assertions.assertFalse;
+import static org.junit.jupiter.api.Assertions.assertNotNull;
+import static org.junit.jupiter.api.Assertions.assertNull;
+import static org.junit.jupiter.api.Assertions.assertTrue;
+
+/**
+ * {@link CacheRequestBodyFilter} 请求体大小上限的单元测试（ZS-SEC-008 调整 #3）。
+ *
+ * <p>锁定行为：JSON 请求声明的 Content-Length 超过上限时，在缓冲前受控拒绝（业务码 400、不进入过滤链）；
+ * 合法大小 JSON 正常缓冲并放行；上限 {@code <= 0} 时不限制；上传 / 流式等非 JSON 请求被 shouldNotFilter 排
除、不受此限影响。
+ */
+public class CacheRequestBodyFilterTest {
+
+    private static final String JSON_URI = "/admin-api/test/echo";
+
+    @BeforeEach
+    public void setUp() {
+        // 初始化 WebFrameworkUtils 的静态 WebProperties（与既有 web 测试约定一致）
+        new WebFrameworkUtils(new WebProperties());
+    }
+
+    private static MockHttpServletRequest jsonRequest(int bodySize) {
+        MockHttpServletRequest request = new MockHttpServletRequest();
+        request.setMethod("POST");
+        request.setRequestURI(JSON_URI);
+        request.setContentType("application/json");
+        byte[] body = new byte[bodySize];
+        Arrays.fill(body, (byte) 'a');
+        request.setContent(body);
+        return request;
+    }
+
+    @Test
+    @DisplayName("超大 JSON（Content-Length 超上限）→ 缓冲前受控拒绝 400，不进入过滤链")
+    public void testOversizedJsonRejected() throws Exception {
+        CacheRequestBodyFilter filter = new CacheRequestBodyFilter(10);
+        MockHttpServletResponse response = new MockHttpServletResponse();
+        MockFilterChain chain = new MockFilterChain();
+
+        filter.doFilter(jsonRequest(100), response, chain);
+
+        assertNull(chain.getRequest(), "超大 body 不应进入过滤链（缓冲前拒绝）");
+        String content = response.getContentAsString();
+        assertTrue(content.contains("400"), "应写业务码 400，实际：" + content);
+        assertTrue(content.contains("请求体大小超过上限"), "应提示请求体超限，实际：" + content);
+    }
+
+    @Test
+    @DisplayName("合法大小 JSON → 正常缓冲为 Wrapper 并放行")
+    public void testNormalJsonPasses() throws Exception {
+        CacheRequestBodyFilter filter = new CacheRequestBodyFilter(1024);
+        MockFilterChain chain = new MockFilterChain();
+
+        filter.doFilter(jsonRequest(20), new MockHttpServletResponse(), chain)
;
+
+        ServletRequest passed = chain.getRequest();
+        assertNotNull(passed, "合法 JSON 应放行");
+        assertTrue(passed instanceof CacheRequestBodyWrapper, "放行请求应被包装为可重复读取的
 Wrapper");
+        assertEquals20Bytes(passed);
+    }
+
+    private static void assertEquals20Bytes(ServletRequest passed) {
+        assertTrue(passed.getContentLength() == 20, "缓冲后 Content-Length 应保持原大小
");
+    }
+
+    @Test
+    @DisplayName("上限 <= 0 → 不施加额外大小限制（向后兼容）")
+    public void testNoLimitWhenMaxNonPositive() throws Exception {
+        CacheRequestBodyFilter filter = new CacheRequestBodyFilter(-1L);
+        MockFilterChain chain = new MockFilterChain();
+
+        filter.doFilter(jsonRequest(5000), new MockHttpServletResponse(), chai
n);
+
+        assertNotNull(chain.getRequest(), "不限制时超大 body 仍放行");
+        assertTrue(chain.getRequest() instanceof CacheRequestBodyWrapper);
+    }
+
+    @Test
+    @DisplayName("非 JSON（上传 / 流式）→ shouldNotFilter 排除，超大也不受此限、原样放行")
+    public void testNonJsonSkipped() throws Exception {
+        CacheRequestBodyFilter filter = new CacheRequestBodyFilter(10);
+        MockHttpServletRequest request = new MockHttpServletRequest();
+        request.setMethod("POST");
+        request.setRequestURI("/admin-api/test/upload");
+        request.setContentType("multipart/form-data");
+        byte[] body = new byte[5000];
+        Arrays.fill(body, (byte) 'b');
+        request.setContent(body);
+        MockFilterChain chain = new MockFilterChain();
+
+        filter.doFilter(request, new MockHttpServletResponse(), chain);
+
+        ServletRequest passed = chain.getRequest();
+        assertNotNull(passed, "非 JSON 请求应放行（不被大小上限拦截）");
+        assertFalse(passed instanceof CacheRequestBodyWrapper, "非 JSON 请求不缓冲、不
包装");
+    }
+
+    @Test
+    @DisplayName("被排除 URI（/actuator/）→ shouldNotFilter 排除，不缓冲")
+    public void testIgnoredUriSkipped() throws Exception {
+        CacheRequestBodyFilter filter = new CacheRequestBodyFilter(10);
+        MockHttpServletRequest request = jsonRequest(100);
+        request.setRequestURI("/actuator/health");
+        MockFilterChain chain = new MockFilterChain();
+
+        filter.doFilter(request, new MockHttpServletResponse(), chain);
+
+        assertNotNull(chain.getRequest(), "被排除 URI 应放行");
+        assertFalse(chain.getRequest() instanceof CacheRequestBodyWrapper, "被排
除 URI 不缓冲");
+    }
+
+    @Test
+    @DisplayName("字节大小写死校验：UTF-8 编码不影响上限判定")
+    public void testUtf8BodyStillMeasuredByBytes() throws Exception {
+        CacheRequestBodyFilter filter = new CacheRequestBodyFilter(8);
+        MockHttpServletRequest request = new MockHttpServletRequest();
+        request.setMethod("POST");
+        request.setRequestURI(JSON_URI);
+        request.setContentType("application/json");
+        // 4 个中文字符 = 12 UTF-8 字节 > 8 上限
+        request.setContent("中文中文".getBytes(StandardCharsets.UTF_8));
+        MockFilterChain chain = new MockFilterChain();
+
+        filter.doFilter(request, new MockHttpServletResponse(), chain);
+
+        assertNull(chain.getRequest(), "按字节数（非字符数）判定超限");
+    }
+
+}
diff --git a/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web
/src/test/java/cn/zszj/framework/web/core/util/WebFrameworkUtilsTest.java b/ser
vices/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/test/java/c
n/zszj/framework/web/core/util/WebFrameworkUtilsTest.java
new file mode 100644
index 00000000..39db6de0
--- /dev/null
+++ b/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/te
st/java/cn/zszj/framework/web/core/util/WebFrameworkUtilsTest.java
@@ -0,0 +1,124 @@
+package cn.zszj.framework.web.core.util;
+
+import cn.zszj.framework.common.exception.ServiceException;
+import cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants;
+import org.junit.jupiter.api.DisplayName;
+import org.junit.jupiter.api.Test;
+import org.springframework.mock.web.MockHttpServletRequest;
+
+import static org.junit.jupiter.api.Assertions.assertEquals;
+import static org.junit.jupiter.api.Assertions.assertNull;
+import static org.junit.jupiter.api.Assertions.assertThrows;
+
+/**
+ * {@link WebFrameworkUtils} 上下文头严格解析的单元测试（ZS-SEC-008 调整 #2）。
+ *
+ * <p>锁定行为：缺失 / 空白 → {@code null}（视为未传递）；合法十进制非负整数 → 对应 {@link Long}；
+ * present-but-malformed（小数、十六进制、科学计数、符号、非数字、超出 Long 范围）→ 受控 {@link ServiceExc
eption}（业务码 400），
+ * 不再抛 {@link NumberFormatException} 逃逸为容器 500 + 栈泄露。
+ */
+public class WebFrameworkUtilsTest {
+
+    private static MockHttpServletRequest tenantRequest(String value) {
+        MockHttpServletRequest request = new MockHttpServletRequest();
+        if (value != null) {
+            request.addHeader(WebFrameworkUtils.HEADER_TENANT_ID, value);
+        }
+        return request;
+    }
+
+    private static MockHttpServletRequest visitTenantRequest(String value) {
+        MockHttpServletRequest request = new MockHttpServletRequest();
+        if (value != null) {
+            request.addHeader(WebFrameworkUtils.HEADER_VISIT_TENANT_ID, value)
;
+        }
+        return request;
+    }
+
+    private static void assertBadRequest(MockHttpServletRequest request) {
+        ServiceException ex = assertThrows(ServiceException.class, () -> WebFr
ameworkUtils.getTenantId(request));
+        assertEquals(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), ex.getCod
e(), "畸形上下文头应归 400");
+    }
+
+    // ========== 缺失 / 空白 → null（视为未传递，保持既有兜底语义） ==========
+
+    @Test
+    @DisplayName("缺失 tenant-id 头 → null")
+    public void testAbsentReturnsNull() {
+        assertNull(WebFrameworkUtils.getTenantId(tenantRequest(null)));
+        assertNull(WebFrameworkUtils.getVisitTenantId(visitTenantRequest(null)
));
+    }
+
+    @Test
+    @DisplayName("空白 tenant-id 头 → null")
+    public void testBlankReturnsNull() {
+        assertNull(WebFrameworkUtils.getTenantId(tenantRequest("")));
+        assertNull(WebFrameworkUtils.getTenantId(tenantRequest("   ")));
+    }
+
+    // ========== 合法十进制非负整数 → Long ==========
+
+    @Test
+    @DisplayName("合法整数 tenant-id → 对应 Long（含首尾空白 trim）")
+    public void testValidIntegerParsed() {
+        assertEquals(1L, WebFrameworkUtils.getTenantId(tenantRequest("1")));
+        assertEquals(2L, WebFrameworkUtils.getTenantId(tenantRequest("  2  "))
);
+        assertEquals(Long.MAX_VALUE, WebFrameworkUtils.getTenantId(
+                tenantRequest(String.valueOf(Long.MAX_VALUE))));
+        assertEquals(9L, WebFrameworkUtils.getVisitTenantId(visitTenantRequest
("9")));
+    }
+
+    // ========== present-but-malformed → 受控 400（原缺陷：isNumber 通过但 Long.valueOf
 抛 NumberFormatException） ==========
+
+    @Test
+    @DisplayName("小数 tenant-id（原会抛 NumberFormatException）→ 受控 400")
+    public void testDecimalRejected() {
+        assertBadRequest(tenantRequest("1.5"));
+    }
+
+    @Test
+    @DisplayName("十六进制 tenant-id → 受控 400")
+    public void testHexRejected() {
+        assertBadRequest(tenantRequest("0x1F"));
+    }
+
+    @Test
+    @DisplayName("科学计数 tenant-id（原会抛 NumberFormatException）→ 受控 400")
+    public void testScientificRejected() {
+        assertBadRequest(tenantRequest("1e5"));
+    }
+
+    @Test
+    @DisplayName("非数字 tenant-id → 受控 400")
+    public void testNonNumericRejected() {
+        assertBadRequest(tenantRequest("abc"));
+    }
+
+    @Test
+    @DisplayName("带符号 tenant-id → 受控 400（拒绝负号 / 正号）")
+    public void testSignedRejected() {
+        assertBadRequest(tenantRequest("-1"));
+        assertBadRequest(tenantRequest("+1"));
+    }
+
+    @Test
+    @DisplayName("内嵌空白 tenant-id → 受控 400")
+    public void testEmbeddedWhitespaceRejected() {
+        assertBadRequest(tenantRequest("1 2"));
+    }
+
+    @Test
+    @DisplayName("超出 Long 范围的数字串 → 受控 400（不抛 NumberFormatException）")
+    public void testOverflowRejected() {
+        assertBadRequest(tenantRequest("99999999999999999999"));
+    }
+
+    @Test
+    @DisplayName("畸形 visit-tenant-id 同样受控 400")
+    public void testVisitTenantMalformedRejected() {
+        ServiceException ex = assertThrows(ServiceException.class,
+                () -> WebFrameworkUtils.getVisitTenantId(visitTenantRequest("1
.5")));
+        assertEquals(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), ex.getCod
e());
+    }
+
+}
diff --git a/services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/mo
dule/system/controller/admin/social/SocialClientController.java b/services/zhon
gshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/controller/adm
in/social/SocialClientController.java
index e63e0bea..03c4cda3 100644
--- a/services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/sy
stem/controller/admin/social/SocialClientController.java
+++ b/services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/sy
stem/controller/admin/social/SocialClientController.java
@@ -87,7 +87,7 @@ public class SocialClientController {
     @PostMapping("/send-subscribe-message")
     @Operation(summary = "发送订阅消息") // 用于测试
     @PreAuthorize("@ss.hasPermission('system:social-client:query')")
-    public void sendSubscribeMessage(@RequestBody SocialWxaSubscribeMessageSen
dReqDTO reqDTO) {
+    public void sendSubscribeMessage(@Valid @RequestBody SocialWxaSubscribeMes
sageSendReqDTO reqDTO) {
         socialClientApi.sendWxaSubscribeMessage(reqDTO);
     }
 
diff --git a/services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/mo
dule/system/controller/admin/social/SocialUserController.java b/services/zhongs
hu-core/zszj-module-system/src/main/java/cn/zszj/module/system/controller/admin
/social/SocialUserController.java
index a004bbe3..80625e9a 100644
--- a/services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/sy
stem/controller/admin/social/SocialUserController.java
+++ b/services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/sy
stem/controller/admin/social/SocialUserController.java
@@ -46,7 +46,7 @@ public class SocialUserController {
 
     @DeleteMapping("/unbind")
     @Operation(summary = "取消社交绑定")
-    public CommonResult<Boolean> socialUnbind(@RequestBody SocialUserUnbindReq
VO reqVO) {
+    public CommonResult<Boolean> socialUnbind(@RequestBody @Valid SocialUserUn
bindReqVO reqVO) {
         socialUserService.unbindSocialUser(getLoginUserId(), UserTypeEnum.ADMI
N.getValue(), reqVO.getType(), reqVO.getOpenid());
         return CommonResult.success(true);
     }
diff --git a/services/zhongshu-core/zszj-server/src/test/java/cn/zszj/server/Va
lidationContractTest.java b/services/zhongshu-core/zszj-server/src/test/java/cn
/zszj/server/ValidationContractTest.java
new file mode 100644
index 00000000..2e415143
--- /dev/null
+++ b/services/zhongshu-core/zszj-server/src/test/java/cn/zszj/server/Validatio
nContractTest.java
@@ -0,0 +1,262 @@
+package cn.zszj.server;
+
+import org.junit.jupiter.api.Test;
+
+import java.io.IOException;
+import java.nio.charset.StandardCharsets;
+import java.nio.file.Files;
+import java.nio.file.Path;
+import java.nio.file.Paths;
+import java.util.ArrayList;
+import java.util.Collections;
+import java.util.LinkedHashMap;
+import java.util.List;
+import java.util.Map;
+import java.util.TreeMap;
+import java.util.stream.Collectors;
+import java.util.stream.Stream;
+
+import static org.junit.jupiter.api.Assertions.assertFalse;
+import static org.junit.jupiter.api.Assertions.assertTrue;
+
+/**
+ * ZS-SEC-008：{@code @RequestBody} 参数校验静态契约扫描 —— 缺口防线与例外目录。
+ *
+ * <p>不依赖 Spring 上下文（仿 {@link ApiInventoryTest} / {@link ModuleWhitelistTest} 
的静态源码扫描约定），
+ * 以启用模块（{@link ModuleWhitelist#ENABLED_MODULES}：system/infra）的 Controller 源码为
事实来源，
+ * 静态解析每个方法签名中的 {@code @RequestBody} 参数，强制其携带参数级 {@code @Valid} 或 {@code @Vali
dated}，
+ * 使 VO/DTO 上已声明的 Bean Validation 约束（{@code @NotNull}/{@code @NotEmpty}/{@code
 @InEnum} 等）在入口真正生效。
+ *
+ * <p>契约动因：类级 {@code @Validated} 只对 {@code @RequestParam}/{@code @PathVariable
} 等直接约束生效，
+ * <b>不触发</b> {@code @RequestBody} 的级联 Bean 校验——后者必须参数级 {@code @Valid}。历史上
+ * {@code SocialUserController#socialUnbind} 与 {@code SocialClientController#s
endSubscribeMessage} 的 VO/DTO
+ * 已声明约束却漏 {@code @Valid}，约束从不生效（本轮已补，见 {@link #regressionFixedGapsNowValidate
d()}）。
+ *
+ * <p>少数结构性例外以带理由的目录 {@link #VALIDATION_EXCEPTIONS} 强约束（仿 {@link ApiInventoryT
est} 的
+ * {@code ANONYMOUS_CATALOGUE}）：仅「第三方 SDK 的 VO」与「{@code @RequestBody String} 原
文回调」两类无法/不应
+ * 参数级 {@code @Valid}，逐条登记理由；新增例外须评审。目录与真实缺口双向比对：目录登记了已不存在的例外（腐化）
+ * 或出现未登记的缺口，测试均失败。
+ *
+ * <p>边界：本扫描覆盖启用模块 Controller 的 {@code @RequestBody} 参数级校验注解存在性；约束本身的充分性
+ * （字段是否都该加 {@code @NotNull} 等）、嵌套级联、分组校验以及运行时拒绝语义由 ZS-SEC-008 的组件测试与
+ * ZS-SEC-012 真实链夹具覆盖，不在此重复。
+ */
+class ValidationContractTest {
+
+    /**
+     * 参数级校验例外目录：{@code Controller#method} -> 登记理由。
+     *
+     * <p>仅两类结构性例外可豁免参数级 {@code @Valid}：① 第三方 SDK 的 VO（非本项目 ReqVO，无法/不应在本仓加约束）
；
+     * ② {@code @RequestBody String} 原文回调（String 非 bean 无法约束，body 交服务层内部解析验签）。
+     * 新增/移除必须同步此目录并评审，否则 {@link #exceptionCatalogueNotStale()} 或
+     * {@link #requestBodyParamsValidatedOrCatalogued()} 失败。
+     */
+    private static final Map<String, String> VALIDATION_EXCEPTIONS = buildExce
ptions();
+
+    // ========== 测试 ==========
+
+    @Test
+    void requestBodyParamsValidatedOrCatalogued() throws IOException {
+        Map<String, String> unvalidated = scanUnvalidatedRequestBody();
+        List<String> uncatalogued = new ArrayList<>();
+        for (Map.Entry<String, String> e : unvalidated.entrySet()) {
+            if (!VALIDATION_EXCEPTIONS.containsKey(e.getKey())) {
+                uncatalogued.add(e.getKey() + " 参数类型=" + e.getValue());
+            }
+        }
+        assertTrue(uncatalogued.isEmpty(),
+                () -> "发现 @RequestBody 参数缺参数级 @Valid/@Validated 且未登记例外，其 VO/DT
O 已声明的约束将不生效。"
+                        + "请补 @Valid（首选，与同类入口一致）或评审后在 VALIDATION_EXCEPTIONS 登记
理由：\n  "
+                        + String.join("\n  ", uncatalogued));
+    }
+
+    @Test
+    void exceptionCatalogueNotStale() throws IOException {
+        Map<String, String> unvalidated = scanUnvalidatedRequestBody();
+        for (String key : VALIDATION_EXCEPTIONS.keySet()) {
+            assertTrue(unvalidated.containsKey(key),
+                    "例外目录登记了已不存在（已补 @Valid、已删除或已改名）的条目，请同步移除并评审: " + key);
+        }
+    }
+
+    @Test
+    void regressionFixedGapsNowValidated() throws IOException {
+        Map<String, String> unvalidated = scanUnvalidatedRequestBody();
+        // ZS-SEC-008 本轮修复的两个同类真实缺口：VO/DTO 已声明约束却漏参数级 @Valid，现必须已带校验
+        assertFalse(unvalidated.containsKey("SocialUserController#socialUnbind
"),
+                "回归：SocialUserController#socialUnbind 必须携带 @Valid"
+                        + "（SocialUserUnbindReqVO 声明了 @InEnum/@NotNull/@NotEmp
ty）");
+        assertFalse(unvalidated.containsKey("SocialClientController#sendSubscr
ibeMessage"),
+                "回归：SocialClientController#sendSubscribeMessage 必须携带 @Valid"
+                        + "（SocialWxaSubscribeMessageSendReqDTO 声明了 @NotNull/@
NotEmpty）");
+    }
+
+    // ========== 扫描 ==========
+
+    /** 扫描启用模块全部 Controller，返回「Controller#method -> 未校验的 @RequestBody 参数类型」（Tr
eeMap 保证确定性顺序）。 */
+    private Map<String, String> scanUnvalidatedRequestBody() throws IOExceptio
n {
+        Map<String, String> result = new TreeMap<>();
+        for (String module : ModuleWhitelist.ENABLED_MODULES) {
+            Path srcRoot = Paths.get("..", "zszj-module-" + module, "src", "ma
in", "java");
+            assertTrue(Files.isDirectory(srcRoot), "启用模块源码根目录不存在: " + srcRoot.
toAbsolutePath());
+            try (Stream<Path> paths = Files.walk(srcRoot)) {
+                List<Path> controllers = paths
+                        .filter(p -> p.getFileName().toString().endsWith("Cont
roller.java"))
+                        .filter(p -> p.toString().contains("controller"))
+                        .sorted()
+                        .collect(Collectors.toList());
+                for (Path controller : controllers) {
+                    parseController(controller, result);
+                }
+            }
+        }
+        System.out.println("[ValidationContract] 未带参数级 @Valid 的 @RequestBody 命
中(应全部为已登记例外)=" + result);
+        return result;
+    }
+
+    private void parseController(Path file, Map<String, String> out) throws IO
Exception {
+        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
+        String controller = file.getFileName().toString().replace(".java", "")
;
+        for (int i = 0; i < lines.size(); i++) {
+            String t = lines.get(i).trim();
+            if (!isMethodStart(t)) {
+                continue;
+            }
+            // 累积完整签名（跨行参数列表）直到括号平衡，避免多行签名漏检
+            StringBuilder sig = new StringBuilder(t);
+            int balance = parenDelta(t);
+            int j = i;
+            while (balance > 0 && j + 1 < lines.size()) {
+                j++;
+                String nt = lines.get(j).trim();
+                sig.append(' ').append(nt);
+                balance += parenDelta(nt);
+            }
+            i = j; // 跳过已消费的行
+            inspectSignature(controller, sig.toString(), out);
+        }
+    }
+
+    private void inspectSignature(String controller, String sig, Map<String, S
tring> out) {
+        int open = sig.indexOf('(');
+        if (open < 0) {
+            return;
+        }
+        int close = matchingParen(sig, open);
+        if (close < 0) {
+            return;
+        }
+        String methodName = methodNameBefore(sig, open);
+        String paramList = sig.substring(open + 1, close);
+        for (String param : splitTopLevel(paramList)) {
+            if (!param.contains("@RequestBody")) {
+                continue;
+            }
+            // "@Valid" 是 "@Validated" 的子串，contains("@Valid") 同时覆盖两者
+            if (param.contains("@Valid")) {
+                continue;
+            }
+            out.put(controller + "#" + methodName, paramTypeOf(param));
+        }
+    }
+
+    // ========== 解析辅助 ==========
+
+    /** 方法签名起始行：以修饰符开头、含左括号，且排除注解/注释/字段初始化(含 '=')/字段或抽象声明(以 ';' 结尾)。 */
+    private static boolean isMethodStart(String t) {
+        if (t.startsWith("@") || t.startsWith("//") || t.startsWith("*") || t.
startsWith("/*")) {
+            return false;
+        }
+        if (!(t.startsWith("public ") || t.startsWith("protected ") || t.start
sWith("private "))) {
+            return false;
+        }
+        return t.contains("(") && !t.contains("=") && !t.endsWith(";");
+    }
+
+    private static int parenDelta(String s) {
+        int d = 0;
+        for (int k = 0; k < s.length(); k++) {
+            char c = s.charAt(k);
+            if (c == '(') {
+                d++;
+            } else if (c == ')') {
+                d--;
+            }
+        }
+        return d;
+    }
+
+    private static String methodNameBefore(String sig, int openParen) {
+        String head = sig.substring(0, openParen).trim();
+        String[] toks = head.split("\\s+");
+        String last = toks[toks.length - 1];
+        int dot = last.lastIndexOf('.');
+        return dot >= 0 ? last.substring(dot + 1) : last;
+    }
+
+    private static int matchingParen(String s, int open) {
+        int depth = 0;
+        for (int k = open; k < s.length(); k++) {
+            char c = s.charAt(k);
+            if (c == '(') {
+                depth++;
+            } else if (c == ')') {
+                depth--;
+                if (depth == 0) {
+                    return k;
+                }
+            }
+        }
+        return -1;
+    }
+
+    /** 按顶层逗号切分参数列表，尊重 ()/<> /[] 嵌套（泛型如 Map&lt;String,String&gt; 不被误切）。 */
+    private static List<String> splitTopLevel(String paramList) {
+        List<String> params = new ArrayList<>();
+        int depth = 0;
+        StringBuilder cur = new StringBuilder();
+        for (int k = 0; k < paramList.length(); k++) {
+            char c = paramList.charAt(k);
+            if (c == '(' || c == '<' || c == '[') {
+                depth++;
+            } else if (c == ')' || c == '>' || c == ']') {
+                depth--;
+            }
+            if (c == ',' && depth == 0) {
+                params.add(cur.toString().trim());
+                cur.setLength(0);
+            } else {
+                cur.append(c);
+            }
+        }
+        if (cur.toString().trim().length() > 0) {
+            params.add(cur.toString().trim());
+        }
+        return params;
+    }
+
+    /** 提取参数类型（剥离注解与参数名，仅用于失败信息定位）。 */
+    private static String paramTypeOf(String param) {
+        String p = param.replaceAll("@\\w+(\\([^)]*\\))?", "").trim();
+        String[] toks = p.split("\\s+");
+        return toks.length >= 1 && !toks[0].isEmpty() ? toks[0] : p;
+    }
+
+    // ========== 例外目录（理由） ==========
+
+    private static Map<String, String> buildExceptions() {
+        Map<String, String> m = new LinkedHashMap<>();
+        // 第三方验证码 SDK VO：非本项目 ReqVO，无法/不应在本仓加约束，校验由 CaptchaService 内部负责；认证前 @P
ermitAll + @TenantIgnore
+        m.put("CaptchaController#get",
+                "com.anji.captcha 第三方 SDK 的 CaptchaVO，非本项目 ReqVO，校验由 CaptchaSe
rvice 内部负责；认证前 @PermitAll 端点");
+        m.put("CaptchaController#check",
+                "com.anji.captcha 第三方 SDK 的 CaptchaVO，非本项目 ReqVO，校验由 CaptchaSe
rvice 内部负责；认证前 @PermitAll 端点");
+        // @RequestBody String 原文回调：String 非 bean 无法参数级校验，body 交 SmsSendServic
e 内部解析验签；@PermitAll 外部平台回调
+        m.put("SmsCallbackController#receiveHuaweiSmsStatus",
+                "@RequestBody String 原文（华为云外部回调），String 非 bean 无法参数级校验，body 交 
SmsSendService 内部解析验签");
+        m.put("SmsCallbackController#receiveQiniuSmsStatus",
+                "@RequestBody String 原文（七牛云外部回调），String 非 bean 无法参数级校验，body 交 
SmsSendService 内部解析验签");
+        return Collections.unmodifiableMap(m);
+    }
+
+}
# 参数校验与请求资源限制规范（ZS-SEC-008）

> 建立日期：2026-09-10。基线：ZS-SEC-002（接口分类与匿名白名单）、ZS-SEC-005（统一错误响应）完成后；与 ZS-SEC-003（
Token 传输）、ZS-SEC-007（日志脱敏）同属 B03 接口与安全链路。
> 性质：登记底座**入口参数校验、上下文头解析与请求资源限制契约**——四类调整：① `@RequestBody` 参数级 `@Valid` 校验契约（含静
态防线 [ValidationContractTest](../zszj-server/src/test/java/cn/zszj/server/Valida
tionContractTest.java)）；② 上下文头（`tenant-id` / `visit-tenant-id`）严格解析；③ 认证前 JSON 
请求体缓冲大小上限；④ 过滤器层异常纳入统一出口。附 XSS / 富文本边界（§6）。
> 复用边界：**Token 传输解析**归 [Token传输与连接凭据规范](Token传输与连接凭据规范.md)（ZS-SEC-003）；**错误响应语义
 / 状态码**归 [错误响应与状态码矩阵](错误响应与状态码矩阵.md)（ZS-SEC-005）；**日志脱敏**归 [日志脱敏策略](日志脱敏策略.md)
（ZS-SEC-007）；**接口分类与匿名白名单**归 [接口清单与匿名白名单](接口清单与匿名白名单.md)（ZS-SEC-002）；**限流**归 ZS
-SEC-010；**ID / 时间 / 分页 / 版本兼容合同**归 ZS-SEC-009；**网关 / 容器层大小与超时限制**、真实链端到端拒绝归 ZS
-SEC-012.B。全局字符串清洗（XSS filter）**不当作**权限控制或 SQL 防注入。

## 1. 现状证据与缺陷

| 证据 | 现状 | 缺陷 / 待验证 |
|---|---|---|
| [UserController][E48] | 使用 `@Validated`，分页 `PageParam` 已有 `@Min`/`@Max` 上下限 |
 正面现状；其余入口 DTO 嵌套 / 集合长度 / 枚举需逐入口验证 |
| [WebFrameworkUtils][E47] `getTenantId` / `getVisitTenantId` | 原用 `NumberUtil.
isNumber(...)` 先判「数字」再 `Long.valueOf(...)` | `isNumber` 对 `"1.5"`/`"0x1F"`/`"1e
5"` 返回 true，但 `Long.valueOf` 抛 `NumberFormatException`；在 MVC 外的过滤器链逃逸为**容器 500 
+ 栈泄露** |
| [CacheRequestBodyFilter][E46] | 认证前全量缓冲 JSON 请求体以支持重复读取（`CacheRequestBodyWrap
per`） | 该层**未见独立大小限制**；超大 body 可耗尽内存。网关 / 容器限制尚未核验，不断言整套部署完全无限制 |
| [SocialUserController](../zszj-module-system/src/main/java/cn/zszj/module/sys
tem/controller/admin/social/SocialUserController.java) `#socialUnbind`、[SocialC
lientController](../zszj-module-system/src/main/java/cn/zszj/module/system/cont
roller/admin/social/SocialClientController.java) `#sendSubscribeMessage` | VO /
 DTO 已声明 Bean Validation 约束（`@InEnum`/`@NotNull`/`@NotEmpty`） | 漏**参数级** `@Vali
d`；类级 `@Validated` **不触发** `@RequestBody` 级联校验，约束从不生效 |
| [application.yaml](../zszj-server/src/main/resources/application.yaml) `zszj.
xss.enable` | `false`（关闭全局 XSS 清洗，`exclude-urls` 仅演示） | 关闭不等同已证明存在可执行 XSS；富文本清洗
与前端输出编码边界待明确 |

## 2. 上下文头严格解析（调整 #2·前半）

[WebFrameworkUtils](../zszj-framework/zszj-spring-boot-starter-web/src/main/jav
a/cn/zszj/framework/web/core/util/WebFrameworkUtils.java) 新增 `parseTenantIdHead
er(rawValue, headerName)`，`getTenantId` / `getVisitTenantId` 均改调它。规则（缺失 / 畸形统一处
理，替代旧 `NumberUtil.isNumber + Long.valueOf`）：

| 输入 | 行为 | 判定 |
|---|---|---|
| 缺失 / `null` | 返回 `null` | 视为未传递，保持既有兜底语义 |
| 空白 / 纯空格（trim 后空） | 返回 `null` | 同上 |
| 合法十进制非负整数（含首尾空白 trim） | 返回对应 `Long` | `"1"`→`1L`；`"  2  "`→`2L`；`Long.MAX_VAL
UE` 原样解析 |
| 含符号 / 小数点 / 十六进制 / 科学计数 / 内嵌空白 / 非数字 | 抛受控 `ServiceException`（业务码 400） | 逐字符 
`Character.isDigit` 校验，`"-1"`/`"+1"`/`"1.5"`/`"0x1F"`/`"1e5"`/`"1 2"`/`"abc"` 全
拒 |
| 纯数字串但超出 `Long` 范围 | 抛受控 `ServiceException`（业务码 400） | `Long.parseLong` 捕 `Num
berFormatException` 兜住溢出，如 `"99999999999999999999"` |

畸形 / 溢出**不再抛** `NumberFormatException`，改抛受控 `ServiceException`（`GlobalErrorCode
Constants.BAD_REQUEST`），错误提示含头名，不泄露栈。

## 3. 过滤器层异常纳入统一出口（调整 #2·后半）

[TenantContextWebFilter](../zszj-framework/zszj-spring-boot-starter-biz-tenant/
src/main/java/cn/zszj/framework/tenant/core/web/TenantContextWebFilter.java) 位于
 MVC **之外**，`GlobalExceptionHandler`（`@RestControllerAdvice`）捕获不到过滤器抛出的 `Servic
eException`。改造：`try` 调 `WebFrameworkUtils.getTenantId`，`catch (ServiceException
)` 就地 `WebFrameworkUtils.writeJSON(CommonResult.error(code, msg))` 并 `return`——
与 MVC 内错误响应结构一致（见 [错误响应与状态码矩阵](错误响应与状态码矩阵.md)）。`tenantId != null` 才 `setTenantI
d`；`finally` 仍 `TenantContextHolder.clear()`，异常路径提前 `return` 不设置、不残留上下文。

## 4. JSON 请求体缓冲大小上限（调整 #3）

[WebProperties.RequestBody](../zszj-framework/zszj-spring-boot-starter-web/src/
main/java/cn/zszj/framework/web/config/WebProperties.java) 新增 `maxCacheSize`（默认
 **1MB** = `1024 * 1024L`，`<= 0` 表示不限制，配置键 `zszj.web.request-body.max-cache-siz
e`；外层 `@NotNull @Valid`）。[CacheRequestBodyFilter](../zszj-framework/zszj-spring
-boot-starter-web/src/main/java/cn/zszj/framework/web/core/filter/CacheRequestB
odyFilter.java) `doFilterInternal` **缓冲前**判定：

- `maxCacheSize > 0 && request.getContentLengthLong() > maxCacheSize` → `writeJ
SON` 业务码 400「请求体大小超过上限」，**不进入过滤链、不全量缓冲**。
- 按声明的 **Content-Length 字节数**（非字符数）判定；UTF-8 多字节按字节计（`"中文中文"` = 12 字节）。
- 上传 / 流式（`multipart/form-data` 等非 JSON）与被排除 URI（`/admin/`、`/actuator/`）由 `shou
ldNotFilter` 排除，**不受此限**、原样放行不缓冲。
- 向后兼容：无参构造委托 `-1L`（不限制）。[ZszjWebAutoConfiguration](../zszj-framework/zszj-spri
ng-boot-starter-web/src/main/java/cn/zszj/framework/web/config/ZszjWebAutoConfi
guration.java) 注入 `webProperties.getRequestBody().getMaxCacheSize()`。

**读取超时 / 网关容器限制**：本轮只在应用层缓冲前设限；网关 / 容器层大小与超时限制尚未核验（不断言整套部署完全无限制），归 ZS-SEC-012.B
 真实链验收。

## 5. `@RequestBody` 参数级 `@Valid` 校验契约（调整 #1）

**契约动因**：类级 `@Validated` 只对 `@RequestParam` / `@PathVariable` 等直接约束生效，**不触发** `
@RequestBody` 的级联 Bean 校验——后者必须**参数级** `@Valid` / `@Validated`。本轮修复两个同类真实缺口（按 Z
S-SEC-007「同模块同缺陷模式一并收敛」先例修复而非豁免）：

| 入口 | VO / DTO | 已声明约束 | 修复 |
|---|---|---|---|
| `SocialUserController#socialUnbind` | `SocialUserUnbindReqVO` | `@InEnum` / `
@NotNull` / `@NotEmpty` | 补 `@Valid` |
| `SocialClientController#sendSubscribeMessage` | `SocialWxaSubscribeMessageSen
dReqDTO` | `@NotNull userId` / `@NotNull userType` / `@NotEmpty templateTitle` 
| 补 `@Valid` |

**静态防线** [ValidationContractTest](../zszj-server/src/test/java/cn/zszj/server/V
alidationContractTest.java)（zszj-server，仿 [ApiInventoryTest] / [ModuleWhitelist
Test] 的静态源码扫描约定，不依赖 Spring 上下文）：

- 扫描启用模块（`ModuleWhitelist.ENABLED_MODULES`：system / infra）全部 Controller，强制每个 `@
RequestBody` 参数携带参数级 `@Valid` / `@Validated`（`"@Valid"` 是 `"@Validated"` 子串，`co
ntains("@Valid")` 同覆盖两者）。
- **4 条带理由例外目录**（仅两类结构性例外）：`CaptchaController#get` / `#check`（`com.anji.captcha
` 第三方 SDK 的 `CaptchaVO`，非本项目 ReqVO，校验由 `CaptchaService` 内部负责；认证前 `@PermitAll`）；
`SmsCallbackController#receiveHuaweiSmsStatus` / `#receiveQiniuSmsStatus`（`@Req
uestBody String` 原文回调，String 非 bean 无法参数级校验，body 交 `SmsSendService` 内部解析验签）。
- **双向防腐**：出现未登记缺口 → 失败；目录登记了已不存在的条目（已补 `@Valid` / 已删 / 已改名的腐化）→ 失败。**回归护栏**锁 `
socialUnbind` + `sendSubscribeMessage` 已带 `@Valid`。
- **扫描口径**：`@RequestBody` 参数级校验注解**存在性**；约束充分性（字段是否都该加 `@NotNull`）、嵌套级联、分组校验、运行
时拒绝语义由组件测试与 ZS-SEC-012 真实链夹具覆盖，不在此重复。

## 6. XSS 与富文本边界（调整 #4）

主配置 `zszj.xss.enable=false`（[application.yaml](../zszj-server/src/main/resource
s/application.yaml) L282，`exclude-urls` 仅演示）；存在 `ZszjXssAutoConfiguration`，可按需开
启全局请求参数清洗。边界原则：

1. **全局字符串清洗（XSS filter）不是权限控制，也不是 SQL 防注入**——SQL 注入由参数化查询 / MyBatis 承接，权限由 ZS-
PERM 承接，二者不得依赖 XSS 开关。
2. **富文本**（如公告 [NoticeController]，见 [05 文档 §15.1](../../../docs/05-底座模块分析与开发任务清
单.md) SYS-NOTICE）清洗与前端输出编码是**两端协作边界**：后端按需清洗存储、前端渲染时输出编码；不把未清洗富文本直接 `innerHTML`
。
3. 关闭全局清洗**不等同**已证明存在可执行 XSS；开启也**不替代**逐入口的输出编码。
4. 逐入口 DTO 嵌套、集合长度、ID / 枚举 / 字符串、批量对象、白名单字段校验由 §5 契约 + 各入口组件测试覆盖。

## 7. 验收对齐（05 文档 ZS-SEC-008 卡）

对齐 [05 文档](../../../docs/05-底座模块分析与开发任务清单.md) ZS-SEC-008 卡验收标准：

- **畸形 / 溢出上下文头**被稳定拒绝、不泄露栈 → §2 严格解析 + §3 统一出口（受控 400）。
- **超大 JSON** → §4 缓冲前受控拒绝 400。
- **缺参数 / 非法枚举** → §5 参数级 `@Valid` 触发 Bean Validation 拒绝。
- **超量集合** → 由各入口 VO 的集合约束（`@Size` 等）承接（§5 口径 + 组件测试）。
- **合法富文本、分页、上传不损坏** → §4 `shouldNotFilter` 排除上传；§6 富文本边界。
- **普通用户不能伪造租户 / 审计字段** → §2 严格解析 + tenant 上下文既有语义。
- **线程复用和异常退出无上下文残留** → §3 `finally clear()`，异常路径提前 `return` 不设置残留。

## 8. 测试证据

| 模块 | 测试 | 用例 | 结果 |
|---|---|---|---|
| zszj-spring-boot-starter-web | [WebFrameworkUtilsTest](../zszj-framework/zszj
-spring-boot-starter-web/src/test/java/cn/zszj/framework/web/core/util/WebFrame
workUtilsTest.java)（`MockHttpServletRequest` 纯单测） | 11 | BUILD SUCCESS |
| zszj-spring-boot-starter-web | [CacheRequestBodyFilterTest](../zszj-framework
/zszj-spring-boot-starter-web/src/test/java/cn/zszj/framework/web/core/filter/C
acheRequestBodyFilterTest.java)（Mock 过滤链纯单测） | 6 | BUILD SUCCESS |
| zszj-server | [ValidationContractTest](../zszj-server/src/test/java/cn/zszj/s
erver/ValidationContractTest.java)（静态源码扫描，无 Spring 上下文） | 3 | BUILD SUCCESS |

- **WebFrameworkUtilsTest 11 用例**：缺失 / 空白→`null`、合法整数（含 trim / `MAX_VALUE`）、小数 
/ 十六进制 / 科学计数 / 非数字 / 带符号 / 内嵌空白 / 溢出→受控 400、`visit-tenant` 畸形→400。
- **CacheRequestBodyFilterTest 6 用例**：超大 JSON 缓冲前拒绝 400、合法 JSON 缓冲放行 `Wrapper`、
上限 `<= 0` 不限制、非 JSON（`multipart`）`shouldNotFilter` 排除、被排除 URI（`/actuator/`）不缓冲、
UTF-8 按字节判定。
- **ValidationContractTest 3 用例**：`requestBodyParamsValidatedOrCatalogued`（未校验必
须在目录）、`exceptionCatalogueNotStale`（目录防腐）、`regressionFixedGapsNowValidated`（锁两缺口
已修复）；扫描输出恰好 4 个已登记例外。
- **编译 / 门禁**：`mvn -pl :zszj-server -am "-Dtest=ValidationContractTest" "-Dsure
fire.failIfNoSpecifiedTests=false" test` 3/3，反应堆 20 模块全 SUCCESS（含 system / web 
/ biz-tenant 编译验证 `@Valid` 与过滤器改动）；`node scripts/ops/run-local-gates.mjs --fast
`；`node scripts/gov/verify-docs.mjs` 0 issue。

## 9. 边界与待验收说明

1. 本轮为**代码级入口校验 / 解析 / 资源限制规范化 + 单元 / 静态契约测试**；网关 / 容器层大小与超时限制、真实链畸形输入端到端拒绝在真实环
境验收归 ZS-SEC-012.B。
2. **限流**（登录 / 刷新 / 验证 / 导出等高风险入口）归 ZS-SEC-010；**ID / 时间 / 分页 / 版本兼容合同**归 ZS-SE
C-009；本规范只管入口参数校验、上下文头解析与请求体缓冲大小。
3. `ValidationContractTest` 覆盖启用模块（system / infra）Controller 的 `@RequestBody` 参
数级校验注解存在性；member 等未启用 / 不在 reactor 模块随其启用纳入。
4. **XSS 全局清洗开关**（`zszj.xss.enable`）生产取值、富文本清洗与前端输出编码端到端归 ZS-SEC-012.B 及各业务入口（如
公告 SYS-NOTICE）验收。
5. 本记录不表示任何主任务已验收。
.chatgpt-projects
.sandbox
.sandbox-bin
.sandbox-secrets
.tmp
ambient-suggestions
browser
cache
computer-use
dictation-history
generated_images
node_repl
pets
plugins
rollout-migrations
sessions
skills
sqlite
thread-writer-locks
tmp
vendor_imports
visualizations
..codex-global-state.json.tmp-1788962413720-1cf6ff6b-f9a0-4106-a52d-7aefed7c125
d
..codex-global-state.json.tmp-1789035993448-60472157-ef54-4d1a-884d-13d58b7796b
d
.codex-global-state.json
.codex-global-state.json.bak
.sandbox_migration
AGENTS.md
auth.json
cap_sid
chrome-native-hosts-v2.json
config.toml
goals_1.sqlite
goals_1.sqlite-shm
goals_1.sqlite-wal
installation_id
logs_2.sqlite
logs_2.sqlite-shm
logs_2.sqlite-wal
memories_1.sqlite
memories_1.sqlite-shm
memories_1.sqlite-wal
models_cache.json
queue_1.sqlite
queue_1.sqlite-shm
queue_1.sqlite-wal
session_index.jsonl
state_5.sqlite
state_5.sqlite-shm
state_5.sqlite-wal
thread_history_1.sqlite
thread_history_1.sqlite-shm
thread_history_1.sqlite-wal
transcription-history.jsonl

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "Get-
Content -LiteralPath C:\\Users\\Administrator\\.codex\\AGENTS.md -Encoding UTF8
; Get-Content services/zhongshu-core/zszj-server/src/test/java/cn/zszj/server/V
alidationContractTest.java -Encoding UTF8; Get-Content services/zhongshu-core/z
szj-framework/zszj-spring-boot-starter-web/src/test/java/cn/zszj/framework/web/
core/util/WebFrameworkUtilsTest.java -Encoding UTF8" in E:\众墅之家AI赋能平台底座
 succeeded in 0ms:
package cn.zszj.server;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ZS-SEC-008：{@code @RequestBody} 参数校验静态契约扫描 —— 缺口防线与例外目录。
 *
 * <p>不依赖 Spring 上下文（仿 {@link ApiInventoryTest} / {@link ModuleWhitelistTest} 的
静态源码扫描约定），
 * 以启用模块（{@link ModuleWhitelist#ENABLED_MODULES}：system/infra）的 Controller 源码为事
实来源，
 * 静态解析每个方法签名中的 {@code @RequestBody} 参数，强制其携带参数级 {@code @Valid} 或 {@code @Valid
ated}，
 * 使 VO/DTO 上已声明的 Bean Validation 约束（{@code @NotNull}/{@code @NotEmpty}/{@code 
@InEnum} 等）在入口真正生效。
 *
 * <p>契约动因：类级 {@code @Validated} 只对 {@code @RequestParam}/{@code @PathVariable}
 等直接约束生效，
 * <b>不触发</b> {@code @RequestBody} 的级联 Bean 校验——后者必须参数级 {@code @Valid}。历史上
 * {@code SocialUserController#socialUnbind} 与 {@code SocialClientController#se
ndSubscribeMessage} 的 VO/DTO
 * 已声明约束却漏 {@code @Valid}，约束从不生效（本轮已补，见 {@link #regressionFixedGapsNowValidated
()}）。
 *
 * <p>少数结构性例外以带理由的目录 {@link #VALIDATION_EXCEPTIONS} 强约束（仿 {@link ApiInventoryTe
st} 的
 * {@code ANONYMOUS_CATALOGUE}）：仅「第三方 SDK 的 VO」与「{@code @RequestBody String} 原文
回调」两类无法/不应
 * 参数级 {@code @Valid}，逐条登记理由；新增例外须评审。目录与真实缺口双向比对：目录登记了已不存在的例外（腐化）
 * 或出现未登记的缺口，测试均失败。
 *
 * <p>边界：本扫描覆盖启用模块 Controller 的 {@code @RequestBody} 参数级校验注解存在性；约束本身的充分性
 * （字段是否都该加 {@code @NotNull} 等）、嵌套级联、分组校验以及运行时拒绝语义由 ZS-SEC-008 的组件测试与
 * ZS-SEC-012 真实链夹具覆盖，不在此重复。
 */
class ValidationContractTest {

    /**
     * 参数级校验例外目录：{@code Controller#method} -> 登记理由。
     *
     * <p>仅两类结构性例外可豁免参数级 {@code @Valid}：① 第三方 SDK 的 VO（非本项目 ReqVO，无法/不应在本仓加约束）；
     * ② {@code @RequestBody String} 原文回调（String 非 bean 无法约束，body 交服务层内部解析验签）。
     * 新增/移除必须同步此目录并评审，否则 {@link #exceptionCatalogueNotStale()} 或
     * {@link #requestBodyParamsValidatedOrCatalogued()} 失败。
     */
    private static final Map<String, String> VALIDATION_EXCEPTIONS = buildExcep
tions();

    // ========== 测试 ==========

    @Test
    void requestBodyParamsValidatedOrCatalogued() throws IOException {
        Map<String, String> unvalidated = scanUnvalidatedRequestBody();
        List<String> uncatalogued = new ArrayList<>();
        for (Map.Entry<String, String> e : unvalidated.entrySet()) {
            if (!VALIDATION_EXCEPTIONS.containsKey(e.getKey())) {
                uncatalogued.add(e.getKey() + " 参数类型=" + e.getValue());
            }
        }
        assertTrue(uncatalogued.isEmpty(),
                () -> "发现 @RequestBody 参数缺参数级 @Valid/@Validated 且未登记例外，其 VO/DTO
 已声明的约束将不生效。"
                        + "请补 @Valid（首选，与同类入口一致）或评审后在 VALIDATION_EXCEPTIONS 登记理
由：\n  "
                        + String.join("\n  ", uncatalogued));
    }

    @Test
    void exceptionCatalogueNotStale() throws IOException {
        Map<String, String> unvalidated = scanUnvalidatedRequestBody();
        for (String key : VALIDATION_EXCEPTIONS.keySet()) {
            assertTrue(unvalidated.containsKey(key),
                    "例外目录登记了已不存在（已补 @Valid、已删除或已改名）的条目，请同步移除并评审: " + key);
        }
    }

    @Test
    void regressionFixedGapsNowValidated() throws IOException {
        Map<String, String> unvalidated = scanUnvalidatedRequestBody();
        // ZS-SEC-008 本轮修复的两个同类真实缺口：VO/DTO 已声明约束却漏参数级 @Valid，现必须已带校验
        assertFalse(unvalidated.containsKey("SocialUserController#socialUnbind"
),
                "回归：SocialUserController#socialUnbind 必须携带 @Valid"
                        + "（SocialUserUnbindReqVO 声明了 @InEnum/@NotNull/@NotEmpt
y）");
        assertFalse(unvalidated.containsKey("SocialClientController#sendSubscri
beMessage"),
                "回归：SocialClientController#sendSubscribeMessage 必须携带 @Valid"
                        + "（SocialWxaSubscribeMessageSendReqDTO 声明了 @NotNull/@N
otEmpty）");
    }

    // ========== 扫描 ==========

    /** 扫描启用模块全部 Controller，返回「Controller#method -> 未校验的 @RequestBody 参数类型」（Tre
eMap 保证确定性顺序）。 */
    private Map<String, String> scanUnvalidatedRequestBody() throws IOException
 {
        Map<String, String> result = new TreeMap<>();
        for (String module : ModuleWhitelist.ENABLED_MODULES) {
            Path srcRoot = Paths.get("..", "zszj-module-" + module, "src", "mai
n", "java");
            assertTrue(Files.isDirectory(srcRoot), "启用模块源码根目录不存在: " + srcRoot.t
oAbsolutePath());
            try (Stream<Path> paths = Files.walk(srcRoot)) {
                List<Path> controllers = paths
                        .filter(p -> p.getFileName().toString().endsWith("Contr
oller.java"))
                        .filter(p -> p.toString().contains("controller"))
                        .sorted()
                        .collect(Collectors.toList());
                for (Path controller : controllers) {
                    parseController(controller, result);
                }
            }
        }
        System.out.println("[ValidationContract] 未带参数级 @Valid 的 @RequestBody 命中
(应全部为已登记例外)=" + result);
        return result;
    }

    private void parseController(Path file, Map<String, String> out) throws IOE
xception {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        String controller = file.getFileName().toString().replace(".java", "");
        for (int i = 0; i < lines.size(); i++) {
            String t = lines.get(i).trim();
            if (!isMethodStart(t)) {
                continue;
            }
            // 累积完整签名（跨行参数列表）直到括号平衡，避免多行签名漏检
            StringBuilder sig = new StringBuilder(t);
            int balance = parenDelta(t);
            int j = i;
            while (balance > 0 && j + 1 < lines.size()) {
                j++;
                String nt = lines.get(j).trim();
                sig.append(' ').append(nt);
                balance += parenDelta(nt);
            }
            i = j; // 跳过已消费的行
            inspectSignature(controller, sig.toString(), out);
        }
    }

    private void inspectSignature(String controller, String sig, Map<String, St
ring> out) {
        int open = sig.indexOf('(');
        if (open < 0) {
            return;
        }
        int close = matchingParen(sig, open);
        if (close < 0) {
            return;
        }
        String methodName = methodNameBefore(sig, open);
        String paramList = sig.substring(open + 1, close);
        for (String param : splitTopLevel(paramList)) {
            if (!param.contains("@RequestBody")) {
                continue;
            }
            // "@Valid" 是 "@Validated" 的子串，contains("@Valid") 同时覆盖两者
            if (param.contains("@Valid")) {
                continue;
            }
            out.put(controller + "#" + methodName, paramTypeOf(param));
        }
    }

    // ========== 解析辅助 ==========

    /** 方法签名起始行：以修饰符开头、含左括号，且排除注解/注释/字段初始化(含 '=')/字段或抽象声明(以 ';' 结尾)。 */
    private static boolean isMethodStart(String t) {
        if (t.startsWith("@") || t.startsWith("//") || t.startsWith("*") || t.s
tartsWith("/*")) {
            return false;
        }
        if (!(t.startsWith("public ") || t.startsWith("protected ") || t.starts
With("private "))) {
            return false;
        }
        return t.contains("(") && !t.contains("=") && !t.endsWith(";");
    }

    private static int parenDelta(String s) {
        int d = 0;
        for (int k = 0; k < s.length(); k++) {
            char c = s.charAt(k);
            if (c == '(') {
                d++;
            } else if (c == ')') {
                d--;
            }
        }
        return d;
    }

    private static String methodNameBefore(String sig, int openParen) {
        String head = sig.substring(0, openParen).trim();
        String[] toks = head.split("\\s+");
        String last = toks[toks.length - 1];
        int dot = last.lastIndexOf('.');
        return dot >= 0 ? last.substring(dot + 1) : last;
    }

    private static int matchingParen(String s, int open) {
        int depth = 0;
        for (int k = open; k < s.length(); k++) {
            char c = s.charAt(k);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return k;
                }
            }
        }
        return -1;
    }

    /** 按顶层逗号切分参数列表，尊重 ()/<> /[] 嵌套（泛型如 Map&lt;String,String&gt; 不被误切）。 */
    private static List<String> splitTopLevel(String paramList) {
        List<String> params = new ArrayList<>();
        int depth = 0;
        StringBuilder cur = new StringBuilder();
        for (int k = 0; k < paramList.length(); k++) {
            char c = paramList.charAt(k);
            if (c == '(' || c == '<' || c == '[') {
                depth++;
            } else if (c == ')' || c == '>' || c == ']') {
                depth--;
            }
            if (c == ',' && depth == 0) {
                params.add(cur.toString().trim());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        if (cur.toString().trim().length() > 0) {
            params.add(cur.toString().trim());
        }
        return params;
    }

    /** 提取参数类型（剥离注解与参数名，仅用于失败信息定位）。 */
    private static String paramTypeOf(String param) {
        String p = param.replaceAll("@\\w+(\\([^)]*\\))?", "").trim();
        String[] toks = p.split("\\s+");
        return toks.length >= 1 && !toks[0].isEmpty() ? toks[0] : p;
    }

    // ========== 例外目录（理由） ==========

    private static Map<String, String> buildExceptions() {
        Map<String, String> m = new LinkedHashMap<>();
        // 第三方验证码 SDK VO：非本项目 ReqVO，无法/不应在本仓加约束，校验由 CaptchaService 内部负责；认证前 @Pe
rmitAll + @TenantIgnore
        m.put("CaptchaController#get",
                "com.anji.captcha 第三方 SDK 的 CaptchaVO，非本项目 ReqVO，校验由 CaptchaSer
vice 内部负责；认证前 @PermitAll 端点");
        m.put("CaptchaController#check",
                "com.anji.captcha 第三方 SDK 的 CaptchaVO，非本项目 ReqVO，校验由 CaptchaSer
vice 内部负责；认证前 @PermitAll 端点");
        // @RequestBody String 原文回调：String 非 bean 无法参数级校验，body 交 SmsSendService
 内部解析验签；@PermitAll 外部平台回调
        m.put("SmsCallbackController#receiveHuaweiSmsStatus",
                "@RequestBody String 原文（华为云外部回调），String 非 bean 无法参数级校验，body 交 S
msSendService 内部解析验签");
        m.put("SmsCallbackController#receiveQiniuSmsStatus",
                "@RequestBody String 原文（七牛云外部回调），String 非 bean 无法参数级校验，body 交 S
msSendService 内部解析验签");
        return Collections.unmodifiableMap(m);
    }

}
package cn.zszj.framework.web.core.util;

import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link WebFrameworkUtils} 上下文头严格解析的单元测试（ZS-SEC-008 调整 #2）。
 *
 * <p>锁定行为：缺失 / 空白 → {@code null}（视为未传递）；合法十进制非负整数 → 对应 {@link Long}；
 * present-but-malformed（小数、十六进制、科学计数、符号、非数字、超出 Long 范围）→ 受控 {@link ServiceExce
ption}（业务码 400），
 * 不再抛 {@link NumberFormatException} 逃逸为容器 500 + 栈泄露。
 */
public class WebFrameworkUtilsTest {

    private static MockHttpServletRequest tenantRequest(String value) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (value != null) {
            request.addHeader(WebFrameworkUtils.HEADER_TENANT_ID, value);
        }
        return request;
    }

    private static MockHttpServletRequest visitTenantRequest(String value) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (value != null) {
            request.addHeader(WebFrameworkUtils.HEADER_VISIT_TENANT_ID, value);
        }
        return request;
    }

    private static void assertBadRequest(MockHttpServletRequest request) {
        ServiceException ex = assertThrows(ServiceException.class, () -> WebFra
meworkUtils.getTenantId(request));
        assertEquals(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), ex.getCode
(), "畸形上下文头应归 400");
    }

    // ========== 缺失 / 空白 → null（视为未传递，保持既有兜底语义） ==========

    @Test
    @DisplayName("缺失 tenant-id 头 → null")
    public void testAbsentReturnsNull() {
        assertNull(WebFrameworkUtils.getTenantId(tenantRequest(null)));
        assertNull(WebFrameworkUtils.getVisitTenantId(visitTenantRequest(null))
);
    }

    @Test
    @DisplayName("空白 tenant-id 头 → null")
    public void testBlankReturnsNull() {
        assertNull(WebFrameworkUtils.getTenantId(tenantRequest("")));
        assertNull(WebFrameworkUtils.getTenantId(tenantRequest("   ")));
    }

    // ========== 合法十进制非负整数 → Long ==========

    @Test
    @DisplayName("合法整数 tenant-id → 对应 Long（含首尾空白 trim）")
    public void testValidIntegerParsed() {
        assertEquals(1L, WebFrameworkUtils.getTenantId(tenantRequest("1")));
        assertEquals(2L, WebFrameworkUtils.getTenantId(tenantRequest("  2  ")))
;
        assertEquals(Long.MAX_VALUE, WebFrameworkUtils.getTenantId(
                tenantRequest(String.valueOf(Long.MAX_VALUE))));
        assertEquals(9L, WebFrameworkUtils.getVisitTenantId(visitTenantRequest(
"9")));
    }

    // ========== present-but-malformed → 受控 400（原缺陷：isNumber 通过但 Long.valueOf 
抛 NumberFormatException） ==========

    @Test
    @DisplayName("小数 tenant-id（原会抛 NumberFormatException）→ 受控 400")
    public void testDecimalRejected() {
        assertBadRequest(tenantRequest("1.5"));
    }

    @Test
    @DisplayName("十六进制 tenant-id → 受控 400")
    public void testHexRejected() {
        assertBadRequest(tenantRequest("0x1F"));
    }

    @Test
    @DisplayName("科学计数 tenant-id（原会抛 NumberFormatException）→ 受控 400")
    public void testScientificRejected() {
        assertBadRequest(tenantRequest("1e5"));
    }

    @Test
    @DisplayName("非数字 tenant-id → 受控 400")
    public void testNonNumericRejected() {
        assertBadRequest(tenantRequest("abc"));
    }

    @Test
    @DisplayName("带符号 tenant-id → 受控 400（拒绝负号 / 正号）")
    public void testSignedRejected() {
        assertBadRequest(tenantRequest("-1"));
        assertBadRequest(tenantRequest("+1"));
    }

    @Test
    @DisplayName("内嵌空白 tenant-id → 受控 400")
    public void testEmbeddedWhitespaceRejected() {
        assertBadRequest(tenantRequest("1 2"));
    }

    @Test
    @DisplayName("超出 Long 范围的数字串 → 受控 400（不抛 NumberFormatException）")
    public void testOverflowRejected() {
        assertBadRequest(tenantRequest("99999999999999999999"));
    }

    @Test
    @DisplayName("畸形 visit-tenant-id 同样受控 400")
    public void testVisitTenantMalformedRejected() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> WebFrameworkUtils.getVisitTenantId(visitTenantRequest("1.
5")));
        assertEquals(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), ex.getCode
());
    }

}

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "rg -
n \"getVisitTenantId|getTenantId\\(request\\)|class Tenant.*Filter|writeJSON|RE
QUEST_BODY_CACHE_FILTER|TENANT_CONTEXT_FILTER\" services/zhongshu-core/zszj-fra
mework; Get-Content services/zhongshu-core/zszj-framework/zszj-spring-boot-star
ter-web/src/main/java/cn/zszj/framework/web/core/filter/CacheRequestBodyWrapper
.java -Encoding UTF8; Get-Content services/zhongshu-core/zszj-framework/zszj-sp
ring-boot-starter-biz-tenant/src/main/java/cn/zszj/framework/tenant/core/securi
ty/TenantSecurityWebFilter.java -Encoding UTF8; Get-Content services/zhongshu-c
ore/zszj-framework/zszj-spring-boot-starter-biz-tenant/src/main/java/cn/zszj/fr
amework/tenant/core/web/TenantContextWebFilter.java -Encoding UTF8; Get-Content
 services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/main/ja
va/cn/zszj/framework/web/core/util/WebFrameworkUtils.java -Encoding UTF8" in E:
\众墅之家AI赋能平台底座
 succeeded in 0ms:
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-biz-tenant\src\t
est\java\cn\zszj\framework\security\SecurityFilterChainFixtureTest.java:532:   
         // 修复前：AuthenticationEntryPointImpl 用 ServletUtils.writeJSON 只写体、不登记 c
ommon_result，
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-biz-tenant\src\t
est\java\cn\zszj\framework\security\SecurityFilterChainFixtureTest.java:534:   
         // 修复后：WebFrameworkUtils.writeJSON 统一登记，访问日志按业务码 401 记录。
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\web\core\util\WebFrameworkUtilsTest.java:39:        Service
Exception ex = assertThrows(ServiceException.class, () -> WebFrameworkUtils.get
TenantId(request));
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\web\core\util\WebFrameworkUtilsTest.java:49:        assertN
ull(WebFrameworkUtils.getVisitTenantId(visitTenantRequest(null)));
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\web\core\util\WebFrameworkUtilsTest.java:68:        assertE
quals(9L, WebFrameworkUtils.getVisitTenantId(visitTenantRequest("9")));
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\web\core\util\WebFrameworkUtilsTest.java:120:              
  () -> WebFrameworkUtils.getVisitTenantId(visitTenantRequest("1.5")));
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-biz-tenant\src\t
est\java\cn\zszj\framework\security\fixture\TestControllers.java:119:        ct
x.put("visitTenantId", user != null ? user.getVisitTenantId() : null);
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-biz-tenant\src\t
est\java\cn\zszj\framework\security\fixture\TestControllers.java:183:        re
sult.put("visitTenantId", user != null ? user.getVisitTenantId() : null);
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-biz-tenant\src\t
est\java\cn\zszj\framework\security\fixture\TenantFixtureConfiguration.java:56:
        registrationBean.setOrder(WebFilterOrderEnum.TENANT_CONTEXT_FILTER);
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-security\src\mai
n\java\cn\zszj\framework\security\core\util\SecurityFrameworkUtils.java:229:   
     if (loginUser.getVisitTenantId() == null) {
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-security\src\mai
n\java\cn\zszj\framework\security\core\util\SecurityFrameworkUtils.java:233:   
     return ObjUtil.notEqual(loginUser.getVisitTenantId(), loginUser.getTenantI
d());
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-security\src\mai
n\java\cn\zszj\framework\security\core\handler\AuthenticationEntryPointImpl.jav
a:32:        WebFrameworkUtils.writeJSON(request, response, CommonResult.error(
UNAUTHORIZED));
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-security\src\mai
n\java\cn\zszj\framework\security\core\handler\AccessDeniedHandlerImpl.java:38:
        WebFrameworkUtils.writeJSON(request, response, CommonResult.error(FORBI
DDEN));
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-security\src\mai
n\java\cn\zszj\framework\security\core\filter\TokenAuthenticationFilter.java:62
:                WebFrameworkUtils.writeJSON(request, response, result);
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-security\src\mai
n\java\cn\zszj\framework\security\core\filter\TokenAuthenticationFilter.java:11
6:                .setTenantId(WebFrameworkUtils.getTenantId(request));
services/zhongshu-core/zszj-framework\zszj-common\src\main\java\cn\zszj\framewo
rk\common\enums\WebFilterOrderEnum.java:16:    int REQUEST_BODY_CACHE_FILTER = 
Integer.MIN_VALUE + 500;
services/zhongshu-core/zszj-framework\zszj-common\src\main\java\cn\zszj\framewo
rk\common\enums\WebFilterOrderEnum.java:18:    int API_ENCRYPT_FILTER = REQUEST
_BODY_CACHE_FILTER + 1;
services/zhongshu-core/zszj-framework\zszj-common\src\main\java\cn\zszj\framewo
rk\common\enums\WebFilterOrderEnum.java:22:    int TENANT_CONTEXT_FILTER = - 10
4; // 需要保证在 ApiAccessLogFilter 前面
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-biz-tenant\src\m
ain\java\cn\zszj\framework\tenant\core\web\TenantVisitContextInterceptor.java:3
2:        Long visitTenantId = WebFrameworkUtils.getVisitTenantId(request);
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-biz-tenant\src\m
ain\java\cn\zszj\framework\tenant\core\web\TenantContextWebFilter.java:21:publi
c class TenantContextWebFilter extends OncePerRequestFilter {
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-biz-tenant\src\m
ain\java\cn\zszj\framework\tenant\core\web\TenantContextWebFilter.java:27:     
   // 本过滤器位于 MVC 之外，GlobalExceptionHandler（@RestControllerAdvice）无法捕获，需就地转统一出口 
writeJSON，
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-biz-tenant\src\m
ain\java\cn\zszj\framework\tenant\core\web\TenantContextWebFilter.java:31:     
       tenantId = WebFrameworkUtils.getTenantId(request);
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-biz-tenant\src\m
ain\java\cn\zszj\framework\tenant\core\web\TenantContextWebFilter.java:33:     
       WebFrameworkUtils.writeJSON(request, response, CommonResult.error(ex.get
Code(), ex.getMessage()));
services/zhongshu-core/zszj-framework\zszj-common\src\main\java\cn\zszj\framewo
rk\common\util\servlet\ServletUtils.java:32:    public static void writeJSON(Ht
tpServletResponse response, Object object) {
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-web\src\main\jav
a\cn\zszj\framework\web\core\util\WebFrameworkUtils.java:66:    public static L
ong getVisitTenantId(HttpServletRequest request) {
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-web\src\main\jav
a\cn\zszj\framework\web\core\util\WebFrameworkUtils.java:200:    public static 
void writeJSON(HttpServletRequest request, HttpServletResponse response, Common
Result<?> result) {
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-web\src\main\jav
a\cn\zszj\framework\web\core\util\WebFrameworkUtils.java:202:        ServletUti
ls.writeJSON(response, result);
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-biz-tenant\src\m
ain\java\cn\zszj\framework\tenant\core\security\TenantSecurityWebFilter.java:35
:public class TenantSecurityWebFilter extends ApiRequestFilter {
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-biz-tenant\src\m
ain\java\cn\zszj\framework\tenant\core\security\TenantSecurityWebFilter.java:80
:                WebFrameworkUtils.writeJSON(request, response, CommonResult.er
ror(GlobalErrorCodeConstants.FORBIDDEN.getCode(),
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-biz-tenant\src\m
ain\java\cn\zszj\framework\tenant\core\security\TenantSecurityWebFilter.java:91
:                WebFrameworkUtils.writeJSON(request, response, CommonResult.er
ror(GlobalErrorCodeConstants.BAD_REQUEST.getCode(),
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-biz-tenant\src\m
ain\java\cn\zszj\framework\tenant\core\security\TenantSecurityWebFilter.java:10
0:                WebFrameworkUtils.writeJSON(request, response, result);
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-web\src\main\jav
a\cn\zszj\framework\web\core\filter\DemoFilter.java:31:        WebFrameworkUtil
s.writeJSON(request, response, CommonResult.error(DEMO_DENY));
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-biz-tenant\src\m
ain\java\cn\zszj\framework\tenant\config\ZszjTenantAutoConfiguration.java:90:  
      registrationBean.setOrder(WebFilterOrderEnum.TENANT_CONTEXT_FILTER);
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-web\src\main\jav
a\cn\zszj\framework\web\core\filter\CacheRequestBodyFilter.java:53:            
WebFrameworkUtils.writeJSON(request, response, CommonResult.error(
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-web\src\main\jav
a\cn\zszj\framework\web\config\ZszjWebAutoConfiguration.java:128:              
  WebFilterOrderEnum.REQUEST_BODY_CACHE_FILTER);
services/zhongshu-core/zszj-framework\zszj-spring-boot-starter-web\src\main\jav
a\cn\zszj\framework\encrypt\core\filter\ApiEncryptFilter.java:108:             
   WebFrameworkUtils.writeJSON(request, response, result);
package cn.zszj.framework.web.core.filter;

import cn.zszj.framework.common.util.servlet.ServletUtils;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;

/**
 *  Request Body 缓存 Wrapper
 *
 * @author 芋道源码
 */
public class CacheRequestBodyWrapper extends HttpServletRequestWrapper {

    /**
     * 缓存的内容
     */
    private final byte[] body;

    public CacheRequestBodyWrapper(HttpServletRequest request) {
        super(request);
        body = ServletUtils.getBodyBytes(request);
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(this.getInputStream()))
;
    }

    @Override
    public int getContentLength() {
        return body.length;
    }

    @Override
    public long getContentLengthLong() {
        return body.length;
    }

    @Override
    public ServletInputStream getInputStream() {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(body)
;
        // 返回 ServletInputStream
        return new ServletInputStream() {

            @Override
            public int read() {
                return inputStream.read();
            }

            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setReadListener(ReadListener readListener) {}

            @Override
            public int available() {
                return body.length;
            }

        };
    }

}
package cn.zszj.framework.tenant.core.security;

import cn.hutool.core.collection.CollUtil;
import cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.zszj.framework.common.pojo.CommonResult;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import cn.zszj.framework.tenant.config.TenantProperties;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.framework.tenant.core.service.TenantFrameworkService;
import cn.zszj.framework.web.config.WebProperties;
import cn.zszj.framework.web.core.filter.ApiRequestFilter;
import cn.zszj.framework.web.core.handler.GlobalExceptionHandler;
import cn.zszj.framework.web.core.util.WebFrameworkUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;

/**
 * 多租户 Security Web 过滤器
 * 1. 如果是登陆的用户，校验是否有权限访问该租户，避免越权问题。
 * 2. 如果请求未带租户的编号，检查是否是忽略的 URL，否则也不允许访问。
 * 3. 校验租户是合法，例如说被禁用、到期
 *
 * @author 芋道源码
 */
@Slf4j
public class TenantSecurityWebFilter extends ApiRequestFilter {

    private final TenantProperties tenantProperties;

    /**
     * 允许忽略租户的 URL 列表
     *
     * 目的：解决 <a href="https://gitee.com/zhijiantianya/yudao-cloud/issues/ICUQL9
">修改配置会导致 @TenantIgnore Controller 接口过滤失效</>
     */
    private final Set<String> ignoreUrls;

    private final AntPathMatcher pathMatcher;

    private final GlobalExceptionHandler globalExceptionHandler;
    private final TenantFrameworkService tenantFrameworkService;

    public TenantSecurityWebFilter(WebProperties webProperties,
                                   TenantProperties tenantProperties,
                                   Set<String> ignoreUrls,
                                   GlobalExceptionHandler globalExceptionHandle
r,
                                   TenantFrameworkService tenantFrameworkServic
e) {
        super(webProperties);
        this.tenantProperties = tenantProperties;
        this.ignoreUrls = ignoreUrls;
        this.pathMatcher = new AntPathMatcher();
        this.globalExceptionHandler = globalExceptionHandler;
        this.tenantFrameworkService = tenantFrameworkService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResp
onse response, FilterChain chain)
            throws ServletException, IOException {
        Long tenantId = TenantContextHolder.getTenantId();
        // 1. 登陆的用户，校验是否有权限访问该租户，避免越权问题。
        LoginUser user = SecurityFrameworkUtils.getLoginUser();
        if (user != null) {
            // 如果获取不到租户编号，则尝试使用登陆用户的租户编号
            if (tenantId == null) {
                tenantId = user.getTenantId();
                TenantContextHolder.setTenantId(tenantId);
            // 如果传递了租户编号，则进行比对租户编号，避免越权问题
            } else if (!Objects.equals(user.getTenantId(), TenantContextHolder.
getTenantId())) {
                log.error("[doFilterInternal][租户({}) User({}/{}) 越权访问租户({}) URL
({}/{})]",
                        user.getTenantId(), user.getId(), user.getUserType(),
                        TenantContextHolder.getTenantId(), request.getRequestUR
I(), request.getMethod());
                WebFrameworkUtils.writeJSON(request, response, CommonResult.err
or(GlobalErrorCodeConstants.FORBIDDEN.getCode(),
                        "您无权访问该租户的数据"));
                return;
            }
        }

        // 如果非允许忽略租户的 URL，则校验租户是否合法
        if (!isIgnoreUrl(request)) {
            // 2. 如果请求未带租户的编号，不允许访问。
            if (tenantId == null) {
                log.error("[doFilterInternal][URL({}/{}) 未传递租户编号]", request.get
RequestURI(), request.getMethod());
                WebFrameworkUtils.writeJSON(request, response, CommonResult.err
or(GlobalErrorCodeConstants.BAD_REQUEST.getCode(),
                        "请求的租户标识未传递，请进行排查"));
                return;
            }
            // 3. 校验租户是合法，例如说被禁用、到期
            try {
                tenantFrameworkService.validTenant(tenantId);
            } catch (Throwable ex) {
                CommonResult<?> result = globalExceptionHandler.allExceptionHan
dler(request, ex);
                WebFrameworkUtils.writeJSON(request, response, result);
                return;
            }
        } else { // 如果是允许忽略租户的 URL，若未传递租户编号，则默认忽略租户编号，避免报错
            if (tenantId == null) {
                TenantContextHolder.setIgnore(true);
            }
        }

        // 继续过滤
        chain.doFilter(request, response);
    }

    private boolean isIgnoreUrl(HttpServletRequest request) {
        String apiUri = request.getRequestURI().substring(request.getContextPat
h().length());
        // 快速匹配，保证性能
        if (CollUtil.contains(tenantProperties.getIgnoreUrls(), apiUri)
            || CollUtil.contains(ignoreUrls, apiUri)) {
            return true;
        }
        // 逐个 Ant 路径匹配
        for (String url : tenantProperties.getIgnoreUrls()) {
            if (pathMatcher.match(url, apiUri)) {
                return true;
            }
        }
        for (String url : ignoreUrls) {
            if (pathMatcher.match(url, apiUri)) {
                return true;
            }
        }
        return false;
    }

}
package cn.zszj.framework.tenant.core.web;

import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.common.pojo.CommonResult;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.framework.web.core.util.WebFrameworkUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 多租户 Context Web 过滤器
 * 将请求 Header 中的 tenant-id 解析出来，添加到 {@link TenantContextHolder} 中，这样后续的 DB 等操作，
可以获得到租户编号。
 *
 * @author 芋道源码
 */
public class TenantContextWebFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResp
onse response, FilterChain chain)
            throws ServletException, IOException {
        // 设置。ZS-SEC-008：上下文头严格解析——畸形/溢出 tenant-id 会抛受控 ServiceException（业务码 40
0）。
        // 本过滤器位于 MVC 之外，GlobalExceptionHandler（@RestControllerAdvice）无法捕获，需就地转
统一出口 writeJSON，
        // 避免 NumberFormatException / ServiceException 逃逸为容器 500 + 栈泄露。
        Long tenantId;
        try {
            tenantId = WebFrameworkUtils.getTenantId(request);
        } catch (ServiceException ex) {
            WebFrameworkUtils.writeJSON(request, response, CommonResult.error(e
x.getCode(), ex.getMessage()));
            return;
        }
        if (tenantId != null) {
            TenantContextHolder.setTenantId(tenantId);
        }
        try {
            chain.doFilter(request, response);
        } finally {
            // 清理
            TenantContextHolder.clear();
        }
    }

}
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

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exce
ption0;

/**
 * 专属于 web 包的工具类
 *
 * @author 芋道源码
 */
public class WebFrameworkUtils {

    private static final String REQUEST_ATTRIBUTE_LOGIN_USER_ID = "login_user_i
d";
    private static final String REQUEST_ATTRIBUTE_LOGIN_USER_TYPE = "login_user
_type";

    private static final String REQUEST_ATTRIBUTE_COMMON_RESULT = "common_resul
t";

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
        return parseTenantIdHeader(request.getHeader(HEADER_TENANT_ID), HEADER_
TENANT_ID);
    }

    /**
     * 获得访问的租户编号，从 header 中
     * 考虑到其它 framework 组件也会使用到租户编号，所以不得不放在 WebFrameworkUtils 统一提供
     *
     * @param request 请求
     * @return 租户编号
     */
    public static Long getVisitTenantId(HttpServletRequest request) {
        return parseTenantIdHeader(request.getHeader(HEADER_VISIT_TENANT_ID), H
EADER_VISIT_TENANT_ID);
    }

    /**
     * 严格解析租户类上下文头（ZS-SEC-008）。
     *
     * <p>规则：缺失 / 空白 → {@code null}（视为未传递，保持既有兜底语义）；present-but-malformed
     * （含符号、小数点、十六进制、科学计数、内嵌空白、非数字，或十进制数字串超出 {@link Long} 范围）
     * → 抛受控 {@link cn.zszj.framework.common.exception.ServiceException}（业务码 40
0），由统一异常出口稳定拒绝、
     * 不泄露栈。替代原 {@code NumberUtil.isNumber(...) + Long.valueOf(...)} 组合——后者对 {@
code "1.5"}、{@code "0x1F"}、
     * {@code "1e5"} 等 isNumber 通过但 Long.valueOf 失败的输入会抛 {@link NumberFormatExc
eption}，
     * 在 MVC 外的过滤器中逃逸为容器 500 + 栈泄露。
     *
     * @param rawValue   上下文头原始值
     * @param headerName 头名，仅用于错误提示
     * @return 合法租户编号；缺失 / 空白返回 {@code null}
     */
    private static Long parseTenantIdHeader(String rawValue, String headerName)
 {
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
                throw exception0(GlobalErrorCodeConstants.BAD_REQUEST.getCode()
,
                        StrUtil.format("请求头 {} 格式非法，必须为十进制非负整数", headerName));
            }
        }
        try {
            // 纯数字串但超出 Long 范围（如 99999999999999999999）→ NumberFormatException →
 受控 400
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
    public static void setLoginUserType(ServletRequest request, Integer userTyp
e) {
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
        Integer userType = (Integer) request.getAttribute(REQUEST_ATTRIBUTE_LOG
IN_USER_TYPE);
        if (userType != null) {
            return userType;
        }
        // 2. 其次，基于 URL 前缀的约定
        if (request.getServletPath().startsWith(properties.getAdminApi().getPre
fix())) {
            return UserTypeEnum.ADMIN.getValue();
        }
        if (request.getServletPath().startsWith(properties.getAppApi().getPrefi
x())) {
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
        return NumberUtil.parseInt(terminalValue, TerminalEnum.UNKNOWN.getTermi
nal());
    }

    public static void setCommonResult(ServletRequest request, CommonResult<?> 
result) {
        request.setAttribute(REQUEST_ATTRIBUTE_COMMON_RESULT, result);
    }

    public static CommonResult<?> getCommonResult(ServletRequest request) {
        return (CommonResult<?>) request.getAttribute(REQUEST_ATTRIBUTE_COMMON_
RESULT);
    }

    /**
     * 统一写出 {@link CommonResult} 响应，并登记到请求属性，供 API 访问日志按业务码记录结果。
     *
     * 底座 API 契约：HTTP 传输层固定 200，业务结果由 {@link CommonResult#getCode()} 表达（业务码刻意镜像
 HTTP 语义，
     * 如 401/403/429/500）。因此本方法不修改 HTTP 状态码，仅统一 filter 直接写出（认证 401 / 权限 403 / 租
户 / Token /
     * Demo / 加密）的出口：这些路径未经 MVC 的 GlobalResponseBodyHandler 登记 common_result，会导
致访问日志误记为成功。
     *
     * @param request  请求，用于登记 common_result 属性（供 ApiAccessLogFilter 读取）
     * @param response 响应，写出 JSON 体
     * @param result   业务结果
     */
    public static void writeJSON(HttpServletRequest request, HttpServletRespons
e response, CommonResult<?> result) {
        setCommonResult(request, result);
        ServletUtils.writeJSON(response, result);
    }

    @SuppressWarnings("PatternVariableCanBeUsed")
    public static HttpServletRequest getRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAt
tributes();
        if (!(requestAttributes instanceof ServletRequestAttributes)) {
            return null;
        }
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttr
ibutes) requestAttributes;
        return servletRequestAttributes.getRequest();
    }

}

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "git 
show 76da2a2f --format= -- '*Social*Controller.java'; Get-Content services/zhon
gshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/controller/adm
in/social/SocialClientController.java -Encoding UTF8; Get-Content services/zhon
gshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/controller/adm
in/social/SocialUserController.java -Encoding UTF8; Get-Content services/zhongs
hu-core/zszj-module-system/src/main/java/cn/zszj/module/system/api/social/dto/S
ocialWxaSubscribeMessageSendReqDTO.java -Encoding UTF8; rg -n 'sendSubscribeMes
sage|send-subscribe-message|socialUnbind|social-unbind' apps services/zhongshu-
core --glob '"'!**/target/**'"' --glob '"'!**/node_modules/**'"'; Get-Content s
ervices/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewor
k/common/enums/WebFilterOrderEnum.java -Encoding UTF8; Get-Content services/zho
ngshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framework/common/ut
il/servlet/ServletUtils.java -Encoding UTF8; Get-Command mvn,java,javac -ErrorA
ction SilentlyContinue" in E:\众墅之家AI赋能平台底座
 succeeded in 200ms:
apps\zhongshu-admin-web\src\views\Profile\components\UserSocial.vue:27:import {
 socialAuthRedirect, socialBind, socialUnbind } from '@/api/system/user/socialU
ser'
apps\zhongshu-admin-web\src\views\Profile\components\UserSocial.vue:87:  const 
res = await socialUnbind(row.type, row.openid)
services/zhongshu-core\zszj-module-system\src\main\java\cn\zszj\module\system\c
ontroller\admin\social\SocialUserController.java:49:    public CommonResult<Boo
lean> socialUnbind(@RequestBody @Valid SocialUserUnbindReqVO reqVO) {
services/zhongshu-core\zszj-module-system\src\main\java\cn\zszj\module\system\c
ontroller\admin\social\SocialClientController.java:87:    @PostMapping("/send-s
ubscribe-message")
services/zhongshu-core\zszj-module-system\src\main\java\cn\zszj\module\system\c
ontroller\admin\social\SocialClientController.java:90:    public void sendSubsc
ribeMessage(@Valid @RequestBody SocialWxaSubscribeMessageSendReqDTO reqDTO) {
services/zhongshu-core\zszj-module-system\src\main\java\cn\zszj\module\system\c
ontroller\admin\social\SocialClientController.http:1:### 请求 /system/social-clie
nt/send-subscribe-message 接口 => 发送测试订阅消息
services/zhongshu-core\zszj-module-system\src\main\java\cn\zszj\module\system\c
ontroller\admin\social\SocialClientController.http:2:POST {{baseUrl}}/system/so
cial-client/send-subscribe-message
services/zhongshu-core\zszj-module-system\src\main\java\cn\zszj\module\system\a
pi\social\SocialClientApiImpl.java:74:            log.warn("[sendSubscribeMessa
ge][reqDTO({}) 发送订阅消息失败，原因：没有找到订阅模板]", reqDTO);
services/zhongshu-core\zszj-module-system\src\main\java\cn\zszj\module\system\a
pi\social\SocialClientApiImpl.java:94:        socialClientService.sendSubscribe
Message(reqDTO, template.getPriTmplId(), socialUser.getOpenid());
apps\zhongshu-admin-web\src\api\system\user\socialUser.ts:16:export const socia
lUnbind = (type, openid) => {
package cn.zszj.framework.common.enums;

/**
 * Web 过滤器顺序的枚举类，保证过滤器按照符合我们的预期
 *
 *  考虑到每个 starter 都需要用到该工具类，所以放到 common 模块下的 enums 包下
 *
 * @author 芋道源码
 */
public interface WebFilterOrderEnum {

    int CORS_FILTER = Integer.MIN_VALUE;

    int TRACE_FILTER = CORS_FILTER + 1;

    int REQUEST_BODY_CACHE_FILTER = Integer.MIN_VALUE + 500;

    int API_ENCRYPT_FILTER = REQUEST_BODY_CACHE_FILTER + 1;

    // OrderedRequestContextFilter 默认为 -105，用于国际化上下文等等

    int TENANT_CONTEXT_FILTER = - 104; // 需要保证在 ApiAccessLogFilter 前面

    int API_ACCESS_LOG_FILTER = -103; // 需要保证在 RequestBodyCacheFilter 后面

    int XSS_FILTER = -102;  // 需要保证在 RequestBodyCacheFilter 后面

    // Spring Security Filter 默认为 -100，可见 org.springframework.boot.autoconfigur
e.security.SecurityProperties 配置属性类

    int TENANT_SECURITY_FILTER = -99; // 需要保证在 Spring Security 过滤器后面

    int FLOWABLE_FILTER = -98; // 需要保证在 Spring Security 过滤后面

    int DEMO_FILTER = Integer.MAX_VALUE;

}
package cn.zszj.framework.common.util.servlet;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.JakartaServletUtil;
import cn.zszj.framework.common.util.http.HttpUtils;
import cn.zszj.framework.common.util.json.JsonUtils;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.util.Map;

/**
 * 客户端工具类
 *
 * @author 芋道源码
 */
public class ServletUtils {

    /**
     * 返回 JSON 字符串
     *
     * @param response 响应
     * @param object   对象，会序列化成 JSON 字符串
     */
    @SuppressWarnings("deprecation") // 必须使用 APPLICATION_JSON_UTF8_VALUE，否则会乱码
    public static void writeJSON(HttpServletResponse response, Object object) {
        String content = JsonUtils.toJsonString(object);
        JakartaServletUtil.write(response, content, MediaType.APPLICATION_JSON_
UTF8_VALUE);
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
    public static HttpServletRequest getRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAt
tributes();
        if (!(requestAttributes instanceof ServletRequestAttributes)) {
            return null;
        }
        return ((ServletRequestAttributes) requestAttributes).getRequest();
    }

    public static String getUserAgent() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return null;
        }
        return getUserAgent(request);
    }

    public static String getClientIP() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return null;
        }
        return JakartaServletUtil.getClientIP(request);
    }

    public static boolean isJsonRequest(ServletRequest request) {
        return StrUtil.startWithIgnoreCase(request.getContentType(), MediaType.
APPLICATION_JSON_VALUE);
    }

    public static String getBody(HttpServletRequest request) {
        // 只有在 json 请求在读取，因为只有 CacheRequestBodyFilter 才会进行缓存，支持重复读取
        if (isJsonRequest(request)) {
            return JakartaServletUtil.getBody(request);
        }
        return null;
    }

    public static byte[] getBodyBytes(HttpServletRequest request) {
        // 只有在 json 请求在读取，因为只有 CacheRequestBodyFilter 才会进行缓存，支持重复读取
        if (isJsonRequest(request)) {
            return JakartaServletUtil.getBodyBytes(request);
        }
        return null;
    }

    public static String getClientIP(HttpServletRequest request) {
        return JakartaServletUtil.getClientIP(request);
    }

    public static Map<String, String> getParamMap(HttpServletRequest request) {
        return JakartaServletUtil.getParamMap(request);
    }

    public static Map<String, String> getHeaderMap(HttpServletRequest request) 
{
        return JakartaServletUtil.getHeaderMap(request);
    }

    public static void writeAttachment(HttpServletResponse response, String fil
ename, byte[] content)
            throws IOException {
        response.getOutputStream().write(content);
        response.addHeader("Content-Disposition", "attachment;filename=" + Http
Utils.encodeUtf8(filename));
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
    }

}

CommandType     Name                                               Version    S
ource                                   
-----------     ----                                               -------    -
-----                                   
Application     mvn.cmd                                            0.0.0.0    E
:\众墅之家AI赋能平台底座\tools\apache-maven-3.9...
Application     java.exe                                           17.0.20.1  E
:\众墅之家AI赋能平台底座\tools\jdk-17.0.20.1+1\...
Application     javac.exe                                          17.0.20.1  E
:\众墅之家AI赋能平台底座\tools\jdk-17.0.20.1+1\...



exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "rg -
n '@RequestBody.*=|public .*@RequestParam\\(["'^)]*=.*@RequestBody|public .*@Re
questBody.*@RequestParam'"\\(["'^)]*='"' services/zhongshu-core/zszj-module-sys
tem/src/main/java services/zhongshu-core/zszj-module-infra/src/main/java; Get-C
ontent services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-biz-tenan
t/src/main/java/cn/zszj/framework/tenant/core/web/TenantVisitContextInterceptor
.java -Encoding UTF8; Get-Content services/zhongshu-core/zszj-framework/zszj-sp
ring-boot-starter-web/src/main/java/cn/zszj/framework/web/core/filter/CacheRequ
estBodyFilter.java -Encoding UTF8; Get-Content services/zhongshu-core/zszj-fram
ework/zszj-spring-boot-starter-web/pom.xml -Encoding UTF8; Get-Content services
/zhongshu-core/pom.xml -Encoding UTF8; Get-ChildItem services/zhongshu-core/zsz
j-framework/zszj-spring-boot-starter-web/target -Name -ErrorAction SilentlyCont
inue; Get-ChildItem services/zhongshu-core -Force -Name; git -c core.quotepath=
false ls-files '*AGENTS*' '*CLAUDE*' '*review*' '*REVIEW*'" in E:\众墅之家AI赋能平台底座
 succeeded in 0ms:
package cn.zszj.framework.tenant.core.web;

import cn.hutool.core.util.ObjUtil;
import cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.security.core.service.SecurityFrameworkService;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import cn.zszj.framework.tenant.config.TenantProperties;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.framework.web.core.util.WebFrameworkUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exce
ption0;

@RequiredArgsConstructor
@Slf4j
public class TenantVisitContextInterceptor implements HandlerInterceptor {

    private static final String PERMISSION = "system:tenant:visit";

    private final TenantProperties tenantProperties;

    private final SecurityFrameworkService securityFrameworkService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse re
sponse, Object handler) {
        // 如果和当前租户编号一致，则直接跳过
        Long visitTenantId = WebFrameworkUtils.getVisitTenantId(request);
        if (visitTenantId == null) {
            return true;
        }
        if (ObjUtil.equal(visitTenantId, TenantContextHolder.getTenantId())) {
            return true;
        }
        // ZS-SEC-001.A：底座默认关闭未经批准的跨租户浏览能力。未显式开启（zszj.tenant.visit-enable=false
，默认）时，
        // 任何切换到不同目标租户的请求（含伪造头、含持有旧 system:tenant:visit 权限者）一律拒绝，且不设置 visitTena
ntId、
        // 不切换 TenantContextHolder，使 SecurityFrameworkUtils#skipPermissionCheck
() 恒为 false，功能权限与数据范围
        // 均按登录租户正常校验，杜绝越权放大。获批的受控跨组织访问由 ZS-SEC-001.B（依赖 D-09）实现。
        if (!Boolean.TRUE.equals(tenantProperties.getVisitEnable())) {
            log.warn("[preHandle][跨租户访问能力未启用，拒绝切换到 visitTenantId({})，当前租户({})]"
,
                    visitTenantId, TenantContextHolder.getTenantId());
            throw exception0(GlobalErrorCodeConstants.FORBIDDEN.getCode(), "跨租户
访问能力未启用，禁止切换租户");
        }
        // 必须是登录用户
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser == null) {
            return true;
        }

        // 校验用户是否可切换租户
        if (!securityFrameworkService.hasAnyPermissions(PERMISSION)) {
            throw exception0(GlobalErrorCodeConstants.FORBIDDEN.getCode(), "您无权
切换租户");
        }

        // 【重点】切换租户编号
        loginUser.setVisitTenantId(visitTenantId);
        TenantContextHolder.setTenantId(visitTenantId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse
 response, Object handler, Exception ex) {
        // 【重点】清理切换，换回原租户编号
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser != null && loginUser.getTenantId() != null) {
            TenantContextHolder.setTenantId(loginUser.getTenantId());
        }
    }

}
package cn.zszj.framework.web.core.filter;

import cn.hutool.core.util.StrUtil;
import cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.zszj.framework.common.pojo.CommonResult;
import cn.zszj.framework.common.util.servlet.ServletUtils;
import cn.zszj.framework.web.core.util.WebFrameworkUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Request Body 缓存 Filter，实现它的可重复读取
 *
 * @author 芋道源码
 */
public class CacheRequestBodyFilter extends OncePerRequestFilter {

    /**
     * 需要排除的 URI
     *
     * 1. 排除 Spring Boot Admin 相关请求，避免客户端连接中断导致的异常。
     *    例如说：<a href="https://github.com/YunaiV/ruoyi-vue-pro/issues/795">795 
ISSUE</a>
     */
    private static final String[] IGNORE_URIS = {"/admin/", "/actuator/"};

    /**
     * JSON 请求体缓冲上限（字节），{@code <= 0} 表示不限制。
     *
     * ZS-SEC-008：在缓冲前按声明的 Content-Length 约束 JSON 大小，超限受控拒绝（业务码 400），
     * 防止认证前全量缓冲超大 body 耗尽内存。由 {@link cn.zszj.framework.web.config.WebPropertie
s.RequestBody#getMaxCacheSize()} 注入。
     */
    private final long maxCacheSize;

    public CacheRequestBodyFilter() {
        this(-1L); // 向后兼容：无参构造不施加额外大小限制
    }

    public CacheRequestBodyFilter(long maxCacheSize) {
        this.maxCacheSize = maxCacheSize;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResp
onse response, FilterChain filterChain)
            throws IOException, ServletException {
        // ZS-SEC-008：缓冲前约束 JSON 大小。声明的 Content-Length 超过上限则受控拒绝，不进入全量缓冲。
        // 上传 / 流式接口非 JSON，已被 shouldNotFilter 排除，不会进入此处。
        if (maxCacheSize > 0 && request.getContentLengthLong() > maxCacheSize) 
{
            WebFrameworkUtils.writeJSON(request, response, CommonResult.error(
                    GlobalErrorCodeConstants.BAD_REQUEST.getCode(), "请求体大小超过上限"
));
            return;
        }
        filterChain.doFilter(new CacheRequestBodyWrapper(request), response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 1. 校验是否为排除的 URL
        String requestURI = request.getRequestURI();
        if (StrUtil.startWithAny(requestURI, IGNORE_URIS)) {
            return true;
        }

        // 2. 只处理 json 请求内容
        return !ServletUtils.isJsonRequest(request);
    }

}
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apa
che.org/xsd/maven-4.0.0.xsd">
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
            <scope>provided</scope> <!-- 解决工具类 SpringExpressionUtils 加载的时候访问不到 
org.aspectj.lang.JoinPoint 问题 -->
        </dependency>

        <dependency>
            <groupId>com.github.xiaoymin</groupId>
            <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactI
d>
        </dependency>
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-core</artifactId>
            <scope>provided</scope> <!-- 设置为 provided，主要是 GlobalExceptionHandle
r 使用 -->
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
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apa
che.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>cn.zszj</groupId>
    <artifactId>zszj</artifactId>
    <version>${revision}</version>
    <packaging>pom</packaging>
    <modules>
        <module>zszj-dependencies</module>
        <module>zszj-framework</module>
        <!-- Server 主项目 -->
        <module>zszj-server</module>
        <!-- 各种 module 拓展 -->
        <module>zszj-module-system</module>
        <module>zszj-module-infra</module>
<!--        <module>zszj-module-member</module>-->
<!--        <module>zszj-module-bpm</module>-->
<!--        <module>zszj-module-report</module>-->
<!--        <module>zszj-module-mp</module>-->
<!--        <module>zszj-module-pay</module>-->
<!--        <module>zszj-module-mall</module>-->
<!--        <module>zszj-module-crm</module>-->
<!--        <module>zszj-module-erp</module>-->
<!--        <module>zszj-module-iot</module>-->
<!--        <module>zszj-module-mes</module>-->
<!--        <module>zszj-module-wms</module>-->
<!--        <module>zszj-module-hrm</module>-->
<!--        <module>zszj-module-fms</module>-->
<!--        <module>zszj-module-pms</module>-->
<!--        <module>zszj-module-im</module>-->
<!-- 请参考 https://doc.iocoder.cn/ai/build/ 文档，完成 AI 模块的启动！！！ -->
<!--        <module>zszj-module-ai</module>-->
    </modules>

    <name>${project.artifactId}</name>
    <description>众墅之家项目基础脚手架</description>
    <url>https://github.com/YunaiV/ruoyi-vue-pro</url>

    <properties>
        <revision>2026.08-SNAPSHOT</revision>
        <!-- Maven 相关 -->
        <java.version>17</java.version>
        <maven.compiler.source>${java.version}</maven.compiler.source>
        <maven.compiler.target>${java.version}</maven.compiler.target>
        <maven-surefire-plugin.version>3.5.3</maven-surefire-plugin.version>
        <maven-compiler-plugin.version>3.14.0</maven-compiler-plugin.version>
        <flatten-maven-plugin.version>1.7.2</flatten-maven-plugin.version>
        <!-- maven-surefire-plugin 暂时无法通过 bom 的依赖读取（兼容老版本 IDEA 2024 及以前版本） -->
        <lombok.version>1.18.42</lombok.version>
        <spring.boot.version>3.5.15</spring.boot.version>
        <mapstruct.version>1.6.3</mapstruct.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>cn.zszj</groupId>
                <artifactId>zszj-dependencies</artifactId>
                <version>${revision}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <!-- maven-surefire-plugin 插件，用于运行单元测试。 -->
                <!-- 注意，需要使用 3.0.X+，因为要支持 Junit 5 版本 -->
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <version>${maven-surefire-plugin.version}</version>
                </plugin>
                <!-- maven-compiler-plugin 插件，解决 spring-boot-configuration-proc
essor + Lombok + MapStruct 组合 -->
                <!-- https://stackoverflow.com/questions/33483697/re-run-spring
-boot-configuration-annotation-processor-to-update-generated-metada -->
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>${maven-compiler-plugin.version}</version>
                    <configuration>
                        <annotationProcessorPaths>
                            <path>
                                <groupId>org.springframework.boot</groupId>
                                <artifactId>spring-boot-configuration-processor
</artifactId>
                                <version>${spring.boot.version}</version>
                            </path>
                            <path>
                                <groupId>org.projectlombok</groupId>
                                <artifactId>lombok</artifactId>
                                <version>${lombok.version}</version>
                            </path>
                            <path>
                                <!-- 确保 Lombok 生成的 getter/setter 方法能被 MapStruct
 正确识别，
                                     避免出现 No property named “xxx" exists 的编译错误 
-->
                                <groupId>org.projectlombok</groupId>
                                <artifactId>lombok-mapstruct-binding</artifactI
d>
                                <version>0.2.0</version>
                            </path>
                            <path>
                                <groupId>org.mapstruct</groupId>
                                <artifactId>mapstruct-processor</artifactId>
                                <version>${mapstruct.version}</version>
                            </path>
                        </annotationProcessorPaths>
                        <!-- 编译参数写在 arg 内，解决 Spring Boot 3.2 的 Parameter Name D
iscovery 问题 -->
                        <debug>false</debug>
                        <compilerArgs>
                            <arg>-parameters</arg>
                        </compilerArgs>
                    </configuration>
                </plugin>
                <plugin>
                    <groupId>org.codehaus.mojo</groupId>
                    <artifactId>flatten-maven-plugin</artifactId>
                </plugin>
            </plugins>
        </pluginManagement>

        <plugins>
            <!-- ZS-ENG-002：强制 JDK 17 / Maven 3.8+ 构建基线（D-02 已确认 JDK 17 维护线），
                 工具链不满足时构建明确失败，不依赖机器隐式版本 -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-enforcer-plugin</artifactId>
                <version>3.5.0</version>
                <executions>
                    <execution>
                        <id>enforce-jdk17-baseline</id>
                        <goals>
                            <goal>enforce</goal>
                        </goals>
                        <configuration>
                            <rules>
                                <requireJavaVersion>
                                    <version>[17,18)</version>
                                    <message>本底座固定 JDK 17 构建基线（D-02），请使用 JDK 17
 构建。</message>
                                </requireJavaVersion>
                                <requireMavenVersion>
                                    <version>[3.8,)</version>
                                    <message>本底座要求 Maven 3.8 及以上版本。</message>
                                </requireMavenVersion>
                            </rules>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            <!-- 统一 revision 版本 -->
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>flatten-maven-plugin</artifactId>
                <version>${flatten-maven-plugin.version}</version>
                <configuration>
                    <flattenMode>oss</flattenMode>
                    <updatePomFile>true</updatePomFile>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>flatten</goal>
                        </goals>
                        <id>flatten</id>
                        <phase>process-resources</phase>
                    </execution>
                    <execution>
                        <goals>
                            <goal>clean</goal>
                        </goals>
                        <id>flatten.clean</id>
                        <phase>clean</phase>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>

    <!-- 使用 huawei / aliyun 的 Maven 源，提升下载速度 -->
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
classes
generated-sources
generated-test-sources
maven-archiver
maven-status
surefire-reports
test-classes
zszj-spring-boot-starter-web-2026.08-SNAPSHOT.jar
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
apps/zhongshu-admin-web/.image/common/ai-preview.gif
apps/zhongshu-admin-web/.image/common/fms-preview.png
apps/zhongshu-admin-web/.image/common/hrm-preview.png
apps/zhongshu-admin-web/.image/common/im-preview-home.png
apps/zhongshu-admin-web/.image/common/im-preview-manager.png
apps/zhongshu-admin-web/.image/common/iot-preview.png
apps/zhongshu-admin-web/.image/common/mall-preview.png
apps/zhongshu-admin-web/.image/common/mes-preview.png
apps/zhongshu-admin-web/.image/common/wms-preview.png
apps/zhongshu-admin-web/src/views/fms/components/print/FmsPrintPreview.vue
apps/zhongshu-admin-web/src/views/hrm/portal/performance/assessment/review/Perf
ormanceQuotaForm.vue
apps/zhongshu-admin-web/src/views/hrm/portal/performance/assessment/review/Perf
ormanceReviewForm.vue
apps/zhongshu-admin-web/src/views/im/home/pages/conversation/components/message
/ReplyPreview.vue
apps/zhongshu-admin-web/src/views/im/manager/message/MessageContentPreview.vue
apps/zhongshu-admin-web/src/views/infra/codegen/PreviewCode.vue
apps/zhongshu-admin-web/src/views/mp/menu/components/MenuPreviewer.vue
apps/zhongshu-miniapp/docs/fms-mobile-migration-review-handoff.md
apps/zhongshu-miniapp/src/pages-hrm/performance/plan/components/review-stage-li
st.vue
apps/zhongshu-miniapp/src/pages-hrm/portal/performance/assessment/review/index.
vue
apps/zhongshu-miniapp/src/pages-im/home/conversation/message/components/group-c
ard-preview.vue
apps/zhongshu-miniapp/src/pages-im/home/conversation/message/components/reply-p
review.vue
apps/zhongshu-miniapp/src/pages-infra/codegen/preview/index.vue
apps/zhongshu-miniapp/src/pages-mes/pro/task/components/task-gantt-preview.vue
apps/zhongshu-miniapp/src/pages-mes/wm/barcode/components/barcode-preview.vue
apps/zhongshu-miniapp/src/pages-mes/wm/stocktaking/task/components/task-line-pr
eview.vue
apps/zhongshu-miniapp/src/pages-mes/wm/stocktaking/task/components/task-result-
preview.vue
apps/zhongshu-miniapp/src/pages-mp/components/media-preview.vue
docs/reviews/HANDOFF-ZS-DB-018.md
docs/reviews/HANDOFF-ZS-PERM-001.A.md
docs/reviews/HANDOFF-ZS-PERM-002.A.md
docs/reviews/HANDOFF-ZS-SEC-001.A.md
docs/reviews/HANDOFF-ZS-SEC-002.md
docs/reviews/HANDOFF-ZS-SEC-003.md
docs/reviews/HANDOFF-ZS-SEC-005.md
docs/reviews/HANDOFF-ZS-SEC-007.md
docs/reviews/HANDOFF-ZS-SEC-012.A.md
docs/reviews/README.md
docs/reviews/codex-ZS-BRAND-001.md
docs/reviews/codex-ZS-BRAND-001.raw.md
docs/reviews/codex-ZS-BRAND-002.md
docs/reviews/codex-ZS-BRAND-002.raw.md
docs/reviews/codex-ZS-BRAND-003.A.md
docs/reviews/codex-ZS-BRAND-003.A.raw.md
docs/reviews/codex-ZS-BRAND-004.A.md
docs/reviews/codex-ZS-BRAND-004.A.raw.md
docs/reviews/codex-ZS-BRAND-005.md
docs/reviews/codex-ZS-BRAND-005.raw.md
docs/reviews/codex-ZS-BRAND-006.A.md
docs/reviews/codex-ZS-BRAND-006.A.raw.md
docs/reviews/codex-ZS-SEC-003.md
docs/reviews/codex-ZS-SEC-003.raw.md
docs/reviews/codex-hotfix-A-r1.raw.md
docs/reviews/codex-hotfix-A-r2.raw.md
docs/reviews/codex-hotfix-A.md
docs/reviews/codex-hotfix-A.raw.md
docs/reviews/codex-hotfix-B-r1.raw.md
docs/reviews/codex-hotfix-B-r2.raw.md
docs/reviews/codex-hotfix-B-r3.raw.md
docs/reviews/codex-hotfix-B.md
docs/reviews/codex-hotfix-B.raw.md
docs/reviews/codex-hotfix-C.md
docs/reviews/codex-hotfix-D.md
docs/reviews/codex-hotfix-E.md
reference/donors/zhongshu-design/yudao-module-design/src/main/resources/db/migr
ation-undo/design/V20260905.205__undo_p7a_submission_review.sql
reference/donors/zhongshu-design/yudao-module-design/src/main/resources/db/migr
ation/design/V20260905.205__p7a_submission_review.sql
services/zhongshu-core/.image/common/ai-preview.gif
services/zhongshu-core/.image/common/fms-preview.png
services/zhongshu-core/.image/common/hrm-preview.png
services/zhongshu-core/.image/common/im-preview-home.png
services/zhongshu-core/.image/common/im-preview-manager.png
services/zhongshu-core/.image/common/iot-preview.png
services/zhongshu-core/.image/common/mall-preview.png
services/zhongshu-core/.image/common/mes-preview.png
services/zhongshu-core/.image/common/pms-preview.png
services/zhongshu-core/.image/common/wms-preview.png
services/zhongshu-core/zszj-module-hrm/src/main/java/cn/zszj/module/hrm/control
ler/admin/portal/performance/vo/review/HrmPortalPerformanceFillQuotaReqVO.java
services/zhongshu-core/zszj-module-hrm/src/main/java/cn/zszj/module/hrm/control
ler/admin/portal/performance/vo/review/HrmPortalPerformanceQuotaSaveReqVO.java
services/zhongshu-core/zszj-module-hrm/src/main/java/cn/zszj/module/hrm/control
ler/admin/portal/performance/vo/review/HrmPortalPerformanceReviewRejectReqVO.ja
va
services/zhongshu-core/zszj-module-hrm/src/main/java/cn/zszj/module/hrm/control
ler/admin/portal/performance/vo/review/HrmPortalPerformanceScorePreviewRespVO.j
ava
services/zhongshu-core/zszj-module-hrm/src/main/java/cn/zszj/module/hrm/control
ler/admin/portal/performance/vo/review/HrmPortalPerformanceScoreReqVO.java
services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/con
troller/admin/codegen/vo/CodegenPreviewRespVO.java
services/zhongshu-core/zszj-module-pms/src/main/java/cn/zszj/module/pms/control
ler/admin/kb/recycle/vo/PmsKnowledgeRecyclePreviewRespVO.java

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "Get-
Content services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/i
nfra/controller/admin/file/FileController.java -Encoding UTF8; rg -n '(byte\\[\
\]|String content|String file|base64|List<)' services/zhongshu-core/zszj-module
-infra/src/main/java/cn/zszj/module/infra/controller/admin --glob '*ReqVO.java'
; Get-Content services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-bi
z-tenant/src/test/java/cn/zszj/framework/security/fixture/FixtureApplication.ja
va -Encoding UTF8 -ErrorAction SilentlyContinue; Get-Content services/zhongshu-
core/zszj-framework/zszj-spring-boot-starter-biz-tenant/src/test/java/cn/zszj/f
ramework/security/SecurityFilterChainFixtureTest.java -Encoding UTF8 -TotalCoun
t 130; Get-ChildItem services/zhongshu-core/zszj-framework/zszj-spring-boot-sta
rter-web/target/surefire-reports -Name" in E:\众墅之家AI赋能平台底座
 succeeded in 0ms:
package cn.zszj.module.infra.controller.admin.file;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.zszj.framework.common.pojo.CommonResult;
import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.framework.common.util.http.HttpUtils;
import cn.zszj.framework.common.util.object.BeanUtils;
import cn.zszj.framework.tenant.core.aop.TenantIgnore;
import cn.zszj.module.infra.controller.admin.file.vo.file.*;
import cn.zszj.module.infra.dal.dataobject.file.FileDO;
import cn.zszj.module.infra.service.file.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static cn.zszj.framework.common.pojo.CommonResult.success;
import static cn.zszj.module.infra.framework.file.core.utils.FileTypeUtils.writ
eAttachment;

@Tag(name = "管理后台 - 文件存储")
@RestController
@RequestMapping("/infra/file")
@Validated
@Slf4j
public class FileController {

    @Resource
    private FileService fileService;

    @PostMapping("/upload")
    @Operation(summary = "上传文件", description = "模式一：后端上传文件")
    @Parameter(name = "file", description = "文件附件", required = true,
            schema = @Schema(type = "string", format = "binary"))
    public CommonResult<String> uploadFile(@Valid FileUploadReqVO uploadReqVO) 
throws Exception {
        MultipartFile file = uploadReqVO.getFile();
        byte[] content = IoUtil.readBytes(file.getInputStream());
        return success(fileService.createFile(content, file.getOriginalFilename
(),
                uploadReqVO.getDirectory(), file.getContentType()));
    }

    @GetMapping("/presigned-url")
    @Operation(summary = "获取文件预签名地址（上传）", description = "模式二：前端上传文件：用于前端直接上传七牛、
阿里云 OSS 等文件存储器")
    @Parameters({
            @Parameter(name = "name", description = "文件名称", required = true),
            @Parameter(name = "directory", description = "文件目录")
    })
    public CommonResult<FilePresignedUrlRespVO> getFilePresignedUrl(
            @RequestParam("name") String name,
            @RequestParam(value = "directory", required = false) String directo
ry) {
        return success(fileService.presignPutUrl(name, directory));
    }

    @PostMapping("/create")
    @Operation(summary = "创建文件", description = "模式二：前端上传文件：配合 presigned-url 接口，
记录上传了上传的文件")
    public CommonResult<Long> createFile(@Valid @RequestBody FileCreateReqVO cr
eateReqVO) {
        return success(fileService.createFile(createReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得文件")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('infra:file:query')")
    public CommonResult<FileRespVO> getFile(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(fileService.getFile(id), FileRespVO.cla
ss));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除文件")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('infra:file:delete')")
    public CommonResult<Boolean> deleteFile(@RequestParam("id") Long id) throws
 Exception {
        fileService.deleteFile(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除文件")
    @Parameter(name = "ids", description = "编号列表", required = true)
    @PreAuthorize("@ss.hasPermission('infra:file:delete')")
    public CommonResult<Boolean> deleteFileList(@RequestParam("ids") List<Long>
 ids) throws Exception {
        fileService.deleteFileList(ids);
        return success(true);
    }

    @GetMapping("/{configId}/get/**")
    @PermitAll
    @TenantIgnore
    @Operation(summary = "下载文件")
    @Parameter(name = "configId", description = "配置编号", required = true)
    public void getFileContent(HttpServletRequest request,
                               HttpServletResponse response,
                               @PathVariable("configId") Long configId) throws 
Exception {
        // 获取请求的路径
        String path = StrUtil.subAfter(request.getRequestURI(), "/get/", false)
;
        if (StrUtil.isEmpty(path)) {
            throw new IllegalArgumentException("结尾的 path 路径必须传递");
        }
        // 解码，解决中文、%、+ 等特殊字符路径的问题
        // https://gitee.com/zhijiantianya/ruoyi-vue-pro/pulls/807/
        // https://gitee.com/zhijiantianya/ruoyi-vue-pro/pulls/1432/
        path = HttpUtils.decodeUrlPath(path);

        // 读取内容
        byte[] content = fileService.getFileContent(configId, path);
        if (content == null) {
            log.warn("[getFileContent][configId({}) path({}) 文件不存在]", configId,
 path);
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return;
        }
        FileDO file = fileService.getFileByConfigIdAndPath(configId, path);
        String filename = file != null && StrUtil.isNotEmpty(file.getName()) ? 
file.getName() : FileUtil.getName(path);
        writeAttachment(response, filename, content);
    }

    @GetMapping("/page")
    @Operation(summary = "获得文件分页")
    @PreAuthorize("@ss.hasPermission('infra:file:query')")
    public CommonResult<PageResult<FileRespVO>> getFilePage(@Valid FilePageReqV
O pageVO) {
        PageResult<FileDO> pageResult = fileService.getFilePage(pageVO);
        return success(BeanUtils.toBean(pageResult, FileRespVO.class));
    }

}
services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/con
troller/admin\codegen\vo\CodegenUpdateReqVO.java:22:    private List<CodegenCol
umnSaveReqVO> columns;
services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/con
troller/admin\codegen\vo\CodegenCreateListReqVO.java:19:    private List<String
> tableNames;
services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/con
troller/admin\demo\demo03\normal\vo\Demo03StudentNormalSaveReqVO.java:37:    pr
ivate List<Demo03CourseDO> demo03Courses;
services/zhongshu-core/zszj-module-infra/src/main/java/cn/zszj/module/infra/con
troller/admin\demo\demo03\inner\vo\Demo03StudentInnerSaveReqVO.java:37:    priv
ate List<Demo03CourseDO> demo03Courses;
package cn.zszj.framework.security;

import cn.zszj.framework.common.pojo.CommonResult;
import cn.zszj.framework.security.fixture.MockOAuth2TokenApi;
import cn.zszj.framework.security.fixture.MockTenantFrameworkService;
import cn.zszj.framework.security.fixture.SecurityFixtureApplication;
import cn.zszj.framework.web.core.util.WebFrameworkUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMoc
kMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilde
rs.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilde
rs.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers
.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers
.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers
.status;

/**
 * ZS-SEC-012.A：真实 Security 过滤器链与双技术租户夹具测试。
 *
 * 验收条件（来自 05 文档 16.1 节）：
 * - 真实安全链启用（TokenAuthenticationFilter + TenantSecurityWebFilter + 方法权限）
 * - 双技术租户可构造（租户 1 和租户 2）
 * - 先允许暴露基线失败（不遮蔽真问题）
 *
 * 归属说明：本测试置于 biz-tenant 模块的 src/test（而非 security），以避免 security↔biz-tenant 的
 * Maven reactor 循环（biz-tenant 已 compile 依赖 security）。夹具装配见 fixture 包与 applicat
ion-fixture.yaml。
 *
 * 测试覆盖：
 * 1. 公开接口（@PermitAll）- 无 token 可访问
 * 2. 认证接口 - 无 token → 401；有效 token → 200；MEMBER token 访问 /admin-api → 403
 * 3. 权限接口（@PreAuthorize）- 无权限 → 403；有权限 → 200
 * 4. 租户校验 - 未传（兜底/400）/不匹配/禁用/过期/未知租户
 * 5. 跨租户访问（visit-tenant-id）- ZS-SEC-001.A 默认关闭：普通头/旧 visit 权限均 403 拒绝，不放大范围
 * 6. 对象授权 - 按 ID 归属校验
 * 7. 异常出口一致性 - CommonResult JSON 格式
 * 8. ZS-SEC-002 分类完整性 - 受保护异步端点首次 REQUEST 派发仍需认证（ASYNC permitAll 不泄露）；
 *    ADMIN token→/app-api 403、MEMBER token→/app-api 200（与组 2 的 MEMBER→/admin-a
pi 403 构成 ADMIN/MEMBER 双向串用矩阵）。
 *    注：全面 async/SSE 运行时合同（流式、异步异常出口、跨线程租户上下文、[Web]/[移动端]同步）仍归 ZS-SEC-012.B（见 0
5 文档 ZS-SEC-012 卡）。
 * 9. ZS-SEC-005 统一错误响应 - filter-direct 出口（401/403/400）登记 common_result，使访问日志按业
务码记录（不误记成功）；
 *    畸形 JSON / 请求体类型错误归 400 客户端错误且不回显敏感入参值；同类错误跨执行层语义一致（HTTP 200 + 镜像业务码）。
 *
 * @author ZS-SEC-012.A
 */
@SpringBootTest(classes = SecurityFixtureApplication.class, webEnvironment = Sp
ringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("fixture")
@DisplayName("ZS-SEC-012.A：安全过滤器链夹具测试")
class SecurityFilterChainFixtureTest {

    @Autowired
    private MockMvc mockMvc;

    // ========== 1. 公开接口（@PermitAll） ==========

    @Nested
    @DisplayName("1. 公开接口（@PermitAll）")
    class PublicEndpoints {

        @Test
        @DisplayName("无 token 可访问公开 GET 端点")
        void publicGetWithoutToken() throws Exception {
            mockMvc.perform(get("/admin-api/fixture/public/hello")
                            .header("tenant-id", MockOAuth2TokenApi.TENANT_1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").value("public-hello"));
        }

        @Test
        @DisplayName("无 token 可访问公开 POST 端点")
        void publicPostWithoutToken() throws Exception {
            mockMvc.perform(post("/admin-api/fixture/public/echo")
                            .header("tenant-id", MockOAuth2TokenApi.TENANT_1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data").value("public-echo"));
        }

        @Test
        @DisplayName("公开端点在 ignore-urls 中，无需 tenant-id")
        void publicEndpointWithoutTenantId() throws Exception {
            // /admin-api/fixture/public/** 在 tenant.ignore-urls 中
            mockMvc.perform(get("/admin-api/fixture/public/hello"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }
    }

    // ========== 2. 认证接口 ==========

    @Nested
    @DisplayName("2. 认证接口（需登录）")
    class AuthEndpoints {

        @Test
        @DisplayName("无 token 访问认证端点 → 401")
        void authEndpointWithoutToken() throws Exception {
            mockMvc.perform(get("/admin-api/fixture/auth/profile")
                            .header("tenant-id", MockOAuth2TokenApi.TENANT_1))
                    .andExpect(status().isOk()) // CommonResult 包装，HTTP 200
                    .andExpect(jsonPath("$.code").value(401))
                    .andExpect(jsonPath("$.msg").value("账号未登录"));
        }

        @Test
        @DisplayName("无效 token 访问认证端点 → 401")
        void authEndpointWithInvalidToken() throws Exception {
            mockMvc.perform(get("/admin-api/fixture/auth/profile")
                            .header("Authorization", "Bearer token-invalid")
                            .header("tenant-id", MockOAuth2TokenApi.TENANT_1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(401));
        }

        @Test
        @DisplayName("有效 token（租户 1 管理员）访问认证端点 → 200")
        void authEndpointWithValidTokenT1() throws Exception {
            mockMvc.perform(get("/admin-api/fixture/auth/profile")
                            .header("Authorization", "Bearer token-t1-admin")
                            .header("tenant-id", MockOAuth2TokenApi.TENANT_1))
                    .andExpect(status().isOk())
2026-09-10T11-12-10_288.dumpstream
2026-09-10T11-31-18_949.dumpstream
2026-09-10T12-02-07_387.dumpstream
2026-09-10T12-59-24_792.dumpstream
2026-09-10T13-22-45_190.dumpstream
2026-09-10T13-23-31_755.dumpstream
2026-09-10T18-11-17_722.dumpstream
2026-09-10T18-47-13_481.dumpstream
cn.zszj.framework.apilog.core.filter.ApiAccessLogFilterTest.txt
cn.zszj.framework.desensitize.core.DesensitizeTest.txt
cn.zszj.framework.encrypt.ApiEncryptTest.txt
cn.zszj.framework.web.core.filter.CacheRequestBodyFilterTest.txt
cn.zszj.framework.web.core.handler.GlobalExceptionHandlerTest.txt
cn.zszj.framework.web.core.util.WebFrameworkUtilsTest.txt
TEST-cn.zszj.framework.apilog.core.filter.ApiAccessLogFilterTest.xml
TEST-cn.zszj.framework.desensitize.core.DesensitizeTest.xml
TEST-cn.zszj.framework.encrypt.ApiEncryptTest.xml
TEST-cn.zszj.framework.web.core.filter.CacheRequestBodyFilterTest.xml
TEST-cn.zszj.framework.web.core.handler.GlobalExceptionHandlerTest.xml
TEST-cn.zszj.framework.web.core.util.WebFrameworkUtilsTest.xml

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'mvn 
-o -pl :zszj-server -am "-Dtest=WebFrameworkUtilsTest,CacheRequestBodyFilterTes
t,ValidationContractTest,SecurityFilterChainFixtureTest" "-Dsurefire.failIfNoSp
ecifiedTests=false" test > outputs/review-76da2a2f-tests.log 2>&1; Get-Content 
outputs/review-76da2a2f-tests.log -Tail 60' in E:\众墅之家AI赋能平台底座\services\zhongsh
u-core
 exited 1 in 14ms:
Get-Content : 找不到路径“E:\众墅之家AI赋能平台底座\services\zhongshu-core\outputs\review-76da2
a2f-tests.log”，因为该路径不存在。
所在位置 行:2 字符: 230
+ ... ests.log 2>&1; Get-Content outputs/review-76da2a2f-tests.log -Tail 60
+                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    + CategoryInfo          : ObjectNotFound: (E:\众墅之家AI赋能平台底座...a2a2f-tests.lo
g:String) [Get-Content], ItemNotFoundEx 
   ception
    + FullyQualifiedErrorId : PathNotFound,Microsoft.PowerShell.Commands.GetCon
tentCommand
 

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'mvn 
-o -pl :zszj-server -am "-Dtest=WebFrameworkUtilsTest,CacheRequestBodyFilterTes
t,ValidationContractTest,SecurityFilterChainFixtureTest" "-Dsurefire.failIfNoSp
ecifiedTests=false" test > ../../outputs/review-76da2a2f-tests.log 2>&1; Get-Co
ntent ../../outputs/review-76da2a2f-tests.log -Tail 60' in E:\众墅之家AI赋能平台底座\serv
ices\zhongshu-core
 succeeded in 816ms:
[ERROR] Could not create local repository at C:\.m2\repository -> [Help 1]
[ERROR] 
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e swi
tch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR] 
[ERROR] For more information about the errors and possible solutions, please re
ad the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/LocalReposito
ryNotAccessibleException

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command '[xml
]$report = Get-Content services/zhongshu-core/zszj-framework/zszj-spring-boot-s
tarter-web/target/surefire-reports/TEST-cn.zszj.framework.web.core.filter.Cache
RequestBodyFilterTest.xml -Encoding UTF8; $report.testsuite.properties.property
 | Where-Object { $_.name -in @('"'java.class.path','maven.repo.local','java.ho
me') } | ForEach-Object { "'$_.name; if ($_.name -eq '"'java.class.path') { "'$
_.value.Substring(0, [Math]::Min(2200, $_.value.Length)) } else { $_.value } };
 Get-ChildItem tools -Force -Name; Get-Content tools/apache-maven-3.9.*/conf/se
ttings.xml | Select-String '"'localRepository'" in E:\众墅之家AI赋能平台底座
 succeeded in 0ms:
java.class.path
E:\众墅之家AI赋能平台底座\services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-
web\target\test-classes;E:\众墅之家AI赋能平台底座\services\zhongshu-core\zszj-framework\z
szj-spring-boot-starter-web\target\classes;E:\众墅之家AI赋能平台底座\services\zhongshu-co
re\zszj-framework\zszj-common\target\classes;C:\Users\Administrator\.m2\reposit
ory\io\opentelemetry\opentelemetry-api\1.65.0\opentelemetry-api-1.65.0.jar;C:\U
sers\Administrator\.m2\repository\io\opentelemetry\opentelemetry-context\1.49.0
\opentelemetry-context-1.49.0.jar;C:\Users\Administrator\.m2\repository\org\pro
jectlombok\lombok\1.18.46\lombok-1.18.46.jar;C:\Users\Administrator\.m2\reposit
ory\org\mapstruct\mapstruct\1.6.3\mapstruct-1.6.3.jar;C:\Users\Administrator\.m
2\repository\org\mapstruct\mapstruct-jdk8\1.6.3\mapstruct-jdk8-1.6.3.jar;C:\Use
rs\Administrator\.m2\repository\org\mapstruct\mapstruct-processor\1.6.3\mapstru
ct-processor-1.6.3.jar;C:\Users\Administrator\.m2\repository\cn\hutool\hutool-a
ll\5.8.47\hutool-all-5.8.47.jar;C:\Users\Administrator\.m2\repository\com\aliba
ba\transmittable-thread-local\2.14.5\transmittable-thread-local-2.14.5.jar;C:\U
sers\Administrator\.m2\repository\com\alibaba\fastjson\2.0.64\fastjson-2.0.64.j
ar;C:\Users\Administrator\.m2\repository\com\alibaba\fastjson2\fastjson2-extens
ion\2.0.64\fastjson2-extension-2.0.64.jar;C:\Users\Administrator\.m2\repository
\com\alibaba\fastjson2\fastjson2\2.0.64\fastjson2-2.0.64.jar;C:\Users\Administr
ator\.m2\repository\org\dromara\easy-trans-anno\3.1.8\easy-trans-anno-3.1.8.jar
;C:\Users\Administrator\.m2\repository\com\fasterxml\jackson\core\jackson-annot
ations\2.21\jackson-annotations-2.21.jar;C:\Users\Administrator\.m2\repository\
org\slf4j\slf4j-api\2.0.18\slf4j-api-2.0.18.jar;C:\Users\Administrator\.m2\repo
sitory\org\springframework\boot\spring-boot-starter-web\3.5.15\spring-boot-star
ter-web-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\bo
ot\spring-boot-starter\3.5.15\spring-boot-starter-3.5.15.jar;C:\Users\Administr
ator\.m2\repository\org\springframework\boot\spring-boot\3.5.15\spring-boot-3.5
.15.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-b
oot-autoconfigure\3.5.15\spring-boot-autoconfigure-3.5.15.jar;C:\Us
java.home
E:\众墅之家AI赋能平台底座\tools\jdk-17.0.20.1+1
apache-maven-3.9.9
jdk-17.0.20.1+1
env.sh
jdk17.zip
maven.zip

  <!-- localRepository
  <localRepository>/path/to/local/repo</localRepository>



exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "rg -
n 'TenantContextHolder.set|TenantContextHolder.clear' services/zhongshu-core/zs
zj-framework --glob '*java' --glob '"'!**/target/**'"'; Get-Content services/zh
ongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/main/java/cn/zszj/f
ramework/web/config/ZszjWebAutoConfiguration.java -Encoding UTF8; Get-Content s
ervices/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/main/java
/cn/zszj/framework/encrypt/core/filter/ApiEncryptFilter.java -Encoding UTF8; gi
t diff 76da2a2f"'^ 76da2a2f --numstat; Get-Item C:'"\\Users\\Administrator\\.m2
\\repository\\cn\\hutool\\hutool-all\\5.8.47\\hutool-all-5.8.47.jar | Select-Ob
ject Length" in E:\众墅之家AI赋能平台底座
 succeeded in 17ms:

 Length
 ------
2830225



exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command '
$reviewDir = Join-Path (Get-Location) '"'outputs/review-76da2a2f'
New-Item -ItemType Directory -Force -Path "'$reviewDir | Out-Null
$source = @'"'
import cn.zszj.framework.web.core.filter.CacheRequestBodyFilter;
import cn.zszj.framework.web.core.filter.CacheRequestBodyWrapper;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

public class ReviewProbe {
    public static void main(String[] args) throws Exception {
        int passed = 0;
        for (String name : new String[] {
            \"cn.zszj.framework.web.core.util.WebFrameworkUtilsTest\",
            \"cn.zszj.framework.web.core.filter.CacheRequestBodyFilterTest\",
            \"cn.zszj.server.ValidationContractTest\"
        }) {
            Class<?> type = Class.forName(name);
            var ctor = type.getDeclaredConstructor();
            ctor.setAccessible(true);
            for (Method test : type.getDeclaredMethods()) {
                if ("'!test.isAnnotationPresent(org.junit.jupiter.api.Test.clas
s)) continue;
                Object instance = ctor.newInstance();
                for (Method setup : type.getDeclaredMethods()) {
                    if (setup.isAnnotationPresent(org.junit.jupiter.api.BeforeE
ach.class)) {
                        setup.setAccessible(true);
                        setup.invoke(instance);
                    }
                }
                test.setAccessible(true);
                test.invoke(instance);
                passed++;
            }
        }
        System.out.println("Existing targeted test methods passed: " + passed);

        int limit = 1024 * 1024;
        byte[] body = ("{'"\\\"payload\\\":\\\"\" + \"a\".repeat(2 * limit) + \
"\\\"}\").getBytes(StandardCharsets.UTF_8);
        for (boolean knownLength : new boolean[] {true, false}) {
            MockHttpServletRequest request = knownLength ? new MockHttpServletR
equest() : new MockHttpServletRequest() {
                @Override public long getContentLengthLong() { return -1L; }
                @Override public int getContentLength() { return -1; }
            };
            request.setMethod(\"POST\");
            request.setRequestURI(\"/admin-api/system/auth/login\");
            request.setContentType(\"application/json\");
            request.setContent(body);
            if ("'!knownLength) request.addHeader("Transfer-Encoding", "chunked
");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();
            new CacheRequestBodyFilter(limit).doFilter(request, response, chain
);
            System.out.println("knownLength=" + knownLength
                + " configuredLimit=" + limit
                + " actualBytes=" + body.length
                + " chainReached=" + (chain.getRequest() != null)
                + " cachedBytes=" + (chain.getRequest() instanceof CacheRequest
BodyWrapper ? chain.getRequest().getContentLengthLong() : 0)
                + " response=" + response.getContentAsString());
        }
    }
}
'"'@
[IO.File]::WriteAllText((Join-Path "'$reviewDir '"'ReviewProbe.java'), "'$sourc
e, [Text.UTF8Encoding]::new($false))
[xml]$report = Get-Content services/zhongshu-core/zszj-framework/zszj-spring-bo
ot-starter-web/target/surefire-reports/TEST-cn.zszj.framework.web.core.filter.C
acheRequestBodyFilterTest.xml -Encoding UTF8
$cp = ($report.testsuite.properties.property | Where-Object name -eq '"'java.cl
ass.path').value
"'$cp += '"';' + (Resolve-Path services/zhongshu-core/zszj-server/target/classe
s).Path
"'$cp += '"';' + (Resolve-Path services/zhongshu-core/zszj-server/target/test-c
lasses).Path
"'$files = @(
    (Join-Path $reviewDir '"'ReviewProbe.java'),
    'services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/mai
n/java/cn/zszj/framework/web/core/filter/CacheRequestBodyFilter.java',
    'services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/mai
n/java/cn/zszj/framework/web/core/util/WebFrameworkUtils.java',
    'services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/tes
t/java/cn/zszj/framework/web/core/filter/CacheRequestBodyFilterTest.java',
    'services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/tes
t/java/cn/zszj/framework/web/core/util/WebFrameworkUtilsTest.java',
    'services/zhongshu-core/zszj-server/src/test/java/cn/zszj/server/Validation
ContractTest.java'
)
& javac -proc:none -encoding UTF-8 -cp "'$cp -d $reviewDir $files
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Push-Location services/zhongshu-core/zszj-server
try { & java -cp ($reviewDir + '"';' + "'$cp) ReviewProbe } finally { Pop-Locat
ion }
' in E:\众墅之家AI赋能平台底座
 succeeded in 1383ms:
������ (17.0.20.1) �г����쳣��������� Bug Database (https://bugs.java.com) ��û���
ҵ��ô�����ͨ�� Java Bug ����ҳ (https://bugreport.java.com) ������ Java ������ Bug
�����ڱ����и������ĳ������������Ϣ�Լ����ݵ� Java �������Ĳ�����лл��
java.nio.file.AccessDeniedException: C:\Users\Administrator\.m2\repository\com\
vaadin\external\google\android-json\0.0.20131108.vaadin1\android-json-0.0.20131
108.vaadin1.jar
	at java.base/sun.nio.fs.WindowsException.translateToIOException(WindowsExcepti
on.java:89)
	at java.base/sun.nio.fs.WindowsException.rethrowAsIOException(WindowsException
.java:96)
	at java.base/sun.nio.fs.WindowsLinkSupport.getRealPath(WindowsLinkSupport.java
:258)
	at java.base/sun.nio.fs.WindowsPath.toRealPath(WindowsPath.java:933)
	at java.base/sun.nio.fs.WindowsPath.toRealPath(WindowsPath.java:42)
	at jdk.zipfs/jdk.nio.zipfs.ZipFileSystemProvider.lambda$removeFileSystem$0(Zip
FileSystemProvider.java:305)
	at java.base/java.security.AccessController.doPrivileged(AccessController.java
:569)
	at jdk.zipfs/jdk.nio.zipfs.ZipFileSystemProvider.removeFileSystem(ZipFileSyste
mProvider.java:307)
	at jdk.zipfs/jdk.nio.zipfs.ZipFileSystem.close(ZipFileSystem.java:526)
	at jdk.compiler/com.sun.tools.javac.file.JavacFileManager$ArchiveContainer.clo
se(JavacFileManager.java:660)
	at jdk.compiler/com.sun.tools.javac.file.JavacFileManager.close(JavacFileManag
er.java:738)
	at jdk.compiler/com.sun.tools.javac.main.Main.compile(Main.java:180)
	at jdk.compiler/com.sun.tools.javac.Main.compile(Main.java:64)
	at jdk.compiler/com.sun.tools.javac.Main.main(Main.java:50)
[ValidationContract] δ�������� @Valid �� @RequestBody ����(Ӧȫ��Ϊ�ѵǼ�����)={Capt
chaController#check=CaptchaVO, CaptchaController#get=CaptchaVO, SmsCallbackCont
roller#receiveHuaweiSmsStatus=String, SmsCallbackController#receiveQiniuSmsStat
us=String}
[ValidationContract] δ�������� @Valid �� @RequestBody ����(Ӧȫ��Ϊ�ѵǼ�����)={Capt
chaController#check=CaptchaVO, CaptchaController#get=CaptchaVO, SmsCallbackCont
roller#receiveHuaweiSmsStatus=String, SmsCallbackController#receiveQiniuSmsStat
us=String}
[ValidationContract] δ�������� @Valid �� @RequestBody ����(Ӧȫ��Ϊ�ѵǼ�����)={Capt
chaController#check=CaptchaVO, CaptchaController#get=CaptchaVO, SmsCallbackCont
roller#receiveHuaweiSmsStatus=String, SmsCallbackController#receiveQiniuSmsStat
us=String}
Existing targeted test methods passed: 20
knownLength=true configuredLimit=1048576 actualBytes=2097166 chainReached=false
 cachedBytes=0 response={"code":400,"msg":"�������С��������"}
knownLength=false configuredLimit=1048576 actualBytes=2097166 chainReached=true
 cachedBytes=2097166 response=

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command '$f =
 '"'services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/main
/java/cn/zszj/framework/web/core/filter/CacheRequestBodyFilter.java'; "'$n=0; G
et-Content $f -Encoding UTF8 | ForEach-Object { $n++; if ($n -ge 45 -and $n -le
 61) { '"'{0}: {1}' -f "'$n, $_ } }; git status --short; git diff --exit-code; 
git show --format= 76da2a2f -- README.md '"'docs/05-底座模块分析与开发任务清单.md'" in E:\众墅
之家AI赋能平台底座
 succeeded in 63ms:
?? docs/reviews/codex-ZS-GOV-001-P0.raw.md
?? docs/reviews/codex-ZS-OPS-001-step4.raw.md
?? docs/reviews/codex-ZS-SEC-008.raw.md
?? docs/reviews/codex-hotfix-C-r1.raw.md
?? docs/reviews/codex-hotfix-C.raw.md
?? docs/reviews/codex-hotfix-D.raw.md
?? docs/reviews/codex-hotfix-E.raw.md
?? outputs/
diff --git a/README.md b/README.md
index c7856833..f0fa8086 100644
--- a/README.md
+++ b/README.md
@@ -16,7 +16,7 @@
 - 未通过/未执行：Web 类型检查有上游基线错误；小程序离线依赖不全，构建未验证；真实 PostgreSQL 初始化、众墅规则、多端联调和部署尚未执行
 - 工程治理：独立 Git；迁入分支 `feat/foundation-source-import-20260908`，起点 `5758eda`；远程为 `
phlong026/zhongshu-ai-platform` 私有仓库；源码快照归档不代表合并主分支、CI 通过或部署
 - 文档治理：`FND-DOC-001～003` 已进入需求基线，当前均为 `DOCUMENTED`；本地文档一致性检查器（ZS-GOV-001 `veri
fy-docs` R1～R7）已实现并纳入 `run-local-gates`，含 §2/README 统计分布一致性门禁（R6/R7）与 `close-ta
sk` 一键收口回填；真实 CI 接入与 CI_VERIFIED 登记归 ZS-OPS-001
-- 模块分析：M01～M12 共 12 个底座模块已完成静态分析；加入品牌命名专项后累计 91 项主任务（43 待开发、11 开发中、31 待验收、6 待前
置）；25 项拆成 57 个分批子项且不重复计数；尚未实施或验收
+- 模块分析：M01～M12 共 12 个底座模块已完成静态分析；加入品牌命名专项后累计 91 项主任务（42 待开发、11 开发中、32 待验收、6 待前
置）；25 项拆成 57 个分批子项且不重复计数；尚未实施或验收
 - 品牌命名：ZS-BRAND-001～006 首轮开发完成（映射冻结、后端改名、两端静态品牌、种子与存量迁移脚本、代码生成模板、命名门禁，2026-09-
08，提交 887bbc4f…b2c26ea9，见 [docs/06](docs/06-品牌素材与命名映射.md) 第 9 节执行记录）；分批联验（B02 工
具链构建、B03/B05 缓存会话与任务、B06 多端）与工商全称/矢量原稿仍待后续补证
 
 ## 文档索引
@@ -27,7 +27,7 @@
 | [一期底座需求规格与待决策台账](docs/02-一期底座需求规格与待决策台账.md) | V0.12 | 一期需求、验收条件、决策状态、阻塞关系和变更
台账 |
 | [底座二次开发顺序与验收标准](docs/03-底座二次开发顺序与验收标准.md) | V1.6 | 24 个工作包对应 12 个批次，逐批前置条件、修
改位置、正反向验收和执行入口 |
 | [源码迁入与验证报告](docs/04-源码迁入与验证报告.md) | V1.0 | 源码范围、凭据净化、文件完整性、构建/测试结果和未通过门禁 |
-| [底座模块分析与开发任务清单](docs/05-底座模块分析与开发任务清单.md) | V1.16 | 12 个模块静态分析；按众墅要求记录代码差距、调
整任务、优先级、依赖和验收；累计 91 项主任务 |
+| [底座模块分析与开发任务清单](docs/05-底座模块分析与开发任务清单.md) | V1.17 | 12 个模块静态分析；按众墅要求记录代码差距、调
整任务、优先级、依赖和验收；累计 91 项主任务 |
 | [品牌素材与命名映射](docs/06-品牌素材与命名映射.md) | V1.1 | ZS-BRAND-001 冻结稿：品牌依据、zszj 命名映射、保
留例外、外部标识排除与首轮执行记录 |
 | [第三方来源与许可证](THIRD_PARTY_NOTICES.md) | 2026-09-08 | 固定 SHA、许可证、供体边界与可追溯差异 |
 | [现阶段底座开发清单与多端架构](.omx/plans/2026-09-08-底座开发清单与多端架构.md) | V1.0 建议稿 | 24 个工作包、
当前源码证据、复用/二开/自研边界、Web/小程序/App/iOS 接入与验收 |
diff --git "a/docs/05-\345\272\225\345\272\247\346\250\241\345\235\227\345\210\
206\346\236\220\344\270\216\345\274\200\345\217\221\344\273\273\345\212\241\346
\270\205\345\215\225.md" "b/docs/05-\345\272\225\345\272\247\346\250\241\345\23
5\227\345\210\206\346\236\220\344\270\216\345\274\200\345\217\221\344\273\273\3
45\212\241\346\270\205\345\215\225.md"
index 28aa9c06..a3cfa65d 100644
--- "a/docs/05-\345\272\225\345\272\247\346\250\241\345\235\227\345\210\206\346
\236\220\344\270\216\345\274\200\345\217\221\344\273\273\345\212\241\346\270\20
5\345\215\225.md"
+++ "b/docs/05-\345\272\225\345\272\247\346\250\241\345\235\227\345\210\206\346
\236\220\344\270\216\345\274\200\345\217\221\344\273\273\345\212\241\346\270\20
5\345\215\225.md"
@@ -1,6 +1,6 @@
 # 众墅之家 AI 赋能平台：底座模块分析与开发任务清单
 
-> 文档版本：V1.16
+> 文档版本：V1.17
 > 建立与更新日期：2026-09-08  
 > 状态：模块分析形成的开发待办；M01 工程骨架、M02 数据库与品牌命名专项 ZS-BRAND-001～006 已完成首轮开发（含真实运行代码与数据库迁
移变更，见各卡开发记录），其余任务待按批次实施；0 项已验收（验收须真实环境按批次放行）  
 > 代码核查基线：000e1dfb77d0b46f58a4a8993c4e4011a90a699d（迁入基线；品牌改名前基线见标签 brand-rename
-baseline=4ffaaf29）  
@@ -42,7 +42,7 @@
 | 横切：品牌与代码命名统一 | 已按用户要求登记；V1.5 完成首轮开发 | ZS-BRAND-001～006，共 6 项；B01 起实施，B06 技术闭
环验收 |
 | 横切：文档与任务治理 | 已登记缺口 | ZS-GOV-001，共 1 项 |
 
-12 个底座模块已完成静态分析。V1.3 为 85 项；本版按用户要求新增品牌与代码命名专项 ZS-BRAND-001～006，共 6 项，累计 91 项主
任务。V1.14 统计（2026-09-10，按各卡「状态」字段重新计数）：43 项待开发、11 项开发中、31 项待验收、0 项待决策、6 项待前置；0 项
已验收（待验收=ENG-001～006、DB-003～009/011～018/020、SEC-002/003/005/007、IAM-001、品牌 001/0
02/005、GOV-001；开发中=DB-001/002/019、CFG-001/002/003、CLIENT-005、OPS-001 与品牌 003/00
4/006，均为分批子项未全部收口的主卡状态；待验收/开发中指首轮开发已完成、待对应批次真实环境放行，非已验收）。25 项主任务拆为 57 个分批子项，子项不
叠加到 91 项统计。任务覆盖当前底座模块分析和品牌命名范围，不等于穷尽未来业务系统、Provider 或渠道的全部开发任务。
+12 个底座模块已完成静态分析。V1.3 为 85 项；本版按用户要求新增品牌与代码命名专项 ZS-BRAND-001～006，共 6 项，累计 91 项主
任务。V1.17 统计（2026-09-10，按各卡「状态」字段重新计数）：42 项待开发、11 项开发中、32 项待验收、0 项待决策、6 项待前置；0 项
已验收（待验收=ENG-001～006、DB-003～009/011～018/020、SEC-002/003/005/007/008、IAM-001、品牌 0
01/002/005、GOV-001；开发中=DB-001/002/019、CFG-001/002/003、CLIENT-005、OPS-001 与品牌 00
3/004/006，均为分批子项未全部收口的主卡状态；待验收/开发中指首轮开发已完成、待对应批次真实环境放行，非已验收）。25 项主任务拆为 57 个分批子项
，子项不叠加到 91 项统计。任务覆盖当前底座模块分析和品牌命名范围，不等于穷尽未来业务系统、Provider 或渠道的全部开发任务。
 
 本轮完成的是关键入口、调用链、数据对象与需求差距的静态分析，不是全仓逐行安全审计，也未执行运行代码修改、数据库连接/迁移、构建、真实集成或部署。后续开发中发
现的新路径或失败测试应继续更新稳定任务，不以本轮分析结论代替运行验证。
 
@@ -374,10 +374,11 @@ V1.13 变更记录（2026-09-10）：ZS-SEC-003（规范 Token 传输与特殊
 
 ### ZS-SEC-008：补齐参数校验、上下文头解析与请求资源限制
 
-- 关联：FND-CLIENT-001、FND-AUTH-001/004；WP-11/19/20；B03。优先级 P0；类别 验证适配；状态 待开发；前置 
ZS-SEC-002、ZS-SEC-005。
+- 关联：FND-CLIENT-001、FND-AUTH-001/004；WP-11/19/20；B03。优先级 P0；类别 验证适配；状态 待验收；前置 
ZS-SEC-002、ZS-SEC-005。
 - 众墅要求与现状：[UserController][E48] 使用 Validated，分页已有上下限；[Web 工具][E47] 先判断“数字”再 Lo
ng.valueOf，需验证超范围/小数等输入；[JSON 缓存 Wrapper][E46] 在认证前读取请求体，未见该层独立大小限制。是否有网关/容器限制尚
未核验，不能断言整套部署完全无限制。主配置关闭 XSS 清洗，也不等同已证明存在可执行 XSS。
 - 调整：逐入口验证 DTO 嵌套、集合长度、ID/枚举/字符串、批量对象及白名单字段；严格且可控地解析上下文头，并纳入统一异常出口；在缓冲前约束 JSON
 大小与读取超时，区分上传/流式接口。确认富文本清洗与前端输出编码边界，不把全局字符串清洗当成权限或 SQL 防注入。
 - 验收：畸形/溢出上下文、超量集合、超大 JSON、缺参数、非法枚举被稳定拒绝，不泄露栈；合法富文本、分页和上传不损坏；普通用户不能伪造租户/审计字段；线
程复用和异常退出无上下文残留。
+- 开发记录（2026-09-10，ZS-SEC-008）：按「逐入口参数校验 + 上下文头严格解析 + 请求资源限制 + 过滤器异常统一出口」交付四项调整
。①**@RequestBody 参数级 @Valid 校验契约**：类级 `@Validated` 只对 `@RequestParam`/`@PathVar
iable` 直接约束生效、不触发 `@RequestBody` 级联 Bean 校验，修复两个同类真实缺口——`SocialUserController#s
ocialUnbind`（`SocialUserUnbindReqVO` 已声明 `@InEnum`/`@NotNull`/`@NotEmpty`）与 `So
cialClientController#sendSubscribeMessage`（`SocialWxaSubscribeMessageSendReqDTO
` 已声明 `@NotNull`/`@NotEmpty`）补参数级 `@Valid`；新增静态防线 `ValidationContractTest`（zszj
-server，仿 `ApiInventoryTest`/`ModuleWhitelistTest` 静态源码扫描、不启动 Spring 上下文），强制启用模
块（system/infra）全部 Controller 的 `@RequestBody` 携带参数级 `@Valid`/`@Validated`，含 4 条
带理由例外目录（`CaptchaController#get`/`#check` 为 `com.anji.captcha` 第三方 SDK `CaptchaV
O`；`SmsCallbackController#receiveHuaweiSmsStatus`/`#receiveQiniuSmsStatus` 为 `@
RequestBody String` 原文回调、交服务层内部解析验签）+ 双向防腐（未登记缺口或目录腐化均失败）+ 回归护栏（锁两缺口已带 `@Valid`
）。②**上下文头严格解析**：[Web 工具][E47] 新增 `parseTenantIdHeader`，`getTenantId`/`getVisitT
enantId` 改调它——缺失/空白返回 `null`，逐字符 `Character.isDigit` + `Long.parseLong` 兜溢出，畸形/
溢出（`1.5`/`0x1F`/`1e5`/`-1`/超 `Long`）抛受控 `ServiceException`（业务码 400），替代旧 `Number
Util.isNumber + Long.valueOf`（isNumber 通过但 valueOf 抛 `NumberFormatException`、在 
MVC 外逃逸为容器 500 + 栈泄露）。③**过滤器异常统一出口**：`TenantContextWebFilter` 在 MVC 外、`GlobalEx
ceptionHandler`（`@RestControllerAdvice`）捕不到，改 try-catch `ServiceException` 就地 `
WebFrameworkUtils.writeJSON` 统一出口（复用 ZS-SEC-005），异常路径提前 return 不设置、`finally` 仍 
`TenantContextHolder.clear()` 无残留。④**JSON 请求体缓冲上限**：[JSON 缓存 Wrapper][E46] `Cac
heRequestBodyFilter` 缓冲前按 `Content-Length` 判定，超 `WebProperties.RequestBody.maxC
acheSize`（默认 1MB、`<=0` 不限制、键 `zszj.web.request-body.max-cache-size`）受控拒绝业务码 400
；上传/流式与 `/admin/`、`/actuator/` 由 `shouldNotFilter` 排除不受限，无参构造 `-1L` 向后兼容，`ZszjW
ebAutoConfiguration` 注入上限。**XSS/富文本边界**：主配置 `zszj.xss.enable=false`，明确全局字符串清洗不当
作权限或 SQL 防注入（SQL 注入归参数化查询、权限归 ZS-PERM），富文本清洗与前端输出编码为两端协作边界。测试：`WebFrameworkUtil
sTest` 11 + `CacheRequestBodyFilterTest` 6（web，`MockHttpServletRequest`/Mock 过滤
链纯单测）+ `ValidationContractTest` 3（zszj-server 静态扫描、输出恰好 4 例外）全 BUILD SUCCESS；`m
vn -pl :zszj-server -am test` 反应堆 20 模块全 SUCCESS（含 system/web/biz-tenant 编译验证 `
@Valid` 与过滤器改动）、`run-local-gates --fast` 10/10；登记[参数校验与请求资源限制规范](../services/zh
ongshu-core/docs/参数校验与请求资源限制规范.md)。待验收说明：本轮为代码级入口校验/解析/资源限制与单元 + 静态契约测试，网关/容器层大
小与超时限制、真实链畸形输入端到端拒绝归 ZS-SEC-012.B；限流归 ZS-SEC-010、ID/时间/分页/版本兼容合同归 ZS-SEC-009；XS
S 开关生产取值与富文本端到端归 ZS-SEC-012.B 及各业务入口（如公告 SYS-NOTICE）。本记录不表示任何主任务已验收。
 
 ### ZS-SEC-009：固定 ID、时间、分页与接口版本兼容合同
 
@@ -1100,3 +1101,4 @@ Vue3 已有登录初始化、后端菜单转动态路由和按钮指令；Admin
 | 2026-09-10 | V1.14 | D-09 用户拍板确认（一个 Account 多 Membership、用户名全平台唯一、手机/邮箱全局唯一、
trim+小写、逻辑删除可重建、跨组织仅限显式平台角色）：ZS-DB-009、ZS-IAM-001 决策包由待决策转待验收，第 2 节统计 2 待决策→0、2
9 待验收→31；M05 说明与 ZS-DB-010 前置门禁更新；D-07 方向已采纳（细节待阶段 2 前）；同步 verify-docs 将 D-09 移
出未确认守护列表；纯决策落库，未提升任何实现验收状态或改动运行代码 |
 | 2026-09-10 | V1.15 | ZS-GOV-001 治理工具增强（提效方案 P0，纯工具、无任务状态变更）：新增 task-stats.mj
s 从卡片「状态 X」字段实时聚合真实分布（countStatus/parseSection2Declared/parseReadmeDeclared，替代手
工数 91 卡，§16.1 子项天然不计入）；verify-docs 增 R6（§2 声明 vs 卡片实际）/R7（README 摘要 vs 卡片实际）统计一
致性门禁 + 4 测试（单测 11/11）；新增 close-task.mjs 一键收口（改状态→重算→回填 §2/README→可选 --bump 升版→变
更记录建议，含 --dry-run/--sync-only 幂等，写盘后自动复核）。上线即抓出并修正 README 存量漂移（83/2/6 → 43/11/3
1/6，卡片实际分布未变）；close-task --sync-only 幂等验证；run-local-gates --fast 10/10。第 2 节统计分
布不变（43/11/31/0/6/0），ZS-GOV-001 维持待验收（真实 CI 归 ZS-OPS-001）；README 索引版本同步 V1.15 |
 | 2026-09-10 | V1.16 | ZS-OPS-001.A 聚合门禁入口提速（提效方案 P1 步骤④，纯工具、无任务状态变更）：run-loca
l-gates.mjs 串行改并发（按 CPU 核数，--jobs 覆盖，结果按定义顺序稳定汇总 + 失败打印输出末尾详情），实测 --fast 10 项 2
407ms→1205ms（2×）；新增 --incremental 按 git 变更路径只跑受影响门禁（G3 品牌全仓扫描/G9 秘密门禁恒定跑，变更含 sc
ripts/ops/ 自身或无法归类非良性路径则 fail-safe 回退全量），--plan 只预览不执行；抽出 candidateGates/planGa
tes 纯函数 + 新增 run-local-gates.test.mjs 14 用例。门禁 id/命令与 CI workflow 不变；增量仅供日常反馈，批
次收口/CI 必须全量。单测 14/14、--fast 并发 10/10。ZS-OPS-001 维持开发中（.B~.E 按批接入），第 2 节统计分布不变（4
3/11/31/0/6/0）；README 索引版本同步 V1.16 |
+| 2026-09-10 | V1.17 | ZS-SEC-008（补齐参数校验、上下文头解析与请求资源限制，B03；前置 ZS-SEC-002、ZS-SE
C-005 已完成）交付四项调整：①`@RequestBody` 参数级 `@Valid` 校验契约——修复 `SocialUserController#so
cialUnbind`、`SocialClientController#sendSubscribeMessage` 两个 VO/DTO 已声明约束却漏 `@V
alid` 的同类真实缺口（类级 `@Validated` 不触发 `@RequestBody` 级联校验），新增静态防线 `ValidationContra
ctTest`（zszj-server 静态源码扫描，强制启用模块 system/infra 全部 Controller 的 `@RequestBody` 带
参数级 `@Valid`/`@Validated`，4 条带理由例外目录 + 双向防腐 + 回归护栏）；②上下文头严格解析——`WebFrameworkUti
ls.parseTenantIdHeader` 逐字符 `isDigit` + `Long.parseLong` 兜溢出，畸形/溢出（`1.5`/`0x1F`
/`1e5`/`-1`/超 Long）抛受控 `ServiceException`（业务码 400），替代旧 `NumberUtil.isNumber + L
ong.valueOf` 在 MVC 外逃逸的容器 500 + 栈泄露；③过滤器异常统一出口——`TenantContextWebFilter`（MVC 外、
`GlobalExceptionHandler` 捕不到）try-catch `ServiceException` 就地 `writeJSON`（复用 ZS-
SEC-005），异常路径提前 return、`finally clear()` 无上下文残留；④JSON 请求体缓冲上限——`CacheRequestBod
yFilter` 缓冲前按 `Content-Length` 超 `WebProperties.RequestBody.maxCacheSize`（默认 1M
B、`<=0` 不限制）受控拒绝 400，上传/流式与 `/admin/`、`/actuator/` 由 `shouldNotFilter` 排除、无参构造 
`-1L` 向后兼容。明确 XSS/富文本边界（`zszj.xss.enable=false`，全局字符串清洗不当作权限或 SQL 防注入）。测试 `WebF
rameworkUtilsTest` 11 + `CacheRequestBodyFilterTest` 6（web 纯单测）+ `ValidationCon
tractTest` 3（静态扫描、输出恰好 4 例外）全绿，`mvn -pl :zszj-server -am test` 反应堆 20 模块全 SUCCE
SS（含 system/web/biz-tenant 编译）、`run-local-gates --fast` 10/10 + 参数校验与请求资源限制规范文档
。卡片 待开发→待验收，第 2 节统计 43/31→42/32（余不变）；网关/容器层限制与真实链端到端拒绝归 ZS-SEC-012.B，限流归 ZS-SEC
-010、ID/时间/分页/版本合同归 ZS-SEC-009；README 索引版本同步 V1.17 |

codex
The new JSON buffering limit is bypassable through chunked requests, leaving un
bounded pre-authentication memory allocation despite the configured cap.

Review comment:

- [P1] Enforce the cache limit while reading unknown-length bodies — E:/众墅之家AI赋
能平台底座/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/ma
in/java/cn/zszj/framework/web/core/filter/CacheRequestBodyFilter.java:52-56
  For chunked `application/json` requests, `getContentLengthLong()` returns `-1
`, so this check passes and `CacheRequestBodyWrapper` still reads the entire bo
dy into memory. A 2 MiB body with unknown length was fully cached despite the c
onfigured 1 MiB limit. Unauthenticated clients can therefore bypass the new res
ource limit and exhaust heap memory. Keep the Content-Length check as an early 
rejection, but also bound the actual stream read and reject once it exceeds `ma
xCacheSize`; add an unknown-length request regression test.
The new JSON buffering limit is bypassable through chunked requests, leaving unbounded pre-authentication memory allocation despite the configured cap.

Review comment:

- [P1] Enforce the cache limit while reading unknown-length bodies — E:/众墅之家AI赋能平台底座/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/main/java/cn/zszj/framework/web/core/filter/CacheRequestBodyFilter.java:52-56
  For chunked `application/json` requests, `getContentLengthLong()` returns `-1`, so this check passes and `CacheRequestBodyWrapper` still reads the entire body into memory. A 2 MiB body with unknown length was fully cached despite the configured 1 MiB limit. Unauthenticated clients can therefore bypass the new resource limit and exhaust heap memory. Keep the Content-Length check as an early rejection, but also bound the actual stream read and reject once it exceeds `maxCacheSize`; add an unknown-length request regression test.
