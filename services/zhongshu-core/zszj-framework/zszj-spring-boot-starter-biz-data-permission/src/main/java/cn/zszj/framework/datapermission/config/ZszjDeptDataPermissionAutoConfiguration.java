package cn.zszj.framework.datapermission.config;

import cn.zszj.framework.common.biz.system.permission.PermissionCommonApi;
import cn.zszj.framework.datapermission.core.authorize.ClassifiedObjectAuthorizationProvider;
import cn.zszj.framework.datapermission.core.authorize.FieldLevelScopeResolver;
import cn.zszj.framework.datapermission.core.authorize.ObjectAuthorizationProvider;
import cn.zszj.framework.datapermission.core.authorize.ObjectAuthorizationService;
import cn.zszj.framework.datapermission.core.rule.dept.DeptDataPermissionChecker;
import cn.zszj.framework.datapermission.core.rule.dept.DeptDataPermissionRule;
import cn.zszj.framework.datapermission.core.rule.dept.DeptDataPermissionRuleCustomizer;
import cn.zszj.framework.datapermission.core.rule.org.OrgDataPermissionChecker;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.security.core.service.SecurityFrameworkService;
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

    /**
     * ZS-PERM-002.B：组织级（跨组织）对象授权检查入口（org 轴）。
     *
     * <p>与 {@link DeptDataPermissionChecker}（dept/self 轴）正交并存，为详情/批量/导出路径提供
     * 「已知他组织对象 ID 越界拒绝」能力（FND-AUTH-004），复用同一 LoginUser 上下文缓存机制。
     */
    @Bean
    public OrgDataPermissionChecker orgDataPermissionChecker(PermissionCommonApi permissionApi) {
        return new OrgDataPermissionChecker(permissionApi);
    }

    /**
     * ZS-PERM-003.B：访问者可读字段等级上限解析器（D-12 §3 等级×角色默认映射——平台 F3/组织负责人 F2/成员 F1）。
     */
    @Bean
    public FieldLevelScopeResolver fieldLevelScopeResolver(PermissionCommonApi permissionApi) {
        return new FieldLevelScopeResolver(permissionApi);
    }

    /**
     * ZS-PERM-003.A：统一「动作 / 字段」授权输出与执行共用机制。
     *
     * <p>业务域按 {@link ObjectAuthorizationProvider} 声明候选动作/字段与状态约束，输出与执行共用同一
     * 裁决核心（前端伪造不生效）；未注册扩展点时 {@code authorize} 返回 null（未接入=零变化）。
     * 裁决依赖 {@code SecurityFrameworkService}（RBAC/visit 收敛）与 {@link OrgDataPermissionChecker}（org 轴对象门）；
     * ZS-PERM-003.B 起对 {@link ClassifiedObjectAuthorizationProvider} 按 D-12 字段等级目录裁剪并输出 maskedFields。
     */
    @Bean
    public ObjectAuthorizationService objectAuthorizationService(SecurityFrameworkService securityFrameworkService,
                                                                 OrgDataPermissionChecker orgDataPermissionChecker,
                                                                 FieldLevelScopeResolver fieldLevelScopeResolver,
                                                                 List<ObjectAuthorizationProvider> providers) {
        return new ObjectAuthorizationService(securityFrameworkService, orgDataPermissionChecker,
                fieldLevelScopeResolver, providers);
    }

}
