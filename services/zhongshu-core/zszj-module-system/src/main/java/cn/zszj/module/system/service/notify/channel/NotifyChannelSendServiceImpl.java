package cn.zszj.module.system.service.notify.channel;

import cn.zszj.framework.common.util.json.JsonUtils;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.module.infra.framework.outbox.OutboxEventMessage;
import cn.zszj.module.infra.framework.outbox.OutboxEventMessage.OutboxActorType;
import cn.zszj.module.infra.framework.outbox.ReliableEventPort;
import cn.zszj.module.system.dal.dataobject.notify.NotifyChannelSendDO;
import cn.zszj.module.system.dal.mysql.notify.NotifyChannelSendMapper;
import cn.zszj.module.system.service.notify.dispatch.NotifyChannel;
import cn.zszj.module.system.service.notify.dispatch.NotifyCommand;
import cn.zszj.module.system.service.notify.dispatch.NotifyRecipient;
import cn.zszj.module.system.service.notify.dispatch.NotifyRecipientContext;
import cn.zszj.module.system.service.notify.dispatch.NotifyRecipientContextResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.module.system.enums.ErrorCodeConstants.NOTIFY_CHANNEL_SEND_CONTACT_STILL_MISSING;
import static cn.zszj.module.system.enums.ErrorCodeConstants.NOTIFY_CHANNEL_SEND_MANUAL_RETRY_INVALID;
import static cn.zszj.module.system.enums.ErrorCodeConstants.NOTIFY_CHANNEL_SEND_NOT_FOUND;
import static cn.zszj.module.system.enums.ErrorCodeConstants.NOTIFY_CHANNEL_SEND_TENANT_REQUIRED;

/**
 * 渠道发送生命周期服务实现（ZS-MSG-004）。
 *
 * <p>众墅要求（docs/05 §11 ZS-MSG-004）落地要点：
 * <ol>
 *   <li><b>状态独立建模</b>：{@link NotifyChannelSendStatus} 五态只描述渠道投递，与站内消息已读/未读、
 *       业务待办状态、派发日志状态互不推断——派发 SUCCESS 仅表示进入可靠投递管道，不推定送达；</li>
 *   <li><b>可靠投递复用 JOB-002</b>：派发同事务追加 {@code NOTIFY_CHANNEL_SEND} Outbox 事件，提交由
 *       {@link NotifyChannelSendEventSink} 在 Outbox at-least-once 语义下驱动；失败退避与 DEAD 由
 *       outbox_event.retry_count/next_retry_at 承担，本表只记实际提交次数 attempt_count（不重复记账）；</li>
 *   <li><b>先回查再重发</b>：提交超时/技术异常一律 UNKNOWN 并抛 {@link NotifyChannelSendRetryableException}
 *       触发退避；重投遇到 UNKNOWN 只允许「回查确认未发出 → PENDING → 重发」一条重发路径，禁止盲目重发；
 *       渠道幂等键 {@code channelMessageId} 创建即生成、所有提交携带同一值，渠道侧可去重兜底；</li>
 *   <li><b>回执权威 + 校验</b>：送达/失败回执可从任意非终态直接推进（乱序回执不丢事实）；终态重复回执
 *       幂等吸收且 receipt_count 累加（可追踪）；未知幂等键/流水号错配显式拒绝；回执永不触发发件；</li>
 *   <li><b>人工重试留痕</b>：仅 PENDING/FAILED/UNKNOWN 可复位（ACCEPTED/DELIVERED 拒绝——人工重发会重复
 *       发件），操作者/原因/次数落台账；对账出口只对齐事实不自动重发；</li>
 *   <li><b>未配置明确阻断</b>：注册器无该渠道实现（派发时）→ 派发侧 CHANNEL_NOT_CONFIGURED（不入本表）；
 *       提交/回查时实现被移除 → FAILED(CHANNEL_NOT_CONFIGURED)，不静默丢弃；</li>
 *   <li><b>事务与脱敏</b>：创建为 MANDATORY（随派发事务回滚）；Sink 路径在事务外调渠道、短事务 CAS 推进；
 *       技术失败只存受控描述（异常类别，循 JOB-002 describeThrowable 先例），联系方式/内容不落日志。</li>
 * </ol>
 */
