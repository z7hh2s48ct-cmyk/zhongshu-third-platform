package cn.zszj.server;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Outbox 监测健康组配置契约测试（ZS-OPS-002.B，纯 yaml 解析、不起 Spring 上下文）。
 *
 * <p>断言 application.yaml 的健康端点分组满足 OPS-002.B 两条硬边界：
 * <ul>
 *   <li><b>liveness 纯净红线</b>（开发计划铁律 9）：{@code outboxQueue} 只允许出现在 {@code monitoring} 组，
 *       <b>绝不</b>出现在 {@code liveness} 组——队列积压计入 liveness 会令编排器在积压时重启实例，
 *       重启不消化积压，形成「越积压越重启」的重启风暴；</li>
 *   <li><b>ENG-005 匿名安全边界</b>：{@code /actuator/health} 与 {@code /actuator/health/**} 对匿名放行，
 *       聚合端点必须保持 Spring Boot 默认 {@code show-details: never}（组件详情只在 {@code monitoring} 组开放，
 *       且该组详情仅指标值+越阈码，无 payload/秘密）。</li>
 * </ul>
 * 告警配置（{@code infra.outbox.health.alert.*}）与探针装配/调度同源，此处一并冻结存在性（占位值红线见运行说明）。
 */
public class OutboxMonitoringHealthGroupTest {

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mergedDocs() {
        Yaml yaml = new Yaml();
        Map<String, Object> merged = new java.util.HashMap<>();
        try (InputStream in = OutboxMonitoringHealthGroupTest.class
                .getResourceAsStream("/application.yaml")) {
            assertNotNull(in, "zszj-server classpath 应含 application.yaml");
            for (Object doc : yaml.loadAll(in)) {
                if (doc instanceof Map) {
                    merge(merged, (Map<String, Object>) doc);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("解析 application.yaml 失败", e);
        }
        return merged;
    }

    @SuppressWarnings("unchecked")
    private static void merge(Map<String, Object> target, Map<String, Object> source) {
        for (Map.Entry<String, Object> e : source.entrySet()) {
            if (e.getValue() instanceof Map && target.get(e.getKey()) instanceof Map) {
                merge((Map<String, Object>) target.get(e.getKey()), (Map<String, Object>) e.getValue());
            } else {
                target.put(e.getKey(), e.getValue());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> healthGroup(Map<String, Object> merged) {
        Map<String, Object> management = (Map<String, Object>) merged.get("management");
        assertNotNull(management, "application.yaml 应含 management 配置");
        Map<String, Object> endpoint = (Map<String, Object>) management.get("endpoint");
        assertNotNull(endpoint, "application.yaml 应含 management.endpoint 配置");
        Map<String, Object> health = (Map<String, Object>) endpoint.get("health");
        assertNotNull(health, "application.yaml 应含 management.endpoint.health 配置");
        return (Map<String, Object>) health.get("group");
    }

    /**
     * 用例 C1：monitoring 组包含 outboxQueue 与 db/redis/ping 依赖（退出条件「依赖故障和积压被发现」），
     * 且<b>组详情不对匿名开放</b>（codex r0 P1：/actuator/health/** 匿名可达，内建 db/redis 指示器详情
     * 含依赖异常原文/内部地址，必须 show-details: never；越阈码/指标详情走探针日志与授权端点）。
     */
    @Test
    public void test_monitoring组_含队列与依赖_详情不对匿名开放() {
        Map<String, Object> group = healthGroup(mergedDocs());
        Map<String, Object> monitoring = (Map<String, Object>) group.get("monitoring");
        assertNotNull(monitoring, "应定义 management.endpoint.health.group.monitoring 组");
        List<String> include = List.of(String.valueOf(monitoring.get("include")).split(","));
        assertTrue(include.contains("outboxQueue"), "monitoring 组应含 outboxQueue 队列指示器");
        assertTrue(include.contains("db"), "monitoring 组应含 db 依赖指示器");
        assertTrue(include.contains("redis"), "monitoring 组应含 redis 依赖指示器");
        assertFalse("always".equalsIgnoreCase(String.valueOf(monitoring.get("show-details"))),
                "monitoring 组匿名可达，不得 show-details: always（内建 db/redis 详情含异常原文，ENG-005/codex r0 P1）");
    }

    /** 用例 C2（铁律 9 反重启风暴）：liveness 组不存在或不含 outboxQueue（readiness 同理校验）。 */
    @Test
    public void test_liveness组_纯净不含队列() {
        Map<String, Object> group = healthGroup(mergedDocs());
        Map<String, Object> liveness = (Map<String, Object>) group.get("liveness");
        if (liveness != null) {
            Object include = liveness.get("include");
            assertTrue(include == null || !String.valueOf(include).contains("outboxQueue"),
                    "liveness 组绝不允许纳入 outboxQueue（积压≠进程死亡，进 liveness 触发重启风暴）");
        }
        Map<String, Object> readiness = (Map<String, Object>) group.get("readiness");
        if (readiness != null) {
            Object include = readiness.get("include");
            assertTrue(include == null || !String.valueOf(include).contains("outboxQueue"),
                    "readiness 组不应纳入 outboxQueue（队列信号归聚合 health 与 monitoring 组）");
        }
    }

    /** 用例 C3（ENG-005 匿名安全边界）：聚合 health 端点保持默认 show-details（组件详情不对匿名暴露）。 */
    @Test
    public void test_聚合健康端点_不开全局详情() {
        Map<String, Object> merged = mergedDocs();
        Map<String, Object> management = (Map<String, Object>) merged.get("management");
        Map<String, Object> endpoint = (Map<String, Object>) management.get("endpoint");
        Map<String, Object> health = (Map<String, Object>) endpoint.get("health");
        // show-details 若显式配置，绝不能是 always（/actuator/health/** 匿名放行，全局 always 会泄露组件详情）
        assertFalse("always".equalsIgnoreCase(String.valueOf(health.get("show-details"))),
                "聚合 /actuator/health 匿名可达，不得全局 show-details: always（ENG-005）");
    }

    /** 用例 C4：告警探针配置存在且启用（infra.outbox.health.alert.enabled/interval-ms）。 */
    @Test
    public void test_告警探针配置_存在且启用() {
        Map<String, Object> merged = mergedDocs();
        Map<String, Object> infra = (Map<String, Object>) merged.get("infra");
        Map<String, Object> outbox = (Map<String, Object>) infra.get("outbox");
        Map<String, Object> healthProps = (Map<String, Object>) outbox.get("health");
        Map<String, Object> alert = (Map<String, Object>) healthProps.get("alert");
        assertNotNull(alert, "应定义 infra.outbox.health.alert 配置（周期告警探针）");
        assertEquals(true, alert.get("enabled"), "告警探针默认应启用（退出条件：积压被发现）");
        assertNotNull(alert.get("interval-ms"), "探针周期应显式声明（占位值，待 WP-19 实测回填）");
    }

}
