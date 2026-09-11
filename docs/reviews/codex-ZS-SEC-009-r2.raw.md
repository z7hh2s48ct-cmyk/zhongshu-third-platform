BEFORE_HEAD=19b736f4fe2bf8b022b0a4736a4c4684a687973b START=2026-09-11T13:04:13.4789116+08:00
codex.cmd : OpenAI Codex v0.154.0
所在位置 行:1 字符: 196
+ ... oding utf8; & "$env:APPDATA\npm\codex.cmd" review --commit 1f6bbb1c 2 ...
+                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    + CategoryInfo          : NotSpecified: (OpenAI Codex v0.154.0:String) [], 
    RemoteException
    + FullyQualifiedErrorId : NativeCommandError
 
--------
workdir: E:\众墅之家AI赋能平台底座
model: gpt-6-astra
provider: openai
approval: never
sandbox: read-only
reasoning effort: none
reasoning summaries: none
session id: 01a08eda-1aea-7cb1-94d4-ae06cc9e74c7
--------
user
commit 1f6bbb1
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'git 
status --short; git show --stat --oneline 1f6bbb1c; Get-ChildItem -Force -Name 
AGENTS*' in E:\众墅之家AI赋能平台底座
 succeeded in 25ms:
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "git 
show 1f6bbb1c; Get-ChildItem -Path . -Recurse -Filter AGENTS*.md -Name; git ls-
files '*Clock*' '*oauth2*' '*AGENTS*'" in E:\众墅之家AI赋能平台底座
 succeeded in 11403ms:
apps/zhongshu-admin-web/src/api/login/oauth2/index.ts
apps/zhongshu-admin-web/src/api/system/oauth2/client.ts
apps/zhongshu-admin-web/src/api/system/oauth2/token.ts
apps/zhongshu-admin-web/src/views/hrm/attendance/clock/AttendanceClockDailyDeta
il.vue
apps/zhongshu-admin-web/src/views/hrm/attendance/clock/AttendanceClockForm.vue
apps/zhongshu-admin-web/src/views/hrm/attendance/clock/AttendanceClockOverview.
vue
apps/zhongshu-admin-web/src/views/hrm/attendance/clock/AttendanceClockRecordLis
t.vue
apps/zhongshu-admin-web/src/views/system/oauth2/client/ClientForm.vue
apps/zhongshu-admin-web/src/views/system/oauth2/client/index.vue
apps/zhongshu-admin-web/src/views/system/oauth2/token/index.vue
apps/zhongshu-miniapp/src/api/system/oauth2/client/index.ts
apps/zhongshu-miniapp/src/api/system/oauth2/token/index.ts
apps/zhongshu-miniapp/src/pages-system/oauth2/client/detail/index.vue
apps/zhongshu-miniapp/src/pages-system/oauth2/client/form/index.vue
apps/zhongshu-miniapp/src/pages-system/oauth2/components/client-list.vue
apps/zhongshu-miniapp/src/pages-system/oauth2/components/client-search-form.vue
apps/zhongshu-miniapp/src/pages-system/oauth2/components/token-list.vue
apps/zhongshu-miniapp/src/pages-system/oauth2/components/token-search-form.vue
apps/zhongshu-miniapp/src/pages-system/oauth2/index.vue
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/biz/system/oauth2/OAuth2TokenCommonApi.java
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/biz/system/oauth2/dto/OAuth2AccessTokenCheckRespDTO.java
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/biz/system/oauth2/dto/OAuth2AccessTokenCreateReqDTO.java
services/zhongshu-core/zszj-framework/zszj-common/src/main/java/cn/zszj/framewo
rk/common/biz/system/oauth2/dto/OAuth2AccessTokenRespDTO.java
services/zhongshu-core/zszj-module-hrm/src/main/java/cn/zszj/module/hrm/control
ler/admin/attendance/HrmAttendanceClockController.java
services/zhongshu-core/zszj-module-hrm/src/main/java/cn/zszj/module/hrm/control
ler/admin/attendance/vo/clock/HrmAttendanceClockPageReqVO.java
services/zhongshu-core/zszj-module-hrm/src/main/java/cn/zszj/module/hrm/control
ler/admin/attendance/vo/clock/HrmAttendanceClockRespVO.java
services/zhongshu-core/zszj-module-hrm/src/main/java/cn/zszj/module/hrm/control
ler/admin/attendance/vo/clock/HrmAttendanceClockSaveReqVO.java
services/zhongshu-core/zszj-module-hrm/src/main/java/cn/zszj/module/hrm/control
ler/admin/attendance/vo/clock/HrmAttendanceClockShiftReqVO.java
services/zhongshu-core/zszj-module-hrm/src/main/java/cn/zszj/module/hrm/control
ler/admin/attendance/vo/clock/HrmAttendanceClockShiftRespVO.java
services/zhongshu-core/zszj-module-hrm/src/main/java/cn/zszj/module/hrm/control
ler/admin/portal/attendance/HrmPortalAttendanceClockController.java
services/zhongshu-core/zszj-module-hrm/src/main/java/cn/zszj/module/hrm/control
ler/admin/portal/attendance/vo/clock/HrmPortalAttendanceClockCreateReqVO.java
services/zhongshu-core/zszj-module-hrm/src/main/java/cn/zszj/module/hrm/control
ler/admin/portal/attendance/vo/clock/HrmPortalAttendanceClockDetailRespVO.java
services/zhongshu-core/zszj-module-hrm/src/main/java/cn/zszj/module/hrm/dal/dat
aobject/attendance/record/HrmAttendanceClockDO.java
services/zhongshu-core/zszj-module-hrm/src/main/java/cn/zszj/module/hrm/dal/mys
ql/attendance/record/HrmAttendanceClockMapper.java
services/zhongshu-core/zszj-module-hrm/src/main/java/cn/zszj/module/hrm/enums/a
ttendance/record/HrmAttendanceClockButtonStatusEnum.java
services/zhongshu-core/zszj-module-hrm/src/main/java/cn/zszj/module/hrm/enums/a
ttendance/record/HrmAttendanceClockSourceEnum.java
services/zhongshu-core/zszj-module-hrm/src/main/java/cn/zszj/module/hrm/enums/a
ttendance/record/HrmAttendanceClockStageEnum.java
services/zhongshu-core/zszj-module-hrm/src/main/java/cn/zszj/module/hrm/enums/a
ttendance/record/HrmAttendanceClockStatusEnum.java
services/zhongshu-core/zszj-module-hrm/src/main/java/cn/zszj/module/hrm/enums/a
ttendance/record/HrmAttendanceClockTypeEnum.java
services/zhongshu-core/zszj-module-hrm/src/main/java/cn/zszj/module/hrm/service
/attendance/record/HrmAttendanceClockService.java
services/zhongshu-core/zszj-module-hrm/src/main/java/cn/zszj/module/hrm/service
/attendance/record/HrmAttendanceClockServiceImpl.java
services/zhongshu-core/zszj-module-hrm/src/test/java/cn/zszj/module/hrm/service
/attendance/record/HrmAttendanceClockServiceImplTest.java
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/a
pi/oauth2/OAuth2TokenApiImpl.java
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/c
ontroller/admin/oauth2/OAuth2ClientController.http
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/c
ontroller/admin/oauth2/OAuth2ClientController.java
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/c
ontroller/admin/oauth2/OAuth2OpenController.http
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/c
ontroller/admin/oauth2/OAuth2OpenController.java
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/c
ontroller/admin/oauth2/OAuth2TokenController.java
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/c
ontroller/admin/oauth2/OAuth2UserController.http
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/c
ontroller/admin/oauth2/OAuth2UserController.java
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/c
ontroller/admin/oauth2/vo/client/OAuth2ClientPageReqVO.java
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/c
ontroller/admin/oauth2/vo/client/OAuth2ClientRespVO.java
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/c
ontroller/admin/oauth2/vo/client/OAuth2ClientSaveReqVO.java
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/c
ontroller/admin/oauth2/vo/open/OAuth2OpenAccessTokenRespVO.java
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/c
ontroller/admin/oauth2/vo/open/OAuth2OpenAuthorizeInfoRespVO.java
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/c
ontroller/admin/oauth2/vo/open/OAuth2OpenCheckTokenRespVO.java
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/c
ontroller/admin/oauth2/vo/token/OAuth2AccessTokenPageReqVO.java
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/c
ontroller/admin/oauth2/vo/token/OAuth2AccessTokenRespVO.java
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/c
ontroller/admin/oauth2/vo/user/OAuth2UserInfoRespVO.java
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/c
ontroller/admin/oauth2/vo/user/OAuth2UserUpdateReqVO.java
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/c
onvert/oauth2/OAuth2OpenConvert.java
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/d
al/dataobject/oauth2/OAuth2AccessTokenDO.java
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/d
al/dataobject/oauth2/OAuth2ApproveDO.java
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/d
al/dataobject/oauth2/OAuth2ClientDO.java
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/d
al/dataobject/oauth2/OAuth2CodeDO.java
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/d
al/dataobject/oauth2/OAuth2RefreshTokenDO.java
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/d
al/mysql/oauth2/OAuth2AccessTokenMapper.java
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/d
al/mysql/oauth2/OAuth2ApproveMapper.java
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/d
al/mysql/oauth2/OAuth2ClientMapper.java
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/d
al/mysql/oauth2/OAuth2CodeMapper.java
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/d
al/mysql/oauth2/OAuth2RefreshTokenMapper.java
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/d
al/redis/oauth2/OAuth2AccessTokenRedisDAO.java
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/e
nums/oauth2/OAuth2ClientConstants.java
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/e
nums/oauth2/OAuth2GrantTypeEnum.java
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/s
ervice/oauth2/OAuth2ApproveService.java
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/s
ervice/oauth2/OAuth2ApproveServiceImpl.java
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/s
ervice/oauth2/OAuth2ClientService.java
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/s
ervice/oauth2/OAuth2ClientServiceImpl.java
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/s
ervice/oauth2/OAuth2CodeService.java
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/s
ervice/oauth2/OAuth2CodeServiceImpl.java
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/s
ervice/oauth2/OAuth2GrantService.java
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/s
ervice/oauth2/OAuth2GrantServiceImpl.java
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/s
ervice/oauth2/OAuth2TokenService.java
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/s
ervice/oauth2/OAuth2TokenServiceImpl.java
services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/u
til/oauth2/OAuth2Utils.java
services/zhongshu-core/zszj-module-system/src/test/java/cn/zszj/module/system/c
ontroller/admin/oauth2/OAuth2OpenControllerTest.java
services/zhongshu-core/zszj-module-system/src/test/java/cn/zszj/module/system/s
ervice/oauth2/OAuth2ApproveServiceImplTest.java
services/zhongshu-core/zszj-module-system/src/test/java/cn/zszj/module/system/s
ervice/oauth2/OAuth2ClientServiceImplTest.java
services/zhongshu-core/zszj-module-system/src/test/java/cn/zszj/module/system/s
ervice/oauth2/OAuth2CodeServiceImplTest.java
services/zhongshu-core/zszj-module-system/src/test/java/cn/zszj/module/system/s
ervice/oauth2/OAuth2GrantServiceImplTest.java
services/zhongshu-core/zszj-module-system/src/test/java/cn/zszj/module/system/s
ervice/oauth2/OAuth2TokenServiceImplTest.java

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command '$pat
hs=@('"'.', 'services','services/zhongshu-core','services/zhongshu-core/zszj-mo
dule-system','services/zhongshu-core/zszj-module-system/src','services/zhongshu
-core/zszj-module-system/src/main','services/zhongshu-core/zszj-module-system/s
rc/main/java','services/zhongshu-core/zszj-module-system/src/main/java/cn','ser
vices/zhongshu-core/zszj-module-system/src/main/java/cn/zszj','services/zhongsh
u-core/zszj-module-system/src/main/java/cn/zszj/module','services/zhongshu-core
/zszj-module-system/src/main/java/cn/zszj/module/system','services/zhongshu-cor
e/zszj-module-system/src/main/java/cn/zszj/module/system/service','services/zho
ngshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/service/oauth
2'); foreach("'$p in $paths){ Get-ChildItem -LiteralPath $p -Filter '"'AGENTS*.
md' | ForEach-Object { "'$_.FullName; Get-Content -LiteralPath $_.FullName } };
 git ls-files '"'*DateUtils.java' '*OAuth2*Test*'" in E:\众墅之家AI赋能平台底座
 succeeded in 6ms:
exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command 'git 
show 1f6bbb1c:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/m
odule/system/service/oauth2/OAuth2ApproveServiceImpl.java; git show 1f6bbb1c:se
rvices/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module/system/ser
vice/oauth2/OAuth2CodeServiceImpl.java; git show 1f6bbb1c:services/zhongshu-cor
e/zszj-framework/zszj-common/src/main/java/cn/zszj/framework/common/util/date/D
ateUtils.java' in E:\众墅之家AI赋能平台底座
 succeeded in 0ms:
package cn.zszj.module.system.service.oauth2;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.zszj.framework.common.util.date.DateUtils;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2ApproveDO;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2ClientDO;
import cn.zszj.module.system.dal.mysql.oauth2.OAuth2ApproveMapper;
import com.google.common.annotations.VisibleForTesting;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;

import static cn.zszj.framework.common.util.collection.CollectionUtils.convertS
et;

/**
 * OAuth2 批准 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class OAuth2ApproveServiceImpl implements OAuth2ApproveService {

    /**
     * 批准的过期时间，默认 30 天
     */
    private static final Integer TIMEOUT = 30 * 24 * 60 * 60; // 单位：秒

    @Resource
    private OAuth2ClientService oauth2ClientService;

    @Resource
    private OAuth2ApproveMapper oauth2ApproveMapper;

    @Override
    @Transactional
    public boolean checkForPreApproval(Long userId, Integer userType, String cl
ientId, Collection<String> requestedScopes) {
        // 第一步，基于 Client 的自动授权计算，如果 scopes 都在自动授权中，则返回 true 通过
        OAuth2ClientDO clientDO = oauth2ClientService.validOAuthClientFromCache
(clientId);
        Assert.notNull(clientDO, "客户端不能为空"); // 防御性编程
        if (CollUtil.containsAll(clientDO.getAutoApproveScopes(), requestedScop
es)) {
            // gh-877 - if all scopes are auto approved, approvals still need t
o be added to the approval store.
            LocalDateTime expireTime = DateUtils.now().plusSeconds(TIMEOUT);
            for (String scope : requestedScopes) {
                saveApprove(userId, userType, clientId, scope, true, expireTime
);
            }
            return true;
        }

        // 第二步，算上用户已经批准的授权。如果 scopes 都包含，则返回 true
        List<OAuth2ApproveDO> approveDOs = getApproveList(userId, userType, cli
entId);
        Set<String> scopes = convertSet(approveDOs, OAuth2ApproveDO::getScope,
                OAuth2ApproveDO::getApproved); // 只保留未过期的 + 同意的
        return CollUtil.containsAll(scopes, requestedScopes);
    }

    @Override
    @Transactional
    public boolean updateAfterApproval(Long userId, Integer userType, String cl
ientId, Map<String, Boolean> requestedScopes) {
        // 如果 requestedScopes 为空，说明没有要求，则返回 true 通过
        if (CollUtil.isEmpty(requestedScopes)) {
            return true;
        }

        // 更新批准的信息
        boolean success = false; // 需要至少有一个同意
        LocalDateTime expireTime = DateUtils.now().plusSeconds(TIMEOUT);
        for (Map.Entry<String, Boolean> entry : requestedScopes.entrySet()) {
            if (entry.getValue()) {
                success = true;
            }
            saveApprove(userId, userType, clientId, entry.getKey(), entry.getVa
lue(), expireTime);
        }
        return success;
    }

    @Override
    public List<OAuth2ApproveDO> getApproveList(Long userId, Integer userType, 
String clientId) {
        List<OAuth2ApproveDO> approveDOs = oauth2ApproveMapper.selectListByUser
IdAndUserTypeAndClientId(
                userId, userType, clientId);
        approveDOs.removeIf(o -> DateUtils.isExpired(o.getExpiresTime()));
        return approveDOs;
    }

    @VisibleForTesting
    void saveApprove(Long userId, Integer userType, String clientId,
                     String scope, Boolean approved, LocalDateTime expireTime) 
{
        // 先更新
        OAuth2ApproveDO approveDO = new OAuth2ApproveDO().setUserId(userId).set
UserType(userType)
                .setClientId(clientId).setScope(scope).setApproved(approved).se
tExpiresTime(expireTime);
        if (oauth2ApproveMapper.update(approveDO) == 1) {
            return;
        }
        // 失败，则说明不存在，进行更新
        oauth2ApproveMapper.insert(approveDO);
    }

}
package cn.zszj.module.system.service.oauth2;

