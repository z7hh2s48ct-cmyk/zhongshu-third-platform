package cn.zszj.module.firstchain.service.lead;

import cn.hutool.core.util.RandomUtil;
import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.framework.datapermission.core.authorize.FieldMaskUtils;
import cn.zszj.framework.datapermission.core.authorize.ObjectAuthorizationRequest;
import cn.zszj.framework.datapermission.core.authorize.ObjectAuthorizationRespDTO;
import cn.zszj.framework.datapermission.core.authorize.ObjectAuthorizationService;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.module.firstchain.controller.admin.lead.vo.LeadAssignReqVO;
import cn.zszj.module.firstchain.controller.admin.lead.vo.LeadClaimReqVO;
import cn.zszj.module.firstchain.controller.admin.lead.vo.LeadConvertReqVO;
import cn.zszj.module.firstchain.controller.admin.lead.vo.LeadDistributeReqVO;
import cn.zszj.module.firstchain.controller.admin.lead.vo.LeadFollowupReqVO;
import cn.zszj.module.firstchain.controller.admin.lead.vo.LeadInvalidateReqVO;
import cn.zszj.module.firstchain.controller.admin.lead.vo.LeadPageReqVO;
import cn.zszj.module.firstchain.controller.admin.lead.vo.LeadReassignReqVO;
import cn.zszj.module.firstchain.controller.admin.lead.vo.LeadRespVO;
import cn.zszj.module.firstchain.framework.FirstchainLeadAuthorizationProvider;
import cn.zszj.module.firstchain.service.FirstchainLeadMetricsService;
import cn.zszj.module.firstchain.service.FirstchainLeadService;
import cn.zszj.module.firstchain.service.wiring.FirstchainNotifyWiringService;
import cn.zszj.module.system.dal.dataobject.membership.MembershipDO;
import cn.zszj.module.system.dal.dataobject.organization.OrganizationDO;
import cn.zszj.module.system.enums.membership.MembershipStatusEnum;
import cn.zszj.module.system.enums.organization.OrganizationTypeEnum;
import cn.zszj.module.system.service.membership.MembershipService;
import cn.zszj.module.system.service.organization.OrganizationService;
import cn.zszj.framework.common.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_ACTOR_NOT_LEADER;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_ACTOR_QUALIFICATION_DENIED;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_CONVERT_NOT_ASSIGNEE;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_DISTRIBUTE_NOT_PLATFORM;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_KEY_CONFLICT;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_TENANT_REQUIRED;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_VISIBLE_DENIED;

/**
 * 首链线索域 REST 面门面服务（ZS-FC-002 REST wave，PILOT-REQ-005~009 出口侧）。
 *
 * <p>职责边界（与领域层 {@link FirstchainLeadService} 分工，接入合同 §1.6 两层模型）：
 * <ul>
 *     <li><b>视角服务端解析</b>（PILOT-REQ-009「员工看本人、负责人看本组织、平台人员看授权范围」）：
 *     操作人视角由默认任职 + 组织类型/负责人字段服务端判定（M2 三角色），不接受客户端声明视角、
 *     归属或分配人——请求 VO 不携带 orgId/assigneeUserId 查询参数；</li>
 *     <li><b>对象级资格二次校验</b>：下发限平台运营（PILOT-REQ-005）、分配/改派限归属组织负责人
 *     （D-07 M6）、转商机限被分配员工本人、无效关闭限被分配员工或负责人代操作（D-07 M7）；
 *     动作级权限由 Controller 层 @PreAuthorize 承担，两层不可互相替代；</li>
 *     <li><b>业务键服务端生成</b>（D-07 M1）：线索编号 {@code LC + yyyyMMdd + 8 位随机}（uk 撞号
 *     有限次重试）、商机编号 {@code OP + ...} 同形态；</li>
 *     <li><b>D-12 A 类字段出口裁剪</b>（FC-002 验收「F2 字段无授权不出现在任何出口」）：
 *     详情/列表逐行经 {@link ObjectAuthorizationService} 实时裁决（对象维越权显式拒绝、字段维
 *     授权输出），F2 低于访问者上限按 {@link FieldMaskUtils#maskKeepTail}（尾四位）脱敏，
 *     超限字段整字段不出——与导出/文件出口共用同一裁决核心；</li>
 *     <li>写侧委托领域层（状态机/执行器/审计在领域层内聚，本层不双写）。</li>
 * </ul>
 *
 * @author ZS-FC-002
 */
