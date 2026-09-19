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
 * 在 ID 与时间维度的稳定合同。以「复刻 Spring Boot 构建」的方式应用真实 builder customizer 与 Module bean，
 * 直接锁定对外 wire 格式（而非仅测单个序列化器），避免配置漂移。
 *
 * <p>ID 合同（ZS-SEC-009.B，已激活）：id/*Id/*Ids 语义的 Long（含 {@code Set<Long>} 集合元素）恒输出 string；
 * 两端前端 ID 数值比较已随本批迁移（string 比较），非 ID 的 Long（count/total）保持 number。
 * <p>时间合同（.A 已交付）：LocalDateTime 输出 epoch millis(number)，且固定 {@link DateUtils#ZONE_DEFAULT}（GMT+8），
 * 不随部署 JVM 默认时区漂移；生产端亦经 {@link DateUtils#now()} 对齐同一固定时区（避免 UTC 部署下令牌过期时间偏移）。
 */
public class ZszjJacksonContractTest {

    /** 复刻 Spring Boot 构建：应用 E44 的 builder customizer + Module bean（introspector 挂载点）得到与运行期一致的 ObjectMapper */
    private static ObjectMapper buildMapper() {
        Jackson2ObjectMapperBuilderCustomizer customizer = new ZszjJacksonAutoConfiguration().ldtEpochMillisCustomizer();
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        customizer.customize(builder);
        // Boot 会把 Module bean 注册进 builder 构建的 mapper；IdToStringAnnotationIntrospector 挂在该 Module 上
        builder.modules(new ZszjJacksonAutoConfiguration().timestampSupportModuleBean());
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
        vo.setPostIds(postIds);          // ID 集合（Set）
        vo.setMenuIds(java.util.Arrays.asList(30L, 40L)); // ID 集合（List，覆盖另一种 Collection 形态）
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

        // ID 集合元素 → string（Set 与 List 两种 Collection 形态一致）
        assertTrue(node.get("postIds").isArray(), "postIds 应为数组");
        assertEquals(2, node.get("postIds").size());
        assertTrue(node.get("postIds").get(0).isTextual(), "集合 ID 元素应为 string");
        assertEquals("10", node.get("postIds").get(0).asText());
        assertEquals("20", node.get("postIds").get(1).asText());
        assertTrue(node.get("menuIds").isArray(), "menuIds 应为数组");
        assertEquals("30", node.get("menuIds").get(0).asText(), "List 集合 ID 元素应为 string");
        assertEquals("40", node.get("menuIds").get(1).asText());

        // 非 ID 的 Long → number（不盲目字符串化）
        assertTrue(node.get("count").isNumber(), "计数应保持 number");
        assertEquals(100L, node.get("count").asLong());
        assertTrue(node.get("total").isNumber(), "total 应保持 number");
        assertEquals(200L, node.get("total").asLong());
        assertTrue(node.get("pageSize").isNumber(), "pageSize 应保持 number");

        // 普通 string 不受影响
        assertEquals("众墅", node.get("name").asText());
    }

    @Test
    @DisplayName("裸 ID 集合响应（r0 P1）：CommonResult<Set<Long>> 的 data 元素恒 string，非 Long 集合不误伤")
    public void testBareIdCollectionData() throws Exception {
        java.util.Set<Long> roleIds = new LinkedHashSet<>();
        roleIds.add(7L);
        roleIds.add(8L);
        JsonNode dataNode = toNode(new SetLongResult(roleIds)).get("data");
        assertTrue(dataNode.get(0).isTextual(), "CommonResult<Set<Long>> 的 data 元素应为 string（permission 端点裸 ID 集合）");
        assertEquals("7", dataNode.get(0).asText());
        assertEquals("8", dataNode.get(1).asText());

        // data 为 List<String>：本就是 string，不因 data 名被二次处理（值不变）
        JsonNode strNode = toNode(new StringListResult(java.util.Arrays.asList("a", "b"))).get("data");
        assertEquals("a", strNode.get(0).asText());
        assertEquals("b", strNode.get(1).asText());

        // data 为 List<Integer>（计数等非 ID 语义）：保持 number 不误伤
        JsonNode intNode = toNode(new IntegerListResult(java.util.Arrays.asList(1, 2))).get("data");
        assertTrue(intNode.get(0).isNumber(), "CommonResult<List<Integer>> 的 data 元素应保持 number");
        assertEquals(1, intNode.get(0).asInt());
    }

    /** 复刻 CommonResult 的 data 形态（具体类型，泛型经 erasure 后 getter 返回 ParameterizedType） */
    record SetLongResult(java.util.Set<Long> data) {
    }

    record StringListResult(java.util.List<String> data) {
    }

    record IntegerListResult(java.util.List<Integer> data) {
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
    @DisplayName("时间合同-令牌生命周期：UTC 部署下新签发令牌的过期时间仍在未来（不被前移 8h）")
    public void testTokenExpiryContract() throws Exception {
        TimeZone original = TimeZone.getDefault();
        try {
            // 模拟部署在 UTC 的 JVM：这是 P1 的触发场景——若生产端仍用 LocalDateTime.now()（systemDefault=UTC），
            // 而序列化固定 GMT+8，令牌 expiresTime 的 epoch 会被前移约 8h → 落到过去 → 客户端判定令牌已过期
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            long before = System.currentTimeMillis();
            // 生产端已对齐固定时区：DateUtils.now() 用 ZONE_DEFAULT，等价 OAuth2TokenServiceImpl 的 expiresTime = now + 有效期
            LocalDateTime expiresTime = DateUtils.now().plusSeconds(1800);
            ContractVO vo = new ContractVO();
            vo.setCreateTime(expiresTime);
            long epoch = toNode(vo).get("createTime").asLong();
            long after = System.currentTimeMillis();
            // 关键回归断言：过期 epoch 必须落在 [before+1800s, after+1800s]（未来），而非前移到过去
            assertTrue(epoch >= before + 1800_000 - 1000 && epoch <= after + 1800_000 + 1000,
                    "新签发令牌过期 epoch 应 ≈ now + 有效期（UTC 部署下不被前移 8h）");
            assertTrue(epoch > after, "令牌过期时间应在未来");
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
        private java.util.List<Long> menuIds;
        private Long count;
        private Long total;
        private Integer pageSize;
        private LocalDateTime createTime;
        private String name;
    }
}
