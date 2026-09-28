package cn.zszj.module.bpm.firstchain;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 首链指标服务（ZS-BPM-003，验收③）——指标从领域事实派生（权威状态列 GROUP BY），不设独立计数器。
 *
 * @author ZS-BPM-003
 */
public class FirstChainMetricsService {

    private final JdbcTemplate jdbcTemplate;

    public FirstChainMetricsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 加盟商申请状态分布（指标口径 = 权威 status 列 GROUP BY，version 终态事实，无独立计数器）；
     * 仅含当前租户、未删除行；不存在的状态不出现在结果中。
     */
    public java.util.Map<String, Long> applicationStateCounts() {
        throw new UnsupportedOperationException("待实现（ZS-BPM-003 GREEN）");
    }

}