@Service
@Slf4j
public class FirstchainLeadAppService {

    /** 线索编号前缀（线索域业务键，与申请域 FC 前缀同形态，D-07 M1） */
    private static final String LEAD_KEY_PREFIX = "LC";

    /** 商机编号前缀（转商机产物业务键，M7） */
    private static final String OPP_KEY_PREFIX = "OP";

    /** 业务键随机段长度（撞号概率 36^8，uk 命中重试上限内不可达） */
    private static final int KEY_RANDOM_LENGTH = 8;

    /** 业务键撞号重试上限（uk(tenant, lead_key) 冲突时的服务端重新生成次数） */
    private static final int KEY_MAX_RETRY = 3;

    /** F2 脱敏保留尾位数（D-12 §5.2 默认值） */
    private static final int MASK_KEEP_TAIL = 4;

    /** D-12 A 类线索出口字段 → LeadRespVO 属性（裁剪映射；followup_content 归跟进出口，不在本 VO） */
    private static final Map<String, String> FIELD_PROPERTIES = Map.of(
            "customer_name", "customerName",
            "customer_phone", "customerPhone",
            "customer_wechat", "customerWechat",
            "customer_address", "customerAddress",
            "source", "source");

    @Resource
    private FirstchainLeadService leadService;

    @Resource
    private FirstchainLeadMetricsService metricsService;

    @Resource
    private MembershipService membershipService;

    @Resource
    private OrganizationService organizationService;

    @Resource
    private ObjectAuthorizationService objectAuthorizationService;

    @Resource
    private FirstchainNotifyWiringService notifyWiringService;

    // ========== PILOT-REQ-005：平台下发 ==========

    /**
     * 下发线索（平台运营；线索编号服务端生成，归属组织由请求显式指定并经服务端写入 org_id）。
     *
     * @return 线索行 ID（新建；重复下发同键同内容由领域层幂等吸收，同键异内容换号重试——
     *         服务端随机键下「撞号异内容」仅为概率事件，重试即解）
     */
    public Long distributeLead(LeadDistributeReqVO reqVO) {
        requireTenantId();
        requireCallerView().assertPlatform();
        ServiceException lastConflict = null;
        for (int attempt = 0; attempt < KEY_MAX_RETRY; attempt++) {
            try {
                Long leadId = leadService.distribute(new FirstchainLeadService.DistributeCmd(
                        generateKey(LEAD_KEY_PREFIX),
                        reqVO.getCustomerName(), reqVO.getCustomerPhone(), reqVO.getCustomerWechat(),
                        reqVO.getCustomerAddress(), reqVO.getSource(), reqVO.getOrgId(), currentActorId()));
                // 接线（ZS-FC-003，同事务）：下发通知致归属组织负责人
                notifyWiringService.onLeadDistributed(leadService.getLead(leadId), currentActorId());
                return leadId;
            } catch (ServiceException conflict) {
                if (!LEAD_KEY_CONFLICT.getCode().equals(conflict.getCode())) {
                    throw conflict;
                }
                lastConflict = conflict;
                log.info("[distributeLead][线索编号撞号重试：attempt={}]", attempt + 1);
            }
        }
        throw lastConflict;
    }

    // ========== PILOT-REQ-006：分配 / 领取 / 改派 ==========

