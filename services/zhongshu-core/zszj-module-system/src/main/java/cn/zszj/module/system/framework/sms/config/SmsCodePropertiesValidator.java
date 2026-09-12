package cn.zszj.module.system.framework.sms.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

/**
 * ZS-LOGIN-004：短信验证码配置的<b>启动期</b>安全校验。
 * <p>
 * 背景：历史配置在所有 profile 共享的 {@code application.yaml} 中把 {@code begin-code} 与 {@code end-code}
 * 都写死为 9999（「测试方便」），导致<b>生产模板同样使用固定演示验证码</b>——任何知道该约定的人都能直接登录/重置密码。
 * 仅在 yaml 里改默认值不足以防止回退，故补一道启动期断言：
 * <ul>
 *     <li>宽松 profile（本地/开发/测试）：允许固定演示码，仅打印告警；</li>
 *     <li>其余 profile（含生产、以及<b>未显式指定 active profile</b> 的情况）：验证码取值域必须足够大，否则启动失败。</li>
 * </ul>
 * 「未指定 profile 即按严格处理」是有意的 fail-safe：不能因为漏配环境变量就静默退化成演示态。
 *
 * @author ZS-LOGIN-004
 */
@Component
@Slf4j
public class SmsCodePropertiesValidator implements InitializingBean {

    /**
     * 允许使用固定演示验证码的 profile 白名单（非生产环境）
     */
    static final Set<String> PERMISSIVE_PROFILES = Set.of("local", "dev", "test", "unit-test");

    /**
     * 安全的最小验证码取值域大小。取值域小于该值即视为「可被暴力枚举」，例如 begin == end 时取值域为 1。
     */
    static final int MIN_SECURE_CODE_RANGE = 1000;

    private final SmsCodeProperties smsCodeProperties;

    private final Environment environment;

    public SmsCodePropertiesValidator(SmsCodeProperties smsCodeProperties, Environment environment) {
        this.smsCodeProperties = smsCodeProperties;
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        validate(smsCodeProperties.getBeginCode(), smsCodeProperties.getEndCode(), resolveProfiles());
    }

    /**
     * 解析生效的 profile：无 active profile 时回落 default profile（Spring 默认即 "default"，不在白名单内 → 严格）。
     */
    String[] resolveProfiles() {
        String[] activeProfiles = environment.getActiveProfiles();
        return activeProfiles.length > 0 ? activeProfiles : environment.getDefaultProfiles();
    }

    /**
     * 校验验证码取值域。非宽松 profile 下不安全即抛 {@link IllegalStateException} 使启动失败。
     * <p>声明为 {@code static} 是为了让策略可被纯单测直接驱动，无需拉起 Spring 上下文。
     */
    static void validate(Integer beginCode, Integer endCode, String[] profiles) {
        if (!isInsecureCodeRange(beginCode, endCode)) {
            return;
        }
        if (isPermissiveProfile(profiles)) {
            log.warn("[validate][验证码取值域不安全(begin-code={}, end-code={})，当前为宽松 profile={}，仅告警不阻断]",
                    beginCode, endCode, Arrays.toString(profiles));
            return;
        }
        throw new IllegalStateException(String.format(
                "验证码取值域不安全：begin-code=%s, end-code=%s。非宽松 profile(%s) 下 begin-code 与 end-code 不得相等"
                        + "且取值域不得小于 %d，否则等同于固定演示验证码，任何人皆可登录/重置密码。"
                        + "请检查 application.yaml 的 zszj.sms-code.begin-code / end-code 配置。",
                beginCode, endCode, Arrays.toString(profiles), MIN_SECURE_CODE_RANGE));
    }

    /**
     * 判断验证码取值域是否不安全（可被暴力枚举）。
     */
    public static boolean isInsecureCodeRange(Integer beginCode, Integer endCode) {
        if (beginCode == null || endCode == null) {
            return false;
        }
        if (beginCode > endCode) {
            return true;
        }
        long range = (long) endCode - beginCode + 1;
        return range < MIN_SECURE_CODE_RANGE;
    }

    /**
     * 判断给定 profile 集合是否属于「允许固定演示码」的宽松环境。
     */
    public static boolean isPermissiveProfile(String[] profiles) {
        if (profiles == null || profiles.length == 0) {
            return false;
        }
        for (String profile : profiles) {
            if (!PERMISSIVE_PROFILES.contains(profile)) {
                return false;
            }
        }
        return true;
    }

}
