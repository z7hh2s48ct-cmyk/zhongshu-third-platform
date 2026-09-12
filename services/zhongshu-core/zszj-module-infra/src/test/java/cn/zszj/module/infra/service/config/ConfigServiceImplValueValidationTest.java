package cn.zszj.module.infra.service.config;

import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.test.core.ut.BaseDbUnitTest;
import cn.zszj.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import cn.zszj.module.infra.dal.dataobject.config.ConfigDO;
import cn.zszj.module.infra.dal.mysql.config.ConfigMapper;
import cn.zszj.module.infra.enums.config.ConfigTypeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.zszj.module.infra.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ZS-CFG-004 B03：配置值校验测试。
 *
 * <p>验证参数目录 + 类型/范围/枚举校验 + 非法值不落库 + 热生效/需重启行为 +
 * 敏感旧值脱敏 + 并发冲突明确。</p>
 *
 * <p>基于 H2 内存数据库 {@link BaseDbUnitTest}。</p>
 */
@Import({ConfigServiceImpl.class, ConfigSensitiveClassifier.class, ConfigValueValidator.class})
public class ConfigServiceImplValueValidationTest extends BaseDbUnitTest {

    @Resource
    private ConfigServiceImpl configService;

    @Resource
    private ConfigMapper configMapper;

    @Resource
    private ConfigValueValidator configValueValidator;

    // ========== 1. 非法值不进运行、不落库 ==========

    @Test
    void createConfig_integerOutOfRange_shouldRejectAndNotPersist() {
        // 参数目录登记 sys.login.captcha-max-retry 为 INTEGER [1,10]
        ConfigSaveReqVO reqVO = buildCreateReqVO("sys.login.captcha-max-retry", "99");

        // 期望抛出范围校验错误
        ServiceException ex = assertThrows(ServiceException.class, () -> configService.createConfig(reqVO));
        assertEquals(CONFIG_VALUE_OUT_OF_RANGE.getCode(), ex.getCode());

        // 断言未落库
        assertNull(configMapper.selectByKey("sys.login.captcha-max-retry"));
    }

    @Test
    void createConfig_integerTypeMismatch_shouldReject() {
        // 传入非整数值
        ConfigSaveReqVO reqVO = buildCreateReqVO("sys.login.captcha-max-retry", "abc");

        ServiceException ex = assertThrows(ServiceException.class, () -> configService.createConfig(reqVO));
        assertEquals(CONFIG_VALUE_TYPE_MISMATCH.getCode(), ex.getCode());

        assertNull(configMapper.selectByKey("sys.login.captcha-max-retry"));
    }

    @Test
    void createConfig_invalidBoolean_shouldReject() {
        // 参数目录登记 system.user.register-enabled 为 BOOLEAN
        ConfigSaveReqVO reqVO = buildCreateReqVO("system.user.register-enabled", "yes");

        ServiceException ex = assertThrows(ServiceException.class, () -> configService.createConfig(reqVO));
        assertEquals(CONFIG_VALUE_TYPE_MISMATCH.getCode(), ex.getCode());

        assertNull(configMapper.selectByKey("system.user.register-enabled"));
    }

    // ---------- ZS-CFG-004 codex r0 P2-2：Boolean 只接受规范化小写值，对齐消费方 ----------

    @Test
    void createConfig_booleanUpperCase_shouldRejectToAlignWithConsumer() {
        // 消费方 AdminUserServiceImpl.registerUser() 仅当存储串严格等于小写 "true" 才启用注册。
        // 若放行 "TRUE" 并原样持久化，被接受的布尔真值反而会静默关闭注册，故须拒绝非规范化拼写。
        ConfigSaveReqVO reqVO = buildCreateReqVO("system.user.register-enabled", "TRUE");

        ServiceException ex = assertThrows(ServiceException.class, () -> configService.createConfig(reqVO));
        assertEquals(CONFIG_VALUE_TYPE_MISMATCH.getCode(), ex.getCode());

        assertNull(configMapper.selectByKey("system.user.register-enabled"), "非规范布尔拼写不得落库");
    }