    /**
     * 负责人分配线索至本组织员工（对象级：操作人=线索归属组织负责人，D-07 M6）。
     * 接线（ZS-FC-003，同事务）：分配通知致被分配员工。
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignLead(LeadAssignReqVO reqVO) {
        requireTenantId();
        LeadCallerView caller = requireCallerView();
        Map<String, Object> lead = leadService.getLead(reqVO.getId());
        requireLeaderOfOrg(caller, leadOrgId(lead));
        leadService.assign(reqVO.getId(), reqVO.getAssigneeUserId(), reqVO.getExpectedVersion(), currentActorId());
        notifyWiringService.onLeadAssigned(lead, reqVO.getAssigneeUserId(), currentActorId());
    }

    /**
     * 员工领取线索（领取人服务端绑定登录用户——请求不可声明领取人，PILOT-REQ-006「只能领取分配给自己的」）。
     * 接线（ZS-FC-003，同事务）：领取通知致归属组织负责人。
     */
    @Transactional(rollbackFor = Exception.class)
    public void claimLead(LeadClaimReqVO reqVO) {
        requireTenantId();
        LeadCallerView caller = requireCallerView();
        leadService.claim(reqVO.getId(), caller.userId(), reqVO.getExpectedVersion(), currentActorId());
        notifyWiringService.onLeadClaimed(leadService.getLead(reqVO.getId()), currentActorId());
    }

    /**
     * 负责人改派（对象级同分配；停用员工线索保留原分配不自动归还，由负责人显式改派，D-07 M9）。
     * 接线（ZS-FC-003，同事务）：改派通知致新被分配员工。
     */
    @Transactional(rollbackFor = Exception.class)
    public void reassignLead(LeadReassignReqVO reqVO) {
        requireTenantId();
        LeadCallerView caller = requireCallerView();
        Map<String, Object> lead = leadService.getLead(reqVO.getId());
        requireLeaderOfOrg(caller, leadOrgId(lead));
        leadService.reassign(reqVO.getId(), reqVO.getNewAssigneeUserId(), reqVO.getExpectedVersion(),
                currentActorId());
        notifyWiringService.onLeadAssigned(lead, reqVO.getNewAssigneeUserId(), currentActorId());
    }

    // ========== PILOT-REQ-007：跟进 ==========

    /**
     * 追加跟进记录（跟进人服务端绑定登录用户——「仅本人可提交」，PILOT-REQ-007；追加式无修改通道）。
     *
     * @return 跟进记录行 ID
     */
    public Long followupLead(LeadFollowupReqVO reqVO) {
        requireTenantId();
        LeadCallerView caller = requireCallerView();
        return leadService.followup(reqVO.getLeadId(), new FirstchainLeadService.FollowupCmd(
                reqVO.getContent(), reqVO.getNextStep(), caller.userId(), reqVO.getFollowupTime(),
                currentActorId()));
    }

    // ========== PILOT-REQ-008：转商机 / 无效关闭 ==========

    /**
     * 转商机（对象级：操作人=被分配员工本人，D-07 M7「员工可发起」；商机编号服务端生成——
     * 每线索单商机由 uk(lead_id) 兜底，重复转化不重试直接显式冲突）。
     * 接线（ZS-FC-003，同事务）：结束通知（转商机）致归属组织负责人。
     *
     * @return 商机行 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long convertLead(LeadConvertReqVO reqVO) {
        requireTenantId();
        LeadCallerView caller = requireCallerView();
        Map<String, Object> lead = leadService.getLead(reqVO.getLeadId());
        requireConvertAssignee(caller, lead);
        Long opportunityId = leadService.convertToOpportunity(reqVO.getLeadId(), generateKey(OPP_KEY_PREFIX),
                reqVO.getExpectedVersion(), currentActorId());
        notifyWiringService.onLeadClosed(lead, "转商机", currentActorId());
        return opportunityId;
    }

    /**
     * 无效关闭（对象级：被分配员工发起或负责人代操作，D-07 M7；一期不设审批环节）。
     * 接线（ZS-FC-003，同事务）：结束通知（无效关闭）致归属组织负责人。
     */
    @Transactional(rollbackFor = Exception.class)
    public void invalidateLead(LeadInvalidateReqVO reqVO) {
        requireTenantId();
        LeadCallerView caller = requireCallerView();
        Map<String, Object> lead = leadService.getLead(reqVO.getLeadId());
        if (!caller.isAssigneeOf(lead) && !caller.isLeaderOf(leadOrgId(lead))) {
            throw exception(LEAD_ACTOR_NOT_LEADER);
        }
        leadService.invalidate(reqVO.getLeadId(), reqVO.getReasonName(), reqVO.getReasonDetail(),
                reqVO.getExpectedVersion(), currentActorId());
        notifyWiringService.onLeadClosed(lead, "无效关闭", currentActorId());
    }

