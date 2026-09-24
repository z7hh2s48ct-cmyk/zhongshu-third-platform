package cn.zszj.module.system.enums.permission;

import cn.zszj.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 跨组织访问授权状态枚举（ZS-SEC-001.B）。
 *
 * <p>D-09 一期口径：跨组织授权以服务端记录（{@code system_cross_org_visit_grant}）承载，
 * 人工发放（ACTIVE）/人工撤销（REVOKED）。到期自动回收 JOB 与双人审批工作流为<b>后置边界</b>，本卡不实现。
 * 撤销采逻辑状态翻转（不改历史行），判定序对 REVOKED 精确回报「已撤销」而非「无记录」。
 *
 * @author ZS-SEC-001.B
 */
@Getter
@AllArgsConstructor
public enum CrossOrgVisitStatusEnum implements ArrayValuable<Integer> {

    /**
     * 生效：授权记录在有效期内且未被撤销，方可进入后续维度裁决
     */
    ACTIVE(0, "生效"),
    /**
     * 已撤销：人工撤销后置此状态，判定序直接拒绝（GRANT_REVOKED）
     */
    REVOKED(1, "已撤销");

    /**
     * 状态值
     */
    private final Integer status;
    /**
     * 状态名
     */
    private final String name;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(CrossOrgVisitStatusEnum::getStatus).toArray(Integer[]::new);

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    public static boolean isActive(Integer status) {
        return ACTIVE.getStatus().equals(status);
    }

}