import cn.hutool.core.util.IdUtil;
import cn.zszj.framework.common.util.date.DateUtils;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2CodeDO;
import cn.zszj.module.system.dal.mysql.oauth2.OAuth2CodeMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;
import java.util.List;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exce
ption;
import static cn.zszj.module.system.enums.ErrorCodeConstants.OAUTH2_CODE_EXPIRE
;
import static cn.zszj.module.system.enums.ErrorCodeConstants.OAUTH2_CODE_NOT_EX
ISTS;

/**
 * OAuth2.0 授权码 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class OAuth2CodeServiceImpl implements OAuth2CodeService {

    /**
     * 授权码的过期时间，默认 5 分钟
     */
    private static final Integer TIMEOUT = 5 * 60;

    @Resource
    private OAuth2CodeMapper oauth2CodeMapper;

    @Override
    public OAuth2CodeDO createAuthorizationCode(Long userId, Integer userType, 
String clientId,
                                                List<String> scopes, String red
irectUri, String state) {
        OAuth2CodeDO codeDO = new OAuth2CodeDO().setCode(generateCode())
                .setUserId(userId).setUserType(userType)
                .setClientId(clientId).setScopes(scopes)
                .setExpiresTime(DateUtils.now().plusSeconds(TIMEOUT))
                .setRedirectUri(redirectUri).setState(state);
        oauth2CodeMapper.insert(codeDO);
        return codeDO;
    }

    @Override
    public OAuth2CodeDO consumeAuthorizationCode(String code) {
        OAuth2CodeDO codeDO = oauth2CodeMapper.selectByCode(code);
        if (codeDO == null) {
            throw exception(OAUTH2_CODE_NOT_EXISTS);
        }
        if (DateUtils.isExpired(codeDO.getExpiresTime())) {
            throw exception(OAUTH2_CODE_EXPIRE);
        }
        oauth2CodeMapper.deleteById(codeDO.getId());
        return codeDO;
    }

    private static String generateCode() {
        return IdUtil.fastSimpleUUID();
    }

}
package cn.zszj.framework.common.util.date;