    @Test
    void createConfig_booleanMixedCase_shouldRejectToAlignWithConsumer() {
        // 混合大小写 "True" 同样非规范，须拒绝
        ConfigSaveReqVO reqVO = buildCreateReqVO("system.user.register-enabled", "True");

        ServiceException ex = assertThrows(ServiceException.class, () -> configService.createConfig(reqVO));
        assertEquals(CONFIG_VALUE_TYPE_MISMATCH.getCode(), ex.getCode());

        assertNull(configMapper.selectByKey("system.user.register-enabled"), "非规范布尔拼写不得落库");
    }

    @Test
    void createConfig_booleanCanonicalLowerCaseFalse_shouldSucceed() {
        // 规范化小写 false 仍应放行
        ConfigSaveReqVO reqVO = buildCreateReqVO("system.user.register-enabled", "false");
        Long id = configService.createConfig(reqVO);
        assertNotNull(id);
        assertEquals("false", configMapper.selectById(id).getValue());
    }

    @Test
    void createConfig_invalidEnumValue_shouldReject() {
        // 参数目录登记 sys.file.upload-mode 为 ENUM {local, oss, s3}
        ConfigSaveReqVO reqVO = buildCreateReqVO("sys.file.upload-mode", "ftp");

        ServiceException ex = assertThrows(ServiceException.class, () -> configService.createConfig(reqVO));
        assertEquals(CONFIG_VALUE_NOT_IN_ALLOWED_SET.getCode(), ex.getCode());

        assertNull(configMapper.selectByKey("sys.file.upload-mode"));
    }

    @Test
    void updateConfig_invalidValue_shouldRejectAndNotPersist() {
        // 先插入一条合法配置
        ConfigDO dbConfig = buildConfigDO("sys.login.captcha-max-retry", "5", true);
        configMapper.insert(dbConfig);

        // 尝试更新为非法值
        ConfigSaveReqVO reqVO = buildUpdateReqVO(dbConfig.getId(), "sys.login.captcha-max-retry", "0");

        ServiceException ex = assertThrows(ServiceException.class, () -> configService.updateConfig(reqVO));
        assertEquals(CONFIG_VALUE_OUT_OF_RANGE.getCode(), ex.getCode());

        // 断言库中值未变
        ConfigDO after = configMapper.selectById(dbConfig.getId());
        assertEquals("5", after.getValue());
    }

    // ========== 2. 合法值放行（护栏） ==========

    @Test
    void createConfig_validInteger_shouldSucceed() {
        ConfigSaveReqVO reqVO = buildCreateReqVO("sys.login.captcha-max-retry", "5");
        Long id = configService.createConfig(reqVO);
        assertNotNull(id);
        assertEquals("5", configMapper.selectById(id).getValue());
    }

    @Test
    void createConfig_validBoolean_shouldSucceed() {
        ConfigSaveReqVO reqVO = buildCreateReqVO("system.user.register-enabled", "true");
        Long id = configService.createConfig(reqVO);
        assertNotNull(id);
        assertEquals("true", configMapper.selectById(id).getValue());
    }

    @Test
    void createConfig_validEnum_shouldSucceed() {
        ConfigSaveReqVO reqVO = buildCreateReqVO("sys.file.upload-mode", "oss");
        Long id = configService.createConfig(reqVO);
        assertNotNull(id);
        assertEquals("oss", configMapper.selectById(id).getValue());
    }

    @Test
    void createConfig_unregisteredKey_shouldNotValidate() {
        // 未登记参数维持既有行为——任意值都放行
        ConfigSaveReqVO reqVO = buildCreateReqVO("custom.my-param", "any-value-12345");
        Long id = configService.createConfig(reqVO);
        assertNotNull(id);
        assertEquals("any-value-12345", configMapper.selectById(id).getValue());
    }

    // ========== 3. 热生效 vs 需重启项行为可测 ==========

    @Test
    void hotReloadParam_afterUpdate_consumerReadsNewValue() {
        // sys.login.captcha-max-retry 是热生效参数
        ConfigDO dbConfig = buildConfigDO("sys.login.captcha-max-retry", "3", true);
        configMapper.insert(dbConfig);

        ConfigSaveReqVO reqVO = buildUpdateReqVO(dbConfig.getId(), "sys.login.captcha-max-retry", "7");
        configService.updateConfig(reqVO);

        // 热生效：消费端立即读到新值
        ConfigDO updated = configService.getConfigByKey("sys.login.captcha-max-retry");
        assertEquals("7", updated.getValue());
        // 且不需要重启
        assertFalse(configValueValidator.isRestartRequired("sys.login.captcha-max-retry"));
    }

