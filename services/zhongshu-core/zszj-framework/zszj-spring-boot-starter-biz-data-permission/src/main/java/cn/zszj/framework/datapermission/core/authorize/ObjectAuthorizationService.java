package cn.zszj.framework.datapermission.core.authorize;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.zszj.framework.datapermission.core.rule.org.OrgDataPermissionChecker;
import cn.zszj.framework.security.core.service.SecurityFrameworkService;
import cn.zszj.framework.security.core.util.CrossOrgVisitScopeHolder;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants.FORBIDDEN;
import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;

/**
 * 统一「动作 / 字段」授权裁决服务（ZS-PERM-003.A；字段等级裁剪 ZS-PERM-003.B / D-12）。
 *
 * <p>裁决流水线（输出与执行共用同一核心，每次调用实时重算、无缓存）：
 * <ol>
 *     <li>扩展点路由：按 {@link ObjectAuthorizationRequest#getObjectType()} 查找
 *     {@link ObjectAuthorizationProvider}；未注册返回 {@code null}（未接入=零变化，不抛异常）；</li>
 *     <li>对象维：非 visit 请求走 org 轴 {@link OrgDataPermissionChecker}（D-09 FND-AUTH-004）；
 *     跨租户 visit 请求经 {@link CrossOrgVisitScopeHolder} 收敛（D7），无 scope 异常态 fail-closed
 *     且不落 home 组织 checker；</li>
 *     <li>动作维：候选动作去重后经状态钩子过滤，再与登录主体功能权限取交集（visit 请求由
 *     {@code SecurityFrameworkServiceImpl} 收敛为 {@code visit.allowedActions}，D6）；
 *     恒返回非 null 集合（空集=无任何动作可用）；</li>
 *     <li>字段维：provider 未声明候选字段 → {@code null}（未启用字段级输出，零变化）；候选字段过滤
 *     null/空白（与动作维同口径）；visit 请求逐字段经 {@link CrossOrgVisitScopeHolder#areFieldsAllowed}
 *     收敛（{@code allowedFields} 缺失 fail-closed）——授权记录为唯一权威；非 visit 时 plain provider
 *     全候选（.A 零变化），分级 provider（ZS-PERM-003.B / D-12）按字段等级裁剪：等级 ≤ 访问者上限
 *     （{@link FieldLevelScopeResolver}）清晰可见、F2 低于上限脱敏可见（maskedFields）、其余超上限拒绝输出。</li>
 * </ol>
 *
 * <p>执行侧 {@link #checkActionAllowed}/{@link #checkFieldsAllowed} 内部复用 {@link #authorize}，
 * 结构保证「前端伪造 allowedActions/authorizedFields 不生效、对象状态或授权变更后重新校验」。
 *
 * @author ZS-PERM-003.A
 * @author ZS-PERM-003.B
 */
@Slf4j
public class ObjectAuthorizationService {

    private final SecurityFrameworkService securityFrameworkService;

    private final OrgDataPermissionChecker orgDataPermissionChecker;

    /**
     * 访问者可读字段等级上限解析器（ZS-PERM-003.B / D-12 §3 等级×角色默认映射）
     */
    private final FieldLevelScopeResolver fieldLevelScopeResolver;

    /**
     * 对象类型 → 扩展点（构造时校验非空白与唯一性，重复注册 fail-fast，避免裁决路由歧义）
     */
    private final Map<String, ObjectAuthorizationProvider> providerMap;

    public ObjectAuthorizationService(SecurityFrameworkService securityFrameworkService,
                                      OrgDataPermissionChecker orgDataPermissionChecker,
                                      FieldLevelScopeResolver fieldLevelScopeResolver,
                                      List<ObjectAuthorizationProvider> providers) {
        this.securityFrameworkService = securityFrameworkService;
        this.orgDataPermissionChecker = orgDataPermissionChecker;
        this.fieldLevelScopeResolver = fieldLevelScopeResolver;
        Map<String, ObjectAuthorizationProvider> map = new HashMap<>();
        if (CollUtil.isNotEmpty(providers)) {
            for (ObjectAuthorizationProvider provider : providers) {
                if (StrUtil.isBlank(provider.getObjectType())) {
                    throw new IllegalStateException("对象授权扩展点 objectType 不能为空: "
                            + provider.getClass().getName());
                }
                if (map.put(provider.getObjectType(), provider) != null) {
                    throw new IllegalStateException("重复注册对象授权扩展点: " + provider.getObjectType());
                }
                // ZS-PERM-003.B：分级 provider 的编目完整性装配校验（配置错误 fail-fast，不静默丢字段）
                if (provider instanceof ClassifiedObjectAuthorizationProvider classified) {
                    validateClassified(classified);
                }
            }
        }
        this.providerMap = map;
    }

