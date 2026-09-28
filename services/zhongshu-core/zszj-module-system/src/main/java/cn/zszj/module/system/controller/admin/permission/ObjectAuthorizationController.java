package cn.zszj.module.system.controller.admin.permission;

import cn.zszj.framework.common.pojo.CommonResult;
import cn.zszj.framework.datapermission.core.authorize.ObjectAuthorizationRequest;
import cn.zszj.framework.datapermission.core.authorize.ObjectAuthorizationRespDTO;
import cn.zszj.framework.datapermission.core.authorize.ObjectAuthorizationService;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.zszj.framework.common.pojo.CommonResult.success;

/**
 * ZS-CLIENT-001.B：对象授权输出 管理后台 Controller——前端消费统一「动作 / 字段」授权的唯一入口。
 *
 * <p>把 ZS-PERM-003.A/.B 统一裁决输出（{@code allowedActions}/{@code authorizedFields}/
 * {@code maskedFields}）暴露给 Web/移动端消费：客户端只消费本结果渲染按钮与字段
 * （动作显隐 / 字段 hidden-masked-clear 三态），<b>不做任何授权推导</b>；动作执行由
 * {@code ObjectAuthorizationService#checkActionAllowed}/{@code #checkFieldsAllowed} 在业务入口
 * 服务端独立拒绝——「前端伪造动作不生效」为结构保证（伪造本地快照不改变服务端裁决）。
 *
 * <p>零输出语义（fail-closed）：未登录 / 空白 objectType / 未接入对象类型（裁决返回 null）/
 * 统一裁决服务未装配，一律返回 {@code success(null)}——客户端按「未接入域」处理（动作不可用、
 * 字段不展示），与渐进接入合同一致。已注册但对象不可见的域由裁决核心以 {@code FORBIDDEN} 显式
 * 拒绝（PERM-003.A 既有行为，fail-closed）——注册状态仅可经该拒绝路径间接推断（输出始终为
 * 调用者自身的授权，不泄露他人授权与业务数据）。visit 请求的字段维收敛由裁决核心内部完成
 * （授权记录为唯一权威），本端点只透传。
 *
 * @author ZS-CLIENT-001.B
 */
@Tag(name = "管理后台 - 对象授权输出")
@RestController
@RequestMapping("/system/object-authorization")
public class ObjectAuthorizationController {

    /**
     * 统一裁决服务：{@code required=false}——未装配 biz-data-permission 的窄上下文返回
     * {@code success(null)}（消费端零输出），不阻断启动。
     */
    @Autowired(required = false)
    private ObjectAuthorizationService objectAuthorizationService;

    @GetMapping("/get")
    @Operation(summary = "获得对象授权输出（允许动作 / 授权字段 / 脱敏字段）")
    @Parameters({
            @Parameter(name = "objectType", description = "对象类型标识（空白按未接入域返回 null）", required = false),
            @Parameter(name = "orgId", description = "对象所属组织编号"),
            @Parameter(name = "ownerUserId", description = "对象负责人用户编号")
    })
    public CommonResult<ObjectAuthorizationRespDTO> getObjectAuthorization(
            @RequestParam(value = "objectType", required = false) String objectType,
            @RequestParam(value = "orgId", required = false) Long orgId,
            @RequestParam(value = "ownerUserId", required = false) Long ownerUserId) {
        // 1. 未登录（异常态）与空白 objectType：零输出，不触达裁决
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser == null || objectType == null || objectType.isBlank()) {
            return success(null);
        }
        // 2. 裁决服务未装配（窄上下文）：零输出，不报 500
        if (objectAuthorizationService == null) {
            return success(null);
        }
        // 3. 委派统一裁决：未注册对象类型返回 null（未接入=零变化），由客户端按未接入域消费
        return success(objectAuthorizationService.authorize(
                ObjectAuthorizationRequest.of(objectType, orgId, ownerUserId)));
    }

}
