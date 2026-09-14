package cn.zszj.module.infra.dal.dataobject.config;

import cn.zszj.framework.mybatis.core.dataobject.BaseDO;
import cn.zszj.framework.tenant.core.aop.TenantIgnore;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 参数配置变更历史（ZS-CFG-004 B04）——变更审计与审查后恢复的持久化基础。
 *
 * <p>仅追加写：每次 create/update/delete/restore 由 {@code ConfigChangeRecorder} 落一行；
 * 秘密/敏感参数只落掩码 {@code ******}，明文不落历史（敏感旧值不写审计原文）。
 * 与 {@link ConfigDO} 同为全局表（{@code @TenantIgnore}，参数配置不区分技术租户）。</p>
 */
@TableName("infra_config_history")
@KeySequence("infra_config_history_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@TenantIgnore
public class ConfigChangeHistoryDO extends BaseDO {

    /** 变更类型：创建。old_value 为 NULL（无历史可恢复）。 */
    public static final String TYPE_CREATE = "CREATE";

    /** 变更类型：更新。old_value 记录变更前值。 */
    public static final String TYPE_UPDATE = "UPDATE";

    /** 变更类型：删除。new_value 为 NULL。 */
    public static final String TYPE_DELETE = "DELETE";

    /** 变更类型：恢复（回写某条 UPDATE/RESTORE 历史的 old_value）。 */
    public static final String TYPE_RESTORE = "RESTORE";

    /**
     * 历史编号
     */
    @TableId
    private Long id;

    /**
     * 参数配置编号（infra_config.id）
     */
    private Long configId;

    /**
     * 变更时的参数键名（key 变更后仍可按当时的键追溯）
     */
    private String configKey;

    /**
     * 变更类型：{@link #TYPE_CREATE} / {@link #TYPE_UPDATE} / {@link #TYPE_DELETE} / {@link #TYPE_RESTORE}
     */
    private String changeType;

    /**
     * 变更前值（秘密/敏感参数为掩码 ******；CREATE 时为 NULL）
     */
    private String oldValue;

    /**
     * 变更前值是否脱敏（TRUE=原值敏感已掩码，不可恢复；FALSE=old_value 为字面原值）。
     * NORMAL 配置的字面 ****** 与脱敏哨兵同形，可恢复性判定以本标志为准，不做值形推断。
     */
    private Boolean oldValueRedacted;

    /**
     * 变更后值（秘密/敏感参数为掩码 ******；DELETE 时为 NULL）
     */
    private String newValue;

    /**
     * 变更后值是否脱敏（语义同 {@link #oldValueRedacted}）。
     */
    private Boolean newValueRedacted;

    /**
     * 变更前乐观锁版本
     */
    private Integer oldVersion;

    /**
     * 变更后乐观锁版本
     */
    private Integer newVersion;

    /**
     * 操作者（管理员用户 ID；系统上下文缺失时为 NULL）
     */
    private Long operatorId;

    /**
     * 审查依据（恢复时必填；普通变更可空）
     */
    private String reason;

}
