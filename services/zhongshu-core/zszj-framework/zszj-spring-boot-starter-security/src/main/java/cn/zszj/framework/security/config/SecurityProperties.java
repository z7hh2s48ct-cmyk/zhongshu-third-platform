package cn.zszj.framework.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;

@ConfigurationProperties(prefix = "zszj.security")
@Validated
@Data
public class SecurityProperties {

    /**
     * HTTP 请求时，访问令牌的请求 Header
     */
    @NotEmpty(message = "Token Header 不能为空")
    private String tokenHeader = "Authorization";
    /**
     * HTTP 请求时，访问令牌的请求参数
     *
     * 初始目的：解决 WebSocket 无法通过 header 传参，只能通过 token 参数拼接
     */
    @NotEmpty(message = "Token Parameter 不能为空")
    private String tokenParameter = "token";

    /**
     * 是否允许通过 URL 请求参数（{@link #tokenParameter}）传递访问令牌（ZS-SEC-003）。
     *
     * 默认 {@code true}：保留 WebSocket 等无法设置 Header 的获准连接能力——浏览器 WebSocket 握手不能自定义
     * Header，只能通过 {@code /ws?token=} 拼接（见 WebSocketProperties#path、LoginUserHandshakeInterceptor）。
     * 普通 API 请求两端前端一律使用 Authorization Header（Web service.ts、小程序 interceptor.ts，不依赖本开关）。
     *
     * 共享/部署环境若未使用 WebSocket/SSE 参数连接，应设为 {@code false}，从服务端禁止普通长效凭据进入 URL，
     * 规避 URL 被访问日志、浏览器历史、Referer、代理留存导致的泄露。SSE、文件下载两端均走 Header，不受影响。
     */
    @NotNull(message = "Token 参数开关不能为空")
    private Boolean tokenParameterEnabled = true;

    /**
     * mock 模式的开关
     */
    @NotNull(message = "mock 模式的开关不能为空")
    private Boolean mockEnable = false;
    /**
     * mock 模式的密钥
     * 一定要配置密钥，保证安全性
     */
    @NotEmpty(message = "mock 模式的密钥不能为空") // 这里设置了一个默认值，因为实际上只有 mockEnable 为 true 时才需要配置。
    private String mockSecret = "test";

    /**
     * 免登录的 URL 列表
     */
    private List<String> permitAllUrls = Collections.emptyList();

    /**
     * PasswordEncoder 加密复杂度，越高开销越大
     */
    private Integer passwordEncoderLength = 4;
}
