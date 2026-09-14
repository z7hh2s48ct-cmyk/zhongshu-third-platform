package cn.zszj.module.infra.service.config;

import cn.zszj.framework.common.biz.system.audit.AuditEventMessage;
import cn.zszj.framework.common.biz.system.audit.AuditEventTypes;
import cn.zszj.framework.common.biz.system.audit.AuditPort;
import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.module.infra.controller.admin.config.vo.ConfigChangeHistoryPageReqVO;
import cn.zszj.module.infra.controller.admin.config.vo.ConfigChangeHistoryRespVO;
import cn.zszj.module.infra.controller.admin.config.vo.ConfigRestoreReqVO;
import cn.zszj.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import cn.zszj.module.infra.dal.dataobject.config.ConfigChangeHistoryDO;
import cn.zszj.module.infra.dal.dataobject.config.ConfigDO;
import cn.zszj.module.infra.dal.mysql.config.ConfigChangeHistoryMapper;
import cn.zszj.module.infra.dal.mysql.config.ConfigMapper;
import cn.zszj.module.infra.enums.config.ConfigTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Comparator;
import java.util.List;

import static cn.zszj.module.infra.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * ZS-CFG-004 B04：配置变更审计（接线 AuditPort）与审查后恢复流程测试。
 *
 * <p>覆盖：变更历史落库（敏感值脱敏）、SUCCESS 审计（脱敏摘要 + fail-closed 回滚）、
 * 恢复正向流程、乐观锁冲突、敏感历史拒绝（DENIED 留痕）、跨配置恢复拒绝、恢复值再校验、
 * 已删除配置不可复活、恢复仅回写 value、拒绝审计失败不阻断业务拒绝、历史分页掩码输出。</p>
 *
 * <p>基于 H2 内存数据库 {@link BaseDbUnitTest}；{@link AuditPort} 以 Mockito Bean 替身（真实实现
 * JdbcAuditPort 落 system 模块，其事务语义已在 ZS-AUDIT-001 的 JdbcAuditPortTest 验证）。</p>
 */
@Import({ConfigServiceImpl.class, ConfigSensitiveClassifier.class, ConfigValueValidator.class,
        ConfigChangeRecorder.class})
public class ConfigChangeAuditRestoreTest extends BaseDbUnitTest {

    @Resource
    private ConfigServiceImpl configService;

    @Resource
    private ConfigMapper configMapper;

    @Resource
    private ConfigChangeHistoryMapper configChangeHistoryMapper;

    @MockitoBean
    private AuditPort auditPort;

    // ========== 1. 变更留痕：历史行 + SUCCESS 审计（脱敏摘要） ==========

    @Test
    void updateConfig_normalKey_writesPlaintextHistoryAndAudit() {
        ConfigDO config = insertConfig("url.druid", "http://v1", true, 3);

        configService.updateConfig(buildUpdateReqVO(config, "http://v2", 3));

        // 历史行：前后值明文（NORMAL 不脱敏）、前后版本、显式脱敏标志为 FALSE
        ConfigChangeHistoryDO history = latestHistory(config.getId());
        assertEquals(ConfigChangeHistoryDO.TYPE_UPDATE, history.getChangeType());
        assertEquals("http://v1", history.getOldValue());
        assertEquals("http://v2", history.getNewValue());
        assertEquals(3, history.getOldVersion());
        assertEquals(4, history.getNewVersion());
        assertEquals(Boolean.FALSE, history.getOldValueRedacted());
        assertEquals(Boolean.FALSE, history.getNewValueRedacted());
        // 审计：OBJECT_UPDATED + SUCCESS + 脱敏摘要（NORMAL 记原值摘要）
        ArgumentCaptor<AuditEventMessage> captor = ArgumentCaptor.forClass(AuditEventMessage.class);
        verify(auditPort).record(captor.capture());
        AuditEventMessage message = captor.getValue();
        assertEquals(AuditEventTypes.OBJECT_UPDATED, message.getEventType());
        assertEquals(AuditEventMessage.AuditResult.SUCCESS, message.getResult());
        assertEquals("infra_config", message.getBizType());
        assertEquals(String.valueOf(config.getId()), message.getBizId());
        assertEquals("4", message.getBizVersion());
        assertEquals("url.druid", message.getDetail().get("key"));
        assertEquals("http://v1", message.getDetail().get("oldValue"));
        assertEquals("http://v2", message.getDetail().get("newValue"));
    }