@Service
@Slf4j
public class NotifyChannelSendServiceImpl implements NotifyChannelSendService {

    /** 未配置渠道受控失败码（提交/回查时发送器缺失） */
    public static final String FAILED_CODE_CONFIG_MISSING = "CHANNEL_NOT_CONFIGURED";

    /** 收件人缺联系方式受控失败码（派发时明确阻断，不入投递管道） */
    public static final String FAILED_CODE_CONTACT_MISSING = "RECIPIENT_CONTACT_MISSING";

    /** 失败回执缺省受控失败码 */
    public static final String FAILED_CODE_RECEIPT = "RECEIPT_FAILED";

    private final NotifyChannelSendMapper channelSendMapper;
    private final NotifyChannelSenderRegistry senderRegistry;
    private final ReliableEventPort reliableEventPort;
    private final NotifyRecipientContextResolver recipientContextResolver;
    private final TransactionTemplate transactionTemplate;

    public NotifyChannelSendServiceImpl(NotifyChannelSendMapper channelSendMapper,
                                        NotifyChannelSenderRegistry senderRegistry,
                                        ReliableEventPort reliableEventPort,
                                        NotifyRecipientContextResolver recipientContextResolver,
                                        PlatformTransactionManager transactionManager) {
        this.channelSendMapper = channelSendMapper;
        this.senderRegistry = senderRegistry;
        this.reliableEventPort = reliableEventPort;
        this.recipientContextResolver = recipientContextResolver;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.MANDATORY)
    public NotifyChannelSendDO createFromDispatch(NotifyCommand command, NotifyRecipient recipient,
                                                  NotifyChannel channel, String contact, String content,
                                                  Long sendLogId) {
        // 派发方（NotifyDispatcherImpl）已 fail-closed 校验租户，MANDATORY 事务内直接取用
        Long tenantId = TenantContextHolder.getTenantId();
        boolean hasContact = StringUtils.hasText(contact);
        NotifyChannelSendDO record = NotifyChannelSendDO.builder()
                .tenantId(tenantId)
                .sendLogId(sendLogId)
                .eventId(command.getEventId())
                .channel(channel.name())
                .recipientType(recipient.getType().name())
                .recipientId(recipient.getId())
                .recipientContact(hasContact ? contact : null)
                .templateCode(command.getTemplateCode())
                .content(content)
                // 渠道幂等键：创建即生成、此后所有提交/回查/回执携带同一值（渠道侧去重兜底）
                .channelMessageId(generateChannelMessageId())
                .status(hasContact ? NotifyChannelSendStatus.PENDING.name() : NotifyChannelSendStatus.FAILED.name())
                .statusReason(hasContact ? null : "收件人缺少该渠道联系方式，明确阻断不入投递管道")
                .failedCode(hasContact ? null : FAILED_CODE_CONTACT_MISSING)
                .attemptCount(0)
                .receiptCount(0)
                .manualRetryCount(0)
                .actorType(command.getActorType().name())
                .actorId(command.getActorId())
                .traceId(command.getTraceId())
                .build();
        channelSendMapper.insert(record);
        if (!hasContact) {
            // 不入投递管道：无 Outbox 事件（未配置联系方式的发送永远不会被驱动，明确可查询）
            return record;
        }
        long outboxEventId = reliableEventPort.append(buildOutboxMessage(command, recipient, channel, record.getId()));
        channelSendMapper.fillOutboxEventId(record.getId(), outboxEventId);
        record.setOutboxEventId(outboxEventId);
        return record;
    }

