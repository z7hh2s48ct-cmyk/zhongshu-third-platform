package cn.zszj.module.system.service.notify.dispatch;

import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.module.system.api.user.AdminUserApi;
import cn.zszj.module.system.api.user.dto.AdminUserRespDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.Objects;

/**
 * Admin 用户收件人上下文解析器（ZS-MSG-001）——B05 只解析 System 用户（AdminUserApi）。
 *
 * <p>众墅要求（docs/05 §11 ZS-MSG-001）：「B05 先使用 System 用户，不先建任职关系；
 * 停用/错误租户收件人不会收到内容」。
 *
 * <p>校验规则：
 * <ol>
 *   <li>用户不存在 → {@link NotifyDispatchStatus#RECIPIENT_INVALID}
 *       （MyBatis-Plus 租户拦截器自动过滤，跨租户用户返回 null）；</li>
 *   <li>用户已停用（status=DISABLE）→ {@link NotifyDispatchStatus#RECIPIENT_INVALID}；</li>
 *   <li>当前租户上下文缺失 → {@link NotifyDispatchStatus#RECIPIENT_TENANT_MISMATCH}
 *       （循 JOB-002 outbox_event 租户强制先例，不默认 0）；</li>
 *   <li>技术查询异常<b>不吞</b>：向上抛触发事务回滚 + 可重试（codex r0 P2）——技术故障不得固化为业务级
 *       RECIPIENT_INVALID 而永久占用幂等键，避免用户查询恢复后同一事件仍被判 DUPLICATE_IGNORED。</li>
 * </ol>
 *
 * <p>任职路由（按组织/岗位/角色动态解析收件人）归 MSG-001.B（D-09 后追加实现），本卡只保留扩展点。
 */
@Component
@Slf4j
public class AdminUserNotifyRecipientContextResolver implements NotifyRecipientContextResolver {

    @Resource
    private AdminUserApi adminUserApi;

    @Override
    public boolean supports(NotifyRecipient recipient) {
        return recipient != null && recipient.getType() == UserTypeEnum.ADMIN;
    }

    @Override
    public NotifyRecipientContext resolve(NotifyRecipient recipient) {
        if (!supports(recipient)) {
            return NotifyRecipientContext.invalid(recipient, NotifyDispatchStatus.RECIPIENT_INVALID,
                    "B05 只支持 ADMIN 收件人");
        }
        // 租户上下文强制（循 JOB-002 outbox_event 先例）
        Long currentTenantId = TenantContextHolder.getTenantId();
        if (currentTenantId == null) {
            return NotifyRecipientContext.invalid(recipient, NotifyDispatchStatus.RECIPIENT_TENANT_MISMATCH,
                    "当前租户上下文缺失");
        }
        // 技术查询异常不在此捕获：向上抛触发回滚 + 可重试，不占用幂等键（仅「不存在/停用」是确定业务拒绝）
        AdminUserRespDTO user = adminUserApi.getUser(recipient.getId());
        // MyBatis-Plus 租户拦截器自动过滤：跨租户用户返回 null
        if (user == null) {
            return NotifyRecipientContext.invalid(recipient, NotifyDispatchStatus.RECIPIENT_INVALID,
                    "用户不存在或不属于当前租户");
        }
        if (Objects.equals(user.getStatus(), CommonStatusEnum.DISABLE.getStatus())) {
            return NotifyRecipientContext.invalid(recipient, NotifyDispatchStatus.RECIPIENT_INVALID, "用户已停用");
        }
        return NotifyRecipientContext.valid(recipient, currentTenantId);
    }

}
