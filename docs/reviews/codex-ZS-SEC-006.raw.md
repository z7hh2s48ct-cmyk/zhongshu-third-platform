node.exe : OpenAI Codex v0.154.0
所在位置 C:\Users\Administrator\AppData\Roaming\npm\codex.ps1:24 字符: 5
+     & "node$exe"  "$basedir/node_modules/@openai/codex/bin/codex.js"  ...
+     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    + CategoryInfo          : NotSpecified: (OpenAI Codex v0.154.0:String) [], 
    RemoteException
    + FullyQualifiedErrorId : NativeCommandError
 
--------
workdir: E:\zszj-wt-sec-006
model: gpt-6-astra
provider: openai
approval: never
sandbox: read-only
reasoning effort: xhigh
reasoning summaries: none
session id: 01a09674-ae44-7340-b1bc-ae40e003401e
--------
user
commit 8785b9f
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'Get-
Location; git status --short; git rev-parse --show-toplevel; git show --stat --
oneline --decorate --no-renames 8785b9f3; Get-ChildItem -Force -Name; if (Test-
Path AGENTS.override.md) { Get-Content AGENTS.override.md }; if (Test-Path AGEN
TS.md) { Get-Content AGENTS.md }' in E:\zszj-wt-sec-006
 succeeded in 64ms:
E:/zszj-wt-sec-006
8785b9f3 (HEAD -> feat/sec-006) feat(sec-006): 无链路代理时的请求关联标识 fallback + 外部 trac
e-id 信任边界 [ZS-SEC-006]
 .../framework/common/util/monitor/TracerUtils.java | 106 +++++++++++
 .../util/monitor/TracerUtilsFallbackTest.java      | 106 +++++++++++
 .../framework/tracer/core/filter/TraceFilter.java  |  44 ++++-
 .../zszj-spring-boot-starter-web/pom.xml           |   5 +
 .../apilog/core/filter/ApiAccessLogFilter.java     |   4 +-
 .../web/core/handler/GlobalExceptionHandler.java   |   4 +-
 .../tracer/core/filter/TraceFilterTest.java        | 197 +++++++++++++++++++++
 7 files changed, 458 insertions(+), 8 deletions(-)
Path              
----              
E:\zszj-wt-sec-006
.github
.omx
apps
docs
reference
scripts
services
third_party
tools
.git
.gitignore
README.md
THIRD_PARTY_NOTICES.md



exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "git 
show --format=fuller --no-ext-diff --no-renames 8785b9f3; Get-ChildItem -Path .
 -Filter AGENTS* -File -Recurse -Force | Select-Object -ExpandProperty FullName
