package cn.zszj.module.infra.service.file;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.core.util.StrUtil;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileDeliveryChunkRespVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileDeliverySessionRespVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileDeliveryTicketIssueReqVO;
import cn.zszj.module.infra.controller.admin.file.vo.file.FileDeliveryTicketIssueRespVO;
import cn.zszj.module.infra.dal.dataobject.file.FileDO;
import cn.zszj.module.infra.dal.dataobject.file.FileDeliveryTicketDO;
import cn.zszj.module.infra.dal.mysql.file.FileDeliveryTicketMapper;
import cn.zszj.module.infra.dal.mysql.file.FileMapper;
import cn.zszj.module.infra.framework.file.core.client.FileClient;
import cn.zszj.framework.common.biz.system.permission.PermissionCommonApi;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.UUID;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.module.infra.enums.ErrorCodeConstants.*;

/**
 * 文件交付票据与鉴权取流 Service 实现（ZS-FILE-004.A）。
 *
 * <p>适配供体 JdbcDeliveryPort（散列存储、到期条件、原子一次消费）并补齐其消费 SQL 缺失的
 * owner·tenant·purpose 匹配谓词——供体实现不能原样当作完整授权（卡片要求）。默认后端鉴权取流：
 * 不向私有附件调用者返回任何可绕过平台授权的存储 URL（含预签名 GET）。</p>
 */
@Service
@Slf4j
@Validated
public class FileDeliveryServiceImpl implements FileDeliveryService {

    /** 票据/下载会话有效期（分钟）：与 FILE-003 上传凭证 30 分钟合同对齐。 */
    private static final int TICKET_EXPIRE_MINUTES = 30;

    /** 服务端分块上限（字节）：单次取流至多 1MiB，保证在途撤权重检间隔与单块内存有界（codex r0 P2）。 */
    private static final long MAX_CHUNK_BYTES = 1024L * 1024L;

    @Resource
    private FileDeliveryTicketMapper deliveryTicketMapper;

    @Resource
    private FileMapper fileMapper;

    @Resource
    private FileService fileService;

    @Resource
    private FileConfigService fileConfigService;

    @Resource
    private PermissionCommonApi permissionCommonApi;

    // ========== 签发 ==========

    @Override
    public FileDeliveryTicketIssueRespVO issueDeliveryTicket(FileDeliveryTicketIssueReqVO reqVO,
                                                             LoginUser loginUser, String loginSession) {
        requireLoginUser(loginUser);
        // 读授权重检（签发时点）：PUBLIC 匿名可读；PRIVATE 本人或同租户 infra:file:query——
        // 复用 FILE-001.A 的统一读取授权，票据只可能签发给「当前有权读该文件」的主体
        FileDO file = requireFile(reqVO.getFileId());
        fileService.validateFileReadable(file, loginUser);

        String ticketToken = UUID.randomUUID().toString().replace("-", "");
        FileDeliveryTicketDO ticket = new FileDeliveryTicketDO();
        ticket.setTicketHash(DigestUtil.sha256Hex(ticketToken)); // 票据不明文落库
        ticket.setFileId(file.getId());
        ticket.setOwnerUserId(loginUser.getId());
        ticket.setPurpose(reqVO.getPurpose());
        ticket.setStatus(FileDeliveryTicketDO.STATUS_WAITING);
        ticket.setTenantId(loginUser.getTenantId());
        ticket.setExpiresTime(LocalDateTime.now().plusMinutes(TICKET_EXPIRE_MINUTES));
        deliveryTicketMapper.insert(ticket);

        FileDeliveryTicketIssueRespVO respVO = new FileDeliveryTicketIssueRespVO();
        respVO.setTicketToken(ticketToken);
        respVO.setExpiresTime(ticket.getExpiresTime());
        return respVO;
    }

