package cn.zszj.framework.security.fixture;

import cn.zszj.framework.common.biz.system.oauth2.OAuth2TokenCommonApi;
import cn.zszj.framework.common.biz.system.oauth2.dto.OAuth2AccessTokenCheckRespDTO;
import cn.zszj.framework.common.biz.system.oauth2.dto.OAuth2AccessTokenCreateReqDTO;
import cn.zszj.framework.common.biz.system.oauth2.dto.OAuth2AccessTokenRespDTO;
import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ZS-SEC-012.A：Mock OAuth2TokenCommonApi，模拟双技术租户的 Token 校验。
 *
 * Token 命名约定（供测试用例使用）：
 * - "token-t1-admin"   → 租户 1 的管理员用户（userId=101, userType=2/ADMIN）
 * - "token-t2-admin"   → 租户 2 的管理员用户（userId=201, userType=2/ADMIN）
 * - "token-t1-member"  → 租户 1 的会员用户（userId=102, userType=1/MEMBER）
 * - "token-t1-noperm"  → 租户 1 的无权限管理员（userId=103, userType=2/ADMIN，无任何权限/角色）
 * - "token-t1-visitor" → 租户 1 的跨租户访问者（userId=104, userType=2/ADMIN，持 system:tenant:visit）
 * - "token-invalid"    → 无效 token（抛 ServiceException）
 * - "token-expired"    → 过期 token（抛 ServiceException）
 *
 * 该 Mock 不持久化任何状态，仅提供确定性的 Token → LoginUser 映射，
 * 使测试可以专注于 Security 过滤器链行为，而非 OAuth2 存储。
 */
public class MockOAuth2TokenApi implements OAuth2TokenCommonApi {

    /** 租户 1（技术租户 A） */
    public static final Long TENANT_1 = 1L;
    /** 租户 2（技术租户 B） */
    public static final Long TENANT_2 = 2L;

    /** 租户 1 管理员 */
    public static final Long USER_T1_ADMIN = 101L;
    /** 租户 2 管理员 */
    public static final Long USER_T2_ADMIN = 201L;
    /** 租户 1 会员 */
    public static final Long USER_T1_MEMBER = 102L;
    /** 租户 1 无权限管理员（ADMIN 类型但无任何权限/角色，用于纯粹的权限拒绝路径） */
    public static final Long USER_T1_NOPERM = 103L;
    /** 租户 1 跨租户访问者（持有 system:tenant:visit 权限，用于跨租户切换成功路径） */
    public static final Long USER_T1_VISITOR = 104L;

    /** ADMIN 用户类型 */
    public static final Integer USER_TYPE_ADMIN = 2;
    /** MEMBER 用户类型 */
    public static final Integer USER_TYPE_MEMBER = 1;

    private static final Map<String, OAuth2AccessTokenCheckRespDTO> TOKEN_MAP = new HashMap<>();

    static {
        // 租户 1 管理员
        TOKEN_MAP.put("token-t1-admin", buildToken(USER_T1_ADMIN, USER_TYPE_ADMIN, TENANT_1,
                List.of("system:user:query", "system:user:create"), "T1-Admin"));
        // 租户 2 管理员
        TOKEN_MAP.put("token-t2-admin", buildToken(USER_T2_ADMIN, USER_TYPE_ADMIN, TENANT_2,
                List.of("system:user:query"), "T2-Admin"));
        // 租户 1 会员（MEMBER 类型：访问 /admin-api 会因 userType 不匹配被拒）
        TOKEN_MAP.put("token-t1-member", buildToken(USER_T1_MEMBER, USER_TYPE_MEMBER, TENANT_1,
                List.of(), "T1-Member"));
        // 租户 1 无权限管理员（ADMIN 类型但无权限：用于验证纯粹的 403 权限拒绝）
        TOKEN_MAP.put("token-t1-noperm", buildToken(USER_T1_NOPERM, USER_TYPE_ADMIN, TENANT_1,
                List.of(), "T1-NoPerm"));
        // 租户 1 跨租户访问者（持有 system:tenant:visit：用于验证跨租户切换成功）
        TOKEN_MAP.put("token-t1-visitor", buildToken(USER_T1_VISITOR, USER_TYPE_ADMIN, TENANT_1,
                List.of("system:tenant:visit"), "T1-Visitor"));
    }

    private static OAuth2AccessTokenCheckRespDTO buildToken(Long userId, Integer userType, Long tenantId,
                                                             List<String> scopes, String nickname) {
        OAuth2AccessTokenCheckRespDTO dto = new OAuth2AccessTokenCheckRespDTO();
        dto.setUserId(userId);
        dto.setUserType(userType);
        dto.setTenantId(tenantId);
        dto.setScopes(scopes);
        dto.setExpiresTime(LocalDateTime.now().plusHours(1));
        Map<String, String> info = new HashMap<>();
        info.put("nickname", nickname);
        dto.setUserInfo(info);
        return dto;
    }

    @Override
    public OAuth2AccessTokenCheckRespDTO checkAccessToken(String accessToken) {
        // 无效 token
        if ("token-invalid".equals(accessToken)) {
            throw new ServiceException(GlobalErrorCodeConstants.UNAUTHORIZED.getCode(), "无效的访问令牌");
        }
        // 过期 token
        if ("token-expired".equals(accessToken)) {
            throw new ServiceException(GlobalErrorCodeConstants.UNAUTHORIZED.getCode(), "访问令牌已过期");
        }
        OAuth2AccessTokenCheckRespDTO dto = TOKEN_MAP.get(accessToken);
        if (dto == null) {
            // 未知 token 返回 null（TokenAuthenticationFilter 会当作未登录处理）
            return null;
        }
        return dto;
    }

    @Override
    public OAuth2AccessTokenRespDTO createAccessToken(OAuth2AccessTokenCreateReqDTO reqDTO) {
        throw new UnsupportedOperationException("Mock 不支持创建 token");
    }

    @Override
    public OAuth2AccessTokenRespDTO removeAccessToken(String accessToken) {
        throw new UnsupportedOperationException("Mock 不支持移除 token");
    }

    @Override
    public void removeAccessToken(Long userId, Integer userType) {
        // no-op
    }

    @Override
    public OAuth2AccessTokenRespDTO refreshAccessToken(String refreshToken, String clientId) {
        throw new UnsupportedOperationException("Mock 不支持刷新 token");
    }
}
