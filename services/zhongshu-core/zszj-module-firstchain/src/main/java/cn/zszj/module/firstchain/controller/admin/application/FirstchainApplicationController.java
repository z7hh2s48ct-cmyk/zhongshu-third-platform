package cn.zszj.module.firstchain.controller.admin.application;

import cn.zszj.framework.common.pojo.CommonResult;
import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
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
import org.springframework.security.access.prepost.PreAuthorize;
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
 * <p>动作级权限串 {@code @PreAuthorize("@ss.hasPermission('firstchain:application:xxx')")}
 * （接入合同 §1.6「无注解写入口为零」；M2 三角色矩阵——create/submit 面向申请方组织，
 * approve/reject 面向平台运营，query 全矩阵；对象级资格〔审批人=任意 PLATFORM 有效任职〕
 * 由 {@code FirstchainOpeningService} 服务层二次校验，两层不可互相替代）。
 * 写侧操作人取登录上下文，Controller 不接受请求体声明操作人（防伪造主体）。
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
    @PreAuthorize("@ss.hasPermission('firstchain:application:create')")
    public CommonResult<Long> createApplication(@Valid @RequestBody ApplicationCreateReqVO createReqVO) {
        return success(applicationService.createApplication(createReqVO));
    }

    @PostMapping("/submit")
    @Operation(summary = "提交申请（DRAFT→SUBMITTED 并发起审批流）")
    @PreAuthorize("@ss.hasPermission('firstchain:application:submit')")
    public CommonResult<Boolean> submitApplication(@Valid @RequestBody ApplicationSubmitReqVO submitReqVO) {
        applicationService.submitApplication(submitReqVO);
        return success(true);
    }

    @PostMapping("/approve")
    @Operation(summary = "审批通过并幂等开通（PILOT-REQ-002/003）")
    @PreAuthorize("@ss.hasPermission('firstchain:application:approve')")
    public CommonResult<Long> approveApplication(@Valid @RequestBody ApplicationApproveReqVO approveReqVO) {
        return success(openingService.approveAndOpen(FirstchainOpeningService.ApproveCmd.builder()
                .appKey(approveReqVO.getAppKey())
                .reason(approveReqVO.getReason())
                .actorId(currentActorId())
                .operatorUserId(SecurityFrameworkUtils.getLoginUserId())
                .build()));
    }

    @PostMapping("/reject")
    @Operation(summary = "审批拒绝（意见必填，不建任何主体）")
    @PreAuthorize("@ss.hasPermission('firstchain:application:reject')")
    public CommonResult<Boolean> rejectApplication(@Valid @RequestBody ApplicationRejectReqVO rejectReqVO) {
        openingService.reject(FirstchainOpeningService.ApproveCmd.builder()
                .appKey(rejectReqVO.getAppKey())
                .reason(rejectReqVO.getReason())
                .actorId(currentActorId())
                .operatorUserId(SecurityFrameworkUtils.getLoginUserId())
                .build());
        return success(true);
    }

    @PostMapping("/withdraw")
    @Operation(summary = "撤回审批流（领域状态不变，可重新发起）")
    @PreAuthorize("@ss.hasPermission('firstchain:application:withdraw')")
    public CommonResult<Boolean> withdrawApplication(@Valid @RequestBody ApplicationWithdrawReqVO withdrawReqVO) {
        applicationService.withdrawApproval(withdrawReqVO);
        return success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "获得加盟商申请分页")
    @PreAuthorize("@ss.hasPermission('firstchain:application:query')")
    public CommonResult<PageResult<ApplicationRespVO>> getApplicationPage(@Valid ApplicationPageReqVO pageReqVO) {
        return success(applicationService.getApplicationPage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得加盟商申请详情")
    @Parameter(name = "id", description = "申请编号（数据库主键 ID）", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('firstchain:application:query')")
    public CommonResult<ApplicationRespVO> getApplication(@RequestParam("id") Long id) {
        return success(applicationService.getApplication(id));
    }

    private static String currentActorId() {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        return loginUserId == null ? null : String.valueOf(loginUserId);
    }

}
