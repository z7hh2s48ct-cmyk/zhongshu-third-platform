package cn.zszj.module.infra.service.config;

import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.module.infra.controller.admin.config.vo.ConfigChangeHistoryPageReqVO;
import cn.zszj.module.infra.controller.admin.config.vo.ConfigChangeHistoryRespVO;
import cn.zszj.module.infra.controller.admin.config.vo.ConfigPageReqVO;
import cn.zszj.module.infra.controller.admin.config.vo.ConfigRespVO;
import cn.zszj.module.infra.controller.admin.config.vo.ConfigRestoreReqVO;
import cn.zszj.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import cn.zszj.module.infra.convert.config.ConfigConvert;
import cn.zszj.module.infra.dal.dataobject.config.ConfigChangeHistoryDO;
import cn.zszj.module.infra.dal.dataobject.config.ConfigDO;
import cn.zszj.module.infra.dal.mysql.config.ConfigChangeHistoryMapper;
import cn.zszj.module.infra.dal.mysql.config.ConfigMapper;
import cn.zszj.module.infra.enums.config.ConfigTypeEnum;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.annotations.VisibleForTesting;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.*;

/**
 * 参数配置 Service 实现类
 */
@Service
@Slf4j
@Validated
public class ConfigServiceImpl implements ConfigService {

    @Resource
    private ConfigMapper configMapper;

    @Resource
    private ConfigSensitiveClassifier sensitiveClassifier;

    @Resource
    private ConfigValueValidator configValueValidator;

    @Resource
    private ConfigChangeHistoryMapper configChangeHistoryMapper;

