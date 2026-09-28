package cn.zszj.module.bpm.firstchain;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 首链版本条件状态迁移执行器（ZS-BPM-003）——乐观锁 + 状态守卫 + 租户过滤的单一执行面。
 *
 * @author ZS-BPM-003
 */
public class FirstChainStateTransitionExecutor {

    private final JdbcTemplate jdbcTemplate;

    public FirstChainStateTransitionExecutor(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

}
