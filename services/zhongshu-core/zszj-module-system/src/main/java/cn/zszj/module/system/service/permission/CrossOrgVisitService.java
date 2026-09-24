package cn.zszj.module.system.service.permission;

import cn.zszj.framework.common.biz.system.permission.dto.CrossOrgVisitCheckReqDTO;
import cn.zszj.framework.common.biz.system.permission.dto.CrossOrgVisitDecisionDTO;
import cn.zszj.module.system.dal.dataobject.permission.CrossOrgVisitGrantDO;

/**
 * 跨组织访问授权 Service 接口（ZS-SEC-001.B）。
 *
 * <p>承载「受控跨组织授权」的裁决与人工发放/撤销：以服务端授权记录（{@link CrossOrgVisitGrantDO}）取代
 * ZS-SEC-001.A 关闭的旧越权放大链。到期自动回收 JOB 与双人审批工作流为 D-09 后置边界，本接口只交付
 * 记录式受控授权（人工发放/撤销 + 有效期 + 状态 + 逐次判定审计）。
 *
 * @author ZS-SEC-001.B
 */
public interface CrossOrgVisitService {

    /**
     * 裁决一次跨组织访问请求（正向矩阵核心，fail-closed）。
     *
     * <p>判定序：参数校验 → 取授权记录（无则拒，取代旧粗粒度权限）→ 状态（撤销拒绝）→ 有效期（过期拒绝）
     * → D-09 平台角色资格（须显式平台角色）→ 目标状态（停用/无效拒绝）→ 动作 → 对象 → 字段。
     * 任一不满足即拒绝；<b>每次判定（获批或拒绝）都落不可改写审计</b>（原主体/目标/理由/结果）。
     *
     * @param req 跨组织访问校验请求
     * @return 判定结论 + 获批范围快照
     */
    CrossOrgVisitDecisionDTO authorizeVisit(CrossOrgVisitCheckReqDTO req);

    /**
     * 发放跨组织访问授权（人工，非双人审批工作流）。
     *
     * <p>校验被授权访问者为显式平台角色、目标租户合法、有效期窗口合理；发放动作写审计（OBJECT_CREATED）。
     *
     * @param grant 授权记录（visitor/target/范围/有效期/理由）
     * @return 授权记录编号
     */
    Long createGrant(CrossOrgVisitGrantDO grant);

    /**
     * 撤销跨组织访问授权（逻辑状态翻转 ACTIVE→REVOKED，不改历史行）。
     *
     * @param id     授权记录编号
     * @param reason 撤销理由（不含敏感明文）
     */
    void revokeGrant(Long id, String reason);

}