    @Override
    public void processOutboxDelivery(long sendId, long outboxEventId) {
        Long tenantId = requireTenant();
        // r0 P1 事件身份吸收：按 (租户, outboxEventId) 定位台账——人工重试换绑后，旧事件重投/外来事件
        // 在此定位不到即幂等吸收，杜绝重叠窗口内的重复外部提交（渠道幂等键为最后一道渠道侧兜底）
        NotifyChannelSendDO record = transactionTemplate.execute(status ->
                channelSendMapper.selectByOutboxEventId(tenantId, outboxEventId));
        if (record == null || record.getId() != sendId) {
            log.warn("[processOutboxDelivery][sendId={} outboxEventId={} 台账未绑定该事件（陈旧/换绑/外来），幂等吸收]",
                    sendId, outboxEventId);
            return;
        }
        NotifyChannelSendStatus status = NotifyChannelSendStatus.valueOf(record.getStatus());
        switch (status) {
            case PENDING:
                submitRecord(record);
                return;
            case UNKNOWN:
                // 先回查再重发（众墅要求）：确认未发出才允许重发
                queryThenResend(record);
                return;
            case ACCEPTED:
            case DELIVERED:
            case FAILED:
            default:
                // 受理待回执 / 已送达 / 明确失败（人工重试路径）：重投幂等吸收，绝不重复发件
                log.info("[processOutboxDelivery][sendId={} 状态{} 无需提交，吸收重投]", sendId, status);
        }
    }

    /** 提交渠道（事务外调用发送器，短事务 CAS 推进状态）。 */
    private void submitRecord(NotifyChannelSendDO record) {
        Optional<NotifyChannelSender> senderOpt = senderRegistry.find(NotifyChannel.valueOf(record.getChannel()));
        if (!senderOpt.isPresent()) {
            // 派发后发送器被移除：明确阻断 FAILED（不静默丢弃；配置恢复后走人工重试）
            transactionTemplate.executeWithoutResult(status ->
                    channelSendMapper.casReject(record.getId(), record.getTenantId(),
                            FAILED_CODE_CONFIG_MISSING, "渠道未配置发送器（提交时缺失）", currentTimestamp()));
            log.warn("[submitRecord][sendId={} 渠道{}未配置发送器，明确阻断]", record.getId(), record.getChannel());
            return;
        }
        ChannelSubmitResult result;
        try {
            // 外部调用严格在事务外（短事务 CAS 只包落库推进）
            result = senderOpt.get().submit(record);
        } catch (Exception e) {
            // 技术异常：无法确定渠道是否已发出 → UNKNOWN（先回查再重发），抛出触发 Outbox 退避重投
            markUnknownAndThrow(record, describeThrowable(e));
            return; // 不可达：markUnknownAndThrow 必抛（编译器不能推断，显式收束）
        }
        switch (result.getOutcome()) {
            case ACCEPTED:
                Integer accepted = transactionTemplate.execute(status ->
                        channelSendMapper.casAccept(record.getId(), record.getTenantId(),
                                result.getChannelSerialNo(), currentTimestamp()));
                if (accepted == null || accepted == 0) {
                    log.warn("[submitRecord][sendId={} 受理推进落败（状态已被并发改变），幂等吸收]", record.getId());
                }
                return;
            case REJECTED:
                // 渠道明确拒绝：终局失败（不再自动重试），Outbox 事件正常确认，人工重试路径处置
                Integer rejected = transactionTemplate.execute(status ->
                        channelSendMapper.casReject(record.getId(), record.getTenantId(),
                                // r0 P2：拒绝码截断至列宽 failed_code varchar(64)，超长码不得使终局失败落不了地
                                truncate(result.getCode(), 64), truncate(result.getReason(), 512), currentTimestamp()));
                if (rejected == null || rejected == 0) {
                    log.warn("[submitRecord][sendId={} 拒绝推进落败（状态已被并发改变），幂等吸收]", record.getId());
                }
                return;
            case UNKNOWN:
            default:
                Integer unknown = transactionTemplate.execute(status ->
                        channelSendMapper.casMarkUnknown(record.getId(), record.getTenantId(),
                                truncate(result.getCode(), 256), currentTimestamp()));
                if (unknown == null || unknown == 0) {
                    log.warn("[submitRecord][sendId={} 未知推进落败（状态已被并发改变），幂等吸收]", record.getId());
                    return;
                }
                throw new NotifyChannelSendRetryableException(
                        "提交结果未知须先回查 sendId=" + record.getId() + " code=" + result.getCode());
        }
    }

