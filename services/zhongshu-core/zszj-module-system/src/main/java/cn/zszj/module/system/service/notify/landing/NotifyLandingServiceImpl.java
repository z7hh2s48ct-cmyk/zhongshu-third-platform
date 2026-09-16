package cn.zszj.module.system.service.notify.landing;

import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.common.catalog.ModuleCatalog;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.module.system.controller.admin.notify.vo.message.NotifyMessageLandingRespVO;
import cn.zszj.module.system.dal.dataobject.notify.NotifyMessageDO;
import cn.zszj.module.system.dal.mysql.notify.NotifyMessageMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Map;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.module.system.enums.ErrorCodeConstants.NOTIFY_LANDING_ACCESS_DENIED;
import static cn.zszj.module.system.enums.ErrorCodeConstants.NOTIFY_LANDING_MESSAGE_NOT_FOUND;
import static cn.zszj.module.system.enums.ErrorCodeConstants.NOTIFY_LANDING_TENANT_REQUIRED;

/**
 * {@link NotifyLandingService} 实现（ZS-MSG-003）——收件箱与消息落点二次授权。
 *
 * <p>解析顺序即防线顺序（fail-closed，任何一步不过即拒绝）：
 * <ol>
 *   <li><b>租户上下文</b>：缺失即拒绝（循 MSG-001/JOB-002 惯例，不默认 0）；跨技术租户
 *       读取由 tenant 行级隔离兜底（非 ignore-tables 表自动追加 tenant_id 条件，
 *       跨租户 ID 解析为「消息不存在」，复用 ZS-DB-018 既有回归，不重复建设）；</li>
 *   <li><b>消息存在性</b>：不存在（含跨租户不可见）→ NOT_FOUND；</li>
 *   <li><b>归属校验</b>：userId/userType 与登录主体不一致 → ACCESS_DENIED
 *       （技术收件箱边界：他人 IDs 不能读、不能借落点触碰）；</li>
 *   <li><b>落点注册判定</b>：模板编码未注册 → NOT_REGISTERED（未知消息类型明确不可用）；
 *       归属模块未启用（{@link ModuleCatalog} 运行白名单）→ MODULE_DISABLED，
 *       且不再触碰业务（关闭模块的业务 Bean 不在，重读无意义）；</li>
 *   <li><b>业务重新授权</b>：调用 {@link NotifyLandingProvider#authorize} 重新读取业务并校验，
 *       拒绝（业务撤权/删除）→ REVOKED，旧消息不得借落点进入详情、下载附件；</li>
 *   <li><b>端描述</b>：取该端落点描述，未声明 → CLIENT_UNSUPPORTED。</li>
 * </ol>
 *
 * <p><b>无缓存</b>：每步决策现算（注册表装配期只读、授权结论不缓存），组织/权限变化即时生效，
 * 无需缓存失效通知；落点响应仅含结构性引用，不复制业务敏感正文。
 */
@Service
@Slf4j
public class NotifyLandingServiceImpl implements NotifyLandingService {

    @Resource
    private NotifyMessageMapper notifyMessageMapper;

    @Resource
    private NotifyLandingRegistry landingRegistry;

    @Override
    public NotifyMessageLandingRespVO resolveMessageLanding(Long messageId, Long userId, Integer userType,
                                                            NotifyLandingClient client) {
        requireTenant();
        NotifyMessageDO message = notifyMessageMapper.selectById(messageId);
        if (message == null) {
            throw exception(NOTIFY_LANDING_MESSAGE_NOT_FOUND);
        }
        if (!message.getUserId().equals(userId) || !message.getUserType().equals(userType)) {
            // 归属校验：技术收件箱边界——他人消息不能解析落点（不泄露他人消息可触达的业务入口）
            throw exception(NOTIFY_LANDING_ACCESS_DENIED);
        }
        NotifyLandingProvider provider = landingRegistry.getByTemplateCode(message.getTemplateCode());
        if (provider == null) {
            return NotifyMessageLandingRespVO.unavailable(NotifyLandingUnavailable.NOT_REGISTERED,
                    "该消息类型未注册跳转落点");
        }
        if (!ModuleCatalog.ENABLED_MODULES.contains(provider.module())) {
            return NotifyMessageLandingRespVO.unavailable(NotifyLandingUnavailable.MODULE_DISABLED,
                    "该消息所属模块未启用，落点不可用");
        }
        Map<String, Object> templateParams = message.getTemplateParams();
        try {
            provider.authorize(message, templateParams);
        } catch (ServiceException e) {
            log.info("[resolveMessageLanding][消息({}) 落点业务授权拒绝：{}]", messageId, e.getMessage());
            return NotifyMessageLandingRespVO.unavailable(NotifyLandingUnavailable.REVOKED, e.getMessage());
        }
        NotifyLandingDescriptor descriptor = provider.resolve(client, templateParams);
        if (descriptor == null) {
            return NotifyMessageLandingRespVO.unavailable(NotifyLandingUnavailable.CLIENT_UNSUPPORTED,
                    "该消息在当前端未注册跳转落点");
        }
        return NotifyMessageLandingRespVO.available(descriptor);
    }

    /** 技术租户强制：缺失即拒绝，不默认 0（循 MSG-001/MSG-002/JOB-002 惯例）。 */
    private void requireTenant() {
        if (TenantContextHolder.getTenantId() == null) {
            throw exception(NOTIFY_LANDING_TENANT_REQUIRED);
        }
    }

}
