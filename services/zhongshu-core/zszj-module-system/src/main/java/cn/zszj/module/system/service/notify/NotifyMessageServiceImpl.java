package cn.zszj.module.system.service.notify;

import cn.zszj.framework.common.biz.system.permission.PermissionCommonApi;
import cn.zszj.framework.common.biz.system.permission.dto.CrossOrgVisitDecisionDTO;
import cn.zszj.framework.common.biz.system.permission.dto.OrgDataPermissionRespDTO;
import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.security.core.util.CrossOrgVisitScopeHolder;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import cn.zszj.module.system.controller.admin.notify.vo.message.NotifyMessageMyPageReqVO;
import cn.zszj.module.system.controller.admin.notify.vo.message.NotifyMessagePageReqVO;
import cn.zszj.module.system.dal.dataobject.notify.NotifyMessageDO;
import cn.zszj.module.system.dal.dataobject.notify.NotifyTemplateDO;
import cn.zszj.module.system.dal.mysql.notify.NotifyMessageMapper;
import cn.zszj.module.system.dal.mysql.notify.NotifyMessageMapper.OrgScope;
import cn.zszj.module.system.service.membership.MembershipContextResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 站内信 Service 实现类
 *
 * <p>ZS-MSG-003.C：读路径按 org 轴可见范围过滤（管理分页/我的分页/未读列表/未读计数），单条正文经
 * {@link NotifyMessageOrgAuthorizer} 对象门裁决；写入归属 = 派发时刻收件人服务端组织上下文
 * （默认任职组织，ZS-IAM-002），任职不可用降级为空归属不阻断派发。
 *
 * @author xrcoder
 */
@Service
@Validated
@Slf4j
public class NotifyMessageServiceImpl implements NotifyMessageService {

    /** 未读列表单次拉取条数上限（ZS-MSG-003：与控制器 @Max(100) 同源，防止绕过入口全量拉取） */
    static final int UNREAD_LIST_MAX_SIZE = 100;

    /** 未读列表默认条数（与控制器 defaultValue=10 同源） */
    static final int UNREAD_LIST_DEFAULT_SIZE = 10;

    @Resource
    private NotifyMessageMapper notifyMessageMapper;

    /** org 轴对象级门（ZS-MSG-003.C）：单条正文读取的统一授权裁决。 */
    @Resource
    private NotifyMessageOrgAuthorizer notifyMessageOrgAuthorizer;

    /**
     * 服务端组织上下文解析器（ZS-IAM-002）：派发时刻收件人 org 归属来源。
     * {@code required=false}——最小测试上下文不因缺 bean 破坏装配，此时归属降级为空。
     */
    @Autowired(required = false)
    private MembershipContextResolver membershipContextResolver;

    /**
     * org 轴范围门面（ZS-PERM-002.B）：读路径过滤范围来源。
     * {@code required=false}——未装配时 fail-closed（仅本人无组织列消息），见 {@link #resolveOrgFilter}。
     */
    @Autowired(required = false)
    private PermissionCommonApi permissionApi;

    @Override
    public Long createNotifyMessage(Long userId, Integer userType,
                                    NotifyTemplateDO template, String templateContent, Map<String, Object> templateParams) {
        NotifyMessageDO message = new NotifyMessageDO().setUserId(userId).setUserType(userType)
                .setOrganizationId(resolveRecipientOrganizationId(userType, userId)) // ZS-MSG-003.C：派发时刻收件人服务端组织上下文
                .setTemplateId(template.getId()).setTemplateCode(template.getCode())
                .setTemplateType(template.getType()).setTemplateNickname(template.getNickname())
                .setTemplateContent(templateContent).setTemplateParams(templateParams).setReadStatus(false);
        notifyMessageMapper.insert(message);
        return message.getId();
    }

    /**
     * 解析消息 org 归属 = 派发时刻收件人的服务端组织上下文（默认任职组织，ZS-IAM-002）。
     *
     * <p><b>仅 ADMIN 命名空间参与任职解析</b>：任职表 {@code MembershipDO.userId} 关联 {@code system_users.id}，
     * 会员（MEMBER）编号空间独立、可同号碰撞——若对 MEMBER 误查任职，会同号错配到某管理员的组织，
     * 故 MEMBER 收件人归属恒空（会员侧读写均命中非 ADMIN 护栏，不受 org 轴约束）。
     *
     * <p>任职不可用（无任职 → 空上下文；离职/未生效/过期/组织停用 → 业务异常）一律降级为空归属，
     * 不阻断派发（消息可达性与组织归属解耦；历史/系统消息同为 NULL，仍由 tenant 轴治理）。
     */
    private Long resolveRecipientOrganizationId(Integer userType, Long userId) {
        // 任职体系仅管理员命名空间：MEMBER 消息不解任职上下文（防同号管理员错配）
        if (!UserTypeEnum.ADMIN.getValue().equals(userType)) {
            return null;
        }
        if (membershipContextResolver == null) {
            return null;
        }
        try {
            return membershipContextResolver.resolve(userId).getOrgId();
        } catch (ServiceException e) {
            log.info("[resolveRecipientOrganizationId][收件人({}) 服务端组织上下文不可用：{}，org 归属降级为空]",
                    userId, e.getMessage());
            return null;
        }
    }

    @Override
    public PageResult<NotifyMessageDO> getNotifyMessagePage(NotifyMessagePageReqVO pageReqVO) {
        return notifyMessageMapper.selectPage(pageReqVO, resolveOrgFilter(false));
    }

