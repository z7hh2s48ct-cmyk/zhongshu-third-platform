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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ZS-SEC-009：分页参数校验静态契约扫描 —— 「客户端不能用不分页参数（pageSize=-1）绕过普通分页」防线。
 *
 * <p>不依赖 Spring 上下文（仿 {@link ValidationContractTest} / {@link ApiInventoryTest} 的静态源码扫描约定），
 * 以启用模块（{@link ModuleWhitelist#ENABLED_MODULES}：system/infra）源码为事实来源：
 * <ol>
 *     <li>传递闭包解析全部 {@code PageParam} 派生类型（seed = {@code PageParam}/{@code SortablePageParam}，
 *     凡 {@code class X extends <闭包内类型>} 均纳入，故不依赖 {@code *PageReqVO} 命名约定）；</li>
 *     <li>扫描 Controller 方法签名，凡参数类型 ∈ 派生集，强制其携带参数级 {@code @Valid}/{@code @Validated}，
 *     使 {@code PageParam} 上的 {@code @Min(1)}/{@code @Max(200)} 在入口真正生效、拒绝客户端传入的 {@code -1}。</li>
 * </ol>
 *
 * <p>契约动因：{@code PageParam.PAGE_SIZE_NONE=-1} 是<b>内部</b>导出信号（export 控制器在 @Valid 之后自行
 * {@code setPageSize(-1)}），{@code BaseMapperX.selectPage}/{@code PageUtils} 见 -1 即查全量。若某分页端点漏
 * 参数级 {@code @Valid}，客户端可直接传 {@code pageSize=-1} 绕过 {@code @Max(200)} 触发全量查询（DoS/数据量风险）。
 * 类级 {@code @Validated} 不触发 model-attribute VO 的级联校验，故必须参数级 {@code @Valid}。
 *
 * <p>例外以带理由的目录 {@link #PAGINATION_EXCEPTIONS} 双向强约束（仿 {@link ValidationContractTest}）：目录登记了
 * 已不存在的例外（腐化）或出现未登记的缺口，测试均失败。首选修复是补 {@code @Valid}，仅结构性不可行者才登记例外。
 *
 * <p>边界：本扫描只保证「分页端点带参数级校验注解、@Min(1) 能拦客户端 -1」；export 全量查询的权限/资源限制由
 * 各端点 {@code @PreAuthorize} 与 ZS-SEC-010 限流覆盖，运行期真实拒绝语义由 ZS-SEC-012 夹具覆盖，不在此重复。
 */
class PaginationContractTest {

    /** PageParam 派生闭包的种子（框架内已知的分页基类）。 */
    private static final Set<String> PAGE_PARAM_SEEDS = new LinkedHashSet<>(List.of("PageParam", "SortablePageParam"));

    /** {@code class X extends Y} 提取。 */
    private static final Pattern EXTENDS = Pattern.compile("\\bclass\\s+(\\w+)\\s+extends\\s+(\\w+)");

    /**
     * 分页参数级校验例外目录：{@code Controller#method} -> 登记理由。默认空——首选修复是补 {@code @Valid}。
     * 新增/移除必须同步此目录并评审，否则 {@link #exceptionCatalogueNotStale()} 或
     * {@link #pageParamArgsValidatedOrCatalogued()} 失败。
     */
    private static final Map<String, String> PAGINATION_EXCEPTIONS = buildExceptions();

    // ========== 测试 ==========

    @Test
    void pageParamArgsValidatedOrCatalogued() throws IOException {
        Set<String> derived = scanPageParamDerivedTypes();
        assertDerivedSetSane(derived);
        Map<String, String> unvalidated = scanUnvalidatedPageArgs(derived);
        List<String> uncatalogued = new ArrayList<>();
        for (Map.Entry<String, String> e : unvalidated.entrySet()) {
            if (!PAGINATION_EXCEPTIONS.containsKey(e.getKey())) {
                uncatalogued.add(e.getKey() + " 分页参数类型=" + e.getValue());
            }
        }
        assertTrue(uncatalogued.isEmpty(),
                () -> "发现分页端点的 PageParam 派生参数缺参数级 @Valid/@Validated 且未登记例外——客户端可传 pageSize=-1 "
                        + "绕过 @Max(200) 触发全量查询。请补 @Valid（首选）或评审后在 PAGINATION_EXCEPTIONS 登记理由：\n  "
                        + String.join("\n  ", uncatalogued));
    }

    @Test
    void exceptionCatalogueNotStale() throws IOException {
        Set<String> derived = scanPageParamDerivedTypes();
        Map<String, String> unvalidated = scanUnvalidatedPageArgs(derived);
        for (String key : PAGINATION_EXCEPTIONS.keySet()) {
            assertTrue(unvalidated.containsKey(key),
                    "例外目录登记了已不存在（已补 @Valid、已删除或已改名）的条目，请同步移除并评审: " + key);
        }
    }

    @Test
    void regressionKnownPageEndpointValidated() throws IOException {
        Set<String> derived = scanPageParamDerivedTypes();
        Map<String, String> unvalidated = scanUnvalidatedPageArgs(derived);
        // 回归锚点：用户分页端点必须带参数级 @Valid（UserController#getUserPage(@Valid UserPageReqVO)）
        assertFalse(unvalidated.containsKey("UserController#getUserPage"),
                "回归：UserController#getUserPage 必须携带 @Valid，否则客户端 pageSize=-1 可绕过分页");
    }

    // ========== 扫描：PageParam 派生闭包 ==========

    /** 传递闭包：seed 出发，凡 {@code class X extends <闭包内类型>} 均纳入，直到不动点（覆盖 VO extends VO 链）。 */
    private Set<String> scanPageParamDerivedTypes() throws IOException {
        Set<String> derived = new LinkedHashSet<>(PAGE_PARAM_SEEDS);
        List<String[]> extendsPairs = new ArrayList<>(); // [子类, 父类]
        for (String module : ModuleWhitelist.ENABLED_MODULES) {
            Path srcRoot = Paths.get("..", "zszj-module-" + module, "src", "main", "java");
            assertTrue(Files.isDirectory(srcRoot), "启用模块源码根目录不存在: " + srcRoot.toAbsolutePath());
            try (Stream<Path> paths = Files.walk(srcRoot)) {
                for (Path java : paths.filter(p -> p.toString().endsWith(".java")).sorted().collect(Collectors.toList())) {
                    collectExtends(java, extendsPairs);
                }
            }
        }
        boolean changed = true;
        while (changed) {
            changed = false;
            for (String[] pair : extendsPairs) {
                if (derived.contains(pair[1]) && derived.add(pair[0])) {
                    changed = true;
                }
            }
        }
        System.out.println("[PaginationContract] PageParam 派生类型(" + derived.size() + ")=" + derived);
        return derived;
    }

    private void collectExtends(Path java, List<String[]> out) throws IOException {
        Matcher m = EXTENDS.matcher(new String(Files.readAllBytes(java), StandardCharsets.UTF_8));
        while (m.find()) {
            out.add(new String[]{m.group(1), m.group(2)});
        }
    }

    /** 派生集健全性：扫描若失效得到空集会令主断言假通过，故锚定必含已知分页 VO。 */
    private void assertDerivedSetSane(Set<String> derived) {
        assertTrue(derived.contains("UserPageReqVO"),
                "派生集未含 UserPageReqVO——扫描逻辑或模块路径可能已失效，主断言不可信");
        assertTrue(derived.size() >= PAGE_PARAM_SEEDS.size() + 10,
                "派生集异常偏小(" + derived.size() + ")，扫描可能失效");
    }

    // ========== 扫描：Controller 分页参数校验 ==========

    /** 返回「Controller#method -> 未校验的分页参数类型」（TreeMap 保证确定性顺序）。 */
    private Map<String, String> scanUnvalidatedPageArgs(Set<String> derived) throws IOException {
        Map<String, String> result = new TreeMap<>();
        for (String module : ModuleWhitelist.ENABLED_MODULES) {
            Path srcRoot = Paths.get("..", "zszj-module-" + module, "src", "main", "java");
            try (Stream<Path> paths = Files.walk(srcRoot)) {
                List<Path> controllers = paths
                        .filter(p -> p.getFileName().toString().endsWith("Controller.java"))
                        .filter(p -> p.toString().contains("controller"))
                        .sorted()
                        .collect(Collectors.toList());
                for (Path controller : controllers) {
                    parseController(controller, derived, result);
                }
            }
        }
        System.out.println("[PaginationContract] 未带参数级 @Valid 的分页参数命中(应全部为已登记例外)=" + result);
        return result;
    }

    private void parseController(Path file, Set<String> derived, Map<String, String> out) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        String controller = file.getFileName().toString().replace(".java", "");
        for (int i = 0; i < lines.size(); i++) {
            String t = lines.get(i).trim();
            if (!isMethodStart(t)) {
                continue;
            }
            StringBuilder sig = new StringBuilder(t);
            int balance = parenDelta(t);
            int j = i;
            while (balance > 0 && j + 1 < lines.size()) {
                j++;
                String nt = lines.get(j).trim();
                sig.append(' ').append(nt);
                balance += parenDelta(nt);
            }
            i = j;
            inspectSignature(controller, sig.toString(), derived, out);
        }
    }

    private void inspectSignature(String controller, String sig, Set<String> derived, Map<String, String> out) {
        int open = sig.indexOf('(');
        if (open < 0) {
            return;
        }
        int close = matchingParen(sig, open);
        if (close < 0) {
            return;
        }
        String methodName = methodNameBefore(sig, open);
        for (String param : splitTopLevel(sig.substring(open + 1, close))) {
            String type = simpleTypeName(param);
            if (type == null || !derived.contains(type)) {
                continue; // 非 PageParam 派生参数
            }
            // "@Valid" 是 "@Validated" 的子串，contains("@Valid") 同时覆盖两者
            if (param.contains("@Valid")) {
                continue;
            }
            out.put(controller + "#" + methodName, type);
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

    /** 提取参数的简单类型名（剥离注解、泛型实参、包名）；不足「类型 + 名」两段时返回 null。 */
    private static String simpleTypeName(String param) {
        String p = param.replaceAll("@\\w+(\\([^)]*\\))?", "").trim();
        String[] toks = p.split("\\s+");
        if (toks.length < 2 || toks[0].isEmpty()) {
            return null;
        }
        String type = toks[0];
        int lt = type.indexOf('<');
        if (lt >= 0) {
            type = type.substring(0, lt);
        }
        int dot = type.lastIndexOf('.');
        if (dot >= 0) {
            type = type.substring(dot + 1);
        }
        return type.isEmpty() ? null : type;
    }

    // ========== 例外目录（理由） ==========

    private static Map<String, String> buildExceptions() {
        Map<String, String> m = new LinkedHashMap<>();
        // 默认空：分页端点漏参数级 @Valid 属可修复缺口，首选补 @Valid 而非登记例外。
        return Collections.unmodifiableMap(m);
    }

}
