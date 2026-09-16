package cn.zszj.framework.idempotent.core.aop;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.zszj.framework.common.util.collection.CollectionUtils;
import cn.zszj.framework.common.util.json.JsonUtils;
import cn.zszj.framework.common.util.log.LogSanitizeUtils;
import cn.zszj.framework.common.util.servlet.ServletUtils;
import cn.zszj.framework.idempotent.core.annotation.Idempotent;
import cn.zszj.framework.idempotent.core.keyresolver.IdempotentKeyResolver;
import cn.zszj.framework.idempotent.core.persistent.PersistentIdempotentRecord;
import cn.zszj.framework.idempotent.core.persistent.PersistentIdempotentStatus;
import cn.zszj.framework.idempotent.core.persistent.PersistentIdempotentStore;
import cn.zszj.framework.idempotent.core.redis.IdempotentRedisDAO;
import cn.zszj.framework.web.core.util.WebFrameworkUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 拦截声明了 {@link Idempotent} 注解的方法，实现幂等操作
 *
 * ZS-SEC-011.A：增加同键异参冲突检测，区分"重复请求"与"幂等键冲突"两种拒绝原因
 *
 * ZS-SEC-011.A 边界声明：非 persistent 模式仍是「窗口锁短时防重」（基于 Redis TTL 窗口），
 * 不是持久化幂等——进程重启、窗口超时、Redis 缓存故障后，不保证返回原业务结果，
 * 仅保证窗口内同键同参不重复执行。
 *
 * ZS-SEC-011.B（持久化幂等 + HTTP 重试联验）：
 * <ul>
 *     <li><b>Key/Value 职责切分</b>：Default/User 解析器 Key 去除 argsStr（REC-2 收编）——同键异参冲突在默认路径可达；
 *         读回摘要失败的告警只落 Key 的 md5 指纹，不落原值（codex 011.A P2-2 修复）；
 *         重放身份摘要为「原始业务入参」的 keyed SHA-256（{@link #computeArgsDigest}，codex 011.B r0 P2-1 修复：
 *         无损身份，仅敏感字段不同的请求按冲突拒绝；摘要输入不落日志/存储，与日志脱敏分属两条管线）；</li>
 *     <li><b>persistent=true 持久化幂等</b>：INSERT RUNNING → 业务 → markSuccess 与业务<b>同事务</b>提交
 *         （{@link PersistentIdempotentStore} 实现方强制 MANDATORY 参与），丢响应重放返回原结果快照、
 *         重启不重复写、业务回滚无残留记录；并发由 DB 唯一约束单层兜底（本模式不走 Redis，无双层竞态）。</li>
 * </ul>
 *
 * 持久化模式使用约束（不满足即 fail-closed，不静默降级）：
 * ① 注解方法必须在业务事务内被调用（无事务 → store 抛 IllegalTransactionStateException）；
 * ② 必须有登录主体（userId 非空）——持久化记录携带结果快照，匿名塌缩即跨主体结果重放；
 * ③ 操作/方法身份 + 主体因子（tenant/userId/userType）强制并入持久化键派生（含 Expression 解析器路径，
 *    codex 011.B r0 P1 修复：不同操作同业务键天然不同键，「取消」不会命中「创建」的快照；
 *    重放时还校验记录 actionScope 一致）；持久化键一律 md5 定长 32，无截断碰撞；
 * ④ 结果快照按声明返回类型反序列化（走无日志路径，损坏快照原文不落日志——codex 011.B r0 P2-2）：
 *    HTTP JSON 形状不变；Java 直调泛型元素退化的边界如实登记；
 *    快照缺失/损坏重放退化为「状态级复用」（900 拒绝），绝不重执行业务。
 *
 * ZS-SEC-011.A 已知缺口（本批 spec §4 文件清单仅含 DefaultIdempotentKeyResolver，以下登记不留白，挂后续任务）：
 * - [IMP-4 → REC-1/后续] ExpressionIdempotentKeyResolver（Redis 路径 Key=裸 SpEL 值，无 method/tenant/user 作用域）与
 *   UserIdempotentKeyResolver（有 user 无 tenant）在 <b>Redis 窗口路径</b>仍缺租户隔离（持久化路径已由主体因子强制并入堵住），
 *   统一 SubjectScope 抽取归 REC-1（SEC-010+011.A 合并后跟进）。
 * - [IMP-5 → REC-1] 租户因子取自可选请求头 tenant-id（WebFrameworkUtils.getTenantId），非权威 TenantContextHolder（避免 protection 反依赖 biz-tenant）；
 *   权威租户源的可注入 port 归 REC-1。与 SEC-010 同性质。
 *
 * 次序合同（重试重检授权）：本切面显式 {@link Ordered#LOWEST_PRECEDENCE}——Spring Security 方法安全
 * （@PreAuthorize，order≈400）恒先于本切面执行：撤权/无权主体重放同键请求在授权层即被拒绝，
 * 幂等记录/快照不构成授权缓存，重放请求每次都重新过完整安全链（infra 合同测试实证）。
 *
 * @author 芋道源码
 */
@Aspect
@Order(Ordered.LOWEST_PRECEDENCE)
@Slf4j
public class IdempotentAspect {

    /**
     * MIN-3：同键异参冲突拒绝文案，提为常量便于统一维护与测试对齐
     */
    private static final String CONFLICT_MESSAGE = "幂等键冲突：相同幂等键携带了不同请求内容";

    /**
     * 摘要 pepper 最小长度（codex 011.B r1 P2-C）：低于该长度视为弱密钥，配置了即启动失败（fail-fast）
     */
    static final int MIN_DIGEST_SECRET_LENGTH = 32;

    /**
     * IdempotentKeyResolver 集合
     */
    private final Map<Class<? extends IdempotentKeyResolver>, IdempotentKeyResolver> keyResolvers;

    private final IdempotentRedisDAO idempotentRedisDAO;

    /**
     * 持久化幂等存储提供者（ZS-SEC-011.B）：懒解析——未提供 store bean 且未启用 persistent 模式的应用零感知；
     * persistent=true 而 store 缺失时 fail-fast（IllegalStateException），不静默降级回 Redis 窗口
     */
    private final ObjectProvider<PersistentIdempotentStore> persistentStoreProvider;

    /**
     * 摘要 pepper（codex 011.B r1 P2-C）：部署期注入（zszj.security.idempotent.digest.secret，
     * 环境变量 ZSZJ_SECURITY_IDEMPOTENT_DIGEST_SECRET，≥{@link #MIN_DIGEST_SECRET_LENGTH} 字符），不硬编码。
     * <ul>
     *     <li>配置了但长度不足 → 构造即抛（fail-fast，明确的配置错误不允许带病启动）；</li>
     *     <li>未配置（null）→ 持久化幂等模式拒绝启用（请求 fail-closed + 构造 WARN 留痕），
     *         <b>不静默降级</b>到硬编码 pepper；Redis 窗口模式不受影响，摘要回退 .A 脱敏口径（legacy，有损边界已登记）。</li>
     * </ul>
     */
    private final String digestSecret;

    public IdempotentAspect(List<IdempotentKeyResolver> keyResolvers, IdempotentRedisDAO idempotentRedisDAO,
                            ObjectProvider<PersistentIdempotentStore> persistentStoreProvider, String digestSecret) {
        this.keyResolvers = CollectionUtils.convertMap(keyResolvers, IdempotentKeyResolver::getClass);
        this.idempotentRedisDAO = idempotentRedisDAO;
        this.persistentStoreProvider = persistentStoreProvider;
        if (digestSecret != null && !digestSecret.isEmpty()) {
            if (digestSecret.length() < MIN_DIGEST_SECRET_LENGTH) {
                // fail-fast：弱 pepper 比没有更危险（给运维「已加固」的错觉），不允许带病启动
                throw new IllegalArgumentException("持久化幂等摘要 pepper 过短：zszj.security.idempotent.digest.secret"
                        + "（环境变量 ZSZJ_SECURITY_IDEMPOTENT_DIGEST_SECRET）须 ≥" + MIN_DIGEST_SECRET_LENGTH + " 字符，实际 "
                        + digestSecret.length());
            }
            this.digestSecret = digestSecret;
        } else {
            this.digestSecret = null;
            // 启动 WARN（不静默）：持久化幂等不可用要在部署期被发现，而非首个 persistent 请求报错时
            log.warn("[IdempotentAspect][未配置 zszj.security.idempotent.digest.secret（环境变量 "
                    + "ZSZJ_SECURITY_IDEMPOTENT_DIGEST_SECRET，须 ≥{} 字符）：持久化幂等模式（persistent=true）不可用，"
                    + "Redis 窗口模式不受影响]", MIN_DIGEST_SECRET_LENGTH);
        }
    }

    /**
     * 摘要 pepper 是否可用（决定 keyed 无损摘要与持久化模式可用性）
     */
    private boolean digestSecretAvailable() {
        return digestSecret != null;
    }

    @Around(value = "@annotation(idempotent)")
    public Object aroundPointCut(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        // 获得 IdempotentKeyResolver
        IdempotentKeyResolver keyResolver = keyResolvers.get(idempotent.keyResolver());
        Assert.notNull(keyResolver, "找不到对应的 IdempotentKeyResolver");
        // 解析 Key
        String key = keyResolver.resolver(joinPoint, idempotent);

        // ZS-SEC-011.A：计算参数摘要，用于同键异参冲突检测 / 重放身份比对
        // codex r0 P1：摘要「每请求必算」（含首次放行），摘要管线内部经 serializableArgs 排除 servlet/spring-web 基础设施入参——
        //        序列化基础设施对象（如 HttpServletResponse）会触发 getWriter() 破坏后续二进制输出
        // codex 011.B r0 P2-1 修复 + r1 P2-C 强化：配置了部署期 pepper（≥32 字符）→「原始业务入参」的 keyed SHA-256
        //        （无损身份 + 不可离线爆破，摘要输入不落任何日志/存储，与日志脱敏分属两条管线）；
        //        未配置 pepper → Redis 窗口路径回退 .A 脱敏口径（md5(脱敏表示)，有损边界登记：仅敏感字段差异视为同参），
        //        持久化路径在下方的 persistent 分支直接拒绝启用（不静默降级）
        String argsDigest = digestSecretAvailable()
                ? computeArgsDigest(digestSecret, joinPoint.getArgs())
                : legacySanitizedArgsDigest(joinPoint.getArgs());

        // ZS-SEC-011.B：持久化幂等模式（DB 唯一约束 + 业务同事务 + 结果快照复用）
        if (idempotent.persistent()) {
            if (!digestSecretAvailable()) {
                // codex 011.B r1 P2-C：无 pepper 不静默降级——持久化幂等携带结果快照且要求不可离线爆破的身份摘要，必须显式启用
                throw new IllegalStateException("@Idempotent(persistent=true) 需要注入摘要 pepper："
                        + "zszj.security.idempotent.digest.secret（环境变量 ZSZJ_SECURITY_IDEMPOTENT_DIGEST_SECRET，"
                        + "≥" + MIN_DIGEST_SECRET_LENGTH + " 字符）；未配置则持久化幂等拒绝启用（Redis 窗口模式不受影响）");
            }
            PersistentIdempotentStore persistentStore = persistentStoreProvider.getIfAvailable();
            if (persistentStore == null) {
                throw new IllegalStateException("@Idempotent(persistent=true) 需要容器提供 PersistentIdempotentStore bean"
                        + "（如 zszj-module-infra 的 JdbcPersistentIdempotentStore）；不静默降级回 Redis 窗口锁");
            }
            return aroundPersistentPointCut(joinPoint, idempotent, key, argsDigest, persistentStore);
        }

        // ========== 以下为 Redis 窗口锁路径（非 persistent 模式，行为与 011.A 一致） ==========
        // 1. 锁定 Key（携带参数摘要）
        // MIN-2：DAO 返回 Boolean，pipeline/transaction 场景可能返回 null，用 Boolean.TRUE.equals 防拆箱 NPE（null 视为未拿到锁 → 拒绝，fail-closed）
        boolean success = Boolean.TRUE.equals(idempotentRedisDAO.setIfAbsent(key, argsDigest, idempotent.timeout(), idempotent.timeUnit()));
        // 锁定失败，区分冲突与重复
        if (!success) {
            // IMP-2：getDigest 是「已决策拒绝」后的额外 GET，防 Redis 抖动把干净的 900 升级为 500；异常降级 null（digest 只影响文案，零语义损失）
            String storedDigest;
            try {
                storedDigest = idempotentRedisDAO.getDigest(key);
            } catch (Exception ex) {
                // codex r0 P2-2 修复：Key 可能是 Expression 解析器的敏感原值（如 #request.token），告警只落 md5 指纹（可关联、不可逆），不落原值
                log.warn("[aroundPointCut][幂等键指纹({}) 读回摘要失败，降级按重复处理]", SecureUtil.md5(key), ex);
                storedDigest = null;
            }
            rejectRepeatOrConflict(joinPoint, argsDigest, storedDigest, idempotent.message());
        }

        // 2. 执行逻辑
        return proceedWithKey(joinPoint, idempotent, key);
    }

    /**
     * Redis 窗口路径的执行逻辑（成功放行；异常按 {@code deleteKeyWhenException} 删键）
     */
    private Object proceedWithKey(ProceedingJoinPoint joinPoint, Idempotent idempotent, String key) throws Throwable {
        try {
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            // 3. 异常时，删除 Key
            // 参考美团 GTIS 思路：https://tech.meituan.com/2016/09/29/distributed-system-mutually-exclusive-idempotence-cerberus-gtis.html
            if (idempotent.deleteKeyWhenException()) {
                idempotentRedisDAO.delete(key);
            }
            throw throwable;
        }
    }

    /**
     * 拒绝分支：比对已存摘要，区分「同键同参重复」（info）与「同键异参冲突」（warn + 冲突文案）
     */
    private void rejectRepeatOrConflict(ProceedingJoinPoint joinPoint, String argsDigest, String storedDigest, String repeatMessage) {
        // IMP-3：StrUtil.isNotEmpty 兼容滚动升级期旧格式空串值（旧实现存 ""），避免误判冲突
        boolean conflict = StrUtil.isNotEmpty(storedDigest) && !storedDigest.equals(argsDigest);
        // MIN-4：冲突通常是客户端 bug/探测，用 warn 提升安全信号；重复是常态（如双击），用 info
        if (conflict) {
            log.warn("[aroundPointCut][方法({}) 参数({}) 同键异参冲突]",
                    joinPoint.getSignature(), LogSanitizeUtils.sanitizeArgs(serializableArgs(joinPoint.getArgs())));
        } else {
            log.info("[aroundPointCut][方法({}) 参数({}) 同键同参重复]",
                    joinPoint.getSignature(), LogSanitizeUtils.sanitizeArgs(serializableArgs(joinPoint.getArgs())));
        }
        throw new ServiceException(GlobalErrorCodeConstants.REPEATED_REQUESTS.getCode(), conflict ? CONFLICT_MESSAGE : repeatMessage);
    }

    // ========== ZS-SEC-011.B：持久化幂等模式 ==========

    /**
     * 持久化幂等主流程：INSERT RUNNING → 业务 → markSuccess 与业务同事务（原子提交），重放复用记录/快照。
     *
     * 并发裁决 = DB 唯一约束单层（store 的 tryInsertRunning 以 ON CONFLICT DO NOTHING 语义原子抢占，
     * 败者事务不 aborted、可读回记录做分类）。本模式不使用 Redis，无 Redis×DB 双层竞态。
     */
    private Object aroundPersistentPointCut(ProceedingJoinPoint joinPoint, Idempotent idempotent, String resolvedKey,
                                            String argsDigest, PersistentIdempotentStore store) throws Throwable {
        // 强制登录主体：持久化记录携带结果快照，匿名/无上下文塌缩即跨主体结果重放 → fail-closed
        HttpServletRequest request = ServletUtils.getRequest();
        Long tenantId = request != null ? WebFrameworkUtils.getTenantId(request) : null;
        Long userId = request != null ? WebFrameworkUtils.getLoginUserId(request) : null;
        Integer userType = request != null ? WebFrameworkUtils.getLoginUserType(request) : null;
        if (userId == null) {
            log.error("[aroundPersistentPointCut][方法({}) 持久化幂等要求登录主体（tenantId={} userType={}），fail-closed 拒绝]",
                    joinPoint.getSignature(), tenantId, userType);
            throw new ServiceException(GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR.getCode(), "持久化幂等要求登录主体");
        }
        // 操作/方法身份（codex r0 P1）：取目标类 Method 的规范串（声明类+方法+参数类型，与代理实现类名无关、跨进程稳定），
        // 既并入持久化键派生（用全量串，md5 输入无界），也以有界形式落库并在重放时校验一致——
        // 不同操作同业务键天然不同键，「取消」不会命中早前「创建」的 SUCCESS 快照
        Method idempotentMethod = ((MethodSignature) joinPoint.getSignature()).getMethod();
        String actionScopeFull = idempotentMethod.toString();
        // codex 011.B r1 P2-B：列宽有界（varchar(1024)），超长降级为定长摘要表示（带 marker，无截断碰撞），
        // 落库与重放同口径（boundActionScope），storeKey 始终用全量串派生
        String actionScope = boundActionScope(actionScopeFull);
        // 主体因子 + 操作/方法身份强制并入键派生（含 Expression 解析器路径）：不同主体/不同操作同业务键永不共用记录
        String storeKey = SecureUtil.md5(actionScopeFull + ":" + resolvedKey + ":" + tenantId + ":" + userId + ":" + userType);

        // 快路径：已有记录（顺序重放，含丢响应/重启后重放）——不产生写入
        Optional<PersistentIdempotentRecord> existing = store.findByIdempotentKey(storeKey);
        if (!existing.isPresent()) {
            // 首次执行：插入 RUNNING 抢占（MANDATORY 参与调用方事务；唯一约束并发兜底）
            PersistentIdempotentRecord record = new PersistentIdempotentRecord();
            record.setIdempotentKey(storeKey);
            record.setTenantId(tenantId);
            record.setSubjectType(userType == null ? null : String.valueOf(userType));
            record.setSubjectId(String.valueOf(userId));
            record.setActionScope(actionScope);
            record.setRequestDigest(argsDigest);
            record.setStatus(PersistentIdempotentStatus.RUNNING);
            if (!store.tryInsertRunning(record)) {
                // 并发竞态败者：读回胜者记录做「重复/冲突/复用」分类（store 保证败者事务不 aborted）
                existing = store.findByIdempotentKey(storeKey);
                if (!existing.isPresent()) {
                    // 理论不可达（防御性兜底）：宁拒绝不重执行
                    log.error("[aroundPersistentPointCut][方法({}) 抢占失败且记录不可读，fail-closed 拒绝]", actionScope);
                    throw new ServiceException(GlobalErrorCodeConstants.REPEATED_REQUESTS.getCode(), idempotent.message());
                }
            }
        }
        if (existing.isPresent()) {
            return replayPersistent(joinPoint, idempotent, actionScope, storeKey, argsDigest, existing.get());
        }

        // 执行业务：markSuccess（或失败删/留）与业务同事务，随调用方提交/回滚同生共死
        try {
            Object result = joinPoint.proceed();
            store.markSuccess(storeKey, snapshotResult(result));
            return result;
        } catch (Throwable throwable) {
            if (idempotent.deleteKeyWhenException()) {
                store.deleteRunning(storeKey);
            } else {
                store.markFailed(storeKey);
            }
            throw throwable;
        }
    }

    /**
     * 持久化重放分类：摘要不等 → 冲突；SUCCESS+快照 → 复用原结果；其余（SUCCESS 无快照 / RUNNING / FAILED）→ 状态级拒绝
     */
    private Object replayPersistent(ProceedingJoinPoint joinPoint, Idempotent idempotent, String actionScope, String storeKey,
                                    String argsDigest, PersistentIdempotentRecord record) throws Throwable {
        // 操作/方法身份一致性校验（codex r0 P1 纵深防御）：键已含 actionScope，命中记录却 scope 不符
        // 只可能是哈希碰撞/记录被篡改——fail-closed 拒绝，绝不返回异操作快照。
        // codex r1 P2-B：两侧同走 boundActionScope 口径（落库即有界形式），超长方法也能一致比对
        if (!boundActionScope(((MethodSignature) joinPoint.getSignature()).getMethod().toString()).equals(record.getActionScope())) {
            log.error("[aroundPersistentPointCut][方法({}) 幂等键指纹({}) 记录操作身份不符（期望={} 实际={}），fail-closed 拒绝]",
                    joinPoint.getSignature(), SecureUtil.md5(storeKey), actionScope, record.getActionScope());
            throw new ServiceException(GlobalErrorCodeConstants.REPEATED_REQUESTS.getCode(), CONFLICT_MESSAGE);
        }
        if (!argsDigest.equals(record.getRequestDigest())) {
            log.warn("[aroundPersistentPointCut][方法({}) 幂等键指纹({}) 同键异参冲突]",
                    joinPoint.getSignature(), SecureUtil.md5(storeKey));
            throw new ServiceException(GlobalErrorCodeConstants.REPEATED_REQUESTS.getCode(), CONFLICT_MESSAGE);
        }
        if (record.getStatus() == PersistentIdempotentStatus.SUCCESS) {
            String snapshot = record.getResultSnapshot();
            if (snapshot != null) {
                try {
                    Type returnType = ((MethodSignature) joinPoint.getSignature()).getMethod().getGenericReturnType();
                    // codex r0 P2-2：必须走「无日志」反序列化路径——JsonUtils.parseObject(String, Type) 失败时
                    // 会先把整个输入（快照原文，可能含凭据/个人数据）打进内部日志再抛，外层 catch 挡不住；
                    // 故直接用 ObjectMapper.readValue，失败仅保留 errClass 级元数据（状态降级 900，绝不重执行业务）
                    ObjectMapper objectMapper = JsonUtils.getObjectMapper();
                    return objectMapper.readValue(snapshot, objectMapper.getTypeFactory().constructType(returnType));
                } catch (Exception ex) {
                    log.error("[aroundPersistentPointCut][方法({}) 幂等键指纹({}) 结果快照解析失败，降级状态级复用 errClass={}]",
                            joinPoint.getSignature(), SecureUtil.md5(storeKey), ex.getClass().getName());
                }
            }
            throw new ServiceException(GlobalErrorCodeConstants.REPEATED_REQUESTS.getCode(), idempotent.message());
        }
        // RUNNING（并发处理中）/ FAILED（deleteKeyWhenException=false 保留）：一律拒绝，不重复执行
        log.info("[aroundPersistentPointCut][方法({}) 幂等键指纹({}) 状态({}) 重放拒绝（不重复执行业务）]",
                joinPoint.getSignature(), SecureUtil.md5(storeKey), record.getStatus());
        throw new ServiceException(GlobalErrorCodeConstants.REPEATED_REQUESTS.getCode(), idempotent.message());
    }

    /**
     * 结果快照：JSON 序列化；失败返回 null（业务不因快照失败而失败，重放退化为状态级复用并 ERROR 留痕）
     */
    private String snapshotResult(Object result) {
        try {
            return JsonUtils.toJsonString(result);
        } catch (Exception ex) {
            log.error("[snapshotResult][结果快照序列化失败 errClass={}，重放将退化为状态级复用]", ex.getClass().getName());
            return null;
        }
    }

    /**
     * action_scope 列宽上限（codex 011.B r1 P2-B，与迁移 V20260916.101 / H2 create_tables 的 varchar(1024) 一致）
     */
    static final int ACTION_SCOPE_MAX_LENGTH = 1024;

    /**
     * action_scope 有界化（codex 011.B r1 P2-B）：{@link Method#toString()} 含全限定返回/参数类型，
     * 超长方法串会超出列宽导致业务执行前即插入失败（SQLSTATE 22001）。
     * ≤ 上限原样落库（保留可观测性）；超长降级为 {@code sha256:<64hex>} 定长表示——
     * 带 marker 前缀 + 全量哈希，<b>无截断碰撞</b>；落库与重放（{@link #replayPersistent}）同口径。
     * 持久化键派生不受影响（始终用全量串，md5 输入无界）。
     *
     * @param actionScope 方法规范串（全量）
     * @return 落库/比对用的有界形式（≤ 71 + 上限）
     */
    static String boundActionScope(String actionScope) {
        if (actionScope.length() <= ACTION_SCOPE_MAX_LENGTH) {
            return actionScope;
        }
        return "sha256:" + SecureUtil.sha256(actionScope);
    }

    /**
     * .A 口径摘要（无 pepper 时的 Redis 窗口路径回退）：「未截断脱敏表示」的 MD5。
     * 有损边界（登记）：password/token 等敏感字段差异被脱敏等价化 → 视为同参；
     * 后果限定为「重复按 900 拒绝」（无结果复用），持久化路径在无 pepper 时直接拒绝启用，不走此口径。
     */
    static String legacySanitizedArgsDigest(Object[] args) {
        return SecureUtil.md5(LogSanitizeUtils.sanitizeArgsUntruncated(serializableArgs(args)));
    }

    /**
     * 重放身份摘要（codex 011.B r0 P2-1 修复 + r1 P2-C pepper 部署期注入）：「原始业务入参」（未脱敏、未截断）
     * 的 keyed SHA-256，pepper 为部署期注入的 {@code digestSecret}（≥{@link #MIN_DIGEST_SECRET_LENGTH} 字符，构造期校验）。
     *
     * <p>与日志脱敏是<b>两条管线</b>：{@link LogSanitizeUtils}（掩码、截断）仅用于日志呈现——
     * 其掩码会把 password/token 等敏感字段等价化为 {@code ***}，若摘要走脱敏口径，
     * 仅敏感字段不同的两个请求会同摘要，第二个会被当重复/复用第一个的结果（有损身份，r0 P2-1 修复对象）。
     * 本方法序列化<b>原始</b>业务入参做 keyed 摘要，摘要输入不落任何日志/存储，
     * 落存储的只有不可逆的 HMAC-SHA256（64 hex，{@code request_digest varchar(64)} 恰容）——存储无泄密风险；
     * pepper 不再硬编码（r1 P2-C）：持有落库摘要 + 知晓源码不再足以离线枚举低熵凭据，还须持有部署期注入的 pepper。
     *
     * <p>{@code args} 先经 {@link #serializableArgs} 排除 servlet/spring-web 基础设施入参（防序列化副作用，codex 011.A r0 P1）
     * ——<b>过滤只做一次，主/降级两条路径共用同一过滤后数组</b>（r1 P2-A 修复：降级路径不得重新引入 servlet 入参）；
     * 极端情况下不可 JSON 化的业务入参降级脱敏口径（有损），并只记 errClass 不落原文。
     *
     * @param digestSecret 部署期注入的摘要 pepper（非空，长度由构造期校验）
     * @param args         原始方法入参
     * @return keyed SHA-256 hex（64 字符）
     */
    public static String computeArgsDigest(String digestSecret, Object[] args) {
        if (digestSecret == null || digestSecret.isEmpty()) {
            throw new IllegalArgumentException("摘要 pepper 未配置：持久化幂等/keyed 摘要要求 zszj.security.idempotent.digest.secret"
                    + "（环境变量 ZSZJ_SECURITY_IDEMPOTENT_DIGEST_SECRET，≥" + MIN_DIGEST_SECRET_LENGTH + " 字符）");
        }
        // codex r1 P2-A：入参过滤只做一次——主/降级路径共用过滤后数组，降级路径不得重新序列化 servlet 入参
        Object[] filteredArgs = serializableArgs(args);
        String digestInput;
        try {
            digestInput = JsonUtils.toJsonString(filteredArgs);
        } catch (Exception ex) {
            log.error("[computeArgsDigest][入参原文序列化失败，摘要降级脱敏口径（有损） errClass={}]", ex.getClass().getName());
            digestInput = LogSanitizeUtils.sanitizeArgsUntruncated(filteredArgs);
        }
        HMac hmac = new HMac(HmacAlgorithm.HmacSHA256, digestSecret.getBytes(StandardCharsets.UTF_8));
        return hmac.digestHex(digestInput);
    }

    /**
     * 排除无法安全序列化的入参（servlet 请求 / 响应等基础设施对象），再交给 {@link LogSanitizeUtils} 计算摘要 / 脱敏日志。
     *
     * 背景（codex r0 P1）：SEC-011.A 把参数摘要从「仅拒绝分支」提升为「每请求必算」（含首次放行）。
     * {@link LogSanitizeUtils} 内部用 Jackson {@code valueToTree} 序列化入参，会调用对象全部 getter；
     * 对 {@code HttpServletResponse} 会触发 {@code getWriter()}，提前选定响应字符输出模式，
     * 导致后续 {@code ServletUtils.writeAttachment} 等二进制输出抛 {@link IllegalStateException}。
     *
     * 排除口径（codex r1 P1 修正）：按「类型」而非「包名前缀」判定——运行时实现类
     * （Tomcat {@code org.apache.catalina.connector.ResponseFacade}、各类 {@code HttpServlet*Wrapper}）并不以 servlet 包名开头，
     * 仅靠包名前缀（{@link cn.zszj.framework.common.util.string.StrUtils#joinMethodArgs} 所用口径）会漏排它们；
     * {@code instanceof} 覆盖所有容器实现与包装器。另保留包名前缀作兜底，排除 {@code MultipartFile} 等其余 spring-web 基础设施对象。
     *
     * @param args 原始方法入参
     * @return 剔除 servlet / spring-web 基础设施对象后的入参（保持原有相对顺序，null 元素保留）
     */
    private static Object[] serializableArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return args;
        }
        List<Object> kept = new ArrayList<>(args.length);
        for (Object arg : args) {
            if (arg == null) {
                kept.add(null);
                continue;
            }
            // 按类型排除：覆盖所有容器实现（Tomcat ResponseFacade 等）与包装器（HttpServlet*Wrapper），运行时类名不在 servlet 包下也能命中
            if (arg instanceof Servlet || arg instanceof ServletRequest || arg instanceof ServletResponse) {
                continue;
            }
            // 兜底按包名前缀排除：其余不宜 Jackson 序列化的 spring-web 基础设施对象（如 MultipartFile），口径与 StrUtils.joinMethodArgs 一致
            String clazzName = arg.getClass().getName();
            if (StrUtil.startWithAny(clazzName, "javax.servlet", "jakarta.servlet", "org.springframework.web")) {
                continue;
            }
            kept.add(arg);
        }
        return kept.toArray();
    }

}
