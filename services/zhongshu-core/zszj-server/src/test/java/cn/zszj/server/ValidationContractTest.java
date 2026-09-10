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
 * <p>不依赖 Spring 上下文（仿 {@link ApiInventoryTest} / {@link ModuleWhitelistTest} 的静态源码扫描约定），
 * 以启用模块（{@link ModuleWhitelist#ENABLED_MODULES}：system/infra）的 Controller 源码为事实来源，
 * 静态解析每个方法签名中的 {@code @RequestBody} 参数，强制其携带参数级 {@code @Valid} 或 {@code @Validated}，
 * 使 VO/DTO 上已声明的 Bean Validation 约束（{@code @NotNull}/{@code @NotEmpty}/{@code @InEnum} 等）在入口真正生效。
 *
 * <p>契约动因：类级 {@code @Validated} 只对 {@code @RequestParam}/{@code @PathVariable} 等直接约束生效，
 * <b>不触发</b> {@code @RequestBody} 的级联 Bean 校验——后者必须参数级 {@code @Valid}。历史上
 * {@code SocialUserController#socialUnbind} 与 {@code SocialClientController#sendSubscribeMessage} 的 VO/DTO
 * 已声明约束却漏 {@code @Valid}，约束从不生效（本轮已补，见 {@link #regressionFixedGapsNowValidated()}）。
 *
 * <p>少数结构性例外以带理由的目录 {@link #VALIDATION_EXCEPTIONS} 强约束（仿 {@link ApiInventoryTest} 的
 * {@code ANONYMOUS_CATALOGUE}）：仅「第三方 SDK 的 VO」与「{@code @RequestBody String} 原文回调」两类无法/不应
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
    private static final Map<String, String> VALIDATION_EXCEPTIONS = buildExceptions();

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
                () -> "发现 @RequestBody 参数缺参数级 @Valid/@Validated 且未登记例外，其 VO/DTO 已声明的约束将不生效。"
                        + "请补 @Valid（首选，与同类入口一致）或评审后在 VALIDATION_EXCEPTIONS 登记理由：\n  "
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
        assertFalse(unvalidated.containsKey("SocialUserController#socialUnbind"),
                "回归：SocialUserController#socialUnbind 必须携带 @Valid"
                        + "（SocialUserUnbindReqVO 声明了 @InEnum/@NotNull/@NotEmpty）");
        assertFalse(unvalidated.containsKey("SocialClientController#sendSubscribeMessage"),
                "回归：SocialClientController#sendSubscribeMessage 必须携带 @Valid"
                        + "（SocialWxaSubscribeMessageSendReqDTO 声明了 @NotNull/@NotEmpty）");
    }

    // ========== 扫描 ==========

    /** 扫描启用模块全部 Controller，返回「Controller#method -> 未校验的 @RequestBody 参数类型」（TreeMap 保证确定性顺序）。 */
    private Map<String, String> scanUnvalidatedRequestBody() throws IOException {
        Map<String, String> result = new TreeMap<>();
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
                    parseController(controller, result);
                }
            }
        }
        System.out.println("[ValidationContract] 未带参数级 @Valid 的 @RequestBody 命中(应全部为已登记例外)=" + result);
        return result;
    }

    private void parseController(Path file, Map<String, String> out) throws IOException {
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

    private void inspectSignature(String controller, String sig, Map<String, String> out) {
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
        if (t.startsWith("@") || t.startsWith("//") || t.startsWith("*") || t.startsWith("/*")) {
            return false;
        }
        if (!(t.startsWith("public ") || t.startsWith("protected ") || t.startsWith("private "))) {
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
        // 第三方验证码 SDK VO：非本项目 ReqVO，无法/不应在本仓加约束，校验由 CaptchaService 内部负责；认证前 @PermitAll + @TenantIgnore
        m.put("CaptchaController#get",
                "com.anji.captcha 第三方 SDK 的 CaptchaVO，非本项目 ReqVO，校验由 CaptchaService 内部负责；认证前 @PermitAll 端点");
        m.put("CaptchaController#check",
                "com.anji.captcha 第三方 SDK 的 CaptchaVO，非本项目 ReqVO，校验由 CaptchaService 内部负责；认证前 @PermitAll 端点");
        // @RequestBody String 原文回调：String 非 bean 无法参数级校验，body 交 SmsSendService 内部解析验签；@PermitAll 外部平台回调
        m.put("SmsCallbackController#receiveHuaweiSmsStatus",
                "@RequestBody String 原文（华为云外部回调），String 非 bean 无法参数级校验，body 交 SmsSendService 内部解析验签");
        m.put("SmsCallbackController#receiveQiniuSmsStatus",
                "@RequestBody String 原文（七牛云外部回调），String 非 bean 无法参数级校验，body 交 SmsSendService 内部解析验签");
        return Collections.unmodifiableMap(m);
    }

}
