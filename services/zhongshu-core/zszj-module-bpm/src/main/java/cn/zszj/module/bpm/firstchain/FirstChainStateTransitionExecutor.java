package cn.zszj.module.bpm.firstchain;

import cn.zszj.framework.common.exception.ServiceException;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.module.bpm.enums.ErrorCodeConstants.FIRST_CHAIN_APPLICATION_NOT_EXISTS;
import static cn.zszj.module.bpm.enums.ErrorCodeConstants.FIRST_CHAIN_STATE_CONFLICT;
import static cn.zszj.module.bpm.enums.ErrorCodeConstants.FIRST_CHAIN_VERSION_CONFLICT;

/**
 * 首链版本条件状态迁移执行器（ZS-BPM-003）——乐观锁 + 状态守卫 + 租户过滤的单一执行面。
 *
 * <p>UPDATE 形状（BPM-004 合同样例同款）：{@code SET status=to, version=version+1 WHERE tenant_id=? AND id=?
 * AND version=? AND status=from AND deleted=FALSE}——0 行即拒，并经<b>读回分类</b>给出可回查的冲突原因：
 * 行不存在/跨租户不可见 → NOT_EXISTS；版本不符 → VERSION_CONFLICT；状态不符（含终态重复操作）→ STATE_CONFLICT。
 *
 * @author ZS-BPM-003
 */
public class FirstChainStateTransitionExecutor {

    private final JdbcTemplate jdbcTemplate;

    public FirstChainStateTransitionExecutor(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * 版本条件状态迁移。
     *
     * @param tenantId        租户（必须显式，缺失即拒——归属合同）
     * @param objectType      对象类型（路由状态机与表）
     * @param id              对象行 ID
     * @param fromStatus      期望当前状态（终态重复操作在此被守卫）
     * @param toStatus        目标状态（调用方须先经 {@link FirstChainStateMachines#assertAllowed} 校验语义）
     * @param expectedVersion 期望当前版本（乐观锁基线）
     * @param updater         操作人留痕
     * @return 迁移后的新版本（expectedVersion + 1）
     * @throws ServiceException 0 行时按读回分类抛 NOT_EXISTS / VERSION_CONFLICT / STATE_CONFLICT
     */
    public long transition(Long tenantId, FirstChainObjectType objectType, Long id, String fromStatus,
                           String toStatus, long expectedVersion, String updater) {
        if (tenantId == null) {
            throw exception(FIRST_CHAIN_APPLICATION_NOT_EXISTS);
        }
        String table = tableNameOf(objectType);
        int updated = jdbcTemplate.update(
                "UPDATE " + table + " SET status = ?, version = version + 1, updater = ?, "
                        + "update_time = CURRENT_TIMESTAMP WHERE tenant_id = ? AND id = ? AND version = ? "
                        + "AND status = ? AND deleted = FALSE",
                toStatus, updater, tenantId, id, expectedVersion, fromStatus);
        if (updated == 1) {
            return expectedVersion + 1;
        }
        // 0 行：读回分类（可回查——同形状对象当前状态与版本一并进入错误信息/审计侧）
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT status, version FROM " + table + " WHERE tenant_id = ? AND id = ? AND deleted = FALSE",
                tenantId, id);
        if (rows.isEmpty()) {
            throw exception(FIRST_CHAIN_APPLICATION_NOT_EXISTS);
        }
        Map<String, Object> row = rows.get(0);
        long actualVersion = ((Number) row.get("version")).longValue();
        if (actualVersion != expectedVersion) {
            throw exception(FIRST_CHAIN_VERSION_CONFLICT, expectedVersion);
        }
        throw exception(FIRST_CHAIN_STATE_CONFLICT, String.valueOf(row.get("status")));
    }

    private static String tableNameOf(FirstChainObjectType objectType) {
        return switch (objectType) {
            case APPLICATION -> "bpm_first_chain_application";
            case LEAD -> "bpm_first_chain_lead"; // 线索业务表随首链业务模块落卡（D-07 M1），执行器先备表名合同
        };
    }

}
