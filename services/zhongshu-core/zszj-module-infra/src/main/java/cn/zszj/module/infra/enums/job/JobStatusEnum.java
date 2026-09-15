package cn.zszj.module.infra.enums.job;

import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.quartz.impl.jdbcjobstore.Constants;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * 任务状态的枚举
 *
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum JobStatusEnum {

    /**
     * 初始化中
     */
    INIT(0, Collections.emptySet()),
    /**
     * 开启
     */
    NORMAL(1, Sets.newHashSet(Constants.STATE_WAITING, Constants.STATE_ACQUIRED, Constants.STATE_BLOCKED)),
    /**
     * 暂停
     */
    STOP(2, Sets.newHashSet(Constants.STATE_PAUSED, Constants.STATE_PAUSED_BLOCKED));

    /**
     * 状态
     */
    private final Integer status;
    /**
     * 对应的 Quartz 触发器的状态集合
     */
    private final Set<String> quartzStates;

    /**
     * 任务表状态是否表示“应当可跑”
     *
     * 启停一致性需要一个单一真源：任务同步（JobServiceImpl#syncJob）与调度器对账（JobSchedulerReconciler）
     * 若各自判断，INIT 这类“尚未开启”的状态就会一侧放行、一侧暂停，反而制造漂移。
     *
     * @param status 任务表状态
     * @return 是否应当可跑；仅 {@link #NORMAL} 为 true
     */
    public static boolean shouldRun(Integer status) {
        return Objects.equals(NORMAL.getStatus(), status);
    }

}