    /** UNKNOWN 重投：回查渠道 → 送达推进 / 确认未发出则重发 / 仍未知退避重查。 */
    private void queryThenResend(NotifyChannelSendDO record) {
        Optional<NotifyChannelSender> senderOpt = senderRegistry.find(NotifyChannel.valueOf(record.getChannel()));
        if (!senderOpt.isPresent()) {
            transactionTemplate.executeWithoutResult(status ->
                    channelSendMapper.casReject(record.getId(), record.getTenantId(),
                            FAILED_CODE_CONFIG_MISSING, "渠道未配置发送器（回查时缺失）", currentTimestamp()));
            log.warn("[queryThenResend][sendId={} 渠道{}未配置发送器，明确阻断]", record.getId(), record.getChannel());
            return;
        }
        ChannelQueryResult result;
        try {
            result = senderOpt.get().queryByReceiptKey(record.getChannelMessageId());
        } catch (Exception e) {
            String evidence = "QUERY_FAILED " + describeThrowable(e);
            transactionTemplate.executeWithoutResult(status ->
                    channelSendMapper.casRecordQueryEvidence(record.getId(), record.getTenantId(),
                            NotifyChannelSendStatus.UNKNOWN.name(), "QUERY_FAILED", truncate(evidence, 512),
                            currentTimestamp()));
            throw new NotifyChannelSendRetryableException(
                    "回查技术异常仍未知 sendId=" + record.getId() + " errorClass=" + e.getClass().getSimpleName());
        }
        switch (result.getOutcome()) {
            case DELIVERED:
                Integer delivered = transactionTemplate.execute(status ->
                        channelSendMapper.casQueryDelivered(record.getId(), record.getTenantId(),
                                truncate(result.getEvidence(), 512), currentTimestamp()));
                if (delivered == null || delivered == 0) {
                    log.warn("[queryThenResend][sendId={} 回查送达推进落败（状态已被并发改变），幂等吸收]", record.getId());
                }
                return;
            case NOT_SENT:
                // 先回查确认未发出 → 复位 PENDING → 立即重发（唯一自动重发入口）
                Integer reset = transactionTemplate.execute(status ->
                        channelSendMapper.casQueryNotSent(record.getId(), record.getTenantId(),
                                truncate(result.getEvidence(), 512), currentTimestamp()));
                if (reset == null || reset == 0) {
                    log.warn("[queryThenResend][sendId={} 未发出复位落败（状态已被并发改变），幂等吸收]", record.getId());
                    return;
                }
                NotifyChannelSendDO fresh = transactionTemplate.execute(status -> channelSendMapper.selectById(record.getId()));
                if (fresh != null) {
                    submitRecord(fresh);
                }
                return;
            case UNKNOWN:
            default:
                transactionTemplate.executeWithoutResult(status ->
                        channelSendMapper.casRecordQueryEvidence(record.getId(), record.getTenantId(),
                                NotifyChannelSendStatus.UNKNOWN.name(), "UNKNOWN",
                                truncate(result.getEvidence(), 512), currentTimestamp()));
                throw new NotifyChannelSendRetryableException("回查仍未知退避重查 sendId=" + record.getId());
        }
    }

