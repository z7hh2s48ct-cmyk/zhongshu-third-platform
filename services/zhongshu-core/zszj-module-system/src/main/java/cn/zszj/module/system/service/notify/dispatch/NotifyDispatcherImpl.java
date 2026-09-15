package cn.zszj.module.system.service.notify.dispatch;

import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.module.infra.framework.outbox.OutboxEventMessage;
import cn.zszj.module.infra.framework.outbox.ReliableEventPort;
import cn.zszj.module.system.dal.dataobject.notify.NotifySendLogDO;
import cn.zszj.module.system.dal.dataobject.notify.NotifyTemplateDO;
import cn.zszj.module.system.dal.mysql.notify.NotifySendLogMapper;
import cn.zszj.module.system.service.notify.NotifyMessageService;
import cn.zszj.module.system.service.notify.NotifyTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.*;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.module.system.enums.ErrorCodeConstants.*;

/**
 * 统一通知派发实现（ZS-MSG-001）。
 *
 * <p>众墅要求（docs/05 §11 ZS-MSG-001）：「建设统一通知命令/结果，绑定事件、业务对象、技术租户和接收主体；
 * 明确禁用模板、无渠道、接收人失效的可查询状态；复用模板渲染」。
 *
 * <p>实现要点：
 * <ol>
 *   <li><b>事务内幂等（claim-first）</b>：循 JOB-003 {@code JdbcConsumerInboxPort.claimNew} 先例——
 *       <b>先原子抢占幂等键（写 {@code system_notify_send_log}，DB 唯一索引硬兜底），只有抢到键的请求才执行
 *       副作用（建消息 + append Outbox）</b>；并发撞 {@code uk_notify_send_log_idempotent} 时经 SAVEPOINT 回滚
 *       恢复事务可用性后复判既有记录（PG 撞 23505 后事务即中止，不回滚保存点则后续复判查询报 25P02）；</li>
 *   <li><b>事务参与</b>：{@code @Transactional(REQUIRED)} 加入调用方事务；业务回滚 → 抢位日志 + 消息 + Outbox
 *       一并回滚（复用 JOB-002 {@link ReliableEventPort} 的 MANDATORY 事务参与保证）；</li>
 *   <li><b>明确状态</b>：非 SUCCESS 状态返回 {@link NotifyDispatchStatus}，不抛异常、不返回 null；
 *       所有状态（含无渠道）均写入 {@code system_notify_send_log}（可追溯、可按事件回查）；</li>
 *   <li><b>Outbox 集成</b>：SUCCESS 状态追加 {@code OutboxEventMessage{eventType=NOTIFY_DISPATCHED}}，
 *       供 MSG-002（业务待办生命周期）与 MSG-004（渠道发送状态/回执对账）消费；</li>
 *   <li><b>收件人上下文</b>：委托 {@link NotifyRecipientContextResolver}（B05 只解析 ADMIN，不查任职关系）；
 *       仅「不存在/停用」归业务状态，技术查询异常向上抛触发回滚可重试（不占用幂等键）；</li>
 *   <li><b>租户上下文强制</b>：循 JOB-002 outbox_event fail-closed 先例，{@link TenantContextHolder} 缺失即
 *       抛异常拒绝派发（系统级错误，非业务状态）；{@link NotifyDispatchStatus#RECIPIENT_TENANT_MISMATCH}
 *       为预留状态（待 AdminUserRespDTO 暴露 tenantId 或 MSG-001.B 任职路由后启用显式比对）。</li>
 * </ol>
 */
@Service
@Slf4j
public class NotifyDispatcherImpl implements NotifyDispatcher {

    /** Outbox 事件类型——MSG-002/004 消费方按此订阅 */
    public static final String OUTBOX_EVENT_TYPE = "NOTIFY_DISPATCHED";

    /**
     * 无渠道哨兵值：命令未指定任何渠道时，收件人级 NO_CHANNEL 结果以此占位落库（channel 列 NOT NULL），
     * 使「无渠道」成为可查询、可幂等的持久化状态（对齐接口「所有状态均写日志」合同）。
     */
    public static final String NO_CHANNEL_SENTINEL = "NONE";

