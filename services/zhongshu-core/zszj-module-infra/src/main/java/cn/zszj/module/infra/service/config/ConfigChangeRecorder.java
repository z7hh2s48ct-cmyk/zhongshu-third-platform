package cn.zszj.module.infra.service.config;

import cn.zszj.framework.common.biz.system.audit.AuditEventMessage;
import cn.zszj.framework.common.biz.system.audit.AuditEventTypes;
import cn.zszj.framework.common.biz.system.audit.AuditPort;
import cn.zszj.framework.common.util.monitor.TracerUtils;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import cn.zszj.module.infra.dal.dataobject.config.ConfigChangeHistoryDO;
import cn.zszj.module.infra.dal.dataobject.config.ConfigDO;
import cn.zszj.module.infra.dal.mysql.config.ConfigChangeHistoryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;

import static cn.zszj.module.infra.dal.dataobject.config.ConfigChangeHistoryDO.TYPE_CREATE;
import static cn.zszj.module.infra.dal.dataobject.config.ConfigChangeHistoryDO.TYPE_DELETE;
import static cn.zszj.module.infra.dal.dataobject.config.ConfigChangeHistoryDO.TYPE_RESTORE;
import static cn.zszj.module.infra.dal.dataobject.config.ConfigChangeHistoryDO.TYPE_UPDATE;

/**
 * 参数配置变更记录器（ZS-CFG-004 B04 复验审计部分）——变更历史的唯一落库点 + 统一 AuditPort 接线。
 *
 * <p>两路留痕，语义对齐 ZS-AUDIT-001：</p>
 * <ul>
 *   <li><b>变更历史</b>（{@code infra_config_history}）——随调用方业务事务落库，业务回滚则历史不留；
 *       秘密/敏感参数只落掩码 {@code ******}（{@link ConfigSensitiveClassifier} 判定），明文不落历史，
 *       恢复流程仅可回写 NORMAL 参数的历史值；</li>
 *   <li><b>审计事件</b>（{@link AuditPort#record}）——SUCCESS 随调用方事务提交（业务回滚不留成功审计，
 *       审计写入失败则整体回滚，杜绝「改了配置却无审计」）；恢复拒绝（敏感不可恢复 / 历史不匹配）以
 *       DENIED 独立事务留痕，其失败不阻断业务拒绝的返回。</li>
 * </ul>
 *
 * <p>detail 只装脱敏摘要（key、前后值掩码、版本、依据、historyId），不携带敏感明文（AuditPort 合同）。</p>
 */
@Component
@Slf4j
public class ConfigChangeRecorder {

    /** 审计对象类型（infra_config 表名，便于按对象跨域追查）。 */
    private static final String BIZ_TYPE = "infra_config";

    @Resource
    private ConfigChangeHistoryMapper configChangeHistoryMapper;

    @Resource
    private AuditPort auditPort;

    @Resource
    private ConfigSensitiveClassifier sensitiveClassifier;

    /**
     * 创建留痕：历史行（old 为 NULL）+ OBJECT_CREATED 成功审计。
     */
    public void recordCreate(ConfigDO after) {
        recordChange(AuditEventTypes.OBJECT_CREATED, "CREATE", null, after, TYPE_CREATE, null, null);
    }

    /**
     * 更新留痕：历史行（前后值 + 前后版本）+ OBJECT_UPDATED 成功审计。key 变更时 detail 同时记录 oldKey。
     */
    public void recordUpdate(ConfigDO before, ConfigDO after) {
        recordChange(AuditEventTypes.OBJECT_UPDATED, "UPDATE", before, after, TYPE_UPDATE, null, null);
    }

    /**
     * 删除留痕：历史行（new 为 NULL）+ OBJECT_DELETED 成功审计。
     */
    public void recordDelete(ConfigDO before) {
        recordChange(AuditEventTypes.OBJECT_DELETED, "DELETE", before, null, TYPE_DELETE, null, null);
    }

    /**
     * 恢复留痕：历史行（RESTORE）+ {@link AuditEventTypes#CONFIG_PARAM_RESTORED} 成功审计，reason 必填。
     *
     * @param before    恢复前配置快照（原值 + 原版本）
     * @param after     恢复后配置快照（历史值 + 版本 +1）
     * @param reason    恢复审查依据
     * @param historyId 恢复目标的变更历史编号
     */
    public void recordRestore(ConfigDO before, ConfigDO after, String reason, Long historyId) {
        recordChange(AuditEventTypes.CONFIG_PARAM_RESTORED, "RESTORE", before, after, TYPE_RESTORE, historyId, reason);
    }

    /**
     * 恢复被业务守卫拒绝（历史不可恢复 / 与配置不匹配）时的 DENIED 留痕：独立事务语义由 AuditPort 保证，
     * 此处吞掉审计自身的失败并告警——业务拒绝结果必须原样返回调用方，不因拒绝审计落库失败而变形。
     *
     * @param config 被恢复目标配置（可为 NULL：目标不存在场景）
     * @param reason 拒绝原因（不含敏感明文）
     */
    public void recordRestoreDenied(ConfigDO config, String reason) {
        try {
            auditPort.record(AuditEventMessage.builder()
                    .eventType(AuditEventTypes.ACCESS_DENIED)
                    .actorType(AuditEventMessage.ActorType.ADMIN)
                    .actorId(resolveActorIdAsString())
                    .action("RESTORE")
                    .bizType(BIZ_TYPE)
                    .bizId(config != null && config.getId() != null ? String.valueOf(config.getId()) : null)
                    .bizVersion(config != null && config.getVersion() != null ? String.valueOf(config.getVersion()) : null)
                    .reason(reason)
                    .result(AuditEventMessage.AuditResult.DENIED)
                    .detail(buildDetail(config, config, null, null))
                    .traceId(resolveTraceId())
                    .build());
        } catch (Exception e) {
            log.warn("[recordRestoreDenied]恢复拒绝审计落库失败（不阻断业务拒绝）[configKey={} reason={}]",
                    config != null ? config.getConfigKey() : null, reason, e);
        }
    }