    // ========== 兑换 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileDeliverySessionRespVO redeemDeliveryTicket(String ticketToken, String purpose,
                                                          LoginUser loginUser, String loginSession) {
        requireLoginUser(loginUser);
        FileDeliveryTicketDO ticket = deliveryTicketMapper.selectByTicketHash(DigestUtil.sha256Hex(ticketToken));
        // 不区分「token 不存在」与「谓词不匹配」，统一 FORBIDDEN（不泄露票据存在性）
        if (ticket == null) {
            throw exception(FILE_DELIVERY_TICKET_FORBIDDEN);
        }
        // 消费前谓词：owner·tenant·purpose 必须匹配（供体缺失的关键谓词），不匹配不消费
        if (!loginUser.getId().equals(ticket.getOwnerUserId())
                || !loginUser.getTenantId().equals(ticket.getTenantId())
                || !StrUtil.equals(purpose, ticket.getPurpose())) {
            throw exception(FILE_DELIVERY_TICKET_FORBIDDEN);
        }
        if (FileDeliveryTicketDO.STATUS_REVOKED.equals(ticket.getStatus())) {
            throw exception(FILE_DELIVERY_TICKET_REVOKED);
        }
        // 已兑换：同主体同登录会话幂等返回既有会话（断线重连不新建）；换登录会话重放拒绝。
        // 幂等返回前重检（codex r0 P2）：过期与当前读授权——不向调用方返回已失效的会话
        if (FileDeliveryTicketDO.STATUS_REDEEMED.equals(ticket.getStatus())) {
            if (!StrUtil.equals(loginSession, ticket.getLoginSession())) {
                throw exception(FILE_DELIVERY_TICKET_FORBIDDEN);
            }
            return returnExistingSession(ticket, loginUser);
        }
        if (ticket.getExpiresTime().isBefore(LocalDateTime.now())) {
            throw exception(FILE_DELIVERY_TICKET_EXPIRED);
        }
        // 交付重检（codex r0 P1：资产已进入删除中间态 → 拒绝新建会话，使引用检查与兑换串行化）
        FileDO file = requireFile(ticket.getFileId());
        if (FileDO.STATUS_DELETING.equals(file.getStatus())) {
            throw exception(FILE_DELIVERY_TICKET_REVOKED);
        }
        // 读授权在签发后可能被回收（角色撤权、scope 调整等）——失败按撤权拒绝
        try {
            fileService.validateFileReadable(file, loginUser);
        } catch (Exception ex) {
            log.warn("[redeemDeliveryTicket][票据({}) 交付重检失败，按撤权拒绝 file({})]", ticket.getId(), file.getId(), ex);
            throw exception(FILE_DELIVERY_TICKET_REVOKED);
        }

        // 原子兑换（CAS）：消费 SQL 带 owner·tenant·purpose·status·有效期谓词，恰好一次成功
        String deliverySessionId = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime now = LocalDateTime.now();
        int affected = deliveryTicketMapper.update(null, new LambdaUpdateWrapper<FileDeliveryTicketDO>()
                .set(FileDeliveryTicketDO::getStatus, FileDeliveryTicketDO.STATUS_REDEEMED)
                .set(FileDeliveryTicketDO::getDeliverySessionId, deliverySessionId)
                .set(FileDeliveryTicketDO::getLoginSession, loginSession)
                .set(FileDeliveryTicketDO::getRedeemTime, now)
                .eq(FileDeliveryTicketDO::getId, ticket.getId())
                .eq(FileDeliveryTicketDO::getOwnerUserId, loginUser.getId())
                .eq(FileDeliveryTicketDO::getTenantId, loginUser.getTenantId())
                .eq(FileDeliveryTicketDO::getPurpose, purpose)
                .eq(FileDeliveryTicketDO::getStatus, FileDeliveryTicketDO.STATUS_WAITING)
                .gt(FileDeliveryTicketDO::getExpiresTime, now));
        if (affected == 0) {
            // 并发竞态：重读核对——同主体同登录会话已兑换则幂等返回（同样先重检），其余拒绝（不新建会话）
            FileDeliveryTicketDO current = deliveryTicketMapper.selectByTicketHash(ticket.getTicketHash());
            if (current != null && FileDeliveryTicketDO.STATUS_REDEEMED.equals(current.getStatus())
                    && loginUser.getId().equals(current.getOwnerUserId())
                    && StrUtil.equals(loginSession, current.getLoginSession())) {
                return returnExistingSession(current, loginUser);
            }
            throw exception(FILE_DELIVERY_TICKET_FORBIDDEN);
        }
        // codex r2 P2：CAS 成功后同事务复核资产状态——兑换读 PUBLISHED 与票据 CAS 之间若删除已完成
        //（DELETING/记录移除），回滚本事务撤销该会话；反之票据先提交则删除侧引用检查会命中并放弃
        FileDO after = fileMapper.selectById(ticket.getFileId());
        if (after == null || FileDO.STATUS_DELETING.equals(after.getStatus())) {
            throw exception(FILE_DELIVERY_TICKET_REVOKED);
        }
        ticket.setStatus(FileDeliveryTicketDO.STATUS_REDEEMED);
        ticket.setDeliverySessionId(deliverySessionId);
        ticket.setLoginSession(loginSession);
        return buildSessionResponse(ticket);
    }

    // ========== 鉴权取流 ==========

    @Override
    public FileDeliveryChunkRespVO readDeliveryChunk(String deliverySessionId, Long start, Long endInclusive,
                                                     LoginUser loginUser, String loginSession) {
        requireLoginUser(loginUser);
        FileDeliveryTicketDO ticket = deliveryTicketMapper.selectByDeliverySessionId(deliverySessionId);
        if (ticket == null) {
            throw exception(FILE_DELIVERY_SESSION_INVALID);
        }
        // 撤权与过期重检（在途传输分块级——撤权/过期后停止后续输出）
        if (FileDeliveryTicketDO.STATUS_REVOKED.equals(ticket.getStatus())) {
            throw exception(FILE_DELIVERY_TICKET_REVOKED);
        }
        if (!FileDeliveryTicketDO.STATUS_REDEEMED.equals(ticket.getStatus())) {
            throw exception(FILE_DELIVERY_SESSION_INVALID);
        }
        if (ticket.getExpiresTime().isBefore(LocalDateTime.now())) {
            throw exception(FILE_DELIVERY_TICKET_EXPIRED);
        }
        // 主体身份重检（转发会话 ID/地址给他人：owner·tenant 谓词拒绝）——必须先于会话重绑定，
        // 防止跨主体请求污染绑定值
        if (!loginUser.getId().equals(ticket.getOwnerUserId())
                || !loginUser.getTenantId().equals(ticket.getTenantId())) {
            throw exception(FILE_DELIVERY_TICKET_FORBIDDEN);
        }
        // 登录会话绑定：会话标识为服务端 token 派生（SHA-256）。同主体且标识变化（令牌刷新/重登录）
        // → 重绑定续传（codex r1 P2：令牌刷新不得中断交付）。
        // codex r2 P1：仅定点更新 login_session，且条件化于「仍 REDEEMED 且未过期」——
        // 整行 updateById 会把读取快照中的 REDEEMED 状态写回，逆转并发撤权
        if (!StrUtil.equals(loginSession, ticket.getLoginSession())) {
            int rebound = deliveryTicketMapper.update(null, new LambdaUpdateWrapper<FileDeliveryTicketDO>()
                    .set(FileDeliveryTicketDO::getLoginSession, loginSession)
                    .eq(FileDeliveryTicketDO::getId, ticket.getId())
                    .eq(FileDeliveryTicketDO::getStatus, FileDeliveryTicketDO.STATUS_REDEEMED)
                    .gt(FileDeliveryTicketDO::getExpiresTime, LocalDateTime.now()));
            if (rebound == 0) {
                // 并发撤权/过期发生在读取与重绑定之间：拒绝本次取流
                throw exception(FILE_DELIVERY_TICKET_REVOKED);
            }
            ticket.setLoginSession(loginSession);
            log.debug("[readDeliveryChunk][会话({}) 登录会话重绑定（同主体令牌变更）]", deliverySessionId);
        }
        FileDO file = requireFile(ticket.getFileId());
        // 读权限重检（每次取流均重检撤权状态）。
        // 注：不在此检查资产 DELETING——引用保护命中时删除会回退 PUBLISHED（codex r1 P2-2），
        // DELETING 仅是瞬态；取流侧仅兑换被 DELETING 阻断，存量会话由引用保护与对象删除事实兜底
        try {
            fileService.validateFileReadable(file, loginUser);
        } catch (Exception ex) {
            log.warn("[readDeliveryChunk][会话({}) 读权限重检失败，按撤权拒绝 file({})]", deliverySessionId, file.getId(), ex);
            throw exception(FILE_DELIVERY_TICKET_REVOKED);
        }

        // Range 语义 [start, endInclusive]：先以资产元数据校验边界，并以【服务端分块上限】收敛——
        // 客户端自选边界不得放大单块规模架空在途撤权重检（codex r0 P2）
        long totalSize = file.getSize() != null ? file.getSize() : 0L;
        long begin = start != null ? start : 0L;
        long requestedEnd = endInclusive != null ? endInclusive : totalSize - 1;
        if (begin < 0 || requestedEnd < begin || begin >= totalSize) {
            throw exception(FILE_DELIVERY_SESSION_INVALID);
        }
        long end = Math.min(requestedEnd, Math.min(begin + MAX_CHUNK_BYTES - 1, totalSize - 1));
        // 后端鉴权取流：范围读取（codex r1 P2：存储流量与内存随分块伸缩，不整对象拉取），按同一不可变版本交付
        FileClient client = fileConfigService.getFileClient(file.getConfigId());
        if (client == null) {
            throw exception(FILE_NOT_EXISTS);
        }
        int readLength = (int) (end - begin + 1);
        byte[] content;
        try {
            content = client.getContentRange(file.getPath(), begin, readLength);
        } catch (Exception ex) {
            log.error("[readDeliveryChunk][文件({}) 范围读取失败]", file.getId(), ex);
            throw exception(FILE_DELIVERY_SESSION_INVALID);
        }
        if (content == null) {
            throw exception(FILE_NOT_EXISTS);
        }
        // codex r2 P2：范围读取返回长度必须等于请求长度——存储/元数据不一致（对象短于记录 size）
        // 显式失败，不得以空块或短块伪装「有效续传/完成」
        if (content.length != readLength) {
            log.error("[readDeliveryChunk][文件({}) 存储范围读取长度（{}）与请求（{}）不符，疑似元数据不一致]",
                    file.getId(), content.length, readLength);
            throw exception(FILE_DELIVERY_SESSION_INVALID);
        }
        FileDeliveryChunkRespVO respVO = new FileDeliveryChunkRespVO();
        respVO.setContent(content);
        respVO.setTotalSize(totalSize);
        respVO.setLast(end >= totalSize - 1);
        return respVO;
    }

    // ========== 撤权 ==========

    @Override
    public void revokeDelivery(String deliverySessionId, LoginUser loginUser) {
        requireLoginUser(loginUser);
        FileDeliveryTicketDO ticket = deliveryTicketMapper.selectByDeliverySessionId(deliverySessionId);
        if (ticket == null) {
            throw exception(FILE_DELIVERY_SESSION_INVALID);
        }
        // 撤权授权（codex r0 P1）：本人（owner·tenant 匹配）或同租户管理员（infra:file:query）——
        // 表为 @TenantIgnore 全局表，必须显式校验，防止仅凭他人会话 ID（含跨租户）撤权
        boolean ownerMatched = loginUser.getId().equals(ticket.getOwnerUserId())
                && loginUser.getTenantId().equals(ticket.getTenantId());
        boolean managerMatched = !ownerMatched
                && loginUser.getTenantId().equals(ticket.getTenantId())
                && hasQueryPermission(loginUser);
        if (!ownerMatched && !managerMatched) {
            throw exception(FILE_DELIVERY_TICKET_FORBIDDEN);
        }
        deliveryTicketMapper.update(null, new LambdaUpdateWrapper<FileDeliveryTicketDO>()
                .set(FileDeliveryTicketDO::getStatus, FileDeliveryTicketDO.STATUS_REVOKED)
                .eq(FileDeliveryTicketDO::getDeliverySessionId, deliverySessionId)
                .eq(FileDeliveryTicketDO::getStatus, FileDeliveryTicketDO.STATUS_REDEEMED));
    }

    // ========== 内部方法 ==========

    private void requireLoginUser(LoginUser loginUser) {
        if (loginUser == null || loginUser.getId() == null || loginUser.getTenantId() == null) {
            throw exception(FILE_DELIVERY_TICKET_FORBIDDEN);
        }
    }

    /**
     * 管理面权限判定（与 @ss.hasPermission 同源 PermissionCommonApi）；系统异常保守拒绝。
     */
    private boolean hasQueryPermission(LoginUser loginUser) {
        try {
            return permissionCommonApi.hasAnyPermissions(loginUser.getId(), "infra:file:query");
        } catch (Exception ex) {
            log.warn("[hasQueryPermission][用户({}) 权限查询失败，保守拒绝]", loginUser.getId(), ex);
            return false;
        }
    }

    private FileDO requireFile(Long fileId) {
        FileDO file = fileMapper.selectById(fileId);
        if (file == null) {
            throw exception(FILE_NOT_EXISTS);
        }
        return file;
    }

    private FileDeliverySessionRespVO buildSessionResponse(FileDeliveryTicketDO ticket) {
        FileDO file = requireFile(ticket.getFileId());
        FileDeliverySessionRespVO respVO = new FileDeliverySessionRespVO();
        respVO.setDeliverySessionId(ticket.getDeliverySessionId());
        respVO.setTotalSize(file.getSize());
        return respVO;
    }

    /**
     * 幂等返回既有下载会话前的重检（codex r0 P2）：已过期或当前读授权已回收时，
     * 不向调用方返回「看似有效」的会话。
     */
    private FileDeliverySessionRespVO returnExistingSession(FileDeliveryTicketDO ticket, LoginUser loginUser) {
        if (ticket.getExpiresTime().isBefore(LocalDateTime.now())) {
            throw exception(FILE_DELIVERY_TICKET_EXPIRED);
        }
        FileDO file = requireFile(ticket.getFileId());
        try {
            fileService.validateFileReadable(file, loginUser);
        } catch (Exception ex) {
            log.warn("[returnExistingSession][票据({}) 读权限已回收，按撤权拒绝 file({})]", ticket.getId(), file.getId(), ex);
            throw exception(FILE_DELIVERY_TICKET_REVOKED);
        }
        return buildSessionResponse(ticket);
    }

}