    @Test
    void updateConfig_secretKey_masksHistoryAndAuditDetail() {
        ConfigDO config = insertConfig("system.user.init-password", "old-secret", false, 1);

        configService.updateConfig(buildUpdateReqVO(config, "new-secret", 1));

        // 历史行只落掩码，明文不落历史（敏感旧值不写审计原文）；显式脱敏标志为 TRUE
        ConfigChangeHistoryDO history = latestHistory(config.getId());
        assertEquals(ConfigSensitiveClassifier.MASK_VALUE, history.getOldValue());
        assertEquals(ConfigSensitiveClassifier.MASK_VALUE, history.getNewValue());
        assertEquals(Boolean.TRUE, history.getOldValueRedacted());
        assertEquals(Boolean.TRUE, history.getNewValueRedacted());
        // 审计明细只装掩码摘要
        ArgumentCaptor<AuditEventMessage> captor = ArgumentCaptor.forClass(AuditEventMessage.class);
        verify(auditPort).record(captor.capture());
        AuditEventMessage message = captor.getValue();
        assertEquals(ConfigSensitiveClassifier.MASK_VALUE, message.getDetail().get("oldValue"));
        assertEquals(ConfigSensitiveClassifier.MASK_VALUE, message.getDetail().get("newValue"));
        assertFalse(message.getDetail().containsValue("old-secret"), "审计明细不得携带敏感明文");
        assertFalse(message.getDetail().containsValue("new-secret"), "审计明细不得携带敏感明文");
    }

    @Test
    void createConfig_writesCreateHistoryAndAudit() {
        ConfigSaveReqVO reqVO = new ConfigSaveReqVO();
        reqVO.setCategory("test");
        reqVO.setName("测试参数");
        reqVO.setKey("test.custom.key");
        reqVO.setValue("custom-value");
        reqVO.setVisible(true);

        Long id = configService.createConfig(reqVO);

        ConfigChangeHistoryDO history = latestHistory(id);
        assertEquals(ConfigChangeHistoryDO.TYPE_CREATE, history.getChangeType());
        assertNull(history.getOldValue());
        assertEquals("custom-value", history.getNewValue());
        assertEquals(0, history.getNewVersion());
        ArgumentCaptor<AuditEventMessage> captor = ArgumentCaptor.forClass(AuditEventMessage.class);
        verify(auditPort).record(captor.capture());
        assertEquals(AuditEventTypes.OBJECT_CREATED, captor.getValue().getEventType());
    }

    @Test
    void deleteConfig_writesDeleteHistoryAndAudit() {
        ConfigDO config = insertConfig("test.delete.key", "to-delete", true, 5);

        configService.deleteConfig(config.getId());

        ConfigChangeHistoryDO history = latestHistory(config.getId());
        assertEquals(ConfigChangeHistoryDO.TYPE_DELETE, history.getChangeType());
        assertEquals("to-delete", history.getOldValue());
        assertNull(history.getNewValue());
        ArgumentCaptor<AuditEventMessage> captor = ArgumentCaptor.forClass(AuditEventMessage.class);
        verify(auditPort).record(captor.capture());
        assertEquals(AuditEventTypes.OBJECT_DELETED, captor.getValue().getEventType());
    }

    // ========== 2. SUCCESS 审计 fail-closed：审计失败则业务整体回滚 ==========

    @Test
    void updateConfig_auditWriteFailure_rollsBackBusiness() {
        ConfigDO config = insertConfig("test.audit.fail.key", "before", true, 1);
        doThrow(new IllegalStateException("audit down")).when(auditPort).record(
                argThat(message -> message.getResult() == AuditEventMessage.AuditResult.SUCCESS));

        assertThrows(IllegalStateException.class,
                () -> configService.updateConfig(buildUpdateReqVO(config, "after", 1)));

        // 业务与历史整体回滚：不允许「改了配置却无成功审计」
        assertEquals("before", configMapper.selectById(config.getId()).getValue());
        assertEquals(1, configMapper.selectById(config.getId()).getVersion());
        assertTrue(configChangeHistoryMapper.selectList(
                ConfigChangeHistoryDO::getConfigId, config.getId()).isEmpty());
    }

