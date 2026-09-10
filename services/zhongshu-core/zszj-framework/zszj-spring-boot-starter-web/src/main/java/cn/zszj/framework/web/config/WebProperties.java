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

}