    @Resource
    private NotifyTemplateService notifyTemplateService;
    @Resource
    private NotifyMessageService notifyMessageService;
    @Resource
    private NotifySendLogMapper notifySendLogMapper;
    @Resource
    private NotifyRecipientContextResolver recipientContextResolver;
    @Resource
    private ReliableEventPort reliableEventPort;
    /** claim-first 保存点管理所需（与 MyBatis 同一事务连接，经 DataSourceUtils 绑定）。 */
    @Resource
    private DataSource dataSource;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<NotifyDispatchResult> dispatch(NotifyCommand command) {
        // 1. 前置校验（fail-fast，抛异常）
        validateCommand(command);

        // 2. 租户上下文强制（循 JOB-002 outbox_event fail-closed 先例：缺失即拒绝，不默认 0）
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw exception(NOTIFY_DISPATCH_TENANT_REQUIRED);
        }

        // 3. 参数归一化（P2：合同允许 null，但真实消息表 template_params NOT NULL，空参归一为不可变空 Map）
        Map<String, Object> params = command.getTemplateParams() == null
                ? Collections.emptyMap() : command.getTemplateParams();

        // 4. 模板解析（一次，供所有收件人复用）
        NotifyTemplateDO template = notifyTemplateService.getNotifyTemplateByCodeFromCache(command.getTemplateCode());
        TemplateCheckResult templateCheck = checkTemplate(template, params);