    // ========== PILOT-REQ-009：三视角查询（范围服务端解析 + D-12 出口裁剪） ==========

    /**
     * 线索分页（视角服务端解析：平台=授权范围（org 轴 checker 兜底）、负责人=本组织、员工=本人）。
     */
    public PageResult<LeadRespVO> getLeadPage(LeadPageReqVO pageReqVO) {
        LeadCallerView caller = requireCallerView();
        PageResult<Map<String, Object>> pageResult = leadService.page(caller.scopeOrgId(), caller.scopeAssigneeUserId(),
                pageReqVO.getStatus(), pageReqVO.getPageNo(), pageReqVO.getPageSize());
        List<LeadRespVO> rows = new ArrayList<>(pageResult.getList().size());
        for (Map<String, Object> row : pageResult.getList()) {
            rows.add(toRespVO(row));
        }
        return new PageResult<>(rows, pageResult.getTotal());
    }

    /**
     * 线索详情（租户内越权显式拒绝——PILOT-REQ-009「对象 ID 越权被拒绝」；D-12 裁决含对象维）。
     */
    public LeadRespVO getLead(Long id) {
        LeadCallerView caller = requireCallerView();
        Map<String, Object> lead = leadService.getLead(id);
        if (!caller.canView(lead)) {
            throw exception(LEAD_VISIBLE_DENIED);
        }
        return toRespVO(lead);
    }

    /**
     * 线索状态指标（三视角同源：权威状态列 GROUP BY，仅范围随视角收窄，口径与领域指标一致）。
     */
    public Map<String, Long> getLeadStatusMetrics() {
        Long tenantId = requireTenantId();
        LeadCallerView caller = requireCallerView();
        return switch (caller.role()) {
            case PLATFORM -> metricsService.countByStatus(tenantId);
            case LEADER -> metricsService.countByStatusAndOrg(tenantId, caller.orgId());
            case EMPLOYEE -> metricsService.countByStatusAndAssignee(tenantId, caller.userId());
        };
    }

    // ========== 内部：视角解析（M2 三角色，服务端判定） ==========

