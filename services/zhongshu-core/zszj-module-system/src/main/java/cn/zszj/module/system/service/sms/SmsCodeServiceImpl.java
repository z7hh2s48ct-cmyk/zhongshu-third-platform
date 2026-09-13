package cn.zszj.module.system.service.sms;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.zszj.framework.common.util.servlet.ServletUtils;
import cn.zszj.module.system.api.sms.dto.code.SmsCodeSendReqDTO;
import cn.zszj.module.system.api.sms.dto.code.SmsCodeUseReqDTO;
import cn.zszj.module.system.api.sms.dto.code.SmsCodeValidateReqDTO;
import cn.zszj.module.system.dal.dataobject.sms.SmsCodeDO;
import cn.zszj.module.system.dal.mysql.sms.SmsCodeMapper;
import cn.zszj.module.system.dal.redis.sms.SmsCodeSecurityRedisDAO;
import cn.zszj.module.system.dal.redis.sms.SmsCodeSecurityRedisDAO.IpQuotaReservation;
import cn.zszj.module.system.dal.redis.sms.SmsCodeSecurityRedisDAO.ValidateAttemptReservation;
import cn.zszj.module.system.enums.sms.SmsSceneEnum;
import cn.zszj.module.system.framework.sms.config.SmsCodeProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static cn.hutool.core.util.RandomUtil.randomInt;
import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.framework.common.util.date.DateUtils.isToday;
import static cn.zszj.module.system.enums.ErrorCodeConstants.*;

