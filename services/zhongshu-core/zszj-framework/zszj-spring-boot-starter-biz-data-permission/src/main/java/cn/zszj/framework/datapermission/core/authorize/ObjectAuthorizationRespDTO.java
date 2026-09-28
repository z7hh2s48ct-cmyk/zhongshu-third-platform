package cn.zszj.framework.datapermission.core.authorize;

import lombok.Data;

import java.util.Set;

/**
 * 对象授权裁决输出（ZS-PERM-003.A；maskedFields 扩展 ZS-PERM-003.B / D-12 §5.2）：Web/移动端只消费
 * 本结果，执行侧经
 * {@link ObjectAuthorizationService#checkActionAllowed}/{@link ObjectAuthorizationService#checkFieldsAllowed}
 * 共用同一裁决核心（前端伪造本结果不生效）。
 *
 * @author ZS-PERM-003.A
 * @author ZS-PERM-003.B
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

    /**
     * 脱敏字段集合（ZS-PERM-003.B / D-12 §5.2）：authorizedFields 的子集，消费方（前端/导出）对这些
     * 字段只得以脱敏形态输出，不得以明文落详情/导出/文件；authorizedFields 为 null 时本字段亦为 null，
     * 非 null 时恒非 null（空集=无脱敏字段）。
     */
    private Set<String> maskedFields;

}
