package cn.zszj.module.system.service.notify.channel;

import cn.zszj.module.system.dal.dataobject.notify.NotifyChannelSendDO;
import cn.zszj.module.system.service.notify.dispatch.NotifyChannel;

/**
 * 渠道发送器 SPI（ZS-MSG-004）——每种渠道一个实现，由 {@link NotifyChannelSenderRegistry} 收集。
 *
 * <p>众墅要求（docs/05 §11 ZS-MSG-004）：「未配置渠道明确阻断或保留待发送，不静默丢弃；
 * 对超时不确定结果先回查再重发；建立渠道幂等键」。合同：
 * <ol>
 *   <li><b>无实现 = 未配置</b>：注册器查不到该渠道实现 → 派发侧记 CHANNEL_NOT_CONFIGURED 明确阻断
 *       （B05 无任何生产实现——真实短信/邮件/推送适配器受 D-10 门禁，归 B11/D-10 后另立）；</li>
 *   <li><b>渠道幂等键</b>：{@code submit} 必须携带 {@code channelMessageId}（我方生成的渠道幂等键）——
 *       同一记录的重发/重试/重复事件到达渠道时按该键去重，不得每次生成新键；</li>
 *   <li><b>三态提交结果</b>：ACCEPTED（受理，携渠道流水号）/ REJECTED（明确拒绝，携失败码——终局，
 *       不再自动重试）/ UNKNOWN（超时或结果不确定——调用方必须先 {@link #queryByReceiptKey} 回查，
 *       确认未发出才允许重发，禁止直接重发）；技术异常直接抛出，由服务归一化为 UNKNOWN；</li>
 *   <li><b>实现自身无状态、外部调用不持锁</b>：提交/回查在事务外执行，落库推进由
 *       {@link NotifyChannelSendService} 以短事务 CAS 完成。</li>
 * </ol>
 */
public interface NotifyChannelSender {

    /** 本发送器承载的渠道 */
    NotifyChannel channel();

    /**
     * 提交发送。
     *
     * @param record 渠道发送台账记录（PENDING；携 channelMessageId / recipientContact / content）
     * @return 三态结果，不得返回 null
     * @throws Exception 技术异常（网络中断等）——服务归一化为 UNKNOWN（先回查再重发）
     */
    ChannelSubmitResult submit(NotifyChannelSendDO record) throws Exception;

    /**
     * 按渠道幂等键回查发送结果（对 UNKNOWN 记录的「先回查」与对 ACCEPTED 记录的对账共用）。
     *
     * @param channelMessageId 渠道幂等键
     * @return 三态回查结论，不得返回 null
     * @throws Exception 技术异常——回查失败按「仍未知」处理，退避后重查
     */
    ChannelQueryResult queryByReceiptKey(String channelMessageId) throws Exception;

}