/**
 * 短信验证码 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class SmsCodeServiceImpl implements SmsCodeService {

    /**
     * ZS-LOGIN-004 P1：X-Forwarded-For 头名（仅在直连 peer 命中可信代理白名单时才被采信）。
     */
    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";

    @Resource
    private SmsCodeProperties smsCodeProperties;

    @Resource
    private SmsCodeMapper smsCodeMapper;

    @Resource
    private SmsSendService smsSendService;

    @Resource
    private SmsCodeSecurityRedisDAO smsCodeSecurityRedisDAO;

    @Override
    public void sendSmsCode(SmsCodeSendReqDTO reqDTO) {
        SmsSceneEnum sceneEnum = SmsSceneEnum.getCodeByScene(reqDTO.getScene());
        Assert.notNull(sceneEnum, "验证码场景({}) 查找不到配置", reqDTO.getScene());
        // ZS-LOGIN-004：通道就绪性前置门控——不就绪则明确失败，不写库、不占配额、不假报已发送
        if (!smsSendService.isTemplateSendable(sceneEnum.getTemplateCode())) {
            throw exception(SMS_CODE_SEND_CHANNEL_NOT_READY);
        }
        // ZS-LOGIN-004 P1：配额 IP 采用「可信代理感知」解析——默认不信任 X-Forwarded-For、直连回退 peer address，
        // 使「每 IP 发送配额」默认不可被伪造 XFF 绕过；createIp 仍原样保留用于落库审计（不改框架级 ServletUtils.getClientIP）。
        String createIp = reqDTO.getCreateIp();
        String quotaIp = resolveQuotaIp(reqDTO);
        // ZS-LOGIN-004 P2-D：在持久化与派发「之前」原子地检查并预留小时桶 + 天桶两个配额，
        // 取代原「先读两桶判上限、建码发短信后再各自 INCR 且不检查结果」的非原子流程（并发下同 IP 对不同 mobile 会超发）。
        // r1(桶边界)：预留返回 IpQuotaReservation，携带本次预留的确切桶键；失败回滚时释放的是这些确切键而非「当前桶」。
        IpQuotaReservation quotaReservation = null;
        if (StrUtil.isNotBlank(quotaIp)) {
            quotaReservation = smsCodeSecurityRedisDAO.reserveIpSendQuota(quotaIp,
                    nullToZero(smsCodeProperties.getSendMaximumQuantityPerIpPerHour()),
                    nullToZero(smsCodeProperties.getSendMaximumQuantityPerIpPerDay()));
            if (quotaReservation != null && quotaReservation.isRejected()) {
                throw exception(SMS_CODE_EXCEED_SEND_MAXIMUM_QUANTITY_PER_IP);
            }
        }
        try {
            // 创建验证码
            String code = createSmsCode(reqDTO.getMobile(), reqDTO.getScene(), createIp);
            // 发送验证码
            smsSendService.sendSingleSms(reqDTO.getMobile(), null, null,
                    sceneEnum.getTemplateCode(), MapUtil.of("code", code));
        } catch (RuntimeException ex) {
            // ZS-LOGIN-004 P2-D：建码或派发失败 → 释放已预留的配额，被拒/失败的请求不占用配额。
            // r1：释放凭预留句柄携带的确切桶键，避免跨桶边界误减新桶 / 造出无 TTL 负计数。
            smsCodeSecurityRedisDAO.releaseIpSendQuota(quotaReservation);
            throw ex;
        }
    }

    /**
     * ZS-LOGIN-004 P1：解析用于「每 IP 发送配额」的客户端地址。
     * <p>安全默认（secure by default）：<b>不信任 X-Forwarded-For</b>，直接使用不可被调用者伪造的
     * peer address（{@code request.getRemoteAddr()}）；仅当 peer 命中
     * {@link SmsCodeProperties#getTrustedProxies() 可信代理白名单} 时，才采信 XFF 派生的客户端地址。
     * <p>无 HTTP 请求上下文时（内部任务 / 非 Web 入口 / 单测直调）退回调用方提供的 {@code createIp}。
     * <p>本方法是 SMS 配额范围内的<b>局部</b>加固，<b>不</b>修改框架级共享的 {@code ServletUtils.getClientIP()}；
     * 框架级 clientIP 的全局可信代理加固归口 ZS-SEC-011.B。
     */
    private String resolveQuotaIp(SmsCodeSendReqDTO reqDTO) {
        HttpServletRequest request = ServletUtils.getRequest();
        if (request == null) {
            return reqDTO.getCreateIp();
        }
        String peer = request.getRemoteAddr();
        List<String> trustedProxies = smsCodeProperties.getTrustedProxies();
        if (StrUtil.isNotBlank(peer) && CollUtil.isNotEmpty(trustedProxies) && trustedProxies.contains(peer)) {
            // 直连来自可信代理：采信 XFF 最左侧的原始客户端地址
            String clientFromXff = firstClientIpFromForwardedFor(request.getHeader(HEADER_X_FORWARDED_FOR));
            return StrUtil.isNotBlank(clientFromXff) ? clientFromXff : peer;
        }
        // 默认：peer 非可信代理（或白名单为空）→ 用 peer address，完全忽略 XFF
        return StrUtil.isNotBlank(peer) ? peer : reqDTO.getCreateIp();
    }

    /**
     * 从 {@code X-Forwarded-For}（形如 {@code "client, proxy1, proxy2"}）取最左侧的原始客户端地址。
     */
    private static String firstClientIpFromForwardedFor(String xff) {
        if (StrUtil.isBlank(xff)) {
            return null;
        }
        for (String part : xff.split(",")) {
            String ip = part.trim();
            if (StrUtil.isNotBlank(ip)) {
                return ip;
            }
        }
        return null;
    }

    private static int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String createSmsCode(String mobile, Integer scene, String ip) {
        // 校验是否可以发送验证码，不用筛选场景
        SmsCodeDO lastSmsCode = smsCodeMapper.selectLastByMobile(mobile, null, null);
        if (lastSmsCode != null) {
            if (LocalDateTimeUtil.between(lastSmsCode.getCreateTime(), LocalDateTime.now()).toMillis()
                    < smsCodeProperties.getSendFrequency().toMillis()) { // 发送过于频繁
                throw exception(SMS_CODE_SEND_TOO_FAST);
            }
            if (isToday(lastSmsCode.getCreateTime()) && // 必须是今天，才能计算超过当天的上限
                    lastSmsCode.getTodayIndex() >= smsCodeProperties.getSendMaximumQuantityPerDay()) { // 超过当天发送的上限。
                throw exception(SMS_CODE_EXCEED_SEND_MAXIMUM_QUANTITY_PER_DAY);
            }
            // ZS-LOGIN-004：每 IP 频控已上提到 sendSmsCode 入口（在写库前原子预留），此处不再重复
        }

        // 创建验证码记录
        String code = String.format("%0" + smsCodeProperties.getEndCode().toString().length() + "d",
                randomInt(smsCodeProperties.getBeginCode(), smsCodeProperties.getEndCode() + 1));
        SmsCodeDO newSmsCode = SmsCodeDO.builder().mobile(mobile).code(code).scene(scene)
                .todayIndex(lastSmsCode != null && isToday(lastSmsCode.getCreateTime()) ? lastSmsCode.getTodayIndex() + 1 : 1)
                .createIp(ip).used(false).build();
        smsCodeMapper.insert(newSmsCode);
        return code;
    }

    @Override
    public void useSmsCode(SmsCodeUseReqDTO reqDTO) {
        // 检测验证码是否有效（含尝试次数上限门控）
        SmsCodeDO lastSmsCode = validateSmsCode0(reqDTO.getMobile(), reqDTO.getCode(), reqDTO.getScene());
        // ZS-LOGIN-004：以「条件 UPDATE」原子消费，把 used=false 放进 WHERE，
        // 由存储层保证 N 个并发消费者中恰有一个拿到影响行数 1，其余拿到 0 → 拒绝
        int consumed = smsCodeMapper.consumeById(lastSmsCode.getId(), LocalDateTime.now(), reqDTO.getUsedIp());
        if (consumed == 0) {
            throw exception(SMS_CODE_USED);
        }
        // ZS-LOGIN-004：成功消费后清零失败尝试计数，合法用户不会因历史误输被越锁越死。
        // r1：reset 删除整个计数 Hash（含全部预留 token），在途请求的 stale 释放因 token 消失而被跳过。
        smsCodeSecurityRedisDAO.resetValidateAttempts(reqDTO.getMobile(), reqDTO.getScene());
    }

    @Override
    public void validateSmsCode(SmsCodeValidateReqDTO reqDTO) {
        validateSmsCode0(reqDTO.getMobile(), reqDTO.getCode(), reqDTO.getScene());
    }

    private SmsCodeDO validateSmsCode0(String mobile, String code, Integer scene) {
        // ZS-LOGIN-004 P2-C：原子「判上限 + 预留一次校验容量」，取代原「读计数-判上限」与「失败后自增」的非原子
        // check-then-act——并发下多个猜测都会在任何失败自增前读到低于上限的计数而全部抵达 DB。
        Integer maxAttempts = smsCodeProperties.getMaxValidateAttempts();
        Duration lockDuration = smsCodeProperties.getAttemptLockDuration();
        ValidateAttemptReservation reservation = smsCodeSecurityRedisDAO.reserveValidateAttempt(mobile, scene,
                maxAttempts == null ? 0 : maxAttempts, lockDuration);
        if (reservation != null && reservation.isRejected()) {
            // 已达上限：锁定期内直接拒绝（连正确的验证码也一并拒绝，否则锁定形同虚设），且不改动计数
            long lockSeconds = lockDuration == null ? 0L : lockDuration.getSeconds();
            throw exception(SMS_CODE_EXCEED_ATTEMPT_LIMIT, lockSeconds);
        }
        // reservation == null（Mock/未预留）或 isReservedSlot()==false（未启用限制）时，后续释放将被 DAO 静默跳过。
        // 校验验证码
        SmsCodeDO lastSmsCode;
        try {
            lastSmsCode = smsCodeMapper.selectLastByMobile(mobile, code, scene);
        } catch (RuntimeException ex) {
            // ZS-LOGIN-004 r1(P2/DB 异常)：查询抛异常（DB 连接/超时）并非「确认的码不匹配」——此刻无法判定用户是否输错，
            // 必须释放刚预留的名额，否则用户用正确码重试若干次即耗尽上限、DB 恢复后仍被锁死。释放后原样抛出异常。
            smsCodeSecurityRedisDAO.releaseValidateAttempt(reservation);
            throw ex;
        }
        // 若验证码不存在，抛出异常
        if (lastSmsCode == null) {
            // ZS-LOGIN-004：「码不匹配 / 错场景」属于暴力尝试信号——已预留的名额即为本次失败计数，保留不释放
            throw exception(SMS_CODE_NOT_FOUND);
        }
        // 超过时间
        if (LocalDateTimeUtil.between(lastSmsCode.getCreateTime(), LocalDateTime.now()).toMillis()
                >= smsCodeProperties.getExpireTimes().toMillis()) { // 验证码已过期
            // ZS-LOGIN-004：过期不是攻击信号（合法用户手慢而已），释放预留名额、不计入失败尝试
            smsCodeSecurityRedisDAO.releaseValidateAttempt(reservation);
            throw exception(SMS_CODE_EXPIRED);
        }
        // 判断验证码是否已被使用
        if (Boolean.TRUE.equals(lastSmsCode.getUsed())) {
            // ZS-LOGIN-004：已使用不是攻击信号（并发消费 / 重复提交），释放预留名额、不计入失败尝试
            smsCodeSecurityRedisDAO.releaseValidateAttempt(reservation);
            throw exception(SMS_CODE_USED);
        }
        // 校验通过：成功不是失败信号，释放预留名额（合法用户不会因正常校验被计数）
        smsCodeSecurityRedisDAO.releaseValidateAttempt(reservation);
        return lastSmsCode;
    }

}