    /**
     * 统一留痕入口：一行变更历史 + 一条 SUCCESS 审计（同一调用方事务，历史或审计任一失败则整体回滚）。
     *
     * @param eventType 审计事件类型（{@link AuditEventTypes}）
     * @param action    动作（CREATE/UPDATE/DELETE/RESTORE）
     * @param before    变更前快照（CREATE 为 NULL）
     * @param after     变更后快照（DELETE 为 NULL）
     * @param changeType 历史行变更类型
     * @param historyId 恢复目标历史编号（仅 RESTORE）
     * @param reason    审查依据（仅 RESTORE）
     */
    private void recordChange(String eventType, String action, ConfigDO before, ConfigDO after,
                              String changeType, Long historyId, String reason) {
        ConfigDO anchor = after != null ? after : before;
        String oldMasked = before != null ? maskValue(before) : null;
        String newMasked = after != null ? maskValue(after) : null;
        // 1. 变更历史（随业务事务）
        ConfigChangeHistoryDO history = new ConfigChangeHistoryDO();
        history.setConfigId(anchor.getId());
        history.setConfigKey(anchor.getConfigKey());
        history.setChangeType(changeType);
        history.setOldValue(oldMasked);
        history.setNewValue(newMasked);
        history.setOldVersion(before != null ? before.getVersion() : null);
        history.setNewVersion(after != null ? after.getVersion() : null);
        history.setOperatorId(resolveActorId());
        history.setReason(reason);
        configChangeHistoryMapper.insert(history);
        // 2. SUCCESS 审计（AuditPort SUCCESS 语义：随事务提交、失败 fail-closed 回滚）
        auditPort.record(AuditEventMessage.builder()
                .eventType(eventType)
                .actorType(AuditEventMessage.ActorType.ADMIN)
                .actorId(resolveActorIdAsString())
                .action(action)
                .bizType(BIZ_TYPE)
                .bizId(anchor.getId() != null ? String.valueOf(anchor.getId()) : null)
                .bizVersion(anchor.getVersion() != null ? String.valueOf(anchor.getVersion()) : null)
                .reason(reason)
                .result(AuditEventMessage.AuditResult.SUCCESS)
                .detail(buildDetail(before, after, historyId, reason))
                .traceId(resolveTraceId())
                .build());
    }

    /**
     * 审计明细（只装脱敏摘要）：key（改名时并列 oldKey）、前后值掩码、前后版本、变更类型、审查依据、historyId。
     */
    private Map<String, Object> buildDetail(ConfigDO before, ConfigDO after, Long historyId, String reason) {
        Map<String, Object> detail = new LinkedHashMap<>();
        ConfigDO anchor = after != null ? after : before;
        detail.put("key", anchor != null ? anchor.getConfigKey() : null);
        if (before != null && after != null
                && before.getConfigKey() != null && !before.getConfigKey().equals(after.getConfigKey())) {
            detail.put("oldKey", before.getConfigKey());
        }
        detail.put("oldValue", before != null ? maskValue(before) : null);
        detail.put("newValue", after != null ? maskValue(after) : null);
        detail.put("oldVersion", before != null ? before.getVersion() : null);
        detail.put("newVersion", after != null ? after.getVersion() : null);
        if (historyId != null) {
            detail.put("historyId", historyId);
        }
        if (reason != null) {
            detail.put("reason", reason);
        }
        return detail;
    }

    /**
     * 按该配置自身的 key/visible 判定敏感级并掩码：SECRET/SENSITIVE 落 {@code ******}，NORMAL 落原值摘要。
     * key 变更场景下前后值各按变更前后的 key 判定，保证改名链上任一侧为敏感即不落明文。
     */
    private String maskValue(ConfigDO config) {
        return sensitiveClassifier.classify(config) == ConfigSensitiveClassifier.SensitiveLevel.NORMAL
                ? config.getValue()
                : ConfigSensitiveClassifier.MASK_VALUE;
    }

    /**
     * 操作者解析（protected 便于测试覆写）：后台配置操作主体为登录管理员，系统上下文缺失时返回 NULL
     * （不伪造操作者，宁缺勿假）。
     */
    protected Long resolveActorId() {
        return SecurityFrameworkUtils.getLoginUserId();
    }

    /**
     * 操作者字符串形态（审计消息 {@code actorId} 为 String；历史行 {@code operatorId} 仍用 Long）。
     */
    private String resolveActorIdAsString() {
        Long actorId = resolveActorId();
        return actorId != null ? String.valueOf(actorId) : null;
    }

    /**
     * 追踪 ID 解析：复用 SEC-006 关联 ID 入口，无链路时为 NULL（不写空串占位）。
     */
    private String resolveTraceId() {
        String traceId = TracerUtils.getTraceId();
        return traceId == null || traceId.isEmpty() ? null : traceId;
    }

}
