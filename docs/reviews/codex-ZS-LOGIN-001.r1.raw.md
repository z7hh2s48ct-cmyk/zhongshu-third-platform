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
reasoning effort: ultra
reasoning summaries: none
session id: 01a09496-9289-7721-acb7-a06a9d1d6adc
--------
user
commit 678bd22
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'Get-
Location; git status --short; git rev-parse --show-toplevel; git show --stat --
oneline --decorate --no-renames 678bd221; Get-ChildItem -Force -Name' in E:\众墅之
家AI赋能平台底座
 succeeded in 63ms:
?? ".omx/plans/2026-09-11-B03-Wave1-\345\205\255\351\241\271\345\271\266\350\24
1\214\345\274\200\345\217\221\350\256\241\345\210\222.md"
?? docs/reviews/codex-ZS-LOGIN-001.raw.md
?? outputs/
E:/众墅之家AI赋能平台底座
678bd221 (HEAD -> main) fix(login-001): 门控关闭时拒绝并自愈清除缓存的合成访问令牌，堵 Redis 命中绕过 gate
 [ZS-LOGIN-001]
 .../service/oauth2/OAuth2TokenServiceImpl.java     | 17 ++++++++
 .../service/oauth2/OAuth2TokenServiceImplTest.java | 45 ++++++++++++++++++++++
 2 files changed, 62 insertions(+)
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
-c core.quotepath=false show --format=fuller --no-ext-diff --no-renames 678bd22
1; rg --files --hidden -g AGENTS.md -g AGENTS.override.md -g '*codex*' -g '"'!*
*/.git/**'"' -g '"'!**/node_modules/**'"'; Get-ChildItem -Force E:\\AGENTS* -Er
rorAction SilentlyContinue" in E:\众墅之家AI赋能平台底座
 succeeded in 16ms:
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command '$p =
 '"'services/zhongshu-core/zszj-module-system'; "'$paths = @('"'.', 'services',
 'services/zhongshu-core', "'$p, "$p/src", "$p/src/main", "$p/src/main/java", "
$p/src/main/java/cn", "$p/src/main/java/cn/zszj", "$p/src/main/java/cn/zszj/mod
ule", "$p/src/main/java/cn/zszj/module/system", "$p/src/main/java/cn/zszj/modul
e/system/service", "$p/src/main/java/cn/zszj/module/system/service/oauth2", "$p
/src/test", "$p/src/test/java", "$p/src/test/java/cn", "$p/src/test/java/cn/zsz
j", "$p/src/test/java/cn/zszj/module", "$p/src/test/java/cn/zszj/module/system"
, "$p/src/test/java/cn/zszj/module/system/service", "$p/src/test/java/cn/zszj/m
odule/system/service/oauth2"); foreach ($d in $paths) { Get-ChildItem -LiteralP
ath $d -Force -File | Where-Object { $_.Name -in @('"'AGENTS.md','AGENTS.overri
de.md','CLAUDE.md') } | ForEach-Object { Write-Output \"--- "'$($_.FullName)"; 
Get-Content -LiteralPath $_.FullName -Encoding UTF8 } }; Get-Content -LiteralPa
th "$p/src/main/java/cn/zszj/module/system/service/oauth2/OAuth2TokenServiceImp
l.java" -Encoding UTF8' in E:\众墅之家AI赋能平台底座
 succeeded in 0ms:
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
            // ZS-LOGIN-001��codex P1 �޸������ſعر�ʱ���ܾ���ǰ�����ڣ�gate=true����
 convertToAccessToken
            // ����� Redis �ġ��ϳɷ������ơ���accessToken==refreshToken�����������
������Ⱦ��Ŀ�������������ڴ�
            // Redis ���д���ǰ return���ƹ��·��ſأ��� gate �رպ��Ա� checkAccessToke
n ������ˢ������ TTL��default client �� 30 �죩��
            if (!refreshTokenAsAccessTokenEnabled && isSyntheticAccessToken(acc
essTokenDO)) {
                oauth2AccessTokenRedisDAO.delete(accessToken);
                return null;
            }
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
     * ZS-LOGIN-001���ж��Ƿ�Ϊ����ˢ������ת�������ĺϳɷ������ơ���
     * <p>{@link #convertToAccessToken} �� refreshToken ��ͬʱ�䵱 accessToken �� r
efreshToken���ʶ�����ȼ��ϳ����ƣ�
     * �����������Ƶ� accessToken �� refreshToken �������������ɵ� UUID��������ȡ�
�����ſعر�ʱʶ�𲢾ܾ�������Ⱦ��Ŀ��
     */
    private static boolean isSyntheticAccessToken(OAuth2AccessTokenDO accessTok
enDO) {
        return StrUtil.isNotEmpty(accessTokenDO.getAccessToken())
                && StrUtil.equals(accessTokenDO.getAccessToken(), accessTokenDO
.getRefreshToken());
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

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command '[Con
sole]::OutputEncoding = [System.Text.Encoding]::UTF8; $base='"'services/zhongsh
u-core/zszj-module-system/src'; "'$files=@("$base/main/java/cn/zszj/module/syst
em/dal/redis/oauth2/OAuth2AccessTokenRedisDAO.java", "$base/main/java/cn/zszj/m
odule/system/dal/dataobject/oauth2/OAuth2AccessTokenDO.java", "$base/main/java/
cn/zszj/module/system/dal/dataobject/oauth2/OAuth2RefreshTokenDO.java", "$base/
test/java/cn/zszj/module/system/service/oauth2/OAuth2TokenServiceImplTest.java"
, "$base/test/java/cn/zszj/module/system/service/oauth2/OAuth2TokenServiceImplC
ompatTest.java"); foreach ($f in $files) { Write-Output "--- $f"; Get-Content -
LiteralPath $f -Encoding UTF8 }; rg -n '"'refresh-token-as-access-token|OAuth2A
ccessTokenRedisDAO|convertToAccessToken' services/zhongshu-core -g '"'!**/targe
t/**'"'" in E:\众墅之家AI赋能平台底座
 succeeded in 252ms:
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
--- services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/syst
em/dal/dataobject/oauth2/OAuth2AccessTokenDO.java
package cn.zszj.module.system.dal.dataobject.oauth2;

import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * OAuth2 �������� DO
 *
 * �����ֶΣ���ʱδʹ�ã���ʱ��֧�֣�
 * user_name��authentication���û���Ϣ��
 *
 * @author ���Դ��
 */
@TableName(value = "system_oauth2_access_token", autoResultMap = true)
@KeySequence("system_oauth2_access_token_seq") // ���� Oracle��PostgreSQL��King
base��DB2��H2 ���ݿ����������������� MySQL �����ݿ⣬�ɲ�д��
@Data
@EqualsAndHashCode(callSuper = true)
public class OAuth2AccessTokenDO extends TenantBaseDO {

    /**
     * ��ţ����ݿ����
     */
    @TableId
    private Long id;
    /**
     * ��������
     */
    private String accessToken;
    /**
     * ˢ������
     */
    private String refreshToken;
    /**
     * �û����
     */
    private Long userId;
    /**
     * �û�����
     *
     * ö�� {@link UserTypeEnum}
     */
    private Integer userType;
    /**
     * �û���Ϣ
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, String> userInfo;
    /**
     * �ͻ��˱��
     *
     * ���� {@link OAuth2ClientDO#getId()}
     */
    private String clientId;
    /**
     * ��Ȩ��Χ
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> scopes;
    /**
     * ����ʱ��
     */
    private LocalDateTime expiresTime;

}
--- services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/syst
em/dal/dataobject/oauth2/OAuth2RefreshTokenDO.java
package cn.zszj.module.system.dal.dataobject.oauth2;

