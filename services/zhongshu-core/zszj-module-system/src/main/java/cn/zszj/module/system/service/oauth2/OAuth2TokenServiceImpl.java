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
import cn.zszj.framework.common.util.monitor.TracerUtils;
import cn.zszj.framework.common.util.object.BeanUtils;
import cn.zszj.framework.security.core.LoginUser;
import cn.zszj.framework.security.core.util.SecurityFrameworkUtils;
import cn.zszj.framework.tenant.core.context.TenantContextHolder;
import cn.zszj.framework.tenant.core.util.TenantUtils;
import cn.zszj.module.system.controller.admin.oauth2.vo.token.OAuth2AccessTokenPageReqVO;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2ClientDO;
import cn.zszj.module.system.dal.dataobject.oauth2.OAuth2RefreshTokenDO;
import cn.zszj.module.system.dal.mysql.oauth2.OAuth2CodeMapper;
import cn.zszj.module.system.dal.mysql.user.AdminUserMapper;
import cn.zszj.module.system.dal.dataobject.user.AdminUserDO;
import cn.zszj.module.system.dal.mysql.oauth2.OAuth2AccessTokenMapper;
import cn.zszj.module.system.dal.mysql.oauth2.OAuth2RefreshTokenMapper;
import cn.zszj.module.system.dal.redis.oauth2.OAuth2AccessTokenRedisDAO;
import cn.zszj.module.system.framework.outbox.SystemOutboxEventTypes;
import cn.zszj.module.system.framework.outbox.TokenRevocationCompensationPayload.TokenType;
import cn.zszj.module.system.service.user.AdminUserService;
import cn.zszj.module.infra.framework.outbox.OutboxEventMessage;
import cn.zszj.module.infra.framework.outbox.ReliableEventPort;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zszj.framework.common.exception.util.ServiceExceptionUtil.exception0;
import static cn.zszj.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.zszj.module.system.enums.ErrorCodeConstants.OAUTH2_TOKEN_SESSION_NOT_OWNED;
import static cn.zszj.module.system.enums.ErrorCodeConstants.OAUTH2_TOKEN_SESSION_SELF_REQUIRES_USER;

