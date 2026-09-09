package cn.zszj.framework.datapermission.config;

import cn.zszj.framework.common.biz.system.permission.PermissionCommonApi;
import cn.zszj.framework.datapermission.core.rule.dept.DeptDataPermissionChecker;
import cn.zszj.framework.datapermission.core.rule.dept.DeptDataPermissionRule;
import cn.zszj.framework.datapermission.core.rule.dept.DeptDataPermissionRuleCustomizer;
import cn.zszj.framework.security.core.LoginUser;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * 基于部门的数据权限 AutoConfiguration
 *
 * @author 芋道源码
 */
@AutoConfiguration
@ConditionalOnClass(LoginUser.class)
@ConditionalOnBean(value = {DeptDataPermissionRuleCustomizer.class})
public class ZszjDeptDataPermissionAutoConfiguration {

    @Bean
    public DeptDataPermissionRule deptDataPermissionRule(PermissionCommonApi permissionApi,
                                                         List<DeptDataPermissionRuleCustomizer> customizers) {
        // 创建 DeptDataPermissionRule 对象
        DeptDataPermissionRule rule = new DeptDataPermissionRule(permissionApi);
        // 补全表配置
        customizers.forEach(customizer -> customizer.customize(rule));
        return rule;
    }

    /**
     * ZS-PERM-002.A：对象级数据范围检查入口。
     *
     * <p>与 {@link DeptDataPermissionRule}（SQL 静默过滤）互补，为详情/批量/导出路径提供显式拒绝能力，
     * 复用同一份数据范围与 LoginUser 上下文缓存。
     */
    @Bean
    public DeptDataPermissionChecker deptDataPermissionChecker(PermissionCommonApi permissionApi) {
        return new DeptDataPermissionChecker(permissionApi);
    }

}