    @Override
    public NotifyChannelReceiptResult applyReceipt(NotifyChannelReceiptCmd cmd) {
        if (cmd == null || !StringUtils.hasText(cmd.getChannel()) || !StringUtils.hasText(cmd.getChannelMessageId())) {
            throw new IllegalArgumentException("渠道回执缺少 channel/channelMessageId");
        }
        Long tenantId = requireTenant();
        Date receiptTime = cmd.getReceiptTime() != null ? cmd.getReceiptTime() : currentTimestamp();
        return transactionTemplate.execute(status -> {
            NotifyChannelSendDO record = channelSendMapper.selectByChannelMessageId(
                    tenantId, cmd.getChannel(), cmd.getChannelMessageId());
            if (record == null) {
                // 回执校验：未知幂等键（伪造/他系统回执）显式拒绝，不建记录、不静默吸收
                log.warn("[applyReceipt][channel={} channelMessageId={} 未知回执拒绝]", cmd.getChannel(), cmd.getChannelMessageId());
                return NotifyChannelReceiptResult.rejectedUnknownReceipt();
            }
            // 回执校验：流水号比对（双方均有流水号且不一致 → 错配拒绝）
            if (StringUtils.hasText(cmd.getChannelSerialNo()) && StringUtils.hasText(record.getChannelSerialNo())
                    && !cmd.getChannelSerialNo().equals(record.getChannelSerialNo())) {
                log.warn("[applyReceipt][sendId={} 流水号错配拒绝 receipt={} recorded={}]",
                        record.getId(), maskContact(cmd.getChannelSerialNo()), maskContact(record.getChannelSerialNo()));
                return NotifyChannelReceiptResult.rejectedSerialMismatch(record.getId(), record.getStatus());
            }
            NotifyChannelSendStatus current = NotifyChannelSendStatus.valueOf(record.getStatus());
            // 终态：登记回执计数（重复回执可追踪），一致性决定 DUPLICATE / CONTRADICTION
            if (current == NotifyChannelSendStatus.DELIVERED || current == NotifyChannelSendStatus.FAILED) {
                channelSendMapper.casCountReceipt(record.getId(), tenantId, receiptTime);
                boolean consistent = (current == NotifyChannelSendStatus.DELIVERED) == cmd.isDelivered();
                log.info("[applyReceipt][sendId={} 终态{}回执 absorbed consistent={}]", record.getId(), current, consistent);
                return consistent
                        ? NotifyChannelReceiptResult.duplicate(record.getId(), record.getStatus())
                        : NotifyChannelReceiptResult.contradiction(record.getId(), record.getStatus());
            }
            // 非终态：回执权威——乱序送达回执不丢事实，直接推进
            boolean moved = cmd.isDelivered()
                    ? channelSendMapper.casDeliver(record.getId(), tenantId, receiptTime) == 1
                    : channelSendMapper.casReceiptFail(record.getId(), tenantId,
                            StringUtils.hasText(cmd.getErrorCode()) ? truncate(cmd.getErrorCode(), 64) : FAILED_CODE_RECEIPT,
                            truncate(cmd.getErrorMsg(), 512), receiptTime) == 1;
            if (!moved) {
                // 并发推进（如 Outbox 同时回查送达）→ 重读按终态一致性分类（r0 P2：矛盾回执不再误判 DUPLICATE）
                NotifyChannelSendDO fresh = channelSendMapper.selectById(record.getId());
                channelSendMapper.casCountReceipt(record.getId(), tenantId, receiptTime);
                String freshStatus = fresh != null ? fresh.getStatus() : record.getStatus();
                if (NotifyChannelSendStatus.DELIVERED.name().equals(freshStatus)
                        || NotifyChannelSendStatus.FAILED.name().equals(freshStatus)) {
                    boolean consistent = NotifyChannelSendStatus.DELIVERED.name().equals(freshStatus) == cmd.isDelivered();
                    log.info("[applyReceipt][sendId={} 推进落败（并发改变）status={} consistent={}]", record.getId(), freshStatus, consistent);
                    return consistent
                            ? NotifyChannelReceiptResult.duplicate(record.getId(), freshStatus)
                            : NotifyChannelReceiptResult.contradiction(record.getId(), freshStatus);
                }
                log.info("[applyReceipt][sendId={} 推进落败（并发改变，仍非终态 status={}），按重复吸收]", record.getId(), freshStatus);
                return NotifyChannelReceiptResult.duplicate(record.getId(), freshStatus);
            }
            String newStatus = cmd.isDelivered() ? NotifyChannelSendStatus.DELIVERED.name() : NotifyChannelSendStatus.FAILED.name();
            log.info("[applyReceipt][sendId={} 回执推进 {}→{}]", record.getId(), current, newStatus);
            return NotifyChannelReceiptResult.applied(record.getId(), newStatus);
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotifyChannelSendDO manualRetry(long sendId, String actorType, String actorId, String reason) {
        Long tenantId = requireTenant();
        NotifyChannelSendDO record = channelSendMapper.selectById(sendId);
        if (record == null || !tenantId.equals(record.getTenantId())) {
            throw exception(NOTIFY_CHANNEL_SEND_NOT_FOUND);
        }
        NotifyChannelSendStatus status = NotifyChannelSendStatus.valueOf(record.getStatus());
        // 受理/送达由回执与回查推进：人工重发会造成重复发件，显式拒绝
        if (status == NotifyChannelSendStatus.ACCEPTED || status == NotifyChannelSendStatus.DELIVERED) {
            throw exception(NOTIFY_CHANNEL_SEND_MANUAL_RETRY_INVALID, record.getStatus());
        }
        // r0 P2：联系方式缺失记录的唯一恢复路径——复位前重新解析（用户可能已补绑手机号/邮箱）；
        // 仍缺失则拒绝（避免把 null 联系方式交给发送器）
        String contact = record.getRecipientContact();
        if (!StringUtils.hasText(contact)) {
            contact = resolveContactForRetry(record);
            if (!StringUtils.hasText(contact)) {
                throw exception(NOTIFY_CHANNEL_SEND_CONTACT_STILL_MISSING);
            }
        }
        String trimmedActorType = truncate(actorType, 16);
        String trimmedActorId = truncate(actorId, 64);
        // 先追加新投递事件（MANDATORY 端口要求事务内），再按旧事件绑定做条件 CAS 复位并换绑；
        // CAS 落败（并发重试/并发终态化）抛异常回滚——连随追加的事件一并回滚，不产生多余存活事件
        long outboxEventId = reliableEventPort.append(
                buildRetryOutboxMessage(record, trimmedActorType, trimmedActorId));
        int updated = channelSendMapper.casManualRetry(record.getId(), tenantId, record.getOutboxEventId(),
                contact, trimmedActorType, trimmedActorId, truncate(reason, 512), outboxEventId, currentTimestamp());
        if (updated == 0) {
            throw exception(NOTIFY_CHANNEL_SEND_MANUAL_RETRY_INVALID, record.getStatus());
        }
        record.setStatus(NotifyChannelSendStatus.PENDING.name());
        record.setStatusReason("人工重试");
        record.setRecipientContact(contact);
        record.setFailedCode(null);
        record.setLastError(null);
        record.setManualRetryCount(record.getManualRetryCount() == null ? 1 : record.getManualRetryCount() + 1);
        record.setLastRetryActorType(trimmedActorType);
        record.setLastRetryActorId(trimmedActorId);
        record.setLastRetryReason(truncate(reason, 512));
        record.setOutboxEventId(outboxEventId);
        log.info("[manualRetry][sendId={} 人工重试 actor={}/{} 新投递事件={}]", sendId, actorType, actorId, outboxEventId);
        return record;
    }

    /** 人工重试时重新解析收件联系方式（循 MSG-001 Resolver 合同：仅 ADMIN；无效/仍缺失返回 null）。 */
    private String resolveContactForRetry(NotifyChannelSendDO record) {
        NotifyRecipient recipient = "MEMBER".equals(record.getRecipientType())
                ? NotifyRecipient.member(record.getRecipientId())
                : NotifyRecipient.admin(record.getRecipientId());
        NotifyRecipientContext ctx = recipientContextResolver.resolve(recipient);
        if (!ctx.isValid()) {
            return null;
        }
        return NotifyChannelContacts.contactFor(NotifyChannel.valueOf(record.getChannel()), ctx);
    }

    @Override
    public NotifyChannelReconcileResult reconcile(long sendId) {
        Long tenantId = requireTenant();
        NotifyChannelSendDO record = transactionTemplate.execute(status -> channelSendMapper.selectById(sendId));
        if (record == null || !tenantId.equals(record.getTenantId())) {
            return new NotifyChannelReconcileResult(NotifyChannelReconcileResult.Outcome.NOT_APPLICABLE, null, null, "记录不存在");
        }
        NotifyChannelSendStatus status = NotifyChannelSendStatus.valueOf(record.getStatus());
        // 对账只针对「受理待回执」与「结果未知」；PENDING 未入渠道、终态已定，无账可对
        if (status != NotifyChannelSendStatus.ACCEPTED && status != NotifyChannelSendStatus.UNKNOWN) {
            return new NotifyChannelReconcileResult(NotifyChannelReconcileResult.Outcome.NOT_APPLICABLE,
                    record.getId(), record.getStatus(), "状态不适用对账");
        }
        Optional<NotifyChannelSender> senderOpt = senderRegistry.find(NotifyChannel.valueOf(record.getChannel()));
        if (!senderOpt.isPresent()) {
            return new NotifyChannelReconcileResult(NotifyChannelReconcileResult.Outcome.QUERY_FAILED,
                    record.getId(), record.getStatus(), FAILED_CODE_CONFIG_MISSING);
        }
        ChannelQueryResult result;
        try {
            result = senderOpt.get().queryByReceiptKey(record.getChannelMessageId());
        } catch (Exception e) {
            return new NotifyChannelReconcileResult(NotifyChannelReconcileResult.Outcome.QUERY_FAILED,
                    record.getId(), record.getStatus(), truncate(describeThrowable(e), 512));
        }
        switch (result.getOutcome()) {
            case DELIVERED:
                // casQueryDelivered 接受 ACCEPTED/UNKNOWN（对账与 Outbox 回查共用推进出口）
                Integer delivered = transactionTemplate.execute(s ->
                        channelSendMapper.casQueryDelivered(record.getId(), tenantId,
                                truncate(result.getEvidence(), 512), currentTimestamp()));
                if (delivered != null && delivered > 0) {
                    return new NotifyChannelReconcileResult(NotifyChannelReconcileResult.Outcome.RECONCILED_DELIVERED,
                            record.getId(), NotifyChannelSendStatus.DELIVERED.name(), result.getEvidence());
                }
                // 落败：并发已推进，重读对齐结论
                NotifyChannelSendDO fresh = transactionTemplate.execute(s -> channelSendMapper.selectById(sendId));
                String freshStatus = fresh != null ? fresh.getStatus() : record.getStatus();
                boolean nowDelivered = NotifyChannelSendStatus.DELIVERED.name().equals(freshStatus);
                return new NotifyChannelReconcileResult(
                        nowDelivered ? NotifyChannelReconcileResult.Outcome.RECONCILED_DELIVERED
                                : NotifyChannelReconcileResult.Outcome.STILL_UNKNOWN,
                        record.getId(), freshStatus, result.getEvidence());
            case NOT_SENT:
                // 对账出口不自动重发：只登记证据（ACCEPTED+未发出属受理矛盾，交人工核实）
                transactionTemplate.executeWithoutResult(s ->
                        channelSendMapper.casRecordQueryEvidence(record.getId(), tenantId, record.getStatus(),
                                "NOT_SENT", truncate(result.getEvidence(), 512), currentTimestamp()));
                return new NotifyChannelReconcileResult(NotifyChannelReconcileResult.Outcome.CONFIRMED_NOT_SENT,
                        record.getId(), record.getStatus(), result.getEvidence());
            case UNKNOWN:
            default:
                transactionTemplate.executeWithoutResult(s ->
                        channelSendMapper.casRecordQueryEvidence(record.getId(), tenantId, record.getStatus(),
                                "UNKNOWN", truncate(result.getEvidence(), 512), currentTimestamp()));
                return new NotifyChannelReconcileResult(NotifyChannelReconcileResult.Outcome.STILL_UNKNOWN,
                        record.getId(), record.getStatus(), result.getEvidence());
        }
    }

    @Override
    public NotifyChannelSendDO getNotifyChannelSend(long id) {
        Long tenantId = requireTenant();
        NotifyChannelSendDO record = transactionTemplate.execute(status -> channelSendMapper.selectById(id));
        // r0 P3：显式租户等值校验（与 manualRetry/reconcile 的「拦截器 + 显式谓词」双保险对齐）
        return record != null && tenantId.equals(record.getTenantId()) ? record : null;
    }

    @Override
    public List<NotifyChannelSendDO> listByStatus(String status, int limit) {
        Long tenantId = requireTenant();
        NotifyChannelSendStatus.valueOf(status); // 非法状态名直接 IllegalArgumentException（调用方编程错误）
        int boundedLimit = Math.min(Math.max(limit, 1), 200);
        return transactionTemplate.execute(tx ->
                channelSendMapper.selectListByStatus(tenantId, status, boundedLimit));
    }

    /** 生成渠道幂等键（NC- 前缀 + 去连字符 UUID，35 字符 ≤ channel_message_id varchar(64)）。 */
    private String generateChannelMessageId() {
        return "NC-" + UUID.randomUUID().toString().replace("-", "");
    }

    /** 构建投递 Outbox 事件（派发路径：biz 引用继承命令；payload 只含投递句柄，不含内容/联系方式）。 */
    private OutboxEventMessage buildOutboxMessage(NotifyCommand command, NotifyRecipient recipient,
                                                  NotifyChannel channel, Long sendId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sendId", sendId);
        payload.put("eventId", command.getEventId());
        payload.put("channel", channel.name());
        payload.put("recipientType", recipient.getType().name());
        payload.put("recipientId", recipient.getId());
        payload.put("templateCode", command.getTemplateCode());
        return OutboxEventMessage.builder()
                .eventType(OUTBOX_EVENT_TYPE)
                .bizType(command.getBizType() != null ? command.getBizType() : "notify_channel_send")
                .bizId(command.getBizId() != null ? command.getBizId() : String.valueOf(sendId))
                .bizVersion(command.getBizVersion())
                .payload(payload)
                .actorType(command.getActorType())
                .actorId(command.getActorId())
                .traceId(command.getTraceId())
                .build();
    }

    /**
     * 构建人工重试投递事件（biz 引用以台账为中心——台账未存 bizType/bizId，重试事件统一用
     * notify_channel_send/sendId 引用；原 eventId 保留供链路回查；actor 取本次重试操作者入参——r0 P3）。
     */
    private OutboxEventMessage buildRetryOutboxMessage(NotifyChannelSendDO record, String actorType, String actorId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sendId", record.getId());
        payload.put("eventId", record.getEventId());
        payload.put("channel", record.getChannel());
        payload.put("recipientType", record.getRecipientType());
        payload.put("recipientId", record.getRecipientId());
        payload.put("templateCode", record.getTemplateCode());
        payload.put("manualRetry", true);
        return OutboxEventMessage.builder()
                .eventType(OUTBOX_EVENT_TYPE)
                .bizType("notify_channel_send")
                .bizId(String.valueOf(record.getId()))
                .payload(payload)
                .actorType(parseActorType(actorType))
                .actorId(actorId)
                .traceId(record.getTraceId())
                .build();
    }

    /** 操作者类型串 → OutboxActorType（宽松解析，无法识别回退 ADMIN——仅影响事件登记语义，不影响台账）。 */
    private OutboxActorType parseActorType(String actorType) {
        if (StringUtils.hasText(actorType)) {
            try {
                return OutboxActorType.valueOf(actorType.trim());
            } catch (IllegalArgumentException ignore) {
                // 落到默认
            }
        }
        return OutboxActorType.ADMIN;
    }

    /** UNKNOWN 推进 + 抛可重试异常（先落 UNKNOWN 再抛，保证状态先持久）。 */
    private void markUnknownAndThrow(NotifyChannelSendDO record, String controlledError) {
        transactionTemplate.executeWithoutResult(status ->
                channelSendMapper.casMarkUnknown(record.getId(), record.getTenantId(),
                        truncate(controlledError, 256), currentTimestamp()));
        throw new NotifyChannelSendRetryableException(
                "提交技术异常转 UNKNOWN sendId=" + record.getId() + " error=" + controlledError);
    }

    /** 租户上下文强制（循 JOB-002/MSG-001 fail-closed 先例：缺失即拒绝，不默认 0）。 */
    private Long requireTenant() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw exception(NOTIFY_CHANNEL_SEND_TENANT_REQUIRED);
        }
        return tenantId;
    }

    /**
     * 失败留痕：异常消息是自由文本，可能携带响应正文/凭据，无可靠值级脱敏——只存受控描述
     * （异常类别 + 消息长度），不落异常原文（循 JOB-002 OutboxDispatcherService.describeThrowable 先例）。
     */
    private String describeThrowable(Throwable e) {
        String message = e.getMessage();
        try {
            return JsonUtils.toJsonString(Map.of(
                    "errorClass", e.getClass().getSimpleName(),
                    "messageLength", message == null ? 0 : message.length()));
        } catch (Exception ex) {
            return "{\"errorClass\":\"(未知)\",\"messageLength\":0}";
        }
    }

    /** 联系方式/流水号日志掩码（保留前 3 后 4，中段掩码；短值全掩）。 */
    private String maskContact(String value) {
        if (!StringUtils.hasText(value)) {
            return "(空)";
        }
        if (value.length() <= 7) {
            return "***";
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static Date currentTimestamp() {
        return new Date();
    }

}
