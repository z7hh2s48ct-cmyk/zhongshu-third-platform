package cn.zszj.framework.datapermission.core.authorize;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.zszj.framework.common.biz.system.permission.PermissionCommonApi;
import cn.zszj.framework.common.biz.system.permission.dto.OrgDataPermissionRespDTO;
import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 访问者可读字段等级上限解析器（ZS-PERM-003.B，D-12 §3 等级×角色默认映射）。
 *
 * <p>判定序（判定骨架与 {@code OrgDataPermissionChecker} 一致，护栏同口径）：
 * <ol>
 *     <li>无登录用户（系统内部/租户供给/定时任务上下文）→ 保守 F1，不触达权限 API；</li>
 *     <li>仅 ADMIN 类型用户适用组织数据范围（MEMBER 属会员另一轴）→ 保守 F1，不触达权限 API；</li>
 *     <li>组织数据权限 {@code all=true}（超管/显式平台角色）→ F3；</li>
 *     <li>对象所属组织 ∈ {@code ledOrgIds}（登录主体任该组织负责人）→ F2；</li>
 *     <li>其余（普通成员/无任职/无组织列对象/负责人身份不跨组织）→ F1。</li>
 * </ol>
 *
 * <p>降级方向恒为<b>收紧可见性</b>：组织数据权限取不到（null）时按 F1 处理并 WARN，不放大、不阻断
 * （对象维可见性已由 checker 裁决，字段等级只在其上进一步收敛）。结果经 LoginUser 上下文缓存
 * （缓存的是范围 DTO，不是等级值——同主体对不同组织对象返回各自等级）。
 *
 * <p>边界：visit 请求不经过本解析器（字段维以授权记录为唯一权威，见裁决服务）；跨组织 visit 的
 * F1 默认上限属授权发放环节约定（SEC-001.B 判定序字段步），本解析器不叠加。
 *
 * @author ZS-PERM-003.B
 */
@RequiredArgsConstructor
@Slf4j
public class FieldLevelScopeResolver {

    /**
     * LoginUser 的 Context 缓存 Key（独立于 org 轴 checker 的 key，两维各缓存各的范围 DTO）
     */
    private static final String CONTEXT_KEY = FieldLevelScopeResolver.class.getSimpleName();

    private final PermissionCommonApi permissionApi;

    /**
     * 解析登录主体对目标组织对象的可读字段等级上限。
     *
     * @param orgId 对象所属组织编号，可为 null（无组织列对象按 F1 口径）
     * @return 可读等级上限（F3 平台 / F2 组织负责人 / F1 默认）
     */
    public FieldLevel resolveMaxLevel(Long orgId) {
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        // 护栏一：无登录用户（系统内部/供给/任务），保守 F1（与 org 轴 checker 护栏一同口径）
        if (loginUser == null) {
            return FieldLevel.F1;
        }
        // 护栏二：仅 ADMIN 类型用户适用组织数据范围（MEMBER 另一轴），保守 F1 且不触达权限 API
        if (ObjectUtil.notEqual(loginUser.getUserType(), UserTypeEnum.ADMIN.getValue())) {
            return FieldLevel.F1;
        }
        OrgDataPermissionRespDTO permission = getOrgDataPermission(loginUser);
        if (permission == null) {
            return FieldLevel.F1;
        }
        // 平台/超管 → F3（D-12 §3：平台角色 ≤F3）
        if (Boolean.TRUE.equals(permission.getAll())) {
            return FieldLevel.F3;
        }
        // 对象所属组织的负责人 → F2（负责人身份不跨组织：仅命中对象自身所属组织）
        if (orgId != null && CollUtil.contains(permission.getLedOrgIds(), orgId)) {
            return FieldLevel.F2;
        }
        return FieldLevel.F1;
    }

    /**
     * 获得登录用户的组织数据权限，复用 LoginUser 上下文缓存（与 org 轴 checker 同机制）；
     * 取不到时返回 null（调用方按保守 F1 处理），不缓存 null。
     */
    private OrgDataPermissionRespDTO getOrgDataPermission(LoginUser loginUser) {
        OrgDataPermissionRespDTO permission = loginUser.getContext(CONTEXT_KEY, OrgDataPermissionRespDTO.class);
        if (permission == null) {
            permission = permissionApi.getOrgDataPermission(loginUser.getId());
            if (permission == null) {
                log.warn("[getOrgDataPermission][登录用户({}) 未返回组织数据权限，字段等级按保守 F1 处理]",
                        loginUser.getId());
                return null;
            }
            loginUser.setContext(CONTEXT_KEY, permission);
        }
        return permission;
    }

}
