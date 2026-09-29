package cn.zszj.module.firstchain.service;

import cn.zszj.framework.common.biz.system.audit.AuditPort;
import cn.zszj.module.bpm.firstchain.FirstChainStateTransitionExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * 线索域指标（ZS-FC-002，D-07 M9/PILOT-REQ-009「三类角色指标来自同一事实」）。
 *
 * <p>口径与 {@code cn.zszj.module.bpm.firstchain.FirstChainMetricsService}（ZS-BPM-003）一致：
 * 从权威状态列 GROUP BY 派生（无独立计数器、无口径旁路）——员工/负责人/平台视角
 * 均以本表为唯一事实来源，仅在 org/assignee 维度收窄范围。
 *
 * @author ZS-FC-002
 */
@Service
@Slf4j
public class FirstchainLeadMetricsService {

    private final JdbcTemplate jdbcTemplate;

    public FirstchainLeadMetricsService(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * 按状态统计（平台全域口径）。
     *
     * @return status → count（仅含实际存在的状态行）
     */
    public Map<String, Long> countByStatus(Long tenantId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT status, COUNT(*) AS cnt FROM bpm_first_chain_lead "
                        + "WHERE tenant_id = ? AND deleted = FALSE GROUP BY status",
                tenantId);
        return rows.stream().collect(java.util.stream.Collectors.toMap(
                r -> (String) r.get("status"), r -> ((Number) r.get("cnt")).longValue()));
    }

    /**
     * 按归属组织 + 状态统计（负责人视角口径：本组织范围）。
     */
    public Map<String, Long> countByStatusAndOrg(Long tenantId, Long orgId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT status, COUNT(*) AS cnt FROM bpm_first_chain_lead "
                        + "WHERE tenant_id = ? AND org_id = ? AND deleted = FALSE GROUP BY status",
                tenantId, orgId);
        return rows.stream().collect(java.util.stream.Collectors.toMap(
                r -> (String) r.get("status"), r -> ((Number) r.get("cnt")).longValue()));
    }

    /**
     * 按分配员工 + 状态统计（员工视角口径：本人范围）。
     */
    public Map<String, Long> countByStatusAndAssignee(Long tenantId, Long userId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT status, COUNT(*) AS cnt FROM bpm_first_chain_lead "
                        + "WHERE tenant_id = ? AND assignee_user_id = ? AND deleted = FALSE GROUP BY status",
                tenantId, userId);
        return rows.stream().collect(java.util.stream.Collectors.toMap(
                r -> (String) r.get("status"), r -> ((Number) r.get("cnt")).longValue()));
    }

}
