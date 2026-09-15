package cn.zszj.module.system.service.oauth2;

import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.module.system.controller.admin.oauth2.vo.token.OAuth2AccessTokenPageReqVO;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;

import java.util.List;

/**
 * OAuth2.0 Token Service 接口
 *
 * 从功能上，和 Spring Security OAuth 的 DefaultTokenServices + JdbcTokenStore 的功能，提供访问令牌、刷新令牌的操作
 *
 * @author 芋道源码
 */
public interface OAuth2TokenService {

    /**
     * 创建访问令牌
     * 注意：该流程中，会包含创建刷新令牌的创建
     *
     * 参考 DefaultTokenServices 的 createAccessToken 方法
     *
     * @param userId 用户编号
     * @param userType 用户类型
     * @param clientId 客户端编号
     * @param scopes 授权范围
     * @return 访问令牌的信息
     */
    OAuth2AccessTokenDO createAccessToken(Long userId, Integer userType, String clientId, List<String> scopes);

    /**
     * 刷新访问令牌
     *
     * 参考 DefaultTokenServices 的 refreshAccessToken 方法
     *
     * @param refreshToken 刷新令牌
     * @param clientId 客户端编号
     * @return 访问令牌的信息
     */
    OAuth2AccessTokenDO refreshAccessToken(String refreshToken, String clientId);

    /**
     * 获得访问令牌
     *
     * 参考 DefaultTokenServices 的 getAccessToken 方法
     *
     * @param accessToken 访问令牌
     * @return 访问令牌的信息
     */
    OAuth2AccessTokenDO getAccessToken(String accessToken);

    /**
     * 校验访问令牌
     *
     * @param accessToken 访问令牌
     * @return 访问令牌的信息
     */
    OAuth2AccessTokenDO checkAccessToken(String accessToken);

    /**
     * 移除访问令牌
     * 注意：该流程中，会移除相关的刷新令牌
     *
     * 参考 DefaultTokenServices 的 revokeToken 方法
     *
     * @param accessToken 刷新令牌
     * @return 访问令牌的信息
     */
    OAuth2AccessTokenDO removeAccessToken(String accessToken);

    /**
     * 移除访问令牌
     * 注意：该流程中，会移除相关的刷新令牌
     *
     * 参考 DefaultTokenServices 的 revokeToken 方法
     *
     * @param userId 用户编号
     * @param userType 用户类型
     */
    void removeAccessToken(Long userId, Integer userType);

    /**
     * 移除访问令牌（按会话 ID，ZS-LOGIN-006）
     *
     * <p>以「不可用于认证的会话 ID（访问令牌 DB 主键）」定位并撤销会话，替代以原始 accessToken 串标识踢出，
     * 避免管理端 / 前端持有可用凭据。天然租户作用域（{@code selectById} 受租户拦截器约束），并显式复核
     * 当前租户上下文作为纵深防御；跨租户 / 不存在的 ID 幂等返回 {@code null}（不回显存在性，杜绝跨租户探测）。
     *
     * @param id             会话 ID（访问令牌主键，不可用于认证）
     * @param expectedUserId 期望归属用户编号：非空时校验会话归属（自助撤销只能操作本人会话，
     *                       非本人抛 {@code OAUTH2_TOKEN_SESSION_NOT_OWNED}）；为空表示管理员撤销（不受归属限制）
     * @return 被撤销的访问令牌信息；不存在 / 跨租户时返回 {@code null}
     */
    OAuth2AccessTokenDO removeAccessTokenById(Long id, Long expectedUserId);

    /**
     * 获得访问令牌分页
     *
     * @param reqVO 请求
     * @return 访问令牌分页
     */
    PageResult<OAuth2AccessTokenDO> getAccessTokenPage(OAuth2AccessTokenPageReqVO reqVO);

    /**
     * 清理过期 exceedDay 天的刷新令牌
     *
     * @param exceedDay   过期多少天就进行清理
     * @param deleteLimit 清理的间隔条数
     */
    Integer cleanRefreshToken(Integer exceedDay, Integer deleteLimit);

    /**
     * 清理过期 exceedDay 天的访问令牌
     *
     * @param exceedDay   过期多少天就进行清理
     * @param deleteLimit 清理的间隔条数
     */
    Integer cleanAccessToken(Integer exceedDay, Integer deleteLimit);
}