import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * OAuth2 ˢ������
 *
 * @author ���Դ��
 */
@TableName(value = "system_oauth2_refresh_token", autoResultMap = true)
// ���� Oracle �� SEQ �����ֳ��������ƣ����Ծ����� system_oauth2_access_token_seq 
�ɣ�����Ҳûɶ����
@KeySequence("system_oauth2_access_token_seq") // ���� Oracle��PostgreSQL��King
base��DB2��H2 ���ݿ����������������� MySQL �����ݿ⣬�ɲ�д��
@Data
public class OAuth2RefreshTokenDO extends TenantBaseDO {

    /**
     * ��ţ����ݿ��ֵ�
     */
    private Long id;
    /**
     * ˢ������
     */
    private String refreshToken;
    /**
     * �û����
     */
    private Long userId;
    /**
     * �û�����
     *
     * ö�� {@link UserTypeEnum}
     */
    private Integer userType;
    /**
     * �ͻ��˱��
     *
     * ���� {@link OAuth2ClientDO#getId()}
     */
    private String clientId;
    /**
     * ��Ȩ��Χ
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> scopes;
    /**
     * ����ʱ��
     */
    private LocalDateTime expiresTime;

}
--- services/zhongshu-core/zszj-module-system/src/test/java/cn/zszj/module/syst
em/service/oauth2/OAuth2TokenServiceImplTest.java
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
 * {@link OAuth2TokenServiceImpl} �ĵ�Ԫ������
 *
 * @author ���Դ��
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
        // ׼������
        Long userId = randomLongId();
        Integer userType = UserTypeEnum.ADMIN.getValue();
        String clientId = randomString();
        List<String> scopes = Lists.newArrayList("read", "write");
        // mock ����
        OAuth2ClientDO clientDO = randomPojo(OAuth2ClientDO.class).setClientId(
clientId)
                .setAccessTokenValiditySeconds(30).setRefreshTokenValiditySecon
ds(60);
        when(oauth2ClientService.validOAuthClientFromCache(eq(clientId))).thenR
eturn(clientDO);
        // mock ���ݣ��û���
        AdminUserDO user = randomPojo(AdminUserDO.class);
        when(adminUserService.getUser(userId)).thenReturn(user);

        // ����
        OAuth2AccessTokenDO accessTokenDO = oauth2TokenService.createAccessToke
n(userId, userType, clientId, scopes);
        // ���Է�������
        OAuth2AccessTokenDO dbAccessTokenDO = oauth2AccessTokenMapper.selectByA
