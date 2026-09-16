package cn.zszj.module.infra.framework.file.core.client.db;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * codex r1 P2：DB 清点 LIKE 前缀字面转义单测。
 *
 * <p>合同（{@link DBFileClient#escapeLikePrefix}）：转义 {@code \}/{@code %}/{@code _} 三字符，
 * 与 {@code FileContentMapper.selectPathSummariesByPrefix} 的 {@code ESCAPE '\'} 子句构成同一
 * 「字面前缀」合同——路径中的 {@code _}/{@code %} 是合法字符，未转义会被展开为通配符
 * （前缀外候选挤占扫描上限/污染清点结果）。</p>
 */
public class DBFileClientEscapeTest {

    @Test
    public void escapeLikePrefix_escapesWildcardCharacters() {
        assertEquals("asset/\\_/a", DBFileClient.escapeLikePrefix("asset/_/a"),
                "下划线转义为字面量");
        assertEquals("report-100\\%.bin", DBFileClient.escapeLikePrefix("report-100%.bin"),
                "百分号转义为字面量");
        assertEquals("a\\\\b", DBFileClient.escapeLikePrefix("a\\b"),
                "反斜杠首先自转义（保持字面且不破坏后续转义）");
        assertEquals("plain/path/x.bin", DBFileClient.escapeLikePrefix("plain/path/x.bin"),
                "常规字符不变");
        assertEquals("", DBFileClient.escapeLikePrefix(""));
        assertNull(DBFileClient.escapeLikePrefix(null));
    }

}