    // ========== 3. 恢复流程：正向 + 全部拒绝路径 ==========

    @Test
    void restoreConfig_happyPath_restoresValueAndAudits() {
        ConfigDO config = insertConfig("system.user.register-enabled", "true", true, 2);
        // 先产生一次真实变更：true → false（历史行 h1：old=true）
        configService.updateConfig(buildUpdateReqVO(config, "false", 2));
        ConfigDO updated = configMapper.selectById(config.getId());
        assertEquals(3, updated.getVersion());
        ConfigChangeHistoryDO target = latestHistory(config.getId());

        ConfigRestoreReqVO reqVO = new ConfigRestoreReqVO();
        reqVO.setId(config.getId());
        reqVO.setHistoryId(target.getId());
        reqVO.setVersion(3);
        reqVO.setReason("审批单-2026-001：回滚误关的注册开关");
        configService.restoreConfig(reqVO);

        // 值回到变更前，版本继续 +1（乐观锁链不重置）
        ConfigDO restored = configMapper.selectById(config.getId());
        assertEquals("true", restored.getValue());
        assertEquals(4, restored.getVersion());
        // 恢复历史行 + CONFIG_PARAM_RESTORED 审计
        ConfigChangeHistoryDO history = latestHistory(config.getId());
        assertEquals(ConfigChangeHistoryDO.TYPE_RESTORE, history.getChangeType());
        assertEquals("false", history.getOldValue());
        assertEquals("true", history.getNewValue());
        assertEquals("审批单-2026-001：回滚误关的注册开关", history.getReason());
        ArgumentCaptor<AuditEventMessage> captor = ArgumentCaptor.forClass(AuditEventMessage.class);
        verify(auditPort, org.mockito.Mockito.times(2)).record(captor.capture());
        AuditEventMessage restoreAudit = captor.getAllValues().get(1);
        assertEquals(AuditEventTypes.CONFIG_PARAM_RESTORED, restoreAudit.getEventType());
        assertEquals(AuditEventMessage.AuditResult.SUCCESS, restoreAudit.getResult());
        assertEquals("审批单-2026-001：回滚误关的注册开关", restoreAudit.getReason());
    }