ccessToken(accessTokenDO.getAccessToken());
        // TODO @��ܵ��expiresTime �����Σ��� win11 �Ḵ�֣���������޸���
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
        // ���Է������ƵĻ���
        OAuth2AccessTokenDO redisAccessTokenDO = oauth2AccessTokenRedisDAO.get(
accessTokenDO.getAccessToken());
        // TODO @��ܵ��expiresTime �����Σ��� win11 �Ḵ�֣���������޸���
        assertPojoEquals(accessTokenDO, redisAccessTokenDO, "expiresTime", "cre
ateTime", "updateTime", "deleted");
        // ����ˢ������
        OAuth2RefreshTokenDO refreshTokenDO = oauth2RefreshTokenMapper.selectLi
st().get(0);
        assertPojoEquals(accessTokenDO, refreshTokenDO, "id", "expiresTime", "c
reateTime", "updateTime", "deleted");
        assertFalse(DateUtils.isExpired(refreshTokenDO.getExpiresTime()));
    }

    @Test
    public void testRefreshAccessToken_null() {
        // ׼������
        String refreshToken = randomString();
        String clientId = randomString();
        // mock ����

        // ���ã�������
        assertServiceException(() -> oauth2TokenService.refreshAccessToken(refr
eshToken, clientId),
                new ErrorCode(400, "��Ч��ˢ������"));
    }

    @Test
    public void testRefreshAccessToken_clientIdError() {
        // ׼������
        String refreshToken = randomString();
        String clientId = randomString();
        // mock ����
        OAuth2ClientDO clientDO = randomPojo(OAuth2ClientDO.class).setClientId(
clientId);
        when(oauth2ClientService.validOAuthClientFromCache(eq(clientId))).thenR
eturn(clientDO);
        // mock ���ݣ��������ƣ�
        OAuth2RefreshTokenDO refreshTokenDO = randomPojo(OAuth2RefreshTokenDO.c
lass)
                .setRefreshToken(refreshToken).setClientId("error");
        oauth2RefreshTokenMapper.insert(refreshTokenDO);

        // ���ã�������
        assertServiceException(() -> oauth2TokenService.refreshAccessToken(refr
eshToken, clientId),
                new ErrorCode(400, "ˢ�����ƵĿͻ��˱�Ų���ȷ"));
    }

    @Test
    public void testRefreshAccessToken_expired() {
        // ׼������
        String refreshToken = randomString();
        String clientId = randomString();
        // mock ����
        OAuth2ClientDO clientDO = randomPojo(OAuth2ClientDO.class).setClientId(
clientId);
        when(oauth2ClientService.validOAuthClientFromCache(eq(clientId))).thenR
eturn(clientDO);
        // mock ���ݣ��������ƣ�
        OAuth2RefreshTokenDO refreshTokenDO = randomPojo(OAuth2RefreshTokenDO.c
lass)
                .setRefreshToken(refreshToken).setClientId(clientId)
                .setExpiresTime(LocalDateTime.now().minusDays(1));
        oauth2RefreshTokenMapper.insert(refreshTokenDO);

        // ���ã�������
        assertServiceException(() -> oauth2TokenService.refreshAccessToken(refr
eshToken, clientId),
                new ErrorCode(401, "ˢ�������ѹ���"));
        assertEquals(0, oauth2AccessTokenMapper.selectCount());
    }

    @Test
    public void testRefreshAccessToken_success() {
        TenantContextHolder.setTenantId(0L);
        // ׼������
        String refreshToken = randomString();
        String clientId = randomString();
        // mock ����
        OAuth2ClientDO clientDO = randomPojo(OAuth2ClientDO.class).setClientId(
clientId)
                .setAccessTokenValiditySeconds(30);
        when(oauth2ClientService.validOAuthClientFromCache(eq(clientId))).thenR
eturn(clientDO);
        // mock ���ݣ��������ƣ�
        OAuth2RefreshTokenDO refreshTokenDO = randomPojo(OAuth2RefreshTokenDO.c
lass, o ->
                o.setRefreshToken(refreshToken).setClientId(clientId)
                        .setExpiresTime(LocalDateTime.now().plusDays(1))
                        .setUserType(UserTypeEnum.ADMIN.getValue())
                        .setTenantId(TenantContextHolder.getTenantId()));
        oauth2RefreshTokenMapper.insert(refreshTokenDO);
        // mock ���ݣ��������ƣ�
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.clas
s).setRefreshToken(refreshToken)
                .setUserType(refreshTokenDO.getUserType());
        oauth2AccessTokenMapper.insert(accessTokenDO);
        oauth2AccessTokenRedisDAO.set(accessTokenDO);
        // mock ���ݣ��û���
        AdminUserDO user = randomPojo(AdminUserDO.class);
        when(adminUserService.getUser(refreshTokenDO.getUserId())).thenReturn(u
ser);

        // ����
        OAuth2AccessTokenDO newAccessTokenDO = oauth2TokenService.refreshAccess
Token(refreshToken, clientId);
        // ���ԣ��ϵķ������Ʊ�ɾ��
        assertNull(oauth2AccessTokenMapper.selectByAccessToken(accessTokenDO.ge
tAccessToken()));
        assertNull(oauth2AccessTokenRedisDAO.get(accessTokenDO.getAccessToken()
));
        // ���ԣ��µķ�������
        OAuth2AccessTokenDO dbAccessTokenDO = oauth2AccessTokenMapper.selectByA
ccessToken(newAccessTokenDO.getAccessToken());
        // TODO @��ܵ��expiresTime �����Σ��� win11 �Ḵ�֣���������޸���
        assertPojoEquals(newAccessTokenDO, dbAccessTokenDO, "expiresTime", "cre
ateTime", "updateTime", "deleted");
        assertPojoEquals(newAccessTokenDO, refreshTokenDO, "id", "expiresTime",
 "createTime", "updateTime", "deleted",
                "creator", "updater");
        assertFalse(DateUtils.isExpired(newAccessTokenDO.getExpiresTime()));
        // ���ԣ��µķ������ƵĻ���
        OAuth2AccessTokenDO redisAccessTokenDO = oauth2AccessTokenRedisDAO.get(
newAccessTokenDO.getAccessToken());
        // TODO @��ܵ��expiresTime �����Σ��� win11 �Ḵ�֣���������޸���
        assertPojoEquals(newAccessTokenDO, redisAccessTokenDO, "expiresTime", "
createTime", "updateTime", "deleted");
    }

    @Test
    public void testGetAccessToken() {
        // mock ���ݣ��������ƣ�
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.clas
s)
                .setExpiresTime(LocalDateTime.now().plusDays(1));
        oauth2AccessTokenMapper.insert(accessTokenDO);
        // ׼������
        String accessToken = accessTokenDO.getAccessToken();

        // ����
        OAuth2AccessTokenDO result = oauth2TokenService.getAccessToken(accessTo
ken);
        // ����
        // TODO @��ܵ��expiresTime �����Σ��� win11 �Ḵ�֣���������޸���
        assertPojoEquals(accessTokenDO, result, "expiresTime", "createTime", "u
pdateTime", "deleted",
                "creator", "updater");
        // TODO @��ܵ��expiresTime �����Σ��� win11 �Ḵ�֣���������޸���
        assertPojoEquals(accessTokenDO, oauth2AccessTokenRedisDAO.get(accessTok
en), "expiresTime", "createTime", "updateTime", "deleted",
                "creator", "updater");
    }

    @Test
    public void testCheckAccessToken_null() {
        // ���У�������
        assertServiceException(() -> oauth2TokenService.checkAccessToken(random
String()),
                new ErrorCode(401, "�������Ʋ�����"));
    }

    @Test
    public void testCheckAccessToken_expired() {
        // mock ���ݣ��������ƣ�
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.clas
s)
                .setExpiresTime(LocalDateTime.now().minusDays(1));
        oauth2AccessTokenMapper.insert(accessTokenDO);
        // ׼������
        String accessToken = accessTokenDO.getAccessToken();

        // ���У�������
        assertServiceException(() -> oauth2TokenService.checkAccessToken(access
Token),
                new ErrorCode(401, "���������ѹ���"));
    }

    @Test
    public void testGetAccessToken_refreshTokenAsAccess_legacyZeroUserId() {
        // ZS-LOGIN-001��gate=false ������ userId ȡֵ���� 0L����ˢ������һ�ɲ��ñ�ת�
�Ϊ�������ƣ����ſش�����·���� null�������� convertToAccessToken��
        // mock ���ݣ��������ƣ�
        OAuth2RefreshTokenDO refreshTokenDO = randomPojo(OAuth2RefreshTokenDO.c
lass)
                .setUserId(0L)
                .setExpiresTime(LocalDateTime.now().plusDays(1));
        oauth2RefreshTokenMapper.insert(refreshTokenDO);
        // ׼������
        String accessToken = refreshTokenDO.getRefreshToken();

        // ���У�������
        OAuth2AccessTokenDO result = oauth2TokenService.getAccessToken(accessTo
ken);
        // ����
        // ZS-LOGIN-001��Ĭ�� gate �رգ�ˢ�����Ʋ��ٱ���Ĭת��Ϊ��������
        assertNull(result, "ˢ�����Ʋ��ñ������������ƾ�Ĭת��");
    }

    @Test
    public void testCheckAccessToken_success() {
        // mock ���ݣ��������ƣ�
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.clas
s)
                .setExpiresTime(LocalDateTime.now().plusDays(1));
        oauth2AccessTokenMapper.insert(accessTokenDO);
        // ׼������
        String accessToken = accessTokenDO.getAccessToken();

        // ���У�������
        OAuth2AccessTokenDO result = oauth2TokenService.getAccessToken(accessTo
ken);
        // ����
        // TODO @��ܵ��expiresTime �����Σ��� win11 �Ḵ�֣���������޸���
        assertPojoEquals(accessTokenDO, result, "expiresTime", "createTime", "u
pdateTime", "deleted",
                "creator", "updater");
    }

    @Test
    public void testRemoveAccessToken_null() {
        // ���ã�������
        assertNull(oauth2TokenService.removeAccessToken(randomString()));
    }

    @Test
    public void testRemoveAccessToken_success() {
        // mock ���ݣ��������ƣ�
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.clas
s)
                .setExpiresTime(LocalDateTime.now().plusDays(1));
        oauth2AccessTokenMapper.insert(accessTokenDO);
        // mock ���ݣ�ˢ�����ƣ�
        OAuth2RefreshTokenDO refreshTokenDO = randomPojo(OAuth2RefreshTokenDO.c
lass)
                .setRefreshToken(accessTokenDO.getRefreshToken());
        oauth2RefreshTokenMapper.insert(refreshTokenDO);
        // ����
        OAuth2AccessTokenDO result = oauth2TokenService.removeAccessToken(acces
sTokenDO.getAccessToken());
        // TODO @��ܵ��expiresTime �����Σ��� win11 �Ḵ�֣���������޸���
        assertPojoEquals(accessTokenDO, result, "expiresTime", "createTime", "u
pdateTime", "deleted",
                "creator", "updater");
        // ��������
        assertNull(oauth2AccessTokenMapper.selectByAccessToken(accessTokenDO.ge
tAccessToken()));
        assertNull(oauth2RefreshTokenMapper.selectByRefreshToken(accessTokenDO.
getRefreshToken()));
        assertNull(oauth2AccessTokenRedisDAO.get(accessTokenDO.getAccessToken()
));
    }


    @Test
    public void testGetAccessTokenPage() {
        // mock ����
        OAuth2AccessTokenDO dbAccessToken = randomPojo(OAuth2AccessTokenDO.clas
s, o -> { // �Ȼ��ѯ��
            o.setUserId(10L);
            o.setUserType(1);
            o.setClientId("test_client");
            o.setExpiresTime(LocalDateTime.now().plusDays(1));
        });
        oauth2AccessTokenMapper.insert(dbAccessToken);
        // ���� userId ��ƥ��
        oauth2AccessTokenMapper.insert(cloneIgnoreId(dbAccessToken, o -> o.setU
serId(20L)));
        // ���� userType ��ƥ��
        oauth2AccessTokenMapper.insert(cloneIgnoreId(dbAccessToken, o -> o.setU
serType(2)));
        // ���� userType ��ƥ��
        oauth2AccessTokenMapper.insert(cloneIgnoreId(dbAccessToken, o -> o.setC
lientId("it_client")));
        // ���� expireTime ��ƥ��
        oauth2AccessTokenMapper.insert(cloneIgnoreId(dbAccessToken, o -> o.setE
xpiresTime(LocalDateTimeUtil.now())));
        // ׼������
        OAuth2AccessTokenPageReqVO reqVO = new OAuth2AccessTokenPageReqVO();
        reqVO.setUserId(10L);
        reqVO.setUserType(1);
        reqVO.setClientId("test");

        // ����
        PageResult<OAuth2AccessTokenDO> pageResult = oauth2TokenService.getAcce
ssTokenPage(reqVO);
        // ����
        assertEquals(1, pageResult.getTotal());
        assertEquals(1, pageResult.getList().size());
        // TODO @��ܵ��expiresTime �����Σ��� win11 �Ḵ�֣���������޸���
        assertPojoEquals(dbAccessToken, pageResult.getList().get(0), "expiresTi
me");
    }

    /**
     * ZS-LOGIN-001��������;���� ���� Ĭ�Ͻ�ֹˢ�����ƾ�Ĭת�������ƣ�getAccessToken ��
�� null
     */
    @Test
    public void testGetAccessToken_refreshTokenAsAccess_shouldReturnNullByDefau
lt() {
        // ����һ����Ч��ˢ�����ƣ�δ�����ڣ�
        OAuth2RefreshTokenDO refreshTokenDO = randomPojo(OAuth2RefreshTokenDO.c
lass)
                .setUserId(randomLongId())
                .setUserType(UserTypeEnum.ADMIN.getValue())
                .setClientId(randomString())
                .setExpiresTime(LocalDateTime.now().plusDays(1));
        oauth2RefreshTokenMapper.insert(refreshTokenDO);
        // ��ˢ�������ַ��������������ƴ���
        String accessToken = refreshTokenDO.getRefreshToken();
        // ��֤ Redis ��û�и� token
        assertNull(oauth2AccessTokenRedisDAO.get(accessToken));

        // ���ã��������� null��gate Ĭ�� false�����þ�Ĭ����ת����
        OAuth2AccessTokenDO result = oauth2TokenService.getAccessToken(accessTo
ken);
        assertNull(result, "ˢ�����Ʋ��ñ������������ƾ�Ĭת��");
    }

    @Test
    public void testCheckAccessToken_refreshTokenAsAccess_shouldThrowUnauthoriz
ed() {
        // ����һ����Ч��ˢ�����ƣ�δ�����ڣ�
        OAuth2RefreshTokenDO refreshTokenDO = randomPojo(OAuth2RefreshTokenDO.c
lass)
                .setUserId(randomLongId())
                .setUserType(UserTypeEnum.ADMIN.getValue())
                .setClientId(randomString())
                .setExpiresTime(LocalDateTime.now().plusDays(1));
        oauth2RefreshTokenMapper.insert(refreshTokenDO);
        String accessToken = refreshTokenDO.getRefreshToken();

        // checkAccessToken -> getAccessToken -> null -> �� UNAUTHORIZED
        assertServiceException(() -> oauth2TokenService.checkAccessToken(access
Token),
                new ErrorCode(401, "�������Ʋ�����"));
    }

    /**
     * ZS-LOGIN-001��codex P1 �޸����أ���gate=false ʱ����ǰ�����ڣ�gate=true���� c
onvertToAccessToken
     * ����� Redis �ġ��ϳɷ������ơ������� accessToken==refreshToken�������ٱ� chec
kAccessToken ���У�
     * ��Ӧ��������������� codex P1���ϳ������� getAccessToken �� Redis ������ǰ r
eturn���ƹ��·��ſء�
     */
    @Test
    public void testCheckAccessToken_cachedSyntheticToken_compatDisabled_should
ThrowUnauthorizedAndEvict() {
        // ���졸��ǰ gate=true ʱ�� convertToAccessToken ����� Redis �ĺϳ����ơ���ac
cessToken==refreshToken������⡢�̳г�Ч TTL
        String syntheticToken = randomString();
        OAuth2AccessTokenDO cachedSynthetic = randomPojo(OAuth2AccessTokenDO.cl
ass)
                .setAccessToken(syntheticToken)
                .setRefreshToken(syntheticToken)
                .setUserType(UserTypeEnum.ADMIN.getValue())
                .setExpiresTime(LocalDateTime.now().plusDays(30));
        oauth2AccessTokenRedisDAO.set(cachedSynthetic);
        // ǰ�ã�Redis ���С�MySQL �޴˷������ƣ��ϳ����Ʊ��Ͳ���⣩
        assertNotNull(oauth2AccessTokenRedisDAO.get(syntheticToken));
        assertNull(oauth2AccessTokenMapper.selectByAccessToken(syntheticToken))
;

        // ���ã������ԣ�gate=false �»���ĺϳ����Ʋ��÷��� �� �� UNAUTHORIZED
        assertServiceException(() -> oauth2TokenService.checkAccessToken(synthe
ticToken),
                new ErrorCode(401, "�������Ʋ�����"));
        // ���ԣ���Ⱦ��Ŀ���������
        assertNull(oauth2AccessTokenRedisDAO.get(syntheticToken),
                "gate �ر�ʱӦ�����������ĺϳ�������Ⱦ��Ŀ");
    }

    /**
     * ZS-LOGIN-001��P1 �޸�������գ���gate=false ʱ��Redis ����ġ���ʵ�������ơ�
     * ��accessToken��refreshToken�����ܺϳ����������߼�Ӱ�죬��Ӧ�������з��ء�
     */
    @Test
    public void testGetAccessToken_cachedRealToken_compatDisabled_shouldStillRe
turn() {
        // ����һ���ѻ������ʵ�������ƣ�accessToken �� refreshToken Ϊ������������
        OAuth2AccessTokenDO realToken = randomPojo(OAuth2AccessTokenDO.class)
                .setExpiresTime(LocalDateTime.now().plusDays(1));
        oauth2AccessTokenRedisDAO.set(realToken);
        assertNotEquals(realToken.getAccessToken(), realToken.getRefreshToken()
);

        // ���ã������ԣ���ʵ���������Դ� Redis ���з��أ�δ������
        OAuth2AccessTokenDO result = oauth2TokenService.getAccessToken(realToke
n.getAccessToken());
        assertNotNull(result, "gate �ر�ʱ��ʵ�������ƣ�accessToken��refreshToken��
��Ӧ��������");
        assertEquals(realToken.getAccessToken(), result.getAccessToken());
    }

}
--- services/zhongshu-core/zszj-module-system/src/test/java/cn/zszj/module/syst
em/service/oauth2/OAuth2TokenServiceImplCompatTest.java
package cn.zszj.module.system.service.oauth2;

import cn.zszj.framework.common.enums.UserTypeEnum;
import cn.zszj.framework.test.core.ut.BaseDbAndRedisUnitTest;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2RefreshTokenDO;
import cn.zszj.module.system.dal.dataobject.user.AdminUserDO;
import cn.zszj.module.system.dal.mysql.oauth2.OAuth2RefreshTokenMapper;
import cn.zszj.module.system.dal.redis.oauth2.OAuth2AccessTokenRedisDAO;
import cn.zszj.module.system.service.user.AdminUserService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;

import static cn.zszj.framework.test.core.util.RandomUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * {@link OAuth2TokenServiceImpl} ���ݿ��أ�gate=true����֧�ĵ�Ԫ�����ࡣ
 *
 * <p>ZS-LOGIN-001��ѡ�� C ������ʵ������Ϊ {@code refresh-token-as-access-token-e
nabled=true}��
 * �÷�֧ΪǨ���ڼ���·���������в��Կ��ء�����ͨ�� {@link TestPropertySource} �� Environ
ment ����ע���ſأ�
 * <b>�Ͻ�</b>ʹ�� {@code ReflectionTestUtils.setField}����{@code OAuth2TokenServ
iceImpl} �� {@code @Transactional} ������
 * ע����� CGLIB ������setField ��д����������Ŀ��������"����û��Ч"�ļ��̡�
 *
 * @author ZS-LOGIN-001
 */
@Import({OAuth2TokenServiceImpl.class, OAuth2AccessTokenRedisDAO.class})
@TestPropertySource(properties = "zszj.security.refresh-token-as-access-token-e
nabled=true")
public class OAuth2TokenServiceImplCompatTest extends BaseDbAndRedisUnitTest {

    @Resource
    private OAuth2TokenServiceImpl oauth2TokenService;

    @Resource
    private OAuth2RefreshTokenMapper oauth2RefreshTokenMapper;

    @Resource
    private OAuth2AccessTokenRedisDAO oauth2AccessTokenRedisDAO;

    @MockitoBean
    private OAuth2ClientService oauth2ClientService;
    @MockitoBean
    private AdminUserService adminUserService;

    @Test
    public void testGetAccessToken_unexpiredRefreshToken_compatEnabled_shouldCo
nvert() {
        // mock ���ݣ��û�����ADMIN ����ת��ʱ buildUserInfo ����� adminUserService
.getUser�������׮���� NPE
        Long userId = randomLongId();
        AdminUserDO user = randomPojo(AdminUserDO.class);
        when(adminUserService.getUser(userId)).thenReturn(user);
        // ����һ��δ���ڵ�ˢ������
        OAuth2RefreshTokenDO refreshTokenDO = randomPojo(OAuth2RefreshTokenDO.c
lass)
                .setUserId(userId).setUserType(UserTypeEnum.ADMIN.getValue())
                .setClientId(randomString()).setExpiresTime(LocalDateTime.now()
.plusDays(1));
        oauth2RefreshTokenMapper.insert(refreshTokenDO);
        String accessToken = refreshTokenDO.getRefreshToken();
        assertNull(oauth2AccessTokenRedisDAO.get(accessToken)); // ǰ�ã��ų������
���

        // ���ã����ݿ��ؿ���ʱ��δ����ˢ������Ӧ��ת��Ϊ��������
        OAuth2AccessTokenDO result = oauth2TokenService.getAccessToken(accessTo
ken);
        assertNotNull(result, "���ݿ��ؿ���ʱ��δ����ˢ������Ӧ��ת��Ϊ��������");
        assertEquals(accessToken, result.getAccessToken());       // convertToA
ccessToken �� refreshToken ���� accessToken
        assertEquals(refreshTokenDO.getUserId(), result.getUserId());
        assertEquals(refreshTokenDO.getUserType(), result.getUserType());
        assertEquals(refreshTokenDO.getClientId(), result.getClientId());
    }

    @Test
    public void testGetAccessToken_expiredRefreshToken_compatEnabled_shouldRetu
rnNull() {
        // ����һ���ѹ��ڵ�ˢ������
        OAuth2RefreshTokenDO refreshTokenDO = randomPojo(OAuth2RefreshTokenDO.c
lass)
                .setUserId(randomLongId()).setUserType(UserTypeEnum.ADMIN.getVa
lue())
                .setClientId(randomString()).setExpiresTime(LocalDateTime.now()
.minusDays(1)); // �ѹ���
        oauth2RefreshTokenMapper.insert(refreshTokenDO);
        String accessToken = refreshTokenDO.getRefreshToken();

        // ���ã������ԣ����ݿ��ؿ���ʱ���ѹ���ˢ�������Բ���ת����DateUtils.isExpired У
���뱣����
        assertNull(oauth2TokenService.getAccessToken(accessToken),
                "���ݿ��ؿ���ʱ���ѹ���ˢ�������Բ���ת����DateUtils.isExpired У���뱣��
��");
    }

}
services/zhongshu-core\zszj-server\src\main\resources\application.yaml:302:    
refresh-token-as-access-token-enabled: true
services/zhongshu-core\zszj-module-system\src\test\java\cn\zszj\module\system\s
ervice\oauth2\OAuth2TokenServiceImplTest.java:17:import cn.zszj.module.system.d
al.redis.oauth2.OAuth2AccessTokenRedisDAO;
services/zhongshu-core\zszj-module-system\src\test\java\cn\zszj\module\system\s
ervice\oauth2\OAuth2TokenServiceImplTest.java:42:@Import({OAuth2TokenServiceImp
l.class, OAuth2AccessTokenRedisDAO.class})
services/zhongshu-core\zszj-module-system\src\test\java\cn\zszj\module\system\s
ervice\oauth2\OAuth2TokenServiceImplTest.java:43:@TestPropertySource(properties
 = "zszj.security.refresh-token-as-access-token-enabled=false")
services/zhongshu-core\zszj-module-system\src\test\java\cn\zszj\module\system\s
ervice\oauth2\OAuth2TokenServiceImplTest.java:55:    private OAuth2AccessTokenR
edisDAO oauth2AccessTokenRedisDAO;
services/zhongshu-core\zszj-module-system\src\test\java\cn\zszj\module\system\s
ervice\oauth2\OAuth2TokenServiceImplTest.java:239:        // ZS-LOGIN-001：gate=
false 下无论 userId 取值（含 0L），刷新令牌一律不得被转换为访问令牌（在门控处即短路返回 null，不进入 convertToAccessTo
ken）
services/zhongshu-core\zszj-module-system\src\test\java\cn\zszj\module\system\s
ervice\oauth2\OAuth2TokenServiceImplTest.java:372:     * ZS-LOGIN-001（codex P1 
修复看守）：gate=false 时，此前兼容期（gate=true）由 convertToAccessToken
services/zhongshu-core\zszj-module-system\src\test\java\cn\zszj\module\system\s
ervice\oauth2\OAuth2TokenServiceImplTest.java:378:        // 构造「此前 gate=true 时代
 convertToAccessToken 缓存进 Redis 的合成令牌」：accessToken==refreshToken、不落库、继承长效 TTL
services/zhongshu-core\zszj-module-system\src\test\java\cn\zszj\module\system\s
ervice\oauth2\OAuth2TokenServiceImplCompatTest.java:9:import cn.zszj.module.sys
tem.dal.redis.oauth2.OAuth2AccessTokenRedisDAO;
services/zhongshu-core\zszj-module-system\src\test\java\cn\zszj\module\system\s
ervice\oauth2\OAuth2TokenServiceImplCompatTest.java:26: * <p>ZS-LOGIN-001：选项 C 
下现网实际配置为 {@code refresh-token-as-access-token-enabled=true}，
services/zhongshu-core\zszj-module-system\src\test\java\cn\zszj\module\system\s
ervice\oauth2\OAuth2TokenServiceImplCompatTest.java:33:@Import({OAuth2TokenServ
iceImpl.class, OAuth2AccessTokenRedisDAO.class})
services/zhongshu-core\zszj-module-system\src\test\java\cn\zszj\module\system\s
ervice\oauth2\OAuth2TokenServiceImplCompatTest.java:34:@TestPropertySource(prop
erties = "zszj.security.refresh-token-as-access-token-enabled=true")
services/zhongshu-core\zszj-module-system\src\test\java\cn\zszj\module\system\s
ervice\oauth2\OAuth2TokenServiceImplCompatTest.java:44:    private OAuth2Access
TokenRedisDAO oauth2AccessTokenRedisDAO;
services/zhongshu-core\zszj-module-system\src\test\java\cn\zszj\module\system\s
ervice\oauth2\OAuth2TokenServiceImplCompatTest.java:68:        assertEquals(acc
essToken, result.getAccessToken());       // convertToAccessToken 用 refreshToke
n 串作 accessToken
services/zhongshu-core\zszj-module-system\src\main\java\cn\zszj\module\system\s
ervice\oauth2\OAuth2TokenServiceImpl.java:24:import cn.zszj.module.system.dal.r
edis.oauth2.OAuth2AccessTokenRedisDAO;
services/zhongshu-core\zszj-module-system\src\main\java\cn\zszj\module\system\s
ervice\oauth2\OAuth2TokenServiceImpl.java:57:    private OAuth2AccessTokenRedis
DAO oauth2AccessTokenRedisDAO;
services/zhongshu-core\zszj-module-system\src\main\java\cn\zszj\module\system\s
ervice\oauth2\OAuth2TokenServiceImpl.java:74:    @Value("${zszj.security.refres
h-token-as-access-token-enabled:false}")
services/zhongshu-core\zszj-module-system\src\main\java\cn\zszj\module\system\s
ervice\oauth2\OAuth2TokenServiceImpl.java:81:                    + "（zszj.secur
ity.refresh-token-as-access-token-enabled=true）：刷新令牌可被当作访问令牌使用，"
services/zhongshu-core\zszj-module-system\src\main\java\cn\zszj\module\system\s
ervice\oauth2\OAuth2TokenServiceImpl.java:135:            // ZS-LOGIN-001（codex
 P1 修复）：门控关闭时，拒绝此前兼容期（gate=true）由 convertToAccessToken
services/zhongshu-core\zszj-module-system\src\main\java\cn\zszj\module\system\s
ervice\oauth2\OAuth2TokenServiceImpl.java:154:                accessTokenDO = c
onvertToAccessToken(refreshTokenDO);
services/zhongshu-core\zszj-module-system\src\main\java\cn\zszj\module\system\s
ervice\oauth2\OAuth2TokenServiceImpl.java:243:    private OAuth2AccessTokenDO c
onvertToAccessToken(OAuth2RefreshTokenDO refreshTokenDO) {
services/zhongshu-core\zszj-module-system\src\main\java\cn\zszj\module\system\s
ervice\oauth2\OAuth2TokenServiceImpl.java:253:     * <p>{@link #convertToAccess
Token} 以 refreshToken 串同时充当 accessToken 与 refreshToken，故二者相等即合成令牌；
services/zhongshu-core\zszj-module-system\src\main\java\cn\zszj\module\system\d
al\redis\oauth2\OAuth2AccessTokenRedisDAO.java:25:public class OAuth2AccessToke
nRedisDAO {
services/zhongshu-core\zszj-module-system\src\main\java\cn\zszj\module\system\d
al\redis\oauth2\OAuth2AccessTokenRedisDAO.java:51:        List<String> redisKey
s = CollectionUtils.convertList(accessTokens, OAuth2AccessTokenRedisDAO::format
Key);

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "rg -
n 'getAccessToken\\(|checkAccessToken\\(' services/zhongshu-core/zszj-module-sy
stem/src/main services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-se
curity services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-websocket
; rg --files services/zhongshu-core | rg '(BaseDbAndRedisUnitTest|RandomUtils|B
eanUtils|JsonUtils|BaseRedisUnitTest|RedisTestConfiguration|pom.xml)"'$|/(BaseD
bAndRedisUnitTest|RandomUtils|BeanUtils|JsonUtils|RedisTestConfiguration)'"\\.j
ava"'$'"'; rg -n '.*' services/zhongshu-core/zszj-framework/zszj-spring-boot-st
arter-test/src/main/java/cn/zszj/framework/test/core/util/RandomUtils.java serv
ices/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/java/c
n/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java services/zhongshu-cor
e/zszj-framework/zszj-common/src/main/java/cn/zszj/framework/common/util/object
/BeanUtils.java; git diff 678bd221"'^ 678bd221 --check' in E:\众墅之家AI赋能平台底座
 succeeded in 91ms:
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:1:package cn.zszj
.framework.test.core.ut;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:2:
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:3:import cn.hutoo
l.extra.spring.SpringUtil;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:4:import cn.zszj.
framework.datasource.config.ZszjDataSourceAutoConfiguration;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:5:import cn.zszj.
framework.mybatis.config.ZszjMybatisAutoConfiguration;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:6:import cn.zszj.
framework.redis.config.ZszjRedisAutoConfiguration;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:7:import cn.zszj.
framework.test.config.RedisTestConfiguration;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:8:import cn.zszj.
framework.test.config.SqlInitializationTestConfiguration;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:9:import com.alib
aba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:10:import com.bao
midou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:11:import org.red
isson.spring.starter.RedissonAutoConfigurationV2;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:12:import org.spr
ingframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:13:import org.spr
ingframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:14:import org.spr
ingframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfigurat
ion;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:15:import org.spr
ingframework.boot.test.context.SpringBootTest;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:16:import org.spr
ingframework.context.annotation.Import;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:17:import org.spr
ingframework.test.context.ActiveProfiles;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:18:import org.spr
ingframework.test.context.jdbc.Sql;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:19:
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:20:/**
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:21: * 依赖内存 DB + R
edis 的单元测试
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:22: *
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:23: * 相比 {@link B
aseDbUnitTest} 来说，额外增加了内存 Redis
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:24: *
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:25: * @author 芋道源
码
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:26: */
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:27:@SpringBootTes
t(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = BaseDbAndRedis
UnitTest.Application.class)
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:28:@ActiveProfile
s("unit-test") // 设置使用 application-unit-test 配置文件
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:29:@Sql(scripts =
 "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD) // 每个
单元测试结束后，清理 DB
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:30:public class B
aseDbAndRedisUnitTest {
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:31:
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:32:    @Import({
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:33:            //
 DB 配置类
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:34:            Zs
zjDataSourceAutoConfiguration.class, // 自己的 DB 配置类
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:35:            Da
taSourceAutoConfiguration.class, // Spring DB 自动配置类
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:36:            Da
taSourceTransactionManagerAutoConfiguration.class, // Spring 事务自动配置类
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:37:            Dr
uidDataSourceAutoConfigure.class, // Druid 自动配置类
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:38:            Sq
lInitializationTestConfiguration.class, // SQL 初始化
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:39:            //
 MyBatis 配置类
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:40:            Zs
zjMybatisAutoConfiguration.class, // 自己的 MyBatis 配置类
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:41:            My
batisPlusAutoConfiguration.class, // MyBatis 的自动配置类
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:42:
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:43:            //
 Redis 配置类
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:44:            Re
disTestConfiguration.class, // Redis 测试配置类，用于启动 RedisServer
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:45:            Zs
zjRedisAutoConfiguration.class, // 自己的 Redis 配置类
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:46:            Re
disAutoConfiguration.class, // Spring Redis 自动配置类
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:47:            Re
dissonAutoConfigurationV2.class, // Redisson 自动配置类
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:48:
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:49:            //
 其它配置类
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:50:            Sp
ringUtil.class
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:51:    })
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:52:    public sta
tic class Application {
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:53:    }
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:54:
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/ut/BaseDbAndRedisUnitTest.java:55:}
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:1:package cn.zszj.framework.common.util.ob
ject;
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:2:
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:3:import cn.hutool.core.bean.BeanUtil;
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:4:import cn.zszj.framework.common.pojo.Pag
eResult;
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:5:import cn.zszj.framework.common.util.col
lection.CollectionUtils;
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:6:
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:7:import java.util.List;
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:8:import java.util.function.Consumer;
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:9:
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:10:/**
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:11: * Bean 工具类
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:12: *
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:13: * 1. 默认使用 {@link cn.hutool.core.bean.B
eanUtil} 作为实现类，虽然不同 bean 工具的性能有差别，但是对绝大多数同学的项目，不用在意这点性能
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:14: * 2. 针对复杂的对象转换，可以搜参考 AuthConvert 实现，通过
 mapstruct + default 配合实现
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:15: *
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:16: * @author 芋道源码
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:17: */
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:18:public class BeanUtils {
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:19:
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:20:    public static <T> T toBean(Object s
ource, Class<T> targetClass) {
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:21:        return BeanUtil.toBean(source, 
targetClass);
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:22:    }
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:23:
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:24:    public static <T> T toBean(Object s
ource, Class<T> targetClass, Consumer<T> peek) {
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:25:        T target = toBean(source, targe
tClass);
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:26:        if (target != null) {
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:27:            peek.accept(target);
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:28:        }
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:29:        return target;
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:30:    }
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:31:
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:32:    public static <S, T> List<T> toBean
(List<S> source, Class<T> targetType) {
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:33:        if (source == null) {
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:34:            return null;
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:35:        }
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:36:        return CollectionUtils.convertL
ist(source, s -> toBean(s, targetType));
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:37:    }
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:38:
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:39:    public static <S, T> List<T> toBean
(List<S> source, Class<T> targetType, Consumer<T> peek) {
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:40:        List<T> list = toBean(source, t
argetType);
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:41:        if (list != null) {
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:42:            list.forEach(peek);
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:43:        }
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:44:        return list;
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:45:    }
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:46:
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:47:    public static <S, T> PageResult<T> 
toBean(PageResult<S> source, Class<T> targetType) {
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:48:        return toBean(source, targetTyp
e, null);
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:49:    }
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:50:
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:51:    public static <S, T> PageResult<T> 
toBean(PageResult<S> source, Class<T> targetType, Consumer<T> peek) {
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:52:        if (source == null) {
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:53:            return null;
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:54:        }
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:55:        List<T> list = toBean(source.ge
tList(), targetType);
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:56:        if (peek != null) {
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:57:            list.forEach(peek);
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:58:        }
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:59:        return new PageResult<>(list, s
ource.getTotal());
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:60:    }
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:61:
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:62:    public static void copyProperties(O
bject source, Object target) {
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:63:        if (source == null || target ==
 null) {
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:64:            return;
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:65:        }
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:66:        BeanUtil.copyProperties(source,
 target, false);
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:67:    }
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:68:
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/util/object/BeanUtils.java:69:}
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:1:package cn.zszj.framewor
k.test.core.util;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:2:
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:3:import cn.hutool.core.da
te.LocalDateTimeUtil;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:4:import cn.hutool.core.ut
il.ArrayUtil;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:5:import cn.hutool.core.ut
il.RandomUtil;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:6:import cn.hutool.core.ut
il.StrUtil;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:7:import cn.zszj.framework
.common.enums.CommonStatusEnum;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:8:import uk.co.jemos.podam
.api.PodamFactory;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:9:import uk.co.jemos.podam
.api.PodamFactoryImpl;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:10:
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:11:import java.lang.reflec
t.Type;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:12:import java.math.BigDec
imal;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:13:import java.time.LocalD
ateTime;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:14:import java.util.Arrays
;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:15:import java.util.Date;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:16:import java.util.List;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:17:import java.util.Set;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:18:import java.util.functi
on.Consumer;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:19:import java.util.stream
.Collectors;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:20:import java.util.stream
.Stream;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:21:
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:22:/**
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:23: * 随机工具类
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:24: *
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:25: * @author 芋道源码
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:26: */
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:27:public class RandomUtil
s {
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:28:
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:29:    private static fina
l int RANDOM_STRING_LENGTH = 10;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:30:
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:31:    private static fina
l int TINYINT_MAX = 127;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:32:
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:33:    private static fina
l int RANDOM_DATE_MAX = 30;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:34:
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:35:    private static fina
l int RANDOM_COLLECTION_LENGTH = 5;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:36:
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:37:    private static fina
l PodamFactory PODAM_FACTORY = new PodamFactoryImpl();
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:38:
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:39:    static {
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:40:        // 字符串
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:41:        PODAM_FACTORY.g
etStrategy().addOrReplaceTypeManufacturer(String.class,
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:42:                (dataPr
oviderStrategy, attributeMetadata, map) -> randomString());
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:43:        // Integer
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:44:        PODAM_FACTORY.g
etStrategy().addOrReplaceTypeManufacturer(Integer.class, (dataProviderStrategy,
 attributeMetadata, map) -> {
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:45:            // 如果是 stat
us 的字段，返回 0 或 1
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:46:            if ("status
".equals(attributeMetadata.getAttributeName())) {
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:47:                return 
RandomUtil.randomEle(CommonStatusEnum.values()).getStatus();
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:48:            }
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:49:            // 如果是 type
、status 结尾的字段，返回 tinyint 范围
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:50:            if (StrUtil
.endWithAnyIgnoreCase(attributeMetadata.getAttributeName(),
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:51:                    "ty
pe", "status", "category", "scope", "result")) {
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:52:                return 
RandomUtil.randomInt(0, TINYINT_MAX + 1);
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:53:            }
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:54:            return Rand
omUtil.randomInt();
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:55:        });
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:56:        // BigDecimal：限
制精度在 DECIMAL(10,2) 范围内，避免 H2 等数据库溢出
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:57:        PODAM_FACTORY.g
etStrategy().addOrReplaceTypeManufacturer(BigDecimal.class,
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:58:                (dataPr
oviderStrategy, attributeMetadata, map) ->
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:59:                       
 BigDecimal.valueOf(RandomUtil.randomInt(0, 10000000), 2));
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:60:        // LocalDateTim
e
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:61:        PODAM_FACTORY.g
etStrategy().addOrReplaceTypeManufacturer(LocalDateTime.class,
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:62:                (dataPr
oviderStrategy, attributeMetadata, map) -> randomLocalDateTime());
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:63:        // Boolean
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:64:        PODAM_FACTORY.g
etStrategy().addOrReplaceTypeManufacturer(Boolean.class, (dataProviderStrategy,
 attributeMetadata, map) -> {
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:65:            // 如果是 dele
ted 的字段，返回非删除
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:66:            if ("delete
d".equals(attributeMetadata.getAttributeName())) {
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:67:                return 
false;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:68:            }
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:69:            return Rand
omUtil.randomBoolean();
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:70:        });
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:71:    }
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:72:
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:73:    public static Strin
g randomString() {
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:74:        return RandomUt
il.randomString(RANDOM_STRING_LENGTH);
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:75:    }
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:76:
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:77:    public static Long 
randomLongId() {
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:78:        return RandomUt
il.randomLong(0, Long.MAX_VALUE);
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:79:    }
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:80:
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:81:    public static Integ
er randomInteger() {
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:82:        return RandomUt
il.randomInt(0, Integer.MAX_VALUE);
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:83:    }
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:84:
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:85:    public static Date 
randomDate() {
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:86:        return RandomUt
il.randomDay(0, RANDOM_DATE_MAX);
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:87:    }
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:88:
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:89:    public static Local
DateTime randomLocalDateTime() {
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:90:        // 设置 Nano 为零的原
因，避免 MySQL、H2 存储不到时间戳
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:91:        return LocalDat
eTimeUtil.of(randomDate()).withNano(0);
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:92:    }
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:93:
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:94:    public static Short
 randomShort() {
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:95:        return (short) 
RandomUtil.randomInt(0, Short.MAX_VALUE);
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:96:    }
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:97:
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:98:    public static <T> S
et<T> randomSet(Class<T> clazz) {
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:99:        return Stream.i
terate(0, i -> i).limit(RandomUtil.randomInt(1, RANDOM_COLLECTION_LENGTH))
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:100:                .map(i
 -> randomPojo(clazz)).collect(Collectors.toSet());
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:101:    }
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:102:
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:103:    public static Inte
ger randomCommonStatus() {
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:104:        return RandomU
til.randomEle(CommonStatusEnum.values()).getStatus();
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:105:    }
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:106:
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:107:    public static Stri
ng randomEmail() {
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:108:        return randomS
tring() + "@qq.com";
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:109:    }
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:110:
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:111:    public static Stri
ng randomMobile() {
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:112:        return "138001
38" + RandomUtil.randomNumbers(3);
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:113:    }
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:114:
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:115:    public static Stri
ng randomURL() {
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:116:        return "https:
//www.iocoder.cn/" + randomString();
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:117:    }
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:118:
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:119:    @SafeVarargs
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:120:    public static <T> 
T randomPojo(Class<T> clazz, Consumer<T>... consumers) {
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:121:        T pojo = PODAM
_FACTORY.manufacturePojo(clazz);
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:122:        // 非空时，回调逻辑。通过
它，可以实现 Pojo 的进一步处理
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:123:        if (ArrayUtil.
isNotEmpty(consumers)) {
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:124:            Arrays.str
eam(consumers).forEach(consumer -> consumer.accept(pojo));
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:125:        }
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:126:        return pojo;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:127:    }
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:128:
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:129:    @SafeVarargs
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:130:    public static <T> 
T randomPojo(Class<T> clazz, Type type, Consumer<T>... consumers) {
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:131:        T pojo = PODAM
_FACTORY.manufacturePojo(clazz, type);
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:132:        // 非空时，回调逻辑。通过
它，可以实现 Pojo 的进一步处理
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:133:        if (ArrayUtil.
isNotEmpty(consumers)) {
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:134:            Arrays.str
eam(consumers).forEach(consumer -> consumer.accept(pojo));
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:135:        }
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:136:        return pojo;
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:137:    }
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:138:
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:139:    @SafeVarargs
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:140:    public static <T> 
List<T> randomPojoList(Class<T> clazz, Consumer<T>... consumers) {
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:141:        int size = Ran
domUtil.randomInt(1, RANDOM_COLLECTION_LENGTH);
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:142:        return randomP
ojoList(clazz, size, consumers);
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:143:    }
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:144:
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:145:    @SafeVarargs
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:146:    public static <T> 
List<T> randomPojoList(Class<T> clazz, int size, Consumer<T>... consumers) {
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:147:        return Stream.
iterate(0, i -> i).limit(size).map(o -> randomPojo(clazz, consumers))
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:148:                .colle
ct(Collectors.toList());
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:149:    }
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:150:
services/zhongshu-core/zszj-framework/zszj-spring-boot-starter-test/src/main/ja
va/cn/zszj/framework/test/core/util/RandomUtils.java:151:}

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "rg -
n -A 14 -B 4 'grantRefreshToken|revokeToken|getAccessToken\\(' services/zhongsh
u-core/zszj-module-system/src/main/java/cn/zszj/module/system/service/oauth2/OA
uth2GrantServiceImpl.java; rg -n -A 15 -B 3 'toBean\\(Object source, Class<T>|c
opyProperties' services/zhongshu-core/zszj-framework/zszj-common/src/main/java/
cn/zszj/framework/common/util/object/BeanUtils.java; rg -n -A 25 -B 3 'getStrin
gValue|RANDOM_STRING_LENGTH|setDefaultStrategy' services/zhongshu-core/zszj-fra
mework/zszj-spring-boot-starter-test/src/main/java/cn/zszj/framework/test/core/
util/RandomUtils.java; rg -n -A 22 -B 4 'checkAccessToken\\(token\\)' services/
zhongshu-core/zszj-framework/zszj-spring-boot-starter-security/src/main/java/cn
/zszj/framework/security/core/filter/TokenAuthenticationFilter.java; git status
 --short" in E:\众墅之家AI赋能平台底座
 succeeded in 29ms:
?? ".omx/plans/2026-09-11-B03-Wave1-\345\205\255\351\241\271\345\271\266\350\24
1\214\345\274\200\345\217\221\350\256\241\345\210\222.md"
?? docs/reviews/codex-ZS-LOGIN-001.raw.md
?? outputs/

codex
No actionable regressions were found. The change rejects and evicts cached synt
hetic tokens when compatibility is disabled, while preserving normal access-tok
en and compatibility-enabled behavior. Tests were inspected but not run in the 
read-only environment.
No actionable regressions were found. The change rejects and evicts cached synthetic tokens when compatibility is disabled, while preserving normal access-token and compatibility-enabled behavior. Tests were inspected but not run in the read-only environment.
