package cn.zszj.module.infra.framework.security.config;

import cn.zszj.framework.security.config.AuthorizeRequestsCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/**
 * Infra 模块的 Security 配置
 */
@Configuration(proxyBeanMethods = false, value = "infraSecurityConfiguration")
public class SecurityConfiguration {

    @Bean("infraAuthorizeRequestsCustomizer")
    public AuthorizeRequestsCustomizer authorizeRequestsCustomizer() {
        return new AuthorizeRequestsCustomizer() {

            @Override
            public void customize(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
                // Swagger 接口文档：springdoc 默认关闭（ZS-ENG-005），仅 local 显式开启；
                // 路径放行保留，未开启时不存在对应端点，开启时供本地匿名预览
                registry.requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/webjars/**").permitAll()
                        .requestMatchers("/swagger-ui.html").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll();
                // Spring Boot Actuator：仅健康检查可匿名访问（探针边界），
                // 其余端点（env/beans/configtree 等）必须认证，且默认不在 exposure 白名单
                registry.requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator/health/**").permitAll();
                // Druid 监控：不再匿名放行（ZS-ENG-005）；stat-view-servlet 默认关闭，
                // 如显式开启，必须经认证访问并配置访问账号
                // 文件读取：公开对象的访问路径，对象级授权归 ZS-FILE-001.A（M08）
                registry.requestMatchers(buildAdminApi("/infra/file/*/get/**")).permitAll();
            }

        };
    }

}
