node.exe : OpenAI Codex v0.154.0
所在位置 C:\Users\Administrator\AppData\Roaming\npm\codex.ps1:24 字符: 5
+     & "node$exe"  "$basedir/node_modules/@openai/codex/bin/codex.js"  ...
+     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    + CategoryInfo          : NotSpecified: (OpenAI Codex v0.154.0:String) [], 
    RemoteException
    + FullyQualifiedErrorId : NativeCommandError
 
--------
workdir: E:\众墅之家AI赋能平台底座
model: gpt-6-astra
provider: openai
approval: never
sandbox: read-only
reasoning effort: xhigh
reasoning summaries: none
session id: 01a0947f-a3a2-77e2-b9ee-90e0dc1f1ca2
--------
user
commit 28257a2
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'Get-
Location; git status --short; git rev-parse --show-toplevel; git show --stat --
oneline --decorate --no-renames 28257a25; Get-ChildItem -Force -Name' in E:\众墅之
家AI赋能平台底座
 succeeded in 38ms:
E:/众墅之家AI赋能平台底座
28257a25 feat(login-001): 令牌用途分离门控（代码默认false，现网WS迁移期gate=true兼容）[ZS-LOGIN-001]
 .../service/oauth2/OAuth2TokenServiceImpl.java     | 30 +++++++-
 .../oauth2/OAuth2TokenServiceImplCompatTest.java   | 88 ++++++++++++++++++++++
 .../service/oauth2/OAuth2TokenServiceImplTest.java | 47 +++++++++++-
 .../src/main/resources/application.yaml            |  5 ++
 4 files changed, 166 insertions(+), 4 deletions(-)
Path                     
----                     
E:\����֮��AI����ƽ̨����
.git
.github
.omx
.tmp-iam003
.tmp-sec011
apps
docs
outputs
reference
scripts
services
third_party
tools
.gitignore
README.md
THIRD_PARTY_NOTICES.md



exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "git 
show --format=fuller --no-ext-diff --no-renames 28257a25; rg --files --hidden -
g 'AGENTS*' -g '"'!**/.git/**'"' -g '"'!**/node_modules/**'"' -g '"'!**/target/
**'"'; Get-Item -LiteralPath 'E:\\AGENTS.override.md','E:\\AGENTS.md','E:\\众墅之家
AI赋能平台底座\\AGENTS.override.md','E:\\众墅之家AI赋能平台底座\\AGENTS.md' -ErrorAction Silent
lyContinue | Select-Object FullName" in E:\众墅之家AI赋能平台底座
 exited 1 in 25ms:
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command '$Out
putEncoding = [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new(); gi
t log -6 --oneline; git diff 28257a25 --stat; rg --files --hidden --no-ignore -
g '"'AGENTS.override.md' -g 'AGENTS.md' -g '"'!**/.git/**'"' -g '"'!**/node_mod
ules/**'"' -g '"'!**/target/**'"'; Get-Content -Encoding UTF8 -LiteralPath 'ser
vices/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/serv
ice/oauth2/OAuth2TokenServiceImpl.java'; Get-Content -Encoding UTF8 -LiteralPat
h 'services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/syste
m/dal/redis/oauth2/OAuth2AccessTokenRedisDAO.java'; Get-Content -Encoding UTF8 
-LiteralPath 'services/zhongshu-core/pom.xml'" in E:\众墅之家AI赋能平台底座
 succeeded in 97ms:
 README.md                                          |    4 +-
 ...273\273\345\212\241\346\270\205\345\215\225.md" |   18 +-
 docs/reviews/README.md                             |    6 +-
 docs/reviews/codex-ZS-CFG-001.B-r1.raw.md          |  Bin 0 -> 2351024 bytes
 docs/reviews/codex-ZS-CFG-001.B-r2.raw.md          |  Bin 0 -> 118812 bytes
 docs/reviews/codex-ZS-CFG-001.B.md                 |   86 +
 docs/reviews/codex-ZS-CFG-001.B.raw.md             |  Bin 0 -> 105130 bytes
 docs/reviews/codex-ZS-SEC-004-r1.raw.md            |  640 ++
 docs/reviews/codex-ZS-SEC-004.md                   |   69 +
 docs/reviews/codex-ZS-SEC-004.raw.md               | 2202 +++++
 docs/reviews/codex-ZS-SEC-010-r1.raw.md            |  Bin 0 -> 168394 bytes
 docs/reviews/codex-ZS-SEC-010.md                   |   70 +
 docs/reviews/codex-ZS-SEC-010.raw.md               |  Bin 0 -> 714918 bytes
 docs/reviews/codex-ZS-SEC-011.A.md                 |   91 +
 docs/reviews/codex-ZS-SEC-011.A.r1.raw.md          | 9278 ++++++++++++++++++++
 docs/reviews/codex-ZS-SEC-011.A.r2.raw.md          | 3032 +++++++
 docs/reviews/codex-ZS-SEC-011.A.r3.raw.md          | 1244 +++
 docs/reviews/codex-ZS-SEC-011.A.raw.md             | 2630 ++++++
 .../idempotent/core/aop/IdempotentAspect.java      |  107 +-
 .../impl/DefaultIdempotentKeyResolver.java         |   26 +-
 .../idempotent/core/redis/IdempotentRedisDAO.java  |   29 +-
 .../ratelimiter/core/annotation/RateLimiter.java   |   10 +
 .../ratelimiter/core/aop/RateLimiterAspect.java    |   32 +-
 .../impl/ExpressionRateLimiterKeyResolver.java     |   21 +-
 .../core/redis/RateLimiterRedisDAO.java            |   17 +-
 .../idempotent/core/aop/IdempotentAspectTest.java  |  242 +-
 .../impl/DefaultIdempotentKeyResolverTest.java     |  251 +
 .../core/aop/RateLimiterAspectTest.java            |  114 +
 .../impl/ExpressionRateLimiterKeyResolverTest.java |  215 +
 .../config/ZszjWebSecurityConfigurerAdapter.java   |   13 +-
 .../zszj/framework/web/config/WebProperties.java   |   63 +
 .../web/config/ZszjWebAutoConfiguration.java       |   16 +-
 .../zszj/framework/web/config/CorsConfigTest.java  |   68 +
 .../controller/admin/config/ConfigController.java  |   21 +-
 .../module/infra/enums/ErrorCodeConstants.java     |    2 +
 .../service/config/ConfigSensitiveClassifier.java  |  126 +
 .../module/infra/service/config/ConfigService.java |   19 +
 .../infra/service/config/ConfigServiceImpl.java    |   43 +-
 .../config/ConfigSensitiveClassifierTest.java      |   64 +
 .../service/config/ConfigServiceImplMaskTest.java  |  193 +
 .../service/config/ConfigServiceImplTest.java      |    2 +-
 services/zhongshu-core/zszj-module-system/pom.xml  |    6 +
 .../controller/admin/auth/AuthController.java      |   14 +-
 .../module/system/dal/mysql/dept/DeptMapper.java   |   12 +
 .../system/dal/mysql/user/AdminUserMapper.java     |   12 +
 .../module/system/enums/ErrorCodeConstants.java    |    2 +
 .../system/service/dept/DeptServiceImpl.java       |   12 +-
 .../system/service/user/AdminUserServiceImpl.java  |   28 +-
 .../system/service/dept/DeptServiceImplTest.java   |   48 +
 .../service/user/AdminUserServiceImplTest.java     |   63 +
 .../src/main/resources/application-local.yaml      |    3 +
 .../src/main/resources/application.yaml            |    8 +
 52 files changed, 21220 insertions(+), 52 deletions(-)
package cn.zszj.module.system.service.oauth2;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.framework.common.util.date.DateUtils;
import cn.zszj.framework.common.util.object.BeanUtils;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.framework.tenant.core.util.TenantUtils;
import cn.zszj.module.system.controller.admin.oauth2.vo.token.OAuth2AccessToken
PageReqVO;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2ClientDO;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2RefreshTokenDO;
import cn.zszj.module.system.dal.dataobject.user.AdminUserDO;
import cn.zszj.module.system.dal.mysql.oauth2.OAuth2AccessTokenMapper;
import cn.zszj.module.system.dal.mysql.oauth2.OAuth2RefreshTokenMapper;
import cn.zszj.module.system.dal.redis.oauth2.OAuth2AccessTokenRedisDAO;
import cn.zszj.module.system.service.user.AdminUserService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exce
ption0;
import static cn.zszj.framework.common.util.collection.CollectionUtils.convertS
et;

/**
 * OAuth2.0 Token Service ʵ����
 *
 * @author ���Դ��
 */
@Slf4j
@Service
public class OAuth2TokenServiceImpl implements OAuth2TokenService {

    @Resource
    private OAuth2AccessTokenMapper oauth2AccessTokenMapper;
    @Resource
    private OAuth2RefreshTokenMapper oauth2RefreshTokenMapper;

    @Resource
    private OAuth2AccessTokenRedisDAO oauth2AccessTokenRedisDAO;

    @Resource
    private OAuth2ClientService oauth2ClientService;
    @Resource
    @Lazy // �����أ�����ѭ������
    private AdminUserService adminUserService;

    /**
     * ZS-LOGIN-001��������;�����ſؿ��ء����Ƿ������ѡ�ˢ�����ơ���Ĭ�������������ơ�ʹ�
á�
     * <p>����Ĭ�� {@code false}����ȫĬ�ϣ�{@link #getAccessToken} ֻ�Ϸ������ƣ�ˢ���
��Ƶ����������� �� ���� null �� checkAccessToken �� UNAUTHORIZED����
     * <p><b>����Ϊ����ʱ�� true��</b>admin-web IM �� miniapp IM/�ͷ��� WebSocket 
������ {@code ?token=<refreshToken>} ��ƾ��
     * ������� WebSocket �����Զ��� Header��ֻ��ƴ URL �����������������˷��У����� 
LOGIN-001.B����ʱ����Ʊ�� + ǰ�� WS Ǩ�ƣ����ǰ���� true �Բ��ƻ��������ӡ�
     * ע����ľ������ {@code X-Access-Token} ��������ƣ�<b>��</b>���������ˡ�
     * <p><b>�������գ�</b>ת������"��������"�̳�ˢ������ TTL��default client �� 30
 �죩������ system_oauth2_access_token ��
     * �����������Ʒ�ҳ/�߳����ɼ�����{@link #removeAccessToken(String)} �޷��������
��� Redis TTL ��Ȼ���ڡ��ʽ�ΪǨ������ʱ���ݡ�
     */
    @Value("${zszj.security.refresh-token-as-access-token-enabled:false}")
    private boolean refreshTokenAsAccessTokenEnabled;

    @PostConstruct
    public void warnRefreshTokenAsAccessTokenCompat() {
        if (refreshTokenAsAccessTokenEnabled) {
            log.warn("[warnRefreshTokenAsAccessTokenCompat][ZS-LOGIN-001 ������
;������ݿ����ѿ���"
                    + "��zszj.security.refresh-token-as-access-token-enabled=tr
ue����ˢ�����ƿɱ�������������ʹ�ã�"
                    + "��������Ч���̳�ˢ������ TTL��������⡢removeAccessToken �޷��
��������� Redis TTL ���ڵ�ƾ�ݡ�"
                    + "��Ϊ admin-web IM / miniapp IM���ͷ� WebSocket Ǩ������ʱ���
ݣ�"
                    + "���� LOGIN-001.B ��ʱƱ��Ǩ����ɺ�ر�]");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OAuth2AccessTokenDO createAccessToken(Long userId, Integer userType,
 String clientId, List<String> scopes) {
        OAuth2ClientDO clientDO = oauth2ClientService.validOAuthClientFromCache
(clientId);
        // ����ˢ������
        OAuth2RefreshTokenDO refreshTokenDO = createOAuth2RefreshToken(userId, 
userType, clientDO, scopes);
        // ������������
        return createOAuth2AccessToken(refreshTokenDO, clientDO);
    }

    @Override
    @Transactional(noRollbackFor = ServiceException.class)
    public OAuth2AccessTokenDO refreshAccessToken(String refreshToken, String c
lientId) {
        // ��ѯ��������
        OAuth2RefreshTokenDO refreshTokenDO = oauth2RefreshTokenMapper.selectBy
RefreshToken(refreshToken);
        if (refreshTokenDO == null) {
            throw exception0(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), "�
�Ч��ˢ������");
        }

        // У�� Client ƥ��
        OAuth2ClientDO clientDO = oauth2ClientService.validOAuthClientFromCache
(clientId);
        if (ObjectUtil.notEqual(clientId, refreshTokenDO.getClientId())) {
            throw exception0(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), "ˢ
�����ƵĿͻ��˱�Ų���ȷ");
        }

        // �Ƴ���صķ�������
        List<OAuth2AccessTokenDO> accessTokenDOs = oauth2AccessTokenMapper.sele
ctListByRefreshToken(refreshToken);
        if (CollUtil.isNotEmpty(accessTokenDOs)) {
            oauth2AccessTokenMapper.deleteByIds(convertSet(accessTokenDOs, OAut
h2AccessTokenDO::getId));
            oauth2AccessTokenRedisDAO.deleteList(convertSet(accessTokenDOs, OAu
th2AccessTokenDO::getAccessToken));
        }

        // �ѹ��ڵ�����£�ɾ��ˢ������
        if (DateUtils.isExpired(refreshTokenDO.getExpiresTime())) {
            oauth2RefreshTokenMapper.deleteById(refreshTokenDO.getId());
            throw exception0(GlobalErrorCodeConstants.UNAUTHORIZED.getCode(), "
ˢ�������ѹ���");
        }

        // ������������
        return createOAuth2AccessToken(refreshTokenDO, clientDO);
    }

    @Override
    public OAuth2AccessTokenDO getAccessToken(String accessToken) {
        // ���ȴ� Redis �л�ȡ
        OAuth2AccessTokenDO accessTokenDO = oauth2AccessTokenRedisDAO.get(acces
sToken);
        if (accessTokenDO != null) {
            return accessTokenDO;
        }

        // ��ȡ�������� MySQL �л�ȡ��������
        accessTokenDO = oauth2AccessTokenMapper.selectByAccessToken(accessToken
);
        // ZS-LOGIN-001��������;���룬Ĭ���ſعر�ʱ���پ�Ĭ��ˢ�����Ƶ���������
        if (accessTokenDO == null && refreshTokenAsAccessTokenEnabled) {
            // ���⣺�� MySQL �л�ȡˢ�����ơ�ԭ�򣺽�����ֳ���������ˢ�·������Ƴ���
            // ����˵����ľ����ֻ�������� token������������ refresh_token�������޷�
ˢ�·�������
            // ������˵��ǰ�� WebSocket �� token ֱ�Ӹ��� url �ϣ��޷����� refresh_to
ken
            OAuth2RefreshTokenDO refreshTokenDO = oauth2RefreshTokenMapper.sele
ctByRefreshToken(accessToken);
            if (refreshTokenDO != null && !DateUtils.isExpired(refreshTokenDO.g
etExpiresTime())) {
                accessTokenDO = convertToAccessToken(refreshTokenDO);
            }
        }

        // ����� MySQL ���ڣ����� Redis ��д��
        if (accessTokenDO != null && !DateUtils.isExpired(accessTokenDO.getExpi
resTime())) {
            oauth2AccessTokenRedisDAO.set(accessTokenDO);
        }
        return accessTokenDO;
    }

    @Override
    public OAuth2AccessTokenDO checkAccessToken(String accessToken) {
        OAuth2AccessTokenDO accessTokenDO = getAccessToken(accessToken);
        if (accessTokenDO == null) {
            throw exception0(GlobalErrorCodeConstants.UNAUTHORIZED.getCode(), "
�������Ʋ�����");
        }
        if (DateUtils.isExpired(accessTokenDO.getExpiresTime())) {
            throw exception0(GlobalErrorCodeConstants.UNAUTHORIZED.getCode(), "
���������ѹ���");
        }
        return accessTokenDO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OAuth2AccessTokenDO removeAccessToken(String accessToken) {
        // ɾ����������
        OAuth2AccessTokenDO accessTokenDO = oauth2AccessTokenMapper.selectByAcc
essToken(accessToken);
        if (accessTokenDO == null) {
            return null;
        }
        oauth2AccessTokenMapper.deleteById(accessTokenDO.getId());
        oauth2AccessTokenRedisDAO.delete(accessToken);
        // ɾ��ˢ������
        oauth2RefreshTokenMapper.deleteByRefreshToken(accessTokenDO.getRefreshT
oken());
        oauth2AccessTokenRedisDAO.delete(accessTokenDO.getRefreshToken());
        return accessTokenDO;
    }

    @Override
    public void removeAccessToken(Long userId, Integer userType) {
        List<OAuth2AccessTokenDO> accessTokens = oauth2AccessTokenMapper.select
ListByUserIdAndUserType(userId, userType);
        if (CollUtil.isEmpty(accessTokens)) {
            return;
        }
        accessTokens.forEach(accessToken -> {
            // ɾ����������
            oauth2AccessTokenMapper.deleteById(accessToken.getId());
            oauth2AccessTokenRedisDAO.delete(accessToken.getAccessToken());
            // ɾ��ˢ������
            oauth2RefreshTokenMapper.deleteByRefreshToken(accessToken.getRefres
hToken());
            oauth2AccessTokenRedisDAO.delete(accessToken.getRefreshToken());
        });
    }

    @Override
    public PageResult<OAuth2AccessTokenDO> getAccessTokenPage(OAuth2AccessToken
PageReqVO reqVO) {
        return oauth2AccessTokenMapper.selectPage(reqVO);
    }

    private OAuth2AccessTokenDO createOAuth2AccessToken(OAuth2RefreshTokenDO re
freshTokenDO, OAuth2ClientDO clientDO) {
        OAuth2AccessTokenDO accessTokenDO = new OAuth2AccessTokenDO().setAccess
Token(generateAccessToken())
                .setUserId(refreshTokenDO.getUserId()).setUserType(refreshToken
DO.getUserType())
                .setUserInfo(buildUserInfo(refreshTokenDO.getUserId(), refreshT
okenDO.getUserType()))
                .setClientId(clientDO.getClientId()).setScopes(refreshTokenDO.g
etScopes())
                .setRefreshToken(refreshTokenDO.getRefreshToken())
                .setExpiresTime(DateUtils.now().plusSeconds(clientDO.getAccessT
okenValiditySeconds()));
        // ���ȴ� refreshToken ��ȡ�⻧��ţ����� ThreadLocal ����Ⱦʱ���� tenantId Ϊ n
ull
        // ���ܹ����� issue��https://t.zsxq.com/JIi5G
        Long tenantId = refreshTokenDO.getTenantId();
        if (tenantId == null) {
            tenantId = TenantContextHolder.getTenantId();
        }
        accessTokenDO.setTenantId(tenantId);
        oauth2AccessTokenMapper.insert(accessTokenDO);
        // ��¼�� Redis ��
        oauth2AccessTokenRedisDAO.set(accessTokenDO);
        return accessTokenDO;
    }

    private OAuth2RefreshTokenDO createOAuth2RefreshToken(Long userId, Integer 
userType, OAuth2ClientDO clientDO, List<String> scopes) {
        OAuth2RefreshTokenDO refreshToken = new OAuth2RefreshTokenDO().setRefre
shToken(generateRefreshToken())
                .setUserId(userId).setUserType(userType)
                .setClientId(clientDO.getClientId()).setScopes(scopes)
                .setExpiresTime(DateUtils.now().plusSeconds(clientDO.getRefresh
TokenValiditySeconds()));
        oauth2RefreshTokenMapper.insert(refreshToken);
        return refreshToken;
    }

    private OAuth2AccessTokenDO convertToAccessToken(OAuth2RefreshTokenDO refre
shTokenDO) {
        OAuth2AccessTokenDO accessTokenDO = BeanUtils.toBean(refreshTokenDO, OA
uth2AccessTokenDO.class)
                .setAccessToken(refreshTokenDO.getRefreshToken());
        TenantUtils.execute(refreshTokenDO.getTenantId(),
                        () -> accessTokenDO.setUserInfo(buildUserInfo(refreshTo
kenDO.getUserId(), refreshTokenDO.getUserType())));
        return accessTokenDO;
    }

    /**
     * �����û���Ϣ������ {@link cn.zszj.framework.security.core.LoginUser} ��ȡ��
�ǳơ����ŵ���Ϣ
     *
     * @param userId �û����
     * @param userType �û�����
     * @return �û���Ϣ
     */
    private Map<String, String> buildUserInfo(Long userId, Integer userType) {
        if (userId == null || userId <= 0) {
            return Collections.emptyMap();
        }
        if (userType.equals(UserTypeEnum.ADMIN.getValue())) {
            AdminUserDO user = adminUserService.getUser(userId);
            return MapUtil.builder(LoginUser.INFO_KEY_NICKNAME, user.getNicknam
e())
                    .put(LoginUser.INFO_KEY_DEPT_ID, StrUtil.toStringOrNull(use
r.getDeptId())).build();
        } else if (userType.equals(UserTypeEnum.MEMBER.getValue())) {
            // ע�⣺Ŀǰ Member ��ʱ����ȡ�����԰���ʵ��
            return Collections.emptyMap();
        }
        throw new IllegalArgumentException("δ֪�û����ͣ�" + userType);
    }

    private static String generateAccessToken() {
        return IdUtil.fastSimpleUUID();
    }

    private static String generateRefreshToken() {
        return IdUtil.fastSimpleUUID();
    }

    @Override
    public Integer cleanRefreshToken(Integer exceedDay, Integer deleteLimit) {
        int count = 0;
        LocalDateTime expireDate = DateUtils.now().minusDays(exceedDay);
        // ѭ��ɾ����ֱ��û����������������
        for (int i = 0; i < Short.MAX_VALUE; i++) {
            int deleteCount = oauth2RefreshTokenMapper.deleteByExpiresTimeLt(ex
pireDate, deleteLimit);
            count += deleteCount;
            // �ﵽɾ��Ԥ��������˵��������
            if (deleteCount < deleteLimit) {
                break;
            }
        }
        return count;
    }

    @Override
    public Integer cleanAccessToken(Integer exceedDay, Integer deleteLimit) {
        int count = 0;
        LocalDateTime expireDate = DateUtils.now().minusDays(exceedDay);
        // ѭ��ɾ����ֱ��û����������������
        for (int i = 0; i < Short.MAX_VALUE; i++) {
            int deleteCount = oauth2AccessTokenMapper.deleteByExpiresTimeLt(exp
ireDate, deleteLimit);
            count += deleteCount;
            // �ﵽɾ��Ԥ��������˵��������
            if (deleteCount < deleteLimit) {
                break;
            }
        }
        return count;
    }
}
package cn.zszj.module.system.dal.redis.oauth2;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.zszj.framework.common.util.collection.CollectionUtils;
import cn.zszj.framework.common.util.json.JsonUtils;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static cn.zszj.module.system.dal.redis.RedisKeyConstants.OAUTH2_ACCESS_T
OKEN;

/**
 * {@link OAuth2AccessTokenDO} �� RedisDAO
 *
 * @author ���Դ��
 */
@Repository
public class OAuth2AccessTokenRedisDAO {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public OAuth2AccessTokenDO get(String accessToken) {
        String redisKey = formatKey(accessToken);
        return JsonUtils.parseObject(stringRedisTemplate.opsForValue().get(redi
sKey), OAuth2AccessTokenDO.class);
    }

    public void set(OAuth2AccessTokenDO accessTokenDO) {
        String redisKey = formatKey(accessTokenDO.getAccessToken());
        // ���������ֶΣ����⻺��
        accessTokenDO.setUpdater(null).setUpdateTime(null).setCreateTime(null).
setCreator(null).setDeleted(null);
        long time = LocalDateTimeUtil.between(LocalDateTime.now(), accessTokenD
O.getExpiresTime(), ChronoUnit.SECONDS);
        if (time > 0) {
            stringRedisTemplate.opsForValue().set(redisKey, JsonUtils.toJsonStr
ing(accessTokenDO), time, TimeUnit.SECONDS);
        }
    }

    public void delete(String accessToken) {
        String redisKey = formatKey(accessToken);
        stringRedisTemplate.delete(redisKey);
    }

    public void deleteList(Collection<String> accessTokens) {
        List<String> redisKeys = CollectionUtils.convertList(accessTokens, OAut
h2AccessTokenRedisDAO::formatKey);
        stringRedisTemplate.delete(redisKeys);
    }

    private static String formatKey(String accessToken) {
        return String.format(OAUTH2_ACCESS_TOKEN, accessToken);
    }

}
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apa
che.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>cn.zszj</groupId>
    <artifactId>zszj</artifactId>
    <version>${revision}</version>
    <packaging>pom</packaging>
    <modules>
        <module>zszj-dependencies</module>
        <module>zszj-framework</module>
        <!-- Server ����Ŀ -->
        <module>zszj-server</module>
        <!-- ���� module ��չ -->
        <module>zszj-module-system</module>
        <module>zszj-module-infra</module>
<!--        <module>zszj-module-member</module>-->
<!--        <module>zszj-module-bpm</module>-->
<!--        <module>zszj-module-report</module>-->
<!--        <module>zszj-module-mp</module>-->
<!--        <module>zszj-module-pay</module>-->
<!--        <module>zszj-module-mall</module>-->
<!--        <module>zszj-module-crm</module>-->
<!--        <module>zszj-module-erp</module>-->
<!--        <module>zszj-module-iot</module>-->
<!--        <module>zszj-module-mes</module>-->
<!--        <module>zszj-module-wms</module>-->
<!--        <module>zszj-module-hrm</module>-->
<!--        <module>zszj-module-fms</module>-->
<!--        <module>zszj-module-pms</module>-->
<!--        <module>zszj-module-im</module>-->
<!-- ��ο� https://doc.iocoder.cn/ai/build/ �ĵ������ AI ģ������������� -->
<!--        <module>zszj-module-ai</module>-->
    </modules>

    <name>${project.artifactId}</name>
    <description>����֮����Ŀ�������ּ�</description>
    <url>https://github.com/YunaiV/ruoyi-vue-pro</url>

    <properties>
        <revision>2026.08-SNAPSHOT</revision>
        <!-- Maven ��� -->
        <java.version>17</java.version>
        <maven.compiler.source>${java.version}</maven.compiler.source>
        <maven.compiler.target>${java.version}</maven.compiler.target>
        <maven-surefire-plugin.version>3.5.3</maven-surefire-plugin.version>
        <maven-compiler-plugin.version>3.14.0</maven-compiler-plugin.version>
        <flatten-maven-plugin.version>1.7.2</flatten-maven-plugin.version>
        <!-- maven-surefire-plugin ��ʱ�޷�ͨ�� bom ��������ȡ�������ϰ汾 IDEA 2024 �
���ǰ�汾�� -->
        <lombok.version>1.18.42</lombok.version>
        <spring.boot.version>3.5.15</spring.boot.version>
        <mapstruct.version>1.6.3</mapstruct.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>cn.zszj</groupId>
                <artifactId>zszj-dependencies</artifactId>
                <version>${revision}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <!-- maven-surefire-plugin ������������е�Ԫ���ԡ� -->
                <!-- ע�⣬��Ҫʹ�� 3.0.X+����ΪҪ֧�� Junit 5 �汾 -->
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <version>${maven-surefire-plugin.version}</version>
                </plugin>
                <!-- maven-compiler-plugin �������� spring-boot-configuration-p
rocessor + Lombok + MapStruct ��� -->
                <!-- https://stackoverflow.com/questions/33483697/re-run-spring
-boot-configuration-annotation-processor-to-update-generated-metada -->
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>${maven-compiler-plugin.version}</version>
                    <configuration>
                        <annotationProcessorPaths>
                            <path>
                                <groupId>org.springframework.boot</groupId>
                                <artifactId>spring-boot-configuration-processor
</artifactId>
                                <version>${spring.boot.version}</version>
                            </path>
                            <path>
                                <groupId>org.projectlombok</groupId>
                                <artifactId>lombok</artifactId>
                                <version>${lombok.version}</version>
                            </path>
                            <path>
                                <!-- ȷ�� Lombok ���ɵ� getter/setter �����ܱ� Map
Struct ��ȷʶ��
                                     ������� No property named ��xxx" exists �ı
������ -->
                                <groupId>org.projectlombok</groupId>
                                <artifactId>lombok-mapstruct-binding</artifactI
d>
                                <version>0.2.0</version>
                            </path>
                            <path>
                                <groupId>org.mapstruct</groupId>
                                <artifactId>mapstruct-processor</artifactId>
                                <version>${mapstruct.version}</version>
                            </path>
                        </annotationProcessorPaths>
                        <!-- �������д�� arg �ڣ���� Spring Boot 3.2 �� Parameter
 Name Discovery ���� -->
                        <debug>false</debug>
                        <compilerArgs>
                            <arg>-parameters</arg>
                        </compilerArgs>
                    </configuration>
                </plugin>
                <plugin>
                    <groupId>org.codehaus.mojo</groupId>
                    <artifactId>flatten-maven-plugin</artifactId>
                </plugin>
            </plugins>
        </pluginManagement>

        <plugins>
            <!-- ZS-ENG-002��ǿ�� JDK 17 / Maven 3.8+ �������ߣ�D-02 ��ȷ�� JDK 17
 ά���ߣ���
                 ������������ʱ������ȷʧ�ܣ�������������ʽ�汾 -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-enforcer-plugin</artifactId>
                <version>3.5.0</version>
                <executions>
                    <execution>
                        <id>enforce-jdk17-baseline</id>
                        <goals>
                            <goal>enforce</goal>
                        </goals>
                        <configuration>
                            <rules>
                                <requireJavaVersion>
                                    <version>[17,18)</version>
                                    <message>�������̶� JDK 17 �������ߣ�D-02����
��ʹ�� JDK 17 ������</message>
                                </requireJavaVersion>
                                <requireMavenVersion>
                                    <version>[3.8,)</version>
                                    <message>������Ҫ�� Maven 3.8 �����ϰ汾��</mes
sage>
                                </requireMavenVersion>
                            </rules>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            <!-- ͳһ revision �汾 -->
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>flatten-maven-plugin</artifactId>
                <version>${flatten-maven-plugin.version}</version>
                <configuration>
                    <flattenMode>oss</flattenMode>
                    <updatePomFile>true</updatePomFile>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>flatten</goal>
                        </goals>
                        <id>flatten</id>
                        <phase>process-resources</phase>
                    </execution>
                    <execution>
                        <goals>
                            <goal>clean</goal>
                        </goals>
                        <id>flatten.clean</id>
                        <phase>clean</phase>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>

    <!-- ʹ�� huawei / aliyun �� Maven Դ�����������ٶ� -->
    <repositories>
        <repository>
            <id>huaweicloud</id>
            <name>huawei</name>
            <url>https://mirrors.huaweicloud.com/repository/maven/</url>
        </repository>
        <repository>
            <id>aliyunmaven</id>
            <name>aliyun</name>
            <url>https://maven.aliyun.com/repository/public</url>
        </repository>
    </repositories>

</project>

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "git 
ls-tree -r --name-only 28257a25 | rg -i '("'^|/)(agents('"\\.override)?\\.md|cl
aude\\.md|gemini\\.md|copilot-instructions\\.md|\\.codex/.*|.*skill\\.md)"'$'"'
; rg -n --hidden --no-ignore 'project_doc_fallback_filenames' .omx .github 'C:/
Users/Administrator/.codex/config.toml'; git show 28257a25:services/zhongshu-co
re/zszj-module-system/src/test/java/cn/zszj/module/system/service/oauth2/OAuth2
TokenServiceImplTest.java; rg -n 'class BaseDbAndRedisUnitTest|class RedisTestC
onfiguration|@TestPropertySource|refresh-token-as-access-token-enabled' service
s/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test services/zhongshu-
core/zszj-server/src/main/resources services/zhongshu-core/zszj-module-system/s
rc/test/resources; git show 28257a25:services/zhongshu-core/zszj-module-system/
pom.xml; Get-Command java,mvn,python,node -ErrorAction SilentlyContinue | Selec
t-Object Name,Source" in E:\众墅之家AI赋能平台底座
 exited 1 in 157ms:
package cn.zszj.module.system.service.oauth2;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.common.exception.ErrorCode;
import cn.zszj.framework.common.pojo.PageResult;
import cn.zszj.framework.common.util.date.DateUtils;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.framework.test.core.ut.BaseDbAndRedisUnitTest;
import cn.zszj.module.system.controller.admin.oauth2.vo.token.OAuth2AccessToken
PageReqVO;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2ClientDO;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2RefreshTokenDO;
import cn.zszj.module.system.dal.dataobject.user.AdminUserDO;
import cn.zszj.module.system.dal.mysql.oauth2.OAuth2AccessTokenMapper;
import cn.zszj.module.system.dal.mysql.oauth2.OAuth2RefreshTokenMapper;
import cn.zszj.module.system.dal.redis.oauth2.OAuth2AccessTokenRedisDAO;
import cn.zszj.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static cn.zszj.framework.common.util.object.ObjectUtils.cloneIgnoreId;
import static cn.zszj.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.zszj.framework.test.core.util.AssertUtils.assertServiceExcepti
on;
import static cn.zszj.framework.test.core.util.RandomUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * {@link OAuth2TokenServiceImpl} 的单元测试类
 *
 * @author 芋道源码
 */
@Import({OAuth2TokenServiceImpl.class, OAuth2AccessTokenRedisDAO.class})
@TestPropertySource(properties = "zszj.security.refresh-token-as-access-token-e
nabled=false")
public class OAuth2TokenServiceImplTest extends BaseDbAndRedisUnitTest {

    @Resource
    private OAuth2TokenServiceImpl oauth2TokenService;

    @Resource
    private OAuth2AccessTokenMapper oauth2AccessTokenMapper;
    @Resource
    private OAuth2RefreshTokenMapper oauth2RefreshTokenMapper;

    @Resource
    private OAuth2AccessTokenRedisDAO oauth2AccessTokenRedisDAO;

    @MockitoBean
    private OAuth2ClientService oauth2ClientService;
    @MockitoBean
    private AdminUserService adminUserService;

    @Test
    public void testCreateAccessToken() {
        TenantContextHolder.setTenantId(0L);
        // 准备参数
        Long userId = randomLongId();
        Integer userType = UserTypeEnum.ADMIN.getValue();
        String clientId = randomString();
        List<String> scopes = Lists.newArrayList("read", "write");
        // mock 方法
        OAuth2ClientDO clientDO = randomPojo(OAuth2ClientDO.class).setClientId(
clientId)
                .setAccessTokenValiditySeconds(30).setRefreshTokenValiditySecon
ds(60);
        when(oauth2ClientService.validOAuthClientFromCache(eq(clientId))).thenR
eturn(clientDO);
        // mock 数据（用户）
        AdminUserDO user = randomPojo(AdminUserDO.class);
        when(adminUserService.getUser(userId)).thenReturn(user);

        // 调用
        OAuth2AccessTokenDO accessTokenDO = oauth2TokenService.createAccessToke
n(userId, userType, clientId, scopes);
        // 断言访问令牌
        OAuth2AccessTokenDO dbAccessTokenDO = oauth2AccessTokenMapper.selectByA
ccessToken(accessTokenDO.getAccessToken());
        // TODO @芋艿：expiresTime 被屏蔽，仅 win11 会复现，建议后续修复。
        assertPojoEquals(accessTokenDO, dbAccessTokenDO, "expiresTime", "create
Time", "updateTime", "deleted");
        assertEquals(userId, accessTokenDO.getUserId());
        assertEquals(userType, accessTokenDO.getUserType());
        assertEquals(2, accessTokenDO.getUserInfo().size());
        assertEquals(user.getNickname(), accessTokenDO.getUserInfo().get("nickn
ame"));
        assertEquals(user.getDeptId().toString(), accessTokenDO.getUserInfo().g
et("deptId"));
        assertEquals(clientId, accessTokenDO.getClientId());
        assertEquals(scopes, accessTokenDO.getScopes());
        assertFalse(DateUtils.isExpired(accessTokenDO.getExpiresTime()));
        // 断言访问令牌的缓存
        OAuth2AccessTokenDO redisAccessTokenDO = oauth2AccessTokenRedisDAO.get(
accessTokenDO.getAccessToken());
        // TODO @芋艿：expiresTime 被屏蔽，仅 win11 会复现，建议后续修复。
        assertPojoEquals(accessTokenDO, redisAccessTokenDO, "expiresTime", "cre
ateTime", "updateTime", "deleted");
        // 断言刷新令牌
        OAuth2RefreshTokenDO refreshTokenDO = oauth2RefreshTokenMapper.selectLi
st().get(0);
        assertPojoEquals(accessTokenDO, refreshTokenDO, "id", "expiresTime", "c
reateTime", "updateTime", "deleted");
        assertFalse(DateUtils.isExpired(refreshTokenDO.getExpiresTime()));
    }

    @Test
    public void testRefreshAccessToken_null() {
        // 准备参数
        String refreshToken = randomString();
        String clientId = randomString();
        // mock 方法

        // 调用，并断言
        assertServiceException(() -> oauth2TokenService.refreshAccessToken(refr
eshToken, clientId),
                new ErrorCode(400, "无效的刷新令牌"));
    }

    @Test
    public void testRefreshAccessToken_clientIdError() {
        // 准备参数
        String refreshToken = randomString();
        String clientId = randomString();
        // mock 方法
        OAuth2ClientDO clientDO = randomPojo(OAuth2ClientDO.class).setClientId(
clientId);
        when(oauth2ClientService.validOAuthClientFromCache(eq(clientId))).thenR
eturn(clientDO);
        // mock 数据（访问令牌）
        OAuth2RefreshTokenDO refreshTokenDO = randomPojo(OAuth2RefreshTokenDO.c
lass)
                .setRefreshToken(refreshToken).setClientId("error");
        oauth2RefreshTokenMapper.insert(refreshTokenDO);

        // 调用，并断言
        assertServiceException(() -> oauth2TokenService.refreshAccessToken(refr
eshToken, clientId),
                new ErrorCode(400, "刷新令牌的客户端编号不正确"));
    }

    @Test
    public void testRefreshAccessToken_expired() {
        // 准备参数
        String refreshToken = randomString();
        String clientId = randomString();
        // mock 方法
        OAuth2ClientDO clientDO = randomPojo(OAuth2ClientDO.class).setClientId(
clientId);
        when(oauth2ClientService.validOAuthClientFromCache(eq(clientId))).thenR
eturn(clientDO);
        // mock 数据（访问令牌）
        OAuth2RefreshTokenDO refreshTokenDO = randomPojo(OAuth2RefreshTokenDO.c
lass)
                .setRefreshToken(refreshToken).setClientId(clientId)
                .setExpiresTime(LocalDateTime.now().minusDays(1));
        oauth2RefreshTokenMapper.insert(refreshTokenDO);

        // 调用，并断言
        assertServiceException(() -> oauth2TokenService.refreshAccessToken(refr
eshToken, clientId),
                new ErrorCode(401, "刷新令牌已过期"));
        assertEquals(0, oauth2AccessTokenMapper.selectCount());
    }

    @Test
    public void testRefreshAccessToken_success() {
        TenantContextHolder.setTenantId(0L);
        // 准备参数
        String refreshToken = randomString();
        String clientId = randomString();
        // mock 方法
        OAuth2ClientDO clientDO = randomPojo(OAuth2ClientDO.class).setClientId(
clientId)
                .setAccessTokenValiditySeconds(30);
        when(oauth2ClientService.validOAuthClientFromCache(eq(clientId))).thenR
eturn(clientDO);
        // mock 数据（访问令牌）
        OAuth2RefreshTokenDO refreshTokenDO = randomPojo(OAuth2RefreshTokenDO.c
lass, o ->
                o.setRefreshToken(refreshToken).setClientId(clientId)
                        .setExpiresTime(LocalDateTime.now().plusDays(1))
                        .setUserType(UserTypeEnum.ADMIN.getValue())
                        .setTenantId(TenantContextHolder.getTenantId()));
        oauth2RefreshTokenMapper.insert(refreshTokenDO);
        // mock 数据（访问令牌）
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.clas
s).setRefreshToken(refreshToken)
                .setUserType(refreshTokenDO.getUserType());
        oauth2AccessTokenMapper.insert(accessTokenDO);
        oauth2AccessTokenRedisDAO.set(accessTokenDO);
        // mock 数据（用户）
        AdminUserDO user = randomPojo(AdminUserDO.class);
        when(adminUserService.getUser(refreshTokenDO.getUserId())).thenReturn(u
ser);

        // 调用
        OAuth2AccessTokenDO newAccessTokenDO = oauth2TokenService.refreshAccess
Token(refreshToken, clientId);
        // 断言，老的访问令牌被删除
        assertNull(oauth2AccessTokenMapper.selectByAccessToken(accessTokenDO.ge
tAccessToken()));
        assertNull(oauth2AccessTokenRedisDAO.get(accessTokenDO.getAccessToken()
));
        // 断言，新的访问令牌
        OAuth2AccessTokenDO dbAccessTokenDO = oauth2AccessTokenMapper.selectByA
ccessToken(newAccessTokenDO.getAccessToken());
        // TODO @芋艿：expiresTime 被屏蔽，仅 win11 会复现，建议后续修复。
        assertPojoEquals(newAccessTokenDO, dbAccessTokenDO, "expiresTime", "cre
ateTime", "updateTime", "deleted");
        assertPojoEquals(newAccessTokenDO, refreshTokenDO, "id", "expiresTime",
 "createTime", "updateTime", "deleted",
                "creator", "updater");
        assertFalse(DateUtils.isExpired(newAccessTokenDO.getExpiresTime()));
        // 断言，新的访问令牌的缓存
        OAuth2AccessTokenDO redisAccessTokenDO = oauth2AccessTokenRedisDAO.get(
newAccessTokenDO.getAccessToken());
        // TODO @芋艿：expiresTime 被屏蔽，仅 win11 会复现，建议后续修复。
        assertPojoEquals(newAccessTokenDO, redisAccessTokenDO, "expiresTime", "
createTime", "updateTime", "deleted");
    }

    @Test
    public void testGetAccessToken() {
        // mock 数据（访问令牌）
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.clas
s)
                .setExpiresTime(LocalDateTime.now().plusDays(1));
        oauth2AccessTokenMapper.insert(accessTokenDO);
        // 准备参数
        String accessToken = accessTokenDO.getAccessToken();

        // 调用
        OAuth2AccessTokenDO result = oauth2TokenService.getAccessToken(accessTo
ken);
        // 断言
        // TODO @芋艿：expiresTime 被屏蔽，仅 win11 会复现，建议后续修复。
        assertPojoEquals(accessTokenDO, result, "expiresTime", "createTime", "u
pdateTime", "deleted",
                "creator", "updater");
        // TODO @芋艿：expiresTime 被屏蔽，仅 win11 会复现，建议后续修复。
        assertPojoEquals(accessTokenDO, oauth2AccessTokenRedisDAO.get(accessTok
en), "expiresTime", "createTime", "updateTime", "deleted",
                "creator", "updater");
    }

    @Test
    public void testCheckAccessToken_null() {
        // 调研，并断言
        assertServiceException(() -> oauth2TokenService.checkAccessToken(random
String()),
                new ErrorCode(401, "访问令牌不存在"));
    }

    @Test
    public void testCheckAccessToken_expired() {
        // mock 数据（访问令牌）
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.clas
s)
                .setExpiresTime(LocalDateTime.now().minusDays(1));
        oauth2AccessTokenMapper.insert(accessTokenDO);
        // 准备参数
        String accessToken = accessTokenDO.getAccessToken();

        // 调研，并断言
        assertServiceException(() -> oauth2TokenService.checkAccessToken(access
Token),
                new ErrorCode(401, "访问令牌已过期"));
    }

    @Test
    public void testGetAccessToken_refreshTokenAsAccess_legacyZeroUserId() {
        // ZS-LOGIN-001：gate=false 下无论 userId 取值（含 0L），刷新令牌一律不得被转换为访问令牌（在门控处即短路
返回 null，不进入 convertToAccessToken）
        // mock 数据（访问令牌）
        OAuth2RefreshTokenDO refreshTokenDO = randomPojo(OAuth2RefreshTokenDO.c
lass)
                .setUserId(0L)
                .setExpiresTime(LocalDateTime.now().plusDays(1));
        oauth2RefreshTokenMapper.insert(refreshTokenDO);
        // 准备参数
        String accessToken = refreshTokenDO.getRefreshToken();

        // 调研，并断言
        OAuth2AccessTokenDO result = oauth2TokenService.getAccessToken(accessTo
ken);
        // 断言
        // ZS-LOGIN-001：默认 gate 关闭，刷新令牌不再被静默转换为访问令牌
        assertNull(result, "刷新令牌不得被当作访问令牌静默转换");
    }

    @Test
    public void testCheckAccessToken_success() {
        // mock 数据（访问令牌）
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.clas
s)
                .setExpiresTime(LocalDateTime.now().plusDays(1));
        oauth2AccessTokenMapper.insert(accessTokenDO);
        // 准备参数
        String accessToken = accessTokenDO.getAccessToken();

        // 调研，并断言
        OAuth2AccessTokenDO result = oauth2TokenService.getAccessToken(accessTo
ken);
        // 断言
        // TODO @芋艿：expiresTime 被屏蔽，仅 win11 会复现，建议后续修复。
        assertPojoEquals(accessTokenDO, result, "expiresTime", "createTime", "u
pdateTime", "deleted",
                "creator", "updater");
    }

    @Test
    public void testRemoveAccessToken_null() {
        // 调用，并断言
        assertNull(oauth2TokenService.removeAccessToken(randomString()));
    }

    @Test
    public void testRemoveAccessToken_success() {
        // mock 数据（访问令牌）
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.clas
s)
                .setExpiresTime(LocalDateTime.now().plusDays(1));
        oauth2AccessTokenMapper.insert(accessTokenDO);
        // mock 数据（刷新令牌）
        OAuth2RefreshTokenDO refreshTokenDO = randomPojo(OAuth2RefreshTokenDO.c
lass)
                .setRefreshToken(accessTokenDO.getRefreshToken());
        oauth2RefreshTokenMapper.insert(refreshTokenDO);
        // 调用
        OAuth2AccessTokenDO result = oauth2TokenService.removeAccessToken(acces
sTokenDO.getAccessToken());
        // TODO @芋艿：expiresTime 被屏蔽，仅 win11 会复现，建议后续修复。
        assertPojoEquals(accessTokenDO, result, "expiresTime", "createTime", "u
pdateTime", "deleted",
                "creator", "updater");
        // 断言数据
        assertNull(oauth2AccessTokenMapper.selectByAccessToken(accessTokenDO.ge
tAccessToken()));
        assertNull(oauth2RefreshTokenMapper.selectByRefreshToken(accessTokenDO.
getRefreshToken()));
        assertNull(oauth2AccessTokenRedisDAO.get(accessTokenDO.getAccessToken()
));
    }


    @Test
    public void testGetAccessTokenPage() {
        // mock 数据
        OAuth2AccessTokenDO dbAccessToken = randomPojo(OAuth2AccessTokenDO.clas
s, o -> { // 等会查询到
            o.setUserId(10L);
            o.setUserType(1);
            o.setClientId("test_client");
            o.setExpiresTime(LocalDateTime.now().plusDays(1));
        });
        oauth2AccessTokenMapper.insert(dbAccessToken);
        // 测试 userId 不匹配
        oauth2AccessTokenMapper.insert(cloneIgnoreId(dbAccessToken, o -> o.setU
serId(20L)));
        // 测试 userType 不匹配
        oauth2AccessTokenMapper.insert(cloneIgnoreId(dbAccessToken, o -> o.setU
serType(2)));
        // 测试 userType 不匹配
        oauth2AccessTokenMapper.insert(cloneIgnoreId(dbAccessToken, o -> o.setC
lientId("it_client")));
        // 测试 expireTime 不匹配
        oauth2AccessTokenMapper.insert(cloneIgnoreId(dbAccessToken, o -> o.setE
xpiresTime(LocalDateTimeUtil.now())));
        // 准备参数
        OAuth2AccessTokenPageReqVO reqVO = new OAuth2AccessTokenPageReqVO();
        reqVO.setUserId(10L);
        reqVO.setUserType(1);
        reqVO.setClientId("test");

        // 调用
        PageResult<OAuth2AccessTokenDO> pageResult = oauth2TokenService.getAcce
ssTokenPage(reqVO);
        // 断言
        assertEquals(1, pageResult.getTotal());
        assertEquals(1, pageResult.getList().size());
        // TODO @芋艿：expiresTime 被屏蔽，仅 win11 会复现，建议后续修复。
        assertPojoEquals(dbAccessToken, pageResult.getList().get(0), "expiresTi
me");
    }

    /**
     * ZS-LOGIN-001：令牌用途分离 —— 默认禁止刷新令牌静默转访问令牌，getAccessToken 返回 null
     */
    @Test
    public void testGetAccessToken_refreshTokenAsAccess_shouldReturnNullByDefau
lt() {
        // 构造一个有效的刷新令牌（未来过期）
        OAuth2RefreshTokenDO refreshTokenDO = randomPojo(OAuth2RefreshTokenDO.c
lass)
                .setUserId(randomLongId())
                .setUserType(UserTypeEnum.ADMIN.getValue())
                .setClientId(randomString())
                .setExpiresTime(LocalDateTime.now().plusDays(1));
        oauth2RefreshTokenMapper.insert(refreshTokenDO);
        // 把刷新令牌字符串当作访问令牌传入
        String accessToken = refreshTokenDO.getRefreshToken();
        // 保证 Redis 中没有该 token
        assertNull(oauth2AccessTokenRedisDAO.get(accessToken));

        // 调用，期望返回 null（gate 默认 false，不得静默回退转换）
        OAuth2AccessTokenDO result = oauth2TokenService.getAccessToken(accessTo
ken);
        assertNull(result, "刷新令牌不得被当作访问令牌静默转换");
    }

    @Test
    public void testCheckAccessToken_refreshTokenAsAccess_shouldThrowUnauthoriz
ed() {
        // 构造一个有效的刷新令牌（未来过期）
        OAuth2RefreshTokenDO refreshTokenDO = randomPojo(OAuth2RefreshTokenDO.c
lass)
                .setUserId(randomLongId())
                .setUserType(UserTypeEnum.ADMIN.getValue())
                .setClientId(randomString())
                .setExpiresTime(LocalDateTime.now().plusDays(1));
        oauth2RefreshTokenMapper.insert(refreshTokenDO);
        String accessToken = refreshTokenDO.getRefreshToken();

        // checkAccessToken -> getAccessToken -> null -> 抛 UNAUTHORIZED
        assertServiceException(() -> oauth2TokenService.checkAccessToken(access
Token),
                new ErrorCode(401, "访问令牌不存在"));
    }

}
services/zhongshu-core/zszj-server/src/main/resources\application.yaml:302:    
refresh-token-as-access-token-enabled: true
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test\src\main\ja
va\cn\zszj\framework\test\core\ut\BaseDbAndRedisUnitTest.java:30:public class B
aseDbAndRedisUnitTest {
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test\src\main\ja
va\cn\zszj\framework\test\config\RedisTestConfiguration.java:20:public class Re
disTestConfiguration {
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apa
che.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>cn.zszj</groupId>
        <artifactId>zszj</artifactId>
        <version>${revision}</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>
    <artifactId>zszj-module-system</artifactId>
    <packaging>jar</packaging>

    <name>${project.artifactId}</name>
    <description>
        system 模块下，我们放通用业务，支撑上层的核心业务。
        例如说：用户、部门、权限、数据字典等等
    </description>

    <dependencies>
        <dependency>
            <groupId>cn.zszj</groupId>
            <artifactId>zszj-module-infra</artifactId>
            <version>${revision}</version>
        </dependency>

        <!-- 业务组件 -->
        <dependency>
            <groupId>cn.zszj</groupId>
            <artifactId>zszj-spring-boot-starter-biz-data-permission</artifactI
d>
        </dependency>
        <dependency>
            <groupId>cn.zszj</groupId>
            <artifactId>zszj-spring-boot-starter-biz-tenant</artifactId>
        </dependency>
        <dependency>
            <groupId>cn.zszj</groupId>
            <artifactId>zszj-spring-boot-starter-biz-ip</artifactId>
        </dependency>

        <!-- Web 相关 -->
        <dependency>
            <groupId>cn.zszj</groupId>
            <artifactId>zszj-spring-boot-starter-security</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- DB 相关 -->
        <dependency>
            <groupId>cn.zszj</groupId>
            <artifactId>zszj-spring-boot-starter-mybatis</artifactId>
        </dependency>

        <dependency>
            <groupId>cn.zszj</groupId>
            <artifactId>zszj-spring-boot-starter-redis</artifactId>
        </dependency>

        <!-- Job 定时任务相关 -->
        <dependency>
            <groupId>cn.zszj</groupId>
            <artifactId>zszj-spring-boot-starter-job</artifactId>
        </dependency>

        <!-- 消息队列相关 -->
        <dependency>
            <groupId>cn.zszj</groupId>
            <artifactId>zszj-spring-boot-starter-mq</artifactId>
        </dependency>

        <!-- Test 测试相关 -->
        <dependency>
            <groupId>cn.zszj</groupId>
            <artifactId>zszj-spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- 工具类相关 -->
        <dependency>
            <groupId>cn.zszj</groupId>
            <artifactId>zszj-spring-boot-starter-excel</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-mail</artifactId>
        </dependency>

        <!-- 三方云服务相关 -->
        <dependency>
            <groupId>me.zhyd.oauth</groupId>
            <artifactId>JustAuth</artifactId> <!-- 社交登陆（例如说，个人微信、企业微信等等） -->
        </dependency>
        <dependency>
            <groupId>com.xkcoding.justauth</groupId>
            <artifactId>justauth-spring-boot-starter</artifactId>
        </dependency>

        <dependency>
            <groupId>com.github.binarywang</groupId>
            <artifactId>wx-java-mp-spring-boot-starter</artifactId> <!-- 微信登录（公
众号） -->
        </dependency>
        <dependency>
            <groupId>com.github.binarywang</groupId>
            <artifactId>wx-java-miniapp-spring-boot-starter</artifactId>  <!-- 
微信登录（小程序） -->
        </dependency>

        <dependency>
            <groupId>com.anji-plus</groupId>
            <artifactId>captcha-spring-boot-starter</artifactId> <!-- 验证码，一般用于登
录使用 -->
        </dependency>

        <dependency>
            <groupId>org.dromara.hutool</groupId>
            <artifactId>hutool-extra</artifactId> <!-- 邮件 -->
        </dependency>

    </dependencies>

</project>

Name       Source                               
----       ------                               
python.exe C:\Program Files\Python311\python.exe
node.exe   C:\Program Files\nodejs\node.exe     



exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "git 
show 28257a25:services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-te
st/src/main/java/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java; gi
t show 28257a25:services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-
test/src/main/java/cn/zszj/framework/test/config/RedisTestConfiguration.java; g
it show 28257a25:services/zhongshu-core/zszj-module-system/src/test/resources/a
pplication-unit-test.yaml; git show 28257a25:services/zhongshu-core/zszj-framew
ork/zszj-spring-boot-starter-test/src/main/java/cn/zszj/framework/test/config/R
edisTestExecutionListener.java; git show 28257a25:services/zhongshu-core/zszj-m
odule-system/src/main/java/cn/zszj/module/system/dal/dataobject/oauth2/OAuth2Ac
cessTokenDO.java; git show 28257a25:services/zhongshu-core/zszj-module-system/s
rc/main/java/cn/zszj/module/system/dal/dataobject/oauth2/OAuth2RefreshTokenDO.j
ava; rg -n 'getRefreshToken\\(|refreshToken|refresh_token' apps/admin-web/src/v
iews/im apps/miniapp -g '*socket*' -g '*Socket*'; python -c \"import os,json; p
rint(json.dumps({'root_docs': [p for p in ['E:/AGENTS.override.md','E:/AGENTS.m
d','E:/众墅之家AI赋能平台底座/AGENTS.override.md','E:/众墅之家AI赋能平台底座/AGENTS.md'] if os.path
.exists(p)]}, ensure_ascii=True))\"" in E:\众墅之家AI赋能平台底座
 succeeded in 39ms:
rg: apps/admin-web/src/views/im: 系统找不到指定的路径。 (os error 3)
rg: apps/miniapp: 系统找不到指定的文件。 (os error 2)
{"root_docs": []}

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "pyth
on -c \"import os,json,tomllib; p='C:/Users/Administrator/.codex/config.toml'; 
d=tomllib.load(open(p,'rb')) if os.path.isfile(p) else {}; print(json.dumps({'p
roject_doc_fallback_filenames':d.get('project_doc_fallback_filenames',[]),'proj
ect_doc_max_bytes':d.get('project_doc_max_bytes')},ensure_ascii=True))\"; git s
how 28257a25:services/zhongshu-core/zszj-server/src/main/resources/application.
yaml; git show 28257a25:services/zhongshu-core/zszj-framework/zszj-spring-boot-
starter-security/src/main/java/cn/zszj/framework/security/core/filter/TokenAuth
enticationFilter.java; git ls-tree -r --name-only 28257a25 apps | rg '(websocke
t|WebSocket|socket|Socket|auth\\.(ts|js)"'$)'"'; git ls-tree -r --name-only 282
57a25 services/zhongshu-core/zszj-server/src/main/resources | rg 'application|b
ootstrap'; rg -n 'extends BaseDbAndRedisUnitTest|new OAuth2TokenServiceImpl|@Im
port.*OAuth2TokenServiceImpl' services/zhongshu-core -g '*.java' -g '"'!**/targ
et/**'"'" in E:\众墅之家AI赋能平台底座
 succeeded in 224ms:
apps/zhongshu-admin-web/src/views/im/home/store/websocketStore.ts
apps/zhongshu-admin-web/src/views/infra/webSocket/index.vue
apps/zhongshu-admin-web/src/views/iot/rule/data/sink/config/WebSocketConfigForm
.vue
apps/zhongshu-miniapp/src/pages-im/home/store/websocketStore.ts
apps/zhongshu-miniapp/src/pages-infra/web-socket/index.vue
apps/zhongshu-miniapp/src/pages-mall/kefu/composables/useKefuWebSocket.ts
services/zhongshu-core/zszj-server/src/main/resources/application-dev.yaml
services/zhongshu-core/zszj-server/src/main/resources/application-local.yaml
services/zhongshu-core/zszj-server/src/main/resources/application.yaml
services/zhongshu-core\zszj-module-member\src\test\java\cn\zszj\module\member\s
ervice\user\MemberUserServiceImplTest.java:36:public class MemberUserServiceImp
lTest extends BaseDbAndRedisUnitTest {
services/zhongshu-core\zszj-module-member\src\test\java\cn\zszj\module\member\s
ervice\auth\MemberAuthServiceTest.java:32:public class MemberAuthServiceTest ex
tends BaseDbAndRedisUnitTest {
services/zhongshu-core\zszj-module-system\src\test\java\cn\zszj\module\system\s
ervice\oauth2\OAuth2TokenServiceImplTest.java:42:@Import({OAuth2TokenServiceImp
l.class, OAuth2AccessTokenRedisDAO.class})
services/zhongshu-core\zszj-module-system\src\test\java\cn\zszj\module\system\s
ervice\oauth2\OAuth2TokenServiceImplTest.java:44:public class OAuth2TokenServic
eImplTest extends BaseDbAndRedisUnitTest {
services/zhongshu-core\zszj-module-system\src\test\java\cn\zszj\module\system\s
ervice\oauth2\OAuth2TokenServiceImplCompatTest.java:33:@Import({OAuth2TokenServ
iceImpl.class, OAuth2AccessTokenRedisDAO.class})
services/zhongshu-core\zszj-module-system\src\test\java\cn\zszj\module\system\s
ervice\oauth2\OAuth2TokenServiceImplCompatTest.java:35:public class OAuth2Token
ServiceImplCompatTest extends BaseDbAndRedisUnitTest {
services/zhongshu-core\zszj-module-pay\src\test\java\cn\zszj\module\pay\service
\transfer\PayTransferServiceTest.java:28:public class PayTransferServiceTest ex
tends BaseDbAndRedisUnitTest {
services/zhongshu-core\zszj-module-pay\src\test\java\cn\zszj\module\pay\service
\refund\PayRefundServiceTest.java:57:public class PayRefundServiceTest extends 
BaseDbAndRedisUnitTest {
services/zhongshu-core\zszj-module-pay\src\test\java\cn\zszj\module\pay\service
\order\PayOrderServiceTest.java:58:public class PayOrderServiceTest extends Bas
eDbAndRedisUnitTest {

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "pyth
on -c \"import subprocess,pathlib,json; files=subprocess.check_output(['git','d
iff-tree','--no-commit-id','--name-only','-r','28257a25'],encoding='utf-8').spl
itlines(); dirs={pathlib.Path.cwd(),pathlib.Path.cwd().parent}; [dirs.update((p
athlib.Path.cwd()/f).parents) for f in files]; names=('AGENTS.override.md','AGE
NTS.md'); print(json.dumps({'applicable_instruction_files':[str(d/n) for d in s
orted(dirs) for n in names if (d/n).is_file()]},ensure_ascii=True)); p='service
s/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/service/
oauth2/OAuth2TokenServiceImpl.java'; s=subprocess.check_output(['git','show','2
8257a25:'+p],encoding='utf-8'); print('\\n'.join(f'{i}: {line}' for i,line in e
numerate(s.splitlines(),1) if 128<=i<=177 or 266<=i<=277));\"; git show 28257a2
5:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system
/api/oauth2/OAuth2TokenApiImpl.java; git show 28257a25:apps/zhongshu-admin-web/
src/views/im/home/store/websocketStore.ts | rg -n -C 4 'getRefreshToken|useWebS
ocket'; git grep -n '2592000' 28257a25 -- services/zhongshu-core/zszj-server/sr
c/main/resources/db/migration services/zhongshu-core/sql | Select-Object -First
 8; git diff --check '28257a25"'^'"' 28257a25" in E:\众墅之家AI赋能平台底座
 succeeded in 115ms:
1-import { defineStore, acceptHMRUpdate } from 'pinia'
2:import { getCurrentUserId, getRefreshToken } from '@/utils/auth'
3-
4-import {
5-  ImWebSocketMessageType,
6-  ImMessageStatus,
--
208-     * ??? zszj ??? /infra/ws ??????????? sendObject(type, content) ???
209-     *
210-     * socket ????????owner????????????????????     */
211-    connect() {
212:      // ?????refreshToken????????????access token ?????????????? frame ???
?????      const refreshToken = getRefreshToken()
213-      const currentUserId = getCurrentUserId()
214-      if (!refreshToken || !currentUserId) {
215-        console.warn('[IM WS] ??????????????????')
216-        return
28257a25:services/zhongshu-core/sql/dm/ruoyi-vue-pro-dm8.sql:3632:INSERT INTO s
ystem_oauth2_client (id, client_id, secret, name, logo, description, status, ac
cess_token_validity_seconds, refresh_token_validity_seconds, redirect_uris, aut
horized_grant_types, scopes, auto_approve_scopes, authorities, resource_ids, ad
ditional_information, creator, create_time, updater, update_time, deleted) VALU
ES (1, 'default', 'admin123', '众墅之家', 'http://static.zszj.example.com/20250502/
sort2_1746189740718.png', '我是描述', 0, 1800, 2592000, '["https://www.zszj.example
.com","https://doc.zszj.example.com"]', '["password","authorization_code","impl
icit","refresh_token","client_credentials"]', '["user.read","user.write"]', '[]
', '["user.read","user.write"]', '[]', '{}', '1', '2022-05-11 21:47:12', '1', '
2025-12-07 20:07:09', '0');
28257a25:services/zhongshu-core/sql/highgo/ruoyi-vue-pro.sql:4052:INSERT INTO s
ystem_oauth2_client (id, client_id, secret, name, logo, description, status, ac
cess_token_validity_seconds, refresh_token_validity_seconds, redirect_uris, aut
horized_grant_types, scopes, auto_approve_scopes, authorities, resource_ids, ad
ditional_information, creator, create_time, updater, update_time, deleted) VALU
ES (1, 'default', 'admin123', '众墅之家', 'http://static.zszj.example.com/20250502/
sort2_1746189740718.png', '我是描述', 0, 1800, 2592000, '["https://www.zszj.example
.com","https://doc.zszj.example.com"]', '["password","authorization_code","impl
icit","refresh_token","client_credentials"]', '["user.read","user.write"]', '[]
', '["user.read","user.write"]', '[]', '{}', '1', '2022-05-11 21:47:12', '1', '
2025-12-07 20:07:09', '0');
28257a25:services/zhongshu-core/sql/kingbase/ruoyi-vue-pro.sql:3810:INSERT INTO
 system_oauth2_client (id, client_id, secret, name, logo, description, status, 
access_token_validity_seconds, refresh_token_validity_seconds, redirect_uris, a
uthorized_grant_types, scopes, auto_approve_scopes, authorities, resource_ids, 
additional_information, creator, create_time, updater, update_time, deleted) VA
LUES (1, 'default', 'admin123', '众墅之家', 'http://static.zszj.example.com/2025050
2/sort2_1746189740718.png', '我是描述', 0, 1800, 2592000, '["https://www.zszj.examp
le.com","https://doc.zszj.example.com"]', '["password","authorization_code","im
plicit","refresh_token","client_credentials"]', '["user.read","user.write"]', '
[]', '["user.read","user.write"]', '[]', '{}', '1', '2022-05-11 21:47:12', '1',
 '2025-12-07 20:07:09', '0');
28257a25:services/zhongshu-core/sql/mysql/ruoyi-vue-pro.sql:4653:INSERT INTO `s
ystem_oauth2_client` (`id`, `client_id`, `secret`, `name`, `logo`, `description
`, `status`, `access_token_validity_seconds`, `refresh_token_validity_seconds`,
 `redirect_uris`, `authorized_grant_types`, `scopes`, `auto_approve_scopes`, `a
uthorities`, `resource_ids`, `additional_information`, `creator`, `create_time`
, `updater`, `update_time`, `deleted`) VALUES (1, 'default', 'admin123', '众墅之家'
, 'http://static.zszj.example.com/20250502/sort2_1746189740718.png', '我是描述', 0,
 1800, 2592000, '[\"https://www.zszj.example.com\",\"https://doc.zszj.example.c
om\"]', '[\"password\",\"authorization_code\",\"implicit\",\"refresh_token\",\"
client_credentials\"]', '[\"user.read\",\"user.write\"]', '[]', '[\"user.read\"
,\"user.write\"]', '[]', '{}', '1', '2022-05-11 21:47:12', '1', '2025-12-07 20:
07:09', b'0');
28257a25:services/zhongshu-core/sql/opengauss/ruoyi-vue-pro.sql:3810:INSERT INT
O system_oauth2_client (id, client_id, secret, name, logo, description, status,
 access_token_validity_seconds, refresh_token_validity_seconds, redirect_uris, 
authorized_grant_types, scopes, auto_approve_scopes, authorities, resource_ids,
 additional_information, creator, create_time, updater, update_time, deleted) V
ALUES (1, 'default', 'admin123', '众墅之家', 'http://static.zszj.example.com/202505
02/sort2_1746189740718.png', '我是描述', 0, 1800, 2592000, '["https://www.zszj.exam
ple.com","https://doc.zszj.example.com"]', '["password","authorization_code","i
mplicit","refresh_token","client_credentials"]', '["user.read","user.write"]', 
'[]', '["user.read","user.write"]', '[]', '{}', '1', '2022-05-11 21:47:12', '1'
, '2025-12-07 20:07:09', '0');
28257a25:services/zhongshu-core/sql/oracle/ruoyi-vue-pro.sql:3731:INSERT INTO s
ystem_oauth2_client (id, client_id, secret, name, logo, description, status, ac
cess_token_validity_seconds, refresh_token_validity_seconds, redirect_uris, aut
horized_grant_types, scopes, auto_approve_scopes, authorities, resource_ids, ad
ditional_information, creator, create_time, updater, update_time, deleted) VALU
ES (1, 'default', 'admin123', '众墅之家', 'http://static.zszj.example.com/20250502/
sort2_1746189740718.png', '我是描述', 0, 1800, 2592000, '["https://www.zszj.example
.com","https://doc.zszj.example.com"]', '["password","authorization_code","impl
icit","refresh_token","client_credentials"]', '["user.read","user.write"]', '[]
', '["user.read","user.write"]', '[]', '{}', '1', to_date('2022-05-11 21:47:12'
, 'SYYYY-MM-DD HH24:MI:SS'), '1', to_date('2025-12-07 20:07:09', 'SYYYY-MM-DD H
H24:MI:SS'), '0');
28257a25:services/zhongshu-core/sql/postgresql/ruoyi-vue-pro.sql:3810:INSERT IN
TO system_oauth2_client (id, client_id, secret, name, logo, description, status
, access_token_validity_seconds, refresh_token_validity_seconds, redirect_uris,
 authorized_grant_types, scopes, auto_approve_scopes, authorities, resource_ids
, additional_information, creator, create_time, updater, update_time, deleted) 
VALUES (1, 'default', 'admin123', '众墅之家', 'http://static.zszj.example.com/20250
502/sort2_1746189740718.png', '我是描述', 0, 1800, 2592000, '["https://www.zszj.exa
mple.com","https://doc.zszj.example.com"]', '["password","authorization_code","
implicit","refresh_token","client_credentials"]', '["user.read","user.write"]',
 '[]', '["user.read","user.write"]', '[]', '{}', '1', '2022-05-11 21:47:12', '1
', '2025-12-07 20:07:09', '0');
28257a25:services/zhongshu-core/sql/sqlserver/ruoyi-vue-pro.sql:8790:INSERT INT
O system_oauth2_client (id, client_id, secret, name, logo, description, status,
 access_token_validity_seconds, refresh_token_validity_seconds, redirect_uris, 
authorized_grant_types, scopes, auto_approve_scopes, authorities, resource_ids,
 additional_information, creator, create_time, updater, update_time, deleted) V
ALUES (1, N'default', N'admin123', N'众墅之家', N'http://static.zszj.example.com/20
250502/sort2_1746189740718.png', N'我是描述', 0, 1800, 2592000, N'["https://www.zsz
j.example.com","https://doc.zszj.example.com"]', N'["password","authorization_c
ode","implicit","refresh_token","client_credentials"]', N'["user.read","user.wr
ite"]', N'[]', N'["user.read","user.write"]', N'[]', N'{}', N'1', N'2022-05-11 
21:47:12', N'1', N'2025-12-07 20:07:09', N'0')

codex
The new security gate does not enforce token-purpose separation for previously 
cached refresh tokens. Tests were not run in the read-only environment; Java an
d Maven were unavailable on PATH.

Review comment:

- [P1] Enforce the compatibility gate on cached tokens — E:/众墅之家AI赋能平台底座/servic
es/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/service
/oauth2/OAuth2TokenServiceImpl.java:140-141
  If a refresh token was accepted while compatibility was enabled—or before thi
s upgrade—disabling the flag and restarting still leaves it usable as an access
 token. `convertToAccessToken` caches the synthetic token in Redis, and the ear
ly return in `getAccessToken` bypasses this new gate. Consequently, `checkAcces
sToken` continues authenticating it for its remaining refresh-token lifetime, p
otentially 30 days. Reject cached synthetic tokens when compatibility is disabl
ed, and add a regression test that populates the cache with compatibility enabl
ed before checking the same token with it disabled.
The new security gate does not enforce token-purpose separation for previously cached refresh tokens. Tests were not run in the read-only environment; Java and Maven were unavailable on PATH.

Review comment:

- [P1] Enforce the compatibility gate on cached tokens — E:/众墅之家AI赋能平台底座/services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/service/oauth2/OAuth2TokenServiceImpl.java:140-141
  If a refresh token was accepted while compatibility was enabled—or before this upgrade—disabling the flag and restarting still leaves it usable as an access token. `convertToAccessToken` caches the synthetic token in Redis, and the early return in `getAccessToken` bypasses this new gate. Consequently, `checkAccessToken` continues authenticating it for its remaining refresh-token lifetime, potentially 30 days. Reject cached synthetic tokens when compatibility is disabled, and add a regression test that populates the cache with compatibility enabled before checking the same token with it disabled.
