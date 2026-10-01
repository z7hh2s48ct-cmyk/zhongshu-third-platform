package cn.zszj.module.firstchain.service.application;

import cn.hutool.core.util.RandomUtil;
import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.framework.common.util.json.JsonUtils;
import cn.zszj.framework.common.util.object.BeanUtils;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.module.bpm.firstchain.FirstChainApplicationService;
import cn.zszj.module.bpm.firstchain.FirstChainApplicationService.CreateApplicationCmd;
import cn.zszj.module.bpm.firstchain.FirstChainObjectType;
import cn.zszj.module.bpm.firstchain.FirstChainProcessBindingService;
import cn.zszj.module.firstchain.controller.admin.application.vo.ApplicationCreateReqVO;
import cn.zszj.module.firstchain.controller.admin.application.vo.ApplicationPageReqVO;
import cn.zszj.module.firstchain.controller.admin.application.vo.ApplicationRespVO;
import cn.zszj.module.firstchain.controller.admin.application.vo.ApplicationSubmitReqVO;
import cn.zszj.module.firstchain.controller.admin.application.vo.ApplicationWithdrawReqVO;
import cn.zszj.module.firstchain.dal.dataobject.application.ApplicationDO;
import cn.zszj.module.firstchain.dal.mysql.application.FirstchainApplicationMapper;
import cn.zszj.module.firstchain.service.wiring.FirstchainNotifyWiringService;
import cn.zszj.module.infra.api.file.FileApi;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.module.bpm.enums.ErrorCodeConstants.FIRST_CHAIN_APP_KEY_EXISTS;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.FIRSTCHAIN_APPLICATION_NOT_EXISTS;
import static cn.zszj.module.firstchain.enums.ErrorCodeConstants.FIRSTCHAIN_TENANT_REQUIRED;

/**
 * 首链申请域应用服务（ZS-FC-001，PILOT-REQ-001 服务端；D-07 M3 状态入口 / M8 字段 / M10 流程发起）。
 *
 * <p>本类为 firstchain 模块 REST 面的薄门面：
 * <ul>
 *     <li>申请编号服务端生成（M1：业务键服务端生成、租户内唯一）——生成规则 {@code FC + yyyMMdd + '-' + 8 位
 *     随机串}，并发撞号由 bpm 域 uk(tenant_id, app_key) 显式分类后有限次重试；</li>
 *     <li>写侧显式租户上下文缺失即拒（接入合同 §1.3：不默认 0、不伪造归属）；操作人取登录上下文留痕；</li>
 *     <li>状态写路径全部委托 bpm 域层 {@link FirstChainApplicationService}（幂等命令门与版本条件迁移在域层
 *     内聚，本层不做双写、不重复校验）；</li>
 *     <li>查询走 {@link FirstchainApplicationMapper}（MyBatis Plus 租户插件自动过滤，跨租户 0 行即
 *     NOT_EXISTS 不静默）。</li>
 * </ul>
 *
 * @author ZS-FC-001
 */
@Slf4j
@Service
public class FirstchainApplicationService {

    /** 申请编号前缀（firstchain 加盟商申请域业务键） */
    private static final String APP_KEY_PREFIX = "FC";

    /** 申请编号随机段长度（撞号概率 36^8，uk 命中重试上限内不可达） */
    private static final int APP_KEY_RANDOM_LENGTH = 8;

    /** 申请编号撞号重试上限（uk(tenant_id, app_key) 冲突时的服务端重新生成次数） */
    private static final int APP_KEY_MAX_RETRY = 3;

    @Resource
    private FirstChainApplicationService firstChainApplicationService;

    @Resource
    private FirstChainProcessBindingService processBindingService;

    @Resource
    private FirstchainNotifyWiringService notifyWiringService;

    @Resource
    private FirstchainApplicationMapper applicationMapper;

    @Resource
    private FileApi fileApi;

