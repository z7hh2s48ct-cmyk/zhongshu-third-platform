package cn.zszj.framework.security.fixture;

import cn.zszj.framework.common.biz.system.permission.CrossOrgVisitApi;
import cn.zszj.framework.common.biz.system.permission.dto.CrossOrgVisitCheckReqDTO;
import cn.zszj.framework.common.biz.system.permission.dto.CrossOrgVisitDecisionDTO;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * ZS-SEC-001.B：Mock {@link CrossOrgVisitApi}，为跨租户访问夹具提供确定性的「服务端授权记录/策略」裁决。
 *
 * <p>取代旧 {@code system:tenant:visit} 粗粒度权限放大：夹具不再凭权限整体跳过校验，而是模拟
 * {@code CrossOrgVisitServiceImpl} 的裁决结论——
 * <ul>
 *   <li>{@code USER_T1_VISITOR(104) → TENANT_2}：GRANT（模拟存在 ACTIVE 授权记录 + 平台角色 + 目标启用），
 *       受控范围 {@code allowedActions={system:user:query}}、{@code allowedFields={contactMobile}}、
 *       {@code targetOrgIds=null}（whole-tenant）。动作范围<b>仅含 query</b>，用于验证 D6 动作收敛：
 *       visit 上下文下 {@code system:user:create} 不在范围内 → 拒绝（不再整体放大）。</li>
 *   <li>其它主体/目标：DENY（{@code NO_GRANT}，无授权记录 → fail-closed）。</li>
 * </ul>
 *
 * <p>该 Mock 不持久化状态，仅提供确定性映射，使夹具专注于拦截器 + 功能权限收敛的运行时行为。
 *
 * @author ZS-SEC-001.B
 */
public class MockCrossOrgVisitApi implements CrossOrgVisitApi {

    /** 获批访问者访问 TENANT_2 时授予的动作范围（仅 query，验证 D6 动作收敛：create 不放行） */
    public static final String GRANTED_ACTION = "system:user:query";
    /** 获批访问者被授予的字段范围 */
    public static final String GRANTED_FIELD = "contactMobile";

    @Override
    public CrossOrgVisitDecisionDTO authorizeCrossOrgVisit(CrossOrgVisitCheckReqDTO req) {
        // 仅 USER_T1_VISITOR(104) → TENANT_2 获批（模拟存在 ACTIVE 授权记录 + 平台角色 + 目标启用 + 窗口内）
        if (req != null
                && MockOAuth2TokenApi.USER_T1_VISITOR.equals(req.getVisitorUserId())
                && MockOAuth2TokenApi.TENANT_2.equals(req.getTargetTenantId())) {
            CrossOrgVisitDecisionDTO decision = new CrossOrgVisitDecisionDTO();
            decision.setAuthorized(true);
            decision.setReason("AUTHORIZED");
            decision.setTargetTenantId(MockOAuth2TokenApi.TENANT_2);
            decision.setTargetOrgIds(null); // whole-tenant 授权（目标租户内全部组织）
            Set<String> actions = new LinkedHashSet<>();
            actions.add(GRANTED_ACTION);
            decision.setAllowedActions(actions);
            Set<String> fields = new LinkedHashSet<>();
            fields.add(GRANTED_FIELD);
            decision.setAllowedFields(fields);
            return decision;
        }
        // 其它主体/目标：无授权记录 → fail-closed 拒绝（取代旧 system:tenant:visit 放大）
        return CrossOrgVisitDecisionDTO.denied("NO_GRANT");
    }
}
