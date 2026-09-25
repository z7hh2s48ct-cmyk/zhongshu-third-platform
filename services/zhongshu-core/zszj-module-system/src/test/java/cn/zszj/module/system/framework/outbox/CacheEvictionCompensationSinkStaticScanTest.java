package cn.zszj.module.system.framework.outbox;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ZS-PERM-004.C 静态扫描：{@link CacheEvictionCompensationSink} 源码<b>只做减语义驱逐、零写缓存</b>。
 *
 * <p><b>合同</b>（循 {@code OutboxEventSink}）：Sink「只做投递动作，禁止回写业务事实」——
 * 补偿重放的合法动作只有 {@code getCache}（定位）/ {@code evict}（单键驱逐）/ {@code clear}（清空）；
 * 全代码路径不得包含任何"写缓存"操作（{@code put}/{@code set}/{@code putIfAbsent}/{@code opsForX} 写入等），
 * 否则重放可能把旧值写回缓存（补偿反而制造不一致）。
 *
 * <p><b>为何静态扫描而非运行时断言</b>：运行时断言只能覆盖已执行路径，静态扫描锁定<b>所有代码路径</b>
 * （含异常分支、未来新增分支），从源码结构上杜绝"Sink 内意外写缓存"的缺口（循
 * {@code SinkNeverWritesAccessTokenCacheTest} 先例）；并含<b>注入自检</b>——合成含写操作的伪源码
 * 必须被规则命中，防规则静默失效。
 *
 * @author ZS-PERM-004.C
 */
public class CacheEvictionCompensationSinkStaticScanTest {

    private static final Path SINK_SOURCE = Paths.get(
            "src/main/java/cn/zszj/module/system/framework/outbox/CacheEvictionCompensationSink.java");

    /** 禁止模式：任何"写缓存"操作（重放写回旧值的安全缺口）。 */
    private static final List<Pattern> FORBIDDEN_WRITE_PATTERNS = List.of(
            Pattern.compile("\\bstringRedisTemplate\\."),
            Pattern.compile("\\bredisTemplate\\."),
            Pattern.compile("\\.opsForValue\\(\\)\\.set\\s*\\("),
            Pattern.compile("\\.opsForHash\\(\\)\\.put"),
            Pattern.compile("\\.putIfAbsent\\s*\\("),
            Pattern.compile("\\.put\\s*\\("),
            Pattern.compile("\\.write\\s*\\("),
            Pattern.compile("\\.increment\\s*\\(")
    );

    /** 允许且必须出现的减语义调用：定位缓存 + 驱逐 + 清空。 */
    private static final List<String> REQUIRED_CALLS = List.of(
            "getCache",
            ".evict(",
            ".clear("
    );

    // ========== 用例 30：真实 Sink 源码零写缓存 + 注入自检 ==========

    /**
     * RED：骨架阶段 {@code deliver} 为空实现 → 必备减语义调用（getCache/evict/clear）缺失 → 失败；
     * GREEN 落地后转绿。若未来 PR 引入写缓存操作，本测试立即失败阻断合并。
     */
    @Test
    public void sinkSource_onlyReduceSemantics_noCacheWrites() throws Exception {
        // 段 1：真实 Sink 源码必须存在且零写缓存
        assertTrue(Files.exists(SINK_SOURCE), "Sink 源文件必须存在: " + SINK_SOURCE.toAbsolutePath());
        String source = Files.readString(SINK_SOURCE, StandardCharsets.UTF_8);
        String codeOnly = stripComments(source);

        for (Pattern forbidden : FORBIDDEN_WRITE_PATTERNS) {
            assertFalse(forbidden.matcher(codeOnly).find(),
                    "Sink 源码禁止出现写缓存模式 `" + forbidden.pattern() + "`（重放写回旧值 = 补偿制造不一致）；"
                            + "若确需引入，必须经 codex 评审 + 计划文档边界更新");
        }
        for (String required : REQUIRED_CALLS) {
            assertTrue(codeOnly.contains(required),
                    "Sink 源码必须包含减语义调用 `" + required + "`（定位 + 驱逐/清空是补偿动作的核心）");
        }

        // 段 2：注入自检——合成含写操作的伪源码必须被规则命中（防规则静默失效）
        String fakeSource = "class FakeSink {"
                + " void deliver() { cacheManager.getCache(\"role\").put(\"1\", \"v\"); } }";
        boolean hit = FORBIDDEN_WRITE_PATTERNS.stream()
                .anyMatch(pattern -> pattern.matcher(stripComments(fakeSource)).find());
        assertTrue(hit, "注入自检：含 `.put(` 的伪 Sink 必须被扫描规则命中（否则规则失效、本测试形同虚设）");
    }

    /**
     * 剥离 Java 注释（行注释 {@code //} + 块注释 {@code /* *}{@code /}），避免文档字符串里的
     * 示例代码触发误报（循 {@code SinkNeverWritesAccessTokenCacheTest} 先例）。
     */
    private static String stripComments(String source) {
        // 块注释（含 Javadoc）：/* ... */
        String noBlock = source.replaceAll("(?s)/\\*.*?\\*/", "");
        // 行注释：// ...（保留换行以维持行号结构，便于失败信息定位）
        return noBlock.replaceAll("(?m)//[^\\n]*", "");
    }

}
