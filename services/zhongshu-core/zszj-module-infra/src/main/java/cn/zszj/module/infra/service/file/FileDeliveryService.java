package cn.zszj.module.infra.service.file;

import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileDeliveryChunkRespVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileDeliverySessionRespVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileDeliveryTicketIssueReqVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileDeliveryTicketIssueRespVO;

/**
 * 文件交付票据与鉴权取流 Service 接口（ZS-FILE-004.A）。
 *
 * <p>协议：签发一次性票据（只存散列）→ 主体原子兑换一次建立下载会话（绑定主体/登录会话/资产版本/用途）
 * → 同会话内 Range/断线续传按同一不可变版本经后端鉴权取流（不返回存储 URL）→ 兑换后退出/撤权/过期
 * 均拒绝；在途传输逐块重检，撤权后停止后续输出。</p>
 */
public interface FileDeliveryService {

    /**
     * 签发一次性交付票据（本人；服务端只存 SHA-256 散列，明文 token 仅本次返回）。
     *
     * @param reqVO        签发参数（文件编号、用途 download/export）
     * @param loginUser    登录主体（票据与其绑定）
     * @param loginSession 登录会话标识（兑换时写入会话绑定）
     * @return 票据 token 与有效期
     */
    FileDeliveryTicketIssueRespVO issueDeliveryTicket(FileDeliveryTicketIssueReqVO reqVO,
                                                      LoginUser loginUser, String loginSession);

    /**
     * 原子兑换票据建立下载会话。
     *
     * 兑换谓词（owner·tenant·purpose·status·有效期）不匹配即拒绝且不消费票据；
     * 同主体同登录会话重复兑换幂等返回既有会话（断线重连），不得新建。
     *
     * @param ticketToken  票据明文 token
     * @param purpose      用途（须与签发一致）
     * @param loginUser    登录主体
     * @param loginSession 登录会话标识（绑定到会话）
     * @return 下载会话（ID + 资产总大小）
     */
    FileDeliverySessionRespVO redeemDeliveryTicket(String ticketToken, String purpose,
                                                   LoginUser loginUser, String loginSession);

    /**
     * 鉴权取流分块（Range 语义 [start, endInclusive]，越界收敛到内容末尾）。
     *
     * 每块重检：会话有效（未撤权/未过期）、登录会话标识一致、主体身份一致（会话 ID 不单独代替认证）、
     * 对象读权限未回收——任一失败停止后续输出。
     *
     * @param deliverySessionId 下载会话 ID
     * @param start             起始字节（含）
     * @param endInclusive      结束字节（含）
     * @param loginUser         登录主体（身份重检）
     * @param loginSession      登录会话标识（须与兑换时一致）
     * @return 分块内容与资产元信息
     */
    FileDeliveryChunkRespVO readDeliveryChunk(String deliverySessionId, Long start, Long endInclusive,
                                              LoginUser loginUser, String loginSession);

    /**
     * 撤权：终止下载会话（在途传输的后续分块将被重检拦截）。
     *
     * @param deliverySessionId 下载会话 ID
     */
    void revokeDelivery(String deliverySessionId);

}