    @Override
    public PageResult<NotifyMessageDO> getMyMyNotifyMessagePage(NotifyMessageMyPageReqVO pageReqVO, Long userId, Integer userType) {
        return notifyMessageMapper.selectPage(pageReqVO, userId, userType, resolveOrgFilter(true));
    }

    @Override
    public NotifyMessageDO getNotifyMessage(Long id) {
        NotifyMessageDO message = notifyMessageMapper.selectById(id);
        if (message != null && !notifyMessageOrgAuthorizer.isObjectAllowed(message)) {
            // 越界即伪装不存在（防存在性探测；与落点 NOT_FOUND 对外口径一致）
            log.info("[getNotifyMessage][消息({}) org 归属({}) 不在当前主体授权范围，按不存在返回]",
                    id, message.getOrganizationId());
            return null;
        }
        return message;
    }

    @Override
    public List<NotifyMessageDO> getUnreadNotifyMessageList(Long userId, Integer userType, Integer size) {
        return notifyMessageMapper.selectUnreadListByUserIdAndUserType(userId, userType,
                normalizeUnreadSize(size), resolveOrgFilter(true));
    }

    /**
     * 防御性收敛未读列表条数（ZS-MSG-003「限制未读列表 size」）：null/非正 → 默认 10，
     * 超上限 → 截断到 {@link #UNREAD_LIST_MAX_SIZE}。不信任调用方（HTTP 入口另有
     * @Min/@Max 显式 400；内部调用无校验链，须在此兜底，避免全量拉取未读）。
     */
    static int normalizeUnreadSize(Integer size) {
        if (size == null || size <= 0) {
            return UNREAD_LIST_DEFAULT_SIZE;
        }
        return Math.min(size, UNREAD_LIST_MAX_SIZE);
    }

    @Override
    public Long getUnreadNotifyMessageCount(Long userId, Integer userType) {
        return notifyMessageMapper.selectUnreadCountByUserIdAndUserType(userId, userType, resolveOrgFilter(true));
    }

    /**
     * 标记消息已读（ZS-MSG-003.C 判据：写路径不施 org 门）。
     *
     * <p>docs/05 line709 既定口径：SQL 已按「本人 + userType」约束（{@code Mapper#updateListRead}），
     * 不返回消息内容、无跨用户越权面，故不叠加 org 范围收窄（授权收缩后本人收件箱状态仍可维护）。
     */
    @Override
    public int updateNotifyMessageRead(Collection<Long> ids, Long userId, Integer userType) {
        return notifyMessageMapper.updateListRead(ids, userId, userType);
    }

    @Override
    public int updateAllNotifyMessageRead(Long userId, Integer userType) {
        return notifyMessageMapper.updateListRead(userId, userType);
    }

    /**
     * 解析当前请求的 org 轴查询范围（ZS-MSG-003.C，判据与 {@link NotifyMessageOrgAuthorizer} 同构）：
     * <ol>
     *   <li><b>visit 上下文优先</b>（SEC-001.B D7 统一入口）：获批跨组织访问时收敛到授权
     *       {@code targetOrgIds}，无组织列消息一并隐藏（限定授权下不可回落到 home 组织范围）；
     *       whole-tenant 授权（{@code targetOrgIds=null}）不受限；</li>
     *   <li><b>护栏</b>：无登录用户（系统内部/任务/供给）与非 ADMIN 主体不施加 org 范围限制
     *       （与 {@code OrgDataPermissionChecker} 同口径）；</li>
     *   <li><b>fail-closed</b>：未装配范围门面 / 无授权组织 → 仅本人无组织列消息
     *       （{@code includeNullOrg=false} 的管理路径则恒假）；</li>
     *   <li>全部组织授权（超管/平台任职）→ 不受限。</li>
     * </ol>
     *
     * @param includeNullOrg 是否放行无组织列消息：「我的」路径 true（本人兜底），管理路径 false
     */
    private OrgScope resolveOrgFilter(boolean includeNullOrg) {
        // 1. visit 上下文优先（SEC-001.B D7 统一入口）
        CrossOrgVisitDecisionDTO visitScope = CrossOrgVisitScopeHolder.getScope();
        if (visitScope != null && visitScope.isAuthorized()) {
            return visitScope.getTargetOrgIds() == null ? OrgScope.unrestricted()
                    : OrgScope.of(visitScope.getTargetOrgIds(), false);
        }
        // 2. 护栏：无登录/非 ADMIN 不施加 org 范围限制（与 OrgDataPermissionChecker 护栏同口径）
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser == null || !Objects.equals(loginUser.getUserType(), UserTypeEnum.ADMIN.getValue())) {
            return OrgScope.unrestricted();
        }
        // 3. 未装配范围门面（最小测试上下文）→ fail-closed
        OrgDataPermissionRespDTO permission = permissionApi == null ? null
                : permissionApi.getOrgDataPermission(loginUser.getId());
        if (permission == null) {
            return OrgScope.of(Collections.emptySet(), includeNullOrg);
        }
        // 4. 全部组织授权 → 不受限
        if (Boolean.TRUE.equals(permission.getAll())) {
            return OrgScope.unrestricted();
        }
        // 5. 授权组织集合（可能为空 = 无有效任职，落本人无组织列兜底）
        return OrgScope.of(permission.getOrgIds() != null ? permission.getOrgIds() : Collections.emptySet(),
                includeNullOrg);
    }

}
