package cn.zszj.module.infra.service.config;

import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.module.infra.controller.admin.config.vo.ConfigPageReqVO;
import cn.zszj.module.infra.controller.admin.config.vo.ConfigRespVO;
import cn.zszj.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import cn.zszj.module.infra.dal.dataobject.config.ConfigDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 参数配置 Service 接口
 *
 * @author 芋道源码
 */
public interface ConfigService {

    /**
     * 创建参数配置
     *
     * @param createReqVO 创建信息
     * @return 配置编号
     */
    Long createConfig(@Valid ConfigSaveReqVO createReqVO);

    /**
     * 更新参数配置
     *
     * @param updateReqVO 更新信息
     */
    void updateConfig(@Valid ConfigSaveReqVO updateReqVO);

    /**
     * 删除参数配置
     *
     * @param id 配置编号
     */
    void deleteConfig(Long id);

    /**
     * 批量删除参数配置
     *
     * @param ids 配置编号列表
     */
    void deleteConfigList(List<Long> ids);

    /**
     * 获得参数配置
     *
     * @param id 配置编号
     * @return 参数配置
     */
    ConfigDO getConfig(Long id);

    /**
     * 根据参数键，获得参数配置
     *
     * @param key 配置键
     * @return 参数配置
     */
    ConfigDO getConfigByKey(String key);

    /**
     * 获得参数配置分页列表
     *
     * @param reqVO 分页条件
     * @return 分页列表
     */
    PageResult<ConfigDO> getConfigPage(ConfigPageReqVO reqVO);

    /**
     * ZS-CFG-001.B：获得脱敏后的参数配置 RespVO（详情/分页/导出统一入口）。
     *
     * 秘密/敏感项的 value 掩码为 {@code ******}，其余字段保持可读；普通项原样返回。
     *
     * @param config 参数配置
     * @return 脱敏后的 RespVO
     */
    ConfigRespVO getMaskedConfigRespVO(ConfigDO config);

    /**
     * ZS-CFG-001.B：判定参数配置的敏感级（供 /get-value-by-key 秘密键防护使用）。
     *
     * @param config 参数配置
     * @return 敏感级
     */
    ConfigSensitiveClassifier.SensitiveLevel classifySensitive(ConfigDO config);

}
