package cn.zszj.module.infra.service.config;

import java.util.*;

/**
 * ZS-CFG-004 B03：参数目录——声明受控参数的值合同（类型/范围/枚举集/热生效/敏感）。
 *
 * <p>只对已登记合同的参数做强校验，未登记参数维持既有行为（渐进式，不一次性锁死全部 key
 * 避免误伤既有自定义配置）。</p>
 *
 * <p>登记原则：系统核心参数优先，覆盖 Boolean/Integer/Enum/String 四种类型，
 * 标注热生效（hotReload=true）或需重启（hotReload=false），以及是否敏感。</p>
 */
public enum ConfigParamCatalog {

    // ===== Boolean 类型 =====
    /** 用户注册开关（热生效） */
    SYSTEM_USER_REGISTER_ENABLED("system.user.register-enabled", ValueType.BOOLEAN,
            null, null, null, true, false),

    // ===== Integer 类型（带范围）=====
    /** 登录验证码最大重试次数（热生效，1~10） */
    SYS_LOGIN_CAPTCHA_MAX_RETRY("sys.login.captcha-max-retry", ValueType.INTEGER,
            1, 10, null, true, false),
    /** 登录锁定持续分钟数（需重启，1~1440） */
    SYS_LOGIN_LOCK_DURATION_MINUTES("sys.login.lock-duration-minutes", ValueType.INTEGER,
            1, 1440, null, false, false),

    // ===== Enum 类型（受限值集）=====
    /** 文件上传模式（热生效，local/oss/s3） */
    SYS_FILE_UPLOAD_MODE("sys.file.upload-mode", ValueType.ENUM,
            null, null, Set.of("local", "oss", "s3"), true, false),

    // ===== String 类型 =====
    /** 用户初始密码（热生效，敏感） */
    SYSTEM_USER_INIT_PASSWORD("system.user.init-password", ValueType.STRING,
            null, null, null, true, true),
    /** Druid 监控地址（需重启） */
    URL_DRUID("url.druid", ValueType.STRING,
            null, null, null, false, false),
    ;

    /**
     * 参数值类型。
     */
    public enum ValueType {
        /** 整数（有范围约束） */
        INTEGER,
        /** 布尔（true/false） */
        BOOLEAN,
        /** 枚举（受限值集） */
        ENUM,
        /** 字符串（无格式约束，仅登记存在性） */
        STRING
    }

    private final String key;
    private final ValueType valueType;
    private final Integer minInt;
    private final Integer maxInt;
    private final Set<String> allowedValues;
    private final boolean hotReload;
    private final boolean sensitive;

    ConfigParamCatalog(String key, ValueType valueType, Integer minInt, Integer maxInt,
                       Set<String> allowedValues, boolean hotReload, boolean sensitive) {
        this.key = key;
        this.valueType = valueType;
        this.minInt = minInt;
        this.maxInt = maxInt;
        this.allowedValues = allowedValues != null ? Collections.unmodifiableSet(allowedValues) : null;
        this.hotReload = hotReload;
        this.sensitive = sensitive;
    }

    public String getKey() {
        return key;
    }

    public ValueType getValueType() {
        return valueType;
    }

    public Integer getMinInt() {
        return minInt;
    }

    public Integer getMaxInt() {
        return maxInt;
    }

    public Set<String> getAllowedValues() {
        return allowedValues;
    }

    public boolean isHotReload() {
        return hotReload;
    }

    public boolean isSensitive() {
        return sensitive;
    }

    // ===== 查找 =====

    private static final Map<String, ConfigParamCatalog> KEY_MAP;

    static {
        Map<String, ConfigParamCatalog> map = new HashMap<>();
        for (ConfigParamCatalog entry : values()) {
            map.put(entry.key, entry);
        }
        KEY_MAP = Collections.unmodifiableMap(map);
    }

    /**
     * 按 configKey 查找已登记的参数合同。
     *
     * @param key 参数键名
     * @return 已登记的参数目录项，未登记返回 empty
     */
    public static Optional<ConfigParamCatalog> findByKey(String key) {
        return Optional.ofNullable(KEY_MAP.get(key));
    }

    /**
     * 为 ENUM 类型提供允许值集（静态工厂辅助）。
     */
    public static Set<String> enumSet(String... values) {
        return new LinkedHashSet<>(Arrays.asList(values));
    }
}
