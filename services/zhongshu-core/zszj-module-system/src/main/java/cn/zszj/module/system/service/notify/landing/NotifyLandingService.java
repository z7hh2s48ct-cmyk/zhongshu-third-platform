package cn.zszj.module.system.service.notify.landing;

import cn.zszj.module.system.controller.admin.notify.vo.message.NotifyMessageLandingRespVO;

/**
 * 消息落点解析 Service（ZS-MSG-003）——收件箱跳转的二次授权入口。
 *
 * <p>站内信正文不是访问业务对象的授权凭据：消息点击后的落点须经本服务统一解析——
 * 确认消息归属（技术收件箱边界）→ 落点注册判定（未知/关闭模块）→ 业务重新授权（失权拒绝），
 * 全部通过才返回结构性落点描述（不含消息正文）。
 */
public interface NotifyLandingService {

    /**
     * 解析指定站内信在指定端的落点。
     *
     * @param messageId 站内信编号
     * @param userId    当前登录用户编号
     * @param userType  当前登录用户类型
     * @param client    请求端
     * @return 解析结果：可用返回落点描述；不可用返回原因码 + 可呈现原因（不抛异常）。
     *         安全类失败（消息不存在 / 他人消息 / 缺租户）仍以 ServiceException 表达
     * @throws cn.zszj.framework.common.exception.ServiceException 1_002_031_000~002
     */
    NotifyMessageLandingRespVO resolveMessageLanding(Long messageId, Long userId, Integer userType,
                                                     NotifyLandingClient client);

}
