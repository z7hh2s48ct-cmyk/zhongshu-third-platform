package cn.zszj.module.firstchain.framework;

import cn.zszj.framework.datapermission.core.rule.org.OrgDataPermissionRuleCustomizer;
import cn.zszj.module.firstchain.service.FirstchainLeadService;
import cn.zszj.module.firstchain.service.FirstchainUserOrgChecker;
import cn.zszj.module.system.api.user.AdminUserApi;
import cn.zszj.module.system.service.membership.MembershipService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * firstchain 模块数据权限与授权装配（ZS-FC-002）。
 *
 * <p>①org 轴 SQL 静默过滤注册：首张业务表 {@code bpm_first_chain_lead} 注册 org_id 列
 * （B08 放行报告 §6.1 移交项在本卡闭合）——循环启动依赖断开：规则 Bean 由
 * {@code ZszjOrgDataPermissionAutoConfiguration}（Customizer 存在性装配）创建，本类只提供注册方；
 * ②线索域动作/字段授权 Provider（D-12 A 类目录编目接入）；③对象级资格校验生产实现
 * （{@link FirstchainUserOrgChecker}，经 system 用户 API + 有效任职实时判定，不缓存结论）。
 *
 * @author ZS-FC-002
 */
@Configuration(proxyBeanMethods = false)
public class FirstchainDataPermissionConfiguration {

    @Bean
    public OrgDataPermissionRuleCustomizer firstchainOrgDataPermissionRuleCustomizer() {
        return rule -> rule.addOrgColumn("bpm_first_chain_lead");
    }

    @Bean
    public FirstchainLeadAuthorizationProvider firstchainLeadAuthorizationProvider() {
        return new FirstchainLeadAuthorizationProvider();
    }

    @Bean
    public FirstchainLeadService.UserOrgChecker firstchainUserOrgChecker(AdminUserApi adminUserApi,
                                                                          MembershipService membershipService) {
        return new FirstchainUserOrgChecker(adminUserApi, membershipService);
    }

}
