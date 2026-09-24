package cn.zszj.framework.common.biz.system.permission;

import cn.zszj.framework.common.biz.system.permission.dto.CrossOrgVisitCheckReqDTO;
import cn.zszj.framework.common.biz.system.permission.dto.CrossOrgVisitDecisionDTO;

/**
 * 跨组织访问授权 API 接口（ZS-SEC-001.B）。
 *
 * <p>以<b>服务端授权记录/策略</b>（{@code system_cross_org_visit_grant}）取代 ZS-SEC-001.A 关闭的旧越权放大链
 * （持 {@code system:tenant:visit} 即 {@code skipPermissionCheck()=true} 整体跳过功能权限）。落实 docs/05 line330
 * 「获批后以服务端授权记录/策略限制目标租户、对象、动作、字段和有效期，仍执行必要授权与目标状态校验，
 * 记录原主体、目标、理由和结果」与 D-09「一期跨组织只允许显式平台角色」。
 *
 * <p>接口置于 zszj-common（framework 层，循 {@link PermissionCommonApi}、
 * {@link cn.zszj.framework.common.biz.system.audit.AuditPort} 先例），实现落 module-system；
 * 框架级 {@code TenantVisitContextInterceptor} 经 {@code ObjectProvider} 可选注入，无实现时 fail-closed 拒绝。
 *
 * @author ZS-SEC-001.B
 */
public interface CrossOrgVisitApi {

    /**
     * 裁决一次跨组织访问请求：校验授权记录（存在/未撤销/在有效期）、D-09 平台角色资格、目标状态，
     * 并按记录限定动作/对象/字段。每次判定（获批或拒绝）都落不可改写审计（原主体/目标/理由/结果）。
     *
     * @param req 跨组织访问校验请求
     * @return 判定结论 + 获批范围快照
     */
    CrossOrgVisitDecisionDTO authorizeCrossOrgVisit(CrossOrgVisitCheckReqDTO req);

}
