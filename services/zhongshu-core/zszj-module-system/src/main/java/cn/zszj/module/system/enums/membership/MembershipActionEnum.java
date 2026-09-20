package cn.zszj.module.system.enums.membership;

import cn.zszj.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 任职生命周期动作枚举类
 *
 * <p>ZS-IAM-002（D-09 FND-IAM-003）：入职、转岗、停用、复职、离职、过期、变更均以流水形式
 * 记入 {@code system_membership_history}，承载历史归属，只增不改。
 *
 * @author ZS-IAM-002
 */
@Getter
@AllArgsConstructor
public enum MembershipActionEnum implements ArrayValuable<Integer> {

    CREATE(1, "入职"),
    TRANSFER(2, "转岗"),
    SUSPEND(3, "停用"),
    RESUME(4, "复职"),
    TERMINATE(5, "离职"),
    EXPIRE(6, "过期"),
    UPDATE(7, "变更");

    /**
     * 动作值
     */
    private final Integer action;
    /**
     * 动作名
     */
    private final String name;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(MembershipActionEnum::getAction).toArray(Integer[]::new);

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
