package cn.zszj.framework.security.fixture;

import cn.zszj.framework.common.biz.infra.logger.ApiErrorLogCommonApi;
import cn.zszj.framework.common.biz.system.oauth2.OAuth2TokenCommonApi;
import cn.zszj.framework.common.biz.system.permission.PermissionCommonApi;
import cn.zszj.framework.security.config.AuthorizeRequestsCustomizer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * ZS-SEC-012.A：安全夹具测试应用。
 *
 * 设计目标：以「真实」的 Security 过滤器链 + MVC 拦截器 + 方法权限运行集成测试，
 * 仅将外部依赖（OAuth2 Token、权限、租户合法性）替换为确定性 Mock，
 * 使测试聚焦过滤器链行为而非业务存储。安全过滤器不被禁用（符合 05 文档 line 389 验收红线）。
 *
 * 归属说明：本夹具（含此类）置于 biz-tenant 模块的 src/test，而非 security 模块。原因：
 * 双技术租户夹具需要 biz-tenant 的 TenantSecurityWebFilter/TenantContextWebFilter/TenantVisitContextInterceptor，
 * 若在 security 模块以 test 依赖引入 biz-tenant，会与 biz-tenant→security(compile) 构成 Maven reactor 循环。
 * biz-tenant 已 compile 依赖 security、test 依赖 starter-test（含 MockMvc），故夹具放此处零新增依赖边、无循环。
 *
 * 组件来源：
 * - Security 链（TokenAuthenticationFilter + 方法权限 + 异常出口）：由 ZszjSecurityAutoConfiguration、
 *   ZszjWebSecurityConfigurerAdapter 自动配置提供（真实组件，未禁用）。
 * - Web/Jackson（WebProperties、GlobalExceptionHandler、CommonResult 序列化）：由 web starter 自动配置提供。
 * - 租户 Web 组件（TenantContextWebFilter、TenantSecurityWebFilter、TenantVisitContextInterceptor）：
 *   由 {@link TenantFixtureConfiguration} 手动装配（真实的 ZszjTenantAutoConfiguration 因拖入 DB/Redis 被排除）。
 *
 * 扫描范围收敛到 fixture 包，避免误扫其它 @Component 与自动配置产生 Bean 冲突。
 *
 * 排除的自动配置（夹具不触达持久化/缓存/调度）：
 * - DataSourceAutoConfiguration、RedisAutoConfiguration、QuartzAutoConfiguration
 * - 其余 zszj 框架自动配置（tenant/mybatis/redis/job/部分 web）在 application-fixture.yaml 中按类名排除
 */
@SpringBootApplication(
        scanBasePackages = {"cn.zszj.framework.security.fixture"},
        exclude = {
                org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
                org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
                org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration.class
        }
)
public class SecurityFixtureApplication {

    @Bean
    public OAuth2TokenCommonApi oauth2TokenCommonApi() {
        return new MockOAuth2TokenApi();
    }

    @Bean
    public PermissionCommonApi permissionCommonApi() {
        return new MockPermissionApi();
    }

    /**
     * GlobalExceptionHandler 依赖 ApiErrorLogCommonApi 记录异常日志（生产由 infra 模块提供实现）。
     * 夹具不落库，提供 no-op 实现即可（接口仅一个抽象方法，用 lambda）。
     */
    @Bean
    public ApiErrorLogCommonApi apiErrorLogCommonApi() {
        return createDTO -> {
            // no-op：夹具不持久化 API 错误日志
        };
    }

    /**
     * 忠实还原生产的 servletPath。
     *
     * MockMvc 默认不设置 servletPath（为空串），而生产中 DispatcherServlet 映射到 "/"，servletPath 即完整请求路径。
     * {@code WebFrameworkUtils.getLoginUserType()} 依据 servletPath 前缀（/admin-api → ADMIN，/app-api → MEMBER）
     * 推导用户类型；若 servletPath 为空则返回 null，会使 TokenAuthenticationFilter 的「用户类型不匹配」校验被跳过
     * （无法覆盖 MEMBER token 访问 /admin-api 的拒绝路径）。本定制器把每个请求的 servletPath 设为其 requestURI，
     * 使 userType 推导与生产一致。
     */
    @Bean
    public MockMvcBuilderCustomizer servletPathMockMvcBuilderCustomizer() {
        return builder -> builder.defaultRequest(get("/").with(request -> {
            request.setServletPath(request.getRequestURI());
            return request;
        }));
    }

    /**
     * 提供一个 no-op 的 {@link AuthorizeRequestsCustomizer}，确保 ZszjWebSecurityConfigurerAdapter
     * 注入的 {@code List<AuthorizeRequestsCustomizer>} 非空（夹具无需自定义放行规则，@PermitAll 由注解扫描处理）。
     */
    @Bean("fixtureAuthorizeRequestsCustomizer")
    public AuthorizeRequestsCustomizer authorizeRequestsCustomizer() {
        return new AuthorizeRequestsCustomizer() {
            @Override
            public void customize(
                    AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
                // no-op：夹具不追加自定义 URL 放行规则
            }
        };
    }
}