    /**
     * 输出裁决：返回允许动作与授权字段；未注册对象类型返回 {@code null}（未接入=零变化）。
     */
    public ObjectAuthorizationRespDTO authorize(ObjectAuthorizationRequest request) {
        // 一、扩展点路由：未注册=未接入，返回 null 零变化（不抛异常，支持业务域渐进接入）
        ObjectAuthorizationProvider provider = providerMap.get(request.getObjectType());
        if (provider == null) {
            return null;
        }
        // 二、对象维：不可见对象不得输出任何授权（隐藏对象不能经批量/导出/文件旁路探测）
        checkObjectVisible(request);
        // 三、四、动作维 + 字段维：各自独立计算（字段维不依赖动作维，动作维不因字段维缺失而跳过）
        ObjectAuthorizationRespDTO resp = new ObjectAuthorizationRespDTO();
        resp.setAllowedActions(resolveAllowedActions(provider, request));
        resolveFields(provider, request, resp);
        return resp;
    }

    /**
     * 执行侧动作校验：动作不在裁决输出内（含前端伪造）显式拒绝（{@code FORBIDDEN}）。
     */
    public void checkActionAllowed(ObjectAuthorizationRequest request, String action) {
        if (action == null || action.isBlank()) {
            throw exception(FORBIDDEN);
        }
        ObjectAuthorizationRespDTO resp = authorize(request);
        if (resp == null || !resp.getAllowedActions().contains(action)) {
            log.warn("[checkActionAllowed][登录用户({}) 对象({}) 动作({}) 不在授权输出内，已拒绝]",
                    SecurityFrameworkUtils.getLoginUserId(), request.getObjectType(), action);
            throw exception(FORBIDDEN);
        }
    }

    /**
     * 执行侧字段校验：请求字段超出授权字段（含隐藏字段旁路）显式拒绝（{@code FORBIDDEN}）；
     * 空字段请求视为不校验字段维。
     */
    public void checkFieldsAllowed(ObjectAuthorizationRequest request, Set<String> fields) {
        if (CollUtil.isEmpty(fields)) {
            return;
        }
        ObjectAuthorizationRespDTO resp = authorize(request);
        // 未接入（resp=null）或未启用字段级（authorizedFields=null）时，非空字段请求 fail-closed 拒绝
        if (resp == null || resp.getAuthorizedFields() == null
                || !resp.getAuthorizedFields().containsAll(fields)) {
            log.warn("[checkFieldsAllowed][登录用户({}) 对象({}) 字段({}) 超出授权字段，已拒绝]",
                    SecurityFrameworkUtils.getLoginUserId(), request.getObjectType(), fields);
            throw exception(FORBIDDEN);
        }
    }

    // ========== 对象维 ==========

    /**
     * 对象维裁决：visit 请求经 {@link CrossOrgVisitScopeHolder} 收敛（无 scope 异常态 fail-closed，
     * 不落 home 组织 checker——目标范围与 home 范围语义不同，混入会引入错误放行/拒绝）；
     * 非 visit 走 org 轴 {@link OrgDataPermissionChecker}。
     */
    private void checkObjectVisible(ObjectAuthorizationRequest request) {
        if (SecurityFrameworkUtils.skipPermissionCheck()) {
            if (!CrossOrgVisitScopeHolder.isObjectAllowed(request.getOrgId())) {
                log.warn("[checkObjectVisible][登录用户({}) visit 请求对象(orgId={}) 不在授权组织范围，已拒绝]",
                        SecurityFrameworkUtils.getLoginUserId(), request.getOrgId());
                throw exception(FORBIDDEN);
            }
            return;
        }
        if (!orgDataPermissionChecker.isObjectVisible(request.getOrgId(), request.getOwnerUserId())) {
            log.warn("[checkObjectVisible][登录用户({}) 越权访问组织对象(orgId={}, ownerUserId={})，已拒绝]",
                    SecurityFrameworkUtils.getLoginUserId(), request.getOrgId(), request.getOwnerUserId());
            throw exception(FORBIDDEN);
        }
    }

    // ========== 动作维 ==========

    /**
     * 动作维裁决：候选动作（去重、过滤 null）→ 状态钩子 → 功能权限交集（visit 请求由
     * {@code SecurityFrameworkServiceImpl} 收敛）；恒返回非 null 集合。
     */
    private Set<String> resolveAllowedActions(ObjectAuthorizationProvider provider,
                                              ObjectAuthorizationRequest request) {
        Set<String> allowedActions = new LinkedHashSet<>();
        Collection<String> candidates = provider.getCandidateActions();
        if (CollUtil.isEmpty(candidates)) {
            return allowedActions;
        }
        for (String action : candidates) {
            if (action == null || action.isBlank()
                    || !provider.isActionAllowedInStatus(action, request.getObject())) {
                continue;
            }
            if (securityFrameworkService.hasAnyPermissions(action)) {
                allowedActions.add(action);
            }
        }
        return allowedActions;
    }

    // ========== 字段维（含脱敏维，ZS-PERM-003.B / D-12） ==========

