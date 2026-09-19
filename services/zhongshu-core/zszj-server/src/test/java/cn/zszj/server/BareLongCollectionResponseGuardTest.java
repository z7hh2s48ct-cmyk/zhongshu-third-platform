package cn.zszj.server;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ZS-SEC-009.B r1 P2-B 架构守卫：启用模块 Controller 禁止返回「裸 Long 集合」响应。
 *
 * <p>背景：{@code CommonResult<Set<Long>>} / {@code CommonResult<List<Long>>} 的顶层属性名是
 * {@code data}，且经泛型擦除后声明类型为 Object——ID→string 序列化合同
 * （{@link cn.zszj.framework.common.util.json.databind.IdToStringAnnotationIntrospector}，
 * 按字段名 id/Id/Ids 识别）无法作用于其集合元素，wire 仍输出 number，与前端已迁移的
 * string 选项 ID 比较失配（codex r0 P1 实证：已授权限被清空风险）。
 *
 * <p>合同：此类响应必须包装为显式命名字段的 VO（先例：PermissionIdListRespVO 的
 * menuIds/roleIds），使命名约定能命中集合元素。本测试静态扫描启用模块（system/infra）
 * 全部 Controller 源码，出现裸 Long 集合返回类型即失败，防止回归。
 *
 * <p>边界：仅扫描 Controller 文件；非 ID 语义的 Long 集合（计数列表等）如确需返回，
 * 同样须走显式字段名 VO（字段名不匹配 id/Id/Ids 时不受序列化合同影响，行为不变）。
 */
public class BareLongCollectionResponseGuardTest {

    /** CommonResult<Set<Long>> / CommonResult<List<Long>>（含 java.util 全限定写法） */
    private static final Pattern BARE_LONG_COLLECTION = Pattern.compile(
            "CommonResult<(?:java\\.util\\.)?(?:Set|List)<\\s*(?:java\\.lang\\.)?Long\\s*>>");

    private static final Pattern CONTROLLER_FILE = Pattern.compile(".*/controller/.*Controller\\.java");

    @Test
    public void enabledModuleControllersMustNotReturnBareLongCollections() throws IOException {
        List<String> violations = new ArrayList<>();
        List<Path> moduleRoots = List.of(
                Paths.get("..", "zszj-module-system", "src", "main", "java"),
                Paths.get("..", "zszj-module-infra", "src", "main", "java"));
        for (Path root : moduleRoots) {
            if (!Files.isDirectory(root)) {
                continue; // 模块目录不存在（白名单裁剪）不视为违规
            }
            try (Stream<Path> paths = Files.walk(root)) {
                paths.filter(p -> CONTROLLER_FILE.matcher(p.toString().replace('\\', '/')).matches())
                        .forEach(p -> scanFile(p, violations));
            }
        }
        assertTrue(violations.isEmpty(),
                "启用模块 Controller 存在裸 Long 集合响应（ID→string 合同无法作用，须包装显式字段 VO，"
                        + "循 PermissionIdListRespVO 先例）：" + String.join("\n  ", violations));
    }

    private static void scanFile(Path file, List<String> violations) {
        try {
            String source = Files.readString(file);
            java.util.regex.Matcher m = BARE_LONG_COLLECTION.matcher(source);
            while (m.find()) {
                violations.add(file + " → " + m.group());
            }
        } catch (IOException e) {
            violations.add(file + " → 读取失败: " + e.getMessage());
        }
    }
}
