package cn.zszj.framework.datapermission.core.rule.org;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.zszj.framework.common.biz.system.permission.PermissionCommonApi;
import cn.zszj.framework.common.biz.system.permission.dto.OrgDataPermissionRespDTO;
import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.function.Function;

import static cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants.FORBIDDEN;
import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;

/**
 * 基于组织数据范围的「对象级」授权检查入口（org 轴，ZS-PERM-002.B）。
 *
 * <p>与 {@code DeptDataPermissionChecker}（dept/self 轴，处理技术租户内 dept_id/user_id）<b>正交并存</b>：
 * 本检查器处理<b>业务组织</b>维度（{@code organization_id} 轴）的跨组织授权，回答「目标对象所属组织是否落在
 * 登录主体的授权组织范围内」，落实 D-09 FND-AUTH-004——<b>即使已知他组织对象 ID，也不能读取/修改/下载/导出</b>。
 *
 * <p>判定骨架与 dept 检查器一致（ALL 全量 / 命中授权组织集合 / 本人兜底），差别仅在数据范围载体换成
 * {@link OrgDataPermissionRespDTO}（orgIds 而非 deptIds），且使用<b>独立的上下文缓存 Key</b>
 * （{@link #CONTEXT_KEY}），与 dept 轴互不串扰。护栏亦与 dept 检查器完全一致（非新增绕过面）：
 * <ul>
 *     <li>无登录用户（系统内部/租户供给/定时任务上下文）时不施加组织范围限制；Web 请求恒有登录用户；</li>
 *     <li>仅 ADMIN 类型用户适用组织数据范围（MEMBER 的对象授权属会员模块另一轴）；</li>
 *     <li>取不到组织数据权限（返回 null）时 fail-closed 拒绝，不放行。</li>
 * </ul>
 *
 * <p>边界：跨租户隔离由租户拦截器与 ZS-DB-018 覆盖；dept/self 轴由 ZS-PERM-002.A 覆盖；本检查器只表达
 * 同一技术租户内的 org 轴范围。一期跨组织只允许显式平台角色（D-09），临时跨组织授权后置。
 *
 * @author ZS-PERM-002.B
 * @see cn.zszj.framework.datapermission.core.rule.dept.DeptDataPermissionChecker
 */
@RequiredArgsConstructor
@Slf4j
public class OrgDataPermissionChecker {

    /**
     * LoginUser 的 Context 缓存 Key（org 轴独立，与 dept 轴 {@code DeptDataPermissionRule.CONTEXT_KEY} 不串扰）
     */
    protected static final String CONTEXT_KEY = OrgDataPermissionChecker.class.getSimpleName();

    private final PermissionCommonApi permissionApi;

    /**
     * 判断指定组织是否在登录用户的授权组织范围内。
     *
     * @param orgId 目标对象所属组织编号
     * @return 是否可见
     */
    public boolean isOrgVisible(Long orgId) {
        return isObjectVisible(orgId, null);
    }

    /**
     * 判断某个对象（按「所属组织 + 负责人」）是否在登录用户的授权组织范围内。
     *
     * <p>命中任一即可见：ALL 全部组织（超管/显式平台角色）；所属组织落在授权组织集合（本组织子树）；
     * <b>无组织列对象</b>（{@code orgId == null}）的本人兜底（负责人为登录用户）。对象一旦归属某组织，
     * 即由组织范围独占裁决，本人所有权不凌驾于组织排除之上（D-09 FND-AUTH-004，与 dept 轴 self 语义有意区分）。
     *
     * @param orgId       对象所属组织编号，可为 null（无组织列的对象）
     * @param ownerUserId 对象负责人（创建人/归属人）用户编号，可为 null
     * @return 是否可见
     */
    public boolean isObjectVisible(Long orgId, Long ownerUserId) {
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        // 护栏一：无登录用户（系统内部/供给/任务），与 dept 检查器一致，不施加组织范围限制
        if (loginUser == null) {
            return true;
        }
        // 护栏二：仅 ADMIN 类型用户适用组织数据范围（与 dept 检查器一致）
        if (ObjectUtil.notEqual(loginUser.getUserType(), UserTypeEnum.ADMIN.getValue())) {
            return true;
        }

        OrgDataPermissionRespDTO permission = getOrgDataPermission(loginUser);
        // 情况一：ALL 可查看全部组织（超管 / 显式平台角色）
        if (Boolean.TRUE.equals(permission.getAll())) {
            return true;
        }
        // 情况二：命中授权组织集合（本人各在职组织 + 组织树后代）
        if (orgId != null && CollUtil.contains(permission.getOrgIds(), orgId)) {
            return true;
        }
        // 情况三：无组织归属对象（orgId == null）的本人兜底——仅当对象没有组织列时，才按负责人为登录用户放行。
        // 与 dept 轴的差异（有意，codex r0 P1）：dept 轴 self 是租户内合法可见层（本人数据权限），可在可见部门集之外
        // 放行本人对象；org 轴受 D-09 FND-AUTH-004 约束——对象一旦归属某组织（orgId != null），必须由组织范围裁决，
        // 本人所有权不得凌驾于组织排除之上，否则转岗/离任成员仍能凭所有权访问其在旧组织创建的对象（跨组织泄漏）。
        if (orgId == null && Boolean.TRUE.equals(permission.getSelf())
                && ownerUserId != null && ownerUserId.equals(loginUser.getId())) {
            return true;
        }
        // 其余：既不在授权组织、又非「无组织列的本人对象」（含已知他组织对象 ID），判定不可见（FND-AUTH-004）
        return false;
    }