; Get-ChildItem -Path . -Filter '*config*' -File -Force | Select-Object -Expand
Property FullName" in E:\zszj-wt-sec-006
 succeeded in 383ms:
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "if (
Test-Path E:\\AGENTS.override.md) { Get-Content E:\\AGENTS.override.md }; if (T
est-Path E:\\AGENTS.md) { Get-Content E:\\AGENTS.md }; Get-ChildItem .omx -Forc
e; git ls-files '*AGENTS*' '*CLAUDE*' '*REVIEW*' '*rules*' '*config.toml'; rg -
n 'SEC-006|关联 ID|关联标识|TraceFilter|TRACE_FILTER|TRACER_FILTER|getCorrelationId\\
(' services docs scripts .github .omx; Get-Content services/zhongshu-core/zszj-
framework/zszj-common/pom.xml; Get-Content services/zhongshu-core/zszj-framewor
k/zszj-spring-boot-starter-monitor/pom.xml; Get-Content services/zhongshu-core/
zszj-framework/zszj-spring-boot-starter-web/pom.xml" in E:\zszj-wt-sec-006
 succeeded in 187ms:
services\zhongshu-core\docs\日志脱敏策略.md:49:- **保留可定位错误码与关联 ID**：访问/异常日志保留 url、tra
ceId、resultCode；切面日志保留方法签名描述；`sanitizeResponseBody` 只净化 `data` 保留 `code`/`msg`。
services\zhongshu-core\docs\接口清单与匿名白名单.md:91:1. **租户枚举面**（`simple-list`）：匿名可获全部
**启用**租户的 `id`+`name`（敏感联系字段已不填充，暴露面收敛到登录页选择租户的最小集）。残余「租户名可被匿名枚举」如需进一步收紧，评估速率限制
/按需精确查询——归后续硬化（限流/日志归 ZS-SEC-006）。`get-by-website` 已对域名做 `@Pattern` 校验、禁用租户返回 n
ull。
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\web\config\CorsConfigTest.java:34:                "ZS-SEC-0
06 联动：须暴露 trace-id");
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:17: * ZS-SEC-006：验证
 TraceFilter 在无 OTel、外部合法/畸形 trace-id、过滤器早退等场景下的行为。
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:19: * RED 现状：TraceF
ilter 直接写入空串（TracerUtils.getTraceId() 无 Span 返回 ""），且不校验外部传入。
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:21:class TraceFilte
rTest {
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:23:    private fina
l TraceFilter traceFilter = new TraceFilter();
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:39:                
"响应头应为 32 字符十六进制关联 ID，实际=" + headerValue);
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:51:        assertNo
tNull(attr, "TraceFilter 应将关联 ID 绑定到请求属性");
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:53:                
"响应头与请求属性应为同一关联 ID");
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:68:        // 合法外部 
trace-id 可复用为关联 ID（便于端到端串联）
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:70:                
"合法外部 trace-id 应被复用为关联 ID");
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:105:               
 "应替换为服务端生成的合法关联 ID，实际=" + headerValue);
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:138:    // ========
== ④过滤器链早退（异常）时响应头仍有关联 ID ==========
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:156:        // 即使链抛
异常，响应头应已设置关联 ID（因为设置在 chain.doFilter 之前）
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:158:        assertN
otNull(headerValue, "过滤器早退时响应头仍应有关联 ID");
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:159:        assertF
alse(headerValue.isEmpty(), "过滤器早退时关联 ID 不应为空");
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:175:               
 "不同请求的关联 ID 不应相同（不串号）");
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:178:    // ========
== ⑥关联 ID 不可承载权限（纯随机，非授权凭据） ==========
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\main\jav
a\cn\zszj\framework\web\core\handler\GlobalExceptionHandler.java:362:          
  log.error("[createExceptionLog][url({}) traceId({}) 写入错误日志失败]", req.getReques
tURI(), TracerUtils.getCorrelationId(req), th);
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\main\jav
a\cn\zszj\framework\web\core\handler\GlobalExceptionHandler.java:383:        er
rorLog.setTraceId(TracerUtils.getCorrelationId(request));
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\main\jav
a\cn\zszj\framework\web\config\WebProperties.java:127:         * 暴露给浏览器的响应头（ZS-
SEC-006 trace-id 关联）。
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\main\jav
a\cn\zszj\framework\apilog\core\filter\ApiAccessLogFilter.java:90:            l
og.error("[createApiAccessLog][url({}) traceId({}) 写入访问日志失败]", request.getReque
stURI(), TracerUtils.getCorrelationId(request), th);
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\main\jav
a\cn\zszj\framework\apilog\core\filter\ApiAccessLogFilter.java:120:        acce
ssLog.setTraceId(TracerUtils.getCorrelationId(request)).setApplicationName(appl
icationName)
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-monitor\src\main
\java\cn\zszj\framework\tracer\core\filter\TraceFilter.java:15: * Trace 过滤器：为每个
请求绑定关联 ID 并写入响应头。
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-monitor\src\main
\java\cn\zszj\framework\tracer\core\filter\TraceFilter.java:17: * <h2>ZS-SEC-00
6 行为说明</h2>
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-monitor\src\main
\java\cn\zszj\framework\tracer\core\filter\TraceFilter.java:19: *   <li>有 OTel 
Span → 使用 traceId 作为关联 ID</li>
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-monitor\src\main
\java\cn\zszj\framework\tracer\core\filter\TraceFilter.java:20: *   <li>无 OTel 
+ 外部传入合法 trace-id 头（32 字符 hex）→ 复用为关联 ID（便于端到端串联，但不承载权限）</li>
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-monitor\src\main
\java\cn\zszj\framework\tracer\core\filter\TraceFilter.java:21: *   <li>无 OTel 
+ 外部畸形/超长/无 trace-id → 服务端生成 fallback 关联 ID，绝不回显攻击者可控原文</li>
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-monitor\src\main
\java\cn\zszj\framework\tracer\core\filter\TraceFilter.java:22: *   <li>关联 ID 绑
定到请求属性（{@link TracerUtils#ATTR_CORRELATION_ID}），供下游日志/错误处理统一取值</li>
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-monitor\src\main
\java\cn\zszj\framework\tracer\core\filter\TraceFilter.java:23: *   <li>响应头在 ch
ain.doFilter 之前设置 → 即使后续过滤器早退/异常，客户端仍可拿到关联 ID</li>
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-monitor\src\main
\java\cn\zszj\framework\tracer\core\filter\TraceFilter.java:28:public class Tra
ceFilter extends OncePerRequestFilter {
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-monitor\src\main
\java\cn\zszj\framework\tracer\core\filter\TraceFilter.java:31:     * Header 名 
- 链路追踪编号 / 请求关联 ID
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-monitor\src\main
\java\cn\zszj\framework\tracer\core\filter\TraceFilter.java:38:        // 解析关联 
ID（OTel > 外部合法 > 服务端生成）
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-monitor\src\main
\java\cn\zszj\framework\tracer\core\filter\TraceFilter.java:49:     * 解析关联 ID：
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\pom.xml:82: 
           <scope>test</scope> <!-- ZS-SEC-006：TraceFilterTest 需要 TraceFilter 类
 -->
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-monitor\src\main
\java\cn\zszj\framework\tracer\config\ZszjTracerAutoConfiguration.java:5:import
 cn.zszj.framework.tracer.core.filter.TraceFilter;
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-monitor\src\main
\java\cn\zszj\framework\tracer\config\ZszjTracerAutoConfiguration.java:47:     
* 创建 TraceFilter 过滤器，响应 header 设置 traceId
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-monitor\src\main
\java\cn\zszj\framework\tracer\config\ZszjTracerAutoConfiguration.java:50:    p
ublic FilterRegistrationBean<TraceFilter> traceFilter() {
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-monitor\src\main
\java\cn\zszj\framework\tracer\config\ZszjTracerAutoConfiguration.java:51:     
   FilterRegistrationBean<TraceFilter> registrationBean = new FilterRegistratio
nBean<>();
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-monitor\src\main
\java\cn\zszj\framework\tracer\config\ZszjTracerAutoConfiguration.java:52:     
   registrationBean.setFilter(new TraceFilter());
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-monitor\src\main
\java\cn\zszj\framework\tracer\config\ZszjTracerAutoConfiguration.java:53:     
   registrationBean.setOrder(WebFilterOrderEnum.TRACE_FILTER);
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-monitor\pom.xml:
34:            <scope>provided</scope> <!-- 设置为 provided，只有 TraceFilter 使用 -->
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-monitor\pom.xml:
40:            <scope>provided</scope> <!-- 设置为 provided，只有 TraceFilter 使用 -->
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:9: * ZS-SEC-006：验证无 OTel Sp
an 时 TracerUtils.getCorrelationId() 提供有效 fallback 关联 ID。
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:11: * RED 现状：getCorrelation
Id() 方法不存在（编译失败）。
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:17:        // 无有效 OTel Span
 时，getCorrelationId() 必须返回非空关联 ID
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:18:        String id = Trac
erUtils.getCorrelationId();
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:19:        assertNotNull(id
, "关联 ID 不应为 null");
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:20:        assertFalse(id.i
sEmpty(), "关联 ID 不应为空串（核心缺口修复）");
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:25:        String id = Trac
erUtils.getCorrelationId();
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:27:        assertTrue(id.le
ngth() <= 64, "关联 ID 长度应有界，实际=" + id.length());
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:28:        assertTrue(id.le
ngth() >= 16, "关联 ID 应有足够熵，实际长度=" + id.length());
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:33:        String id = Trac
erUtils.getCorrelationId();
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:35:        assertTrue(id.ma
tches("[0-9a-fA-F]+"), "关联 ID 应为合法十六进制格式，实际=" + id);
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:42:        String id1 = Tra
cerUtils.getCorrelationId(request);
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:43:        String id2 = Tra
cerUtils.getCorrelationId(request);
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:44:        assertEquals(id1
, id2, "同一请求内连续调用应返回相同的关联 ID（稳定性）");
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:52:        String id1 = Tra
cerUtils.getCorrelationId(req1);
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:53:        String id2 = Tra
cerUtils.getCorrelationId(req2);
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:54:        assertNotEquals(
id1, id2, "不同请求应生成不同的关联 ID（不串号）");
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:59:        // 如果请求属性已设置关联 I
D，应优先使用
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:63:        String id = Trac
erUtils.getCorrelationId(request);
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:64:        assertEquals(pre
set, id, "应优先使用请求属性中已绑定的关联 ID");
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:96:        assertEquals(32,
 id.length(), "生成的关联 ID 应为 32 字符（128-bit hex）");
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:97:        assertTrue(id.ma
tches("[0-9a-f]+"), "生成的关联 ID 应为小写十六进制");
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:104:        assertNotEquals
(id1, id2, "连续生成的关联 ID 不应重复");
services\zhongshu-core\zszj-framework\zszj-common\src\main\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtils.java:14: * <h2>ZS-SEC-006 关联 ID 设计说明</h2>
services\zhongshu-core\zszj-framework\zszj-common\src\main\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtils.java:17: *   <li>{@link #getCorrelationId()}
 / {@link #getCorrelationId(HttpServletRequest)} —— 统一关联 ID 入口：
services\zhongshu-core\zszj-framework\zszj-common\src\main\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtils.java:20: * <p>关联 ID ≠ 分布式 Trace。关联 ID 仅用于前端/
日志定位同一请求，绝不作为授权/身份凭据。</p>
services\zhongshu-core\zszj-framework\zszj-common\src\main\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtils.java:27:     * 请求属性键：关联 ID（由 TraceFilter 在请求
入口绑定）
services\zhongshu-core\zszj-framework\zszj-common\src\main\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtils.java:47:     * 若需保证非空的请求关联标识，请使用 {@link #get
CorrelationId()}。</p>
services\zhongshu-core\zszj-framework\zszj-common\src\main\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtils.java:57:     * 获得请求关联 ID（统一入口，保证非空）。
services\zhongshu-core\zszj-framework\zszj-common\src\main\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtils.java:60:     * <p>此方法绝不返回 null 或空串。返回值为有界（32
 字符）、合法十六进制格式、不可承载权限的关联标识。</p>
services\zhongshu-core\zszj-framework\zszj-common\src\main\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtils.java:62:     * @return 关联 ID（非空）
services\zhongshu-core\zszj-framework\zszj-common\src\main\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtils.java:64:    public static String getCorrelat
ionId() {
services\zhongshu-core\zszj-framework\zszj-common\src\main\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtils.java:76:     * 获得请求关联 ID（统一入口，保证非空），绑定请求作用域。
services\zhongshu-core\zszj-framework\zszj-common\src\main\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtils.java:78:     * <p>优先级：OTel traceId → 请求属性中已绑
定的 fallback（由 TraceFilter 设置）→ 新生成并绑定。</p>
services\zhongshu-core\zszj-framework\zszj-common\src\main\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtils.java:82:     * @return 关联 ID（非空）
services\zhongshu-core\zszj-framework\zszj-common\src\main\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtils.java:84:    public static String getCorrelat
ionId(HttpServletRequest request) {
services\zhongshu-core\zszj-framework\zszj-common\src\main\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtils.java:106:     * 生成 128-bit 随机十六进制关联 ID（32 字符
，小写）。
services\zhongshu-core\zszj-framework\zszj-common\src\main\java\cn\zszj\framewo
rk\common\enums\WebFilterOrderEnum.java:14:    int TRACE_FILTER = CORS_FILTER +
 1;
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
    <artifactId>zszj-common</artifactId>
    <packaging>jar</packaging>

    <name>${project.artifactId}</name>
    <description>定义基础 pojo 类、枚举、工具类等等</description>
    <url>https://github.com/YunaiV/ruoyi-vue-pro</url>

    <dependencies>
        <!-- Spring 核心 -->
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-core</artifactId>
            <scope>provided</scope> <!-- 设置�?provided，只有工具类需要使用到 -->
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-expression</artifactId>
            <scope>provided</scope> <!-- 设置�?provided，只有工具类需要使用到 -->
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-aop</artifactId>
            <scope>provided</scope> <!-- 设置�?provided，只有工具类需要使用到 -->
        </dependency>
        <dependency>
            <groupId>org.aspectj</groupId>
            <artifactId>aspectjweaver</artifactId>
            <scope>provided</scope> <!-- 设置�?provided，只有工具类需要使用到 -->
        </dependency>

        <dependency>
            <!-- 用于生成自定义的 Spring @ConfigurationProperties 配置类的说明文件 -->
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Web 相关 -->
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-web</artifactId>
            <scope>provided</scope> <!-- 设置�?provided，只有工具类需要使用到 -->
        </dependency>

        <dependency>
            <groupId>jakarta.servlet</groupId>
            <artifactId>jakarta.servlet-api</artifactId>
            <scope>provided</scope> <!-- 设置�?provided，只有工具类需要使用到 -->
        </dependency>

        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <scope>provided</scope> <!-- 设置�?provided，主要是 PageParam 使用�?-->
        </dependency>

        <!-- 监控相关 -->
        <dependency>
            <groupId>io.opentelemetry</groupId>
            <artifactId>opentelemetry-api</artifactId>
        </dependency>

        <!-- 工具类相�?-->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>

        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct-jdk8</artifactId> <!-- use mapstruct-jdk8 for
 Java 8 or higher -->
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct-processor</artifactId>
        </dependency>

        <dependency>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
            <scope>provided</scope> <!-- 设置�?provided，只有工具类需要使用到 -->
        </dependency>

        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <scope>provided</scope> <!-- 设置�?provided，只有工具类需要使用到 -->
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-core</artifactId>
            <scope>provided</scope> <!-- 设置�?provided，只有工具类需要使用到 -->
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.datatype</groupId>
            <artifactId>jackson-datatype-jsr310</artifactId>
            <scope>provided</scope> <!-- 设置�?provided，只有工具类需要使用到 -->
        </dependency>

        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <scope>provided</scope> <!-- 设置�?provided，只有工具类需要使用到 -->
        </dependency>

        <dependency>
            <groupId>jakarta.validation</groupId>
            <artifactId>jakarta.validation-api</artifactId>
            <scope>provided</scope> <!-- 设置�?provided，主要是 PageParam 使用�?-->
        </dependency>

        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
        </dependency>

        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>transmittable-thread-local</artifactId>
        </dependency>

        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>fastjson</artifactId> <!-- JustAuth、Jimu 等三方库仍使�?1.x 包名
，由项目统一提供 fastjson2 兼容实现 -->
        </dependency>

        <dependency>
            <groupId>org.dromara</groupId> <!-- VO 数据翻译 -->
            <artifactId>easy-trans-anno</artifactId> <!-- 默认引入的原因，方便 xxx-module
-api 包使�?-->
        </dependency>

        <!-- Test 测试相关 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

</project>
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
    <artifactId>zszj-spring-boot-starter-monitor</artifactId>
    <packaging>jar</packaging>

    <name>${project.artifactId}</name>
    <description>服务监控，提供链路追踪、日志服务、指标收集等等功�?/description>
    <url>https://github.com/YunaiV/ruoyi-vue-pro</url>

    <dependencies>
        <dependency>
            <groupId>cn.zszj</groupId>
            <artifactId>zszj-common</artifactId>
        </dependency>

        <!-- Spring 核心 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>

        <!-- Web 相关 -->
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-web</artifactId>
            <scope>provided</scope> <!-- 设置�?provided，只�?TraceFilter 使用 -->
        </dependency>

        <dependency>
            <groupId>jakarta.servlet</groupId>
            <artifactId>jakarta.servlet-api</artifactId>
            <scope>provided</scope> <!-- 设置�?provided，只�?TraceFilter 使用 -->
        </dependency>

        <!-- 监控相关 -->
        <dependency>
            <groupId>io.opentelemetry</groupId>
            <artifactId>opentelemetry-api</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>io.opentelemetry</groupId>
            <artifactId>opentelemetry-sdk</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>io.opentelemetry</groupId>
            <artifactId>opentelemetry-exporter-otlp</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.apache.skywalking</groupId>
            <artifactId>apm-toolkit-logback-1.x</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Micrometer �?Prometheus 的支�?-->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
            <optional>true</optional>
        </dependency>

        <dependency>
            <groupId>de.codecentric</groupId>
            <artifactId>spring-boot-admin-starter-client</artifactId> <!-- 实现 S
pring Boot Admin Client 客户�?-->
            <optional>true</optional>
        </dependency>
    </dependencies>

</project>
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
    <description>Web 框架，全局异常、API 日志、脱敏、错误码�?/description>
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
            <scope>provided</scope> <!-- 解决工具�?SpringExpressionUtils 加载的时候访问不�?
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
            <scope>provided</scope> <!-- 设置�?provided，主要是 GlobalExceptionHandle
r 使用 -->
        </dependency>

        <!-- 工具类相�?-->
        <dependency>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
            <scope>provided</scope> <!-- 设置�?provided，只有工具类需要使用到 -->
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
        <dependency>
            <groupId>cn.zszj</groupId>
            <artifactId>zszj-spring-boot-starter-monitor</artifactId>
            <scope>test</scope> <!-- ZS-SEC-006：TraceFilterTest 需�?TraceFilter 
�?-->
        </dependency>
    </dependencies>

</project>



exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "rg -
n 'TraceFilter|TRACE_FILTER|TracerUtils|getTraceId\\(|trace-id|traceId' service
s/zhongshu-core/zszj-framework/zszj-spring-boot-starter-monitor services/zhongs
hu-core/zszj-framework/zszj-common/src/main services/zhongshu-core/zszj-server/
src/main; Get-Content -Encoding UTF8 services/zhongshu-core/zszj-framework/zszj
-spring-boot-starter-monitor/src/main/java/cn/zszj/framework/tracer/config/Zszj
TracerAutoConfiguration.java; Get-Content -Encoding UTF8 services/zhongshu-core
/zszj-framework/zszj-common/src/main/java/cn/zszj/framework/common/enums/WebFil
terOrderEnum.java; Get-ChildItem -Recurse -Force -Path .omx/plans | Select-Obje
ct -ExpandProperty FullName; Get-Content -Encoding UTF8 services/zhongshu-core/
zszj-framework/zszj-spring-boot-starter-web/src/main/java/cn/zszj/framework/api
log/core/filter/ApiAccessLogFilter.java; Get-Content -Encoding UTF8 services/zh
ongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framework/common/u
til/servlet/ServletUtils.java" in E:\zszj-wt-sec-006
 succeeded in 106ms:
package cn.zszj.framework.tracer.config;

import cn.zszj.framework.common.enums.WebFilterOrderEnum;
import cn.zszj.framework.tracer.core.aop.BizTraceAspect;
import cn.zszj.framework.tracer.core.filter.TraceFilter;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBea
n;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationPropertie
s;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

/**
 * Tracer ������
 *
 * @author mashu
 */
@AutoConfiguration
@ConditionalOnClass(name = {
        "io.opentelemetry.api.trace.Tracer", // ���� opentelemetry-api.jar
        "jakarta.servlet.Filter"
})
@EnableConfigurationProperties(TracerProperties.class)
@ConditionalOnProperty(prefix = "zszj.tracer", value = "enable", matchIfMissing
 = true)
public class ZszjTracerAutoConfiguration {

    @Value("${spring.application.name:application}")
    private String applicationName;

    @Bean
    @ConditionalOnMissingBean
    public Tracer tracer() {
        return GlobalOpenTelemetry.getTracer(applicationName);
    }

    @Bean
    @ConditionalOnMissingBean
    public BizTraceAspect bizTracingAop(Tracer tracer) {
        return new BizTraceAspect(tracer);
    }

    /**
     * ���� TraceFilter ����������Ӧ header ���� traceId
     */
    @Bean
    public FilterRegistrationBean<TraceFilter> traceFilter() {
        FilterRegistrationBean<TraceFilter> registrationBean = new FilterRegist
rationBean<>();
        registrationBean.setFilter(new TraceFilter());
        registrationBean.setOrder(WebFilterOrderEnum.TRACE_FILTER);
        return registrationBean;
    }

}
package cn.zszj.framework.common.enums;

/**
 * Web ������˳���ö���࣬��֤���������շ������ǵ�Ԥ��
 *
 *  ���ǵ�ÿ�� starter ����Ҫ�õ��ù����࣬���Էŵ� common ģ���µ� enums ����
 *
 * @author ���Դ��
 */
public interface WebFilterOrderEnum {

    int CORS_FILTER = Integer.MIN_VALUE;

    int TRACE_FILTER = CORS_FILTER + 1;

    int REQUEST_BODY_CACHE_FILTER = Integer.MIN_VALUE + 500;

    int API_ENCRYPT_FILTER = REQUEST_BODY_CACHE_FILTER + 1;

    // OrderedRequestContextFilter Ĭ��Ϊ -105�����ڹ��ʻ������ĵȵ�

    int TENANT_CONTEXT_FILTER = - 104; // ��Ҫ��֤�� ApiAccessLogFilter ǰ��

    int API_ACCESS_LOG_FILTER = -103; // ��Ҫ��֤�� RequestBodyCacheFilter ����

    int XSS_FILTER = -102;  // ��Ҫ��֤�� RequestBodyCacheFilter ����

    // Spring Security Filter Ĭ��Ϊ -100���ɼ� org.springframework.boot.autoconfi
gure.security.SecurityProperties ����������

    int TENANT_SECURITY_FILTER = -99; // ��Ҫ��֤�� Spring Security ����������

    int FLOWABLE_FILTER = -98; // ��Ҫ��֤�� Spring Security ���˺���

    int DEMO_FILTER = Integer.MAX_VALUE;

}
E:\zszj-wt-sec-006\.omx\plans\2026-09-08-���������嵥���˼ܹ�.md
package cn.zszj.framework.apilog.core.filter;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.zszj.framework.apilog.core.annotation.ApiAccessLog;
import cn.zszj.framework.apilog.core.enums.OperateTypeEnum;
import cn.zszj.framework.common.biz.infra.logger.ApiAccessLogCommonApi;
import cn.zszj.framework.common.biz.infra.logger.dto.ApiAccessLogCreateReqDTO;
import cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.zszj.framework.common.pojo.CommonResult;
import cn.zszj.framework.common.util.log.LogSanitizeUtils;
import cn.zszj.framework.common.util.monitor.TracerUtils;
import cn.zszj.framework.common.util.servlet.ServletUtils;
import cn.zszj.framework.web.config.WebProperties;
import cn.zszj.framework.web.core.filter.ApiRequestFilter;
import cn.zszj.framework.web.core.util.WebFrameworkUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static cn.zszj.framework.apilog.core.interceptor.ApiAccessLogInterceptor
.ATTRIBUTE_HANDLER_METHOD;
import static cn.zszj.framework.common.util.json.JsonUtils.toJsonString;

/**
 * API ������־ Filter
 *
 * Ŀ�ģ���¼ API ������־�����ݿ���
 *
 * @author ���Դ��
 */
@Slf4j
public class ApiAccessLogFilter extends ApiRequestFilter {

    private final String applicationName;

    private final ApiAccessLogCommonApi apiAccessLogApi;

    public ApiAccessLogFilter(WebProperties webProperties, String applicationNa
me, ApiAccessLogCommonApi apiAccessLogApi) {
        super(webProperties);
        this.applicationName = applicationName;
        this.apiAccessLogApi = apiAccessLogApi;
    }

    @Override
    @SuppressWarnings("NullableProblems")
    protected void doFilterInternal(HttpServletRequest request, HttpServletResp
onse response, FilterChain filterChain)
            throws ServletException, IOException {
        // ��ÿ�ʼʱ��
        LocalDateTime beginTime = LocalDateTime.now();
        // ��ǰ��ò��������� XssFilter ���˴���
        Map<String, String> queryString = ServletUtils.getParamMap(request);
        String requestBody = ServletUtils.getBody(request);

        try {
            // ����������
            filterChain.doFilter(request, response);
            // ����ִ�У���¼��־
            createApiAccessLog(request, beginTime, queryString, requestBody, nu
ll);
        } catch (Exception ex) {
            // �쳣ִ�У���¼��־
            createApiAccessLog(request, beginTime, queryString, requestBody, ex
);
            throw ex;
        }
    }

    private void createApiAccessLog(HttpServletRequest request, LocalDateTime b
eginTime,
                                    Map<String, String> queryString, String req
uestBody, Exception ex) {
        ApiAccessLogCreateReqDTO accessLog = new ApiAccessLogCreateReqDTO();
        try {
            boolean enable = buildApiAccessLog(accessLog, request, beginTime, q
ueryString, requestBody, ex);
            if (!enable) {
                return;
            }
            apiAccessLogApi.createApiAccessLogAsync(accessLog);
        } catch (Throwable th) {
            log.error("[createApiAccessLog][url({}) traceId({}) д�������־ʧ��]",
 request.getRequestURI(), TracerUtils.getCorrelationId(request), th);
        }
    }

    private boolean buildApiAccessLog(ApiAccessLogCreateReqDTO accessLog, HttpS
ervletRequest request, LocalDateTime beginTime,
                                      Map<String, String> queryString, String r
equestBody, Exception ex) {
        // �жϣ��Ƿ�Ҫ��¼������־
        HandlerMethod handlerMethod = (HandlerMethod) request.getAttribute(ATTR
IBUTE_HANDLER_METHOD);
        ApiAccessLog accessLogAnnotation = null;
        if (handlerMethod != null) {
            accessLogAnnotation = handlerMethod.getMethodAnnotation(ApiAccessLo
g.class);
            if (accessLogAnnotation != null && BooleanUtil.isFalse(accessLogAnn
otation.enable())) {
                return false;
            }
        }

        // �����û���Ϣ
        accessLog.setUserId(WebFrameworkUtils.getLoginUserId(request))
                .setUserType(WebFrameworkUtils.getLoginUserType(request));
        // ���÷��ʽ��
        CommonResult<?> result = WebFrameworkUtils.getCommonResult(request);
        if (result != null) {
            accessLog.setResultCode(result.getCode()).setResultMsg(result.getMs
g());
        } else if (ex != null) {
            accessLog.setResultCode(GlobalErrorCodeConstants.INTERNAL_SERVER_ER
ROR.getCode())
                    .setResultMsg(ExceptionUtil.getRootCauseMessage(ex));
        } else {
            accessLog.setResultCode(GlobalErrorCodeConstants.SUCCESS.getCode())
.setResultMsg("");
        }
        // ���������ֶ�
        accessLog.setTraceId(TracerUtils.getCorrelationId(request)).setApplicat
ionName(applicationName)
                .setRequestUrl(request.getRequestURI()).setRequestMethod(reques
t.getMethod())
                .setUserAgent(ServletUtils.getUserAgent(request)).setUserIp(Ser
vletUtils.getClientIP(request));
        String[] sanitizeKeys = accessLogAnnotation != null ? accessLogAnnotati
on.sanitizeKeys() : null;
        Boolean requestEnable = accessLogAnnotation != null ? accessLogAnnotati
on.requestEnable() : Boolean.TRUE;
        if (!BooleanUtil.isFalse(requestEnable)) { // Ĭ�ϼ�¼�������ж� !false
            Map<String, Object> requestParams = MapUtil.<String, Object>builder
()
                    .put("query", LogSanitizeUtils.sanitizeMap(queryString, san
itizeKeys))
                    .put("body", LogSanitizeUtils.sanitizeJson(requestBody, san
itizeKeys)).build();
            accessLog.setRequestParams(toJsonString(requestParams));
        }
        Boolean responseEnable = accessLogAnnotation != null ? accessLogAnnotat
ion.responseEnable() : Boolean.FALSE;
        if (BooleanUtil.isTrue(responseEnable)) { // Ĭ�ϲ���¼��Ĭ��ǿ��Ҫ�� true
            accessLog.setResponseBody(LogSanitizeUtils.sanitizeResponseBody(res
ult, sanitizeKeys));
        }
        // ����ʱ��
        accessLog.setBeginTime(beginTime).setEndTime(LocalDateTime.now())
                .setDuration((int) LocalDateTimeUtil.between(accessLog.getBegin
Time(), accessLog.getEndTime(), ChronoUnit.MILLIS));

        // ����ģ��
        if (handlerMethod != null) {
            Tag tagAnnotation = handlerMethod.getBeanType().getAnnotation(Tag.c
lass);
            Operation operationAnnotation = handlerMethod.getMethodAnnotation(O
peration.class);
            String operateModule = accessLogAnnotation != null && StrUtil.isNot
Blank(accessLogAnnotation.operateModule()) ?
                    accessLogAnnotation.operateModule() :
                    tagAnnotation != null ? StrUtil.nullToDefault(tagAnnotation
.name(), tagAnnotation.description()) : null;
            String operateName = accessLogAnnotation != null && StrUtil.isNotBl
ank(accessLogAnnotation.operateName()) ?
                    accessLogAnnotation.operateName() :
                    operationAnnotation != null ? operationAnnotation.summary()
 : null;
            OperateTypeEnum operateType = accessLogAnnotation != null && access
LogAnnotation.operateType().length > 0 ?
                    accessLogAnnotation.operateType()[0] : parseOperateLogType(
request);
            accessLog.setOperateModule(operateModule).setOperateName(operateNam
e).setOperateType(operateType.getType());
        }
        return true;
    }

    // ========== ���� @ApiAccessLog��@Swagger ע��  ==========

    private static OperateTypeEnum parseOperateLogType(HttpServletRequest reque
st) {
        RequestMethod requestMethod = RequestMethod.resolve(request.getMethod()
);
        if (requestMethod == null) {
            return OperateTypeEnum.OTHER;
        }
        switch (requestMethod) {
            case GET:
                return OperateTypeEnum.GET;
            case POST:
                return OperateTypeEnum.CREATE;
            case PUT:
                return OperateTypeEnum.UPDATE;
            case DELETE:
                return OperateTypeEnum.DELETE;
            default:
                return OperateTypeEnum.OTHER;
        }
    }

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
 * �ͻ��˹�����
 *
 * @author ���Դ��
 */
public class ServletUtils {

    /**
     * ���� JSON �ַ���
     *
     * @param response ��Ӧ
     * @param object   ���󣬻����л��� JSON �ַ���
     */
    @SuppressWarnings("deprecation") // ����ʹ�� APPLICATION_JSON_UTF8_VALUE����
�������
    public static void writeJSON(HttpServletResponse response, Object object) {
        String content = JsonUtils.toJsonString(object);
        JakartaServletUtil.write(response, content, MediaType.APPLICATION_JSON_
UTF8_VALUE);
    }

    /**
     * @param request ����
     * @return ua
     */
    public static String getUserAgent(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        return ua != null ? ua : "";
    }

    /**
     * �������
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
        // ֻ���� json �����ڶ�ȡ����Ϊֻ�� CacheRequestBodyFilter �Ż���л��棬֧���ظ���
ȡ
        if (isJsonRequest(request)) {
            return JakartaServletUtil.getBody(request);
        }
        return null;
    }

    public static byte[] getBodyBytes(HttpServletRequest request) {
        // ֻ���� json �����ڶ�ȡ����Ϊֻ�� CacheRequestBodyFilter �Ż���л��棬֧���ظ���
ȡ
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

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "rg -
n 'tracer:|zszj.tracer|trace-id|TracerUtils|getCorrelationId|@BizTrace|OpenTele
metry|opentelemetry|MDC|\\.reset\\(' services/zhongshu-core --glob '"'!**/src/t
est/**'"' --glob '"'!**/sql/**'"' --glob '"'!**/docs/**'"'; rg -n 'trace-id|tra
ceId' apps/zhongshu-admin-web/src/config apps/zhongshu-miniapp/src; Get-Content
 services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-monitor/src/mai
n/java/cn/zszj/framework/tracer/core/aop/BizTraceAspect.java; Get-Content servi
ces/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/main/java/cn/
zszj/framework/web/core/handler/GlobalExceptionHandler.java; Get-Content servic
es/zhongshu-core/pom.xml; Get-ChildItem tools -Force; Get-Command mvn,java,jshe
ll,python -ErrorAction SilentlyContinue | Select-Object Name,Source" in E:\zszj
-wt-sec-006
 exited 1 in 221ms:
apps/zhongshu-miniapp/src\api\system\operate-log\index.ts:7:  traceId?: string
apps/zhongshu-miniapp/src\api\system\login-log\index.ts:7:  traceId?: string
apps/zhongshu-miniapp/src\pages-infra\api-error-log\detail\index.vue:14:       
 <wd-cell title="链路追踪" :value="formData?.traceId || '-'" />
apps/zhongshu-miniapp/src\pages-infra\api-access-log\detail\index.vue:14:      
  <wd-cell title="链路追踪" :value="formData?.traceId || '-'" />
apps/zhongshu-miniapp/src\pages-system\operate-log\detail\index.vue:14:        
<wd-cell v-if="formData?.traceId" title="链路追踪" :value="formData.traceId" />
apps/zhongshu-miniapp/src\api\infra\api-error-log\index.ts:7:  traceId: string
apps/zhongshu-miniapp/src\api\infra\api-access-log\index.ts:7:  traceId: string
package cn.zszj.framework.tracer.core.aop;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.zszj.framework.tracer.core.annotation.BizTrace;
import cn.zszj.framework.common.util.spring.SpringExpressionUtils;
import cn.zszj.framework.tracer.core.util.TracerFrameworkUtils;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.util.Map;

import static java.util.Arrays.asList;

/**
 * {@link BizTrace} 切面，记录业务链�? *
 * @author mashu
 */
@Aspect
@AllArgsConstructor
@Slf4j
public class BizTraceAspect {

    private static final String BIZ_OPERATION_NAME_PREFIX = "Biz/";

    private final Tracer tracer;

    @Around(value = "@annotation(trace)")
    public Object around(ProceedingJoinPoint joinPoint, BizTrace trace) throws 
Throwable {
        // 创建 span
        String operationName = getOperationName(joinPoint, trace);
        Span span = tracer.spanBuilder(operationName)
                .setAttribute("component", "biz")
                .startSpan();
        try (Scope ignored = span.makeCurrent()) {
            // 执行原有方法
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            TracerFrameworkUtils.onError(throwable, span);
            throw throwable;
        } finally {
            // 设置 Span �?biz 属�?            setBizTag(span, joinPoint, trace);
            // 完成 Span
            span.end();
        }
    }

    private String getOperationName(ProceedingJoinPoint joinPoint, BizTrace tra
ce) {
        // 自定义操作名
        if (StrUtil.isNotEmpty(trace.operationName())) {
            return BIZ_OPERATION_NAME_PREFIX + trace.operationName();
        }
        // 默认操作名，使用方法�?        return BIZ_OPERATION_NAME_PREFIX
                + joinPoint.getSignature().getDeclaringType().getSimpleName()
                + "/" + joinPoint.getSignature().getName();
    }

    private void setBizTag(Span span, ProceedingJoinPoint joinPoint, BizTrace t
race) {
        try {
            Map<String, Object> result = SpringExpressionUtils.parseExpressions
(joinPoint, asList(trace.type(), trace.id()));
            span.setAttribute(BizTrace.TYPE_TAG, MapUtil.getStr(result, trace.t
ype()));
            span.setAttribute(BizTrace.ID_TAG, MapUtil.getStr(result, trace.id(
)));
        } catch (Exception ex) {
            log.error("[setBizTag][解析 bizType �?bizId 发生异常]", ex);
        }
    }

}
package cn.zszj.framework.web.core.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.zszj.framework.common.biz.infra.logger.ApiErrorLogCommonApi;
import cn.zszj.framework.common.biz.infra.logger.dto.ApiErrorLogCreateReqDTO;
import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.common.exception.util.ServiceExceptionUtil;
import cn.zszj.framework.common.pojo.CommonResult;
import cn.zszj.framework.common.util.collection.SetUtils;
import cn.zszj.framework.common.util.json.JsonUtils;
import cn.zszj.framework.common.util.log.LogSanitizeUtils;
import cn.zszj.framework.common.util.monitor.TracerUtils;
import cn.zszj.framework.common.util.servlet.ServletUtils;
import cn.zszj.framework.web.core.util.WebFrameworkUtils;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.google.common.util.concurrent.UncheckedExecutionException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.Assert;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchExce
ption;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants
.*;

/**
 * 全局异常处理器，�?Exception 翻译�?CommonResult + 对应的异常编�? *
 * @author 芋道源码
 */
@RestControllerAdvice
@Order(0) // 优先于三方库默认的全局异常处理器，例如 JimuReport
@AllArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 忽略�?ServiceException 错误提示，避免打印过�?logger
     */
    public static final Set<String> IGNORE_ERROR_MESSAGES = SetUtils.asSet("无效的
刷新令�?);

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private final String applicationName;

    private final ApiErrorLogCommonApi apiErrorLogApi;

    /**
     * 处理所有异常，主要是提供给 Filter 使用
     * 因为 Filter 不走 SpringMVC 的流程，但是我们又需要兜底处理异常，所以这里提供一个全量的异常处理过程，保持逻辑统一�?     
*
     * @param request 请求
     * @param ex 异常
     * @return 通用返回
     */
    public CommonResult<?> allExceptionHandler(HttpServletRequest request, Thro
wable ex) {
        if (ex instanceof MissingServletRequestParameterException) {
            return missingServletRequestParameterExceptionHandler((MissingServl
etRequestParameterException) ex);
        }
        if (ex instanceof MethodArgumentTypeMismatchException) {
            return methodArgumentTypeMismatchExceptionHandler((MethodArgumentTy
peMismatchException) ex);
        }
        if (ex instanceof MethodArgumentNotValidException) {
            return methodArgumentNotValidExceptionExceptionHandler((MethodArgum
entNotValidException) ex);
        }
        if (ex instanceof BindException) {
            return bindExceptionHandler((BindException) ex);
        }
        if (ex instanceof ConstraintViolationException) {
            return constraintViolationExceptionHandler((ConstraintViolationExce
ption) ex);
        }
        if (ex instanceof ValidationException) {
            return validationException((ValidationException) ex);
        }
        if (ex instanceof MaxUploadSizeExceededException) {
            return maxUploadSizeExceededExceptionHandler((MaxUploadSizeExceeded
Exception) ex);
        }
        if (ex instanceof NoHandlerFoundException) {
            return noHandlerFoundExceptionHandler((NoHandlerFoundException) ex)
;
        }
        if (ex instanceof NoResourceFoundException) {
            return noResourceFoundExceptionHandler(request, (NoResourceFoundExc
eption) ex);
        }
        if (ex instanceof HttpRequestMethodNotSupportedException) {
            return httpRequestMethodNotSupportedExceptionHandler((HttpRequestMe
thodNotSupportedException) ex);
        }
        if (ex instanceof HttpMediaTypeNotSupportedException) {
            return httpMediaTypeNotSupportedExceptionHandler((HttpMediaTypeNotS
upportedException) ex);
        }
        if (ex instanceof ServiceException) {
            return serviceExceptionHandler((ServiceException) ex);
        }
        if (ex instanceof AccessDeniedException) {
            return accessDeniedExceptionHandler(request, (AccessDeniedException
) ex);
        }
        return defaultExceptionHandler(request, ex);
    }

    /**
     * 处理 SpringMVC 请求参数缺失
     *
     * 例如说，接口上设置了 @RequestParam("xx") 参数，结果并未传�?xx 参数
     */
    @ExceptionHandler(value = MissingServletRequestParameterException.class)
    public CommonResult<?> missingServletRequestParameterExceptionHandler(Missi
ngServletRequestParameterException ex) {
        log.warn("[missingServletRequestParameterExceptionHandler]", ex);
        return CommonResult.error(BAD_REQUEST.getCode(), String.format("请求参数缺失:
%s", ex.getParameterName()));
    }

    /**
     * 处理 SpringMVC 请求参数类型错误
     *
     * 例如说，接口上设置了 @RequestParam("xx") 参数�?Integer，结果传�?xx 参数类型�?String
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public CommonResult<?> methodArgumentTypeMismatchExceptionHandler(MethodArg
umentTypeMismatchException ex) {
        log.warn("[methodArgumentTypeMismatchExceptionHandler]", ex);
        return CommonResult.error(BAD_REQUEST.getCode(), String.format("请求参数类型错
误:%s", ex.getMessage()));
    }

    /**
     * 处理 SpringMVC 参数校验不正�?     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public CommonResult<?> methodArgumentNotValidExceptionExceptionHandler(Meth
odArgumentNotValidException ex) {
        log.warn("[methodArgumentNotValidExceptionExceptionHandler]", ex);
        // 获取 errorMessage
        String errorMessage = null;
        FieldError fieldError = ex.getBindingResult().getFieldError();
        if (fieldError == null) {
            // 组合校验，参考自 https://t.zsxq.com/3HVTx
            List<ObjectError> allErrors = ex.getBindingResult().getAllErrors();
            if (CollUtil.isNotEmpty(allErrors)) {
                errorMessage = allErrors.get(0).getDefaultMessage();
            }
        } else {
            errorMessage = fieldError.getDefaultMessage();
        }
        // 转换 CommonResult
        if (StrUtil.isEmpty(errorMessage)) {
            return CommonResult.error(BAD_REQUEST);
        }
        return CommonResult.error(BAD_REQUEST.getCode(), String.format("请求参数不正�
?%s", errorMessage));
    }

    /**
     * 处理 SpringMVC 参数绑定不正确，本质上也是通过 Validator 校验
     */
    @ExceptionHandler(BindException.class)
    public CommonResult<?> bindExceptionHandler(BindException ex) {
        log.warn("[handleBindException]", ex);
        FieldError fieldError = ex.getFieldError();
        assert fieldError != null; // 断言，避免告�?        return CommonResult.error
(BAD_REQUEST.getCode(), String.format("请求参数不正�?%s", fieldError.getDefaultMessag
e()));
    }

    /**
     * 处理 SpringMVC 请求参数类型错误
     *
     * 例如说，接口上设置了 @RequestBody 实体�?xx 属性类型为 Integer，结果传�?xx 参数类型�?String
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @SuppressWarnings("PatternVariableCanBeUsed")
    public CommonResult<?> methodArgumentTypeInvalidFormatExceptionHandler(Http
MessageNotReadableException ex) {
        log.warn("[methodArgumentTypeInvalidFormatExceptionHandler]", ex);
        if (ex.getCause() instanceof InvalidFormatException) {
            InvalidFormatException invalidFormatException = (InvalidFormatExcep
tion) ex.getCause();
            // 安全（ZS-SEC-005）：不回显原始入参�?invalidFormatException.getValue()，避免泄露敏感
数据；仅提示期望类�?            return CommonResult.error(BAD_REQUEST.getCode(), String.
format("请求参数类型错误：期望类�?%s",
                    invalidFormatException.getTargetType() != null ? invalidFor
matException.getTargetType().getSimpleName() : "未知"));
        }
        if (StrUtil.startWith(ex.getMessage(), "Required request body is missin
g")) {
            return CommonResult.error(BAD_REQUEST.getCode(), "请求参数类型错误: request
 body 缺失");
        }
        // 安全（ZS-SEC-005）：畸形 JSON / 请求体无法解析属于客户端错误，返�?400，不落入 500 系统异常，也不回显原始报�
?        return CommonResult.error(BAD_REQUEST.getCode(), "请求参数格式错误：无法解析请求体，请检查
是否为合法 JSON");
    }

    /**
     * 处理 Validator 校验不通过产生的异�?     */
    @ExceptionHandler(value = ConstraintViolationException.class)
    public CommonResult<?> constraintViolationExceptionHandler(ConstraintViolat
ionException ex) {
        log.warn("[constraintViolationExceptionHandler]", ex);
        ConstraintViolation<?> constraintViolation = ex.getConstraintViolations
().iterator().next();
        return CommonResult.error(BAD_REQUEST.getCode(), String.format("请求参数不正�
?%s", constraintViolation.getMessage()));
    }

    /**
     * 处理 Dubbo Consumer 本地参数校验时，抛出�?ValidationException 异常
     */
    @ExceptionHandler(value = ValidationException.class)
    public CommonResult<?> validationException(ValidationException ex) {
        log.warn("[constraintViolationExceptionHandler]", ex);
        // 无法拼接明细的错误信息，因为 Dubbo Consumer 抛出 ValidationException 异常时，是直接的字符串信息，且
人类不可读
        return CommonResult.error(BAD_REQUEST);
    }

    /**
     * 处理上传文件过大异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public CommonResult<?> maxUploadSizeExceededExceptionHandler(MaxUploadSizeE
xceededException ex) {
        return CommonResult.error(BAD_REQUEST.getCode(), "上传文件过大，请调整后重�?);
    }

    /**
     * 处理 SpringMVC 请求地址不存�?     *
     * 注意，它需要设置如下两个配置项�?     * 1. spring.mvc.throw-exception-if-no-handler-foun
d �?true
     * 2. spring.mvc.static-path-pattern �?/statics/**
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public CommonResult<?> noHandlerFoundExceptionHandler(NoHandlerFoundExcepti
on ex) {
        log.warn("[noHandlerFoundExceptionHandler]", ex);
        return CommonResult.error(NOT_FOUND.getCode(), String.format("请求地址不存�?%
s", ex.getRequestURL()));
    }

    /**
     * 处理 SpringMVC 请求地址不存�?     */
    @ExceptionHandler(NoResourceFoundException.class)
    private CommonResult<?> noResourceFoundExceptionHandler(HttpServletRequest 
req, NoResourceFoundException ex) {
        log.warn("[noResourceFoundExceptionHandler]", ex);
        return CommonResult.error(NOT_FOUND.getCode(), String.format("请求地址不存�?%
s", ex.getResourcePath()));
    }

    /**
     * 处理 SpringMVC 请求方法不正�?     *
     * 例如说，A 接口的方法为 GET 方式，结果请求方法为 POST 方式，导致不匹配
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public CommonResult<?> httpRequestMethodNotSupportedExceptionHandler(HttpRe
questMethodNotSupportedException ex) {
        log.warn("[httpRequestMethodNotSupportedExceptionHandler]", ex);
        return CommonResult.error(METHOD_NOT_ALLOWED.getCode(), String.format("
请求方法不正�?%s", ex.getMessage()));
    }

    /**
     * 处理 SpringMVC 请求�?Content-Type 不正�?     *
     * 例如说，A 接口�?Content-Type �?application/json，结果请求的 Content-Type �?applicati
on/octet-stream，导致不匹配
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public CommonResult<?> httpMediaTypeNotSupportedExceptionHandler(HttpMediaT
ypeNotSupportedException ex) {
        log.warn("[httpMediaTypeNotSupportedExceptionHandler]", ex);
        return CommonResult.error(BAD_REQUEST.getCode(), String.format("请求类型不正�
?%s", ex.getMessage()));
    }

    /**
     * 处理 Spring Security 权限不足的异�?     *
     * 来源是，使用 @PreAuthorize 注解，AOP 进行权限拦截
     */
    @ExceptionHandler(value = AccessDeniedException.class)
    public CommonResult<?> accessDeniedExceptionHandler(HttpServletRequest req,
 AccessDeniedException ex) {
        log.warn("[accessDeniedExceptionHandler][userId({}) 无法访问 url({})]", Web
FrameworkUtils.getLoginUserId(req),
                req.getRequestURL(), ex);
        return CommonResult.error(FORBIDDEN);
    }

    /**
     * 处理 Guava UncheckedExecutionException
     *
     * 例如说，缓存加载报错，可�?<a href="https://t.zsxq.com/UszdH">https://t.zsxq.com/Uszd
H</a>
     */
    @ExceptionHandler(value = UncheckedExecutionException.class)
    public CommonResult<?> uncheckedExecutionExceptionHandler(HttpServletReques
t req, UncheckedExecutionException ex) {
        return allExceptionHandler(req, ex.getCause());
    }

    /**
     * 处理业务异常 ServiceException
     *
     * 例如说，商品库存不足，用户手机号已存在�?     */
    @ExceptionHandler(value = ServiceException.class)
    public CommonResult<?> serviceExceptionHandler(ServiceException ex) {
        // 不包含的时候，才进行打印，避免 ex 堆栈过多
        if (!IGNORE_ERROR_MESSAGES.contains(ex.getMessage())) {
            // 即使打印，也只打印第一�?StackTraceElement，并且使�?warn 在控制台输出，更容易看到
            try {
                StackTraceElement[] stackTraces = ex.getStackTrace();
                for (StackTraceElement stackTrace : stackTraces) {
                    if (ObjUtil.notEqual(stackTrace.getClassName(), ServiceExce
ptionUtil.class.getName())) {
                        log.warn("[serviceExceptionHandler]\n\t{}", stackTrace)
;
                        break;
                    }
                }
            } catch (Exception ignored) {
                // 忽略日志，避免影响主流程
            }
        }
        // ServiceException 的错误码可能为成功码，直接调�?CommonResult.error 会抛出异常，此处统一按系统异常处
理�?        // 详见 Issue #1196：https://github.com/YunaiV/ruoyi-vue-pro/issues/119
6
        if (CommonResult.isSuccess(ex.getCode())) {
            return CommonResult.error(INTERNAL_SERVER_ERROR.getCode(), INTERNAL
_SERVER_ERROR.getMsg());
        }
        return CommonResult.error(ex.getCode(), ex.getMessage());
    }

    /**
     * 处理系统异常，兜底处理所有的一�?     */
    @ExceptionHandler(value = Exception.class)
    public CommonResult<?> defaultExceptionHandler(HttpServletRequest req, Thro
wable ex) {
        // 特殊：如果是 ServiceException 的异常，则直接返�?        // 例如说：https://gitee.com/z
hijiantianya/yudao-cloud/issues/ICSSRM、https://gitee.com/zhijiantianya/yudao-cl
oud/issues/ICT6FM
        if (ex.getCause() != null && ex.getCause() instanceof ServiceException)
 {
            return serviceExceptionHandler((ServiceException) ex.getCause());
        }

        // 情况一：处理表不存在的异常
        CommonResult<?> tableNotExistsResult = handleTableNotExists(ex);
        if (tableNotExistsResult != null) {
            return tableNotExistsResult;
        }

        // 情况二：处理异常
        log.error("[defaultExceptionHandler]", ex);
        // 插入异常日志
        createExceptionLog(req, ex);
        // 返回 ERROR CommonResult
        return CommonResult.error(INTERNAL_SERVER_ERROR.getCode(), INTERNAL_SER
VER_ERROR.getMsg());
    }

    private void createExceptionLog(HttpServletRequest req, Throwable e) {
        // 插入错误日志
        ApiErrorLogCreateReqDTO errorLog = new ApiErrorLogCreateReqDTO();
        try {
            // 初始�?errorLog
            buildExceptionLog(errorLog, req, e);
            // 执行插入 errorLog
            apiErrorLogApi.createApiErrorLogAsync(errorLog);
        } catch (Throwable th) {
            log.error("[createExceptionLog][url({}) traceId({}) 写入错误日志失败]", req
.getRequestURI(), TracerUtils.getCorrelationId(req), th);
        }
    }

    private void buildExceptionLog(ApiErrorLogCreateReqDTO errorLog, HttpServle
tRequest request, Throwable e) {
        // 处理用户信息
        errorLog.setUserId(WebFrameworkUtils.getLoginUserId(request));
        errorLog.setUserType(WebFrameworkUtils.getLoginUserType(request));
        // 设置异常字段
        errorLog.setExceptionName(e.getClass().getName());
        errorLog.setExceptionMessage(ExceptionUtil.getMessage(e));
        errorLog.setExceptionRootCauseMessage(ExceptionUtil.getRootCauseMessage
(e));
        errorLog.setExceptionStackTrace(ExceptionUtil.stacktraceToString(e));
        StackTraceElement[] stackTraceElements = e.getStackTrace();
        Assert.notEmpty(stackTraceElements, "异常 stackTraceElements 不能为空");
        StackTraceElement stackTraceElement = stackTraceElements[0];
        errorLog.setExceptionClassName(stackTraceElement.getClassName());
        errorLog.setExceptionFileName(stackTraceElement.getFileName());
        errorLog.setExceptionMethodName(stackTraceElement.getMethodName());
        errorLog.setExceptionLineNumber(stackTraceElement.getLineNumber());
        // 设置其它字段
        errorLog.setTraceId(TracerUtils.getCorrelationId(request));
        errorLog.setApplicationName(applicationName);
        errorLog.setRequestUrl(request.getRequestURI());
        Map<String, Object> requestParams = MapUtil.<String, Object>builder()
                .put("query", LogSanitizeUtils.sanitizeMap(ServletUtils.getPara
mMap(request)))
                .put("body", LogSanitizeUtils.sanitizeJson(ServletUtils.getBody
(request))).build();
        errorLog.setRequestParams(JsonUtils.toJsonString(requestParams));
        errorLog.setRequestMethod(request.getMethod());
        errorLog.setUserAgent(ServletUtils.getUserAgent(request));
        errorLog.setUserIp(ServletUtils.getClientIP(request));
        errorLog.setExceptionTime(LocalDateTime.now());
    }

    /**
     * 处理 Table 不存在的异常情况
     *
     * @param ex 异常
     * @return 如果�?Table 不存在的异常，则返回对应�?CommonResult
     */
    private CommonResult<?> handleTableNotExists(Throwable ex) {
        String message = ExceptionUtil.getRootCauseMessage(ex);
        if (!message.contains("doesn't exist")) {
            return null;
        }
        // 1. 数据报表
        if (message.contains("report_")) {
            log.error("[报表模块 zszj-module-report - 表结构未导入][参�?https://cloud.ioco
der.cn/report/ 开启]");
            return CommonResult.error(NOT_IMPLEMENTED.getCode(),
                    "[报表模块 zszj-module-report - 表结构未导入][参�?https://cloud.iocode
r.cn/report/ 开启]");
        }
        // 2. 工作�?        if (message.contains("bpm_")) {
            log.error("[工作流模�?zszj-module-bpm - 表结构未导入][参�?https://cloud.iocode
r.cn/bpm/ 开启]");
            return CommonResult.error(NOT_IMPLEMENTED.getCode(),
                    "[工作流模�?zszj-module-bpm - 表结构未导入][参�?https://cloud.iocoder.
cn/bpm/ 开启]");
        }
        // 3. 微信公众�?        if (message.contains("mp_")) {
            log.error("[微信公众�?zszj-module-mp - 表结构未导入][参�?https://cloud.iocoder
.cn/mp/build/ 开启]");
            return CommonResult.error(NOT_IMPLEMENTED.getCode(),
                    "[微信公众�?zszj-module-mp - 表结构未导入][参�?https://cloud.iocoder.c
n/mp/build/ 开启]");
        }
        // 4. 商城系统
        if (StrUtil.containsAny(message, "product_", "promotion_", "trade_")) {
            log.error("[商城系统 zszj-module-mall - 已禁用][参�?https://cloud.iocoder.c
n/mall/build/ 开启]");
            return CommonResult.error(NOT_IMPLEMENTED.getCode(),
                    "[商城系统 zszj-module-mall - 已禁用][参�?https://cloud.iocoder.cn/
mall/build/ 开启]");
        }
        // 5. ERP 系统
        if (message.contains("erp_")) {
            log.error("[ERP 系统 zszj-module-erp - 表结构未导入][参�?https://cloud.iocod
er.cn/erp/build/ 开启]");
            return CommonResult.error(NOT_IMPLEMENTED.getCode(),
                    "[ERP 系统 zszj-module-erp - 表结构未导入][参�?https://cloud.iocoder
.cn/erp/build/ 开启]");
        }
        // 6. WMS 仓库管理系统
        if (message.contains("wms_")) {
            log.error("[WMS 仓库管理系统 zszj-module-wms - 表结构未导入][参�?https://doc.ioc
oder.cn/wms/build/ 开启]");
            return CommonResult.error(NOT_IMPLEMENTED.getCode(),
                    "[WMS 仓库管理系统 zszj-module-wms - 表结构未导入][参�?https://doc.iocod
er.cn/wms/build/ 开启]");
        }
        // 7. CRM 系统
        if (message.contains("crm_")) {
            log.error("[CRM 系统 zszj-module-crm - 表结构未导入][参�?https://cloud.iocod
er.cn/crm/build/ 开启]");
            return CommonResult.error(NOT_IMPLEMENTED.getCode(),
                    "[CRM 系统 zszj-module-crm - 表结构未导入][参�?https://cloud.iocoder
.cn/crm/build/ 开启]");
        }
        // 8. MES 系统
        if (message.contains("mes_")) {
            log.error("[MES 系统 zszj-module-mes - 表结构未导入][参�?https://doc.iocoder
.cn/mes/build/ 开启]");
            return CommonResult.error(NOT_IMPLEMENTED.getCode(),
                    "[MES 系统 zszj-module-mes - 表结构未导入][参�?https://doc.iocoder.c
n/mes/build/ 开启]");
        }
        // 9. IM 即时通讯
        if (message.contains("im_")) {
            log.error("[IM 即时通讯 zszj-module-im - 表结构未导入][参�?https://doc.iocoder
.cn/im/build/ 开启]");
            return CommonResult.error(NOT_IMPLEMENTED.getCode(),
                    "[IM 即时通讯 zszj-module-im - 表结构未导入][参�?https://doc.iocoder.c
n/im/build/ 开启]");
        }
        // 10. 支付平台
        if (message.contains("pay_")) {
            log.error("[支付模块 zszj-module-pay - 表结构未导入][参�?https://cloud.iocoder
.cn/pay/build/ 开启]");
            return CommonResult.error(NOT_IMPLEMENTED.getCode(),
                    "[支付模块 zszj-module-pay - 表结构未导入][参�?https://cloud.iocoder.c
n/pay/build/ 开启]");
        }
        // 11. AI 大模�?        if (message.contains("ai_")) {
            log.error("[AI 大模�?zszj-module-ai - 表结构未导入][参�?https://cloud.iocode
r.cn/ai/build/ 开启]");
            return CommonResult.error(NOT_IMPLEMENTED.getCode(),
                    "[AI 大模�?zszj-module-ai - 表结构未导入][参�?https://cloud.iocoder.
cn/ai/build/ 开启]");
        }
        // 12. IoT 物联�?        if (message.contains("iot_")) {
            log.error("[IoT 物联�?zszj-module-iot - 表结构未导入][参�?https://doc.iocode
r.cn/iot/build/ 开启]");
            return CommonResult.error(NOT_IMPLEMENTED.getCode(),
                    "[IoT 物联�?zszj-module-iot - 表结构未导入][参�?https://doc.iocoder.
cn/iot/build/ 开启]");
        }
        return null;
    }

}
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
        <!-- Server 主项�?-->
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
<!-- 请参�?https://doc.iocoder.cn/ai/build/ 文档，完�?AI 模块的启动！！！ -->
<!--        <module>zszj-module-ai</module>-->
    </modules>

    <name>${project.artifactId}</name>
    <description>众墅之家项目基础脚手�?/description>
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
        <!-- maven-surefire-plugin 暂时无法通过 bom 的依赖读取（兼容老版�?IDEA 2024 及以前版本） -->
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
                <!-- maven-surefire-plugin 插件，用于运行单元测试�?-->
                <!-- 注意，需要使�?3.0.X+，因为要支持 Junit 5 版本 -->
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <version>${maven-surefire-plugin.version}</version>
                </plugin>
                <!-- maven-compiler-plugin 插件，解�?spring-boot-configuration-proc
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
                                <!-- 确保 Lombok 生成�?getter/setter 方法能被 MapStruct
 正确识别�?                                     避免出现 No property named “xxx" exists
 的编译错�?-->
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
                        <!-- 编译参数写在 arg 内，解决 Spring Boot 3.2 �?Parameter Name D
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
            <!-- ZS-ENG-002：强�?JDK 17 / Maven 3.8+ 构建基线（D-02 已确�?JDK 17 维护线）�? 
                工具链不满足时构建明确失败，不依赖机器隐式版�?-->
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
                                    <message>本底座固�?JDK 17 构建基线（D-02），请使�?JDK 17
 构建�?/message>
                                </requireJavaVersion>
                                <requireMavenVersion>
                                    <version>[3.8,)</version>
                                    <message>本底座要�?Maven 3.8 及以上版本�?/message>
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

    <!-- 使用 huawei / aliyun �?Maven 源，提升下载速度 -->
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


    Ŀ¼: E:\zszj-wt-sec-006\tools


Mode                 LastWriteTime         Length Name                         
                                        
----                 -------------         ------ ----                         
                                        
d-----         2024/8/14      8:48                apache-maven-3.9.9           
                                        
d-----         2026/8/19      5:59                jdk-17.0.20.1+1              
                                        
-a----          2026/9/9      2:40            164 env.sh                       
                                        
-a----          2026/9/9      2:37      190817615 jdk17.zip                    
                                        
-a----          2026/9/9      2:38        9202456 maven.zip                    
                                        

Name   : python.exe
Source : C:\Program Files\Python311\python.exe




exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "rg -
n 'getAttribute|setAttribute|getHeader|reset|trace-id' services/zhongshu-core/z
szj-framework/zszj-spring-boot-starter-web/src/main/java; Get-Content services/
zhongshu-core/zszj-server/pom.xml; Get-Content services/zhongshu-core/zszj-fram
ework/zszj-spring-boot-starter-monitor/src/main/resources/META-INF/spring/org.s
pringframework.boot.autoconfigure.AutoConfiguration.imports; Get-ChildItem -Pat
h services/zhongshu-core -Directory -Recurse -Filter target | Select-Object -Ex
pandProperty FullName; Get-ChildItem tools/jdk-17.0.20.1+1/bin -Filter 'j*' | S
elect-Object -ExpandProperty Name; Get-ChildItem E:\\ -Force | Select-Object -E
xpandProperty Name; if (Test-Path C:\\Users\\Administrator\\.m2\\repository) { 
Get-ChildItem C:\\Users\\Administrator\\.m2\\repository -Directory | Select-Obj
ect -ExpandProperty Name }; Get-Content services/zhongshu-core/zszj-framework/z
szj-spring-boot-starter-web/src/main/java/cn/zszj/framework/web/config/ZszjWebA
utoConfiguration.java" in E:\zszj-wt-sec-006
 succeeded in 283ms:
E:\zszj-wt-sec-006\services\zhongshu-core\zszj-framework\zszj-common\target
E:\zszj-wt-sec-006\services\zhongshu-core\zszj-framework\zszj-spring-boot-start
er-monitor\target
E:\zszj-wt-sec-006\services\zhongshu-core\zszj-framework\zszj-spring-boot-start
er-web\target
j2gss.dll
j2pcsc.dll
j2pkcs11.dll
jaas.dll
jabswitch.exe
jaccessinspector.exe
jaccesswalker.exe
jar.exe
jarsigner.exe
java.dll
java.exe
javaaccessbridge.dll
javac.exe
javadoc.exe
javajpeg.dll
javap.exe
javaw.exe
jawt.dll
jcmd.exe
jconsole.exe
jdb.exe
jdeprscan.exe
jdeps.exe
jdwp.dll
jfr.exe
jhsdb.exe
jimage.dll
jimage.exe
jinfo.exe
jli.dll
jlink.exe
jmap.exe
jmod.exe
jpackage.dll
jpackage.exe
jps.exe
jrunscript.exe
jshell.exe
jsound.dll
jstack.exe
jstat.exe
jstatd.exe
jsvml.dll
$RECYCLE.BIN
.pnpm-store
migprobe
migshield
other?
System Volume Information
tmp
zszj-base-check
zszj-wt-cfg-001-b
zszj-wt-cfg-004
zszj-wt-db-019-b
zszj-wt-login-001
zszj-wt-login-002
zszj-wt-sec-004
zszj-wt-sec-006
zszj-wt-sec-010
zszj-wt-sec-011-a
����֮��AI����ƽ̨����
����֮�ұ����������
����֮�ұ������Ƶ����
����֮�����ƽ̨
����֮����Ӫƽ̨
众墅之家设计平台
众墅之家爆款短视频创作\.dev-env
console.log
iam003-review-diff.txt
temp_add_tests.ps1
temp_backup.txt
antlr
aopalliance
asm
at
avalon-framework
backport-util-concurrent
ch
classworlds
cn
com
commons-beanutils
commons-chain
commons-cli
commons-codec
commons-collections
commons-digester
commons-io
commons-lang
commons-logging
commons-net
commons-validator
de
dom4j
io
jakarta
javax
junit
log4j
logkit
me
net
org
oro
redis
software
tools
uk
xml-apis
xmlpull
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBea
n;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfig
uration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.boot.context.properties.EnableConfigurationPropertie
s;
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
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHand
lerMapping;

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
    public WebMvcRegistrations webMvcRegistrations(WebProperties webProperties)
 {
        return new WebMvcRegistrations() {

            @Override
            public RequestMappingHandlerMapping getRequestMappingHandlerMapping
() {
                RequestMappingHandlerMapping mapping = new RequestMappingHandle
rMapping();
                // 实例化时就带上前缀
                mapping.setPathPrefixes(buildPathPrefixes(webProperties));
                return mapping;
            }

            /**
             * 构建 prefix �?匹配条件的映�?             */
            private Map<String, Predicate<Class<?>>> buildPathPrefixes(WebPrope
rties webProperties) {
                AntPathMatcher antPathMatcher = new AntPathMatcher(".");
                Map<String, Predicate<Class<?>>> pathPrefixes = Maps.newLinkedH
ashMapWithExpectedSize(2);
                putPathPrefix(pathPrefixes, webProperties.getAdminApi(), antPat
hMatcher);
                putPathPrefix(pathPrefixes, webProperties.getAppApi(), antPathM
atcher);
                return pathPrefixes;
            }

            /**
             * 设置 API 前缀，仅仅匹�?controller 包下�?             */
            private void putPathPrefix(Map<String, Predicate<Class<?>>> pathPre
fixes, WebProperties.Api api, AntPathMatcher matcher) {
                if (api == null || StrUtil.isEmpty(api.getPrefix())) {
                    return;
                }
                pathPrefixes.put(api.getPrefix(), // api 前缀
                        clazz -> clazz.isAnnotationPresent(RestController.class
)
                                && matcher.match(api.getController(), clazz.get
Package().getName()));
            }

        };
    }

    @Bean
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public GlobalExceptionHandler globalExceptionHandler(ApiErrorLogCommonApi a
piErrorLogApi) {
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
     * ZS-SEC-004：由全通配收紧为精确白名单——源/方法/�?暴露�?凭据/maxAge 全部读取 {@link WebProperties.
Cors}�?     * 默认仅本地开发源，杜绝“allowedOriginPattern=* + allowCredentials=true”的任意源携带
凭据缺陷�?     */
    @Bean
    @Order(value = WebFilterOrderEnum.CORS_FILTER) // 特殊：修复因执行顺序影响到跨域配置不生效问题
    public FilterRegistrationBean<CorsFilter> corsFilterBean(WebProperties webP
roperties) {
        WebProperties.Cors cors = webProperties.getCors();
        // 创建 CorsConfiguration 对象
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(cors.isAllowCredentials());
        cors.getAllowedOriginPatterns().forEach(config::addAllowedOriginPattern
); // 设置访问源地址（精确白名单�?        cors.getAllowedHeaders().forEach(config::addAllowe
dHeader); // 设置访问源请求头
        cors.getAllowedMethods().forEach(config::addAllowedMethod); // 设置访问源请求方
�?        cors.getExposedHeaders().forEach(config::addExposedHeader); // 设置暴露给浏
览器的响应头
        config.setMaxAge(cors.getMaxAge()); // 预检缓存秒数
        // 创建 UrlBasedCorsConfigurationSource 对象
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationS
ource();
        source.registerCorsConfiguration("/**", config); // 对接口配置跨域设�?        r
eturn createFilterBean(new CorsFilter(source), WebFilterOrderEnum.CORS_FILTER);
    }

    /**
     * 创建 RequestBodyCacheFilter Bean，可重复读取请求内容
     */
    @Bean
    public FilterRegistrationBean<CacheRequestBodyFilter> requestBodyCacheFilte
r(WebProperties webProperties) {
        // ZS-SEC-008：注�?JSON 请求体缓冲上限，缓冲前受控拒绝超�?body
        return createFilterBean(new CacheRequestBodyFilter(webProperties.getReq
uestBody().getMaxCacheSize()),
                WebFilterOrderEnum.REQUEST_BODY_CACHE_FILTER);
    }

    /**
     * 创建 DemoFilter Bean，演示模�?     */
    @Bean
    @ConditionalOnProperty(value = "zszj.demo", havingValue = "true")
    public FilterRegistrationBean<DemoFilter> demoFilter() {
        return createFilterBean(new DemoFilter(), WebFilterOrderEnum.DEMO_FILTE
R);
    }

    public static <T extends Filter> FilterRegistrationBean<T> createFilterBean
(T filter, Integer order) {
        FilterRegistrationBean<T> bean = new FilterRegistrationBean<>(filter);
        bean.setOrder(order);
        return bean;
    }

    /**
     * 创建 RestTemplate 实例
     *
     * @param restTemplateBuilder {@link RestTemplateAutoConfiguration#restTemp
lateBuilder}
     */
    @Bean
    @ConditionalOnMissingBean
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.build();
    }

}

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "rg -
n -C 7 'allowedHeaders|exposedHeaders' services/zhongshu-core/zszj-framework/zs
zj-spring-boot-starter-web/src/main/java/cn/zszj/framework/web/config/WebProper
ties.java; rg -n -C 8 'allowed-headers|exposed-headers|tracer' services/zhongsh
u-core/zszj-server/src/main/resources; Get-ChildItem services/zhongshu-core/zsz
j-framework/zszj-spring-boot-starter-web/target/surefire-reports | Select-Objec
t Name; Get-Content services/zhongshu-core/zszj-framework/zszj-spring-boot-star
ter-web/src/test/java/cn/zszj/framework/web/config/CorsConfigTest.java; Get-Con
tent services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/fr
amework/common/biz/infra/logger/dto/ApiAccessLogCreateReqDTO.java; Get-Content 
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/main/jav
a/cn/zszj/framework/web/core/filter/ApiRequestFilter.java; Get-ChildItem servic
es/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/target -Force | Se
lect-Object Name,LastWriteTime" in E:\zszj-wt-sec-006
 succeeded in 0ms:
112-         */
113-        private List<String> allowedMethods = new ArrayList<>(List.of(
114-                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD")
);
115-
116-        /**
117-         * 允许的请求头（含租户头 tenant-id）。
118-         */
119:        private List<String> allowedHeaders = new ArrayList<>(List.of(
120-                "Authorization", "Content-Type", "X-Requested-With", "tenan
t-id",
121-                // ZS-SEC-004 P1-2：admin-web service.ts 对每个 GET 注入 Cache-Co
ntrol/Pragma（防缓存）、跨租户注入
122-                // visit-tenant-id、API 加密注入 X-Api-Encrypt；须列入白名单，否则预检 Acces
s-Control-Request-Headers
123-                // 校验失败，拦截所有 admin GET（含租户查询/权限加载）。
124-                "Cache-Control", "Pragma", "visit-tenant-id", "X-Api-Encryp
t"));
125-
126-        /**
127-         * 暴露给浏览器的响应头（ZS-SEC-006 trace-id 关联）。
128-         */
129:        private List<String> exposedHeaders = new ArrayList<>(List.of("trac
e-id"));
130-
131-        /**
132-         * 是否携带凭据。精确源白名单下可为 true。
133-         */
134-        private boolean allowCredentials = true;
135-
136-        /**

Name                                                                  
----                                                                  
2026-09-12T22-01-28_754.dumpstream                                    
2026-09-12T22-03-16_511.dumpstream                                    
cn.zszj.framework.apilog.core.filter.ApiAccessLogFilterTest.txt       
cn.zszj.framework.desensitize.core.DesensitizeTest.txt                
cn.zszj.framework.encrypt.ApiEncryptTest.txt                          
cn.zszj.framework.jackson.config.ZszjJacksonContractTest.txt          
cn.zszj.framework.tracer.core.filter.TraceFilterTest.txt              
cn.zszj.framework.web.config.CorsConfigTest.txt                       
cn.zszj.framework.web.core.filter.CacheRequestBodyFilterTest.txt      
cn.zszj.framework.web.core.handler.GlobalExceptionHandlerTest.txt     
cn.zszj.framework.web.core.util.WebFrameworkUtilsTest.txt             
TEST-cn.zszj.framework.apilog.core.filter.ApiAccessLogFilterTest.xml  
TEST-cn.zszj.framework.desensitize.core.DesensitizeTest.xml           
TEST-cn.zszj.framework.encrypt.ApiEncryptTest.xml                     
TEST-cn.zszj.framework.jackson.config.ZszjJacksonContractTest.xml     
TEST-cn.zszj.framework.tracer.core.filter.TraceFilterTest.xml         
TEST-cn.zszj.framework.web.config.CorsConfigTest.xml                  
TEST-cn.zszj.framework.web.core.filter.CacheRequestBodyFilterTest.xml 
TEST-cn.zszj.framework.web.core.handler.GlobalExceptionHandlerTest.xml
TEST-cn.zszj.framework.web.core.util.WebFrameworkUtilsTest.xml        
package cn.zszj.framework.web.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ZS-SEC-004：CORS 由全通配收紧为精确白名单 + frameOptions 可配�? *
 * 纯单元测试：不启�?Spring 上下文，直接校验 {@link WebProperties.Cors} 的默认值与约束�? */
class CorsConfigTest {

    @Test
    void defaultAllowedOrigins_shouldNotContainBareWildcard() {
        WebProperties.Cors cors = new WebProperties().getCors();
        // 回归护栏：默认白名单绝不能含�?"*"（credentials=true 下通配源是安全缺陷，会被浏览器拒绝且扩大攻击面�?      
  assertFalse(cors.getAllowedOriginPatterns().contains("*"),
                "默认 CORS 白名单禁止裸 *（credentials=true 下通配是安全缺陷）");
        // credentials 默认�?true，与"精确源白名单"搭配才合�?        assertTrue(cors.isAllowC
redentials(),
                "默认 allowCredentials 应为 true（与精确白名单搭配）");
    }

    @Test
    void exposedHeaders_shouldContainTraceId() {
        WebProperties.Cors cors = new WebProperties.Cors();
        assertTrue(cors.getExposedHeaders().contains("trace-id"),
                "ZS-SEC-006 联动：须暴露 trace-id");
    }

    @Test
    void frameOptions_defaultShouldBeSameOriginNotDisabled() {
        assertEquals("SAMEORIGIN", new WebProperties.Cors().getFrameOptions());
    }

    @Test
    void allowedHeaders_shouldCoverAllFrontendSentHeaders() {
        WebProperties.Cors cors = new WebProperties.Cors();
        // ZS-SEC-004 P1-2：admin-web service.ts 对每�?GET 注入 Cache-Control/Pragma
（防缓存），跨租户开启注�?        // visit-tenant-id，API 加密注入 X-Api-Encrypt。收�?allowedHeade
rs 后若缺这些头，浏览器预检
        // Access-Control-Request-Headers 校验不通过 �?拦截所�?admin GET（含租户查询/权限加载）�? 
       assertTrue(cors.getAllowedHeaders().containsAll(List.of(
                        "Authorization", "Content-Type", "X-Requested-With", "t
enant-id",
                        "Cache-Control", "Pragma", "visit-tenant-id", "X-Api-En
crypt")),
                "allowedHeaders 必须覆盖前端 service.ts 实际发送的全部自定义头，否则预检失败拦截请求");
    }

    @Test
    void allowedOriginPatterns_shouldMatchPortlessLocalhost() {
        WebProperties.Cors cors = new WebProperties().getCors();
        // ZS-SEC-004 P1-1：admin-web 本地 VITE_PORT=80，浏览器发出�?Origin 为无端口�?http:/
/localhost�?        // �?Spring 真实匹配语义（CorsConfiguration.checkOrigin）验证白名单是否放行该
无端口源；返回 null 即被拒�?        CorsConfiguration config = new CorsConfiguration();
        cors.getAllowedOriginPatterns().forEach(config::addAllowedOriginPattern
);
        config.setAllowCredentials(cors.isAllowCredentials());
        assertEquals("http://localhost", config.checkOrigin("http://localhost")
,
                "无端�?http://localhost 必须�?CORS 白名单放行（VITE_PORT=80 本地前端场景�?);
        assertEquals("http://127.0.0.1", config.checkOrigin("http://127.0.0.1")
,
                "无端�?http://127.0.0.1 必须�?CORS 白名单放�?);
    }

}
package cn.zszj.framework.common.biz.infra.logger.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * API 访问日志
 *
 * @author 芋道源码
 */
@Data
public class ApiAccessLogCreateReqDTO {

    /**
     * 链路追踪编号
     */
    private String traceId;
    /**
     * 用户编号
     */
    private Long userId;
    /**
     * 用户类型
     */
    private Integer userType;
    /**
     * 应用�?     */
    @NotNull(message = "应用名不能为�?)
    private String applicationName;

    /**
     * 请求方法�?     */
    @NotNull(message = "http 请求方法不能为空")
    private String requestMethod;
    /**
     * 访问地址
     */
    @NotNull(message = "访问地址不能为空")
    private String requestUrl;
    /**
     * 请求参数
     */
    private String requestParams;
    /**
     * 响应结果
     */
    private String responseBody;
    /**
     * 用户 IP
     */
    @NotNull(message = "ip 不能为空")
    private String userIp;
    /**
     * 浏览�?UA
     */
    @NotNull(message = "User-Agent 不能为空")
    private String userAgent;

    /**
     * 操作模块
     */
    private String operateModule;
    /**
     * 操作�?     */
    private String operateName;
    /**
     * 操作分类
     *
     * 枚举，参�?OperateTypeEnum �?     */
    private Integer operateType;

    /**
     * 开始请求时�?     */
    @NotNull(message = "开始请求时间不能为�?)
    private LocalDateTime beginTime;
    /**
     * 结束请求时间
     */
    @NotNull(message = "结束请求时间不能为空")
    private LocalDateTime endTime;
    /**
     * 执行时长，单位：毫秒
     */
    @NotNull(message = "执行时长不能为空")
    private Integer duration;
    /**
     * 结果�?     */
    @NotNull(message = "错误码不能为�?)
    private Integer resultCode;
    /**
     * 结果提示
     */
    private String resultMsg;

}
package cn.zszj.framework.web.core.filter;

import cn.hutool.core.util.StrUtil;
import cn.zszj.framework.web.config.WebProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 过滤 /admin-api�?app-api �?API 请求的过滤器
 *
 * @author 芋道源码
 */
@RequiredArgsConstructor
public abstract class ApiRequestFilter extends OncePerRequestFilter {

    protected final WebProperties webProperties;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 只过�?API 请求的地址
        String apiUri = request.getRequestURI().substring(request.getContextPat
h().length());
        return !StrUtil.startWithAny(apiUri, webProperties.getAdminApi().getPre
fix(), webProperties.getAppApi().getPrefix());
    }

}
classes                                                               
generated-sources                                                     
generated-test-sources                                                
maven-status                                                          
surefire-reports                                                      
test-classes                                                          



exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "@'
import xml.etree.ElementTree as ET, os
p='services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/target/su
refire-reports/TEST-cn.zszj.framework.tracer.core.filter.TraceFilterTest.xml'
r=ET.parse(p).getroot()
cp=next(x.attrib['value'] for x in r.find('properties') if x.attrib['name']=='j
ava.class.path')
print('classpath length',len(cp))
print('\\n'.join(s for s in cp.split(';') if 'zszj-' in s or 'spring-web' in s 
or 'servlet' in s or 'opentelemetry' in s))
print('all paths exist?', all(os.path.exists(p) for p in cp.split(';')))
'@ | python -; git grep -n -C 5 'allowed-headers\\|exposed-headers\\|tracer:' -
- services/zhongshu-core/zszj-server/src/main/resources; git ls-files '*AGENTS*
' '*CLAUDE*'; git status --short" in E:\zszj-wt-sec-006
 succeeded in 77ms:
warning: unable to access 'C:\Users\Administrator/.config/git/ignore': Permissi
on denied
warning: unable to access 'C:\Users\Administrator/.config/git/ignore': Permissi
on denied

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "Get-
ChildItem services/zhongshu-core/zszj-server/src/main/resources -Force; @'
import xml.etree.ElementTree as ET, os
p='services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/target/su
refire-reports/TEST-cn.zszj.framework.tracer.core.filter.TraceFilterTest.xml'
r=ET.parse(p).getroot(); cp=next(x.attrib['value'] for x in r.find('properties'
) if x.attrib['name']=='java.class.path')
print('\\n'.join('missing: '+s for s in cp.split(';') if not os.path.exists(s))
)
'@ | python -; Get-Content services/zhongshu-core/zszj-framework/zszj-common/ta
rget/surefire-reports/TEST-cn.zszj.framework.common.util.monitor.TracerUtilsFal
lbackTest.xml -TotalCount 8; & tools/jdk-17.0.20.1+1/bin/javap.exe -classpath s
ervices/zhongshu-core/zszj-framework/zszj-common/target/classes cn.zszj.framewo
rk.common.util.monitor.TracerUtils" in E:\zszj-wt-sec-006
 succeeded in 306ms:
missing: 
<?xml version="1.0" encoding="UTF-8"?>
<testsuite xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespac
eSchemaLocation="https://maven.apache.org/surefire/maven-surefire-plugin/xsd/su
refire-test-report.xsd" version="3.0.2" name="cn.zszj.framework.common.util.mon
itor.TracerUtilsFallbackTest" time="0.061" tests="11" errors="0" skipped="0" fa
ilures="0">
  <properties>
    <property name="java.specification.version" value="17"/>
    <property name="sun.cpu.isalist" value="amd64"/>
    <property name="sun.jnu.encoding" value="GBK"/>
    <property name="java.class.path" value="E:\zszj-wt-sec-006\services\zhongsh
u-core\zszj-framework\zszj-common\target\test-classes;E:\zszj-wt-sec-006\servic
es\zhongshu-core\zszj-framework\zszj-common\target\classes;C:\Users\Administrat
or\.m2\repository\org\springframework\spring-core\6.2.19\spring-core-6.2.19.jar
;C:\Users\Administrator\.m2\repository\org\springframework\spring-jcl\6.2.19\sp
ring-jcl-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\springframework\s
pring-expression\6.2.19\spring-expression-6.2.19.jar;C:\Users\Administrator\.m2
\repository\org\springframework\spring-aop\6.2.19\spring-aop-6.2.19.jar;C:\User
s\Administrator\.m2\repository\org\springframework\spring-beans\6.2.19\spring-b
eans-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\aspectj\aspectjweaver
\1.9.25.1\aspectjweaver-1.9.25.1.jar;C:\Users\Administrator\.m2\repository\org\
springframework\boot\spring-boot-configuration-processor\3.5.15\spring-boot-con
figuration-processor-3.5.15.jar;C:\Users\Administrator\.m2\repository\org\sprin
gframework\spring-web\6.2.19\spring-web-6.2.19.jar;C:\Users\Administrator\.m2\r
epository\io\micrometer\micrometer-observation\1.15.12\micrometer-observation-1
.15.12.jar;C:\Users\Administrator\.m2\repository\io\micrometer\micrometer-commo
ns\1.15.12\micrometer-commons-1.15.12.jar;C:\Users\Administrator\.m2\repository
\jakarta\servlet\jakarta.servlet-api\6.0.0\jakarta.servlet-api-6.0.0.jar;C:\Use
rs\Administrator\.m2\repository\org\springdoc\springdoc-openapi-starter-webmvc-
ui\2.8.17\springdoc-openapi-starter-webmvc-ui-2.8.17.jar;C:\Users\Administrator
\.m2\repository\org\springdoc\springdoc-openapi-starter-webmvc-api\2.8.17\sprin
gdoc-openapi-starter-webmvc-api-2.8.17.jar;C:\Users\Administrator\.m2\repositor
y\org\springdoc\springdoc-openapi-starter-common\2.8.17\springdoc-openapi-start
er-common-2.8.17.jar;C:\Users\Administrator\.m2\repository\org\springframework\
boot\spring-boot-starter-validation\3.5.15\spring-boot-starter-validation-3.5.1
5.jar;C:\Users\Administrator\.m2\repository\org\apache\tomcat\embed\tomcat-embe
d-el\10.1.55\tomcat-embed-el-10.1.55.jar;C:\Users\Administrator\.m2\repository\
org\hibernate\validator\hibernate-validator\8.0.3.Final\hibernate-validator-8.0
.3.Final.jar;C:\Users\Administrator\.m2\repository\org\jboss\logging\jboss-logg
ing\3.6.3.Final\jboss-logging-3.6.3.Final.jar;C:\Users\Administrator\.m2\reposi
tory\com\fasterxml\classmate\1.7.3\classmate-1.7.3.jar;C:\Users\Administrator\.
m2\repository\io\swagger\core\v3\swagger-core-jakarta\2.2.47\swagger-core-jakar
ta-2.2.47.jar;C:\Users\Administrator\.m2\repository\org\apache\commons\commons-
lang3\3.20.0\commons-lang3-3.20.0.jar;C:\Users\Administrator\.m2\repository\io\
swagger\core\v3\swagger-annotations-jakarta\2.2.47\swagger-annotations-jakarta-
2.2.47.jar;C:\Users\Administrator\.m2\repository\io\swagger\core\v3\swagger-mod
els-jakarta\2.2.47\swagger-models-jakarta-2.2.47.jar;C:\Users\Administrator\.m2
\repository\com\fasterxml\jackson\dataformat\jackson-dataformat-yaml\2.21.4\jac
kson-dataformat-yaml-2.21.4.jar;C:\Users\Administrator\.m2\repository\org\sprin
gframework\spring-webmvc\6.2.19\spring-webmvc-6.2.19.jar;C:\Users\Administrator
\.m2\repository\org\springframework\spring-context\6.2.19\spring-context-6.2.19
.jar;C:\Users\Administrator\.m2\repository\org\webjars\swagger-ui\5.32.2\swagge
r-ui-5.32.2.jar;C:\Users\Administrator\.m2\repository\org\webjars\webjars-locat
or-lite\1.1.3\webjars-locator-lite-1.1.3.jar;C:\Users\Administrator\.m2\reposit
ory\io\opentelemetry\opentelemetry-api\1.65.0\opentelemetry-api-1.65.0.jar;C:\U
sers\Administrator\.m2\repository\io\opentelemetry\opentelemetry-context\1.49.0
\opentelemetry-context-1.49.0.jar;C:\Users\Administrator\.m2\repository\org\pro
jectlombok\lombok\1.18.46\lombok-1.18.46.jar;C:\Users\Administrator\.m2\reposit
ory\org\mapstruct\mapstruct\1.6.3\mapstruct-1.6.3.jar;C:\Users\Administrator\.m
2\repository\org\mapstruct\mapstruct-jdk8\1.6.3\mapstruct-jdk8-1.6.3.jar;C:\Use
rs\Administrator\.m2\repository\org\mapstruct\mapstruct-processor\1.6.3\mapstru
ct-processor-1.6.3.jar;C:\Users\Administrator\.m2\repository\com\google\guava\g
uava\33.7.1-jre\guava-33.7.1-jre.jar;C:\Users\Administrator\.m2\repository\com\
google\guava\failureaccess\1.0.3\failureaccess-1.0.3.jar;C:\Users\Administrator
\.m2\repository\com\google\guava\listenablefuture\9999.0-empty-to-avoid-conflic
t-with-guava\listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar;C:\
Users\Administrator\.m2\repository\org\jspecify\jspecify\1.0.0\jspecify-1.0.0.j
ar;C:\Users\Administrator\.m2\repository\com\google\errorprone\error_prone_anno
tations\2.50.0\error_prone_annotations-2.50.0.jar;C:\Users\Administrator\.m2\re
pository\com\google\j2objc\j2objc-annotations\3.1\j2objc-annotations-3.1.jar;C:
\Users\Administrator\.m2\repository\com\fasterxml\jackson\core\jackson-databind
\2.21.4\jackson-databind-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\f
asterxml\jackson\core\jackson-annotations\2.21\jackson-annotations-2.21.jar;C:\
Users\Administrator\.m2\repository\com\fasterxml\jackson\core\jackson-core\2.21
.4\jackson-core-2.21.4.jar;C:\Users\Administrator\.m2\repository\com\fasterxml\
jackson\datatype\jackson-datatype-jsr310\2.21.4\jackson-datatype-jsr310-2.21.4.
jar;C:\Users\Administrator\.m2\repository\org\slf4j\slf4j-api\2.0.18\slf4j-api-
2.0.18.jar;C:\Users\Administrator\.m2\repository\jakarta\validation\jakarta.val
idation-api\3.0.2\jakarta.validation-api-3.0.2.jar;C:\Users\Administrator\.m2\r
epository\cn\hutool\hutool-all\5.8.47\hutool-all-5.8.47.jar;C:\Users\Administra
tor\.m2\repository\com\alibaba\transmittable-thread-local\2.14.5\transmittable-
thread-local-2.14.5.jar;C:\Users\Administrator\.m2\repository\com\alibaba\fastj
son\2.0.64\fastjson-2.0.64.jar;C:\Users\Administrator\.m2\repository\com\alibab
a\fastjson2\fastjson2-extension\2.0.64\fastjson2-extension-2.0.64.jar;C:\Users\
Administrator\.m2\repository\com\alibaba\fastjson2\fastjson2\2.0.64\fastjson2-2
.0.64.jar;C:\Users\Administrator\.m2\repository\org\dromara\easy-trans-anno\3.1
.8\easy-trans-anno-3.1.8.jar;C:\Users\Administrator\.m2\repository\org\springfr
amework\boot\spring-boot-starter-test\3.5.15\spring-boot-starter-test-3.5.15.ja
r;C:\Users\Administrator\.m2\repository\org\springframework\boot\spring-boot-st
arter\3.5.15\spring-boot-starter-3.5.15.jar;C:\Users\Administrator\.m2\reposito
ry\org\springframework\boot\spring-boot\3.5.15\spring-boot-3.5.15.jar;C:\Users\
Administrator\.m2\repository\org\springframework\boot\spring-boot-autoconfigure
\3.5.15\spring-boot-autoconfigure-3.5.15.jar;C:\Users\Administrator\.m2\reposit
ory\org\springframework\boot\spring-boot-starter-logging\3.5.15\spring-boot-sta
rter-logging-3.5.15.jar;C:\Users\Administrator\.m2\repository\ch\qos\logback\lo
gback-classic\1.5.34\logback-classic-1.5.34.jar;C:\Users\Administrator\.m2\repo
sitory\ch\qos\logback\logback-core\1.5.34\logback-core-1.5.34.jar;C:\Users\Admi
nistrator\.m2\repository\org\apache\logging\log4j\log4j-to-slf4j\2.24.3\log4j-t
o-slf4j-2.24.3.jar;C:\Users\Administrator\.m2\repository\org\apache\logging\log
4j\log4j-api\2.24.3\log4j-api-2.24.3.jar;C:\Users\Administrator\.m2\repository\
org\slf4j\jul-to-slf4j\2.0.18\jul-to-slf4j-2.0.18.jar;C:\Users\Administrator\.m
2\repository\jakarta\annotation\jakarta.annotation-api\2.1.1\jakarta.annotation
-api-2.1.1.jar;C:\Users\Administrator\.m2\repository\org\yaml\snakeyaml\2.4\sna
keyaml-2.4.jar;C:\Users\Administrator\.m2\repository\org\springframework\boot\s
pring-boot-test\3.5.15\spring-boot-test-3.5.15.jar;C:\Users\Administrator\.m2\r
epository\org\springframework\boot\spring-boot-test-autoconfigure\3.5.15\spring
-boot-test-autoconfigure-3.5.15.jar;C:\Users\Administrator\.m2\repository\com\j
ayway\jsonpath\json-path\2.9.0\json-path-2.9.0.jar;C:\Users\Administrator\.m2\r
epository\jakarta\xml\bind\jakarta.xml.bind-api\4.0.5\jakarta.xml.bind-api-4.0.
5.jar;C:\Users\Administrator\.m2\repository\jakarta\activation\jakarta.activati
on-api\2.1.4\jakarta.activation-api-2.1.4.jar;C:\Users\Administrator\.m2\reposi
tory\net\minidev\json-smart\2.5.2\json-smart-2.5.2.jar;C:\Users\Administrator\.
m2\repository\net\minidev\accessors-smart\2.5.2\accessors-smart-2.5.2.jar;C:\Us
ers\Administrator\.m2\repository\org\assertj\assertj-core\3.27.7\assertj-core-3
.27.7.jar;C:\Users\Administrator\.m2\repository\net\bytebuddy\byte-buddy\1.17.8
\byte-buddy-1.17.8.jar;C:\Users\Administrator\.m2\repository\org\awaitility\awa
itility\4.2.2\awaitility-4.2.2.jar;C:\Users\Administrator\.m2\repository\org\ha
mcrest\hamcrest\3.0\hamcrest-3.0.jar;C:\Users\Administrator\.m2\repository\org\
junit\jupiter\junit-jupiter\5.12.2\junit-jupiter-5.12.2.jar;C:\Users\Administra
tor\.m2\repository\org\junit\jupiter\junit-jupiter-api\5.12.2\junit-jupiter-api
-5.12.2.jar;C:\Users\Administrator\.m2\repository\org\opentest4j\opentest4j\1.3
.0\opentest4j-1.3.0.jar;C:\Users\Administrator\.m2\repository\org\junit\platfor
m\junit-platform-commons\1.12.2\junit-platform-commons-1.12.2.jar;C:\Users\Admi
nistrator\.m2\repository\org\apiguardian\apiguardian-api\1.1.2\apiguardian-api-
1.1.2.jar;C:\Users\Administrator\.m2\repository\org\junit\jupiter\junit-jupiter
-params\5.12.2\junit-jupiter-params-5.12.2.jar;C:\Users\Administrator\.m2\repos
itory\org\junit\jupiter\junit-jupiter-engine\5.12.2\junit-jupiter-engine-5.12.2
.jar;C:\Users\Administrator\.m2\repository\org\junit\platform\junit-platform-en
gine\1.12.2\junit-platform-engine-1.12.2.jar;C:\Users\Administrator\.m2\reposit
ory\org\mockito\mockito-junit-jupiter\5.17.0\mockito-junit-jupiter-5.17.0.jar;C
:\Users\Administrator\.m2\repository\org\skyscreamer\jsonassert\1.5.3\jsonasser
t-1.5.3.jar;C:\Users\Administrator\.m2\repository\com\vaadin\external\google\an
droid-json\0.0.20131108.vaadin1\android-json-0.0.20131108.vaadin1.jar;C:\Users\
Administrator\.m2\repository\org\springframework\spring-test\6.2.19\spring-test
-6.2.19.jar;C:\Users\Administrator\.m2\repository\org\xmlunit\xmlunit-core\2.10
.4\xmlunit-core-2.10.4.jar;"/>
    <property name="java.vm.vendor" value="Eclipse Adoptium"/>
Compiled from "TracerUtils.java"
public class cn.zszj.framework.common.util.monitor.TracerUtils {
  public static final java.lang.String ATTR_CORRELATION_ID;
  public static java.lang.String getTraceId();
  public static java.lang.String getCorrelationId();
  public static java.lang.String getCorrelationId(jakarta.servlet.http.HttpServ
letRequest);
  public static java.lang.String generateCorrelationId();
  public static boolean isValidTraceIdFormat(java.lang.String);
}



exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "@'
import xml.etree.ElementTree as ET, subprocess
r=ET.parse('services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/
target/surefire-reports/TEST-cn.zszj.framework.tracer.core.filter.TraceFilterTe
st.xml').getroot()
cp=next(x.attrib['value'] for x in r.find('properties') if x.attrib['name']=='j
ava.class.path').rstrip(';')
script='''
import cn.zszj.framework.common.util.monitor.TracerUtils;
import cn.zszj.framework.tracer.core.filter.TraceFilter;
import cn.zszj.framework.web.config.*;
import org.springframework.mock.web.*;
import org.springframework.web.context.request.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
var req = new MockHttpServletRequest();
var res = new MockHttpServletResponse();
new TraceFilter().doFilter(req, res, new MockFilterChain());
RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
System.out.println(\"BOUND=\"+res.getHeader(\"trace-id\"));
System.out.println(\"REQUEST_OVERLOAD=\"+TracerUtils.getCorrelationId(req));
System.out.println(\"NOARG_1=\"+TracerUtils.getCorrelationId());
System.out.println(\"NOARG_2=\"+TracerUtils.getCorrelationId());
RequestContextHolder.resetRequestAttributes();
var preflight = new MockHttpServletRequest(\"OPTIONS\", \"/admin-api/test\");
preflight.addHeader(\"Origin\", \"http://localhost\");
preflight.addHeader(\"Access-Control-Request-Method\", \"GET\");
preflight.addHeader(\"Access-Control-Request-Headers\", \"trace-id\");
var preflightResponse = new MockHttpServletResponse();
var filter = new ZszjWebAutoConfiguration().corsFilterBean(new WebProperties())
.getFilter();
filter.doFilter(preflight, preflightResponse, new MockFilterChain());
System.out.println(\"PREFLIGHT_STATUS=\"+preflightResponse.getStatus());
System.out.println(\"PREFLIGHT_BODY=\"+preflightResponse.getContentAsString());
/exit
'''
p=subprocess.run(['tools/jdk-17.0.20.1+1/bin/jshell.exe', '--class-path', cp, '
--execution','local','--feedback','concise','-'],input=script,text=True,capture
_output=True,timeout=60)
print(p.stdout); print(p.stderr)
'@ | python -" in E:\zszj-wt-sec-006
 succeeded in 2955ms:

9�� 13, 2026 12:32:56 ���� java.util.prefs.WindowsPreferences <init>
WARNING: Could not open/create prefs root node Software\JavaSoft\Prefs at root 
0xffffffff80000001. Windows RegCreateKeyEx(...) returned error code 5.
9�� 13, 2026 12:32:56 ���� java.util.prefs.WindowsPreferences WindowsRegOpenKey
1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs at ro
ot 0xffffffff80000001.
9�� 13, 2026 12:32:56 ���� java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs at root 0
xffffffff80000001. Windows RegOpenKey(...) returned error code 2.
9�� 13, 2026 12:32:56 ���� java.util.prefs.WindowsPreferences WindowsRegOpenKey
1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs\tool 
at root 0xffffffff80000001.
9�� 13, 2026 12:32:56 ���� java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs\tool at r
oot 0xffffffff80000001. Windows RegOpenKey(...) returned error code 2.
9�� 13, 2026 12:32:56 ���� java.util.prefs.WindowsPreferences WindowsRegOpenKey
1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs\tool\
/J/Shell at root 0xffffffff80000001.
9�� 13, 2026 12:32:56 ���� java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs\tool\/J/S
hell at root 0xffffffff80000001. Windows RegOpenKey(...) returned error code 2.
9�� 13, 2026 12:32:56 ���� java.util.prefs.WindowsPreferences WindowsRegOpenKey
1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs\tool\
/J/Shell at root 0xffffffff80000001.
9�� 13, 2026 12:32:56 ���� java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs\tool\/J/S
hell at root 0xffffffff80000001. Windows RegOpenKey(...) returned error code 2.
9�� 13, 2026 12:32:56 ���� java.util.prefs.WindowsPreferences WindowsRegOpenKey
1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs\tool\
/J/Shell at root 0xffffffff80000001.
9�� 13, 2026 12:32:56 ���� java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs\tool\/J/S
hell at root 0xffffffff80000001. Windows RegOpenKey(...) returned error code 2.
�쳣���� java.lang.NoClassDefFoundError��org/springframework/mock/web/MockHttpSer
vletRequest
      at do_it$Aux (#8:1)
      at (#8:1)
ԭ��: java.lang.ClassNotFoundException�� org.springframework.mock.web.MockHttpSe
rvletRequest
      at URLClassLoader.findClass (URLClassLoader.java:445)
      at DefaultLoaderDelegate$RemoteClassLoader.findClass (DefaultLoaderDelega
te.java:154)
      at ClassLoader.loadClass (ClassLoader.java:592)
      at ClassLoader.loadClass (ClassLoader.java:525)
      ...
�쳣���� java.lang.NoClassDefFoundError��org/springframework/mock/web/MockHttpSer
vletResponse
      at do_it$Aux (#9:1)
      at (#9:1)
ԭ��: java.lang.ClassNotFoundException�� org.springframework.mock.web.MockHttpSe
rvletResponse
      at URLClassLoader.findClass (URLClassLoader.java:445)
      at DefaultLoaderDelegate$RemoteClassLoader.findClass (DefaultLoaderDelega
te.java:154)
      at ClassLoader.loadClass (ClassLoader.java:592)
      at ClassLoader.loadClass (ClassLoader.java:525)
      ...
�쳣���� java.lang.NoClassDefFoundError��org/springframework/mock/web/MockHttpSer
vletResponse
      at (#12:1)
ԭ��: java.lang.ClassNotFoundException�� org.springframework.mock.web.MockHttpSe
rvletResponse
      at URLClassLoader.findClass (URLClassLoader.java:445)
      at DefaultLoaderDelegate$RemoteClassLoader.findClass (DefaultLoaderDelega
te.java:154)
      at ClassLoader.loadClass (ClassLoader.java:592)
      at ClassLoader.loadClass (ClassLoader.java:525)
      ...
�쳣���� java.lang.NoClassDefFoundError��cn/zszj/framework/common/util/monitor/Tr
acerUtils
      at (#14:1)
ԭ��: java.lang.ClassNotFoundException�� cn.zszj.framework.common.util.monitor.T
racerUtils
      at URLClassLoader.findClass (URLClassLoader.java:445)
      at DefaultLoaderDelegate$RemoteClassLoader.findClass (DefaultLoaderDelega
te.java:154)
      at ClassLoader.loadClass (ClassLoader.java:592)
      at ClassLoader.loadClass (ClassLoader.java:525)
      ...
�쳣���� java.lang.NoClassDefFoundError��cn/zszj/framework/common/util/monitor/Tr
acerUtils
      at (#15:1)
ԭ��: java.lang.ClassNotFoundException�� cn.zszj.framework.common.util.monitor.T
racerUtils
      at URLClassLoader.findClass (URLClassLoader.java:445)
      at DefaultLoaderDelegate$RemoteClassLoader.findClass (DefaultLoaderDelega
te.java:154)
      at ClassLoader.loadClass (ClassLoader.java:592)
      at ClassLoader.loadClass (ClassLoader.java:525)
      ...
�쳣���� java.lang.NoClassDefFoundError��org/springframework/web/context/request/
RequestContextHolder
      at (#16:1)
ԭ��: java.lang.ClassNotFoundException�� org.springframework.web.context.request
.RequestContextHolder
      at URLClassLoader.findClass (URLClassLoader.java:445)
      at DefaultLoaderDelegate$RemoteClassLoader.findClass (DefaultLoaderDelega
te.java:154)
      at ClassLoader.loadClass (ClassLoader.java:592)
      at ClassLoader.loadClass (ClassLoader.java:525)
      ...
�쳣���� java.lang.NoClassDefFoundError��org/springframework/mock/web/MockHttpSer
vletRequest
      at do_it$Aux (#17:1)
      at (#17:1)
ԭ��: java.lang.ClassNotFoundException�� org.springframework.mock.web.MockHttpSe
rvletRequest
      at URLClassLoader.findClass (URLClassLoader.java:445)
      at DefaultLoaderDelegate$RemoteClassLoader.findClass (DefaultLoaderDelega
te.java:154)
      at ClassLoader.loadClass (ClassLoader.java:592)
      at ClassLoader.loadClass (ClassLoader.java:525)
      ...
�쳣���� java.lang.NoClassDefFoundError��org/springframework/mock/web/MockHttpSer
vletRequest
      at (#18:1)
ԭ��: java.lang.ClassNotFoundException�� org.springframework.mock.web.MockHttpSe
rvletRequest
      at URLClassLoader.findClass (URLClassLoader.java:445)
      at DefaultLoaderDelegate$RemoteClassLoader.findClass (DefaultLoaderDelega
te.java:154)
      at ClassLoader.loadClass (ClassLoader.java:592)
      at ClassLoader.loadClass (ClassLoader.java:525)
      ...
�쳣���� java.lang.NoClassDefFoundError��org/springframework/mock/web/MockHttpSer
vletRequest
      at (#19:1)
ԭ��: java.lang.ClassNotFoundException�� org.springframework.mock.web.MockHttpSe
rvletRequest
      at URLClassLoader.findClass (URLClassLoader.java:445)
      at DefaultLoaderDelegate$RemoteClassLoader.findClass (DefaultLoaderDelega
te.java:154)
      at ClassLoader.loadClass (ClassLoader.java:592)
      at ClassLoader.loadClass (ClassLoader.java:525)
      ...
�쳣���� java.lang.NoClassDefFoundError��org/springframework/mock/web/MockHttpSer
vletRequest
      at (#20:1)
ԭ��: java.lang.ClassNotFoundException�� org.springframework.mock.web.MockHttpSe
rvletRequest
      at URLClassLoader.findClass (URLClassLoader.java:445)
      at DefaultLoaderDelegate$RemoteClassLoader.findClass (DefaultLoaderDelega
te.java:154)
      at ClassLoader.loadClass (ClassLoader.java:592)
      at ClassLoader.loadClass (ClassLoader.java:525)
      ...
�쳣���� java.lang.NoClassDefFoundError��org/springframework/mock/web/MockHttpSer
vletResponse
      at do_it$Aux (#21:1)
      at (#21:1)
ԭ��: java.lang.ClassNotFoundException�� org.springframework.mock.web.MockHttpSe
rvletResponse
      at URLClassLoader.findClass (URLClassLoader.java:445)
      at DefaultLoaderDelegate$RemoteClassLoader.findClass (DefaultLoaderDelega
te.java:154)
      at ClassLoader.loadClass (ClassLoader.java:592)
      at ClassLoader.loadClass (ClassLoader.java:525)
      ...
�쳣���� java.lang.NoClassDefFoundError��cn/zszj/framework/web/config/ZszjWebAuto
Configuration
      at do_it$Aux (#22:1)
      at (#22:1)
ԭ��: java.lang.ClassNotFoundException�� cn.zszj.framework.web.config.ZszjWebAut
oConfiguration
      at URLClassLoader.findClass (URLClassLoader.java:445)
      at DefaultLoaderDelegate$RemoteClassLoader.findClass (DefaultLoaderDelega
te.java:154)
      at ClassLoader.loadClass (ClassLoader.java:592)
      at ClassLoader.loadClass (ClassLoader.java:525)
      ...
�쳣���� java.lang.NoClassDefFoundError��org/springframework/mock/web/MockHttpSer
vletResponse
      at (#24:1)
ԭ��: java.lang.ClassNotFoundException�� org.springframework.mock.web.MockHttpSe
rvletResponse
      at URLClassLoader.findClass (URLClassLoader.java:445)
      at DefaultLoaderDelegate$RemoteClassLoader.findClass (DefaultLoaderDelega
te.java:154)
      at ClassLoader.loadClass (ClassLoader.java:592)
      at ClassLoader.loadClass (ClassLoader.java:525)
      ...
�쳣���� java.lang.NoClassDefFoundError��org/springframework/mock/web/MockHttpSer
vletResponse
      at (#25:1)
ԭ��: java.lang.ClassNotFoundException�� org.springframework.mock.web.MockHttpSe
rvletResponse
      at URLClassLoader.findClass (URLClassLoader.java:445)
      at DefaultLoaderDelegate$RemoteClassLoader.findClass (DefaultLoaderDelega
te.java:154)
      at ClassLoader.loadClass (ClassLoader.java:592)
      at ClassLoader.loadClass (ClassLoader.java:525)
      ...


exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "@'
import xml.etree.ElementTree as ET, subprocess
r=ET.parse('services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/
target/surefire-reports/TEST-cn.zszj.framework.tracer.core.filter.TraceFilterTe
st.xml').getroot()
cp=next(x.attrib['value'] for x in r.find('properties') if x.attrib['name']=='j
ava.class.path').rstrip(';')
script='''
import cn.zszj.framework.common.util.monitor.TracerUtils;
import cn.zszj.framework.tracer.core.filter.TraceFilter;
import cn.zszj.framework.web.config.*;
import org.springframework.mock.web.*;
import org.springframework.web.context.request.*;
var req = new MockHttpServletRequest();
var res = new MockHttpServletResponse();
new TraceFilter().doFilter(req, res, new MockFilterChain());
RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
System.out.println(\"BOUND=\"+res.getHeader(\"trace-id\"));
System.out.println(\"REQUEST_OVERLOAD=\"+TracerUtils.getCorrelationId(req));
System.out.println(\"NOARG_1=\"+TracerUtils.getCorrelationId());
System.out.println(\"NOARG_2=\"+TracerUtils.getCorrelationId());
RequestContextHolder.resetRequestAttributes();
var preflight = new MockHttpServletRequest(\"OPTIONS\", \"/admin-api/test\");
preflight.addHeader(\"Origin\", \"http://localhost\");
preflight.addHeader(\"Access-Control-Request-Method\", \"GET\");
preflight.addHeader(\"Access-Control-Request-Headers\", \"trace-id\");
var preflightResponse = new MockHttpServletResponse();
var filter = new ZszjWebAutoConfiguration().corsFilterBean(new WebProperties())
.getFilter();
filter.doFilter(preflight, preflightResponse, new MockFilterChain());
System.out.println(\"PREFLIGHT_STATUS=\"+preflightResponse.getStatus());
System.out.println(\"PREFLIGHT_BODY=\"+preflightResponse.getContentAsString());
/exit
'''
p=subprocess.run(['tools/jdk-17.0.20.1+1/bin/jshell.exe', '-J--class-path='+cp,
'--class-path', cp, '--execution','local','--feedback','concise','-'],input=scr
ipt,text=True,capture_output=True,timeout=60)
print(p.stdout); print(p.stderr[:1500])
'@ | python -" in E:\zszj-wt-sec-006
 succeeded in 11162ms:
BOUND=4391d3b9bb1b4d33aa4ed61e18232d9e
REQUEST_OVERLOAD=4391d3b9bb1b4d33aa4ed61e18232d9e
NOARG_1=c36d598465174e7f8a1b4a8283bc3470
NOARG_2=129b346350664ea99a2c29c6f5656639
PREFLIGHT_STATUS=200
PREFLIGHT_BODY=

9�� 13, 2026 12:33:25 ���� java.util.prefs.WindowsPreferences <init>
WARNING: Could not open/create prefs root node Software\JavaSoft\Prefs at root 
0xffffffff80000001. Windows RegCreateKeyEx(...) returned error code 5.
9�� 13, 2026 12:33:25 ���� java.util.prefs.WindowsPreferences WindowsRegOpenKey
1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs at ro
ot 0xffffffff80000001.
9�� 13, 2026 12:33:25 ���� java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs at root 0
xffffffff80000001. Windows RegOpenKey(...) returned error code 2.
9�� 13, 2026 12:33:25 ���� java.util.prefs.WindowsPreferences WindowsRegOpenKey
1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs\tool 
at root 0xffffffff80000001.
9�� 13, 2026 12:33:25 ���� java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs\tool at r
oot 0xffffffff80000001. Windows RegOpenKey(...) returned error code 2.
9�� 13, 2026 12:33:25 ���� java.util.prefs.WindowsPreferences WindowsRegOpenKey
1
WARNING: Trying to recreate Windows registry node Software\JavaSoft\Prefs\tool\
/J/Shell at root 0xffffffff80000001.
9�� 13, 2026 12:33:25 ���� java.util.prefs.WindowsPreferences openKey
WARNING: Could not open windows registry node Software\JavaSoft\Prefs\tool\/J/S
hell at root 0xffffffff80000001. Windows RegOpenKey(...) returned error code 2.
9�� 13, 2026 12:33:25 ���� java.util.prefs.WindowsPreferences WindowsRegO

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "@'
from pathlib import Path
for name in ['application.yaml','application-local.yaml','logback-spring.xml']:
 p=Path('services/zhongshu-core/zszj-server/src/main/resources')/name
 b=p.read_bytes()
 print(name, b[:20])
 for n,s in enumerate(b.decode('utf-8',errors='replace').splitlines(),1):
  if any(t in s for t in ['cors:', 'allowed-headers','exposed-headers','trace-i
d','tracer:', 'tid','pattern']): print(n,s)
'@ | python -; git diff 8785b9f3"'^ 8785b9f3 --check; Get-Content .git; Get-Con
tent services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/target/
surefire-reports/cn.zszj.framework.apilog.core.filter.ApiAccessLogFilterTest.tx
t' in E:\zszj-wt-sec-006
 succeeded in 22ms:
gitdir: E:/众墅之家AI赋能平台底座/.git/worktrees/zszj-wt-sec-006
-------------------------------------------------------------------------------
Test set: cn.zszj.framework.apilog.core.filter.ApiAccessLogFilterTest
-------------------------------------------------------------------------------
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.267 s -- in c
n.zszj.framework.apilog.core.filter.ApiAccessLogFilterTest

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "@'
import xml.etree.ElementTree as ET, subprocess
r=ET.parse('services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/
target/surefire-reports/TEST-cn.zszj.framework.tracer.core.filter.TraceFilterTe
st.xml').getroot()
cp=next(x.attrib['value'] for x in r.find('properties') if x.attrib['name']=='j
ava.class.path').rstrip(';')
script='''
import cn.zszj.framework.web.config.*;
import org.springframework.mock.web.*;
var preflight = new MockHttpServletRequest(\"OPTIONS\", \"/admin-api/test\");
preflight.setServerPort(48080);
preflight.addHeader(\"Origin\", \"http://localhost\");
preflight.addHeader(\"Access-Control-Request-Method\", \"GET\");
preflight.addHeader(\"Access-Control-Request-Headers\", \"trace-id\");
var response = new MockHttpServletResponse();
var filter = new ZszjWebAutoConfiguration().corsFilterBean(new WebProperties())
.getFilter();
filter.doFilter(preflight, response, new MockFilterChain());
System.out.println(\"PREFLIGHT_STATUS=\"+response.getStatus());
System.out.println(\"PREFLIGHT_BODY=\"+response.getContentAsString());
var props = new WebProperties();
props.getCors().getAllowedHeaders().add(\"trace-id\");
var response2 = new MockHttpServletResponse();
new ZszjWebAutoConfiguration().corsFilterBean(props).getFilter().doFilter(prefl
ight, response2, new MockFilterChain());
System.out.println(\"WITH_ALLOWED_HEADER_STATUS=\"+response2.getStatus());
System.out.println(\"WITH_ALLOWED_HEADER_ALLOW=\"+response2.getHeader(\"Access-
Control-Allow-Headers\"));
int passed = 0;
for (String name : java.util.List.of(\"cn.zszj.framework.common.util.monitor.Tr
acerUtilsFallbackTest\", \"cn.zszj.framework.tracer.core.filter.TraceFilterTest
\")) { try { var type = Class.forName(name); var ctor = type.getDeclaredConstru
ctor(); ctor.setAccessible(true); for(var method : type.getDeclaredMethods()) {
 if (method.isAnnotationPresent(org.junit.jupiter.api.Test.class)) { method.set
Accessible(true); method.invoke(ctor.newInstance()); passed++; } } } catch (Thr
owable e) { System.out.println(\"TEST_FAILURE=\"+e); } }
System.out.println(\"NEW_TESTS_PASSED=\"+passed);
/exit
'''
common_tests='services/zhongshu-core/zszj-framework/zszj-common/target/test-cla
sses'
cp=cp+';'+str(__import__('pathlib').Path(common_tests).resolve())
p=subprocess.run(['tools/jdk-17.0.20.1+1/bin/jshell.exe', '-J--class-path='+cp,
'--class-path', cp, '--execution','local','--feedback','concise','-'],input=scr
ipt,text=True,capture_output=True,timeout=60)
print(p.stdout)
print('\\n'.join(s for s in p.stderr.splitlines() if 'Error' in s or 'Exception
' in s))
'@ | python -" in E:\zszj-wt-sec-006
 succeeded in 11334ms:
PREFLIGHT_STATUS=403
PREFLIGHT_BODY=Invalid CORS request
WITH_ALLOWED_HEADER_STATUS=200
WITH_ALLOWED_HEADER_ALLOW=trace-id
NEW_TESTS_PASSED=21



exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "@'
from pathlib import Path
for file, start, end in [
('services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/frame
work/common/util/monitor/TracerUtils.java',55,104),
('services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-monitor/src/ma
in/java/cn/zszj/framework/tracer/core/filter/TraceFilter.java',35,68),
('services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/main/j
ava/cn/zszj/framework/web/config/WebProperties.java',116,130)]:
 print(str(Path(file).resolve()))
 for i,l in enumerate(Path(file).read_text(encoding='utf-8').splitlines(),1):
  if start<=i<=end: print(f'{i}: {l}'.encode('ascii','backslashreplace').decode
('ascii'))
'@ | python -; git status --short" in E:\zszj-wt-sec-006
 succeeded in 125ms:
warning: unable to access 'C:\Users\Administrator/.config/git/ignore': Permissi
on denied
warning: unable to access 'C:\Users\Administrator/.config/git/ignore': Permissi
on denied

codex
The basic fallback path works, but the no-argument accessor breaks request corr
elation and the default CORS policy blocks the newly supported incoming header.
 Both issues were reproduced using the existing compiled classes.

Full review comments:

- [P2] Allow the new trace-id request header through CORS — E:/zszj-wt-sec-006/
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-monitor/src/main
/java/cn/zszj/framework/tracer/core/filter/TraceFilter.java:61-63
  When a cross-origin browser client supplies the newly supported `trace-id` he
ader, `WebProperties.Cors.allowedHeaders` rejects it before this filter runs. A
n OPTIONS request from `http://localhost` to port 48080 with `Access-Control-Re
quest-Headers: trace-id` returns 403. The existing `exposedHeaders` entry only 
permits reading response headers. Add `trace-id` to the request-header allowlis
t and cover this with a preflight test.

- [P2] Reuse the bound request ID in the no-argument accessor — E:/zszj-wt-sec-
006/services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/fra
mework/common/util/monitor/TracerUtils.java:70-72
  Without an OTel span, this accessor generates a fresh ID on every call even w
hen `TraceFilter` has already bound an ID and the request is available through 
`RequestContextHolder`. Its results therefore differ from both the response hea
der and the access/error logs, contradicting its documented request-attribute f
allback. Resolve the current request through the existing `ServletUtils.getRequ
est()` and delegate to the request overload, generating a one-off ID only outsi
de a request.
The basic fallback path works, but the no-argument accessor breaks request correlation and the default CORS policy blocks the newly supported incoming header. Both issues were reproduced using the existing compiled classes.

Full review comments:

- [P2] Allow the new trace-id request header through CORS — E:/zszj-wt-sec-006/services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-monitor/src/main/java/cn/zszj/framework/tracer/core/filter/TraceFilter.java:61-63
  When a cross-origin browser client supplies the newly supported `trace-id` header, `WebProperties.Cors.allowedHeaders` rejects it before this filter runs. An OPTIONS request from `http://localhost` to port 48080 with `Access-Control-Request-Headers: trace-id` returns 403. The existing `exposedHeaders` entry only permits reading response headers. Add `trace-id` to the request-header allowlist and cover this with a preflight test.

- [P2] Reuse the bound request ID in the no-argument accessor — E:/zszj-wt-sec-006/services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framework/common/util/monitor/TracerUtils.java:70-72
  Without an OTel span, this accessor generates a fresh ID on every call even when `TraceFilter` has already bound an ID and the request is available through `RequestContextHolder`. Its results therefore differ from both the response header and the access/error logs, contradicting its documented request-attribute fallback. Resolve the current request through the existing `ServletUtils.getRequest()` and delegate to the request overload, generating a one-off ID only outside a request.
