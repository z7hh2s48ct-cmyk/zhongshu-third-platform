package cn.zszj.module.infra.framework.idempotent;

import cn.zszj.framework.idempotent.core.persistent.PersistentIdempotentRecord;
import cn.zszj.framework.idempotent.core.persistent.PersistentIdempotentStatus;
import cn.zszj.framework.idempotent.core.persistent.PersistentIdempotentStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * {@link PersistentIdempotentStore} 的 JDBC 落地（ZS-SEC-011.B）——循 ZS-JOB-002 JdbcReliableEventPort 的
 * infra 承载先例：protection starter 定义 SPI、infra 以 JdbcTemplate 落 {@code infra_persistent_idempotent} 表。
 *
 * <p>SPI 契约兑现：
 * <ul>
 *     <li><b>事务参与强制（MANDATORY）</b>：写方法一律经 {@link PersistentIdempotentTransactions#mandatoryTemplate}
 *         参与调用方在本数据源上的真实事务，无即抛 IllegalTransactionStateException、不另开事务——
 *         INSERT RUNNING → 业务 → markSuccess 与业务同生共死；</li>
 *     <li><b>唯一约束并发兜底</b>：抢锁 INSERT 用 {@code ON CONFLICT (idempotent_key) DO NOTHING}——
 *         PG 下裸 INSERT 唯一冲突会 abort 整个调用方事务（败者连读回记录都做不到），DO NOTHING 不 aborted，
 *         败者可继续 findByIdempotentKey 做「重复/冲突/复用」分类；H2（2.x）同文法可移植；</li>
 *     <li><b>fail-closed</b>：存储异常一律向上抛，不吞、不静默降级；</li>
 *     <li><b>脱敏</b>：本类日志不落请求原文/结果快照原文/幂等键原文（键由切面指纹化）。</li>
 * </ul>
 *
 * <p>{@code complete_time} 由应用侧时钟写入（与 outbox 同一「单一时钟源」惯例）；{@code create_time} 走 DB 默认值。
 */
@Repository
@Slf4j
public class JdbcPersistentIdempotentStore implements PersistentIdempotentStore {

    /** 抢锁插入：冲突时静默不插（0 行），不毒化调用方事务 */
    private static final String INSERT_RUNNING_SQL = "INSERT INTO infra_persistent_idempotent "
            + "(idempotent_key, tenant_id, subject_type, subject_id, action_scope, request_digest, status) "
            + "VALUES (?, ?, ?, ?, ?, ?, 'RUNNING')";

    /** PG 方言的抢锁插入：唯一冲突静默不插（0 行），不 abort 调用方事务 */
    private static final String INSERT_RUNNING_ON_CONFLICT_SQL = INSERT_RUNNING_SQL
            + " ON CONFLICT (idempotent_key) DO NOTHING";

    private static final String SELECT_SQL = "SELECT idempotent_key, tenant_id, subject_type, subject_id, "
            + "action_scope, request_digest, status, result_snapshot, create_time, complete_time "
            + "FROM infra_persistent_idempotent WHERE idempotent_key = ?";

    private static final String MARK_SUCCESS_SQL = "UPDATE infra_persistent_idempotent "
            + "SET status = 'SUCCESS', result_snapshot = ?, complete_time = ? "
            + "WHERE idempotent_key = ? AND status = 'RUNNING'";

    private static final String MARK_FAILED_SQL = "UPDATE infra_persistent_idempotent "
            + "SET status = 'FAILED', complete_time = ? "
            + "WHERE idempotent_key = ? AND status = 'RUNNING'";

    private static final String DELETE_RUNNING_SQL = "DELETE FROM infra_persistent_idempotent "
            + "WHERE idempotent_key = ? AND status = 'RUNNING'";

    private final JdbcTemplate jdbcTemplate;

    private final DataSource dataSource;

    private final TransactionTemplate callerTransactionTemplate;

    /**
     * 抢锁写入方言（懒解析，首次写入时确定后复用）：
     * PG 用 {@code ON CONFLICT DO NOTHING}（裸 INSERT 唯一冲突会 abort 整个调用方事务，败者无法读回记录）；
     * H2（测试 MYSQL 兼容模式）不支持 ON CONFLICT，但其唯一冲突是「语句级」失败、事务仍可用，
     * 故用普通 INSERT + 捕获 DuplicateKeyException。两方言的 SPI 语义一致：
     * <b>插入成功返回 true；键已存在返回 false 且调用方事务不进入 aborted 状态</b>。
     */
    private volatile Boolean h2Dialect;

    public JdbcPersistentIdempotentStore(DataSource dataSource, org.springframework.transaction.PlatformTransactionManager transactionManager) {
        this.dataSource = dataSource;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.callerTransactionTemplate = PersistentIdempotentTransactions.mandatoryTemplate(dataSource, transactionManager);
    }

    @Override
    public boolean tryInsertRunning(PersistentIdempotentRecord record) {
        int[] inserted = new int[1];
        String insertSql = isH2Dialect() ? INSERT_RUNNING_SQL : INSERT_RUNNING_ON_CONFLICT_SQL;
        callerTransactionTemplate.execute(status -> {
            try {
                inserted[0] = jdbcTemplate.update(insertSql, ps -> {
                    ps.setString(1, record.getIdempotentKey());
                    if (record.getTenantId() == null) {
                        ps.setNull(2, Types.BIGINT);
                    } else {
                        ps.setLong(2, record.getTenantId());
                    }
                    setNullableString(ps, 3, record.getSubjectType());
                    setNullableString(ps, 4, record.getSubjectId());
                    ps.setString(5, record.getActionScope());
                    ps.setString(6, record.getRequestDigest());
                });
            } catch (DuplicateKeyException e) {
                // H2 方言：唯一冲突为语句级失败、事务仍可用 → 按未获得执行权处理（PG 走 ON CONFLICT 恒 0 行、不进此分支）
                inserted[0] = 0;
            }
            return null;
        });
        return inserted[0] == 1;
    }

    private boolean isH2Dialect() {
        Boolean h2 = this.h2Dialect;
        if (h2 == null) {
            synchronized (this) {
                if (this.h2Dialect == null) {
                    String product;
                    try {
                        product = JdbcUtils.extractDatabaseMetaData(dataSource, DatabaseMetaData::getDatabaseProductName);
                    } catch (org.springframework.jdbc.support.MetaDataAccessException e) {
                        // 方言不可判定则无法保证「冲突不毒化事务」契约 → fail-closed
                        throw new IllegalStateException("持久化幂等存储无法判定数据库方言", e);
                    }
                    log.info("[isH2Dialect][持久化幂等存储方言={}，抢锁写入：PG=ON CONFLICT DO NOTHING / H2=语句级冲突捕获]", product);
                    this.h2Dialect = product != null && product.toLowerCase().contains("h2");
                }
                h2 = this.h2Dialect;
            }
        }
        return h2;
    }

    @Override
    public Optional<PersistentIdempotentRecord> findByIdempotentKey(String idempotentKey) {
        List<PersistentIdempotentRecord> records = jdbcTemplate.query(SELECT_SQL,
                (rs, rowNum) -> {
                    PersistentIdempotentRecord record = new PersistentIdempotentRecord();
                    record.setIdempotentKey(rs.getString("idempotent_key"));
                    long tenantId = rs.getLong("tenant_id");
                    record.setTenantId(rs.wasNull() ? null : tenantId);
                    record.setSubjectType(rs.getString("subject_type"));
                    record.setSubjectId(rs.getString("subject_id"));
                    record.setActionScope(rs.getString("action_scope"));
                    record.setRequestDigest(rs.getString("request_digest"));
                    record.setStatus(PersistentIdempotentStatus.valueOf(rs.getString("status")));
                    record.setResultSnapshot(rs.getString("result_snapshot"));
                    Timestamp createTime = rs.getTimestamp("create_time");
                    record.setCreateTime(createTime == null ? null : new Date(createTime.getTime()));
                    Timestamp completeTime = rs.getTimestamp("complete_time");
                    record.setCompleteTime(completeTime == null ? null : new Date(completeTime.getTime()));
                    return record;
                },
                idempotentKey);
        return records.isEmpty() ? Optional.empty() : Optional.of(records.get(0));
    }

    @Override
    public void markSuccess(String idempotentKey, String resultSnapshot) {
        callerTransactionTemplate.execute(status -> {
            int updated = jdbcTemplate.update(MARK_SUCCESS_SQL, ps -> {
                if (resultSnapshot == null) {
                    ps.setNull(1, Types.VARCHAR);
                } else {
                    ps.setString(1, resultSnapshot);
                }
                ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
                ps.setString(3, idempotentKey);
            });
            if (updated != 1) {
                // 状态守卫（status='RUNNING'）未命中：并发/状态机异常，fail-closed 上抛由事务回滚兜底
                log.error("[markSuccess][持久化幂等状态推进失败 keyFingerprint={} updated={}]", idempotentKey, updated);
                throw new IllegalStateException("持久化幂等 markSuccess 未命中 RUNNING 记录: updated=" + updated);
            }
            return null;
        });
    }

    @Override
    public void markFailed(String idempotentKey) {
        callerTransactionTemplate.execute(status -> {
            int updated = jdbcTemplate.update(MARK_FAILED_SQL, ps -> {
                ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                ps.setString(2, idempotentKey);
            });
            if (updated != 1) {
                log.error("[markFailed][持久化幂等状态推进失败 keyFingerprint={} updated={}]", idempotentKey, updated);
                throw new IllegalStateException("持久化幂等 markFailed 未命中 RUNNING 记录: updated=" + updated);
            }
            return null;
        });
    }

    @Override
    public boolean deleteRunning(String idempotentKey) {
        boolean[] deleted = new boolean[1];
        callerTransactionTemplate.execute(status -> {
            deleted[0] = jdbcTemplate.update(DELETE_RUNNING_SQL, ps -> ps.setString(1, idempotentKey)) == 1;
            return null;
        });
        return deleted[0];
    }

    private static void setNullableString(PreparedStatement ps, int index, String value) throws java.sql.SQLException {
        if (value == null) {
            ps.setNull(index, Types.VARCHAR);
        } else {
            ps.setString(index, value);
        }
    }

}
