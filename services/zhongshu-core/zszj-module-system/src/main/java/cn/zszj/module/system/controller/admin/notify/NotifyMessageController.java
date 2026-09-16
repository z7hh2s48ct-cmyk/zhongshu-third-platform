package cn.zszj.module.system.controller.admin.notify;

import cn.zszj.framework.apilog.core.annotation.ApiAccessLog;
import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.common.pojo.CommonResult;
import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.framework.common.util.object.BeanUtils;
import cn.zszj.module.system.controller.admin.notify.vo.message.NotifyMessageLandingRespVO;
import cn.zszj.module.system.controller.admin.notify.vo.message.NotifyMessageMyPageReqVO;
import cn.zszj.module.system.controller.admin.notify.vo.message.NotifyMessagePageReqVO;
import cn.zszj.module.system.controller.admin.notify.vo.message.NotifyMessageRespVO;
import cn.zszj.module.system.dal.dataobject.notify.NotifyMessageDO;
import cn.zszj.module.system.service.notify.NotifyMessageService;
import cn.zszj.module.system.service.notify.landing.NotifyLandingClient;
import cn.zszj.module.system.service.notify.landing.NotifyLandingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.zszj.framework.common.pojo.CommonResult.success;
import static cn.zszj.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 我的站内信")
@RestController
@RequestMapping("/system/notify-message")
@Validated
public class NotifyMessageController {

    @Resource
    private NotifyMessageService notifyMessageService;

    @Resource
    private NotifyLandingService notifyLandingService;

    // ========== 管理所有的站内信 ==========

    @GetMapping("/get")
    @Operation(summary = "获得站内信")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:notify-message:query')")
    public CommonResult<NotifyMessageRespVO> getNotifyMessage(@RequestParam("id") Long id) {
        NotifyMessageDO message = notifyMessageService.getNotifyMessage(id);
        return success(BeanUtils.toBean(message, NotifyMessageRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得站内信分页")
    @PreAuthorize("@ss.hasPermission('system:notify-message:query')")
    public CommonResult<PageResult<NotifyMessageRespVO>> getNotifyMessagePage(@Valid NotifyMessagePageReqVO pageVO) {
        PageResult<NotifyMessageDO> pageResult = notifyMessageService.getNotifyMessagePage(pageVO);
        return success(BeanUtils.toBean(pageResult, NotifyMessageRespVO.class));
    }

    // ========== 查看自己的站内信 ==========

    @GetMapping("/my-page")
    @Operation(summary = "获得我的站内信分页")
    public CommonResult<PageResult<NotifyMessageRespVO>> getMyMyNotifyMessagePage(@Valid NotifyMessageMyPageReqVO pageVO) {
        PageResult<NotifyMessageDO> pageResult = notifyMessageService.getMyMyNotifyMessagePage(pageVO,
                getLoginUserId(), UserTypeEnum.ADMIN.getValue());
        return success(BeanUtils.toBean(pageResult, NotifyMessageRespVO.class));
    }

    @PutMapping("/update-read")
    @Operation(summary = "标记站内信为已读")
    @Parameter(name = "ids", description = "编号列表", required = true, example = "1024,2048")
    public CommonResult<Boolean> updateNotifyMessageRead(@RequestParam("ids") List<Long> ids) {
        notifyMessageService.updateNotifyMessageRead(ids, getLoginUserId(), UserTypeEnum.ADMIN.getValue());
        return success(Boolean.TRUE);
    }

    @PutMapping("/update-all-read")
    @Operation(summary = "标记所有站内信为已读")
    public CommonResult<Boolean> updateAllNotifyMessageRead() {
        notifyMessageService.updateAllNotifyMessageRead(getLoginUserId(), UserTypeEnum.ADMIN.getValue());
        return success(Boolean.TRUE);
    }

    @GetMapping("/get-unread-list")
    @Operation(summary = "获取当前用户的最新站内信列表，默认 10 条")
    @Parameter(name = "size", description = "10")
    public CommonResult<List<NotifyMessageRespVO>> getUnreadNotifyMessageList(
            @RequestParam(name = "size", defaultValue = "10") @Min(value = 1, message = "未读列表条数至少为 1")
            @Max(value = 100, message = "未读列表条数最多为 100") Integer size) {
        List<NotifyMessageDO> list = notifyMessageService.getUnreadNotifyMessageList(
                getLoginUserId(), UserTypeEnum.ADMIN.getValue(), size);
        return success(BeanUtils.toBean(list, NotifyMessageRespVO.class));
    }

    @GetMapping("/get-landing")
    @Operation(summary = "解析站内信落点", description = "消息点击跳转的二次授权入口：确认消息归属 → 落点注册判定 → 业务重新授权；不可用时返回原因码")
    @Parameter(name = "id", description = "站内信编号", required = true, example = "1024")
    @Parameter(name = "client", description = "请求端（WEB / MOBILE）", required = true, example = "WEB")
    public CommonResult<NotifyMessageLandingRespVO> getNotifyMessageLanding(
            @RequestParam("id") Long id,
            @RequestParam("client") NotifyLandingClient client) {
        return success(notifyLandingService.resolveMessageLanding(id, getLoginUserId(),
                UserTypeEnum.ADMIN.getValue(), client));
    }

    @GetMapping("/get-unread-count")
    @Operation(summary = "获得当前用户的未读站内信数量")
    @ApiAccessLog(enable = false) // 由于前端会不断轮询该接口，记录日志没有意义
    public CommonResult<Long> getUnreadNotifyMessageCount() {
        return success(notifyMessageService.getUnreadNotifyMessageCount(
                getLoginUserId(), UserTypeEnum.ADMIN.getValue()));
    }

}
