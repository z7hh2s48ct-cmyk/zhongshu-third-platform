package cn.zszj.framework.datapermission.config;

import cn.zszj.framework.common.biz.system.permission.PermissionCommonApi;
import cn.zszj.framework.datapermission.core.rule.org.OrgDataPermissionChecker;
import cn.zszj.framework.datapermission.core.rule.org.OrgDataPermissionRule;
import cn.zszj.framework.datapermission.core.rule.org.OrgDataPermissionRuleCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * 基于组织归属的数据权限 AutoConfiguration（org 轴，ZS-FC-002）。
 *
 * <p>独立于 {@link ZszjDeptDataPermissionAutoConfiguration} 装配（该类级 {@code @ConditionalOnBean}
 * 为 dept 轴 Customizer——PERM-003.A 评审登记的下游接入注意点）：org 轴规则按自身的
 * {@code OrgDataPermissionRuleCustomizer} 存在性装配，两轴互不牵连。
 *
 * <p>SQL 静默过滤（本类 {@link OrgDataPermissionRule}）与对象级显式拒绝
 * （{@link OrgDataPermissionChecker}，ZS-PERM-002.B）共用同一 org 轴范围来源，口径同源互补。
 *
 * @author ZS-FC-002
 */
@AutoConfiguration
@ConditionalOnClass(OrgDataPermissionChecker.class)
@ConditionalOnBean(value = {OrgDataPermissionRuleCustomizer.class})
public class ZszjOrgDataPermissionAutoConfiguration {

    @Bean
    public OrgDataPermissionRule orgDataPermissionRule(PermissionCommonApi permissionApi,
                                                       List<OrgDataPermissionRuleCustomizer> customizers) {
        // 创建 OrgDataPermissionRule 对象
        OrgDataPermissionRule rule = new OrgDataPermissionRule(permissionApi);
        // 补全表配置
        customizers.forEach(customizer -> customizer.customize(rule));
        return rule;
    }

}
