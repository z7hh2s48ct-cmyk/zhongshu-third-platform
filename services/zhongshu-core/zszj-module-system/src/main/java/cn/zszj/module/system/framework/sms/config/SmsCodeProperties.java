package cn.zszj.module.system.framework.sms.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

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

    /**
     * ZS-LOGIN-004 P1（codex r0）：可信反向代理的 peer address 白名单，<b>默认为空</b>（secure by default：不信任任何 X-Forwarded-For）。
     * <p>用途：短信「每 IP 发送配额」的计数键必须来自<b>不可被调用者伪造</b>的地址。框架级
     * {@code ServletUtils.getClientIP()}（Hutool 实现）会<b>先采信 X-Forwarded-For 再看 socket 地址</b>，
     * 于是攻击者只需改 XFF 头就能让每个伪造地址都拿到全新的小时/天配额桶，绕过频控。
     * <p>本配置只在 SMS 配额范围内做<b>局部</b>加固（不改框架级共享工具，框架级全局加固归口 ZS-SEC-011.B）：
     * <ul>
     *     <li>直连 peer address（{@code request.getRemoteAddr()}）<b>命中</b>本白名单时，才认为请求确实经过可信代理，
     *         进而采信 XFF 派生的客户端地址用于配额；</li>
     *     <li>否则（白名单为空 / peer 不在白名单）一律使用 peer address，XFF 被完全忽略，
     *         伪造 XFF 不再能获得新配额桶。</li>
     * </ul>
     * <p>生产部署若确有可信反代（如 nginx/SLB），应把其内网出口地址列入本白名单；未列时默认行为即为最安全行为。
     */
    private List<String> trustedProxies = Collections.emptyList();

}
