package cn.zszj.framework.security.core.util;

import cn.hutool.core.collection.CollUtil;
import cn.zszj.framework.common.biz.system.permission.dto.CrossOrgVisitDecisionDTO;
import cn.zszj.framework.security.core.LoginUser;

import java.util.Collection;

/**
 * 跨组织访问授权范围持有者（ZS-SEC-001.B）。
 *
 * <p>承载「获批跨组织访问」的受控范围快照在请求内的读写：{@code TenantVisitContextInterceptor} 获批后
 * 经 {@link #setScope} 将 {@link CrossOrgVisitDecisionDTO} 写入 {@link LoginUser#getContext() LoginUser 上下文}，
 * 功能权限（{@code SecurityFrameworkServiceImpl}）与对象/字段访问点经本类消费，{@code afterCompletion} 经
 * {@link #clear} 清理（落实 docs/05 line331「授权撤销与上下文清理有效」）。
 *
 * <p>本类同时是 D6（功能权限收敛）与 D7（对象/字段访问入口，"入口先行"）的统一读取面：visit 上下文下
 * <b>不再整体跳过</b>校验，而是按授权记录范围裁决；无 scope（异常态/未获批）一律 <b>fail-closed</b>。
 * 各业务路径逐一接入 {@link #isObjectAllowed}/{@link #areFieldsAllowed} 随下游领域模块落地（边界）。
 *
 * @author ZS-SEC-001.B
 */
public class CrossOrgVisitScopeHolder {

    /**
     * LoginUser 上下文中授权范围快照的 key
     */
    public static final String CONTEXT_KEY = "crossOrgVisitScope";

    private CrossOrgVisitScopeHolder() {
    }

    /**
     * 写入授权范围快照（拦截器获批后调用）。
     */
    public static void setScope(CrossOrgVisitDecisionDTO scope) {
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser != null && scope != null) {
            loginUser.setContext(CONTEXT_KEY, scope);
        }
    }

    /**
     * 读取授权范围快照；无登录用户 / 未获批 / 类型不符时返回 {@code null}。
     *
     * <p>直接对上下文 Map 取值并做 {@code instanceof} 判定（不经 {@code MapUtil} 类型转换），
     * 避免复杂对象被 bean 转换重构、集合字段丢失。
     */
    public static CrossOrgVisitDecisionDTO getScope() {
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser == null || loginUser.getContext() == null) {
            return null;
        }
        Object raw = loginUser.getContext().get(CONTEXT_KEY);
        return raw instanceof CrossOrgVisitDecisionDTO ? (CrossOrgVisitDecisionDTO) raw : null;
    }

    /**
     * 清理授权范围快照（拦截器 {@code afterCompletion} 调用，与恢复原租户同处，确保上下文不残留）。
     */
    public static void clear() {
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser != null && loginUser.getContext() != null) {
            loginUser.getContext().remove(CONTEXT_KEY);
        }
    }

    /**
     * 动作维度裁决（D6）：visit 上下文下，{@code permissions} 任一 ∈ 授权 {@code allowedActions} 才放行。
     *
     * <p>无 scope / 未获批 / 授权动作为空 → <b>fail-closed false</b>（落实 line331「即使具备访问入口权限，
     * 也不能自动获得所有目标动作」）。取代旧 {@code skipPermissionCheck() → return true} 的整体跳过。
     */
    public static boolean isAnyActionAllowed(String... permissions) {
        CrossOrgVisitDecisionDTO scope = getScope();
        if (scope == null || !scope.isAuthorized() || CollUtil.isEmpty(scope.getAllowedActions()) || permissions == null) {
            return false;
        }
        for (String permission : permissions) {
            if (scope.getAllowedActions().contains(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 对象维度裁决（D7 入口）：{@code orgId} ∈ 授权 {@code targetOrgIds} 才放行；{@code targetOrgIds} 为 {@code null}
     * 表示目标租户内全部组织（whole-tenant 授权），此维放行。无 scope / 未获批 → fail-closed false。
     */
    public static boolean isObjectAllowed(Long orgId) {
        CrossOrgVisitDecisionDTO scope = getScope();
        if (scope == null || !scope.isAuthorized()) {
            return false;
        }
        if (scope.getTargetOrgIds() == null) {
            return true;
        }
        return orgId != null && scope.getTargetOrgIds().contains(orgId);
    }

    /**
     * 字段维度裁决（D7 入口）：{@code fields} ⊆ 授权 {@code allowedFields} 才放行；请求字段为空视为不校验字段维。
     * 无 scope / 未获批 → fail-closed false。字段目录归 ZS-PERM-003，本卡交付判定机制。
     */
    public static boolean areFieldsAllowed(Collection<String> fields) {
        CrossOrgVisitDecisionDTO scope = getScope();
        if (scope == null || !scope.isAuthorized()) {
            return false;
        }
        if (CollUtil.isEmpty(fields)) {
            return true;
        }
        return scope.getAllowedFields() != null && scope.getAllowedFields().containsAll(fields);
    }

}
