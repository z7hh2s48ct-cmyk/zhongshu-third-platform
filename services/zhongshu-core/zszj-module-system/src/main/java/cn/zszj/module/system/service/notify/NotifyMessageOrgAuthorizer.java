package cn.zszj.module.system.service.notify;

import cn.zszj.framework.common.biz.system.permission.dto.CrossOrgVisitDecisionDTO;
import cn.zszj.framework.datapermission.core.rule.org.OrgDataPermissionChecker;
import cn.zszj.framework.security.core.util.CrossOrgVisitScopeHolder;
import cn.zszj.module.system.dal.dataobject.notify.NotifyMessageDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 站内消息 org 轴对象级授权器（ZS-MSG-003.C）。
 *
 * <p>消息列表（我的/管理）、消息正文（单条读取）与消息落点解析的<b>统一对象门</b>：判定消息的
 * {@link NotifyMessageDO#getOrganizationId() org 归属} 是否落在当前主体的可见范围。落实主卡
 * docs/05 line710「覆盖消息列表/正文/管理查询的范围」与 line711「旧消息在组织/权限变化后不得越界可见」。
 *
 * <p>判据（与 ZS-SEC-001.B 跨组织访问、ZS-PERM-002.B org 轴检查器同构，fail-closed）：
 * <ol>
 *   <li><b>visit 上下文优先</b>：获批跨组织访问时按 {@link CrossOrgVisitScopeHolder#isObjectAllowed(Long)}
 *       收敛到授权 targetOrgIds（whole-tenant 授权不受限；限定授权下无组织列消息拒绝）；
 *       不叠加 home 组织的 org 范围（visit 获批即代表目标租户内的受控范围）；</li>
 *   <li>否则经产线同构的 {@link OrgDataPermissionChecker} 裁决：授权组织集合命中放行；无组织列消息
 *       （orgId=null）按「负责人为登录用户」本人兜底（D-09 FND-AUTH-004：对象一旦归属某组织，
 *       本人所有权不凌驾组织排除）；</li>
 *   <li>护栏与检查器一致：无登录用户（系统内部/供给/任务）与非 ADMIN 类型不施加组织范围限制；
 *       未装配检查器的测试上下文 fail-closed（仅无组织列消息放行）。</li>
 * </ol>
 *
 * @author ZS-MSG-003.C
 */
@Component
public class NotifyMessageOrgAuthorizer {

    /**
     * org 轴对象级检查入口（ZS-PERM-002.B；产线由 {@code ZszjDeptDataPermissionAutoConfiguration} 装配）。
     * {@code required=false}——未装配 biz-data-permission 的测试上下文不因缺 bean 破坏装配；
     * 此时按 fail-closed 处理（仅无组织列消息放行，见 {@link #isObjectAllowed}）。
     */
    @Autowired(required = false)
    private OrgDataPermissionChecker orgDataPermissionChecker;

    /**
     * 判定消息对象是否在当前主体的可见范围内。
     *
     * @param message 消息 DO（org 归属 = {@link NotifyMessageDO#getOrganizationId()}；负责人 = userId）
     * @return 是否可见
     */
    public boolean isObjectAllowed(NotifyMessageDO message) {
        // 1. 获批 visit 上下文优先（SEC-001.B D7 入口）：按授权 targetOrgIds 收敛
        CrossOrgVisitDecisionDTO visitScope = CrossOrgVisitScopeHolder.getScope();
        if (visitScope != null && visitScope.isAuthorized()) {
            return CrossOrgVisitScopeHolder.isObjectAllowed(message.getOrganizationId());
        }
        // 2. 未装配检查器的测试上下文：fail-closed，仅无组织列消息放行
        if (orgDataPermissionChecker == null) {
            return message.getOrganizationId() == null;
        }
        // 3. 产线同构 org 轴裁决（授权组织命中 / 无组织列消息本人兜底）
        return orgDataPermissionChecker.isObjectVisible(message.getOrganizationId(), message.getUserId());
    }

}
