package cn.zszj.module.firstchain.service;

import cn.hutool.core.util.StrUtil;
import cn.zszj.framework.common.biz.system.audit.AuditEventMessage;
import cn.zszj.framework.common.biz.system.audit.AuditEventMessage.ActorType;
import cn.zszj.framework.common.biz.system.audit.AuditEventMessage.AuditResult;
import cn.zszj.framework.common.biz.system.audit.AuditEventTypes;
import cn.zszj.framework.common.biz.system.audit.AuditPort;
import cn.zszj.framework.common.exception.ErrorCode;
import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.module.bpm.firstchain.FirstChainObjectType;
import cn.zszj.module.bpm.firstchain.FirstChainStateMachines;
import cn.zszj.module.bpm.firstchain.FirstChainStateTransitionExecutor;
import cn.zszj.module.bpm.firstchain.LeadStatus;
import cn.zszj.module.firstchain.enums.LeadInvalidateReason;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.FOLLOWUP_LEAD_TERMINAL;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.FOLLOWUP_NOT_ALLOWED_FOR_ACTOR;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.FOLLOWUP_TIME_REQUIRED;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_ASSIGN_TARGET_NOT_IN_ORG;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_CLAIM_NOT_ASSIGNEE;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_CONVERT_REQUIRE_CUSTOMER_NAME;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_CONVERT_REQUIRE_FOLLOWUP;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_INVALIDATE_REASON_DETAIL_REQUIRED;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_INVALIDATE_REASON_REQUIRED;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_KEY_CONFLICT;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_NOT_EXISTS;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_REASSIGN_TARGET_NOT_IN_ORG;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_STATE_CONFLICT;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_TENANT_REQUIRED;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.LEAD_VERSION_CONFLICT;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.OPPORTUNITY_ALREADY_EXISTS;

/**
 * 首链线索领域服务（ZS-FC-002，PILOT-REQ-005~008 服务端；D-07 M6/M7/M9）。
 *
 * <p>形态与 {@code cn.zszj.module.bpm.firstchain.FirstChainApplicationService}（ZS-BPM-003）同构：
 * 状态迁移一律经 {@link FirstChainStateTransitionExecutor}（乐观锁 + fromStatus 守卫 + 租户过滤的单一执行面，
 * 表名合同 {@code bpm_first_chain_lead} 由 ZS-BPM-003 预留），语义守卫先经
 * {@link FirstChainStateMachines#assertAllowed}（fail-closed，终态锁定）；业务动作同事务写
 * SUCCESS 审计（bizId = leadId / lead_key 稳定业务键）。
 *
 * <p>分配/改派的目标员工归属校验经 {@link UserOrgChecker}（对象级资格在服务层二次校验，
 * 动作权限串由 Controller 层 @PreAuthorize 承担——接入合同 §1.6 两层不可互相替代）。
 * 跟进记录为追加式子对象（M6）：结构上无修改通道，PILOT-REQ-007「不能篡改」以此收敛。
 *
 * @author ZS-FC-002
 */
@Service
@Slf4j
public class FirstchainLeadService {

    private final JdbcTemplate jdbcTemplate;

    private final TransactionTemplate transactionTemplate;

    private final AuditPort auditPort;

    private final FirstChainStateTransitionExecutor stateTransitionExecutor;

    private final UserOrgChecker userOrgChecker;

