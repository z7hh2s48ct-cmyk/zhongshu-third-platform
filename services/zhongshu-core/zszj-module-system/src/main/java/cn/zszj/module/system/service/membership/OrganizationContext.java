package cn.zszj.module.system.service.membership;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 服务端组织上下文
 *
 * <p>ZS-IAM-002（D-09 FND-IAM-004）：由服务端依据账号的默认任职解析得出，写入 token userInfo，
 * 客户端无法伪造。{@link #orgId} 为 null 表示「无默认任职」的降级上下文（兼容历史无部门账号），
 * 由调用方按业务决定放行或拒绝。
 *
 * @author ZS-IAM-002
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationContext {

    /**
     * 当前组织编号；无默认任职时为 null
     */
    private Long orgId;
    /**
     * 当前组织类型；关联 {@code OrganizationTypeEnum}
     */
    private Integer orgType;
    /**
     * 当前任职编号；无默认任职时为 null
     */
    private Long membershipId;

    /**
     * 无组织上下文的降级实例（账号无默认任职）
     */
    public static OrganizationContext empty() {
        return new OrganizationContext(null, null, null);
    }

    public boolean isEmpty() {
        return orgId == null;
    }

}
