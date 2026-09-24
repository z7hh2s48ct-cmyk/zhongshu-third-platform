package cn.zszj.framework.tenant.core.web;

import cn.hutool.core.util.ObjUtil;
import cn.zszj.framework.common.biz.system.permission.CrossOrgVisitApi;
import cn.zszj.framework.common.biz.system.permission.dto.CrossOrgVisitCheckReqDTO;
import cn.zszj.framework.common.biz.system.permission.dto.CrossOrgVisitDecisionDTO;
import cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.security.core.util.CrossOrgVisitScopeHolder;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import cn.zszj.framework.tenant.config.TenantProperties;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.framework.web.core.util.WebFrameworkUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.servlet.HandlerInterceptor;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception0;

/**
 * 跨租户（跨组织）访问上下文拦截器。
 *
 * <p>ZS-SEC-001.A：默认（{@code zszj.tenant.visit-enable=false}）拒绝一切跨租户切换。
 * <p>ZS-SEC-001.B：显式开启后，<b>不再</b>以旧 {@code system:tenant:visit} 粗粒度权限整体放大
 * （{@code skipPermissionCheck()→return true}），改为经 {@link CrossOrgVisitApi} 以服务端授权记录/策略
 * 裁决目标租户合法性与受控范围；获批才切换租户并把范围快照写入 {@code LoginUser} 上下文，供功能权限
 * （{@code SecurityFrameworkServiceImpl}）与对象/字段访问点按范围收敛裁决（docs/05 line330-331）。
 * 无授权实现（provider 为空）时 fail-closed 拒绝，绝不放大。
 *
 * @author ZS-SEC-001.A / ZS-SEC-001.B
 */
@RequiredArgsConstructor
@Slf4j
public class TenantVisitContextInterceptor implements HandlerInterceptor {

    private final TenantProperties tenantProperties;

    /**
     * 跨组织访问授权 SPI（ZS-SEC-001.B）。经 {@link ObjectProvider} 可选注入：实现落 module-system，
     * 框架 starter 不硬依赖；无实现（provider 为空）时 fail-closed 拒绝，绝不放大为旧 skipPermissionCheck。
     */
    private final ObjectProvider<CrossOrgVisitApi> crossOrgVisitApiProvider;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 如果和当前租户编号一致，则直接跳过
        Long visitTenantId = WebFrameworkUtils.getVisitTenantId(request);
        if (visitTenantId == null) {
            return true;
        }
        if (ObjUtil.equal(visitTenantId, TenantContextHolder.getTenantId())) {
            return true;
        }
        // ZS-SEC-001.A：底座默认关闭未经批准的跨租户浏览能力。未显式开启（zszj.tenant.visit-enable=false，默认）时，
        // 任何切换到不同目标租户的请求（含伪造头、含持有旧 system:tenant:visit 权限者）一律拒绝，且不设置 visitTenantId、
        // 不切换 TenantContextHolder，使 SecurityFrameworkUtils#skipPermissionCheck() 恒为 false，功能权限与数据范围
        // 均按登录租户正常校验，杜绝越权放大。获批的受控跨组织访问由 ZS-SEC-001.B（依赖 D-09）实现。
        if (!Boolean.TRUE.equals(tenantProperties.getVisitEnable())) {
            log.warn("[preHandle][跨租户访问能力未启用，拒绝切换到 visitTenantId({})，当前租户({})]",
                    visitTenantId, TenantContextHolder.getTenantId());
            throw exception0(GlobalErrorCodeConstants.FORBIDDEN.getCode(), "跨租户访问能力未启用，禁止切换租户");
        }
        // 必须是登录用户
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser == null) {
            return true;
        }

        // ZS-SEC-001.B：以服务端授权记录/策略取代旧 system:tenant:visit 粗粒度权限放大（docs/05 line330-331）。
        // 无授权实现（module-system 未装配）+ visit-enable=true → fail-closed 拒绝，绝不放大。
        CrossOrgVisitApi crossOrgVisitApi = crossOrgVisitApiProvider.getIfAvailable();
        if (crossOrgVisitApi == null) {
            log.warn("[preHandle][跨组织授权服务不可用，拒绝切换到 visitTenantId({})，visitorUserId({})]",
                    visitTenantId, loginUser.getId());
            throw exception0(GlobalErrorCodeConstants.FORBIDDEN.getCode(), "跨组织授权服务不可用，禁止切换租户");
        }
        // 目标级粗粒度门：action/objectOrgId/requestedFields 留空，具体动作/对象/字段由业务访问点二次裁决（D6/D7）
        CrossOrgVisitCheckReqDTO checkReq = new CrossOrgVisitCheckReqDTO();
        checkReq.setVisitorUserId(loginUser.getId());
        checkReq.setVisitorTenantId(loginUser.getTenantId());
        checkReq.setTargetTenantId(visitTenantId);
        CrossOrgVisitDecisionDTO decision = crossOrgVisitApi.authorizeCrossOrgVisit(checkReq);
        if (decision == null || !decision.isAuthorized()) {
            // DENY：不切换（不设 visitTenantId、不切 TenantContextHolder）；审计已由 CrossOrgVisitService 落
            log.warn("[preHandle][跨组织访问被拒绝 visitorUserId({}) targetTenantId({}) reason({})]",
                    loginUser.getId(), visitTenantId, decision != null ? decision.getReason() : "NO_DECISION");
            throw exception0(GlobalErrorCodeConstants.FORBIDDEN.getCode(), "您无权跨组织访问目标租户");
        }

        // 【重点】获批：切换租户编号 + 写入受控授权范围快照（供功能权限/对象/字段访问层按范围裁决，非整体跳过）
        loginUser.setVisitTenantId(visitTenantId);
        TenantContextHolder.setTenantId(visitTenantId);
        CrossOrgVisitScopeHolder.setScope(decision);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 【重点】清理切换，换回原租户编号
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser != null && loginUser.getTenantId() != null) {
            TenantContextHolder.setTenantId(loginUser.getTenantId());
        }
        // ZS-SEC-001.B：清理授权范围快照（docs/05 line331「授权撤销与上下文清理有效」，防跨请求残留）
        CrossOrgVisitScopeHolder.clear();
    }

}
