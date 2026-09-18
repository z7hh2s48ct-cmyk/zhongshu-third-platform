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
 * ZS-LOGIN-005.B 静态扫描：{@link OAuth2TokenRevocationCompensationSink} 源码<b>结构上不可能复活凭据</b>。
 *
 * <p><b>合同</b>（循 {@code OutboxEventSink}）：Sink「只做投递动作，禁止回写业务事实」——
 * 全代码路径不得包含任何"写缓存"操作（{@code redisDAO.set} / {@code opsForValue().set} /
 * {@code stringRedisTemplate.*put/write/increment} 等），只允许 {@code markRevoked}（SETNX 墓碑）
 * 与 {@code delete}（缓存清理）两种"减语义"操作。
 *
 * <p><b>为何静态扫描而非运行时断言</b>：运行时断言只能覆盖已执行路径，静态扫描锁定<b>所有代码路径</b>
 * （含异常分支、未来新增分支），从源码结构上杜绝"Sink 内意外写缓存导致凭据复活"的安全缺口。
 * 对齐计划 §5-3 边界「Sink 全代码路径无任何写缓存操作」。
 *
 * <p>本测试<b>骨架与 GREEN 均须通过</b>——Sink 从 RED-1 骨架起就只调用 {@code markRevoked + delete}，
 * GREEN-1 落地后仍保持该契约。若未来 PR 引入写缓存操作，本测试立即失败阻断合并。
 *
 * @author ZS-LOGIN-005.B
 */
public class SinkNeverWritesAccessTokenCacheTest {

    private static final Path SINK_SOURCE = Paths.get(
            "src/main/java/cn/zszj/module/system/framework/outbox/OAuth2TokenRevocationCompensationSink.java");

    /** 禁止模式：任何"写缓存"操作（复活凭据的安全缺口）。 */
    private static final List<Pattern> FORBIDDEN_PATTERNS = List.of(
            // redisDAO.set(...) — OAuth2AccessTokenRedisDAO 的缓存写入方法
            Pattern.compile("\\bredisDAO\\.set\\s*\\("),
            Pattern.compile("\\boauth2AccessTokenRedisDAO\\.set\\s*\\("),
            // stringRedisTemplate 直接写入（绕过 DAO 层）
            Pattern.compile("\\bstringRedisTemplate\\."),
            Pattern.compile("\\.opsForValue\\(\\)\\.set\\s*\\("),
            Pattern.compile("\\.opsForHash\\(\\)\\.put"),
            Pattern.compile("\\.opsForList\\(\\)\\.(left|right)Push"),
            Pattern.compile("\\.opsForSet\\(\\)\\.add"),
            Pattern.compile("\\.opsForZSet\\(\\)\\.add"),
            // 通用写入动词（在 Sink 上下文内出现即可疑）
            Pattern.compile("\\.write\\s*\\("),
            Pattern.compile("\\.put\\s*\\("),
            Pattern.compile("\\.increment\\s*\\(")
    );

    /** 允许模式：Sink 必须调用的"减语义"操作（墓碑 SETNX + 缓存清理）。 */
    private static final List<String> REQUIRED_CALLS = List.of(
            "markRevoked",
            ".delete("
    );

    @Test
    public void sinkSource_neverWritesAccessTokenCache() throws Exception {
        assertTrue(Files.exists(SINK_SOURCE),
                "Sink 源文件必须存在: " + SINK_SOURCE.toAbsolutePath());
        String source = Files.readString(SINK_SOURCE, StandardCharsets.UTF_8);

        // 剥离注释与 Javadoc，避免文档字符串里的示例触发误报
        String codeOnly = stripComments(source);

        for (Pattern forbidden : FORBIDDEN_PATTERNS) {
            assertFalse(forbidden.matcher(codeOnly).find(),
                    "Sink 源码禁止出现写缓存模式 `" + forbidden.pattern() + "`（结构上不可能复活凭据）；"
                            + "若确需引入，必须经 codex 评审 + 计划文档 §5-3 边界更新");
        }

        for (String required : REQUIRED_CALLS) {
            assertTrue(codeOnly.contains(required),
                    "Sink 源码必须包含减语义调用 `" + required + "`（墓碑 + 缓存清理是补偿动作的核心）");
        }
    }

    @Test
    public void sinkSource_onlyImportsAllowedRedisCollaborators() throws Exception {
        assertTrue(Files.exists(SINK_SOURCE), "Sink 源文件必须存在");
        String source = Files.readString(SINK_SOURCE, StandardCharsets.UTF_8);

        // 允许 import：OAuth2AccessTokenRedisDAO（markRevoked + delete）
        assertTrue(source.contains("import cn.zszj.module.system.dal.redis.oauth2.OAuth2AccessTokenRedisDAO;"),
                "Sink 必须仅通过 OAuth2AccessTokenRedisDAO 与 Redis 交互（DAO 层封装减语义）");
        // 禁止 import：StringRedisTemplate（绕过 DAO 直接操作 Redis 的入口）
        assertFalse(source.contains("import org.springframework.data.redis.core.StringRedisTemplate;"),
                "Sink 禁止直接 import StringRedisTemplate（必须经 DAO 层，便于静态扫描与契约锁定）");
        assertFalse(source.contains("import org.springframework.data.redis.core.RedisTemplate;"),
                "Sink 禁止直接 import RedisTemplate");
    }

    /**
     * 剥离 Java 注释（行注释 {@code //} + 块注释 {@code /* *}{@code /}），避免文档字符串里的
     * 示例代码触发误报（如 Javadoc 里引用 {@code redisDAO.set} 说明"禁止此操作"）。
     */
    private static String stripComments(String source) {
        // 块注释（含 Javadoc）：/* ... */
        String noBlock = source.replaceAll("(?s)/\\*.*?\\*/", "");
        // 行注释：// ...（保留换行以维持行号结构，便于失败信息定位）
        return noBlock.replaceAll("(?m)//[^\\n]*", "");
    }

}