    @Test
    void restoreConfig_staleVersion_rejectedAsConflict() {
        ConfigDO config = insertConfig("test.restore.stale", "v1", true, 1);
        configService.updateConfig(buildUpdateReqVO(config, "v2", 1));
        ConfigChangeHistoryDO target = latestHistory(config.getId());

        ConfigRestoreReqVO reqVO = new ConfigRestoreReqVO();
        reqVO.setId(config.getId());
        reqVO.setHistoryId(target.getId());
        reqVO.setVersion(1); // 期间已被 update 推进到 2，携带旧版本=盲写
        reqVO.setReason("过期快照恢复");
        ServiceException ex = assertThrows(ServiceException.class, () -> configService.restoreConfig(reqVO));

        assertEquals(CONFIG_UPDATE_CONFLICT.getCode(), ex.getCode());
        assertEquals("v2", configMapper.selectById(config.getId()).getValue());
        // 拒绝路径不产生恢复历史与恢复审计（仅先前 update 的一条）
        assertEquals(1, configChangeHistoryMapper.selectList(
                ConfigChangeHistoryDO::getConfigId, config.getId()).size());
        verify(auditPort, org.mockito.Mockito.times(1)).record(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void restoreConfig_normalLiteralMaskValue_restorable() {
        // r0 P2：NORMAL 配置的字面 ****** 与脱敏哨兵同形，可恢复性以显式标志判定，不做值形推断
        ConfigDO config = insertConfig("test.restore.literal-mask", "******", true, 1);
        configService.updateConfig(buildUpdateReqVO(config, "changed", 1));
        assertEquals("******", latestHistory(config.getId()).getOldValue());
        assertEquals(Boolean.FALSE, latestHistory(config.getId()).getOldValueRedacted());

        ConfigRestoreReqVO reqVO = new ConfigRestoreReqVO();
        reqVO.setId(config.getId());
        reqVO.setHistoryId(latestHistory(config.getId()).getId());
        reqVO.setVersion(2);
        reqVO.setReason("恢复 NORMAL 配置的字面 ****** 值");
        configService.restoreConfig(reqVO);

        assertEquals("******", configMapper.selectById(config.getId()).getValue());
        assertEquals(3, configMapper.selectById(config.getId()).getVersion());
    }

    @Test
    void restoreConfig_secretHistory_refusedWithDeniedAudit() {
        ConfigDO config = insertConfig("system.user.init-password", "real-secret", false, 1);
        configService.updateConfig(buildUpdateReqVO(config, "rotated-secret", 1));
        ConfigChangeHistoryDO target = latestHistory(config.getId());

        ConfigRestoreReqVO reqVO = new ConfigRestoreReqVO();
        reqVO.setId(config.getId());
        reqVO.setHistoryId(target.getId());
        reqVO.setVersion(2);
        reqVO.setReason("尝试恢复秘密历史");
        ServiceException ex = assertThrows(ServiceException.class, () -> configService.restoreConfig(reqVO));

        assertEquals(CONFIG_RESTORE_NOT_RESTORABLE.getCode(), ex.getCode());
        assertEquals("rotated-secret", configMapper.selectById(config.getId()).getValue());
        // DENIED 独立留痕：拒绝主体与原因可追查
        ArgumentCaptor<AuditEventMessage> captor = ArgumentCaptor.forClass(AuditEventMessage.class);
        verify(auditPort, org.mockito.Mockito.times(2)).record(captor.capture());
        AuditEventMessage denied = captor.getAllValues().get(1);
        assertEquals(AuditEventTypes.ACCESS_DENIED, denied.getEventType());
        assertEquals(AuditEventMessage.AuditResult.DENIED, denied.getResult());
        assertFalse(denied.getReason().contains("real-secret"), "拒绝原因不得携带敏感明文");
    }

    @Test
    void restoreConfig_createHistory_refused() {
        ConfigSaveReqVO createReqVO = new ConfigSaveReqVO();
        createReqVO.setCategory("test");
        createReqVO.setName("创建型历史");
        createReqVO.setKey("test.restore.create-history");
        createReqVO.setValue("v1");
        createReqVO.setVisible(true);
        Long id = configService.createConfig(createReqVO);
        ConfigChangeHistoryDO createHistory = latestHistory(id);

        ConfigRestoreReqVO reqVO = new ConfigRestoreReqVO();
        reqVO.setId(id);
        reqVO.setHistoryId(createHistory.getId());
        reqVO.setVersion(0);
        reqVO.setReason("CREATE 行无可恢复的变更前值");
        ServiceException ex = assertThrows(ServiceException.class, () -> configService.restoreConfig(reqVO));

        assertEquals(CONFIG_RESTORE_NOT_RESTORABLE.getCode(), ex.getCode());
    }

    @Test
    void restoreConfig_historyOfAnotherConfig_refused() {
        ConfigDO configA = insertConfig("test.restore.a", "a1", true, 1);
        ConfigDO configB = insertConfig("test.restore.b", "b1", true, 1);
        configService.updateConfig(buildUpdateReqVO(configA, "a2", 1));
        ConfigChangeHistoryDO historyOfA = latestHistory(configA.getId());

        ConfigRestoreReqVO reqVO = new ConfigRestoreReqVO();
        reqVO.setId(configB.getId());
        reqVO.setHistoryId(historyOfA.getId());
        reqVO.setVersion(1);
        reqVO.setReason("跨配置恢复探测");
        ServiceException ex = assertThrows(ServiceException.class, () -> configService.restoreConfig(reqVO));

        assertEquals(CONFIG_RESTORE_HISTORY_MISMATCH.getCode(), ex.getCode());
        assertEquals("b1", configMapper.selectById(configB.getId()).getValue());
    }

    @Test
    void restoreConfig_illegalStoredValue_rejectedByValidator() {
        // 模拟历史脏数据：受控 key 的历史 old_value 违反参数目录合同（captcha-max-retry 限定 [1,10]）
        ConfigDO config = insertConfig("sys.login.captcha-max-retry", "5", true, 4);
        ConfigChangeHistoryDO dirty = new ConfigChangeHistoryDO();
        dirty.setConfigId(config.getId());
        dirty.setConfigKey("sys.login.captcha-max-retry");
        dirty.setChangeType(ConfigChangeHistoryDO.TYPE_UPDATE);
        dirty.setOldValue("99");
        dirty.setNewValue("5");
        dirty.setOldVersion(3);
        dirty.setNewVersion(4);
        configChangeHistoryMapper.insert(dirty);

        ConfigRestoreReqVO reqVO = new ConfigRestoreReqVO();
        reqVO.setId(config.getId());
        reqVO.setHistoryId(dirty.getId());
        reqVO.setVersion(4);
        reqVO.setReason("恢复越界历史值");
        ServiceException ex = assertThrows(ServiceException.class, () -> configService.restoreConfig(reqVO));

        // 恢复值与更新走同一值校验路径——非法值不进入运行
        assertEquals(CONFIG_VALUE_OUT_OF_RANGE.getCode(), ex.getCode());
        assertEquals("5", configMapper.selectById(config.getId()).getValue());
    }

    @Test
    void restoreConfig_deletedConfig_notResurrected() {
        ConfigSaveReqVO createReqVO = new ConfigSaveReqVO();
        createReqVO.setCategory("test");
        createReqVO.setName("待删除");
        createReqVO.setKey("test.restore.deleted");
        createReqVO.setValue("v1");
        createReqVO.setVisible(true);
        Long id = configService.createConfig(createReqVO);
        configService.deleteConfig(id);

        ConfigRestoreReqVO reqVO = new ConfigRestoreReqVO();
        reqVO.setId(id);
        reqVO.setHistoryId(latestHistory(id).getId());
        reqVO.setVersion(0);
        reqVO.setReason("已删除配置不得借恢复复活");
        ServiceException ex = assertThrows(ServiceException.class, () -> configService.restoreConfig(reqVO));

        assertEquals(CONFIG_NOT_EXISTS.getCode(), ex.getCode());
        // 不插入新行：恢复结构性排除「复活」路径（模块启停归 ModuleCatalog 构建期白名单，配置不可触碰）
        assertNull(configMapper.selectByKey("test.restore.deleted"));
    }

    @Test
    void restoreConfig_onlyValueRewritten_keyAndVisibleUntouched() {
        ConfigDO config = insertConfig("test.restore.scope", "v1", true, 2);
        ConfigSaveReqVO updateReqVO = buildUpdateReqVO(config, "v2", 2);
        updateReqVO.setName("改名后的名称");
        configService.updateConfig(updateReqVO);
        ConfigChangeHistoryDO target = latestHistory(config.getId());

        ConfigRestoreReqVO reqVO = new ConfigRestoreReqVO();
        reqVO.setId(config.getId());
        reqVO.setHistoryId(target.getId());
        reqVO.setVersion(3);
        reqVO.setReason("仅回写值");
        configService.restoreConfig(reqVO);

        ConfigDO restored = configMapper.selectById(config.getId());
        assertEquals("v1", restored.getValue());
        assertEquals("test.restore.scope", restored.getConfigKey());
        assertEquals(Boolean.TRUE, restored.getVisible());
        assertEquals("改名后的名称", restored.getName(), "恢复仅回写 value，不回滚 key/名称/可见性等元数据");
    }

    @Test
    void restoreConfig_deniedAuditFailure_doesNotMaskBusinessRejection() {
        ConfigDO config = insertConfig("system.user.init-password", "real-secret", false, 1);
        configService.updateConfig(buildUpdateReqVO(config, "rotated-secret", 1));
        ConfigChangeHistoryDO target = latestHistory(config.getId());
        doThrow(new IllegalStateException("audit down")).when(auditPort).record(
                argThat(message -> message.getResult() == AuditEventMessage.AuditResult.DENIED));

        ConfigRestoreReqVO reqVO = new ConfigRestoreReqVO();
        reqVO.setId(config.getId());
        reqVO.setHistoryId(target.getId());
        reqVO.setVersion(2);
        reqVO.setReason("拒绝审计宕机时业务拒绝仍须原样返回");
        ServiceException ex = assertThrows(ServiceException.class, () -> configService.restoreConfig(reqVO));

        assertEquals(CONFIG_RESTORE_NOT_RESTORABLE.getCode(), ex.getCode(), "业务拒绝不得被审计失败变形");
    }

    // ========== 4. 变更历史查询：敏感配置整页掩码 ==========

    @Test
    void getConfigChangeHistoryPage_masksSensitiveConfigOutput() {
        ConfigDO secret = insertConfig("system.user.init-password", "real-secret", false, 1);
        configService.updateConfig(buildUpdateReqVO(secret, "rotated-secret", 1));
        ConfigDO normal = insertConfig("url.druid", "http://v1", true, 1);
        configService.updateConfig(buildUpdateReqVO(normal, "http://v2", 1));

        PageResult<ConfigChangeHistoryRespVO> secretPage = configService.getConfigChangeHistoryPage(
                buildHistoryPageReq(secret.getId()));
        PageResult<ConfigChangeHistoryRespVO> normalPage = configService.getConfigChangeHistoryPage(
                buildHistoryPageReq(normal.getId()));

        // 敏感配置整页掩码（含 NORMAL 期明文入史后改密的行）；普通配置原样输出
        secretPage.getList().forEach(vo -> {
            if (vo.getOldValue() != null) {
                assertEquals(ConfigSensitiveClassifier.MASK_VALUE, vo.getOldValue());
            }
            if (vo.getNewValue() != null) {
                assertEquals(ConfigSensitiveClassifier.MASK_VALUE, vo.getNewValue());
            }
        });
        ConfigChangeHistoryRespVO normalRow = normalPage.getList().get(0);
        assertEquals("http://v1", normalRow.getOldValue());
        assertEquals("http://v2", normalRow.getNewValue());
    }

    @Test
    void getConfigChangeHistoryPage_configNotExists_rejected() {
        ConfigChangeHistoryPageReqVO reqVO = buildHistoryPageReq(999999L);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> configService.getConfigChangeHistoryPage(reqVO));
        assertEquals(CONFIG_NOT_EXISTS.getCode(), ex.getCode());
    }

    // ========== 夹具 ==========

    private ConfigDO insertConfig(String key, String value, boolean visible, int version) {
        ConfigDO config = new ConfigDO();
        config.setCategory("test");
        config.setType(ConfigTypeEnum.CUSTOM.getType());
        config.setName(key);
        config.setConfigKey(key);
        config.setValue(value);
        config.setVisible(visible);
        config.setVersion(version);
        configMapper.insert(config);
        return configMapper.selectByKey(key);
    }

    private ConfigSaveReqVO buildUpdateReqVO(ConfigDO config, String newValue, int version) {
        ConfigSaveReqVO reqVO = new ConfigSaveReqVO();
        reqVO.setId(config.getId());
        reqVO.setCategory(config.getCategory());
        reqVO.setName(config.getName());
        reqVO.setKey(config.getConfigKey());
        reqVO.setValue(newValue);
        reqVO.setVisible(config.getVisible());
        reqVO.setVersion(version);
        return reqVO;
    }

    private ConfigChangeHistoryPageReqVO buildHistoryPageReq(Long configId) {
        ConfigChangeHistoryPageReqVO reqVO = new ConfigChangeHistoryPageReqVO();
        reqVO.setConfigId(configId);
        reqVO.setPageSize(100);
        return reqVO;
    }

    private ConfigChangeHistoryDO latestHistory(Long configId) {
        List<ConfigChangeHistoryDO> list = configChangeHistoryMapper.selectList(
                ConfigChangeHistoryDO::getConfigId, configId);
        return list.stream().max(Comparator.comparing(ConfigChangeHistoryDO::getId)).orElseThrow();
    }

}
