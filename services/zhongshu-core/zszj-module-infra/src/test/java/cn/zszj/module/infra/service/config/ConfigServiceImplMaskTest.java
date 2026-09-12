package cn.zszj.module.infra.service.config;

import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.test.core.ut.BaseMockitoUnitTest;
import cn.zszj.module.infra.controller.admin.config.vo.ConfigRespVO;
import cn.zszj.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import cn.zszj.module.infra.dal.dataobject.config.ConfigDO;
import cn.zszj.module.infra.dal.mysql.config.ConfigMapper;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import static cn.zszj.module.infra.enums.ErrorCodeConstants.CONFIG_SENSITIVE_CAN_NOT_SET_VISIBLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * {@link ConfigServiceImpl} 脱敏与写守卫的纯 Mockito 单元测试（ZS-CFG-001.B）。
 *
 * 说明：模块既有的 {@code ConfigServiceImplTest} 基于 {@code BaseDbUnitTest}（H2 + Spring 上下文），
 * 本批次要求新增用例为 mock-based、不启动 Spring/DB，故独立成本类。
 */
public class ConfigServiceImplMaskTest extends BaseMockitoUnitTest {

    @Mock
    private ConfigMapper configMapper;

    @Spy
    private ConfigSensitiveClassifier sensitiveClassifier = new ConfigSensitiveClassifier();

    @InjectMocks
    private ConfigServiceImpl configService;

    private static ConfigDO config(String key, String value, boolean visible) {
        ConfigDO cfg = new ConfigDO();
        cfg.setConfigKey(key);
        cfg.setValue(value);
        cfg.setVisible(visible);
        return cfg;
    }

    @Test
    public void getMaskedConfigRespVO_secretValue_shouldBeMasked() {
        ConfigDO cfg = config("sys.db.password", "P@ssw0rd", true);
        ConfigRespVO vo = configService.getMaskedConfigRespVO(cfg);
        assertEquals("******", vo.getValue(), "详情路径秘密值必须脱敏");
        assertEquals("sys.db.password", vo.getKey(), "键名保持可读");
    }

    @Test
    public void getMaskedConfigRespVO_normalValue_shouldKeep() {
        ConfigDO cfg = config("biz.cache.ttl", "300", true);
        ConfigRespVO vo = configService.getMaskedConfigRespVO(cfg);
        assertEquals("300", vo.getValue(), "普通项 value 原样返回");
    }

    @Test
    public void classifySensitive_secretKey_shouldBeSecret() {
        ConfigDO cfg = config("sys.db.password", "P@ssw0rd", true);
        assertEquals(ConfigSensitiveClassifier.SensitiveLevel.SECRET, configService.classifySensitive(cfg));
    }

    @Test
    public void updateConfig_setSecretKeyVisible_shouldReject() {
        ConfigSaveReqVO req = new ConfigSaveReqVO();
        req.setId(1L);
        req.setKey("sys.db.password");
        req.setVisible(true);
        when(configMapper.selectById(1L)).thenReturn(config("sys.db.password", "P@ssw0rd", false));

        ServiceException ex = assertThrows(ServiceException.class, () -> configService.updateConfig(req));
        assertEquals(CONFIG_SENSITIVE_CAN_NOT_SET_VISIBLE.getCode(), ex.getCode(),
                "秘密键由不可见翻为可见必须被拒绝");
    }

    @Test
    public void updateConfig_changeNormalKeyToSecretAndSetVisible_shouldReject() {
        ConfigSaveReqVO req = new ConfigSaveReqVO();
        req.setId(1L);
        req.setKey("sys.db.password");
        req.setVisible(true);
        when(configMapper.selectById(1L)).thenReturn(config("biz.foo", "bar", false));

        ServiceException ex = assertThrows(ServiceException.class, () -> configService.updateConfig(req));
        assertEquals(CONFIG_SENSITIVE_CAN_NOT_SET_VISIBLE.getCode(), ex.getCode(),
                "普通键改为秘密模式键并置可见必须被拒绝（TOCTOU 旁路）");
    }

}