    @Resource
    private ConfigChangeRecorder configChangeRecorder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createConfig(ConfigSaveReqVO createReqVO) {
        // 校验参数配置 key 的唯一性
        validateConfigKeyUnique(null, createReqVO.getKey());

        // ZS-CFG-004 B03：值校验——按参数目录合同校验类型/范围/枚举
        configValueValidator.validate(createReqVO.getKey(), createReqVO.getValue());

        // 插入参数配置
        ConfigDO config = ConfigConvert.INSTANCE.convert(createReqVO);
        config.setType(ConfigTypeEnum.CUSTOM.getType());
        // ZS-CFG-004 codex r2 P2 修复：版本由服务端初始化，忽略请求携带值（MapStruct 会自动映射同名 version 字段）
        config.setVersion(0);
        configMapper.insert(config);
        // ZS-CFG-004 B04：变更留痕——历史行 + SUCCESS 审计随本事务提交（审计失败则整体回滚）
        configChangeRecorder.recordCreate(config);
        return config.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(ConfigSaveReqVO updateReqVO) {
        // 校验自己存在
        ConfigDO exists = validateConfigExists(updateReqVO.getId());
        // 校验参数配置 key 的唯一性
        validateConfigKeyUnique(updateReqVO.getId(), updateReqVO.getKey());

        // ZS-CFG-001.B：秘密键（无论新旧 key）禁止置为 visible，堵住"改 key 成秘密模式 + 翻可见"的 TOCTOU 旁路
        ConfigDO updateObj = ConfigConvert.INSTANCE.convert(updateReqVO);
        boolean wasSecret = sensitiveClassifier.classify(exists) == ConfigSensitiveClassifier.SensitiveLevel.SECRET;
        boolean nowSecret = sensitiveClassifier.classify(updateObj) == ConfigSensitiveClassifier.SensitiveLevel.SECRET;
        boolean toVisible = Boolean.TRUE.equals(updateReqVO.getVisible());
        if ((wasSecret || nowSecret) && toVisible) {
            throw exception(CONFIG_SENSITIVE_CAN_NOT_SET_VISIBLE);
        }

        // ZS-CFG-001.B r0 P1 修复：脱敏往返保护——敏感项详情/分页/导出输出被掩码为 ******，前端 ConfigForm.vue
        // 仅编辑名称/备注后会把掩码原样回传；若直接持久化会用掩码覆盖库中真实秘密值（如 system.user.init-password），
        // 造成数据损坏。故提交值为掩码哨兵且库中项为敏感级时保留原值（管理员改真值时提交新值、非哨兵，不受影响）。
        boolean maskedEcho = sensitiveClassifier.isMaskedEcho(exists, updateObj.getValue());
        if (maskedEcho) {
            // ZS-CFG-001.B r1 P1 修复：回传掩码=调用方不掌握真值，禁止在同一更新里下调该值保护级，否则可两步洗密
            // （SECRET 改名脱密降 SENSITIVE → 翻 visible 降 NORMAL）后经 /get-value-by-key 与详情读出明文；现有
            // TOCTOU 守卫只拦 SECRET→visible，拦不住改名降级链，故此处补齐。
            if (sensitiveClassifier.isProtectionDowngrade(exists, updateObj)) {
                throw exception(CONFIG_SENSITIVE_CAN_NOT_DOWNGRADE_ON_MASKED_ECHO);
            }
            updateObj.setValue(exists.getValue());
        }

        // ZS-CFG-004 B03：值校验——按参数目录合同校验类型/范围/枚举。
        // 掩码回显且 key 未变时保留原值、不重复校验（原值此前已按其自身 key 校验过）。
        // ZS-CFG-004 codex r0 P2-1 修复：一旦 key 发生变化，被恢复/保留的值须按【目标 key】的合同重新校验，
        // 堵住"把未登记、不可见行（如 value=99）改名为受控 key（如 sys.login.captcha-max-retry，[1,10]）+
        // 提交掩码 ****** 保留越界旧值"从而绕过校验、令非法值进入运行的旁路（违背 CFG-004"非法值不进入运行"验收）。
        boolean keyChanged = !Objects.equals(exists.getConfigKey(), updateReqVO.getKey());
        if (!maskedEcho || keyChanged) {
            configValueValidator.validate(updateReqVO.getKey(), updateObj.getValue());
        }

        // ZS-CFG-004 codex r1 P1 修复 + codex r2 P1 改型：乐观锁版本改为【独立整数 version 列】——
        // r1 曾以客户端回传 updateTime 作版本，r2 评审证明其根本缺陷：JSON 时间序列化丢毫秒以下精度
        // （微秒版本回传即冲突拒绝）、datetime 秒级精度下同秒内两次更新版本不推进（旧表单仍可覆盖）、
        // 创建路径 MapStruct 自动映射可伪造初始版本。整数 version 由服务端维护、成功更新原子 +1，
        // 详情返回、更新必须原样回传，彻底保证每次更新版本唯一推进。
        Integer expectedVersion = updateReqVO.getVersion();
        // ZS-CFG-004 codex r3 P2 修复：快照版本与请求版本必须一致才递增——否则请求预填高版本（如 8）、
        // 快照 7、期间他人推进至 8 时，本请求仍以 7→8 成功，版本不推进且可回填旧快照值。
        // 不一致立即按冲突拒绝；条件 UPDATE 保留作兜底（覆盖读后写之间的极短窗口）。
        if (expectedVersion == null || !expectedVersion.equals(exists.getVersion())) {
            throw exception(CONFIG_UPDATE_CONFLICT);
        }
        updateObj.setVersion(expectedVersion + 1);
        int affected = configMapper.update(updateObj,
                new LambdaQueryWrapper<ConfigDO>()
                        .eq(ConfigDO::getId, updateObj.getId())
                        .eq(ConfigDO::getVersion, expectedVersion));
        if (affected == 0) {
            throw exception(CONFIG_UPDATE_CONFLICT);
        }
        // ZS-CFG-004 B04：变更留痕——before=库中原快照（exists），after=本次更新结果（updateObj，version 已 +1）
        configChangeRecorder.recordUpdate(exists, updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfig(Long id) {
        // 校验配置存在
        ConfigDO config = validateConfigExists(id);
        // 内置配置，不允许删除
        if (ConfigTypeEnum.SYSTEM.getType().equals(config.getType())) {
            throw exception(CONFIG_CAN_NOT_DELETE_SYSTEM_TYPE);
        }
        // 删除（逻辑删除 UPDATE 自带 deleted=0 条件，affected=0 即并发下已被他方删除，
        // r0 P2：不得给未实际删除的行记成功审计）
        int affected = configMapper.deleteById(id);
        if (affected == 0) {
            throw exception(CONFIG_NOT_EXISTS);
        }
        // ZS-CFG-004 B04：删除留痕——仅对真实删除成功的行留痕
        configChangeRecorder.recordDelete(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfigList(List<Long> ids) {
        // 校验是否有内置配置
        List<ConfigDO> configs = configMapper.selectByIds(ids);
        configs.forEach(config -> {
            if (ConfigTypeEnum.SYSTEM.getType().equals(config.getType())) {
                throw exception(CONFIG_CAN_NOT_DELETE_SYSTEM_TYPE);
            }
        });

        // 逐行条件删除并对真实删除成功的行留痕（r0 P2：并发下半删成功不得整批伪报，
        // 也不得给被并发抢先删除的行记成功审计）
        configs.forEach(config -> {
            if (configMapper.deleteById(config.getId()) > 0) {
                configChangeRecorder.recordDelete(config);
            }
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreConfig(ConfigRestoreReqVO reqVO) {
        // 校验配置存在——已删除配置不可恢复：恢复仅回写既有行的 value、不插入新行，
        // 结构性排除「复活」路径（模块启停由 ModuleCatalog 构建期白名单决定（ZS-ENG-001/ZS-CFG-003.A），
        // infra_config 无模块开关键，恢复不触碰模块装配状态）
        ConfigDO config = validateConfigExists(reqVO.getId());
        // 校验历史记录存在
        ConfigChangeHistoryDO history = configChangeHistoryMapper.selectById(reqVO.getHistoryId());
        if (history == null) {
            throw exception(CONFIG_RESTORE_HISTORY_NOT_EXISTS);
        }
        // 历史与配置必须匹配——跨配置恢复即拒绝，按 DENIED 独立留痕
        if (!config.getId().equals(history.getConfigId())) {
            configChangeRecorder.recordRestoreDenied(config,
                    "恢复目标历史与参数配置不匹配：historyId=" + history.getId());
            throw exception(CONFIG_RESTORE_HISTORY_MISMATCH);
        }
        // 仅 UPDATE/RESTORE 历史可恢复，且 old_value 存在且未脱敏——
        // 秘密/敏感参数的历史值落库时已掩码（显式 oldValueRedacted 标志，r0 P2：不做值形推断，
        // NORMAL 配置的字面 ****** 可正常恢复），不可自动恢复的须管理员手工重新填写
        boolean restorableType = ConfigChangeHistoryDO.TYPE_UPDATE.equals(history.getChangeType())
                || ConfigChangeHistoryDO.TYPE_RESTORE.equals(history.getChangeType());
        boolean restorableValue = history.getOldValue() != null
                && !Boolean.TRUE.equals(history.getOldValueRedacted());
        if (!restorableType || !restorableValue) {
            configChangeRecorder.recordRestoreDenied(config,
                    "历史记录不可自动恢复：changeType=" + history.getChangeType()
                            + "，秘密/敏感历史值已脱敏或该记录类型不支持恢复，请手工重新填写");
            throw exception(CONFIG_RESTORE_NOT_RESTORABLE);
        }
        // 恢复值走与更新同一值校验路径——非法历史值不得进入运行
        configValueValidator.validate(config.getConfigKey(), history.getOldValue());
        // 与更新同一乐观锁契约：请求版本必须等于当前版本（null 同样按冲突拒绝，防内部调用绕过校验注解）
        if (reqVO.getVersion() == null || !reqVO.getVersion().equals(config.getVersion())) {
            throw exception(CONFIG_UPDATE_CONFLICT);
        }
        // 仅回写 value（MP 非空字段更新策略：key/visible/category/name 均不动），版本 +1，条件更新兜底
        ConfigDO updateObj = new ConfigDO();
        updateObj.setId(config.getId());
        updateObj.setValue(history.getOldValue());
        updateObj.setVersion(config.getVersion() + 1);
        int affected = configMapper.update(updateObj,
                new LambdaQueryWrapper<ConfigDO>()
                        .eq(ConfigDO::getId, config.getId())
                        .eq(ConfigDO::getVersion, reqVO.getVersion()));
        if (affected == 0) {
            throw exception(CONFIG_UPDATE_CONFLICT);
        }
        // 恢复留痕：before=恢复前快照（原值 + 原版本），after=恢复后快照（历史值 + 版本 +1）
        ConfigDO after = new ConfigDO();
        after.setId(config.getId());
        after.setCategory(config.getCategory());
        after.setName(config.getName());
        after.setConfigKey(config.getConfigKey());
        after.setValue(history.getOldValue());
        after.setVisible(config.getVisible());
        after.setVersion(config.getVersion() + 1);
        configChangeRecorder.recordRestore(config, after, reqVO.getReason(), history.getId());
    }

    @Override
    public PageResult<ConfigChangeHistoryRespVO> getConfigChangeHistoryPage(ConfigChangeHistoryPageReqVO pageReqVO) {
        ConfigDO config = validateConfigExists(pageReqVO.getConfigId());
        PageResult<ConfigChangeHistoryDO> page = configChangeHistoryMapper.selectPageByConfigId(pageReqVO);
        // 输出统一脱敏：当前配置为敏感/秘密级时整页掩码——覆盖「NORMAL 期明文入史 + 后改名/改可见为敏感」的历史行；
        // NORMAL 配置原样输出（敏感期历史落库时已按当时敏感级存为掩码，不泄露）
        boolean maskAll = sensitiveClassifier.classify(config) != ConfigSensitiveClassifier.SensitiveLevel.NORMAL;
        List<ConfigChangeHistoryRespVO> list = page.getList().stream().map(history -> {
            ConfigChangeHistoryRespVO vo = new ConfigChangeHistoryRespVO();
            vo.setId(history.getId());
            vo.setConfigId(history.getConfigId());
            vo.setConfigKey(history.getConfigKey());
            vo.setChangeType(history.getChangeType());
            vo.setOldValue(maskAll ? ConfigSensitiveClassifier.MASK_VALUE : history.getOldValue());
            vo.setNewValue(maskAll ? ConfigSensitiveClassifier.MASK_VALUE : history.getNewValue());
            vo.setOldVersion(history.getOldVersion());
            vo.setNewVersion(history.getNewVersion());
            vo.setOperatorId(history.getOperatorId());
            vo.setReason(history.getReason());
            vo.setCreateTime(history.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Override
    public ConfigDO getConfig(Long id) {
        return configMapper.selectById(id);
    }

    @Override
    public ConfigDO getConfigByKey(String key) {
        return configMapper.selectByKey(key);
    }

    @Override
    public PageResult<ConfigDO> getConfigPage(ConfigPageReqVO pageReqVO) {
        return configMapper.selectPage(pageReqVO);
    }

    @Override
    public ConfigRespVO getMaskedConfigRespVO(ConfigDO config) {
        ConfigRespVO vo = ConfigConvert.INSTANCE.convert(config);
        if (vo != null) {
            // ZS-CFG-001.B：秘密/敏感项仅掩码 value，其余字段保持可读
            vo.setValue(sensitiveClassifier.maskValue(config));
        }
        return vo;
    }

    @Override
    public ConfigSensitiveClassifier.SensitiveLevel classifySensitive(ConfigDO config) {
        return sensitiveClassifier.classify(config);
    }

    @VisibleForTesting
    public ConfigDO validateConfigExists(Long id) {
        if (id == null) {
            return null;
        }
        ConfigDO config = configMapper.selectById(id);
        if (config == null) {
            throw exception(CONFIG_NOT_EXISTS);
        }
        return config;
    }

    @VisibleForTesting
    public void validateConfigKeyUnique(Long id, String key) {
        ConfigDO config = configMapper.selectByKey(key);
        if (config == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的参数配置
        if (id == null) {
            throw exception(CONFIG_KEY_DUPLICATE);
        }
        if (!config.getId().equals(id)) {
            throw exception(CONFIG_KEY_DUPLICATE);
        }
    }

}
