package cn.zszj.module.firstchain.framework;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 首链域手写 JDBC SQL 的 PG 方言守卫（源码扫描）：H2 单测宽容、真实 PG 报错的写法一律禁止。
 *
 * <ul>
 *     <li>{@code ? IS NULL}：未定型的空参数——PG 无列上下文推断类型，{@code could not determine data type of
 *     parameter $N}（42P18）；H2 不报，故单测与历次 PG 运行期套件都未发现。线索分页 {@code (? IS NULL OR org_id = ?)}
 *     在真实 server 返回 500（首链 E2E 暴露）。可选过滤应<b>按需拼接条件</b>，而不是以 NULL 参数「关闭」条件。</li>
 *     <li>{@code IFNULL(}：MySQL 专有，PG 无此函数（FC-002 R1 同款缺陷）；应使用 {@code COALESCE}。</li>
 * </ul>
 *
 * <p>扫描范围：本模块与 bpm 域首链包的主源码（手写 SQL 都在字符串字面量内）。
 *
 * @author ZS-FC-002
 */
class PgDialectSqlGuardTest {

    private static final List<Path> ROOTS = List.of(
            Path.of("src/main/java"),
            Path.of("../zszj-module-bpm/src/main/java/cn/zszj/module/bpm/firstchain"));

    private static final Pattern UNTYPED_NULL_PARAM = Pattern.compile("\\?\\s+IS\\s+(NOT\\s+)?NULL", Pattern.CASE_INSENSITIVE);

    private static final Pattern IFNULL = Pattern.compile("\\bIFNULL\\s*\\(", Pattern.CASE_INSENSITIVE);

    @Test
    void noUntypedNullParameterInHandWrittenSql() throws IOException {
        assertThat(violations(UNTYPED_NULL_PARAM))
                .as("手写 SQL 禁止 `? IS NULL`（PG 42P18 未定型参数）；可选过滤请按需拼接条件")
                .isEmpty();
    }

    @Test
    void noMysqlOnlyIfNullInHandWrittenSql() throws IOException {
        assertThat(violations(IFNULL)).as("手写 SQL 禁止 IFNULL（PG 无此函数），请用 COALESCE").isEmpty();
    }

    private static List<String> violations(Pattern pattern) throws IOException {
        List<String> hits = new ArrayList<>();
        for (Path root : ROOTS) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(root)) {
                for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".java"))::iterator) {
                    List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                    for (int i = 0; i < lines.size(); i++) {
                        String line = lines.get(i);
                        String trimmed = line.trim();
                        if (trimmed.startsWith("*") || trimmed.startsWith("//") || trimmed.startsWith("/*")) {
                            continue; // 注释内的说明性文字不计
                        }
                        if (pattern.matcher(line).find()) {
                            hits.add(file + ":" + (i + 1) + "  " + trimmed);
                        }
                    }
                }
            }
        }
        return hits;
    }

}
