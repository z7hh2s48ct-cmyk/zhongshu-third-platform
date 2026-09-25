package cn.zszj.module.system.controller.admin.permission;

import cn.zszj.framework.common.pojo.CommonResult;
import cn.zszj.framework.datapermission.core.annotation.DataPermission;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import cn.zszj.module.system.controller.admin.permission.vo.crossorgvisit.CrossOrgVisitMyTargetsRespVO;
import cn.zszj.module.system.service.permission.CrossOrgVisitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;

import static cn.zszj.framework.common.pojo.CommonResult.success;

/**
 * ZS-CLIENT-002.B：跨组织访问授权 管理后台 Controller——「我的授权目标」查询。
 *
 * <p>获批业务组织导航的服务端入口：返回当前登录主体可切换的跨组织访问目标（唯一数据源），
 * 客户端据此显隐租户切换入口并构建切换选项，不做任何授权推导。非平台角色（D-09 资格门）返回空
 * 目标列表。登录租户取 {@link LoginUser#getTenantId()}（登录主体所属租户，非访问态租户——
 * 与 {@code TenantVisitContextInterceptor} 的访客判定同源）。
 *
 * @author ZS-CLIENT-002.B
 */
@Tag(name = "管理后台 - 跨组织访问授权")
@RestController
@RequestMapping("/system/cross-org-visit")
public class CrossOrgVisitController {

    @Resource
    private CrossOrgVisitService crossOrgVisitService;

    @GetMapping("/my-targets")
    @Operation(summary = "获得我的跨组织访问授权目标列表")
    @DataPermission(enable = false) // 平台级跨租户授权记录（@TenantIgnore）自读，不叠加数据权限
    public CommonResult<CrossOrgVisitMyTargetsRespVO> getMyVisitTargets() {
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser == null) {
            return success(null);
        }
        return success(crossOrgVisitService.listMyAuthorizedTargets(loginUser.getId(), loginUser.getTenantId()));
    }

}
