package cn.zszj.module.infra.controller.admin.file;

import cn.hutool.crypto.digest.DigestUtil;
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
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.framework.common.pojo.CommonResult.success;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.FILE_DELIVERY_SESSION_INVALID;

/**
 * 文件交付票据与鉴权取流（ZS-FILE-004.A）。
 *
 * <p>授权不在 @PreAuthorize 层：票据只签发给当前有权读该文件的主体（owner / 同租户
 * infra:file:query），兑换与逐块取流均由服务层做主体·租户·用途·会话·撤权重检——
 * 与 FILE-001.A 的既有后端鉴权下载端点同策。</p>
 *
 * <p>登录会话绑定（codex r0 P1）：服务端以【当前访问令牌的 SHA-256】派生会话标识并写入
 * 下载会话——token 即登录会话凭据（退出即失效、重登录即变更），由认证过滤器权威校验，
 * 绝不采信客户端自报的会话串。</p>
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
            @Valid @RequestBody FileDeliveryTicketIssueReqVO reqVO, HttpServletRequest request) {
        LoginUser loginUser = requireLoginUser(request);
        return success(fileDeliveryService.issueDeliveryTicket(
                reqVO, loginUser, currentLoginSession(request)));
    }

    @PostMapping("/redeem")
    @Operation(summary = "原子兑换票据建立下载会话", description = "同主体同登录会话重复兑换幂等返回既有会话")
    public CommonResult<FileDeliverySessionRespVO> redeemDeliveryTicket(
            @RequestParam("ticketToken") String ticketToken,
            @RequestParam("purpose") String purpose, HttpServletRequest request) {
        LoginUser loginUser = requireLoginUser(request);
        return success(fileDeliveryService.redeemDeliveryTicket(
                ticketToken, purpose, loginUser, currentLoginSession(request)));
    }

    @GetMapping("/chunk")
    @Operation(summary = "鉴权取流分块", description = "Range 语义 [start, endInclusive]，服务端收敛分块上限；逐块重检身份/会话/撤权/过期")
    public CommonResult<FileDeliveryChunkRespVO> readDeliveryChunk(
            @RequestParam("deliverySessionId") String deliverySessionId,
            @RequestParam("start") Long start,
            @RequestParam("end") Long end, HttpServletRequest request) {
        LoginUser loginUser = requireLoginUser(request);
        return success(fileDeliveryService.readDeliveryChunk(
                deliverySessionId, start, end, loginUser, currentLoginSession(request)));
    }

    @PostMapping("/revoke")
    @Operation(summary = "撤权下载会话", description = "本人或同租户管理员；在途传输的后续分块将被重检拦截")
    @Parameter(name = "deliverySessionId", description = "下载会话 ID", required = true)
    public CommonResult<Boolean> revokeDelivery(
            @RequestParam("deliverySessionId") String deliverySessionId, HttpServletRequest request) {
        LoginUser loginUser = requireLoginUser(request);
        fileDeliveryService.revokeDelivery(deliverySessionId, loginUser);
        return success(true);
    }

    /**
     * 登录主体（认证过滤器已校验 token 有效性；缺失即拒绝）。
     */
    private LoginUser requireLoginUser(HttpServletRequest request) {
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser == null) {
            throw exception(FILE_DELIVERY_SESSION_INVALID);
        }
        return loginUser;
    }

    /**
     * 登录会话标识：服务端从当前访问令牌派生（SHA-256），客户端不可自报——
     * 与主体身份双重绑定，退出/重登录自然失效或更换。
     */
    private String currentLoginSession(HttpServletRequest request) {
        String token = SecurityFrameworkUtils.obtainAuthorization(request, "Authorization", "token", false);
        if (token == null || token.isEmpty()) {
            throw exception(FILE_DELIVERY_SESSION_INVALID);
        }
        return DigestUtil.sha256Hex(token);
    }

}