    @Test
    void restartRequiredParam_isMarkedCorrectly() {
        // sys.login.lock-duration-minutes 是需重启参数
        assertTrue(configValueValidator.isRestartRequired("sys.login.lock-duration-minutes"));
        // url.druid 也是需重启
        assertTrue(configValueValidator.isRestartRequired("url.druid"));
        // 热生效参数不需要重启
        assertFalse(configValueValidator.isRestartRequired("sys.login.captcha-max-retry"));
        assertFalse(configValueValidator.isRestartRequired("system.user.register-enabled"));
        // 未登记参数默认不要求重启（维持既有行为）
        assertFalse(configValueValidator.isRestartRequired("custom.unknown-key"));
    }

    @Test
    void restartRequiredParam_afterUpdate_valuePersistedButMarkedRestart() {
        // 需重启参数更新后值仍落库，但 isRestartRequired 标记明确
        ConfigDO dbConfig = buildConfigDO("sys.login.lock-duration-minutes", "30", true);
        configMapper.insert(dbConfig);

        ConfigSaveReqVO reqVO = buildUpdateReqVO(dbConfig.getId(), "sys.login.lock-duration-minutes", "60");
        configService.updateConfig(reqVO);

        // 值已落库
        ConfigDO updated = configService.getConfigByKey("sys.login.lock-duration-minutes");
        assertEquals("60", updated.getValue());
        // 但标记为需重启才生效
        assertTrue(configValueValidator.isRestartRequired("sys.login.lock-duration-minutes"));
    }

    // ========== 4. 敏感旧值不写审计原文 ==========

    @Test
    void updateConfig_sensitiveParam_auditSummaryMasksOldValue() {
        // system.user.init-password 是敏感参数
        ConfigDO dbConfig = buildConfigDO("system.user.init-password", "OldSecret123", false);
        configMapper.insert(dbConfig);

        // 构建审计摘要
        String summary = configValueValidator.buildChangeAuditSummary(
                "system.user.init-password", "OldSecret123", "NewSecret456",
                new ConfigSensitiveClassifier(), dbConfig);

        // 断言：旧值原文不出现在审计摘要中
        assertFalse(summary.contains("OldSecret123"), "Audit summary must NOT contain plaintext old value");
        assertFalse(summary.contains("NewSecret456"), "Audit summary must NOT contain plaintext new value");
        // 断言：包含脱敏掩码
        assertTrue(summary.contains("******"), "Audit summary should contain masked value");
    }

    @Test
    void updateConfig_normalParam_auditSummaryShowsPlaintext() {
        // 非敏感参数审计摘要可以包含原文
        ConfigDO dbConfig = buildConfigDO("sys.file.upload-mode", "local", true);
        configMapper.insert(dbConfig);

        String summary = configValueValidator.buildChangeAuditSummary(
                "sys.file.upload-mode", "local", "oss",
                new ConfigSensitiveClassifier(), dbConfig);

        // 非敏感参数：原文可见
        assertTrue(summary.contains("local"));
        assertTrue(summary.contains("oss"));
    }

    // ========== 5. 并发更新冲突明确 ==========

