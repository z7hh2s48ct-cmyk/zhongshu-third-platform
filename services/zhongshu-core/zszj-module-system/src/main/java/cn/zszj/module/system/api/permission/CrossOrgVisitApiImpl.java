package cn.zszj.module.system.api.permission;

import cn.zszj.framework.common.biz.system.permission.CrossOrgVisitApi;
import cn.zszj.framework.common.biz.system.permission.dto.CrossOrgVisitCheckReqDTO;
import cn.zszj.framework.common.biz.system.permission.dto.CrossOrgVisitDecisionDTO;
import cn.zszj.module.system.service.permission.CrossOrgVisitService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

/**
 * 跨组织访问授权 API 实现类（ZS-SEC-001.B）。
 *
 * <p>框架 SPI {@link CrossOrgVisitApi}（zszj-common）的 module-system 门面实现，循 {@link PermissionApiImpl} 范式，
 * 委托 {@link CrossOrgVisitService#authorizeVisit}。框架级 {@code TenantVisitContextInterceptor} 经
 * {@code ObjectProvider<CrossOrgVisitApi>} 可选注入本 Bean；未装配时拦截器 fail-closed 拒绝跨组织切换。
 *
 * @author ZS-SEC-001.B
 */
@Service
public class CrossOrgVisitApiImpl implements CrossOrgVisitApi {

    @Resource
    private CrossOrgVisitService crossOrgVisitService;

    @Override
    public CrossOrgVisitDecisionDTO authorizeCrossOrgVisit(CrossOrgVisitCheckReqDTO req) {
        return crossOrgVisitService.authorizeVisit(req);
    }

}
