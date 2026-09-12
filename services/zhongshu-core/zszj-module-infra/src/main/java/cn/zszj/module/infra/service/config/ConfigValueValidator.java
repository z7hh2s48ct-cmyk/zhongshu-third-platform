package cn.zszj.module.infra.service.config;

import cn.zszj.module.infra.dal.dataobject.config.ConfigDO;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.*;

/**
 * ZS-CFG-004 B03：配置值校验器——按 {@link ConfigParamCatalog} 登记的合同校验参数值。
 *
 * <p>只对已登记参数做强校验，未登记参数维持既有行为（渐进式）。校验规则：</p>
 * <ul>
 *   <li>INTEGER：值必须可解析为整数且在 [minInt, maxInt] 范围内</li>
 *   <li>BOOLEAN：值必须为规范化小写的 "true" 或 "false"（严格小写，见 {@link #validateBoolean}）</li>
 *   <li>ENUM：值必须在 allowedValues 集合中</li>
 *   <li>STRING：无格式约束（仅登记存在性/敏感性/热生效标注）</li>
 * </ul>
 */
@Component
public class ConfigValueValidator {

    /**
     * 校验配置值是否符合参数目录合同。
     *
     * <p>未登记参数直接放行（返回），已登记参数按类型校验。</p>
     *
     * @param key   参数键名
     * @param value 参数值
     * @throws cn.zszj.framework.common.exception.ServiceException 校验失败时抛出对应错误码
     */
    public void validate(String key, String value) {
        Optional<ConfigParamCatalog> catalogOpt = ConfigParamCatalog.findByKey(key);
        if (catalogOpt.isEmpty()) {
            // 未登记参数：维持既有行为，不校验
            return;
        }
        ConfigParamCatalog catalog = catalogOpt.get();
        switch (catalog.getValueType()) {
            case INTEGER:
                validateInteger(catalog, value);
                break;
            case BOOLEAN:
                validateBoolean(catalog, value);
                break;
            case ENUM:
                validateEnum(catalog, value);
                break;
            case STRING:
                // STRING 类型无格式约束，仅登记存在性
                break;
        }
    }

    private void validateInteger(ConfigParamCatalog catalog, String value) {
        int parsed;
        try {
            parsed = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw exception(CONFIG_VALUE_TYPE_MISMATCH, "INTEGER");
        }
        Integer min = catalog.getMinInt();
        Integer max = catalog.getMaxInt();
        if (min != null && max != null && (parsed < min || parsed > max)) {
            throw exception(CONFIG_VALUE_OUT_OF_RANGE, min, max);
        }
    }

    /**
     * ZS-CFG-004 codex r0 P2-2 修复：Boolean 校验只接受【规范化小写】值，与消费方对齐。
     *
     * <p>消费方 {@code AdminUserServiceImpl.registerUser()} 判定注册开关时使用
     * {@code ObjUtil.notEqual(configValue, "true")}——仅当存储串严格等于小写 {@code "true"} 才启用注册。
     * 若此处按 {@code equalsIgnoreCase} 放行 {@code "TRUE"}/{@code "True"} 并原样持久化，则被校验接受的
     * 布尔真值反而会因拼写不匹配而【静默关闭】注册（校验通过 ≠ 消费方按预期解析）。故收紧为严格小写，
     * 保证"校验通过 ⇒ 存储串即为消费方识别的规范值"的契约对齐；非规范拼写返回类型不匹配错误，提示调用方归一化。</p>
     */
    private void validateBoolean(ConfigParamCatalog catalog, String value) {
        if (!"true".equals(value) && !"false".equals(value)) {
            throw exception(CONFIG_VALUE_TYPE_MISMATCH, "BOOLEAN");
        }
    }

    private void validateEnum(ConfigParamCatalog catalog, String value) {
        if (catalog.getAllowedValues() != null && !catalog.getAllowedValues().contains(value)) {
            throw exception(CONFIG_VALUE_NOT_IN_ALLOWED_SET, catalog.getAllowedValues());
        }
    }

    /**
     * 判断指定 key 的参数是否需要重启才能生效。
     *
     * @param key 参数键名
     * @return true=需重启，false=热生效或未登记（未登记默认视为热生效，维持既有行为）
     */
    public boolean isRestartRequired(String key) {
        return ConfigParamCatalog.findByKey(key)
                .map(catalog -> !catalog.isHotReload())
                .orElse(false);
    }

    /**
     * B03 审计摘要：构建变更摘要，敏感参数旧值脱敏。
     *
     * <p>完整审计持久化（版本/操作者/审查恢复流）留 B04；本方法仅提供脱敏摘要生成能力，
     * 确保敏感旧值原文不出现在任何变更记录中。</p>
     *
     * @param key                 参数键名
     * @param oldValue            旧值
     * @param newValue            新值
     * @param sensitiveClassifier 敏感分类器（复用 CFG-001.B）
     * @param oldConfig           旧配置 DO（供分类器判定）
     * @return 脱敏后的变更摘要字符串
     */
    public String buildChangeAuditSummary(String key, String oldValue, String newValue,
                                          ConfigSensitiveClassifier sensitiveClassifier,
                                          ConfigDO oldConfig) {
        boolean isSensitive = sensitiveClassifier.classify(oldConfig) != ConfigSensitiveClassifier.SensitiveLevel.NORMAL;
        String maskedOld = isSensitive ? ConfigSensitiveClassifier.MASK_VALUE : oldValue;
        String maskedNew = isSensitive ? ConfigSensitiveClassifier.MASK_VALUE : newValue;
        return String.format("key=%s, oldValue=%s, newValue=%s", key, maskedOld, maskedNew);
    }
}
