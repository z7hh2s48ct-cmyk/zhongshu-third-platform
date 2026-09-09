package cn.zszj.framework.security.fixture;

import cn.zszj.framework.common.enums.WebFilterOrderEnum;
import cn.zszj.framework.security.core.service.SecurityFrameworkService;
import cn.zszj.framework.tenant.config.TenantProperties;
import cn.zszj.framework.tenant.core.security.TenantSecurityWebFilter;
import cn.zszj.framework.tenant.core.service.TenantFrameworkService;
import cn.zszj.framework.tenant.core.web.TenantContextWebFilter;
import cn.zszj.framework.tenant.core.web.TenantVisitContextInterceptor;
import cn.zszj.framework.web.config.WebProperties;
import cn.zszj.framework.web.core.handler.GlobalExceptionHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.HashSet;

/**
 * ZS-SEC-012.A：租户 Web 组件的手动装配。
 *
 * 背景：真实的 {@code ZszjTenantAutoConfiguration} 无条件创建 MyBatis 拦截器、Redis 缓存管理器、
 * TenantFrameworkServiceImpl(TenantCommonApi) 等重型 Bean，会拖入 DataSource/Redis/Quartz 依赖，
 * 使纯 Web 层的 MockMvc 安全夹具无法加载上下文。因此本夹具：
 * 1. 在 application-fixture.yaml 中按类名排除 ZszjTenantAutoConfiguration，以及 mybatis/redis/job starter
 *    的自动配置（这些 starter 是 biz-tenant 的 compile 依赖，无法用 pom 排除，只能按类名排除自动配置）；
 * 2. 在此仅手动装配「真实」的 3 个租户 Web 组件（不改动其源码），保留其真实行为：
 *    - {@link TenantContextWebFilter}：从 header 解析 tenant-id 写入 TenantContextHolder（order=-104）
 *    - {@link TenantSecurityWebFilter}：校验登录用户租户越权/租户合法性（order=-99，在 Spring Security 链之后）
 *    - {@link TenantVisitContextInterceptor}：跨租户切换（MVC 拦截器，需 system:tenant:visit 权限）
 *
 * 过滤器顺序严格对齐 {@link WebFilterOrderEnum}，与生产一致：
 * TenantContextWebFilter(-104) → Spring Security 链(-100) → TenantSecurityWebFilter(-99) → MVC 拦截器 → @PreAuthorize。
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TenantProperties.class)
public class TenantFixtureConfiguration {

    /**
     * 真实的 TenantFrameworkService 由 TenantCommonApi 驱动；夹具用 Mock 提供确定性的双技术租户校验。
     */
    @Bean
    public TenantFrameworkService tenantFrameworkService() {
        return new MockTenantFrameworkService();
    }

    /**
     * 租户上下文过滤器：将 header 的 tenant-id 写入 TenantContextHolder。order=-104（在安全链之前）。
     */
    @Bean
    public FilterRegistrationBean<TenantContextWebFilter> tenantContextWebFilter() {
        FilterRegistrationBean<TenantContextWebFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new TenantContextWebFilter());
        registrationBean.setOrder(WebFilterOrderEnum.TENANT_CONTEXT_FILTER);
        return registrationBean;
    }

    /**
     * 租户安全过滤器：校验越权与租户合法性。order=-99（在 Spring Security 链之后）。
     * 第 3 个参数为 @TenantIgnore 注解 URL 集合，夹具无此类端点，传空集合；
     * zszj.tenant.ignore-urls（yaml 配置）仍由 TenantProperties 绑定并生效。
     */
    @Bean
    public FilterRegistrationBean<TenantSecurityWebFilter> tenantSecurityWebFilter(
            TenantProperties tenantProperties,
            WebProperties webProperties,
            GlobalExceptionHandler globalExceptionHandler,
            TenantFrameworkService tenantFrameworkService) {
        FilterRegistrationBean<TenantSecurityWebFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new TenantSecurityWebFilter(webProperties, tenantProperties, new HashSet<>(),
                globalExceptionHandler, tenantFrameworkService));
        registrationBean.setOrder(WebFilterOrderEnum.TENANT_SECURITY_FILTER);
        return registrationBean;
    }

    /**
     * 跨租户切换拦截器：依赖真实的 SecurityFrameworkService（"ss" Bean，由 ZszjSecurityAutoConfiguration 提供）。
     */
    @Bean
    public TenantVisitContextInterceptor tenantVisitContextInterceptor(
            TenantProperties tenantProperties,
            SecurityFrameworkService securityFrameworkService) {
        return new TenantVisitContextInterceptor(tenantProperties, securityFrameworkService);
    }

    /**
     * 将跨租户拦截器注册到 MVC，排除路径取自 zszj.tenant.ignore-visit-urls（夹具为空）。
     */
    @Bean
    public WebMvcConfigurer tenantWebMvcConfigurer(TenantProperties tenantProperties,
                                                   TenantVisitContextInterceptor tenantVisitContextInterceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(tenantVisitContextInterceptor)
                        .excludePathPatterns(tenantProperties.getIgnoreVisitUrls().toArray(new String[0]));
            }
        };
    }
}
