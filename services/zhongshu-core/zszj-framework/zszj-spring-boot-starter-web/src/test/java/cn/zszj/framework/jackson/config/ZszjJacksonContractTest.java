package cn.zszj.framework.jackson.config;

import cn.zszj.framework.common.util.date.DateUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ZS-SEC-009 接口边界契约测试：验证经 E44 {@link ZszjJacksonAutoConfiguration} 定制后的 ObjectMapper
 * 在 ID 与时间维度的稳定合同。以「复刻 Spring Boot 构建」的方式应用真实 builder customizer，
 * 直接锁定对外 wire 格式（而非仅测单个序列化器），避免配置漂移。
 *
 * <p>ID 合同：id/*Id/*Ids 语义的 Long（含 {@code Set<Long>} 集合元素）恒输出 string，与数值大小无关；
 * 计数/金额等非 ID 的 Long 仍为 number。
 * <p>时间合同：LocalDateTime 输出 epoch millis(number)，且固定 {@link DateUtils#ZONE_DEFAULT}（GMT+8），
 * 不随部署 JVM 默认时区漂移。
 */
public class ZszjJacksonContractTest {

    /** 复刻 Spring Boot 构建：应用 E44 的 builder customizer 得到与运行期一致的 ObjectMapper */
    private static ObjectMapper buildMapper() {
        Jackson2ObjectMapperBuilderCustomizer customizer = new ZszjJacksonAutoConfiguration().ldtEpochMillisCustomizer();
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        customizer.customize(builder);
        return builder.build();
    }

    /** 用独立纯净 mapper 解析，避免自定义配置干扰断言（只看 wire JSON 的节点类型/值） */
    private static JsonNode toNode(Object vo) throws Exception {
        String json = buildMapper().writeValueAsString(vo);
        return new ObjectMapper().readTree(json);
    }

    @Test
    @DisplayName("ID 语义 Long 恒 string（大/小 ID 一致），非 ID 的 Long 保持 number")
    public void testIdContract() throws Exception {
        ContractVO vo = new ContractVO();
        vo.setId(1L);                    // 小 ID：旧 NumberSerializer 会输出 number，本合同要求 string
        vo.setUserId(2L);                // 外键 ID
        vo.setBigId(9007199254740993L);  // > 2^53-1 大 ID
        Set<Long> postIds = new LinkedHashSet<>();
        postIds.add(10L);
        postIds.add(20L);
        vo.setPostIds(postIds);          // ID 集合
        vo.setCount(100L);               // 非 ID 的 Long（计数）
        vo.setTotal(200L);               // 非 ID 的 Long（总数）
        vo.setPageSize(10);              // 非 Long
        vo.setName("众墅");

        JsonNode node = toNode(vo);

        // 关键回归断言：小 ID 也必须是 string（证明恒 string、与数值大小无关，压过 NumberSerializer）
        assertTrue(node.get("id").isTextual(), "小 ID 应为 string（不随数值变类型）");
        assertEquals("1", node.get("id").asText());
        assertTrue(node.get("userId").isTextual(), "外键 ID 应为 string");
        assertEquals("2", node.get("userId").asText());
        assertTrue(node.get("bigId").isTextual(), "大 ID 应为 string");
        assertEquals("9007199254740993", node.get("bigId").asText());

        // ID 集合元素 → string
        assertTrue(node.get("postIds").isArray(), "postIds 应为数组");
        assertEquals(2, node.get("postIds").size());
        assertTrue(node.get("postIds").get(0).isTextual(), "集合 ID 元素应为 string");
        assertEquals("10", node.get("postIds").get(0).asText());
        assertEquals("20", node.get("postIds").get(1).asText());

        // 非 ID 的 Long → number（不盲目字符串化）
        assertTrue(node.get("count").isNumber(), "计数应保持 number");
        assertEquals(100L, node.get("count").asLong());
        assertTrue(node.get("total").isNumber(), "total 应保持 number");
        assertEquals(200L, node.get("total").asLong());
        assertTrue(node.get("pageSize").isNumber(), "pageSize 应保持 number");

        // 普通字符串不受影响
        assertEquals("众墅", node.get("name").asText());
    }

    @Test
    @DisplayName("null ID 输出 JSON null，而非字符串 \"null\"")
    public void testNullId() throws Exception {
        ContractVO vo = new ContractVO();
        vo.setId(null);
        JsonNode node = toNode(vo);
        assertNotNull(node.get("id"), "字段应存在（默认 inclusion 不忽略 null）");
        assertTrue(node.get("id").isNull(), "null ID 应序列化为 JSON null");
    }

    @Test
    @DisplayName("LocalDateTime 输出 epoch millis，且固定时区不随 JVM 默认时区漂移")
    public void testTimeContract() throws Exception {
        LocalDateTime t = LocalDateTime.of(2026, 9, 10, 12, 0, 0);
        long expected = t.atZone(DateUtils.ZONE_DEFAULT).toInstant().toEpochMilli();

        TimeZone original = TimeZone.getDefault();
        try {
            // 模拟部署在 UTC 的 JVM
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            ContractVO vo = new ContractVO();
            vo.setCreateTime(t);
            JsonNode utcNode = toNode(vo);
            assertTrue(utcNode.get("createTime").isNumber(), "时间应为 epoch millis(number)");
            assertEquals(expected, utcNode.get("createTime").asLong(), "UTC JVM 下 millis 应基于固定时区");

            // 模拟部署在纽约的 JVM：若仍用 systemDefault，millis 会偏移；固定时区后应与 UTC 一致
            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
            JsonNode nyNode = toNode(vo);
            assertEquals(expected, nyNode.get("createTime").asLong(), "纽约 JVM 下 millis 应与 UTC 一致（固定时区）");
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    @DisplayName("反序列化：string 形式的 ID/millis 能还原为 Long/LocalDateTime（请求侧兼容）")
    public void testDeserializeCompatibility() throws Exception {
        // 前端以 string 传 ID、以 millis 传时间，后端应能还原
        String json = "{\"id\":\"1024\",\"userId\":\"2048\",\"createTime\":"
                + LocalDateTime.of(2026, 9, 10, 12, 0, 0)
                        .atZone(DateUtils.ZONE_DEFAULT).toInstant().toEpochMilli() + "}";
        ContractVO vo = buildMapper().readValue(json, ContractVO.class);
        assertEquals(1024L, vo.getId(), "string ID 应强转为 Long");
        assertEquals(2048L, vo.getUserId(), "string 外键 ID 应强转为 Long");
        assertEquals(LocalDateTime.of(2026, 9, 10, 12, 0, 0), vo.getCreateTime(), "millis 应还原为固定时区 LocalDateTime");
    }

    @Data
    static class ContractVO {
        private Long id;
        private Long userId;
        private Long bigId;
        private Set<Long> postIds;
        private Long count;
        private Long total;
        private Integer pageSize;
        private LocalDateTime createTime;
        private String name;
    }
}