    public FirstchainLeadService(DataSource dataSource, PlatformTransactionManager transactionManager,
                                 AuditPort auditPort, FirstChainStateTransitionExecutor stateTransitionExecutor,
                                 UserOrgChecker userOrgChecker) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.auditPort = auditPort;
        this.stateTransitionExecutor = stateTransitionExecutor;
        this.userOrgChecker = userOrgChecker;
    }

    // ========== PILOT-REQ-005：平台下发（M6 DISTRIBUTED；归属服务端写入；重复下发幂等） ==========

    /**
     * 下发线索（创建 DISTRIBUTED 基线，version=0 起步；org_id 由服务端写入归属——调用方不得声明归属）。
     *
     * <p>幂等规则（PILOT-REQ-005「重复下发按幂等规则处理」）：同 (tenant, lead_key) 且核心内容
     * （orgId/客户姓名/手机号）一致 → 吸收并返回既有 ID；同键异内容 → 显式冲突（不静默覆盖）。
     * lead_key 的「服务端生成」契约由 Controller 层承担（同 ZS-BPM-003 app_key 形态，M1）。
     *
     * @return 线索行 ID（新建或既有）
     */
    public Long distribute(DistributeCmd cmd) {
        Long tenantId = requireTenantId();
        if (cmd.orgId() == null) {
            throw exception(LEAD_TENANT_REQUIRED); // 归属组织缺失即拒：不默认、不伪造归属
        }
        return transactionTemplate.execute(status -> {
            List<Map<String, Object>> existingRows = jdbcTemplate.queryForList(
                    "SELECT id, org_id, COALESCE(customer_name, '') AS customer_name, "
                            + "COALESCE(customer_phone, '') AS customer_phone FROM bpm_first_chain_lead "
                            + "WHERE tenant_id = ? AND lead_key = ? AND deleted = FALSE",
                    tenantId, cmd.leadKey());
            if (!existingRows.isEmpty()) {
                Map<String, Object> existing = existingRows.get(0);
                boolean sameContent = String.valueOf(existing.get("org_id")).equals(String.valueOf(cmd.orgId()))
                        && existing.get("customer_name").equals(StrUtil.nullToEmpty(cmd.customerName()))
                        && existing.get("customer_phone").equals(StrUtil.nullToEmpty(cmd.customerPhone()));
                if (sameContent) {
                    return ((Number) existing.get("id")).longValue(); // 幂等吸收：重复下发返回既有线索
                }
                throw exception(LEAD_KEY_CONFLICT, cmd.leadKey());
            }
            try {
                jdbcTemplate.update(
                        "INSERT INTO bpm_first_chain_lead (lead_key, customer_name, customer_phone, customer_wechat, "
                                + "customer_address, source, org_id, status, version, tenant_id, creator, updater) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?, ?)",
                        cmd.leadKey(), cmd.customerName(), cmd.customerPhone(), cmd.customerWechat(),
                        cmd.customerAddress(), cmd.source(), cmd.orgId(), LeadStatus.DISTRIBUTED.name(),
                        tenantId, cmd.actorId(), cmd.actorId());
            } catch (org.springframework.dao.DuplicateKeyException concurrentDuplicate) {
                // COUNT 快路径的并发窗口由 uk(tenant, lead_key) 兜底：裸唯一键异常转译为业务错误码
                throw exception(LEAD_KEY_CONFLICT, cmd.leadKey());
            }
            Long id = jdbcTemplate.queryForObject(
                    "SELECT id FROM bpm_first_chain_lead WHERE tenant_id = ? AND lead_key = ? AND deleted = FALSE",
                    Long.class, tenantId, cmd.leadKey());
            auditPort.record(AuditEventMessage.builder()
                    .eventType(AuditEventTypes.OBJECT_CREATED)
                    .actorType(ActorType.ADMIN)
                    .actorId(cmd.actorId())
                    .action("DISTRIBUTE")
                    .bizType(FirstChainObjectType.LEAD.getKey())
                    .bizId(cmd.leadKey())
                    .bizVersion("0")
                    .result(AuditResult.SUCCESS)
                    .tenantId(tenantId)
                    .build());
            return id;
        });
    }

    // ========== PILOT-REQ-006：负责人分配（DISTRIBUTED→ASSIGNED）/ 员工领取（ASSIGNED→FOLLOWING）/ 改派 ==========

    /**
     * 负责人分配线索至本组织员工（DISTRIBUTED→ASSIGNED）。
     *
     * <p>「负责人只能分配本组织人员」（PILOT-REQ-006）：目标员工归属经 {@link UserOrgChecker} 校验；
     * 并发分配/领取的唯一性由执行器条件 UPDATE 保证（0 行读回分类）。
     */
    public void assign(Long leadId, Long targetUserId, long expectedVersion, String actorId) {
        Long tenantId = requireTenantId();
        FirstChainStateMachines.assertAllowed(FirstChainObjectType.LEAD, LeadStatus.DISTRIBUTED.name(),
                LeadStatus.ASSIGNED.name());
        transactionTemplate.executeWithoutResult(status -> {
            Long orgId = requireLeadOrgId(tenantId, leadId);
            if (!userOrgChecker.isUserInOrg(tenantId, targetUserId, orgId)) {
                throw exception(LEAD_ASSIGN_TARGET_NOT_IN_ORG);
            }
            stateTransitionExecutor.transition(tenantId, FirstChainObjectType.LEAD, leadId,
                    LeadStatus.DISTRIBUTED.name(), LeadStatus.ASSIGNED.name(), expectedVersion, actorId);
            jdbcTemplate.update("UPDATE bpm_first_chain_lead SET assignee_user_id = ? "
                    + "WHERE tenant_id = ? AND id = ? AND deleted = FALSE", targetUserId, tenantId, leadId);
            auditPort.record(AuditEventMessage.builder()
                    .eventType(AuditEventTypes.OBJECT_UPDATED)
                    .actorType(ActorType.ADMIN)
                    .actorId(actorId)
                    .action("ASSIGN")
                    .bizType(FirstChainObjectType.LEAD.getKey())
                    .bizId(String.valueOf(leadId))
                    .bizVersion(String.valueOf(expectedVersion + 1))
                    .result(AuditResult.SUCCESS)
                    .tenantId(tenantId)
                    .build());
        });
    }

    /**
     * 员工领取线索（ASSIGNED→FOLLOWING）。
     *
     * <p>「员工只能领取分配给自己的线索」（PILOT-REQ-006）：非被分配人领取即拒；
     * 并发领取结果唯一（条件 UPDATE——两事务同抢，失败方按读回分类显式冲突）。
     */
    public void claim(Long leadId, Long userId, long expectedVersion, String actorId) {
        Long tenantId = requireTenantId();
        FirstChainStateMachines.assertAllowed(FirstChainObjectType.LEAD, LeadStatus.ASSIGNED.name(),
                LeadStatus.FOLLOWING.name());
        Map<String, Object> lead = requireLead(tenantId, leadId);
        Long assignee = lead.get("assignee_user_id") == null ? null
                : ((Number) lead.get("assignee_user_id")).longValue();
        if (assignee == null || !assignee.equals(userId)) {
            throw exception(LEAD_CLAIM_NOT_ASSIGNEE);
        }
        transactionTemplate.executeWithoutResult(status -> {
            stateTransitionExecutor.transition(tenantId, FirstChainObjectType.LEAD, leadId,
                    LeadStatus.ASSIGNED.name(), LeadStatus.FOLLOWING.name(), expectedVersion, actorId);
            auditPort.record(AuditEventMessage.builder()
                    .eventType(AuditEventTypes.OBJECT_UPDATED)
                    .actorType(ActorType.ADMIN)
                    .actorId(actorId)
                    .action("CLAIM")
                    .bizType(FirstChainObjectType.LEAD.getKey())
                    .bizId(String.valueOf(leadId))
                    .bizVersion(String.valueOf(expectedVersion + 1))
                    .result(AuditResult.SUCCESS)
                    .tenantId(tenantId)
                    .build());
        });
    }

    /**
     * 负责人改派（ASSIGNED 域内更换分配员工，状态不变——D-07 M6「负责人可改派」；
     * M9 异常条件①：员工停用/离职线索保留原分配，由负责人显式改派，不自动归还）。
     */
    public void reassign(Long leadId, Long newAssigneeUserId, long expectedVersion, String actorId) {
        Long tenantId = requireTenantId();
        transactionTemplate.executeWithoutResult(status -> {
            Long orgId = requireLeadOrgId(tenantId, leadId);
            if (!userOrgChecker.isUserInOrg(tenantId, newAssigneeUserId, orgId)) {
                throw exception(LEAD_REASSIGN_TARGET_NOT_IN_ORG);
            }
            // 条件更新：钉住 ASSIGNED 域内 + 版本乐观锁 + 租户过滤；0 行读回分类（版本/状态）
            int updated = jdbcTemplate.update(
                    "UPDATE bpm_first_chain_lead SET assignee_user_id = ?, version = version + 1, updater = ?, "
                            + "update_time = CURRENT_TIMESTAMP WHERE tenant_id = ? AND id = ? AND status = ? "
                            + "AND version = ? AND deleted = FALSE",
                    newAssigneeUserId, actorId, tenantId, leadId, LeadStatus.ASSIGNED.name(), expectedVersion);
            if (updated == 0) {
                readBackAndClassify(tenantId, leadId, expectedVersion);
            }
            auditPort.record(AuditEventMessage.builder()
                    .eventType(AuditEventTypes.OBJECT_UPDATED)
                    .actorType(ActorType.ADMIN)
                    .actorId(actorId)
                    .action("REASSIGN")
                    .bizType(FirstChainObjectType.LEAD.getKey())
                    .bizId(String.valueOf(leadId))
                    .bizVersion(String.valueOf(expectedVersion + 1))
                    .result(AuditResult.SUCCESS)
                    .tenantId(tenantId)
                    .build());
        });
    }

    // ========== PILOT-REQ-007：跟进记录（M6 独立子对象；追加式——结构上无修改通道） ==========

    /**
     * 追加跟进记录（内容 F1 循 D-12 A 类挂级；仅被分配员工本人可提交；
     * 状态守卫：仅 FOLLOWING（领取后跟进，M6「分配→领取→跟进」流转）；
     * 终态（CONVERTED/INVALID）拒绝——终态锁定延伸）。
     *
     * @return 跟进记录行 ID
     */
    public Long followup(Long leadId, FollowupCmd cmd) {
        Long tenantId = requireTenantId();
        if (cmd.followupTime() == null) {
            throw exception(FOLLOWUP_TIME_REQUIRED);
        }
        return transactionTemplate.execute(status -> {
            Map<String, Object> lead = requireLead(tenantId, leadId);
            String currentStatus = (String) lead.get("status");
            if (!LeadStatus.FOLLOWING.name().equals(currentStatus)) {
                if (FirstChainStateMachines.isTerminal(FirstChainObjectType.LEAD, currentStatus)) {
                    throw exception(FOLLOWUP_LEAD_TERMINAL, currentStatus);
                }
                throw exception(LEAD_STATE_CONFLICT, currentStatus);
            }
            Long assignee = lead.get("assignee_user_id") == null ? null
                    : ((Number) lead.get("assignee_user_id")).longValue();
            if (assignee == null || !assignee.equals(cmd.followupUserId())) {
                throw exception(FOLLOWUP_NOT_ALLOWED_FOR_ACTOR);
            }
            jdbcTemplate.update(
                    "INSERT INTO bpm_first_chain_followup (lead_id, content, next_step, followup_user_id, "
                            + "followup_time, tenant_id, creator, updater) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    leadId, cmd.content(), cmd.nextStep(), cmd.followupUserId(), cmd.followupTime(),
                    tenantId, cmd.actorId(), cmd.actorId());
            Long followupId = jdbcTemplate.queryForObject(
                    "SELECT MAX(id) FROM bpm_first_chain_followup WHERE tenant_id = ? AND lead_id = ?",
                    Long.class, tenantId, leadId);
            auditPort.record(AuditEventMessage.builder()
                    .eventType(AuditEventTypes.OBJECT_CREATED)
                    .actorType(ActorType.ADMIN)
                    .actorId(cmd.actorId())
                    .action("FOLLOWUP")
                    .bizType(FirstChainObjectType.LEAD.getKey())
                    .bizId(String.valueOf(leadId))
                    .bizVersion(String.valueOf(lead.get("version")))
                    .result(AuditResult.SUCCESS)
                    .tenantId(tenantId)
                    .build());
            return followupId;
        });
    }

    // ========== PILOT-REQ-008：转商机（FOLLOWING→CONVERTED）/ 无效关闭（FOLLOWING→INVALID） ==========

    /**
     * 转有效商机（M7 前置：≥1 条跟进 + 客户姓名非空 + FOLLOWING；商机创建并双向引用线索；
     * CONVERTED 终态锁定；每线索至多一个有效商机——uk(lead_id) 兜底并发）。
     *
     * @return 商机行 ID
     */
    public Long convertToOpportunity(Long leadId, String oppKey, long expectedVersion, String actorId) {
        Long tenantId = requireTenantId();
        FirstChainStateMachines.assertAllowed(FirstChainObjectType.LEAD, LeadStatus.FOLLOWING.name(),
                LeadStatus.CONVERTED.name());
        return transactionTemplate.execute(status -> {
            Map<String, Object> lead = requireLead(tenantId, leadId);
            if (StrUtil.isBlank((String) lead.get("customer_name"))) {
                throw exception(LEAD_CONVERT_REQUIRE_CUSTOMER_NAME);
            }
            Integer followupCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM bpm_first_chain_followup WHERE tenant_id = ? AND lead_id = ? "
                            + "AND deleted = FALSE",
                    Integer.class, tenantId, leadId);
            if (followupCount == null || followupCount < 1) {
                throw exception(LEAD_CONVERT_REQUIRE_FOLLOWUP);
            }
            stateTransitionExecutor.transition(tenantId, FirstChainObjectType.LEAD, leadId,
                    LeadStatus.FOLLOWING.name(), LeadStatus.CONVERTED.name(), expectedVersion, actorId);
            try {
                jdbcTemplate.update(
                        "INSERT INTO bpm_first_chain_opportunity (opp_key, lead_id, customer_name, status, "
                                + "version, tenant_id, creator, updater) VALUES (?, ?, ?, ?, 0, ?, ?, ?)",
                        oppKey, leadId, lead.get("customer_name"), OpportunityStatus.OPEN.name(),
                        tenantId, actorId, actorId);
            } catch (org.springframework.dao.DuplicateKeyException concurrentDuplicate) {
                // 并发转商机：uk(lead_id)（每线索单商机）与 uk(tenant, opp_key) 兜底，转译为业务错误码
                throw exception(OPPORTUNITY_ALREADY_EXISTS);
            }
            Long opportunityId = jdbcTemplate.queryForObject(
                    "SELECT id FROM bpm_first_chain_opportunity WHERE tenant_id = ? AND opp_key = ? AND deleted = FALSE",
                    Long.class, tenantId, oppKey);
            auditPort.record(AuditEventMessage.builder()
                    .eventType(AuditEventTypes.OBJECT_UPDATED)
                    .actorType(ActorType.ADMIN)
                    .actorId(actorId)
                    .action("CONVERT")
                    .bizType(FirstChainObjectType.LEAD.getKey())
                    .bizId(String.valueOf(leadId))
                    .bizVersion(String.valueOf(expectedVersion + 1))
                    .result(AuditResult.SUCCESS)
                    .tenantId(tenantId)
                    .build());
            return opportunityId;
        });
    }

    /**
     * 无效关闭（M7：原因枚举必填〔无法联系/预算不符/非目标客户/重复线索/其他+说明〕，
     * OTHER 时说明必填；INVALID 终态锁定；员工可发起、负责人可代操作，一期不设审批环节）。
     */
    public void invalidate(Long leadId, String reasonName, String reasonDetail, long expectedVersion, String actorId) {
        Long tenantId = requireTenantId();
        if (StrUtil.isBlank(reasonName) || LeadInvalidateReason.of(reasonName) == null) {
            throw exception(LEAD_INVALIDATE_REASON_REQUIRED);
        }
        LeadInvalidateReason reason = LeadInvalidateReason.of(reasonName);
        if (reason == LeadInvalidateReason.OTHER && StrUtil.isBlank(reasonDetail)) {
            throw exception(LEAD_INVALIDATE_REASON_DETAIL_REQUIRED);
        }
        FirstChainStateMachines.assertAllowed(FirstChainObjectType.LEAD, LeadStatus.FOLLOWING.name(),
                LeadStatus.INVALID.name());
        transactionTemplate.executeWithoutResult(status -> {
            stateTransitionExecutor.transition(tenantId, FirstChainObjectType.LEAD, leadId,
                    LeadStatus.FOLLOWING.name(), LeadStatus.INVALID.name(), expectedVersion, actorId);
            auditPort.record(AuditEventMessage.builder()
                    .eventType(AuditEventTypes.OBJECT_UPDATED)
                    .actorType(ActorType.ADMIN)
                    .actorId(actorId)
                    .action("INVALIDATE:" + reason.name())
                    .bizType(FirstChainObjectType.LEAD.getKey())
                    .bizId(String.valueOf(leadId))
                    .bizVersion(String.valueOf(expectedVersion + 1))
                    .result(AuditResult.SUCCESS)
                    .tenantId(tenantId)
                    .build());
        });
    }

    // ========== 查询（PILOT-REQ-009 服务端范围基座；完整分页 API 面随 FC-003 工作台落卡） ==========

    /**
     * 按归属组织列表（org 轴范围由调用方约束；SQL 层 {@code OrgDataPermissionRule} 注册后叠加静默过滤）。
     */
    public List<Map<String, Object>> listByOrg(Long orgId) {
        Long tenantId = requireTenantId();
        return jdbcTemplate.queryForList(
                "SELECT * FROM bpm_first_chain_lead WHERE tenant_id = ? AND org_id = ? AND deleted = FALSE ORDER BY id",
                tenantId, orgId);
    }

    /**
     * 按分配员工列表（员工「本人」范围；对象 ID 越权由详情入口 {@code requireLead} 的租户过滤兜底）。
     */
    public List<Map<String, Object>> listByAssignee(Long userId) {
        Long tenantId = requireTenantId();
        return jdbcTemplate.queryForList(
                "SELECT * FROM bpm_first_chain_lead WHERE tenant_id = ? AND assignee_user_id = ? AND deleted = FALSE "
                        + "ORDER BY id",
                tenantId, userId);
    }

    /**
     * 线索详情（租户内存在性判定；跨租户 ID → NOT_EXISTS 分类不泄露存在性）。
     */
    public Map<String, Object> getLead(Long leadId) {
        return requireLead(requireTenantId(), leadId);
    }

    /**
     * 分页查询（平台侧按归属组织 + 状态过滤；org 轴静默过滤见 {@code OrgDataPermissionRule} 注册）。
     */
    public PageResult<Map<String, Object>> page(Long orgId, String status, long pageNo, long pageSize) {
        Long tenantId = requireTenantId();
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM bpm_first_chain_lead WHERE tenant_id = ? AND deleted = FALSE "
                        + "AND (? IS NULL OR org_id = ?) AND (? IS NULL OR status = ?)",
                Long.class, tenantId, orgId, orgId, status, status);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM bpm_first_chain_lead WHERE tenant_id = ? AND deleted = FALSE "
                        + "AND (? IS NULL OR org_id = ?) AND (? IS NULL OR status = ?) ORDER BY id DESC LIMIT ? OFFSET ?",
                tenantId, orgId, orgId, status, status, pageSize, (pageNo - 1) * pageSize);
        return new PageResult<>(rows, total == null ? 0L : total);
    }

    // ========== 内部工具 ==========

    private Map<String, Object> requireLead(Long tenantId, Long leadId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM bpm_first_chain_lead WHERE tenant_id = ? AND id = ? AND deleted = FALSE",
                tenantId, leadId);
        if (rows.isEmpty()) {
            throw exception(LEAD_NOT_EXISTS);
        }
        return rows.get(0);
    }

    private Long requireLeadOrgId(Long tenantId, Long leadId) {
        return ((Number) requireLead(tenantId, leadId).get("org_id")).longValue();
    }

    /** 0 行读回分类（reassign 条件更新专用）：先比版本（VERSION_CONFLICT），再报状态（STATE_CONFLICT）。 */
    private void readBackAndClassify(Long tenantId, Long leadId, long expectedVersion) {
        Map<String, Object> lead = requireLead(tenantId, leadId);
        long actualVersion = ((Number) lead.get("version")).longValue();
        if (actualVersion != expectedVersion) {
            throw exception(LEAD_VERSION_CONFLICT, expectedVersion);
        }
        throw exception(LEAD_STATE_CONFLICT, String.valueOf(lead.get("status")));
    }

    private static Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw exception(LEAD_TENANT_REQUIRED);
        }
        return tenantId;
    }

    /**
     * 「用户是否属于组织」的对象级资格校验端口（服务层二次校验，接入合同 §1.6）。
     *
     * <p>生产装配 {@code FirstchainUserOrgChecker}（经 system 模块用户 API 实现真实归属判定）；
     * 测试以桩注入。接口默认方法 {@link #isUserInOrg} 的任何实现都不得缓存授权结论。
     */
    @FunctionalInterface
    public interface UserOrgChecker {

        boolean isUserInOrg(Long tenantId, Long userId, Long orgId);

    }

    // ========== 命令对象 ==========

    /**
     * 下发命令（PILOT-REQ-005）。orgId 由平台侧按目标加盟商组织显式传入，服务端写入归属列。
     */
    public record DistributeCmd(String leadKey, String customerName, String customerPhone, String customerWechat,
                                String customerAddress, String source, Long orgId, String actorId) {
    }

    /**
     * 跟进命令（PILOT-REQ-007）：content F1、followupTime 必填；followupUserId 须为线索被分配人。
     */
    public record FollowupCmd(String content, String nextStep, Long followupUserId, LocalDateTime followupTime,
                              String actorId) {
    }

    /**
     * 商机一期状态（M8 最小对象：创建即 OPEN 占位；状态机随阶段 3 报价域细化，
     * 不入 {@link FirstChainObjectType} 状态机管辖）。
     */
    public enum OpportunityStatus {
        OPEN
    }

}
