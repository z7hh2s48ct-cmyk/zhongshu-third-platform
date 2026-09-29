package cn.zszj.framework.datapermission.core.rule.org;

/**
 * {@link OrgDataPermissionRule} 的自定义配置接口（org 轴 SQL 静默过滤的表注册 SPI，ZS-FC-002）。
 *
 * <p>业务模块实现本接口并声明为 Bean，把带组织归属列的表注册进 org 轴规则；
 * 装配为 {@code @ConditionalOnBean(OrgDataPermissionRuleCustomizer.class)}——存在注册方才启用规则，
 * 无注册方时零开销（循 {@code DeptDataPermissionRuleCustomizer} 先例）。
 *
 * @author ZS-FC-002
 */
@FunctionalInterface
public interface OrgDataPermissionRuleCustomizer {

    /**
     * 自定义 org 轴「组织归属列」配置（表名 → org 列）
     */
    void customize(OrgDataPermissionRule rule);

}
