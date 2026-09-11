package cn.zszj.module.infra.service.config;

import cn.zszj.module.infra.dal.dataobject.config.ConfigDO;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ZS-CFG-001.B：参数敏感级分类器，复用 CFG-001.A 三档分类（秘密/敏感/普通）。
 *
 * <p>不引入新列（本批次无 schema 变更）：判定依据为「configKey 命中秘密键模式」或「visible==false」。
 * 秘密键模式移植自 {@code scripts/cfg/verify-config-secrets.mjs} 的 SECRET_KEY 集合。</p>
 */
@Component
public class ConfigSensitiveClassifier {

    /**
     * 敏感级三档。
     */
    public enum SensitiveLevel {
        /** 秘密：命中秘密键模式，任何情况下都不应输出/翻为可见 */
        SECRET,
        /** 敏感：不可见参数，输出需脱敏 */
        SENSITIVE,
        /** 普通：可见且非秘密键，原样输出 */
        NORMAL
    }

    /**
     * 秘密键模式（对齐 scripts/cfg/verify-config-secrets.mjs 键集，小写归一 + 去除 -/_ 后 contains 匹配）。
     */
    private static final List<String> SECRET_KEY_TOKENS = List.of(
            "password", "passwd", "secret", "token", "apikey", "api_key",
            "privatekey", "private_key", "secretkey", "secret_key",
            "credential", "accesskey", "access_key");

    /**
     * 判定参数的敏感级。
     *
     * @param config 参数配置
     * @return 敏感级
     */
    public SensitiveLevel classify(ConfigDO config) {
        if (config == null) {
            return SensitiveLevel.NORMAL;
        }
        String key = config.getConfigKey() == null ? ""
                : config.getConfigKey().toLowerCase().replace("-", "").replace("_", "");
        boolean secretKey = SECRET_KEY_TOKENS.stream().anyMatch(t -> key.contains(t.replace("_", "")));
        if (secretKey) {
            return SensitiveLevel.SECRET;
        }
        if (Boolean.FALSE.equals(config.getVisible())) {
            return SensitiveLevel.SENSITIVE;
        }
        return SensitiveLevel.NORMAL;
    }

    /**
     * 秘密/敏感项的 value 掩码为 {@code ******}；普通项原样返回。
     *
     * @param config 参数配置
     * @return 掩码后的 value
     */
    public String maskValue(ConfigDO config) {
        if (config == null) {
            return null;
        }
        return classify(config) == SensitiveLevel.NORMAL ? config.getValue() : "******";
    }

}
