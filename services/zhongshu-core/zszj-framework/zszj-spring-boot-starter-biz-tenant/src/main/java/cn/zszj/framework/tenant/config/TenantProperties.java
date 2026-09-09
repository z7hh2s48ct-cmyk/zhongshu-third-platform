package cn.zszj.framework.tenant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 多租户配置
 *
 * @author 芋道源码
 */
@ConfigurationProperties(prefix = "zszj.tenant")
@Data
public class TenantProperties {

    /**
     * 租户是否开启
     */
    private static final Boolean ENABLE_DEFAULT = true;

    /**
     * 是否开启
     */
    private Boolean enable = ENABLE_DEFAULT;

    /**
     * 跨租户访问（visit-tenant-id）能力总开关，默认关闭。
     *
     * ZS-SEC-001.A：底座默认关闭未经批准的跨租户浏览能力。关闭时，任何携带与当前租户不一致的
     * visit-tenant-id 的请求（即使持有旧的 system:tenant:visit 权限）都会被拒绝，不会设置
     * LoginUser.visitTenantId、不会切换 TenantContextHolder，从而 SecurityFrameworkUtils#skipPermissionCheck()
     * 恒为 false，功能权限与数据范围均按登录租户正常校验，杜绝越权放大。
     *
     * 获批的受控跨组织访问（限定目标租户/对象/动作/字段/有效期）由 ZS-SEC-001.B 实现，依赖 D-09 组织模型决策；
     * 开启本开关仅恢复旧的切换链路，不代表已获得完整的跨组织授权方案。
     */
    private Boolean visitEnable = false;

    /**
     * 需要忽略多租户的请求
     *
     * 默认情况下，每个请求需要带上 tenant-id 的请求头。但是，部分请求是无需带上的，例如说短信回调、支付回调等 Open API！
     */
    private Set<String> ignoreUrls = new HashSet<>();

    /**
     * 需要忽略跨（切换）租户访问的请求
     *
     * 原因是：某些接口，访问的是个人信息，在跨租户是获取不到的！
     */
    private Set<String> ignoreVisitUrls = Collections.emptySet();

    /**
     * 需要忽略多租户的表
     *
     * 即默认所有表都开启多租户的功能，所以记得添加对应的 tenant_id 字段哟
     */
    private Set<String> ignoreTables = Collections.emptySet();

    /**
     * 需要忽略多租户的 Spring Cache 缓存
     *
     * 即默认所有缓存都开启多租户的功能，所以记得添加对应的 tenant_id 字段哟
     */
    private Set<String> ignoreCaches = Collections.emptySet();

}