import cn.hutool.core.date.LocalDateTimeUtil;

import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * 时间工具类
 *
 * @author 芋道源码
 */
public class DateUtils {

    /**
     * 时区 - 默认
     */
    public static final String TIME_ZONE_DEFAULT = "GMT+8";

    /**
     * 时区 - 默认 ZoneId
     *
     * ZS-SEC-009：接口边界时间序列化/反序列化的单一事实源，由 {@link #TIME_ZONE_DEFAULT} 派生
     * （GMT+8 固定偏移，等价 Asia/Shanghai，中国无夏令时），替代易受部署 JVM 影响的
     * {@link ZoneId#systemDefault()}，保证同一 LocalDateTime 在任意部署时区下得到一致的 epoch mi
llis。
     * 与 {@code LocalDateTimeUtils#DEFAULT_ZONE_ID} 采用同一派生方式，全项目时区约定唯一。
     */
    public static final ZoneId ZONE_DEFAULT = TimeZone.getTimeZone(TIME_ZONE_DE
FAULT).toZoneId();

    /**
     * 秒转换成毫秒
     */
    public static final long SECOND_MILLIS = 1000;

    public static final String FORMAT_YEAR_MONTH_DAY = "yyyy-MM-dd";

    public static final String FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND = "yyyy
-MM-dd HH:mm:ss";