    /**
     * 创建申请（PILOT-REQ-001：草稿基线 DRAFT；申请编号服务端生成，租户内唯一）。
     *
     * <p>资质附件只存 fileId 引用（接入合同 §1.10），写入前经 {@link FileApi#validatePrivateFileReferences}
     * 校验——存在于当前租户、{@code scope=PRIVATE}、非导出件/删除中，且提交人对其具备读取资格；任一不满足整单拒绝，
     * 申请行不落库（先校验后落库，非法引用不入库）。重复编号去重保序。读取侧仍由文件域授权与票据交付把关。
     *
     * @return 申请行 ID
     */
    public Long createApplication(ApplicationCreateReqVO createReqVO) {
        Long tenantId = requireTenantId();
        String actorId = currentActorId();
        List<Long> attachmentFileIds = distinctFileIds(createReqVO.getAttachmentFileIds());
        if (!attachmentFileIds.isEmpty()) {
            fileApi.validatePrivateFileReferences(attachmentFileIds);
        }
        // M1：申请编号服务端生成；bpm 域 uk(tenant, app_key) 显式分类撞号 → 有限次重试后仍失败即抛
        ServiceException lastConflict = null;
        for (int attempt = 0; attempt < APP_KEY_MAX_RETRY; attempt++) {
            try {
                return firstChainApplicationService.createApplication(CreateApplicationCmd.builder()
                        .appKey(generateAppKey())
                        .applicantName(createReqVO.getApplicantName())
                        .contactName(createReqVO.getContactName())
                        .contactPhone(createReqVO.getContactPhone())
                        .attachmentFileIds(serializeFileIds(attachmentFileIds))
                        .actorId(actorId)
                        .build());
            } catch (ServiceException conflict) {
                if (!FIRST_CHAIN_APP_KEY_EXISTS.getCode().equals(conflict.getCode())) {
                    throw conflict;
                }
                lastConflict = conflict;
                log.info("[createApplication][申请编号撞号重试：attempt={}]", attempt + 1);
            }
        }
        throw lastConflict;
    }

    /**
     * 提交申请（DRAFT→SUBMITTED，M3；同事务发起 Flowable 单节点审批流 + 绑定，M10=B）。
     *
     * <p>审批人取值：请求显式指定优先（M2：审批人=任意 PLATFORM 有效任职）；缺省为提交人
     * （M2 双人分离后置登记——提交人与审批人同员为试点简化，审批资格的对象级校验随审批入口二次把关）。
     * 接线（ZS-FC-003）：审批待办注册 + 审批提醒通知随本事务落位（外层 @Transactional 使 Outbox
     * MANDATORY 合同可达；bpm 域事务模板 REQUIRED 加入本事务）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void submitApplication(ApplicationSubmitReqVO submitReqVO) {
        requireTenantId();
        Long approverUserId = submitReqVO.getApproverUserId() != null
                ? submitReqVO.getApproverUserId()
                : SecurityFrameworkUtils.getLoginUserId();
        ApplicationDO application = applicationMapper.selectById(submitReqVO.getId());
        firstChainApplicationService.submitApplication(submitReqVO.getId(), submitReqVO.getExpectedVersion(),
                approverUserId, currentActorId());
        wireSubmission(application, submitReqVO.getId(), approverUserId);
    }

    /** 提交后接线（同事务）：读回绑定取流程实例（待办键组成部分），注册审批待办 + 审批提醒通知。 */
    private void wireSubmission(ApplicationDO application, Long applicationId, Long approverUserId) {
        if (application == null) {
            return; // bpm 域已对不存在/不可见申请 fail-closed，此处只防脏读路径
        }
        Map<String, Object> binding = processBindingService.findLatestByDomain(
                TenantContextHolder.getTenantId(), FirstChainObjectType.APPLICATION, applicationId);
        if (binding == null || binding.get("process_instance_id") == null) {
            log.warn("[wireSubmission][审批绑定缺失，跳过待办/通知接线（可观测不静默）：applicationId={}]", applicationId);
            return;
        }
        notifyWiringService.onApplicationSubmitted(application.getAppKey(), applicationId,
                String.valueOf(binding.get("process_instance_id")),
                ((Number) application.getVersion()).longValue() + 1, application.getApplicantName(),
                approverUserId, currentActorId());
    }

