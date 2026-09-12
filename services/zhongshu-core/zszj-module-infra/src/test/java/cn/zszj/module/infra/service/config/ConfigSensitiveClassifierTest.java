package cn.zszj.module.infra.service.config;

import cn.zszj.module.infra.dal.dataobject.config.ConfigDO;
import cn.zszj.module.infra.service.config.ConfigSensitiveClassifier.SensitiveLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link ConfigSensitiveClassifier} 的纯单元测试（ZS-CFG-001.B）。
 *
 * 无 Spring 上下文、无 DB，直接验证三档分类与掩码逻辑。
 */
public class ConfigSensitiveClassifierTest {

    private final ConfigSensitiveClassifier classifier = new ConfigSensitiveClassifier();

    private static ConfigDO config(String key, String value, boolean visible) {
        ConfigDO cfg = new ConfigDO();
        cfg.setConfigKey(key);
        cfg.setValue(value);
        cfg.setVisible(visible);
        return cfg;
    }

    @Test
    public void classify_secretKeyPattern_shouldBeSecret() {
        ConfigDO cfg = config("sys.db.password", "P@ssw0rd", true);
        assertEquals(SensitiveLevel.SECRET, classifier.classify(cfg),
                "秘密键模式即使 visible=true 也判 SECRET");
    }

    @Test
    public void classify_secretKeyWithSeparator_shouldBeSecret() {
        // 归一化：去掉 - 与 _ 后应命中 accesskey / apikey 等模式
        ConfigDO cfg = config("sys.sms.access_key", "AK-123", true);
        assertEquals(SensitiveLevel.SECRET, classifier.classify(cfg));
    }

    @Test
    public void classify_invisible_shouldBeSensitive() {
        ConfigDO cfg = config("biz.internal.addr", "http://10.0.0.1", false);
        assertEquals(SensitiveLevel.SENSITIVE, classifier.classify(cfg));
    }

    @Test
    public void classify_normalVisible_shouldBeNormal() {
        ConfigDO cfg = config("biz.cache.ttl", "300", true);
        assertEquals(SensitiveLevel.NORMAL, classifier.classify(cfg));
    }

    @Test
    public void maskValue_secret_shouldMask() {
        ConfigDO cfg = config("biz.pay.secret", "sk_live_123", true);
        assertEquals("******", classifier.maskValue(cfg));
    }

    @Test
    public void maskValue_normal_shouldKeep() {
        ConfigDO cfg = config("biz.cache.ttl", "300", true);
        assertEquals("300", classifier.maskValue(cfg));
    }

}
