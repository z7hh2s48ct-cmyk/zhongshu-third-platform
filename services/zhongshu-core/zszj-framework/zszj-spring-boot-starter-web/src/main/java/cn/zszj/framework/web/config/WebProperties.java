package cn.zszj.framework.web.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "zszj.web")
@Validated
@Data
public class WebProperties {

    @NotNull(message = "APP API 不能为空")
    @Valid
    private Api appApi = new Api("/app-api", "**.controller.app.**");
    @NotNull(message = "Admin API 不能为空")
    @Valid
    private Api adminApi = new Api("/admin-api", "**.controller.admin.**");

    @NotNull(message = "Admin UI 不能为空")
    @Valid
    private Ui adminUi;

    @NotNull(message = "请求体限制不能为空")
    @Valid
    private RequestBody requestBody = new RequestBody();

    @NotNull(message = "CORS 配置不能为空")
    @Valid
    private Cors cors = new Cors();

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Api {

        /**
         * API 前缀，实现所有 Controller 提供的 RESTFul API 的统一前缀
         *
         *
         * 意义：通过该前缀，避免 Swagger、Actuator 意外通过 Nginx 暴露出来给外部，带来安全性问题
         *      这样，Nginx 只需要配置转发到 /api/* 的所有接口即可。
         *
         * @see ZszjWebAutoConfiguration#configurePathMatch(PathMatchConfigurer)
         */
        @NotEmpty(message = "API 前缀不能为空")
        private String prefix;

        /**
         * Controller 所在包的 Ant 路径规则
         *
         * 主要目的是，给该 Controller 设置指定的 {@link #prefix}
         */
        @NotEmpty(message = "Controller 所在包不能为空")
        private String controller;

    }

    @Data
    public static class Ui {

        /**
         * 访问地址
         */
        private String url;

    }

    @Data
    public static class RequestBody {

        /**
         * JSON 请求体缓冲上限（字节）。
         *
         * ZS-SEC-008：认证前 CacheRequestBodyFilter 会全量缓冲 JSON 请求体以支持重复读取。超限则在缓冲前受控拒绝
         * （业务码 400），防止超大 body 耗尽内存。默认 1MB；配置为 {@code <= 0} 表示不限制。
         * 上传 / 流式接口非 JSON，已被 CacheRequestBodyFilter#shouldNotFilter 排除，不受此限影响。
         */
        private long maxCacheSize = 1024 * 1024L;

    }

    /**
     * 浏览器跨域（CORS）与点击劫持防护配置。
     *
     * ZS-SEC-004：由原先的全通配（{@code allowedOriginPattern=*} + {@code allowCredentials=true}）收紧为精确白名单，
     * 消除“任意源携带凭据”的安全缺陷；同时将 frameOptions 由机械全关改为可配置（默认 SAMEORIGIN 防点击劫持）。
     */
    @Data
    public static class Cors {

        /**
         * 允许的源模式白名单（精确，禁止裸 * 与 credentials 并存）。默认仅本地开发源。
         */
        private List<String> allowedOriginPatterns = new ArrayList<>(List.of(
                "http://localhost:*", "http://127.0.0.1:*",
                // ZS-SEC-004 P1-1：Spring 的 ":*" 模式不匹配无端口源；admin-web 本地 VITE_PORT=80 时浏览器发出
                // Origin: http://localhost（省略默认端口），须显式放行无端口 loopback 源，否则本地前端跨域被拒。
                "http://localhost", "http://127.0.0.1"));

        /**
         * 允许的方法。
         */
        private List<String> allowedMethods = new ArrayList<>(List.of(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));

        /**
         * 允许的请求头（含租户头 tenant-id）。
         */
        private List<String> allowedHeaders = new ArrayList<>(List.of(
                "Authorization", "Content-Type", "X-Requested-With", "tenant-id",
                // ZS-SEC-004 P1-2：admin-web service.ts 对每个 GET 注入 Cache-Control/Pragma（防缓存）、跨租户注入
                // visit-tenant-id、API 加密注入 X-Api-Encrypt；须列入白名单，否则预检 Access-Control-Request-Headers
                // 校验失败，拦截所有 admin GET（含租户查询/权限加载）。
                "Cache-Control", "Pragma", "visit-tenant-id", "X-Api-Encrypt"));

        /**
         * 暴露给浏览器的响应头（ZS-SEC-006 trace-id 关联）。
         */
        private List<String> exposedHeaders = new ArrayList<>(List.of("trace-id"));

        /**
         * 是否携带凭据。精确源白名单下可为 true。
         */
        private boolean allowCredentials = true;

        /**
         * 预检缓存秒数。
         */
        private long maxAge = 3600L;

        /**
         * frame-options：默认 SAMEORIGIN 防点击劫持；需嵌入时显式改 DISABLE。
         */
        private String frameOptions = "SAMEORIGIN";

    }

}
