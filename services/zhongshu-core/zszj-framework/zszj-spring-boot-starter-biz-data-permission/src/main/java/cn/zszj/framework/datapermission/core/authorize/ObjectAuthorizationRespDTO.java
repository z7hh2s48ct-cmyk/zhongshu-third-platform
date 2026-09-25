package cn.zszj.framework.datapermission.core.authorize;

import lombok.Data;

import java.util.Set;

/**
 * 对象授权裁决输出（ZS-PERM-003.A）：Web/移动端只消费本结果，执行侧经
 * {@link ObjectAuthorizationService#checkActionAllowed}/{@link ObjectAuthorizationService#checkFieldsAllowed}
 * 共用同一裁决核心（前端伪造本结果不生效）。
 *
 * @author ZS-PERM-003.A
 */
@Data
public class ObjectAuthorizationRespDTO {

    /**
     * 允许动作集合（恒非 null；空集=无任何动作可用）
     */
    private Set<String> allowedActions;

    /**
     * 授权字段集合；<b>null=未启用字段级输出</b>（provider 未声明候选字段，零变化），
     * 非 null（可为空集）=已启用，空集=全部候选字段均不可见
     */
    private Set<String> authorizedFields;

}
