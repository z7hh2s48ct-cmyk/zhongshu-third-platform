package cn.zszj.module.bpm.firstchain;

import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.module.bpm.enums.ErrorCodeConstants.FIRST_CHAIN_TENANT_REQUIRED;

/**
 * 首链指标服务（ZS-BPM-003，验收③）——指标从领域事实派生（权威状态列 GROUP BY），不设独立计数器。
 *
 * <p>口径登记：统计范围=当前租户未删除行；线索指标（M6 五态分布）随线索业务表落卡（D-07 M1），
 * 本卡仅交付申请域派生实现与口径合同。无缓存（每次实时统计）。
 *
 * @author ZS-BPM-003
 */
public class FirstChainMetricsService {

    private final JdbcTemplate jdbcTemplate;

    public FirstChainMetricsService(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * 加盟商申请状态分布（指标口径 = 权威 status 列 GROUP BY，version 终态事实，无独立计数器）；
     * 仅含当前租户、未删除行；不存在的状态不出现在结果中（消费方按缺失=0 处理）。
     */
    public Map<String, Long> applicationStateCounts() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw exception(FIRST_CHAIN_TENANT_REQUIRED);
        }
        Map<String, Long> counts = new LinkedHashMap<>();
        jdbcTemplate.queryForList(
                        "SELECT status, COUNT(*) AS cnt FROM bpm_first_chain_application "
                                + "WHERE tenant_id = ? AND deleted = FALSE GROUP BY status",
                        tenantId)
                .forEach(row -> counts.put((String) row.get("status"), ((Number) row.get("cnt")).longValue()));
        return counts;
    }

}
