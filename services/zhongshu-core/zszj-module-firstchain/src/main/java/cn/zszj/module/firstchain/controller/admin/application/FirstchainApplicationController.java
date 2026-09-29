package cn.zszj.module.firstchain.controller.admin.application;

import cn.zszj.framework.common.pojo.CommonResult;
import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.module.firstchain.controller.admin.application.vo.ApplicationApproveReqVO;
import cn.zszj.module.firstchain.controller.admin.application.vo.ApplicationCreateReqVO;
import cn.zszj.module.firstchain.controller.admin.application.vo.ApplicationPageReqVO;
import cn.zszj.module.firstchain.controller.admin.application.vo.ApplicationRejectReqVO;
import cn.zszj.module.firstchain.controller.admin.application.vo.ApplicationRespVO;
import cn.zszj.module.firstchain.controller.admin.application.vo.ApplicationSubmitReqVO;
import cn.zszj.module.firstchain.controller.admin.application.vo.ApplicationWithdrawReqVO;
import cn.zszj.module.firstchain.service.application.FirstchainApplicationService;
import cn.zszj.module.firstchain.service.opening.FirstchainOpeningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.zszj.framework.common.pojo.CommonResult.success;

/**
 * 首链申请域管理端 Controller（ZS-FC-001，PILOT-REQ-001~004 服务端 REST 面；D-07 M2 权限串落点）。
 *
 * <p>RED 骨架：端点体 {@code UnsupportedOperationException}；动作级权限串
 * {@code @PreAuthorize("@ss.hasPermission('firstchain:application:xxx')")}（M2 三角色矩阵，
 * 接入合同 §1.6「无注解写入口为零」）随 GREEN 提交补齐——
 * {@code FirstchainApplicationControllerTest} 源反射扫描用例据此先红后绿。
 *
 * @author ZS-FC-001
 */
@Tag(name = "管理后台 - 首链加盟商申请")
@RestController
@RequestMapping("/firstchain/application")
public class FirstchainApplicationController {

    @Resource
    private FirstchainApplicationService applicationService;

    @Resource
    private FirstchainOpeningService openingService;

    @PostMapping("/create")
    @Operation(summary = "创建加盟商申请（草稿基线，申请编号服务端生成）")
    public CommonResult<Long> createApplication(@Valid @RequestBody ApplicationCreateReqVO createReqVO) {
        throw new UnsupportedOperationException("ZS-FC-001 GREEN 待实现");
    }

    @PostMapping("/submit")
    @Operation(summary = "提交申请（DRAFT→SUBMITTED 并发起审批流）")
    public CommonResult<Boolean> submitApplication(@Valid @RequestBody ApplicationSubmitReqVO submitReqVO) {
        throw new UnsupportedOperationException("ZS-FC-001 GREEN 待实现");
    }

    @PostMapping("/approve")
    @Operation(summary = "审批通过并幂等开通（PILOT-REQ-002/003）")
    public CommonResult<Boolean> approveApplication(@Valid @RequestBody ApplicationApproveReqVO approveReqVO) {
        throw new UnsupportedOperationException("ZS-FC-001 GREEN 待实现");
    }

    @PostMapping("/reject")
    @Operation(summary = "审批拒绝（意见必填，不建任何主体）")
    public CommonResult<Boolean> rejectApplication(@Valid @RequestBody ApplicationRejectReqVO rejectReqVO) {
        throw new UnsupportedOperationException("ZS-FC-001 GREEN 待实现");
    }

    @PostMapping("/withdraw")
    @Operation(summary = "撤回审批流（领域状态不变，可重新发起）")
    public CommonResult<Boolean> withdrawApplication(@Valid @RequestBody ApplicationWithdrawReqVO withdrawReqVO) {
        throw new UnsupportedOperationException("ZS-FC-001 GREEN 待实现");
    }

    @GetMapping("/page")
    @Operation(summary = "获得加盟商申请分页")
    public CommonResult<PageResult<ApplicationRespVO>> getApplicationPage(@Valid ApplicationPageReqVO pageReqVO) {
        throw new UnsupportedOperationException("ZS-FC-001 GREEN 待实现");
    }

    @GetMapping("/get")
    @Operation(summary = "获得加盟商申请详情")
    @Parameter(name = "id", description = "申请编号（数据库主键 ID）", required = true, example = "1024")
    public CommonResult<ApplicationRespVO> getApplication(@RequestParam("id") Long id) {
        throw new UnsupportedOperationException("ZS-FC-001 GREEN 待实现");
    }

}
