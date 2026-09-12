package cn.zszj.module.infra.service.config;

import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.module.infra.controller.admin.config.vo.ConfigPageReqVO;
import cn.zszj.module.infra.controller.admin.config.vo.ConfigRespVO;
import cn.zszj.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import cn.zszj.module.infra.convert.config.ConfigConvert;
import cn.zszj.module.infra.dal.dataobject.config.ConfigDO;
import cn.zszj.module.infra.dal.mysql.config.ConfigMapper;
import cn.zszj.module.infra.enums.config.ConfigTypeEnum;
import com.google.common.annotations.VisibleForTesting;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

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

    @Override
    public Long createConfig(ConfigSaveReqVO createReqVO) {
        // 校验参数配置 key 的唯一性
        validateConfigKeyUnique(null, createReqVO.getKey());

        // 插入参数配置
        ConfigDO config = ConfigConvert.INSTANCE.convert(createReqVO);
        config.setType(ConfigTypeEnum.CUSTOM.getType());
        configMapper.insert(config);
        return config.getId();
    }

    @Override
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
        if (sensitiveClassifier.isMaskedEcho(exists, updateObj.getValue())) {
            // ZS-CFG-001.B r1 P1 修复：回传掩码=调用方不掌握真值，禁止在同一更新里下调该值保护级，否则可两步洗密
            // （SECRET 改名脱密降 SENSITIVE → 翻 visible 降 NORMAL）后经 /get-value-by-key 与详情读出明文；现有
            // TOCTOU 守卫只拦 SECRET→visible，拦不住改名降级链，故此处补齐。
            if (sensitiveClassifier.isProtectionDowngrade(exists, updateObj)) {
                throw exception(CONFIG_SENSITIVE_CAN_NOT_DOWNGRADE_ON_MASKED_ECHO);
            }
            updateObj.setValue(exists.getValue());
        }

        configMapper.updateById(updateObj);
    }

    @Override
    public void deleteConfig(Long id) {
        // 校验配置存在
        ConfigDO config = validateConfigExists(id);
        // 内置配置，不允许删除
        if (ConfigTypeEnum.SYSTEM.getType().equals(config.getType())) {
            throw exception(CONFIG_CAN_NOT_DELETE_SYSTEM_TYPE);
        }
        // 删除
        configMapper.deleteById(id);
    }

    @Override
    public void deleteConfigList(List<Long> ids) {
        // 校验是否有内置配置
        List<ConfigDO> configs = configMapper.selectByIds(ids);
        configs.forEach(config -> {
            if (ConfigTypeEnum.SYSTEM.getType().equals(config.getType())) {
                throw exception(CONFIG_CAN_NOT_DELETE_SYSTEM_TYPE);
            }
        });

        // 批量删除
        configMapper.deleteByIds(ids);
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
