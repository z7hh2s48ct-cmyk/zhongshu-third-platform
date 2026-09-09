package cn.zszj.framework.datapermission.core.rule.dept;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.zszj.framework.common.biz.system.permission.PermissionCommonApi;
import cn.zszj.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
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
 * 基于部门数据范围的「对象级」授权检查入口（{@link DeptDataPermissionRule} 的显式拒绝补充）。
 *
 * <p>背景：{@link DeptDataPermissionRule} 以 MyBatis-Plus 数据权限插件的方式，在 SQL 执行前对**已注册表/列**
 * 追加 WHERE 条件，从而对列表/分页查询做「静默过滤」——越权行不会出现在结果里。但静默过滤不足以满足两类路径：
 * <ol>
 *     <li>**单对象详情**：越权对象被静默过滤为 null，调用方无法区分「不存在」与「无权限」，也无法显式拒绝；</li>
 *     <li>**批量操作/导出/统计**：混入的越权对象被静默丢弃，而非整批拒绝（众墅要求「批量混入拒绝」）。</li>
 * </ol>
 *
 * <p>本检查器复用与 {@link DeptDataPermissionRule} **完全一致**的数据范围语义（ALL / 指定部门 / 本人），
 * 并共享其在 {@link LoginUser} 上下文中的数据权限缓存（同一 {@code CONTEXT_KEY}），不重复计算范围；
 * 差别在于：它以「显式抛 {@code FORBIDDEN}」的方式拒绝越权，供详情与批量路径直接调用。
 *
 * <p>护栏（与 {@link DeptDataPermissionRule#getExpression} 保持一致，非新增绕过面）：
 * <ul>
 *     <li>无登录用户（系统内部/租户供给/定时任务上下文）时不施加数据范围限制；Web 请求恒有登录用户，
 *     故该分支仅对受信任的系统级调用生效；</li>
 *     <li>仅 ADMIN 类型用户适用部门数据范围（MEMBER 的对象授权属会员模块另一轴，不在本检查器范围）；</li>
 *     <li>取不到数据权限（返回 null）时 fail-closed 拒绝，不放行。</li>
 * </ul>
 *
 * <p>边界：本检查器只处理**技术租户内的部门/本人数据范围**（dept_id / user_id 轴）。跨租户隔离由租户拦截器
 * 与 ZS-DB-018 数据层回归覆盖；业务组织的 SELF/ASSIGNED 等跨组织授权范围须待 D-09 批准后由 ZS-PERM-002.B 映射。
 *
 * @author 众墅之家
 * @see DeptDataPermissionRule
 */
@RequiredArgsConstructor
@Slf4j
public class DeptDataPermissionChecker {

    private final PermissionCommonApi permissionApi;

    /**
     * 判断指定部门是否在登录用户的数据范围内。
     *
     * @param deptId 目标对象所属部门编号
     * @return 是否可见
     */
    public boolean isDeptVisible(Long deptId) {
        return isObjectVisible(deptId, null);
    }

    /**
     * 判断某个对象（按「所属部门 + 负责人」）是否在登录用户的数据范围内。
     *
     * <p>命中任一即可见：ALL 全量；所属部门落在可见部门集合；可查看本人且负责人为登录用户。
     *
     * @param deptId      对象所属部门编号，可为 null（无部门列的对象）
     * @param ownerUserId 对象负责人（创建人/归属人）用户编号，可为 null
     * @return 是否可见
     */
    public boolean isObjectVisible(Long deptId, Long ownerUserId) {
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        // 护栏一：无登录用户（系统内部/供给/任务），与 DeptDataPermissionRule 一致，不施加数据范围限制
        if (loginUser == null) {
            return true;
        }
        // 护栏二：仅 ADMIN 类型用户适用部门数据范围（与 DeptDataPermissionRule 一致）
        if (ObjectUtil.notEqual(loginUser.getUserType(), UserTypeEnum.ADMIN.getValue())) {
            return true;
        }

        DeptDataPermissionRespDTO permission = getDeptDataPermission(loginUser);
        // 情况一：ALL 可查看全部
        if (Boolean.TRUE.equals(permission.getAll())) {
            return true;
        }
        // 情况二：命中可见部门集合
        if (deptId != null && CollUtil.contains(permission.getDeptIds(), deptId)) {
            return true;
        }
        // 情况三：可查看本人，且对象负责人即登录用户
        if (Boolean.TRUE.equals(permission.getSelf())
                && ownerUserId != null && ownerUserId.equals(loginUser.getId())) {
            return true;
        }
        // 其余：既不在可见部门、又非本人（含 100% 无权限），判定不可见
        return false;
    }

    /**
     * 校验单个对象可见，否则显式拒绝（抛 {@code FORBIDDEN}）。用于详情、单对象读写等路径。
     *
     * @param deptId      对象所属部门编号
     * @param ownerUserId 对象负责人用户编号
     */
    public void checkObjectVisible(Long deptId, Long ownerUserId) {
        if (!isObjectVisible(deptId, ownerUserId)) {
            log.warn("[checkObjectVisible][登录用户({}) 越权访问对象(deptId={}, ownerUserId={})，已拒绝]",
                    SecurityFrameworkUtils.getLoginUserId(), deptId, ownerUserId);
            throw exception(FORBIDDEN);
        }
    }

    /**
     * 批量校验对象可见性：**任一对象越权即整批拒绝**（抛 {@code FORBIDDEN}），而非静默丢弃越权项。
     *
     * <p>用于批量读写、导出、统计等「混入越权对象必须整批拒绝」的路径，践行众墅「批量混入拒绝」要求。
     *
     * @param objects          待校验对象集合，为空时直接通过
     * @param deptIdGetter     从对象提取所属部门编号的函数，可为 null（表示对象无部门列）
     * @param ownerUserIdGetter 从对象提取负责人用户编号的函数，可为 null
     * @param <T>              对象类型
     */
    public <T> void checkBatchVisible(Collection<T> objects,
                                      Function<T, Long> deptIdGetter,
                                      Function<T, Long> ownerUserIdGetter) {
        if (CollUtil.isEmpty(objects)) {
            return;
        }
        for (T object : objects) {
            Long deptId = deptIdGetter != null ? deptIdGetter.apply(object) : null;
            Long ownerUserId = ownerUserIdGetter != null ? ownerUserIdGetter.apply(object) : null;
            // 复用单对象判定：命中任一越权即整批拒绝
            if (!isObjectVisible(deptId, ownerUserId)) {
                log.warn("[checkBatchVisible][登录用户({}) 批量混入越权对象(deptId={}, ownerUserId={})，整批拒绝]",
                        SecurityFrameworkUtils.getLoginUserId(), deptId, ownerUserId);
                throw exception(FORBIDDEN);
            }
        }
    }

    /**
     * 获得登录用户的数据权限，复用 {@link DeptDataPermissionRule} 在 LoginUser 上下文中的缓存，避免重复计算。
     *
     * <p>取不到数据权限（null）时 fail-closed 拒绝，不放行。
     */
    private DeptDataPermissionRespDTO getDeptDataPermission(LoginUser loginUser) {
        // 与 DeptDataPermissionRule 共用 CONTEXT_KEY，命中同一请求内已计算的范围
        DeptDataPermissionRespDTO permission = loginUser.getContext(
                DeptDataPermissionRule.CONTEXT_KEY, DeptDataPermissionRespDTO.class);
        if (permission == null) {
            permission = permissionApi.getDeptDataPermission(loginUser.getId());
            if (permission == null) {
                log.error("[getDeptDataPermission][登录用户({}) 未返回数据权限，fail-closed 拒绝]", loginUser.getId());
                throw exception(FORBIDDEN);
            }
            loginUser.setContext(DeptDataPermissionRule.CONTEXT_KEY, permission);
        }
        return permission;
    }

}
