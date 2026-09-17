package cn.zszj.module.system.dal.mysql.oauth2;

import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.framework.mybatis.core.mapper.BaseMapperX;
import cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zszj.framework.tenant.core.aop.TenantIgnore;
import cn.zszj.module.system.controller.admin.oauth2.vo.token.OAuth2AccessTokenPageReqVO;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import cn.zszj.module.system.framework.outbox.TokenRowSnapshot;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OAuth2AccessTokenMapper extends BaseMapperX<OAuth2AccessTokenDO> {

    /**
     * ZS-LOGIN-005.A：鉴权权威存在性核验——{@code flushCache=TRUE} 强制回源，绕过 MyBatis SESSION
     * 一级缓存（长事务内复用 SqlSession 会读到撤销前的旧快照）；忽略租户（鉴权权威跨租户）；
     * 仅统计未删除行（逻辑删除即已撤销）。
     */
    @TenantIgnore
    @Select("SELECT COUNT(1) FROM system_oauth2_access_token WHERE access_token = #{accessToken} AND deleted = 0")
    @Options(flushCache = Options.FlushCachePolicy.TRUE)
    int selectAuthorityCountByAccessToken(@Param("accessToken") String accessToken);

    @TenantIgnore // 获取 token 的时候，需要忽略租户编号。原因是：一些场景下，可能不会传递 tenant-id 请求头，例如说文件上传、积木报表等等
    default OAuth2AccessTokenDO selectByAccessToken(String accessToken) {
        return selectOne(OAuth2AccessTokenDO::getAccessToken, accessToken);
    }

    default List<OAuth2AccessTokenDO> selectListByRefreshToken(String refreshToken) {
        return selectList(OAuth2AccessTokenDO::getRefreshToken, refreshToken);
    }

    default PageResult<OAuth2AccessTokenDO> selectPage(OAuth2AccessTokenPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<OAuth2AccessTokenDO>()
                .eqIfPresent(OAuth2AccessTokenDO::getUserId, reqVO.getUserId())
                .eqIfPresent(OAuth2AccessTokenDO::getUserType, reqVO.getUserType())
                .likeIfPresent(OAuth2AccessTokenDO::getClientId, reqVO.getClientId())
                .gt(OAuth2AccessTokenDO::getExpiresTime, LocalDateTime.now())
                .orderByDesc(OAuth2AccessTokenDO::getId));
    }

    default List<OAuth2AccessTokenDO> selectListByUserIdAndUserType(Long userId, Integer userType) {
        return selectList(OAuth2AccessTokenDO::getUserId, userId,
                OAuth2AccessTokenDO::getUserType, userType);
    }

    /**
     * 物理删除指定过期时间之前的访问令牌
     *
     * @param expiresTime 最大时间
     * @param limit       删除条数，防止一次删除太多
     * @return 删除条数
     */
    @Delete("DELETE FROM system_oauth2_access_token WHERE id IN (SELECT id FROM system_oauth2_access_token WHERE expires_time < #{expiresTime} LIMIT #{limit})")
    Integer deleteByExpiresTimeLt(@Param("expiresTime") LocalDateTime expiresTime, @Param("limit") Integer limit);

    /**
     * ZS-LOGIN-005.B：按主键反查令牌行快照（穿透逻辑删除）——供
     * {@link cn.zszj.module.system.framework.outbox.OAuth2TokenRevocationCompensationSink} 幂等重放时判定：
     * <ul>
     *   <li>{@code deleted=TRUE}：撤销已生效，重放 {@code markRevoked + delete}（幂等）；</li>
     *   <li>{@code deleted=FALSE} 且 {@code expiresTime > now}：业务事务已回滚（token 仍活跃），
     *       <b>静默 skip</b>（防误撤销有效凭据）；</li>
     *   <li>返回 {@code null}：行已物理清理，静默 skip。</li>
     * </ul>
     *
     * <p>{@code flushCache=TRUE} 强制回源，绕过 MyBatis SESSION 一级缓存（dispatcher 多实例重放时
     * 需读到已提交的最新 {@code deleted} 状态）；{@code @TenantIgnore} 与鉴权权威核验保持一致（撤销
     * 可能跨租户运维发起）。
     *
     * @param id 令牌 DB 主键
     * @return 行快照；行不存在（已物理删除）时返回 {@code null}
     */
    @TenantIgnore
    @Select("SELECT access_token AS token, deleted, expires_time AS expiresTime "
            + "FROM system_oauth2_access_token WHERE id = #{id}")
    @Options(flushCache = Options.FlushCachePolicy.TRUE)
    TokenRowSnapshot selectByIdIncludeDeleted(@Param("id") Long id);

}
