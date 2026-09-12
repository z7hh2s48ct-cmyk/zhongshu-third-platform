package cn.zszj.module.system.framework.sms.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;

@ConfigurationProperties(prefix = "zszj.sms-code")
@Validated
@Data
public class SmsCodeProperties {

    /**
     * 过期时间
     */
    @NotNull(message = "过期时间不能为空")
    private Duration expireTimes;
    /**
     * 短信发送频率
     */
    @NotNull(message = "短信发送频率不能为空")
    private Duration sendFrequency;
    /**
     * 每日发送最大数量
     */
    @NotNull(message = "每日发送最大数量不能为空")
    private Integer sendMaximumQuantityPerDay;
    /**
     * 验证码最小值
     */
    @NotNull(message = "验证码最小值不能为空")
    private Integer beginCode;
    /**
     * 验证码最大值
     */
    @NotNull(message = "验证码最大值不能为空")
    private Integer endCode;

    // ========== ZS-LOGIN-004：安全默认值 ==========
    // 既是 yaml 漏配时的兜底，也是单测中 {@code @MockitoBean SmsCodeProperties} 返回 null 时的兜底（单一来源，避免各处魔数）。

    /**
     * 验证码错误尝试次数上限的默认值
     */
    public static final int DEFAULT_MAX_VALIDATE_ATTEMPTS = 5;
    /**
     * 尝试次数超限后的锁定时长默认值
     */
    public static final Duration DEFAULT_ATTEMPT_LOCK_DURATION = Duration.ofMinutes(10);
    /**
     * 每 IP 每小时发送数量上限的默认值
     */
    public static final int DEFAULT_SEND_MAXIMUM_QUANTITY_PER_IP_PER_HOUR = 20;
    /**
     * 每 IP 每天发送数量上限的默认值
     */
    public static final int DEFAULT_SEND_MAXIMUM_QUANTITY_PER_IP_PER_DAY = 50;

    /**
     * 验证码错误尝试次数上限，超过则在锁定期内直接拒绝（防暴力破解）
     */
    private Integer maxValidateAttempts = DEFAULT_MAX_VALIDATE_ATTEMPTS;
    /**
     * 尝试次数超限后的锁定时长
     */
    private Duration attemptLockDuration = DEFAULT_ATTEMPT_LOCK_DURATION;
    /**
     * 每个 IP 每小时可发送的短信数量上限
     */
    private Integer sendMaximumQuantityPerIpPerHour = DEFAULT_SEND_MAXIMUM_QUANTITY_PER_IP_PER_HOUR;
    /**
     * 每个 IP 每天可发送的短信数量上限
     */
    private Integer sendMaximumQuantityPerIpPerDay = DEFAULT_SEND_MAXIMUM_QUANTITY_PER_IP_PER_DAY;

}
