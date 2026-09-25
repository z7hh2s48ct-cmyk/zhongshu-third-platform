package cn.zszj.module.system.service.notify.landing;

import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.common.catalog.ModuleCatalog;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.module.system.controller.admin.notify.vo.message.NotifyMessageLandingRespVO;
import cn.zszj.module.system.dal.dataobject.notify.NotifyMessageDO;
import cn.zszj.module.system.dal.mysql.notify.NotifyMessageMapper;
import cn.zszj.module.system.service.notify.NotifyMessageOrgAuthorizer;
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
 *   <li><b>租户上下文</b>：缺失即拒绝（循 MSG-001/JOB-002 惯例，不默认 0）；跨技术租户读取由
 *       tenant 行级隔离在生产装配兜底（system_notify_message 非 ignore-tables，自动追加 tenant_id
 *       条件，跨租户 ID 解析为「消息不存在」）。边界：H2 测试上下文不含租户拦截器，消息链路的
 *       跨租户专项回归（真实 Mapper/HTTP 链验证跨租户消息 ID 不可读、不可解析、不可改已读）登记为
 *       后续专项，随真实环境联调收口，不在本卡 H2 用例内伪装覆盖；</li>
 *   <li><b>消息存在性</b>：不存在（含跨租户不可见）→ NOT_FOUND；</li>
 *   <li><b>归属校验</b>：userId/userType 与登录主体不一致 → ACCESS_DENIED
 *       （技术收件箱边界：他人消息不能解析落点、不泄露他人可触达的业务入口）。
 *       r0-P3：NOT_FOUND 与 ACCESS_DENIED 的对外文案统一（防同租户消息 ID 存在性探测），
 *       错误码保持区分供内部日志与本卡验收证据；</li>
 *   <li><b>org 归属门</b>（ZS-MSG-003.C）：消息 org 归属不在当前主体授权范围（组织授权收缩 /
 *       跨组织 visit 目标外）→ REVOKED，旧消息不得借落点进入详情、下载附件；</li>
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

    /** org 轴对象门（ZS-MSG-003.C）：落点解析前裁决消息 org 归属是否在当前主体授权范围。 */
    @Resource
    private NotifyMessageOrgAuthorizer notifyMessageOrgAuthorizer;

    @Override
    public NotifyMessageLandingRespVO resolveMessageLanding(Long messageId, Long userId, Integer userType,
                                                            NotifyLandingClient client) {
        requireTenant();
        NotifyMessageDO message = notifyMessageMapper.selectById(messageId);
        if (message == null) {
            throw exception(NOTIFY_LANDING_MESSAGE_NOT_FOUND);
        }
        if (!message.getUserId().equals(userId) || !message.getUserType().equals(userType)) {
            // 归属校验：技术收件箱边界——他人消息不能解析落点（不泄露他人消息可触达的业务入口）。
            // 对外文案与 NOT_FOUND 统一（r0-P3），此处记内部日志保留区分度
            log.info("[resolveMessageLanding][消息({}) 归属不匹配：消息 userId/userType={}/{}, 登录主体 {}/{}]",
                    messageId, message.getUserId(), message.getUserType(), userId, userType);
            throw exception(NOTIFY_LANDING_ACCESS_DENIED);
        }
        // ZS-MSG-003.C：org 轴对象门（归属校验后、注册判定前——不向越界者泄露「未注册/模块关闭」信息）
        if (!notifyMessageOrgAuthorizer.isObjectAllowed(message)) {
            log.info("[resolveMessageLanding][消息({}) org 归属({}) 不在当前主体授权范围，落点判不可用]",
                    messageId, message.getOrganizationId());
            return NotifyMessageLandingRespVO.unavailable(NotifyLandingUnavailable.REVOKED,
                    "消息所属业务组织已不在授权范围");
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
