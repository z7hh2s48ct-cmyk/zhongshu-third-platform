package cn.zszj.module.fms.framework.web.config;

import cn.zszj.framework.swagger.config.ZszjSwaggerAutoConfiguration;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * fms 模块的 web 组件的 Configuration
 *
 * @author 芋道源码
 */
@Configuration(proxyBeanMethods = false)
public class FmsWebConfiguration {

    /**
     * fms 模块的 API 分组
     *
     * @return fms 模块的 OpenAPI 分组
     */
    @Bean
    public GroupedOpenApi fmsGroupedOpenApi() {
        return ZszjSwaggerAutoConfiguration.buildGroupedOpenApi("fms");
    }

}
