package cn.zszj.framework.idempotent.core.persistent;

import lombok.Data;

import java.util.Date;

/**
 * 持久化幂等记录（ZS-SEC-011.B）——SPI 的传输对象，与表 {@code infra_persistent_idempotent} 一一对应。
 *
 * 字段说明：
 * <ul>
 *     <li>{@code idempotentKey}：稳定短键（切面侧已做 md5 定长 32，主体/租户/动作范围已烘入哈希输入，
 *         无长度截断碰撞）；表上唯一约束兜底并发；</li>
 *     <li>{@code tenantId/subjectType/subjectId}：主体/租户作用域（可观测列；键隔离已由 idempotentKey 承担）；</li>
 *     <li>{@code actionScope}：方法签名（可观测，定位是哪个动作在重放）；</li>
 *     <li>{@code requestDigest}：请求摘要（未截断脱敏表示的 MD5，全量入参参与比对，无 2048 截断碰撞）；</li>
 *     <li>{@code resultSnapshot}：成功结果 JSON 快照；null 表示仅状态级复用（重放拒绝而非返回原结果）。</li>
 * </ul>
 *
 * @author 众墅之家
 */
@Data
public class PersistentIdempotentRecord {

    /** 幂等键（定长 md5 短键，表上唯一） */
    private String idempotentKey;

    /** 租户编号（可空：无租户上下文的调用方；键隔离不依赖本列） */
    private Long tenantId;

    /** 主体类型（WebFrameworkUtils.getLoginUserType，可空） */
    private String subjectType;

    /** 主体编号（登录用户编号，持久化模式强制非空） */
    private String subjectId;

    /** 动作范围（方法签名，可观测） */
    private String actionScope;

    /** 请求摘要（未截断脱敏表示的 MD5） */
    private String requestDigest;

    /** 状态 */
    private PersistentIdempotentStatus status;

    /** 成功结果 JSON 快照（null = 仅状态级复用） */
    private String resultSnapshot;

    /** 创建时间 */
    private Date createTime;

    /** 完成时间（SUCCESS / FAILED 时设置） */
    private Date completeTime;

}
