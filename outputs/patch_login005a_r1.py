import io

root = r"E:\zszj-wt-login-005-a\services\zhongshu-core\zszj-module-system\src\main\java\cn\zszj\module\system"

# ===== 1) OAuth2AccessTokenMapper: authority count query (flushCache bypasses MyBatis SESSION local cache) =====
p = root + r"\dal\mysql\oauth2\OAuth2AccessTokenMapper.java"
s = io.open(p, encoding='utf-8').read()
s = s.replace("""import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;""", """import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;""")
anchor = """    @TenantIgnore // 获取 token 的时候，需要忽略租户编号。原因是：一些场景下，可能不会传递 tenant-id 请求头，例如说文件上传、积木报表等等
    default OAuth2AccessTokenDO selectByAccessToken(String accessToken) {"""
assert anchor in s
s = s.replace(anchor, """    /**
     * ZS-LOGIN-005.A：鉴权权威存在性核验——{@code flushCache=TRUE} 强制回源，绕过 MyBatis SESSION
     * 一级缓存（长事务内复用 SqlSession 会读到撤销前的旧快照）；忽略租户（鉴权权威跨租户）；
     * 仅统计未删除行（逻辑删除即已撤销）。
     */
    @TenantIgnore
    @Select("SELECT COUNT(1) FROM system_oauth2_access_token WHERE access_token = #{accessToken} AND deleted = 0")
    @Options(flushCache = Options.FlushCachePolicy.TRUE)
    int selectAuthorityCountByAccessToken(@Param("accessToken") String accessToken);

""" + anchor)
io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('access mapper ok')

# ===== 2) OAuth2RefreshTokenMapper: authority count query =====
p = root + r"\dal\mysql\oauth2\OAuth2RefreshTokenMapper.java"
s = io.open(p, encoding='utf-8').read()
s = s.replace("import org.apache.ibatis.annotations.Mapper;", """import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;""", 1)
if "import cn.zszj.framework.tenant.core.aop.TenantIgnore;" not in s:
    s = s.replace("import cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX;",
                  "import cn.zszj.framework.mybatis.core.query.LambdaQueryWrapperX;\nimport cn.zszj.framework.tenant.core.aop.TenantIgnore;")
anchor2 = "    @TenantIgnore // 与 selectByRefreshToken 保持一致忽略租户"
if anchor2 not in s:
    # find first method to anchor
    anchor2 = "    default OAuth2RefreshTokenDO selectByRefreshTokenForUpdate(String refreshToken) {"
assert anchor2 in s
s = s.replace(anchor2, """    /**
     * ZS-LOGIN-005.A：鉴权权威存在性核验（gate 合成令牌）——{@code flushCache=TRUE} 强制回源，
     * 绕过 MyBatis SESSION 一级缓存；忽略租户；仅统计未删除行。
     */
    @TenantIgnore
    @Select("SELECT COUNT(1) FROM system_oauth2_refresh_token WHERE refresh_token = #{refreshToken} AND deleted = 0")
    @Options(flushCache = Options.FlushCachePolicy.TRUE)
    int selectAuthorityCountByRefreshToken(@Param("refreshToken") String refreshToken);

""" + anchor2, 1)
io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('refresh mapper ok')
