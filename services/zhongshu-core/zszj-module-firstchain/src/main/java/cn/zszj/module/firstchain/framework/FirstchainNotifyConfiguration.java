package cn.zszj.module.firstchain.framework;

import cn.zszj.module.firstchain.service.lead.FirstchainLeadAppService;
import cn.zszj.module.system.service.membership.MembershipService;
import cn.zszj.module.system.service.organization.OrganizationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

import static cn.zszj.module.firstchain.framework.FirstchainNotifyTemplates.APPLICATION_APPROVED;
import static cn.zszj.module.firstchain.framework.FirstchainNotifyTemplates.APPLICATION_REJECTED;
import static cn.zszj.module.firstchain.framework.FirstchainNotifyTemplates.APPLICATION_SUBMITTED;
import static cn.zszj.module.firstchain.framework.FirstchainNotifyTemplates.LEAD_ASSIGNED;
import static cn.zszj.module.firstchain.framework.FirstchainNotifyTemplates.LEAD_CLAIMED;
import static cn.zszj.module.firstchain.framework.FirstchainNotifyTemplates.LEAD_CLOSED;
import static cn.zszj.module.firstchain.framework.FirstchainNotifyTemplates.LEAD_DISTRIBUTED;

/**
 * 首链通知落点装配（ZS-FC-003 服务端接线 wave）——MSG-003 {@code NotifyLandingProvider} 首批生产注册方：
 * 七条目按模板编码注册（重复编码启动即失败，登记即合同）。
 *
 * <p>authorize 重新授权为硬合同：申请域=提交人或 PLATFORM 任职；线索域=三视角可见性 + D-12 裁决链
 * （详见 {@link FirstchainNotifyLandingProvider}）。MOBILE 端落点随 UniApp 工作台 wave 落地后注册。
 *
 * @author ZS-FC-003
 */
@Configuration(proxyBeanMethods = false)
public class FirstchainNotifyConfiguration {

    @Bean
    public FirstchainNotifyLandingProvider firstchainApplicationSubmittedLandingProvider(DataSource dataSource,
            MembershipService membershipService, OrganizationService organizationService,
            FirstchainLeadAppService leadAppService) {
        return new FirstchainNotifyLandingProvider(APPLICATION_SUBMITTED,
                FirstchainNotifyLandingProvider.Kind.APPLICATION, dataSource, membershipService,
                organizationService, leadAppService);
    }

    @Bean
    public FirstchainNotifyLandingProvider firstchainApplicationApprovedLandingProvider(DataSource dataSource,
            MembershipService membershipService, OrganizationService organizationService,
            FirstchainLeadAppService leadAppService) {
        return new FirstchainNotifyLandingProvider(APPLICATION_APPROVED,
                FirstchainNotifyLandingProvider.Kind.APPLICATION, dataSource, membershipService,
                organizationService, leadAppService);
    }

    @Bean
    public FirstchainNotifyLandingProvider firstchainApplicationRejectedLandingProvider(DataSource dataSource,
            MembershipService membershipService, OrganizationService organizationService,
            FirstchainLeadAppService leadAppService) {
        return new FirstchainNotifyLandingProvider(APPLICATION_REJECTED,
                FirstchainNotifyLandingProvider.Kind.APPLICATION, dataSource, membershipService,
                organizationService, leadAppService);
    }

    @Bean
    public FirstchainNotifyLandingProvider firstchainLeadDistributedLandingProvider(DataSource dataSource,
            MembershipService membershipService, OrganizationService organizationService,
            FirstchainLeadAppService leadAppService) {
        return new FirstchainNotifyLandingProvider(LEAD_DISTRIBUTED,
                FirstchainNotifyLandingProvider.Kind.LEAD, dataSource, membershipService,
                organizationService, leadAppService);
    }

    @Bean
    public FirstchainNotifyLandingProvider firstchainLeadAssignedLandingProvider(DataSource dataSource,
            MembershipService membershipService, OrganizationService organizationService,
            FirstchainLeadAppService leadAppService) {
        return new FirstchainNotifyLandingProvider(LEAD_ASSIGNED,
                FirstchainNotifyLandingProvider.Kind.LEAD, dataSource, membershipService,
                organizationService, leadAppService);
    }

    @Bean
    public FirstchainNotifyLandingProvider firstchainLeadClaimedLandingProvider(DataSource dataSource,
            MembershipService membershipService, OrganizationService organizationService,
            FirstchainLeadAppService leadAppService) {
        return new FirstchainNotifyLandingProvider(LEAD_CLAIMED,
                FirstchainNotifyLandingProvider.Kind.LEAD, dataSource, membershipService,
                organizationService, leadAppService);
    }

    @Bean
    public FirstchainNotifyLandingProvider firstchainLeadClosedLandingProvider(DataSource dataSource,
            MembershipService membershipService, OrganizationService organizationService,
            FirstchainLeadAppService leadAppService) {
        return new FirstchainNotifyLandingProvider(LEAD_CLOSED,
                FirstchainNotifyLandingProvider.Kind.LEAD, dataSource, membershipService,
                organizationService, leadAppService);
    }

}
