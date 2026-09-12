package cn.zszj.module.infra.service.config;

import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.test.core.ut.BaseMockitoUnitTest;
import cn.zszj.module.infra.controller.admin.config.vo.ConfigRespVO;
import cn.zszj.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import cn.zszj.module.infra.dal.dataobject.config.ConfigDO;
import cn.zszj.module.infra.dal.mysql.config.ConfigMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import static cn.zszj.module.infra.enums.ErrorCodeConstants.CONFIG_SENSITIVE_CAN_NOT_DOWNGRADE_ON_MASKED_ECHO;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.CONFIG_SENSITIVE_CAN_NOT_SET_VISIBLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    @Test
    public void updateConfig_maskedEchoOnSecret_shouldPreserveStoredValue() {
        // ZS-CFG-001.B P1 回归：管理员仅编辑名称/备注时，前端 ConfigForm.vue 把详情返回的掩码 ****** 原样回传，
        // 保存不得用掩码覆盖库中真实秘密值（如 system.user.init-password），否则数据损坏。
        ConfigSaveReqVO req = new ConfigSaveReqVO();
        req.setId(1L);
        req.setKey("system.user.init-password");
        req.setName("用户初始密码");
        req.setValue("******"); // 回显的掩码，非管理员输入的新值
        req.setVisible(false);
        when(configMapper.selectById(1L))
                .thenReturn(config("system.user.init-password", "RealInitPwd123", false));

        configService.updateConfig(req);

        ArgumentCaptor<ConfigDO> captor = ArgumentCaptor.forClass(ConfigDO.class);
        verify(configMapper).updateById(captor.capture());
        assertEquals("RealInitPwd123", captor.getValue().getValue(),
                "敏感项回传掩码 ****** 时必须保留库中真实值，不得用掩码覆盖（数据损坏回归）");
    }

    @Test
    public void updateConfig_newRealValueOnSecret_shouldPersist() {
        // 管理员为敏感项输入的新真实值（非掩码）应正常持久化，往返保护不得误伤合法改值
        ConfigSaveReqVO req = new ConfigSaveReqVO();
        req.setId(2L);
        req.setKey("sys.db.password");
        req.setName("数据库密码");
        req.setValue("NewP@ssw0rd");
        req.setVisible(false);
        when(configMapper.selectById(2L)).thenReturn(config("sys.db.password", "OldPwd", false));

        configService.updateConfig(req);

        ArgumentCaptor<ConfigDO> captor = ArgumentCaptor.forClass(ConfigDO.class);
        verify(configMapper).updateById(captor.capture());
        assertEquals("NewP@ssw0rd", captor.getValue().getValue(), "敏感项提交非掩码新值时应正常更新");
    }

    @Test
    public void updateConfig_literalMaskOnNormal_shouldPersist() {
        // 普通项（可见且非秘密键）从不脱敏输出，其字面 ****** 是管理员真实输入，不应被当作掩码回显而丢弃
        ConfigSaveReqVO req = new ConfigSaveReqVO();
        req.setId(3L);
        req.setKey("biz.banner.text");
        req.setName("横幅文案");
        req.setValue("******");
        req.setVisible(true);
        when(configMapper.selectById(3L)).thenReturn(config("biz.banner.text", "old", true));

        configService.updateConfig(req);

        ArgumentCaptor<ConfigDO> captor = ArgumentCaptor.forClass(ConfigDO.class);
        verify(configMapper).updateById(captor.capture());
        assertEquals("******", captor.getValue().getValue(), "普通项字面 ****** 应原样持久化，不做往返保护");
    }

    @Test
    public void updateConfig_renameSecretToNonSecretKeyWithMask_shouldRejectDowngrade() {
        // ZS-CFG-001.B r1 P1 回归（codex jshell 实证复现）：有更新权限者不知秘密真值，试图两步洗密——
        // 第一步把 SECRET 键 sys.db.password 改名为非秘密键 biz.alias 并回传掩码 ******（触发保留原值），
        // 保护级由 SECRET 降为 SENSITIVE；若放行，第二步翻 visible=true 即降为 NORMAL，
        // 秘密经 /get-value-by-key 与详情明文可读。故保留掩码值时下调保护级必须被拒绝，且不落库。
        ConfigSaveReqVO req = new ConfigSaveReqVO();
        req.setId(1L);
        req.setCategory("biz");
        req.setName("业务别名");
        req.setKey("biz.alias"); // 非秘密键
        req.setValue("******");  // 回显掩码，调用方并不掌握真值
        req.setVisible(false);
        when(configMapper.selectById(1L)).thenReturn(config("sys.db.password", "RealDbSecret", false));

        ServiceException ex = assertThrows(ServiceException.class, () -> configService.updateConfig(req));
        assertEquals(CONFIG_SENSITIVE_CAN_NOT_DOWNGRADE_ON_MASKED_ECHO.getCode(), ex.getCode(),
                "改名脱密（SECRET→SENSITIVE）且回传掩码保留真值时必须被拒绝，否则秘密可被两步洗白暴露");
        verify(configMapper, never()).updateById(any(ConfigDO.class));
    }

    @Test
    public void updateConfig_maskedEchoSetSensitiveVisible_shouldRejectDowngrade() {
        // ZS-CFG-001.B r1 P1 回归：SENSITIVE 项（非秘密键但 visible=false，库中藏真实敏感值）回传掩码并翻 visible=true，
        // 保护级由 SENSITIVE 降为 NORMAL，被保留的真值随即可读。现有 TOCTOU 守卫只拦 SECRET→visible，拦不住此路径。
        ConfigSaveReqVO req = new ConfigSaveReqVO();
        req.setId(2L);
        req.setCategory("biz");
        req.setName("业务配置");
        req.setKey("biz.internal.endpoint"); // 非秘密键
        req.setValue("******");
        req.setVisible(true); // 翻为可见 → 降为 NORMAL
        when(configMapper.selectById(2L)).thenReturn(config("biz.internal.endpoint", "RealSensitiveVal", false));

        ServiceException ex = assertThrows(ServiceException.class, () -> configService.updateConfig(req));
        assertEquals(CONFIG_SENSITIVE_CAN_NOT_DOWNGRADE_ON_MASKED_ECHO.getCode(), ex.getCode(),
                "SENSITIVE→NORMAL 且回传掩码保留真值时必须被拒绝，否则敏感值被暴露");
        verify(configMapper, never()).updateById(any(ConfigDO.class));
    }

}
