package cn.zszj.module.system.framework.outbox;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ZS-PERM-004.C：缓存驱逐失败补偿事件载荷（outbox payload，JSON 序列化）。
 *
 * <p>由 {@link OutboxCacheEvictionFailureRecorder} 写入、{@link CacheEvictionCompensationSink}
 * 重放消费；字段与 {@code RetryEvictCache} 记录点一一对应。
 *
 * <p>载荷不携带任何用户数据/令牌内容（缓存名与键均为资源标识）；biz_id 冗余为
 * {@code cacheName:key}（CLEAR 时为 {@code cacheName:__clear__}）供 JOB-004 恢复台账人工排查。
 *
 * @author ZS-PERM-004.C
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CacheEvictionCompensationPayload {

    /** 缓存名（如 role / user_role_ids）。 */
    private String cacheName;

    /** 失败键字符串形式；{@code null} 表示 CLEAR 操作。 */
    private String key;

    /** 操作类型：EVICT / CLEAR。 */
    private String operation;

    /** 失败发生时间（记录点时刻）。 */
    private LocalDateTime failedAt;

}