    /**
     * 当前时间（固定 {@link #ZONE_DEFAULT} GMT+8）
     *
     * ZS-SEC-009：接口边界时间合同的「生产端」原语。替代 {@link LocalDateTime#now()}（依赖部署 JVM 默认时区
），
     * 使被序列化/比较的 LocalDateTime（如令牌过期时间）与固定时区的 TimestampLocalDateTimeSerializer 
对齐，
     * 消除 UTC 部署下 creation(systemDefault) 与 serialization(GMT+8) 失配导致的时间偏移。
     */
    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE_DEFAULT);
    }

    /**
     * 将 LocalDateTime 转换成 Date
     *
     * @param date LocalDateTime
     * @return LocalDateTime
     */
    public static Date of(LocalDateTime date) {
        if (date == null) {
            return null;
        }
        // 将此日期时间与时区相结合以创建 ZonedDateTime
        ZonedDateTime zonedDateTime = date.atZone(ZoneId.systemDefault());
        // 本地时间线 LocalDateTime 到即时时间线 Instant 时间戳
        Instant instant = zonedDateTime.toInstant();
        // UTC时间(世界协调时间,UTC + 00:00)转北京(北京,UTC + 8:00)时间
        return Date.from(instant);
    }

    /**
     * 将 Date 转换成 LocalDateTime
     *
     * @param date Date
     * @return LocalDateTime
     */
    public static LocalDateTime of(Date date) {
        if (date == null) {
            return null;
        }
        // 转为时间戳
        Instant instant = date.toInstant();
        // UTC时间(世界协调时间,UTC + 00:00)转北京(北京,UTC + 8:00)时间
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    public static Date addTime(Duration duration) {
        return new Date(System.currentTimeMillis() + duration.toMillis());
    }

    public static boolean isExpired(LocalDateTime time) {
        // ZS-SEC-009：用固定时区的 now() 比较，与令牌 expiresTime 的生产端（DateUtils.now()）对齐
        LocalDateTime now = now();
        return now.isAfter(time);
    }

    /**
     * 创建指定时间
     *
     * @param year  年
     * @param month 月
     * @param day   日
     * @return 指定时间
     */
    public static Date buildTime(int year, int month, int day) {
        return buildTime(year, month, day, 0, 0, 0);
    }

    /**
     * 创建指定时间
     *
     * @param year   年
     * @param month  月
     * @param day    日
     * @param hour   小时
     * @param minute 分钟
     * @param second 秒
     * @return 指定时间
     */
    public static Date buildTime(int year, int month, int day,
                                 int hour, int minute, int second) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);
        calendar.set(Calendar.MILLISECOND, 0); // 一般情况下，都是 0 毫秒
        return calendar.getTime();
    }

    public static Date max(Date a, Date b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.compareTo(b) > 0 ? a : b;
    }

    public static LocalDateTime max(LocalDateTime a, LocalDateTime b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.isAfter(b) ? a : b;
    }

    /**
     * 是否今天
     *
     * @param date 日期
     * @return 是否
     */
    public static boolean isToday(LocalDateTime date) {
        return LocalDateTimeUtil.isSameDay(date, LocalDateTime.now());
    }

    /**
     * 是否昨天
     *
     * @param date 日期
     * @return 是否
     */
    public static boolean isYesterday(LocalDateTime date) {
        return LocalDateTimeUtil.isSameDay(date, LocalDateTime.now().minusDays(
1));
    }

}

exec
"C:\\windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe" -Command "Get-
ChildItem E:\\ -Filter 'AGENTS*.md' -ErrorAction SilentlyContinue; git grep -n 
'getExpiresTime\\|now()' 1f6bbb1c -- services/zhongshu-core/zszj-module-system/
src/test/java/cn/zszj/module/system/service/oauth2/OAuth2CodeServiceImplTest.ja
va services/zhongshu-core/zszj-module-system/src/test/java/cn/zszj/module/syste
m/service/oauth2/OAuth2ApproveServiceImplTest.java; git grep -n 'OAuth2ApproveD
O\\|OAuth2CodeDO' 1f6bbb1c -- services/zhongshu-core/zszj-module-system/src/mai
n/java | Select-Object -Last 35" in E:\众墅之家AI赋能平台底座
 succeeded in 13ms:
