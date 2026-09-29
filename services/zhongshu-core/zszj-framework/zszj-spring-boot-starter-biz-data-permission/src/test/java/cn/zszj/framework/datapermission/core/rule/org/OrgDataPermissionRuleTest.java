package cn.zszj.framework.datapermission.core.rule.org;

import cn.zszj.framework.common.biz.system.permission.PermissionCommonApi;
import cn.zszj.framework.common.biz.system.permission.dto.OrgDataPermissionRespDTO;
import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.common.util.collection.SetUtils;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import cn.zszj.framework.test.core.ut.BaseMockitoUnitTest;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link OrgDataPermissionRule} 的单元测试（ZS-FC-002）——org 轴 SQL 列表静默过滤。
 *
 * <p>覆盖：表注册生效面；ALL 不拼接；orgIds 命中 IN 表达式；空范围且非 self 恒假（fail-closed）；
 * 未登录/非 ADMIN 护栏（与 DeptDataPermissionRule 同口径，非新增绕过面）；取不到权限 fail-loudly；
 * LoginUser 上下文缓存不重复计算（独立 CONTEXT_KEY，与 dept 轴/对象级 checker 不串扰）。
 *
 * @author ZS-FC-002
 */
class OrgDataPermissionRuleTest extends BaseMockitoUnitTest {

    private static final String TABLE = "bpm_first_chain_lead";

    @InjectMocks
    private OrgDataPermissionRule rule;

    @Mock
    private PermissionCommonApi permissionApi;

    private LoginUser adminUser(Long userId) {
        return java.util.Objects.requireNonNull(cn.zszj.framework.test.core.util.RandomUtils.randomPojo(
                LoginUser.class, o -> o.setId(userId).setUserType(UserTypeEnum.ADMIN.getValue())));
    }

    @Test // 注册表生效、未注册表不生效
    public void testGetExpression_unregisteredTable_returnsNull() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(adminUser(1L));
            rule.addOrgColumn(TABLE);
            assertNull(rule.getExpression("system_users", null));
        }
    }

    @Test // 未登录护栏：不拼接条件（与 dept 轴同口径）
    public void testGetExpression_noLoginUser_returnsNull() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(null);
            rule.addOrgColumn(TABLE);
            assertNull(rule.getExpression(TABLE, null));
        }
    }

    @Test // ALL 全量：不拼接条件
    public void testGetExpression_all_returnsNull() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            LoginUser loginUser = adminUser(1L);
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser);
            when(permissionApi.getOrgDataPermission(eq(1L))).thenReturn(new OrgDataPermissionRespDTO().setAll(true));
            rule.addOrgColumn(TABLE);

            assertNull(rule.getExpression(TABLE, null));
        }
    }

    @Test // orgIds 命中：拼接 org_id IN (...)
    public void testGetExpression_orgIdsHit_inExpression() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            LoginUser loginUser = adminUser(1L);
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser);
            when(permissionApi.getOrgDataPermission(eq(1L)))
                    .thenReturn(new OrgDataPermissionRespDTO().setOrgIds(SetUtils.asSet(100L, 200L)));
            rule.addOrgColumn(TABLE);

            Expression expression = rule.getExpression(TABLE, new Alias("t"));
            assertInstanceOf(InExpression.class, expression);
            assertEquals("t.org_id IN (100, 200)", expression.toString());
        }
    }

    @Test // 空范围且非 self：恒假（不返回数据，fail-closed）
    public void testGetExpression_emptyScopeAlwaysFalse() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            LoginUser loginUser = adminUser(1L);
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser);
            when(permissionApi.getOrgDataPermission(eq(1L)))
                    .thenReturn(new OrgDataPermissionRespDTO().setOrgIds(java.util.Set.of()).setSelf(false));
            rule.addOrgColumn(TABLE);

            Expression expression = rule.getExpression(TABLE, null);
            assertInstanceOf(EqualsTo.class, expression); // WHERE null = null → 空结果
        }
    }

    @Test // 上下文缓存：同 LoginUser 第二次查询不重复调用 api（独立 CONTEXT_KEY）
    public void testGetExpression_contextCache_singleApiCall() {
        try (MockedStatic<SecurityFrameworkUtils> ms = mockStatic(SecurityFrameworkUtils.class)) {
            LoginUser loginUser = adminUser(1L);
            ms.when(SecurityFrameworkUtils::getLoginUser).thenReturn(loginUser);
            when(permissionApi.getOrgDataPermission(eq(1L)))
                    .thenReturn(new OrgDataPermissionRespDTO().setOrgIds(SetUtils.asSet(100L)));
            rule.addOrgColumn(TABLE);

            rule.getExpression(TABLE, null);
            rule.getExpression(TABLE, null);
            verify(permissionApi, times(1)).getOrgDataPermission(eq(1L));
        }
    }

}
