package cn.zszj.framework.common.biz.system.permission.dto;

import lombok.Data;

import java.util.List;

/**
 * 跨组织访问授权校验 Request DTO（ZS-SEC-001.B）。
 *
 * <p>承载「受控跨组织访问」判定所需的原主体、目标与本次访问意图。由框架级
 * {@code TenantVisitContextInterceptor}（目标级粗粒度门，{@code action}/{@code objectOrgId}/{@code requestedFields}
 * 可为空）与业务访问点（携带具体动作/对象/字段）构造，交 {@code CrossOrgVisitApi#authorizeCrossOrgVisit} 裁决。
 *
 * <p>D-09 一期口径：跨组织只允许显式平台角色；本 DTO 不携带任何客户端可伪造的授权结论，
 * 授权范围一律由服务端授权记录（{@code system_cross_org_visit_grant}）派生。
 *
 * @author ZS-SEC-001.B
 */
@Data
public class CrossOrgVisitCheckReqDTO {

    /**
     * 原主体：发起跨组织访问的账号（用户）编号
     */
    private Long visitorUserId;
    /**
     * 原主体所属（home）技术租户编号
     */
    private Long visitorTenantId;
    /**
     * 目标租户编号
     */
    private Long targetTenantId;
    /**
     * 本次访问的动作（权限标识，如 {@code system:user:query}）；为空表示目标级粗粒度门，不校验动作维度
     */
    private String action;
    /**
     * 本次访问对象的所属组织编号；为空表示不校验对象维度
     */
    private Long objectOrgId;
    /**
     * 本次访问请求的字段集合；为空表示不校验字段维度
     */
    private List<String> requestedFields;

}
