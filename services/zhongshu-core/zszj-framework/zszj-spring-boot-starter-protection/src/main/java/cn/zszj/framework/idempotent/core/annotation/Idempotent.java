package cn.zszj.framework.idempotent.core.annotation;

import cn.zszj.framework.idempotent.core.keyresolver.impl.DefaultIdempotentKeyResolver;
import cn.zszj.framework.idempotent.core.keyresolver.IdempotentKeyResolver;
import cn.zszj.framework.idempotent.core.keyresolver.impl.ExpressionIdempotentKeyResolver;
import cn.zszj.framework.idempotent.core.keyresolver.impl.UserIdempotentKeyResolver;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 幂等注解
 *
 * @author 芋道源码
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /**
     * 幂等的超时时间，默认为 1 秒
     *
     * 注意，如果执行时间超过它，请求还是会进来
     */
    int timeout() default 1;
    /**
     * 时间单位，默认为 SECONDS 秒
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 提示信息，正在执行中的提示
     */
    String message() default "重复请求，请稍后重试";

    /**
     * 使用的 Key 解析器
     *
     * @see DefaultIdempotentKeyResolver 全局级别
     * @see UserIdempotentKeyResolver 用户级别
     * @see ExpressionIdempotentKeyResolver 自定义表达式，通过 {@link #keyArg()} 计算
     */
    Class<? extends IdempotentKeyResolver> keyResolver() default DefaultIdempotentKeyResolver.class;
    /**
     * 使用的 Key 参数
     */
    String keyArg() default "";

    /**
     * 是否启用持久化幂等（ZS-SEC-011.B），默认 false 走 Redis 窗口锁
     *
     * 使用约束（不满足即 fail-closed 拒绝，不做静默降级）：
     * 1. 注解方法必须「在业务事务内被调用」（推荐直接落 @Transactional service 方法）——
     *    INSERT RUNNING → 业务 → markSuccess 与业务同事务提交（MANDATORY 参与），
     *    这是「丢响应可复用原结果、重启不重复写、业务回滚无残留记录」的原子性根基；
     *    无事务调用会抛 IllegalTransactionStateException；
     * 2. 必须存在登录主体（userId 非空）——持久化记录携带结果快照，
     *    匿名/无上下文塌缩即跨主体结果重放风险，故匿名调用直接拒绝；
     * 3. 容器必须提供 {@link cn.zszj.framework.idempotent.core.persistent.PersistentIdempotentStore} bean
     *    （如 zszj-module-infra 的 JdbcPersistentIdempotentStore），缺失即抛 IllegalStateException。
     *
     * 持久化模式只走 DB 单层（唯一约束并发兜底），不再叠加 Redis 窗口锁——双层竞态在结构上不存在；
     * timeout/timeUnit 对持久化模式不生效（记录无 TTL，保留期清理归后续容量标定）。
     */
    boolean persistent() default false;

    /**
     * 删除 Key，当发生异常时候
     *
     * 问题：为什么发生异常时，需要删除 Key 呢？
     * 回答：发生异常时，说明业务发生错误，此时需要删除 Key，避免下次请求无法正常执行。
     *
     * 问题：为什么不搞 deleteWhenSuccess 执行成功时，需要删除 Key 呢？
     * 回答：这种情况下，本质上是分布式锁，推荐使用 @Lock4j 注解
     */
    boolean deleteKeyWhenException() default true;

}