    /**
     * 撤回审批流（M3：领域状态不变仍 SUBMITTED，仅解绑流程实例，可重新发起）。
     * 接线（ZS-FC-003）：审批待办经 Outbox 事件撤销（WITHDRAW，事件驱动不双写）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void withdrawApproval(ApplicationWithdrawReqVO withdrawReqVO) {
        requireTenantId();
        ApplicationDO application = applicationMapper.selectById(withdrawReqVO.getId());
        firstChainApplicationService.withdrawApproval(withdrawReqVO.getId(), withdrawReqVO.getExpectedVersion(),
                currentActorId());
        if (application == null) {
            return;
        }
        Map<String, Object> binding = processBindingService.findLatestByDomain(
                TenantContextHolder.getTenantId(), FirstChainObjectType.APPLICATION, withdrawReqVO.getId());
        if (binding == null || binding.get("process_instance_id") == null) {
            log.warn("[withdrawApproval][审批绑定缺失，跳过待办撤销接线：applicationId={}]", withdrawReqVO.getId());
            return;
        }
        notifyWiringService.onApprovalWithdrawn(application.getAppKey(), withdrawReqVO.getId(),
                String.valueOf(binding.get("process_instance_id")),
                ((Number) application.getVersion()).longValue() + 1, currentActorId());
    }

    /**
     * 分页查询（租户过滤自动施加；跨租户不可见）。
     */
    public PageResult<ApplicationRespVO> getApplicationPage(ApplicationPageReqVO pageReqVO) {
        PageResult<ApplicationDO> pageResult = applicationMapper.selectPage(pageReqVO);
        return new PageResult<>(convertList(pageResult.getList()), pageResult.getTotal());
    }

    /**
     * 详情查询（跨租户 0 行即 NOT_EXISTS，不静默）。
     */
    public ApplicationRespVO getApplication(Long id) {
        ApplicationDO application = applicationMapper.selectById(id);
        if (application == null) {
            throw exception(FIRSTCHAIN_APPLICATION_NOT_EXISTS);
        }
        return convert(application);
    }

    // ========== 内部 ==========

    private static List<ApplicationRespVO> convertList(List<ApplicationDO> list) {
        return list.stream().map(FirstchainApplicationService::convert).toList();
    }

    private static ApplicationRespVO convert(ApplicationDO application) {
        ApplicationRespVO respVO = BeanUtils.toBean(application, ApplicationRespVO.class);
        // BeanUtils 不做 String→List 跨型拷贝，附件引用从原始列值补齐
        respVO.setAttachmentFileIds(parseFileIds(application.getAttachmentFileIds()));
        return respVO;
    }

    /**
     * 附件 fileIds 出参转换：列值为 bpm 域写入的 JSON 数组串（如 {@code [101,102]}），解析为
     * {@code List<Long>} 出参（FILE 私有：只出引用编号，票据下载走 FileDeliveryService 既有设施）。
     * 解析失败（历史脏数据）降级为 null 不阻断查询，原串仍可经审计侧回查。
     */
    static List<Long> parseFileIds(String attachmentFileIds) {
        if (attachmentFileIds == null || attachmentFileIds.isBlank()) {
            return null;
        }
        try {
            return JsonUtils.parseArray(attachmentFileIds, Long.class);
        } catch (Exception malformed) {
            log.warn("[parseFileIds][资质附件列值非 JSON 数组，出参降级为 null：value={}]", attachmentFileIds);
            return null;
        }
    }

    /** 附件编号去重保序（null 视为无附件；null 元素原样保留，交由文件域校验拒绝，不在此静默丢弃）。 */
    private static List<Long> distinctFileIds(List<Long> fileIds) {
        return fileIds == null ? List.of() : new ArrayList<>(new LinkedHashSet<>(fileIds));
    }

    private static String serializeFileIds(List<Long> fileIds) {
        return fileIds == null || fileIds.isEmpty() ? null : JsonUtils.toJsonString(fileIds);
    }

    /**
     * 申请编号生成（M1：服务端生成、租户内唯一）：{@code FC + yyyyMMdd + '-' + 8 位随机大写串}。
     */
    private static String generateAppKey() {
        return APP_KEY_PREFIX + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + RandomUtil.randomStringUpper(APP_KEY_RANDOM_LENGTH);
    }

    private static Long requireTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw exception(FIRSTCHAIN_TENANT_REQUIRED);
        }
        return tenantId;
    }

    private static String currentActorId() {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        return loginUserId == null ? null : String.valueOf(loginUserId);
    }

}