1f6bbb1c:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module
/system/dal/mysql/oauth2/OAuth2ApproveMapper.java:5:import cn.zszj.module.syste
m.dal.dataobject.oauth2.OAuth2ApproveDO;
1f6bbb1c:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module
/system/dal/mysql/oauth2/OAuth2ApproveMapper.java:11:public interface OAuth2App
roveMapper extends BaseMapperX<OAuth2ApproveDO> {
1f6bbb1c:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module
/system/dal/mysql/oauth2/OAuth2ApproveMapper.java:13:    default int update(OAu
th2ApproveDO updateObj) {
1f6bbb1c:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module
/system/dal/mysql/oauth2/OAuth2ApproveMapper.java:14:        return update(upda
teObj, new LambdaQueryWrapperX<OAuth2ApproveDO>()
1f6bbb1c:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module
/system/dal/mysql/oauth2/OAuth2ApproveMapper.java:15:                .eq(OAuth2
ApproveDO::getUserId, updateObj.getUserId())
1f6bbb1c:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module
/system/dal/mysql/oauth2/OAuth2ApproveMapper.java:16:                .eq(OAuth2
ApproveDO::getUserType, updateObj.getUserType())
1f6bbb1c:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module
/system/dal/mysql/oauth2/OAuth2ApproveMapper.java:17:                .eq(OAuth2
ApproveDO::getClientId, updateObj.getClientId())
1f6bbb1c:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module
/system/dal/mysql/oauth2/OAuth2ApproveMapper.java:18:                .eq(OAuth2
ApproveDO::getScope, updateObj.getScope()));
1f6bbb1c:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module
/system/dal/mysql/oauth2/OAuth2ApproveMapper.java:21:    default List<OAuth2App
roveDO> selectListByUserIdAndUserTypeAndClientId(Long userId, Integer userType,
 String clientId) {
1f6bbb1c:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module
/system/dal/mysql/oauth2/OAuth2ApproveMapper.java:22:        return selectList(
new LambdaQueryWrapperX<OAuth2ApproveDO>()
1f6bbb1c:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module
/system/dal/mysql/oauth2/OAuth2ApproveMapper.java:23:                .eq(OAuth2
ApproveDO::getUserId, userId)
1f6bbb1c:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module
/system/dal/mysql/oauth2/OAuth2ApproveMapper.java:24:                .eq(OAuth2
ApproveDO::getUserType, userType)
1f6bbb1c:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module
/system/dal/mysql/oauth2/OAuth2ApproveMapper.java:25:                .eq(OAuth2
ApproveDO::getClientId, clientId));
1f6bbb1c:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module
/system/dal/mysql/oauth2/OAuth2CodeMapper.java:4:import cn.zszj.module.system.d
al.dataobject.oauth2.OAuth2CodeDO;
1f6bbb1c:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module
/system/dal/mysql/oauth2/OAuth2CodeMapper.java:8:public interface OAuth2CodeMap
per extends BaseMapperX<OAuth2CodeDO> {
1f6bbb1c:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module
/system/dal/mysql/oauth2/OAuth2CodeMapper.java:10:    default OAuth2CodeDO sele
ctByCode(String code) {
1f6bbb1c:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module
/system/dal/mysql/oauth2/OAuth2CodeMapper.java:11:        return selectOne(OAut
h2CodeDO::getCode, code);
1f6bbb1c:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module
/system/service/oauth2/OAuth2ApproveService.java:3:import cn.zszj.module.system
.dal.dataobject.oauth2.OAuth2ApproveDO;
1f6bbb1c:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module
/system/service/oauth2/OAuth2ApproveService.java:50:    List<OAuth2ApproveDO> g
etApproveList(Long userId, Integer userType, String clientId);
1f6bbb1c:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module
/system/service/oauth2/OAuth2ApproveServiceImpl.java:6:import cn.zszj.module.sy
stem.dal.dataobject.oauth2.OAuth2ApproveDO;
1f6bbb1c:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module
/system/service/oauth2/OAuth2ApproveServiceImpl.java:56:        List<OAuth2Appr
oveDO> approveDOs = getApproveList(userId, userType, clientId);
1f6bbb1c:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module
/system/service/oauth2/OAuth2ApproveServiceImpl.java:57:        Set<String> sco
pes = convertSet(approveDOs, OAuth2ApproveDO::getScope,
1f6bbb1c:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module
/system/service/oauth2/OAuth2ApproveServiceImpl.java:58:                OAuth2A
pproveDO::getApproved); // 只保留未过期�?+ 同意�?1f6bbb1c:services/zhongshu-core/zszj-m
odule-system/src/main/java/cn/zszj/module/system/service/oauth2/OAuth2ApproveSe
rviceImpl.java:83:    public List<OAuth2ApproveDO> getApproveList(Long userId, 
Integer userType, String clientId) {
1f6bbb1c:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module
/system/service/oauth2/OAuth2ApproveServiceImpl.java:84:        List<OAuth2Appr
oveDO> approveDOs = oauth2ApproveMapper.selectListByUserIdAndUserTypeAndClientI
d(
1f6bbb1c:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module
/system/service/oauth2/OAuth2ApproveServiceImpl.java:94:        OAuth2ApproveDO
 approveDO = new OAuth2ApproveDO().setUserId(userId).setUserType(userType)
1f6bbb1c:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module
/system/service/oauth2/OAuth2CodeService.java:3:import cn.zszj.module.system.da
l.dataobject.oauth2.OAuth2CodeDO;
1f6bbb1c:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module
/system/service/oauth2/OAuth2CodeService.java:29:    OAuth2CodeDO createAuthori
zationCode(Long userId, Integer userType, String clientId,
1f6bbb1c:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module
/system/service/oauth2/OAuth2CodeService.java:37:    OAuth2CodeDO consumeAuthor
izationCode(String code);
1f6bbb1c:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module
/system/service/oauth2/OAuth2CodeServiceImpl.java:5:import cn.zszj.module.syste
m.dal.dataobject.oauth2.OAuth2CodeDO;
1f6bbb1c:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module
/system/service/oauth2/OAuth2CodeServiceImpl.java:35:    public OAuth2CodeDO cr
eateAuthorizationCode(Long userId, Integer userType, String clientId,
1f6bbb1c:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module
/system/service/oauth2/OAuth2CodeServiceImpl.java:37:        OAuth2CodeDO codeD
O = new OAuth2CodeDO().setCode(generateCode())
1f6bbb1c:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module
/system/service/oauth2/OAuth2CodeServiceImpl.java:47:    public OAuth2CodeDO co
nsumeAuthorizationCode(String code) {
1f6bbb1c:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module
/system/service/oauth2/OAuth2CodeServiceImpl.java:48:        OAuth2CodeDO codeD
O = oauth2CodeMapper.selectByCode(code);
1f6bbb1c:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module
/system/service/oauth2/OAuth2GrantServiceImpl.java:8:import cn.zszj.module.syst
em.dal.dataobject.oauth2.OAuth2CodeDO;
1f6bbb1c:services/zhongshu-core/zszj-module-system/src/main/java/cn/zszj/module
/system/service/oauth2/OAuth2GrantServiceImpl.java:51:        OAuth2CodeDO code
DO = oauth2CodeService.consumeAuthorizationCode(code);

codex
The changes align authorization-code and approval expiration timestamps with th
e fixed GMT+8 clock already used by DateUtils.isExpired. No actionable regressi
ons were identified; tests were inspected but not executed.
The changes align authorization-code and approval expiration timestamps with the fixed GMT+8 clock already used by DateUtils.isExpired. No actionable regressions were identified; tests were inspected but not executed.
AFTER_HEAD=19b736f4fe2bf8b022b0a4736a4c4684a687973b DONE=2026-09-11T13:05:06.5423324+08:00
CODEX_DONE
