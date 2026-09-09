package cn.zszj.framework.security.fixture;

import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.zszj.framework.tenant.core.service.TenantFrameworkService;

import java.util.Arrays;
import java.util.List;

/**
 * ZS-SEC-012.A：Mock TenantFrameworkService，模拟双技术租户的合法性校验。
 *
 * 租户配置：
 * - 租户 1（TENANT_1）：合法
 * - 租户 2（TENANT_2）：合法
 * - 租户 999：被禁用（用于测试禁用租户拒绝）
 * - 租户 998：已过期（用于测试过期租户拒绝）
 *
 * 该 Mock 提供确定性的租户校验，使测试可以专注于 TenantSecurityWebFilter 行为。
 */
public class MockTenantFrameworkService implements TenantFrameworkService {

    /** 被禁用的租户 */
    public static final Long TENANT_DISABLED = 999L;
    /** 已过期的租户 */
    public static final Long TENANT_EXPIRED = 998L;

    @Override
    public List<Long> getTenantIds() {
        return Arrays.asList(MockOAuth2TokenApi.TENANT_1, MockOAuth2TokenApi.TENANT_2);
    }

    @Override
    public void validTenant(Long id) {
        if (id == null) {
            throw new ServiceException(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), "租户编号不能为空");
        }
        // 合法租户
        if (MockOAuth2TokenApi.TENANT_1.equals(id) || MockOAuth2TokenApi.TENANT_2.equals(id)) {
            return;
        }
        // 被禁用的租户
        if (TENANT_DISABLED.equals(id)) {
            throw new ServiceException(GlobalErrorCodeConstants.FORBIDDEN.getCode(), "租户已被禁用");
        }
        // 已过期的租户
        if (TENANT_EXPIRED.equals(id)) {
            throw new ServiceException(GlobalErrorCodeConstants.FORBIDDEN.getCode(), "租户已过期");
        }
        // 未知租户
        throw new ServiceException(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), "租户不存在: " + id);
    }
}
