package cn.zszj.framework.common.biz.system.permission.dto;

import lombok.Data;

import java.util.Set;

/**
 * 跨组织访问授权判定 Response DTO（ZS-SEC-001.B）。
 *
 * <p>{@code CrossOrgVisitApi#authorizeCrossOrgVisit} 的返回：既表达判定结论（{@link #authorized} + {@link #reason}），
 * 又携带获批时派生自服务端授权记录的<b>受控范围快照</b>（{@link #targetTenantId}/{@link #targetOrgIds}/
 * {@link #allowedActions}/{@link #allowedFields}）。获批后由拦截器写入 {@code LoginUser} 上下文，
 * 供功能权限（{@code SecurityFrameworkServiceImpl}）与对象/字段访问点消费——落实 docs/05 line331
 * 「即使具备访问入口权限，也不能自动获得所有目标动作/敏感字段」。
 *
 * <p>{@link #targetOrgIds} 为 {@code null} 表示目标租户内全部组织（whole-tenant 授权）；
 * {@link #allowedActions}/{@link #allowedFields} 恒为显式集合（fail-closed：空集合即不允许任何动作/敏感字段）。
 *
 * @author ZS-SEC-001.B
 */
@Data
public class CrossOrgVisitDecisionDTO {

    /**
     * 是否获批
     */
    private boolean authorized;
    /**
     * 判定理由：获批为 {@code AUTHORIZED}；拒绝为 {@code CrossOrgVisitDenyReason} 枚举名
     * （如 {@code NO_GRANT}/{@code GRANT_EXPIRED}/{@code GRANT_REVOKED}/{@code NOT_PLATFORM_ROLE}/
     * {@code TARGET_INVALID}/{@code ACTION_NOT_ALLOWED}/{@code OBJECT_OUT_OF_SCOPE}/{@code FIELD_NOT_ALLOWED}）
     */
    private String reason;

    // ========== 获批范围快照（仅 authorized=true 时有意义） ==========
    /**
     * 目标租户编号
     */
    private Long targetTenantId;
    /**
     * 授权可见的目标组织编号集合；{@code null}=目标租户内全部组织
     */
    private Set<Long> targetOrgIds;
    /**
     * 授权允许的动作（权限标识）集合
     */
    private Set<String> allowedActions;
    /**
     * 授权允许访问的字段集合
     */
    private Set<String> allowedFields;

    public static CrossOrgVisitDecisionDTO denied(String reason) {
        CrossOrgVisitDecisionDTO dto = new CrossOrgVisitDecisionDTO();
        dto.setAuthorized(false);
        dto.setReason(reason);
        return dto;
    }

}