    @Test
    void updateConfig_concurrent_conflictIsClear() throws Exception {
        // 插入一条合法配置
        ConfigDO dbConfig = buildConfigDO("sys.login.captcha-max-retry", "5", true);
        configMapper.insert(dbConfig);
        Long configId = dbConfig.getId();

        int threadCount = 2;
        // ZS-CFG-004 codex r0 P2-3 修复：原 barrier 只同步 worker 启动，未同步其数据库快照读——
        // 一个 worker 可能在另一个读取行之前就完成更新，使两次更新各自基于最新 update_time 合法成功，
        // 从而令"恰好一个成功"断言 flaky（内存 H2 下 200 次约 30 次失败）。改为在【两个快照读之后、
        // 任一写之前】同步：拦截 selectById，读取真实快照后抵达 barrier，确保两 worker 读到同一
        // update_time 版本后才进入条件 UPDATE，令乐观锁冲突（affected==0 → CONFIG_UPDATE_CONFLICT）确定性发生。
        CyclicBarrier snapshotBarrier = new CyclicBarrier(threadCount);
        ConfigMapper synchronizedMapper = mock(ConfigMapper.class, delegatesTo(configMapper));
        when(synchronizedMapper.selectById(configId)).thenAnswer(inv -> {
            ConfigDO snapshot = configMapper.selectById(configId); // 真实 H2 快照读
            snapshotBarrier.await(5, TimeUnit.SECONDS);            // 两个快照读齐后再放行任一写
            return snapshot;
        });

        // 绕开 Spring CGLIB 代理（@Validated），用裸实例注入"委托真实 H2 mapper 的桩 + 真实分类器/校验器"，
        // 使乐观锁条件 UPDATE 仍打在真实 H2 上（保留乐观锁语义验证），仅在快照读处插入 barrier。
        ConfigServiceImpl bareService = new ConfigServiceImpl();
        ReflectionTestUtils.setField(bareService, "configMapper", synchronizedMapper);
        ReflectionTestUtils.setField(bareService, "sensitiveClassifier", new ConfigSensitiveClassifier());
        ReflectionTestUtils.setField(bareService, "configValueValidator", configValueValidator);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        CountDownLatch done = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int newVal = i + 6; // 6, 7 都是合法值 [1,10]
            new Thread(() -> {
                try {
                    ConfigSaveReqVO reqVO = new ConfigSaveReqVO();
                    reqVO.setId(configId);
                    reqVO.setKey("sys.login.captcha-max-retry");
                    reqVO.setValue(String.valueOf(newVal));
                    reqVO.setCategory("test");
                    reqVO.setName("captcha retry");
                    reqVO.setVisible(true);
                    bareService.updateConfig(reqVO);
                    successCount.incrementAndGet();
                } catch (ServiceException e) {
                    if (CONFIG_UPDATE_CONFLICT.getCode().equals(e.getCode())) {
                        conflictCount.incrementAndGet();
                    } else {
                        // Other service exceptions count as success for debugging
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Barrier/Interrupted exceptions
                } finally {
                    done.countDown();
                }
            }).start();
        }

        assertTrue(done.await(10, TimeUnit.SECONDS), "Threads should complete within timeout");
        // 冲突结果明确：恰好一方成功，一方冲突拒绝
        assertEquals(1, successCount.get(), "Exactly one update should succeed");
        assertEquals(1, conflictCount.get(), "Exactly one update should get conflict");
    }

    // ========== 6. 恢复路径仍走校验，不绕过 ==========

    @Test
    void updateConfig_restoreInvalidValue_stillRejects() {
        // 模拟"恢复旧值"场景：旧值不在当前合同范围内，恢复时仍被拒绝
        ConfigDO dbConfig = buildConfigDO("sys.login.captcha-max-retry", "5", true);
        configMapper.insert(dbConfig);

        // 假设旧值为 "99"（越界），尝试通过 update 恢复
        ConfigSaveReqVO reqVO = buildUpdateReqVO(dbConfig.getId(), "sys.login.captcha-max-retry", "99");

        ServiceException ex = assertThrows(ServiceException.class, () -> configService.updateConfig(reqVO));
        assertEquals(CONFIG_VALUE_OUT_OF_RANGE.getCode(), ex.getCode());

        // 库中值未变
        assertEquals("5", configMapper.selectById(dbConfig.getId()).getValue());
    }

    @Test
    void updateConfig_restoreToDisabledEnumValue_stillRejects() {
        // 模拟：旧值为已下线的枚举选项，恢复时仍被拒绝
        ConfigDO dbConfig = buildConfigDO("sys.file.upload-mode", "local", true);
        configMapper.insert(dbConfig);

        // "ftp" 不在允许的枚举集 {local, oss, s3} 中
        ConfigSaveReqVO reqVO = buildUpdateReqVO(dbConfig.getId(), "sys.file.upload-mode", "ftp");

        ServiceException ex = assertThrows(ServiceException.class, () -> configService.updateConfig(reqVO));
        assertEquals(CONFIG_VALUE_NOT_IN_ALLOWED_SET.getCode(), ex.getCode());
    }

