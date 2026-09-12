package cn.zszj.framework.idempotent.core.aop;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.zszj.framework.common.exception.ServiceException;
import cn.zszj.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.zszj.framework.common.util.collection.CollectionUtils;
import cn.zszj.framework.common.util.log.LogSanitizeUtils;
import cn.zszj.framework.idempotent.core.annotation.Idempotent;
import cn.zszj.framework.idempotent.core.keyresolver.IdempotentKeyResolver;
import cn.zszj.framework.idempotent.core.redis.IdempotentRedisDAO;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 拦截声明了 {@link Idempotent} 注解的方法，实现幂等操作
 *
 * ZS-SEC-011.A：增加同键异参冲突检测，区分"重复请求"与"幂等键冲突"两种拒绝原因
 *
 * ZS-SEC-011.A 边界声明：本幂等能力是「窗口锁短时防重」（基于 Redis TTL 窗口），
 * 不是持久化幂等——进程重启、窗口超时、Redis 缓存故障后，不保证返回原业务结果，
 * 仅保证窗口内同键同参不重复执行；「同键异参冲突拒绝」仅在 Key 不含入参的解析器（ExpressionIdempotentKeyResolver）下生效——
 * DefaultIdempotentKeyResolver/UserIdempotentKeyResolver 已把 argsStr 烘入 Key，同键必然同参，conflict 恒为 false（构造上不可达）。
 * 让冲突检测对默认路径也有意义的 Key/Value 职责切分（Key 用业务幂等号、Value 存全量入参摘要）归 ZS-SEC-011.B。
 * 持久化幂等（落库 + HTTP 重试联验）归 ZS-SEC-011.B（B05 批次）范围。
 * 获准的重试仍会重新走完整鉴权链（幂等不替代鉴权）。
 *
 * ZS-SEC-011.A 已知缺口（本批 spec §4 文件清单仅含 DefaultIdempotentKeyResolver，以下登记不留白，挂后续任务）：
 * - [IMP-4 → REC-1/SEC-011.B] ExpressionIdempotentKeyResolver（Key=裸 SpEL 值，无 method/tenant/user 作用域）与
 *   UserIdempotentKeyResolver（有 user 无 tenant）仍缺租户隔离，与 SEC-010 已给 ExpressionRateLimiterKeyResolver 加租户作用域的先例不一致；
 *   且冲突检测唯一生效路径恰是 Expression 解析器 → 存在跨租户撞键 + 存在性侧信道风险。统一 SubjectScope 抽取归 REC-1（SEC-010+011.A 合并后跟进）。
 * - [IMP-5 → REC-1] 租户因子取自可选请求头 tenant-id（WebFrameworkUtils.getTenantId），非权威 TenantContextHolder（避免 protection 反依赖 biz-tenant，控制器授权偏离）；
 *   省略头会产生不同 Key → 同一主体自绕过防重窗口（非跨主体：userId/userType 取自权威 request attribute 不可伪造，tenant-id 头受 TenantSecurityWebFilter 校验不可伪造成他人租户）。
 *   权威租户源的可注入 port 归 REC-1。与 SEC-010 同性质。
 *
 * @author 芋道源码
 */
@Aspect
@Slf4j
public class IdempotentAspect {

    /**
     * MIN-3：同键异参冲突拒绝文案，提为常量便于统一维护与测试对齐
     */
    private static final String CONFLICT_MESSAGE = "幂等键冲突：相同幂等键携带了不同请求内容";

    /**
     * IdempotentKeyResolver 集合
     */
    private final Map<Class<? extends IdempotentKeyResolver>, IdempotentKeyResolver> keyResolvers;

    private final IdempotentRedisDAO idempotentRedisDAO;

    public IdempotentAspect(List<IdempotentKeyResolver> keyResolvers, IdempotentRedisDAO idempotentRedisDAO) {
        this.keyResolvers = CollectionUtils.convertMap(keyResolvers, IdempotentKeyResolver::getClass);
        this.idempotentRedisDAO = idempotentRedisDAO;
    }

    @Around(value = "@annotation(idempotent)")
    public Object aroundPointCut(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        // 获得 IdempotentKeyResolver
        IdempotentKeyResolver keyResolver = keyResolvers.get(idempotent.keyResolver());
        Assert.notNull(keyResolver, "找不到对应的 IdempotentKeyResolver");
        // 解析 Key
        String key = keyResolver.resolver(joinPoint, idempotent);

        // ZS-SEC-011.A：计算参数摘要，用于同键异参冲突检测
        // IMP-6：摘要输入改为「脱敏后」入参，避免未脱敏原文（含密码/令牌）MD5 后落 Redis value 被离线爆破（SEC-007 在 Redis 侧的对称缺口）；
        //        敏感字段差异被视为同参，对幂等语义无害
        // codex r0 P1：摘要「每请求必算」（含首次放行），须先经 serializableArgs 排除 servlet/spring-web 基础设施入参——
        //        LogSanitizeUtils 内部用 Jackson valueToTree 序列化会调用全部 getter，对 HttpServletResponse 触发 getWriter()，
        //        提前选定响应字符输出模式，破坏后续 ServletUtils.writeAttachment 等二进制输出（抛 IllegalStateException）
        String argsDigest = SecureUtil.md5(LogSanitizeUtils.sanitizeArgs(serializableArgs(joinPoint.getArgs())));

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
                log.warn("[aroundPointCut][幂等键({}) 读回摘要失败，降级按重复处理]", key, ex);
                storedDigest = null;
            }
            // IMP-3：StrUtil.isNotEmpty 兼容滚动升级期旧格式空串值（旧实现存 ""），避免误判冲突
            // IMP-1：默认路径（Default/User 解析器）Key 已烘入 argsStr，同键必然同参，conflict 恒 false（构造上不可达）；冲突分支仅对 Expression 解析器等 Key 不含入参的解析器生效
            boolean conflict = StrUtil.isNotEmpty(storedDigest) && !storedDigest.equals(argsDigest);
            // MIN-4：冲突通常是客户端 bug/探测（尤其 Expression 解析器下可能跨租户撞键），用 warn 提升安全信号；重复是常态（如双击），用 info
            if (conflict) {
                log.warn("[aroundPointCut][方法({}) 参数({}) 同键异参冲突]",
                        joinPoint.getSignature(), LogSanitizeUtils.sanitizeArgs(serializableArgs(joinPoint.getArgs())));
            } else {
                log.info("[aroundPointCut][方法({}) 参数({}) 同键同参重复]",
                        joinPoint.getSignature(), LogSanitizeUtils.sanitizeArgs(serializableArgs(joinPoint.getArgs())));
            }
            String msg = conflict ? CONFLICT_MESSAGE : idempotent.message();
            throw new ServiceException(GlobalErrorCodeConstants.REPEATED_REQUESTS.getCode(), msg);
        }

        // 2. 执行逻辑
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
     * 排除无法安全序列化的入参（servlet 请求 / 响应等基础设施对象），再交给 {@link LogSanitizeUtils#sanitizeArgs} 计算摘要 / 脱敏日志。
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