    /**
     * 字段维 + 脱敏维裁决：
     * <ul>
     *     <li>未声明候选字段 → authorizedFields/maskedFields 双 null（未启用，零变化）；</li>
     *     <li>visit 请求：逐字段经 {@link CrossOrgVisitScopeHolder#areFieldsAllowed} 收敛
     *     （{@code allowedFields} 缺失 fail-closed）——授权记录为唯一权威（D-12 §3：F2/F3 须经授权记录
     *     字段维显式放行），显式放行=清晰输出、无脱敏叠加，plain/classified 语义一致；</li>
     *     <li>非 visit + 分级 provider（{@link ClassifiedObjectAuthorizationProvider}）：按 D-12 等级裁剪——
     *     等级 ≤ 访问者上限清晰可见；F2 低于上限脱敏可见（进 authorizedFields + maskedFields）；
     *     其余超上限等级拒绝输出（不进 authorizedFields，执行侧 fail-closed，隐藏字段不能经详情/批量旁路）；</li>
     *     <li>非 visit + plain provider：全候选（.A 零变化），maskedFields 空集。</li>
     * </ul>
     * maskedFields 合同：authorizedFields 非 null 时恒非 null（空集=无脱敏字段），且 ⊆ authorizedFields。
     */
    private void resolveFields(ObjectAuthorizationProvider provider, ObjectAuthorizationRequest request,
                               ObjectAuthorizationRespDTO resp) {
        Collection<String> candidates = provider.getCandidateFields();
        if (CollUtil.isEmpty(candidates)) {
            resp.setAuthorizedFields(null);
            resp.setMaskedFields(null);
            return;
        }
        Set<String> authorized = new LinkedHashSet<>();
        Set<String> masked = new LinkedHashSet<>();
        if (SecurityFrameworkUtils.skipPermissionCheck()) {
            // visit：授权记录字段维为唯一权威（D7 判定序字段步），逐字段收敛
            for (String field : candidates) {
                if (field == null || field.isBlank()) {
                    continue;
                }
                if (CrossOrgVisitScopeHolder.areFieldsAllowed(Collections.singleton(field))) {
                    authorized.add(field);
                }
            }
        } else if (provider instanceof ClassifiedObjectAuthorizationProvider classified) {
            // 非 visit 分级裁剪：等级 = 显式编目 ?? 域级默认（注册校验保证可解析）
            FieldLevel maxLevel = fieldLevelScopeResolver.resolveMaxLevel(request.getOrgId());
            for (String field : candidates) {
                if (field == null || field.isBlank()) {
                    continue;
                }
                FieldLevel level = resolveFieldLevel(classified, field);
                if (level.isWithin(maxLevel)) {
                    authorized.add(field);
                } else if (level == FieldLevel.F2) {
                    // F2 敏感级对低于 F2 的访问者默认脱敏可见（D-12 §5.2，非整字段隐藏）
                    authorized.add(field);
                    masked.add(field);
                }
                // 其余超上限等级（如 F3）：拒绝输出——隐藏字段不进授权集合，执行侧 fail-closed 拒绝
            }
        } else {
            // plain provider 维持 .A 非 visit 全候选（未分级域零变化）
            for (String field : candidates) {
                if (field == null || field.isBlank()) {
                    continue;
                }
                authorized.add(field);
            }
        }
        resp.setAuthorizedFields(authorized);
        resp.setMaskedFields(masked);
    }

    /**
     * 字段等级解析：显式编目优先，未编目字段落域级默认（装配校验保证二者至少其一覆盖候选字段）。
     */
    private FieldLevel resolveFieldLevel(ClassifiedObjectAuthorizationProvider classified, String field) {
        FieldLevel level = classified.getFieldLevels().get(field);
        return level != null ? level : classified.getDomainDefaultLevel();
    }

    /**
     * 分级 provider 的编目完整性装配校验（ZS-PERM-003.B）：编目映射/键/值非空，且每个非空白候选字段
     * 必须有显式编目或域级默认——配置错误在装配期 fail-fast，而非运行期静默丢字段（对齐 .A 空白
     * objectType/重复注册的 fail-fast 口径）。编目键可超出当前候选（目录先改、代码后接的回填制）。
     */
    private void validateClassified(ClassifiedObjectAuthorizationProvider classified) {
        Map<String, FieldLevel> levels = classified.getFieldLevels();
        if (levels == null) {
            throw new IllegalStateException("字段等级编目不能为 null: " + classified.getObjectType());
        }
        for (Map.Entry<String, FieldLevel> entry : levels.entrySet()) {
            if (StrUtil.isBlank(entry.getKey())) {
                throw new IllegalStateException("字段等级编目键不能为空: " + classified.getObjectType());
            }
            if (entry.getValue() == null) {
                throw new IllegalStateException("字段等级编目值不能为 null: "
                        + classified.getObjectType() + "." + entry.getKey());
            }
        }
        if (classified.getDomainDefaultLevel() != null) {
            return;
        }
        for (String field : classified.getCandidateFields()) {
            if (StrUtil.isNotBlank(field) && !levels.containsKey(field)) {
                throw new IllegalStateException("候选字段未编目且无域级默认: "
                        + classified.getObjectType() + "." + field);
            }
        }
    }

}
