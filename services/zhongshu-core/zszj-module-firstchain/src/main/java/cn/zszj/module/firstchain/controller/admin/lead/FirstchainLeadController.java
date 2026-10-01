package cn.zszj.module.firstchain.controller.admin.lead;

import cn.zszj.framework.common.pojo.CommonResult;
import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.module.firstchain.controller.admin.lead.vo.LeadAssignReqVO;
import cn.zszj.module.firstchain.controller.admin.lead.vo.LeadClaimReqVO;
import cn.zszj.module.firstchain.controller.admin.lead.vo.LeadConvertReqVO;
import cn.zszj.module.firstchain.controller.admin.lead.vo.LeadDistributeReqVO;
import cn.zszj.module.firstchain.controller.admin.lead.vo.LeadFollowupReqVO;
import cn.zszj.module.firstchain.controller.admin.lead.vo.LeadInvalidateReqVO;
import cn.zszj.module.firstchain.controller.admin.lead.vo.LeadPageReqVO;
import cn.zszj.module.firstchain.controller.admin.lead.vo.LeadReassignReqVO;
import cn.zszj.module.firstchain.controller.admin.lead.vo.LeadRespVO;
import cn.zszj.module.firstchain.service.lead.FirstchainLeadAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static cn.zszj.framework.common.pojo.CommonResult.success;

/**
 * 首链线索域管理端 Controller（ZS-FC-002 REST wave，PILOT-REQ-005~009 REST 面；D-07 M6/M7 权限串落点）。
 *
 * <p>动作级权限串 {@code @PreAuthorize("@ss.hasPermission('firstchain:lead:xxx')")}
 * （接入合同 §1.6「无注解写入口为零」；M2 三角色矩阵——distribute 面向平台运营，
 * assign/reassign 面向加盟商负责人，claim/followup/convert/invalidate 面向员工与负责人，
 * query 全矩阵；对象级资格〔视角与归属〕由 {@link FirstchainLeadAppService} 服务层二次校验，
 * 两层不可互相替代）。写侧操作人/领取人/跟进人一律取登录上下文，Controller 不接受请求体声明
 * 操作人（防伪造主体）；查询视角由服务端解析，请求不携带归属/分配人参数。
 *
 * @author ZS-FC-002
 */
@Tag(name = "管理后台 - 首链线索")
@RestController
@RequestMapping("/firstchain/lead")
public class FirstchainLeadController {

    @Resource
    private FirstchainLeadAppService leadAppService;

    @PostMapping("/distribute")
    @Operation(summary = "下发线索（平台运营；线索编号服务端生成，归属组织服务端写入）")
    @PreAuthorize("@ss.hasPermission('firstchain:lead:distribute')")
    public CommonResult<Long> distributeLead(@Valid @RequestBody LeadDistributeReqVO distributeReqVO) {
        return success(leadAppService.distributeLead(distributeReqVO));
    }

    @PostMapping("/assign")
    @Operation(summary = "分配线索（负责人分配本组织员工，DISTRIBUTED→ASSIGNED）")
    @PreAuthorize("@ss.hasPermission('firstchain:lead:assign')")
    public CommonResult<Boolean> assignLead(@Valid @RequestBody LeadAssignReqVO assignReqVO) {
        leadAppService.assignLead(assignReqVO);
        return success(true);
    }

    @PostMapping("/claim")
    @Operation(summary = "领取线索（员工领取分配给自己的线索，ASSIGNED→FOLLOWING）")
    @PreAuthorize("@ss.hasPermission('firstchain:lead:claim')")
    public CommonResult<Boolean> claimLead(@Valid @RequestBody LeadClaimReqVO claimReqVO) {
        leadAppService.claimLead(claimReqVO);
        return success(true);
    }

    @PostMapping("/reassign")
    @Operation(summary = "改派线索（负责人改派，状态不变推版本）")
    @PreAuthorize("@ss.hasPermission('firstchain:lead:reassign')")
    public CommonResult<Boolean> reassignLead(@Valid @RequestBody LeadReassignReqVO reassignReqVO) {
        leadAppService.reassignLead(reassignReqVO);
        return success(true);
    }

    @PostMapping("/followup")
    @Operation(summary = "追加跟进记录（仅被分配员工本人，追加式无修改通道）")
    @PreAuthorize("@ss.hasPermission('firstchain:lead:followup')")
    public CommonResult<Long> followupLead(@Valid @RequestBody LeadFollowupReqVO followupReqVO) {
        return success(leadAppService.followupLead(followupReqVO));
    }

    @PostMapping("/convert")
    @Operation(summary = "转商机（被分配员工发起，FOLLOWING→CONVERTED 终态）")
    @PreAuthorize("@ss.hasPermission('firstchain:lead:convert')")
    public CommonResult<Long> convertLead(@Valid @RequestBody LeadConvertReqVO convertReqVO) {
        return success(leadAppService.convertLead(convertReqVO));
    }

    @PostMapping("/invalidate")
    @Operation(summary = "无效关闭（员工发起或负责人代操作，原因枚举必填，FOLLOWING→INVALID 终态）")
    @PreAuthorize("@ss.hasPermission('firstchain:lead:invalidate')")
    public CommonResult<Boolean> invalidateLead(@Valid @RequestBody LeadInvalidateReqVO invalidateReqVO) {
        leadAppService.invalidateLead(invalidateReqVO);
        return success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "获得线索分页（视角服务端解析：平台=授权范围/负责人=本组织/员工=本人）")
    @PreAuthorize("@ss.hasPermission('firstchain:lead:query')")
    public CommonResult<PageResult<LeadRespVO>> getLeadPage(@Valid LeadPageReqVO pageReqVO) {
        return success(leadAppService.getLeadPage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得线索详情（越权显式拒绝；D-12 字段裁决出口）")
    @Parameter(name = "id", description = "线索编号（数据库主键 ID）", required = true, example = "2048")
    @PreAuthorize("@ss.hasPermission('firstchain:lead:query')")
    public CommonResult<LeadRespVO> getLead(@RequestParam("id") Long id) {
        return success(leadAppService.getLead(id));
    }

    @GetMapping("/metrics")
    @Operation(summary = "获得线索状态指标（三视角同源：权威状态列 GROUP BY）")
    @PreAuthorize("@ss.hasPermission('firstchain:lead:query')")
    public CommonResult<Map<String, Long>> getLeadStatusMetrics() {
        return success(leadAppService.getLeadStatusMetrics());
    }

}