/**
 * OAuth2.0 Token Service 实现类
 *
 * @author 芋道源码
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
    private OAuth2CodeMapper oauth2CodeMapper;
    @Resource
    private AdminUserMapper adminUserMapper;

    @Resource
    private OAuth2ClientService oauth2ClientService;
    @Resource
    @Lazy // 懒加载，避免循环依赖
    private AdminUserService adminUserService;

    /**
     * ZS-LOGIN-005.B：Outbox 事件写入端口（JOB-002 SPI）——ObjectProvider 懒解析：
     * <ul>
     *   <li>system + infra 合并部署（生产）：解析到 {@code JdbcReliableEventPort}，撤销主逻辑事务内预写补偿事件；</li>
     *   <li>system 单独运行（部分测试 / 精简部署）：解析为 {@code null}，降级为 {@code .A} 语义
     *       （无补偿链路，仅事务后失效 + 权威校验）。</li>
     * </ul>
     * 选择 ObjectProvider 而非直接 @Resource：避免 system 单独运行时因 infra bean 缺失导致上下文启动失败。
     */
    @Resource
    private ObjectProvider<ReliableEventPort> reliableEventPortProvider;

    /**
     * ZS-LOGIN-001：令牌用途分离门控开关——是否允许把「刷新令牌」静默当作「访问令牌」使用。
     * <p>代码默认 {@code false}（安全默认：{@link #getAccessToken} 只认访问令牌；刷新令牌当访问令牌用 → 返回 null → checkAccessToken 抛 UNAUTHORIZED）。
     * <p><b>迁移状态（ZS-LOGIN-001.B 已交付）：</b>admin-web/miniapp 的 WebSocket 握手已迁移为一次性短时票据
     * （{@code POST /system/auth/ws-ticket} 换票 → 握手带 {@code ?ticket=}），生产 application.yaml 的
     * {@code refresh-token-as-access-token-enabled: true} 键已删除，本开关保持代码默认 {@code false}；
     * 兼容分支与自愈 evict 逻辑保留（应对历史 gate=true 期间的存量合成令牌缓存条目）。
     * 注：积木报表走 {@code X-Access-Token} 真访问令牌，<b>不</b>依赖本回退。
     * <p><b>开启风险（如未来误开）：</b>转换出的"访问令牌"继承刷新令牌 TTL（default client 达 30 天）、不落 system_oauth2_access_token 表
     * （管理端令牌分页/踢出不可见）、{@link #removeAccessToken(String)} 无法撤销，仅随 Redis TTL 自然过期。
     */
    @Value("${zszj.security.refresh-token-as-access-token-enabled:false}")
    private boolean refreshTokenAsAccessTokenEnabled;

    @PostConstruct
    public void warnRefreshTokenAsAccessTokenCompat() {
        if (refreshTokenAsAccessTokenEnabled) {
            log.warn("[warnRefreshTokenAsAccessTokenCompat][ZS-LOGIN-001 令牌用途分离兼容开关已开启"
                    + "（zszj.security.refresh-token-as-access-token-enabled=true）：刷新令牌可被当作访问令牌使用，"
                    + "将产生长效（继承刷新令牌 TTL）、不落库、removeAccessToken 无法撤销、仅随 Redis TTL 过期的凭据。"
                    + "此为 admin-web IM / miniapp IM·客服 WebSocket 迁移期临时兼容，"
                    + "须在 LOGIN-001.B 短时票据迁移完成后关闭]");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OAuth2AccessTokenDO createAccessToken(Long userId, Integer userType, String clientId, List<String> scopes) {
        OAuth2ClientDO clientDO = oauth2ClientService.validOAuthClientFromCache(clientId);
        // 创建刷新令牌
        OAuth2RefreshTokenDO refreshTokenDO = createOAuth2RefreshToken(userId, userType, clientDO, scopes);
        // 创建访问令牌
        return createOAuth2AccessToken(refreshTokenDO, clientDO);
    }

    /**
     * ZS-LOGIN-002：刷新访问令牌。
     *
     * <p><b>并发合同（本底座决策，见开发计划 §3.3）</b>：沿用原 refresh token（<b>不轮换</b>，以不破坏 ZS-LOGIN-001 门控
     * 与前端刷新契约）+ 会话代际号（session generation）+ 重放检测。任一时刻，同一刷新凭据下<b>恰好 1 个</b>有效访问令牌。
     *
     * <p><b>修复前的失控面</b>：原实现首句为无锁的 {@code selectByRefreshToken}——
     * <ol>
     *     <li>N 个并发刷新读到同一条刷新令牌，各自插入访问令牌 → 最终 N 个同时有效的失控会话；</li>
     *     <li>与 {@link #removeAccessToken(String)} 并发时，晚到的刷新已持有旧的 refreshTokenDO 快照，
     *         退出提交后仍插入新访问令牌 → 「复活」已退出的会话（孤儿令牌）。</li>
     * </ol>
     *
     * <p><b>修复手法</b>：首句改为 {@link OAuth2RefreshTokenMapper#selectByRefreshTokenForUpdate(String)}
     * （{@code SELECT ... FOR UPDATE} 行锁）。行锁在<b>事务提交/回滚时</b>才释放，因此本方法的
     * 「读刷新令牌 → 校验 Client → 删旧访问令牌 → 插新访问令牌 → 登记代际」整体成为同一会话上的临界区，
     * 并发刷新被串行化：后一个请求获锁后重读，必然看到前一个已提交的新访问令牌并把它删除，
     * 于是「有效访问令牌恒为 1」「会话代际号 == 成功刷新次数」（无丢失更新）。
     *
     * <p><b>重放（旧代际）拒绝</b>：旧代际访问令牌在刷新时被同时从 DB（逻辑删除）与 Redis 撤销，
     * 重放旧令牌 → {@link #getAccessToken(String)} 返回 null → {@link #checkAccessToken(String)} 抛既有
     * 401「访问令牌不存在」。此处<b>刻意不新增业务错误码</b>：前端与网关的「401 → 跳登录」契约不可破。
     * 代际标识（Redis）仅用于审计定位「某令牌属于第几代会话」，旧代际标识保留至自然过期。
     *
     * <p><b>锁序</b>：与 {@link #removeAccessToken(String)} 一致——先锁刷新令牌行，再处理访问令牌。
     * 退出侧在获锁后会<b>重读</b>该刷新令牌下的访问令牌，把并发刷新已提交的新代际一并撤销，
     * 因此「晚到刷新」不可能在退出之后留下存活令牌。
     *
     * <p><b>不加 DB 列</b>：代际/重放状态只存 Redis（{@link OAuth2AccessTokenRedisDAO}），避免 Flyway 迁移
     * 与 ZS-DB-019.B 的 PG 回归耦合；代际登记为 best-effort，Redis 抖动不阻断合法刷新
     * （令牌 DB/缓存一致性恢复归 ZS-LOGIN-005.A）。
     */
    @Override
    @Transactional(noRollbackFor = ServiceException.class)
    public OAuth2AccessTokenDO refreshAccessToken(String refreshToken, String clientId) {
        // 查询刷新令牌（ZS-LOGIN-002：行锁，串行化同一会话的刷新与退出）
        OAuth2RefreshTokenDO refreshTokenDO = oauth2RefreshTokenMapper.selectByRefreshTokenForUpdate(refreshToken);
        if (refreshTokenDO == null) {
            // 读不到刷新令牌：可能从未存在，也可能已被并发/先行的退出删除。
            // 因为处在同一锁序内，晚到的刷新在此必然读不到 → 不会复活会话。
            throw exception0(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), "无效的刷新令牌");
        }

        // 校验 Client 匹配
        OAuth2ClientDO clientDO = oauth2ClientService.validOAuthClientFromCache(clientId);
        if (ObjectUtil.notEqual(clientId, refreshTokenDO.getClientId())) {
            throw exception0(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), "刷新令牌的客户端编号不正确");
        }

        // 移除相关的访问令牌（在行锁内重读，可撤销并发刷新已提交的旧代际令牌 → 重放旧代际按既有 401 失效）
        // ZS-LOGIN-003 codex r2 P1：逐个落撤销墓碑——并发鉴权可能持有被淘汰旧令牌的 DB 快照，
        // 若只删缓存不落墓碑，「读 A 快照 → 刷新删 A 建 B → 用户撤销 B/R → A 快照回填」交错会复活 A
        List<OAuth2AccessTokenDO> accessTokenDOs = oauth2AccessTokenMapper.selectListByRefreshToken(refreshToken);
        if (CollUtil.isNotEmpty(accessTokenDOs)) {
            oauth2AccessTokenMapper.deleteByIds(convertSet(accessTokenDOs, OAuth2AccessTokenDO::getId));
            // ZS-LOGIN-005.A：旧代际缓存失效注册到事务提交后执行（回滚亦补偿）
            List<Runnable> evictions = new ArrayList<>();
            for (OAuth2AccessTokenDO evicted : accessTokenDOs) {
                evictions.add(() -> revokeWithTombstone(evicted.getAccessToken(), evicted.getExpiresTime()));
                // ZS-LOGIN-005.B：事务内预写补偿事件——业务回滚则事件一并回滚（Outbox 语义）
                appendRevocationCompensationEvent(evicted.getId(), TokenType.ACCESS, evicted.getExpiresTime(),
                        evicted.getTenantId(), evicted.getUserId());
            }
            invalidateCacheAfterCommit("刷新淘汰旧代际(refreshAccessToken)", evictions);
        }

        // 已过期的情况下，删除刷新令牌
        if (DateUtils.isExpired(refreshTokenDO.getExpiresTime())) {
            oauth2RefreshTokenMapper.deleteById(refreshTokenDO.getId());
            // ZS-LOGIN-002：会话随刷新令牌一并终结，清理代际键（best-effort，不影响既有异常语义）
            deleteSessionGenerationQuietly(refreshToken);
            throw exception0(GlobalErrorCodeConstants.UNAUTHORIZED.getCode(), "刷新令牌已过期");
        }

        // 创建访问令牌
        OAuth2AccessTokenDO accessTokenDO = createOAuth2AccessToken(refreshTokenDO, clientDO);
        // ZS-LOGIN-002：推进会话代际并登记访问令牌所属代际（可定位标识，best-effort）。
        // 刻意放在 createOAuth2AccessToken 之后：DB 插入失败时不会虚增代际号。
        recordSessionGeneration(refreshTokenDO, accessTokenDO);
        return accessTokenDO;
    }

    @Override
    public OAuth2AccessTokenDO getAccessToken(String accessToken) {
        // 优先从 Redis 中获取
        OAuth2AccessTokenDO accessTokenDO = oauth2AccessTokenRedisDAO.get(accessToken);
        if (accessTokenDO != null) {
            // ZS-LOGIN-001（codex P1 修复）：门控关闭时，拒绝此前兼容期（gate=true）由 convertToAccessToken
            // 缓存进 Redis 的「合成访问令牌」（accessToken==refreshToken），并自愈清除该污染条目——否则它会在此
            // Redis 命中处提前 return、绕过下方门控，在 gate 关闭后仍被 checkAccessToken 放行至刷新令牌 TTL（default client 达 30 天）。
            if (!refreshTokenAsAccessTokenEnabled && isSyntheticAccessToken(accessTokenDO)) {
                oauth2AccessTokenRedisDAO.delete(accessToken);
                return null;
            }
            return accessTokenDO;
        }

        // 获取不到，从 MySQL 中获取访问令牌
        accessTokenDO = oauth2AccessTokenMapper.selectByAccessToken(accessToken);
        // ZS-LOGIN-001：令牌用途分离，默认门控关闭时不再静默把刷新令牌当访问令牌
        if (accessTokenDO == null && refreshTokenAsAccessTokenEnabled) {
            // 特殊：从 MySQL 中获取刷新令牌。原因：解决部分场景不方便刷新访问令牌场景
            // 例如说，积木报表只允许传递 token，不允许传递 refresh_token，导致无法刷新访问令牌
            // 再例如说，前端 WebSocket 的 token 直接跟在 url 上，无法传递 refresh_token
            OAuth2RefreshTokenDO refreshTokenDO = oauth2RefreshTokenMapper.selectByRefreshToken(accessToken);
            if (refreshTokenDO != null && !DateUtils.isExpired(refreshTokenDO.getExpiresTime())) {
                accessTokenDO = convertToAccessToken(refreshTokenDO);
            }
        }

        // 如果在 MySQL 存在，则往 Redis 中写入
        if (accessTokenDO != null && !DateUtils.isExpired(accessTokenDO.getExpiresTime())) {
            oauth2AccessTokenRedisDAO.set(accessTokenDO);
        }
        return accessTokenDO;
    }

    @Override
    public OAuth2AccessTokenDO checkAccessToken(String accessToken) {
        OAuth2AccessTokenDO accessTokenDO = getAccessToken(accessToken);
        if (accessTokenDO == null) {
            throw exception0(GlobalErrorCodeConstants.UNAUTHORIZED.getCode(), "访问令牌不存在");
        }
        if (DateUtils.isExpired(accessTokenDO.getExpiresTime())) {
            throw exception0(GlobalErrorCodeConstants.UNAUTHORIZED.getCode(), "访问令牌已过期");
        }
        // ZS-LOGIN-005.A：DB 权威校验——Redis 降级为纯加速，撤销的权威状态在 DB（行逻辑删除）。
        // 缓存可能残留已撤销凭据（撤销路径缓存失效失败 / 回填竞态），命中后必须回源核验：
        // 普通令牌查 access 表；gate 合成令牌（LOGIN-001 兼容路径）查 refresh 表。
        // 核验使用 flushCache=TRUE 的专用 count 语句（codex r0 P1）：MyBatis SESSION 一级缓存
        // 会在长事务内复用 SqlSession 返回撤销前的旧快照，普通 select 不可作权威依据。
        // 查无即已撤销 → 自愈（落墓碑防即时回填复活 + evict 缓存）并按 401 拒绝（幽灵凭据根本闭环，
        // 同时是 Redis 失效失败的自愈修复路径）；DB 查询异常失败关闭（安全默认：宁可拒绝也不放行幽灵）并告警。
        try {
            int count = isSyntheticAccessToken(accessTokenDO)
                    ? oauth2RefreshTokenMapper.selectAuthorityCountByRefreshToken(accessToken)
                    : oauth2AccessTokenMapper.selectAuthorityCountByAccessToken(accessToken);
            if (count <= 0) {
                // 自愈复用 revokeWithTombstone（墓碑/删除各自容错，codex r1 P2：避免墓碑失败中断删除、
                // Redis 异常被误记为 DB 核验异常）；随后仍按 401 拒绝
                revokeWithTombstone(accessToken, accessTokenDO.getExpiresTime());
                throw exception0(GlobalErrorCodeConstants.UNAUTHORIZED.getCode(), "访问令牌不存在");
            }
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("[checkAccessToken][ZS-LOGIN-005.A DB 权威校验异常，失败关闭拒绝鉴权 token({})]", maskToken(accessToken), ex);
            throw exception0(GlobalErrorCodeConstants.UNAUTHORIZED.getCode(), "访问令牌不存在");
        }
        return accessTokenDO;
    }

    /**
     * ZS-LOGIN-002：退出（撤销访问令牌 + 刷新令牌 + 会话代际）。
     *
     * <p><b>与 {@link #refreshAccessToken(String, String)} 采用同一锁序</b>：先对刷新令牌行加行锁，再处理访问令牌；
     * 获锁后<b>重读</b>该刷新令牌下的访问令牌，把「并发刷新在本事务获锁前已提交的新代际令牌」一并撤销。
     * 这样退出与晚到刷新的两种交错都不会复活会话：
     * <ul>
     *     <li>刷新先获锁：退出在获锁后重读到刷新插入的新令牌并删除，且删除刷新令牌 → 最终无有效凭据；</li>
     *     <li>退出先获锁：刷新获锁后读不到刷新令牌 → 抛 400「无效的刷新令牌」，不插入任何令牌。</li>
     * </ul>
     *
     * <p><b>已知边界（记录为遗留，不在本任务范围）</b>：若调用方持有的是「已被前一次刷新取代的旧访问令牌串」，
     * 首句 {@code selectByAccessToken} 读不到（DB 逻辑删除 + Redis 已撤销）→ 按既有契约早退返回 {@code null}，
     * 不会撤销当前代际的会话。此为 ZS-LOGIN-001 之前就存在的既有语义（见 OAuth2TokenServiceImplTest#testRemoveAccessToken_null），
     * 且现实退出路径（请求头携带的即当前访问令牌）不受影响；如需「凭旧令牌串也能杀会话」，
     * 需要额外的 accessToken → refreshToken 反查（含软删行），归 ZS-LOGIN-005.A / codex 评审决策。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OAuth2AccessTokenDO removeAccessToken(String accessToken) {
        // 删除访问令牌
        OAuth2AccessTokenDO accessTokenDO = oauth2AccessTokenMapper.selectByAccessToken(accessToken);
        if (accessTokenDO == null) {
            return null;
        }
        // ZS-LOGIN-006 codex r0 P2：撤销主体逻辑抽取为 revokeSession，与 removeAccessTokenById 共享；
        // 二者都以「已定位的 DO」直接进入行锁 + 重读，杜绝二次按串重查造成的撤销竞态窗口
        return revokeSession(accessTokenDO);
    }

    /**
     * ZS-LOGIN-002/006：撤销一条会话（访问令牌 + 刷新令牌 + 代际 + 缓存），以入参 DO 的 refreshToken 为锁锚。
     *
     * <p><b>锁序与重读</b>：先对刷新令牌行加行锁（{@code selectByRefreshTokenForUpdate}），获锁后按 refreshToken
     * <b>重读</b>当前存活的访问令牌代际（{@link #listAliveAccessTokens}），把「并发刷新在本事务获锁前已提交的
     * 新代际令牌」一并撤销。<b>不</b>依赖入参 DO 自身是否仍存活——即便它已被并发刷新取代（逻辑删除），
     * 重读仍会命中新代际并撤销，消除「校验时 DO 存活、委托时按串重查得 null 提前返回、新代际幸存」的竞态窗口
     * （ZS-LOGIN-006 codex r0 P2）。
     */
    private OAuth2AccessTokenDO revokeSession(OAuth2AccessTokenDO accessTokenDO) {
        // ZS-LOGIN-002：行锁 + 获锁后重读，撤销并发刷新可能已提交的新代际访问令牌
        String refreshToken = accessTokenDO.getRefreshToken();
        oauth2RefreshTokenMapper.selectByRefreshTokenForUpdate(refreshToken);
        List<Runnable> cacheInvalidations = new ArrayList<>();
        for (OAuth2AccessTokenDO aliveToken : listAliveAccessTokens(refreshToken, accessTokenDO)) {
            oauth2AccessTokenMapper.deleteById(aliveToken.getId());
            cacheInvalidations.add(() -> revokeWithTombstone(aliveToken.getAccessToken(), aliveToken.getExpiresTime()));
            // ZS-LOGIN-005.B：事务内预写 ACCESS 补偿事件
            appendRevocationCompensationEvent(aliveToken.getId(), TokenType.ACCESS, aliveToken.getExpiresTime(),
                    aliveToken.getTenantId(), aliveToken.getUserId());
        }
        // 删除刷新令牌
        OAuth2RefreshTokenDO refreshTokenDO = oauth2RefreshTokenMapper.selectByRefreshToken(refreshToken);
        // ZS-LOGIN-005.B：REFRESH 类型预写补偿事件（合成凭据 key 命名空间与 ACCESS 共用）
        if (refreshTokenDO != null) {
            appendRevocationCompensationEvent(refreshTokenDO.getId(), TokenType.REFRESH, refreshTokenDO.getExpiresTime(),
                    refreshTokenDO.getTenantId(), refreshTokenDO.getUserId());
        }
        oauth2RefreshTokenMapper.deleteByRefreshToken(refreshToken);
        // ZS-LOGIN-003 codex r1 P1：刷新令牌串可能充当「已缓存转换凭据」的 Redis key（ZS-LOGIN-001 兼容路径），
        // 撤销时同样落墓碑（TTL 取刷新令牌剩余有效期，删除前读取），堵住并发门控路径从旧 DB 快照回填复活
        cacheInvalidations.add(() -> revokeWithTombstone(refreshToken, refreshTokenDO != null
                ? refreshTokenDO.getExpiresTime() : accessTokenDO.getExpiresTime()));
        // ZS-LOGIN-002：会话终结，清理代际键（访问令牌侧的代际标识保留至自然过期，供审计定位）
        cacheInvalidations.add(() -> deleteSessionGenerationQuietly(refreshToken));
        // ZS-LOGIN-005.A：缓存失效注册到事务提交后执行（回滚亦执行失效补偿），失败告警不阻断
        invalidateCacheAfterCommit("退出撤销(removeAccessToken)", cacheInvalidations);
        return accessTokenDO;
    }

    /**
     * ZS-LOGIN-006：按「不可用于认证的会话 ID」撤销会话，替代以原始 accessToken 串标识踢出。
     *
     * <p><b>为何用 ID 而非令牌串</b>：以 accessToken 串踢出，要求管理端分页回显令牌、前端持有令牌，
     * 等同把「持有即可认证」的凭据暴露给查看权限主体（前端表格 / 浏览器缓存 / 访问日志）。改用 DB 主键 ID
     * 作为「不可用于认证的会话标识」，管理端无需再持有任何秘密即可完成踢出。
     *
     * <p><b>租户隔离（纵深防御）</b>：{@code selectById} 天然租户作用域（{@link OAuth2AccessTokenDO} 继承
     * {@code TenantBaseDO}，生产环境租户拦截器追加 {@code tenant_id} 过滤），跨租户 ID 查不到 → 幂等 {@code null}；
     * 服务层再显式复核当前租户上下文，杜绝拦截器被绕过 / 误配时的越权撤销（无上下文或显式忽略租户时跳过）。
     *
     * <p><b>归属校验</b>：{@code expectedUserId} 非空（自助撤销）时只能操作本人会话，非本人抛
     * {@code OAUTH2_TOKEN_SESSION_NOT_OWNED}；且必须为真实用户（> 0），client_credentials 机器令牌 userId=0
     * 抛 {@code OAUTH2_TOKEN_SESSION_SELF_REQUIRES_USER}（ZS-LOGIN-006 codex r0 P1）；{@code expectedUserType} 非空时
     * 一并校验用户类型，防同编号跨 ADMIN/MEMBER 误撤（codex r0 P2）；均为空（管理员撤销）不受归属限制。
     *
     * <p><b>撤销主体</b>委托与 {@link #removeAccessToken(String)} 共享的 {@link #revokeSession}（行锁 + 获锁后重读 +
     * 代际清理 + 刷新令牌撤销 + 提交后缓存失效）；以已校验 DO 的 refreshToken 为锁锚，<b>不</b>再二次
     * {@code selectByAccessToken} 重查，消除「校验→委托」两次查询间的撤销竞态窗口（codex r0 P2）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OAuth2AccessTokenDO removeAccessTokenById(Long id, Long expectedUserId, Integer expectedUserType) {
        // 1. 以会话 ID 定位（selectById 租户作用域）；不存在 / 已清理 / 跨租户（拦截器已过滤）→ 查不到
        OAuth2AccessTokenDO accessTokenDO = oauth2AccessTokenMapper.selectById(id);
        if (accessTokenDO == null) {
            return null; // 幂等：不回显存在性，杜绝跨租户 / 越权探测
        }
        // 2. 纵深防御：显式复核租户上下文（H2 单测无拦截器，生产即便拦截器被绕过 / 误配也不越权）
        Long currentTenantId = TenantContextHolder.getTenantId();
        if (currentTenantId != null && !TenantContextHolder.isIgnore()
                && !currentTenantId.equals(accessTokenDO.getTenantId())) {
            return null; // 跨租户参数：幂等拒绝
        }
        // 3. 自助撤销前置：expectedUserId 非空即自助语义，必须为真实用户（> 0）。client_credentials 机器令牌
        //    userId=0，多个客户端共享 userId=0，若放行将跨客户端列出 / 撤销他人会话（ZS-LOGIN-006 codex r0 P1）。
        //    与 Controller 双重设防：即使 Controller 守卫被绕过，服务层仍拒绝机器主体自助撤销。
        if (expectedUserId != null && expectedUserId <= 0L) {
            throw exception(OAUTH2_TOKEN_SESSION_SELF_REQUIRES_USER);
        }
        // 4. 归属校验：自助撤销只能操作本人会话（userId 维）；管理员撤销（expectedUserId 为空）不受限
        if (expectedUserId != null && !expectedUserId.equals(accessTokenDO.getUserId())) {
            throw exception(OAUTH2_TOKEN_SESSION_NOT_OWNED);
        }
        // 4.1 userType 双维校验：防同编号跨 ADMIN/MEMBER 类型误撤（ZS-LOGIN-006 codex r0 P2）
        if (expectedUserType != null && !expectedUserType.equals(accessTokenDO.getUserType())) {
            throw exception(OAUTH2_TOKEN_SESSION_NOT_OWNED);
        }
        // 5. 委托共享撤销逻辑：以已校验 DO 的 refreshToken 加锁 + 重读撤销当前代际，不再二次按串重查
        return revokeSession(accessTokenDO);
    }

    /**
     * ZS-LOGIN-002：按用户维度批量撤销令牌（用户被禁用/删除/改密等）。
     *
     * <p>与 {@link #removeAccessToken(String)} 同样遵循「先锁刷新令牌行，再撤销访问令牌」的锁序，
     * 并在获锁后重读，避免与并发刷新互相复活。
     *
     * <p>ZS-LOGIN-002 P2-A（codex r0 修复）：获取锁前对 refresh-token 标识<b>去重并按自然序排序</b>。
     * 原因：access-token 查询结果的 ID 顺序跨刷新不稳定（刷新会删旧插新、改变 ID 序），
     * 若按该顺序逐个 FOR UPDATE，两个并发的用户级批量撤销可能以相反顺序获取锁 → 死锁。
     * 排序后所有并发调用者以相同顺序竞争行锁，从根本上消除锁环（Coffman 条件之「循环等待」）。
     *
     * <p>本方法此前<b>无 {@code @Transactional}</b>：加上事务既是原子性要求（批量撤销不应半途而废），
     * 也是行锁生效的前提（无事务则每条语句自动提交、锁立即释放）。
     *
     * <p><b>ZS-LOGIN-003 补齐（孤立刷新凭据）</b>：此前待撤销的 refresh-token 集合<b>完全由 access-token
     * 记录反推</b>，且 access-token 为空时首句即 early-return。于是「无 Access 记录但仍有 Refresh」的
     * 孤立刷新凭据<b>永远不会被撤销</b>——它仍可用于 {@link #refreshAccessToken} 换出全新访问令牌
     * （会话复活），在 ZS-LOGIN-001 兼容门控开启时更可直接当作访问令牌通过鉴权；同时该门控下
     * {@code convertToAccessToken} 写入 Redis 的「已缓存转换凭据」（key = refreshToken 串）也无人清理，
     * 会一直存活到刷新令牌 TTL（default client 达 30 天）。现在改为取
     * 「<b>access-token 反推 ∪ refresh-token 直查</b>」的<b>全集</b>，逐个撤销访问令牌、刷新令牌、
     * Redis 缓存凭据（含合成条目）与会话代际键。
     *
     * <p><b>ZS-LOGIN-003 补齐（租户上下文）</b>：撤销是安全操作，必须覆盖该用户名下的<b>全部</b>会话，
     * 不能被调用方的 ThreadLocal 租户上下文静默收窄——无租户上下文（定时任务 / MQ 消费）时租户拦截器会因
     * {@code getRequiredTenantId()} 抛 NPE，跨租户运维调用时则查不到目标用户凭据 → 撤销静默 no-op，
     * 被禁用/被删除用户的会话继续存活。故整个撤销体在 {@link TenantUtils#executeIgnore(Runnable)} 内执行，
     * 退出后自动恢复调用方上下文（不污染）。
     *
     * <p><b>ZS-LOGIN-003 补齐（审计）</b>：大批会话被静默撤销此前无任何日志，事件不可追踪；
     * 现在撤销完成后输出结构化 INFO 日志（用户编号 / 用户类型 / 会话数 / 其中孤立刷新凭据数），
     * 与调用方（{@code AdminUserServiceImpl}）的「失效原因」日志经 trace-id 关联。
     *
     * <p><b>范围边界</b>：仅技术账号闭环（B03）。任职撤销 / 业务组织语义归 B07（D-09 后），本方法不预埋。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeAccessToken(Long userId, Integer userType) {
        TenantUtils.executeIgnore(() -> doRemoveAccessTokenByUser(userId, userType));
    }

    /**
     * ZS-LOGIN-003：用户维度撤销的实际执行体，<b>必须</b>在「忽略租户」作用域内被调用。
     *
     * @param userId   用户编号
     * @param userType 用户类型
     */
    private void doRemoveAccessTokenByUser(Long userId, Integer userType) {
        // ZS-LOGIN-003 codex r2 P1：统一锁序最外层先锁用户行（仅技术账号）——
        // 与凭据兑换路径（grantAuthorizationCodeForAccessToken 的 selectByIdForUpdate）及账号状态
        // 更新（UPDATE 隐式行锁）互斥，杜绝「兑换读到启用态 → 撤销提交 → 兑换仍建新会话」交错；
        // 会员（MEMBER）账号兑换状态校验归其模块任务，不加锁
        if (UserTypeEnum.ADMIN.getValue().equals(userType)) {
            adminUserMapper.selectByIdForUpdate(userId);
        }
        // ZS-LOGIN-003 codex r2 P1：无论有无会话，未消费授权码都必须失效——用户取得 code 后退出登录
        // 再被改密/禁用时无 Access/Refresh 记录，若因空会话提前返回将残留可兑换的旧 code（撤销后复活）
        int revokedCodes = oauth2CodeMapper.deleteByUserIdAndUserType(userId, userType);
        if (revokedCodes > 0 && log.isInfoEnabled()) {
            log.info("[removeAccessToken][ZS-LOGIN-003 用户({}/{}) 的未消费授权码已一并失效：数量({})]", userType, userId, revokedCodes);
        }
        // 1. 汇总待撤销的 refresh-token 全集：access-token 反推 ∪ refresh-token 直查
        List<OAuth2AccessTokenDO> accessTokens =
                ObjectUtil.defaultIfNull(oauth2AccessTokenMapper.selectListByUserIdAndUserType(userId, userType),
                        Collections.emptyList());
        List<OAuth2RefreshTokenDO> refreshTokens =
                ObjectUtil.defaultIfNull(oauth2RefreshTokenMapper.selectListByUserIdAndUserType(userId, userType),
                        Collections.emptyList());
        // ZS-LOGIN-002 P2-A：去重 + 自然序排序 refresh-token，确保并发批量撤销以稳定顺序获取行锁
        Set<String> sortedRefreshTokens = new TreeSet<>();
        for (OAuth2AccessTokenDO accessToken : accessTokens) {
            if (StrUtil.isNotEmpty(accessToken.getRefreshToken())) {
                sortedRefreshTokens.add(accessToken.getRefreshToken());
            }
        }
        // ZS-LOGIN-003：refresh-token 直查补齐「孤立刷新凭据」（无 access-token 记录，反推发现不了）
        int orphanCount = 0;
        for (OAuth2RefreshTokenDO refreshTokenDO : refreshTokens) {
            String refreshToken = refreshTokenDO.getRefreshToken();
            if (StrUtil.isEmpty(refreshToken)) {
                continue;
            }
            if (sortedRefreshTokens.add(refreshToken)) {
                orphanCount++;
            }
        }
        if (sortedRefreshTokens.isEmpty()) {
            return; // 该用户无任何会话，无害空操作（不加锁、不写日志）
        }
        // 2. 按稳定顺序逐个锁定并撤销（缓存失效统一注册到事务提交后执行——ZS-LOGIN-005.A）
        List<Runnable> cacheInvalidations = new ArrayList<>();
        for (String refreshToken : sortedRefreshTokens) {
            // ZS-LOGIN-002：行锁 + 获锁后重读
            oauth2RefreshTokenMapper.selectByRefreshTokenForUpdate(refreshToken);
            // 找到该 refresh-token 对应的任一 access-token 作为 fallback；孤立刷新凭据没有 fallback（为 null）
            OAuth2AccessTokenDO fallback = null;
            for (OAuth2AccessTokenDO at : accessTokens) {
                if (refreshToken.equals(at.getRefreshToken())) {
                    fallback = at;
                    break;
                }
            }
            // 获锁后重读，覆盖并发刷新在获锁前刚插入的新代际访问令牌（防会话复活）
            for (OAuth2AccessTokenDO aliveToken : listAliveAccessTokens(refreshToken, fallback)) {
                oauth2AccessTokenMapper.deleteById(aliveToken.getId());
                cacheInvalidations.add(() -> revokeWithTombstone(aliveToken.getAccessToken(), aliveToken.getExpiresTime()));
                // ZS-LOGIN-005.B：事务内预写 ACCESS 补偿事件
                appendRevocationCompensationEvent(aliveToken.getId(), TokenType.ACCESS, aliveToken.getExpiresTime(),
                        aliveToken.getTenantId(), aliveToken.getUserId());
            }
            // ZS-LOGIN-003 codex r1 P1：删除前先读刷新令牌剩余有效期（删除后查不到），落撤销墓碑，
            // 堵住并发鉴权/门控路径从旧 DB 快照回填复活
            OAuth2RefreshTokenDO revokeTarget = oauth2RefreshTokenMapper.selectByRefreshToken(refreshToken);
            // ZS-LOGIN-005.B：REFRESH 类型预写补偿事件
            if (revokeTarget != null) {
                appendRevocationCompensationEvent(revokeTarget.getId(), TokenType.REFRESH, revokeTarget.getExpiresTime(),
                        revokeTarget.getTenantId(), revokeTarget.getUserId());
            }
            // 删除刷新令牌
            oauth2RefreshTokenMapper.deleteByRefreshToken(refreshToken);
            // ZS-LOGIN-003：清除「刷新令牌被当作访问令牌」时缓存下来的转换凭据（ZS-LOGIN-001 兼容路径写入，
            // key 即 refreshToken 串，TTL 继承刷新令牌）；对孤立刷新凭据而言这是唯一的清理时机
            cacheInvalidations.add(() -> revokeWithTombstone(refreshToken,
                    revokeTarget != null ? revokeTarget.getExpiresTime() : null));
            // ZS-LOGIN-002：会话终结，清理代际键
            cacheInvalidations.add(() -> deleteSessionGenerationQuietly(refreshToken));
        }
        // ZS-LOGIN-005.A：缓存失效注册到事务提交后执行（回滚亦执行失效补偿），失败告警不阻断
        invalidateCacheAfterCommit("用户级撤销(removeAccessTokenByUser)", cacheInvalidations);
        // 3. ZS-LOGIN-003：审计可追踪（令牌串不入日志，仅记数量与用户标识）
        if (log.isInfoEnabled()) {
            log.info("[removeAccessToken][ZS-LOGIN-003 用户({}/{}) 的全部会话已撤销：会话数({})，其中孤立刷新凭据({})]",
                    userType, userId, sortedRefreshTokens.size(), orphanCount);
        }
    }

    @Override
    public PageResult<OAuth2AccessTokenDO> getAccessTokenPage(OAuth2AccessTokenPageReqVO reqVO) {
        return oauth2AccessTokenMapper.selectPage(reqVO);
    }

    private OAuth2AccessTokenDO createOAuth2AccessToken(OAuth2RefreshTokenDO refreshTokenDO, OAuth2ClientDO clientDO) {
        OAuth2AccessTokenDO accessTokenDO = new OAuth2AccessTokenDO().setAccessToken(generateAccessToken())
                .setUserId(refreshTokenDO.getUserId()).setUserType(refreshTokenDO.getUserType())
                .setUserInfo(buildUserInfo(refreshTokenDO.getUserId(), refreshTokenDO.getUserType()))
                .setClientId(clientDO.getClientId()).setScopes(refreshTokenDO.getScopes())
                .setRefreshToken(refreshTokenDO.getRefreshToken())
                .setExpiresTime(DateUtils.now().plusSeconds(clientDO.getAccessTokenValiditySeconds()));
        // 优先从 refreshToken 获取租户编号，避免 ThreadLocal 被污染时导致 tenantId 为 null
        // 可能关联的 issue：https://t.zsxq.com/JIi5G
        Long tenantId = refreshTokenDO.getTenantId();
        if (tenantId == null) {
            tenantId = TenantContextHolder.getTenantId();
        }
        accessTokenDO.setTenantId(tenantId);
        oauth2AccessTokenMapper.insert(accessTokenDO);
        // 记录到 Redis 中
        // ZS-LOGIN-005.A：缓存发布注册到事务提交后执行——事务回滚时不得残留「DB 无行、Redis 有值」的幽灵新令牌
        publishCacheAfterCommit("新访问令牌(createOAuth2AccessToken)",
                () -> oauth2AccessTokenRedisDAO.set(accessTokenDO),
                () -> oauth2AccessTokenRedisDAO.delete(accessTokenDO.getAccessToken()));
        return accessTokenDO;
    }

    private OAuth2RefreshTokenDO createOAuth2RefreshToken(Long userId, Integer userType, OAuth2ClientDO clientDO, List<String> scopes) {
        OAuth2RefreshTokenDO refreshToken = new OAuth2RefreshTokenDO().setRefreshToken(generateRefreshToken())
                .setUserId(userId).setUserType(userType)
                .setClientId(clientDO.getClientId()).setScopes(scopes)
                .setExpiresTime(DateUtils.now().plusSeconds(clientDO.getRefreshTokenValiditySeconds()));
        oauth2RefreshTokenMapper.insert(refreshToken);
        return refreshToken;
    }

    private OAuth2AccessTokenDO convertToAccessToken(OAuth2RefreshTokenDO refreshTokenDO) {
        OAuth2AccessTokenDO accessTokenDO = BeanUtils.toBean(refreshTokenDO, OAuth2AccessTokenDO.class)
                .setAccessToken(refreshTokenDO.getRefreshToken());
        TenantUtils.execute(refreshTokenDO.getTenantId(),
                        () -> accessTokenDO.setUserInfo(buildUserInfo(refreshTokenDO.getUserId(), refreshTokenDO.getUserType())));
        return accessTokenDO;
    }

    /**
     * ZS-LOGIN-001：判断是否为「由刷新令牌转换而来的合成访问令牌」。
     * <p>{@link #convertToAccessToken} 以 refreshToken 串同时充当 accessToken 与 refreshToken，故二者相等即合成令牌；
     * 正常访问令牌的 accessToken 与 refreshToken 是两个独立生成的 UUID，不会相等。用于门控关闭时识别并拒绝缓存污染条目。
     */
    private static boolean isSyntheticAccessToken(OAuth2AccessTokenDO accessTokenDO) {
        return StrUtil.isNotEmpty(accessTokenDO.getAccessToken())
                && StrUtil.equals(accessTokenDO.getAccessToken(), accessTokenDO.getRefreshToken());
    }

    /**
     * ZS-LOGIN-002：读取某刷新令牌下当前存活的访问令牌；若一条都读不到（例如已被并发路径删除），
     * 退化为仅包含入参 {@code fallback} 自身，保证「至少撤销调用方所持的那一张」。
     *
     * <p>ZS-LOGIN-003：{@code fallback} 允许为 {@code null}——用户维度撤销时，
     * 「无 Access 记录但仍有 Refresh」的孤立刷新凭据没有可退化的 access-token，
     * 此时返回空集合（该会话本来就没有访问令牌需要撤销）。
     */
    private List<OAuth2AccessTokenDO> listAliveAccessTokens(String refreshToken, OAuth2AccessTokenDO fallback) {
        List<OAuth2AccessTokenDO> aliveTokens = oauth2AccessTokenMapper.selectListByRefreshToken(refreshToken);
        if (CollUtil.isNotEmpty(aliveTokens)) {
            return aliveTokens;
        }
        return fallback == null ? Collections.emptyList() : Collections.singletonList(fallback);
    }

    /**
     * ZS-LOGIN-002：推进会话代际号，并把新访问令牌登记到该代际（审计可定位标识）。
     *
     * <p>best-effort：Redis 抖动<b>不得</b>阻断合法刷新，故整体 try/catch 降级为告警。
     * 真正的重放拒绝由「刷新时撤销旧代际访问令牌（DB + Redis）」保证，与本登记无关。
     * 代际状态只存 Redis、<b>不新增 DB 列</b>（避开 Flyway 迁移与 ZS-DB-019.B 耦合）。
     *
     * <p>ZS-LOGIN-002 P2-B（codex r0 修复）：改用毫秒精度 TTL（{@link #millisUntil}），
     * 对正的亚秒剩余时间仍设有限 TTL（而非误当"无限期"），对已过期条目跳过写入。
     */
    private void recordSessionGeneration(OAuth2RefreshTokenDO refreshTokenDO, OAuth2AccessTokenDO accessTokenDO) {
        String refreshToken = refreshTokenDO.getRefreshToken();
        try {
            long generation = oauth2AccessTokenRedisDAO.nextSessionGeneration(refreshToken,
                    millisUntil(refreshTokenDO.getExpiresTime()));
            oauth2AccessTokenRedisDAO.setAccessTokenGeneration(accessTokenDO.getAccessToken(), generation,
                    millisUntil(accessTokenDO.getExpiresTime()));
            if (log.isInfoEnabled()) {
                log.info("[recordSessionGeneration][ZS-LOGIN-002 会话(user={}/{}, refresh={}) 刷新至代际({})，"
                                + "旧代际访问令牌已撤销，新访问令牌({})]",
                        refreshTokenDO.getUserType(), refreshTokenDO.getUserId(), maskToken(refreshToken),
                        generation, maskToken(accessTokenDO.getAccessToken()));
            }
        } catch (Exception ex) {
            log.warn("[recordSessionGeneration][ZS-LOGIN-002 会话(refresh={}) 代际登记失败，降级为不登记；"
                    + "不影响本次刷新结果，但审计侧将缺失该代际标识]", maskToken(refreshToken), ex);
        }
    }

    /**
     * ZS-LOGIN-003 codex r1 P1：撤销缓存凭据前先落「撤销墓碑」，再删除缓存。
     *
     * <p>竞态背景：{@link #getAccessToken} 缓存未命中时从 DB 回源并回填缓存。若「读 DB 旧快照 →
     * 本方撤销提交并删缓存 → 旧快照回填」交错，旧凭据会在 Redis 复活，后续缓存命中不再校验
     * 用户状态，被禁用/删除用户的旧凭据（含 gate 开启时的合成凭据）继续有效。墓碑以
     * 「查墓碑 + 写缓存」的 Lua 原子门闩拒绝该回填；TTL 取凭据剩余有效期，自清理。
     */
    private void revokeWithTombstone(String token, LocalDateTime expiresTime) {
        // ZS-LOGIN-005.A codex r0 P2：墓碑与删缓存逐项隔离——墓碑写失败仍须尝试删缓存，反之亦然
        long ttlMillis = millisUntil(expiresTime);
        try {
            oauth2AccessTokenRedisDAO.markRevoked(token, ttlMillis);
        } catch (Exception ex) {
            log.warn("[revokeWithTombstone][ZS-LOGIN-005.A 墓碑写入失败 token({}) 剩余ttl(ms)({})]"
                    + "——继续尝试删缓存，凭据权威以 DB 为准]", maskToken(token), ttlMillis, ex);
        }
        try {
            oauth2AccessTokenRedisDAO.delete(token);
        } catch (Exception ex) {
            log.warn("[revokeWithTombstone][ZS-LOGIN-005.A 缓存删除失败 token({})]"
                    + "——checkAccessToken DB 权威校验自愈 evict 即修复路径]", maskToken(token), ex);
        }
    }

    /**
     * ZS-LOGIN-005.B：事务内预写补偿事件到 Outbox（JOB-002 SPI，MANDATORY 传播）——业务回滚则事件一并回滚。
     *
     * <p><b>当前状态：RED-2 骨架</b>——方法体仅 log.debug 后 return，测试断言 {@code outbox_event}
     * 新增行数 > 0 会失败；GREEN-2 阶段通过 {@link #reliableEventPortProvider} 懒解析
     * {@link ReliableEventPort} 并调用 {@code append}。
     *
     * <p><b>降级契约</b>：{@code ObjectProvider.getIfAvailable()} 返回 {@code null} 时（system 单独运行 /
     * infra 未装配），静默降级为 {@code .A} 语义（无补偿链路），保留撤销主逻辑不变。
     *
     * @param tokenId       令牌 DB 主键
     * @param tokenType     令牌类型（ACCESS / REFRESH）
     * @param expiresTime   到期时间（写入事件载荷快照，Sink 用于过期识别）
     * @param tokenTenantId 被撤销令牌行的租户（事件租户归属权威，非调用者上下文租户）
     * @param subjectUserId 被撤销凭据属主用户 ID（回填 actorId 供后台重放关联）
     */
    /**
     * r1 P3：撤销事件主体按【调用时请求上下文】判定——管理员踢出与用户自操作共用同一撤销方法，
     * 按方法名硬编码会错标（r1 实证：revokeSession 固定 USER 漏管理员踢出、用户级撤销固定 ADMIN
     * 混入自助改密）。上下文有登录主体时按其 userType 分 ADMIN/USER，无上下文（定时清理/MQ）为 SYSTEM。
     */
    private OutboxEventMessage.OutboxActorType resolveRevocationActorType() {
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser == null) {
            return OutboxEventMessage.OutboxActorType.SYSTEM;
        }
        return UserTypeEnum.ADMIN.getValue().equals(loginUser.getUserType())
                ? OutboxEventMessage.OutboxActorType.ADMIN
                : OutboxEventMessage.OutboxActorType.USER;
    }

    private void appendRevocationCompensationEvent(Long tokenId, TokenType tokenType, LocalDateTime expiresTime,
                                                   Long tokenTenantId, Long subjectUserId) {
        // ZS-LOGIN-005.B GREEN-2：ObjectProvider 懒解析 → 降级或预写
        ReliableEventPort port = reliableEventPortProvider.getIfAvailable();
        if (port == null) {
            // 静默降级为 .A 语义（system 单独运行 / infra 未装配），不阻断撤销主逻辑
            log.debug("[appendRevocationCompensationEvent][ZS-LOGIN-005.B 降级：ReliableEventPort 不可用 tokenId={} tokenType={}]",
                    tokenId, tokenType);
            return;
        }
        // 构造 payload Map（JdbcReliableEventPort 内部 Jackson 序列化为 JSON 存入 outbox_event.payload）
        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("tokenId", tokenId);
        payloadMap.put("tokenType", tokenType);
        payloadMap.put("expiresTime", expiresTime);
        OutboxEventMessage message = OutboxEventMessage.builder()
                .eventType(SystemOutboxEventTypes.TOKEN_REVOCATION_COMPENSATION)
                .bizType(tokenType == TokenType.ACCESS
                        ? SystemOutboxEventTypes.BIZ_TYPE_OAUTH2_ACCESS_TOKEN
                        : SystemOutboxEventTypes.BIZ_TYPE_OAUTH2_REFRESH_TOKEN)
                .bizId(tokenType.name() + ":" + tokenId)
                .payload(payloadMap)
                .actorType(resolveRevocationActorType())
                .actorId(subjectUserId != null ? String.valueOf(subjectUserId) : null)
                .traceId(TracerUtils.getTraceId())
                .build();
        // codex r0 P1 修复（跨租户归属）：OutboxEventMessage 合同规定技术租户取自 TenantContextHolder
        // 当前上下文——跨租户管理端操作（A 租户管理员撤销 B 租户用户令牌）时若沿用调用者租户，
        // 事件落错租户、Sink 在事件租户上下文反查令牌行不可见 → 补偿静默丢失。改以【被撤销令牌行】
        // 的 tenantId 建立上下文完成 append（同一业务事务内仅切换租户上下文，回滚语义不变）。
        if (tokenTenantId == null) {
            port.append(message); // 防御分支：令牌行缺租户时维持原上下文（端口 MANDATORY 拒绝兜底）
            return;
        }
        TenantUtils.execute(tokenTenantId, () -> port.append(message));
    }

    /**
     * ZS-LOGIN-005.A：把「缓存失效类」Redis 动作（墓碑 + 删缓存 + 代际键）注册到事务<b>提交后</b>执行——
     * 撤销的权威状态在 DB，缓存失效不必也不应在事务内抢跑（事务回滚时 DB 凭据仍有效，缓存不应被提前清理后
     * 无人补偿）。事务回滚时同样执行失效补偿：缓存 miss 可回源重建，宁可可用性抖动也不留撤销态不一致；
     * 无事务上下文（测试 / 自调用）时立即执行。
     *
     * <p>Redis 动作失败<b>不阻断</b> DB 提交：输出结构化 WARN（可重放修复证据），修复路径 =
     * {@link #checkAccessToken} 的 DB 权威校验自愈 evict；可靠重放补偿归 ZS-LOGIN-005.B（B05 JOB-002）。
     */
    private void invalidateCacheAfterCommit(String desc, List<Runnable> invalidationSteps) {
        // ZS-LOGIN-005.A codex r0 P2：逐项隔离——任一步失败不中断剩余队列，每步分别记录修复证据；
        // afterCompletion 对【所有】结束状态幂等兜底（提交/回滚/UNKNOWN）：失效动作幂等（墓碑+删除），
        // 覆盖 STATUS_UNKNOWN（提交/回滚异常）与前序同步回调抛异常导致 afterCommit 被跳过的情形
        Runnable safeAll = () -> {
            for (int i = 0; i < invalidationSteps.size(); i++) {
                try {
                    invalidationSteps.get(i).run();
                } catch (Exception ex) {
                    log.warn("[invalidateCache][ZS-LOGIN-005.A {} 第 {}/{} 步缓存失效失败——凭据权威以 DB 为准"
                            + "（checkAccessToken DB 权威校验自愈 evict 即修复路径，可靠重放归 ZS-LOGIN-005.B）]",
                            desc, i + 1, invalidationSteps.size(), ex);
                }
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            // ZS-LOGIN-005.A codex r1 P3：单次执行于事务结束后（afterCompletion 覆盖全部结束状态）——
            // COMMITTED 即「提交后失效」；ROLLED_BACK / STATUS_UNKNOWN 为幂等兜底（回滚补偿 +
            // 前序同步回调异常跳过 afterCommit 的情形一并覆盖），且避免 afterCommit+afterCompletion 双跑翻倍
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    safeAll.run();
                }
            });
        } else {
            safeAll.run();
        }
    }

    /**
     * ZS-LOGIN-005.A：把「缓存发布类」Redis 动作（新令牌写缓存）注册到事务<b>提交后</b>执行——
     * 事务回滚时执行回滚清理，杜绝「缓存成功而事务回滚」留下的幽灵新令牌（DB 无行、Redis 有值）；
     * 无事务上下文（测试 / 自调用）时立即发布。
     */
    private void publishCacheAfterCommit(String desc, Runnable publish, Runnable rollbackCleanup) {
        Runnable safePublish = () -> {
            try {
                publish.run();
            } catch (Exception ex) {
                log.warn("[publishCache][ZS-LOGIN-005.A {} 缓存发布失败——新令牌可经缓存 miss 回源重建]", desc, ex);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            // ZS-LOGIN-005.A：与失效动作一致，单次执行于事务结束后——COMMITTED 才发布；
            // ROLLED_BACK / STATUS_UNKNOWN（提交/回滚异常）按保守清理处理：误清缓存只损失可用性
            // （miss 回源重建），残留幽灵发布则是安全缺口
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == TransactionSynchronization.STATUS_COMMITTED) {
                        safePublish.run();
                    } else {
                        try {
                            rollbackCleanup.run();
                        } catch (Exception ex) {
                            log.warn("[publishCache][ZS-LOGIN-005.A {} 回滚/未知状态清理失败]", desc, ex);
                        }
                    }
                }
            });
        } else {
            safePublish.run();
        }
    }

    /**
     * ZS-LOGIN-002：清理会话代际键（best-effort，失败只告警，不影响退出主流程）。
     */
    private void deleteSessionGenerationQuietly(String refreshToken) {
        try {
            oauth2AccessTokenRedisDAO.deleteSessionGeneration(refreshToken);
        } catch (Exception ex) {
            log.warn("[deleteSessionGenerationQuietly][ZS-LOGIN-002 会话(refresh={}) 代际键清理失败，"
                    + "将随 TTL 自然过期]", maskToken(refreshToken), ex);
        }
    }

    /**
     * ZS-LOGIN-002 P2-B：返回到期剩余毫秒数。
     * <p>返回值 <= 0 表示已过期或无到期时间（调用方应跳过写入，避免创建永不过期的键）。
     * <p>相比原 {@code secondsUntil}（截断为秒、亚秒返回 0 被误当"无限期"），毫秒精度确保
     * 配置 1 秒 access-token 生命周期的客户端每次刷新都得到有限 TTL 的代际键。
     */
    private static long millisUntil(LocalDateTime expiresTime) {
        if (expiresTime == null) {
            return -1L;
        }
        return Duration.between(LocalDateTime.now(), expiresTime).toMillis();
    }

    /**
     * 令牌脱敏：仅保留前 8 位，避免日志泄露完整凭据。
     */
    private static String maskToken(String token) {
        if (StrUtil.isEmpty(token)) {
            return "";
        }
        return token.length() <= 8 ? "***" : token.substring(0, 8) + "***";
    }

    /**
     * 加载用户信息，方便 {@link cn.zszj.framework.security.core.LoginUser} 获取到昵称、部门等信息
     *
     * @param userId 用户编号
     * @param userType 用户类型
     * @return 用户信息
     */
    private Map<String, String> buildUserInfo(Long userId, Integer userType) {
        if (userId == null || userId <= 0) {
            return Collections.emptyMap();
        }
        if (userType.equals(UserTypeEnum.ADMIN.getValue())) {
            AdminUserDO user = adminUserService.getUser(userId);
            return MapUtil.builder(LoginUser.INFO_KEY_NICKNAME, user.getNickname())
                    .put(LoginUser.INFO_KEY_DEPT_ID, StrUtil.toStringOrNull(user.getDeptId())).build();
        } else if (userType.equals(UserTypeEnum.MEMBER.getValue())) {
            // 注意：目前 Member 暂时不读取，可以按需实现
            return Collections.emptyMap();
        }
        throw new IllegalArgumentException("未知用户类型：" + userType);
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
        // 循环删除，直到没有满足条件的数据
        for (int i = 0; i < Short.MAX_VALUE; i++) {
            int deleteCount = oauth2RefreshTokenMapper.deleteByExpiresTimeLt(expireDate, deleteLimit);
            count += deleteCount;
            // 达到删除预期条数，说明到底了
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
        // 循环删除，直到没有满足条件的数据
        for (int i = 0; i < Short.MAX_VALUE; i++) {
            int deleteCount = oauth2AccessTokenMapper.deleteByExpiresTimeLt(expireDate, deleteLimit);
            count += deleteCount;
            // 达到删除预期条数，说明到底了
            if (deleteCount < deleteLimit) {
                break;
            }
        }
        return count;
    }
}
