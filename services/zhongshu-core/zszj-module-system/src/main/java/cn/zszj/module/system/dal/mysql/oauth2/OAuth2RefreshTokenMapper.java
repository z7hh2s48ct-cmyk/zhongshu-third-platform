package cn.zszj.module.system.dal.mysql.oauth2;

import cn.hutool.core.collection.CollUtil;
import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zszj.framework.tenant.core.aop.TenantIgnore;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2RefreshTokenDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OAuth2RefreshTokenMapper extends BaseMapperX<OAuth2RefreshTokenDO> {

    default int deleteByRefreshToken(String refreshToken) {
        return delete(new LambdaQueryWrapperX<OAuth2RefreshTokenDO>()
                .eq(OAuth2RefreshTokenDO::getRefreshToken, refreshToken));
    }

    @TenantIgnore // 获取 token 的时候，需要忽略租户编号。原因是：一些场景下，可能不会传递 tenant-id 请求头，例如说文件上传、积木报表等等
    default OAuth2RefreshTokenDO selectByRefreshToken(String refreshToken) {
        return selectOne(OAuth2RefreshTokenDO::getRefreshToken, refreshToken);
    }

    /**
     * ZS-LOGIN-002：以「行锁」方式查询刷新令牌（生成 {@code SELECT ... FOR UPDATE}）。
     *
     * <p>用途：把「同一会话（同一 refreshToken）」的刷新与退出串行化。调用方<b>必须</b>处于事务中
     * （{@code @Transactional}），否则语句一结束即自动提交、锁立即释放，串行化失效。
     *
     * <p>选型说明（见开发计划 §3.3「优先行锁」）：
     * <ul>
     *     <li>行锁在 PG / MySQL / H2（MVStore）均为原生语义，无需新增 DB 列（避开 Flyway 迁移与 ZS-DB-019.B 耦合）；</li>
     *     <li>相比乐观锁「条件领取」（{@code UPDATE ... WHERE version=?}）不依赖版本号列；</li>
     *     <li>这里刻意使用 MyBatis-Plus 的 Wrapper + {@code last("FOR UPDATE")} 而非裸 {@code @Select}：
     *         裸 SQL 需要手写 resultMap（{@code user_info} 等 JSON 列依赖 autoResultMap 的 JacksonTypeHandler），
     *         且 {@code deleted = 0} 在 PG 的 boolean 列上不成立；走 Wrapper 由框架统一处理逻辑删除、
     *         字段映射与多方言。</li>
     * </ul>
     *
     * <p>锁序约定：{@code refreshAccessToken} 与 {@code removeAccessToken} 均遵循
     * 「<b>先锁刷新令牌行，再处理访问令牌</b>」，避免两条路径交叉加锁导致死锁。
     *
     * @param refreshToken 刷新令牌串
     * @return 刷新令牌；不存在（含已被退出删除）时返回 {@code null}，此时不会持有任何行锁
     */
    @TenantIgnore // 与 selectByRefreshToken 保持一致：退出/刷新可能不携带 tenant-id 请求头
    default OAuth2RefreshTokenDO selectByRefreshTokenForUpdate(String refreshToken) {
        LambdaQueryWrapperX<OAuth2RefreshTokenDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(OAuth2RefreshTokenDO::getRefreshToken, refreshToken);
        wrapper.last("FOR UPDATE");
        // 用 selectList 而非 selectOne：FOR UPDATE 需要明确的行集合语义，且历史上可能残留同串的软删除行
        List<OAuth2RefreshTokenDO> list = selectList(wrapper);
        return CollUtil.isEmpty(list) ? null : list.get(0);
    }

    /**
     * ZS-LOGIN-003：按「用户 + 用户类型」直查该用户名下的<b>全部</b>刷新令牌。
     *
     * <p>背景：此前 {@code OAuth2TokenServiceImpl#removeAccessToken(Long, Integer)} 的刷新凭据集合
     * 完全由 access-token 记录<b>反推</b>，于是「无 Access 记录但仍有 Refresh」的孤立刷新凭据
     * 永远不会被撤销 —— 它仍可用于 {@code refreshAccessToken} 换出全新访问令牌（会话复活）。
     * 本方法提供与 {@code OAuth2AccessTokenMapper#selectListByUserIdAndUserType} 对称的直查能力，
     * 使撤销可以取「access-token 反推 ∪ refresh-token 直查」的<b>全集</b>。
     *
     * <p>租户：撤销是安全操作，可能在无租户上下文（定时任务 / MQ 消费）或跨租户运维场景下发起，
     * 与 {@link #selectByRefreshToken(String)}、{@link #selectByRefreshTokenForUpdate(String)} 保持一致忽略租户。
     * 调用方（{@code OAuth2TokenServiceImpl}）另以 {@code TenantUtils#executeIgnore} 包裹整个撤销体作为主保障。
     *
     * @param userId   用户编号
     * @param userType 用户类型
     * @return 该用户名下的全部刷新令牌；无会话时返回空集合
     */
    @TenantIgnore // 与 selectByRefreshToken 保持一致：撤销可能不携带 tenant-id 请求头
    default List<OAuth2RefreshTokenDO> selectListByUserIdAndUserType(Long userId, Integer userType) {
        return selectList(OAuth2RefreshTokenDO::getUserId, userId,
                OAuth2RefreshTokenDO::getUserType, userType);
    }

    /**
     * 物理删除指定过期时间之前的刷新令牌
     *
     * @param expiresTime 最大时间
     * @param limit       删除条数，防止一次删除太多
     * @return 删除条数
     */
    @Delete("DELETE FROM system_oauth2_refresh_token WHERE id IN (SELECT id FROM system_oauth2_refresh_token WHERE expires_time < #{expiresTime} LIMIT #{limit})")
    Integer deleteByExpiresTimeLt(@Param("expiresTime") LocalDateTime expiresTime, @Param("limit") Integer limit);
}