    /**
     * 校验单个对象可见，否则显式拒绝（抛 {@code FORBIDDEN}）。用于详情、单对象读写等路径。
     *
     * @param orgId       对象所属组织编号
     * @param ownerUserId 对象负责人用户编号
     */
    public void checkObjectVisible(Long orgId, Long ownerUserId) {
        if (!isObjectVisible(orgId, ownerUserId)) {
            log.warn("[checkObjectVisible][登录用户({}) 越权访问组织对象(orgId={}, ownerUserId={})，已拒绝]",
                    SecurityFrameworkUtils.getLoginUserId(), orgId, ownerUserId);
            throw exception(FORBIDDEN);
        }
    }

    /**
     * 批量校验对象可见性：**任一对象越权即整批拒绝**（抛 {@code FORBIDDEN}），而非静默丢弃越权项。
     *
     * <p>用于批量读写、导出、统计等「混入越权组织对象必须整批拒绝」的路径，践行众墅「批量混入拒绝」要求。
     *
     * @param objects           待校验对象集合，为空时直接通过
     * @param orgIdGetter       从对象提取所属组织编号的函数，可为 null（表示对象无组织列）
     * @param ownerUserIdGetter 从对象提取负责人用户编号的函数，可为 null
     * @param <T>               对象类型
     */
    public <T> void checkBatchVisible(Collection<T> objects,
                                      Function<T, Long> orgIdGetter,
                                      Function<T, Long> ownerUserIdGetter) {
        if (CollUtil.isEmpty(objects)) {
            return;
        }
        for (T object : objects) {
            Long orgId = orgIdGetter != null ? orgIdGetter.apply(object) : null;
            Long ownerUserId = ownerUserIdGetter != null ? ownerUserIdGetter.apply(object) : null;
            // 复用单对象判定：命中任一越权即整批拒绝
            if (!isObjectVisible(orgId, ownerUserId)) {
                log.warn("[checkBatchVisible][登录用户({}) 批量混入越权组织对象(orgId={}, ownerUserId={})，整批拒绝]",
                        SecurityFrameworkUtils.getLoginUserId(), orgId, ownerUserId);
                throw exception(FORBIDDEN);
            }
        }
    }

    /**
     * 获得登录用户的组织数据权限，复用 LoginUser 上下文缓存（{@link #CONTEXT_KEY}）避免同一请求内重复计算。
     *
     * <p>取不到组织数据权限（null）时 fail-closed 拒绝，不放行。
     */
    private OrgDataPermissionRespDTO getOrgDataPermission(LoginUser loginUser) {
        OrgDataPermissionRespDTO permission = loginUser.getContext(CONTEXT_KEY, OrgDataPermissionRespDTO.class);
        if (permission == null) {
            permission = permissionApi.getOrgDataPermission(loginUser.getId());
            if (permission == null) {
                log.error("[getOrgDataPermission][登录用户({}) 未返回组织数据权限，fail-closed 拒绝]", loginUser.getId());
                throw exception(FORBIDDEN);
            }
            loginUser.setContext(CONTEXT_KEY, permission);
        }
        return permission;
    }

}