        // 5. 逐收件人 × 逐渠道处理
        List<NotifyDispatchResult> results = new ArrayList<>();
        boolean noChannel = CollectionUtils.isEmpty(command.getChannels());
        for (NotifyRecipient recipient : command.getRecipients()) {
            if (noChannel) {
                // 无渠道：以哨兵渠道持久化 NO_CHANNEL（可查询、幂等；不入 outbox、不建消息）
                results.add(dispatchNoChannel(command, recipient, template, tenantId));
                continue;
            }
            for (NotifyChannel channel : command.getChannels()) {
                results.add(dispatchOne(command, recipient, channel, template, templateCheck, params, tenantId));
            }
        }
        return results;
    }

    /** 前置校验：eventId / recipients / actorType 必填（空 → 抛异常，fail-fast）。 */
    private void validateCommand(NotifyCommand command) {
        if (command == null || !StringUtils.hasText(command.getEventId())) {
            throw exception(NOTIFY_DISPATCH_EVENT_ID_REQUIRED);
        }
        if (CollectionUtils.isEmpty(command.getRecipients())) {
            throw exception(NOTIFY_DISPATCH_RECIPIENTS_REQUIRED);
        }
        if (command.getActorType() == null) {
            throw exception(NOTIFY_DISPATCH_ACTOR_TYPE_REQUIRED);
        }
    }

    /** 模板校验结果（不存在 / 禁用 / 参数缺失 → 明确状态；通过 → null）。 */
    private record TemplateCheckResult(NotifyDispatchStatus status, String reason) {}

    private TemplateCheckResult checkTemplate(NotifyTemplateDO template, Map<String, Object> params) {
        if (template == null) {
            return new TemplateCheckResult(NotifyDispatchStatus.TEMPLATE_NOT_FOUND, "模板不存在");
        }
        if (Objects.equals(template.getStatus(), CommonStatusEnum.DISABLE.getStatus())) {
            return new TemplateCheckResult(NotifyDispatchStatus.DISABLED_TEMPLATE, "模板已禁用");
        }
        // 参数校验
        if (template.getParams() != null) {
            for (String key : template.getParams()) {
                Object value = params.get(key);
                if (value == null) {
                    return new TemplateCheckResult(NotifyDispatchStatus.TEMPLATE_PARAM_MISSING, "模板参数缺失: " + key);
                }
            }
        }
        return null; // 通过
    }

    /**
     * 单收件人 × 单渠道派发（claim-first）：
     * 预检 → 判定状态（无副作用）→ 原子抢位（写日志）→ 仅抢到键且 SUCCESS 才建消息 + Outbox → 回填。
     */
    private NotifyDispatchResult dispatchOne(NotifyCommand command, NotifyRecipient recipient, NotifyChannel channel,
                                             NotifyTemplateDO template, TemplateCheckResult templateCheck,
                                             Map<String, Object> params, Long tenantId) {
        String channelKey = channel.name();

        // 1. 幂等预检（覆盖所有状态：成功/失败重试第二次直接命中，不再触发任何副作用）
        NotifySendLogDO existing = findExisting(tenantId, command, recipient, channelKey);
        if (existing != null) {
            return duplicateResult(command, recipient, channel, existing, "幂等键已存在，返回既有结果");
        }

        // 2. 判定状态（均无副作用）：渠道 → 模板 → 收件人上下文
        NotifyDispatchStatus status;
        String reason;
        if (channel != NotifyChannel.INBOX) {
            status = NotifyDispatchStatus.NO_CHANNEL;
            reason = "B05 只支持 INBOX 渠道";
        } else if (templateCheck != null) {
            status = templateCheck.status();
            reason = templateCheck.reason();
        } else {
            NotifyRecipientContext ctx = recipientContextResolver.resolve(recipient);
            if (ctx.isValid()) {
                status = NotifyDispatchStatus.SUCCESS;
                reason = null;
            } else {
                status = ctx.getInvalidStatus();
                reason = ctx.getInvalidReason();
            }
        }

        // 3. 原子抢位：先写日志抢占唯一键（保存点保护）。只有抢到键的请求执行副作用。
        NotifySendLogDO logDO = buildLog(command, recipient, channelKey, template, tenantId, status, reason);
        if (!claimIdempotentKey(logDO)) {
            // 并发撞键：保存点已回滚、事务恢复可用，复判既有记录（他事务已提交则读到）
            NotifySendLogDO concurrent = findExisting(tenantId, command, recipient, channelKey);
            if (concurrent == null) {
                throw exception(NOTIFY_DISPATCH_WRITE_FAILED);
            }
            return duplicateResult(command, recipient, channel, concurrent, "并发幂等兜底");
        }

        // 4. 非 SUCCESS：抢位即终态，无副作用，直接返回
        if (status != NotifyDispatchStatus.SUCCESS) {
            return toResult(command, recipient, channel, status, reason, logDO.getId(), null, null);
        }

        // 5. SUCCESS 且已抢到键：执行副作用（建消息 + Outbox），随后回填日志关联 ID
        String content = notifyTemplateService.formatNotifyTemplateContent(template.getContent(), params);
        Long messageId = notifyMessageService.createNotifyMessage(
                recipient.getId(), recipient.getType().getValue(), template, content, params);
        long outboxEventId = reliableEventPort.append(buildOutboxMessage(command, recipient, channel, messageId));

        notifySendLogMapper.fillDispatchSideEffects(logDO.getId(), messageId, outboxEventId);
        return toResult(command, recipient, channel, NotifyDispatchStatus.SUCCESS, null,
                logDO.getId(), messageId, outboxEventId);
    }

    /** 无渠道（命令未指定任何渠道）：以哨兵渠道持久化 NO_CHANNEL，可查询、幂等，不建消息、不入 Outbox。 */
    private NotifyDispatchResult dispatchNoChannel(NotifyCommand command, NotifyRecipient recipient,
                                                   NotifyTemplateDO template, Long tenantId) {
        NotifySendLogDO existing = findExisting(tenantId, command, recipient, NO_CHANNEL_SENTINEL);
        if (existing != null) {
            return duplicateResult(command, recipient, null, existing, "幂等键已存在，返回既有结果");
        }
        NotifySendLogDO logDO = buildLog(command, recipient, NO_CHANNEL_SENTINEL, template, tenantId,
                NotifyDispatchStatus.NO_CHANNEL, "未指定任何派发渠道");
        if (!claimIdempotentKey(logDO)) {
            NotifySendLogDO concurrent = findExisting(tenantId, command, recipient, NO_CHANNEL_SENTINEL);
            if (concurrent == null) {
                throw exception(NOTIFY_DISPATCH_WRITE_FAILED);
            }
            return duplicateResult(command, recipient, null, concurrent, "并发幂等兜底");
        }
        return toResult(command, recipient, null, NotifyDispatchStatus.NO_CHANNEL, "未指定任何派发渠道",
                logDO.getId(), null, null);
    }

    /** 按幂等键查既有日志（tenant + eventId + recipientType + recipientId + channel）。 */
    private NotifySendLogDO findExisting(Long tenantId, NotifyCommand command, NotifyRecipient recipient, String channelKey) {
        return notifySendLogMapper.selectByIdempotentKey(
                tenantId, command.getEventId(), recipient.getType().name(), recipient.getId(), channelKey);
    }

    /**
     * 原子抢占幂等键（claim-first，循 JOB-003 inbox claimNew 先例）：
     * 保存点内 INSERT 日志——撞唯一键（并发）则回滚保存点恢复事务可用性并返回 false，由调用方复判；
     * 抢位成功释放保存点返回 true。
     *
     * <p>异常归一化（codex r1/r2 P2）：
     * <ul>
     *   <li>{@link DuplicateKeyException} → 唯一键冲突（幂等），回滚保存点后复判；回滚与释放保存点
     *       <b>任一失败即抛 WRITE_FAILED</b>，禁止在不可信事务上继续复判而掩盖真正的恢复失败
     *       （PG 撞 23505 后事务中止，RELEASE/ROLLBACK SAVEPOINT 语句被取消或连接中断同样使事务不可信）；</li>
     *   <li>其余 {@link DataAccessException}（字段超长/非空/CHECK 等真实写入失败）→ WRITE_FAILED，不静默、
     *       不误判为重复；</li>
     *   <li>{@link SQLException}（保存点 JDBC 操作失败）→ 事务状态不可信，WRITE_FAILED。</li>
     * </ul>
     * 三者均向外抛触发整体回滚（技术失败可重试）；连接经 {@code finally} 与 getConnection 配对释放。
     * 注意：回滚/释放辅助方法在 {@code catch(DuplicateKeyException)} 块内抛出的是 ServiceException，
     * 同级后续 {@code catch(SQLException)} 不会捕获它，故辅助方法内部自行归一化为 WRITE_FAILED。
     */
    private boolean claimIdempotentKey(NotifySendLogDO logDO) {
        Connection con = DataSourceUtils.getConnection(dataSource);
        Savepoint savepoint = null;
        try {
            savepoint = con.setSavepoint();
            notifySendLogMapper.insert(logDO);
            con.releaseSavepoint(savepoint);
            return true;
        } catch (DuplicateKeyException e) {
            // 并发撞 uk_notify_send_log_idempotent：PG 撞 23505 后事务即中止，必须回滚到插入前保存点
            // 恢复事务可用性后才能复判既有记录（否则报 25P02）；回滚失败即抛，不在不可信事务上继续复判
            rollbackSavepointOrFail(con, savepoint);
            releaseSavepointOrFail(con, savepoint); // 回滚后保存点仍有效，释放避免长事务内残留；释放失败即抛（事务不可信，不复判）
            return false;
        } catch (DataAccessException e) {
            // 非唯一键的真实写入失败：归一化为 WRITE_FAILED（不静默、不误判为重复），整体回滚可重试
            log.error("[claimIdempotentKey][发送日志写入失败 eventId={} errClass={}]",
                    logDO.getEventId(), e.getClass().getName());
            throw exception(NOTIFY_DISPATCH_WRITE_FAILED);
        } catch (SQLException e) {
            // 保存点 JDBC 操作失败：事务状态不可信，归一化为 WRITE_FAILED（整体回滚）
            log.error("[claimIdempotentKey][保存点操作失败 eventId={} errClass={}]",
                    logDO.getEventId(), e.getClass().getName());
            throw exception(NOTIFY_DISPATCH_WRITE_FAILED);
        } finally {
            // 与 DataSourceUtils.getConnection 配对：事务绑定连接不会物理关闭，仅解绑本次引用
            DataSourceUtils.releaseConnection(con, dataSource);
        }
    }

    /** 回滚到保存点恢复事务可用性；回滚失败即抛 WRITE_FAILED（不得在中止事务上继续复判，掩盖真正恢复失败）。 */
    private void rollbackSavepointOrFail(Connection con, Savepoint savepoint) {
        if (savepoint == null) {
            return;
        }
        try {
            con.rollback(savepoint);
        } catch (SQLException ex) {
            log.error("[rollbackSavepointOrFail][回滚保存点失败 errClass={}]", ex.getClass().getName());
            throw exception(NOTIFY_DISPATCH_WRITE_FAILED);
        }
    }

    /**
     * 释放保存点（回滚成功后清理，避免长事务内残留）；释放失败即抛 WRITE_FAILED。
     * PG 的 {@code RELEASE SAVEPOINT} 是真实数据库语句，被取消或连接中断会使事务再次中止/连接失效，
     * 此时不得继续复判（否则以 25P02/连接异常掩盖释放失败并绕过 WRITE_FAILED）。
     */
    private void releaseSavepointOrFail(Connection con, Savepoint savepoint) {
        if (savepoint == null) {
            return;
        }
        try {
            con.releaseSavepoint(savepoint);
        } catch (SQLException ex) {
            log.error("[releaseSavepointOrFail][释放保存点失败 errClass={}]", ex.getClass().getName());
            throw exception(NOTIFY_DISPATCH_WRITE_FAILED);
        }
    }

    /** 构建 Outbox 事件消息（与业务写同事务，循 JOB-002 ReliableEventPort 合同）。 */
    private OutboxEventMessage buildOutboxMessage(NotifyCommand command, NotifyRecipient recipient,
                                                  NotifyChannel channel, Long messageId) {
        return OutboxEventMessage.builder()
                .eventType(OUTBOX_EVENT_TYPE)
                .bizType(command.getBizType() != null ? command.getBizType() : "notify_message")
                .bizId(command.getBizId() != null ? command.getBizId() : String.valueOf(messageId))
                .bizVersion(command.getBizVersion())
                .payload(buildOutboxPayload(command, recipient, channel, messageId))
                .actorType(command.getActorType())
                .actorId(command.getActorId())
                .traceId(command.getTraceId())
                .build();
    }

    /** 构建 Outbox 载荷（供 MSG-002/004 消费方解释；不含模板正文/参数值，循脱敏惯例）。 */
    private Map<String, Object> buildOutboxPayload(NotifyCommand command, NotifyRecipient recipient,
                                                   NotifyChannel channel, Long messageId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId", command.getEventId());
        payload.put("templateCode", command.getTemplateCode());
        payload.put("recipientType", recipient.getType().name());
        payload.put("recipientId", recipient.getId());
        payload.put("channel", channel.name());
        payload.put("messageId", messageId);
        payload.put("status", NotifyDispatchStatus.SUCCESS.name());
        return payload;
    }

    /** 构建发送日志 DO（抢位时 messageId/outboxEventId 为空，SUCCESS 副作用后回填）。 */
    private NotifySendLogDO buildLog(NotifyCommand command, NotifyRecipient recipient, String channelKey,
                                     NotifyTemplateDO template, Long tenantId,
                                     NotifyDispatchStatus status, String reason) {
        return NotifySendLogDO.builder()
                .eventId(command.getEventId())
                .recipientType(recipient.getType().name())
                .recipientId(recipient.getId())
                .channel(channelKey)
                .templateCode(command.getTemplateCode())
                .templateId(template != null ? template.getId() : null)
                .bizType(command.getBizType())
                .bizId(command.getBizId())
                .bizVersion(command.getBizVersion())
                .status(status.name())
                .statusReason(truncate(reason, 512))
                .actorType(command.getActorType().name())
                .actorId(command.getActorId())
                .traceId(command.getTraceId())
                .tenantId(tenantId)
                .build();
    }

    /** 幂等命中结果（携带既有日志的三个关联 ID，供调用方重放）。 */
    private NotifyDispatchResult duplicateResult(NotifyCommand command, NotifyRecipient recipient, NotifyChannel channel,
                                                 NotifySendLogDO existing, String reason) {
        return NotifyDispatchResult.builder()
                .eventId(command.getEventId()).recipient(recipient).channel(channel)
                .status(NotifyDispatchStatus.DUPLICATE_IGNORED)
                .statusReason(reason)
                .sendLogId(existing.getId()).messageId(existing.getMessageId())
                .outboxEventId(existing.getOutboxEventId())
                .build();
    }

    private NotifyDispatchResult toResult(NotifyCommand command, NotifyRecipient recipient, NotifyChannel channel,
                                          NotifyDispatchStatus status, String reason,
                                          Long sendLogId, Long messageId, Long outboxEventId) {
        return NotifyDispatchResult.builder()
                .eventId(command.getEventId()).recipient(recipient).channel(channel)
                .status(status).statusReason(truncate(reason, 512))
                .sendLogId(sendLogId).messageId(messageId).outboxEventId(outboxEventId)
                .build();
    }

    /** 截断字符串（防 status_reason 超长）。 */
    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

}
