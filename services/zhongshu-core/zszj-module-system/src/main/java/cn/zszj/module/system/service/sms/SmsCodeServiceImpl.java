package cn.zszj.module.system.service.sms;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.zszj.module.system.api.sms.dto.code.SmsCodeSendReqDTO;
import cn.zszj.module.system.api.sms.dto.code.SmsCodeUseReqDTO;
import cn.zszj.module.system.api.sms.dto.code.SmsCodeValidateReqDTO;
import cn.zszj.module.system.dal.dataobject.sms.SmsCodeDO;
import cn.zszj.module.system.dal.mysql.sms.SmsCodeMapper;
import cn.zszj.module.system.dal.redis.sms.SmsCodeSecurityRedisDAO;
import cn.zszj.module.system.enums.sms.SmsSceneEnum;
import cn.zszj.module.system.framework.sms.config.SmsCodeProperties;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;

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
        // ZS-LOGIN-004：每 IP 发送频控（封堵「同一 IP 轮换手机号」的短信喷洒）
        String createIp = reqDTO.getCreateIp();
        if (StrUtil.isNotBlank(createIp)) {
            validateIpSendLimit(createIp);
        }
        // 创建验证码
        String code = createSmsCode(reqDTO.getMobile(), reqDTO.getScene(), createIp);
        // ZS-LOGIN-004：创建成功后才累加 IP 计数，被拒的请求不占用配额
        if (StrUtil.isNotBlank(createIp)) {
            smsCodeSecurityRedisDAO.increaseIpSendCountPerHour(createIp);
            smsCodeSecurityRedisDAO.increaseIpSendCountPerDay(createIp);
        }
        // 发送验证码
        smsSendService.sendSingleSms(reqDTO.getMobile(), null, null,
                sceneEnum.getTemplateCode(), MapUtil.of("code", code));
    }

    /**
     * ZS-LOGIN-004：校验每 IP 发送频控（小时桶 + 天桶）。
     */
    private void validateIpSendLimit(String ip) {
        Integer hourLimit = smsCodeProperties.getSendMaximumQuantityPerIpPerHour();
        if (hourLimit != null && hourLimit > 0
                && smsCodeSecurityRedisDAO.getIpSendCountPerHour(ip) >= hourLimit) {
            throw exception(SMS_CODE_EXCEED_SEND_MAXIMUM_QUANTITY_PER_IP);
        }
        Integer dayLimit = smsCodeProperties.getSendMaximumQuantityPerIpPerDay();
        if (dayLimit != null && dayLimit > 0
                && smsCodeSecurityRedisDAO.getIpSendCountPerDay(ip) >= dayLimit) {
            throw exception(SMS_CODE_EXCEED_SEND_MAXIMUM_QUANTITY_PER_IP);
        }
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
            // ZS-LOGIN-004：每 IP 频控已上提到 sendSmsCode 入口（需在写库前拦截），此处不再重复
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
        // ZS-LOGIN-004：成功消费后清零失败尝试计数，合法用户不会因历史误输被越锁越死
        smsCodeSecurityRedisDAO.resetValidateAttempts(reqDTO.getMobile(), reqDTO.getScene());
    }

    @Override
    public void validateSmsCode(SmsCodeValidateReqDTO reqDTO) {
        validateSmsCode0(reqDTO.getMobile(), reqDTO.getCode(), reqDTO.getScene());
    }

    private SmsCodeDO validateSmsCode0(String mobile, String code, Integer scene) {
        // ZS-LOGIN-004：尝试次数上限门控——超限后在锁定期内直接拒绝，连 DB 都不再查询，杜绝枚举探测
        validateAttemptLimit(mobile, scene);
        // 校验验证码
        SmsCodeDO lastSmsCode = smsCodeMapper.selectLastByMobile(mobile, code, scene);
        // 若验证码不存在，抛出异常
        if (lastSmsCode == null) {
            // ZS-LOGIN-004：「码不匹配 / 错场景」属于暴力尝试信号，按 手机号+场景 计数；
            // 过期、已使用不是攻击信号（合法用户手慢 / 重复提交），不计入，避免把合法用户锁死
            smsCodeSecurityRedisDAO.increaseValidateAttempts(mobile, scene,
                    smsCodeProperties.getAttemptLockDuration());
            throw exception(SMS_CODE_NOT_FOUND);
        }
        // 超过时间
        if (LocalDateTimeUtil.between(lastSmsCode.getCreateTime(), LocalDateTime.now()).toMillis()
                >= smsCodeProperties.getExpireTimes().toMillis()) { // 验证码已过期
            throw exception(SMS_CODE_EXPIRED);
        }
        // 判断验证码是否已被使用
        if (Boolean.TRUE.equals(lastSmsCode.getUsed())) {
            throw exception(SMS_CODE_USED);
        }
        return lastSmsCode;
    }

    /**
     * ZS-LOGIN-004：校验「手机号 + 场景」维度的失败尝试次数是否已达上限。
     * <p>达到上限即在锁定期内直接拒绝（连正确的验证码也一并拒绝，否则锁定形同虚设）。
     */
    private void validateAttemptLimit(String mobile, Integer scene) {
        Integer maxAttempts = smsCodeProperties.getMaxValidateAttempts();
        if (maxAttempts == null || maxAttempts <= 0) {
            return;
        }
        long attempts = smsCodeSecurityRedisDAO.getValidateAttempts(mobile, scene);
        if (attempts >= maxAttempts) {
            Duration lockDuration = smsCodeProperties.getAttemptLockDuration();
            long lockSeconds = lockDuration == null ? 0L : lockDuration.getSeconds();
            throw exception(SMS_CODE_EXCEED_ATTEMPT_LIMIT, lockSeconds);
        }
    }

}