    /**
     * 操作人视角（实时重算不缓存）：默认任职 ACTIVE + 组织类型判定——PLATFORM 任职=平台运营；
     * FRANCHISEE 组织 leaderUserId=本人=加盟商负责人；其余 FRANCHISEE 有效任职=员工。
     * 无任职/组织不可用/他类组织一律 fail-closed 拒绝。
     */
    private LeadCallerView requireCallerView() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            throw exception(LEAD_ACTOR_QUALIFICATION_DENIED);
        }
        MembershipDO membership = membershipService.getPrimaryMembership(userId);
        if (membership == null || !MembershipStatusEnum.ACTIVE.getStatus().equals(membership.getStatus())) {
            throw exception(LEAD_ACTOR_QUALIFICATION_DENIED);
        }
        OrganizationDO organization = organizationService.getOrganization(membership.getOrganizationId());
        if (organization == null) {
            throw exception(LEAD_ACTOR_QUALIFICATION_DENIED);
        }
        if (OrganizationTypeEnum.PLATFORM.getType().equals(organization.getType())) {
            return new LeadCallerView(LeadRole.PLATFORM, organization.getId(), userId);
        }
        if (OrganizationTypeEnum.FRANCHISEE.getType().equals(organization.getType())) {
            boolean leader = userId.equals(organization.getLeaderUserId());
            return new LeadCallerView(leader ? LeadRole.LEADER : LeadRole.EMPLOYEE,
                    organization.getId(), userId);
        }
        throw exception(LEAD_ACTOR_QUALIFICATION_DENIED);
    }

    private void requireLeaderOfOrg(LeadCallerView caller, Long leadOrgId) {
        if (!caller.isLeaderOf(leadOrgId)) {
            throw exception(LEAD_ACTOR_NOT_LEADER);
        }
    }

    private void requireConvertAssignee(LeadCallerView caller, Map<String, Object> lead) {
        if (!caller.isAssigneeOf(lead)) {
            throw exception(LEAD_CONVERT_NOT_ASSIGNEE);
        }
    }

    // ========== 内部：D-12 出口裁剪（详情/列表统一出口） ==========

    /**
     * 行 → 出口 VO，并施加 D-12 裁决：对象维越权由裁决服务显式拒绝（FORBIDDEN）；
     * 字段维超限字段整字段不出、F2 低于上限脱敏（尾四位，D-12 §5.2）。
     * 每行实时裁决（对象状态/授权变更后即生效，与执行侧共用同一裁决核心）。
     */
    private LeadRespVO toRespVO(Map<String, Object> row) {
        LeadRespVO respVO = new LeadRespVO();
        respVO.setId(((Number) row.get("id")).longValue());
        respVO.setLeadKey((String) row.get("lead_key"));
        respVO.setCustomerName((String) row.get("customer_name"));
        respVO.setCustomerPhone((String) row.get("customer_phone"));
        respVO.setCustomerWechat((String) row.get("customer_wechat"));
        respVO.setCustomerAddress((String) row.get("customer_address"));
        respVO.setSource((String) row.get("source"));
        respVO.setOrgId(leadOrgId(row));
        respVO.setAssigneeUserId(row.get("assignee_user_id") == null
                ? null : ((Number) row.get("assignee_user_id")).longValue());
        respVO.setStatus((String) row.get("status"));
        respVO.setVersion(row.get("version") == null ? null : ((Number) row.get("version")).longValue());
        respVO.setCreateTime(toLocalDateTime(row.get("create_time")));
        respVO.setUpdateTime(toLocalDateTime(row.get("update_time")));
        respVO.setCreator((String) row.get("creator"));
        respVO.setUpdater((String) row.get("updater"));
        applyFieldScope(respVO);
        return respVO;
    }

    /**
     * JDBC 时间列 → LocalDateTime：PG 驱动经 JdbcTemplate.queryForList 返回 {@link java.sql.Timestamp}，H2 返回
     * LocalDateTime——硬转 LocalDateTime 会使线索详情/分页在真实 PG 上 ClassCastException（首链 E2E 暴露）。
     */
    static LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value instanceof java.util.Date date) {
            return new java.sql.Timestamp(date.getTime()).toLocalDateTime();
        }
        throw new IllegalStateException("不支持的时间列类型：" + value.getClass().getName());
    }

    private void applyFieldScope(LeadRespVO respVO) {
        ObjectAuthorizationRespDTO authorization = objectAuthorizationService.authorize(
                ObjectAuthorizationRequest.of(FirstchainLeadAuthorizationProvider.OBJECT_TYPE,
                        respVO.getOrgId(), respVO.getAssigneeUserId(), respVO.getStatus()));
        if (authorization == null || authorization.getAuthorizedFields() == null) {
            return; // 未接入=零变化（SPI 未注册的部署形态兜底，不抛异常）
        }
        Set<String> authorized = authorization.getAuthorizedFields();
        Set<String> masked = authorization.getMaskedFields() == null ? Set.of() : authorization.getMaskedFields();
        FIELD_PROPERTIES.forEach((field, property) -> {
            if (!authorized.contains(field)) {
                writeProperty(respVO, property, null); // 超上限等级：整字段不出（fail-closed）
                return;
            }
            if (masked.contains(field)) {
                writeProperty(respVO, property,
                        FieldMaskUtils.maskKeepTail((String) readProperty(respVO, property), MASK_KEEP_TAIL));
            }
        });
    }

    private static Object readProperty(LeadRespVO respVO, String property) {
        return switch (property) {
            case "customerName" -> respVO.getCustomerName();
            case "customerPhone" -> respVO.getCustomerPhone();
            case "customerWechat" -> respVO.getCustomerWechat();
            case "customerAddress" -> respVO.getCustomerAddress();
            case "source" -> respVO.getSource();
            default -> throw new IllegalStateException("未知的线索出口字段: " + property);
        };
    }

    private static void writeProperty(LeadRespVO respVO, String property, Object value) {
        String stringValue = (String) value;
        switch (property) {
            case "customerName" -> respVO.setCustomerName(stringValue);
            case "customerPhone" -> respVO.setCustomerPhone(stringValue);
            case "customerWechat" -> respVO.setCustomerWechat(stringValue);
            case "customerAddress" -> respVO.setCustomerAddress(stringValue);
            case "source" -> respVO.setSource(stringValue);
            default -> throw new IllegalStateException("未知的线索出口字段: " + property);
        }
    }

    // ========== 内部：工具 ==========

    private static Long leadOrgId(Map<String, Object> lead) {
        return ((Number) lead.get("org_id")).longValue();
    }

    /**
     * 业务键生成（D-07 M1：服务端生成、租户内唯一）：{@code 前缀 + yyyyMMdd + '-' + 8 位随机大写串}。
     */
    private static String generateKey(String prefix) {
        return prefix + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + RandomUtil.randomStringUpper(KEY_RANDOM_LENGTH);
    }

    private static Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw exception(LEAD_TENANT_REQUIRED);
        }
        return tenantId;
    }

    private static String currentActorId() {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        return loginUserId == null ? null : String.valueOf(loginUserId);
    }

    /** 操作人视角角色（M2 三角色：平台运营/加盟商负责人/员工） */
    public enum LeadRole {
        PLATFORM, LEADER, EMPLOYEE
    }

    /**
     * 操作人视角值对象：范围收敛查询参数（平台=全域、负责人=本组织、员工=本人）与对象级判定。
     */
    public record LeadCallerView(LeadRole role, Long orgId, Long userId) {

        void assertPlatform() {
            if (role != LeadRole.PLATFORM) {
                throw exception(LEAD_DISTRIBUTE_NOT_PLATFORM);
            }
        }

        /** 查询范围收敛：平台不限（org 轴 checker 兜底授权范围）、负责人钉本组织、员工钉本人 */
        Long scopeOrgId() {
            return role == LeadRole.LEADER ? orgId : null;
        }

        Long scopeAssigneeUserId() {
            return role == LeadRole.EMPLOYEE ? userId : null;
        }

        boolean isLeaderOf(Long leadOrgId) {
            return role == LeadRole.LEADER && orgId.equals(leadOrgId);
        }

        boolean isAssigneeOf(Map<String, Object> lead) {
            Object assignee = lead.get("assignee_user_id");
            return assignee instanceof Number assigneeId && assigneeId.longValue() == userId;
        }

        boolean canView(Map<String, Object> lead) {
            return switch (role) {
                case PLATFORM -> true; // 授权范围由 org 轴 checker 在裁决对象维兜底
                case LEADER -> isLeaderOf(leadOrgId(lead));
                case EMPLOYEE -> isAssigneeOf(lead);
            };
        }
    }

    /** 改派参数载体已并入 {@link LeadReassignReqVO}（保持 Controller→服务直传）。 */

}
