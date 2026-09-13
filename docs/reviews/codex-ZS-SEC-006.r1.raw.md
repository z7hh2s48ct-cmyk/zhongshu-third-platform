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
session id: 01a096d6-e2f9-7900-81cb-73b07b0e3d96
--------
user
commit 30e7735
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'Get-
Location; git status --short; git rev-parse --show-toplevel; git show --stat --
oneline --decorate --no-renames 30e77350; Get-ChildItem -Force -Name AGENTS*' i
n E:\zszj-wt-sec-006
 succeeded in 47ms:
E:/zszj-wt-sec-006
30e77350 (HEAD -> feat/sec-006) fix(sec-006): 修复 codex r0 两项 P2（CORS allowedHea
ders 放行 trace-id 请求头 + 无参 getCorrelationId 复用绑定 correlationId）
 .../framework/common/util/monitor/TracerUtils.java | 15 ++++--
 .../util/monitor/TracerUtilsFallbackTest.java      | 58 ++++++++++++++++++++++
 .../zszj/framework/web/config/WebProperties.java   |  7 ++-
 .../zszj/framework/web/config/CorsConfigTest.java  | 31 ++++++++++++
 4 files changed, 106 insertions(+), 5 deletions(-)
Path              
----              
E:\zszj-wt-sec-006



exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "git 
show --format=fuller --no-ext-diff --no-renames 30e77350; Get-ChildItem -Force 
-Name; Get-ChildItem -Path E:\\ -Force -Name AGENTS*; Get-ChildItem -Recurse -F
orce -File -Include AGENTS.md,AGENTS.override.md -ErrorAction SilentlyContinue 
| Select-Object -ExpandProperty FullName" in E:\zszj-wt-sec-006
 succeeded in 1110ms:
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "Get-
Content services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj
/framework/common/util/monitor/TracerUtils.java; Get-Content services/zhongshu-
core/zszj-framework/zszj-common/src/main/java/cn/zszj/framework/common/util/ser
vlet/ServletUtils.java; Get-Content services/zhongshu-core/zszj-framework/zszj-
common/pom.xml; rg -n 'getCorrelationId\\(|RequestContextHolder|RequestContextF
ilter|RequestContextListener|allowed-headers|allowedHeaders' services/zhongshu-
core --glob '*.java' --glob '*.yaml' --glob '*.yml' --glob '*.xml'; git ls-file
s '*AGENTS*' '*CLAUDE*' '*GEMINI*' '*codex*'" in E:\zszj-wt-sec-006
 succeeded in 275ms:
services/zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\main\jav
a\cn\zszj\framework\web\core\util\WebFrameworkUtils.java:15:import org.springfr
amework.web.context.request.RequestContextHolder;
services/zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\main\jav
a\cn\zszj\framework\web\core\util\WebFrameworkUtils.java:207:        RequestAtt
ributes requestAttributes = RequestContextHolder.getRequestAttributes();
services/zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\main\jav
a\cn\zszj\framework\web\core\handler\GlobalExceptionHandler.java:362:          
  log.error("[createExceptionLog][url({}) traceId({}) 写入错误日志失败]", req.getReques
tURI(), TracerUtils.getCorrelationId(req), th);
services/zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\main\jav
a\cn\zszj\framework\web\core\handler\GlobalExceptionHandler.java:383:        er
rorLog.setTraceId(TracerUtils.getCorrelationId(request));
services/zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\t
est\java\cn\zszj\framework\signature\core\aop\ApiSignatureAspectTest.java:21:im
port org.springframework.web.context.request.RequestContextHolder;
services/zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\t
est\java\cn\zszj\framework\signature\core\aop\ApiSignatureAspectTest.java:73:  
      RequestContextHolder.resetRequestAttributes();
