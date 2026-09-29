package cn.zszj.framework.datapermission.core.rule.org;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.zszj.framework.common.biz.system.permission.PermissionCommonApi;
import cn.zszj.framework.common.biz.system.permission.dto.OrgDataPermissionRespDTO;
import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.common.util.collection.CollectionUtils;
import cn.zszj.framework.common.util.json.JsonUtils;
import cn.zszj.framework.datapermission.core.rule.DataPermissionRule;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import cn.zszj.framework.mybatis.core.util.MyBatisUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 基于组织归属的 {@link DataPermissionRule} 数据权限规则实现（org 轴 SQL 列表静默过滤，ZS-FC-002）。
 *
 * <p>对象级门 {@link OrgDataPermissionChecker}（详情/批量/单对象显式拒绝，ZS-PERM-002.B）已先行交付；
 * 本规则补齐 SQL 列表路径的静默过滤，两者共用 {@link PermissionCommonApi#getOrgDataPermission(Long)}
 * 的同一 org 轴范围（orgIds），口径同源。B08 放行报告 §6.1 移交项（首张业务表
 * {@code bpm_first_chain_lead} 注册，ZS-FC-002 落地联验）。
 *
 * <p>语义与对象级门同构：对象一旦归属组织（org_id 列注册），列表行一律由组织范围裁决——
 * {@code org_id IN (orgIds)}；{@code all=true} 不拼接条件；空 orgIds 且非 self → 恒假（不返回数据）。
 * self（本人兜底）语义仅适用于无组织列对象，本规则只注册有 org 列的表，故不参与拼接。
 *
 * <p>上下文缓存 Key 独立于 dept 轴 {@code DeptDataPermissionRule} 与 {@link OrgDataPermissionChecker}，
 * 三者互不串扰。
 *
 * @author ZS-FC-002
 */
@AllArgsConstructor
@Slf4j
public class OrgDataPermissionRule implements DataPermissionRule {

    /**
     * LoginUser 的 Context 缓存 Key（org 轴规则独立，与 dept 轴/对象级 checker 不串扰）
     */
    protected static final String CONTEXT_KEY = OrgDataPermissionRule.class.getSimpleName();

    /**
     * 基于组织归属的表字段配置；key：表名，value：组织归属列名（默认 org_id）
     */
    private final Map<String, String> orgColumns = new HashMap<>();

    /**
     * 生效表名集合
     */
    private final Set<String> TABLE_NAMES = new HashSet<>();

    private final PermissionCommonApi permissionApi;

    @Override
    public Set<String> getTableNames() {
        return TABLE_NAMES;
    }

    @Override
    public Expression getExpression(String tableName, Alias tableAlias) {
        // 只有有登陆用户的情况下，才进行数据权限的处理（与 DeptDataPermissionRule 同口径）
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser == null) {
            return null;
        }
        // 只有管理员类型的用户，才进行数据权限的处理
        if (ObjectUtil.notEqual(loginUser.getUserType(), UserTypeEnum.ADMIN.getValue())) {
            return null;
        }
        // 未注册 org 列的表不生效
        String columnName = orgColumns.get(tableName);
        if (isBlankColumn(columnName)) {
            return null;
        }

        // 获得 org 轴数据权限（上下文缓存避免重复计算，独立 Key）
        OrgDataPermissionRespDTO orgDataPermission = loginUser.getContext(CONTEXT_KEY, OrgDataPermissionRespDTO.class);
        if (orgDataPermission == null) {
            orgDataPermission = permissionApi.getOrgDataPermission(loginUser.getId());
            if (orgDataPermission == null) {
                log.error("[getExpression][LoginUser({}) 获取 org 轴数据权限为 null]", JsonUtils.toJsonString(loginUser));
                throw new NullPointerException(String.format("LoginUser(%d) Table(%s/%s) 未返回 org 数据权限",
                        loginUser.getId(), tableName, tableAlias == null ? "-" : tableAlias.getName()));
            }
            loginUser.setContext(CONTEXT_KEY, orgDataPermission);
        }

        // 情况一，ALL 可查看全部，无需拼接条件
        if (Boolean.TRUE.equals(orgDataPermission.getAll())) {
            return null;
        }

        // 情况二，无组织范围且无本人兜底 → 恒假（不返回数据，fail-closed）
        if (CollUtil.isEmpty(orgDataPermission.getOrgIds()) && !Boolean.TRUE.equals(orgDataPermission.getSelf())) {
            return new EqualsTo(null, null); // WHERE null = null
        }

        // 情况三，拼接 org_id IN (orgIds)；空集合（有 self 兜底的口径不适用有组织列对象）→ 恒假
        if (CollUtil.isEmpty(orgDataPermission.getOrgIds())) {
            return new EqualsTo(null, null);
        }
        return new InExpression(MyBatisUtils.buildColumn(tableName, tableAlias, columnName),
                new ParenthesedExpressionList(
                        new net.sf.jsqlparser.expression.operators.relational.ExpressionList<>(
                                CollectionUtils.convertList(orgDataPermission.getOrgIds(), LongValue::new))));
    }

    // ==================== 添加配置 ====================

    public void addOrgColumn(String tableName) {
        addOrgColumn(tableName, "org_id");
    }

    public void addOrgColumn(String tableName, String columnName) {
        orgColumns.put(tableName, columnName);
        TABLE_NAMES.add(tableName);
    }

    private static boolean isBlankColumn(String columnName) {
        return columnName == null || columnName.isBlank();
    }

}
