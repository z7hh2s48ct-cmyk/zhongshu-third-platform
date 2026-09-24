package cn.zszj.module.system.service.permission;

import cn.zszj.framework.common.biz.system.permission.dto.OrgDataPermissionRespDTO;
import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.module.system.dal.dataobject.membership.MembershipDO;
import cn.zszj.module.system.dal.dataobject.organization.OrganizationDO;
import cn.zszj.module.system.dal.mysql.membership.MembershipMapper;
import cn.zszj.module.system.dal.mysql.organization.OrganizationMapper;
import cn.zszj.module.system.enums.membership.MembershipStatusEnum;
import cn.zszj.module.system.enums.organization.OrganizationTypeEnum;
import cn.zszj.module.system.enums.permission.OrgDataScopeEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 组织级（跨组织）数据范围解析器（org 轴，ZS-PERM-002.B）。
 *
 * <p>落实 D-09 一期口径（docs/02 §3 / §5.3 PEND-003 采纳 A）：<b>跨组织只允许显式平台角色</b>，不发明临时跨组织授权。
 * 由 {@code PermissionServiceImpl#getOrgDataPermission}（已 {@code @DataPermission(enable = false)}）委托调用，
 * <b>只读消费</b>组织/任职 Mapper，不触碰 IAM-002/IAM-004 的写路径。ZS-PERM-001.B 起另经
 * {@link #resolveEffectiveOrgIds} 服务于授权写入的目标归属判定（获批跨组织 visit 上下文）。
 *
 * <p>范围派生规则：
 * <ol>
 *   <li>超级管理员 → {@link OrgDataScopeEnum#ORG_ALL}（全部组织）；</li>
 *   <li>存在 PLATFORM 类型组织的<b>有效任职</b> → {@link OrgDataScopeEnum#ORG_ALL}（显式平台角色，跨组织全量）；</li>
 *   <li>存在其它类型的有效任职 → {@link OrgDataScopeEnum#ORG_AND_CHILD}：本人各在职组织 + 其组织树后代并集
 *       （支持两组织合法互授），并置 {@code self=true} 兜底无组织列对象；</li>
 *   <li>无任何有效任职 → {@link OrgDataScopeEnum#ORG_SELF}：仅本人兜底（历史无组织账号兼容，与
 *       {@code MembershipContextResolver} 的降级空上下文一致）。</li>
 * </ol>
 *
 * <p>「有效任职」判定与 IAM-002 {@code MembershipContextResolver} <b>同口径</b>：状态 ACTIVE、valid_from 已到、
 * valid_to 未过、且所属组织存在并显式开启（{@link CommonStatusEnum#isEnable}）；任一不满足即排除（fail-closed）。
 *
 * @author ZS-PERM-002.B
 * @author ZS-PERM-001.B
 */
@Component
public class OrgDataScopeResolver {

    /**
     * 组织树下钻的最大深度（fail-safe）。visited 去重已保证终止性，此上限额外防御异常深的合法树导致性能劣化，
     * 与 IAM-002 组织成环校验的深度上限同范式。
     */
    private static final int MAX_DESCENDANT_DEPTH = 256;

    @Resource
    private MembershipMapper membershipMapper;

    @Resource
    private OrganizationMapper organizationMapper;

    /**
     * 解析登录主体的组织（跨组织）数据范围。
     *
     * @param userId     账号编号
     * @param superAdmin 是否超级管理员（由调用方以启用态超管角色判定后传入）
     * @return 组织数据范围
     */
    public OrgDataPermissionRespDTO resolve(Long userId, boolean superAdmin) {
        OrgDataPermissionRespDTO result = new OrgDataPermissionRespDTO();
        // 1. 超级管理员：全部组织
        if (superAdmin) {
            return allOrgs(result);
        }
        // 2. 账号编号非法：仅本人兜底（不阻断，与上下文解析器降级一致）
        if (userId == null || userId <= 0) {
            return selfOnly(result);
        }
        // 3. 收集有效任职组织；命中 PLATFORM 类型即显式平台角色
        EffectiveOrgs effective = collectEffectiveOrgs(userId);
        // 4. 显式平台角色 → 全部组织（D-09：跨组织只允许显式平台角色）
        if (effective.platform) {
            return allOrgs(result);
        }
        // 5. 无任何有效任职 → 仅本人兜底
        if (effective.orgIds.isEmpty()) {
            return selfOnly(result);
        }
        // 6. 本组织子树并集 → ORG_AND_CHILD（两组织合法互授即各自子树并集）
        Set<Long> scopeOrgIds = new HashSet<>();
        for (Long orgId : effective.orgIds) {
            collectSelfAndDescendants(orgId, scopeOrgIds);
        }
        result.setSelf(true);
        result.setOrgIds(scopeOrgIds);
        result.setScopeType(OrgDataScopeEnum.ORG_AND_CHILD.getScope());
        return result;
    }

    /**
     * 解析账号的【直接】有效任职组织集合（ZS-PERM-001.B 复用）：状态 ACTIVE + valid_from 已到 + valid_to 未过
     * + 所属组织存在且显式开启，与 {@link #resolve} 及 IAM-002 {@code MembershipContextResolver} 的有效任职判定 <b>同口径</b>。
     * 与 {@link #resolve} 有两处刻意差异（均为授权目标归属判定所必需）：① <b>不含组织树后代下钻</b>——只需目标用户
     * 的直接任职组织；② <b>不施加</b> {@link #resolve} 的 PLATFORM→{@code ORG_ALL} 放大与超管豁免——若把「平台角色=全组织」
     * 搬到授权目标侧，会让「仅任职于平台组织」的目标用户在 org 限定授权下被放行，等于放宽授权写入范围，与 fail-closed 相悖。
     *
     * <p><b>调用不变式</b>：本方法只在 {@code @DataPermission(enable = false)} 入口或 {@code skipPermissionCheck() == true}
     * 的获批 visit 上下文下被调用；org 轴 SQL 规则（{@code OrgDataPermissionRule}）注册后如新增调用方，须自行包
     * {@code DataPermissionUtils.executeIgnore}，避免目标用户任职行被操作者 org 范围静默收窄。
     *
     * @param userId 账号编号
     * @return 直接有效任职的组织编号集合（无有效任职时为空集，fail-closed 由调用方裁决）
     */
    public Set<Long> resolveEffectiveOrgIds(Long userId) {
        return collectEffectiveOrgs(userId).orgIds;
    }

    /**
     * 收集账号的有效任职组织（去重保序）+ 是否命中 PLATFORM 类型组织（显式平台角色）。
     * {@link #resolve}（org 轴数据范围）与 {@link #resolveEffectiveOrgIds}（授权目标归属）共用本收集逻辑，保证同口径。
     */
    private EffectiveOrgs collectEffectiveOrgs(Long userId) {
        EffectiveOrgs result = new EffectiveOrgs();
        if (userId == null || userId <= 0) {
            return result;
        }
        List<MembershipDO> memberships = membershipMapper.selectListByUserId(userId);
        LocalDateTime now = LocalDateTime.now();
        for (MembershipDO membership : memberships) {
            if (!isEffective(membership, now)) {
                continue;
            }
            OrganizationDO organization = organizationMapper.selectById(membership.getOrganizationId());
            // 组织必须存在且显式开启（fail-closed，与 MembershipContextResolver 同口径）
            if (organization == null || !CommonStatusEnum.isEnable(organization.getStatus())) {
                continue;
            }
            if (OrganizationTypeEnum.PLATFORM.getType().equals(organization.getType())) {
                result.platform = true;
            }
            result.orgIds.add(organization.getId());
        }
        return result;
    }

    /**
     * 有效任职组织收集结果：直接任职组织集合（去重保序）+ 是否含平台类型组织。
     */
    private static class EffectiveOrgs {
        private final Set<Long> orgIds = new LinkedHashSet<>();
        private boolean platform = false;
    }

    /**
     * 任职是否「有效」——与 IAM-002 {@code MembershipContextResolver} 同口径：
     * 状态 ACTIVE + valid_from 已到 + valid_to 未过（以同一 now 校验起止两端）。
     */
    private boolean isEffective(MembershipDO membership, LocalDateTime now) {
        if (!MembershipStatusEnum.isActive(membership.getStatus())) {
            return false;
        }
        if (membership.getValidFrom() != null && membership.getValidFrom().isAfter(now)) {
            return false; // 未到生效期
        }
        if (membership.getValidTo() != null && membership.getValidTo().isBefore(now)) {
            return false; // 已过有效期
        }
        return true;
    }

    /**
     * 收集组织自身 + 组织树全部后代（BFS）。visited 去重保证对脏数据成环的终止性，深度上限作二次 fail-safe。
     *
     * <p>后代下钻同样施加 {@link CommonStatusEnum#isEnable} 校验（codex r0 P2）：{@code selectListByParentId}
     * 仅按父组织编号过滤、不校验状态，若不在此处显式排除，启用组织 A 的禁用子组织 B 会被纳入范围，
     * 使 {@code isOrgVisible(B)} 通过——而直接任职于 B 却被启用校验排除，前后矛盾。禁用组织既不入范围、
     * 也不继续下钻（其子树 fail-closed 一并排除），与直接任职的启用口径完全一致。根组织已由调用方校验为启用。
     */
    private void collectSelfAndDescendants(Long rootOrgId, Set<Long> accumulator) {
        Deque<Long> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        queue.add(rootOrgId);
        visited.add(rootOrgId);
        int depth = 0;
        while (!queue.isEmpty() && depth < MAX_DESCENDANT_DEPTH) {
            int levelSize = queue.size();
            for (int i = 0; i < levelSize; i++) {
                Long current = queue.poll();
                accumulator.add(current);
                List<OrganizationDO> children = organizationMapper.selectListByParentId(current);
                for (OrganizationDO child : children) {
                    // 后代组织必须显式开启：禁用/无效状态的组织及其子树不纳入授权范围（fail-closed，codex r0 P2）
                    if (!CommonStatusEnum.isEnable(child.getStatus())) {
                        continue;
                    }
                    if (visited.add(child.getId())) { // 去重防环：已访问过的组织不再入队
                        queue.add(child.getId());
                    }
                }
            }
            depth++;
        }
    }

    private OrgDataPermissionRespDTO allOrgs(OrgDataPermissionRespDTO result) {
        result.setAll(true);
        result.setSelf(true);
        result.setScopeType(OrgDataScopeEnum.ORG_ALL.getScope());
        return result;
    }

    private OrgDataPermissionRespDTO selfOnly(OrgDataPermissionRespDTO result) {
        result.setSelf(true);
        result.setScopeType(OrgDataScopeEnum.ORG_SELF.getScope());
        return result;
    }

}
