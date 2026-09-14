package cn.zszj.module.infra.controller.admin.file;

import cn.zszj.framework.common.pojo.CommonResult;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileDeliveryChunkRespVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileDeliverySessionRespVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileDeliveryTicketIssueReqVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileDeliveryTicketIssueRespVO;
import cn.zszj.module.infra.service.file.FileDeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

import static cn.zszj.framework.common.pojo.CommonResult.success;

/**
 * 文件交付票据与鉴权取流（ZS-FILE-004.A）。
 *
 * <p>授权不在 @PreAuthorize 层：票据只签发给当前有权读该文件的主体（owner / 同租户
 * infra:file:query），兑换与逐块取流均由服务层做主体·租户·用途·会话·撤权重检——
 * 与 FILE-001.A 的既有后端鉴权下载端点同策。</p>
 *
 * <p>登录会话标识（{@code login-session} 头）：由客户端在登录后持有并在整个交付流程携带，
 * 兑换时写入会话绑定、取流时重检；真实登录态（token sessionId）的自动接线归 CLIENT-005.B 联调。</p>
 */
@Tag(name = "管理后台 - 文件交付票据与鉴权取流")
@RestController
@RequestMapping("/infra/file/delivery")
@Validated
public class FileDeliveryController {

    @Resource
    private FileDeliveryService fileDeliveryService;

    @PostMapping("/issue")
    @Operation(summary = "签发一次性交付票据", description = "本人主体绑定；服务端只存散列，token 仅此一次返回")
    public CommonResult<FileDeliveryTicketIssueRespVO> issueDeliveryTicket(
            @Valid @RequestBody FileDeliveryTicketIssueReqVO reqVO,
            @RequestHeader("login-session") String loginSession) {
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        return success(fileDeliveryService.issueDeliveryTicket(reqVO, loginUser, loginSession));
    }

    @PostMapping("/redeem")
    @Operation(summary = "原子兑换票据建立下载会话", description = "同主体同登录会话重复兑换幂等返回既有会话")
    public CommonResult<FileDeliverySessionRespVO> redeemDeliveryTicket(
            @RequestParam("ticketToken") String ticketToken,
            @RequestParam("purpose") String purpose,
            @RequestHeader("login-session") String loginSession) {
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        return success(fileDeliveryService.redeemDeliveryTicket(ticketToken, purpose, loginUser, loginSession));
    }

    @GetMapping("/chunk")
    @Operation(summary = "鉴权取流分块", description = "Range 语义 [start, endInclusive]；逐块重检身份/会话/撤权/过期")
    public CommonResult<FileDeliveryChunkRespVO> readDeliveryChunk(
            @RequestParam("deliverySessionId") String deliverySessionId,
            @RequestParam("start") Long start,
            @RequestParam("end") Long end,
            @RequestHeader("login-session") String loginSession) {
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        return success(fileDeliveryService.readDeliveryChunk(deliverySessionId, start, end, loginUser, loginSession));
    }

    @PostMapping("/revoke")
    @Operation(summary = "撤权下载会话", description = "在途传输的后续分块将被重检拦截，停止后续输出")
    @Parameter(name = "deliverySessionId", description = "下载会话 ID", required = true)
    public CommonResult<Boolean> revokeDelivery(@RequestParam("deliverySessionId") String deliverySessionId) {
        fileDeliveryService.revokeDelivery(deliverySessionId);
        return success(true);
    }

}