services/zhongshu-core\zszj-framework\zszj-spring-boot-starter-protection\src\t
est\java\cn\zszj\framework\signature\core\aop\ApiSignatureAspectTest.java:80:  
      RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(re
quest));
services/zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\main\jav
a\cn\zszj\framework\web\config\WebProperties.java:119:        private List<Stri
ng> allowedHeaders = new ArrayList<>(List.of(
services/zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\main\jav
a\cn\zszj\framework\web\config\WebProperties.java:125:                // ZS-SEC
-006 P2-1：前端拦截器注入 trace-id 请求头用于端到端关联，须在预检 allowedHeaders 中放行，
services/zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\main\jav
a\cn\zszj\framework\apilog\core\filter\ApiAccessLogFilter.java:90:            l
og.error("[createApiAccessLog][url({}) traceId({}) 写入访问日志失败]", request.getReque
stURI(), TracerUtils.getCorrelationId(request), th);
services/zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\main\jav
a\cn\zszj\framework\apilog\core\filter\ApiAccessLogFilter.java:120:        acce
ssLog.setTraceId(TracerUtils.getCorrelationId(request)).setApplicationName(appl
icationName)
services/zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:6:import org.springframewor
k.web.context.request.RequestContextHolder;
services/zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:12: * ZS-SEC-006：验证无 OTel S
pan 时 TracerUtils.getCorrelationId() 提供有效 fallback 关联 ID。
services/zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:14: * RED 现状：getCorrelation
Id() 方法不存在（编译失败）。
services/zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:20:        // 清除 RequestCon
textHolder，避免测试间污染
services/zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:21:        RequestContextHo
lder.resetRequestAttributes();
services/zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:26:        // 无有效 OTel Span
 时，getCorrelationId() 必须返回非空关联 ID
services/zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:27:        String id = Trac
erUtils.getCorrelationId();
services/zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:34:        String id = Trac
erUtils.getCorrelationId();
services/zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:42:        String id = Trac
erUtils.getCorrelationId();
services/zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:51:        String id1 = Tra
cerUtils.getCorrelationId(request);
services/zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:52:        String id2 = Tra
cerUtils.getCorrelationId(request);
services/zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:61:        String id1 = Tra
cerUtils.getCorrelationId(req1);
services/zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:62:        String id2 = Tra
cerUtils.getCorrelationId(req2);
services/zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:72:        String id = Trac
erUtils.getCorrelationId(request);
services/zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:116:    // ========== ZS-SE
C-006 codex r0 P2-2: no-arg getCorrelationId() must reuse bound request ID ====
======
services/zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:120:        // P2-2 RED：无参 
getCorrelationId() 应通过 ServletUtils.getRequest() 解析当前请求，
services/zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:121:        // 委托给 getCorre
lationId(request)，复用 TraceFilter 已绑定的 ID。
services/zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:127:        // 模拟 Servlet 容
器已绑定请求到当前线程（TraceFilter 运行后 RequestContextHolder 已有值）
services/zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:128:        RequestContextH
older.setRequestAttributes(new ServletRequestAttributes(request));
services/zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:131:        String result =
 TracerUtils.getCorrelationId();
services/zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:133:                "P2-2：无
参 getCorrelationId() 在请求上下文存在时应复用 TraceFilter 绑定的 ID，"
services/zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:139:        // P2-2：同一线程多次调
用无参 getCorrelationId() 应返回相同值（稳定性）
services/zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:143:        RequestContextH
older.setRequestAttributes(new ServletRequestAttributes(request));
services/zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:145:        String first = 
TracerUtils.getCorrelationId();
services/zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:146:        String second =
 TracerUtils.getCorrelationId();
services/zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:147:        String third = 
TracerUtils.getCorrelationId();
services/zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:156:        RequestContextH
older.resetRequestAttributes(); // 确保无请求上下文
services/zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:158:        String id = Tra
cerUtils.getCorrelationId();
services/zhongshu-core\zszj-framework\zszj-common\src\main\java\cn\zszj\framewo
rk\common\util\servlet\ServletUtils.java:12:import org.springframework.web.cont
ext.request.RequestContextHolder;
services/zhongshu-core\zszj-framework\zszj-common\src\main\java\cn\zszj\framewo
rk\common\util\servlet\ServletUtils.java:52:        RequestAttributes requestAt
tributes = RequestContextHolder.getRequestAttributes();
services/zhongshu-core\zszj-framework\zszj-common\src\main\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtils.java:18: *   <li>{@link #getCorrelationId()}
 / {@link #getCorrelationId(HttpServletRequest)} —— 统一关联 ID 入口：
services/zhongshu-core\zszj-framework\zszj-common\src\main\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtils.java:48:     * 若需保证非空的请求关联标识，请使用 {@link #get
CorrelationId()}。</p>
services/zhongshu-core\zszj-framework\zszj-common\src\main\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtils.java:64:     * 委托给 {@link #getCorrelationId(
HttpServletRequest)} 复用 TraceFilter 已绑定的 ID，
services/zhongshu-core\zszj-framework\zszj-common\src\main\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtils.java:69:    public static String getCorrelat
ionId() {
services/zhongshu-core\zszj-framework\zszj-common\src\main\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtils.java:78:            return getCorrelationId(
request);
services/zhongshu-core\zszj-framework\zszj-common\src\main\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtils.java:93:    public static String getCorrelat
ionId(HttpServletRequest request) {
services/zhongshu-core\zszj-framework\zszj-common\src\main\java\cn\zszj\framewo
rk\common\enums\WebFilterOrderEnum.java:20:    // OrderedRequestContextFilter 默
认为 -105，用于国际化上下文等等
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
docs/reviews/codex-ZS-CFG-001.B-r1.raw.md
docs/reviews/codex-ZS-CFG-001.B-r2.raw.md
docs/reviews/codex-ZS-CFG-001.B.md
docs/reviews/codex-ZS-CFG-001.B.raw.md
docs/reviews/codex-ZS-CFG-002.B.md
docs/reviews/codex-ZS-CFG-002.B.raw.md
docs/reviews/codex-ZS-GOV-001-P0.md
docs/reviews/codex-ZS-GOV-001-P0.raw.md
docs/reviews/codex-ZS-IAM-003.md
docs/reviews/codex-ZS-IAM-003.r1.raw.md
docs/reviews/codex-ZS-IAM-003.raw.md
docs/reviews/codex-ZS-LOGIN-001.md
docs/reviews/codex-ZS-LOGIN-001.r1.raw.md
docs/reviews/codex-ZS-LOGIN-001.raw.md
docs/reviews/codex-ZS-OPS-001-step4.md
docs/reviews/codex-ZS-OPS-001-step4.raw.md
docs/reviews/codex-ZS-OPS-001-step5-r1.raw.md
docs/reviews/codex-ZS-OPS-001-step5-r2.raw.md
docs/reviews/codex-ZS-OPS-001-step5.md
docs/reviews/codex-ZS-OPS-001-step5.raw.md
docs/reviews/codex-ZS-SEC-003.md
docs/reviews/codex-ZS-SEC-003.raw.md
docs/reviews/codex-ZS-SEC-004-r1.raw.md
docs/reviews/codex-ZS-SEC-004.md
docs/reviews/codex-ZS-SEC-004.raw.md
docs/reviews/codex-ZS-SEC-008-r1.raw.md
docs/reviews/codex-ZS-SEC-008.md
docs/reviews/codex-ZS-SEC-008.raw.md
docs/reviews/codex-ZS-SEC-009-r1.raw.md
docs/reviews/codex-ZS-SEC-009-r2.raw.md
docs/reviews/codex-ZS-SEC-009.md
docs/reviews/codex-ZS-SEC-009.raw.md
docs/reviews/codex-ZS-SEC-010-r1.raw.md
docs/reviews/codex-ZS-SEC-010.md
docs/reviews/codex-ZS-SEC-010.raw.md
docs/reviews/codex-ZS-SEC-011.A.md
docs/reviews/codex-ZS-SEC-011.A.r1.raw.md
docs/reviews/codex-ZS-SEC-011.A.r2.raw.md
docs/reviews/codex-ZS-SEC-011.A.r3.raw.md
docs/reviews/codex-ZS-SEC-011.A.raw.md
docs/reviews/codex-fix-5p2-ea739b9c-r1.raw.md
docs/reviews/codex-fix-5p2-ea739b9c.md
docs/reviews/codex-fix-5p2-ea739b9c.raw.md
docs/reviews/codex-hotfix-A-r1.raw.md
docs/reviews/codex-hotfix-A-r2.raw.md
docs/reviews/codex-hotfix-A.md
docs/reviews/codex-hotfix-A.raw.md
docs/reviews/codex-hotfix-B-r1.raw.md
docs/reviews/codex-hotfix-B-r2.raw.md
docs/reviews/codex-hotfix-B-r3.raw.md
docs/reviews/codex-hotfix-B.md
docs/reviews/codex-hotfix-B.raw.md
docs/reviews/codex-hotfix-C-r1.raw.md
docs/reviews/codex-hotfix-C.md
docs/reviews/codex-hotfix-C.raw.md
docs/reviews/codex-hotfix-D.md
docs/reviews/codex-hotfix-D.raw.md
docs/reviews/codex-hotfix-E.md
docs/reviews/codex-hotfix-E.raw.md

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "Get-
Content -Encoding UTF8 services/zhongshu-core/zszj-framework/zszj-spring-boot-s
tarter-web/src/main/java/cn/zszj/framework/web/config/WebProperties.java; Get-C
ontent -Encoding UTF8 services/zhongshu-core/zszj-framework/zszj-spring-boot-st
arter-web/src/main/java/cn/zszj/framework/web/config/ZszjWebAutoConfiguration.j
ava; Get-Content -Encoding UTF8 services/zhongshu-core/zszj-framework/zszj-spri
ng-boot-starter-monitor/src/main/java/cn/zszj/framework/monitor/core/filter/Tra
ceFilter.java; rg -n 'getCorrelationId\\(|ATTR_CORRELATION_ID|allowed-headers|a
llowedHeaders|trace-id' apps services --glob '"'!pnpm-lock.yaml'"' --glob '"'!p
ackage-lock.json'"'; Get-Content -Encoding UTF8 services/zhongshu-core/zszj-fra
mework/zszj-common/src/test/java/cn/zszj/framework/common/util/monitor/TracerUt
ilsFallbackTest.java; Get-Content -Encoding UTF8 services/zhongshu-core/zszj-fr
amework/zszj-common/src/main/java/cn/zszj/framework/common/enums/WebFilterOrder
Enum.java; Get-Command java,mvn -ErrorAction SilentlyContinue | Select-Object N
ame,Source; Get-ChildItem services/zhongshu-core -Force -Name" in E:\zszj-wt-se
c-006
 succeeded in 243ms:
Get-Content : �Ҳ���·����E:\zszj-wt-sec-006\services\zhongshu-core\zszj-framewor
k\zszj-spring-boot-starter-monitor\src\
main\java\cn\zszj\framework\monitor\core\filter\TraceFilter.java������Ϊ��·�����
��ڡ�
����λ�� ��:2 �ַ�: 326
+ ... ation.java; Get-Content -Encoding UTF8 services/zhongshu-core/zszj-fr ...
+                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    + CategoryInfo          : ObjectNotFound: (E:\zszj-wt-sec-...raceFilter.jav
a:String) [Get-Content], ItemNotFoundEx 
   ception
    + FullyQualifiedErrorId : PathNotFound,Microsoft.PowerShell.Commands.GetCon
tentCommand
 
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\web\config\CorsConfigTest.java:34:        assertTrue(cors.g
etExposedHeaders().contains("trace-id"),
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\web\config\CorsConfigTest.java:35:                "ZS-SEC-0
06 联动：须暴露 trace-id");
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\web\config\CorsConfigTest.java:44:    void allowedHeaders_s
houldCoverAllFrontendSentHeaders() {
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\web\config\CorsConfigTest.java:47:        // visit-tenant-i
d，API 加密注入 X-Api-Encrypt。收窄 allowedHeaders 后若缺这些头，浏览器预检
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\web\config\CorsConfigTest.java:52:                "allowedH
eaders 必须覆盖前端 service.ts 实际发送的全部自定义头，否则预检失败拦截请求");
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\web\config\CorsConfigTest.java:69:    // ========== ZS-SEC-
006 codex r0 P2-1: trace-id must be allowed as REQUEST header ==========
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\web\config\CorsConfigTest.java:72:    void allowedHeaders_s
houldContainTraceId_forPreflight() {
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\web\config\CorsConfigTest.java:74:        // ZS-SEC-006 P2-
1：跨域浏览器客户端发送 trace-id 请求头（由前端拦截器注入），
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\web\config\CorsConfigTest.java:75:        // 预检 Access-Cont
rol-Request-Headers: trace-id 必须通过 allowedHeaders 校验，否则 403。
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\web\config\CorsConfigTest.java:77:        assertTrue(cors.g
etAllowedHeaders().contains("trace-id"),
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\web\config\CorsConfigTest.java:78:                "ZS-SEC-0
06 P2-1：allowedHeaders 必须包含 trace-id，否则跨域预检拒绝该请求头");
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\web\config\CorsConfigTest.java:94:        List<String> resu
lt = config.checkHeaders(List.of("trace-id"));
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\web\config\CorsConfigTest.java:95:        assertNotNull(res
ult, "ZS-SEC-006 P2-1：预检 Access-Control-Request-Headers: trace-id 应通过");
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\web\config\CorsConfigTest.java:96:        assertTrue(result
.contains("trace-id"),
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\web\config\CorsConfigTest.java:97:                "预检结果应包含 
trace-id，实际=" + result);
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:17: * ZS-SEC-006：验证
 TraceFilter 在无 OTel、外部合法/畸形 trace-id、过滤器早退等场景下的行为。
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:25:    // =========
= ①无 OTel 时响应头 trace-id 非空且为合法 fallback ==========
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:35:        String h
eaderValue = response.getHeader("trace-id");
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:36:        assertNo
tNull(headerValue, "响应头 trace-id 不应为 null");
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:37:        assertFa
lse(headerValue.isEmpty(), "无 OTel 时响应头 trace-id 不应为空串（核心缺口）");
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:50:        Object a
ttr = request.getAttribute(TracerUtils.ATTR_CORRELATION_ID);
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:52:        assertEq
uals(response.getHeader("trace-id"), attr,
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:56:    // =========
= ②外部传入合法 trace-id：按信任边界复用 ==========
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:62:        request.
addHeader("trace-id", validExternal);
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:68:        // 合法外部 
trace-id 可复用为关联 ID（便于端到端串联）
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:69:        assertEq
uals(validExternal, response.getHeader("trace-id"),
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:70:                
"合法外部 trace-id 应被复用为关联 ID");
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:77:        request.
addHeader("trace-id", upperCase);
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:84:        assertEq
uals(upperCase.toLowerCase(), response.getHeader("trace-id"),
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:85:                
"大写合法 trace-id 应归一化为小写");
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:88:    // =========
= ③外部畸形/超长 trace-id：拒绝并替换为服务端生成值，不回显 ==========
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:94:        request.
addHeader("trace-id", malformed);
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:100:        String 
headerValue = response.getHeader("trace-id");
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:102:        assertF
alse(headerValue.contains(malformed), "畸形外部 trace-id 不得回显");
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:112:        request
.addHeader("trace-id", oversized);
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:118:        String 
headerValue = response.getHeader("trace-id");
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:119:        assertN
otEquals(oversized, headerValue, "超长 trace-id 不得回显");
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:127:        request
.addHeader("trace-id", "");
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:133:        String 
headerValue = response.getHeader("trace-id");
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:134:        assertF
alse(headerValue.isEmpty(), "空串外部 trace-id 应被替换为服务端生成值");
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:157:        String 
headerValue = response.getHeader("trace-id");
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:174:        assertN
otEquals(res1.getHeader("trace-id"), res2.getHeader("trace-id"),
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:182:        // 验证：即
使外部传入了一个"看起来像"有效 trace-id 的值，它只被用于关联，
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:186:        request
.addHeader("trace-id", externalId);
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:193:        assertE
quals(externalId, response.getHeader("trace-id"));
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\test\jav
a\cn\zszj\framework\tracer\core\filter\TraceFilterTest.java:195:        assertE
quals(externalId, request.getAttribute(TracerUtils.ATTR_CORRELATION_ID));
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\main\jav
a\cn\zszj\framework\web\core\handler\GlobalExceptionHandler.java:362:          
  log.error("[createExceptionLog][url({}) traceId({}) 写入错误日志失败]", req.getReques
tURI(), TracerUtils.getCorrelationId(req), th);
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\main\jav
a\cn\zszj\framework\web\core\handler\GlobalExceptionHandler.java:383:        er
rorLog.setTraceId(TracerUtils.getCorrelationId(request));
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\main\jav
a\cn\zszj\framework\web\config\WebProperties.java:119:        private List<Stri
ng> allowedHeaders = new ArrayList<>(List.of(
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\main\jav
a\cn\zszj\framework\web\config\WebProperties.java:125:                // ZS-SEC
-006 P2-1：前端拦截器注入 trace-id 请求头用于端到端关联，须在预检 allowedHeaders 中放行，
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\main\jav
a\cn\zszj\framework\web\config\WebProperties.java:126:                // 否则跨域浏览
器 OPTIONS Access-Control-Request-Headers: trace-id 被拒 403。
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\main\jav
a\cn\zszj\framework\web\config\WebProperties.java:127:                "trace-id
"));
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\main\jav
a\cn\zszj\framework\web\config\WebProperties.java:130:         * 暴露给浏览器的响应头（ZS-
SEC-006 trace-id 关联）。
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\main\jav
a\cn\zszj\framework\web\config\WebProperties.java:132:        private List<Stri
ng> exposedHeaders = new ArrayList<>(List.of("trace-id"));
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\main\jav
a\cn\zszj\framework\apilog\core\filter\ApiAccessLogFilter.java:90:            l
og.error("[createApiAccessLog][url({}) traceId({}) 写入访问日志失败]", request.getReque
stURI(), TracerUtils.getCorrelationId(request), th);
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-web\src\main\jav
a\cn\zszj\framework\apilog\core\filter\ApiAccessLogFilter.java:120:        acce
ssLog.setTraceId(TracerUtils.getCorrelationId(request)).setApplicationName(appl
icationName)
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
\java\cn\zszj\framework\tracer\core\filter\TraceFilter.java:33:    private stat
ic final String HEADER_NAME_TRACE_ID = "trace-id";
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-monitor\src\main
\java\cn\zszj\framework\tracer\core\filter\TraceFilter.java:41:        request.
setAttribute(TracerUtils.ATTR_CORRELATION_ID, correlationId);
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-monitor\src\main
\java\cn\zszj\framework\tracer\core\filter\TraceFilter.java:51:     * 2. 外部传入合法
格式 trace-id → 归一化为小写后复用（信任边界：仅用于关联，不作授权凭据）
services\zhongshu-core\zszj-framework\zszj-spring-boot-starter-monitor\src\main
\java\cn\zszj\framework\tracer\core\filter\TraceFilter.java:60:        // 优先级 2
：外部传入合法 trace-id（格式校验：32 字符 hex）
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:12: * ZS-SEC-006：验证无 OTel S
pan 时 TracerUtils.getCorrelationId() 提供有效 fallback 关联 ID。
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:14: * RED 现状：getCorrelation
Id() 方法不存在（编译失败）。
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:26:        // 无有效 OTel Span
 时，getCorrelationId() 必须返回非空关联 ID
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:27:        String id = Trac
erUtils.getCorrelationId();
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:34:        String id = Trac
erUtils.getCorrelationId();
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:42:        String id = Trac
erUtils.getCorrelationId();
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:51:        String id1 = Tra
cerUtils.getCorrelationId(request);
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:52:        String id2 = Tra
cerUtils.getCorrelationId(request);
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:61:        String id1 = Tra
cerUtils.getCorrelationId(req1);
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:62:        String id2 = Tra
cerUtils.getCorrelationId(req2);
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:71:        request.setAttri
bute(TracerUtils.ATTR_CORRELATION_ID, preset);
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:72:        String id = Trac
erUtils.getCorrelationId(request);
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:116:    // ========== ZS-SE
C-006 codex r0 P2-2: no-arg getCorrelationId() must reuse bound request ID ====
======
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:120:        // P2-2 RED：无参 
getCorrelationId() 应通过 ServletUtils.getRequest() 解析当前请求，
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:121:        // 委托给 getCorre
lationId(request)，复用 TraceFilter 已绑定的 ID。
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:125:        request.setAttr
ibute(TracerUtils.ATTR_CORRELATION_ID, boundId);
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:131:        String result =
 TracerUtils.getCorrelationId();
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:133:                "P2-2：无
参 getCorrelationId() 在请求上下文存在时应复用 TraceFilter 绑定的 ID，"
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:139:        // P2-2：同一线程多次调
用无参 getCorrelationId() 应返回相同值（稳定性）
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:142:        request.setAttr
ibute(TracerUtils.ATTR_CORRELATION_ID, boundId);
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:145:        String first = 
TracerUtils.getCorrelationId();
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:146:        String second =
 TracerUtils.getCorrelationId();
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:147:        String third = 
TracerUtils.getCorrelationId();
services\zhongshu-core\zszj-framework\zszj-common\src\test\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtilsFallbackTest.java:158:        String id = Tra
cerUtils.getCorrelationId();
services\zhongshu-core\zszj-framework\zszj-common\src\main\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtils.java:18: *   <li>{@link #getCorrelationId()}
 / {@link #getCorrelationId(HttpServletRequest)} —— 统一关联 ID 入口：
services\zhongshu-core\zszj-framework\zszj-common\src\main\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtils.java:30:    public static final String ATTR_
CORRELATION_ID = "zszj.correlation.id";
services\zhongshu-core\zszj-framework\zszj-common\src\main\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtils.java:33:     * 合法 trace-id 格式长度（128-bit = 32
 hex chars）
services\zhongshu-core\zszj-framework\zszj-common\src\main\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtils.java:48:     * 若需保证非空的请求关联标识，请使用 {@link #get
CorrelationId()}。</p>
services\zhongshu-core\zszj-framework\zszj-common\src\main\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtils.java:64:     * 委托给 {@link #getCorrelationId(
HttpServletRequest)} 复用 TraceFilter 已绑定的 ID，
services\zhongshu-core\zszj-framework\zszj-common\src\main\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtils.java:69:    public static String getCorrelat
ionId() {
services\zhongshu-core\zszj-framework\zszj-common\src\main\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtils.java:78:            return getCorrelationId(
request);
services\zhongshu-core\zszj-framework\zszj-common\src\main\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtils.java:93:    public static String getCorrelat
ionId(HttpServletRequest request) {
services\zhongshu-core\zszj-framework\zszj-common\src\main\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtils.java:101:            Object attr = request.g
etAttribute(ATTR_CORRELATION_ID);
services\zhongshu-core\zszj-framework\zszj-common\src\main\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtils.java:107:            request.setAttribute(AT
TR_CORRELATION_ID, generated);
services\zhongshu-core\zszj-framework\zszj-common\src\main\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtils.java:126:     * 校验外部传入的 trace-id 格式是否合法。
services\zhongshu-core\zszj-framework\zszj-common\src\main\java\cn\zszj\framewo
rk\common\util\monitor\TracerUtils.java:131:     * @param value 外部传入的 trace-id 
值
package cn.zszj.framework.common.util.monitor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ZS-SEC-006����֤�� OTel Span ʱ TracerUtils.getCorrelationId() �ṩ��Ч fallback 
���� ID��
 *
 * RED ��״��getCorrelationId() ���������ڣ�����ʧ�ܣ���
 */
class TracerUtilsFallbackTest {

    @AfterEach
    void tearDown() {
        // ��� RequestContextHolder��������Լ���Ⱦ
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void getCorrelationId_noSpan_returnsNonEmpty() {
        // ����Ч OTel Span ʱ��getCorrelationId() ���뷵�طǿչ��� ID
        String id = TracerUtils.getCorrelationId();
        assertNotNull(id, "���� ID ��ӦΪ null");
        assertFalse(id.isEmpty(), "���� ID ��ӦΪ�մ�������ȱ���޸���");
    }

    @Test
    void getCorrelationId_noSpan_boundedLength() {
        String id = TracerUtils.getCorrelationId();
        // �н磺���Ȳ����� 64 �ַ���128-bit hex = 32 �ַ�����������
        assertTrue(id.length() <= 64, "���� ID ����Ӧ�н磬ʵ��=" + id.length());
        assertTrue(id.length() >= 16, "���� ID Ӧ���㹻�أ�ʵ�ʳ���=" + id.length());
    }

    @Test
    void getCorrelationId_noSpan_validHexFormat() {
        String id = TracerUtils.getCorrelationId();
        // ��ʽ�Ϸ�������ʮ�������ַ�
        assertTrue(id.matches("[0-9a-fA-F]+"), "���� ID ӦΪ�Ϸ�ʮ�����Ƹ�ʽ��ʵ��=" +
 id);
    }

    @Test
    void getCorrelationId_sameRequest_stable() {
        // ͬһ�����������ڣ��������÷�����ͬ ID
        MockHttpServletRequest request = new MockHttpServletRequest();
        String id1 = TracerUtils.getCorrelationId(request);
        String id2 = TracerUtils.getCorrelationId(request);
        assertEquals(id1, id2, "ͬһ��������������Ӧ������ͬ�Ĺ��� ID���ȶ��ԣ�");
    }

    @Test
    void getCorrelationId_differentRequests_noCollision() {
        // ��ͬ���󲻴���
        MockHttpServletRequest req1 = new MockHttpServletRequest();
        MockHttpServletRequest req2 = new MockHttpServletRequest();
        String id1 = TracerUtils.getCorrelationId(req1);
        String id2 = TracerUtils.getCorrelationId(req2);
        assertNotEquals(id1, id2, "��ͬ����Ӧ���ɲ�ͬ�Ĺ��� ID�������ţ�");
    }

    @Test
    void getCorrelationId_withRequestAttribute_usesAttribute() {
        // ����������������ù��� ID��Ӧ����ʹ��
        MockHttpServletRequest request = new MockHttpServletRequest();
        String preset = "abcdef0123456789abcdef0123456789";
        request.setAttribute(TracerUtils.ATTR_CORRELATION_ID, preset);
        String id = TracerUtils.getCorrelationId(request);
        assertEquals(preset, id, "Ӧ����ʹ�������������Ѱ󶨵Ĺ��� ID");
    }

    @Test
    void getTraceId_noSpan_returnsEmpty_semanticsPreserved() {
        // ���� getTraceId() ���岻�䣺�� OTel Span ʱ�Է��ؿմ�
        String traceId = TracerUtils.getTraceId();
        assertEquals("", traceId, "getTraceId() ���岻�䣺�� Span ʱ���ؿմ�");
    }

    @Test
    void isValidTraceIdFormat_validId() {
        assertTrue(TracerUtils.isValidTraceIdFormat("abcdef0123456789abcdef0123
456789"));
        assertTrue(TracerUtils.isValidTraceIdFormat("ABCDEF0123456789ABCDEF0123
456789"));
        assertTrue(TracerUtils.isValidTraceIdFormat("00000000000000000000000000
000000"));
    }

    @Test
    void isValidTraceIdFormat_invalidId() {
        assertFalse(TracerUtils.isValidTraceIdFormat(null));
        assertFalse(TracerUtils.isValidTraceIdFormat(""));
        assertFalse(TracerUtils.isValidTraceIdFormat("short"));
        assertFalse(TracerUtils.isValidTraceIdFormat("abcdef0123456789abcdef012
345678900")); // 34 chars, too long
        assertFalse(TracerUtils.isValidTraceIdFormat("abcdef0123456789abcdef012
345678")); // 31 chars, too short
        assertFalse(TracerUtils.isValidTraceIdFormat("zzzzzz0123456789abcdef012
3456789")); // invalid chars
        assertFalse(TracerUtils.isValidTraceIdFormat("abcdef0123456789abcdef012
345678<script>")); // injection attempt
    }

    @Test
    void generateCorrelationId_producesValidId() {
        String id = TracerUtils.generateCorrelationId();
        assertNotNull(id);
        assertEquals(32, id.length(), "���ɵĹ��� ID ӦΪ 32 �ַ���128-bit hex��");
        assertTrue(id.matches("[0-9a-f]+"), "���ɵĹ��� ID ӦΪСдʮ������");
    }

    @Test
    void generateCorrelationId_unique() {
        String id1 = TracerUtils.generateCorrelationId();
        String id2 = TracerUtils.generateCorrelationId();
        assertNotEquals(id1, id2, "�������ɵĹ��� ID ��Ӧ�ظ�");
    }

    // ========== ZS-SEC-006 codex r0 P2-2: no-arg getCorrelationId() must reus
e bound request ID ==========

    @Test
    void getCorrelationId_noArg_withBoundRequestContext_reusesBoundId() {
        // P2-2 RED���޲� getCorrelationId() Ӧͨ�� ServletUtils.getRequest() ����
��ǰ����
        // ί�и� getCorrelationId(request)������ TraceFilter �Ѱ󶨵� ID��
        // ��ǰʵ��ÿ�ζ������� ID������Ӧͷ/��־��һ�¡�
        MockHttpServletRequest request = new MockHttpServletRequest();
        String boundId = "aabbccdd00112233aabbccdd00112233";
        request.setAttribute(TracerUtils.ATTR_CORRELATION_ID, boundId);

        // ģ�� Servlet �����Ѱ����󵽵�ǰ�̣߳�TraceFilter ���к� RequestContextHolder
 ����ֵ��
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
request));

        // �޲ε���Ӧ�����Ѱ󶨵� correlation ID�����������µ�
        String result = TracerUtils.getCorrelationId();
        assertEquals(boundId, result,
                "P2-2���޲� getCorrelationId() �����������Ĵ���ʱӦ���� TraceFilter
 �󶨵� ID��"
                        + "����ÿ�������� ID������Ӧͷ/��־��һ�£�");
    }

    @Test
    void getCorrelationId_noArg_withRequestContext_stableAcrossCalls() {
        // P2-2��ͬһ�̶߳�ε����޲� getCorrelationId() Ӧ������ֵͬ���ȶ��ԣ�
        MockHttpServletRequest request = new MockHttpServletRequest();
        String boundId = "11223344556677881122334455667788";
        request.setAttribute(TracerUtils.ATTR_CORRELATION_ID, boundId);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
request));

        String first = TracerUtils.getCorrelationId();
        String second = TracerUtils.getCorrelationId();
        String third = TracerUtils.getCorrelationId();
        assertEquals(first, second, "��������Ӧ������ͬ ID");
        assertEquals(second, third, "��������Ӧ������ͬ ID");
        assertEquals(boundId, first, "Ӧ���ð�ֵ");
    }

    @Test
    void getCorrelationId_noArg_withoutRequestContext_generatesOneTimeId() {
        // P2-2������������֮�⣨���̨�����̣߳����޲ε�����������Ч��һ���� ID
        RequestContextHolder.resetRequestAttributes(); // ȷ��������������

        String id = TracerUtils.getCorrelationId();
        assertNotNull(id, "������������ʱ��Ӧ���طǿ� ID");
        assertFalse(id.isEmpty());
        assertEquals(32, id.length(), "һ���� ID ӦΪ 32 �ַ� hex");
        assertTrue(id.matches("[0-9a-f]{32}"), "һ���� ID ӦΪ�Ϸ� hex ��ʽ");
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
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "git 
show HEAD:services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-monito
r/src/main/java/cn/zszj/framework/tracer/core/filter/TraceFilter.java; git show
 HEAD:services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-web/src/te
st/java/cn/zszj/framework/web/config/CorsConfigTest.java; rg -n 'allowed-header
s|allowedHeaders' --glob '"'!*.raw.md'"' --glob '"'!*.lock'"' --glob '"'!pnpm-l
ock.yaml'"'; rg -n 'TracerUtils::getCorrelationId|TracerUtils.getCorrelationId\
\(\\)' --glob '*.java' --glob '"'!**/src/test/**'"'; git diff --check 30e77350"
'^ 30e77350; rg --files -g '"'*mvn*' -g '*gradle*' -g '*AGENTS*' -g '*CLAUDE*' 
-g '*GEMINI*' -g 'config.toml' -g '*instructions*' -g '"'!third_party/**'"' -g 
'"'!reference/**'"'" in E:\zszj-wt-sec-006
 exited 1 in 22ms:
codex
The changes correctly allow trace-id in CORS preflights and reuse request-bound
 correlation IDs while preserving OpenTelemetry precedence. No actionable regre
ssions were identified. Tests were not executed in the read-only environment.
The changes correctly allow trace-id in CORS preflights and reuse request-bound correlation IDs while preserving OpenTelemetry precedence. No actionable regressions were identified. Tests were not executed in the read-only environment.
