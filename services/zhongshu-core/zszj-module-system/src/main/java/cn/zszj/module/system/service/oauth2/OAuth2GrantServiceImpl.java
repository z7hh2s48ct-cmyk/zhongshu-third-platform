package cn.zszj.module.system.service.oauth2;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.zszj.framework.common.enums.CommonStatusEnum;
import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2CodeDO;
import cn.zszj.module.system.dal.dataobject.user.AdminUserDO;
import cn.zszj.module.system.enums.ErrorCodeConstants;
import cn.zszj.module.system.dal.mysql.oauth2.OAuth2CodeMapper;
import cn.zszj.module.system.dal.mysql.user.AdminUserMapper;
import cn.zszj.module.system.service.auth.AdminAuthService;
import cn.zszj.framework.common.util.date.DateUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.List;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.module.system.enums.ErrorCodeConstants.OAUTH2_CODE_EXPIRE;
import static cn.zszj.module.system.enums.ErrorCodeConstants.OAUTH2_CODE_NOT_EXISTS;

/**
 * OAuth2 授予 Service 实现类
 *
 * @author 芋道源码
 */
@Service
public class OAuth2GrantServiceImpl implements OAuth2GrantService {

    @Resource
    private OAuth2TokenService oauth2TokenService;
    @Resource
    private OAuth2CodeService oauth2CodeService;
    @Resource
    private AdminAuthService adminAuthService;
    @Resource
    private AdminUserMapper adminUserMapper;
    @Resource
    private OAuth2CodeMapper oauth2CodeMapper;

    @Override
    public OAuth2AccessTokenDO grantImplicit(Long userId, Integer userType,
                                             String clientId, List<String> scopes) {
        return oauth2TokenService.createAccessToken(userId, userType, clientId, scopes);
    }

    @Override
    public String grantAuthorizationCodeForCode(Long userId, Integer userType,
                                                String clientId, List<String> scopes,
                                                String redirectUri, String state) {
        return oauth2CodeService.createAuthorizationCode(userId, userType, clientId, scopes,
                redirectUri, state).getCode();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OAuth2AccessTokenDO grantAuthorizationCodeForAccessToken(String clientId, String code,
                                                                    String redirectUri, String state) {
        // ZS-LOGIN-003 codex r3 P1（锁序）：先【只读】获取 code（不加行锁）以确定归属用户，
        // 统一锁序固定为「用户行锁 → code 行锁 → 令牌」；若先消费 code（持有 code 行锁）再取用户行锁，
        // 会与「撤销（持用户行锁）删除同一 code」形成锁环，令禁用/改密事务回滚。
        OAuth2CodeDO codeDO = oauth2CodeMapper.selectByCode(code);
        if (codeDO == null) {
            throw exception(OAUTH2_CODE_NOT_EXISTS);
        }
        if (DateUtils.isExpired(codeDO.getExpiresTime())) {
            throw exception(OAUTH2_CODE_EXPIRE);
        }
        Assert.notNull(codeDO, "授权码不能为空"); // 防御性编程
        // 校验 clientId 是否匹配
        if (!StrUtil.equals(clientId, codeDO.getClientId())) {
            throw exception(ErrorCodeConstants.OAUTH2_GRANT_CLIENT_ID_MISMATCH);
        }
        // 校验 redirectUri 是否匹配
        if (!StrUtil.equals(redirectUri, codeDO.getRedirectUri())) {
            throw exception(ErrorCodeConstants.OAUTH2_GRANT_REDIRECT_URI_MISMATCH);
        }
        // 校验 state 是否匹配
        state = StrUtil.nullToDefault(state, ""); // 数据库 state 为 null 时，会设置为 "" 空串
        if (!StrUtil.equals(state, codeDO.getState())) {
            throw exception(ErrorCodeConstants.OAUTH2_GRANT_STATE_MISMATCH);
        }

        // ZS-LOGIN-003 codex r1 P1：兑换时校验账号状态——授权码签发后、兑换前账号可能已被禁用/删除，
        // 不校验则撤销完成后仍能凭旧 code 换出可用新会话（撤销后复活）。技术账号（ADMIN）先闭环，
        // 会员（MEMBER）兑换状态校验归其模块任务。
        if (UserTypeEnum.ADMIN.getValue().equals(codeDO.getUserType())) {
            // ZS-LOGIN-003 codex r2 P1：在消费 code 之前先取用户行锁（与用户级撤销
            // doRemoveAccessTokenByUser、账号状态更新同一把行锁，统一锁序最外层），锁内重读状态——
            // 撤销与兑换的交错只有两种结果：兑换先提交（新会话随后被撤销覆盖）或撤销先提交（兑换被拒）。
            // 会员（MEMBER）账号的状态校验归其模块任务
            AdminUserDO user = adminUserMapper.selectByIdForUpdate(codeDO.getUserId());
            if (user == null || CommonStatusEnum.isDisable(user.getStatus())) {
                throw exception(ErrorCodeConstants.USER_NOT_EXISTS);
            }
        }

        // 原子消费授权码（ZS-LOGIN-003 codex r3 P1：条件删除恰好 1 行才放行，防撤销后兑换/重复兑换）；
        // 对 ADMIN 此调用发生在用户行锁内，锁序保持「用户 → code → 令牌」
        oauth2CodeService.consumeAuthorizationCode(code);

        // 创建访问令牌
        return oauth2TokenService.createAccessToken(codeDO.getUserId(), codeDO.getUserType(),
                codeDO.getClientId(), codeDO.getScopes());
    }

    @Override
    public OAuth2AccessTokenDO grantPassword(String username, String password, String clientId, List<String> scopes) {
        // 使用账号 + 密码进行登录
        AdminUserDO user = adminAuthService.authenticate(username, password);
        Assert.notNull(user, "用户不能为空！"); // 防御性编程

        // 创建访问令牌
        return oauth2TokenService.createAccessToken(user.getId(), UserTypeEnum.ADMIN.getValue(), clientId, scopes);
    }

    @Override
    public OAuth2AccessTokenDO grantRefreshToken(String refreshToken, String clientId) {
        return oauth2TokenService.refreshAccessToken(refreshToken, clientId);
    }

    @Override
    public OAuth2AccessTokenDO grantClientCredentials(String clientId, List<String> scopes) {
        // 特殊：https://yuanbao.tencent.com/bot/app/share/chat/wFj642xSZHHx
        return oauth2TokenService.createAccessToken(0L, UserTypeEnum.ADMIN.getValue(), clientId, scopes);
    }

    @Override
    public boolean revokeToken(String clientId, String accessToken) {
        // 先查询，保证 clientId 时匹配的
        OAuth2AccessTokenDO accessTokenDO = oauth2TokenService.getAccessToken(accessToken);
        if (accessTokenDO == null || ObjectUtil.notEqual(clientId, accessTokenDO.getClientId())) {
            return false;
        }
        // 再删除
        return oauth2TokenService.removeAccessToken(accessToken) != null;
    }

}
