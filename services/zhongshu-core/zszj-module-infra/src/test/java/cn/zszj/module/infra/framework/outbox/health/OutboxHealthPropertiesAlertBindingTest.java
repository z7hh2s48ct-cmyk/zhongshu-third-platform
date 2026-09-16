package cn.zszj.module.infra.framework.outbox.health;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link OutboxHealthProperties.Alert} 绑定测试（ZS-OPS-002.B，用例 ⑪）。
 *
 * <p>验证 {@code infra.outbox.health.alert.enabled / interval-ms} 经 relaxed binding（{@code interval-ms} ↔
 * {@code intervalMs}）正确绑定到 {@link OutboxHealthProperties} 嵌套 {@code alert} 对象——与
 * {@code OutboxHealthAlertScheduler} 的 {@code @ConditionalOnProperty}（Environment 直读）和
 * {@code @Scheduled(fixedRateString = "${infra.outbox.health.alert.interval-ms:60000}")} 同源，
 * 断言「yaml 键名 → 属性对象」链路无错位（收口择一：扩展既有前缀嵌套）。
 *
 * <p>用 Spring Boot {@link Binder} 直测绑定（不起上下文；{@link OutboxHealthMonitorTest} 已验证阈值绑定同机制）。
 */
public class OutboxHealthPropertiesAlertBindingTest {

    /** 用例 B1：显式配置（enabled=false、interval-ms=30000，kebab-case）正确覆盖默认值。 */
    @Test
    public void test_alert配置_显式绑定() {
        Map<String, Object> source = new HashMap<>();
        source.put("infra.outbox.health.alert.enabled", "false");
        source.put("infra.outbox.health.alert.interval-ms", "30000");

        OutboxHealthProperties props = bind(source);

        assertFalse(props.getAlert().isEnabled(), "alert.enabled=false 应绑定成功（门控关闭探针装配）");
        assertEquals(30000L, props.getAlert().getIntervalMs(), "interval-ms kebab-case 应 relaxed 绑定到 intervalMs");
    }

    /** 用例 B2：缺省时默认启用、默认周期 60000（与 application.yaml 显式声明值一致）。 */
    @Test
    public void test_alert配置_默认值() {
        OutboxHealthProperties props = bind(new HashMap<>());

        assertTrue(props.getAlert().isEnabled(), "默认应启用告警探针（退出条件：积压被发现）");
        assertEquals(60000L, props.getAlert().getIntervalMs(), "默认探针周期 60000ms（保守占位，待 WP-19 实测回填）");
        // 既有 JOB-004 阈值字段不受 alert 扩展影响（同前缀共存不覆盖）
        assertEquals(1000, props.getBacklogWarn(), "既有阈值字段默认值不受嵌套 alert 扩展影响");
    }

    private OutboxHealthProperties bind(Map<String, Object> source) {
        // 空源时 bind 结果为空（无任何可绑定属性），取代码默认实例断言默认值（与 Spring Boot 缺省绑定语义一致）
        return new Binder(new MapConfigurationPropertySource(source))
                .bind("infra.outbox.health", Bindable.of(OutboxHealthProperties.class))
                .orElseGet(OutboxHealthProperties::new);
    }

}
