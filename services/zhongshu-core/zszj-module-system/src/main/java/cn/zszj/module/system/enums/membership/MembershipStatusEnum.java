package cn.zszj.module.system.enums.membership;

import cn.zszj.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 任职状态枚举类
 *
 * <p>ZS-IAM-002（D-09 FND-IAM-003）：Membership 含组织、岗位、角色、状态与有效期。
 * 仅 ACTIVE 任职可进入服务端组织上下文；SUSPENDED/EXPIRED/TERMINATED 均被上下文解析器拒绝，
 * 但保留行以承载历史归属（不物理删除）。
 *
 * @author ZS-IAM-002
 */
@Getter
@AllArgsConstructor
public enum MembershipStatusEnum implements ArrayValuable<Integer> {

    ACTIVE(1, "在职"),
    SUSPENDED(2, "停用"),
    EXPIRED(3, "过期"),
    TERMINATED(4, "离职");

    /**
     * 状态值
     */
    private final Integer status;
    /**
     * 状态名
     */
    private final String name;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(MembershipStatusEnum::getStatus).toArray(Integer[]::new);

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    public static boolean isActive(Integer status) {
        return ACTIVE.getStatus().equals(status);
    }

}
