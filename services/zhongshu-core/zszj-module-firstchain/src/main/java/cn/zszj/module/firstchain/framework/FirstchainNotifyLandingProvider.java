package cn.zszj.module.firstchain.framework;

import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.module.firstchain.service.lead.FirstchainLeadAppService;
import cn.zszj.module.system.dal.dataobject.notify.NotifyMessageDO;
import cn.zszj.module.system.dal.dataobject.membership.MembershipDO;
import cn.zszj.module.system.dal.dataobject.organization.OrganizationDO;
import cn.zszj.module.system.enums.membership.MembershipStatusEnum;
import cn.zszj.module.system.enums.organization.OrganizationTypeEnum;
import cn.zszj.module.system.service.membership.MembershipService;
import cn.zszj.module.system.service.notify.landing.NotifyLandingClient;
import cn.zszj.module.system.service.notify.landing.NotifyLandingDescriptor;
import cn.zszj.module.system.service.notify.landing.NotifyLandingProvider;
import cn.zszj.module.system.service.organization.OrganizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.FIRSTCHAIN_APPLICATION_NOT_EXISTS;

/**
 * 首链消息落点注册条目（ZS-FC-003 服务端接线 wave）——MSG-003 {@link NotifyLandingProvider SPI 的
 * <b>首个生产注册方</b>（按模板编码注册七条目，见 {@link FirstchainNotifyConfiguration}）。
 *
 * <p>两条硬合同落实（MSG-003 五道防线之上的业务侧防线）：
 * <ul>
 *   <li><b>authorize 重新授权</b>：每次解析实时重读业务对象并校验当前用户资格——申请域=提交人本人或
 *       PLATFORM 有效任职；线索域=复用 {@link FirstchainLeadAppService#getLead} 的三视角可见性 +
 *       D-12 裁决链（越权抛业务异常 → 解析判 REVOKED）。不依据消息内容放行、不缓存授权结论，
 *       撤权/改派/组织变化即时生效；</li>
 *   <li><b>结构化落点</b>：{@link #resolve} 只返回模块/路由/业务 ID，不复制业务正文。</li>
 * </ul>
 *
 * <p>边界登记：MOBILE 端落点随 FC-003 UniApp 工作台 wave 落地后注册（本 wave 返回 null →
 * 解析判 CLIENT_UNSUPPORTED，五道防线既有语义）；WEB 路由指向 Web 工作台页面（同 wave 交付）。
 *
 * @author ZS-FC-003
 */
@Slf4j
public class FirstchainNotifyLandingProvider implements NotifyLandingProvider {

    /** 落点业务域（决定 authorize 面与 WEB 路由） */
    public enum Kind {
        /** 申请域落点（审批/开通通知） */
        APPLICATION,
        /** 线索域落点（下发/分配/领取/结束通知） */
        LEAD
    }

    private final String templateCode;

    private final Kind kind;

    private final JdbcTemplate jdbcTemplate;

    private final MembershipService membershipService;

    private final OrganizationService organizationService;

    private final FirstchainLeadAppService leadAppService;

    public FirstchainNotifyLandingProvider(String templateCode, Kind kind, DataSource dataSource,
                                           MembershipService membershipService,
                                           OrganizationService organizationService,
                                           FirstchainLeadAppService leadAppService) {
        this.templateCode = templateCode;
        this.kind = kind;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.membershipService = membershipService;
        this.organizationService = organizationService;
        this.leadAppService = leadAppService;
    }

    @Override
    public String templateCode() {
        return templateCode;
    }

    @Override
    public String module() {
        return FirstchainNotifyTemplates.MODULE;
    }

    @Override
    public void authorize(NotifyMessageDO message, Map<String, Object> templateParams) {
        switch (kind) {
            case APPLICATION -> authorizeApplication(templateParams);
            case LEAD -> authorizeLead(templateParams);
        }
    }

    /** 申请域重新授权：提交人本人或 PLATFORM 有效任职可进入详情（实时重算，不缓存）。 */
    private void authorizeApplication(Map<String, Object> templateParams) {
        String appKey = strParam(templateParams.get("appKey"));
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, creator FROM bpm_first_chain_application "
                        + "WHERE tenant_id = ? AND app_key = ? AND deleted = FALSE",
                TenantContextHolder.getTenantId(), appKey);
        if (rows.isEmpty()) {
            throw exception(FIRSTCHAIN_APPLICATION_NOT_EXISTS);
        }
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        String creator = (String) rows.get(0).get("creator");
        if (creator != null && creator.equals(loginUserId == null ? null : String.valueOf(loginUserId))) {
            return;
        }
        if (hasPlatformMembership(loginUserId)) {
            return;
        }
        throw exception(FIRSTCHAIN_APPLICATION_NOT_EXISTS);
    }

    /** 线索域重新授权：复用三视角可见性 + D-12 裁决链（越权抛业务异常 → REVOKED）。 */
    private void authorizeLead(Map<String, Object> templateParams) {
        Long leadId = parseLeadId(templateParams.get("leadId"));
        leadAppService.getLead(leadId);
    }

    private static Long parseLeadId(Object raw) {
        if (raw instanceof Number number) {
            return number.longValue();
        }
        if (raw != null) {
            try {
                return Long.parseLong(String.valueOf(raw));
            } catch (NumberFormatException malformed) {
                // fall through：伪造/畸形参数按对象不存在拒绝
            }
        }
        throw exception(cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_NOT_EXISTS);
    }

    @Override
    public NotifyLandingDescriptor resolve(NotifyLandingClient client, Map<String, Object> templateParams) {
        if (client != NotifyLandingClient.WEB) {
            return null; // MOBILE 落点随 UniApp 工作台 wave 注册（边界登记）
        }
        Map<String, Object> params = Map.of(kind == Kind.APPLICATION
                ? "appKey" : "id",
                kind == Kind.APPLICATION
                        ? String.valueOf(templateParams.get("appKey"))
                        : String.valueOf(templateParams.get("leadId")));
        String route = kind == Kind.APPLICATION ? "/firstchain/application" : "/firstchain/lead";
        return NotifyLandingDescriptor.builder()
                .module(FirstchainNotifyTemplates.MODULE)
                .route(route)
                .params(params)
                .build();
    }

    // ========== 内部 ==========

    private boolean hasPlatformMembership(Long userId) {
        if (userId == null) {
            return false;
        }
        MembershipDO membership = membershipService.getPrimaryMembership(userId);
        if (membership == null || !MembershipStatusEnum.ACTIVE.getStatus().equals(membership.getStatus())) {
            return false;
        }
        OrganizationDO organization = organizationService.getOrganization(membership.getOrganizationId());
        return organization != null && OrganizationTypeEnum.PLATFORM.getType().equals(organization.getType());
    }

    private static String strParam(Object value) {
        return value == null ? null : String.valueOf(value);
    }

}
