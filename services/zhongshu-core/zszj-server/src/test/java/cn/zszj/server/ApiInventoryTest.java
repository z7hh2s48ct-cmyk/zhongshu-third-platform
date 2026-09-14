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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ZS-SEC-002：接口分类、匿名白名单与方法权限清单 —— 静态清单生成与漂移基线。
 *
 * <p>不依赖 Spring 上下文（仿 {@link ModuleWhitelistTest} 的静态源码扫描约定），以启用模块
 * （{@link ModuleWhitelist#ENABLED_MODULES}：system/infra）的 Controller 源码为事实来源，静态解析出
 * 全部 REST 端点清单，每行编码：
 * <pre>HTTP方法 | 完整路径（含 /admin-api、/app-api 前缀）| 主体类型 | 授权类别 | 权限标识 | 模块 | Controller</pre>
 *
 * <p>主体类型由包路径判定（与 {@code ZszjWebAutoConfiguration} 的 prefix 规则一致）：
 * {@code **.controller.admin.**} → ADMIN + {@code /admin-api}；{@code **.controller.app.**} → MEMBER + {@code /app-api}。
 *
 * <p>授权类别：
 * <ul>
 *   <li>{@code ANONYMOUS} —— 标注 {@code @PermitAll}（方法级或类级），免登录；</li>
 *   <li>{@code PERMISSION} —— 标注 {@code @PreAuthorize("@ss.hasPermission('xxx')")}，需登录 + 方法权限；</li>
 *   <li>{@code AUTHENTICATED} —— 两者皆无，需登录但无特定方法权限（合法的已登录公共能力，逐项编目复核）。</li>
 * </ul>
 *
 * <p>清单与基线 {@code src/test/resources/api-inventory-baseline.txt} 逐行比对：任何路由/注解新增、删除或
 * 权限变更都会使清单漂移、测试失败并要求评审后更新基线（对齐验收「路由或注解新增导致清单变化时测试失败并
 * 要求评审」）。匿名白名单另以带理由的目录 {@link #ANONYMOUS_CATALOGUE} 强约束：新增/移除匿名端点必须登记
 * 理由并评审（对齐验收「匿名仅能访问获准能力」）。
 *
 * <p>重新生成基线（评审通过后）：
 * <pre>mvn -pl :zszj-server -Dtest=ApiInventoryTest -Dapi.inventory.update=true test</pre>
 *
 * <p>2026-09-15 P2 硬化（codex-ZS-SEC-002 评审处置 #1/#4）：
 * <ul>
 *   <li>#1 基线缺失门禁硬化：基线文件不存在时，仅 {@code -Dapi.inventory.update=true} 允许创建；
 *       否则直接失败——防止从 checkout 省略基线时静默写入当前清单再与自身比对通过。</li>
 *   <li>#4 类级 @PreAuthorize 生效语义：类级 for 循环识别 {@code @PreAuthorize} 并提取权限标识；
 *       方法级缺省 {@code @PreAuthorize} 时回落到类级权限（方法级优先）。</li>
 * </ul>
 *
 * <p>边界：本清单为静态源码事实来源，覆盖启用模块的 API 分类/匿名/方法权限；ADMIN/MEMBER Token 串用、
 * ASYNC 派发不免认证、缺方法权限拒绝等运行时合同由 ZS-SEC-012.A 的真实安全链夹具覆盖，不在此重复。
 * zszj-server 的 DefaultController（未启用模块兜底路由）由 ZS-ENG-001 的 ModuleWhitelistTest 治理，不纳入本清单。
 */
class ApiInventoryTest {

    private static final String ADMIN_MARKER = ".controller.admin.";
    private static final String APP_MARKER = ".controller.app.";
    private static final String ADMIN_PREFIX = "/admin-api";
    private static final String APP_PREFIX = "/app-api";

    private static final Pattern PACKAGE = Pattern.compile("^package\\s+([\\w.]+)\\s*;");
    private static final Pattern CLASS_DECL = Pattern.compile("\\b(?:class|interface|enum)\\s+\\w+");
    private static final Pattern QUOTED = Pattern.compile("\"([^\"]*)\"");
    private static final Pattern SINGLE_QUOTED = Pattern.compile("'([^']+)'");
    private static final Pattern REQUEST_METHOD_ATTR = Pattern.compile("RequestMethod\\.(\\w+)");

    /** 基线文件（相对 zszj-server 模块目录，surefire 工作目录）。 */
    private static final Path BASELINE = Paths.get("src", "test", "resources", "api-inventory-baseline.txt");

    /**
     * 匿名白名单目录：完整「METHOD path」→ 登记理由。新增/移除匿名端点必须同步此目录与
     * {@code services/zhongshu-core/docs/接口清单与匿名白名单.md}，否则 {@link #anonymousWhitelistMatchesCatalogue()} 失败。
     */
    private static final Map<String, String> ANONYMOUS_CATALOGUE = buildAnonymousCatalogue();

    // ========== 测试 ==========

    @Test
    void inventoryMatchesBaseline() throws IOException {
        List<String> actual = generateInventory();
        assertFalse(actual.isEmpty(), "接口清单不应为空，请检查启用模块 Controller 扫描路径");

        // P2 硬化 #1：基线缺失时仅 update=true 允许创建；否则直接失败（防止静默自比通过）
        boolean updateMode = Boolean.getBoolean("api.inventory.update");
        if (!Files.exists(BASELINE)) {
            assertTrue(updateMode,
                    "基线文件缺失：" + BASELINE.toAbsolutePath()
                            + "\n  首次创建或基线被删除时，必须显式带 -Dapi.inventory.update=true 评审后再生成，"
                            + "防止从 checkout 省略基线时静默写入当前清单再与自身比对通过（codex-ZS-SEC-002 P2 #1 硬化）");
            Files.createDirectories(BASELINE.getParent());
            Files.write(BASELINE, renderBaseline(actual), StandardCharsets.UTF_8);
            System.out.println("[ApiInventory] 基线已重新生成：" + actual.size() + " 个端点 -> " + BASELINE.toAbsolutePath());
        } else if (updateMode) {
            Files.write(BASELINE, renderBaseline(actual), StandardCharsets.UTF_8);
            System.out.println("[ApiInventory] 基线已更新：" + actual.size() + " 个端点 -> " + BASELINE.toAbsolutePath());
        }

        List<String> expected = readBaseline();
        // 逐行比对：漂移时给出新增/删除差异，要求评审后更新基线
        List<String> added = actual.stream().filter(l -> !expected.contains(l)).collect(Collectors.toList());
        List<String> removed = expected.stream().filter(l -> !actual.contains(l)).collect(Collectors.toList());
        assertTrue(added.isEmpty() && removed.isEmpty(),
                () -> "接口清单发生漂移，须经评审并更新基线 api-inventory-baseline.txt。\n"
                        + "  新增端点(" + added.size() + "): " + added + "\n"
                        + "  移除端点(" + removed.size() + "): " + removed);
        assertEquals(expected, actual, "接口清单顺序或内容与基线不一致");
    }

    @Test
    void anonymousWhitelistMatchesCatalogue() throws IOException {
        Map<String, String> actualAnonymous = new LinkedHashMap<>();
        for (String line : generateInventory()) {
            String[] c = line.split("\\|");
            if ("ANONYMOUS".equals(c[3])) {
                actualAnonymous.put(c[0] + " " + c[1], c[6]);
            }
        }
        // 目录里的每个匿名端点都必须真实存在（防止目录腐化）
        for (String key : ANONYMOUS_CATALOGUE.keySet()) {
            assertTrue(actualAnonymous.containsKey(key),
                    "匿名目录登记了不存在的端点（已被移除或改路径），请同步目录与文档理由: " + key);
        }
        // 每个真实匿名端点都必须在目录里登记理由（新增匿名端点必须评审）
        for (String key : actualAnonymous.keySet()) {
            assertTrue(ANONYMOUS_CATALOGUE.containsKey(key),
                    "发现未登记的匿名(@PermitAll)端点，收紧匿名白名单要求先评审理由再登记: " + key
                            + "（来源 Controller: " + actualAnonymous.get(key) + "）");
        }
        assertEquals(ANONYMOUS_CATALOGUE.size(), actualAnonymous.size(), "匿名端点数量与目录不一致");
    }

    @Test
    void everyEndpointHasKnownSubjectAndApiPrefix() throws IOException {
        for (String line : generateInventory()) {
            String[] c = line.split("\\|");
            String path = c[1];
            String subject = c[2];
            assertTrue("ADMIN".equals(subject) || "MEMBER".equals(subject),
                    "端点主体类型必须为 ADMIN/MEMBER（由 controller.admin/app 包判定），实际=" + subject + " 行=" + line);
            assertTrue(path.startsWith(ADMIN_PREFIX + "/") || path.startsWith(APP_PREFIX + "/"),
                    "端点路径必须带 /admin-api 或 /app-api 前缀，实际=" + path);
            // 主体类型与前缀必须一致，防止 ADMIN 走 app-api 或 MEMBER 走 admin-api
            assertTrue(("ADMIN".equals(subject) && path.startsWith(ADMIN_PREFIX))
                            || ("MEMBER".equals(subject) && path.startsWith(APP_PREFIX)),
                    "主体类型与 API 前缀不一致: " + line);
        }
    }

    @Test
    void permissionEndpointsHaveWellFormedPermission() throws IOException {
        for (String line : generateInventory()) {
            String[] c = line.split("\\|");
            if ("PERMISSION".equals(c[3])) {
                String perm = c[4];
                assertFalse("-".equals(perm) || perm.isEmpty(),
                        "PERMISSION 类别端点必须有非空权限标识: " + line);
                assertTrue(perm.chars().allMatch(ch -> ch == ':' || ch == ',' || ch == '*' || ch == '-' || ch == '_' || ch == '.' || Character.isLetterOrDigit(ch)),
                        "权限标识字符集异常（RBAC 为 module:resource:action，OAuth2 开放端点为 scope 如 user.read，多值以逗号分隔）: " + perm + " 行=" + line);
            } else {
                assertEquals("-", c[4], "非 PERMISSION 类别端点权限标识应为占位 '-': " + line);
            }
        }
    }

    // ========== 清单生成 ==========

    private List<String> generateInventory() throws IOException {
        List<String> out = new ArrayList<>();
        for (String module : ModuleWhitelist.ENABLED_MODULES) {
            Path srcRoot = Paths.get("..", "zszj-module-" + module, "src", "main", "java");
            assertTrue(Files.isDirectory(srcRoot), "启用模块源码根目录不存在: " + srcRoot.toAbsolutePath());
            try (Stream<Path> paths = Files.walk(srcRoot)) {
                List<Path> controllers = paths
                        .filter(p -> p.getFileName().toString().endsWith("Controller.java"))
                        .filter(p -> p.toString().contains("controller"))
                        .sorted()
                        .collect(Collectors.toList());
                for (Path controller : controllers) {
                    parseController(controller, module, out);
                }
            }
        }
        Collections.sort(out);
        printSummary(out);
        return out;
    }

    private void parseController(Path file, String module, List<String> out) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        String controller = file.getFileName().toString().replace(".java", "");

        // 1. 包名 → 主体类型与前缀
        String pkg = "";
        for (String l : lines) {
            Matcher m = PACKAGE.matcher(l.trim());
            if (m.matches()) {
                pkg = m.group(1);
                break;
            }
        }
        boolean admin = pkg.contains(ADMIN_MARKER);
        boolean app = pkg.contains(APP_MARKER);
        if (!admin && !app) {
            // 非标准 API Controller（不在 admin/app 包下）：不纳入清单，但显式暴露以便评审
            System.out.println("[ApiInventory] 跳过非 admin/app 包 Controller: " + pkg + "." + controller);
            return;
        }
        String subject = admin ? "ADMIN" : "MEMBER";
        String prefix = admin ? ADMIN_PREFIX : APP_PREFIX;

        // 2. 类声明位置：之前为类级注解，之后为方法级
        int classIdx = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (CLASS_DECL.matcher(lines.get(i)).find()) {
                classIdx = i;
                break;
            }
        }
        assertTrue(classIdx > 0, "未找到类声明: " + controller);

        // 3. 类级基路径、类级 @PermitAll、类级 @PreAuthorize（P2 硬化 #4）
        String basePath = "";
        boolean classPermitAll = false;
        String classPermission = null;
        for (int i = 0; i < classIdx; i++) {
            String t = lines.get(i).trim();
            if (t.startsWith("@RequestMapping")) {
                List<String> ps = extractPaths(t);
                basePath = ps.isEmpty() ? "" : ps.get(0);
            } else if (t.startsWith("@PermitAll")) {
                classPermitAll = true;
            } else if (t.startsWith("@PreAuthorize")) {
                classPermission = extractPermission(t);
            }
        }

        // 4. 方法级：跟踪待处理注解，遇方法签名则产出一个端点并复位
        String pendingMethod = null;
        List<String> pendingPaths = null;
        boolean pendingPermitAll = false;
        String pendingPermission = null;
        boolean hasMethodPreAuthorize = false;  // P2 硬化 #4 r1：跟踪方法级 @PreAuthorize 注解是否存在（与提取的权限标识分开）
        for (int i = classIdx + 1; i < lines.size(); i++) {
            String t = lines.get(i).trim();
            if (t.startsWith("@GetMapping")) {
                pendingMethod = "GET";
                pendingPaths = extractPaths(t);
            } else if (t.startsWith("@PostMapping")) {
                pendingMethod = "POST";
                pendingPaths = extractPaths(t);
            } else if (t.startsWith("@PutMapping")) {
                pendingMethod = "PUT";
                pendingPaths = extractPaths(t);
            } else if (t.startsWith("@DeleteMapping")) {
                pendingMethod = "DELETE";
                pendingPaths = extractPaths(t);
            } else if (t.startsWith("@PatchMapping")) {
                pendingMethod = "PATCH";
                pendingPaths = extractPaths(t);
            } else if (t.startsWith("@RequestMapping")) {
                pendingMethod = extractRequestMethod(t);
                pendingPaths = extractPaths(t);
            } else if (t.startsWith("@PermitAll")) {
                pendingPermitAll = true;
            } else if (t.startsWith("@PreAuthorize")) {
                pendingPermission = extractPermission(t);
                hasMethodPreAuthorize = true;  // P2 硬化 #4 r1：方法级有 @PreAuthorize 注解（即使无权限字面量如 isAuthenticated()）
            } else if (isMethodSignature(t)) {
                if (pendingMethod != null) {
                    // P2 硬化 #4 r1：方法级有 @PreAuthorize 注解时使用方法级权限（即使为 null，如 isAuthenticated()）；
                    // 仅当方法级无 @PreAuthorize 注解时才回落到类级权限（防止 isAuthenticated() 错误保留类级权限隐藏授权弱化）
                    String effectivePermission = hasMethodPreAuthorize ? pendingPermission : classPermission;
                    emit(out, pendingMethod, pendingPaths, pendingPermitAll || classPermitAll,
                            effectivePermission, prefix, basePath, subject, module, controller);
                }
                pendingMethod = null;
                pendingPaths = null;
                pendingPermitAll = false;
                pendingPermission = null;
                hasMethodPreAuthorize = false;  // P2 硬化 #4 r1：复位
            }
        }
    }

    private void emit(List<String> out, String httpMethod, List<String> paths, boolean anonymous,
                      String permission, String prefix, String basePath, String subject,
                      String module, String controller) {
        List<String> usePaths = (paths == null || paths.isEmpty()) ? Collections.singletonList("") : paths;
        String authCat = anonymous ? "ANONYMOUS" : (permission != null ? "PERMISSION" : "AUTHENTICATED");
        String perm = permission != null ? permission : "-";
        for (String p : usePaths) {
            String full = normalizePath(prefix, basePath, p);
            out.add(httpMethod + "|" + full + "|" + subject + "|" + authCat + "|" + perm + "|" + module + "|" + controller);
        }
    }

    // ========== 解析辅助 ==========

    private static boolean isMethodSignature(String t) {
        if (t.startsWith("@") || t.startsWith("//") || t.startsWith("*") || t.startsWith("/*")) {
            return false;
        }
        return (t.startsWith("public ") || t.startsWith("protected ") || t.startsWith("private ")) && t.contains("(");
    }

    /** 提取映射注解中的路径：所有双引号字符串（本仓映射注解的 {@code value=} 均为路径，无 produces/consumes 干扰）。 */
    private static List<String> extractPaths(String ann) {
        int open = ann.indexOf('(');
        if (open < 0) {
            return Collections.singletonList("");
        }
        int close = ann.lastIndexOf(')');
        if (close <= open) {
            return Collections.singletonList("");
        }
        String content = ann.substring(open + 1, close).trim();
        if (content.isEmpty()) {
            return Collections.singletonList("");
        }
        List<String> paths = new ArrayList<>();
        Matcher m = QUOTED.matcher(content);
        while (m.find()) {
            paths.add(m.group(1));
        }
        return paths.isEmpty() ? Collections.singletonList("") : paths;
    }

    /** 方法级 @RequestMapping 的 method 属性；缺省时视为全方法（REQUEST）。本仓当前无方法级 @RequestMapping。 */
    private static String extractRequestMethod(String ann) {
        Matcher m = REQUEST_METHOD_ATTR.matcher(ann);
        return m.find() ? m.group(1) : "REQUEST";
    }

    /** 提取 @PreAuthorize 中的权限标识：所有单引号字符串以逗号连接（hasPermission/hasAnyPermissions 通用）。 */
    private static String extractPermission(String ann) {
        List<String> perms = new ArrayList<>();
        Matcher m = SINGLE_QUOTED.matcher(ann);
        while (m.find()) {
            perms.add(m.group(1));
        }
        return perms.isEmpty() ? null : String.join(",", perms);
    }

    /** 拼接并规范化完整路径：折叠重复斜杠、去尾斜杠，保留 {var} 与 ** 通配。 */
    private static String normalizePath(String... segments) {
        List<String> tokens = new ArrayList<>();
        for (String seg : segments) {
            if (seg == null) {
                continue;
            }
            for (String tok : seg.split("/")) {
                if (!tok.isEmpty()) {
                    tokens.add(tok);
                }
            }
        }
        return "/" + String.join("/", tokens);
    }

    // ========== 基线渲染与读取 ==========

    private List<String> renderBaseline(List<String> inventory) {
        List<String> out = new ArrayList<>();
        out.add("# ZS-SEC-002 接口清单基线（自动生成，勿手改；变更须经评审后以 -Dapi.inventory.update=true 重新生成）");
        out.add("# 列：HTTP方法|完整路径|主体类型|授权类别(ANONYMOUS/PERMISSION/AUTHENTICATED)|权限标识|模块|Controller");
        out.add("# 端点总数：" + inventory.size());
        out.addAll(inventory);
        return out;
    }

    private List<String> readBaseline() throws IOException {
        if (!Files.exists(BASELINE)) {
            return Collections.emptyList();
        }
        return Files.readAllLines(BASELINE, StandardCharsets.UTF_8).stream()
                .filter(l -> !l.isBlank() && !l.startsWith("#"))
                .collect(Collectors.toList());
    }

    private void printSummary(List<String> inventory) {
        Map<String, Long> byCat = inventory.stream()
                .collect(Collectors.groupingBy(l -> l.split("\\|")[3], TreeMap::new, Collectors.counting()));
        Map<String, Long> byModule = inventory.stream()
                .collect(Collectors.groupingBy(l -> l.split("\\|")[5], TreeMap::new, Collectors.counting()));
        Map<String, Long> byMethod = inventory.stream()
                .collect(Collectors.groupingBy(l -> l.split("\\|")[0], TreeMap::new, Collectors.counting()));
        System.out.println("[ApiInventory] 端点总数=" + inventory.size()
                + " 按授权类别=" + byCat + " 按模块=" + byModule + " 按方法=" + byMethod);
    }

    // ========== 匿名白名单目录（理由） ==========

    private static Map<String, String> buildAnonymousCatalogue() {
        Map<String, String> m = new LinkedHashMap<>();
        // 认证入口：登录/登出/刷新/注册/短信登录/发码/改密/社交登录，认证前必须免登录
        m.put("POST /admin-api/system/auth/login", "账号密码登录入口，认证前免登录");
        m.put("POST /admin-api/system/auth/logout", "登出入口，允许无有效令牌调用以清理客户端状态");
        m.put("POST /admin-api/system/auth/refresh-token", "刷新令牌入口，凭 refresh_token 换取新 access_token");
        m.put("POST /admin-api/system/auth/register", "自助注册入口（是否启用由 ZS-LOGIN-004 收口）");
        m.put("POST /admin-api/system/auth/sms-login", "短信验证码登录入口，认证前免登录");
        m.put("POST /admin-api/system/auth/send-sms-code", "发送登录/注册短信验证码，认证前免登录");
        m.put("POST /admin-api/system/auth/reset-password", "忘记密码自助重置，认证前免登录");
        m.put("GET /admin-api/system/auth/social-auth-redirect", "第三方社交登录授权回调跳转，认证前免登录");
        m.put("POST /admin-api/system/auth/social-login", "第三方社交登录入口，认证前免登录");
        // 验证码：登录前人机校验，必须免登录
        m.put("POST /admin-api/system/captcha/get", "获取图形/行为验证码，登录前免登录");
        m.put("POST /admin-api/system/captcha/check", "校验验证码，登录前免登录");
        // OAuth2 开放端点：以 client_id/client_secret（Basic）自校验，非用户令牌
        m.put("POST /admin-api/system/oauth2/token", "OAuth2 令牌端点，凭 client 凭据Basic认证，免用户登录");
        m.put("DELETE /admin-api/system/oauth2/token", "OAuth2 令牌撤销端点，凭 client 凭据Basic认证");
        m.put("POST /admin-api/system/oauth2/check-token", "OAuth2 令牌校验端点，凭 client 凭据Basic认证");
        // 短信服务商回调：外部平台异步回调，无用户令牌，须验签（真实性归领域校验）
        m.put("POST /admin-api/system/sms/callback/aliyun", "阿里云短信发送状态回调，外部平台调用免登录");
        m.put("POST /admin-api/system/sms/callback/tencent", "腾讯云短信发送状态回调，外部平台调用免登录");
        m.put("POST /admin-api/system/sms/callback/huawei", "华为云短信发送状态回调，外部平台调用免登录");
        m.put("POST /admin-api/system/sms/callback/qiniu", "七牛云短信发送状态回调，外部平台调用免登录");
        // 租户查询：登录页按名称/域名定位租户，认证前免登录
        m.put("GET /admin-api/system/tenant/get-id-by-name", "登录页按租户名查租户ID，认证前免登录");
        m.put("GET /admin-api/system/tenant/simple-list", "租户精简列表（登录页选择），认证前免登录");
        m.put("GET /admin-api/system/tenant/get-by-website", "按访问域名定位租户，认证前免登录");
        // App(MEMBER)侧公共数据：字典/地区/租户，小程序启动即需读取
        m.put("GET /app-api/system/dict-data/type", "App 侧按类型读取字典数据，公共基础数据免登录");
        m.put("GET /app-api/system/area/tree", "App 侧地区树，公共基础数据免登录");
        m.put("GET /app-api/system/tenant/get-by-website", "App 侧按域名定位租户，认证前免登录");
        // 文件读取：公开素材下载（私有文件主体绑定授权归 ZS-FILE-001.A）
        m.put("GET /admin-api/infra/file/{configId}/get/**", "文件读取端点，公开素材下载；私有文件主体绑定与授权归 ZS-FILE-001.A");
        return Collections.unmodifiableMap(m);
    }

}
