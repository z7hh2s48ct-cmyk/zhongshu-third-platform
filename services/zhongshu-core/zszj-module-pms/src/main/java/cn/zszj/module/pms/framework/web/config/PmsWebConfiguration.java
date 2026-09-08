package cn.zszj.module.pms.framework.web.config;

import cn.zszj.framework.swagger.config.ZszjSwaggerAutoConfiguration;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * PMS 模块的 Web 配置
 *
 * @author 芋道源码
 */
@Configuration(proxyBeanMethods = false)
public class PmsWebConfiguration {

    /**
     * 创建 PMS 模块的 OpenAPI 分组
     *
     * @return PMS OpenAPI 分组
     */
    @Bean
    public GroupedOpenApi pmsGroupedOpenApi() {
        return ZszjSwaggerAutoConfiguration.buildGroupedOpenApi("pms");
    }

}