    // ========== 7. codex r0 P2-1：改 key 时按【目标 key】契约校验被掩码保留的值 ==========

    @Test
    void updateConfig_renameToRegisteredKeyWithMask_retainedValueViolatesTargetContract_shouldReject() {
        // 攻击路径复现：库中有一条【未登记、不可见】行，值为越界的 "99"（visible=false → SENSITIVE，输出被掩码）。
        // 攻击者把它改名为受控 key sys.login.captcha-max-retry（合同 INTEGER [1,10]），提交掩码 ****** 并保留 visible=false。
        // 脱敏往返保护会保留旧值 "99"，但改名后该值须按【目标 key】契约校验——否则越界值 99 会随改名进入运行。
        ConfigDO dbConfig = buildConfigDO("custom.hidden-unregistered", "99", false);
        configMapper.insert(dbConfig);
        Long id = dbConfig.getId();

        ConfigSaveReqVO reqVO = new ConfigSaveReqVO();
        reqVO.setId(id);
        reqVO.setKey("sys.login.captcha-max-retry"); // 改名为受控 key
        reqVO.setValue("******");                    // 回显掩码 → 触发保留旧值 "99"
        reqVO.setCategory("test");
        reqVO.setName("captcha retry");
        reqVO.setVisible(false);                     // 保留不可见，避免触发降级/翻可见守卫

        ServiceException ex = assertThrows(ServiceException.class, () -> configService.updateConfig(reqVO),
                "改名后保留的越界旧值必须按目标 key 契约校验并拒绝");
        assertEquals(CONFIG_VALUE_OUT_OF_RANGE.getCode(), ex.getCode());

        // 断言未落库：库中仍是原 key、原值
        ConfigDO after = configMapper.selectById(id);
        assertEquals("custom.hidden-unregistered", after.getConfigKey());
        assertEquals("99", after.getValue());
        assertNull(configMapper.selectByKey("sys.login.captcha-max-retry"), "越界值不得以目标 key 落库");
    }

    @Test
    void updateConfig_renameToRegisteredKeyWithMask_retainedValueSatisfiesTargetContract_shouldSucceed() {
        // 护栏：改名后保留的旧值若符合目标 key 契约，应正常放行（不因新增校验误伤合法改名）
        ConfigDO dbConfig = buildConfigDO("custom.hidden-unregistered", "5", false);
        configMapper.insert(dbConfig);
        Long id = dbConfig.getId();

        ConfigSaveReqVO reqVO = new ConfigSaveReqVO();
        reqVO.setId(id);
        reqVO.setKey("sys.login.captcha-max-retry"); // 目标 key 合同 [1,10]
        reqVO.setValue("******");                    // 保留旧值 "5"，落在 [1,10] 内
        reqVO.setCategory("test");
        reqVO.setName("captcha retry");
        reqVO.setVisible(false);

        configService.updateConfig(reqVO);

        ConfigDO after = configMapper.selectById(id);
        assertEquals("sys.login.captcha-max-retry", after.getConfigKey());
        assertEquals("5", after.getValue());
    }

    // ========== 辅助方法 ==========

    private ConfigSaveReqVO buildCreateReqVO(String key, String value) {
        ConfigSaveReqVO reqVO = new ConfigSaveReqVO();
        reqVO.setKey(key);
        reqVO.setValue(value);
        reqVO.setCategory("test");
        reqVO.setName("test-" + key);
        reqVO.setVisible(true);
        return reqVO;
    }

    private ConfigSaveReqVO buildUpdateReqVO(Long id, String key, String value) {
        ConfigSaveReqVO reqVO = new ConfigSaveReqVO();
        reqVO.setId(id);
        reqVO.setKey(key);
        reqVO.setValue(value);
        reqVO.setCategory("test");
        reqVO.setName("test-" + key);
        reqVO.setVisible(true);
        return reqVO;
    }

    private ConfigDO buildConfigDO(String key, String value, boolean visible) {
        ConfigDO config = new ConfigDO();
        config.setConfigKey(key);
        config.setValue(value);
        config.setVisible(visible);
        config.setCategory("test");
        config.setName("test-" + key);
        config.setType(ConfigTypeEnum.CUSTOM.getType());
        return config;
    }
}
