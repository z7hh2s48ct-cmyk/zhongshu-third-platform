package cn.zszj.module.system.service.oauth2;

import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.test.core.ut.BaseMockitoUnitTest;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2CodeDO;
import cn.zszj.module.system.dal.dataobject.user.AdminUserDO;
import cn.zszj.module.system.dal.mysql.user.AdminUserMapper;
import cn.zszj.module.system.service.auth.AdminAuthService;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static cn.hutool.core.util.RandomUtil.randomEle;
import static cn.zszj.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.zszj.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zszj.module.system.enums.ErrorCodeConstants.USER_NOT_EXISTS;
import static cn.zszj.framework.test.core.util.RandomUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * {@link OAuth2GrantServiceImpl} 的单元测试
 *
 * @author 芋道源码
 */
public class OAuth2GrantServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private OAuth2GrantServiceImpl oauth2GrantService;

    @Mock
    private OAuth2TokenService oauth2TokenService;
    @Mock
    private OAuth2CodeService oauth2CodeService;
    @Mock
    private AdminAuthService adminAuthService;
    @Mock
    private AdminUserMapper adminUserMapper;

    @Test
    public void testGrantImplicit() {
        // 准备参数
        Long userId = randomLongId();
        Integer userType = randomEle(UserTypeEnum.values()).getValue();
        String clientId = randomString();
        List<String> scopes = Lists.newArrayList("read", "write");
        // mock 方法
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.class);
        when(oauth2TokenService.createAccessToken(eq(userId), eq(userType),
                eq(clientId), eq(scopes))).thenReturn(accessTokenDO);

        // 调用，并断言
        assertPojoEquals(accessTokenDO, oauth2GrantService.grantImplicit(
                userId, userType, clientId, scopes));
    }

    @Test
    public void testGrantAuthorizationCodeForCode() {
        // 准备参数
        Long userId = randomLongId();
        Integer userType = UserTypeEnum.MEMBER.getValue();
        String clientId = randomString();
        List<String> scopes = Lists.newArrayList("read", "write");
        String redirectUri = randomString();
        String state = randomString();
        // mock 方法（userType 固定 MEMBER：发码路径不涉及兑换时账号状态校验，避免随机 ADMIN 触发无关分支）
        OAuth2CodeDO codeDO = randomPojo(OAuth2CodeDO.class);
        when(oauth2CodeService.createAuthorizationCode(eq(userId), eq(userType),
                eq(clientId), eq(scopes), eq(redirectUri), eq(state))).thenReturn(codeDO);

        // 调用，并断言
        assertEquals(codeDO.getCode(), oauth2GrantService.grantAuthorizationCodeForCode(userId, userType,
                clientId, scopes, redirectUri, state));
    }

    @Test
    public void testGrantAuthorizationCodeForAccessToken() {
        // 准备参数
        String clientId = randomString();
        String code = randomString();
        List<String> scopes = Lists.newArrayList("read", "write");
        String redirectUri = randomString();
        String state = randomString();
        // mock 方法（code）——技术账号（ADMIN）兑换时校验账号状态（ZS-LOGIN-003 codex r1 P1）
        OAuth2CodeDO codeDO = randomPojo(OAuth2CodeDO.class, o -> {
            o.setClientId(clientId);
            o.setRedirectUri(redirectUri);
            o.setState(state);
            o.setScopes(scopes);
            o.setUserType(UserTypeEnum.ADMIN.getValue());
        });
        when(oauth2CodeService.consumeAuthorizationCode(eq(code))).thenReturn(codeDO);
        when(adminUserMapper.selectByIdForUpdate(eq(codeDO.getUserId()))).thenReturn(
                new AdminUserDO().setId(codeDO.getUserId()).setStatus(CommonStatusEnum.ENABLE.getStatus()));
        // mock 方法（创建令牌）
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.class);
        when(oauth2TokenService.createAccessToken(eq(codeDO.getUserId()), eq(codeDO.getUserType()),
                eq(codeDO.getClientId()), eq(codeDO.getScopes()))).thenReturn(accessTokenDO);

        // 调用，并断言
        assertPojoEquals(accessTokenDO, oauth2GrantService.grantAuthorizationCodeForAccessToken(
                clientId, code, redirectUri, state));
    }

    @Test
    public void testGrantAuthorizationCodeForAccessToken_disabledUser_shouldReject() {
        // 准备参数
        String clientId = randomString();
        String code = randomString();
        String redirectUri = randomString();
        String state = randomString();
        // mock 方法（code 属已禁用的 ADMIN 用户）
        OAuth2CodeDO codeDO = randomPojo(OAuth2CodeDO.class, o -> {
            o.setClientId(clientId);
            o.setRedirectUri(redirectUri);
            o.setState(state);
            o.setUserType(UserTypeEnum.ADMIN.getValue());
        });
        when(oauth2CodeService.consumeAuthorizationCode(eq(code))).thenReturn(codeDO);
        when(adminUserMapper.selectByIdForUpdate(eq(codeDO.getUserId()))).thenReturn(
                new AdminUserDO().setId(codeDO.getUserId()).setStatus(CommonStatusEnum.DISABLE.getStatus()));

        // 调用，并断言：禁用账号不得凭旧授权码换出新会话
        assertServiceException(() -> oauth2GrantService.grantAuthorizationCodeForAccessToken(
                clientId, code, redirectUri, state), USER_NOT_EXISTS);
    }

    @Test
    public void testGrantAuthorizationCodeForAccessToken_deletedUser_shouldReject() {
        // 准备参数
        String clientId = randomString();
        String code = randomString();
        String redirectUri = randomString();
        String state = randomString();
        // mock 方法（code 属已被删除（查无）的 ADMIN 用户）
        OAuth2CodeDO codeDO = randomPojo(OAuth2CodeDO.class, o -> {
            o.setClientId(clientId);
            o.setRedirectUri(redirectUri);
            o.setState(state);
            o.setUserType(UserTypeEnum.ADMIN.getValue());
        });
        when(oauth2CodeService.consumeAuthorizationCode(eq(code))).thenReturn(codeDO);
        when(adminUserMapper.selectByIdForUpdate(eq(codeDO.getUserId()))).thenReturn(null);

        // 调用，并断言
        assertServiceException(() -> oauth2GrantService.grantAuthorizationCodeForAccessToken(
                clientId, code, redirectUri, state), USER_NOT_EXISTS);
    }

    @Test
    public void testGrantPassword() {
        // 准备参数
        String username = randomString();
        String password = randomString();
        String clientId = randomString();
        List<String> scopes = Lists.newArrayList("read", "write");
        // mock 方法(认证)
        AdminUserDO user = randomPojo(AdminUserDO.class);
        when(adminAuthService.authenticate(eq(username), eq(password))).thenReturn(user);
        // mock 方法（访问令牌）
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.class);
        when(oauth2TokenService.createAccessToken(eq(user.getId()), eq(UserTypeEnum.ADMIN.getValue()),
                eq(clientId), eq(scopes))).thenReturn(accessTokenDO);

        // 调用，并断言
        assertPojoEquals(accessTokenDO, oauth2GrantService.grantPassword(
                username, password, clientId, scopes));
    }

    @Test
    public void testGrantRefreshToken() {
        // 准备参数
        String refreshToken = randomString();
        String clientId = randomString();
        // mock 方法
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.class);
        when(oauth2TokenService.refreshAccessToken(eq(refreshToken), eq(clientId)))
                .thenReturn(accessTokenDO);

        // 调用，并断言
        assertPojoEquals(accessTokenDO, oauth2GrantService.grantRefreshToken(
                refreshToken, clientId));
    }

    @Test
    public void testRevokeToken_clientIdError() {
        // 准备参数
        String clientId = randomString();
        String accessToken = randomString();
        // mock 方法
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.class);
        when(oauth2TokenService.getAccessToken(eq(accessToken))).thenReturn(accessTokenDO);

        // 调用，并断言
        assertFalse(oauth2GrantService.revokeToken(clientId, accessToken));
    }

    @Test
    public void testRevokeToken_success() {
        // 准备参数
        String clientId = randomString();
        String accessToken = randomString();
        // mock 方法（访问令牌）
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.class).setClientId(clientId);
        when(oauth2TokenService.getAccessToken(eq(accessToken))).thenReturn(accessTokenDO);
        // mock 方法（移除）
        when(oauth2TokenService.removeAccessToken(eq(accessToken))).thenReturn(accessTokenDO);

        // 调用，并断言
        assertTrue(oauth2GrantService.revokeToken(clientId, accessToken));
    }

}
