/**
 * ZS-AUDIT-001：统一业务审计的 system 模块适配层。
 *
 * <p>{@code audit.core} 提供 {@link cn.zszj.framework.common.biz.system.audit.AuditPort} 的 JDBC 实现
 * （同库事务内同步落库、不可改写）；审计接口与事件模型置于 {@code cn.zszj.framework.common.biz.system.audit}
 * （zszj-common），实现不落 infra/file 或 infra/job（避 WT-A/WT-C 领地）。
 */
package cn.zszj.module.system.framework.audit;
